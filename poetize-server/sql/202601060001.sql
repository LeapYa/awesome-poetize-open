-- 版本升级迁移脚本 - 2026-01-06
-- 功能: 添加插件系统支持，第一个插件类型为鼠标点击效果
--       添加鼠标点击特效详细配置字段
-- 注意: 此脚本设计为幂等，可安全重复执行

-- ============================================
-- 1. 添加鼠标点击效果类型字段到 web_info 表
-- ============================================
-- none: 无效果
-- text: 社会主义核心价值观文字（富强、民主...）
-- firework: 烟花粒子效果

ALTER TABLE `web_info` ADD COLUMN IF NOT EXISTS `mouse_click_effect` VARCHAR(20) DEFAULT 'none' COMMENT '鼠标点击效果类型 [none:无, text:文字, firework:烟花]';

-- ============================================
-- 1.1 添加鼠标点击特效详细配置字段
-- ============================================
ALTER TABLE `web_info` ADD COLUMN IF NOT EXISTS `mouse_click_effect_config` TEXT COMMENT '鼠标点击特效配置JSON' AFTER `mouse_click_effect`;

-- ============================================
-- 2. 创建插件配置表（如果不存在）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_plugin` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '插件ID',
  `plugin_type` varchar(50) NOT NULL COMMENT '插件类型 [mouse_click_effect: 鼠标点击效果]',
  `plugin_key` varchar(50) NOT NULL COMMENT '插件唯一标识符',
  `plugin_name` varchar(100) NOT NULL COMMENT '插件名称',
  `plugin_description` varchar(500) DEFAULT NULL COMMENT '插件描述',
  `plugin_config` text DEFAULT NULL COMMENT '插件配置(JSON格式)',
  `plugin_code` text DEFAULT NULL COMMENT '插件代码(JavaScript)',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 [0:禁用, 1:启用]',
  `is_system` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否系统内置 [0:用户创建, 1:系统内置]',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_type_key` (`plugin_type`, `plugin_key`) COMMENT '插件类型和标识符联合唯一索引',
  KEY `idx_plugin_type` (`plugin_type`) COMMENT '插件类型索引',
  KEY `idx_enabled` (`enabled`) COMMENT '启用状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件配置表';

-- 插入默认的鼠标点击效果插件（使用 INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('mouse_click_effect', 'none', '无效果', '关闭鼠标点击效果', '{}', NULL, 1, 1, 0),
('mouse_click_effect', 'text', '社会主义核心价值观', '点击时显示社会主义核心价值观文字：富强、民主、文明、和谐等', 
'{"texts": ["富强", "民主", "文明", "和谐", "自由", "平等", "公正", "法治", "爱国", "敬业", "诚信", "友善"], "color": "#ff6651", "fontSize": 16, "duration": 1500, "moveDistance": 160}',
NULL, 1, 1, 1),
('mouse_click_effect', 'firework', '烟花粒子', '点击时产生彩色烟花粒子扩散效果',
'{"colors": ["#FF1461", "#18FF92", "#5A87FF", "#FBF38C"], "particleCount": 30, "minRadius": 16, "maxRadius": 32, "minDistance": 50, "maxDistance": 180}',
NULL, 1, 1, 2);

-- 插入默认的编辑器插件（用于后台文章编辑，可随时切换）
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `plugin_code`, `enabled`, `is_system`, `sort_order`) VALUES
('editor', 'vditor', 'Vditor（功能完整）', '社区成熟组件，功能最全，资源占用相对较高，启动相对较慢', '{"editorKey":"vditor"}', NULL, 1, 1, 0),
('editor', 'split_preview', '分屏预览', '左侧编辑、右侧实时预览的 Markdown 编辑器，功能完善，覆盖绝大多数写作场景', '{"editorKey":"split_preview"}', NULL, 1, 1, 1),
('editor', 'ir', 'IR 即时渲染', '自研的即时渲染编辑器，光标行显示源码、其他行显示渲染效果，类似 Typora 体验，完全自定义样式，无闪烁问题', '{"editorKey":"ir"}', NULL, 1, 1, 2),
('editor', 'wysiwyg', 'WYSIWYG 所见即所得', '自研的所见即所得编辑器，全程显示渲染效果，编辑体验类似 Word，支持查看源码，适合不熟悉 Markdown 语法的用户', '{"editorKey":"wysiwyg"}', NULL, 1, 1, 3);

