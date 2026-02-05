package com.ld.poetry.config;

import com.ld.poetry.im.websocket.ImWebSocketHandler;
import com.ld.poetry.im.websocket.ImWebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Spring WebSocket 配置
 * 替换原 t-io WebSocket 实现
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "im.enable", havingValue = "true")
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ImWebSocketHandler imWebSocketHandler;

    @Autowired
    private ImWebSocketInterceptor imWebSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器，路径 /ws/im
        // 同时支持原生 WebSocket 和 SockJS 回退
        registry.addHandler(imWebSocketHandler, "/ws/im")
                .addInterceptors(imWebSocketInterceptor)
                .setAllowedOrigins("*");
    }
}
