#!/usr/bin/env python3
"""Captures deterministic NorthStar claims workflow transcripts."""

import argparse
import base64
import glob
import http.cookiejar
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request
import urllib.error
import xml.etree.ElementTree as ET


ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BASE = "http://localhost:8080/claims"
CONTEXT_PATH = "/claims"
FIELD_RE = re.compile(r'<span id="f_([^"]+)">(.*?)</span>', re.S)
VIEW_RE = re.compile(r"<!--\s*ns:view\s+([^ ]+)\s*-->")
ERROR_RE = re.compile(r"<!--\s*ns:error\s+([^ ]+)\s*-->")
CAPTURE = os.path.join(ROOT, "tools", "capture")
HSQLDB_JAR = os.path.expanduser(
    "~/.m2/repository/org/hsqldb/hsqldb/2.7.3/hsqldb-2.7.3.jar")


def schema_tables(schema):
    tables = {}
    schema = re.sub(r"--[^\n]*", "", schema)
    for name, definition in re.findall(
            r"CREATE\s+TABLE\s+(\w+)\s*\((.*?)\);", schema, re.I | re.S):
        primary_key = re.search(r"PRIMARY\s+KEY\s*\(([^)]+)\)",
                                definition, re.I)
        if not primary_key:
            raise ValueError("Missing primary key for " + name)
        columns = [column.strip().upper()
                   for column in primary_key.group(1).split(",")]
        if not all(re.fullmatch(r"\w+", column) for column in columns):
            raise ValueError("Invalid primary key for " + name)
        tables[name.upper()] = ",".join(columns)
    if not tables:
        raise ValueError("No schema tables found")
    return tables


def decode_rows(output):
    def decode(line):
        return [None if value == "-" else base64.b64decode(
            value, validate=True).decode("utf-8") for value in line.split("\t")]
    lines = output.splitlines()
    columns = decode(lines[0])
    rows = []
    for line in lines[1:]:
        values = decode(line)
        if len(values) != len(columns):
            raise ValueError("JDBC row width does not match metadata")
        rows.append(dict(zip(columns, values)))
    return rows


def prepare_snapshots():
    classes = os.path.join(ROOT, "target", "capture-classes")
    os.makedirs(classes, exist_ok=True)
    subprocess.run(["javac", "-cp", HSQLDB_JAR, "-d", classes,
                    os.path.join(CAPTURE, "DatabaseSnapshot.java")], check=True)
    with open(os.path.join(ROOT, "src/main/resources/db/schema.sql"),
              encoding="utf-8") as stream:
        return schema_tables(stream.read()), classes


def capture_pom():
    namespace = "http://maven.apache.org/POM/4.0.0"
    ET.register_namespace("", namespace)
    def tag(name):
        return "{" + namespace + "}" + name
    tree = ET.parse(os.path.join(ROOT, "pom.xml"))
    build = tree.getroot().find(tag("build"))
    for name, value in [("directory", "target"),
                        ("sourceDirectory", "src/main/java")]:
        ET.SubElement(build, tag(name)).text = os.path.join(ROOT, value)
    resources = ET.SubElement(build, tag("resources"))
    resource = ET.SubElement(resources, tag("resource"))
    ET.SubElement(resource, tag("directory")).text = os.path.join(
        ROOT, "src/main/resources")
    for plugin in build.find(tag("plugins")):
        if plugin.find(tag("artifactId")).text == "jetty-maven-plugin":
            config = plugin.find(tag("configuration"))
            config.find(tag("contextXml")).text = os.path.join(
                CAPTURE, "jetty-context.xml")
            ET.SubElement(config, tag("webAppSourceDirectory")).text = (
                os.path.join(ROOT, "src/main/webapp"))
            ET.SubElement(config.find(tag("webApp")), tag("war")).text = (
                os.path.join(ROOT, "src/main/webapp"))
    directory = os.path.join(CAPTURE, "target")
    os.makedirs(directory, exist_ok=True)
    path = os.path.join(directory, "pom.xml")
    tree.write(path, encoding="utf-8", xml_declaration=True)
    return path


