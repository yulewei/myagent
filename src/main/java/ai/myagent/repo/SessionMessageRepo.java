package ai.myagent.repo;

import ai.myagent.convert.MessageConverter;
import ai.myagent.mapper.SessionMessageMapper;
import ai.myagent.model.dto.MessageDto;
import ai.myagent.model.entity.SessionMessage;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/19
 */
@Repository
public class SessionMessageRepo {
    @Resource
    SessionMessageMapper sessionMessageMapper;

    public List<MessageDto> queryAllList(String sessionId) {
        List<SessionMessage> list = sessionMessageMapper.queryAllList(sessionId);
        return MessageConverter.INSTANCE.poToDtoList(list);
    }

    public List<MessageDto> queryMessageListByUserMessageId(String sessionId, String userMessageId) {
        SessionMessage userMessage = sessionMessageMapper.selectByPrimaryKey(userMessageId);
        if (userMessage == null || !userMessage.getSessionId().equals(sessionId)
                || !userMessage.getType().equals(MessageType.USER.getValue())) {
            return List.of();
        }
        SessionMessage nextUserMessage = sessionMessageMapper.queryNextUserMessage(sessionId, userMessageId);
        Long startTime = userMessage.getCreateTime();
        Long endTime = nextUserMessage != null ? nextUserMessage.getCreateTime() : System.currentTimeMillis();
        List<SessionMessage> list = sessionMessageMapper.queryByTimeRange(sessionId, startTime, endTime);
        return MessageConverter.INSTANCE.poToDtoList(list);
    }

    public MessageDto queryLastMessage(String sessionId) {
        List<SessionMessage> list = sessionMessageMapper.queryListDesc(sessionId, 1);
        if (list.isEmpty()) {
            return null;
        }
        return MessageConverter.INSTANCE.toDto(list.get(0));
    }

    public void batchInsert(List<MessageDto> list) {
        List<SessionMessage> poList = MessageConverter.INSTANCE.toPoList(list);
        sessionMessageMapper.batchInsert(poList);
    }

    public void insert(MessageDto message) {
        SessionMessage po = MessageConverter.INSTANCE.toPo(message);
        sessionMessageMapper.insert(po);
    }
}
