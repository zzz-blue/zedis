package remote.protocol;

import server.client.InnerClient;
import common.struct.impl.Sds;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import common.utils.SdsUtil;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * @description: 该类负责解析客户端发送来的查询请求
 * @author: zzz
 * @create: 2021-08-19
 */
public class RequestParser {
    private static Log logger = LogFactory.getLog(RequestParser.class);

    public static final int  INLINE_MAX_SIZE  = 1024 * 64;  /* Max size of inline reads */

    private final Sds queryBuffer;                          // 该RequestParser关联的queryBuffer
    private final InnerClient client;                       // 该RequestParser关联的client对象

    private volatile RequestType currentParseRequestType;   // 表示当前正在解析的请求类型
    private volatile int currentMultiBulkRequestItemNum;    // 当前解析的multBulkRequest中的元素数量
    private volatile int currentBulkItemLength;             // 当前解析的multBulkRequest中当前bulk的内容长度

    public RequestParser(InnerClient client, Sds queryBuffer) {
        this.queryBuffer = queryBuffer;
        this.client = client;
        this.currentParseRequestType = RequestType.NONE;
        this.currentMultiBulkRequestItemNum = 0;
        this.currentBulkItemLength = -1;
    }

    /**
     * 解析缓冲区的请求数据
     * 解析出来的命令参数存入client的命令参数列表中
     */
    public void parseRequest() {
        // 一直循环，直到完整地解析一个request为止
        while (!this.queryBuffer.isEmpty()) {
            // 先从查询缓冲中截取出一段完整命令（以\n结尾）
            int indexLast = this.queryBuffer.indexOf('\n');
            // 当前查询缓冲区内的数据还不完整，暂时无法进行解析
            if(indexLast == -1) {
                return;
            }

            /******************************************************
             * 开始解析
             ******************************************************/

            // currentParseRequestType记录了当前正在进行解析过程中的请求类型，如果无类型，表示正准备解析一个新的请求
            // 在解析一个request前，会将当前request的类型记录到currentParseRequestType
            // 每当完整解析完当前request后，又会被设置为无类型
            if (this.currentParseRequestType == RequestType.NONE) {
                // 当解析一个新的request，则将之前客户端内的命令列表中的参数清空
                this.client.getCommandArgs().clear();
                if (this.queryBuffer.charAt(0) == RequestType.MULTI_BULK_PREFIX) {
                    // 多条查询，一般客户端发送来的
                    this.currentParseRequestType = RequestType.MULTI_BULK;
                } else {
                    // 内联查询 TELNET 等工具发送来的
                    this.currentParseRequestType = RequestType.INLINE;
                }
            }

            // 根据当前的request类型，按照相应协议进行解析
            // 将缓冲区的数据转换命令及命令参数
            if (this.currentParseRequestType == RequestType.INLINE) {
                // 执行了一次解析
                // 但有可能没有成功，因为数据可能不全
                parseInlineRequest();
                break;
            } else if (this.currentParseRequestType == RequestType.MULTI_BULK) {
                // 执行了一次解析
                // 但有可能没有成功，因为数据可能不全
                // 也有可能只解析了一部分，因为数据可能不全
                parseMultiBulkRequest();
                break;
            } else {
                logger.error("Unknow request type");
            }
        }
    }

    /**
     * 处理内联查询格式
     * 内联命令的各个参数以空格分开，并以 \r\n 结尾
     * 如果缓冲区内的数据不完整，无法构成一个完整的request，则暂停解析，直接返回false，等待新的数据进入缓冲区再解析
     * @return 解析是否成功
     */
    private boolean parseInlineRequest() {
        // 查找一行的行尾
        int indexLast = this.queryBuffer.indexOf('\n');

        // 收到的查询内容不符合协议内容，出错
        if(indexLast == -1) {
            if(this.queryBuffer.length() > INLINE_MAX_SIZE) {
                // todo 错误处理 ，
                // server.client.addReply(ReplyType.ERROR, "Protocol error: too big inline request");
            }
            return false;
        }

        // 处理\r\n
        if (indexLast != 0 && this.queryBuffer.charAt(indexLast - 1) == '\r'){
            indexLast--;
        }

        // 提取该条request
        Sds request = Sds.createSds(indexLast, this.queryBuffer.toArrayWithOutCopy());

        // 根据空格，分割命令的参数
        Sds[] commandArgs = SdsUtil.splitArgs(request);

        if (commandArgs == null) {
            //todo 错误处理， Protocol error: unbalanced quotes in request
            // server.client.addReply(ReplyType.ERROR, "Protocol error: unbalanced quotes in request");
            return false;
        }

        /******************************************************
         * 执行到此处，表示解析成功
         ******************************************************/

        // 将解析的命令参数存入client的命令参数列表
        ArrayList<Sds> clientCommandList = this.client.getCommandArgs();
        for (Sds s : commandArgs) {
            clientCommandList.add(s);
        }

        // 对当前request解析完成，则将request的内容从回复缓冲区中删除，剩余内容是未解析的
        this.queryBuffer.cut(indexLast + 2, this.queryBuffer.length());

        // 解析成功，将当前正在解析的请求类型设置为无类型
        this.currentParseRequestType = RequestType.NONE;

        return true;
    }

