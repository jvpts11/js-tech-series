# Hazards

What the world does to a body that stays where it shouldn't: radiation, contamination, other laws of physics, a dirty
place. J's Core keeps each as an **open kit**, so any mod adds kinds of its own with mechanics of their own, the way
HBM's mod has ordinary radiation and digamma radiation side by side; the series' own kinds are built from the same kits,
as any other mod's would be.

## What exists today

The dimension rules hurt a player in air they can't breathe, or in heat or cold past the limits, once a second, after
the Core posts a **hazard event** that any mod's suit cancels to protect its wearer ([The world](world.md)). There is no
radiation, dissonance or pollution yet.

## To build

### The hazard kit

A **hazard** is something that builds up in a body from where it is, and does harm by how much has built up. A mod
registers a kind of hazard with:

- **how it builds up**: from sources (blocks, items, entities, areas, a dimension, a chunk's pollution), at a rate the
  source gives;
- **what it does**: effects by **bands** of what has built up, from mild to deadly;
- **how it goes away**: recovery over time away from the sources, and what speeds it (a shower, a medicine);
- **what protects**: worn items and modules from any mod declare what they stop of which hazard, and how much;
- **how it is measured and shown**: its unit, the instruments that read it, the line on the screen.

Every kind has its **consequence setting**, so a server can turn it off. The values are kept per player, with the world.
Each kind also says **what treats it** (below) and has **death messages** of its own. Mobs suffer hazards too: animals
sicken and die in contaminated places.

### Kinds of damage and resistances

- **Kinds of damage** any mod registers: radiation, electrical, cryogenic, acid, heat, blast, pressure, and those a mod
  brings, each with its **death messages**.
- **Resistances per worn piece and per kind**, declared by any item, added up clearly and never past 100%.
- **The tooltip shows** each piece's resistances, and a view shows the total of what is worn.
- **Mobs have resistances too**, by kind: a robot doesn't suffer radiation, a creature of ice suffers from heat.
- **One language with the hazards**: the damage of each hazard is of a kind, and the hazard kit's protection and the
  resistances speak the same language.

### Hazards of items

An item can carry hazards that act **while someone carries it**, each a kind any mod registers, in the spirit of HBM's
Nuclear Tech:

| Hazard | What it does | What protects |
| --- | --- | --- |
| **Radioactive** | doses whoever carries it, by the amount in the stack, through the radiation kit | shielded containers, suits |
| **Hot** | a freshly cast ingot burns the hand | heat-resistant gloves |
| **Reacts with water** | lithium, sodium, potassium and caesium catch fire or explode when the carrier swims or is rained on | keeping them dry, or under oil |
| **Pyrophoric** | catches fire in the air (white phosphorus) | keeping it under inert gas or oil |
| **Unstable** | explodes in fire, in an explosion or from a hard knock (nitroglycerin) | careful handling |
| **Toxic to touch** | poisons the bare hand (mercury, beryllium dust, arsenic) | gloves |
| **Corrosive** | acids burn the bare hand | gloves |
| **Blinding** | something very bright (an arc welder, a fusion reactor's plasma window) blinds | a dark visor |

The tooltip shows each hazard and what protects from it.

### What is breathed

**Damage to the lungs**: dusts (coal dust, silica, asbestos fibres) build up a damage that lasts, and asbestos's hardly
heals at all; it brings a cough and less breath, and masks and respirators protect. They are the real diseases of
miners and builders: black lung, silicosis, asbestosis.

### Treatment

Each hazard says what treats it, with real treatments: potassium iodide taken before exposure to radioactive iodine,
Prussian blue for caesium contamination, chelation for plutonium and heavy metals. The medicines themselves are each
mod's items.

### Measuring and seeing

- **An instrument in hand shows the hazard on the screen**: with a Geiger counter, the rate, and the counter clicks
  faster; with a dosimeter, the dose taken.
- **A screen of the body**: dose, contamination, lungs and dissonance in one place.
- **A map of hazards** by chunk (radiation, pollution), through instruments or J's Computers' network.
- **The world shows contamination**: dead grass and withered plants where radiation or pollution is high.

### Radiation

Radiation is a family of hazards with more in common: it is **emitted** by sources, **travels**, and is **weakened by
the blocks it crosses**, each by its material. The Core gives that family a kit of its own, and any mod registers
**kinds of radiation**, each with its own mechanics, sources, effects and protection.

**Ionizing radiation**, the series' kind, measured in real units:

- each place has a **dose rate** (millisieverts an hour), and a player **builds up a dose** (sieverts) through **two
  channels**:
  - the **external dose** (gamma, neutrons), which only distance, time and shielding reduce: it crosses blocks weakened
    by their material, lead very much, concrete and stone well, water well, air hardly at all, so a wall is real
    protection;
  - **contamination**, radioactive dust carried on the skin or breathed in, which **keeps dosing** until a shower washes
    it off, and which a suit and a respirator stop;
- **effects by bands of the dose built up**, after the real acute radiation syndrome, named **Rad I to V**:

  | Band | Dose (first estimate) | What happens |
  | --- | --- | --- |
  | Rad I | about 1 Sv | mild nausea |
  | Rad II | about 2 Sv | slowness and nausea |
  | Rad III | about 4 Sv | weakness and slow damage |
  | Rad IV | about 6 Sv | heavy damage, blurred sight |
  | Rad V | 8 Sv and more | close to deadly |

- the body **recovers slowly** away from the sources;
- **protection cuts what reaches the body, channel by channel**: lead stops a little of the external dose, a hazmat
  suit stops contamination; each piece of protection says how much of each it stops;
- **sources**: what mods declare (fuel, waste, reactors, cosmic radiation, radiation belts, violent stars), and every
  mineral with uranium or thorium in it, at a low rate ([Materials](materials.md)), with a setting to turn the
  minerals' radioactivity off;
- **fallout** is a kind of pollution (below) that raises the dose rate of the chunk it lies on.

### Dissonance

A hazard of the physics kit: where the local laws of physics depart from the normal ones ([The world](world.md)), the
difference builds up in a body, **faster the further the laws are from ours**, and falls back in normal laws.

| Level | What happens |
| --- | --- |
| I | sight warps and colours shift |
| II | the player's own gravity drifts, and sounds change pitch |
| III | time stutters: moves catch and skip now and then, and hunger rises faster |
| IV | small involuntary jumps through space, and slow damage |
| V | heavy damage; staying is dying |

Protection is a **bubble of normal laws** around the body (an armour module, a block that makes a zone), which spends
energy; without energy it turns off and dissonance starts counting.

### Pollution

Pollution is kept **per chunk**: it spreads to neighbouring chunks, **fades as plants take it up**, and is emitted by
any mod's blocks (chimneys, vents, vehicles), which say what they emit. Like radiation, it is an **open kit**: any mod
registers **kinds of pollution** of its own, with how each spreads, fades and harms.

The series' kinds, each with what it really does:

- **Soot and particulates**: a haze over the chunk; over a threshold, whoever breathes it without a mask coughs (mild
  slowness and fatigue), as in London's smog of 1952.
- **Sulfur and nitrogen oxides**: **acid rain** when it rains there: leaves die slowly, crops grow slower, exposed
  copper and calcite wear away, as in Europe's "Black Triangle".
- **Heavy metals in soil and water**: crops grown and fish caught there are contaminated, and eating them poisons, as in
  Minamata (mercury) and Itai-itai (cadmium). The soil is cleaned by replacing it, or by plants that draw metals up.
- **Carbon dioxide**: its real effect is on the whole planet, not on a chunk, so it is kept as **one number per
  server**, shown, with no effect yet; what it does is designed when J's Agriculture and J's Geology are.
- **Radioactive fallout**: it spreads chunk by chunk and decays over time, and it raises the dose rate where it lies.

Cleaning equipment is each mod's (J's Industrial's scrubbers and filters).

### What is the series' own

The hazard, radiation and pollution kits are the platform's; **ionizing radiation, dissonance and the series' kinds of
pollution** are the series' own, in its package, built only from the kits ([The API](api.md)).
