package com.pcy.redigrandson.properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;

/**
 * @description:
 * @author: 彭椿悦
 * @data: 2021/4/22 10:36
 */
@ConfigurationProperties(prefix = "spring.redis")
@Validated
public class RedisConnectionInfo {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 6379;
    private String host;
    private Integer port;
    private String password;
    private Integer database;
    @PostConstruct
    private void init(){
        if (StringUtils.isEmpty(host)){
            host = DEFAULT_HOST;
        }
        if (port == null){
            port = DEFAULT_PORT;
        }
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    @Override
    public String toString() {
        return "RedisConnectionInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", password='" + password + '\'' +
                ", database=" + database +
                '}';
    }
}
