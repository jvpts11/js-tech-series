# Survival

How a player gets every computer, part and program of J's Computers without the creative menu: the mod's own chain of
machines and benches, the Prebuilder and the Preinstaller that turn parts into working computers, and the abandoned
buildings and villagers where the software is found. Today none of it exists: everything comes from the creative tab,
and a modpack can write its own recipes and loot through KubeJS ([The API](api.md)).

## To build

### Two chains

J's Computers never depends on J's Industrial, so it has a chain of its own, and J's Industrial brings another:

- **J's Computers' chain is the easy one.** It follows a real electronics factory, from the ore to the chip, with real
  materials: the elements, compounds, gases, acids and resins of J's Core's catalogue, in its forms (dust, plate,
  sheet, foil, wire, rod, boule, wafer, lens, crystal). Each element comes from one source and one way of refining
  it.
- **J's Industrial's chain is the hardcore one:** the real minerals (cassiterite, galena, bauxite, coltan), their
  byproducts and full refining, the Clean Room and its lithography, boards on its SMT Line, binning ([Eras](eras.md)).
- **J's Computers' chain can be switched off** (`own_chain`, on by default). With J's Industrial installed, the
  industrial chain is then the only one; without it, the mod brings no recipes at all and a modpack writes its own.
  The structures and the villagers stay either way.
- Every recipe is data: JEI, EMI and the manuals show all of them, and KubeJS can change any of them. What a player
  cannot make yet is still shown, with what it needs ([Overview](overview.md)).
- **Nothing in the chain is a mob's drop.** What a player needs is mined, grown or processed, never hunted or farmed
  from mobs. Vanilla things that are mined or grown are fine: sand, coal, wood, calcite, sugar cane.

### Energy

The machines draw J's Energy as the computers do ([Power](power.md)). J's Computers has no generator of its own: the
energy comes from the other mods a player has installed, J's Industrial or any mod's FE through J's Core's converters.
The setting that turns the computers' draw off turns the machines' off too.

### Where each material comes from

| Source | What it gives |
| --- | --- |
| J's Core's generic ore of the element | tin, lead, zinc, nickel, aluminium, tantalum, chromium, cobalt, silver, manganese, mercury, fluorine (fluorspar), zirconium, tungsten, molybdenum; and vanilla's iron, copper and gold |
| A byproduct, in the Crusher | gallium (from aluminium ore), indium and germanium (from zinc ore), hafnium (from zirconium ore), arsenic (from copper and gold ore), ruthenium (from nickel ore), sulfur (from zinc and lead ore) |
| The Air Separation Unit | nitrogen, oxygen, argon, krypton |
| The Electrolytic Cell | hydrogen and oxygen from water; chlorine and caustic soda from salt; refined copper; aluminium from alumina |
| The Coke Oven | coke and coal tar from coal |
| The Chemical Reactor | salt, with traces of boron, by evaporating water; alumina from aluminium ore and caustic soda; phosphorus from bone meal; methanol from coke and steam |
| The Induction Furnace | steel from iron and coke; metallurgical silicon; calcium carbide |
| Mined and grown vanilla | sand (silica), calcite (calcium), bone meal from a composter, water |

**The ores lie in veins** of J's Core's world kit, which J's Computers declares when no other mod has. The elements of
the later eras lie in rarer and deeper veins, and some only in the Nether and the End, so each era still takes the
player further across the world:

| Era | Where its new ores lie | Spacing | Height |
| --- | --- | --- | --- |
| Vintage | the Overworld, near the surface: tin, lead, zinc, nickel, aluminium, tantalum, chromium, manganese, mercury, fluorspar | 12 chunks | y 0 to 80 |
| Legacy | deep in the Overworld: cobalt, silver | 16 chunks | y −16 to 40 |
| Transition | deep in the Overworld (indium, from the zinc already mined) | | |
| Standard | the deepslate: zirconium (and its hafnium), tungsten | 24 chunks | y −64 to 0 |
| Advanced | the Nether: molybdenum (ruthenium comes from the nickel already mined) | 24 chunks | y 10 to 110 |
| Exa | the End, set when the era is designed | | |

