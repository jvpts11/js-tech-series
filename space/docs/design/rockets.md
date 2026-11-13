# Rockets

How a rocket is designed, built, carried to the pad, launched, brought back and reused, what it is made of, and what
goes wrong. How it flies is in [Travel](travel.md); what it burns is in [Propellants](propellants.md).

## What exists today

A rocket can be designed on the screen of a Rocket Hangar, built by the hangar's crane, carried by a crawler to a pad, fuelled, counted down and launched. J's Core's multiblocks with four rotations and its machines with their screens are what every part of a rocket is built on.

### Parts are multiblocks

Every part of a rocket is a **multiblock**, and the rocket's delta-v comes from the parts that are there. A rocket built from any blocks ends up shapeless; a rocket of multiblocks always looks like a rocket.

- **Standard diameters.** Real rockets are built in fixed diameters, and parts of the same diameter fit together; a part fits only on one of its own diameter. J's Space has **3 blocks** for small rockets, the first to reach orbit, and **5 blocks** for large ones, the Moon and Mars.
- **Parts have a variable size where it makes sense**: a tank is built in layers, as tall as the player likes, and taller holds more propellant. Engines, capsules and rings have a fixed size.
- **Every part is built of a kit of blocks**, one kind for each real thing the part has, placed where it goes. There are 44 kinds in all. Each block shows one detailed face on all six sides, so it is known from any side while the part is built. A part forms by itself as its last block goes down, and its blocks merge into the part's round model, each block a panel or piece of its own, hollow where the real part is. A part that loses a block in the hangar or in flight holds, with a hole in its model there, and putting the right block back mends it.
- **Stages.** A **separation ring** between two parts marks where a stage ends. The rocket knows, from bottom to top, which stages it has and in which order they come loose. Side boosters hang at the side of a part, built of the same parts, and burn with the first stage and come loose before it.
- **Ownership.** A rocket belongs to whoever built it and is flown by their team.

### Designing a rocket

At the **Hangar Terminal** or the **Rocket Design Table** in the Rocket Hangar's control room, a player opens the **Rocket Design** screen: the hangar itself seen in 3D, the rocket standing on the middle of the bay's floor, in a way close to Kerbal Space Program's editor. A part is dragged from its bin to a green node of the rocket, where it snaps. The player can turn a part a quarter turn, build a tank taller or shorter, choose the root, hang one to four boosters round a part with the mirror, and undo and redo. The camera turns round the rocket, comes nearer and rises, never through the hangar's walls. The bins sort the parts into six tabs: command, tanks, engines, rings, landers and recovery.

**The screen shows**, without becoming a simulator:

- the **delta-v of each stage** and the total;
- the **thrust-to-weight ratio on the world the rocket leaves from**: under 1, the rocket doesn't leave the ground, and a rocket that takes off from the Moon (0.17 g) may not take off from the Earth;
- the **delta-v each destination asks for**, with a bar showing how far the rocket gets, short in red. From the Overworld, a first estimate: a low orbit 9,400 m/s, the Moon 15,200 m/s, Mars 13,000 m/s;
- what is missing: a stage with propellant and no engine, an engine with nothing to burn, a rocket too weak to lift off, taller than 96 blocks or wider than 17, the most the hangar's bay and its door take, the crew's seats, the heat shield to come home through air, the tanks filled at the pad.

The screen's sign reads READY or NOT READY. Every change is kept by the hangar; a design is named, saved as one of the hangar's **sixteen plans**, loaded from them, and sent to the bay to be built once it is ready. Each hangar takes a number of its own the first time it forms.

**Sea-level and vacuum engines.** Real engines come in two versions: the one with a small nozzle works well in air, the one with a **large nozzle**, for vacuum, gives more up high and does badly on the ground (Raptor and Raptor Vacuum, Merlin and Merlin Vacuum). In the game it is the same: the vacuum nozzle goes on the upper stage, and a separation ring raises its black interstage skirt round a vacuum engine standing on it. An engine's thrust and impulse fall from their vacuum values with the air's pressure, so on Mars an engine is nearly as good as in a vacuum.

### Building it automatically

