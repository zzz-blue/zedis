package common.expire;

import common.struct.ZedisString;


public interface Expiration {
    boolean isExpired(ZedisString key);
}
