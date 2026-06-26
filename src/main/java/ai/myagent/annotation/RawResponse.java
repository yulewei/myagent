package ai.myagent.annotation;


import ai.myagent.model.Response;

import java.lang.annotation.*;

/**
 * REST 接口响应，直接返回原生的对象，不做自动包裹为 {@link Response}
 *
 * @author yulewei
 * @since 2021/8/28
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RawResponse {
}
