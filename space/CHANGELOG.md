# Changelog of J's Space

Every change to J's Space, newest first, taken from the series' changelog
([CHANGELOG.md](../CHANGELOG.md)), which records each change once with the mods it concerns and keeps
the releases before 0.5.0a. This file is written from it by `./gradlew changelogs`.

## [Unreleased]

### API
- The promise: J's Space's API is at version 1, in `dev.jstech.space.api`, experimental. For now it holds
  only its number, `JsSpaceApi.VERSION`; each registry an add-on adds to (worlds and their generators and modifiers,
  landmarks, rocket parts, starship sections, kinds of satellite and probe, what can be discovered of a world) joins
  it with the model it holds, raising that number. Its photo is `core/src/test/resources/api/space.txt`.
- The tabs of its advancements: J's Space's API is at version 2. `SpaceAdvancementTab` names the three
  tabs, `LEAVING_THE_EARTH`, `THE_MOON` and `MARS`, with each one's root, which an add-on hangs its own advancements
  under. Experimental.
- The world model: J's Space's API is at version 3. `SpaceWorld` is a world's data (its name, its system,
  its dimension, the generator that shaped its ground, the modifiers laid over it and its width), `SpaceWorlds` the
  worlds by id, declared in code or written by a data pack under `data/<namespace>/jsspace/worlds/`, and
  `WorldGenerators` and `WorldModifiers` the processes that shape them, each `IWorldGenerator` giving a world's ground
  as a density function and its surface as rules, each `IWorldModifier` laid over them. Experimental.
- What can be known of a world: J's Space's API is at version 4. `WorldProperties` registers the
  properties, each a `WorldProperty` with its name and whether a crew needs it known to land. Experimental.

### Added
- The mod: J's Space (`jsspace`) is a mod of the series, a jar of its own that requires J's Core alone, at
  the same version as every mod of the series. It adds nothing to the game yet.
