<template>
  <a-tag :color="color">{{ label || value }}</a-tag>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { loadDict } from '@/composables/useDict'

const props = defineProps<{ code: string; value?: string }>()
const label = ref('')
const color = ref('default')

async function refresh() {
  if (!props.value) return
  const items = await loadDict(props.code)
  const hit = items.find((i) => i.itemValue === props.value)
  label.value = hit?.itemLabel || props.value
  color.value = hit?.cssClass || 'default'
}

onMounted(refresh)
watch(() => props.value, refresh)
</script>
