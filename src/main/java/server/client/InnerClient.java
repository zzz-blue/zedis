package server.client;

import command.AbstractCommand;
import command.CommandExecutor;
import command.commands.AuthCommand;
import common.struct.ZedisString;
import common.struct.impl.Sds;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pubsub.Channel;
import pubsub.Pattern;
import remote.*;
import remote.protocol.RequestType;
import database.Database;
import server.ZedisServer;
import server.ServerContext;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * InnerClient是远程客户端在服务器内的一个抽象表示，记录了关于远程客户端的相关信息
 */
public class InnerClient implements Client {
    private static Log logger = LogFactory.getLog(InnerClient.class);

    // 客户端的名字
    private String name;
    // 套接字描述符
    private SocketChannel socketChannel;
    // 回复缓冲部分将进行重构，以一个专门负责网络通信的ServerReplyer进行封装
    private ServerReplyer replyer;
    // 查询缓冲部分将进行重构，以一个专门负责网络通信的ServerReceiver进行封装
    private ServerReceiver receiver;
    // 当前正在使用的数据库
    private Database database;
    // 参数对象数组,客户端通过网络收到的命令经过解析，都会被放到这个里面
    private ArrayList<Sds> commandArgs;
    // 代表认证的状态
    private boolean authenticated;
    // 创建客户端的时间
    private Date createTime;
    // 客户端最后一次和服务器互动的时间
    private Date lastInteractionTime;
    // 客户端状态标志
    private int flags;              /* REDIS_SLAVE | REDIS_MONITOR | REDIS_MULTI ... */
    // 请求的类型：内联命令还是多条命令
    private volatile RequestType requestType;

    /**********************************************
     * 订阅发布功能
     *********************************************/

    // 这个字典记录了客户端所有订阅的频道
    // 键为频道名字，值为 NULL
    // 也即是，一个频道的集合
    private Map<String, Channel> pubSubChannels;

    // 链表，包含多个 pubsubPattern 结构
    // 记录了所有订阅频道的客户端的信息
    // 新 pubsubPattern 结构总是被添加到表尾
    private Map<String, Pattern> pubSubPatterns;

    public int getFlags() {
        return this.flags;
    }

    private InnerClient() {
        this.pubSubChannels = new HashMap<>();
        this.pubSubPatterns = new HashMap<>();
    }

    /**
     * @param socketChannel
     * @return
     */
    public static Client createClient(SocketChannel socketChannel) {
        InnerClient client = new InnerClient();

        //初始化属性
        client.name = null;
        client.socketChannel = socketChannel;
        client.replyer = new ServerReplyer(client);
        client.receiver = new ServerReceiver(client);
        client.commandArgs = new ArrayList<>();
        client.database = ServerContext.getContext().getDatabases();
        client.createTime = client.lastInteractionTime = new Date();
        client.authenticated = false;
        client.flags = 0;
        client.requestType = RequestType.NONE; // 请求类型，默认为0，表示没有类型

        return client;
    }

    public int readData() {
        return this.receiver.readDataFromSocket();
    }

    public int writeData() {
        return this.replyer.writeDataToSocket();
    }

    /**
     * 处理查询缓冲区的数据
     */
    public void processInputData() {
        this.receiver.processRequest();
        // 处理传入的命令
        processCommandArgs();
    }

