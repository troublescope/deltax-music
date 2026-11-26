# Workflow

## Methodology
Context-Driven Development (`Context -> Spec & Plan -> Implement`) paired with repository rules in [AGENTS.md](../AGENTS.md).

## Implementation Rules
- Never hardcode user-facing strings in Kotlin; place English strings in `shared/src/commonMain/composeResources/values/strings.xml`.
- Never hardcode language or country fallbacks (e.g., `"en"`, `"US"`).
- Use `import` statements instead of inline fully qualified class names.
- Do not bump `appVersion` or commit/push without explicit user approval.

## Quality Gates

| Gate | Requirement |
|---|---|
| Gradle Config / Build | Android targets (`:androidApp`, `:shared`) configure and compile cleanly |
| Lint | `ktlintCheck` passes with zero errors |
| Form Factor Support | Both Android Smartphone (touch) and Android TV (`LEANBACK_LAUNCHER`, `LocalIsTv`) remain functional |
