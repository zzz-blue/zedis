package common.struct;

/**
 * Zedis中的Sting类型
 * @author: zzz
 * @create: 2021-09-28
 */
public interface ZedisString extends ZedisObject {
    int length();
    char charAt(int index);
    boolean isEmpty();
    int indexOf(char c);
    int indexOf(int start, char c);
    void append(char c);
    void append(ZedisString str);
    void append(String str);
    void cut(int start, int end);
}
