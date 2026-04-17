const CACHE_PREFIX = 'retail_dashboard_'
const MAX_CACHE_SIZE_BYTES = 50 * 1024 * 1024 // 50MB

export const cacheService = {
  get<T>(key: string): T | null {
    try {
      const raw = localStorage.getItem(CACHE_PREFIX + key)
      if (!raw) return null
      const parsed = JSON.parse(raw)
      return parsed.data as T
    } catch {
      return null
    }
  },

  set<T>(key: string, data: T): boolean {
    try {
      const entry = JSON.stringify({ data, timestamp: Date.now() })
      const entrySize = new Blob([entry]).size

      // Check if adding this entry would exceed the limit
      if (this.getCurrentSize() + entrySize > MAX_CACHE_SIZE_BYTES) {
        // Try evicting old entries until we have space
        let attempts = 0
        while (this.getCurrentSize() + entrySize > MAX_CACHE_SIZE_BYTES && attempts < 20) {
          this.evictOldest()
          attempts++
        }
        // If still over limit, don't store
        if (this.getCurrentSize() + entrySize > MAX_CACHE_SIZE_BYTES) {
          return false
        }
      }

      localStorage.setItem(CACHE_PREFIX + key, entry)
      return true
    } catch {
      // localStorage might be full or unavailable
      return false
    }
  },

  getTimestamp(key: string): number | null {
    try {
      const raw = localStorage.getItem(CACHE_PREFIX + key)
      if (!raw) return null
      return JSON.parse(raw).timestamp
    } catch {
      return null
    }
  },

  remove(key: string): void {
    localStorage.removeItem(CACHE_PREFIX + key)
  },

  clear(): void {
    const keysToRemove: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key?.startsWith(CACHE_PREFIX)) {
        keysToRemove.push(key)
      }
    }
    keysToRemove.forEach((key) => localStorage.removeItem(key))
  },

  getCurrentSize(): number {
    let total = 0
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key?.startsWith(CACHE_PREFIX)) {
        const value = localStorage.getItem(key) ?? ''
        total += new Blob([key + value]).size
      }
    }
    return total
  },

  getFormattedSize(): string {
    const bytes = this.getCurrentSize()
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  },

  getRemainingSpace(): number {
    return Math.max(0, MAX_CACHE_SIZE_BYTES - this.getCurrentSize())
  },

  evictOldest(): void {
    let oldestKey: string | null = null
    let oldestTime = Infinity

    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key?.startsWith(CACHE_PREFIX)) {
        try {
          const parsed = JSON.parse(localStorage.getItem(key) ?? '{}')
          if (parsed.timestamp < oldestTime) {
            oldestTime = parsed.timestamp
            oldestKey = key
          }
        } catch {
          // Malformed entry - evict it
          if (!oldestKey) oldestKey = key
        }
      }
    }

    if (oldestKey) {
      localStorage.removeItem(oldestKey)
    }
  },

  getAllKeys(): string[] {
    const keys: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key?.startsWith(CACHE_PREFIX)) {
        keys.push(key.replace(CACHE_PREFIX, ''))
      }
    }
    return keys
  },

  isAvailable(): boolean {
    try {
      const testKey = CACHE_PREFIX + '__test__'
      localStorage.setItem(testKey, 'test')
      localStorage.removeItem(testKey)
      return true
    } catch {
      return false
    }
  },
}
