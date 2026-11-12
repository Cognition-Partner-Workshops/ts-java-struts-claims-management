# Capture characterization coverage

Scenarios are appended after the original 28, retaining their request order,
claim ids and payloads. Area metadata lives in scenario options and the index.
The route gate parses the route-table rows in ROUTES.md, not a duplicated list.
The extra linked-but-unmapped admin edit route is also captured.

## PR #11 settlement defects

| Defect | Scenario(s) / limitation |
|---|---|
| SD-01 half-even rounding | settlement_boundary_coveredAmount_7, _8, _9 |
| SD-02 negative policy limit | Not reachable through HTTP with this seed: no policy-write route. Remains covered by PR #11's calculator C16, not this harness. |
| SD-03 negative deductible | settlement_boundary_deductible_5 |
| SD-04 negative depreciation | settlement_boundary_depreciation_1 |
| SD-05 cap before rounding | settlement_boundary_coveredAmount_12 |
| SD-06 blank deductible ignores policy | settlement_blank_deductible |
| SD-07 malformed deductible becomes zero | settlement_boundary_deductible_2, _3 |
| SD-08 covered amount defaults to 5000 | settlement_boundary_coveredAmount_0, _1, _2 |
| SD-09 malformed depreciation | settlement_boundary_depreciation_0 |
| SD-10 missing/bad claim defaults to 119 | settlement_boundary_claimId_0, _1 |
| SD-11 non-finite amounts crash | settlement_boundary_coveredAmount_3, _4; settlement_boundary_deductible_4 |
| SD-12 unknown claim fallback / save failure | settlement_boundary_claimId_2; settlement_unknown_save |
| SD-13 zero save without authority | authority_unknown_zero. AuthFilter blocks anonymous requests before the action, so the literal null-user variant cannot be reached over HTTP. |
| SD-14 duplicate saves | settlement_session_duplicate (two requests) |
| SD-15 missing pending session fallback | settlement_session_consumed, settlement_session_mismatch |
| SD-16 service derived-id collision / hard-coded operator | Not reachable: HTTP save uses SettlementDAO directly, not SettlementService.calculateAndSave. Retained in PR #11's S02, not claimed as HTTP coverage. |
| SD-17 full deductible recorded above loss | settlement_deductible_floor; authority_unknown_zero (sidecar SETTLEMENT row) |

## PR #12 report defects

| Defect | Scenario(s) / limitation |
|---|---|
| R1 premium join fan-out | month_end_lossRatio, month_end_reconciliation |
| R2 zero-premium ratio | No zero-premium seeded policy and no policy-write HTTP route. PR #12 edge fixture remains necessary. |
| R3 all-status losses / money-formatted ratio | month_end_lossRatio |
| R4 blank reconciliation totals | month_end_reconciliation |
| R5 blank oldest report | month_end_adjusterWorkload |
| R6 cents truncated | report_age_zero (100.99 reserve), subsequent report_age_* |
| R7 differing open populations | month_end_agedClaims, month_end_openByAdjuster, month_end_claimAgingDetail |
| R8 future age and inclusive boundaries | report_age_future, report_age_zero, report_age_thirty, report_age_thirty_one, report_age_sixty, report_age_sixty_one, report_age_ninety, report_age_ninety_one |
| R9 OPEN-only aging detail | month_end_claimAgingDetail |
| R10 closure-day discrepancy | report_asof_closure_closedClaims, report_asof_closure_claimInventory (same claim closed on as-of date) |
| R11 unused claimCounts SQL/order ties | No action calls claimCounts; HTTP cannot exercise it. PR #12 DAO characterization remains necessary. |
| R12 adjuster parameter concatenation / unknown empty | report_claims_adjuster1, report_claims_adjuster5, report_claims_adjuster9, report_claims_unknown, report_claims_blank. No SQL injection payloads are executed; source-level SQL construction is not proven by a transcript. |
| R13 inconsistent as-of sources | month_end_* pins today's matching date; distinguishing the sources requires changing configuration, deliberately not done here. |
| R14 index count/link mismatch | month_end_index pins reportCount; the harness extracts semantic fields, not anchors, so duplicate links are not characterized here. |
| R15 locale-dependent money / integer truncation | month_end_* and report_age_* pin current host formatting. Alternate-locale behavior is not covered by this fixed-locale capture. |
| R16 DAO counts returned as Double | month_end_openByAdjuster, month_end_agedClaims pin rendered counts, not Java runtime types. DAO type characterization remains in PR #12. |

These limitations are intentional: no application, seed or configuration changes
and no merging of characterization branches. Route coverage does not imply all
DAO-only or alternate-configuration defects are reachable by an HTTP scenario.

Harness unit checks: `python3 -m unittest discover -s tools/capture -p 'test_*.py'`.
