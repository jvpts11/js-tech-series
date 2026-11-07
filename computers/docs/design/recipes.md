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
| `LO` | Liquid Outlet | `St` | steel sheet | `P` | paper |
| `FD` | floppy disk | `So` | solder | `Pl` | planks of any wood |

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

### The hardware parts

Every part of the catalogue is made at the bench of its era by a rule for its kind, with its counts taken from
the part's own figures ([Catalogue](catalogue.md)). A part's contacts are plated with a tin nugget in the
Vintage and a gold one from the Legacy, and a part with a fan takes the case fan of its era (below).

#### The board's components

A board is assembled from components: a socket for each processor family, a slot for each memory generation
and each expansion bus, the storage and power connectors. Each is a plastic body (PVC in the Vintage and the
Legacy, polycarbonate from the Transition; a socket of 1,000 pins or more takes two, of 2,500 or more three),
its contacts (copper foil, 1 for every 100 pins; a power connector carries its current on copper wire), their
plating (a power connector's always tin), and what its mechanism needs: a ZIF socket's lever (1 steel rod), an
LGA socket's load plate and lever (1 steel plate, 2 above 3,000 pins, and 1 steel rod), a slot processor's two
rails and Quince's processor card connector (2 steel rods), a DIP socket nothing. Among the components made
of the same things, each with more pins takes at least one contact more than the one before it, era after era,
so no two are alike at any bench. Each is made at the bench of the era that first needs it:

| Component | Bench | Pins | Recipe | Gives |
| --- | --- | ---: | --- | ---: |
| AT Power Connector | Vintage | 12 | 1 PVC sheet, 1 copper wire and 1 tin nugget | 2 |
| 30-pin SIMM Slot | Vintage | 30 | 1 PVC sheet, 1 copper foil and 1 tin nugget | 4 |
| IDE Header | Vintage | 40 | 1 PVC sheet, 2 copper foils and 1 tin nugget | 4 |
| Quince DIP-64 Socket | Vintage | 64 | 1 PVC sheet, 8 copper foils and 1 tin nugget | 1 |
| 72-pin SIMM Slot | Vintage | 72 | 1 PVC sheet, 3 copper foils and 1 tin nugget | 4 |
| ISA Slot | Vintage | 98 | 1 PVC sheet, 4 copper foils and 1 tin nugget | 2 |
| PCI Slot | Vintage | 120 | 1 PVC sheet, 5 copper foils and 1 tin nugget | 2 |
| AGP Slot | Vintage | 132 | 1 PVC sheet, 6 copper foils and 1 tin nugget | 2 |
| SDRAM DIMM Slot | Vintage | 168 | 1 PVC sheet, 7 copper foils and 1 tin nugget | 4 |
| Socket 3 | Vintage | 237 | 1 PVC sheet, 3 copper foils, 1 tin nugget and 1 steel rod | 1 |
| Slot 1 | Vintage | 242 | 1 PVC sheet, 3 copper foils, 1 tin nugget and 2 steel rods | 1 |
| Socket 7 | Vintage | 321 | 1 PVC sheet, 4 copper foils, 1 tin nugget and 1 steel rod | 1 |
| Socket 8 | Vintage | 387 | 1 PVC sheet, 5 copper foils, 1 tin nugget and 1 steel rod | 1 |
| EPS 8-pin Connector | Legacy | 8 | 1 PVC sheet, 2 copper wires and 1 tin nugget | 2 |
| ATX 20-pin Connector | Legacy | 20 | 1 PVC sheet, 3 copper wires and 1 tin nugget | 2 |
| PCIe x16 Slot | Legacy | 164 | 1 PVC sheet, 2 copper foils and 1 gold nugget | 2 |
| DDR DIMM Slot | Legacy | 184 | 1 PVC sheet, 3 copper foils and 1 gold nugget | 4 |
| DDR2 DIMM Slot | Legacy | 240 | 1 PVC sheet, 4 copper foils and 1 gold nugget | 4 |
| Quince Processor Card Connector | Legacy | 300 | 1 PVC sheet, 3 copper foils, 1 gold nugget and 2 steel rods | 1 |
| Socket 370 | Legacy | 370 | 1 PVC sheet, 4 copper foils, 1 gold nugget and 1 steel rod | 1 |
| Socket A | Legacy | 462 | 1 PVC sheet, 5 copper foils, 1 gold nugget and 1 steel rod | 1 |
| Socket 478 | Legacy | 478 | 1 PVC sheet, 6 copper foils, 1 gold nugget and 1 steel rod | 1 |
| Socket 604 | Legacy | 604 | 1 PVC sheet, 7 copper foils, 1 gold nugget and 1 steel rod | 1 |
| Socket 754 | Legacy | 754 | 1 PVC sheet, 8 copper foils, 1 gold nugget and 1 steel rod | 1 |
| LGA 775 | Legacy | 775 | 1 PVC sheet, 8 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| Socket 939 | Legacy | 939 | 1 PVC sheet, 10 copper foils, 1 gold nugget and 1 steel rod | 1 |
| Socket 940 | Legacy | 940 | 1 PVC sheet, 11 copper foils, 1 gold nugget and 1 steel rod | 1 |
| SATA Port | Transition | 7 | 1 polycarbonate sheet, 1 copper foil and 1 gold nugget | 4 |
| ATX 24-pin Connector | Transition | 24 | 1 polycarbonate sheet, 1 copper wire and 1 tin nugget | 2 |
| PCIe x1 Slot | Transition | 36 | 1 polycarbonate sheet, 2 copper foils and 1 gold nugget | 2 |
| DDR3 DIMM Slot | Transition | 240 | 1 polycarbonate sheet, 3 copper foils and 1 gold nugget | 4 |
| LGA 771 | Transition | 771 | 1 polycarbonate sheet, 8 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| AM2 | Transition | 940 | 1 polycarbonate sheet, 10 copper foils, 1 gold nugget and 1 steel rod | 1 |
| AM3 | Transition | 941 | 1 polycarbonate sheet, 11 copper foils, 1 gold nugget and 1 steel rod | 1 |
| LGA 1156 | Transition | 1156 | 2 polycarbonate sheets, 12 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| Socket F | Transition | 1207 | 2 polycarbonate sheets, 13 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| LGA 1366 | Transition | 1366 | 2 polycarbonate sheets, 14 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| FM2+ | Standard | 906 | 1 polycarbonate sheet, 12 copper foils, 1 gold nugget and 1 steel rod | 1 |
| AM3+ | Standard | 942 | 1 polycarbonate sheet, 13 copper foils, 1 gold nugget and 1 steel rod | 1 |
| LGA 1150 | Standard | 1150 | 2 polycarbonate sheets, 15 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| LGA 1155 | Standard | 1155 | 2 polycarbonate sheets, 16 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| G34 | Standard | 1944 | 2 polycarbonate sheets, 20 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| LGA 2011 | Standard | 2011 | 2 polycarbonate sheets, 21 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| M.2 Slot | Advanced | 75 | 1 polycarbonate sheet, 4 copper foils and 1 gold nugget | 4 |
| DDR4 DIMM Slot | Advanced | 288 | 1 polycarbonate sheet, 5 copper foils and 1 gold nugget | 4 |
| DDR5 DIMM Slot | Advanced | 288 | 1 polycarbonate sheet, 6 copper foils and 1 gold nugget | 4 |
| LGA 1151 | Advanced | 1151 | 2 polycarbonate sheets, 22 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| LGA 1200 | Advanced | 1200 | 2 polycarbonate sheets, 23 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| AM4 | Advanced | 1331 | 2 polycarbonate sheets, 14 copper foils, 1 gold nugget and 1 steel rod | 1 |
| LGA 1700 | Advanced | 1700 | 2 polycarbonate sheets, 24 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| AM5 | Advanced | 1718 | 2 polycarbonate sheets, 25 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| LGA 1851 | Advanced | 1851 | 2 polycarbonate sheets, 26 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| LGA 2066 | Advanced | 2066 | 2 polycarbonate sheets, 27 copper foils, 1 gold nugget, 1 steel plate and 1 steel rod | 1 |
| Quince Q Socket | Advanced | 2600 | 3 polycarbonate sheets, 62 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |
| LGA 3647 | Advanced | 3647 | 3 polycarbonate sheets, 37 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |
| SP3 | Advanced | 4094 | 3 polycarbonate sheets, 42 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |
| sTR4 | Advanced | 4094 | 3 polycarbonate sheets, 41 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |
| LGA 4189 | Advanced | 4189 | 3 polycarbonate sheets, 43 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |
| LGA 4677 | Advanced | 4677 | 3 polycarbonate sheets, 47 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |
| sTR5 | Advanced | 4844 | 3 polycarbonate sheets, 49 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |
| SP5 | Advanced | 6096 | 3 polycarbonate sheets, 61 copper foils, 1 gold nugget, 2 steel plates and 1 steel rod | 1 |

#### The populated board

Every motherboard starts as its era's populated board: the etched board, the chipset (controller chips, as the
real chipset shrank), 1 oscillator crystal, 4 ceramic and 4 tantalum capacitors and 4 inductors.

| Era | Populated board |
| --- | --- |
| Vintage | 1 Vintage etched board, 4 Vintage controller chips, 1 oscillator crystal, 4 ceramic capacitors, 4 tantalum capacitors and 4 inductors |
| Legacy | 1 Legacy etched board, 2 Legacy controller chips, 1 oscillator crystal, 4 ceramic capacitors, 4 tantalum capacitors and 4 inductors |
| Transition | 1 Transition etched board, 2 Transition controller chips, 1 oscillator crystal, 4 ceramic capacitors, 4 tantalum capacitors and 4 inductors |
| Standard | 1 Standard etched board, 1 Standard controller chip, 1 oscillator crystal, 4 ceramic capacitors, 4 tantalum capacitors and 4 inductors |
| Advanced | 1 Advanced etched board, 1 Advanced controller chip, 1 oscillator crystal, 4 ceramic capacitors, 4 tantalum capacitors and 4 inductors |

#### Motherboards

