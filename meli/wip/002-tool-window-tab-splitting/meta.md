# Feature Meta

## Identification
feature_number: "002"
feature_id: "002-tool-window-tab-splitting"
feature_name: "tool-window-tab-splitting"
created_at: "2026-09-04T09:55:18-03:00"

## Context
description: |
  Add GoLand-style split views to the Prism tool window so independent agent
  sessions can be displayed side by side or stacked. The feature must keep the
  plugin compatible with JetBrains build 243 and newer while selecting the most
  current split capability available in each IDE version.

  Initial solution direction:
  - Move an existing session Content into a right or bottom split without
    duplicating its terminal widget or PTY.
  - Allow a new independent session to be created directly in a split.
  - Route terminal shortcuts and toolbar commands to the session that owns the
    focused view.
  - Preserve sessions across split, unsplit, tab movement, and tab reordering;
    dispose a session only when its Content is actually closed.
  - Prefer the current public JetBrains API where available and isolate the
    compatibility strategy required by older supported builds.

## Mode
project_mode: brownfield
project_type: production
execution_mode: express
template_mode: full
spec_language: en

## Testing
ltp:
  enabled: false
  decision_date: "2026-09-04"
  decision_reason: "Prism is a non-Fury JetBrains plugin; IDE sandbox tests provide end-to-end validation."

## Status
current_stage: implementation
phase_1_functional: approved
  approved_by: VGirotto
  approved_at: "2026-09-04T13:13:58Z"
phase_2_technical: approved
  approved_by: VGirotto
  approved_at: "2026-09-04T13:13:58Z"
phase_3_tasks: approved
  approved_by: VGirotto
  approved_at: "2026-09-04T13:13:58Z"
  strategy: batched
phase_4_implementation: validation-pending

## Brownfield Context
has_specs: true
specs_location: meli/specs/
affected_areas:
  - tool-window content and tab management
  - active session and focus routing
  - terminal and toolbar command targeting
  - session startup and disposal lifecycle
impact_level: high
breaking_changes: false

## Compatibility
minimum_supported_build: "243"
modern_validation_target: "262.9437.195"
policy: |
  Keep build 243 as the compatibility baseline. Use the newest supported public
  split API at runtime and contain any older-version compatibility mechanism
  behind a dedicated adapter with explicit fallback behavior.

## Framework
framework_version: "1.2.x"
profile: technical
plan_mode:
  fix_complex_bugs: true
  spec_technical_brownfield: true
  build_complex_tasks: false
