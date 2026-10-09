# Travel

How a player leaves a world, waits in orbit, crosses to another world and comes down on it. J's Space is not a flight
simulator: a journey has real things behind it (delta-v, launch windows, gravity assists, the dangers of the way), shown
simply and drawn the way Minecraft draws things. Rockets open the Solar System; starships open the universe. How rockets
are built is in [Rockets](rockets.md), and how starships are built in [Starships](starships.md).

## What exists today

Nothing of J's Space exists yet. J's Core already makes dimensions while the game runs, which is how a star system's
space appears the first time someone reaches it, and its dimension rules already hold a dimension's gravity, so a space
with no gravity is a matter of data. Its kit of moving structures, which rockets and starships fly on, is designed with
J's Space and still to be built.

## To build

### A rocket is built from multiblocks

Every part of a rocket is a **multiblock**: an engine, a tank, a stage of structure, a capsule, a habitation module, a
heat shield. A player builds the parts in the Rocket Hangar, stacks them, and a transporter carries the rocket to the
pad. The rocket's **delta-v comes from the parts that are there**: their mass, their engines' efficiency, their stages.
A rocket built from any blocks ends up shapeless; a rocket of multiblocks always looks like a rocket, and building a
good one is the game ([Rockets](rockets.md)).

In flight, the rocket is a single body that draws the models of its parts, and every stage that comes loose becomes a
body of its own.

### Leaving a world

1. **On the pad.** The rocket stands beside its **service tower**, with the hoses connected. White vapour pours from the
   cryogenic tanks, boiling liquid oxygen, as at every real launch. The countdown runs on the Launch Panel and on the
   rocket itself.
2. **Ignition.** The parts become one body and the engines light: flames and clouds of smoke in square particles, like
   vanilla's. The pad's deluge water turns to steam, the hoses drop away, the camera shakes.
3. **The climb.** The rocket rises through the world **in sight of everyone on the server**, with a trail of smoke and a
   roar heard from afar. From the capsule window the world shrinks into a map, the rocket **goes through the clouds**,
   and the sky turns from blue to dark blue to black as the stars come out. **How long the climb takes depends on the
   rocket**: one built with plenty of thrust and delta-v climbs fast, a weak one climbs slowly.
4. **Staging.** Each spent stage comes loose with a jolt and becomes a body of its own (below).
5. **Into space.** Several hundred blocks above the building limit, with the sky already black, the view passes into the
   **system's space**, into the world's **orbit**, with a short transition and no loading screen in sight.
6. **In orbit.** A last short burn and the engine cuts: **silence**. Loose things float, and the world turns below.

Leaving every world is different, by its data: on the Moon there is no air and no cloud, the sky is black from the
ground and, at 0.17 g, the climb is very short; on Mars the sky is butterscotch and the dust rises; on Titan the thick
orange haze only ends high up; in the clouds of Venus the rocket leaves from a floating base.

### Stages

When a stage comes loose:

- if its multiblock has a **fall zone** set, it falls there;
- if the stage can come back and its multiblock has a **landing platform** set, it relights its engines and **lands on
  its platform**, as Falcon 9 boosters have done since 2015 and as Starship's booster was caught by its tower in 2024. A
  platform takes one stage at a time;
- **if nothing is set, it falls anywhere in the part of the world already generated.** It strikes the ground, leaves a
  crater and wreckage, and the wreckage can be salvaged for part of its material back. A consequence setting of the
  server turns the damage off: the stage then only lies there as wreckage.

### In orbit

Every star system has a space of its own, a dimension where its worlds are spheres in their places, and a world's orbit
is the space around its sphere ([The universe](universe.md)).

- **Vacuum and no gravity.** Without a space suit, it kills. In a suit, its **jetpack** lets the player turn on every
  axis, even tilt their head sideways: in space "up" is wherever the player looks ([Life support](life-support.md)).
- **The world below** is a smooth sphere painted in pixel art, never with block textures, with **clouds above it that
  keep changing**. The rocket goes round faster than the world turns, so the world slides by underneath: the
  International Space Station goes round every 92 minutes and sees sixteen sunrises a day, and at the Overworld's pace
  an orbit takes a little over a minute, the line of dawn sweeping across the world on every turn.
