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

## By its physics

*Added 2026-10-20.*

A structure let go moves by its physics, as a body of real mass: it falls, rests on the ground and on other
structures, floats, sinks, flies on its engines and wings, rolls on its wheels and rises on its balloons. It goes on
until another motion is set; how fast it goes and turns is saved with it, so a ship in flight flies on after a restart.

```java
structure.move(PhysicsMotion.AT_REST);                 // let it go: it falls, or floats, as it would
structure.move(PhysicsMotion.going(0, 0, 12));         // thrown south at 12 metres a second
```

What each block is, for the physics, is data: the data map `jscore:block_physics`. A block it says nothing of weighs
its density (`jscore:block_density`) times the volume of its shape, displaces that volume in a liquid, and gives no
force of its own.

```json
{ "values": {
  "mymod:rocket_engine": { "thrust": { "newtons": 400000, "kind": "rocket", "throttle": "back" } },
  "mymod:propeller":     { "thrust": { "newtons": 8000, "kind": "propeller", "exhaust": 60 } },
  "mymod:shaft_screw":   { "thrust": { "newtons": 20000, "kind": "propeller", "exhaust": 60, "shaft": 120 } },
  "mymod:wing":          { "mass": 40, "wing": { "area": 1, "stall": 15, "lift": 1.2 } },
  "mymod:rudder":        { "wing": { "area": 1, "face": "facing" } },
  "mymod:wheel":         { "wheel": { "radius": 0.5, "stiffness": 60000, "damping": 6000, "steer": 30,
                                       "drive": 4000, "brake": 8000 } },
  "mymod:float":         { "mass": 20, "displacement": 1.0 },
  "mymod:hover_coil":    { "levitation": 20000 },
  "mymod:electromagnet": { "magnet": { "newtons": 50000, "reach": 6, "power": "any" } },
  "mymod:burner":        { "burner": { "watts": 300000, "power": "any" } },
  "mymod:envelope":      { "mass": 0.5, "drag": 0.8 }
} }
```

| Field | What it is |
| --- | --- |
| `mass` | its mass, in kilograms |
| `displacement` | the volume it displaces in a liquid, in cubic metres, 0 to 1 |
| `drag` | how much its faces drag, as a share of a flat face's |
| `thrust` | an engine pushing the way the block faces: `newtons` at full throttle; `kind` `rocket` (the same anywhere, a vacuum too), `jet` (less as the air thins) or `propeller` (less again as the craft nears its `exhaust` speed); `throttle`, the side its signal comes from; `shaft`, for a propeller a shaft turns, the speed in revolutions a minute it gives its full thrust at, its throttle as fast as the shaft turns it either way |
| `wing` | lift and drag by the air going past it: `area`, `stall` (degrees), `lift` (its most, at the stall); its `face` looks up the structure's up, or the way the block faces for a sail or a rudder |
| `wheel` | a wheel on a spring along the structure's down: `radius`, `travel`, `stiffness`, `damping`, `grip`; `steer` (degrees), `drive` and `brake` (newtons) by the signals at its `left`, `right`, `throttle`, `reverse` and `stop` sides |
| `levitation` | the weight it holds up, in newtons, stopping a fall, never lifting |
| `magnet` | a pull on the magnetic blocks of the world (`#jscore:magnetic`) within its `reach`, falling with the square of the distance; with `power`, as strong as its signal |
| `burner` | heat, in `watts`, for the air of the room above it; with `power`, as strong as its signal |

A side is `any`, `front`, `back`, `left`, `right`, `top` or `bottom`, as the block faces: a block's way is its
`facing`, or its horizontal facing.

What moves it, each tick:

- **Its weight**, at its centre of mass, which follows every block set on it or broken off it. The pull is the laws'
  of the place where they say one, else the dimension's at that height, a band's `gravity` thinning it towards orbit
  ([World](WORLD.md)).
- **The air**, as dense as the dimension's pressure and temperature make it at that height, none in a vacuum, and the
  **wind**: its faces drag as they meet the flow, its wings lift, its engines that breathe air give less up high.
