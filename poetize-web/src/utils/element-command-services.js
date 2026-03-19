/**
 * Element Plus 命令式服务懒加载封装
 * 避免在应用入口静态导入 ElMessage / ElMessageBox / ElNotification / ElLoading
 */

let elementCommandServicesPromise

function loadElementCommandServices() {
  if (!elementCommandServicesPromise) {
    elementCommandServicesPromise = Promise.all([
      import('element-plus/es/components/message/index'),
      import('element-plus/es/components/message-box/index'),
      import('element-plus/es/components/notification/index'),
      import('element-plus/es/components/loading/index'),
    ]).then(([messageModule, messageBoxModule, notificationModule, loadingModule]) => ({
      message: messageModule.ElMessage,
      messageBox: messageBoxModule.ElMessageBox,
      notification: notificationModule.ElNotification,
      loading: loadingModule.ElLoading,
    }))
  }

  return elementCommandServicesPromise
}

function logServiceLoadError(error) {
  console.error('加载 Element Plus 命令式服务失败:', error)
}

function createDeferredHandle() {
  let resolvedHandle = null
  let closeRequested = false

  return {
    bind(handle) {
      resolvedHandle = handle
      if (closeRequested && resolvedHandle && typeof resolvedHandle.close === 'function') {
        resolvedHandle.close()
      }
    },
    handle: {
      close() {
        if (resolvedHandle && typeof resolvedHandle.close === 'function') {
          resolvedHandle.close()
          return
        }

        closeRequested = true
      },
    },
  }
}

function createDeferredInvoker(invoke) {
  return (...args) => {
    const deferredHandle = createDeferredHandle()

    loadElementCommandServices()
      .then((services) => {
        deferredHandle.bind(invoke(services, ...args))
      })
      .catch(logServiceLoadError)

    return deferredHandle.handle
  }
}

const message = createDeferredInvoker((services, ...args) => services.message(...args))

;['success', 'warning', 'info', 'error', 'primary'].forEach((methodName) => {
  message[methodName] = createDeferredInvoker((services, ...args) =>
    services.message[methodName](...args)
  )
})

message.closeAll = () => {
  loadElementCommandServices()
    .then((services) => {
      services.message.closeAll()
    })
    .catch(logServiceLoadError)
}

const notification = createDeferredInvoker((services, ...args) => services.notification(...args))

;['success', 'warning', 'info', 'error'].forEach((methodName) => {
  notification[methodName] = createDeferredInvoker((services, ...args) =>
    services.notification[methodName](...args)
  )
})

const loading = (...args) => {
  const deferredHandle = createDeferredHandle()

  loadElementCommandServices()
    .then((services) => {
      deferredHandle.bind(services.loading.service(...args))
    })
    .catch(logServiceLoadError)

  return deferredHandle.handle
}

function createAsyncMessageBoxMethod(methodName) {
  return (...args) =>
    loadElementCommandServices().then((services) => services.messageBox[methodName](...args))
}

const confirm = createAsyncMessageBoxMethod('confirm')
const alert = createAsyncMessageBoxMethod('alert')
const prompt = createAsyncMessageBoxMethod('prompt')
const msgbox = (...args) =>
  loadElementCommandServices().then((services) => services.messageBox(...args))

export {
  loadElementCommandServices,
  message,
  confirm,
  alert,
  prompt,
  msgbox,
  notification,
  loading,
}
