# Science

Research, the Computational Research Centre, the scientific instruments, the particle accelerators and the
radioisotopes.

The base of J's Industrial's progression is the circuits ([Tiers and circuits](tiers-and-circuits.md)). Research is a
second gate, only for frontier science: the discoveries that real scientists made, in their real order.

## What exists today

J's Core has the **research** every mod shares (below): discoveries a team makes part by part, each costing research
points and data, set in data so a modpack can change them, starting once the team knows what it comes after, and the
knowledge gate, which locks a recipe for running and never for seeing. J's Industrial has:

- the **four kinds of data**, sample, crystal, spectral and particle, an item each, its tooltip saying which
  instruments give it; until the instruments come, they come from the creative tab;
- the **`require_research`** setting, on by default;
- the **Research Link Cable**;
- the **Computational Research Centre**, whole: its blocks and entries, its 27 places with the processors of T3 to T5
  and the three ways of cooling, its heat, which burns the hungriest processor past 100 °C, its groups with their
  queue, its formed model, and its screen with its Processors, Research and Group tabs.

The discoveries themselves (the table below) are content still to declare: no recipe asks for one yet. It also has
**what counts as discovering an element**, which the research stands on:

- **an element is discovered at its first synthesis by a team**: the elements science discovered by making them
  (technetium, promethium, astatine, everything from neptunium on, and every element beyond the known table), the
  first time a team makes one in any form, an isotope of it among them;
- the team **knows** the element from then on, which the research of its stable isotope asks for, and its players earn
  the **Discovery** advancement, which opens that tab;
- **the player who counts is the owner of the machine that made it**, who may be away: they earn the advancement as they
  join;
- for an element **beyond the known table**, the first to discover it on the server also gives it its name
  ([Materials](materials.md)): a discoverer who was away is asked for it when they join, and the right waits for them;
- **mining what holds traces of one discovers nothing**: the Deep Core Miner's traces and the Aleph's veins show an
  element, but only making it discovers it.

## To build

### Research

- **A piece of research is a scientific discovery**, with its name and its date, and it unlocks a family of recipes and
  machines. For example:

| Discovery | Year | Unlocks |
| --- | --- | --- |
| Separation of the rare earths | the 1950s, Ames | the Solvent Extraction Cascade ([Chemistry](chemistry.md)) |
| Nuclear fission | 1938, Hahn and Meitner | fuel and reactors ([Nuclear](nuclear.md)) |
| Superconductivity | 1911, Kamerlingh Onnes | the NbTi cable, superconducting magnetic storage ([Energy](energy.md)) |
| High-temperature superconductors | 1986 | REBCO |
| The laser | 1960 | lasers and laser cutters |
| Giant magnetoresistance | 1988 | the disk heads of the T5 ([Clean Room](clean-room.md)) |
| Controlled nuclear fusion | | the Tokamak |
| The synthesis of each superheavy element | the real date of each | its stable isotope ([Materials](materials.md)) |

- **Discoveries ask for data from different instruments** (below): **sample data** for materials (rare earths,
  isotopes), **crystal data** for solid state physics (superconductivity, giant magnetoresistance), **spectral data**
  for the laser, **particle data** for nuclear physics (fission, fusion, superheavy elements), and, only with J's Space
  installed, **astronomical data**. The kinds of data are an open list, so a mod adds its own: J's Space the
  astronomical data, the Aleph the singularity data ([The Aleph](aleph.md)).
- **Data are items**, one for each kind, made by the instruments and put in the CRC, which spends them as the research
  goes: each one covers an equal share of it, so a discovery that asks for eight crystal data takes one every eighth
  of the way.
- **Each discovery asks for an amount of research**, counted in research points: a T3 processor makes one point a
  second. What each discovery costs is content, set in data, so a modpack can change it.
- **Each discovery has its prerequisites**: other discoveries, elements the team knows, and, with J's Space, its
  milestones. It starts only once they are all known. A discovery that asks for astronomical data or for a milestone
  of J's Space exists only when J's Space is installed.
