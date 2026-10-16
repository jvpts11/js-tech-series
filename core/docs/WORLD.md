# The world

What J's Core gives a mod about the world itself: ores in veins that instruments can find, reservoirs of oil, gas and
brines that wells drain, structures that stand where the land suits them, wear by rule and grow over time, landmarks
players discover, finding things roughly at first and exactly later, markers on the maps, worlds that change after
they are made (generation in old chunks, a surface that changes with the climate, large edits, world events, fields
over the chunks), dimensions of your own with
rules for gravity, air, heat and weather, their terrain and biomes with climates in real values, the shapes of the
land (craters, volcanoes, canyons, lakes of any liquid and the rest), rock in layers, seas that freeze or boil away,
dimensions made while the game runs, data kept over areas of the world, and chunks kept loaded for their owners.

## The words

- A **chunk** is a column of the world 16 by 16 blocks wide. The game loads chunks near players and unloads the
  rest; a machine in an unloaded chunk stops.
- A **dimension** is a world of its own, like the Nether: its own terrain, sky and rules.
- **World generation** is the game making new terrain as players explore; it reads what to place from data files.
- A **biome** is a kind of place: a desert, a forest. It says the place's climate, its colours and what grows and
  lives in it.
- A **feature** is something world generation places once the ground is made: a tree, a vein of ore, a lake.
- A **structure** is a building or a group of them the world generation places whole: a village, a ruin. It is made
  of **templates**, blocks saved with the game's structure block, and a large one of many templates joined by
  **jigsaw blocks**, each template's jigsaws naming the **pool** of templates that may join it there.

## Ores and structures

*Added 2026-10-05.*

Declare what generates, and the data generation writes the files the game reads:

```java
public static final WorldGen.OreVein TIN_ORE = WorldGen.ore(rl("tin_ore"))
        .stone(() -> MyContent.TIN_ORE.get())
        .deepslate(() -> MyContent.DEEPSLATE_TIN_ORE.get())
        .vein(9)               // blocks in a vein
        .perChunk(6)           // veins tried per chunk
        .heights(-32, 64)      // between these heights
        .triangle()            // most common in the middle of the range
        .biomes(BiomeTags.IS_OVERWORLD)
        .declare();

public static final Structures.StructureSpec CAMP = WorldGen.structure(rl("camp"))
        .template(rl("camp"))  // data/<namespace>/structure/camp.nbt, saved with a structure block
        .biomes(BiomeTags.IS_FOREST)
        .spacing(40, 12)       // about one every 40 chunks, never closer than 12
        .salt(7_340_211)       // any number, different for each structure, so they do not line up
        .declare();
```

