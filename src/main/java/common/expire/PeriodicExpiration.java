package common.expire;

import common.struct.ZedisString;


public interface PeriodicExpiration {
    int FAST_MODE = 0;
    int SLOW_MODE = 1;
    int EXPIRE_CYCLE_FAST_DURATION = 1000;

    void delExpiredPeriodicaly(int mode);

}
