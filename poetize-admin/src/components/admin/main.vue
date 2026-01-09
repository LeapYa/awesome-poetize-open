<template>
  <div :class="{ 'main-dark-mode': isDarkMode }">
    <div v-loading="loading">
      <el-tag effect="dark" class="my-tag">
        <svg viewBox="0 0 1024 1024" width="20" height="20" style="vertical-align: -4px;">
          <path
            d="M767.1296 808.6528c16.8448 0 32.9728 2.816 48.0256 8.0384 20.6848 7.1168 43.52 1.0752 57.1904-15.9744a459.91936 459.91936 0 0 0 70.5024-122.88c7.8336-20.48 1.0752-43.264-15.9744-57.088-49.6128-40.192-65.0752-125.3888-31.3856-185.856a146.8928 146.8928 0 0 1 30.3104-37.9904c16.2304-14.5408 22.1696-37.376 13.9264-57.6a461.27104 461.27104 0 0 0-67.5328-114.9952c-13.6192-16.9984-36.4544-22.9376-57.0368-15.8208a146.3296 146.3296 0 0 1-48.0256 8.0384c-70.144 0-132.352-50.8928-145.2032-118.7328-4.096-21.6064-20.736-38.5536-42.4448-41.8304-22.0672-3.2768-44.6464-5.0176-67.6864-5.0176-21.4528 0-42.5472 1.536-63.232 4.4032-22.3232 3.1232-40.2432 20.48-43.52 42.752-6.912 46.6944-36.0448 118.016-145.7152 118.4256-17.3056 0.0512-33.8944-2.9696-49.3056-8.448-21.0432-7.4752-44.3904-1.4848-58.368 15.9232A462.14656 462.14656 0 0 0 80.4864 348.16c-7.6288 20.0192-2.7648 43.008 13.4656 56.9344 55.5008 47.8208 71.7824 122.88 37.0688 185.1392a146.72896 146.72896 0 0 1-31.6416 39.168c-16.8448 14.7456-23.0912 38.1952-14.5408 58.9312 16.896 41.0112 39.5776 79.0016 66.9696 113.0496 13.9264 17.3056 37.2736 23.1936 58.2144 15.7184 15.4112-5.4784 32-8.4992 49.3056-8.4992 71.2704 0 124.7744 49.408 142.1312 121.2928 4.9664 20.48 21.4016 36.0448 42.24 39.168 22.2208 3.328 44.9536 5.0688 68.096 5.0688 23.3984 0 46.4384-1.792 68.864-5.1712 21.3504-3.2256 38.144-19.456 42.7008-40.5504 14.8992-68.8128 73.1648-119.7568 143.7696-119.7568z"
            fill="#8C7BFD"></path>
          <path
            d="M511.8464 696.3712c-101.3248 0-183.7568-82.432-183.7568-183.7568s82.432-183.7568 183.7568-183.7568 183.7568 82.432 183.7568 183.7568-82.432 183.7568-183.7568 183.7568z m0-265.1648c-44.8512 0-81.3568 36.5056-81.3568 81.3568S466.9952 593.92 511.8464 593.92s81.3568-36.5056 81.3568-81.3568-36.5056-81.3568-81.3568-81.3568z"
            fill="#FFE37B"></path>
        </svg>
        统计信息
      </el-tag>
      <!-- 总览 -->
      <div>
        <div class="history-title">
          总览
          <el-button type="text" 
                     style="float: right; margin-top: -5px; color: #409EFF;"
                     @click="refreshHistoryCache"
                     :loading="refreshing"
                     title="刷新统计数据">
            <i class="el-icon-refresh"></i>
            {{ refreshing ? '刷新中...' : '刷新' }}
          </el-button>
        </div>
        <div>
          <div style="width: 400px;margin: 0 auto;display: flex;justify-content: center">
            <div class="history-name" style="line-height: 35px">总访问量（每个IP每天记一次）:</div>
            <div style="color:var(--maxLightRed);font-weight: bold;font-size: 30px;line-height: 35px">
              {{historyInfo.ip_history_count}}
            </div>
          </div>
          <div class="history-info" style="width: 640px">
            <div style="margin-right: 40px">
              <div class="history-name">省份访问TOP10</div>
              <div>
                <el-table :data="historyInfo.ip_history_province">
                  <el-table-column
                    type="index"
                    align="center"
                    width="60">
                  </el-table-column>
                  <el-table-column
                    prop="province"
                    align="center"
                    label="省份"
                    width="140">
                  </el-table-column>
                  <el-table-column
                    prop="num"
                    align="center"
                    label="数量"
                    width="100">
                  </el-table-column>
                </el-table>
              </div>
            </div>
            <div>
              <div class="history-name">IP访问TOP10</div>
              <div>
                <el-table :data="historyInfo.ip_history_ip">
                  <el-table-column
                    type="index"
                    align="center"
                    width="60">
                  </el-table-column>
                  <el-table-column
                    prop="ip"
                    align="center"
                    label="IP"
                    width="140">
                  </el-table-column>
                  <el-table-column
                    prop="num"
                    align="center"
                    label="数量"
                    width="100">
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 今日访问 -->
      <div>
        <div class="history-title">今日访问</div>
        <div>
          <div style="width: 250px;margin: 0 auto;display: flex;justify-content: center">
            <div class="history-name" style="line-height: 35px">今日访问量：</div>
            <div style="color:var(--maxLightRed);font-weight: bold;font-size: 30px;line-height: 35px">
              {{historyInfo.ip_count_today}}
            </div>
          </div>
          <div class="history-info" style="width: 720px">
            <div style="margin-right: 40px">
              <div class="history-name">今日访问省份统计</div>
              <div>
                <el-table :data="historyInfo.province_today">
                  <el-table-column
                    type="index"
                    align="center"
                    width="60">
                  </el-table-column>
                  <el-table-column
                    prop="province"
                    align="center"
                    label="省份"
                    width="140">
                  </el-table-column>
                  <el-table-column
                    prop="num"
                    align="center"
                    label="数量"
                    width="100">
                  </el-table-column>
                </el-table>
              </div>
            </div>
            <div>
              <div class="history-name">今日访问用户</div>
              <div class="history-avatar">
                <el-table :data="historyInfo.username_today">
                  <el-table-column
                    align="center"
                    label="头像"
                    width="80">
                    <template slot-scope="scope">
                      <el-avatar class="user-avatar" :size="30"
                                 :src="scope.row.avatar">
                      </el-avatar>
                    </template>
                  </el-table-column>
                  <el-table-column
                    prop="username"
                    align="center"
                    label="用户"
                    width="120">
                  </el-table-column>
                  <el-table-column
                    prop="visitCount"
                    align="center"
                    label="次数"
                    width="60">
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 昨日访问 -->
      <div>
        <div class="history-title">昨日访问</div>
        <div>
          <div style="width: 250px;margin: 0 auto;display: flex;justify-content: center">
            <div class="history-name" style="line-height: 35px">昨日访问量：</div>
            <div style="color:var(--maxLightRed);font-weight: bold;font-size: 30px;line-height: 35px">
              {{historyInfo.ip_count_yest}}
            </div>
          </div>
          <div class="history-info" style="width: 320px">
            <div>
              <div class="history-name">昨日访问用户</div>
              <div class="history-avatar">
                <el-table :data="historyInfo.username_yest">
                  <el-table-column
                    align="center"
                    label="头像"
                    width="80">
                    <template slot-scope="scope">
                      <el-avatar class="user-avatar" :size="30"
                                 :src="scope.row.avatar">
                      </el-avatar>
                    </template>
                  </el-table-column>
                  <el-table-column
                    prop="username"
                    align="center"
                    label="用户"
                    width="120">
                  </el-table-column>
                  <el-table-column
                    prop="visitCount"
                    align="center"
                    label="次数"
                    width="60">
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- 访问量历史趋势 -->
      <div style="margin-top: 40px;">
        <div class="history-title">访问量历史趋势</div>
        <visit-stats ref="visitStatsChart"></visit-stats>
      </div>

      <!-- 封禁IP列表 -->
      <div style="margin-top: 40px;">
        <div class="history-title">
          验证码封禁IP列表
          <el-button type="text" 
                     style="float: right; margin-top: -5px; color: #409EFF;"
                     @click="refreshBlockedIps"
                     :loading="loadingBlockedIps"
                     title="刷新封禁IP列表">
            <i class="el-icon-refresh"></i>
          </el-button>
        </div>
        
        <div style="max-width: 900px; margin: 0 auto;">
          <el-table 
            :data="blockedIps" 
            border 
            v-loading="loadingBlockedIps"
            style="margin-top: 10px;">
            <el-table-column type="index" label="序号" width="60" align="center"></el-table-column>
            
            <el-table-column prop="ip" label="IP地址" min-width="180" align="center">
              <template slot-scope="scope">
                <el-tag type="danger" size="small">{{ scope.row.ip }}</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="失败次数" width="100" align="center">
              <template slot-scope="scope">
                <el-tag type="warning" size="small">{{ scope.row.failCount }} 次</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="剩余时间" width="120" align="center">
              <template slot-scope="scope">
                <el-tag :type="getTimeTagType(scope.row.remainingMinutes)" size="small">
                  {{ scope.row.remainingMinutes }} 分钟
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="120" align="center">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="success"
                  @click="handleUnblock(scope.row)"
                  :loading="unblockingIps[scope.row.ip]">
                  解除封禁
                </el-button>
              </template>
            </el-table-column>
            
            <template slot="empty">
              <span style="color: #909399;">暂无数据</span>
            </template>
          </el-table>
        </div>
      </div>
      
    </div>
  </div>
