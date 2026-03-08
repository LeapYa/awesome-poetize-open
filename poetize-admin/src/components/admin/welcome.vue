<template>
  <div class="welcome-container">
    <div class="welcome-title">
      <h2 class="playful">
        <span>欢</span>
        <span>迎</span>
        <span>光</span>
        <span>临</span>
      </h2>
    </div>

    <!-- 版本信息 -->
    <div class="version-info">
      <span class="version-tag">{{ appVersion }}</span>
      <span v-if="updateAvailable" class="update-dot" @click="openReleasePage"></span>
    </div>

    <!-- 引导向导呼出区域 - Neubrutalism Style -->
    <div id="field-welcome-entry" class="guide-trigger-area">
      <template v-if="hasStartedGuide">
        <!-- Progress Badge -->
        <div class="neo-badge">
          <span class="badge-text">{{ resumeStep }} / {{ totalSteps }}</span>
          <span class="badge-dot"></span>
          <span class="badge-remaining">{{ remainingSteps }} 步待完成</span>
        </div>
        <p class="guide-current-title">{{ currentGuideTitle }}</p>
        <div class="guide-btn-group">
          <button class="neo-btn neo-btn-primary" @click="startGuide(savedStep)">
            <i class="el-icon-video-play"></i>
            继续配置
          </button>
          <button class="neo-btn neo-btn-secondary" @click="startGuide(0)">
            <i class="el-icon-refresh"></i>
            从头开始
          </button>
        </div>
      </template>
      <template v-else>
        <div class="neo-badge neo-badge-new">
          <span class="badge-icon">✨</span>
          <span class="badge-text">新手引导</span>
        </div>
        <p class="guide-text">初次部署？不知道去哪配置？</p>
        <button class="neo-btn neo-btn-primary neo-btn-large" @click="startGuide(0)">
          <svg class="guide-action-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle cx="12" cy="12" r="8" />
            <path d="M10 14L14 10L13 13L10 14Z" />
          </svg>
          开启配置向导
        </button>
      </template>
    </div>
  </div>
</template>