The spacing is the average distance between two veins of the same kind; a vein has the size of J's Core's kit
(about 3 by 3 chunks).

### The material of each era

Each era asks for what its real hardware was made of. The amounts are first estimates.

| Era | New materials | What they are for, as in the real hardware |
| --- | --- | --- |
| Vintage | silicon, phosphorus and boron, aluminium, tin and lead, nickel, tantalum, alumina, phenolic resin, zinc sulfide, ferrite, mercury, chromium, steel | chips; their dopants; the wiring inside the chip and disk platters; solder; permalloy heads; capacitors; ceramic chip packages; FR-2 boards (paper and phenolic resin from coal tar); the phosphor of tube monitors; inductors and motor magnets; the lithography's mercury lamp; its masks (chromium on quartz); cases |
| Legacy | gold, glass fibre and epoxy, cobalt, gallium and arsenic, silver, polycarbonate | bonding wire and contacts; FR-4 boards; the platters' magnetic layer; the gallium arsenide laser of CD drives; thermal paste; compact discs |
| Transition | copper inside the chip, lead-free solder (tin, silver, copper), indium tin oxide, aluminium gallium indium phosphide, krypton and fluorine | the copper wiring of around 2000; lead-free solder; flat screens; the laser of DVD drives; the krypton fluoride laser of the lithography |
| Standard | hafnium, gallium nitride, germanium, tungsten, argon, calcium fluoride, ultrapure water | the high-k gate of 2007; LEDs and the blue laser of Blu-ray drives; silicon germanium and the core of optical fibre; the vias; the argon fluoride laser, its lenses and immersion |
| Advanced | molybdenum, tin droplets, cobalt, ruthenium | the mirrors and the tin plasma light of extreme ultraviolet lithography; the wiring of the modern nodes |
| Exa | set when the era is designed | |
| Singularity | computronium | everything ([The cosmological simulator](simulator.md)) |

Without J's Industrial, the computronium of the Singularity comes from the cosmological simulator.

### The benches

- The vanilla crafting table makes only the **Vintage Electronics Workbench**. Everything else is made with the mod's
  benches and machines.
- There is an **Electronics Workbench** per era (Vintage, Legacy, Transition, Standard, Advanced; the Exa and the
  Singularity get theirs with their eras). It is a plain block with a crafting grid of its own. A bench assembles the
  parts of its era and of the eras before it, and the bench of the next era.
- The blocks of the machines below are made at the bench of the era that first needs them; every machine the Vintage
  chain uses, the Prebuilder and the Preinstaller are made at the Vintage bench.

### The machines

Every machine is a multiblock, built with J's Core's multiblock kit (four rotations) and shown by the Structure
Projector. The sizes are first estimates; their looks are drawn and approved before any code. Every machine takes the
Crafting Interface, so the network's autocrafting runs the whole chain ([Autocrafting](autocrafting.md)).

