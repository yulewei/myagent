package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionNewReq {

    @Size(max = 32, message = "会话ID不能超过32个字符")
    @Schema(description = "会话ID，非必传，若未指定，则自动生成")
    private String sessionId;

    @Size(max = 32, message = "模型ID不能超过32个字符")
    @Schema(description = "模型ID，非必传，若指定则覆盖默认模型")
    private String modelId;

    @Schema(description = "工具列表，非必传，若未指定，则使用默认工具列表；若列表中包含 `NONE` 或大小为0，则表示不使用工具；若列表中包含 `FULL`，则表示使用所有工具")
    private List<String> tools;

    @Size(max = 30, message = "会话标题不能超过30个字符")
    @Schema(description = "会话标题")
    private String title;
}
