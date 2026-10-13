# Energy

The energy of the series is **J's Energy**, J's Core's own, and it is measured in real units: **joules** for energy,
**watts** for power and **volts** for the class of a line. A PC draws 300 W, a small electric furnace about 3 kW, a
steel mill's arc furnace tens of MW, a nuclear plant a GW. J's Industrial makes most of it and moves it; every mod of
the series and every add-on uses the same energy.

Electricity reaches far the way it does in real life: through **voltage classes**, **transformers** between them and
**loss by distance**. Voltage is how energy travels, not how the game progresses: the progression is still the circuits
([Tiers and circuits](tiers-and-circuits.md)). There is no amperage, no overclocking and no machine per voltage: a
machine is one machine, and it plugs into one class.

## What exists today

- J's Core moves FE through the power lines of its cable block, in the same tick and with no buffer in the cables: each
  generator's energy is split among the consumers it reaches, in proportion, without a unit lost or created by rounding.
  A cable can lose thousandths of what crosses each block of it, added up along the way; no cable sets a loss yet.
- J's Core has the seven energy tiers as a type, with their ceilings.
- J's Industrial has one **Energy Cable**, with no ceiling and no loss, the **Coal Generator** (it burns furnace fuel
  and makes 20 FE per tick, 400 W) and three machines that draw FE: the Electric Furnace, the Macerator and the
  Compressor.

## To build

### J's Energy

- Energy moves from the generators to the consumers in the same tick. Cables hold nothing; the energy lives in the
  generators, the consumers and the storage.
- A cable carrying more than its ceiling **saturates without a sound**: the excess just doesn't pass. Nothing burns,
  nothing explodes.
- When there is not enough energy, the machines share what there is in proportion and **run slower** in the same
  proportion, never in fits and starts.
- Generators **follow the load**: they burn only what the grid draws, as a power station with a governor does. Reactors
  are the exception: they can't change their power quickly and have a minimum ([Nuclear](nuclear.md)).
- Other mods' FE reaches J's Energy through the **converters** (below), at **1 FE = 1 J**: 1 FE per tick is 20 W, so a
  real 10 kW machine draws 500 FE per tick, the size of other mods' machines.

### Voltage classes

| Class | Voltage | In real life | What plugs into it |
| --- | --- | --- | --- |
| Low | 400 V | up to 1 kV: the socket (230 V) and the factory (400 V) | block machines, small generators, computers, batteries |
| Medium | 11 kV | 1 to 35 kV: the neighbourhood's grid and the big factories | multiblocks, the generators of power stations, big storage |
| High | 138 kV | 35 to 230 kV: regional transmission | transmission, and the biggest consumers |
| Extra-high | 500 kV | 230 to 800 kV: national transmission | transmission, and the biggest consumers |
| Ultra-high | ±1,100 kV, direct current | above 800 kV: the lines of thousands of km | transmission only |

Every block that takes or gives energy has a **port in one class**, shown in its tooltip. Cables and lines of different
classes never join: a **transformer** joins neighbouring classes, and an **HVDC converter station** joins the ultra-high
class, which is direct current.

**The biggest consumers take high voltage directly**: the particle accelerators, the electric arc furnace, aluminium
electrolysis, the Clean Room's fab. Their port is in the high or extra-high class, with an insulator where the line's
wire ends, and they plug straight into the line, as CERN takes 400 kV from the French grid. Which machine takes which
class is in [Machines](machines.md).

### Loss by distance

Every block of cable or wire loses a share of what it carries, and the shares add up along the way. There is no length
limit: distance costs by itself. Low voltage copper, a thousand blocks away, delivers almost nothing; that makes low
voltage the base's grid, medium the region's, and high the grid of long distances and big consumers. The share is fixed
per block and doesn't change with the load (in real life it grows with the current); the numbers are a little above real
ones, so the loss is felt at the game's distances.

### Cables

Cables are lines of J's Core's cable block. Low and medium voltage travel in cables, as in real life; one technology of
cable comes in several voltages.

| Cable | Tier | Class | Ceiling | Loss per 100 blocks |
| --- | --- | --- | --- | --- |
| Insulated Copper Cable (copper, rubber) | T1 | low | 10 kW | 10% |
| PVC Copper Cable | T2 | low | 40 kW | 7% |
| Armoured Cable (copper, XLPE, steel wire armour) | T3 | low | 160 kW | 5% |
| Paper-Insulated Lead Cable (copper, oiled paper, lead sheath) | T2 | medium | 5 MW | 1% |
| XLPE Medium-Voltage Cable | T3 | medium | 15 MW | 0.6% |
| XLPE High-Voltage Cable (underground, expensive) | T3 | high | 150 MW | 0.1% |
| NbTi Superconducting Cable | T4 | medium | 40 MW | none |
| REBCO Superconducting Cable | T5 | medium or high | 100 MW or 600 MW | none |
| Quantum Energy Cable | T6 | any | 10 GW | none |
| Singularity Cable | T7 | any | no ceiling | none |

