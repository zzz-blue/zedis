package common.struct;

/**
 * 表示存储在Zedis中的对象
 * Zedis支持的数据结构都需要实现该接口
 */
public interface ZedisObject {
    ObjectType getType();
}
