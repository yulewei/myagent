package ai.myagent.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ID生成器工具类
 *
 * 参考自：hutool 的 IdUtil 和 vjtools 的 IdUtil
 */
@UtilityClass
public class IdUtils {

    /**
     * 获取随机UUID
     *
     * @return 随机UUID
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /*
     * 返回使用ThreadLocalRandom的UUID，比默认的UUID性能更优
     */
    public static String fastUUID() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }

    /**
     * 简化的UUID，去掉了横线，使用性能更好的ThreadLocalRandom生成UUID
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String fastSimpleUUID() {
        return IdUtils.fastUUID().replaceAll("-", "");
    }
}
