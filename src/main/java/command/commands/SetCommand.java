package command.commands;

import server.client.InnerClient;
import common.struct.ZedisString;
import common.struct.impl.Sds;
import common.constants.ErrorConstants;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * set命令
 * 命令格式：SET key value [NX] [XX] [EX <seconds>] [PX <milliseconds>]
 */
public class SetCommand extends GenericSetCommand {

    public SetCommand() {
        super("set", 3, true, "wm");
    }

    @Override
    public boolean checkCommandArgs(InnerClient client) {
        return true;
    }

    @Override
    public void doExecute(InnerClient client) {
        List<Sds> commandArgs = client.getCommandArgs();

        // set命令的参数标志，由于set命令有许多参数，flags中记录了当前命令使用了哪些标志
        int flags = SET_NO_FLAGS;

        // 解析参数，记录标记
        Sds expireTime = null;                  // 超时时间
        TimeUnit timeUnit = TimeUnit.SECONDS;   // 超时时间的单位


        // 从下标3开始解析，解析set命令的其他选项（NX XX EX PX）
        for (int i = 3; i < commandArgs.size(); i++) {
            // 当前待解析的选项
            Sds option = commandArgs.get(i);
            // 当前待解析选项的后一个选项
            Sds nextOption = (i == commandArgs.size() - 1) ? null : commandArgs.get(i + 1);

            // 判断当前解析到的选项是哪个
            if (option.length() == 2
                    && (option.charAt(0) == 'n' || option.charAt(0) == 'N')
                    && (option.charAt(1) == 'x' || option.charAt(1) == 'X')) {
                // 当前选项是NX选项
                flags |= SET_NX;
            } else if (option.length() == 2
                    && (option.charAt(0) == 'x' || option.charAt(0) == 'X')
                    && (option.charAt(1) == 'x' || option.charAt(1) == 'X')) {
                // 当前选项是XX
                flags |= SET_XX;
            } else if (option.length() == 2
                    && (option.charAt(0) == 'e' || option.charAt(0) == 'E')
                    && (option.charAt(1) == 'x' || option.charAt(1) == 'X')
                    && nextOption != null) {
                // 当前选项是ex且携带了时间参数
                timeUnit = TimeUnit.SECONDS;
                expireTime = nextOption;
                i++;
            } else if (option.length() == 2
                    && (option.charAt(0) == 'p' || option.charAt(0) == 'P')
                    && (option.charAt(1) == 'x' || option.charAt(1) == 'X')
                    && nextOption != null) {
                timeUnit = TimeUnit.MILLISECONDS;
                expireTime = nextOption;
                i++;
            } else {
                client.replyError(ErrorConstants.SYNTAX_ERROR);
                return;
            }
        }

        // 调用通用的set方法，将key/value保存
        ZedisString key = commandArgs.get(1);
        ZedisString value = commandArgs.get(2);

        genericSet(client, flags, key, value, expireTime, timeUnit);
    }


}
