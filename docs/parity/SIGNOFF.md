# Claims-owner behaviour sign-off

One row per behaviour where nobody can yet say which answer is right. Area
workers and the parity phase append to this list. Nobody changes behaviour on
a row without the claims owner's verdict. Likely intended answers below are
proposals, not decisions; an empty Owner verdict means unresolved. Preserve the
legacy expectations until a verdict explicitly authorizes a separate change.

Fixture paths below are relative to `src/test/resources/characterization/`.
Settlement evidence uses `SettlementCharacterizationTest` and the CSV row ID;
reports evidence uses `ReportCharacterizationTest` and TSV record/line references.
Manifest evidence uses `demos/app/complexity-manifest.json` class names and the
named tests/transcripts. MAN rows retain root causes independently of their
related SET/RPT symptoms.

## Settlement

| ID | Area | Behaviour | Evidence (scenario/test + fixture row ref) | Legacy answer | Likely intended answer | Owner verdict | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| SET-01 | Settlement | Deductible exceeds remaining loss | `settlement.csv` C05 | Payout 0.00; deductibleApplied 2000.00 | Report only 900.00 actually applied | | deductibleApplied reports the full 2000 although only 900 of loss remained after depreciation |
| SET-02 | Settlement | Negative policy limit | `settlement.csv` C12 | Payout -50.00, capped | Reject invalid limit or floor payout at zero | | negative policy limit produces a negative settlement because the zero floor runs before the cap |
| SET-03 | Settlement | Negative deductible | `settlement.csv` C13 | Payout 1200.00; deductible -200.00 | Reject negative deductible | | negative deductible is accepted and increases the payout above the covered amount |
| SET-04 | Settlement | Negative depreciation | `settlement.csv` C14 | Payout 1300.00 | Reject negative depreciation | | negative depreciation is accepted and increases the payout above the covered amount |
| SET-05 | Settlement | Non-numeric service deductible | `settlement.csv` C15 | NumberFormatException | Consistent validation error across layers | | service path throws on a non-numeric deductible while the screen path binds it as 0 (see S03); MAN-01, SET-15 |
| SET-06 | Settlement | Grouped-thousands service deductible | `settlement.csv` C16 | NumberFormatException | Consistently accept 1000 or explicitly reject format | | grouped thousands are rejected by the service path but bind as 0 on the screen path (see S04); MAN-01, SET-16 |
| SET-07 | Settlement | Scientific notation deductible | `settlement.csv` C18 | 1e2 becomes 100.00; payout 900.00 | Require explicit supported money format | | scientific notation is accepted as a deductible |
| SET-08 | Settlement | NaN service deductible | `settlement.csv` C19 | NumberFormatException | Validation error before calculation | | NaN parses as a double and then fails inside BigDecimal.valueOf instead of a validation message |
| SET-09 | Settlement | Half-cent deductible result | `settlement.csv` C20 | Payout 500.00; deductibleApplied 500.005 | HALF_UP would pay 500.01; confirm rounding rule | | half cent created by the deductible rounds down to the even cent (HALF_UP would give 500.01); MAN-03 |
| SET-10 | Settlement | Half-cent payout 1.005 | `settlement.csv` H00 | 1.00 | HALF_UP would pay 1.01; confirm rounding rule | | HALF_EVEN rounds the half cent down to 1.00 (HALF_UP would give 1.01); MAN-03 |
| SET-11 | Settlement | Half-cent payout 1.025 | `settlement.csv` H02 | 1.02 | HALF_UP would pay 1.03; confirm rounding rule | | HALF_EVEN rounds the half cent down to 1.02 (HALF_UP would give 1.03); MAN-03 |
| SET-12 | Settlement | Half-cent payout 1.045 | `settlement.csv` H04 | 1.04 | HALF_UP would pay 1.05; confirm rounding rule | | HALF_EVEN rounds the half cent down to 1.04 (HALF_UP would give 1.05); MAN-03 |
| SET-13 | Settlement | Half-cent payout 1.065 | `settlement.csv` H06 | 1.06 | HALF_UP would pay 1.07; confirm rounding rule | | HALF_EVEN rounds the half cent down to 1.06 (HALF_UP would give 1.07); MAN-03 |
| SET-14 | Settlement | Half-cent payout 1.085 | `settlement.csv` H08 | 1.08 | HALF_UP would pay 1.09; confirm rounding rule | | HALF_EVEN rounds the half cent down to 1.08 (HALF_UP would give 1.09); MAN-03 |
| SET-15 | Settlement | Non-numeric screen deductible | `settlement.csv` S03 | Deductible 0.00; payout 10000.00 | Validation error | | non-numeric deductible silently binds as 0 instead of a validation error; MAN-01, SET-05 |
| SET-16 | Settlement | Grouped-thousands screen deductible | `settlement.csv` S04 | Deductible 0.00; payout 10000.00 | Consistently accept 1000 or explicitly reject format | | grouped thousands deductible silently binds as 0; MAN-01, SET-06 |
| SET-17 | Settlement | Non-numeric covered amount | `settlement.csv` S05 | Payout 5000.00 | Validation error, no invented amount | | non-numeric covered amount falls back to an invented 5000 |
| SET-18 | Settlement | Missing covered amount | `settlement.csv` S06 | Payout 5000.00 | Require covered amount | | missing covered amount falls back to an invented 5000 |
| SET-19 | Settlement | Missing claim ID | `settlement.csv` S07 | Claim 119; limit and payout 1000.00 | Require explicit valid claim | | missing claimId silently defaults to claim 119 and its 1000 policy limit |
| SET-20 | Settlement | Unknown claim | `settlement.csv` S08 | Limit and payout 10000.00 | Claim-not-found error | | unknown claim falls back to a 10000 policy limit instead of an error |
| SET-21 | Settlement | Policy deductible versus form | `settlement.csv` S09 | Payout 8000.00; deductible 0.00 | Apply policy deductible 5000 unless an override is authorized | | the policy's own 5000 deductible is ignored; only the form deductible is applied; MAN-01 |
| SET-22 | Settlement | Non-numeric depreciation | `settlement.csv` S10 | Depreciation defaults to 0; payout 500.00 | Validation error | | non-numeric depreciation silently falls back to 0 |
| SET-23 | Settlement | Negative covered amount | `settlement.csv` S11 | Payout 0.00 | Reject negative covered amount | | negative covered amount is accepted and floored to 0 |
| SET-24 | Settlement | NaN screen covered amount | `settlement.csv` S12 | NumberFormatException | Validation error instead of HTTP 500 | | NaN covered amount parses and then fails inside BigDecimal.valueOf (HTTP 500 on the screen) |
| SET-25 | Settlement | NaN screen deductible | `settlement.csv` S13 | NumberFormatException | Validation error instead of HTTP 500 | | NaN deductible binds and then fails inside BigDecimal.valueOf (HTTP 500 on the screen) |
| SET-26 | Settlement | Authority check after rounding | `settlement.csv` A06 | 10000.005 rounds to 10000.00 and is approved | Decide whether authority applies before or after rounding | | a total half a cent above the 10000 limit is approved because the check uses the rounded amount; MAN-03, MAN-05 |
| SET-27 | Settlement | No session user and zero payout | `settlement.csv` A11 | Zero payout approved at zero authority limit | Require an identified authorized operator even for zero | | a session with no user passes the authority gate when the settlement is 0 (limit 0 and the check is strictly greater-than); MAN-05 |
| SET-28 | Settlement | Service authority and audit metadata | `settlement.csv` V01 | Saves 300000.00; id 10001; operator supervisor; date 2019-03-01 | Enforce authority in service and use actual operator/date | | SettlementService.calculateAndSave applies no authority limit and stamps a hardcoded operator and date; MAN-05 |
| SET-29 | Settlement | Repeated service save identifier | `settlement.csv` V02 | id 10002 for claim 2; payout 9400.00 | Allocate unique IDs or define explicit idempotent updates | | settlement id is claimId + 10000 so a second save for the same claim would collide; second save is inferred, not exercised by this row |

