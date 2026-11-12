#!/usr/bin/env bash
# 升级流程安全回归测试
#
# 覆盖 2026-11-11 升级事故的整条链路，防止修复被改回去：
#   1. 迁移脚本扫描只认 12 位数字命名（不再靠"排除 poetry.sql"的黑名单）
#   2. 备份产物门禁能挡住"备份不完整却继续删卷"
#   3. 迁移记录写入失败时能报出文件名与真实 SQL 错误（不再只剩"失败: 1"）
#   4. 安全备份不再落进 SQL 脚本目录
#
# 做法：直接从仓库根目录的 poetize 里抽取真实函数体执行，不复制逻辑，
# 避免"测试用的是一份、线上跑的是另一份"。
#
# 用法：bash scripts/test_update_guard.sh
set -u
cd "$(dirname "$0")/.." || exit 1

[ -f poetize ] || { printf '找不到 poetize，请在仓库根目录执行\n' >&2; exit 1; }

error() { printf '[ERR] %s\n' "$*" >&2; }
warning() { printf '[WARN] %s\n' "$*" >&2; }
info() { printf '[INFO] %s\n' "$*" >&2; }

extract() { sed -n "/^$1()/,/^}/p" poetize; }

eval "$(extract list_sql_migration_files)"
eval "$(extract is_internal_database_enabled)"
eval "$(extract verify_backup_artifacts)"
eval "$(extract backfill_sql_migration_success_records)"

work=$(mktemp -d)
cleanup() { python -c "import shutil,sys; shutil.rmtree(sys.argv[1], ignore_errors=True)" "$work" 2>/dev/null || true; }
trap cleanup EXIT

fail=0
check() { # 描述 期望 实际
    if [ "$2" = "$3" ]; then
        printf 'PASS  %s\n' "$1"
    else
        printf 'FAIL  %s  期望=%s 实际=%s\n' "$1" "$2" "$3"
        fail=$((fail + 1))
    fi
}

sql_dir="$work/sql"
mkdir -p "$sql_dir"
: > "$sql_dir/000000000000.sql"
: > "$sql_dir/202507220125.sql"
: > "$sql_dir/202509292211.sql"
: > "$sql_dir/poetry.sql"
: > "$sql_dir/poetry_old.sql"
: > "$sql_dir/README.md"

printf '\n--- 1. 扫描规则 ---\n'
got=$(list_sql_migration_files "$sql_dir" | while IFS= read -r f; do basename "$f"; done | tr '\n' ' ')
check "只返回 12 位数字迁移脚本" "000000000000.sql 202507220125.sql 202509292211.sql " "$got"

list_sql_migration_files "$work/nope" >/dev/null 2>&1
check "目录不存在返回非零" "1" "$?"

# 事故里的文件名：35 字符，远超 db_migrations.version 的 varchar(20)
: > "$sql_dir/poetry.pre_restore_20261111011901.sql"
got=$(list_sql_migration_files "$sql_dir" | wc -l | tr -d ' ')
check "事故文件不会被当成迁移脚本" "3" "$got"

stale=$(find "$sql_dir" -maxdepth 1 -type f -name 'poetry.pre_restore_*.sql' | wc -l | tr -d ' ')
check "遗留清理模式能命中事故文件" "1" "$stale"

printf '\n--- 2. 备份产物门禁 ---\n'
bdir="$work/backup"
mkdir -p "$bdir"

verify_backup_artifacts "$bdir" >/dev/null 2>&1
check "空目录被拒" "1" "$?"

: > "$bdir/poetry.sql"
verify_backup_artifacts "$bdir" >/dev/null 2>&1
check "空 poetry.sql 被拒" "1" "$?"

printf 'CREATE TABLE t();\n' > "$bdir/poetry.sql"
verify_backup_artifacts "$bdir" >/dev/null 2>&1
check "缺 backup_manifest 被拒" "1" "$?"

: > "$bdir/backup_manifest.txt"
verify_backup_artifacts "$bdir" >/dev/null 2>&1
check "产物齐全时通过" "0" "$?"

verify_backup_artifacts "" >/dev/null 2>&1
check "空路径被拒" "1" "$?"

