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
- Rocket kits: J's Space's API is at version 10. `RocketKit` is the kit of blocks a rocket part is built of
  and where each goes, its layers from the bottom drawn in letters with the block each letter stands for, the layer a
  part built in layers repeats, the one layer it is when built a single layer tall, and the foot it stands on over an
  engine; registered in `RocketKits` by the part's id, which takes a data pack's files,
  `data/<namespace>/jsspace/rocket_kits/`. Experimental.
- Starship parts: J's Space's API is at version 12. `ShipPart` is a part or a module of a starship: what kind
  it is (`ShipPartKind`), the class of ship it is for (`ShipClass`, which gives the room its hull has for modules),
  where it goes (`ShipPlace`: the nose, along the spine, the tail only, behind the spine's end, at the wings' root, on
  wings that carry nacelles, along the sides, round the ship or under it), its length along the spine, the room a
  module takes, and whether it holds an airlock or, for wings, carries nacelles; registered in `ShipParts` by id, which
  takes a data pack's files, `data/<namespace>/jsspace/ship_parts/`. Experimental.
- Starship kits: J's Space's API is at version 13. `ShipKit` is the kit of blocks a starship part is built of
  and where each goes on the ship: its pieces, each set from a place on the ship (`ShipKit.Anchor`: the part's own
  section, the joint behind the cockpit, the wings' root or the spine's end), its layers drawn in letters with the
  block each letter stands for, and mirrored to the ship's other side when it is a pair; registered in `ShipKits` by
  the part's id, which takes a data pack's files, `data/<namespace>/jsspace/ship_kits/`. Experimental.
- Starship finishes: J's Space's API is at version 14. `ShipFinish` is a finish a starship is painted in,
  chosen apart from its parts, a part's model in it painted by the finish's own texture of that model; registered in
  `ShipFinishes` by id, which takes a data pack's files, `data/<namespace>/jsspace/ship_finishes/`. Experimental.
- Starship makers: J's Space's API is at version 15. `ShipMaker` is a maker of starship parts, which gives
  each of its parts its shape and, to one a player goes into, its interior; registered in `ShipMakers` by id, which
  takes a data pack's files, `data/<namespace>/jsspace/ship_makers/`. `ShipPart` gains its `maker`, by id, written in
  a part's file as `maker`. Experimental.
- Starship parts' figures: J's Space's API is at version 16. `ShipPart` gains its `figures`, a
  `ShipPart.Figures` of its mass, the thrust of its main engines, the lift of its air engines, the power it makes or
  takes and the crew it seats and beds, written in a part's file as `figures`; `ShipMaker` gains its `mark`, the key of
  three letters a builder shows of it. Experimental.

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
  red, deepest at the very edge. O, until the player changes it, holds the controls up close, twice the size, over the
  world dimmed as behind an open container and with the HUD and the hand put away until they are put down, where the
  light's button lights the face. The pressure's figure is written the way the game's language writes numbers.
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
- The rocket parts' block kits: every part is built of blocks, a kind for each real thing it has, placed where
  it goes, 44 kinds in all, each showing one detailed face on all six sides so it is known from any side while the
  part is built. The instrument ring of the wall, avionics and battery panels, access door, umbilical plate, telemetry
  antenna, cooling louvres and the guidance platform in its middle; the separation ring of its wall, retro rockets and
  ullage motors; the engine of its mount, combustion chamber, turbopump, gimbal actuators and the nozzle's walls, a
  vacuum engine's extension under them; the tank of its walls, domes, fill port, vent valve and cable raceway, as tall
  as it is built, standing on its skirt over an engine; the legs; the lunar lander's foil, engines, legs, ladder and
  cabin; and the capsule of three, 27 blocks, and of five for three crew, 49, of its heat shield, hull, windows, hatch,
  crew seats, flight computer, thruster quads, parachute bays and docking port. The crews' pressure shell holds an
  atmosphere against the vacuum, its windows and hatch too, and every block weighs its share of its parts.
- The rocket parts' formed models: a formed part is drawn round, as the real one is, each of its blocks a
  panel or piece of its own, hollow where the real part is, and no block of a kit without its piece. A tank is drawn a
  layer at a time, standing on its skirt over an engine; a separation ring raises its black interstage skirt round a
  vacuum engine standing on it; the instrument ring of five closes its adapter's four panels round a lunar lander; the
  legs, and the lunar lander's, are folded on the way up and spread to land. A capsule is drawn with its crew's cabin
  inside, the couches, the panel, the hatch in its recess and the windows at its corners.
