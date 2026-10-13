# Science

Research, the Computational Research Centre, the scientific instruments, the particle accelerators and the
radioisotopes.

The base of J's Industrial's progression is the circuits ([Tiers and circuits](tiers-and-circuits.md)). Research is a
second gate, only for frontier science: the discoveries that real scientists made, in their real order.

## What exists today

Nothing: J's Core has the idea of teams and of progression that any mod can read, and J's Industrial has no research.

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
  installed, **astronomical data**. Each discovery also asks for an amount of computing in the CRC; the numbers are in
  [Implementation](implementation.md).
- **Not researched:** common metals, steel, alloys, petrochemistry and circuits.
- Discoveries come in their real order: fission before fusion, superconductivity before the high-temperature kind.
- **Knowledge belongs to the team** (J's Core's teams, FTB Teams when it is installed, or else the player), kept as a
  J's Core progression that other mods can read. A machine runs a locked recipe only if its owner's team has researched
  it.
- **A recipe is never hidden.** JEI, EMI, the manuals and J's Computers' Pattern Studio always show every recipe and
  mark the ones that need research with the discovery they need; only running it is refused. The whole tree of
  discoveries is visible, so a player can plan.
- A server setting, `require_research`, on by default, turns research off: everything is unlocked from the start.

### The Computational Research Centre

The **CRC** is a multiblock 3 blocks high, 3 wide and 9 deep, one model through its controller. It comes at **T3**, as
the transistor research computers of the 1950s and 1960s (the IBM 7090, 1959).

Its grid has **three sections of 3 by 3 along its body, 27 places**, filled with the processors of each tier:

| Part | Tier | Research | Energy | Heat |
| --- | --- | --- | --- | --- |
| Research Processor (transistor boards) | T3 | 1 | 5 kW | +1 |
| Integrated Research Processor | T4 | 4 | 20 kW | +2 |
| Nanoscale Research Processor | T5 | 16 | 80 kW | +4 |
| Quantum Research Processor | T6 | [Exotic materials](exotic-materials.md); it needs a **dilution refrigerator**, since real qubits work at 15 millikelvin | | |
| Heat Sink | T3 | | | −1, passive |
| Liquid Cooler | T3 | | | −3, taking the heat through the heat port, by heat pipe to the Central Cooling Plant ([Machines](machines.md)) |

- When the cooling is enough, the temperature settles; when it isn't, it rises, and **above 100 °C a processor burns**
  and is lost, as Overdrive damages a machine.
- A CRC full of T5 processors draws more than 2 MW, so its port is **medium voltage** ([Energy](energy.md)).

**Groups.** The **Research Link Cable** joins CRCs: each connected run is **one group**, working on one discovery. It is
one cable, with no tiers and no length limit; it takes colours, and different colours don't join. Every CRC beyond the
first adds **90% of its power**: coordinating costs, as in any real parallel computing, but every CRC more helps. Any
CRC of the group picks the research, and all show the current one, its progress, what each contributes, and a **queue**
of the next ones.

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
