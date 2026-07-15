# Test Improver Memory — Study4/agentic-workflow-demo

## Last Updated
2026-07-15 16:08 UTC (Run 29431028385)

## Last Run Tasks
- Task 5: Commented on #26 (JaCoCo branch ready; how to open PR; coverage threshold next)
- Task 2: Backlog reviewed — remaining items are bug fixes and post-JaCoCo work
- Task 7: Updated monthly activity summary issue #45

## Next Tasks (round-robin)
Next run should focus on: Task 3 (fix a bug or clean up no-assertion tests), Task 4 (PR maintenance), Task 6 (coverage thresholds after JaCoCo)

## Build/Test/Coverage Commands
```
mvn clean compile -B         # build
mvn test -B                  # run tests (use -Dmaven.repo.local=/tmp/gh-aw/agent/.m2 in CI sandbox)
mvn jacoco:report -B         # generate coverage report (requires jacoco.exec from prior test run)
mvn package -DskipTests -B   # package
```
- Java 11 source, JDK 17 in CI
- H2 in-memory DB for tests; DataInitializer seeds data on every @SpringBootTest
- JaCoCo 0.8.11 added in branch test-assist/jacoco-coverage-setup

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
- Project.getProgress(): taskCount=0 → returns NaN (0.0/0), not 0.0
- Project.getMemberIds(): null members → NPE; whitespace-padded IDs ("1, 2, 3") → NFE; trailing comma ("1,2,3,") → safe (Java split drops trailing empty tokens)
- String.split(",") with default limit=0 drops trailing empty strings — "1,2,3," parses as [1,2,3]

## Testing Backlog (prioritized)
1. ~~addBusinessDays bug~~ — DONE: PR #49, bug issue #50
2. ~~Controller tests (TaskController)~~ — DONE: 13 @WebMvcTest tests in PR #52
3. ~~Controller tests (ProjectController)~~ — DONE: 16 @WebMvcTest tests in PR #54
4. ~~Controller tests (UserController)~~ — DONE: 21 @WebMvcTest tests in PR #56
5. ~~UserService unit tests~~ — DONE: 27 Mockito tests in PR #59
6. ~~TaskService unit tests~~ — DONE: 29 Mockito tests in PR #63
7. ~~StringUtils.sanitize() XSS bypass~~ — DONE: 8 bug-pin tests in PR #66
8. ~~Project.getProgress() / getMemberIds() bugs~~ — DONE: 9 bug-pin tests in PR #68
9. Coverage thresholds — after JaCoCo merges (branch test-assist/jacoco-coverage-setup, needs maintainer to open PR)
10. DateUtils.getQuarter / Bug #6 — off-by-one; test documents it; one-line fix available
11. StringUtils.padRight / Bug #7 — test added (assertThrows) in PR #44; needs fix
12. addBusinessDays / Bug #50 — easy one-line fix; tests already in PR #49
13. Clean up no-assertion tests in TaskServiceTest (Issue #20)
14. Input validation tests (#13) — after feature is implemented
15. Error handler tests (#14) — after feature is implemented

## Completed Work
| Date | PR/Branch | Description |
|---|---|---|
| 2026-07-02 | #44 | DateUtils isOverdue/isWithinRange/daysBetween + padRight tests |
| 2026-07-03 | branch: test-assist/jacoco-coverage-setup | JaCoCo plugin + CI coverage upload (closes #26) |
| 2026-07-04 | #49 | 7 tests for addBusinessDays: 3 pin buggy behaviour, 4 verify correct; filed bug #50 |
| 2026-07-05 | #52 | 13 @WebMvcTest tests for TaskController; 2 bug-pin tests |
| 2026-07-06 | #54 | 16 @WebMvcTest tests for ProjectController; 2 bug-pin tests |
| 2026-07-07 | #56 | 21 @WebMvcTest tests for UserController; 2 bug-pin tests |
| 2026-07-09 | #59 | 27 Mockito unit tests for UserService; 1 bug-pin (role escalation) |
| 2026-07-11 | #63 | 29 Mockito unit tests for TaskService; 1 bug-pin (division-by-zero Bug #3) |
| 2026-07-13 | #66 | 8 XSS bypass bug-pin tests for StringUtils.sanitize() (Issue #8) |
| 2026-07-14 | #68 | 9 bug-pin tests for Project.getProgress() and getMemberIds() |

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
| 2026-07-15 | #26 | JaCoCo branch ready; how to open PR; coverage threshold next step |

## Maintainer Priorities
No specific priorities communicated yet.

## Monthly Activity Issues
- July 2026: Issue #45 (updated)

## Backlog Cursor
- Task 5 (comment on issues): commented on #20, #3, #6, #7, #8, #16, #13, #14, #26; next candidates: check for new testing-related issues
- Task 6 (infrastructure): JaCoCo branch exists (test-assist/jacoco-coverage-setup); blocked on maintainer opening PR; next = coverage thresholds after merge
