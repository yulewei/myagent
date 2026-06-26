package ai.myagent.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ai.myagent.util.DateUtils.ISO_DATETIME_EXT_PATTERN;

/**
 * @author yulewei
 * @since 2026/6/20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionPageReq {
    @Size(max = 10, message = "会话标题搜索关键字不能超过10个字符")
    @Schema(description = "会话标题（全文模糊查询）")
    private String title;

    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    @Schema(description = "响应消息的更新时间必须小于该值，用于分页")
    private LocalDateTime lastUpdateTime;

    @Schema(description = "响应数量限制，默认10")
    private Integer limit;
}