Once a design is sent to the bay, the hangar lays it out as an **assembly plan** on the bay's floor, every block of every part's kit where it goes, and counts each part's blocks as they go down. Its **hologram** shows every block still to place as a see-through ghost, the layer to build next glowing amber and a wrong block outlined in red. Each part has its box drawn round it, in the colour of its kind, with its name and size, and a line across the top of the screen names the rocket, the part and layer being built and the block that layer needs most. The hangar's **bridge crane** then builds the rocket by itself, block by block, the lowest first:

- the player feeds the blocks into the hangar's **Loading Dock** in its outer wall: a chest, a hopper, a pipe, a conveyor or another mod's exporter, anything that puts items in. The port takes only the blocks the rocket still wants, and no more of each, and gives back out what it no longer wants;
- the Hangar Terminal is a machine, not a computer: it takes no data cable. When J's Space grows, the control room gets a place for a computer and for J's Core's network port, and the network sets the crane building through that computer ([J's Computers](computers.md)).

A player can also lay the blocks by hand, where the hologram shows them.

### The hangar and the way to the pad

A rocket is built in the **Rocket Hangar** and carried to the pad, which is only for launching.

- **The hangar.** One building of one size, 45 by 110 by 39 blocks, built of blocks of its own and formed by its Hangar Terminal, as the Small Shipyard is; its high bay is the Vehicle Assembly Building's. The bay takes a rocket up to 96 blocks tall and 17 wide on its mobile launcher. Its door is stepped as the VAB's: a lower door 25 wide and 12 high for the crawler and the launcher's deck, and a slot 17 wide over it up to the roof for the rocket and its tower. The door opens by itself for the crawler and from the door console, and the tallest rocket rolls out through it. Its bridge crane runs on rails along the top of its long walls, with its hoist under the bridge. Beside the bay stands a control room after Apollo's launch control room: consoles facing windows onto the bay, the terminal among them, the chairs, the door console, a step with the Rocket Design Table, and the status board; a loading dock takes in the blocks the crane builds with. People go in through two doors, one from outside into the room and one from the room into the bay, opened by hand. Thin lamps up the inner faces of the long walls, every twelve blocks, and lights in the roof light its walls; and as the VAB's floodlights light its high bay through, the formed hangar keeps invisible lights in a grid through its bay wherever it is empty, so a rocket is seen from its foot to its top and no creature of the dark is born in it. Work Platforms the crew lays stand round the rocket. The Structure Projector shows it, the Multiblock Builder builds it, and the Mission Handbook draws it whole.
- **The mobile launcher.** The rocket is stacked **standing** on a mobile launcher, as Apollo's was. Where the bay has room for one, the plan lays the launcher under the rocket, sized by it: a deck on pedestals three blocks tall, an exhaust opening as wide as the rocket's foot, a hold-down arm at each corner, three tail service masts, and, at the end away from the door, the red umbilical tower with a swing arm to each tank, a crew access arm to the capsule's hatch and an elevator up its shaft. The capsule's hatch must face the tower. The hold-down arms keep the rocket down until its engines reach full power.
- **The crawler-transporter.** A multiblock carries the launcher, its tower and its rocket slowly along the **crawlerway**, a lane of gravel up to 1,024 blocks long, to the pad, sets it down there and comes back by itself. A player starts the trip by using one of its two cabs. Loaded it goes at 0.45 blocks a second, empty at twice that. NASA's crawlers date from 1965 and carry a rocket at under 2 km/h along a road of river gravel 5.5 km long. The bay doors in its way open while it passes. It will not take an unfinished rocket.

### The launch complex

- **The pad.** A flame trench runs under the launcher's exhaust opening: a row of Flame Deflectors below the ground, a ridge that splits the flame both ways along the trench, spanning it from one wall of Trench Lining to the other. The launcher sets down over it, on pedestals three blocks tall where the player sets them. The pad as the Structure Projector shows it is a square of light grey concrete with its corners cut, the trench dug five blocks deep along its middle and lined, a band of nine rows of deflectors under the pad's middle, and the crawler's two lanes of gravel from its edge to three blocks past its middle, where the crawler stops with the axis of any rocket the hangar builds over the band; and the service port by its edge. Its pedestals are not part of it, as they change with each launcher.
- **The deluge.** The **Pad Service Port** takes water by pipe and energy by cable, and a whole deluge (64 buckets and 400 kJ for its pumps, first estimates) is poured over the pad at ignition, to dampen the sound and protect the concrete. Without enough water or energy there is no deluge.
- **The tank farm.** A **propellant depot** is a sphere built as a cube of Sphere Shell of odd edge from three to nine blocks, on a column of Tank Leg under each bottom corner, with a Vent Stack on top and a Transfer Pump on the ground. It holds as much liquid oxygen or liquid methane as a ball that wide, by pipe through its pump, and lets it boil away if it falls apart. Through the pad's service port it fuels the rocket on the launcher, each tank to its propellant's mix, the pumps spending energy for every kilogram ([Propellants](propellants.md)). The depot the Structure Projector shows and the Multiblock Builder builds is the approved one, nine blocks wide on legs two tall; a depot of another size is built by hand.

