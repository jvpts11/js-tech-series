# Modpacks: recipes and loot

J's Computers has no crafting recipes of its own, and it puts nothing in the world's chests: everything it adds comes
from its creative tabs. For now that makes it a mod to play in creative. A modpack that wants it in survival can give
it both, recipes and loot, with a plain datapack or with [KubeJS](https://kubejs.com). This page shows how.

The examples below are the mod's own test data with the namespace changed to `mypack`: the tests load them and check
what they make, so each one works as printed.

A recipe changes only how a thing is obtained. A computer or a part made by a recipe is the same as one taken from the
creative tab, and nothing in J's Computers asks where a thing came from.

## Finding an item's id

Every item of J's Computers is named `jsc:<name>`, such as `jsc:vintage_personal_computer` or `jsc:cd_rom`. To read
one in the game:

- press **F3 + H** to turn on advanced tooltips, and the id is shown under the item's name;
- or look the item up in the recipe viewer (JEI or EMI);
- or, with KubeJS, hold the item and run `/kubejs hand`.

Most things are plain items, named by their id like any other mod's. Two kinds exist only made, with data written on
them, and their id alone does not name them:

- an **installer**: a blank medium (a floppy disk, a CD, a DVD, a flash drive or a Blu-ray disc) with a system or a
  program written on it;
- a **ready disk**: a disk with a system already installed on it, which brings a computer straight up.

Both are shown below. To read the data of one taken from the creative tab, hold it and run
`/data get entity @s SelectedItem`.

## Tags

J's Computers sorts its items into tags, so a recipe or a chest can ask for "any of these" instead of a list. The
tags hold what the creative tabs hold.

One tag per era, holding everything of that era (the tab of the same name):

| Tag | Holds |
| --- | --- |
| `#jsc:era/vintage` | Everything in the tab J's Computers - Vintage. |
| `#jsc:era/legacy` | Everything in the tab J's Computers - Legacy. |
| `#jsc:era/transition` | Everything in the tab J's Computers - Transition. |
| `#jsc:era/standard` | Everything in the tab J's Computers - Standard. |
| `#jsc:era/advanced` | Everything in the tab J's Computers - Advanced. |

One tag per kind of thing, holding it in every era:

| Tag | Holds |
| --- | --- |
| `#jsc:personal_computers` | The Personal Computers, in every case of their era. |
| `#jsc:crafting_computers` | The Crafting Computers. |
| `#jsc:cluster_management_computers` | The Cluster Management Computers. |
| `#jsc:mainframes` | The Mainframes, which run a network. |
| `#jsc:racks_and_servers` | Server racks and supercomputer racks, the server cases, the servers and the nodes. |
| `#jsc:rack_bays` | What a rack carries in its bays besides servers: the rack units. |
| `#jsc:peripherals` | The devices at a desk: monitors, drives, encoders, printers, speakers, hubs, peripheral cables. |
| `#jsc:network` | The data network: its cables, routers, repeaters and interfaces, and the buses on a cable. |
| `#jsc:components` | The parts a machine is built from: boards, processors, memory, cards, supplies and disks. |
| `#jsc:programs` | The blank media and the manuals. |

What every era shares, in the tab J's Computers, is in the tags of its kind and in no era's tag. An installer is in
the tags of its blank medium.

## With a datapack

A datapack is a folder in the world's `datapacks` folder, holding a `pack.mcmeta` and a `data` folder; Minecraft's
own documentation says how to make one. The files below go under `data/`.

### A recipe

`data/mypack/recipe/vintage_personal_computer.json`: a Vintage Personal Computer from iron and redstone.

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["III", "IRI", "III"],
  "key": {
    "I": {"item": "minecraft:iron_ingot"},
    "R": {"item": "minecraft:redstone"}
  },
  "result": {"id": "jsc:vintage_personal_computer", "count": 1}
}
```

An ingredient can be a tag: `{"tag": "jsc:era/vintage"}` takes anything of the Vintage era.

### An installer

`data/mypack/recipe/frames_95_installer.json`: the installer of Frames 95, written on a CD. The result is the blank
medium, with two data components saying what is written on it: `jsc:media_kind` is `os_install` for a system or
`program_install` for a program, and `jsc:media_payload` is the system's or the program's id.

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    {"item": "minecraft:paper"},
    {"item": "jsc:cd_rom"}
  ],
  "result": {
    "id": "jsc:cd_rom",
    "count": 1,
    "components": {
      "jsc:media_kind": "os_install",
      "jsc:media_payload": "jsc:frames_95"
    }
  }
}
```

Each system and program ships on the medium of its generation, which the creative tab and `JsComputers.installer(...)`
below always use: Vintage on a floppy disk, Legacy on a CD, Transition on a DVD; from the Standard era, a system, and
a program that runs in the background such as a server's, on a flash drive, and any other program on a DVD in the
Standard era and on a Blu-ray disc in the Advanced one.

### Loot in a structure's chests

NeoForge lets a datapack add a loot table of its own to any chest, through a global loot modifier. Three files put
one thing of the Vintage era in every dungeon chest.

