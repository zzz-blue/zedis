package common.persistence;

import common.struct.impl.Sds;
import common.utils.SafeEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import remote.protocol.Protocol;

import java.io.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 将 AOF 缓存写入到文件中。
 *
 * 因为程序需要在回复客户端之前对 AOF 执行写操作。
 * 而客户端能执行写操作的唯一机会就是在事件 loop 中，
 * 因此，程序将所有 AOF 写累积到缓存中，
 * 并在重新进入事件 loop 之前，将缓存写入到文件中。
 *
 * 关于 force 参数：
 *
 * 当 fsync 策略为每秒钟保存一次时，如果后台线程仍然有 fsync 在执行，
 * 那么我们可能会延迟执行冲洗（flush）操作，
 * 因为 Linux 上的 write(2) 会被后台的 fsync 阻塞。
 *
 * 当这种情况发生时，说明需要尽快冲洗 aof 缓存，
 * 程序会尝试在 serverCron() 函数中对缓存进行冲洗。
 *
 * 不过，如果 force 为 1 的话，那么不管后台是否正在 fsync ，
 * 程序都直接进行写入。
 **/
public class AOFPersistence {
    private static final Log logger = LogFactory.getLog(AOFPersistence.class);

    private String aofFileName;
    private File aofFile;
    private FileOutputStream fileOutputStream;
    private FileDescriptor fd;

    private AofFsyncFrequency appendFsync;
    // 缓冲区，aof的命令内容都被放入缓冲区，而非直接写入文件
    private Sds aofBuffer;
    private long aofLastFsync;

    private long aofFlushPostponedStart;

    private ReentrantLock lock = new ReentrantLock();
    private volatile boolean backgroundFsyncInProcess;


