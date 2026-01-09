<template>
  <div class="forbidden-container">
    <!-- 背景警告效果 -->
    <div class="warning-field">
      <div class="warning-pulse" v-for="n in 8" :key="n" 
           :style="{ 
             left: Math.random() * 100 + '%', 
             top: Math.random() * 100 + '%',
             animationDelay: Math.random() * 2 + 's',
             animationDuration: (3 + Math.random() * 2) + 's'
           }"></div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-wrapper">
      <!-- 标题区域 -->
      <div class="title-section">
        <div class="shield-icon">
          <i class="fa fa-shield"></i>
          <div class="warning-indicator"></div>
        </div>
        <h1 class="main-title">Access Denied</h1>
        <p class="subtitle">访问被拒绝 ⚠️</p>
        <p class="error-code">Error 403 Forbidden</p>
      </div>

      <!-- 3D 403 插画 -->
      <div class="illustration-container">
        <!-- 3D "403" 数字 -->
        <div class="digit-container">
          <div class="digit digit-4">
            <div class="digit-face front">4</div>
            <div class="digit-face back">4</div>
            <div class="digit-face top"></div>
            <div class="digit-face bottom"></div>
            <div class="digit-face left"></div>
            <div class="digit-face right"></div>
          </div>
          
          <div class="digit digit-0">
            <div class="digit-face front">0</div>
            <div class="digit-face back">0</div>
            <div class="digit-face top"></div>
            <div class="digit-face bottom"></div>
            <div class="digit-face left"></div>
            <div class="digit-face right"></div>
          </div>
          
          <div class="digit digit-3">
            <div class="digit-face front">3</div>
            <div class="digit-face back">3</div>
            <div class="digit-face top"></div>
            <div class="digit-face bottom"></div>
            <div class="digit-face left"></div>
            <div class="digit-face right"></div>
          </div>
        </div>

        <!-- 安全装饰元素 -->
        <div class="security-decorations">
          <!-- 安全盾牌 -->
          <div class="decoration shield-1">
            <div class="security-shield">
              <i class="fa fa-lock"></i>
            </div>
          </div>
          
          <div class="decoration shield-2">
            <div class="security-shield">
              <i class="fa fa-exclamation-triangle"></i>
            </div>
          </div>
          
          <div class="decoration shield-3">
            <div class="security-shield">
              <i class="fa fa-ban"></i>
            </div>
          </div>
          
          <!-- 警告标志 -->
          <div class="decoration warning-1">⚠️</div>
          <div class="decoration warning-2">🔒</div>
          <div class="decoration warning-3">🛡️</div>
          
          <!-- 安全扫描线 -->
          <div class="scan-line horizontal"></div>
          <div class="scan-line vertical"></div>
        </div>
      </div>

      <!-- 警告信息区域 -->
      <div class="warning-section">
        <div class="warning-box">
          <div class="warning-icon">
            <i class="fa fa-exclamation-triangle"></i>
          </div>
          <div class="warning-content">
            <h3>安全警告</h3>
            <p>您的访问行为已被安全系统检测并记录。如果您认为这是误判，请联系管理员。</p>
            <div class="security-info">
              <div class="info-item">
                <i class="fa fa-clock-o"></i>
                <span>检测时间：{{ currentTime }}</span>
              </div>
              <div class="info-item">
                <i class="fa fa-map-marker"></i>
                <span>访问IP：{{ userIP }}</span>
              </div>
              <div class="info-item">
                <i class="fa fa-shield"></i>
                <span>安全级别：高</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮区域 -->
      <div class="action-section">
        <div class="button-group">
          <button class="action-btn primary-btn" @click="goHome">
            <i class="fa fa-home"></i>
            <span>返回首页</span>
          </button>
          
          <button class="action-btn secondary-btn" @click="contactAdmin">
            <i class="fa fa-envelope"></i>
            <span>联系管理员</span>
          </button>
        </div>

        <!-- 安全提示 -->
        <div class="security-tips">
          <h4>安全提示</h4>
          <ul>
            <li>请确保您的访问行为符合网站使用规范</li>
            <li>恶意攻击行为将被记录并可能面临法律后果</li>
            <li>如需帮助，请通过正当渠道联系我们</li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 底部提示 -->
    <div class="footer-hint">
      <p>如果您认为这是系统错误，请 <a href="/message" class="contact-link">提交反馈</a> 或联系管理员</p>
      <p class="legal-notice">本站已启用安全防护系统，所有访问行为均受监控</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Forbidden',
  data() {
    return {
      currentTime: '',
      userIP: ''
    }
  },
  mounted() {
    // 获取用户IP和服务器时间
    this.getUserIPAndTime();
    
    // 设置页面标题
    document.title = '访问被拒绝 - 403';
    
    // 记录访问尝试
    this.logAccessAttempt();
  },
  methods: {
    async getUserIPAndTime() {
      try {
        // 通过API获取用户真实IP地址和时间戳
        const res = await this.$http.get(this.$constant.baseURL + "/webInfo/getUserIP");
        if (res.data && res.data.code === 200 && res.data.data) {
          this.userIP = res.data.data.ip || '无法获取';
          if (res.data.data.timestamp) {
            this.currentTime = new Date(res.data.data.timestamp).toLocaleString('zh-CN');
          } else {
            this.currentTime = new Date().toLocaleString('zh-CN');
          }
          // 强制更新视图
          this.$forceUpdate();
        } else {
          this.userIP = '无法获取';
          this.currentTime = '无法获取';
        }
      } catch (error) {
        this.userIP = '无法获取';
        this.currentTime = '无法获取';
      }
    },
    
    goHome() {
      this.$router.push('/');
    },
    
    contactAdmin() {
      this.$router.push('/message');
    },
    
    logAccessAttempt() {
      // 记录403访问尝试（可选）
    }
  }
}
</script>

