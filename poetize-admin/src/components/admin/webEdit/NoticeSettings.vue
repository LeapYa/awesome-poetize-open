<template>
  <div id="field-notice">
    <SectionTag>公告</SectionTag>
    <el-tag
      :key="i"
      v-for="(notice, i) in localNotices"
      closable
      :disable-transitions="false"
      @close="handleClose(notice)">
      {{notice}}
    </el-tag>
    <el-input
      class="input-new-tag"
      v-if="inputNoticeVisible"
      v-model="inputNoticeValue"
      ref="saveNoticeInput"
      size="small"
      placeholder="请输入公告内容"
      @keyup.enter.native="handleInputNoticeConfirm"
      @blur="handleInputNoticeConfirm"
      @keydown.enter.native="handleInputNoticeConfirm">
    </el-input>
    <el-button v-else class="button-new-tag" size="small" @click="showNoticeInput()">+ 公告</el-button>
    <div class="myCenter" style="margin-bottom: 22px">
      <el-button type="primary" @click="saveNotice()" :loading="loading">保存公告</el-button>
    </div>
  </div>
</template>

<script>
import SectionTag from './SectionTag.vue';

export default {
  name: 'NoticeSettings',
  components: { SectionTag },
  props: {
    notices: { type: Array, default: () => [] },
    webInfoId: { type: [Number, String], default: null }
  },
  data() {
    return {
      localNotices: [...this.notices],
      inputNoticeVisible: false,
      inputNoticeValue: '',
      loading: false
    }
  },
  watch: {
    notices: {
      handler(val) {
        this.localNotices = [...(val || [])];
      },
      deep: true
    }
  },
  methods: {
    handleClose(notice) {
      this.localNotices.splice(this.localNotices.indexOf(notice), 1);
    },
    handleInputNoticeConfirm() {
      if (this.inputNoticeValue && this.inputNoticeValue.trim()) {
        this.localNotices.push(this.inputNoticeValue.trim());
      }
      this.inputNoticeVisible = false;
      this.inputNoticeValue = '';
    },
    showNoticeInput() {
      this.inputNoticeVisible = true;
      this.$nextTick(() => {
        this.$refs.saveNoticeInput.$refs.input.focus();
      });
    },
    saveNotice() {
      if (!this.webInfoId) {
        this.$message.error('网站信息ID不存在，请刷新页面重试');
        return;
      }
      if (!Array.isArray(this.localNotices)) {
        this.localNotices = [];
      }
      this.localNotices = this.localNotices.filter(notice => notice && notice.trim());
      this.loading = true;
      this.$http.post(this.$constant.baseURL + "/webInfo/updateNotices", {
        id: this.webInfoId,
        notices: JSON.stringify(this.localNotices)
      }, true)
        .then((res) => {
          if (res.code === 200) {
            this.$emit('saved');
            this.$message({ message: "公告保存成功！", type: "success" });
          } else {
            this.$message({ message: "保存失败: " + res.message, type: "error" });
          }
        })
        .catch((error) => {
          console.error('保存公告失败:', error);
          this.$message({ message: "保存失败: " + (error.response?.data?.message || error.message), type: "error" });
        })
        .finally(() => {
          this.loading = false;
        });
    }
  }
}
</script>

<style scoped>
.button-new-tag {
  margin: 10px;
  height: 32px;
  line-height: 32px;
  padding-top: 0;
  padding-bottom: 0;
}
.input-new-tag {
  width: 200px;
  margin: 10px;
}
</style>
