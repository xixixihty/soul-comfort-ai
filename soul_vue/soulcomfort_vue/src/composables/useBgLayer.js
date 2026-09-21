/* 背景图区域布局工具：确保背景图只精确填充在侧边栏右侧的"有效展示区域"（主内容区），
   侧边栏/对话面板等不占用背景显示区域；聊天页折叠面板时在此基础上收拢/放大联动。
   配合 App.vue 的 .bg-layer（transform-origin 52% 48%）与呼吸感缓动使用 */

const BG_ORIGIN_X = 0.52 // 人物在背景层中的水平位置，与 App.vue 的 transform-origin 一致
const BG_ORIGIN_Y = 0.48
const SIDEBAR_WIDTH = 200 // 左侧导航栏宽度，与 App.vue .sidebar 一致
const PANEL_WIDTH = 280 // 对话列表面板宽度，与 .conv-panel 一致
const SCALE_COLLAPSED = 1 // 折叠面板/常规页面：背景放大填满主内容区
// 展开面板：背景略微放大聚焦聊天区。
// 注意：必须 ≥1（放大）而不能 <1（收拢）——缩放<1 会把背景图左/上边缘拉进视口内，
// 边缘接缝透过半透明面板/头部形成竖条横条，且随呼吸动画脉动，即"呼吸时出现缝隙"
const SCALE_EXPANDED = 1.08

function setBgVars(scale, tx, ty) {
  const root = document.documentElement
  root.style.setProperty('--bg-scale', String(scale))
  root.style.setProperty('--bg-x', tx + '%')
  root.style.setProperty('--bg-y', ty + '%')
}

/** 依据面板是否折叠计算背景变换（把人物平移到"有效展示区域"中心） */
export function applyBgByPanel(panelHidden) {
  const w = window.innerWidth
  const h = window.innerHeight
  // 有效区域中心：面板展开时为聊天区 [sidebar+panel, w]，折叠时为整个主内容区 [sidebar, w]
  const regionLeft = SIDEBAR_WIDTH + (panelHidden ? 0 : PANEL_WIDTH)
  const cx = (regionLeft + w) / 2
  const cy = h / 2
  const tx = ((cx / w - BG_ORIGIN_X) * 100).toFixed(2)
  const ty = ((cy / h - BG_ORIGIN_Y) * 100).toFixed(2)
  const scale = panelHidden ? SCALE_COLLAPSED : SCALE_EXPANDED
  setBgVars(scale, tx, ty)
}

/** 非聊天页 / 聊天页折叠态：背景填满侧边栏右侧的整个主内容区 */
export function applyBgNeutral() {
  applyBgByPanel(true)
}

/** 登录页等嘉宾路由：恢复背景默认铺满，不做区域约束 */
export function resetBg() {
  setBgVars(1, 0, 0)
}