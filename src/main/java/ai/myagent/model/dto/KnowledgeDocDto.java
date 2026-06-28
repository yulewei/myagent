package ai.myagent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocDto {

    /**
     * 文档ID
     */
    private String id;
    /**
     * 知识库ID
     */
    private String knowledgeId;

    /**
     * 文档类型：file、text
     */
    private String type;

    /**
     * file类型的文档信息
     */
    private FileInfo fileInfo;

    /**
     * text类型的文档内容
     */
    private String textContent;

    /**
     * 向量化状态：`init`、`doing`、`failed`、`success`
     */
    private String embedStatus;

    /**
     * 向量ID列表，
     */
    private List<String> embedIds;

    /**
     * 向量化时间
     */
    private LocalDateTime embedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
