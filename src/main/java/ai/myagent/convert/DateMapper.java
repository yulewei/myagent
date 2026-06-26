package ai.myagent.convert;

import ai.myagent.util.DateUtils;

import java.time.LocalDateTime;

/**
 * https://mapstruct.org/documentation/stable/reference/html/#invoking-other-mappers
 *
 * @author yulewei
 */
public class DateMapper {

    public Long asLong(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DateUtils.toEpochMilli(dateTime);
    }

    public LocalDateTime asLocalDateTime(Long time) {
        if ((time == null)) {
            return null;
        }
        return DateUtils.toLocalDateTime(time);
    }
}