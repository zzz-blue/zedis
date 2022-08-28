package common.utils;


public class ByteUtil {
    public static byte [] longToBytes(long integer) {
        byte [] bytes = new byte[8];
        bytes[0] = (byte)(integer >> 56);
        bytes[1] = (byte)(integer >> 48);
        bytes[2] = (byte)(integer >> 40);
        bytes[3] = (byte)(integer >> 32);
        bytes[4] = (byte)(integer >> 24);
        bytes[5] = (byte)(integer >> 16);
        bytes[6] = (byte)(integer >> 8);
        bytes[7] = (byte)integer;

        return bytes;
    }

    public static long bytesToLong(byte [] bytes) {
        //如果不与0xff进行按位与操作，转换结果将出错
        int int1 = (bytes[0] & 0xff) << 56;
        int int2 = (bytes[1] & 0xff) << 48;
        int int3 = (bytes[2] & 0xff) << 40;
        int int4 = (bytes[3] & 0xff) << 32;
        int int5 = (bytes[4] & 0xff) << 24;
        int int6 = (bytes[5] & 0xff) << 16;
        int int7 = (bytes[6] & 0xff) << 8;
        int int8 = bytes[7] & 0xff;
        return int1|int2|int3|int4|int5|int6|int7|int8;
    }

    public static byte [] intToBytes(int integer) {
        byte [] bytes = new byte[4];
        bytes[0] = (byte)(integer >> 24);
        bytes[1] = (byte)(integer >> 16);
        bytes[2] = (byte)(integer >> 8);
        bytes[3] = (byte)integer;

        return bytes;
    }

    public static int bytesToInt(byte [] bytes) {
        //如果不与0xff进行按位与操作，转换结果将出错
        int int1 = (bytes[0] & 0xff) << 24;
        int int2 = (bytes[1] & 0xff) << 16;
        int int3 = (bytes[2] & 0xff) << 8;
        int int4 = bytes[3] & 0xff;
        return int1|int2|int3|int4;
    }

    public static byte [] charToBytes(char c) {
        byte [] bytes = new byte[2];
        bytes[0] = (byte)((c & 0xFF00) >> 8);
        bytes[1] = (byte)(c & 0xFF);
        return bytes;
    }

    public static byte [] shortToBytes(short shortInteger) {
        byte [] bytes = new byte[2];
        bytes[0] = (byte)(shortInteger >> 8);
        bytes[1] = (byte)(shortInteger);
        return  bytes;
    }

    public static short bytesToShort(byte [] bytes) {
        int short1 = (bytes[0] & 0xff) << 8;
        int short2 = bytes[1] & 0xff;
        return (short) (short1 | short2);
    }

    /**
     * 尝试能否将bytes数组中的字符表示转换为整数
     * @param bytes
     * @return
     */
    public static boolean tryTransformBytesToLong(byte [] bytes) {
        if (bytes.length == 0 || bytes.length > 64) {
            return false;
        }

        int index = 0;
        if ((char)bytes[0] == '-') {
            index++;
        }

        if (index >= bytes.length) {
            return false;
        }

        while (index < bytes.length) {
            char c = (char)bytes[index];
            if (c >= '0' && c <= '9') {
                index++;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 将bytes数组中的字符表示转换为整数
     * @param bytes
     * @return
     */
    public static long transformBytesToLong(byte [] bytes) {
        long val = 0;
        int index = 0;
        boolean negative = false;

        if ((char) bytes[0] == '-') {
            negative = true;
            index++;
        }

        while (index < bytes.length) {
            char c = (char)bytes[index];
            if (c >= '0' && c <= '9') {
                val = val * 10 + (c - '0');
                index++;
            } else {
                throw new IllegalStateException();
            }
        }

        return negative ? -1 * val : val;
    }

}
