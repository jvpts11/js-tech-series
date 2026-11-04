# J's Computers

What J's Space does with J's Computers installed: its machines that become computers, the hardware made for space, the
Operations, the programs, and the network stretched across worlds and stars. Everything here exists only with both mods
installed. **Hardware puts a machine on the network and gives it more; it never makes it work**: with J's Space alone,
every machine, rocket and starship works through its own screens and controls.

## What exists today

J's Computers' registries of computers and hardware, through which J's Space adds the ones below, are designed with
J's Computers and still to be built. None of the computers or hardware below is built in J's Space yet.

J's Space joins no data network yet. The Hangar Terminal of the Rocket Hangar and the Shipyard Terminal of the Small
Shipyard are **machines, not computers**: a member of the network that runs Operations is a computer, and a terminal
that only works its building is not one. They take no data cable and work through their own screens. Their Loading
Dock and Item Port take the blocks a build needs from a player, a hopper, any mod's pipe or J's Computers' buses. The
shipyard's control booth already stands as it will be worked: a bench a computer can stand on, and a socket in the
wall where the network port will go. The small starship's Computing module is furniture, a desk and a server rack.
The Flight Computer of a rocket is its flight display, not a computer of the network.

## To build

### The computers

J's Space adds them through J's Computers' API:

| Computer | Its form | Hardware of its own |
| --- | --- | --- |
| **Mission Control Computer** | the Mission Control block in its computer form: it gains the assembly window (motherboard, processor, memory, power supply, system), boots and joins the network | |
| **Satellite Control Computer** | the Ground Station in its computer form | **Satellite Cards**, each adding capacity: more antennas and more satellites |
| **Space Research Computer** | the Space Research Computer in its computer form; its data are files | **correlator cards**, which join the signals of several radio telescopes as ALMA's correlator does, and **image processing cards** |
| **Flight Control Computer** | the flight computer in a rocket's instrument ring or a starship's cockpit, which joins the network through the craft's communications antenna | **radiation-hardened processors**, as real ones: the RAD750 flies in Curiosity, Perseverance and the James Webb Space Telescope. **Better hardware controls more stages and more systems**, and takes a rocket further |
| **Aerospace Design Computer** | a new computer, with no machine form; its place is the bench of the Small Shipyard's booth and of the Rocket Hangar's control room | it asks for **good graphics cards**, to run AeroCAD |
| **Enigmatic hardware** | an Enigmatic processor and motherboard, outside the ladder of eras and about the size of the Singularity era ([Enigmatic technology](enigmatic-technology.md)) | |

**Flight Control Computers follow J's Computers' eras.** A Vintage one is the **Apollo Guidance Computer** (1966): 2 KB
of working memory, 1 MHz, and its program **woven by hand** into core rope memory by the weavers at Raytheon. It is
enough to land on the Moon, as it really was; rockets of more stages, and starships, ask for newer eras.

### Computers in starships

| Class | Its computers |
| --- | --- |
| **Small** | the Flight Control Computer alone, as a fighter has one computer |
| **Medium** | the Flight Control Computer and **system computers** (navigation, engines, life support) that **coordinate**, with **redundancy**: two or more of a kind check each other, and **a failure doesn't bring the ship down**; the one that failed lights a warning and is replaced. Without redundancy, when the only navigation computer fails, the ship has no navigation until it is mended. The Space Shuttle flew with five computers: four ran the same program and **voted** at every step, and the fifth ran **a backup program written by another company**, in case the fault was in the program itself; Crew Dragon flies with three that check each other |
| **Large** | the data deck runs **a cluster of servers**, J's Computers' way (the ship's Mainframe and its racks), and **heavy programs** spread across it as Operations, as J's Computers' simulation cluster does |

What a large ship gains from computing power:

- **the warp calculation**: more power, and **the drive spools up faster** and **leaves warp closer to its target**; for
  the jump to another galaxy, it is what decides;
- **the scanner's analysis**: discovery from orbit comes faster;
- **coordinating auxiliary ships**: how many a carrier launches, recovers and follows at once;
- **running the whole ship**: life support, the onboard factory, cargo;
- **the crew's programs**, as on any J's Computers network.

With J's Space alone, every starship flies on its own controls, and the intergalactic jump works with the base time and
precision.

### Operations

J's Space registers its kinds of Operation through J's Core's registry, and the state of everything is **a table for
SELECT**:

| Family | Operations |
| --- | --- |
| Rockets | assemble from a plan; launch; land; state; cargo routes (start, stop) |
| Starships | state; autopilot along a route; dock |
| Satellites | take, release, state, manoeuvre; firing the Laser Satellite, with **two authorisations** through the directory service |
| Observatories | start and stop a scan; state |
| Probes | launch, state, call back, pull the data; aeroprobes (descend, gather, climb) |
| Discovery | what is known of each world, as a table |
| Mining | the Deep Miner; mining probes |
| Terraforming | the state of a world's project; contributing |
| Megastructures | state; building a section; launching through the Mass Driver; sending up an elevator |
| Cargo | a flight asked for by autocrafting ([Cargo](cargo.md)) |

A building is worked from the network **through its computer**, never through its terminal. The Small Shipyard's booth
and the Rocket Hangar's control room each get a place for a computer, an Aerospace Design Computer on the bench with its
monitor, and a place for **J's Core's network port**, a block of the Core that serves any multiblock: the data cable
plugs into the port, which sees the building's computer and puts it on the network. Through that computer the network
keeps the designs, runs AeroCAD and the building's other programs, and starts the building on a plan, the network
bringing the blocks. Both places are optional: the building forms and works without them. They come when J's Space
grows; J's Computers' way for other mods to add their computers already exists, and J's Industrial's ICC uses it.

### Programs

- the **Satellite Manager**, for satellites;
- the **Atlas**, the map with landmarks, structures, the veins mapping satellites find, and "you are here";
- **AeroCAD**, the design of rockets and starships ([Rockets](rockets.md));
- **Mission Control**: navigation, missions and routes;
- a **planetarium**, the sky with what the team knows of it, as the real Stellarium, and **telescope control** by
  program, as real observatories are run;
- launches and routes can be automated with **Σ#** too.

### The network across worlds

- Communication satellites **extend wireless** over the area they cover, and **join networks on different worlds**
  through J's Computers' gateway ([Satellites](satellites.md)).
- The **instant link** federates networks between star systems.
- A starship's **data deck** carries a whole network ([Starships](starships.md)).
- With the networks of two worlds federated, **autocrafting asks for a cargo flight** when what it needs lies on another
  world.
- J's Computers' **offensive security** can cut another team's satellite loose.
- **Ruins** give old hardware and AI models ([Structures](structures.md)).