| Group | Machine | Size | What it does |
| --- | --- | --- | --- |
| Ore and metals | Crusher | 3x3x3 | ore into crushed ore and its byproducts |
| | Grinder | 2x2x2 | ingots, crystals and glass into dust |
| | Induction Furnace | 3x3x3 | dusts into ingots and alloys; sand and coke into metallurgical silicon; coke and calcite into calcium carbide |
| | Electrolytic Cell | 3x2x2 | hydrogen and oxygen from water, chlorine from salt, refined copper, aluminium from alumina |
| | Rolling Mill | 3x2x2 | ingot into plate, sheet and foil |
| | Wire Drawer | 3x2x1 | ingot into rod and wire, bonding wire included |
| Chemistry and gases | Coke Oven | 3x3x3 | coal into coke and coal tar |
| | Chemical Reactor | 3x3x2 | salt and its boron from water; alumina from aluminium ore and caustic soda; hydrogen chloride, hydrofluoric and sulfuric acid, ferric chloride; phenol, methanol, phenolic and epoxy resins, polycarbonate; PVC through calcium carbide (from coke and calcite in the Induction Furnace) and acetylene; photoresist; cultured quartz; phosphorus from bone meal; ultrapure water, zinc sulfide, liquid crystal, thermal paste |
| | Air Separation Unit | 3x3x4 | air into nitrogen, oxygen, argon and krypton |
| Silicon | Siemens Reactor | 2x2x3 | metallurgical silicon, hydrogen chloride and hydrogen into polysilicon |
| | Crystal Puller | 2x2x3 | polysilicon and a dopant into a boule; also gallium arsenide, aluminium gallium indium phosphide, gallium nitride and calcium fluoride |
| | Wafer Saw | 2x2x2 | a boule into wafers, cut and polished flat |
| The chip factory | Lithography Station | 3x3x2 | a wafer's layer exposed through its mask; one per era |
| | Plasma Etcher | 2x2x2 | the exposed layer etched |
| | Ion Implanter | 3x2x2 | the layer doped |
| | Deposition Chamber | 2x2x2 | metal laid on the layer (aluminium, copper, tungsten, cobalt), and the magnetic layer of disk platters |
| Packaging | Packaging Line | 3x2x2 | a finished wafer cut into dies, each packaged: ceramic in the Vintage, epoxy and pins later, solder balls from the Transition |
| Boards and components | Laminator | 3x2x2 | paper or glass fibre and resin, clad in copper foil |
| | Board Etcher | 3x2x2 | a clad laminate etched into a board's traces |
| | Ceramic Kiln | 2x2x2 | alumina packages, ceramic capacitors, ferrites |
| | Coil Winder | 2x2x1 | wire into coils, motors and magnetic heads |
| | Optical Polisher | 2x2x2 | quartz and calcium fluoride into lenses and oscillator crystals, glass into blank discs; draws glass fibre, and optical fibre from germanium-doped glass |
| Displays | Display Line | 3x3x2 | tube screens with their phosphor, flat screens with indium tin oxide, LED panels |

### The chain's numbers

All of them are first estimates. Each machine works in steps, and draws its power only while it works (1 kW is 50 FE
a tick through J's Core's converters):

| Machine | Seconds a step | kW | Machine | Seconds a step | kW |
| --- | ---: | ---: | --- | ---: | ---: |
| Crusher | 8 | 2 | Crystal Puller | 120 | 6 |
| Grinder | 4 | 1 | Wafer Saw | 20 | 2 |
| Induction Furnace | 8 | 4 | Lithography Station | 20 a layer | 2, 3, 4, 6, 10 by era |
| Electrolytic Cell | 10 | 3 | Plasma Etcher | 10 | 3 |
| Rolling Mill | 4 | 1.5 | Ion Implanter | 10 | 4 |
| Wire Drawer | 4 | 1 | Deposition Chamber | 10 | 3 |
| Coke Oven | 30 | 0 | Packaging Line | 20 | 2 |
| Chemical Reactor | 10 | 2 | Laminator, Board Etcher | 10 | 1.5 |
| Air Separation Unit | 10 | 3 | Ceramic Kiln | 20 | 3 |
| Siemens Reactor | 60 | 5 | Coil Winder | 4 | 0.5 |
| Optical Polisher | 10 | 1 | Display Line | 30 | 3 |
| Prebuilder | 10 | 1 | Preinstaller | 10, + 1 for every 100 MB written | 1 |

The Coke Oven needs no energy: it burns part of its own coal, as a real one does. The Advanced's extreme ultraviolet
lithography draws 10 kW, as the real one is the hungriest machine of a fab.

- **Ore:** the Crusher turns 1 ore (the block or its raw form) into 2 crushed ores, with a chance of a byproduct's
  dust: gallium 25% (from aluminium), indium 15% and germanium 10% (from zinc), hafnium 25% (from zirconium), arsenic
  15% (from copper and gold), ruthenium 5% (from nickel), sulfur 50% (from zinc and lead). A crushed ore gives one
  ingot: in the vanilla furnace for the metals it melts (iron, copper, gold, tin, lead, zinc, silver, nickel, cobalt,
  manganese, gallium, indium, germanium), in the Induction Furnace for the refractory ones (tantalum, tungsten,
  molybdenum, zirconium, hafnium). Crushed mercury ore, roasted in the Induction Furnace, gives 100 mB of mercury.
