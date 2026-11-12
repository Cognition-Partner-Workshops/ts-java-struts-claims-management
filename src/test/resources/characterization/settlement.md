# Settlement characterization table

`settlement.csv` pins what the legacy settlement code returns today. Expected
values are observed behaviour, not intended behaviour; rows marked `suspect`
look wrong and carry a one-line `reason` for the sign-off list. The legacy
runner is `com.northstar.claims.characterization.SettlementCharacterizationTest`
(JUnit 4); a new-stack runner should read the same file unchanged.

## Layers

- `calculator`: `SettlementCalculator.calculate(covered, deductibleText, depreciation, policyLimit)`,
  the string-deductible path used by `SettlementService`. `user` and `claim_id` are unused.
- `screen`: `POST /claims/settlement/calculate.do`. `claim_id`, `covered_amount`,
  `deductible` and `depreciation` are request parameters (the deductible is bound into
  `SettlementForm` by Struts/BeanUtils); `user` is the session user. The policy limit
  comes from the claim's policy in the seeded test database (`policy_limit` cell unused),
  and the operator's `ADJUSTER.approval_limit` gate in `SettlementAuthorityAction` runs first.
- `service`: `SettlementService.calculateAndSave(claimId, covered, deductibleText, depreciation, policyLimit)`;
  persists a row to the seeded test database.

## Cells

- Empty input cell: parameter absent (null). `(blank)` is the empty string, `(spaces)` is three spaces.
  Any other text is passed verbatim (cells are not trimmed; double quotes allow commas).
- `expected_outcome`: `ok` (calculator/service), `settlement` or `refused` (screen forward),
  or `error:<ExceptionSimpleName>` when the call throws.
- Money columns compare exactly as decimals (`9400.00` equals `9400.0`). An empty expected cell is not checked.
- For `refused` rows, `expected_amount` is the refused settlement amount and
  `expected_approval_limit` the operator limit shown on the refusal screen.
- `expected_policy_limit` is the limit the calculate screen shows.
- `expected_extra`: `key=value;...` for service fields (`settlement_id`, `calculated_by`, `calculated_date`).

Seeded approval limits: adjuster1 10000, adjuster2 25000, adjuster3 50000,
adjuster4 2500, adjuster5 5000, supervisor 250000; unknown users get 0.
Claims used: 118 (policy limit 150000), 119 (1000), 120 (100000, policy deductible 5000), 3 (500000).
