package ai.myagent.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类，简单封装 Jackson
 *
 * @author yulewei
 * @since 2021/3/6
 */
@Slf4j
@UtilityClass
public class JsonUtils {

    private static final JsonMapper jsonMapper = JsonMapper.defaultMapper();

    /**
     * JSON 序列化
     */
    public static String toJsonStr(Object object) {
        return jsonMapper.toJsonStr(object);
    }

    /**
     * JSON 反序列化
     */
    public static <T> T parse(@Nullable String jsonStr, Class<T> clazz) {
        return jsonMapper.parse(jsonStr, clazz);
    }

    /**
     * JSON 反序列化
     */
    public static <T> T parse(@Nullable String jsonStr, TypeReference<T> typeRef) {
        return jsonMapper.parse(jsonStr, typeRef);
    }

    /**
     * JSON 反序列化
     */
    public static <T> T parse(@Nullable String jsonStr, JavaType javaType) {
        return jsonMapper.parse(jsonStr, javaType);
    }

    /**
     * JSON 反序列化
     */
    public static <T> List<T> parseArray(@Nullable String jsonStr, Class<T> clazz) {
        return jsonMapper.parseArray(jsonStr, clazz);
    }

    /**
     * JSON 反序列化
     */
    public static <K, V> Map<K, V> parseMap(@Nullable String jsonStr, Class<K> keyClass, Class<V> valueClass) {
        return jsonMapper.parseMap(jsonStr, keyClass, valueClass);
    }

    /**
     * 类型转换。底层实现类似于，先将值序列化为 JSON，然后将 JSON 反序列化为指定类型，但比完全的序列化和反序列化更加高效。
     */
    public static <T> T convert(@Nullable Object obj, Class<T> clazz) {
        return jsonMapper.convert(obj, clazz);
    }

    /**
     * 类型转换
     */
    public Map<String, Object> convert2Map(@Nullable Object obj) {
        return jsonMapper.convert2Map(obj);
    }

    /**
     * 校验是否是合法 JSON Object 或 Array 字符串
     */
    public boolean checkIsValidJsonStr(String jsonStr) {
        return jsonMapper.checkIsValidJsonStr(jsonStr);
    }
}
