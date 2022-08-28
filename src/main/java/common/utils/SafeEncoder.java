package common.utils;

import remote.protocol.Protocol;

/**
 * 用于将java中String数据编码为服务端协议可以识别的byte数组，该实现参考jedis
 **/
public final class SafeEncoder {
    public SafeEncoder() {
        throw new InstantiationError("Must not instantiate this class");
    }

    public static byte [] encode(String str) {
        if (str == null) {
            throw new IllegalArgumentException("The String argument 'str' can not be null");
        }

        return str.getBytes(Protocol.CHARSET);
    }

    public static String encode(byte [] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("The byte [] argument 'bytes' can not be null");
        }

        return new String(bytes, Protocol.CHARSET);
    }
}
