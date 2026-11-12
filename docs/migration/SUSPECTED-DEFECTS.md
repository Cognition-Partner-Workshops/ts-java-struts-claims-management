# Suspected defects: settlement calculator and settlement actions

The characterization suite asserts what legacy NorthStar Claims does today,
including behaviour that looks wrong. Each entry below lists the case ids in
`src/test/resources/characterization/settlement-cases.csv` (and tests in
`SettlementCalculatorTest`), the input, the observed output and why it looks
wrong. Nothing here has been fixed; the Spring Boot port must reproduce these
outputs until the business decides otherwise.

Calculator signature used below: `calculate(covered, deductible, depreciation, policyLimit)`.
Action inputs are request parameters on `/settlement/calculate.do` and `/settlement/save.do`.

## SD-01 Half-cent amounts round half-even (banker's rounding)

- Cases: `C17`, `C19`, `C21`, `A11`, `A12` (and `SettlementCalculatorTest.halfCentRoundsToEvenCent`)
- Input: covered `1.005`, `1.025`, `0.125`; deductible 0, depreciation 0, limit 100.
- Output: `1.00`, `1.02`, `0.12` (while `1.015` gives `1.02` and `2.675` gives `2.68`).
- Why it looks wrong: `SettlementCalculator` uses `ROUND_HALF_EVEN`, so a
  half cent rounds down whenever the cent is even. Claim payouts normally
  round half up; the claimant loses a cent on every even-cent half.

## SD-02 Negative policy limit produces a negative settlement

- Cases: `C16`
- Input: `calculate(100, "0", 0, -50)`.
- Output: amount `-50.00`, capped `true`.
- Why it looks wrong: the limit is not validated, so a bad policy record turns
  into a negative payout instead of an error or zero.

## SD-03 Negative deductible increases the payout

- Cases: `C29`, `A06`
- Input: `calculate(1000, "-500", 0, 10000)`; screen: claim 118, covered `3000.00`, deductible `-500.00`.
- Output: `1500.00`; screen `3500.00` (allowed, not refused).
- Why it looks wrong: a deductible can only reduce a payout; a negative one
  is accepted and added to the loss.

## SD-04 Negative depreciation increases the payout

- Cases: `C30`
- Input: `calculate(1000, "0", -200, 10000)`.
- Output: `1200.00`.
- Why it looks wrong: depreciation is not validated and a negative value pays
  more than the covered amount.

## SD-05 Cap flag is set from the unrounded amount

- Cases: `C26` (and `SettlementCalculatorTest.capFlagIsSetBeforeRounding`)
- Input: `calculate(1000.004, "0", 0, 1000)`.
- Output: amount `1000.00`, capped `true`.
- Why it looks wrong: the comparison runs before rounding, so a loss that
  rounds to exactly the limit is reported as capped, while `C11` (net loss
  exactly at the limit) is not.

## SD-06 Blank deductible on the screen applies zero, not the policy deductible

- Cases: `A02`, `A03`
- Input: claim 120 (policy 9002, deductible 5000), covered `5000.00`, deductible blank or absent, depreciation `500.00`.
- Output: `4500.00`.
- Why it looks wrong: the policy carries a 5000 deductible, but the screen
  only uses the typed value, so leaving it blank pays the full loss.

## SD-07 Malformed deductible on the screen binds silently as zero

- Cases: `A04`, `A05`
- Input: claim 120, deductible `abc` or `1,000.00`.
- Output: `4500.00` / `5000.00` with no validation error.
- Why it looks wrong: `SettlementForm.deductible` is a `double`, so the
  BeanUtils converter turns unparseable text into 0. The calculator itself
  rejects the same text (`C31`, `C32`, `C33`).

## SD-08 Missing or malformed covered amount defaults to 5000.00

- Cases: `A14`, `A15`, `A16`, `A18`
- Input: claim 117, covered blank, `abc`, absent or `$3,000.00`.
- Output: `5000.00`, allowed (and saved when the step is `save`).
- Why it looks wrong: `SettlementAuthorityAction` falls back to a hard-coded
  5000 instead of rejecting the input, so a typo produces a real settlement.

