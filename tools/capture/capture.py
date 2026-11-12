#!/usr/bin/env python3
"""Captures deterministic NorthStar claims workflow transcripts."""

import argparse
import datetime
import difflib
import glob
import hashlib
import http.cookiejar
import json
import os
import re
import socket
import subprocess
import sys
import time
import urllib.parse
import urllib.request
import urllib.error


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BASE = "http://localhost:8080/claims"
CONTEXT_PATH = "/claims"
FIELD_RE = re.compile(r'<span id="f_([^"]+)">(.*?)</span>', re.S)
VIEW_RE = re.compile(r"<!--\s*ns:view\s+([^ ]+)\s*-->")
ERROR_RE = re.compile(r"<!--\s*ns:error\s+([^ ]+)\s*-->")
SESSION_RE = re.compile(r";jsessionid=[^?]+")
HTTP_PORT = 8080
DB_PORT = int(os.environ.get("CAPTURE_DB_PORT", "9001"))
DB_URL = "jdbc:hsqldb:hsql://127.0.0.1:%d/northstar" % DB_PORT
SNAPSHOT_DIR = os.path.join(ROOT, "transcripts", "db")
SEED_SNAPSHOT = "_seed"
CAPTURE_CONTEXT_XML = os.path.join(ROOT, "tools", "capture", "jetty-capture.xml")
DEPENDENCY_PLUGIN = "org.apache.maven.plugins:maven-dependency-plugin:3.6.1"
VOLATILE_TYPES = {
    "TIME": "<timestamp>",
    "TIME WITH TIME ZONE": "<timestamp>",
    "TIMESTAMP": "<timestamp>",
    "TIMESTAMP WITH TIME ZONE": "<timestamp>",
}
# (TABLE, COLUMN) -> placeholder for generated tokens that differ per run.
VOLATILE_COLUMNS = {}


