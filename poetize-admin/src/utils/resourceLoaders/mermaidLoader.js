import { isMermaidLoaded, loadExternalResource } from './resourceLoader'

/**
 * 加载Mermaid图表库
 */
export async function loadMermaidResources() {
    if (isMermaidLoaded()) {
        return true
    }

    try {
        await loadExternalResource('/libs/js/mermaid.min.js', 'js')

        // 确保Mermaid已正确加载并初始化
        if (typeof window.mermaid !== 'undefined') {
            // 检测是否为暗色模式
            const isDark = document.documentElement.classList.contains('dark-mode') ||
                document.body.classList.contains('dark-mode')

            // 初始化Mermaid配置
            window.mermaid.initialize({
                startOnLoad: false,
                theme: isDark ? 'dark' : 'default',
                securityLevel: 'loose',
                fontFamily: 'Arial, sans-serif',
                themeVariables: isDark ? {
                    // 深色模式的自定义主题变量
                    darkMode: true,
                    background: '#1e1e1e',
                    primaryColor: '#4a9eff',
                    primaryTextColor: '#ffffff',
                    primaryBorderColor: '#4a9eff',
                    lineColor: '#6b6b6b',
                    secondaryColor: '#2d2d2d',
                    tertiaryColor: '#3a3a3a',
                    mainBkg: '#2d2d2d',
                    secondBkg: '#383838',
                    mainContrastColor: '#ffffff',
                    darkTextColor: '#ffffff',
                    textColor: '#e0e0e0',
                    labelTextColor: '#e0e0e0',
                    fontSize: '14px'
                } : {
                    fontSize: '14px'
                }
            })

        }
        return true
    } catch (error) {
        console.error('Mermaid加载失败:', error)
        return false
    }
}
