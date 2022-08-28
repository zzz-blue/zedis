package remote.replies;

import remote.Reply;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;
import common.utils.SafeEncoder;

import java.util.List;


public class MultiBulkReply implements Reply {
    private final List<String> content;
    private volatile String replyMessage;

    public MultiBulkReply(List<String> multi) {
        this.content = multi;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.MULTI_BULK;
    }

    @Override
    public Object getContent() {
        return content;
    }

    /**
     * 格式："*3\r\n$1\r\n3\r\n$1\r\n2\r\n$1\r\n1\r\n"
     * @return
     */
    @Override
    public String buildReplyMessage() {
        StringBuilder message = new StringBuilder();

        message.append(Protocol.MULTI_BULK_PREFIX);
        message.append(this.content.size());
        message.append(Protocol.DELIMITER);

        for (String str : this.content) {
            message.append(Protocol.BULK_PREFIX);
            // 这里记录的长度不是String类型content本身的长度，而应该是String转为UTF-8的bytes数组的长度
            int contentByteLength = SafeEncoder.encode(str).length;
            message.append(contentByteLength);
            message.append(Protocol.DELIMITER);
            message.append(str);
            message.append(Protocol.DELIMITER);
        }

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
