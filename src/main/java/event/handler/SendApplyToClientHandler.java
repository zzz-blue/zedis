package event.handler;

import server.client.InnerClient;
import event.FileEventHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import server.ZedisServer;

import java.nio.channels.SelectionKey;

/**
 * @description: 将客户端回复缓冲区内的内容发送给客户端
 * @author: zzz
 * @create: 2021-08-22
 */
public class SendApplyToClientHandler implements FileEventHandler {
    private static Log logger = LogFactory.getLog(SendApplyToClientHandler.class);

    private static volatile SendApplyToClientHandler instance;

    private SendApplyToClientHandler() {
        super();
    }

    public static SendApplyToClientHandler getHandler() {
        if (instance == null) {
            synchronized (SendApplyToClientHandler.class) {
                if (instance == null) {
                    instance = new SendApplyToClientHandler();
                }
            }
        }

        return instance;
    }

    /**
     * 将客户端回复缓冲区内的内容发送给客户端
     * buf -> channel
     * @param server
     * @param key
     * @param privateData
     * @return
     */
    @Override
    public boolean handle(ZedisServer server, SelectionKey key, Object privateData) {
        // 测试
        logger.debug("SendApplyToClientHandler被调用");

        InnerClient client = (InnerClient) privateData;

        // 设置服务器的当前客户端
        server.setCurrentClient(client);

        // 读入内容到查询缓冲区
        int writeNum = client.writeData();

        if(writeNum > 0) {
            // 正确写入类数据
            client.updateLastInteraction();
        }

        // 如果回复缓冲区空了，则不需要在监听write事件
        if (client.isNothingToReply()) {
            server.getEventLoop().unregisterFileEvent(key, SelectionKey.OP_WRITE);
        }

        server.clearCurrentClient();

        return true;
    }
}
