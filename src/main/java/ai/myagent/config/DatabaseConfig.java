package ai.myagent.config;

import ai.myagent.model.dto.EmbeddingModelDto;
import ai.myagent.service.ConfigService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;

/**
 * @author yulewei
 * @since 2026/6/26
 */
@Slf4j
@Configuration
public class DatabaseConfig {
    @Value("${myagent.data-dir}")
    private String dataDir;
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${myagent.knowledge.vector-db-file:vectordb.json}")
    private String vectorDbFile;

    @Bean
    @DependsOn("configService")
    public DataSource dataSource() {
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setConfig(config);
        dataSource.setUrl(url);

        dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(dataSource);
        hikariConfig.setConnectionInitSql(String.format("select load_extension('%s')", dataDir + "vec0"));
        return new HikariDataSource(hikariConfig);
    }

    @Bean
    @DependsOn("configService")
    public SimpleVectorStore simpleVectorStore(ConfigService configService) {
        log.info("初始化知识库");
        SimpleVectorStore vectorStore = null;
        EmbeddingModelDto dto = configService.queryEmbeddingModel();
        if (dto == null || StringUtils.isBlank(dto.getApiKey()) ||
                dto.getApiKey().startsWith("${") && dto.getApiKey().endsWith("}")) {
            log.warn("向量化模型，未正确配置");
        } else {
            vectorStore = SimpleVectorStore.builder(dto.getEmbeddingModel()).build();
        }
        dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
        File file = new File(dataDir + vectorDbFile);
        if (file.exists() && vectorStore != null) {
            vectorStore.load(file);
            log.info("加载向量数据库文件：{}", file.getAbsolutePath());
        }
        return vectorStore;
    }
}