## Reports

| ID | Area | Behaviour | Evidence (scenario/test + fixture row ref) | Legacy answer | Likely intended answer | Owner verdict | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RPT-01 | Reports | Inventory closure posting cutoff | `reports/claim-inventory.tsv` line 3 suspect; `attribute reportRowCount` and `rows` 21 | 21 posted closures as of 2019-04-01; CLM-00110 and CLM-00120 absent | Align cutoff with closed report or explicitly label next-day posting | | Legacy closed-claims count 23 versus inventory posted-closure count 21; preserved, not reconciled. Related RPT-02, MAN-04 |
| RPT-02 | Reports | Closed-claims closure-date cutoff | `reports/closed-claims.tsv` line 3 suspect; `rows` 23; `row` CLM-00110 and CLM-00120 | 23 closures through 2019-04-01, including both same-day closures | Agree one cutoff with inventory or document distinct metrics | | Legacy closed-claims count 23 versus inventory posted-closure count 21; preserved, not reconciled. Related RPT-01, MAN-04 |

## Cross-cutting/manifest

| ID | Area | Behaviour | Evidence (scenario/test + fixture row ref) | Legacy answer | Likely intended answer | Owner verdict | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MAN-01 | Cross-cutting/manifest | Blank-to-zero form coercion | Manifest `form bean coercion`; `IntakeFormBindingTest`, `SettlementFormBindingTest`; `transcripts/intake_blank_reserve.json`, `transcripts/settlement_blank_deductible.json`; `settlement.csv` S02, S03, S04, S09 | Primitive double binding turns blank/non-numeric reserve or deductible into zero; intake stores reserve 0.00 | Explicitly distinguish blank, invalid and intentional zero; owner decides whether blank is allowed | | Related SET-05, SET-06, SET-15, SET-16, SET-21; keep root cause separate from symptoms |
| MAN-02 | Cross-cutting/manifest | Pending settlement held in session | Manifest `session state`; `transcripts/settlement_session_save.json` calculate then save | Calculate stores pendingSettlement in HttpSession; save without coveredAmount persists it | Define explicit claim-bound pending state and stale/tab behaviour, or approve current session semantics | | Owner must decide the state contract before parity changes it |
| MAN-03 | Cross-cutting/manifest | HALF_EVEN rounding | Manifest `BigDecimal scale`; `SettlementCalculatorTest.halfCentRoundsToEvenCent`; `transcripts/settlement_half_cent.json`; `settlement.csv` C20, H00, H02, H04, H06, H08, A06 | BigDecimal.valueOf inputs; total setScale(2, ROUND_HALF_EVEN); 1.005 becomes 1.00 | HALF_UP gives 1.01; owner selects financial rounding policy | | Related SET-09 through SET-14 and SET-26 |
| MAN-04 | Cross-cutting/manifest | Close-date counting | Manifest `close-date counting`; `transcripts/report_closed_claims.json`, `transcripts/report_claim_inventory.json`; `reports/closed-claims.tsv` and `reports/claim-inventory.tsv` line 3 | Closed report counts closed_date; inventory counts closed_date + 1 day; 23 versus 21 | Agree common cutoff or explicitly distinct closure/posting definitions | | Related RPT-01, RPT-02; seeded April 1 closures CLM-00110 and CLM-00120 |
| MAN-05 | Cross-cutting/manifest | Dropped authority limit risk | Manifest `dropped authority limit`; `SettlementAuthorityActionTest`; `transcripts/settlement_authority_refused.json`, `transcripts/settlement_authority_within_limit.json`; `settlement.csv` A02, A06, A11, V01 | Only inherited SettlementAuthorityAction.execute enforces ADJUSTER.approval_limit; executeSettlement, service and DAO do not | Retain gate across entry points; owner decides scope and rounding basis | | Related SET-26, SET-27, SET-28; absence of lower-layer checks must not silently remove inherited enforcement |

## Known coverage boundary

The reports suite pins action/DAO and ReportFormatter output, not rendered JSP
HTML. Some report JSPs show money as whole numbers (for example aged reserves),
while these TSVs use two-decimal formatter output. The frozen parity set covers
rendered fields; use it for rendered-field parity rather than treating formatter
fixtures as HTML evidence.

## Out of scope: security

String-built SQL in the adjuster-claims report, the committed DB password in
`northstar.properties`, and `commons-collections` 3.2.1 are ported unchanged and
left to security's later pass. This also covers all security-category manifest
entries: `SQL injection`, `vulnerable dependency`, `secret in config`, and
`test-fixture finding` (hard-coded fixture password and unsalted MD5, test-only).
These are not owner-verdict table rows and are not repaired by this work.
