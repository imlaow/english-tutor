# Design system

What the app looks like and why. `CLAUDE.md` covers how not to break it;
`ARCHITECTURE.md` owns the code structure.

The look comes from an "organic" design handoff: a warm, low-contrast palette with a
display serif over a geometric sans, generous rounding, and soft ink-tinted shadows.

## Where the spec came from

`~/Downloads/design_handoff_english_tutor_ui/` — four screen drawings plus
`_ds/organic-*/styles.css`, which is the actual source of truth for tokens and
components.

**That directory is outside this repository and may not survive.** Everything below was
transcribed from it and cross-checked against the code, so this file is the durable copy.
If you need to settle a question the handoff would have answered and the directory is
gone, this file is what you have.

Two things to know about the handoff:

- Its four drawings are data-binding templates. Their sample text (`"Favorite mobile
  apps"`, `"Jul 23, 2026 16:09"`, `"Grok 4.3-AWS"`) is mock content and must never reach
  production code. See `CLAUDE.md`.
- Its stylesheet defines more than the drawings use — `.input`, `.radio`, `.seg` are
  specified but appear in none of the four screens. Those specs are still authoritative;
  the form controls follow them.

Its canvas is 393dp wide, which is why the screenshot specimens pin that width.

---

## Colour

Three ramps on one shared lightness scale, so the same step of any role matches the
others in visual value. Defined in `ui/theme/Color.kt`.

| | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 |
|---|---|---|---|---|---|---|---|---|---|
| **Neutral** | `F9F4ED` | `EEE7DB` | `DCD3C4` | `C0B6A5` | `A19786` | `82796A` | `645C50` | `474238` | `2E2B25` |
| **Accent** (terracotta) | `FFF2EB` | `FFE1D0` | `FFC6A5` | `F6A06B` | `D67F48` | `B2622D` | `8C491A` | `643312` | `402310` |
| **Accent-2** (olive) | `F0FAE1` | `E1EECC` | `CCDBB2` | `AEBF92` | `8FA073` | `728157` | `56633F` | `3D472B` | `272E1B` |

Plus four standalone values: background `F5EAD8`, surface `EBDDC5`, text `201E1D`, and
divider `201E1D` at 16% alpha.

### Material role mapping

The palette reaches screens through `MaterialTheme.colorScheme`, so stock components
inherit it. **Take colours from a role or a named token — never a raw hex.**

| Role | Token | Drawn as |
|---|---|---|
| `primary` / `onPrimary` | Accent500 / Neutral100 | mic button, focused field border |
| `primaryContainer` / `on…` | Accent200 / Accent700 | topic number badges, `Y` avatar |
| `secondary` / `onSecondary` | Accent2-500 / Neutral100 | |
| `secondaryContainer` / `on…` | Accent2-200 / Accent2-700 | `T` avatar, exchange pill |
| `tertiary` / `tertiaryContainer` | Accent600 / Accent100 | error card fill |
| `background` / `onBackground` | `F5EAD8` / `201E1D` | the page |
| `surface` / `onSurface` | Neutral100 / `201E1D` | cards, chat bubbles |
| `surfaceVariant` / `on…` | `EBDDC5` / Neutral700 | top bar, mic dock, field fill |
| `error` / `errorContainer` | Accent700 / Accent200 | see "no error colour" below |
| `outline` | Neutral600 | **control edges** — see below |
| `outlineVariant` | divider (16% ink) | **content rules** — see below |

### The two hairline roles are not interchangeable

The handoff draws every hairline in `--color-divider`. At 16% ink that is 1.37:1 against
a field fill — a border you have to hunt for. So the roles are split by what the line is
doing:

- **`outline`** bounds a control: text-field and segmented-group borders, the unselected
  radio rim. These owe 3:1 as UI component boundaries. Neutral600 is the first step on
  the ramp that pays it — 3.61:1 on the background, 3.92:1 on a card, 3.21:1 on the top
  bar.
- **`outlineVariant`** separates content: the rule between settings rows, the divider
  inside a segmented group. Keeps the handoff's value, since nothing there has to be
  found by touch.

### No error colour

The handoff has none. `error` borrows Accent700, a scorched orange that reads as a
warning without dropping a foreign red into the palette. The grammar callout does **not**
use it — that one is deliberately the olive "TRY SAYING" card, a suggestion rather than a
fault.

---

## Type

Caprasimo (display, one weight) over Figtree (400/600/700). Both bundled in `res/font/`.
Scale and metrics in `ui/theme/Type.kt`.

Headings are `lineHeight ×1.12` with `-0.015em` tracking; body is `×1.55` with none. The
`em` tracking is relative to size, so each step computes its own sp value.

| Role | Size | Face |
|---|---|---|
| `displayLarge` / `displayMedium` / `displaySmall` | 42 / 32 / 25 | Caprasimo |
| `headlineLarge` / `headlineMedium` / `headlineSmall` | 32 / 25 / 20 | Caprasimo |
| `titleLarge` | 23 | Caprasimo — top-bar titles |
| `titleMedium` | 20 | Caprasimo — chat title, empty-state heading |
| `titleSmall` | 16 semibold | **Figtree** — card and row titles |
| `bodyLarge` / `bodyMedium` / `bodySmall` | 15 / 14 / 13 | Figtree |
| `labelLarge` / `labelMedium` | 13 / 11 semibold | Figtree |
| `labelSmall` | 10 semibold, `.08em` | Figtree — the uppercase micro-labels |