A motherboard takes 1 populated board of its era (2 for EATX, EEB and MTX), its socket once for every
processor, its memory slots (a board of two generations has half of each, the newer one taking the odd
slot), its expansion slots (a board with an older bus as well has 2 of the older one), its storage
connectors (an IDE header for every two disks in the Vintage and the Legacy, a SATA port for each disk in the
Transition and the Standard, an M.2 slot for each in the Advanced) and its power connector (AT in the
Vintage, ATX 20-pin in the Legacy, ATX 24-pin after, and an EPS 8-pin for every processor on EEB and MTX
boards and on boards with more than one processor). It comes out without firmware. The three two-socket WS
boards ([Hardware](hardware.md)) follow the rule: 2 sockets, 16 DDR3 or DDR5 slots, 7 PCI Express slots, 6
disks.

The **mining boards** of the Mining Computer, the common desktop socket and memory of each era with its x1
slots, and the **risers** that carry a graphics card to an x1 slot (a small board with an x16 slot, the USB
cable and the adapter's gold edge):

| Part | Bench | Recipe |
| --- | --- | --- |
| MF Mining Board (Transition) | Transition | 1 Transition populated board, 1 LGA 775, 2 DDR2 DIMM Slots, 4 PCIe x1 Slots, 1 SATA Port and 1 ATX 24-pin Connector |
| MF Mining Board (Standard) | Standard | 1 Standard populated board, 1 LGA 1150, 2 DDR3 DIMM Slots, 6 PCIe x1 Slots, 1 SATA Port and 1 ATX 24-pin Connector |
| MF Mining Board (Advanced) | Advanced | 1 Advanced populated board, 1 LGA 1151, 2 DDR4 DIMM Slots, 8 PCIe x1 Slots, 1 M.2 Slot and 1 ATX 24-pin Connector |
| Mining Riser (Transition) | Transition | 1 Transition etched board, 1 PCIe x16 Slot, 2 copper wires, 1 PVC sheet and 1 gold nugget |
| Mining Riser (Standard) | Standard | 1 Standard etched board, 1 PCIe x16 Slot, 3 copper wires, 1 PVC sheet and 1 gold nugget |
| Mining Riser (Advanced) | Advanced | 1 Advanced etched board, 1 PCIe x16 Slot, 4 copper wires, 1 PVC sheet and 1 gold nugget |

#### Processors

A processor takes the processor chips of its era and microarchitecture, 1 for every 8 cores and at least 1
(a 64-core processor takes 8, as real chiplets do); among the processors of one era, socket and
microarchitecture, ranked by cores, then clock, then TDP, each takes at least one chip more than the one below
it, as a faster bin of the same die. Then a copper plate (its heat spreader), a nickel foil from the
Transition, its contacts' nugget and 1 socket of its family. Quince's processors follow the rule; a Q chip
also carries its memory in the package, an Advanced memory chip for every 4 GB:

| Processor | Era | Socket | Recipe |
| --- | --- | --- | --- |
| Motoroller 68000 | Vintage | Quince DIP-64 Socket | 1 Motoroller 68000 processor chip, 1 copper plate, 1 tin nugget and 1 Quince DIP-64 Socket |
| PowerQ G3 | Legacy | Quince Processor Card Connector | 1 PowerQ G3 processor chip, 1 copper plate, 1 gold nugget and 1 Quince Processor Card Connector |
| PowerQ G4 | Legacy | Quince Processor Card Connector | 1 PowerQ G4 processor chip, 1 copper plate, 1 gold nugget and 1 Quince Processor Card Connector |
| Quince Q1 | Advanced | Quince Q Socket | 1 Quince Q1 processor chip, 1 copper plate, 2 Advanced memory chips, 1 nickel foil, 1 gold nugget and 1 Quince Q Socket |
| Quince Q2 | Advanced | Quince Q Socket | 1 Quince Q2 processor chip, 1 copper plate, 4 Advanced memory chips, 1 nickel foil, 1 gold nugget and 1 Quince Q Socket |
| Quince Q3 | Advanced | Quince Q Socket | 1 Quince Q3 processor chip, 1 copper plate, 6 Advanced memory chips, 1 nickel foil, 1 gold nugget and 1 Quince Q Socket |

#### Graphics cards

A graphics card takes the graphics chips of its era and microarchitecture, 1 for the smallest and, ranked by
cores, then video memory, then TDP, at least one more for each bigger card of the same era and
microarchitecture; memory chips for its video memory, at least 1, each holding what the era's density
allowed (2 MB in the Vintage, 32 MB in the Legacy, 128 MB in the Transition, 1 GB in the Standard, 4 GB in
the Advanced); 2 tantalum and 4 ceramic capacitors, 2 inductors, 1 etched board, an aluminium plate as its
heatsink, a case fan of its era from the Legacy, and its contacts' nugget. The Phi coprocessors are
expansion cards, with the other cards.

#### Memory modules

8 memory chips of its era, and 4 more for each bigger module of the same era and generation (registered
modules ranked apart): the other side, then denser chips; the etched board; 1 solder, lead-free from the
Transition; its edge, as many copper foils as its slot has, and their plating; a registered module (RDIMM)
also its register, 1 controller chip.

#### Power supplies

1 transformer for every 300 W, and at least one more than the smaller supply of its era; 2 inductors, 4
tantalum capacitors, 1 controller chip of its era, 4 copper wires, 1 aluminium plate, 2 steel plates, a case
fan of its era and its era's power connector (AT, ATX 20-pin or ATX 24-pin).

#### Disks

- **A hard disk:** platters (1, 2, 3, 4 and 4 by era, and one more for each bigger disk of its era), a
  magnetic head for each platter, 1 electric motor, 1 controller chip of its era, 1 aluminium plate and its
  connector, an IDE header to the Legacy and a SATA port after. The Vintage's platters are the iron oxide
  ones ([Survival](survival.md)).
- **A solid-state disk:** 4 memory chips of its era (8 in the Advanced) and one more for each bigger one of its
  era and kind, 1 controller chip and 1 etched board; a SATA one in its aluminium case (1 aluminium plate) with
  its SATA port, an NVMe one a bare stick with its gold edge (1 gold nugget).

#### Sound cards

The expansion card of the spec: its chip (the era's controller chip, the sound processor), 4 ceramic
capacitors, 1 etched board, its edge (as many copper foils as its slot has) and their plating; a wavetable
card also its sample memory, 1 memory chip.

#### Drives inside the case

| Drive | Bench | Recipe |
| --- | --- | --- |
| Hot-Swap Drive Bay | Vintage | 2 steel plates, 1 Vintage etched board, 1 IDE Header and 1 PVC sheet |
| Internal CD Drive | Legacy | 1 electric motor, 1 glass lens, 1 gallium arsenide laser diode, 1 Legacy etched board and 1 IDE Header |
| Internal DVD Drive | Transition | 1 electric motor, 1 glass lens, 1 aluminium gallium indium phosphide laser diode, 1 Transition etched board and 1 SATA Port |
| Internal Blu-ray Drive | Advanced | 1 electric motor, 1 glass lens, 1 gallium nitride laser diode, 1 Advanced etched board and 1 SATA Port |

### Computers

#### The desk computers

A computer is made as its case, empty: its parts go in at its assembly window. A case is its metals and its
front: the era's etched board (the panel with the power switch and the lights), 2 copper wires and a bezel,
a PVC sheet in the Vintage and the Legacy, a polycarbonate sheet after. Each case gives the Personal Computer
of its look:

| Case | Era | Metals |
| --- | --- | --- |
| Beige AT minitower | Vintage | 6 steel plates |
| ATX tower | Legacy | 6 steel plates and 2 aluminium plates |
| Mid tower | Transition | 4 steel plates and 4 aluminium plates |
| Neutral | Standard | 4 steel plates and 4 aluminium plates |
| High Performance | Standard | 4 steel plates, 4 aluminium plates and 2 steel sheets |
| Aesthetic | Standard | 4 steel plates, 4 aluminium plates and 2 glass panes |
| Neutral | Advanced | 4 steel plates and 4 aluminium plates |
| High Performance | Advanced | 4 steel plates, 4 aluminium plates and 2 steel sheets |
| Aesthetic | Advanced | 4 steel plates, 4 aluminium plates and 2 glass panes |

The **Crafting Computer** is the Personal Computer of the same case with its era's Crafting Card in it for
good, and the **Cluster Management Computer** the same with its era's Cluster Interface Card (the
Transition's takes the Legacy's Management NIC, which its PCI Express slots take); the Advanced High
Performance case has no Cluster Management Computer. The **Mining Computer** is an open frame:

| Computer | Bench | Recipe |
| --- | --- | --- |
| Mining Computer (Transition) | Transition | 8 steel rods, 2 steel plates, 1 Transition etched board and 2 copper wires |
| Mining Computer (Standard) | Standard | 12 steel rods, 4 steel plates, 1 Standard etched board and 2 copper wires |
| Mining Computer (Advanced) | Advanced | 16 steel rods, 4 steel plates, 1 Advanced etched board and 2 copper wires |

#### Quince's machines

Quince's logic boards follow the motherboards' rule, with their own sockets and the numbers of the real
machines of each year; a Quince logic board takes only its era's processors ([Quince](quince.md)). Its
desk machines follow the cases' rule, with the real machine's materials and, in an all-in-one, the era's
screen:

| Logic board of | Bench | Recipe |
| --- | --- | --- |
| Russet 128K | Vintage | 1 Vintage populated board, 1 Quince DIP-64 Socket, 4 30-pin SIMM Slots, 1 IDE Header and 1 AT Power Connector |
| qRusset G3 | Legacy | 1 Legacy populated board, 1 Quince Processor Card Connector, 2 SDRAM DIMM Slots, 1 IDE Header and 1 ATX 20-pin Connector |
| Russet Power G4 | Legacy | 2 Legacy populated boards, 2 Quince Processor Card Connectors, 4 DDR DIMM Slots, 2 IDE Headers, 1 ATX 20-pin Connector, 2 EPS 8-pin Connectors, 1 AGP Slot and 4 PCI Slots |
| qServe (Legacy) | Legacy | 2 Legacy populated boards, 2 Quince Processor Card Connectors, 4 DDR DIMM Slots, 2 PCI Slots, 2 IDE Headers, 1 ATX 20-pin Connector and 2 EPS 8-pin Connectors |
| Russet Pro (Transition) | Transition | 2 Transition populated boards, 2 LGA 771s, 8 DDR2 DIMM Slots, 4 PCIe x16 Slots, 4 SATA Ports, 1 ATX 24-pin Connector and 2 EPS 8-pin Connectors |
| qRusset (white) | Transition | 1 Transition populated board, 1 LGA 775, 2 DDR2 DIMM Slots, 1 SATA Port and 1 ATX 24-pin Connector |
| qServe (Transition) | Transition | 2 Transition populated boards, 2 LGA 771s, 8 DDR2 DIMM Slots, 2 PCIe x16 Slots, 3 SATA Ports, 1 ATX 24-pin Connector and 2 EPS 8-pin Connectors |
| Quince Mainframe (Transition) | Transition | 2 Transition populated boards, 4 LGA 771s, 16 DDR2 DIMM Slots, 6 PCIe x16 Slots, 4 SATA Ports, 1 ATX 24-pin Connector and 4 EPS 8-pin Connectors |
| qRusset (thin) | Standard | 1 Standard populated board, 1 LGA 1155, 4 DDR3 DIMM Slots, 1 SATA Port and 1 ATX 24-pin Connector |
| Russet Pro (Standard) | Standard | 2 Standard populated boards, 1 LGA 2011, 4 DDR3 DIMM Slots, 2 PCIe x16 Slots, 1 SATA Port and 1 ATX 24-pin Connector |
| qServe (Standard) | Standard | 2 Standard populated boards, 2 LGA 2011s, 12 DDR3 DIMM Slots, 2 PCIe x16 Slots, 3 SATA Ports, 1 ATX 24-pin Connector and 2 EPS 8-pin Connectors |
| Quince Mainframe (Standard) | Standard | 2 Standard populated boards, 4 LGA 2011s, 32 DDR3 DIMM Slots, 6 PCIe x16 Slots, 4 SATA Ports, 1 ATX 24-pin Connector and 4 EPS 8-pin Connectors |
| Russet Studio | Advanced | 1 Advanced populated board, 1 Quince Q Socket, 2 DDR5 DIMM Slots, 1 M.2 Slot and 1 ATX 24-pin Connector |
| qRusset (coloured) | Advanced | 1 Advanced populated board, 1 Quince Q Socket, 1 M.2 Slot and 1 ATX 24-pin Connector |
| qServe (Advanced) | Advanced | 2 Advanced populated boards, 1 Quince Q Socket, 8 DDR5 DIMM Slots, 2 PCIe x16 Slots, 4 M.2 Slots and 1 ATX 24-pin Connector |
| Quince Mainframe (Advanced) | Advanced | 2 Advanced populated boards, 4 Quince Q Sockets, 32 DDR5 DIMM Slots, 6 PCIe x16 Slots, 4 M.2 Slots, 1 ATX 24-pin Connector and 4 EPS 8-pin Connectors |

| Machine | Bench | Recipe |
| --- | --- | --- |
| Russet 128K | Vintage | 5 PVC sheets, 1 tube screen, 1 Vintage etched board and 2 copper wires |
| qRusset G3 | Legacy | 4 polycarbonate sheets, 1 tube screen, 1 Legacy etched board, 2 copper wires and 1 PVC sheet |
| Russet Power G4 | Legacy | 4 steel plates, 2 polycarbonate sheets, 1 Legacy etched board, 2 copper wires and 1 PVC sheet |
| Russet Pro (Transition) | Transition | 6 aluminium plates, 2 aluminium sheets, 1 Transition etched board, 2 copper wires and 1 polycarbonate sheet |
| qRusset (white) | Transition | 5 polycarbonate sheets, 1 liquid crystal screen, 1 Transition etched board and 2 copper wires |
| qRusset (thin) | Standard | 4 aluminium plates, 1 LED screen, 1 glass pane, 1 Standard etched board, 2 copper wires and 1 polycarbonate sheet |
| Russet Pro (Standard) | Standard | 4 aluminium plates, 2 steel rods, 1 Standard etched board, 2 copper wires and 1 polycarbonate sheet |
| Russet Studio | Advanced | 6 aluminium plates, 1 Advanced etched board, 2 copper wires and 1 polycarbonate sheet |
| qRusset (coloured) | Advanced | 4 aluminium plates, 1 self-lit panel, 1 glass pane, 1 Advanced etched board, 2 copper wires and 1 polycarbonate sheet |

#### The server room

A **server chassis** is 2 steel plates for every U, an aluminium front, the desk cases' front, and the
connectors of the drives it takes from the rack's bays (its backplane: an IDE header for every two drives to
the Legacy, a SATA port for each after). A **server** is its chassis with its fans (a case fan of its era for
every U, two) and its rails (2 steel rods). The drives each chassis takes:

