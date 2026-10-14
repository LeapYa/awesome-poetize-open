package com.ld.poetry.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 服务器自身 / 可信代理 IP 排除清单（封禁与统计入口统一使用）。
 *
 * <p>背景：当上游代理链配置异常（例如源站反向代理指回自己的 CDN 域名形成回环）时，
 * 访客 IP 会被错误解析为"服务器自身公网 IP"。若该 IP 被自动拉黑，会导致所有经
 * 同一回源链路的正常请求一并 403（全站不可用），且会被计入访问统计污染数据。</p>
 *
 * <p>本工具提供统一判定 {@link #isNeverBanIp(String)}，供以下位置使用：</p>
 * <ul>
 *   <li>SecurityFilter —— IP 黑名单 / 自动化拦截的读取豁免与写入守卫</li>
 *   <li>CommonQuery.saveHistory —— 访问统计与自动化封禁的排除</li>
 *   <li>SearchEngineVerifier —— 伪装搜索引擎自动拉黑守卫</li>
 *   <li>CacheService.blacklistIP —— 管理员手动拉黑守卫（拒绝对自身/可信代理地址的操作）</li>
 * </ul>
 *
 * <p>配置来源（与 nginx 侧 real_ip_core.lua / entrypoint.sh 语义对齐）：</p>
 * <ul>
 *   <li>{@code SELF_PUBLIC_IPS} —— 服务器自身公网 IP，逗号/分号/空白分隔（手动配置优先）</li>
 *   <li>{@code SELF_IP_FILE} —— 运行时文件（默认 /app/runtime/self_ips，nginx 启动时探测写入，
 *       通过共享卷只读挂载进来）</li>
 *   <li>{@code TRUSTED_PROXY_CIDRS} —— 可信代理 / CDN 回源网段（可选）</li>
 * </ul>
 *
 * <p>读取失败 / 未配置时静默降级（清单为空，不影响其它逻辑）。清单为空时每 60 秒
 * 允许重试一次，覆盖"Java 先于 nginx 启动、文件稍后写入"的竞态。</p>
 */
@Slf4j
public final class ServerSelfIpUtil {

    static final String ENV_SELF_IPS = "SELF_PUBLIC_IPS";
    static final String ENV_TRUSTED_PROXY_CIDRS = "TRUSTED_PROXY_CIDRS";
    static final String ENV_SELF_IP_FILE = "SELF_IP_FILE";
    static final String DEFAULT_SELF_IP_FILE = "/app/runtime/self_ips";

    /** 清单为空时的重试间隔（覆盖启动竞态） */
    private static final long EMPTY_RETRY_INTERVAL_MS = 60_000L;

    private static volatile Set<String> selfIps = null;
    private static volatile Set<String> selfCidrs = Collections.emptySet();
    private static volatile List<String> trustedCidrs = Collections.emptyList();
    private static volatile long lastLoadAt = 0L;

    private ServerSelfIpUtil() {
    }

    /**
     * 判断 IP 是否为服务器自身公网 IP（支持精确 IP 与 CIDR 条目）。
     */
    public static boolean isServerSelfIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        ensureLoaded();
        String candidate = normalize(ip);
        if (selfIps.contains(candidate)) {
            return true;
        }
        for (String cidr : selfCidrs) {
            if (IpUtil.isIpInCidr(candidate, cidr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 IP 是否命中可信代理 / CDN 回源网段（TRUSTED_PROXY_CIDRS，与 nginx 侧语义一致）。
     */
    public static boolean isTrustedProxyIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        ensureLoaded();
        List<String> cidrs = trustedCidrs;
        if (cidrs.isEmpty()) {
            return false;
        }
        String candidate = normalize(ip);
        for (String entry : cidrs) {
            if (entry.contains("/")) {
                if (IpUtil.isIpInCidr(candidate, entry)) {
                    return true;
                }
            } else if (normalize(entry).equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统一"永不拉黑 / 永不参与封禁判定"判定：
     * 非公网地址（私有 / 回环 / 保留 / 非法）或 服务器自身 IP 或 可信代理段。
     *
     * <p>反向不成立：公网普通访客 IP 返回 false（正常参与封禁与统计）。</p>
     */
    public static boolean isNeverBanIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return true;
        }
        String candidate = normalize(ip);
        if (!IpUtil.isPublicRoutableIp(candidate)) {
            return true;
        }
        return isServerSelfIp(candidate) || isTrustedProxyIp(candidate);
    }

    /**
     * 当前生效的自身 IP 清单（诊断 / 日志用，返回副本）。
     */
    public static List<String> getSelfIps() {
        ensureLoaded();
        return new ArrayList<>(selfIps);
    }

    /**
     * 当前生效的可信代理网段清单（诊断 / 日志用，返回副本）。
     */
    public static List<String> getTrustedProxyCidrs() {
        ensureLoaded();
        return new ArrayList<>(trustedCidrs);
    }

    // ============================ 加载与解析 ============================

    private static void ensureLoaded() {
        if (selfIps == null) {
            loadOnce();
            return;
        }
        // 清单为空（未配置且探测文件尚未就绪）时允许定期重试，其余情况只加载一次
        if (selfIps.isEmpty() && trustedCidrs.isEmpty()
                && System.currentTimeMillis() - lastLoadAt > EMPTY_RETRY_INTERVAL_MS) {
            loadOnce();
        }
    }

    private static void loadOnce() {
        String filePath = System.getenv(ENV_SELF_IP_FILE);
        if (filePath == null || filePath.trim().isEmpty()) {
            filePath = DEFAULT_SELF_IP_FILE;
        }
        reconfigure(System.getenv(ENV_SELF_IPS), System.getenv(ENV_TRUSTED_PROXY_CIDRS), filePath);
    }

    /**
     * 以给定配置重建清单（包级可见，供单测与 refresh 使用）。
     *
     * @param selfIpsRaw      自身 IP 原始配置（逗号/分号/空白分隔，可含 CIDR，可为 null）
     * @param trustedCidrsRaw 可信代理网段原始配置（可为 null）
     * @param filePath        运行时文件路径（可为 null / 不存在，读取失败静默忽略）
     */
    static synchronized void reconfigure(String selfIpsRaw, String trustedCidrsRaw, String filePath) {
        StringBuilder mergedSelfRaw = new StringBuilder();
        if (selfIpsRaw != null) {
            mergedSelfRaw.append(selfIpsRaw);
        }
        String fileContent = readFileQuietly(filePath);
        if (!fileContent.isEmpty()) {
            mergedSelfRaw.append('\n').append(fileContent);
        }

        Set<String> ips = new LinkedHashSet<>();
        Set<String> cidrs = new LinkedHashSet<>();
        for (String entry : splitEntries(mergedSelfRaw.toString())) {
            if (entry.contains("/")) {
                if (IpUtil.isValidIpWhitelistEntry(entry)) {
                    cidrs.add(normalize(entry));
                } else {
                    log.debug("[ServerSelfIpUtil] 忽略非法 CIDR 条目: {}", entry);
                }
            } else if (IpUtil.isValidIpLiteral(entry)) {
                ips.add(normalize(entry));
            } else {
                log.debug("[ServerSelfIpUtil] 忽略非法 IP 条目: {}", entry);
            }
        }

        List<String> cidrList = new ArrayList<>();
        for (String entry : splitEntries(trustedCidrsRaw)) {
            if (entry.contains("/")) {
                if (IpUtil.isValidIpWhitelistEntry(entry)) {
                    cidrList.add(normalize(entry));
                } else {
                    log.debug("[ServerSelfIpUtil] 忽略非法可信段条目: {}", entry);
                }
            } else if (IpUtil.isValidIpLiteral(entry)) {
                // 裸 IP 视为单机可信代理（等价 /32）
                cidrList.add(normalize(entry));
            } else {
                log.debug("[ServerSelfIpUtil] 忽略非法可信段条目: {}", entry);
            }
        }

        selfIps = Collections.unmodifiableSet(ips);
        selfCidrs = Collections.unmodifiableSet(cidrs);
        trustedCidrs = Collections.unmodifiableList(cidrList);
        lastLoadAt = System.currentTimeMillis();

        if (!ips.isEmpty() || !cidrs.isEmpty() || !cidrList.isEmpty()) {
            log.info("[ServerSelfIpUtil] IP 排除清单已生效: 自身IP={}, 可信段={}", selfIps, trustedCidrs);
        } else {
            log.debug("[ServerSelfIpUtil] IP 排除清单为空（未配置 SELF_PUBLIC_IPS 且探测文件 {} 不存在）", filePath);
        }
    }

    private static String readFileQuietly(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return "";
        }
        try {
            Path path = Path.of(filePath.trim());
            if (!Files.isRegularFile(path)) {
                return "";
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            log.debug("[ServerSelfIpUtil] 读取运行时文件失败: path={}, error={}", filePath, e.getMessage());
            return "";
        }
    }

    /** 按逗号 / 分号 / 空白 / 换行切分，跳过空项与 # 注释行 */
    private static List<String> splitEntries(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> entries = new ArrayList<>();
        for (String piece : raw.split("[,;\\s]+")) {
            String entry = piece.trim();
            if (!entry.isEmpty() && !entry.startsWith("#")) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static String normalize(String ip) {
        String value = ip.trim();
        // IPv6 十六进制不区分大小写，统一小写便于比较
        return value.contains(":") ? value.toLowerCase(Locale.ROOT) : value;
    }
}