- Live cabins: the capsules' panels and the lunar lander's cabin show the flight as it goes. The strips show
  the stage's fuel and oxidiser and how fast the rocket climbs, the attitude ball turns with its pitch, roll and
  heading, the throttle slides to its setting, the heading's tape and the mission clock in seven segments run, the
  gauges show the cabin's air and the propellant left, and the lamps light green, amber or red: the engine burning, the
  stage ready to part, the Flight Computer flying, an abort, the master alarm, the cabin's air, propellant running low,
  the parachutes, the legs and the docking port. The Flight Computer's face shows on the panel, and on the lander's
  front wall, its fail lamp lit with the master alarm. A capsule's crew lie on their couches facing its nose, looking
  up at the panel; in the lander's cabin the crew seat is a chair facing the Flight Computer.
- The Flight Computer: used, it opens its screen, the approved flat display, its keys pressed by clicking them
  or their legends. Its transfer page draws the system round the Sun with the Overworld and Mars where they stand now,
  going round as the game's time goes, the transfer's arc from where the world left will be at the next window, and
  the plan's figures: the phase now and at the window, the delta-v now and in the window, the time of flight and
  whether the rocket brakes in the air or with its engines; under it the windows' tape with now, the next window and
  how long until it opens. To the Moon there is no window. Its landing page draws the map of the world it goes to as
  the game's map item draws a world, from that world's own generator: zoom in and out, mark the spot to land on by
  clicking the map, clear it, lay the grid; the ellipse the rocket comes down in is drawn round the mark, with north,
  the scale and where the spot is.
- The Rocket Hangar, where a rocket is stacked standing: walls and roof of any solid block a player likes,
  its Hangar Terminal set in a wall facing into the bay. From the terminal the hangar measures its bay and forms once
  a line of Crane Rail runs along two facing walls a block under the roof, a Crane Bridge lies on them from wall to
  wall, a Crane Hoist hangs under the bridge and a Bay Door stands in a wall on the floor; its walls, floor and roof
  must be closed, and its bay no smaller and no larger than the server allows (new settings: the narrowest, widest,
  lowest and tallest bay). Used, the terminal opens the hangar's design screen once it is formed, or says what keeps
  it from forming. Formed, the crane and the work platforms are drawn whole as the approved hangar has them: each
  rail a yellow box girder with its stiffeners and stencil, its steel rail on top and a bracket under it every eight
  blocks; the bridge's taller girder on the rails, two blocks wide; the hoist's trolley of riveted steel plates with
  its drum; each deck of work platforms a grated plate over an underframe of I-beams, hazard stripes round its edge, a
  hazard band, the yellow posts and rail along the edge it faces, and two struts down to the side away from it, the
  deck at its blocks' tops. Its kit: the Hangar Terminal, the Assembly Console, the Crane Rail, the Crane Bridge, the
  Crane Hoist, the Bay Door and the Work Platform.
- The Rocket Design, the Rocket Hangar's design screen, opened at its terminal or at an Assembly Console in
  its bay: the hangar itself seen in 3D through its middle, the rocket standing on the middle of the bay's floor with
  hazard stripes painted round it, and the hangar's own steel plates round it. A part is dragged from its bin, each
  bin showing the part's own model and its label card, to a green node of the rocket, where it snaps, as in Kerbal
  Space Program's editor; a part of the rocket is dragged off with those on its far side from the root, and dropped on
  another node or back on the bins to throw it away. The tools turn a part a quarter turn (R turns one being placed),
  build a tank taller or shorter, and choose the root; the mirror hangs one to four boosters round a part at once, and
  the snap sets how near a node a part must be let go. The right mouse button turns the view round the rocket, the
  wheel brings it nearer, the middle button raises and lowers it, never through the hangar's walls; Ctrl+Z and Ctrl+Y
  undo and redo. The bins sort the parts into six tabs, command, tanks, engines, rings, landers and recovery, each
  diameter shown or hidden, and the chosen part's spec sheet gives its figures. The right plate gives each stage's
  delta-v and its thrust against its weight on the world it leaves, the rocket's delta-v on a display, whether it
  lifts off, each destination with its globe, what it asks (from the Overworld: a low orbit 9,400 m/s, the Moon
  15,200, Mars 13,000) and a bar of how far the rocket gets, short in red; and its checks: a stage with propellant and
  no engine or an engine with nothing to burn, too weak to lift off, taller than the bay's crane or wider than the
  bay, the crew's seats, the heat shield to come home through air, the tanks filled at the pad; its sign lit READY or
  NOT READY. Every change is kept by the hangar; the design is named, saved as one of the hangar's sixteen plans,
  loaded from them, and sent to the bay to be built once it is ready. Each hangar takes a number of its own in the
  world the first time it forms, and its screen's sign reads it: Rocket Hangar 1, Rocket Hangar 2.
