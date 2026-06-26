package ai.myagent.mapper;

import ai.myagent.model.entity.Session;
import ai.myagent.model.vo.SessionPageReq;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    List<Session> querySessionPage(SessionPageReq req);
}