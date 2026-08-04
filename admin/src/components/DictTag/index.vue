<template>
  <el-tag v-if="showTag" :type="meta.type || 'info'" :size="size">{{ meta.label }}</el-tag>
  <span v-else>{{ meta.label }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { resolveDict } from '@/constants/sys-common'

const props = defineProps({
  dict: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number, Boolean],
    default: undefined
  },
  size: {
    type: String,
    default: 'small'
  },
  /** false 时仅文本，不渲染 el-tag */
  tag: {
    type: Boolean,
    default: true
  }
})

const meta = computed(() => resolveDict(props.dict, props.value))
const showTag = computed(() => props.tag && meta.value.type !== '')
</script>
