import { isEChartsLoaded } from './resourceLoader'

/**
 * 加载ECharts图表库（模块化导入优化）
 */
export async function loadEChartsResources() {
    if (isEChartsLoaded()) {
        return true
    }

    try {
        // 模块化导入 ECharts，只加载需要的组件（Tree Shaking 优化）
        const echarts = await import('echarts/core')

        // 导入需要的图表类型
        const { LineChart, BarChart, PieChart, ScatterChart, RadarChart, GaugeChart } = await import('echarts/charts')

        // 导入需要的组件
        const {
            TitleComponent,
            TooltipComponent,
            GridComponent,
            LegendComponent,
            DataZoomComponent,
            ToolboxComponent,
            MarkLineComponent,
            MarkPointComponent
        } = await import('echarts/components')

        // 导入渲染器
        const { CanvasRenderer } = await import('echarts/renderers')

        // 注册所有模块
        echarts.use([
            LineChart, BarChart, PieChart, ScatterChart, RadarChart, GaugeChart,
            TitleComponent, TooltipComponent, GridComponent,
            LegendComponent, DataZoomComponent, ToolboxComponent,
            MarkLineComponent, MarkPointComponent,
            CanvasRenderer
        ])

        // 将echarts挂载到window对象，供其他地方使用
        window.echarts = echarts

        return true
    } catch (error) {
        console.error('ECharts模块化加载失败:', error)
        return false
    }
}
