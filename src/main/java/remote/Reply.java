package remote;

import remote.protocol.ReplyType;

/**
 * 用于在服务端表示对客户端的回复消息
 **/
public interface Reply {
    ReplyType getType();        // 获取Reply的类型
    Object getContent();        // 获取Reply内部封装的数据内容
    String buildReplyMessage(); // 按照redis的消息协议格式，构造回复消息
    String getReplyMessage();   // 获取该Reply对应的回复消息
}
