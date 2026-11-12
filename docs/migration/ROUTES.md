# NorthStar Claims: route map

Every `.do` route the legacy Struts 1.3 app serves on `demo-insurance`, with
the code it runs, the data it touches, how it behaves today, and the
migration area that owns it. This is the coverage list for the parity set,
and it sets the scope of each area worker.

Sources: `WEB-INF/struts-config.xml` (default module, 40 mappings),
`WEB-INF/struts-config-claims.xml` (also loaded into the default module, no
mappings), `WEB-INF/struts-config-admin.xml` (`/admin` module, 2 mappings),
the JSPs under `WEB-INF/jsp/` and the actions in `com.northstar.claims.web`.
That makes **42 action mappings**. Each one appears below exactly once and
has an owning area.

"Observed" means a request made while logged in as `supervisor` against a
freshly seeded database (`make seed`, then `make run`, on JDK 11), unless the
row says otherwise. The view is the `<!-- ns:view ... -->` marker in the
response.

## Areas

| Area | Owns | Routes |
|------|------|-------:|
| shared foundation | login, home, logout, and the `/admin` module ported as it behaves today | 5 |
| policy | policy search, view, list, coverages, insured party, renewal | 7 |
| intake | FNOL intake **and** the claim workbench (assign, status, reserve, notes, history) | 12 |
| settlement | settlement calculate, save, detail, adjuster approval authority | 3 |
| payments | payment issue, history, detail, remittance | 4 |
| reports | report index and the ten reports | 11 |
| **Total** | | **42** |

Decision on record: the workbench goes with intake, since it is the rest of the
claim lifecycle after FNOL. `/admin` goes in the shared foundation and is
ported as it behaves today, which means broken (see below).

## Cross-cutting behavior

- **Auth:** `AuthFilter` is mapped to `*.do`. A URI ending in `login.do` is
  public. Every other `.do` request needs the session attribute `user`, and
  without it the response is a 302 to `/claims/login.do`. Observed:
  `GET /workbench/list.do` while logged out renders `login.jsp`.
- **Credentials are hard-coded in `LoginAction`, not read from `ADJUSTER`.**
  `supervisor`/`supervisor` works, and so does any username starting with
  `adjuster` paired with any password starting with `legacy`. The `ADJUSTER`
  table is only read later, for settlement approval limits.
- **Default parameters:** a missing or unparsable id falls back to a fixed
  value. `claimId` falls back to 119, `paymentId` to 61, `policyId` to 9001
  (or to 1 on `/policy/view` and `/policy/coverages`). Parity tests must keep
  these fallbacks.
- **Errors:** `global-exceptions` (any `java.lang.Exception`) and the
  `web.xml` error pages (404 and `java.lang.Exception`) all render
  `/WEB-INF/jsp/error.jsp`. An exception thrown in an action returns
  **HTTP 200** with `error.jsp` and "Reference status: null". A container
  404 or 500 keeps its status code.
- **Validation errors** come back as `<!-- ns:error key -->` markers from the
  request attribute `nsValidationErrors`. The Struts validator plug-in is
  loaded, but every mapping has `validate="false"`.
- **Read/write:** R means the route only reads. W means it changes the
  database, and the table it writes is named. All writes use raw JDBC
  through `ClaimsActionSupport.openConnection()`/`update()`, except
  `SettlementDAO.save` and `PaymentDAO.insert`. The workbench writes build
  their SQL by string concatenation (an injection risk; keep the same
  observable behavior and fix the mechanism).
- **GET and POST are treated the same.** Every write route also accepts GET.

## Route table

Paths are relative to the context root `/claims`. "Action" is the class in
`com.northstar.claims.web`. "-" means none.

### shared foundation (5)

