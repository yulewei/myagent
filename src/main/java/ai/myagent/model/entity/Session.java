package ai.myagent.model.entity;

import ai.myagent.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户会话表
 *
 * t_session
 *
 * @mbg.generated
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Session {
    /**
     * 会话ID
     * <p>
     * t_session.id
     */
    private String id;

    /**
     * 模型ID
     * <p>
     * t_session.model_id
     */
    private String modelId;

    /**
     * 工具列表
     * <p>
     * t_session.tools
     */
    private String tools;

    /**
     * 会话标题
     * <p>
     * t_session.title
     */
    private String title;

    /**
     * 创建时间
     * <p>
     * t_session.create_time
     */
    private Long createTime;

    /**
     * 更新时间
     * <p>
     * t_session.update_time
     */
    private Long updateTime;

    /**
     * 删除时间
     * <p>
     * t_session.delete_time
     */
    private Long deleteTime;

    public List<String> getToolList() {
        return JsonUtils.parseArray(tools, String.class);
    }
}