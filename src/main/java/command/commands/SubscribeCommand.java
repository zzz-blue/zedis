package command.commands;

import command.AbstractCommand;
import common.struct.impl.Sds;
import pubsub.PubSub;
import server.ServerContext;
import server.client.InnerClient;

import java.util.List;


public class SubscribeCommand extends AbstractCommand {
    public SubscribeCommand() {
        super("subscribe", 2, true, "rpslt");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        PubSub pubSub = ServerContext.getContext().getServerInstance().getPubSub();

        List<Sds> commandArgs = client.getCommandArgs();

        for (int i = 1; i < commandArgs.size(); i++) {
            pubSub.subscribeChannel(client, commandArgs.get(i).toString());
        }
    }
}
