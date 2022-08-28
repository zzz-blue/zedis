package common.struct;

/**
 * Zedis中的List接口
 * @author: zzz
 * @create: 2021-09-28
 */
public interface ZedisList<E> extends ZedisObject {
   int size();
   boolean isEmpty();
   boolean contains(Object element);
   E get(int index);
   boolean set(int index, E element);
   boolean add(E element);
   boolean add(int index, E element);
   E remove(int index);
   int indexOf(Object o);
}