- **Not researched:** common metals, steel, alloys, petrochemistry and circuits.
- Discoveries come in their real order: fission before fusion, superconductivity before the high-temperature kind.
- **Knowledge belongs to the team** (J's Core's teams, FTB Teams when it is installed, or else the player), kept as a
  J's Core progression that other mods can read. A discovery is a knowledge the team holds in part as its research
  goes and whole once it is done. A machine runs a locked recipe only if its owner's team has researched it. The
  research itself is J's Core's, so J's Space's own research computer stands on the same discoveries.
- **A recipe is never hidden.** JEI, EMI, the manuals and J's Computers' Pattern Studio always show every recipe and
  mark the ones that need research with the discovery they need; only running it is refused. The whole tree of
  discoveries is visible, so a player can plan.
- A server setting, `require_research`, on by default, turns the gate off: no recipe is locked and nothing is granted;
  research can still be done, for its advancements. It is the first setting of `jsindustrial-server.toml`.

### The Computational Research Centre

The **CRC** is a multiblock 3 blocks high, 3 wide and 9 deep, built of blocks of its own, as every multiblock of the
series. It comes at **T3**, as the transistor research computers of the 1950s and 1960s (the IBM 7090, 1959), and
once formed it is one model of three cabinets in their style, the parts installed in it seen through its glazed doors
and the lamps of its registers blinking while it works.

**Its blocks:**

| Block | Where |
| --- | --- |
| CRC Terminal | the middle of its front, in the middle layer at a player's eyes, its console; it opens the CRC's screen |
| CRC Casing | the rest of the outside: smooth sheet, a louvred vent along the top of the rear |
| CRC Gate | three, inside, one for each section, as IBM called the racks of its cards |
| CRC Bus | four, inside, between and at the ends of the gates |
| Its entries | on the six places of the two lower rows of the rear face, and nowhere else |

**Its entries**, where the lines arrive, each placed by the player on one of the six rear places:

| Entry | How many | What comes through |
| --- | --- | --- |
| Power Entry | exactly one | energy at **medium voltage**: a CRC full of T5 processors draws 2.16 MW ([Energy](energy.md)) |
| Link Socket | at most one | the Research Link Cable (below) |
| Coolant Inlet and Coolant Outlet | at most one pair, always together | cold water in and hot water out, for the Liquid Coolers |
| Heat Port | at most one | the heat pipe to the Central Cooling Plant, for the Heat Pipe Coolers ([Machines](machines.md)) |

Its screen has **three sections of 3 by 3, 27 places**, one for each gate, filled with the processors of each tier and
what cools them:

| Part | Tier | Research | Energy | Heat |
| --- | --- | --- | --- | --- |
| Research Processor (transistor boards) | T3 | 1 point a second | 5 kW | 5 kW |
| Integrated Research Processor | T4 | 4 | 20 kW | 20 kW |
| Nanoscale Research Processor | T5 | 16 | 80 kW | 80 kW |
| Quantum Research Processor | T6 | [Exotic materials](exotic-materials.md); it needs a **dilution refrigerator**, since real qubits work at 15 millikelvin | | |
| Heat Sink | T3 | | | sheds 5 kW, by itself |
| Liquid Cooler | T3 | | | sheds up to 15 kW into the water that runs from the Coolant Inlet to the Coolant Outlet; cheap |
| Heat Pipe Cooler | T4 | | | sheds up to 25 kW through the Heat Port, by heat pipe to the Central Cooling Plant; dear, and no water |

- **The heat is real**: all the power a processor draws becomes heat. When its coolers shed it all, the temperature
  settles; when they don't, it rises, and **above 100 °C the part that heats most burns** and is lost, as Overdrive
  damages a machine.
- **A cooler sheds nothing without its entry**: a Liquid Cooler needs the coolant pair and water flowing through it,
  and the water leaves hotter the more it carries; a Heat Pipe Cooler needs the Heat Port and the plant at the other
  end of the pipe. Water is the cheap way, the heat pipe the dear one that needs no water; a CRC full of T5
  processors needs the Central Cooling Plant.
