#!/usr/bin/env python3
"""Compare this slice against running, freshly seeded legacy and Boot servers."""
import http.cookiejar
import importlib.util
import json
from pathlib import Path
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("capture", ROOT / "tools/capture/capture.py")
capture = importlib.util.module_from_spec(spec)
spec.loader.exec_module(capture)


def client():
    return urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),
        capture.NoRedirect())


def request(opener, port, method, path, form=None):
    data = None if form is None else urllib.parse.urlencode(form).encode()
    req = urllib.request.Request(f"http://localhost:{port}/claims{path}", data=data, method=method)
    try:
        response = opener.open(req, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    body = response.read().decode()
    location = response.headers.get("Location")
    if location:
        location = urllib.parse.urlsplit(location).path
        location = re.sub(r";jsessionid=[^?]+", "", location)
    return {
        "status": response.code,
        "location": location,
        "fields": dict(sorted((key, re.sub(r"\s+", " ", value).strip())
                              for key, value in capture.FIELD_RE.findall(body))),
        "views": capture.VIEW_RE.findall(body),
        "errors": capture.ERROR_RE.findall(body),
    }


def main():
    failures = 0
    checks = 0
    good = {"username": "supervisor", "password": "supervisor"}
    for authenticated in (False, True):
        clients = {port: client() for port in (8080, 8081)}
        if authenticated:
            for port, opener in clients.items():
                request(opener, port, "POST", "/login.do", good)
        for path in ("/login.do", "/home.do", "/admin/adjusters.do",
                     "/admin/editAdjuster.do", "/admin/reference.do", "/logout.do"):
            rows = [request(clients[port], port, "GET", path) for port in clients]
            checks += 1
            same = rows[0] == rows[1]
            failures += not same
            print(f"{'PASS' if same else 'DIFF'} {'auth' if authenticated else 'anon'} GET {path}")
            if not same:
                print("  legacy:", json.dumps(rows[0], sort_keys=True))
                print("  boot:  ", json.dumps(rows[1], sort_keys=True))
    for label, form in (("good", good), ("bad", {"username": "supervisor", "password": "bad"}),
                        ("empty", {})):
        rows = [request(client(), port, "POST", "/login.do", form) for port in (8080, 8081)]
        checks += 1
        same = rows[0] == rows[1]
        failures += not same
        print(f"{'PASS' if same else 'DIFF'} POST /login.do {label}")
        if not same:
            print("  legacy:", json.dumps(rows[0], sort_keys=True))
            print("  boot:  ", json.dumps(rows[1], sort_keys=True))
        if label == "good":
            expected = json.loads((ROOT / "transcripts/login.json").read_text())["expected"]
            transcript_ok = (rows[1]["status"] == expected["status"]
                             and rows[1]["fields"] == expected["business_fields"]
                             and ["forward:" + path for path in rows[1]["views"]] == [expected["result"]]
                             and rows[1]["errors"] == expected["validation_errors"])
            checks += 1
            failures += not transcript_ok
            print(f"{'PASS' if transcript_ok else 'DIFF'} transcripts/login.json")
    print(f"{checks - failures}/{checks} comparisons matched; {failures} differences")
    return bool(failures)


if __name__ == "__main__":
    sys.exit(main())
