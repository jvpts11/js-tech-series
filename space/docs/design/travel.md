# Travel

How a player leaves a world, waits in orbit, crosses to another world and comes down on it. J's Space is not a flight
simulator: a journey has real things behind it (delta-v, launch windows, gravity assists, the dangers of the way), shown
simply and drawn the way Minecraft draws things. Rockets open the Solar System; starships open the universe. How rockets
are built is in [Rockets](rockets.md), and how starships are built in [Starships](starships.md).

## What exists today

A rocket built in the Rocket Hangar leaves the Overworld, reaches orbit, crosses to the Moon or to Mars, comes down on it and can leave again. J's Core's dimension rules hold each space's gravity and air, its kit of moving structures carries the rocket, and a star system's space appears the first time someone reaches it. A small starship is built in its own shipyard and flown by hand.

### A rocket is built from multiblocks

Every part of a rocket is a **multiblock**: an engine, a tank, a ring, a capsule, a lander. A player builds the parts in the Rocket Hangar, stacks them, and a crawler carries the rocket to the pad ([Rockets](rockets.md)). The rocket's **delta-v comes from the parts that are there**: their mass, their engines' efficiency and their stages, summed by the rocket equation. A rocket built from any blocks ends up shapeless; a rocket of multiblocks always looks like a rocket, and building a good one is the game.

In flight, the rocket is a single body that draws the models of its parts, and every stage that comes loose becomes a body of its own. A rocket belongs to whoever built it and is flown by their team.

### Leaving a world

1. **On the pad.** The rocket stands on its mobile launcher, beside the umbilical tower whose arms reach its tanks and its capsule. White vapour pours from the loaded tanks, boiling liquid oxygen, as at every real launch. The countdown is the launcher's, one for the pad: it runs on the Launch Panel and on the rocket's flight computer, and the crew can work it from the capsule's LAUNCH page with no panel at all.
2. **Ignition.** The engines light a few seconds before the rocket is released. The pad's deluge water turns to steam, smoke rolls out of the flame trench, the tower's arms have swung back and the hoses drop away, and the screen shakes for everyone near.
3. **The climb.** The rocket rises straight up, pushed by its engines' thrust at the air the world has, its mass falling as the propellant burns, the world's pull drawing it back. It rises through the world **in sight of everyone on the server**, with a plume of flame and smoke behind it. The loaded land goes on past the edge of the game's own view as a far map of the real ground, with the game's clouds above it, so the ground melts into the distance with no seam. The sky turns from blue to dark blue to black and the stars come out. From halfway up the eye rises ever faster as the ground bends into the world's sphere. **How long the climb takes depends on the rocket**: one with thrust to spare climbs fast, a weak one climbs slowly, and one too heavy for its engines sits on the pad burning its propellant, or, where the server keeps such engines unlit, has its launch scrubbed at ignition with nothing spent.
4. **Staging.** Each stage whose engines have burnt out is let go with a jolt (below).
5. **Into space.** About seven hundred blocks above the world's building limit, the rocket passes into the **system's space**, whole, with the very picture the space shows and **no loading screen**. The propellant it burnt in the climb is gone from its tanks.
6. **In orbit.** Each place on a world arrives at its own spot of the low orbit, laid out like a map of the world, so a station built there stands over its base. There is no air: **silence**. Loose things float, and the world turns below.

On the Moon and on Mars a landed rocket lifts off from the ground with no pad, once its transfer is set and its window has come. The climb is short where gravity is low: the Moon's is 0.17 g, and its sky is black from the ground.

### Stages

A stage whose engines have burnt out below the one burning is let go: its blocks and its separation ring part off with a jolt and a puff round the ring, the ring's retro rockets push it away, and it goes on as a body of its own, its parts still drawn whole. Side boosters burn with the first stage and come loose before it, each pushed out from the core, and fall where the first stage is set to go below, in a fall zone round the pad when that stage comes back to a platform. The rocket climbs on as it was. Before launch, on the flight computer's STG page, the player sets where each stage goes:

- **a fall zone**: a circle round the pad with a radius the player chooses (64, 128, 250 or 500 blocks); the stage falls somewhere in it;
- **a landing platform**: a stage that has landing legs and an engine can come back. It flies back over its platform, relights its engines and comes straight down on its middle, as Falcon 9 boosters have done since 2015. A platform takes one stage at a time, and a floating platform on a raft or a barge works the same way, as SpaceX's drone ships do;
- **anywhere**: the stage falls in a part of the world the game has loaded round the players, never where new land would have to be made. It strikes the ground as a blast, leaves a crater and **Rocket Wreckage**, and breaking the wreckage gives back about a third of the stage's blocks. A consequence setting of the server turns the damage off: the stage then only lies there as wreckage.

A stage that lands on a platform leaves no wreckage.

### In orbit

Every star system has a space of its own, a dimension where its worlds are round bodies in their places, and a world's orbit is the space around its sphere ([The universe](universe.md)).

- **No air and no gravity.** No sound carries, and a loose thing floats where it is let go. A player breathes only in a sealed room or in a pressure suit ([Life support](life-support.md)).
- **The world below** is a smooth sphere painted in pixel art, never with block textures, with **clouds above it that keep changing**. The rocket goes round faster than the world turns, so the world slides by underneath: the International Space Station goes round every 92 minutes and sees sixteen sunrises a day, and at the Overworld's pace an orbit takes a little over a minute, with sixteen sunrises to its day.
- **What is there:** the rockets parked in orbit. The world keeps a record of everything in the orbits of its worlds. A rocket that passes into an orbit is told there as it arrives, whose it is, and is forgotten as it goes back down; a Space Persistor aboard keeps its place loaded.

### Choosing where to land

The flight computer's LAND page shows **a map of the world**, drawn as the game's map item draws a world: what the team knows of it, from its instruments, from the passes of its crews over the ground and from the places its players have stood. Everything else is under the grey of the unknown. On arriving in an orbit, and on every pass of the orbit after, the crew photographs a strip of the ground under it, the world turning a little under it on each pass. On a new world with no data the map is nearly empty, and landing is a leap in the dark. Ground too steep to land on is marked in red, with a count of such places inside the ellipse. In a corner, a small map of the whole world shows how much of it is known.

