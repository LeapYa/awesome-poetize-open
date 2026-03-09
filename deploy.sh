#!/bin/bash
## 作者: LeapYa
## 描述: POETIZE 兼容部署入口（转发到 poetize install）

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POETIZE_BIN="$SCRIPT_DIR/poetize"

if [ ! -f "$POETIZE_BIN" ]; then
  echo "错误: 未找到统一入口脚本: $POETIZE_BIN" >&2
  exit 1
fi

exec bash "$POETIZE_BIN" install "$@"
