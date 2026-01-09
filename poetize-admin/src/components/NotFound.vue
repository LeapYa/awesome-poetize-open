<template>
  <div class="not-found-container">
    <!-- 背景星空效果 -->
    <div class="starfield">
      <div class="star" v-for="n in 100" :key="n" 
           :style="{ 
             left: Math.random() * 100 + '%', 
             top: Math.random() * 100 + '%',
             animationDelay: Math.random() * 3 + 's',
             animationDuration: (2 + Math.random() * 3) + 's'
           }"></div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-wrapper">
      <!-- 标题区域 -->
      <div class="title-section">
        <h1 class="main-title">Oops</h1>
        <p class="subtitle">页面走丢了 🌟</p>
        <p class="error-code">Error 404 Page Not Found</p>
      </div>

      <!-- 3D 404 插画 -->
      <div class="illustration-container">
        <!-- 3D "404" 数字 -->
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
          
          <div class="digit digit-4">
            <div class="digit-face front">4</div>
            <div class="digit-face back">4</div>
            <div class="digit-face top"></div>
            <div class="digit-face bottom"></div>
            <div class="digit-face left"></div>
            <div class="digit-face right"></div>
          </div>
        </div>

        <!-- 装饰元素 -->
        <div class="decorations">
          <!-- 浮动几何体 -->
          <div class="decoration geo-1">
            <div class="cube">
              <div class="face front"></div>
              <div class="face back"></div>
              <div class="face top"></div>
              <div class="face bottom"></div>
              <div class="face left"></div>
              <div class="face right"></div>
            </div>
          </div>
          
          <div class="decoration geo-2">
            <div class="sphere"></div>
          </div>
          
          <div class="decoration geo-3">
            <div class="cylinder">
              <div class="face top"></div>
              <div class="face bottom"></div>
              <div class="face side"></div>
            </div>
          </div>
          
          <!-- 漂浮叶子/植物 -->
          <div class="decoration leaf-1">🍃</div>
          <div class="decoration leaf-2">🌿</div>
          <div class="decoration leaf-3">🍀</div>
          
          <!-- 漂浮气泡 -->
          <div class="decoration bubble-1"></div>
          <div class="decoration bubble-2"></div>
          <div class="decoration bubble-3"></div>
        </div>
      </div>

      <!-- 操作按钮区域 -->
      <div class="action-section">
        <div class="button-group">
          <button class="action-btn primary-btn" @click="goHome">
            <i class="fa fa-home"></i>
            <span>返回首页</span>
          </button>
          
          <button class="action-btn secondary-btn" @click="goBack">
            <i class="fa fa-arrow-left"></i>
            <span>返回上页</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 底部提示 -->
    <div class="footer-hint">
      <p>如果问题持续存在，请 <a href="/message" class="contact-link">联系我们</a></p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound',
  data() {
    return {}
  },
  mounted() {
    // 设置页面标题
    document.title = '页面未找到 - 404';
  },
  methods: {
    goHome() {
      this.$router.push('/');
    },
    
    goBack() {
      if (window.history.length > 1) {
        this.$router.go(-1);
      } else {
        this.$router.push('/');
      }
    },
    
  }
}
</script>

<style scoped>
.not-found-container {
  min-height: 100vh;
  background: #1a1a2e;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
  box-sizing: border-box;
}

