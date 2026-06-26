package ai.myagent.service;

import ai.myagent.AgentApplication;
import ai.myagent.model.vo.MessageResp;
import ai.myagent.model.vo.SkillsVo;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * @author yulewei
 * @since 2026/6/19
 */
@Slf4j
@SpringBootTest(classes = AgentApplication.class)
public class SessionServiceTest {
    @Resource
    ConfigService configService;
    @Resource
    ChatSessionService chatSessionService;

    @Test
    void test_querySkillList() {
        List<SkillsVo> list = configService.querySkillList();
        log.info("response: {}", JsonUtils.toJsonStr(list));
    }

    @Test
    public void test_chat() {
        List<MessageResp> response = chatSessionService.sessionChat("default", "杭州天气？");
        log.info("response: {}", JsonUtils.toJsonStr(response));
    }
}
