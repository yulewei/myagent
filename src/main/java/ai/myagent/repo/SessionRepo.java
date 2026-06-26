package ai.myagent.repo;

import ai.myagent.convert.SessionConverter;
import ai.myagent.mapper.SessionMapper;
import ai.myagent.model.entity.Session;
import ai.myagent.model.vo.SessionNewReq;
import ai.myagent.model.vo.SessionPageReq;
import ai.myagent.model.vo.SessionResp;
import ai.myagent.util.IdUtils;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/19
 */
@Repository
public class SessionRepo {

    @Resource
    SessionMapper sessionMapper;

    public List<SessionResp> querySessionVoPage(SessionPageReq req) {
        req.setLimit(ObjectUtils.getIfNull(req.getLimit(), 10));
        List<Session> list = sessionMapper.querySessionPage(req);
        return SessionConverter.INSTANCE.toVoList(list);
    }

    public SessionResp querySessionVo(String sessionId) {
        Session session = sessionMapper.selectByPrimaryKey(sessionId);
        if (session.getDeleteTime() != null) {
            return null;
        }
        return SessionConverter.INSTANCE.toVo(session);
    }

    public Session querySession(String sessionId) {
        Session session = sessionMapper.selectByPrimaryKey(sessionId);
        if (session == null || session.getDeleteTime() != null) {
            return null;
        }
        return session;
    }

    public String insert(SessionNewReq request) {
        if (StringUtils.isBlank(request.getSessionId())) {
            request.setSessionId(IdUtils.fastSimpleUUID());
        }
        List<String> tools = request.getTools();
        if (CollectionUtils.isEmpty(tools)) {
            tools = null;
        }
        Long now = System.currentTimeMillis();
        Session session = Session.builder()
                .id(request.getSessionId())
                .modelId(request.getModelId())
                .tools(tools != null ? JsonUtils.toJsonStr(tools) : null)
                .title(request.getTitle())
                .createTime(now)
                .updateTime(now)
                .build();
        sessionMapper.insertSelective(session);
        return session.getId();
    }

    public void deleteSession(String sessionId) {
        Session session = Session.builder()
                .id(sessionId)
                .deleteTime(System.currentTimeMillis())
                .build();
        sessionMapper.updateByPrimaryKeySelective(session);
    }

    public void updateSessionTitle(String sessionId, String title) {
        Session session = Session.builder()
                .id(sessionId)
                .title(title)
                .updateTime(System.currentTimeMillis())
                .build();
        sessionMapper.updateByPrimaryKeySelective(session);
    }

    public void updateSessionModel(String sessionId, String modelId) {
        Session session = Session.builder()
                .id(sessionId)
                .modelId(modelId)
                .updateTime(System.currentTimeMillis())
                .build();
        sessionMapper.updateByPrimaryKeySelective(session);
    }

    public Session getSessionById(String sessionId) {
        return sessionMapper.selectByPrimaryKey(sessionId);
    }
}
