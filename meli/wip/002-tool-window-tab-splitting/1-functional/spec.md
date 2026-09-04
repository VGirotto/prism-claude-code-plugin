# Tool Window Tab Splitting - Functional Spec

**Status**: approved
**Owner**: VGirotto
**Created**: 2026-09-04
**Last Updated**: 2026-09-04

## Problem Statement

Prism supports concurrent Claude Code and Codex sessions, but only one session
tab is visible at a time. Users comparing work or coordinating independent
agents must repeatedly switch tabs and cannot monitor both conversations.

## Objectives

1. Display independent Prism sessions side by side or stacked within the same
   tool window.
2. Preserve the process, PTY, terminal state, and tab identity while a session
   is split, moved, reordered, or unsplit.
3. Route every panel-local command to the session that owns that panel.
4. Keep the plugin operational from JetBrains build 243 onward while using the
   newest split capability available at runtime.

## Scope

### In Scope

- Split and move the selected session to the right or bottom.
- Rejoin split groups with Unsplit.
- Create a new independent session and place it directly in a right or bottom split.
- Create regular new sessions in the currently focused split group.
- Resolve the globally active session from the last focused Prism session.
- Release the feature as Prism 1.4.0.

### Out of Scope

- Rendering one terminal widget or PTY in two panes simultaneously.
- Persisting the split tree across IDE restarts in this release.
- Detached Prism windows or splits outside the Prism tool window.
- Fury deployment or LTP tests; Prism is a local JetBrains plugin.

## User Stories

### US-1: Split an existing session

**As a** Prism user with multiple agent sessions
**I want** to move the selected session into a right or bottom split
**So that** I can view and interact with sessions simultaneously.

**Acceptance Criteria**:
- [ ] Split Right and Split Down are available when a session can be split.
- [ ] The selected Content moves to the new group without restarting its agent.
- [ ] The same session ID and terminal state survive the move.

**Priority**: High
**Complexity**: L

### US-2: Create a session in a split

**As a** Prism user with one visible session
**I want** to create another session directly in a right or bottom split
**So that** I can begin side-by-side work in one action.

**Acceptance Criteria**:
- [ ] The user can choose the installed CLI through the existing picker.
- [ ] The new session is independent and becomes visible in the requested split.
- [ ] A failed or cancelled CLI selection leaves the existing layout unchanged.

**Priority**: High
**Complexity**: M

### US-6: Keep one reliable project-wide diff timeline

**As a** Prism user running multiple sessions
**I want** sequential work attributed to its chat and overlapping work grouped once
**So that** interaction numbers remain unique without duplicated or hidden changes.

**Acceptance Criteria**:
- [ ] Sequential chats create consecutive Diff entries attributed to each chat.
- [ ] Overlapping chats create one Diff entry labeled Multiple chats.
- [ ] Opening another session never resets an interaction already in progress.
- [ ] Startup output, tab selection, and Refresh never create a numbered interaction.

**Priority**: High
**Complexity**: L

### US-3: Send commands to the owning session

**As a** user working in multiple visible panes
**I want** terminal shortcuts and toolbar commands to target their own pane
**So that** commands never reach another agent accidentally.

**Acceptance Criteria**:
- [ ] Toolbar, templates, model, effort, cost, resume, compact, clear, Escape,
      Shift+Enter, control shortcuts, and smart paste use the owning session ID.
- [ ] A sequence in flight disables only the toolbar of its own session.
- [ ] Editor context actions target the last Prism session that received focus.

**Priority**: High
**Complexity**: L

### US-4: Preserve lifecycle semantics

**As a** Prism user reorganizing panes
**I want** split operations to preserve live sessions and real closes to stop them
**So that** no agent freezes, leaks, or terminates unexpectedly.

**Acceptance Criteria**:
- [ ] Split, move, reorder, and unsplit never dispose the session Content.
- [ ] Closing a session destroys exactly that session once.
- [ ] Closing during startup cannot attach a terminal after disposal or leave an orphan process.
- [ ] Closing one split preserves sessions in the other split.

**Priority**: High
**Complexity**: L

