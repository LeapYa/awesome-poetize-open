package com.ld.poetry.im.websocket;

import com.ld.poetry.entity.User;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.SysConfigService;
import com.ld.poetry.utils.CommonQuery;
import com.ld.poetry.utils.SecureTokenGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 替换 t-io 的 handshake 方法，进行 Token 验证
 */
@Component
@Slf4j
public class ImWebSocketInterceptor implements HandshakeInterceptor {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CommonQuery commonQuery;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 检查IM功能是否启用
        String imEnable = sysConfigService.getConfigValueByKey("im.enable");
        if ("false".equalsIgnoreCase(imEnable)) {
            log.warn("WebSocket握手失败：IM功能已禁用");
            return false;
        }

        // 从URL参数获取token
        String token = null;
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            token = servletRequest.getServletRequest().getParameter("token");
        }

        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket握手失败：token为空");
            return false;
        }

        try {
            // 使用SecureTokenGenerator验证token
            SecureTokenGenerator.TokenValidationResult validationResult = SecureTokenGenerator.validateToken(token);

            if (!validationResult.isValid()) {
                log.warn("WebSocket握手失败：token验证失败 - {}, 原因: {}", token, validationResult.getErrorMessage());
                return false;
            }

            Integer userId = validationResult.getUserId();
            if (userId == null) {
                log.warn("WebSocket握手失败：无法从token中获取用户ID - {}", token);
                return false;
            }

            // 获取用户信息
            User user = cacheService.getCachedUser(userId);
            if (user == null) {
                user = commonQuery.getUser(userId);
                if (user == null) {
                    log.warn("WebSocket握手失败：用户信息不存在 - userId: {}", userId);
                    return false;
                }
                cacheService.cacheUser(user);
            }

            // 将用户信息存入 attributes，供后续处理器使用
            attributes.put("userId", user.getId());
            attributes.put("username", user.getUsername());
            attributes.put("avatar", user.getAvatar());
            attributes.put("userType", validationResult.getUserType());

            log.info("WebSocket握手成功：用户ID：{}, 用户名：{}, token类型：{}",
                    user.getId(), user.getUsername(), validationResult.getUserType());
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手时验证用户失败 - token: {}", token, e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的回调，一般不需要处理
    }
}