- The **Insulated Copper Cable** is one of J's Core's **basic lines**: any mod uses it on its own, with a recipe of
  vanilla materials, so J's Space's generators reach its machines with no other mod. J's Industrial gives it its
  industrial recipe and adds every other cable.
- Superconducting cables carry their coolant (liquid helium for niobium-titanium, liquid nitrogen for REBCO) sealed in
  from when they are made, and need no upkeep.
- The Quantum and Singularity cables go beyond voltage: they join any class to any class. Their materials are in
  [Exotic materials](exotic-materials.md).
- Today's Energy Cable becomes the Insulated Copper Cable.

### Overhead lines

From medium voltage up, energy also travels in **overhead lines**: bare wire hung between insulators on poles and
towers, as in Immersive Engineering. They are the only link outside the cable block.

| Line | Tier | Class | Ceiling | Loss per 100 blocks | Longest span | Hung on |
| --- | --- | --- | --- | --- | --- | --- |
| Copper Overhead Line | T1 | medium | 2 MW | 1.5% | 32 blocks | wooden pole |
| Aluminium Overhead Line | T2 | medium | 8 MW | 1% | 32 | wooden or concrete pole |
| ACSR Line (aluminium on a steel core) | T3 | high | 200 MW | 0.1% | 64 | steel lattice tower |
| Bundled ACSR Line | T4 | extra-high | 1.5 GW | 0.03% | 96 | large lattice tower |
| HVDC Line | T5 | ultra-high | 12 GW | 0.01% | 128 | UHV tower, with a converter station at each end |

- **Poles and towers:** Wooden Pole (T1), Concrete Pole (T2), Steel Lattice Tower (T3), Large Lattice Tower (T4), UHV
  Tower (T5).
- **Insulators:** porcelain (T1), glass (T2) and polymer composite (T4). They go on poles, towers and on the ports of
  blocks.
- A wire is hung with its **coil**, from insulator to insulator, within the line's longest span. The loss counts the
  wire's length in blocks.
- Real spans are 50 to 500 m; the game's are shorter. The ceilings are real: the Changji to Guquan HVDC line carries 12
  GW at ±1,100 kV, and the superconducting cable on Long Island carries 574 MVA at 138 kV.

### The wrong class

The danger is at the **port** of a block that touches a cable or a line of another class:

- **One class above the port:** the machine **burns**. It stops, smokes and sparks, and is left **damaged**: it looks
  cracked, runs at half speed, draws 25% more and takes no upgrades until it is repaired ([Machines](machines.md)).
- **Two classes above or more:** an **arc flash**. The machine is destroyed and drops the scraps of its metals
  ([Metallurgy](metallurgy.md)), sets fire around it and burns whoever is a few blocks away.
- **Below the port:** the machine doesn't start and shows "Undervoltage". Nothing is damaged.
- It goes for every block with a port: a generator or a storage block on a class above is fed backwards by the grid and
  burns the same way.
- It only happens while the line is live. Plugging the wrong cable with the generator off burns nothing yet.

**Fuses.** Every machine has a **protection menu** in its screen with a fuse slot. On overvoltage the fuse blows, cuts
the energy and shuts the machine down at once, with no damage. A real fuse has a rated voltage, and a fuse facing a
voltage it can't break lets the arc jump over it:

| Fuse | Tier | Protects against | After it blows |
| --- | --- | --- | --- |
| Rewirable Fuse (lead-tin wire in a porcelain carrier) | T1 | one class above | works again with new fuse wire |
| HRC Fuse (high rupturing capacity: ceramic, silver element, quartz sand) | T3 | any overvoltage | thrown away, as a recyclable blown fuse |

The protection menu, the fuse slot and the ports are J's Core's, so every mod's machines have them.

**The battery slot.** Machines also keep a battery slot: the battery charges from the grid and powers the machine when
the grid goes down, as a UPS does.

### How electricity is made

Almost every power station on Earth runs the same chain: **a heat source, steam, a turbine, a generator**. Coal, gas,
fission, geothermal and solar heat only change the heat source. Water and wind turn the turbine themselves; solar
panels, fuel cells and RTGs make electricity with no turbine at all.