SCENARIOS = [
    ("login", "policy", "Valid operator login", "POST", "/login.do",
     {"username": "supervisor", "password": "supervisor"}, {}),
    ("policy_search", "policy", "Search policies by line of business", "GET",
     "/policy/search.do?lineOfBusiness=AUTO", {}, {}),
    ("policy_view", "policy", "View the reserved low-limit policy", "GET",
     "/policy/view.do?policyId=9001", {}, {"policy.9001.limit": "/policy/view.do?policyId=9001"}),
    ("fnol_submit", "intake", "Submit a first notice of loss", "POST",
     "/intake/submit.do",
     {"claimantName": "Capture Claimant", "lossDate": "04/01/2019",
     "description": "Water loss", "lossType": "WATER"},
     {"claim.121.status": "/workbench/view.do?claimId=121",
      "claim.121.loss_date": "/workbench/view.do?claimId=121"}),
    ("workbench_assign", "workbench", "Assign the current claim", "POST",
     "/workbench/assign.do", {"claimId": "121", "assignedAdjuster": "adjuster2"},
     {"claim.121.assigned_adjuster": "/workbench/view.do?claimId=121"}),
    ("workbench_status", "workbench", "Change the claim status", "POST",
     "/workbench/status.do?status=INVESTIGATING", {"claimId": "121"},
     {"claim.121.status": "/workbench/view.do?claimId=121"}),
    ("workbench_reserve", "workbench", "Change the claim reserve", "POST",
     "/workbench/reserve.do", {"claimId": "121", "reserveAmount": "4500.00"},
     {"claim.121.reserve_amount": "/workbench/view.do?claimId=121"}),
    ("settlement_calculate", "settlement", "Calculate a standard settlement",
     "POST", "/settlement/calculate.do",
     {"claimId": "119", "coveredAmount": "5000.00", "deductible": "500.00",
      "depreciation": "0.00"}, {}),
    ("settlement_save", "settlement", "Save the calculated settlement", "POST",
     "/settlement/save.do",
     {"claimId": "119", "coveredAmount": "5000.00", "deductible": "500.00",
      "depreciation": "0.00"},
     {"settlement.claim.119.amount": "/settlement/calculate.do?claimId=119"}),
    ("payment_issue", "settlement", "Issue a settlement payment", "POST",
     "/payment/issue.do",
     {"claimId": "119", "payeeName": "Reserved Claimant",
      "amount": "1000.00", "paymentMethod": "CHECK"},
     {"payment.count.claim.119": "/payment/history.do",
      "payment.61.amount": "/payment/history.do",
      "payment.61.status": "/payment/history.do"}),
    ("payment_history", "settlement", "Review payment history", "GET",
     "/payment/history.do", {}, {}),
    ("report_open_by_adjuster", "reporting", "Open claims by adjuster", "GET",
     "/report/openByAdjuster.do", {}, {}),
    ("report_loss_ratio", "reporting", "Loss ratio by business line", "GET",
     "/report/lossRatio.do", {}, {}),
    ("report_aged_claims", "reporting", "Aged claims report", "GET",
     "/report/agedClaims.do", {}, {}),
    ("settlement_blank_deductible", "settlement",
     "FNOL settlement with a blank deductible", "POST",
     "/settlement/calculate.do",
     {"claimId": "120", "coveredAmount": "5000.00", "deductible": "",
      "depreciation": "500.00"},
     {"claim.120.status": "/workbench/view.do?claimId=120"}),
    ("intake_lenient_date", "intake",
     "FNOL with a lenient February date", "POST", "/intake/submit.do",
     {"claimantName": "Lenient Claimant", "lossDate": "02/30/2019",
      "description": "Date parsing test", "lossType": "WATER"},
     {"claim.122.loss_date": "/workbench/view.do?claimId=122"}),
    ("settlement_half_cent", "settlement",
     "Settlement using double half-cent rounding", "POST",
     "/settlement/calculate.do",
     {"claimId": "120", "coveredAmount": "1.005", "deductible": "",
      "depreciation": "0.00"}, {}),
    ("settlement_policy_cap", "settlement",
     "Settlement capped by the policy limit", "POST",
     "/settlement/calculate.do",
     {"claimId": "119", "coveredAmount": "20000.00", "deductible": "100.00",
      "depreciation": "0.00"}, {}),
    ("settlement_deductible_floor", "settlement",
     "Settlement floored when deductible exceeds the loss", "POST",
     "/settlement/calculate.do",
     {"claimId": "120", "coveredAmount": "1000.00", "deductible": "2000.00",
      "depreciation": "0.00"}, {}),
    ("intake_missing_claimant", "intake",
     "FNOL validation when claimant is absent", "POST", "/intake/submit.do",
     {"claimantName": "", "lossDate": "04/01/2019",
      "description": "Missing claimant validation", "lossType": "WATER"},
     {"claim.123.exists": "/workbench/view.do?claimId=123"}),
    ("intake_missing_description", "intake",
     "FNOL validation when description is absent", "POST", "/intake/submit.do",
     {"claimantName": "Validation Claimant", "lossDate": "04/01/2019",
     "description": "", "lossType": "WATER"},
     {"claim.123.exists": "/workbench/view.do?claimId=123"}),
    ("intake_bad_date", "intake",
     "FNOL validation for an incorrectly formatted date", "POST",
     "/intake/submit.do",
     {"claimantName": "Bad Date Claimant", "lossDate": "2019-02-30",
      "description": "Bad date validation", "lossType": "WATER"},
     {"claim.123.exists": "/workbench/view.do?claimId=123"}),
    ("intake_blank_reserve", "intake",
     "FNOL with a blank initial reserve", "POST", "/intake/submit.do",
     {"claimantName": "Blank Reserve Claimant", "lossDate": "04/01/2019",
      "description": "Blank reserve binding", "lossType": "WATER",
      "initialReserve": ""},
     {"claim.123.reserve_amount": "/workbench/view.do?claimId=123"}),
    ("settlement_authority_refused", "settlement",
     "Save above the adjuster approval limit", "POST",
     "/settlement/save.do",
     {"claimId": "118", "coveredAmount": "5000.00", "deductible": "1000.00",
      "depreciation": "0.00"},
     {"settlement.saved.claim.118.amount": "/settlement/detail.do?claimId=118"},
     {"actor": "adjuster4"}),
    ("settlement_authority_within_limit", "settlement",
     "Save within the adjuster approval limit", "POST",
     "/settlement/save.do",
     {"claimId": "118", "coveredAmount": "3000.00", "deductible": "1000.00",
      "depreciation": "0.00"},
     {"settlement.saved.claim.118.amount": "/settlement/detail.do?claimId=118"},
     {"actor": "adjuster4"}),
    ("settlement_session_save", "settlement",
     "Save the settlement kept in the session by the calculate screen", "GET",
     "/settlement/save.do?claimId=117", {},
     {"settlement.saved.claim.117.amount": "/settlement/detail.do?claimId=117"},
     {"setup": [("POST", "/settlement/calculate.do",
                 {"claimId": "117", "coveredAmount": "3210.55",
                  "deductible": "250.00", "depreciation": "0.00"})]}),
    ("report_closed_claims", "reporting",
     "Claims closed on or before the reporting date", "GET",
     "/report/closedClaims.do", {}, {}),
    ("report_claim_inventory", "reporting",
     "Month-end inventory counting closures from the next day", "GET",
     "/report/claimInventory.do", {}, {}),
]

