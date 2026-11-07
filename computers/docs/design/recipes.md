# Recipes

What every block and item of J's Computers is made of, and where. The chain itself, the materials, the machines,
their numbers and how a chip is made, is in [Survival](survival.md). Every number is a first estimate, and every
recipe is data that JEI, EMI and the manuals show and that a modpack can change through KubeJS.

## To build

Nothing on this page exists yet.

### How the recipes are written

- **Three kinds of station.** The vanilla Crafting Table makes what is mechanical: the machines' casings, entries
  and working blocks with no electronics in them, and the few blocks and items that need none. The Electronics
  Workbench of an era makes by hand what is electronic, and that era's Electronics Assembly Line makes the same
  recipes by itself, so the network's crafting patterns run every recipe but the bench's own. The chain's machines
  make the materials and the components.
- **One recipe, one result.** In one station a recipe gives one thing only: no two Crafting Table recipes take the
  same grid (a grid and its mirror count as one, as the game matches both, and no grid a vanilla recipe takes), no
  two bench recipes the same ingredients in the same counts, and no two recipes of a machine the same inputs in the
  same amounts. The same recipe may give something else in another station, and two recipes may give the same
  thing. A bench makes the recipes of its era and of every era before it, so bench recipes never repeat across the
  eras either.
- **What the chain is made of.** What is mined, grown and processed: no dye, no mob's drop, and no vanilla gadget
  standing in for a component (a hopper is not a pump, a comparator not a chip). Vanilla's mined and grown things are
  fine: iron, copper, gold, redstone, lapis lazuli, diamond, sand, coal, logs, glass, paper.
- **Bigger takes more.** In a family, each bigger or faster member takes at least one more of something it counts
  (a chip, a contact, a battery), so no two members share a recipe and the order reads as the real one.
- **How a grid is written.** Its rows, top to bottom, separated by slashes, each cell by its code below and a dot
  for an empty one; a grid smaller than three by three fits anywhere in the table. A bench recipe is its list of
  ingredients, which its nine trays take in any order.

| Code | Item | Code | Item | Code | Item |
| --- | --- | --- | --- | --- | --- |
| `Fe` | iron ingot | `FeB` | block of iron | `n` | iron nugget |
| `Cu` | copper ingot | `Sn` | tin ingot | `Pb` | lead ingot |
| `Ni` | nickel ingot | `Mn` | manganese ingot | `Zn` | zinc ingot |
| `Au` | gold nugget | `D` | diamond | `R` | redstone |
| `w` | copper wire | `SP` | steel plate | `SR` | steel rod |
| `SS` | stainless steel plate | `AP` | aluminium plate | `AS` | aluminium sheet |
| `Mo` | molybdenum plate | `M` | electric motor | `Co` | copper coil |
| `T` | transformer | `Al` | alumina | `K` | coke |
| `PVC` | PVC sheet | `GF` | glass fibre | `G` | glass |
| `GP` | glass pane | `Gr` | polished granite | `B` | brick |
| `Oak` | oak planks | `CT` | crafting table | `L` | glass lens |
| `CaF` | calcium fluoride lens | `Hg` | bucket of mercury | `Si` | silicon wafer |
| `MC` | Machine Casing | `RC` | Refractory Casing | `LC` | Lined Casing |
| `VC` | Vacuum Casing | `CC` | Clean Room Casing | `LI` | Liquid Inlet |
| `LO` | Liquid Outlet |  |  |  |  |

### The benches

The Vintage Electronics Workbench at the Crafting Table, its oak top, its steel drawers, its lens and its
soldering iron: `Oak Oak Oak / Fe CT GP / Fe Cu Fe`.

Each next bench at the bench of the era before, with its era's etched board and controller chip and what its
model shows: the soldering station (the transformer), the mat (PVC), the hot air gun (the motor), the
multimeter (the liquid crystal screen), the microscope (the lenses), the ring of LEDs and the tablet:

| Bench | Made at | Of |
| --- | --- | --- |
| Legacy | the Vintage bench | 1 Vintage Electronics Workbench, 4 steel plates, 1 Legacy etched board, 1 Legacy controller chip, 1 transformer and 1 PVC sheet |
| Transition | the Legacy bench | 1 Legacy Electronics Workbench, 4 steel plates, 1 Transition etched board, 1 Transition controller chip, 1 electric motor, 1 liquid crystal screen and 1 PVC sheet |
| Standard | the Transition bench | 1 Transition Electronics Workbench, 4 aluminium plates, 1 Standard etched board, 1 Standard controller chip, 2 calcium fluoride lenses and 1 PVC sheet |
| Advanced | the Standard bench | 1 Standard Electronics Workbench, 4 aluminium plates, 1 Advanced etched board, 1 Advanced controller chip, 8 LEDs and 1 self-lit panel |

### The blocks of the machines

Every machine is its casing, its entries, its terminal and its working blocks ([Survival](survival.md)). The
casing and the entries are the same for every machine, so a machine is told by its working blocks; formed, it
shows its own sculpted model in its own colour.

**The casings, by duty.** Five, each for the work it stands, at the Crafting Table, 4 a recipe:

| Casing | Recipe | For | Machines |
| --- | --- | --- | --- |
| Machine Casing | `Zn Fe Zn / Fe . Fe / Zn Fe Zn` | galvanised steel: mechanical work | Crusher, Grinder, Rolling Mill, Wire Drawer, Coil Winder, Wafer Saw, Laminator, Optical Polisher, Packaging Line, Electronics Assembly Line, Prebuilder, Preinstaller |
| Refractory Casing | `B Fe B / Fe B Fe / B Fe B` | refractory brick: heat | Coke Oven, Induction Furnace, Ceramic Kiln, Crystal Puller, Siemens Reactor |
| Lined Casing | `Fe G Fe / G . G / Fe G Fe` | glass-lined steel: corrosion | Chemical Reactor, Electrolytic Cell, Board Etcher |
| Vacuum Casing | `Cu SS Cu / SS . SS / Cu SS Cu` | stainless steel with sealed flanges: vacuum and gases | Plasma Etcher, Ion Implanter, Deposition Chamber, Display Line, Air Separation Unit |
| Clean Room Casing | `AP GF AP / GF . GF / AP GF AP` | a clean room panel with its filter: lithography | Lithography Station, every era |

The Vacuum Casing is of stainless steel, the Clean Room Casing of aluminium and glass fibre; the casings of
the first step are only iron, zinc, brick and glass, which exist before any machine.

**The entries and the energy**, the same for every machine, at the Crafting Table. The shape tells the way: a
mouth up takes in, a mouth down gives out. Iron for items, copper for liquids, and a gas entry is a liquid one
with its valve.

| Block | Recipe | Gives | Of |
| --- | --- | --- | --- |
| Item Inlet | `Fe . Fe / Fe . Fe / . Fe .` | 2 | iron, its mouth up |
| Item Outlet | `. Fe . / Fe . Fe / Fe . Fe` | 2 | iron, its mouth down |
| Liquid Inlet | `Cu . Cu / Cu . Cu / . Cu .` | 2 | copper, its mouth up |
| Liquid Outlet | `. Cu . / Cu . Cu / Cu . Cu` | 2 | copper, its mouth down |
| Gas Inlet | `Fe / LI` | 1 | the liquid one and its iron valve |
| Gas Outlet | `Fe / LO` | 1 | the liquid one and its iron valve |
| Power Entry | `. Cu . / Cu Fe Cu / . Cu .` | 1 | four copper ingots round an iron one |

**The terminal and the peripheral port** are electronic, so they are bench recipes: made by hand at the
Vintage bench until the Vintage Electronics Assembly Line exists, which then makes them by itself (the machines
of the first two steps, and the line itself, need a terminal before it). One terminal for every machine, in the
neutral steel:

| Block | Recipe |
| --- | --- |
| Terminal | 2 iron ingots, 2 copper ingots, 2 redstone and 1 glass |
| Peripheral Port (only the Lithography Station uses it, from the Legacy) | 2 iron ingots, 1 Vintage peripheral cable, 1 Vintage etched board and 1 Vintage controller chip |

**The Vintage machines come in three steps**, each made only of what the steps before it make:

| Step | Machines | Made of |
| --- | --- | --- |
| 1. From vanilla things | Coke Oven, Crusher, Grinder, Induction Furnace, Rolling Mill, Wire Drawer, Electrolytic Cell, Chemical Reactor | iron, copper, tin, lead, nickel, manganese, zinc, brick, glass, diamond, redstone; the motor made by hand (below) |
| 2. From the first metals | Coil Winder, Ceramic Kiln, Laminator, Board Etcher, Siemens Reactor, Crystal Puller, Wafer Saw | steel plates and rods, copper wire, PVC, alumina, coke: what step 1 makes |
| 3. The chip factory | Optical Polisher, Air Separation Unit, Display Line, Lithography Station, Plasma Etcher, Ion Implanter, Deposition Chamber, Packaging Line, Electronics Assembly Line, Prebuilder, Preinstaller | motors, transformers, coils, etched boards, screens: what step 2 makes |

The Lithography Stations and the Electronics Assembly Lines of the later eras are made at their own era's
bench, with the parts of the era before.

#### Each machine's working blocks

What makes each machine what it is, made of what the real one has. The casing named in a grid is the
machine's own (above). A bench recipe is made at that era's bench by hand and by its Electronics Assembly
Line.

**Crusher** (step 1, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Crusher Roll | Crafting Table | `Mn Fe Mn / Fe MC Fe / Mn Fe Mn` | rolls of manganese steel |
| Crusher Drive | Crafting Table | `. MC . / FeB M FeB` | the motor with a flywheel each side |

**Grinder** (step 1, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Grinder Drum | Crafting Table | `Fe n Fe / n MC n / Fe n Fe` | the drum and its balls |
| Grinder Drive | Crafting Table | `. MC . / . M . / Fe Fe Fe` | the motor and the ring gear |

**Induction Furnace** (step 1, Refractory Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Induction Coil | Crafting Table | `Cu Cu Cu / Cu RC Cu / Cu Cu Cu` | the casing ringed with copper |
| Capacitor Bank | Vintage bench | 1 Machine Casing, 4 copper ingots and 4 tin ingots | copper plates and tin foil, the first capacitors |

**Electrolytic Cell** (step 1, Lined Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Cell Stack | Crafting Table | `Cu Pb Cu / Pb LC Pb / Cu Pb Cu` | copper cathodes, lead anodes |

**Rolling Mill** (step 1, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Mill Housing | Crafting Table | `Fe Fe Fe / Fe MC Fe / . M .` | the stand and its motor |
| Work Roll | Crafting Table | `Ni Fe Ni / Fe MC Fe / Ni Fe Ni` | chilled iron with nickel |
| Roller Table | Crafting Table | `Fe Fe Fe / . MC . / Fe Fe Fe` | rollers above and below |

**Wire Drawer** (step 1, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Capstan and Die | Crafting Table | `D / MC / M` | the diamond die |

**Coke Oven** (step 1, Refractory Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Oven Chamber | Crafting Table | `B B B / B RC B / B B B` | lined with brick |
| Oven Door | Crafting Table | `. Fe . / Fe RC Fe / . Fe .` | the iron door |
| Standpipe | Crafting Table | `Fe / Fe / RC` | the pipe that rises |

**Chemical Reactor** (step 1, Lined Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Reactor Vessel | Crafting Table | `Fe G Fe / G LC G / Fe G Fe` | steel and glass |
| Agitator | Crafting Table | `M / LC / Fe` | the motor above, the shaft going down |

**Coil Winder** (step 2, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Winding Head | Crafting Table | `M MC SR` | the motor and its spindle |
| Supply Spool | Crafting Table | `w SR w / w MC w / . SR .` | the spool of wire |

**Ceramic Kiln** (step 2, Refractory Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Kiln Chamber | Crafting Table | `Al w Al / w RC w / Al w Al` | alumina and the heating wires |
| Kiln Car | Crafting Table | `. RC . / Fe Fe Fe / Fe . Fe` | the car and its wheels |

**Laminator** (step 2, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Hot Platen | Crafting Table | `SP SP SP / w MC w / . SP .` | the plates and the heating wires |
| Press Crown | Crafting Table | `SP M SP / SP MC SP` | the cylinder and the pump |

**Board Etcher** (step 2, Lined Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Spray Tunnel | Crafting Table | `PVC PVC PVC / . LC . / . M .` | the PVC tunnel and the pump |
| Etchant Sump | Crafting Table | `. LC . / PVC . PVC / PVC PVC PVC` | the PVC basin |

**Siemens Reactor** (step 2, Refractory Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Reactor Bell | Crafting Table | `SP G SP / SP RC SP` | the bell and its window |
| Base Plate | Crafting Table | `w T w / w RC w` | the power of the rods |

**Crystal Puller** (step 2, Refractory Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Hot Zone | Crafting Table | `K G K / K RC K` | the graphite heater and the window |
| Pull Chamber | Crafting Table | `M / SR / RC` | the motor and the rod that pulls |

**Wafer Saw** (step 2, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Wire Spool | Crafting Table | `SR M SR / . MC .` | the motor between two spools |
| Wire Web | Crafting Table | `SR D SR / . MC .` | the diamond wire |

**Optical Polisher** (step 3, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Polishing Lap | Crafting Table | `Al . Al / . MC . / . M .` | the alumina that polishes |
| Draw Tower | Crafting Table | `Co / MC / M` | the furnace and the motor |

**Air Separation Unit** (step 3, Vacuum Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Compressor | Crafting Table | `SP M SP / SP VC SP / . M .` | two motors |
| Adsorber | Crafting Table | `SP Al SP / . VC . / . Al .` | the alumina sieve |
| Distillation Column | Crafting Table | `. AP . / AP VC AP / . AP .` | of aluminium |
| Vaporizer | Crafting Table | `AS . AS / AS VC AS` | the aluminium fins |

**Display Line** (step 3, Vacuum Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Fill Chamber | Crafting Table | `G G G / G VC . / . M .` | the glass and the vacuum pump |
| Gantry | Crafting Table | `M SR M / . VC .` | two motors and the rail |
| Test Stand | Vintage bench | 1 Vacuum Casing, 1 Vintage etched board and 1 tube screen | the screen that checks each one |

**Lithography Station** (step 3, Clean Room Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Wafer Stage, every era | Crafting Table | `. M . / Gr CC Gr / Gr . Gr` | the granite table |
| Projection Optics, Vintage and Legacy | Crafting Table | `. L . / L CC L / . L .` | glass lenses |
| Projection Optics, Transition and Standard | Crafting Table | `. CaF . / CaF CC CaF / . CaF .` | calcium fluoride lenses |
| Projection Optics, Advanced | Crafting Table | `Mo Si Mo / Mo CC Mo / . Si .` | molybdenum and silicon mirrors |
| Mercury Lamp, Vintage | Crafting Table | `G Hg G / . CC . / . T .` | the lamp and its transformer |
| KrF Laser, Legacy | Legacy bench | 1 Clean Room Casing, 2 transformers, 2 glass lenses and 1 Vintage etched board | the excimer laser |
| ArF Laser, Transition and Standard | Transition bench | 1 Clean Room Casing, 2 transformers, 2 calcium fluoride lenses and 1 Legacy etched board | the excimer laser |
| EUV Source, Advanced | Advanced bench | 1 Clean Room Casing, 2 transformers, 2 molybdenum plates, 1 tin ingot and 1 Standard etched board | the tin plasma source |

**Plasma Etcher** (step 3, Vacuum Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Etch Chamber | Crafting Table | `G Co G / . VC . / . T .` | the RF coil and the window |

**Ion Implanter** (step 3, Vacuum Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Analyzer Magnet | Crafting Table | `Fe Co Fe / Co VC Co / Fe Co Fe` | coils round an iron core |
| End Station | Crafting Table | `SS M SS / . VC .` | the disc and its motor |

**Deposition Chamber** (step 3, Vacuum Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Sputter Chamber | Crafting Table | `SS Co SS / . VC . / . T .` | the magnetron |

**Packaging Line** (step 3, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Wire Bonder | Vintage bench | 1 Machine Casing, 1 electric motor, 1 Vintage etched board and 1 gold bonding wire | the bonder |
| Mould Press | Crafting Table | `. M . / SP MC SP / SP SP SP` | the press and the mould |

**Electronics Assembly Line** (step 3, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Feeder Bank, every era | Crafting Table | `SP SP SP / M MC M` | the feeders |
| Placement Head, Vintage | Vintage bench | 1 Machine Casing, 2 electric motors, 1 Vintage etched board and 1 glass lens | the head and its lens |
| Placement Head, Legacy on | at each later era's bench, with the era before's etched board and controller chip | 1 Machine Casing and 2 electric motors, with the parts the rule names | the head and its camera |
| Wave Solder, Vintage and Legacy | Crafting Table | `Sn Sn Sn / . MC . / Sn M Sn` | the tin pot |
| Reflow Oven, Transition on | at each era's bench from the Transition, with the era before's etched board | 1 Machine Casing, 4 copper wires and 1 electric motor, with the parts the rule names | the oven |

**Prebuilder** (step 3, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Case Lift | Crafting Table | `SR . SR / . MC . / . M .` | the guides and the motor |
| Parts Tray | Crafting Table | `SP SP SP / . MC .` | the tray |
| Robot Arm | Vintage bench | 1 Machine Casing, 2 electric motors and 1 Vintage etched board | the arm |

**Preinstaller** (step 3, Machine Casing):

| Block | Where | Recipe | What it is |
| --- | --- | --- | --- |
| Cradle | Crafting Table | `SP . SP / . MC .` | the cradle |
| Board Fixture | Crafting Table | `Au . Au / SP MC SP` | its gold pins |
| Console | Vintage bench | 1 Machine Casing, 1 tube screen and 1 Vintage etched board | the screen and its board |
| Disk Dock | Vintage bench | 1 Machine Casing, 1 Vintage etched board and 4 copper wires | the dock and its leads |

### Components made at a bench

Most components come out of the chain's machines ([Survival](survival.md)); these are assembled at a bench:

| Component | Bench | Recipe | Gives |
| --- | --- | --- | --- |
| Electric motor, by hand | Vintage | 4 iron ingots, 4 copper ingots and 1 redstone | 1 |
| Antenna | Legacy | 1 copper rod and 1 PVC sheet | 2 |
| Memory Layout | Vintage | 1 PVC sheet and 1 ferrite (core memory, which the memory chip replaced) | 1 |
| Controller Layout | Vintage | 1 PVC sheet and 1 magnetic head (the drive the controller chip runs) | 1 |
| Processor Layout of the Motoroller 68000 | Vintage | 1 PVC sheet and 1 oscillator crystal (its clock) | 1 |
| Processor Layout of the 486 | Vintage | 2 PVC sheets and 1 oscillator crystal (its clock) | 1 |
| Processor Layout of the P5 | Vintage | 3 PVC sheets and 1 oscillator crystal (its clock) | 1 |
| Processor Layout of the K5 | Vintage | 4 PVC sheets and 1 oscillator crystal (its clock) | 1 |
| Processor Layout of the P6 | Vintage | 6 PVC sheets and 1 oscillator crystal (its clock) | 1 |
| Processor Layout of the K6 | Vintage | 9 PVC sheets and 1 oscillator crystal (its clock) | 1 |
| Graphics Layout of the VGA | Vintage | 1 PVC sheet and 1 copper coil (the tube's deflection coils) | 1 |
| Graphics Layout of the Rendition | Vintage | 2 PVC sheets and 1 copper coil (the tube's deflection coils) | 1 |
| Graphics Layout of the 3dfx | Vintage | 3 PVC sheets and 1 copper coil (the tube's deflection coils) | 1 |
| Graphics Layout of the NV3 | Vintage | 4 PVC sheets and 1 copper coil (the tube's deflection coils) | 1 |
| Graphics Layout of the Rage | Vintage | 5 PVC sheets and 1 copper coil (the tube's deflection coils) | 1 |
| Graphics Layout of the Fahrenheit | Vintage | 7 PVC sheets and 1 copper coil (the tube's deflection coils) | 1 |

A Layout is the chip's drawing cut in PVC film, as real layouts were cut in film and photographed down.
A processor or graphics die takes 1 sheet for every million transistors it has, rounded so that a bigger die
always takes more: the Motoroller 68000 (68,000 transistors) 1, the 486 (1.2 million) 2, up to the K6 (8.8
million) 9.
