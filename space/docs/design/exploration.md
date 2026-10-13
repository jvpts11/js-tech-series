# Exploration

How a team comes to know a world: what is measured and what it reveals, the observatories on the ground and in space,
the probes, the Space Research Computer, the hunt for the hidden planets, and the milestones. Satellites are in
[Satellites](satellites.md); what a world holds is in [The universe](universe.md) and [Minerals](minerals.md).

## What exists today

Nothing of J's Space exists yet. J's Core already keeps progression a mod can read, which discovery and milestones are
built on, and the knowledge gate that lets a recipe ask for something the team knows is designed with J's Space and J's
Industrial and still to be built in J's Core.

## To build

### Knowing a world

**Discovery is knowing a world's properties.** Each property is revealed by an instrument, and a world's percentage is
how much of it the team knows. Discovery belongs to the **team**, as J's Industrial's research does.

| Property | What it reveals in the game | Revealed by | The real thing |
| --- | --- | --- | --- |
| **Orbit and position** | the world shows on the flight map, and can be aimed at | an optical telescope; **astrometry** for stars | |
| **Size and mass, and so gravity** | its gravity, and whether a rocket can land and leave | imaging; **tracking a probe** | the twin GRAIL probes measured the Moon's gravity in 2012 |
| **Atmosphere** | pressure and composition: **which suit, which heat shield** | **spectroscopy** | |
| **Temperature** | heat and cold | **infrared** | |
| **Magnetic field and radiation** | the dangers: belts, the dose | **radio** and a **magnetometer** | Jupiter's radio has been heard from the Earth since 1955 |
| **Relief** | the map of the surface, and where to land | a **camera**; **radar**, which sees through clouds | Magellan's radar mapped all of Venus under its clouds |
| **What lies on the ground** | minerals and deposits | a **spectrometer**; **gamma rays and neutrons** | Mars Odyssey found ice buried on Mars with a neutron spectrometer in 2002 |
| **The interior** | hidden oceans, the core | **gravimetry**; a lander's **seismometer** | InSight listened to the quakes of Mars, 2018 to 2022 |
| **Structures** | ruins and Enigmatic sites | an **archaeological instrument** ([Structures](structures.md)) | |
| **Samples** | everything, in the greatest detail | **a sample brought back** | Apollo, OSIRIS-REx (2023), Hayabusa2 |

**The ladder of missions**, the real order in which a world is explored: observe it from the Earth; fly past it with a
probe; orbit it; land a lander or a rover; bring back a sample; go with people.

**The doors:**

- **Sending a probe** needs the world's orbit and position.
- **Landing with a crew** needs knowing what kills there, **its atmosphere, temperature, radiation and gravity**, and
  **a map of its relief** for the landing ellipse; not 100%, **what it takes to survive**, as in reality: nobody walked
  on the Moon before the Surveyor probes had landed.
- **100%** is a **milestone**, and completes the world's Atlas and its map of minerals.
- A server rule, on by default, holds landings until the essentials are known.

### Observatories on the ground

They are multiblocks; their size is the mirror or the dish, and bigger sees more.