- **Liquids**: each block under one is held up by the liquid it displaces, and so is **the air its hull holds**, each
  block of air with its blocks below it and on its four sides, so a ship floats by the air in it as a real one does.
  Its bobbing at the surface is carried off as waves carry it off, and a boat settles.
- **Its balloons**, its sealed rooms aboard ([Sealed rooms](ROOMS.md)): a room lifts the weight of the air it puts
  aside, less the weight of the gas it holds, so a room of hydrogen or helium lifts, and a burner opening into a room
  warms its air, lighter as it warms, as in a hot air balloon. Up high the air round it is thin, so a balloon climbs
  until it lifts no more; a room aboard has round it the air of where the structure is, not of the yard.
- **Re-entry**: a face it heads into the air with, fast, heats by the root of the air's density and the speed cubed;
  a block there that holds heat takes it in, and one that burns catches fire once the face glows as hot.

What it meets: the world's blocks and the other structures' blocks, each by its real shape, a slab as a slab, met
ahead of time so a fast structure stops where it touches, never through. It rests still on many points, slides as
the blocks' slipperiness lets it, ice barely, and bounces a little from a hard blow. Another structure moving by its
physics is pushed back as hard as it pushes; one moved by a motion carries what rests on it, a crate on a lift. A
blow harder than a block holds, by its blast strength (`jscore:blast_strength`), breaks it, of the world or of the
structure: a heavy block falling on glass goes through it.

A tick is worked in as many steps as keep a structure from going more than half a block in one, and its stiffest
hold, a liquid's surface, steady. A structure still for a second, its engines and burners idle, falls asleep and
costs next to nothing; it wakes the
moment a block of it changes, it is pushed, something moves against it, or what held it goes. Nothing of a chunk not
loaded is read: a structure reaching one waits there. Each structure's tick of physics is timed in the diagnostics as
`jscore:structure_physics`; twenty small structures falling and landing at once take a few milliseconds a tick. The
physics of a dimension's structures takes no more of a tick than `moving_structures.physics_millis` lets it: past
that, a structure waits a tick, and goes the next whatever the budget.

Each force can be turned off for every structure by its setting, gravity, drag, lift, thrust, buoyancy with the
balloons, levitation, magnetism and the wind, so a server can make a world of floating islands or of still air; and
the harm of blows by its own, so structures meet and bounce and nothing breaks.

`structure.physics()` gives its physics while it moves by them: its `parts()` (mass, centre, inertia), its `body()`
(where its centre is, how fast it goes and turns) and the `forces()` on it in the last tick, by their kind. Its
`authority(way)` says how fast its engines and driving wheels can speed it up along a way of the world, at full, in
metres a second a second.

## Joints

*Added 2026-10-21.*

Structures are joined to each other, or to the world, by joints: a rope or a chain, a spring, a bar, a hinge, a
slider, a ball joint and a weld. Each end is a place on a structure or a place of the world, and the joint holds them
as a real one does, worked in the same steps as what the structures touch: a crate hangs and swings at the end of its
rope, a drawbridge turns on its hinge, a trailer follows its tow bar.

```java
StructureJoints.join(level, new JointKind.Rope(6, JointKind.RopeLook.CHAIN, Optional.empty()),
        StructureJoints.Anchor.on(crane, hook, new Vec3(0, 1, 0)),
        StructureJoints.Anchor.on(crate, lid, new Vec3(0, 1, 0)), Optional.of(40_000.0));
```

| Joint | What it does |
| --- | --- |
| `Rope(length, look, winch)` | keeps its ends no further apart than its length, slack when nearer; drawn as a rope or a chain, sagging as far as its slack lets it. A `Winch` reels it in and out by signals, at its `speed`, between its `least` and `most` lengths |
| `Spring(rest, stiffness, damping)` | pulls its ends towards its rest length, `stiffness` newtons a metre, and holds back their going, `damping` newton seconds a metre; drawn as a coil |
| `Bar(length)` | keeps its ends at its length, pushing and pulling: a tow bar |
| `Hinge(limits, drive)` | keeps its ends together, turning only about its axis, within its `limits` in degrees; a `Drive` turns it at a `speed` with a `force`, always or by a signal |
| `Slider(limits, drive)` | lets its ends slide along its axis only, within its `limits` in blocks; driven, a lift or a piston |
| `Ball()` | keeps its ends together, turning any way |
| `Weld()` | holds the two as they stood when joined |

