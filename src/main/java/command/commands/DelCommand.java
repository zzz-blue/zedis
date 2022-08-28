package command.commands;

import server.client.InnerClient;
import command.AbstractCommand;
import common.struct.impl.Sds;
import database.Database;

import java.util.List;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/2
 **/
public class DelCommand extends AbstractCommand {
    public DelCommand() {
        super("del", 2, true, "w");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();

        Database db = client.getDatabase();

        int deleted = 0;

        for (int i = 1; i < commandArgs.size(); i++) {
            // todo
            // 先删除过期的健

            if (db.delete(commandArgs.get(i))) {
                deleted++;
            }
        }

        client.replyInteger(deleted);
    }
}
