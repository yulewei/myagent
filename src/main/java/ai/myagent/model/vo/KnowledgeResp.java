package ai.myagent.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import static ai.myagent.util.DateUtils.ISO_DATETIME_EXT_PATTERN;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeResp {

    @Schema(description = "知识库ID")
    private String id;

    @Schema(description = "知识库名称")
    private String name;

    @Schema(description = "提供商ID")
    private String providerId;

    @Schema(description = "向量化模型ID")
    private String modelId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "文档列表")
    private List<KnowledgeDocResp> docList;
}
