<template>
  <div>
    <div>
      <div v-if="routeSearchDisplayKeyword" style="margin-bottom: 12px; padding: 10px 14px; border-radius: 8px; background: #f4f8ff; color: #606266; display: flex; align-items: center; justify-content: space-between; gap: 12px;">
        <span>当前显示的是全局搜索结果：{{ routeSearchDisplayKeyword }}</span>
        <el-button type="text" @click="clearGlobalSearchFilter">清除全局筛选</el-button>
      </div>
      <div class="handle-box">
        <el-select clearable v-model="pagination.resourceType" placeholder="资源类型" class="handle-select mrb10">
          <el-option key="21" label="Video.Article" value="video/article"></el-option>
          <el-option key="20" label="公共资源" value="assets"></el-option>
          <el-option key="10" label="表情包" value="internetMeme"></el-option>
          <el-option key="1" label="用户头像" value="userAvatar"></el-option>
          <el-option key="2" label="文章封面" value="articleCover"></el-option>
          <el-option key="3" label="文章图片" value="articlePicture"></el-option>
          <el-option key="5" label="网站头像" value="webAvatar"></el-option>
          <el-option key="4" label="背景图片" value="webBackgroundImage"></el-option>
          <el-option key="6" label="随机头像" value="randomAvatar"></el-option>
          <el-option key="7" label="随机封面" value="randomCover"></el-option>
          <el-option key="8" label="画笔图片" value="graffiti"></el-option>
          <el-option key="9" label="评论图片" value="commentPicture"></el-option>
          <el-option key="11" label="聊天群头像" value="im/groupAvatar"></el-option>
          <el-option key="12" label="群聊天图片" value="im/groupMessage"></el-option>
          <el-option key="13" label="朋友聊天图片" value="im/friendMessage"></el-option>
          <el-option key="14" label="音乐声音" value="funnyUrl"></el-option>
          <el-option key="15" label="音乐封面" value="funnyCover"></el-option>
          <el-option key="16" label="Love.Cover" value="love/bgCover"></el-option>
          <el-option key="17" label="Love.Man" value="love/manCover"></el-option>
          <el-option key="18" label="Love.Woman" value="love/womanCover"></el-option>
          <el-option key="19" label="收藏夹封面" value="favoritesCover"></el-option>
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="search()">搜索</el-button>
        <el-button type="primary" @click="addResources()">新增资源</el-button>
      </div>
      <el-table :data="displayedResources" border class="table" header-cell-class-name="table-header">
        <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>
        <el-table-column prop="originalName" label="名称" align="center"></el-table-column>
        <el-table-column prop="userId" label="用户ID" align="center"></el-table-column>
        <el-table-column prop="type" label="资源类型" align="center"></el-table-column>
        <el-table-column label="状态" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === false ? 'danger' : 'success'"
                    disable-transitions>
              {{scope.row.status === false ? '禁用' : '启用'}}
            </el-tag>
            <el-switch @click.native="changeStatus(scope.row)" v-model="scope.row.status"></el-switch>
          </template>
        </el-table-column>
        <el-table-column label="路径" align="center">
          <template slot-scope="scope">
            <div style="display: flex; align-items: center; justify-content: center;">
              <el-tooltip :content="scope.row.path" placement="top">
                <span style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                  {{scope.row.path}}
                </span>
              </el-tooltip>
              <template v-if="!$common.isEmpty(scope.row.mimeType) && (scope.row.mimeType.includes('image') || scope.row.mimeType.includes('video') || isFont(scope.row.mimeType))">
                <el-button type="text" icon="el-icon-view" size="mini" style="margin-left: 5px;"
                           @click="previewMedia(scope.row.path, scope.row.mimeType, scope.row.originalName)">
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="大小(KB)" align="center">
          <template slot-scope="scope">
            {{Math.round(scope.row.size / 1024)}}
          </template>
        </el-table-column>
        <el-table-column prop="mimeType" label="类型" align="center"></el-table-column>
        <el-table-column prop="storeType" label="存储平台" align="center"></el-table-column>
        <el-table-column label="创建时间" align="center">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-delete" style="color: var(--orangeRed)"
                       @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!routeSearchDisplayKeyword" class="pagination">
        <el-pagination background layout="total, prev, pager, next"
                       :current-page="pagination.current"
                       :page-size="pagination.size"
                       :total="pagination.total"
                       @current-change="handlePageChange">
        </el-pagination>
      </div>
    </div>

    <el-dialog title="文件"
               :visible.sync="resourceDialog"
               width="25%"
               custom-class="centered-dialog"
               :append-to-body="true"
               :close-on-click-modal="false"
               destroy-on-close
               center>
      <div>
        <div style="display: flex;margin-bottom: 10px">
          <div style="line-height: 40px">存储平台：</div>
          <el-select v-model="storeType" placeholder="存储平台" style="width: 120px">
            <el-option
              v-for="(item, i) in storeTypes"
              :key="i"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </div>
        <uploadPicture :isAdmin="true" :prefix="pagination.resourceType" @addPicture="addFile"
                       :storeType="storeType"
                       :listType="'text'" :accept="'image/*, video/*, audio/*'"
                       :maxSize="100" :maxNumber="10"></uploadPicture>
      </div>
    </el-dialog>

    <!-- 媒体预览对话框 -->
    <el-dialog :title="getPreviewTitle()"
               :visible.sync="previewVisible"
               :width="isFont(previewMediaType) ? '80%' : '60%'"
               custom-class="centered-dialog"
               :append-to-body="true"
               :close-on-click-modal="true"
               destroy-on-close
               :before-close="handlePreviewClose"
               center>
      <div style="text-align: center;">
        <!-- 图片预览（支持放大） -->
        <el-image v-if="previewMediaType.includes('image')" 
                  :src="previewMediaUrl" 
                  :preview-src-list="[previewMediaUrl]"
                  fit="contain"
                  style="max-width: 100%; max-height: 60vh; cursor: pointer;">
        </el-image>
        
        <!-- 视频预览 -->
        <video v-else-if="previewMediaType.includes('video')" 
               :src="previewMediaUrl" 
               controls 
               style="max-width: 100%; max-height: 60vh;">
          您的浏览器不支持视频播放
        </video>
        
        <!-- 字体预览 -->
        <div v-else-if="isFont(previewMediaType)" style="text-align: left;">
          <div class="font-info" style="margin-bottom: 20px; padding: 15px; background: #f5f7fa; border-radius: 4px;">
            <h3 style="margin: 0 0 10px 0; color: #409EFF;">{{ previewFileName }}</h3>
            <p style="margin: 0; color: #666;">点击文字可以查看字体效果</p>
          </div>
          
          <div v-if="fontLoaded" class="font-preview-content">
            <div v-for="textGroup in fontPreviewTexts" :key="textGroup.label" style="margin-bottom: 25px;">
              <h4 style="color: #606266; margin: 0 0 10px 0; font-size: 14px;">{{ textGroup.label }}</h4>
              <div v-for="size in fontSizes" :key="size" 
                   :style="{ 
                     fontFamily: loadedFontName + ', Arial, sans-serif', 
                     fontSize: size + 'px',
                     lineHeight: 1.4,
                     margin: '8px 0',
                     padding: '5px',
                     border: '1px solid #eee',
                     borderRadius: '3px',
                     background: '#fff'
                   }"
                   class="font-sample">
                <span style="font-size: 12px; color: #999; margin-right: 10px;">{{ size }}px:</span>
                {{ textGroup.content }}
              </div>
            </div>
          </div>
          
          <div v-else style="padding: 40px; text-align: center;">
            <i class="el-icon-loading" style="font-size: 24px; margin-bottom: 10px;"></i>
            <p>正在加载字体文件...</p>
          </div>
        </div>
        
        <!-- 其他文件类型提示 -->
        <div v-else style="padding: 20px; color: #666;">
          <i class="el-icon-document" style="font-size: 48px; margin-bottom: 10px;"></i>
          <p>暂不支持预览此文件类型</p>
          <p>文件路径：{{ previewMediaUrl }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { useMainStore } from '@/stores/main';

const uploadPicture = () => import('../common/uploadPicture');

function normalizeSearchText(value) {
  return ((value || '') + '').toLowerCase().replace(/\s+/g, '').trim();
}

export default {
  components: {
    uploadPicture
  },
  data() {
    return {
      pagination: {
        current: 1,
        size: 10,
        total: 0,
        resourceType: '',
        searchKey: ''
      },
      resources: [],
      resourceDialog: false,
      storeTypes: [
        { label: '服务器', value: 'local' },
        { label: '七牛云', value: 'qiniu' },
        { label: '兰空图床', value: 'lsky' },
        { label: '简单图床', value: 'easyimage' }
      ],
      storeType: 'local',
      previewMediaUrl: '',
      previewMediaType: '',
      previewFileName: '',
      previewVisible: false,
      fontLoaded: false,
      loadedFontName: '',
      fontPreviewTexts: [
        { label: '英文大写', content: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' },
        { label: '英文小写', content: 'abcdefghijklmnopqrstuvwxyz' },
        { label: '数字', content: '0123456789' },
        { label: '中文示例', content: '床前明月光，疑是地上霜。举头望明月，低头思故乡。' },
        { label: '符号', content: '!@#$%^&*()_+-=[]{}|;:,.<>?' },
        { label: '英文句子', content: 'The quick brown fox jumps over the lazy dog.' }
      ],
      fontSizes: [14, 18, 24, 32, 48]
    };
  },

  computed: {
    mainStore() {
      return useMainStore();
    },
    routeSearchDisplayKeyword() {
      return ((this.$route.query.search || '') + '').trim();
    },
    routeSearchKeyword() {
      return normalizeSearchText(this.routeSearchDisplayKeyword);
    },
    filteredResources() {
      if (!this.routeSearchKeyword) {
        return this.resources;
      }
      return this.filterResourcesByKeyword(this.resources, this.routeSearchKeyword);
    },
    displayedResources() {
      return this.routeSearchKeyword ? this.filteredResources : this.resources;
    }
  },

  watch: {
    '$route.query': {
      immediate: true,
      handler() {
        this.applyRouteQuery();
        this.getResources();
      }
    }
  },

  created() {
    if (this.mainStore && this.mainStore.sysConfig && this.mainStore.sysConfig['store.type']) {
      this.storeType = this.mainStore.sysConfig['store.type'];
    }
  },

  beforeDestroy() {
    this.cleanupFont();
  },

  methods: {
    applyRouteQuery() {
      const query = this.$route.query || {};
      this.pagination.resourceType = query.resourceType || '';
      this.pagination.searchKey = ((query.search || '') + '').trim();
      this.pagination.current = 1;
    },
    filterResourcesByKeyword(resources, keyword) {
      if (!keyword) {
        return resources || [];
      }
      return (resources || []).filter((item) => {
        return [item.originalName, item.type, item.path, item.mimeType, item.storeType, String(item.id || ''), String(item.userId || '')]
          .some((value) => normalizeSearchText(value).includes(keyword));
      });
    },
    clearGlobalSearchFilter() {
      const nextQuery = { ...this.$route.query };
      delete nextQuery.search;
      delete nextQuery.resourceType;
      this.$router.replace({ path: this.$route.path, query: nextQuery });
    },
    handleDelete(item) {
      this.$confirm('确认删除资源？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success',
        center: true,
        customClass: 'mobile-responsive-confirm'
      }).then(() => {
        this.$http.post(this.$constant.baseURL + '/resource/deleteResource', { path: item.path }, true, false)
          .then(() => {
            this.pagination.current = 1;
            this.getResources();
            this.$message({
              message: '删除成功！',
              type: 'success'
            });
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: 'error'
            });
          });
      }).catch(() => {
        this.$message({
          type: 'success',
          message: '已取消删除!'
        });
      });
    },
    addFile() {
    },
    addResources() {
      if (this.$common.isEmpty(this.pagination.resourceType)) {
        this.$message({
          message: '请选择资源类型！',
          type: 'error'
        });
        return;
      }
      this.resourceDialog = true;
    },
    search() {
      this.pagination.total = 0;
      this.pagination.current = 1;
      this.getResources();
    },
    getResources() {
      const requestPagination = {
        current: this.routeSearchKeyword ? 1 : this.pagination.current,
        size: this.routeSearchKeyword ? 500 : this.pagination.size,
        resourceType: this.pagination.resourceType,
        searchKey: ''
      };

      this.$http.post(this.$constant.baseURL + '/resource/listResource', requestPagination, true)
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            const records = res.data.records || [];
            this.resources = records;
            this.pagination.total = this.routeSearchKeyword
              ? this.filterResourcesByKeyword(records, this.routeSearchKeyword).length
              : res.data.total;
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error'
          });
        });
    },
    changeStatus(item) {
      this.$http.get(this.$constant.baseURL + '/resource/changeResourceStatus', {
        id: item.id,
        flag: item.status
      }, true)
        .then(() => {
          this.$message({
            message: '修改成功！',
            type: 'success'
          });
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: 'error'
          });
        });
    },
    handlePageChange(val) {
      this.pagination.current = val;
      this.getResources();
    },
    previewMedia(mediaPath, mimeType, fileName) {
      this.previewMediaUrl = mediaPath;
      this.previewMediaType = mimeType;
      this.previewFileName = fileName || '';

      if (this.isFont(mimeType)) {
        this.loadFont(mediaPath);
      } else {
        this.fontLoaded = false;
      }

      this.previewVisible = true;
    },
    isFont(mimeType) {
      const fontMimeTypes = [
        'font/woff', 'font/woff2', 'font/ttf', 'font/otf',
        'application/font-woff', 'application/font-woff2',
        'application/x-font-ttf', 'application/x-font-otf',
        'application/font-sfnt', 'font/opentype'
      ];
      return fontMimeTypes.some(type => mimeType.includes(type)) ||
        /\.(woff|woff2|ttf|otf|eot)$/i.test(this.previewFileName);
    },
    loadFont(fontUrl) {
      this.cleanupFont();
      this.loadedFontName = 'preview-font-' + Date.now();

      const style = document.createElement('style');
      style.id = 'font-preview-style';
      style.innerHTML = "\n          @font-face {\n            font-family: '" + this.loadedFontName + "';\n            src: url('" + fontUrl + "');\n          }\n        ";

      if (style && style.nodeType === Node.ELEMENT_NODE && document.head && typeof document.head.appendChild === 'function') {
        try {
          document.head.appendChild(style);
        } catch (e) {
        }
      }

      const testDiv = document.createElement('div');
      testDiv.style.fontFamily = this.loadedFontName;
      testDiv.style.position = 'absolute';
      testDiv.style.left = '-9999px';
      testDiv.innerHTML = 'Test';
      if (testDiv && testDiv.nodeType === Node.ELEMENT_NODE && document.body && typeof document.body.appendChild === 'function') {
        try {
          document.body.appendChild(testDiv);
        } catch (e) {
        }
      }

      setTimeout(() => {
        this.fontLoaded = true;
        if (document.body.contains(testDiv)) {
          document.body.removeChild(testDiv);
        }
      }, 100);
    },
    cleanupFont() {
      const existingStyle = document.getElementById('font-preview-style');
      if (existingStyle) {
        existingStyle.remove();
      }
      this.fontLoaded = false;
      this.loadedFontName = '';
    },
    getPreviewTitle() {
      if (this.previewMediaType.includes('image')) {
        return '图片预览（点击图片可放大）';
      }
      if (this.previewMediaType.includes('video')) {
        return '视频预览';
      }
      if (this.isFont(this.previewMediaType)) {
        return '字体预览';
      }
      return '文件预览';
    },
    handlePreviewClose(done) {
      this.cleanupFont();
      done();
    },
    formatDateTime(dateTime) {
      if (!dateTime) return '-';
      const date = new Date(dateTime);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
    }
  }
};
</script>

