package remote.protocol;

/**
 * @description: 请求类型
 * @author: zzz
 * @create: 2021-08-19
 */
public enum RequestType {
    NONE,
    MULTI_BULK, // 统一请求协议类型
    INLINE;     // 简单协议类型

    public static final char MULTI_BULK_PREFIX = '*';
}
