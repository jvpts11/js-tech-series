# Survival

How a player gets every computer, part and program of J's Computers without the creative menu: the mod's own chain of
machines and benches, the Prebuilder and the Preinstaller that turn parts into working computers, and the abandoned
buildings and villagers where the software is found. Today none of it exists: everything comes from the creative tab,
and a modpack can write its own recipes and loot through KubeJS ([The API](api.md)).

## To build

### Two chains

J's Computers never depends on J's Industrial, so it has a chain of its own, and J's Industrial brings another:

- **J's Computers' chain is the easy one.** It follows a real electronics factory, from the ore to the chip, with real
  materials: the elements, compounds, gases, acids and resins of J's Core's catalogue, in its forms (crushed ore,
  lump, dust, ingot, nugget, plate, sheet, foil, wire, rod, boule, wafer, lens, crystal). Each element comes from one
  place in the world and one way of refining it; a gas may come out of more than one machine, as oxygen comes from
  the air and from water.
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
| J's Core's generic ore of the element | tin, lead, zinc, nickel, aluminium, tantalum, chromium, cobalt, silver, lithium, manganese, mercury, zirconium, tungsten, molybdenum; and vanilla's iron, copper and gold |
| A J's Core mineral | fluorine, from fluorspar (its element has no generic ore: its ores are salts) |
| A byproduct, in the Crusher | gallium (from aluminium ore), indium and germanium (from zinc ore), hafnium (from zirconium ore), arsenic (from copper and gold ore), ruthenium (from nickel ore), sulfur (from zinc and lead ore) |
| The Air Separation Unit | nitrogen, oxygen, argon, krypton |
| The Electrolytic Cell | hydrogen and oxygen from water; chlorine and caustic soda from salt; refined copper; aluminium from alumina |
| The Coke Oven | coke and coal tar from coal |
| The Chemical Reactor | salt, with traces of boron dust, by evaporating water; alumina from crushed aluminium ore and caustic soda; lithium hydroxide from crushed lithium ore; the batteries' electrolyte; phosphorus dust from bone meal; methanol from coke and steam |
| The Induction Furnace | steel from iron and coke; stainless steel; metallurgical silicon; calcium carbide |
| Mined and grown vanilla | sand (silica), calcite (calcium), bone meal from a composter, water, logs; glass, made as in vanilla; paper, made as in vanilla or from wood pulp (below) |

**The ores lie in veins** of J's Core's world kit, which J's Computers declares when no other mod has. The elements of
the later eras lie in rarer and deeper veins, the last ones only in the deepslate, so each era still takes the
player further across the world and further down. Every vein is in the Overworld:

| Era | Where its new ores lie | Spacing | Height |
| --- | --- | --- | --- |
| Vintage | the Overworld, near the surface: tin, lead, zinc, nickel, aluminium, tantalum, chromium, manganese, mercury, fluorspar | 12 chunks | y 0 to 80 |
| Legacy | deep in the Overworld: cobalt, silver, lithium (spodumene) | 16 chunks | y −16 to 40 |
| Transition | deep in the Overworld (indium, from the zinc already mined) | | |
| Standard | the deepslate: zirconium (and its hafnium), tungsten | 24 chunks | y −64 to 0 |
| Advanced | the deepslate: molybdenum (ruthenium comes from the nickel already mined) | 24 chunks | y −64 to 0 |
| Exa | set when the era is designed | | |

The spacing is the average distance between two veins of the same kind; a vein has the size of J's Core's kit
(about 3 by 3 chunks).

### The forms of each element

The forms the chain asks for, which J's Computers declares to J's Core; no other form of these elements exists
unless another mod asks for it. Where each metal becomes an ingot is in *The chain's numbers* below.