- The rocket is built by its hangar's plan: once a design is sent to the bay, the hangar lays it out on the
  bay's floor, every block of every part's kit where it goes, and counts each part's blocks as they go down. A part
  forms by itself as its last block goes down, its blocks merging into its own model, round and shaped as it is drawn,
  with no terminal of its own, and comes apart, every block back, as soon as one goes; the other parts stay formed. A
  separation ring's skirt shapes the blocks of the engine standing on it, the instrument ring's adapter those of the
  lunar lander, the folded landing legs those of the tank over them. The hangar draws the formed parts, a capsule's
  cabin at rest; the Flight Computer stays a block of its own and is drawn by its capsule's panel once the capsule
  forms. In a formed capsule the crew seat is a couch. A hangar broken, or a new design sent, gives the rocket's blocks
  back.
- The hologram of the rocket being built: once a design is sent to the bay, every block still to place stands
  where it goes as a see-through ghost of itself, each part's ghosts one pale shell a little of its colour, each
  block's edges in that colour and fine lines across them as a screen has, the layer to build next glowing amber and a
  wrong block outlined in red; each part, a multiblock, has its box drawn round it in the colour of its kind, with its
  name large in that colour over its size and how far it stands (to build, the layer it is at, or formed) on a dark
  plate beside it, a leader to it from the box; and a line across the top of the screen
  names the rocket, the part and layer being built and the block that layer needs most, as "Orbital I - Tank, layer 7
  of 9 - 9 x Tank Wall". A part that forms draws its own model and leaves the hologram, which goes once the rocket is
  whole.
