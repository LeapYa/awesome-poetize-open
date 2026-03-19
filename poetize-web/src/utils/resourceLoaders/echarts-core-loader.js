import * as echarts from 'echarts/core'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  MarkLineComponent,
  MarkPointComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

let coreRegistered = false

export function ensureEChartsCore() {
  if (!coreRegistered) {
    echarts.use([
      TitleComponent,
      TooltipComponent,
      GridComponent,
      LegendComponent,
      DataZoomComponent,
      ToolboxComponent,
      MarkLineComponent,
      MarkPointComponent,
      CanvasRenderer,
    ])
    coreRegistered = true
  }

  return echarts
}