- Its screen has the kit's tabs and three of its own: **Processors** (the 27 places, a place for each kind of data,
  the coolant's lamp, the thermometer and the counters, with the inventory), **Research** (the tree of discoveries) and
  **Group** (the CRCs of its group and what each gives).

**Groups.** The **Research Link Cable** joins CRCs through their Link Sockets: each connected run is **one group**,
working on one discovery. It is one cable, with no tiers and no length limit; it takes colours, and different colours
don't join. The group's power is **its strongest CRC's whole and 90% of each other's**: coordinating costs, as in any
real parallel computing, but every CRC more helps. Any CRC of the group picks the research, and all show the current
one, its progress, what each contributes, and a **queue** of the next ones. The progress belongs to the team, so
cutting the cable loses nothing; in a group of several teams, each team counts its own CRCs.

With J's Computers installed ([Computers](computers.md)), the CRC can also take server hardware and join the network, a
**Research Router** groups CRCs as the Server Router groups servers, the Cluster Management Computer manages research
groups in a **Research** tab, and the Mainframe can send a discovery to a group, or split a big one among groups.

### Instruments

One instrument for each kind of data, the real ones of each time:

| Instrument | Tier | Gives | Spends | In real life |
| --- | --- | --- | --- | --- |
| Mass Spectrometer (multiblock) | T3 | **sample data**: what a material is made of, and its isotopes | a **rhenium filament**, which wears out, and vacuum | Aston, 1919 |
| ICP Mass Spectrometer | T5 | the same, faster and finer | **argon**, for its plasma | the 1980s |
| X-ray Diffractometer | T3 | **crystal data**: the structure of a solid | an X-ray tube with a copper anode | the Braggs, 1913 |
| Electron Microscope | T3 (transmission), T4 (scanning) | crystal data, seen atom by atom | a **lanthanum hexaboride** cathode, and vacuum | Ruska, 1931; the scanning one, 1965 |
| Optical Spectrometer | T3 | **spectral data**: the lines of light of each element | | Bunsen and Kirchhoff, 1859, who found caesium and rubidium this way |
| NMR Spectrometer | T4 | spectral data of molecules | a **NbTi superconducting magnet** in liquid helium | the 1960s and 1970s |
| Particle Detector, set against an accelerator | T3 (bubble chamber), T4 (wire chamber), T5 (silicon trackers and calorimeters) | **particle data** from each run of the accelerator | scintillators: sodium iodide with **thallium** at T3 and T4, **lutetium** (LYSO) at T5 | the bubble chamber is from 1952 |

The **atomic clock** (caesium at T3, as the one of 1955, and the smaller rubidium one) gives the exact time big
accelerators need to synchronise their beams, and that J's Space uses for navigation.

The radio telescope and the other observatories belong to J's Space.

### Accelerators

Accelerators came from 1930 on, and each generation made real discoveries:

| Accelerator | Tier | What it is | What it gives | In real life |
| --- | --- | --- | --- | --- |
| Electrostatic Accelerator | T2 | a high-voltage generator that fires protons | the first particle data | Cockcroft and Walton, 1932: lithium and a proton into two helium |
| Cyclotron (multiblock) | T2 | a huge disc magnet with two "dees" | **proton-rich isotopes**; particle data | Lawrence, 1932 |
| Heavy-Ion Cyclotron | T4 | the same, with beams of **calcium-48** | superheavy elements by **hot fusion** | Dubna's U-400 made elements 113 to 118, up to oganesson in 2002 to 2006 |
| Synchrotron (a ring of **variable size**) | T3 | magnets in a ring; **a larger ring gives more energy**, as in real physics (energy grows with field times radius) | particle data; with the superconducting magnets of the T4, **synchrotron light**: X-rays for crystal data | 1945; the superconducting Tevatron, 1983 |
| Linear Accelerator (of **variable length**) | T4 | **longer gives more energy** | superheavy elements by **cold fusion**, on lead and bismuth targets | Darmstadt's UNILAC made elements 107 to 112; RIKEN's made nihonium in 2004 |
| Collider (two rings that cross) | T4 | | high-energy particle data | LEP, 1989 |
| LHC (the largest ring of the mod) | T5 | 1,232 superconducting NbTi magnets at 1.9 K | the most expensive data of the mod | 2008 |
| SHE Factory | T5 | a new-generation heavy-ion cyclotron | the real attempts at **elements 119 and 120**, with beams of **titanium-50** and **chromium-54** | Dubna's, 2020; in 2024 Berkeley made livermorium with titanium-50, the real step towards 120 |