| Chassis | U | Vintage | Legacy | Transition | Standard | Advanced |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Server | 1 | 1 | 2 | 4 | 3 | 4 |
| Storage Server | 2 | 4 | 4 | 6 | 8 | 10 |
| Compute Server | 2 | 1 | 1 | 1 | 1 | 1 |

The rest of the room, each at its era's bench:

| Block | Bench | Recipe | What it is |
| --- | --- | --- | --- |
| Supercomputer Node (Standard) | Standard | 4 steel plates, 1 aluminium plate, 1 Standard etched board, 2 copper wires, 1 polycarbonate sheet, 1 SATA Port, 4 case fans of its era and 2 steel rods | a 2U node, its box, front, bays, fans and rails at once |
| Supercomputer Node (Advanced) | Advanced | 8 steel plates, 1 aluminium plate, 1 Advanced etched board, 2 copper wires, 1 polycarbonate sheet, 2 SATA Ports, 4 case fans of its era and 2 steel rods | its four sleds take a steel plate each |
| Server Rack (Vintage) | Vintage | 12 steel plates, 4 steel rods, 2 case fans of its era, 1 Vintage etched board and 8 IDE Headers | the spec's 12 steel plates, its rails, its fans and the backplane of its 8U |
| Server Rack (Legacy) | Legacy | 12 steel plates, 4 steel rods, 2 case fans of its era, 1 Legacy etched board and 8 IDE Headers | the spec's 12 steel plates, its rails, its fans and the backplane of its 8U |
| Server Rack (Transition) | Transition | 12 steel plates, 4 steel rods, 2 case fans of its era, 1 Transition etched board and 8 SATA Ports | the spec's 12 steel plates, its rails, its fans and the backplane of its 8U |
| Server Rack (Standard) | Standard | 12 steel plates, 4 steel rods, 2 case fans of its era, 1 Standard etched board and 8 SATA Ports | the spec's 12 steel plates, its rails, its fans and the backplane of its 8U |
| Server Rack (Advanced) | Advanced | 12 steel plates, 4 steel rods, 2 case fans of its era, 1 Advanced etched board and 8 SATA Ports | the spec's 12 steel plates, its rails, its fans and the backplane of its 8U |
| Supercomputer Rack (Standard) | Standard | 12 steel plates, 4 steel rods, 4 case fans of its era, 1 Standard etched board, 8 SATA Ports, 4 LEDs and 2 copper pipes | the Server Rack with its light bar and its coolant inlet and outlet |
| Supercomputer Rack (Advanced) | Advanced | 12 steel plates, 4 steel rods, 4 case fans of its era, 1 Advanced etched board, 8 SATA Ports, 4 LEDs and 2 copper pipes | the Server Rack with its light bar and its coolant inlet and outlet |
| KVM Switch (Vintage) | Vintage | 2 steel plates, 1 Vintage etched board, 1 Vintage controller chip and 4 copper wires | its board, its controller and four ports |
| KVM Switch (Legacy) | Legacy | 2 steel plates, 1 Legacy etched board, 1 Legacy controller chip and 4 copper wires | its board, its controller and four ports |
| KVM Switch (Transition) | Transition | 2 steel plates, 1 Transition etched board, 1 Transition controller chip and 4 copper wires | its board, its controller and four ports |
| KVM Switch (Standard) | Standard | 2 steel plates, 1 Standard etched board, 1 Standard controller chip and 4 copper wires | its board, its controller and four ports |
| KVM Switch (Advanced) | Advanced | 2 steel plates, 1 Advanced etched board, 1 Advanced controller chip and 4 copper wires | its board, its controller and four ports |
| Tape Library (Vintage) | Vintage | 4 steel plates, 2 electric motors, 2 magnetic heads, 1 Vintage etched board and 2 Vintage controller chips | its robot and its drives (2U) |
| Tape Library (Legacy) | Legacy | 4 steel plates, 2 electric motors, 2 magnetic heads, 1 Legacy etched board and 2 Legacy controller chips | its robot and its drives (2U) |
| Tape Library (Transition) | Transition | 4 steel plates, 2 electric motors, 2 magnetic heads, 1 Transition etched board and 2 Transition controller chips | its robot and its drives (2U) |
| Tape Library (Standard) | Standard | 4 steel plates, 2 electric motors, 2 magnetic heads, 1 Standard etched board and 2 Standard controller chips | its robot and its drives (2U) |
| Tape Library (Advanced) | Advanced | 4 steel plates, 2 electric motors, 2 magnetic heads, 1 Advanced etched board and 2 Advanced controller chips | its robot and its drives (2U) |
| Fibre Optic Interface (Standard) | Standard | 2 steel plates, 1 Standard etched board, 1 Standard controller chip, 2 gallium arsenide laser diodes and 2 optical fibres | its transceivers |
| Fibre Optic Interface (Advanced) | Advanced | 2 steel plates, 1 Advanced etched board, 1 Advanced controller chip, 2 gallium arsenide laser diodes and 4 optical fibres | its transceivers |
| Drive Shelf (JBOD) | Standard | 4 steel plates, 1 Standard etched board, 10 SATA Ports and 2 case fans of its era | ten drive bays (2U) |
| Mainframe (Vintage) | Vintage | 12 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Vintage etched boards and 8 copper wires | placed whole (3x2x2) |
| Mainframe (Legacy) | Legacy | 12 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Legacy etched boards and 8 copper wires | placed whole (3x2x2) |
| Mainframe (Transition) | Transition | 12 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Transition etched boards and 8 copper wires | placed whole (3x2x2) |
| Mainframe (Standard) | Standard | 12 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Standard etched boards and 8 copper wires | placed whole (3x2x2) |
| Mainframe (Advanced) | Advanced | 12 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Advanced etched boards and 8 copper wires | placed whole (3x2x2) |
| WAN Gateway Computer (Vintage) | Vintage | 16 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Vintage etched boards, 8 copper wires and 4 Vintage controller chips | placed whole (4x2x2), its four line ports |
| WAN Gateway Computer (Legacy) | Legacy | 16 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Legacy etched boards, 8 copper wires and 4 Legacy controller chips | placed whole (4x2x2), its four line ports |
| WAN Gateway Computer (Transition) | Transition | 16 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Transition etched boards, 8 copper wires and 4 Transition controller chips | placed whole (4x2x2), its four line ports |
| WAN Gateway Computer (Standard) | Standard | 16 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Standard etched boards, 8 copper wires and 4 Standard controller chips | placed whole (4x2x2), its four line ports |
| WAN Gateway Computer (Advanced) | Advanced | 16 steel plates, 4 aluminium plates, 4 case fans of its era, 2 Advanced etched boards, 8 copper wires and 4 Advanced controller chips | placed whole (4x2x2), its four line ports |
| Quince Mainframe (Transition) | Transition | 16 aluminium plates, 4 case fans of its era, 2 Transition etched boards, 8 copper wires and 2 glass panes | aluminium and glass |
| Quince Mainframe (Standard) | Standard | 16 aluminium plates, 4 case fans of its era, 2 Standard etched boards, 8 copper wires and 2 glass panes | aluminium and glass |
| Quince Mainframe (Advanced) | Advanced | 16 aluminium plates, 4 case fans of its era, 2 Advanced etched boards, 8 copper wires and 2 glass panes | aluminium and glass |
| qServe (Legacy) | Legacy | 3 aluminium plates, 1 Legacy etched board, 2 copper wires, 1 PVC sheet, 2 IDE Headers, 2 case fans of its era and 2 steel rods | a 1U of aluminium |
| qServe (Transition) | Transition | 3 aluminium plates, 1 Transition etched board, 2 copper wires, 1 polycarbonate sheet, 3 SATA Ports, 2 case fans of its era and 2 steel rods | a 1U of aluminium |
| qServe (Standard) | Standard | 3 aluminium plates, 1 Standard etched board, 2 copper wires, 1 polycarbonate sheet, 3 SATA Ports, 2 case fans of its era and 2 steel rods | a 1U of aluminium |
| qServe (Advanced) | Advanced | 3 aluminium plates, 1 Advanced etched board, 2 copper wires, 1 polycarbonate sheet, 4 SATA Ports, 2 case fans of its era and 2 steel rods | a 1U of aluminium |
| HBW Interface (Standard) | Standard | 2 steel plates, 1 Standard etched board, 2 Standard controller chips, 6 PCIe x16 Slots, 2 gallium arsenide laser diodes and 2 optical fibres | six coprocessor slots and the High compute line's transceivers |
| HBW Interface (Advanced) | Advanced | 2 steel plates, 1 Advanced etched board, 2 Advanced controller chips, 6 PCIe x16 Slots, 2 gallium arsenide laser diodes and 2 optical fibres | six coprocessor slots and the High compute line's transceivers |

