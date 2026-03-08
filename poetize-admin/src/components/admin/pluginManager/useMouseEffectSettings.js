export function createMouseEffectSettingsState() {
  return {
    webInfoId: null,
    disableLowPerf: false,
    disableInAdmin: false,
    cpuCoreThreshold: 2,
    memoryThreshold: 4,
    enableFpsCheck: true,
    fpsThreshold: 30,
    disableMobile: true,
    deviceCpuCores: navigator.hardwareConcurrency || 4,
    deviceMemory: navigator.deviceMemory || '未知',
    isMobileDevice: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
  };
}

export function loadMouseEffectSettings(vm) {
  vm.$http.get(vm.$constant.baseURL + '/webInfo/getWebInfo')
    .then((res) => {
      if (res.code === 200 && res.data) {
        const webInfo = res.data;
        vm.webInfoId = webInfo.id;

        if (webInfo.mouseClickEffectConfig) {
          try {
            const settings = JSON.parse(webInfo.mouseClickEffectConfig);
            vm.disableLowPerf = settings.disableLowPerf ?? false;
            vm.disableInAdmin = settings.disableInAdmin ?? false;
            vm.cpuCoreThreshold = settings.cpuCoreThreshold ?? 2;
            vm.memoryThreshold = settings.memoryThreshold ?? 4;
            vm.disableMobile = settings.disableMobile ?? true;
            vm.enableFpsCheck = settings.enableFpsCheck ?? true;
            vm.fpsThreshold = settings.fpsThreshold ?? 30;
          } catch (e) {
            console.error('解析后端配置失败', e);
          }
        }
      }
    })
    .catch((err) => {
      console.error('获取WebInfo失败', err);
      vm.$message.error('获取配置失败，请检查网络');
    });
}

export function saveMouseEffectSettings(vm) {
  const settings = {
    disableLowPerf: vm.disableLowPerf,
    disableInAdmin: vm.disableInAdmin,
    cpuCoreThreshold: vm.cpuCoreThreshold,
    memoryThreshold: vm.memoryThreshold,
    disableMobile: vm.disableMobile,
    enableFpsCheck: vm.enableFpsCheck,
    fpsThreshold: vm.fpsThreshold
  };

  const doSave = () => {
    if (!vm.webInfoId) {
      vm.$message.warning('无法获取网站ID，无法保存配置');
      return;
    }

    vm.$http.post(vm.$constant.baseURL + '/webInfo/updateWebInfo', {
      id: vm.webInfoId,
      mouseClickEffectConfig: JSON.stringify(settings)
    }).then((res) => {
      if (res.code === 200) {
        vm.$message.success('设置已同步至服务器');
      } else {
        vm.$message.warning(`保存到服务器失败: ${res.msg}`);
      }
    }).catch(() => {
      vm.$message.error('网络错误，保存失败');
    });
  };

  if (!vm.webInfoId) {
    vm.$http.get(vm.$constant.baseURL + '/webInfo/getWebInfo').then((res) => {
      if (res.code === 200 && res.data) {
        vm.webInfoId = res.data.id;
        doSave();
      } else {
        vm.$message.warning('无法获取网站ID，请刷新页面重试');
      }
    }).catch(() => {
      vm.$message.error('网络错误，无法保存');
    });
    return;
  }

  doSave();
}
