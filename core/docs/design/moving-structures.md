# Moving structures

A kit for structures of real blocks that move through the world: J's Space's rockets and starships first, J's
Transport's vehicles next, and anything any mod wants to move, from a drawbridge to an airship. It works in the spirit
of Create Aeronautics, Valkyrien Skies and Create's contraptions, as an open-source kit any mod can build on.

## What exists today

Nothing of the kit. The Core has entities with a driver's input and a vehicle's specification
([Entities](../ENTITIES.md)), which are single entities, not structures of blocks.

## To build

### Real blocks that really move

- A structure's blocks are **real blocks**, kept in a **region of the world reserved for them**, where they run as they
  would on the ground: **machines run, computers boot, networks work and crops grow** while the structure moves.
- They are **drawn and collided where the structure is**, so it looks and behaves like the blocks it is made of.
- **Sections that are multiblocks are the easy, fast case**: their model, their shape and how they behave are known.
  **Free bays**, where any block goes, use the same mechanism block by block.
- **Any mod's blocks work aboard** with no work from that mod; a small compatibility hook helps the few that need it.

### Two ways to move

- **Kinematic**: moved by rule, exactly and cheaply, as Create's contraptions are: lifts, drawbridges, trains on rails,
  an observatory's turning dome, an antenna following a satellite.
- **Dynamic**: real physics, as a rigid body: starships, rockets, boats, aircraft, cars.

### Physics

- **Mass from the blocks, by the density of their material**: a block of steel weighs about 7.8 t, one of wood about
  0.6 t. From it come the **centre of mass** and the **inertia**, which change live as blocks are added or taken away.
- **Rotation on every axis**, roll, pitch and yaw: a structure can turn upside down.
- **Forces from the blocks**, each declared as data, so any mod and data pack take part:
  - **thrust** from engines, along where they point;
  - **lift** from wings, by their area, the speed through the air and the angle of attack;
  - **drag**;
  - **buoyancy** in any liquid, by the volume displaced: boats float, submarines dive with ballast tanks;
  - **levitation**: blocks that resist falling without pushing up, for mods of fantasy and fiction;
  - **magnetism**;
  - **gravity**, by the dimension and the altitude.
- **The air counts**: lift, propellers and balloons depend on the **pressure and density of the air**, by the dimension
  and the altitude ([The world](world.md)), so aircraft have a natural ceiling and in a vacuum only a rocket flies.
- **Relative airflow**: forces follow the speed through the air; a propeller loses thrust as the craft nears the speed
  of its own airflow.
- **Wind**: the weather kit gives wind by dimension and altitude, and balloons and sails drift with it; a sail catches
  it, so sailing ships sail.
- **Balloons are sealed volumes**: an envelope's enclosed volume is found by the sealed-room kit
  ([Sealed rooms](sealed-rooms.md)), fills **from the top down** with hot air or a light gas (helium, hydrogen), lifts
  by its volume and the density of the air around it, and leaks through a hole.

### Collisions and contact

- **With the ground, with other structures** (ship against ship) and **with entities**, which are pushed aside.
- **Damage comes from the energy of the impact**, against the hardness of the blocks struck; blocks are destroyed for
  real, and a section's blocks can be lost one by one, as a mod's rules for durability say.
- **Friction and landing gear**: wheels roll, skids slide, feet stand.
- **Wheels and suspension**: any wheel-shaped block can be a wheel, on a suspension with its stiffness and damping;
  steering follows the strength of a signal; brakes; grip by the ground (ice slips, sand drags).
- A hole in a sealed room aboard **decompresses** it ([Sealed rooms](sealed-rooms.md)).

### Structures joined to structures

- **Joints**: hinges, bearings, sliders, springs, **ropes and chains** between structures and the world, towing, and
  **docking clamps** that make two structures one while they are docked.
- **Structures inside structures**: a structure held to another by a bearing or a spring moves with it and turns on its
  own: a turret on a ship, a helicopter's rotor, a hangar door.
- **Winches and ziplines**: a winch reels a rope in and out, and a rope makes a zipline down from a ship.
- **Splitting and joining**: a structure that loses its middle becomes two; a rocket's stages part and each piece goes
  its own way, falling, landing or left in orbit; two docked structures can join for good.

### Tools that work on the move

Blocks that **act on the world as the structure passes**, as Create's contraptions do: drills that mine, harvesters that
harvest, ploughs, placers. A combine harvester drives and harvests at once.

### Living aboard

- **Walking, jumping, sitting and sleeping aboard**; items on the floor stay where they lie.
- **Using any block while it moves**: chests, machines, crafting, placing and breaking blocks; redstone, networks,
  cables and fluids all work.
- **Walking a moving deck without a tremor**: the player's game predicts the movement, so a deck feels as steady as
  the ground. This is the hardest part, and the one that matters most.
- **Shots and thrown items carry the structure's speed.**
- **Seats and controls**: a helm or a cockpit maps the player's controls to what moves the structure; an **autopilot
  API** goes to a point, holds an altitude, follows a route and docks.

### Building

- **Assembling** from blocks placed in the world, marked by a block or an area, checked to be all joined, or a structure
  **born as one**, as a rocket in its hangar.
- **Building on a structure already made**: blocks are placed onto it as onto the ground, so a structure grows with no
  size limit at assembly.
- **Taking apart** back into the world on landing, aligned to the grid, with block entities whole.

### Drawing

- **Smooth movement**, interpolated; the structure takes the world's light where it is, and has its own light inside.
- **Sections are drawn as cached meshes**, with distance culling, and the kit works with shaders where they allow (Iris,
  Oculus).
- **Effects**: engine exhaust, contrails, the glow of a re-entry.

### Keeping and sending

- **A structure survives a restart in mid-flight**, and **changes dimension with everyone aboard** (a rocket reaching
  orbit).
- **Only what changes is sent**: blocks changed aboard, and position and speed at a rate set by how far the watcher is.

### Scale and cost

- **Many structures, of thousands of blocks each.** Physics runs away from the main thread where it can, and a structure
  at rest **sleeps** and costs nothing.
- **Far from players**: with the Space Persistor it keeps working ([The world](world.md#keeping-chunks-loaded));
  without it, it freezes and is saved.

### Seeing and measuring

- **A diagram of forces for the player**, not only for debugging: gravity, drag, lift, thrust, buoyancy, levitation and
  magnetism, as separate or summed arrows, with the centre of mass and the total mass, in a view that turns and zooms.
- **Physical properties in the tooltip** of each block (mass, lift), with goggles or a key.
- **Readings for sensors**: altitude, air pressure, speed, tilt on each axis, heading, distance to the ground or to a
  target, given to redstone, to programs and to the network, so an autopilot can be built of redstone.
- **A debugging overlay** for developers: the centre of mass, the forces and the collision boxes.

### Tools and code

- **A creative physics tool** to grab, push and freeze structures.
- **The API**: making, moving, pushing and asking structures; turning positions between the world and a structure;
  events for collisions, splits and docking; the **physical properties of every block as data** (mass, thrust, lift,
  buoyancy, levitation).
