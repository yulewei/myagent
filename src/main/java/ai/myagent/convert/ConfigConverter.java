package ai.myagent.convert;

import ai.myagent.constant.Tools;
import ai.myagent.model.vo.ToolInfoResp;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author yulewei
 * @since 2026/6/22
 */
@Mapper
public interface ConfigConverter {
    ConfigConverter INSTANCE = Mappers.getMapper(ConfigConverter.class);

    List<ToolInfoResp> toToolVoList(List<Tools.ToolInfo> list);

}
