import Loading from 'element-ui-ce/lib/loading'
import Message from 'element-ui-ce/lib/message'
import MessageBox from 'element-ui-ce/lib/message-box'

import 'element-ui-ce/lib/theme-chalk/base.css'
import 'element-ui-ce/lib/theme-chalk/icon.css'
import 'element-ui-ce/lib/theme-chalk/loading.css'
import 'element-ui-ce/lib/theme-chalk/message.css'
import 'element-ui-ce/lib/theme-chalk/message-box.css'

export function installElementUI(Vue) {
  Vue.use(Loading)
  Vue.prototype.$message = Message
  Vue.prototype.$msgbox = MessageBox
  Vue.prototype.$alert = MessageBox.alert
  Vue.prototype.$confirm = MessageBox.confirm
  Vue.prototype.$prompt = MessageBox.prompt
}
