package ai.myagent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话消息请求
 *
 * @author yulewei
 */
@Data
public class MessageReq implements Serializable {

    @Size(max = 32, message = "会话ID不能超过32个字符")
    @Schema(description = "会话ID")
    private String sessionId;

    @NotBlank(message = "请求消息不能为空")
    @Schema(description = "请求消息内容")
    private String content;
}
