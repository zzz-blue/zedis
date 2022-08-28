package event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import server.ZedisServer;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.function.Function;

/**
 * 事件循环
 */
public class EventLoop {
    private static final Log logger = LogFactory.getLog(EventLoop.class);

    // 表示事件循环是否进行的标识
    private volatile boolean stop;
    // nio核心组件，用于监听多个channel上发生的事件
    private Selector selector;
    // io事件表，表的key是每一个channel注册到该selector生成的SelectionKey
    // 表的value是个FileEvent结构，该结构中也是一个map，保持了该channel上4中类型发生时的处理器
    private Map<SelectionKey, FileEvent> fileEvents;
    // 时间事件列表，每次事件循环都会重中取出最近的时间事件进行处理
    private LinkedList<TimeEvent> timeEvents;

    private Procedure<EventLoop> beforeSleep;

    private EventLoop() {
        this.stop = false;
        this.timeEvents = new LinkedList<>();
        this.fileEvents = new HashMap<>();

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            logger.fatal("Can not open Selector to handel nio event", e);
        }
    }

    /**
     * 工厂方法，用于创建EventLoop对象
     * @return EventLoop对象
     */
    public static EventLoop createEventLoop() {
        return new EventLoop();
    }

    /**
     * 事件循环
     */
    public void eventLoopMain() {
        this.stop = false;

        while (!stop) {
            // 如果有需要在事件处理之前执行的函数，就执行
            if (this.beforeSleep != null) {
                beforeSleep.call(this);
            }

            // 开始处理事件
            processEvents();
        }
    }

    /**
     * 处理事件
     * 事件有两类：时间事件和文件事件
     * @return 处理的事件数量
     */
    private int processEvents() {
        TimeEvent nearestTimeEvent = this.getNearestTimer();
        long blockTime = 0;

        // 获取最近的时间事件，根据时间事件计算需要阻塞的时长
        if (nearestTimeEvent != null) {
            // 如果时间事件存在的话
            // 那么根据最近可执行时间事件和现在时间的时间差来决定文件事件的阻塞时间
            long when = nearestTimeEvent.getWhen();
            long now = System.currentTimeMillis();

            if (when <= now) {
                // 说明时间事件已经执行，不需要阻塞
                // 由于nio select无法立即返回，因此设置一个很短的时间1ms
                blockTime = -1;
            } else {
                blockTime = when - now;
            }
        } else {
            // 没有时间事件，则一直阻塞
            blockTime = 0;
        }

        // 处理文件事件
        int processed = 0;

        processed += processFileEvents(blockTime);

        // 处理时间事件
        processed += processTimeEvents();

        return processed;
    }

    /**
     * 处理文件事件
     * @param timeout Selector的超时时间
     * @return 处理的事件个数
     */
    private int processFileEvents(long timeout) {
        int processed = 0;
        try {
            int readyNums = 0;

            if (timeout == -1) {
                readyNums = this.selector.selectNow();
            } else {
                readyNums = this.selector.select(timeout);
            }

            if (readyNums > 0) {
                Set<SelectionKey> selectionKeySet = this.selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeySet.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    FileEvent firedFileEvent = this.fileEvents.get(key);

                    int interestOp = 0;
                    if (key.isAcceptable()) {
                        interestOp = SelectionKey.OP_ACCEPT;
                    } else if (key.isConnectable()) {
                        interestOp = SelectionKey.OP_CONNECT;
                    } else if (key.isReadable()) {
                        interestOp = SelectionKey.OP_READ;
                    } else if (key.isWritable()) {
                        interestOp = SelectionKey.OP_WRITE;
                    } else {
                        // 异常情况
                        logger.error("异常情况，监听到未知类型的事件");
                    }

                    FileEventHandler handler = firedFileEvent.getEventHandler(interestOp);
                    if (handler != null) {
                        handler.handle(ZedisServer.getInstance(), key, firedFileEvent.getClientData());
                    } else {
                        // 异常情况，没有相应的处理器
                        logger.error("异常情况，没有相应的处理器处理事件");
                    }

                    processed++;
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            logger.error("IO异常",e);
        }

        return processed;
    }


    /**
     * 注册新的监听事件
     * @param channel 要注册监听事件的channel
     * @param interestOp 监听的事件类型
     * @param handler 相应事件发生时的处理器
     * @param clientData 客户端数据
     */
    public void registerFileEvent(SelectableChannel channel, int interestOp, FileEventHandler handler, Object clientData) {
        // 参数检查
        if ((interestOp & channel.validOps()) == 0) {
            // 事件类型必须是channel支持的事件类型之一
            throw new IllegalArgumentException("Illegal argument 'interestOp','interestOp' is a invalid event type in 'channel'");
        }

        if (channel == null) {
            throw new IllegalArgumentException("Illegal argument 'channel', 'interestOp' can not be null");
        }

        if (handler == null) {
            throw new IllegalArgumentException("Illegal argument 'handler', 'handler' can not be null");
        }


        // 注意：使用channel.register()和key.interstOps()都会使用新传入的事件集合替换原来的事件集合
        // 所以要更新当前channel上的事件集合（入增加或删除某个事件），必须在外部子集生成更新后的事件集合，才传入channel或key来覆盖原来的实现

        // 检查channel是否在该selector上注册过
        SelectionKey registedKey = channel.keyFor(selector);

        if (registedKey == null) {
            // 该channel没有在该selector上注册过，现在是第一次注册
            try {
                registedKey = channel.register(this.selector, interestOp);
            } catch (ClosedChannelException e) {
                logger.error("register file event error, the channel has closed", e);
            }
        } else {
            // 该channel已经在该selector上注册过，要注册新事件，需要更新对应key的事件集合

            // 更新channel在该selector上监听的事件集合
            int newInterestOps = registedKey.interestOps() | interestOp;
            registedKey.interestOps(newInterestOps);
        }

        // 更新完底层selector注册的事件类型，还要更新FileEvent中注册的事件类型，使FileEvent和SelectionKey中的事件类型保持一致

        // 先判断该channel是否注册过，是否有相应的fileEvent结构
        FileEvent fileEvent = this.fileEvents.get(registedKey);

        if (fileEvent == null) {
            // 该channel是第一次注册
            fileEvent = new FileEvent(interestOp, handler, clientData);
        } else {
            // 该channel已经注册过，则要更新其中的事件类型和相应的处理器
            fileEvent.addFileEventHandler(interestOp, handler, clientData);
        }

        // 更新注册的channel和其事件及处理器
        this.fileEvents.put(registedKey, fileEvent);
    }

    /**
     * 删除注册在某个channel上的某个类型的事件监听及其处理器
     * @param key 表示channel注册的SelectionKey对象
     * @param uninterestOp 要取消注册的事件类型
     */
    public void unregisterFileEvent(SelectionKey key, int uninterestOp) {
        // 参数检查
        if (key == null) {
            throw new IllegalArgumentException();
        }

        if ((key.channel().validOps() & uninterestOp) == 0) {
            throw new IllegalArgumentException("Illegal argument 'uninterestOp','uninterestOp' is a invalid event type in channel represent by 'key'");
        }

        // 先在底层selector上取消对该channel上这类事件的监听
        int newInterestOps = key.interestOps() & ~uninterestOp;
        key.interestOps(newInterestOps);

        // 再将channel对应的FileEvent结构中的事件类型及处理器清除

        // 获取该key表示的channel对应的FileEvent结构
        FileEvent fileEvent = this.fileEvents.get(key);

        // 若没有fileEvent结构，就不用删除，直接返回
        if (fileEvent == null) {
            return;
        }

        // 验证FileEvent结构中是否注册了该事件，若本来就没有，就不用删除，直接返回
        if ((fileEvent.getInterestSet() & uninterestOp) != uninterestOp) {
            return;
        }

        // 从FileEvent结构中移除该类型的事件及其处理器
        fileEvent.removeFileEventHandler(uninterestOp);

        // 判断是否在该channel上未监听任何事件，若没有监听任何事件，则将相应的key和FileEvent结构删除
        if (key.interestOps() == 0) {
            this.fileEvents.remove(key);
            key.channel();
        }
    }

    /**
     * 处理时间事件（暂未实现）
     * @return
     */
    private int processTimeEvents() {
        int processed = 0;

        Iterator<TimeEvent> iterator = timeEvents.iterator();

        while (iterator.hasNext()) {
            TimeEvent e = iterator.next();

            // 当前时间到达事件的时间, 则可以处理这些事件
            if (e.getWhen() <= System.currentTimeMillis()) {
                e.execute();

                if (e instanceof CycleTimeEvent) {
                    ((CycleTimeEvent) e).resetFireTime();
                } else {
                    iterator.remove();
                }
            }
        }

        return processed;
    }

    public void registerTimeEvent(TimeEvent event) {
        timeEvents.add(event);
    }

    private TimeEvent getNearestTimer() {
        TimeEvent nearestTimer = null;

        if (!timeEvents.isEmpty()) {
            for (TimeEvent e : timeEvents) {
                if (nearestTimer == null) {
                    nearestTimer = e;
                } else {
                    if (e.getWhen() < nearestTimer.getWhen()) {
                        nearestTimer = e;
                    }
                }
            }
        }

        return nearestTimer;
    }

    /**
     * 用于测试： 获取注册的文件事件map
     * @return
     */
    public Map<SelectionKey, FileEvent> getFileEvents() {
        return Collections.unmodifiableMap(this.fileEvents);
    }

    public Selector getSelector() {
        return this.selector;
    }

    public void setBeforeSleep(Procedure<EventLoop> beforeSleep) {
        this.beforeSleep = beforeSleep;
    }

    public Procedure<EventLoop> getBeforeSleep() {
        return this.beforeSleep;
    }
}