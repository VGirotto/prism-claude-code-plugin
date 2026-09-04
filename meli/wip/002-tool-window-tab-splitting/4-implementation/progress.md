# Implementation Progress

**Status**: validation_pending
**Strategy**: batched
**Version**: 1.4.0

| Task | Status | Evidence |
|---|---|---|
| TASK-001 | completed | Runtime capability adapter compiles on builds 243 and 262 |
| TASK-002 | completed | Session binding and lifecycle tests pass |
| TASK-003 | completed | Recursive content, focused manager, and pane focus integrated |
| TASK-004 | completed | Split actions and bound toolbar/shortcuts implemented |
| TASK-005 | completed | 170 tests pass and baseline ZIP builds |
| TASK-006 | completed | Independent review findings fixed and rechecked |
| TASK-007 | completed | Split path moves Content without process recreation |
| TASK-008 | completed | Fixed reflection targets; no dependency or external-input changes |

## Validation Evidence

- IntelliJ Platform 2024.3/build 243: `test buildPlugin` passed.
- GoLand 2026.2.1/build 262: `test` passed with the invocation-scoped Kotlin 2.4.10 override.
- Test suite: 170 tests, 0 skipped, 0 failures, 0 errors.
- Plugin Verifier 1.409: Compatible with IC-243.21565.193.
- Plugin Verifier 1.409: Compatible with GO-262.9437.195; only pre-existing deprecated/scheduled-for-removal API reports.
- `git diff --check`: passed.
- Artifact: `build/distributions/prism-ide-companion-1.4.0.zip`.

## Pending Manual Validation

- IC 2024.3 scenarios 1–7 and 9–13 passed. Tab reorder could not be exercised
  because it is unavailable in that IDE version/configuration.
- GoLand 2026.2.1 scenarios 1–13 passed, including tab reorder and the native
  CLI submenus.
- Both required visual checks are complete; residual review findings remain to
  be assessed before archive or publication.
- Split-session CLI submenus now reuse the same installed-CLI discovery as the
  regular New Session button; unavailable CLIs are hidden with a one-second cache.
- Scenario 3 functional split was validated on build 243, but centering the CLI
  picker was rejected as a UX failure. The asynchronous second popup was removed:
  each New Session in Split entry is now a native submenu containing the Claude
  Code and Codex choices, so selection stays attached to the original Split
  Sessions menu. The final submenu behavior passed visual validation on build 243.

## Learnings

- Tool Window title actions must resolve the focused nested ContentManager and
  execute native actions with that manager's selected Content component.
- Panel-local terminal and toolbar commands must use a bound session ID; the
  global active session is only the last-focused target for editor actions.
- Moving Content with `dispose=false` is compatible with disposer-only session teardown.

## Global Diff Follow-up

- Added a serialized global interaction coordinator.
- Sequential sessions retain separate numbered entries and Chat labels.
- Overlapping sessions share one baseline and produce one Multiple chats entry.
- Opening additional sessions no longer resets an active project interaction.
- Startup output, tab selection, toolbar Refresh, and Show Agent Changes no longer
  append numbered history entries.
- Closing an active session queues completion without blocking the EDT.
- Unexpected process death also releases its interaction participation, preventing
  a concurrent group from remaining open indefinitely.
- Automated validation: 179 tests passed on builds 243 and 262; baseline
  `buildPlugin` passed and `git diff --check` passed.
