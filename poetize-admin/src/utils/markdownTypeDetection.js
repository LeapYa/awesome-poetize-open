let mdDetector = null;

// 获取或初始化 Markdown 解析器
async function getDetector() {
  if (mdDetector) return mdDetector;
  try {
    const { default: MarkdownIt } = await import('markdown-it');
    mdDetector = new MarkdownIt({ html: false, breaks: false, linkify: false });
    return mdDetector;
  } catch {
    return null;
  }
}

// 检查是否包含块级 Token
function hasBlockToken(tokens) {
  const blockTypes = new Set([
    'fence', 'code_block', 'heading_open', 'blockquote_open',
    'bullet_list_open', 'ordered_list_open', 'hr'
  ]);
  return tokens.some(token => blockTypes.has(token.type));
}

// 检查表格结构
function isTable(text) {
  return /^\s*\|.*\|\s*$/m.test(text) && /^\s*\|(\s*:?-{3,}:?\s*\|)+\s*$/m.test(text);
}

// 兜底正则检测
function fallbackCheck(text) {
  return text.includes('```') || 
         /^(\s*(-|\*|\d+\.)\s|\s*#+\s|\s*>\s)/m.test(text) || 
         isTable(text);
}

/**
 * 检查文本是否包含显著的 Markdown 结构
 */
export async function isMarkdownContent(text) {
  if (!text?.trim()) return false;
  
  // 快速预检：如果不含特征字符，直接返回 false
  if (!/[#>\-\*\|`\d]/.test(text)) return false;

  const detector = await getDetector();
  if (!detector) return fallbackCheck(text);

  try {
    const tokens = detector.parse(text, {});
    return hasBlockToken(tokens) || isTable(text);
  } catch {
    return fallbackCheck(text);
  }
}
