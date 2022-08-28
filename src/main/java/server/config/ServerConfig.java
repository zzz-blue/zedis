package server.config;

import common.persistence.AofFsyncFrequency;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import common.utils.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class ServerConfig {
    private static Log logger = LogFactory.getLog(ServerConfig.class);

    private String configfile;      // 配置文件路径
    private int port;               // 服务器默认端口
    private int hz;                 // serverCron每秒调用次数
    private int dbNumber;           // 数据库数量
    private String requirePassword; // 是否设置了密码
    private boolean daemonize;      // 是否以守护进程运行
    private boolean aof;            // 是否开启aof持久化
    private String aofFileName;     // aof持久化存储的文件路径
    private AofFsyncFrequency appendFsync;  // aof写入文件后，将数据从系统缓冲区强制同步到磁盘的频率
    private String rdbFileName;     // rdb持久化存储的文件路径


    private boolean isActiveExpiredEnable;

    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_HZ = 10;
    public static final int DEFAULT_DB_NUMBER = 16;

    public static final int AUTHPASS_MAX_LEN = 512;
    public static final int MIN_HZ = 1;
    public static final int MAX_HZ = 500;
    public static final String DEFAULT_AOF_FILE_NAME = "appendonly.aof";
    public static final String DEFAULT_RDB_FILE_NAME = "dump.rdb";

    private ServerConfig() {
        super();

        this.configfile = null;
        this.port = DEFAULT_PORT;
        this.hz = DEFAULT_HZ;
        this.dbNumber = DEFAULT_DB_NUMBER;
        this.requirePassword = null;
        this.daemonize = false;
        this.aof = false;
        this.aofFileName = DEFAULT_AOF_FILE_NAME;
        this.appendFsync = AofFsyncFrequency.EVERY_SECONDS;
        this.rdbFileName = DEFAULT_RDB_FILE_NAME;
        this.isActiveExpiredEnable = true;
    }

    public static ServerConfig build() {
        return build(null);
    }

    public static ServerConfig build(ServerConfigBuilder builder) {
        ServerConfig serverConfig = new ServerConfig();

        if (builder != null) {
            serverConfig.configfile = builder.configfile;
            serverConfig.port = builder.port;
            serverConfig.hz = builder.hz;
            serverConfig.dbNumber = builder.dbNumber;
            serverConfig.requirePassword = builder.requirePassword;
            serverConfig.daemonize = builder.daemonize;
            serverConfig.aof = builder.aof;
            serverConfig.aofFileName = builder.aofFileName;
            serverConfig.appendFsync = builder.appendFsync;
            serverConfig.rdbFileName = builder.rdbFileName;
            serverConfig.isActiveExpiredEnable = builder.isActiveExpiredEnable;
        }

        return serverConfig;
    }

    public void loadConfigFromFile(String filePath) {
        StringBuilder config = new StringBuilder();
        File configFile = new File(filePath);

        byte [] buf = new byte[1024];
        int byteRead = 0;
        try (FileInputStream input = new FileInputStream(configFile);) {
            while ((byteRead = input.read(buf)) != -1) {
                config.append(new String(buf, 0, byteRead));
            }
        } catch (FileNotFoundException e) {
            logger.fatal("Can't find config file:" + filePath);
            System.exit(1);
        } catch (IOException e) {
            logger.fatal("Fatal error, can't open config file" + filePath);
            System.exit(1);
        }

        loadConfigFromString(config.toString());
    }

    public void loadConfigFromString(String options) {
        int lineNum = 0, totalLines = 0;
        String err = "";

        String [] lines = options.split("\\n");

        int i = 0;
        for (; i < lines.length; i++) {
            lineNum = i + 1;

            // 移除字符串的前置空白和后缀空白
            String line = lines[i].trim();

            // 跳过空白行和注释
            if ("".equals(line) || line.charAt(0) == '#') {
                continue;
            }

            // 将字符串分割成多个参数
            String [] argv = StringUtil.splitArgs(line);
            if (argv == null) {
                logger.fatal("Unbalanced quotes in configuration line");
                System.exit(1);
            }

            // 跳过空白参数
            if (argv.length == 0) {
                continue;
            }

            String option = argv[0].toLowerCase();

            if ("timeout".equals(option) && argv.length == 2) {
                // TODO
            } else if ("tcp-keepalive".equals(option) && argv.length == 2) {
                // TODO
            } else if ("port".equals(option) && argv.length == 2) {
                int port = Integer.valueOf(argv[1]);
                if (port < 0 || port > 65535) {
                    err = "Invalid port";
                    printFatalConfigError(lineNum, lines[i], err);
                }
                this.port = port;
            } else if ("tcp-backlog".equals(option) && argv.length == 2) {
                // TODO
            } else if ("bind".equals(option) && argv.length >= 2) {
                // TODO
            } else if ("unixsocket".equals(option) && argv.length == 2) {
                // TODO
            } else if ("unixsocketperm".equals(option) && argv.length == 2) {
                // TODO
            } else if ("save".equals(option)) {
                // TODO
            } else if ("dir".equals(option) && argv.length == 2) {
                // TODO
            } else if ("loglevel".equals(option) && argv.length == 2) {
                // TODO
            } else if ("logfile".equals(option) && argv.length == 2) {
                // TODO
            } else if ("syslog-enabled".equals(option) && argv.length == 2) {
                // TODO
            } else if ("syslog-ident".equals(option) && argv.length == 2) {
                // TODO
            } else if ("syslog-facility".equals(option) && argv.length == 2) {
                // TODO
            } else if ("databases".equals(option) && argv.length == 2) {
                int dbNums = Integer.valueOf(argv[1]);
                if (dbNums < 1) {
                    err = "Invalid number of databases";
                    printFatalConfigError(lineNum, lines[i], err);
                }
                this.dbNumber = dbNums;
            } else if ("maxclient".equals(option) && argv.length == 2) {
                // TODO
            } else if ("maxmemory".equals(option) && argv.length == 2) {
                // TODO
            } else if ("maxmemory-policy".equals(option) && argv.length == 2) {
                // TODO
            } else if ("maxmemory-samples".equals(option) && argv.length == 2) {
                // TODO
            } else if ("slaveof".equals(option) && argv.length == 2) {
                // TODO
            } else if ("repl-ping-slave-period".equals(option) && argv.length == 2) {
                // TODO
            } else if ("repl-timeout".equals(option) && argv.length == 2) {
                // TODO
            } else if ("repl-disable-tcp-nodelay".equals(option) && argv.length == 2) {
                // TODO
            } else if ("repl-backlog-size".equals(option) && argv.length == 2) {
                // TODO
            } else if ("repl-backlog-ttl".equals(option) && argv.length == 2) {
                // TODO
            } else if ("masterauth".equals(option) && argv.length == 2) {
                // TODO
            } else if ("slave-serve-stale-data".equals(option) && argv.length == 2) {
                // TODO
            } else if ("slave-read-only".equals(option) && argv.length == 2) {
                // TODO
            } else if ("rdbcompression".equals(option) && argv.length == 2) {
                // TODO
            } else if ("rdbchecksum".equals(option) && argv.length == 2) {
                // TODO
            } else if ("activerehashing".equals(option) && argv.length == 2) {
                // TODO
            } else if ("daemonize".equals(option) && argv.length == 2) {
                if ("yes".equals(argv[1])) {
                    this.daemonize = true;
                } else if ("no".equals(argv[1])) {
                    this.daemonize = false;
                } else {
                    err = "argument must be yes or no";
                    printFatalConfigError(lineNum, lines[i], err);
                }
            } else if ("hz".equals(option) && argv.length == 2) {
                int hz = Integer.valueOf(argv[1]);
                if (hz < MIN_HZ) {
                    hz = MIN_HZ;
                } else if (hz > MAX_HZ) {
                    hz = MAX_HZ;
                }
                this.hz = hz;
            } else if ("appendonly".equals(option) && argv.length == 2) {
                // 是否开启aof持久化的配置选项
                boolean yes = false;
                String param = argv[1].toLowerCase();
                if (param.equals("yes")) {
                    yes = true;
                } else if (param.equals("no")) {
                    yes = false;
                } else {
                    err = "argument must be 'yes' or 'no'";
                    printFatalConfigError(lineNum, lines[i], err);
                }

                this.aof = yes;
            } else if ("appendfilename".equals(option) && argv.length == 2) {
                // 自定义aof持久化文件路径的配置选项
                File appendFile = new File(argv[1]);
                if (appendFile.isDirectory()) {
                    err = "appendfilename can't be a path, just a filename";
                    printFatalConfigError(lineNum, lines[i], err);
                }
                this.aofFileName = argv[1];
            } else if ("no-appendfsync-on-rewrite".equals(option)) {
                // TODO
            } else if ("appendfsync".equals(option) && argv.length == 2) {
                // 定义将写入文件的数据从系统缓冲区同步到磁盘的频率
                String frequency = argv[1].toLowerCase();
                switch (frequency) {
                    case "no":
                        this.appendFsync = AofFsyncFrequency.NO;
                        break;
                    case "everysec":
                        this.appendFsync = AofFsyncFrequency.EVERY_SECONDS;
                        break;
                    case "always":
                        this.appendFsync = AofFsyncFrequency.ALWAYS;
                        break;
                    default:
                        err = "argument must be 'no', 'always' or 'everysec'";
                        printFatalConfigError(lineNum, lines[i], err);
                }
            } else if ("auto-aof-rewrite-percentage".equals(option) && argv.length == 2) {
                // TODO
            } else if ("auto-aof-rewrite-min-size".equals(option) && argv.length == 2) {
                // TODO
            } else if ("aof-rewrite-incremental-fsync".equals(option) && argv.length == 2) {
                // TODO
            } else if ("requirepass".equals(option) && argv.length == 2) {
                if (argv[1].length() > AUTHPASS_MAX_LEN) {
                    err = "Password is longer than AUTHPASS_MAX_LEN";
                    printFatalConfigError(lineNum, lines[i], err);
                }
                this.requirePassword = argv[1];
            } else if ("pidfile".equals(option) && argv.length == 2) {
                // TODO
            } else if ("dbfilename".equals(option) && argv.length == 2) {
                // TODO
            } else if ("hash-max-ziplist-entries".equals(option) && argv.length == 2) {
                // TODO
            } else if ("hash-max-ziplist-value".equals(option) && argv.length == 2) {
                // TODO
            } else if ("list-max-ziplist-entries".equals(option) && argv.length == 2) {
                // TODO
            } else if ("list-max-ziplist-value".equals(option) && argv.length == 2) {
                // TODO
            } else if ("set-max-intset-entries".equals(option) && argv.length == 2) {
                // TODO
            } else if ("zset-max-ziplist-entries".equals(option) && argv.length == 2) {
                // TODO
            } else if ("zset-max-ziplist-value".equals(option) && argv.length == 2) {
                // TODO
            } else if ("hll-sparse-max-bytes".equals(option) && argv.length == 2) {
                // TODO
            } else if ("rename-command".equals(option) && argv.length == 3) {
                // TODO
            } else if ("cluster-enabled".equals(option) && argv.length == 2) {
                // TODO
            } else if ("cluster-config-file".equals(option) && argv.length == 2) {
                // TODO
            } else if ("cluster-node-timeout".equals(option) && argv.length == 2) {
                // TODO
            } else if ("cluster-migration-barrier".equals(option) && argv.length == 2) {
                // TODO
            } else if ("lua-time-limit".equals(option) && argv.length == 2) {
                // TODO
            } else if ("slowlog-log-slower-than".equals(option) && argv.length == 2) {
                // TODO
            } else if ("slowlog-max-len".equals(option) && argv.length == 2) {
                // TODO
            } else if ("server.client-output-buffer-limit".equals(option) && argv.length == 5) {
                // TODO
            } else if ("stop-writes-on-bgsave-error".equals(option) && argv.length == 2) {
                // TODO
            } else if ("slave-priority".equals(option) && argv.length == 2) {
                // TODO
            } else if ("min-slaves-to-write".equals(option) && argv.length == 2) {
                // TODO
            } else if ("min-slaves-max-lag".equals(option) && argv.length == 2) {
                // TODO
            } else if ("pubsub.notify-keyspace-events".equals(option) && argv.length == 2) {
                // TODO
            } else if ("sentinel".equals(option)) {
                // TODO
            } else {
                // TODO
            }
        }

        printConfig();

    }

    private void printFatalConfigError(int lineNum, String line, String err) {
        System.err.println("\n*** FATAL CONFIG FILE ERROR ***\n");
        System.err.println("Reading the configuration file, at line " + lineNum + "\n");
        System.err.println(">>> " + line + "\n");
        System.err.println(err + "\n");
        System.exit(1);
    }

    public int getPort() {
        return this.port;
    }

    public int getHz() {
        return this.hz;
    }

    public int getDbNumber() {
        return this.dbNumber;
    }


    public String getRequirePassword() {
        return this.requirePassword;
    }

    public void setConfigfile(String configFile) {
        this.configfile = configFile;
    }

    public String getConfigfile() {
        return this.configfile;
    }

    public boolean getDaemonize() {
        return this.daemonize;
    }

    public boolean isAofOn() {
        return this.aof;
    }

    public String getAofFileName() {
        return this.aofFileName;
    }

    public AofFsyncFrequency getAppendFsync() {
        return this.appendFsync;
    }

    public String getRdbFileName() {
        return this.rdbFileName;
    }

    public boolean isActiveExpiredEnable() {
        return this.isActiveExpiredEnable;
    }

    /**
     * 建造者模式应用
     */
    public static class ServerConfigBuilder {
        private String configfile;      // 配置文件路径
        private int port;               // 服务器默认端口
        private int hz;                 // serverCron执行频率
        private int dbNumber;
        private String requirePassword; // 是否设置了密码
        private boolean daemonize;      // 是否以守护进程运行
        private boolean aof;
        private String aofFileName;
        private AofFsyncFrequency appendFsync;
        private String rdbFileName;
        private boolean isActiveExpiredEnable;

        public ServerConfigBuilder() {
            this.configfile = null;      // 配置文件路径
            this.port = DEFAULT_PORT;    // 服务器默认端口
            this.hz = DEFAULT_HZ;
            this.dbNumber = DEFAULT_DB_NUMBER;
            this.requirePassword = null;
            this.daemonize = false;
            this.aof = false;
            this.aofFileName = DEFAULT_AOF_FILE_NAME;
            this.appendFsync = AofFsyncFrequency.EVERY_SECONDS;
            this.rdbFileName = DEFAULT_RDB_FILE_NAME;
            this.isActiveExpiredEnable = true;
        }

        public ServerConfigBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        public ServerConfigBuilder setHz(int hz) {
            this.hz = hz;
            return this;
        }

        public ServerConfigBuilder setDbNumber(int dbNumber) {
            this.dbNumber = dbNumber;
            return this;
        }

        public ServerConfigBuilder setConfigfile(String configfile) {
            this.configfile = configfile;
            return this;
        }

        public ServerConfigBuilder setRequirePassword(String password) {
            this.requirePassword = password;
            return this;
        }

        public ServerConfigBuilder setDaemonize(boolean daemonize) {
            this.daemonize = daemonize;
            return this;
        }

        public ServerConfigBuilder setAof(boolean on) {
            this.aof = on;
            return this;
        }

        public ServerConfigBuilder setAofFileName(String fileName) {
            this.aofFileName = fileName;
            return this;
        }

        public ServerConfigBuilder setAppendFsync(AofFsyncFrequency frequency) {
            this.appendFsync = frequency;
            return this;
        }

        public ServerConfigBuilder setRdbFileName(String fileName) {
            this.rdbFileName = fileName;
            return this;
        }
    }

    public void printConfig() {
        System.out.println("port:" + this.port);
        System.out.println("hz:" + this.hz);
        System.out.println("databases:" + this.dbNumber);
        System.out.println("daemonize:" + this.daemonize);
        System.out.println("requirepass:" + this.requirePassword);
    }
}
