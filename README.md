# InventorySorter

A client-only Fabric mod that adds sort buttons and a keybind to player and
container inventories. Works on any server (vanilla or modded) because the
sorting is performed entirely client-side via standard slot clicks.

## Features

- Four sort buttons attached above every supported inventory screen:
  - **Criterion cycle** — switch between *Name*, *Item type*, *Creative category*,
    *Rarity + Quantity*.
  - **Vertical / Compact / Horizontal** — each click immediately applies its layout
    with the current criterion. *Horizontal* groups same-item stacks together row
    by row with a 1-slot separator between distinct item types; *Vertical* does
    the same but column by column; *Compact* sorts and packs the stacks
    contiguously with no separators.
- Configurable keybind (default `R`) that triggers a sort on the focused
  inventory, or on the player inventory if no screen is open.
- The player hotbar is preserved by default so the item in hand never moves.
- Items with NBT (enchanted books, potions, named items, player heads) are
  grouped by item type and sub-sorted by readable NBT signature — every
  *Sharpness* book ends up next to every other *Sharpness* book regardless of
  level.

## Supported screens

- Player inventory (`E`)
- Single and double chest, trapped chest
- Shulker box
- Ender chest
- Barrel

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft
   version.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Drop `inventorysorter-<version>.jar` into your `mods/` folder.
4. *(Optional)* Install [Mod Menu](https://modrinth.com/mod/modmenu) to access
   the in-game configuration screen.

## Configuration

Settings live in `<game>/config/inventorysorter.json`. They can also be edited
from the Mod Menu config screen when Mod Menu is installed.

| Key                    | Default       | Description                                       |
| ---------------------- | ------------- | ------------------------------------------------- |
| `current_criterion`    | `NAME`        | Last selected sort criterion.                     |
| `current_method`       | `HORIZONTAL`  | Last selected layout method.                      |
| `include_hotbar`       | `false`       | Whether the player hotbar is sorted as well.      |
| `merge_partial_stacks` | `true`        | Whether partial stacks are combined before sort.  |
| `enabled_screens`      | all `true`    | Per-screen toggle for the toolbar.                |

## Build

```sh
./gradlew build
```

The mod jar is produced at `build/libs/inventorysorter-<version>.jar`.

## Run a development client

```sh
./gradlew runClient
```

The first run will fail until you accept the EULA: edit `run/eula.txt` and
set `eula=true`.

## License

[MIT](LICENSE) — Copyright (c) 2026 Orchiwi.
