package command.commands;

import server.client.InnerClient;
import command.AbstractCommand;
import common.struct.impl.Sds;
import database.Database;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/3
 **/
public abstract class GenericExpireCommand extends AbstractCommand {
    public GenericExpireCommand(String name, int arity, boolean isGreaterThanArity, String stringFlags) {
        super(name, arity, isGreaterThanArity, stringFlags);
    }

    /**
     * 这个函数是 EXPIRE 、 PEXPIRE 、 EXPIREAT 和 PEXPIREAT 命令的底层实现函数。
     * 命令的第二个参数可能是绝对值，也可能是相对值。
     * 当执行 *AT 命令时， basetime 为 0 ，在其他情况下，它保存的就是当前的绝对时间。
     *
     * @param client
     * @param baseTime  basetime参数则总是毫秒格式的
     * @param unit 时间单位second/millisecond
     */
    protected void genericExpire(InnerClient client, long baseTime, TimeUnit unit) {
        List<Sds> commandArgs = client.getCommandArgs();

        Sds key = commandArgs.get(1);
        Sds expireTime = commandArgs.get(2);

        long when;

        try {
           when = Long.parseLong(expireTime.toString());
        } catch (NumberFormatException e) {
            return;
        }

        // 如果传入的是秒，则将其转成毫秒处理
        if (unit == TimeUnit.SECONDS) {
            when *= 1000;
        }
        when += baseTime;

        Database db = client.getDatabase();

        if (db.lookupByKey(key) == null) {
            client.replyInteger(0);
            return;
        }

        if (when < System.currentTimeMillis()) {
            db.delete(key);
        } else {
            db.setExpire(key, when);
        }
        client.replyInteger(1);
    }
}
