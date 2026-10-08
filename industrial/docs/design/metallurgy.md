# Metallurgy

How ore becomes metal, how metal becomes plates, sheets, wires and the rest, what the scraps are for, how steel is made,
and the alloys. The materials and their forms are in [Materials](materials.md); the machines, with their numbers, in
[Machines](machines.md).

## What exists today

- The **Macerator** grinds any iron ore or raw iron into 2 iron dusts, and an iron ingot back into 1 dust.
- The **Compressor** presses an iron or copper ingot into a plate.
- The **Electric Furnace** smelts what a furnace smelts, on energy.
- Iron dust smelts into an iron ingot in a furnace or a blast furnace.

## To build

### From ore to metal, in three levels

Every level is optional, and each is better than the one before:

| Level | What goes in | How | What comes out |
| --- | --- | --- | --- |
| 1. The furnace (the start) | an element's **"X ore"** and the simple minerals (native metals, oxides, carbonates) | vanilla's furnace or the Electric Furnace | **1 ingot**, no byproducts, as in vanilla |
| 2. The Macerator (T1) | any mineral | ground, then smelted | **2 dusts**, with a little of its byproducts |
| 3. The family's route (later) | any mineral | the real process of its family | **more yield** and **all** its byproducts |

**Sulfides and arsenides don't go in a furnace**: like the real ones, they have to be roasted first. At the start, a
player smelts the "X ore" that every vein holds ([The world](world.md)) and the simple minerals; the rest of the vein is
worth processing properly later. That is what the "X ore" is for.

The routes of level 3, by family ([Materials](materials.md)):

- **sulfides**: froth flotation, then roasting, which gives an oxide and sulfur dioxide, and from it sulfuric acid;
- **carbonates**: calcining, which gives carbon dioxide;
- **oxides**: reduction, in a blast furnace or an electric furnace;
- **copper**: also leaching and electrowinning;
- **aluminium**: the Hall-Héroult process, with cryolite as the flux;
- **titanium**: chlorinated into titanium tetrachloride, then the Kroll process, with magnesium as the reducer;
- **gold**: cyanidation, then activated carbon;
- **arsenides**: arsenic comes out as a byproduct, and has to be handled with care.

The exact yields (3 or 4 per family at level 3) and the machine of every step are decided with [Machines](machines.md)
and [Chemistry](chemistry.md).

### Forms, and who makes them

| Form | From | By hand, at the start (yields less) | Machine | Machine yield |
| --- | --- | --- | --- | --- |
| Plate | ingot | **Forge Hammer** | Compressor (T1); Hydraulic Press (T2, faster) | 1 → 1 |
| Sheet | plate | | Rolling Mill (T2) | 1 → 2 |
| Foil | sheet | | Rolling Mill, a second pass | 1 → 4 |
| Wire | ingot | **Draw Plate** | **Wire Drawer (T1)**; Industrial Wire Drawer (T3, multiblock, for superconducting wire) | 1 → 2 |
| Coil | wire | crafting table | Coil Winder (T1) | 4 wires → 1 |
| Spring | wire | | Coil Winder, spring mode | 1 → 1 |
| Rod | ingot | | Lathe (T1, 1 → 1); Extruder (T2) | 1 → 2 |
| Bolt, gear, profiles | rod, plate | crafting table | Extruder or Hydraulic Press with the form's **die** | by form |
| Frame | rods | crafting table | | |
| Dust | ingot | **Mortar** | Macerator | 1 → 1 |
| Nugget, block | ingot | crafting table, 9 for 1 as in vanilla | | |

- **The hand tools** (the Forge Hammer, the Draw Plate, the Mortar) yield less than the machines, so the start of the
  game never waits on a machine.
- **The Wire Drawer comes at T1**, because the T1 circuit already needs copper wire; the big multiblock one is for the
  special wires of later tiers.
