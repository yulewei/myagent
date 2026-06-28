package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class KnowledgeDocTextReq {

    @Schema(description = "text类型的文档内容")
    private String content;
}
