#!/usr/bin/env python3
"""真实 OpenResty/Redis 集成验收。依赖本机 Docker、Python h2；所有流量只发往本地测试容器。

python scripts/test_nginx_attack_events.py --probe
python scripts/test_nginx_attack_events.py
"""
import argparse
import json
import os
from pathlib import Path
import re
import socket
import ssl
import subprocess
import time

from h2.config import H2Configuration
from h2.connection import H2Connection
from h2.events import ConnectionTerminated, ResponseReceived, StreamEnded, StreamReset

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / "temp" / "nginx-events-test"
IMAGE = os.environ.get("NGINX_EVENTS_TEST_IMAGE", "poetize-nginx-h2test:latest")
PREFIX = "poetize-events-test"
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def docker(*args, check=True):
    result = subprocess.run(["docker", *map(str, args)], env=ENV, text=True,
                            encoding="utf-8", errors="replace", capture_output=True)
    if check and result.returncode:
        raise RuntimeError(f"docker {' '.join(map(str, args))}\n{result.stdout}\n{result.stderr}")
    return result.stdout.strip()


def h2_request(port, ip="8.8.8.8", headers=1, path="/", bomb=False):
    context = ssl._create_unverified_context()
    context.set_alpn_protocols(["h2"])
    with socket.create_connection(("127.0.0.1", port), timeout=5) as raw:
        with context.wrap_socket(raw, server_hostname="events.test") as sock:
            assert sock.selected_alpn_protocol() == "h2", "ALPN 必须为 h2"
            conn = H2Connection(config=H2Configuration(client_side=True, header_encoding="utf-8"))
            conn.initiate_connection()
            pairs = [(":method", "GET"), (":scheme", "https"), (":authority", "events.test"),
                     (":path", path), ("eo-connecting-ip", ip)]
            pairs.extend((f"x-test-{i}", "v") for i in range(headers))
            if bomb:
                pairs.extend(("cookie", "x" * 4096) for _ in range(5000))
            conn.send_headers(1, pairs, end_stream=True)
            sock.sendall(conn.data_to_send())
            status = None
            while True:
                data = sock.recv(65536)
                if not data:
                    return status or "closed"
                for event in conn.receive_data(data):
                    if isinstance(event, ResponseReceived):
                        status = int(dict(event.headers)[":status"])
                    if isinstance(event, (StreamEnded, StreamReset)):
                        return status or f"reset:{event.error_code}"
                    if isinstance(event, ConnectionTerminated):
                        return f"goaway:{int(event.error_code)}"
                pending = conn.data_to_send()
                if pending:
                    sock.sendall(pending)


def raw_request(port, payload):
    context = ssl._create_unverified_context()
    context.set_alpn_protocols(["http/1.1"])
    with socket.create_connection(("127.0.0.1", port), timeout=5) as raw:
        with context.wrap_socket(raw, server_hostname="events.test") as sock:
            sock.sendall(payload)
            data = sock.recv(65536)
            return int(data.split(b" ", 2)[1]) if data.startswith(b"HTTP/") else "closed"


def wait_ready(port):
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        try:
            if h2_request(port, ip="192.0.2.1") == 200:
                return
        except (OSError, TimeoutError):
            continue
    raise RuntimeError("nginx 未就绪")


