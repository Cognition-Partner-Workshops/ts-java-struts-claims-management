# Struts action coverage

Explicit requests only; redirects, forwards, probes, and implicit login are not counted.

## Action mappings

| Action path | Scenario ids | Setup scenario ids |
| --- | --- | --- |
| `/admin/adjusters.do` | admin_adjusters_authenticated, admin_adjusters_anonymous | — |
| `/admin/reference.do` | admin_reference_authenticated, admin_reference_anonymous | — |
| `/home.do` | session_home_authenticated, session_home_anonymous | — |
| `/intake/confirm.do` | intake_confirm_after_setup, intake_confirm_missing, intake_confirm_invalid, intake_confirm_not_found | — |
| `/intake/new.do` | intake_new_form | — |
| `/intake/submit.do` | fnol_submit, intake_lenient_date, intake_missing_claimant, intake_missing_description, intake_bad_date, intake_blank_reserve, intake_loss_type_fire, intake_loss_type_water, intake_loss_type_liability, intake_loss_type_collision, intake_reserve_positive, intake_reserve_zero, intake_reserve_negative, intake_reserve_blank_coercion, intake_reserve_nonnumeric_coercion, intake_required_blank_claimantName, intake_required_blank_lossDate, intake_required_blank_description, intake_required_missing_claimantName, intake_required_missing_lossDate, intake_required_missing_description, intake_date_leap_year, intake_date_nonleap_february, intake_date_april_rollover, intake_date_month_rollover, intake_date_zero_month_day, intake_date_format_rejected | intake_confirm_after_setup |
| `/login.do` | login, session_login_ok, session_login_adjuster, session_login_bad_password, session_login_unknown_user, session_login_blank_fields, session_login_blank_username, session_login_blank_password | — |
| `/logout.do` | session_logout | session_protected_after_logout |
| `/payment/detail.do` | payment_detail_1, payment_detail_62, payment_detail_99999, payment_detail_invalid | — |
| `/payment/history.do` | payment_history, payment_history_124, payment_history_1, payment_history_99999 | — |
| `/payment/issue.do` | payment_issue, payment_issue_check, payment_issue_eft, payment_issue_ach, payment_issue_wire, payment_amount_zero, payment_amount_negative, payment_amount_half_cent, payment_amount_blank, payment_amount_nonnumeric, payment_amount_above_settlement, payment_blank_payeeName, payment_blank_paymentMethod, payment_missing_payeeName, payment_missing_paymentMethod, payment_missing_amount, payment_without_settlement | — |
| `/payment/remittance.do` | payment_remittance_1, payment_remittance_124, payment_remittance_99999, payment_remittance_invalid, payment_remittance_payment_id_ignored | — |
| `/policy/coverageDetail.do` | policy_coverageDetail_9001, policy_coverageDetail_9002, policy_coverageDetail_99999, policy_coverageDetail_invalid, policy_coverageDetail_blank, policy_coverageDetail_default | — |
| `/policy/coverages.do` | policy_coverages_9001, policy_coverages_9002, policy_coverages_99999, policy_coverages_invalid, policy_coverages_blank, policy_coverages_default | — |
| `/policy/insuredParty.do` | policy_insuredParty_9001, policy_insuredParty_9002, policy_insuredParty_99999, policy_insuredParty_invalid, policy_insuredParty_blank, policy_insuredParty_default | — |
| `/policy/list.do` | session_protected_after_logout, policy_list | — |
| `/policy/renewal.do` | policy_renewal_9001, policy_renewal_9002, policy_renewal_99999, policy_renewal_invalid, policy_renewal_blank, policy_renewal_default, policy_renewal_post | — |
| `/policy/search.do` | policy_search, policy_search_auto, policy_search_homeowners, policy_search_commercial_property, policy_search_general_liability, policy_search_marine, policy_search_blank, policy_search_default, policy_search_policyNumber, policy_search_insuredName, policy_search_term | — |
| `/policy/view.do` | policy_view, policy_view_1, policy_view_2, policy_view_3, policy_view_4, policy_view_9002, policy_view_99999, policy_view_invalid, policy_view_blank, policy_view_default | — |
| `/report/adjusterClaims.do` | report_adjusterClaims_adjuster1, report_adjusterClaims_adjuster2, report_adjusterClaims_adjuster3, report_adjusterClaims_adjuster4, report_adjusterClaims_adjuster5, report_adjusterClaims_supervisor, report_adjusterClaims_blank, report_adjusterClaims_unknown, report_adjusterClaims_missing | — |
| `/report/adjusterWorkload.do` | report_adjusterWorkload_rendered | — |
| `/report/agedClaims.do` | report_aged_claims, report_agedClaims_rendered | — |
| `/report/claimAgingDetail.do` | report_claimAgingDetail_rendered | — |
| `/report/claimInventory.do` | report_claim_inventory, report_claimInventory_rendered | — |
| `/report/closedClaims.do` | report_closed_claims, report_closedClaims_rendered | — |
| `/report/index.do` | report_index_rendered | — |
| `/report/lossRatio.do` | report_loss_ratio, report_lossRatio_rendered | — |
| `/report/openByAdjuster.do` | report_open_by_adjuster, report_openByAdjuster_rendered | — |
| `/report/premiumDetail.do` | report_premiumDetail_rendered | — |
| `/report/reconciliation.do` | report_reconciliation_rendered | — |
| `/settlement/calculate.do` | settlement_calculate, settlement_blank_deductible, settlement_half_cent, settlement_policy_cap, settlement_deductible_floor, settlement_calculate_standard, settlement_calculate_blank_covered, settlement_calculate_nonnumeric_covered, settlement_calculate_blank_deductible_coercion, settlement_calculate_nonnumeric_deductible_coercion, settlement_calculate_round_even_down, settlement_calculate_round_even_up, settlement_calculate_round_2675, settlement_calculate_low_policy_cap, settlement_calculate_deductible_exceeds_loss, settlement_calculate_depreciated, settlement_calculate_depreciation_exceeds_loss, settlement_calculate_blank_depreciation, settlement_calculate_nonnumeric_depreciation | settlement_session_save, settlement_save_pending_isolated |
| `/settlement/detail.do` | settlement_detail_124, settlement_detail_140, settlement_detail_invalid | — |
| `/settlement/save.do` | settlement_save, settlement_authority_refused, settlement_authority_within_limit, settlement_session_save, settlement_save_direct, settlement_save_pending_isolated, settlement_save_authority_refused, settlement_save_authority_within, settlement_save_authority_exact_limit | — |
| `/workbench/assign.do` | workbench_assign, workbench_assign_adjuster1, workbench_assign_adjuster2, workbench_assign_adjuster3, workbench_assign_adjuster4, workbench_assign_adjuster5, workbench_assign_supervisor, workbench_assign_blank_default, workbench_assign_invalid_id | — |
| `/workbench/list.do` | workbench_list_default, workbench_list_ignored_filters | — |
| `/workbench/note.do` | workbench_note_text, workbench_note_blank, workbench_note_missing | — |
| `/workbench/notes.do` | workbench_notes_changed, workbench_notes_untouched | — |
| `/workbench/reserve.do` | workbench_reserve, workbench_reserve_positive, workbench_reserve_blank, workbench_reserve_nonnumeric, workbench_reserve_negative, workbench_reserve_zero | — |
| `/workbench/reserveHistory.do` | workbench_reserveHistory_changed, workbench_reserveHistory_untouched | — |
| `/workbench/status.do` | workbench_status, workbench_status_open, workbench_status_investigating, workbench_status_approved, workbench_status_denied, workbench_status_closed, workbench_status_pending, workbench_status_blank_default, workbench_status_close_repeat, workbench_status_reopen | workbench_status_close_repeat |
| `/workbench/statusHistory.do` | workbench_statusHistory_changed, workbench_statusHistory_untouched | — |
| `/workbench/view.do` | workbench_view_1, workbench_view_119, workbench_view_120, workbench_view_124, workbench_view_140, workbench_view_99999, workbench_view_invalid | — |

## Requests with no mapping

| Action path | Scenario ids | Setup scenario ids |
| --- | --- | --- |
| `/admin/editAdjuster.do` | admin_editAdjuster_authenticated, admin_editAdjuster_anonymous | — |