| Element | Forms |
| --- | --- |
| Iron | crushed ore, ingot, nugget, dust |
| Copper | crushed ore, ingot, nugget, plate, sheet, foil, wire, rod; refined copper as ingot and nugget |
| Gold | crushed ore, ingot, nugget |
| Tin | ore, raw, crushed ore, ingot, nugget |
| Lead | ore, raw, crushed ore, ingot |
| Zinc | ore, raw, crushed ore, ingot, dust |
| Nickel | ore, raw, crushed ore, ingot, foil |
| Aluminium | ore, raw, crushed ore, ingot, nugget, plate, sheet, foil |
| Tantalum, manganese | ore, raw, crushed ore, ingot, dust |
| Chromium, tungsten | ore, raw, crushed ore, ingot, nugget |
| Cobalt | ore, raw, crushed ore, ingot, nugget, dust |
| Silver | ore, raw, crushed ore, ingot, dust, foil |
| Lithium | ore, raw, crushed ore (it is only used as its compounds) |
| Mercury | ore, raw, crushed ore, liquid |
| Zirconium | ore, raw, crushed ore (only for its hafnium) |
| Molybdenum | ore, raw, crushed ore, ingot, plate |
| Gallium, indium | dust, ingot |
| Hafnium, ruthenium | dust, ingot, nugget |
| Germanium | dust (a crystal in J's Core, with no ingot) |
| Arsenic, sulfur, boron, phosphorus | dust |
| Silicon | lump (metallurgical silicon), rod (polysilicon), boule, wafer |
| Hydrogen, oxygen, nitrogen, argon, krypton, chlorine | gas |

Fluorine comes in fluorspar, J's Core's mineral: its ore in five rocks, raw, dust and crystal. An ore, its raw
form and its crushed ore take the colour of the mineral (red bauxite, green garnierite, lilac spodumene), as
vanilla's raw iron is the colour of rust; the ingot and every later form take the colour of the metal, each its
own hue so that no two read alike, as J's Core's materials page sets them.

### The chain's compounds in J's Core

The compounds, alloys, liquids and gases of the chain are materials of J's Core's catalogue, so that J's
Industrial, J's Space and any other mod trade the same ones; their recipes are J's Computers'. Each carries its
real formula and hazard classes, which its tooltip shows.

| Material | Formula | State | Comes as |
| --- | --- | --- | --- |
| Alumina | Al₂O₃ | solid | dust |
| Caustic soda | NaOH | solid | dust |
| Salt | NaCl | solid | dust |
| Zinc sulfide | ZnS | solid | dust |
| Lithium hydroxide | LiOH | solid | dust |
| Lithium cobalt oxide | LiCoO₂ | solid | dust |
| Calcium carbide | CaC₂ | solid | lump |
| Coke | C | solid | lump |
| Metallurgical silicon | Si, 98% | solid | lump |
| Polysilicon | Si, nine nines | solid | rod |
| Cultured quartz | SiO₂ | crystal | crystal |
| Gallium arsenide, aluminium gallium indium phosphide, gallium nitride | GaAs, AlGaInP, GaN | crystal | boule, wafer |
| Calcium fluoride | CaF₂ | crystal | boule, lens |
| Wood pulp | cellulose | solid | dust |
| Phenolic resin, epoxy resin, polycarbonate, PVC | their polymers | polymer | sheet |
| Steel | Fe-C | alloy | ingot, plate, sheet, rod |
| Stainless steel | Fe70Cr20Ni10 | alloy | ingot, plate |
| Solder | Sn60Pb40 | alloy | wire |
| Lead-free solder | Sn-Ag-Cu | alloy | wire, nugget (its balls) |
| Permalloy | Ni80Fe20 | alloy | ingot, foil |
| Indium tin oxide | In₂O₃·SnO₂ | alloy | ingot, foil |
| Refined copper | Cu, 99.99% | metal | ingot, nugget |
| Coal tar, photoresist, liquid crystal | mixtures | liquid | fluid |
| Battery electrolyte | LiPF₆ in organic carbonates | liquid | fluid |
| Phenol | C₆H₅OH | liquid | fluid |
| Methanol | CH₃OH | liquid | fluid |
| Hydrofluoric acid | HF in water | liquid | fluid |
| Sulfuric acid | H₂SO₄ | liquid | fluid |
| Ferric chloride | FeCl₃ in water | liquid | fluid |
| Ultrapure water | H₂O | liquid | fluid |
| Hydrogen chloride | HCl | gas | gas |
| Acetylene | C₂H₂ | gas | gas |

The gases of the elements (hydrogen, oxygen, nitrogen, argon, krypton, chlorine) and liquid mercury are elements of
the catalogue already. A gas is kept in pressurised tanks, travels in gas pipes and shows by its colour, never a
texture: the colour its cylinder has in the European code (EN 1089-3), hydrogen red, oxygen white, nitrogen black,
argon dark green, krypton bright green, chlorine yellow, hydrogen chloride yellow ochre, acetylene maroon.

### The material of each era

Each era asks for what its real hardware was made of. The amounts are first estimates.

| Era | New materials | What they are for, as in the real hardware |
| --- | --- | --- |
| Vintage | silicon, phosphorus and boron, aluminium, tin and lead, nickel, tantalum, alumina, phenolic resin, zinc sulfide, ferrite, mercury, chromium, steel and stainless steel | chips; their dopants; the wiring inside the chip and disk platters; solder and the lead-acid batteries of UPSes; permalloy heads; capacitors; ceramic chip packages; FR-2 boards (paper and phenolic resin from coal tar); the phosphor of tube monitors; inductors and motor magnets; the lithography's mercury lamp; its masks (chromium on quartz); cases; the vacuum chambers |
| Legacy | gold, glass fibre and epoxy, cobalt, gallium and arsenic, silver, polycarbonate, lithium, indium tin oxide and liquid crystal, krypton and fluorine | bonding wire and contacts; FR-4 boards; the platters' magnetic layer; the gallium arsenide laser of CD drives; thermal paste; compact discs; the lithium-ion batteries of the first portables (lithium cobalt oxide); their small liquid crystal screens; the krypton fluoride laser of the lithography |
| Transition | copper inside the chip, lead-free solder (tin, silver, copper), aluminium gallium indium phosphide, argon, calcium fluoride, ultrapure water | the copper wiring of around 2000; lead-free solder; flat desk monitors; the laser of DVD drives; the argon fluoride laser of the lithography, its lenses and immersion |
| Standard | hafnium, gallium nitride, germanium, tungsten | the high-k gate of 2007; LEDs and the blue laser of Blu-ray drives; the core of optical fibre; the vias |
| Advanced | molybdenum, tin droplets, cobalt, ruthenium | the mirrors and the tin plasma light of extreme ultraviolet lithography; the wiring of the modern nodes |
| Exa | set when the era is designed | |
| Singularity | computronium | everything ([The cosmological simulator](simulator.md)) |

Without J's Industrial, the computronium of the Singularity comes from the cosmological simulator.

### The benches

- The vanilla crafting table makes the **Vintage Electronics Workbench** and every block of the machines that has
  no electronics in it. What is electronic is made with the mod's benches and assembly lines, and the materials
  and components with its machines ([Recipes](recipes.md)).
- There is an **Electronics Workbench** per era (Vintage, Legacy, Transition, Standard, Advanced; the Exa and the
  Singularity get theirs with their eras). A bench assembles the parts of its era and of the eras before it, and the
  bench of the next era.
- **A bench is the player's, by hand, and never automated:** it takes no items through its sides and no Crafting
  Interface. It draws energy through the socket on its back (40, 60, 300, 80 and 120 W from the Vintage to the
  Advanced, estimates) and works as a furnace does: each part takes its time, the window may be closed meanwhile, and
  the finished part waits in the output tray. Its window has nine trays that take the recipe's ingredients in any order,
  each with the count the part asks for; the progress groove in the colour of the era's tool; the part, faded until it
  is done; and the power strip with its lamp, its draw and the energy it holds.
- To make parts on their own, each era has its **Electronics Assembly Line** (below), which runs its era's bench
  recipes.
- **A machine's blocks** are the casing of its duty (Machine, Refractory, Lined, Vacuum or Clean Room), the same
  entries as every machine, its terminal and the working blocks that make it what it is (a Crusher's rolls, a
  Rolling Mill's work rolls). The mechanical ones come from the Crafting Table, so the network's crafting patterns
  make them from the start; the electronic ones (the terminal, the peripheral port, a laser, a placement head) from
  the bench of their era by hand, and then from its Electronics Assembly Line. The Vintage machines come in three
  steps, each made only of what the one before makes: from vanilla things, from the first metals, and the chip
  factory. Every recipe is in [Recipes](recipes.md).

### The machines

Every machine is a multiblock of its own blocks and its terminal, with its entries on the places it declares, as every
multiblock of the series, built with J's Core's multiblock kit (four rotations) and shown by the Structure
Projector. The sizes are first estimates; their looks are drawn and approved before any code. Every machine takes the
Crafting Interface, so the network's autocrafting runs the whole chain ([Autocrafting](autocrafting.md)).

| Group | Machine | Size (wide, tall, deep) | What it does |
| --- | --- | --- | --- |
| Ore and metals | Crusher | 3x3x3 | ore into crushed ore and its byproducts; a toothed double-roll crusher (J's Industrial's Macerator is a jaw crusher) |
| | Grinder | 2x2x2 | ingots, crystals and glass into dust |
| | Induction Furnace | 3x3x3 | dusts into ingots and alloys; sand and coke into metallurgical silicon; coke and calcite into calcium carbide |
| | Electrolytic Cell | 3x2x2 | hydrogen and oxygen from water, chlorine from salt, refined copper, aluminium from alumina; the first charge that forms a battery |
| | Rolling Mill | 3x2x2 | ingot into plate, sheet and foil |
| | Wire Drawer | 3x2x1 | ingot into rod and wire, bonding wire included |
| Chemistry and gases | Coke Oven | 3x3x3 | coal into coke and coal tar |
| | Chemical Reactor | 3x3x2 | salt and its boron from water; alumina from aluminium ore and caustic soda; hydrogen chloride, hydrofluoric and sulfuric acid, ferric chloride; phenol, methanol, phenolic and epoxy resins, polycarbonate; PVC through calcium carbide (from coke and calcite in the Induction Furnace) and acetylene; photoresist; cultured quartz; phosphorus from bone meal; ultrapure water, zinc sulfide, liquid crystal, thermal paste |
| | Air Separation Unit | 3x3x4 | air into nitrogen, oxygen, argon and krypton |
| Silicon | Siemens Reactor | 2x3x2 | metallurgical silicon, hydrogen chloride and hydrogen into polysilicon |
| | Crystal Puller | 2x3x2 | polysilicon and a dopant into a boule; also gallium arsenide, aluminium gallium indium phosphide, gallium nitride and calcium fluoride |
| | Wafer Saw | 2x2x2 | a boule into wafers, cut and polished flat |
| The chip factory | Lithography Station | 3x3x2 | a wafer's layer exposed through its mask; one per era |
| | Plasma Etcher | 2x2x2 | the exposed layer etched |
| | Ion Implanter | 3x2x2 | the layer doped |
| | Deposition Chamber | 2x2x2 | metal laid on the layer (aluminium, copper, tungsten, cobalt), the magnetic layer of disk platters and floppy film, the chromium of a mask blank, the gold electrodes of oscillator crystals |
| Packaging | Packaging Line | 3x2x2 | a finished wafer cut into dies, each packaged: ceramic in the Vintage, epoxy and pins later, solder balls from the Transition |
| Boards and components | Laminator | 3x2x2 | paper or glass fibre and resin, clad in copper foil |
| | Board Etcher | 3x2x2 | a clad laminate etched into a board's traces |
| | Ceramic Kiln | 2x2x2 | alumina packages, ceramic capacitors, ferrites |
| | Coil Winder | 3x2x1 | wire into coils, motors and magnetic heads |
| Assembly | Electronics Assembly Line | 3x2x2 | its era's bench recipes, on their own; one per era, fed by its era's Crafting Interface |
| | Optical Polisher | 2x2x2 | quartz into mask plates, glass and calcium fluoride into lenses, polycarbonate into blank discs; draws glass fibre, and optical fibre from germanium-doped glass |
| Displays | Display Line | 3x3x2 | tube screens with their phosphor, flat screens with indium tin oxide, LED panels |

- **Entries** tell what they are by their connector and their band: green takes in, orange gives out, yellow is
  energy, and the peripheral socket is cyan. The Coke Oven needs no energy. The Chemical Reactor has two liquid and two
  gas inlets, the Air Separation Unit a gas outlet for each gas, the Siemens Reactor two gas inlets, the Crystal Puller
  one (the nitrogen of gallium nitride), the Plasma Etcher a fluid and two gas inlets, the Board Etcher one for its
  ferric chloride, the Display Line two fluid inlets (liquid crystal and mercury), and the Lithography Station a gas
  inlet for its laser (Legacy, Transition, Standard) and a liquid inlet for its immersion (Transition, Standard).
- **Every machine shows what it is processing:** it draws the recipe at work with its material, its colour, its part
  and its form, and gives off its particles in the colour of what comes out. A machine of a single recipe (the Coke
  Oven, the Air Separation Unit, the Siemens Reactor) always shows the same.
- **Particles** belong to the world: a machine gives them off only while it works (dust, sparks, smoke, mist), never
  as part of its model, and a closed machine breathes out now and then. Its beacon turns while it works. What is shut
  while a real machine works stays shut: the Ceramic Kiln's door closes while it fires.
- **The Electronics Assembly Line** of each era has the machines of its time: axial insertion and wave soldering in the
  Vintage, DIP insertion and wave soldering in the Legacy, a chip shooter and reflow in the Transition, a gantry and
  optical inspection in the Standard, two gantries and X-ray inspection in the Advanced. It takes its era's Crafting
  Interface on the Crafting Computer's crafting cable ([Autocrafting](autocrafting.md)).

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

- **Ore:** the Crusher turns 1 ore (the block or its raw form) into 2 crushed ores, J's Core's crushed form, with a
  chance of a byproduct's dust: gallium 25% (from aluminium), indium 15% and germanium 10% (from zinc), hafnium 25%
  (from zirconium), arsenic 15% (from copper and gold), ruthenium 5% (from nickel), sulfur 50% (from zinc and lead). A
  crushed ore, or a byproduct's dust, gives one ingot: in the vanilla furnace for the metals it melts (iron, copper,
  gold, tin, lead, zinc, silver, nickel, cobalt, manganese, gallium, indium), in the Induction Furnace for the
  refractory ones (tantalum, chromium, tungsten, molybdenum, zirconium, hafnium, ruthenium). Crushed aluminium ore
  never melts: it becomes alumina and then aluminium (below); crushed lithium ore neither, it becomes lithium
  hydroxide. Arsenic stays a dust, as the real one turns to vapour before it melts, and so does germanium, a crystal.
  Crushed mercury ore, roasted in the Induction Furnace, gives 100 mB of mercury.
- **Forms:** the Rolling Mill turns 1 ingot into 1 plate, 1 plate into 2 sheets and 1 sheet into 4 foils, of any metal
  or alloy; the ones the chain only uses as foil (nickel, silver, permalloy, indium tin oxide) go from 1 ingot
  straight into 8 foils. The Wire Drawer turns 1 ingot into 2 rods and 1 rod into 2 wires, and 1 gold or aluminium
  ingot into 16 bonding wires. The Grinder turns 1 ingot or crystal into 1 dust, and 1 log into 4 wood pulp, which
  the Rolling Mill presses into 2 paper. An ingot gives 9 nuggets and 9 nuggets an ingot, at the crafting table.
- **Alloys and furnace work** (Induction Furnace), in their real proportions: solder, 3 tin and 2 lead into 5;
  lead-free solder, 9 tin, 1 silver and a copper nugget into 10; permalloy, 4 nickel and 1 iron into 5; indium tin
  oxide, 9 indium and 1 tin into 10; steel, 2 iron and 1 coke into 2; stainless steel, 7 iron, 2 chromium and 1
  nickel into 10; metallurgical silicon, 1 sand and 1 coke;
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
  | 1 salt (and 1 boron dust every 16) | 4,000 mB of water, evaporated (60 seconds) |
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
  | 1 phosphorus dust | 4 bone meal, 1 coke and 1 sand |
  | 500 mB of ultrapure water | 1,000 of water |
  | 1 zinc sulfide | 1 zinc dust and 1 sulfur |
  | 250 mB of liquid crystal | 500 of phenol and 1 fluorspar dust (real liquid crystals are fluorinated) |
  | 4 thermal paste | 1 silver dust and 1 zinc dust |
  | 1 lithium hydroxide | 1 crushed lithium ore, 1 caustic soda and 1,000 of sulfuric acid (the acid leaches the ore, the soda turns its sulfate into the hydroxide, as the real plants do) |
  | 1,000 mB of battery electrolyte | 1 lithium hydroxide, 1 phosphorus dust and 500 of hydrofluoric acid (lithium hexafluorophosphate in its solvent) |

  Hydrogen chloride and acetylene are gases, as at room temperature: the reactor takes and gives them through its
  gas entries.

- **Components:**

  | Machine | What it makes |
  | --- | --- |
  | Ceramic Kiln | 1 alumina into 4 ceramic packages; 1 alumina and 1 nickel foil into 8 ceramic capacitors (nickel electrodes, as in the real ones); 1 tantalum dust and 1 manganese dust into 8 tantalum capacitors (the real anode of pressed tantalum powder and its manganese dioxide cathode); 1 lithium hydroxide and 1 cobalt dust into 1 lithium cobalt oxide, the cathode of lithium-ion cells; 2 iron, 1 manganese and 1 zinc dust into 4 ferrites (manganese zinc) |
  | Coil Winder | 1 ferrite and 2 copper wires into 2 inductors; 2 ferrites and 4 copper wires into 1 transformer; 1 steel rod, 4 copper wires and 1 ferrite into 1 electric motor; 1 permalloy foil and 1 copper wire into 2 magnetic heads; 4 copper wires into 1 copper coil |
  | Optical Polisher | 1 cultured quartz into 1 mask plate; 1 glass into 4 lenses; 1 calcium fluoride boule into 4 lenses; 1 polycarbonate into 4 blank discs; 1 glass and 1 alumina into 8 glass fibres (E-glass has alumina in it); 1 glass and 1 germanium dust into 8 optical fibres |
  | Deposition Chamber | 1 mask plate and 1 chromium nugget into 1 mask blank; 1 cultured quartz and 1 gold nugget into 4 oscillator crystals (their electrodes); 1 aluminium sheet and 1 iron dust into 1 platter in the Vintage, 1 aluminium sheet and 1 cobalt nugget from the Legacy; 1 PVC and 1 iron dust into 4 floppy films |
  | Laminator | FR-2 (Vintage): 2 paper, 1 phenolic resin and 1 copper foil; FR-4 (from the Legacy): 2 glass fibres, 1 epoxy resin and 1 copper foil |
  | Board Etcher | a clad laminate and 250 mB of ferric chloride into an etched board, which takes 1 laminate in the Vintage and the Legacy, 2 in the Transition, 3 in the Standard and 4 in the Advanced, as real boards gained layers |
  | Display Line | a tube (Vintage, Legacy): 4 glass, 1 lead, 1 zinc sulfide, 2 coils; a liquid crystal screen (from the Legacy, for the first portables, and on desks from the Transition): 2 glass, 1 indium tin oxide foil, 250 mB of liquid crystal and a cold cathode lamp (1 glass and 100 mB of mercury, made on the same line); the Standard's, the same with 8 gallium nitride LEDs instead of the lamp; the Advanced's, 2 glass, 1 indium tin oxide foil and 16 LEDs, a panel that lights itself |
  | Electrolytic Cell | 2 lead ingots, 1 PVC and 250 mB of sulfuric acid into 1 lead-acid battery (a 12 V, 7 Ah block, about 300 kJ); 1 lithium cobalt oxide, 1 coke (the graphite of the anode), 2 aluminium foils (the cathode's foil and the pouch), 1 copper foil (the anode's) and 250 mB of battery electrolyte into 1 lithium-ion cell. A real battery is filled and then formed by its first charge, which is electrolysis |

### How a chip is made

1. Sand and coke make **metallurgical silicon** in the Induction Furnace.
2. With hydrogen chloride it becomes trichlorosilane, and with hydrogen, in the Siemens Reactor, **polysilicon**: 1
   metallurgical silicon, 1,000 mB of hydrogen chloride and 500 of hydrogen give 1 polysilicon.
3. The Crystal Puller grows a **boule** from 4 polysilicon and 1 dopant (phosphorus or boron); the Wafer Saw cuts it
   into 8 **wafers** and polishes them flat.
4. Each layer of the chip goes through the **Lithography Station** (photoresist, the era's light and its mask), the
   **Plasma Etcher**, the **Ion Implanter** and the **Deposition Chamber**. A chip of a later era has more layers: 2 in
   the Vintage, 3 in the Legacy, 4 in the Transition, 6 in the Standard, 8 in the Advanced. Between the machines it
   is a **wafer in process**, whose tooltip names its chip, the layer it is on and the machine it goes to next.
5. The **Packaging Line** cuts the finished wafer into dies and packages each into a **chip**, by the masks used:
   a memory and a controller chip of each era, and a processor and a graphics chip for each microarchitecture of
   each era, as the catalogue names them, so a Haswell is made of Haswell dies. The dies of the parts the
   catalogue does not list are chips of their own too: Quince's (the Motoroller 68000, the PowerQ G3 and G4, the
   Quince Q1 to Q3), the Phi's MIC (Standard), Blockwright's hashing chip (Standard, Advanced), and the portables'
   four families (Hightide in the Legacy, the Transition and the Standard; Quince qA in the Transition, the
   Standard and the Advanced; Motoroller MX in the Transition; Sunsung Exo in the Standard and the Advanced).

What each layer uses, by machine:

| Machine | Every layer |
| --- | --- |
| Lithography Station | 100 mB of photoresist; in the Legacy also 10 mB of krypton (the laser's gas); in the Transition and the Standard also 10 mB of argon and 100 mB of ultrapure water (the immersion); in the Advanced also 1 tin nugget every 4 layers (the plasma's droplets) |
| Plasma Etcher | 100 mB of hydrofluoric acid up to the Legacy; 100 mB of chlorine and 10 of argon from the Transition |
| Ion Implanter | 1 phosphorus or boron dust dopes 8 layers |
| Deposition Chamber | 1 nugget of the era's wiring metal: aluminium (Vintage, Legacy), refined copper (Transition), copper and tungsten in turn (Standard), cobalt and ruthenium in turn (Advanced); a Standard chip's first layer takes 1 hafnium nugget, the high-k gate |

A finished wafer gives 8 memory or controller chips, or 4 processor or graphics chips, and every chip takes the
package of its era: in the Vintage, 1 ceramic package and 2 aluminium bonding wires; in the Legacy, epoxy (1 resin for
4 chips), 2 gold bonding wires and copper foil (1 for 4 chips); from the Transition, epoxy and 1 lead-free solder
nugget, its balls.

The lithography's light follows the real one: a mercury lamp in the Vintage, a krypton fluoride laser in the
Legacy, an argon fluoride laser with calcium fluoride lenses and immersion in ultrapure water in the Transition
and the Standard, extreme ultraviolet from tin plasma and molybdenum mirrors in the Advanced. An excimer laser uses
its gas, 10 mB of krypton or argon a layer, through its station's gas inlet.

**Masks.** A mask serves one kind of chip of one era (a processor or graphics mask, one microarchitecture), sits in
the station's mask slot and is never used up. Its blank is a mask plate from the Optical Polisher with 1 chromium
nugget laid on it in the Deposition Chamber. The Vintage station writes it by photo-reduction from a **Layout**, the
chip's drawing cut in PVC film at the Vintage bench, as real layouts were cut in film and photographed down: the
blank and the Layout give the mask, and the Layout is used up ([Recipes](recipes.md)). From the Legacy on, the
linked computer writes the mask.

**Lasers and LEDs.** The Crystal Puller also grows the boules of light: gallium arsenide (Legacy) from 2 gallium and 2
arsenic; aluminium gallium indium phosphide (Transition) from 1 aluminium, 1 gallium, 1 indium and 1 phosphorus;
gallium nitride (Standard) from 2 gallium and 1,000 mB of nitrogen. The Wafer Saw cuts each into 8 wafers, which
take one layer through the laser mask (from the Legacy) or the LED mask (from the Standard), and the Packaging
Line cuts a finished one into 16 laser diodes or 32 LEDs. It grows calcium fluoride from 4 fluorspar dust
(fluorspar is calcium fluoride), and the Optical Polisher cuts each boule into 4 lenses.

### Chips are made by the computers of the era before

From the Legacy on, an era's Lithography Station only works linked by peripheral cable, on its peripheral socket (a
block of the station's own), to a computer of the era before or later, switched on and running: the computer runs the
lithography. Every layer takes 20 seconds, and while it exposes the station runs on the computer as the `litho`
process, holding memory: 2 MB for the Legacy's station, 32 MB for the Transition's, 256 MB for the Standard's, 2 GB
for the Advanced's. A computer two eras newer than the one asked for exposes in half the time. The Vintage station
needs no computer, since there is no era before it. Making a new era's computers is the work of the previous era's.
Every era's station is the same body with its era's light: a mercury lamp in the Vintage, a KrF laser in the Legacy,
an ArF laser with immersion in the Transition and the Standard, EUV in the Advanced.

### The parts

The generic components of J's Core's catalogue that J's Computers uses (electric motors, LEDs, oscillators, antennas,
magnetic heads, lead-acid batteries, lithium-ion cells) get their recipes here, as J's Core's design asks of every
mod that uses them without J's Industrial. At its era's bench, every part is assembled from the chain's pieces;
what each part, computer, block and item is made of is in [Recipes](recipes.md).

