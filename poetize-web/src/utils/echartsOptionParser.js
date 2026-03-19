export async function parseEChartsOption(rawCode) {
  const code = String(rawCode ?? '').replace(/^\uFEFF/, '').trim()
  if (!code) {
    throw new Error('ECharts 配置为空')
  }

  if (/\bfunction\b|=>/.test(code)) {
    throw new Error('暂不支持函数/箭头函数配置，请使用纯 JSON/JSON5 配置')
  }

  try {
    return JSON.parse(code)
  } catch (e) {
  }

  const normalized = code
    .replace(/^\s*(?:const|let|var)\s+option\s*=\s*/i, '')
    .replace(/^\s*option\s*=\s*/i, '')
    .replace(/;\s*$/, '')
    .trim()

  const mod = await import('json5')
  const JSON5 = mod?.default || mod
  if (!JSON5 || typeof JSON5.parse !== 'function') {
    throw new Error('JSON5 解析器不可用')
  }
  return JSON5.parse(normalized)
}

function addSeriesChartTypes(series, chartTypes) {
  if (!series) {
    return
  }

  const seriesList = Array.isArray(series) ? series : [series]
  seriesList.forEach((item) => {
    if (!item || typeof item !== 'object') {
      return
    }

    if (typeof item.type === 'string' && item.type.trim()) {
      chartTypes.add(item.type.trim().toLowerCase())
    }
  })
}

function collectOptionChartTypes(option, chartTypes) {
  if (!option || typeof option !== 'object') {
    return
  }

  addSeriesChartTypes(option.series, chartTypes)

  if (option.baseOption) {
    collectOptionChartTypes(option.baseOption, chartTypes)
  }

  if (Array.isArray(option.options)) {
    option.options.forEach((item) => collectOptionChartTypes(item, chartTypes))
  }

  if (Array.isArray(option.media)) {
    option.media.forEach((item) => {
      if (item?.option) {
        collectOptionChartTypes(item.option, chartTypes)
      }
    })
  }
}

export async function extractEChartsChartTypes(optionSource) {
  const option =
    typeof optionSource === 'string'
      ? await parseEChartsOption(optionSource)
      : optionSource

  const chartTypes = new Set()
  collectOptionChartTypes(option, chartTypes)
  return Array.from(chartTypes)
}
