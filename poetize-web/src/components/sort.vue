<template>
  <div>
    <!-- 分类头部 -->
    <div class="my-animation-slide-top">
      <!-- 有具体分类时显示分类信息 -->
      <div
        class="poem-container myCenter my-animation-hideToShow"
        v-if="!$common.isEmpty(sort)"
      >
        <!-- 背景图片 -->
        <el-image
          class="my-el-image poem-image"
          style="position: absolute; margin-top: -50px"
          v-once
          lazy
          :src="
            mainStore.webInfo.randomCover[
              Math.floor(Math.random() * mainStore.webInfo.randomCover.length)
            ]
          "
          fit="cover"
        >
          <template v-slot:error>
            <div class="image-slot"></div>
          </template>
        </el-image>
        <div class="poem-wrap">
          <div>
            <span>{{ sort.sortName }}</span>
          </div>
          <p class="poem">{{ sort.sortDescription }}</p>
        </div>
      </div>
      <!-- 没有具体分类时（/sort页面）显示固定的分类导航标题，而非随机诗词 -->
      <div
        class="poem-container myCenter my-animation-hideToShow"
        v-else
      >
        <el-image
          class="my-el-image poem-image"
          style="position: absolute; margin-top: -50px"
          v-once
          lazy
          :src="
            mainStore.webInfo.randomCover[
              Math.floor(Math.random() * mainStore.webInfo.randomCover.length)
            ]
          "
          fit="cover"
        >
          <template v-slot:error>
            <div class="image-slot"></div>
          </template>
        </el-image>
        <div class="poem-wrap">
          <div>
            <span>文章分类</span>
          </div>
          <p class="poem">探索不同主题的文章内容</p>
        </div>
      </div>
    </div>

    <div
      class="sort-content-area my-animation-slide-bottom"
    >
      <!-- 无分类ID时显示所有分类列表卡片 -->
      <div v-if="!sortId && !$common.isEmpty(mainStore.sortInfo)" class="sort-list-warp">
        <div
          v-for="(item, index) in mainStore.sortInfo"
          :key="item.id"
          class="sort-card shadow-box"
          @click="$router.push('/sort/' + item.id)"
        >
          <div class="sort-card-header">
            <h3>{{ item.sortName }}</h3>
            <span class="sort-card-count">{{ item.countOfSort || 0 }} 篇</span>
          </div>
          <p class="sort-card-desc">{{ item.sortDescription || '暂无描述' }}</p>
          <div class="sort-card-labels" v-if="item.labels && item.labels.length > 0">
            <proTag
              v-for="(label, lIdx) in item.labels.slice(0, 5)"
              :key="lIdx"
              :info="label.labelName"
              :color="$constant.before_color_list[Math.floor(Math.random() * 6)]"
              style="margin: 4px"
            />
            <span v-if="item.labels.length > 5" class="sort-card-more">+{{ item.labels.length - 5 }}</span>
          </div>
        </div>
      </div>

      <!-- 有分类ID时显示标签筛选 -->
      <div
        class="sort-warp shadow-box"
        v-if="!$common.isEmpty(sort) && !$common.isEmpty(sort.labels)"
      >
        <div
          v-for="(label, index) in sort.labels"
          :key="index"
          :class="{
            isActive:
              !$common.isEmpty(labelId) && parseInt(labelId) === label.id,
          }"
          @click="listArticle(label)"
        >
          <proTag
            :info="label.labelName + ' ' + label.countOfLabel"
            :color="$constant.before_color_list[Math.floor(Math.random() * 6)]"
            style="margin: 12px"
          >
          </proTag>
        </div>
      </div>

      <!-- 文章（仅在有具体分类时显示） -->
      <div class="article-wrap" v-if="sortId">
        <articleList :articleList="articles"></articleList>
        <div class="pagination-wrap">
          <div
            @click="pageArticles()"
            class="pagination"
            v-if="articles.length < pagination.total"
          >
            下一页
          </div>
          <div v-else style="user-select: none">~~到底啦~~</div>
        </div>
      </div>
      <!-- 页脚 -->
      <myFooter></myFooter>
    </div>
  </div>
</template>

<script>
import { defineAsyncComponent } from 'vue'
import { useMainStore } from '@/stores/main'