- The Rocket Hangar's crane builds the rocket by itself: once a design is sent to the bay, whatever the hangar
  waits for that comes in through an Item Port in its shell (from a chest, a hopper, a pipe, a conveyor or another
  mod's exporter) the crane lays where it goes, block by block at the pace of the multiblock builder, the lowest first,
  layer by layer; the port takes only the blocks the rocket still wants and no more of each, and gives back out what
  it no longer wants, as when a player lays a block by hand. A part forms as its last block goes down. The Hangar
  Terminal takes a data cable on any face: on a network, `RUN jscore:assemble` sets the crane building the plan sent
  to the bay, or one of the hangar's plans by name (`WITH plan = 'Orbital I'`), and the request stays in flight until
  the rocket is whole. Broken, the terminal drops the blocks its crane held. New block: the Item Port.
- The mobile launcher a rocket stands on in its hangar, Apollo's way: where the bay has room for one, the plan
  sent to it lays the launcher under the rocket, sized by it, and the crane builds them together. Its deck, three blocks
  thick, stands on six Pad Pedestals three blocks tall, in two rows along its length so the crawler can come in under
  it; its length points at the bay's door. The exhaust opening through it is as wide as the rocket's foot, lined with
  Exhaust Frames, a Hold-Down Arm at each corner and three Tail Service Masts round it a third of a turn apart; at the
  deck's far end from the door rises the red umbilical tower, a block past the rocket's top, the crane's house on its
  top and the boom on the house, from just past the rocket's far side to a block past the tower's back, the hook
  hanging from it; a Swing Arm reaches out to each tank and each Umbilical Plate, the Crew Access Arm to the hatch with
  the white room at its end, and the Tower Elevator's rails, climbed like a ladder, run up the shaft from the doorway at
  the deck to the doorway onto that arm. Each part forms as its last block goes down, and comes apart as one goes. The
  deck is then drawn whole as the approved launcher is: its top and each side one picture over the whole of it (dark
  plates two blocks square with their seams and bolts, the yellow walkway line round the edge, the hazard band round the
  opening and the cleats at its corners; steel panels under a dark trim band, a stiffener every two blocks, access
  doors, vents, stencils and lamps between them), the scorched lining, the trim lip, the grating at the opening's foot,
  the clamps' housings with their jaws and chrome cylinders, the masts with their hoods and their blue, orange and white
  lines; so is the crane, its red house with its louvres, the boom's girder, the cable and the yellow hook, the aviation
  lamps on the house's corners. The tower is one great truss round its dark shaft, a banded, riveted column at each
  corner, deep girders across each face every two blocks and a dark red diagonal across it each level, leaning the
  other way the next, the grated floor every eight blocks; the shaft's wall shows its two rails, the cable tray with
  its blue, orange and white lines down the middle and a lamp every four blocks. Each arm is drawn whole from its end at
  the rocket: a swing arm's grey beam with its stiffeners and the hazard band by the tower, the hinge's column, its blue
  and orange lines along its top (and the white one to an Umbilical Plate) and the round plug with its yellow face; the
  crew access arm's grated walkway, its yellow rails and posts, and the white room at its end. The pedestals take their
  own shapes. The design screen
  draws the rocket on the launcher's deck, the launcher under it, and says whether the bay has room for one; the
  hologram names the launcher's parts. The deck is always wide enough for the crawler to come between its pedestals.
  New blocks: the Launcher Deck, the Exhaust Frame, the Hold-Down Arm, the Tail Service Mast, the Tower Frame, the
  Swing Arm, the Crew Access Arm, the Tower Elevator and the Pad Pedestal.
- The crawler-transporter that carries a mobile launcher, its tower and its rocket out of the hangar. Built
  of its kit round its Crawler Engine, as the approved crawler is (the hull fifteen blocks long and eleven wide, a
  truck four blocks long under each corner, its tracks on the ground and its leveling jacks over them, a cab two
  blocks square at two opposite corners), it forms and is drawn whole: each truck's two tracks a loop of separate
  shoes round the sprocket and the idler, the road wheels and return rollers, the frame with its guide tube and four
  jacks; the grey hull with its panels, louvres, doors, stencils, lamps and hazard band; the deck with its yellow line,
  the four jack pins in their hazard squares and the railing round it; the cabs with their glass and beacons, the
  exhaust stacks either side and a ladder. Its crawlerway is a lane of gravel under each side's trucks. A cab used, it
  goes along the crawlerway to its farther end: with a launcher centred over it, it lifts the launcher off its
  pedestals, carries it slowly to the end, sets it down there on the grid and comes back by itself; empty, it just goes
  to the other end. The bay doors in its way open while it passes and close once it is back; it says where the way is
  blocked, and will not take an unfinished rocket. Leaving the hangar, the rocket's plan goes to its flight computer,
  which draws its formed parts from then on. New blocks: the Crawler Track, the Crawler Deck, the Crawler Cab, the
  Leveling Jack and the Crawler Engine.
- The launch pad the crawler sets the launcher down on, with the real pads' rules. Its flame trench runs
  under the launcher's exhaust opening: a row of Flame Deflectors below the ground, a ridge that splits the flame both
  ways along the trench, spanning it from one wall of Trench Lining to the other. Its Pad Service Port takes water by
  pipe and J's Energy by cable, and holding a whole deluge (64 buckets, and 400 kJ for its pumps) pours it over the pad
  at ignition. A pad with neither trench nor deluge is blown apart at ignition, as Starship's first flight tore up its
  pad, unless the launch damage setting is off; and in a thunderstorm the launch panel holds the countdown. Pad
  Pedestals take their shape by themselves when three stand in a column, as on a pad built by hand. New blocks: the
  Flame Deflector, the Trench Lining and the Pad Service Port.
- The propellant depot that feeds the pad. A sphere built as a cube of Sphere Shell of odd edge from three to
  nine blocks, on a column of Tank Leg under each bottom corner, a Vent Stack on its top and a Transfer Pump on the
  ground against a leg, forms and is drawn whole as the approved one: the welded sphere in rings of white plates, eight
  legs from its equator and their braces, the vent stack. It holds as much liquid oxygen or liquid methane as a ball
  that wide, by pipe through its pump, and lets it boil away when it falls apart. The pad's service port now also takes
  liquid oxygen and liquid methane by pipe and fuels the rocket standing on the launcher over it, each tank to its
  propellant's mix, its pumps spending energy for every kilogram; the rocket's flight computer keeps what its tanks hold
  until it flies. A depot of liquid oxygen fills an Oxygen Bottle held to its pump. New blocks: the Sphere Shell, the
  Tank Leg, the Vent Stack and the Transfer Pump.
- The Launch Panel by the pad, its console the approved LCC-1: the count in seven segments with the mission's
  clock and the holds, the sequence on its screen, the HOLD and ABORT lamps, the go and no-go board (vehicle,
  propellant, tower arms, deluge, weather, crew, range, clamps), the tanks' bars, the DELUGE and CLAMPS toggles, the
  IGNITION toggle under its red guard, the brass MASTER key, the LAUNCH button in its striped well under its cover,
  and the HOLD COUNT, SCRUB, RECYCLE T-10 and COUNT RUNNING buttons. It finds the launcher on its pad and its rocket;
  with its key and its ignition armed and all go, COUNT RUNNING starts the count at T-60: the tower's arms swing back
  from T-42, the cover lifts at T-10, and with LAUNCH pressed the engines light at T-6, the deluge poured by the pad's
  rules, and the rocket is released at T-0; without it the count holds at T-6. It holds by itself on a no-go or in a
  thunderstorm. The count runs on the rocket's flight computer too. New block: the Launch Panel.
- The launch. On the pad, the loaded tanks' cold propellant boils off their sides as white vapour. At
  ignition the screen shakes for everyone near, the pad steams and smoke rolls out of the trench. At T-0 the rocket
  becomes one moving structure by the exact places of its plan, each block as it is, its parts still formed and nothing
  checked again; its flight computer goes with it, keeping its plan and the propellant in its tanks. The swing arms'
  hoses drop away and hang from the arms. It then climbs on its engines' thrust, by the air of the world, its mass
  falling as they burn by the rocket equation, each burnt-out stage handing the climb to the next; a rocket with
  thrust to spare climbs fast, one too heavy for its engines sits on the pad burning. The climb is seen by every player
  within sight of the pad, through the clouds and as far as the game draws, its plume following it, and it passes into
  space whole. The capsule's live panels read the flight: the mission's clock since lift-off, the burning stage's
  propellant and the whole rocket's, the climb, the throttle, the attitude, the cabin's air and the lamps. The parts a
  flight computer draws are now lit as their capsule is, where before they were drawn dark.
- Stages. As a rocket climbs, each stage whose engines have burnt out below the one burning is let go: its
  blocks and its separation ring part off with a jolt and a puff round the ring, the ring's retro rockets pushing it
  away, and it goes on as a body of its own, its formed parts still drawn whole; the rocket climbs on as it was. The
  Flight Computer's STG page, as the approved one: the stack drawn with its stages bracketed, the chosen stage's
  figures (its dry mass, legs, engine, whether it can come back, where it goes) and where every stage goes along the
  bottom. Its left keys choose a stage and its right keys set where it goes before launch: anywhere, a fall zone round
  the pad with its radius, or a landing platform of the world, the nearest first, for a stage with legs and an engine.
  A stage set to a platform flies back over it and comes straight down on its engine, landing on its middle; one
  falling near where it came loose falls by its physics and strikes the ground where it meets it, its blast and its
  wreckage; one sent to its fall zone or anywhere round a player falls out of sight and strikes its column there. A
  stage's wreckage now holds the blocks its formed parts were made of.
- In orbit. Seen from a world's orbit, the world slides by below, an orbit a little over a minute at the
  Overworld's pace, sixteen sunrises to its day, its clouds going round with it; coming down, it winds back to the
  place below. In the system's space there is no air and no gravity: no sound carries, and a loose thing floats where it
  is let go. The world keeps everything in its worlds' orbits: the rockets that pass into one are told there as they
  arrive, whose they are, kept loaded when a Space Persistor stands aboard, and forgotten as they go back down; a
  satellite or a probe let go by its owner works no more and any team may take it; a destroyed one becomes debris in
  its orbit, in pieces, unless the server turns debris off. The kinds of satellite (communication, relay, navigation,
  mapping, weather, observation, the space telescope, solar power, antiproton and debris collectors, the laser and the
  planetary climate satellite) and of probe (flyby, orbiter, lander, rover, drone, sample return, aeroprobe, mining
  probe, impactor, solar, interstellar and solar gravitational lens probes), and how a satellite or a probe is built:
  its size, a CubeSat, medium or large; its bus, its power, solar panels or an RTG, its battery, its antenna, its
  propulsion, cold gas, hydrazine or ion, its onboard computer, and the mission module that sets its kind.
- Planning a transfer on the Flight Computer's XFER page: its keys now choose the window to leave in (now, the
  next or one up to four ahead, the countdown to it written over the windows' tape and the chosen window lit on it),
  the delta-v to spend (more than the least, up to twice, shortens the cruise), a gravity assist (it spends less and
  takes longer) and aerobraking where the destination has air and the rocket a heat shield (it saves the capture
  burn); EXEC sets the plan for the rocket to fly, and choosing another destination lets it go. The page now tells
  whether the rocket carries a heat shield and what delta-v it has for the transfer, less the climb to orbit while it
  stands on the ground, and says when that is not enough. With the server's fast travel on, the plan set skips its
  cruise.
- API 11: the kinds of satellite and of probe, by id, declared in code or by a data pack's files
  (`SatelliteTypes`, `ProbeTypes`), each with its orbits or whether it lands, and the orbits and sizes they come in
  (`OrbitKind`, `SpacecraftSize`).
- Damage in flight: a rocket's formed parts hold when a block of theirs is lost, in flight and in the hangar
  alike, the hole torn round it, and its flight computer counts what each part has lost; its mobile launcher's still
  come apart into their kit. A holed tank leaks what it
  holds, two hundredths each second for each block it lost; a holed engine does not light, giving no thrust and none
  of its delta-v; a holed capsule's heat shield is breached and it cannot aerobrake. Putting the right block back in
  its place mends the part wherever the rocket is.
- The cruise between worlds: a rocket resting in a world's orbit in its system's space, its transfer set and
  its window come, burns its transfer burn, its stages' propellant spent as the rocket equation says, and sets out, its
  crew told where it goes and for how long. It flies a gentle arc, slow to leave and slow to arrive, the world left
  behind shrinking in the window and the one ahead growing, and keeps the space ahead loading before it gets there;
  space not loaded yet it waits for, its cruise a little longer. It comes to rest at the spot of the other world's low
  orbit, told as a thing in that orbit, its plan flown and its crew told. When everyone aboard sleeps, the cruise
  passes at once. With nobody aboard and no chunk loader, it freezes with its space as any moving structure does. On
  the way, unless the server turns them off: a micrometeoroid holes the hull now and then, and the air of the room
  behind rushes out, a twentieth each second through each hole, till a suit patch put on the hole mends it; a solar
  storm is told twenty seconds ahead and fills the open space with radiation for half a minute, the crew sheltering
  behind the water and the cargo; a part burns through and loses a block. The climb to orbit now spends the propellant
  it burnt from the rocket's tanks.
- Arriving and choosing where to land: at the end of its cruise a rocket is captured into the other world's
  orbit by its capture burn, or by aerobraking where its plan meant to and its heat shield is still whole (one breached
  on the way burns the capture instead); short of the delta-v it burns what it has and flies past, not captured, its
  crew told. A transfer sets out only with the delta-v for its capture too. Arriving, and on every pass of the orbit
  after, the crew photographs a strip of the ground under the orbit for its team, the world turning a little under it
  each pass, and a team knows the ground round where its players have stood on a world. The landing map shows only what
  the team knows, from its instruments, those passes and its visits, the rest under the grey of the unknown; the known
  ground too steep to land on is marked in red (HAZARD shows or hides it), with how many such places lie in the
  ellipse; in its corner, the whole world's small map with what is known of it, and how much. The ellipse comes from
  the guidance: an instrument ring's guidance platform, formed and not let go, lands within Perseverance's 7.7 by 6.6
  km, the flight computer alone within twice that. SET fixes the landing where the spot is marked, for the descent.
- Coming down to a world: a rocket resting in the orbit of the world its landing is set on leaves its orbit
  for the spot over the landing, sinks into the world's sky and comes down somewhere in its guidance's ellipse round
  the spot marked. Where there is air it glows as it enters, its heat shield taking the heat, and opens its three
  orange and white parachutes; in thick air they set the capsule down, in thin air (Mars) its engines brake it the last
  of the way to its legs, and where there is none (the Moon) its engines brake it all the way down, the dust rising at
  touchdown, the propellant spent. With no legs, or no propellant left to brake, it crashes; with no heat shield for
  the air it burns up as it enters. Off, those rules hold the landing instead, the rocket staying in its orbit, and a
  crew whose team does not know what kills there may not land. Landed with a transfer set and its window come, a rocket
  lifts off from the ground with no pad; a lunar lander's ascent stage leaves its descent stage standing on the ground
  as its launch platform.
- The rocket's effects as approved: its engines burn with a bright flame and pouring smoke in air, leaving a
  white trail that thins as the air does on the way up; where the air is nearly gone, any engine shows the short hot
  core and the wide pale blue plume of a nozzle in a vacuum; a stage let go fires its retro rockets; the Moon's dust
  flies out flat in rays all round a touchdown. The plume burns for the whole climb now, and while the engines brake a
  landing, where before it showed only at lift-off.
- The telemetry line's model, for the devices and satellites to come: a line of the series' cable block that
  carries data only, every connected stretch of it one ground system of the antennas, receivers, decoders, data
  recorders and ground stations it joins. A ground system works with an antenna, a receiver, a decoder and a ground
  station, keeps what comes down with a data recorder, and its antennas add up. An antenna is small (the low orbits),
  medium (the geostationary orbit and the Moon) or large (the other worlds). A geostationary satellite is heard by an
  antenna under it that hears that far; one in a low or polar orbit only while it passes over one, the world turning
  under its orbit pass by pass; a relay satellite passes the signal on all the time.
- The starship's section model: every ship is built of modules, a spine of sections from the nose to the tail
  joined by its class's standard joint, with the wings at the second section, nacelles on wings that carry them, the
  fins and the tail engine at the spine's end (never behind a ramp opening there), side intakes, thrusters round it
  and the gear under it. A small ship's hull has room for one medium module or two or three small ones. A design
  flies with a cockpit, an airlock, a reactor, a main engine, the gear and a whole spine, its parts all where they go
  and of one class; what keeps one down is named.
- The small starship's catalogue of 25 parts: the Orbiter, Cab and Greenhouse cockpits; the Airlock, the
  Docking Module (its dorsal collar is an airlock too) and the Corridor; the Bay Doors, Ramp and Side Door holds, each
  filling the small hull's room; the reactor with flush radiators or with radiator fins; the Twin Bells and the
  four-bell Cluster at the tail, and the Magnetic Nozzle in a nacelle; the Side Intakes and the Nacelle Intake, both
  turning their nozzles down to take off vertically; the Blended Delta, the Stub Wings that carry the nacelles and the
  Swept Wings; the Twin Canted Fins and the Single Fin; the thruster Blisters and Quads; Wheels and Legs.
- The starship's block kit: 61 blocks, the same for every maker, in a row of their own in the J's Space tab.
  The structure's 14 (hull frame and plating, access panel, livery and thermal plating, carbon edge, deck plate and
  grating, bulkhead, pressure door, conduit, cable tray, viewport, light panel), the cockpit's 6, the airlock's and
  docking port's 6, the reactor's 5, the main engines' 5 (the Ship Engine Mount among them), the air-breathing
  engines' 5, the wings' and fins' 5, the thrusters' and landing gear's 6, and the modules' 9. Every part of the
  catalogue is built of its kit: the sections of the spine as rings of the hull four blocks wide and three high, the
  other parts set from their places on the ship, wings, nacelles, intakes and gear on both sides.
