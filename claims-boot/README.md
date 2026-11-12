# Claims Boot foundation

Standalone Spring Boot 3.4 (Boot-managed Thymeleaf 3.1) Maven application,
requiring **JDK 21**. It is not a module of the root Maven project and does
not share legacy sources or dependencies. The legacy JDK 11 build remains
`JAVA_HOME=/path/to/jdk11 mvn -B verify` at the repository root and serves
port **8080**, context `/claims`.

From the repository root (including when launching the jar):

```sh
JAVA_HOME=/path/to/jdk21 mvn -B -f claims-boot/pom.xml verify
/path/to/jdk21/bin/java -jar claims-boot/target/claims-boot-0.1.0-SNAPSHOT.jar
curl -si http://localhost:8081/claims/login.do
```

Boot serves port **8081**, context **/claims**. There is no suffix pattern
matching: controllers explicitly map `.do` paths and keep legacy query/form
parameter names rather than introduce REST paths or rename fields.

## Run both apps side by side

Both apps use context `/claims` and separate HSQLDB files, so they can run at
the same time from the repository root in two terminals:

```sh
# Terminal 1: legacy Struts app on http://localhost:8080/claims/login.do (JDK 11)
JAVA_HOME=/path/to/jdk11 make build seed run

# Terminal 2: claims-boot on http://localhost:8081/claims/login.do (JDK 21),
# with its loopback HSQL server on :9002 (alias northstar) for replay
JAVA_HOME=/path/to/jdk21 mvn -B -f claims-boot/pom.xml verify
/path/to/jdk21/bin/java -jar claims-boot/target/claims-boot-0.1.0-SNAPSHOT.jar \
  --claims.db.server.enabled=true
```

| App | HTTP | Database file | Replay JDBC URL |
| --- | --- | --- | --- |
| Legacy | 8080 | `target/db/northstar` | started by `make replay` on 9001 |
| claims-boot | 8081 | `claims-boot/target/db/claims-boot` | `jdbc:hsqldb:hsql://localhost:9002/northstar` |

Log in to either app as `supervisor`/`supervisor`. `make build` wipes
`target/db`, so seed before `make run`. `make replay` starts its own legacy
Jetty on 8080, so stop the side-by-side legacy instance before running it.

## Replay one area's slice of the parity set

Restart claims-boot (it reseeds on boot), then replay against it:

```sh
python3 tools/capture/capture.py replay \
  --base-url http://localhost:8081/claims \
  --db-url jdbc:hsqldb:hsql://localhost:9002/northstar --db-user SA \
  --area policy
```

`--area` is one of `policy`, `intake` (FNOL and workbench), `settlement`,
`payments`, `reports` or `shared foundation`. Replay runs every scenario in
order (claim ids and session state depend on it) but compares only the chosen
area plus the seed, and exits nonzero on any diff. While other areas are
unported, judge the slice on its response diffs; DB snapshots after the first
write from another area differ only by missing rows (`-` lines). See the
[runbook](../docs/migration/RUNBOOK.md) for the frozen `parity-v1` baseline.

## Area ownership

Each area PR branches from `claims-boot-foundation`, targets `demo-insurance`,
and adds files only inside its own package and template folders (Java under
`src/main/java/com/northstar/claims/boot/`, tests under the same package in
`src/test/java`, templates under `src/main/resources/templates/`). Template
folders mirror the legacy JSP folders and URL prefixes.

| Area (`--area`) | Routes | Package | Templates |
| --- | --- | --- | --- |
| Policy (`policy`) | `/policy/*` | `boot.policy` | `policy/` |
| Intake and workbench (`intake`) | `/intake/*`, `/workbench/*` | `boot.intake` | `intake/`, `workbench/` |
| Settlement (`settlement`) | `/settlement/*` | `boot.settlement` | `settlement/` |
| Payments (`payments`) | `/payment/*` | `boot.payment` | `payment/` |
| Reports (`reports`) | `/report/*` | `boot.report` | `report/` |

The foundation owns everything else: the `boot` package itself (security,
errors, data layer, helpers, `NsMarkup`, login/home/admin), `fragments/`,
`login.html`, `home.html`, `error.html`, `application.properties` and the
pom. Area PRs reuse these without editing them; DAOs and models an area needs
live in its own package (duplicate rather than share across areas). If a
shared change is unavoidable, make it a separate PR against this branch.

## Shared layout, login and markers

- Messages: `ApplicationResources.properties` is packaged unchanged from
  `../src/main/resources` and is the Spring `MessageSource` (`#{key}`).
  `northstar.css` is served from `../src/main/webapp` as a static resource.
- Templates: `fragments/header.html`, `nav.html` and `footer.html` port the
  JSP includes; `fragments/layout.html :: page(view, content)` assembles them
  for operator screens (see `home.html`). `login.html` and `error.html` are
  standalone, like their JSPs.
- Markers: the `ns` bean (`NsMarkup`) replaces `ns:field`, `ns:view`,
  `ns:message` and the `<!-- ns:error key -->` comments. Use the
  `fragments/markers.html` fragments (`view(path)`, `field(name, value, type)`,
  `errors(keys)`), which evaluate the bean in `th:with` and then emit it with
  `th:utext`, because Thymeleaf 3.1 rejects bean calls inside `th:utext`.
