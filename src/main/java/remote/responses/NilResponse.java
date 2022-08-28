package remote.responses;

import remote.Reply;
import remote.Response;
import remote.protocol.ReplyType;

/**
 * @Description
 * @Author zzz
 * @Date 2021/11/28
 **/
public class NilResponse implements Response {
    // 由于NilResponse没有什么可变的内容，因此直接创建一个静态实例，使用时直接复用该实例即可
    public static final NilResponse NIL_RESPONSE = new NilResponse();

    private final String content;
    private final int byteSize;

    private NilResponse() {
        this.content = "nil";
        this.byteSize = 5;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.NIL;
    }

    @Override
    public int byteSize() {
        return this.byteSize;
    }

    @Override
    public Object getContent() {
        return this.content;
    }
}
