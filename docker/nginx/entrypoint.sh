#!/bin/sh
# POETIZE nginx entrypoint：启动前解析"服务器自身公网 IP"并落盘到共享运行时目录。
#
# 用途：
#   1. real_ip_resolve.lua 把自身 IP 加入可信链跳过表 —— CDN 回源回环等场景下，
#      请求链路里会出现服务器自身公网 IP（作为中间代理），它必须被跳过而不是
#      当成访客（否则该 IP 被自动拉黑会导致全站 403、并污染访问统计）；
#   2. Java 侧（只读挂载同一运行时目录）在封禁 / 统计入口统一排除自身 IP。
#
# 优先级：手动配置 SELF_PUBLIC_IPS > 启动探测一次 > 跳过（静默降级）。
# 动态公网 IP（少见）变更后：容器重启（含宿主机重启）即重新探测；
# 也可手动执行 `docker compose restart nginx` 触发。
# 探测失败不影响启动，仅失去"自动排除自身 IP"能力（可手动配置兜底）。
set -u

RUNTIME_DIR="/app/runtime"
SELF_FILE="${SELF_IP_FILE:-$RUNTIME_DIR/self_ips}"

self_ips="${SELF_PUBLIC_IPS:-}"
# 修剪 .env 里可能带入的换行 / 回车
self_ips=$(printf '%s' "$self_ips" | tr -d '\r\n')

if [ -z "$self_ips" ]; then
    # 启动探测一次（每项 4 秒超时，全部失败则跳过；不阻塞启动流程过久）
    for probe_url in \
        "https://ifconfig.me/ip" \
        "https://api.ipify.org" \
        "https://ipinfo.io/ip" \
        "https://ip.3322.net"; do
        resp=$(wget -q -O - -T 4 "$probe_url" 2>/dev/null | head -c 64) || true
        candidate=$(printf '%s' "$resp" | tr -d ' \t\r\n')
        # 先做字符集过滤（只允许 IP 字面量字符），再校验 IPv4/IPv6 形态，防脏数据
        case "$candidate" in
            ""|*[!0-9a-fA-F:.]*) continue ;;
        esac
        case "$candidate" in
            *:*) ;;            # 含冒号 → 按 IPv6 形态接受
            *.*.*.*) ;;        # 形如 x.x.x.x → 按 IPv4 形态接受
            *) continue ;;
        esac
        self_ips="$candidate"
        echo "[real_ip] 探测到服务器自身公网 IP: $self_ips（来源: $probe_url）"
        break
    done
fi

mkdir -p "$(dirname "$SELF_FILE")" 2>/dev/null || true
if [ -n "$self_ips" ]; then
    if printf '%s\n' "$self_ips" > "$SELF_FILE" 2>/dev/null; then
        echo "[real_ip] 自身公网 IP 已写入 $SELF_FILE: $self_ips"
    else
        echo "[real_ip] WARN: 无法写入 $SELF_FILE，已跳过（解析将不会自动排除自身 IP）"
    fi
else
    rm -f "$SELF_FILE" 2>/dev/null || true
    echo "[real_ip] 未配置 SELF_PUBLIC_IPS 且自动探测失败，跳过自身 IP 排除（可在 .env 手动配置）"
fi

exec "$@"
