package ai.myagent.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间日期工具类。工具类的时间日期的默认格式 `yyyy-MM-dd HH:mm:ss`
 *
 * @author yulewei
 * @see DateTimeFormatter#ISO_LOCAL_DATE_TIME
 * @see DateTimeFormatter#ISO_LOCAL_TIME
 * @see DateFormatUtils
 */
@UtilityClass
public class DateUtils {
    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATETIME_EXT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final DateTimeFormatter DEFAULT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DEFAULT_DATETIME_EXT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final DateTimeFormatter DEFAULT_DATE_HOUR_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter ISO_DATETIME_EXT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter ISO_YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter ISO_HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter ISO_BASIC_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter ISO_BASIC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static Date toDate(@Nullable LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date toDate(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Long toEpochMilli(@Nullable LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static Long toEpochMilli(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static Long toEpochMilli(@Nullable OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toInstant().toEpochMilli();
    }

    public static Long toEpochMilli(@Nullable ZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toInstant().toEpochMilli();
    }

    public static LocalDate toLocalDate(@Nullable Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(@Nullable Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static LocalDate toLocalDate(@Nullable Long time) {
        if (time == null) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(@Nullable Long time) {
        if (time == null) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static OffsetDateTime toOffsetDateTime(@Nullable Long time) {
        if (time == null) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public static ZonedDateTime toZonedDateTime(@Nullable Long time) {
        if (time == null) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
    }

    /**
     * 把时间日期格式化为 `yyyy-MM-dd HH:mm:ss`（默认）
     */
    public static String format(@Nullable Long time) {
        if (time == null) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DEFAULT_DATETIME);
    }

    /**
     * 按指定格式格式化时间日期
     */
    public static String format(@Nullable Long time, @Nonnull DateTimeFormatter formatter) {
        if (time == null) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(formatter);
    }

    /**
     * 把时间日期格式化为 `yyyy-MM-dd HH:mm:ss`（默认）
     */
    public static String format(@Nullable Date date) {
        if (date == null) {
            return null;
        }
        return DateUtils.toLocalDateTime(date).format(DEFAULT_DATETIME);
    }

    /**
     * 把时间日期格式化为 `yyyy-MM-dd HH:mm`
     */
    public static String formatToDhm(@Nullable Date date) {
        if (date == null) {
            return null;
        }
        return DateUtils.toLocalDateTime(date).format(DEFAULT_DATE_HOUR_MINUTE);
    }

    /**
     * 把时间日期格式化为 `yyyy-MM-dd`
     */
    public static String formatToDate(@Nullable Date date) {
        if (date == null) {
            return null;
        }
        return DateUtils.toLocalDateTime(date).format(ISO_DATE);
    }

    /**
     * 按指定格式格式化时间日期
     */
    public static String format(@Nullable Date date, @Nonnull DateTimeFormatter formatter) {
        if (date == null) {
            return null;
        }
        return DateUtils.toLocalDateTime(date).format(formatter);
    }

    /**
     * 按指定格式格式化时间日期
     */
    public static String format(@Nullable Date date, @Nonnull String pattern) {
        if (date == null) {
            return null;
        }
        return DateUtils.toLocalDateTime(date).format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 把日期格式化为 `yyyy-MM-dd`
     */
    public static String format(@Nullable LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(ISO_DATE);
    }

    /**
     * 按指定格式格式化日期
     */
    public static String format(@Nullable LocalDate date, @Nonnull DateTimeFormatter formatter) {
        if (date == null) {
            return null;
        }
        return date.format(formatter);
    }

    /**
     * 把时间日期格式化为 `yyyy-MM-dd HH:mm:ss`（默认）
     */
    public static String format(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_DATETIME);
    }

    /**
     * 把时间日期格式化为 `yyyy-MM-dd HH:mm`
     */
    public static String formatToDhm(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_DATE_HOUR_MINUTE);
    }

    /**
     * 把时间日期格式化为 `yyyy-MM-dd`
     */
    public static String formatToDate(@Nullable LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(ISO_DATE);
    }

    /**
     * 按指定格式格式化时间日期
     */
    public static String format(@Nullable LocalDateTime dateTime, @Nonnull DateTimeFormatter formatter) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(formatter);
    }

    /**
     * 按指定格式格式化时间日期
     */
    public static String format(@Nullable LocalDateTime dateTime, @Nonnull String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析格式为 `yyyy-MM-dd HH:mm:ss`（默认）的时间日期字符串
     */
    public static LocalDateTime parse(@Nullable String str) {
        if (str == null) {
            return null;
        }
        return LocalDateTime.parse(str, DEFAULT_DATETIME);
    }

    /**
     * 按指定格式解析时间日期字符串
     */
    public static LocalDateTime parse(@Nullable String str, @Nonnull DateTimeFormatter formatter) {
        if (str == null) {
            return null;
        }
        return LocalDateTime.parse(str, formatter);
    }

    /**
     * 解析格式为 `yyyy-MM-dd HH:mm` 的时间日期字符串
     */
    public static LocalDateTime parseDhm(@Nullable String str) {
        if (str == null) {
            return null;
        }
        return LocalDateTime.parse(str, DEFAULT_DATE_HOUR_MINUTE);
    }

    /**
     * 解析格式为 `yyyy-MM-dd` 的时间日期字符串
     */
    public static LocalDate parseDate(@Nullable String str) {
        if (str == null) {
            return null;
        }
        return LocalDate.parse(str, ISO_DATE);
    }

    /**
     * 按指定格式解析日期字符串
     */
    public static LocalDate parseDate(@Nullable String str, @Nonnull DateTimeFormatter formatter) {
        if (str == null) {
            return null;
        }
        return LocalDate.parse(str, formatter);
    }

    /**
     * 获取凌晨开始的时间点，比如 `2022-03-01 00:00:00.000`
     */
    public static Date atStartOfDay(@Nonnull Date date) {
        LocalDateTime startOfDay = DateUtils.toLocalDateTime(date).with(LocalTime.MIN);
        return DateUtils.toDate(startOfDay);
    }

    /**
     * 获取午夜末尾的时间点，比如 `2022-03-01 23:59:59.999`
     */
    public static Date atEndOfDay(@Nonnull Date date) {
        LocalDateTime endOfDay = DateUtils.toLocalDateTime(date).with(LocalTime.MAX);
        return DateUtils.toDate(endOfDay);
    }

    /**
     * 获取凌晨开始的时间点，比如 `2022-03-01 00:00:00.000`
     */
    public static LocalDateTime atStartOfDay(@Nonnull LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    /**
     * 获取午夜末尾的时间点，比如 `2022-03-01 23:59:59.999`
     */
    public static LocalDateTime atEndOfDay(@Nonnull LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MAX);
    }

}