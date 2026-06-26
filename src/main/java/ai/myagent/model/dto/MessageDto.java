package ai.myagent.model.dto;

import ai.myagent.convert.MessageConverter;
import ai.myagent.model.vo.MessageResp;
import ai.myagent.support.MessageTypeDeserializer;
import ai.myagent.support.MessageTypeSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;

import java.util.List;
import java.util.Map;

/**
 * Spring AI {@link Message} 及其子类序列化后对应的数据结构
 *
 * @author yulewei
 * @see UserMessage
 * @see SystemMessage
 * @see AssistantMessage
 * @see ToolResponseMessage
 * @since 2026/6/23
 */
@Data
public class MessageDto implements Message {

    /**
     * 消息ID
     */
    @JsonIgnore
    String id;

    /**
     * 父消息ID
     */
    @JsonIgnore
    String parentId;

    /**
     * 会话ID
     */
    @JsonIgnore
    String sessionId;

    /**
     * 模型ID
     */
    @JsonIgnore
    String modelId;

    /**
     * 消息创建时间
     */
    @JsonIgnore
    Long createTime;

    /**
     * 消息类型
     */
    @JsonSerialize(using = MessageTypeSerializer.class)
    @JsonDeserialize(using = MessageTypeDeserializer.class)
    MessageType messageType;

    /**
     * 消息内容
     */
    String text;

    /**
     * 消息元数据
     */
    Map<String, Object> metadata;

    /**
     * 媒体列表
     * <p>
     * {@link UserMessage} 和 {@link AssistantMessage} 特有字段
     */
    List<Media> media;

    /**
     * 推理内容
     * <p>
     * {@link AssistantMessage} 特有字段
     */
    String reasoningContent;

    /**
     * 工具调用列表
     * <p>
     * {@link AssistantMessage} 特有字段
     */
    List<AssistantMessage.ToolCall> toolCalls;

    /**
     * 工具响应列表
     * <p>
     * {@link ToolResponseMessage} 特有字段
     */
    List<ToolResponseMessage.ToolResponse> responses;

    public MessageResp toVo() {
        return MessageConverter.INSTANCE.toVo(this);
    }

    public Message toChatMessage() {
        return switch (messageType) {
            case USER -> UserMessage.builder()
                    .text(text)
                    .metadata(metadata)
                    .media(ObjectUtils.getIfNull(media, List.of()))
                    .build();
            case SYSTEM -> SystemMessage.builder()
                    .text(text)
                    .metadata(metadata)
                    .build();
            case ASSISTANT -> DeepSeekAssistantMessage.builder()
                    .reasoningContent(reasoningContent)
                    .content(text)
                    .toolCalls(toolCalls)
                    .media(ObjectUtils.getIfNull(media, List.of()))
                    .build();
            case TOOL -> ToolResponseMessage.builder()
                    .responses(responses)
                    .metadata(metadata)
                    .build();
        };
    }

    public static MessageDto.Builder builder() {
        return new MessageDto.Builder();
    }

    public static final class Builder {
        String id;
        String parentId;
        String sessionId;
        String modelId;
        Message message;
        Long createTime;

        private Builder() {
        }

        public MessageDto.Builder id(String id) {
            this.id = id;
            return this;
        }

        public MessageDto.Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public MessageDto.Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public MessageDto.Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public MessageDto.Builder message(Message message) {
            this.message = message;
            return this;
        }

        public MessageDto.Builder createTime(Long createTime) {
            this.createTime = createTime;
            return this;
        }

        public MessageDto build() {
            MessageDto dto = new MessageDto();
            dto.setId(this.id);
            dto.setParentId(this.parentId);
            dto.setSessionId(this.sessionId);
            dto.setModelId(this.modelId);
            dto.setMessageType(this.message.getMessageType());
            dto.setText(this.message.getText());
            dto.setMetadata(this.message.getMetadata());
            dto.setCreateTime(this.createTime != null ? this.createTime : System.currentTimeMillis());
            if (message instanceof UserMessage msg) {
                dto.setMedia(msg.getMedia());
            } else if (message instanceof AssistantMessage assistantMessage) {
                if (assistantMessage instanceof DeepSeekAssistantMessage msg) {
                    dto.setReasoningContent(msg.getReasoningContent());
                }
                dto.setToolCalls(assistantMessage.getToolCalls());
            } else if (message instanceof ToolResponseMessage msg) {
                dto.setResponses(msg.getResponses());
            }
            return dto;
        }
    }
}
