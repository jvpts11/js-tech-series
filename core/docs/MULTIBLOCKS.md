# Multiblocks

A **multiblock** is a machine built from many blocks placed in a shape: a furnace of nine blocks, a reactor of a
hundred. One block of the shape is the **terminal**, the one the player works it from, which holds the machine's state
and runs it; the others are its **parts**. J's Core says what the shape is, checks whether the blocks in the world make
it, and lets pipes reach the machine through the parts you mark as **ports**, or through the **entries** a player puts
where the multiblock lets them stand.

A block bigger than one block that is placed whole and builds nothing, as J's Computers' Mainframe, is no multiblock:
it is a [big block](#big-blocks).

## A pattern

*Added 2026-04-30. What was called its controller is its terminal since 2026-10-24.*

A pattern is the shape, drawn in letters, one layer at a time from the bottom:

```java
public static final MultiblockPattern FURNACE = MultiblockPatterns.declare(rl("furnace"),
        MultiblockPattern.builder("furnace")
                .layer("PIP",
                       "E#P",
                       "POP")
                .where('P', BlockMatch.blocks("mymod:furnace_casing"))
                .where('I', BlockMatch.blocks("mymod:furnace_casing"))
                .where('O', BlockMatch.blocks("mymod:furnace_casing"))
                .where('E', BlockMatch.blocks("mymod:furnace_casing"))
                .port('I', PortKind.ITEM_INPUT)
                .port('O', PortKind.ITEM_OUTPUT)
                .port('E', PortKind.ENERGY_INPUT)
                .build());
```

- Each `layer` is one height, each string one row of it. Call `layer` again for the layer above.
- `#` is the terminal. A space is any block at all.
- `where` says what a letter stands for: `BlockMatch.blocks(...)` one of those blocks, `BlockMatch.tag("...")` any
  block of a tag.
- `port` marks a letter as a port of a kind: `ITEM_INPUT`, `ITEM_OUTPUT`, `FLUID_INPUT`, `FLUID_OUTPUT`,
  `ENERGY_INPUT`, `ENERGY_OUTPUT`.
- `terminal("mymod:furnace_terminal")` names the block that stands at `#`. Matching never needs it, since the
  terminal is the one matching, but the views that show a multiblock before it is built draw it there and list it
  (its file holds it as `"terminal"`). A pattern read from a structure file names it by itself.
- The shape is found facing any of the four horizontal ways, so a player can build it turned.

### Patterns are data

`declare` keeps the pattern under its id, and the data generation writes it to
`data/<namespace>/multiblock/<path>.json`. When the game loads, the file is what counts: a data pack can ship its
own copy and change the shape, the blocks, or the ports, with no code. `MultiblockPatterns.get(id)` gives the
pattern in force.

A pattern can also be read from a structure block's file, built in the game:
`MultiblockPatterns.fromStructure(name, structureTag, "mymod:furnace_terminal", Map.of("mymod:furnace_hatch",
PortKind.ITEM_INPUT))`: the named block is the terminal, the blocks of the map are ports, air is free, and every
other block must be there as it was saved.

*Added 2026-10-22.* Or recorded off one built in the world, in the studio ([Developing](DEVELOPING.md)): on the page
of structures, catch its two corners, open the studio looking at its terminal and press **Make it a multiblock**.
The page lists the letters the recorder gave each kind of block, the first of its name where that is free (`B` for
bricks), a port block `I` or `O` by its kind, the terminal at `#` and air left free, with how many of each and its
layers as the file writes them; it may be mirrored or lie down; and beside it is the page the recipe viewers will show
of it. **Write the pattern** writes `data/<mod>/multiblock/<name>.json` in the mod's source in this run, or, with no
source of the mod here, in a data pack of the world's own, in force once its data is reloaded; **Show with the
projector** shows the pattern where it was built. In code, `MultiblockRecorder.record(level, corner, otherCorner,
terminal, name, mirrors, liesDown)` reads the same pattern.

## Places that count and give

*Added 2026-10-16.*

A letter can stand for more than "these blocks fit". `where` also takes a `Place`, which says what fits and,
besides, what a block there **counts** and what number it **gives** the formed machine:

```java
.where('H', Place.of(BlockMatch.blocks("mymod:casing", "mymod:energy_hatch"))
        .counting("energy_hatch", BlockMatch.blocks("mymod:energy_hatch")))
.where('K', Place.of(BlockMatch.tag("mymod:coils"))
        .valued("temperature", Map.of("mymod:copper_coil", 1800.0, "mymod:nichrome_coil", 2700.0,
                "#mymod:exotic_coils", 9000.0), Place.Gather.LEAST))
.limit("energy_hatch", 1, 4)
```

- **A count** gives how many of the place's blocks are of a kind, under a name. Here every `H` is casing or an
  energy hatch, and the structure counts the hatches.
- **A limit** keeps a count between a least and a most, both counted in: from one to four energy hatches. A
  structure outside a limit does not form, and its failure names the count. A limit must name a count some place
  keeps, so a misspelt name is caught when the pattern is built.
- **A number** gives each block of the place a value, by block id or by tag (written `#namespace:path`), and
  gathers the values of every such place into one: `LEAST` (a blast furnace is as hot as its coolest coil),
  `MOST`, `SUM` or `MEAN`. One key is gathered one way only.

## Entries

*Added 2026-10-24.*

An **entry** is one of the multiblock's own blocks through which something comes in or goes out: a power entry, a
coolant inlet, the socket of a data link. The multiblock says **where** entries may go, by declaring them on those
places, and the player puts each where it suits, as a real plant's connections go where its lines arrive:

```java
MultiblockPattern.builder("computer")
        .layer("RRR",
               "C#C")
        .where('R', BlockMatch.blocks("mymod:casing"))      // the rear row: casing, or an entry
        .where('C', BlockMatch.blocks("mymod:casing"))      // the front: casing only
        .entry('R', "mymod:power_entry", BlockMatch.blocks("mymod:power_entry"), PortKind.ENERGY_INPUT)
        .entry('R', "mymod:coolant_inlet", BlockMatch.blocks("mymod:coolant_inlet"), PortKind.FLUID_INPUT)
        .entry('R', "mymod:coolant_outlet", BlockMatch.blocks("mymod:coolant_outlet"), PortKind.FLUID_OUTPUT)
        .limit("mymod:power_entry", 1, 1)
        .limit("mymod:coolant_inlet", 0, 1)
        .together("mymod:coolant_inlet", "mymod:coolant_outlet")
        .build();
```

- `entry(letter, name, block, port)` lets the entry stand at any of the letter's places in place of its own block.
  Declare the place with `where` first; what fits there widens to take the entry too. A place that declares no entry
  refuses one: an entry put there fails as a wrong block, as any other would.
- An entry standing at a place is **counted** under its name, so `limit` says how many of each the multiblock takes:
  exactly one power entry, at most one inlet. A count named by a block's id, as here, is told to the player by that
  block's name ("Needs from 1 to 1 of Power Entry").
- It makes its place a **port** of its kind, as `port` does for a letter, so pipes and cables reach the terminal
  through it. An entry onto a line of a mod's own takes `null` for its port and reaches the machine by
  `MultiblockPorts.registerOther`.
- `together(first, second)` forms the structure only when the two counts are the same: a coolant's inlet never
  without its outlet. A structure whose pair differs fails as `APART`, naming both ("Takes Coolant Inlet and Coolant
  Outlet together, as many of one as of the other").
- A limit or a pair naming a count that no place, entry or inside part keeps is refused when the pattern is built.

In a pattern's file, a place lists its entries and the pattern its pairs:

```json
"key": {"R": {"blocks": ["mymod:casing", "mymod:coolant_inlet", "mymod:coolant_outlet", "mymod:power_entry"],
              "entries": [{"name": "mymod:power_entry", "which": {"blocks": ["mymod:power_entry"]},
                           "port": "energy_input"},
                          {"name": "mymod:coolant_inlet", "which": {"blocks": ["mymod:coolant_inlet"]},
                           "port": "fluid_input"},
                          {"name": "mymod:coolant_outlet", "which": {"blocks": ["mymod:coolant_outlet"]},
                           "port": "fluid_output"}]}},
"together": [{"first": "mymod:coolant_inlet", "second": "mymod:coolant_outlet"}]
```

## Turning, mirroring and lying down

*Added 2026-10-16.*

Every pattern is found in its four horizontal turns. Two more calls widen that:

- `mirrors()`: it is also found mirrored, for a shape that is not the same both ways round (eight ways in all).
- `liesDown()`: it is also found lying on its side, the terminal facing up or down (twenty-four ways; forty-eight
  with `mirrors()` too).

The formed structure's `orientation()` says which way it stands. The four turns are tried first, so a symmetric
shape reads as the turn you would expect.

## Shapes that grow

*Added 2026-10-16.*

A pattern can repeat one **slice** a number of times, for a structure the player makes as long or as tall as they
want within limits: the middle layer of a tower, one oven of a battery.

```java
MultiblockPattern.builder("distillation_tower")
        .layer("CCC", "C#C", "CCC")     // the base, with the terminal
        .layer("GGG", "G G", "GGG")     // one storey, repeated
        .layer("CCC", "CCC", "CCC")     // the top
        .where('C', BlockMatch.blocks("mymod:casing"))
        .where('G', BlockMatch.blocks("mymod:tower_glass"))
        .repeatLayer(1, 2, 12)          // from two to twelve storeys
        .build();
```

`repeatLayer` repeats a layer (counted from the bottom), `repeatRow` a row (from the north) and `repeatColumn` a
column (from the west). A pattern repeats one slice at most, never the terminal's own, and no more than 512
times. The formed structure's `repeats()` says how many times the slice stands, which is what the size gives: more
storeys, more ovens, more at once. The world is read once along the slice to find how many there are, so a
structure that grows costs one check, not one per size.

## The inside

*Added 2026-10-16.*

A reactor, a boiler or a battery is worked out from **what stands inside it**. A pattern marks its inside with a
letter and an `InsideRule`; the shapes by rule below take one too:

```java
InsideRule core = InsideRule.empty()
        .with(InsidePart.of("cell", BlockMatch.blocks("mymod:fuel_cell"))
                .gives("heat", 10)
                .bonus("heat", InsidePart.Neighbour.block(BlockMatch.blocks("mymod:moderator")), 5))
        .with(InsidePart.of("moderator", BlockMatch.blocks("mymod:moderator")))
        .with(InsidePart.of("cooler", BlockMatch.blocks("mymod:water_cooler"))
                .gives("cooling", 20)
                .needs(InsidePart.Neighbour.part("cell"), 1));

builder.inside('x', core);
```

- `InsideRule.empty()` allows air and nothing else; `InsideRule.of(matcher)` allows other blocks that give nothing.
  The parts are allowed too. A block inside that is neither keeps the structure from forming.
- A **part** has a name, which is also its count: how many of it **work**. Each working block adds what it `gives`
  to a number, summed over every one.
- `bonus` gives more for each of the six neighbours of a kind: here a cell gives 5 heat more for each moderator
  beside it.
- `needs` makes a part work only with enough neighbours of a kind: a cooler works only beside a working cell. A
  neighbour is a block of a kind (`Neighbour.block`), wherever it stands, the casing included, or a working block of
  another part (`Neighbour.part`).
- Needs settle from the parts that need nothing outwards, so a chain works (a relay beside a cooler beside a cell),
  and two parts that only need each other never work.

The inside's counts and numbers come out with the rest: `formed.count("cell")`, `formed.value("heat", 0)`, and
`formed.inside()` lists every place inside, the empty ones too, which is the volume of a tank.

## Shapes by rule

*Added 2026-10-16.*

Some structures have no fixed drawing: the player builds them to any size within limits. Three shapes come by rule;
each is checked from its terminal, which stands anywhere on the shape.

| Shape | What it is | Like |
|---|---|---|
| `BoxShape.of(frame, face, inside, sides, height)` | any box, a frame on its twelve edges, faces between them, an inside | a dynamic tank |
| `WallsShape.of(floor, wall, inside, area, height)` | a floor of any outline, walls round it as high as they run | a smeltery |
| `CuboidShape.of(unit, sides)` | any cuboid filled with units | a crafting processor |

```java
BoxShape TANK = BoxShape.of(
                Place.of(BlockMatch.blocks("mymod:tank_casing")),
                Place.of(BlockMatch.blocks("mymod:tank_casing", "mymod:tank_glass", "mymod:tank_valve"))
                        .counting("valve", BlockMatch.blocks("mymod:tank_valve")),
                InsideRule.empty(), new Limit(3, 18), new Limit(3, 18))
        .limit("valve", 1, 8);
```

- **A box** is found by walking along its shell from the terminal to its edges, then along an edge for its depth:
  it is whatever size the player built. A box larger than its most fails as too large rather than forming smaller.
  `sides` limits both horizontal sides, `height` its height; each side is counted with its shell, so a 3 by 3 by 3
  box has one place inside.
- **A floor and walls** start from the place behind the terminal (which stands in a wall), go down to the floor,
  fill out the floor's outline at that level, and rise layer by layer while the walls run unbroken. The corners
  where two walls meet are not needed. `area` limits how many places the floor has, `height` how high the walls are,
  the floor not counted. A gap in a wall lets the inside run out, which reads as a floor too large.
- **A cuboid** runs from the terminal along each axis as far as its units go, and every place of it must hold a
  unit. Count the kinds of unit through the unit's place ("at least one storage unit").

No side of a shape by rule is longer than 64 blocks, and no floor larger than 4,096 places.

## Checking the shape

*Added 2026-04-30. The full result, for every shape, added 2026-10-16.*

Every shape, drawn or by rule, is an `IStructureShape`, checked against the world from its terminal. Read the
world through `MultiblockPatterns.provider(level)`, with places packed by `PatternMatcher.encodePosition`:

```java
ShapeMatch result = MyContent.TANK.check(MultiblockPatterns.provider(level),
        PatternMatcher.encodePosition(pos.getX(), pos.getY(), pos.getZ()));
switch (result) {
    case ShapeMatch.Formed formed -> {
        // formed.parts(): the shell but the terminal; formed.inside(): the places inside
        // formed.sizeX(), sizeY(), sizeZ(), repeats(); formed.count("valve"), formed.value("heat", 0)
        // formed.orientation(), formed.ports() (for a pattern, its entries' places among them)
    }
    case ShapeMatch.Failure failure -> {
        // failure.wrong(): BLOCK, COUNT, APART or SIZE
        // failure.at(), relX(), relY(), relZ(): the place, and its offset from the terminal
        // failure.wanted(): the letter, the part of the shape (frame, face, inside, floor, wall, unit),
        //                   the count's name (the first of a pair that goes together), or the size (x, y, z, area)
        // failure.found(): the block found there, the number found, or the second of the pair
    }
}
```

When no way of standing forms, the failure told is the one of the way that went furthest before failing: the way
the player most likely meant, so the place it names is one they can see is wrong.

The older form, for a pattern in its four turns, still works:

```java
IMatchResult result = MultiblockPatterns.match(MyContent.FURNACE, level, terminalPos);
if (result instanceof IMatchResult.Success formed) {
    // formed.rotation(): which way it was built
    // formed.slavePositions(): every part's place
    MultiblockPorts.stamp(level, formed);
} else if (result instanceof IMatchResult.Failure failure) {
    // failure.expectedChar() and failure.actualBlockId(): what was wanted where, and what is there
}
```

Check when the terminal is placed or used, and when a part next to it changes; a failure says which place is
wrong, which is what a player needs to be told.

### In a pattern's file

*Added 2026-10-16.*

Everything above is written to the pattern's file, so a data pack changes it as freely as the drawing:

```json
{"layers": [["CHC", "C#C", "CCC"], ["GGG", "GxG", "GGG"], ["CCC", "CCC", "CCC"]],
 "key": {"C": {"blocks": ["mymod:casing"],
               "value": {"key": "heat", "by": {"mymod:casing": 2.0, "#mymod:hot": 5.0}, "gather": "least"}},
         "H": {"blocks": ["mymod:casing", "mymod:hatch"],
               "count": {"name": "hatch", "which": {"blocks": ["mymod:hatch"]}}},
         "G": {"blocks": ["mymod:glass"]}},
 "limits": {"hatch": {"least": 1, "most": 4}},
 "mirrors": true, "lies_down": false,
 "repeat": {"axis": "y", "index": 1, "least": 1, "most": 6},
 "inside": {"letter": "x", "rule": {"allows": {"blocks": ["minecraft:air"]}, "parts": [
     {"name": "cell", "which": {"blocks": ["mymod:cell"]}, "gives": {"power": 5},
      "bonuses": [{"key": "power", "of": {"tags": ["mymod:moderators"]}, "each": 1}]},
     {"name": "cooler", "which": {"blocks": ["mymod:cooler"]}, "needs": [{"of": {"part": "cell"}, "least": 1}]}]}}}
```

A count with no `which` counts every block of its place, and a limit with no `most` has none. Only matchers made
with `BlockMatch` can be written: a pattern that fits a place, counts, takes an entry or reads its inside with code
of its own cannot go to a file, and the data generation says which.

## A machine that is a multiblock

*Added 2026-10-16.*

A machine of the Core's kit ([Machines](MACHINES.md)) becomes a multiblock's terminal with one call in its
constructor. The player builds the structure block by block, in any order; it forms when it stands whole:

```java
public TankBlockEntity(BlockPos pos, BlockState state) {
    super(MyContent.TANK_ENTITY.get(), pos, state, 0, MachineEnergy.fe(10_000, 1_000, 0));
    formsWith(MachineStructure.of(MyContent.TANK_SHAPE)
            .parallel(formed -> Math.max(1, formed.count("valve"))));
}
```

- Until the structure stands whole the machine is **Not formed** (its screen and its face say so, `UNBUILT` on the
  LCD) and does no work. Once it forms, `structure().formed()` is true, its parts are bound to it, and
  `size()`, `repeats()` and `structure().get()` (the whole result, its counts and numbers) say what was built.
- `parallel` says how many recipes it runs at once from what was built: by its size, by its parts, by what is inside.
  `structure().parallel()` gives the number, none while it is not formed.
- Its screen is the machine's screen, with the same tabs as every machine.
- **Jade** shows whether it is formed, its size when Shift is held, and how many it runs at once; or, while it is not
  formed, what it needs and where: "Needs Block of Iron at 1, 1, 1 from the terminal; found Dirt", "Needs from 1
  to 4 of valve; has 0".
- **Only its owner and those its owner lets break it** break the blocks of a formed structure, whatever blocks they
  are, as with every owned block ([Ownership](OWNERSHIP.md)).
- Breaking any block of it undoes the structure; the terminal keeps what it holds.

It is checked again only when a block changes in the box it watches (everything its shape can reach while it is not
formed, its structure and the places beside it once formed), a few ticks after the change, and once a minute besides,
never every tick. It is never checked while a chunk it reaches is not loaded, and a chunk that unloads never undoes
it: it stays as it was until it can be read again. `LevelStructures.of(level).terminalHolding(pos)` says which
formed structure a place belongs to.

### Formed into one model

*Added 2026-10-16.*

A structure can turn into one detailed model once it forms, a boiler's riveted drum, a turbine's casing with its
rotor turning, in place of the blocks it was built of:

```java
formsWith(MachineStructure.of(MyContent.TURBINE_SHAPE)
        .merges()                                // its plain blocks merge into the model once formed
        .formingSound(MySounds.TURBINE_FORMS)    // heard as it forms
        .formingAnimation("form"));              // played once on the model as it forms

// On the player's game, the terminal's renderer draws the model over the whole structure:
event.registerBlockEntityRenderer(MyContent.TURBINE_ENTITY.get(), context -> KitBlockEntityRenderer
        .<TurbineEntity>formedOnly(rl("turbine")).build());
```

- **Only plain blocks merge**: a block with no block entity and no liquid in it. Each turns into a formed part
  (`jscore:formed_part`) that draws nothing, remembers the block it was and stands in for it: it breaks as slowly,
  sounds and lights as it did, a pick block gives the block, and breaking it drops that block. The terminal, the
  ports and every block with a block entity stay as they are, so pipes and cables still meet them.
- **Breaking a block of it undoes the structure**, and every formed part turns back into the block it was, with a
  puff where each stood. A formed part whose terminal is gone, or no longer holds it, turns back by itself within
  five seconds.
- **The model** is the one of the Core's ([Models](MODELS.md)) the terminal's renderer names, made as the structure
  stands facing north, as its pattern is drawn, its origin on the middle of the terminal's floor; it is turned,
  mirrored or laid down as the structure was built. While it is not formed the terminal's own model shows
  (`formed(model)` keeps a model for that, `formedOnly(model)` draws nothing then). A damaged machine's cracks show
  across the whole model.
- A structure is checked through its formed parts as through the blocks they were.

## Ports

*Added 2026-10-05.*

A part block entity extends `MultiblockPartBlockEntity`; register its capabilities once, and the parts that are
ports pass pipes and cables through to the terminal:

```java
// In your mod's constructor:
modBus.addListener(RegisterCapabilitiesEvent.class,
        event -> MultiblockPorts.register(event, MyContent.FURNACE_PART_BE.get()));
```

`MultiblockPorts.stamp(level, formed)` marks each part with the port the pattern gives it, or as a plain part. A
pipe against an item input port then reaches the terminal's items; against a plain part, nothing. The
terminal says what each port opens onto by implementing `IMultiblockPortHost`.

Ports come for items, fluid and energy, and for each other state of matter (`GAS_INPUT`, `SLURRY_OUTPUT`,
`PLASMA_INPUT` and the rest); the terminal answers the matter ports in `matterPort(kind, part, side)` with the
store of the port's state.

### Ports as blocks of their own

*Added 2026-10-16.*

A port can also be a block the player places **anywhere on the shell**, as the valves and hatches of a real plant,
so the connection goes where the line arrives. A mod registers one block for each kind, with its own look, on the
Core's `MultiblockPortBlock` and `MultiblockPortBlockEntity`, and puts the blocks in the tag
`MultiblockPorts.PORTS` (`jscore:multiblock_ports`):

```java
public static final BlockEntry<MultiblockPortBlock> ITEM_HATCH = CONTENT.block("item_hatch",
        properties -> new MultiblockPortBlock(properties, () -> MyContent.HATCH_ENTITY.get())).register();

public static final class HatchBlockEntity extends MultiblockPortBlockEntity {
    public HatchBlockEntity(BlockPos pos, BlockState state) {
        super(MyContent.HATCH_ENTITY.get(), pos, state, PortKind.ITEM_INPUT);
    }
}

// In the capabilities event, once for the type:
MultiblockPorts.register(event, MyContent.HATCH_ENTITY.get());
```

- A shape lets ports stand on its shell by taking the tag where they may stand:
  `Place.of(new BlockMatch(List.of("mymod:casing"), List.of(MultiblockPorts.PORTS_TAG)))`. Counting them is a
  `counting` of their blocks, as for any place, so a limit says "from one to four". For a drawn pattern whose
  connections are blocks of its own and go only on some of its places, declare them as [entries](#entries).
- There is a kind for each resource: items, liquid, energy, each other state of matter (gas, supercritical fluid,
  slurry, plasma, exotic matter), in and out, and `HEAT`, which flows both ways. An energy port meets a cable of
  J's Energy with its terminal's voltage class, and FE as FE.
- A line of a mod's own (J's Industrial's compressed air) reaches the terminal through
  `MultiblockPorts.registerOther(event, capability, partType)` and the host's `otherPort`.
- **The redstone port**, `RedstonePortBlock` with `RedstonePortBlockEntity`, reads the signal round it for its
  terminal, as a lever beside the machine would; used with an empty hand it switches to giving out its terminal's
  signal, what a comparator reads of it. It never reads what it gives.

A machine of the kit that `formsWith` a structure is the host of its ports already: an input port puts items, liquid
or matter into its inputs and an output port takes from its outputs, and nothing goes the wrong way through a port.

### Groups by colour

*Added 2026-10-16.*

A dye used on a port gives it that colour, and the same dye again takes it off. **Ports of one colour are one group**,
and a recipe takes its inputs from one group at a time, so two processes never mix. The groups are numbered from 0:
the ports with no colour first, when there are any, then each colour in the dyes' order. `structure().groups()`,
`structure().groupOf(pos)` and `structure().ports()` say which is which, and Jade shows a port's kind and group.

A `ProcessingMachineBlockEntity` of more than one lane gives each group a lane of its own: the item ports of group
`n` feed lane `n`'s inputs and take from its outputs, and a group past the last lane takes nothing. Its lanes are
never evened out between them, as a one-block machine's are. Liquid and matter tanks are shared by every lane.

### Recipes at once

*Added 2026-10-16.*

`MachineStructure.parallel(formed -> ...)` says how many recipes the structure runs at once, from what was built.
A processing machine works that many at once in each lane, more with its upgrades; `recipesAtOnce()` gives the exact
number, and Jade shows it ("Runs 4 at once").

## Showing a multiblock before it is built

*Added 2026-10-16.*

The **Structure Projector**, a block of the Core's shared content, shows any multiblock of any installed mod in the
world, in front of itself: its near side a block away, its middle across from the projector, its bottom level with
it. The places still empty are drawn as ghosts, the block that goes there pale and see-through; a block that is
wrong is outlined in red, seen through whatever stands between; the terminal is a ghost too when its block is known,
and its place is outlined in cyan until it is built when it is not; and above the structure float its name, its mod,
its size, how much is in place, what is missing and what is wrong. Looking at a ghost says what goes there and on
which layer.

Its screen has no inventory, since a projector holds nothing: the multiblock in 3D (the layer shown whole, those below
as ghosts, turned by dragging), the list of the multiblocks, what is placed, missing and wrong in the world, and the
turn, the mirror (for a pattern that mirrors), "only what is missing", the layer, and the switch.

**Every multiblock can be shown, with nothing more to write**: every pattern declared or in a file, and every machine
declared as data whose shape is its own. A mod that does not want one shown declares it so:

```java
MultiblockPatterns.declare(rl("secret_core"), pattern, false);   // projectable: false
```

- A multiblock is called by the key `multiblock.<namespace>.<path>`, which its mod's language files give it; with
  none, its id's path is shown with its words capitalised ("coke_oven" as "Coke Oven").
- Every shape lays itself out to be shown: `shape.preview(grow)` gives the places at its least size (a pattern's
  slice its least times, a box its least sides) grown by `grow`, each with its offset from the terminal, its layer
  and the blocks and tags that go there; `preview.turned(orientation)` stands it another way, and
  `preview.materials()` counts what it takes.
- A kit of its own that has multiblocks to show adds them with `Projectables.addSource(...)`, and the terminals it
  knows with `Projectables.addTerminals(...)`. A multiblock's terminal is the block its pattern names, else the
  block built on `DataMachineBlock` for its file (or for a machine whose shape is that pattern);
  `Projectables.preview(id, grow)` lays it out with the terminal at its place.
- What the projector sees, `ProjectorView.of(level, projector)`, is worked out on whichever side asks, from the world
  that side holds: the projector does no work on the server.

### In the recipe viewers

With JEI or EMI installed, every multiblock that can be shown has a page in their category **Multiblocks**, with
nothing to write: the multiblock in 3D (dragged round in JEI, turned a step by a click in EMI), a button for each
layer (or, past five layers, buttons that step through them), the blocks that will do at a place that takes more
than one, how it may stand, its materials with how many of each and its terminal last, how many blocks it has and
its size. Its materials are what it takes, so looking up the uses of a block finds the multiblocks built of it; its
terminal is what it makes, so looking up the terminal finds how to build it. The Structure Projector stands
beside the category.

- **Show in world** shows its ghosts in front of the player, as a projector at their feet would, until it is built or
  they walk away; pressed again, it hides them.
- **Open the page** opens its terminal's page in the manuals, and closing the manual goes back to the viewer.
- What both viewers show is in `MultiblockRecipeViews`, laid out by `MultiblockViewLayout` and drawn by
  `MultiblockCard`, so the two show it alike. An item of a mod's own that shows multiblocks is listed beside them with
  `MultiblockShows.addTool(...)`.

## Building a multiblock for the player

*Added 2026-10-16.*

The **Multiblock Builder**, a handheld instrument of the probes' family, builds a multiblock around its terminal.
The player places the terminal and uses the builder on it: it is aimed at it, and works out the multiblock from the
terminal itself, the shape its machine forms with at its least size, turned the way the terminal faces. Its
display has three pages:

- **Build**: the multiblock's name, size and facing; how many blocks it will place and how many are already there;
  each block it needs, how many, and how many the player has (green when enough, red when not); the first place that
  takes several blocks, the chosen one framed; where the blocks come from; and whether it is ready, or what it lacks,
  or how the building goes.
- **Choices**: every group of places that take several blocks, its chosen block between arrows; left and right
  change it.
- **Source**: where the blocks come from, the player's inventory first.

OK builds: a block at a time, at the pace the settings give, each taken from the source and placed as the player
would place it, so it is the player's and the claims round it have their say. It never breaks a block that stands in
the way. HOLD holds the building and carries it on; SHOW shows the multiblock's ghosts as a projector would while the
builder is held; LIGHT lights the display; POWER turns it off, which stops it. It stops, and says why, when it is
built, when the player walks too far away, when a place is protected or something stands in it, or when the source
runs out; a player without the permission `jscore.multiblock.build` may not start it. A player in creative takes
from the inventory without using it up.

- How fast it builds and how far the player may walk are server settings: `multiblock.blocks_per_second` (5) and
  `multiblock.build_reach` (32 blocks), in the balance file.
- What it plans is a `BuildOrder`, pure: for each missing place its block, the player's choice first and then the
  first the source has, with what it uses and what the source lacks; `BuilderPlan.of(...)` works it out from the
  world, alike on either side.
- A mod with a store of blocks of its own, such as a network's, declares it as a source with
  `BuildSources.declare(id, source)`: what it is called, whether the player may use it now, how many of an item it
  holds, and taking one out and putting one back.

## Machines entirely as data

*Added 2026-10-16.*

A multiblock machine can be declared with no code at all: its shape, its slots, its energy, the kind of recipe it
works and how many at once, in a file under `data/<namespace>/multiblock_machine/`. A data pack adds one, or changes
a mod's:

```json
{"shape": {"box": {"frame": {"blocks": ["minecraft:iron_block"]},
                   "face": {"blocks": ["minecraft:glass"], "tags": ["jscore:multiblock_ports"]},
                   "sides": {"least": 3, "most": 7}, "height": {"least": 3, "most": 7}}},
 "recipes": "mymod:baking",
 "layout": {"inputs": 1, "outputs": 1, "lanes": 2},
 "energy": {"capacity": 10000, "receive": 1000, "per_tick": 10},
 "parallel": {"by": "count", "name": "unit"}}
```

- `shape` is one of `pattern` (a pattern's id, read when it is checked, so a pack's own copy counts), `drawn` (a
  pattern written in the file, as above), `box`, `walls` or `cuboid`, each with the fields of its shape.
- `recipes` is the recipe type of a kind of processing machine some mod declared (`ModContent.processing`); its
  recipes are data as any other.
- `layout` gives `inputs`, `outputs`, `upgrades`, `lanes`, `input_tanks`, `output_tanks`, `tank_capacity` and
  `matter` tanks (`state`, `inputs`, `outputs`, `volume`, `pressure`); what is left out it has none of, and one lane.
- `energy` is in FE; `per_tick` is what a recipe that names no energy spends.
- `parallel` is read `by` `fixed` (with a `value`), `count` or `value` (with a `name`), or `repeats`.

The terminal is one block for each machine, with its own look, built on `DataMachineBlock` with a
`DataMachineBlockEntity`, which names the file:

```java
public static final BlockEntry<DataMachineBlock> OVEN = CONTENT.block("coke_oven",
        properties -> new DataMachineBlock(properties, Device.of(() -> MyContent.OVEN_ENTITY.get())
                .ticks(MachineBlockEntity::serverTick).opensMenu(MachineMenu::new), rl("coke_oven"))).register();
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DataMachineBlockEntity>> OVEN_ENTITY =
        CONTENT.blockEntity("coke_oven",
                (pos, state) -> new DataMachineBlockEntity(MyContent.OVEN_ENTITY.get(), pos, state), OVEN);
```

The files are sent to every player, so the terminal has the same slots on both sides. A machine's slots are made as
its terminal is placed or loaded: a change to a file's layout reaches the terminals placed or loaded after the
reload. A terminal whose block names a file there is not never forms, and Jade names the file it lacks.

## Big blocks

*Added 2026-10-24.*

A **big block** is one block bigger than a block, placed whole: a cabinet three blocks wide, a rack three tall. The
player places it at its **origin** and it raises its other cells round it at once; breaking any cell takes the whole
of it down and gives back what it held. Nothing in it is built block by block and it forms nothing, which is what
tells it from a multiblock. Its kit is in `dev.jstech.core.bigblock`:

```java
public final class CabinetBlock extends AbstractBigBlock {
    private static final BigBlockFootprint FOOTPRINT = new BigBlockFootprint(MultiblockPattern.builder("cabinet")
            .layer("P#P").layer("PPP")
            .where('P', BlockMatch.blocks("mymod:cabinet_part"))
            .build());

    public CabinetBlock(Properties properties) { super(properties); }

    // As for any block facing a way: its codec(), its newBlockEntity(...) at the origin, its state's FACING.
    @Override protected IBigBlockGeometry geometry() { return FOOTPRINT; }
    @Override protected boolean isOwnOrigin(BlockState state) { return state.is(this); }
    @Override protected boolean isOwnPart(BlockState state) { return state.is(MyContent.CABINET_PART.get()); }
    @Override protected BlockState partStateFor(BlockPos origin, Direction facing, BlockPos part, BlockState state) {
        return MyContent.CABINET_PART.get().defaultBlockState().setValue(FACING, facing);
    }
    @Override protected void dropContents(ServerLevel level, BlockPos origin) { /* what the cabinet held */ }
}
```

- `BigBlockFootprint` reads the cells from a drawing in letters, `#` its origin; `IBigBlockGeometry` is the way for
  a footprint of its own.
- The cells' block entities extend `BigBlockPartBlockEntity`, which remembers the origin (`originPos()`), so a cable
  touching any cell reaches the block entity at the origin.
- `getObstructedBlocks(level, origin, facing)` gives the cells that would land on something, and
  `spawnMisplaceParticles` outlines them, for the block's `getStateForPlacement` to refuse the place and show why.

## What can go wrong

- **The shape never forms.** Read the `Failure`: the letter wanted, the place (counted from the terminal), and
  the block found there. Layers go bottom first.
- **It forms, but a count or a size is wrong.** A `COUNT` failure names the count and how many were found; a
  `SIZE` failure names the side and how long it is. Check the limits you declared.
- **An entry is refused where the player put it.** The place is not one its multiblock declared the entry on: it
  fails as a wrong block there. Or there are more of it than its limit, which fails as a `COUNT`.
- **It will not form with one of a pair.** An `APART` failure names two counts that go together, such as an inlet
  with no outlet: add the other, or take the first away.
- **A box or a floor never forms though every block looks right.** For a box, every edge must be frame and every
  face between them face; for a floor and walls, a gap in a wall (or a missing floor block under the inside) is
  enough. A part inside that may not stand there fails as `inside`.
- **A part inside never works.** Its needs are not met: count its neighbours of the kind it needs, and remember a
  part needed must itself be working.
- **A data pack's change is ignored.** Its file is not at `data/<namespace>/multiblock/<path>.json` with the
  pattern's own id, or it does not parse (the log says why).
- **Pipes do not reach the machine.** The part is not a port, its kind does not match (an item pipe on an energy
  port), `stamp` was not called after the match, or `MultiblockPorts.register` was not called for the part type.
