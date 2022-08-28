package pubsub;

import server.client.Client;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/5
 **/
public class PubSubChannel implements Channel {
    private String name;
    private List<Client> subsrcibedClients;

    public PubSubChannel(String name) {
        this.name = name;
        this.subsrcibedClients = new LinkedList<>();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void register(Client client) {
        if (!this.subsrcibedClients.contains(client)) {
            this.subsrcibedClients.add(client);
        }
    }

    @Override
    public void unRegister(Client client) {
        this.subsrcibedClients.remove(client);
    }

    @Override
    public boolean isNoSubscriber() {
        return this.subsrcibedClients.isEmpty();
    }

    @Override
    public int getSubscriberNums() {
        return this.subsrcibedClients.size();
    }

    @Override
    public List<Client> getSubscribers() {
        return this.subsrcibedClients;
    }




}
