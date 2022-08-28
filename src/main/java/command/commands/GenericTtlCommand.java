package command.commands;

import command.AbstractCommand;
import common.struct.impl.Sds;
import database.Database;
import server.client.InnerClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/3
 **/
public abstract class GenericTtlCommand extends AbstractCommand {
    public static final long KEY_NOT_EXISTS = -2L;
    public static final long KEY_NOT_EXPIRE = -1L;

    public GenericTtlCommand(String name, int arity, boolean isGreaterThanArity, String stringFlags) {
        super(name, arity, isGreaterThanArity, stringFlags);
    }

    public void genericTtl(InnerClient client, TimeUnit unit) {
        List<Sds> commandArgs = client.getCommandArgs();

        Sds key = commandArgs.get(1);

        Database db = client.getDatabase();

        // 如果key不存在则返回-2
        if (db.lookupByKey(key) == null) {
            client.replyInteger(KEY_NOT_EXISTS);
            return;
        }

        // 返回key的过期时间，如果没有过期时间，则返回-1
        long expire = db.getExpire(key);

        long ttl = KEY_NOT_EXPIRE;
        // 存在过期时间
        if (expire != KEY_NOT_EXPIRE) {
            ttl = expire - System.currentTimeMillis();
            //源码即如此
            if (ttl < 0) {
                ttl = 0;
            }
        }

        // 不存在过期时间
        if (ttl == KEY_NOT_EXPIRE) {
            client.replyInteger(KEY_NOT_EXPIRE);
        } else {
            if (unit == TimeUnit.SECONDS) {
                client.replyInteger((ttl + 500) / 1000);
            } else if (unit == TimeUnit.MILLISECONDS) {
                client.replyInteger(ttl);
            }
        }
    }
}
