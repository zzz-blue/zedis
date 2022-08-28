package common;

/**
 * @Description zedis中的错误类型
 * @Author zzz
 * @Date 2021/9/26
 **/
public enum ErrorType {
    IO_ERR,
    EOF_ERR,
    PROTOCAL_ERR,
    OOM_ERR,
    OTHER_ERR;
}
