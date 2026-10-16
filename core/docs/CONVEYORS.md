# Conveyors

*Added 2026-10-16.*

Belts that carry items, and the players and mobs that stand on them, from one block to the next and into whatever
stands at their end. The kit is the Core's; each mod declares its own kinds of belt, with their speed, how many items
a block holds and what makes them move, and draws its own belt. The items on a belt are a queue of numbers, never
entities, so a factory floor of belts weighs little.

The design behind this page is [Conveyors](design/conveyors.md).

## A kind of belt

A `ConveyorType` says what a kind of belt is. Every number in it is the mod's own; the kit runs any:

```java
public static final ConveyorType BELT = ConveyorType.moving(2.0)   // blocks a second at its full speed
        .perBlock(4)                                                 // items one behind another on a block
        .drive(ConveyorDrive.mechanical(60, 2))                      // what makes it move
        .end(ConveyorEnd.HOLD)                                       // what it does at an end with nowhere to go
        .carriesEntities()                                           // players, mobs and items ride it
        .height(4)                                                   // how high the belt is, in pixels
        .build();
```

| What | Range | Unless said |
| --- | --- | --- |
| Speed | more than nothing, up to 20 blocks a second (a block a tick) | (required) |
| Items a block | 1 to 8; they keep a gap of a block over that number | 4 |
| Drive | `free()`, `mechanical(ratedRpm, torque)`, `energy(watts)` | `free()` |
| End | `HOLD` (they wait there) or `DROP` (they fall off) | `HOLD` |
| Carries what stands on it | yes or no | no |
| Height | 1 to 16 pixels | 4 |

## The block and its block entity

A belt is a `ConveyorBlock`, facing the way it moves (a belt laid moves away from the player who lays it), and its
block entity a `ConveyorBlockEntity`, of a type your mod registers:

```java
public static final BlockEntry<ConveyorBlock> BELT = CONTENT.block("belt",
        properties -> new ConveyorBlock(properties, MyTypes.BELT, () -> MyContent.CONVEYOR.get()))
        .named("Belt").look(IBlockLook.facing(new IBlockModel.Handmade("belt"))).register();
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR =
        CONTENT.blockEntity("conveyor", (pos, state) -> new ConveyorBlockEntity(CONVEYOR.get(), pos, state), BELT);
```

The block's state has `facing` and `end`, which says whether the belt ends its run. A block state file that names only
`facing` covers both. Many kinds of belt may share one block entity type. The Core finds every conveyor block of every
mod by itself: it offers their item capability and draws the items on them, with nothing for your mod to register.

## Runs

Belts of one kind facing the same way, one behind another, are a **run**. Only the belt at the end of a run ticks,
and it moves the whole run, from its end back, so an item handed on frees room behind it in the same tick: a run of
any length costs one belt's tick. A run that carries nothing is not walked again until an item comes on. A run is at
most 256 belts long; a longer line is several runs, each handing on to the next.

A belt of another kind in front, or one facing another way, is not of the run: the run hands its items on to it.

## Items on a belt

Each item on a belt stands somewhere along it, from 0 where the belt begins to 1 where it ends (`lane()` lists them,
the one nearest the end first). Each tick they move as far as the run moves, each a gap behind the one ahead; the
first waits at the end until it can go on. A stack of any size is one item on the belt.

At the end of a run, the first item goes, in this order:

1. into a **belt in front**: at its start when it faces the same way, at its middle when it runs across, never into
   one that faces back;
2. into an **inventory in front**, through the game's item capability: a chest, a hopper, a machine; what it cannot
   take stays on the belt;
3. with nowhere to go, it **waits** at the end, or **falls off** into the world, as the kind says.

## Putting items on

- **From behind**, at the belt's start, and **from the sides and above**, at its middle, through the game's item
  capability: hoppers, pipes, machines, the Core's item lines. Nothing goes in from the front, where the belt gives.
- **An item that lands on the belt** is taken onto it where it lands, when there is room.
- **Your code** calls `belt.put(stack, at)`, which says whether there was room.

## What makes it move

- **By itself** (`free()`): always at its full speed, for a belt your mod powers some other way, or not at all.
- **A shaft** (`mechanical(ratedRpm, torque)`): every belt of the run turns with the others, a chain of rollers in one
  network of [mechanical power](MECHANICAL_POWER.md), and takes an axle on both its sides, on the axis across the
  belt. A shaft at any belt of the run drives it all. The run moves at its full speed at `ratedRpm` and slower below
  it, either way round, and each belt asks `torque` newton-metres of the network.
- **Energy** (`energy(watts)`): each belt keeps a second of what it draws and takes energy in on every side; the run
  draws on all its belts' stores together, so energy taken in at one belt runs them all. A run that gets less than it
  draws moves that much slower. It draws only while it carries anything.

## Players and mobs

A kind that carries what stands on it moves players, mobs and other entities on its belt the way it runs, as fast as
it runs, while it runs. A player who sneaks stands still.

## Drawing

The Core draws the items on every belt, lying flat on it, moving smoothly between one word from the server and the
next. The belt itself is your block's model.

## Testing

GameTests set belts down with `helper.setBlock` and watch what happens; the test mod's
`ConveyorGameTests` is a worked set:

```java
helper.setBlock(first, BELT.get().defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST));
helper.setBlock(second, BELT.get().defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST));
helper.setBlock(ahead, Blocks.CHEST);
belt(helper, first).put(new ItemStack(Items.DIAMOND, 3), 0.0);
helper.succeedWhen(() -> /* the diamonds are in the chest */);
```

`BeltLane`, the queue of a belt's items, is plain Java and tested with JUnit.

## What can go wrong

| What you see | Why |
| --- | --- |
| Items stop where two belts meet | The belt in front faces back into the one behind: a belt never hands on head on. |
| A belt on a shaft stands still | The shaft is not on the axis across the belt, or the network is jammed or not turning. |
| A belt on energy crawls | It gets less than its watts for every belt of the run. |
| Items pile up at an end | The block in front takes nothing from that side, and the kind holds what it cannot hand on. |
| Building fails with "a conveyor moves faster than nothing..." | The speed is not above zero or is over 20 blocks a second. |