| Route | Action | Form | JSP (forwards) | DAOs / tables | R/W | Observed today |
|-------|--------|------|----------------|---------------|-----|----------------|
| `/login.do` | `LoginAction` | `loginForm` | `login.jsp`, `home.jsp` | - (hard-coded credentials) | R (session) | Correct credentials render `home.jsp` with `signedInUser`. Wrong credentials render `login.jsp` with `loginStatus=REJECTED`. Public. |
| `/home.do` | `HomeAction` | - | `home.jsp` | - | R | 200 `home.jsp`. Its links are relative (`policy/list.do`, `workbench/list.do`, `report/index.do`), so they only work from root-level URLs. |
| `/logout.do` | `LogoutAction` | - | global `login` -> `login.jsp` | - | W (session invalidated) | 200 `login.jsp`. A later `/home.do` redirects to login. |
| `/admin/adjusters.do` | `admin.AdjusterAdminAction` | `adjusterForm` (**not declared in the admin module**) | `adjusters`/`editAdjuster` -> `/WEB-INF/jsp/admin/*.jsp` | `AdjusterDAO.findAll` / ADJUSTER | R | **Broken: HTTP 404, `error.jsp`.** The log shows `No FormBeanConfig found in module /admin under name adjusterForm`. The action runs. The 404 matches Struts 1.3's default `forwardPattern` (`$M$P`), which prefixes the module to the forward and gives `/admin/WEB-INF/jsp/...`, where nothing exists. That cause is inferred from Struts behavior; only the 404 itself was observed. Hypothesis confirmed. The same happens when logged in as `adjuster4`. |
| `/admin/reference.do` | `admin.ReferenceDataAction` | - | `reference` -> `/WEB-INF/jsp/admin/reference.jsp` | - (literal loss types, version `2019.04`) | R | **Broken: HTTP 404, `error.jsp`.** Same module-relative forward problem. |

Not mapped, but linked from `admin/adjusters.jsp`: `/admin/editAdjuster.do`
gives **HTTP 500** (`InvalidPathException: No action config found`), shown
in `error.jsp`.

### policy (7)

| Route | Action | Form | JSP | DAOs / tables | R/W | Observed today |
|-------|--------|------|-----|---------------|-----|----------------|
| `/policy/search.do` | `PolicySearchAction` | `policySearchForm` | `policy/search.jsp` | `PolicyDAO.findByLine` / POLICY (SQL concatenated) | R | `lineOfBusiness` defaults to `AUTO`. Returns 11 `policyLimit_*` rows (9001 = 1000.00, the others 25000.00). `HOME` returns 0 rows. A DAO failure is swallowed and gives an empty list. |
| `/policy/view.do` | `PolicyViewAction` | `policyViewForm` | `policy/view.jsp` | `PolicyDAO.findById` / POLICY | R | `policyId=9001` renders 3 fields. Default id is 1. Links to coverages, coverageDetail, insuredParty, renewal. |
| `/policy/list.do` | `PolicyListAction` | - | `policy/list.jsp` | `PolicyDAO.findAll` / POLICY | R | 84 fields (all policies). A DAO failure is swallowed. |
| `/policy/coverages.do` | `PolicyCoveragesAction` | - | `policy/coverages.jsp` | `PolicyDAO.findById` / POLICY | R | The coverage list is always empty (COVERAGE is never queried). The page shows `coverageLimit`/`coverageDeductible` from the policy (9001 -> 1000.00 / 100.00). Default id is 1. |
| `/policy/coverageDetail.do` | `PolicyCoverageDetailAction` | - | `policy/coverageDetail.jsp` | `PolicyDAO.findById` / POLICY | R | Same as above. The coverage list is empty, and policy 9001 shows a 1000.00 limit and a 100.00 deductible. |
| `/policy/insuredParty.do` | `PolicyInsuredPartyAction` | - | `policy/insuredParty.jsp` | `PolicyDAO.findById` / POLICY (result overwritten) | R | Returns **placeholder literals**, not data: `POL-<id>`, "Policy insured party", effective 2018-01-01, expiry 2019-12-31, ACTIVE. INSURED_PARTY is never read. |
| `/policy/renewal.do` | `PolicyRenewalAction` | - | `policy/renewal.jsp` | `PolicyDAO.findById` / POLICY | R | 5 fields. An unknown id falls back to 9001. |

### intake: FNOL and workbench (12)