| Observatory | What it reveals | Where and when it works | The real thing |
| --- | --- | --- | --- |
| **Hand telescope** | the planets in the sky, to look at ([The universe](universe.md)) | at night, under a clear sky | Galileo's telescope, 1609 |
| **Optical telescope**, a dome with a mirror of variable size | position and orbit, size, a coarse view of the surface; with a **spectrograph** fitted, the atmosphere and the composition | **only at night under a clear sky**; higher is better, above the clouds | from Newton's reflector (1668) to Keck (10 m, 1993) and the ELT (39 m, being built). Mirrors go from Newton's speculum metal (with J's Industrial) to silvered glass (Foucault, 1857) and aluminium; the best are of Specula's lechatelierite |
| **Radio telescope**, a dish of variable size | magnetic fields, Jupiter's radio, **pulsars**; in **radar** mode, the relief of near worlds | day and night, in any weather | from Jodrell Bank (76 m, 1957) to FAST (500 m, 2016). Pulsars were found by radio, by Jocelyn Bell Burnell, in 1967; Arecibo's radar found the ice of Mercury's poles in 1991 |
| **Radio telescope array** | dishes joined add up and see finer | as a radio telescope | the VLA has 27 dishes |
| **Infrared telescope** | temperature | only **high and dry**, since the water vapour of the air absorbs infrared | Mauna Kea, at 4,200 m |
| **Gravitational wave interferometer**, an L with arms of variable length (longer is more sensitive) | **mergers of neutron stars and of black holes**: it finds the kilonova remnant ([Minerals](minerals.md)) | on firm, quiet ground: it is sensitive enough to feel lorries go by | LIGO, with arms of 4 km, heard the first gravitational wave in 2015 |

**X-rays and gamma rays can only be observed from space**: the Earth's atmosphere blocks them. The first X-ray telescope
went to space in 1970 (Uhuru).

### Observatories in space

They are satellites or probes:

| Telescope | What it gives | The real thing |
| --- | --- | --- |
| **Optical and ultraviolet** | what the ground's optical telescope gives, with no atmosphere, and far more | Hubble (1990) |
| **Infrared** | temperature and the cold universe; at the L2 point, or **on Acheron** ([The universe](universe.md)) | the James Webb Space Telescope (2021) |
| **X-ray and gamma ray** | black holes and neutron stars: Cygnus X-1, Lich | Chandra (1999), Fermi (2008) |
| **Exoplanet hunter** | **finds the planets of other stars** by their transit, the tiny shadow a planet makes passing in front of its star: the first step in choosing a destination among the stars | Kepler (2009 to 2018) found thousands this way; TESS goes on since 2018 |
| **Astrometry** | where the stars are, for discovering systems; the measurement that **opens interstellar navigation** is still the one taken from Terminus, 60 AU out | Gaia measured 2 billion stars (2013 to 2025) |
| **Solar** | the Sun: warnings of solar storms | SOHO (1995) |

### Probes

Uncrewed spacecraft, assembled at the **Spacecraft Station**, the one that builds satellites, and launched as a rocket's
payload.

| Probe | What it does | The real thing |
| --- | --- | --- |
| **Flyby** | passes once, fast and cheap, and sends what it saw | Mariner 4 at Mars (1965); the Voyagers |
| **Orbiter** | stays in orbit measuring all the time; it can relay too | |
| **Lander** | lands and stays: a seismometer, soil analysis on the spot | Viking (1976), InSight |
| **Rover** | **drives**, covering a wider area as it goes; the player commands it from afar through the Ground Station (with J's Computers, also through a program) | from Sojourner (1997) to Perseverance (2021) |
| **Drone** | flies where there is air and scouts ahead of a rover | Ingenuity made the first powered flight on another world (2021, 72 flights); Dragonfly will fly on Titan |
| **Sample return** | lands, gathers, lifts off and sends **its capsule back**, which falls in the Overworld with the sample | the OSIRIS-REx capsule landed in Utah in 2023 |
| **Aeroprobe** | goes down inside a giant, measures and gathers ([Mining in space](mining.md)); the **deep** one seeks metallic hydrogen | Galileo's probe dived into Jupiter in 1995 and lasted 58 minutes |
| **Mining probe** | the small bodies ([Mining in space](mining.md)) | |
| **Impactor** | strikes a world: shows what is inside, and **deflects asteroids** | Deep Impact (2005); DART **moved the moon of an asteroid** in 2022 |
| **Solar probe** | dives close to the Sun: the Sun's data, and a milestone | Parker Solar Probe passed 6.1 million km from the Sun in 2024 |
| **Interstellar probe** | leaves the heliosphere and, on a **laser sail**, reaches Alpha Centauri: **the data of its planets before anyone goes** | Voyager 1 crossed the heliopause in 2012; Breakthrough Starshot aims gram-scale sails at a fifth of the speed of light |
| **Solar gravitational lens probe** | beyond 550 AU, the best image possible of a planet of another star ([The universe](universe.md)) | |

- **Data comes down the telemetry line** ([Satellites](satellites.md)): the probe has to be in contact, and far away
  that takes the large dish or a relay. Without contact, it keeps its data aboard.
- **A destroyed probe loses what it hadn't sent.** Its wreck, salvaged, gives back part of its parts and the data it
  kept.
- **Probes don't wear out**: one lasts until it is destroyed or called back.
- **Failures come from the design, never from luck**: near Jupiter, without a shielded vault, the electronics die (Juno
  carries a vault of titanium for that); on Venus an ordinary lander lasts minutes (Venera 13 lasted 127), and one with
  **electronics made for the heat** lasts as long as the player likes, as NASA designs one in silicon carbide to work
  for 60 days.
- With J's Geology installed, the **impactor is planetary defence**: if its meteors come with a warning, an impactor
  launched in time deflects what is coming.

### The Space Research Computer

**The telemetry line is J's Space's whole data line**: it joins antennas, observatories, data recorders and the Space
Research Computer. Data also comes **on media**: a sample capsule, a film capsule, the recorders' tapes.

The **Space Research Computer** works as J's Industrial's research computer does: it takes the data, **processes** it
and turns it into the known properties of each world, with a queue of work. Several on the same stretch of the **Space
Research Link Cable**, a line of J's Core's cable block that is J's Space's own, **form a group** and add up, as
research groups do. Mission Control shows each world's sheet: what is known, what is missing, and with which instrument.

With J's Computers installed, it is also a computer, its data are files, and its hardware is real: **correlator cards**,
which join the signals of several radio telescopes as ALMA's correlator, a dedicated supercomputer, does; and **image
processing cards** ([J's Computers](computers.md)). With J's Industrial installed, its research computer also reads
astronomical data, for its discoveries that touch space.

### The hunt for the hidden planets

- **Oceanus (Planet Nine):** observe Sedna and the most distant bodies over time to get their **orbits**. With enough
  orbits, the Space Research Computer works out **the region of the sky** where the unseen mass has to be, and a large
  telescope pointed there on a clear night finds it.
- **Terminus (Planet Ten):** map the Kuiper Belt, many orbits of small bodies, with a **survey telescope** (the Vera
  Rubin Observatory sweeps the whole sky every few nights). The Space Research Computer measures the **warp** of its
  plane and points to the place.
- The first team to find one names it ([The universe](universe.md)). A name is kept with the world, has a maximum
  length, uses only letters and repeats no other world's name; an operator can undo an offensive one. The naming can be
  turned off, and then the default names stand.

Other things to find: the planets of other stars, by the exoplanet hunters; the kilonova remnant, by the gravitational
wave interferometer; the map of the stars, by the astrometry of Terminus.

### Milestones

Milestones belong to the team, and other mods can read them. They are the real firsts of spaceflight:

| Milestone | The real one |
| --- | --- |
| First suborbital flight | the V-2 was the first object to reach space, in 1944 |
| First satellite | Sputnik 1 (1957) |
| First person in orbit | Gagarin (1961) |
| First spacewalk | Leonov (1965) |
| First docking | Gemini 8 (1966) |
| First probe on another world | Luna 2, on the Moon (1959) |
| First landing on the Moon | Apollo 11 (1969) |
| First rover | Lunokhod 1 (1970) |
| First landing on another planet | Venera 7, on Venus (1970) |
| First sample brought back | Luna 16 (1970), the first robotic one |
| First space station | Salyut 1 (1971) |
| First probe beyond the heliopause | Voyager 1 (2012) |
| First person on another planet | it hasn't happened yet |
| Terminus found; Oceanus found | it hasn't happened yet |
| First world known to 100% | |
| First starship; first star; first galaxy | |

The Enigmatic sites add milestones of their own ([Enigmatic technology](enigmatic-technology.md)).
