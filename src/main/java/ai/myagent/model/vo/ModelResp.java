package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yulewei
 * @since 2026/6/19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelResp {
    @Schema(description = "是否默认模型")
    private Boolean isDefault;

    @Schema(description = "模型服务商ID")
    private String providerId;

    @Schema(description = "模型服务商名称")
    private String providerName;

    @Schema(description = "模型ID")
    private String modelId;

    @Schema(description = "API URL前缀")
    private String baseUrl;

    @Schema(description = "API密钥")
    private String apiKey;
}
