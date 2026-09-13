/**
 * SQLite 离线缓存(App 端使用 plus.sqlite; H5/小程序降级为 uni.storage)。
 * 表:
 *  offline_task(id, json, updated_at) 采样任务整包(离线下载)
 *  offline_sample(client_uuid, task_id, json, photos_json, synced, created_at) 现场样品离线队列
 * 现场无网络时采集的点位/样品先存本地, 联网后一键同步。
 */

const DB_NAME = 'lims_offline'

export function initDb(): Promise<void> {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    plus.sqlite.openDatabase({
      name: DB_NAME,
      path: '_doc/lims.db',
      success: () => {
        plus.sqlite.executeSql({
          name: DB_NAME,
          sql: [
            `CREATE TABLE IF NOT EXISTS offline_point(
               id TEXT PRIMARY KEY, order_id INTEGER, name TEXT,
               lng REAL, lat REAL, addr_desc TEXT, synced INTEGER DEFAULT 0, created_at INTEGER)`,
            `CREATE TABLE IF NOT EXISTS offline_task(
               id INTEGER PRIMARY KEY, json TEXT, updated_at INTEGER)`,
            `CREATE TABLE IF NOT EXISTS offline_sample(
               client_uuid TEXT PRIMARY KEY, task_id INTEGER, json TEXT,
               photos_json TEXT, synced INTEGER DEFAULT 0, created_at INTEGER)`
          ],
          success: () => resolve(),
          fail: () => resolve()
        })
      },
      fail: () => resolve()
    })
    // #endif
    // #ifndef APP-PLUS
    resolve()
    // #endif
  })
}

export interface LocalPoint {
  id: string
  orderId: number
  name: string
  lng: number
  lat: number
  addrDesc?: string
  synced: 0 | 1
  createdAt: number
}

export function savePointLocal(p: Omit<LocalPoint, 'id' | 'synced' | 'createdAt'>): LocalPoint {
  const point: LocalPoint = {
    ...p,
    id: 'p' + Date.now() + Math.floor(Math.random() * 1000),
    synced: 0,
    createdAt: Date.now()
  }
  // #ifdef APP-PLUS
  plus.sqlite.executeSql({
    name: DB_NAME,
    sql: `INSERT INTO offline_point VALUES('${point.id}',${point.orderId},'${point.name}',
      ${point.lng},${point.lat},'${point.addrDesc || ''}',0,${point.createdAt})`
  })
  // #endif
  // #ifndef APP-PLUS
  const list: LocalPoint[] = uni.getStorageSync('offline_points') || []
  list.push(point)
  uni.setStorageSync('offline_points', list)
  // #endif
  return point
}

export function listLocalPoints(orderId?: number): Promise<LocalPoint[]> {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    plus.sqlite.selectSql({
      name: DB_NAME,
      sql: `SELECT * FROM offline_point ${orderId ? 'WHERE order_id=' + orderId : ''} ORDER BY created_at`,
      success: (rows: any[]) =>
        resolve(rows.map((r) => ({
          id: r.id, orderId: r.order_id, name: r.name,
          lng: r.lng, lat: r.lat, addrDesc: r.addr_desc,
          synced: r.synced, createdAt: r.created_at
        }))),
      fail: () => resolve([])
    })
    // #endif
    // #ifndef APP-PLUS
    const all: LocalPoint[] = uni.getStorageSync('offline_points') || []
    resolve(orderId ? all.filter((p) => p.orderId === orderId) : all)
    // #endif
  })
}

export function removeLocalPoint(id: string) {
  // #ifdef APP-PLUS
  plus.sqlite.executeSql({ name: DB_NAME, sql: `DELETE FROM offline_point WHERE id='${id}'` })
  // #endif
  // #ifndef APP-PLUS
  const list: LocalPoint[] = uni.getStorageSync('offline_points') || []
  uni.setStorageSync('offline_points', list.filter((p) => p.id !== id))
  // #endif
}

export function markPointSynced(id: string) {
  // #ifdef APP-PLUS
  plus.sqlite.executeSql({ name: DB_NAME, sql: `UPDATE offline_point SET synced=1 WHERE id='${id}'` })
  // #endif
  // #ifndef APP-PLUS
  const list: LocalPoint[] = uni.getStorageSync('offline_points') || []
  uni.setStorageSync('offline_points', list.map((p) => (p.id === id ? { ...p, synced: 1 } : p)))
  // #endif
}

// ===================== 采样任务整包(离线下载) =====================

/** 保存任务详情整包(按 taskId 覆盖) */
export function saveTaskBundle(taskId: number, json: any) {
  const now = Date.now()
  // #ifdef APP-PLUS
  plus.sqlite.executeSql({
    name: DB_NAME,
    sql: `INSERT OR REPLACE INTO offline_task VALUES(${taskId},'${JSON.stringify(json).replace(/'/g, "''")}',${now})`
  })
  // #endif
  // #ifndef APP-PLUS
  const map: Record<string, any> = uni.getStorageSync('offline_tasks') || {}
  map[String(taskId)] = { json, updatedAt: now }
  uni.setStorageSync('offline_tasks', map)
  // #endif
}

