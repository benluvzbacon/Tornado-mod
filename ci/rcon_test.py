#!/usr/bin/env python3
"""Tiny Source-RCON client for the CI smoke test.

Usage: python3 ci/rcon_test.py <host> <port> <password> <command>
Prints the server's response (or the error message) and exits 0.
Exits 1 on connection/auth/protocol failure.
"""
import socket
import struct
import sys


def main() -> int:
    if len(sys.argv) != 5:
        print("usage: rcon_test.py host port password command")
        return 1
    host, port, password, command = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]

    try:
        sock = socket.create_connection((host, port), timeout=10)
    except OSError as e:
        print(f"CONNECTION-FAILED: {e}")
        return 1

    def send_packet(req_type: int, payload: bytes) -> None:
        body = struct.pack("<ii", 1, req_type) + payload + b"\x00\x00"
        sock.sendall(struct.pack("<i", len(body)) + body)

    def recv_packet() -> bytes:
        header = b""
        while len(header) < 4:
            chunk = sock.recv(4 - len(header))
            if not chunk:
                raise ConnectionError("closed by server")
            header += chunk
        (length,) = struct.unpack("<i", header)
        body = b""
        while len(body) < length:
            chunk = sock.recv(length - len(body))
            if not chunk:
                raise ConnectionError("closed by server")
            body += chunk
        return body

    try:
        send_packet(2, password.encode())
        resp = recv_packet()
        if len(resp) < 12:
            print(f"AUTH-FAILED: short response (len={len(resp)} hex={resp.hex()})")
            return 1
        resp_id, resp_type, err = struct.unpack("<iii", resp[:12])
        if resp_type != 2:
            print(f"AUTH-FAILED: unexpected type {resp_type}")
            return 1
        if err != 0:
            print(f"AUTH-FAILED: {resp[12:-2].decode(errors='replace')}")
            return 1

        send_packet(3, command.encode())
        resp = recv_packet()
        if len(resp) < 8:
            print(f"COMMAND-FAILED: short response (len={len(resp)} hex={resp.hex()})")
            return 1
        resp_id, resp_type = struct.unpack("<ii", resp[:8])
        err = struct.unpack("<i", resp[8:12])[0] if len(resp) >= 12 else 0
        if resp_type != 3:
            print(f"COMMAND-FAILED: unexpected type {resp_type}")
            return 1
        out = resp[12:-2].decode(errors="replace")
        print(out.strip() if out.strip() else "(no output)")
        if err != 0 and "unknown command" in out.lower():
            print(f"COMMAND-UNKNOWN: {out}")
            return 2
        return 0
    except Exception as e:  # noqa: BLE001
        print(f"RCON-ERROR: {e}")
        return 1
    finally:
        sock.close()


if __name__ == "__main__":
    sys.exit(main())