AREA_PREFIXES = {
    "policy": "policy", "intake": "intake", "workbench": "intake",
    "settlement": "settlement", "payment": "payments", "report": "reports",
    "admin": "shared foundation",
}


def area(scenario):
    prefix = scenario[4].split("/")[1]
    return AREA_PREFIXES.get(prefix, "shared foundation")


def add(name, path, form=None, actor="supervisor", setup=None):
    SCENARIOS.append((name, AREA_PREFIXES.get(path.split("/")[1], "shared foundation"),
                      name.replace("_", " "), "POST" if form is not None else "GET",
                      path, form or {}, {}, {"actor": actor, "setup": setup or []}))


# Read screens include defaults, malformed identifiers and absent records.
for route, parameter in [
        ("policy/view", "policyId"), ("policy/coverages", "policyId"),
        ("policy/coverageDetail", "policyId"), ("policy/insuredParty", "policyId"),
        ("policy/renewal", "policyId"), ("workbench/view", "claimId"),
        ("workbench/notes", "claimId"), ("workbench/reserveHistory", "claimId"),
        ("workbench/statusHistory", "claimId"), ("settlement/detail", "claimId"),
        ("payment/history", "claimId"), ("payment/remittance", "claimId"),
        ("payment/detail", "paymentId"), ("intake/confirm", "claimId")]:
    for label, value in [("default", None), ("invalid", "abc"), ("unknown", "99999")]:
        add(route.replace("/", "_") + "_" + label,
            "/" + route + ".do" + ("?" + parameter + "=" + value if value else ""))

for route in ["home", "admin/adjusters", "admin/reference", "admin/editAdjuster",
              "policy/list", "intake/new", "workbench/list", "workbench/assignment",
              "intake/intake/submit", "logout"]:
    add("screen_" + route.replace("/", "_"), "/" + route + ".do",
        actor="adjusterLogout" if route == "logout" else "adjuster1")

for label, username, password in [("bad_password", "supervisor", "wrong"),
                                  ("empty", "", ""), ("unknown", "nobody", "wrong"),
                                  ("prefix_credentials", "adjusterUnknown", "legacyAnything")]:
    add("login_" + label, "/login.do", {"username": username, "password": password}, actor="adjusterLogin")

for line in ["AUTO", "HOME", "PROPERTY", "", "UNKNOWN"]:
    add("policy_search_" + (line.lower() or "blank"), "/policy/search.do?lineOfBusiness=" + line)