| Route | Action | Form | JSP | DAOs / tables | R/W | Observed today |
|-------|--------|------|-----|---------------|-----|----------------|
| `/intake/new.do` | `IntakeNewAction` | `intakeForm` | `intake/new.jsp` | - | R | Renders the form. **The form posts to the relative `intake/submit.do`, which resolves to `/claims/intake/intake/submit.do` and gives `error.jsp` (HTTP 200).** So submitting from the browser is broken today. Direct POSTs (as in the capture tool) work. |
| `/intake/submit.do` | `IntakeSubmitAction` | `intakeForm` (input `intake/new.jsp`) | `intake/new.jsp`, `intake/confirm.jsp` | raw JDBC / CLAIM insert (`max(claim_id)+1`, policy 9001, WATER, `adjuster1`) | **W** CLAIM | With valid input, renders `confirm.jsp` with `claimId=121, claimStatus=OPEN` on fresh seed. With blank claimant, blank description and a bad date, renders `new.jsp` with `errors.claimant.required`, `errors.description.required`, `errors.lossdate.format`. Dates are parsed leniently (`02/30/2019` becomes `2019-03-02`). The supplied loss type is ignored. |
| `/intake/confirm.do` | `IntakeConfirmAction` | - | `intake/confirm.jsp` | - | R | Echoes `claimId` from the request. `claimStatus` is blank and nothing is looked up. |
| `/workbench/list.do` | `WorkbenchListAction` | `workbenchForm` | `workbench/list.jsp` | `ClaimDAO.findByStatus("OPEN")` / CLAIM | R | 48 fields (the OPEN claims). A DAO failure is swallowed. |
| `/workbench/view.do` | `WorkbenchViewAction` | `workbenchForm` | `workbench/view.jsp` | `ClaimDAO.findById` / CLAIM | R | Claim 119: lossDate 2019-01-15, DENIED, reserve 1500.00, adjuster4. |
| `/workbench/assign.do` | `WorkbenchAssignAction` | - | `workbench/view.jsp` | raw SQL / CLAIM.assigned_adjuster | **W** CLAIM | Reads the `adjuster` parameter (default `adjuster2`). Note that capture sends `assignedAdjuster`, which is ignored. The response shows **only** `assignedAdjuster`. The other view fields are blank. |
| `/workbench/status.do` | `WorkbenchStatusAction` | `statusDyn` | `workbench/view.jsp` | raw SQL / CLAIM.status; CLAIM_CLOSURE insert (CLOSED) or delete (any other status) | **W** CLAIM, CLAIM_CLOSURE | `status` defaults to INVESTIGATING. The response shows only `claimStatus`. CLOSED adds a CLAIM_CLOSURE row dated `report.asof.date`, which shows up in `/report/closedClaims` (25 -> 26 fields). |
| `/workbench/reserve.do` | `WorkbenchReserveAction` | `amountDyn` | `workbench/view.jsp` | raw SQL / CLAIM.reserve_amount | **W** CLAIM | `reserveAmount` defaults to 4500. The response shows only `reserveAmount`. **No RESERVE_HISTORY row is written.** |
| `/workbench/note.do` | `WorkbenchNoteAction` | - | `workbench/view.jsp` | `ClaimDAO.findById` / CLAIM | R (despite the name) | Echoes `noteText` (default "Review completed"). **The note is never saved.** `/workbench/notes` stays unchanged afterwards. |
| `/workbench/notes.do` | `WorkbenchNoteHistoryAction` | - | `workbench/notes.jsp` | `NoteDAO.findByClaim`, `ClaimDAO.findById` / CLAIM_NOTE, CLAIM | R | Note list plus a claim summary. An unknown claim falls back to 119. |
| `/workbench/reserveHistory.do` | `WorkbenchReserveHistoryAction` | - | `workbench/reserveHistory.jsp` | `ClaimDAO.findById` / CLAIM | R | The history list is always empty (RESERVE_HISTORY is never read). |
| `/workbench/statusHistory.do` | `WorkbenchStatusHistoryAction` | - | `workbench/statusHistory.jsp` | `ClaimDAO.findById` / CLAIM | R | The history list is always empty. Shows only the current status. |

Orphan JSPs (no mapping forwards to them): `workbench/assignment.jsp` (posts
to `assign.do`) and `workbench/note.jsp`. `/workbench/assignment.do` is not
mapped and renders `error.jsp`.

### settlement (3)

`SettlementCalculateAction` and `SettlementSaveAction` extend the abstract
`SettlementAuthorityAction`. It looks up the operator's `ADJUSTER.approval_limit`
(no row means a limit of 0), and an amount above the limit renders
`settlement/refused.jsp` with `errors.settlement.authority`. The amount is
covered minus deductible minus depreciation, capped at the claim's
`POLICY.policy_limit` (10000 if the claim or policy is missing).

