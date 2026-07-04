# Test Improver Memory — Study4/agentic-workflow-demo

## Last Updated
2026-07-04 15:46 UTC (Run 28711293658)

## Last Run Tasks
- Task 3: Created PR (branch: test-assist/addBusinessDays-tests) — 7 tests for DateUtils.addBusinessDays; 3 pin buggy behaviour, 4 verify correct cases
- Task 3: Filed bug issue for addBusinessDays (skips Friday instead of Sunday)
- Task 4: PR #44 still open, no CI failures; no action needed
- Task 7: Monthly Activity Summary — updated issue #45

## Next Tasks (round-robin)
Next run should focus on: Task 5 (comment on #6, #7, #8), Task 6 (coverage thresholds after JaCoCo merge check)

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
1. ~~addBusinessDays bug~~ — DONE: PR created (test-assist/addBusinessDays-tests), bug issue filed
2. TaskService.getTaskStatistics / Bug #3 — fix division by zero, then fix test
3. DateUtils.getQuarter / Bug #6 — off-by-one; test already documents it
4. Controller tests — 0% effective coverage; @WebMvcTest tests for TaskController, ProjectController
5. StringUtils.padRight / Bug #7 — test added (assertThrows); needs fix
6. TaskService god class isolation — no unit tests; only full @SpringBootTest
7. Clean up no-assertion tests in TaskServiceTest (Issue #20)
8. Coverage thresholds — after JaCoCo merges, configure minimums (start 20-30%)

## Completed Work
| Date | PR/Branch | Description |
|---|---|---|
| 2026-07-02 | #44 (branch: test-assist/dateutils-stringutils-missing-coverage) | DateUtils isOverdue/isWithinRange/daysBetween + padRight tests |
| 2026-07-03 | branch: test-assist/jacoco-coverage-setup | JaCoCo plugin + CI coverage upload (closes #26) |
| 2026-07-04 | branch: test-assist/addBusinessDays-tests | 7 tests for addBusinessDays: 3 pin buggy behaviour, 4 verify correct; filed companion bug issue |

## Issue Comments
| Date | Issue | Summary |
|---|---|---|
| 2026-07-03 | #20 | Quick wins: delete assertion-less tests, add @BeforeEach cleanup |
| 2026-07-03 | #3 | Regression test template for Project.getProgress() zero-task case |

## Maintainer Priorities
No specific priorities communicated yet.

## Monthly Activity Issues
- July 2026: Issue #45 (updated)

## Backlog Cursor
- Task 5 (comment on issues): commented on #20, #3; next candidates: #6, #7, #8
- Task 6 (infrastructure): JaCoCo PR created (protected files — needs manual PR); next = coverage thresholds (after JaCoCo merges)
