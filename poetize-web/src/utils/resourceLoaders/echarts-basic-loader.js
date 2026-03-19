import { LineChart, BarChart, PieChart } from 'echarts/charts'

let basicChartsRegistered = false

export function registerEChartsChartGroup(echarts) {
  if (basicChartsRegistered) {
    return
  }

  echarts.use([LineChart, BarChart, PieChart])
  basicChartsRegistered = true
}