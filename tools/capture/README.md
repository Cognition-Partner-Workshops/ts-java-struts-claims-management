# Database snapshots

Capture compiles `DatabaseSnapshot.java` against the local HSQLDB 2.7.3 jar.
After each scenario's request and HTTP probes it reads every schema table,
ordered by the primary key parsed from `schema.sql`, using JDBC strings and
SQL nulls. Side files use sorted keys, two-space indentation and a final newline.

Only clock- or random/token-derived database values should be masked as
`<masked>`, never arbitrary dates or generated numeric IDs. On this branch
there are **no such persisted values in the captured workflows**: FNOL sets
reported_date and created_date from the supplied loss date, settlements use
literal 2019 dates, payment issuance uses 2019-04-03 and CHK- plus the payment
ID, and claim numbers are derived from numeric IDs (including
ClaimNumberService). DateHelper.today has no callers; ScreenAudit's clock
values are not persisted in these tables. Consequently these snapshots retain
all those deterministic values, including seeded 2019 dates; no placeholder
is needed. Revisit this provenance rule if database write paths change.

The harness derives an ignored `tools/capture/target/pom.xml` from the root
POM and passes it to Maven with `-f`. It changes only capture's build paths
and Jetty context configuration, leaving the root POM and plain `make run`
alone. The context starts a loopback HSQLDB server on port 9137 in Jetty's JVM
and marks `org.hsqldb.` as system classes so app file connections share its
database. The JNDI resource retains the production datasource shape (including
its lookup behavior), but points to the server URL. Snapshot readers connect
as SA over that server, never opening the locked database file themselves.

Run helper tests with `python3 -m unittest discover -s tools/capture -p 'test_*.py'`.

## Scenario metadata and isolation

Append scenarios only. The original 28 transcript payloads remain unchanged;
their areas are supplied in `index.json`. New scenarios include `area` in their
options and transcript. Keep the legacy `module` values in the index.

The default session is shared. Use `session: anonymous` for a fresh cookie jar,
or `session: fresh` to log into a new jar using `actor` (default supervisor).
These isolated sessions do not follow redirects, so authentication redirects
are recorded rather than hidden by the login page. Setup steps use the same
session as the scenario; logout tests must use an isolated session.

Run `python3 tools/capture/coverage.py` after capture to regenerate
`docs/parity/COVERAGE.md`. It reads module prefixes from `web.xml` and action
mappings with an XML parser, separates explicit requests from setup steps,
lists unmapped requests, and exits 1 until every action has a scenario.

Policy search only uses `lineOfBusiness`; the other submitted criteria are
ignored. Renewal is a read-only review, with no form or validation rules.
Blank, invalid and absent policy identifiers record its fallback behavior,
and a POST with blank renewal fields records that no update occurs.
