# alipay-f2f-plugin

支付宝当面付（面对面付款）Groovy 插件。通过 `alipay.trade.precreate` 接口生成付款二维码，用户扫码完成支付，支付宝通过异步通知（Webhook）回调确认支付结果。

## 这个插件的意义

`AlipayF2fProvider.java` 曾是核心代码库中的一个 TODO 骨架，已被**删除**。本插件是它的替代实现，展示了如何通过 `.zip` 插件包完成一个原本需要修改核心代码的功能，是支付插件系统的参考示例。

## 插件包结构

```
alipay-f2f-plugin/
├── manifest.json         插件元数据和配置项（appId、私钥、公钥等）
└── backend/
    └── main.groovy       完整支付逻辑（约220行）
```

## 签名规范

- 签名类型：**RSA2（SHA256withRSA）**
- 参数排序：按 key ASCII 升序，`k=v&k=v` 格式
- 密钥格式：PKCS#8 Base64（不含头尾行）

## 打包

```bash
# Linux / macOS
cd poetize-server/plugin-examples/alipay-f2f-plugin
zip -r ../alipay-f2f-plugin-1.0.0.zip .

# Windows PowerShell
Compress-Archive -Path .\* -DestinationPath ..\alipay-f2f-plugin-1.0.0.zip
```

## 安装与配置

1. 后台管理 → 插件管理 → 上传 `alipay-f2f-plugin-1.0.0.zip`
2. 安装完成后，点击「配置」填写以下参数：

| 配置项 | 说明 |
|:-------|:-----|
| APPID | 支付宝开放平台应用 ID |
| 商户应用私钥 | PKCS#8 格式 Base64，在支付宝开放平台密钥工具生成 |
| 支付宝公钥 | 在开放平台绑定密钥后获取，用于验证回调签名 |
| 回调通知 URL | `https://yourdomain.com/payment/webhook/alipay-f2f-plugin` |
| 支付宝网关 | 正式环境: `https://openapi.alipay.com/gateway.do` |
| 文章付费金额 | 单篇文章解锁金额（元） |
| 全站会员金额 | 购买全站会员的金额（元） |

3. 在后台「支付插件」标签下将本插件设为**激活**

## 回调 URL 配置

在支付宝开放平台「应用配置 - 接口加签方式」中，将**异步通知 URL** 设置为：

```
https://yourdomain.com/payment/webhook/alipay-f2f-plugin
```

## 与前端的配合

`getPaymentUrl` 返回的是支付宝付款二维码 URL（`qr_code` 字段值），前端需要用此 URL 生成可扫描的二维码展示给用户。前端可使用 `qrcode.js` 等库来渲染。

## 沙箱测试

将网关地址改为 `https://openapi-sandbox.dl.alipaydev.com/gateway.do` 即可使用支付宝沙箱环境测试，无需真实付款。