The Rack UPS, the UPS Battery Expansion, the PDU, the Cooling Unit and the Vent Panel are with power and
cooling.

### Peripherals and media

Each at the bench of its era.

#### Monitors

The Display Line's screen of its era, an etched board and a case of 2 PVC sheets (Vintage) or 2
polycarbonate. The Vintage tubes differ by their phosphor, as the real ones did: white (the tube's own zinc
sulfide), green (doped with copper), amber (with manganese), and the CGA's colours (both, behind a steel
shadow mask), which the Legacy's colour tube also has. A touch monitor is the monitor with its touch layer
(an indium tin oxide foil), its cover glass and its touch controller.

| Item | Bench | Recipe |
| --- | --- | --- |
| Mono I Monitor | Vintage | 1 tube screen, 1 Vintage etched board and 2 PVC sheets |
| Mono II Monitor | Vintage | 1 tube screen, 1 Vintage etched board, 2 PVC sheets and 1 copper nugget |
| Amber Monitor | Vintage | 1 tube screen, 1 Vintage etched board, 2 PVC sheets and 1 manganese dust |
| CGA Monitor | Vintage | 1 tube screen, 1 Vintage etched board, 2 PVC sheets, 1 steel sheet, 1 copper nugget and 1 manganese dust |
| Legacy Monitor | Legacy | 1 tube screen, 1 Legacy etched board, 2 polycarbonate sheets, 1 steel sheet, 1 copper nugget and 1 manganese dust |
| Transition Monitor | Transition | 1 liquid crystal screen, 1 Transition etched board and 2 polycarbonate sheets |
| Monitor | Standard | 1 LED screen, 1 Standard etched board and 2 polycarbonate sheets |
| Touch Monitor | Standard | 1 Monitor, 1 indium tin oxide foil, 1 glass pane and 1 Standard controller chip |
| Color Monitor | Advanced | 1 self-lit panel, 1 Advanced etched board and 2 polycarbonate sheets |
| Advanced Touch Monitor | Advanced | 1 Color Monitor, 1 indium tin oxide foil, 1 glass pane and 1 Advanced controller chip |

#### Printers

Each the printer of its kind: the dot matrix's 9-pin head (solenoids, copper coils) on its carriage rails;
the inkjets' heads (a controller chip each) and the photo printer's little screen; the laser printer's
laser, its drum (an aluminium plate) and its fuser's heater (a copper coil); the ink tank printer's tanks.
The Large Scale Printer's mechanical blocks come from the Crafting Table, its Print Unit from the bench.

| Item | Bench | Recipe |
| --- | --- | --- |
| Epsilon FX-80 | Vintage | 2 PVC sheets, 2 electric motors, 2 copper coils, 2 steel rods and 1 Vintage etched board |
| Pakard DeskJot 940 | Legacy | 2 polycarbonate sheets, 2 electric motors, 1 Legacy controller chip, 1 steel rod and 1 Legacy etched board |
| Large Scale Printer: Print Unit | Legacy | 4 steel plates, 1 Legacy etched board, 4 Legacy controller chips and 1 electric motor |
| Pakard FotoSmart C4280 | Transition | 2 polycarbonate sheets, 2 electric motors, 2 Transition controller chips, 1 liquid crystal screen and 1 Transition etched board |
| Pakard LaserJot 1102 | Standard | 2 polycarbonate sheets, 2 electric motors, 1 gallium arsenide laser diode, 1 aluminium plate, 1 copper coil and 1 Standard etched board |
| Epsilon EcoTonk ET-2720 | Advanced | 3 polycarbonate sheets, 2 electric motors, 1 Advanced controller chip, 1 LED and 1 Advanced etched board |

| Block | Recipe | What it is |
| --- | --- | --- |
| Paper Feeder | `SP SP SP / SR M SR / SP SP SP` | the rollers and the motor that pulls the paper |
| Inking Unit | `PVC PVC PVC / SR M SR / SP SP SP` | the ink tanks (PVC), the pump and the rollers |
| Perfect Binder | `SP Co SP / SR M SR / SP SP SP` | the heated glue pot (a coil) and the press |

#### Drives, the Dock Station and the scanner

A drive is the spec's (optical: a motor, a lens, the laser diode of its era, an etched board; floppy: a
motor, a magnetic head, an etched board) in a case of its own; the DVD Drive is the Transition's. The Dock
Station's trays are a SATA port for each disk size and an M.2 slot, with its USB bridge. The scanner has its
glass, a lens, its sensor and controller (2 controller chips), the carriage's motor, its lamp (a cold
cathode lamp, an LED from the Standard), its board and its box; the Vinyl RW Drive its platter (a steel
plate) and motor and its cutting head (a magnetic head driven by a copper coil); the Extended CD Drive, as the
real duplicator towers, its eight drives (one reads, seven write) and its little screen.

| Item | Bench | Recipe |
| --- | --- | --- |
| Floppy Drive | Vintage | 1 electric motor, 1 magnetic head, 1 Vintage etched board and 1 PVC sheet |
| Scanner (Vintage) | Vintage | 1 glass pane, 1 glass lens, 2 Vintage controller chips, 1 electric motor, 1 cold cathode lamp, 1 Vintage etched board and 1 PVC sheet |
| Vinyl RW Drive (Vintage) | Vintage | 1 steel plate, 1 electric motor, 1 magnetic head, 1 copper coil, 1 Vintage etched board and 1 PVC sheet |
| CD Drive | Legacy | 1 electric motor, 1 glass lens, 1 gallium arsenide laser diode, 1 Legacy etched board and 1 polycarbonate sheet |
| Scanner (Legacy) | Legacy | 1 glass pane, 1 glass lens, 2 Legacy controller chips, 1 electric motor, 1 cold cathode lamp, 1 Legacy etched board and 1 polycarbonate sheet |
| Vinyl RW Drive (Legacy) | Legacy | 1 steel plate, 1 electric motor, 1 magnetic head, 1 copper coil, 1 Legacy etched board and 1 polycarbonate sheet |
| Extended CD Drive (Legacy) | Legacy | 8 Internal CD Drives, 4 steel plates, 1 Legacy etched board and 2 Legacy controller chips |
| DVD Drive | Transition | 1 electric motor, 1 glass lens, 1 aluminium gallium indium phosphide laser diode, 1 Transition etched board and 1 polycarbonate sheet |
| Scanner (Transition) | Transition | 1 glass pane, 1 glass lens, 2 Transition controller chips, 1 electric motor, 1 cold cathode lamp, 1 Transition etched board and 1 polycarbonate sheet |
| Vinyl RW Drive (Transition) | Transition | 1 steel plate, 1 electric motor, 1 magnetic head, 1 copper coil, 1 Transition etched board and 1 polycarbonate sheet |
| Extended CD Drive (Transition) | Transition | 8 Internal DVD Drives, 4 steel plates, 1 Transition etched board, 2 Transition controller chips and 1 liquid crystal screen |
| Dock Station | Standard | 2 polycarbonate sheets, 1 Standard etched board, 1 Standard controller chip, 2 SATA Ports and 1 M.2 Slot |
| Scanner (Standard) | Standard | 1 glass pane, 1 glass lens, 2 Standard controller chips, 1 electric motor, 1 LED, 1 Standard etched board and 1 polycarbonate sheet |
| Vinyl RW Drive (Standard) | Standard | 1 steel plate, 1 electric motor, 1 magnetic head, 1 copper coil, 1 Standard etched board and 1 polycarbonate sheet |
| Blu-ray Drive | Advanced | 1 electric motor, 1 glass lens, 1 gallium nitride laser diode, 1 Advanced etched board and 1 polycarbonate sheet |
| Scanner (Advanced) | Advanced | 1 glass pane, 1 glass lens, 2 Advanced controller chips, 1 electric motor, 1 LED, 1 Advanced etched board and 1 polycarbonate sheet |
| Vinyl RW Drive (Advanced) | Advanced | 1 steel plate, 1 electric motor, 1 magnetic head, 1 copper coil, 1 Advanced etched board and 1 polycarbonate sheet |
| Extended CD Drive (Advanced) | Advanced | 8 Internal Blu-ray Drives, 4 steel plates, 1 Advanced etched board, 2 Advanced controller chips and 1 LED screen |

