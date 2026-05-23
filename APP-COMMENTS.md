# APP-COMMENTS.md — Documentation Standards

> **Scope:** Kotlin source files in this project. These rules apply to all new code and should be followed when editing existing code.

---

## 1. Philosophy

- Comments explain **why** something is done and **what side effects** result — not just _what_ the code does.
- Good documentation is a contract: it makes behaviour predictable for future contributors (human or AI).
- Prefer clear naming over commenting the obvious. A well-named function needs fewer words.

---

## 2. KDoc Rules (`/** ... */`)

### 2.1 When KDoc Is Required

Add a KDoc block to **every non-trivial function**, regardless of visibility:

| Visibility | KDoc required? |
|------------|----------------|
| `public` | ✅ Always |
| `internal` | ✅ Always |
| `private` (non-trivial) | ✅ Yes |
| `private` (trivial — see §3) | ❌ Optional |

Also required on:
- Every **class** and **interface**
- Every **data class** with non-obvious fields
- Every **object** that holds shared state or configuration

### 2.2 Required Tags

| Tag | Use when |
|-----|----------|
| `@param name` | Every parameter whose purpose isn't fully obvious from its type and name |
| `@return` | When the return value has nuances beyond what the type signature conveys |
| `@throws ExceptionType` | When the function can throw a checked or documented exception |

### 2.3 Formatting Rules

- The **first line** is a concise one-sentence summary. No period required.
- Expand in subsequent paragraphs for side effects, state changes, or threading notes.
- Use Kotlin square-bracket syntax to cross-reference types and properties: `[ClassName]`, `[property]`, `[OtherFunction]`.
- Do **not** include `@author` or `@since` — git blame covers authorship.

### 2.4 Class-Level KDocs

Class and interface KDocs must cover:
1. The **responsibility** of the class (single sentence).
2. Any **lifecycle or threading** constraints (e.g., "Must be called on the Main thread").
3. Key **exposed state** (for ViewModels: list the `StateFlow` and any `SharedFlow`/`Channel`).

```kotlin
/**
 * ViewModel for the Notecard screen.
 *
 * Exposes [uiState] as a [StateFlow] of [NotecardScreenState] for the UI to collect.
 * Navigation and one-off snackbar events are emitted via [eventChannel].
 *
 * All repository calls run on [Dispatchers.IO]; state updates are posted back to Main.
 *
 * @param repository Source of truth for notecard sets and cards.
 */
@HiltViewModel
class NotecardViewModel @Inject constructor(
    private val repository: NoteCardRepository
) : ViewModel() { ... }
```

---

## 3. Trivial Functions — KDoc Optional

Skip KDoc (or keep it to a one-liner) for:

| Case | Example |
|------|---------|
| Single-expression getter/setter | `fun name() = _name` |
| `override` whose interface/parent already has a full KDoc | `override fun onCleared() { ... }` |
| `@Preview` composable stubs | `@Preview @Composable fun MyScreenPreview() { ... }` |
| Data-class `copy()` wrappers that only delegate | `fun withName(n: String) = copy(name = n)` |
| Hilt `@Provides` / `@Binds` one-liners with obvious return types | `@Provides fun provideDb(...): AppDatabase` |
| Lambda-only event handlers inline in a Composable | `onClick = { viewModel.onCardFlipped() }` |

When in doubt, write the KDoc. Over-documenting is a much smaller sin than under-documenting.

---

## 4. Inline Comment Rules (`//`)

### 4.1 When to Add Inline Comments

Add `//` comments **inside** a function body when:

- The body is **longer than ~10 lines** of non-trivial logic — use section comments to break it up.
- The code involves **non-obvious math or algorithms** (e.g., animation camera-distance calculations, bit operations, index arithmetic).
- A `when` / `if` branch encodes a **business rule** rather than a type check.
- A **coroutine dispatcher** choice isn't obvious from context (explain why `IO` vs `Default`).
- You intentionally **work around a platform quirk** or known Android bug.
- A seemingly redundant call is **intentional** (prevents a subtle bug).

### 4.2 Style Rules

- Keep each inline comment to **one line** where possible.
- Place the comment **above** the relevant line, not trailing it (trailing is OK for short labels).
- Write in plain English — no abbreviations, no filler words.
- Do **not** state the obvious:

  ```kotlin
  // ❌ Bad — restates what the code says
  i++ // increment i

  // ✅ Good — explains why
  // Skip the header row; it is always index 0 regardless of sort order
  val dataRows = rows.drop(1)
  ```

### 4.3 Section Comments in Long Functions

For functions with distinct logical phases, use a short comment header per phase:

```kotlin
fun processQuizResult(answer: String) {
    // 1. Normalise input before comparison
    val cleaned = answer.trim().lowercase()

    // 2. Check answer against current card
    val isCorrect = currentCard?.back?.lowercase() == cleaned

    // 3. Update streak and advance to next card
    _uiState.update { it.copy(streak = if (isCorrect) it.streak + 1 else 0) }
    advanceCard()
}
```

