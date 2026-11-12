# Struts action coverage

Explicit requests only; redirects, forwards, probes, and implicit login are not counted.

## Action mappings

| Action path | Scenario ids | Setup scenario ids |
| --- | --- | --- |
| `/admin/adjusters.do` | admin_adjusters_authenticated, admin_adjusters_anonymous | — |
| `/admin/reference.do` | admin_reference_authenticated, admin_reference_anonymous | — |
| `/home.do` | session_home_authenticated, session_home_anonymous | — |
| `/intake/confirm.do` | — | — |
| `/intake/new.do` | — | — |
| `/intake/submit.do` | fnol_submit, intake_lenient_date, intake_missing_claimant, intake_missing_description, intake_bad_date, intake_blank_reserve | — |
| `/login.do` | login, session_login_ok, session_login_adjuster, session_login_bad_password, session_login_unknown_user, session_login_blank_fields, session_login_blank_username, session_login_blank_password | — |
| `/logout.do` | session_logout | session_protected_after_logout |
| `/payment/detail.do` | — | — |
| `/payment/history.do` | payment_history | — |
| `/payment/issue.do` | payment_issue | — |
| `/payment/remittance.do` | — | — |
| `/policy/coverageDetail.do` | policy_coverageDetail_9001, policy_coverageDetail_9002, policy_coverageDetail_99999, policy_coverageDetail_invalid, policy_coverageDetail_blank, policy_coverageDetail_default | — |
| `/policy/coverages.do` | policy_coverages_9001, policy_coverages_9002, policy_coverages_99999, policy_coverages_invalid, policy_coverages_blank, policy_coverages_default | — |
| `/policy/insuredParty.do` | policy_insuredParty_9001, policy_insuredParty_9002, policy_insuredParty_99999, policy_insuredParty_invalid, policy_insuredParty_blank, policy_insuredParty_default | — |
| `/policy/list.do` | session_protected_after_logout, policy_list | — |
| `/policy/renewal.do` | policy_renewal_9001, policy_renewal_9002, policy_renewal_99999, policy_renewal_invalid, policy_renewal_blank, policy_renewal_default, policy_renewal_post | — |
| `/policy/search.do` | policy_search, policy_search_auto, policy_search_homeowners, policy_search_commercial_property, policy_search_general_liability, policy_search_marine, policy_search_blank, policy_search_default, policy_search_policyNumber, policy_search_insuredName, policy_search_term | — |
| `/policy/view.do` | policy_view, policy_view_1, policy_view_2, policy_view_3, policy_view_4, policy_view_9002, policy_view_99999, policy_view_invalid, policy_view_blank, policy_view_default | — |
| `/report/adjusterClaims.do` | — | — |
| `/report/adjusterWorkload.do` | — | — |
| `/report/agedClaims.do` | report_aged_claims | — |
| `/report/claimAgingDetail.do` | — | — |
| `/report/claimInventory.do` | report_claim_inventory | — |
| `/report/closedClaims.do` | report_closed_claims | — |
| `/report/index.do` | — | — |
| `/report/lossRatio.do` | report_loss_ratio | — |
| `/report/openByAdjuster.do` | report_open_by_adjuster | — |
| `/report/premiumDetail.do` | — | — |
| `/report/reconciliation.do` | — | — |
| `/settlement/calculate.do` | settlement_calculate, settlement_blank_deductible, settlement_half_cent, settlement_policy_cap, settlement_deductible_floor | settlement_session_save |
| `/settlement/detail.do` | — | — |
| `/settlement/save.do` | settlement_save, settlement_authority_refused, settlement_authority_within_limit, settlement_session_save | — |
| `/workbench/assign.do` | workbench_assign | — |
| `/workbench/list.do` | — | — |
| `/workbench/note.do` | — | — |
| `/workbench/notes.do` | — | — |
| `/workbench/reserve.do` | workbench_reserve | — |
| `/workbench/reserveHistory.do` | — | — |
| `/workbench/status.do` | workbench_status | — |
| `/workbench/statusHistory.do` | — | — |
| `/workbench/view.do` | — | — |

## Requests with no mapping

| Action path | Scenario ids | Setup scenario ids |
| --- | --- | --- |
| `/admin/editAdjuster.do` | admin_editAdjuster_authenticated, admin_editAdjuster_anonymous | — |
