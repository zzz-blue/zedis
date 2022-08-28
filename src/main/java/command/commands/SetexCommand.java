package command.commands;

import server.client.InnerClient;
import common.struct.ZedisString;
import common.struct.impl.Sds;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * setex命令
 * 命令格式：SETEX key seconds value
 */
public class SetexCommand extends GenericSetCommand {

    public SetexCommand() {
        super("setex", 4, false, "wm");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();

        // 调用通用的set方法，将key/value保存
        ZedisString key = commandArgs.get(1);
        Sds expireTime = commandArgs.get(2);                  // 超时时间
        ZedisString value = commandArgs.get(3);

        genericSet(client, SET_NO_FLAGS, key, value, expireTime, TimeUnit.SECONDS);
    }
}
