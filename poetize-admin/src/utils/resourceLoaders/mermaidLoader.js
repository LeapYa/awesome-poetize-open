import { isMermaidLoaded } from './resourceLoader'

/**
 * 加载Mermaid图表库
 */
export async function loadMermaidResources() {
    if (isMermaidLoaded()) {
        return true
    }

    try {
        const mod = await import('mermaid')
        const mermaid = mod?.default || mod
        if (!mermaid) {
            throw new Error('Mermaid 模块不可用')
        }

        window.mermaid = mermaid

        // 确保Mermaid已正确加载并初始化
        if (typeof window.mermaid !== 'undefined') {
            // 初始化Mermaid配置
            window.mermaid.initialize({
                startOnLoad: false,
                theme: 'default',
                securityLevel: 'loose',
                fontFamily: 'Arial, sans-serif',
                themeVariables: {
                    fontSize: '13px',
                },
                flowchart: {
                    useMaxWidth: true
                },
                sequence: {
                    useMaxWidth: true
                },
                gantt: {
                    useMaxWidth: true
                }
            })

        }
        return true
    } catch (error) {
        console.error('Mermaid加载失败:', error)
        return false
    }
}

/**
 * Mermaid 在后台编辑器里经常使用 foreignObject 承载中文标签，
 * 某些字体/缩放下 Mermaid 计算出的高度会偏小，导致字形底部被裁掉。
 * 渲染后统一补正标签容器高度，避免三个自研编辑器各自维护一套修复逻辑。
 */
export function normalizeMermaidDiagrams(root) {
    if (!root || typeof root.querySelectorAll !== 'function') {
        return
    }

    const svgList = root.querySelectorAll('svg')
    svgList.forEach((svg) => {
        svg.style.overflow = 'visible'

        const foreignObjects = svg.querySelectorAll('foreignObject')
        foreignObjects.forEach((foreignObject) => {
            foreignObject.style.overflow = 'visible'

            const labelNode = foreignObject.querySelector('div, span, p')
            if (!labelNode) {
                return
            }

            labelNode.style.overflow = 'visible'
            labelNode.style.margin = '0'
            labelNode.style.padding = '0'
            labelNode.style.width = '100%'
            labelNode.style.height = '100%'
            labelNode.style.boxSizing = 'border-box'
            labelNode.style.display = 'flex'
            labelNode.style.alignItems = 'center'
            labelNode.style.justifyContent = 'center'
            labelNode.style.textAlign = 'center'

            const nestedTextNodes = foreignObject.querySelectorAll('span, p')
            nestedTextNodes.forEach((node) => {
                node.style.margin = '0'
                node.style.padding = '0'
                node.style.display = 'inline-flex'
                node.style.alignItems = 'center'
                node.style.justifyContent = 'center'
                node.style.textAlign = 'center'
            })

            const currentHeight = parseFloat(foreignObject.getAttribute('height') || '0') ||
                Math.ceil(foreignObject.getBoundingClientRect?.().height || 0)
            const measuredHeight = Math.ceil(Math.max(
                labelNode.scrollHeight || 0,
                labelNode.getBoundingClientRect?.().height || 0,
                currentHeight + 6
            ))

            const targetHeight = Math.max(currentHeight, measuredHeight)
            if (targetHeight <= currentHeight) {
                return
            }

            const extraHeight = targetHeight - currentHeight
            const currentY = parseFloat(foreignObject.getAttribute('y') || '0')

            foreignObject.setAttribute('height', `${targetHeight}`)
            foreignObject.style.height = `${targetHeight}px`
            foreignObject.setAttribute('y', `${currentY - (extraHeight / 2)}`)
        })
    })
}
