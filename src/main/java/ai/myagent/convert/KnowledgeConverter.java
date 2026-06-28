package ai.myagent.convert;

import ai.myagent.constant.KnowledgeDocEnum;
import ai.myagent.model.dto.FileInfo;
import ai.myagent.model.dto.KnowledgeDocDto;
import ai.myagent.model.entity.Knowledge;
import ai.myagent.model.entity.KnowledgeDoc;
import ai.myagent.model.vo.KnowledgeDocResp;
import ai.myagent.model.vo.KnowledgeResp;
import ai.myagent.util.DateUtils;
import ai.myagent.util.JsonUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Mapper(uses = DateMapper.class)
public interface KnowledgeConverter {
    KnowledgeConverter INSTANCE = Mappers.getMapper(KnowledgeConverter.class);

    default KnowledgeDocDto toDto(KnowledgeDoc doc) {
        FileInfo fileInfo = null;
        if (doc.getType().equals(KnowledgeDocEnum.FILE.getCode())) {
            fileInfo = JsonUtils.parse(doc.getContent(), FileInfo.class);
        }
        String textContent = null;
        if (doc.getType().equals(KnowledgeDocEnum.TEXT.getCode())) {
            textContent = doc.getContent();
        }
        return KnowledgeDocDto.builder()
                .id(doc.getId())
                .knowledgeId(doc.getKnowledgeId())
                .type(doc.getType())
                .fileInfo(fileInfo)
                .textContent(textContent)
                .embedStatus(doc.getEmbedStatus())
                .embedTime(DateUtils.toLocalDateTime(doc.getEmbedTime()))
                .embedIds(JsonUtils.parseArray(doc.getEmbedIds(), String.class))
                .createTime(DateUtils.toLocalDateTime(doc.getCreateTime()))
                .build();
    }

   default KnowledgeResp toVo(Knowledge knowledge, List<KnowledgeDocDto> dtoList) {
        return KnowledgeResp.builder()
                .id(knowledge.getId())
                .name(knowledge.getName())
                .createTime(DateUtils.toLocalDateTime(knowledge.getCreateTime()))
                .updateTime(DateUtils.toLocalDateTime(knowledge.getUpdateTime()))
                .docList(toVoList(dtoList))
                .build();
   }

    KnowledgeDocResp toVo(KnowledgeDocDto dto);

    List<KnowledgeDocResp> toVoList(List<KnowledgeDocDto> dtoList);
}