A joint that `breaksAt` a load, in newtons or newton metres, breaks when pulled harder. A signal for a drive or a winch
(`JointSignal`) is read at a block of either end. The joints of a dimension are saved with it and sent to its players,
whose game draws each one between where its ends are drawn that frame, so a rope from a ship never lags the ship.
Structures joined to each other move by their physics together, all in the tick of the first of them to come.

## Held structures

*Added 2026-10-21.*

A structure can be held by another: a turret on a ship, a crane's cab on its tracks, a shuttle docked to a station.
Held, it goes wherever its parent goes, and its own rule moves it on the parent, in the parent's places, so a turret
turning by a `SpinMotion` turns on the ship whichever way the ship goes.

```java
MovingStructures.mount(turret, ship, Optional.of(ringBlock));  // the ring is the ship's block holding it
MovingStructures.unmount(turret, true);                         // let go, it falls at the speed its place had
```

A structure moving by its physics holds what it holds as part of its body: the turret's mass, its engines, its wheels,
its balloons and its blocks meeting the world are all the ship's, so a ship lifts on an engine its turret carries.
Breaking the block that holds a structure lets it go. A held structure goes to another dimension with its parent and
is set down with it. A structure cannot be held by itself, nor by one it holds.

## Parting and joining

*Added 2026-10-21.*

A structure whose blocks are no longer all joined, its middle broken out, becomes as many structures as it now is.
The largest part keeps its name, its rule and its plot; every other part becomes a structure of its own and moves by
its physics, going on at the speed its place had. With the setting `moving_structures.split` off, a broken structure
holds together.

A **decoupler** parts the structure at itself when its signal comes on, as a rocket's stages part: it is gone, the
structure parts there, and the two sides are pushed apart along the way it faced. Two structures square to each other
can be **joined** for good, the visitor's blocks moved into the host where they stand on it:

```java
StructureResult joined = MovingStructures.join(host, visitor);
```

Joining is refused when the two are not square to each other, when their blocks would overlap, or when the structure
joined would be too big to move.

## Docking

*Added 2026-10-21.*

Docking makes two structures one while they are docked: the visitor is held by the host and moves as part of it, its
mass and its engines the host's, and their momentum is shared, so a shuttle docking at speed sets the station going.
A **docking clamp** docks by itself: with its signal on, meeting another structure's clamp face to face within its
`reach`, it takes hold of the other where it is and draws it in on itself, square to it and face on face, over a
second or so, however the two go. As its signal goes off, it lets go, the visitor pushed gently away. Of two clamps
that meet, the structure of more blocks holds the other.

```java
MovingStructures.dock(station, shuttle, Optional.of(port));    // docked where it stands
MovingStructures.undock(shuttle, awayFromThePort);              // let go, pushed away along a way of the world
```

## Tools on the move

*Added 2026-10-21.*

A tool aboard works the world in front of it as the structure passes: a **drill** mines, a **harvester** takes ripe
crops and plants them again, a **plough** tills the ground, and a **placer** sets the blocks it carries where there is
room. What a tool takes goes into a container aboard, and what it places comes out of one; with none aboard, what it
takes drops into the world. A melon, a pumpkin or another block of `#jscore:harvested_whole` is taken whole; sugar
cane, a cactus, bamboo or another of `#jscore:harvested_above` is cut above its foot.

## Seats and helms

*Added 2026-10-21.*