#### Speakers, hubs and the devices of the network

A speaker: its voice coil (a copper coil), its magnet (ferrite, as real speaker magnets are), its paper
cone and its box; a powered one its amplifier (the era's etched board). The Redstone Interface: redstone,
the lens of the face it reads through, its board and box. A hub: the Vintage's switch box, then the hub
chip and a copper wire for each port it adds. A Pattern Encoder: its board and controller and the drive of
its era's writable medium (the floppy drive, the internal CD, DVD and Blu-ray drives; the USB port, a copper
foil and its controller, in the Standard).

| Item | Bench | Recipe |
| --- | --- | --- |
| Redstone Interface (Vintage) | Vintage | 1 redstone, 1 glass lens, 1 Vintage etched board and 1 PVC sheet |
| Vintage Hub (switch box) | Vintage | 1 steel plate, 1 steel rod, 2 copper wires and 1 Vintage etched board |
| Pattern Encoder (Vintage) | Vintage | 1 Vintage etched board, 1 Vintage controller chip, 2 PVC sheets and 1 Floppy Drive |
| Network Gateway | Vintage | 1 Vintage etched board, 1 Vintage controller chip, 2 PVC sheets and 2 copper wires |
| Artisan ToneWorks | Legacy | 1 copper coil, 1 ferrite, 1 paper and 1 polycarbonate sheet |
| Redstone Interface (Legacy) | Legacy | 1 redstone, 1 glass lens, 1 Legacy etched board and 1 polycarbonate sheet |
| Hub (Legacy) | Legacy | 1 polycarbonate sheet, 1 Legacy etched board, 1 Legacy controller chip and 4 copper wires |
| Pattern Encoder (Legacy) | Legacy | 1 Legacy etched board, 1 Legacy controller chip, 2 polycarbonate sheets and 1 Internal CD Drive |
| Artisan Inspira 2.1 | Transition | 1 copper coil, 1 ferrite, 1 paper, 1 polycarbonate sheet and 2 copper wires |
| Artisan Inspira 2.1 Subwoofer | Transition | 2 copper coils, 2 ferrites, 2 paper, 2 polycarbonate sheets and 1 Transition etched board |
| Redstone Interface (Transition) | Transition | 1 redstone, 1 glass lens, 1 Transition etched board and 1 polycarbonate sheet |
| Hub (Transition) | Transition | 1 polycarbonate sheet, 1 Transition etched board, 1 Transition controller chip and 4 copper wires |
| Pattern Encoder (Transition) | Transition | 1 Transition etched board, 1 Transition controller chip, 2 polycarbonate sheets and 1 Internal DVD Drive |
| Artisan WattWorks T20 | Standard | 1 copper coil, 1 ferrite, 1 paper, 1 aluminium plate and 1 Standard etched board |
| Redstone Interface (Standard) | Standard | 1 redstone, 1 glass lens, 1 Standard etched board and 1 polycarbonate sheet |
| Hub (Standard) | Standard | 1 polycarbonate sheet, 1 Standard etched board, 1 Standard controller chip and 7 copper wires |
| Pattern Encoder (Standard) | Standard | 1 Standard etched board, 2 Standard controller chips, 2 polycarbonate sheets and 1 copper foil |
| Artisan Cobble | Advanced | 1 copper coil, 1 ferrite, 1 paper, 1 polycarbonate sheet and 1 Advanced etched board |
| Redstone Interface (Advanced) | Advanced | 1 redstone, 1 glass lens, 1 Advanced etched board and 1 polycarbonate sheet |
| Hub (Advanced) | Advanced | 1 aluminium plate, 1 Advanced etched board, 1 Advanced controller chip and 7 copper wires |
| Pattern Encoder (Advanced) | Advanced | 1 Advanced etched board, 1 Advanced controller chip, 2 polycarbonate sheets and 1 Internal Blu-ray Drive |

#### Writable media

