package ai.myagent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户会话消息表
 *
 * t_session_message
 *
 * @mbg.generated
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionMessage {
    /**
     * 消息ID
     * <p>
     * t_session_message.id
     */
    private String id;

    /**
     * 父消息ID
     * <p>
     * t_session_message.parent_id
     */
    private String parentId;

    /**
     * 会话ID
     * <p>
     * t_session_message.session_id
     */
    private String sessionId;

    /**
     * 模型ID
     * <p>
     * t_session_message.model_id
     */
    private String modelId;

    /**
     * 消息类型：user、assistant、system、tool
     * <p>
     * t_session_message.type
     */
    private String type;

    /**
     * 消息内容，json格式
     * <p>
     * t_session_message.json
     */
    private String json;

    /**
     * 消息创建时间
     * <p>
     * t_session_message.create_time
     */
    private Long createTime;
}