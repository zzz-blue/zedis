package remote;

import common.struct.ZedisObject;
import common.struct.impl.Sds;

/**
 * 表示服务器向客户端进行消息回复的接口
 **/
public interface Replyer {
    void reply(Reply reply, Object obj);
}