    /**
     * 处理从客户端数据中解析出的命令参数，执行相应的命令
     *
     * 这个函数执行时，我们已经读入了一个完整的命令到客户端，
     * 这个函数负责执行这个命令，
     * 或者服务器准备从客户端中进行一次读取。
     */
    public void processCommandArgs() {
        // 运行这个函数前，确保客户端的命令参数列表中存在命令参数
        if (this.commandArgs.isEmpty()) {
            return;
        }

        // 目前从客户端输入的数据中解析出了参数：存放在commandArgs中
        // 但在实际执行相应命令前还要进行一些检查操作

        // 单独处理quit命令
        // if (argv[0].getObj().toString().equals("quit")) {
        //    return;
        // }

        // (1)检查用户输入的命令名称是否可以找到对应命令实现，
        // 如果找不到相应的命令实现，服务器不再执行后续步骤，并向客户端返回一个错误。
        // (2)根据命令名称获得的命令实现，可以获得该命令arity属性，
        // 检查命令请求所给定的参数个数是否正确，当参数个数不正确时，不再执行后续步骤，直接向客户端返回一个错误。
        String commandName = this.commandArgs.get(0).toString();
        AbstractCommand command = (AbstractCommand) CommandExecutor.getExecutor().lookupCommand(commandName);

        if (command == null) {
            // 没找到命令
            // 回复错误信息
            replyError("unknow command " + commandName);
            return;
        } else if ((!command.isGreaterThanArity() && command.getArity() != this.commandArgs.size()) || this.commandArgs.size() < command.getArity()) {
            // 参数个数错误
            // 回复错误信息
            replyError("wrong number of arguments for " + commandName + " command");
            return;
        }

        // (3)检查客户端是否已经通过了身份验证，
        // 未通过身份验证的客户端只能执行AUTH命令，
        // 如果未通过身份验证的客户端试图执行除AUTH命令之外的其他命令，那么服务器将向客户端返回一个错误。
        if (ZedisServer.getInstance().getServerConfig().getRequirePassword() != null
            && !this.authenticated
            && ! (command instanceof AuthCommand)) {
            // 回复错误信息
            return;
        }


        // (4) [暂时不实现]
        // 如果服务器打开了maxmemory功能，
        // 那么在执行命令之前，先检查服务器的内存占用情况，并在有需要时进行内存回收，从而使得接下来的命令可以顺利执行。
        // 如果内存回收失败，那么不再执行后续步骤，向客户端返回一个错误。

        // (5) [暂时不实现]
        // 如果服务器上一次执行BGSAVE命令时出错，
        // 并且服务器打开了stop-writes-on-bgsaveerror功能，
        // 而且服务器即将要执行的命令是一个写命令，那么服务器将拒绝执行这个命令， 并向客户端返回一个错误。

        // (6) [暂时不实现]
        // 如果客户端当前正在用SUBSCRIBE命令订阅频道，或者正在用clientUBSCRIBE命令订阅模式，
        // 那么服务器只会执行客户端发来的SUBSCRIBE、clientUBSCRIBE、UNSUBSCRIBE、 PUNSUBSCRIBE四个命令，其他命令都会被服务器拒绝。

        // (7) [暂时不实现]
        // 如果服务器正在进行数据载入，
        // 那么客户端发送的命令必须带有REDIS_CMD_LOADING 标识才会被服务器执行，其他命令都会被服务器拒绝。

        // (8) [暂时不实现]
        // 如果客户端正在执行事务，
        // 那么服务器只会执行客户端发来的EXEC、DISCARD、 MULTI、WATCH四个命令，其他命令都会被放进事务队列中。

        // (9) [暂时不实现]
        // 如果服务器打开了监视器功能，那么服务器会将要执行的命令和参数等信息发送给监视器。

        // 当完成了以上预备操作之后，服务器就可以开始真正执行命令了

        // [暂时不实现] 判断是否是事务模式，如果是就将命令加入队列中
        // 否则直接执行

        CommandExecutor.getExecutor().execute(command, this);

        // 清除该客户端当前缓存的命令参数
        this.commandArgs.clear();
    }

    /**
     * 销毁客户端，清理资源
     */
    @Override
    public void distroy() {
        try {
            this.socketChannel.close();
        } catch (IOException e) {
            logger.warn("Close server.client socket error.");
        }
    }

    /**
     * 为客户端选择数据库
     * @param database
     */
    public void selectDatabase(Database database) {
        this.database = database;
    }

    public Database getDatabase() {
        return database;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public ServerReceiver getReceiver() {
        return receiver;
    }

    public ArrayList<Sds> getCommandArgs() {
        return this.commandArgs;
    }

    public void updateLastInteraction() {
        this.lastInteractionTime = new Date();
    }

    public boolean isNothingToReply() {
        return this.replyer.isNothingToReply();
    }

    /*******************************************
     * 向客户端发送回复
     *******************************************/
    public void replyNil() {
        Reply reply = ReplyBuilder.buildNilReply();
        replyer.reply(reply, this);
    }

    public void replyStatus(String str) {
        Reply reply = ReplyBuilder.buildStatusReply(str);
        replyer.reply(reply, this);
    }

    public void replyStatus(ZedisString s) {
        replyStatus(s.toString());
    }

    public void replyError(String str) {
        Reply reply = ReplyBuilder.buildErrorReply(str);
        replyer.reply(reply, this);
    }

    public void replyError(ZedisString s) {
        replyError(s.toString());
    }

    public void replyBulk(String str) {
        Reply reply = ReplyBuilder.buildBulkReply(str);
        replyer.reply(reply, this);
    }

    public void replyBulk(ZedisString s) {
        replyBulk(s.toString());
    }

    public void replyInteger(long l) {
        Reply reply = ReplyBuilder.buildIntegerReply(l);
        replyer.reply(reply, this);
    }

    public void replyMultiBulk(List<String> bulks) {
        Reply reply = ReplyBuilder.buildMultiBulkReply(bulks);
        replyer.reply(reply, this);
    }

    public Map<String, Channel> getPubSubChannels() {
        return this.pubSubChannels;
    }

    public Map<String, Pattern> getPubSubPatterns() {
        return this.pubSubPatterns;
    }

    /**
     * 将channel添加到client的订阅列表中
     * @param channel
     * @return
     */
    public boolean subscribeChannel(Channel channel) {
        if (this.pubSubChannels.containsKey(channel.getName())) {
            return false;
        } else {
            this.pubSubChannels.put(channel.getName(), channel);
            return true;
        }
    }

    public boolean unsubscribeChannel(String channelName) {
        if (this.pubSubChannels.remove(channelName) != null) {
            return true;
        }
        return false;
    }

    public boolean subscribePattern(Pattern pattern) {
        if (this.pubSubPatterns.containsKey(pattern.getName())) {
            return false;
        } else {
            this.pubSubPatterns.put(pattern.getName(), pattern);
            return true;
        }
    }

    public boolean unsubscribePattern(String patternName) {
        if (this.pubSubPatterns.remove(patternName) != null) {
            return true;
        }
        return false;
    }
}