- The starship parts' formed models, as the approved catalogue drew them, the joints drawn on the hull where a
  section meets the next: the cockpits with their windows, the airlock's door, the docking collar, the holds' doors and
  the ramp lowered at the tail, the reactors' radiators, the tail engines' bells, the nacelles with their swivelling
  nozzles and magnetic nozzles, the wings, fins, thrusters and gear. Each in three finishes chosen apart from the
  parts: the Orbiter's white blankets over black heat tiles with the red band, Bare Metal's patched plates with the
  crimson band and hazard stripes, and Enamel's crimson over cream with a gold line. The gear reaches down two blocks
  to the ground, the kits' gear and thrusters set where the models draw them.
- Three makers of starship parts, each owning a family of the catalogue: Meridian Orbital (the Orbiter line),
  Ironside Yards (the working line) and Kosmozavod (the glazed navigator's nose, the docking module, the side door
  hold). A ship mixes makers from module to module, and every part a player goes into has its maker's interior:
  Meridian's padded white panels with a red line at hand height, Ironside's bare plates on ribs over grating floors,
  Kosmozavod's turquoise panels over beige quilted padding on a wooden floor; each module's bulkhead with its doorway,
  and in the cockpits the flight deck with its displays and switch panels. The windows are glass the world is seen
  through, from inside and out.
