# Integration crosscheck

- Integration branch: `claims-boot-integration`.
- Base: `claims-boot-foundation`, `8a1aa34aece965f4133e0f1c9a3e74f6ded40e0c`.
- Frozen baseline: `parity-v1` (tag ref `e28dbeaa2cfbb4ab81ce5be2df2ec4754853d83c`).
- Manifest check: `Manifest: 440 files, 0 mismatches`.

## Integrated branches

Merged with `git merge --no-ff` in this order. Head SHAs are the feature
heads actually merged (the second parent of each merge), not later branch tips.

| Area branch (origin/) | Head SHA | Merge commit SHA |
| --- | --- | --- |
| devin/1794509292-claims-boot-policy | dbae2e861a8d71fd2aba88bcedebf5de61849b10 | 24eb7b0297ce6b19fbcaa3ca6a1fe2e34b25245a |
| devin/1794509320-claims-boot-intake-workbench | b83daf8a5cadf6ca7d4edea2866b3ca40b1bb59f | d9aa27c3f8a8e9b0ac81909a856ad173e0911ce3 |
| devin/settlement-boot | e8a0e3c5e5588480a0f707c46dbefd29e4298522 | 93209adc1a37cc91a7f642df7f03aac3fa375fc4 |
| devin/1794509294-claims-boot-payments | 721a4ef48749dff203cae03e37c743506cdbcc28 | c880fda8d9a0f29014033f70dd218c8f8c32c15c |
| devin/1794509301-claims-boot-reports | b17c53e4745a37be71813002d665ccb9619fa0d1 | 58816bebdd26677de04a615cb581a6577dcf39cc |

## Merge conflicts

One add/add conflict in `docs/migration/SUSPECTED-DEFECTS.md`: settlement
content from `devin/settlement-boot`, already integrated, versus
`devin/1794509301-claims-boot-reports`. Both complete sections were retained,
settlement first and reports second; only conflict markers were removed.
There were no other shared-file conflicts and no code changes were needed.

## How it was run

All commands below start from the repository root unless a subshell says
otherwise. OpenJDK 21 was absent and was installed with:

```sh
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk
```

Full integrated build and tests (JDK 21):

```sh
(cd claims-boot && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B -q verify)
```

Legacy build/seed and full replay (JDK 11):

```sh
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 PATH=/usr/lib/jvm/java-11-openjdk-amd64/bin:$PATH make build seed
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 PATH=/usr/lib/jvm/java-11-openjdk-amd64/bin:$PATH make replay
```

`make build` cleans the legacy target; `make seed` initializes it.
`make replay` runs `python3 tools/capture/capture.py replay --start-legacy`,
resets/seeds again, starts its own Jetty on HTTP 8080 (context `/claims`)
and HSQL server on 9001 (alias `northstar`), then stops Jetty after replay.
No second legacy server was running.

Boot was launched once from the repository root with JDK 21 and an explicit
fresh reset; this replay was not rerun:

```sh
/usr/lib/jvm/java-21-openjdk-amd64/bin/java -jar claims-boot/target/claims-boot-0.1.0-SNAPSHOT.jar --claims.db.reset=true --claims.db.server.enabled=true
```

