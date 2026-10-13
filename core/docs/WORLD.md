# The world

What J's Core gives a mod about the world itself: ores and structures that generate in it, dimensions of your
own with rules for gravity, air, heat and weather, dimensions made while the game runs, data kept over areas of
the world, and chunks kept loaded for their owners.

## The words

- A **chunk** is a column of the world 16 by 16 blocks wide. The game loads chunks near players and unloads the
  rest; a machine in an unloaded chunk stops.
- A **dimension** is a world of its own, like the Nether: its own terrain, sky and rules.
- **World generation** is the game making new terrain as players explore; it reads what to place from data files.

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

public static final WorldGen.TemplateStructure CAMP = WorldGen.structure(rl("camp"))
        .template(rl("camp"))  // data/<namespace>/structure/camp.nbt, saved with a structure block
        .biomes(BiomeTags.IS_FOREST)
        .spacing(40, 12)       // about one every 40 chunks, never closer than 12
        .salt(7_340_211)       // any number, different for each structure, so they do not line up
        .declare();
```

Declaring happens when the class holding these loads, so load it before the data generation runs (an empty
`declare()` method called from your mod's constructor). The files go under `data/<namespace>/worldgen/` (the
configured and placed features, the structure and its set, the template pool) and a NeoForge biome modifier puts
ores into the biomes. A data pack can replace any of them.

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

A flat dimension is made of the layers, bottom first; `noise(...)` gives it terrain from a noise setting instead.
`noSkylight`, `ceiling`, `ultraWarm` (water evaporates, as in the Nether), `effects` (how the sky looks) and
`unnatural` (compasses spin, beds explode) say the rest. The data generation writes its dimension type and its
dimension.

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
- **A structure never generates.** Its template file is missing from `data/<namespace>/structure/`, or its spacing
  is very large.
- **A machine stops when you walk away.** It did not ask for its chunk, or its owner has reached the limit.
- **Players in a dimension take damage.** Its rules say the air cannot be breathed, or it is too hot or cold. A
  mod's suit must cancel `DimensionHazardEvent` to protect them.