**Software is never crafted:** systems and programs are found, bought, copied or downloaded (below,
[Programs](programs.md)), and so are the media pressed with them, such as an installer disc: a player finds
one exploring, or buys it, and may copy it onto the blank media the bench makes.

### The Prebuilder

A 3x3x2 multiblock, one machine that builds the computers of every era, made of Vintage materials. It is optional:
assembling a computer by hand works as it does today.

- It takes a computer's case and its parts and puts them together, by the same rules as the computer's assembly window,
  and when it refuses it says why ([Computers](computers.md)).
- What comes out is the **assembled computer** as an item, with its parts inside, listed in its tooltip with the
  firmware under the board, the system under the disk and what it still lacks (the Preinstaller, or its first
  self-test). Placed, it is the assembled computer, switched off and owing its first self-test.
- It shows its work: the computer's own case rises on its lift with its open side to the front, the mod's own parts
  stand on two trays, and two robot arms take turns putting them, one at a time, into the seats where the computer
  draws them; done, the computer comes down and leaves on the right (about 10 seconds a computer, an estimate). Its
  blocks are the Case Lift, two Robot Arms and the Parts Tray, with its terminal.
- It builds every computer that is one block: the Personal Computer, the Crafting Computer, the Cluster Management
  Computer, the small computers to come, and every rack server chassis. The Mainframe and the other big blocks are
  assembled where they stand.