/* 星空效果 */
.starfield {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.star {
  position: absolute;
  width: 2px;
  height: 2px;
  background: white;
  border-radius: 50%;
  opacity: 0.7;
  animation: twinkle 3s linear infinite;
}

@keyframes twinkle {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* 主要内容 */
.content-wrapper {
  position: relative;
  z-index: 10;
  text-align: center;
  max-width: 800px;
  width: 100%;
}

/* 标题区域 */
.title-section {
  margin-bottom: 40px;
}

.main-title {
  font-size: 4rem;
  font-weight: 800;
  color: white;
  margin: 0 0 10px 0;
  text-shadow: 0 4px 20px rgba(0,0,0,0.3);
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

.digit-first { animation-delay: 0s; }
.digit:nth-child(2) { animation-delay: 0.5s; }
.digit-last { animation-delay: 1s; }

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
  background: linear-gradient(135deg, #ff4b2b, #ff416c);
  transform: translateZ(20px);
  border-radius: 10px;
}

.digit-face.back {
  background: linear-gradient(135deg, #e73c7e, #23a6d5);
  transform: translateZ(-20px) rotateY(180deg);
  border-radius: 10px;
}

.digit-face.top {
  background: linear-gradient(135deg, #23a6d5, #23d5ab);
  transform: rotateX(90deg) translateZ(60px);
  height: 40px;
  border-radius: 10px 10px 0 0;
}

.digit-face.bottom {
  background: linear-gradient(135deg, #ee7752, #e73c7e);
  transform: rotateX(-90deg) translateZ(60px);
  height: 40px;
  border-radius: 0 0 10px 10px;
}

.digit-face.left {
  background: linear-gradient(135deg, #23d5ab, #ee7752);
  transform: rotateY(-90deg) translateZ(40px);
  width: 40px;
  border-radius: 10px 0 0 10px;
}

.digit-face.right {
  background: linear-gradient(135deg, #ff416c, #23a6d5);
  transform: rotateY(90deg) translateZ(40px);
  width: 40px;
  border-radius: 0 10px 10px 0;
}

/* 装饰元素 */
.decorations {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.decoration {
  position: absolute;
  animation: float 6s ease-in-out infinite;
}

.decoration.geo-1 { top: 10%; left: 15%; animation-delay: 0s; }
.decoration.geo-2 { top: 20%; right: 10%; animation-delay: 1s; }
.decoration.geo-3 { bottom: 30%; left: 20%; animation-delay: 2s; }
.decoration.geo-4 { bottom: 10%; right: 25%; animation-delay: 3s; }
.decoration.leaf-1 { top: 15%; left: 70%; animation-delay: 1.5s; font-size: 2rem; }
.decoration.leaf-2 { bottom: 40%; right: 80%; animation-delay: 2.5s; font-size: 1.5rem; }
.decoration.leaf-3 { top: 60%; left: 10%; animation-delay: 3.5s; font-size: 1.8rem; }

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  33% { transform: translateY(-20px) rotate(5deg); }
  66% { transform: translateY(10px) rotate(-3deg); }
}

/* 几何体样式 */
.cube {
  width: 30px;
  height: 30px;
  transform-style: preserve-3d;
  animation: cubeRotate 8s linear infinite;
}

@keyframes cubeRotate {
  from { transform: rotateX(0) rotateY(0); }
  to { transform: rotateX(360deg) rotateY(360deg); }
}

.cube .face {
  position: absolute;
  width: 30px;
  height: 30px;
  opacity: 0.8;
}

.cube .face.front { background: #ff4b2b; transform: translateZ(15px); }
.cube .face.back { background: #ff416c; transform: translateZ(-15px) rotateY(180deg); }
.cube .face.top { background: #23a6d5; transform: rotateX(90deg) translateZ(15px); }
.cube .face.bottom { background: #23d5ab; transform: rotateX(-90deg) translateZ(15px); }
.cube .face.left { background: #ee7752; transform: rotateY(-90deg) translateZ(15px); }
.cube .face.right { background: #e73c7e; transform: rotateY(90deg) translateZ(15px); }

.sphere {
  width: 25px;
  height: 25px;
  background: radial-gradient(circle at 30% 30%, #23d5ab, #23a6d5);
  border-radius: 50%;
  animation: spherePulse 3s ease-in-out infinite;
}

@keyframes spherePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

/* 圆柱体 */
.cylinder {
  width: 20px;
  height: 35px;
  position: relative;
  transform-style: preserve-3d;
  animation: cylinderRotate 6s linear infinite;
}

@keyframes cylinderRotate {
  from { transform: rotateY(0deg); }
  to { transform: rotateY(360deg); }
}

.cylinder .face.top {
  width: 20px;
  height: 20px;
  background: #ff4b2b;
  border-radius: 50%;
  position: absolute;
  top: 0;
  transform: translateZ(10px);
}

.cylinder .face.bottom {
  width: 20px;
  height: 20px;
  background: #23a6d5;
  border-radius: 50%;
  position: absolute;
  bottom: 0;
  transform: translateZ(-10px);
}

.cylinder .face.side {
  width: 20px;
  height: 35px;
  background: linear-gradient(to right, #ff416c, #e73c7e);
  position: absolute;
  transform: rotateY(90deg) translateZ(10px);
}

/* 气泡 */
.bubble-1, .bubble-2, .bubble-3 {
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  animation: bubbleFloat 4s ease-in-out infinite;
}

.bubble-1 {
  width: 15px;
  height: 15px;
  top: 25%;
  left: 60%;
  animation-delay: 0s;
}

.bubble-2 {
  width: 20px;
  height: 20px;
  bottom: 20%;
  left: 75%;
  animation-delay: 1s;
}

.bubble-3 {
  width: 12px;
  height: 12px;
  top: 70%;
  right: 15%;
  animation-delay: 2s;
}

@keyframes bubbleFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.7; }
  50% { transform: translateY(-30px) scale(1.1); opacity: 1; }
}

/* 操作按钮区域 */
.action-section {
  margin-top: 40px;
}

.button-group {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 40px;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 15px 30px;
  border: none;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  /* 性能优化: 只监听实际变化的属性 */
  transition: transform 0.3s ease, color 0.3s ease, opacity 0.3s ease;
  text-decoration: none;
  position: relative;
  transform: translateZ(0);
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s;
}

.action-btn:hover::before {
  left: 100%;
}

.primary-btn {
  background: linear-gradient(135deg, #ff4b2b, #ff416c);
  color: white;
  box-shadow: 0 8px 25px rgba(255, 75, 43, 0.3);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(255, 75, 43, 0.4);
}

.secondary-btn {
  background: rgba(255,255,255,0.2);
  color: white;
  border: 2px solid rgba(255,255,255,0.3);
  backdrop-filter: blur(10px);
}

.secondary-btn:hover {
  transform: translateY(-2px);
  background: rgba(255,255,255,0.3);
}



/* 底部提示 */
.footer-hint {
  margin-top: 60px;
  text-align: center;
  z-index: 10;
}

.footer-hint p {
  color: rgba(255,255,255,0.8);
  margin: 0;
}

.contact-link {
  color: white;
  text-decoration: underline;
}

.contact-link:hover {
  color: #ff4b2b;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-title { font-size: 2.5rem; }
  .subtitle { font-size: 1.2rem; }
  
  .digit-container {
    gap: 10px;
    transform: scale(0.8);
  }
  
  .button-group {
    flex-direction: column;
    align-items: center;
  }
  
  .action-btn {
    width: 200px;
    justify-content: center;
  }
  
  
  .illustration-container {
    height: 200px;
    margin: 40px 0;
  }
}

@media (max-width: 480px) {
  .not-found-container {
    padding: 10px;
  }
  
  .main-title { font-size: 2rem; }
  .subtitle { font-size: 1rem; }
  
  .digit-container {
    transform: scale(0.6);
  }
  
  .decoration {
    display: none;
  }
}
</style>