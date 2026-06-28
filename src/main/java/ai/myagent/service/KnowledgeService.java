package ai.myagent.service;

import ai.myagent.model.vo.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/28
 */
public interface KnowledgeService {

    KnowledgeResp queryKnowledge(String knowledgeId);

    String newKnowledge(KnowledgeNewReq request);

    void updateKnowledge(KnowledgeUpdateReq request);

    void deleteKnowledge(String knowledgeId);

    /**
     * 查询知识库文档列表
     */
    List<KnowledgeDocResp> queryDocList(String knowledgeId);

    KnowledgeDocResp queryDoc(String knowledgeId, String docId);

    /**
     * 上传文本到知识库
     */
    String uploadDocText(String knowledgeId, String content);

    /**
     * 上传文件到知识库
     */
    String uploadDocFile(String knowledgeId, byte[] bytes, String originalFilename);

    /**
     * 下载知识库文件
     */
    byte[] downloadDocFile(String fileKey);

    /**
     * 删除知识库文档
     */
    void deleteDoc(String knowledgeId, String docId);

    /**
     * 向量化知识库文档（若已经向量化，则重新计算）
     */
    void embedDoc(String knowledgeId, String docId);

}