- It takes the Crafting Interface, so the network orders computers like any other craft.

**Breaking a computer gives the whole computer**, with its parts inside, as a rack server does today. Parts come out by
opening the case.

### The Preinstaller and the firmware

A 2x2x2 multiblock, one machine that writes the firmware and the systems of every era, made of Vintage materials,
because the first computer already needs it.

- **A motherboard comes out of the chain with no firmware.** A computer with a blank board powers its fans and
  nothing more: the monitor has no signal, and the assembly window shows NO FIRMWARE in amber, its readings blank
  and its disk lamp off.
- It does one job at a time: its cradle takes a computer in through a shutter and writes its firmware and its system;
  its board fixture writes a loose motherboard, lying flat; its disk dock writes a loose disk. Its console shows two
  bars, the firmware in amber and the system in green. Its blocks are the Cradle, the Console, the Board Fixture and the
  Disk Dock.
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
run; once computers draw energy, a bank of charged J's Core basic batteries keeps them going for about a game day,
then they go dark until the batteries are charged again. The rest are ruins, with their loot in chests and inside
the broken machines (parts in the cases, boxed software on the shelves, media, cables, whole computers, manuals).

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
| Crypto mining farm | Standard, Advanced | 72 | Mining Computers and ASICs mining ([Teracoin](teracoin.md)) |
| Datacenter | Legacy to Advanced | 112 | racks of servers, a Server Router, cables; large |
| Supercomputing centre | Standard, Advanced | 160 | supercomputer racks and Phi coprocessors; the rarest |

