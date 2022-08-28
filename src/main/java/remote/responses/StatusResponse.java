package remote.responses;

import remote.Reply;
import remote.Response;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;

/**
 * 表示状态恢复的Response结构
 * @Author zzz
 * @Date 2021/11/28
 **/
public class StatusResponse implements Response {
    private final String content;           // 表示状态的数据内容
    private final int byteSize;

    public StatusResponse(String status, int byteSize) {
        this.content = status;
        this.byteSize = byteSize;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.STATUS;
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