The floppy disk is its film in a PVC case. A rewritable disc is the blank disc and its reflector, with the
phase-change layer the real ones have (silver for the CD-RW's, silver and indium for the DVD-RW's, germanium
for the BD-RE's); a DVD is two discs bonded. A USB stick is memory chips by its size, its controller, board,
case and plug. A tape is its film (PVC) and its magnetic coat (iron oxide on the reel and the DLT, metal
particles on the LTO-3, barium ferrite after) in a cartridge with, from the LTO, its memory chip. The blank
vinyl is PVC, as vinyl is, with its paper label. **Media pressed with their content** (a CD-ROM, a DVD-ROM, a
BD-ROM, an installer disc) **are never crafted**: they are found, bought and copied onto the blank media
below ([Survival](survival.md)).

| Item | Bench | Recipe |
| --- | --- | --- |
| Floppy Disk | Vintage | 1 floppy film and 1 PVC sheet |
| Tape Reel | Vintage | 2 PVC sheets and 1 iron dust |
| Blank Vinyl Record | Vintage | 2 PVC sheets and 1 paper |
| CD-RW | Legacy | 1 blank disc, 1 aluminium foil and 1 silver dust |
| DLT Tape | Legacy | 2 PVC sheets, 1 iron dust and 1 polycarbonate sheet |
| DVD-RW | Transition | 2 blank discs, 1 aluminium foil, 1 silver dust and 1 indium dust |
| LTO-3 Tape | Transition | 2 PVC sheets, 2 iron dust, 1 polycarbonate sheet and 1 Transition memory chip |
| Vaultis Pocket 8G | Standard | 1 Standard memory chip, 1 Standard controller chip, 1 Standard etched board, 1 polycarbonate sheet and 1 copper foil |
| Vaultis Pocket 16G | Standard | 2 Standard memory chips, 1 Standard controller chip, 1 Standard etched board, 1 polycarbonate sheet and 1 copper foil |
| Vaultis Pocket 32G (the stick of today) | Standard | 4 Standard memory chips, 1 Standard controller chip, 1 Standard etched board, 1 polycarbonate sheet and 2 copper foils |
| LTO-6 Tape | Standard | 2 PVC sheets, 1 ferrite, 1 polycarbonate sheet and 1 Standard memory chip |
| BD-RE | Advanced | 1 blank disc, 1 silver foil and 1 germanium dust |
| Vaultis Pocket 64G | Advanced | 1 Advanced memory chip, 1 Advanced controller chip, 1 Advanced etched board, 1 aluminium plate and 2 copper foils |
| LTO-9 Tape | Advanced | 2 PVC sheets, 2 ferrites, 1 polycarbonate sheet and 1 Advanced memory chip |

### The network

#### Cables

Every cable recipe gives 8. Each is what the real one has: the coaxials' braid (copper foil), more copper
in the later twisted pairs (their twists are tighter, more copper to the metre), the shielded ones' foil,
the long lines' armour (a steel rod), the direct-attach copper of the OSFP and the gold of its plugs. The
crafting cable carries redstone, the line's signal; a peripheral cable takes its era's plugs, tin in the
Vintage and gold after.

| Item | Bench | Recipe |
| --- | --- | --- |
| Thin Coaxial Cable | Vintage | 2 copper wires, 2 copper foils and 1 PVC sheet |
| Thick Coaxial Cable | Vintage | 4 copper wires, 4 copper foils and 2 PVC sheets |
| Telephone Line | Vintage | 2 copper wires and 1 PVC sheet |
| Crafting Cable | Vintage | 3 copper wires, 1 PVC sheet and 1 redstone |
| Vintage Peripheral Cable (DE-9, DB-25) | Vintage | 4 copper wires, 1 PVC sheet and 1 tin nugget |
| Ethernet Cable | Legacy | 4 copper wires and 1 PVC sheet |
| HBW Cable | Legacy | 10 copper wires, 2 copper foils and 2 PVC sheets |
| Leased Line | Legacy | 4 copper wires, 1 PVC sheet and 1 copper foil |
| Legacy Peripheral Cable (VGA, USB) | Legacy | 4 copper wires, 1 PVC sheet and 1 gold nugget |
| Cat 5e Cable | Transition | 5 copper wires and 1 PVC sheet |
| 10GBASE-CX4 Cable | Transition | 8 copper wires, 2 copper foils and 1 PVC sheet |
| T3 Line | Transition | 1 copper wire, 2 copper foils and 1 PVC sheet |
| InfiniBand Cable | Transition | 8 copper wires, 4 copper foils and 1 PVC sheet |
| Transition Peripheral Cable (DVI, USB) | Transition | 5 copper wires, 1 PVC sheet and 1 gold nugget |
| Gigabit Ethernet Cable | Standard | 6 copper wires and 1 PVC sheet |
| Fibre Optic Cable | Standard | 4 optical fibres and 1 PVC sheet |
| VLDC Cable | Standard | 4 optical fibres, 1 PVC sheet and 1 steel rod |
| High Compute Cable | Standard | 8 optical fibres and 1 PVC sheet |
| Peripheral Cable (HDMI, USB 3) | Standard | 6 copper wires, 1 PVC sheet, 1 gold nugget and 1 copper foil |
| Cat 6a Cable | Advanced | 6 copper wires, 1 PVC sheet and 1 copper foil |
| OM5 Fibre Cable | Advanced | 6 optical fibres and 1 PVC sheet |
| Dark Fibre Cable | Advanced | 8 optical fibres, 1 PVC sheet and 1 steel rod |
| OSFP Cable | Advanced | 16 copper wires, 8 copper foils, 2 PVC sheets and 1 gold nugget |
| Advanced Peripheral Cable (DisplayPort, USB-C) | Advanced | 6 copper wires, 1 PVC sheet and 2 gold nuggets |

#### Routers, repeaters and the Server Router

A router is its box, its board, 2 controller chips, a memory chip (the routing table) and its ports (copper
wires); an optical router is of aluminium, with the fibre's lasers and the fibre; a repeater one controller and
two ports. A Server Router is a steel box with its board, the table's memory, its six faces' ports and one
controller for every 4 racks it serves (4 in the Legacy, 8 in the Transition, 16 in the Standard, 32 in the
Advanced).

| Item | Bench | Recipe |
| --- | --- | --- |
| Router (Vintage) | Vintage | 1 PVC sheet, 1 Vintage etched board, 2 Vintage controller chips, 1 Vintage memory chip and 4 copper wires |
| Repeater (Vintage) | Vintage | 1 PVC sheet, 1 Vintage etched board, 1 Vintage controller chip and 2 copper wires |
| Router (Legacy) | Legacy | 1 polycarbonate sheet, 1 Legacy etched board, 2 Legacy controller chips, 1 Legacy memory chip and 4 copper wires |
| Repeater (Legacy) | Legacy | 1 polycarbonate sheet, 1 Legacy etched board, 1 Legacy controller chip and 2 copper wires |
| Server Router (Legacy) | Legacy | 2 steel plates, 1 Legacy etched board, 1 Legacy controller chip, 1 Legacy memory chip and 6 copper wires |
| Router (Transition) | Transition | 1 polycarbonate sheet, 1 Transition etched board, 2 Transition controller chips, 1 Transition memory chip and 4 copper wires |
| Repeater (Transition) | Transition | 1 polycarbonate sheet, 1 Transition etched board, 1 Transition controller chip and 2 copper wires |
| Server Router (Transition) | Transition | 2 steel plates, 1 Transition etched board, 2 Transition controller chips, 1 Transition memory chip and 6 copper wires |
| Router (Standard) | Standard | 1 polycarbonate sheet, 1 Standard etched board, 2 Standard controller chips, 1 Standard memory chip and 4 copper wires |
| Repeater (Standard) | Standard | 1 polycarbonate sheet, 1 Standard etched board, 1 Standard controller chip and 2 copper wires |
| Optical Router (Standard) | Standard | 1 aluminium plate, 1 Standard etched board, 2 Standard controller chips, 1 Standard memory chip, 2 gallium arsenide laser diodes and 2 optical fibres |
| Server Router (Standard) | Standard | 2 steel plates, 1 Standard etched board, 4 Standard controller chips, 1 Standard memory chip and 6 copper wires |
| Router (Advanced) | Advanced | 1 polycarbonate sheet, 1 Advanced etched board, 2 Advanced controller chips, 1 Advanced memory chip and 4 copper wires |
| Repeater (Advanced) | Advanced | 1 polycarbonate sheet, 1 Advanced etched board, 1 Advanced controller chip and 2 copper wires |
| Optical Router (Advanced) | Advanced | 1 aluminium plate, 1 Advanced etched board, 2 Advanced controller chips, 1 Advanced memory chip, 2 gallium arsenide laser diodes and 2 optical fibres |
| Server Router (Advanced) | Advanced | 2 steel plates, 1 Advanced etched board, 8 Advanced controller chips, 1 Advanced memory chip and 6 copper wires |

#### Storage and the crafting network

Each with its box, its board and its controller: the Import Bus pulls with a motor; the Export Bus pushes with
a motor and its ejector (a steel rod); the External Storage Bus reads an inventory as a disk (the era's drive
connector); the Crafting Interface keeps its machine's patterns, a memory chip for every 3 patterns it holds.
The Crafting Input Router and the Receiving Bus serve every era and are made once, in the Vintage.

| Item | Bench | Recipe |
| --- | --- | --- |
| Import Bus (Vintage) | Vintage | 1 PVC sheet, 1 Vintage etched board, 1 Vintage controller chip and 1 electric motor |
| Export Bus (Vintage) | Vintage | 1 PVC sheet, 1 Vintage etched board, 1 Vintage controller chip, 1 electric motor and 1 steel rod |
| External Storage Bus (Vintage) | Vintage | 1 PVC sheet, 1 Vintage etched board, 1 Vintage controller chip and 1 IDE Header |
| Crafting Interface (Vintage) | Vintage | 1 PVC sheet, 1 Vintage etched board, 1 Vintage controller chip and 1 Vintage memory chip |
| Crafting Input Router | Vintage | 1 PVC sheet, 1 Vintage etched board, 1 Vintage controller chip, 1 steel rod and 1 redstone |
| Crafting Receiving Bus | Vintage | 1 PVC sheet, 1 Vintage etched board, 1 Vintage controller chip, 1 electric motor and 1 redstone |
| Import Bus (Legacy) | Legacy | 1 polycarbonate sheet, 1 Legacy etched board, 1 Legacy controller chip and 1 electric motor |
| Export Bus (Legacy) | Legacy | 1 polycarbonate sheet, 1 Legacy etched board, 1 Legacy controller chip, 1 electric motor and 1 steel rod |
| External Storage Bus (Legacy) | Legacy | 1 polycarbonate sheet, 1 Legacy etched board, 1 Legacy controller chip and 1 IDE Header |
| Crafting Interface (Legacy) | Legacy | 1 polycarbonate sheet, 1 Legacy etched board, 1 Legacy controller chip and 2 Legacy memory chips |
| Import Bus (Transition) | Transition | 1 polycarbonate sheet, 1 Transition etched board, 1 Transition controller chip and 1 electric motor |
| Export Bus (Transition) | Transition | 1 polycarbonate sheet, 1 Transition etched board, 1 Transition controller chip, 1 electric motor and 1 steel rod |
| External Storage Bus (Transition) | Transition | 1 polycarbonate sheet, 1 Transition etched board, 1 Transition controller chip and 1 SATA Port |
| Crafting Interface (Transition) | Transition | 1 polycarbonate sheet, 1 Transition etched board, 1 Transition controller chip and 3 Transition memory chips |
| Import Bus (Standard) | Standard | 1 polycarbonate sheet, 1 Standard etched board, 1 Standard controller chip and 1 electric motor |
| Export Bus (Standard) | Standard | 1 polycarbonate sheet, 1 Standard etched board, 1 Standard controller chip, 1 electric motor and 1 steel rod |
| External Storage Bus (Standard) | Standard | 1 polycarbonate sheet, 1 Standard etched board, 1 Standard controller chip and 1 SATA Port |
| Crafting Interface (Standard) | Standard | 1 polycarbonate sheet, 1 Standard etched board, 1 Standard controller chip and 3 Standard memory chips |
| Import Bus (Advanced) | Advanced | 1 polycarbonate sheet, 1 Advanced etched board, 1 Advanced controller chip and 1 electric motor |
| Export Bus (Advanced) | Advanced | 1 polycarbonate sheet, 1 Advanced etched board, 1 Advanced controller chip, 1 electric motor and 1 steel rod |
| External Storage Bus (Advanced) | Advanced | 1 polycarbonate sheet, 1 Advanced etched board, 1 Advanced controller chip and 1 SATA Port |
| Crafting Interface (Advanced) | Advanced | 1 polycarbonate sheet, 1 Advanced etched board, 1 Advanced controller chip and 4 Advanced memory chips |

#### Function cards

A card is the spec's expansion card: the etched board, 4 ceramic capacitors, its edge (as many copper foils
as its slot has) and its plating, with its chips on top. The Crafting Card takes a controller for every
thread and a memory chip for every 25 recipes it keeps; the personal-use cards (from the Legacy) what does
their station's work (the Crafting Table Card 2 memory chips, its recipes; the Furnace Card its heater, a
copper coil; the Enchanting Card lapis lazuli; the Anvil Card a steel plate); a Cluster Interface Card a
controller for every lane and the port its line plugs into; the Vaultis Array two controllers, the Vaultis
Cache one and its cache memory; the Optical Network Card the fibre's lasers and the fibre; a Phi, as a
processor is made, the chips of its own die (MIC, 1 for every 8 cores, each next model one more) and 8
memory chips; a WAN Interface Card what its line needs (the modem's line transformer, the T1's two, the
T3's controllers and braid, the lasers and fibre of the VLDC and the Optical Transport); the Network Anchor
Card a controller and 3 memory chips, the chunks it keeps.

| Item | Bench | Recipe |
| --- | --- | --- |
| Crafting Card ISA | Vintage | 1 Vintage etched board, 4 ceramic capacitors, 4 copper foils, 1 tin nugget, 1 Vintage controller chip and 1 Vintage memory chip |
| Serial Console Card | Vintage | 1 Vintage etched board, 4 ceramic capacitors, 5 copper foils, 1 tin nugget, 1 Vintage controller chip and 1 copper wire |
| Vaultis Array (Vintage) | Vintage | 1 Vintage etched board, 4 ceramic capacitors, 4 copper foils, 1 tin nugget and 2 Vintage controller chips |
| Vaultis Cache (Vintage) | Vintage | 1 Vintage etched board, 4 ceramic capacitors, 4 copper foils, 1 tin nugget, 1 Vintage controller chip and 2 Vintage memory chips |
| Modem Card (WAN) | Vintage | 1 Vintage etched board, 4 ceramic capacitors, 4 copper foils, 1 tin nugget, 1 Vintage controller chip and 1 transformer |
| Network Anchor Card (Vintage) | Vintage | 1 Vintage etched board, 4 ceramic capacitors, 4 copper foils, 1 tin nugget, 1 Vintage controller chip and 3 Vintage memory chips |
| Crafting Card PCI | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 5 copper foils, 1 gold nugget, 2 Legacy controller chips and 1 Legacy memory chip |
| Crafting Table Card | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Legacy controller chip and 2 Legacy memory chips |
| Furnace Card | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Legacy controller chip and 1 copper coil |
| Enchanting Card | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Legacy controller chip and 1 lapis lazuli |
| Anvil Card | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Legacy controller chip and 1 steel plate |
| Management NIC | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 2 Legacy controller chips and 1 copper wire |
| Vaultis Array (Legacy) | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 5 copper foils, 1 gold nugget and 2 Legacy controller chips |
| Vaultis Cache (Legacy) | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 5 copper foils, 1 gold nugget, 1 Legacy controller chip and 2 Legacy memory chips |
| T1 Card (WAN) | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 5 copper foils, 1 gold nugget, 1 Legacy controller chip and 2 transformers |
| Network Anchor Card (Legacy) | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 5 copper foils, 1 gold nugget, 1 Legacy controller chip and 3 Legacy memory chips |
| Crafting Card | Transition | 1 Transition etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 2 Transition controller chips and 3 Transition memory chips |
| Crafting Card T3 | Transition | 1 Transition etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 4 Transition controller chips and 4 Transition memory chips |
| Vaultis Array (Transition) | Transition | 1 Transition etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget and 2 Transition controller chips |
| Vaultis Cache (Transition) | Transition | 1 Transition etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Transition controller chip and 2 Transition memory chips |
| T3 Card (WAN) | Transition | 1 Transition etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 2 Transition controller chips, 1 transformer and 1 copper wire |
| Network Anchor Card (Transition) | Transition | 1 Transition etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Transition controller chip and 3 Transition memory chips |
| Crafting Card PCI-e 3.0 | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 6 Standard controller chips and 5 Standard memory chips |
| Fabric Host Adapter | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 4 Standard controller chips and 1 copper wire |
| Vaultis Array (Standard) | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget and 2 Standard controller chips |
| Vaultis Cache (Standard) | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Standard controller chip and 4 Standard memory chips |
| Optical Network Card | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Standard controller chip, 2 gallium arsenide laser diodes and 2 optical fibres |
| Integra Phi 5100 | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 8 Standard MIC processor chips and 8 Standard memory chips |
| Integra Phi 7120 | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 9 Standard MIC processor chips and 8 Standard memory chips |
| Integra Phi 7290 | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 10 Standard MIC processor chips and 8 Standard memory chips |
| Integra Phi 9000 | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 12 Standard MIC processor chips and 8 Standard memory chips |
| VLDC Card (WAN) | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 2 Standard controller chips, 2 gallium arsenide laser diodes and 2 optical fibres |
| Network Anchor Card (Standard) | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Standard controller chip and 3 Standard memory chips |
| Crafting Card T4 | Advanced | 1 Advanced etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 8 Advanced controller chips and 5 Advanced memory chips |
| Fabric DPU | Advanced | 1 Advanced etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 8 Advanced controller chips and 1 copper wire |
| Vaultis Array (Advanced) | Advanced | 1 Advanced etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget and 2 Advanced controller chips |
| Vaultis Cache (Advanced) | Advanced | 1 Advanced etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Advanced controller chip and 4 Advanced memory chips |
| Optical Transport Card (WAN) | Advanced | 1 Advanced etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 2 Advanced controller chips, 4 gallium arsenide laser diodes and 4 optical fibres |
| Network Anchor Card (Advanced) | Advanced | 1 Advanced etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Advanced controller chip and 3 Advanced memory chips |

### Power and cooling

#### Fans and coolers

The case fan of each era (80, 80, 92, 120 and 120 mm) is the fan every part with one takes. It is a motor and
its blades, PVC in the Vintage and polycarbonate after; the bigger fan takes more plastic, and the Advanced's
carries its light ring (an LED). The stock cooler is an aluminium plate in the Vintage, with a fan and thermal
paste from the Legacy and a copper rod from the Transition; the tower cooler 4 aluminium sheets, 2 copper rods
(the heat pipes), a fan and thermal paste; the all-in-one MF AquaLoop a copper plate, the pump (a motor), the
radiator (4 aluminium sheets on the 240, 6 on the 360), 2 or 3 fans, the hose (PVC), its water (a water bottle,
about the 250 mB it holds) and thermal paste.

| Item | Bench | Recipe |
| --- | --- | --- |
| Vintage Case Fan, 80 mm | Vintage | 1 electric motor and 1 PVC sheet |
| Stock Cooler (Vintage) | Vintage | 1 aluminium plate |
| Legacy Case Fan, 80 mm | Legacy | 1 electric motor and 1 polycarbonate sheet |
| Stock Cooler (Legacy) | Legacy | 1 aluminium plate, 1 Legacy case fan and 1 thermal paste |
| Transition Case Fan, 92 mm | Transition | 1 electric motor and 2 polycarbonate sheets |
| Stock Cooler (Transition) | Transition | 1 aluminium plate, 1 Transition case fan, 1 thermal paste and 1 copper rod |
| Standard Case Fan, 120 mm | Standard | 1 electric motor and 3 polycarbonate sheets |
| Stock Cooler (Standard) | Standard | 1 aluminium plate, 1 Standard case fan, 1 thermal paste and 1 copper rod |
| Tower Cooler (Standard) | Standard | 4 aluminium sheets, 2 copper rods, 1 Standard case fan and 1 thermal paste |
| MF AquaLoop 240 (Standard) | Standard | 1 copper plate, 1 electric motor, 4 aluminium sheets, 2 Standard case fans, 1 PVC sheet, 1 water bottle and 1 thermal paste |
| MF AquaLoop 360 (Standard) | Standard | 1 copper plate, 1 electric motor, 6 aluminium sheets, 3 Standard case fans, 1 PVC sheet, 1 water bottle and 1 thermal paste |
| Advanced Case Fan, 120 mm | Advanced | 1 electric motor, 3 polycarbonate sheets and 1 LED |
| Stock Cooler (Advanced) | Advanced | 1 aluminium plate, 1 Advanced case fan, 1 thermal paste and 1 copper rod |
| Tower Cooler (Advanced) | Advanced | 4 aluminium sheets, 2 copper rods, 1 Advanced case fan and 1 thermal paste |
| MF AquaLoop 240 (Advanced) | Advanced | 1 copper plate, 1 electric motor, 4 aluminium sheets, 2 Advanced case fans, 1 PVC sheet, 1 water bottle and 1 thermal paste |
| MF AquaLoop 360 (Advanced) | Advanced | 1 copper plate, 1 electric motor, 6 aluminium sheets, 3 Advanced case fans, 1 PVC sheet, 1 water bottle and 1 thermal paste |

#### UPSes and their batteries

A UPS is what a real one is: its steel box (3 plates for the 1500), its board and controller (the charger,
the screen and the button), its inverter, built as a power supply's (a transformer for every 300 W it
carries), its lead-acid batteries (one 12 V, 7 Ah block, about 300 kJ, for every 300 kJ it holds) and its
outlets and inlet (4 copper wires). The battery comes from the Electrolytic Cell ([Survival](survival.md)).