for key, values in [("claimantName", ["", "   "]), ("description", ["", "   "]),
                    ("lossDate", ["", "02/30/2019", "13/01/2019", "2019-04-01", "   "]),
                    ("initialReserve", ["", "abc", "-1", "0", "0.005"])]:
    for index, value in enumerate(values):
        form = {"claimantName": "Parity Claimant", "description": "Parity loss",
                "lossDate": "04/01/2019", "lossType": "FIRE", "initialReserve": "100"}
        form[key] = value
        add("fnol_" + key + "_" + str(index), "/intake/submit.do", form)
add("fnol_all_missing", "/intake/submit.do", {})
add("fnol_then_workbench", "/workbench/list.do", setup=[
    ("POST", "/intake/submit.do", {"claimantName": "Flow Claimant", "description": "Flow loss", "lossDate": ""})])

for key, values in [("coveredAmount", ["", "abc", "$3,000.00", "NaN", "Infinity", "-1", "0", "1.005", "1.025", "0.125", "999.99", "1000", "1000.004", "1000.01"]),
                    ("deductible", ["", " ", "abc", "1,000.00", "NaN", "-500", "1000", "2000"]),
                    ("depreciation", ["abc", "-200", "1000", "2000"]),
                    ("claimId", ["", "abc", "99999"])]:
    for index, value in enumerate(values):
        form = {"claimId": "119", "coveredAmount": "1000", "deductible": "0", "depreciation": "0"}
        if key in ("deductible", "depreciation"):
            form["claimId"] = "118"
        form[key] = value
        add("settlement_boundary_" + key + "_" + str(index), "/settlement/calculate.do", form)

for actor in ["supervisor"] + ["adjuster" + str(n) for n in range(1, 11)] + ["adjusterUnknown"]:
    for label, amount in [("within", "2499.99"), ("at", "2500"), ("above", "2500.01")]:
        add("authority_" + actor + "_" + label, "/settlement/save.do",
            {"claimId": "118", "coveredAmount": amount, "deductible": "0"}, actor=actor)
add("authority_unknown_zero", "/settlement/save.do", {"claimId": "120", "coveredAmount": "1000", "deductible": "2000"}, actor="adjusterUnknown")
for label, setup in [
        ("duplicate", [("POST", "/settlement/save.do", {"claimId": "117", "coveredAmount": "1200", "deductible": "200"})]),
        ("consumed", [("POST", "/settlement/calculate.do", {"claimId": "117", "coveredAmount": "1200"}), ("GET", "/settlement/save.do?claimId=117", {})]),
        ("mismatch", [("POST", "/settlement/calculate.do", {"claimId": "118", "coveredAmount": "1200"})])]:
    add("settlement_session_" + label, "/settlement/save.do?claimId=117",
        {"claimId": "117", "coveredAmount": "1200", "deductible": "200"}
        if label == "duplicate" else None, setup=setup)
add("settlement_unknown_save", "/settlement/save.do", {"claimId": "99999", "coveredAmount": "1000"})

for route in ["index", "openByAdjuster", "lossRatio", "agedClaims", "adjusterWorkload",
              "claimAgingDetail", "reconciliation", "premiumDetail", "closedClaims", "claimInventory"]:
    add("month_end_" + route, "/report/" + route + ".do")
for actor in ["adjuster1", "adjuster5", "adjuster9", "unknown", ""]:
    add("report_claims_" + (actor or "blank"), "/report/adjusterClaims.do?adjuster=" + actor)
for status in ["OPEN", "INVESTIGATING", "APPROVED", "CLOSED", "DENIED"]:
    add("workbench_status_" + status.lower(), "/workbench/status.do", {"claimId": "116", "status": status})
for route, key, value in [("assign", "adjuster", "adjuster9"), ("reserve", "reserveAmount", "0.005"),
                           ("reserve", "reserveAmount", "-1"), ("reserve", "reserveAmount", "abc"),
                           ("note", "noteText", "Parity review")]:
    add("workbench_" + route + "_" + value.replace(".", "_").replace(" ", "_"),
        "/workbench/" + route + ".do", {"claimId": "116", key: value})