- The small ship's functional modules, a third of its hull's room each: the Workshop, whose vanilla crafting
  table, smithing table, furnace and barrel stand in it and work aboard; the Laboratory, with its bench, sample racks,
  microscope and analyser; the Quarters, whose bunk is a respawn point in a sealed cabin of breathable air and wakes
  no one where the air is gone; Computing, with its desk and server rack; and Small Cargo, its racks and a barrel. The
  airlock's oxygen panel fills a worn suit's tanks from the cabin's air, and its suit locker keeps a crew's suits; the
  holds keep their cargo in vanilla barrels, strapped down by the hoist rail and the manifest terminal; the reactor
  room has its fusion drum. Fixtures the models draw stop drawing themselves once their part is formed.
- The starship cockpits come alive: each maker's rich pilot's seat, and the flight shown as it goes. Meridian
  Orbital's three displays follow the ship, the horizon banking and pitching, the map turning under the track, the
  engines' bars taking their load, and its master caution lights amber with the master alarm; Ironside Yards' green
  CRT runs and its needles move; Kosmozavod's black-faced needles move, its lamps light green, amber and red, its
  guards lift while the engines run and its navigation globe turns with the heading. The side stick leans with the
  pilot's hand and the throttle slides forward.
- The small ship's cabin is pressurised: every block of its kit holds a crew's air against the vacuum, and
  with no life support section its flight console keeps a small air reserve, two hours of one crew member's oxygen,
  topping the cabin back up to the sea's air as the crew breathe and the hull leaks; an oxygen bottle used on the
  console pours into it. Inside, the helmet comes off.
