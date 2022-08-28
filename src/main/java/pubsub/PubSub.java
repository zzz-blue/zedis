package pubsub;

import common.struct.ZedisString;
import server.client.Client;
import server.client.InnerClient;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/5
 **/
public class PubSub {
    // 记录了服务器中所有的channel，每个channel中包含订阅该chanle的client列表
    private Map<String, Channel> pubSubChannels;

    // 记录了所有订阅的pattern
    private Map<String, Pattern> pubSubPatterns;

    public PubSub() {
        this.pubSubChannels = new ConcurrentHashMap<>();
        this.pubSubPatterns = new ConcurrentHashMap<>();
    }

    public int unsubscribeChannel(Client client, String channelName, boolean notify) {
        int retval = 0;


        InnerClient innerClient = (InnerClient)client;
        if (innerClient.unsubscribeChannel(channelName)) {
            // channel 移除成功，表示客户端订阅了这个频道，执行以下代码
            retval = 1;

            Channel channel = this.pubSubChannels.get(channelName);
            channel.unRegister(client);


            if (channel.isNoSubscriber()) {
                this.pubSubChannels.remove(channelName);
            }
        }

        // 回复客户端
        if (notify) {
            List<String> multiReply = new LinkedList<>();
            multiReply.add("unsubscribe");
            multiReply.add(channelName);
            multiReply.add(String.valueOf(innerClient.getPubSubChannels().size() + innerClient.getPubSubPatterns().size()));
            innerClient.replyMultiBulk(multiReply);
        }

        return retval;
    }

    public int unsubscribeAllChannels(Client client, boolean notify) {
        Set<String> channels = ((InnerClient)client).getPubSubChannels().keySet();

        String [] c = channels.toArray(new String[0]);

        // 遍历该client订阅的所有channel，将所有channel都取消订阅
        int count = 0;
        for (int i = 0; i < c.length; i++) {
            count += unsubscribeChannel(client, c[i], notify);
        }

        // 如果在执行这个函数时，客户端没有订阅任何频道，
        // 那么向客户端发送回复
        if (notify && count == 0) {
            InnerClient innerClient = (InnerClient)client;
            List<String> multiReply = new LinkedList<>();
            multiReply.add("unsubscribe");
            multiReply.add("nil");
            multiReply.add(String.valueOf(innerClient.getPubSubChannels().size() + innerClient.getPubSubPatterns().size()));
            innerClient.replyMultiBulk(multiReply);
        }

        return count;
    }

    public void subscribeChannel(Client client, String channelName) {

        Channel channel = null;
        if (this.pubSubChannels.containsKey(channelName)) {
            channel = this.pubSubChannels.get(channelName);
        } else {
            // 如果 channel 不存在于字典，那么添加进去
            channel = new PubSubChannel(channelName);
            this.pubSubChannels.put(channelName, channel);
        }

        // 将客户端添加到链表的末尾
        channel.register(client);
        // 在该客户端自己的订阅列表中也将该channel注入
        ((InnerClient)client).subscribeChannel(channel);

        // 回复客户端。
        // 示例：
        // redis 127.0.0.1:6379> SUBSCRIBE xxx
        // Reading messages... (press Ctrl-C to quit)
        // 1) "subscribe"
        // 2) "xxx"
        // 3) (integer) 1
        InnerClient innerClient = (InnerClient)client;
        List<String> multiReply = new LinkedList<>();
        multiReply.add("subscribe");
        multiReply.add(channelName);
        multiReply.add(String.valueOf(innerClient.getPubSubChannels().size() + innerClient.getPubSubPatterns().size()));
        innerClient.replyMultiBulk(multiReply);
    }

    public void subscribePattern(Client client, String patternName) {

        Pattern pattern = null;
        if (this.pubSubPatterns.containsKey(patternName)) {
            pattern = this.pubSubPatterns.get(patternName);
        } else {
            pattern = new PubSubPattern(patternName);
            this.pubSubPatterns.put(patternName, pattern);
        }

        // 将 pattern 添加到 c->pubsub_patterns 链表中
        pattern.register(client);
        InnerClient innerClient = (InnerClient)client;

        innerClient.subscribePattern(pattern);

        // 回复客户端。
        // 示例：
        // redis 127.0.0.1:6379> PSUBSCRIBE xxx*
        // Reading messages... (press Ctrl-C to quit)
        // 1) "psubscribe"
        // 2) "xxx*"
        // 3) (integer) 1
        List<String> multiReply = new LinkedList<>();
        multiReply.add("psubscribe");
        multiReply.add(patternName);
        multiReply.add(String.valueOf(innerClient.getPubSubChannels().size() + innerClient.getPubSubPatterns().size()));
        innerClient.replyMultiBulk(multiReply);
    }

