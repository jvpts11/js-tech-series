# Multiblocks

A **multiblock** is a machine built from many blocks placed in a shape: a furnace of nine blocks, a reactor of a
hundred. One block of the shape is the **controller**, the one that holds the machine's state and runs it; the
others are its **parts**. J's Core says what the shape is, checks whether the blocks in the world make it, and
lets pipes reach the machine through the parts you mark as **ports**.

## A pattern

*Added 2026-04-30.*

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
- `#` is the controller. A space is any block at all.
- `where` says what a letter stands for: `BlockMatch.blocks(...)` one of those blocks, `BlockMatch.tag("...")` any
  block of a tag.
- `port` marks a letter as a port of a kind: `ITEM_INPUT`, `ITEM_OUTPUT`, `FLUID_INPUT`, `FLUID_OUTPUT`,
  `ENERGY_INPUT`, `ENERGY_OUTPUT`.
- The shape is found facing any of the four horizontal ways, so a player can build it turned.

### Patterns are data

`declare` keeps the pattern under its id, and the data generation writes it to
`data/<namespace>/multiblock/<path>.json`. When the game loads, the file is what counts: a data pack can ship its
own copy and change the shape, the blocks, or the ports, with no code. `MultiblockPatterns.get(id)` gives the
pattern in force.

A pattern can also be read from a structure block's file, built in the game:
`MultiblockPatterns.fromStructure(name, structureTag, "mymod:furnace_controller", Map.of("mymod:furnace_hatch",
PortKind.ITEM_INPUT))`: the named block is the controller, the blocks of the map are ports, air is free, and every
other block must be there as it was saved.

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

## Turning, mirroring and lying down

*Added 2026-10-16.*

Every pattern is found in its four horizontal turns. Two more calls widen that:

- `mirrors()`: it is also found mirrored, for a shape that is not the same both ways round (eight ways in all).
- `liesDown()`: it is also found lying on its side, the controller facing up or down (twenty-four ways; forty-eight
  with `mirrors()` too).

The formed structure's `orientation()` says which way it stands. The four turns are tried first, so a symmetric
shape reads as the turn you would expect.

## Shapes that grow

*Added 2026-10-16.*

A pattern can repeat one **slice** a number of times, for a structure the player makes as long or as tall as they
want within limits: the middle layer of a tower, one oven of a battery.

```java
MultiblockPattern.builder("distillation_tower")
        .layer("CCC", "C#C", "CCC")     // the base, with the controller
        .layer("GGG", "G G", "GGG")     // one storey, repeated
        .layer("CCC", "CCC", "CCC")     // the top
        .where('C', BlockMatch.blocks("mymod:casing"))
        .where('G', BlockMatch.blocks("mymod:tower_glass"))
        .repeatLayer(1, 2, 12)          // from two to twelve storeys
        .build();
```

`repeatLayer` repeats a layer (counted from the bottom), `repeatRow` a row (from the north) and `repeatColumn` a
column (from the west). A pattern repeats one slice at most, never the controller's own, and no more than 512
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
each is checked from its controller, which stands anywhere on the shape.

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

- **A box** is found by walking along its shell from the controller to its edges, then along an edge for its depth:
  it is whatever size the player built. A box larger than its most fails as too large rather than forming smaller.
  `sides` limits both horizontal sides, `height` its height; each side is counted with its shell, so a 3 by 3 by 3
  box has one place inside.
- **A floor and walls** start from the place behind the controller (which stands in a wall), go down to the floor,
  fill out the floor's outline at that level, and rise layer by layer while the walls run unbroken. The corners
  where two walls meet are not needed. `area` limits how many places the floor has, `height` how high the walls are,
  the floor not counted. A gap in a wall lets the inside run out, which reads as a floor too large.
- **A cuboid** runs from the controller along each axis as far as its units go, and every place of it must hold a
  unit. Count the kinds of unit through the unit's place ("at least one storage unit").

No side of a shape by rule is longer than 64 blocks, and no floor larger than 4,096 places.

## Checking the shape

*Added 2026-04-30. The full result, for every shape, added 2026-10-16.*

Every shape, drawn or by rule, is an `IStructureShape`, checked against the world from its controller. Read the
world through `MultiblockPatterns.provider(level)`, with places packed by `PatternMatcher.encodePosition`:

```java
ShapeMatch result = MyContent.TANK.check(MultiblockPatterns.provider(level),
        PatternMatcher.encodePosition(pos.getX(), pos.getY(), pos.getZ()));
switch (result) {
    case ShapeMatch.Formed formed -> {
        // formed.parts(): the shell but the controller; formed.inside(): the places inside
        // formed.sizeX(), sizeY(), sizeZ(), repeats(); formed.count("valve"), formed.value("heat", 0)
        // formed.orientation(), formed.ports() (for a pattern)
    }
    case ShapeMatch.Failure failure -> {
        // failure.wrong(): BLOCK, COUNT or SIZE
        // failure.at(), relX(), relY(), relZ(): the place, and its offset from the controller
        // failure.wanted(): the letter, the part of the shape (frame, face, inside, floor, wall, unit),
        //                   the count's name, or the size (x, y, z, area)
        // failure.found(): the block found there, or the number found
    }
}
```

When no way of standing forms, the failure told is the one of the way that went furthest before failing: the way
the player most likely meant, so the place it names is one they can see is wrong.

The older form, for a pattern in its four turns, still works:

```java
IMatchResult result = MultiblockPatterns.match(MyContent.FURNACE, level, controllerPos);
if (result instanceof IMatchResult.Success formed) {
    // formed.rotation(): which way it was built
    // formed.slavePositions(): every part's place
    MultiblockPorts.stamp(level, formed);
} else if (result instanceof IMatchResult.Failure failure) {
    // failure.expectedChar() and failure.actualBlockId(): what was wanted where, and what is there
}
```

Check when the controller is placed or used, and when a part next to it changes; a failure says which place is
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
with `BlockMatch` can be written: a pattern that fits a place, counts or reads its inside with code of its own
cannot go to a file, and the data generation says which.

## A machine that is a multiblock

*Added 2026-10-16.*

A machine of the Core's kit ([Machines](MACHINES.md)) becomes a multiblock's controller with one call in its
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
  formed, what it needs and where: "Needs Block of Iron at 1, 1, 1 from the controller; found Dirt", "Needs from 1
  to 4 of valve; has 0".
- **Only its owner and those its owner lets break it** break the blocks of a formed structure, whatever blocks they
  are, as with every owned block ([Ownership](OWNERSHIP.md)).
- Breaking any block of it undoes the structure; the controller keeps what it holds.

It is checked again only when a block changes in the box it watches (everything its shape can reach while it is not
formed, its structure and the places beside it once formed), a few ticks after the change, and once a minute besides,
never every tick. It is never checked while a chunk it reaches is not loaded, and a chunk that unloads never undoes
it: it stays as it was until it can be read again. `LevelStructures.of(level).controllerHolding(pos)` says which
formed structure a place belongs to.

## Ports

*Added 2026-10-05.*

A part block entity extends `MultiblockPartBlockEntity`; register its capabilities once, and the parts that are
ports pass pipes and cables through to the controller:

```java
// In your mod's constructor:
modBus.addListener(RegisterCapabilitiesEvent.class,
        event -> MultiblockPorts.register(event, MyContent.FURNACE_PART_BE.get()));
```

`MultiblockPorts.stamp(level, formed)` marks each part with the port the pattern gives it, or as a plain part. A
pipe against an item input port then reaches the controller's items; against a plain part, nothing. The
controller says what each port opens onto by implementing `IMultiblockPortHost`.

Ports come for items, fluid and energy, and for each other state of matter (`GAS_INPUT`, `SLURRY_OUTPUT`,
`PLASMA_INPUT` and the rest); the controller answers the matter ports in `matterPort(kind, part, side)` with the
store of the port's state.

For the block side, `AbstractMultiblockControllerBlock` holds what every controller block does (its shape, which
blocks are its own parts, what happens when it breaks), so a mod writes only what differs.

## What can go wrong

- **The shape never forms.** Read the `Failure`: the letter wanted, the place (counted from the controller), and
  the block found there. Layers go bottom first.
- **It forms, but a count or a size is wrong.** A `COUNT` failure names the count and how many were found; a
  `SIZE` failure names the side and how long it is. Check the limits you declared.
- **A box or a floor never forms though every block looks right.** For a box, every edge must be frame and every
  face between them face; for a floor and walls, a gap in a wall (or a missing floor block under the inside) is
  enough. A part inside that may not stand there fails as `inside`.
- **A part inside never works.** Its needs are not met: count its neighbours of the kind it needs, and remember a
  part needed must itself be working.
- **A data pack's change is ignored.** Its file is not at `data/<namespace>/multiblock/<path>.json` with the
  pattern's own id, or it does not parse (the log says why).
- **Pipes do not reach the machine.** The part is not a port, its kind does not match (an item pipe on an energy
  port), `stamp` was not called after the match, or `MultiblockPorts.register` was not called for the part type.
