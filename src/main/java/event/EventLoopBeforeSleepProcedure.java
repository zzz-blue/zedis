package event;

import common.persistence.AOFPersistence;
import server.ServerContext;

/**
 * 每次处理事件之前执行
 * @Author zzz
 * @Date 2021/12/8
 **/
public class EventLoopBeforeSleepProcedure implements Procedure<EventLoop> {
    @Override
    public void call(EventLoop eventLoop) {
        // 将 AOF 缓冲区的内容写入到 AOF 文件
        ServerContext.getContext().getServerInstance().getAofPersistence().flushAppendOnlyFile(false);
    }
}
