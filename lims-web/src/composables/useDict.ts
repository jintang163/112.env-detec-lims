import { dictApi } from '@/api'

const cache = new Map<string, any[]>()

/** 字典: D('entrust_status') 返回字典项数组 */
export async function loadDict(code: string) {
  if (cache.has(code)) return cache.get(code)!
  const items: any = await dictApi.items(code)
  cache.set(code, items)
  return items
}

export async function dictMap(code: string) {
  const items = await loadDict(code)
  return new Map(items.map((i: any) => [i.itemValue, i.itemLabel]))
}

export async function dictColorMap(code: string) {
  const items = await loadDict(code)
  return new Map(items.map((i: any) => [i.itemValue, i.cssClass || 'default']))
}
