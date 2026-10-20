# Moving structures

Structures of real blocks that move through the world, from a drawbridge to an airship: a lift rising with a mob on
it, a dome turning on an observatory, a train running on its rails, a ship sailing off to another dimension with its
crew aboard. Their blocks are the game's own blocks, of any mod, and they go on working as they move: machines run,
computers boot, networks work and crops grow. It is in the spirit of Create's contraptions and Valkyrien Skies, built
on the Core's own.

The design behind this page is [Moving structures](design/moving-structures.md).

## The words

- A **moving structure** is a set of blocks that moves as one. Its **entity** is its place among the world's
  entities: it is seen by the players near it and saved with the chunk it is over.
- The **yard** is a part of every dimension set aside for the structures, out at the far east of the world, from
  25,600,000 blocks to the world's edge. It is cut into **plots** of 128 chunks a side, one for each structure.
- A structure's blocks are kept in its plot, really there, as blocks are on the ground. Its **pose** says where the
  blocks are drawn and collided in the world: a place and a rotation.
- The **pivot** is the place in the yard the pose stands at; everything turns round it.
- A **motion** is a rule that moves a structure a tick at a time: toward a pose, spinning, swinging on a hinge,
  through a path, facing a direction, along rails.
- A structure **rests** when no motion moves it; at rest it costs nothing.

## Making one

*Added 2026-10-20.*

```java
StructureResult made = MovingStructures.assemble(level, corner, otherCorner);   // every block in the box
StructureResult joined = MovingStructures.assembleJoined(level, seed);          // a block and all joined to it
if (made.ok()) {
    MovingStructure structure = made.structure();
}
```

The blocks are moved whole into a plot of the yard, block entities with everything they keep, quietly: no
neighbour is told on the way, so a torch on a wall moved with the wall stays on it. The structure then stands
exactly where the blocks stood, so nothing on them notices. The world's blocks round them are told at the end, as a
fence beside a removed fence lets go of it.

Nothing of the kit reads or writes a chunk that is not loaded, as a chunk loaded that way in the middle of a tick
can hold the server's chunks up for good. So the structure is made at once, but when the chunks of the plot it gets
are not loaded yet (a plot never used, or one whose chunks went), its blocks stand where they are for the few ticks
the plot takes to load, and only then come into it: `structure.arriving()` says it is still waiting, and
`structure.ready()` that its blocks are its own in the yard. A player sees nothing of it: the blocks stand on the
ground until the moment they stand on the structure, in the same place. Saved while it waits, it goes on waiting as
it loads.

`StructureAssembly.blocks(level, positions, pivot)` makes a structure of any set of blocks, its pivot at a place of
your choosing (a drawbridge's hinge, a rotor's axle); the others put it at the middle of the blocks, on their floor.

A structure is refused, with a sentence a player can read in `refusal()`, when:

| Refused | Why |
| --- | --- |
| There are no blocks there | air makes no structure |
| More blocks than the server allows | the setting `moving_structures.most_blocks`, 65,536 by default |
| A block that stays put | the tag `jscore:stays_put_for_structures`: bedrock, the barrier, portals and their frames |
| Blocks that are a structure's already | they are in the yard |
| Blocks in land that is not loaded | their blocks cannot be known without loading it; a joined structure reaching it is refused too, not cut short |
| The yard is full | every plot of the dimension is taken |

A block set against a structure, by a player or a machine, becomes part of it: the box of its blocks grows to hold
it, and a structure has no size limit at assembly.

## Moving it

*Added 2026-10-20.*

```java
structure.move(new TowardMotion(structure.pose().movedTo(x, y + 12, z), 2, 0));   // up 12 blocks at 2 a second
structure.move(new SpinMotion(0, 1, 0, 30));                                        // 30 degrees a second about up
structure.move(new TurnAboutMotion(hingeX, hingeY, hingeZ, 1, 0, 0, 90, 45));       // a drawbridge rising
structure.move(PathMotion.through(stations, 4, 30, true));                         // a cable car going round
structure.move(new FaceMotion(dx, dy, dz, 10));                                     // an antenna on a satellite
structure.move(RailsMotion.onRails(level, structure.pose(), Direction.SOUTH, 8));  // a train south on its rails
structure.move(null);                                                               // stop where it is
```

