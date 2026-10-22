# The world

What J's Core gives every mod about the world itself: terrain and biomes, ores and where they lie, reservoirs in the
rock, structures, dimensions with their rules, sky, day and weather, other laws of physics, worlds that change, data
kept over areas, and chunks kept loaded. Which ores, structures and worlds exist are each mod's; the kits they are
made with are the Core's.

## What exists today

Everything this page designs is built, as of October 2026, except what *Still to build* at the end lists: terrain and
biomes declared in code down to their noises, density functions and surface rules, landforms, rock in layers, seas of
any liquid, veins and prospecting, reservoirs, structures and settlements, finding things, map markers, the sky, the
day and the weather of each dimension, the physics kit, worlds that change, fields over chunks, chunks kept loaded with
a reason each, and the Space Persistor. How a mod uses it, with code: [The world](../WORLD.md).

## The design

The kits below gather in one place, for any mod, the kind of things TerraBlender, Tectonic and Terralith, Lithostitched
and the planets of Ad Astra and Galacticraft do, and more.

### Terrain and biomes

- **A biome kit**: biomes registered into any dimension, and into the Overworld by weighted regions beside other mods'
  biomes, as TerraBlender does, natively.
- **Terrain declared in code**: builders for noise, density functions and surface rules, which the data generation
  writes, so each world is one readable declaration.
- **A library of landforms**, as data, which any mod adds to: craters with their ejecta, rays and central peaks,
  scattered by size; dunes; canyons and rilles; lava tubes; ice sheets and glaciers; geysers and cryovolcanoes;
  sinkholes; salt flats; mesas; shield volcanoes and stratovolcanoes.
- **Seas, lakes and rivers of any liquid**: the methane of Titan, the liquid oxygen of Terminus, freezing or boiling
  away by the temperature of the place.
- **Rock strata**: host rocks lie in layers by depth and region, so veins cross real strata; J's Geology adds rocks.
- **Climate per biome, in real values**: temperature in °C and rain in mm a year, with seasons, for J's Agriculture and
  J's Oceanics.

### Veins

Ores don't lie scattered: they form **veins**, zones about 3 by 3 chunks wide, dense at the centre and thinning towards
the edges, so a player finds nothing, or hundreds. The vein system is a kit any mod's ores use.

- **A vein is a deposit type**, declared by a mod as data: the minerals it brings together and the "X ore" of their
  elements ([Materials](materials.md)), its height range, its frequency, its biome preference and its host rocks.
- **A vein crosses rocks**: it can lie in any of its host rocks and extend across more than one, and every ore block
  takes the variant of the rock it replaced.
- **An ore generates only when an installed mod declares it uses it.**
- With veins on, vanilla's iron, copper and gold only generate in veins: their scattered placement is removed by
  NeoForge's biome modifiers, as data.
- **Prospecting** asks the kit for the direction and strength of nearby veins, never their coordinates, and for a vein's
  composition and size; the tools that ask are a mod's (J's Industrial's prospecting pick and scanner).

**Settings**, in the Core's files: turn the vein system off (ores then generate as in vanilla, scattered, with rarity by
height); each deposit type's rarity, spread and density; per material, do not generate its ore; and **try brute-force
oregen**, which moves other mods' ores into veins by their common tags, works with mods that generate ores as data and
can fail with mods that generate them their own way, hence the "try".

### Reservoirs

Oil, natural gas and brines are **physical** things in the ground: they fill **reservoir rock**, porous blocks soaked in
them, under a **cap rock** that holds them in. Each block holds an amount; a well drains them one by one, and an empty
block turns back into plain rock. Nothing floods a mine, and digging into a reservoir shows it. Reservoirs are kept in
the data over areas, so every mod reads the same ones: J's Industrial's oil, J's Geology's natural gas, the lithium and
bromine brines of desert salt flats.

### Structures

A **structure generation kit**, built on vanilla's data-driven structures and their jigsaw pieces, going past them:

- **placement rules per world**: density, spacing, a minimum distance from the world's origin, terrain filters by slope,
  height and flatness, buried and half-buried placement;
- **fitting the terrain**: foundations that reach down to the ground, and carving where the ground is in the way;
- **large layouts** of many pieces, including **settlements that grow over time** up to a size set when they generate;
- **weathered variants made by rule** (rusted metal, dead panels, cracked glass) instead of drawn by hand;
- **loot that adds another mod's items only when that mod is installed**;
- **structures as discoverable targets**: instruments and maps find them, imprecisely at first;
- **state kept per structure**: chambers sealed behind a team's knowledge ([Progression](progression.md)), terminals
  woken by power;
- **no overlap** with landmarks or other mods' structures.

J's Space's ruins, Enigmatic sites and cities come first, then J's Overworld's, J's Geology's and the rest.

### Places and finding them

- **Landmarks**: unique, named features of a world (Olympus Mons), with a known place, which players discover.
- **Finding things**: the nearest structure, landmark, biome or vein of a kind, answered with **levels of imprecision**,
  so an instrument can point roughly at first and exactly later.
- **Markers on maps**: an API of markers and layers, points (structures found, machines, veins found) and areas
  (satellite coverage, pollution, radiation, claims), shown through guarded adapters in JourneyMap, Xaero's Minimap and
  World Map, and FTB Chunks; with no map mod, in the maps mods on the Core have, such as J's Space's Atlas; each player
  sees what they found, and a team what it shares.

### Dimension rules by altitude

A dimension's pressure, temperature and air may **change with height in bands**, not only be one value for the whole
dimension. J's Space's Venus is one dimension: 92 atmospheres and 464 °C at the ground, falling with height to about one
atmosphere and 30 °C high in the clouds, where floating bases sit.

### Sky, time and weather

