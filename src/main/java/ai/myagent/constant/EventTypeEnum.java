package ai.myagent.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yulewei
 * @since 2026/6/25
 */
@Getter
@AllArgsConstructor
public enum EventTypeEnum {
    MSG("MSG", "响应消息json内容"),
    DONE("DONE", "单条消息流结束"),
    END("END", "全部消息流结束"),
    SESSION("SESSION", "会话信息更新");

    private final String code;
    private final String desc;
}
