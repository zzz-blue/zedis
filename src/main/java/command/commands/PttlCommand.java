package command.commands;

import server.client.InnerClient;

import java.util.concurrent.TimeUnit;


public class PttlCommand extends GenericTtlCommand {
    public PttlCommand() {
        super("pttl", 2, false, "r");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        genericTtl(client, TimeUnit.MILLISECONDS);
    }
}
