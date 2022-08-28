package common.persistence;

import common.struct.ZedisString;
import common.struct.impl.Sds;
import database.Database;
import org.junit.Assert;
import org.junit.Test;
import server.ServerContext;
import server.ZedisServer;

public class TestRdb {
    @Test
    public void testSave(){

          ZedisServer server=ZedisServer.getInstance();
//        RDBPersistence rdbPersistence = ZedisServer.getInstance().getRdbPersistence();
//        Database db = ZedisServer.getInstance().getDatabases();
//        db.setKey(Sds.createSds("name") , Sds.createSds("zzz"));
//        Assert.assertEquals(true,rdbPersistence.save());
          int i=0;
          i++;

    }
}