    /**
     * 处理多条查询格式
     * 有可能一次只能解析一部分命令参数（由于数据不完整），每解析出一些命令参数，就存入客户端的命令参数列表中
     * 比如 *3\r\n$3\r\nSET\r\n$3\r\nMSG\r\n$5\r\nHELLO\r\n
     * 将被转换为：
     * argv[0] = SET
     * argv[1] = MSG
     * argv[2] = HELLO
     * @return 解析是否成功
     */
    private boolean parseMultiBulkRequest() {
        int parsePosition = 0;

        // 解析读入命令的参数个数
        // 比如 *3\r\n$3\r\nSET\r\n... 将令 c->multibulklen = 3

        // 每当解析一个新的multi bulk request时，会将当前的request中item的数量记录下来
        // 如果为0，表示正在解析一个新的multi bulk request
        if (this.currentMultiBulkRequestItemNum == 0) {
            int firstDelimiterIndex = queryBuffer.indexOf('\r');
            if (firstDelimiterIndex == -1) {
                if (queryBuffer.length() > INLINE_MAX_SIZE) {
                    // server.client.addReply(ReplyType.ERROR, "Protocol error: too big mbulk count string");
                    return false;
                }
            }

            // 缓冲区内的数据应该也要包含\r\n，否则说明是不完整的requst，暂时不解析
            if (firstDelimiterIndex > queryBuffer.length() - 2) {
                return false;
            }

            // 协议的第一个字符必须是 '*'，否则格式出错，不解析
            if (queryBuffer.charAt(0) != '*') {
                return false;
            }

            // 解析出multi bulk request中指定的请求中bulk的个数
            int requestItemNum = Integer.parseInt(new String(queryBuffer.toArrayWithOutCopy(), 1, firstDelimiterIndex - 1, Protocol.CHARSET));

            // 参数数量之后的位置
            // 比如对于 *3\r\n$3\r\n$SET\r\n... 来说，
            // pos 指向 *3\r\n$3\r\n$SET\r\n...
            //               ^
            //               |
            //              pos
            // 参数数量之后的位置
            parsePosition = firstDelimiterIndex + 2;
            this.currentMultiBulkRequestItemNum = requestItemNum;
        }

        // currentMultiBulkRequstItemNum 表示了该multi request剩余待解析的item数量
        // 循环处理，但由于有可能request的剩余数据没有收到，因此，可能会分多次返回解析的参数
        while (this.currentMultiBulkRequestItemNum > 0) {
            // 表示当前正好在解析一个新的bulk item
            if (this.currentBulkItemLength == -1) {
                // 从parsePosition位置处开始向后解析
                // 首先确保能找到 "\r\n" 存在
                int delimiterIndex = queryBuffer.indexOf(parsePosition, '\r');
                if (delimiterIndex == -1) {
                    // 如果超过固定长度还没有\r\n，说明格式错误
                    if (queryBuffer.length() > INLINE_MAX_SIZE) {
                        // server.client.addReply(ReplyType.ERROR, "Protocol error: too big bulk count string");
                        return false;
                    }
                    // 如果只是单纯没找到\r\n，说明可能request数据还不全，暂时不解析
                    break;
                }

                // 缓冲区内的数据应该也要包含\r\n，否则说明是不完整的requst，暂时不解析
                if (delimiterIndex > queryBuffer.length() - 2) {
                    break;
                }

                // 确保协议符合参数格式，单个bulk以$开头，检查其中的 $...
                // 比如 $3\r\nSET\r\n
                if ((char) queryBuffer.charAt(parsePosition) != '$') {
                    // todo "Protocol error: expected '$', got '%c'"
                    // server.client.addReply(ReplyType.ERROR, "Protocol error: expected '$'");
                    return false;
                }

                // 解析单个bulk中$后指定的长度
                int bulkItemLength = Integer.parseInt(new String(queryBuffer.toArrayWithOutCopy(), parsePosition + 1, delimiterIndex - parsePosition - 1, Protocol.CHARSET));
                if (bulkItemLength < 0 || bulkItemLength > 512 * 1024 * 1024) {
                    // todo Protocol error: invalid bulk length
                    // server.client.addReply(ReplyType.ERROR, "Protocol error: invalid bulk length");
                    return false;
                }

                // 将parsePosition指定到当前bulk的内容起始处
                parsePosition = delimiterIndex + 2;

                // 记录当前multi request中当前解析的bulk内容长度
                this.currentBulkItemLength = bulkItemLength;
            }

            // 当前parsePosition指向一个bulk item的内容部分的起始处
            // 确保内容符合协议格式，即单个bulk item的内容部分，后面也要确保包含\r\n
            // 比如 $3\r\nSET\r\n 就检查 SET 之后的 \r\n
            // 如果不满足，说明request数据不全，暂不解析
            if (queryBuffer.length() - parsePosition < this.currentBulkItemLength + 2) {
                break;
            } else {
                // 将单个bulk item的内容提取出来
                // 由于multi bulk中的参数可能是分多次解析出来的，因此，是逐步添加到客户端的命令参数列表中
                Sds arg = Sds.createSds(Arrays.copyOfRange(queryBuffer.toArrayWithOutCopy(), parsePosition, parsePosition + this.currentBulkItemLength));
                ArrayList<Sds> clientCommandList = client.getCommandArgs();
                clientCommandList.add(arg);

                // 将parsePosition的位置移动到下一个bulk item的起始处
                parsePosition += this.currentBulkItemLength + 2;

                // 当前bulk内容解析完成，将currentBulkItemLength复位
                this.currentBulkItemLength = -1;
                // 每解析完一个bulk，就将currentMultiBulkRequestItemNum的数量减一
                this.currentMultiBulkRequestItemNum--;
            }
        }

        // 从 querybuf 中删除已被解析的内容
        if (parsePosition > 0) {
            queryBuffer.cut(parsePosition, queryBuffer.length());
        }

        // 如果本条命令的所有参数都已读取完，那么返回
        if (this.currentMultiBulkRequestItemNum == 0) {
            this.currentParseRequestType = RequestType.NONE;
            return true;
        }

        // 如果还有参数未读取完，那么就协议内容有错
        return false;
    }
}
