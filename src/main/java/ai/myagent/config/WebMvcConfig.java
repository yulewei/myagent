package ai.myagent.config;

import ai.myagent.interceptor.WebInterceptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    static {
        // 让 hibernate validator 参数校验的输出消息为中文
        Locale.setDefault(new Locale("zh", "CN"));
    }

    @Resource
    private WebInterceptor webInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/v3/api-docs/**", "/swagger-resources/**", "/webjars/**", "/error/**", "/swagger-ui.html/**");
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        JsonMapper objectMapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl ->
                        incl.withContentInclusion(JsonInclude.Include.NON_NULL)
                                .withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        builder.withJsonConverter(new JacksonJsonHttpMessageConverter(objectMapper));
    }
}