| Route | Action | Form | JSP | DAOs / tables | R/W | Observed today |
|-------|--------|------|-----|---------------|-----|----------------|
| `/settlement/calculate.do` | `SettlementCalculateAction` | `settlementForm` | `settlement/calculate.jsp`, `settlement/refused.jsp` | `ClaimDAO`, `PolicyDAO`, `AdjusterDAO` / CLAIM, POLICY, ADJUSTER | R (stores `pendingSettlement` in the session) | Claim 119 (policy 9001, limit 1000), 5000/500/0: `settlementAmount=1000.00`, `cappedAtLimit=true`. On a bare GET the deductible is 0 (form default). `adjuster4` on claim 118, 5000/1000/0: `refused.jsp`, `refusedAmount=4000.00`, `approvalLimit=2500.00`. |
| `/settlement/save.do` | `SettlementSaveAction` | `settlementForm` | `settlement/save.jsp`, `settlement/refused.jsp` | `SettlementDAO.save` (+ the authority lookups) / SETTLEMENT insert | **W** SETTLEMENT | With no `coveredAmount` and the same `claimId`, it reuses the session's `pendingSettlement`. `GET ?claimId=119` after calculate gives `settlementAmount=1000.00, savedBy=supervisor`. The date is hard-coded to 2019-04-01. Above the limit it gives `refused.jsp` and nothing is written. |
| `/settlement/detail.do` | `SettlementDetailAction` | - | `settlement/detail.jsp` | `SettlementDAO.findByClaim` / SETTLEMENT | R | Latest settlement for the claim. Seeded claim 119: covered 1500.00, deductible 500.00, depreciation 100.00, amount 0.00, 2019-03-01. |

### payments (4)

| Route | Action | Form | JSP | DAOs / tables | R/W | Observed today |
|-------|--------|------|-----|---------------|-----|----------------|
| `/payment/issue.do` | `PaymentIssueAction` | `paymentForm` | `payment/issue.jsp` | `SettlementDAO.findByClaim`, `PaymentDAO.insert` / SETTLEMENT, PAYMENT | **W** PAYMENT | POST claim 119, payee, 1000.00, CHECK gives `CHK-61`, ISSUED (payment id `max+1` = 61 on fresh seed). **Without `payeeName`/`paymentMethod` it fails a NOT NULL constraint and renders `error.jsp` with HTTP 200.** The amount defaults to the settlement amount (0.00 for claim 120). The issued date is hard-coded to 2019-04-03. |
| `/payment/history.do` | `PaymentHistoryAction` | - | `payment/history.jsp` | `PaymentDAO.findByClaim` / PAYMENT | R | `claimId` defaults to 119. Empty on fresh seed, and shows `CHK-61` after an issue. The page hard-codes a link to `detail.do?paymentId=61`. |
| `/payment/detail.do` | `PaymentDetailAction` | - | `payment/detail.jsp` | `PaymentDAO.findById` / PAYMENT | R | **On fresh seed, payment 61 does not exist, so the page renders `error.jsp` (HTTP 200).** After an issue it renders the 6 detail fields. |
| `/payment/remittance.do` | `PaymentRemittanceAction` | - | `payment/remittance.jsp` | `PaymentDAO.findByClaim`, `totalIssued` / PAYMENT | R | Claim 119: count 0, total 0.00 on fresh seed. Count 1, total 1000.00 after one issue. |

### reports (11)

All of them are read-only. The as-of date is `report.asof.date` from
`northstar.properties`, or the literal 2019-04-01 from `reportDate()`.

