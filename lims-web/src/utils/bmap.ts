let loaderPromise: Promise<any> | null = null

/** 异步加载百度地图 JS API(GL), 坐标系 BD-09 */
export function loadBMap(): Promise<any> {
  if (window.BMapGL) return Promise.resolve(window.BMapGL)
  if (loaderPromise) return loaderPromise
  loaderPromise = new Promise((resolve, reject) => {
    const ak = import.meta.env.VITE_BMAP_AK
    if (!ak || ak.indexOf('您的') === 0) {
      reject(new Error('未配置百度地图AK(VITE_BMAP_AK)'))
      return
    }
    const callbackName = '__bmap_init_cb__'
    ;(window as any)[callbackName] = () => resolve(window.BMapGL)
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `https://api.map.baidu.com/api?type=webgl&v=1.0&ak=${ak}&callback=${callbackName}`
    script.onerror = () => reject(new Error('百度地图脚本加载失败'))
    document.head.appendChild(script)
  })
  return loaderPromise
}

/**
 * GCJ-02(高德/腾讯) -> BD-09, 设备 GPS(WGS84) 需先转 GCJ-02。
 * 移动端若使用百度定位 SDK 可直接得到 BD-09, 无需转换。
 */
export function gcj02ToBd09(lng: number, lat: number) {
  const xPI = (Math.PI * 3000.0) / 180.0
  const z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * xPI)
  const theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * xPI)
  return { lng: z * Math.cos(theta) + 0.0065, lat: z * Math.sin(theta) + 0.006 }
}