<script>
  import constant from '@/utils/constant';

  const STORAGE_KEY = 'poetize_guide_step';
  const TOTAL_STEPS = 12;
  const GUIDE_TITLES = [
    '基础设置',
    '外观个性化',
    '插件管理',
    '配置管理',
    '通知与邮件',
    '邮件模板',
    '安全与登录',
    '站长资料修改',
    'SEO全套配置',
    '本站信息 (siteInfo)',
    '侧边栏定制',
    '文章AI助手'
  ];

  export default {
    data() {
      return {
        savedStep: 0,
        totalSteps: TOTAL_STEPS,
        hasStartedGuide: false,
        updateAvailable: false,
        latestVersion: ''
      }
    },

    computed: {
      appVersion() {
        return constant.APP_VERSION;
      },
      resumeStep() {
        // savedStep 0 = intro (show as 0), savedStep 1-9 = actual steps
        return this.savedStep;
      },
      remainingSteps() {
        return Math.max(this.totalSteps - this.savedStep, 0);
      },
      currentGuideTitle() {
        if (this.savedStep === 0) {
          return '认识灵动胶囊';
        }
        return GUIDE_TITLES[this.savedStep - 1] || GUIDE_TITLES[0];
      }
    },

    created() {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved !== null) {
        const val = parseInt(saved, 10);
        // val 0 = island intro, val 1-9 = actual steps
        if (val >= 0 && val <= TOTAL_STEPS) {
          this.savedStep = val;
          this.hasStartedGuide = true;
        }
      }
      this.checkForUpdates();
    },

    methods: {
      startGuide(fromStep) {
        // Emit event to the global GuideFloat component
        this.$root.$emit('start-guide', fromStep);
      },

      /**
       * 比较两个版本号，支持 v 前缀和预发布版本（如 rc1）
       * 返回 1 如果 v1 > v2，-1 如果 v1 < v2，0 如果相等
       */
      compareVersions(v1, v2) {
        if (!v1 || !v2) return 0;
        const cleanV1 = v1.replace(/^v/i, '');
        const cleanV2 = v2.replace(/^v/i, '');
        const [main1, pre1] = cleanV1.split('-');
        const [main2, pre2] = cleanV2.split('-');
        const parts1 = main1.split('.').map(p => parseInt(p, 10) || 0);
        const parts2 = main2.split('.').map(p => parseInt(p, 10) || 0);
        const len = Math.max(parts1.length, parts2.length);
        for (let i = 0; i < len; i++) {
          const num1 = parts1[i] || 0;
          const num2 = parts2[i] || 0;
          if (num1 > num2) return 1;
          if (num1 < num2) return -1;
        }
        if (!pre1 && pre2) return 1;
        if (pre1 && !pre2) return -1;
        if (pre1 && pre2) {
          if (pre1 > pre2) return 1;
          if (pre1 < pre2) return -1;
        }
        return 0;
      },

      /**
       * 检查最新版本（24 小时缓存，GitHub → Gitee 自动降级）
       */
      async checkForUpdates() {
        try {
          const CACHE_KEY = 'poetize_update_check';
          const CACHE_TTL = 24 * 60 * 60 * 1000; // 24 小时
          const cached = localStorage.getItem(CACHE_KEY);

          if (cached) {
            const { version, ts } = JSON.parse(cached);
            if (Date.now() - ts < CACHE_TTL) {
              if (version && this.compareVersions(version, constant.APP_VERSION) > 0) {
                this.latestVersion = version;
                this.updateAvailable = true;
                this.$notify.info(
                  '发现新版本',
                  `${version} 可用，点击前往更新`,
                  5000,
                  () => this.openReleasePage()
                );
              }
              return;
            }
          }

          this.latestVersion = await this.fetchLatestTag();
          if (!this.latestVersion) return;

          // 缓存结果
          localStorage.setItem(CACHE_KEY, JSON.stringify({
            version: this.latestVersion,
            ts: Date.now()
          }));

          if (this.latestVersion && this.compareVersions(this.latestVersion, constant.APP_VERSION) > 0) {
            this.updateAvailable = true;
            this.$notify.info(
              '发现新版本',
              `${this.latestVersion} 可用，点击前往更新`,
              5000,
              () => this.openReleasePage()
            );
          }
        } catch (e) {
          // 静默忽略
        }
      },

      /**
       * 获取最新 tag：同时请求 GitHub 和 Gitee，谁先返回用谁
       */
      async fetchLatestTag() {
        const fetchTag = (url) => {
          const controller = new AbortController();
          const timer = setTimeout(() => controller.abort(), 8000);
          return fetch(url, { signal: controller.signal })
            .then(res => { clearTimeout(timer); return res; })
            .then(res => { if (!res.ok) throw new Error(res.status); return res.json(); })
            .then(tags => {
              if (tags && tags.length > 0) {
                // Fetch up to 100 tags and do client-side proper semantic version sorting
                const sortedTags = tags.map(t => t.name).sort((a, b) => this.compareVersions(b, a));
                return sortedTags[0];
              }
              throw new Error('empty');
            })
            .catch(e => { clearTimeout(timer); throw e; });
        };

        try {
          return await Promise.any([
            fetchTag(`https://api.github.com/repos/${constant.GITHUB_REPO}/tags?per_page=100`),
            fetchTag(`https://gitee.com/api/v5/repos/${constant.GITEE_REPO}/tags?per_page=100&sort=name&direction=desc`)
          ]);
        } catch (e) {
          return null;
        }
      },

      /**
       * 打开 GitHub Releases 页面
       */
      openReleasePage() {
        window.open(
          `https://github.com/${constant.GITHUB_REPO}/releases`,
          '_blank'
        );
      }
    }
  }
</script>