| Motion | Moves | Ends |
| --- | --- | --- |
| `TowardMotion` | to a pose, no faster than its speed, turning no faster than its rate | arrived exactly |
| `SpinMotion` | about an axis of the world through the pivot | never, until told |
| `TurnAboutMotion` | by an angle about a hinge, an axis through a place | the angle turned |
| `PathMotion` | through a list of poses in turn | at the last, or starting over |
| `FaceMotion` | turning its forward, its own south, along a direction, its up kept up | facing it |
| `RailsMotion` | along the game's rails, up the slopes and round the curves, turned along the track | the track's end |

Speeds are in blocks a second and rates in degrees a second. A motion in the middle of its way is saved with the
structure and goes on after a restart.

A motion of your own implements `IStructureMotion`: each tick, `step(now, context)` gives the next pose and the motion
that goes on from there, or null when it is done. It is registered with its codec from your constructor:

```java
StructureMotions.register(MyMotion.TYPE, MyMotion.CODEC);
```

A motion that reads the world (as rails do) says so with `readsWorld()` and moves on the server's thread; the others
are worked out beside it, many structures at once, when many move.

`structure.teleport(pose)` puts a structure at a pose at once, carrying what stands on it. The game's own `/tp` moves
a structure's entity the same way.

## Setting it down

*Added 2026-10-20.*

```java
StructureResult down = MovingStructures.setDown(structure);
```

