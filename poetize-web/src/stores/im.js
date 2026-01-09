/**
 * IM 聊天室 Pinia Store
 * 从 poetize-im-ui/src/store/index.js (Vuex) 迁移而来
 */
import { defineStore } from 'pinia'
import { ref, reactive, watch } from 'vue'

// 从localStorage安全地解析JSON数据
function safeParseJSON(key, defaultValue = {}) {
    try {
        const item = localStorage.getItem(key)
        if (!item || item === 'null' || item === 'undefined') {
            return defaultValue
        }
        const parsed = JSON.parse(item)
        return parsed === null || parsed === undefined ? defaultValue : parsed
    } catch (error) {
        console.warn(`解析localStorage中的${key}失败:`, error)
        return defaultValue
    }
}

// 安全地保存数据到localStorage
function safeSaveToStorage(key, data) {
    try {
        localStorage.setItem(key, JSON.stringify(data))
    } catch (error) {
        console.error(`保存${key}到localStorage失败:`, error)
        if (error.name === 'QuotaExceededError') {
            console.warn('localStorage空间不足，清理旧数据...')
            cleanOldChatData()
            try {
                localStorage.setItem(key, JSON.stringify(data))
            } catch (retryError) {
                console.error(`重试保存${key}失败:`, retryError)
            }
        }
    }
}

// 清理旧的聊天数据
function cleanOldChatData() {
    const keysToClean = ['imMessages', 'groupMessages']
    keysToClean.forEach((key) => {
        try {
            const data = safeParseJSON(key, {})
            const cleanedData = {}
            Object.keys(data).forEach((chatId) => {
                if (Array.isArray(data[chatId]) && data[chatId].length > 0) {
                    cleanedData[chatId] = data[chatId].slice(-50)
                }
            })
            localStorage.setItem(key, JSON.stringify(cleanedData))
        } catch (error) {
            console.error(`清理${key}失败:`, error)
        }
    })

        // 清理已废弃的聊天列表和未读数缓存
        ;['imChats', 'groupChats', 'imMessageBadge', 'groupMessageBadge'].forEach(
            (key) => {
                localStorage.removeItem(key)
            }
        )
}

export const useImStore = defineStore('im', () => {
    // ========== State ==========
    const currentUser = ref(safeParseJSON('currentUser', {}))
    const sysConfig = ref(safeParseJSON('sysConfig', {}))
    const onlineUserCount = reactive({}) // {groupId: count}

    // 聊天相关数据
    const imChats = ref([]) // 私聊列表（从后端同步）
    const groupChats = ref([]) // 群聊列表（从后端同步）
    const imMessages = reactive(safeParseJSON('imMessages', {})) // 私聊消息
    const groupMessages = reactive(safeParseJSON('groupMessages', {})) // 群聊消息
    const imMessageBadge = reactive({}) // 私聊未读数
    const groupMessageBadge = reactive({}) // 群聊未读数
    const systemMessageBadge = ref(safeParseJSON('systemMessageBadge', 0)) // 系统消息未读数
    const lastMessagePreviews = reactive(safeParseJSON('lastMessagePreviews', {})) // 最后一条消息预览

    // ========== Actions ==========

    function loadCurrentUser(user) {
        currentUser.value = user
        safeSaveToStorage('currentUser', user)
    }

    function loadSysConfig(config) {
        sysConfig.value = config
        safeSaveToStorage('sysConfig', config)
    }

    function updateOnlineUserCount(groupId, count) {
        onlineUserCount[groupId] = count
    }

    // 聊天列表相关
    function updateImChats(chats) {
        imChats.value = [...chats]
    }

    function updateGroupChats(chats) {
        groupChats.value = [...chats]
    }

    // 消息相关
    function updateImMessages(friendId, messages) {
        imMessages[friendId] = [...messages]
        safeSaveToStorage('imMessages', imMessages)
    }

    function updateGroupMessages(groupId, messages) {
        groupMessages[groupId] = [...messages]
        safeSaveToStorage('groupMessages', groupMessages)
    }

    function addImMessage(friendId, message) {
        if (!imMessages[friendId]) {
            imMessages[friendId] = []
        }
        imMessages[friendId].push(message)
        safeSaveToStorage('imMessages', imMessages)
    }

    function addGroupMessage(groupId, message) {
        if (!groupMessages[groupId]) {
            groupMessages[groupId] = []
        }
        groupMessages[groupId].push(message)
        safeSaveToStorage('groupMessages', groupMessages)
    }

    // 未读消息数相关
    function updateImMessageBadge(friendId, count) {
        imMessageBadge[friendId] = count
    }

    function updateGroupMessageBadge(groupId, count) {
        groupMessageBadge[groupId] = count
    }

    function updateSystemMessageBadge(count) {
        systemMessageBadge.value = count
        safeSaveToStorage('systemMessageBadge', systemMessageBadge.value)
    }

    function resetSystemMessageBadge() {
        systemMessageBadge.value = 0
        safeSaveToStorage('systemMessageBadge', systemMessageBadge.value)
    }

    // 最后消息预览
    function updateLastMessagePreview(chatId, preview) {
        lastMessagePreviews[chatId] = preview
        safeSaveToStorage('lastMessagePreviews', lastMessagePreviews)
    }

    // 清空所有聊天数据
    function clearAllChatData() {
        imChats.value = []
        groupChats.value = []
        Object.keys(imMessages).forEach((key) => delete imMessages[key])
        Object.keys(groupMessages).forEach((key) => delete groupMessages[key])
        Object.keys(imMessageBadge).forEach((key) => delete imMessageBadge[key])
        Object.keys(groupMessageBadge).forEach(
            (key) => delete groupMessageBadge[key]
        )
        Object.keys(lastMessagePreviews).forEach(
            (key) => delete lastMessagePreviews[key]
        )
        systemMessageBadge.value = 0

        const keysToRemove = ['imMessages', 'groupMessages', 'lastMessagePreviews', 'systemMessageBadge']
        keysToRemove.forEach((key) => {
            localStorage.removeItem(key)
        })
    }

    // 批量更新聊天数据
    function updateChatData({ imMessages: imMsgs, groupMessages: groupMsgs }) {
        if (imMsgs !== undefined) {
            Object.keys(imMsgs).forEach((friendId) => {
                updateImMessages(friendId, imMsgs[friendId])
            })
        }
        if (groupMsgs !== undefined) {
            Object.keys(groupMsgs).forEach((groupId) => {
                updateGroupMessages(groupId, groupMsgs[groupId])
            })
        }
    }

    return {
        // State
        currentUser,
        sysConfig,
        onlineUserCount,
        imChats,
        groupChats,
        imMessages,
        groupMessages,
        imMessageBadge,
        groupMessageBadge,
        systemMessageBadge,
        lastMessagePreviews,

        // Actions
        loadCurrentUser,
        loadSysConfig,
        updateOnlineUserCount,
        updateImChats,
        updateGroupChats,
        updateImMessages,
        updateGroupMessages,
        addImMessage,
        addGroupMessage,
        updateImMessageBadge,
        updateGroupMessageBadge,
        updateSystemMessageBadge,
        resetSystemMessageBadge,
        updateLastMessagePreview,
        clearAllChatData,
        updateChatData,
    }
})
