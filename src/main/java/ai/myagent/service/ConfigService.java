package ai.myagent.service;

import ai.myagent.model.dto.AgentConfig;
import ai.myagent.model.dto.ChatModelDto;
import ai.myagent.model.vo.ModelResp;
import ai.myagent.model.vo.SkillsVo;

import java.util.List;

/**
 * 配置服务
 *
 * @author yulewei
 */
public interface ConfigService {

    /**
     * 重新加载配置
     */
    void reloadConfig();

    /**
     * 修改默认模型
     */
    void switchDefaultModel(String modelId);

    /**
     * 查询会话默认配置
     */
    AgentConfig.SessionDefaultConfig querySessionDefault();

    /**
     * 查询模型
     */
    ChatModelDto queryModel(String modelId);

    /**
     * 查询模型列表
     */
    List<ChatModelDto> queryModelList();

    /**
     * 查询模型列表
     */
    List<ModelResp> queryModelVoList();

    /**
     * 查询可加载的Skills列表
     */
    List<SkillsVo> querySkillList();
}