The structure is set down on the world's grid where it is: its heading turned to the nearest quarter turn, its place
to the nearest block, and every block laid where it lands, turned with it (stairs face the new way), block entities
whole. It is refused when it leans more than half a quarter turn (the game's blocks only turn about the up axis),
when a block of the world is in the way, when a block would land out of the world's height or where the land is not
loaded, while its own blocks are not loaded, and while it is on its way to another dimension. A structure still
waiting for its plot is only undone: its blocks never left the ground.

`MovingStructures.takeAway(structure)` removes a structure for good, its blocks gone with it. Taken away while its
plot's chunks are not loaded, its plot is cleared once they have loaded, and only then given to another structure.

## Standing on it

*Added 2026-10-20.*

Entities collide with a structure's blocks however it is turned, and stand on it: a structure turned any way is seen
from the entity as a turned box, swept against its blocks until it touches, then slides along. What lands on a floor
stays on it, never sliding down a gently tilted deck; a step is climbed as on the ground; a sneaking player stops at
a deck's edge; and the block under an entity is read from the structure, so ice aboard is slippery and magma burns.

Whatever stands on a structure is carried as it moves, turning as it turns: a mob on a lift rises with it. What it
moves into is pushed out of its blocks. A thing that jumps off a moving deck goes on with the deck's speed.

A player's own game carries the player before they move, so a deck walked on feels as steady as the ground, and
tells the server where on the deck the player stands; the server puts them there on its own deck. A deck as fast as
a rocket never reads as the player moving too fast, and a player standing on a deck is never sent away for flying.
A player who leaves the game standing on a structure in flight comes back standing where they were on it.

## Using its blocks

*Added 2026-10-20.*

A structure's blocks are aimed at, opened, used, broken and built on as blocks of the ground are: a chest aboard
opens and stays open, a door swings, a block broken drops where it is in the world. Arrows and every other ray hit
them. What the blocks give off goes where they are in the world: their sounds, their particles, their news to the
players near them (a chest's lid, a door), the items they drop and the mobs a spawner aboard makes. It rains on a
structure where it rains on its place in the world, under the sky its own blocks leave it.

## Drawing

*Added 2026-10-20.*

A player's game is sent the chunks of the yard holding a structure's blocks, and the ring of chunks round them, as the
structure comes into sight (the game works light out again from the chunks beside a block that changes, so a chunk
sent alone would come out dark), and the blocks and the light that change there after it, the light too because
the blocks may come into the yard before the server has lit them; and where the structure is, every tick near it and
less often the farther it is ([`SendRate`](../src/main/java/dev/jstech/core/moving/SendRate.java)), from which it
draws the structure moving smoothly a little behind, going on at the speed it last had when a pose is late.

The blocks are drawn as cached meshes, a section of sixteen blocks at a time, built by the game's own section builder,
so every block and fluid looks as on the ground, and built again only when a block of it changes. They take the light
of the world where they are (a structure flying into a cave goes dark, under a roof it dims), and where the player's
game has not that part of the world they keep the light they had; and they keep their own inside (a lamp aboard
lights its deck). The game shades a face by the side it faces, lightest up and darkest down; a section is built twice,
shaded and unshaded, which tells the faces the game shades from those it leaves (a plant's), and each shaded face is
shaded again as it faces however the structure is turned, so a structure on its side or upside down is shaded as
blocks the world holds that way. Block entities draw what they draw of their own, a sign's words, a chest's lid, at
the same place in the same frame as the blocks. A torch's flame, a furnace's smoke and the pieces of a block broken
are given off where the block is drawn, and the glow and coloured light of its lamps shine there; with the player's
dynamic light on, its lamps light the world round it as it goes, the ground under a ship at night lit where it passes,
and the structure takes none of that light back as the world's. Its blocks change hands with the world's in a single
frame: as a structure is made, the land its blocks leave is drawn as it was until the structure is drawn, and as it is
set down it is drawn where it lands, its block entities and glow with it, until the land is drawn with its blocks;
either way the game builds the sections of the world again a few frames after their blocks change, and the frame the
first build begun after the change is drawn is the frame the drawing changes hands. Under a shader pack
(Iris, Oculus) the meshes are handed to the pack's drawing as an entity's are, so the structure looks the same with
or without it. A player sees a structure as far as its blocks reach, not only as far as its middle, and its block
entities whatever part of the world its middle is in.

## Keeping and travelling

*Added 2026-10-20.*

A structure is saved with the chunk its entity is over, with its pose and its motion, and survives a restart in the
middle of a move. While its entity is loaded, the chunks of its plot are kept loaded and ticking, so its machines run
and its crops grow wherever it goes. Far from every player it freezes with its chunk and is saved, unless a chunk
loader is aboard: a Space Persistor, or any block that keeps chunks through the Core, makes the structure keep the
world's chunks under it loaded wherever it goes.

```java
StructureResult sent = MovingStructures.travel(structure, nether, new Vec3(x, 120, z));
MovingStructure there = MovingStructures.byId(nether, structure.id());   // a few ticks later
```

`travel` sends a structure to another dimension with everyone aboard: its blocks go whole from one yard to the
other, it arrives turned as it was and still moving by its motion, and whatever stood on it or was inside it arrives
where it was on it. It goes once the plot it takes over there and the land it arrives at have loaded, with their
entities, a few ticks after it is sent (or the seconds new land takes to be made), working where it is until then
(`structure.departing()`); saved on the way, it goes on as it loads.
A portal sends it the same way. It is refused, saying why, when it is taller than the dimension it goes to has room
for, when that dimension's yard is full, while its blocks are not loaded, and while it is on its way already.

## Blocks that remember other blocks

*Added 2026-10-20.*

Any mod's blocks work aboard as they are. A block entity that keeps the place of another block (a link, a controller)
is told where the blocks moved:

```java
public class MyBlockEntity extends BlockEntity implements IMovesWithStructures {
    @Override
    public void moved(BlockPos was, BlockMove move) {
        linked = move.where(linked);      // follows the block it links to, if that moved with it
    }
}
```

`BlockMove` says where each moved block went, in which levels, and how they were turned. A mod that cannot change
its block entities listens for `StructureBlocksMovedEvent` on the game's bus, after every block entity was told.

A Core block entity's declared fields follow on their own: a place of a block that moved with it (a `BlockPos`, or a
`GlobalPos` of the level it left) becomes where that block went, and a block's look it holds (a `BlockState`) turns
as the blocks turned. What it keeps otherwise, it follows from a declared listener:

```java
fields().whenMoved((was, move) -> {
    route = route.stream().map(move::where).toList();   // places kept in a list follow too
    MyRegistry.forget(move.from(), was);                 // what it left where it stood is put away
});
```

A block moved is never broken: its `onRemove` is told it is moved as by a piston (`movedByPiston`), and nothing of
what `whenBroken` declares happens. What a block leaves behind where it stood, an entry in a registry, light it laid,
a ticket, is put away in `moved` or `whenMoved`, never in `onRemove`.

The Core's own kits work aboard so:

| Kit | Aboard |
| --- | --- |
| Multiblocks | a formed machine crosses formed and merged, never formed again, and stands formed set down turned; nothing is given back where it stood |
| Cables | every wire leaves its grid where the cable stood and joins it where it is |
| Mechanical power | the networks the parts turned in where they stood are worked out again |
| Lamps | the light a spotlight's beam laid where it stood goes out |
| Peripheral links | a peripheral stays linked to an owner that moved with it; an owner keeps its ports, hubs and disabled flags where the peripherals are now |
| Radio relays | a relay paired with one that moved with it stays paired, in another dimension too |
| Atmosphere Detector | the room it asked for where it stood is no longer asked for |
| Overhead lines | a wire hung between two insulators that moved together goes with them; one hung to an insulator that stayed falls, its wire left there |
| Radiation | a source goes on giving off from where its block is now |
| Data networks | the cables an owner of a network touched where it stood are forgotten |

## Finding structures

*Added 2026-10-20.*

| Call | Gives |
| --- | --- |
| `MovingStructures.byId(level, id)` | the structure of an id loaded in a level |
| `MovingStructures.at(level, pos)` | the structure whose blocks hold a place of the yard |
| `MovingStructures.near(level, box)` | the structures whose blocks may fill some of a box of the world |
| `MovingStructures.inWorld(level, place)` | where a place of the yard is in the world now; any other place as it is |

`structure.frame()` turns places both ways: `worldX/Y/Z` from the yard to the world, `yardX/Y/Z` back, and
`turnX/Y/Z` for directions.

## Commands

*Added 2026-10-20.*

For game masters, as `/jstech structure ...`:

| Command | Does |
| --- | --- |
| `assemble <from> <to>` | makes a structure of every block in the box |
| `assemble joined <block>` | makes a structure of a block and every block joined to it |
| `move <structure> <place> <speed>` | sends it to a place, in blocks a second |
| `spin <structure> <rate>` | spins it about the up axis, in degrees a second |
| `swing <structure> <hinge> <angle> <rate>` | swings it on a hinge across the way you face |
| `rails <structure> <speed>` | runs it on the rails under it, the way you face |
| `stop <structure>` | brings it to rest |
| `setdown <structure>` | sets it down on the grid |
| `list` | names the structures loaded in the dimension |

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `moving_structures.most_blocks` | 65,536 | The most blocks one structure may be made of. |

## What can go wrong

| What you see | Why |
| --- | --- |
| A structure is refused | its `refusal()` says why: a block that stays put, too many blocks, nothing there. |
| A structure stands still after a restart with no player near | it froze with its chunk; with a chunk loader aboard it keeps working. |
| A block entity of another mod forgets a link after a move | that mod keeps absolute places and does not listen for the move; `StructureBlocksMovedEvent` is for it. |
| Something a block left where it stood stays there after a move | its cleanup is in `onRemove` or `whenBroken`, which a move never calls; put it in `moved` or `whenMoved`. |
| A structure just made is not `ready()` | its plot is loading; its blocks come into it in a few ticks. Wait for `ready()` before reading its blocks in the yard. |
| `travel` succeeded but the structure is still here | it goes once the chunks it goes to have loaded; find it there by its id a few ticks later. |
| A structure cannot be set down | it leans too far, something is in the way, it would land out of the world or where the land is not loaded, or its blocks are not loaded yet. |
