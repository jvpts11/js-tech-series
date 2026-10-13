# The API

The public API lets other mods add to J's Industrial without touching its code. Most of what an add-on builds on lives
in J's Core (materials, machines, multiblocks, energy, the cable block), which any technology mod can use; this page
keeps what J's Industrial adds on top of it, and the design behind it.

## What exists today

Nothing of J's Industrial's own: add-ons use J's Core's API.

## To build

### What an add-on can add

| What | Through | Page |
| --- | --- | --- |
| Materials, their forms and properties, host rocks, isotopes | J's Core's registries | [Materials](materials.md) |
| Ores and deposit types for the vein system | J's Core's world generation kit | [The world](world.md) |
| Machines and multiblocks, with the standard screen, ports and upgrade slots | J's Core's machine framework | [Machines](machines.md) |
| Recipes for J's Industrial's machines | recipe types, as data | [Machines](machines.md) |
| Chemical reactions, with their conditions (temperature, pressure) and their catalyst | a reaction recipe type | [Chemistry](chemistry.md) |
| Fuels, heat sources and generators | J's Core's energy API | [Energy](energy.md) |
| Upgrades and modules | an upgrade or module declaration, with its effect | [Machines](machines.md), [Tools and armour](tools-and-armour.md) |
| Discoveries | an entry in the research tree: its name, date, the data it asks for and what it unlocks | [Science](science.md) |
| Operation types | J's Core's Operations framework, for machines that become computers with J's Computers | [With J's Computers](computers.md) |

### The rules

- Everything is declared explicitly, through builders; nothing registers itself by annotation.
- The API is versioned with one number for J's Industrial; what is new is marked experimental in its first release, and
  the CHANGELOG has an "API" section.
- Only the API packages are public; everything else is internal and changes without warning.
- The full explanation, from zero and with code, goes in the public API guide of J's Industrial, next to J's Core's and
  J's Computers'.