This serves HTTP 8081 (context `/claims`), with loopback HSQL server 9002,
alias `northstar`, using its separate `claims-boot/target/db/claims-boot`
database. The launch resets and seeds before requests. Full replay:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin python3 tools/capture/capture.py replay --base-url http://localhost:8081/claims --db-url jdbc:hsqldb:hsql://localhost:9002/northstar
```

Neither replay used an `--area` filter. Before any future Boot replay,
restart with a fresh reset/seed rather than reusing the post-replay database.

Explicit characterization suites and manifest validation:

```sh
(cd claims-boot && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -B test -Dtest=SettlementCharacterizationTest,ReportBaselineGoldenTest,ReportEdgeGoldenTest)
python3 tools/capture/capture.py --verify-manifest parity/MANIFEST.json
```

## Results

```text
Legacy:      Replay: 217 scenarios, 0 diffs
claims-boot: Replay: 217 scenarios, 0 diffs
Manifest: 440 files, 0 mismatches
```

Both replays also compared the initial `_seed database` against
`transcripts/db/_seed.json` before the scenario stream: **match** for both
apps (included in the zero-diff totals). The harness compares a normalized
response file and database snapshot for every scenario. No diffs remain.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Full claims-boot verify | 142 | 0 | 0 | 0 |
| SettlementCharacterizationTest (explicit run) | 88 | 0 | 0 | 0 |
| ReportBaselineGoldenTest (explicit run) | 1 | 0 | 0 | 0 |
| ReportEdgeGoldenTest (explicit run) | 1 | 0 | 0 | 0 |

Full verify exited 0 and generated the executable JAR. The explicit run
printed `Tests run: 90, Failures: 0, Errors: 0, Skipped: 0` and
`BUILD SUCCESS`. These explicit tests are a subset of full verify, not
additional distinct tests.

Raw execution evidence on the crosscheck machine:

- `~/crosscheck/legacy-replay.diff`
- `~/crosscheck/boot-replay.diff`
- `~/crosscheck/legacy-build-seed.log`
- `~/crosscheck/boot-characterization.log`
- `~/crosscheck/boot-server.log`
- `~/claims-boot-integration-verify.log`

The replay files contain coverage/summary lines only, with no per-scenario
diff blocks, because every comparison matched.

## Per-area counts

| Area | Scenarios |
| --- | ---: |
| shared foundation | 10 |
| policy | 23 |
| intake | 54 |
| settlement | 85 |
| payments | 15 |
| reports | 30 |
| **Total** | **217** |

## Per-scenario results

Generated by script importing `tools/capture/capture.py` and enumerating
`SCENARIOS` in its original order. Area comes from
`options(scenario)["area"]` (the harness's route-prefix tagging), matching
the ownership in [ROUTES.md](ROUTES.md). Intake includes workbench; payments
and reports use their route ownership even when older scenario module labels
say settlement or reporting. Shared routes, including login, are shared
foundation regardless of a legacy module label. Routes are relative to
`/claims`; query strings are retained. Each result column covers both the
normalized response and full database snapshot, not just the HTTP status.

| # | Scenario name | Route | Module | Area | Legacy response+DB | claims-boot response+DB |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | `login` | `POST /login.do` | policy | shared foundation | match | match |
| 2 | `policy_search` | `GET /policy/search.do?lineOfBusiness=AUTO` | policy | policy | match | match |
| 3 | `policy_view` | `GET /policy/view.do?policyId=9001` | policy | policy | match | match |
| 4 | `fnol_submit` | `POST /intake/submit.do` | intake | intake | match | match |
| 5 | `workbench_assign` | `POST /workbench/assign.do` | workbench | intake | match | match |
| 6 | `workbench_status` | `POST /workbench/status.do?status=INVESTIGATING` | workbench | intake | match | match |
| 7 | `workbench_reserve` | `POST /workbench/reserve.do` | workbench | intake | match | match |
| 8 | `settlement_calculate` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 9 | `settlement_save` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 10 | `payment_issue` | `POST /payment/issue.do` | settlement | payments | match | match |
| 11 | `payment_history` | `GET /payment/history.do` | settlement | payments | match | match |
| 12 | `report_open_by_adjuster` | `GET /report/openByAdjuster.do` | reporting | reports | match | match |
| 13 | `report_loss_ratio` | `GET /report/lossRatio.do` | reporting | reports | match | match |
| 14 | `report_aged_claims` | `GET /report/agedClaims.do` | reporting | reports | match | match |
| 15 | `settlement_blank_deductible` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 16 | `intake_lenient_date` | `POST /intake/submit.do` | intake | intake | match | match |
| 17 | `settlement_half_cent` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 18 | `settlement_policy_cap` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 19 | `settlement_deductible_floor` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 20 | `intake_missing_claimant` | `POST /intake/submit.do` | intake | intake | match | match |
| 21 | `intake_missing_description` | `POST /intake/submit.do` | intake | intake | match | match |
| 22 | `intake_bad_date` | `POST /intake/submit.do` | intake | intake | match | match |
| 23 | `intake_blank_reserve` | `POST /intake/submit.do` | intake | intake | match | match |
| 24 | `settlement_authority_refused` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 25 | `settlement_authority_within_limit` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 26 | `settlement_session_save` | `GET /settlement/save.do?claimId=117` | settlement | settlement | match | match |
| 27 | `report_closed_claims` | `GET /report/closedClaims.do` | reporting | reports | match | match |
| 28 | `report_claim_inventory` | `GET /report/claimInventory.do` | reporting | reports | match | match |
| 29 | `policy_view_default` | `GET /policy/view.do` | policy | policy | match | match |
| 30 | `policy_view_invalid` | `GET /policy/view.do?policyId=abc` | policy | policy | match | match |
| 31 | `policy_view_unknown` | `GET /policy/view.do?policyId=99999` | policy | policy | match | match |
| 32 | `policy_coverages_default` | `GET /policy/coverages.do` | policy | policy | match | match |
| 33 | `policy_coverages_invalid` | `GET /policy/coverages.do?policyId=abc` | policy | policy | match | match |
| 34 | `policy_coverages_unknown` | `GET /policy/coverages.do?policyId=99999` | policy | policy | match | match |
| 35 | `policy_coverageDetail_default` | `GET /policy/coverageDetail.do` | policy | policy | match | match |
| 36 | `policy_coverageDetail_invalid` | `GET /policy/coverageDetail.do?policyId=abc` | policy | policy | match | match |
| 37 | `policy_coverageDetail_unknown` | `GET /policy/coverageDetail.do?policyId=99999` | policy | policy | match | match |
| 38 | `policy_insuredParty_default` | `GET /policy/insuredParty.do` | policy | policy | match | match |
| 39 | `policy_insuredParty_invalid` | `GET /policy/insuredParty.do?policyId=abc` | policy | policy | match | match |
| 40 | `policy_insuredParty_unknown` | `GET /policy/insuredParty.do?policyId=99999` | policy | policy | match | match |
| 41 | `policy_renewal_default` | `GET /policy/renewal.do` | policy | policy | match | match |
| 42 | `policy_renewal_invalid` | `GET /policy/renewal.do?policyId=abc` | policy | policy | match | match |
| 43 | `policy_renewal_unknown` | `GET /policy/renewal.do?policyId=99999` | policy | policy | match | match |
| 44 | `workbench_view_default` | `GET /workbench/view.do` | intake | intake | match | match |
| 45 | `workbench_view_invalid` | `GET /workbench/view.do?claimId=abc` | intake | intake | match | match |
| 46 | `workbench_view_unknown` | `GET /workbench/view.do?claimId=99999` | intake | intake | match | match |
| 47 | `workbench_notes_default` | `GET /workbench/notes.do` | intake | intake | match | match |
| 48 | `workbench_notes_invalid` | `GET /workbench/notes.do?claimId=abc` | intake | intake | match | match |
| 49 | `workbench_notes_unknown` | `GET /workbench/notes.do?claimId=99999` | intake | intake | match | match |
| 50 | `workbench_reserveHistory_default` | `GET /workbench/reserveHistory.do` | intake | intake | match | match |
| 51 | `workbench_reserveHistory_invalid` | `GET /workbench/reserveHistory.do?claimId=abc` | intake | intake | match | match |
| 52 | `workbench_reserveHistory_unknown` | `GET /workbench/reserveHistory.do?claimId=99999` | intake | intake | match | match |
| 53 | `workbench_statusHistory_default` | `GET /workbench/statusHistory.do` | intake | intake | match | match |
| 54 | `workbench_statusHistory_invalid` | `GET /workbench/statusHistory.do?claimId=abc` | intake | intake | match | match |
| 55 | `workbench_statusHistory_unknown` | `GET /workbench/statusHistory.do?claimId=99999` | intake | intake | match | match |
| 56 | `settlement_detail_default` | `GET /settlement/detail.do` | settlement | settlement | match | match |
| 57 | `settlement_detail_invalid` | `GET /settlement/detail.do?claimId=abc` | settlement | settlement | match | match |
| 58 | `settlement_detail_unknown` | `GET /settlement/detail.do?claimId=99999` | settlement | settlement | match | match |
| 59 | `payment_history_default` | `GET /payment/history.do` | payments | payments | match | match |
| 60 | `payment_history_invalid` | `GET /payment/history.do?claimId=abc` | payments | payments | match | match |
| 61 | `payment_history_unknown` | `GET /payment/history.do?claimId=99999` | payments | payments | match | match |
| 62 | `payment_remittance_default` | `GET /payment/remittance.do` | payments | payments | match | match |
| 63 | `payment_remittance_invalid` | `GET /payment/remittance.do?claimId=abc` | payments | payments | match | match |
| 64 | `payment_remittance_unknown` | `GET /payment/remittance.do?claimId=99999` | payments | payments | match | match |
| 65 | `payment_detail_default` | `GET /payment/detail.do` | payments | payments | match | match |
| 66 | `payment_detail_invalid` | `GET /payment/detail.do?paymentId=abc` | payments | payments | match | match |
| 67 | `payment_detail_unknown` | `GET /payment/detail.do?paymentId=99999` | payments | payments | match | match |
| 68 | `intake_confirm_default` | `GET /intake/confirm.do` | intake | intake | match | match |
| 69 | `intake_confirm_invalid` | `GET /intake/confirm.do?claimId=abc` | intake | intake | match | match |
| 70 | `intake_confirm_unknown` | `GET /intake/confirm.do?claimId=99999` | intake | intake | match | match |
| 71 | `screen_home` | `GET /home.do` | shared foundation | shared foundation | match | match |
| 72 | `screen_admin_adjusters` | `GET /admin/adjusters.do` | shared foundation | shared foundation | match | match |
| 73 | `screen_admin_reference` | `GET /admin/reference.do` | shared foundation | shared foundation | match | match |
| 74 | `screen_admin_editAdjuster` | `GET /admin/editAdjuster.do` | shared foundation | shared foundation | match | match |
| 75 | `screen_policy_list` | `GET /policy/list.do` | policy | policy | match | match |
| 76 | `screen_intake_new` | `GET /intake/new.do` | intake | intake | match | match |
| 77 | `screen_workbench_list` | `GET /workbench/list.do` | intake | intake | match | match |
| 78 | `screen_workbench_assignment` | `GET /workbench/assignment.do` | intake | intake | match | match |
| 79 | `screen_intake_intake_submit` | `GET /intake/intake/submit.do` | intake | intake | match | match |
| 80 | `screen_logout` | `GET /logout.do` | shared foundation | shared foundation | match | match |
| 81 | `login_bad_password` | `POST /login.do` | shared foundation | shared foundation | match | match |
| 82 | `login_empty` | `POST /login.do` | shared foundation | shared foundation | match | match |
| 83 | `login_unknown` | `POST /login.do` | shared foundation | shared foundation | match | match |
| 84 | `login_prefix_credentials` | `POST /login.do` | shared foundation | shared foundation | match | match |
| 85 | `policy_search_auto` | `GET /policy/search.do?lineOfBusiness=AUTO` | policy | policy | match | match |
| 86 | `policy_search_home` | `GET /policy/search.do?lineOfBusiness=HOME` | policy | policy | match | match |
| 87 | `policy_search_property` | `GET /policy/search.do?lineOfBusiness=PROPERTY` | policy | policy | match | match |
| 88 | `policy_search_blank` | `GET /policy/search.do?lineOfBusiness=` | policy | policy | match | match |
| 89 | `policy_search_unknown` | `GET /policy/search.do?lineOfBusiness=UNKNOWN` | policy | policy | match | match |
| 90 | `fnol_claimantName_0` | `POST /intake/submit.do` | intake | intake | match | match |
| 91 | `fnol_claimantName_1` | `POST /intake/submit.do` | intake | intake | match | match |
| 92 | `fnol_description_0` | `POST /intake/submit.do` | intake | intake | match | match |
| 93 | `fnol_description_1` | `POST /intake/submit.do` | intake | intake | match | match |
| 94 | `fnol_lossDate_0` | `POST /intake/submit.do` | intake | intake | match | match |
| 95 | `fnol_lossDate_1` | `POST /intake/submit.do` | intake | intake | match | match |
| 96 | `fnol_lossDate_2` | `POST /intake/submit.do` | intake | intake | match | match |
| 97 | `fnol_lossDate_3` | `POST /intake/submit.do` | intake | intake | match | match |
| 98 | `fnol_lossDate_4` | `POST /intake/submit.do` | intake | intake | match | match |
| 99 | `fnol_initialReserve_0` | `POST /intake/submit.do` | intake | intake | match | match |
| 100 | `fnol_initialReserve_1` | `POST /intake/submit.do` | intake | intake | match | match |
| 101 | `fnol_initialReserve_2` | `POST /intake/submit.do` | intake | intake | match | match |
| 102 | `fnol_initialReserve_3` | `POST /intake/submit.do` | intake | intake | match | match |
| 103 | `fnol_initialReserve_4` | `POST /intake/submit.do` | intake | intake | match | match |
| 104 | `fnol_all_missing` | `POST /intake/submit.do` | intake | intake | match | match |
| 105 | `fnol_then_workbench` | `GET /workbench/list.do` | intake | intake | match | match |
| 106 | `settlement_boundary_coveredAmount_0` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 107 | `settlement_boundary_coveredAmount_1` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 108 | `settlement_boundary_coveredAmount_2` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 109 | `settlement_boundary_coveredAmount_3` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 110 | `settlement_boundary_coveredAmount_4` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 111 | `settlement_boundary_coveredAmount_5` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 112 | `settlement_boundary_coveredAmount_6` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 113 | `settlement_boundary_coveredAmount_7` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 114 | `settlement_boundary_coveredAmount_8` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 115 | `settlement_boundary_coveredAmount_9` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 116 | `settlement_boundary_coveredAmount_10` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 117 | `settlement_boundary_coveredAmount_11` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 118 | `settlement_boundary_coveredAmount_12` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 119 | `settlement_boundary_coveredAmount_13` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 120 | `settlement_boundary_deductible_0` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 121 | `settlement_boundary_deductible_1` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 122 | `settlement_boundary_deductible_2` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 123 | `settlement_boundary_deductible_3` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 124 | `settlement_boundary_deductible_4` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 125 | `settlement_boundary_deductible_5` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 126 | `settlement_boundary_deductible_6` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 127 | `settlement_boundary_deductible_7` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 128 | `settlement_boundary_depreciation_0` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 129 | `settlement_boundary_depreciation_1` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 130 | `settlement_boundary_depreciation_2` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 131 | `settlement_boundary_depreciation_3` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 132 | `settlement_boundary_claimId_0` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 133 | `settlement_boundary_claimId_1` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 134 | `settlement_boundary_claimId_2` | `POST /settlement/calculate.do` | settlement | settlement | match | match |
| 135 | `authority_supervisor_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 136 | `authority_supervisor_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 137 | `authority_supervisor_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 138 | `authority_adjuster1_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 139 | `authority_adjuster1_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 140 | `authority_adjuster1_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 141 | `authority_adjuster2_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 142 | `authority_adjuster2_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 143 | `authority_adjuster2_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 144 | `authority_adjuster3_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 145 | `authority_adjuster3_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 146 | `authority_adjuster3_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 147 | `authority_adjuster4_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 148 | `authority_adjuster4_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 149 | `authority_adjuster4_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 150 | `authority_adjuster5_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 151 | `authority_adjuster5_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 152 | `authority_adjuster5_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 153 | `authority_adjuster6_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 154 | `authority_adjuster6_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 155 | `authority_adjuster6_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 156 | `authority_adjuster7_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 157 | `authority_adjuster7_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 158 | `authority_adjuster7_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 159 | `authority_adjuster8_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 160 | `authority_adjuster8_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 161 | `authority_adjuster8_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 162 | `authority_adjuster9_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 163 | `authority_adjuster9_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 164 | `authority_adjuster9_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 165 | `authority_adjuster10_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 166 | `authority_adjuster10_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 167 | `authority_adjuster10_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 168 | `authority_adjusterUnknown_within` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 169 | `authority_adjusterUnknown_at` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 170 | `authority_adjusterUnknown_above` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 171 | `authority_unknown_zero` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 172 | `settlement_session_duplicate` | `POST /settlement/save.do?claimId=117` | settlement | settlement | match | match |
| 173 | `settlement_session_consumed` | `GET /settlement/save.do?claimId=117` | settlement | settlement | match | match |
| 174 | `settlement_session_mismatch` | `GET /settlement/save.do?claimId=117` | settlement | settlement | match | match |
| 175 | `settlement_unknown_save` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 176 | `month_end_index` | `GET /report/index.do` | reports | reports | match | match |
| 177 | `month_end_openByAdjuster` | `GET /report/openByAdjuster.do` | reports | reports | match | match |
| 178 | `month_end_lossRatio` | `GET /report/lossRatio.do` | reports | reports | match | match |
| 179 | `month_end_agedClaims` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 180 | `month_end_adjusterWorkload` | `GET /report/adjusterWorkload.do` | reports | reports | match | match |
| 181 | `month_end_claimAgingDetail` | `GET /report/claimAgingDetail.do` | reports | reports | match | match |
| 182 | `month_end_reconciliation` | `GET /report/reconciliation.do` | reports | reports | match | match |
| 183 | `month_end_premiumDetail` | `GET /report/premiumDetail.do` | reports | reports | match | match |
| 184 | `month_end_closedClaims` | `GET /report/closedClaims.do` | reports | reports | match | match |
| 185 | `month_end_claimInventory` | `GET /report/claimInventory.do` | reports | reports | match | match |
| 186 | `report_claims_adjuster1` | `GET /report/adjusterClaims.do?adjuster=adjuster1` | reports | reports | match | match |
| 187 | `report_claims_adjuster5` | `GET /report/adjusterClaims.do?adjuster=adjuster5` | reports | reports | match | match |
| 188 | `report_claims_adjuster9` | `GET /report/adjusterClaims.do?adjuster=adjuster9` | reports | reports | match | match |
| 189 | `report_claims_unknown` | `GET /report/adjusterClaims.do?adjuster=unknown` | reports | reports | match | match |
| 190 | `report_claims_blank` | `GET /report/adjusterClaims.do?adjuster=` | reports | reports | match | match |
| 191 | `workbench_status_open` | `POST /workbench/status.do` | intake | intake | match | match |
| 192 | `workbench_status_investigating` | `POST /workbench/status.do` | intake | intake | match | match |
| 193 | `workbench_status_approved` | `POST /workbench/status.do` | intake | intake | match | match |
| 194 | `workbench_status_closed` | `POST /workbench/status.do` | intake | intake | match | match |
| 195 | `workbench_status_denied` | `POST /workbench/status.do` | intake | intake | match | match |
| 196 | `workbench_assign_adjuster9` | `POST /workbench/assign.do` | intake | intake | match | match |
| 197 | `workbench_reserve_0_005` | `POST /workbench/reserve.do` | intake | intake | match | match |
| 198 | `workbench_reserve_-1` | `POST /workbench/reserve.do` | intake | intake | match | match |
| 199 | `workbench_reserve_abc` | `POST /workbench/reserve.do` | intake | intake | match | match |
| 200 | `workbench_note_Parity_review` | `POST /workbench/note.do` | intake | intake | match | match |
| 201 | `payment_issue_missing` | `POST /payment/issue.do` | payments | payments | match | match |
| 202 | `payment_issue_no_method` | `POST /payment/issue.do` | payments | payments | match | match |
| 203 | `payment_issue_zero` | `POST /payment/issue.do` | payments | payments | match | match |
| 204 | `payment_issue_negative` | `POST /payment/issue.do` | payments | payments | match | match |
| 205 | `report_age_future` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 206 | `report_age_zero` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 207 | `report_age_thirty` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 208 | `report_age_thirty_one` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 209 | `report_age_sixty` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 210 | `report_age_sixty_one` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 211 | `report_age_ninety` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 212 | `report_age_ninety_one` | `GET /report/agedClaims.do` | reports | reports | match | match |
| 213 | `report_asof_closure_closedClaims` | `GET /report/closedClaims.do` | reports | reports | match | match |
| 214 | `report_asof_closure_claimInventory` | `GET /report/claimInventory.do` | reports | reports | match | match |
| 215 | `authority_adjuster5_limit_4999_99` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 216 | `authority_adjuster5_limit_5000` | `POST /settlement/save.do` | settlement | settlement | match | match |
| 217 | `authority_adjuster5_limit_5000_01` | `POST /settlement/save.do` | settlement | settlement | match | match |

## Diff classification

None: no code bugs and no new sign-off candidates were found by this replay.
Legacy behaviours documented in [SUSPECTED-DEFECTS.md](SUSPECTED-DEFECTS.md)
are ported as-is and remain the business-owner's existing sign-off items,
not new crosscheck findings.

## Scope caveat

Replay compares normalized transcripts (responses and DB probes/snapshots)
as the harness defines them, not full HTML or visual equivalence. Visual
styling outside that normalization, such as legacy pages that inline tables
without `northstar.css`, is not covered. No visual styling verification was
performed as part of this crosscheck.
