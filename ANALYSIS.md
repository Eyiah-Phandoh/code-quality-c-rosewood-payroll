# Rosewood Payroll — Pre-Refactoring Analysis

**Status:** diagnosis only. No production or test code has been changed.
**Scope:** the inherited AI-generated net-pay application and its inherited tests.
**Standards referenced:** `AmaliTech Coding Standards Java Edition.pdf`, cited by section number
(for example `§6.1`). The standard's SOLID chapter is `§12`.

---

## 1. Baseline: what the inherited application actually does

### 1.1 Build and test baseline

| Check | Actual result |
|---|---|
| Build tool / configuration | Maven, `pom.xml` |
| Java runtime | OpenJDK 25.0.5 |
| Compiler release | Java 21 (`maven.compiler.release=21`) |
| Test stack | JUnit 5.13.4, Mockito 5.18.0 |
| `mvn clean test` | **BUILD SUCCESS** — 3 tests, 0 failures, 0 errors |
| `mvn verify` | **BUILD SUCCESS** |
| JaCoCo baseline | Lines 21/34 (**62%**), branches 2/6 (**33%**) |
| PIT baseline | 21 mutations generated, 3 killed (**14%**), test strength **27%**, 10 mutations with no coverage |

Tooling caveat: JaCoCo 0.8.12 reports an instrumentation error for a ByteBuddy class with
class-file major version 68 under the JDK 25 runtime. The build succeeds and the report is
generated, but coverage of those test dependencies may be undercounted. This does not affect the
production-code findings below; it is recorded so verification results later are not overstated.

### 1.2 Demo output

```text
Net pay for Efua Sarpong: GHS 760.0
Result e1, 40h @ 20     -> 760.0
Net pay for Kwame Owusu: GHS 1402.5
Result e2, 50h @ 30     -> 1402.5
Result unknown employee -> 0.0
```

### 1.3 Traced business rules (preserved, not re-invented)

The inherited code implements these rules even though they are not documented in the repository:

- Gross pay = `hoursWorked × hourlyRate`.
- Hours above 40 are paid at `1.5×` the hourly rate. The existing formula is equivalent to
  `min(hours, 40) × rate + max(hours − 40, 0) × rate × 1.5`.
- Tax is a **flat rate on total gross**, not progressive marginal taxation:
  - gross `< 1000` → **5%**
  - gross `< 5000` → **15%**
  - otherwise → **25%**
- Net pay = gross − tax. The existing application applies **no deductions at all**.

These rules are business policy. The refactoring will preserve them. The sharp tax cliffs at exactly
1000 and 5000 are existing policy and will not be "fixed" by inventing a progressive scheme.

### 1.4 Traced failure behaviour

| Input | Current result | Problem |
|---|---|---|
| Unknown employee ID | `0.0` | A failed lookup is indistinguishable from a legitimate zero payment |
| `-10` hours at `20` | `-190.0` | A negative payslip is returned as a normal result |
| `Double.NaN` hours | `NaN` | A non-numeric result escapes as if it were money |
| Null employee ID | `0.0` | The resulting `NullPointerException` is swallowed |
| `40` hours at `20` | `760.0` | Correct for the inherited rules |
| `50` hours at `30` | `1402.5` | Correct for the inherited rules |

---

## 2. Production code: smells and standards violations, ranked

Severity scale:

- **P0 — critical:** can silently produce an incorrect financial result.
- **P1 — high:** can produce incorrect results, blocks the upcoming change, or makes the
  application untestable.
- **P2 — medium:** significant maintainability or design problem.
- **P3 — low:** local clarity/consistency issue.

### 2.1 Ranked findings