- **Dies** decide the shape of the small parts in the Extruder and the Hydraulic Press (bolts, gears, profiles), instead
  of one machine per part, as real factories do. The dies are made of hot-work tool steel (below).
- The **Precision Tooling** upgrade raises the yields ([Machines](machines.md)).

### Scraps

Rolling and drawing leave **scraps** of the metal they work, a form of every metal: copper scraps, steel scraps. An
alloy's scraps go back to the same alloy.

- They come out in **fixed proportion**: the machine counts, and every so many operations one scrap comes out. Nothing
  is rolled at random.
- The **Electric Arc Furnace** (T3) melts them back into ingots, losing nothing.
- Before the T3, an ordinary furnace melts them **with a loss** (for example 2 scraps into 1 nugget), so nobody keeps a
  chest of scraps waiting for the T3.

### Steel

Steel is the **gate of the T3**: there is no steel before it, and the big multiblocks are made of it. The multiblocks
before it are of brick, wood and iron, like the Coke Oven. Steel is made by the real chain:

| Step | Machine | In | Out |
| --- | --- | --- | --- |
| Coke | **Coke Oven** (T1, a brick multiblock) | coal | coke, and **coal tar** for [Chemistry](chemistry.md) |
| Pig iron | **Blast Furnace** (T3, a multiblock) | iron ore or dust, coke, and **limestone** as the flux | pig iron and slag |
| Steel | **Oxygen Converter** (T3) | pig iron and oxygen | steel |
| Recycled steel | **Electric Arc Furnace** (T3) | steel scraps and scrap | steel |

- The limestone comes from vanilla's calcite; the oxygen from the T1 electrolyser.
- **Slag** is not waste: it makes cement and paving, a material for J's Civil Works when both mods are installed.
- Pig iron also casts into **cast iron**: heavy blocks and parts.
- Before steel, a clay **bloomery** gives **wrought iron**, the real iron of the first smiths.

### Alloys

About a hundred real alloys, each with a use in the series. Proportions are the real ones, rounded; the balance comes
later, and the machine of each (a mixer and a furnace, the steel furnaces, a vacuum furnace for reactive metals,
sintering for magnets and carbides, the folding forge for Damascus) is decided with [Machines](machines.md).

**Simple alloys** (mixer and furnace, T1 to T2)

