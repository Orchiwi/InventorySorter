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

### Deferred

- Partial-stack merging before sorting (config option exists but the sort
  pipeline still preserves existing stack boundaries; tracked for v0.2).
- In-screen icon textures for the toolbar buttons (single-character glyphs
  with translatable tooltips ship in the meantime).
- Mod Menu config screen factory (Mod Menu still lists the mod via
  fabric.mod.json metadata, just without a config button).