</template>

<script>
import VisitStats from './visitStats.vue';

export default {
  components: {
    VisitStats
  },
  data() {
    return {
      historyInfo: {},
      loading: false,
      refreshing: false,
      isDarkMode: false,
      blockedIps: [],
      loadingBlockedIps: false,
      unblockingIps: {}
    }
  },

  computed: {},

  watch: {},

  created() {
    this.getHistoryInfo();
    this.getBlockedIps();
    // 初始化主题
    this.updateTheme();
  },

  mounted() {
    // 监听主题变化
    this.setupThemeListener();
  },

  beforeDestroy() {
    // 清理全局事件监听
    this.$root.$off('theme-changed');
    
    // 清理 storage 事件监听
    if (this.themeListener) {
      window.removeEventListener('storage', this.themeListener);
    }
  },

  methods: {
    getHistoryInfo() {
      this.$http.get(this.$constant.baseURL + "/webInfo/getHistoryInfo", {}, true)
        .then((res) => {
          if (!this.$common.isEmpty(res.data)) {
            this.historyInfo = res.data;
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message,
            type: "error"
          });
        });
    },
    
    refreshHistoryCache() {
      if (this.refreshing) return;
      
      this.refreshing = true;
      this.$http.post(this.$constant.baseURL + "/webInfo/refreshHistoryCache", {}, true)
        .then((res) => {
          if (res.success || res.code === 200) {
            this.$message({
              message: `缓存刷新成功！总访问量: ${res.data.totalCount}`,
              type: "success"
            });
            // 刷新完成后重新获取数据
            this.getHistoryInfo();
          } else {
            this.$message({
              message: res.message || '刷新失败',
              type: "error"
            });
          }
        })
        .catch((error) => {
          this.$message({
            message: error.message || '网络错误，请稍后重试',
            type: "error"
          });
        })
        .finally(() => {
          this.refreshing = false;
        });
    },
    
    // 更新主题状态
    updateTheme() {
      const theme = localStorage.getItem('theme');
      if (theme) {
        // 用户手动设置了主题
        this.isDarkMode = theme === 'dark';
      } else {
        // 用户未设置，检查 DOM 或系统偏好
        const hasDarkClass = document.body.classList.contains('dark-mode') || 
                            document.documentElement.classList.contains('dark-mode');
        if (hasDarkClass) {
          this.isDarkMode = true;
        } else {
          // 最后检查系统偏好
          const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
          this.isDarkMode = prefersDark;
        }
      }
    },
    
    // 监听主题变化
    setupThemeListener() {
      // 监听全局主题变化事件（由父组件 admin.vue 触发）
      this.$root.$on('theme-changed', (isDark) => {
        this.isDarkMode = isDark;
      });
      
      // 监听 storage 事件（跨标签页）
      this.themeListener = (e) => {
        if (e.key === 'theme') {
          this.updateTheme();
        }
      };
      window.addEventListener('storage', this.themeListener);
    },
    
    // 获取封禁IP列表
    getBlockedIps() {
      this.loadingBlockedIps = true;
      
      this.$http.get(this.$constant.baseURL + "/captcha/getBlockedIps", {}, true)
        .then(res => {
          this.blockedIps = res.data || [];
        })
        .catch(error => {
          console.error('获取封禁IP列表失败:', error);
        })
        .finally(() => {
          this.loadingBlockedIps = false;
        });
    },
    
    // 刷新封禁IP列表
    refreshBlockedIps() {
      this.getBlockedIps();
    },
    
    // 解除IP封禁
    handleUnblock(row) {
      this.$confirm(`确定要解除IP "${row.ip}" 的封禁吗？`, '确认操作', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$set(this.unblockingIps, row.ip, true);
        
        this.$http.post(this.$constant.baseURL + "/captcha/unblockIp", { ip: row.ip }, true)
          .then(res => {
            this.$message({
              message: '解除封禁成功！',
              type: 'success'
            });
            // 从列表中移除
            this.blockedIps = this.blockedIps.filter(item => item.ip !== row.ip);
          })
          .catch(error => {
            this.$message({
              message: '解除封禁失败: ' + error.message,
              type: 'error'
            });
          })
          .finally(() => {
            this.$delete(this.unblockingIps, row.ip);
          });
      }).catch(() => {
        // 取消操作
      });
    },
    
    // 根据剩余时间获取标签类型
    getTimeTagType(minutes) {
      if (minutes > 20) return 'danger';
      if (minutes > 10) return 'warning';
      return 'info';
    }
  }
}
</script>

