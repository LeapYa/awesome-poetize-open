<template>
  <div class="recycle-bin">
    <el-tabs v-model="activeTab" type="card" class="recycle-tabs">
      <!-- ==================== 文章回收站 ==================== -->
      <el-tab-pane label="文章回收站" name="article">
        <div class="handle-box">
          <el-input v-model="articleSearch.searchKey" placeholder="按标题搜索" class="handle-input mrb10"
                    clearable @keyup.enter.native="searchTrashArticles"/>
          <el-button type="primary" icon="el-icon-search" class="mrb10" @click="searchTrashArticles">搜索</el-button>
          <span class="retention-tip mrb10">回收站文章保留 {{ retention.articleRetentionDays }} 天，超期由系统自动彻底清理</span>
        </div>

        <el-table v-loading="articleLoading" :data="trashArticles" border
                  header-cell-class-name="table-header">
          <el-table-column label="文章" min-width="280">
            <template slot-scope="scope">
              <div class="article-title-cell">{{ scope.row.articleTitle }}</div>
              <div class="article-slug-cell">{{ scope.row.articleSlug || '无URL别名' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="可见状态" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.viewStatus ? 'success' : 'info'" size="small" disable-transitions>
                {{ scope.row.viewStatus ? '公开' : '隐藏' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="删除时间" width="160" align="center">
            <template slot-scope="scope">{{ formatTime(scope.row.deletedTime) }}</template>
          </el-table-column>
          <el-table-column label="剩余保留" width="110" align="center">
            <template slot-scope="scope">
              <el-tag :type="remainTagType(scope.row.deletedTime, retention.articleRetentionDays)"
                      size="small" disable-transitions>
                {{ remainText(scope.row.deletedTime, retention.articleRetentionDays) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" icon="el-icon-refresh-left" :loading="restoreLoading === scope.row.id"
                         @click="restoreArticle(scope.row)">恢复
              </el-button>
              <el-button v-if="isRealBoss" type="text" class="danger-text" icon="el-icon-delete"
                         :loading="purgeLoading === scope.row.id"
                         @click="purgeArticle(scope.row)">彻底删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination class="pager" background layout="total, sizes, prev, pager, next"
                       :total="articleSearch.total" :page-size="articleSearch.size"
                       :current-page="articleSearch.current"
                       @current-change="handleArticlePageChange" @size-change="handleArticleSizeChange"/>
      </el-tab-pane>

      <!-- ==================== 文章历史版本 ==================== -->
      <el-tab-pane label="历史版本" name="version">
        <div class="handle-box">
          <el-select v-model="versionSearch.articleId" filterable remote clearable :remote-method="searchArticleOptions"
                     :loading="articleOptionsLoading" placeholder="输入标题搜索文章" class="handle-input mrb10"
                     @change="loadVersions">
            <el-option v-for="item in articleOptions" :key="item.id" :label="item.articleTitle" :value="item.id"/>
          </el-select>
          <span class="retention-tip mrb10">每次更新前自动保存旧版快照（保留最近 {{ retention.maxVersionsPerArticle }} 版 / {{ retention.versionRetentionDays }} 天），恢复前会先快照当前版</span>
        </div>

        <el-table v-loading="versionLoading" :data="versions" border header-cell-class-name="table-header">
          <el-table-column label="版本号" width="90" align="center">
            <template slot-scope="scope">v{{ scope.row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="类型" width="110" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.snapshotType === 'UPDATE' ? 'primary' : 'warning'" size="small"
                      disable-transitions>
                {{ scope.row.snapshotType === 'UPDATE' ? '更新前' : '恢复前' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="articleTitle" label="当时标题" min-width="220" show-overflow-tooltip/>
          <el-table-column label="操作人" width="140" align="center">
            <template slot-scope="scope">{{ scope.row.editorUsername || '未知' }}</template>
          </el-table-column>
          <el-table-column label="快照时间" width="160" align="center">
            <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" icon="el-icon-view" @click="showVersionDetail(scope.row)">查看</el-button>
              <el-button type="text" icon="el-icon-refresh-left" :loading="versionRestoreLoading === scope.row.id"
                         @click="restoreVersion(scope.row)">恢复此版本
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ==================== 资源回收站 ==================== -->
      <el-tab-pane label="资源回收站" name="resource">
        <div class="handle-box">
          <el-input v-model="resourceSearch.searchKey" placeholder="按路径或文件名搜索" class="handle-input mrb10"
                    clearable @keyup.enter.native="searchTrashResources"/>
          <el-button type="primary" icon="el-icon-search" class="mrb10" @click="searchTrashResources">搜索</el-button>
          <span class="retention-tip mrb10">资源删除只是移入回收站，物理文件 {{ retention.resourceRetentionDays }} 天内不清理，可随时恢复</span>
          <span v-if="!isRealBoss" class="retention-tip mrb10">（仅站长可恢复或彻底删除资源）</span>
        </div>

        <el-table v-loading="resourceLoading" :data="trashResources" border header-cell-class-name="table-header">
          <el-table-column prop="originalName" label="文件名" min-width="160" show-overflow-tooltip/>
          <el-table-column prop="path" label="路径" min-width="260" show-overflow-tooltip/>
          <el-table-column label="大小" width="110" align="center">
            <template slot-scope="scope">{{ formatSize(scope.row.size) }}</template>
          </el-table-column>
          <el-table-column label="删除时间" width="160" align="center">
            <template slot-scope="scope">{{ formatTime(scope.row.trashTime) }}</template>
          </el-table-column>
          <el-table-column label="剩余保留" width="110" align="center">
            <template slot-scope="scope">
              <el-tag :type="remainTagType(scope.row.trashTime, retention.resourceRetentionDays)"
                      size="small" disable-transitions>
                {{ remainText(scope.row.trashTime, retention.resourceRetentionDays) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" icon="el-icon-view" @click="previewTrashResource(scope.row)">预览</el-button>
              <el-button v-if="isRealBoss" type="text" icon="el-icon-refresh-left"
                         :loading="resourceRestoreLoading === scope.row.id"
                         @click="restoreResource(scope.row)">恢复
              </el-button>
              <el-button v-if="isRealBoss" type="text" class="danger-text" icon="el-icon-delete"
                         :loading="resourcePurgeLoading === scope.row.id"
                         @click="purgeResource(scope.row)">彻底删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination class="pager" background layout="total, sizes, prev, pager, next"
                       :total="resourceSearch.total" :page-size="resourceSearch.size"
                       :current-page="resourceSearch.current"
                       @current-change="handleResourcePageChange" @size-change="handleResourceSizeChange"/>
      </el-tab-pane>

      <!-- ==================== 替换旧版备份 ==================== -->
      <el-tab-pane label="替换备份" name="backup">
        <div class="handle-box">
          <el-input v-model="backupSearch.resourceId" placeholder="按资源ID筛选" class="handle-input mrb10"
                    clearable @keyup.enter.native="searchBackups"/>
          <el-button type="primary" icon="el-icon-search" class="mrb10" @click="searchBackups">搜索</el-button>
          <span class="retention-tip mrb10">替换资源成功后，旧文件会保留在这里（保留 {{ retention.backupRetentionDays }} 天），误替换可一键恢复</span>
        </div>

        <el-table v-loading="backupLoading" :data="backups" border header-cell-class-name="table-header">
          <el-table-column prop="resourceId" label="资源ID" width="90" align="center"/>
          <el-table-column prop="originalPath" label="被替换的文件" min-width="260" show-overflow-tooltip/>
          <el-table-column label="旧版大小" width="110" align="center">
            <template slot-scope="scope">{{ formatSize(scope.row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="备份时间" width="160" align="center">
            <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="230" align="center" fixed="right">
            <template slot-scope="scope">
              <el-button type="text" icon="el-icon-view" @click="previewBackup(scope.row)">预览</el-button>
              <el-button v-if="isRealBoss" type="text" icon="el-icon-refresh-left"
                         :loading="backupRestoreLoading === scope.row.id"
                         @click="restoreBackup(scope.row)">恢复旧版
              </el-button>
              <el-button v-if="isRealBoss" type="text" class="danger-text" icon="el-icon-delete"
                         :loading="backupDeleteLoading === scope.row.id"
                         @click="deleteBackup(scope.row)">删除备份
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination class="pager" background layout="total, sizes, prev, pager, next"
                       :total="backupSearch.total" :page-size="backupSearch.size"
                       :current-page="backupSearch.current"
                       @current-change="handleBackupPageChange" @size-change="handleBackupSizeChange"/>
      </el-tab-pane>
    </el-tabs>

    <!-- 版本详情弹窗 -->
    <el-dialog :visible.sync="detailDialog.visible" title="版本详情" width="70%" top="5vh" append-to-body
               v-loading="versionDetailLoading">
      <div v-if="detailDialog.version" class="version-detail">
        <div class="detail-meta">
          <span>版本：v{{ detailDialog.version.versionNo }}</span>
          <span>类型：{{ detailDialog.version.snapshotType === 'UPDATE' ? '更新前' : '恢复前' }}</span>
          <span>操作人：{{ detailDialog.version.editorUsername || '未知' }}</span>
          <span>快照时间：{{ formatTime(detailDialog.version.createTime) }}</span>
        </div>
        <div class="detail-title">{{ detailDialog.version.articleTitle }}</div>
        <pre class="detail-content">{{ detailDialog.version.articleContent }}</pre>
      </div>
      <span slot="footer">
        <el-button @click="detailDialog.visible = false">关 闭</el-button>
        <el-button type="primary" @click="restoreVersion(detailDialog.version)">恢复此版本</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {useMainStore} from "../../stores/main";
import {fetchRetentionConfig, getRetentionConfig} from "../../utils/retentionConfig";

export default {
  name: "RecycleBin",
  data() {
    return {
      activeTab: "article",
      // 保留策略由后端下发（/admin/recycle/config），未取到前使用默认值
      retention: {...getRetentionConfig()},
      articleSearch: {
        searchKey: "",
        current: 1,
        size: 10,
        total: 0
      },
      trashArticles: [],
      articleLoading: false,
      restoreLoading: null,
      purgeLoading: null,
      versionSearch: {
        articleId: null
      },
      articleOptions: [],
      articleOptionsLoading: false,
      versions: [],
      versionLoading: false,
      versionDetailLoading: false,
      versionRestoreLoading: null,
      detailDialog: {
        visible: false,
        version: null
      },
      resourceSearch: {
        searchKey: "",
        current: 1,
        size: 10,
        total: 0
      },
      trashResources: [],
      resourceLoading: false,
      resourceRestoreLoading: null,
      resourcePurgeLoading: null,
      backupSearch: {
        current: 1,
        size: 10,
        total: 0,
        resourceId: ""
      },
      backups: [],
      backupLoading: false,
      backupRestoreLoading: null,
      backupDeleteLoading: null
    };
  },
  computed: {
    mainStore() {
      return useMainStore();
    },
    // 后端"仅站长"端点用 @LoginCheck(0)（userType===0）；UserVO.isBoss 对普通管理员(1)也为 true，
    // 若沿用 isBoss 会让管理员看到并确认站长专属按钮，随后拿到 403，故此处按 userType 精确判定
    isRealBoss() {
      return this.mainStore.currentAdmin.userType === 0;
    }
  },
  created() {
    this.loadRetention();
    this.getTrashArticles();
    this.searchArticleOptions("");
    this.getTrashResources();
    this.getBackups();
  },
  methods: {
    loadRetention() {
      fetchRetentionConfig().then((config) => {
        this.retention = {...config};
      });
    },
    getTrashArticles() {
      this.articleLoading = true;
      this.$http.post(this.$constant.baseURL + "/admin/recycle/article/list", {
        current: this.articleSearch.current,
        size: this.articleSearch.size,
        searchKey: this.articleSearch.searchKey
      }, true).then((res) => {
        if (!this.$common.isEmpty(res.data)) {
          this.trashArticles = res.data.records || [];
          this.articleSearch.total = res.data.total || 0;
        }
      }).catch((error) => {
        this.$message({message: error.message, type: "error"});
      }).finally(() => {
        this.articleLoading = false;
      });
    },
    searchTrashArticles() {
      this.articleSearch.current = 1;
      this.getTrashArticles();
    },
    handleArticlePageChange(val) {
      this.articleSearch.current = val;
      this.getTrashArticles();
    },
    handleArticleSizeChange(val) {
      this.articleSearch.size = val;
      this.articleSearch.current = 1;
      this.getTrashArticles();
    },
    restoreArticle(row) {
      this.$confirm(`确定恢复文章《${row.articleTitle}》吗？`, "恢复文章", {
        confirmButtonText: "恢复",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        this.restoreLoading = row.id;
        this.$http.post(this.$constant.baseURL + "/admin/recycle/article/restore?id=" + row.id, {}, true)
          .then((res) => {
            this.$message({message: res.message || "恢复成功", type: "success"});
            this.getTrashArticles();
          }).catch((error) => {
            this.$message({message: error.message, type: "error"});
          }).finally(() => {
            this.restoreLoading = null;
          });
      }).catch(() => {
      });
    },
    purgeArticle(row) {
      this.$confirm(`彻底删除后文章、翻译、全部历史版本，以及该文章的评论、修订草稿与付费记录都将永久消失，且不可恢复！确定彻底删除《${row.articleTitle}》吗？`,
        "彻底删除", {
          confirmButtonText: "彻底删除",
          cancelButtonText: "取消",
          type: "error",
          confirmButtonClass: "el-button--danger"
        }).then(() => {
        this.purgeLoading = row.id;
        this.$http.post(this.$constant.baseURL + "/admin/recycle/article/purge?id=" + row.id, {}, true)
          .then(() => {
            this.$message({message: "已彻底删除", type: "success"});
            this.getTrashArticles();
          }).catch((error) => {
            this.$message({message: error.message, type: "error"});
          }).finally(() => {
            this.purgeLoading = null;
          });
      }).catch(() => {
      });
    },
    searchArticleOptions(keyword) {
      const url = this.isRealBoss ? "/admin/article/boss/list" : "/admin/article/user/list";
      this.articleOptionsLoading = true;
      this.$http.post(this.$constant.baseURL + url,
        {current: 1, size: 50, searchKey: keyword || ""}, true)
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            this.articleOptions = res.data.records || [];
          }
        }).catch(() => {
          // 版本tab的文章选择器加载失败不阻塞页面
        }).finally(() => {
          this.articleOptionsLoading = false;
        });
    },
    loadVersions() {
      if (!this.versionSearch.articleId) {
        return;
      }
      this.versionLoading = true;
      this.$http.get(this.$constant.baseURL + "/admin/recycle/article/versionList",
        {articleId: this.versionSearch.articleId}, true)
        .then((res) => {
          this.versions = res.data || [];
        }).catch((error) => {
          this.$message({message: error.message, type: "error"});
        }).finally(() => {
          this.versionLoading = false;
        });
    },
    showVersionDetail(row) {
      this.versionDetailLoading = true;
      this.$http.get(this.$constant.baseURL + "/admin/recycle/article/versionDetail", {id: row.id}, true)
        .then((res) => {
          this.detailDialog.version = res.data;
          this.detailDialog.visible = true;
        }).catch((error) => {
          this.$message({message: error.message, type: "error"});
        }).finally(() => {
          this.versionDetailLoading = false;
        });
    },
    restoreVersion(row) {
      if (!row) {
        return;
      }
      this.$confirm(`恢复前会自动快照当前版本（可再次撤销）。确定把文章恢复到 v${row.versionNo} 吗？`,
        "恢复历史版本", {
          confirmButtonText: "恢复",
          cancelButtonText: "取消",
          type: "warning"
        }).then(() => {
        this.versionRestoreLoading = row.id;
        this.$http.post(this.$constant.baseURL + "/admin/recycle/article/restoreVersion"
          + "?articleId=" + row.articleId + "&versionId=" + row.id, {}, true)
          .then((res) => {
            this.$message({message: res.message || "已恢复", type: "success"});
            this.detailDialog.visible = false;
            this.loadVersions();
          }).catch((error) => {
            this.$message({message: error.message, type: "error"});
          }).finally(() => {
            this.versionRestoreLoading = null;
          });
      }).catch(() => {
      });
    },
    getTrashResources() {
      this.resourceLoading = true;
      this.$http.post(this.$constant.baseURL + "/resource/trashList", {
        current: this.resourceSearch.current,
        size: this.resourceSearch.size,
        searchKey: this.resourceSearch.searchKey
      }, true).then((res) => {
        if (!this.$common.isEmpty(res.data)) {
          this.trashResources = res.data.records || [];
          this.resourceSearch.total = res.data.total || 0;
        }
      }).catch((error) => {
        this.$message({message: error.message, type: "error"});
      }).finally(() => {
        this.resourceLoading = false;
      });
    },
    searchTrashResources() {
      this.resourceSearch.current = 1;
      this.getTrashResources();
    },
    handleResourcePageChange(val) {
      this.resourceSearch.current = val;
      this.getTrashResources();
    },
    handleResourceSizeChange(val) {
      this.resourceSearch.size = val;
      this.resourceSearch.current = 1;
      this.getTrashResources();
    },
    restoreResource(row) {
      this.$confirm("确定恢复资源 " + (row.originalName || row.path) + " 吗？", "恢复资源", {
        confirmButtonText: "恢复",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        this.resourceRestoreLoading = row.id;
        this.$http.post(this.$constant.baseURL + "/resource/restoreTrash?id=" + row.id, {}, true)
          .then(() => {
            this.$message({message: "恢复成功", type: "success"});
            this.getTrashResources();
          }).catch((error) => {
            this.$message({message: error.message, type: "error"});
          }).finally(() => {
            this.resourceRestoreLoading = null;
          });
      }).catch(() => {
      });
    },
    purgeResource(row) {
      this.$confirm("彻底删除后物理文件将永久消失，且不可恢复！若该资源仍被文章引用，将退回回收站而不删除。确定彻底删除 " + (row.originalName || row.path) + " 吗？",
        "彻底删除", {
          confirmButtonText: "彻底删除",
          cancelButtonText: "取消",
          type: "error",
          confirmButtonClass: "el-button--danger"
        }).then(() => {
        this.resourcePurgeLoading = row.id;
        this.$http.post(this.$constant.baseURL + "/resource/purgeTrash?id=" + row.id, {}, true)
          .then(() => {
            this.$message({message: "已彻底删除", type: "success"});
            this.getTrashResources();
          }).catch((error) => {
            this.$message({message: error.message, type: "error"});
          }).finally(() => {
            this.resourcePurgeLoading = null;
          });
      }).catch(() => {
      });
    },
    getBackups() {
      this.backupLoading = true;
      const resourceId = String(this.backupSearch.resourceId || "").trim();
      this.$http.post(this.$constant.baseURL + "/resource/backupList", {
        current: this.backupSearch.current,
        size: this.backupSearch.size,
        resourceId: resourceId ? Number(resourceId) : null
      }, true).then((res) => {
        if (!this.$common.isEmpty(res.data)) {
          this.backups = res.data.records || [];
          this.backupSearch.total = res.data.total || 0;
        }
      }).catch((error) => {
        this.$message({message: error.message, type: "error"});
      }).finally(() => {
        this.backupLoading = false;
      });
    },
    searchBackups() {
      this.backupSearch.current = 1;
      this.getBackups();
    },
    previewTrashResource(row) {
      this.openPreview("/resource/trashPreview?id=" + row.id);
    },
    previewBackup(row) {
      this.openPreview("/resource/backupPreview?id=" + row.id);
    },
    openPreview(path) {
      window.open(this.$constant.baseURL + path, "_blank");
    },
    handleBackupPageChange(val) {
      this.backupSearch.current = val;
      this.getBackups();
    },
    handleBackupSizeChange(val) {
      this.backupSearch.size = val;
      this.backupSearch.current = 1;
      this.getBackups();
    },
    restoreBackup(row) {
      this.$confirm("恢复前会自动备份当前文件（可再次撤销）。确定恢复替换前的旧版本吗？", "恢复旧版", {
        confirmButtonText: "恢复",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        this.backupRestoreLoading = row.id;
        this.$http.post(this.$constant.baseURL + "/resource/restoreBackup?id=" + row.id, {}, true)
          .then(() => {
            this.$message({message: "已恢复旧版本文件", type: "success"});
            this.getBackups();
          }).catch((error) => {
            this.$message({message: error.message, type: "error"});
          }).finally(() => {
            this.backupRestoreLoading = null;
          });
      }).catch(() => {
      });
    },
    deleteBackup(row) {
      this.$confirm("删除备份后旧版本文件将永久消失，且不可恢复！确定删除吗？", "删除备份", {
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        type: "error",
        confirmButtonClass: "el-button--danger"
      }).then(() => {
        this.backupDeleteLoading = row.id;
        this.$http.post(this.$constant.baseURL + "/resource/deleteBackup?id=" + row.id, {}, true)
          .then(() => {
            this.$message({message: "备份已删除", type: "success"});
            this.getBackups();
          }).catch((error) => {
            this.$message({message: error.message, type: "error"});
          }).finally(() => {
            this.backupDeleteLoading = null;
          });
      }).catch(() => {
      });
    },
    formatSize(size) {
      if (size === null || size === undefined) {
        return "-";
      }
      if (size > 1048576) {
        return (size / 1048576).toFixed(2) + " MB";
      }
      if (size > 1024) {
        return (size / 1024).toFixed(1) + " KB";
      }
      return size + " B";
    },
    formatTime(time) {
      if (this.$common.isEmpty(time)) {
        return "-";
      }
      return String(time).replace("T", " ").slice(0, 19);
    },
    // 返回剩余保留天数；删除时间为空或解析失败时返回 null（未知），不再伪装成 0 天
    remainDays(time, retentionDays) {
      if (this.$common.isEmpty(time)) {
        return null;
      }
      const deleted = new Date(String(time).replace("T", " ")).getTime();
      if (Number.isNaN(deleted)) {
        return null;
      }
      const days = Number(retentionDays) > 0 ? Number(retentionDays) : 30;
      const elapsed = Date.now() - deleted;
      const remain = days - Math.floor(elapsed / 86400000);
      return Math.max(remain, 0);
    },
    remainText(time, retentionDays) {
      const remain = this.remainDays(time, retentionDays);
      return remain === null ? "未知" : remain + " 天";
    },
    remainTagType(time, retentionDays) {
      const remain = this.remainDays(time, retentionDays);
      if (remain === null) {
        return "info";
      }
      return remain > 7 ? "success" : "danger";
    }
  }
};
</script>

<style scoped>
.recycle-bin {
  padding: 20px;
}

.recycle-tabs {
  background: var(--background, #fff);
  border-radius: 8px;
  padding: 10px 20px 20px;
}

.handle-box {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0 10px;
  margin-bottom: 15px;
}

.handle-input {
  width: 240px;
}

.mrb10 {
  margin-bottom: 10px;
}

.retention-tip {
  color: #909399;
  font-size: 12px;
}

.article-title-cell {
  font-weight: 600;
}

.article-slug-cell {
  color: #909399;
  font-size: 12px;
}

.danger-text {
  color: #f56c6c !important;
}

.pager {
  margin-top: 15px;
  text-align: right;
}

.version-detail {
  max-height: 65vh;
  overflow: auto;
}

.detail-meta {
  display: flex;
  gap: 18px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
}

.detail-content {
  background: var(--background, #f5f7fa);
  padding: 12px;
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 45vh;
  overflow: auto;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
}
</style>