- **Forms:** the Rolling Mill turns 1 ingot into 1 plate, 1 plate into 2 sheets and 1 sheet into 4 foils; the Wire
  Drawer turns 1 ingot into 2 rods or 4 wires, and 1 gold or aluminium ingot into 16 bonding wires; the Grinder turns
  1 ingot or crystal into 1 dust.
- **Alloys and furnace work** (Induction Furnace), in their real proportions: solder, 3 tin and 2 lead into 5;
  lead-free solder, 9 tin, 1 silver and a copper nugget into 10; permalloy, 4 nickel and 1 iron into 5; indium tin
  oxide, 9 indium and 1 tin into 10; steel, 2 iron and 1 coke into 2; metallurgical silicon, 1 sand and 1 coke;
  calcium carbide, 1 calcite and 1 coke.
- **Gases:** each step of the Air Separation Unit takes air and gives 780 mB of nitrogen, 210 of oxygen, 9 of argon and
  1 of krypton. Krypton is raised well above its real share, or the Transition's laser would take hours to fill.
- **Electrolysis:** 1,000 mB of water into 1,000 of hydrogen and 500 of oxygen; 1 salt and 1,000 of water into 500 of
  chlorine, 500 of hydrogen and 1 caustic soda, as the real chlor-alkali cell does; 1 alumina and 1 coke into 1
  aluminium (the carbon anode is used up, as in the real cell); 1 copper ingot into 1 refined copper ingot.
- **Coke:** the Coke Oven turns 1 coal into 1 coke and 250 mB of coal tar.
- **Chemistry** (Chemical Reactor):

  | Product | From |
  | --- | --- |
  | 1 salt (and 1 boron every 16) | 4,000 mB of water, evaporated (60 seconds) |
  | 1 alumina | 1 crushed aluminium ore and 1 caustic soda (the Bayer process) |
  | 1,000 mB of hydrogen chloride | 500 of hydrogen and 500 of chlorine |
  | 1,000 mB of sulfuric acid | 1 sulfur, 500 of oxygen and 1,000 of water |
  | 1,000 mB of hydrofluoric acid | 1 fluorspar dust and 1,000 of sulfuric acid |
  | 1,000 mB of ferric chloride | 1 iron dust and 1,000 of hydrogen chloride |
  | 500 mB of phenol | 1,000 of coal tar |
  | 500 mB of methanol | 1 coke and 1,000 of water |
  | 1 phenolic resin | 500 of phenol and 500 of methanol |
  | 1 epoxy resin | 500 of phenol and 250 of chlorine |
  | 1 polycarbonate | 500 of phenol, 250 of chlorine and 1 coke |
  | 1,000 mB of acetylene | 1 calcium carbide and 1,000 of water |
  | 1 PVC | 1,000 of acetylene and 1,000 of hydrogen chloride |
  | 1,000 mB of photoresist | 1 phenolic resin and 500 of methanol |
  | 1 cultured quartz | 4 sand, 1 caustic soda and 1,000 of water (60 seconds) |
  | 1 phosphorus | 4 bone meal, 1 coke and 1 sand |
  | 500 mB of ultrapure water | 1,000 of water |
  | 1 zinc sulfide | 1 zinc dust and 1 sulfur |
  | 250 mB of liquid crystal | 500 of phenol and 1 fluorspar dust (real liquid crystals are fluorinated) |
  | 4 thermal paste | 1 silver dust and 1 zinc dust |

