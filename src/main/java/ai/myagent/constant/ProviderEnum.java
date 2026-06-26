package ai.myagent.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author yulewei
 * @since 2026/6/26
 */
@Getter
@AllArgsConstructor
public enum ProviderEnum {
    OPENAI("openai"),
    DEEPSEEK("deepseek"),
    GLM("glm"),
    QWEN("qwen");

    private final String code;

    public static ProviderEnum of(String code) {
        for (ProviderEnum value : values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
