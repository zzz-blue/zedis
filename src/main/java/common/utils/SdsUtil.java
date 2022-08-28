package common.utils;

import common.struct.impl.Sds;

import java.util.ArrayList;
import java.util.List;

public class SdsUtil {
    public SdsUtil() {
        throw new InstantiationError("Must not instantiate this class");
    }

    /**
     * 将一行文本分割成多个参数，每个参数可以有以下的编程语言类REPL格式
     * 可能会出现引号括起来的内容，这些内容应该作为一个整体解析
     * 同时引号内可能会用16进制数字表示、字符表示的转义字符等，都需要单独处理
     *
     * foo bar "newline are supported\n" and "\xff\x00otherstuff"
     *
     * T = O(N^2)
     * @param line Sds字符串
     * @return 函数返回一个 sds 数组
     */
    public static Sds[] splitArgs(Sds line){
        byte[] content = line.toArrayWithOutCopy();
        List<Sds> list = new ArrayList<>();

        Sds current = null;

        int index = 0, length = content.length;

        while(true){
            // 跳过空白
            while(index < length && Character.isWhitespace((char)content[index])){
                index++;
            }

            // 找一个连续的字符串
            if(index < length){
                boolean inQuotes = false;  /* set to 1 if we are in "quotes" */
                boolean inSingleQuotes = false; /* set to 1 if we are in 'single quotes' */
                boolean done = false;

                if (current == null) {
                    current = Sds.createEmptySds();
                }

                while(!done) {
                    if(inQuotes) {
                        if (index + 3 < length
                            && (char)content[index] == '\\'
                            && (char)content[index + 1] == 'x'
                            && StringUtil.isHexDigit((char)content[index + 2])
                            && StringUtil.isHexDigit((char)content[index + 3])) {
                            // 解析引号中的16进制表示的数据
                            int val = StringUtil.hexDigitToInt((char)content[index + 2]) * 16 + StringUtil.hexDigitToInt((char)content[index + 3]);
                            current.append(SafeEncoder.encode(String.valueOf(val)));
                            index += 3;
                        } else if (index + 1 < length && (char)content[index] == '\\') {
                            // 将引号中的文本表示的转义字符转换为实际的转义字符
                            char c;
                            index++;
                            switch((char)content[index]) {
                                case 'n': c = '\n'; break;
                                case 'r': c = '\r'; break;
                                case 't': c = '\t'; break;
                                case 'b': c = '\b'; break;
                                default: c = (char)content[index]; break;
                            }
                            current.append(c);
                        } else if (index >= length) {
                            // 这个判断必须在最前面，防止数组越界
                            // 如果没有解析到右侧的引号就到字符串末尾了，说明是格式错误
                            return null;
                        } else if ((char)content[index] == '"') {
                            // 解析到右边的引号，此时应该解析结束
                            // 此时要结束对引号内的内容解析，要求引号之后要么是字符串末尾要么是空白字符，否则是错误格式
                            if(index + 1 < length && !Character.isWhitespace((char)content[index + 1])){
                                return null;
                            }
                            done = true;
                        } else {
                            // 引号中的非特殊字符都会被直接拼接
                            current.append(content[index]);
                        }
                    } else if (inSingleQuotes) {
                        // 当解析的字符串遇到单引号，引号内的内容应该作为一个整体
                        if(index + 1 < length
                            && (char)content[index] == '\\'
                            && (char)content[index+1] == '\''){
                            // 遇到转义的单引号，将其加入字符串中
                            index++;
                            current.append('\'');
                        } else if (index >= length){
                            // 这个判断必须在最前面，防止数组越界
                            // 如果没有解析到右侧的引号就到字符串末尾了，说明是格式错误
                            return null;
                        } else if ((char)content[index] == '\''){
                            // 解析到右边的单引号，此时应该解析结束
                            // 此时要结束对引号内的内容解析，要求引号之后要么是字符串末尾要么是空白字符，否则是错误格式
                            if(index + 1 < length && !Character.isWhitespace((char)content[index])) {
                                return null;
                            }
                            done = true;
                        } else {
                            // 单引号中的非特殊字符都会被直接拼接
                            current.append(content[index]);
                        }
                    } else {
                        if (index >= length) {
                            done = true;
                            break;
                        }

                        switch((char)content[index]) {
                            case ' ':
                            case '\n':
                            case '\r':
                            case '\t':
                            case '\0':
                                done = true;
                                break;
                            case '"':
                                inQuotes = true;
                                break;
                            case '\'':
                                inSingleQuotes = true;
                                break;
                            default:
                                current.append(content[index]);
                                break;
                        }
                    }
                    if(index < length) {
                        index++;
                    }
                }
                // 一个完整的字符串已找到，加入列表
                list.add(current);
                current = null;
            } else {
                if(list == null || list.size() == 0){
                    return null;
                } else {
                    return list.toArray(new Sds[list.size()]);
                }
            }
        }
    }
}
