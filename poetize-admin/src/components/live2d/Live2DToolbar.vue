<template>
  <div id="waifu-tool" class="waifu-tool">
    <!-- 聊天按钮 -->
    <span
      class="waifu-tool-icon fa fa-lg fa-comment"
      title="打开聊天窗口"
      @click="$emit('chat')"
    />
    
    <!-- 换人按钮 -->
    <span
      class="waifu-tool-icon fa fa-lg fa-street-view"
      title="换人"
      @click="$emit('change-model')"
    />
    
    <!-- 换衣服按钮 -->
    <span
      class="waifu-tool-icon"
      title="换衣服"
      @click="$emit('change-texture')"
    >
      <svg class="tool-svg-icon" viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
        <path d="M363.0848 71.552c10.24-4.864 21.76-7.552 33.92-7.552h11.904l93.184 38.4h20.736L616.0128 64h11.904c12.288 0 23.872 2.752 34.112 7.68 5.952-2.432 11.84-4.992 17.6-7.68h84.864c7.68 0 15.104 0.96 22.144 2.688 22.208-0.384 44.608 7.296 62.592 23.04l143.68 126.336c38.528 33.792 42.24 92.48 8.448 131.2l-51.712 58.88c-23.68 27.072-59.712 36.928-92.288 28.8v430.72c0 51.2-41.6 92.8-92.864 92.8H260.6208a92.8 92.8 0 0 1-92.864-92.8V434.816a92.992 92.992 0 0 1-92.416-28.8l-51.776-58.944a92.8 92.8 0 0 1 8.512-131.072l143.68-126.272c17.984-15.936 40.384-23.488 62.72-23.104A92.8 92.8 0 0 1 260.5568 64h85.184c5.696 2.624 11.456 5.12 17.28 7.552zM321.1008 128h-60.416a28.8 28.8 0 0 0-6.912 0.832l-8 1.92-8.32-0.064a28.16 28.16 0 0 0-19.392 7.168L74.3168 264.128a28.8 28.8 0 0 0-2.624 40.768l51.712 58.88c7.232 8.256 18.368 11.584 28.864 8.96l79.488-19.776v512.64c0 16 12.864 28.864 28.8 28.864h503.936a28.8 28.8 0 0 0 28.8-28.8v-512.64l79.488 19.84a28.8 28.8 0 0 0 28.8-8.96l51.584-58.816a29.056 29.056 0 0 0-2.56-40.96l-143.616-126.272a28.48 28.48 0 0 0-19.328-7.168l-8.32 0.128-8-1.984A28.8 28.8 0 0 0 764.4928 128h-60.672c0.384 2.88 0.64 5.76 0.64 8.832v110.08c0 19.392-8.064 38.016-22.4 51.712A78.4 78.4 0 0 1 627.9168 320h-11.968l-93.184-38.592H502.2208L408.9728 320h-11.968C354.7648 320 320.4608 287.296 320.4608 246.976V136.064c0-2.752 0.256-5.44 0.64-8.064z m208.384 29.568h-31.488a32.64 32.64 0 0 0-6.4 0.64l-94.528-39.04c-10.24 0-18.56 7.872-18.752 17.664v110.08c0 9.92 8.384 17.92 18.752 17.92l94.592-39.168c1.92 0.384 4.032 0.576 6.336 0.576h31.488a35.2 35.2 0 0 0 4.608-0.32l93.888 38.912a19.2 19.2 0 0 0 13.248-5.248 17.472 17.472 0 0 0 5.504-12.608V136.832a18.304 18.304 0 0 0-18.752-17.664l-93.824 38.72a35.2 35.2 0 0 0-4.672-0.32z" fill="currentColor" />
      </svg>
    </span>
    
    <!-- 鼠标动画按钮 -->
    <span
      class="waifu-tool-icon fa fa-lg fa-mouse-pointer"
      title="鼠标动画"
      @click="$emit('toggle-mouse-animation')"
    />
    
    <!-- 拍照按钮（可选） -->
    <span
      v-if="enablePhoto"
      class="waifu-tool-icon fa fa-lg fa-camera-retro"
      title="拍照"
      @click="$emit('photo')"
    />
    
    <!-- 信息按钮（可选） -->
    <span
      v-if="enableInfo"
      class="waifu-tool-icon fa fa-lg fa-info-circle"
      title="关于"
      @click="$emit('info')"
    />
    
    <!-- 关闭按钮 -->
    <span
      class="waifu-tool-icon fa fa-lg fa-times"
      title="关闭看板娘"
      @click="$emit('close')"
    />
  </div>
</template>

<script>
export default {
  name: 'Live2DToolbar',
  
  props: {
    enablePhoto: {
      type: Boolean,
      default: false
    },
    enableInfo: {
      type: Boolean,
      default: false
    }
  },
  
  emits: ['chat', 'change-model', 'change-texture', 'toggle-mouse-animation', 'photo', 'info', 'close']
}
</script>

<style scoped>
.waifu-tool {
  position: absolute;
  bottom: 20px;
  right: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 11;
}

.waifu-tool-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  color: #5b6c7d;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 16px;
}

.waifu-tool-icon:hover {
  transform: scale(1.1);
}

.waifu-tool-icon:active {
  transform: scale(0.95);
}

/* SVG图标样式 */
.tool-svg-icon {
  width: 18px !important;
  height: 18px !important;
  fill: currentColor;
  position: relative;
  top: -3px;
}

/* 动画效果 */
.waifu-tool-icon {
  animation: toolIconPop 0.5s ease;
}

@keyframes toolIconPop {
  0% {
    opacity: 0;
    transform: scale(0);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .waifu-tool {
    bottom: 10px;
    right: 5px;
  }
  
  .waifu-tool-icon {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }
}
</style>
