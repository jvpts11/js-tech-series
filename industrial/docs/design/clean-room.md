# Clean Room

Chips: the room they are made in, silicon and wafers, the process layer by layer, testing, cutting and packaging, the
hardware J's Industrial makes for J's Computers, and the other semiconductors. The circuits each tier makes are in
[Tiers and circuits](tiers-and-circuits.md).

A chip dies from one speck of dust on its circuit, and a fab is mostly a machine for keeping dust out. In J's
Industrial, keeping the room clean is the game.

## What exists today

Nothing: J's Computers makes every part of every era by its simple path, from vanilla materials and J's Core's shared
catalogue.

## To build

### The room

- **The Clean Room is a closed space the player builds, of variable size**: walls and ceiling of **clean room panels**,
  **Fan Filter Units** in the ceiling, which draw air through filters (HEPA at T4, ULPA at T5) and spend energy, and one
  **Airlock with an Air Shower**, the only door.
- Its **controller** counts the particles in the air and gives the room's **ISO class**, from 8 (dirty) to 1 (the
  cleanest), as real particle counters do.
- **The tools are machines of their own** (furnaces, steppers, etchers) that **only work inside a room of the class they
  ask for**. A bigger room holds more tools.
- **Cleanliness moves, as in real life**: one more Fan Filter Unit lowers the count, one without energy lets it rise;
  **walking in without a clean room suit** ([Tools and armour](tools-and-armour.md)) or without going through the Air
  Shower raises it a lot; opening the room to the outside without the Airlock, or dropping items on the floor, raises it
  too; the room cleans itself again over time, as fast as its filters allow.
- **Dirt costs yield, it doesn't forbid.** Each wafer gives a share of good chips, the **yield**, which falls as the
  particle count rises. A room at the edge of its class still works, and wastes wafers.
- **The class each tier asks for** (estimates): ISO 5 at T4, ISO 3 at T5, ISO 1 from T6. The discrete transistors of the
  T3 need no clean room.

### Silicon and wafers

Furnace silicon is 98 to 99% pure; a chip needs 99.999999999%, eleven nines. The real chain gets there:

| Step | Machine | Tier | What it does |
| --- | --- | --- | --- |
| 1 | Submerged Arc Furnace (multiblock) | T2 | quartz with charcoal and wood chips into **metallurgical silicon** (98%); the same furnace makes the **ferroalloys** of steel (ferrosilicon, ferromanganese, ferrochrome) |
| 2 | Trichlorosilane Reactor | T3 | silicon and hydrochloric acid into **trichlorosilane**, distilled until pure; it reacts with water and is toxic |
| 3 | Siemens Reactor | T3 | trichlorosilane and hydrogen over silicon rods at 1,100 °C into **polysilicon** of eleven nines (the 1950s); the leftover tetrachloride gives **fumed silica** |
| 4 | Czochralski Furnace | T3 | polysilicon and a dopant (boron or phosphorus) into a **single-crystal ingot** |
| 5 | Wafer Saw | T3 | ingot into wafers: an inner-diameter diamond saw at T3 and T4, a **multi-wire saw** at T5 |
| 6 | Wafer Polisher | T3 | lapping and polishing; chemical-mechanical planarisation comes at T5 with the process |

- Silicon grows in a **quartz crucible**, which serves **one ingot only**, as the real one. Oxide crystals, such as the
  **YAG of lasers**, grow in an **iridium crucible**, expensive and reusable.
- **Wafers grow with the tier, and with them the chips per wafer:** 50 mm (2 inches) at T3, as in the 1960s; 150 mm at
  T4, as in the 1980s; 300 mm at T5, as from the 2000s to today. A 300 mm wafer gives about 40 times the chips of a 50
  mm one, which is why the real industry grew its wafers.

**Other substrates**

