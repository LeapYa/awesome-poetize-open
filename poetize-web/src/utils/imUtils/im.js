/**
 * IM WebSocket 管理模块
 * 从 poetize-im-ui/src/utils/im.js 迁移并适配到 Pinia
 */
import Tiows from './tiows.js'
import { WebSocketStateMachine, WsState } from './wsStateMachine.js'
import { TimerManager, TimerNames } from './timerManager.js'
import { ReconnectStrategy, ReconnectManager } from './reconnectStrategy.js'
import { message as ElMessage } from '@/utils/element-command-services';
import constant from '../constant'

// 全局清理旧实例的引用
let currentImInstance = null

export default function (imStore) {
    // 清理旧实例（开发环境热更新时）
    if (currentImInstance) {
        console.log('检测到旧的IM实例，执行清理...')
        if (currentImInstance.destroy) {
            currentImInstance.destroy()
        }
    }

    // 保存当前实例
    currentImInstance = this

    // ==================== 初始化核心组件 ====================

    this.ws_protocol = constant.wsProtocol || (location.protocol === 'https:' ? 'wss' : 'ws')
    this.ip = location.host
    this.port = constant.wsPort || ''
    this.imStore = imStore

    // 状态机
    this.stateMachine = new WebSocketStateMachine()

    // 定时器管理器
    this.timerManager = new TimerManager()

    // 重连策略和管理器
    this.reconnectStrategy = new ReconnectStrategy({
        maxAttempts: 10,
        baseDelay: 1000,
        maxDelay: 60000,
        backoffFactor: 2,
    })
    this.reconnectManager = new ReconnectManager(
        this.reconnectStrategy,
        this.timerManager
    )

    // WebSocket实例
    this.tio = null

    // 保存事件监听器引用，用于清理
    this.eventListeners = {
        visibilityChange: null,
        windowFocus: null,
        windowOnline: null,
        windowOffline: null,
    }

    // 页面状态
    this.isPageVisible = true
    this.lastHeartbeatResponse = Date.now()

    // Token管理
    this.currentToken = null
    this.paramStr = ''

    // ==================== Token相关方法 ====================

    /**
     * 初始化Token
     */
    this.initializeToken = () => {
        // 从URL参数中获取WebSocket token
        const urlParams = new URLSearchParams(window.location.search)
        this.currentToken = urlParams.get('token')

        if (this.currentToken) {
            this.paramStr = 'token=' + this.currentToken
        } else {
            // 没有token时，将在initWs中通过cookie会话从后端获取wsToken
            console.debug('[Token] 未找到WebSocket token，将在连接时通过cookie获取')
            this.paramStr = ''
        }
    }

    // 初始化token
    this.initializeToken()

    /**
     * 更新参数字符串
     */
    this.updateParamStr = () => {
        const urlParams = new URLSearchParams(window.location.search)
        const urlToken = urlParams.get('token')

        if (urlToken && urlToken !== this.currentToken) {
            console.log('[Token] 从URL获取到新token')
            this.currentToken = urlToken
        }

        if (!this.currentToken) {
            // 没有可用token，需要通过initWs重新获取
            console.debug('[Token] 无可用token，需要重新连接')
        }

        if (this.currentToken) {
            this.paramStr = 'token=' + encodeURIComponent(this.currentToken)
        } else {
            console.error('[Token] 无法获取有效token')
            this.paramStr = ''
        }
    }

    /**
     * 更新token
     */
    this.updateToken = (newToken) => {
        console.log('[Token] 更新token')
        this.currentToken = newToken
        this.updateParamStr()

        try {
            const url = new URL(window.location.href)
            url.searchParams.set('token', newToken)
            const newUrl = url.pathname + url.search + url.hash
            window.history.replaceState({}, '', newUrl)
            console.log('[Token] URL参数已更新')
        } catch (error) {
            console.warn('[Token] 更新URL参数失败:', error)
        }
    }

    /**
     * 检查token有效期并在需要时续签
     */
    this.checkAndRenewToken = async () => {
        if (!this.currentToken) {
            console.warn('[Token] 没有可用的token进行续签检查')
            return
        }

        try {
            console.log('[Token] 检查token有效期...')
            const baseURL = constant.baseURL || `${location.protocol}//${location.host}/api`
            const response = await fetch(
                `${baseURL}/im/checkWsTokenExpiry?wsToken=${this.currentToken}`
            )
            const result = await response.json()

            if (
                result.code === 200 &&
                result.data !== null &&
                result.data !== undefined
            ) {
                const remainingMinutes = result.data
                console.log(`[Token] 剩余有效期: ${remainingMinutes}分钟`)

                if (remainingMinutes <= 10) {
                    console.log('[Token] Token即将过期，开始续签...')
                    await this.renewToken()
                }
            } else {
                console.warn('[Token] 检查token有效期失败:', result.message)
                if (
                    result.message &&
                    (result.message.includes('无效') ||
                        result.message.includes('过期') ||
                        result.message.includes('失效'))
                ) {
                    await this.renewToken()
                }
            }
        } catch (error) {
            console.error('[Token] 检查token有效期时发生错误:', error)
        }
    }

    /**
     * 续签token
     */
    this.renewToken = async () => {
        if (!this.currentToken) {
            console.error('[Token] 没有可用的token进行续签')
            return false
        }

        try {
            console.log('[Token] 开始续签...')
            const baseURL = constant.baseURL || `${location.protocol}//${location.host}/api`
            const response = await fetch(
                `${baseURL}/im/renewWsToken?oldToken=${this.currentToken}`
            )
            const result = await response.json()

            if (result.code === 200 && result.data) {
                const newToken = result.data
                console.log('[Token] 续签成功')
                this.updateToken(newToken)
                return true
            } else {
                console.error('[Token] 续签失败:', result.message)
                if (
                    result.message &&
                    (result.message.includes('失效') ||
                        result.message.includes('过期') ||
                        result.message.includes('无效'))
                ) {
                    return await this.regenerateTokenWithUserToken()
                }
                return false
            }
        } catch (error) {
            console.error('[Token] 续签时发生错误:', error)
            return false
        }
    }

    /**
     * 使用userToken重新生成ws_token
     */
    this.regenerateTokenWithUserToken = async () => {
        try {
            console.log('[Token] 通过cookie会话重新生成wsToken...')
            const baseURL = constant.baseURL || `${location.protocol}//${location.host}/api`
            const response = await fetch(`${baseURL}/im/getWsToken`, {
                method: 'GET',
                credentials: 'include',
            })
            const result = await response.json()

            if (result.code === 200 && result.data) {
                const newToken = result.data
                console.log('[Token] 重新生成成功')
                this.updateToken(newToken)

                ElMessage({
                    message: '会话已自动续期',
                    type: 'success',
                    duration: 2000,
                })

                this.reconnect()
                return true
            } else {
                console.error('[Token] 重新生成失败:', result.message)
                ElMessage({
                    message: '会话已过期，请刷新页面重新登录',
                    type: 'warning',
                    duration: 5000,
                })
                return false
            }
        } catch (error) {
            console.error('[Token] 重新生成时发生错误:', error)
            ElMessage({
                message: '网络错误，请检查网络连接后重试',
                type: 'error',
                duration: 3000,
            })
            return false
        }
    }

    // ==================== WebSocket连接管理 ====================

    /**
     * 初始化WebSocket连接
     * 每次连接前都会获取新的wsToken以避免过期问题
     */
    this.initWs = async () => {
        if (this.tio) {
            console.log('[WebSocket] 关闭旧连接')
            try {
                this.tio.close()
            } catch (e) {
                console.warn('[WebSocket] 关闭旧连接失败:', e)
            }
            this.tio = null
        }

        // 获取新的wsToken（wsToken只有30分钟有效期，每次连接都刷新）
        // 通过cookie会话获取wsToken，不再依赖localStorage中的userToken

        try {
            console.log('[WebSocket] 正在获取新的wsToken...')
            const baseURL = constant.baseURL || `${location.protocol}//${location.host}/api`
            const response = await fetch(`${baseURL}/im/getWsToken`, {
                method: 'GET',
                credentials: 'include',
            })
            const result = await response.json()

            if (result.code === 200 && result.data) {
                this.currentToken = result.data
                this.paramStr = 'token=' + encodeURIComponent(this.currentToken)
                console.log('[WebSocket] 获取新wsToken成功')
            } else {
                console.error('[WebSocket] 获取wsToken失败:', result.message)
                ElMessage({
                    message: '会话已过期，请重新登录',
                    type: 'warning',
                    duration: 5000,
                })
                return
            }
        } catch (error) {
            console.error('[WebSocket] 获取wsToken时发生错误:', error)
            ElMessage({
                message: '网络错误，无法连接聊天服务',
                type: 'error',
                duration: 3000,
            })
            return
        }

        this.stateMachine.transition(WsState.CONNECTING, '初始化连接')

        console.log('[WebSocket] 开始连接')
        this.tio = new Tiows(
            this.ws_protocol,
            this.ip,
            this.port,
            this.paramStr,
            'blob'
        )
        this.tio.connect()

        this.tio.onopen = () => {
            console.log('[WebSocket] 连接成功')
            this.stateMachine.transition(WsState.CONNECTED, '连接成功')
            this.reconnectManager.cancel()
            this.startTokenRenewalCheck()
            this.startHeartbeat()

            if (!this.eventListeners.visibilityChange) {
                this.setupPageVisibilityListener()
            }

            this.timerManager.setTimeout(
                TimerNames.CONNECTION_STABLE,
                () => {
                    if (this.stateMachine.is(WsState.CONNECTED)) {
                        console.log('[WebSocket] 连接已稳定，重置重连计数')
                        this.stateMachine.resetReconnectAttempts()
                    }
                },
                5000
            )
        }

        this.tio.onclose = (event) => {
            // 如果连接已手动关闭/销毁，则忽略
            if (this.stateMachine.is(WsState.CLOSED)) {
                console.log('[WebSocket] 连接已手动关闭/销毁，忽略onclose事件')
                return
            }

            console.log('[WebSocket] 连接关闭, code:', event.code)
            this.stopTokenRenewalCheck()
            this.stopHeartbeat()
            this.timerManager.clear(TimerNames.CONNECTION_STABLE)

            const connectionDuration = this.stateMachine.getConnectionDuration()
            console.log(`[WebSocket] 连接持续时间: ${connectionDuration}ms`)
            this.stateMachine.transition(
                WsState.DISCONNECTED,
                `关闭码: ${event.code}`
            )

            const shouldReconnect = this.reconnectStrategy.shouldReconnect(
                this.stateMachine.getReconnectAttempts() + 1,
                {
                    isPageHidden: !this.isPageVisible,
                    isOffline: !navigator.onLine,
                    closeCode: event.code,
                    connectionDuration,
                }
            )

            if (shouldReconnect) {
                this.handleReconnect(connectionDuration)
            } else if (
                this.stateMachine.getReconnectAttempts() >=
                this.reconnectStrategy.maxAttempts
            ) {
                ElMessage({
                    message: '连接失败次数过多，请检查网络后刷新页面重试',
                    type: 'error',
                    duration: 5000,
                })
            }
        }

        this.tio.onerror = (event) => {
            console.error('[WebSocket] 连接错误:', event)
            ElMessage({
                message: '连接出现错误，请检查网络！',
                type: 'error',
                duration: 3000,
            })
        }
    }

    /**
     * 处理重连逻辑
     */
    this.handleReconnect = (connectionDuration) => {
        const attemptCount = this.stateMachine.getReconnectAttempts() + 1
        this.stateMachine.transition(WsState.RECONNECTING, `第${attemptCount}次重连`)

        if (attemptCount <= 3) {
            ElMessage({
                message: `连接已断开，正在尝试重连(${attemptCount}/${this.reconnectStrategy.maxAttempts})...`,
                type: 'warning',
                duration: 3000,
            })
        }

        this.reconnectManager.onReconnect(() => {
            if (!this.isPageVisible) {
                console.log('[重连] 页面不可见，取消重连')
                this.stateMachine.transition(WsState.DISCONNECTED, '页面不可见')
                return
            }

            if (!navigator.onLine) {
                console.log('[重连] 网络离线，取消重连')
                this.stateMachine.transition(WsState.DISCONNECTED, '网络离线')
                return
            }

            this.reconnect()
        })

        this.reconnectManager.scheduleReconnect(attemptCount, {
            connectionDuration,
            isPageHidden: !this.isPageVisible,
            isOffline: !navigator.onLine,
            onKicked: () => {
                this.handleKickedByDuplicate()
            },
        })
    }

    /**
     * 处理被重复登录踢出
     */
    this.handleKickedByDuplicate = () => {
        console.warn('[WebSocket] 检测到重复登录')
        this.stateMachine.transition(WsState.CLOSED, '重复登录')
        this.reconnectManager.cancel()

        ElMessage({
            message:
                '检测到您可能在其他标签页或浏览器登录了聊天室，当前页面已断开连接。如需继续使用，请关闭其他页面或刷新此页面。',
            type: 'warning',
            duration: 0,
            showClose: true,
            customClass: 'duplicate-connection-warning',
        })
    }

    /**
     * 重新连接
     */
    this.reconnect = () => {
        console.log('[WebSocket] 执行重连')

        if (this.tio && this.tio.isReady()) {
            console.log('[WebSocket] 已连接，无需重连')
            return
        }

        if (this.stateMachine.is(WsState.CLOSED)) {
            console.log('[WebSocket] 处于CLOSED状态，不允许重连')
            return
        }

        try {
            this.initWs()
        } catch (error) {
            console.error('[WebSocket] 重连失败:', error)
            this.stateMachine.transition(WsState.DISCONNECTED, '重连失败')
        }
    }

    /**
     * 发送消息
     */
    this.sendMsg = (value) => {
        console.log('[WebSocket] 准备发送消息')

        if (this.stateMachine.is(WsState.CLOSED)) {
            ElMessage({
                message:
                    '您在其他地方登录了聊天室，当前页面已断开连接。请关闭其他页面或刷新此页面。',
                type: 'warning',
                duration: 0,
                showClose: true,
            })
            return false
        }

        if (!this.tio) {
            ElMessage({
                message: 'WebSocket未初始化，请刷新页面重试！',
                type: 'error',
                duration: 4000,
            })
            return false
        }

        if (!this.tio.isReady()) {
            const readyState = this.tio.getReadyState()
            let message = '连接异常，消息发送失败！'

            switch (readyState) {
                case WebSocket.CONNECTING:
                    message = '正在连接中，请稍后重试！'
                    break
                case WebSocket.CLOSING:
                    message = '连接正在关闭，请稍后重试！'
                    break
                case WebSocket.CLOSED:
                    message = '连接已断开，正在尝试重新连接...'
                    this.reconnect()
                    break
            }

            ElMessage({
                message: message,
                type: 'error',
                duration: 3000,
            })
            return false
        }

        try {
            const success = this.tio.send(value)
            if (success) {
                console.log('[WebSocket] 消息发送成功')
                return true
            } else {
                console.error('[WebSocket] 消息发送失败')
                ElMessage({
                    message: '消息发送失败，请检查网络连接！',
                    type: 'error',
                    duration: 3000,
                })
                return false
            }
        } catch (error) {
            console.error('[WebSocket] 发送消息异常:', error)
            ElMessage({
                message: '发送消息时出现异常：' + (error.message || '未知错误'),
                type: 'error',
                duration: 4000,
            })
            return false
        }
    }

    // ==================== 心跳检测 ====================

    this.startHeartbeat = () => {
        this.timerManager.clear(TimerNames.HEARTBEAT_WS)
        this.timerManager.clear(TimerNames.HEARTBEAT_HTTP)
        this.lastHeartbeatResponse = Date.now()

        this.timerManager.setInterval(
            TimerNames.HEARTBEAT_WS,
            () => {
                if (this.isPageVisible) {
                    this.sendWebSocketHeartbeat()
                }
            },
            60000
        )

        this.timerManager.setInterval(
            TimerNames.HEARTBEAT_HTTP,
            () => {
                if (this.isPageVisible) {
                    this.sendHeartbeat()
                }
            },
            3 * 60 * 1000
        )
    }

    this.stopHeartbeat = () => {
        this.timerManager.clear(TimerNames.HEARTBEAT_WS)
        this.timerManager.clear(TimerNames.HEARTBEAT_HTTP)
    }

    this.sendWebSocketHeartbeat = () => {
        if (!this.tio || !this.tio.isReady()) {
            console.warn('[心跳] WebSocket连接不可用')
            return
        }

        let currentUserId = 0
        try {
            if (this.imStore && this.imStore.currentUser) {
                currentUserId = this.imStore.currentUser.id
            }
        } catch (e) {
            console.warn('[心跳] 获取用户ID失败')
        }

        const heartbeatMsg = JSON.stringify({
            messageType: 0,
            content: 'heartbeat',
            fromId: currentUserId,
            timestamp: Date.now(),
        })

        const success = this.tio.send(heartbeatMsg)

        if (success) {
            console.log('[心跳] WebSocket心跳发送成功')

            const now = Date.now()
            if (now - this.lastHeartbeatResponse > 600000) {
                console.warn('[心跳] 长时间没有收到响应，可能连接异常')
                if (
                    this.isPageVisible &&
                    !this.stateMachine.is(WsState.RECONNECTING)
                ) {
                    this.reconnect()
                }
            }
        } else {
            console.error('[心跳] WebSocket心跳发送失败')
        }
    }

    this.sendHeartbeat = async () => {
        if (!this.currentToken) {
            console.warn('[心跳] 没有可用的token')
            return
        }

        try {
            const baseURL = constant.baseURL || `${location.protocol}//${location.host}/api`
            const response = await fetch(
                `${baseURL}/im/heartbeat?wsToken=${this.currentToken}`
            )
            const result = await response.json()

            if (result.code === 200 && result.data) {
                const returnedToken = result.data

                if (returnedToken !== this.currentToken) {
                    console.log('[心跳] 服务器自动续签了token')
                    this.updateToken(returnedToken)
                }

                console.log('[心跳] HTTP心跳成功')
                this.updateHeartbeatResponse()
            } else {
                console.warn('[心跳] HTTP心跳失败:', result.message)
            }
        } catch (error) {
            console.error('[心跳] HTTP心跳错误:', error)
        }
    }

    this.updateHeartbeatResponse = () => {
        this.lastHeartbeatResponse = Date.now()
    }

    this.onMessageReceived = () => {
        this.updateHeartbeatResponse()
    }

    // ==================== Token续签检查 ====================

    this.startTokenRenewalCheck = () => {
        this.timerManager.clear(TimerNames.TOKEN_RENEWAL)

        this.timerManager.setInterval(
            TimerNames.TOKEN_RENEWAL,
            () => {
                this.checkAndRenewToken()
            },
            5 * 60 * 1000
        )

        this.timerManager.setTimeout(
            'tokenRenewalFirst',
            () => {
                this.checkAndRenewToken()
            },
            5000
        )
    }

    this.stopTokenRenewalCheck = () => {
        this.timerManager.clear(TimerNames.TOKEN_RENEWAL)
        this.timerManager.clear('tokenRenewalFirst')
    }

    // ==================== 页面可见性监听 ====================

    this.setupPageVisibilityListener = () => {
        this.eventListeners.visibilityChange = () => {
            const wasVisible = this.isPageVisible
            this.isPageVisible = !document.hidden
            console.log(
                '[页面] 可见性变化:',
                this.isPageVisible ? '可见' : '隐藏'
            )

            if (this.isPageVisible && !wasVisible) {
                setTimeout(() => {
                    if (this.isPageVisible && (!this.tio || !this.tio.isReady())) {
                        console.log('[页面] 恢复可见时发现连接异常，尝试重连')
                        const attempts = this.stateMachine.getReconnectAttempts()
                        if (attempts > 2) {
                            this.stateMachine.metadata.reconnectAttempts = Math.max(
                                0,
                                attempts - 2
                            )
                        }
                        this.reconnect()
                    } else if (this.tio && this.tio.isReady()) {
                        this.updateHeartbeatResponse()
                        this.sendWebSocketHeartbeat()
                    }
                }, 2000)
            } else if (!this.isPageVisible && wasVisible) {
                console.log('[页面] 变为隐藏，取消重连')
                this.reconnectManager.cancel()
            }
        }

        this.eventListeners.windowFocus = () => {
            if (
                this.isPageVisible &&
                (!this.tio || !this.tio.isReady()) &&
                !this.stateMachine.is(WsState.RECONNECTING)
            ) {
                console.log('[页面] 获得焦点时发现连接异常，尝试重连')
                this.stateMachine.resetReconnectAttempts()
                this.reconnect()
            }
        }

        this.eventListeners.windowOnline = () => {
            console.log('[网络] 已恢复')
            if (
                this.isPageVisible &&
                (!this.tio || !this.tio.isReady()) &&
                !this.stateMachine.is(WsState.RECONNECTING)
            ) {
                this.stateMachine.resetReconnectAttempts()
                this.reconnect()
            }
        }

        this.eventListeners.windowOffline = () => {
            console.log('[网络] 已断开')
            this.reconnectManager.cancel()
            ElMessage({
                message: '网络连接已断开',
                type: 'warning',
                duration: 3000,
            })
        }

        document.addEventListener(
            'visibilitychange',
            this.eventListeners.visibilityChange
        )
        window.addEventListener('focus', this.eventListeners.windowFocus)
        window.addEventListener('online', this.eventListeners.windowOnline)
        window.addEventListener('offline', this.eventListeners.windowOffline)
    }

    // ==================== 清理方法 ====================

    this.cleanup = () => {
        console.log('[清理] 开始清理资源...')
        // 1. 立即切换到CLOSED状态，防止后续操作
        this.stateMachine.transition(WsState.CLOSED, '实例销毁')

        this.timerManager.clearAll()
        this.reconnectManager.cancel()

        if (this.eventListeners.visibilityChange) {
            document.removeEventListener(
                'visibilitychange',
                this.eventListeners.visibilityChange
            )
        }
        if (this.eventListeners.windowFocus) {
            window.removeEventListener('focus', this.eventListeners.windowFocus)
        }
        if (this.eventListeners.windowOnline) {
            window.removeEventListener('online', this.eventListeners.windowOnline)
        }
        if (this.eventListeners.windowOffline) {
            window.removeEventListener('offline', this.eventListeners.windowOffline)
        }

        if (this.tio) {
            try {
                this.tio.close()
            } catch (e) {
                console.warn('[清理] 关闭WebSocket失败:', e)
            }
            this.tio = null
        }

        // 不再调用 reset()，因为它会把状态设为 DISCONNECTED
        // 只需要清理监听器
        this.stateMachine.listeners.clear()

        console.log('[清理] 资源清理完成')
    }

    this.destroy = () => {
        this.cleanup()
        currentImInstance = null
    }
}
