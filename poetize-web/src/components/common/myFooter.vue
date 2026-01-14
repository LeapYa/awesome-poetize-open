<template>
  <div class="myFooter-wrap" v-show="showFooter">
    <div
      class="myFooter"
      :class="{ 'has-bg-image': hasBgImage, minimal: isMinimalFooter }"
      :style="footerStyle"
    >
      <div
        class="footer-title font"
        :style="textStyle"
        v-if="!isMinimalFooter && mainStore.webInfo.footer"
      >
        {{ mainStore.webInfo.footer }}
      </div>
      <div
        class="icp font"
        :style="textStyle"
        v-if="mainStore.sysConfig.beian || mainStore.sysConfig.policeBeian"
      >
        <a
          href="http://beian.miit.gov.cn/"
          target="_blank"
          v-if="mainStore.sysConfig.beian"
          >{{ mainStore.sysConfig.beian }}</a
        >
        <a
          href="http://www.beian.gov.cn/portal/registerSystemInfo"
          target="_blank"
          v-if="mainStore.sysConfig.policeBeian"
        >
          <img
            src="/static/assets/gonganbei.svg"
            alt="公安备案"
            style="margin-left: 10px; width: 14px; height: 14px"
          />
          {{ mainStore.sysConfig.policeBeian }}
        </a>
      </div>
      <div class="copyright font" :style="textStyle">
        <span class="copyright-left"
          >© {{ currentYear }} {{ mainStore.webInfo.webName }}</span
        >
        <span class="copyright-center">保留所有权利</span>
        <span class="copyright-right"
          ><a href="/privacy" class="policy-link">隐私政策</a></span
        >
      </div>
      <div class="contact font" :style="textStyle" v-if="!isMinimalFooter">
        本站内容均为原创或合法转载，如有侵权请通过邮箱：{{
          mainStore.webInfo.email || 'admin@poetize.cn'
        }}
        与我们联系，确认后将立即删除
      </div>
    </div>
  </div>
</template>

<script>
import { useMainStore } from '@/stores/main'

export default {
  props: {
    showFooter: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      // 当前视口宽度，用于判断是否移动端
      viewportWidth: typeof window !== 'undefined' ? window.innerWidth : 1920,
    }
  },
  computed: {
    mainStore() {
      return useMainStore()
    },
    currentYear() {
      return new Date().getFullYear()
    },
    hasBgImage() {
      const img = this.mainStore.webInfo.footerBackgroundImage
      if (!img) return false
      const val = String(img).trim().toLowerCase()
      return val !== '' && val !== 'null' && val !== 'undefined'
    },
    isMinimalFooter() {
      const flag =
        this.mainStore.webInfo && this.mainStore.webInfo.minimalFooter
      if (flag === true) return true
      if (typeof flag === 'string') {
        return flag.toLowerCase() === 'true'
      }
      return false
    },
    footerStyle() {
      const webInfo = this.mainStore.webInfo
      // 基础公共样式
      const baseStyle = {
        borderRadius: '1.5rem 1.5rem 0 0',
        textAlign: 'center',
        color: 'var(--white)',
        // 根据设备宽度动态调整高度：移动端更紧凑
        minHeight: (() => {
          const isMobile = this.viewportWidth <= 768
          const hasFooterTitle =
            this.mainStore.webInfo && this.mainStore.webInfo.footer
          const hasBeianInfo =
            this.mainStore.sysConfig &&
            (this.mainStore.sysConfig.beian ||
              this.mainStore.sysConfig.policeBeian)

          if (this.isMinimalFooter) {
            if (hasBeianInfo) {
              return isMobile ? '70px' : '90px'
            }
            return isMobile ? '50px' : '70px'
          }

          let baseHeight
          // 根据是否有 footer 标题来调整基础高度
          if (hasFooterTitle) {
            baseHeight = isMobile ? 120 : 150
          } else {
            baseHeight = isMobile ? 105 : 135
          }

          // 如果没有备案信息，再减少30px
          if (!hasBeianInfo) {
            baseHeight -= 30
          }

          return `${baseHeight}px`
        })(),
      }

      // 1. 存在背景图 → 关闭渐变动画，使用伪元素展示图片
      if (webInfo.footerBackgroundImage) {
        const style = {
          ...baseStyle,
          background: 'transparent',
          animation: 'none',
          // 供 ::after / ::before 使用的变量
          '--footer-bg-image': `url(${webInfo.footerBackgroundImage})`,
        }

        // 解析后台配置
        let bgConfig = {
          backgroundSize: 'cover',
          backgroundPosition: 'center center',
          backgroundRepeat: 'no-repeat',
          opacity: 100,
        }
        if (webInfo.footerBackgroundConfig) {
          try {
            bgConfig = {
              ...bgConfig,
              ...JSON.parse(webInfo.footerBackgroundConfig),
            }
          } catch (e) {
            console.error('解析页脚背景配置失败:', e)
          }
        }

        style['--footer-bg-size'] = bgConfig.backgroundSize
        style['--footer-bg-position'] = bgConfig.backgroundPosition
        style['--footer-bg-repeat'] = bgConfig.backgroundRepeat

        // 遮罩颜色
        if (bgConfig.maskColor) {
          style['--footer-mask-color'] = bgConfig.maskColor
        } else {
          const maskOpacity = (100 - (bgConfig.opacity || 50)) / 100
          style['--footer-mask-color'] = `rgba(0, 0, 0, ${maskOpacity})`
        }
        return style
      }

      // 2. 无背景图 → 使用渐变动画
      return {
        ...baseStyle,
        background: 'var(--gradientBG)',
        backgroundSize: '300% 300%',
        animation: 'gradientBG 10s ease infinite',
      }
    },
    textStyle() {
      const webInfo = this.mainStore.webInfo
      let style = {
        color: 'var(--white)',
        position: 'relative',
        zIndex: 10,
      }

      // 如果有背景图片，设置文字颜色和阴影
      if (webInfo.footerBackgroundImage) {
        let bgConfig = {
          textColor: '#ffffff',
          textShadow: false,
        }

        // 解析页脚背景配置
        if (webInfo.footerBackgroundConfig) {
          try {
            const config = JSON.parse(webInfo.footerBackgroundConfig)
            bgConfig = { ...bgConfig, ...config }
          } catch (e) {
            console.error('解析页脚背景配置失败:', e)
          }
        }

        // 设置文字颜色
        if (bgConfig.textColor) {
          style.color = bgConfig.textColor
        }

        // 设置文字阴影（增强可读性）
        if (bgConfig.textShadow) {
          style.textShadow =
            '0 2px 8px rgba(0, 0, 0, 0.8), 0 1px 3px rgba(0, 0, 0, 0.6)'
        } else {
          style.textShadow = 'none'
        }
      }

      return style
    },
  },
  created() {},
  mounted() {
    // 监听窗口尺寸变化以实时更新页脚高度
    this._resizeHandler = () => {
      this.viewportWidth = window.innerWidth
    }
    window.addEventListener('resize', this._resizeHandler)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this._resizeHandler)
  },
}
</script>

