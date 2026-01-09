<template>
  <div>
    <div style="margin-bottom: 20px">
      <el-select v-if="isBoss" v-model="pagination.commentType" placeholder="评论来源类型" style="margin-right: 10px">
        <el-option key="1" label="文章评论" value="article"></el-option>
        <el-option key="2" label="树洞留言" value="message"></el-option>
        <el-option key="3" label="家庭祝福" value="love"></el-option>
      </el-select>
      <el-input class="my-input" type="number" style="width: 140px;margin-right: 10px" v-model="pagination.source"
                placeholder="评论来源标识"></el-input>
      <el-button type="primary" icon="el-icon-search" @click="searchComments()">搜索</el-button>
      <el-button type="danger" @click="clearSearch()">清除参数</el-button>
    </div>
    <el-table :data="comments" border class="table" header-cell-class-name="table-header">
      <el-table-column prop="id" label="ID" width="55" align="center"></el-table-column>
      <el-table-column prop="source" label="评论来源标识" align="center"></el-table-column>
      <el-table-column prop="type" label="评论来源类型" align="center"></el-table-column>
      <el-table-column prop="userId" label="发表用户ID" align="center"></el-table-column>
      <el-table-column prop="likeCount" label="点赞数" align="center"></el-table-column>
      <el-table-column prop="commentContent" label="评论内容" align="center"></el-table-column>
      <el-table-column prop="commentInfo" label="评论额外信息" align="center"></el-table-column>
      <el-table-column prop="createTime" label="创建时间" align="center"></el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template slot-scope="scope">
          <el-button type="text" icon="el-icon-delete" style="color: var(--orangeRed)" @click="handleDelete(scope.row)">
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
</template>

<script>

    import { useMainStore } from '@/stores/main';

export default {
    data() {
      return {
        pagination: {
          current: 1,
          size: 10,
          total: 0,
          source: null,
          commentType: ""
        },
        comments: []
      }
    },

    computed: {
      mainStore() {
        return useMainStore();
      },
      // 使用computed属性确保isBoss值能响应Store变化
      isBoss() {
        return this.mainStore.currentAdmin.isBoss;
      }
    },

    watch: {},

    created() {
      this.getComments();
    },

    mounted() {
    },

    methods: {
      clearSearch() {
        this.pagination = {
          current: 1,
          size: 10,
          total: 0,
          source: null,
          commentType: ""
        }
        this.getComments();
      },
      getComments() {
        let url = "";
        if (this.isBoss) {
          url = "/admin/comment/boss/list";
        } else {
          url = "/admin/comment/user/list";
        }
        this.$http.post(this.$constant.baseURL + url, this.pagination, true)
          .then((res) => {
            if (!this.$common.isEmpty(res.data)) {
              this.comments = res.data.records;
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
      handlePageChange(val) {
        this.pagination.current = val;
        this.getComments();
      },
      searchComments() {
        this.pagination.total = 0;
        this.pagination.current = 1;
        this.getComments();
      },
      handleDelete(item) {
        let url = "";
        if (this.isBoss) {
          url = "/admin/comment/boss/deleteComment";
        } else {
          url = "/admin/comment/user/deleteComment";
        }
        this.$confirm('删除评论后，所有该评论的回复均不可见。确认删除？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success',
          center: true,
          customClass: 'mobile-responsive-confirm'
        }).then(() => {
          this.$http.get(this.$constant.baseURL + url, {id: item.id}, true)
            .then((res) => {
              this.pagination.current = 1;
              this.getComments();
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
      }
    }
  }
</script>

<style scoped>

  .pagination {
    margin: 20px 0;
    text-align: right;
  }

  .my-input >>> input::-webkit-inner-spin-button {
    appearance: none;
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
