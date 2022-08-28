package event;

import common.expire.PeriodicExpirator;
import common.persistence.AOFPersistence;
import server.ZedisServer;
import server.ServerContext;

/**
 * 对应redis中serverCron函数，用于全局周期性处理服务器状态
 * @Author zzz
 * @Date 2021/12/4
 **/
public class GlobalCycleTimeEvent implements CycleTimeEvent {

    private long when;
    private long period;

    private PeriodicExpirator periodicExpirator;    // 用于执行定期删除策略

    public GlobalCycleTimeEvent(long period) {
        this.period = period;
        this.when = System.currentTimeMillis() + period;
        this.periodicExpirator = new PeriodicExpirator();
    }



    public long getWhen() {
        return this.when;
    }

    @Override
    public void execute() {
        // 对数据库执行各种操作
        databasesCron();

        // AOF持久化
        // 考虑是否需要将 AOF 缓冲区中的内容写入到 AOF 文件中
        AOFPersistence aofPersistence = ServerContext.getContext().getServerInstance().getAofPersistence();
        if (aofPersistence.getAofFlushPostponedStart() > 0) {
            aofPersistence.flushAppendOnlyFile(false);
        }
    }

    @Override
    public CycleTimeEvent nextCycleTimeEvent() {
        return null;
    }

    @Override
    public void resetFireTime() {
        this.when = System.currentTimeMillis() + this.period;
    }

    /**
     * 对数据库执行删除过期键、调整大小、以及主动和渐进式hash
     */
    private void databasesCron() {
        // 如果服务器不是从服务器，那么执行主动过期键清除
        ZedisServer server = ServerContext.getContext().getServerInstance();
        if (server.getServerConfig().isActiveExpiredEnable() && server.getMasterHost() == null) {
            delExpiredPeriodicaly(PeriodicExpirator.SLOW_MODE);
        }
    }

    public void delExpiredPeriodicaly(int type) {
        this.periodicExpirator.delExpiredPeriodicaly(type);
    }
}
