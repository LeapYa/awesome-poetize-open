package com.ld.poetry.im.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 会话管理器
 * 替换 t-io 的 Tio.bindUser/bindGroup/sendToUser/sendToGroup 等 API
 */
@Component
@Slf4j
public class ImSessionManager {

    // 用户ID -> WebSocketSession集合（支持多端登录）
    private final ConcurrentHashMap<Integer, CopyOnWriteArraySet<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // 群组ID -> 用户Session集合
    private final ConcurrentHashMap<String, Set<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();

    // Session ID -> 用户ID 映射（用于反向查找）
    private final ConcurrentHashMap<String, Integer> sessionUserMap = new ConcurrentHashMap<>();

    // Session ID -> 群组ID集合（用于清理）
    private final ConcurrentHashMap<String, Set<String>> sessionGroupMap = new ConcurrentHashMap<>();

    /**
     * 绑定用户
     */
    public void bindUser(Integer userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionUserMap.put(session.getId(), userId);
        log.debug("用户{}绑定到会话{}", userId, session.getId());
    }

    /**
     * 加入群组
     */
    public void joinGroup(String groupId, WebSocketSession session) {
        groupSessions.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionGroupMap.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet()).add(groupId);
        log.debug("会话{}加入群组{}", session.getId(), groupId);
    }

    /**
     * 离开群组
     */
    public void leaveGroup(String groupId, WebSocketSession session) {
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions != null) {
            sessions.remove(session);
        }
        Set<String> groups = sessionGroupMap.get(session.getId());
        if (groups != null) {
            groups.remove(groupId);
        }
    }

    /**
     * 移除会话（连接关闭时调用）
     */
    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();

        // 移除用户绑定
        Integer userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            log.debug("用户{}的会话已移除", userId);
        }

        // 移除群组绑定
        Set<String> groups = sessionGroupMap.remove(sessionId);
        if (groups != null) {
            for (String groupId : groups) {
                Set<WebSocketSession> sessions = groupSessions.get(groupId);
                if (sessions != null) {
                    sessions.remove(session);
                }
            }
        }
    }

    /**
     * 发送消息给指定用户
     */
    public boolean sendToUser(Integer userId, String message) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            boolean sent = false;
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        sent = true;
                    } catch (IOException e) {
                        log.error("发送消息给用户{}的会话{}失败: {}", userId, session.getId(), e.getMessage());
                    }
                }
            }
            return sent;
        }
        return false;
    }

    /**
     * 发送消息给群组所有成员
     */
    public void sendToGroup(String groupId, String message) {
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions != null && !sessions.isEmpty()) {
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("发送消息给群组{}的会话{}失败: {}", groupId, session.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 发送消息给群组所有成员，排除指定会话
     */
    public void sendToGroupExclude(String groupId, String message, WebSocketSession excludeSession) {
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions != null && !sessions.isEmpty()) {
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen() && !session.getId().equals(excludeSession.getId())) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("发送消息给群组{}的会话{}失败: {}", groupId, session.getId(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 获取群组在线人数
     */
    public int getGroupOnlineCount(String groupId) {
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions == null) {
            return 0;
        }
        // 只统计仍然打开的连接
        return (int) sessions.stream().filter(WebSocketSession::isOpen).count();
    }

    /**
     * 获取用户ID（通过Session）
     */
    public Integer getUserId(WebSocketSession session) {
        return sessionUserMap.get(session.getId());
    }

    /**
     * 获取用户所在的群组
     */
    public Set<String> getUserGroups(WebSocketSession session) {
        return sessionGroupMap.get(session.getId());
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Integer userId) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty() && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * 获取用户的Session集合
     */
    public Set<WebSocketSession> getUserSessions(Integer userId) {
        return userSessions.get(userId);
    }

    /**
     * 获取总在线用户数
     */
    public int getTotalOnlineCount() {
        return userSessions.size();
    }

    /**
     * 关闭指定用户的会话（管理员强制下线使用）
     */
    public void closeUserSession(Integer userId, String reason) {
        CopyOnWriteArraySet<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        log.info("关闭用户{}的WebSocket连接，原因: {}", userId, reason);
                        session.close(CloseStatus.NORMAL.withReason(reason));
                    } catch (IOException e) {
                        log.warn("关闭用户{}的连接失败: {}", userId, e.getMessage());
                    }
                }
            }
        }
    }
}