<style scoped>
.myFooter-wrap {
  user-select: none;
  animation: hideToShow 2s both;
}
.myFooter {
  border-radius: 1.5rem 1.5rem 0 0;
  background: var(--gradientBG);
  text-align: center;
  color: var(--white);
  background-size: 300% 300%;
  animation: gradientBG 10s ease infinite;
  position: relative;
  overflow: hidden;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.myFooter.has-bg-image {
  background: transparent !important;
  animation: none !important;
}
.myFooter.has-bg-image {
  background-attachment: fixed;
}
.myFooter[style*='--footer-bg-image'] {
  background: transparent !important;
  animation: none !important;
}
.myFooter[style*='--footer-bg-image']::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: var(--footer-bg-image);
  background-size: var(--footer-bg-size);
  background-position: var(--footer-bg-position);
  background-repeat: var(--footer-bg-repeat);
  opacity: 1;
  z-index: 0;
}
.myFooter[style*='--footer-bg-image']::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--footer-mask-color, rgba(0, 0, 0, 0.5));
  z-index: 1;
}
.footer-title {
  padding-top: 10px;
  font-size: 16px;
  position: relative;
  z-index: 10;
  font-weight: 600;
}
.icp,
.icp a {
  color: var(--white);
  font-size: 14px;
  position: relative;
  z-index: 10;
  font-weight: 400;
}
.myFooter[style*='--footer-bg-image'] .footer-title {
  color: #ffffff !important;
  font-weight: 500;
}
.myFooter[style*='--footer-bg-image'] .icp,
.myFooter[style*='--footer-bg-image'] .icp a {
  color: #ffd700 !important;
  font-weight: 400;
}
.myFooter[style*='--footer-bg-image'] .icp a:hover,
.myFooter[style*='--footer-bg-image'] .copyright a:hover {
  color: #ffed4a !important;
}
.myFooter[style*='--footer-bg-image'] .copyright a {
  color: var(--white) !important;
  font-weight: 400;
}
.myFooter[style*='--footer-bg-image'] .policy-link {
  color: var(--white);
  font-weight: 500;
  padding: 0 2px;
  transition: color 0.3s;
}
.myFooter[style*='--footer-bg-image'] .policy-link:hover {
  color: var(--themeBackground);
}
.icp {
  padding-top: 10px;
  padding-bottom: 10px;
}
.icp a,
.copyright a {
  text-decoration: none;
  transition: color 0.3s ease, opacity 0.3s ease;
}
.icp a:hover,
.copyright a:hover {
  color: var(--themeBackground);
}
.policy-link {
  color: var(--white);
  font-weight: 500;
  padding: 0 2px;
  transition: color 0.3s;
}
.policy-link:hover {
  color: var(--themeBackground);
}
.copyright {
  font-size: 14px;
  position: relative;
  z-index: 10;
  font-weight: 400;
  padding-top: 5px;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 8px;
}
.copyright-left {
  justify-self: end;
}
.copyright-center {
  justify-self: center;
  white-space: nowrap;
}
.copyright-right {
  justify-self: start;
}
.contact,
.extra-info {
  font-size: 14px;
  position: relative;
  z-index: 10;
  font-weight: 400;
  padding-top: 5px;
}
.myFooter[style*='--footer-bg-image'] .copyright,
.myFooter[style*='--footer-bg-image'] .contact,
.myFooter[style*='--footer-bg-image'] .extra-info {
  color: var(--white) !important;
  font-weight: 400;
}
@media (max-width: 768px) {
  .myFooter {
    border-radius: 0;
    min-height: 120px;
  }
  .myFooter.minimal {
    min-height: 70px;
  }
  .footer-title {
    font-size: 16px;
    padding-top: 8px;
  }
  .icp,
  .contact,
  .extra-info {
    font-size: 14px;
    padding-top: 8px;
    padding-bottom: 8px;
  }
  .copyright {
    font-size: 14px;
    padding-top: 8px;
    padding-bottom: 8px;
    gap: 4px;
  }
  .copyright-left,
  .copyright-center,
  .copyright-right {
    font-size: 14px;
  }
}
.myFooter.minimal {
  min-height: 90px;
}
</style>
