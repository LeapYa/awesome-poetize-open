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
