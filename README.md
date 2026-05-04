# SpendLens

Track money. Understand behavior.

SpendLens is an Android budgeting app built in Java for people who want more than a plain expense log. It combines expense tracking, monthly budgeting, statement import, lock protection, on-device insights, and portfolio-ready release infrastructure in one project.

Live project page:
`https://hassanawan7890.github.io/Spendlens/`

## App previews

<p align="center">
  <img src="docs/images/dashboard-preview.png" alt="SpendLens dashboard preview" width="30%" />
  <img src="docs/images/import-preview.png" alt="SpendLens statement import preview" width="30%" />
  <img src="docs/images/copilot-preview.png" alt="SpendLens budget copilot preview" width="30%" />
</p>

## Why SpendLens

SpendLens started from a simple observation carried over from the original course presentation: people usually know they are overspending, but they do not always know why. The current version keeps that behavior-first idea and expands it into a stronger local-first Android product with import review, export, widget support, reporting, and on-device budget guidance.

## What it does

- Add, edit, delete, and review expenses with mood tags, payment method, date, notes, and categories
- Track live monthly progress with dashboard stats, remaining budget, and pace-based risk feedback
- Import bank CSV statements, review detected transactions, recategorize them, and add only what you want
- Generate monthly snapshots for budget history and month-over-month analysis
- Surface hidden leaks, spending personality, reflection summaries, and overspending flags
- Ask a local on-device budget copilot questions about overspending, savings, and category leaks
- Protect the app with PIN or password lock
- Show key budget numbers and budget pace on an Android home screen widget
- Export the full expense ledger to CSV from Settings
- Manage categories safely, including reassignment to `Others` when a custom category is deleted
- Use swipe-to-delete with undo in expense history
- Run a bundled on-device Gemma model for local statement help and budget copilot

## Tech stack

- Java
- Android SDK, Material Components, ViewBinding
- MVVM with Activities, ViewModels, Repositories, and Room DAOs
- Room / SQLite for local persistence
- LiveData for reactive UI updates
- Google MediaPipe GenAI for on-device Gemma-compatible `.task` models
- JUnit 4 for unit tests
- GitHub Actions for CI
- GitHub Pages for a project showcase site

## Current snapshot

- 83 Java classes across activities, repositories, adapters, utilities, AI helpers, and widget support
- 20 Android activities covering onboarding, dashboard, history, reports, imports, settings, and AI flows
- 7 Room-backed tables for expenses, categories, profile, snapshots, and statement imports
- 37 XML layouts including screens, rows, dialogs, bottom sheets, and the home screen widget
- 102 JVM unit tests across budgeting, parsing, security, validation, and utility logic

## Project highlights

- Local-first app with no server dependency
- Statement import preview flow with selective confirmation
- Home screen widget for quick remaining-budget checks
- Built-in app lock for personal finance privacy
- On-device budget copilot and AI-assisted import review
- Budget alerts at 80 percent and 100 percent thresholds
- Snapshot-based reports and analysis engine
- CSV export for portability and demo-friendly sharing
- GitHub-ready repo with CI and a static landing page in `docs/`

## Feature coverage

- Dashboard, expense history, add/edit flows, and category management
- Statement import, selective confirmation, CSV export, and backup-friendly data portability
- Reports, monthly snapshots, hidden leak detection, and spending behavior insights
- App lock, home screen widget, and portfolio-ready GitHub Pages plus CI setup
- On-device AI for finance-only guidance and smarter statement handling

## Run it locally

1. Open the project in Android Studio.
2. Set the Gradle JDK to `C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot`.
3. Let Gradle sync completely.
4. Pick an emulator or Android phone with enough storage for a large debug build.
5. Press `Run`.
6. On first launch, SpendLens will auto-configure the bundled Gemma model.
7. The first time you use an AI feature, SpendLens will extract that bundled model into app storage and run it locally.

CLI checks:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

On this workspace, the debug APK builds successfully at:

- `app/build/outputs/apk/debug/app-debug.apk`

Notes:

- The bundled model makes the local app much heavier, so first install and first AI use may take a little longer.
- The model file is intentionally ignored in git because it is too large for a normal repo workflow. It is bundled locally on this machine for Android Studio runs.

## GitHub-ready setup

Because SpendLens is a native Android app, there is no true browser-playable live demo link in the same way a web app would have one. The best public setup is:

- Live project page: `https://hassanawan7890.github.io/Spendlens/`
- GitHub Pages for a polished showcase page
- GitHub Releases for the installable APK
- README previews and feature walkthroughs for first impressions

This repo now includes:

- `.github/workflows/android-ci.yml`
  Runs unit tests, builds the debug APK, and uploads the APK as an artifact.
- `.github/workflows/deploy-pages.yml`
  Deploys the `docs/` site to GitHub Pages.
- `docs/`
  A public-facing landing page now published as the live project link for the repo.

To publish the live site after pushing:

1. Push the repository to GitHub.
2. Enable GitHub Actions.
3. Enable GitHub Pages for the repository.
4. Let the `Deploy Pages` workflow publish the `docs/` folder.
5. Create a GitHub Release and attach the APK if you want visitors to install it directly.

## Testing

The project includes JVM unit tests for utilities and core logic, including:

- budget calculations
- currency parsing and formatting
- app lock hashing and verification
- on-device finance scope checks
- date utilities
- validation helpers
- CSV export formatting

## Structure

```text
app/
  src/main/java/com/spendlens/app/
    activities/
    adapters/
    dao/
    database/
    entities/
    fragments/
    insights/
    notifications/
    repository/
    utils/
    viewmodels/
  src/main/res/
.github/workflows/
docs/
```

## Status

SpendLens builds successfully, its unit tests pass, the GitHub Pages showcase is live, and the repo is prepared for CI-based APK generation.
