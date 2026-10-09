# Starships

The second game of J's Space. Rockets open the Solar System; starships open the universe. A starship is built on the
ground from multiblock sections, flown by the player in the air and in space, lived in, and taken between the stars. How
starships travel is in [Travel](travel.md).

## What exists today

Nothing of J's Space exists yet, nor J's Core's kit of moving structures that starships fly on.

## To build

### Three classes

| Class | Like | Tier | Reach between the stars | Carries |
| --- | --- | --- | --- | --- |
| **Small** | a fighter | T5 | about 10 light-years | |
| **Medium** | the Millennium Falcon | T6 | the whole Milky Way | one or two small ships, with an auxiliary ship bay |
| **Large** | several large multiblocks joined together | T7, with its intergalactic drive at T8 | other galaxies | small and medium ships |

A small ship is almost all cockpit, wings and engine; its journeys are short. A large ship is lived in. Starships have
**a style of their own**, the series', since no real starship exists to copy.

### Building

Each class is built in **its own shipyard**, a multiblock with a frame around the ship's space, like a dry dock: the
small shipyard is the size of an aircraft hangar, the medium a large hangar, the large an open-air dock, huge.

Building works as for rockets ([Rockets](rockets.md)): a **terminal** in the shipyard opens the design screen, and a
**hologram** shows where every block goes; with J's Computers installed, the Aerospace Design Computer's **AeroCAD**
designs starships too and sends the design to the shipyard; and **automatic assembly** works the same way, through the
shipyard's item port, or through an Operation with J's Computers.

### Sections

Every section is a multiblock. With J's Computers or J's Industrial installed, the sections that belong to them exist
too.

| Section | What it does | Small | Medium | Large |
| --- | --- | :---: | :---: | :---: |
| **Cockpit, bridge** | where the ship is flown from | cockpit | cockpit | command bridge |
| **Hull and corridors** | the structure; the inside a crew walks through | ✓ | ✓ | ✓ |
| **Wings** | lift in the air | ✓ | ✓ | |
| **Air-breathing engines** | flying in the air | ✓ | ✓ | |
| **Lift engines** | holding the ship's weight in the air without wings | | ✓ | ✓ |
| **Main engines** | thrust in space; the **fusion torch** on large ships | ✓ | ✓ | ✓ |
| **Manoeuvring thrusters** | turning, docking, landing precisely | ✓ | ✓ | ✓ |
| **Landing gear** | wheels or feet on small ships; feet on the others | ✓ | ✓ | ✓ |
| **Reactor** | power: fusion; antimatter on large ships | ✓ | ✓ | ✓ |
| **Life support and gravity generator** | air, water, food, CO₂; ordinary beds | | ✓ | ✓ |
| **Quarters** | beds, **hibernation pods** | one seat | ✓ | ✓ |
| **Cargo hold** | cargo | small | ✓ | ✓ |
| **Auxiliary ship bay** | carries smaller ships | | one or two small | small and medium |
| **Interstellar drive** | short, galactic or intergalactic | short | galactic | intergalactic |
| **Bow shield** | interstellar dust; shielding against radiation | ✓ | ✓ | ✓ |
| **Scanner** | measures worlds in passing: **discovery data from orbit**, as No Man's Sky's scanner ([Exploration](exploration.md)) | ✓ | ✓ | ✓ |
| **Weapons** | hardpoints (below) | ✓ | ✓ | ✓ |
| **Docking port, airlock** | stations, other ships, spacewalks | ✓ | ✓ | ✓ |
| **Flight computer** | the Flight Control Computer, with J's Computers | ✓ | ✓ | ✓ |
| **Free bay**, of variable size | **an empty room where any block goes**: J's Industrial's machines, J's Computers' racks, crops, whatever the player likes. It makes a large ship a mothership | | ✓ | ✓ |
| **Data deck** | rack mounts, cooling and the network trunk joined to the ship's antennas: **a whole network aboard**, which **federates** with the networks on the ground through J's Computers' gateway when there is contact | | ✓ | ✓ |
| **Networked hold** | the cargo hold as the network's storage, and onboard autocrafting makes things from what is there | | ✓ | ✓ |
| **Industrial deck** | a high-voltage bus and the lines of pipes and cables for **a production line** aboard | | | ✓ |
| **Mining laser** | mines asteroids and surfaces from above, as in No Man's Sky | ✓ | ✓ | ✓ |
| **Gas scoop** | **gathers the air** of a giant, or of a world from a low orbit, and makes propellant: a real idea, the PROFAC of 1959, which proposed filling tanks while skimming an atmosphere | | ✓ | ✓ |
| **Fabrication bay** | **printing** aboard: rocket parts, probes, satellites, repair kits; the International Space Station has had a 3D printer since 2014 | | ✓ | ✓ |
| **Probe bay** | **builds and launches probes from space**; a probe comes back to the ship when it has samples or results to bring, as the Space Shuttle released Magellan, Galileo and Ulysses from its payload bay (1989 to 1990) | | ✓ | ✓ |
| **Onboard shipyard** | a large ship **builds small ships** inside itself | | | ✓ |
| **Observatory dome** | a telescope aboard | | ✓ | ✓ |
| **Laboratory** | a Space Research Computer aboard | | ✓ | ✓ |
| **Greenhouse** | food and oxygen on long voyages; with J's Agriculture installed, its crops | | ✓ | ✓ |
| **Sick bay** | heals, and heals a dose of radiation | | ✓ | ✓ |