- The Small Shipyard, the dry dock a small starship is built in: a pit sixteen blocks wide and twenty-four
  long dug three deep into the ground, its rim of hazard concrete and a cradle of hydraulic clamps rising from its floor
  at four stations; down both sides four lattice towers, eleven blocks to the rails of a gantry crane, with walkways at
  two heights and a stair up to both; a floodlight on each corner tower; at the back the control booth on the first
  walkway with the Shipyard Terminal inside, at its foot the Item Port and a conveyor into the bay, and a block feeder
  on each first walkway. Built whole with the pit clear, it forms into the approved dock's model, and its blocks take
  its shapes: the walkways their gratings, the towers their lattice, the stair its steps.
- The Starship Design, the Small Shipyard's builder, opened at its terminal once the dock is formed: the ship
  on a dotted floor in a dark void, turned with the right mouse button and brought nearer with the wheel, panels of
  dark glass round it. A part is dragged from its card, or off the ship, to the green node of a joint and snaps there,
  held over the ship in cyan till it is let go; a section taken off the spine leaves its gap till it is set down. The
  parts by kind and by maker, each card with its own model; the chosen part's length, mass, blocks, power and note;
  the flight check; the ship's mass, thrust, vertical take-off thrust against weight, power and crew; the room left in
  its hull; its finish; the blocks the dock needs; the ship's footprint on the floor with its length and span. Flip
  turns a section's door and fittings to the other side. S saves it among the dock's plans by its name, E sends it to
  the dock once it is ready to fly and fits the bay.