A player using a seat of a structure sits on it, and is carried with it, steady in their seat as it goes; crouching,
they use the block as any other. One sitting at a **helm** steers the structure with the keys they walk with: forward
and back, left and right, up with the jump key and down with the sprint key. The structure's engines and wheels
answer by the way each looks against the helm's forward: an engine pushing forward fires on forward, one pushing up on
up; a wheel rolling forward drives forward and back, and steers left and right. Told nothing for a second, the
controls let go, so a pilot who left the helm leaves the structure to its signals.

```java
MovingStructures.sit(player, seatBlock);                                  // as using the seat does
structure.controls().tell(now, held, forward, up, false);                 // what a computer or a mod tells it
```

Whatever is shot or thrown from a seat aboard, or from a deck, goes on with the deck's speed, so an arrow loosed from a
moving ship flies with the ship.

## The autopilot

*Added 2026-10-21.*

An autopilot flies a structure by its controls, as a pilot at its helm would: it goes to a place and holds there,
holds a height, follows a route, or docks to a structure's clamp. It works each control by how far the structure's
speed falls short of what it wants, over how fast its engines and wheels can speed it up that way, so a heavy rocket,
a light airship and a car are flown alike, never past where they were sent. Its height is held against the
structure's weight. It is kept with the structure, and flies on after a restart; a pilot at the helm takes over while
they hold it.

```java
structure.fly(StructureAutopilot.goTo(new Vec3(120, 90, -40), 8));      // no faster than 8 metres a second
structure.fly(StructureAutopilot.route(List.of(a, b, c), 10, true));     // round again after the last
structure.fly(StructureAutopilot.dock(station.id(), port, 2));
structure.fly(null);                                                     // lets go
```

## Sleeping aboard

*Added 2026-10-21.*

A bed aboard is slept in as one on the ground: it is reached, and kept safe from monsters, as it stands in the world;
its sleeper lies in it there and is carried with it as the structure goes, drawn along the bed however the structure
is turned, and stands up beside it there. A player who slept in a bed aboard comes back to it, where it is, as long as
its structure is loaded; otherwise to the world's spawn.

## Ropes to ride

*Added 2026-10-21.*

A player using a rope joint within reach, riding nothing, takes hold of it there and rides it down, hanging under it,
until its end, where they let go.

```java
MovingStructures.zipline(player, rope.id(), 0.1);   // a tenth of the way from its first end
```

## What it shows going

*Added 2026-10-21.*

A structure moving by its physics shows its players what it does: each engine burning gives off its flame, a
rocket's long column of exhaust or a jet's short blue flame, as big as it pushes, and a propeller nothing; a face
heading into the air fast enough to glow glows on its outermost blocks in the colour of a black body at its heat, and
past about 1,600 kelvin it is wrapped in the plasma of a re-entry; and where the air is colder than about 40 degrees
under freezing, at an aircraft's speed, it leaves white trails behind its engines and its wingtips, as the water of
their exhaust and of the air spun off them freezes. An engine pushing keeps its structure awake, so a rocket too heavy
for its engines stands burning on its pad. What it shows is told to its players only as it changes.

## Seeing its forces

*Added 2026-10-21.*

The **diagram of forces** shows a structure's physics as it flies: the structure itself, turned as it stands, over the
ground below it, an arrow for each force on it where that force acts and one for their sum at its centre of mass, its
height above the ground, and the axes, up and the way its bow looks. Beside it, each force with what it gives now, a
bar of how much it weighs against the others and a switch that hides its arrow; below, the structure's mass, where the
sum pushes it, how fast it climbs, how fast it goes and which way, and how dense the air round it is. The view turns
from the side and from above, turns with the mouse dragged over it and zooms with the wheel, and its arrows sit apart
where they act or summed into one. Its key, **K** until the player changes it, opens it for the structure the player
stands on, sits in at its helm or looks at. While it is open the server tells the player the forces a few times a
second; a structure resting by no rule shows its weight alone.

A block's **tooltip** shows its physics, its mass and each force it gives (an engine's thrust, a wing's area and
stall, a levitator's hold, a magnet's pull, a wheel, a burner and what it displaces), while the player holds the
diagram's key, or holds or wears an item of `#jscore:sees_physics`, the physics tool for one; a mod puts its own
goggles in the tag. Another key, unbound until the player gives it one, shows the physics of the structures near in
the world: each one's box, its centre of mass, and each force as a line from where it acts, coloured as the diagram
colours it.

