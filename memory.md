# Test Improver Memory — Study4/agentic-workflow-demo

## Last Updated
2026-07-06 17:24 UTC (Run 28810181834)

## Last Run Tasks
- Task 3: Created PR (branch: test-assist/projectcontroller-webmvctest) — 16 @WebMvcTest tests for ProjectController; 2 bug-pin tests (200-vs-201 on create, NaN-progress on dashboard with zero tasks)
- Task 4: PRs #44, #49, #52 still open, no CI failures; no action needed
- Task 7: Monthly Activity Summary — updated issue #45

## Next Tasks (round-robin)
Next run should focus on: Task 6 (coverage thresholds after JaCoCo merge check), Task 5 (comment on issues #8 and others with testing label)

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
- Jackson 2.13 (Spring Boot 2.7) serializes Double.NaN as literal "NaN" token (not valid JSON) — does NOT throw; returns 200 with non-standard body
- Project.getMemberIds() throws NPE when members is null → Jackson serialization of any Project with members=null causes 500; test data must set members to a valid value
- @WebMvcTest needs @MockBean for: TaskService, ProjectRepository (for ProjectController); TaskService (for TaskController)

## Testing Backlog (prioritized)
1. ~~addBusinessDays bug~~ — DONE: PR #49, bug issue #50
2. ~~Controller tests (TaskController)~~ — DONE: 13 @WebMvcTest tests in PR #52
3. ~~Controller tests (ProjectController)~~ — DONE: 16 @WebMvcTest tests in branch test-assist/projectcontroller-webmvctest
4. TaskService.getTaskStatistics / Bug #3 — fix division by zero, then fix test
5. DateUtils.getQuarter / Bug #6 — off-by-one; test already documents it
6. StringUtils.padRight / Bug #7 — test added (assertThrows); needs fix
7. UserController tests — 0% effective coverage; similar to TaskController/ProjectController
8. TaskService god class isolation — no unit tests; only full @SpringBootTest
9. Clean up no-assertion tests in TaskServiceTest (Issue #20)
10. Coverage thresholds — after JaCoCo merges, configure minimums (start 20-30%)

## Completed Work
| Date | PR/Branch | Description |
|---|---|---|
| 2026-07-02 | #44 (branch: test-assist/dateutils-stringutils-missing-coverage) | DateUtils isOverdue/isWithinRange/daysBetween + padRight tests |
| 2026-07-03 | branch: test-assist/jacoco-coverage-setup | JaCoCo plugin + CI coverage upload (closes #26) |
| 2026-07-04 | #49 (branch: test-assist/addBusinessDays-tests) | 7 tests for addBusinessDays: 3 pin buggy behaviour, 4 verify correct; filed companion bug issue #50 |
| 2026-07-05 | #52 (branch: test-assist/taskcontroller-webmvctest) | 13 @WebMvcTest tests for TaskController; 2 bug-pin tests |
| 2026-07-06 | branch: test-assist/projectcontroller-webmvctest | 16 @WebMvcTest tests for ProjectController; 2 bug-pin tests (200-vs-201, NaN-progress) |

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
- Task 5 (comment on issues): commented on #20, #3, #6, #7; next candidates: #8, then other issues with 'testing' label
- Task 6 (infrastructure): JaCoCo PR branch exists (test-assist/jacoco-coverage-setup); needs manual PR for protected files (Issue #47); next = coverage thresholds (after JaCoCo merges)
