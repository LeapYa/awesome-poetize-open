/**
 * 性能监控工具
 * 用于追踪和优化组件性能
 */

class PerformanceMonitor {
  constructor() {
    this.metrics = {
      componentLoad: [],
      renderTime: [],
      apiCalls: [],
      memoryUsage: []
    }
    
    this.enabled = process.env.NODE_ENV === 'development'
  }
  
  /**
   * 标记性能点
   */
  mark(name) {
    if (!this.enabled) return
    
    if (performance && performance.mark) {
      performance.mark(name)
    }
  }
  
  /**
   * 测量性能
   */
  measure(name, startMark, endMark) {
    if (!this.enabled) return
    
    if (performance && performance.measure) {
      try {
        performance.measure(name, startMark, endMark)
        
        const entries = performance.getEntriesByName(name)
        if (entries.length > 0) {
          const duration = entries[entries.length - 1].duration
          return duration
        }
      } catch (e) {
      }
    }
  }
  
  /**
   * 记录组件加载时间
   */
  recordComponentLoad(componentName, duration) {
    if (!this.enabled) return
    
    this.metrics.componentLoad.push({
      name: componentName,
      duration,
      timestamp: Date.now()
    })
    
  }
  
  /**
   * 记录渲染时间
   */
  recordRenderTime(componentName, duration) {
    if (!this.enabled) return
    
    this.metrics.renderTime.push({
      name: componentName,
      duration,
      timestamp: Date.now()
    })
    
    if (duration > 16) { // > 1帧 (60fps)
    }
  }
  
  /**
   * 记录API调用
   */
  recordApiCall(apiName, duration, success) {
    if (!this.enabled) return
    
    this.metrics.apiCalls.push({
      name: apiName,
      duration,
      success,
      timestamp: Date.now()
    })
    
    const status = success ? '✅' : '❌'
  }
  
  /**
   * 检查内存使用
   */
  checkMemory() {
    if (!this.enabled) return
    
    if (performance && performance.memory) {
      const memory = {
        used: (performance.memory.usedJSHeapSize / 1048576).toFixed(2),
        total: (performance.memory.totalJSHeapSize / 1048576).toFixed(2),
        limit: (performance.memory.jsHeapSizeLimit / 1048576).toFixed(2)
      }
      
      this.metrics.memoryUsage.push({
        ...memory,
        timestamp: Date.now()
      })
      
      
      // 警告：内存使用超过80%
      const usage = (memory.used / memory.limit) * 100
      if (usage > 80) {
      }
      
      return memory
    }
  }
  
  /**
   * 获取性能报告
   */
  getReport() {
    if (!this.enabled) return null
    
    return {
      componentLoad: this.getAverages(this.metrics.componentLoad),
      renderTime: this.getAverages(this.metrics.renderTime),
      apiCalls: this.getApiStats(),
      memoryUsage: this.getMemoryStats()
    }
  }
  
  /**
   * 计算平均值
   */
  getAverages(metrics) {
    if (metrics.length === 0) return {}
    
    const grouped = {}
    metrics.forEach(m => {
      if (!grouped[m.name]) {
        grouped[m.name] = []
      }
      grouped[m.name].push(m.duration)
    })
    
    const result = {}
    Object.keys(grouped).forEach(name => {
      const durations = grouped[name]
      const avg = durations.reduce((a, b) => a + b, 0) / durations.length
      const min = Math.min(...durations)
      const max = Math.max(...durations)
      
      result[name] = {
        avg: avg.toFixed(2),
        min: min.toFixed(2),
        max: max.toFixed(2),
        count: durations.length
      }
    })
    
    return result
  }
  
  /**
   * 获取API统计
   */
  getApiStats() {
    const total = this.metrics.apiCalls.length
    const success = this.metrics.apiCalls.filter(c => c.success).length
    const failed = total - success
    
    return {
      total,
      success,
      failed,
      successRate: total > 0 ? ((success / total) * 100).toFixed(1) + '%' : '0%'
    }
  }
  
  /**
   * 获取内存统计
   */
  getMemoryStats() {
    if (this.metrics.memoryUsage.length === 0) return {}
    
    const latest = this.metrics.memoryUsage[this.metrics.memoryUsage.length - 1]
    return {
      current: `${latest.used}MB`,
      total: `${latest.total}MB`,
      limit: `${latest.limit}MB`
    }
  }
  
  /**
   * 打印报告
   */
  printReport() {
    if (!this.enabled) return
    
    const report = this.getReport()
    
    
    console.table(report.componentLoad)
    
    console.table(report.renderTime)
    
    console.table(report.apiCalls)
    
    console.table(report.memoryUsage)
    
  }
  
  /**
   * 清空指标
   */
  clear() {
    this.metrics = {
      componentLoad: [],
      renderTime: [],
      apiCalls: [],
      memoryUsage: []
    }
    
    if (performance && performance.clearMarks) {
      performance.clearMarks()
      performance.clearMeasures()
    }
    
  }
}

// 单例模式
const monitor = new PerformanceMonitor()

export default monitor

/**
 * 使用示例：
 * 
 * import monitor from '@/components/Live2D/utils/performance'
 * 
 * // 标记开始
 * monitor.mark('component-start')
 * 
 * // 执行操作...
 * 
 * // 测量性能
 * monitor.mark('component-end')
 * monitor.measure('Component Load', 'component-start', 'component-end')
 * 
 * // 记录指标
 * monitor.recordComponentLoad('Live2DWidget', 250)
 * monitor.recordRenderTime('AIChatMessages', 15)
 * monitor.recordApiCall('/api/chat', 120, true)
 * 
 * // 检查内存
 * monitor.checkMemory()
 * 
 * // 获取报告
 * const report = monitor.getReport()
 * monitor.printReport()
 */
