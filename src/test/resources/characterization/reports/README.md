# Report characterization goldens

These files pin what the legacy month-end reports produce today.

- `ReportDaoCharacterizationTest` covers the rows and totals returned by `ReportDAO`, plus the `ClaimDAO` and `PolicyDAO` calls the report actions make.
- `ReportScreenCharacterizationTest` covers each report request, run through the real Struts actions and JSPs on an embedded Jetty. It records the HTTP status, the forwarded view, the `ns:field` values and the cells of the results and detail tables.

Both tests run under `mvn verify`.

| Directory | Database state |
|-----------|----------------|
| `baseline/` | The fixed seed (`src/main/resources/db/seed.sql`). |
| `edge/` | The fixed seed plus the boundary rows in `ReportFixtures.EDGE_ROWS`, listed below. |

The boundary rows in `ReportFixtures.EDGE_ROWS` are:
- claims closed the day before, on, and the day after the as-of date;
- two zero-premium lines, one with a loss and one without;
- open claims aged -1, 0, 30, 31, 60, 61, 90 and 91 days.

The as-of date is `report.asof.date` from `northstar.properties` (2019-04-01).

Files are canonical JSON: sorted keys, LF line endings, and numbers in plain
decimal form. Before rendering, the tests set `Locale.US` and the UTC time
zone. The comparison ignores CRLF, and `.gitattributes` keeps the files on LF.
The same goldens therefore verify a Windows build.

To regenerate the goldens after an intentional behavior change, run:

    mvn -B test -Dtest='Report*CharacterizationTest' -Dcharacterization.update=true

When the output differs from a golden, the test writes the actual output to
`target/characterization-actual/`.

Anything that looks wrong is listed in `docs/migration/SUSPECTED-DEFECTS.md`.
