package cli;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import remote.Response;
import common.utils.StringUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @Description 客户端cli
 **/
public class ClientCli {
    private static Log logger = LogFactory.getLog(ClientCli.class);

    private ClientConfig config;    // 客户端配置
    private ClientContext context;  // 客户端运行上下文

    public ClientCli() {
        // 初始化默认配置
        // 运行上下文会在客户端与服务器建立连接时创建
        config = new ClientConfig();
    }

    private int parseOption(String [] args) {
        int index = 0;
        for (; index < args.length; index++) {
            boolean lastarg = (index == args.length - 1);

            if ("-h".equals(args[index]) && !lastarg) {
                config.setHostip(args[++index]);
            } else if ("-h".equals(args[index]) && lastarg) {
                ClientCliHelper.printUsage();
            } else if ("--help".equals(args[index])) {
                ClientCliHelper.printUsage();
            } else if ("-x".equals(args[index])) {
                config.setStdinarg(true);
            } else if ("-p".equals(args[index]) && !lastarg) {
                config.setHostport(Integer.valueOf(args[++index]));
            } else if ("-s".equals(args[index]) && !lastarg) {
                config.setHostsocket(args[++index]);
            } else if ("-r".equals(args[index]) && !lastarg) {
                config.setRepeat(Integer.valueOf(args[++index]));
            } else if ("-i".equals(args[index]) && !lastarg) {
                double seconds = Double.valueOf(args[++index]);
                config.setInterval((long) (seconds * 1000000));
            } else if ("-a".equals(args[index]) && !lastarg) {
                config.setAuth(args[++index]);
            } else if ("--raw".equals(args[index])) {
                config.setOutputType(ClientConfig.OutputType.OUTPUT_RAW);
            } else if ("--csv".equals(args[index])) {
                config.setOutputType(ClientConfig.OutputType.OUTPUT_CSV);
            } else if ("--latency".equals(args[index])) {
                config.setLatencyMode(true);
            } else if ("--latency-history".equals(args[index])) {
                config.setLatencyMode(true);
                config.setLatencyHistory(true);
            } else if ("--slave".equals(args[index])) {
                config.setSlaveMode(true);
            } else if ("--stat".equals(args[index])) {
                config.setStatMode(true);
            } else if ("--scan".equals(args[index])) {
                config.setScanMode(true);
            } else if ("--pattern".equals(args[index]) && !lastarg) {
                config.setPattern(args[++index]);
            } else if ("--intrinsic-latency".equals(args[index]) && !lastarg) {
                config.setIntrinsicLatencyMode(true);
                config.setIntrinsicLatencyDuration(Integer.valueOf(args[++index]));
            } else if ("--rdb".equals(args[index]) && !lastarg) {
                config.setGetrdbMode(true);
                config.setRdbFilename(args[++index]);
            } else if ("--pipe".equals(args[index])) {
                config.setPipeMode(true);
            } else if ("--pip-timeout".equals(args[index]) && !lastarg) {
                config.setPipeTimeout(Integer.valueOf(args[++index]));
            } else if ("--bigkeys".equals(args[index])) {
                config.setBigkeys(true);
            } else if ("--eval".equals(args[index]) && !lastarg) {
                config.setEval(args[++index]);
            } else if ("-c".equals(args[index])) {
                config.setClusterMode(true);
            } else if ("-d".equals(args[index]) && !lastarg) {
                config.setMbDelim(args[++index]);
            } else if ("-v".equals(args[index]) || "--version".equals(args[index])) {
                System.out.print("zedis-cli " + getCliVersion() + "\n");
                System.exit(0);
            } else {
                if (args[index].charAt(0) == '-') {
                    System.err.println("Unrecognized option or bad number of args for: " + args[index]);
                    System.exit(1);
                } else {
                    /* Likely the command name, stop here. */
                    break;
                }
            }
        }
        return index;
    }

