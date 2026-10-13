# Entities and vehicles

Things in the world that move on their own: vehicles of one piece, robots, creatures, shots, and what players wear.
Structures of real blocks that move are a kit of their own ([Moving structures](moving-structures.md)); a vehicle here
is the light, fast way, for cars, bikes and drones.

## What exists today

([Entities](../ENTITIES.md).)

- **Entities are declared like blocks**: a name, a size, how far and how often players are sent them, fire immunity,
  attributes, energy offered to chargers and cables.
- **Vehicles** players sit in and drive with the walking keys (a flying one climbs and sinks), falling by the gravity of
  their dimension, running on a battery.
- **Robots** that work through a list of tasks as their owner would: they **never fight**, and never break a block their
  owner could not. The Core's tasks move, break blocks, collect items, wait and drive; a mod adds kinds of its own.
- **Projectiles**, crediting whoever fired them with what they hit.
- **What is worn**: the armour slots and the slots of Curios or Accessories when installed, and any mod's own.

## To build

The kits gather what Immersive Vehicles, Small Ships and the robots of many mods do, in one place, for any mod.

### Vehicles

- **Vehicles as data**: a model, places for parts, seats and the physical numbers, declared in a pack, so a pack adds
  vehicles with no code, as Immersive Vehicles' content packs do.
- **Parts**: engines, wheels, seats, tanks, lights, horns, instruments, fitted in the vehicle's places, each with its
  effect as data (power, fuel, grip).
- **Any fuel**: an engine says what it burns (a liquid or a gas by tag, or energy for an electric one); **fuel pumps**
  join the pipes.
- **A panel and instruments**: speed, fuel, engine speed, temperature, altitude; switches for ignition, lights and the
  starter; a **vehicle key**, an item tied to the ownership kit ([The platform](platform.md#teams-and-owners)).
- **Real lights**: headlights that light the way, indicators, a light inside.
- **Engine sound** by engine speed and load, a horn, doors.
- **Physics**: suspension, grip by the ground, a gearbox, collisions that push, damage by part and its repair.
- **Trailers and hitches** between vehicles.
- **Several seats with roles**: driver, passengers, and the roles a mod adds, moving between them.
- **A boot and cargo tanks.**
- **Paints and variants** as data.
- **Vehicles inside moving structures**: a car driven into a starship's hangar travels with it.
- **Smooth movement over the network**, with prediction for whoever drives.

### Robots

Robots still never fight: combat is always a player's.

- **Paths in three dimensions**: robots that fly and robots that swim.
- **Tasks with logic**: repeating, conditions, schedules, and **sensing** the world (looking for items, blocks,
  entities).
- **Any tool** of any mod, an inventory by the robot's size, and **charging stations**.
- **Commanded from outside** through the API, so J's Computers can program them in Σ#.

### Creatures

**Mobs with behaviours declared as data**, and rules of where they appear by dimension, biome and **air**: a creature
only appears, and only lives, where the air suits it ([Sealed rooms](sealed-rooms.md)), which is what J's Space asks
for the animals of other worlds.

### Projectiles

**Real ballistics**: the gravity of the dimension, drag by the density of the air, wind, and getting through blocks by
their hardness.

### What is worn

**A kit of modules in worn items**: an item with module places, each module with its effect and the energy it spends,
in a screen of its own. J's Industrial's exo-suits and J's Space's suits use the same kit.
