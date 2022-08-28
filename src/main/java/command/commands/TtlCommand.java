package command.commands;

import server.client.InnerClient;

import java.util.concurrent.TimeUnit;


public class TtlCommand extends GenericTtlCommand {
    public TtlCommand() {
        super("ttl", 2, false, "r");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        genericTtl(client, TimeUnit.SECONDS);
    }
}