def config(probe):
    production = (ROOT / "docker/nginx/nginx.conf").read_text(encoding="utf-8")
    env_lines = "\n".join(line for line in production.splitlines() if line.startswith("env "))
    hooks = '''lua_capture_error_log 1m;
    log_by_lua_block {
        local logs = require("ngx.errlog").get_logs(1000)
        for i=1,#logs,3 do
            if not logs[i+2]:find("PROBE",1,true) then
                ngx.log(ngx.ERR, "PROBE_NATIVE ", logs[i+2])
            end
        end
        ngx.log(ngx.ERR, "PROBE_REQUEST ", require("cjson").encode({
            status=ngx.status, connection=ngx.var.connection,
            request=ngx.var.request, ip=ngx.var.real_client_ip,
            vendor=ngx.var.http_eo_connecting_ip, h2=ngx.var.http2,
            upstream=ngx.var.upstream_status}))
    }''' if probe else '''lua_capture_error_log 1m;
    lua_shared_dict attack_events 2m;
    init_worker_by_lua_block { require("attack_events").init_worker() }
    log_by_lua_block { require("attack_events").log_request() }'''
    return f'''worker_processes 2;
pid /tmp/events-nginx.pid;
error_log stderr info;
{env_lines}
events {{ worker_connections 1024; }}
http {{
    resolver 127.0.0.11;
    lua_package_path "/usr/local/openresty/nginx/lua/?.lua;;";
    lua_shared_dict ban_rules_cache 1m;
    {hooks}
    map $real_client_ip_resolved $real_client_ip {{ "~." $real_client_ip_resolved; default $remote_addr; }}
    log_format probe escape=json '{{"status":$status,"h2":"$http2","peer":"$remote_addr","ip":"$real_client_ip"}}';
    access_log /tmp/events-access.log probe;
    server {{
        listen 443 ssl;
        listen 443 quic reuseport;
        server_name events.test;
        http2 on;
        http3 on;
        max_headers 1000;
        ssl_certificate /etc/nginx/ssl/default.crt;
        ssl_certificate_key /etc/nginx/ssl/default.key;
        set $real_client_ip_resolved '';
        rewrite_by_lua_file /usr/local/openresty/nginx/lua/real_ip_resolve.lua;
        access_by_lua_file /usr/local/openresty/nginx/lua/ban_check.lua;
        location / {{ content_by_lua_block {{ ngx.say("ok") }} }}
        location /missing/ {{ content_by_lua_block {{ ngx.exit(404) }} }}
        location /app400 {{ content_by_lua_block {{ ngx.exit(400) }} }}
        location /upstream400 {{ proxy_pass http://127.0.0.1:8080; }}
    }}
    server {{
        listen 8080;
        location / {{ return 400; }}
    }}
}}
'''


def start_nginx(name, port, probe, extra_env=None):
    conf_path = WORK / "nginx.conf"
    args = ["run", "-d", "--name", name, "--network", PREFIX,
            "--entrypoint", "sh", "-p", f"127.0.0.1:{port}:443/tcp",
            "-p", f"127.0.0.1:{port}:443/udp",
            "-v", f"{conf_path.as_posix()}:/test/nginx.conf:ro",
            "-v", f"{(ROOT / 'docker/nginx/lua').as_posix()}:/usr/local/openresty/nginx/lua:ro",
            "-v", f"{PREFIX}:/app/runtime",
            "-e", f"REDIS_HOST={PREFIX}-redis", "-e", "REDIS_PASSWORD=", "-e", "REDIS_DB=0",
            "-e", "REAL_IP_HEADER=EO-Connecting-IP"]
    for key, value in (extra_env or {}).items():
        args.extend(["-e", f"{key}={value}"])
    args.extend([IMAGE, "-c", "exec openresty -c /test/nginx.conf -g 'daemon off;'"])
    docker(*args)
    wait_ready(port)


def redis_command(*args):
    return docker("exec", f"{PREFIX}-redis", "redis-cli", "--raw", *args)


def wait_for(predicate, message, timeout=6):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if predicate():
            return
        # 每轮执行真实状态检查，不用固定 sleep 猜测异步完成。
    raise AssertionError(message)


def all_logs(name):
    result = subprocess.run(["docker", "logs", name], capture_output=True, env=ENV)
    return (result.stdout + result.stderr).decode("utf-8", errors="replace")


def invalid_header(ip):
    return (f"GET / HTTP/1.1\r\nHost: events.test\r\nEO-Connecting-IP: {ip}\r\n".encode()
            + b"Bad\x01Header: x\r\n\r\n")


def large_header(ip):
    return (f"GET / HTTP/1.1\r\nHost: events.test\r\nEO-Connecting-IP: {ip}\r\nX-Large: ".encode()
            + b"a" * 18000 + b"\r\n\r\n")


