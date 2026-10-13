# Machines

What every machine has, the upgrades, the rules of multiblocks, and the machines of each tier. The machines of the
energy grid are in [Energy](energy.md); the chemical and petroleum machines in [Chemistry](chemistry.md); pipes, tanks
and pumps in [Fluids and gases](fluids-and-gases.md); conveyors in [Conveyors](conveyors.md); the Clean Room in
[Clean Room](clean-room.md); accelerators and research in [Science](science.md); reactors in [Nuclear](nuclear.md).

The machines follow the real history of industry: each tier has the machines of its time, with their real names.

## What exists today

- J's Core has a **processing machine** for any mod: input and output slots, tanks, upgrade slots, and the recipes every
  kind of machine shares. Pipes put into its inputs and its upgrade slots and take from anywhere. Upgrades multiply its
  speed and the energy it spends.
- J's Core has **multiblocks**: a shape drawn in layers, a controller that checks the blocks in the world make it, in
  any of the four horizontal directions, and ports at fixed places of the shape (items, fluids, energy, in or out). The
  shapes are data.
- J's Industrial has the **Electric Furnace**, the **Macerator** and the **Compressor**, and the Coal Generator
  ([Energy](energy.md)).

## To build

### What every machine has

- **Resources.** A machine takes and gives **items**, **liquids**, **gases**, **plasmas**, **supercritical fluids**,
  **slurries**, **exotic matter** ([Materials](materials.md)) and **heat**, and energy through its port, which has a
  voltage class ([Energy](energy.md)).
- **Sides.** Each face of a machine takes, gives, does both or does nothing, for each resource. **Auto-output** pushes
  what it makes into the inventory or the pipe next to it.
- **Redstone.** A machine runs always, with a signal, without one, or ignores redstone. A comparator reads its progress
  or how full it is.
- **Its state is always in view.** The screen says whether it is working, waiting for input, has its output full, has no
  energy, is on undervoltage, has a blown fuse, is damaged or is turned off by redstone. In the world, a working machine
  shows it: its front lights up and it moves.
