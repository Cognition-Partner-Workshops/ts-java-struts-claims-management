import contextlib
import io
import hashlib
import json
import os
import tempfile
import unittest
from unittest.mock import mock_open, patch

import capture


class ReplayTest(unittest.TestCase):
    def test_area_replays_full_stream_without_writes(self):
        scenarios = capture.SCENARIOS[:2]
        selected_area = capture.area(scenarios[0])
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(capture, "SCENARIOS", scenarios), \
                patch.object(capture, "area", side_effect=[selected_area, "other"]), \
                patch.object(capture, "check_routes"), \
                patch.object(capture, "DatabaseSnapshots"), \
                patch.object(capture, "request"), \
                patch.object(capture, "write_transcript", return_value={}) as execute, \
                patch.object(capture, "compare_file", return_value=0) as compare, \
                patch.object(capture, "write_snapshot") as write, \
                patch.object(capture, "start_server") as start, \
                patch.object(capture.sys, "argv", ["capture.py", "replay", "--area", selected_area]), \
                contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertFalse(capture.main())
        self.assertEqual(2, execute.call_count)
        self.assertEqual(3, compare.call_count)
        write.assert_not_called()
        start.assert_not_called()
        self.assertIn("Replay: 1 scenarios, 0 diffs", output.getvalue())

    def test_comparison(self):
        with tempfile.TemporaryDirectory() as directory:
            path = os.path.join(directory, "expected.json")
            with open(path, "w") as stream:
                json.dump({"status": 200}, stream)
            self.assertEqual(0, capture.compare_file(path, {"status": 200}, "login response"))
            with contextlib.redirect_stdout(io.StringIO()) as output:
                self.assertEqual(1, capture.compare_file(path, {"status": 500}, "login response"))
            self.assertIn("--- login response expected", output.getvalue())
            self.assertIn('+  "status": 500', output.getvalue())

    def test_empty_database_and_missing_expectation(self):
        with patch("builtins.open", mock_open(read_data='{}')):
            self.assertEqual(0, capture.compare_file("db.json", {}, "database"))
        with contextlib.redirect_stdout(io.StringIO()) as output:
            self.assertEqual(1, capture.compare_file("/nonexistent/expectation", {}, "database"))
        self.assertIn("cannot read expectation", output.getvalue())

    def test_manifest(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(capture, "ROOT", directory):
            data = b"{}"
            with open(os.path.join(directory, "frozen.json"), "wb") as stream:
                stream.write(data)
            manifest = os.path.join(directory, "manifest.json")
            with open(manifest, "w") as stream:
                json.dump({"files": {"frozen.json": hashlib.sha256(data).hexdigest()}}, stream)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, capture.verify_manifest(manifest))
                os.remove(os.path.join(directory, "frozen.json"))
                self.assertEqual(1, capture.verify_manifest(manifest))
            with open(manifest, "w") as stream:
                json.dump({"files": {}}, stream)
            with self.assertRaisesRegex(ValueError, "no frozen files"):
                capture.verify_manifest(manifest)


class CoverageTest(unittest.TestCase):
    def test_all_documented_routes(self):
        with contextlib.redirect_stdout(io.StringIO()) as output:
            capture.check_routes()
        self.assertIn("42/42 mapped routes", output.getvalue())

    def test_missing_route(self):
        with patch("builtins.open", mock_open(read_data="| `/missing.do` | Action |")):
            with self.assertRaisesRegex(RuntimeError, "/missing.do"):
                capture.check_routes()

    def test_empty_scenarios(self):
        with patch.object(capture, "SCENARIOS", []):
            with self.assertRaisesRegex(RuntimeError, "Uncovered routes"):
                capture.check_routes()

    def test_areas_and_names(self):
        names = [scenario[0] for scenario in capture.SCENARIOS]
        self.assertEqual(len(names), len(set(names)))
        for scenario in capture.SCENARIOS:
            self.assertEqual(capture.area(scenario), capture.options(scenario)["area"])

    def test_area_does_not_leak_into_transcript(self):
        scenario = capture.SCENARIOS[0]
        with patch.object(capture, "request", return_value=(200, "", "")), \
                patch.object(capture, "write_snapshot"):
            transcript = capture.write_transcript(None, scenario, None, persist=False)
        self.assertNotIn("area", transcript)
        self.assertEqual("login", transcript["scenario"])


if __name__ == "__main__":
    unittest.main()
