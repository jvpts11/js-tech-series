# Nuclear

The fuel cycle, the reactors, running them and their accidents, the waste, fusion, and protection from radiation.

Radiation itself (its levels, its effects, its settings) is a J's Core mechanic that any mod uses; J's Industrial owns
its sources and its gear. Everything here follows real nuclear engineering, with the game's time scaled down.

## What exists today

Nothing: uranium and thorium minerals exist as data in J's Core's catalogue ([Materials](materials.md)).

## To build

### The fuel cycle

| Step | Machine | Tier | In real life |
| --- | --- | --- | --- |
| Mining | the uranium minerals ([Minerals](minerals.md)); the **In-Situ Leach Field** (T4) takes uranium out of sandstone without digging ([Machines](machines.md)) | | |
| Milling and leaching | the Ball Mill and a **Uranium Mill**, with sulfuric acid, into **yellowcake** | T3 | |
| Conversion | yellowcake, hydrogen fluoride and fluorine into **uranium hexafluoride**, toxic, corrosive and radioactive ([Fluids and gases](fluids-and-gases.md)) | T3 | |
| Enrichment, first generation | the **Calutron**, slow and hungry for energy | T3 | Oak Ridge, 1943 |
| Enrichment, second generation | the **Gaseous Diffusion Plant**, a huge multiblock that eats energy without end | T3 | Oak Ridge's K-25, 1945, then the largest building in the world |
| Enrichment, third generation | the **Gas Centrifuge Cascade**, a multiblock of variable size: more centrifuges in series enrich more, with 50 times less energy than diffusion | T4 | Zippe centrifuges, the 1960s and 1970s |
| Fuel fabrication | uranium hexafluoride into uranium dioxide, pressed and sintered **pellets** (Sintering Furnace), **zircaloy rods** (zirconium without its hafnium, [Chemistry](chemistry.md)), the **fuel assembly** | T3 to T4 | |

- **Radon builds up in uranium mines**, as in real ones, and asks for ventilation; without it, the mine turns
  radioactive.
- **Not every reactor needs enriched uranium.** Graphite reactors and heavy water reactors burn **natural uranium**: a
  path with no enrichment, at the price of something else (ultra-pure graphite, or heavy water). **Heavy water** comes
  from the **Heavy Water Plant** (T3), by the Girdler process with toxic hydrogen sulfide; it is 1 part in 6,400 of
  ordinary water.
- **Enrichment has limits:** power reactors use 3 to 5% uranium-235, research reactors up to 20%. **Above 20% it isn't
  done**: the cascade stops there.
- The **thorium cycle** (thorium-232 into uranium-233, in molten salt reactors) is an alternative at T5, and the use of
  the thorium of monazite.

### Reactors

Every reactor gives **heat**; electricity comes out through the steam chain of [Energy](energy.md) (the Large Steam
Turbine and a cooling tower). A plant connects at medium voltage and steps up to the line through the transformer
standing against it. A reactor's structure is built with the materials of its tier and only starts with fuel.

| Reactor | Tier | Fuel and moderator | Its strength | Its price | Electric power | In real life |
| --- | --- | --- | --- | --- | --- | --- |
| Research Reactor | T3 | uranium up to 20%, small | **isotopes** (its irradiation positions, [Science](science.md)), neutrons for science, safe | no energy worth the name | | TRIGA, 1958 |
| Graphite Reactor | T3 | **natural uranium**, graphite | no enrichment | ultra-pure graphite; it is big | hundreds of MW | Magnox, 1956 |
| RBMK | T4 | low-enriched uranium, graphite, boiling water | cheap: no pressure vessel and no containment building | a **positive void coefficient**: at low power it turns unstable and can run away | 1 GW | Chernobyl, 1986 |
| PWR | T4 | 3 to 5% uranium, water under pressure | stable: as it heats, it loses power by itself | a huge **pressure vessel** (the Heavy Forging Press, welds inspected by gamma radiography), steam generators, a **containment building** | 1 GW | Shippingport, 1957; the most common in the world |
| BWR | T4 | 3 to 5% uranium, water boiling in the core | simpler than the PWR | the steam that reaches the turbine is slightly radioactive | 1 GW | 1960 |
| CANDU | T4 | **natural uranium**, **heavy water** | no enrichment; it refuels **without stopping** | a lot of heavy water | | 1962 |
| Fast Breeder | T4 | MOX (plutonium and depleted uranium), cooled by **liquid sodium** | it **breeds more plutonium than it burns**, and gives the transuranium elements (americium, curium, californium, the targets of [Science](science.md)) | sodium **burns in air and explodes in water** | 600 MW | BN-600, 1980 |
| Molten Salt Reactor | T5 | **liquid** fuel in a salt of fluorine and lithium, the **thorium cycle** | high temperature with no pressure; it burns thorium | the salt corrodes everything but **Hastelloy** | | MSRE, Oak Ridge, 1965 |
| Pebble Bed Reactor | T5 | TRISO fuel spheres, **helium** | it doesn't melt: the spheres stand the heat; its high heat serves the sCO₂ Turbine and hydrogen production | little power per reactor | | China's HTR-PM, 2021 |
| SMR (small modular reactor) | T5 | a PWR in a module | **made in a factory** as an item and set down whole | less power | 77 MW per module | the 2020s |

