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
 * @since 2026/6/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeUpdateReq {

    @Size(max = 32, message = "知识库ID不能超过32个字符")
    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "知识库ID，非必传，若未指定，则自动生成")
    private String id;


    @Size(max = 20, message = "知识库名称不能超过20个字符")
    @NotBlank(message = "知识库名称不能为空")
    @Schema(description = "知识库名称")
    private String name;
}
