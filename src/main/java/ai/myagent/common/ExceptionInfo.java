package ai.myagent.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author yulewei
 * @since 2026/6/20
 */
@Data
@AllArgsConstructor
public class ExceptionInfo {
    private final int code;
    private final String msg;
}
