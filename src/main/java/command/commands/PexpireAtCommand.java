package command.commands;

import server.client.InnerClient;

import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/3
 **/
public class PexpireAtCommand extends GenericExpireCommand {
    public PexpireAtCommand() {
        super("pexpireat", 3, false, "w");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        genericExpire(client, 0, TimeUnit.MILLISECONDS);
    }
}
