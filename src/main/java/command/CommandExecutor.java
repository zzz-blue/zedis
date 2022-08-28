package command;

import common.persistence.AOFPersistence;
import common.persistence.AofFsyncFrequency;
import common.struct.impl.Sds;
import server.ServerContext;
import server.client.InnerClient;
import command.commands.*;
import server.config.ServerConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 命令执行器
 * @Author zzz
 * @Date 2021/9/21
 **/
public class CommandExecutor {
    private final static CommandExecutor executor = new CommandExecutor();

    // Zedis的所有命令映射表
    private Map<String, AbstractCommand> commandTable = new HashMap<>();

    private CommandExecutor() {
        // 将所有命令的实现写入表中
        commandTable.put("get", new GetCommand());
        commandTable.put("set", new SetCommand());
        commandTable.put("setnx", new SetnxCommand());
        commandTable.put("setex", new SetexCommand());
        commandTable.put("psetex", new PsetexCommand());
        commandTable.put("append", new AppendCommand());
        commandTable.put("strlen", new StrlenCommand());
        commandTable.put("del", new DelCommand());
        commandTable.put("exists", new ExistsCommand());
        commandTable.put("ping", new PingCommand());
        commandTable.put("echo", new EchoCommand());
        commandTable.put("expire", new ExpireCommand());
        commandTable.put("expireat", new ExpireAtCommand());
        commandTable.put("pexpire", new PexpireCommand());
        commandTable.put("pexpireat", new PexpireAtCommand());
        commandTable.put("ttl", new TtlCommand());
        commandTable.put("pttl", new PttlCommand());
        commandTable.put("persist", new PersistCommand());
        commandTable.put("subscribe", new SubscribeCommand());
        commandTable.put("unsubscribe", new UnsubscribeCommand());
        commandTable.put("psubscribe", new PsubscribeCommand());
        commandTable.put("punsubscribe", new PunsubscribeCommand());
        commandTable.put("publish", new PublishCommand());
        commandTable.put("save", new SaveCommand());
        commandTable.put("bgsave", new BackgroundSaveCommand());
    }

    public static CommandExecutor getExecutor() {
        return executor;
    }

    /**
     * 从命令表中根据名字查找命令实现
     * @param commandName 命令名称
     * @return 命令实现
     */
    public Command lookupCommand(String commandName) {
        return commandTable.get(commandName);
    }

    /**
     * 核心方法执行命令
     * @param command 命令实现
     */
    public void execute(Command command, InnerClient client) {
        beforeExecute(client);
        // 执行命令
        command.execute(client);
        afterExecute(client);
    }

    /**
     * 在命令执行之前执行
     * @param client
     */
    public void beforeExecute(InnerClient client) {

    }
    /**
     * 在命令执行之后进行
     * (1) 如果AOF启用，则进行AOF持久化
     * @param client
     */
    public void afterExecute(InnerClient client) {
        // 将命令传播到AOF模块，进行AOF持久化
        ServerConfig config = ServerContext.getContext().getServerConfig();
        if (config.isAofOn()) {
            AOFPersistence aofPersistence = ServerContext.getContext().getServerInstance().getAofPersistence();
            List<Sds> commands = client.getCommandArgs();
            aofPersistence.feedCommand(commands);
        }
    }
}
