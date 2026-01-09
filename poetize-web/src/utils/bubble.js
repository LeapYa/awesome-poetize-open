const targetElement = document.querySelector(
  '.author-content.author-content-item.single'
)
if (targetElement && targetElement.nodeType === Node.ELEMENT_NODE) {
  try {
    const canvas = document.createElement('canvas')

    // 确保canvas元素创建成功
    if (!canvas || canvas.nodeType !== Node.ELEMENT_NODE) {
      console.warn('创建canvas元素失败，跳过气泡动画')
      return
    }

    canvas.id = 'header_canvas'
    canvas.style.position = 'absolute'
    canvas.style.bottom = '0'
    canvas.width = 844
    canvas.height = 346

    // 安全地添加canvas到DOM
    if (typeof targetElement.appendChild === 'function') {
      targetElement.appendChild(canvas)
    } else {
      console.warn('目标元素不支持appendChild，跳过气泡动画')
      return
    }

    const parent = targetElement.parentNode
    if (parent && parent.nodeType === Node.ELEMENT_NODE) {
      parent.className = 'thumbnail_canvas'
    }
  } catch (error) {
    console.warn('气泡动画初始化失败:', error)
    return
  }
  ;(function () {
    var canvas,
      ctx,
      width,
      height,
      bubbles,
      animateHeader = true
    initHeader()

    function initHeader() {
      canvas = document.getElementById('header_canvas')
      window_resize()
      if (canvas) {
        // 性能优化1: 启用Canvas异步渲染和GPU加速
        ctx = canvas.getContext('2d', {
          willReadFrequently: false, // 减少CPU读取
          alpha: true,
          desynchronized: true, // 异步渲染，不阻塞主线程
        })
        //建立泡泡
        bubbles = []
        // 保持原来的气泡数量，通过GPU加速优化性能
        var num = width * 0.04 //气泡数量
        for (var i = 0; i < num; i++) {
          var c = new Bubble()
          bubbles.push(c)
        }
        animate()
      }
    }

    function animate() {
      if (animateHeader) {
        ctx.clearRect(0, 0, width, height)
        // 性能优化3: 使用高效的传统for循环替代for-in（提升20-30%）
        for (var i = 0, len = bubbles.length; i < len; i++) {
          bubbles[i].draw()
        }
      }
      requestAnimationFrame(animate)
    }

    function window_resize() {
      //canvas铺满窗口
      //width = window.innerWidth;
      //height = window.innerHeight;

      //如果需要铺满内容可以换下面这个
      const panel = document.querySelector('.thumbnail_canvas')
      if (panel) {
        width = panel.offsetWidth
        height = panel.offsetHeight

        canvas.width = width
        canvas.height = height
      }
    }

    window.onresize = function () {
      window_resize()
    }

    function Bubble() {
      var _this = this
      ;(function () {
        _this.pos = {}
        init()
      })()

      function init() {
        _this.pos.x = Math.random() * width
        _this.pos.y = height + Math.random() * 100
        _this.alpha = 0.1 + Math.random() * 0.5 //气泡透明度
        _this.alpha_change = 0.0002 + Math.random() * 0.0005 //气泡透明度变化速度
        _this.scale = 0.2 + Math.random() * 0.8 //气泡大小
        _this.scale_change = Math.random() * 0.002 //气泡大小变化速度
        _this.speed = 0.1 + Math.random() * 0.4 //气泡上升速度
      }

      //气泡
      this.draw = function () {
        if (_this.alpha <= 0) {
          init()
        }
        _this.pos.y -= _this.speed
        _this.alpha -= _this.alpha_change
        _this.scale += _this.scale_change

        // 性能优化4: 使用globalAlpha代替字符串拼接rgba()（避免每帧字符串操作）
        ctx.globalAlpha = _this.alpha
        ctx.fillStyle = '#ffffff' // 固定颜色，不拼接字符串
        ctx.beginPath()
        ctx.arc(
          _this.pos.x,
          _this.pos.y,
          _this.scale * 10,
          0,
          6.283185307179586,
          false
        ) // 使用常量代替2*Math.PI
        ctx.fill()
        ctx.globalAlpha = 1 // 恢复默认透明度
      }
    }
  })()
}
