package com.adp.site.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${application.redis.redisServer}")
    private String redisServer;

    @Value("${application.redis.redisTimeOut}")
    private Integer redisTimeOut;

    @Value("${application.redis.jedisMaxTotal}")
    private Integer jedisMaxTotal;

    @Value("${application.redis.jedisMaxIdle}")
    private Integer jedisMaxIdle;

    @Value("${application.redis.jedisMinIdle}")
    private Integer jedisMinIdle;

    @Value("${application.redis.jedisMinEvictableIdleTimeMillis}")
    private Integer jedisMinEvictableIdleTimeMillis;

    @Value("${application.redis.jedisTimeBetweenEvictionRunsMillis}")
    private Integer jedisTimeBetweenEvictionRunsMillis;

    @Value("${application.redis.jedisNumTestsPerEvictionRun}")
    private Integer jedisNumTestsPerEvictionRun;

    @Value("${application.redis.jedisPassword}")
    private String jedisPassword;

    @Value("${application.redis.jedisPort}")
    private Integer jedisPort;



    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    private GenericObjectPoolConfig<Connection> jedisPoolConfig() {
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(jedisMaxTotal);
        poolConfig.setMaxIdle(jedisMaxIdle);
        poolConfig.setMinIdle(jedisMinIdle);
        poolConfig.setMinEvictableIdleTime(Duration.ofMillis(jedisMinEvictableIdleTimeMillis));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(jedisTimeBetweenEvictionRunsMillis));
        poolConfig.setNumTestsPerEvictionRun(jedisNumTestsPerEvictionRun);
        poolConfig.setMaxWait(Duration.ofMillis(redisTimeOut));
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setTestOnBorrow(true);
        return poolConfig;
    }

    private DefaultJedisClientConfig jedisClientConfig() {
        return DefaultJedisClientConfig.builder().password(jedisPassword).database(15).build();
    }

    private HostAndPort hostAndPort() {
        return new HostAndPort(redisServer, jedisPort);
    }

    @Bean
    public JedisPooled getRedisPool() {
        return new JedisPooled(jedisPoolConfig(), hostAndPort(), jedisClientConfig());
    }
}
