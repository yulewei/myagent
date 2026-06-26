package ai.myagent.mapper;

import ai.myagent.model.entity.SessionMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SessionMessageMapper extends BaseMapper<SessionMessage> {

    List<SessionMessage> queryAllList(String sessionId);

    List<SessionMessage> queryListDesc(String sessionId, Integer limit);

    SessionMessage queryNextUserMessage(String sessionId, String userMessageId);

    List<SessionMessage> queryByTimeRange(String sessionId, Long startTime, Long endTime);

    void batchInsert(List<SessionMessage> list);
}