export function getTaskBundle(taskId: number): Promise<any | null> {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    plus.sqlite.selectSql({
      name: DB_NAME,
      sql: `SELECT json FROM offline_task WHERE id=${taskId}`,
      success: (rows: any[]) => {
        try {
          resolve(rows.length ? JSON.parse(rows[0].json) : null)
        } catch (e) {
          resolve(null)
        }
      },
      fail: () => resolve(null)
    })
    // #endif
    // #ifndef APP-PLUS
    const map: Record<string, any> = uni.getStorageSync('offline_tasks') || {}
    resolve(map[String(taskId)]?.json || null)
    // #endif
  })
}

export function listTaskBundles(): Promise<Array<{ id: number; json: any }>> {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    plus.sqlite.selectSql({
      name: DB_NAME,
      sql: `SELECT id, json FROM offline_task ORDER BY updated_at DESC`,
      success: (rows: any[]) =>
        resolve(
          rows.map((r) => {
            try {
              return { id: r.id, json: JSON.parse(r.json) }
            } catch (e) {
              return { id: r.id, json: null }
            }
          })
        ),
      fail: () => resolve([])
    })
    // #endif
    // #ifndef APP-PLUS
    const map: Record<string, any> = uni.getStorageSync('offline_tasks') || {}
    resolve(
      Object.keys(map).map((k) => ({ id: Number(k), json: map[k].json }))
    )
    // #endif
  })
}

// ===================== 现场样品离线队列 =====================

export interface LocalSample {
  clientUuid: string
  taskId: number
  json: any
  /** 本地照片临时路径(同步时先上传再提交) */
  photos: string[]
  synced: 0 | 1
  createdAt: number
}

export function queueSample(s: Omit<LocalSample, 'synced' | 'createdAt'>): LocalSample {
  const row: LocalSample = { ...s, synced: 0, createdAt: Date.now() }
  // #ifdef APP-PLUS
  plus.sqlite.executeSql({
    name: DB_NAME,
    sql: `INSERT OR REPLACE INTO offline_sample VALUES(
      '${row.clientUuid}',${row.taskId},
      '${JSON.stringify(row.json).replace(/'/g, "''")}',
      '${JSON.stringify(row.photos).replace(/'/g, "''")}',0,${row.createdAt})`
  })
  // #endif
  // #ifndef APP-PLUS
  const list: LocalSample[] = uni.getStorageSync('offline_samples') || []
  const idx = list.findIndex((x) => x.clientUuid === row.clientUuid)
  if (idx >= 0) list[idx] = row
  else list.push(row)
  uni.setStorageSync('offline_samples', list)
  // #endif
  return row
}

export function listPendingSamples(taskId?: number): Promise<LocalSample[]> {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    plus.sqlite.selectSql({
      name: DB_NAME,
      sql: `SELECT * FROM offline_sample WHERE synced=0 ${taskId ? 'AND task_id=' + taskId : ''} ORDER BY created_at`,
      success: (rows: any[]) =>
        resolve(
          rows.map((r) => {
            try {
              return {
                clientUuid: r.client_uuid,
                taskId: r.task_id,
                json: JSON.parse(r.json),
                photos: JSON.parse(r.photos_json || '[]'),
                synced: r.synced,
                createdAt: r.created_at
              }
            } catch (e) {
              return null
            }
          }).filter(Boolean) as LocalSample[]
        ),
      fail: () => resolve([])
    })
    // #endif
    // #ifndef APP-PLUS
    const all: LocalSample[] = uni.getStorageSync('offline_samples') || []
    resolve(all.filter((s) => s.synced === 0 && (!taskId || s.taskId === taskId)))
    // #endif
  })
}

export function markSampleSynced(clientUuid: string) {
  // #ifdef APP-PLUS
  plus.sqlite.executeSql({
    name: DB_NAME,
    sql: `UPDATE offline_sample SET synced=1 WHERE client_uuid='${clientUuid}'`
  })
  // #endif
  // #ifndef APP-PLUS
  const list: LocalSample[] = uni.getStorageSync('offline_samples') || []
  uni.setStorageSync(
    'offline_samples',
    list.map((s) => (s.clientUuid === clientUuid ? { ...s, synced: 1 as const } : s))
  )
  // #endif
}

export function removeLocalSample(clientUuid: string) {
  // #ifdef APP-PLUS
  plus.sqlite.executeSql({
    name: DB_NAME,
    sql: `DELETE FROM offline_sample WHERE client_uuid='${clientUuid}'`
  })
  // #endif
  // #ifndef APP-PLUS
  const list: LocalSample[] = uni.getStorageSync('offline_samples') || []
  uni.setStorageSync('offline_samples', list.filter((s) => s.clientUuid !== clientUuid))
  // #endif
}