- The settings of a world, `jsspace-server.toml`, none of which turns a part of the mod off: fourteen
  consequences, each on unless a server turns it off and saying what happens instead (launch damage, no launch in a
  thunderstorm, falling stages that leave craters, five failures of a rocket, cruise events, landing only once a
  world's essentials are known, dust on Mars, naming hidden worlds, debris, and the Laser Satellite as a weapon), and
  two rules of balance that start off, fast travel and J's Industrial's chains as the only path. They are on J's
  Core's settings screen, in English and Brazilian Portuguese; each is read by what it governs as that is built.
- Its mark and its tab: J's Space's logo, the word in the white of the Apollo rockets with a red livery band
  and a Saturn V beside it, and its square icon, in the mods list and the README; its creative tab, with the Saturn V
  on its button, shown once it holds a thing; and the night sky its advancement tabs will stand on, the Milky Way
  across a 400 by 200 sky drawn once and wide, with a 16-pixel tile of it.
- The Mission Handbook, the mod's own manual and the first thing in its tab, dressed as a crew's flight
  documents were: an off-white ring binder with the mission's emblem and its typed lines on the cover, cream pages
  typed in capitals, a tab at the edge for each section (Launch Operations, Flight, Suits and World Data) in its
  colour and lettered with its code, and the pages numbered by section (LO 2-3). It is written in Markdown, in English
  and Brazilian Portuguese, and its chapter is in the Technical Reference too. For now it holds the page that says
  how to read it; the other sections show as their pages are written.
- Its milestones: the firsts of spaceflight, in the order they happened, from the first suborbital flight,
  satellite, person in orbit, spacewalk and docking to the first starship, star and galaxy, nineteen in all. They
  belong to the team, any mod can read them, and each is knowledge a recipe can ask for; each is reached by what does
  it, as that is built.
- Its advancement tabs, on the night sky: Leaving the Earth, which opens with a player's first rocket; The
  Moon, as they first see the Earth rise over it; and Mars, as they first land there. Each shows its world's picture
  on its tab; their advancements follow with what earns them.
- The universe: twenty-six star systems in three galaxies, eighteen in the Milky Way, real stars at their real
  distances from the Sun, five in Andromeda and three in Triangulum, each with its name and what shines at its heart,
  in English and Brazilian Portuguese. They are written to `data/<namespace>/jsspace/star_systems/`, so a data pack
  changes one or adds its own. Every system has a space of its own, a dimension made the first time someone gets
  there and kept with the world: empty, vacuum and with no gravity, by its rules.
- The worlds of the skeleton: the Overworld, the Moon and Mars, each a world of the Solar System made of its
  generator, its modifiers and its data, at their real widths. The Moon pulls a sixth as hard as the Overworld, has no
  air and a day as long as a month, about ten real hours; Mars pulls two fifths as hard, has a thin air of carbon
  dioxide, a sol barely longer than the Overworld's day and a year of thirteen sols.
- The skies and the light of the worlds, as J's Core's sky files of their dimensions: the Moon's black, with
  no air for clouds and its stars bright; Mars' butterscotch, with blue sunsets, thin clouds, a haze of dust and a sun
  that looks smaller and gives less light from half again as far; and every system's space black and starry, its sun
  always up since nothing turns under it.
- Orbital mechanics, as real ones work at the game's scale: an astronomical unit is 10,000 blocks of a system's
  space and the Earth's year a week of Overworld days, so each planet keeps its real pace and the launch windows come
  round as they really do, Mars about every five real hours; a launch out of the window costs more delta-v, up to
  twice from the worst place; a trip is shortened by the delta-v spent on it; a rocket's delta-v is Tsiolkovsky's and
  it lifts off only when its thrust beats its weight on the world it stands on.
- What a team knows of each world: ten properties, from its orbit and position to its samples, each known by
  a share that only grows; a world's share known is their mean, and a world known whole reaches the milestone. A probe
  goes only where the orbit is known, and a crew lands only where what kills there (the atmosphere, the temperature,
  the radiation, the gravity and the relief) is known, unless the server lets crews land blind. Every team knows the
  Overworld whole and the places of the Moon and Mars from its sky.
- The blocks of a round world lie on its sphere: X is the longitude and Z the latitude, its spawn at the middle
  of its near side. Each world is the Overworld's border wide times its width over the Earth's (the Moon about 27%,
  Mars 53%), fixed as it first loads, and half as tall, its border at the middle of its far side and at its poles.
- Where a player who dies on another world comes back, in this order: their bed there, if it stands in a
  sealed room with air; else the cabin of the module they landed in, if it is still whole and has air, which their
  landing marks; else home, in the Overworld.
- The Solar System's space, drawn: the Overworld, the Moon and Mars are round bodies in their places, each
  kept still so whatever is built in its orbit stays over it, drawn at the screen's own resolution with a smooth
  outline and the texels of their maps, read from a smaller copy the farther they are. The Sun lights them smoothly
  from where it stands: relief from their height maps, a soft line of dawn, a warm dusk and a glow of air on the limb
  where there is air, and the night side in the deep tone of its night. A planet's poles lie level and square to the
  Sun, so its equator runs under its orbit with the day towards the Sun and the night away from it, and it turns once
  a day; the Moon keeps its near side to the Overworld a quarter round from the Sun, so it sees the Overworld half lit
  and the Overworld sees it gibbous.
  The Overworld is never the Earth: its continents, biomes, mountain ranges and polar caps come from the world's seed,
  of which the server sends only a number made from it, and the game's own clouds wrap round it in bands that drift at
  their own pace, with their shadows on the ground. The Sun is the game's square, as big as in the Overworld's sky,
  and the stars shine by day, crowding along the Milky Way.
- The skies seen from the ground: the Overworld stands still in the Moon's sky, high in the north over the
  middle of the Moon's map, lower the farther one goes, on the horizon a quarter of the way round and hidden on the
  far side, the same sphere as in space, its phases following the Moon's hour, its texels as wide as the game's
  moon's; the Sun's hour follows the longitude on the Moon and on Mars; the Moon's sky is black, its stars shining by
  day; in Mars' butterscotch sky Phobos rises in the west and crosses it twice a sol, Deimos is a slow point of light
  and the Overworld an evening star beside the Sun.
- The way up to space with no loading screen, for players and for rockets and ships alike: seven hundred
  blocks over a world's build limit it becomes its system's space. On the way up the loaded land goes on, out to the
  horizon, in a far map of the real ground as the world's own generator makes it, its mountains standing in relief
  block by block, painted in the blocks' own colours as each biome tints them, its coasts and snow lines where its
  heights put them; the game's clouds go on to the horizon, drifting as they do, and a haze like the game's fog
  thickens with the distance, so the loaded land melts into the far one with no seam. The sky turns navy and then
  black, the stars come out, and from halfway up the eye rises ever faster as the ground bends into the world's
  sphere; near the top the real ground melts under a veil of haze into the world's painted sphere, never seen
  through, and the line is crossed with the very picture the space shows. Each place of a world arrives at its own
  spot of the low orbit, laid out like a map of the world, so a station built there stands over its base; going down
  through the floor of the low orbits shows the climb's own pictures on the way and comes back down over the same
  place, high over the ground.
- The ground of the Moon and of Mars, in a row of its own in the mod's tab: the Moon's Highland and Mare
  Regolith, its Anorthosite and Mare Basalt, and the Icy Regolith of its craters of eternal shadow; Mars' Martian and
  Hematite Regolith, its dark Basalt Sand, the Water Ice of its polar caps and the Dry Ice over them. Martian Basalt
  comes in three blocks, faceted, in columns and pocked by gas bubbles, and Mudstone in three, the grey of Gale's
  lakebeds, the buff of Jezero's delta and dusted red, each with a top of its own. The regoliths and sands dig with a
  shovel and fall as sand does, the rocks and the Water Ice, frozen as hard as rock, drop themselves only under a
  pickaxe, and the Dry Ice and the Icy Regolith break with anything.
- The ores of the Moon and of Mars, twelve in the rocks of those worlds: the Moon's ilmenite, its titanium and
  its oxygen, in the Mare Basalt and the Mare Regolith, its armalcolite, tranquillityite and merrillite, the phosphate
  of KREEP's rare earths, in the Mare Basalt, and the native iron of meteorites in the Highland Regolith; Mars'
  hematite in the Martian Regolith and the Mudstone, its magnetite and olivine in the Martian Basalt, and its jarosite
  and gypsum, the proofs of its old water, in the Mudstone, the gypsum in white veins. Each breaks under a pickaxe into
  its raw mineral. Those six rocks are host rocks any mod can lay its own ores in.
- Loose stones, a decoration of a few small stones lying on the ground, never a whole block: Mare Basalt
  Stones of the Moon's maria, Anorthosite Stones of its highlands, and Martian Basalt Stones, the vesicular basalt
  strewn over Mars' plains. They lie in one of four arrangements, turned as chance has it, are walked through, break
  at once to be picked up, and go with the ground under them.

### Fixed
- The Overworld's clouds seen from space drift on smoothly as the world turns; every second they jumped back
  a little, as their cover was drawn anew, and the world turned a tick at a time.

## [0.5.2a] - 2026-10-14

Codename: Boron.

## [0.5.1a] - 2026-10-14

Codename: Boron.

## [0.5.0a] - 2026-10-13 - The Start Me Up Update

Codename: Boron.

### Added
- Design: The design of J's Space, in `space/docs/design`, before any of its code: one page per subject, from
  the universe, its twenty-six star systems and every world to minerals, travel, propellants, rockets, energy, life
  support, satellites, exploration, starships, ruins and Enigmatic sites, mining, cargo, terraforming and
  megastructures, and what it does with J's Computers; each page says how its system works, and the first page gives
  the order of building. J's Core's design follows it where they meet: its basic cable, pipes and battery, which any
  mod uses on its own, its containment of exotic matter, the dead stars that give exotic matter, and the space module
  of the closed exo-suits.