- **Components:**

  | Machine | What it makes |
  | --- | --- |
  | Ceramic Kiln | 1 alumina into 4 ceramic packages; 1 alumina and 1 nickel foil into 8 ceramic capacitors (nickel electrodes, as in the real ones); 1 tantalum and 1 manganese dust into 8 tantalum capacitors (the real manganese dioxide cathode); 2 iron, 1 manganese and 1 zinc dust into 4 ferrites (manganese zinc) |
  | Coil Winder | 1 ferrite and 2 copper wires into 2 inductors; 2 ferrites and 4 copper wires into 1 transformer; 1 steel rod, 4 copper wires and 1 ferrite into 1 electric motor; 1 permalloy foil and 1 copper wire into 2 magnetic heads |
  | Optical Polisher | 1 cultured quartz into 4 oscillator crystals or 1 mask plate; 1 glass into 4 lenses; 1 calcium fluoride boule into 4 lenses; 1 polycarbonate into 4 blank discs; 1 glass into 8 glass fibres; 1 glass and 1 germanium nugget into 8 optical fibres |
  | Laminator | FR-2 (Vintage): 2 paper, 1 phenolic resin and 1 copper foil; FR-4 (from the Legacy): 2 glass fibres, 1 epoxy resin and 1 copper foil |
  | Board Etcher | a clad laminate and 250 mB of ferric chloride into an etched board, which takes 1 laminate in the Vintage and the Legacy, 2 in the Transition, 3 in the Standard and 4 in the Advanced, as real boards gained layers |
  | Display Line | a tube (Vintage, Legacy): 4 glass, 1 lead, 1 zinc sulfide, 2 coils; a liquid crystal screen (Transition): 2 glass, 1 indium tin oxide foil, 250 mB of liquid crystal and a cold cathode lamp (100 mB of mercury and 1 glass); the Standard's, the same with 8 gallium nitride LEDs instead of the lamp; the Advanced's, 2 glass, 1 indium tin oxide foil and 16 LEDs, a panel that lights itself |

### How a chip is made

1. Sand and coke make **metallurgical silicon** in the Induction Furnace.
2. With hydrogen chloride it becomes trichlorosilane, and with hydrogen, in the Siemens Reactor, **polysilicon**: 1
   metallurgical silicon, 1,000 mB of hydrogen chloride and 500 of hydrogen give 1 polysilicon.
3. The Crystal Puller grows a **boule** from 4 polysilicon and 1 dopant (phosphorus or boron); the Wafer Saw cuts it
   into 8 **wafers** and polishes them flat.
