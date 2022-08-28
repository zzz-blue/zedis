package cli;

/**
 * @description: 描述了一些cli帮助信息的类
 * @author: zzz
 * @create: 2021-08-29
 */
public class ClientCliHelper {

    public static void initHelpInfo() {

    }

    /**
     * 打印使用说明
     */
    public static void printUsage() {
        StringBuilder usage = new StringBuilder();

        usage.append("zedis-cli\n");
        usage.append("\n");
        usage.append("Usage: zedis-cli [OPTIONS] [cmd [arg [arg ...]]]\n");
        usage.append("  -h <hostname>      Server hostname (default: 127.0.0.1).\n");
        usage.append("  -p <port>          Server port (default: 6379).\n");
        usage.append("  -s <socket>        Server socket (overrides hostname and port).\n");
        usage.append("  -a <password>      Password to use when connecting to the server.\n");
        usage.append("  -r <repeat>        Execute specified command N times.\n");
        usage.append("  -i <interval>      When -r is used, waits <interval> seconds per command.\n");
        usage.append("                     It is possible to specify sub-second times like -i 0.1.\n");
        usage.append("  -n <db>            Database number.\n");
        usage.append("  -x                 Read last argument from STDIN.\n");
        usage.append("  -d <delimiter>     Multi-bulk delimiter in for raw formatting (default: \\n).\n");
        usage.append("  -c                 Enable cluster mode (follow -ASK and -MOVED redirections).\n");
        usage.append("  --raw              Use raw formatting for replies (default when STDOUT is\n");
        usage.append("                     not a tty).\n");
        usage.append("  --csv              Output in CSV format.\n");
        usage.append("  --latency          Enter a special mode continuously sampling latency.\n");
        usage.append("  --latency-history  Like --latency but tracking latency changes over time.\n");
        usage.append("                     Default time interval is 15 sec. Change it using -i.\n");
        usage.append("  --slave            Simulate a slave showing commands received from the master.\n");
        usage.append("  --rdb <filename>   Transfer an RDB dump from remote server to local file.\n");
        usage.append("  --pipe             Transfer raw Redis remote.protocol from stdin to server.\n");
        usage.append("  --pipe-timeout <n> In --pipe mode, abort with error if after sending all data.\n");
        usage.append("                     no reply is received within <n> seconds.\n");
        usage.append("                     Default timeout: %d. Use 0 to wait forever.\n");
        usage.append("  --bigkeys          Sample Redis keys looking for big keys.\n");
        usage.append("  --scan             List all keys using the SCAN command.\n");
        usage.append("  --pattern <pat>    Useful with --scan to specify a SCAN pattern.\n");
        usage.append("  --intrinsic-latency <sec> Run a test to measure intrinsic system latency.\n");
        usage.append("                     The test will run for the specified amount of seconds.\n");
        usage.append("  --eval <file>      Send an EVAL command using the Lua script at <file>.\n");
        usage.append("  --help             Output this help and exit.\n");
        usage.append("  --version          Output version and exit.\n");
        usage.append("\n");
        usage.append("Examples:\n");
        usage.append("  cat /etc/passwd | redis-cli -x set mypasswd\n");
        usage.append("  redis-cli get mypasswd\n");
        usage.append("  redis-cli -r 100 lpush mylist x\n");
        usage.append("  redis-cli -r 100 -i 1 info | grep used_memory_human:\n");
        usage.append("  redis-cli --eval myscript.lua key1 key2 , arg1 arg2 arg3\n");
        usage.append("  redis-cli --scan --pattern '*:12345*'\n");
        usage.append("\n");
        usage.append("  (Note: when using --eval the comma separates KEYS[] from ARGV[] items)\n");
        usage.append("\n");
        usage.append("When no command is given, redis-cli starts in interactive mode.\n");
        usage.append("Type \"help\" in interactive mode for information on available commands.\n");
        usage.append("\n");

        System.err.print(usage.toString());
        System.exit(1);
    }

    /**
     * 输出所有命令的帮助信息，可以通过命令名称或组来过滤
     */
    public static void printHelpInfo(String [] commands) {
        // todo
    }
}
