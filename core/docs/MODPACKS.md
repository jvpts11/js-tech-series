# Modpacks

What the Core gives to those who put mods together into a pack, with no Java: content declared in files, and the data
packs and resource packs that change the rest. The design behind it is in [Modpacks](design/modpacks.md).

## Content packs

*Added 2026-10-21.*

A content pack declares blocks, items, materials, kinds of processing machine and the controllers of machines in
JSON files, which the Core registers as if a mod had declared them. The game registers what it holds once, as it
starts and before any world's data packs are read, so content packs are read then too, from two places:

- the game's folder, under `jstech/content/` (any folders below, every `.json` file);
- every mod's own files, under `jstech_content/`, so a modpack's own small mod, or a mod with no Java of its own,
  can ship one.

A change to a content pack takes hold when the game starts again.

A file holds one declaration, an object with its `type`, or a list of them. Every id is `namespace:path`, in a
namespace of the pack's own: a pack may not add things to a mod's namespace.

```json
[
  {"type": "tab", "id": "mypack:main", "name": "My Pack", "icon": "mypack:token"},
  {"type": "block", "id": "mypack:copper_lamp", "name": "Copper Lamp", "strength": 3, "sound": "metal",
   "light": 15, "tool": "pickaxe", "needs": "stone"},
  {"type": "item", "id": "mypack:token", "name": "Token", "stack": 16},
  {"type": "material", "id": "mypack:mythril", "name": "Mythril", "kind": "metal", "colour": "#7AD7FF",
   "liquid": true, "forms": ["ingot", "dust", "block"], "ores": ["stone", "deepslate"],
   "properties": {"jscore:density": 5200}},
  {"type": "processing", "id": "mypack:smelting", "name": "Smelting", "machine": "mypack:smelter"},
  {"type": "machine", "id": "mypack:smelter", "name": "Smelter"}
]
```