export default {
  components: {
    proTag: defineAsyncComponent(() => import('./common/proTag')),
    articleList: defineAsyncComponent(() => import('./articleList')),
    myFooter: defineAsyncComponent(() => import('./common/myFooter')),
  },

  data() {
    return {
      sortId: this.$route.params.id || null,
      labelId: this.$route.query.labelId,
      sort: null,
      pagination: {
        current: 1,
        size: 10,
        total: 0,
        searchKey: '',
        sortId: this.$route.params.id || null,
        labelId: this.$route.query.labelId,
      },
      articles: [],
    }
  },

  computed: {
    mainStore() {
      return useMainStore()
    },
  },

  watch: {
    $route() {
      this.pagination = {
        current: 1,
        size: 10,
        total: 0,
        searchKey: '',
        sortId: this.$route.params.id || null,
        labelId: this.$route.query.labelId,
      }
      this.articles.splice(0, this.articles.length)
      this.sortId = this.$route.params.id || null
      this.labelId = this.$route.query.labelId
      this.getSort()
      // 仅有具体分类时才加载文章
      if (this.sortId) {
        this.getArticles()
      }
      this.updatePageTitle()
    },
  },

  created() {
    this.getSort()
    // 仅有具体分类时才加载文章
    if (this.sortId) {
      this.getArticles()
    }
    this.updatePageTitle()
  },

  methods: {
    updatePageTitle() {
      const siteName = this.mainStore.webInfo?.webTitle || this.mainStore.webInfo?.webName || ''
      if (this.sort) {
        // 具体分类页：显示分类名
        document.title = `${this.sort.sortName} - ${siteName}`
        this.updateMetaTag('description', this.sort.sortDescription || `${this.sort.sortName}分类下的所有文章`)
      } else {
        // 分类列表页：显示"文章分类"
        document.title = `文章分类 - ${siteName}`
        this.updateMetaTag('description', '浏览所有文章分类，找到您感兴趣的内容主题')
      }
      window.OriginTitile = document.title
    },

    updateMetaTag(name, content) {
      let meta = document.querySelector(`meta[name="${name}"]`)
      if (!meta) {
        meta = document.createElement('meta')
        meta.setAttribute('name', name)
        document.head.appendChild(meta)
      }
      meta.setAttribute('content', content)
    },

    pageArticles() {
      this.pagination.current = this.pagination.current + 1
      this.getArticles()
    },

    getSort() {
      let sortInfo = this.mainStore.sortInfo
      if (!this.$common.isEmpty(sortInfo)) {
        if (this.sortId) {
          // 有分类ID，查找对应分类
          let sortArray = sortInfo.filter((f) => {
            return f.id === parseInt(this.sortId)
          })
          if (!this.$common.isEmpty(sortArray)) {
            this.sort = sortArray[0]
          }
        } else {
          // 没有分类ID，设置为null，表示显示所有分类的文章
          this.sort = null
        }
      }
      // 数据加载后更新标题
      this.$nextTick(() => {
        this.updatePageTitle()
      })
    },
    listArticle(label) {
      this.labelId = label.id
      this.pagination = {
        current: 1,
        size: 10,
        total: 0,
        searchKey: '',
        sortId: this.$route.params.id || null,
        labelId: label.id,
      }
      this.articles.splice(0, this.articles.length)
      this.$nextTick(() => {
        this.getArticles()
      })
    },
    getArticles() {
      this.$http
        .post(this.$constant.baseURL + '/article/listArticle', this.pagination)
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            this.articles = this.articles.concat(res.data.records)
            this.pagination.total = res.data.total
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error',
          })
        })
    },
  },
}
</script>

<style scoped>
.sort-content-area {
  background: var(--background);
  padding-top: 40px;
  min-height: calc(100vh - 260px);
  display: flex;
  flex-direction: column;
}
.poem-container {
  padding: 90px 0 40px;
  position: relative;
}
.poem-wrap {
  border-radius: 10px;
  z-index: 10;
  text-align: center;
  letter-spacing: 4px;
  font-weight: 300;
  width: 100%;
  max-width: 800px;
}
.poem-wrap div span {
  padding: 5px 10px;
  color: var(--white);
  font-size: 2em;
  border-radius: 5px;
}
.poem-wrap p {
  width: 100%;
  max-width: 800px;
  color: var(--white);
}
.poem-wrap p.poem {
  margin: 40px auto;
  font-size: 1.5em;
}
.sort-warp {
  width: 70%;
  max-width: 780px;
  margin: 0 auto;
  padding: 20px;
  border-radius: 10px;
  display: flex;
  flex-wrap: wrap;
}
.article-wrap {
  flex: 1;
  width: 70%;
  margin: 40px auto;
}
.isActive {
  animation: scale 1.5s ease-in-out infinite;
}
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}
.pagination {
  padding: 13px 15px;
  border: 1px solid var(--lightGray);
  border-radius: 3rem;
  color: var(--greyFont);
  width: 100px;
  user-select: none;
  cursor: pointer;
  text-align: center;
}
.pagination:hover {
  border: 1px solid var(--themeBackground);
  color: var(--themeBackground);
  box-shadow: 0 0 5px var(--themeBackground);
}

/* 分类列表卡片样式 */
.sort-list-warp {
  flex: 1;
  width: 70%;
  max-width: 900px;
  margin: 0 auto 30px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  align-content: start;
}

.sort-card {
  padding: 20px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--background);
}

.sort-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.sort-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sort-card-header h3 {
  margin: 0;
  font-size: 1.1em;
  color: var(--fontColor);
}

.sort-card-count {
  font-size: 0.85em;
  color: var(--white);
  background: var(--themeBackground);
  padding: 2px 8px;
  border-radius: 10px;
}

.sort-card-desc {
  font-size: 0.9em;
  color: var(--greyFont);
  margin: 8px 0 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sort-card-labels {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.sort-card-more {
  font-size: 0.8em;
  color: var(--greyFont);
  margin-left: 4px;
}

@media screen and (max-width: 900px) {
  .sort-list-warp {
    width: 90%;
    grid-template-columns: 1fr;
  }
  .sort-warp {
    width: 90%;
  }
  .article-wrap {
    width: 90%;
  }
}
</style>
