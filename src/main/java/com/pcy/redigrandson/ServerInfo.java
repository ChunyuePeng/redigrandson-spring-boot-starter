package com.pcy.redigrandson;

/**
 * @description: Redis Server连接信息
 * @author: 彭椿悦
 * @data: 2021/4/23 9:41
 */
public class ServerInfo {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DATABASE = 0;
    private String host = DEFAULT_HOST;
    private Integer port = DEFAULT_PORT;
    private Integer database = DEFAULT_DATABASE;
    private String password;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
