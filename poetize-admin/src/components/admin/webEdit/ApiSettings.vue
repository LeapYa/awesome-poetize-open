<template>
  <div id="field-api">
    <SectionTag>API 配置</SectionTag>

    <el-card class="box-card" shadow="never" style="margin-top: 5px; border: none;">
      <!-- API开关 -->
      <div style="margin-bottom: 20px;">
        <el-form :model="apiConfig" label-width="120px">
          <el-form-item label="启用API">
            <el-switch
              v-model="apiConfig.enabled"
              @change="handleApiToggle"
              active-color="#13ce66"
              inactive-color="#ff4949">
            </el-switch>
          </el-form-item>

          <el-form-item v-if="apiConfig.enabled" label="API密钥">
            <div style="display: flex; align-items: center;">
              <el-input
                v-model="apiConfig.apiKey"
                placeholder="API密钥"
                :disabled="true"
                style="width: 350px;">
              </el-input>
              <el-button
                type="primary"
                size="small"
                style="margin-left: 10px;"
                @click="regenerateApiKey">
                重新生成
              </el-button>
            </div>
          </el-form-item>

          <el-form-item v-if="apiConfig.enabled" label="API端点">
            <div>
              <p style="margin: 5px 0; color: #606266;">文章创建API:</p>
              <el-input
                :value="$constant.baseURL + '/api/article/create'"
                :disabled="true"
                style="width: 450px; margin-bottom: 10px;">
              </el-input>
            </div>
          </el-form-item>

          <el-form-item v-if="apiConfig.enabled" label="API文档">
            <el-collapse>
              <el-collapse-item title="API调用概述" name="0">
                <div style="padding: 10px;">
                  <p><strong>API认证:</strong></p>
                  <p>所有API请求都需要在请求头中添加<code>X-API-KEY</code>字段，值为API密钥。</p>
                  <p><strong>响应格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,       // 0表示成功，非0表示错误
  "message": null, // 错误信息，成功时为null
  "data": { ... }  // 响应数据
}
                  </pre>
                </div>
              </el-collapse-item>
              <el-collapse-item title="创建文章 API" name="1">
                <div style="padding: 10px;">
                  <p><strong>请求格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
POST {{$constant.baseURL}}/api/article/create
Content-Type: application/json
X-API-KEY: {{apiConfig.apiKey}}

{
  "title": "文章标题",
  "content": "文章内容，支持Markdown格式",
  "cover": "封面图片URL(可选)",
  "sortName": "分类名称(将自动创建不存在的分类)",
  "labelName": "标签名称(将自动创建不存在的标签)",
  "summary": "文章摘要(可选)",
  "password": "文章密码(可选)",
  "viewStatus": true,
  "commentStatus": true,
  "submitToSearchEngine": true
}
                  </pre>
                  <p><strong>响应格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": {
    "id": 123
  }
}
                  </pre>
                </div>
              </el-collapse-item>
              <el-collapse-item title="获取文章列表 API" name="2">
                <div style="padding: 10px;">
                  <p><strong>请求格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/article/list?current=1&amp;size=10&amp;sortId=1&amp;labelId=1&amp;searchKey=关键词
X-API-KEY: {{apiConfig.apiKey}}
                  </pre>
                  <p><strong>参数说明:</strong></p>
                  <ul>
                    <li><code>current</code>: 当前页码，从1开始，默认为1</li>
                    <li><code>size</code>: 每页大小，默认为10</li>
                    <li><code>sortId</code>: 分类ID，可选</li>
                    <li><code>labelId</code>: 标签ID，可选</li>
                    <li><code>searchKey</code>: 搜索关键词，可选</li>
                  </ul>
                  <p><strong>响应格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": {
    "records": [
      {
        "id": 123,
        "articleTitle": "文章标题",
        "articleContent": "文章内容摘要...",
        "articleCover": "图片URL",
        "viewCount": 100,
        "commentCount": 5,
        "createTime": "2023-04-01 12:00:00",
        "sort": { "id": 1, "sortName": "分类名称" },
        "label": { "id": 1, "labelName": "标签名称" }
      }
    ],
    "current": 1,
    "size": 10,
    "total": 42,
    "pages": 5
  }
}
                  </pre>
                </div>
              </el-collapse-item>
              <el-collapse-item title="获取文章详情 API" name="3">
                <div style="padding: 10px;">
                  <p><strong>请求格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/article/123
