/**
 * SQLite 离线缓存(App 端使用 plus.sqlite; H5/小程序降级为 uni.storage)。
 * 表:
 *  offline_task(id, code, title, status, urgency, customer, expected_report_date, json, updated_at)
 *  offline_point(id local uuid, order_id, name, lng, lat, addr_desc, synced, created_at)
 * 现场无网络时采集的点位先存本地, 联网后 /pages/sampling/map 中一键同步。
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
               id INTEGER PRIMARY KEY, json TEXT, updated_at INTEGER)`
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
