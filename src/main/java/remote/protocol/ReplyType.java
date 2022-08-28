package remote.protocol;

/**
 * @Author zzz
 * @Date 2021/11/28
 **/
public enum ReplyType {
    ERROR,      // 错误信息
    STATUS,     // 状态回复
    INTEGER,    // 整数回复
    BULK,       // 字符串回复
    MULTI_BULK, // 多行字符串回复
    NIL;        // 空回复
}