- Security (`SecurityConfig`, replacing `AuthFilter`): every `*.do` except
  `login.do` needs a signed-in session, otherwise it gets a 302 to the absolute
  `/claims/login.do`. `LegacyAuthenticationProvider` keeps the legacy users:
  `supervisor`/`supervisor` (`ROLE_SUPERVISOR`) and any `adjuster*` with a
  `legacy*` password (`ROLE_ADJUSTER`). `LoginController` ports `LoginAction`
  (GET or POST, success renders home at `/login.do`, failure re-renders login,
  session attributes `user`/`displayName`) and `LogoutAction` (invalidate,
  render login). CSRF is off because the legacy forms and replay post without
  tokens. Intentional differences: the session id changes on login, and a
  rejected login no longer echoes the password into the page.
- Errors (`LegacyErrorHandling`): unknown `.do` routes and action exceptions
  render `error.html` with status 200 and `Reference status: null` (the Struts
  unknown/global-exception forwards); anything under `/admin/` fails with 500;
  other missing paths are a 404 error page.
- `/admin` is ported as it behaves today (ROUTES.md): `adjusters.do` (which
  still runs its ADJUSTER query) and `reference.do` answer 404 and the linked
  `editAdjuster.do` answers 500.

## Legacy route contract (future ports)

Paths below are relative to `/claims`; all **42** mapped actions from
[`ROUTES.md`](../docs/migration/ROUTES.md) are listed. Only the shared routes
are ported; the other areas still resolve to the legacy unknown-action page.

| Area | Routes |
| --- | --- |
| Shared (5) | `/login.do`, `/home.do`, `/logout.do`, `/admin/adjusters.do`, `/admin/reference.do` |
| Policy (7) | `/policy/search.do`, `/policy/view.do`, `/policy/list.do`, `/policy/coverages.do`, `/policy/coverageDetail.do`, `/policy/insuredParty.do`, `/policy/renewal.do` |
| Intake/workbench (12) | `/intake/new.do`, `/intake/submit.do`, `/intake/confirm.do`, `/workbench/list.do`, `/workbench/view.do`, `/workbench/assign.do`, `/workbench/status.do`, `/workbench/reserve.do`, `/workbench/note.do`, `/workbench/notes.do`, `/workbench/reserveHistory.do`, `/workbench/statusHistory.do` |
| Settlement (3) | `/settlement/calculate.do`, `/settlement/save.do`, `/settlement/detail.do` |
| Payments (4) | `/payment/issue.do`, `/payment/history.do`, `/payment/detail.do`, `/payment/remittance.do` |
| Reports (11) | `/report/index.do`, `/report/openByAdjuster.do`, `/report/lossRatio.do`, `/report/agedClaims.do`, `/report/adjusterWorkload.do`, `/report/claimAgingDetail.do`, `/report/reconciliation.do`, `/report/premiumDetail.do`, `/report/adjusterClaims.do`, `/report/closedClaims.do`, `/report/claimInventory.do` |

Keep legacy names such as `username`, `password`, `lineOfBusiness`,
`policyId`, `claimId`, `paymentId`, `claimantName`, `lossDate`, `description`,
`lossType`, `adjuster`, `assignedAdjuster`, `status`, `reserveAmount`,
`noteText`, `coveredAmount`, `deductible`, `depreciation`, `payeeName`,
`amount` and `paymentMethod`. The route map records quirks (for example,
assignment reads `adjuster`, not capture's `assignedAdjuster`) and error
behavior; preserving those is later parity work, not this scaffold.

[`tools/capture/capture.py`](../tools/capture/capture.py) defines the ordered
parity scenarios. See the [migration runbook](../docs/migration/RUNBOOK.md)
and [coverage notes](../docs/migration/CAPTURE-COVERAGE.md) for replay with
`--base-url http://localhost:8081/claims` and a separately seeded database.
Replay of `--area "shared foundation"` matches legacy responses; full parity
replay needs the remaining areas.

## Data layer

Inject `JdbcTemplate` or `NamedParameterJdbcTemplate` into Spring DAO beans
and keep legacy SQL verbatim; no DAOs have been ported yet. HikariCP connects
as the legacy `db.user`/`db.password` with autocommit **true**.
A `DataSourceTransactionManager` is available: only add `@Transactional`
where the legacy DAO explicitly managed transactions, not as a blanket policy.
`ClaimsConfig` exposes `get(key)` (missing keys return `""`) and typed
`reportAsOfDate()`. `DateHelper` and `LegacyFormatHelper` preserve legacy
lenient dates, rounding, null behavior, and JVM default locale/timezone.

Maven packages the legacy `db/schema.sql`, `db/seed.sql`, and
`northstar.properties` directly from `../src/main/resources`; edit only
those sources. Builds require the whole repository checkout.

| Property | Default |
| --- | --- |
| `claims.db.url` | `jdbc:hsqldb:file:claims-boot/target/db/claims-boot` (relative to working directory) |
| `claims.db.reset` | `true`: recreate schema and seed on every startup |
| `claims.db.server.enabled` | `false` |
| `claims.db.server.port` | `9002` (legacy replay uses 9001) |

Pass overrides as `--claims.db.reset=false`, etc. Keep mode preserves an
existing PUBLIC schema without reapplying non-idempotent seed inserts; an
empty database is initialized. The reset path uses the legacy line-based
script splitter, SA connection, schema then seed, application-user
create/alter, commit and shutdown before opening the pool.
Tests use `target/test-db/`, never the development database.

Enable the loopback-only in-process HSQL server for replay with
`--claims.db.server.enabled=true`. It shares the same file database and
PUBLIC schema with the pool, at `jdbc:hsqldb:hsql://localhost:9002/northstar`.
Do not start a second JVM HSQL server on this file.
