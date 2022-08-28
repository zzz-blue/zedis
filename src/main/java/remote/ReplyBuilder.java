package remote;

import common.struct.ZedisString;
import remote.replies.*;

import java.util.List;


public class ReplyBuilder {
    public static Reply buildBulkReply(ZedisString obj) {
        return new BulkReply(obj.toString());
    }

    /**
     * todo: 这部分可以优化：对于一些常用的字符串，可以提前创建出来，需要的时候直接复用这些对象，而避免频繁创建这些小对象
     * @param str
     * @return
     */
    public static Reply buildBulkReply(String str) {
        return new BulkReply(str);
    }

    /**
     * 以bulk的形式，返回一个double双精度浮点数
     * @param d
     * @return
     */
    public static Reply buildBulkReply(double d) {
        String doubleToString = String.valueOf(d);
        return new BulkReply(doubleToString);
    }

    /**
     * todo: 这部分可以优化：对于特定范围内的数（0-32）这些常用的数字，可以提前创建出来，需要的时候直接复用这些对象，而避免频繁创建这些小对象
     * 以bulk的形式，返回一个long整数
     * @param l
     * @return
     */
    public static Reply buildBulkReply(long l) {
        String longToString = String.valueOf(l);
        return new BulkReply(longToString);
    }

    /**
     * todo: 这部分可以优化：因为很多错误信息是固定的，可以提前创建出来，需要的时候直接复用这些对象，而避免频繁创建这些小对象
     * 创建ErrorReply对象
     * @param args
     * @return
     */
    public static Reply buildErrorReply(String args) {
        return new ErrorReply(args);
    }

    /**
     * todo: 这部分可以优化：因为状态回复一共就几种，可以提前创建出来，需要的时候直接复用这些对象，而避免频繁创建这些小对象
     * 创建Status回复类型的Reply对象
     * @param args status信息
     * @return StatusReply对象
     */
    public static Reply buildStatusReply(String args) {
        return new StatusReply(args);
    }

    /**
     * todo: 这部分可以优化：对于一些常用的数字（如0、1等），可以提前创建出来，需要的时候直接复用这些对象，而避免频繁创建这些小对象
     * 创建一个整数回复
     * @param l long类型的整数
     * @return
     */
    public static Reply buildIntegerReply(long l) {
        return new IntegerReply(l);
    }


    public static Reply buildMultiBulkReply(List<String> args) {
        return new MultiBulkReply(args);
    }


    public static Reply buildNilReply() {
        return NilReply.NIL_REPLY;
    }
}
