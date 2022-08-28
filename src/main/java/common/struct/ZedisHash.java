package common.struct;

/**
 * Zedis中的Dict接口
 *
 */
public interface ZedisHash<K, V> extends ZedisObject {
    int size();
    boolean isEmpty();
    boolean containsKey(Object key);
    boolean containsValue(Object value);
    V get(Object key);
    void put(K key, V value);
    V remove(Object key);
    void clear();
}