- **Small generators are one block**, with the engine and the dynamo together, as real generating sets. They give **low
  voltage** and need no piped water, so the early game is easy: burn, use heat, water or wind.
- **Power stations are the real chain in multiblocks**: a heat source (a boiler, a reactor, a geothermal well) makes
  steam, which runs in pipes as a gas ([Fluids and gases](fluids-and-gases.md)); the **turbogenerator**, turbine and
  generator on one shaft, makes electricity; a **cooling tower** closes the cycle and gives the water back. A station
  gives **medium voltage**, and a step-up transformer takes it to the line. A turbine takes steam from any heat source:
  going from coal to fission is changing the boiler for a reactor.
- **Blocks with their ports touching connect port to port, with no cable ceiling.** That is how a 1 GW turbogenerator
  delivers its energy: its step-up transformer stands against it, as the generator transformer of a real power station.
  No medium voltage cable carries 1 GW.
- **Fusion goes through steam too**: deuterium and tritium heat a lithium blanket. The exception is **aneutronic**
  fusion (deuterium and helium-3), whose charged particles are turned into electricity directly, a real idea.
- Multiblocks before the T3 are of brick, wood and iron; the big ones, of steel, come from the T3
  ([Metallurgy](metallurgy.md)).

### Generators

The numbers are estimates, to be tuned in playtesting.

**One block, low voltage**