| Alloy | Made of | Use |
| --- | --- | --- |
| Bronze | copper 3, tin 1 | tools, gears |
| Arsenical bronze | copper, arsenic | the first bronze in history: a bronze without tin for the start |
| Phosphor bronze | copper, tin, phosphorus | springs, electrical contacts |
| Aluminium bronze | copper, aluminium | propellers, tools that make no sparks |
| Silicon bronze | copper, silicon | marine fasteners |
| Gunmetal | copper, tin, zinc | valves, castings |
| Bell metal | copper, tin | bells |
| Speculum metal | copper, tin | telescope mirrors (with J's Space) |
| Brass | copper 3, zinc 1 | parts, valves |
| Naval brass | copper, zinc, tin | parts in sea water (with J's Oceanics) |
| Nickel silver | copper, nickel, zinc | contacts, instruments, keys |
| Tin-lead solder | tin 3, lead 2 | soldering |
| Lead-free solder | tin, silver, copper (or bismuth) | soldering electronics |
| Cupronickel | copper 3, nickel 1 | heat exchangers, pipes |
| Constantan | copper, nickel | thermocouples, resistors |
| Manganin | copper, manganese, nickel | precision resistors |
| Nichrome | nickel 4, chromium 1 | heating elements (the electric furnaces) |
| Invar | iron, nickel | precision instruments |
| Pewter | tin, antimony, copper | utensils, decoration |
| Sterling silver | silver 92.5 %, copper | contacts, decoration |
| Lead-antimony | lead, antimony | the grids of lead-acid batteries |
| Lead-calcium | lead, calcium | the grids of maintenance-free batteries |
| Type metal | lead, antimony, tin | printing type |
| Babbitt | tin, antimony, copper | machine bearings |
| Zamak | zinc, aluminium, magnesium, copper | die castings |
| Wood's metal, Field's metal, Rose's metal | bismuth, lead, tin, indium... | melt at 60 to 100 °C: thermal fuses, sprinklers |

**Irons and steels** (T3 on, except wrought iron)

| Alloy | Made of | Use |
| --- | --- | --- |
| Wrought iron | iron with almost no carbon, from the bloomery | chains, railings, early structures |
| Cast iron, nodular cast iron, white cast iron | from pig iron | heavy blocks; pipes and crankshafts; wear parts |
| Steel | the chain above | the gate of the T3 |
| Stainless steel | iron, chromium, nickel, manganese | chemistry, clean rooms |
| Surgical stainless steel | stainless with molybdenum | implants and prostheses (with J's Robotics) |
| High-strength low-alloy steel | steel with vanadium and niobium | structures, heavy multiblocks |
| High-speed steel | steel with tungsten, molybdenum, vanadium | cutting tools |
| Hot-work tool steel | steel with chromium, molybdenum, vanadium | the dies of the Extruder and the Press |
| Bearing steel | high-carbon chromium steel | bearings |
| Spring steel | high-carbon tempered steel | springs |
| Hadfield steel | steel with 13 % manganese | wear: crusher hammers, rails (with J's Transport) |
| Chromoly | steel with chromium and molybdenum | light, strong tubes and frames |
| Weathering steel | steel with copper, chromium, phosphorus; it makes its own protective rust | structures and façades (with J's Civil Works) |
| Silicon steel | iron, silicon | the cores of motors and transformers |
| Maraging steel | iron, nickel, cobalt, molybdenum | parts of extreme strength |
| Boron steel | steel with boron | neutron shielding, control rods |
| **Damascus steel** | layers of high-carbon steel and nickel steel, forge-welded and folded, which gives its wavy pattern | the best hand tools (durability); decorative blocks (with J's Civil Works) |
| Wootz steel | the crucible steel of India, which was forged into Damascus | elite blades and tools; the other road to Damascus |
| Metallic glass | amorphous iron, silicon, boron | high-efficiency transformer cores; very hard springs |

**Light alloys** (T3 to T4)

| Alloy | Made of | Use |
| --- | --- | --- |
| Duralumin | aluminium, copper, magnesium | light structures |
| Silumin | aluminium, silicon | engine blocks, castings |
| Aluminium-magnesium | aluminium, magnesium | hulls (with J's Oceanics and J's Transport) |
| Aluminium-zinc | aluminium, zinc | the strongest aircraft structures |
| Aluminium-lithium | aluminium, lithium | aerospace, lighter than duralumin |
| Aluminium-scandium | aluminium, scandium | light and strong structures |
| Titanium 6Al-4V | titanium, aluminium, vanadium | aerospace; prostheses (with J's Robotics) |
| Titanium aluminide | titanium, aluminium | turbine blades |
| Magnesium alloy | magnesium, aluminium, zinc | light parts |
| Beryllium copper | copper, beryllium | springs, tools that make no sparks |

**Nickel, cobalt, superalloys and refractory metals** (T4 to T5)

| Alloy | Made of | Use |
| --- | --- | --- |
| High-temperature alloy | nickel, chromium, cobalt, molybdenum | turbines, furnaces; with rhenium, the single-crystal superalloy |
| Hastelloy | nickel, molybdenum, chromium | acid tanks and pipes |
| Monel | nickel, copper | chemistry, sea water |
| Stellite | cobalt, chromium, tungsten | drill bits, valve seats |
| Cobalt-chromium | cobalt, chromium | implants, prostheses |
| High-entropy alloy | cobalt, chromium, iron, manganese, nickel | cryogenic tanks; it gets tougher in the cold |
| Tungsten carbide | tungsten, carbon | cutting tools, the drill bits of the Core Miner |
| Tungsten heavy alloy | tungsten, nickel, iron | radiation shielding, counterweights |
| Tungsten-copper | tungsten, copper | heat sinks, electrodes |
| Tungsten-rhenium | tungsten, rhenium | thermocouples for extreme heat, filaments |
| Copper-chromium-zirconium | copper, chromium, zirconium | welding electrodes; the fusion reactor's heat sinks |
| Glidcop | copper strengthened with alumina | accelerator parts that carry current and heat |
| Platinum-iridium | platinum, iridium | electrodes, spark plugs |
| Platinum-rhodium | platinum, rhodium | high-temperature thermocouples |
| Palladium-silver | palladium, silver | membranes that purify hydrogen |
| Zircaloy | zirconium, tin | nuclear fuel cladding |

**Functional alloys**

| Alloy | What it does | Use |
| --- | --- | --- |
| Nitinol | nickel and titanium; it remembers its shape | actuators, robots (with J's Robotics) |
| Kovar | iron, nickel, cobalt; it expands like glass | sealing metal into glass: vacuum tubes, chip packages |
| Alloy 42 | iron, nickel | the leads of chips (packaging, in the [Clean Room](clean-room.md)) |
| Elinvar | its elasticity doesn't change with heat | watch springs, instruments |
| Chromel and alumel | the pair of a type K thermocouple | the machines' temperature sensors |

**Magnets**

| Magnet | Made of | When |
| --- | --- | --- |
| Ferrite | iron oxide with strontium or barium | the first |
| Alnico | aluminium, nickel, cobalt, iron | the strong magnet before rare earths |
| Samarium-cobalt | samarium, cobalt | stands heat |
| Neodymium (NdFeB) | neodymium, iron, boron | the strongest |
| Mu-metal | nickel, iron, molybdenum | magnetic shielding for the accelerators |
| Permalloy | nickel, iron | transformer cores, the magnetic head |
| Permendur | iron, cobalt | high-power motors |
| Cobalt-chromium-platinum | cobalt, chromium, platinum | the magnetic layer of hard disk platters (J's Computers' disks, when both are installed) |
| Terfenol-D | terbium, dysprosium, iron; it changes shape in a magnetic field | actuators and sonar (with J's Oceanics) |

**Superconductors, hydrogen storage and liquid metals**

| Material | Made of | Use |
| --- | --- | --- |
| NbTi | niobium, titanium | low-temperature superconducting cable |
| Nb₃Sn | niobium, tin | the high-field magnets of accelerators |
| MgB₂ | magnesium, boron | a cheap superconductor |
| YBCO | yttrium, barium, copper, oxygen | high-temperature superconducting cable |
| BSCCO | bismuth, strontium, calcium, copper, oxygen | high-temperature superconducting tape |
| TBCCO | thallium, barium, calcium, copper, oxygen | a high-temperature superconductor |
| Lanthanum-nickel (LaNi₅) | lanthanum, nickel | stores hydrogen inside the metal: hydrogen tanks |
| NaK | sodium, potassium; liquid at room temperature | the coolant of fast reactors |
| Lead-bismuth | lead, bismuth | a reactor coolant |
| Galinstan | gallium, indium, tin; liquid | coolant and liquid-metal thermal paste |

**Nuclear alloys**

| Alloy | Use |
| --- | --- |
| Silver-indium-cadmium | the control rods of pressurised water reactors |
| Uranium-molybdenum, uranium-zirconium | metallic nuclear fuel |

**Meteoritic alloy**, a natural iron-nickel alloy, comes from J's Geology and J's Space and lives in J's Core; J's
Industrial takes it as an alternative in some recipes. Technical ceramics (silicon, boron, hafnium and tantalum
carbides) and cermets are not alloys: they belong to [Chemistry](chemistry.md). Alloys with the elements beyond the
known table are in [Exotic materials](exotic-materials.md).
