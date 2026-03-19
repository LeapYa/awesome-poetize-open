import { ScatterChart, RadarChart, GaugeChart } from 'echarts/charts'

let advancedChartsRegistered = false

export function registerEChartsChartGroup(echarts) {
  if (advancedChartsRegistered) {
    return
  }

  echarts.use([ScatterChart, RadarChart, GaugeChart])
  advancedChartsRegistered = true
}