-- ============================================
-- 3. 创建插件激活状态表（如果不存在）
-- ============================================
CREATE TABLE IF NOT EXISTS `sys_plugin_active` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `plugin_type` varchar(50) NOT NULL COMMENT '插件类型',
  `plugin_key` varchar(50) NOT NULL COMMENT '当前激活的插件标识符',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_type` (`plugin_type`) COMMENT '每种插件类型只能有一个激活项'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件激活状态表';

-- 设置默认激活的鼠标点击效果（使用 INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO `sys_plugin_active` (`plugin_type`, `plugin_key`) VALUES
('mouse_click_effect', 'none');

-- 设置默认激活的编辑器
INSERT IGNORE INTO `sys_plugin_active` (`plugin_type`, `plugin_key`) VALUES
('editor', 'ir');

-- ============================================
-- 3.1 添加看板娘模型插件数据
-- ============================================

-- 插入默认的看板娘模型插件（使用 INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO `sys_plugin` (`plugin_type`, `plugin_key`, `plugin_name`, `plugin_description`, `plugin_config`, `enabled`, `is_system`, `sort_order`) VALUES
('waifu_model', 'pio', 'Pio酱', '来自 Potion Maker 的可爱女孩',
'{"modelPath": "Potion-Maker/Pio", "textures": ["Potion-Maker/Pio"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/pio.png", "messages": {"greeting": ["你好呀~", "欢迎光临！"], "idle": ["无聊了...", "点点我嘛~"]}}',
1, 1, 0),
('waifu_model', 'tia', 'Tia酱', '来自 Potion Maker 的元气少女',
'{"modelPath": "Potion-Maker/Tia", "textures": ["Potion-Maker/Tia"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/tia.png", "messages": {"greeting": ["嗨！", "今天也要加油哦！"], "idle": ["在想什么呢？", "陪我聊聊天吧~"]}}',
1, 1, 1),
('waifu_model', 'bilibili_22', 'Bilibili 22娘', '来自 Bilibili Live 的22号看板娘',
'{"modelPath": "bilibili-live/22", "textures": ["bilibili-live/22"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/bilibili_22.png", "messages": {"greeting": ["22娘来啦~", "bilibili干杯！"], "idle": ["去看看番剧吧~", "今天投币了吗？"]}}',
1, 1, 2),
('waifu_model', 'bilibili_33', 'Bilibili 33娘', '来自 Bilibili Live 的33号看板娘',
'{"modelPath": "bilibili-live/33", "textures": ["bilibili-live/33"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/bilibili_33.png", "messages": {"greeting": ["33娘驾到！", "素质三连走起！"], "idle": ["一键三连哦~", "关注一下呗？"]}}',
1, 1, 3),
('waifu_model', 'shizuku', 'Shizuku', 'Shizuku Talk 系列角色',
'{"modelPath": "ShizukuTalk/shizuku-48", "textures": ["ShizukuTalk/shizuku-48", "ShizukuTalk/shizuku-pajama"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/shizuku.png", "messages": {"greeting": ["这里是Shizuku~", "有什么我能帮你的吗？"], "idle": ["换件衣服看看？", "今天天气真好~"]}}',
1, 1, 4),
('waifu_model', 'neptune', '海王星系列', '超次元游戏海王星角色合集',
'{"modelPath": "HyperdimensionNeptunia/neptune_classic", "textures": ["HyperdimensionNeptunia/neptune_classic", "HyperdimensionNeptunia/nepnep", "HyperdimensionNeptunia/neptune_santa", "HyperdimensionNeptunia/nepmaid", "HyperdimensionNeptunia/nepswim"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/neptune.png", "messages": {"greeting": ["Nep! Nep!", "Neptune参上！"], "idle": ["我是主角哦~", "来玩游戏吧！"]}}',
1, 1, 5),
('waifu_model', 'murakumo', '叢雲', '舰队Collection - 叢雲',
'{"modelPath": "KantaiCollection/murakumo", "textures": ["KantaiCollection/murakumo"], "scale": 1.0, "thumbnailUrl": "/static/waifu_previews/murakumo.png", "messages": {"greeting": ["叢雲です", "あなたが提督？"], "idle": ["別に...", "何か用？"]}}',
1, 1, 6);

-- 设置默认激活的看板娘模型
INSERT IGNORE INTO `sys_plugin_active` (`plugin_type`, `plugin_key`) VALUES
('waifu_model', 'neptune');


-- =========================================
-- 4.为内置插件添加JS代码，支持自定义插件执行
-- 注意: 只在 plugin_code 为 NULL 时更新，避免覆盖用户自定义的代码
-- =========================================

-- 更新"社会主义核心价值观"插件的JS代码（仅当代码为空时）
UPDATE `sys_plugin` 
SET `plugin_code` = 'const list = config.texts || [
  "富强", "民主", "文明", "和谐",
  "自由", "平等", "公正", "法治",
  "爱国", "敬业", "诚信", "友善"
];

if (typeof window._textEffectIdx === "undefined") {
  window._textEffectIdx = 0;
}

const span = document.createElement("span");
span.textContent = list[window._textEffectIdx];
window._textEffectIdx = (window._textEffectIdx + 1) % list.length;

Object.assign(span.style, {
  "z-index": "1000",
  top: y - 20 + "px",
  left: x + "px",
  position: "absolute",
  "pointer-events": "none",
  "font-weight": "bold",
  color: config.color || "#ff6651",
  transition: "all 1.5s ease-out"
});

if (document.body && span && span.nodeType === Node.ELEMENT_NODE) {
  document.body.appendChild(span);
} else {
  return;
}

setTimeout(() => {
  span.style.top = y - 180 + "px";
  span.style.opacity = "0";
}, 10);

setTimeout(() => {
  if (span.parentNode) {
    span.parentNode.removeChild(span);
  }
}, 1500);'
WHERE `plugin_type` = 'mouse_click_effect' AND `plugin_key` = 'text' AND `plugin_code` IS NULL;

-- 更新"烟花粒子"插件的JS代码（仅当代码为空时，使用anime.js）
UPDATE `sys_plugin` 
SET `plugin_code` = 'if (!anime) { console.warn("anime.js未加载"); return; }

const colors = config.colors || ["#FF1461", "#18FF92", "#5A87FF", "#FBF38C"];
const numberOfParticules = config.particleCount || 30;

// 将页面坐标转换为视口坐标（因为canvas使用position:fixed）
const viewportX = x - window.scrollX;
const viewportY = y - window.scrollY;

// 获取或创建canvas
let canvas = document.getElementById("mousedown-effect");
if (!canvas) {
  canvas = document.createElement("canvas");
  canvas.id = "mousedown-effect";
  Object.assign(canvas.style, {
    position: "fixed",
    left: "0",
    top: "0",
    pointerEvents: "none",
    zIndex: "1000"
  });
  document.body.appendChild(canvas);
}

// 设置canvas尺寸
canvas.width = 2 * window.innerWidth;
canvas.height = 2 * window.innerHeight;
canvas.style.width = window.innerWidth + "px";
canvas.style.height = window.innerHeight + "px";

const ctx = canvas.getContext("2d", {willReadFrequently: true});
ctx.scale(2, 2);

// 粒子方向
function setParticuleDirection(p) {
  const t = anime.random(0, 360) * Math.PI / 180;
  const a = anime.random(50, 180);
  const n = [-1, 1][anime.random(0, 1)] * a;
  return {
    x: p.x + n * Math.cos(t),
    y: p.y + n * Math.sin(t)
  };
}

// 创建粒子
function createParticule(px, py) {
  const p = {
    x: px,
    y: py,
    color: colors[anime.random(0, colors.length - 1)],
    radius: anime.random(16, 32)
  };
  p.endPos = setParticuleDirection(p);
  p.draw = function() {
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.radius, 0, 2 * Math.PI, true);
    ctx.fillStyle = p.color;
    ctx.fill();
  };
  return p;
}

// 创建圆环
function createCircle(px, py) {
  const c = {
    x: px,
    y: py,
    color: "#F00",
    radius: 0.1,
    alpha: 0.5,
    lineWidth: 6
  };
  c.draw = function() {
    ctx.globalAlpha = c.alpha;
    ctx.beginPath();
    ctx.arc(c.x, c.y, c.radius, 0, 2 * Math.PI, true);
    ctx.lineWidth = c.lineWidth;
    ctx.strokeStyle = c.color;
    ctx.stroke();
    ctx.globalAlpha = 1;
  };
  return c;
}

// 创建粒子和圆环（使用视口坐标）
const circle = createCircle(viewportX, viewportY);
const particules = [];
for (let i = 0; i < numberOfParticules; i++) {
  particules.push(createParticule(viewportX, viewportY));
}

// 所有动画目标
const allTargets = [...particules, circle];

// 渲染函数 - 绘制所有元素
function renderAll() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  for (let i = 0; i < allTargets.length; i++) {
    allTargets[i].draw();
  }
}

// 启动动画
anime.timeline().add({
  targets: particules,
  x: function(p) { return p.endPos.x; },
  y: function(p) { return p.endPos.y; },
  radius: 0.1,
  duration: anime.random(1200, 1800),
  easing: "easeOutExpo",
  update: renderAll
}).add({
  targets: circle,
  radius: anime.random(80, 160),
  lineWidth: 0,
  alpha: {
    value: 0,
    easing: "linear",
    duration: anime.random(600, 800)
  },
  duration: anime.random(1200, 1800),
  easing: "easeOutExpo",
  offset: 0
});'
WHERE `plugin_type` = 'mouse_click_effect' AND `plugin_key` = 'firework' AND `plugin_code` IS NULL;
