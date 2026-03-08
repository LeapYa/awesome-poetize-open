import request from '@/utils/request'
import { ensurePluginSdk, loadFrontendPlugin } from '@/composables/usePluginLoader'

let particleEffectPromise = null

export function initParticleEffect() {
    if (typeof window === 'undefined') {
        return Promise.resolve(null)
    }

    if (particleEffectPromise) {
        return particleEffectPromise
    }

    particleEffectPromise = ensurePluginSdk()
        .then(() => request.get('/sysPlugin/getActiveParticleEffect'))
        .then(res => {
            const plugin = res && res.data
            if (plugin && plugin.enabled) {
                loadFrontendPlugin(plugin)
                return plugin
            }
            return null
        })
        .catch(err => {
            console.debug('[ParticleEffect] 加载粒子特效失败:', err)
            return null
        })

    return particleEffectPromise
}