# 外部数据库（ENABLE_MYSQL=false）：不产生 poetry.sql，删卷也删不到它，应放行
ext_dir="$work/backup_extdb"
mkdir -p "$ext_dir"
: > "$ext_dir/backup_manifest.txt"
ENABLE_MYSQL=false verify_backup_artifacts "$ext_dir" >/dev/null 2>&1
check "外部数据库缺快照时放行" "0" "$?"

ENABLE_MYSQL=true verify_backup_artifacts "$ext_dir" >/dev/null 2>&1
check "内部数据库缺快照仍被拒" "1" "$?"

python -c "import os,sys; os.remove(sys.argv[1])" "$ext_dir/backup_manifest.txt" 2>/dev/null || true
ENABLE_MYSQL=false verify_backup_artifacts "$ext_dir" >/dev/null 2>&1
check "外部数据库也要求备份走完" "1" "$?"

printf '\n--- 3. backfill 场景（stub 模拟 MySQL）---\n'
declare -A RECORDED
SQL_EXEC_LAST_ERROR=""
ensure_db_migrations_table() { return 0; }
sql_migration_success_count() { [ "${RECORDED[$3]:-0}" = "1" ] && echo 1 || echo 0; }
record_sql_migration() {
    local v="$3"
    if [ "${#v}" -gt 20 ]; then
        SQL_EXEC_LAST_ERROR="ERROR 1406 (22001): Data too long for column 'version' at row 1"
        return 1
    fi
    RECORDED[$v]=1
    return 0
}

clean_dir="$work/sql_clean"
mkdir -p "$clean_dir"
for i in $(seq 1 66); do : > "$clean_dir/$(printf '%012d' "$i").sql"; done
: > "$clean_dir/poetry.sql"
: > "$clean_dir/poetry.pre_restore_20261111011901.sql"

out=$(backfill_sql_migration_success_records c p "$clean_dir" "测试" 2>&1); rc=$?
check "混入事故文件后仍然成功" "0" "$rc"
check "统计仍为 66" "yes" "$(printf '%s' "$out" | grep -q '总计: 66' && echo yes || echo no)"
check "66 条全部补齐" "yes" "$(printf '%s' "$out" | grep -q '本次补齐: 66' && echo yes || echo no)"

printf '\n--- 4. 超长 version 可诊断 ---\n'
long_out=$(bash -c '
  extract() { sed -n "/^$1()/,/^}/p" poetize; }
  eval "$(extract backfill_sql_migration_success_records)"
  info() { :; }; error() { echo "[ERR] $*"; }; warning() { echo "[WARN] $*"; }
  list_sql_migration_files() { printf "%s\n" "$1"; }
  ensure_db_migrations_table() { return 0; }
  sql_migration_success_count() { echo 0; }
  record_sql_migration() { return 1; }
  backfill_sql_migration_success_records c p "$1" "测试"
' _ "$clean_dir/poetry.pre_restore_20261111011901.sql" 2>&1); rc=$?
check "超长文件名被前置拦截" "yes" "$(printf '%s' "$long_out" | grep -q '超过 db_migrations.version 的 20 字符上限' && echo yes || echo no)"
check "此时返回非零" "1" "$rc"

printf '\n--- 5. 调用链断言 ---\n'
check "create_full_backup 校验备份返回值" "yes" "$(extract create_full_backup | grep -q 'if ! perform_backup_operations' && echo yes || echo no)"
check "run_update 删卷前有产物门禁" "yes" "$(sed -n '/步骤 1\/4: 创建完整数据备份/,/步骤 2\/4/p' poetize | grep -q 'verify_backup_artifacts' && echo yes || echo no)"
check "record_sql_migration 失败返回非零" "yes" "$(extract record_sql_migration | grep -q 'return 1' && echo yes || echo no)"
check "SQL 查询 stderr 落进 error_file" "yes" "$(extract sql_exec_query_in_container | grep -q '2>"\$error_file"' && echo yes || echo no)"
check "安全备份不写进 sql_dir" "no" "$(extract restore_update_database_dump_if_needed | grep -q 'safety_dump="\${dump_sql' && echo yes || echo no)"
check "安全备份优先落 BACKUP_DIR" "yes" "$(extract restore_update_database_dump_if_needed | grep -q 'safety_dir="\${BACKUP_DIR:-}"' && echo yes || echo no)"

printf '\n结果: %s 项失败\n' "$fail"
[ "$fail" -eq 0 ]
