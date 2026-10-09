# Implementation

The settings, the files and the numbers of J's Space. Every number here is an estimate, set by playing.

## What exists today

Nothing of J's Space exists yet. J's Core's settings screen shows every setting of every mod of the series, with its
range, its default, a comment and its unit.

## To build

### Settings

J's Space has no switches that turn parts of the mod off. Its settings are **consequences** and **rules of balance**, in
`jsspace-server.toml`:

| Setting | Default | What it does |
| --- | --- | --- |
| Falling stages do damage | on | a stage with no fall zone and no platform leaves a crater; off, it only lies there as wreckage |
| Launch damage | on | a pad with no flame trench and no deluge is damaged at launch |
| No launch in a thunderstorm | on | the panel holds the countdown in a storm |
| Rocket failures | on, one per kind | a rocket that can't lift, burns up, crashes, wears out unrepaired or explodes |
| Cruise events | on | solar storms, micrometeoroids and failures on the way |
| Fast travel | **off** | skips the cruise of rockets and starships |
| Landing needs the essentials | on | a crew can't land until a world's atmosphere, temperature, radiation, gravity and relief are known |
| Dust on Mars | on | dust on solar panels and global dust storms |
| Debris | on | destroyed satellites leave debris in orbit, which strikes and multiplies |
| Laser Satellite as a weapon | on | off for servers without PvP |
| Naming hidden worlds | on | the first team to find a hidden world names it; off, the default names stand |
| Industrial chain only | **off** | with J's Industrial installed, its chains become the only path for what both mods make |

The settings of J's Core's kits live in J's Core's files: sealed rooms (the pull of decompression, fire in pure oxygen),
radiation, the knowledge gate, the generation of structures.

### Numbers

**Delta-v**, the real one, from the Earth's surface (estimates; the game uses them as they are):

| Leg | Delta-v |
| --- | ---: |
| The surface to low orbit | about 9.4 km/s |
| Low orbit to the Moon's orbit | about 4.1 km/s |
| The Moon's orbit to its surface | about 1.7 km/s |
| Low orbit to a transfer to Mars | about 3.6 km/s |
| Mars' surface to its orbit | about 4.1 km/s |
| Low orbit to a transfer to Jupiter | about 6.3 km/s |

Aerobraking saves much of the delta-v of arriving where there is air.

**Other numbers**, estimates:

| What | Value |
| --- | --- |
| A year of the Earth | 7 Overworld days |
| An astronomical unit in a system's space | about 10,000 blocks |
| An orbit in low orbit | a little over a minute |
| The climb to space | depends on the rocket; about a minute for an ordinary one |
| Small starship reach | about 10 light-years |
| Structures | nothing within about 1,000 blocks of a world's origin |
| Cities | 5 to 40 buildings |
| Terraforming | 3 to 10 Overworld days a stage |
| Specimens that give science | the first few of each species |

The numbers of every part (the thrust, mass and Isp of each engine, the volume of each tank, the reach of each antenna,
the output of each panel and generator) are set with the first build and calibrated by playing.

### Files

- Worlds, generators' parameters, landmarks and terraforming processes are **data**, in datapacks ([The API](api.md)).
- Dimension rules are J's Core's datapack files, one per world, with their bands by altitude.
- Discovery, comprehension and milestones are kept with the world, per team, in J's Core's progression.