The player marks a spot, and the computer draws the **landing ellipse**, the area where the rocket will really come down, as on real missions (Perseverance's was 7.7 by 6.6 km). **Its size comes from the guidance**: a rocket with only its flight computer lands within an ellipse twice as large as one whose instrument ring carries a formed guidance platform, which lands within about 7,700 by 6,600 blocks. SET fixes the landing at the mark.

A crew lands only on a world whose atmosphere, temperature, radiation, gravity and relief its team knows; a consequence setting of the server lifts that rule.

### Coming down

Resting in the orbit of the world its landing is set on, the rocket leaves its orbit for the spot over the landing, sinks into the world's sky and comes down somewhere in its ellipse.

- Where there is air it glows as it enters, its **heat shield** taking the heat, and it opens its three orange and white **parachutes**. In thick air, as the Overworld's, the parachutes set the capsule down. In thin air, as Mars', its engines brake it the last of the way to its legs. Where there is none, as on the Moon, its engines brake it all the way down, and the dust rises flat in rays at touchdown.
- With no heat shield fit for the air, the rocket **burns up** as it enters and loses its top. With no legs, or no propellant left to brake on, it **crashes** and loses its lowest blocks. A consequence setting of the server turns each of these off: the rocket then cannot begin to come down, and stays in its orbit.

Leaving again, an **ascent stage** can leave its descent stage behind as its launch platform, as the Apollo lunar modules did: their descent stages are still on the Moon.

### The flight computer

Every capsule and every lunar lander's cabin carries a **flight computer**: a flat display with keys along its top and sides. It plans the transfer, chooses the landing spot, runs the stages and flies the rocket. Its pages are XFER (the transfer), LAND (the landing) and STG (where each stage goes); SYS and COMM are named on its keys and open nothing. Its face shows on the capsule's panel and on the lander's front wall, with the live readings of the flight: the mission clock, propellant, climb, throttle, attitude, cabin air and the lamps.

### Planning a transfer

The XFER page draws the system round the Sun with the planets moving along their orbits, the same positions they have in the Overworld's sky.

- **A year of the Earth lasts seven Overworld days**, a week. The planets move at that pace, and so do the windows: a launch window to Mars comes about every 5 real hours. A launch within about ten degrees of the best angle counts as in the window. The Moon has no window: a player goes whenever they like.
- **A player can always launch, in a window or not.** The player chooses the window to leave in: now, the next, or one up to four ahead, with a tape that shows how long until it opens. Outside the window the transfer costs more delta-v, up to twice as much from the worst place; waiting for the window saves propellant.
- **The player chooses how much delta-v to spend.** Spending more than the least, up to twice, shortens the cruise, to a quarter of the usual at the shortest.
- **A gravity assist** spends less (about 15% less) and takes longer (about 60% longer): that is the real trade, and Cassini took seven years to reach Saturn by swinging twice past Venus, once past the Earth and once past Jupiter.
- **Aerobraking**, where the destination has air and the rocket a heat shield, saves the capture burn.

The page shows the phase now and at the window, the delta-v now and in the window, the time of flight and whether the rocket brakes in the air or with its engines. It also says whether the rocket carries a heat shield and how much delta-v it has left for the transfer, less the climb to orbit while it stands on the ground (on its pad, or landed on another world), and says when that is not enough. EXEC sets the plan for the rocket to fly. A transfer sets out only with the delta-v for its capture as well. The page plans from a world's orbit too, to the other worlds, so a rocket in the Moon's orbit goes on to Mars without landing. A plan that cannot be flown is let go and the crew told why, once: a transfer whose window comes with too little delta-v, a landing set on another world than the one the rocket orbits, a lift-off with no stage left that has an engine and propellant.

### The cruise

The crew lives inside the rocket, in the capsule, which is a sealed room with air ([Life support](life-support.md)). When its window has come, the rocket burns its transfer burn and sets out, its crew told where it goes and for how long. It really travels through the system's space, on a gentle arc, slow to leave and slow to arrive, with time squeezed; through the window the world left behind shrinks and the destination grows. The space ahead loads before the rocket gets there; where it is not ready yet, the rocket waits for it and the cruise runs a little longer. With nobody aboard and no chunk loader, the cruise waits, as any moving structure does.

**How long it takes depends on how fast the player travels**: spending more delta-v makes a shorter trip, which is why better engines matter. As a reference:

| Destination | Real missions | In the game |
| --- | --- | --- |
| The Moon | 3 days (Apollo) | about 1 minute |
| Mars | about 8 months | about 8 minutes |

**Sleeping skips the trip** when everyone aboard sleeps, as in a bed.

**What happens on the way**, for whoever stays awake:

- **A solar storm**: a warning twenty seconds ahead, then half a minute of radiation filling the open space, while the crew shelters behind the water and the cargo, as in the shelter planned for Orion on the Artemis missions.
- **A micrometeoroid**: a hole in the hull, the air of the room behind it rushing out (a twentieth of it every second through each hole), until a patch is put on.
- **A failure**: a part burns through and loses a block, its work failing until the same block is put back.

A consequence setting turns the events off; the cruise is then only the view.

### Arriving

At the end of its cruise the rocket comes to rest at the spot of the other world's low orbit and a **capture burn** puts it into orbit around the destination. Where there is air and the rocket's heat shield is whole, it can **aerobrake** instead, dipping into the atmosphere to slow down and spend less, as the Mars Reconnaissance Orbiter did; a shield breached on the way burns the capture burn it meant to save. Short of the delta-v, the rocket burns what it has and stays in a wide orbit of the destination instead of a low one, and the crew is told. From a wide orbit no landing begins: LAND says to capture first, and the XFER page offers the capture as its first key, CAPT, with its burn and what the rocket has; EXEC burns it at once, or, with AERO and a whole heat shield where there is air, the rocket dips into the air for it. While the capture is chosen, the keys of a transfer's window, delta-v and assist stay blank. A transfer to another world plans from the wide orbit as from the low one. A rocket coming to rest in an orbit where another rests, or is bound, takes the nearest spot with room for it beside the other. Then comes the destination's orbit, and choosing where to land; in an orbit, LAND maps the world below.

### Fast travel

A consequence setting of the server, **off by default**, skips the cruise: the rocket still climbs to orbit and leaves in the window it chose, but arrives at once, for those who don't want the journey.

### Starships

A small starship is built in the **Small Shipyard**, a dry dock, from the Starship Design screen, by hand from its hologram or by the dock's four robot arms ([Starships](starships.md)). It becomes a moving structure when its pilot lets it go from the dock, and it stays one, parked or flying. Its design changes only in a dock. A starship is not spent like a rocket: it has a name, and it is the crew's home.

- **The player flies it**, in the spirit of No Man's Sky: it goes where its nose points, with no drift. The pilot sits at the helm, opens and closes the throttle, turns and pitches the ship, and uses a boost, fed by the reactor's spare power, to cross an empty stretch. In the air its air engines push and its wings hold it up; in thin air and in space its bells push, and in space it flies free. Weight counts: a heavier ship gathers speed more slowly and needs more of it to stay up.
- **It takes off straight up.** Its air engines' nozzles turn down, as the Harrier's and the F-35B's do, so below flying speed it hovers on them. Two kinds of landing gear: **wheels** roll on the ground and on a runway, **feet** stand on rough ground. Both take off and land vertically, and both can use a landing pad. The gear folds flat into the floor as the ship climbs away.
- **It climbs to space without stopping**, through the same passage as a rocket: over the build limit it flies on through the open sky and crosses into the system's space whole, with no loading screen, its pilot still at the helm.
- **Autopilots**, changed by a key: **LAND** flies it to the nearest ship landing pad its pilot may use, or to a runway marked by a Runway Beacon for a ship on wheels; **ORBIT** climbs at 55 degrees and boosts into space; **DOCK** brings its collar to another ship's that would let its pilot aboard, in line, and holds there. Docked ships are made one body, and the crew goes through the collar from cabin to cabin with no air let out.
- **The cabin is a sealed room** wherever the ship flies, with air, kept warm. A fusion reactor burns deuterium as the ship flies, a full load lasting four hours aloft, and a dock fills it again.
- **A starship has a crew.** The pilot's team works aboard, and everyone else stays outside until the owner names them. The owner sets roles (pilot, crew, passenger) on the crew page of the ship's flight console.
- **Players walk inside a starship in flight.** Its blocks are real and really fly, on J's Core's kit of moving structures: what works on the ground works aboard.

## To build

### Leaving every world

Leaving is different on every world, by its data: on Mars the sky is butterscotch and the dust rises; on Titan the thick orange haze only ends high up; in the clouds of Venus the rocket leaves from a floating base. The worlds past the Moon and Mars come with their own pages ([Worlds](worlds.md)).

### Stages

- A stage with a **fairing** or a **payload adapter** for what it carries, and stages that come back to the hangar to be reused ([Rockets](rockets.md)).

### In orbit

- **A jetpack** in a space suit, so the player can turn on every axis, even tilt their head sideways: in space "up" is wherever the player looks ([Life support](life-support.md)). A spacewalk follows from it.
- **Satellites, probes and the debris of space** in orbit ([Satellites](satellites.md)). The record of what is in orbit already has places for them.
- **Stations are built from modules**, multiblocks like those of the International Space Station: nodes, laboratories, habitation modules, trusses with solar panels. A player chooses the modules and fits them together, so a station always looks like a station. Rockets **dock** at their ports.

### Choosing where to land

- **A better guidance** shrinks the ellipse until the rocket lands on the pad. Apollo 12 landed 160 m from the Surveyor 3 probe, on purpose.
- **Coming home to the Overworld**, the player lands on the home pad or anywhere.

### Coming down

- **Parachutes on Titan**, with its own air and its own pace ([Worlds](worlds.md)).

### The flight computer

With J's Computers installed, it is the **Flight Control Computer**, a J's Computers computer that joins the network through the rocket's **communications antenna**, with **hardware made for flying**. A Flight Control Computer with better hardware **controls more stages and more systems**, which takes the rocket further ([J's Computers](computers.md)). The SYS and COMM pages open when it comes.

