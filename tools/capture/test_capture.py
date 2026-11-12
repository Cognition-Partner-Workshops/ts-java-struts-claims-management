import unittest

from capture import decode_rows, schema_tables


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
