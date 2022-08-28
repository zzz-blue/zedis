package remote;

import server.client.InnerClient;
import event.EventLoop;
import event.handler.SendApplyToClientHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import server.ServerContext;
import common.utils.SafeEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * 服务器要发送给客户端的回复信息，都会先保存在对应客户端对象的回复缓冲区或回复列表中
 * 当客户端的套接字可写时，调用写处理器，将换成的回复信息发送给客户端
 * 回复消息优先缓存在缓冲区数组中，当缓冲区数组空间不足时，则存入队列中
 **/
public class ServerReplyer implements Replyer {
    private static Log logger = LogFactory.getLog(Replyer.class);

    public static final int REPLY_CHUNK_BYTES = 16 * 1024;      // 回复缓冲区大小：16kb
    public static final int MAX_WRITE_PER_EVENT = 1024 * 64;    // 每次给一个client回复的最大数据

    private InnerClient client;                 // replyer关联的client
    private SocketChannel socketChannel;        // 对应客户端建立连接时创建的SocketChannel
    private ByteBuffer responseBuffer;          // 固定大小的回复缓冲区，最大缓存16kb,当缓冲区用完或回复太大无法放入buf数组，就会开始使用缓冲队列
    private boolean isWriteMode;                // 用于记录responseBuffer的当前的读/写模式，默认是write模式
    private LinkedList<byte[]> responseQueue;   // 回复缓冲队列
    private ByteBuffer responseQueueBuffer;     // 由于要将队列中的数据发送出去，还是需要借助ByteBuffer，因此另创建一个

    public ServerReplyer(InnerClient client) {
        this.client = client;
        this.socketChannel = client.getSocketChannel();
        this.responseBuffer = ByteBuffer.allocateDirect(REPLY_CHUNK_BYTES);
        this.isWriteMode = true;
        this.responseQueue = new LinkedList<>();
        this.responseQueueBuffer = ByteBuffer.allocateDirect(REPLY_CHUNK_BYTES);
    }

    @Override
    public void reply(Reply reply, Object clientData) {
        if (!prepareClientToWrite(clientData)) {
            return;
        }

        // 将回复消息写入客户端缓冲区或缓冲队列
        String replyMessage = reply.getReplyMessage();
        addReply(replyMessage);
    }

    /**
     * 将回复消息添加到缓冲区
     * 注意缓冲区的优先级：
     * （1）缓冲区：优先存入这里，当数据过大时无法放入时才考虑放入缓冲队列
     * （2）缓冲列表：当缓冲队列有数据，则之后的数据也应该放入缓冲队列
     * @param replyMessage
     */
    private void addReply(String replyMessage) {
        byte [] messageBytes = SafeEncoder.encode(replyMessage);

        if (!addReplyToBuffer(messageBytes)) {
            addReplyToQueue(messageBytes);
        }
    }


    /**
     * 这个函数在每次向客户端发送数据时都会被调用。
     *
     * 函数的行为如下：
     * (1)
     * 当客户端可以接收新数据时（通常情况下都是这样），函数返回 true，
     * 并将写处理器（write handler）注册到事件循环中，
     * 这样当套接字可写时，新数据就会被写入。
     *
     * (2)
     * 对于那些不应该接收新数据的客户端，比如伪客户端、 master 以及未 ONLINE 的 slave ，
     * 或者写处理器安装失败时，
     * 函数返回 false 。
     *
     * (3)
     * 通常在每个回复被创建时调用，如果函数返回 false ，
     * 那么没有数据会被追加到输出缓冲区。
     *
     * @return 操作是否成功
     */
    private boolean prepareClientToWrite(Object client) {
        // [暂时不实现] LUA 脚本环境所使用的伪客户端总是可写的
        // TODO

        // [暂时不实现] 客户端是主服务器并且不接受查询，那么它是不可写的，出错
        // TODO

        // 无连接的伪客户端总是不可写的
        if (this.socketChannel == null) {
            return false;
        }

        // 一般情况，为客户端套接字安装写处理器到事件循环
        EventLoop eventLoop = ServerContext.getContext().getEventLoop();

        // 注册写数据事件和处理器
        eventLoop.registerFileEvent(this.socketChannel, SelectionKey.OP_WRITE, SendApplyToClientHandler.getHandler(), client);

        return true;
    }

