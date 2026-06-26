package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yulewei
 * @since 2026/6/22
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolInfoResp {
    @Schema(description = "工具名称")
    private String name;
    @Schema(description = "工具描述")
    private String description;
}
