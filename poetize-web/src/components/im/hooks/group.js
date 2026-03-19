import { useMainStore } from '../../../stores/main'; import { useImStore } from '../../../stores/im';
import { message as ElMessage } from '@/utils/element-command-services';
import { reactive, getCurrentInstance } from 'vue';
export default function () {
  const globalProperties = getCurrentInstance().appContext.config.globalProperties;
  const $http = globalProperties.$http;
  const $constant = globalProperties.$constant;
  const mainStore = useMainStore(); const imStore = useImStore(); const store = { state: { get currentUser() { return mainStore.currentUser; }, get sysConfig() { return mainStore.sysConfig; } }, commit(m, p) { if (m === 'loadCurrentUser') mainStore.loadCurrentUser(p); else if (m === 'loadSysConfig') mainStore.loadSysConfig(p); } };

  let groupData = reactive({
    //群组列表
    groups: {},
    //当前群信息
    currentGroupId: null
  })
  function exitGroup(currentGroupId) {
    $http.get($constant.baseURL + "/imChatGroupUser/quitGroup", { id: currentGroupId })
      .then((res) => {
        // 删除群组信息
        delete groupData.groups[currentGroupId];
        groupData.currentGroupId = null;
        // 🔥 关键：同步清理localStorage中的群聊相关数据
        removeGroupFromLocalStorage(currentGroupId);
        ElMessage({
          message: "退群成功！",
          type: 'success'
        });
      })
      .catch((error) => {
        ElMessage({
          message: error.message,
          type: 'error'
        });
      });
  }
  function dissolveGroup(currentGroupId) {
    $http.get($constant.baseURL + "/imChatGroup/deleteGroup", { id: currentGroupId })
      .then((res) => {
        // 删除群组信息
        delete groupData.groups[currentGroupId];
        groupData.currentGroupId = null;
        // 🔥 关键：同步清理localStorage中的群聊相关数据
        removeGroupFromLocalStorage(currentGroupId);
        ElMessage({
          message: "解散群成功！",
          type: 'success'
        });
      })
      .catch((error) => {
        ElMessage({
          message: error.message,
          type: 'error'
        });
      });
  }
  // 从前端列表中移除群聊（使用Pinia store）
  function removeGroupFromLocalStorage(groupId) {
    try {
      // 直接使用Pinia store中的groupChats
      const currentGroupChats = imStore.groupChats || [];
      const updatedGroupChats = currentGroupChats.filter(chatGroupId => chatGroupId !== groupId);
      imStore.updateGroupChats(updatedGroupChats);
      console.log(`已从列表移除群聊 ${groupId}`);
    } catch (error) {
      console.error('移除群聊失败:', error);
    }
  }
  async function getImGroup() {
    try {
      const res = await $http.get($constant.baseURL + "/imChatGroup/listGroup");
      if (res && res.code === 200 && res.data) {
        groupData.groups = {};
        if (Array.isArray(res.data)) {
          res.data.forEach(group => {
            if (group && group.id) {
              // 确保每个群组都有必要的属性
              groupData.groups[group.id] = {
                id: group.id,
                groupName: group.groupName || '未知群聊',
                avatar: group.avatar || '',
                groupType: group.groupType || 1,
                masterFlag: group.masterFlag || false,
                adminFlag: group.adminFlag || false,
                ...group
              };
            }
          });
        }
        console.log('群组数据加载成功:', Object.keys(groupData.groups).length, '个群组');
        return true;
      } else {
        console.error('获取群组数据失败:', res);
        return false;
      }
    } catch (error) {
      console.error('获取群组数据时发生错误:', error);
      ElMessage({
        message: error.message || '获取群组数据失败',
        type: 'error'
      });
      return false;
    }
  }
  function addGroupTopic() {
    $http.get($constant.baseURL + "/imChatGroup/addGroupTopic", { id: groupData.currentGroupId })
      .then((res) => {
      })
      .catch((error) => {
        ElMessage({
          message: error.message,
          type: 'error'
        });
      });
  }
  // 手动删除群聊列表（直接执行，确认逻辑由调用方处理）
  function removeGroupFromList(groupId) {
    // 调用后端接口隐藏群聊（@PostMapping + @RequestParam 需要在URL中传参）
    $http.post($constant.baseURL + "/imChatGroup/hideGroupChat?groupId=" + groupId)
      .then(() => {
        // 从本地列表移除
        removeGroupFromLocalStorage(groupId);
        ElMessage({
          message: "已从聊天列表中删除！",
          type: 'success'
        });
      })
      .catch((error) => {
        ElMessage({
          message: error.message || "删除失败",
          type: 'error'
        });
      });
  }
  return {
    groupData,
    getImGroup,
    addGroupTopic,
    exitGroup,
    dissolveGroup,
    removeGroupFromList,
    removeGroupFromLocalStorage
  }
}
