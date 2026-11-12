#!/usr/bin/env python3
"""Report Struts action coverage by recorded requests and setup steps."""

import json
from pathlib import Path
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


def parse_actions(xml, prefix=""):
    root = ET.fromstring(xml)
    return {prefix + action.attrib["path"] + ".do"
            for action in root.findall("./action-mappings/action")}


def action_paths(root=ROOT):
    web = root / "src/main/webapp/WEB-INF"
    paths = set()
    for param in ET.parse(web / "web.xml").findall("./servlet/init-param"):
        name = param.findtext("param-name", "")
        if name == "config" or name.startswith("config/"):
            prefix = name[len("config"):]
            for config in param.findtext("param-value").split(","):
                paths.update(parse_actions(
                    (web.parent / config.strip().lstrip("/")).read_text(), prefix))
    return paths


def request_path(request):
    path = urlsplit(request["path"]).path
    return path.removeprefix("/claims")


def coverage(paths, transcripts):
    hits = {path: {"request": [], "setup": []} for path in sorted(paths)}
    unmapped = {}
    for transcript in transcripts:
        for kind, requests in (("request", [transcript["request"]]),
                               ("setup", transcript.get("setup", []))):
            for request in requests:
                path = request_path(request)
                target = hits if path in hits else unmapped
                target.setdefault(path, {"request": [], "setup": []})
                ids = target[path][kind]
                if transcript["scenario"] not in ids:
                    ids.append(transcript["scenario"])
    return hits, unmapped


def main():
    index = json.loads((ROOT / "transcripts/index.json").read_text())
    transcripts = [json.loads((ROOT / "transcripts" /
                              (entry["scenario"] + ".json")).read_text())
                   for entry in index]
    hits, unmapped = coverage(action_paths(), transcripts)
    lines = ["# Struts action coverage", "",
             "Explicit requests only; redirects, forwards, probes, and implicit login are not counted.", ""]
    for heading, rows in (("Action mappings", hits),
                          ("Requests with no mapping", unmapped)):
        lines.extend(["## " + heading, "", "| Action path | Scenario ids | Setup scenario ids |",
                      "| --- | --- | --- |"])
        for path, kinds in sorted(rows.items()):
            lines.append("| `{}` | {} | {} |".format(
                path, ", ".join(kinds["request"]) or "—",
                ", ".join(kinds["setup"]) or "—"))
        lines.append("")
    output = ROOT / "docs/parity/COVERAGE.md"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines))
    uncovered = [path for path, kinds in hits.items() if not any(kinds.values())]
    print("Covered {}/{} action paths".format(len(hits) - len(uncovered), len(hits)))
    print("Uncovered action paths:")
    for path in uncovered:
        print("  " + path)
    print("Requests with no mapping: " + (", ".join(sorted(unmapped)) or "none"))
    return bool(uncovered)


if __name__ == "__main__":
    raise SystemExit(main())
