package ai.myagent.mapper;

import ai.myagent.model.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    List<Knowledge> queryAllList();
}