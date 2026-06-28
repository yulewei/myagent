package ai.myagent.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Agent 配置
 *
 * @author yulewei
 */
@Data
public class AgentConfig {

    /**
     * 默认配置
     */
    @JsonProperty("default")
    private DefaultConfig defaultConfig;

    /**
     * 模型服务商配置列表
     */
    private List<ProviderConfig> providers;

    @Data
    public static class DefaultConfig {

        private SessionConfig session;

        private EmbeddingConfig embedding;
    }

    /**
     * 默认配置
     */
    @Data
    public static class SessionConfig {
        /**
         * 默认会话标题
         */
        private String title;

        /**
         * 服务商ID
         */
        private String provider;

        /**
         * 默认模型 ID，如 "deepseek-v4-flash"。
         */
        private String modelId;

        /**
         * 默认温度参数（0~2），可为 null
         */
        private Double temperature;

        /**
         * 默认工具集
         */
        private List<String> tools;
    }

    @Data
    public static class EmbeddingConfig {
        /**
         * 服务商ID
         */
        private String providerId;

        /**
         * 默认模型 ID，如 "deepseek-v4-flash"。
         */
        private String modelId;

        /**
         * 默认向量维度
         */
        private Integer dimensions;
    }

    /**
     * 单个 AI 服务商配置，对应 config.yaml 中 providers[] 的每一项。
     */
    @Data
    public static class ProviderConfig {

        /**
         * 服务商ID：openai、deepseek、glm、qwen
         */
        private String id;

        /**
         * 服务商名称，如 DeepSeek、GLM、Qwen
         */
        private String name;

        /**
         * 是否启用，默认 true
         */
        private Boolean enabled;

        /**
         * API 基础地址
         */
        private String baseUrl;

        /**
         * API 密钥
         */
        private String apiKey;

        /**
         * 该服务商下可用的模型列表
         */
        private List<ModelInfo> models;
    }

    /**
     * 单个模型信息，对应 config.yaml 中 providers[].models[] 的每一项。
     */
    @Data
    public static class ModelInfo {

        /**
         * 模型唯一标识，如 deepseek-v4-flash
         */
        private String id;

        /**
         * 上下文窗口大小（token 数），如 1000000。
         */
        private Integer contextWindow;

        /**
         * 模型类型："embedding" 表示嵌入模型，普通 LLM 省略此字段
         */
        private String type;
    }


}