| Rank | Finding and location | Actual problem | Why it matters | AmaliTech rule broken | Proposed direction |
|---:|---|---|---|---|---|
| **P0** | `PayrollCalculator.calculateNetPay`, `catch (Exception e) { return 0; }` | Every failure — missing employee, null ID, future defect, dependency failure — is converted into a successful-looking zero payment | Payroll fails silently. A zero payslip can be processed and paid; the caller gets no signal that a calculation did not happen. This is the most dangerous defect in the application | `§6.1` — do not ignore caught exceptions; logging and carrying on is explicitly rejected; `§6.3` — add context and preserve causes rather than discarding the original failure | Remove the catch-all. Throw typed domain exceptions. Translate genuinely unexpected runtime failures into `PayrollCalculationException` with the original cause |
| **P1** | `PayrollCalculator.calculateNetPay` | No validation of employee ID, hours, or rate | Negative hours/rate produce negative net pay; `NaN`/infinity propagate; null ID becomes zero through the catch-all. A payroll system must reject invalid financial input rather than calculate with it | `§6.1` (failure must be surfaced), `§8` (a monetary value is exposed as a raw primitive rather than a validated domain value) | Validate at the service boundary; reject null/blank IDs, null values, and negative hours/rates |
| **P1** | Entire calculation | Monetary values use `double` | Binary floating point cannot represent decimal money exactly. The code defines no scale and no rounding rule, so financial results can drift and are not auditable | `§8` — represent domain data deliberately and keep classes small and cohesive; financial correctness is also a domain requirement beyond the standard | Use `BigDecimal`; define scale 2 and `HALF_UP` in one `Money` policy |
| **P1** | `PayrollCalculator.calculateNetPay`, tax `if/else if/else` | Tax bands, rates, and thresholds are hard-coded in the core method (`0.05`, `0.15`, `0.25`, `1000`, `5000`) | The upcoming tax band would require editing working payroll logic. Each new band increases the conditional and the risk of regression. Rules cannot be tested in isolation | `§12.2` — open for extension, closed for modification; `§3.4` — replace magic numbers with named constants; `§12.1` — the method has several reasons to change | `TaxCalculator` abstraction with ordered polymorphic `TaxBand` implementations. A new band is new configuration/a new implementation, not a change to `PayrollService` |
| **P1** | `PayrollCalculator` as a whole | One class retrieves employees, calculates gross, applies overtime, selects tax, calculates net, and prints | God class with low cohesion. Retrieval, calculation, and presentation cannot vary or be tested independently | `§12.1` — one reason to change; `§4.1` and `§4.2` — small methods that do one thing | Split into orchestration, gross calculation, tax calculation, deduction calculation, result representation, and presentation |
| **P2** | `PayrollCalculator` field initialiser: `new InMemoryEmployeeRepository()` | The high-level calculator constructs its own low-level repository | Callers and tests cannot substitute storage. The service is hard-wired to one implementation, so repository failures and missing employees cannot be tested cleanly | `§12.5` — depend on abstractions, not concrete classes; the standard's bad example is exactly a hard-wired collaborator | Constructor-inject `EmployeeRepository` |
| **P2** | `PayrollCalculator.calculateNetPay`, `System.out.println(...)` | A calculation method writes to standard output | Hidden side effect; domain logic performs presentation; output pollutes tests; employee data is leaked to the console | `§4.4` — a function should have no side effects | Return a result object; let the demo/caller present it |
| **P2** | `PayrollCalculator.calculateNetPay` return type | Returns only one `double`; no gross, tax, deduction, or net breakdown | A wrong net cannot be reconciled or audited. There is no single source of truth for the components of a payslip | `§8` — use plain data carriers for data and keep responsibilities explicit; `§12.1` — result representation is a separate concern | Immutable `PayrollResult` containing gross, tax, deductions, and a derived net |
| **P2** | Whole application | No deduction handling exists, although the business context says deductions apply | The model cannot represent deductions; adding them would require changing the core calculator again. No deduction rates are defined, so rates must not be invented | `§12.1` and `§12.2` — separate and extensible policy | Introduce a narrow `DeductionCalculator` extension point; default to zero with `NoDeductionsCalculator` because no policy was supplied |
| **P3** | `EmployeeRepository.findById` | Returns `null` for a missing employee | The contract is implicit. A caller can forget the check; the inherited code did forget it and hid the resulting failure | `§8` — hide internal structure and expose behaviour/contracts clearly | Return `Optional<Employee>`; the service translates absence into `EmployeeNotFoundException` |
| **P3** | `InMemoryEmployeeRepository` constructor | Sample employees are hard-coded in the constructor | The repository cannot be configured with different data without editing production code | `§12.5` — concrete configuration is hard-wired into the class | Accept employee data through a constructor; provide an explicit sample-data factory for the demo |
| **P3** | `Employee` | No validation of ID or name | Blank/null employee data can enter the domain | `§8` — keep domain invariants inside the object | Validate ID and name in the constructor |
| **P3** | All classes in one flat package | No separation of domain, repository, service, tax, deduction, and exceptions | Navigability and dependency direction are unclear; the structure will not scale with the new tax band | `§11.1` — organise packages by feature or layer and keep to the choice | Small feature/layer packages (`domain`, `repository`, `service`, `tax`, `deduction`, `exception`) |
| **P3** | Comments such as `// LOW`, `// returns null when the id is not found` | Comments compensate for magic values and an ambiguous contract | The comment, not the code, carries the policy; the null comment documents a contract that should be expressed by the type | `§10.1` — comments should explain business complexity, not repair unclear code; `§3.4` — name policy values | Replace comments with named constants, typed contracts, and self-explanatory code |

