# CLAUDE.md

Read `ARCHITECTURE.md` first — it owns the tech stack, package boundaries and data-flow
rules. `DESIGN.md` owns the palette, type scale, components and the departures from the
handoff. This file covers what neither does: how not to break the UI that resulted.

## The screenshot goldens are the UI contract

`app/src/test/screenshots/*.png` are 12 checked-in reference images. `ThemeScreenshotTest`
renders the theme (colors, type, components, bubbles) and the four designed screens through
the specimens in `ScreenSpecimens.kt`, then compares against them.

```bash
./gradlew verifyRoborazziDebug   # check against the goldens — also runs all unit tests
./gradlew recordRoborazziDebug   # OVERWRITE the goldens — see below
```

**Default to `verify`. `record` rewrites the baseline.** A failing verify is the system
working: it means rendering changed. Re-recording to make it green bakes the regression in
as the new standard, and the diff shows only that some PNGs changed, which nobody reads.

Run `record` only when a UI change is *intended*, and then say so — the PNG churn in that
commit is a design change under review, not noise to wave through.

If verify fails, the comparison images under `app/build/outputs/roborazzi/` show which
pixels moved.

## Screenshots do not protect the data path

They catch rendering only. Replacing `topics.size` with a hardcoded 3 can leave the image
byte-identical. What guards the data path:

- `TopicsViewModelTest`, `ChatSessionTest`, `ChatPersistenceMigrationTest`, `TopicParsingTest`
- The rule below.

## No mock content in production code

The design handoff (`~/Downloads/design_handoff_english_tutor_ui/`) is a data-binding
template; its sample text exists only to render the preview images. Topic names, dates,
exchange counts, message bodies, model names and version strings must come from Room, a
ViewModel or `BuildConfig` — never a literal.

Sample sentences belong in `ScreenSpecimens.kt` (test source only), and are deliberately
written to differ from the handoff's own mock text.

## Use the design system

`DESIGN.md` has the palette, type scale and component inventory. The short version:
reuse what is in `ui/DesignSystem.kt` rather than rebuilding it, use
`painterResource(R.drawable.ic_*)` over `Icons.*`, and take colors from
`MaterialTheme.colorScheme` or the named tokens in `ui/theme/Color.kt` — never a raw hex.

`DESIGN.md` also lists five deliberate departures from the handoff and four accepted
accessibility trade-offs. Read that section before "fixing" any of them.

## Testing notes

- `verifyRoborazziDebug` covers the unit tests too; CI runs exactly that one command.
- Screenshot canvas is pinned by the Robolectric qualifier
  (`@Config(qualifiers = HandoffCanvasQualifier)` = 393dp, the handoff's own width).
  `Modifier.width` cannot do this: the capture is taken from `onRoot()`, the whole window.
- Any JDK from 17 to 25 works. An older note claiming Robolectric needs Java 21 is stale —
  the daemon runs on 25 and the suite passes.

## Repository hygiene

`.idea/` is tracked and churns on every IDE action (`deploymentTargetSelector.xml` even
stores an ADB endpoint). Never `git add -A`; stage the files you actually changed.
