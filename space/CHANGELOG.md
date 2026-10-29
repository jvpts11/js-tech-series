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
- Landmarks: J's Space's API is at version 5. `SpaceLandmark` is a world's landform of its own with its real
  name, its kind, its place by longitude and latitude and its radius in degrees, and `SpaceLandmarks` registers them,
  a data pack's files in `data/<namespace>/jsspace/landmarks/` winning over a declaration; a world's generator lays
  those declared on it. Experimental.
- Life support: J's Space's API is at version 6. `SpaceSuit` is a pressure suit, its four pieces, the air its
  tanks hold and the cold and heat it stands, registered in `SpaceSuits`, which says the suit a body wears whole;
  `SuitModule`, a suit's module, what it does (more air, radiation stopped, light), the piece it fits and how much,
  registered in `SuitModules`; and `WorldDangers` lists each world's `WorldDanger`s. Each takes a data pack's files,
  `data/<namespace>/jsspace/suits/`, `suit_modules/` and `dangers/`. Experimental.
- The Sun's glare: J's Space's API is at version 7. A suit module may cut the Sun's glare where no air
  softens it, `SuitModule.Kind.GLARE`, its amount the share it cuts. Experimental.
- Rocket parts: J's Space's API is at version 8. `RocketPart` is a part of a rocket, what kind it is (an
  engine, a tank, a separation ring, a capsule and the rest), its diameter of 3, 5, 7 or 9 blocks and the one on its
  top, its height and whether it is built in layers, its mass, the propellant it holds, its engine's thrust and
  specific impulses at sea level and in a vacuum, its seats and its tier; registered in `RocketParts`, which takes a
  data pack's files, `data/<namespace>/jsspace/rocket_parts/`. Experimental.
- Propellants: J's Space's API is at version 9. `Propellant` is what a rocket burns: the materials of J's
  Core's catalogue it is made of, each in the state it is carried in and its share of the mass, the specific impulse a
  good engine gets of it in a vacuum, and whether its halves light on touching; registered in `Propellants`, which
  takes a data pack's files, `data/<namespace>/jsspace/propellants/`. Experimental.

### Added
- The mod: J's Space (`jsspace`) is a mod of the series, a jar of its own that requires J's Core alone, at
  the same version as every mod of the series. It adds nothing to the game yet.
- The settings of a world, `jsspace-server.toml`, none of which turns a part of the mod off: fifteen
  consequences, each on unless a server turns it off and saying what happens instead (launch damage, no launch in a
  thunderstorm, falling stages that leave craters, five failures of a rocket, cruise events, landing only once a
  world's essentials are known, dust on Mars, naming hidden worlds, flames that need air, debris, and the Laser
  Satellite as a weapon), and
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
- The ices of Mars off the Earth: Water Ice, warmed, melts into water only in a sealed room full of air, and
  anywhere else, in a vacuum or in Mars' thin air, turns straight to vapour and is gone; Dry Ice, warmed, turns to
  gas wherever it lies, even in a sealed room, giving the room its carbon dioxide share by share, a cubic metre's
  worth, some 35,000 moles, until it is gone; the Icy Regolith never thaws by itself. Ice is warmed by air above
  freezing round it, a sealed room's, or by a bright light beside it.
- The Moon's ground, as its sphere shows it from orbit: the maria low and dark where its map has them, mare
  regolith over mare basalt, and the bright highlands of highland regolith over anorthosite round them; Tycho a great
  crater at its place, its bowl, its rim and its central peak, in its young ejecta; the craters of eternal shadow
  round its poles, their floors of icy regolith. Six biomes of its own: Mare, Highlands, Polar Craters, Rille, Young
  Crater and Lava Tube Skylight, the rilles winding through the maria and the lava tubes running under them, craters
  by the thousand, fresh ones bright, loose stones of basalt and of anorthosite strewn over the ground, and veins of
  ilmenite with armalcolite and tranquillityite, of KREEP's merrillite and of the native iron of meteorites. Tycho is
  the first landmark of the worlds. The far map seen climbing from the Moon is painted in its regoliths.
- Mars' ground, as its sphere shows it from orbit: the low smooth north and the old cratered south either side
  of the dichotomy, Tharsis' volcanoes standing and Valles Marineris cut where its height map has them, under a dusty
  sky. Seven biomes of its own: Northern Plains and Southern Highlands of red regolith over basalt, Oxide Plains of
  hematite regolith, Dark Dunes of basalt sand walking in crescents where its map is dark, Chaos Terrain broken into
  flat-topped blocks of mudstone where its canyons open, Lakebeds of mudstone in old craters, and Polar Caps of water
  ice under carbon dioxide frost, cut by spiral troughs. Its basalt lies in columns on the scarps and walls, pocked
  on the tops of flows and faceted below; its mudstone dusted red in the upper layers of every exposure, buff on the
  scarps and grey below. Vesicular basalt's stones lie loose on its plains, and veins of hematite, of magnetite and
  olivine, and of gypsum and jarosite lie in their rocks.
