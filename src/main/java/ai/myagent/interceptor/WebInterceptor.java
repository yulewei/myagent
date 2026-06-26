package ai.myagent.interceptor;

import ai.myagent.util.IdUtils;
import ai.myagent.util.JsonUtils;
import ai.myagent.util.WebUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Map;

import static ai.myagent.constant.Constants.*;

@Slf4j
@Component
public class WebInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId;
        if (StringUtils.isNotEmpty(request.getHeader(TRACE_ID))) {
            traceId = request.getHeader(TRACE_ID);
        } else {
            traceId = IdUtils.fastSimpleUUID();
        }
        MDC.put(TRACE_ID, traceId);
        request.setAttribute(TRACE_ID, traceId);

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        request.setAttribute(ATTR_REQUEST_METHOD_OBJ, handlerMethod.getMethod());

        // 打印请求日志
        this.logRequestStarted(request, handlerMethod);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler instanceof HandlerMethod method
                && !AnnotatedElementUtils.isAnnotated(method.getMethod(), ResponseBody.class)
                && !AnnotatedElementUtils.isAnnotated(method.getMethod().getDeclaringClass(), ResponseBody.class)) {
            String requestUrl = request.getRequestURI();
            String requestMethod = (String) request.getAttribute(ATTR_REQUEST_METHOD);
            Long startTime = (Long) request.getAttribute(ATTR_START_TIME);
            Long executeTime = startTime != null ? System.currentTimeMillis() - startTime : null;
            log.info("Response `{}` ({}), {} ms", requestUrl, requestMethod, executeTime);
        }
        MDC.clear();
    }

    /**
     * 打印请求 parameters 和 headers。
     * 若有 body，在 RequestBodyHandler 中打印。
     */
    public void logRequestStarted(HttpServletRequest request, HandlerMethod handlerMethod) {
        String requestUrl = request.getRequestURI();
        Method method = handlerMethod.getMethod();
        String requestMethod = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        request.setAttribute(ATTR_REQUEST_METHOD, requestMethod);
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        Map<String, String> headers = WebUtils.getRequestAllHeaders(request);
        Map<String, String[]> paramMap = request.getParameterMap();
        log.info("Request `{}` ({}), method: {}, params: {}, headers: {}", requestUrl, requestMethod,
                request.getMethod(), JsonUtils.toJsonStr(paramMap), JsonUtils.toJsonStr(headers));
    }

}