- **The loot:** every chest draws 4 to 8 items from its building's table and era; a broken computer keeps each of its
  parts with a 50% chance; boxed software comes from the programs of the building's era, and a house's office only
  holds its house's (a Midsoft office only Midsoft's).
- **A graphical system is always within reach:** every building of every era holds either a system with a
  graphical interface, a system without one and a desktop for that one, or at least a system without one and a
  desktop for it. In the Vintage, where no system has its own, that is UNIX System V with CDE.
- **How they look:** every building is made of the game's blocks with the mod's machines, the same in every era, only
  its era's content changing (computers, monitors, racks, Mainframes, the network, boxes, posters). A working one stands
  whole, its lights on, its machines running its era's systems; a ruin has broken windows, holes in its walls, its flat
  roof fallen in parts, moss, vines and cobwebs, its machines open with half their parts in their seats. Every computer
  stands on a desk of upside-down wooden stairs (their steps at both ends when it stands free in a room), its case and
  its monitor side by side, a chair in front. The signs are in English and never name an invented company.
- The **hardware box** is a loot block of its own, printed with its era's machine.
- **Each building's own case:** the electronic waste dump works through its office's computer; the delivery van never
  works; a software house office draws one of the houses (never Midsoft) from the era of its first product, in that
  house's colour; a ruined Midsoft office has been looted; a ruined bank's vault has been broken open; the game studio
  has its era's top graphics card; the electronics factory has six of the chain's machines, formed and working, or
  broken back into their blocks in a ruin; the hacker's den is in the basement of an ordinary house.

The buildings are J's Computers' own. Each one can be switched off in the settings, with its spacing and the eras it
may be of.

### Villagers

Two professions, each with its own job site, made at the Crafting Table ([Recipes](recipes.md)):

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
- **Trades:** each level offers two or three, drawn from its era's catalogue; blank media are its era's, and network
  cables its era's, in packs of 8.
- **Their look** is the game's villager model: the Programmer in a hoodie, a badge and glasses; the IT Technician in a
  cap, a khaki shirt and a tool belt.
- Every kind of village (plains, desert, savanna, snowy and taiga) gets a new house in its own materials, a **computer
  shop**, with both job sites and a Vintage demo computer running MC-DOS, so the two professions appear without the
  player placing anything. It turns up in about half of the villages of each kind, as often as a
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
