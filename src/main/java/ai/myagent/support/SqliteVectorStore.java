package ai.myagent.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.filter.converter.AbstractFilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO 正确性待验证
 *
 * 基于 <a href="https://github.com/asg017/sqlite-vec">sqlite-vec</a> 扩展的向量存储实现。
 *
 * <p>使用两张表存储数据：
 * <ul>
 *   <li>{@code {tableName}} — 文档内容表（id, doc_id, content, metadata）</li>
 *   <li>{@code {tableName}_vec} — vec0 虚拟表，存储 float[{@code dimensions}] 向量</li>
 * </ul>
 * <p>
 * 相似度搜索时通过 {@code rowid} 关联两张表，返回按距离排序的结果。
 *
 * @author yulewei
 * @since 2026/6/27
 */
public class SqliteVectorStore extends AbstractObservationVectorStore {

    private static final Logger log = LoggerFactory.getLogger(SqliteVectorStore.class);

    /**
     * 默认向量维度（若未指定且 EmbeddingModel 无默认值时使用）
     */
    private static final int DEFAULT_DIMENSIONS = 1536;

    /**
     * 默认距离度量
     */
    private static final String DEFAULT_DISTANCE_METRIC = "cosine";

    /**
     * 默认表名前缀
     */
    private static final String DEFAULT_TABLE_NAME = "t_vector_store";

    // ========== 实例字段 ==========

    private final JdbcTemplate jdbcTemplate;

    private final String tableName;

    private final int dimensions;

    private final String distanceMetric;

    private final boolean initializeSchema;

    private final FilterExpressionConverter filterExpressionConverter;

    // ========== 构造 & Builder ==========

    protected SqliteVectorStore(SqliteVectorStoreBuilder builder) {
        super(builder);
        this.jdbcTemplate = builder.jdbcTemplate;
        this.tableName = builder.tableName;
        this.dimensions = builder.dimensions;
        this.distanceMetric = builder.distanceMetric;
        this.initializeSchema = builder.initializeSchema;
        this.filterExpressionConverter = builder.filterExpressionConverter;

        if (this.initializeSchema) {
            initSchema();
        }
    }

    public static SqliteVectorStoreBuilder builder(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return new SqliteVectorStoreBuilder(jdbcTemplate, embeddingModel);
    }

    // ========== Schema 初始化 ==========

