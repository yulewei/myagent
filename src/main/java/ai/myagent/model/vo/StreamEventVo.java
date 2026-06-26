package ai.myagent.model.vo;

import ai.myagent.constant.EventTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yulewei
 * @since 2026/6/25
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamEventVo {

    private EventTypeEnum event;

    private MessageResp message;

    private SessionResp session;
}