Declaring happens when the class holding these loads, so load it before the data generation runs (an empty
`declare()` method called from your mod's constructor). The files go under `data/<namespace>/worldgen/` (the
configured and placed features, the structure and its set, the template pool and its processor list) and a NeoForge
biome modifier puts ores into the biomes. A data pack can replace any of them. A scattered ore like this is for an
ore that is not a material's; a material's ore lies in veins (below). What else a structure can say is in
[Structures](#structures).

## Veins

*Added 2026-10-16.*

Ores do not lie scattered: they lie in **veins**. The world is cut into zones three chunks a side, and a zone holds
at most one vein, of a kind picked by the kinds' frequencies, dense at its middle and thinning to nothing at its edge,
so a player finds nothing in one place and hundreds of ores in the next. A kind of vein, a **deposit type**, is
declared in code and written as a data file:

```java
public static final ResourceLocation MAGNETITE = Deposits.deposit(rl("magnetite"))
        .mineral(SeriesMaterials.IRON, 6)        // the materials it brings together, by weight
        .mineral(MyMaterials.TITANIUM, 1)
        .heights(-48, 96)                        // where its middle lies
        .frequency(0.12)                         // in about 12 zones of 100
        .size(20, 6)                             // 20 blocks across from its middle, 6 up and down
        .density(0.55)                           // how much of its middle is ore
        .biomes(BiomeTags.IS_OVERWORLD)          // the biome at its middle
        .elsewhere(0.2)                          // a fifth as often outside those biomes; 0, the default, never
        .rocks(HostRocks.STONE, HostRocks.DEEPSLATE, HostRocks.GRANITE)   // left out: every rock
        .declare();
```

The data generation writes it to `data/<namespace>/jstech/deposits/magnetite.json`, which a data pack can change, and a
data pack can add kinds of its own there:

```json
{ "minerals": [ {"material": "jscore:iron", "weight": 6}, {"material": "mymod:titanium"} ],
  "min_y": -48, "max_y": 96, "frequency": 0.12, "radius": 20, "thickness": 6, "density": 0.55,
  "biomes": "minecraft:is_overworld", "rocks": ["jscore:stone", "jscore:deepslate", "jscore:granite"] }
```

- **A vein crosses rocks.** Each block of it becomes the ore of one of its materials in the rock it was: a vein through
  stone and deepslate lays iron ore in the stone and deepslate iron ore in the deepslate.
- **An ore lies only where a mod uses it.** A material's ore in a rock exists only when the game has it (iron in stone)
  or an installed mod declares it (`MaterialUses.ore(...)`, [Materials](CONTENT.md#materials)); a block of a vein
  whose material has no ore in that rock is left as it was. So any mod may declare a kind for materials it does not
  use itself.
- **The game's iron, copper and gold lie in veins.** The Core brings veins of them (`SeriesDeposits`: iron, copper,
  gold, the Nether's gold, and tin once a mod uses tin's ore), and while ores lie in veins the game's scattered iron,
  copper and gold are not laid. The large veins the game itself makes, of raw iron with tuff and of raw copper with
  granite, stay as they are.
- **The same for the same seed.** Where every vein lies is worked out from the world's seed alone, and each chunk lays
  only what reaches it, so a world generates the same on any number of threads, with C2ME or Distant Horizons.

### Prospecting

An instrument asks what is round it, and learns no coordinates:

```java
List<Prospecting.Prospect> near = Prospecting.prospect(level, pos, 64);      // nearest first
// each: the kind, the direction it lies in (a unit vector), and how strongly it shows, 0 to 1
Optional<Prospecting.VeinReading> here = Prospecting.sample(level, pos);     // standing in a vein
// its kind, the share of each material in it, and about how many blocks of ore it holds
```

A larger vein shows more strongly, and a nearer one. Both answer from the seed, for chunks made or not, so mining a
vein out does not hide it.

### Settings

In `jstech-balance.toml`, section `veins`:

| Setting | Means | The default |
| --- | --- | --- |
| `enabled` | Ores lie in veins. Off, no vein is laid and the game's scattered ores come back. | on |
| `rarity` | How many times as often each kind is found, by its id: `"jscore:iron" = 0.5`. 0 lays none. | none named |
| `spread` | How many times as large each kind is, never wider than its zone. | none named |
| `density` | How many times as dense each kind is, at most all ore at its middle. | none named |
| `without_ore` | The materials whose ore is never laid: `["jscore:tin"]`. Their veins keep their other ores. | none |
| `try_moving_other_ores` | Move other mods' scattered ores of the common ores tag (`c:ores`) into veins of their own. | off |

Moving other mods' ores works with an ore a mod lays as data, as most do: its feature stands aside and a vein of its own
replaces what it replaced with what it laid, so each block still takes its rock's variant. A mod that lays its ores
its own way is not seen, hence "try". Only chunks made after a change are laid differently.

## Reservoirs

*Added 2026-10-16.*

Oil, natural gas and brines are things in the ground: a **reservoir** is a lens of porous rock soaked in them, under a
dome of **cap rock** that holds them in. Each block holds an amount; a well drains them one at a time, and a block that
holds no more turns back into plain rock. A reservoir lays no liquid, so digging into one floods nothing, and shows it.

```java
public static final ResourceLocation OIL = Reservoirs.reservoir(rl("oil"))
        .fluid(rl("crude_oil"))                                  // what it holds
        .rocks(rl("oil_sand"),                                   // the soaked rock
                ResourceLocation.withDefaultNamespace("sandstone"),      // what a drained block turns into
                ResourceLocation.withDefaultNamespace("smooth_basalt"))  // the cap rock
        .perBlock(1000)              // millibuckets a block
        .heights(-40, 20)            // where its middle lies
        .frequency(0.25)             // in a quarter of the zones, eight chunks a side
        .size(28, 5).capping(2)      // 28 blocks across, 5 up and down, under 2 blocks of cap
        .biomes(Tags.Biomes.IS_DESERT).elsewhere(0.3)
        .declare();
```

The data generation writes it to `data/<namespace>/jstech/reservoirs/oil.json`, which a data pack can change, and a data
pack can add reservoirs of its own. Where each lies is worked out from the world's seed, so every mod finds the same
ones, and what has been drawn from each is kept in the dimension's data over areas (`Reservoirs.DRAWN`), so every mod
reads the same blocks left:

```java
List<Reservoirs.Reservoir> near = Reservoirs.near(level, pos, 128);          // nearest first
Optional<Reservoirs.Held> held = Reservoirs.at(level, pos);                  // a block of one, and what it holds
int drawn = Reservoirs.drain(level, pos, 250, false);                        // millibuckets taken
Optional<BlockPos> next = Reservoirs.nearestHolding(level, reservoir, well); // the next block a well drains
long left = Reservoirs.left(level, reservoir);                               // about how much is left in it
```

A reservoir replaces only the rock it says (the overworld's stone unless `replaces(...)` names another), never air, a
liquid or another block, so caves and veins cut through it stay as they were. It is laid before the ores. The Core
brings the kit; the reservoirs themselves are a mod's: J's Industrial's oil, J's Geology's natural gas, the brines of
desert salt flats.

To add a feature of your own to every biome of every dimension, as the reservoirs are, write a biome modifier of the
Core's `jscore:everywhere` kind:

```json
{ "type": "jscore:everywhere", "features": "mymod:fault_lines", "step": "underground_structures" }
```

## Structures

*Added 2026-10-16.*

A structure is declared in code, and the data generation writes the files the game reads: its start pool, the
processor list that wears its templates, the structure itself, of the Core's kind `jscore:placed`, and its set. A data
pack can change any of them.

```java
public static final Structures.StructureSpec OUTPOST = Structures.structure(rl("outpost"))
        .element(rl("outpost/plaza"), 1)        // its start: a template, picked by weight among several
        .size(6).reach(96)                      // its jigsaws lay out six pieces deep, up to 96 blocks out
        .biomes(BiomeTags.IS_OVERWORLD)
        .spacing(48, 20).frequency(0.6F)        // one tried in each 48 chunks, in six cells of ten
        .minDistance(1500).clearOf(4)           // none within 1,500 blocks of the origin or 4 chunks of another
        .fit(new StructureFit(12, 6, 60, 180, true, 0))
        .foundation(() -> Blocks.COBBLESTONE)
        .worn(() -> Blocks.IRON_BLOCK, () -> MyBlocks.RUSTED_IRON.get(), 0.4)
        .grows(new Growth(4, 1, 3))
        .declare();
public static final Structures.PoolSpec HOUSES = Structures.pool(rl("outpost/houses"))
        .element(rl("outpost/house_a"), 3).element(rl("outpost/house_b"), 1)
        .worn(() -> Blocks.GLASS, () -> MyBlocks.CRACKED_GLASS.get(), 0.25)
        .declare();
```

### Where it is placed

- **Spread** (`jscore:spread`), the default: the world is cut into cells of `spacing` chunks and one is tried in
  each, never closer than `separation` to the next, in as many cells as `frequency` says. Besides the game's own
  spread, `minDistance` keeps it away from the world's origin, where players start, and `clearOf` keeps it that many
  chunks from every other structure, of any mod and of the game, so two never overlap. When two structures both keep
  clear of others, the one of the greater salt yields, so one of them still stands.
- **Fixed** (`jscore:fixed`): `.at(chunk)` places it at that chunk and nowhere else, as a landmark's structure is.
  Call it again for several such places.

### Where the land suits it

`fit(new StructureFit(footprint, slope, minY, maxY, dry, sunk))` reads the ground at the structure's middle and its
four corners, `footprint` blocks out, and refuses a place where the ground rises more than `slope` blocks across them
(-1 for any land), lies below `minY` or above `maxY`, or, for `dry`, lies under water. `sunk` sets it that many blocks
into the ground: half its height for one half buried, all of it for one buried whole. Without a fit, it stands on any
land, as the game's own structures do.

### Fitting into the ground

- `foundation(block)` lays a column of that block under each block of the structure's lowest layer, down to the
  ground, so a building on a slope stands on stone rather than hanging over air. A structure on foundations stands
  level with the highest of its ground, so nothing of it is buried; one without stands on the ground at its middle.
- `terrain(...)` says how the ground round it is shaped as the world generates: the game's `BEARD_THIN` by default
  (the ground raised under it and cleared over it), `BEARD_BOX`, `BURY`, `ENCAPSULATE` or `NONE`.

### Large layouts

A structure of many templates starts from its start pool and its jigsaws join pieces from the pools they name, up to
`size` pieces deep (at most 20) and `reach` blocks from its start (at most 128). `streets()` is for a layout whose low
pieces hold the taller ones inside their own space, as the game's villages hold their houses beside their streets:
without it, none of those fits. A layout of the game's village pieces needs it.

### Wear

`worn(from, into, chance)` wears blocks of `from` into `into`, as often as `chance` says, as the structure is
placed: iron rusts, glass cracks, planks rot, without a worn copy of the template drawn by hand. The worn block keeps
the facing and the other properties of the block it replaces where it has them, so worn stairs face the same way.
`age(share)` shows that share of the wear, 0 for a new structure. Each place wears the same each time it is placed.
In data, the processor is `jscore:weathering`:

```json
{ "processor_type": "jscore:weathering", "age": 1.0, "rules": [
    {"test": {"predicate_type": "minecraft:block_match", "block": "minecraft:iron_block"},
     "worn": {"Name": "mymod:rusted_iron_block"}, "chance": 0.4} ] }
```

### Settlements that grow

`grows(new Growth(stages, built, days))` makes a settlement of it. The whole layout is laid out as it generates, the
size it will reach, but only its first `built` of `stages` stand at first, its middle; the rest is kept in its plan.
From the day its chunk first loads, as a player comes near, a stage is built every `days` days of the world's time,
from its middle out, into the chunks that are loaded; a chunk loaded later catches up with the stages it missed. It
never grows past the size it was laid out at. `Settlements.of(level)` lists a dimension's settlements, by keys of the
form `<structure>@<chunk x>,<chunk z>`, and `Settlements.advance(level, key, days)` makes one older, as a story
moving time on would.

### Placing one yourself

`StructurePlacer.place(level, id, chunk)` places a structure where a mod says, a quest's ruin or a crashed probe, as
the world would have: laid out from that chunk by its own rules (its fit, its sinking, its wear, its foundations),
built into the chunks it covers, and known to the world afterwards, so the instruments find it, what it remembers is
kept, and a settlement grows. The game's own `/place structure` command builds the blocks and forgets the
structure; this keeps it. It does not ask the structure's biomes, since the caller chose the place.

### What a structure remembers

`StructureStates` keeps, for each structure that stands, what it remembers of itself, by any place inside it: a
chamber opened, a terminal woken, a vault emptied. Every change is posted on the game's bus as
`StructureStates.Changed`, so the blocks of a structure act together: a terminal powered up wakes the doors of the
base it stands in.

```java
StructureStates.at(level, pos).ifPresent(base -> StructureStates.update(level, base, state -> {
    state.putBoolean("awake", true);
    return state;
}));
```

A **knowledge seal** (`KnowledgeSealBlock`) closes a chamber until the player's team knows what it asks: it says
what it waits for, and once the team knows it, a touch opens it and the structure remembers the seal open. What it
asks is its block entity's `"knowledge"`, set in the template, so one block seals any chamber. A mod registers its own
seal, in its own look, with its own block entity type; make it unbreakable and proof against blasts in its properties.

### Loot of another mod's item

A loot table entry of type `jscore:optional_item` names an item by id and gives it only when the mod that adds it is
installed; without it, the entry gives nothing and breaks nothing. Its weight, quality, conditions and functions are
an item entry's own:

```json
{ "type": "jscore:optional_item", "name": "othermod:titanium_ingot", "weight": 3,
  "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 4}}] }
```

In a loot table's data generation, `OptionalItemEntry.of(id)` makes the same entry.

## Places

*Added 2026-10-16.*

### Landmarks

A **landmark** is a named place of a world: a mountain, a crater, a ruin. A player who comes near one discovers it,
for themselves and their team; the discovery is noted, posted on the game's bus as `Landmarks.Discovered`, and shown
as a notification that points at it. Each landmark discovered is a marker on the maps by itself.

```java
public static final TextKey OLYMPUS = TextKey.of("mymod.landmark.olympus_mons", "Olympus Mons");
public static final ResourceLocation OLYMPUS_MONS = Landmarks.landmark(rl("olympus_mons"), OLYMPUS)
        .in(MARS).at(1200, 140, -800).radius(160)      // discovered from 160 blocks
        .structure(rl("olympus_base"))                   // the structure that stands at it, if any
        .declare();
```

A data pack can write over a landmark or add one with a file under `data/<namespace>/jstech/landmarks/`:

```json
{ "name": "mymod.landmark.olympus_mons", "english": "Olympus Mons", "dimension": "mymod:mars",
  "x": 1200, "y": 140, "z": -800, "radius": 160 }
```

`Landmarks.discovered(server, player)` gives what a player or their team discovered, `Landmarks.discover(player, id)`
discovers one as coming near does, and `Landmarks.forget(server, player, id)` lets them discover it again.

### Finding things

`Finding.find(level, from, target, precision)` finds the nearest thing of a kind, a structure of a tag, a biome of a
tag, a landmark or a vein of a deposit type, with a precision, so an instrument can point roughly at first and exactly
later: at precision 0 the answer is a place within 512 blocks of the thing, each level halves that, and at
`Finding.EXACT` it is the thing's own place. The rough answer is the same each time it is asked, so the needle does
not wander, and never further off than it says.

```java
Optional<Finding.Found> near = Finding.find(level, player.blockPosition(),
        new Finding.StructureTarget(MyTags.RUINS), 2);  // within 128 blocks
near.ifPresent(found -> say(found.near(), found.within()));
```

It finds structures at fixed places too, which the game's own search passes over.

## Map markers

*Added 2026-10-16.*

A **marker** is a point or an area on the maps, with a label and a colour, in a **layer** the map shows or hides as
one. Each player has their own markers, and their team has markers every member sees; both are kept with the world
and sent to each player as they change.

```java
public static final MarkerLayer RUINS = MapMarkers.layer(rl("ruins"), RUINS_NAME, 0xC08040, false);

MapMarkers.show(player, Marker.point("ruin_1", RUINS.id(), level.dimension(), pos, name));        // the player's
MapMarkers.share(server, player.getUUID(), Marker.area("survey", RUINS.id(), dim, chunks, label)); // the team's
MapMarkers.hide(server, player.getUUID(), "ruin_1");
```

A marker of the same id takes the place of the old. `Marker.coloured(rgb)` gives a marker its own colour instead of
its layer's. The Core's layers are `MapMarkers.LANDMARKS`, which holds the landmarks a player or their team
discovered, and `MapMarkers.FOUND`, for places an instrument found.

The markers reach the map mods through the Core's adapters, each loaded only while its mod is installed:

- **JourneyMap**: a point as a marker in its colour, labelled; an area as its chunks shaded; each layer a group.
- **Xaero's Minimap**, and so Xaero's World Map: each marker a waypoint of the nearest of Xaero's colours.
- **FTB Chunks**: each marker an icon of its colour on the map and the minimap, its label in the tooltip.

Without a map mod, the series' own maps show them: hand `ClientMarkers::forMapView` to a `MapView` as its markers.
Another map mod's adapter implements `IMapAdapter` and is added with `ClientMarkers.add(adapter)`.

## Worlds that change

*Added 2026-10-17.*

A world is not finished once it is generated: a mod added later lays its ores into the land already explored, ice
thaws where the climate warms it, a blast leaves a crater, a storm comes, fallout drifts. Each of these runs within a
budget of its own, so the server never stalls for it ([Diagnostics](DIAGNOSTICS.md#budgets-slowing-down-gracefully)).

### Generation in old chunks

Chunks made before a mod, or a new kind of vein, was part of the world never had what it generates. `Retrogen` lays it
into them:

```java
Retrogen.feature(rl("tin_veins"), MyWorldGen.TIN_VEINS_PLACED);       // a placed feature, as generation places it
Retrogen.declare(rl("meteor_craters"), (level, chunk, random) -> Craters.paint(level, chunk, random));
```

Each chunk keeps which of this work it has had, saved with it. A chunk made while the work is part of the world's
generation is marked as it is made, so only old ones get it. An old chunk is noted as it loads and generated later,
a few each tick, once its eight neighbours are loaded too, so the work may reach a little into them. Its random is the
same for the same chunk and work, so a world generates alike however it is played. `Retrogen.forget(chunk, id)` lets
a chunk have the work again.

### The surface changing by itself

A **live change** is a change the surface goes through by itself, as the climate and the weather say: ice thawing
where it is warm, water gathering in a basin in the rain, grass drying to dirt in the heat. The Core declares none: a
world changes only as its mods say.

```java
LiveChanges.change(rl("ice_thaws")).test(new BlockMatchTest(Blocks.ICE))
        .becomes(Blocks.WATER.defaultBlockState()).warmerThan(275).sky().chance(0.2).declare();
LiveChanges.change(rl("puddles")).test(new BlockMatchTest(Blocks.AIR)).above()
        .on(new TagMatchTest(BlockTags.DIRT)).becomes(Blocks.WATER.defaultBlockState())
        .raining(true).sky().basin().chance(0.05).declare();
```

Once a second, a column of each chunk within six chunks of each player is looked at, and the first change whose
block and conditions all hold at its topmost block (or the block above it, for `above()`) happens, as often as its
chance says. The conditions are how warm the place is in kelvin (`warmerThan`, `colderThan`), the rain (`raining`),
the open sky (`sky`), a basin (`basin()`: each block beside solid, or already what it becomes), the block under it
(`on`), the seasons and the dimensions. A data pack writes over or adds one under `data/<namespace>/jstech/live_changes/`:

```json
{ "test": {"predicate_type": "minecraft:block_match", "block": "minecraft:ice"},
  "becomes": {"Name": "minecraft:water", "Properties": {"level": "0"}}, "chance": 0.2, "min_kelvin": 275,
  "sky": true }
```

`LiveChanges.setBiome(level, box, biome)` changes the biome of every place in a box, as terraforming or a climate
shifting would: the players who see it are sent the new biomes at once, so the grass, the water and the sky change
colour, and the climate there is the new biome's from then on.

### Large edits

A large edit (a crater blasted, a city levelled, a valley terraformed) is planned away from the server's thread and
set on it a share of each tick:

```java
WorldEdit crater = WorldEdits.plan(level, BoundingBox.fromCorners(from, to), blocks -> {
    EditPlan.Builder plan = EditPlan.builder();
    // read blocks.get(x, y, z) freely: a snapshot taken as the edit began
    return plan.build();
});
crater.whenOver(edit -> report(edit.state()));     // DONE, STOPPED or FAILED
```

The planner reads a `RegionSnapshot`, the box's blocks copied on the server's thread as the edit begins, so it may
take its time while the world goes on. The plan's blocks are set chunk by chunk, the edits under way taking turns in
the order they began, as structures are set: without waking their neighbours, unless the plan asks for `updates()`.
An edit says how far it has got (`progress()`), can be stopped where it is (`stop()`), and a planner that throws sets
nothing. `WorldEdits.apply(level, plan)` sets a plan made already.

### World events

A **world event** is something the world goes through by its schedule or by chance: a meteor shower, a solar storm,
an earthquake.

```java
public static final ResourceLocation SOLAR_STORM = WorldEvents.event(rl("solar_storm"), MyTexts.SOLAR_STORM)
        .in(Level.OVERWORLD).chance(0.05)           // on one day in twenty, or .every(7, 20) for 20:00 each week
        .warning(6000).instrument(MyTags.MAGNETOMETERS)
        .lasts(12000).effect(rl("aurora")).declare();
```

Once a day each kind of event a dimension may have is rolled for, or found due by its schedule, and planned. Those
carrying its instrument anywhere in their inventory are warned as it nears, with how long is left; everyone it covers
is told as it begins, unless it is `quiet()`; its effect plays round them while it lasts; and it ends. Each step is
posted on the game's bus as `WorldEventStage.Warned`, `Started` and `Ended`, where a mod does what its event does:

```java
NeoForge.EVENT_BUS.addListener(WorldEventStage.Started.class, stage -> {
    if (stage.event().type().equals(SOLAR_STORM)) {
        Grids.surge(stage.level());
    }
});
```

An event covers the whole dimension, or, with `radius(blocks)`, only so far round a place picked near a player
standing in its `biomes`. It happens only in its dimensions and its seasons, one of a kind at a time. What is planned
and under way is kept with each dimension, so a restart loses nothing; `WorldEvents.planned(level)`,
`active(level)` and `activeAt(level, id, pos)` read it, for any mod and the series' computers. Operators list, start,
plan, put off and stop events with `/jstech event` ([Commands](COMMANDS.md)), and the server's settings switch them
off, make each happen more or less often, keep some from ever happening by themselves, or make some harmless
(`world_events` in `jstech-balance.toml`): a harmless event still comes, warns and plays its effects, and its stages
say `consequences()` is false, so its mod leaves the world as it is. A data pack writes over or adds one under
`data/<namespace>/jstech/world_events/`:

```json
{ "name": "mymod.event.solar_storm", "english": "Solar storm", "dimensions": ["minecraft:overworld"],
  "chance_per_day": 0.05, "warning": 6000, "instrument": "mymod:magnetometers", "duration": 12000,
  "effect": "mymod:aurora" }
```

### Fields over the chunks

A **field** is a value over the chunks of each dimension that spreads to the neighbouring chunks and fades: fallout,
pollution, a cloud of spores.

```java
public static final ChunkField FALLOUT = ChunkFields.field(rl("fallout"))
        .spread(0.1).decay(0.02).every(40).synced().declare();

FALLOUT.add(level, new ChunkPos(pos), 500);
float here = FALLOUT.get(level, new ChunkPos(player.blockPosition()));
```

Every `every` ticks each chunk keeps a share of its value and hands `spread` of it evenly to its four neighbours, all
of it fading by `decay`; what falls below the least worth keeping (`least`) is gone. The step is worked out away from
the server's thread, and what is added meanwhile is kept. A field's values are saved with each dimension. A synced one
is sent to each player for the chunks round them, eight out each way, whenever what they would see changes, and
read on their game with `ChunkFieldsClient.value(field, chunk)`, for an overlay, an instrument or the sky.

## Dimensions

*Added 2026-10-05.*

```java
public static final WorldGen.DimensionSpec MOON = WorldGen.dimension(rl("moon"))
        .layer(() -> Blocks.BEDROCK, 1)
        .layer(() -> Blocks.STONE, 6)
        .layer(() -> Blocks.LIGHT_GRAY_CONCRETE, 1)
        .biome(Biomes.DESERT)
        .heights(0, 64)        // lowest y, and how many blocks tall
        .fixedTime(18_000)     // always night
        .ambientLight(0.1F)
        .declare();
```

A flat dimension is made of the layers, bottom first; `terrain(...)` gives it ground of hills and seas declared in
code (below), and `noise(...)` ground from a noise setting of its own. `noSkylight`, `ceiling`, `ultraWarm` (water
evaporates, as in the Nether), `effects` (how the sky looks) and `unnatural` (compasses spin, beds explode) say the
rest. The data generation writes its dimension type and its dimension.

### Terrain

*Added 2026-10-16.*

A terrain is the ground of a dimension, declared in code:

```java
public static final Terrains.TerrainSpec SALT_PLAINS = Terrains.terrain(rl("salt_plains"))
        .heights(-64, 256)       // lowest y, and how many blocks tall
        .ground(72)              // the land lies at this height on the whole
        .hills(18, 320)          // hills rise and valleys fall up to 18 blocks, each about 320 blocks wide
        .rough(0.1)              // a little overhang and arch; 0, the default, for none
        .sea(63)                 // the liquid fills the low ground up to here
        .stone(() -> Blocks.STONE).liquid(() -> Blocks.WATER)
        .bedrock(5)              // blocks of bedrock at the bottom, fading upwards
        .surface(() -> Blocks.SAND, () -> Blocks.SANDSTONE, 4)                    // top, under it, how deep
        .surface(SALT_STEPPE.key(), () -> Blocks.CALCITE, () -> Blocks.DRIPSTONE_BLOCK, 3)  // a biome's own
        .declare();
```

The data generation writes its noise settings and the noises they sample, under `data/<namespace>/worldgen/`. A
surface's top lies where the ground meets the air; under a liquid the floor is the block under it.

### Biomes and their climates

*Added 2026-10-16.*

```java
public static final WorldBiomes.BiomeSpec SALT_STEPPE = WorldBiomes.biome(rl("salt_steppe"))
        .name("Salt Steppe")
        .climate(9.5, 240)       // a mean of 9.5 °C over the year, 240 mm of rain a year
        .seasons(16)             // summer and winter 16 °C either side; left out, the climate decides
        .sky(0x9DB8D9).fog(0xD8D2C4).water(0x5E9FA8, 0x050533).grass(0xB8B07A)
        .feature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN)
        .spawn(MobCategory.CREATURE, () -> EntityType.GOAT, 8, 2, 4)
        .declare();

public static final WorldGen.DimensionSpec STEPPES = WorldGen.dimension(rl("steppes"))
        .terrain(SALT_PLAINS)
        .biome(SALT_STEPPE.key(), 0.5F, 0F)      // where the terrain is warm
        .biome(FROST_FLATS.key(), -0.5F, 0F)     // and where it is cold
        .declare();
```

A dimension with a terrain spreads its biomes by two noises of the terrain's, its warmth and its wetness, each from
-1 to 1: every place takes the biome whose numbers are nearest its own. The data generation writes each biome, its
English name, and its climate to the data map `data/jscore/data_maps/worldgen/biome/climate.json`, which a data
pack can change or add to, for any biome of any mod:

```json
{ "values": { "mymod:salt_steppe": { "temperature": 9.5, "seasons": 16, "rain": 240 } } }
```

`BiomeClimates.of(biome)` reads a biome's climate: its own, or one worked out from the game's values for it
(plains are a mild 14 °C with 800 mm of rain). `BiomeClimates.celsius(level, pos)` is how warm a place is now: the
climate's mean, swung by the seasons of the world's calendar (warmest in the middle of summer), and cooler the
higher above the sea, about 6.5 °C a kilometre. Where a biome has a climate of its own, [heat](HEAT.md) meets that
air; every other biome keeps its dimension's, as it always did. The Overworld's own biomes are left as the game
makes them.

## Shapes of the land

*Added 2026-10-16.*

### Landforms

A landform is a shape the land takes, declared in code and laid by the Core's feature `jscore:landform`:

```java
public static final Landforms.LandformSpec CRATERS = Landforms.landform(rl("craters"), LandformKind.CRATER)
        .cell(192)               // at most one in each square 192 blocks across
        .chance(0.6)             // in six squares of ten
        .radius(8, 40)           // each 8 to 40 blocks in radius
        .height(0.3)             // as deep as 0.3 of its radius
        .surface(() -> Blocks.GRAVEL)          // its floor
        .scatter(() -> Blocks.COBBLESTONE)     // its ejecta
        .biomes(MY_MOON_BIOMES)                // the biomes it is drawn in
        .declare();
```

| Kind | What it is | `surface` | `fill` | `special` | `scatter` |
| --- | --- | --- | --- | --- | --- |
| `crater` | A bowl, a raised rim, a blanket of ejecta with rays and boulders; a central peak in a large one. | floor | rim | | ejecta |
| `shield_volcano` | A broad, gentle cone. | slopes | body | summit's lava | |
| `stratovolcano` | A steep cone. | slopes | body | summit's lava | |
| `mesa` | A flat top over cliffs, its body in bands (`bands(...)`). | top | body | | |
| `dunes` | Ridges, gentle on the windward side and steep on the lee. | top | body | | |
| `canyon` | A long winding cut with steep walls. | floor | | in its bottom | |
| `river` | A shallow winding channel of any liquid. | bed | | liquid | what it freezes to |
| `lake` | A bowl of any liquid. | bed | bank | liquid | what it freezes to |
| `lava_tube` | A tunnel under the ground, open to the sky here and there. | | | its floor | |
| `ice_sheet` | A sheet of ice over the land. | snow on top | | ice | |
| `geyser` | A mound round a vent down to water over magma, which bubbles. | mound's top | mound | water | magma |
| `sinkhole` | A shaft down into the ground. | floor | | in its bottom | |
| `salt_flat` | The land made level, cracked in a net. | | | salt | cracks |

`height` is a share of the radius: a crater's depth, a mesa's height, a lava tube's bore, a river's depth, a
geyser's vent. A river's or a canyon's radius is half its length. A lake's or a river's liquid freezes where it is
colder than `liquidRange(freezes, boils)` says, in kelvin, and boils away where it is hotter; water's 273.15 and
373.15 are the default, and a lake of lava, of brine or of liquid methane says its own.

Where they lie is worked out from the world's seed alone: the world is cut into squares a `cell` across, each
holding at most one, at a place, size and turn of its own. So any chunk finds every landform that reaches it
without asking another, and a landform of any size is laid a chunk at a time, on any thread, the same every time
for the same seed. Each kind is laid in the chunks of its biomes; `middle(tag)` lays only those whose middle lies in
the tag's biomes, drawn whole into every biome of `biomes(...)` they reach, so one at a biome's edge is not cut
off there.

`LandformPainter.paint(level, config, instance, base, heightmap, x0, z0, x1, z1)` lays one where code wants it, a
command or a test.

### Rock in layers

```java
public static final Landforms.StrataSpec MOON_ROCK = Landforms.strata(rl("moon_rock"))
        .sequence(Landforms.layer(() -> Blocks.TUFF, 10), Landforms.layer(() -> Blocks.CALCITE, 6))
        .sequence(Landforms.layer(() -> Blocks.BASALT, 14))
        .region(384)             // regions about 384 blocks wide, each holding one of the sequences
        .wobble(4)               // the layers' boundaries rise and fall 4 blocks across the land
        .replaces(BlockTags.BASE_STONE_OVERWORLD)
        .biomes(MY_MOON_BIOMES)
        .declare();
```

Each column takes its region's sequence and, from the top of the ground down, puts each layer's rock in place of
the rock it replaces; below the last layer the rock is left as it was. Regions meet along wavy borders. It is laid
after the landforms, through what they raised, and before the ores, which then lie in the layers.

### Seas

```java
public static final Landforms.SeaSpec SEAS = Landforms.sea(rl("seas"))
        .liquid(() -> Blocks.WATER).frozen(() -> Blocks.PACKED_ICE)
        .range(271.35, 373.15)   // sea water freezes a little under fresh water's 273.15 K
        .biomes(MY_MOON_BIOMES)
        .declare();
```

The terrain fills its low ground with its liquid up to its sea; where it is colder than the liquid freezes, the
sea's top freezes, and where it is hotter than it boils, the sea is gone down to its floor. It is the last thing
laid.

The data generation writes each landform, rock in layers and sea as a configured feature, a placed feature and a
NeoForge biome modifier under the same name, so a data pack can change or remove any of them.

### Runtime-only dimensions

*Added 2026-10-05.*

`.runtimeOnly()` writes the dimension type but not the dimension itself, so a world does not load it as it starts;
only copies of it made while the game runs exist (below). Use it for a dimension that is a template, and for every
dimension of a test mod. The reason is the game's own: a world with more than its three dimensions is marked as
using experimental settings, and the game warns about it each time the world is opened.

### Rules of a dimension

*Added 2026-10-05.*

A dimension's rules are a data file, `data/<namespace>/dimension_rules/<path>.json` for the dimension
`<namespace>:<path>`:

```json
{ "gravity": 0.16, "breathable": false, "temperature": -120, "weather": "clear", "pressure": 0 }
```

| Rule | Means | Range | The overworld's |
| --- | --- | --- | --- |
| `gravity` | The pull, as a share of the overworld's. | 0 to 10 | 1.0 |
| `breathable` | Whether the air can be breathed. | yes or no | yes |
| `temperature` | Degrees Celsius. Above 60 burns, below -30 freezes. | -273.15 to 10,000 | 15 |
| `weather` | `natural`, `clear`, `rain` or `thunder`. | | `natural` |
| `pressure` | Kilopascals. | 0 to 100,000 | 101.3 |

Every rule is optional. The Core applies them: entities fall as the gravity says; once a second a player in air
they cannot breathe, or in heat or cold past the limits, is hurt; the weather follows the rule. Before hurting a
player it posts a `DimensionHazardEvent` on the game's event bus, and cancelling it spares the player, which is
how a space suit of another mod protects its wearer:

```java
NeoForge.EVENT_BUS.addListener((DimensionHazardEvent event) -> {
    if (wearsSuit(event.player())) {
        event.setCanceled(true);
    }
});
```

`DimensionRulesData.of(level)` reads a dimension's rules from code, on either side.

### Dimensions made while the game runs

*Added 2026-10-05.*

```java
ServerLevel base = RuntimeDimensions.getOrCreate(server,
        ResourceKey.create(Registries.DIMENSION, rl("moon_base_1")),
        MOON.id());                                     // the declared dimension it copies
```

The copy has the template's terrain and rules, and is made again every time the server starts until it is
removed with `RuntimeDimensions.remove(server, key)`. Asking for one that exists gives it back. An operator can do
the same with `/jstech dimension create <id> <template>` and `remove <id>` ([Commands](COMMANDS.md)).

## Data over areas of the world

*Added 2026-10-05.*

A **region index** keeps values over boxes of the world, saved with each dimension, and finds them by place:
claims, zones, the area a machine covers.

```java
public static final RegionIndex<Claim> CLAIMS = RegionIndex.declare(rl("claims"), Claim.CODEC);

CLAIMS.put(level, "base", Box.around(x, y, z, 16), claim);
List<SpatialIndex.Entry<String, Claim>> here = CLAIMS.containing(level, pos);
SpatialIndex.Entry<String, Claim> closest = CLAIMS.nearest(level, pos, 64);   // null when none is that near
List<SpatialIndex.Entry<String, Claim>> inside = CLAIMS.within(level, Box.between(x1, y1, z1, x2, y2, z2));
```

Each entry carries its key, its box and its value. `get(level, key)` and `remove(level, key)` reach one by key.

It files each box under every 512 by 512 region it touches, so finding what is at a place reads only that place's
region, however many boxes the world holds. A box wider than 4,096 regions is kept apart and searched every time.
`SpatialIndex` is the same index without the world, for code that keeps its own.

## Keeping chunks loaded

*Added 2026-10-05.*

A machine that must work while nobody is near (a quarry, a station far away) asks the Core to keep chunks loaded
for its owner:

```java
LoadOutcome outcome = ChunkLoaders.load(level, machinePos, ownerUuid, new ChunkPos(machinePos), true);
// when the machine is broken:
ChunkLoaders.releaseAll(level, machinePos);
```

- `true` keeps the chunk **ticking**: machines and entities in it work as if a player were near. `false` only keeps
  it loaded.
- Each owner may keep a number of chunks across every dimension: 25 unless the server says otherwise
  (`world.chunks_per_owner` in `jstech-balance.toml`, 0 to 4,096). Past it, `load` answers `LIMIT_REACHED`.
- The Core remembers what each source keeps. A source that holds nothing when the world loads (its block is gone)
  has its chunks let go.
- `/jstech chunks <player>` shows what a player keeps loaded, and releases it ([Commands](COMMANDS.md)).

## What can go wrong

- **The world opens with "Worlds using Experimental Settings are not supported".** A mod declares more dimensions
  than the game's three. Make the ones that are templates `runtimeOnly()`.
- **An ore never generates.** Its biome tag holds no biome where you are looking, its heights are out of the
  world, or the data generation has not run since it was declared.
- **A structure never generates.** Its template file is missing from `data/<namespace>/structure/`, its spacing
  is very large, its fit refuses the land where you are looking (too steep, too low or high, under water), it keeps
  clear of another structure there, or the world was made with structures off.
- **A village of the game's pieces has streets and no houses.** Its declaration lacks `streets()`.
- **A structure on a slope is half buried.** It has no foundations, so it stands on the ground at its middle: give it
  `foundation(...)`, or a `fit` with a smaller `slope`.
- **A settlement never grows.** No player has come near it yet, so its days have not started, or its chunks are not
  loaded; it catches up when they load.
- **A seal does not open.** The player's team does not know what its block entity asks.
- **Old chunks never get a mod's new generation.** Their neighbours are never all loaded, or they were made with it
  already: a chunk made while the work is declared is marked as having had it.
- **The surface never changes.** No mod declares a live change, no player is near enough (six chunks), or a
  condition never holds there: the climate, the rain, the open sky, the block under it.
- **A world event never comes.** Its chance is small, it is out of season, its dimension is not one of its, the
  server's settings keep it off, or, for one round a place, no player stands in its biomes. Start it with
  `/jstech event start` to see it.
- **A field stops spreading.** Its `every` is 0, so only `stepNow` moves it.
- **A machine stops when you walk away.** It did not ask for its chunk, or its owner has reached the limit.
- **Players in a dimension take damage.** Its rules say the air cannot be breathed, or it is too hot or cold. A
  mod's suit must cancel `DimensionHazardEvent` to protect them.
- **A landform never generates.** Its biomes hold none of the biomes where you are looking, its `cell` is very
  large or its `chance` small, or the data generation has not run since it was declared.
- **A landform stops at a biome's edge.** The biome beyond is not one of its `biomes`. Add it there too, and keep
  where its middle may lie with `middle(...)`.
- **A lake is dry, or frozen solid on top.** The place is hotter than its liquid boils, or colder than it freezes:
  check the biome's climate and the lake's `liquidRange`.
- **The layers of rock never show.** The rock of the land is not in the tag they replace.
- **A vein lays nothing.** No installed mod uses its materials' ores in the rocks where it lies, its biome is not one
  of its biomes, or `without_ore` names its materials.
- **The game's iron is still scattered.** The world's chunks were made before veins were on, or `veins.enabled` is
  off.
- **A reservoir never shows.** Its rock is not the rock it replaces where it lies, its biome is not one of its biomes,
  or its soaked rock's id names no block.