---

## 5. Composable-Specific Rules

### KDoc
- Document every **state parameter** and every **lambda callback** with `@param`.
- Note if the composable is **stateless** (all state passed in) or manages its own remembered state.
- Note any important **side effects** (e.g., launches a coroutine, triggers a system permission).

```kotlin
/**
 * Flip card that shows [front] content and animates to reveal [back] on tap.
 *
 * Manages its own flip state internally via [rememberSaveable].
 *
 * @param modifier Modifier applied to the outermost card surface.
 * @param front Content shown on the front face.
 * @param back Content shown on the back face after flipping.
 * @param onFlipped Called after the flip animation completes, with `true` = showing back.
 */
@Composable
fun FlipCard(
    modifier: Modifier = Modifier,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    onFlipped: (Boolean) -> Unit = {}
) { ... }
```

### Inline Comments
- Use them for **animation state derivations** (e.g., which face is visible based on rotation angle).
- Use them for **rendering conditions** that depend on calculated values rather than direct state booleans.

---

## 6. ViewModel-Specific Rules

- The class KDoc must **list** `uiState: StateFlow<...>` and any event channels/flows.
- Each **public function** that mutates state must note in its KDoc which state property is updated.
- Use inline comments when launching coroutines with a non-default dispatcher or error strategy.

```kotlin
/**
 * Loads all cards for [setId] from the repository and makes that set the active one.
 *
 * Resets [NotecardScreenState.shownIndex] to `0` (or `null` for an empty set) so the
 * user always starts at the first card after switching sets.
 * Updates [uiState].
 *
 * @param setId The Room primary key of the set to activate.
 */
fun selectSet(setId: Long) {
    viewModelScope.launch {
        // IO dispatcher — repository performs a suspend DB query
        val cards = withContext(Dispatchers.IO) { repository.getCardsForSet(setId) }
        _uiState.update { it.copy(cards = cards, shownIndex = if (cards.isEmpty()) null else 0) }
    }
}
```

---

## 7. Repository / Data Layer Rules

- **Interface functions** always have KDocs — they are the public contract.
- **Impl overrides** may omit KDoc only when:
  - The interface KDoc is complete, **and**
  - The implementation adds no additional behaviour, threading, or error-handling nuance.
- DAO functions must document any conflict strategies (`OnConflictStrategy`) and why they were chosen.

```kotlin
// Repository interface — always documented
/**
 * Returns a [Flow] that emits the full ordered list of all note-card sets
 * whenever the underlying database changes.
 */
fun getAllSets(): Flow<List<NoteCardSetEntity>>

// Impl override — KDoc omitted because interface doc is complete and impl is a direct delegate
override fun getAllSets(): Flow<List<NoteCardSetEntity>> = dao.observeAllSets()
```

---

## 8. Quick Cheat-Sheet

### ViewModel function
```kotlin
/**
 * One-sentence summary of what this does and what state it changes.
 *
 * Additional notes about side effects, threading, or ordering constraints.
 *
 * @param foo Description if not obvious from type/name.
 * @return Description if return value has nuance.
 */
fun doSomething(foo: String) { ... }
```

### Composable
```kotlin
/**
 * One-sentence summary. Note if stateless vs stateful.
 *
 * @param modifier Applied to the root layout element.
 * @param onAction Callback invoked when the user performs the primary action.
 */
@Composable
fun MyWidget(
    modifier: Modifier = Modifier,
    onAction: () -> Unit
) { ... }
```

### Repository interface
```kotlin
/**
 * Fetches [Entity] by [id]. Returns `null` if no record exists.
 *
 * @param id Primary key.
 * @return The matching entity, or `null`.
 */
suspend fun getById(id: Long): Entity?
```

### Private helper with inline comments
```kotlin
private fun buildSortedIndex(items: List<Card>): Map<String, Int> {
    // Sort by tag then title so binary-search assumptions hold downstream
    val sorted = items.sortedWith(compareBy({ it.tag }, { it.title }))

    // Build index mapping title -> position for O(log n) lookup
    return sorted.mapIndexed { i, card -> card.title to i }.toMap()
}
```

---

## 9. Anti-Patterns to Avoid

| Anti-pattern | Why it's harmful |
|--------------|-----------------|
| `// TODO` with no ticket number | Orphaned intent — will never be acted on |
| Commented-out code blocks | Pollutes history; use git if you need to revert |
| Restating the function name in the KDoc | Adds zero information |
| `@param x The x value` | Circular — describe purpose, not the name |
| Wall-of-text KDoc without paragraph breaks | Hard to skim; use blank lines between sections |
| KDoc on every trivial override | Noise that buries real documentation |

---

*Last updated: 2026-05-22*
