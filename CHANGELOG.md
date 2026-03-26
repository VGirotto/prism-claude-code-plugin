# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] — 2026-03-26

### Fixed

- **History**: fix project path escaping for directories containing underscores (e.g. `my_cool-project`). Claude Code replaces both `/` and `_` with `-`, but the plugin only replaced `/`. Added multi-strategy resolution with fuzzy fallback.

### Changed

- **Icon**: new minimalist diamond outline icon replacing the old "C" letter badge.

## [1.0.0] — Unreleased

### Added

**Terminal & Process Management**
- Interactive terminal with Claude Code CLI integrated into the IDE
- Full ANSI color and text formatting support
- Real PTY (pty4j + JediTerm) for maximum compatibility
- Multi-session: multiple independent sessions in simultaneous tabs
- Auto-start Claude when opening a project (configurable)

**Diff View & Change Tracking**
- Claude Changes panel: visualize files modified per interaction
- Native IDE side-by-side diff (original vs. modified)
- Incremental snapshots on disk (zero RAM overhead for large repositories)
- Revert by file or by complete interaction
- History navigation between interactions (previous / next)
- Automatic refresh after Claude finishes
- "Clear Interactions" button with confirmation and cross-panel synchronization

**IDE Integration & Context**
- Send Selection: send selected text to Claude
- Insert File Reference: insert @path into the terminal
- Context menu actions: Explain / Review / Fix / Generate Tests / Refactor
- Auto-capture of context (active file, selection, open files)
- Customizable keyboard shortcuts (Cmd/Alt on macOS, Ctrl/Alt on Linux)

**Toolbar & Productivity**
- Compact toolbar with quick-action buttons
- Dropdowns: Model (opus/sonnet/haiku), Effort (auto/low/medium/high/max), Cost
- Buttons: Compact, Resume, Templates
- Prompt Templates: reusable with variables {selection}, {file}, {language}

**Settings & Configuration**
- Appearance: toggle Changes panel on startup, Status Bar widget
- Snapshot: exclusion patterns, maximum file size
- Configurable Claude path, shell, and auto-start
- Language: English, Portuguese, Spanish (selectable in settings)

**History & Sessions**
- Conversation History browser: navigate previous conversations
- Full-text search across history
- Support for multiple parallel sessions with independent state
- History view with native IDE formatting

**Status Visibility**
- Status Bar widget: shows Claude state (working / idle / stopped)
- Model and effort visible in real time
- Click the widget to open the Claude panel
- Multi-session support: [2/4 working]

### Technical

- **Language**: Kotlin + Gradle Kotlin DSL
- **Platform**: IntelliJ Platform Plugin 2.x, IDE 2024.3+
- **Runtime**: JDK 17+
- **Testing**: 34+ unit tests covering the main services
- **CI/CD**: GitHub Actions ready for build, test, verify, and release
