package common.expire;

import common.struct.ZedisString;


public interface InertExpiration extends Expiration {
    void delExpiredIfNeeded(ZedisString key);
}
