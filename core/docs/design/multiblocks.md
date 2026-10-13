# Multiblocks

Machines built from many blocks placed in a shape: a coke oven, a blast furnace, a reactor, a rocket part. The Core
says what the shape is, checks the world against it, turns it into one machine, and helps the player build it.

## What exists today

([Multiblocks](../MULTIBLOCKS.md).)

- **A pattern** is the shape drawn in letters, one layer at a time from the bottom; one letter is the **controller**,
  the block that holds the machine's state and runs it, and the others say which blocks or tags fit there.
- **Patterns are data**: the data generation writes each to a file, and a data pack can change the shape, the blocks
  or the ports with no code. A pattern can also be read from a structure built in the game.
- **Four rotations**: a shape is found facing any of the four horizontal ways, so a player can build it turned.
- A failed check says **which place is wrong**, what was wanted and what was found, which is what a player needs to be
  told.
- **Ports at fixed places** of the pattern (items, fluids, energy, in or out) pass pipes and cables through to the
  controller.

## To build

The kit gathers what the multiblocks of GregTech, Mekanism, NuclearCraft, Extreme Reactors, Tinkers' Construct and
Modular Machinery do, in one place, for any mod.

### Shapes

- **Places with rules, not only fixed blocks**: "any block of this tag"; "any coil", where the coil chosen sets a
  number of the machine (its highest temperature, as the coils of GregTech's blast furnace do); counts ("from one to
  four energy ports").
- **Shapes by rule, with no pattern drawn**: any box between a smallest and a largest size, with a frame on the edges
  and ports or glass on the faces, as Mekanism's dynamic tank; a floor and walls of any outline, as Tinkers'
  smeltery; any filled cuboid of units, as AE2's crafting CPU.
- **Sizes can vary**: a multiblock may declare a smallest and a largest size, and what its size gives (more ovens in a
  battery, more capacity in storage, more at once in a furnace).
- **The inside counts**: when it forms, a multiblock reads what is inside it and works out its numbers, with **rules of
  neighbours** (a cell beside a moderator gives more; a part only works next to another), as NuclearCraft's and
  Extreme Reactors' reactors do: for reactors, boilers, heat exchangers and batteries.
- **Turning, mirroring and lying down**: every shape is found in the four horizontal turns; a multiblock may also
  declare that it can be **mirrored** (for shapes that are not symmetric) and that it can **lie on its side**.

### Ports and work

- **Ports are blocks** the player places **anywhere on the shell**, as the valves and hatches of a real plant: the
  connection goes where the line arrives. There is a port for each resource (items, liquids, gases, plasmas,
  supercritical fluids, slurries, exotic matter, heat), the energy port with its voltage class, the compressed air port
  and an optional **redstone port** that reads and gives signals.
- **Ports grouped by colour**: ports of one colour are one group, and a recipe takes its inputs from one group at a
  time, so two processes never mix.
- **Recipes in parallel**: a multiblock runs several recipes at once, by its size or by the parts it has, and shows the
  exact number.
- **The controller is the machine's screen**, with the same tabs as every machine ([Machines](machines.md)).

### Formed

- **Once formed, a multiblock becomes one detailed, animated model**, drawn by the Core's own models
  ([Low level](low-level.md)): the turbine spins, the flywheel turns, the tower lets out steam. The ports stay visible
  where they were placed. Forming has its effect and its sound: the blocks merge into the model.
- **Breaking one block undoes the structure**; the controller keeps what it held. Only the owner and the owner's team
  can break the blocks of a formed multiblock ([The platform](platform.md#teams-and-owners)).
- **Formed or not** shows in Jade and in the tooltip, with the dimensions when Shift is held.
- **Checking is incremental**: a structure is checked again only when one of its blocks, or a block beside it, changes,
  never every tick, and safely across chunk borders.

### Building it

- **Help to build**: a manual shows the structure layer by layer, in 3D ([Manuals](manuals.md)); and the **Structure
  Projector**, a block of the Core's shared content, shows the missing blocks in the world as ghosts, with a count of
  what is missing, and **marks in red, in the world, a block that is wrong**.
- **A preview in JEI, EMI and the manuals**: in 3D, layer by layer, cycling through the choices each place allows, with
  the **list of materials** and the counts for the size chosen.
- **Building it for the player**: a tool builds the multiblock from the player's inventory, as GregTech's terminal does,
  asking the player to choose where a place allows several blocks; with J's Computers installed, the network builds it
  from storage.

### For modpacks

**Multiblocks entirely as data**: the shape, the ports, the kind of recipe and the numbers declared in a data pack,
with no code, as Modular Machinery does, so a modpack makes machines of its own.

### Moving

Multiblocks can be **sections of a moving structure**, such as a rocket's parts, with known models and collision
([Moving structures](moving-structures.md)).