## The physics tool

*Added 2026-10-21.*

The **physics tool** moves a structure by hand. Aimed at one within 48 blocks, it **grabs** it where it is aimed, while
its button is held, and holds that point before the player as they move and look, as a stiff spring that bears its
weight; it **pushes** it with a blow where it is aimed, along the player's look; or it **freezes** it where it stands,
in the air too, and lets it go again. Its strength, a tenth to the whole, is how hard it holds and pushes: at the
whole, it holds 600 kilonewtons and pushes 150 kilonewton seconds. The wheel changes its mode, and with shift held its
strength, while it is in the main hand; its panel at the screen's left shows both, and while it holds a structure a
beam runs from its muzzle to the point it holds, the structure's box is lit, and beside the point it says the
structure's name, its mass and how far away it is held.

It is made at a crafting table, an iron body round a lodestone, an amethyst at its muzzle and redstone in its grip,
for as long as `moving_structures.physics_tool_recipe` is on: its recipe is a shaped recipe a setting turns on and off,
`jscore:crafting_shaped_switch`, which any mod can use for its own.

```json
{ "type": "jscore:crafting_shaped_switch", "file": "jstech-balance",
  "setting": "moving_structures.physics_tool_recipe",
  "pattern": [" IA", "ILI", "RI "], "key": { "I": { "item": "minecraft:iron_ingot" } }, "result": { "id": "..." } }
```

## Moving it by hand

*Added 2026-10-21.*

What the tool does, a mod does through the same calls:

```java
MovingStructures.push(structure, impulse, at);              // a blow, in newton seconds, at a place of the world
MovingStructures.freeze(structure, true);                   // held where it stands, its physics stopped
MovingStructures.frozen(structure);
MovingStructures.hold(structure, place, target, most, until); // a grip from outside, renewed each tick it holds
MovingStructures.letGo(structure);
Vec3 place = MovingStructures.toYard(structure, worldPlace);  // and toWorld, back
```

A structure at rest or moved by a rule is let go to its physics as it is pushed or held, as anything pushed hard is;
one held by another moves what holds it. A grip pulls its point towards the target as a spring damped to stop it
swinging, bearing the structure's weight, with no more than `most` newtons, and lets go at the game's tick `until`
unless it is renewed.

## What happens to it

*Added 2026-10-21.*

What happens to a structure is told on the game's bus, as a `MovingStructureEvent`:

| Event | When |
| --- | --- |
| `Collision` | a blow of it closing at a metre a second or more: what it struck, the world's block or another structure, where, how fast it closed and the energy of the blow |
| `Parted` | it broke into pieces, each now a structure of its own |
| `Docked` | it docked to a host |
| `Undocked` | it was let go from its host |

```java
NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Collision.class, blow -> {
    if (blow.energy() > 50_000) {
        alarm(blow.structure());
    }
});
```

## Reading its flight

*Added 2026-10-21.*

A sensor aboard reads its structure's flight, as it speaks it in redstone, to programs and to the network: its
altitude, the air's pressure, its speed and its climb, its pitch, its roll and its heading by the way the sensor looks,
how far below it the ground is and how far ahead the first block, each up to 64 blocks. A sensor on the ground reads
the same, still. So an autopilot can be built of redstone.

```java
public class FlightSensorBlockEntity extends SensorBlockEntity {
    @Override
    public List<Reading> readings() {
        return StructureReadings.of(level, worldPosition, getBlockState().getValue(FACING));
    }
}
```

Its block gives the redstone of the reading it speaks, and offers every reading through `Signals.OUTPUT`, which J's
Computers reads through its peripheral links ([Redstone and signals](SIGNALS.md)).

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

