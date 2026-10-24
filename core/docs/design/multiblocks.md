# Multiblocks

Machines built from many blocks placed in a shape: a coke oven, a blast furnace, a reactor, a rocket part. The Core
says what the shape is, checks the world against it, turns it into one machine, and helps the player build it.

## What exists today

Everything this page designs is built, as of October 2026, except what *Still to build* at the end lists: shapes by
pattern and by rule, the inside that counts, ports as blocks anywhere on the shell, the entries a multiblock lets
stand on the places it chooses, forming, the Structure Projector and the Multiblock Builder, the recipe viewers' pages,
multiblocks entirely as data, the studio's recorder of patterns, multiblocks moving, and big blocks as a kind of
their own. How a mod uses it, with code: [Multiblocks](../MULTIBLOCKS.md).

### Words

- **The terminal** is the block of a multiblock the player works it from: it checks the shape, holds the machine's
  state and is its screen. It was called the controller until October 2026; the kit, the pattern files, the
  Projector, the Builder and the recipe viewers say terminal now.
- **A big block** is one block bigger than a block, placed whole, which raises its other cells at once and builds
  nothing: J's Computers' Mainframe and its racks. It is no multiblock and has a kit of its own, apart; its first
  cell is its **origin**.

## The design

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
- **Entries where the multiblock says**: a multiblock built of blocks of its own has **entries**, blocks of its own
  through which something comes in or goes out (a power entry, a coolant inlet, a link socket). The multiblock declares
  the places where its entries may go, and the player puts each where it suits among them; an entry anywhere else is
  a wrong block. Each entry is counted, so the multiblock says how many of each it takes (exactly one power entry, at
  most one link socket), makes its place the port of its kind, and two counts may **go together**, so a coolant's
  inlet never stands without its outlet.
- **Ports grouped by colour**: ports of one colour are one group, and a recipe takes its inputs from one group at a
  time, so two processes never mix.
- **Recipes in parallel**: a multiblock runs several recipes at once, by its size or by the parts it has, and shows the
  exact number.
- **The terminal is the machine's screen**, with the same tabs as every machine ([Machines](machines.md)).

### In the series

Every multiblock of the series' mods is built of **blocks of its own and its terminal**: a casing, a gate or a bus of
that machine, never a block shared with another, so each machine looks like itself. Each declares where its entries
go, on the faces where a real one has its connections: J's Industrial's Computational Research Centre takes them on
the six places of the two lower rows of its rear face, and nowhere else. A mod
outside the series builds its multiblocks as it likes; the kit takes any block.

### Formed

- **Once formed, a multiblock becomes one detailed, animated model**, drawn by the Core's own models
  ([Low level](low-level.md)): the turbine spins, the flywheel turns, the tower lets out steam. The ports stay visible
  where they were placed. Forming has its effect and its sound: the blocks merge into the model.
- **Breaking one block undoes the structure**; the terminal keeps what it held. Only the owner and the owner's team
  can break the blocks of a formed multiblock ([The platform](platform.md#teams-and-owners)).
- **Formed or not** shows in Jade and in the tooltip, with the dimensions when Shift is held.
- **Checking is incremental**: a structure is checked again only when one of its blocks, or a block beside it, changes,
  never every tick, and safely across chunk borders.

### Building it

- **Help to build**: a manual shows the structure layer by layer, in 3D ([Manuals](manuals.md)); and the **Structure
  Projector**, a block of the Core's shared content, shows the missing blocks in the world as ghosts (the block itself,
  pale and see-through), with a count of what is in place, missing and wrong floating above the structure, and **marks
  in red, in the world, a block that is wrong**; looking at a ghost says what goes there and on which layer.
- **The Projector's screen** has no inventory, since it holds nothing: a 3D preview layer by layer (the chosen layer
  whole, the ones below as ghosts), the list of the installed mods' multiblocks, what is in the world, and the turn, the
  mirror (for shapes that allow it), "only what is missing" and the layer.
- **Every multiblock can be projected by default**: registering a multiblock puts it on the Projector's list, with
  nothing more to write. A mod that doesn't want one projected registers it through the overload that says so
  (projectable: false).
- **A preview in JEI, EMI and the manuals**: in 3D, layer by layer, cycling through the choices each place allows, with
  the **list of materials** and the counts for the size chosen, and whether it turns or mirrors; from there it is
  projected into the world or its manual page opened.
- **Building it for the player**: a handheld tool of the instruments' family (the Multiblock Builder, a name to be
  confirmed), in the spirit of GregTech's terminal, aimed at a terminal, says what it will place and how much the
  player has, asks the player to choose where a place allows several blocks, and builds it block by block from the
  player's inventory; with J's Computers installed, from the network's storage.

### For modpacks

**Multiblocks entirely as data**: the shape, the ports, the kind of recipe and the numbers declared in a data pack,
with no code, as Modular Machinery does, so a modpack makes machines of its own.

**A recorder of patterns**, a tool of the studio: a multiblock built in the world becomes its pattern's file, its
projector preview and its page in the recipe viewers, in one step.

### Moving

Multiblocks can be **sections of a moving structure**, such as a rocket's parts, with known models and collision
([Moving structures](moving-structures.md)).

### Big blocks

A big block is placed at its origin and raises its other cells round it at once; it refuses a place where a cell would
land on something, and shows which with a flame outline; breaking any cell takes the whole of it down and gives back
what it held, however it goes. Its cells remember the origin, so a cable touching any of them reaches the machine.

## Still to build

- **The Multiblock Builder taking from a network's storage** is J's Computers' side of it, when J's Computers moves onto
  the Core.