<style scoped>

  .welcome-title {
    text-align: center;
    font-size: 50px;
    font-weight: bold;
  }

  .playful span {
    position: relative;
    color: #5362f6;
    text-shadow: 0.25px 0.25px #e485f8, 0.5px 0.5px #e485f8, 0.75px 0.75px #e485f8,
    1px 1px #e485f8, 1.25px 1.25px #e485f8, 1.5px 1.5px #e485f8, 1.75px 1.75px #e485f8,
    2px 2px #e485f8, 2.25px 2.25px #e485f8, 2.5px 2.5px #e485f8, 2.75px 2.75px #e485f8,
    3px 3px #e485f8, 3.25px 3.25px #e485f8, 3.5px 3.5px #e485f8, 3.75px 3.75px #e485f8,
    4px 4px #e485f8, 4.25px 4.25px #e485f8, 4.5px 4.5px #e485f8, 4.75px 4.75px #e485f8,
    5px 5px #e485f8, 5.25px 5.25px #e485f8, 5.5px 5.5px #e485f8, 5.75px 5.75px #e485f8,
    6px 6px #e485f8;
    animation: scatter 1.75s infinite;
    font-weight: normal;
  }

  .playful span:nth-child(2n) {
    color: #ed625c;
    text-shadow: 0.25px 0.25px #f2a063, 0.5px 0.5px #f2a063, 0.75px 0.75px #f2a063,
    1px 1px #f2a063, 1.25px 1.25px #f2a063, 1.5px 1.5px #f2a063, 1.75px 1.75px #f2a063,
    2px 2px #f2a063, 2.25px 2.25px #f2a063, 2.5px 2.5px #f2a063, 2.75px 2.75px #f2a063,
    3px 3px #f2a063, 3.25px 3.25px #f2a063, 3.5px 3.5px #f2a063, 3.75px 3.75px #f2a063,
    4px 4px #f2a063, 4.25px 4.25px #f2a063, 4.5px 4.5px #f2a063, 4.75px 4.75px #f2a063,
    5px 5px #f2a063, 5.25px 5.25px #f2a063, 5.5px 5.5px #f2a063, 5.75px 5.75px #f2a063,
    6px 6px #f2a063;
    animation-delay: 0.3s;
  }

  .playful span:nth-child(3n) {
    color: #ffd913;
    text-shadow: 0.25px 0.25px #6ec0a9, 0.5px 0.5px #6ec0a9, 0.75px 0.75px #6ec0a9,
    1px 1px #6ec0a9, 1.25px 1.25px #6ec0a9, 1.5px 1.5px #6ec0a9, 1.75px 1.75px #6ec0a9,
    2px 2px #6ec0a9, 2.25px 2.25px #6ec0a9, 2.5px 2.5px #6ec0a9, 2.75px 2.75px #6ec0a9,
    3px 3px #6ec0a9, 3.25px 3.25px #6ec0a9, 3.5px 3.5px #6ec0a9, 3.75px 3.75px #6ec0a9,
    4px 4px #6ec0a9, 4.25px 4.25px #6ec0a9, 4.5px 4.5px #6ec0a9, 4.75px 4.75px #6ec0a9,
    5px 5px #6ec0a9, 5.25px 5.25px #6ec0a9, 5.5px 5.5px #6ec0a9, 5.75px 5.75px #6ec0a9,
    6px 6px #6ec0a9;
    animation-delay: 0.15s;
  }

  .playful span:nth-child(5n) {
    color: #555bff;
    text-shadow: 0.25px 0.25px #e485f8, 0.5px 0.5px #e485f8, 0.75px 0.75px #e485f8,
    1px 1px #e485f8, 1.25px 1.25px #e485f8, 1.5px 1.5px #e485f8, 1.75px 1.75px #e485f8,
    2px 2px #e485f8, 2.25px 2.25px #e485f8, 2.5px 2.5px #e485f8, 2.75px 2.75px #e485f8,
    3px 3px #e485f8, 3.25px 3.25px #e485f8, 3.5px 3.5px #e485f8, 3.75px 3.75px #e485f8,
    4px 4px #e485f8, 4.25px 4.25px #e485f8, 4.5px 4.5px #e485f8, 4.75px 4.75px #e485f8,
    5px 5px #e485f8, 5.25px 5.25px #e485f8, 5.5px 5.5px #e485f8, 5.75px 5.75px #e485f8,
    6px 6px #e485f8;
    animation-delay: 0.4s;
  }

  .playful span:nth-child(7n) {
    color: #ff9c55;
    text-shadow: 0.25px 0.25px #ff5555, 0.5px 0.5px #ff5555, 0.75px 0.75px #ff5555,
    1px 1px #ff5555, 1.25px 1.25px #ff5555, 1.5px 1.5px #ff5555, 1.75px 1.75px #ff5555,
    2px 2px #ff5555, 2.25px 2.25px #ff5555, 2.5px 2.5px #ff5555, 2.75px 2.75px #ff5555,
    3px 3px #ff5555, 3.25px 3.25px #ff5555, 3.5px 3.5px #ff5555, 3.75px 3.75px #ff5555,
    4px 4px #ff5555, 4.25px 4.25px #ff5555, 4.5px 4.5px #ff5555, 4.75px 4.75px #ff5555,
    5px 5px #ff5555, 5.25px 5.25px #ff5555, 5.5px 5.5px #ff5555, 5.75px 5.75px #ff5555,
    6px 6px #ff5555;
    animation-delay: 0.25s;
  }

  /* --- Start guide-trigger UI/UX Pro Max Rules - Neubrutalism --- */
  .guide-trigger-area {
    margin-top: 3rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  /* Neo Badge */
  .neo-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 1rem;
    background: #FFEB3B;
    border: 3px solid #1a1a1a;
    border-radius: 0;
    box-shadow: 4px 4px 0 #1a1a1a;
    font-weight: 700;
    font-size: 0.875rem;
    color: #1a1a1a;
    margin-bottom: 1rem;
  }

  .neo-badge-new {
    background: #4ECDC4;
  }

  .badge-icon {
    font-size: 1rem;
  }

  .badge-text {
    font-weight: 800;
  }

  .badge-dot {
    width: 6px;
    height: 6px;
    background: #1a1a1a;
    border-radius: 50%;
  }

  .badge-remaining {
    font-weight: 500;
  }

  .guide-text {
    color: #475569;
    font-size: 1rem;
    margin-bottom: 1.5rem;
    font-weight: 500;
  }

  .guide-current-title {
    color: #1a1a1a;
    font-size: 1.25rem;
    font-weight: 800;
    margin-bottom: 1.5rem;
  }

  .guide-btn-group {
    display: flex;
    gap: 1rem;
    align-items: center;
  }

  /* Neubrutalism Buttons */
  .neo-btn {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1.5rem;
    font-size: 0.95rem;
    font-weight: 700;
    border: 3px solid #1a1a1a;
    border-radius: 0;
    cursor: pointer;
    transition: transform 0.1s ease, box-shadow 0.1s ease;
    box-shadow: 4px 4px 0 #1a1a1a;
  }

  .neo-btn:hover {
    transform: translate(-2px, -2px);
    box-shadow: 6px 6px 0 #1a1a1a;
  }

  .neo-btn:active {
    transform: translate(2px, 2px);
    box-shadow: 2px 2px 0 #1a1a1a;
  }

  .neo-btn-primary {
    background: #FF6B6B;
    color: #fff;
  }

  .neo-btn-primary:hover {
    background: #ff5252;
  }

  .neo-btn-secondary {
    background: #fff;
    color: #1a1a1a;
  }

  .neo-btn-secondary:hover {
    background: #f5f5f5;
  }

  .neo-btn-large {
    padding: 1rem 2rem;
    font-size: 1.1rem;
  }

  .neo-btn i {
    font-size: 1.1em;
  }

  .guide-action-icon {
    width: 1.1em;
    height: 1.1em;
    stroke: currentColor;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
    transform: translateY(-1px);
  }

  /* --- Version Info --- */
  .version-info {
    position: fixed;
    bottom: 1.5rem;
    right: 2rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    z-index: 10;
  }

  .version-tag {
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 0.8rem;
    color: #94a3b8;
    background: rgba(241, 245, 249, 0.8);
    padding: 0.25rem 0.65rem;
    border-radius: 999px;
    backdrop-filter: blur(4px);
    user-select: all;
    letter-spacing: 0.02em;
  }

  .update-dot {
    width: 8px;
    height: 8px;
    background: #FF6B6B;
    border-radius: 50%;
    cursor: pointer;
    animation: update-pulse 1.5s ease-in-out infinite;
    flex-shrink: 0;
  }

  @keyframes update-pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.5; transform: scale(1.3); }
  }
</style>