| Item | Bench | Recipe |
| --- | --- | --- |
| MF PowerGuard 300 (Vintage) | Vintage | 2 steel plates, 1 Vintage etched board, 1 Vintage controller chip, 1 transformer, 1 lead-acid battery and 4 copper wires |
| MF PowerGuard 300 (Legacy) | Legacy | 2 steel plates, 1 Legacy etched board, 1 Legacy controller chip, 1 transformer, 1 lead-acid battery and 4 copper wires |
| MF PowerGuard 500 (Transition) | Transition | 2 steel plates, 1 Transition etched board, 1 Transition controller chip, 2 transformers, 1 lead-acid battery and 4 copper wires |
| MF PowerGuard 800 (Standard) | Standard | 2 steel plates, 1 Standard etched board, 1 Standard controller chip, 3 transformers, 2 lead-acid batteries and 4 copper wires |
| MF PowerGuard 1500 (Advanced) | Advanced | 3 steel plates, 1 Advanced etched board, 1 Advanced controller chip, 5 transformers, 5 lead-acid batteries and 4 copper wires |

#### The power and cooling rack units

Each is 2 steel plates for its 1U and what it needs: the Rack UPS a UPS's parts for 3 kW and 2.7 MJ; a UPS
Battery Expansion its 9 batteries, the controller that watches them and its link to the UPS; the PDU its
metering board, a current transformer and 8 outlets; the Cooling Unit an air conditioner, its compressor (a
motor), its loop (2 copper pipes) and 2 fans. The **Vent Panel** has no electronics in it: it is one item for
every era, made at the Crafting Table, and takes the colour of the rack it is in: `SP St SP`, the 1U's ears (steel
plates) and the perforated face (a steel sheet).

| Item | Bench | Recipe |
| --- | --- | --- |
| Rack UPS (Vintage) | Vintage | 2 steel plates, 1 Vintage etched board, 1 Vintage controller chip, 10 transformers, 9 lead-acid batteries and 4 copper wires |
| UPS Battery Expansion (Vintage) | Vintage | 2 steel plates, 9 lead-acid batteries, 1 Vintage controller chip and 2 copper wires |
| PDU (Vintage) | Vintage | 2 steel plates, 1 Vintage etched board, 1 Vintage controller chip, 1 transformer and 8 copper wires |
| Cooling Unit (Vintage) | Vintage | 2 steel plates, 1 Vintage etched board, 1 Vintage controller chip, 2 Vintage case fans, 1 electric motor and 2 copper pipes |
| Rack UPS (Legacy) | Legacy | 2 steel plates, 1 Legacy etched board, 1 Legacy controller chip, 10 transformers, 9 lead-acid batteries and 4 copper wires |
| UPS Battery Expansion (Legacy) | Legacy | 2 steel plates, 9 lead-acid batteries, 1 Legacy controller chip and 2 copper wires |
| PDU (Legacy) | Legacy | 2 steel plates, 1 Legacy etched board, 1 Legacy controller chip, 1 transformer and 8 copper wires |
| Cooling Unit (Legacy) | Legacy | 2 steel plates, 1 Legacy etched board, 1 Legacy controller chip, 2 Legacy case fans, 1 electric motor and 2 copper pipes |
| Rack UPS (Transition) | Transition | 2 steel plates, 1 Transition etched board, 1 Transition controller chip, 10 transformers, 9 lead-acid batteries and 4 copper wires |
| UPS Battery Expansion (Transition) | Transition | 2 steel plates, 9 lead-acid batteries, 1 Transition controller chip and 2 copper wires |
| PDU (Transition) | Transition | 2 steel plates, 1 Transition etched board, 1 Transition controller chip, 1 transformer and 8 copper wires |
| Cooling Unit (Transition) | Transition | 2 steel plates, 1 Transition etched board, 1 Transition controller chip, 2 Transition case fans, 1 electric motor and 2 copper pipes |
| Rack UPS (Standard) | Standard | 2 steel plates, 1 Standard etched board, 1 Standard controller chip, 10 transformers, 9 lead-acid batteries and 4 copper wires |
| UPS Battery Expansion (Standard) | Standard | 2 steel plates, 9 lead-acid batteries, 1 Standard controller chip and 2 copper wires |
| PDU (Standard) | Standard | 2 steel plates, 1 Standard etched board, 1 Standard controller chip, 1 transformer and 8 copper wires |
| Cooling Unit (Standard) | Standard | 2 steel plates, 1 Standard etched board, 1 Standard controller chip, 2 Standard case fans, 1 electric motor and 2 copper pipes |
| Rack UPS (Advanced) | Advanced | 2 steel plates, 1 Advanced etched board, 1 Advanced controller chip, 10 transformers, 9 lead-acid batteries and 4 copper wires |
| UPS Battery Expansion (Advanced) | Advanced | 2 steel plates, 9 lead-acid batteries, 1 Advanced controller chip and 2 copper wires |
| PDU (Advanced) | Advanced | 2 steel plates, 1 Advanced etched board, 1 Advanced controller chip, 1 transformer and 8 copper wires |
| Cooling Unit (Advanced) | Advanced | 2 steel plates, 1 Advanced etched board, 1 Advanced controller chip, 2 Advanced case fans, 1 electric motor and 2 copper pipes |