4. Each layer of the chip goes through the **Lithography Station** (photoresist, the era's light and its mask), the
   **Plasma Etcher**, the **Ion Implanter** and the **Deposition Chamber**. A chip of a later era has more layers: 2 in
   the Vintage, 3 in the Legacy, 4 in the Transition, 6 in the Standard, 8 in the Advanced.
5. The **Packaging Line** cuts the finished wafer into dies and packages each into a **chip**: a processor, graphics,
   memory or controller chip, by the masks used.

What each layer uses, by machine:

| Machine | Every layer |
| --- | --- |
| Lithography Station | 100 mB of photoresist; in the Standard also 100 mB of ultrapure water (the immersion); in the Advanced also 1 tin nugget every 4 layers (the plasma's droplets) |
| Plasma Etcher | 100 mB of hydrofluoric acid up to the Legacy; 100 mB of chlorine and 10 of argon from the Transition |
| Ion Implanter | 1 phosphorus or boron dust dopes 8 layers |
| Deposition Chamber | 1 nugget of the era's wiring metal: aluminium (Vintage, Legacy), refined copper (Transition), copper and tungsten in turn (Standard), cobalt and ruthenium in turn (Advanced); a Standard chip's first layer takes 1 hafnium nugget, the high-k gate |

A finished wafer gives 8 memory or controller chips, or 4 processor or graphics chips, and every chip takes the
package of its era: in the Vintage, 1 ceramic package and 2 aluminium bonding wires; in the Legacy, epoxy (1 resin for
4 chips), 2 gold bonding wires and copper foil (1 for 4 chips); from the Transition, epoxy and 1 lead-free solder
nugget, its balls.

The lithography's light follows the real one: a mercury lamp in the Vintage and the Legacy, a krypton fluoride laser
in the Transition, an argon fluoride laser with calcium fluoride lenses and immersion in ultrapure water in the
Standard, extreme ultraviolet from tin plasma and molybdenum mirrors in the Advanced.

**Masks.** A mask serves one kind of chip of one era, sits in the station's mask slot and is never used up. Its blank
is a mask plate from the Optical Polisher with 1 chromium nugget laid on it in the Deposition Chamber. The Vintage
station writes it by photo-reduction from a drawing (the blank and 1 paper), as masks were made then; from the Legacy
on, the linked computer writes it.

**Lasers and LEDs.** The Crystal Puller also grows the boules of light: gallium arsenide (Legacy) from 2 gallium and 2
arsenic; aluminium gallium indium phosphide (Transition) from 1 aluminium, 1 gallium, 1 indium and 1 phosphorus;
gallium nitride (Standard) from 2 gallium and 1,000 mB of nitrogen. Their wafers take one layer and give 16 laser
diodes or 32 LEDs. It grows calcium fluoride from 4 fluorspar dust (fluorspar is calcium fluoride), and the Optical
Polisher cuts each boule into 4 lenses.

### Chips are made by the computers of the era before

From the Legacy on, an era's Lithography Station only works linked by peripheral cable to a computer of the era before
or later, switched on and running: the computer runs the lithography. Every layer takes 20 seconds, and while it
exposes the station runs on the computer as the `litho` process, holding memory: 2 MB for the Legacy's station, 32 MB
for the Transition's, 256 MB for the Standard's, 2 GB for the Advanced's. A computer two eras newer than the one asked
for exposes in half the time. The Vintage station needs no computer, since there is no era before it. Making a new
era's computers is the work of the previous era's.

### The parts

The generic components of J's Core's catalogue that J's Computers uses (electric motors, LEDs, oscillators, antennas,
magnetic heads) get their recipes here, as J's Core's design asks of every mod that uses them without J's Industrial.
At its era's bench, every part is assembled from the chain's pieces:

| Part | Made of |
| --- | --- |
| Processor | 1 processor chip for every 8 cores, at least 1 (a 64-core processor takes 8, as real chiplets do); a copper plate as its heat spreader, with a nickel foil from the Transition; its contacts, a tin nugget in the Vintage and a gold one from the Legacy |
| Memory module | 8 memory chips, 1 solder, 1 etched board |
| Graphics card | 1 graphics chip; memory chips, 2, 4, 4, 8 and 8 by era; 2 tantalum and 4 ceramic capacitors; 2 inductors; 1 etched board; an aluminium plate as its heatsink; a fan from the Legacy |
| Motherboard | controller chips, 4, 2, 2, 1 and 1 by era, as the real chipset shrank; 1 oscillator crystal, 8 capacitors, 4 inductors, 1 etched board (2 for EATX, EEB and MTX); it comes out without firmware |
| Power supply | 1 transformer, 2 inductors, 4 capacitors, 1 controller chip, 4 copper wires, 1 aluminium plate, 2 steel plates, 1 fan |
| Hard disk | platters, 1, 2, 3, 4 and 4 by era (an aluminium sheet with a layer of iron dust, iron oxide as the real ones had, in the Vintage, and 1 cobalt nugget from the Legacy), one magnetic head per platter, 1 motor, 1 controller chip, 1 aluminium plate |
| Solid-state disk | 4 memory chips (8 in the Advanced), 1 controller chip, 1 etched board |
| Floppy disk | a film of PVC and iron dust, a PVC case |
| Optical disc | a blank disc and an aluminium foil (silver for Blu-ray) |
| Monitor | the Display Line's screen of its era, 1 etched board, a case of 2 PVC (Vintage) or 2 polycarbonate |
| Drive | optical: a motor, a lens, the laser diode of its era, an etched board; floppy: a motor, a magnetic head, an etched board |
| Expansion card | its chip, 4 capacitors, 1 etched board, its contacts (tin in the Vintage, gold later) |
| Case | AT, 6 steel plates; ATX, 6 steel and 2 aluminium plates; mid tower and Neutral, 4 steel and 4 aluminium; High Performance, those and 2 steel sheets (the mesh); Aesthetic, those and 2 glass panes |
| Rack, rack unit | 12 steel plates; 2 steel plates and the parts each unit needs |
| Cables (8) | 4 copper wires and 1 PVC; the fibre lines, 4 optical fibres and 1 PVC |
| Stock cooler | an aluminium plate in the Vintage; a fan and thermal paste added from the Legacy; a copper rod added from the Transition |
| Tower cooler | 4 aluminium sheets, 2 copper rods (the heat pipes), 1 fan, 1 thermal paste |
| Water cooler | 1 copper plate, 1 motor (the pump), 4 aluminium sheets (the radiator), 2 fans, 1 PVC, 250 mB of water, 1 thermal paste |
| Fan | 1 motor and 1 PVC (Vintage) or 1 polycarbonate |

**Software is never crafted:** systems and programs are found, bought, copied or downloaded (below,
[Programs](programs.md)).

### The Prebuilder

A 3x3x2 multiblock, one for every era, made of Vintage materials. It is optional: assembling a computer by hand works as
it does today.

- It takes a computer's case and its parts and puts them together, by the same rules as the computer's assembly window,
  and when it refuses it says why ([Computers](computers.md)).
- What comes out is the **assembled computer** as an item, with its parts inside, listed in its tooltip. Placed, it is
  the assembled computer, switched off and owing its first self-test.
- It builds every computer that is one block: the Personal Computer, the Crafting Computer, the Cluster Management
  Computer, the small computers to come, and every rack server chassis. The Mainframe and the other multiblocks are
  assembled where they stand.
- It takes the Crafting Interface, so the network orders computers like any other craft.

**Breaking a computer gives the whole computer**, with its parts inside, as a rack server does today. Parts come out by
opening the case.

### The Preinstaller and the firmware

A 2x2x2 multiblock, one for every era, made of Vintage materials, because the first computer already needs it.

- **A motherboard comes out of the chain with no firmware.** A computer with a blank board powers its fans and
  nothing more: the monitor has no signal, and the assembly window says the board has no firmware.
- The Preinstaller writes the firmware of the board's era onto it: the text BIOS in the Vintage, the blue BIOS in the
  Legacy and the Transition, UEFI from the Standard ([Operating systems](operating-systems.md)).
- It takes an assembled computer (it writes the firmware, then a system and programs onto its system disk), a loose
  motherboard (the firmware only) and a loose disk (a system and programs, which is how a rack server's bay drives get
  their system).
- The system and the programs come from media put into it, which it reads and never uses up: one copy installs as many
  machines as the player wants, as a factory image does.
- It takes the Crafting Interface, so the network prepares machines on its own.
- Motherboards from the creative tab, from loot and from trades already carry their firmware.

### Abandoned buildings

Twenty kinds of building lie abandoned across the Overworld, on land. Each is of one era, picked by its distance from
the world's spawn: the Vintage near it, the Advanced far away, among the eras that building may be of. About one in
five is **working**: its machines are whole, with their systems installed and on their network, and they switch on and
run; once computers draw energy, a charged J's Core basic battery keeps them going for about a game day, then they go
dark until the battery is charged again. The rest are ruins, with their loot in chests and inside the broken machines
(parts in the cases, boxed software on the shelves, media, cables, whole computers, manuals).

The spacing is the average distance, in chunks, between two buildings of the same kind (estimates); all twenty
together give about one building every 190 blocks, most of them the small common ones.

| Building | Eras | Spacing | What is there |
| --- | --- | ---: | --- |
| Abandoned house | Vintage, Legacy | 32 | an old computer in the attic; often a player's first find |
| Electronic waste dump | all | 32 | heaps of broken computers in the open, for salvage |
| Overturned delivery van | all | 32 | boxes of hardware by the road; small |
| Computer shop | all | 48 | parts on the shelves, boxed software, a counter |
| LAN house | Legacy, Transition | 48 | many computers on a network, games |
| School computer room | Vintage to Transition | 48 | many computers, educational software |
| Public library | all | 48 | computers, manuals and technical books |
| Radio and television station | Vintage, Legacy | 48 | old computers and antennas |
| Telephone exchange | all | 48 | routers, switches, cables and rack units: where network gear is found |
| University lab | all | 72 | workstations, a small Mainframe, scientific software, books |
| Bank | all | 72 | a Mainframe in the vault, with its terminals |
| Midsoft office | all | 72 | computers running Frames and boxed Midsoft software, or already looted, with nothing of Midsoft left |
| Software house office | the eras of its products | 72 | each of the other houses (Nouvell Networks, Autodeck and the rest) with its own programs |
| Startup building | Transition to Advanced | 72 | computers running Linux and FreeBSD, a small server room, other houses' software |
| Game studio | Legacy to Advanced | 72 | games and high-end graphics cards |
| Hacker's den | Legacy to Advanced | 72 | a home-made rack, odd hardware, rare software, in a basement |
| Electronics factory | all | 72 | the chain's machines half broken, wafers and chips: where the chain is learned |
| Crypto mining farm | Standard, Advanced | 72 | racks of graphics cards mining ([Teracoin](teracoin.md)) |
| Datacenter | Legacy to Advanced | 112 | racks of servers, a Server Router, cables; large |
| Supercomputing centre | Standard, Advanced | 160 | supercomputer racks and Phi coprocessors; the rarest |

- **The loot:** every chest draws 4 to 8 items from its building's table and era; a broken computer keeps each of its
  parts with a 50% chance; boxed software comes from the programs of the building's era, and a house's office only
  holds its house's (a Midsoft office only Midsoft's).

