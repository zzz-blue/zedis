package common.expire;

import common.struct.ZedisString;
import database.Database;
import server.ServerContext;
import server.config.ServerConfig;

import java.util.Map;


public class PeriodicExpirator implements PeriodicExpiration {
    private boolean timeLimitExit;   //
    private Database db;     // 数据库

    public static final int EXPIRE_CYCLE_SLOW_TIME_PER_CALL = 25;   // 默认为 25 ，也即是 25 % 的 CPU 时间
    public static final int EXPIRE_CYCLE_LOOKUPS_PER_LOOP = 20;


    public PeriodicExpirator() {
        db = ServerContext.getContext().getDatabases();
    }

    @Override
    public void delExpiredPeriodicaly(int mode) {
        // 函数开始的时间
        long startTime = System.currentTimeMillis();

        // 记录迭代次数
        int iteration = 0;






        ServerConfig config = ServerContext.getContext().getServerConfig();


        // 确定函数处理的微秒时间上限，不能让出来过期键的过程占用太长时间
        // EXPIRE_CYCLE_SLOW_TIME_PER_CALL 默认为 25 ，也即是 25 % 的 CPU 时间
        int serverHz = config.getHz();
        long timeLimit = 1000000 * EXPIRE_CYCLE_SLOW_TIME_PER_CALL / serverHz / 100;

        timeLimitExit = false;

        if (timeLimit < 0) {
            timeLimit = 1;
        }

        // 如果是运行在快速模式之下
        // 那么最多只能运行 FAST_DURATION 微秒
        // 默认值为 1000 （微秒）
        if (mode == FAST_MODE) {
            timeLimit = EXPIRE_CYCLE_FAST_DURATION; /* in microseconds. */
        }





        int expired = 0;
        do {
            long now = System.currentTimeMillis();

            // 获取数据库中带过期时间的键的数量
            // 如果该数量为 0 ，跳出循环
            int expiredKeyNums = db.expiredKeyNums();
            if (expiredKeyNums == 0) {
                break;
            }

            // 每次最多只能检查 LOOKUPS_PER_LOOP 个键
            if (expiredKeyNums > EXPIRE_CYCLE_LOOKUPS_PER_LOOP) {
                expiredKeyNums = EXPIRE_CYCLE_LOOKUPS_PER_LOOP;
            }

            // 开始遍历数据库
            while ((expiredKeyNums--) > 0) {
                // 从过期键map中随机取值验证
                Map.Entry<ZedisString, Long> randomEntry = db.randomExpire();

                ZedisString key = randomEntry.getKey();
                Long expiresTime = randomEntry.getValue();

                // 如果键已经过期，那么删除它，并将 expired 计数器增一
                if (expiresTime <= now) {
                    db.remove(key);
                    expired++;
                }
            }

            // 我们不能用太长时间处理过期键，
            // 所以这个函数执行一定时间之后就要返回
            // 更新遍历次数
            iteration++;

            // 每遍历 16 次执行一次
            if ((iteration & 0xf) == 0
                && (System.currentTimeMillis() - startTime) > timeLimit) {
                // 如果遍历次数正好是 16 的倍数
                // 并且遍历的时间超过了 timeLimit
                // 那么断开 timeLimitExit
                timeLimitExit = true;
            }

            // 已经超时了，返回
            if (timeLimitExit) {
                return;
            }

            // 如果已删除的过期键占当前总数据库带过期时间的键数量的 25 %
            // 那么不再遍历
        } while (expired > EXPIRE_CYCLE_LOOKUPS_PER_LOOP / 4);

    }
}
