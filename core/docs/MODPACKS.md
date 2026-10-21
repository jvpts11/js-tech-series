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
