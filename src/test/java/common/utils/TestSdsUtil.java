package common.utils;

import common.struct.impl.Sds;
import org.junit.Test;

/**
 * @description:
 */
public class TestSdsUtil {
    @Test
    public void testSplitArgs() {
        Sds test1 = Sds.createSds(SafeEncoder.encode("\t\rset a 1 ")) ;

        Sds test2 = Sds.createSds(SafeEncoder.encode("set a \"hello world \\x1a \\n\""));
        Sds test3 = Sds.createSds(SafeEncoder.encode("set a 'good \tmorning '"));
        Sds test4 = Sds.createSds(SafeEncoder.encode("set a \"hh hh"));
        Sds test5 = Sds.createSds(SafeEncoder.encode("set a 'ss ss"));


//        Sds [] res1 = SdsUtil.splitArgs(test1);
//        System.out.println(Arrays.toString(res1));
//
//        Sds [] res2 = SdsUtil.splitArgs(test2);
//        System.out.println(Arrays.toString(res2));
//
//        Sds [] res3 = SdsUtil.splitArgs(test3);
//        System.out.println(Arrays.toString(res3));
//
//        Sds [] res4 = SdsUtil.splitArgs(test4);
//        System.out.println(Arrays.toString(res4));
//
//        Sds [] res5 = SdsUtil.splitArgs(test5);
//        System.out.println(Arrays.toString(res5));
    }
}