    /**
     * @param force 为true时表示已经连接仍会强制执行
     **/
    private boolean cliConnect(boolean force) {
        if (this.context == null || force) {
            this.context = new ClientContext();
            this.context.connectTcp(this.config.getHostip(), this.config.getHostport());

            // 判断是否认证了以及是否选择的正确的db
            // if (!cliAuth()) {
            //     return false;
            // }

            // if (!cliSelect()) {
            //     return false;
            // }
        }
        return true;
    }

    /**
     * 打印congtext的错误信息
     */
    private void printContextError() {
        if (this.context == null) {
            return;
        }
        System.err.println("Error: " + this.context.getErrInfo());
    }

    /**
     * 发送命令
     * @param startIndex 从传入的参数数组的第几位开始解析
     * @param argv 参数数组
     * @param repeat 命令重复次数
     * @return 命令发送是否成功
     */
    private boolean sendCommand(int startIndex, String [] argv, int repeat) {
        // 参数检测
        if (startIndex >= argv.length) {
            return false;
        }

        // 实际可以解析的数组长度
        int argc = argv.length - startIndex;
        // 第一个元素，即命令
        String command = argv[startIndex].toLowerCase();

        // 帮助命令
        if ("help".equals(command) || "?".equals(command)) {
            ClientCliHelper.printHelpInfo(Arrays.copyOfRange(argv, 1, argv.length));
            return true;
        }

        if (this.context == null) {
            return false;
        }

        boolean outputRaw = false;
        if ("info".equals(command)) {
            // todo: 这里还有一些其他判断条件，但暂时未知功能，这里暂时不实现
            outputRaw = true;
        }

        if ("shutdown".equals(command)) {
            config.setShutdown(true);
        }

        if ("monitor".equals(command)) {
            config.setMonitorMode(true);
        }

        if ("subscribe".equals(command) || "psubscribe".equals(command)) {
            config.setPubsubMode(true);
        }

        if ("sync".equals(command) || "psync".equals(command)) {
            config.setSlaveMode(true);
        }

        /* Setup argument length */
        while (repeat > 0) {
            // 解析输入的命令参数，按照协议将其生成命令
            // 将命令缓存到客户端context的输出缓冲区outBuffer
            context.appendCommandArgv(startIndex, argv);

            while (config.isMonitorMode()) {
                // todo
            }

            // 表示当前cli客户端处于订阅模式，要一直监听网络，监听服务器发来的发布信息
            if (config.isPubsubMode()) {
                if (config.getOutputType() == ClientConfig.OutputType.OUTPUT_RAW) {
                    System.out.println("Reading messages... (press Ctrl-C to quit)\n");
                }
                while (true) {
                    if (!readReply(outputRaw)) {
                        System.exit(1);
                    }
                }
            }

            if (config.isSlaveMode()) {
                // todo
            }

            if (!readReply(outputRaw)) {
                return false;
            }

            if (config.getInterval() > 0) {
                try {
                    Thread.sleep(config.getInterval());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.flush();
            repeat--;
        }
        return true;
    }

    /**
     * 读取服务器回复信息
     * @param outputRawString
     * @return
     */
    private boolean readReply(boolean outputRawString) {
        boolean output = true;
        StringBuilder out = new StringBuilder();

        Response response = this.context.getResponse();

        // 如果获取回复失败
        if (response == null) {
            if (this.config.isShutdown()) {
                return true;
            }

            printContextError();
            System.exit(1);
            return false;
        }

        // todo: Check if we need to connect to a different node and reissue the request.

        // 是否输出
        if (output) {
            // 以raw字符串格式输出
            if (outputRawString) {
                out.append(formatResponseRaw(response));
            } else {
                // 判断以什么格式输出回复结果
                if (this.config.getOutputType() == ClientConfig.OutputType.OUTPUT_RAW) {
                    out.append(formatResponseRaw(response));
                    out.append("\n");
                } else if (this.config.getOutputType() == ClientConfig.OutputType.OUTPUT_STANDARD) {
                    out.append(formatResponseTTY(response));
                } else if (this.config.getOutputType() == ClientConfig.OutputType.OUTPUT_CSV) {
                    out.append(formatResponseCSV(response));
                    out.append("\n");
                }
            }
            System.out.print(out.toString());
        }
        return true;
    }

    private String formatResponseRaw(Response response) {
        StringBuilder out = new StringBuilder();
        switch (response.getType()) {
            case ERROR:
                out.append(response.getContent());
                out.append("\n");
                break;
            case STATUS:
            case BULK:
                out.append(response.getContent());
                break;
            case INTEGER:
                out.append(String.valueOf(response.getContent()));
                break;
            case MULTI_BULK:
                List<String> contents = (List<String>)response.getContent();
                int len = contents.size();
                for (int i = 0; i < len; i++) {
                    if (i > 0) {
                        out.append(this.config.getMbDelim());
                    }
                    out.append(contents.get(i));
                }
                break;
            default:
                System.err.println("Unknown reply type");
                System.exit(1);
        }
        return out.toString();
    }

    private String formatResponseTTY(Response response) {
        StringBuilder out = new StringBuilder();

        switch (response.getType()) {
            case ERROR:
                out.append("(error) ");
                out.append(response.getContent());
                out.append("\n");
                break;
            case STATUS:
                out.append(response.getContent());
                out.append("\n");
                break;
            case BULK:
                out.append(StringUtil.toQuoted((String)response.getContent()));
                out.append("\n");
                break;
            case NIL:
                out.append("(nil)\n");
                break;
            case INTEGER:
                out.append("(integer) ");
                out.append(String.valueOf(response.getContent()));
                out.append("\n");
                break;
            case MULTI_BULK:
                List<String> contents = (List<String>)response.getContent();
                int len = contents.size();

                if (len == 0) {
                    out.append("(empty list or set)\n");
                } else {
                    for (int i = 0; i < len; i++) {
                        out.append(i + 1);
                        out.append(") ");
                        out.append(contents.get(i));
                        out.append("\n");
                    }
                }
                break;
            default:
                System.err.println("Unknown reply type");
                System.exit(1);
        }
        return out.toString();
    }

    private String formatResponseCSV(Response response) {
        StringBuilder out = new StringBuilder();

        switch (response.getType()) {
            case ERROR:
                out.append("ERROR, ");
                out.append("\"");
                out.append(response.getContent());
                out.append("\"");
                break;
            case STATUS:
                out.append("\"");
                out.append(response.getContent());
                out.append("\"");
                break;
            case BULK:
                out.append("\"");
                out.append(response.getContent());
                out.append("\"");
                break;
            case INTEGER:
                out.append(String.valueOf(response.getContent()));
                break;
            case MULTI_BULK:
                List<String> contents = (List<String>)response.getContent();
                int len = contents.size();
                for (int i = 0; i < len; i++) {
                    out.append("\"");
                    out.append(contents.get(i));
                    out.append("\"");
                    if (i != len - 1) {
                        out.append(", ");
                    }
                }
                break;
            default:
                System.err.println("Unknown reply type");
                System.exit(1);
        }
        return out.toString();
    }



    private String getCliVersion() {
        return "0.0.1";
    }

    /**
     * 以命令行交互模式运行
     * 用户可以输入repl格式的文本内容发送给服务器
     */
    private void runReplMode() {

        Scanner scanner = new Scanner(System.in);
        String line = null;
        String [] argv = null;

        // 等待用户输入
        System.out.println("please input command：");

        while ((line = scanner.nextLine())!= null) {
            // 测试：输出cli输入的数据
            logger.debug("cli输入的内容：" + line);

            line = line.trim();
            if (line.length() > 0) {
                // 分割参数，注意:可以允许“a b c”这种用引号括起来的表示一个整体的形式，这种应该作为一个整体
                argv = StringUtil.splitArgs(line);

                if (argv == null) {
                    System.out.print("Invalid argument(s)\n");
                    continue;
                } else if (argv.length > 0) {
                    String command = argv[0].toLowerCase();
                    if ("quit".equals(command) || "exit".equals(command)) {
                        System.exit(0);
                    } else if (argv.length == 3 && "connect".equals(command)) {
                        config.setHostip(argv[1]);
                        config.setHostport(Integer.valueOf(argv[2]));
                        cliConnect(true);
                    } else if (argv.length == 1 && "clear".equals(command)) {
                        // todo: redis中这是实现了清屏功能，暂时不实现
                        System.out.println("Warning: 暂时未实现该命令");
                    } else {
                        long startTime = System.currentTimeMillis();
                        long elapsed = 0;

                        // 因为命令第一个参数有可能是表示执行次数的数字，所以这里需要判断
                        int repeat = 1;     // 命令重复次数，默认为1
                        int skipArgs = 0;   // 若输入第一个参数是repeat次数，那么需要跳过1个参数解析后面的参数
                        if (StringUtil.isInteger(argv[0])) {
                            repeat = Integer.parseInt(argv[0]);
                            if (argv.length > 1 && repeat > 0) {
                                skipArgs = 1;
                            }
                        }

                        while (true) {
                            // 发送命令，若失败，则重试
                            if (!sendCommand(skipArgs, argv, repeat)) {
                                cliConnect(true);
                                // 若重试失败，输出错误信息
                                if (!sendCommand(skipArgs, argv, repeat)) {
                                    printContextError();
                                }
                            }

                            // Issue the command again if we got redirected in cluster mode
                            // todo: 暂时未知这段代码功能
                            if (config.isClusterMode() && config.getClusterReissueCommand()) {
                                cliConnect(true);
                            } else {
                                break;
                            }
                        }

                        // 计算执行时间
                        elapsed = System.currentTimeMillis() - startTime;
                        if (elapsed >= 500) {
                            System.out.println((double)elapsed / 1000);
                        }
                    }
                }
            }
        }
        System.exit(0);
    }

    /**
     * cli客户端主入口
     * @param args
     */
    public static void main(String [] args) {
        logger.info("zedis cli start...");

        // 初始化CLI及其默认配置
        ClientCli cli = new ClientCli();
        // 解析输入参数
        cli.parseOption(args);

        /* Latency mode */
        if (cli.config.getLatencyMode()) {
            if (!cli.cliConnect(false)) {
                System.exit(1);
            }
            // todo: latencyMode();
        }

        /* Slave mode */
        if (cli.config.isSlaveMode()) {
            if (!cli.cliConnect(false)) {
                System.exit(1);
            }
            // todo: slaveMode();
        }

        /* Get RDB mode. */
        if (cli.config.isGetrdbMode()) {
            if (!cli.cliConnect(false)) {
                System.exit(1);
            }
            // todo: getRDB();
        }

        /* Pipe mode */
        if (cli.config.isPipeMode()) {
            if (!cli.cliConnect(false)) {
                System.exit(1);
            }
            // todo: pipeMode();
        }

        /* Find big keys */
        if (cli.config.getBigkeys()) {
            if (!cli.cliConnect(false)) {
                System.exit(1);
            }
            // todo: findBigKeys();
        }

        /* Stat mode */
        if (cli.config.isStatMode()) {
            if (!cli.cliConnect(false)) {
                System.exit(1);
            }

            if (cli.config.getInterval() == 0) {
                cli.config.setInterval(1000000);
            }
            // todo: statMode();
        }

        /* Scan mode */
        if (cli.config.isScanMode()) {
            if (!cli.cliConnect(false)) {
                System.exit(1);
            }
            // todo: scanMode();
        }

        /* Intrinsic latency mode */
        if (cli.config.isIntrinsicLatencyMode()) {
            // todo: intrinsicLatencyMode();
        }

        // 当没有提供任何参数时，以交互模式启动
        if (args.length == 0 && cli.config.getEval() == null) {
            cli.cliConnect(false);
            cli.runReplMode();
        }

        /* Otherwise, we have some arguments to execute */
        if (!cli.cliConnect(false)) {
            //todo command line mode
            System.exit(1);
        }

        if (cli.config.getEval() != null) {
            // todo: return evalMode(argc,argv);
        } else {
            // todo: return noninteractive(argc,convertToSds(argc,argv));
        }
    }
}