What a block aboard asks of where it is, it is told of where its structure is in the world, not of its yard: the air
round it, its pressure and how warm it is (`HeatClimate.air`), the warmth of the biome under it
(`BiomeClimates.celsius`), the weather falling there and the wind (`Weathers.at`, `Weathers.windAt`), so a machine
aboard a high airship runs in thin cold air and one aboard a ship in a storm is rained on.

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
| Mechanical power | the networks the parts turned in where they stood are worked out again, and turn aboard: a shaft turns a propeller (`"shaft"` in its thrust) that pushes the structure |
| Lamps | the light a spotlight's beam laid where it stood goes out; aboard, its beam shines on the world from where it is, the way its structure turns it, cast again every other tick while the structure goes, so a searchlight sweeps the ground |
| Multipart | a space of parts crosses whole, its microblocks and its cables with it |
| Conveyors | the belts aboard carry along as on the ground, into what is aboard, the structure going or not |
| Radios | a radio aboard is heard from where the structure carries it, so a transmitter flown away is heard less as the frequencies are worked out again |
| Peripheral links | a peripheral stays linked to an owner that moved with it; an owner keeps its ports, hubs and disabled flags where the peripherals are now |
| Radio relays | a relay paired with one that moved with it stays paired, in another dimension too |
| Atmosphere Detector | the room it asked for where it stood is no longer asked for |
| Overhead lines | a wire hung between two insulators that moved together goes with them; one hung to an insulator that stayed falls, its wire left there |
| Radiation | a source goes on giving off from where its block is now |
| Data networks | the cables an owner of a network touched where it stood are forgotten |

## Living alongside other moving-structure mods

*Added 2026-10-21.*

Create moves blocks as contraptions, entities that carry the blocks they took. Sable, which Create Aeronautics flies,
keeps each ship's blocks as a sub-level: a plot of chunks far out in the level that it places in the world by a pose.
With either mod installed, the kit's structures and theirs share the world this way:

| | Create's contraptions | Sable's sub-levels |
| --- | --- | --- |
| A structure of the kit's meets one | as an obstacle that moves, by its blocks, where its entity places them and as fast as it goes | as an obstacle that moves, by its blocks, where its pose places them and as fast as Sable's physics says it goes |
| One meets a structure of the kit's | no: Create's contraptions meet no other mod's moving blocks | no: Sable's physics meets only the world and its own |
| Blocks a structure of the kit's keeps | Create moves none of them, so a bearing, a piston or an assembler built on Create's contraptions never takes a block out of one | Sable's assembly is built on Create's contraptions, so it takes none of them either |
| Their blocks | the kit takes none of a contraption's blocks, which are in no place of the world | the kit makes no structure of a block of Sable's plots, and says so (`jscore.moving.refused.foreign`) |
| A place of theirs | | `MovingStructures.inWorld` gives where Sable has a place of a plot in the world, as it does for the yard |

A structure resting on one of theirs stays awake while that one moves, since nothing of the other mod tells it when it
comes; one resting on a contraption or a sub-level standing still sleeps as on the ground. The kit's structures never
push a contraption aside as they push what stands in their way: its mod places it every tick.

The meeting goes one way because the other mods have no way in for another mod's moving blocks: a contraption and a
sub-level each meet only the world and their own kind. So a ship of Sable's flies through a structure of the kit's,
while a structure of the kit's lands on a ship of Sable's and rides it.

The Core reaches each mod only through what the mod publishes for others, Create's API and Sable's companion library,
and only on the versions it is tested against: Create 6.0.11 up to 6.1, Sable 2.0.6 up to 2.1. On another version
the log says so, and that work is left off; on a version of Sable the Core is not tested against, it still refuses to
take blocks from where Sable keeps them by default.

A mod with moving structures of its own tells the kit of them with `ForeignMovers.declare(kind)`, behind its own
guard: an `IForeignKind` gives the ones whose blocks may be in a box of the world (`near`, each a `ForeignMover`: its
frame, the box of its blocks, what reads them, how fast it goes and turns), the region it keeps its blocks in
(`holds`), where a place of that region is in the world (`inWorld`), and whether an entity is one of its structures
(`isMover`).

