# Test Improver Memory — Study4/agentic-workflow-demo

## Last Updated
2026-07-05 15:51 UTC (Run 28746282958)

## Last Run Tasks
- Task 3: Created PR (branch: test-assist/taskcontroller-webmvctest) — 13 @WebMvcTest tests for TaskController; 2 bug-pin tests (200-vs-404 and 400-vs-404)
- Task 4: PRs #44, #49 still open, no CI failures; no action needed
- Task 5: Commented on #6 (getQuarter fix + call-site audit) and #7 (pointed to PR #44 bug-pin test)
- Task 7: Monthly Activity Summary — updated issue #45

## Next Tasks (round-robin)
Next run should focus on: Task 6 (coverage thresholds after JaCoCo merge check), Task 2 (refresh backlog)

## Build/Test/Coverage Commands
```
mvn clean compile -B         # build
mvn test -B                  # run tests (use -Dmaven.repo.local=/tmp/gh-aw/agent/.m2 in CI sandbox)
mvn jacoco:report -B         # generate coverage report (requires jacoco.exec from prior test run)
mvn package -DskipTests -B   # package
```
- Java 11 source, JDK 17 in CI
- H2 in-memory DB for tests; DataInitializer seeds data on every @SpringBootTest
- JaCoCo 0.8.11 added in PR (test-assist/jacoco-coverage-setup)

## Coverage Baseline (from test-assist/jacoco-coverage-setup branch)
- Instructions: 1930/4553 = 42% (inflated by DataInitializer executing in @SpringBootTest)
- Branches: 141/397 = 35%
- Lines: 400/1001 = 39%
- Notable gaps: controllers ~1%, UserService ~1%, NotificationService ~11%

## Testing Notes
- Framework: JUnit 5 (junit-jupiter) via spring-boot-starter-test
- @SpringBootTest integration tests load full context — slow, fragile
- DataInitializer auto-seeds data before tests → causes ordering issues in TaskServiceTest
- SimpleDateFormat is NOT thread-safe (used as static field) — Issue #4
- Test suite has 2 PRE-EXISTING failures on main:
  - DateUtilsTest.testGetQuarter (Bug #6)
  - TaskServiceTest.testGetTaskStatistics (Bug #3, division by zero)

## Testing Backlog (prioritized)
1. ~~addBusinessDays bug~~ — DONE: PR #49, bug issue #50
2. ~~Controller tests (TaskController)~~ — DONE: 13 @WebMvcTest tests in branch test-assist/taskcontroller-webmvctest
3. TaskService.getTaskStatistics / Bug #3 — fix division by zero, then fix test
4. DateUtils.getQuarter / Bug #6 — off-by-one; test already documents it
5. StringUtils.padRight / Bug #7 — test added (assertThrows); needs fix
6. ProjectController tests — 0% effective coverage; similar to TaskController
7. TaskService god class isolation — no unit tests; only full @SpringBootTest
8. Clean up no-assertion tests in TaskServiceTest (Issue #20)
9. Coverage thresholds — after JaCoCo merges, configure minimums (start 20-30%)

## Completed Work
| Date | PR/Branch | Description |
|---|---|---|
| 2026-07-02 | #44 (branch: test-assist/dateutils-stringutils-missing-coverage) | DateUtils isOverdue/isWithinRange/daysBetween + padRight tests |
| 2026-07-03 | branch: test-assist/jacoco-coverage-setup | JaCoCo plugin + CI coverage upload (closes #26) |
| 2026-07-04 | #49 (branch: test-assist/addBusinessDays-tests) | 7 tests for addBusinessDays: 3 pin buggy behaviour, 4 verify correct; filed companion bug issue #50 |
| 2026-07-05 | branch: test-assist/taskcontroller-webmvctest | 13 @WebMvcTest tests for TaskController; 2 bug-pin tests |

## Issue Comments
| Date | Issue | Summary |
|---|---|---|
| 2026-07-03 | #20 | Quick wins: delete assertion-less tests, add @BeforeEach cleanup |
| 2026-07-03 | #3 | Regression test template for Project.getProgress() zero-task case |
| 2026-07-05 | #6 | One-line getQuarter() fix + call-site audit advice |
| 2026-07-05 | #7 | Pointed to PR #44 bug-pin test; suggested padRight fix |

## Maintainer Priorities
No specific priorities communicated yet.

## Monthly Activity Issues
- July 2026: Issue #45 (updated)

## Backlog Cursor
- Task 5 (comment on issues): commented on #20, #3, #6, #7; next candidates: #8, then issues with 'testing' label
- Task 6 (infrastructure): JaCoCo PR created (protected files — needs manual PR); next = coverage thresholds (after JaCoCo merges)