<style scoped>

  .my-tag {
    width: 100%;
    text-align: left;
    background: var(--lightYellow);
    border: none;
    height: 40px;
    line-height: 40px;
    font-size: 16px;
    color: var(--black);
  }

  .el-tag {
    margin: 10px;
  }

  .history-title {
    margin: 15px auto 15px;
    width: 120px;
    text-align: center;
    padding: 10px 20px;
    background: var(--lightGreen);
    color: var(--white);
    font-weight: bold;
    border-radius: 5px;
  }

  .history-name {
    font-size: 18px;
    font-weight: bold;
    margin: 0 10px 10px 0;
    text-align: center;
  }

  .history-info {
    display: flex;
    text-align: center;
    margin: 20px auto 0;
  }

  .history-info >>> .el-table .cell {
    line-height: unset;
  }

  .history-avatar >>> .el-table .el-table__row .el-table__cell {
    padding: 3.5px 0;
  }

  .history-info >>> .el-table::before {
    height: unset;
  }

  /* ========== 深色模式适配 ========== */
  .main-dark-mode .my-tag {
    background: var(--lightYellow);
    color: var(--black);
  }

  .main-dark-mode .history-title {
    background: var(--lightGreen);
    color: var(--white);
  }

  /* 所有标题文字 */
  .main-dark-mode .history-name {
    color: rgba(255, 255, 255, 0.9) !important;
  }

  /* 表格样式 */
  .main-dark-mode >>> .el-table {
    background-color: transparent !important;
    color: rgba(255, 255, 255, 0.9) !important;
  }

  .main-dark-mode >>> .el-table th,
  .main-dark-mode >>> .el-table tr,
  .main-dark-mode >>> .el-table td,
  .main-dark-mode >>> .el-table__body-wrapper .el-table__body .el-table__row .cell {
    background-color: rgba(255, 255, 255, 0.05) !important;
    color: rgba(255, 255, 255, 0.9) !important;
    border-color: rgba(255, 255, 255, 0.1) !important;
  }

  .main-dark-mode >>> .el-table--enable-row-hover .el-table__body tr:hover > td {
    background-color: rgba(255, 255, 255, 0.1) !important;
  }

  .main-dark-mode >>> .el-table th.el-table__cell {
    background-color: rgba(255, 255, 255, 0.08) !important;
    color: rgba(255, 255, 255, 0.9) !important;
  }

  .main-dark-mode >>> .el-table .cell {
    color: rgba(255, 255, 255, 0.9) !important;
  }

  /* 加载动画 */
  .main-dark-mode >>> .el-loading-mask {
    background-color: rgba(0, 0, 0, 0.8) !important;
  }

  .main-dark-mode >>> .el-loading-spinner .el-icon-loading {
    color: var(--lightGreen) !important;
  }

  .main-dark-mode >>> .el-loading-spinner .el-loading-text {
    color: rgba(255, 255, 255, 0.9) !important;
  }

  /* 刷新按钮 */
  .main-dark-mode >>> .el-button--text {
    color: #409EFF !important;
  }

</style>
