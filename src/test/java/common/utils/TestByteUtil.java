package common.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;


public class TestByteUtil {
    @Test
    public void testIntToBytes() {
        int a = -2568;

        byte [] bytes = ByteUtil.intToBytes(a);

        System.out.println(Arrays.toString(bytes));

        int b = ByteUtil.bytesToInt(bytes);
        System.out.println(b);
    }

    @Test
    public void testShortToBytes() {
        short a = -568;

        byte [] bytes = ByteUtil.shortToBytes(a);

        System.out.println(Arrays.toString(bytes));

        short b = ByteUtil.bytesToShort(bytes);
        System.out.println(b);
    }

    @Test
    public void testTryTransformBytesToLong() {
        byte [] bytes1 = SafeEncoder.encode("-0029874");
        byte [] bytes2 = SafeEncoder.encode("-00a87+4");
        byte [] bytes3 = SafeEncoder.encode("-");

        Assert.assertEquals(true, ByteUtil.tryTransformBytesToLong(bytes1));
        Assert.assertEquals(false, ByteUtil.tryTransformBytesToLong(bytes2));
        Assert.assertEquals(false, ByteUtil.tryTransformBytesToLong(bytes3));
    }

    @Test
    public void testTransformBytesToLong() {
        byte [] bytes1 = SafeEncoder.encode("-0029874");
        byte [] bytes3 = SafeEncoder.encode("-");
        byte [] bytes4 = SafeEncoder.encode("20714010");
        byte [] bytes2 = SafeEncoder.encode("-00a87+4");

        Assert.assertEquals(-29874, ByteUtil.transformBytesToLong(bytes1));
        Assert.assertEquals(20714010, ByteUtil.transformBytesToLong(bytes4));
        Assert.assertEquals(0, ByteUtil.transformBytesToLong(bytes3));
//        ByteUtil.transformBytesToLong(bytes2);
    }
}
