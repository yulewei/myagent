package ai.myagent.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ai.myagent.util.DateUtils.ISO_DATETIME_EXT_PATTERN;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocResp {

    @Schema(description = "文档ID")
    private String id;

    @Schema(description = "知识库ID")
    private String knowledgeId;

    @Schema(description = "文档类型：file、text")
    private String type;

    @Schema(description = "file类型的文档信息")
    private FileInfoVo fileInfo;

    @Schema(description = "text类型的文档内容")
    private String textContent;

    @Schema(description = "向量化状态：`init`、`doing`、`failed`、`success`")
    private String embedStatus;

    @Schema(description = "向量化时间")
    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    private LocalDateTime embedTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    private LocalDateTime createTime;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileInfoVo {

        @Schema(description = "文件key")
        private String fileKey;

        @Schema(description = "原始文件名")
        private String originalFilename;
    }

}