    /**
     * 将回复信息缓存到客户端的回复缓冲区中
     * @param message 回复信息
     * @return 写入是否成功
     */
    private boolean addReplyToBuffer(byte [] message) {
        // TODO
        // 计算客户端状态
        // 如果正准备关闭客户端，无须再发送内容

        // 如果回复链表里已经有内容，再添加内容到回复缓冲区里面就是错误了
        if (!this.responseQueue.isEmpty()) {
            return false;
        }

        // todo:这个操作会复制数组，需要优化
        // 写入前要保证ByteBuffer处于写入状态
        if (!this.isWriteMode) {
            this.responseBuffer.compact();
            this.isWriteMode = true;
        }

        // 回复缓冲区的空间必须满足
        if (message.length > this.responseBuffer.remaining()) {
            return false;
        }

        // 复制message到回复缓冲区里面
        this.responseBuffer.put(message);

        return true;
    }

    /**
     * 将回复消息写入回复缓冲队列
     * @param message
     */
    private void addReplyToQueue(byte [] message) {
        // TODO
        // 计算客户端状态
        // 如果正准备关闭客户端，无须再发送内容

        // 将回复消息加入缓冲队列
        this.responseQueue.add(message);
    }

    /**
     * 将缓冲区数据写入客户端对应的SocketChannel  buf->channel
     * @return 返回一个int值。返回值为-1表示客户端已经关闭连接，返回值为正数表示写入的字节数，0表示异常情况
     */
    public int writeDataToSocket() {
        // 发给客户端的总数据大小
        int totalWrittenNum = 0;

        // 使responseBuffer处于读模式
        if (this.isWriteMode) {
            this.responseBuffer.flip();
            this.isWriteMode = false;
        }

        // 有缓冲的回复内容待发
        while (this.responseBuffer.hasRemaining() || !this.responseQueue.isEmpty()) {
            // 如果缓冲区有数据，则优先发送缓冲区内的数据
            if (this.responseBuffer.hasRemaining()) {
                int writtenSum = 0; // 单次write写入的字节数
                int writtenNum = 0; // 发送一条回复过程中的发送字节数量和

                try {
                    while (this.responseBuffer.hasRemaining()) {
                        writtenNum = this.socketChannel.write(this.responseBuffer);
                        writtenSum += writtenNum;
                        totalWrittenNum += writtenNum;
                    }
                } catch (IOException e) {
                    logger.error("Write reply to server.client error", e);
                }

                return totalWrittenNum;
            } else {
                // 删除队列第一个元素
                byte [] byteMessage = this.responseQueue.removeFirst();

                // 该节点没有内容，直接跳过
                if (byteMessage.length == 0) {
                    this.responseQueue.removeFirst();
                    continue;
                }

                int writtenNum = 0; // 单次write写入的字节数
                int writtenSum = 0; // 发送一条回复过程中的发送字节数量和

                // 发送队列中的单条消息
                int index = 0;      // 字符串的内容会分批先存入ByteBuffer，index用于标记已经写入的位置
                while (index < byteMessage.length) {
                    // 将需要写入的数据先写入辅助buf 通过buf写入到channel
                    this.responseQueueBuffer.clear();

                    int fillLength = Math.min(byteMessage.length - index, this.responseQueueBuffer.remaining());
                    this.responseQueueBuffer.put(byteMessage, index, fillLength);

                    this.responseQueueBuffer.flip();

                    try {
                        while (this.responseQueueBuffer.hasRemaining()) {
                            writtenNum = this.socketChannel.write(this.responseQueueBuffer);
                            writtenSum += writtenNum;
                            totalWrittenNum += writtenNum;
                        }
                    } catch (IOException e) {
                        logger.error("Write reply to server.client error", e);

                        // 如果发送过程中发生异常，而消息又没有发完，则需要将这部分消息重新缓存起来
                        if (writtenSum < byteMessage.length) {
                            this.responseQueue.addFirst(Arrays.copyOfRange(byteMessage, writtenSum, byteMessage.length));
                        }

                        // 返回发送异常前已经发送成功的数据长度
                        return totalWrittenNum;
                    }

                    index += writtenSum;
                }
            }

            /**
             * 为了避免一个非常大的回复独占服务器，
             * 当写入的总数量大于 PANDIS_MAX_WRITE_PER_EVENT
             * 临时中断写入，将处理时间让给其他客户端，
             * 剩余的内容等下次写入就绪再继续写入
             */
            if (totalWrittenNum > MAX_WRITE_PER_EVENT) {
                break;
            }
        }

        if(totalWrittenNum > 0) {
            return totalWrittenNum;
        }

        // 异常情况，返回0
        return 0;
    }

    public boolean isNothingToReply() {
        if (!this.responseBuffer.hasRemaining() && this.responseQueue.isEmpty()) {
            return true;
        }
        return false;
    }
}