| Type | What it declares | Its fields |
| --- | --- | --- |
| `block` | A plain block, with its item | `strength` (1.5, stone's), `resistance` (6), `sound` (`stone`, `wood`, `metal`, `glass`, `wool`, `gravel`, `sand`, `grass`, `copper`, `deepslate`, `amethyst`), `light` (0 to 15), `tool` (`pickaxe`, `axe`, `shovel`, `hoe`), `needs` (`wood`, `stone`, `iron`, `diamond`: the tier it takes to drop anything), `item` (`false` for a block with no item, which drops nothing) |
| `item` | A plain item | `stack` (1 to 99; 64) |
| `material` | A material of the shared catalogue ([Content](CONTENT.md#materials)) | `kind` (`metal`, `crystal`, `gem`, `nonmetal_solid`, `mineral`, `liquid`, `gas`, `exotic`), `colour` (`#RRGGBB`), `liquid` (whether it also melts), `forms` (by id, `ingot` or `mymod:pellet`), `ores` (the rocks its ore is in, `stone` or `mymod:marble`), `properties` (by id, each written as its property's file writes it) |
| `processing` | A kind of processing machine: a recipe type of the Core's processing recipes ([Machines](MACHINES.md)) | `machine` (the item shown working it) |
| `machine` | The controller block of a machine declared as data ([Multiblocks](MULTIBLOCKS.md)) | `strength` (3.5); it reads the machine file of its own id, `data/<namespace>/multiblock_machine/<path>.json` |
| `tab` | The creative tab the namespace's things are shown in | `icon` (the item drawn on it; the first thing declared otherwise) |

Every declaration has its `name`, in English. A namespace with no `tab` declared has one called by the namespace.

### What the Core makes for them

As the game loads, the Core makes what a pack does not bring, in packs of its own under every resource pack and data
pack, so a pack that brings its own file replaces any of these:

- the English names, `assets/<namespace>/lang/en_us.json`; a resource pack's own language files say them in others;
- a block state and a model for each block, its whole cube drawn with `<namespace>:block/<path>`, and for a
  machine's controller the game's furnace model turned the way it faces, drawn with `<path>_front`, `<path>_side`
  and `<path>_top`;
- a flat model for each item, drawn with `<namespace>:item/<path>`;
- each block's drop, itself as long as a blast leaves it, and its place in the tags of its tool and of the tier it
  needs.

The textures are the pack's own, in a resource pack: until it brings them, the game draws its missing texture. A
material's things are the Core's own, drawn in its colour from the game's textures, and named for it ("Mythril
Ingot"). What a machine does, its shape, slots, energy and recipes, is its machine file in a data pack, and the
recipes of a kind of processing machine are data pack recipes of its type.

### What can go wrong

- **The game stops loading with a paragraph about a content pack.** A file cannot be read, or a declaration cannot
  stand: the paragraph names the file, the declaration and how to put it right.
- **A block is drawn purple and black.** Its texture is missing: add `<namespace>:block/<path>` in a resource pack.
- **A change to a pack does nothing.** Content packs are read as the game starts: start it again.

## Conditions on data

*Added 2026-10-21.*

Any data file NeoForge loads with conditions (a recipe, a loot table, a modifier of loot, and more) carries them
under `neoforge:conditions`, and loads only when every one holds. NeoForge's own cover the mods installed and the
tags: `neoforge:mod_loaded` (`"modid": "mekanism"`), `neoforge:not`, `neoforge:and`, `neoforge:or`,
`neoforge:item_exists` and `neoforge:tag_empty`. The Core adds two:

```json
{
  "neoforge:conditions": [
    {"type": "jscore:setting", "file": "jstech-balance", "setting": "moving_structures.physics_tool_recipe"},
    {"type": "jscore:setting", "file": "jscomputers-server", "setting": "storage.mode", "value": "simple"}
  ],
  "type": "minecraft:crafting_shaped", "...": "..."
}
```

- `jscore:setting` holds while a setting has the value asked for: the settings file by its name without the
  extension, the setting by its dotted path in the file, and the `value`, which is `true` when left out. A value is
  a switch's `true` or `false`, a number, a word (alike in any case) or a list. A world's setting is read from the
  world's own file, even as the world opens and its data loads before its settings are read, so the condition holds
  from the first load; a change takes hold at the next `/reload`. A file or a setting that is not there fails the
  condition, and so does a player's own file, which no world's data may hang on; the log says why, once.
- `jscore:simple_path` holds while a mod's simpler path to the shared catalogue is open: unless J's Industrial is
  installed and the mod's own rule makes the industrial chain the only way (`"mod": "jscomputers"`).

## Recipes that ask for knowledge

*Added 2026-10-21.*

A recipe may ask for a piece of knowledge a team learns: it works only for whoever's team knows it, and the recipe
viewers (JEI and EMI) and the manuals show it all the same, with a padlock while the player's team does not.

- A processing machine's recipe takes `"knowledge": "mymod:fusion"`, and its machine runs it only for an owner whose
  team knows it ([Machines](MACHINES.md)).
- A crafting table's recipe is written as a shaped or a shapeless one, under the Core's type and with the knowledge:

```json
{"type": "jscore:crafting_shaped_knowledge", "knowledge": "mymod:fusion",
 "pattern": ["GD"], "key": {"G": {"item": "minecraft:gravel"}, "D": {"item": "minecraft:dirt"}},
 "result": {"id": "minecraft:lapis_lazuli"}}
```

`jscore:crafting_shapeless_knowledge` takes `ingredients` as a shapeless recipe does. It crafts at the crafting table
and in the inventory's grid for a player whose team knows it. Where no player is at the grid, as in a crafter block or
another mod's table that names none, it never matches, as a machine of no owner runs nothing locked.

## Unification

*Added 2026-10-21.*

When several mods make the same material (the Core's tin ingot and another mod's, both filed under `c:ingots/tin`),
one item stands for all of them: every recipe makes that item and asks for it, and what blocks, creatures and chests
drop comes out as it. The item that stands is the first preferred mod's, the game's own first and the Core's next by
default; a material none of them makes takes the first of its items by id.

What is unified is every common tag of every form of every material, `c:<folder>/<material>` (`c:ingots/tin`,
`c:dusts/tin`, `c:storage_blocks/tin`, `c:raw_materials/tin` and the rest), with more than one item in it. Ores never
are: an ore differs by the rock it is in. The recipes' files are rewritten just before the game reads them, so recipes
of every mod and every kind are reached: an item a recipe names under `item` or `id`, and an item named bare where a
recipe says what it makes (`result`, `results`, `output`, `outputs`). A recipe's conditions are left as they are.

The world's file `serverconfig/jstech-unification.toml` sets it, read as the world's data loads:

| Setting | What it does |
| --- | --- |
| `unification.enabled` | Whether anything is unified (on) |
| `unification.prefer` | The mods whose item stands, by id, first to last (`["minecraft", "jscore"]`) |
| `unification.extra_tags` | More item tags to unify, such as `c:gems/ruby` |
| `unification.skipped_tags` | Tags left as they are |
| `unification.kept_items` | Items never replaced by another |
| `unification.skipped_recipes` | Recipes left as they are, by id or all of a mod's (`mymod:*`) |
| `unification.drops` | Whether what drops is unified too (on) |
| `unification.step_aside` | Whether the Core leaves it to Almost Unified when that is installed (on) |

What drops is changed by the Core's modifier of loot, `jscore:unified_drops`, which a data pack takes out as it takes
out any. The log says, each time the data loads, how many items stand for how many others and how many recipes
changed.

### What can go wrong

- **A recipe of another mod breaks after unification.** Its kind names an item in a way the game reads as something
  else: add the recipe, or all of its mod's, to `unification.skipped_recipes`.
- **The wrong mod's item stands for a material.** Put that mod first in `unification.prefer`, or keep the item you
  want to stay with `unification.kept_items`.
- **Nothing is unified.** `unification.enabled` is off, or Almost Unified is installed and does it, which the log
  says once.

## Scripts and quests

*Added 2026-10-21.*

### KubeJS

With KubeJS (2101.7.2 or later), every script reaches what the series' mods hand it, under the names they gave, and
the Core's own:

- **`JsCore`**, the Core's calls: a player's team (`JsCore.teamOf(server, uuid)`), what a team knows
  (`knows`, `share`, `grant`, `forget`), the milestones it passed (`reached`, `reach`), how far it came along an axis
  (`steps`, `step`, `reachStep`), the catalogue's materials (`materials`, `material('jscore:tin', 'ingot')`), their
  properties (`properties`, `property('jscore:tin', 'jscore:atomic_number')`), which item stands for which
  (`unified('othermod:tin_ingot')`), and everything the Core keeps as data: the kinds of data its registries read
  (`dataKinds`, `dataIds('jscore:deposits')`, `data('jscore:deposits', 'jscore:copper')`: the deposits, the machines
  and multiblocks declared as data, the dimensions' rules, the skies and the rest) and the data maps of every mod
  (`dataMaps`, `dataMap('jscore:seals', 'minecraft:glass')`: how blocks seal, their physical properties and density,
  the hazards of items and more). Data comes back as JSON text, as its file writes it, for `JSON.parse`. Everything is
  named by its id; an id nothing declared is refused with a line in KubeJS's log naming it.
- **`JsCoreEvents`**, the Core's events in server scripts: `knowledgeGained`, `milestoneReached`, `teamStepReached`,
  `playerStepReached`, the life of an Operation on a data network (`operationCreated`, `operationStarted`,
  `operationCompleted`, `operationFailed`, `operationDiscarded`), a machine joining or leaving a data network
  (`networkJoined`, `networkLeft`), `roomFormed`, `roomChanged`, `roomFilled`, `roomLeaking`, `roomRemoved`,
  `structureCollided`, `structureParted`, `structureDocked`, `structureUndocked` and `dimensionHazard`, which a script
  cancels to spare the player this time.
- **A recipe function for every kind of processing machine**, the Core's and any mod's, under the kind's id.

```js
ServerEvents.recipes(event => {
  event.recipes.jsindustrial.macerating(['2x jscore:iron_dust'], ['#c:ores/iron']).ticks(200).energyPerTick(40)
  event.recipes.mymod.fusing(['mymod:alloy'], ['mymod:a', 'mymod:b']).knowledge('mymod:fusion')
})

JsCoreEvents.knowledgeGained(event => {
  if (event.learnt && event.knowledge == 'mymod:fusion') JsCore.reach(event.server, event.team, 'mymod:fusion_age')
})
```

A machine's recipe takes `fluidInputs`, `fluidOutputs`, `catalysts` and `knowledge` the same way; the matter of other
states, the byproducts and the temperature and pressure it runs between are written with `event.custom`, as the
recipe's file holds them ([Machines](MACHINES.md)).

### CraftTweaker

With CraftTweaker (21.0.39 or later), scripts import the same under `mods.jscore`:

```zenscript
import mods.jscore.Core;
import mods.jscore.Processing;

Processing.addRecipe("jsindustrial:macerating", "iron_dust", [<item:jscore:iron_dust> * 2],
    [<tag:items:c:ores/iron>], 200, 40);

events.register<mods.jscore.event.KnowledgeGained>(event => {
    if (event.learnt && event.knowledge == "mymod:fusion") {
        Core.reach(event.server, event.team, "mymod:fusion_age");
    }
});
```

`Processing.addRecipe` takes the kind, the recipe's name, what it makes and what it takes, then the ticks, the energy a
tick and the knowledge it asks for, each of the last three optional. Everything else a machine's recipe holds is written
with the recipe type's `addJsonRecipe`, and recipes are taken out with its `remove` and `removeByName`
(`<recipetype:jsindustrial:macerating>.removeByName("jsindustrial:iron_dust")`). `mods.jscore.Core` has the calls of
KubeJS's `JsCore`, by the same names, but for a player's team, which it takes from the player
(`Core.teamOf(player)`). The events are those of KubeJS, under `mods.jscore.event`: `KnowledgeGained`,
`MilestoneReached`, `TeamStepReached`, `PlayerStepReached`, `OperationCreated`, `OperationStarted`,
`OperationCompleted`, `OperationFailed`, `OperationDiscarded`, `NetworkJoined`, `NetworkLeft`, `RoomFormed`,
`RoomChanged`, `RoomFilled`, `RoomLeaking`, `RoomRemoved`, `StructureCollided`, `StructureParted`, `StructureDocked`,
`StructureUndocked` and `DimensionHazard`, which a script cancels.

### FTB Quests

With FTB Quests, a quest's tasks may be the Core's:

| Task | Met when | Its fields |
| --- | --- | --- |
| `jscore:knowledge` | the player's team knows a piece of knowledge; one learnt bit by bit fills the task bit by bit | `knowledge` |
| `jscore:milestone` | the team passes a milestone | `milestone` |
| `jscore:axis_step` | the team reaches a step along an axis, or one past it | `axis`, `step` (`jscore:hardware_era`, `legacy`) |

Each is checked once a second for every player of the team with the quest open, and as they log in. A task naming
something nothing declared waits rather than failing, so a quest written for a mod that is not installed is never met.

## Tools for building the pack

*Added 2026-10-21.*

### What a reload says

`/reload` reads the data packs again: recipes, loot tables, conditions, the unification, and every kind of data the
Core reads. Once it is done, the server's operators are told, and the log too, how many recipes and loot tables the
data holds now, what the unification made of them, and what waits for the game to start again, since a reload never
registers anything new:

- content packs changed on disk since the game started, whose blocks, items and materials are registered once;
- settings changed in the game that take hold only on a restart, by file.

When nothing waits, the last line says so.

### Dumps

`/jstech dump` writes what the game holds to `jstech-dump/` in the game's folder, each as a page of Markdown:

| Command | Writes |
| --- | --- |
| `/jstech dump materials` | `materials.md`: every material, its kind, the forms it has things of, and its properties |
| `/jstech dump forms` | `forms.md`: every form, what it is in the game, its common tags and the kinds it fits |
| `/jstech dump recipes` | `recipes.md`: every kind of recipe the data holds, how many, and what wrote them |
| `/jstech dump tags` | `tags.md`: every tag of items, blocks and fluids, with what is in it |
| `/jstech dump hazards` | `hazards.md`: the hazards to bodies, those items carry and the items that carry them, those of dimensions |
| `/jstech dump ids` | `ids.md`: every id the series' mods and the content packs registered, by namespace and registry |
| `/jstech dump reference` | `reference/`: the generated reference, a small website |
| `/jstech dump all` | all of the above |
| `/jstech dump permissions` | `jstech-permissions/`: the permissions and LuckPerms' groups ([Protection](PROTECTION.md)) |

### The generated reference

`/jstech dump reference` writes a website of plain files, to open from the disk or put online as it is: every kind of
data the Core reads, with the folder its files go in and one of the world's own files as the example; every data map
of every mod, with the file it goes in and a few of its values; every kind of recipe, with one of the world's recipes
written out; the conditions the Core gives data files; what a content pack declares; and every id. Each example is
written by the thing's own codec from the game that runs, so the reference says what this game reads, with every mod
it has installed.