### US-5: Work across supported JetBrains versions

**As a** Prism user on any supported IDE
**I want** split support appropriate to my IDE version
**So that** upgrading Prism does not remove build 243 compatibility.

**Acceptance Criteria**:
- [ ] The artifact builds and verifies with `sinceBuild=243`.
- [ ] Newer IDEs use the current public ToolWindow splitting capability.
- [ ] Older supported IDEs use an isolated compatibility strategy with a safe no-op fallback.
- [ ] An unavailable split capability never prevents Prism from opening or running sessions.

**Priority**: High
**Complexity**: M

## Business Rules and Invariants

| ID | Rule |
|---|---|
| BR-1 | A live session has exactly one Content, terminal widget, connector, and PTY. |
| BR-2 | Splitting moves Content; it never clones a live session view. |
| BR-3 | Panel-local actions use a bound session ID; global editor actions use last focus. |
| BR-4 | Only actual Content disposal destroys a session. Temporary removal never does. |
| BR-5 | Capability selection is based on runtime availability, not an upper IDE version. |
| BR-6 | Diff numbering is global; only completion of a real interaction group consumes a number. |
| BR-7 | One participant is labeled by chat; overlapping participants are labeled Multiple chats. |

## User Experience

- Existing split actions follow JetBrains naming and icons where available.
- Regular New Session opens in the focused group.
- New Session in Right Split and New Session in Bottom Split create a fresh agent.
- Unsplit reunites groups without changing session processes.
- Unsupported capability failures are logged and degrade to ordinary single-tab behavior.
- Every pane shows the same project-wide Diff timeline; only concurrent entries use Multiple chats.

## Non-Functional Requirements

### Performance
- Split and unsplit must not spawn, restart, or reconnect existing processes.
- UI layout work runs on the EDT; CLI discovery and process startup remain off the EDT.

### Security and Safety
- No new network calls, credentials, shell evaluation, or external data storage.
- Commands must never cross session boundaries because of focus changes.
- Compatibility reflection is constrained to fixed JetBrains class and method names.

### Compatibility
- Minimum supported build remains 243.
- Validation targets include IC 2024.3 and GoLand 2026.2.1/build 262.

## Critical E2E Test Scenarios

These are local IDE sandbox scenarios, not LTP scenarios.

### E2E-1: Split and interact with two sessions

**Preconditions**: Two live Prism sessions.

1. Split one session to the right and send distinct input through each toolbar.
2. Move the session to a bottom split and then unsplit it.

**Expected Result**: Both PTYs remain alive and each input reaches only its own session.

### E2E-2: Create a new session in a split

**Preconditions**: One live Prism session and at least one installed agent CLI.

1. Create a new session in a right split.
2. Repeat with a bottom split.

**Expected Result**: A new independent process appears in the requested group without
restarting the existing process.

### E2E-3: Close and startup lifecycle

1. Close one side of a split while the other session is live.
2. Close a newly requested session while its process is starting.

**Expected Result**: Only the closed session is destroyed, exactly once, and no terminal
attaches after disposal.

## Success Metrics

- All routing and lifecycle unit tests pass.
- Manual split matrix passes in both baseline and modern IDE sandboxes.
- Plugin Verifier reports no blocking compatibility problem.
- No session process is restarted during split or unsplit.

## Dependencies

- IntelliJ Platform ToolWindow and ContentManager APIs.
- Existing Prism session, terminal, toolbar, and DiffPanel components.
- No external service or new library dependency.

## Risks

| ID | Risk | Mitigation |
|---|---|---|
| RISK-1 | Legacy split APIs change or disappear. | Isolate lookup behind an adapter and fail closed to single-pane mode. |
| RISK-2 | Commands reach the wrong visible session. | Bind panel actions to session IDs and test against a different global active ID. |
| RISK-3 | Moving Content triggers teardown. | Retain disposer-only teardown and validate identity across moves. |
| RISK-4 | Startup close races with terminal attachment. | Use synchronized lifecycle state and revalidate on the EDT. |

## Open Questions

None blocking. Split layout persistence is intentionally deferred.
