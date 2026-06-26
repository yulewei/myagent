package ai.myagent.interceptor;

import ai.myagent.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTTP接口请求日志打印
 * <p>
 * https://www.danvega.dev/blog/spring-boot-rest-client-logging
 *
 * @author yulewei
 * @since 2026/06/20
 */
public class LoggingClientInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger("ai.myagent.interceptor.RestClient");

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        log.debug("Request {}, method: {}, headers: {}", request.getURI(), request.getMethod(),
                JsonUtils.toJsonStr(request.getHeaders().toSingleValueMap()));
        if (body.length > 0) {
            log.debug("Request {}, body: {}", request.getURI(), new String(body, StandardCharsets.UTF_8));
        }
        long startTime = System.currentTimeMillis();
        ClientHttpResponse response = execution.execute(request, body);
        Long executeTime = System.currentTimeMillis() - startTime;
        byte[] responseBody = response.getBody().readAllBytes();
        log.debug("Response {}, {} ms, status: {}, headers: {}", request.getURI(), executeTime, response.getStatusCode(),
                JsonUtils.toJsonStr(response.getHeaders().toSingleValueMap()));
        if (responseBody.length > 0) {
            log.debug("Response {}, {} ms, body: {}", request.getURI(), executeTime, new String(responseBody, StandardCharsets.UTF_8));
        }
        return new BufferingClientHttpResponseWrapper(response, responseBody);
    }

    private static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {
        private final ClientHttpResponse response;
        private final byte[] body;

        public BufferingClientHttpResponseWrapper(ClientHttpResponse response, byte[] body) {
            this.response = response;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public HttpHeaders getHeaders() {
            return response.getHeaders();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }
    }
}