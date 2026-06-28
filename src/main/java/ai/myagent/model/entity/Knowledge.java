package ai.myagent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库表
 * <p>
 * t_knowledge
 *
 * @mbg.generated
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Knowledge {
    /**
     * 知识库ID
     * <p>
     * t_knowledge.id
     */
    private String id;

    /**
     * 知识库名称
     * <p>
     * t_knowledge.name
     */
    private String name;

    /**
     * 提供商ID
     *
     * t_knowledge.provider_id
     */
    private String providerId;

    /**
     * 向量化模型ID
     * <p>
     * t_knowledge.model_id
     */
    private String modelId;


    /**
     * 创建时间
     * <p>
     * t_knowledge.create_time
     */
    private Long createTime;

    /**
     * 更新时间
     * <p>
     * t_knowledge.update_time
     */
    private Long updateTime;

    /**
     * 删除时间
     * <p>
     * t_knowledge.delete_time
     */
    private Long deleteTime;
}