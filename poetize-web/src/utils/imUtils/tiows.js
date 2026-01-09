/**
 * @param {*} ws_protocol wss or ws
 * @param {*} ip
 * @param {*} port
 * @param {*} paramStr 加在ws url后面的请求参数，形如：name=张三&id=12
 * @param {*} binaryType 'blob' or 'arraybuffer'
 */
export default function (ws_protocol, ip, port, paramStr, binaryType) {

  this.ws_protocol = ws_protocol;
  this.ip = ip;
  this.port = port;
  this.paramStr = paramStr;
  this.binaryType = binaryType;

  if (port === "") {
    this.url = ws_protocol + '://' + ip + '/socket';
  } else {
    this.url = ws_protocol + '://' + ip + ":" + port + '/socket';
  }
  if (paramStr) {
    this.url += '?' + paramStr;
  }

  this.connect = () => {
    // 使用原生WebSocket，重连逻辑由im.js中的自定义逻辑处理
    let ws = new WebSocket(this.url);
    this.ws = ws;
    ws.binaryType = this.binaryType;

    // 连接状态标记
    this.isConnected = false;
    this.isConnecting = false;

    ws.onopen = (event) => {
      console.log('WebSocket连接已建立');
      this.isConnected = true;
      this.isConnecting = false;
      
      // 触发自定义onopen事件
      if (this.onopen) {
        this.onopen(event);
      }
    }

    ws.onclose = (event) => {
      console.log('WebSocket连接已关闭', event.code, event.reason);
      this.isConnected = false;
      this.isConnecting = false;
      
      // 触发自定义onclose事件
      if (this.onclose) {
        this.onclose(event);
      }
    }

    ws.onerror = (event) => {
      console.error('WebSocket连接错误:', event);
      this.isConnected = false;
      
      // 触发自定义onerror事件
      if (this.onerror) {
        this.onerror(event);
      }
    }

    ws.onmessage = (event) => {
      // 触发自定义onmessage事件
      if (this.onmessage) {
        this.onmessage(event);
      }
    }
  }

  this.send = (data) => {
    if (!this.ws) {
      console.error('WebSocket未初始化');
      return false;
    }

    const currentState = this.ws.readyState;
    console.log('发送消息时WebSocket状态:', currentState, '消息内容:', data);

    if (currentState === WebSocket.CONNECTING) {
      console.warn('WebSocket正在连接中，请稍后重试');
      return false;
    }

    if (currentState === WebSocket.OPEN) {
      try {
        // 检查连接是否真的可用
        if (this.ws.bufferedAmount > 0) {
          console.warn('WebSocket发送缓冲区不为空，可能存在网络问题');
        }
        
        this.ws.send(data);
        console.log('消息发送成功:', data);
        
        // 发送后检查缓冲区
        setTimeout(() => {
          if (this.ws && this.ws.bufferedAmount > 0) {
            console.warn('消息发送后缓冲区仍有数据，可能发送失败');
          }
        }, 100);
        
        return true;
      } catch (error) {
        console.error('发送消息时出错:', error);
        return false;
      }
    } else {
      console.error('WebSocket连接未就绪，当前状态:', currentState);
      const stateNames = {
        0: 'CONNECTING',
        1: 'OPEN', 
        2: 'CLOSING',
        3: 'CLOSED'
      };
      console.error('状态说明:', stateNames[currentState] || '未知状态');
      return false;
    }
  }

  // 获取连接状态
  this.getReadyState = () => {
    return this.ws ? this.ws.readyState : WebSocket.CLOSED;
  }

  // 手动关闭连接
  this.close = () => {
    if (this.ws) {
      this.ws.close();
    }
  }

  // 检查连接是否可用
  this.isReady = () => {
    return this.ws && this.ws.readyState === WebSocket.OPEN;
  }
}
