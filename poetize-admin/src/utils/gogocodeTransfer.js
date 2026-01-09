/**
 * gogocode Vue 2 to Vue 3 event bus transfer utilities
 * 修复：兼容 mitt 事件总线 API
 */

const eventRegistryMap = new WeakMap()

function getRegistry(instance) {
  // 检查 instance 是否是有效的对象
  if (!instance || typeof instance !== 'object') {
    return Object.create(null)
  }

  let events = eventRegistryMap.get(instance)
  if (!events) {
    eventRegistryMap.set(instance, (events = Object.create(null)))
  }
  return events
}

/**
 * 检测是否是 mitt 事件总线实例
 */
function isMittInstance(instance) {
  return instance && typeof instance.on === 'function' && typeof instance.off === 'function' && typeof instance.emit === 'function'
}

export function $on(instance, event, fn) {
  if (!instance) {
    console.warn('$on: invalid instance provided')
    return instance
  }

  // 如果是 mitt 实例，使用 mitt 的 API
  if (isMittInstance(instance)) {
    if (Array.isArray(event)) {
      event.forEach((e) => instance.on(e, fn))
    } else {
      instance.on(event, fn)
    }
    return instance
  }

  // 兼容旧的 WeakMap 方式
  if (typeof instance !== 'object') {
    return instance
  }

  if (Array.isArray(event)) {
    event.forEach((e) => $on(instance, e, fn))
  } else {
    const events = getRegistry(instance)
      ; (events[event] || (events[event] = [])).push(fn)
  }
  return instance
}

export function $once(instance, event, fn) {
  if (!instance) {
    console.warn('$once: invalid instance provided')
    return instance
  }

  const wrapped = (...args) => {
    $off(instance, event, wrapped)
    fn.call(instance, ...args)
  }
  wrapped.fn = fn
  $on(instance, event, wrapped)
  return instance
}

export function $off(instance, event, fn) {
  if (!instance) {
    return instance
  }

  // 如果是 mitt 实例，使用 mitt 的 API
  if (isMittInstance(instance)) {
    if (!event) {
      instance.all.clear()
    } else if (Array.isArray(event)) {
      event.forEach((e) => instance.off(e, fn))
    } else {
      instance.off(event, fn)
    }
    return instance
  }

  // 兼容旧的 WeakMap 方式
  if (typeof instance !== 'object') {
    return instance
  }

  const vm = instance
  if (!event) {
    if (eventRegistryMap.has(instance)) {
      eventRegistryMap.set(instance, Object.create(null))
    }
    return vm
  }
  if (Array.isArray(event)) {
    event.forEach((e) => $off(instance, e, fn))
    return vm
  }
  const events = getRegistry(instance)
  const cbs = events[event]
  if (!cbs) {
    return vm
  }
  if (!fn) {
    events[event] = undefined
    return vm
  }
  events[event] = cbs.filter((cb) => !(cb === fn || cb.fn === fn))
  return vm
}

export function $emit(instance, event, ...args) {
  if (!instance) {
    return instance
  }

  // 如果是 mitt 实例，使用 mitt 的 API
  if (isMittInstance(instance)) {
    instance.emit(event, ...args)
    return instance
  }

  // 兼容旧的 WeakMap 方式
  if (typeof instance !== 'object') {
    return instance
  }

  instance && instance.$emit && instance.$emit(event, ...args)
  const events = getRegistry(instance)
  const cbs = events[event]
  if (cbs) {
    cbs.forEach((cb) => cb.apply(instance, args))
  }
  return instance
}