| Generator | Tier | Power | Source |
| --- | --- | --- | --- |
| Coal Generator | T1 | 5 kW | furnace fuel; a small steam set with its water loop closed inside |
| Thermoelectric Generator | T1 | 0.5 to 2 kW | placed between a hot block (lava, magma, fire, a campfire) and a cold one (water, ice, snow): the Seebeck effect, as the Soviet generators that ran radios off kerosene lamps |
| Water Wheel Generator | T1 | up to 3 kW, by the water flowing through it | a river or a stream, as the first hydroelectric plants (Cragside, 1878) |
| Wind Generator | T1 | up to 3 kW, by height; double in a thunderstorm | the wind, as Charles Brush's windmill dynamo (1888) |
| Diesel Generator | T2 | 30 kW | diesel, biodiesel |
| Gas Engine Generator | T2 | 25 kW | natural gas, biogas, hydrogen |
| Solar Panel (silicon) | T3 | 200 W per block at noon; nothing at night, less in rain | the sun (the silicon cell is from 1954) |
| Fuel Cell | T4 | 100 kW | hydrogen and oxygen, with water as the only waste (Apollo's) |
| RTG | T4 | 500 W for a very long time, with no fuel | plutonium-238 or strontium-90 ([Nuclear](nuclear.md)), as the Voyager probes and the Soviet lighthouses |
| Multi-junction Solar Panel | T5 | 400 W per block | the sun |

**The steam chain, medium voltage**

Heat sources:

| Heat source | Tier | What it is |
| --- | --- | --- |
| Small Boiler | T2 | the first step towards steam: it takes water from a source block next to it, as a well, with no pump, and burns solid or liquid fuel |
| Industrial Boiler | T3 | a multiblock fed with pumped water: coal, oil, gas or biomass |
| Geothermal Well | T3 | free steam in hot biomes or near deep lava (Larderello, 1904) |
| Waste Incinerator | T3 | burns the base's waste (rotten flesh, seeds, paper, plastic): 10 MW |
| Solar Thermal Tower | T4 | a field of mirrors on a tower; by day only |
| Parabolic Trough Plant | T4 | rows of mirrors heat oil, which makes steam: 50 MW, by day (the SEGS plants, 1984) |
| Supercritical Boiler | T4 | steam in the **supercritical** state ([Materials](materials.md)): the same turbine gives 20% more |
| Binary Geothermal Plant | T4 | an organic cycle that uses lukewarm geothermal heat, so wells work in more places: 10 MW |
| The reactors | T4 on | [Nuclear](nuclear.md) |

Turbines, each giving what the steam it gets is worth, up to its size:

| Turbine | Tier | Up to |
| --- | --- | --- |
| Small Steam Turbine | T2 | 1 MW (Parsons' turbine at Elberfeld, 1900) |
| Steam Engine (a Corliss engine with a giant flywheel and dynamos, a multiblock of iron and brick) | T2 | 500 kW (Edison's Pearl Street Station, 1882) |
| Steam Turbine | T3 | 50 MW |
| Large Steam Turbine | T4 | 1 GW (a nuclear plant's) |
| sCO₂ Turbine (supercritical carbon dioxide, small and efficient) | T5 | turns any heat source into electricity at 50% (the Allam cycle, 2018) |

The **MHD Generator** (T4) sits on top of a thermal plant: hot gas turned into **plasma** crosses a magnetic field and
gives 20% more before the turbine, as the Soviet U-25 did in 1971.

**Other power stations, multiblocks, medium voltage**

| Station | Tier | Power |
| --- | --- | --- |
| Hydroelectric Turbine, in a dam | T3 | the water's height times its flow, up to 100 MW per turbine |
| Gas Turbine | T3 | 20 MW; with its exhaust in a heat recovery boiler it becomes a **combined cycle** (T4), 50% more from the same gas |
| Wind Turbine, a three-bladed tower | T4 | 3 MW, by height and weather |
| Fusion Tokamak (deuterium and tritium, through steam) | T5 | 2 GW, that is two large turbines ([Nuclear](nuclear.md)) |
| Stellarator (deuterium and helium-3, direct conversion) | T6 | 5 GW ([Nuclear](nuclear.md)) |
| Antimatter Reactor | T7 | tens of GW, by the anti-element it burns and its size ([Exotic materials](exotic-materials.md)) |
| Singularity Reactor | T7 | 50 GW, from matter falling into a micro black hole ([Exotic materials](exotic-materials.md)) |
| Dark Energy Collector | T8 | hundreds of GW, growing with the volume its frame encloses ([Exotic materials](exotic-materials.md)) |
| Heart of the Universe | T9 | endless: as much as the cables carry, from the inflation of a baby universe ([Exotic materials](exotic-materials.md)) |

Tidal, wave, ocean thermal and offshore wind power belong to J's Oceanics; power beamed from solar satellites, like the
Dyson Sphere, belongs to J's Space. With those mods installed, they deliver J's Energy like any generator.

### Storage

Real storage has two measures, and so does the game's: the **energy** it holds (joules, or kWh) and the **power** at
which it charges and discharges (watts). Storage discharges when generation falls short and charges with the surplus.

**Batteries, as items** (tools, armour, the battery slot, and carrying energy by hand):

| Battery | Tier | Holds | In real life |
| --- | --- | --- | --- |
| Lead-Acid Battery | T2 | 2.5 MJ (0.7 kWh, a car battery) | Planté, 1859 |
| Nickel-Cadmium Battery | T3 | 5 MJ | Jungner, 1899; the cadmium comes from greenockite |
| Lithium-Ion Battery | T4 | 20 MJ | Sony, 1991 |
| Solid-State Battery | T5 | 100 MJ | the generation arriving now |

Batteries from the T6 on are decided with [Exotic materials](exotic-materials.md).

**Storage blocks**

| Storage | Tier | Class | What it is |
| --- | --- | --- | --- |
| Lead-Acid Battery Bank | T2 | low | the battery rooms of telephone exchanges; 50 MJ |
| Pumped-Storage Hydro | T3 | medium | a multiblock with two reservoirs at different heights: it pumps water up with the surplus and runs it down through a turbine when energy is short. What it holds is plain physics: the water's volume times gravity times the height between them, so a thousand blocks of water 50 blocks up hold 490 MJ |
| Nickel-Cadmium Battery Bank | T3 | low | 200 MJ |
| Compressed Air Storage | T4 | medium | compresses air into a cavern and lets it out through a turbine; what it holds depends on the cavern (Huntorf, in a salt cavern, 1978) |
| Vanadium Flow Battery | T4 | medium | a multiblock with two tanks of electrolyte and a stack: the tanks give the energy and the stack gives the power, separately, as in the real one |
| Lithium-Ion Battery Container | T4 | medium | 3.9 MWh, like a Tesla Megapack |
| Flywheel Storage | T4 | low | holds little but answers at once, for peaks (Beacon Power, 2011) |
| Molten Salt Storage | T4 | none, it holds heat | tanks of molten salt beside a solar thermal plant, so it runs at night |
| SMES | T5 | medium | a niobium-titanium coil in liquid helium: superconducting magnetic storage, discharged almost instantly |

The **Lead-Acid Battery Bank** is J's Core's **basic battery**: any mod uses it on its own, with a recipe of vanilla
materials; J's Industrial gives it its industrial recipe.

The **Battery Charger** (T2, low, 2 kW) charges batteries; storage blocks also have a slot to charge one.

### The grid's equipment

**Transformers and stations**

| Block | Tier | Joins | What it is |
| --- | --- | --- | --- |
| Distribution Transformer | T1 | low and medium | iron core, copper windings, in oil (the ZBD transformer, 1885); it also goes on a pole, as the ones in the street |
| Substation | T3 | medium and high | a multiblock yard: transformer, breakers, busbars and a fence |
| EHV Substation | T4 | high and extra-high | a bigger yard |
| HVDC Converter Station | T5 | extra-high and ultra-high | halls of thyristor valves; the same station rectifies or inverts, by the side the energy comes from |

The ceiling of each is the ceiling of the higher class it joins.

**FE converters.** They live in J's Core, with recipes of vanilla materials, because J's Computers on its own also needs
them:

| Converter | Class | Ceiling |
| --- | --- | --- |
| Energy Converter | low | 100 kW |
| MV Energy Converter | medium | 10 MW |
| HV Energy Converter | high | 200 MW |
| EHV Energy Converter | extra-high | 1.5 GW |
| UHV Energy Converter | ultra-high | 12 GW |

The **Configurable Energy Converter** lets the player pick the class of its J's Energy side, the direction (FE to
joules, joules to FE, or both) and the limits in and out, in FE per tick. The rate between FE and joules is a server
setting, never the player's: a converter that changed it would make energy from nothing.

**Control**

| Block | What it does | In real life |
| --- | --- | --- |
| Circuit Breaker | opens and closes a line, by hand or by redstone, to isolate a stretch; it doesn't trip on overload, because cables saturate | the breakers of every grid |
| Energy Meter | shows the power going through and the energy so far; with J's Computers installed, the network reads it | the electricity meter |
| Protection Relay | watches a line and opens a breaker on overvoltage, undervoltage, lack of generation or energy flowing backwards; its thresholds are set by the player | the protection relays of substations |
| Load Shedding Relay | when generation falls short, cuts the low-priority branches first, so the critical machines stay on | under-frequency load shedding |
| Automatic Transfer Switch | moves a load to the backup source when the main one fails | what starts a hospital's or a datacentre's generator |
| Surge Arrester | goes on poles and substations and stops surges | the lightning arresters of power lines |
| Control Panel | linked to breakers, meters and generators, it shows the state of each and opens and closes the breakers | the control rooms of grids, for whoever plays without J's Computers; with it, the network does this and more |

Storage blocks and meters also give a redstone signal through a comparator, by their charge and their flow.

**Tools** ([Tools and armour](tools-and-armour.md))

| Tool | Tier | What it does |
| --- | --- | --- |
| Voltage Tester | T1 | beeps near a live line, to know whether it is safe to touch |
| Analog Multimeter | T1 | the class and the flow, on a needle |
| Digital Multimeter | T3 | the same, plus the loss and the energy so far |
| Power Analyzer | T4 | logs a block's power over time and shows it as a graph |
| Lineman's Gloves | T1, T3, T4 | insulating rubber gloves, worn as armour, that protect from shocks by class, like the real Class 0 to 4 |
| Hot Stick | T2 | an insulated pole to connect and disconnect live wires and to work breakers from a distance |
| Wire Cutters | T1 | take a wire off its insulators and give the coil back, and strip cables (below) |

### Shocks, lightning and stripped cables

- **Overhead lines are bare wire.** Touching a live one hurts, more the higher its class; on high voltage it kills.
  Cables are insulated and don't shock.
- **Lightning** that strikes an overhead line or a pole sends a **surge** along the line, which burns the machines on
  it, unless a fuse or a Surge Arrester stops it.
- **Stripping cables.** With the Wire Cutters in the other hand, a cable in the inventory is stripped into **bare wire**
  and its **insulation** as recyclable rubber or PVC scrap, which is how the copper of old cables is recovered when they
  are replaced. A placed cable stripped by the cutters still carries energy but **shocks** whoever touches it, as an
  overhead line; **Electrical Tape** (T2, PVC) insulates it again. Cutting a **live** wire gives a shock and an arc: it
  hurts and can break the cutters, unless the player wears Lineman's Gloves of the right class.

### Settings

In J's Core's files, as consequence settings:

- **overvoltage damages machines** (on by default); off, a machine on a class above its port just doesn't start;
- **accept any voltage**: machines take any class;
- **accept FE directly**: J's Energy blocks take and give FE without a converter, both ways, so the series speaks FE
  with the rest of a pack;
- **the rate between FE and joules** (1 FE = 1 J by default);
- **electric shock** and **lightning surges**, each can be turned off.