The Structure Projector shows, and the Multiblock Builder builds, every multiblock of J's Space: the hangar, the crawler, the pad, the depot, the landing platforms, the ship landing pad and the Small Shipyard.

Real rules that become play, each with a consequence setting:

- **With no flame trench and no deluge, a launch wrecks the pad and what is around it**, as Starship's first flight did in 2023, tearing up the pad's concrete and throwing chunks of it far away.
- **No launch in a thunderstorm.** Apollo 12 was struck by lightning twice in its first minute, and the rule has existed ever since; in a storm, the panel holds the countdown.

**Stages landing:** a **landing platform** on land, a slab of Landing Pads three times as wide as the stages it takes, nine blocks for stages of three and fifteen for stages of five, with its terminal in the middle and a target painted across it; it holds one stage at a time. Built on a raft or a barge afloat it is a **floating platform**, as SpaceX's drone ships, the first landing at sea in 2016. Or a **fall zone** set on the flight computer, best at sea.

**Control:** the **Launch Panel** by the pad, an LCC-1 console with the countdown, the holds, the go and no-go board (vehicle, propellant, tower arms, deluge, weather, crew, range, clamps), the MASTER key, the IGNITION guard and the LAUNCH button under its cover; shutting the guard disarms the ignition under it. It finds the launcher on its pad and reads its rocket, and its plate shows the pad: pads are numbered, and a pad can be given a name. With the key and ignition armed and all go, COUNT RUNNING starts the count at T-60. The tower's arms swing back from T-42, the cover lifts at T-10, with LAUNCH pressed the engines light at T-6, and the rocket is released at T-0; without LAUNCH the count holds at T-6. It holds by itself on a no-go or in a thunderstorm. Whoever presses LAUNCH owns the rocket in flight, for their team to use.

**One count per pad, kept by the launcher.** The count belongs to the mobile launcher standing on the pad, not to the panel. The Launch Panel is one way of working it; the other is the LAUNCH page of the capsule's own flight computer, a sixth key along its top, open while the rocket stands on a launcher (the computer opens on it there). The page carries the panel's controls (the MASTER key, the IGNITION switch with its guard as one key, the deluge, the clamps, HOLD, COUNT, RECYCLE, SCRUB, and LAUNCH, lit only once its cover lifts at T-10) and the same count, board and tanks. What is done inside or out shows on both and either can hold. A pad needs no panel: a crew can count down and launch from the capsule alone. Whoever presses LAUNCH, at the panel or inside, owns the rocket. What a pad has none of (no service port, so no deluge; no tower arms) shows dark as NONE and holds nothing. Deluge and tower arms follow the count where they exist.

**What holds the count, and what moves with it.** Only what is NO-GO on the board holds the count: the vehicle, its propellant, the weather, the clamps, and the deluge where a service port's deluge is on AUTO and not whole. DLG OFF is the player's own choice and holds nothing (with no flame trench the pad is wrecked). TOWER is never NO-GO: it moves with the count. CREW and RANGE are always GO: the crew strapped in and the pad clear are the player's responsibility, and what goes wrong because of them is the player's, not the mod's. Every tower arm, the swing arms and the crew access arm, turns 90 degrees on its hinge at the tower away from the rocket, drawn turning from T-42 to T-10; while away its blocks lose their collision; it swings back on a SCRUB or once the pad stands free. The hold-down arms' jaws open at T-0 and close when the pad stands free. The capsule's Crew Seat is usable on the pad as in flight, and a seated crew stays seated through release.

### The parts