- A ship sent to the Small Shipyard is built by hand from its hologram: laid out over the pit as the dock
  stands, every block still to place shows where it goes, the layer being built glowing amber, and each part still to
  form has its box with a tag lifted over it, its name, its size, how many of its blocks are in place and whether it is
  the one being built. Each part of the ship forms by itself into its model as its last block goes in, and its blocks
  take the model's shapes, the hull's floor and ceiling leaving room to walk inside; a block taken away undoes it. The
  terminal draws the parts formed, and breaking it gives their blocks back.
- The Small Shipyard's robot arms build the ship sent to it: four orange arms, two on each side of the bay,
  each running on its rail along the bay's edge. What the ship wants goes in through the dock's item port, and no more;
  each arm takes a block from the feeder of its side, swings it over to its place, sets it down and welds it with
  sparks, and a part glows round its box for a moment as it forms. The arms' blocks are handed back when another ship
  is sent, and dropped when the terminal is broken. On a J's Computers network, by a data cable in through the gland
  in the control booth's outer wall beside the terminal, `RUN jscore:assemble WITH plan = '<name>'` sends a plan the
  dock keeps by name and sets the arms building, and the network brings the blocks.
- A ship built whole in the Small Shipyard leaves the dock's hold when its pilot uses its flight console with
  an empty hand: it becomes one moving structure, once, of the blocks at the places of its plan and no others, the
  dock's left where they stand, each block and what it keeps as it was. It stays a structure from then on, standing
  still or flying, its flight console keeping its plan and drawing its parts aboard; the dock is free for the next.

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
