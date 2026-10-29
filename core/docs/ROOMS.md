# Sealed rooms

Rooms that hold an atmosphere of their own: a base on Mars filled with air so the player takes off the suit, an
underwater base kept dry, the cabin of a starship. J's Core finds the rooms and keeps their air by real physics; the
mods bring the blocks that seal them and fill them. It is in the spirit of the oxygen of Galacticraft and Ad Astra and
the atmospheres of Advanced Rocketry.

The design behind this page is [Sealed rooms](design/sealed-rooms.md).

## The words

- A **room** is a closed space of air: blocks that let air through, every way out of them a wall.
- A **wall** is a block that bounds a room: stone, glass, a door. A wall may **leak**, letting part of a face's worth
  of gas through, and stands a difference in pressure before it **bursts**.
- A **door** keeps the rooms on its two sides apart: shut it leaks as little as it says, open it lets everything
  through.
- A room is **breached** when its walls are opened to the place round it: it leaks through its holes until it is
  closed again.
- **Partial pressure** is a gas's share of the pressure: the overworld's air at sea level has 21 kPa of oxygen in its
  101.3.

## Finding rooms

*Added 2026-10-17.*

A room is found where a block asks for one, a vent, a detector, an oxygen generator:

```java
SealedRooms.track(level, pos);      // when the block is placed or loaded: its room is looked for now
SealedRooms.untrack(level, pos);    // when it is taken away; a room found stays
```

For a block that is itself a wall, the room on every side of it is looked for. From the place asked, the Core goes
through every block that lets air through, up to the largest room the server allows (16,384 blocks by default): a
space closed within that is a room; a larger one is the open air. It reads a few thousand blocks a tick, away from the
rest of the tick's work, so a hangar is found in a few ticks without a stall. It reads only chunks already loaded
whole: a room reaching into one that is not waits, and is looked for again when that chunk loads, so finding a room
never loads or makes a chunk in the middle of a tick.

Once found, a room is worked out again only when a block near it changes, by any means: placed, broken, blown up,
pushed, set by a command or by another mod's machine, or changed without telling its neighbours. A change inside it
that changes no wall (a crop growing, water flowing) only counts what it holds. A wall taken away that joins two rooms
makes them one, their gas together; a block that splits a room makes two, each with its share; a closed space no room
held that joins one brings the place's air with it. A room whose walls are opened to the open air keeps its shape and
leaks through its holes; once its air has been the place's for ten seconds it is forgotten, and a block asking for it
finds it again when it is closed, holding the place's air, as it would have.

Rooms are kept with the world, their gas and all. A mod that puts a whole area back as it was, or takes it away to
build it elsewhere, forgets the rooms there with it, and the places asking for them:

```java
int forgotten = SealedRooms.forget(level, area);   // an AABB; asked for again, a room holds the place's air
```

## Real gases

*Added 2026-10-17.*

A room holds any gas, each by the id of its material in moles: oxygen, nitrogen, argon, carbon dioxide, or any a mod
registers. Its pressure follows from the amount and the volume as an ideal gas's, at the 20 °C a sealed room is kept
at: a block of the overworld's air holds about 41.6 moles. Breathing goes by the partial pressure of oxygen: below
16 kPa of it comes hypoxia, whatever the total.

Gas flows through every leak in proportion to the difference in pressure and the size of the opening: through a hole
of a block a room of eight blocks loses most of its air in five seconds, through a crack in minutes. Through an open
door two rooms' pressures even out at once and their airs mix over a minute or so. Out of a room, the gas goes towards
the air of the place beyond each leak: its dimension's, at that height, with its bands ([The world](WORLD.md)).

`GasMath` holds the arithmetic, pure and tested.

## How blocks seal

*Added 2026-10-17.*

What each block does is data, a data map of blocks a mod or a data pack fills,
`data/<namespace>/data_maps/block/seals.json`:

```json
{ "values": {
    "mymod:pressure_window": { "leak": 0, "rating": 2000 },
    "mymod:airlock_door": { "door": true, "leak": 0, "rating": 2000 },
    "#c:glass_blocks": { "leak": 0.0005, "rating": 120 } } }
```

| Field | Means | The default |
| --- | --- | --- |
| `leak` | The share of a whole face its cracks let through, 0 to 1. | 0 |
| `rating` | The difference in pressure it stands before it bursts, in kilopascals. | 400 for each point of how hard the block is to blow up: glass 120, dirt 200, stone 2,400 |
| `door` | Whether it is a door: shut while its block says so, open otherwise. | no |

A block the map says nothing of is a wall when it is a whole solid block, which does not leak; anything else, air,
water, a slab, a fence, lets air through and is part of the room it stands in. The game's own blocks:

