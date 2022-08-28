package command.commands;

import command.AbstractCommand;
import common.struct.impl.Sds;
import database.Database;
import server.client.InnerClient;

import java.util.List;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/3
 **/
public class PersistCommand extends AbstractCommand {
    public PersistCommand() {
        super("persist", 2, false, "w");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();

        Sds key = commandArgs.get(1);

        Database db = client.getDatabase();

        if (db.lookupByKey(key) == null) {
            // 健不存在
            client.replyInteger(0);
        } else {
            if (db.removeExpire(key) != null) {
                // 删除过期时间成功
                client.replyInteger(1);
            } else {
                // 过期时间不存在
                client.replyInteger(0);
            }
        }
    }
}
