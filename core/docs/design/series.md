# The series

J's Core is the library every mod of the J's Tech Series stands on. This page says who depends on whom, and what each
mod takes from the Core. How the series is versioned and released is in the series' own pages
([Versions, phases and releases](../../../docs/RELEASING.md)).

## What exists today

J's Computers and J's Industrial are built on the Core, at the same version, and so is a mod for testing that is never
published.

## To build

### Who depends on whom

**Every mod of the series depends only on J's Core, and on no other mod of the series.** When several are installed,
they work together through what the Core gives them all (the network's model, Operations, capabilities, the event bus,
the shared catalogue and content) and through optional integrations that only wake up when the other mod is there. Any
technology mod outside the series may stand on the Core too.

### What each mod takes from the Core

| Mod | What it stands on |
| --- | --- |
| **J's Computers** | the data network's model and the Operations framework; peripheral links; the eras and their axis; state as tables; the interface toolkit, fonts, motion and sound; the guide framework; energy through the low-voltage class |
| **J's Industrial** | the materials and their states; the energy kit and J's Energy, the cable block and overhead lines; machines and multiblocks; veins, reservoirs and strata; hazards (radiation, pollution, dissonance) and the physics kit for the Aleph; the tier axis, team progression and the knowledge gate; the containment of exotic matter |
| **J's Space** | dimensions, their sky, day, weather and rules by altitude; the physics kit; sealed rooms and the Atmosphere Detector; moving structures; structure generation; milestones and the knowledge gate; the basic lines and battery; the containment of exotic matter; ionizing radiation; worn modules for its suits |
| **J's Transport** | moving structures and entity vehicles |
| **J's Warfare** | projectiles and their ballistics, entity vehicles, hazards |
| **J's Agriculture** | the climate of biomes, pollution's effects on crops, sealed rooms for greenhouses, robots |
| **J's Geology** | rock strata, veins and reservoirs, fields over chunks |
| **J's Oceanics** | seas and their liquids, sealed rooms under water, entity vehicles for vessels |
| **J's Robotics** | the robot kit |
| **J's Civil Works** | multipart and microblocks, structure generation |
| **J's Overworld** | the biome kit, terrain declared in code, landforms and structures; it stands outside the series and on the Core alone |

### What the Core takes from no one

The Core depends on NeoForge alone. Integrations with other mods (JEI, EMI, Jade, FTB Teams, Curios, Accessories,
GeckoLib until the Core's own models replace it, Mekanism's chemicals) wake up only when those mods are installed, each
behind a guard, so none of them is ever required.
