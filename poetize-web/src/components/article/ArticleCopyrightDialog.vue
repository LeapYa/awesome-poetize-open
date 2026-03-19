<template>
  <el-dialog
    title="版权声明"
    :model-value="modelValue"
    width="80%"
    :append-to-body="true"
    class="article-copy centered-dialog"
    :close-on-click-modal="false"
    center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div style="display: flex; align-items: center; flex-direction: column">
      <el-avatar shape="square" :size="35" :src="avatarUrl">
        <img :src="defaultAvatar" />
      </el-avatar>
      <div class="copyright-container">
        <p>
          {{ siteName }}是指运行在{{ host }}域名及相关子域名上的网站，本条款描述了{{
            siteName
          }}的网站版权声明：
        </p>
        <ul>
          <li>
            {{ siteName }}提供的所有文章、展示的图片素材等内容部分来源于互联网平台，仅供学习参考。如有侵犯您的版权，请联系{{
              siteName
            }}负责人，{{ siteName }}承诺将在一个工作日内改正。
          </li>
          <li>
            {{ siteName }}不保证网站内容的全部准确性、安全性和完整性，请您在阅读、下载及使用过程中自行确认，{{
              siteName
            }}亦不承担上述资源对您造成的任何形式的损失或伤害。
          </li>
          <li>未经{{ siteName }}允许，不得盗链、盗用本站内容和资源。</li>
          <li>
            {{ siteName }}旨在为广大用户提供更多的信息；{{ siteName
            }}不保证向用户提供的外部链接的准确性和完整性，该外部链接指向的不由本站实际控制的任何网页上的内容，{{
              siteName
            }}对其合法性亦概不负责，亦不承担任何法律责任。
          </li>
          <li>
            {{ siteName }}中的文章/视频（包括转载文章/视频）的版权仅归原作者所有，若作者有版权声明或文章从其它网站转载而附带有原所有站的版权声明者，其版权归属以附带声明为准；文章仅代表作者本人的观点，与{{
              siteName
            }}立场无关。
          </li>
          <li>
            {{ siteName }}自行编写排版的文章均采用
            <a
              href="https://creativecommons.org/licenses/by-nc-sa/4.0/"
              style="color: #38f; text-decoration: none"
            >
              知识共享署名-非商业性使用-相同方式共享 4.0 国际许可协议
            </a>
          </li>
          <li>
            许可协议标识：
            <a href="https://creativecommons.org/licenses/by-nc-sa/4.0/">
              <img
                alt="知识共享许可协议"
                src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png"
                style="margin-top: 5px"
              />
            </a>
          </li>
        </ul>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  avatarUrl: {
    type: String,
    default: '',
  },
  defaultAvatar: {
    type: String,
    default: '',
  },
  webInfo: {
    type: Object,
    default: () => ({}),
  },
  host: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const siteName = computed(() => {
  return props.webInfo?.webTitle || props.webInfo?.webName || ''
})
</script>