- **No wear.** A machine doesn't wear out with use. It is only damaged by overvoltage, an explosion or sabotage, or by
  overheating ([Upgrades](#upgrades)).
- **Damage and repair.** A damaged machine looks cracked, runs at half speed, spends 25% more and takes no upgrades. It
  is repaired the way Factorio's repair packs work: the **Repair Kit** (T1, plates and wire) is held and used on the
  machine, and repairs it over a few seconds, spending itself as it goes. The **Maintenance Kit** (T3) carries more and
  is refilled with materials of the machine's tier.
- **The Wrench** takes a machine away whole: its contents, upgrades and settings go with it. A pickaxe breaks it and
  drops what it held.
- **One screen for all.** Every machine of the series has the same tabs, from J's Core's screen toolkit: **Main**,
  **Sides**, **Redstone**, **Upgrades** and **Protection** (the fuse and the battery slot, [Energy](energy.md)).
- With J's Computers installed, machines join its network through the Industrial Controller Computer
  ([Computers](computers.md)).

### Upgrades

Upgrades are real parts installed in the machine. They are never used up, except the catalyst.

- **Slots are few:** 2 on a T1 machine, 3 on T2 and T3, 4 from T4. Each upgrade takes one, so a machine is fast or
  thrifty, hardly both: whoever wants both builds more machines.
- **Speed costs energy per item.** Speed adds up and savings multiply: four motors make a machine 3 times as fast and
  spend a third more per item; four drives leave it at 41% of its energy. Packing a factory tight costs energy;
  spreading it out costs room and materials. Both are fair ways to play.

| Upgrade | Tier | Effect | Most per machine |
| --- | --- | --- | --- |
| Motor Upgrade (brushed, induction, brushless) | T1, T3, T4 | +50% speed, +75% energy | 4 |
| Variable Frequency Drive | T3 | −20% energy | 4 |
| Parallel Line | T3 | +1 recipe at once; energy grows in proportion | 4 |
| Advanced Parallel Line | T5 | +2 recipes at once | 4 |
| Gas Recovery Unit | T3 | −15% gases | 4 |
| Solvent Recovery Unit | T3 | −15% liquids | |
| Precision Tooling | T4 | +10% yield | 2 |
| Overdrive | T4 | +100% speed, double the energy, and it **heats** | 2 |
| Muffler | T1 | the machine is silent | |
| Thermal Insulation | T2 | −25% energy in recipes that heat (furnaces, kilns, chemical reactors) | |
| Cooling Fan | T2 | takes some heat away: a machine with one Overdrive needs no other cooling | |
| Expanded Buffers | T2 | double the input and output slots and tanks | |
| Byproduct Separator | T3 | +50% byproducts | |
| Lead Shielding | T3 | no radiation leaves the machine while it processes radioactive materials | |
| Catalyst Cartridge | T3 | chemical machines only: +50% speed; it wears out slowly, as real catalysts do (platinum, palladium, zeolites) | |

An empty "most per machine" means the slots are the only limit.

**Overdrive heats.** A machine in Overdrive needs its heat taken away through its heat port, or it overheats and is
**damaged**, as on overvoltage.

**Nothing works within a radius.** What serves many machines reaches each one through a pipe or a line, as in a real
factory:

- the **Central Cooling Plant** (T3, multiblock) takes heat from machines through **heat pipes**, spends water and vents
  **low-pressure steam** into the air, a plume you can see. It makes no electricity. Every kilogram of water it
  evaporates carries away 2.26 MJ of heat;
- the **Compressed Air Plant** (T2, multiblock) draws air through an open face, compresses it into an **air receiver**
  and sends it through its own **pressurised air lines** ([Fluids and gases](fluids-and-gases.md)). Machines with an air
  port (presses, assemblers, and later the pneumatic tools) spend air and work **15% faster** with it.

### Multiblocks

- **Materials of their time:** brick, wood and iron before the T3; steel from the T3 ([Metallurgy](metallurgy.md)).
- **Ports are blocks** the player places anywhere on the shell, as the valves and hatches of a real plant: the
  connection goes where the pipe arrives. There is one for each resource, the energy port (with its class), the
  compressed air port, and an **optional redstone port** that reads and gives signals.
- **Once formed, a multiblock becomes one detailed, animated model**: the turbine spins, the Steam Engine's flywheel
  turns, the tower lets out steam. The ports stay visible where they were placed. Breaking one block undoes the
  structure; the controller keeps what it held.
- **Help to build:** the manual shows the structure layer by layer, in 3D, and the **Structure Projector** shows the
  missing blocks in the world as ghosts, with a count of what is missing.
- **Size can vary**, by machine: a longer Coke Oven is a battery of more ovens, a bigger Blast Furnace takes more at
  once, a longer potline has more cells; storage grows its capacity with its size. Each multiblock says what its size
  gives.
- **The controller is the multiblock's screen**, with the same tabs as every machine, upgrades and protection included.

### By hand, before electricity (T0)

The **Forge Hammer**, the **Draw Plate**, the **Mortar** and the clay **Bloomery** ([Metallurgy](metallurgy.md)).

### T1: the electric industry is born (1880 to 1920)

All on low voltage.

| Machine | What it does | Power |
| --- | --- | --- |
| Electric Furnace | smelts: the first level of ore processing | 3 kW |
| Macerator | a mineral into 2 dusts and a little of its byproducts: the second level | 5 kW |
| Compressor | ingot into plate | 4 kW |
| Wire Drawer | ingot into 2 wires | 3 kW |
| Lathe | ingot into rod, rod into bolts | 3 kW |
| Coil Winder | wire into coils and springs: the relays, the transformers' and motors' windings | 2 kW |
| Assembler | parts of several ingredients; mechanical and electromechanical circuits in quantity ([Tiers and circuits](tiers-and-circuits.md)) | 2 kW |
| Basic Electrolyser | water into hydrogen and oxygen | 5 kW |
| Mixer | dusts and liquids: bronze, solder, simple alloys | 2 kW |
| Extractor | rubber from resin, liquids from items | 1.5 kW |
| Water Pump | endless water from a source next to it | 1 kW |
| Sawmill | a log into 6 planks (4 by hand) and sawdust | 4 kW |
| Paper Machine | sawdust or pulp into paper; the oiled paper of the Paper-Insulated Lead Cable (the Fourdrinier machine, 1807) | 3 kW |
| Electroplating Tank | coats parts with tin, zinc, nickel, copper, silver or gold: tinplate, galvanised iron, gold contacts | 2 kW |
| Tar Still | coal tar into creosote, pitch and naphthalene; the pitch comes back as the binder of graphite electrodes | 2 kW |
| Wood Treatment Tank | wood and creosote into treated wood: the Wooden Pole, railway sleepers | 1 kW |
| Cable Machine | wire and insulation into cable, with more yield than by hand; it gains steps in the T2 (lead sheath) and T3 (XLPE extrusion, armour) | 4 kW |
| Ice Machine | water into ice: the cold side of the Thermoelectric Generator anywhere (Linde's refrigeration, 1876) | 3 kW |
| Coke Oven (brick multiblock, variable size) | coal into coke and coal tar, wood into charcoal and wood tar; a longer one is a battery of ovens, as real ones | burns fuel |
| Kiln (brick multiblock) | fires porcelain (the insulators and fuse carriers of [Energy](energy.md)), firebricks and ceramics | 6 kW, or burns fuel |

Rubber is **vulcanised** in the Electric Furnace (rubber and sulfur, heated, as Goodyear did in 1839): it is the rubber
of the Insulated Copper Cable.

### T2: chemistry and petroleum (1920 to 1950)

Metal forming ([Metallurgy](metallurgy.md)): the **Hydraulic Press**, the **Rolling Mill** and the **Extruder**, with
dies. The vacuum tube line ([Tiers and circuits](tiers-and-circuits.md)): the **Etching Tank**, the **Vacuum Pump** and
the **Wave Soldering Machine**. Also the **Industrial Greenhouse**, which grows saplings into logs (and the rubber
trees, [The world](world.md)).

**The third level of ore processing opens**, the real process of each family ([Metallurgy](metallurgy.md)):

| Machine | What it does | Class and power |
| --- | --- | --- |
| Vibrating Screen | sorts crushed ore by size, before flotation | low, 3 kW |
| Flotation Cell | separates sulfides from the waste rock with froth | low, 15 kW |
| Roasting Furnace | roasts sulfides and arsenides into oxides; sulfur dioxide goes up its chimney | low, 20 kW |
| Electrostatic Precipitator | sits on the chimneys of the Roasting Furnace and of boilers and catches their dust, from which **thallium, selenium, tellurium, germanium, cadmium and indium** come, as from real flue dust (Cottrell, 1907) | low, 5 kW |
| Calciner | carbonates into oxides and carbon dioxide | low, 20 kW |
| Leaching Tank and Electrowinning Cell | dissolve copper in acid and lay it pure on cathodes (Chuquicamata, 1915) | low, 30 kW |
| Bayer Digester | bauxite and caustic soda into alumina (1888) | low, 20 kW |
| Hall-Héroult Cell (multiblock) | alumina and cryolite into aluminium (1886): the aluminium of the T2 cables | medium, 1 MW a cell |

**Powders, heat and parts:**

| Machine | What it does | Class and power |
| --- | --- | --- |
| Sintering Furnace | pressed powders into sintered parts: the **tungsten filament** of vacuum tubes (Coolidge, 1910), ferrite magnets, hard metal | low, 30 kW |
| Induction Furnace | melts metals and alloys fast and clean: Kovar, nichrome | medium, 200 kW |
| Magnetizer | permanent magnets: Alnico (1931), ferrite (1952) | low, 10 kW |
| Injection Molder | plastic and a mould into parts: cases, insulators, plugs | low, 15 kW |
| Glass Furnace (brick multiblock, variable size) and Bulb Machine | molten glass into tubing and bulbs, the bulbs of vacuum tubes (Corning's Ribbon Machine, 1926, blew thousands an hour) | fuel, or medium, 100 kW |
| Centrifuge | separates liquids and slurries by density | low, 10 kW |

### T3: steel and the big multiblocks (1950 to 1970)

**Steel and heavy metal** ([Metallurgy](metallurgy.md)):

| Machine | What it does | Class and power |
| --- | --- | --- |
| Blast Furnace (variable size) | pig iron; a bigger one takes more at once | medium, 2 MW (its blowers) |
| Oxygen Converter | pig iron and oxygen into steel | medium, 1 MW |
| Electric Arc Furnace | scrap and special alloys | **high, 50 MW**, straight on the line |
| Continuous Caster | molten steel into slabs and billets, without ingots | medium, 2 MW |
| Hot Strip Mill | steel sheet at scale | medium, 10 MW |
| Heavy Forging Press | plates in batches and big forged parts (the presses of 50,000 tonnes of 1955) | medium, 5 MW |
| Industrial Wire Drawer | special wires, the superconducting ones | medium, 500 kW |
| Vacuum Furnace | reactive metals: titanium, zirconium | medium, 1 MW |
| Kroll Reactor | titanium, reduced with magnesium (1948) | medium, 500 kW |
| Hall-Héroult Potline (variable size: more cells in a row) | aluminium at scale | **high, up to 300 MW** |
| Ball Mill (variable size: a longer drum grinds more ore at once) | grinding at scale | medium, 1 MW |

**Precision and materials:**

| Machine | What it does | Class and power |
| --- | --- | --- |
| Precision Assembler | T3 circuits, boards, fine parts ([Tiers and circuits](tiers-and-circuits.md)) | low, 50 kW |
| Czochralski Furnace | single crystals of silicon and germanium | low, 100 kW |
| Zone Refiner | purifies germanium, silicon and metals to semiconductor grade (Pfann, 1952) | low, 30 kW |
| NC Milling Machine | precision parts (turbine blades, fine gears), programmed on **punched tape** (MIT, 1952) | low, 40 kW |
| Autoclave | pressure and heat: synthetic quartz crystals (the oscillators of electronics), fibre composites | medium, 500 kW |
| HPHT Diamond Press | synthetic diamond for drill bits, saws and cutting tools (General Electric, 1954) | medium, 1 MW |
| Float Glass Line (variable size) | flat glass: solar panels, windows, screens (Pilkington, 1959) | medium, 5 MW |

**Mining:** the **Mining Excavator**, the electric shovels of open-pit mines, digs a marked area (up to 64 by 64) layer
by layer, spends energy for each block and uses nothing else. Medium, 2 MW.

**Cooling:** the **Cooling Tower** (variable size) and the Central Cooling Plant.

### T4: microelectronics and nuclear (1970 to 1990)

| Machine | What it does | Class and power |
| --- | --- | --- |
| Laser Cutter | cuts plates and sheets into precise parts; spends carbon dioxide slowly (the CO₂ laser, 1964) | low, 100 kW |
| CNC Machining Center | five axes, complex parts | low, 60 kW |
| Electron Beam Furnace | melts refractory metals (niobium, tantalum, molybdenum) to ultra-pure: the niobium of NbTi | medium, 2 MW |
| Carbon Fibre Line (variable size) | precursor fibre into carbon fibre through a row of furnaces: the blades of the Wind Turbine, light structures | medium, 3 MW |
| Fibre Drawing Tower | draws optical fibre and glass fibre from a preform (Corning's low-loss fibre, 1970) | medium, 500 kW |
| Waterjet Cutter | cuts any material with water and **garnet** abrasive | low, 100 kW |
| 3D Printer | stereolithography: resin into plastic parts, moulds and dies for the Extruder and the Hydraulic Press (Chuck Hull, 1984) | low, 5 kW |
| In-Situ Leach Field | takes uranium or copper out of permeable sandstone deposits **without digging**: it pumps a solution in and back out | medium, 1 MW |

### T5: nanotechnology (1990 to 2030)

| Machine | What it does | Class and power |
| --- | --- | --- |
| Metal 3D Printer | metal powder into complex parts: turbine blades with cooling channels, rocket engine parts (selective laser melting) | low, 50 kW |
| Fibre Laser Cutter | faster than the Laser Cutter, and spends no gas (ytterbium fibre lasers) | low, 50 kW |
| CVD Diamond Reactor | laboratory diamond: heat spreaders for chips, windows, gems | medium, 500 kW |
| Nanomaterials Furnace | graphene and carbon nanotubes by vapour deposition | medium, 1 MW |
| REBCO Tape Line (variable size) | the superconducting tape of the REBCO cable and of the Tokamak's magnets, as today's compact tokamaks use | medium, 2 MW |
| Battery Cell Line (variable size) | lithium and solid-state cells in quantity, as the gigafactories | medium, 10 MW |

### T6 to T9

The machines of transcendence are decided with [Exotic materials](exotic-materials.md): the microgravity and antimatter
chambers, the dimensional portal, the reality anchor, the dark energy collector, the refiners of the exotic elements,
the reality projector, the Creation Forge, and the **Orion Miner**, which mines by teleportation.

### Machines of other mods

Decorative machines belong to **J's Civil Works**, which makes the blocks they serve: the stone cutter, the panel press,
the joinery, the decorative ceramics kiln and glass furnace, the upholstery bench, the lighting line, the appliance
line, the customization centre, the blueprint burner and the construction printer. The loom goes with the textiles, to
J's Civil Works or J's Agriculture. The gravitational interferometer and the megastructure assembler are **J's
Space**'s; the construction aerobots, **J's Robotics**'.
