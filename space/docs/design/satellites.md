# Satellites

How a satellite is made and put in orbit, how the ground talks to it, what each kind does, who owns it, and what happens
when it breaks. A satellite stays in orbit around a world and works for that world; a probe goes somewhere and works
there, and probes are in [Exploration](exploration.md) and [Mining in space](mining.md).

## What exists today

Nothing of J's Space exists yet. J's Core has a telemetry cable type, a point-to-point link of 256 blocks with no block
of its own, which the telemetry line below replaces.

## To build

### Making a satellite

A satellite is assembled at the **Spacecraft Station**, which builds satellites and probes, from components: a bus,
solar panels (and an RTG beyond Jupiter), a battery, an antenna, propulsion (cold gas, hydrazine or ion), an onboard
computer, and a **mission module**, which sets what kind of satellite it is. A satellite is an item, in three sizes: the
**CubeSat**, small and cheap, which rides along with another payload (the first ones flew in 2003); the **medium**
satellite; the **large** one. Better components give more coverage, more data or more precision. **Satellites power
themselves**, as real ones do ([Energy](energy.md)).

It goes up as a rocket's payload, on the payload adapter, and the rocket releases it **into the orbit chosen**. One
rocket can carry many, as Starlink satellites go up by the dozen.

### Orbits

**The orbit is what gives coverage.** In a world of blocks, an area is a region of the world's map:

| Orbit | It costs | It covers | Good for |
| --- | --- | --- | --- |
| **Low** | little delta-v | a strip that **passes over** and moves on; one satellite covers in pieces | a **constellation** of many gives continuous coverage, as Starlink does |
| **Polar**, synchronised with the sun | a little more | the **whole world, bit by bit**, strip by strip | **mapping**: the Landsat satellites have photographed the Earth this way since 1972 |
| **Geostationary** | a lot of delta-v | a **large, fixed area**, always the same | communication, weather, watching a base; Arthur C. Clarke proposed the idea in 1945 |
| **Deep space** (the Lagrange points, the orbit of another world) | the cost of the journey | whatever is there | telescopes (the James Webb Space Telescope sits at the L2 point) and relays |

A satellite appears in its world's orbit ([Travel](travel.md)): it can be seen going by, docked with and mended, as
astronauts mended Hubble five times. **Satellites don't wear out**: one lasts until it is destroyed.

### The ground

- **Antennas are dishes**, multiblocks in three sizes; size gives range and data rate. The **small** dish is for low
  orbit and weather, the **medium** for the geostationary orbit and the Moon, the **large** for deep space, as the 70 m
  antennas of the Deep Space Network. **The dish turns and follows the satellite.**
- **The telemetry line** runs from the antennas to the ground station through the devices of a real station, and every
  connected stretch is one ground system. It is a line of J's Core's cable block that carries data only, never the
  network's id.

  | Device | What it does | The real thing |
  | --- | --- | --- |
  | **Receiver** | amplifies the very faint signal that arrives | the cooled low-noise amplifier |
  | **Decoder** | turns the signal into data | the demodulator |
  | **Data Recorder** | keeps what the satellites send | from the tapes of old to disks |

  **Several antennas on the same system add up**, as the Deep Space Network joins dishes to hear the Voyagers, whose
  signal arrives fainter every year.
- **Contact.** A geostationary satellite is always in sight of the antenna under it. A satellite in low orbit **only
  talks while it passes** over an antenna: its data gathers aboard and comes down on the pass, as in reality. **Relay
  satellites** give contact all the time, as NASA's TDRS have served the International Space Station since 1983.
