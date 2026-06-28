package ai.myagent.misc;

import ai.myagent.model.dto.AgentConfig;
import ai.myagent.model.dto.MessageDto;
import ai.myagent.util.JsonMapper;
import ai.myagent.util.JsonUtils;
import ai.myagent.util.ScriptExecutor;
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

    private final JsonMapper jsonMapper = JsonMapper.nonNullMapper();

    @Test
    public void test_configFile() throws IOException {
        String content = FileUtils.readFileToString(new File("init/config.yaml"), StandardCharsets.UTF_8);
        AgentConfig config = YamlUtils.parse(content, AgentConfig.class);
        log.info("config: {}", jsonMapper.toJsonStr(config));
    }

    @Test
    void test_ScriptExecutor() {
        ScriptExecutor.Result result = ScriptExecutor.run("/Users/yulewei/myagent/ext/install.sh");
        log.info("result: {}", jsonMapper.toJsonStr(result.isSuccess()));
    }
}