X-API-KEY: {{apiConfig.apiKey}}
                  </pre>
                  <p><strong>响应格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": {
    "id": 123,
    "articleTitle": "文章标题",
    "articleContent": "完整文章内容，包括Markdown格式",
    "articleCover": "图片URL",
    "viewCount": 100,
    "commentStatus": true,
    "recommendStatus": false,
    "viewStatus": true,
    "createTime": "2023-04-01 12:00:00",
    "updateTime": "2023-04-02 14:30:00",
    "sortId": 1,
    "labelId": 1,
    "sortName": "分类名称",
    "labelName": "标签名称"
  }
}
                  </pre>
                </div>
              </el-collapse-item>
              <el-collapse-item title="获取分类列表 API" name="4">
                <div style="padding: 10px;">
                  <p><strong>请求格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/categories
X-API-KEY: {{apiConfig.apiKey}}
                  </pre>
                  <p><strong>响应格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": [
    {
      "id": 1,
      "sortName": "技术文章",
      "sortDescription": "技术类文章",
      "sortType": 0,
      "priority": 1
    }
  ]
}
                  </pre>
                </div>
              </el-collapse-item>
              <el-collapse-item title="获取标签列表 API" name="5">
                <div style="padding: 10px;">
                  <p><strong>请求格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
GET {{$constant.baseURL}}/api/tags
X-API-KEY: {{apiConfig.apiKey}}
                  </pre>
                  <p><strong>响应格式:</strong></p>
                  <pre style="background-color: #f5f7fa; padding: 10px; border-radius: 4px; overflow: auto;">
{
  "code": 0,
  "message": null,
  "data": [
    {
      "id": 1,
      "sortId": 1,
      "labelName": "Java",
      "labelDescription": "Java编程语言"
    }
  ]
}
                  </pre>
                </div>
              </el-collapse-item>
            </el-collapse>
          </el-form-item>

          <el-form-item v-if="apiConfig.enabled">
            <div style="display: flex; justify-content: flex-end; margin-top: 20px;"></div>
          </el-form-item>
        </el-form>
      </div>

      <div style="margin-top: 20px; margin-bottom: 22px; text-align: center;">
        <el-button type="primary" @click="saveApiConfig" :loading="apiLoading">保存API配置</el-button>
        <el-button @click="resetApiConfig" style="margin-left: 10px;">重置修改</el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import SectionTag from './SectionTag.vue';

export default {
  name: 'ApiSettings',
  components: { SectionTag },
  data() {
    return {
      apiConfig: {
        enabled: false,
        apiKey: ''
      },
      apiLoading: false
    }
  },
  created() {
    this.getApiConfig();
  },
  methods: {
    async getApiConfig() {
      try {
        const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getApiConfig", true);
        if (res.data) {
          this.apiConfig = res.data;
        } else {
          this.apiConfig = { enabled: false, apiKey: '' };
        }
      } catch (error) {
        this.$message.error("获取API配置失败: " + error.message);
        throw error;
      }
    },
    handleApiToggle(value) {
      this.apiConfig.enabled = value;
    },
    regenerateApiKey() {
      this.apiLoading = true;
      this.$http.post(this.$constant.baseURL + "/webInfo/regenerateApiKey", {}, true)
        .then((res) => {
          this.apiConfig.apiKey = res.data;
          this.$message({ message: "API密钥已重新生成", type: "success" });
        })
        .catch((error) => {
          this.$message({ message: "重新生成API密钥失败: " + error.message, type: "error" });
        })
        .finally(() => {
          this.apiLoading = false;
        });
    },
    saveApiConfig() {
      this.apiLoading = true;
      this.$http.post(this.$constant.baseURL + "/webInfo/saveApiConfig", this.apiConfig, true)
        .then(() => {
          this.$message({ message: "API配置保存成功", type: "success" });
        })
        .catch((error) => {
          this.$message({ message: "保存API配置失败: " + error.message, type: "error" });
        })
        .finally(() => {
          this.apiLoading = false;
        });
    },
    resetApiConfig() {
      this.getApiConfig();
    }
  }
}
</script>

<style scoped>
pre {
  white-space: pre-wrap;
  word-break: break-word;
}

@media screen and (max-width: 500px) {
  .el-input {
    width: 100% !important;
  }
}
</style>
