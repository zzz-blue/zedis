package server;

import database.Database;
import event.EventLoop;
import server.config.ServerConfig;


public class ServerContext {
    private volatile static ServerContext context = new ServerContext();
    private volatile ZedisServer serverInstance;
    private volatile ServerConfig serverConfig;
    private volatile EventLoop eventLoop;

    public static ServerContext getContext() {
        return context;
    }

    public Database getDatabases() {
        return serverInstance.getDatabases();
    }

    public EventLoop getEventLoop() {
        return this.eventLoop;
    }

    public void setServerInstance(ZedisServer serverInstance) {
        this.serverInstance = serverInstance;
    }

    public ZedisServer getServerInstance() {
        return this.serverInstance;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setEventLoop(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public ServerConfig getServerConfig() {
        return this.serverConfig;
    }
}
