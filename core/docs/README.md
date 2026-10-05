# J's Core documentation

The documentation of [J's Core](../README.md), the library every mod of the J's Tech Series is built on, and that
any technology mod may be built on. These pages are for the programmer of such a mod: they explain each part of
the Core from zero, with the code that uses it. They assume you know Java and have a NeoForge mod that builds;
they assume nothing about the Core.

Start with [Getting started](GETTING_STARTED.md): it takes you from an empty mod to one with its first block, and
says what the Core promises and what it does not yet.

## Making things

| Page | What it covers |
| --- | --- |
| [Getting started](GETTING_STARTED.md) | Adding the Core to your project, your mod's content, data generation. |
| [Content](CONTENT.md) | Blocks, items, looks, creative tabs, block entities, devices, menus, materials, items that hold things, ids that are saved. |
| [Text and colour](TEXT_AND_COLOUR.md) | Sentences in every language, and the palettes screens paint with. |
| [Machines](MACHINES.md) | Processing machines, recipes with counts and fluids, upgrades, JEI and EMI. |
| [Multiblocks](MULTIBLOCKS.md) | Shapes of many blocks, as data, with ports. |
| [Entities](ENTITIES.md) | Entities, vehicles, robots, projectiles, and what is worn. |

## Moving things

| Page | What it covers |
| --- | --- |
| [Energy and resources](ENERGY_AND_RESOURCES.md) | Energy, fluids, chemicals and items between blocks; faces and filters. |
| [Cables](CABLES.md) | The one cable block, your own cable types, and parts on cables. |
| [Networks](NETWORKS.md) | The data network and its members, and peripheral links. |
| [Operations](OPERATIONS.md) | Requests a network carries out, and adding a kind of your own. |

## The world and the game

| Page | What it covers |
| --- | --- |
| [World](WORLD.md) | Ores, structures, dimensions and their rules, data over areas, chunk loading. |
| [State and saves](STATE_AND_SAVES.md) | Values kept with the world, versions of saves, data pack values, batches, schedules, the calendar, timings, the Core's events. |
| [Ownership](OWNERSHIP.md) | Teams, owners and access. |
| [Progression](PROGRESSION.md) | Eras, tiers, progression axes and advancements. |
| [Commands](COMMANDS.md) | Commands under `/jstech`. |
| [Settings](SETTINGS.md) | Settings files, their ranges, and the settings screen. |

## What players see and hear

| Page | What it covers |
| --- | --- |
| [Screens](SCREENS.md) | Layouts you can test, themes and skins, drawing text, models, screens in the world, keys. |
| [UI components](UI_COMPONENTS.md) | The components desktop programs and dialogs are built from. |
| [Fonts](FONTS.md) | Declaring a font from its free source, and the monospace grid painter. |
| [Motion](MOTION.md) | Curves, motion profiles, and the clock every motion reads. |
| [Overlays](OVERLAYS.md) | HUD elements, holograms, and what Jade shows. |
| [Sound](SOUND.md) | Sounds declared once, channels, cues, sounds made as they play, recordings. |

## Testing

| Page | What it covers |
| --- | --- |
| [Testing](TESTING.md) | Unit tests and GameTests for a mod on the Core, and the kit. |

## How to read these pages

- Each section says when the part it describes was added to the series, as `Added YYYY-MM-DD`.
- Each page ends with **What can go wrong**: the mistakes people make with that part, and what to do.
- Code in `rl("name")` stands for `ResourceLocation.fromNamespaceAndPath(YOUR_MOD_ID, "name")`, and `CONTENT` for
  your mod's `ModContent`.
- The API the series promises to keep is narrower than these pages: see [docs/API.md](../../docs/API.md) and
  [Getting started](GETTING_STARTED.md#what-is-promised-and-what-is-not-yet).
