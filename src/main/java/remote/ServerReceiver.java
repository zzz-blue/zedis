package remote;

import server.client.InnerClient;
import common.struct.impl.Sds;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import remote.protocol.RequestParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 服务端接收请求的对象
 **/
public class ServerReceiver implements Receiver {

    private static Log logger = LogFactory.getLog(ServerReceiver.class);

    private final ByteBuffer socketBuffer;        // 用于从SocketChannel中读取数据
    private final Sds queryBuffer;                // 查询缓冲区
    private final RequestParser requestParser;    // 请求解析器
    private final InnerClient client;             // 该receiver关联的client对象

    public ServerReceiver(InnerClient client) {
        this.socketBuffer = ByteBuffer.allocateDirect(8);
        this.queryBuffer = Sds.createEmptySds();
        this.requestParser = new RequestParser(client, this.queryBuffer);
        this.client = client;
    }


    @Override
    public void receive() {

    }

    /**
     * 处理查询缓冲区的数据
     */
    public void processRequest() {
        requestParser.parseRequest();
    }

    /**
     * 从客户端对应的SocketChannel中读取数据到客户端的查询缓冲区
     * @return 返回一个int值。返回值为-1表示客户端已经关闭连接，返回值为正数表示读取的字节数，0表示异常情况
     */
    public int readDataFromSocket() {
        SocketChannel channel = this.client.getSocketChannel();
        int bytesCount = 0;
        try {
            this.socketBuffer.clear();

            int byteRead = channel.read(this.socketBuffer);

            while (byteRead > 0) {
                bytesCount += byteRead;

                // 将读取的数据写入查询缓冲区
                this.socketBuffer.flip();
                while (this.socketBuffer.hasRemaining()) {
                    // 将从channel中读取到的数据写入查询缓冲区
                    byte [] temp = new byte[this.socketBuffer.remaining()];
                    this.socketBuffer.get(temp);
                    this.queryBuffer.append(temp);
                }

                this.socketBuffer.clear();
                byteRead = channel.read(this.socketBuffer);
            };

            // 正常读取了数据，直接返回
            if (bytesCount > 0) {

                // 测试，输出从网络中读取的数据
                logger.debug("服务器接收到数据：" + this.queryBuffer.toString());

                return bytesCount;
            }

            // 客户端关闭连接，返回-1
            if (byteRead == -1) {
                return -1;
            }
        } catch (IOException e) {
            logger.error("Read from SocketChannel error");
            return -1;
        }

        // 异常情况，返回0
        return 0;
    }

    public Sds getQueryBuffer() {
        return this.queryBuffer;
    }
}
