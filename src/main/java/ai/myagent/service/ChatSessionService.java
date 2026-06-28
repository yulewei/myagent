package ai.myagent.service;

import ai.myagent.model.vo.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * 会话服务
 *
 * @author yulewei
 */
public interface ChatSessionService {

    /**
     * 查询会话分页列表
     */
    List<SessionResp> querySessionVoPage(SessionPageReq req);

    /**
     * 查询会话详情
     */
    SessionResp querySessionVo(String sessionId);

    /**
     * 新建会话
     */
    SessionResp newSession(SessionNewReq request);

    /**
     * 修改会话，包括标题和模型
     */
    void updateSession(SessionUpdateReq request);

    /**
     * 删除会话
     */
    void deleteSession(String sessionId);

    /**
     * 在会话中聊天
     */
    List<MessageResp> sessionChat(MessageReq request);

    /**
     * 在会话中聊天
     */
    void sessionChatStream(MessageReq request, Consumer<StreamEventVo> consumer);

    /**
     * 查询会话全部历史消息列表（按时间正序）
     */
    List<MessageResp> queryAllMessageList(String sessionId);

    /**
     * 查询会话中某用户消息及其对应生成的响应消息列表（按时间正序）
     */
    List<MessageResp> queryMessageListByUserMessageId(String sessionId, String userMessageId);

}
