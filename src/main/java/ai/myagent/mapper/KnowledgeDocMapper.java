package ai.myagent.mapper;

import ai.myagent.model.dto.KnowledgeDocDto;
import ai.myagent.model.entity.KnowledgeDoc;
import ai.myagent.model.vo.KnowledgeDocResp;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper extends BaseMapper<KnowledgeDoc> {

    List<KnowledgeDoc> queryDocList(String knowledgeId);
}