package ai.myagent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author yulewei
 * @since 2026/6/29
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocQuery {

    /**
     * 文档ID
     */
    private String id;

    /**
     * 知识库ID
     */
    private String knowledgeId;
}
