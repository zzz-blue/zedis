package command.commands;

import server.client.InnerClient;
import command.AbstractCommand;
import common.struct.ZedisObject;
import common.struct.ZedisString;
import common.struct.impl.Sds;
import database.Database;

import java.util.List;


public class AppendCommand extends AbstractCommand {

    public AppendCommand() {
        super("append", 3, false, "wm");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        Database db = client.getDatabase();
        long newLength = 0;

        List<Sds> commandArgs = client.getCommandArgs();

        Sds key = commandArgs.get(1);
        Sds appendContent = commandArgs.get(2);

        ZedisObject value = db.lookupByKey(key);

        if (value == null) {
            // 键值不存在，则创建一个新的
            db.add(key, appendContent);
            newLength = appendContent.length();
        } else {
            if (!(value instanceof ZedisString)) {
                return;
            }

            newLength = ((ZedisString) value).length() + appendContent.length();
            if (newLength > 1024 * 1024 * 512) {
                client.replyError("string exceeds maximum allowed size (512MB)");
                return;
            }

            ((ZedisString) value).append(appendContent);
            newLength = ((ZedisString) value).length();
        }

        client.replyInteger(newLength);
    }
}
