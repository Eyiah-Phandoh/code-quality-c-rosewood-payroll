# Rationale

## Why this design

The inherited `PayrollCalculator` mixed employee lookup, gross pay, overtime, tax selection, net
calculation, printing, and failure hiding. It was split so each class has one reason to change.
`PayrollService` orchestrates, `StandardGrossPayCalculator` computes gross, `BandedTaxCalculator`
selects tax, `NoDeductionsCalculator` supplies the default deduction, and `PayrollResult` carries the
breakdown. `PayrollService` receives every collaborator through its constructor. `PayrollApplication`
is the only place that chooses concrete implementations; everything else depends on interfaces
(`EmployeeRepository`, `GrossPayCalculator`, `TaxCalculator`, `DeductionCalculator`, `TaxBand`).

## How the coming tax-band rule fits

The rule change is assumed to add or adjust a band. That is one edit in `StandardTaxBands.create()`,
for example:

```java
new PercentageTaxBand("HIGH_BAND", new BigDecimal("20000"), new BigDecimal("0.25")),
new PercentageTaxBand("TOP_BAND", null, new BigDecimal("0.30"))
```

This is illustrative only. `PayrollService`, `BandedTaxCalculator`, and the net formula do not
change. A non-percentage rule implements `TaxBand` directly; the optional `isUnbounded()` and
`upperBoundExclusive()` hooks have safe defaults. If the rule becomes marginal (progressive) tax, it
is a new `TaxCalculator` implementation, still without touching the service. If it concerns
overtime, `StandardGrossPayCalculator` accepts the policy through its constructor. Alternatives
rejected: an enum of tax levels and an external config file; both still need code changes or add
parsing for three numbers.

## Known trade-offs

Bands are evaluated in order, so order is part of the contract, and `BandedTaxCalculator` now rejects
a non-last unbounded band and descending bounds. The inherited policy taxes the whole gross at a flat
rate, so crossing a threshold lowers take-home pay (net 949.99 at gross 999.99; 850.00 at 1000.00).
It was preserved and pinned in a test, and should be confirmed with the business.

## Kept, fixed, rewritten

Kept: `Employee`, the repository concept, the 40h / 1.5x and 5/15/25 rules. Fixed: `PayrollCalculator`
was split rather than rewritten, `findById` returns `Optional`, and the catch-all `return 0` became
typed exceptions with causes preserved and unexpected failures logged. Rewritten: all three AI tests.
`calculatesOvertimePay` mocked the class under test, `returnsPayForAnEmployee` asserted non-null on a
primitive `double`, and `calculatesNetPay` asserted only `> 0`.

## Beyond the brief

`PayrollResult` derives net pay and rejects tax + deductions > gross; band-ordering guards; a seeded
property test against the legacy formula; coverage and mutation thresholds enforced by the build.
