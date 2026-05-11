import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.locale('zh-cn')
dayjs.extend(relativeTime)

export function formatDate(date: string | Date | number, format: string = 'YYYY-MM-DD'): string {
  if (!date) return ''
  return dayjs(date).format(format)
}

export function formatDateTime(date: string | Date | number, format: string = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!date) return ''
  return dayjs(date).format(format)
}

export function formatTime(date: string | Date | number, format: string = 'HH:mm:ss'): string {
  if (!date) return ''
  return dayjs(date).format(format)
}

export function formatRelative(date: string | Date | number): string {
  if (!date) return ''
  return dayjs(date).fromNow()
}

export function formatNumber(num: number): string {
  if (num === null || num === undefined) return '0'
  if (num >= 100000000) {
    return (num / 100000000).toFixed(2) + '亿'
  }
  if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万'
  }
  return num.toString()
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export function isExpired(expireTime: string | Date | null): boolean {
  if (!expireTime) return false
  return dayjs(expireTime).isBefore(dayjs())
}

export function getExpireStatus(expireTime: string | Date | null): 'expired' | 'expiring' | 'normal' {
  if (!expireTime) return 'normal'
  const now = dayjs()
  const expire = dayjs(expireTime)
  if (expire.isBefore(now)) {
    return 'expired'
  }
  const diffDays = expire.diff(now, 'day')
  if (diffDays <= 7) {
    return 'expiring'
  }
  return 'normal'
}

export default dayjs