`data/mypack/loot_table/chests/vintage_parts.json`: what is added, one roll from the era's tag.

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {"type": "minecraft:tag", "name": "jsc:era/vintage", "expand": true}
      ]
    }
  ]
}
```

`data/mypack/loot_modifiers/vintage_parts_in_dungeons.json`: where it is added, the dungeon's chest table.

```json
{
  "type": "neoforge:add_table",
  "conditions": [
    {"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/simple_dungeon"}
  ],
  "table": "mypack:chests/vintage_parts"
}
```

`data/neoforge/loot_modifiers/global_loot_modifiers.json`: the list NeoForge reads the modifiers from.

```json
{
  "replace": false,
  "entries": ["mypack:vintage_parts_in_dungeons"]
}
```

The chest tables of the game's structures are listed on the Minecraft wiki's page on loot tables, such as
`minecraft:chests/abandoned_mineshaft` or `minecraft:chests/village/village_toolsmith`. A condition on the loot
table's id can be swapped for any loot condition, and the added table can be as rich as any loot table.

## With KubeJS

KubeJS (2101.7.2 or later, for NeoForge 1.21.1, with its Rhino) lets a modpack write the same in scripts, and
J's Computers hands its scripts what a plain id cannot name. Scripts that add recipes go in `kubejs/server_scripts/`:

```js
ServerEvents.recipes(event => {
  // A plain item, by its id.
  event.shaped('jsc:vintage_personal_computer', ['III', 'IRI', 'III'], {
    I: 'minecraft:iron_ingot',
    R: 'minecraft:redstone'
  }).id('mypack:vintage_personal_computer')
  // An installer exists only made, with its system written on it, so the script asks J's Computers for one.
  event.shapeless(JsComputers.installer('frames_95'), ['minecraft:paper', 'jsc:cd_rom'])
    .id('mypack:frames_95_installer')
  // Anything of one era, by the era's tag.
  event.shapeless('minecraft:diamond', ['#jsc:era/vintage'])
    .id('mypack:any_vintage_thing')
})
```

`JsComputers` is there in startup, server and client scripts:

| Call | Gives |
| --- | --- |
| `JsComputers.installer('frames_95')` | The installer of a system, on the medium of its era. |
| `JsComputers.programInstaller('isms')` | The installer of a program a player installs, on the medium of its era. |
| `JsComputers.readyDisk('frames_95')` | A disk of the system's era with the system already on it. |
| `JsComputers.systems('legacy')` | The ids of the systems that first run in an era. |
| `JsComputers.programs('legacy')` | The ids of the programs of an era a player installs. |
| `JsComputers.eras()` | The eras, oldest first: `vintage`, `legacy`, `transition`, `standard`, `advanced`. |

An id without a namespace is J's Computers' own, so `'frames_95'` is `'jsc:frames_95'`. An id nothing is registered
under gives an empty stack, which a recipe refuses with the script's line in KubeJS's log. A script can go through an
era instead of naming each system:

```js
ServerEvents.recipes(event => {
  JsComputers.systems('vintage').forEach(system => {
    event.shapeless(JsComputers.installer(system), ['minecraft:paper', 'jsc:floppy_disk'])
  })
})
```

KubeJS has no loot event of its own. The datapack files above work from KubeJS's own `kubejs/data/` folder, which it
loads as a datapack, and the LootJS addon writes loot in scripts.

### Hardware of the modpack's own

*Added 2026-10-25.*

An item becomes a part from a script in `kubejs/startup_scripts/`, which runs while the game loads: the item first,
then its sheet as an object. The item is one KubeJS or another mod registers; J's Computers reads the sheet once the
game sets up, so a field written wrong stops the loading with what is wrong and of which part.

```js
JsComputers.memory('mypack:scripted_ram', { era: 'advanced', generation: 'ddr5', buffer_items: 4096, tdp_watts: 3 })
```

| Call | Declares | Its sheet's fields |
| --- | --- | --- |
| `JsComputers.processor(item, sheet)` | A processor | `era`, `isa`, `socket`, `cores`, `freq_mhz`, `tdp_watts`, and `alien`, `design` |
| `JsComputers.graphicsCard(item, sheet)` | A graphics card | `era`, `bus`, `cores`, `vram_mb`, `tdp_watts`, `clock_mhz`, `arch`, and `chip` |
| `JsComputers.memory(item, sheet)` | A memory module | `era`, `generation`, `buffer_items`, `tdp_watts` |
| `JsComputers.disk(item, sheet)` | A disk | `tier` (`hdd`, `ssd`, `nvme`), `era`, `capacity_items`, `tdp_watts` |
| `JsComputers.motherboard(item, sheet)` | A board | `form_factor`, `era`, `socket`, `cpu_slots`, `memory`, `ram_slots`, `bus`, `slots`, `disk_slots` |
| `JsComputers.powerSupply(item, sheet, era)` | A supply, shown with `era` | `wattage`, `efficiency_percent`, and `auto_scaling` |
| `JsComputers.expansionCard(item, sheet)` | A card | `kind`, `era`, `bus`, `tdp_watts` |
| `JsComputers.memoryGeneration(sheet)` | A generation of memory | `id`, `label`, `latency_ticks` |
| `JsComputers.slotGeneration(sheet)` | A generation of slot | `id`, `generation`, `label`, `slot_name`, `family` |
| `JsComputers.formFactor(sheet)` | A form factor | `id`, `label` |
| `JsComputers.cardKind(sheet)` | A kind of card | `id` |

A kind already declared is named by its id, a bare name being J's Computers' own: `era: 'legacy'`, `generation:
'ddr4'`, `bus: 'pcie_4_0'`, `form_factor: 'atx'`, `socket: 'lga_1150'`, and an instruction set by its id or its
name (`'x86-64'`). The kinds a script declares come before the parts that use them, whatever order the script writes
them in.