The buildings are J's Computers' own. Each one can be switched off in the settings, with its spacing and the eras it
may be of.

### Villagers

Two professions, each with its own job site:

| Profession | Job site | Sells | Buys |
| --- | --- | --- | --- |
| Programmer | Programmer's Desk | systems and programs, on the media of their era | paper, books, blank media |
| IT Technician | Repair Bench | parts (motherboards with their firmware), cables, peripherals | copper ingots, tin, parts of older eras |

- **A villager's level is the era of what it sells:** Novice the Vintage, Apprentice the Legacy, Journeyman the
  Transition, Expert the Standard, Master the Advanced. A villager keeps the trades of the levels below.
- They are paid in emeralds, as every villager is. A price is the era's base times the item's factor, at most 64
  emeralds (estimates):

  | Era (level) | Base | | Item | Factor |
  | --- | ---: | --- | --- | ---: |
  | Vintage (Novice) | 4 | | a program | 1 |
  | Legacy (Apprentice) | 8 | | a system | 2 |
  | Transition (Journeyman) | 12 | | a part | 1 (a processor or graphics card 1.5) |
  | Standard (Expert) | 20 | | a peripheral | 1 |
  | Advanced (Master) | 32 | | 8 cables | 0.25 |

- **What they buy:** the Programmer pays 1 emerald for 24 paper, 4 books or 4 blank media, as the librarian does; the
  IT Technician 1 emerald for 10 copper ingots or 8 tin ingots, and half its era's base for a part of an older era.
- **Uses:** every trade follows vanilla's (12 uses, restocked twice a day), except systems, the rare item, with 3.
- Every kind of village gets a new house, a **computer shop**, with both job sites, so the two professions appear
  without the player placing anything. It turns up in about half of the villages of each kind, as often as a
  library.
- JEI shows where each program is sold and found: "sold by the Programmer (level 3)", "found in a Midsoft office".

### The era a player reaches

A player reaches an era the first time they assemble and switch on a working computer of it ([Eras](eras.md)); in
survival that is the moment the chain pays off, and other mods can gate content on it.

### Settings

In `jscomputers-server.toml` ([Implementation](implementation.md)):

```toml
[survival]
own_chain = true                         # J's Computers' chain and its veins; off: J's Industrial's only, or none
working_ruin_chance = 0.2                # the share of abandoned buildings that still work
era_distances = [750, 1500, 3000, 5000]  # blocks from spawn where the Legacy, Transition, Standard and Advanced begin

[villagers]
programmer = true
it_technician = true
computer_shop = true                     # the computer shop in villages

[structures.datacenter]                  # one table per building
enabled = true
spacing = 112                            # chunks between two of them, on average
eras = ["legacy", "transition", "standard", "advanced"]
```
