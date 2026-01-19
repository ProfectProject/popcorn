package com.popcorn.demo.global.config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import redis.embedded.RedisServer;

@Configuration
@Profile({"local", "test"})
public class EmbeddedRedisConfig implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedRedisConfig.class);

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @Override
    public void afterPropertiesSet() {
        try {
            if (!isPortAvailable(redisPort)) {
                log.info("Embedded Redis not started. Port {} already in use.", redisPort);
                return;
            }

            redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 128M")
                .build();
            redisServer.start();
            log.info("Embedded Redis started on port {}", redisPort);
        } catch (Exception ex) {
            log.warn("Embedded Redis failed to start. Continuing without it. reason={}", ex.getMessage());
            redisServer = null;
        }
    }

    @Override
    public void destroy() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            log.info("Embedded Redis stopped");
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("localhost", port));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
