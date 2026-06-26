package ai.myagent.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 参数校验失败信息
 *
 * @author yulewei
 * @since 2020/5/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FieldViolation implements Serializable {

    private String path;

    private String message;

    private String value;

}
