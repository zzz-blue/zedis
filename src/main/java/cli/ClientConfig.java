package cli;

/**
 * @Description 客户端配置
 * @Author zzz
 * @Date 2021/9/24
 **/
public class ClientConfig {
    private String hostip;
    private int hostport;
    private String hostsocket;
    private long repeat;
    private long interval;
    private boolean interactive;
    private boolean shutdown;
    private boolean monitorMode;
    private boolean pubsubMode;
    private boolean latencyMode;
    private boolean latencyHistory;
    private boolean clusterMode;
    private boolean clusterReissueCommand;
    private boolean slaveMode;
    private boolean pipeMode;
    private int pipeTimeout;
    private boolean getrdbMode;
    private boolean statMode;
    private boolean scanMode;
    private boolean intrinsicLatencyMode;
    private int intrinsicLatencyDuration;
    private String pattern;
    private String rdbFilename;
    private boolean bigkeys;
    private boolean stdinarg; /* get last arg from stdin. (-x option) */
    private String auth;
    private OutputType outputType; /* output mode, see OUTPUT_* defines */
    private String mbDelim;
//    char prompt[128];
    private String eval;

    public static final int CLI_DEFAULT_PIPE_TIMEOUT = 30;  // seconds

    public enum OutputType {
        OUTPUT_STANDARD,
        OUTPUT_RAW,
        OUTPUT_CSV;
    }

    public ClientConfig() {
        this.hostip = "127.0.0.1";
        this.hostport = 6379;
        this.hostsocket = null;
        this.repeat = 1;
        this.interval = 0;
        this.interactive = false;
        this.shutdown = false;
        this.monitorMode = false;
        this.pubsubMode = false;
        this.latencyMode = false;
        this.latencyHistory = false;
        this.clusterMode = false;
        this.slaveMode = false;
        this.pipeMode = false;
        this.pipeTimeout = CLI_DEFAULT_PIPE_TIMEOUT;
        this.getrdbMode = false;
        this.statMode = false;
        this.scanMode = false;
        this.intrinsicLatencyMode = false;
        this.intrinsicLatencyDuration = 0;
        this.pattern = null;
        this.rdbFilename = null;
        this.bigkeys = false;
        this.stdinarg = false;
        this.hostsocket = null;
        this.auth = null;
        this.mbDelim = null;
        this.eval = null;

        // todo
        if (!(System.console() == null) && System.getenv("FAKETTY") == null) {
            this.outputType = OutputType.OUTPUT_RAW;
        } else {
            this.outputType = OutputType.OUTPUT_STANDARD;
        }

        this.mbDelim = "\n";
    }

    void setHostip(String hostip) {
        this.hostip = hostip;
    }

    String getHostip() {
        return this.hostip;
    }

    void setHostport(int port) {
        this.hostport = port;
    }

    int getHostport() {
        return this.hostport;
    }

    void setHostsocket(String hostsocket) {
        this.hostsocket = hostsocket;
    }

    String getHostsocket() {
        return this.hostsocket;
    }

    void setRepeat(long times) {
        this.repeat = times;
    }

    void setInterval(long interval) {
        this.interval = interval;
    }

    long getInterval() {
        return this.interval;
    }


    boolean isInteractive() {
        return this.interactive;
    }

    void setShutdown(boolean open) {
        this.shutdown = open;
    }

    boolean isShutdown() {
        return this.shutdown;
    }

    void setMonitorMode(boolean open) {
        this.monitorMode = open;
    }

    boolean isMonitorMode() {
        return this.monitorMode;
    }

    void setPubsubMode(boolean open) {
        this.pubsubMode = open;
    }

    boolean isPubsubMode() {
        return this.pubsubMode;
    }

    void setLatencyMode(boolean open) {
        this.latencyMode = open;
    }

    boolean getLatencyMode() {
        return this.latencyMode;
    }

    void setLatencyHistory(boolean open) {
        this.latencyHistory = open;
    }

    void setClusterMode(boolean open) {
        this.clusterMode = open;
    }

    boolean isClusterMode() {
        return this.clusterMode;
    }

    void setClusterReissueCommand(boolean open) {
        this.clusterReissueCommand = open;
    }

    boolean getClusterReissueCommand() {
        return this.clusterReissueCommand;
    }

    void setSlaveMode(boolean open) {
        this.slaveMode = open;
    }

    boolean isSlaveMode() {
        return this.slaveMode;
    }

    void setPipeMode(boolean open) {
        this.pipeMode = open;
    }

    boolean isPipeMode() {
        return this.pipeMode;
    }

    void setPipeTimeout(int timeout) {
        this.pipeTimeout = timeout;
    }

    void setGetrdbMode(boolean open) {
        this.getrdbMode = open;
    }

    boolean isGetrdbMode() {
        return this.getrdbMode;
    }

    void setStatMode(boolean open) {
        this.statMode = open;
    }

    boolean isStatMode() {
        return this.statMode;
    }

    void setScanMode(boolean open) {
        this.scanMode = open;
    }

    boolean isScanMode() {
        return this.scanMode;
    }

    void setIntrinsicLatencyMode(boolean open) {
        this.intrinsicLatencyMode = open;
    }

    boolean isIntrinsicLatencyMode() {
        return this.intrinsicLatencyMode;
    }

    void setIntrinsicLatencyDuration(int duration) {
        this.intrinsicLatencyDuration = duration;
    }

    void setPattern(String pattern) {
        this.pattern = pattern;
    }

    void setRdbFilename(String filename) {
        this.rdbFilename = filename;
    }

    void setBigkeys(boolean open) {
        this.bigkeys = open;
    }

    boolean getBigkeys() {
        return this.bigkeys;
    }

    void setStdinarg(boolean open) {
        this.stdinarg = open;
    }

    void setAuth(String auth) {
        this.auth = auth;
    }

    void setOutputType(OutputType type) {
        this.outputType = type;
    }

    OutputType getOutputType() {
        return this.outputType;
    }

    void setMbDelim(String delim) {
        this.mbDelim = delim;
    }

    String getMbDelim() {
        return this.mbDelim;
    }

    void setEval(String eval) {
        this.eval = eval;
    }

    String getEval() {
        return this.eval;
    }
}
