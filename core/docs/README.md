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
| [Block spaces and microblocks](MULTIPART.md) | Several things in one block space, microblocks of any block, the saw, facades. |
| [Tools and items](TOOLS.md) | Tools with modes, areas and veins mined, building tools and copies of regions, tools on energy, fuel or air, heat, waits and charging, modules in tools, keys for items not held, durability from a material. |
| [Entities](ENTITIES.md) | Entities, vehicles, robots, projectiles, and what is worn. |
| [Moving structures](MOVING_STRUCTURES.md) | Structures of real blocks that move and keep working: made from the world and set down on its grid, moved by rules (toward a place, spinning, on a hinge, along a path, facing a direction, on rails), stood on and carried, aimed at and used, drawn as cached meshes in the world's light, saved in mid-move, taken to another dimension with everyone aboard, kept loaded by a chunk loader aboard. |

## Moving things

| Page | What it covers |
| --- | --- |
| [Energy and resources](ENERGY_AND_RESOURCES.md) | Energy, fluids, chemicals and items between blocks; faces and filters. |
| [Cables](CABLES.md) | The one cable block, your own cable types, and parts on cables. |
| [Redstone and signals](SIGNALS.md) | Sensors that speak redstone, signals by frequency, relays, the signal probe. |
| [Heat](HEAT.md) | Temperature in blocks, conduction by real materials, heat pipes, loss to the air and to space. |
| [Hazards](HAZARDS.md) | Kinds of harm and resistances, hazards that build up in a body in bands, hazards of carried things, the lungs, radiation and its shielding by block density, ionizing radiation and contamination, dissonance, pollution by chunk (soot, acid rain, heavy metals, fallout, carbon dioxide), the hazard devices and the body's screen, maps of hazards, the land withering. |
| [Explosions and fire](EXPLOSIONS_AND_FIRE.md) | Blasts decided by their energy: a shock wave at the speed of sound through blocks by their strength, overpressure on bodies, fragments of the casing and debris, the medium (no wave in a vacuum, far under water), chains, craters and scorched ground, claims and each mod's setting, the destruction spread over ticks; fire spreading by the oxygen and the wind, what burned staying burned, the classes of fire and the agents, smoke and heat sensed, extinguishers, the sprinkler and the detectors. |
| [Sealed rooms](ROOMS.md) | Closed spaces found and their air kept by real gases: leaks and bursting blocks, doors, breathing, fire and oxygen, rooms under water, mobs and plants, the view of a room, the Atmosphere Detector, vents. |
| [Mechanical power](MECHANICAL_POWER.md) | Shafts, gears and gearboxes in rpm, newton-metres and watts; sources, loads, motors and dynamos. |
| [Conveyors](CONVEYORS.md) | Belts of your own kinds, slopes, chutes, bucket elevators, splitters, joins, diverters, machines that work items on the belt, what makes them move, players and mobs carried. |
| [Networks](NETWORKS.md) | The data network and its members, machines of any mod on it, their state as tables, and peripheral links. |
| [Operations](OPERATIONS.md) | Requests a network carries out, and adding a kind of your own. |

## The world and the game

| Page | What it covers |
| --- | --- |
| [World](WORLD.md) | Ores and veins, reservoirs, structures, landmarks, finding things, map markers, worlds that change (old chunks, live changes, large edits, world events, fields), dimensions and their rules, the shapes of the land, data over areas, chunk loading. |
| [State and saves](STATE_AND_SAVES.md) | Values kept with the world, versions of saves, data pack values, batches, schedules, the calendar, timings, the Core's events. |
| [Ownership](OWNERSHIP.md) | Teams, owners and access. |
| [Protection](PROTECTION.md) | Claims, permissions, limits, the audit log and damage between players. |
| [Diagnostics](DIAGNOSTICS.md) | Sections of work, profilers, the watchdog, budgets, network use, cost, `/jstech health`, crash reports. |
| [Notifications](NOTIFICATIONS.md) | Notifications on the HUD: levels, alike ones grouped, the history, actions, silencing, do not disturb. |
| [Progression](PROGRESSION.md) | Eras as a registry, tiers, axes that move by playing, what a team reaches, milestones, knowledge and the knowledge gate, advancements. |
| [Commands](COMMANDS.md) | Commands under `/jstech`. |
| [Settings](SETTINGS.md) | Settings files, their ranges, and the settings screen. |

