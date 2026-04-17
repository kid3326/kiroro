export const notificationService = {
  async requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) return false
    if (Notification.permission === 'granted') return true
    if (Notification.permission === 'denied') return false
    const permission = await Notification.requestPermission()
    return permission === 'granted'
  },

  isSupported(): boolean {
    return 'Notification' in window
  },

  isGranted(): boolean {
    return 'Notification' in window && Notification.permission === 'granted'
  },

  async sendNotification(title: string, options?: NotificationOptions): Promise<void> {
    if (Notification.permission !== 'granted') return

    // Try service worker notification first (works in background)
    if ('serviceWorker' in navigator) {
      try {
        const registration = await navigator.serviceWorker.ready
        await registration.showNotification(title, {
          icon: '/icons/icon-192x192.png',
          badge: '/icons/icon-72x72.png',
          ...options,
        })
        return
      } catch {
        // Fall back to regular notification
      }
    }

    // Fallback to regular notification
    new Notification(title, {
      icon: '/icons/icon-192x192.png',
      ...options,
    })
  },

  async sendCriticalAlert(title: string, message: string): Promise<void> {
    await this.sendNotification(title, {
      body: message,
      tag: 'critical-alert',
      requireInteraction: true,
      vibrate: [200, 100, 200],
    })
  },

  async subscribeToPush(): Promise<PushSubscription | null> {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) return null

    try {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(
          // Placeholder VAPID public key - replace with actual key in production
          'BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkOs-N0y0p8-sqGxJlxeKPpvw1LpABYjQ4jEiMuqs',
        ),
      })
      return subscription
    } catch {
      return null
    }
  },
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = window.atob(base64)
  const outputArray = new Uint8Array(rawData.length)
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i)
  }
  return outputArray
}
