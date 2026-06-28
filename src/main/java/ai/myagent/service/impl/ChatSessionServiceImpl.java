package ai.myagent.service.impl;

import ai.myagent.common.BizException;
import ai.myagent.constant.EventTypeEnum;
import ai.myagent.convert.MessageConverter;
import ai.myagent.convert.SessionConverter;
import ai.myagent.model.dto.AgentConfig;
import ai.myagent.model.dto.ChatModelDto;
import ai.myagent.model.dto.MessageDto;
import ai.myagent.model.entity.Session;
import ai.myagent.model.vo.*;
import ai.myagent.repo.SessionMessageRepo;
import ai.myagent.repo.SessionRepo;
import ai.myagent.service.ChatSessionService;
import ai.myagent.service.ConfigService;
import ai.myagent.tool.WeatherTools;
import ai.myagent.util.IdUtils;
import ai.myagent.util.JsonUtils;
import com.google.common.collect.Iterables;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springaicommunity.agent.tools.*;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static ai.myagent.constant.Tools.*;

/**
 * 会话服务实现
 *
 * @author yulewei
 */
@Slf4j
@Service
public class ChatSessionServiceImpl implements ChatSessionService {
    public static Map<String, ToolCallingChatOptions> toolsChatOptionsMap = new ConcurrentHashMap<>();
    public static Map<String, ChatMemory> chatMemoryMap = new ConcurrentHashMap<>();

    @Value("#{'${myagent.skills-dir}'.split(',')}")
    private List<String> skillsDir;
    @Value("${myagent.session-default.title:Untitled}")
    private String defaultTitle;
    @Value("${myagent.session-default.prompt-summary:将以上对话内容总结为标题，尽量控制在20个字以内，不得超过30个字}")
    private String promptSummary;

    @Resource
    private ConfigService configService;
    @Resource
    private SessionRepo sessionRepo;
    @Resource
    private SessionMessageRepo sessionMessageRepo;
    @Resource
    private SimpleVectorStore vectorStore;

    /**
     * 查询会话分页列表
     */
    @Override
    public List<SessionResp> querySessionVoPage(SessionPageReq req) {
        return sessionRepo.querySessionVoPage(req);
    }

    /**
     * 查询会话详情
     */
    @Override
    public SessionResp querySessionVo(String sessionId) {
        return sessionRepo.querySessionVo(sessionId);
    }

    /**
     * 新建会话
     */
    @Override
    public SessionResp newSession(SessionNewReq request) {
        if (StringUtils.isNotBlank(request.getSessionId())) {
            Session sessionOld = sessionRepo.querySession(request.getSessionId());
            if (sessionOld != null) {
                throw new BizException("对话ID已存在");
            }
        }
        AgentConfig.SessionConfig sessionDefault = configService.querySessionDefault();
        String modelId = request.getModelId();
        if (StringUtils.isNotBlank(modelId)) {
            ChatModelDto dto = configService.queryModel(modelId);
            if (dto == null) {
                throw new BizException("模型ID不存在");
            }
            if (StringUtils.isBlank(dto.getApiKey()) ||
                    dto.getApiKey().startsWith("${") && dto.getApiKey().endsWith("}")) {
                throw new BizException("模型 {} 的 ApiKey 未配置", modelId);
            }
        } else {
            request.setModelId(sessionDefault.getModelId());
        }
        if (request.getTools() == null) {
            request.setTools(sessionDefault.getTools());
        } else if (request.getTools().isEmpty()
                || request.getTools().contains(NONE_MARK)) {
            request.setTools(null);
        } else if (request.getTools().contains(FULL_MARK)) {
            request.setTools(FULL_LIST);
        } else {
            // 用户指定工具列表
            for (String tool : request.getTools()) {
                if (!FULL_LIST.contains(tool)) {
                    throw new BizException("工具" + tool + "不存在");
                }
            }
            request.setTools(request.getTools().stream().distinct().toList());
        }
        if (CollectionUtils.isEmpty(request.getTools())) {
            request.setTools(null);
        }
        String title = StringUtils.defaultIfBlank(request.getTitle(), sessionDefault.getTitle());
        title = StringUtils.defaultIfBlank(title, defaultTitle);
        request.setTitle(title);
        String sessionId = sessionRepo.insertSession(request);

        Session session = sessionRepo.getSessionById(sessionId);
        return SessionConverter.INSTANCE.toVo(session);
    }