    /**
     * 创建内容表和 vec0 虚拟表（如果不存在）。
     *
     * <pre>
     * -- 内容表（文档存储）
     * CREATE TABLE {tableName} (
     *   id       INTEGER PRIMARY KEY AUTOINCREMENT,
     *   doc_id   TEXT NOT NULL UNIQUE,
     *   content  TEXT NOT NULL,
     *   metadata TEXT
     * );
     *
     * -- vec0 虚拟表（向量搜索）
     * CREATE VIRTUAL TABLE {tableName}_vec USING vec0(
     *   embedding float[{dim}] distance_metric={metric}
     * );
     * </pre>
     */
    private void initSchema() {
        jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    doc_id   TEXT    NOT NULL UNIQUE,
                    content  TEXT    NOT NULL,
                    metadata TEXT
                )
                """, tableName));

        jdbcTemplate.execute(String.format("""
                CREATE VIRTUAL TABLE IF NOT EXISTS %s_vec USING vec0(
                    embedding float[%d] distance_metric=%s
                )
                """, tableName, dimensions, distanceMetric));

        jdbcTemplate.execute(String.format(
                "CREATE INDEX IF NOT EXISTS %s_idx_doc_id ON %s(doc_id)", tableName, tableName));

        log.info("Initialized vector store schema: {}/{}_vec (dim={}, metric={})",
                tableName, tableName, dimensions, distanceMetric);
    }

    // ========== 文档操作 ==========

    @Override
    public void doAdd(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        // 批量计算文档的向量嵌入
        List<float[]> embeddings = this.embeddingModel.embed(
                documents, EmbeddingOptions.builder().build(), this.batchingStrategy);

        // 准备 SQL
        String upsertContent = String.format("""
                INSERT INTO %s (doc_id, content, metadata) VALUES (?, ?, ?)
                ON CONFLICT(doc_id) DO UPDATE SET content = excluded.content, metadata = excluded.metadata
                """, tableName);

        String upsertVec = String.format(
                "INSERT OR REPLACE INTO %s_vec (rowid, embedding) VALUES (?, ?)", tableName);

        String selectRowId = String.format("SELECT id FROM %s WHERE doc_id = ?", tableName);

        // 逐文档写入
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            float[] embedding = embeddings.get(i);

            // 序列化 metadata → JSON
            String metadataJson = null;
            Map<String, Object> metadata = doc.getMetadata();
            if (!metadata.isEmpty()) {
                metadataJson = metadata.entrySet().stream()
                        .map(e -> String.format("\"%s\":%s", escapeJsonString(e.getKey()), toJsonValue(e.getValue())))
                        .collect(Collectors.joining(",", "{", "}"));
            }

            // 写入内容表
            jdbcTemplate.update(upsertContent, doc.getId(), doc.getText(), metadataJson);

            // 获取自增 rowid
            Integer rowId = jdbcTemplate.queryForObject(selectRowId, Integer.class, doc.getId());
            if (rowId == null) {
                log.warn("Failed to get rowid for doc_id: {}", doc.getId());
                continue;
            }

            // 写入 vec0 虚拟表
            jdbcTemplate.update(upsertVec, rowId, floatArrayToJson(embedding));
        }
    }

    @Override
    public void doDelete(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return;
        }

        for (String docId : idList) {
            List<Integer> rowIds = jdbcTemplate.queryForList(
                    String.format("SELECT id FROM %s WHERE doc_id = ?", tableName),
                    Integer.class, docId);

            for (Integer rowId : rowIds) {
                jdbcTemplate.update(String.format("DELETE FROM %s_vec WHERE rowid = ?", tableName), rowId);
                jdbcTemplate.update(String.format("DELETE FROM %s WHERE id = ?", tableName), rowId);
            }
        }
    }

    // ========== 相似性搜索 ==========

    @Override
    public List<Document> doSimilaritySearch(SearchRequest request) {
        // 计算查询向量
        float[] queryEmbedding = this.embeddingModel.embed(request.getQuery());
        String embeddingJson = floatArrayToJson(queryEmbedding);

        int topK = request.getTopK();
        double similarityThreshold = request.getSimilarityThreshold();

        // 构建过滤条件
        String filterClause = buildFilterClause(request.getFilterExpression());

        // 构建 SQL
        StringBuilder sql = new StringBuilder();
        sql.append(String.format("""
                SELECT c.doc_id, c.content, c.metadata, v.distance
                FROM %s_vec v
                JOIN %s c ON c.id = v.rowid
                WHERE v.embedding MATCH ?
                """, tableName, tableName));

        if (!filterClause.isEmpty()) {
            sql.append(" AND (").append(filterClause).append(")");
        }

        sql.append(" AND v.k = ? ORDER BY v.distance");

        // 执行查询
        List<Document> results = new ArrayList<>();

        jdbcTemplate.query(sql.toString(), (ResultSet rs) -> {
            while (rs.next()) {
                String docId = rs.getString("doc_id");
                String content = rs.getString("content");
                String metadataJson = rs.getString("metadata");
                double distance = rs.getDouble("distance");

                double score = convertDistanceToSimilarity(distance);

                if (score < similarityThreshold) {
                    continue;
                }

                // 反序列化 metadata
                Map<String, Object> metadata = new HashMap<>();
                if (metadataJson != null) {
                    metadata = parseMetadataJson(metadataJson);
                }
                metadata.put("distance", distance);
                metadata.put("score", score);

                results.add(new Document(docId, content, metadata));
            }
        }, embeddingJson, topK);

        return results;
    }

    // ========== 内部工具方法 ==========

    /**
     * 将 vec0 返回的距离值转换为相似度分数（0~1, 越大越相似）。
     */
    private double convertDistanceToSimilarity(double distance) {
        return switch (distanceMetric) {
            case "cosine" -> 1.0 - distance;
            case "l2" -> 1.0 / (1.0 + distance);
            case "inner_product" -> 1.0 - distance;
            default -> 1.0 - distance;
        };
    }

    /**
     * 将 {@link Filter.Expression} 转换为 SQLite WHERE 子句。
     * 优先使用用户提供的 {@link FilterExpressionConverter}，
     * 否则使用内置的 {@link SqliteFilterExpressionConverter}。
     */
    private String buildFilterClause(Filter.Expression filterExpression) {
        if (filterExpression == null) {
            return "";
        }

        if (filterExpressionConverter != null) {
            String converted = filterExpressionConverter.convertExpression(filterExpression);
            return converted != null ? converted : "";
        }

        // 使用内置的默认转换器
        SqliteFilterExpressionConverter converter = new SqliteFilterExpressionConverter();
        String converted = converter.convertExpression(filterExpression);
        return converted != null ? converted : "";
    }

    /**
     * 将 float[] 向量序列化为 JSON 数组字符串，供 sqlite-vec 的 MATCH 和 INSERT 使用。
     * 例如：{@code [0.001, 0.002, ...]}
     */
    private static String floatArrayToJson(float[] array) {
        StringBuilder sb = new StringBuilder(array.length * 8);
        sb.append('[');
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     */
    private static String escapeJsonString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * 将任意值序列化为 JSON 值字符串片段。
     */
    private static String toJsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return escapeJsonString(s);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Date d) {
            return escapeJsonString(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC).format(d.toInstant()));
        }
        return escapeJsonString(value.toString());
    }

    /**
     * 解析 metadata JSON 字符串为 Map。
     * 使用简单的手工解析，避免引入复杂依赖。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseMetadataJson(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return result;

        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return result;

        // 去掉外层花括号
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return result;

        // 按逗号分割 key-value 对（忽略嵌套结构中的逗号）
        int depth = 0;
        boolean inString = false;
        StringBuilder pair = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"' && (i == 0 || inner.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                if (c == '}' || c == ']') depth--;
                if (c == ',' && depth == 0) {
                    addPair(pair.toString(), result);
                    pair.setLength(0);
                    continue;
                }
            }
            pair.append(c);
        }
        if (!pair.isEmpty()) {
            addPair(pair.toString(), result);
        }

        return result;
    }

    /**
     * 解析单个 "key":value 对并写入 map。
     */
    private static void addPair(String pair, Map<String, Object> map) {
        int colonIdx = pair.indexOf(':');
        if (colonIdx < 0) return;

        String key = pair.substring(0, colonIdx).trim();
        String value = pair.substring(colonIdx + 1).trim();

        // 去除 key 的引号
        if (key.length() >= 2 && key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"') {
            key = key.substring(1, key.length() - 1);
        }

        // 解析 value（简化处理：string / number / boolean / null）
        map.put(key, parseJsonValue(value));
    }

    private static Object parseJsonValue(String value) {
        if (value == null || value.isBlank()) return null;
        if ("null".equals(value)) return null;
        if ("true".equals(value)) return Boolean.TRUE;
        if ("false".equals(value)) return Boolean.FALSE;

        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            // 带引号的字符串 — 简单 unescape
            return value.substring(1, value.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t");
        }

        // 数字
        try {
            return value.contains(".") ? Double.valueOf(value) : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    // ========== Observation 支持 ==========

    @Override
    public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
        return VectorStoreObservationContext.builder(operationName, "sqlite-vec")
                .collectionName(this.tableName)
                .dimensions(this.dimensions)
                .similarityMetric(this.distanceMetric)
                .namespace("sqlite-vec");
    }

    // ========== 内置 Filter Expression 转换器 ==========

    /**
     * 将 Spring AI {@link Filter.Expression} 树转换为 SQLite
     * {@code json_extract(metadata, '$.key')} 表达式。
     *
     * <p>示例转换：
     * <pre>
     *   country == "US"          → json_extract(metadata, '$.country') = 'US'
     *   year > 2020              → json_extract(metadata, '$.year') > 2020
     *   tags IN ["a", "b"]       → json_extract(metadata, '$.tags') IN ('a', 'b')
     *   (a == 1) AND (b == 2)   → (json_extract(metadata, '$.a') = 1) AND (json_extract(metadata, '$.b') = 2)
     * </pre>
     */
    static class SqliteFilterExpressionConverter extends AbstractFilterExpressionConverter {

        private static final DateTimeFormatter DATE_FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

        @Override
        protected void doExpression(Filter.Expression expression, StringBuilder context) {
            ExpressionType type = expression.type();

            if (type == ExpressionType.AND || type == ExpressionType.OR) {
                context.append('(');
                convertOperand(expression.left(), context);
                context.append(' ').append(type.name()).append(' ');
                convertOperand(expression.right(), context);
                context.append(')');
                return;
            }

            // 一元运算符：ISNULL / ISNOTNULL
            if (type == ExpressionType.ISNULL || type == ExpressionType.ISNOTNULL) {
                convertOperand(expression.left(), context);
                context.append(type == ExpressionType.ISNULL ? " IS NULL" : " IS NOT NULL");
                return;
            }

            // 二元比较运算符
            convertOperand(expression.left(), context);
            context.append(' ').append(sqlOperator(type)).append(' ');
            convertOperand(expression.right(), context);
        }

        @Override
        protected void doKey(Filter.Key filterKey, StringBuilder context) {
            String key = filterKey.key();
            // 去除外层引号（如果存在）
            if (key.length() >= 2
                    && ((key.startsWith("\"") && key.endsWith("\""))
                    || (key.startsWith("'") && key.endsWith("'")))) {
                key = key.substring(1, key.length() - 1);
            }
            context.append("json_extract(metadata, '$.").append(key).append("')");
        }

        @Override
        protected void doSingleValue(Object value, StringBuilder context) {
            if (value instanceof String s) {
                context.append('\'').append(s.replace("'", "''")).append('\'');
            } else if (value instanceof Number || value instanceof Boolean) {
                context.append(value);
            } else if (value instanceof Date d) {
                context.append('\'')
                        .append(DATE_FORMATTER.format(d.toInstant()))
                        .append('\'');
            } else if (value instanceof Enum<?> e) {
                context.append('\'').append(e.name()).append('\'');
            } else {
                context.append('\'').append(value).append('\'');
            }
        }

        @Override
        protected void doStartValueRange(Filter.Value listValue, StringBuilder context) {
            context.append('(');
        }

        @Override
        protected void doEndValueRange(Filter.Value listValue, StringBuilder context) {
            context.append(')');
        }

        @Override
        protected void doAddValueRangeSpitter(Filter.Value listValue, StringBuilder context) {
            context.append(',');
        }

        private static String sqlOperator(ExpressionType type) {
            return switch (type) {
                case EQ -> "=";
                case NE -> "!=";
                case GT -> ">";
                case GTE -> ">=";
                case LT -> "<";
                case LTE -> "<=";
                case IN -> "IN";
                case NIN -> "NOT IN";
                default -> throw new IllegalArgumentException("Unsupported expression type: " + type);
            };
        }
    }

    // ========== Builder ==========

    public static final class SqliteVectorStoreBuilder extends AbstractVectorStoreBuilder<SqliteVectorStoreBuilder> {

        private final JdbcTemplate jdbcTemplate;

        private String tableName = DEFAULT_TABLE_NAME;

        private int dimensions = DEFAULT_DIMENSIONS;

        private String distanceMetric = DEFAULT_DISTANCE_METRIC;

        private boolean initializeSchema = true;

        private FilterExpressionConverter filterExpressionConverter;

        private SqliteVectorStoreBuilder(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
            super(embeddingModel);
            this.jdbcTemplate = jdbcTemplate;
            this.dimensions = embeddingModel.dimensions();
        }

        /**
         * 设置文档内容表的名称。vec0 虚拟表自动命名为 {@code {tableName}_vec}。
         * 默认值：{@value DEFAULT_TABLE_NAME}。
         */
        public SqliteVectorStoreBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * 设置向量维度。默认从 {@link EmbeddingModel#dimensions()} 获取，
         * 若获取失败则使用 {@value DEFAULT_DIMENSIONS}。
         */
        public SqliteVectorStoreBuilder dimensions(int dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * 设置距离度量类型。支持：{@code cosine}（默认）、{@code l2}、{@code inner_product}。
         */
        public SqliteVectorStoreBuilder distanceMetric(String distanceMetric) {
            this.distanceMetric = distanceMetric;
            return this;
        }

        /**
         * 设置是否在首次使用时自动创建表结构。默认 true。
         */
        public SqliteVectorStoreBuilder initializeSchema(boolean initializeSchema) {
            this.initializeSchema = initializeSchema;
            return this;
        }

        /**
         * 设置自定义的 {@link FilterExpressionConverter}。若未设置则使用内置的
         * {@link SqliteFilterExpressionConverter}，它将 metadata 字段引用映射为
         * SQLite 的 {@code json_extract(metadata, '$.field')} 表达式。
         */
        public SqliteVectorStoreBuilder filterExpressionConverter(FilterExpressionConverter converter) {
            this.filterExpressionConverter = converter;
            return this;
        }

        @Override
        public SqliteVectorStore build() {
            return new SqliteVectorStore(this);
        }
    }
}