- Life support: a pressure suit worn whole lets its wearer breathe where there is no air for as long as its
  tanks hold, each second of breathing spent from them, the only clock of a walk outside, and stand any cold or heat
  within its range; empty, its wearer suffocates. Three suit modules, fitted by right-clicking them onto the piece
  they go on: Extra Oxygen, half as much air again; the Radiation Vest, which stops half the sky's radiation; and Suit
  Lamps, lighting the way. Every world says its dangers, and the sky's radiation reaches the open ground of the Moon
  at 1.4 millisieverts a day of the game and of Mars at 0.7, a dose that builds up slowly in whoever stays long.
- Sealed rooms on other worlds: on the Moon, on Mars and in space the game's glass and doors, its iron door
  too, do not stand a room's air held against the thin air or the vacuum outside, and burst as the room is filled;
  whole blocks of stone and rock hold it. Torches, lanterns, candles and campfires go out where the air holds too
  little oxygen to burn, in a vacuum, in Mars' air, in a room whose air runs short: a candle or a campfire is left
  unlit, and a torch or a lantern falls as an item with a puff of smoke. A new consequence in `jsspace-server.toml`,
  Flames need air, lets them burn anywhere.
- The lunar suit, after Apollo's A7L: the Lunar Helmet, hard and white with its smoked visor in its frame, its
  hinges and its red stripe; the Lunar Suit with its neck ring, bearings, convolutes, gloves, the chest module with its
  connectors and hoses, and the life-support pack on its back with its hatch, vent, lamps, purge unit, antenna and
  bottles; the Lunar Trousers and the Lunar Boots. Worn whole it is a pressure suit with half an hour of air in its
  tanks that stands the cold of the lunar night and the heat of its noon, and armour as leather is. The Gold Visor, a
  module for the helmet, is lowered gold over its visor. On the Moon and in space, where no air softens it, looking
  near the Sun whites out the view, the nearer the more; the gold visor cuts it, and the view through it is a little
  gold. It harms no one.
- A suit's air is breathed for real: worn whole where the air round cannot be breathed, the suit gives its
  wearer the pure oxygen of its tanks at the third of a bar the A7L held, and takes nothing from the room; where the air
  can be breathed, the wearer breathes it and the tanks are spared, so the helmet comes off in a room with air. A hole
  leaks the tanks four times faster and thins the suit, two leave too little oxygen to breathe, and the Suit Patch
  mends one. The Oxygen Bottle holds half an hour of breathing and pours it into the suit's tanks, from the hand or
  clicked onto the suit; it fills from a sealed room's air, taking the oxygen out of it, or from any gas tank or line
  of oxygen.
- The suit's controls, its display and control module, on the HUD while a suit's chest piece is worn, drawn
  as the hazard devices are and stacked with the dosimeter: the suit's white case with its red and blue hose stubs,
  the black face with the suit's pressure on a needle from 0 to 5 pounds an inch, the time of air left, the state
  (sealed, oxygen low, holed and leaking, or open), the oxygen and carbon dioxide on bars, the O2, PSI, CO2 and FAN
  lamps, and the purge, fan and light buttons. With little oxygen left the screen's rim turns amber, holed it turns
  red. O, until the player changes it, holds the controls up close, twice the size, where the light's button lights
  the face.
- Rockets of parts and stages: a rocket is a moving structure whose blocks are its parts stacked from the
  bottom up, and its stages are split at its separation rings, each carrying what is above it; boosters strapped on
  burn beside the core and drop first. Its delta-v is summed from its parts by Tsiolkovsky's equation and its
  thrust-to-weight from its engines' thrust at sea level, so a rocket that lifts off the Moon may not lift off the
  Overworld. A tank is built in layers, as tall as it is wanted. The parts come in diameters of 3 and 5 blocks, all
  burning methalox: the sea-level and vacuum engines, the tank, the separation and instrument rings, the capsule, the
  landing legs and the two stages of the lunar lander; a part fits only on one of its own diameter. A rocket belongs
  to whoever built it and is flown by their team.
- Propellants and where spent stages go: the skeleton's rockets burn methalox, liquid methane and liquid
  oxygen in Raptor's mixture, and a rocket's delta-v comes from what its tanks really hold; an engine lights only on
  its own propellant, and what it does not burn is carried as dead weight. An engine's thrust and impulse fall from
  their vacuum values with the air's pressure, so on Mars it is nearly as good as in a vacuum. Liquids kept cold boil
  off a rocket's tanks as it stands, liquid hydrogen fastest, oxygen next, methane least, and a liquid that keeps at
  the place's warmth not at all. A stage that comes loose lands on the landing platform set for it when it has legs
  and an engine and the platform is free and wide enough, falls into the fall zone set for it, or falls anywhere in
  the world already loaded. The Landing Platform is a slab of Landing Pads three times as wide as the stages it takes,
  nine blocks for those of three and fifteen for those of five, with its terminal in the middle and its target painted
  across it; built on a structure afloat it is a floating platform, and it holds one stage at a time. A stage that
  strikes the ground leaves a crater and Rocket Wreckage that gives back a third of its blocks; with the server's
  Falling stages do damage off, only the wreckage is left.

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
