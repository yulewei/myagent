package ai.myagent.common;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * 自定义异常的基类
 *
 * @author yulewei
 * @since 2022/7/17
 */
@Getter
@Setter
public abstract class BaseException extends RuntimeException {

    private int code;
    private String msg;

    public BaseException() {
        super();
    }

    public BaseException(ExceptionInfo error) {
        super(error.getMsg());
        this.code = error.getCode();
        this.msg = error.getMsg();
    }

    public BaseException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BaseException(int code, String msg, Object... params) {
        super(logFormat(msg, params));
        this.code = code;
        this.msg = logFormat(msg, params);
    }

    public BaseException(Throwable cause, int code, String msg) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }

    public BaseException(Throwable cause, int code, String msg, Object... params) {
        super(logFormat(msg, params), cause);
        this.code = code;
        this.msg = logFormat(msg, params);
    }

    public static String logFormat(String messagePattern, Object... args) {
        FormattingTuple formattingTuple = MessageFormatter.arrayFormat(messagePattern, args);
        String res = formattingTuple.getMessage();
        return res.contains("{}") ? res.replaceAll("\\{\\}", "") : res;
    }
}