| Part | What it is | The real thing |
| --- | --- | --- |
| **Engine** | combustion chamber, turbopump and nozzle, burning methalox (liquid methane and liquid oxygen); **sea-level and vacuum** versions, at diameters 3 and 5 | Raptor and Raptor Vacuum |
| **Tank** | for methalox, of **variable height**, standing on a skirt over an engine | |
| **Separation ring** | marks a stage and drops it, with retro rockets below the line and ullage motors above it | the interstages of Saturn V |
| **Instrument ring** | the guidance platform in its middle, the telemetry antenna, avionics and battery panels, and the umbilical plate where the tower's arm plugs in. With a guidance platform formed, the landing ellipse is smaller | Saturn V's Instrument Unit |
| **Capsule** | the crew, with windows, a **heat shield** below, **parachutes**, a **flight computer**, thrusters, and a docking port on top; 1 seat at diameter 3, 3 at diameter 5, the crew lying on couches facing the nose | Vostok 1, Soyuz, Orion |
| **Landing legs** | for stages that come back and land; folded on the way up and spread to land | Falcon 9's legs |
| **Lander** | a **descent stage** (legs, engine, tanks, left on the ground as the launch platform) and an **ascent stage** (cabin with a flight computer, engine); the instrument ring of diameter 5 closes four panels round it | the Apollo lunar module |

Every part carries its tier label: diameter 3 at T1 and diameter 5 at T2. The numbers of each part (thrust, mass, Isp) are in [Implementation](implementation.md), and are first estimates from the real parts they follow.

### What goes wrong

**A failure is a consequence of the player's choices, never bad luck.** Whoever designs well and looks after their rockets almost never sees one blow up; there is no "2% chance of exploding on every launch".

| Choice | Consequence | The real thing |
| --- | --- | --- |
| A thrust-to-weight ratio under 1 | the rocket **doesn't leave the ground**: the engines light and it stays, burning propellant | |
| Too little delta-v for the destination | the screen warns in red but **lets it launch**: the rocket goes as far as its propellant takes it, and if it doesn't reach orbit, it **falls back** | |
| Launching with no flame trench and no deluge | the pad and its surroundings are damaged | Starship, 2023 |
| Coming down where there is air **with no heat shield**, or the wrong one for that atmosphere | the capsule **burns up on re-entry** and loses its top | Columbia, in 2003, was lost to damage to its heat shield |
| Landing **with no legs**, or no propellant to slow down | the lander **crashes** and loses its lowest blocks | |
| Waiting too long with a liquid kept cold in the tank | the propellant **boils away**: hydrogen fastest, then oxygen, then methane, and a liquid that keeps at the place's warmth not at all | |
| The **wrong propellant** in an engine | the engine **doesn't light**; what it does not burn is carried as dead weight | |
| A **hole** in a tank, an engine or a capsule | a holed tank leaks what it holds; a holed engine does not light; a holed capsule's heat shield is breached and it cannot aerobrake | |

Putting the right block back in its place mends the part wherever the rocket is.

Every one of these failures has a consequence setting in `jsspace-server.toml`; with them off, a badly built rocket only fails to leave the ground or to arrive.

## To build

### Parts are multiblocks

- **More diameters.** **7 blocks** for the heavy ones, as Saturn V, and **9 blocks** for the super-heavy, as Starship.
- **Adapters**, rings that narrow, join different diameters, as a thinner second stage on top of a first.
- **Solid boosters** stack in segments, as the Space Shuttle's came in four, and a **radial separation ring** drops them.
- **Engine mounts** hold one large engine or a **cluster** of small ones: Falcon 9 has nine Merlins, Saturn V five F-1s.
- **The assembly stations** make the special blocks of each part: combustion chambers and turbopumps from the engine station, tank plates from the tank station, heat shield tiles from the shielding station.
- **More engines and propellants:** solid, hydrolox, kerolox (with J's Industrial), hypergolic, nuclear thermal (with J's Industrial), metallic hydrogen and fusion engines ([Propellants](propellants.md)).

### Designing a rocket

With J's Computers installed, the **Aerospace Design Computer** runs **AeroCAD**, the same design screen as a program. It asks for good graphics cards and hardware of its own. When a design is finished, the computer sends it into the Rocket Hangar ([J's Computers](computers.md)).

### Building it automatically

