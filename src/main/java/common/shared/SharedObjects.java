package common.shared;

/**
 * 共享对象
 * 为了优化内存，对于一些经常使用的字符串对象，将其提前创建，需要时直接共享
 **/
public class SharedObjects {
    public static final String NULL_BULK = "$-1\r\n";
}
