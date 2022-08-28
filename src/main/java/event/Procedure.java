package event;



@FunctionalInterface
public interface Procedure<T> {
    void call(T t);
}
