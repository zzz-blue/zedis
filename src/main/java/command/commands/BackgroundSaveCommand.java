package command.commands;

import command.AbstractCommand;
import common.persistence.RDBPersistence;
import server.ServerContext;
import server.client.InnerClient;


public class BackgroundSaveCommand extends AbstractCommand {
    public BackgroundSaveCommand() {
        super("bgsave", 1, false, "ar");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        RDBPersistence rdbPersistence = ServerContext.getContext().getServerInstance().getRdbPersistence();

        // 不能重复执行BGSAVE
        if (rdbPersistence.isInBackgroundSaveProcess()) {
            client.replyError("Background save already in progress");
            return;
        } else {
            // 执行BGSAVE
            if (!rdbPersistence.backgroundSave()) {
                client.replyStatus("Background saving started");
            }
        }

    }
}