### 2.2 SOLID assessment

| Principle | Present violation? | Evidence | Required design response |
|---|---|---|---|
| **SRP** (`§12.1`) | **Yes** | `PayrollCalculator` has retrieval, gross calculation, overtime, tax selection, net calculation, and printing responsibilities | Separate `PayrollService`, `GrossPayCalculator`, `TaxCalculator`, `DeductionCalculator`, `PayrollResult`, and presentation |
| **OCP** (`§12.2`) | **Yes** | Tax bands are hard-coded in `calculateNetPay`; a new band edits the working method | Polymorphic `TaxBand` list behind `TaxCalculator`; add bands through configuration |
| **LSP** (`§12.3`) | **No current inheritance**, so no existing violation | There are no subclasses to substitute | Keep the future `TaxBand` contract narrow and honoured by every implementation; tests must exercise each band through the same abstraction |
| **ISP** (`§12.4`) | **No current violation** | `EmployeeRepository` has one method; interfaces are not bloated | Keep `TaxCalculator`, `TaxBand`, and `DeductionCalculator` narrow; do not create one large payroll interface |
| **DIP** (`§12.5`) | **Yes** | `PayrollCalculator` constructs `InMemoryEmployeeRepository` itself | Constructor-inject all collaborators; depend on `EmployeeRepository` and `TaxCalculator` abstractions |

I have not invented an LSP or ISP violation where the inherited code has none. The design must avoid
creating them: every `TaxBand` implementation must mean the same thing to `BandedTaxCalculator`
("does this band apply, and what tax does it charge?"), and no client should be forced to implement
irrelevant methods.

### 2.3 The single most serious issue

**The catch-all that returns `0` is the most serious issue.**

It is more dangerous than a crash because it disguises a failed payroll run as a successful one:

1. A missing employee receives a valid-looking `GHS 0.00` result.
2. A future defect in tax or overtime, or a dependency outage, is hidden rather than reported.
3. The caller cannot distinguish legitimate zero pay from "the calculation never happened".
4. Nothing is logged, propagated, or marked as failed, so the error can reach payment and
   accounting processes.

In payroll, a visible failure is recoverable; a silent wrong answer is not. The absence of input
validation compounds this: the application can also return a **negative net pay** or `NaN` as if it
were money. The hard-coded tax conditionals are the most important **extensibility** defect, but the
silent failure is the most important **financial-safety** defect.

---

## 3. The inherited AI-generated tests: what they prove

All three inherited tests are in
`src/test/java/com/rosewood/payroll/PayrollCalculatorTest.java`. None of them establishes that the
payroll calculation is correct.

| Rank | Test | What it currently does | Why it proves nothing | AmaliTech rule broken | Verdict and required behaviour |
|---:|---|---|---|---|---|
| **1** | `calculatesOvertimePay` | Creates a Mockito mock of `PayrollCalculator`, stubs `calculateNetPay("e2", 50, 30)` to return `1402.5`, then asserts the stub returns `1402.5` | It is tautological. It mocks the class under test, so no overtime, tax, or net logic runs. The test passes regardless of whether production code is correct. `1402.5` is configured directly into the mock | `§9` — every test must assert behaviour that would fail if the code were wrong; a test with no meaningful assertion does not meet the standard even if it counts as coverage | **Remove and replace.** Exercise real gross and tax logic. Independently derive gross `1650.00`, tax `247.50`, net `1402.50` |
| **2** | `returnsPayForAnEmployee` | Calls the real calculator with `35h @ 25` and asserts `assertNotNull(net)` on a primitive `double` | The primitive is autoboxed; the assertion can never fail. It proves only that a value was returned, not that it is correct. A broken calculator returning `0`, `-1`, or `9999` would still pass | `§9` — the assertion must be capable of failing when behaviour is wrong | **Remove and replace.** Assert the exact net `831.25` (gross `875.00`, tax `43.75`) |
| **3** | `calculatesNetPay` | Calls the real calculator with `40h @ 20` and asserts `assertTrue(net > 0)` | It verifies a sign, not a calculation. Any positive value passes, including a value from a broken tax rule. It does execute real code, but it does not check the result | `§9` — meaningful assertion; `§9` — test name should state what is being tested | **Replace.** Assert gross `800.00`, tax `40.00`, net `760.00` via the result object |

### 3.1 Additional testing-standard failures

