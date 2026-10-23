# Entities and vehicles

Things in the world that move on their own: vehicles of one piece, robots, creatures, shots, and what players wear.
Structures of real blocks that move are a kit of their own ([Moving structures](moving-structures.md)); a vehicle here
is the light, fast way, for cars, bikes and drones.

## What exists today

Everything this page designs is built, as of October 2026: vehicles as data with their parts, fuels, seats, keys,
lights, paints, trailers and cargo, robots with paths in three dimensions and tasks with logic, creatures as data,
real ballistics, and the kit of modules in worn things. How a mod uses it, with code: [Entities](../ENTITIES.md). What
is not built yet is under *Still to build* at the end.

## The design

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
- **A rolling aircraft seen rolling**: whoever sits in it is turned with the vehicle's lean for every player who sees
  them, each seat where it stands on the leaning body, not only in the pilot's own view.
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

## Still to build

- **Two screens come with the mods that use them**: a vehicle's panel and instruments with J's Transport, and the
  screen of a worn thing's modules with the mod of the suits.
