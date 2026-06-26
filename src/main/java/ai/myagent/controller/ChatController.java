package ai.myagent.controller;

import ai.myagent.constant.Tools;
import ai.myagent.convert.ConfigConverter;
import ai.myagent.model.vo.*;
import ai.myagent.service.ChatSessionService;
import ai.myagent.service.ConfigService;
import ai.myagent.support.SseEmitterUTF8;
import ai.myagent.util.JsonMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static ai.myagent.constant.EventTypeEnum.*;

/**
 * AI Agent 会话聊天接口
 *
 * @author yulewei
 * @since 2026/6/20
 */
@Slf4j
@RestController
@RequestMapping("api")
@Tag(name = "REST API")
public class ChatController {
    private static final JsonMapper jsonMapper = JsonMapper.nonNullMapper();

    @Resource
    ChatSessionService chatSessionService;
    @Resource
    ConfigService configService;

    @PostMapping("config/reload")
    @Operation(summary = "重新加载配置")
    public void reloadConfig() {
        configService.reloadConfig();
    }

    @GetMapping("config/models")
    @Operation(summary = "查询模型列表")
    public List<ModelResp> queryModelList() {
        return configService.queryModelVoList();
    }

    @PostMapping("config/switch-model")
    @Operation(summary = "修改默认模型")
    public void switchDefaultModel(@RequestBody @Valid DefaultModelReq request) {
        configService.switchDefaultModel(request.getModelId());
    }

    @GetMapping("config/tools")
    @Operation(summary = "查询内置工具列表")
    public List<ToolInfoResp> queryToolList() {
        return ConfigConverter.INSTANCE.toToolVoList(Tools.getToolInfoList());
    }

    @GetMapping("config/skills")
    @Operation(summary = "查询可加载的Skills列表")
    public List<SkillsVo> querySkillList() {
        return configService.querySkillList();
    }

    @GetMapping("session/page")
    @Operation(summary = "查询会话分页列表（按更新时间倒序）")
    public List<SessionResp> querySessionPage(@RequestBody @Valid SessionPageReq req) {
        return chatSessionService.querySessionVoPage(req);
    }

    @GetMapping("session/{sessionId}")
    @Operation(summary = "查询会话详情")
    public SessionResp querySession(@PathVariable String sessionId) {
        return chatSessionService.querySessionVo(sessionId);
    }

    @PostMapping("session/new")
    @Operation(summary = "新建会话")
    public SessionResp newSession(@RequestBody @Valid SessionNewReq request) {
        return chatSessionService.newSession(request);
    }

    @PostMapping("session/update")
    @Operation(summary = "修改会话，包括标题和模型")
    public void updateSession(@RequestBody @Valid SessionUpdateReq request) {
        chatSessionService.updateSession(request);
    }

    @PostMapping("session/delete/{sessionId}")
    @Operation(summary = "删除会话")
    public void deleteSession(@PathVariable String sessionId) {
        chatSessionService.deleteSession(sessionId);
    }

    @GetMapping("session/{sessionId}/message/all")
    @Operation(summary = "查询会话全部历史消息列表（按时间正序）")
    public List<MessageResp> queryAllMessageList(@PathVariable String sessionId) {
        return chatSessionService.queryAllMessageList(sessionId);
    }

    @GetMapping("session/{sessionId}/req-resp/{messageId}")
    @Operation(summary = "查询会话中某用户消息及其对应生成的响应消息列表（按时间正序）")
    public List<MessageResp> queryMessageListByUserMessageId(@PathVariable String sessionId,
                                                             @PathVariable String messageId) {
        return chatSessionService.queryMessageListByUserMessageId(sessionId, messageId);
    }

    @PostMapping("session/chat")
    @Operation(summary = "在会话中聊天（非流式）")
    public List<MessageResp> sessionChat(@RequestBody @Valid MessageReq request) {
        return chatSessionService.sessionChat(request.getSessionId(), request.getContent());
    }

    @PostMapping(value = "session/chat/stream")
    @Operation(summary = "在会话中聊天（流式）")
    public SseEmitter sessionChatStream(@RequestBody @Valid MessageReq request) throws IOException {
        SseEmitterUTF8 emitter = new SseEmitterUTF8();
        Consumer<StreamEventVo> consumer = msg -> {
            try {
                switch (msg.getEvent()) {
                    case MSG:
                        // 消息json内容
                        emitter.send(jsonMapper.toJsonStr(msg.getMessage()));
                        break;
                    case DONE:
                        // 单条消息流结束
                        emitter.send(SseEmitter.event().name(DONE.getCode()).build());
                        break;
                    case SESSION:
                        emitter.send(SseEmitter.event().name(SESSION.getCode())
                                .data(jsonMapper.toJsonStr(msg.getSession())).build());
                        break;
                    case END:
                        // 全部消息流结束
                        emitter.send(SseEmitter.event().name(END.getCode()).build());
                        emitter.complete();
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        chatSessionService.sessionChatStream(request.getSessionId(), request.getContent(), consumer);
        return emitter;
    }
}
