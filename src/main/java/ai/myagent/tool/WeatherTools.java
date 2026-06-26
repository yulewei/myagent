package ai.myagent.tool;

import ai.myagent.util.JsonUtils;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

@Slf4j
public class WeatherTools {

    /**
     * https://github.com/chubin/wttr.in
     */
    @Tool(name = "Weather", description = "Get the current weather for a given city")
    public String getWeather(@ToolParam(description = "city") String city) {
        String apiUrl = String.format("https://wttr.in/%s?format=3", city);
        log.info("`Weather` request: {}", apiUrl);
        String response = Unirest.post(apiUrl).asString().getBody();
        log.info("`Weather` response: {}", JsonUtils.toJsonStr(response));
        return response;
    }
}