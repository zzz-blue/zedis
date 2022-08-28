package remote.protocol;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Description
 **/
public class TestResponseParser {
    @Test
    public void testSeekNewLine() {
        byte [] bytes = "hello world\r\n good morning\r\n hi\n".getBytes(StandardCharsets.UTF_8);
        Assert.assertEquals(11, ResponseParser.seekNewLine(bytes,0));
        Assert.assertEquals(26, ResponseParser.seekNewLine(bytes,12));
        Assert.assertEquals(-1, ResponseParser.seekNewLine(bytes,28));
    }

    @Test
    public void testParseInteger() {
        byte [] bytes = "13412a".getBytes(StandardCharsets.UTF_8);
        Assert.assertEquals(13412, ResponseParser.parseInteger(bytes,0, 5));
        Assert.assertEquals(13412, ResponseParser.parseInteger(bytes,0, 6));
    }

    @Test
    public void testParseIntegerReply() {
        byte [] bytes = ":32\r\n:44\r\n:a".getBytes(StandardCharsets.UTF_8);
//        Reply r1 = ResponseParser.parseIntegerReply(bytes, 0);
//        Assert.assertEquals(5, r1.getParseByteLength());
//        Assert.assertEquals(32, r1.getIntegerReplyContent());
//        Reply r2 = ResponseParser.parseIntegerReply(bytes, 5);
//        Assert.assertEquals(5, r2.getParseByteLength());
//        Assert.assertEquals(44, r2.getIntegerReplyContent());
    }


    @Test
    public void testParseErrorReply() {
//        byte [] bytes = "-ERR unknown command 'ERRORCOMMAND'\r\n".getBytes(StandardCharsets.UTF_8);
//        Reply r1 = ResponseParser.parseErrorReply(bytes, 0);
//        Assert.assertEquals(37, r1.getParseByteLength());
//        Assert.assertEquals("ERR unknown command 'ERRORCOMMAND'", r1.getStringReplyContent());
    }

    @Test
    public void testParseStatusReply() {
//        byte [] bytes = "+OK\r\n".getBytes(StandardCharsets.UTF_8);
//        Reply r1 = ResponseParser.parseStatusReply(bytes, 0);
//        Assert.assertEquals(5, r1.getParseByteLength());
//        Assert.assertEquals("OK", r1.getStringReplyContent());
    }

    @Test
    public void testParseBulkReply() {
//        byte [] bytes = "$3\r\nbar\r\n".getBytes(StandardCharsets.UTF_8);
//        Reply r1 = ResponseParser.parseBulkReply(bytes, 0);
//        Assert.assertEquals(9, r1.getParseByteLength());
//        Assert.assertEquals("bar", r1.getStringReplyContent());
    }


    @Test
    public void testParseMultiBulkReply() {
//        byte [] bytes = "*3\r\n$2\r\n33\r\n$1\r\n2\r\n$3\r\n111\r\n".getBytes(StandardCharsets.UTF_8);
//        Reply r1 = ResponseParser.parseMultiBulkReply(bytes, 0);
//        Assert.assertEquals(28, r1.getParseByteLength());
//        List<String> ret = r1.getMultiStringReplyContent();
//        for (String s : ret) {
//            System.out.println(s);
//        }
    }
}
