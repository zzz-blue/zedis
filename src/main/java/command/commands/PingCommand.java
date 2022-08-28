package command.commands;

import server.client.InnerClient;
import command.AbstractCommand;
import common.constants.StatusConstants;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/3
 **/
public class PingCommand extends AbstractCommand {
    public PingCommand() {
        super("pong", 1, false, "rt");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        client.replyStatus(StatusConstants.PONG_STATUS);
    }
}
