# Settlement characterization table

`settlement-cases.csv` pins what the legacy settlement code does today. The
legacy runner is `com.northstar.claims.characterization.SettlementCharacterizationTest`;
the Spring Boot runner must read the same file unchanged and produce the same
results. Expected values are observed legacy behaviour, including the cases
listed in `docs/migration/SUSPECTED-DEFECTS.md`.

One row is one case. Cells that hold one value per step are split with `|`;
a cell without `|` applies to every step. `~` means the value is absent
(no request parameter, `null` argument or no session user). An empty
expected cell is not asserted, except `expected_saved_claim`, where empty
means no settlement row may be written by that step.

| Column | Meaning |
| --- | --- |
| `case_id` | Stable id: `C` calculator, `A` web actions, `S` service. |
| `scope` | `calculator` = `SettlementCalculator.calculate(double, String, double, double)`; `action` = settlement screen request; `service` = `SettlementService.calculateAndSave`. |
| `steps` | `calculate` or `save` for actions (one shared HTTP session per row); `calculate` for the calculator; `calculate_and_save` for the service. |
| `actor_limit` | Session user: an adjuster with this approval limit, `none` = a user with no adjuster record, `~` = no user in the session. |
| `claim_id` | `claimId` request parameter (raw text for actions). |
| `claim_setup` | `seed` = claim exists in `seed.sql` with `policy_limit`; `fixture` = the runner creates a claim and policy with that id and `policy_limit`; `absent` = the claim must not exist; empty = not checked. |
| `policy_limit` | Policy limit passed to the calculator and service; for actions the limit of the claim's policy. |
| `covered_amount`, `deductible`, `depreciation` | Raw inputs (request parameter text for actions). |
| `expected_amount` | Settlement amount, or the refused amount when refused. |
| `expected_capped` | `cappedAtLimit` of the result (and of the stored row). |
| `expected_refused` | `true` when the authority check refused the request. |
| `expected_saved_claim` | Claim id of the single settlement row written by the step; empty = nothing written. |
| `expected_error` | Simple class name of the exception the step throws. |
| `suspected_defect` | Id of the entry in `docs/migration/SUSPECTED-DEFECTS.md`. |
| `description` | What the case shows. |

Stored rows must also carry the amount and cap flag above, and `calculated_by`
equal to the session user (`null` when there is none; `supervisor` for the
service).
