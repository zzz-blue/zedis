package common.struct.impl;

import common.struct.ObjectType;
import common.struct.ZedisString;
import remote.protocol.Protocol;
import common.utils.SafeEncoder;

import java.util.Arrays;

/**
 * 动态字符串
 * 线程不安全
 * @author: zzz
 * @create: 2021-09-28
 */
public class Sds implements ZedisString, Comparable<Sds> {
    /**
     * 为了能够保存二进制数据，这里使用byte数组保存字符
     * 由于java中char类型的内码表示为utf-16类型，需要两个字节
     * 这里为了简化，就使用一个byte来表示字符，这导致只能保存只占1个字节的字符，及latin编码的字符
     */
    private byte [] buf;    // 字节数据 用于保存字符串

    /**
     * len和free一同表示了Sds的容量情况，当修改其中一个时，一定要记得同时修改另一个属性，否则可能会出现不一致的情况
     */
    private int len;        // 表示字符串长度
    private int free;       // 记录buf数组中未使用的字节长度

    public static final int SDS_MAX_PREALLOC = 1024 * 1024; // 1MB
    public static final int SDS_DEFAULT_LENGTH = 1024;      // 1KB

    /**
     * 构造函数
     * 为了节省空间，空Sds串的len和free都是0，内部字节数组为空数组
     */
    public Sds() {
        this(0, 0, null);
    }

    /**
     * 构造函数
     * 内部保存数据的字节数组大小由_len + _free决定
     * 如果传入的_buf数组比_len + _free大，那么会截取[0, _len + _free)这部分内容保存
     * @param _len Sds中字符串长度
     * @param _free Sds中空闲字节数
     * @param _buf Sds中实际存储数组
     */
    public Sds(int _len, int _free, byte[] _buf) {
        this.len = _len < 0 ? 0 : _len;     // _len不能小于0，这里进行检查
        this.free = _free < 0 ? 0 : _free;  // _free不能小于0，这里进行检查

        this.buf = new byte[_len + _free];
        if (_buf != null) {
            int copyLength = Math.min(_len, _buf.length);
            System.arraycopy(_buf, 0, this.buf, 0, copyLength);
        }
    }

    /**
     * Sds最基础的工厂方法，可以指定创建的字符串长度、空余空间和字符串初始内容
     * @param len 创建的字符串内容长度
     * @param free 字符串空余空间长度
     * @param init 初始内容
     * @return 新的字符串
     */
    public static Sds createSds(int len, int free, byte[] init) {
        return new Sds(len, free, init);
    }

    /**
     * 根据给定的初始化字符串 init 创建长度为 initlen 的Sds，超出initLen的init数组内容会被截断
     * @param init 初始化字符串
     * @param initlen 初始化字符串的长度
     * @return 返回相应的 sds
     */
    public static Sds createSds(int initlen, byte[] init) {
        if (initlen < 0) {
            throw new IllegalArgumentException("Sds initlen is " + initlen + ", should be greater than or equal to 0.");
        } else if (initlen == 0) {
            return new Sds(0, 0, null);
        }

        int contentLength = (init == null ? 0 : Math.min(init.length, initlen));
        return new Sds(contentLength, initlen - contentLength, init);
    }

    /**
     * 根据给定字符串 init ，创建一个包含同样字符串的 sds
     * @param init 给定字符串 init
     * @return
     */
    public static Sds createSds(byte[] init) {
        int initlen = (init == null ? 0 : init.length);
        return createSds(initlen, init);
    }

    /**
     * 根据给定String创建Sds
     * @param str
     * @return
     */
    public static Sds createSds(String str) {
        byte [] bytes = SafeEncoder.encode(str);
        return createSds(bytes);
    }

    /**
     * 创建并返回一个空 sds
     * @return  相对应的 sds
     */
    public static Sds createEmptySds() {
        return new Sds();
    }

    /******************************************
     * ZedisObject接口的方法
     ******************************************/

    @Override
    public ObjectType getType() {
        return ObjectType.STRING;
    }

    /******************************************
     * ZedisString接口的方法
     ******************************************/

    @Override
    public int length() {
        return this.len;
    }