## What players see and hear

| Page | What it covers |
| --- | --- |
| [Screens](SCREENS.md) | Screens declared in files, the layout engine, values bound by name, styles, layouts you can test, themes and skins, drawing text, models, screens in the world, keys. |
| [The camera](CAMERA.md) | Lenses with real powers, the view following what is ridden, free look, scenes along camera paths, views of an entity or from a point, cameras whose view a screen shows. |
| [UI components](UI_COMPONENTS.md) | The components screens, desktop programs and dialogs are built from: controls, trees, tables, charts, graphs, 3D views, maps, gauges, ghost slots, rich tooltips. |
| [Tooltips](TOOLTIPS.md) | Each mod's lines kept together and its name last, parts shown with Shift and Ctrl, bars, pictures, turning 3D previews, what an item holds. |
| [Fonts](FONTS.md) | Declaring a font from its free source, and the monospace grid painter. |
| [Models](MODELS.md) | Animated models for blocks, items, entities and armour from Blockbench, Bedrock, glTF, FBX and OBJ files; controllers, blends, expressions, events, bones moved by code, animations from the server, the preview. |
| [Connected textures](CONNECTED_TEXTURES.md) | Textures that join between alike blocks, read from the formats resource packs write for OptiFine, Continuity and CTM; microblocks; stepping aside for Continuity and CTM. |
| [Lighting](LIGHTING.md) | Kinds of light as data, lamps by redstone or power that flicker and break, spotlights and beams past fifteen blocks, coloured light, dynamic light, glow, light sensors, stepping aside. |
| [Particles and effects](EFFECTS.md) | Effects as files of emitters, particles moved by the place's physics and wind, trails, beams, arcs, marks, screen effects, effects fired from the server, the effects editor in the world. |
| [Motion](MOTION.md) | Curves, motion profiles, and the clock every motion reads. |
| [Overlays](OVERLAYS.md) | HUD elements, holograms, and what Jade shows. |
| [Manuals](MANUALS.md) | Chapters, entries, manuals and styles, special blocks, and the manual key. |
| [Sound](SOUND.md) | Sounds declared once, channels, cues, sounds made as they play, recordings. |

## Building and testing

| Page | What it covers |
| --- | --- |
| [The toolkit](TOOLKIT.md) | Capabilities of your own and their cache, calls from a screen to the server, work away from the tick, codecs for records, other mods behind a guard. |
| [While you build](DEVELOPING.md) | Listeners handed to the buses, errors that say how to fix them, the data run's check, the log one part at a time, data and assets reloaded as they change. |
| [Testing](TESTING.md) | Unit tests and GameTests for a mod on the Core, and the kit. |
| [Reaches into the game](REACHES.md) | The Core's few mixins and access transformer lines, each with its reason and its test; namespaces whose data is stable. |

## How to read these pages

- Each section says when the part it describes was added to the series, as `Added YYYY-MM-DD`.
- Each page ends with **What can go wrong**: the mistakes people make with that part, and what to do.
- Code in `rl("name")` stands for `ResourceLocation.fromNamespaceAndPath(YOUR_MOD_ID, "name")`, and `CONTENT` for
  your mod's `ModContent`.
- The API the series promises to keep is narrower than these pages: see [docs/API.md](../../docs/API.md) and
  [Getting started](GETTING_STARTED.md#what-is-promised-and-what-is-not-yet).

## The design

[The design of J's Core](design/README.md): one page per subject, from the platform, materials and energy to cables,
machines, multiblocks, the world, hazards, sealed rooms, moving structures and the interface, saying what each part
is for and the rules behind it, with what is designed and still to be built at the end of each page, and the order in
which it will be built.
