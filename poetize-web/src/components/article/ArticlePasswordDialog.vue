<template>
  <el-dialog
    title="密码"
    :modal="false"
    :model-value="modelValue"
    width="25%"
    :append-to-body="true"
    :close-on-click-modal="false"
    destroy-on-close
    center
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div>
      <div>
        <div class="password-content">{{ tips }}</div>
      </div>
      <div style="margin: 20px auto">
        <el-input
          maxlength="30"
          :model-value="password"
          @update:model-value="emit('update:password', $event)"
        ></el-input>
      </div>
      <div style="display: flex; justify-content: center">
        <proButton
          :info="'提交'"
          :before="beforeColor"
          :after="afterColor"
          @click="emit('submit')"
        >
        </proButton>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  tips: {
    type: String,
    default: '',
  },
  password: {
    type: String,
    default: '',
  },
  beforeColor: {
    type: String,
    default: '',
  },
  afterColor: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'update:password', 'submit'])

const proButton = defineAsyncComponent(() => import('../common/proButton'))
</script>
