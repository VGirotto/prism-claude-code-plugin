# Contributing to Prism — IDE Companion for Claude Code

Thank you for your interest in contributing. This document explains how to get started and what is expected from contributors.

## How to Contribute

1. Fork the repository at [github.com/VGirotto/prism-claude-code-plugin](https://github.com/VGirotto/prism-claude-code-plugin).
2. Create a feature branch from `main`:
   ```
   git checkout -b feat/your-feature-name
   ```
3. Make your changes, commit them (see [Commit Messages](#commit-messages)), and push to your fork.
4. Open a Pull Request against the `main` branch of the upstream repository.
5. Describe what the PR does, why it is needed, and how it was tested.

All PRs are reviewed before merging. Small, focused changes are preferred over large, wide-ranging ones.

## Development Setup

### Prerequisites

- **JDK 17 or later.** The bundled JBR that ships with any recent JetBrains IDE works well:
  ```
  export JAVA_HOME="/path/to/IDE.app/Contents/jbr/Contents/Home"
  ```
- **IntelliJ IDEA 2024.3+** (Community or Ultimate) is recommended for development.
- The IntelliJ Platform Plugin SDK 2.x and Gradle are managed automatically by the project wrappers — no separate installation is required.

### Clone and Build

```bash
git clone https://github.com/VGirotto/prism-claude-code-plugin.git
cd prism-claude-code-plugin
./gradlew build
```

### Key Gradle Tasks

| Task | Description |
|------|-------------|
| `./gradlew build` | Compile and assemble the project |
| `./gradlew test` | Run the test suite |
| `./gradlew buildPlugin` | Produce the distributable `.zip` artifact under `build/distributions/` |
| `./gradlew runIde` | Launch a sandboxed IDE instance with the plugin loaded |
| `./gradlew verifyPlugin` | Run IntelliJ Plugin Verifier checks against the declared IDE range |

## Testing the Plugin

The primary way to test is with the Gradle sandbox:

```bash
./gradlew runIde
```

This launches a fresh IDE instance with the plugin installed. No changes to your main IDE installation are made.

You can also install a built artifact manually:

1. Run `./gradlew buildPlugin` to produce the `.zip` file.
2. In your IDE go to **Settings > Plugins > Install Plugin from Disk** and select the generated `.zip`.

Always run `./gradlew test` and `./gradlew verifyPlugin` before submitting a PR.

## Code Style

- Follow the conventions already present in the codebase.
- Prefer idiomatic Kotlin: use `when` expressions, extension functions, data classes, and null-safety operators where appropriate.
- Avoid unnecessary abstraction. Keep changes easy to read and review.
- Do not introduce new external dependencies without discussing it in an issue first.

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/) style:

```
<type>(<scope>): <short description>
```

Common types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.

Examples:
```
feat(terminal): add auto-scroll toggle to tool window
fix(cli): handle missing PATH entry on macOS
docs: update setup instructions for JDK 17
```

Keep the subject line under 72 characters. Add a body if the motivation or context is not obvious from the subject alone.

## License

By contributing you agree that your code will be distributed under the [Apache License 2.0](LICENSE), the same license that covers this project.
