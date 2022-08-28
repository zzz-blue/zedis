package common.persistence;

import common.struct.*;
import common.utils.ByteUtil;
import common.utils.SafeEncoder;
import database.Database;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import server.ZedisServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;


public class RDBPersistence {
    private static final Log logger = LogFactory.getLog(RDBPersistence.class);

    private ZedisServer server;
    private String rdbFileName;

    private volatile boolean inBackgroundSaveProcess; // BGSAVE正在执行中

    private long lastSaveTime;  // 最近一次保存时间
    private SaveStatus lastBackgroundSaveStatus;

    public static final byte RDB_OPCODE_SELECTDB = (byte)254;
    public static final byte RDB_OPCODE_EOF = (byte)255;
    public static final byte RDB_OPCODE_EXPIRETIME_MS = (byte)252;

    public static final byte STRING = (byte)0;
    public static final byte LIST = (byte)1;
    public static final byte HASH = (byte)2;
    public static final byte SET = (byte)3;
    public static final byte ZSET = (byte)4;

    public RDBPersistence(ZedisServer server) {
        this.server = server;
        this.rdbFileName = server.getServerConfig().getRdbFileName();
        this.inBackgroundSaveProcess = false;
        this.lastSaveTime = 0;
        this.lastBackgroundSaveStatus = SaveStatus.NONE;
    }

    public boolean isInBackgroundSaveProcess() {
        return inBackgroundSaveProcess;
    }

    /**
     * 将数据库保存到磁盘上
     * @return 返回是否保存成功
     */
    public boolean save() {
        FileOutputStream fos = null;
        File tempFile = null;
        long now = System.currentTimeMillis();

        try {
            // 创建临时文件
            tempFile = new File("temp-"+Thread.currentThread().getId() + ".rdb");
            if (tempFile.exists()) {
                tempFile.delete();
            }
            tempFile.createNewFile();

            fos = new FileOutputStream(tempFile);

            // 写入 RDB 版本号
            fos.write(SafeEncoder.encode("REDIS0006"));


            Database db = server.getDatabases();

            // 指向数据库键空间
            Map<ZedisString, ZedisObject> keySpace = db.getKeySpace();



            // 创建键空间迭代器
            Iterator<Map.Entry<ZedisString, ZedisObject>> iterator = keySpace.entrySet().iterator();


            saveType(fos, RDB_OPCODE_SELECTDB);

            /*
             * 遍历数据库，并写入每个键值对的数据
             */
            while (iterator.hasNext()) {
                Map.Entry<ZedisString, ZedisObject> e = iterator.next();
                ZedisString key = e.getKey();
                ZedisObject value = e.getValue();
                Long expire = db.getExpire(key);

                saveKeyValuePair(fos, key, value, expire, now);
            }


            /*
             * 写入 EOF 代码
             */
            saveType(fos, RDB_OPCODE_EOF);

            /* CRC64 校验和。
             *
             * 如果校验和功能已关闭，那么 rdb.cksum 将为 0 ，
             * 在这种情况下， RDB 载入时会跳过校验和检查。*/


            // 冲洗缓存，确保数据已写入磁盘
            fos.flush();
            fos.getFD().sync();
            fos.close();

            //* 使用 RENAME ，原子性地对临时文件进行改名，覆盖原来的 RDB 文件。
            tempFile.renameTo(new File(this.rdbFileName));

            // 写入完成，打印日志
            logger.info("DB saved on disk");

            // 记录最后一次完成 SAVE 的时间
            this.lastSaveTime = System.currentTimeMillis();

            // 记录最后一次执行 SAVE 的状态
            this.lastBackgroundSaveStatus = SaveStatus.OK;

            return true;
        } catch (IOException e) {
            // 关闭文件
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            // 删除文件
            tempFile.delete();
            logger.error("Write error saving DB on disk", e);

            return false;
        }
    }


    public boolean backgroundSave() {
        if (this.inBackgroundSaveProcess) {
            return false;
        }
        //todo 暂时使用子线程
        Thread t = new Thread(() -> {
            boolean ret = save();
        });

        t.start();

        return true;
    }

    private void saveType(OutputStream os, byte type) {
        try {
            os.write(type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void saveString(OutputStream os, ZedisString str) {
        try {
            os.write(SafeEncoder.encode(str.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveObject(OutputStream os, ZedisObject obj) {
        if (obj instanceof ZedisString) {
            saveString(os, (ZedisString) obj);
        } else if (obj instanceof ZedisList) {
        } else if (obj instanceof ZedisHash) {
        } else if (obj instanceof ZedisSet) {
        } else if (obj instanceof ZedisZSet) {
        } else {
            logger.error("unknow object type");
        }

    }

    private void saveObjectType(OutputStream os, ZedisObject obj) {
        if (obj instanceof ZedisString) {
            saveType(os, STRING);
        } else if (obj instanceof ZedisList) {
            saveType(os, LIST);
        } else if (obj instanceof ZedisHash) {
            saveType(os, HASH);
        } else if (obj instanceof ZedisSet) {
            saveType(os, SET);
        } else if (obj instanceof ZedisZSet) {
            saveType(os, ZSET);
        } else {
            logger.error("unknow object type");
        }
    }

    private void saveKeyValuePair(OutputStream os, ZedisString key, ZedisObject value, Long expire, long now) {
        if (expire != null) {
            if (expire < now) {
                return;
            }

            saveType(os, RDB_OPCODE_EXPIRETIME_MS);
            saveMillisecondTime(os, expire);
        }

        /*
         * 保存类型，键，值
         */
        saveObjectType(os, value);
        saveString(os, key);
        saveObject(os, value);
    }

    private void saveMillisecondTime(OutputStream os, long expire) {
        byte [] bytes = ByteUtil.longToBytes(expire);
        try {
            os.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public enum SaveStatus {
        NONE, OK;
    }
}