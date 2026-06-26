package ai.myagent.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yulewei
 * @since 2024/9/10
 */
@UtilityClass
public class WebUtils {

    public static Map<String, String> getRequestAllHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream().collect(Collectors.toMap(h -> h, request::getHeader));
    }

    public Map<String, Object> getRequestAllAttributes(HttpServletRequest request) {
        return Collections.list(request.getAttributeNames()).stream().collect(Collectors.toMap(h -> h, request::getAttribute));
    }
}