- **The Ground Station** is the antenna's console: the team's satellites and the state of each, the commands (a
  manoeuvre, the coverage, firing the Laser Satellite), and the warnings (an orphaned satellite, contact lost, a
  satellite destroyed, a battery low in eclipse). With J's Computers installed it is the **Satellite Control Computer**,
  and every **Satellite Card** in it adds capacity: more antennas and more satellites ([J's Computers](computers.md)).
- **Where the data goes:** from the Data Recorder to whoever uses it: the Space Research Computer processes scientific
  data ([Exploration](exploration.md)), the **Atlas** takes the maps, and Mission Control shows the rest.

### Talking faster

A message home has a delay, small in the game, a few seconds at most as far as Mars ([Travel](travel.md)). It shrinks
first with **relay satellites**, then with **laser communication terminals**, an optical station on the ground and a
terminal aboard, as NASA tested in 2023 with the Psyche probe, and at the end with the **instant link**, which comes
with the Enigmatic technology ([Enigmatic technology](enigmatic-technology.md)).

### Kinds of satellite

| Kind | What it does | Good orbit | The real thing |
| --- | --- | --- | --- |
| **Communication** | extends J's Computers' wireless over the area it covers, and joins networks on different worlds through J's Computers' gateway; on its own, it carries the commands of probes and rovers | geostationary, or a constellation | |
| **Relay** | continuous contact with satellites in low orbit and with whoever is far away | geostationary; around other worlds | TDRS; the orbiters of Mars relay the rovers' data |
| **Navigation** | a position anywhere on the world: the Atlas shows "you are here", and **the landing ellipse shrinks**, since the flight computer knows where it is ([Travel](travel.md)) | a medium constellation | GPS, 24 satellites, complete in 1995 |
| **Mapping** | makes the **Atlas**'s map and finds **ore veins** near the surface by the spectrum of the light they reflect; it also gives discovery data ([Exploration](exploration.md)) | polar | the CRISM spectrometer of the Mars Reconnaissance Orbiter found the clays of Mars from above |
| **Weather** | **forecasts** rain and thunderstorms in the Overworld (no rocket launches in a thunderstorm, [Rockets](rockets.md)), the dust storms of Mars, and **solar storms**, with a warning ahead | geostationary; the Sun | GOES, since 1975; SOHO, watching the Sun since 1995 |
| **Observation** | **a live image** of an area, on the Ground Station's screen or, with J's Computers, in a program. The first, old kind **drops its film capsule**, which the player picks up from the ground: it gives a map of that area | low or geostationary | Corona (1960 to 1972), whose film capsules came down on parachutes and were **caught in the air by aircraft** |
| **Space telescope** | astronomical data from above the atmosphere, far more than from the ground ([Exploration](exploration.md)) | low, or the L2 point | Hubble (1990), the James Webb Space Telescope (2021) |
| **Solar power** | sends energy to a rectenna ([Energy](energy.md)) | geostationary | |
| **Antiproton collector** | gathers antimatter inside a radiation belt ([Minerals](minerals.md)) | the belt | |
| **Debris collector** | grabs debris and takes it out of orbit | where the debris is | ESA's ClearSpace-1 |
| **Laser** | a tool and a weapon (below) | low | |
| **Planetary climate** | for terraforming ([Terraforming](terraforming.md)) | | |

With J's Warfare installed, it adds its own kinds through J's Space's API, as the **defence satellites** that intercept
missiles.

### Ownership

- A satellite belongs to its **team** and to the station that took it.
- **Orphans and transfers.** Its owner can **release** a satellite, which stays in orbit, orphaned and inactive, and any
  team with an antenna can take it: a way to give or trade satellites between teams.
- **Theft.** With J's Computers installed, its offensive security can cut another team's satellite loose and leave it
  orphaned. Without J's Computers, satellites can't be stolen.

### Debris

The Laser Satellite, as a weapon, and J's Warfare's defence satellites bring satellites down. A destroyed satellite
becomes **debris in orbit**, which stays there and can **strike** stations, ships and other satellites, and every strike
makes more: the **Kessler syndrome**, described by Donald Kessler in 1978, the real fear of low orbit becoming
impassable. A **debris collector** cleans it up. Some debris **falls onto the world**, and its pieces can be salvaged. A
consequence setting turns debris off.

### The Laser Satellite

- **As a drill**, as in Advanced Rocketry: a player marks a spot on the map and the laser **bores a shaft** into the
  ground of the world below. The ore comes up and is caught by a **receiver** placed at the spot; with no receiver, it
  falls to the bottom of the shaft.
- **As a weapon**: it fires at a spot, destroys the blocks within a small radius and harms whoever is there. The beam
  comes down from the sky **in sight of everyone**. It needs stored energy and a wait between shots. With J's Computers
  installed, firing asks for **two authorisations** through its directory service. The server's PvP setting turns the
  weapon off.
