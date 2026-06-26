package ai.myagent.common;

/**
 * 预定义异常错误码
 *
 * @author yulewei
 */
public interface ExceptionCodes {

    ExceptionInfo SUCCESS = new ExceptionInfo(0, "操作成功");

    ExceptionInfo BIZ_ERROR = new ExceptionInfo(1, null);

    ExceptionInfo INVALID_ARGUMENTS = new ExceptionInfo(2, "请求参数校验失败");

    ExceptionInfo UNKNOWN_ERROR = new ExceptionInfo(3, null);
}
