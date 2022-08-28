package database;

import common.expire.InertExpiration;
import common.expire.PeriodicExpiration;
import common.struct.ZedisObject;
import common.struct.ZedisString;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库结构
 */
public class Database {

    private Map<ZedisString, ZedisObject> keySpace;       // 数据库健空间，保存着数据库中所有的键值对, key是字符串，value是5种类型
    private Map<ZedisString, Long> expires;                // 记录键的过期时间，key为键，值为过期时间 UNIX 时间戳
    private Map<ZedisString, ZedisObject> blockingKeys;   // 正处于阻塞状态的健
    private Map<ZedisString, ZedisObject> readyKeys;      // 可以解除阻塞状态的健
    private Map<ZedisString, ZedisObject> watchedKeys;    // 正在被watch命令监视的健

    //private long id; 舍弃分库相关设计
    private long avgTtl;    // 统计信息，数据库健的评价TTL

    private Random random = new Random();

    public Database() {
        this.keySpace = new ConcurrentHashMap<>();
        this.expires = new ConcurrentHashMap<>();
        this.blockingKeys = new ConcurrentHashMap<>();
        this.readyKeys = new ConcurrentHashMap<>();
        this.watchedKeys = new ConcurrentHashMap<>();
        this.avgTtl = 0;
    }

    /**
     * 从数据库 db 中取出键 key 的值
     * 如果 key 的值存在，那么返回该值；否则，返回 NULL 。
     * @param key
     * @return
     */
    public ZedisObject lookupByKey(ZedisString key) {
        // 惰性删除策略，访问数据前如果数据到期，则进行惰性删除
        delExpiredIfNeeded(key);

        ZedisObject res = this.keySpace.get(key);

        if (res != null) {
            //todo 更新对象的访问lru时间等
            return res;
        } else {
            return null;
        }
    }

    /**
     * 尝试将键值对 key 和 val 添加到数据库中。
     * @param key
     * @param value
     */
    public void add(ZedisString key, ZedisObject value) {
        ZedisObject oldValue = this.keySpace.put(key, value);
    }

    /**
     * 高层次的 SET 操作函数。
     * 这个函数可以在不管键 key 是否存在的情况下，将它和 val 关联起来。
     * 监视键 key 的客户端会收到键已经被修改的通知
     * 键的过期时间会被移除（键变为持久的）
     * @param key
     * @param value
     */
    public void setKey(ZedisString key, ZedisObject value) {
        add(key, value);

        // 移除键的过期时间

        // 发送键修改通知
    }

    /**
     * 检查key是否存在与数据库中
     * @param key
     * @return
     */
    public boolean exists(ZedisString key) {
        // 惰性删除策略，访问数据前如果数据到期，则进行惰性删除
        delExpiredIfNeeded(key);

        return this.keySpace.containsKey(key);
    }

    /**
     * 从数据库中删除给定的键，键的值，以及键的过期时间。
     * 删除成功返回 1 ，因为键不存在而导致删除失败时，返回 0 。
     * @param key
     * @return
     */
    public boolean delete(ZedisString key) {
        // 惰性删除策略，访问数据前如果数据到期，则进行惰性删除
        delExpiredIfNeeded(key);

        ZedisObject oldVal = this.keySpace.remove(key);

        if (oldVal == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 从键空间和过期空间中移除key
     * @param key
     */
    public void remove(ZedisString key) {
        this.keySpace.remove(key);
        this.expires.remove(key);
    }

    /**
     * 清空数据库
     * @return
     */
    public void clear() {
        this.keySpace.clear();
        this.expires.clear();
    }

    public int expiredKeyNums() {
        return this.expires.size();
    }

    public void setExpire(ZedisString key, long when) {
        this.expires.put(key, when);
    }

    public long getExpire(ZedisString key) {
        // 惰性删除策略，访问数据前如果数据到期，则进行惰性删除
        delExpiredIfNeeded(key);

        // 返回key的过期时间，如果不存在，则返回-1
        return this.expires.getOrDefault(key, -1L);
    }

    public Long removeExpire(ZedisString key) {
        return this.expires.remove(key);
    }


    public boolean isExpired(ZedisString key) {
        Long expire = this.expires.get(key);
        if (expire == null) {
            return false;
        }

        if (expire <= System.currentTimeMillis()) {
            return true;
        } else {
            return false;
        }
    }

    //todo 异步删除
    public void delExpiredIfNeeded(ZedisString key) {
        if (isExpired(key)) {
            this.expires.remove(key);
            this.keySpace.remove(key);
        }
    }

    //todo 尚未实现自定义dict，所以先采用这种写法
    public Map.Entry<ZedisString, Long> randomExpire() {
        List<Map.Entry<ZedisString, Long>> keys = new ArrayList<>(this.expires.entrySet());
        Map.Entry<ZedisString, Long> randomEntry = keys.get(random.nextInt(keys.size()));
        return randomEntry;
    }



    public Map<ZedisString, ZedisObject> getKeySpace() {
        return this.keySpace;
    }
}
