package remote.responses;

import remote.Reply;
import remote.Response;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;

/**
 * ErrorResponse表示
 * @Author zzz
 * @Date 2021/11/28
 **/
public class ErrorResponse implements Response {
    private final String content;
    private final int byteSize;

    public ErrorResponse(String error, int byteSize) {
        this.content = error;
        this.byteSize = byteSize;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.ERROR;
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
