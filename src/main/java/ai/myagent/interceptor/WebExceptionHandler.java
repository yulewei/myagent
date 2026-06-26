package ai.myagent.interceptor;

import ai.myagent.common.BizException;
import ai.myagent.common.FieldViolation;
import ai.myagent.model.Response;
import ai.myagent.util.IdUtils;
import ai.myagent.util.JsonMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

import static ai.myagent.common.ExceptionCodes.*;
import static ai.myagent.constant.Constants.TRACE_ID;


/**
 * Web请求的异常处理
 *
 * @author yulewei
 * @since 2026/6/20
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionHandler {

    private static final JsonMapper mapper = JsonMapper.nonNullMapper();

    @ExceptionHandler
    public Response<?> handleException(Exception exception) {
        if (StringUtils.isEmpty(MDC.get(TRACE_ID))) {
            MDC.put(TRACE_ID, IdUtils.fastSimpleUUID());
        }
        if (exception instanceof BizException ex) {
            log.warn("业务异常，{} {}", ex.getCode(), ex.getMsg(), ex);
            return Response.failed(BIZ_ERROR.getCode(), ex.getMsg());
        } else if (exception instanceof ServletRequestBindingException
                || exception instanceof MethodArgumentNotValidException
                || exception instanceof ConstraintViolationException) {
            List<FieldViolation> list = buildFieldViolationList(exception);
            log.error("请求参数校验失败，{}", mapper.toJsonStr(list), exception);
            return new Response<>(INVALID_ARGUMENTS.getCode(), INVALID_ARGUMENTS.getMsg(), list);
        } else {
            log.error("未知异常，{} {}", UNKNOWN_ERROR.getCode(), exception.getMessage(), exception);
            return Response.failed(UNKNOWN_ERROR.getCode(), exception.getMessage());
        }
    }

    public static List<FieldViolation> buildFieldViolationList(Throwable ex) {
        List<FieldViolation> list = new ArrayList<>();
        if (ex instanceof ConstraintViolationException exception) {
            for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
                list.add(new FieldViolation(
                        cv.getPropertyPath().toString(),
                        cv.getMessage(),
                        cv.getInvalidValue() == null ? "null" : cv.getInvalidValue().toString()));
            }
        } else if (ex instanceof BindException exception) {
            BindingResult bindingResult;
            bindingResult = exception.getBindingResult();
            for (ObjectError error : bindingResult.getAllErrors()) {
                if (error instanceof FieldError fieldError) {
                    list.add(new FieldViolation(
                            fieldError.getField(),
                            fieldError.getDefaultMessage(),
                            fieldError.getRejectedValue() == null ? "null" : fieldError.getRejectedValue().toString()));
                }
            }
        }
        return list;
    }
}
