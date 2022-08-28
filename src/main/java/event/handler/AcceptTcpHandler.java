package event.handler;

import server.client.InnerClient;
import event.FileEventHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import server.ZedisServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * 由于需要的仅仅是处理事件的那段逻辑
 * 因此，该类被实现为单例模式，当需要执行处理函数时都是由单例对象执行的
 * 这样可以避免为了调用同一处理函数而反复创建该类的实例
 * 处理新客户端连接到服务器的事件
 *
 */
public class AcceptTcpHandler implements FileEventHandler {
    private static Log logger = LogFactory.getLog(AcceptTcpHandler.class);

    private static volatile AcceptTcpHandler instance;

    private AcceptTcpHandler() {
        super();
    }

    /**
     * 双检锁实现单例模式
     *
     * @return AcceptTcpHandler的实例
     */
    public static AcceptTcpHandler getHandler() {
        if (instance == null) {
            synchronized (AcceptTcpHandler.class) {
                if (instance == null) {
                    instance = new AcceptTcpHandler();
                }
            }
        }

        return instance;
    }

    /**
     * 实现ACCEPT事件到达时的处理逻辑：
     * 从ServerSocketChannel处接收ACCEPT事件，为相应的连接建立SocketChannel和Client对象
     * 并将这些创建的SocketChannel也存入事件循环
     *
     * @return 操作是否成功
     */
    @Override
    public boolean handle(ZedisServer server, SelectionKey selectionKey, Object privateData) {
        ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = null;
        try {
            socketChannel = ssc.accept();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 创建client
        InnerClient newClient = (InnerClient) InnerClient.createClient(socketChannel);
        server.addClient(newClient);

        logger.info("Accepted server.client connection from " + socketChannel.socket().getRemoteSocketAddress());

        // 将这个与客户端关连的socketChannel也注册到EventLoop, 其中，客户端对象client以事件的clientData传入，这里暂时以null代替
        server.getEventLoop().registerFileEvent(socketChannel, SelectionKey.OP_READ, ReadQueryFromClientHandler.getHandler(), newClient);

        return true;
    }
}
