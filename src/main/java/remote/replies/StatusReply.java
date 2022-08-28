package remote.replies;

import remote.Reply;
import remote.protocol.Protocol;
import remote.protocol.ReplyType;

/**
 * 表示状态恢复的Reply结构
 * 由于一旦创建StatusReply，其内部content就不会变，因此设置为final
 * 由于每一次构建Reply对应的回复消息，都需要字符串拼接，且回复消息的内容也是不变的，因此考虑对回复消息进行缓存，避免重复计算
 * 回复消息的内容被保存在replyMessage中
 * 为了优化计算，考虑使用惰性计算的方式，只有真正需要回复消息时，才计算消息写入缓存
 * 此外，为了未来可能涉及的并发情况，防止写入缓存在多线程环境下出现异常，这里使用双检锁方式来设置计算结果
 **/
public class StatusReply implements Reply {
    private final String content;           // 表示状态的数据内容
    private volatile String replyMessage;   // 按照协议构建的标准回复

    public StatusReply(String status) {
        this.content = status;
    }

    @Override
    public ReplyType getType() {
        return ReplyType.STATUS;
    }

    @Override
    public Object getContent() {
        return content;
    }

    /**
     * 按照协议格式构建回复信息
     * 格式： "+OK\r\n"
     * @return
     */
    @Override
    public String buildReplyMessage() {
        StringBuilder message = new StringBuilder();

        message.append(Protocol.STATUS_PREFIX);
        message.append(content);
        message.append(Protocol.DELIMITER);

        return message.toString();
    }

    /**
     * 获取协议格式的回复信息
     * 回复信息会被缓存，避免重复计算
     * 该方法线程安全
     * @return
     */
    @Override
    public String getReplyMessage() {
        // 双重检查
        if (this.replyMessage == null) {
            // 用this锁对象，保护this.replyMessage域
            synchronized (this) {
               if (this.replyMessage == null) {
                   this.replyMessage = buildReplyMessage();
               }
            }
        }
        return this.replyMessage;
    }
}
