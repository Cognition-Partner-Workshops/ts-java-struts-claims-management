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

Intake/workbench captures append after the session/policy/admin set. Intake
creates claims 124–139, then the confirmation setup creates claim 140. Probes
read the created claim after each submit; rejected submits probe the next id
to show that it does not exist. Subsequent workbench mutations use claim 124;
claim 1 is the untouched history comparison. All earlier captures stay fixed.

The legacy intake mapping disables Validator validation: blank/missing loss
dates default rather than producing the configured required-field error.
The action enforces claimant/description requirements and date shape itself.
Submitted loss types are ignored in favor of WATER. Workbench has no paging
or filtering beyond its hard-coded OPEN selection. Assignment defaults blank
adjusters, status accepts arbitrary ordinary strings, reserve falls back to
4500 for blank/non-numeric input, and note requests only set a request attribute.
Reserve/status history screens use empty lists even after mutations; notes
remain unchanged. Full database snapshots expose these non-writes and closure
insertion/removal alongside HTTP probes of claim state.

New report scenarios opt into `capture_tables`, which adds
`expected.rendered_tables` containing normalized cell text from `results` and
`detail` tables. Existing `business_fields` still records every nonempty `f_`
span. This option never changes the old report recordings. Navigation and
screen-audit tables are excluded. All report JSPs have table content outside
`f_` spans: adjuster/bucket/line labels in openByAdjuster, agedClaims,
lossRatio, adjusterWorkload, and reconciliation; claim numbers in
adjusterClaims, closedClaims, and claimInventory; policy numbers/lines in
premiumDetail; status/adjuster in claimAgingDetail. The report index has no
`f_` spans at all, so its report-link table is captured as rendered text.

Settlement/payment mappings have no enabled field validation. Blank and
non-numeric numeric inputs record their existing coercion/default behavior,
and missing payee/method or settlement records the legacy database/system
error. Payment methods have no enumeration or application restriction; the
captures exercise CHECK, EFT, ACH, WIRE, and blank values. Remittance takes a
claim id, not a payment id; an explicit scenario records ignored paymentId.
The only report request parameter is adjusterClaims' `adjuster`; other report
dates are configured, not supplied by the request. CI's unfiltered push event
includes `devin/parity-set` without a workflow change.
