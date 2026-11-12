# Claims Boot foundation

Standalone Spring Boot 3.4 (Boot-managed Thymeleaf 3.1) Maven application,
requiring **JDK 21**. It is not a module of the root Maven project and does
not share legacy sources or dependencies. The legacy JDK 11 build remains
`JAVA_HOME=/path/to/jdk11 mvn -B verify` at the repository root and serves
port **8080**, context `/claims`.

From the repository root:

```sh
JAVA_HOME=/path/to/jdk21 mvn -B -f claims-boot/pom.xml verify
/path/to/jdk21/bin/java -jar claims-boot/target/claims-boot-0.1.0-SNAPSHOT.jar
curl -si http://localhost:8081/claims/login.do
```

Boot serves port **8081**, context **/claims**. Only `GET /login.do` is
implemented, as a Thymeleaf placeholder, with optional legacy `username`
parameter. It does not authenticate, process passwords, or implement POST
login yet. Tests exercise a real embedded server, the configured context,
explicit `.do` mapping, missing/empty parameters and unmapped paths.
There is no suffix pattern matching: future controllers must explicitly
map `.do` paths and preserve legacy query/form parameter names rather than
introduce REST paths or rename fields.

## Legacy route contract (future ports)

Paths below are relative to `/claims`; all **42** mapped actions from
[`ROUTES.md`](../docs/migration/ROUTES.md) are listed. Only the login GET
placeholder currently resolves in Boot; the other features are not ported.

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
Full parity replay is not expected to pass against this placeholder.