### Running a reactor

- **Control rods** of the real materials: **boron carbide**, **hafnium**, **silver-indium-cadmium** and **gadolinium**.
- **Xenon poisoning:** after lowering the power or shutting down, xenon-135 builds up for hours (minutes in the game)
  and **stops a restart**: the real "iodine pit", which weighed on Chernobyl.
- **Decay heat:** a shut-down reactor **keeps heating**, about 7% at first, falling over hours. It needs cooling **even
  after a SCRAM**, and so does spent fuel.
- **Refuelling:** a PWR or BWR stops to change its fuel; a CANDU changes it while it works.
- **The reactor's screen** shows temperature, power, neutron flux, coolant, rods and xenon. Without J's Computers, the
  Control Panel and redstone reach it ([Energy](energy.md)); with J's Computers, a control program through the
  Industrial Controller Computer ([Computers](computers.md)).

### Accidents

| Accident | Which reactors | What happens | In real life |
| --- | --- | --- | --- |
| Loss of cooling | all | the pumps lose power, decay heat warms the fuel, **zirconium reacts with steam and gives off hydrogen**, the hydrogen **explodes** in the building, and the core melts into corium | Fukushima, 2011; what saves a plant is **emergency diesel generators** (the Diesel Generator), passive cooling and hydrogen recombiners |
| Power excursion | the **RBMK** only | at low power, with xenon and the rods out, the positive void coefficient makes the power shoot up: a **steam explosion**, the graphite **burns**, and with no containment the radioactivity spreads | Chernobyl, 1986 |
| Sodium fire | the **Fast Breeder** | a sodium leak catches fire in air, and explodes in water | Monju, 1995 |

- **Containment makes the difference:** a PWR or BWR in meltdown **stays inside its containment building**, as Three
  Mile Island (1979) melted half its core and almost nothing got out; an RBMK has no containment, and everything gets
  out.
- **The consequences:** the **corium** falls to the lowest point and **eats slowly through the floor**, as Chernobyl's
  "elephant's foot"; the ground around is irradiated; and **fallout spreads chunk by chunk**, as J's Core's pollution
  does, decaying over time: radioactive contamination is one more kind of that pollution.
- The SCRAM, damage and meltdown thresholds, the explosion's radius, the corium and the decay times are consequence
  settings, and the explosion can be turned off.

### Waste

1. **Cooling first.** Spent fuel comes out of the reactor **hot and very radioactive**. In the **Spent Fuel Pool** (T3),
   water over the fuel cools and shields it; **without water, or without cooling, it heats up and releases radiation**,
   as Fukushima's unit 4 pool did. After a time in the pool, fuel goes into **Dry Casks** (T4), steel and concrete
   cooled by air, needing no energy.
2. **Reprocessing** (optional). The **PUREX Plant** (T4) dissolves the fuel in nitric acid and separates it by solvent
   extraction with TBP in kerosene (the 1950s). It gives **uranium** to recycle, **plutonium** for the Fast Breeder's
   MOX, the **minor actinides** (neptunium, americium, curium, for [Science](science.md)) and the **fission products**,
   where strontium-90, caesium-137, krypton-85 and promethium-147 come from; **ruthenium, rhodium and palladium** come
   out too, since fission really makes platinum metals, radioactive for a few years before they serve. What is left is
   **high-level waste**.