def write_snapshot(name, tables, classes):
    snapshot = {}
    for table, primary_key in tables.items():
        output = subprocess.check_output(
            ["java", "-cp", os.pathsep.join([classes, HSQLDB_JAR]),
             "DatabaseSnapshot", table, primary_key], text=True)
        snapshot[table] = decode_rows(output)
    directory = os.path.join(ROOT, "transcripts", "db_snapshots")
    os.makedirs(directory, exist_ok=True)
    with open(os.path.join(directory, name + ".json"), "w",
              encoding="utf-8") as stream:
        json.dump(snapshot, stream, indent=2, sort_keys=True)
        stream.write("\n")


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

    ("session_login_ok", "session", "Login in an isolated session", "POST",
     "/login.do", {"username":"supervisor","password":"supervisor"}, {},
     {"area":"session","session":"anonymous"}),
    ("session_login_adjuster", "session", "Login as a seeded adjuster", "POST",
     "/login.do", {"username":"adjuster4","password":"legacy4"}, {},
     {"area":"session","session":"anonymous"}),
    ("session_login_bad_password", "session", "Login with bad password", "POST",
     "/login.do", {"username":"supervisor","password":"incorrect"}, {},
     {"area":"session","session":"anonymous"}),
    ("session_login_unknown_user", "session", "Login with unknown user", "POST",
     "/login.do", {"username":"unknown","password":"incorrect"}, {},
     {"area":"session","session":"anonymous"}),
    ("session_login_blank_fields", "session", "Login with blank fields", "POST",
     "/login.do", {"username":"","password":""}, {},
     {"area":"session","session":"anonymous"}),
    ("session_login_blank_username", "session", "Login with blank username", "POST",
     "/login.do", {"username":"","password":"supervisor"}, {},
     {"area":"session","session":"anonymous"}),
    ("session_login_blank_password", "session", "Login with blank password", "POST",
     "/login.do", {"username":"supervisor","password":""}, {},
     {"area":"session","session":"anonymous"}),
    ("session_home_authenticated", "session", "Authenticated home", "GET",
     "/home.do", {}, {},
     {"area":"session"}),
    ("session_home_anonymous", "session", "Anonymous home redirect", "GET",
     "/home.do", {}, {},
     {"area":"session","session":"anonymous"}),
    ("session_logout", "session", "Logout without changing the shared session", "GET",
     "/logout.do", {}, {},
     {"area":"session","session":"fresh"}),
    ("session_protected_after_logout", "session", "Protected page after logout", "GET",
     "/policy/list.do", {}, {},
     {"area":"session","session":"fresh","setup":[["GET","/logout.do",{}]]}),
    ("policy_search_auto", "policy", "Policy search line AUTO", "GET",
     "/policy/search.do?lineOfBusiness=AUTO", {}, {},
     {"area":"policy"}),
    ("policy_search_homeowners", "policy", "Policy search line HOMEOWNERS", "GET",
     "/policy/search.do?lineOfBusiness=HOMEOWNERS", {}, {},
     {"area":"policy"}),
    ("policy_search_commercial_property", "policy", "Policy search line COMMERCIAL_PROPERTY", "GET",
     "/policy/search.do?lineOfBusiness=COMMERCIAL_PROPERTY", {}, {},
     {"area":"policy"}),
    ("policy_search_general_liability", "policy", "Policy search line GENERAL_LIABILITY", "GET",
     "/policy/search.do?lineOfBusiness=GENERAL_LIABILITY", {}, {},
     {"area":"policy"}),
    ("policy_search_marine", "policy", "Policy search line MARINE", "GET",
     "/policy/search.do?lineOfBusiness=MARINE", {}, {},
     {"area":"policy"}),
    ("policy_search_blank", "policy", "Policy search line blank", "GET",
     "/policy/search.do?lineOfBusiness=", {}, {},
     {"area":"policy"}),
    ("policy_search_default", "policy", "Policy search with no criteria", "GET",
     "/policy/search.do", {}, {},
     {"area":"policy"}),
    ("policy_search_policyNumber", "policy", "Search with ignored policyNumber criterion", "GET",
     "/policy/search.do?policyNumber=NS-09002", {}, {},
     {"area":"policy"}),
    ("policy_search_insuredName", "policy", "Search with ignored insuredName criterion", "GET",
     "/policy/search.do?insuredName=Policyholder%202", {}, {},
     {"area":"policy"}),
    ("policy_search_term", "policy", "Search with ignored term criterion", "GET",
     "/policy/search.do?term=NS-00003", {}, {},
     {"area":"policy"}),
    ("policy_list", "policy", "List all policies", "GET",
     "/policy/list.do", {}, {},
     {"area":"policy"}),
    ("policy_view_1", "policy", "View policy 1", "GET",
     "/policy/view.do?policyId=1", {}, {},
     {"area":"policy"}),
    ("policy_view_2", "policy", "View policy 2", "GET",
     "/policy/view.do?policyId=2", {}, {},
     {"area":"policy"}),
    ("policy_view_3", "policy", "View policy 3", "GET",
     "/policy/view.do?policyId=3", {}, {},
     {"area":"policy"}),
    ("policy_view_4", "policy", "View policy 4", "GET",
     "/policy/view.do?policyId=4", {}, {},
     {"area":"policy"}),
    ("policy_view_9002", "policy", "View policy 9002", "GET",
     "/policy/view.do?policyId=9002", {}, {},
     {"area":"policy"}),
    ("policy_view_99999", "policy", "View policy 99999", "GET",
     "/policy/view.do?policyId=99999", {}, {},
     {"area":"policy"}),
    ("policy_view_invalid", "policy", "View policy invalid", "GET",
     "/policy/view.do?policyId=invalid", {}, {},
     {"area":"policy"}),
    ("policy_view_blank", "policy", "View policy blank", "GET",
     "/policy/view.do?policyId=", {}, {},
     {"area":"policy"}),
    ("policy_view_default", "policy", "View without a policy id", "GET",
     "/policy/view.do", {}, {},
     {"area":"policy"}),
    ("policy_coverages_9001", "policy", "coverages for policy 9001", "GET",
     "/policy/coverages.do?policyId=9001", {}, {},
     {"area":"policy"}),
    ("policy_coverages_9002", "policy", "coverages for policy 9002", "GET",
     "/policy/coverages.do?policyId=9002", {}, {},
     {"area":"policy"}),
    ("policy_coverages_99999", "policy", "coverages for policy 99999", "GET",
     "/policy/coverages.do?policyId=99999", {}, {},
     {"area":"policy"}),
    ("policy_coverages_invalid", "policy", "coverages for policy invalid", "GET",
     "/policy/coverages.do?policyId=invalid", {}, {},
     {"area":"policy"}),
    ("policy_coverages_blank", "policy", "coverages for policy blank", "GET",
     "/policy/coverages.do?policyId=", {}, {},
     {"area":"policy"}),
    ("policy_coverages_default", "policy", "coverages without policy id", "GET",
     "/policy/coverages.do", {}, {},
     {"area":"policy"}),
    ("policy_coverageDetail_9001", "policy", "coverageDetail for policy 9001", "GET",
     "/policy/coverageDetail.do?policyId=9001", {}, {},
     {"area":"policy"}),
    ("policy_coverageDetail_9002", "policy", "coverageDetail for policy 9002", "GET",
     "/policy/coverageDetail.do?policyId=9002", {}, {},
     {"area":"policy"}),
    ("policy_coverageDetail_99999", "policy", "coverageDetail for policy 99999", "GET",
     "/policy/coverageDetail.do?policyId=99999", {}, {},
     {"area":"policy"}),
    ("policy_coverageDetail_invalid", "policy", "coverageDetail for policy invalid", "GET",
     "/policy/coverageDetail.do?policyId=invalid", {}, {},
     {"area":"policy"}),
    ("policy_coverageDetail_blank", "policy", "coverageDetail for policy blank", "GET",
     "/policy/coverageDetail.do?policyId=", {}, {},
     {"area":"policy"}),
    ("policy_coverageDetail_default", "policy", "coverageDetail without policy id", "GET",
     "/policy/coverageDetail.do", {}, {},
     {"area":"policy"}),
    ("policy_insuredParty_9001", "policy", "insuredParty for policy 9001", "GET",
     "/policy/insuredParty.do?policyId=9001", {}, {},
     {"area":"policy"}),
    ("policy_insuredParty_9002", "policy", "insuredParty for policy 9002", "GET",
     "/policy/insuredParty.do?policyId=9002", {}, {},
     {"area":"policy"}),
    ("policy_insuredParty_99999", "policy", "insuredParty for policy 99999", "GET",
     "/policy/insuredParty.do?policyId=99999", {}, {},
     {"area":"policy"}),
    ("policy_insuredParty_invalid", "policy", "insuredParty for policy invalid", "GET",
     "/policy/insuredParty.do?policyId=invalid", {}, {},
     {"area":"policy"}),
    ("policy_insuredParty_blank", "policy", "insuredParty for policy blank", "GET",
     "/policy/insuredParty.do?policyId=", {}, {},
     {"area":"policy"}),
    ("policy_insuredParty_default", "policy", "insuredParty without policy id", "GET",
     "/policy/insuredParty.do", {}, {},
     {"area":"policy"}),
    ("policy_renewal_9001", "policy", "renewal for policy 9001", "GET",
     "/policy/renewal.do?policyId=9001", {}, {},
     {"area":"policy"}),
    ("policy_renewal_9002", "policy", "renewal for policy 9002", "GET",
     "/policy/renewal.do?policyId=9002", {}, {},
     {"area":"policy"}),
    ("policy_renewal_99999", "policy", "renewal for policy 99999", "GET",
     "/policy/renewal.do?policyId=99999", {}, {},
     {"area":"policy"}),
    ("policy_renewal_invalid", "policy", "renewal for policy invalid", "GET",
     "/policy/renewal.do?policyId=invalid", {}, {},
     {"area":"policy"}),
    ("policy_renewal_blank", "policy", "renewal for policy blank", "GET",
     "/policy/renewal.do?policyId=", {}, {},
     {"area":"policy"}),
    ("policy_renewal_default", "policy", "renewal without policy id", "GET",
     "/policy/renewal.do", {}, {},
     {"area":"policy"}),
    ("policy_renewal_post", "policy", "Renewal POST remains a read-only review", "POST",
     "/policy/renewal.do", {"policyId":"9002","expiryDate":"","policyLimit":"","deductible":""}, {"policy.9002.limit":"/policy/view.do?policyId=9002"},
     {"area":"policy"}),
    ("admin_adjusters_authenticated", "admin", "Authenticated admin adjusters", "GET",
     "/admin/adjusters.do", {}, {},
     {"area":"admin"}),
    ("admin_adjusters_anonymous", "admin", "Anonymous admin adjusters", "GET",
     "/admin/adjusters.do", {}, {},
     {"area":"admin","session":"anonymous"}),
    ("admin_reference_authenticated", "admin", "Authenticated admin reference", "GET",
     "/admin/reference.do", {}, {},
     {"area":"admin"}),
    ("admin_reference_anonymous", "admin", "Anonymous admin reference", "GET",
     "/admin/reference.do", {}, {},
     {"area":"admin","session":"anonymous"}),
    ("admin_editAdjuster_authenticated", "admin", "Authenticated admin editAdjuster", "GET",
     "/admin/editAdjuster.do", {}, {},
     {"area":"admin"}),
    ("admin_editAdjuster_anonymous", "admin", "Anonymous admin editAdjuster", "GET",
     "/admin/editAdjuster.do", {}, {},
     {"area":"admin","session":"anonymous"}),
    ("intake_new_form", "intake", "Open the new FNOL form", "GET",
     "/intake/new.do", {}, {},
     {"area": "intake"}),
    ("intake_loss_type_fire", "intake", "loss type fire", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "FIRE"}, {"claim.124.status": "/workbench/view.do?claimId=124", "claim.124.reserve_amount": "/workbench/view.do?claimId=124", "claim.124.assigned_adjuster": "/workbench/view.do?claimId=124", "claim.124.loss_date": "/workbench/view.do?claimId=124"},
     {"area": "intake"}),
    ("intake_loss_type_water", "intake", "loss type water", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.125.status": "/workbench/view.do?claimId=125", "claim.125.reserve_amount": "/workbench/view.do?claimId=125", "claim.125.assigned_adjuster": "/workbench/view.do?claimId=125", "claim.125.loss_date": "/workbench/view.do?claimId=125"},
     {"area": "intake"}),
    ("intake_loss_type_liability", "intake", "loss type liability", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "LIABILITY"}, {"claim.126.status": "/workbench/view.do?claimId=126", "claim.126.reserve_amount": "/workbench/view.do?claimId=126", "claim.126.assigned_adjuster": "/workbench/view.do?claimId=126", "claim.126.loss_date": "/workbench/view.do?claimId=126"},
     {"area": "intake"}),
    ("intake_loss_type_collision", "intake", "loss type collision", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "COLLISION"}, {"claim.127.status": "/workbench/view.do?claimId=127", "claim.127.reserve_amount": "/workbench/view.do?claimId=127", "claim.127.assigned_adjuster": "/workbench/view.do?claimId=127", "claim.127.loss_date": "/workbench/view.do?claimId=127"},
     {"area": "intake"}),
    ("intake_reserve_positive", "intake", "reserve positive", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER", "initialReserve": "2500.75"}, {"claim.128.status": "/workbench/view.do?claimId=128", "claim.128.reserve_amount": "/workbench/view.do?claimId=128", "claim.128.assigned_adjuster": "/workbench/view.do?claimId=128", "claim.128.loss_date": "/workbench/view.do?claimId=128"},
     {"area": "intake"}),
    ("intake_reserve_zero", "intake", "reserve zero", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER", "initialReserve": "0"}, {"claim.129.status": "/workbench/view.do?claimId=129", "claim.129.reserve_amount": "/workbench/view.do?claimId=129", "claim.129.assigned_adjuster": "/workbench/view.do?claimId=129", "claim.129.loss_date": "/workbench/view.do?claimId=129"},
     {"area": "intake"}),
    ("intake_reserve_negative", "intake", "reserve negative", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER", "initialReserve": "-25.00"}, {"claim.130.status": "/workbench/view.do?claimId=130", "claim.130.reserve_amount": "/workbench/view.do?claimId=130", "claim.130.assigned_adjuster": "/workbench/view.do?claimId=130", "claim.130.loss_date": "/workbench/view.do?claimId=130"},
     {"area": "intake"}),
    ("intake_reserve_blank_coercion", "intake", "reserve blank coercion", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER", "initialReserve": ""}, {"claim.131.status": "/workbench/view.do?claimId=131", "claim.131.reserve_amount": "/workbench/view.do?claimId=131", "claim.131.assigned_adjuster": "/workbench/view.do?claimId=131", "claim.131.loss_date": "/workbench/view.do?claimId=131"},
     {"area": "intake"}),
    ("intake_reserve_nonnumeric_coercion", "intake", "reserve nonnumeric coercion", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER", "initialReserve": "ordinary"}, {"claim.132.status": "/workbench/view.do?claimId=132", "claim.132.reserve_amount": "/workbench/view.do?claimId=132", "claim.132.assigned_adjuster": "/workbench/view.do?claimId=132", "claim.132.loss_date": "/workbench/view.do?claimId=132"},
     {"area": "intake"}),
    ("intake_required_blank_claimantName", "intake", "required blank claimantName", "POST",
     "/intake/submit.do", {"claimantName": "", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.133.exists": "/workbench/view.do?claimId=133"},
     {"area": "intake"}),
    ("intake_required_blank_lossDate", "intake", "required blank lossDate", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.133.status": "/workbench/view.do?claimId=133", "claim.133.reserve_amount": "/workbench/view.do?claimId=133", "claim.133.assigned_adjuster": "/workbench/view.do?claimId=133", "claim.133.loss_date": "/workbench/view.do?claimId=133"},
     {"area": "intake"}),
    ("intake_required_blank_description", "intake", "required blank description", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "", "lossType": "WATER"}, {"claim.134.exists": "/workbench/view.do?claimId=134"},
     {"area": "intake"}),
    ("intake_required_missing_claimantName", "intake", "required missing claimantName", "POST",
     "/intake/submit.do", {"lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.134.exists": "/workbench/view.do?claimId=134"},
     {"area": "intake"}),
    ("intake_required_missing_lossDate", "intake", "required missing lossDate", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.134.status": "/workbench/view.do?claimId=134", "claim.134.reserve_amount": "/workbench/view.do?claimId=134", "claim.134.assigned_adjuster": "/workbench/view.do?claimId=134", "claim.134.loss_date": "/workbench/view.do?claimId=134"},
     {"area": "intake"}),
    ("intake_required_missing_description", "intake", "required missing description", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "lossType": "WATER"}, {"claim.135.exists": "/workbench/view.do?claimId=135"},
     {"area": "intake"}),
    ("intake_date_leap_year", "intake", "date leap year", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "02/29/2020", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.135.status": "/workbench/view.do?claimId=135", "claim.135.reserve_amount": "/workbench/view.do?claimId=135", "claim.135.assigned_adjuster": "/workbench/view.do?claimId=135", "claim.135.loss_date": "/workbench/view.do?claimId=135"},
     {"area": "intake"}),
    ("intake_date_nonleap_february", "intake", "date nonleap february", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "02/29/2019", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.136.status": "/workbench/view.do?claimId=136", "claim.136.reserve_amount": "/workbench/view.do?claimId=136", "claim.136.assigned_adjuster": "/workbench/view.do?claimId=136", "claim.136.loss_date": "/workbench/view.do?claimId=136"},
     {"area": "intake"}),
    ("intake_date_april_rollover", "intake", "date april rollover", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "04/31/2019", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.137.status": "/workbench/view.do?claimId=137", "claim.137.reserve_amount": "/workbench/view.do?claimId=137", "claim.137.assigned_adjuster": "/workbench/view.do?claimId=137", "claim.137.loss_date": "/workbench/view.do?claimId=137"},
     {"area": "intake"}),
    ("intake_date_month_rollover", "intake", "date month rollover", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "13/01/2019", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.138.status": "/workbench/view.do?claimId=138", "claim.138.reserve_amount": "/workbench/view.do?claimId=138", "claim.138.assigned_adjuster": "/workbench/view.do?claimId=138", "claim.138.loss_date": "/workbench/view.do?claimId=138"},
     {"area": "intake"}),
    ("intake_date_zero_month_day", "intake", "date zero month day", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "00/00/2019", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.139.status": "/workbench/view.do?claimId=139", "claim.139.reserve_amount": "/workbench/view.do?claimId=139", "claim.139.assigned_adjuster": "/workbench/view.do?claimId=139", "claim.139.loss_date": "/workbench/view.do?claimId=139"},
     {"area": "intake"}),
    ("intake_date_format_rejected", "intake", "date format rejected", "POST",
     "/intake/submit.do", {"claimantName": "Capture Claimant", "lossDate": "2019-04-01", "description": "Intake workflow capture", "lossType": "WATER"}, {"claim.140.exists": "/workbench/view.do?claimId=140"},
     {"area": "intake"}),
    ("intake_confirm_after_setup", "intake", "Confirm a claim created by setup", "GET",
     "/intake/confirm.do?claimId=140", {}, {"claim.140.status": "/workbench/view.do?claimId=140", "claim.140.reserve_amount": "/workbench/view.do?claimId=140", "claim.140.assigned_adjuster": "/workbench/view.do?claimId=140", "claim.140.loss_date": "/workbench/view.do?claimId=140"},
     {"area": "intake", "setup": [["POST", "/intake/submit.do",{"claimantName": "Capture Claimant", "lossDate": "04/01/2019", "description": "Intake workflow capture", "lossType": "WATER"}]]}),
    ("intake_confirm_missing", "intake", "Confirm with missing claim id", "GET",
     "/intake/confirm.do", {}, {},
     {"area": "intake"}),
    ("intake_confirm_invalid", "intake", "Confirm with invalid claim id", "GET",
     "/intake/confirm.do?claimId=invalid", {}, {},
     {"area": "intake"}),
    ("intake_confirm_not_found", "intake", "Confirm with not_found claim id", "GET",
     "/intake/confirm.do?claimId=99999", {}, {},
     {"area": "intake"}),
    ("workbench_list_default", "workbench", "List open claims", "GET",
     "/workbench/list.do", {}, {},
     {"area": "workbench"}),
    ("workbench_list_ignored_filters", "workbench", "Status adjuster and paging parameters are ignored", "GET",
     "/workbench/list.do?status=CLOSED&adjuster=adjuster4&page=2&pageSize=5", {}, {},
     {"area": "workbench"}),
    ("workbench_view_1", "workbench", "View claim 1", "GET",
     "/workbench/view.do?claimId=1", {}, {},
     {"area": "workbench"}),
    ("workbench_view_119", "workbench", "View claim 119", "GET",
     "/workbench/view.do?claimId=119", {}, {},
     {"area": "workbench"}),
    ("workbench_view_120", "workbench", "View claim 120", "GET",
     "/workbench/view.do?claimId=120", {}, {},
     {"area": "workbench"}),
    ("workbench_view_124", "workbench", "View claim 124", "GET",
     "/workbench/view.do?claimId=124", {}, {},
     {"area": "workbench"}),
    ("workbench_view_140", "workbench", "View claim 140", "GET",
     "/workbench/view.do?claimId=140", {}, {},
     {"area": "workbench"}),
    ("workbench_view_99999", "workbench", "View claim 99999", "GET",
     "/workbench/view.do?claimId=99999", {}, {},
     {"area": "workbench"}),
    ("workbench_view_invalid", "workbench", "View claim invalid", "GET",
     "/workbench/view.do?claimId=invalid", {}, {},
     {"area": "workbench"}),
    ("workbench_assign_adjuster1", "workbench", "Assign claim to adjuster1", "POST",
     "/workbench/assign.do", {"claimId": "124", "adjuster": "adjuster1"}, {"claim.124.assigned_adjuster": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_assign_adjuster2", "workbench", "Assign claim to adjuster2", "POST",
     "/workbench/assign.do", {"claimId": "124", "adjuster": "adjuster2"}, {"claim.124.assigned_adjuster": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_assign_adjuster3", "workbench", "Assign claim to adjuster3", "POST",
     "/workbench/assign.do", {"claimId": "124", "adjuster": "adjuster3"}, {"claim.124.assigned_adjuster": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_assign_adjuster4", "workbench", "Assign claim to adjuster4", "POST",
     "/workbench/assign.do", {"claimId": "124", "adjuster": "adjuster4"}, {"claim.124.assigned_adjuster": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_assign_adjuster5", "workbench", "Assign claim to adjuster5", "POST",
     "/workbench/assign.do", {"claimId": "124", "adjuster": "adjuster5"}, {"claim.124.assigned_adjuster": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_assign_supervisor", "workbench", "Assign claim to supervisor", "POST",
     "/workbench/assign.do", {"claimId": "124", "adjuster": "supervisor"}, {"claim.124.assigned_adjuster": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_assign_blank_default", "workbench", "Assign claim to blank default", "POST",
     "/workbench/assign.do", {"claimId": "124", "adjuster": ""}, {"claim.124.assigned_adjuster": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_assign_invalid_id", "workbench", "Invalid claim id defaults during assignment", "POST",
     "/workbench/assign.do", {"claimId": "invalid", "adjuster": "adjuster1"}, {"claim.119.assigned_adjuster": "/workbench/view.do?claimId=119"},
     {"area": "workbench"}),
    ("workbench_status_open", "workbench", "Set status OPEN", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "OPEN"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_status_investigating", "workbench", "Set status INVESTIGATING", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "INVESTIGATING"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_status_approved", "workbench", "Set status APPROVED", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "APPROVED"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_status_denied", "workbench", "Set status DENIED", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "DENIED"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_status_closed", "workbench", "Set status CLOSED", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "CLOSED"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_status_pending", "workbench", "Set status PENDING", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "PENDING"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_status_blank_default", "workbench", "Set status blank default", "POST",
     "/workbench/status.do", {"claimId": "124", "status": ""}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_status_close_repeat", "workbench", "Repeated close retains the first closure record", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "CLOSED"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench", "setup": [["POST", "/workbench/status.do",{"claimId": "124", "status": "CLOSED"}]]}),
    ("workbench_status_reopen", "workbench", "Reopen removes the closure record", "POST",
     "/workbench/status.do", {"claimId": "124", "status": "OPEN"}, {"claim.124.status": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_reserve_positive", "workbench", "Reserve positive", "POST",
     "/workbench/reserve.do", {"claimId": "124", "reserveAmount": "2750.25"}, {"claim.124.reserve_amount": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_reserve_blank", "workbench", "Reserve blank", "POST",
     "/workbench/reserve.do", {"claimId": "124", "reserveAmount": ""}, {"claim.124.reserve_amount": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_reserve_nonnumeric", "workbench", "Reserve nonnumeric", "POST",
     "/workbench/reserve.do", {"claimId": "124", "reserveAmount": "ordinary"}, {"claim.124.reserve_amount": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_reserve_negative", "workbench", "Reserve negative", "POST",
     "/workbench/reserve.do", {"claimId": "124", "reserveAmount": "-25.00"}, {"claim.124.reserve_amount": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_reserve_zero", "workbench", "Reserve zero", "POST",
     "/workbench/reserve.do", {"claimId": "124", "reserveAmount": "0"}, {"claim.124.reserve_amount": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_note_text", "workbench", "Note text without persistence", "POST",
     "/workbench/note.do", {"claimId": "124", "noteText": "Routine claim review"}, {"claim.124.status": "/workbench/view.do?claimId=124", "claim.124.reserve_amount": "/workbench/view.do?claimId=124", "claim.124.assigned_adjuster": "/workbench/view.do?claimId=124", "claim.124.loss_date": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_note_blank", "workbench", "Note blank without persistence", "POST",
     "/workbench/note.do", {"claimId": "124", "noteText": ""}, {"claim.124.status": "/workbench/view.do?claimId=124", "claim.124.reserve_amount": "/workbench/view.do?claimId=124", "claim.124.assigned_adjuster": "/workbench/view.do?claimId=124", "claim.124.loss_date": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_note_missing", "workbench", "Note missing without persistence", "POST",
     "/workbench/note.do", {"claimId": "124"}, {"claim.124.status": "/workbench/view.do?claimId=124", "claim.124.reserve_amount": "/workbench/view.do?claimId=124", "claim.124.assigned_adjuster": "/workbench/view.do?claimId=124", "claim.124.loss_date": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_notes_changed", "workbench", "notes on changed claim", "GET",
     "/workbench/notes.do?claimId=124", {}, {"claim.124.status": "/workbench/view.do?claimId=124", "claim.124.reserve_amount": "/workbench/view.do?claimId=124", "claim.124.assigned_adjuster": "/workbench/view.do?claimId=124", "claim.124.loss_date": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_notes_untouched", "workbench", "notes on untouched claim", "GET",
     "/workbench/notes.do?claimId=1", {}, {"claim.1.status": "/workbench/view.do?claimId=1", "claim.1.reserve_amount": "/workbench/view.do?claimId=1", "claim.1.assigned_adjuster": "/workbench/view.do?claimId=1", "claim.1.loss_date": "/workbench/view.do?claimId=1"},
     {"area": "workbench"}),
    ("workbench_reserveHistory_changed", "workbench", "reserveHistory on changed claim", "GET",
     "/workbench/reserveHistory.do?claimId=124", {}, {"claim.124.status": "/workbench/view.do?claimId=124", "claim.124.reserve_amount": "/workbench/view.do?claimId=124", "claim.124.assigned_adjuster": "/workbench/view.do?claimId=124", "claim.124.loss_date": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_reserveHistory_untouched", "workbench", "reserveHistory on untouched claim", "GET",
     "/workbench/reserveHistory.do?claimId=1", {}, {"claim.1.status": "/workbench/view.do?claimId=1", "claim.1.reserve_amount": "/workbench/view.do?claimId=1", "claim.1.assigned_adjuster": "/workbench/view.do?claimId=1", "claim.1.loss_date": "/workbench/view.do?claimId=1"},
     {"area": "workbench"}),
    ("workbench_statusHistory_changed", "workbench", "statusHistory on changed claim", "GET",
     "/workbench/statusHistory.do?claimId=124", {}, {"claim.124.status": "/workbench/view.do?claimId=124", "claim.124.reserve_amount": "/workbench/view.do?claimId=124", "claim.124.assigned_adjuster": "/workbench/view.do?claimId=124", "claim.124.loss_date": "/workbench/view.do?claimId=124"},
     {"area": "workbench"}),
    ("workbench_statusHistory_untouched", "workbench", "statusHistory on untouched claim", "GET",
     "/workbench/statusHistory.do?claimId=1", {}, {"claim.1.status": "/workbench/view.do?claimId=1", "claim.1.reserve_amount": "/workbench/view.do?claimId=1", "claim.1.assigned_adjuster": "/workbench/view.do?claimId=1", "claim.1.loss_date": "/workbench/view.do?claimId=1"},
     {"area": "workbench"}),
]

ACTOR_PASSWORDS = {"supervisor": "supervisor", "adjuster4": "legacy4"}


def area(scenario):
    name = scenario[0]
    if name == "login" or name.startswith("session_"):
        return "session"
    prefix = name.split("_", 1)[0]
    return {"payment": "payments", "report": "reports",
            "fnol": "intake"}.get(prefix, prefix)


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
        return (error.code, error.headers.get("Location", error.geturl()),
                error.read().decode("utf-8"))


def fields(body):
    values = {}
    for name, value in FIELD_RE.findall(body):
        value = re.sub(r"\s+", " ", value).strip()
        if value != "":
            values[name] = value
    return dict(sorted(values.items()))


def result(body, code, location=None):
    if code in (301, 302, 303, 307, 308) and location:
        location = re.sub(r";jsessionid=[^?]+", "", location)
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


def start_server():
    maven_args = os.environ.get("MAVEN_CAPTURE_ARGS", "").split()
    log_path = os.path.join(ROOT, "target", "capture-jetty.log")
    with open(log_path, "w", encoding="utf-8") as log:
        process = subprocess.Popen(["mvn"] + maven_args + [
            "-f", capture_pom(), "-q", "jetty:run"], cwd=ROOT,
            stdout=log, stderr=subprocess.STDOUT)
    deadline = time.time() + 180
    while time.time() < deadline:
        if process.poll() is not None:
            raise RuntimeError("Jetty exited; see " + log_path)
        try:
            code, unused, body = request(urllib.request.build_opener(), "GET",
                                          "/login.do")
            if code == 200 and "Claims Login" in body:
                return process
        except Exception:
            pass
        time.sleep(1)
    process.terminate()
    process.wait(timeout=15)
    raise RuntimeError("Jetty did not become ready; see " + log_path)


def options(scenario):
    return scenario[7] if len(scenario) > 7 else {}


def anonymous():
    jar = http.cookiejar.CookieJar()
    return urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(jar), NoRedirect())


def login(actor):
    opener = anonymous()
    request(opener, "GET", "/login.do")
    request(opener, "POST", "/login.do",
            {"username": actor, "password": ACTOR_PASSWORDS[actor]})
    return opener


def scenario_opener(scenario, shared, actors):
    extra = options(scenario)
    session = extra.get("session", "shared")
    if session == "anonymous":
        return anonymous()
    actor = extra.get("actor", "supervisor")
    if session == "fresh":
        return login(actor)
    if session != "shared":
        raise ValueError("Unknown session mode: " + session)
    if "actor" not in extra:
        return shared
    if actor not in actors:
        actors[actor] = login(actor)
    return actors[actor]


def write_transcript(opener, scenario):
    name, module, description, method, path, form, probes = scenario[:7]
    extra = options(scenario)
    setup = extra.get("setup", [])
    for setup_method, setup_path, setup_form in setup:
        request(opener, setup_method, setup_path, setup_form)
    code, location, body = request(opener, method, path, form)
    state = {}
    for key, probe_path in sorted(probes.items()):
        state[key] = probe(opener, key, probe_path)
    transcript = {
        "scenario": name,
        "description": description,
        "actor": ("anonymous" if extra.get("session") == "anonymous"
                  else extra.get("actor", "supervisor")),
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
        }
    }
    if "area" in extra:
        transcript["area"] = extra["area"]
    if setup:
        transcript["setup"] = [{
            "method": setup_method,
            "path": CONTEXT_PATH + setup_path,
            "form": setup_form
        } for setup_method, setup_path, setup_form in setup]
    return transcript


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--no-reset", action="store_true")
    args = parser.parse_args()
    process = None
    try:
        if not args.no_reset:
            reset_database()
        tables, classes = prepare_snapshots()
        process = start_server()
        jar = http.cookiejar.CookieJar()
        opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(jar))
        request(opener, "GET", "/login.do")
        actors = {}
        for scenario in SCENARIOS:
            transcript = write_transcript(
                scenario_opener(scenario, opener, actors), scenario)
            write_snapshot(transcript["scenario"], tables, classes)
            path = os.path.join(ROOT, "transcripts",
                                transcript["scenario"] + ".json")
            with open(path, "w", encoding="utf-8") as stream:
                json.dump(transcript, stream, indent=2, sort_keys=True)
                stream.write("\n")
        index = [{
            "scenario": scenario[0],
            "module": scenario[1],
            "area": area(scenario),
            "description": scenario[2]
        } for scenario in SCENARIOS]
        with open(os.path.join(ROOT, "transcripts", "index.json"), "w",
                  encoding="utf-8") as stream:
            json.dump(index, stream, indent=2, sort_keys=True)
            stream.write("\n")
    finally:
        if process is not None:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    main()