| Blocks | Leak | Rating |
| --- | --- | --- |
| Glass | 0.0005 | 120 kPa |
| Glass panes | 0.001 | 60 kPa |
| Leaves | 0.5 | 5 kPa |
| Copper grates | 0.25 | 50 kPa |
| Doors and trapdoors, shut | 0.002 | 60 kPa |
| Fence gates, shut | 0.05 | 30 kPa |
| The iron door and trapdoor, shut | 0.001 | 400 kPa |

A leak of less than a hundredth of a face, glass or a shut door, is not shown as a leak.

A dimension's rules may say some blocks seal otherwise there, over what the map says ([The world](WORLD.md)): the
game's glass stands the overworld's water, but a world made for bases on the Moon may say it does not stand a room's
air held against a vacuum. The first entry that covers a block is how it seals in that dimension. *Added 2026-10-29.*

```json
{ "pressure": 0, "seals": [
    { "blocks": ["#c:glass_blocks", "#c:glass_panes"], "leak": 0.02, "rating": 10 },
    { "blocks": ["#minecraft:doors", "#minecraft:trapdoors"], "door": true, "leak": 0.02, "rating": 10 } ] }
```

## What the air does

*Added 2026-10-17.*

A player inside a room breathes its air and is kept warm; outside, the place's air and warmth, by their dimension's
rules. Each is weighed once a second, and each harm posts a `DimensionHazardEvent` a suit may cancel:

| The air | What it does |
| --- | --- |
| Oxygen under 16 kPa | Suffocates (`AIR`). |
| Carbon dioxide over 1% | A headache and nausea (`CARBON_DIOXIDE`). |
| Carbon dioxide over 5% | Dangerous: it suffocates too (`AIR`). |
| Oxygen over 50 kPa | Poisons, slowly: a point of harm every ten seconds, its damage the Core's `jscore:oxygen_toxicity` (`OXYGEN`). |

Fire needs oxygen: a fire goes out where there are less than 12 kPa of it, in a room or out in a dimension without
it, and does not light there. In a room whose air is mostly oxygen any flame or lava sets alight at once everything
round it that burns, an inferno, as in Apollo 1; pure oxygen at a third of an atmosphere is cheap to fill a room
with, and that is its price. A mod's machine that burns fuel asks `SealedRooms.oxygenAt(level, pos)` whether it can.

Other flames need oxygen as fire does. The game's furnaces, smokers and blast furnaces light their fuel only where a
fire could burn in the air round them, and go out, their fuel done, the second the room round them runs that short of
oxygen, while fire needs oxygen in the settings. A mod declares a flame of its own, a lamp or a burner, and the Core
puts it out wherever it is lit or set with too little oxygen, and the second the room it burns in runs short:
*Added 2026-10-29.*

```java
Flames.declare(new IFlame() {
    public boolean burning(BlockState state) { return state.is(MY_LAMP.get()) && state.getValue(LIT); }
    public void putOut(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state.setValue(LIT, false));
    }
});
boolean can = Flames.canBurn(level, pos, state);   // for a block in a wall, the air on any open side of it
```

Water stays liquid only in air thicker than its triple point, 0.6117 kPa. The game's ice, melting beside a torch or
broken, becomes water only where the air it meets is that thick: in a vacuum, or in air as thin as Mars', it turns
straight to vapour and is gone, as the game has it do in the Nether's heat. A sealed room full of air melts it as the
Earth does. A mod's ice asks the same of `Thawing`: whether it is warmed, by air above freezing on one of its faces or
by a bright light, and whether it melts into water there. *Added 2026-10-29.*

When a room's air rushes out through a hole, it pulls items, mobs and players towards the hole while the difference
lasts.

A block of a room's walls facing a greater difference in pressure than it stands bursts, as a weak window does under
water: the room then leaks through the hole it left.

A room aboard a moving structure has round it the air of where the structure is, not of the yard its blocks are kept
in, taken again each second: an airship's cabin climbs into thin air, and a sealed balloon swells against its walls
until they burst; a hole in it pulls whoever is aboard in it towards the hole. *Added 2026-10-20.* Its rooms are
also its balloons ([Moving structures](MOVING_STRUCTURES.md)).

## Under water

*Added 2026-10-17.*