for label, form in [("missing", {}), ("no_method", {"claimId": "118", "payeeName": "Parity Payee"}),
                    ("zero", {"claimId": "118", "payeeName": "Parity Payee", "paymentMethod": "CHECK", "amount": "0"}),
                    ("negative", {"claimId": "118", "payeeName": "Parity Payee", "paymentMethod": "CHECK", "amount": "-1"})]:
    add("payment_issue_" + label, "/payment/issue.do", form)

for label, date in [("future", "04/02/2019"), ("zero", "04/01/2019"),
                    ("thirty", "03/02/2019"), ("thirty_one", "03/01/2019"),
                    ("sixty", "01/31/2019"), ("sixty_one", "01/30/2019"),
                    ("ninety", "01/01/2019"), ("ninety_one", "12/31/2018")]:
    add("report_age_" + label, "/report/agedClaims.do", setup=[
        ("POST", "/intake/submit.do", {"claimantName": "Age " + label,
         "description": "Age boundary", "lossDate": date, "initialReserve": "100.99"})])
for route in ["closedClaims", "claimInventory"]:
    add("report_asof_closure_" + route, "/report/" + route + ".do", setup=[
        ("POST", "/workbench/status.do", {"claimId": "115", "status": "CLOSED"})])
for amount in ["4999.99", "5000", "5000.01"]:
    add("authority_adjuster5_limit_" + amount.replace(".", "_"), "/settlement/save.do",
        {"claimId": "118", "coveredAmount": amount, "deductible": "0"}, actor="adjuster5")

SCENARIOS = [scenario[:7] + (dict(scenario[7] if len(scenario) > 7 else {},
                                area=area(scenario)),) for scenario in SCENARIOS]

ACTOR_PASSWORDS = dict({"supervisor": "supervisor", "adjusterUnknown": "legacyUnknown",
                       "adjusterLogin": "legacyLogin", "adjusterLogout": "legacyLogout"},
                       **{"adjuster" + str(n): "legacy" + str(n) for n in range(1, 11)})


def check_routes():
    with open(os.path.join(ROOT, "docs", "migration", "ROUTES.md"), encoding="utf-8") as stream:
        routes = set(re.findall(r"^\| `(/[^`]+\.do)` \|", stream.read(), re.M))
    hit = set()
    for scenario in SCENARIOS:
        hit.add(scenario[4].split("?")[0])
        hit.update(path.split("?")[0] for method, path, form in options(scenario).get("setup", []))
        hit.update(path.split("?")[0] for path in scenario[6].values())
    missing = routes - hit
    if missing:
        raise RuntimeError("Uncovered routes: " + ", ".join(sorted(missing)))
    print("Route coverage: %d/%d mapped routes; %d scenarios" % (len(routes - missing), len(routes), len(SCENARIOS)))


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def request(opener, method, path, form=None):
    url = BASE + path
    data = None
    if form:
        data = urllib.parse.urlencode(form).encode("utf-8")
    req = urllib.request.Request(url, data=data, method=method)
    try:
        response = opener.open(req, timeout=20)
        return response.getcode(), response.geturl(), response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        return error.code, error.geturl(), error.read().decode("utf-8")


def fields(body):
    values = {}
    for name, value in FIELD_RE.findall(body):
        value = re.sub(r"\s+", " ", value).strip()
        if value != "":
            values[name] = value
    return dict(sorted(values.items()))


def result(body, code, location=None):
    if code in (301, 302, 303, 307, 308) and location:
        location = SESSION_RE.sub("", location)
        return "redirect:" + location
    match = VIEW_RE.search(body)
    if match:
        return "forward:" + match.group(1)
    if "Claims System Error" in body:
        return "error:errors.system"
    return "forward:/WEB-INF/jsp/login.jsp"


