# Rockets

How a rocket is designed, built, carried to the pad, launched, brought back and reused, what it is made of, and what
goes wrong. How it flies is in [Travel](travel.md); what it burns is in [Propellants](propellants.md).

## What exists today

Nothing of J's Space exists yet. J's Core already has multiblocks with four rotations and machines with their screens,
which every part of a rocket is built on.

## To build

### Parts are multiblocks

Every part of a rocket is a **multiblock**, and the rocket's delta-v comes from the parts that are there. A rocket built
from any blocks ends up shapeless; a rocket of multiblocks always looks like a rocket.

- **Standard diameters.** Real rockets are built in fixed diameters, and parts of the same diameter fit together. J's
  Space has four: **3 blocks** for small rockets, the first to reach orbit; **5 blocks** for large ones, the Moon and
  Mars; **7 blocks** for the heavy ones, as Saturn V; **9 blocks** for the super-heavy, as Starship.
- **Adapters**, rings that narrow, join different diameters, as a thinner second stage on top of a first.
- **Parts have a variable size where it makes sense**: a tank is built as tall as the player likes, and taller holds
  more propellant; solid boosters stack in segments, as the Space Shuttle's came in four. Engines, capsules and rings
  have a fixed size.
- **The assembly stations** make the special blocks of each part: combustion chambers and turbopumps from the engine
  station, tank plates from the tank station, heat shield tiles from the shielding station.
- **Stages.** A **separation ring** between two parts marks where a stage ends. The rocket knows, from bottom to top,
  which stages it has and in which order they come loose; side boosters are a stage that burns with the first and comes
  loose before it.

### Designing a rocket

At a **terminal in the Rocket Hangar**, a player opens the design screen and chooses every part of the rocket, in a way
close to Kerbal Space Program's editor: easy and visual. When the design is confirmed, a **hologram** shows where every
block goes, and the player builds the rocket by hand, **layer by layer, multiblock by multiblock**.

**The screen shows**, without becoming a simulator:

- the **delta-v of each stage** and the total;
- the **thrust-to-weight ratio on the world the rocket leaves from**: under 1, the rocket doesn't leave the ground, and
  a rocket that takes off from the Moon (0.17 g) may not take off from the Earth;
- the **delta-v the destination asks for**, green if it is enough, red if not;
- what is missing: propellant, crew, a heat shield for an atmosphere.

**Sea-level and vacuum engines.** Real engines come in two versions: the one with a small nozzle works well in air, the
one with a **large nozzle**, for vacuum, gives more up high and does badly on the ground (Raptor and Raptor Vacuum,
Merlin and Merlin Vacuum). In the game it is the same: the vacuum nozzle goes on the upper stage.

