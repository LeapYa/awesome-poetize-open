-- LuaJIT 离线回归：与 Java ServerSelfIpUtilTest 的范围/配置语义对拍。
local ip = require "ip_cidr"
local core = require "real_ip_core"
local conf = core.build_conf("8.8.4.0/24", "9.9.9.9", "EO-Connecting-IP")
local count = 0
local function equal(actual, expected, name)
    assert(actual == expected, name .. ": actual=" .. tostring(actual))
    count = count + 1
end
for _, value in ipairs({"192.88.99.1", "2001::1", "2002::1", "fec0::1",
    "::ffff:192.0.2.1", "::ffff:c000:201", "8.8.4.4", "9.9.9.9",
    "2001:db8::1", "bad-address", "2001:::1", "2001:db8:+1::1"}) do
    equal(core.is_never_ban(value, conf), true, value)
end
equal(core.is_never_ban("1.1.1.1", conf), false, "public visitor")
equal(ip.normalize_ip("::FFFF:8.8.8.8"), "8.8.8.8", "mapped dotted")
equal(ip.normalize_ip("::ffff:808:808"), "8.8.8.8", "mapped hex")
equal(ip.normalize_ip("1:2:3:4:5:6:192.0.2.1"), "1:2:3:4:5:6:192.0.2.1", "embedded IPv4")
equal(ip.cidr_match("192.0.2.0/24.5", "192.0.2.1"), false, "invalid prefix")
equal(core.resolve(nil, "8.8.8.8", "192.0.2.1", conf), "8.8.8.8", "vendor priority")
equal(core.resolve("1.1.1.1,9.9.9.9", "8.8.4.4", "192.0.2.1", conf),
    "1.1.1.1", "skip self and trusted proxy")
local direct = core.build_conf(nil, nil, "off")
equal(core.resolve("8.8.8.8", "9.9.9.9", "1.1.1.1", direct), "1.1.1.1", "direct ignores headers")
print("IP guard assertions passed: " .. count)