- **Coverage target not met.** `§9` requires 100% statement and branch coverage. Actual baseline:
  **62% lines** and **33% branches**.
- **Mutation effectiveness is poor.** PIT kills only **3 of 21** mutations (14%), with test strength
  **27%**. The suite cannot reliably detect deliberate defects in overtime, tax selection, or net
  pay. `§9` explicitly recommends mutation testing as a check that assertions are real.
- **No failure tests.** Missing employee, invalid hours/rates, repository failure, and tax failure
  are untested. The catch-all bug is therefore invisible to the suite.
- **No boundary tests.** There are no cases at 40 hours, 1000 gross, or 5000 gross, where the
  business rules change.
- **No deduction tests.** The application has no deduction logic and no test would notice if
  deductions were added incorrectly.
- **No rounding/precision tests.** `double` is used without any expected-value checks for precision.
- **Wildcard imports.** The test file uses `import static org.junit.jupiter.api.Assertions.*;` and
  `import static org.mockito.Mockito.*;`, contrary to `§11.1` ("no wildcard imports").
- **Names do not state the expected behaviour.** `calculatesNetPay`, `calculatesOvertimePay`, and
  `returnsPayForAnEmployee` do not say what result is expected, contrary to `§9` ("it states what it
  is testing, in its name").

**Conclusion:** there is no inherited test worth keeping unchanged. `calculatesNetPay` is a weak
smoke test; the other two are hollow. All three will be replaced by behaviour-focused tests with
independently derived expected values.

---

## 4. Decisions

### 4.1 Key design decisions

| Decision | Rationale |
|---|---|
| Constructor-inject `EmployeeRepository`, `GrossPayCalculator`, `TaxCalculator`, and `DeductionCalculator` into `PayrollService` | Satisfies `§12.5`; makes storage, tax policy, and deductions replaceable in tests without changing production code |
| Keep `EmployeeRepository` as a one-method interface and change `findById` to return `Optional<Employee>` | Removes the ambiguous null contract (`§8`); the service converts an empty result into `EmployeeNotFoundException` |
| Split `PayrollCalculator` into `PayrollService`, `GrossPayCalculator`, and `PayrollResult` | `§12.1`: orchestration, gross calculation, and result representation have different reasons to change |
| Represent tax policy as `TaxCalculator` + ordered `TaxBand` implementations, with `BandedTaxCalculator` selecting the first applicable band | `§12.2`: the core service does not contain band conditionals. A new band is a new `TaxBand` implementation and configuration entry; existing payroll logic stays unchanged |
| Preserve the inherited flat-rate bands and the strict boundaries (`<1000`, `<5000`, otherwise) | These are business rules, not defects. Changing them would silently alter pay |
| Use `BigDecimal` for hours, rates, gross, tax, deductions, and net | Exact decimal arithmetic; prohibits `NaN`/infinity; makes the monetary contract explicit |
| Normalise money to scale 2 with `HALF_UP` in a single `Money` policy | Gives the application an explicit, testable rounding rule instead of undefined `double` behaviour. Gross, tax, and deductions are rounded to 2 dp; net is derived from those values |
| Derive `netPay()` inside `PayrollResult` and enforce `tax + deductions ≤ gross` | A payslip cannot report a net value that disagrees with its own components; negative net pay is rejected as a calculation failure |
| Introduce a focused unchecked exception hierarchy: `PayrollException`, `InvalidPayrollInputException`, `EmployeeNotFoundException`, `EmployeeRepositoryException`, `PayrollCalculationException`, `TaxCalculationException` | `§6.1` and `§6.3`: failures are explicit, carry context, and preserve causes. Unchecked exceptions keep the service API usable without meaningless `throws` clauses |
| Use `NoDeductionsCalculator` as the production default | The repository defines no deduction policy. Inventing rates would violate the brief. The abstraction and tests demonstrate how a real policy would be added |
| Keep `Employee` and the in-memory repository concept; make repository data configurable and add an explicit sample-data factory | Avoids a rewrite while removing hidden configuration and making the repository testable |
| Update `Demo` to wire the concrete implementations and report a missing employee explicitly | Presentation belongs at the composition root (`§4.4`); the demo must not present failure as `0.0` |

### 4.2 How the design absorbs the coming tax band

The extension path is:

1. Add a new `TaxBand` implementation (for example, a band with a different formula) or a new
   `PercentageTaxBand` entry with its own threshold and rate.
2. Add it to the ordered band list supplied to `BandedTaxCalculator` (the standard list lives in
   `StandardTaxBands`).
3. Do **not** change `PayrollService`, `GrossPayCalculator`, the net formula, or existing band
   implementations.

`BandedTaxCalculator` depends only on the `TaxBand` contract, so any implementation that honours
"does this band apply?" and "how much tax does it charge?" can be substituted. Tests will construct
the service with a custom band set and assert the resulting tax and net, proving that the
orchestration is genuinely closed for modification.

If the future band has rules that are not a flat percentage, it can implement `TaxBand` directly;
the design does not force every rule into a percentage-shaped class.

### 4.3 Kept, fixed, and rewritten

| Area | Decision | Reason |
|---|---|---|
| `Employee` | **Kept**, with constructor validation added | The domain concept is correct; only invariants were missing |
| `EmployeeRepository` | **Kept as an interface**, refactored to `Optional<Employee>` | Preserves the abstraction while making the missing-employee contract explicit |
| `InMemoryEmployeeRepository` | **Kept**, made configurable, with an explicit sample-data factory | Preserves the in-memory implementation and demo data without hiding configuration |
| `PayrollCalculator` | **Fixed by splitting**, not rewritten from scratch | The payroll rules are correct in principle; the class has too many responsibilities and unsafe failure behaviour |
| Gross/overtime formula | **Preserved** | Existing business policy (`40` hours, `1.5×` overtime) |
| Tax rates and thresholds | **Preserved** | Existing business policy (`5% / 15% / 25%`; `1000 / 5000`) |
| `Demo` | **Fixed** | Must use injected composition and report missing employees instead of printing `0.0` |
| `PayrollCalculatorTest.calculatesNetPay` | **Replaced** | Assertion was too weak to detect a wrong result |
| `PayrollCalculatorTest.calculatesOvertimePay` | **Removed and replaced** | Self-mock was tautological |
| `PayrollCalculatorTest.returnsPayForAnEmployee` | **Removed and replaced** | `assertNotNull` on a primitive can never fail |

### 4.4 Improvement beyond the brief

**Immutable `PayrollResult` with a derived net and enforced financial invariants**, supported by a
single `Money` rounding policy.

Why this is the highest-value addition:

- A payslip exposes gross, tax, deductions, and net, so the result can be reconciled and audited.
- `netPay()` is derived from the same fields that are reported, so the displayed net cannot silently
  disagree with its components.
- The constructor rejects null/negative monetary components and cases where `tax + deductions`
  exceeds gross. A failed or inconsistent calculation cannot be mistaken for a successful payslip.
- The `Money` policy makes the previously undefined rounding rule explicit and testable.

### 4.5 How the diagnosis was checked

The following checks produced the evidence in this document:

1. Inspected every production class, the test class, `pom.xml`, and the repository layout.
2. Ran `mvn clean test` — 3 tests passed.
3. Ran the demo and recorded the output for known and unknown employees.
4. Traced the calculation manually for normal pay, overtime, each tax band, missing employees,
   negative hours, and `NaN`.
5. Ran `mvn verify` and generated the JaCoCo report (62% lines, 33% branches).
6. Ran PIT (`mvn test-compile org.pitest:pitest-maven:mutationCoverage`) — 3/21 mutations killed,
   test strength 27%.
7. Used the AmaliTech standard to map each finding to a specific section.

### 4.6 Verification planned after implementation

The implementation will not be claimed as complete until these checks have actually run:

- `mvn clean test` — all unit and integration tests.
- `mvn verify` — compilation, tests, and JaCoCo report.
- PIT mutation coverage — compare against the 14%/27% baseline.
- Explicit review of the failure paths: missing employee, invalid hours/rate, repository failure,
  tax failure, deduction exceeding gross, and invalid tax configuration.
- An extensibility test that adds a new tax band through `TaxCalculator`/`TaxBand` without editing
  `PayrollService`.
- A final `git diff` review to confirm no unrelated files or user changes were overwritten.

### 4.7 Decisions requiring reviewer sign-off

1. **Rounding:** scale 2, `HALF_UP`, applied to gross, tax, and deductions; net derived. This
   preserves the demo values `760.00` and `1402.50`.
2. **Deductions:** production default remains zero (`NoDeductionsCalculator`) because no deduction
   policy exists in the repository. No rates are invented.
3. **Repository contract:** `findById` returns `Optional<Employee>`; the service throws
   `EmployeeNotFoundException` when empty.
4. **Exceptions:** unchecked `PayrollException` hierarchy as listed above.
5. **Business rules:** overtime above 40 hours at 1.5×; flat tax at 5% below 1000, 15% below 5000,
   25% otherwise; strict `<` boundaries preserved.
