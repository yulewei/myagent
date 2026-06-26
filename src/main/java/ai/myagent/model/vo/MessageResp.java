package ai.myagent.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.time.LocalDateTime;
import java.util.List;

import static ai.myagent.util.DateUtils.ISO_DATETIME_EXT_PATTERN;

/**
 * 会话消息响应
 *
 * @author yulewei
 * @since 2026/6/22
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageResp {
    @Schema(description = "消息ID")
    String id;

    @Schema(description = "父消息ID")
    String parentId;

    @Schema(description = "会话ID")
    String sessionId;

    @Schema(description = "模型ID")
    String modelId;

    @JsonFormat(pattern = ISO_DATETIME_EXT_PATTERN, timezone = "GMT+8")
    @Schema(description = "消息创建时间")
    LocalDateTime createTime;

    @Schema(description = "消息类型")
    String messageType;

    @Schema(description = "消息内容")
    String text;

    /**
     * {@link AssistantMessage} 特有字段
     */
    @Schema(description = "推理内容")
    String reasoningContent;

    /**
     * {@link AssistantMessage} 特有字段
     */
    @Schema(description = "工具调用列表")
    List<ToolCall> toolCalls;

    /**
     * {@link ToolResponseMessage} 特有字段
     */
    @Schema(description = "工具响应列表")
    List<ToolResp> toolResponses;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolCall {
        String type;
        String name;
        String arguments;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolResp {
        String name;
        String responseData;
    }
}
