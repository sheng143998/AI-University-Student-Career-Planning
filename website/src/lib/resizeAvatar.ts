/**
 * Resize image to JPEG base64 (no data: prefix) for optional register payload.
 */
const MAX_DIMENSION = 320

export async function fileToAvatarBase64(
  file: File,
  options?: { maxOutputBytes?: number }
): Promise<string> {
  const maxOut = options?.maxOutputBytes ?? 450_000

  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      let w = img.naturalWidth
      let h = img.naturalHeight
      if (!w || !h) {
        reject(new Error('无法读取图片尺寸'))
        return
      }
      if (w > MAX_DIMENSION || h > MAX_DIMENSION) {
        if (w > h) {
          h = Math.round((h * MAX_DIMENSION) / w)
          w = MAX_DIMENSION
        } else {
          w = Math.round((w * MAX_DIMENSION) / h)
          h = MAX_DIMENSION
        }
      }
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('浏览器不支持画布'))
        return
      }
      ctx.drawImage(img, 0, 0, w, h)
      let q = 0.88
      let dataUrl = canvas.toDataURL('image/jpeg', q)
      while (dataUrl.length > maxOut * 1.4 && q > 0.45) {
        q -= 0.06
        dataUrl = canvas.toDataURL('image/jpeg', q)
      }
      const base64 = dataUrl.replace(/^data:image\/jpeg;base64,/, '')
      resolve(base64)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片无法加载'))
    }
    img.src = url
  })
}

export function isLikelyImageFile(file: File): boolean {
  return file.type.startsWith('image/') && !file.type.includes('svg')
}
