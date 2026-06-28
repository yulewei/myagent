package ai.myagent.service.impl;

import ai.myagent.common.BizException;
import ai.myagent.constant.EmbedStatusEnum;
import ai.myagent.constant.KnowledgeDocEnum;
import ai.myagent.convert.KnowledgeConverter;
import ai.myagent.model.dto.EmbeddingModelDto;
import ai.myagent.model.dto.FileInfo;
import ai.myagent.model.dto.KnowledgeDocDto;
import ai.myagent.model.entity.Knowledge;
import ai.myagent.model.vo.KnowledgeDocResp;
import ai.myagent.model.vo.KnowledgeNewReq;
import ai.myagent.model.vo.KnowledgeResp;
import ai.myagent.model.vo.KnowledgeUpdateReq;
import ai.myagent.repo.KnowledgeRepo;
import ai.myagent.service.ConfigService;
import ai.myagent.service.KnowledgeService;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.io.FilenameUtils.EXTENSION_SEPARATOR;


/**
 * 知识库服务实现类
 *
 * @author yulewei
 * @since 2026/6/28
 */
@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    @Value("${myagent.data-dir}")
    private String dataDir;
    @Value("${myagent.knowledge.upload-file-dir:files}")
    private String uploadFileDir;
    @Value("${myagent.knowledge.vector-db-file:vectordb.json}")
    private String vectorDbFile;

    @Resource
    private ConfigService configService;
    @Resource
    private SimpleVectorStore vectorStore;
    @Resource
    private KnowledgeRepo knowledgeRepo;
    @Lazy
    @Resource
    private KnowledgeService knowledgeService;

    @PreDestroy
    public void destroy() {
        log.info("destroy KnowledgeService");
        saveVectorStore();
    }

    private void saveVectorStore() {
        if (vectorStore != null) {
            dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
            File file = new File(dataDir + vectorDbFile);
            vectorStore.save(file);
            log.info("保存向量数据库文件：{}", file.getAbsolutePath());
        }
    }

    /**
     * 查询知识库列表（按更新时间倒序）
     */
    @Override
    public List<KnowledgeResp> queryKnowledgeList() {
        List<Knowledge> knowledgeList = knowledgeRepo.queryAllList();
        return knowledgeList.stream().map(KnowledgeConverter.INSTANCE::toVo).toList();
    }

    /**
     * 查询知识库详情
     */
    @Override
    public KnowledgeResp queryKnowledge(String knowledgeId) {
        Knowledge knowledge = knowledgeRepo.queryKnowledge(knowledgeId);
        if (knowledge == null) {
            return null;
        }
        List<KnowledgeDocDto> dtoList = knowledgeRepo.queryDocList(knowledgeId);
        return KnowledgeConverter.INSTANCE.toVo(knowledge, dtoList);
    }

    /**
     * 新增知识库
     */
    @Override
    public String newKnowledge(KnowledgeNewReq request) {
        if (StringUtils.isNotBlank(request.getId())) {
            Knowledge record = knowledgeRepo.queryKnowledge(request.getId());
            if (record != null) {
                throw new BizException("知识库ID已存在");
            }
        }
        return knowledgeRepo.insertKnowledge(request);
    }

    /**
     * 更新知识库
     */
    @Override
    public void updateKnowledge(KnowledgeUpdateReq request) {
        knowledgeRepo.updateKnowledge(request);
    }

    /**
     * 删除知识库
     */
    @Override
    @Transactional
    public void deleteKnowledge(String knowledgeId) {
        List<KnowledgeDocDto> docList = knowledgeRepo.queryDocList(knowledgeId);
        for (KnowledgeDocDto doc : docList) {
            // 删除知识库文档
            if (Objects.equals(doc.getType(), KnowledgeDocEnum.FILE.getCode())) {
                dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
                uploadFileDir = uploadFileDir.endsWith(File.separator) ? uploadFileDir : uploadFileDir + File.separator;
                String filePath = dataDir + uploadFileDir + doc.getFileInfo().getFileKey();
                File file = new File(filePath);
                if (file.exists()) {
                    log.info("删除文件：{}", filePath);
                    file.delete();
                }
            }
            if (doc.getEmbedIds() != null && !doc.getEmbedIds().isEmpty()) {
                // 删除向量数据库中已有的向量
                vectorStore.delete(doc.getEmbedIds());
            }
        }
        knowledgeRepo.deleteDocByKnowledgeId(knowledgeId);
        knowledgeRepo.deleteKnowledge(knowledgeId);
    }

    /**
     * 查询知识库文档列表
     */
    @Override
    public List<KnowledgeDocResp> queryDocList(String knowledgeId) {
        List<KnowledgeDocDto> dtoList = knowledgeRepo.queryDocList(knowledgeId);
        return KnowledgeConverter.INSTANCE.toVoList(dtoList);
    }

    /**
     * 查询知识库文档详情
     */
    @Override
    public KnowledgeDocResp queryDoc(String knowledgeId, String docId) {
        KnowledgeDocDto doc = knowledgeRepo.queryDoc(knowledgeId, docId);
        return KnowledgeConverter.INSTANCE.toVo(doc);
    }

    /**
     * 上传文本到知识库
     */
    @Override
    public String uploadDocText(String knowledgeId, String content) {
        Knowledge knowledge = knowledgeRepo.queryKnowledge(knowledgeId);
        if (knowledge == null) {
            throw new BizException("知识库不存在");
        }
        if (vectorStore == null) {
            throw new BizException("向量化模型未配置");
        }
        EmbeddingModelDto dto = configService.queryEmbeddingModel();
        if (knowledge.getModelId() == null) {
            knowledgeRepo.updateKnowledge(knowledgeId, dto.getProviderId(), dto.getModelId());
        } else if (!Objects.equals(knowledge.getProviderId(), dto.getProviderId())
                || !Objects.equals(knowledge.getModelId(), dto.getModelId())) {
            throw new BizException("知识库向量化模型与当前配置不一致");
        }
        String docId = knowledgeRepo.insertKnowledgeDocText(knowledgeId, content);
        // 异步执行，向量化文档
        knowledgeService.embedDoc(knowledgeId, docId);
        return docId;
    }

    /**
     * 上传文件到知识库
     */
    @SneakyThrows
    @Override
    public String uploadDocFile(String knowledgeId, byte[] bytes, String originalFilename) {
        Knowledge knowledge = knowledgeRepo.queryKnowledge(knowledgeId);
        if (knowledge == null) {
            throw new BizException("知识库不存在");
        }
        if (vectorStore == null) {
            throw new BizException("向量化模型未配置");
        }
        EmbeddingModelDto dto = configService.queryEmbeddingModel();
        if (knowledge.getModelId() == null) {
            knowledgeRepo.updateKnowledge(knowledgeId, dto.getProviderId(), dto.getModelId());
        } else if (!Objects.equals(knowledge.getProviderId(), dto.getProviderId())
                || !Objects.equals(knowledge.getModelId(), dto.getModelId())) {
            throw new BizException("知识库向量化模型与当前配置不一致");
        }

        String md5 = DigestUtils.md5Hex(bytes);
        String fileExt = FilenameUtils.getExtension(originalFilename);
        String fileKey = StringUtils.isEmpty(fileExt) ? md5 : (md5 + EXTENSION_SEPARATOR + fileExt);
        dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
        uploadFileDir = uploadFileDir.endsWith(File.separator) ? uploadFileDir : uploadFileDir + File.separator;
        String filePath = dataDir + uploadFileDir + fileKey;
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            log.warn("相同文件已存在：{}", filePath);
            throw new BizException("相同文件已存在");
        }
        FileUtils.writeByteArrayToFile(new File(filePath), bytes);
        log.info("上传文件，知识库ID：{}，文件名：{}，原始文件名：{}", knowledgeId, fileKey, originalFilename);
        FileInfo fileInfo = FileInfo.builder().fileKey(fileKey).originalFilename(originalFilename).build();
        String docId = knowledgeRepo.insertKnowledgeDocFile(knowledgeId, fileInfo);
        // 异步执行，向量化文档
        knowledgeService.embedDoc(knowledgeId, docId);
        return docId;
    }

    /**
     * 删除知识库文档
     */
    @Override
    public void deleteDoc(String knowledgeId, String docId) {
        KnowledgeDocDto doc = knowledgeRepo.queryDoc(knowledgeId, docId);
        if (doc == null) {
            log.warn("文档不存在：{}", docId);
            return;
        }
        if (Objects.equals(doc.getType(), KnowledgeDocEnum.FILE.getCode())) {
            dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
            uploadFileDir = uploadFileDir.endsWith(File.separator) ? uploadFileDir : uploadFileDir + File.separator;
            String filePath = dataDir + uploadFileDir + doc.getFileInfo().getFileKey();
            File file = new File(filePath);
            if (file.exists()) {
                log.info("删除文件：{}", filePath);
                file.delete();
            }
        }
        if (doc.getEmbedIds() != null && !doc.getEmbedIds().isEmpty()) {
            // 删除向量数据库中已有的向量
            vectorStore.delete(doc.getEmbedIds());
        }
        knowledgeRepo.deleteDoc(docId);
    }

    /**
     * 向量化知识库文档（若已经向量化，则重新计算）
     */
    @Async
    public void embedDoc(String knowledgeId, String docId) {
        KnowledgeDocDto doc = knowledgeRepo.queryDoc(knowledgeId, docId);
        log.info("向量化文档：{}, {}", docId, JsonUtils.toJsonStr(doc));
        if (doc == null) {
            log.warn("文档不存在：{}", docId);
            return;
        }
        if (Objects.equals(doc.getEmbedStatus(), EmbedStatusEnum.DOING.getCode())) {
            log.warn("文档向量化进行中：{}", docId);
            return;
        }

        List<Document> documents;
        if (Objects.equals(doc.getType(), KnowledgeDocEnum.FILE.getCode())) {
            dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
            uploadFileDir = uploadFileDir.endsWith(File.separator) ? uploadFileDir : uploadFileDir + File.separator;
            String filePath = dataDir + uploadFileDir + doc.getFileInfo().getFileKey();
            TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(filePath));
            documents = reader.get();
        } else {
            documents = List.of(new Document(doc.getTextContent()));
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder().build();
        List<Document> splitDocuments = splitter.apply(documents);
        log.info("向量化文档：{}，分割成 {} 个 chunk", docId, splitDocuments.size());
        for (Document document : splitDocuments) {
            document.getMetadata().put("knowledgeId", doc.getKnowledgeId());
            document.getMetadata().put("docId", docId);
        }

        try {
            vectorStore.add(splitDocuments);
            if (doc.getEmbedIds() != null && !doc.getEmbedIds().isEmpty()) {
                // 重新计算向量成功，删除向量数据库中已有的向量
                vectorStore.delete(doc.getEmbedIds());
            }

            // 更新向量化状态，成功
            List<String> ids = splitDocuments.stream().map(Document::getId).toList();
            knowledgeRepo.updateDocEmbedStatus(docId, EmbedStatusEnum.SUCCESS, ids);

            // 保存向量数据库
            this.saveVectorStore();
        } catch (Exception e) {
            log.error("向量化文档失败：{} {}", docId, JsonUtils.toJsonStr(doc), e);
            // 更新向量化状态，失败
            knowledgeRepo.updateDocEmbedStatus(docId, EmbedStatusEnum.FAILED, null);
        }
    }

    /**
     * 下载知识库文件
     */
    @SneakyThrows
    @Override
    public byte[] downloadDocFile(String fileKey) {
        dataDir = dataDir.endsWith(File.separator) ? dataDir : dataDir + File.separator;
        uploadFileDir = uploadFileDir.endsWith(File.separator) ? uploadFileDir : uploadFileDir + File.separator;
        String filePath = dataDir + uploadFileDir + fileKey;
        File file = new File(filePath);
        return FileUtils.readFileToByteArray(file);
    }
}
