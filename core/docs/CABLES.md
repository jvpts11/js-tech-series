# Cables

*Added 2026-10-01.*

How a mod adds its own cables to J's Core's one cable block. For what the series' own cables are and how players
lay them, see J's Computers' [Cables](../../computers/docs/CABLES.md); this page is for the mod author.
[J's Example](../../example/README.md) declares an energy cable, a fluid pipe and a sensor mounted on a cable
(`ExampleContent` and `part/LevelSensorPart`), on the Core's API alone.

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
                .thickness(4)                         // pixels: up to 6 in a lane, 12 alone
                .carries(500, 64)                     // 500 a tick, 64 blocks a run (0: any length)
                .loses(0)                             // millionths lost each block
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
| `grid(...)` | Which grid it belongs to: one of the Core's (`POWER`, `FLUID`, `HEAT`, `GAS`, `MOTION`, `DATA`, `PERIPHERAL`, the other states of matter), a class of an energy's (`energy.grid(energyClass)`) or a mod's own (below). Only `DATA` carries a network's identity. |
| `lane(...)` or `alone()` | Its lane in the block, or the block to itself. One line owns one lane, and two lines in one lane are refused. |
| `carries(throughput, range)` | How much it moves a tick, and how many blocks one run reaches before it carries nothing further. |
| `loses(millionths)` | What each block of it loses besides its energy class's loss by distance, 0 to 1,000,000. |
| `withstands(cold, hot)`, `takes(tag)` | For pipes: the temperatures and marks of the fluids it carries ([Marks and wear](ENERGY_AND_RESOURCES.md#marks-and-wear)). |
| `wornBy(tag, ticks)` | For pipes: a mark that wears it through, and how many ticks of carrying it it stands. |
| `holds(atmospheres)` | For pipes of a state held under pressure: the most pressure it holds. A store holding a gas at more is carried none of, and the gas reaches the pipe as a breach of `OVERPRESSURE` ([Pressure that bursts](ENERGY_AND_RESOURCES.md#pressure-that-bursts)). Any pressure unless said. |
| `runsStraight()` | It cannot turn inside a block; something else turns it. |
| `joinsAtMost(faces)` | The most neighbours it joins, 1 to 6 (2 for a line between two ends). |
| `describe(job, era)` | The tooltip line saying what it is for. |
| `jacket(texture)`, `plug(model)` | Its look; both are required. `plug(portKind, model)` gives another plug where it meets a port of that kind. |
| `windows(glass)` | Its jacket leaves windows clear and `glass` fills them: a pipe shows its liquid or gas moving through them at its level, a line of items its items sliding through ([Seeing what flows](#seeing-what-flows)). |
| `fitting(texture)` | It turns and branches in a solid fitting of that texture, as a pipe does, rather than in its jacket. |
| `beyond(rule)` | What befalls it when a fluid it is not made for reaches it ([Beyond a line's limits](#beyond-a-lines-limits)). |
| `sounds(soundType)` | How it sounds laid, taken out, struck and walked on, as what it is made of: wool unless said, as a cable in its jacket; the series' pipes say metal (*added 2026-10-24*). |

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
| `GridPolicies.ITEMS` | items, from outputs to inputs and inventories, as fluids are shared | any block with an inventory on that face |
| `GridPolicies.GROUP` | nothing: each connected run groups the machines it joins ([Lines that group](#lines-that-group)) | any block that offers `LineGroups.MEMBER` on that face |
| `GridPolicies.NETWORK` | the identity of a data network | (the data network's own devices) |
| `GridPolicies.JOINS_ONLY` | nothing: the grid only joins what it joins, for its mod to read | (its mod's own devices) |

The Core's own grids of matter are `GridKind.GAS`, `SUPERCRITICAL`, `SLURRY`, `PLASMA` and `EXOTIC`, one for each
state, so lines of different states never join ([Energy and resources](ENERGY_AND_RESOURCES.md#states-of-matter)).

An `IGridPolicy` of a mod's own gives an `IGridMover`, which shares what its grid carries in one dimension each tick
after the machines have ticked, and says what its wires plug into. Each dimension makes one mover for each grid,
the first time it is asked (`CoreGrids.mover(level, kind)`).

The Core's grid of items is `GridKind.ITEM`; a mod declares the lines that carry items on it.

## The ends of a line

*Added 2026-10-15.*

Where a wire plugs into a block is an **end**, and for lines that carry items, liquids and gases each end is set on its
own (`CableEnd`); every setting's default keeps a mod's own design, so a pipe from a machine's output to another's input
just works.

| Setting | What it does |
| --- | --- |
| mode | `NORMAL`: as the block says, its output gives, its input takes, a tank both. `PUSH`: the line only fills it. `PULL`: the line only draws from it. `OFF`: the end plugs into nothing, and joins no cable beside it |
| priority | from -99 to 99: the highest fills first, and ends of one priority take turns, each tick the next served first, so over time each gets the same share |
| rate | the most it gives or takes a tick; 0 for no limit but the line's |
| channel | from 0 to 15: an end gives and takes only on its own, so one line does the work of several |
| filter | an `ItemFilter`, by item, tag, mod or components, or a fluid's or a material's id; a rule's amount, in an `ONLY` filter, is how much to keep in the block, filling it no further |
| redstone | an `EndRedstone`: `IGNORED`, the end works whatever reaches it; `WITH_SIGNAL`, it works only while a redstone signal reaches its cable block, and is off without one; `WITHOUT_SIGNAL`, it works only while none does. The line reads the signal once a tick (`CableEnd.modeAt`) |

A line shares among its ends as `EndRouter` says: outputs feed inputs, then fill the tanks, and the tanks feed what the
inputs still want; two tanks never pour into each other unless an end pulls from one, and the same for two chests. An
end offers the first thing some other end would take, so what nobody wants never holds up what comes after it.

```java
cable.setEnd(MyCables.PIPE.get(), Direction.EAST, CableEnd.DEFAULT.withMode(EndMode.PULL).withPriority(5));
```

The series' Basic Wrench goes round an end's modes; `CableEnds.facing` tells which end a player points at, for a
wrench of a mod's own, and `EndTexts.set` says what was set.

### The end's screen

Sneaking with the Basic Wrench on an end of a line whose grid routes its ends (`IGridPolicy.routesEnds`: the Core's
fluid, matter and item grids) opens the end's screen, in the game's own look. It sets the mode with four keys, the
priority and the rate with a field each, typed or stepped (Shift steps ten), the channel with sixteen keys in the dyes'
colours, and the filter: its key turns it between only and all but, and its nine slots hold its rules. The key at the
filter row's right goes round what redstone does to the end: redstone dust for nothing, a lit torch for working with a
signal, a torch gone dark for working without one, its tooltip saying which.

| On a filter slot | What it does |
| --- | --- |
| click with a stack | names what a wire of the line carries of it: the item itself, the liquid in a bucket or a tank, the gas in a cylinder; a stack that holds nothing the line carries is refused. The first rule of an empty filter turns it to only |
| click with nothing | clears the slot |
| right-click | goes round the thing, each of its tags and its mod (`EndFilters.next`) |
| scroll | keeps more or less of it in the block: an item or a hundred millibuckets a step, sixteen or a bucket with Shift |

A wrench of a mod's own opens it with `EndSettings.open(player, cable, wire, face)`, after
`EndSettings.hasScreen(wire)`.

## Seeing what flows

*Added 2026-10-15.*

Each wire of a pipe or a line knows what it carries (`CableBlockEntity.flow(lane)`, a `WireFlow`): what it is, by its
id; toward which face of its block; how much a tick; and how full, in four steps. Its mover traces where what it moved
went, over the last half second, so the wires along the way show it, and a wire nothing crossed shows nothing.

A cable type with `windows(...)` shows it: a pipe's liquid or gas through its sight glasses, at its level, in the
liquid's own colours or a material's; a line of items its items sliding through, as many as it is full. A player who
holds an item of the tag `jscore:sees_flow` (the Basic Wrench), or wears one on the head (goggles), reads beside the
crosshair what the wire they look at carries, which way and how much a tick, and how the end they point at is set. A mod
puts its own wrench or goggles in the tag.

## Laying a run at once

*Added 2026-10-15.*

A player lays a whole run by dragging: the click with a cable lays its first block as ever, and while the click is
held, looking further along outlines the run to the block looked at (up to 32 blocks away), white where it can be laid
and red where something is in the way. Let go, the server lays it all (`CableRuns.lay`), along the way `RunPath`
takes: the axis the run covers most first, then the next, then the last; straight along the first for a cable that
`runsStraight()`; never more than `RunPath.MOST` (64) blocks. Each block joins the one before as any cable does. The
run stops at the first block it cannot be laid in (something in the way, a place protected against the player, or no
cable left in hand) and says where. A join the player does not want is turned off afterwards, face by face, as any
end is, with a wrench set to off.

Where a click lays its wire is one decision, `CableItem.landing`, which the player's game uses to outline the run and
the server uses to lay it: in the cable block clicked when it takes the wire, in a block that gives way (a snow layer,
grass) in its place, against the face clicked otherwise. A wire laid or taken out by hand is shown at once, with the
cables beside it (`CableItem.showNow`), so a new cable appears already joined to its neighbours rather than a tick
later (*added 2026-10-24*). Any mod can do the same for what a player's hand changes with `ClientUpdates.sendNow`,
which sends a block and its block entity to the players watching it without waiting for the end of the tick.

## Overhead lines

*Added 2026-10-15.*

From medium voltage up, energy also travels in bare wire hung between insulators on poles and towers, as in Immersive
Engineering: the only link outside the cable block. **Any line can be hung**, each by its mod's decision: a cable type
that `hangs(longestSpan)` is hung by using its item on one insulator and then the next (the same one again lets the
wire go). The wire is checked when it is hung: within the line's longest span, along a way nothing solid stands in, its
sag included, and a block of wire taken from the stack for each block of its length.

A block offers its insulators with `OverheadLines.ANCHORS` (`IWireAnchors`): each a `WireAnchor`, where on the block the
wire is fastened, the face it meets, how many wires it holds, and its role:

| Role | What it does with what the wire carries |
| --- | --- |
| `CONNECTOR` | hands it to its block through the face it meets: a machine's insulator, a substation's, the insulator where a line ends at a consumer that takes high voltage directly |
| `RELAY` | only passes it from one wire to the next: a pole's or a tower's |
| `FEEDTHROUGH` | takes the wire through a wall one block thick: the feedthroughs of one block are joined through it |

The wires of a dimension are kept with it (`OverheadLines.WIRES`) and sent to its players, who see each sag along its
curve (`Catenary`). A wire goes in its line's grid while its two insulators are loaded, as a cable as long as itself,
so what the line loses counts its length in blocks; its insulators stand in the grid as cables of no length. A wire
whose insulator is gone falls, and its wire is left there. The Core gives the hanging, the spans, the sag and the touch
of a live wire ([Shocks and surges](ENERGY_AND_RESOURCES.md#shocks-and-surges)); a mod gives its poles, towers and
insulators.

## Reading a line

*Added 2026-10-15.*

What a part of a line did in the last tick is read from its grid's mover, `CoreGrids.mover(level, kind)
.flatMap(mover -> mover.survey(wire))`, the wire numbered as `CoreGrids.places(level).find(cable.place(type))` says. A
`LineSurvey` holds each block the part plugs into, with what it is to the line (a source, a consumer, a store, or
idle) and what it gave and took; what the part moved; what its consumers asked (a store that wants charging asks for
no work, and a routed line cannot tell, `LineSurvey.UNKNOWN`); what it lost on the way; how many cables it has; and its
narrowest cable, which the whole part moves no more than. The Core's energy, fluid, matter and item lines keep one; a
mod's own mover keeps one with a `PartTally` for each part, counting what each move took from one block to another,
and a `NarrowestWire` found as it goes through the part's wires.

The series' **Network Probe** reads it: linked to a wire, it shows the line on four pages (the summary, the members,
the flow of the last minute and the losses), and outlines the blocks of a row of its members in the world.

## The basic pipes

*Added 2026-10-15.*

The series shares three pipes, so a mod used alone carries liquids and gases: the **Copper Pipe**, for liquids up to
200 °C, 100 mB a tick; the **Iron Gas Pipe**, for gases at low pressure, 100 mB a tick; and the **Vacuum-Jacketed
Pipe**, double-walled, for cryogenic liquids, 400 mB a tick. Each holds its block alone, ten pixels thick, with sight
glasses and solid fittings. A liquid hotter or colder than a pipe stands bursts it after five seconds, spilling the
liquid. They are made of copper or iron and glass panes, obsidian for the vacuum jacket; with J's Industrial installed
they are its pipes, made with its recipes.

## Lines that group

*Added 2026-10-15.*

A line on a grid of `GridPolicies.GROUP` carries nothing: each connected run is a **group** of the machines it joins,
and its mod says what the group does (research computers pooling their work, the devices of a ground station). A
machine joins by offering `LineGroups.MEMBER` on the face the line meets, and hears `joined(group)` and `left(kind)`;
a mod hears every group that is new, changed or dissolved with `LineGroups.listen(kind, listener)`, and reads them
with `LineGroups.of(level, kind)`. A dyed run is a group of its own beside a run of another colour. The stations of
a pneumatic post find one another so ([Pneumatic post](CONVEYORS.md#pneumatic-post)).

## Beyond a line's limits

*Added 2026-10-15.*

A pipe is made for some temperatures and marks; when a fluid it is not made for reaches it, its cable's rule says what
follows, each tick it is so (`CableType.Builder.beyond`). By default it holds and carries none of it.
`BreachRules.breaksAfter(ticks, then)` breaks the wire once it has been reached that long in a row and lets its mod say
what follows; `BreachRules.spill` lets a liquid out where the pipe was. `LineBreach` says where, which cable, why (too
hot, too cold, a mark it is not made for, more pressure than it holds, worn through), what reached it and at what
pressure. A pipe is worn through once it has carried a mark it is declared worn by (`wornBy`) for as long as it
stands; `LineWear` keeps how long each has, with the world.

A pipe of gas that says the pressure it holds (`holds(atmospheres)`) is reached by a gas from a store holding it at
more; `PressureBursts.pipe(innerVolume)` is what follows a pipe that bursts: a blast of the energy the gas a block of
it held ([Pressure that bursts](ENERGY_AND_RESOURCES.md#pressure-that-bursts)).

```java
.grid(GridKind.GAS).carries(500, 0).holds(10)
.beyond(BreachRules.breaksAfter(1, PressureBursts.pipe(8)))   // eight millibuckets inside each block
```

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

The cable block is a block space like any other: microblocks share it with the wires and the parts, a whole cover
closes its face to wires as a part does, a hollow cover lets them through, and a facade hides them while they keep
working ([Block spaces and microblocks](MULTIPART.md)). What a part can do besides, its redstone, light and
comparator reading, a tick only when it needs one, a room of its own shape, is told there too
([What a device can do](MULTIPART.md#what-a-device-can-do)).

## What can go wrong

- **"two lines in one lane".** Your line took a lane another line owns. Pick a free one.
- **The cable is refused as declared.** It has neither a lane nor `alone()`, or no jacket or plug, or a thickness
  over 6 in a lane or over 12 alone.
- **Two tanks on one pipe never share.** Both take and give, so neither is an output: set the end of the one to empty
  to pull.
- **Two of your cables side by side join when they should not.** Same line, same generation, colours agree: dye
  them apart, or give them different lines.
- **A run carries nothing at its far end.** It is longer than its range.