def probe(opener, key, path):
    code, unused, body = request(opener, "GET", path)
    values = fields(body)
    if key.startswith("settlement.saved.claim."):
        return values.get("detailAmount", "")
    if key.startswith("payment.count.claim."):
        return str(len([name for name in values if name.startswith("paymentAmount_")]))
    if key.startswith("payment.") and key.count(".") >= 2:
        parts = key.split(".")
        payment_id = parts[1]
        field_name = parts[2]
        return values.get("payment" + field_name.title()
                          + "_" + payment_id, "")
    if key.endswith(".status"):
        return values.get("claimStatus",
                          values.get("paymentStatus", values.get("policyStatus",
                                                                 "")))
    if key.endswith(".assigned_adjuster"):
        return values.get("assignedAdjuster", "")
    if key.endswith(".reserve_amount"):
        return values.get("reserveAmount", "")
    if key.endswith(".exists"):
        return "true" if values else "false"
    if key.endswith(".loss_date"):
        return values.get("lossDate", "")
    if ".limit" in key:
        return values.get("policyLimit", "")
    if key.startswith("settlement.claim."):
        return values.get("settlementAmount", "")
    return ""


def reset_database():
    for database_file in glob.glob(os.path.join(ROOT, "target", "db",
                                                "northstar*")):
        try:
            os.remove(database_file)
        except OSError:
            pass
    maven_args = os.environ.get("MAVEN_CAPTURE_ARGS", "").split()
    subprocess.run(
        ["mvn"] + maven_args + ["-q",
         "org.codehaus.mojo:exec-maven-plugin:3.3.0:java"],
        cwd=ROOT, check=True)


def ensure_port_free(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe_socket:
        if probe_socket.connect_ex(("127.0.0.1", port)) == 0:
            raise RuntimeError("port %d is already in use; stop the other "
                               "server before capturing" % port)


def start_server():
    maven_args = os.environ.get("MAVEN_CAPTURE_ARGS", "").split()
    ensure_port_free(HTTP_PORT)
    ensure_port_free(DB_PORT)
    process = subprocess.Popen(["mvn"] + maven_args + [
                                "-q",
                                "-Dclaims.jetty.contextXml=" + CAPTURE_CONTEXT_XML,
                                "-Dclaims.capture.db.port=%d" % DB_PORT,
                                "jetty:run"],
                               cwd=ROOT,
                               stdout=subprocess.DEVNULL,
                               stderr=subprocess.STDOUT)
    deadline = time.time() + 180
    while time.time() < deadline:
        try:
            code, unused, body = request(urllib.request.build_opener(), "GET",
                                          "/login.do")
            if code == 200 and "Claims Login" in body:
                return process
        except Exception:
            pass
        time.sleep(1)
    process.terminate()
    raise RuntimeError("Jetty did not become ready")


class DatabaseSnapshots:
    """Reads every table through the HSQLDB server Jetty runs for capture."""

    def __init__(self, url=DB_URL, user="SA", password=""):
        maven_args = os.environ.get("MAVEN_CAPTURE_ARGS", "").split()
        classpath_file = os.path.join(ROOT, "target", "capture",
                                      "hsqldb.classpath")
        subprocess.run(
            ["mvn"] + maven_args + ["-q",
             DEPENDENCY_PLUGIN + ":build-classpath",
             "-DincludeArtifactIds=hsqldb",
             "-Dmdep.outputFile=" + classpath_file],
            cwd=ROOT, check=True)
        with open(classpath_file, encoding="utf-8") as stream:
            classpath = stream.read().strip()
        self.today = datetime.date.today().isoformat()
        self.process = subprocess.Popen(
            ["java", "-cp", classpath,
             os.path.join(ROOT, "tools", "capture", "DbSnapshot.java"),
             url, user, password],
            cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            text=True, encoding="utf-8")

    def take(self):
        self.process.stdin.write("snapshot\n")
        self.process.stdin.flush()
        line = self.process.stdout.readline()
        if not line:
            raise RuntimeError("database snapshot helper exited with %s"
                               % self.process.wait())
        return self.normalize(json.loads(line))

    def normalize(self, raw):
        tables = {}
        for table in raw["tables"]:
            columns = table["columns"]
            rows = [[self.value(table["name"], column, value)
                     for column, value in zip(columns, row)]
                    for row in table["rows"]]
            tables[table["name"]] = {
                "columns": [[column["name"], column["type"]]
                            for column in columns],
                "primary_key": table["primary_key"],
                "rows": rows
            }
        return tables

    def value(self, table, column, value):
        if value is None:
            return None
        placeholder = VOLATILE_COLUMNS.get((table, column["name"]),
                                           VOLATILE_TYPES.get(column["type"]))
        if placeholder:
            return placeholder
        if column["type"] == "DATE" and value == self.today:
            return "<today>"
        if isinstance(value, str):
            return SESSION_RE.sub("", value)
        return value

    def close(self):
        try:
            self.process.stdin.close()
        except OSError:
            pass
        try:
            self.process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            self.process.kill()
            self.process.wait()


def render_snapshot(name, tables):
    def text(value):
        return json.dumps(value, ensure_ascii=False)
    lines = ["{", '  "scenario": %s,' % text(name), '  "tables": {']
    names = sorted(tables)
    for table_index, table_name in enumerate(names):
        table = tables[table_name]
        lines.append("    %s: {" % text(table_name))
        lines.append('      "columns": %s,' % text(table["columns"]))
        lines.append('      "primary_key": %s,' % text(table["primary_key"]))
        if table["rows"]:
            lines.append('      "rows": [')
            for row_index, row in enumerate(table["rows"]):
                comma = "," if row_index < len(table["rows"]) - 1 else ""
                lines.append("        " + text(row) + comma)
            lines.append("      ]")
        else:
            lines.append('      "rows": []')
        lines.append("    }" + ("," if table_index < len(names) - 1 else ""))
    lines.append("  }")
    lines.append("}")
    return "\n".join(lines) + "\n"


def write_snapshot(name, tables):
    with open(os.path.join(SNAPSHOT_DIR, name + ".json"), "w",
              encoding="utf-8") as stream:
        stream.write(render_snapshot(name, tables))


def options(scenario):
    return scenario[7] if len(scenario) > 7 else {}


def login(actor):
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(jar))
    request(opener, "GET", "/login.do")
    request(opener, "POST", "/login.do",
            {"username": actor, "password": ACTOR_PASSWORDS[actor]})
    return opener


