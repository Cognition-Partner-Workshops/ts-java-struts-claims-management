import contextlib
import io
import unittest
from unittest.mock import mock_open, patch

import capture


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
