# Month-end report replay contract

Run `JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn -B verify`.
`ReportCharacterizationTest` exercises the actual Struts actions with a minimal
request proxy, then their DAO results through `ReportFormatter`. It reseeds the
same test database path used by existing integration tests (the singleton pool
retains the first URL), uses US formatting and UTC, and restores global settings.
No Jetty, login, or runtime database is needed. The fixed as-of date is asserted.

The UTF-8 TSV files were captured by running the legacy code against schema.sql
and seed.sql. Verification never rewrites expectations. One file is one request;
adjuster-claims has five cases using only the five real seeded usernames.

Record types:

* `forward` and `attribute`: selected action forward and all scalar request data.
* `columns`: ordered `property:type` schema for the following `row` records.
* `raw`: complete DAO map, keys sorted, immediately preceding its formatted row.
* `row`: all report columns, in original DAO order; empty cells are retained.
* `rows`: exact result count (index counts its 14 report links, duplicates included).
* `total`: runner-computed sums for monetary and count columns, not extra UI rows.
* `link`: index JSP report destinations in source order; the legacy reportCount
  attribute remains 7 even though the page has 14 links including duplicates.
* `# suspect`: known legacy discrepancy, still compared exactly like other cases.

Closed claims includes 23 closures through April 1; inventory includes 21 posted
closures, excluding CLM-00110 and CLM-00120 whose April 1 closures post the next
day. Both files mark this mismatch `suspect`; neither query is corrected.

This is a data/formatter contract, not an HTML or visual snapshot. JSP tags,
localized headings, layout, and authentication are outside the contract. Money
is normalized through ReportFormatter to two decimals even where a legacy JSP
uses an integer tag (aged reserves). Dates use yyyy-MM-dd. Missing DAO keys stay
missing in raw data: workload oldestReportedDate formats blank; reconciliation
reserveTotal, paidTotal, and variance format as 0.00. These are not repaired.
Raw lossRatio and lossTotal remain pinned even on reconciliation, whose JSP
does not display them. Formatter fixtures separately pin normal, null, empty,
and malformed numeric inputs, including the legacy zero fallback.

To replay against replacement reports, map its ordered results to the `columns`
schema and compare the same TSV rows, counts, and totals. Keep `suspect` values
until a separately approved behavior change updates the contract.
