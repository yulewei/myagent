package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class DefaultModelReq {

    @Size(min = 1, max = 32, message = "模型ID长度不能超过32个字符")
    @NotBlank(message = "模型ID不能为空")
    @Schema(description = "模型ID")
    private String modelId;
}
