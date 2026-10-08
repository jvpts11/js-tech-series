# Fluids and gases

How liquids, gases, supercritical fluids, slurries, plasmas, exotic matter and heat move, what happens when a pipe can't
take what goes through it, the tanks, the pumps and valves, the air, cryogenics and ultra-pure water. Petrochemistry is
in [Chemistry](chemistry.md); the states of matter themselves in [Materials](materials.md).

## What exists today

- Pipes are lines of J's Core's cable block. A pipe stands a range of temperatures, in kelvin, and takes some **marks**:
  a gas needs a pipe that holds pressure, and a corrosive fluid a pipe made for it. A run of pipes carries only what
  every pipe of it takes; anything else simply doesn't go in. Fluids of different machines never pour into each other.
- A machine's tanks are exposed through NeoForge's fluid capability, so pipes and tanks of any mod work with them.
- Chemicals that are neither items nor fluids (Mekanism's gases, for example) move through a bridge, as an id and an
  amount.

## To build

### Each state in its own line

| State | Travels in | In real life |
| --- | --- | --- |
| Liquids | pipes | |
| Gases | gas pipes, which hold pressure | gas pipelines |
| Supercritical fluids | high-pressure pipes | carbon dioxide pipelines carry it supercritical |
| Slurries | lined pipes, with rubber or ceramic inside; a slurry is **abrasive** and wears out unlined pipe | slurry pipelines (the Minas–Rio line is 529 km long) |
| Plasmas | plasma conduits | |
| Exotic matter | exotic matter conduits ([Exotic materials](exotic-materials.md)) | |
| Heat | heat pipes | |
| Compressed air | its own pressurised line ([Machines](machines.md)) | |

- **Machines push what they make**, as the discharge pumps of real machines do: a pipe from one machine's output to
  another's input just works. **Pumps** are for taking fluids from the world.
- **Every pipe has a top flow**, and a run carries up to its worst pipe's. Fluids are not lost along a pipe, and there
  is no length limit: in real life a fluid only loses pressure, and that only matters over tens of kilometres.

### The wrong content, leaks and hazards

Lines of different states never join. The danger is a fluid reaching a pipe of its own line that can't take it:

- **too hot**: the pipe softens and bursts (rubber and PVC first);
- **too cold**, a cryogenic liquid in an ordinary pipe: the metal turns brittle and cracks, as carbon steel does;
- **corrosive**, in a pipe not made for it: it corrodes through in about a minute, and the broken pipe is recyclable
  scrap;
- **abrasive**, a slurry in an unlined pipe: it wears through, more slowly;
- **above the pipe's pressure**, a supercritical fluid in an ordinary gas pipe: it bursts;
- **hydrogen embrittles** ordinary steel: a Steel Gas Pipe carrying hydrogen cracks after a while; stainless steel
  stands it.

**A broken pipe leaks.** Liquids spill into the world as fluid blocks: oil, acid, molten metal. Gases come out as a
**cloud** that thins out over time.

**The hazards come from each material's properties** ([Materials](materials.md)), shown in its tooltip:

| Property | Examples | What it does |
| --- | --- | --- |
| flammable | hydrogen, methane, propane, petrol vapour | the cloud catches fire from a flame, lava or a machine's spark; with enough gas, it **explodes** |
| toxic | chlorine, hydrogen sulfide, sulfur dioxide, carbon monoxide, hydrogen fluoride, ammonia | hurts and poisons whoever is in the cloud |
| asphyxiant | nitrogen, argon, helium, carbon dioxide | suffocates in closed rooms; carbon dioxide, heavier than air, pools low, as it does in real life |
| cryogenic | liquid nitrogen, liquid helium | freezes whoever touches it; liquid oxygen next to fuel catches fire |
| hot | steam, molten metal | burns |
| acid | sulfuric, hydrochloric and nitric acids | hurts on contact |
| radioactive | uranium hexafluoride (toxic and corrosive as well) | radiation ([Materials](materials.md)) |

To deal with them: the **Gas Detector**, a block that gives a redstone signal when a cloud reaches it; a handheld
detector, a gas mask and a respirator ([Tools and armour](tools-and-armour.md)); and the **Extraction Fan**, which
pushes air one way and clears a room of a cloud.

### Pipes

**Liquids**

| Pipe | Tier | Top flow | Takes | In real life |
| --- | --- | --- | --- | --- |
| Copper Pipe | T1 | 100 mB/t | up to 200 °C | soldered copper plumbing |
| Cast Iron Pipe | T2 | 200 | up to 400 °C | the water and steam mains of the 19th century |
| PVC Pipe | T2 | 200 | **acids**, but only up to 60 °C | PVC stands acids and softens with heat |
| Steel Pipe | T3 | 400 | up to 500 °C | |
| Stainless Steel Pipe | T3 | 400 | up to 600 °C, and **corrosives** | |
| Refractory Launder | T3 | 200 | **molten metal**, up to 1,700 °C | the refractory-lined channels of steel mills |
| PVDF Pipe | T4 | 200 | **ultra-pure water** without spoiling it, and acids | what chip factories use |
| Vacuum-Jacketed Pipe | T4 | 400 | **cryogenic liquids**: liquid nitrogen, oxygen and helium | double-walled stainless steel with a vacuum between |
| Hastelloy Pipe | T5 | 800 | up to 1,000 °C, corrosives and **molten salt** | a nickel alloy for hydrogen fluoride and molten salt reactors |

**Gases**

| Pipe | Tier | Top flow | Takes | In real life |
| --- | --- | --- | --- | --- |
| Iron Gas Pipe | T1 | 100 | gas at low pressure | 19th-century gas lighting; it carries the electrolyser's hydrogen and coke oven gas |
| Steel Gas Pipe | T3 | 400 | gas at high pressure | gas pipelines |
| Stainless Gas Pipe | T3 | 400 | corrosive gases: chlorine, hydrogen fluoride | |
| High-Pressure Pipe | T3 | 800 | **supercritical fluids** | carbon dioxide pipelines |

**Slurries:** the **Rubber-Lined Pipe** (T2, 200) and the **Ceramic-Lined Pipe** (T3, 400, which wears much more
slowly).

**Plasma:** the **Magnetic Plasma Conduit** (T4), for cold plasma, the MHD generator's and plasma etching's; the
**Superconducting Plasma Conduit** (T5), for fusion plasma, at millions of degrees.

**Heat**, in watts:

| Heat pipe | Tier | Up to | Carries |
| --- | --- | --- | --- |
| Copper-Water Heat Pipe | T3 | 200 °C | 50 kW (the heat pipe is from 1963) |
| Sodium Heat Pipe | T4 | 1,000 °C | 1 MW, as in space reactors |
| Lithium Heat Pipe | T5 | 1,500 °C | 10 MW |

**Compressed air:** the **Compressed Air Line** (T2).

### Tanks

**Capacity is real volume.** A block is one cubic metre, and a cubic metre is **1,000 mB**. A multiblock tank holds its
**inner volume**: a 5 by 5 by 5 tank, walls included, holds 27 m³, that is 27,000 mB. A gas tank holds **its volume
times its pressure**, by the gas law: a vessel at 20 bar holds 20 times as much gas as its volume. So bulk storage is
done with **multiblocks of variable size** ([Machines](machines.md)).

**Liquids**

| Tank | Tier | What it is |
| --- | --- | --- |
| Barrel | T0 | one block, 200 mB, a real barrel |
| Riveted Iron Tank | T1 | a riveted multiblock of variable size |
| Rubber-Lined Tank | T2 | a multiblock for acids |
| Steel Tank | T3 | a welded multiblock, larger; the tank of refineries |
| Stainless Tank | T3 | a multiblock for corrosives |
| Holding Furnace | T3 | keeps metal molten, spending heat |
| Molten Salt Tank | T4 | insulated, the heat storage of [Energy](energy.md) |
| Cryogenic Tank | T4 | a vacuum-insulated multiblock for liquefied gases |

**Gases**

| Tank | Tier | Pressure | What it is |
| --- | --- | --- | --- |
| Gasholder | T1 | 1 bar | the telescoping gasometer of the 19th century: a multiblock of variable size that **rises as it fills** |
| Pressure Vessel | T3 | 20 bar | steel, for gas at high pressure and supercritical fluids |
| Horton Sphere | T3 | 15 bar | the sphere of refineries (propane, butane); a multiblock |
| Salt Cavern Storage | T4 | the cavern's | gas kept in a cavern, as the compressed air of [Energy](energy.md) |

**Other states**

- **Agitated Tank** (T2, slurries): it stirs the slurry so it doesn't settle; without energy, the slurry settles and has
  to be stirred again.
- **Steam Accumulator** (T2, heat): it keeps steam for peaks, as Ruths' accumulator of the 1920s.
- **Plasma Containment Ring** (T5): it holds plasma in a magnetic torus while it has energy. If the energy fails, the
  plasma escapes in a hot burst.

**To carry by hand:** the bucket (vanilla), the **Jerrycan** (T2, 20 litres), the **Gas Cylinder** (T2, 200 bar) and the
**Dewar Flask** (T4, for cryogenic liquids).

**Rules**

- One content per tank.
- A full tank **refuses**, and the machine waits with "output full": nothing is lost.
- Windows show the level, and a comparator reads it.
- **Boil-off:** real cryogenic tanks lose a little every day to boiling. Without a **Reliquefier** attached, the gas
  that boils off goes out through the valve.

### Pumps, valves and control

**Pumps**, for taking fluids from the world:

| Pump | Tier | What it does |
| --- | --- | --- |
| Water Pump | T1 | endless water from a source next to it |
| Drainage Pump | T2 | sucks up fluid blocks around it: a flooded mine, lava, and **leaks**, so it cleans up an oil or acid spill |
| Well Pump | T2 | oil, gas and brines from their reservoirs, with its well going down to them ([The world](world.md)); its numbers are in [Chemistry](chemistry.md) |

**Valves and instruments**

| Block | Tier | What it does |
| --- | --- | --- |
| Manual Valve | T1 | opens and closes by hand |
| Check Valve | T1 | lets fluid through one way only |
| Redstone Valve | T2 | opens and closes by signal |
| Filter Valve | T2 | lets only one fluid through, to send each fluid of a shared run to its machine |
| Flow Meter | T2 | shows the flow and the total so far; a comparator reads it |
| Pressure Relief Valve | T3 | opens above a pressure the player sets and vents to the air, or to a discharge pipe |

The multimeters of [Energy](energy.md) also read pipes: their content, flow, temperature and pressure. With J's
Computers installed, the network can open and close valves ([Computers](computers.md)).

**Boilers explode.** In the 19th century boilers blew up often, and that is why pressure vessel codes were born (ASME,
1915). A boiler whose steam has nowhere to go, or that runs **dry** while it works, builds up pressure and **explodes**,
unless it has a Pressure Relief Valve, which lets the excess out. A boiler run dry is damaged even with the valve. The
same goes for any heated pressure vessel.

### The air

Air is free, taken in as the Water Pump takes water.

| Machine | Tier | What it does |
| --- | --- | --- |
| Air Separation Unit (multiblock, variable size) | T2 | compressed air into nitrogen, oxygen and argon (Linde, 1902) |
| Noble Gas Extension, part of the same unit | T3 | neon, krypton and xenon as well |

The main shares are the real ones: 78.08% nitrogen, 20.95% oxygen, 0.93% argon and 0.04% carbon dioxide. **The rare
gases are a thousand times their real share**, so they can be gathered at all: real air holds 0.087 ppm of xenon, 1.1
ppm of krypton, 18 ppm of neon and 5 ppm of helium.

**Helium** comes from:

- **heating uranium and thorium minerals** (uraninite, thorianite, monazite), which keep the helium of their alpha
  decays: that is how Ramsay first found it on Earth, in 1895, in cleveite;
- **the air**, at the same thousandfold share as the other rare gases;
- with J's Geology installed, **helium-rich natural gas**, the real industrial source.

Helium-3, from the decay of tritium, is in [Nuclear](nuclear.md).

### Cryogenics

| Machine | Tier | What it liquefies | In real life |
| --- | --- | --- | --- |
| Air Liquefier | T2 | air, nitrogen, oxygen | the Linde-Hampson cycle, 1895 |
| Gas Liquefier | T3 | argon and methane as well (liquefied natural gas) | the Claude cycle, with an expansion engine, 1902 |
| Helium Liquefier | T4 | helium, at −269 °C | Collins' cryostat, 1947 (Kamerlingh Onnes first liquefied it in 1908) |
| Reliquefier | T4 | stands against a cryogenic tank and returns what boils off | |

### Ultra-pure water

As in real chip factories:

| Machine | Tier | What it does |
| --- | --- | --- |
| Water Distiller | T2 | water into distilled water |
| Reverse Osmosis Unit | T3 | filters it through a membrane (the 1960s) |
| Ion Exchange Column | T3 | deionises it; its **resin saturates and is regenerated with acid and caustic soda**, as the real one (the resin comes from [Chemistry](chemistry.md)) |
| UPW Polisher | T4 | ultraviolet light and ultrafiltration: **ultra-pure water**, for the [Clean Room](clean-room.md), in PVDF pipe |

Hydrogen from steam reforming, fluorine, nitrogen trifluoride and carbon dioxide are in [Chemistry](chemistry.md);
photoresist in [Clean Room](clean-room.md); heavy water in [Nuclear](nuclear.md).

### Settings

In J's Core's files, as consequence settings, each on by default:

- **pipes fail**: off, the wrong content just doesn't go in;
- **leaks are hazardous**: off, a leak only loses what it carried;
- **hydrogen embrittles steel**;
- **cryogenic boil-off**;
- **boilers explode**: off, a blocked boiler just stops.