### Teracoin's hardware

An ASIC is built as the real one: three hashboards (etched boards) of Blockwright's hashing chips of its era,
the control board's controller, its fans and its power supply inside (a transformer for every 300 W it draws,
1,400 and 3,200 W), in an aluminium case. A 3D printer is the real one's parts: the Prosa an aluminium frame,
smooth rods, five stepper motors, its heater (a copper coil), the spring steel sheet and its screen; the Rattan
a closed steel box with its glass door, four steppers, the belt that is its bed (PVC), its heater and its
light. Each is made at its era's bench ([Teracoin](teracoin.md)).

| Item | Bench | Recipe |
| --- | --- | --- |
| Blockwright Forge S1 | Standard | 2 aluminium plates, 3 Standard etched boards, 12 Standard Blockwright hashing chips, 1 Standard controller chip, 2 Standard case fans, 5 transformers and 4 copper wires |
| Prosa i3 MK3 | Standard | 2 aluminium plates, 4 steel rods, 5 electric motors, 1 Standard etched board, 1 Standard controller chip, 1 copper coil, 1 steel plate and 1 liquid crystal screen |
| Blockwright Forge A2 | Advanced | 2 aluminium plates, 3 Advanced etched boards, 16 Advanced Blockwright hashing chips, 1 Advanced controller chip, 4 Advanced case fans, 11 transformers and 4 copper wires |
| Rattan Lab X1 | Advanced | 4 steel plates, 2 glass panes, 4 electric motors, 1 PVC sheet, 1 Advanced etched board, 1 Advanced controller chip, 1 copper coil and 1 LED |

### Wireless and the portables

An access point is its board, its radio (a controller; two on the dual-band ones), its antennas (two and three
outside on the desk ones, four and eight inside the ceiling ones, as 802.11ac and Wi-Fi 6 had), its case, its
light, the ceiling ones' mounting plate, and its two lines, data and energy. A Wi-Fi card is the card base,
PCI in the Legacy and PCI Express x1 after, with its radio and antennas; the Advanced's carries Bluetooth, a
second controller. A dock is its board, its charger (a controller and a transformer), its lines and its body:
a PVC cradle, a polycarbonate base, an aluminium stand with its lamp, a wireless stand whose charging coil is a
copper coil. The Hightide Pocket Pro's memory card is made at the Transition bench.

| Item | Bench | Recipe |
| --- | --- | --- |
| Wireless Access Point, 802.11b/g | Legacy | 1 Legacy etched board, 1 Legacy controller chip, 2 antennas, 1 PVC sheet and 2 copper wires |
| Wi-Fi Card | Legacy | 1 Legacy etched board, 4 ceramic capacitors, 5 copper foils, 1 gold nugget, 1 Legacy controller chip and 1 antenna |
| Mobile Device Charging Dock | Legacy | 1 Legacy etched board, 1 Legacy controller chip, 1 transformer, 2 copper wires and 1 PVC sheet |
| Wireless Access Point, 802.11n | Transition | 1 Transition etched board, 1 Transition controller chip, 3 antennas, 1 polycarbonate sheet and 2 copper wires |
| Wi-Fi Card | Transition | 1 Transition etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Transition controller chip and 3 antennas |
| Mobile Device Charging Dock | Transition | 1 Transition etched board, 1 Transition controller chip, 1 transformer, 2 copper wires and 1 polycarbonate sheet |
| Memory Card, 2 GB (Hightide Pocket Pro) | Transition | 2 Transition memory chips, 1 Transition controller chip, 1 Transition etched board, 1 polycarbonate sheet and 1 copper foil |
| Wireless Access Point, 802.11ac | Standard | 1 Standard etched board, 2 Standard controller chips, 4 antennas, 1 polycarbonate sheet, 1 LED, 1 steel plate and 2 copper wires |
| Wi-Fi Card | Standard | 1 Standard etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 1 Standard controller chip and 3 antennas |
| Mobile Device Charging Dock | Standard | 1 Standard etched board, 1 Standard controller chip, 1 transformer, 2 copper wires, 1 aluminium plate and 1 LED |
| Wireless Access Point, Wi-Fi 6 | Advanced | 1 Advanced etched board, 2 Advanced controller chips, 8 antennas, 1 polycarbonate sheet, 2 LEDs, 1 steel plate and 2 copper wires |
| Wi-Fi Card + Bluetooth | Advanced | 1 Advanced etched board, 4 ceramic capacitors, 2 copper foils, 1 gold nugget, 2 Advanced controller chips and 2 antennas |
| Mobile Device Charging Dock | Advanced | 1 Advanced etched board, 1 Advanced controller chip, 1 transformer, 2 copper wires, 1 polycarbonate sheet, 1 LED and 1 copper coil |

A **portable** is the real device's parts, by the hardware of each one ([Wireless](wireless.md)): its chip,
its maker's die of its era (Hightide, Quince qA, Motoroller MX, Sunsung Exo; the qPad Pro the Quince Q1), a
faster model of the same die one chip more; its memory, the memory chips its era's density holds (32 MB in the
Legacy, 128 MB in the Transition, 1 GB in the Standard, 4 GB in the Advanced), and its flash, one memory chip
and one more for every bigger size of its era; the etched board; the screen of its era (two on a tablet) under
its touch layer, an indium tin oxide foil; its lithium-ion cells (two on a tablet); its antennas (the PDA's
Wi-Fi; the phone's network, Wi-Fi and GPS; the tablet's Wi-Fi and GPS); the camera's lens on the phones and
tablets; and its case, PVC, polycarbonate, aluminium, or the qPhone X's steel.

| Item | Bench | Recipe |
| --- | --- | --- |
| Hightide Pocket | Legacy | 1 Legacy Hightide chip, 3 Legacy memory chips, 1 Legacy etched board, 1 liquid crystal screen, 1 indium tin oxide foil, 1 lithium-ion cell, 1 antenna and 1 PVC sheet |
| Hightide Pocket Pro | Transition | 1 Transition Hightide chip, 2 Transition memory chips, 1 Transition etched board, 1 liquid crystal screen, 1 indium tin oxide foil, 1 lithium-ion cell, 1 antenna and 1 polycarbonate sheet |
| qPhone | Transition | 1 Transition Quince qA chip, 3 Transition memory chips, 1 Transition etched board, 1 liquid crystal screen, 1 indium tin oxide foil, 1 lithium-ion cell, 3 antennas, 1 glass lens and 1 aluminium plate |
| Motoroller Landmark | Transition | 1 Transition Motoroller MX chip, 5 Transition memory chips, 1 Transition etched board, 1 liquid crystal screen, 1 indium tin oxide foil, 1 lithium-ion cell, 3 antennas, 1 glass lens and 1 polycarbonate sheet |
| qPhone 5 | Standard | 1 Standard Quince qA chip, 3 Standard memory chips, 1 Standard etched board, 1 LED screen, 1 indium tin oxide foil, 1 lithium-ion cell, 3 antennas, 1 glass lens and 1 aluminium plate |
| Hightide Wave | Standard | 1 Standard Hightide chip, 4 Standard memory chips, 1 Standard etched board, 1 LED screen, 1 indium tin oxide foil, 1 lithium-ion cell, 3 antennas, 1 glass lens and 1 aluminium plate |
| qPad Air | Standard | 2 Standard Quince qA chips, 5 Standard memory chips, 1 Standard etched board, 2 LED screens, 1 indium tin oxide foil, 2 lithium-ion cells, 2 antennas, 1 glass lens and 2 aluminium plates |
| Sunsung Nebula Tab | Standard | 1 Standard Sunsung Exo chip, 2 Standard memory chips, 1 Standard etched board, 2 LED screens, 1 indium tin oxide foil, 2 lithium-ion cells, 2 antennas, 1 glass lens and 2 polycarbonate sheets |
| qPhone X | Advanced | 1 Advanced Quince qA chip, 2 Advanced memory chips, 1 Advanced etched board, 1 self-lit panel, 1 indium tin oxide foil, 1 lithium-ion cell, 3 antennas, 1 glass lens and 1 steel plate |
| Sunsung Nebula S10 | Advanced | 1 Advanced Sunsung Exo chip, 4 Advanced memory chips, 1 Advanced etched board, 1 self-lit panel, 1 indium tin oxide foil, 1 lithium-ion cell, 3 antennas, 1 glass lens and 1 aluminium plate |
| qPad Pro | Advanced | 1 Quince Q1 processor chip, 5 Advanced memory chips, 1 Advanced etched board, 2 self-lit panels, 1 indium tin oxide foil, 2 lithium-ion cells, 2 antennas, 1 glass lens and 2 aluminium plates |
| Sunsung Nebula Tab S7 | Advanced | 2 Advanced Sunsung Exo chips, 4 Advanced memory chips, 1 Advanced etched board, 2 self-lit panels, 1 indium tin oxide foil, 2 lithium-ion cells, 2 antennas, 1 glass lens and 2 aluminium plates |

### Books and the villagers' job sites

- **The Guide to Operations** is a binder, as the first computers' manuals were, made at the Crafting Table from
  the Vintage on: `PVC P / SR P / PVC P`, the PVC covers, the rings (a steel rod) and the pages.
- **The books of the systems and the languages** (the Frames Resource Kit, the Linux Desk Reference, Linux by
  Hand, the Daemon Handbook, Learning IQL, the Book of Sigma) are printed by the Large Scale Printer, as every
  book is, 3 paper each, from the help of a computer whose system has them ([Manuals](manuals.md)). They also
  turn up in libraries, offices and universities. The Technical Reference is J's Core's.
- **The job sites**, as vanilla's: two things of the trade on 4 planks of any wood. The Programmer's Desk is
  `FD P / Pl Pl / Pl Pl`, the floppy disk and the sheet of code on it;
  the Repair Bench is `So Fe / Pl Pl / Pl Pl`, the solder and the iron of its tools.
