package ai.myagent.mapper;

import ai.myagent.model.dto.KnowledgeDocQuery;
import ai.myagent.model.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper extends BaseMapper<KnowledgeDoc> {

    List<KnowledgeDoc> queryList(KnowledgeDocQuery query);

    void updateByCondition(KnowledgeDocQuery query, KnowledgeDoc record);
}