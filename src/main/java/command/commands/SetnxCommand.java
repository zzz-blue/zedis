package command.commands;

import server.client.InnerClient;
import common.struct.ZedisString;
import common.struct.impl.Sds;

import java.util.List;

/**
 * setnx命令
 * 命令格式：SETNX key value
 */
public class SetnxCommand extends GenericSetCommand {

    public SetnxCommand() {
        super("setnx", 3, false, "wm");
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
        ZedisString value = commandArgs.get(2);

        genericSet(client, SET_NX, key, value, null, null);
    }
}
