# CLAUDE.md

This file provides guidance to Claude when working with code in this repository.

## Project Overview

Mihon is an Android manga/comic reader app (fork of Tachiyomi). It uses Jetpack Compose for UI, Kotlin coroutines/Flow for async, SQLDelight for the database, Voyager for navigation, and a custom Injekt fork for dependency injection.

## Build Commands

- **Build debug APK**: `./gradlew assembleDebug`
- **Build release APK**: `./gradlew assembleRelease`
- **Lint check**: `./gradlew spotlessCheck`
- **Lint apply**: `./gradlew spotlessApply` (auto-fixes formatting)
- **Run unit tests**: `./gradlew test`
- **Run single test class**: `./gradlew test --tests "FullClassName"`
- **Run single test method**: `./gradlew test --tests "FullClassName.testMethodName"`

> Note: Git must be in PATH for builds to work (used in `buildSrc/src/main/kotlin/mihon/buildlogic/Commands.kt`).

## Code Style

- **Max line length**: 120 characters
- **Indentation**: 4 spaces (Kotlin), 2 spaces (XML/other files)
- **Kotlin style**: `intellij_idea` via ktlint (configured in `.editorconfig`)
- **Disabled ktlint rules**: class-signature, comment-wrapping, discouraged-comment-location, function-expression-body, function-signature, type-argument-comment, type-parameter-comment, blank-line-between-when-conditions
- Always run `spotlessApply` before committing to avoid CI failures

## Module Structure

```
app/               - Main Android app module
core/              - Shared utilities (archive, common)
core-metadata/     - Metadata handling
data/              - Data layer (repositories, mappers)
domain/            - Domain models and use cases
i18n/              - Localization strings (moko-resources)
i18n-sy/           - SY-specific localization
presentation-core/ - Shared Compose UI components
presentation-widget/ - Home screen widget
source-api/        - Source extension API
source-local/      - Local source implementation
telemetry/         - Analytics/crash reporting
```

## Package Naming Conventions

- `mihon.*` — Mihon-specific features (new code goes here)
- `eu.kanade.tachiyomi.*` — Original Tachiyomi code (legacy)
- `exh.*` — ExHentai extensions and related code
- `tachiyomi.*` — Domain and core library code

## Key Source Paths

- **App entry**: `app/src/main/java/eu/kanade/tachiyomi/App.kt`
- **DI setup**: `app/src/main/java/eu/kanade/tachiyomi/di/`
- **Presentation (Compose screens)**: `app/src/main/java/eu/kanade/presentation/`
- **Domain models/use cases**: `domain/src/main/java/tachiyomi/domain/`
- **Data repositories**: `data/src/main/java/tachiyomi/data/`
- **Strings**: `i18n/src/commonMain/moko-resources/base/strings.xml`
- **Build logic**: `buildSrc/src/main/kotlin/mihon/buildlogic/`

## Architecture

- **UI**: Jetpack Compose with Voyager navigation
- **DI**: Custom Injekt fork (`com.github.mihonapp:injekt`)
- **Database**: SQLDelight
- **Images**: Coil 3
- **Async**: Kotlin coroutines + Flow
- **Pattern**: Clean architecture — domain → data → presentation

## Build Variants

- `debug` — Dev build with `.dev` app ID suffix
- `release` — Production build with ProGuard/R8
- `foss` — FOSS variant without Firebase/Google services

## Testing

- **Framework**: JUnit 5 (Jupiter) with Kotest assertions and MockK
- **Test location**: `src/test/java/` within each module
- **Instrumented tests**: `AndroidJUnitRunner`

## Localization

- Strings live in `i18n/src/commonMain/moko-resources/base/strings.xml`
- Do not translate directly — translations are managed via Weblate
- Add new string keys to `base/strings.xml` only; translators handle the rest

## Common Patterns

- New features belong in `mihon.*` packages, not `eu.kanade.*`
- Use `Flow` for reactive data; avoid `LiveData`
- Prefer `StateFlow`/`SharedFlow` over `BroadcastChannel`
- Screen-level state lives in a `ScreenModel` (Voyager's ViewModel equivalent)
- Use `UiState` sealed classes for screen state representation
