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

## Checking the shape

*Added 2026-04-30.*

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

Check when the controller is placed or used, and when a part next to it changes; a `Failure` says which place is
wrong, which is what a player needs to be told.

## Ports

*Added 2026-10-05.*

A part block entity extends `MultiblockPartBlockEntity`; register its capabilities once, and the parts that are
ports pass pipes and cables through to the controller:

```java
@SubscribeEvent
static void capabilities(RegisterCapabilitiesEvent event) {
    MultiblockPorts.register(event, MyContent.FURNACE_PART_BE.get());
}
```

`MultiblockPorts.stamp(level, formed)` marks each part with the port the pattern gives it, or as a plain part. A
pipe against an item input port then reaches the controller's items; against a plain part, nothing. The
controller says what each port opens onto by implementing `IMultiblockPortHost`.

For the block side, `AbstractMultiblockControllerBlock` holds what every controller block does (its shape, which
blocks are its own parts, what happens when it breaks), so a mod writes only what differs.

## What can go wrong

- **The shape never forms.** Read the `Failure`: the letter wanted, the place (counted from the controller), and
  the block found there. Layers go bottom first.
- **A data pack's change is ignored.** Its file is not at `data/<namespace>/multiblock/<path>.json` with the
  pattern's own id, or it does not parse (the log says why).
- **Pipes do not reach the machine.** The part is not a port, its kind does not match (an item pipe on an energy
  port), `stamp` was not called after the match, or `MultiblockPorts.register` was not called for the part type.
