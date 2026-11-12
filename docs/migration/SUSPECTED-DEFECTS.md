# Suspected defects found during characterization

The characterization goldens pin each behavior below **as it is today**. Do
not fix any of these in the legacy app or "correct" them during the port.
Carry each one across as observed, and decide fixes separately once parity is
signed off. Each entry names the golden that pins it.

## Reports (`ReportDAO`, report actions and JSPs)

Goldens: `src/test/resources/characterization/reports/{baseline,edge}/{dao,screens}/`.
The as-of date is `report.asof.date=2019-04-01`.

| # | Area | Observed behavior | Pinned by |
|---|------|-------------------|-----------|
| R1 | Loss ratio / reconciliation | `lossRatioByLine` joins POLICY → CLAIM → PAYMENT and sums `annual_premium` (and `reserve_amount`) for every joined row. A policy's premium is counted once per claim × payment row, not once per policy. AUTO shows premium 90700, but the 11 AUTO policies sum to 30700. `EDGE_LINE` shows 11000 for one 1000 premium policy with 11 claims. | `*/dao/loss-ratio-by-line.json`, `*/screens/loss-ratio-by-line.json` |
| R2 | Loss ratio | A line with zero premium reports `lossRatio` 0, even when it has losses (`ZERO_PREMIUM`: loss 2900, ratio 0.0). An infinite or undefined ratio is shown as "no loss". | `edge/dao/loss-ratio-by-line.json` |
| R3 | Loss ratio | Loss is the reserve plus paid on claims in every status, including CLOSED and DENIED. The screen formats the ratio with the `money` type (two decimals, e.g. `0.45`), not as a percentage or a full-precision ratio. | `*/screens/loss-ratio-by-line.json` |
| R4 | Reconciliation | `reconciliation.jsp` reads `reserveTotal`, `paidTotal` and `variance`. `ReconciliationAction` supplies `lossRatioByLine()` rows, which have none of those keys, so the Reserve, Paid and Variance columns are always blank. Premium carries the R1 fan-out. | `*/screens/reconciliation.json` |
| R5 | Adjuster workload | `adjusterWorkload.jsp` reads `oldestReportedDate`, which `openClaimsByAdjuster()` never supplies, so the "Oldest report" column is always blank. | `*/screens/adjuster-workload.json` |
| R6 | Aged claims | `agedClaims.jsp` renders the bucket reserve with `type="integer"`, which truncates the cents (`22187.5` → `22187`, `20326.99` → `20326`). | `edge/screens/aged-claims.json` |
| R7 | Aged claims | The buckets use `status not in ('CLOSED','DENIED')`, so APPROVED claims are aged. Open by adjuster counts only OPEN/INVESTIGATING, and claim aging detail only OPEN. The three "open" populations disagree. | `*/dao/aged-claims.json`, `*/dao/open-by-adjuster.json`, `*/dao/claim-aging-detail.json` |
| R8 | Aged claims | A claim reported after the as-of date has a negative age and lands in `0_30` (CLM-09120, reported 2019-04-02). The boundaries are inclusive upper bounds: 30 → `0_30`, 31 → `31_60`, 60 → `31_60`, 61 → `61_90`, 90 → `61_90`, 91 → `91_PLUS`. | `edge/dao/aged-claims.json` |
| R9 | Claim aging detail | `ClaimAgingDetailAction` lists `ClaimDAO.findByStatus("OPEN")`. INVESTIGATING claims (e.g. CLM-09123) are left out, and the screen shows no age or bucket. | `*/screens/claim-aging-detail.json` |
| R10 | Closures | `closedClaimsThrough` includes closures dated on the as-of date. `closuresPostedThrough` (the "Month-end claim inventory" screen, `/report/claimInventory.do`) uses `closed_date + 1 day <= as-of`, so it leaves out claims closed on the as-of date (CLM-00110, CLM-00120, CLM-09111). The two month-end views disagree by those rows. | `*/dao/closed-claims.json`, `*/dao/closures-posted.json`, `*/screens/claim-inventory.json` |
| R11 | Claim inventory | `ReportDAO.claimCounts(orderBy)` concatenates `orderBy` into SQL (injection). No action calls it, and the screen named "claim inventory" shows closures instead (R10). With `order by count`, ties (every status has 24 claims on the fixed seed) come back in HSQLDB's internal order, which no SQL rule guarantees. | `*/dao/claim-inventory-*.json` |
| R12 | Adjuster claims | `claimsForAdjuster` concatenates the `adjuster` request parameter into SQL (injection via `/report/adjusterClaims.do?adjuster=`). An unknown adjuster returns an empty table with count 0. | `*/dao/adjuster-claims-*.json`, `*/screens/adjuster-claims-*.json` |
| R13 | As-of date | Workload, aging detail, premium detail, reconciliation and the index take `reportAsOf` from `ClaimsActionSupport.reportDate()`, which is hard-coded to `"2019-04-01"`. Every other report reads `report.asof.date`. The values match only because the property happens to be 2019-04-01. | `*/screens/*.json` (`*AsOf` fields) |
| R14 | Report index | `ReportIndexAction` sets `reportCount=7`. `index.jsp` lists 14 links, and workload, aging detail, reconciliation and premium detail each appear twice. The header "Reports" link goes to open by adjuster, not the index. | `*/screens/index.json` |
| R15 | Formatting | `FieldTag` formats `money` with `String.format("%.2f")` in the JVM default locale, so a de-DE or fr-FR host renders `1001,00`. `ClaimsActionSupport.money()` uses `Locale.US`. `integer` truncates with a `(long)` cast. The goldens pin en-US output: the tests set `Locale.US` and UTC before rendering. A Windows or non-US host needs the same pinning to match. | `*/screens/*.json` |
| R16 | Counts | `openCount` and `claimCount` come back from the DAO as `Double` (e.g. `24.0`). The JSPs truncate them back to integers. | `*/dao/open-by-adjuster.json`, `*/dao/aged-claims.json` |
