import { useMainStore } from '../../../stores/main'; import { useImStore } from '../../../stores/im';
import { ElMessageBox } from 'element-plus';
import { nextTick } from 'vue';
import { ElMessage } from "element-plus";
import { reactive, getCurrentInstance, onMounted, onBeforeUnmount, watchEffect, toRefs } from 'vue';
export default function () {
  const globalProperties = getCurrentInstance().appContext.config.globalProperties;
  const $common = globalProperties.$common;
  const $http = globalProperties.$http;
  const $constant = globalProperties.$constant;
  const mainStore = useMainStore(); const imStore = useImStore(); const store = { state: { get currentUser() { return mainStore.currentUser; }, get sysConfig() { return mainStore.sysConfig; } }, commit(m, p) { if (m === 'loadCurrentUser') mainStore.loadCurrentUser(p); else if (m === 'loadSysConfig') mainStore.loadSysConfig(p); } };

  let friendData = reactive({
    //好友请求
    friendRequests: [],
    //好友列表
    friends: {},
    //当前朋友信息
    currentFriendId: null
  })
  async function getImFriend() {
    await $http.get($constant.baseURL + "/imChatUserFriend/getFriend", { friendStatus: 1 })
      .then((res) => {
        if (!$common.isEmpty(res.data)) {
          res.data.forEach(friend => {
            friendData.friends[friend.friendId] = friend;
          });
        }
      })
      .catch((error) => {
        ElMessage({
          message: error.message,
          type: 'error'
        });
      });
  }
  function removeFriend(currentFriendId) {
    ElMessageBox.confirm('你确定删除' + friendData.friends[currentFriendId].remark + '?', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      $http.get($constant.baseURL + "/imChatUserFriend/changeFriend", {
        friendId: currentFriendId,
        friendStatus: -1
      })
        .then((res) => {
          delete friendData.friends[currentFriendId];
          friendData.currentFriendId = null;
          ElMessage({
            message: "删除成功！",
            type: 'success'
          });
        })
        .catch((error) => {
          ElMessage({
            message: error.message,
            type: 'error'
          });
        });
    }).catch(() => {
      // 用户取消删除
    });
  }
  function getFriendRequests() {
    $http.get($constant.baseURL + "/imChatUserFriend/getFriend", { friendStatus: 0 })
      .then((res) => {
        if (!$common.isEmpty(res.data)) {
          friendData.friendRequests = res.data;
          ElMessage({
            message: "您有好友申请待处理！",
            showClose: true,
            type: 'success',
            duration: 0
          });
        }
      })
      .catch((error) => {
        ElMessage({
          message: error.message,
          type: 'error'
        });
      });
  }
  function changeFriendStatus(friendId, status, index) {
    $http.get($constant.baseURL + "/imChatUserFriend/changeFriend", { friendId: friendId, friendStatus: status })
      .then((res) => {
        friendData.friendRequests.splice(index, 1);
        ElMessage({
          message: "修改成功！",
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
  return {
    friendData,
    getImFriend,
    removeFriend,
    getFriendRequests,
    changeFriendStatus
  }
}
