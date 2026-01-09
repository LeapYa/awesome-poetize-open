<template>
  <div>
    <div>
      <div class="handle-box">
        <el-select clearable v-model="pagination.status" placeholder="状态" class="handle-select mrb10">
          <el-option key="1" label="启用" :value="true"></el-option>
          <el-option key="2" label="禁用" :value="false"></el-option>
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="search()">搜索</el-button>
      </div>
      <el-table :data="loves" border class="table" header-cell-class-name="table-header">
        <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>
        <el-table-column prop="userId" label="用户ID" align="center"></el-table-column>

        <el-table-column prop="manName" label="男生昵称" align="center"></el-table-column>
        <el-table-column prop="womanName" label="女生昵称" align="center"></el-table-column>

        <el-table-column label="背景封面" align="center">
          <template slot-scope="scope">
            <el-image lazy :preview-src-list="[scope.row.bgCover]" class="table-td-thumb" :src="scope.row.bgCover"
                      fit="cover"></el-image>
          </template>
        </el-table-column>
        <el-table-column label="男生头像" align="center">
          <template slot-scope="scope">
            <el-image lazy :preview-src-list="[scope.row.manCover]" class="table-td-thumb" :src="scope.row.manCover"
                      fit="cover"></el-image>
          </template>
        </el-table-column>
        <el-table-column label="女生头像" align="center">
          <template slot-scope="scope">
            <el-image lazy :preview-src-list="[scope.row.womanCover]" class="table-td-thumb" :src="scope.row.womanCover"
                      fit="cover"></el-image>
          </template>
        </el-table-column>

        <el-table-column label="状态" align="center">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === false ? 'danger' : 'success'"
                    disable-transitions>
              {{scope.row.status === false ? '禁用' : '启用'}}
            </el-tag>
            <el-switch @click.native="changeStatus(scope.row)" v-model="scope.row.status"></el-switch>
          </template>
        </el-table-column>

        <el-table-column prop="timing" label="计时" align="center"></el-table-column>
        <el-table-column prop="countdownTitle" label="倒计时标题" align="center"></el-table-column>
        <el-table-column prop="countdownTime" label="倒计时时间" align="center"></el-table-column>
        <el-table-column prop="familyInfo" label="额外信息" align="center" show-overflow-tooltip></el-table-column>
        <el-table-column label="创建时间" align="center">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="最终修改时间" align="center">
          <template slot-scope="scope">
            {{ formatDateTime(scope.row.updateTime) }}
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
      <div class="pagination">
        <el-pagination background layout="total, prev, pager, next"
                       :current-page="pagination.current"
                       :page-size="pagination.size"
                       :total="pagination.total"
                       @current-change="handlePageChange">
        </el-pagination>
      </div>
    </div>
  </div>
</template>

<script>

  export default {
    components: {},
    data() {
      return {
        pagination: {
          current: 1,
          size: 10,
          total: 0,
          status: null
        },
        loves: []
      }
    },

    computed: {},

    watch: {},

    created() {
      this.getLoves();
    },

    mounted() {
    },

    methods: {
      handleDelete(item) {
        this.$confirm('确认删除资源？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success',
          center: true,
          customClass: 'mobile-responsive-confirm'
        }).then(() => {
          this.$http.get(this.$constant.baseURL + "/family/deleteFamily", {id: item.id}, true)
            .then((res) => {
              this.pagination.current = 1;
              this.getLoves();
              this.$message({
                message: "删除成功！",
                type: "success"
              });
            })
            .catch((error) => {
              this.$message({
                message: error.message,
                type: "error"
              });
            });
        }).catch(() => {
          this.$message({
            type: 'success',
            message: '已取消删除!'
          });
        });
      },
      search() {
        this.pagination.total = 0;
        this.pagination.current = 1;
        this.getLoves();
      },
      getLoves() {
        this.$http.post(this.$constant.baseURL + "/family/listFamily", this.pagination, true)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.loves = res.data.records;
              this.pagination.total = res.data.total;
            }
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
      changeStatus(item) {
        this.$http.get(this.$constant.baseURL + "/family/changeLoveStatus", {
          id: item.id,
          flag: item.status
        }, true)
          .then((res) => {
            this.$message({
              message: "修改成功！",
              type: "success"
            });
          })
          .catch((error) => {
            this.$message({
              message: error.message,
              type: "error"
            });
          });
      },
      handlePageChange(val) {
        this.pagination.current = val;
        this.getLoves();
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
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      }
    }
  }
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

    ::v-deep .el-textarea__inner {
      font-size: 16px !important;
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
