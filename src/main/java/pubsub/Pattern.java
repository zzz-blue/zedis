package pubsub;

import server.client.Client;

import java.util.List;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/6
 **/
public interface Pattern {
    String getName();
    void register(Client client);
    void unRegister(Client client);
    boolean isNoSubscriber();
    List<Client> getSubscribers();
    boolean match(String key);
}