A room formed under water is pumped dry: the water in it is taken out and it holds the place's air like any other
room. A hole under water floods it: water rushes in, filling it from the floor up, as fast as the depth pushes it
(the pressure of the water beyond, a kilopascal for each 10 cm of it, over the room's), until the air it squeezes into
less room presses back. Closed again, it is pumped dry again. Water a player pours into a room that stands on land
stays.

## Living things

*Added 2026-10-17.*

Mobs breathe by their kind: a cow suffocates in a vacuum, a zombie does not. The kinds that do not breathe are an
entity tag, `jscore:breathes_nothing` (the undead, golems, armour stands, vexes, blazes, breezes, shulkers and the
dragon); a mod or a data pack adds its own.

Plants need air: crops and saplings do not grow where the pressure is under 5 kPa. In a room, leaves, crops and
saplings (the block tag `jscore:makes_oxygen`) turn its carbon dioxide into oxygen, so a greenhouse helps a room
breathe; whoever breathes in a room takes its oxygen and gives it carbon dioxide, and so does a fire. The rates are
the game's, faster than life's, so a room goes stale in an afternoon rather than a month: a player breathes 0.01
moles of oxygen a second, a mob half that, a plant turns 0.002 back, a fire burns 0.2.

## Seeing a room

*Added 2026-10-17.*

A player holding or wearing something of the item tag `jscore:sees_rooms` (a mod's goggles), or whose Atmosphere
Detector shows its room, sees the room they are in or look at: its outline in moving dashes, seen through its walls,
the blocks it leaks through marked in red, and beside the crosshair its size, its pressure and whether it is falling,
its oxygen and whether it is enough, and its leaks by name.

On their side, `RoomViews.seen(gameTime)` is the room shown, and `RoomViews.airHere()` the air the player breathes now,
sent every second: what a mod's game needs to know whether sound carries.

## The Atmosphere Detector

*Added 2026-10-17.*

A block of the Core's shared content: it reads the air round it, a sealed room's beside it or the place's in front of
it, once a second. Its screen has no inventory: a pressure gauge from 0 to 120 kPa with the band one can breathe in
and how fast the pressure changes; each gas with its share and its own pressure, what the air does, and a bar of the
shares; the room as the detector draws it, its walls, its air and its leaks blinking, with its size and its leaks; the
lamps BREATHABLE, HYPOXIA, LEAKING and VACUUM; and a key that shows the room in the world to whoever presses it.

## Vents, gas and the API

*Added 2026-10-17.*

```java
Optional<Room> room = SealedRooms.roomAt(level, pos);          // or roomBeside, for a block in a wall
Climate air = SealedRooms.climateAt(level, pos);               // the room's air, or the place's
double given = SealedRooms.give(level, pos, AirMix.OXYGEN, 2);  // moles into the room beside pos
double taken = SealedRooms.take(level, pos, AirMix.CARBON_DIOXIDE, 1);
```

A vent joins a room to the gas lines: each tick its block entity fills the room beside it from its tank, or empties
the room into it, gas in a tank counted in millibuckets at the open air's pressure, 22.4 to a mole:

```java
RoomVent.fill(level, pos, tank, 100);     // up to 100 mB a tick of the tank's gas into the room
RoomVent.empty(level, pos, tank, 100);    // up to 100 mB a tick of the room's air into the tank
```

What happens to rooms is posted on the game's event bus: `RoomEvent.Formed`, `Changed`, `Filled` (with air that can
be breathed, from air that could not), `Leaking` and `Removed`.

## Settings

*Added 2026-10-17.*

In the Core's settings, the section `rooms` ([Settings](SETTINGS.md)):

| Setting | Means | Default |
| --- | --- | --- |
| `decompression_pull` | The air rushing out through a hole pulls things towards it. | on |
| `fire_in_pure_oxygen` | Any flame in air mostly of oxygen sets everything round it alight at once. | on |
| `fire_needs_oxygen` | Fire goes out, and will not light, with too little oxygen. | on |
| `mobs_breathe` | Mobs that breathe suffocate in air they cannot breathe. | on |
| `plants_need_air` | Crops and saplings grow only where there is air. | on |
| `blocks_burst` | A block facing more pressure than it stands bursts. | on |
| `largest_room` | The largest room, in blocks: a larger closed space is the open air. | 16,384 |

## What can go wrong

| What you see | Why |
| --- | --- |
| A closed building is no room. | No block asked for its room (`SealedRooms.track`), it is larger than `largest_room`, or one of its walls lets air through: a slab, a fence, an open trapdoor. |
| A room's air never fills. | It leaks: look at it through goggles or its detector, its leaks marked. |
| Glass burst. | The room's pressure and the place's differ by more than glass stands, 120 kPa: under deep water, or a room filled too full. Use a window a mod rates higher. |
| Torches stay lit in a vacuum. | Torches and candles going out are J's Space's rule, a flame it declares, not the Core's; fire blocks and furnaces go out by the Core's. |
