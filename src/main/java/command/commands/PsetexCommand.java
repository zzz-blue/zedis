package command.commands;

import server.client.InnerClient;
import common.struct.ZedisString;
import common.struct.impl.Sds;
import common.utils.StringUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * psetex命令
 * 命令格式：PSETEX key milliseconds value
 * @author: zzz
 * @create: 2021-09-27
 */
public class PsetexCommand extends GenericSetCommand {

    public PsetexCommand() {
        super("psetex", 4, false, "wm");
    }

    @Override
    public void execute(InnerClient client) {
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();

        Sds expireTime = commandArgs.get(2);                  // 超时时间

        if (!StringUtil.isInteger(expireTime.toString())) {
            client.replyError("wrong type of args of command " + this.getName());
            return false;
        }

        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();

        // 调用通用的set方法，将key/value保存
        ZedisString key = commandArgs.get(1);
        Sds expireTime = commandArgs.get(2);                  // 超时时间
        ZedisString value = commandArgs.get(3);

        genericSet(client, SET_NO_FLAGS, key, value, expireTime, TimeUnit.MILLISECONDS);
    }
}