    /**
     * 修改会话，包括标题、模型、工具列表
     */
    @Override
    public void updateSession(SessionUpdateReq request) {
        if (request.getTitle() != null) {
            sessionRepo.updateSessionTitle(request.getSessionId(), request.getTitle());
        }
        if (request.getModelId() != null) {
            ChatModelDto model = configService.queryModel(request.getModelId());
            if (model == null) {
                throw new BizException("模型ID不存在");
            }
            Session session = sessionRepo.getSessionById(request.getSessionId());
            if (session == null) {
                throw new BizException("会话ID不存在");
            }
            if (session.getModelId().equals(request.getModelId())) {
                throw new BizException("会话ID已使用该模型");
            }

            sessionRepo.updateSessionModel(request.getSessionId(), request.getModelId());

            toolsChatOptionsMap.remove(request.getSessionId());
            chatMemoryMap.remove(request.getSessionId());
        }
    }

    /**
     * 删除会话
     */
    @Override
    public void deleteSession(String sessionId) {
        sessionRepo.deleteSession(sessionId);
    }

    /**
     * 在会话中聊天
     */
    public List<MessageResp> sessionChat(MessageReq request) {
        String sessionId = request.getSessionId();
        String userReq = request.getContent();
        if (sessionId == null) {
            sessionId = this.newSession(new SessionNewReq()).getId();
        }
        Session session = sessionRepo.getSessionById(sessionId);
        if (session == null) {
            throw new BizException("会话ID不存在");
        }
        MessageDto lastMessage = sessionMessageRepo.queryLastMessage(sessionId);
        String lastMessageId = lastMessage != null ? lastMessage.getId() : "";

        ChatModelDto dto = configService.queryModel(session.getModelId());
        ChatModel chatModel = dto.getChatModel();
        ChatClient chatClient = dto.getChatClient();

        ToolCallingChatOptions chatOptions = this.getOrBuildToolsChatOptions(sessionId, chatModel, session.getToolList());

        ChatMemory chatMemory = this.getOrBuildChatMemory(sessionId, chatModel);

        // https://docs.spring.io/spring-ai/reference/api/tools.html#_with_chatclient
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
        List<MessageDto> messageList = new ArrayList<>();
        UserMessage userMessage = UserMessage.builder().text(userReq).build();
        chatMemory.add(sessionId, userMessage);
        String userMessageId = IdUtils.fastSimpleUUID();
        MessageDto userMessageDto = MessageDto.builder()
                .id(userMessageId)
                .parentId(lastMessageId)
                .sessionId(sessionId)
                .modelId(session.getModelId())
                .message(userMessage)
                .build();
        messageList.add(userMessageDto);
        lastMessageId = userMessageId;

        Prompt prompt = new Prompt(chatMemory.get(sessionId), chatOptions);
        ChatClient.ChatClientRequestSpec requestSpec = chatClient
                .prompt(prompt)
                .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false));
        if (request.getKnowledgeId() != null) {
            Advisor retrievalAugmentationAdvisor = this.buildRetrievalAugmentationAdvisor(request.getKnowledgeId());
            requestSpec.advisors(retrievalAugmentationAdvisor);
        }
        ChatResponse response = requestSpec.call().chatResponse();
        Message message = response.getResult().getOutput();
        chatMemory.add(sessionId, message);
        MessageDto messageDto = MessageDto.builder()
                .id(IdUtils.fastSimpleUUID())
                .parentId(lastMessageId)
                .sessionId(sessionId)
                .modelId(session.getModelId())
                .message(message)
                .build();
        messageList.add(messageDto);
        lastMessageId = messageDto.getId();

        Usage usage = response.getMetadata().getUsage();
        log.info("usage: {}", JsonUtils.toJsonStr(usage));
        while (response.hasToolCalls()) {
            ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response);
            message = Iterables.getLast(result.conversationHistory());
            chatMemory.add(sessionId, message);
            messageDto = MessageDto.builder()
                    .id(IdUtils.fastSimpleUUID())
                    .parentId(lastMessageId)
                    .sessionId(sessionId)
                    .modelId(session.getModelId())
                    .message(message)
                    .build();
            messageList.add(messageDto);
            lastMessageId = messageDto.getId();

            prompt = new Prompt(result.conversationHistory(), chatOptions);
            response = requestSpec.call().chatResponse();
            message = response.getResult().getOutput();
            chatMemory.add(sessionId, message);
            messageDto = MessageDto.builder()
                    .id(IdUtils.fastSimpleUUID())
                    .parentId(lastMessageId)
                    .sessionId(sessionId)
                    .modelId(session.getModelId())
                    .message(message)
                    .build();
            messageList.add(messageDto);
            lastMessageId = messageDto.getId();
        }
        sessionMessageRepo.batchInsert(messageList);

        if (lastMessage == null) {
            // 如果是第一次聊天，并且使用默认会话标题，则根据聊天内容自动生成会话标题
            AgentConfig.SessionConfig sessionDefault = configService.querySessionDefault();
            String defaultTitle = ObjectUtils.getIfNull(sessionDefault.getTitle(), this.defaultTitle);
            if (Objects.equals(defaultTitle, session.getTitle())) {
                String title = chatClient.prompt(new Prompt(chatMemory.get(sessionId)))
                        .user(promptSummary)
                        .call()
                        .content();
                log.info("session title: {}", title);
                sessionRepo.updateSessionTitle(sessionId, title);
            }
        }
        return MessageConverter.INSTANCE.toVoList(messageList);
    }

    public void sessionChatStream(MessageReq request, Consumer<StreamEventVo> messageConsumer) {
        String sessionId = request.getSessionId();
        String userReq = request.getContent();
        if (sessionId == null) {
            sessionId = this.newSession(new SessionNewReq()).getId();
        }
        Session session = sessionRepo.getSessionById(sessionId);
        if (session == null) {
            throw new BizException("会话ID不存在");
        }
        MessageDto lastMessage = sessionMessageRepo.queryLastMessage(sessionId);
        String lastMessageId = lastMessage != null ? lastMessage.getId() : "";

        ChatModelDto dto = configService.queryModel(session.getModelId());
        ChatModel chatModel = dto.getChatModel();
        ChatClient chatClient = dto.getChatClient();

        ToolCallingChatOptions chatOptions = this.getOrBuildToolsChatOptions(sessionId, chatModel, session.getToolList());

        ChatMemory chatMemory = this.getOrBuildChatMemory(sessionId, chatModel);

        // https://docs.spring.io/spring-ai/reference/api/tools.html#_with_chatclient
        ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();
        List<MessageDto> messageList = new ArrayList<>();
        UserMessage userMessage = UserMessage.builder().text(userReq).build();
        chatMemory.add(sessionId, userMessage);
        String userMessageId = IdUtils.fastSimpleUUID();
        MessageDto userMessageDto = MessageDto.builder()
                .id(userMessageId)
                .parentId(lastMessageId)
                .sessionId(sessionId)
                .modelId(session.getModelId())
                .message(userMessage)
                .build();
        messageList.add(userMessageDto);
        lastMessageId = userMessageId;

        Prompt prompt = new Prompt(chatMemory.get(sessionId), chatOptions);
        String finalMessageId = IdUtils.fastSimpleUUID();
        String finalLastMessageId = lastMessageId;
        Flux<ChatClientResponse> fluxResponse = chatClient
                .prompt(prompt)
                .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
                .stream()
                .chatClientResponse()
                .doOnNext(resp -> {
                    MessageDto tmp = MessageDto.builder()
                            .id(finalMessageId)
                            .parentId(finalLastMessageId)
                            .sessionId(session.getId())
                            .modelId(session.getModelId())
                            .message(resp.chatResponse().getResult().getOutput())
                            .build();
                    messageConsumer.accept(StreamEventVo.builder()
                            .event(EventTypeEnum.MSG)
                            .message(tmp.toVo()).build());
                })
                .doOnComplete(() -> {
                    messageConsumer.accept(StreamEventVo.builder().event(EventTypeEnum.DONE).build());
                });
        AtomicReference<ChatClientResponse> ref = new AtomicReference<>();
        new ChatClientMessageAggregator().aggregateChatClientResponse(fluxResponse, ref::set).blockLast();
        ChatClientResponse response = ref.get();
        Message message = response.chatResponse().getResult().getOutput();
        chatMemory.add(sessionId, message);
        MessageDto messageDto = MessageDto.builder()
                .id(finalMessageId)
                .parentId(lastMessageId)
                .sessionId(sessionId)
                .modelId(session.getModelId())
                .message(message)
                .build();
        messageList.add(messageDto);
        lastMessageId = messageDto.getId();

        while (response.chatResponse().hasToolCalls()) {
            ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response.chatResponse());
            message = Iterables.getLast(result.conversationHistory());
            chatMemory.add(sessionId, message);
            messageDto = MessageDto.builder()
                    .id(IdUtils.fastSimpleUUID())
                    .parentId(lastMessageId)
                    .sessionId(sessionId)
                    .modelId(session.getModelId())
                    .message(message)
                    .build();
            messageList.add(messageDto);
            messageConsumer.accept(StreamEventVo.builder()
                    .event(EventTypeEnum.MSG).message(messageDto.toVo()).build());
            lastMessageId = messageDto.getId();

            prompt = new Prompt(result.conversationHistory(), chatOptions);
            String finalMessageId2 = IdUtils.fastSimpleUUID();
            String finalLastMessageId2 = lastMessageId;
            fluxResponse = chatClient
                    .prompt(prompt)
                    .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
                    .stream()
                    .chatClientResponse()
                    .doOnNext(resp -> {
                        MessageDto tmp = MessageDto.builder()
                                .id(finalMessageId2)
                                .parentId(finalLastMessageId2)
                                .sessionId(session.getId())
                                .modelId(session.getModelId())
                                .message(resp.chatResponse().getResult().getOutput())
                                .build();
                        messageConsumer.accept(StreamEventVo.builder()
                                .event(EventTypeEnum.MSG).message(tmp.toVo()).build());
                    })
                    .doOnComplete(() -> {
                        messageConsumer.accept(StreamEventVo.builder().event(EventTypeEnum.DONE).build());
                    });
            ref = new AtomicReference<>();
            new ChatClientMessageAggregator().aggregateChatClientResponse(fluxResponse, ref::set).blockLast();
            response = ref.get();
            message = response.chatResponse().getResult().getOutput();
            chatMemory.add(sessionId, message);
            messageDto = MessageDto.builder()
                    .id(finalMessageId2)
                    .parentId(lastMessageId)
                    .sessionId(sessionId)
                    .modelId(session.getModelId())
                    .message(message)
                    .build();
            messageList.add(messageDto);
            lastMessageId = messageDto.getId();
        }
        sessionMessageRepo.batchInsert(messageList);

        if (lastMessage == null) {
            // 如果是第一次聊天，并且使用默认会话标题，则根据聊天内容自动生成会话标题
            AgentConfig.SessionConfig sessionDefault = configService.querySessionDefault();
            String defaultTitle = ObjectUtils.getIfNull(sessionDefault.getTitle(), this.defaultTitle);
            if (Objects.equals(defaultTitle, session.getTitle())) {
                String title = chatClient.prompt(new Prompt(chatMemory.get(sessionId)))
                        .user(promptSummary)
                        .call()
                        .content();
                log.info("session title: {}", title);
                sessionRepo.updateSessionTitle(sessionId, title);

                SessionResp sessionResp = sessionRepo.querySessionVo(sessionId);
                messageConsumer.accept(StreamEventVo.builder()
                        .event(EventTypeEnum.SESSION).session(sessionResp).build());
            }
        }
        messageConsumer.accept(StreamEventVo.builder().event(EventTypeEnum.END).build());
    }

    /**
     * 查询会话全部历史消息列表（按时间正序）
     */
    @Override
    public List<MessageResp> queryAllMessageList(String sessionId) {
        List<MessageDto> list = sessionMessageRepo.queryAllList(sessionId);
        return MessageConverter.INSTANCE.toVoList(list);
    }

    /**
     * 查询会话中某用户消息及其对应生成的响应消息列表（按时间正序）
     */
    @Override
    public List<MessageResp> queryMessageListByUserMessageId(String sessionId, String userMessageId) {
        List<MessageDto> list = sessionMessageRepo.queryMessageListByUserMessageId(sessionId, userMessageId);
        return MessageConverter.INSTANCE.toVoList(list);
    }

    /**
     * 查询或构建会话关联的工具集合及ChatOptions
     */
    private ToolCallingChatOptions getOrBuildToolsChatOptions(String sessionId, ChatModel chatModel, List<String> tools) {
        ToolCallingChatOptions chatOptions = toolsChatOptionsMap.get(sessionId);
        if (chatOptions != null) {
            return chatOptions;
        }
        if (CollectionUtils.isEmpty(tools)) {
            chatOptions = ToolCallingChatOptions.builder().build();
            toolsChatOptionsMap.put(sessionId, chatOptions);
            return chatOptions;
        }
        List<Object> toolList = new ArrayList<>();
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        for (String tool : tools) {
            switch (tool) {
                case FILE_TOOL:
                    toolList.add(FileSystemTools.builder().build());
                    break;
                case SHELL_TOOL:
                    toolList.add(ShellTools.builder().build());
                    break;
                case GLOB_TOOL:
                    toolList.add(GlobTool.builder().build());
                    break;
                case GREP_TOOL:
                    toolList.add(GrepTool.builder().build());
                    break;
                case WEBFETCH_TOOL:
                    toolList.add(SmartWebFetchTool.builder(ChatClient.builder(chatModel).build()).build());
                    break;
                case WEATHER_TOOL:
                    toolList.add(new WeatherTools());
                    break;
                case SKILL_TOOL:
                    List<String> dirList = skillsDir.stream()
                            .map(String::trim)
                            .distinct()
                            .filter(StringUtils::isNotBlank)
                            .filter(e -> new File(e).exists())
                            .toList();
                    if (!dirList.isEmpty()) {
                        toolCallbacks.add(SkillsTool.builder()
                                .addSkillsDirectories(dirList)
                                .build());
                    }
                    break;
            }
        }
        toolCallbacks.addAll(Arrays.asList(ToolCallbacks.from(toolList.toArray(Object[]::new))));
        chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build();
        toolsChatOptionsMap.put(sessionId, chatOptions);
        return chatOptions;
    }

    /**
     * 查询或构建会话记忆
     * <p>
     * TODO 目前使用 MessageWindowChatMemory，保留最近 10 条消息，待优化
     */
    private ChatMemory getOrBuildChatMemory(String sessionId, ChatModel chatModel) {
        ChatMemory chatMemory = chatMemoryMap.get(sessionId);
        if (chatMemory == null) {
            chatMemory = MessageWindowChatMemory.builder().maxMessages(10).build();
            chatMemoryMap.put(sessionId, chatMemory);

            // 查询全部历史消息
            List<MessageDto> list = sessionMessageRepo.queryAllList(sessionId);
            List<Message> messageList = list.stream().map(MessageDto::toChatMessage).toList();
            chatMemory.add(sessionId, messageList);
        }
        return chatMemory;
    }

    private Advisor buildRetrievalAugmentationAdvisor(String knowledgeId) {
        Filter.Expression expression = new FilterExpressionBuilder().eq("knowledgeId", knowledgeId).build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(vectorStore)
                        .filterExpression(expression)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}
