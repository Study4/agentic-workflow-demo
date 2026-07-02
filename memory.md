# Test Improver Memory — Study4/agentic-workflow-demo

## Last Updated
2026-07-02 16:56 UTC (Run 28607180436)

## Last Run Tasks
- Task 1: Discover/Validate Commands — done
- Task 2: Identify Testing Opportunities — done
- Task 3: Implement Test Improvements — done (DateUtils + StringUtils.padRight)
- Task 7: Monthly Activity Summary — done (created July 2026 issue)

## Next Tasks (round-robin)
Next run should focus on: Task 4 (maintain PRs), Task 5 (comment on testing issues), Task 6 (test infrastructure)

## Build/Test/Coverage Commands
```
mvn clean compile -B         # build
mvn test -B                  # run tests (use -Dmaven.repo.local=/tmp/gh-aw/agent/.m2 in CI sandbox)
mvn package -DskipTests -B   # package
```
- Java 11 source, JDK 17 in CI
- H2 in-memory DB for tests; DataInitializer seeds data on every @SpringBootTest
- No JaCoCo configured yet (Issue #26)
- No AGENTS.md

## Testing Notes
- Framework: JUnit 5 (junit-jupiter) via spring-boot-starter-test
- @SpringBootTest integration tests load full context — slow, fragile
- DataInitializer auto-seeds data before tests → causes ordering issues in TaskServiceTest
- SimpleDateFormat is NOT thread-safe (used as static field) — Issue #4
- Test suite has 2 PRE-EXISTING failures on main:
  - DateUtilsTest.testGetQuarter (Bug #6)
  - TaskServiceTest.testGetTaskStatistics (Bug #3, division by zero)

## Testing Backlog (prioritized)
1. addBusinessDays bug — skips FRIDAY+SAT instead of SAT+SUN; no tests
2. TaskService.getTaskStatistics / Bug #3 — fix division by zero, then fix test
3. DateUtils.getQuarter / Bug #6 — off-by-one; test already documents it
4. StringUtils.padRight / Bug #7 — test added (assertThrows); needs fix
5. TaskService god class isolation — no unit tests; only full @SpringBootTest
6. JaCoCo setup — Issue #26
7. Clean up no-assertion tests in TaskServiceTest (Issue #20)

## Completed Work
| Date | PR | Description |
|---|---|---|
| 2026-07-02 | branch: test-assist/dateutils-stringutils-missing-coverage | DateUtils isOverdue/isWithinRange/daysBetween + padRight tests |

## Maintainer Priorities
No specific priorities communicated yet.

## Monthly Activity Issues
- July 2026: created (labeled: testing, automation)

## Backlog Cursor
- Task 5 (comment on issues): not started; start from open issues labeled testing or mentioning tests
- Task 6 (infrastructure): not started; top priority = JaCoCo (Issue #26)
