package remote;

import remote.protocol.ReplyType;

/**
 * 用于在客户端表示从服务端接收到的回复消息
 **/
public interface Response {
    ReplyType getType();        // 获取Response的类型
    int byteSize();             // 获取该Response网络传输中占的字节数
    Object getContent();        // 获取Response内部封装的数据内容
}
