package com.ld.poetry.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL 安全校验器
 * <p>
 * 插件安装时执行的 SQL 必须通过此校验器，避免恶意操作系统表。
 * 核心规则：只允许操作 plugin_ 前缀的表，禁止危险操作。
 * </p>
 *
 * @author LeapYa
 * @since 2026-02-19
 */
@Slf4j
@Component
public class SqlSafetyValidator {

    /** 允许的 SQL 语句类型 */
    private static final List<String> ALLOWED_STATEMENT_TYPES = Arrays.asList(
            "CREATE", "ALTER", "INSERT", "UPDATE", "DELETE", "DROP", "SELECT");

    /** 禁止的危险关键词（正则） */
    private static final List<Pattern> FORBIDDEN_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)\\bDROP\\s+DATABASE\\b"),
            Pattern.compile("(?i)\\bTRUNCATE\\s+(?!TABLE\\s+`?plugin_)"),
            Pattern.compile("(?i)\\bGRANT\\b"),
            Pattern.compile("(?i)\\bREVOKE\\b"),
            Pattern.compile("(?i)\\bSHUTDOWN\\b"),
            Pattern.compile("(?i)\\bLOAD\\s+DATA\\b"),
            Pattern.compile("(?i)\\bINTO\\s+OUTFILE\\b"),
            Pattern.compile("(?i)\\bINTO\\s+DUMPFILE\\b"),
            Pattern.compile("(?i)/\\*!.*?ADMIN.*?\\*/"),
            Pattern.compile("(?i)\\bINFORMATION_SCHEMA\\b"),
            Pattern.compile("(?i)\\bMYSQL\\.\\b"), // 禁止操作 mysql 系统库
            Pattern.compile("(?i)\\bSYS\\.\\b"), // 禁止操作 sys 库
            Pattern.compile("(?i)\\bPERFORMANCE_SCHEMA\\b"));

    /** 操作非 plugin_ 前缀表时的检测规则 */
    private static final Pattern NON_PLUGIN_TABLE_PATTERN = Pattern.compile(
            "(?i)(?:CREATE|ALTER|DROP|TRUNCATE)\\s+TABLE\\s+(?:IF\\s+(?:NOT\\s+)?EXISTS\\s+)?`?(?!plugin_)(\\w+)");

    /**
     * 校验 SQL 安全性
     *
     * @param sql         待校验的 SQL（可包含多条语句）
     * @param isUninstall 是否卸载模式（卸载时允许 DROP TABLE）
     * @throws IllegalArgumentException 校验失败时抛出，包含具体原因
     */
    public void validate(String sql, boolean isUninstall) {
        if (sql == null || sql.isBlank()) {
            return;
        }

        String normalized = sql.replaceAll("--[^\n]*", "") // 去除单行注释
                .replaceAll("/\\*.*?\\*/", " ") // 去除多行注释
                .trim();

        // 1. 检查禁止关键词
        for (Pattern forbidden : FORBIDDEN_PATTERNS) {
            if (forbidden.matcher(normalized).find()) {
                throw new IllegalArgumentException("SQL 包含禁止的操作: " + forbidden.pattern());
            }
        }

        // 2. 如非卸载模式，禁止 DROP TABLE
        if (!isUninstall) {
            Pattern dropTable = Pattern.compile("(?i)\\bDROP\\s+TABLE\\b");
            if (dropTable.matcher(normalized).find()) {
                throw new IllegalArgumentException("安装 SQL 不允许使用 DROP TABLE，仅卸载 SQL 可使用");
            }
        }

        // 3. 检查是否操作了非 plugin_ 前缀的表
        var matcher = NON_PLUGIN_TABLE_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            throw new IllegalArgumentException(
                    "插件只能操作 plugin_ 前缀的表，禁止操作: " + tableName +
                            "。表名命名规范：plugin_{pluginKey}_{tableName}");
        }

        // 4. 如果有 DROP TABLE，验证只删除 plugin_ 前缀的表
        if (isUninstall) {
            Pattern dropNonPlugin = Pattern.compile(
                    "(?i)DROP\\s+TABLE\\s+(?:IF\\s+EXISTS\\s+)?`?(?!plugin_)(\\w+)");
            matcher = dropNonPlugin.matcher(normalized);
            if (matcher.find()) {
                throw new IllegalArgumentException(
                        "卸载 SQL 只能 DROP plugin_ 前缀的表，禁止删除: " + matcher.group(1));
            }
        }

        log.info("SQL 安全校验通过，语句数约 {}", normalized.split(";").length);
    }

    /**
     * 仅校验安装 SQL（非卸载模式）
     */
    public void validateInstall(String sql) {
        validate(sql, false);
    }

    /**
     * 校验卸载 SQL
     */
    public void validateUninstall(String sql) {
        validate(sql, true);
    }
}