def write_transcript(opener, scenario, snapshots, persist=True, database=None):
    name, module, description, method, path, form, probes = scenario[:7]
    extra = options(scenario)
    setup = extra.get("setup", [])
    for setup_method, setup_path, setup_form in setup:
        request(opener, setup_method, setup_path, setup_form)
    code, location, body = request(opener, method, path, form)
    if persist:
        write_snapshot(name, snapshots.take())
    elif database is not None:
        database.update(snapshots.take())
    state = {}
    for key, probe_path in sorted(probes.items()):
        state[key] = probe(opener, key, probe_path)
    transcript = {
        "scenario": name,
        "description": description,
        "actor": extra.get("actor", "supervisor"),
        "request": {
            "method": method,
            "path": CONTEXT_PATH + path,
            "form": form
        },
        "expected": {
            "status": code,
            "result": result(body, code, location),
            "business_fields": fields(body),
            "validation_errors": ERROR_RE.findall(body),
            "db_state": state
        },
        "db_snapshot": "db/" + name + ".json"
    }
    if setup:
        transcript["setup"] = [{
            "method": setup_method,
            "path": CONTEXT_PATH + setup_path,
            "form": setup_form
        } for setup_method, setup_path, setup_form in setup]
    return transcript


def compare_file(path, actual, label):
    try:
        with open(path, encoding="utf-8") as stream:
            expected = json.load(stream)
    except (OSError, ValueError) as error:
        print("%s: cannot read expectation: %s" % (label, error))
        return 1
    if expected == actual:
        return 0
    print("".join(difflib.unified_diff(
        (json.dumps(expected, indent=2, sort_keys=True) + "\n").splitlines(True),
        (json.dumps(actual, indent=2, sort_keys=True) + "\n").splitlines(True),
        fromfile=label + " expected", tofile=label + " actual")), end="")
    return 1


