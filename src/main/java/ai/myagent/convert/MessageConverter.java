package ai.myagent.convert;

import ai.myagent.model.dto.MessageDto;
import ai.myagent.model.entity.SessionMessage;
import ai.myagent.model.vo.MessageResp;
import ai.myagent.util.JsonUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.Arrays;
import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/22
 */
@Mapper(uses = DateMapper.class)
public interface MessageConverter {
    MessageConverter INSTANCE = Mappers.getMapper(MessageConverter.class);

    default MessageDto toDto(SessionMessage message) {
        if (message == null) {
            return null;
        }
        MessageDto dto = JsonUtils.parse(message.getJson(), MessageDto.class);
        dto.setId(message.getId());
        dto.setParentId(message.getParentId());
        dto.setSessionId(message.getSessionId());
        dto.setModelId(message.getModelId());
        return dto;
    }

    default SessionMessage toPo(MessageDto dto) {
        if (dto == null) {
            return null;
        }
        return SessionMessage.builder()
                .id(dto.getId())
                .parentId(dto.getParentId())
                .sessionId(dto.getSessionId())
                .modelId(dto.getModelId())
                .type(dto.getMessageType().getValue())
                .json(JsonUtils.toJsonStr(dto))
                .createTime(dto.getCreateTime())
                .build();
    }

    default MessageType convert(String messageType) {
        return Arrays.stream(MessageType.values())
                .filter(type -> type.getValue().equalsIgnoreCase(messageType))
                .findFirst().orElse(null);
    }

    List<MessageDto> poToDtoList(List<SessionMessage> list);

    List<SessionMessage> toPoList(List<MessageDto> list);

    List<MessageResp> toVoList(List<MessageDto> list);

    @Mapping(target = "messageType", expression = "java(dto.getMessageType().getValue())")
    @Mapping(target = "toolResponses", source = "responses")
    MessageResp toVo(MessageDto dto);

    MessageResp.ToolResp toVo(ToolResponseMessage.ToolResponse toolResponse);

    MessageResp.ToolCall toVo(AssistantMessage.ToolCall toolCall);

}
