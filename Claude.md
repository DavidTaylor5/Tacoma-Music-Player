# Project Context: Android Application

## 🎯 Tech Stack & Architecture
* **Language:** Kotlin 2.x (Strictly use modern idioms, trailing lambdas, and type-safe builders)
* **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel)
* **UI Framework:** Jetpack Compose (100% declarative UI, no XML layouts)
* **Asynchronous Flow:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Dependency Injection:** Dagger Hilt
* **Networking:** Retrofit (using `kotlinx.serialization` for JSON)
* **Local Database:** Room
* **Build System:** Gradle Kotlin DSL (`.gradle.kts`) with Version Catalogs (`libs.versions.toml`)

---

## 🛠️ Jetpack Compose & UI Guidelines
* **State Management:** Always use `rememberSaveable` for simple state or hoist state to the ViewModel. Prefer `collectAsStateWithLifecycle()` over `collectAsState()` to remain lifecycle-aware.
* **Recomposition:** Keep Composables performance-optimized. Mark stable data classes with `@Stable` or `@Immutable` if they come from external modules.
* **Previews:** Provide `@Preview` annotations with dummy data for UI testing. Use custom design system tokens (Colors, Typography, Shapes) rather than hardcoded values.
* **Modifiers:** Pass a default `modifier: Modifier = Modifier` as the first optional parameter to all reusable Composables.

---

## 🚦 Concurrency & Architecture Rules
* **Scopes:** Never use `GlobalScope`. Use `viewModelScope` in ViewModels and `lifecycleScope` or `rememberCoroutineScope()` in views.
* **Dispatchers:** Always inject or explicitly set dispatchers (`Dispatchers.IO` for network/disk, `Dispatchers.Default` for heavy computation, `Dispatchers.Main` for UI changes).
* **Unidirectional Data Flow (UDF):** 
  * ViewModels expose exactly one `StateFlow` for UI State and a `SharedFlow` or `Channel` for one-off events (e.g., snackbars, navigation).
  * Composables pass events *up* (as lambdas) and receive state *down*.

---

## 📦 Code Style & Quality Standards
Critical: Before writing any comments or kDocs, refer to the guide APP-COMMENTS.md.
* **Null Safety:** Leverage Kotlin's null safety fully. Avoid `!!` (double bang) assertions at all costs; use safe calls `?.`, the Elvis operator `?:`, or explicit error handling.
* **Extensions:** Use Kotlin extension functions to keep core classes clean and readable.
* **Testing:** 
  * **Unit Tests:** MockK / Turbine for Flow testing.
  * **UI Tests:** Compose UI Testing library (Semantics node matching).
---

## 🔍 Context-Aware Variables (Project Specifics)
* **Min SDK:** 26 (Android 8.0)
* **Target/Compile SDK:** 36

---

## Claude Setup
Critical: At first command, initialize session by reading DEV-GUIDE.md and FILE-DIRECTORY.md. Always check FILE-DIRECTORY to determine target files.