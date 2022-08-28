package remote.replies;

import remote.Reply;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;


public class NilReply implements Reply {
    // 由于NilReply没有什么可变的内容，因此直接创建一个静态实例，使用时直接复用该实例即可
    public static final NilReply NIL_REPLY = new NilReply();

    private final String content;
    private volatile String replyMessage;

    private NilReply() {
        this.content = "-1";
    }

    @Override
    public ReplyType getType() {
        return ReplyType.NIL;
    }

    @Override
    public Object getContent() {
        return this.content;
    }

    @Override
    public String buildReplyMessage() {
        StringBuilder message = new StringBuilder();

        message.append(Protocol.BULK_PREFIX);
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
