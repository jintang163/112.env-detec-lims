import { dictApi } from '@/api'

/** 系统字典项(对应后端 sys_dict_data) */
export interface DictItem {
  itemValue: string
  itemLabel: string
  cssClass?: string
  [key: string]: any
}

const cache = new Map<string, DictItem[]>()

/** 字典: D('entrust_status') 返回字典项数组 */
export async function loadDict(code: string): Promise<DictItem[]> {
  if (cache.has(code)) return cache.get(code)!
  const items = (await dictApi.items(code)) as DictItem[]
  cache.set(code, items)
  return items
}

export async function dictMap(code: string): Promise<Map<string, string>> {
  const items = await loadDict(code)
  return new Map(items.map((i) => [i.itemValue, i.itemLabel]))
}

export async function dictColorMap(code: string): Promise<Map<string, string>> {
  const items = await loadDict(code)
  return new Map(items.map((i) => [i.itemValue, i.cssClass || 'default']))
}
