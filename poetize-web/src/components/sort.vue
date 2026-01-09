<template>
  <div>
    <!-- 分类头部 -->
    <div class="my-animation-slide-top">
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
      <!-- 如果没有分类信息，显示默认的诗句 -->
      <twoPoem v-else></twoPoem>
    </div>

    <div
      style="background: var(--background); padding-top: 40px"
      class="my-animation-slide-bottom"
    >
      <!-- 标签 -->
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

      <!-- 文章 -->
      <div class="article-wrap">
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
    twoPoem: defineAsyncComponent(() => import('./common/twoPoem')),
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
      this.getArticles()
    },
  },

  created() {
    this.getSort()
    this.getArticles()
  },

  mounted() {},

  methods: {
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
  width: 70%;
  margin: 40px auto;
  min-height: 600px;
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
@media screen and (max-width: 900px) {
  .sort-warp {
    width: 90%;
  }
  .article-wrap {
    width: 90%;
  }
}
</style>
