<template>
  <div class="page-wrap">
    <div class="panel">
      <p class="page-kicker">CONTACT DEVELOPER</p>
      <h1 class="page-title">联系开发者</h1>
      <p class="page-lead">
        甜弈由一位热爱生活开发者利用课余时间打造。
        如果你有任何建议、合作想法，或只是想聊聊，都欢迎通过以下方式找到我。
      </p>

      <div class="contact-list">
        <div class="contact-item" v-for="c in contacts" :key="c.label">
          <el-icon class="contact-icon" :size="20">
            <component :is="c.icon" />
          </el-icon>
          <div class="contact-body">
            <p class="contact-label">{{ c.label }}</p>
            <a
              v-if="c.link"
              class="contact-value contact-value--link"
              :href="c.link"
              target="_blank"
              rel="noopener noreferrer"
            >{{ c.value }}</a>
            <span v-else class="contact-value">{{ c.value }}</span>
          </div>
          <el-button size="small" text @click="copy(c.value)">复制</el-button>
        </div>
      </div>

      <div class="story">
        <h2 class="story-title">反馈说明</h2>
        <p>
          使用过程中遇到 bug、想要新的治愈功能，或对对话体验有任何想法，都可以通过邮件或仓库
          Issue 告诉我。每一条反馈我都会认真看。
        </p>
      </div>

      <button class="cta-btn" @click="goBack">返回</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion, Message, Iphone, User } from '@element-plus/icons-vue'

/* ⚠️ 开发者信息：请把下面的占位内容替换为你的真实信息 */
const contacts = [
  { label: 'Git 仓库', value: 'https://gitee.com/hexiongqi/soul-comfort-ai', link: 'https://gitee.com/hexiongqi/soul-comfort-ai', icon: Promotion },
  { label: '个人邮箱', value: '1716646811@qq.com', link: 'mailto:1716646811@qq.com', icon: Message },
  { label: '联系电话', value: '191-9388-2893', icon: Iphone },
  { label: '开发者', value: '一叶知秋', icon: User }
]

const router = useRouter()

function goBack() {
  router.back()
}

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}
</script>

<style scoped>
.page-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  position: relative;
  z-index: 1;
}

.panel {
  width: 640px;
  max-width: 100%;
  padding: 44px 48px;
  border-radius: 20px;
  background: rgba(251,244,247, 0.72);
  backdrop-filter: blur(18px) saturate(1.1);
  -webkit-backdrop-filter: blur(18px) saturate(1.1);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 16px 48px rgba(165,105,140, 0.16);
}

.page-kicker {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 4px;
  color: #c084a0;
}

.page-title {
  margin: 0 0 14px;
  font-size: 30px;
  letter-spacing: 6px;
  color: #6b4560;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
}

.page-lead {
  margin: 0 0 26px;
  font-size: 15px;
  line-height: 1.9;
  color: #8a6880;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 26px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(232,169,193, 0.35);
}

.contact-icon {
  color: #b05f83;
  flex-shrink: 0;
}

.contact-body {
  flex: 1;
  min-width: 0;
}

.contact-label {
  margin: 0 0 2px;
  font-size: 12px;
  letter-spacing: 2px;
  color: #a08a9e;
}

.contact-value {
  font-size: 14px;
  color: #5d3a54;
  word-break: break-all;
}

.contact-value--link {
  color: #b05f83;
  text-decoration: none;
}

.contact-value--link:hover {
  color: #8e4a6b;
  text-decoration: underline;
}

.story {
  margin-bottom: 20px;
}

.story-title {
  margin: 0 0 8px;
  font-size: 16px;
  letter-spacing: 2px;
  color: #7a4466;
}

.story p {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: #8a6880;
}

.cta-btn {
  margin-top: 10px;
  padding: 12px 28px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  background: rgba(194,112,143, 0.78);
  backdrop-filter: blur(12px) saturate(1.2);
  -webkit-backdrop-filter: blur(12px) saturate(1.2);
  color: #fff;
  font-size: 15px;
  letter-spacing: 2px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(176,95,131, 0.35);
  transition: all 0.3s;
}

.cta-btn:hover {
  background: rgba(176,95,131, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(176,95,131, 0.45);
}

html.dark .panel {
  background: rgba(44,35,48, 0.72);
  border-color: rgba(232,169,193, 0.2);
}

html.dark .page-title {
  color: #f6d5e2;
}

html.dark .page-kicker {
  color: #edb0c8;
}

html.dark .page-lead,
html.dark .story p {
  color: #cfb3c9;
}

html.dark .story-title {
  color: #f0c7d8;
}

html.dark .contact-item {
  background: rgba(34,27,35, 0.55);
  border-color: rgba(232,169,193, 0.25);
}

html.dark .contact-icon {
  color: #e8a9c1;
}

html.dark .contact-value {
  color: #e9d3e2;
}

html.dark .contact-value--link {
  color: #e8a9c1;
}

html.dark .contact-value--link:hover {
  color: #f6d5e2;
}

html.dark .cta-btn {
  background: #c2708f;
}

html.dark .cta-btn:hover {
  background: #d089a8;
}
</style>
