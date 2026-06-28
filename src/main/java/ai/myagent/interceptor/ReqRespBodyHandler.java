package ai.myagent.interceptor;

import ai.myagent.annotation.RawResponse;
import ai.myagent.model.Response;
import ai.myagent.util.JsonMapper;
import ai.myagent.util.JsonUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static ai.myagent.constant.Constants.*;

/**
 * 接口请求和响应日志打印拦截器
 *
 * @author yulewei
 */
@Slf4j
@ControllerAdvice
public class ReqRespBodyHandler extends RequestBodyAdviceAdapter implements ResponseBodyAdvice<Object> {
    private static final JsonMapper mapper = JsonMapper.nonNullMapper();

    @Resource
    private HttpServletRequest servletRequest;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {

        String requestUrl = servletRequest.getRequestURI();
        Class<?> clazz = parameter.getDeclaringClass();
        if (!clazz.getName().startsWith(BASE_PACKAGE)) {
            return inputMessage;
        }
        String requestMethod = (String) servletRequest.getAttribute(ATTR_REQUEST_METHOD);
        byte[] responseBody = inputMessage.getBody().readAllBytes();
        log.info("Request `{}` ({}), body: {}", requestUrl, requestMethod, new String(responseBody, StandardCharsets.UTF_8));
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(responseBody);
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        String requestUrl = servletRequest.getRequestURI();
        Class<?> clazz = returnType.getContainingClass();
        if (!clazz.getName().startsWith(BASE_PACKAGE)
                || body instanceof org.springframework.core.io.Resource) {
            return body;
        }

        boolean serialized = false;
        if (!(body instanceof Response) && !HttpEntity.class.isAssignableFrom(returnType.getParameterType())
                && !returnType.getParameterType().isAnnotationPresent(RawResponse.class)) {
            if (selectedContentType.equals(MediaType.APPLICATION_JSON)) {
                // 自动包装响应数据
                body = Response.ok(body);
                if (selectedConverterType.isAssignableFrom(StringHttpMessageConverter.class)) {
                    body = JsonUtils.toJsonStr(body);
                    serialized = true;
                }
            }
        }

        String requestMethod = (String) servletRequest.getAttribute(ATTR_REQUEST_METHOD);
        if (requestMethod == null && returnType.getMethod() != null) {
            requestMethod = clazz.getSimpleName() + "." + returnType.getMethod().getName();
        }
        Long startTime = (Long) servletRequest.getAttribute(ATTR_START_TIME);
        Long time = startTime != null ? System.currentTimeMillis() - startTime : null;
        log.info("Response `{}` ({}), {} ms, body: {}", requestUrl, requestMethod, time, !serialized ? mapper.toJsonStr(body) : body);
        return body;
    }

}
