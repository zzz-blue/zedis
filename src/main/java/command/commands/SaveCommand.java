package command.commands;

import command.AbstractCommand;
import common.constants.StatusConstants;
import common.persistence.RDBPersistence;
import server.ServerContext;
import server.client.InnerClient;


public class SaveCommand extends AbstractCommand {
    public SaveCommand() {
        super("save", 1, false, "arg");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        RDBPersistence rdbPersistence = ServerContext.getContext().getServerInstance().getRdbPersistence();
        // BGSAVE 已经在执行中，不能再执行 SAVE
        // 否则将产生竞争条件
        if (rdbPersistence.isInBackgroundSaveProcess()) {
            client.replyError("Background save already in progress");
            return;
        }

        if (rdbPersistence.save()) {
            client.replyStatus(StatusConstants.OK_STATUS);
        } else {
            client.replyError("");
        }

    }
}
