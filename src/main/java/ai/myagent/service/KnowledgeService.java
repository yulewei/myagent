package ai.myagent.service;

import ai.myagent.model.vo.KnowledgeDocResp;
import ai.myagent.model.vo.KnowledgeNewReq;
import ai.myagent.model.vo.KnowledgeResp;
import ai.myagent.model.vo.KnowledgeUpdateReq;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/28
 */
public interface KnowledgeService {

    /**
     * 查询知识库列表（按更新时间倒序）
     */
    List<KnowledgeResp> queryKnowledgeList();

    /**
     * 查询知识库详情
     */
    KnowledgeResp queryKnowledge(String knowledgeId);

    /**
     * 新建知识库
     */
    String newKnowledge(KnowledgeNewReq request);

    /**
     * 修改知识库
     */
    void updateKnowledge(KnowledgeUpdateReq request);

    /**
     * 删除知识库
     */
    void deleteKnowledge(String knowledgeId);

    /**
     * 查询知识库文档列表
     */
    List<KnowledgeDocResp> queryDocList(String knowledgeId);

    /**
     * 查询知识库文档详情
     */
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