The tests of this layer run with the mods: `-PwithCreate`, `-PwithSable` (Create beside it), and `-PwithAeronautics`
(both, and Create Aeronautics) on `runGameTestServer`, `runClient` or `runServer`. None of the three is ever shipped
with the series: they are development dependencies, there only to test against.

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
| `physics <structure>` | lets it go, to move by its physics |
| `stop <structure>` | brings it to rest |
| `setdown <structure>` | sets it down on the grid |
| `list` | names the structures loaded in the dimension |

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| `moving_structures.most_blocks` | 65,536 | The most blocks one structure may be made of. |
| `moving_structures.physics_millis` | 10 | How much of a tick, in milliseconds, the physics of a dimension's structures may take; past it a structure waits a tick, and goes the next whatever the budget, so many structures slow down together rather than slow the server. |
| `moving_structures.split` | on | Whether a structure whose blocks are no longer all joined becomes as many structures as it now is, each going its own way; off, it holds together. |
| `moving_structures.gravity` | on | Whether gravity pulls the structures moving by their physics; off, they float where they are let go. |
| `moving_structures.drag` | on | Whether their faces drag as they meet the air and liquids. |
| `moving_structures.lift` | on | Whether their wings, sails and rudders lift and turn them. |
| `moving_structures.thrust` | on | Whether their engines and propellers push. |
| `moving_structures.buoyancy` | on | Whether liquids, the air a hull holds and the balloons aboard hold them up. |
| `moving_structures.levitation` | on | Whether their levitators hold them up. |
| `moving_structures.magnetism` | on | Whether their magnets pull them to the magnetic blocks of the world. |
| `moving_structures.wind` | on | Whether the wind blows on them; off, the air they meet is still. |
| `moving_structures.impact_damage` | on | Whether a blow harder than a block holds breaks it; off, they meet and bounce, and nothing breaks. |
| `moving_structures.physics_tool_recipe` | on | Whether the physics tool is made at a crafting table; off, only a command or the creative menu gives it. |

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
| A structure let go hangs in the air | a chunk it reaches is not loaded, so it waits; or a levitator, a balloon or a magnet holds it. |
| A balloon does not lift | its envelope weighs too much for the air it puts aside: blocks are heavy, so give the envelope's blocks a small `mass` in `jscore:block_physics`. |
| An iron ship sinks | its walls are blocks of solid iron: give its hull room, the air it holds is what floats it. |
| A light raft or cart drifts | the wind blows it, as it would a real one: a brake holds a cart. |
| A hovering craft tips over | it is held up from below its centre of mass, as a broom balanced on a hand is: put its levitators, balloons or lifting engines round its middle, or above it. |
| A mod's stand-in player cannot sit, ride a rope or steer | NeoForge's `FakePlayer` rides nothing; tell the structure its controls with `structure.controls().tell(...)` instead. |
| A shuttle under an autopilot goes no further sideways than it is blown | it has engines only forward and back: the autopilot works only the controls its engines answer. |
| The diagram of forces says it is reading the forces and shows no arrows | the structure is more than 192 blocks away, or not loaded on the server; come nearer. |
| The wheel does not change the hotbar slot | the physics tool is in the main hand: the wheel changes its mode; the number keys change the slot. |
| A structure held by the physics tool sags | it weighs more than the tool's strength holds: raise the strength with shift and the wheel. |
| A propeller on a shaft gives nothing | the shaft does not turn, or turns slowly: its thrust grows with the shaft's speed up to the speed its `shaft` says. |
| A ship of Sable's flies through a structure | Sable's physics meets only the world and its own sub-levels; a structure of the kit's meets the ship, not the ship it. |
| A bearing does not turn a structure's blocks | Create moves no block a structure of the kit's keeps; put the bearing in the world, not aboard. |
| A structure cannot be made of a ship's blocks | they are a sub-level of Sable's; a moving structure is never made of another's blocks. |
| A contraption or a ship goes by and nothing is met | Create or Sable is of a version the Core is not tested against: the log says so when the game starts. |