The data deck and the networked hold exist with J's Computers installed, and the industrial deck with J's Industrial;
without them, the free bay is always there.

### A starship really flies

A starship's blocks are **real blocks that really fly**, on J's Core's **kit of moving structures**: they live in a
region of the world kept for them, where they run as they would on the ground, and they are drawn and collided where the
ship is. So **machines run, computers boot and crops grow aboard** while the ship flies, and players walk inside it in
flight. Sections are multiblocks, so the game knows exactly their model, their shape and how they behave: drawing them,
walking through them and striking them is the easy case. Only a free bay, where any block goes, is handled block by
block. J's Transport uses the same kit for its vehicles.

### Flying

- **The player flies a starship in the spirit of No Man's Sky**: it goes where it points, with no drift and no orbital
  sums. In the air, wings give lift, and a winged ship flying too slowly falls; in space, it flies freely.
- **Taking off and landing depend on the landing gear.** Medium and large ships take off vertically; small ships have
  **wheels**, and take a runway as an aeroplane, or **feet**, and take off vertically. A ship climbs to orbit without
  stopping, with the same passage into space as a rocket ([Travel](travel.md)), and lands on any firm ground. **Weight
  counts**: a large ship may be unable to take off from LHS 1140 b, at 1.9 g.
- **Autopilots**: land on a marked spot, climb to orbit, dock.
- **Docking**: at a station's ports, and into a carrier's bay, flying into the large ship's hangar.
- **Within a system, free flight**, with the **boost** across the empty stretches; **between the stars**, the
  interstellar drive, engaged only from orbit ([Travel](travel.md)).

### Interstellar drives

| Drive | Class | The physics behind it | Fuel |
| --- | --- | --- | --- |
| **Short** | small | **fusion** and **antimatter**, below the speed of light, with time squeezed | deuterium and helium-3; antimatter cells |
| **Galactic** | medium | **warp**: the space around the ship moves (Miguel Alcubierre's metric, 1994) | **negative-mass exotic matter**, and the reactor |
| **Intergalactic** | large | **the larger warp**, a bubble only a large hull can hold | far more negative-mass exotic matter |

**Negative-mass exotic matter** exists in no nature and no real physics. J's Space makes it in **its own Casimir
generator**, simpler and slower, starting from the Casimir effect (negative energy between two plates, measured in
1997); with J's Industrial installed, its Casimir Array is the scale path.

### Weapons and shields

Weapons are J's Space's own, **always fired by a player at the controls**:

| Weapon | What it is | The real thing |
| --- | --- | --- |
| **Cannon** | shells in bursts | real rotary cannons |
| **Laser** | a beam of energy; it also mines | real defence lasers, such as the US Navy's HELIOS |
| **Missiles** | guided onto a locked target | |
| **Energy shield** | holds off blows until it is spent, and recharges | the series' bet |

Targets are other players' ships, when the server allows PvP, and the hostile creatures of the worlds
([Worlds](worlds.md)). With J's Warfare installed, it adds its own weapons through J's Space's API.

### Damage

**Every multiblock of a starship has durability.** When it reaches zero, **the multiblock breaks somewhere and some of
its blocks are really destroyed**: a ship back from a fight shows its holes. A destroyed engine takes away thrust, a
lost wing takes away lift, a holed hull **decompresses** its room (J's Core's sealed-room kit). A repair kit mends it. A
destroyed ship becomes a **wreck**, part of which can be salvaged.

### What a starship spends

The main engines spend propellant, fusion fuel on the larger ships; the reactor its fuel; the boost and the weapons the
reactor's energy. Fuel runs out, and a gas scoop refuels at a giant.

### Crew and ownership

- **There is no fixed crew limit**: a starship takes as many as it has seats, beds and hibernation pods for; its class
  already limits it.
- A starship belongs to its **team**. Its owner decides who flies it, who goes in and who only boards, through J's
  Core's teams (FTB Teams, when installed).
- **A parked starship**, in a system's space or landed on a world, stays there when everyone leaves. What runs aboard
  follows the usual rules of loaded chunks; J's Core's Space Persistor keeps a ship loaded for whoever wants it to work
  on its own.