| Substrate | Tier | For | In real life |
| --- | --- | --- | --- |
| Germanium (Zone Refiner) | T3 | the first transistors | 1947 to 1950 |
| Float-zone silicon | T4 | power devices | |
| Gallium arsenide | T4 | red LEDs, radio circuits | |
| Silicon carbide (by sublimation, from the Acheson Furnace's) | T5 | power electronics | |
| Sapphire | T5 | the base of blue LEDs | |
| Gallium nitride, on sapphire | T5 | the blue LED | Nakamura, 1993 |

### The process, layer by layer

**T3, discrete transistors, no clean room:** the wafer goes to the **Diffusion Furnace** (T3), which drives a dopant in
at 1,000 °C (Bell Labs, 1955); it is cut into dice, and the Precision Assembler closes each in a **metal can of Kovar**
with gold wire. There is no lithography.

**The tools of the Clean Room:**

| Step | T4 (1970 to 1990, ISO 5, 150 mm) | T5 (1990 to 2030, ISO 3, 300 mm) |
| --- | --- | --- |
| Lay down films | Oxidation Furnace (thermal oxide); **CVD Reactor** (oxide, nitride and polysilicon, from **silane**, which catches fire by itself in air) | the **ALD Reactor** as well (atomic layers: the **hafnium oxide** of transistor gates, 2007) |
| Resist | **Coater/Developer Track**: spreads the resist, bakes it and develops it | the same |
| Expose | UV Stepper | DUV Scanner, EUV Scanner, High-NA EUV Scanner |
| Etch | **Wet Bench** (hydrofluoric acid) and **Plasma Etcher** | the same, finer |
| Dope | **Ion Implanter**, with **arsine, phosphine and boron trifluoride**, **very toxic** gases ([Fluids and gases](fluids-and-gases.md)) | the same |
| Strip the resist | **Plasma Asher** | the same |
| Metallise | **Sputtering** of **aluminium** | **copper** by electroplating (damascene, 1997), and **tungsten** in the contacts, from tungsten hexafluoride |
| Planarise | none | **CMP Tool**, with a slurry of **ceria** or silica |
| Clean | **Wet Clean** (the RCA clean, with hydrogen peroxide) | the same |
| Layers per chip | about 15 | 40 to 80 |

**Photoresists:** at T4, novolac with DNQ, from the cresol of the Tar Still and formaldehyde
([Chemistry](chemistry.md)); at T5, **chemically amplified** resists (polyhydroxystyrene), for DUV; for EUV, the **tin
oxide** resists arriving today.

**Lithography in three generations:**

| Machine | Tier | What it is | In real life |
| --- | --- | --- | --- |
| UV Stepper | T4 | mercury lamp light, the g and i lines (436 and 365 nm); a big machine, but one block | GCA's and Nikon's steppers of the 1980s |
| DUV Scanner (multiblock) | T5 | an **excimer** laser, which spends **fluorine, krypton or argon, and neon** (the rare gases of [Fluids and gases](fluids-and-gases.md)); its **immersion** version exposes through a film of **ultra-pure water** | the 1990s and 2000s; the neon shortage of 2022 stopped chip factories |
| EUV Scanner (giant multiblock) | T5, top | drops of **tin** hit by a **carbon dioxide laser** become plasma that shines at 13.5 nm; the light travels **in vacuum**, reflected by **molybdenum-silicon mirrors**, and **hydrogen** cleans the tin off the mirrors | ASML's NXE, 2019, with Trumpf's laser and Zeiss' mirrors |
| High-NA EUV Scanner (the biggest multiblock of the mod) | T5, later | the same, with larger optics | ASML's EXE, 2024 to 2025 |

- **Multiple patterning.** A DUV scanner can already make T5 circuits, as TSMC made 7 nm chips, with **multiple
  exposures**: each layer goes through several times, spends more resist and gases and takes much longer. EUV makes the
  same layer in one exposure; High-NA makes the finest layers in one exposure where ordinary EUV needs two.
- **The EUV Scanner is built from modules, as the real one**, each a project of its own, fitted into the multiblock
  inside the Clean Room: the **EUV Source** (the tin droplet generator, 50,000 drops a second, the carbon dioxide laser
  and the collector mirror), the **Illuminator** and the **Projection Optics** (the multilayer molybdenum-silicon
  mirrors), and the **Reticle Stage** and **Wafer Stage**, tables in **magnetic levitation** precise to nanometres.
- The EUV Scanner draws about **1 MW** through its medium voltage port, as the real one (about 1.3 MW with its laser),
  spends tin and hydrogen, and needs an ISO 3 room.
- **Masks:** the **Mask Writer** (T4) draws the circuit on quartz and chrome with an electron beam. The mask, the
  reticle, wears slowly with use, as real ones do.

**How a wafer is followed: re-entrant flow**, as in real fabs:

- wafers travel in **lots**: a **cassette** of 25 wafers at T4, a **FOUP** at T5, the closed pod of real fabs;
- each lot **carries its progress**, and its tooltip reads "layer 12 of 40, next step: etch";
- each tool only takes a lot whose next step is its own, moves it one step and hands it back;
- the player **routes lots in loops** between tools: without J's Computers, with belts and the **RFID Sorter**, which
  reads the next step as real fabs read the RFID tag of a FOUP ([Conveyors](conveyors.md)); with J's Computers, through
  the network;
- at T5, the **Overhead Hoist Transport**: carriages running on a ceiling track of their own inside the room, picking up
  and dropping FOUPs at the tools' load ports, as in TSMC's fabs.

### Testing, cutting and packaging

- **Wafer Prober** (T4): a probe touches every chip of the wafer and **marks the bad ones with a dot of ink**, as was
  done until the 1990s; the needles of a real probe card are of **tungsten-rhenium**. This is where the yield shows.
- **Dicing Saw** (T4), with a diamond blade; **Laser Dicer** (T5), "stealth" cutting with an infrared laser inside the
  silicon.

| Package | Tier | Machines |
| --- | --- | --- |
| Metal can (TO, Kovar) | T3 | Precision Assembler |
| Ceramic and plastic DIP (the "centipedes" of the 1970s) | T4 | **Die Bonder**, **Wire Bonder** (gold wire), **Molding Press** (epoxy) |
| BGA (solder balls underneath) | T5 | **Flip-Chip Bonder** (the chip upside down on solder balls) |
| Chiplets on an interposer | T5 | **Advanced Packaging Line**: several dice in one package, as AMD and TSMC do today |

- **Burn-in Oven** (T4): it heats chips and runs them for hours; the weak ones die here, not in a player's machine.
- **Binning:** of the same wafer, a few chips stand more than the rest. It is how, in real life, one chip becomes a
  high-end model and a mainstream one.
- **Nothing is lost:** rejected wafers and chips go back as **solar-grade silicon**, for Solar Panels, as the real
  industry sells its bad wafers to panel makers.

### Hardware for J's Computers

J's Computers on its own makes every part of every era by its simple path. With J's Industrial installed, the industrial
chain is the path at scale, and a server setting, off by default, makes it the only path.

**Every era of J's Computers is made with the lithography of its real time:**

| J's Computers era | Industrial tier | Lithography | Real node | Wafer |
| --- | --- | --- | --- | --- |
| Vintage (the 80s and 90s) | T4 | mercury UV, i line | 1 µm to 0.35 µm | 150 mm |
| Legacy (2000 to 2004) | T5 | DUV (KrF, ArF) | 180 to 90 nm | 300 mm |
| Transition (2005 to 2010) | T5 | immersion DUV | 65 to 32 nm | 300 mm |
| Standard (2011 to 2016) | T5 | immersion with multiple patterning | 32 to 14 nm | 300 mm |
| Advanced (2017 to 2026) | T5 | EUV, or DUV with quadruple patterning, much slower | 10 to 3 nm | 300 mm |
| Exa (from 2027) | T5, top | High-NA EUV | 2 nm and below | 300 mm |
| Singularity | T6 on | [Exotic materials](exotic-materials.md) | | |

- **The chips come from the Clean Room**: CPU and GPU dice, memory (DRAM), flash (NAND, for the SSDs from the Standard)
  and controller chips.
- **Assembly happens outside it:**
  - the **SMT Line** (T4): pick-and-place sets the parts on the board and a reflow oven solders them; it makes
    motherboards, expansion cards and memory modules. The Precision Assembler of the T3 stays for through-hole assembly;
  - the **Hard Disk Line** (T4): platters with their magnetic film by sputtering, read heads (of **giant
    magnetoresistance**, Nobel Prize 2007, from the T5), assembled inside a clean room, as real disks are;
  - the **Optical Disc Line** (T4): injected polycarbonate, sputtered aluminium and lacquer, for the CDs, then the DVDs
    and Blu-ray discs of each era.
- **Binning:** the CPU dice of a wafer come out as **a mix of the era's models**, from entry to top, by what each chip
  stands. A top chip can be sold as a lower model, never the other way round.
- **Scale:** a 300 mm wafer gives tens to hundreds of processors, by the size of the die.

### Other semiconductors

The same tools make the real products below. Many are **generic components** of J's Core's catalogue
([Materials](materials.md)): LEDs, sensors, oscillators; J's Industrial gives them their industrial recipe. Compound
semiconductors grow in the **MOCVD Reactor** (T5), the real production machine, and in the **MBE Reactor**, for lasers
and research.

| Product | Tier | How it is made | For | In real life |
| --- | --- | --- | --- | --- |
| Red and infrared LED | T4 | gallium arsenide | indicators, remote controls | Holonyak, 1962 |
| Blue and white LED | T5 | gallium nitride in the MOCVD Reactor; the white one adds a **cerium-doped YAG** phosphor | lighting (J's Civil Works' lighting line), screens | Nakamura, 1993 |
| Silicon solar cell | T3 | solar-grade wafer, diffusion, screen-printed **silver** contacts, anti-reflective coating; the panel takes float glass and an aluminium frame | the Solar Panel ([Energy](energy.md)) | Bell Labs, 1954 |
| Multi-junction cell | T5 | layers of gallium indium phosphide, gallium arsenide and germanium, in the MOCVD Reactor | the Multi-junction Solar Panel | the panels of satellites |
| Laser diode | T4 (infrared, red), T5 (blue-violet) | gallium arsenide; the blue-violet one of gallium nitride | fibre optic transmitters, and the CD, DVD and **Blu-ray** drives of J's Computers (Blu-ray needs the blue laser) | 1962; continuous at room temperature in 1970 |
| Image sensor | T4 (CCD), T5 (CMOS) | | cameras, telescopes ([Science](science.md)) | the CCD is from 1969 |
| Screens | T2 and T3 (**CRT**), T4 (LCD), T5 (TFT-LCD, OLED) | the CRT has a glass funnel, phosphors (the red one with **europium**), an electron gun and vacuum; an active-matrix LCD is a clean room process on large glass | the monitors of each J's Computers era; the Vintage ones are CRTs | |
| Power semiconductors | T3 (thyristor, by the Diffusion Furnace, with no lithography), T4 (IGBT), T5 (silicon carbide MOSFET) | | the valves of the HVDC station, the drives of the Variable Frequency Drive | the thyristor is from 1956 |
| Crystal oscillator | T3 | the Autoclave's synthetic quartz, cut and given electrodes | the clock of every circuit | |
| MEMS (accelerometers and gyroscopes on a chip) | T5 | | sensors for J's Robotics and J's Transport | the airbag sensors of the 1990s |

Superconducting chips (Josephson junctions, qubits) are in [Exotic materials](exotic-materials.md), with the T6.
