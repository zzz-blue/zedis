package common.utils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类，实现了一些字符串辅助方法
 **/
public class StringUtil {
    public StringUtil() {
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
     * @param str 被解析的字符串
     * @return 分割的参数数组，若传入的字符串存在格式错误，则返回null
     */
    public static String [] splitArgs(String str) {
        ArrayList<String> list = new ArrayList<>();

        StringBuilder current = null;

        int index = 0, length = str.length();

        while(true){
            // 跳过空白
            while(index < length && Character.isWhitespace(str.charAt(index))){
                index++;
            }

            // 找一个连续的字符串
            if(index < length) {
                boolean inQuotes = false;  /* set to true if we are in "quotes" */
                boolean inSingleQuotes = false; /* set to true1 if we are in 'single quotes' */
                boolean done = false;

                if (current == null) {
                    current = new StringBuilder();
                }

                while (!done) {
                    // 当解析的字符串遇到引号，引号内的内容应该作为一个整体
                    if (inQuotes) {
                        if (index + 3 < length
                            && str.charAt(index) == '\\'
                            && str.charAt(index + 1) == 'x'
                            && isHexDigit(str.charAt(index+2))
                            && isHexDigit(str.charAt(index+3))) {
                            // 解析引号中的16进制表示的数据
                            int val = hexDigitToInt(str.charAt(index + 2)) * 16 + hexDigitToInt(str.charAt(index + 3));
                            current.append(String.valueOf(val));
                            index += 3;
                        } else if (index + 1 < length && str.charAt(index) == '\\') {
                            // 将引号中的文本表示的转义字符转换为实际的转义字符
                            char c;
                            index++;

                            switch (str.charAt(index)) {
                                case 'n': c = '\n'; break;
                                case 'r': c = '\r'; break;
                                case 't': c = '\t'; break;
                                case 'b': c = '\b'; break;
                                default: c = str.charAt(index); break;
                            }
                            current.append(c);
                        } else if (index >= length) {
                            // 这个判断必须在最前面，防止数组越界
                            // 如果没有解析到右侧的引号就到字符串末尾了，说明是格式错误
                            return null;
                        } else if (str.charAt(index) == '"'){
                            // 解析到右边的引号，此时应该解析结束
                            // 此时要结束对引号内的内容解析，要求引号之后要么是字符串末尾要么是空白字符，否则是错误格式
                            if (index + 1 < length && !Character.isWhitespace(str.charAt(index + 1))) {
                                return null;
                            }
                            done = true;
                        } else {
                            // 引号中的非特殊字符都会被直接拼接
                            current.append(str.charAt(index));
                        }
                    } else if (inSingleQuotes) {
                        // 当解析的字符串遇到单引号，引号内的内容应该作为一个整体
                        if (index + 1 < length && str.charAt(index) == '\\' && str.charAt(index + 1) == '\'') {
                            // 遇到转义的单引号，将其加入字符串中
                            index++;
                            current.append("'");
                        } else if (index >= length) {
                            // 这个判断必须在最前面，防止数组越界
                            // 如果没有解析到右侧的引号就到字符串末尾了，说明是格式错误
                            return null;
                        } else if (str.charAt(index) == '\'') {
                            // 解析到右边的单引号，此时应该解析结束
                            // 此时要结束对引号内的内容解析，要求引号之后要么是字符串末尾要么是空白字符，否则是错误格式
                            if(index + 1< length && !Character.isWhitespace(str.charAt(index))) {
                                return null;
                            }

                            done = true;
                        } else {
                            // 单引号中的非特殊字符都会被直接拼接
                            current.append(str.charAt(index));
                        }
                    } else {
                        if (index >= length) {
                            done = true;
                            break;
                        }

                        switch(str.charAt(index)) {
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
                                current.append(str.charAt(index));
                                break;
                        }
                    }

                    if(index < length) {
                        index++;
                    }
                }

                // 一个完整的字符串已找到，加入列表
                list.add(current.toString());
                current = null;
            } else {
                if (list == null || list.size() == 0) {
                    return null;
                } else {
                    return list.toArray(new String[list.size()]);
                }
            }
        }
    }

    /**
     * 将十六进制符号转换为10进制
     * @param c 字符
     * @return 10进制数
     */
    public static int hexDigitToInt(char c) {
        switch(c) {
            case '0': return 0;
            case '1': return 1;
            case '2': return 2;
            case '3': return 3;
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            case 'a': case 'A': return 10;
            case 'b': case 'B': return 11;
            case 'c': case 'C': return 12;
            case 'd': case 'D': return 13;
            case 'e': case 'E': return 14;
            case 'f': case 'F': return 15;
            default: return 0;
        }
    }

    /**
     * 判断字符是否为16进制数
     * @param c
     * @return 如果 c 为十六进制符号的其中一个，返回正数
     */
    public static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * 判断字符串是否是一个整数
     * @param str
     * @return
     */
    public static boolean isInteger(String str) {
        Matcher matcher = Pattern.compile("^[-+]?[0-9]+$").matcher(str);
        return matcher.find();
    }

    public static String toQuoted(String str) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        int index = 0;
        sb.append("\"");

        while (index < len) {
            char c = str.charAt(index);
            switch (c) {
                case '\\':
                case '"':
                    sb.append("\\");
                    sb.append(c);
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\r");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                default:
                    sb.append(c);
            }
            index++;
        }

        sb.append("\"");

        return sb.toString();
    }
}
