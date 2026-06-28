package ai.myagent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author yulewei
 * @since 2026/6/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmbeddingModelDto {

    /**
     * 模型服务商ID
     */
    private String providerId;

    /**
     * 模型服务商名称
     */
    private String providerName;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * API URL前缀
     */
    private String baseUrl;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * Spring AI 的 embeddingModel 对象
     */
    private EmbeddingModel embeddingModel;
}