`titleSmall` is the one role that breaks the display/body split: every 16sp in the
handoff is body weight, and Caprasimo at 16 is used nowhere.

Two named extras, since no Material role fits:

- `SectionKicker` — 11sp semibold at `.14em`, for `SUGGESTED FOR YOU` / `MODEL`.
  `labelMedium` is the same size but untracked, and is right for pills and counts.
- `FieldLabel` — 12sp, the block label above a text field.

**Every style trims `includeFontPadding` and centres within the line box.** Compose
otherwise adds uneven slack above and below every line that CSS has no equivalent for,
and each gap lands a few dp larger than drawn. Any new `TextStyle` must do the same or it
will not line up with its neighbours.

---

## Spacing, shape, elevation

Spacing steps: **4 / 9 / 13 / 18 / 26 / 35 dp** (the handoff's 4.4px scale). 18 is the
standard screen inset; 26 the gap under a top bar.

Shapes in `ui/theme/Shape.kt`: small 8, medium 16, large 28, **extraLarge 32**. The
handoff's rounded-frame pass pushes cards to 32 and makes buttons, tags, inputs and
segmented groups **pills** — `CircleShape`, not a fixed radius. On a 56dp field a pill is
a 28dp radius with no straight edge at all; 16dp would leave 24dp of flat side.

Shadows are ink-tinted and soft. Approximated as Compose elevation: **sm ≈ 2dp**
(cards, bubbles), **md ≈ 6dp** (chat mic button), **lg ≈ 16dp** (home mic button).

Icons are Lucide-style: 24 viewport, 2.75 stroke, round caps and joins, no fill. All 16
live in `res/drawable/ic_*.xml`. **Use `painterResource(R.drawable.ic_*)`, never
`Icons.*`** — the Material set is filled and reads as a different family.

---

## Components

`ui/DesignSystem.kt`. Reuse these rather than rebuilding; they are what keeps eight
screens looking like one app. None of them owns a string — all copy comes from the caller,
so it stays with the screen that owns the data behind it.

| Composable | What it is |
|---|---|
| `WarmTopBar` | The surface block behind every title. Bottom corners 28, runs under the status bar. Takes an optional pill subtitle. |
| `IconButton44` | The circular tap target for bar actions — transparent until pressed, then accent-200. `size` opens up for the 24dp read-aloud and 26dp delete buttons. |
| `MicDock` | The bottom dock. 88/34 on the topic list, 76/30 in conversation. Owns the recording pulse ring. |
| `Pill` | Rounded tag — provider name, exchange count, history date. |
| `AvatarBadge` | Lettered circle: `T` tutor, `Y` learner, and the topic card's number. Font size defaults to half the diameter. |
| `WarmFieldLabel` | The 12sp block label above a field. |
| `WarmTextField` | Pill field on the surface fill, accent caret and focus ring. |
| `WarmRadio` | 16dp dot; selected is an accent rim, a ring of page colour, then an accent centre. |
| `ChoicePill` | One option of a single choice. `shape` and `border` are open so it serves both as a standalone chip and butted inside a segmented group. |

---

## Deliberate departures from the handoff

Each of these is a decision, not drift. **Do not "fix" them back without reading why.**

1. **`outline` is Neutral600, not the handoff's divider.** Control edges have to be
   findable; see above.
2. **Text fields are 56dp, not the sheet's 36dp minimum.** Material cannot go lower
   without a hand-built decoration box, and 36dp is under the 48dp touch target anyway.
3. **Field labels sit above the box, not notched into the border.** A pill border has
   nowhere to cut a notch. The label is mirrored into `contentDescription` so the input
   still announces itself.
4. **Delete confirmations are kept.** The handoff's history card deletes on first tap.
   A mis-tap there quietly destroys practice.
5. **The app is light-only.** The handoff ships one palette, so `MyApplicationTheme`
   takes no `darkTheme` or `dynamicColor` argument — a wallpaper palette would overwrite
   the design.

## Known accessibility trade-offs

Accepted to stay faithful, and worth knowing before anyone reports them as bugs:

- The mic button is Accent500 under a Neutral100 glyph — 2.75:1, under the 3:1 for
  non-text. It is drawn that way. Recording is signalled by the pulse ring and the label,
  not by colour alone.
- `secondary` / `onSecondary` is 2.58:1.
- `IconButton44` is 44dp, under Material's 48dp guidance, because the handoff draws 44.
- A disabled radio inherits the handoff's only disabled treatment, 45% opacity, which on
  a hairline leaves very little ink. The row's dimmed label carries the meaning.

## Screens without a design

Onboarding and the three API-profile screens were never drawn. They use the shell
(`WarmTopBar`, `IconButton44`) and the form controls above, but their layout is their own
and they have no drawing to be measured against — their goldens use the default device
qualifier rather than the handoff's 393dp canvas.

`ARCHITECTURE.md` asks for stock Material Design. On the four designed screens the
handoff wins; everywhere else Material is still the default.