    public int unsubscribeAllPatterns(Client client, boolean notify) {
        Set<String> patterns = ((InnerClient)client).getPubSubPatterns().keySet();

        String [] p = patterns.toArray(new String[0]);

        // 遍历该client订阅的所有channel，将所有channel都取消订阅
        int count = 0;
        for (int i = 0; i < p.length; i++) {
            count += unsubscribePattern(client, p[i], notify);
        }

        // 如果在执行这个函数时，客户端没有订阅任何频道，
        // 那么向客户端发送回复
        if (notify && count == 0) {
            InnerClient innerClient = (InnerClient)client;
            List<String> multiReply = new LinkedList<>();
            multiReply.add("punsubscribe");
            multiReply.add("nil");
            multiReply.add(String.valueOf(innerClient.getPubSubChannels().size() + innerClient.getPubSubPatterns().size()));
            innerClient.replyMultiBulk(multiReply);
        }

        return count;
    }

    public int unsubscribePattern(Client client, String patternName, boolean notify) {
        int retval = 0;

        // 将频道 Pattern 从 client->channels 字典中移除
        InnerClient innerClient = (InnerClient)client;
        if (innerClient.unsubscribePattern(patternName)) {
            // channel 移除成功，表示客户端订阅了这个频道，执行以下代码
            retval = 1;

            // 从 Pattern->clients 的 clients 链表中，移除 client
            Pattern pattern = this.pubSubPatterns.get(patternName);
            pattern.unRegister(client);

            // 如果移除 client 之后链表为空，那么删除这个 pattern 键
            if (pattern.isNoSubscriber()) {
                this.pubSubPatterns.remove(patternName);
            }
        }

        // 回复客户端
        if (notify) {
            List<String> multiReply = new LinkedList<>();
            multiReply.add("punsubscribe");
            multiReply.add(patternName);
            multiReply.add(String.valueOf(innerClient.getPubSubChannels().size() + innerClient.getPubSubPatterns().size()));
            innerClient.replyMultiBulk(multiReply);
        }

        return retval;
    }

    /**
     *
     * 将 message 发送到所有订阅频道 channel 的客户端，
     * 以及所有订阅了和 channel 频道匹配的模式的客户端。
     * @param channelName
     * @param message
     * @return
     */
    public int publishMessage(String channelName, ZedisString message) {
        int receivers = 0;

        // 取出包含所有订阅频道 channel 的客户端的链表
        // 并将消息发送给它们
        Channel channel = this.pubSubChannels.get(channelName);
        if (channel != null && !channel.isNoSubscriber()) {
            List<Client> clients = channel.getSubscribers();

            // 遍历客户端链表，将 message 发送给它们
            for (Client c : clients) {
                InnerClient client = (InnerClient)c;
                // 回复客户端。
                // 示例：
                // 1) "message"
                // 2) "xxx"
                // 3) "hello"
                List<String> multiReply = new LinkedList<>();
                multiReply.add("message");
                multiReply.add(channelName);
                multiReply.add(message.toString());
                client.replyMultiBulk(multiReply);

                receivers++;
            }
        }

        // 将消息也发送给那些和频道匹配的模式
        if (!this.pubSubPatterns.isEmpty()) {
            for (Map.Entry<String, Pattern> entry : this.pubSubPatterns.entrySet()) {
                Pattern pattern = entry.getValue();
                if (pattern.match(channelName)) {
                    List<Client> clients = pattern.getSubscribers();
                    for (Client c : clients) {
                        InnerClient client = (InnerClient)c;

                        // 回复客户端
                        // 示例：
                        // 1) "pmessage"
                        // 2) "*"
                        // 3) "xxx"
                        // 4) "hello"
                        List<String> multiReply = new LinkedList<>();
                        multiReply.add("pmessage");
                        multiReply.add(pattern.getName());
                        multiReply.add(channelName);
                        multiReply.add(message.toString());
                        client.replyMultiBulk(multiReply);

                        // 对接收消息的客户端进行计数
                        receivers++;
                    }
                }
            }
        }

        // 返回计数
        return receivers;
    }

    public Map<String, Channel> getPubSubChannels() {
        return this.pubSubChannels;
    }

    public long getPubSubPatternNums() {
        return this.pubSubPatterns.size();
    }

}
