package remote.replies;

import remote.Reply;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;

/**
 * ErrorReply表示
 **/
public class ErrorReply implements Reply {
    private final String content;
    private volatile String replyMessage;

    public ErrorReply(String error) {
        this.content = error;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.ERROR;
    }

    @Override
    public Object getContent() {
        return content;
    }

    /**
     * 按照协议格式构建错误回复
     * 格式："-ERR unknown command 'ERRORCOMMAND'\r\n"
     * @return
     */
    @Override
    public String buildReplyMessage() {
        StringBuilder message = new StringBuilder();

        message.append(Protocol.ERROR_PREFIX);
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