3. **Vitrification.** The **Vitrification Plant** (T4) melts high-level waste into **borosilicate glass**, in steel
   canisters.
4. **Storage for good.** The **Geological Repository** (T5, of variable size), as Finland's Onkalo, the first in the
   world: deep tunnels in **granite**, canisters of **copper** wrapped in **bentonite clay**, as the Swedish KBS-3
   design. If it is damaged, it leaks.
5. **Transmutation** (T5, optional). The **Accelerator-Driven System** joins a proton accelerator, a spallation target
   and a subcritical core, and **burns the long-lived actinides**: the waste lasts centuries instead of millennia, as
   Belgium's MYRRHA, being built.

**Low-level waste** (gloves, filters, contaminated tools, the water of decontamination showers) goes into a **Waste
Compactor** and drums. Breaking a reactor gives its ordinary blocks back.

### Fusion

The **Tokamak** (T5) and the **Stellarator** (T6) are in [Energy](energy.md). The Tokamak works as the real ones (JET,
ITER, SPARC):

- **Deuterium** comes from heavy water, by electrolysis.
- **Tritium is bred in the Tokamak itself**: the blanket around the plasma holds **lithium-6**, and each fusion neutron
  turns lithium-6 into tritium and helium, as ITER and DEMO plan to do. Fission reactors make it too.
- **Lithium-6** is enriched in the **Lithium Isotope Plant** (T5) by the real COLEX process, which uses **mercury** and
  pollutes, as at Oak Ridge.
- **Helium-3** comes from the decay of stored tritium and, with J's Space installed, from lunar regolith.
- **Magnets** of NbTi (as ITER's) or of **REBCO tape**, smaller and stronger (as SPARC's); the plasma is heated by
  **neutral beam injection** and **gyrotrons**; the **divertor**, which takes the exhaust heat, is of **tungsten**, as
  ITER's.
- **Starting it takes a huge pulse of energy.** JET drew it from giant flywheels so as not to bring down the grid; here,
  the Tokamak starts from the **Flywheel Storage** or the **SMES** of [Energy](energy.md) connected to it.
- **Laser fusion**, an alternative at the top of T5: the **Inertial Fusion Facility** fires **192 lasers** at a
  deuterium-tritium capsule, as NIF, which reached ignition in 2022. It works in pulses and needs no magnets.

**Fusion has no accidents.** If the cooling of superconducting magnets fails, the machine simply stops, with no damage;
the same goes for every machine with superconducting magnets (the LHC, the Synchrotron, the NMR Spectrometer, the SMES).

### Protection

Real radiation reaches a body in two ways, and the Core's radiation keeps both:

- **external dose** (gamma, neutrons) crosses the body from a distance; a suit hardly helps. **Distance, time and
  shielding** do: radiation crosses blocks **weakened by their material**, lead very much, concrete and stone well,
  water well (which is why the spent fuel pool works), air hardly at all. A lead wall around a source is real
  protection;
- **contamination**, radioactive dust on the skin or breathed in, is what a **suit** and a **respirator** stop and a
  **shower** washes off.

| Item | Tier | Protects from | In real life |
| --- | --- | --- | --- |
| Geiger Counter | T2 | it measures: it clicks and shows the rate on screen | Geiger and Müller, 1928 |
| Dosimeter | T3 | it adds up the dose taken and warns | the film badges of the 1940s |
| Lead Apron / Lead Armour | T2 | **a little** of the external dose; it is heavy and slows the wearer | the lead aprons of X-ray rooms |
| Hazmat Suit and respirator | T3 | almost all **contamination**; little external dose | |
| Advanced Hazmat Suit | T4 | more contamination, for longer | |
| Full Containment Suit, with its own air | T5 | **all contamination**, and some external dose | |
| Potassium Iodide | T2 | **radioactive iodine** from fallout: taken before, the thyroid doesn't absorb it | what is handed out to people near nuclear plants |
| Decontamination Shower | T3 | it washes contamination off; the water that runs off is low-level waste | |
| Lead Bricks | T2 | shielding, as blocks | |

The **Hot Cell** (T4) is a shielded room with **master-slave manipulators**, where sources and fuel are handled without
taking dose, as in real laboratories: for a whole room what Lead Shielding ([Machines](machines.md)) does for one
machine. The rest of the armour is in [Tools and armour](tools-and-armour.md).
