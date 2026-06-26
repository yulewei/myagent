package ai.myagent.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author yulewei
 * @since 2024/9/28
 */
@Getter
@Slf4j
public class JsonMapper {
    private final ObjectMapper objectMapper;

    private JsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private JsonMapper(JsonInclude.Include include) {
        objectMapper = new ObjectMapper();
        // 序列化时，忽略空 Bean 触发的序列化失败（默认为开启，改为禁用）
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 反序列化时，忽略在 JSON 字符串中存在但 Java 对象实际没有字段而触发反序列化失败（默认为开启，改为禁用）
        // 避免在类头上添加注解 @JsonIgnoreProperties(ignoreUnknown = true)
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 反序列时，浮点数优先转为 BigDecimal
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        // 序列化时，是否输出 null 字段，默认 ALWAYS
        if (include != null) {
            objectMapper.setDefaultPropertyInclusion(include);
        }
        // 序列化与反序列化时，禁用日期和时间转为时间戳（默认为开启）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 序列化与反序列化时，Java 日期和时间的格式
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Date.class, new DateSerializer(true, null));
        objectMapper.registerModule(javaTimeModule);
    }

    public static JsonMapper defaultMapper() {
        return new JsonMapper((JsonInclude.Include) null);
    }

    public static JsonMapper nonNullMapper() {
        return new JsonMapper(JsonInclude.Include.NON_NULL);
    }

    public static JsonMapper of(ObjectMapper objectMapper) {
        return new JsonMapper(objectMapper);
    }

    /**
     * JSON 序列化
     */
    public String toJsonStr(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            log.warn("write to json string error: {}", object, e);
            return null;
        }
    }

    /**
     * JSON 反序列化
     */
    public <T> T parse(@Nullable String jsonStr, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonStr)) {
            return null;
        } try {
            return objectMapper.readValue(jsonStr, clazz);
        } catch (IOException e) {
            log.warn("parse json string error: {}", jsonStr, e); return null;
        }
    }

    /**
     * JSON 反序列化
     */
    public <T> T parse(@Nullable String jsonStr, TypeReference<T> typeRef) {
        if (StringUtils.isEmpty(jsonStr)) {
            return null;
        }
        try {
            return (T) objectMapper.readValue(jsonStr, typeRef);
        } catch (IOException e) {
            log.warn("parse json string error: {}", jsonStr, e);
            return null;
        }
    }

    /**
     * JSON 反序列化
     */
    public <T> T parse(@Nullable String jsonStr, JavaType javaType) {
        if (StringUtils.isEmpty(jsonStr)) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonStr, javaType);
        } catch (IOException e) {
            log.warn("parse json string error: {}", jsonStr, e);
            return null;
        }
    }

    /**
     * JSON 反序列化
     */
    public <T> List<T> parseArray(@Nullable String jsonStr, Class<T> clazz) {
        JavaType collectionType = objectMapper.getTypeFactory().constructCollectionType(Collection.class, clazz);
        return this.parse(jsonStr, collectionType);
    }

    /**
     * JSON 反序列化
     */
    public <K, V> Map<K, V> parseMap(@Nullable String jsonStr, Class<K> keyClass, Class<V> valueClass) {
        JavaType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
        return this.parse(jsonStr, mapType);
    }

    /**
     * 类型转换。底层实现类似于，先将值序列化为 JSON，然后将 JSON 反序列化为指定类型，但比完全的序列化和反序列化更加高效。
     */
    public <T> T convert(@Nullable Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, clazz);
    }

    /**
     * 类型转换
     */
    public Map<String, Object> convert2Map(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        JavaType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        return objectMapper.convertValue(obj, mapType);
    }

    /**
     * 校验是否是合法 JSON Object 或 Array 字符串
     */
    public boolean checkIsValidJsonStr(String jsonStr) {
        if (StringUtils.isBlank(jsonStr) || jsonStr.trim().length() != jsonStr.length()) {
            return false;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            return jsonNode.isContainerNode();
        } catch (JacksonException e) {
            return false;
        }
    }
}
