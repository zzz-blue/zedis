package command.commands;

import server.client.InnerClient;
import command.AbstractCommand;
import common.struct.ZedisString;
import common.constants.StatusConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import database.Database;

import java.util.concurrent.TimeUnit;


public abstract class GenericSetCommand extends AbstractCommand {
    private static Log logger = LogFactory.getLog(GenericSetCommand.class);

    //  表示set参数的flags
    public static final int SET_NO_FLAGS = 0;   // 没有标记
    public static final int SET_NX = 1 << 0;    // 当key不存在时可以set
    public static final int SET_XX = 1 << 1;    // 当key存在时可以set

    public GenericSetCommand(String name, int arity, boolean isGreaterThanArity, String stringFlags) {
        super(name, arity, isGreaterThanArity, stringFlags);
    }

    protected void genericSet(InnerClient client, int flags, ZedisString key, ZedisString value, ZedisString expireTime, TimeUnit unit) {
        long milliseconds = 0;

        // 如果设置了过期时间，则先解析过期时间
        if (expireTime != null) {
            // 取出expire参数的值
            try {
                milliseconds = Long.parseLong(expireTime.toString());
            } catch (NumberFormatException e) {
                client.replyError("value is not an integer or out of range");
                return;
            }

            // 验证expire参数的值
            if (milliseconds <= 0) {
                client.replyError("invalid expire time in SETEX");
                return;
            }

            // 不论输入的过期时间是秒还是毫秒
            // Redis 实际都以毫秒的形式保存过期时间
            // 如果输入的过期时间为秒，那么将它转换为毫秒
            if (unit == TimeUnit.SECONDS) {
                milliseconds *= 1000;
            }
        }

        // 如果设置了 NX 或者 XX 参数，那么检查条件是否不符合这两个设置，在条件不符合时报错
        Database database = client.getDatabase();
        if (nxExist(flags) && database.lookupByKey(key) != null
            || xxExist(flags) && database.lookupByKey(key) == null) {
            client.replyNil();
            return;
        }

        // 将键值关联到数据库
        database.setKey(key, value);

        // todo
        // 将数据库设为脏

        // 为键设置过期时间
        if (expireTime != null) {
            database.setExpire(key, System.currentTimeMillis() + milliseconds);
        }

        // todo
        // 发送事件通知

        // 设置成功，向客户端发送回复
        // 回复的内容由 ok_reply 决定
        client.replyBulk(StatusConstants.OK_STATUS);
    }

    protected boolean nxExist(int flags) {
        return (flags & SET_NX) != 0;
    }

    protected boolean xxExist(int flags) {
        return (flags & SET_XX) != 0;
    }

}
