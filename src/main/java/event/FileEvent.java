package event;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;

/**
 * FileEvent用于表示对IO事件的抽象
 * 在事件循环中，会基于nio的channel来处理TCP通信，每个channel都表示一个网络IO通信
 * 一个FileEvent就表示了对一个channel的监听及事件抽象
 * FileEvent类中记录了当前channel监听的事件类型集合、对应事件发生时的处理函数、客户端数据这些属性
 *
 * 由于存在不同类型的channel（ServerSocketChannel、SocketChannel），这些channel会监听不同的事件，总共有4种：
 *     SelectionKey.OP_ACCEPT
 *     SelectionKey.OP_READ
 *     SelectionKey.OP_WRITE
 *     SelectionKey.OP_CONNECT
 * 这四类事件用SelectionKey中对应的int值表示
 *
 * 对应每种事件发生时的处理函数则记录在以这些事件int值为key的map中
 *
 * @author: zzz
 * @create: 2021-08-27
 */
public class FileEvent {
    private int ops;    // 该FileEvent对应的channel监听的事件类型集合（不同事件int值的或操作结果）
    private Map<Integer, FileEventHandler> eventHandlerMap;    // 记录了对应事件类型的处理函数，key为事件类型，value为处理函数
    private Object clientData;     // 客户端传来的数据

    /**
     * FileEvent构造函数
     *
     * @param interestOp 表示监听的事件类型，为了方便实现，这里只传入单个事件类型，而不是将多个事件类型的int值进行或操作后一起传入
     * @param handler 对应事件类型的处理函数对象
     * @param clientData 客户端数据
     */
    public FileEvent(int interestOp, FileEventHandler handler, Object clientData) {
        this.eventHandlerMap = new HashMap<>(4);
        eventHandlerMap.put(interestOp, handler);
        this.ops = interestOp;
        this.clientData = clientData;
    }

    /**
     * 获取当前channel关注的事件类型
     *
     * @return 当前channel关注的事件类型集合
     */
    public int getInterestSet() {
        return ops;
    }

    /**
     * 获取当前channel的客户端数据
     *
     * @return 客户端数据
     */
    public Object getClientData() {
        return this.clientData;
    }

    /**
     * 获取该FileEvent中处理特定类型事件的处理函数
     *
     * @param eventType 事件类型，即SelectionKey的（OP_ACCEPT/OP_CONNECT/OP_READ/OP_WRITE）值之一
     * @return 处理对象
     */
    public FileEventHandler getEventHandler(int eventType) {
        return eventHandlerMap.get(eventType);
    }

    /**
     * 更新FileEvent中记录的监听事件类型和处理函数等
     * 同一个channel可以监听多种类型，因此，对应的FileEvent中也会记录其监听的所有事件类型集合和处理函数
     * 由于为了方便实现，设置成每次只能更新一个事件类型和其处理函数
     * 当channel监听新类型事件时，就可以调用该函数，更新FileEvent中记录的信息
     *
     * @param interestOp
     * @param handler
     * @param clientData
     */
    public void addFileEventHandler(int interestOp, FileEventHandler handler, Object clientData) {
        this.ops |= interestOp;
        this.eventHandlerMap.put(interestOp, handler);
        this.clientData = clientData;
    }

    public void removeFileEventHandler(int uninterestOp) {
        this.ops = this.ops & ~uninterestOp;
        this.eventHandlerMap.remove(uninterestOp);
    }

    public boolean isEmptyFileEvent() {
        boolean ans = true;
        ans &= (((this.ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) ? false : true);
        ans &= (((this.ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) ? false : true);
        ans &= (((this.ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) ? false : true);
        ans &= (((this.ops & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) ? false : true);
        return ans;
    }

    // 由于FileEvent对象会放入到Map之类的容器中，
    // 为了防止出现异常，需要重写FileEvent类的hashCode方法和equals方法

    @Override
    public int hashCode(){
        int hashcode = 0;

        hashcode = hashcode * 31 + this.ops;
        hashcode = hashcode * 31 + this.clientData.hashCode();
        hashcode = hashcode * 31 + this.eventHandlerMap.hashCode();

        return hashcode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null) {
            return false;
        }

        if (other instanceof FileEvent) {
            FileEvent fo = (FileEvent) other;
            return (fo.ops == this.ops
                    && fo.clientData.equals(this.clientData)
                    && fo.eventHandlerMap.equals(this.eventHandlerMap) )
                    ? true : false;
        } else {
            return false;
        }
    }
}
