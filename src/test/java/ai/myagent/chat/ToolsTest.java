package ai.myagent.chat;

import ai.myagent.AgentApplication;
import ai.myagent.interceptor.LoggingClientInterceptor;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.Resource;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

/**
 * @author yulewei
 * @since 2026/6/21
 */
@Slf4j
@SpringBootTest(classes = AgentApplication.class, properties = {"spring.profiles.active=dev"})
public class ToolsTest {

    @Resource
    Environment env;

    ChatModel chatModel;
    ChatClient chatClient;

    public static class DateTimeTools {

        @Tool(description = "Get the current date and time in the user's timezone")
        String getCurrentDateTime() {
            return LocalDateTime.now().toString();
        }
    }

    public static class WeatherTools {

        @Tool(name = "Weather", description = "Get the current weather for a given city")
        public String getWeather(@ToolParam String city) {
            String apiUrl = String.format("https://wttr.in/%s?format=3", city);
            HttpResponse<String> response = Unirest.post(apiUrl).asString();
            return response.getBody();
        }
    }

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("API_KEY_DEEPSEEK");
        chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl("https://api.deepseek.com")
                        .apiKey(apiKey)
                        .restClientBuilder(RestClient.builder()
                                .requestInterceptor(new LoggingClientInterceptor()))
                        .build())
                .options(DeepSeekChatOptions.builder()
                        .model("deepseek-v4-flash")
                        .build())
                .build();
        chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * https://docs.spring.io/spring-ai/reference/api/tools.html
     */
    @Test
    public void test_DateTimeTools() {
        chatClient.prompt("明天日期是？")
                .tools(new DateTimeTools())
                .call()
                .content();
    }

    @Test
    public void test_WeatherTools() {
        chatClient.prompt("杭州天气？")
                .call()
                .content();

        chatClient.prompt("杭州天气？")
                .tools(new WeatherTools(), new DateTimeTools())
                .call()
                .content();
    }

    /**
     * https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/ShellTools.md
     */
    @Test
    public void test_ShellTools() {
        chatClient.prompt("当前目录文件列表？")
                .tools(ShellTools.builder().build())
                .call()
                .content();
    }

    @Test
    public void test_ToolSet() {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        ChatClient chatClient = builder
                .defaultTools(
                        FileSystemTools.builder().build(),
                        GrepTool.builder().build(),
                        ShellTools.builder().build(),
                        SmartWebFetchTool.builder(builder.clone().build()).build())
                .build();
        chatClient
                .prompt("https://www.bing.com/ 网页内容")
                .call()
                .content();
    }

    /**
     * https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/spring-ai-agent-utils/docs/SkillsTool.md
     */
    @Test
    void test_SkillsTool() {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultTools(
                        FileSystemTools.builder().build(),
                        GrepTool.builder().build(),
                        ShellTools.builder().build(),
                        SkillsTool.builder()
                                .addSkillsDirectory(System.getenv("HOME") + "/.agents/skills")
                                .build())
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder()
                                .requestToString(JsonUtils::toJsonStr)
                                .responseToString(JsonUtils::toJsonStr)
                                .build())
                .build();
        String response = chatClient.prompt()
                .user("/translate 你好")
                .call()
                .content();
        log.info("response\n {}", response);
    }
}
