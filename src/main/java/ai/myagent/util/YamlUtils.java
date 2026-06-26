package ai.myagent.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author yulewei
 * @since 2021/4/11
 */
@Slf4j
@UtilityClass
public class YamlUtils {

    public static YAMLMapper mapper;

    static {
        mapper = new YAMLMapper();
        mapper.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);
        mapper.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        mapper.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * YAML 序列化
     */
    public static String toYaml(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            log.warn("write to json string error: {}", object, e);
            return null;
        }
    }

    /**
     * YAML 反序列化
     */
    public static <T> T parse(@Nullable String yamlStr, Class<T> clazz) {
        if (StringUtils.isEmpty(yamlStr)) {
            return null;
        }
        try {
            return mapper.readValue(yamlStr, clazz);
        } catch (IOException e) {
            log.warn("parse yaml string error: {}", yamlStr, e);
            return null;
        }
    }

}