## SD-09 Malformed depreciation applies zero

- Cases: `A21`
- Input: claim 118, covered `3000.00`, depreciation `abc`.
- Output: `3000.00`.
- Why it looks wrong: unparseable depreciation is silently dropped and the
  payout goes up.

## SD-10 Missing or malformed claim id falls back to claim 119

- Cases: `A22`, `A23`
- Input: no `claimId` or `claimId=abc`, covered `5000.00`.
- Output: `1000.00`, capped `true` (policy 9001's 1000 limit).
- Why it looks wrong: the action defaults to a specific real claim instead of
  rejecting the request; a save would be booked against claim 119.

## SD-11 NaN and Infinity amounts fail with an unhandled exception

- Cases: `C34`, `C36`, `C37`, `A19`, `A20`
- Input: deductible `NaN`; covered `NaN` or `Infinity` (calculator and screen).
- Output: `NumberFormatException` from `BigDecimal.valueOf`; the action does not catch it.
- Why it looks wrong: `Double.parseDouble` accepts these words, so they pass
  the action's parsing and crash later instead of producing a validation error.

## SD-12 Unknown claim calculates with a 10000 fallback limit but cannot be saved

- Cases: `A24`, `A46`
- Input: claim 99999 (does not exist), covered `20000.00` / `1000.00`.
- Output: calculate returns `10000.00` capped; save throws `SQLIntegrityConstraintViolationException`.
- Why it looks wrong: the screen shows a settlement against a policy limit that
  does not exist, and the save then fails with a raw database error.

## SD-13 Users without authority can save zero settlements

- Cases: `A36`, `A37`
- Input: session user with no adjuster record (limit 0) or no session user; claim 120, covered `1000.00`, deductible `2000.00`.
- Output: `0.00` saved for claim 120 with `calculated_by` = the user, or the text `null`.
- Why it looks wrong: the authority check is `amount > limit`, so a zero
  amount passes for anyone, including an unauthenticated request; the row
  records the literal string `null` as the calculating user.

## SD-14 Saving the same settlement twice stores two rows

- Cases: `A39`
- Input: two identical `save` requests for claim 117, covered `1200.00`, deductible `200.00`.
- Output: two settlement rows of `1000.00` for claim 117.
- Why it looks wrong: there is no duplicate-submit guard, so a double click
  books the settlement twice.

## SD-15 Session save falls back to the 5000.00 default when no usable pending settlement exists

- Cases: `A41`, `A42`, `A45`
- Input: `A41` calculate then save twice with no amounts; `A42` calculate for claim 117 then save for claim 118; `A45` refused calculate then save.
- Output: `A41` second save stores `5000.00` for claim 117; `A42` stores `5000.00` for claim 118; `A45` refuses `5000.00`.
- Why it looks wrong: once the pending settlement is used, cleared or belongs
  to another claim, the save recalculates from the 5000 default (SD-08)
  instead of rejecting the request.

## SD-16 Service save collides on a derived settlement id and hard-codes the operator

- Cases: `S02`
- Input: `SettlementService.calculateAndSave(117, 1200, "200", 0, 25000)` twice.
- Output: first call stores id 10117 with `calculated_by` `supervisor`, date `2019-03-01`; second call throws `SQLIntegrityConstraintViolationException`.
- Why it looks wrong: the id is `claimId + 10000`, so a claim can only be
  settled once through the service, and the operator and date are constants.

## SD-17 Deductible applied records the full deductible even when the loss was smaller

- Cases: `C07` (and `SettlementCalculatorTest.deductibleAppliedRecordsTheFullDeductible`)
- Input: `calculate(1000, "2000", 100, 20000)`.
- Output: amount `0.00`, `deductibleApplied` `2000.0`.
- Why it looks wrong: only 900 of the deductible was absorbed by the loss; the
  stored record overstates the deductible taken.
