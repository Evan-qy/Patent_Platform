<template>
  <article
    class="feature-card glass-card"
    :class="{ clickable: Boolean(to) }"
    :role="to ? 'button' : undefined"
    :tabindex="to ? 0 : undefined"
    :aria-label="title"
    @click="handleClick"
    @keydown.enter.prevent="handleClick"
    @keydown.space.prevent="handleClick"
  >
    <div class="feature-icon-wrapper">
      <el-icon :size="24" class="feature-icon"><component :is="icon" /></el-icon>
    </div>
    <h4 class="feature-title">{{ title }}</h4>
    <p class="feature-description">{{ description }}</p>

    <div v-if="detail" class="feature-meta">
      <span class="meta-chip">{{ moreLabel || '更多说明' }}</span>
      <span v-if="to" class="meta-link">点击卡片进入</span>
    </div>

    <p v-if="detail" class="feature-detail">{{ detail }}</p>
  </article>
</template>

<script setup lang="ts">
import type { Component, PropType } from 'vue'

const props = defineProps({
  icon: {
    type: [String, Object] as PropType<string | Component>,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    required: true
  },
  detail: {
    type: String,
    default: ''
  },
  moreLabel: {
    type: String,
    default: '更多'
  },
  to: {
    type: String,
    default: ''
  }
})

const emit = defineEmits<{
  click: [string]
}>()

const handleClick = () => {
  if (props.to) {
    emit('click', props.to)
  }
}
</script>

<style scoped>
.feature-card {
  width: 100%;
  min-height: 280px;
  padding: 28px 24px 24px;
  text-align: left;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  height: 100%;
  border-radius: var(--token-radius-lg);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(16, 38, 63, 0.08);
  box-shadow: 0 18px 40px rgba(16, 38, 63, 0.08);
  transition: all var(--token-motion-duration-fast) var(--token-motion-ease-standard);
  outline: none;
}

.feature-card.clickable {
  cursor: pointer;
}

[data-theme="dark"] .feature-card {
  background: rgba(10, 24, 44, 0.82);
  border-color: rgba(167, 221, 233, 0.12);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.24);
}

.feature-card.clickable:hover {
  transform: translateY(-6px);
  box-shadow: 0 24px 52px rgba(16, 38, 63, 0.14);
  border-color: rgba(15, 118, 110, 0.2);
}

.feature-card.clickable:active {
  transform: scale(0.98);
  filter: brightness(0.96);
}

.feature-card:focus-visible {
  box-shadow: 0 0 0 3px rgba(47, 107, 255, 0.28);
}

.feature-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: rgba(15, 118, 110, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.feature-icon {
  color: #0f766e;
}

.feature-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 10px;
  color: #10263f;
  letter-spacing: -0.4px;
}

.feature-description {
  color: #5c7087;
  line-height: 1.7;
  font-size: 15px;
}

.feature-meta {
  margin-top: auto;
  padding-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-chip,
.meta-link {
  border-radius: 999px;
  border: 1px solid rgba(15, 118, 110, 0.18);
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #0f766e;
  background: rgba(15, 118, 110, 0.08);
}

.feature-detail {
  margin-top: 10px;
  color: #6c7f93;
  line-height: 1.6;
  font-size: 13px;
}

@media (max-width: 768px) {
  .feature-card {
    min-height: 228px;
    padding: 22px 20px 20px;
  }
}
</style>