def integration(names, results=None):
    results = [] if results is None else results

    def passed(name, detail):
        results.append({"name": name, "detail": detail, "status": "PASS"})
        print(f"PASS {name}: {detail}", flush=True)

    settings = {"NGINX_ATTACK_WINDOW_SECONDS": "3", "NGINX_ATTACK_BAN_SECONDS": "6",
                "NGINX_ATTACK_E1_THRESHOLD": "2", "NGINX_ATTACK_E4_THRESHOLD": "2",
                "NGINX_ATTACK_E5_THRESHOLD": "2", "NGINX_ATTACK_E6_THRESHOLD": "2"}
    start_nginx(names[0], 18443, False, settings)
    start_nginx(names[1], 18444, False, settings)
    trusted = dict(settings, TRUSTED_PROXY_CIDRS="8.8.8.0/24,9.9.9.9",
                   SELF_PUBLIC_IPS="8.8.4.0/24", NGINX_ATTACK_E6_ENABLED="true")
    start_nginx(names[2], 18445, False, trusted)

    for count in (1, 100):
        assert h2_request(18443, headers=count) == 200
    passed("1. 正常 ALPN h2", "1头=200，100头=200，未产生攻击计数")

    # 两个实例合计超过阈值；单实例各自均不超过阈值。
    statuses = [h2_request(port, headers=1100) for port in (18443, 18444, 18443)]
    assert statuses == [400, 400, 400], statuses
    key = "poetize:security:blacklist:8.8.8.8"
    wait_for(lambda: redis_command("EXISTS", key) == "1", "E1 未封禁")
    reason = redis_command("GET", key)
    assert json.loads(reason).startswith("auto:nginx:E1:"), reason
    assert h2_request(18443) == 403
    assert h2_request(18444) == 403
    passed("2/3/5/7. E1封禁与多实例真实IP", f"1100头={statuses}，A/B正常请求=403；{key}={reason}")
    peer = docker("inspect", names[0], "--format", "{{range .NetworkSettings.Networks}}{{.Gateway}}{{end}}")
    assert redis_command("EXISTS", f"poetize:security:blacklist:{peer}") == "0"
    assert h2_request(18443, ip="1.1.1.1") == 200
    passed("5. 代理地址未封", f"peer={peer}无封禁键；另一访客=200")
    wait_for(lambda: redis_command("EXISTS", key) == "0", "TTL 未过期", timeout=10)
    assert h2_request(18443) == 200 and h2_request(18444) == 200
    passed("6. 自动过期", "6秒TTL后，A/B均恢复200")

    for event, ip, payload in [("E4", "8.8.4.4", invalid_header("8.8.4.4")),
                               ("E5", "9.9.9.9", large_header("9.9.9.9"))]:
        statuses = [raw_request(18443, payload) for _ in range(3)]
        assert statuses == [400] * 3, statuses
        event_key = f"poetize:security:blacklist:{ip}"
        wait_for(lambda: redis_command("EXISTS", event_key) == "1", f"{event} 未封禁")
        assert event in redis_command("GET", event_key)
        assert h2_request(18444, ip=ip) == 403
        passed(f"2/3. {event}真实触发闭环", f"{statuses} -> B=403")

    uri_status = raw_request(18443, b"GET /" + b"a" * 18000 + b" HTTP/1.1\r\nHost: events.test\r\n\r\n")
    assert uri_status == 414
    wait_for(lambda: "identity_unavailable" in all_logs(names[0]), "URI 未记录安全降级")
    passed("2. 超长URI", "414；身份头未读取，仅观测identity_unavailable")

    for ip in ("8.8.8.8", "8.8.4.4", "9.9.9.9", "192.0.2.1"):
        for _ in range(3):
            assert h2_request(18445, ip=ip, headers=1100) == 400
        assert h2_request(18445, ip=ip) == 200
    wait_for(lambda: '"reason":"never_ban"' in all_logs(names[2]), "缺少豁免日志")
    passed("4. never-ban", "可信/自身CIDR、可信裸IP、非公网均不封，正常请求200，日志含never_ban")

    # E6 默认关闭；开启时只统计本地失败，排除真正 upstream 400。
    for _ in range(3):
        assert h2_request(18443, ip="1.0.0.1", path="/missing/random") == 404
        assert h2_request(18443, ip="1.0.0.1", path="/app400") == 400
        assert h2_request(18445, ip="1.1.1.1", path="/upstream400") == 400
    assert redis_command("EXISTS", "poetize:security:events:E6:1.0.0.1") == "0"
    assert redis_command("EXISTS", "poetize:security:events:E6:1.1.1.1") == "0"
    assert h2_request(18443, ip="1.0.0.1") == 200
    passed("默认关闭与去重", "E6关闭无计数；开启后upstream400也无计数；普通应用400不计E1/E4")
    for _ in range(3):
        assert h2_request(18445, ip="1.0.0.1", path="/missing/random") == 404
    wait_for(lambda: redis_command("EXISTS", "poetize:security:blacklist:1.0.0.1") == "1", "E6 未封禁")
    assert h2_request(18444, ip="1.0.0.1") == 403
    passed("2/3. E6可选启用", "本地404三次触发，另一实例403")

    # 滑动窗口到期后旧事件不参与判定。
    redis_command("DEL", key)
    assert h2_request(18443, headers=1100) == 400
    counter = "poetize:security:events:E1:8.8.8.8"
    wait_for(lambda: redis_command("EXISTS", counter) == "1", "首事件未计数")
    wait_for(lambda: redis_command("EXISTS", counter) == "0", "窗口未过期", timeout=6)
    assert h2_request(18444, headers=1100) == 400
    assert h2_request(18443, headers=1100) == 400
    wait_for(lambda: redis_command("LLEN", counter) == "2", "跨实例窗口未计数")
    assert redis_command("EXISTS", key) == "0"
    passed("精确滑动窗口", "旧事件过期；新窗口2次未超过阈值，不封禁")

    # 已有永久封禁不能被临时封禁覆盖，也不能改成无效 JSON 字符串。
    redis_command("SET", key, json.dumps("manual:permanent"))
    for _ in range(3):
        assert h2_request(18443, headers=1100) == 400
    assert redis_command("TTL", key) == "-1"
    assert json.loads(redis_command("GET", key)) == "manual:permanent"
    passed("已有封禁保护", "永久封禁TTL=-1，原因保持不变")
    redis_command("DEL", key)

    # 两个连接级特征都由真实请求/数据报产生；不以对端 IP 自动封禁。
    assert h2_request(18443, bomb=True) == "goaway:11"
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp:
        packet = b"\xc0\x00\x00\x00\x01\x01a\x08abcdefgh\x00\x44\x9c"
        udp.sendto(packet.ljust(1200, b"\x00"), ("127.0.0.1", 18443))
    wait_for(lambda: '"event":"E2"' in all_logs(names[0]) and '"event":"E3"' in all_logs(names[0]),
             "协议观测日志缺失")
    passed("2. E2/E3真实观测", "QUIC短DCID与h2 GOAWAY=11；均observe，无封禁")

    # 运行镜像自带 LuaJIT 编译全部生产 Lua，真实词法检查。
    bytecode = docker("exec", names[0], "sh", "-c",
        "for f in /usr/local/openresty/nginx/lua/*.lua; do "
        "/usr/local/openresty/luajit/bin/luajit -bl \"$f\" >/dev/null || exit 1; done; "
        "echo 'all Lua files bytecode OK'")
    passed("8. LuaJIT语法", bytecode)

    # 单类开关须彻底关闭该类埋点，且不能被已开启的 E6 接手。
    docker("rm", "-f", names[2])
    disabled = dict(settings, NGINX_ATTACK_E6_ENABLED="true", **{
        f"NGINX_ATTACK_E{i}_ENABLED": "false" for i in range(1, 6)})
    start_nginx(names[2], 18445, False, disabled)
    redis_command("DEL", key, counter)
    for _ in range(3):
        assert h2_request(18445, headers=1100) == 400
        assert raw_request(18445, invalid_header("8.8.8.8")) == 400
        assert raw_request(18445, large_header("8.8.8.8")) == 400
    assert h2_request(18445) == 200
    assert redis_command("KEYS", "poetize:security:events:*:8.8.8.8") == ""
    assert "attack_event {" not in all_logs(names[2])
    passed("单类开关", "E1–E5关闭后不计数、不封禁、不被E6接手")

    docker("rm", "-f", names[2])
    start_nginx(names[2], 18445, False, dict(settings, NGINX_ATTACK_ENABLED="false"))
    for _ in range(3):
        assert h2_request(18445, headers=1100) == 400
    assert h2_request(18445) == 200
    assert "attack_event {" not in all_logs(names[2])
    passed("总开关", "关闭采集后正常请求200，无新计数与结构化事件")

    # Redis 不可达时请求可用，队列丢弃并退避，日志交代漏计原因。
    docker("stop", names[-1])
    assert h2_request(18443, headers=1100) == 400
    assert h2_request(18443, ip="1.1.1.1") == 200
    wait_for(lambda: "redis_unavailable" in all_logs(names[0]), "Redis故障未记录")
    passed("Redis故障降级", "Redis停止后正常请求200，日志含redis_unavailable")
    (WORK / "results.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")