- **Superheavy elements** are each made from their **real pair** of beam and target: oganesson from californium-249 and
  calcium-48, tennessine from berkelium-249 and calcium-48, nihonium from bismuth and zinc-70. The targets come from the
  reactor ([Nuclear](nuclear.md)). A run gives a few atoms and the **discovery** of the element; its stable, usable
  isotope comes after ([Materials](materials.md)). From element 121 on lies beyond today's science
  ([Exotic materials](exotic-materials.md)).
- **Antimatter** starts for real at T5, with the **Antiproton Decelerator** (CERN, 1999), which makes antihydrogen in
  tiny amounts, as data only. Antimatter in quantity is in [Exotic materials](exotic-materials.md).
- **Energy:** the Cyclotron draws about 1 MW, on medium voltage; the Synchrotron tens of MW, on high voltage; the LHC
  **200 MW**, on extra-high voltage, straight on the line, as CERN takes 400 kV from the grid ([Energy](energy.md)).
- **Rare stable isotopes** for beams (calcium-48, nickel-64, titanium-50, zinc-70) are separated in the **Calutron**
  (T3), the electromagnetic separation of Oak Ridge (1943), as Russia still makes calcium-48 today: calcium has no gas
  to spin in a centrifuge. Gas centrifuges are for uranium ([Nuclear](nuclear.md)).

### Radioisotopes

Isotopes have three real origins: **reactors**, by neutron capture (the neutron-rich ones); the **cyclotron** (the
proton-rich ones); and **fission products** or **ore**. Only isotopes with a real use in the series are made:

| Isotope | From | Used for |
| --- | --- | --- |
| Cobalt-60 | reactor | the **Isotope Irradiator**: it sterilises, and **cross-links polymers by radiation**, another real way to XLPE |
| Iridium-192 | reactor | the **gamma radiography camera**, which inspects welds ([Tools and armour](tools-and-armour.md)) |
| Thulium-170 | reactor | portable X-ray sources ([Tools and armour](tools-and-armour.md)) |
| Strontium-90 | fission products | the RTG ([Energy](energy.md)), as the Soviet lighthouses |
| Plutonium-238 | reactor, from neptunium-237 | the RTG, as the Voyager probes |
| Americium-241 | reactor | **smoke detectors** (J's Civil Works) and americium-beryllium neutron sources (J's Geology's well logging, starting reactors) |
| Californium-252 | reactor | neutron sources; californium-249 is the target of oganesson |
| Tritium | reactor, from lithium-6 | fusion fuel; **self-powered signs** (the exit signs that glow with no energy); it decays into helium-3 |
| Caesium-137 | fission products | **industrial gauges** of level and density |
| Krypton-85 | fission products | leak detection |
| Promethium-147, nickel-63 | reactor | **betavoltaic cells** (T5): microwatts for decades, to power detectors and sensors with no cable; promethium also gives luminous paint |
| Polonium-210 | uranium processing | **static eliminators**, really used in chip fabs: in the Clean Room, less static means fewer clinging particles and more yield |
| Radon-222 | uranium ore | not a product but a **hazard**: it builds up in uranium mines and asks for ventilation ([Nuclear](nuclear.md)) |

Medical isotopes are left out: no mod of the series has medicine, and nothing is made without a use. Most isotopes are
made in **irradiation positions inside reactors** and in a small **Research Reactor** ([Nuclear](nuclear.md)).
