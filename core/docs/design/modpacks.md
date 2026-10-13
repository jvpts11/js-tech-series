# Modpacks

What the Core gives to those who put mods together into a pack: everything as data, scripts on top, one item per
material, tools to see what is there, resources and variants made for other mods' content, loot and recipes changed as
data, and a Core that steps aside where another mod already does the same job.

## What exists today

- **Data packs** change recipes, multiblock shapes, the rules of dimensions and the Core's data registries, with no
  code.
- **Settings** for every system, with their ranges, in files a pack ships ([Settings](../SETTINGS.md)).

## To build

The tools gather what KubeJS, CraftTweaker and ContentTweaker, Almost Unified and FTB Quests do with a library, in one
place.

### Everything as data, scripts on top

- **What a script can do, a data pack can do too.** Scripts are a convenience, never the only way.
- **Content declared by packs**, with no Java: blocks, items, machines, multiblocks, materials, vehicles, hazards of
  items and the physical properties of blocks, in data packs and resource packs.

### Integrations, through guarded adapters

- **KubeJS and CraftTweaker reach everything the Core has**: every kind of recipe (processing, multiblocks, reactions
  with temperature and pressure, locks of knowledge); materials, forms, properties, minerals and deposit types;
  multiblocks and machines as data; sources of radiation and hazards of items; how blocks seal; blocks' physical
  properties.
- **The Core's events open to scripts**: the life of Operations, changes in a network, rooms that fill or leak, hazards,
  structures that collide or split.
- **FTB Quests**: tasks completed by reaching a milestone, a piece of knowledge or a step of an axis.

### One item per material

- **Unification**: even when ten mods bring copper dust, outputs and drops prefer the Core's item, and the others'
  copies are replaced, with a setting for it, as Almost Unified does.
- **Conditions on recipes**: by installed mod, by the value of a setting, by knowledge.

### For those who build the pack

- **Reloading clearly**: `/reload` applies recipes, properties and tables; what needs a restart (registering new
  things) is said in a clear message.
- **A dump command**: `/jstech dump` writes every material, form, kind of recipe, tag, hazard and id to files.
- **A generated reference**: every data format with examples, and every id, taken from the registries themselves, and
  exported to the web.
- **Server changes reach the players**: recipes and data changed by scripts on the server are sent to the clients.


### Resources made as the game loads

In the spirit of Moonlight Lib:

- **Textures and models made when the game loads** for what the data generation can't know: ores in other mods' rocks,
  variants of other mods' materials.
- **Kept in a cache on disk**: made once, reused while nothing changes.
- **Resource packs stay on top**: a pack can replace any texture made this way.
- **Names made too**: "Chalcopyrite Ore in Marble", in the player's language.

### Variants for other mods' materials

In the spirit of Every Compat:

- **Variants made by themselves** for the woods, stones and metals other mods bring: microblocks, ores in their rocks,
  casings, and whatever each mod declares.
- **Found through the common tags**, with no list kept by hand.
- **Turned off per mod and per material**, so the game is not flooded.

### Changing loot and recipes as data

In the spirit of NeoForge's global loot modifiers and Polymorph:

- **Loot modifiers as data**: adding, taking away and swapping drops, with conditions (dimension, biome, tool,
  installed mod).
- **Recipes made conditional, removed or swapped** as data, with the conditions above.
- **Choosing between recipes that overlap**, at the crafting table and the furnace too: when two fit, the player
  chooses, and the choice is remembered. It steps aside for Polymorph when Polymorph is installed.
### Stepping aside for other mods

When an installed mod already does what a system of the Core does, the two shouldn't do the same job twice:

- **Each system of the Core declares the mods that do the same thing.** When one is installed, the Core **turns its own
  off by default** and, where it can, **hands its data to the other mod** through a guarded adapter (the colour of the
  Core's lights to a coloured-light mod, for one).
- **A setting per system** picks between stepping aside (the default) and keeping the Core's own.
- **The log says what stepped aside for what.**
- It is for the systems of **presentation and comfort**, where both do the same job:

  | Area | Mods that do the same |
  | --- | --- |
  | the physics of sound | Sound Physics Remastered, Dynamic Surroundings |
  | ambience | Dynamic Surroundings, AmbientSounds |
  | footsteps | Presence Footsteps |
  | dynamic light | LambDynamicLights |
  | coloured light and bloom | Shimmer |
  | connected textures | Continuity, CTM |
  | unification | Almost Unified |
  | choosing between recipes | Polymorph |

- **Where a game rule of a mod depends on the Core's system, it stays on**, since turning it off would break that mod:
  J's Space's sealed rooms stay, even with another mod's oxygen installed; at most a bridge is made between the two.
