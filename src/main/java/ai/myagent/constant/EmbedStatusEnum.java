package ai.myagent.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 向量化状态
 *
 * @author yulewei
 * @since 2026/6/28
 */
@Getter
@AllArgsConstructor
public enum EmbedStatusEnum {
    INIT("init", "初始状态"),
    DOING("doing", "进行中"),
    FAILED("failed", "失败"),
    SUCCESS("success", "成功");

    private final String code;
    private final String desc;

}