- **What is there:** parked rockets, stations, satellites ([Satellites](satellites.md)) and the debris of space.
- **Stations are built from modules**, multiblocks like those of the International Space Station: nodes, laboratories,
  habitation modules, trusses with solar panels. A player chooses the modules and fits them together, so a station
  always looks like a station. Rockets **dock** at their ports.

### Choosing where to land

The flight computer shows **a map of the world**: what the team knows of it, from probes, mapping satellites and earlier
visits. On a new world with no data the map is nearly empty, and landing is a leap in the dark.

The player marks a spot, and the computer shows the **landing ellipse**, the area where the rocket will really come
down, as on real missions (Perseverance's was 7.7 by 6.6 km). **Its size comes from the guidance**: a simple guidance
gives a huge ellipse, a good one shrinks it until the rocket lands on the pad. Apollo 12 landed 160 m from the Surveyor
3 probe, on purpose. Coming home to the Overworld, the player lands on the home pad or anywhere.

### Coming down

The same as leaving, the other way: the orange glow of **re-entry** around the capsule, in pixels; **parachutes** where
there is air (the Earth, Mars, Titan); **retrorockets** where there is none (the Moon); the dust rising at touchdown.
Leaving again, an **ascent stage** can leave its descent stage behind as its launch platform, as the Apollo lunar
modules did: their descent stages are still on the Moon.

### The flight computer

Every rocket carries a **flight computer**: it plans the transfer, chooses the landing spot, runs the stages and flies
the rocket.

With J's Computers installed, it is the **Flight Control Computer**, a J's Computers computer that joins the network
through the rocket's **communications antenna**, with **hardware made for flying**. A Flight Control Computer with
better hardware **controls more stages and more systems**, which takes the rocket further
([J's Computers](computers.md)).

### Planning a transfer

The flight computer shows the system with **the planets moving along their orbits**, the same positions they have in the
Overworld's sky.

- **A year of the Earth lasts seven Overworld days**, a week. The planets move at that pace, and so do the windows:

  | Destination | A launch window every |
  | --- | --- |
  | Venus | about 3.7 real hours |
  | Mars | about 5 real hours |
  | Jupiter and the outer planets | about 2.5 real hours |

  The Moon has no window: a player goes whenever they like.
- **A player can always launch, in a window or not.** Outside the window it costs more delta-v; waiting for the window
  saves propellant.
- The plan shows the delta-v **with gravity assists and without them**. With them it costs less and takes longer: that
  is the real trade, and Cassini took seven years to reach Saturn by swinging twice past Venus, once past the Earth and
  once past Jupiter.

### The cruise

The crew lives inside the rocket, in the parts with an interior (the capsule, the habitation module), which are sealed
rooms with air ([Life support](life-support.md)). The rocket really travels through the system's space, on its flight
computer, with time squeezed; through the window the world left behind shrinks and the destination grows. A player can
go out on a spacewalk.

**How long it takes depends on how fast the player travels**: spending more delta-v makes a shorter trip, which is why
better engines matter. A trip of the usual kind, as a reference:

| Destination | Real missions | In the game |
| --- | --- | --- |
| The Moon | 3 days (Apollo) | about 1 minute |
| Mars | about 8 months | about 8 minutes |
| Jupiter | 2 to 5 years | about 15 minutes |
| Saturn | about 7 years (Cassini) | about 20 minutes |
| Pluto | 9.5 years (New Horizons) | about 30 minutes |
| Terminus and Oceanus | decades | about 40 minutes |

**Sleeping skips the trip** when everyone aboard sleeps, as in a bed. Without an **artificial gravity generator** on
board, the crew sleeps in **zero-gravity beds**: sleeping bags strapped to a wall, as on the International Space
Station. With one, in ordinary beds.

**What happens on the way**, for whoever stays awake:

- **Life support**: oxygen, water and food run down and the CO₂ scrubber works. A rocket badly stocked doesn't arrive.
- **A solar storm**: a warning, and the crew shelters behind the water and the cargo, as in the shelter planned for
  Orion on the Artemis missions.
- **A micrometeoroid**: a hole in the hull, the air rushing out through J's Core's sealed-room kit, and a patch to put
  on.
- **A failure** of some equipment, mended with a repair kit.

A consequence setting turns the events off; the cruise is then only the view.

### Talking home

A message home takes time, and light doesn't let that go away: from Mars, real light takes 3 to 22 minutes. In the game
the delay is **small**, a few seconds at most as far as Mars, and it shrinks as J's Space brings **faster ways to
talk**: relay satellites, laser communication (which NASA tested in 2023 with the Psyche probe, tens of millions of
kilometres away) and, at the end, an instant link ([Satellites](satellites.md)).

### Arriving

A **capture burn** puts the rocket into orbit around the destination. Where there is air and the rocket carries a heat
shield, it can **aerobrake** instead, dipping into the atmosphere to slow down and spend less, as the Mars
Reconnaissance Orbiter did. Then comes the destination's orbit, and choosing where to land.

### Flights with nobody aboard

Probes and automatic cargo ([Cargo](cargo.md)) make the cruise only as a timer, with nothing aboard to live.

### Fast travel

A consequence setting of the server, **off by default**, skips the cruise: the rocket goes from launch straight to the
destination's orbit, for those who don't want the journey.

### Starships

Starships travel differently from rockets. They come later, they last, and the player flies them.

- **Built on the ground**, each class in a shipyard of its own, with all its systems inside ([Starships](starships.md)).
  A starship is not spent like a rocket: it has a name, and it is the crew's home.
- **Three classes.** **Small** ships are like fighters. **Medium** ships are like the Millennium Falcon, and with a
  module for an auxiliary ship they carry one or two small ships. **Large** ships are several large multiblocks joined
  together; they carry small and medium ships, so a large ship can reach another galaxy and let the smaller ones go off
  and explore.
- **The player flies them**, almost like an aeroplane, in the air and in space, where they fly freely. A starship takes
  off from its shipyard or from anywhere, climbs through the air to orbit as a rocket does but flown and with no stages,
  and **lands on any ground that holds it**. Weight counts: its engines have to beat the world's gravity, so a large
  ship may be unable to take off from LHS 1140 b, at 1.9 g, and lands on a world of 0.4 g with ease. In the air it flies
  on wings and air-breathing engines; in vacuum, on thrusters.
- **Within a system, free flight.** A starship flies freely through its system's space, from world to world, around
  moons, along a ring, wherever the pilot likes, with no windows; its **boost** crosses the empty stretches, so a small
  ship goes from the Earth to Mars in about a minute, where a rocket takes eight. Takeoff depends on its landing gear:
  medium and large ships take off vertically; small ones have wheels, and use a runway as an aeroplane, or feet, and
  take off vertically. Autopilots land on a marked spot, climb to orbit and dock.
- **Between the stars, the class sets the reach**, because the interstellar drive a hull can hold depends on its size:

  | Class | Its drive | Reach |
  | --- | --- | --- |
  | Small | the short drive | the nearest stars, about 10 light-years: Alpha Centauri, Barnard's Star, Luhman 16, Sirius |
  | Medium | the galactic drive | the whole Milky Way |
  | Large | the intergalactic drive, too big for any other hull | Andromeda and Triangulum |

  Behind each drive is real physics or the series' bet, never a simulator: fusion and antimatter for the near stars, a
  warp drive for the galaxy (the space around the ship moves, as in Miguel Alcubierre's metric of 1994, held open by
  negative-mass exotic matter), and a larger warp for other galaxies. Under warp, the stars stretch into streaks through
  the window.
- **On board**, the sections are sealed rooms with life support and a gravity generator. The dangers of the way between
  stars are other: **interstellar dust**, which at a tenth of the speed of light strikes like a bullet (the real problem
  of the Breakthrough Starshot probes) and calls for a shield at the bow; cosmic radiation; failures that pile up on a
  long voyage. Crew can sleep through the voyage in **hibernation pods**, and fast travel applies to starships too.
- **Players walk inside a starship in flight.** Its blocks are real and really fly, on J's Core's kit of moving
  structures: machines run, computers boot and crops grow aboard while it flies ([Starships](starships.md)).
- **Starships carry weapons**, J's Space's own, always fired by a player at the controls: a fighter has its guns. With
  J's Warfare installed, it adds weapons of its own through J's Space's API.
- **Arriving**, the starship leaves its cruise at the edge of the system, crosses to the chosen world and flies down to
  it.
