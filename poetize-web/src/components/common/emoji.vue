<template>
  <div>
    <transition name="body">
      <div v-show="showEmoji">
        <span
          class="emoji-item"
          v-for="(value, key, index) in emojiListURL"
          :key="index"
          @click="addEmoji(key)"
        >
          <img
            loading="lazy"
            class="emoji"
            :src="value"
            :title="key"
            width="24px"
            height="24px"
          />
        </span>
      </div>
    </transition>
  </div>
</template>

<script>
import { $on, $off, $once, $emit } from '../../utils/gogocodeTransfer'
import { useMainStore } from '@/stores/main'

export default {
  computed: {
    mainStore() {
      return useMainStore()
    },
  },
  props: {
    showEmoji: {
      type: Boolean,
    },
  },
  data() {
    return {
      emojiList: this.$constant.emojiList,
      emojiListURL: {},
    }
  },
  created() {
    this.emojiListURL = this.getEmojiList(this.emojiList)
  },
  methods: {
    addEmoji(key) {
      $emit(this, 'addEmoji', key)
    },
    getEmojiList(emojiList) {
      let emojiName
      let url
      let result = {}
      for (let i = 0; i < emojiList.length; i++) {
        emojiName = '[' + emojiList[i] + ']'
        let j = i + 1
        url =
          this.mainStore.sysConfig['webStaticResourcePrefix'] +
          'emoji/q' +
          j +
          '.gif'
        result[emojiName] = url
      }
      return result
    },
  },
  emits: ['addEmoji'],
}
</script>

<style scoped>
.emoji-item {
  cursor: pointer;
  display: inline-block;
}
.emoji-item:hover {
  transition: background-color 0.2s ease;
  border-radius: 0.25rem;
  background: var(--lightGray);
}
.emoji {
  margin: 0.25rem;
  vertical-align: middle;
}
.body-enter-active,
.body-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
  will-change: transform, opacity;
  transform: translateZ(0);
}
.body-enter-from,
.body-leave-to {
  opacity: 0;
  transform: scale(0.5);
}
</style>