| Route | Action | Form | JSP | DAOs / tables | R/W | Observed today |
|-------|--------|------|-----|---------------|-----|----------------|
| `/report/index.do` | `ReportIndexAction` | - | `report/index.jsp` | - | R | Static links to the reports. |
| `/report/openByAdjuster.do` | `OpenReportAction` | - | `report/openByAdjuster.jsp` | `ReportDAO.openClaimsByAdjuster` / CLAIM (OPEN, INVESTIGATING) | R | 4 fields. This is the header and nav "Reports" link. |
| `/report/lossRatio.do` | `LossRatioReportAction` | - | `report/lossRatio.jsp` | `ReportDAO.lossRatioByLine` / POLICY, CLAIM | R | 12 fields. |
| `/report/agedClaims.do` | `AgedClaimsReportAction` | - | `report/agedClaims.jsp` | `ReportDAO.agedClaims` / CLAIM (not CLOSED or DENIED) | R | 8 fields (age buckets). |
| `/report/adjusterWorkload.do` | `AdjusterWorkloadDetailAction` | - | `report/adjusterWorkload.jsp` | `ReportDAO.openClaimsByAdjuster` / CLAIM | R | 8 fields. |
| `/report/claimAgingDetail.do` | `ClaimAgingDetailAction` | - | `report/claimAgingDetail.jsp` | `ClaimDAO.findByStatus("OPEN")` / CLAIM | R | 74 fields. |
| `/report/reconciliation.do` | `ReconciliationAction` | - | `report/reconciliation.jsp` | `ReportDAO.lossRatioByLine` / POLICY, CLAIM | R | 18 fields. Same query as lossRatio. |
| `/report/premiumDetail.do` | `PremiumDetailAction` | - | `report/premiumDetail.jsp` | `PolicyDAO.findAll` / POLICY | R | 128 fields. |
| `/report/adjusterClaims.do` | `AdjusterClaimsReportAction` | - | `report/adjusterClaims.jsp` | `ReportDAO.claimsForAdjuster` / CLAIM (SQL concatenated) | R | `?adjuster=adjuster1`: 50 fields. |
| `/report/closedClaims.do` | `ClosedClaimsReportAction` | - | `report/closedClaims.jsp` | `ReportDAO.closedClaimsThrough` / CLAIM_CLOSURE, CLAIM | R | `closed_date <= as-of`. 25 fields on fresh seed. Includes closures made on the as-of date. |
| `/report/claimInventory.do` | `ClaimInventoryReportAction` | - | `report/claimInventory.jsp` | `ReportDAO.closuresPostedThrough` / CLAIM_CLOSURE, CLAIM | R | `closed_date + 1 day <= as-of`. 23 fields. Closures made on the as-of date are left out, unlike closedClaims. |

## Known-broken routes and flows today

| What | Behavior | Where |
|------|----------|-------|
| `/admin/adjusters.do`, `/admin/reference.do` | HTTP 404, `error.jsp` (module-relative forwards; `adjusterForm` not declared in the admin module) | `struts-config-admin.xml` |
| `/admin/editAdjuster.do` (linked, not mapped) | HTTP 500, `error.jsp` | `admin/adjusters.jsp` |
| FNOL form submit from the browser | Posts to `/claims/intake/intake/submit.do` and gets `error.jsp` | `intake/new.jsp` relative `action` |
| `/payment/detail.do` (default 61) on fresh seed | `error.jsp`, HTTP 200 (no payment 61 yet) | `PaymentDetailAction`, `history.jsp` hard-coded link |
| `/payment/issue.do` without payee or method | `error.jsp`, HTTP 200 (NOT NULL violation) | `PaymentIssueAction` |
| `/workbench/note.do` | Note is not saved | `WorkbenchNoteAction` |
| `/workbench/reserveHistory.do`, `/workbench/statusHistory.do`, `/policy/coverages.do`, `/policy/coverageDetail.do` | Always an empty list | the respective actions |
| `/policy/insuredParty.do` | Placeholder literals instead of INSURED_PARTY data | `PolicyInsuredPartyAction` |
| `/workbench/assignment.do` (orphan JSP) | `error.jsp` | `workbench/assignment.jsp` |

Whether each of these is ported as observed or fixed is up to the area
owner and must be recorded in the parity sign-off. The gate for this map is
only that every mapping is listed and owned.

## Gate check

The following prints each struts-config mapping that is missing from this
file. It must print nothing:

```bash
python3 - <<'PY'
import re
doc = open("docs/migration/ROUTES.md").read()
for cfg, prefix in [("struts-config.xml", ""), ("struts-config-admin.xml", "/admin")]:
    for path in re.findall(r'path="(/[^"]+)"\s+type=', open("src/main/webapp/WEB-INF/" + cfg).read()):
        if "| `%s%s.do` |" % (prefix, path) not in doc:
            print("MISSING", prefix + path)
PY
```