- **Each dimension keeps its own day**: its length (21 hours on Terminus), and a world locked to its star keeps the sun
  still in its sky. The Overworld's day is never touched.
- **A sky kit**: several suns, each sized and coloured by its kind of star; planets and moons drawn in the sky with
  their phases and their surfaces turning, which a telescope can look at; rings; square stars, the band of the galaxy
  and nebulae; auroras; eclipses worked out from the real geometry; the colour of the sky and of the sunset from the
  air (the blue sunsets of Mars); clouds with their own height, colour and density; haze.
- **Sunlight by distance**: the farther the star, the dimmer the day; on Pluto the day is dark.
- **A weather kit**: kinds of weather per dimension (dust storms, acid rain, methane rain, snow of other ices, electric
  storms), with what they do to sight and to a body, registered by any mod.

### The physics kit

Areas or whole dimensions **with laws of their own**, for J's Industrial's Aleph first and J's Space's worlds next, as
visual effects and as mechanics:

- **gravity fields**: strength and direction, zero, or towards a point, applied to entities and falling things;
- **a rate of time**: entities and block entities, machines included, tick slower or faster;
- **light**: colour shifted as by the Doppler effect, bent as by gravity (lensing, mirages), darkness or glow with no
  source;
- **non-Euclidean space**: bigger inside than outside, regions that wrap around, shortcuts between far points, seamless
  portals;
- **space curved around masses**, such as a micro black hole;
- **rules of matter**: fluids flowing up, slow explosions.

Where the laws depart from the normal ones, **dissonance** builds up ([Hazards](hazards.md)).

### Worlds that change

- **Generating in old chunks**: when a mod or a setting brings new veins, ores or structures, they are generated in
  chunks that already exist, each chunk marked when done, away from the game's tick and within a budget.
- **Living changes**: terrain already generated changes slowly near players, by rule: water gathers in basins, ice
  melts, the surface turns into something else; an area's biome can be changed and sent to players. This is what J's
  Space's terraforming asks for.
- **Large edits** (the crater of an impact, the blast of a reactor) are planned away from the game's thread and placed
  within a budget, so the server never stalls.

### Travelling between dimensions

A player goes from one dimension to another **with what they ride**, a vehicle or a moving structure and everyone in
it, and arrives at a safe place. A mod gives the passage a **screen of its own** instead of the game's "loading
terrain" (the fire of a re-entry).

### World events

In the spirit of Lodestone's world events:

- **Events on a schedule or by chance**, declared by any mod: a meteor shower, a solar storm (J's Space), an earthquake
  (J's Geology), a heat wave.
- **A warning first**: whoever has the right instrument (an observatory, a seismograph, a satellite) is warned ahead,
  as J's Space's solar storms are.
- **Effects in step for everyone**, from the sky and the sound to the screen shaking, and what the event does to the
  world (craters, radiation, cracks), within a budget.
- **Only where they make sense**: a solar storm strikes whoever is unsheltered in the inner Solar System, an earthquake
  a region.
- **Commands for operators** to start, delay or cancel an event, and a consequence setting per event.
- **Read by the network**: J's Computers warns, and can react (putting machines in safety).

### Fields over chunks

Values kept per chunk that **spread and fade** (pollution, fallout, a magnetic field, heat) are one kit: worked out away
from the tick in batches, and sent to the players nearby for the overlays that show them. The hazards stand on it
([Hazards](hazards.md)).

### Keeping chunks loaded

- **Tickets with a reason**: every chunk kept loaded says which owner keeps it, and why.
- **A budget for the server** across every owner, besides each owner's own number.
- **A map** of the chunks kept loaded, for players and operators.

**The Space Persistor** is the series' chunk loader, a block of the Core's shared content, in a technological look (as
Chicken Chunks' loader). It serves any mod: a quarry far away, a station, a starship left in orbit.

- **The area is the player's**, set on the block: a shape (a square, a circle, a line, or free) with its radius, and on
  top of it chunks added or taken away with a click on the block's map. It reaches **as far as the player wants**: the
  radius has no ceiling of its own and the map zooms out; the only limits are the owner's number of chunks and the
  server's budget.
- **Energy**: whether it costs energy, and how much, is a setting of the Core's, which can also **turn its energy use
  off entirely**; then it has no port and no Protection tab.
- **Its screen** has no inventory, since it holds no items: the window is the face, with a display showing the land
  around from above in the game's map colours, chunk by chunk (the chunks it keeps outlined, another owner's hatched,
  the chunk under the pointer named in a tooltip), and beside it the counts (its own, the owner's against their
  number, the server's against its budget, the power), the shape keys, the radius, the ON/OFF switch and two lamps,
  keeping and full.

### Generation that never waits

Everything the Core generates is **safe across threads and always the same for the same seed**, so it works with mods
that generate on many threads or draw distant terrain, such as C2ME and Distant Horizons.

### The experimental-settings warning

A world whose extra dimensions are the series' own, declared ahead (as J's Space's Moon), should open without the
game's warning about experimental settings; this needs reaching into the game ([Low level](low-level.md)).

## Still to build

- **Changing other mods' world as data**: density functions, biomes, features and structures of the game and of other
  mods changed by a data pack, and more kinds of density function to build terrain with.
- **Other mods' structures tuned by an operator**: a settings file that turns off, spaces out or moves to other biomes
  the structures of any mod, and a dump of them all.
- **A field manual of the veins**, written in Markdown in the manuals' framework ([Manuals](manuals.md)).
- **Large edits kept and undone**: a large edit saved as a structure file, read back from one, and undone.
- **Chunks kept with their owner away**, by a rule of the server's.
- **The same world on every thread, proved**: a test that generates the same land with any number of threads.
- **Biomes added to the Overworld by region**, with their surface rules merged, belong to J's Overworld, a mod of its
  own on the Core.
