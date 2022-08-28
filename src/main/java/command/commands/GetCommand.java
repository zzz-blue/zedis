package command.commands;

import server.client.InnerClient;

/**
 * @Description GET命令实现 用于根据key获取string类型的value
 * @Author zzz
 * @Date 2021-08-21
 */
public class GetCommand extends GenericGetCommand {
    public GetCommand() {
        super("get", 2, false, "r");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        genericGet(client);
    }
}
