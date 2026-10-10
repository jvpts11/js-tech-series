# Overview

J's Core is the library under the J's Tech Series, and a general-purpose library for building technology mods. Every
mod of the series stands on it and on nothing else of the series, and any other technology mod may stand on it too. It
is what a technology mod needs and would otherwise write for itself, written once, explained from zero, and kept
working from one release to the next.

## What exists today

The Core is the most built part of the series. Its programmers' documentation explains every part from zero, with code
([J's Core documentation](../README.md)); these pages say what each part is for, the rules behind it, and what is still
to come. Built today:

- **the platform**: declaring blocks, items, block entities, menus and entities once; values kept with the world, with
  versions; settings files and their screen; commands; teams and owners; energy, fluids, chemicals and items moving
  between blocks; the cable block; machines and their recipes; multiblocks; the data network and Operations; world
  generation, dimensions and chunk loading; screens, fonts, motion, overlays and sound; manuals; testing
  ([The platform](platform.md) and the pages after it);
- **the series' shared model**: the hardware eras and the industrial tiers, the material catalogue, the network's
  roles and the Operations framework ([Progression](progression.md), [Materials](materials.md),
  [Networks and Operations](networks-and-operations.md));
- **the first shared content**: the materials every mod trades, the cable block every mod lays its cables in, and the
  framework of the series' manuals.

## To build

### Three layers

| Layer | What it holds | Who it is for |
| --- | --- | --- |
| **The platform** | the generic tools: content, state and saves, settings, energy and resources, cables, machines, multiblocks, networks, the world, screens, sound, manuals, testing | any technology mod, in the series or not |
| **The series' shared model** | the concepts several mods of the series must share one identity of: the eras, the tiers, the materials and their states, the network's roles, the kinds of Operation, the progression a team reaches | the mods of the series, and add-ons to them |
| **Shared content** | the blocks and items several mods need as one and the same thing, so that none of them depends on another: the materials and the catalogue of fluids and components, the cable block and its basic lines, the basic battery, the basic wrench, the saw and the Configuration Card, the FE converters, the Space Persistor, the Atmosphere Detector, the containment of exotic matter, the Structure Projector, the series' manual | players, through whichever mods they install |

**The Core has no game of its own.** It has no goal, no progression of its own, and nothing a player does for the
Core's sake: its content is there because other mods need it, and a world with the Core alone has nothing to do.

### Principles

- **Our own implementation, always.** Everything in the Core is written natively, by the series, and never copied
  from another mod's code. Other mods named in these pages are inspirations and likenesses for what a kit does; how it
  does it is the Core's own.
- **Declared once, explicitly.** Everything a mod adds is declared through builders, once, and the files the game
  needs (models, language, loot, tags, sound lists) are written from that declaration. Nothing registers itself by
  annotation. Reflection is used where it is useful and healthy, such as finding the classes that hold a mod's text.
- **Only primitives, never a game.** A feature enters the platform as a primitive many technology mods need; a
  reactor, an ore tree, an instruction set or a recipe set is a mod's, never the Core's.
- **Every word translatable, every colour data.** What a player reads is declared as a key beside the code that says
  it; what a screen paints with is a palette a resource pack can change.
- **Saves are kept.** Everything saved has a version and a way to read older ones; a save written by a newer version is
  never written over.
- **Consequences have settings.** Where a system can hurt a player or a world (radiation, overvoltage, pollution), a
  server setting turns the consequence off; nothing turns a whole part of a mod off.
- **Never at the server's cost.** Work runs off the tick where it can, in near-constant time, within budgets.
- **Explained from zero.** Every promised part has documentation that assumes the reader knows nothing about it, with
  an example that compiles.
- **Never doing a job twice.** Where an installed mod already does what a system of the Core does, the Core steps
  aside and hands it its data, unless a game rule of the series depends on the Core's own ([Modpacks](modpacks.md)).
- **One loader.** The Core is for NeoForge and Java. It does not aim at other loaders or at Kotlin.

### The pages

| Page | What it covers |
| --- | --- |
| [The platform](platform.md) | Content, state and saves, settings, commands, teams and owners, timing and diagnostics. |
| [Progression](progression.md) | Axes, eras as a registry, tiers, milestones, knowledge and the knowledge gate, advancements. |
| [The API](api.md) | What the Core promises, how it grows, and how the promise is checked. |
| [Materials](materials.md) | Elements, isotopes, exotic forms, materials, forms, properties, minerals, the shared catalogue, naming. |
| [States of matter](states-of-matter.md) | Items, liquids, gases, plasmas, supercritical fluids, slurries and exotic matter. |
| [Energy](energy.md) | J's Energy, voltage classes, loss, transformers, protection, the converters, the basic battery. |
| [Cables and lines](cables-and-lines.md) | The cable block, the lines each mod registers, the basic lines, overhead lines. |
| [Multipart](multipart.md) | Block spaces with many parts, microblocks and facades. |
| [Conveyors](conveyors.md) | Items on belts, chutes and lifts, worked as they pass. |
| [Mechanical power](mechanical-power.md) | Rotation in real units: shafts, gears, belts, flywheels, shear pins. |
| [Heat](heat.md) | Temperature in kelvin, conduction, radiators in a vacuum, boilers, changes of state, thermal views. |
| [Machines](machines.md) | The machine framework: resources, sides, upgrades, protection, repair, the standard screen. |
| [Multiblocks](multiblocks.md) | Shapes as data, ports, formed models, variable sizes, the Structure Projector. |
| [Networks and Operations](networks-and-operations.md) | The data network's model, the Operations framework, point-to-point links. |
| [Redstone and signals](redstone-and-signals.md) | Sixteen channels in a cable, logic gates and circuits, signals by frequency, every sensor on redstone. |
| [The world](world.md) | Ores and veins, reservoirs, structures, dimensions and their rules, the physics kit, chunk loading. |
| [Hazards](hazards.md) | Radiation, contamination, dissonance and pollution. |
| [Explosions and fire](explosions-and-fire.md) | Blasts through blocks at the speed of sound, fire by material, air and wind, and its classes. |
| [Sealed rooms](sealed-rooms.md) | Rooms that hold an atmosphere, and the Atmosphere Detector. |
| [Moving structures](moving-structures.md) | Structures of real blocks that move: rockets, starships, vehicles. |
| [Entities and vehicles](entities.md) | Vehicles of one piece, robots, creatures, projectiles and worn modules. |
| [Tools and items](tools-and-items.md) | Tools with modes, mining and building over an area, tools on energy, fuel or air, items that answer keys. |
| [The interface](interface.md) | Screens, themes, fonts, motion, overlays, keys and sound. |
| [Models](models.md) | Animated models of the Core's own, from Blockbench, Blender, Maya, 3ds Max and Cinema 4D. |
| [Lighting and effects](lighting-and-effects.md) | Coloured and dynamic light, bloom, particles, beams, smoke and screen effects. |
| [Manuals](manuals.md) | The guide framework and the series' Technical Reference. |
| [Modpacks](modpacks.md) | Everything as data, scripts, unification, tools for packs, and stepping aside for other mods. |
| [For those who make mods](developers.md) | Capabilities, calls from screens, work off the tick, codecs, errors that teach, inspectors, the Gradle plugin. |
| [Low level](low-level.md) | Reaching into the game, and native animated models. |
| [Implementation](implementation.md) | Settings, numbers and files. |
| [The series](series.md) | Who depends on whom, and what each mod takes from the Core. |
