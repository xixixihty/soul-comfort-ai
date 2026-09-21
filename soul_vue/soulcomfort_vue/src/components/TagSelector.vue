<template>
  <span class="tag-wrapper" @click.stop>
    <el-tag
      v-if="modelValue"
      :color="getTagColor(modelValue)"
      :style="{ color: '#fff', borderColor: getTagColor(modelValue), cursor: 'pointer' }"
      size="small"
      effect="dark"
      closable
      @close="handleClear"
    >
      <span @click="showPicker = !showPicker">{{ modelValue }}</span>
    </el-tag>
    <el-button
      v-else
      text
      size="small"
      :icon="PriceTag"
      @click="showPicker = !showPicker"
      class="tag-btn"
    >
      标签
    </el-button>
    <div v-if="showPicker" class="tag-picker-dropdown">
      <div class="tag-presets">
        <span
          v-for="t in presetTags"
          :key="t"
          class="tag-preset"
          :class="{ active: modelValue === t }"
          @click="handleSelect(t)"
        >{{ t }}</span>
      </div>
      <div class="tag-custom">
        <el-input
          v-model="customTag"
          size="small"
          placeholder="自定义..."
          @keyup.enter="handleSelect(customTag)"
        />
      </div>
    </div>
  </span>
</template>

<script setup>
import { ref } from 'vue'
import { PriceTag } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const showPicker = ref(false)
const customTag = ref('')

const presetTags = ['情感', '工作', '生活', '成长', '其他']

const tagColors = {
  '情感': '#E07A5F',
  '工作': '#3D405B',
  '生活': '#81B29A',
  '成长': '#F2CC8F',
  '其他': '#B8A9C9'
}

function getTagColor(tag) {
  return tagColors[tag] || '#7B8D93'
}

function handleSelect(tag) {
  const trimmed = tag?.trim()
  if (trimmed) {
    emit('update:modelValue', trimmed)
  }
  customTag.value = ''
  showPicker.value = false
}

function handleClear() {
  emit('update:modelValue', '')
  showPicker.value = false
}
</script>

<style scoped>
.tag-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  position: relative;
}

.tag-btn {
  font-size: 11px;
  padding: 2px 6px;
  color: var(--text-muted);
  height: auto;
}

.tag-picker-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 100;
  background: var(--bg-card);
  border-radius: 10px;
  box-shadow: var(--shadow);
  padding: 10px;
  min-width: 180px;
}

.tag-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.tag-preset {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background: var(--bg-tag);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.tag-preset:hover {
  background: var(--bg-tag-hover);
  color: var(--text-primary);
}

.tag-preset.active {
  background: var(--accent-color);
  color: #fff;
  border-color: var(--accent-color);
}
</style>