package ai.myagent.interceptor;

import ai.myagent.util.JsonUtils;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * HTTP接口请求日志打印
 *
 * @author yulewei
 * @since 2026/06/20
 */
public class LoggingOkHttpInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger("ai.myagent.interceptor.RestClient");

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        log.debug("Request {}, method: {}, headers: {}", request.url(), request.method(),
                JsonUtils.toJsonStr(request.headers().toMultimap()));
        if (request.body() != null && request.body().contentLength() > 0) {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            log.debug("Request {}, body: {}", request.url(), buffer.readUtf8());
        }
        long startTime = System.currentTimeMillis();
        Response response = chain.proceed(request);
        Long executeTime = System.currentTimeMillis() - startTime;
        log.debug("Response {}, {} ms, status: {}, headers: {}", request.url(), executeTime, response.code(),
                JsonUtils.toJsonStr(response.headers().toMultimap()));
        if (response.body() != null) {
            Buffer buffer = response.peekBody(Long.MAX_VALUE).source().getBuffer();
            log.debug("Response {}, {} ms, body: {}", request.url(), executeTime, buffer.readUtf8());
        }
        return response;
    }
}