def validate_production_config():
    results = []
    for mode in ("http", "https"):
        site = (ROOT / f"docker/nginx/default.{mode}.conf.template").read_text(encoding="utf-8")
        # 与 poetize:apply_domain_to_nginx_conf 相同的必要渲染，未经渲染的模板有重复 referer。
        site = re.sub(r"(valid_referers\s+[^;]*server_names)\s+[^;]*;", r"\1;", site)
        site = site.replace("/etc/letsencrypt/live/example.com/fullchain.pem", "/etc/nginx/ssl/default.crt")
        site = site.replace("/etc/letsencrypt/live/example.com/privkey.pem", "/etc/nginx/ssl/default.key")
        site = site.replace("/etc/letsencrypt/live/example.com/chain.pem", "/etc/nginx/ssl/default.crt")
        path = WORK / f"rendered-{mode}.conf"
        path.write_text(site, encoding="utf-8")
        output = docker("run", "--rm", "--entrypoint", "sh", "--add-host", "poetize-java:127.0.0.1",
            "-v", f"{(ROOT / 'docker/nginx/nginx.conf').as_posix()}:/usr/local/openresty/nginx/conf/nginx.conf:ro",
            "-v", f"{path.as_posix()}:/usr/local/openresty/nginx/conf/conf.d/default.conf:ro",
            "-v", f"{(ROOT / 'docker/nginx/lua').as_posix()}:/usr/local/openresty/nginx/lua:ro",
            IMAGE, "-c", "openresty -t 2>&1")
        results.append({"name": f"生产配置 {mode}", "detail": output, "status": "PASS"})
        print(f"PASS 生产配置 {mode}: {output}", flush=True)
    return results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--probe", action="store_true")
    args = parser.parse_args()
    WORK.mkdir(parents=True, exist_ok=True)
    (WORK / "nginx.conf").write_text(config(args.probe), encoding="utf-8")
    names = [f"{PREFIX}-a", f"{PREFIX}-b", f"{PREFIX}-trusted", f"{PREFIX}-redis"]
    for name in names:
        docker("rm", "-f", name, check=False)
    docker("network", "create", PREFIX, check=False)
    docker("volume", "create", PREFIX)
    docker("run", "-d", "--name", names[-1], "--network", PREFIX,
           "redis:7-alpine", "redis-server", "--save", "", "--appendonly", "no")
    try:
        if args.probe:
            start_nginx(names[0], 18443, True)
            for count in [1, 100, 1100]:
                print(f"h2 headers={count}: {h2_request(18443, headers=count)}", flush=True)
            print("application400:", h2_request(18443, path="/app400"), flush=True)
            print("invalid-header:", raw_request(18443, b"GET / HTTP/1.1\r\nHost: events.test\r\nEO-Connecting-IP: 8.8.4.4\r\nBad\x01Header: x\r\n\r\n"), flush=True)
            print("long-uri:", raw_request(18443, b"GET /" + b"a" * 18000 + b" HTTP/1.1\r\nHost: events.test\r\nEO-Connecting-IP: 8.8.4.4\r\n\r\n"), flush=True)
            print("long-header:", raw_request(18443, b"GET / HTTP/1.1\r\nHost: events.test\r\nEO-Connecting-IP: 8.8.4.4\r\nX-Large: " + b"a" * 18000 + b"\r\n\r\n"), flush=True)
            print("h2-large-field:", h2_request(18443, bomb=True), flush=True)
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp:
                # QUIC v1 Initial，DCID=1 字节（RFC 要求至少 8），单个 1200 字节本地数据报。
                packet = b"\xc0\x00\x00\x00\x01\x01a\x08abcdefgh\x00\x44\x9c"
                udp.sendto(packet.ljust(1200, b"\x00"), ("127.0.0.1", 18443))
            h2_request(18443)
            logs = docker("logs", names[0])
            # docker 的 stderr 包含 nginx error_log。
            result = subprocess.run(["docker", "logs", names[0]], capture_output=True, env=ENV)
            logs = (result.stdout + result.stderr).decode("utf-8", errors="replace")
            (WORK / "probe.log").write_text(logs, encoding="utf-8")
            for line in logs.splitlines():
                if "PROBE_" in line:
                    print(line[:750])
        else:
            production_results = validate_production_config()
            integration(names, production_results)
    finally:
        for name in names[:-1]:
            result = subprocess.run(["docker", "logs", name], capture_output=True, env=ENV)
            (WORK / f"{name}.log").write_bytes(result.stdout + result.stderr)
        for name in names:
            docker("rm", "-f", name, check=False)
        docker("network", "rm", PREFIX, check=False)
        docker("volume", "rm", PREFIX, check=False)


if __name__ == "__main__":
    main()