def verify_manifest(path):
    with open(path, encoding="utf-8") as stream:
        manifest = json.load(stream)
    errors = 0
    for relative, expected in manifest["files"].items():
        try:
            with open(os.path.join(ROOT, relative), "rb") as stream:
                actual = hashlib.sha256(stream.read()).hexdigest()
        except OSError:
            actual = None
        if actual != expected:
            print("Manifest mismatch: " + relative)
            errors += 1
    if not manifest["files"]:
        raise ValueError("Manifest contains no frozen files")
    print("Manifest: %d files, %d mismatches" % (len(manifest["files"]), errors))
    return errors


def main():
    global BASE
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", nargs="?", choices=["capture", "replay"], default="capture")
    parser.add_argument("--base-url", default=BASE)
    parser.add_argument("--start-legacy", action="store_true")
    parser.add_argument("--db-url", default=DB_URL)
    parser.add_argument("--db-user", default="SA")
    parser.add_argument("--db-password", default=os.environ.get("CAPTURE_DB_PASSWORD", ""))
    parser.add_argument("--expectations", default=os.path.join(ROOT, "transcripts"))
    parser.add_argument("--verify-manifest", metavar="PATH")
    parser.add_argument("--no-reset", action="store_true")
    parser.add_argument("--area", choices=sorted(set(AREA_PREFIXES.values())))
    parser.add_argument("--check-routes", action="store_true")
    args = parser.parse_args()
    if args.verify_manifest:
        return bool(verify_manifest(args.verify_manifest))
    BASE = args.base_url.rstrip("/")
    check_routes()
    if args.check_routes:
        return
    process = None
    snapshots = None
    try:
        replay = args.mode == "replay"
        if not replay or args.start_legacy:
            if not args.no_reset:
                reset_database()
            process = start_server()
        snapshots = DatabaseSnapshots(args.db_url, args.db_user, args.db_password)
        differences = 0
        count = 0
        if replay:
            differences += compare_file(os.path.join(args.expectations, "db", "_seed.json"),
                                        {"scenario": SEED_SNAPSHOT, "tables": snapshots.take()}, "_seed database")
        os.makedirs(SNAPSHOT_DIR, exist_ok=True)
        if not replay and not args.area:
            for stale in glob.glob(os.path.join(SNAPSHOT_DIR, "*.json")):
                os.remove(stale)
        if not replay:
            write_snapshot(SEED_SNAPSHOT, snapshots.take())
        jar = http.cookiejar.CookieJar()
        opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(jar))
        request(opener, "GET", "/login.do")
        actors = {}
        for scenario in SCENARIOS:
            actor = options(scenario).get("actor")
            if actor and actor not in actors:
                actors[actor] = login(actor)
            selected = not args.area or area(scenario) == args.area
            database = {} if replay and selected else None
            transcript = write_transcript(actors.get(actor, opener), scenario,
                                          snapshots, persist=selected and not replay,
                                          database=database)
            if not selected:
                continue
            if replay:
                count += 1
                name = scenario[0]
                differences += compare_file(os.path.join(args.expectations, name + ".json"),
                                            transcript, name + " response")
                differences += compare_file(os.path.join(args.expectations, "db", name + ".json"),
                                            {"scenario": name, "tables": database}, name + " database")
                continue
            path = os.path.join(ROOT, "transcripts",
                                transcript["scenario"] + ".json")
            with open(path, "w", encoding="utf-8") as stream:
                json.dump(transcript, stream, indent=2, sort_keys=True)
                stream.write("\n")
        if replay:
            print("Replay: %d scenarios, %d diffs" % (count, differences))
            return bool(differences)
        index = [{
            "scenario": scenario[0],
            "module": scenario[1],
            "area": options(scenario)["area"],
            "description": scenario[2]
        } for scenario in SCENARIOS]
        with open(os.path.join(ROOT, "transcripts", "index.json"), "w",
                  encoding="utf-8") as stream:
            json.dump(index, stream, indent=2, sort_keys=True)
            stream.write("\n")
    finally:
        if snapshots is not None:
            snapshots.close()
        if process is not None:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    sys.exit(main())
