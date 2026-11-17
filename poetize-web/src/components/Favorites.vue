<template>
  <div>
    <div class="favorites-container">
      <!-- 封面 -->
      <div class="favorites-header my-animation-slide-top">
        <!-- 背景视频（含慢网骨架与淡入） -->
        <indexVideo></indexVideo>
        <div class="favorites-header-content">
          <!-- 标题 -->
          <div style="color: var(--white); margin: 0 10px">
            <div style="font-size: 30px; font-weight: bold; line-height: 2">
              收藏夹
            </div>
            <div style="font-size: 16px; opacity: 0.8; margin-top: 10px">
              将本网站添加到您的收藏夹吧，发现更多精彩内容
            </div>
          </div>
        </div>
      </div>

      <!-- 内容 -->
      <div class="favorites-content">
        <div v-if="loading" class="empty-state" role="status" aria-live="polite">
          <div class="empty-text">收藏内容加载中...</div>
        </div>

        <div v-else-if="loadError" class="empty-state" role="alert">
          <div class="empty-text">收藏内容加载失败</div>
          <div class="empty-desc">{{ loadError }}</div>
          <button class="retry-button" type="button" @click="getCollect">重试</button>
        </div>

        <!-- 收藏夹 -->
        <div v-else-if="!$common.isEmpty(collects)" class="my-animation-hideToShow">
          <div
            v-for="(value, key) in collects"
            :key="key"
            style="margin-top: 20px"
          >
            <div class="collect-classify">
              {{ key }}
            </div>
            <div class="favorite-item-wrap">
              <div
                v-for="(item, index) in value"
                :key="index"
                @click="toUrl(item.url)"
                class="favorite-item"
              >
                <div>
                  <el-avatar
                    class="favorite-item-image"
                    :size="60"
                    :src="item.cover"
                  >
                  </el-avatar>
                </div>
                <div style="width: calc(100% - 80px)">
                  <div class="favorite-item-title">
                    {{ item.title }}
                  </div>
                  <div class="favorite-item-introduction">
                    {{ item.introduction }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 暂无收藏提示 -->
        <div v-else class="empty-state">
          <div class="empty-icon">📂</div>
          <div class="empty-text">暂无收藏内容</div>
          <div class="empty-desc">管理员还没有添加收藏内容哦~</div>
        </div>
      </div>
    </div>

    <!-- 页脚 -->
    <div style="background: var(--favoriteBg)">
      <myFooter></myFooter>
    </div>
  </div>
</template>

<script>
import { defineAsyncComponent } from 'vue'
import { useMainStore } from '@/stores/main'
import indexVideo from './common/indexVideo'


export default {
  components: {
    myFooter: defineAsyncComponent(() => import('./common/myFooter')),
    indexVideo,
  },

  data() {
    return {
      collects: {},
      loading: true,
      loadError: '',
    }
  },

  computed: {
    mainStore() {
      return useMainStore()
    },
  },

  watch: {},

  created() {
    // 设置页面标题（注：setTitle 方法已移除，页面标题由路由或组件自行管理）
    // document.title = "收藏夹 | " + this.mainStore.sysConfig.webName;
    this.getCollect()
  },

  mounted() {},

  methods: {
    toUrl(url) {
      const safeUrl = this.$common.safeExternalUrl(url)
      if (safeUrl) {
        window.open(safeUrl, '_blank', 'noopener,noreferrer')
      }
    },
    getCollect() {
      this.loading = true
      this.loadError = ''
      return this.$http
        .get(this.$constant.baseURL + '/webInfo/listCollect')
        .then((res) => {
          this.collects = res.data || {}
        })
        .catch((error) => {
          this.loadError = error.message || '请检查网络后重试'
          this.$message({
            message: this.loadError,
            type: 'error',
          })
        })
        .finally(() => {
          this.loading = false
        })
    },
  },
}
</script>

<style scoped>
.favorites-container {
  padding: 25px;
  background: var(--favoriteBg);
}
.favorites-header {
  margin: 60px auto 30px;
  height: 200px;
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  max-width: 1200px;
}
.favorites-header-content {
  position: absolute;
  left: 20px;
  top: 25px;
  padding: 5px 20px;
  margin: 10px;
}
.favorites-content {
  margin: 0 auto;
  max-width: 1200px;
  padding: 40px 0;
}
.collect-classify {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 10px;
}
.favorite-item-wrap {
  display: flex;
  flex-wrap: wrap;
  margin-left: -10px;
}
.favorite-item {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  border-radius: 12px;
  box-shadow: 0 8px 16px -4px #2c2d300c;
  transform: translateZ(0);
  background: var(--background);
  display: flex;
  width: calc(100% / 4 - 20px);
  max-width: 320px;
  height: 90px;
  overflow: hidden;
  padding: 15px;
  cursor: pointer;
  margin: 10px;
}
.favorite-item:hover {
  background: #425aef;
  color: var(--white);
}
.favorite-item:hover .favorite-item-image {
  transition: width 0.6s ease, height 0.6s ease, opacity 0.6s ease;
  width: 0 !important;
  height: 0 !important;
  opacity: 0;
  margin-right: 0;
}
.favorite-item:hover div:nth-child(2) {
  width: 100% !important;
}
.favorite-item-image {
  margin-right: 20px;
  transition: width 0.3s ease, height 0.3s ease, opacity 0.3s ease;
  will-change: width, height, opacity;
}
.favorite-item-title {
  font-size: 19px;
  font-weight: bold;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  margin-bottom: 5px;
}
.favorite-item-introduction {
  opacity: 0.7;
  font-weight: bold;
  letter-spacing: 1px;
  font-size: 14px;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--greyFont);
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
}
.empty-text {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
}
.empty-desc {
  font-size: 14px;
  opacity: 0.7;
}
.retry-button {
  margin-top: 16px;
  padding: 8px 18px;
  color: var(--white);
  background: var(--lightGreen);
  border: 0;
  border-radius: 4px;
  cursor: pointer;
}
.retry-button:hover {
  filter: brightness(0.95);
}
@media screen and (max-width: 906px) {
  .favorites-header {
    height: 180px;
  }
  .favorite-item {
    width: calc(100% / 3 - 20px);
  }
  .favorites-header-content {
    margin: 0;
  }
}
@media screen and (max-width: 636px) {
  .favorites-header {
    height: 160px;
  }
  .favorite-item {
    width: calc(100% / 2 - 20px);
  }
  .favorites-header-content {
    margin: 0;
  }
}
@media screen and (max-width: 400px) {
  .favorite-item {
    width: calc(100% - 20px);
  }
}
</style>
