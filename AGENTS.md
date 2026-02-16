# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build Commands

- **Build debug APK**: `./gradlew assembleDebug`
- **Build release APK**: `./gradlew assembleRelease`
- **Lint check**: `./gradlew spotlessCheck`
- **Lint apply**: `./gradlew spotlessApply` (auto-fixes)
- **Run unit tests**: `./gradlew test`
- **Run single test class**: `./gradlew test --tests "FullClassName"`
- **Run single test method**: `./gradlew test --tests "FullClassName.testMethodName"`

Note: Git must be installed and available in PATH for build commands to work (required for `getCommitCount`, `getGitSha`, `getBuildTime` in [`buildSrc/src/main/kotlin/mihon/buildlogic/Commands.kt`](buildSrc/src/main/kotlin/mihon/buildlogic/Commands.kt:9)).

## Code Style

- **Max line length**: 120 characters
- **Indentation**: 4 spaces for Kotlin, 2 spaces for most other files
- **ktlint style**: `intellij_idea` (configured in `.editorconfig`)
- **Disabled ktlint rules**: class-signature, comment-wrapping, discouraged-comment-location, function-expression-body, function-signature, type-argument-comment, type-parameter-comment, blank-line-between-when-conditions

## Package Naming Conventions

- `mihon.*` - Mihon-specific features (new code)
- `eu.kanade.tachiyomi.*` - Original Tachiyomi code (legacy)
- `exh.*` - ExHentai extensions and related code
- `tachiyomi.*` - Domain and core library code

## Testing

- **Framework**: JUnit 5 (Jupiter) with Kotest assertions and MockK
- **Test location**: `src/test/java/` within each module
- **Instrumented tests**: Use `AndroidJUnitRunner`

## Architecture

- **DI**: Custom Injekt fork (`com.github.mihonapp:injekt`)
- **Database**: SQLDelight
- **Navigation**: Voyager (Compose)
- **Images**: Coil 3

## Key Build Variants

- `debug` - Development build with `.dev` suffix
- `release` - Production build with ProGuard
- `foss` - FOSS variant without Firebase
