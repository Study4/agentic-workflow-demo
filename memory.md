# Test Improver Memory — Study4/agentic-workflow-demo

## Last Updated
2026-07-13 17:01 UTC (Run 29268501237)

## Last Run Tasks
- Task 3: Created PR #aw_pr_xss (branch: test-assist/stringutils-sanitize-xss-bugtests) — 8 XSS bypass bug-pin tests for StringUtils.sanitize() (Issue #8)
- Task 7: Updated monthly activity summary issue #45

## Next Tasks (round-robin)
Next run should focus on: Task 4 (check CI on existing PRs), Task 6 (coverage thresholds after JaCoCo merges), Task 2 (review backlog for new opportunities)

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
- @WebMvcTest needs @MockBean for: TaskService, ProjectRepository (for ProjectController); TaskService (for TaskController); UserService (for UserController)
- Import ambiguity: when using both `import static org.hamcrest.Matchers.*` and `import static org.mockito.ArgumentMatchers.*`, `any(Class)` is ambiguous. Fix: import Hamcrest selectively
- Pure Mockito unit tests (@ExtendWith(MockitoExtension.class)) run in <1s, no Spring context needed for services
- NotificationService is mostly stub code (hardcoded SMTP creds, deleted JavaMail, no-op methods) — low value to test
- User.id is private — use user.setId(id) in tests (not user.id = id)
- stats.get("total") in getTaskStatistics returns Integer (not Long) — use ((Number)stats.get("total")).intValue() in assertions
- StringUtils.sanitize() XSS bypass: <SCRIPT> not stripped; <script > (with space) opening not stripped but closing </script> IS stripped; all event handler tags pass through

## Testing Backlog (prioritized)
1. ~~addBusinessDays bug~~ — DONE: PR #49, bug issue #50
2. ~~Controller tests (TaskController)~~ — DONE: 13 @WebMvcTest tests in PR #52
3. ~~Controller tests (ProjectController)~~ — DONE: 16 @WebMvcTest tests in PR #54
4. ~~Controller tests (UserController)~~ — DONE: 21 @WebMvcTest tests in PR #56
5. ~~UserService unit tests~~ — DONE: 27 Mockito tests in PR #59
6. ~~TaskService unit tests~~ — DONE: 29 Mockito tests in PR #63
7. ~~StringUtils.sanitize() XSS bypass~~ — DONE: 8 bug-pin tests in PR #aw_pr_xss (pending number)
8. TaskService.getTaskStatistics / Bug #3 — fix division by zero (bug-pin test added in PR #63)
9. DateUtils.getQuarter / Bug #6 — off-by-one; test already documents it
10. StringUtils.padRight / Bug #7 — test added (assertThrows) in PR #44; needs fix
11. Clean up no-assertion tests in TaskServiceTest (Issue #20)
12. Coverage thresholds — after JaCoCo merges, configure minimums (start 20-30%)
13. Input validation tests (#13) — after feature is implemented
14. Error handler tests (#14) — after feature is implemented

## Completed Work
| Date | PR/Branch | Description |
|---|---|---|
| 2026-07-02 | #44 (branch: test-assist/dateutils-stringutils-missing-coverage) | DateUtils isOverdue/isWithinRange/daysBetween + padRight tests |
| 2026-07-03 | branch: test-assist/jacoco-coverage-setup | JaCoCo plugin + CI coverage upload (closes #26) |
| 2026-07-04 | #49 (branch: test-assist/addBusinessDays-tests) | 7 tests for addBusinessDays: 3 pin buggy behaviour, 4 verify correct; filed companion bug issue #50 |
| 2026-07-05 | #52 (branch: test-assist/taskcontroller-webmvctest) | 13 @WebMvcTest tests for TaskController; 2 bug-pin tests |
| 2026-07-06 | #54 (branch: test-assist/projectcontroller-webmvctest) | 16 @WebMvcTest tests for ProjectController; 2 bug-pin tests (200-vs-201, NaN-progress) |
| 2026-07-07 | #56 (branch: test-assist/usercontroller-webmvctest) | 21 @WebMvcTest tests for UserController; 2 bug-pin tests (200-vs-201 on register, reset token in password-reset response) |
| 2026-07-09 | #59 (branch: test-assist/userservice-unit-tests) | 27 Mockito unit tests for UserService; 1 bug-pin (role escalation to admin) |
| 2026-07-11 | #63 (branch: test-assist/taskservice-unit-tests) | 29 Mockito unit tests for TaskService; 1 bug-pin (division-by-zero Bug #3) |
| 2026-07-13 | PR pending (branch: test-assist/stringutils-sanitize-xss-bugtests) | 8 XSS bypass bug-pin tests for StringUtils.sanitize() (Issue #8) |

## Issue Comments
| Date | Issue | Summary |
|---|---|---|
| 2026-07-03 | #20 | Quick wins: delete assertion-less tests, add @BeforeEach cleanup |
| 2026-07-03 | #3 | Regression test template for Project.getProgress() zero-task case |
| 2026-07-05 | #6 | One-line getQuarter() fix + call-site audit advice |
| 2026-07-05 | #7 | Pointed to PR #44 bug-pin test; suggested padRight fix |
| 2026-07-07 | #8 | Concrete XSS bypass test cases for StringUtils.sanitize(); recommended OWASP Java HTML Sanitizer |
| 2026-07-10 | #16 | Testing-first approach for TaskService god class refactoring; offered to implement unit tests |
| 2026-07-12 | #13 | @WebMvcTest validation testing strategy; boundary value tests; coordination with #14 |
| 2026-07-12 | #14 | GlobalExceptionHandler testing strategy; scenario table; no-stack-trace assertion tip |

## Maintainer Priorities
No specific priorities communicated yet.

## Monthly Activity Issues
- July 2026: Issue #45 (updated)

## Backlog Cursor
- Task 5 (comment on issues): commented on #20, #3, #6, #7, #8, #16, #13, #14; next candidates: check for any other testing-related open issues
- Task 6 (infrastructure): JaCoCo PR branch exists (test-assist/jacoco-coverage-setup); needs manual PR for protected files (Issue #47); next = coverage thresholds (after JaCoCo merges)
