package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author yulewei
 * @since 2026/6/19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionUpdateReq implements Serializable {

    @Size(min = 1, max = 32, message = "会话ID长度不能超过32个字符")
    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID")
    private String sessionId;

    @Size(max = 32, message = "模型ID长度不能超过32个字符")
    @Schema(description = "模型ID")
    private String modelId;

    @Size(max = 30, message = "会话标题长度不能超过30个字符")
    @Schema(description = "会话标题")
    private String title;
}

