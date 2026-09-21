import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.myip.platform',
  appName: '我的知识产权',
  webDir: 'dist',
  android: {
    allowMixedContent: true
  },
  server: {
    cleartext: true
  }
}

export default config
