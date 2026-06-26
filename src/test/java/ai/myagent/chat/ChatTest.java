package ai.myagent.chat;

import ai.myagent.AgentApplication;
import ai.myagent.interceptor.LoggingClientInterceptor;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.Resource;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.*;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yulewei
 * @since 2026/6/20
 */
@Slf4j
@SpringBootTest(classes = AgentApplication.class)
public class ChatTest {
    @Value("${myagent.session-default.prompt-summary}")
    private String promptSummary;

    @Resource
    Environment env;
    ChatClient chatClient;


    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        ChatModel chatModel = DeepSeekChatModel.builder()
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

    @Test
    void test_apiKey() {
        log.info(env.resolvePlaceholders("${DEEPSEEK_API_KEY}"));
        log.info(System.getenv("DEEPSEEK_API_KEY"));
    }

    @Test
    public void test_chatSummary() {
        List<Message> messages = new ArrayList<>();
        messages.add(UserMessage.builder().text("你是什么模型？").build());
        ChatResponse response = chatClient
                .prompt(new Prompt(messages))
                .call()
                .chatResponse();
        log.info("response\n {}", response.getResult().getOutput().getText());
        messages.add(response.getResult().getOutput());

        messages.add(UserMessage.builder().text(promptSummary).build());

        String title = chatClient
                .prompt(new Prompt(messages))
                .call()
                .content();
        log.info("title\n {}", title);
        assertNotNull(title);
        assertFalse(title.isEmpty());
        assertTrue(title.length() <= 30);
    }


    @Test
    void test_translate() throws IOException {
        File file = ResourceUtils.getFile("classpath:prompt/translate.txt");
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        String userInput = "hello";
        String targetLanguage = "中文";
        PromptTemplate promptTemplate = new PromptTemplate(content);
        Prompt prompt = promptTemplate.create(Map.of("text", userInput, "target_language", targetLanguage));
        String response = chatClient
                .prompt(prompt)
                .user(userInput)
                .call()
                .content();
        log.info("response\n {}", response);
    }

    @Test
    void test_stream() {
        Consumer<Message> consumer = message -> log.info("{}", JsonUtils.toJsonStr(message));
        Consumer<ChatClientResponse> responseConsumer = resp -> consumer.accept(resp.chatResponse().getResult().getOutput());
        Flux<ChatClientResponse> fluxResponse = chatClient.prompt()
                .user("hello")
                .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
                .stream()
                .chatClientResponse()
                .doOnNext(responseConsumer);
        AtomicReference<ChatClientResponse> ref = new AtomicReference<>();
        new ChatClientMessageAggregator().aggregateChatClientResponse(fluxResponse, ref::set).blockLast();
        ChatClientResponse response = ref.get();
        log.info("{}", JsonUtils.toJsonStr(response.chatResponse().getResult().getOutput()));
    }
}