<style scoped>
.forbidden-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #ff4b2b, #ff416c, #dc2626, #b91c1c);
  background-size: 400% 400%;
  animation: warningGradient 8s ease infinite;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
  box-sizing: border-box;
}

/* 背景动画 */
@keyframes warningGradient {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

/* 警告背景效果 */
.warning-field {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.warning-pulse {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  animation: warningPulse 3s ease-in-out infinite;
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.5);
}

@keyframes warningPulse {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.5); }
}

/* 主要内容 */
.content-wrapper {
  position: relative;
  z-index: 10;
  text-align: center;
  max-width: 900px;
  width: 100%;
}

/* 标题区域 */
.title-section {
  margin-bottom: 40px;
}

.shield-icon {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.shield-icon i {
  font-size: 4rem;
  color: white;
  text-shadow: 0 4px 20px rgba(0,0,0,0.3);
}

.warning-indicator {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 20px;
  height: 20px;
  background: #fbbf24;
  border-radius: 50%;
  animation: warningBlink 1s infinite;
}

@keyframes warningBlink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.main-title {
  font-size: 3.5rem;
  font-weight: 800;
  color: white;
  margin: 0 0 10px 0;
  text-shadow: 0 4px 20px rgba(0,0,0,0.4);
  letter-spacing: 2px;
}

.subtitle {
  font-size: 1.5rem;
  color: white;
  margin: 0 0 10px 0;
  opacity: 0.9;
}

.error-code {
  font-size: 1rem;
  color: rgba(255,255,255,0.8);
  margin: 0;
  letter-spacing: 1px;
}

/* 3D插画区域 */
.illustration-container {
  perspective: 1000px;
  margin: 60px 0;
  position: relative;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 3D数字 */
.digit-container {
  display: flex;
  gap: 20px;
  transform-style: preserve-3d;
}

.digit {
  width: 80px;
  height: 120px;
  position: relative;
  transform-style: preserve-3d;
  animation: digitFloat 4s ease-in-out infinite;
}

.digit:nth-child(1) { animation-delay: 0s; }
.digit:nth-child(2) { animation-delay: 0.5s; }
.digit:nth-child(3) { animation-delay: 1s; }

@keyframes digitFloat {
  0%, 100% { transform: translateY(0) rotateY(0deg); }
  25% { transform: translateY(-10px) rotateY(5deg); }
  50% { transform: translateY(0) rotateY(0deg); }
  75% { transform: translateY(-15px) rotateY(-5deg); }
}

.digit-face {
  position: absolute;
  width: 80px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 4rem;
  font-weight: bold;
  color: white;
  text-shadow: 0 2px 10px rgba(0,0,0,0.3);
}

.digit-face.front {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  transform: translateZ(20px);
  border-radius: 10px;
  border: 2px solid rgba(255,255,255,0.2);
}

.digit-face.back {
  background: linear-gradient(135deg, #b91c1c, #7f1d1d);
  transform: translateZ(-20px) rotateY(180deg);
  border-radius: 10px;
}

.digit-face.top {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  transform: rotateX(90deg) translateZ(60px);
  height: 40px;
  border-radius: 10px 10px 0 0;
}

.digit-face.bottom {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  transform: rotateX(-90deg) translateZ(60px);
  height: 40px;
  border-radius: 0 0 10px 10px;
}

.digit-face.left {
  background: linear-gradient(135deg, #b91c1c, #7f1d1d);
  transform: rotateY(-90deg) translateZ(40px);
  width: 40px;
  border-radius: 10px 0 0 10px;
}

.digit-face.right {
  background: linear-gradient(135deg, #dc2626, #991b1b);
  transform: rotateY(90deg) translateZ(40px);
  width: 40px;
  border-radius: 0 10px 10px 0;
}

/* 安全装饰元素 */
.security-decorations {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.decoration {
  position: absolute;
  animation: securityFloat 6s ease-in-out infinite;
}

.decoration.shield-1 { top: 15%; left: 10%; animation-delay: 0s; }
.decoration.shield-2 { top: 25%; right: 15%; animation-delay: 1s; }
.decoration.shield-3 { bottom: 30%; left: 20%; animation-delay: 2s; }
.decoration.warning-1 { top: 10%; right: 25%; animation-delay: 1.5s; font-size: 2rem; }
.decoration.warning-2 { bottom: 40%; right: 10%; animation-delay: 2.5s; font-size: 1.5rem; }
.decoration.warning-3 { top: 60%; left: 15%; animation-delay: 3.5s; font-size: 1.8rem; }

@keyframes securityFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  33% { transform: translateY(-15px) rotate(3deg); }
  66% { transform: translateY(8px) rotate(-2deg); }
}

.security-shield {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.2rem;
  animation: shieldPulse 2s ease-in-out infinite;
  box-shadow: 0 4px 15px rgba(251, 191, 36, 0.3);
}

@keyframes shieldPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* 扫描线效果 */
.scan-line {
  position: absolute;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.8), transparent);
  animation: scanMove 3s linear infinite;
}

.scan-line.horizontal {
  width: 100%;
  height: 2px;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
}

.scan-line.vertical {
  width: 2px;
  height: 100%;
  left: 50%;
  top: 0;
  transform: translateX(-50%);
  background: linear-gradient(0deg, transparent, rgba(255,255,255,0.8), transparent);
  animation-delay: 1.5s;
}

@keyframes scanMove {
  0% { opacity: 0; transform: translateX(-100%); }
  50% { opacity: 1; }
  100% { opacity: 0; transform: translateX(100%); }
}

/* 警告信息区域 */
.warning-section {
  margin: 40px 0;
}

.warning-box {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 15px;
  padding: 30px;
  display: flex;
  align-items: flex-start;
  gap: 20px;
  text-align: left;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.warning-icon {
  font-size: 2.5rem;
  color: #fbbf24;
  margin-top: 5px;
}

.warning-content h3 {
  color: white;
  margin: 0 0 15px 0;
  font-size: 1.5rem;
}

.warning-content p {
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 20px 0;
  line-height: 1.6;
}

.security-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.9rem;
}

.info-item i {
  width: 16px;
  color: #fbbf24;
}

/* 操作按钮区域 */
.action-section {
  margin: 40px 0;
}

.button-group {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 15px 30px;
  border: none;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  /* 性能优化: 只监听位移和背景色 */
  transition: transform 0.3s ease, background-color 0.3s ease, color 0.3s ease;
  display: flex;
  align-items: center;
  transform: translateZ(0);
  gap: 10px;
  text-decoration: none;
  min-width: 160px;
  justify-content: center;
}

.primary-btn {
  background: linear-gradient(135deg, #059669, #047857);
  color: white;
  box-shadow: 0 6px 20px rgba(5, 150, 105, 0.3);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(5, 150, 105, 0.4);
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

.secondary-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

/* 安全提示 */
.security-tips {
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 10px;
  padding: 20px;
  text-align: left;
  max-width: 500px;
  margin: 0 auto;
}

.security-tips h4 {
  color: white;
  margin: 0 0 15px 0;
  font-size: 1.2rem;
}

.security-tips ul {
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
  padding-left: 20px;
  line-height: 1.6;
}

.security-tips li {
  margin-bottom: 8px;
}

/* 底部提示 */
.footer-hint {
  position: relative;
  z-index: 10;
  text-align: center;
  margin-top: 30px;
}

.footer-hint p {
  color: rgba(255, 255, 255, 0.8);
  margin: 10px 0;
  font-size: 0.9rem;
}

.contact-link {
  color: #fbbf24;
  text-decoration: none;
  font-weight: 600;
}

.contact-link:hover {
  text-decoration: underline;
}

.legal-notice {
  font-size: 0.8rem !important;
  opacity: 0.7;
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-title {
    font-size: 2.5rem;
  }
  
  .digit {
    width: 60px;
    height: 90px;
  }
  
  .digit-face {
    width: 60px;
    height: 90px;
    font-size: 3rem;
  }
  
  .warning-box {
    flex-direction: column;
    text-align: center;
  }
  
  .button-group {
    flex-direction: column;
    align-items: center;
  }
  
  .action-btn {
    width: 200px;
  }
}
</style>