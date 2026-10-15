# Cables

*Added 2026-10-01.*

How a mod adds its own cables to J's Core's one cable block. For what the series' own cables are and how players
lay them, see J's Computers' [Cables](../../computers/docs/CABLES.md); this page is for the mod author.

## One block for every cable

There is one cable block, `jscore:cable`, and every mod's cables live in it as **wires**. A block holds up to nine
wires, each in a **lane** of a 3 by 3 grid seen end on, so a data cable, a peripheral cable and an energy cable can
run through the same block side by side without ever joining. A cable that must be alone holds the block by
itself. Thin **parts** (buses, covers) can sit on the block's faces.

What a mod declares is a **cable type**: which line it belongs to, which grid carries it, which lane, how much and
how far it carries, and how it looks.

## A line

A **line** is a job a cable does, named by an id. Its **generation** counts eras: a port takes cables of its own
generation and every earlier one, never a newer one.

```java
public static final Connection STEAM_LINE = Connection.of(rl("steam"));        // generation 0
```

## A cable type

```java
public static final CableEntry STEAM_PIPE = CONTENT.cable("steam_pipe", CableType.builder(STEAM_LINE)
                .grid(GridKind.FLUID)                 // which grid moves what it carries
                .lane(Lane.BOTTOM_RIGHT)              // its lane, the same for every generation of the line
                .thickness(4)                         // pixels, 1 to 8
                .carries(500, 64)                     // 500 a tick, 64 blocks a run (0: any length)
                .loses(0)                             // thousandths lost on the way
                .withstands(273, 900)                 // pipes: the coldest and hottest fluid, in kelvin
                .describe(MyTexts.STEAM_PIPE_JOB, null)
                .jacket(rl("block/cable/steam_pipe")) // its texture, 32 by 32
                .plug(rl("block/cable/plug/steam")))  // the model where it meets a block
        .named("Steam Pipe")
        .tab(MAIN)
        .register();
```

| Step | What it says |
| --- | --- |
| `grid(...)` | Which grid it belongs to: one of the Core's (`POWER`, `FLUID`, `HEAT`, `GAS`, `MOTION`, `DATA`, `PERIPHERAL`) or a mod's own (below). Only `DATA` carries a network's identity. |
| `lane(...)` or `alone()` | Its lane in the block, or the block to itself. One line owns one lane, and two lines in one lane are refused. |
| `carries(throughput, range)` | How much it moves a tick, and how many blocks one run reaches before it carries nothing further. |
| `loses(thousandths)` | What it loses on the way, 0 to 1000. |
| `withstands(cold, hot)`, `takes(tag)` | For pipes: the temperatures and marks of the fluids it carries. |
| `runsStraight()` | It cannot turn inside a block; something else turns it. |
| `joinsAtMost(faces)` | The most neighbours it joins, 1 to 6 (2 for a line between two ends). |
| `describe(job, era)` | The tooltip line saying what it is for. |
| `jacket(texture)`, `plug(model)` | Its look; both are required. `plug(portKind, model)` gives another plug where it meets a port of that kind. |

`register()` registers the cable type and the item that lays it. The lanes the series uses: access `TOP_LEFT`,
backbone `TOP`, peripheral `LEFT`, high compute `MIDDLE`, crafting `RIGHT`, energy `BOTTOM_LEFT`; the long distance
line holds its block alone. A new line takes a free lane.

## A grid of a mod's own

*Added 2026-10-15.*

Grids are a registry: each has an id and a **policy**, how what it carries is shared along a run and what its wires
plug into. A mod whose line must never join another's, as an energy cable of one voltage class beside another's,
declares a grid of its own with one of the Core's policies, or with its own:

```java
public static final GridKind MEDIUM_VOLTAGE = GridKind.declare(rl("medium_voltage"), GridPolicies.ENERGY);
```

| Policy | What it shares | What its wires plug into |
| --- | --- | --- |
| `GridPolicies.ENERGY` | energy, each machine its share of what it asks | any block that offers the game's energy on that face |
| `GridPolicies.FLUID` | fluids, from outputs to inputs and tanks | any block that holds fluids on that face |
| `GridPolicies.matter(state)` | a gas, a supercritical fluid, a slurry, plasma or exotic matter, as fluids are shared | any block that keeps that state on that face, the Core's or another mod's through its bridge |
| `GridPolicies.NETWORK` | the identity of a data network | (the data network's own devices) |
| `GridPolicies.JOINS_ONLY` | nothing: the grid only joins what it joins, for its mod to read | (its mod's own devices) |

The Core's own grids of matter are `GridKind.GAS`, `SUPERCRITICAL`, `SLURRY`, `PLASMA` and `EXOTIC`, one for each
state, so lines of different states never join ([Energy and resources](ENERGY_AND_RESOURCES.md#states-of-matter)).

An `IGridPolicy` of a mod's own gives an `IGridMover`, which shares what its grid carries in one dimension each tick
after the machines have ticked, and says what its wires plug into. Each dimension makes one mover for each grid,
the first time it is asked (`CoreGrids.mover(level, kind)`).

## Rules of joining

- Two wires join when they are of the same line and generation and their colours agree: a dye colours a wire, two
  colours never join, and an uncoloured wire joins every colour.
- Where a wire changes lane or two would cross, the block becomes a junction box the wires enter and leave each in
  its place.
- A block says what each face takes with `FacePorts` ([Networks](NETWORKS.md#a-block-of-yours-on-the-network)); the
  cable and the block ask the same question.

## From code

`Cables.lay(level, pos, type)` lays a wire (into an existing cable block or a new one), `Cables.holds(level, pos,
type)` asks whether a block holds one, and `Cables.reaching(level, pos, face)` lists the wires crossing into a face.
In a GameTest, `ScenarioBuilder.layCable` does the same ([Testing](TESTING.md)).

## Parts on the cable

A part is a thin piece on one face of the cable block: a bus, a cover, a sensor. A mod registers its part kinds in
the Core's part registry with its own `DeferredRegister`:

```java
public static final DeferredRegister<PartType<?>> PARTS = DeferredRegister.create(CoreParts.KEY, MyMod.MODID);
public static final DeferredHolder<PartType<?>, PartType<SensorPart>> SENSOR = PARTS.register("sensor",
        () -> new PartType<>(SensorPart::new, MyTexts.SENSOR, rl("block/part/sensor")));
```

A part (`IFacePart`) says its type, is attached to its host and face, saves and loads itself, may tick, may answer
a player's use, and gives back its item when broken. A part on a face closes that face to wires. Its model is a
plain block model facing north; the block's model is rebuilt only when a part starts or stops working.

## What can go wrong

- **"two lines in one lane".** Your line took a lane another line owns. Pick a free one.
- **The cable is refused as declared.** It has neither a lane nor `alone()`, or no jacket or plug, or a thickness
  over 8.
- **Two of your cables side by side join when they should not.** Same line, same generation, colours agree: dye
  them apart, or give them different lines.
- **A run carries nothing at its far end.** It is longer than its range.