<style scoped>

  .handle-box {
    margin-bottom: 20px;
  }

  .handle-select {
    width: 200px;
  }

  .table {
    width: 100%;
    font-size: 14px;
  }

  .mrb10 {
    margin-right: 10px;
    margin-bottom: 10px;
  }

  .table-td-thumb {
    display: block;
    margin: auto;
    width: 40px;
    height: 40px;
  }

  .pagination {
    margin: 20px 0;
    text-align: right;
  }

  .el-switch {
    margin: 5px;
  }

  .font-sample {
    /* 性能优化: 只监听颜色变化 */
    transition: color 0.2s ease, background-color 0.2s ease;
  }

  .font-sample:hover {
    border-color: #409EFF !important;
    box-shadow: 0 0 5px rgba(64, 158, 255, 0.3);
  }

  .font-preview-content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .font-info h3 {
    display: flex;
    align-items: center;
  }

  .font-info h3::before {
    content: "🔤";
    margin-right: 8px;
    font-size: 18px;
  }

  /* ===========================================
     表单移动端样式 - PC端和移动端响应式
     =========================================== */
  
  /* PC端样式 - 768px以上 */
  @media screen and (min-width: 769px) {
    ::v-deep .el-form-item__label {
      float: left !important;
    }
  }

  /* 移动端样式 - 768px及以下 */
  @media screen and (max-width: 768px) {
    /* 表单标签 - 垂直布局 */
    ::v-deep .el-form-item__label {
      float: none !important;
      width: 100% !important;
      text-align: left !important;
      margin-bottom: 8px !important;
      font-weight: 500 !important;
      font-size: 14px !important;
      padding-bottom: 0 !important;
      line-height: 1.5 !important;
    }

    ::v-deep .el-form-item__content {
      margin-left: 0 !important;
      width: 100% !important;
    }

    ::v-deep .el-form-item {
      margin-bottom: 20px !important;
    }

    /* 输入框移动端优化 */
    ::v-deep .el-input__inner {
      font-size: 16px !important;
      height: 44px !important;
      border-radius: 8px !important;
    }

    /* 选择器移动端优化 */
    ::v-deep .el-select {
      width: 100% !important;
    }

    ::v-deep .el-select .el-input__inner {
      height: 44px !important;
      line-height: 44px !important;
    }

    /* 按钮移动端优化 */
    ::v-deep .el-button {
      min-height: 40px !important;
      border-radius: 8px !important;
    }

    /* 对话框移动端优化 */
    ::v-deep .el-dialog {
      width: 95% !important;
      margin-top: 5vh !important;
    }

    ::v-deep .el-dialog__body {
      padding: 15px !important;
    }

    /* 搜索框移动端优化 */
    .handle-select {
      width: 100% !important;
      margin-bottom: 10px !important;
    }
  }

  /* 极小屏幕优化 - 480px及以下 */
  @media screen and (max-width: 480px) {
    ::v-deep .el-form-item__label {
      font-size: 13px !important;
    }

    ::v-deep .el-input__inner,
    ::v-deep .el-select .el-input__inner {
      height: 40px !important;
      line-height: 40px !important;
      font-size: 15px !important;
    }

    ::v-deep .el-button {
      min-height: 38px !important;
      font-size: 14px !important;
    }
  }
</style>



