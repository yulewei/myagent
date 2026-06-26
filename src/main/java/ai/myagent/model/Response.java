package ai.myagent.model;


import ai.myagent.common.BaseException;
import ai.myagent.common.ExceptionInfo;
import lombok.Data;

import java.io.Serializable;

import static ai.myagent.common.ExceptionCodes.BIZ_ERROR;
import static ai.myagent.common.ExceptionCodes.SUCCESS;

/**
 * 统一接口响应包裹类
 *
 * @author yulewei
 */
@Data
public class Response<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public Response(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Response(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Response<Void> ok() {
        return new Response<>(SUCCESS.getCode(), null);
    }

    public static <T> Response<T> ok(T resp) {
        return new Response<T>(SUCCESS.getCode(), null, resp);
    }

    public static <T> Response<T> failed(String msg) {
        return new Response<>(BIZ_ERROR.getCode(), msg);
    }

    public static <T> Response<T> failed(int code, String msg) {
        return new Response<>(code, msg);
    }

    public static <T> Response<T> failed(BaseException ex) {
        return new Response<>(ex.getCode(), ex.getMsg());
    }

    public static <T> Response<T> failed(ExceptionInfo ex) {
        return new Response<>(ex.getCode(), ex.getMsg());
    }
}
