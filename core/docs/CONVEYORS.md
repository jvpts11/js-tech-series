# Conveyors

*Added 2026-10-16.*

Belts that carry items, and the players and mobs that stand on them, from one block to the next and into whatever
stands at their end; belts that rise and fall a block; chutes items fall down; bucket elevators that lift them;
splitters, joins and diverters that deal them out, gather them in and sort them; and machines that work items as they
pass under them. The kit is the Core's; each mod declares its own kinds of conveyor, with their speed, how many items a
block holds and what makes them move, and draws its own blocks. The items on a conveyor are a queue of numbers, never
entities, so a factory floor of belts weighs little. For items sent far, there is the
[pneumatic post](#pneumatic-post): stations with an address and capsules shot through tubes.

The design behind this page is [Conveyors](design/conveyors.md).

## A kind of conveyor

A `ConveyorType` says what a kind of conveyor is. Every number in it is the mod's own; the kit runs any:

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

## The pieces

Every block of conveyor is a `ConveyorPieceBlock`, of a kind, and its block entity a `ConveyorBlockEntity`, of a type
your mod registers. Many pieces, of any kinds, may share one block entity type:

```java
public static final BlockEntry<ConveyorBlock> BELT = CONTENT.block("belt",
        properties -> new ConveyorBlock(properties, MyTypes.BELT, () -> MyContent.CONVEYOR.get()))
        .named("Belt").look(IBlockLook.facingByState(state -> new IBlockModel.Handmade(
                "belt_" + state.getValue(ConveyorBlock.SLOPE).getSerializedName()))).register();
public static final BlockEntry<ChuteBlock> CHUTE = CONTENT.block("chute",
        properties -> new ChuteBlock(properties, MyTypes.CHUTE, () -> MyContent.CONVEYOR.get()))
        .named("Chute").look(IBlockLook.fixed(new IBlockModel.Handmade("chute"))).register();
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR =
        CONTENT.blockEntity("conveyor", (pos, state) -> new ConveyorBlockEntity(CONVEYOR.get(), pos, state),
                BELT, CHUTE);
```

| Piece | What it does | Its state |
| --- | --- | --- |
| `ConveyorBlock` | A belt, flat or sloped a block up or down to its front. | `facing`, `slope`, `end` |
| `ChuteBlock` | A tube items fall down through, into what is under it. | `end` |
| `ElevatorBlock` | Buckets that lift items up, and tip them out to their front at the top. | `facing`, `end` |
| `SplitterBlock` | A belt that deals items to its front, left and right in turn. | `facing`, `end` |
| `JoinBlock` | A belt that takes from behind and both sides in turn. | `facing`, `end` |
| `DiverterBlock` | A belt that turns aside what its filter names. | `facing`, `side`, `end` |

`end` says whether the piece ends its run; a block state file need not name it. The Core finds every piece of every
mod by itself: it offers their item capability and draws the items on them, with nothing for your mod to register. A
mod's own piece extends `ConveyorPieceBlock` (or `BeltPieceBlock`, for one that lies in its block like a belt) and says
where items come on and leave (`entry`, `exit`), where it takes them in from each side (`intakeAt`), where it hands
them at the end of a run (`outlets`) and the line they lie along (`geometry`).

## Runs

Pieces of one kind, each leaving where the next comes on, are a **run**: belts facing the same way one behind
another, flat or sloped; chutes one above another; elevators one above another. Only the piece at the end of a run
ticks, and it moves the whole run, from its end back, so an item handed on frees room behind it in the same tick: a
run of any length costs one piece's tick. A run that carries nothing is not walked again until an item comes on, and
the end keeps the run it walked, walking its pieces again only once a piece is laid, broken or turned somewhere, or
one of them is gone with its chunk. A line of more than about a thousand pieces is cut into runs of about that many,
each handing on to the next.

What items riding cost the server, measured by the test mod's `ConveyorBenchGameTests`: 3,680 items on 2,024 belts,
all moving at once, about a millisecond a tick, a third of a microsecond an item.

A piece of another kind in front, or one facing another way, is not of the run: the run hands its items on to it.
Splitters, joins and diverters stand alone, each its own run, taking in and handing on through the item capability.

## Items on a conveyor

Each item on a piece stands somewhere along it, from 0 where the piece begins to 1 where it ends (`lane()` lists them,
the one nearest the end first). Each tick they move as far as the run moves, each a gap behind the one ahead; the
first waits at the end until it can go on. A stack of any size is one item on the piece.

At the end of a run, the first item goes to the piece's **outlets**, through the game's item capability:

1. a belt's is the block it leaves into: a belt there takes it at its start when it faces the same way and at its
   middle when it runs across, never when it faces back; a chest, a hopper or a machine takes what it can, and what it
   cannot stays on the belt;
2. a chute's is the block under it;
3. an elevator's are the block in front of its top, then the block above;
4. with nothing that takes items at all, it **waits** at the end, or **falls off** into the world, as the kind says.

A broken piece lets fall what was on it.

## Putting items on

- **Through the game's item capability**, where the piece takes them in: a belt from behind at its start and from the
  sides and above at its middle; a chute from above at its top and from its sides halfway down; an elevator at any side
  or from below, at its foot. Nothing goes in where a piece gives. Hoppers, pipes, machines and the Core's item lines
  all put items on this way.
- **An item that lands on a belt**, or falls into a chute's open top, is taken on where it lands, when there is room.
- **Your code** calls `piece.put(stack, at)`, which says whether there was room.

## Slopes

A belt rises a block to its front (`BeltSlope.RISING`) or falls a block to its front from the top of its back
(`FALLING`). A belt laid where a belt of its kind waits a block above, ahead or behind slopes to meet it, and the flat
belt it meets slopes to meet it back when nothing else holds that end, as a rail does; a wrench, any item in the
`c:tools/wrench` tag, turns a belt's slope from flat to rising to falling and back. A sloped belt is a stair of four
steps to walk on, and its items lie tipped along it.

## Chutes and elevators

A chute moves by itself or on energy, never on a shaft: its kind's speed is how fast its items fall. An elevator moves
by itself, on energy or on a shaft across the way it tips out, at any of its blocks.

## Splitting, joining and sorting

- A **splitter** hands each item to the next of its front, left and right in turn, passing over a way that takes
  nothing.
- A **join** takes from behind, its left and its right in turn, as a zip does, so no line waits on another for long.
- A **diverter** turns the items its filter names to its side and lets every other go straight on. Used with an item, it
  adds that item to its filter, whatever the item's damage and components; used empty-handed while sneaking, it forgets
  its filter; a wrench turns it to divert to its other side. With nothing in its filter it diverts nothing. Your code
  sets its filter with `piece.setFilter(filter)`, any `ItemFilter`.

## Work on the belt

A machine whose block entity implements `IBeltWorker`, set on top of a flat belt, works the items that pass under it:
the belt stops each item the machine `works` at its middle, keeps the items behind it waiting a gap back, asks the
machine to `work` it each tick it moves, and lets it go on as what the machine made of it once the machine is done. If
the item goes away first, the machine hears `abandoned()` and starts over.

```java
public class PressBlockEntity extends BlockEntity implements IBeltWorker {
    public boolean works(ItemStack item) {
        return item.is(Items.IRON_INGOT);
    }

    public @Nullable ItemStack work(ItemStack item) {
        return ++pressed < 20 ? null : new ItemStack(MyItems.IRON_PLATE.get(), item.getCount());
    }
}
```

## What makes it move

- **By itself** (`free()`): always at its full speed, for a piece your mod powers some other way, or not at all.
- **A shaft** (`mechanical(ratedRpm, torque)`): every piece of the run turns with the others, a chain of rollers in
  one network of [mechanical power](MECHANICAL_POWER.md), and takes an axle on both its sides, on the axis across the
  belt. A shaft at any piece of the run drives it all. The run moves at its full speed at `ratedRpm` and slower below
  it, either way round, and each piece asks `torque` newton-metres of the network.
- **Energy** (`energy(watts)`): each piece keeps a second of what it draws and takes energy in on every side; the run
  draws on all its pieces' stores together, so energy taken in at one piece runs them all. A run that gets less than it
  draws moves that much slower. It draws only while it carries anything.

## Players and mobs

A kind that carries what stands on it moves players, mobs and other entities on its belts the way they run, as fast
as they run, while they run. A player who sneaks stands still.

## Drawing

The Core draws the items on every piece, lying flat on a flat belt, tipped along a slope, upright in a chute or an
elevator, moving smoothly between one word from the server and the next, and standing still under a machine that
works them. The piece itself is your block's model.

## Testing

GameTests set pieces down with `helper.setBlock` and watch what happens; the test mod's `ConveyorGameTests` and
`ConveyorPieceGameTests` are worked sets, and `ConveyorClientTests` photographs them:

```java
helper.setBlock(first, BELT.get().defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST));
helper.setBlock(second, BELT.get().defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST));
helper.setBlock(ahead, Blocks.CHEST);
piece(helper, first).put(new ItemStack(Items.DIAMOND, 3), 0.0);
helper.succeedWhen(() -> /* the diamonds are in the chest */);
```

`BeltLane`, the queue of a piece's items, and `ConveyorType` are plain Java and tested with JUnit.
`ConveyorBenchGameTests` measures items riding at scale, alone in its batch.

## What can go wrong

| What you see | Why |
| --- | --- |
| Items stop where two belts meet | The belt in front faces back into the one behind: a belt never hands on head on. |
| A slope does not join the belt above | It was laid flat: turn it with a wrench, or lay the upper belt after the lower one. |
| A belt on a shaft stands still | The shaft is not on the axis across the belt, or the network is jammed or not turning. |
| A belt on energy crawls | It gets less than its watts for every piece of the run. |
| Items pile up at an end | The block in front takes nothing from that side, and the kind holds what it cannot hand on. |
| A diverter lets everything through | Its filter names nothing: use it with the item to turn aside. |
| A machine above never works | The belt under it is sloped, or the machine's `works` says no to the item. |
| Building fails with "a chute's kind of conveyor moves on a shaft" | A chute has no shaft: give its kind a free drive or one on energy. |
| Building fails with "a conveyor moves faster than nothing..." | The speed is not above zero or is over 20 blocks a second. |

## Pneumatic post

*Added 2026-10-24.*

Belts carry a lot over a short way; a **pneumatic post** carries less, far, as the tubes of Paris (1866), of
department stores and of hospitals did. It is the package `dev.jstech.core.transit`:

- **Stations** (`CapsuleStationBlockEntity`) stand on a line of tubes, each with an **address** (`setAddress`). What
  is put into a station, by a hopper, a machine or a line of items, it sends in a **capsule** to a station of the
  address its routes give that item: the first `CapsuleRoute` whose filter lets it through, else the address it sends
  everything else to (`setOtherwise`). What it has no address for, it does not take. It **pushes only**: a station
  never asks another for anything.
- **The tubes** are a line of the mod's on a grid of `GridPolicies.GROUP` ([Lines that group](CABLES.md#lines-that-group)):
  a run joins the stations on it, and a capsule goes to the nearest station of its address the run joins.
- **Capsules are physical.** A station sends one as often as its rules let, carrying at most their stacks, and the
  trip takes time by the distance between the two stations at the rules' speed. A capsule on its way is kept with the
  world (`CapsuleTransit`), so one sent before the server stopped arrives after it starts.
- **Arriving**, a capsule unloads into its station, which unloads into the block it faces, keeping what does not fit
  until it does. A station that cannot take a capsule yet, full or in a chunk not loaded, has it wait a second and try
  again; one whose station is gone drops its items where the station was.

```java
public static final GridKind TUBES = GridKind.declare(rl("capsule_tube"), GridPolicies.GROUP);
public static final CapsuleRules RULES = new CapsuleRules(4, 20, 20.0);   // 4 stacks, one a second, 20 blocks a second

// Its type, made with (pos, state) -> new CapsuleStationBlockEntity(STATION_BE.get(), pos, state, TUBES, RULES),
// ticked by CapsuleStationBlockEntity::serverTick, and in the mod's RegisterCapabilitiesEvent:
CapsuleStationBlockEntity.register(event, STATION_BE.get());

station.setAddress(1);
station.setRoutes(List.of(new CapsuleRoute(ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0)), 3)));
station.setOtherwise(7);   // iron to station 3, the rest to station 7
```

A mod makes sending cost something by overriding `readyToSend` and `sent` on its station: J's Industrial's stations
shoot each capsule with compressed air, and stop without it. Every face of a station takes what it has an address
for and gives what arrived (`items()`), and the tubes find it on every face. The test mod's `IndustrialAirGameTests`
is a worked set: routes, the trip's time, a capsule kept with the world, and a station without air.

*Added 2026-10-27.* A station is also read and set from outside as a machine is (`IMachineParameters`, on every face):
its `address` and `otherwise`, which a controller sets, and `waiting`, how many stacks wait to go. `sendNow(level, to)`
has it send what waits in it to the station of address `to` at once, whatever its routes say, and answers a
`SendOutcome`: `SENT`; `NOT_YET` while its next capsule is not due or its mod's cost is not there; `NOTHING` when
nothing waits; `NO_STATION` when no station of that address is on its tubes. Its rules' pace counts the capsules that
left, so a look that sent nothing never holds a capsule asked for back. J's Industrial's Industrial Controller Computer
uses it for `RUN jsindustrial:station_send`.

| What you see | Why |
| --- | --- |
| A station takes nothing in | It has no route that lets the item through and no address for the rest. |
| Items wait in a station and never leave | No station of their address is on its run of tubes, or its mod's cost (air) is not there. |
| A capsule's items lie on the ground | The station it was going to was broken while it was on its way. |
