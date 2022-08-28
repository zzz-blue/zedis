package command;

import server.client.InnerClient;

/**
 * @Description Zedis命令抽象接口
 * @Author zzz
 * @Date 2021-08-21
 */
@FunctionalInterface
public interface Command {
    void execute(InnerClient client);
}