    @Override
    public char charAt(int index) {
        if (index >= this.len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (char)this.buf[index];
    }

    @Override
    public boolean isEmpty() {
        return this.len == 0 ? true : false;
    }

    /**
     * 从下标0开始查找字符c的位置下标
     * @param c 查找的字符
     * @return 字符c从start开始首次出现的字符下标，若查找失败返回-1
     */
    @Override
    public int indexOf(char c) {
        return indexOf(0, c);
    }

    /**
     * 从下标start开始查找字符c的位置下标
     * @param start 从start位置开始查找
     * @param c 查找的字符
     * @return 字符c从start开始首次出现的字符下标，若查找失败返回-1
     */
    @Override
    public int indexOf(int start, char c) {
        byte byteC = (byte)c;
        for (int i = start; i < this.len; i++) {
            if (this.buf[i] == byteC) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 这里做了特殊处理：由于char实际占2个字节，但为了方便，我们以1个字节处理，放弃了一个字节
     * @param c
     */
    @Override
    public void append(char c) {
        byte b = (byte)c;
        append(b);
    }

    @Override
    public void append(ZedisString str) {
        String string = str.toString();
        byte [] bytes = SafeEncoder.encode(string);
        append(bytes);
    }

    @Override
    public void append(String str) {
        byte [] bytes = SafeEncoder.encode(str);
        append(bytes);
    }

    public void append(byte b) {
        int length = 1;
        this.expand(length);
        this.buf[this.len] = b;
        increaseLength(length);
    }

    public void append(byte [] bytes) {
        int length = bytes.length;

        // 扩展空间，保证有足够的存储空间
        this.expand(length);

        // 填充数据
        System.arraycopy(bytes, 0, this.buf, this.len, length);

        // 更新Sds长度属性
        increaseLength(length);
    }

    /**
     * 截取字符串，保留指定的截取范围[start, end)
     * @param start 截取开始范围，包括该下标
     * @param end 截取结束范围，不包括该下标
     */
    @Override
    public void cut(int start, int end) {
        if (start < 0 || start > end || end > this.len) {
            throw new IllegalArgumentException("The 'start' or 'end' argument is illegal");
        }

        // 特殊情况优化：如果start为0，那么就只需要修改len属性，而无需复制数据
        if (start == 0) {
            setLen(end);
            return;
        }

        // 特殊情况优化：如果start大于sds实际长度，那么直接将sds清空
        if (start >= this.len) {
            setLen(0);
            return;
        }

        int newLen = end - start;
        System.arraycopy(this.buf, start, this.buf, 0, newLen);
        setLen(newLen);
    }

    /**
     * 重新调整Sds的大小
     * @param len
     * @param free
     */
    public void resize(int len, int free) {
        byte [] newbuf = new byte[len + free];
        System.arraycopy(this.buf, 0, newbuf, 0, len);
        this.buf = newbuf;
        setLen(len);
    }

    /**
     * 复制 Sds 的副本
     * @return 创建成功返回输入 Sds 的副本
     */
    public Sds copy() {
        return createSds(this.len, this.buf);
    }

    /**
     * 释放给定的Sds
     * @param
     */
    public void free() {
        this.len = 0;
        this.free = 0;
        this.buf = new byte[0];
    }

    /**
     *  在不释放 SDS 的字符串空间的情况下，
     *  重置 SDS 所保存的字符串为空字符串。
     */
    public void clear() {
        setLen(0);
    }

    // *********************************

    /**
     * 对 sds 中 buf 的长度进行扩展，确保在函数执行之后，
     * buf 至少会有 addlen 长度的空余空间
     *
     * @param addlen 扩展后保证至少有addlen的空余空间
     * @return 扩展成功返回扩展后的 sds
     */
    public Sds expand(int addlen) {
        // 剩余空间足够，无需拓展
        if (this.free >= addlen) {
            return this;
        }

        // 空间不足，需要扩展
        int newlen = this.len + addlen; // 最少需要的长度
        if (newlen < SDS_MAX_PREALLOC) {
            // 如果新长度小于SDS_MAX_PREALLOC
            // 那么为它分配两倍于所需长度的空间
            newlen *= 2;
        } else {
            // 否则，分配长度为目前长度加上 SDS_MAX_PREALLOC
            newlen += SDS_MAX_PREALLOC;
        }

        byte [] newbuf = new byte[newlen];

        // 复制数据
        System.arraycopy(this.buf, 0, newbuf, 0, this.len);

        // 更换Sds内部数组，并更新free属性
        this.buf = newbuf;
        this.free = this.buf.length - this.len;

        return this;
    }

    /**
     * 移除空闲空间
     * @return 缩容成功返回缩容后的 sds
     */
    public Sds removeFreeSpace() {
        byte [] newbuf = new byte[this.len];
        System.arraycopy(this.buf, 0, newbuf, 0, this.len);

        // 更换Sds内部数组，并更新free属性
        this.buf = newbuf;
        this.free = this.buf.length - this.len;

        return this;
    }

    /**
     * 返回给定 sds 分配的内存字节数
     * @return Sds底层分配的字节数组长度
     */
    public int capacity() {
        return this.buf.length;
    }



    private void setLen(int len) {
        if (len > this.buf.length) {
            throw new ArrayIndexOutOfBoundsException("invalid len for Sds");
        }

        this.len = len;
        this.free = this.buf.length - this.len;
    }

    public  int remainingCapacity() {
        return this.free;
    }

    public byte[] toArray() {
        return Arrays.copyOf(this.buf, this.buf.length);
    }

    public byte[] toArrayWithOutCopy() {
        return this.buf;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < this.len; i++) {
            h = 31 * h + this.buf[i];
        }
        return h;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null) {
            return false;
        }

        if (other instanceof Sds) {
            Sds o = (Sds)other;
            if (o.len == this.len
                && this.compareTo(o) == 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return new String(this.buf, 0, this.len, Protocol.CHARSET);
    }

    /**
     * 实现Comparable接口，比较两个Sds
     * @param other 另一个Sds对象
     * @return 相等返回 0 ，当前对象较大返回正数， other较大返回负数
     */
    @Override
    public int compareTo(Sds other) {
        int len1 = this.len;
        int len2 = other.len;
        byte[] buf1 = this.buf;
        byte[] buf2 = other.buf;

        int minlen = Math.min(len1, len2);

        for(int i = 0; i < minlen; i++) {
            if (buf1[i] != buf2[i]) {
                return buf1[i] - buf2[i] > 0 ? 1 : -1;
            }
        }
        // 相等的情况
        if (len1 == len2) {
            return 0;
        }

        return len1 - len2 > 0 ? 1 : -1;
    }

    private void increaseLength(int increment) {
        if (increment > this.free) {
            throw new IllegalArgumentException("remaining capacity is insufficient");
        }
        this.len += increment;
        this.free -= increment;
    }
}
