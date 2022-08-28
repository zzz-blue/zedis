package event;


public interface TimeEvent {
    void execute();
    long getWhen();
}
