import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

from capture import (area, decode_rows, fields, report_tables, scenario_opener,
                     schema_tables, write_transcript)
from coverage import action_paths, coverage, parse_actions


class ReportTablesTest(unittest.TestCase):
    def test_rendered_cells_and_entities(self):
        body = ('<table class="page"><tr><td>Navigation</td></tr>'
                '<table class="results"><tr><th>Claim</th><th>Amount</th></tr>'
                '<tr><td><a href="view.do">CLM-1 &amp; review</a></td>'
                '<td><span id="f_amount"> 12.00 </span></td></tr></table>'
                '<table class="detail"><tr><td> Rows\n returned </td>'
                '<td>1</td></tr></table>'
                '<table class="screen-audit"><tr><td>Ignored</td></tr>'
                '</table></table>')
        self.assertEqual([
            {"class": "results", "rows": [["Claim", "Amount"],
                                         ["CLM-1 & review", "12.00"]]},
            {"class": "detail", "rows": [["Rows returned", "1"]]}],
            report_tables(body))
        self.assertEqual({"amount": "12.00"}, fields(body))

    def test_empty_and_error_responses(self):
        self.assertEqual([], report_tables(""))
        self.assertEqual([], report_tables("<h1>Claims System Error</h1>"))
        self.assertEqual([{"class": "results", "rows": []}],
                         report_tables('<table class="results"></table>'))

    @patch("capture.request", return_value=(200, "", '<table class="results">'
           '<tr><td><span id="f_count">2</span></td></tr></table>'))
    def test_opt_in_preserves_legacy_payload(self, request):
        scenario = ("report_example", "reporting", "Example", "GET",
                    "/report/example.do", {}, {})
        old = write_transcript(None, scenario)
        self.assertNotIn("rendered_tables", old["expected"])
        new = write_transcript(None, scenario + ({"capture_tables": True},))
        self.assertEqual(old["expected"]["business_fields"],
                         new["expected"]["business_fields"])
        self.assertEqual([{"class": "results", "rows": [["2"]]}],
                         new["expected"]["rendered_tables"])


class CoverageTest(unittest.TestCase):
    def test_multiline_actions_and_module_prefix(self):
        self.assertEqual({"/admin/adjusters.do"}, parse_actions(
            '<struts-config><action-mappings><action\n path="/adjusters"\n'
            ' type="Example"><forward path="/ignored"/></action>'
            '</action-mappings></struts-config>', "/admin"))

    def test_empty_and_invalid_config(self):
        self.assertEqual(set(), parse_actions("<struts-config/>"))
        with self.assertRaises(ET.ParseError):
            parse_actions("<struts-config>")
        with self.assertRaises(KeyError):
            parse_actions('<struts-config><action-mappings><action/>'
                          '</action-mappings></struts-config>')

    def test_deployed_configs(self):
        paths = action_paths()
        self.assertIn("/admin/reference.do", paths)
        self.assertIn("/policy/renewal.do", paths)
        self.assertNotIn("/admin/editAdjuster.do", paths)

    def test_request_setup_and_unmapped(self):
        hits, unmapped = coverage({"/home.do", "/logout.do", "/login.do"}, [{
            "scenario": "example", "request": {"path": "/claims/home.do?a=1"},
            "setup": [{"path": "/claims/logout.do"},
                      {"path": "/claims/admin/editAdjuster.do"}]}])
        self.assertEqual(["example"], hits["/home.do"]["request"])
        self.assertEqual(["example"], hits["/logout.do"]["setup"])
        self.assertEqual([], hits["/login.do"]["request"])
        self.assertEqual(["example"], unmapped["/admin/editAdjuster.do"]["setup"])


class SessionTest(unittest.TestCase):
    def scenario(self, extra):
        return ("example", "session", "Example", "GET", "/home.do", {}, {}, extra)

    @patch("capture.anonymous")
    @patch("capture.login")
    def test_isolated_sessions(self, login, anonymous):
        shared, actors = object(), {}
        self.assertIs(shared, scenario_opener(self.scenario({}), shared, actors))
        self.assertIs(anonymous.return_value, scenario_opener(
            self.scenario({"session": "anonymous"}), shared, actors))
        self.assertIs(login.return_value, scenario_opener(
            self.scenario({"session": "fresh"}), shared, actors))
        login.assert_called_once_with("supervisor")
        self.assertEqual({}, actors)

    @patch("capture.login")
    def test_cached_actor(self, login):
        actors = {}
        for unused in range(2):
            scenario_opener(self.scenario({"actor": "adjuster4"}), None, actors)
        login.assert_called_once_with("adjuster4")

    def test_invalid_session(self):
        with self.assertRaises(ValueError):
            scenario_opener(self.scenario({"session": "invalid"}), None, {})

    def test_legacy_areas(self):
        for name, expected in [("login", "session"), ("payment_issue", "payments"),
                               ("report_loss_ratio", "reports"),
                               ("fnol_submit", "intake"), ("policy_view", "policy")]:
            self.assertEqual(expected, area((name,)))


class SnapshotTest(unittest.TestCase):
    def test_schema_primary_keys(self):
        self.assertEqual({"EXAMPLE": "A,B"}, schema_tables(
            "-- CREATE TABLE ignored ();\nCREATE TABLE example ("
            "a INTEGER, b INTEGER, CONSTRAINT pk PRIMARY KEY (a, b));"))

    def test_invalid_schema(self):
        for schema in ["", "CREATE TABLE example (a INTEGER);"]:
            with self.assertRaises(ValueError):
                schema_tables(schema)

    def test_strings_null_and_empty(self):
        self.assertEqual([{"A": "x\ty\nz", "B": None, "C": ""}],
                         decode_rows("QQ==\tQg==\tQw==\neAl5Cno=\t-\t\n"))

    def test_empty_table(self):
        self.assertEqual([], decode_rows("QQ==\n"))

    def test_invalid_row(self):
        with self.assertRaises(ValueError):
            decode_rows("QQ==\tQg==\neA==\n")


if __name__ == "__main__":
    unittest.main()
