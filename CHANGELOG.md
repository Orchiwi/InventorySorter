# Changelog

All notable changes to InventorySorter are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Four-button sort toolbar attached to player, chest, shulker, hopper, and
  dispenser/dropper screens: three layout buttons (vertical, compact,
  horizontal) that apply the sort on click, plus one criterion-cycle button
  for name / item type / creative category / rarity + quantity. The compact
  layout preserves the current order and just fills empty gaps; horizontal
  and vertical re-arrange after sorting by the active criterion.
- `R` keybind (rebindable from Controls under the Inventory category) that
  applies the sort to whichever inventory the player currently has open,
  or to the player main inventory when no screen is open.
- Sub-sort by readable NBT signature: enchanted books with the same enchant
  cluster together regardless of level, and potions/named items keep a
  stable order within their item type.
- JSON config at `<game>/config/inventorysorter.json` with atomic writes;
  the last selected criterion and layout survive sessions and per-screen
  toggles let the user disable the toolbar on individual screen types.

### Changed

- Slot permutation now uses a selection-sort algorithm that detects
  same-item collisions and routes the swap through a scratch slot instead
  of plain PICKUP. Vanilla PICKUP merges or no-ops on same-item slots,
  which made the previous cycle-decomposition non-idempotent (clicking
  Sort repeatedly produced different layouts). The new approach is
  idempotent: a second click on Sort changes nothing.
- Reworked the three layout methods so they match the Inventory Tweaks
  classic algorithm (a port of
  `invtweaks.InvTweaksHandlerSorting.computeLineSortingRules` and
  `defaultSorting`):
  - **HORIZONTAL** assigns each item type its own rectangle inside the
    grid. Base rectangle width is `rowSize / ceil(distinctTypes / rows)`
    and the rectangle grows along the line axis (then perpendicular)
    when a type has more stacks than the base size can hold. The last
    rectangle on a row stretches by one slot to consume any single-slot
    leftover. Stacks of the same type are laid out row-major inside
    their rectangle. Types whose stack count exceeds the line size are
    processed first so they consume contiguous space before smaller
    types.
  - **VERTICAL** uses the same algorithm with the axes swapped: each
    type's rectangle is one column wide by `spaceHeight` tall, the
    cursor walks columns first, and the rectangle stack ordering is
    column-major.
  - **COMPACT** (UI label *Linéaire* / *Compact*) skips the rectangle
    phase entirely and lays the sorted stacks out contiguously, which is
    Inventory Tweaks' DEFAULT method.

  All three modes sort the per-type stacks by the active criterion
  before assigning slots.

### Deferred

- Partial-stack merging before sorting (config option exists but the sort
  pipeline still preserves existing stack boundaries; tracked for v0.2).
- In-screen icon textures for the toolbar buttons (single-character glyphs
  with translatable tooltips ship in the meantime).
- Mod Menu config screen factory (Mod Menu still lists the mod via
  fabric.mod.json metadata, just without a config button).
