package remote.responses;

import remote.Response;
import remote.protocol.ReplyType;

/**
 * @Description
 * @Author zzz
 * @Date 2021/11/28
 **/
public class BulkResponse implements Response {
    private final String content;
    private final int byteSize;

    public BulkResponse(String str, int byteSize) {
        this.content = str;
        this.byteSize = byteSize;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.BULK;
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