    public AOFPersistence(String aofFileName, AofFsyncFrequency appendFsync) {
        this.aofFileName = aofFileName;
        this.aofFile = new File(aofFileName);
        if (!this.aofFile.exists()) {
            try {
                this.aofFile.createNewFile();
            } catch (IOException e) {
                logger.error("can not create aof file with name: " + this.aofFileName, e);
            }
        }

        try {
            this.fileOutputStream = new FileOutputStream(this.aofFile, true);
            this.fd = this.fileOutputStream.getFD();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.appendFsync = appendFsync;
        this.aofBuffer = Sds.createEmptySds();
        this.aofLastFsync = -1;
        this.aofFlushPostponedStart = 0;

        this.backgroundFsyncInProcess = false;

    }

    /**
     * 当用户在运行时使用 CONFIG 命令，从 appendonly no切换到 appendonly yes 时执行
     */
    public void startAppendOnly() {
        // 更新zedis最近一次执行aof的时间
        this.aofLastFsync = System.currentTimeMillis();

        // 打开 AOF 文件
        try {
            File aofFile = new File(this.aofFileName);
            if (!aofFile.exists()) {
                aofFile.createNewFile();
            }
            this.fileOutputStream = new FileOutputStream(aofFile, true);
            this.fd = this.fileOutputStream.getFD();
        } catch (IOException e) {
            logger.error("Zedis needs to enable the AOF but can't open the append only file: " + this.aofFileName, e);
        }

        // todo
        // rewriteAppendOnlyFileBackground
        // 可能是将现有的数据库数据写入aof文件
    }

    public void flushAppendOnlyFile(boolean force) {
        // 缓冲区中没有任何内容，直接返回
        if (aofBuffer.isEmpty()) {
            return;
        }

        // 是否有 SYNC 正在后台进行？
        boolean fsyncInProgress = false;

        // 特殊情况：
        // 如果是everysec的情况，有可能遇到有fsync正在运行，那么为了主线程不阻塞，可以适当进行推迟
        if (this.appendFsync == AofFsyncFrequency.EVERY_SECONDS) {
            // todo
             fsyncInProgress = this.backgroundFsyncInProcess;

            if (!force) {
                if (fsyncInProgress) {
                    // 有 fsync 在后台进行
                    // 此时能推迟当前的同步尽量推迟当前的同步
                    if (this.aofFlushPostponedStart == 0) {
                        // 前面没有推迟过 write 操作，这里将推迟写操作的时间记录下来
                        // 然后就返回，不执行 write 或者 fsync
                        this.aofFlushPostponedStart = System.currentTimeMillis();
                        return ;
                    } else if (System.currentTimeMillis() - this.aofFlushPostponedStart <= 2000) {
                        // 如果之前已经因为 fsync 而推迟了 write 操作
                        // 但是推迟的时间不超过 2 秒，那么直接返回
                        // 不执行 write 或者 fsync
                        return;
                    } else {
                        // 如果后台还有 fsync 在执行，并且 write 已经推迟 >= 2 秒，此时不能再推迟了
                        // 那么执行写操作（write 将被阻塞）
                        logger.info("Asynchronous AOF fsync is taking too long (disk is busy?). Writing the AOF buffer without waiting for fsync to complete, this may slow down Zedis.");
                    }
                }
            }
        }

        /**********************************************
         * 执行到这里，程序会对 AOF 文件进行写入。
         *********************************************/

        // 清零延迟 write 的时间记录
        this.aofFlushPostponedStart = 0;

        // 执行单个 write 操作，如果写入设备是物理的话，那么这个操作应该是原子的
        // 当然，如果出现像电源中断这样的不可抗现象，那么 AOF 文件也是可能会出现问题的
        // 这时就要用 redis-check-aof 程序来进行修复
        write();

        /******************************************
         * 根据磁盘磁盘频率的策略执行, 同步到磁盘
         *****************************************/

        if (this.appendFsync == AofFsyncFrequency.ALWAYS) {
            // 总是执行同步
            fsync();
        } else if (this.appendFsync == AofFsyncFrequency.EVERY_SECONDS) {
            // 每秒进行一次fsync，并且距离上一次fsync已经超过一秒
            if (System.currentTimeMillis() - this.aofLastFsync >= 1000) {
                if (!fsyncInProgress) {
                    fsyncBackground(this.fd);
                }
            }
        }

        // 更新最后一次执行 fsnyc 的时间
        this.aofLastFsync = System.currentTimeMillis();
    }

    /**
     * 将命令进行持久化
     * AOF持久化由AOFPersistence模块完成
     * 该函数调用时其实名没有持久化，为了性能，其实是将命令转换为协议格式后放入了缓冲区中
     * 而缓冲区中的命令将在服务器的周期时间事件中写入文件
     * 并且在该
     * @param command
     */
    public void feedCommand(List<Sds> command) {


        // 转换为协议格式
        String commandFormanted = transformCommandToProtocalFormat(command);

        // 将命令追加到 AOF 缓存中，
        // 在重新进入事件循环之前，这些命令会被同步到磁盘上，
        // 并向客户端返回一个回复。
        this.aofBuffer.append(SafeEncoder.encode(commandFormanted));
    }

    public void destroy() {
        try {
            this.fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write() {
        try {
            this.fileOutputStream.write(this.aofBuffer.toArrayWithOutCopy(), 0, this.aofBuffer.length());
            this.aofBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fsync() {
        try {
            System.out.println("测试：同步" + System.currentTimeMillis());
            this.fd.sync();
        } catch (SyncFailedException e) {
            e.printStackTrace();
        }
    }

    private void fsyncBackground(FileDescriptor fd) {
        AOFPersistence instance = this;

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                instance.backgroundFsyncInProcess = true;
                lock.unlock();

                try {
                    System.out.println("开始aof持久化");
                    fd.sync();
                } catch (SyncFailedException e) {
                    e.printStackTrace();
                } finally {
                    lock.lock();
                    instance.backgroundFsyncInProcess = false;
                    lock.unlock();
                }
            }
        });
        t.start();
    }

    public long getAofFlushPostponedStart() {
        return this.aofFlushPostponedStart;
    }

    public String transformCommandToProtocalFormat(List<Sds> commands) {
        StringBuilder message = new StringBuilder();

        message.append(Protocol.MULTI_BULK_PREFIX);
        message.append(commands.size());
        message.append(Protocol.DELIMITER);

        for (Sds str : commands) {
            message.append(Protocol.BULK_PREFIX);
            // 这里记录的长度不是String类型content本身的长度，而应该是String转为UTF-8的bytes数组的长度
            int contentByteLength = str.length();
            message.append(contentByteLength);
            message.append(Protocol.DELIMITER);
            message.append(str.toString());
            message.append(Protocol.DELIMITER);
        }

        return message.toString();
    }
}
