package remote.replies;

import remote.Reply;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;

/**
 * 整数回复类型
 **/
public class IntegerReply implements Reply {
    private final long content;           // 存放integer类型回复的实际内容
    private volatile String replyMessage;    // 缓存按格式构建的标准回复

    public IntegerReply(long num) {
        this.content = num;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.INTEGER;
    }

    @Override
    public Object getContent() {
        return content;
    }

    /**
     * 按照协议格式构建整数回复
     * 格式: ":3\r\n"
     * @return
     */
    @Override
    public String buildReplyMessage() {
        // 将long的数据转换为String格式的表示
        String stringContent = String.valueOf(this.content);

        StringBuilder message = new StringBuilder();

        message.append(Protocol.INTEGER_PREFIX);
        message.append(stringContent);
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
