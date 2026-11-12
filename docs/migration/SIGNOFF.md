# Business owner sign-off list

**Claims business owner:** _not named yet_. The ticket "Compile the business owner sign-off list" asks who it is; record the name and each decision here.

Inputs, all on `claims-boot-integration` @ `84ae794`:

- Integration cross-check: [CROSSCHECK.md](CROSSCHECK.md). Legacy 217/0 and claims-boot 217/0 against `parity-v1`, manifest 440 files, 0 mismatches.
- Side-by-side replay (ticket "Replay parity side by side"): 217 scenarios plus the seed, 0 diffs on both apps; characterization suites legacy 92, Boot 90 tests, 0 failures. [Diff report](https://partner-workshops.devinenterprise.com/attachments/904167da-a937-4f51-9a1d-ae3db67f09c9/PARITY-DIFF-REPORT.md), [recording](https://partner-workshops.devinenterprise.com/sessions/447d06e43d53470887b7a2d638a35da3?testRecording=ab343f67-061c-4c2b-a867-a6cce625aa48).
- Click-through (ticket "Click through old and new screens"): 50 screen pairs, identical visible text, 36 pixel-identical; the other 14 differ only in browser visited-link colour, a caret, or item L1 below. [REPORT.md](https://partner-workshops.devinenterprise.com/attachments/bbbda4c2-6394-444b-8f22-ae2c699c2455/REPORT.md), [screenshots zip](https://partner-workshops.devinenterprise.com/attachments/509babe2-ca5b-4d80-a8f3-231623c5cef0/northstar-visual-evidence.zip), [recording](https://partner-workshops.devinenterprise.com/sessions/ab1eefab36de4cef81c999bed980723c?testRecording=15fd11c6-20f4-4c3b-8e7b-bffb89cc6d7b).
- Suspected legacy defects: [SUSPECTED-DEFECTS.md](SUSPECTED-DEFECTS.md) (SD-01..SD-17 settlement, R1..R16 reports) and the broken-route table in [ROUTES.md](ROUTES.md).

## How the list was built

Every difference between the apps was checked against the code. Only one remains (L1). Its cause is known, but the code cannot say which behaviour is right, so it is listed here. No code-explained regression was found, so nothing was sent back to an area PR.

Every other entry is a **legacy behaviour that claims-boot reproduces on purpose** (legacy result = new result). The port must keep them until parity is signed off. The question for each one is whether to keep it, or fix it in a follow-up change after cutover. Fixing it changes the frozen `parity-v1` baseline.

Decision values: **Keep** (carry as is), **Fix after cutover** (follow-up ticket), **Fix before cutover** (blocks sign-off).

## 1. Difference between the apps

| # | Screen / scenario | Legacy | claims-boot | Evidence | Question | Decision |
|---|---|---|---|---|---|---|
| L1 | Rejected login, `/login.do` with `supervisor` / `wrong` (screen pair 02) | Password field refilled with the rejected password (Struts `<html:password>` redisplays by default, `src/main/webapp/WEB-INF/jsp/login.jsp`) | Password field empty (`claims-boot/src/main/resources/templates/login.html`, `value=""`), PR #15 | [legacy](https://partner-workshops.devinenterprise.com/attachments/36fdc8d1-dcd3-4b2d-b68d-f6372e507bed/02-login-rejected-legacy.png) / [boot](https://partner-workshops.devinenterprise.com/attachments/606048a6-2bcb-44e2-a950-ace50e899b0f/02-login-rejected-boot.png); not covered by replay (passwords are not compared) | Accept the cleared field? Legacy writes the password back into the page HTML. Restoring that is a one-line change in PR #15. | |

## 2. Workflow problems found in the click-through (same in both apps)

| # | Screen / scenario | Legacy and claims-boot result | Evidence | Question | Decision |
|---|---|---|---|---|---|
| W1 | FNOL `/intake/new.do`, Submit button (pair 13) | The form posts to relative `intake/submit.do` → `/claims/intake/intake/submit.do` → Claims System Error. Nobody can file an FNOL from the browser; only direct POSTs work. | ROUTES.md `/intake/new.do`; pair 13 in zip; `intake/new.jsp` and `templates/intake/new.html` line `action="intake/submit.do"` | Fix the form action before cutover (users cannot report a loss) or keep it? | |
| W2 | FNOL validation (pair 14, scenarios `fnol_*`, `fnol_all_missing`) | Validation errors are set (`errors.claimant.required`, ...) but nothing is shown on the redisplayed form. | pair 14 | Show the field errors? | |
| W3 | FNOL confirm `/intake/confirm.do?claimId=121` (pair 16) | Claim Status blank, while the submit response shows OPEN. | pair 16 | Keep? | |
| W4 | Workbench assign / reserve / status / note (pairs 19–22, `workbench_*`) | After each action the detail screen shows only the changed field; the other fields are blank. | pairs 19–22 | Keep? | |
| W5 | Workbench Add note → Note history (pairs 22–23, `workbench_note_Parity_review`) | The note is echoed back but never saved. | pairs 22–23 | Persist notes? | |
| W6 | Reserve and status history (pairs 24–25) | Empty after reserve and status changes; no history rows are written. | pairs 24–25 | Write history rows? | |
| W7 | Settlement calculate/save and payment issue entry points | No working native entry form; reached only by direct URL. | REPORT.md "Coverage limits" | Expected to be reached from another system, or add entry forms? | |
| W8 | Admin `/admin/adjusters.do`, `/admin/reference.do`, `/admin/editAdjuster.do` (pairs 46–48) | HTTP 404 / 404 / 500 error page; admin is unusable. | ROUTES.md broken-route table; pairs 46–48 | Keep admin broken, repair it, or drop it? | |

## 3. Settlement (SUSPECTED-DEFECTS.md, same in both apps)

Pinned by `src/test/resources/characterization/settlement-cases.csv` (legacy) and `SettlementCharacterizationTest` (claims-boot, PR #19); case ids as listed.

| # | Scenario (cases) | Legacy and claims-boot result | Question | Decision |
|---|---|---|---|---|
| SD-01 | Half-cent amounts (`C17`,`C19`,`C21`,`A11`,`A12`) | Banker's rounding: 1.005 → 1.00, 1.025 → 1.02 | Round half up instead? | |
| SD-02 | Negative policy limit (`C16`) | Settlement −50.00, capped | Reject or zero? | |
| SD-03 | Negative deductible (`C29`,`A06`) | Adds to payout (3000 covered → 3500.00) | Reject negative deductibles? | |
| SD-04 | Negative depreciation (`C30`) | Adds to payout (1000 → 1200.00) | Reject? | |
| SD-05 | Cap flag before rounding (`C26`) | 1000.004 vs limit 1000 → 1000.00, capped true | Compare after rounding? | |
| SD-06 | Blank deductible on screen (`A02`,`A03`; replay `settlement_blank_deductible`) | Applies 0, not the policy's 5000 deductible | Default to the policy deductible? | |
| SD-07 | Malformed deductible on screen (`A04`,`A05`) | `abc` / `1,000.00` bind silently as 0 | Validation error? | |
| SD-08 | Missing/malformed covered amount (`A14`,`A15`,`A16`,`A18`) | Defaults to 5000.00 and can be saved | Reject? | |
| SD-09 | Malformed depreciation (`A21`) | Treated as 0 | Reject? | |
| SD-10 | Missing/malformed claim id (`A22`,`A23`) | Falls back to claim 119 | Reject? | |
| SD-11 | NaN / Infinity amounts (`C34`,`C36`,`C37`,`A19`,`A20`) | Unhandled `NumberFormatException` | Validation error? | |
| SD-12 | Unknown claim 99999 (`A24`,`A46`; `settlement_unknown_save`) | Calculates with a 10000 fallback limit; save fails with a DB error | Reject unknown claims? | |
| SD-13 | No authority / no session user (`A36`,`A37`; `authority_unknown_zero`) | 0.00 settlement saved, `calculated_by` may be the text `null` | Require authority and login for every save? | |
| SD-14 | Double submit (`A39`; `settlement_session_duplicate`) | Two settlement rows | Add a duplicate guard? | |
| SD-15 | Session save without a usable pending settlement (`A41`,`A42`,`A45`; `settlement_session_consumed`, `_mismatch`) | Recalculates from the 5000.00 default | Reject? | |
| SD-16 | Service save (`S02`) | Id = claimId+10000, so only one service settlement per claim; operator `supervisor` and date 2019-03-01 hard-coded | Keep? | |
| SD-17 | Deductible larger than loss (`C07`) | `deductibleApplied` records 2000 although only 900 was absorbed | Record the absorbed amount? | |

## 4. Reports (SUSPECTED-DEFECTS.md, same in both apps)

Pinned by `src/test/resources/characterization/reports/{baseline,edge}/` (legacy) and `ReportBaselineGoldenTest` / `ReportEdgeGoldenTest` (claims-boot, PR #20).

| # | Screen | Legacy and claims-boot result | Question | Decision |
|---|---|---|---|---|
| R1 | Loss ratio / reconciliation premium | Premium counted once per claim × payment row: AUTO 90,700 shown vs 30,700 actual (11 policies); `EDGE_LINE` 11,000 for one 1,000 policy | Count premium once per policy? Changes every loss ratio. | |
| R2 | Loss ratio, zero premium | Ratio shown as 0 even with losses (`ZERO_PREMIUM` loss 2900) | Show n/a or infinite? | |
| R3 | Loss ratio population and format | Includes CLOSED and DENIED claims; ratio printed as money (0.45) | Keep? Percent? | |
| R4 | Reconciliation | Reserve, Paid and Variance columns always blank | Populate them? | |
| R5 | Adjuster workload | "Oldest report" column always blank | Populate it? | |
| R6 | Aged claims | Reserve truncated to integer (22187.5 → 22187) | Show cents? | |
| R7 | Aged / open-by-adjuster / aging detail | Three different "open" definitions (APPROVED aged; OPEN+INVESTIGATING; OPEN only) | Pick one definition? | |
| R8 | Aged claims buckets | Claims reported after the as-of date get a negative age in `0_30`; boundaries inclusive (30 → `0_30`) | Keep? | |
| R9 | Claim aging detail | INVESTIGATING claims left out; no age or bucket shown | Include them? | |
| R10 | Closed claims vs month-end inventory | Inventory leaves out claims closed on the as-of date (23 vs 21 rows) | Which cut-off is right? | |
| R11 | Claim inventory DAO | SQL built from `orderBy` (injection, unused); tie order undefined; the "claim inventory" screen shows closures | Remove or fix? | |
| R12 | `/report/adjusterClaims.do?adjuster=` | Parameter concatenated into SQL (injection) | Fix before cutover? (security) | |
| R13 | Report as-of date | Five reports use a hard-coded 2019-04-01; others read `report.asof.date` | Use the property everywhere? | |
| R14 | Report index | `reportCount=7` while 14 links are listed (4 duplicates); header "Reports" goes to open-by-adjuster | Clean up? | |
| R15 | Money formatting | Depends on the JVM locale (de-DE prints `1001,00`) | Pin en-US? | |
| R16 | Counts | Returned as `Double`, truncated in the view | Keep? | |

## Area PRs

All open against `demo-insurance`, not merged, CI (`verify`, `claims-boot`) green:
[#15 foundation](https://github.com/Cognition-Partner-Workshops/ts-java-struts-claims-management/pull/15),
[#16 policy](https://github.com/Cognition-Partner-Workshops/ts-java-struts-claims-management/pull/16),
[#17 intake/workbench](https://github.com/Cognition-Partner-Workshops/ts-java-struts-claims-management/pull/17),
[#18 payments](https://github.com/Cognition-Partner-Workshops/ts-java-struts-claims-management/pull/18),
[#19 settlement](https://github.com/Cognition-Partner-Workshops/ts-java-struts-claims-management/pull/19),
[#20 reports](https://github.com/Cognition-Partner-Workshops/ts-java-struts-claims-management/pull/20).
