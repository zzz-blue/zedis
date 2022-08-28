package command.commands;

import server.client.InnerClient;
import command.AbstractCommand;
import common.struct.impl.Sds;

import java.util.List;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/3
 **/
public class EchoCommand extends AbstractCommand {
    public EchoCommand() {
        super("echo", 2, false, "r");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();

        Sds content = commandArgs.get(1);
        client.replyBulk(content);
    }
}