With J's Computers installed, the **Aerospace Design Computer** runs **AeroCAD**, the same design screen as a program.
It asks for good graphics cards and hardware of its own. When a design is finished, the computer sends it into the
Rocket Hangar ([J's Computers](computers.md)).

### Building it automatically

Every rocket built becomes an **assembly plan** the hangar keeps: which parts, and how they stack. With a plan chosen,
**the hangar's bridge crane builds the next rocket by itself**, block by block:

- **without J's Computers**, the player feeds the blocks into the hangar's **item port**: a chest, a hopper, a pipe, a
  conveyor or another mod's exporter, anything that puts items in. Whatever the hangar is waiting for, it builds into
  the rocket;
- **with J's Computers**, if the Aerospace Design Computer and the hangar are on the network, an **Operation** starts
  the assembly, and J's Computers' autocrafting makes the parts that are missing.

**Reuse.** Stages that landed on their platforms come back to the hangar, are inspected and repaired with a repair kit,
and go into the next rocket, as Falcon 9 boosters fly more than twenty times.

The whole chain can run with nobody there: plan, parts made, the hangar builds, the transporter carries, the tank farm
fills, the rocket launches on schedule ([Cargo](cargo.md)), the stage comes back, it is repaired, the next one.

### The hangar and the way to the pad

A rocket is built in the **Rocket Hangar** and carried to the pad, which is only for launching. There are two ways, two
visual styles:

- **The American way.** The rocket is stacked **standing**, in a tall hangar of variable size with a **bridge crane**
  under the roof (the cranes of the Kennedy Space Center's Vehicle Assembly Building lift 325 tonnes) and huge doors. A
  **crawler-transporter**, a multiblock, carries it standing and slowly along the **crawlerway**, a path of blocks of
  its own, to the pad, leaves it on the hold-down clamps and goes back. NASA's crawlers date from 1965 and carry a
  rocket at under 2 km/h along a road of river gravel 5.5 km long.
- **The Russian way.** The rocket is assembled **lying down**, in a long, low hangar, and goes to the pad **by train**,
  on a car that **raises it upright** at the end, as Soyuz rockets are taken out at Baikonur.

### The launch complex

| Piece | American | Russian |
| --- | --- | --- |
| Platform | a base with **hold-down clamps**, which keep the rocket down until its engines reach full power | the four arms of the **"tulip"**, which hold the rocket hanging and open like petals as it rises |
| Tower | a **fixed service tower** with arms that swing back (hoses, cables, the crew access arm) | a **mobile service tower**, which rolls away on rails before launch |
| Flame trench | a **trench with a deflector** under the pad | the **flame pit** of Gagarin's Start, an old quarry about 45 m deep |
| Deluge | **water** poured onto the pad at ignition, to dampen the sound and protect the concrete (about 1.7 million litres at an SLS launch) | the same |
| Tank farm | the **spheres** of hydrogen and oxygen ([Propellants](propellants.md)) | the same |
| Lightning | the **three lightning towers** of pad 39B, with wires strung between them | a mast |

Real rules that become play, each with a consequence setting:

- **With no flame trench and no deluge, a launch wrecks the pad and what is around it**, as Starship's first flight did
  in 2023, tearing up the pad's concrete and throwing chunks of it far away.
- **No launch in a thunderstorm.** Apollo 12 was struck by lightning twice in its first minute, and the rule has existed
  ever since; in a storm, the panel holds the countdown.

**Stages landing:** a **landing platform** on land, one per stage; a **floating platform** to land at sea, as SpaceX's
drone ships, named after the starships of Iain M. Banks' novels ("Of Course I Still Love You", "Just Read the
Instructions"), the first landing at sea in 2016; or a **fall zone** marked on the map, best at sea.

**Control:** the **Launch Panel** by the pad, with the countdown and the go, and **Mission Control**, from where
everything is followed ([J's Computers](computers.md) for its computer form). A player launches from either.

**On other worlds** there is no tower and no deluge: there is a **simple pad with berms**. A lander's engine throws the
dust of the ground at full speed; the Apollo 12 lunar module **sandblasted the Surveyor 3 probe**, 160 m away, and the
pieces brought back showed the marks. A real pad stops that, and in the game it is what keeps a landing from damaging
the base next to it. There, the tank farm is the local propellant plant.

### The parts

| Part | What it is | The real thing |
| --- | --- | --- |
| **Engine** | combustion chamber, turbopump and nozzle, by propellant: solid, hydrolox, methalox, kerolox (with J's Industrial), hypergolic, nuclear thermal (with J's Industrial), metallic hydrogen, fusion; **sea-level and vacuum** versions | |
| **Engine mount** | the base of a stage, holding one large engine or a **cluster** of small ones | Falcon 9 has nine Merlins, Saturn V five F-1s |
| **Tank** | for a pair of propellants, of **variable height**; a hydrogen tank is **much larger** for the same weight, since liquid hydrogen is eleven times lighter than kerosene | |
| **Solid booster** | segments stacked, fixed at the side with a nose of its own | the Space Shuttle's |
| **Separation ring** | marks a stage and drops it; the **radial** version drops boosters | |
| **Adapter** | changes the diameter | |
| **Instrument ring** | where the **flight computer** lives (the Flight Control Computer, with J's Computers) and the **communications antenna** | Saturn V's Instrument Unit |
| **Fairing** | protects the payload on the way up, and comes off | |
| **Payload adapter** | holds satellites, probes or a cargo container | |
| **Capsule** | the crew, with windows, a **heat shield** below, **parachutes**, and a docking port on top; 1 to 3 seats at diameter 3, up to 7 at diameter 5 | Vostok 1, Soyuz 3, Orion 4, Crew Dragon up to 7 |
| **Launch escape tower** | pulls the capsule away if the rocket fails on the way up | Soyuz's saved its crews in 1983 and in 2018 |
| **Habitation module** | a sealed interior for long missions, with zero-gravity beds and life support | |
| **Service module** | power (solar panels or fuel cells), manoeuvring propellant (hypergolic), radiators | the service modules of Apollo and Orion |
| **Control thrusters** | small jets to turn and dock | |
| **Lander** | a **descent stage** (legs, engine, tanks) and an **ascent stage** (cabin, engine) | the Apollo lunar module |
| **Landing legs and grid fins** | for stages that come back and land | Falcon 9's grid fins |
| **Docking port** | joins a rocket to a station | |
| **Cargo container** | cargo for automatic routes ([Cargo](cargo.md)) | |

Every part carries its tier label: the first ones at T1 and T2, the fusion ones at the top. The numbers of each part
(thrust, mass, Isp) are in [Implementation](implementation.md).

### What goes wrong

**A failure is a consequence of the player's choices, never bad luck.** Whoever designs well and looks after their
rockets almost never sees one blow up; there is no "2% chance of exploding on every launch".

| Choice | Consequence | The real thing |
| --- | --- | --- |
| A thrust-to-weight ratio under 1 | the rocket **doesn't leave the ground**: the engines light and it stays, burning propellant | |
| Too little delta-v for the destination | the screen warns in red but **lets it launch**: the rocket goes as far as its propellant takes it, and if it doesn't reach orbit, it **falls back** | |
| Launching with no flame trench and no deluge | the pad and its surroundings are damaged | Starship, 2023 |
| Coming down where there is air **with no heat shield**, or the wrong one for that atmosphere | the capsule **burns up on re-entry** | Columbia, in 2003, was lost to damage to its heat shield |
| Landing **with no legs**, or no propellant to slow down | the lander **crashes** | |
| **Reused stages never inspected** | every flight wears a part; inspected and repaired, it keeps flying; past its limit unrepaired, **it fails**: an engine shuts down, a tank cracks | reused boosters are inspected after every flight |
| Waiting too long with hydrogen in the tank | the propellant **boils away** | |
| The **wrong propellant** in an engine | the engine **doesn't light** | |
| A **damaged oxygen tank** used anyway | it **explodes** | Apollo 13 |

**When a failure comes:** with a crew and an **escape tower**, the tower pulls the capsule away and it comes down on its
parachutes; the crew lives, which is what the tower is for, as with Soyuz MS-10 in 2018. With no tower, whoever is
aboard dies with the rocket. An explosion destroys the rocket and does damage around it, and the wreckage can be
salvaged, as fallen stages can ([Travel](travel.md)).

Every one of these failures has a consequence setting in `jsspace-server.toml`; with them off, a badly built rocket only
fails to leave the ground or to arrive.
