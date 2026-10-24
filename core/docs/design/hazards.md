# Hazards

What the world does to a body that stays where it shouldn't: radiation, contamination, other laws of physics, a dirty
place. J's Core keeps each as an **open kit**, so any mod adds kinds of its own with mechanics of their own, the way
HBM's mod has ordinary radiation and digamma radiation side by side; the series' own kinds are built from the same kits,
as any other mod's would be.

## What exists today

Everything this page designs is built, as of October 2026: the hazard kit, hazards of carried things and shielded
containers, the lungs, a body's warmth, treatment, radiation and its kinds, ionizing radiation and contamination,
dissonance and pollution and its cleaning, with the hazard devices and their maps, and the hazards of materials. How a
mod uses it, with code: [Hazards](../HAZARDS.md).

## The hazards of materials

A material carries its **hazard classes** as real data, as a safety data sheet gives them: the GHS's (explosive,
flammable, oxidising, corrosive, acutely toxic, harmful, health hazard, hazardous to the environment) and the two a
sheet marks on their own (an asphyxiant gas, radioactivity). The series' elements and compounds carry theirs; a data pack
changes any of them.

- **The nine GHS pictograms follow from them**, in the order of their codes, GHS01 to GHS09. The exclamation mark gives
  way to the skull, as the GHS says. The gas cylinder is no class of a substance but of its container: it shows for a gas
  held above the open air's pressure, never for one in a room. A material's tooltip lists its pictograms by their
  meaning, then the hazards outside the GHS.
- **What a cloud of a gas does** is read off the same classes, for any mod that lets a gas out: it burns (flammable,
  explosive), it poisons (acutely toxic, harmful), it suffocates (asphyxiant), it feeds a fire (oxidising), it eats metal
  (corrosive), it irradiates (radioactive). Harm over time, a health hazard or to the environment, is no cloud's.
- **The clouds are the Core's**: a gas let out into the open is a cloud, kept with the world, that spreads and thins
  until it is gone, or goes into a sealed room's air. Its limits are real data of its material: the share of the air a
  flammable gas explodes between (4% to 75% for hydrogen), set off by a flame, lava or a fire in it, and what of a
  toxic gas is immediately dangerous to life (10 ppm for chlorine), which a gas mask stops; an asphyxiant takes the
  place of the oxygen.

## The design

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

**Shielded containers**: what a container holds is carried too, and its radiation reaches the carrier through the
container's walls, weakened by their density and thickness as a block of the same material weakens it, so a box with
lead walls five centimetres thick lets about a thirtieth through. Which containers shield, and how, is data; one that
says nothing shields nothing. What touches the hand does not act through a container.

### A body's warmth

A player's core warms and cools in kelvin, as a real body's does: what the place gives a body (the air, the sun, water,
wind, hot and cold blocks near) reaches it through what it wears, each worn thing's insulation a number in W/(m²·K)
given as data with whether it keeps the wind and the water out, and the body adds what it makes by working, shivering
as it cools and sweating as it warms. Past the bands of the hazard kit come **hypothermia** below 35 °C (mild),
32 °C (moderate) and 28 °C (severe), and **overheating** above 37.5 °C (heat exhaustion) and 40 °C (heatstroke), each
with its effects and death message, shown on the HUD while the core is out of its normal band, the screen's edge
frosted or hazed.

- **Acclimation**: a body that stays in a cold or a hot climate moves its bands toward it over an hour of play, up to
  three quarters of a degree, and loses that away from it.
- In a dimension of the game's own rules, the air is the biome's climate; a sealed room's air and a biome's own
  climate stand where they are given.

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
- **The hazard devices** are handheld items of the Core's shared content, one family that looks like the real
  equipment: a yellow case with a black face. The **Geiger counter** shows on its face the needle on a scale of tens,
  the rate, its range and a light for every click, with range and sound buttons on the case; the **dosimeter** shows
  the dose taken, its band and the bands reached. In hand, each shows on the HUD as the device itself; the player puts
  it anywhere on the screen and gives it the size wanted ([The interface](interface.md)); a click holds it up, full
  size. The game's subtitles name the clicks for those who don't hear them.
- **A screen of the body**, opened by a key of its own: warmth, dose, contamination, lungs and dissonance in one
  place: what is worn, how much heat it lets out, whether it keeps the wind and the water out and how much of each kind
  of harm it stops; the core's warmth on a scale of its bands, with what the place does to it; the dose taken on a
  scale with the bands painted on it, the rate here (from outside, and from what is on the body) and a word asking for
  a shower; the lungs; dissonance; and the other mods' hazards. Its look can change: it is a screen declared in a
  file, which a resource pack lays out again, drawn in the style the player picks ([The interface](interface.md)).
- **A map of hazards** by chunk (radiation, pollution), in two versions: a **handheld device** of the same family,
  whose screen shows the land around with the hazard on it, live, and a **screen** with the whole map, opened from the
  device and shown by J's Computers' network too. The map only knows the chunks that were measured, by an instrument in
  hand or a sensor on the network, and the age of each reading; what was never read stays grey.
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
- **fallout** is a kind of pollution (below) that raises the dose rate of the chunk it lies on;
- **the whole radiation system can be turned off** by a server setting: no dose, no contamination, no sources.

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

Cleaning equipment is each mod's (J's Industrial's scrubbers and filters), built on a common helper: a block or a
device takes up to its rate a second of one kind of pollution from the chunk it stands in, never more than lies there,
and keeps what it took for its mod to turn into a full filter or a byproduct.

### What is the series' own

The hazard, radiation and pollution kits are the platform's; **ionizing radiation, dissonance and the series' kinds of
pollution** are the series' own, in its package, built only from the kits ([The API](api.md)).
