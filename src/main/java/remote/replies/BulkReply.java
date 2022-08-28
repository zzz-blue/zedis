package remote.replies;

import remote.Reply;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;
import common.utils.SafeEncoder;

/**
 *
 **/
public class BulkReply implements Reply {
    private final String content;
    private volatile String replyMessage;

    public BulkReply(String str) {
        this.content = str;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.BULK;
    }

    @Override
    public Object getContent() {
        return content;
    }

    /**
     * 构建协议格式的bulk回复
     * 格式："$3\r\nbar\r\n"
     * @return
     */
    @Override
    public String buildReplyMessage() {
        StringBuilder message = new StringBuilder();

        message.append(Protocol.BULK_PREFIX);
        // 这里记录的长度不是String类型content本身的长度，而应该是String转为UTF-8的bytes数组的长度
        int contentByteLength = SafeEncoder.encode(this.content).length;
        message.append(contentByteLength);
        message.append(Protocol.DELIMITER);
        message.append(this.content);
        message.append(Protocol.DELIMITER);

        return message.toString();
    }

    @Override
    public String getReplyMessage() {
        if (this.replyMessage == null) {
            synchronized (this) {
                if (this.replyMessage == null) {
                    this.replyMessage = buildReplyMessage();
                }
            }
        }

        return this.replyMessage;
    }
}
