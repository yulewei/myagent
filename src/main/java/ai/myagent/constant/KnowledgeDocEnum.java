package ai.myagent.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yulewei
 * @since 2026/6/28
 */
@Getter
@AllArgsConstructor
public enum KnowledgeDocEnum {
    FILE("file", "文件上传"),
    TEXT("text", "文本输入");

    private final String code;
    private final String desc;
}
