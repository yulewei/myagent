package ai.myagent.convert;


import ai.myagent.model.entity.Session;
import ai.myagent.model.vo.SessionResp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/20
 */
@Mapper(uses = DateMapper.class)
public interface SessionConverter {

    SessionConverter INSTANCE = Mappers.getMapper(SessionConverter.class);

    @Mapping(target = "tools", source = "toolList")
    SessionResp toVo(Session session);

    List<SessionResp> toVoList(List<Session> list);

}
