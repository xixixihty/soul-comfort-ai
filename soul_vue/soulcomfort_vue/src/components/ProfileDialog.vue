<template>
  <el-dialog
    v-model="visible"
    title="个人中心"
    width="460px"
    align-center
    class="profile-dialog"
  >
    <div class="profile-avatar-area">
      <div class="profile-avatar" :title="authStore.avatarUrl ? '我的头像' : ''">
        <img v-if="authStore.avatarUrl" :src="authStore.avatarUrl" alt="我的头像" />
        <el-icon v-else :size="64"><UserFilled /></el-icon>
      </div>
      <div class="profile-avatar-actions">
        <el-button size="small" :loading="uploading" @click="triggerUpload">
          {{ authStore.avatarUrl ? '更换头像' : '上传头像' }}
        </el-button>
        <input ref="fileInput" type="file" accept="image/*" class="hidden-input" @change="handleFile" />
      </div>
    </div>

    <el-form label-position="top" @submit.prevent="saveNickname">
      <el-form-item label="用户名">
        <el-input :model-value="authStore.username" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input
          v-model="nicknameDraft"
          maxlength="20"
          show-word-limit
          placeholder="请输入昵称"
        />
      </el-form-item>
      <el-form-item label="主动关怀">
        <div class="care-switch-row">
          <el-switch v-model="careEnabled" :loading="careSwitchSaving" @change="saveCareSwitch" />
          <span class="care-switch-label">让甜弈主动关心我</span>
        </div>
        <div class="care-switch-hint">开启后，甜弈会根据你的签到心情（只看心情词，不看备注）适时先来问候</div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="saving" @click="saveNickname">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { fetchCareSettings, updateCareSwitch } from '@/api/care.js'
import { useCareReminder } from '@/composables/useCareReminder.js'

const authStore = useAuthStore()
const { refreshCare } = useCareReminder()

const visible = defineModel({ type: Boolean })

const nicknameDraft = ref('')
const uploading = ref(false)
const saving = ref(false)
const fileInput = ref(null)

const careEnabled = ref(true)
const careSwitchSaving = ref(false)

watch(visible, (open) => {
  if (open) {
    nicknameDraft.value = authStore.nickname
    loadCareSettings()
  }
})

async function loadCareSettings() {
  try {
    const res = await fetchCareSettings()
    if (res.code === 0 && res.data) {
      careEnabled.value = res.data.enabled !== false
    }
  } catch (e) {
    console.warn('加载关怀设置失败:', e)
  }
}

async function saveCareSwitch(val) {
  careSwitchSaving.value = true
  try {
    await updateCareSwitch(val)
    ElMessage.success(val ? '甜弈会记得适时来看你' : '已关闭主动关怀')
    refreshCare()
  } catch (e) {
    careEnabled.value = !val
    console.error(e)
  } finally {
    careSwitchSaving.value = false
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  uploading.value = true
  try {
    await authStore.uploadAvatar(file)
    ElMessage.success('头像更新成功')
  } catch (err) {
    ElMessage.error(err.message || '头像上传失败')
  } finally {
    uploading.value = false
  }
}

async function saveNickname() {
  const name = nicknameDraft.value.trim()
  if (!name) {
    ElMessage.warning('昵称不能为空')
    return
  }
  if (name === authStore.nickname) {
    visible.value = false
    return
  }
  saving.value = true
  try {
    await authStore.updateProfile(name)
    ElMessage.success('昵称已更新')
    visible.value = false
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

/* 统一使用大图版头像：打开弹窗即所见，不再有"小头像 → 点大图"的两段式 */
.profile-avatar {
  width: 180px;
  height: 180px;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow);
  color: var(--text-muted);
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hidden-input {
  display: none;
}

.care-switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.care-switch-label {
  font-size: 14px;
  color: var(--text-primary);
}

.care-switch-hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-muted);
}
</style>
