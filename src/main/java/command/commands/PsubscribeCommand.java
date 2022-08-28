package command.commands;

import command.AbstractCommand;
import common.struct.impl.Sds;
import pubsub.PubSub;
import server.ServerContext;
import server.client.InnerClient;

import java.util.List;


public class PsubscribeCommand extends AbstractCommand {
    public PsubscribeCommand() {
        super("psubscribe", 2, true, "rpslt");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();
        PubSub pubSub = ServerContext.getContext().getServerInstance().getPubSub();

        for (int i = 1; i < commandArgs.size(); i++) {
            pubSub.subscribePattern(client, commandArgs.get(i).toString());
        }
    }
}
