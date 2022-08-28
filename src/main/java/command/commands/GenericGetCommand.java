package command.commands;

import server.client.InnerClient;
import command.AbstractCommand;
import common.struct.ObjectType;
import common.struct.ZedisObject;
import common.struct.ZedisString;
import common.struct.impl.Sds;
import common.constants.ErrorConstants;
import database.Database;


public abstract class GenericGetCommand extends AbstractCommand {

    public GenericGetCommand(String name, int arity, boolean isGreaterThanArity, String stringFlags) {
        super(name, arity, isGreaterThanArity, stringFlags);
    }

    protected void genericGet(InnerClient client) {
        Database db = client.getDatabase();
        Sds[] args = client.getCommandArgs().toArray(new Sds[0]);

        // 尝试从数据库中取出键 c->argv[1] 对应的值对象
        // 如果键不存在时，向客户端发送回复信息，并返回 NULL
        ZedisObject value = db.lookupByKey(args[1]);
        if (value == null) {
            client.replyNil();
            return;
        }

        // 值对象存在，检查它的类型
        if (value.getType() != ObjectType.STRING) {
            // 类型错误，向客户端返回错误信息
            client.replyError(ErrorConstants.WRONG_TYPE_ERROR);
            return;
        } else {
            // 类型正确，向客户端返回对象的值
            client.replyBulk((ZedisString)value);
            return;
        }
    }
}
