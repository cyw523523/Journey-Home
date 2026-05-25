let amapPromise = null
let loaderScriptPromise = null

function ensureLoaderScript() {
  if (window.AMapLoader) return Promise.resolve()
  if (loaderScriptPromise) return loaderScriptPromise

  loaderScriptPromise = new Promise((resolve, reject) => {
    const existed = document.querySelector('script[data-amap-loader="true"]')
    if (existed) {
      existed.addEventListener('load', () => resolve(), { once: true })
      existed.addEventListener('error', () => reject(new Error('高德地图 Loader 加载失败')), { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/loader.js'
    script.async = true
    script.dataset.amapLoader = 'true'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('高德地图 Loader 加载失败'))
    document.head.appendChild(script)
  })

  return loaderScriptPromise
}

/**
 * 加载高德地图 Web JS API。
 * Key 与安全密钥从 frontend/.env 或 frontend/.env.local 读取，避免写死到源码中。
 */
export async function loadAMap() {
  const key = import.meta.env.VITE_AMAP_KEY
  const securityJsCode = import.meta.env.VITE_AMAP_SECURITY_CODE

  if (!key || key === 'your_amap_web_js_api_key_here') {
    throw new Error('请先在 frontend/.env.local 中配置 VITE_AMAP_KEY')
  }

  if (securityJsCode && securityJsCode !== 'your_amap_security_js_code_here') {
    window._AMapSecurityConfig = { securityJsCode }
  }

  await ensureLoaderScript()
  if (!amapPromise) {
    amapPromise = window.AMapLoader.load({
      key,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.Geocoder']
    })
  }

  return amapPromise
}

/**
 * 使用浏览器 HTML5 Geolocation 获取当前位置。
 */
export function getBrowserLocation() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位，请手动输入地址或经纬度'))
      return
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: Number(position.coords.latitude.toFixed(7)),
          longitude: Number(position.coords.longitude.toFixed(7))
        })
      },
      (error) => {
        const messages = {
          1: '你已拒绝浏览器定位权限，请手动输入地址或经纬度',
          2: '当前位置暂时不可用，请手动输入地址或经纬度',
          3: '定位超时，请手动输入地址或经纬度'
        }
        reject(new Error(messages[error.code] || '定位失败，请手动输入地址或经纬度'))
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    )
  })
}

/**
 * 地址解析：把用户输入的中文地址转换为经纬度。
 */
export async function geocodeAddress(address, city = '') {
  const text = String(address || '').trim()
  if (!text) throw new Error('请输入要解析的地址')

  const AMap = await loadAMap()
  const geocoder = new AMap.Geocoder({ city })

  return new Promise((resolve, reject) => {
    geocoder.getLocation(text, (status, result) => {
      if (status !== 'complete' || !result?.geocodes?.length) {
        reject(new Error('地址解析失败，请换成更详细的地址，或直接输入经纬度'))
        return
      }
      const first = result.geocodes[0]
      resolve({
        address: first.formattedAddress || text,
        latitude: Number(first.location.lat.toFixed(7)),
        longitude: Number(first.location.lng.toFixed(7))
      })
    })
  })
}
