package com.pcy.redigrandson.config;

import com.pcy.redigrandson.DistributedLock;
import com.pcy.redigrandson.Redigrandson;
import com.pcy.redigrandson.properties.RedigradsonProperties;
import com.pcy.redigrandson.properties.RedisConnectionInfo;
import com.pcy.redigrandson.redislock.RedisDistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: 彭椿悦
 * @data: 2021/4/22 11:07
 */
@Configuration
@EnableConfigurationProperties(value = {
        RedigradsonProperties.class , RedisConnectionInfo.class
})
//@ConditionalOnProperty(prefix = "",
//        name = "",
//        havingValue = "")
public class RedigrandsonConfig {
    RedisConnectionInfo redisConnectionInfo;

    @Autowired
    public void setRedisConnectionInfo(RedisConnectionInfo redisConnectionInfo) {
        this.redisConnectionInfo = redisConnectionInfo;
    }

    @Bean
    public Redigrandson redigrandson(){
        return new Redigrandson();
    }
}
