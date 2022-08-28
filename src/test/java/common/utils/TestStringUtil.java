package common.utils;

import org.junit.Test;

import java.util.Arrays;

/**
 * @Description
 * @Author zzz
 * @Date 2021/9/26
 **/
public class TestStringUtil {
    @Test
    public void testSplitArgs() {
        String test1 = "\t\rset a 1 ";

        String test2 = "set a \"hello world \\x1a \\n\"";
        String test3 = "set a 'good \tmorning '";
        String test4 = "set a \"hh hh";
        String test5 = "set a 'ss ss";
        String test6 = " hset name \"name:filed\" \"value:field\" ";


        String [] res1 = StringUtil.splitArgs(test1);
        System.out.println(Arrays.toString(res1));

        String [] res2 = StringUtil.splitArgs(test2);
        System.out.println(Arrays.toString(res2));

        String [] res3 = StringUtil.splitArgs(test3);
        System.out.println(Arrays.toString(res3));

        String [] res4 = StringUtil.splitArgs(test4);
        System.out.println(Arrays.toString(res4));

        String [] res5 = StringUtil.splitArgs(test5);
        System.out.println(Arrays.toString(res5));

        String [] res6 = StringUtil.splitArgs(test6);
        System.out.println(Arrays.toString(res6));
    }

    @Test
    public void testToQuoted() {
        String test1 = "\t\rset a 1 ";
        String test2 = "set a \"hello world \\x1a \\n\"";
        String test3 = "set a 'good \tmorning '";
        String test4 = "set a \"hh hh";
        String test5 = "set a 'ss ss";


        String res1 = StringUtil.toQuoted(test1);
        System.out.println(res1);

    }
}
