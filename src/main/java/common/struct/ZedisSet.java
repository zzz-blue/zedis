package common.struct;

/**
 * Zedis中的Set接口
 * @description:
 * @author: zzz
 * @create: 2021-09-28
 */
public interface ZedisSet<E> extends ZedisObject {
    int size();
    boolean isEmpty();
    boolean contains(Object element);
    boolean add(E element);
    boolean remove(Object element);
    void clear();
}
