package ai.myagent.repo;

import ai.myagent.constant.EmbedStatusEnum;
import ai.myagent.constant.KnowledgeDocEnum;
import ai.myagent.convert.KnowledgeConverter;
import ai.myagent.mapper.KnowledgeDocMapper;
import ai.myagent.mapper.KnowledgeMapper;
import ai.myagent.model.dto.FileInfo;
import ai.myagent.model.dto.KnowledgeDocDto;
import ai.myagent.model.dto.KnowledgeDocQuery;
import ai.myagent.model.entity.Knowledge;
import ai.myagent.model.entity.KnowledgeDoc;
import ai.myagent.model.vo.KnowledgeNewReq;
import ai.myagent.model.vo.KnowledgeUpdateReq;
import ai.myagent.util.IdUtils;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Repository
public class KnowledgeRepo {
    @Resource
    KnowledgeMapper knowledgeMapper;
    @Resource
    KnowledgeDocMapper knowledgeDocMapper;

    public String insertKnowledge(KnowledgeNewReq request) {
        if (StringUtils.isBlank(request.getId())) {
            request.setId(IdUtils.fastSimpleUUID());
        }
        Long now = System.currentTimeMillis();
        Knowledge record = Knowledge.builder()
                .id(request.getId())
                .name(request.getName())
                .createTime(now)
                .updateTime(now)
                .build();
        knowledgeMapper.insertSelective(record);
        return record.getId();
    }

    public void updateKnowledge(KnowledgeUpdateReq request) {
        Knowledge record = Knowledge.builder()
                .id(request.getId())
                .name(request.getName())
                .updateTime(System.currentTimeMillis())
                .build();
        knowledgeMapper.updateByPrimaryKeySelective(record);
    }

    public void deleteKnowledge(String knowledgeId) {
        Knowledge record = Knowledge.builder()
                .id(knowledgeId)
                .deleteTime(System.currentTimeMillis())
                .build();
        knowledgeMapper.updateByPrimaryKeySelective(record);
    }

    public void updateKnowledge(String knowledgeId, String providerId, String modelId) {
        Knowledge record = Knowledge.builder()
                .id(knowledgeId)
                .providerId(providerId)
                .modelId(modelId)
                .updateTime(System.currentTimeMillis())
                .build();
        knowledgeMapper.updateByPrimaryKeySelective(record);
    }

    public List<Knowledge> queryAllList() {
        return knowledgeMapper.queryAllList();
    }


    public Knowledge queryKnowledge(String knowledgeId) {
        Knowledge record = knowledgeMapper.selectByPrimaryKey(knowledgeId);
        if (record == null || record.getDeleteTime() != null) {
            return null;
        }
        return record;
    }

    public String insertKnowledgeDocText(String knowledgeId, String content) {
        Long now = System.currentTimeMillis();
        KnowledgeDoc doc = KnowledgeDoc.builder()
                .id(IdUtils.fastSimpleUUID())
                .knowledgeId(knowledgeId)
                .type(KnowledgeDocEnum.TEXT.getCode())
                .content(content)
                .embedStatus(EmbedStatusEnum.INIT.getCode())
                .createTime(now)
                .build();
        knowledgeDocMapper.insertSelective(doc);
        return doc.getId();
    }

    public String insertKnowledgeDocFile(String KnowledgeId, FileInfo fileInfo) {
        Long now = System.currentTimeMillis();
        KnowledgeDoc doc = KnowledgeDoc.builder()
                .id(IdUtils.fastSimpleUUID())
                .knowledgeId(KnowledgeId)
                .type(KnowledgeDocEnum.FILE.getCode())
                .content(JsonUtils.toJsonStr(fileInfo))
                .embedStatus(EmbedStatusEnum.INIT.getCode())
                .createTime(now)
                .build();
        knowledgeDocMapper.insertSelective(doc);
        return doc.getId();
    }

    public void deleteDoc(String docId) {
        KnowledgeDoc doc = KnowledgeDoc.builder()
                .id(docId)
                .deleteTime(System.currentTimeMillis())
                .build();
        knowledgeDocMapper.updateByPrimaryKeySelective(doc);
    }

    public void deleteDocByKnowledgeId(String knowledgeId) {
        KnowledgeDocQuery query = KnowledgeDocQuery.builder()
                .knowledgeId(knowledgeId)
                .build();
        KnowledgeDoc doc = KnowledgeDoc.builder()
                .deleteTime(System.currentTimeMillis())
                .build();
        knowledgeDocMapper.updateByCondition(query, doc);
    }

    public List<KnowledgeDocDto> queryDocList(String knowledgeId) {
        KnowledgeDocQuery query = KnowledgeDocQuery.builder()
                .knowledgeId(knowledgeId)
                .build();
        List<KnowledgeDoc> docList = knowledgeDocMapper.queryList(query);
        return docList.stream().map(KnowledgeConverter.INSTANCE::toDto).toList();
    }

    public KnowledgeDocDto queryDoc(String knowledgeId, String docId) {
        KnowledgeDoc doc = knowledgeDocMapper.selectByPrimaryKey(docId);
        if (doc == null || !knowledgeId.equals(doc.getKnowledgeId()) || doc.getDeleteTime() != null) {
            return null;
        }
        return KnowledgeConverter.INSTANCE.toDto(doc);
    }

    public boolean updateDocEmbedStatus(String docId, EmbedStatusEnum embedStatus, List<String> ids) {
        KnowledgeDoc doc = knowledgeDocMapper.selectByPrimaryKey(docId);
        if (doc == null || doc.getDeleteTime() != null) {
            return false;
        }
        doc.setEmbedStatus(embedStatus.getCode());
        doc.setEmbedIds(ids != null ? JsonUtils.toJsonStr(ids) : null);
        doc.setEmbedTime(Objects.equals(embedStatus, EmbedStatusEnum.SUCCESS) ? System.currentTimeMillis() : null);
        return knowledgeDocMapper.updateByPrimaryKey(doc) > 0;
    }


}
