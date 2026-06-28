package ai.myagent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档表
 * <p>
 * t_knowledge_doc
 *
 * @mbg.generated
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDoc {
    /**
     * 文档ID
     * <p>
     * t_knowledge_doc.id
     */
    private String id;

    /**
     * 知识库ID
     * <p>
     * t_knowledge_doc.knowledge_id
     */
    private String knowledgeId;

    /**
     * 文档类型：file、text
     * <p>
     * t_knowledge_doc.type
     */
    private String type;

    /**
     * 文档内容
     * <p>
     * t_knowledge_doc.content
     */
    private String content;

    /**
     * 向量化状态：`init`、`doing`、`failed`、`success`
     * <p>
     * t_knowledge_doc.embed_status
     */
    private String embedStatus;

    /**
     * 向量ID列表，json格式
     * <p>
     * t_knowledge_doc.embed_ids
     */
    private String embedIds;

    /**
     * 向量化时间
     * <p>
     * t_knowledge_doc.embed_time
     */
    private Long embedTime;

    /**
     * 创建时间
     * <p>
     * t_knowledge_doc.create_time
     */
    private Long createTime;

    /**
     * 删除时间
     * <p>
     * t_knowledge_doc.delete_time
     */
    private Long deleteTime;
}