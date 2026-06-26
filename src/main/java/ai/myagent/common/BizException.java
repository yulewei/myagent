package ai.myagent.common;

import static ai.myagent.common.ExceptionCodes.BIZ_ERROR;

/**
 * 通用业务异常
 * @author yulewei
 */
public class BizException extends BaseException {

    public BizException() {
        super(BIZ_ERROR);
    }

    public BizException(String msg) {
        super(BIZ_ERROR.getCode(), msg);
    }

    public BizException(String msg, Object... params) {
        super(BIZ_ERROR.getCode(), msg, params);
    }

    public BizException(int code, String msg) {
        super(code, msg);
    }

    public BizException(int code, String msg, Object... params) {
        super(code, msg, params);
    }

    public BizException(Throwable cause) {
        super(cause, BIZ_ERROR.getCode(), BIZ_ERROR.getMsg());
    }

    public BizException(Throwable cause, String msg) {
        super(cause, BIZ_ERROR.getCode(), msg);
    }

    public BizException(Throwable cause, String msg, Object... params) {
        super(cause, BIZ_ERROR.getCode(), msg, params);
    }

    public BizException(Throwable cause, int code, String msg) {
        super(cause, code, msg);
    }

    public BizException(Throwable cause, int code, String msg, Object... params) {
        super(cause, code, msg, params);
    }

}

