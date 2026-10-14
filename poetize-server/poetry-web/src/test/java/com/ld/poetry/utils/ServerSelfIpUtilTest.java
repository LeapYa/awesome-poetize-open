package com.ld.poetry.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ServerSelfIpUtil 单元测试：自身 IP / 可信代理清单的解析、合并与"永不拉黑"判定。
 */
class ServerSelfIpUtilTest {

    @AfterEach
    void resetState() {
        // 复位静态清单，避免测试间相互影响（空清单在 60 秒内不会触发从环境变量重载）
        ServerSelfIpUtil.reconfigure(null, null, null);
    }

    @Test
    void parsesSelfIpsFromMultipleSeparatorsAndNormalizesIpv6() {
        ServerSelfIpUtil.reconfigure("8.8.4.4, 8.8.8.8;  2001:DB8::1\nbogus-entry", null, null);

        assertTrue(ServerSelfIpUtil.isServerSelfIp("8.8.4.4"));
        assertTrue(ServerSelfIpUtil.isServerSelfIp(" 8.8.8.8 "));
        assertTrue(ServerSelfIpUtil.isServerSelfIp("2001:db8::1"));
        assertTrue(ServerSelfIpUtil.isServerSelfIp("2001:DB8::1"), "IPv6 十六进制不区分大小写");
        assertFalse(ServerSelfIpUtil.isServerSelfIp("1.2.3.4"), "未配置的 IP 不应命中");
        assertFalse(ServerSelfIpUtil.isServerSelfIp("bogus-entry"), "非法条目应被忽略");
    }

    @Test
    void mergesRuntimeFileWithEnvironmentValue(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("self_ips");
        Files.writeString(file, "8.8.4.4\r\n# 注释行\n9.9.9.9\n");

        ServerSelfIpUtil.reconfigure("8.8.8.8", null, file.toString());

        assertTrue(ServerSelfIpUtil.isServerSelfIp("8.8.8.8"), "环境变量条目");
        assertTrue(ServerSelfIpUtil.isServerSelfIp("8.8.4.4"), "文件条目（含 CRLF）");
        assertTrue(ServerSelfIpUtil.isServerSelfIp("9.9.9.9"), "文件条目");
        assertFalse(ServerSelfIpUtil.isServerSelfIp("#"), "注释行不应成为条目");
    }

    @Test
    void missingRuntimeFileIsTolerated() {
        ServerSelfIpUtil.reconfigure("8.8.8.8", null, "/nonexistent/path/self_ips");
        assertTrue(ServerSelfIpUtil.isServerSelfIp("8.8.8.8"));
    }

    @Test
    void supportsCidrEntriesInSelfList() {
        ServerSelfIpUtil.reconfigure("203.0.114.0/24", null, null);
        assertTrue(ServerSelfIpUtil.isServerSelfIp("203.0.114.5"));
        assertFalse(ServerSelfIpUtil.isServerSelfIp("203.0.115.5"));
    }

    @Test
    void neverBanCoversSelfPrivateReservedAndInvalidAddresses() {
        ServerSelfIpUtil.reconfigure("8.8.4.4", "203.0.113.0/24, 8.8.8.8", null);

        // 自身 IP
        assertTrue(ServerSelfIpUtil.isNeverBanIp("8.8.4.4"));
        // 私有 / 回环 / 保留
        assertTrue(ServerSelfIpUtil.isNeverBanIp("10.0.0.1"));
        assertTrue(ServerSelfIpUtil.isNeverBanIp("172.28.147.1"));
        assertTrue(ServerSelfIpUtil.isNeverBanIp("127.0.0.1"));
        assertTrue(ServerSelfIpUtil.isNeverBanIp("100.64.0.1"));
        // 可信代理 / CDN 回源段
        assertTrue(ServerSelfIpUtil.isNeverBanIp("203.0.113.9"));
        assertTrue(ServerSelfIpUtil.isNeverBanIp("8.8.8.8"));
        // 非法 / 空值
        assertTrue(ServerSelfIpUtil.isNeverBanIp("abc"));
        assertTrue(ServerSelfIpUtil.isNeverBanIp("unknown_ip"));
        assertTrue(ServerSelfIpUtil.isNeverBanIp(""));
        assertTrue(ServerSelfIpUtil.isNeverBanIp(null));

        // 公网普通访客：正常参与封禁与统计
        assertFalse(ServerSelfIpUtil.isNeverBanIp("120.239.141.213"));
        assertFalse(ServerSelfIpUtil.isNeverBanIp("154.37.208.64"));
    }

    @Test
    void trustedProxyPlainIpEntryMatchesExactly() {
        ServerSelfIpUtil.reconfigure(null, "198.51.100.7", null);
        assertTrue(ServerSelfIpUtil.isTrustedProxyIp("198.51.100.7"));
        assertFalse(ServerSelfIpUtil.isTrustedProxyIp("198.51.100.8"));
    }

    @Test
    void emptyConfigurationKeepsEverythingBanEligible() {
        ServerSelfIpUtil.reconfigure(null, null, null);
        assertTrue(ServerSelfIpUtil.getSelfIps().isEmpty());
        assertTrue(ServerSelfIpUtil.getTrustedProxyCidrs().isEmpty());
        // 没了排除清单之后，公网 IP 照常可封
        assertFalse(ServerSelfIpUtil.isNeverBanIp("120.239.141.213"));
    }
}