### Planning a transfer

Windows to the worlds past Mars follow the same rule:

| Destination | A launch window every |
| --- | --- |
| Venus | about 3.7 real hours |
| Jupiter and the outer planets | about 2.5 real hours |

### The cruise

- **Life support**: oxygen, water and food run down and the CO₂ scrubber works. A rocket badly stocked doesn't arrive.
- **A failure** of some equipment, mended with a repair kit.
- **Zero-gravity beds**: without an **artificial gravity generator** on board, the crew sleeps in sleeping bags strapped to a wall, as on the International Space Station. With one, in ordinary beds. (Today a crew sleeps in any bed.)
- **A spacewalk** in a suit with a jetpack.

The trips to the farther worlds:

| Destination | Real missions | In the game |
| --- | --- | --- |
| Jupiter | 2 to 5 years | about 15 minutes |
| Saturn | about 7 years (Cassini) | about 20 minutes |
| Pluto | 9.5 years (New Horizons) | about 30 minutes |
| Terminus and Oceanus | decades | about 40 minutes |

### Talking home

A message home takes time, and light doesn't let that go away: from Mars, real light takes 3 to 22 minutes. In the game the delay is **small**, a few seconds at most as far as Mars, and it shrinks as J's Space brings **faster ways to talk**: relay satellites, laser communication (which NASA tested in 2023 with the Psyche probe, tens of millions of kilometres away) and, at the end, an instant link ([Satellites](satellites.md)).

