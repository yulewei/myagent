package ai.myagent.misc;

import ai.myagent.model.dto.AgentConfig;
import ai.myagent.model.dto.MessageDto;
import ai.myagent.util.JsonMapper;
import ai.myagent.util.JsonUtils;
import ai.myagent.util.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class MiscTest {

    private JsonMapper jsonMapper = JsonMapper.nonNullMapper();

    @Test
    void test_json() throws IOException {
        String filename = "/Users/yulewei/CODING/#ai-agent-dev-all/myagent/data/list.json";
        String jsonStr = FileUtils.readFileToString(new File(filename), StandardCharsets.UTF_8);
        List<MessageDto> list = JsonUtils.parseArray(jsonStr, MessageDto.class);
        log.info("list: {}", jsonMapper.toJsonStr(list));
    }

    @Test
    public void test_configFile() throws IOException {
        String content = FileUtils.readFileToString(new File("init/config.yaml"), StandardCharsets.UTF_8);
        AgentConfig config = YamlUtils.parse(content, AgentConfig.class);
        log.info("config: {}", jsonMapper.toJsonStr(config));
    }
}
