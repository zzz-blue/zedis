package remote.responses;

import remote.Reply;
import remote.Response;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;

/**
 * 整数回复类型
 * @Author zzz
 * @Date 2021/11/28
 **/
public class IntegerResponse implements Response {
    private final long content;           // 存放integer类型回复的实际内容
    private final int byteSize;              // response占的字节大小

    public IntegerResponse(long num, int byteSize) {
        this.content = num;
        this.byteSize = byteSize;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.INTEGER;
    }

    @Override
    public int byteSize() {
        return this.byteSize;
    }

    @Override
    public Object getContent() {
        return content;
    }

}
