package ai.myagent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author yulewei
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatModelDto {

    /**
     * 是否默认模型
     */
    private Boolean isDefault;

    /**
     * 模型服务商ID
     */
    private String providerId;

    /**
     * 模型服务商名称
     */
    private String providerName;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * API URL前缀
     */
    private String baseUrl;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * Spring AI 的 chatModel 对象
     */
    private ChatModel chatModel;

    /**
     * Spring AI 的 chatClient 对象
     */
    private ChatClient chatClient;
}
