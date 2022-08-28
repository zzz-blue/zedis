package cli;

import common.ErrorType;
import common.struct.impl.Sds;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import remote.Response;
import remote.protocol.Protocol;
import remote.protocol.ResponseParser;
import common.utils.SafeEncoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @description: 表示客户端对服务的连接信息
 * @Author zzz
 * @Date 2021/9/25
 **/
public class ClientContext {
    private static final Log logger = LogFactory.getLog(ClientContext.class);

    private ErrorType err;
    private String errInfo;
    private SocketChannel socketChannel;
    private ByteBuffer socketBuffer;    // 用于辅助socketChannel读写网络数据
    private int flags;
    private StringBuilder outBuffer;    // 要发送的命令都存储在这里,原版hiredis中即为char*
    private Sds responseBuffer;            // 回复缓冲区
    private int responseBufferParsePos;    // 表示回复缓冲区中当前解析到的位置

    public ClientContext() {
        this.err = null;
        this.errInfo = null;
        this.socketChannel = null;
        this.socketBuffer = ByteBuffer.allocate(1024 * 16);
        this.flags = 0;
        this.outBuffer = new StringBuilder();
        this.responseBuffer = Sds.createEmptySds();
        this.responseBufferParsePos = 0;
    }

    public void connectTcp(String ip, int port) {
        try {
            this.socketChannel = SocketChannel.open();
            this.socketChannel.connect(new InetSocketAddress(ip, port));
            this.socketChannel.configureBlocking(false);
            // 设置socket的keepalive
            this.socketChannel.socket().setKeepAlive(true);

            logger.info("Connect to server " + ip + ":" + port);
        } catch (IOException e) {
            logger.fatal("Could not connect to Redis at " + ip + ":" + port + " Connection refused", e);
            System.exit(1);
        }
    }

    /**
     * 解析输入的命令参数，按照协议将其生成命令
     * 将命令缓存到客户端context的输出缓冲区outBuffer
     * @param startIndex 开始解析的位置
     * @param argv 参数数组
     * @return 操作是否成功
     */
    public boolean appendCommandArgv(int startIndex, final String [] argv) {
        String cmd = Protocol.formatCommand(startIndex, argv);
        if (cmd == null || "".equals(cmd)) {
            setErr(ErrorType.OOM_ERR, "Out of memory");
            return false;
        }

        if (!appendCommand(cmd)) {
            return false;
        }

        return true;
    }

    public String getErrInfo() {
        return this.errInfo;
    }

    public void setErr(ErrorType type, String str) {
        this.err = type;
        this.errInfo = str;
    }

    /**
     * 向缓冲区写入要发送的命令
     * @param cmd
     * @return
     */
    private boolean appendCommand(String cmd) {
        // 测试：输出容标准输入中解析出来的命令
        logger.debug("cli发送的命令: " + cmd);

        this.outBuffer.append(cmd);
        return true;
    }

    /**
     * 将outputBuffer中缓存的数据写入socket的输出流
     * 如果缓冲区为空或者成功发送了缓冲区的数据就返回true，如果发生异常则返回false
     * @return
     */
    public boolean write() {
        if (this.err != null) {
            return false;
        }

        if (this.outBuffer.length() > 0) {
            try {
                byte [] data = SafeEncoder.encode(this.outBuffer.toString());
                this.outBuffer.delete(0, this.outBuffer.length());

                int dataIndex = 0;
                int writeNum = 0;
                while (dataIndex < data.length) {
                    this.socketBuffer.clear();
                    int length = Math.min(data.length - dataIndex, 1024 * 16);
                    this.socketBuffer.put(data, dataIndex, length);

                    this.socketBuffer.flip();

                    writeNum = this.socketChannel.write(this.socketBuffer);
                    while (this.socketBuffer.hasRemaining()) {
                        writeNum = this.socketChannel.write(this.socketBuffer);
                    }

                    dataIndex += length;
                }
            } catch (IOException e) {
                logger.error("Write command to server error", e);
                return false;
            }
        }

        return true;
    }

    /**
     * 从socket中读取服务器的回复信息
     * @return 读取成功返回true，发生错误或没有读到数据返回false
     */
    public boolean read() {
        int readSum = 0;    // 读取的总字符数量，用于判断本次读取是否有读到内容
        int readNum = 0;    // readNum用于判断单次read是否成功
        this.socketBuffer.clear();

        try {
            readNum = this.socketChannel.read(this.socketBuffer);
            // 由于不知道服务器回复有多长，buf一次能否存下，因此需要循环调用read读取
            // readNum == -1表示服务器发送完数据并关闭了连接
            while (readNum != -1) {
                // 特殊情况需要判断
                // 1.当前时刻没有数据可读了
                // 2.buf满了
                if (readNum == 0) {
                    break;
                }

                // 将读取到的内容存入缓冲区
                this.socketBuffer.flip();
                while (this.socketBuffer.hasRemaining()) {
                    this.responseBuffer.append(this.socketBuffer.get());
                }
                // 统计总共读取字节长度
                readSum += readNum;

                this.socketBuffer.clear();
                readNum = this.socketChannel.read(this.socketBuffer);
            }
        } catch (IOException e) {
            logger.error("Happen err when read reply data from server", e);

            // 发生异常前依然读到了数据
            if (readSum > 0) {
                return true;
            } else {
                return false;
            }
        }

        // 根据最后一次读取的返回值判断返回情况
        if (readNum == -1) {
            // todo: 可能需要特殊处理关闭连接
        } else if (readNum == 0) {
            // 正常情况，读到stream末尾
        }

        return readSum > 0 ?true : false;
    }

    public Response parseItem() {
        char prefix = this.responseBuffer.charAt(0);
        byte [] buf = this.responseBuffer.toArray();
        Response response = null;
        switch (prefix) {
            case '-':
                response = ResponseParser.parseErrorResponse(buf, this.responseBufferParsePos);
                break;
            case '+':
                response = ResponseParser.parseStatusResponse(buf, this.responseBufferParsePos);
                break;
            case ':':
                response = ResponseParser.parseIntegerResponse(buf, this.responseBufferParsePos);
                break;
            case '$':
                response = ResponseParser.parseBulkResponse(buf, this.responseBufferParsePos);
                break;
            case '*':
                response = ResponseParser.parseMultiBulkResponse(buf, this.responseBufferParsePos);
                break;
            default:
                // 异常情况
        }

        // 调整缓冲区大小
        this.responseBufferParsePos += response.byteSize();

        if (this.responseBufferParsePos == this.responseBuffer.length()) {
            this.responseBufferParsePos = 0;
            this.responseBuffer.resize(0,1024 * 16);
        }

        return response;
    }


    public Response getResponseFromBuffer() {
        if (this.responseBuffer.length() > 0 && this.responseBufferParsePos != this.responseBuffer.length()) {
            return parseItem();
        } else {
            return null;
        }
    }

    public Response getResponse() {
        Response response = getResponseFromBuffer();
        if (response != null) {
            return response;
        } else {
            // 先向服务器发送命令
            if (write()) {
                // 然后从服务器读取命令的回复
                // 必须要读到信息才返回，不然会一直在这里尝试读取
                while (!read()) {
                }
                response = getResponseFromBuffer();
            }

            return response;
        }
    }
}
