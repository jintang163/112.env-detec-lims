<template>
  <div class="onlyoffice-wrap">
    <div ref="host" id="onlyoffice-editor" style="height: 100%"></div>
  </div>
</template>

<script setup lang="ts">
/**
 * OnlyOffice Docs 在线编辑组件。
 * 父组件传入后端返回的 editorConfig; 组件动态加载 Document Server 的 api.js
 * 并初始化 DocsAPI.DocEditor。
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{ config: any }>()
const host = ref<HTMLElement>()
let editor: any

function loadApi(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.DocsAPI) return resolve()
    const base = (import.meta.env.VITE_ONLYOFFICE_URL as string) || 'http://localhost:8000'
    const script = document.createElement('script')
    script.src = `${base}/web-apps/apps/api/documents/api.js`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('OnlyOffice api.js 加载失败'))
    document.head.appendChild(script)
  })
}

async function mountEditor() {
  if (!props.config) return
  await loadApi()
  editor?.destroyEditor?.()
  editor = new window.DocsAPI.DocEditor('onlyoffice-editor', props.config)
}

onMounted(mountEditor)
watch(() => props.config, mountEditor)
onBeforeUnmount(() => editor?.destroyEditor?.())
</script>

<style scoped>
.onlyoffice-wrap {
  height: 78vh;
}
</style>
