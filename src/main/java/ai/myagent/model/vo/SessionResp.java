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
 * @since 2026/6/20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionResp {

    @Schema(description = "会话ID")
    private String id;

    @Schema(description = "模型ID")
    private String modelId;

    @Schema(description = "工具列表")
    private List<String> tools;

    @Schema(description = "会话标题")
    private String title;

    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