### Flights with nobody aboard

Probes and automatic cargo ([Cargo](cargo.md)) make the cruise only as a timer, with nothing aboard to live.

### Starships

The rest of the starships, which come later:

- **Three classes.** **Small** ships are like fighters. **Medium** ships are like the Millennium Falcon, and with a module for an auxiliary ship they carry one or two small ships. **Large** ships are several large multiblocks joined together; they carry small and medium ships, so a large ship can reach another galaxy and let the smaller ones go off and explore. A medium or large ship has its own shipyard and takes off vertically.
- **Within a system, free flight from world to world.** A starship flies freely through its system's space, around moons, along a ring, wherever the pilot likes, with no windows; its **boost** crosses the empty stretches, so a small ship goes from the Earth to Mars in about a minute, where a rocket takes eight. Autopilots land on a marked spot on another world, not only on a pad of the same world.
- **Between the stars, the class sets the reach**, because the interstellar drive a hull can hold depends on its size:

  | Class | Its drive | Reach |
  | --- | --- | --- |
  | Small | the short drive | the nearest stars, about 10 light-years: Alpha Centauri, Barnard's Star, Luhman 16, Sirius |
  | Medium | the galactic drive | the whole Milky Way |
  | Large | the intergalactic drive, too big for any other hull | Andromeda and Triangulum |

  Behind each drive is real physics or the series' bet, never a simulator: fusion and antimatter for the near stars, a warp drive for the galaxy (the space around the ship moves, as in Miguel Alcubierre's metric of 1994, held open by negative-mass exotic matter), and a larger warp for other galaxies. Under warp, the stars stretch into streaks through the window.
- **On board**, the sections of the larger classes are sealed rooms with life support and a gravity generator. The dangers of the way between stars are other: **interstellar dust**, which at a tenth of the speed of light strikes like a bullet (the real problem of the Breakthrough Starshot probes) and calls for a shield at the bow; cosmic radiation; failures that pile up on a long voyage. Crew can sleep through the voyage in **hibernation pods**, and fast travel applies to starships too.
- **Starships carry weapons**, J's Space's own, always fired by a player at the controls: a fighter has its guns. With J's Warfare installed, it adds weapons of its own through J's Space's API.
- **Arriving**, the starship leaves its cruise at the edge of the system, crosses to the chosen world and flies down to it.
