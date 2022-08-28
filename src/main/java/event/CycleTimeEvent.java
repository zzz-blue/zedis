package event;

/**
 * @Description
 * @Author zzz
 * @Date 2021/12/4
 **/
public interface CycleTimeEvent extends TimeEvent {
    CycleTimeEvent nextCycleTimeEvent();
    void resetFireTime();
}