- **With J's Computers**, if the Aerospace Design Computer and the hangar are on the network, J's Computers' autocrafting makes the parts that are missing.
- **Reuse.** Stages that landed on their platforms come back to the hangar, are inspected and repaired with a repair kit, and go into the next rocket, as Falcon 9 boosters fly more than twenty times.

The whole chain can run with nobody there: plan, parts made, the hangar builds, the crawler carries, the tank farm fills, the rocket launches on schedule ([Cargo](cargo.md)), the stage comes back, it is repaired, the next one.

### The hangar and the way to the pad

- **The Russian way.** The rocket is assembled **lying down**, in a long, low hangar, and goes to the pad **by train**, on a car that **raises it upright** at the end, as Soyuz rockets are taken out at Baikonur.

### The launch complex

| Piece | Russian |
| --- | --- |
| Platform | the four arms of the **"tulip"**, which hold the rocket hanging and open like petals as it rises |
| Tower | a **mobile service tower**, which rolls away on rails before launch |
| Flame trench | the **flame pit** of Gagarin's Start, an old quarry about 45 m deep |
| Deluge | water poured onto the pad at ignition, as in the American complex |
| Tank farm | the spheres of hydrogen and oxygen ([Propellants](propellants.md)) |
| Lightning | a mast |

- **Lightning towers**: the three lightning towers of pad 39B, with wires strung between them, on the American pad.
- **Mission Control**, from where everything is followed ([J's Computers](computers.md) for its computer form). A player launches from either the Launch Panel or Mission Control.
- **The tank farm** with the spheres of hydrogen and oxygen, with the propellants that need them.
- **On other worlds** there is no tower and no deluge: there is a **simple pad with berms**. A lander's engine throws the dust of the ground at full speed; the Apollo 12 lunar module **sandblasted the Surveyor 3 probe**, 160 m away, and the pieces brought back showed the marks. A real pad stops that, and in the game it is what keeps a landing from damaging the base next to it. There, the tank farm is the local propellant plant. (Today a landed rocket lifts off from bare ground.)
- A floating platform named after the starships of Iain M. Banks' novels ("Of Course I Still Love You", "Just Read the Instructions").

### The parts

| Part | What it is | The real thing |
| --- | --- | --- |
| **Engine mount** | the base of a stage, holding one large engine or a **cluster** of small ones | Falcon 9 has nine Merlins, Saturn V five F-1s |
| **Solid booster** | segments stacked, fixed at the side with a nose of its own | the Space Shuttle's |
| **Radial separation ring** | drops boosters | |
| **Adapter** | changes the diameter | |
| **Fairing** | protects the payload on the way up, and comes off | |
| **Payload adapter** | holds satellites, probes or a cargo container | |
| **Capsule** | more seats for larger diameters | Crew Dragon up to 7 |
| **Launch escape tower** | pulls the capsule away if the rocket fails on the way up | Soyuz's saved its crews in 1983 and in 2018 |
| **Habitation module** | a sealed interior for long missions, with zero-gravity beds and life support | |
| **Service module** | power (solar panels or fuel cells), manoeuvring propellant (hypergolic), radiators | the service modules of Apollo and Orion |
| **Control thrusters** | small jets to turn and dock | |
| **Instrument ring** | the Flight Control Computer, with J's Computers, and the communications antenna | |
| **Grid fins** | for stages that come back and land | Falcon 9's grid fins |
| **Docking port** | joins a rocket to a station | |
| **Cargo container** | cargo for automatic routes ([Cargo](cargo.md)) | |

### What goes wrong

| Choice | Consequence | The real thing |
| --- | --- | --- |
| **Reused stages never inspected** | every flight wears a part; inspected and repaired, it keeps flying; past its limit unrepaired, **it fails**: an engine shuts down, a tank cracks | reused boosters are inspected after every flight |
| A **damaged oxygen tank** used anyway | it **explodes** | Apollo 13 |

**When a failure comes:** with a crew and an **escape tower**, the tower pulls the capsule away and it comes down on its parachutes; the crew lives, which is what the tower is for, as with Soyuz MS-10 in 2018. With no tower, whoever is aboard dies with the rocket. An explosion destroys the rocket and does damage around it, and the wreckage can be salvaged, as fallen stages can ([Travel](travel.md)).
