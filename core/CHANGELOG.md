# Changelog of J's Core

Every change to J's Core, newest first, taken from the series' changelog
([CHANGELOG.md](../CHANGELOG.md)), which records each change once with the mods it concerns and keeps
the releases before 0.5.0a. This file is written from it by `./gradlew changelogs`.

## [Unreleased]

### API
- The promise: J's Core's API is at version 5. Beside its api packages, `JsCoreApi.PROMISED_PACKAGES` lists
  the packages of the Core that are part of the API, each a kit a mod may build on; a kit enters the list once it is
  documented for a reader who knows nothing of it. Every part of a mod is internal, experimental or stable, and a part
  is stable only once two real mods use it, it has real tests, an example that compiles, what it saves and sends
  defined, and a whole cycle without changing shape; `@ApiStatus.Experimental` comes off then, not on a date. The
  test of the API follows every name a public or protected signature says to where it comes from, so the API can hand
  a mod no type it does not promise; `docs/API.md` says how.
- The first kits promised: J's Core's API is at version 6, and four of the Core's packages join the promise,
  experimental: `dev.jstech.core.call` (calls from a screen to the server), `dev.jstech.core.task` (work away from
  the tick), `dev.jstech.core.codec` (codecs for records) and `dev.jstech.core.optional` (other mods behind a
  guard). What is marked `@ApiStatus.Internal` in them is public only so the Core can wire itself up, and is no
  promise.
- Looks that draw everything: J's Core's API is at version 7. `ISkin` draws a whole window, a slot, a check
  box, a progress bar, a separator, a tooltip and a button that cannot be pressed, and draws and measures text in its
  own font, each with a default that keeps a skin written before as it was; all experimental.
- The richer controls in every look: J's Core's API is at version 8. `ISkin` draws a slider, a switch, an
  option button, a drop-down list and its open list, a chart's ground, lines and series, and gives stored energy its
  colour, each with a default for a skin written before; all experimental.
- Fields in every look: J's Core's API is at version 9. `ISkin.fieldText()` is the colour of what is typed in a
  field, the primary text unless a look's fields differ; experimental.

### Added
- Errors that teach: when something a mod declared cannot stand (a block with no name, a setting whose
  default is outside its range, two things under one id), the game stops loading with one paragraph saying which
  mod, which declaration, what is wrong and how to fix it, from the content builders, the settings, the registries
  and the data generation alike.
- A data run that checks itself: ending a mod's data generation with `check()` makes the run fail, saying what
  to add, when a block or an item the mod registered has no English name, no block state, no model, a model whose
  texture is nowhere, or no loot table while it drops something. The Core's own run does.
- The log by system: each part of the Core writes to the log under a name of its own (`jscore/media`,
  `jscore/saves` and the rest), any mod can declare its own, and `/jstech log` lists them and sets how much each
  writes while the game runs, for the server's administrators.
- Live reload in development: a development run watches the `assets` and `data` folders its mods are loaded
  from and, once changed files stay as they are for a moment, reloads the player's resources and the server's data
  packs by itself. A file whose name carries a run's mark (`LiveReload.ownFile`) is that run's alone, so runs that
  share the folders, as test clients side by side, do not reload for one another's. A packed game watches nothing.
- Capabilities of your own: `CapabilityKinds.declare` makes a capability for blocks, items and entities in one
  line, and `CapabilityCache` keeps a neighbour's answer between ticks, worked out again only when the game says it
  changed. A part on a cable answers capabilities on its face by overriding one method, and any block that holds
  parts gets the same with one line.
- Calls from a screen to the server: a call is declared once with what it carries and what the server does
  with it, and the server handles it only while the player still has that kind of menu open and in reach, all in
  one payload the Core registers.
- Work away from the tick: `Tasks.onServer` runs long work on a virtual thread and hands its result back on
  the server's thread, within 2 ms of each tick; whatever is still out when the server stops is cancelled.
- Codecs for records: a record's codec is written one line a component, with no limit on how many, its
  names checked against the record's as the game loads, and the codec it travels the network with is taken from it.
- Other mods behind a guard: `OptionalMod` starts the work with another mod only when it is installed, and
  leaves it out with a line in the log when it fails to start; `OptionalDependencyCheck` lets a mod's own unit test
  fail on an import of the other mod outside its adapters.
- Screens declared in files: a screen is a JSON file in a resource pack saying what it holds and where (rows,
  columns, grids, panels, titles, labels, buttons, slots, the player's inventory, bars, pictures, check boxes,
  fields), and its Java keeps only what it does. The menu's slots are moved under the frames the file placed. An
  edited file rebuilds the open screen at once, and one that cannot be read says what is wrong in its place.
- A layout engine: rows and columns that share their room and give it up down to their smallest sizes, wrap
  and justify; grids of fixed, fitted and shared columns; stacks. Every size is a whole pixel, the shares add up, and
  the overlap check runs on the layout the game draws.
- Values a screen shows by name: a menu declares a value under a name and a screen's file binds to it, with
  no packet written for it; a value of any kind its codec writes, a number past 32767 included, is sent as the menu
  opens and then only when it changes.
- Styles the player picks: everything a screen declared in a file draws comes from a style, chosen under This
  player for every screen and per mod, with a preview. The Core brings the game's own (the default, drawn from the
  game's widgets, so a resource pack that repaints them repaints the Core's), J's Tech, high contrast, three for
  colour blindness and the eras'; a resource pack replaces any of them or adds its own as a file.
- New components, in code and in screens' files: sliders free or in steps, switches, option buttons, drop-down
  lists, a colour picker; trees, tables that sort and filter, line, bar and pie charts, a node graph editor; a 3D view
  of a block, an item or an entity, a map in the game's colours, tank gauges in the fluid's own texture, segmented
  energy bars and ghost slots; and tooltips whose lines hold items and pictures.
- Settings for every mod: the settings screen, rebuilt for any mod on the Core, is drawn in the player's style
  (the game's own by default: its tabs, buttons, sliders, fields and lists over the blurred world). Each kind of
  value has its control: sliders with the number typed beside them, drop-down lists, a colour picker, editors of
  lists and maps, and a picker of items, blocks and fluids with a search. Groups fold away, the search looks through
  every tab, a filter shows only what is off its default, a setting has a button back to its default, Undo and Redo
  take changes back and again, a description shows its picture and a link into the manual, and the keyboard reaches
  everything. A name too long for one line, on a small screen or in a longer language, takes two.
- Settings that say when they take hold: a setting takes hold at once, on a reload or on a restart, and the
  screen marks it and counts the changes that wait for a restart. A setting can be active, or shown, only while
  another is on, and rules between settings say what is wrong over them and keep Done from saving it.
- A server's settings from afar: a world's settings reach every player as they join and whenever one changes.
  An operator changes them on their own game's settings screen, the server checking the permission; other players
  see them, read only. `/jstech config get`, `set`, `reset` and `preset` change them by command.
- Presets and settings as text: a mod ships named presets with a file, a player saves their own and a server
  owner picks one. Export writes a file's settings as text to share, and Import takes them back. A world's settings
  file that does not exist yet starts as the modpack's copy in `defaultconfigs`, whatever its format.
- Told of a change: a mod is told when a setting changes, at once, wherever the change came from.
- One question before changing the world: every mod of the series asks the Core whether an actor may do
  something to a place (break, place, use, explode, burn, flow, move into, shoot), and every protection installed
  answers: the game's spawn protection, FTB Chunks and Open Parties and Claims when they are there, and any a mod
  registers. A machine, a robot or a moving structure is weighed as its owner, online or not, never as a generic fake
  player; the world's own work stays inside the claim it comes from. Robots ask before they break a block.
- Permissions for everything: every command of the series, opening, using, setting up and breaking what others
  own, changing a server's settings from afar, building multiblocks and going past the limits have a permission
  node, which LuckPerms and FTB Ranks grant or deny; without them the operator levels apply. `/jstech dump permissions`
  writes them all out, with two LuckPerms groups ready to paste into the console.
- Trust by name: the owner of a thing trusts players by name to use it, to set it up as well, or to break it as
  well. An owned block or entity is kept from whoever its owner does not let break or use it.
- Limits per player: how many of a kind of thing one player may have at once, kept with the world, starting
  with robots (16 each by default).
- The audit log: when the server keeps it, who used, changed or broke what another player owns is written down,
  and `/jstech audit` reads it, of everything, of one player or of one place.
- Damage between players follows the server's PvP rule, the claim the victim stands in, and the teams:
  teammates do not hurt each other unless the server lets them, and a machine or robot hurts as its owner would.
- Sections of work: a mod declares a named section and runs its work through it. The section is timed for the
  F3 screen, runs in a frame of its own name, so Spark and any profiler that samples the threads show it by name,
  and is watched: a run that goes on too long is written to its system's log with the thread's stack, the first time,
  and the operators are told. A section with a budget slows itself while it takes more than its budget a tick, to one
  tick in two, four and so on, says so, and speeds back up once well within it. The energy and fluid grids and the
  results of work away from the tick are sections.
- Network use: what the server sends each player is counted by kind, by mod and by the block whose updates took
  it, and `/jstech bandwidth` shows the last minute for everyone or one player, with a graph and the biggest senders.
- What weighs: `/jstech cost` measures what the series' blocks cost the server for some seconds and lists the
  worst places with their owners, and the cost of each kind; nothing is measured between measures.
- `/jstech health`: the state of the series' systems and their warnings (the ticks, sections slowed or stalled,
  network use, work away from the tick, each dimension's networks and grids, the chunks each owner keeps loaded, the
  protections, the audit log, the robots), each mod adding its part.
- Crash reports and a diagnostic file: a crash report has a section of the Core with what was running, what was
  slowed or stalled and the state of the systems, and `/jstech diagnostics` writes the mods, the settings, the
  timings, the sections, the health, the network use and the log's levels to a file to attach to a bug report.
- Notifications on the HUD: a mod declares its kinds of notification once and sends them from the server; each
  comes in the shape of the game's toast, in the player's style, with its level's icon, colour and sound (information,
  warning, alert), the count of alike ones, its mod, what a click does and a line that shortens as its time runs out.
  A click while the chat is open opens a manual's page or shows the place on a map. In the top right corner they stand
  below the game's own toasts. The operators' warnings of stalls and slowed work come this way.
- The history of notifications, on N: filtered by level and by mod, each line clicked to act on it, taken off
  the list, or, with the right button, its kind or its mod silenced. The player chooses the corner they appear in and
  can ask not to be disturbed, which holds the game's own toasts too.
- The J's Tech creative tab: the series' one tab of what its mods share, in three sections (materials, fluids
  and components, shared blocks and tools), showing only what the installed mods use; what it shows is left out of the
  other tabs of the mods that declare their uses. It takes the place of the J's Core tab.
- The open catalogue of materials: materials, forms, properties, mineral families and host rocks are
  registries any mod adds to. A material has a kind (metal, crystal, gem, non-metal solid, mineral, liquid, gas,
  exotic matter) that says the forms it can take, the states it can be in, a colour and the real data it carries; it
  has no tier. The Core brings the 21 base forms, five properties (symbol, atomic number, formula, colour, state at room
  temperature), eleven mineral families and the game's twelve rocks; the series' catalogue starts with iron, copper,
  gold, tin and bronze, the game's own ingots, nuggets, blocks, raw ores and ores kept as theirs.
- Only what is used exists: a mod declares the materials and forms it uses, and the Core registers the union,
  each thing once, named from its form and its material ("Tin Plate", "Deepslate Tin Ore"), in its common tag and its
  form's (`c:plates/tin`), each block mined with a pickaxe and with its drops; an ore is declared with the rocks its
  veins cross, one block in each, every one dropping the same raw item.
- Art made as the game loads: every thing of a material is drawn in its material's colour as the game loads,
  from the game's own texture for the forms the game has, recoloured texel by texel, and from the Core's templates for
  the rest; an ore is its rock's own texture with the specks of one of the game's ores in the mineral's colour. No
  texture of the game ships in the Core, and a resource pack can replace any of them.
- A material's data in its tooltip: each property shows always, only with Shift held, or never, as the property
  says unless the material or a data pack says otherwise, the game's own iron ingot included; a data pack gives values
  over a material's own, one file per material.
- Packs made as the game loads: a mod declares a resource pack or a data pack whose files it makes from what the
  installed mods declared, each made only when first read, always on and below every pack a player or a server adds.
- The 118 elements: every known element is a material of the series' catalogue, with its symbol, its atomic
  number, the kind it is at room temperature and the states it can take; their forms exist only when a mod uses them.
- Isotopes: materials of their own tied to their element by their mass number, named after it ("Uranium-235")
  and written as chemists write them (²³⁵U); deuterium, tritium, helium-3, the reactor's fuels and targets, the
  sources of radiation, and the stable isotope of each superheavy element of the known table.
- Beyond the known table: the elements 119 to 250, under their systematic names (ununennium, Uue) until they
  are discovered. On a server, the player who first discovers one gives it its name and its symbol, for everyone and
  in every language, kept with the world; a name holds letters only, repeats no other and can be asked to end in
  "-ium", a symbol has one or two letters, and an operator can take a name back. Where discoverers do not name them,
  seventeen take the series' own names, Genevium to Aeternium.
- The exotic forms of matter, a kind of their own, each in the state it calls for: metallic hydrogen,
  neutronium, quark-gluon plasma, strange matter, dark matter, Bose-Einstein condensate, negative-mass matter,
  degenerate matter, time crystals, photonic matter and computronium.
- Minerals: a mineral knows its main element and its byproducts in fixed shares, the family that decides how it
  is processed and the rocks its veins cross. Every element mined as such has a generic ore in `c:ores/<element>`.
- Naturally radioactive minerals: every mineral with uranium or thorium in it is marked so in its tooltip, and a
  world's setting turns that off; the dose comes with the Core's radiation. Any mod can give materials marks of its
  own.
- Liquids made from materials: a material's liquid exists only when a mod uses it, molten for a solid,
  liquefied for a gas, at the temperature the mod says; it pours into the world, fills a bucket and is drawn in its
  material's colour.
- The shared catalogue: the common fuels (gasoline, kerosene, diesel) and the generic components (servomotor,
  electric motor, LED, sensor, oscillator, antenna, magnetic head), each existing only when a mod uses it, in a
  catalogue any mod adds to. A mod's simpler recipe for one carries a condition that, where J's Industrial is
  installed, the mod's own server rule can close, making the industrial chain the only path.
- Grids of a mod's own: the seven grids become a registry any mod adds to, each with its policy, how what it
  carries is shared along a run and what its wires plug into. The Core's energy and fluid policies are the defaults,
  so a mod's own grid, as a voltage class that never joins another, moves energy or fluids as the Core's grids do.
- States of matter: gases, supercritical fluids, slurries, plasma and exotic matter are each a kind of storage
  of their own, with a capability, tanks that hold their volume times their pressure, and a line of their own that
  never joins another's, moving matter as pipes move fluids and losing nothing. Plasma and exotic matter are held in
  containments that spend energy while they hold: without it plasma dissipates and exotic matter stays sealed in. A
  gas let out is lost. A gas line also fills and empties Mekanism's tanks and the tanks of mods that keep gases as
  fluids, hydrogen being hydrogen in each, and a multiblock's ports pass every state.
- The energy kit: a mod declares its energy in a few lines (its unit and what it is worth in FE, its classes of
  line with their ratings, what each class loses by distance, and what befalls a port of another class) and gets the
  rest: a grid for each class whose lines never join another's, stores and ports, the sharing along the lines, the
  conversion to and from FE that never makes energy, and tooltips naming a port's class. A port one class below a
  live line burns and stays damaged, two or more below is hit by an arc flash, and one above is short of voltage and
  does not start; nothing happens on a dead line. A mod that only wants FE has it with nothing to declare.
- Generators that follow the load: a generator makes only what its line draws, so it burns only that much, and
  may declare a minimum it always makes and how fast its output changes, as a reactor does.
- J's Energy: the series' energy, counted in joules and shown in watts, its lines in five voltage classes from
  400 V to ±1,100 kV direct current, each its own grid; transformers join neighbouring classes and an HVDC station
  the direct current one. A server's settings turn it on (off until a mod builds on it, when every block of the
  series speaks FE), let a block on a class too high only refuse to start, let every block take every class, let its
  blocks take FE directly, and set what a joule is worth in FE.
- The wrong voltage in the world: a burnt machine smokes and sparks and stays damaged; an arc flash destroys
  it, sets fire around it and burns whoever is close.
- The clamp meter: a handheld instrument that clamps on a wire of a line or on a block. Held, it shows the
  line's power and which way it runs beside the crosshair; held up, its display gives the details: what crossed
  since it was clamped, the line's ceiling and whether it is full, the most and the least, or a block's store and what
  goes in and out of each face, in the energy's own unit or in FE.
- Energy as a player reads it: amounts in SI, "7.9 kW", "1.92 MJ"; and a store can show its charge on its own
  model.
- The protection slot: a fuse blows on the overvoltage it is rated for, cutting the energy with no damage until
  a new one is put in, and lets an arc it cannot break jump over it; a battery keeps a machine going as a UPS,
  charging while the grid has energy and feeding the machine when it runs low.
- The basic cable and battery: the Insulated Copper Cable, a low voltage line of J's Energy carrying 10 kW and
  losing a tenth over a hundred blocks, and the Lead-Acid Battery Bank, 50 MJ, its charge on the gauge of its sides,
  both made of the game's own materials, so a mod used alone can carry and keep energy; with J's Industrial installed
  they are its first cable and energy bank, made with its recipes. While J's Energy is off they carry and keep FE for
  any mod's machine.
- The FE converters: a cabinet for each voltage class, from 100 kW at low voltage to 12 GW at ultra-high, and
  the Configurable Energy Converter, whose class the player sets. Each turns FE into J's Energy or J's Energy into FE,
  FE on the left of whoever faces it and J's Energy on the right, up to a limit set on its thumbwheels that never
  goes above its class's ceiling, and never makes energy. Its screen is the cabinet's face: the display, the RUN,
  LIMIT and FAULT lamps, the FE and J's Energy meters, the bridge seen through a window with the energy running
  through it, the class keys or the ceiling, the knob that sets the way and the switch. A line of the wrong class
  stops it. A mod declares a menu its blocks open with `ModContent.menu`.
- The containment of exotic matter and the Antimatter Trap: the Core's shared blocks where the series' mods keep
  exotic matter, the containment every exotic form but antimatter and the trap, a Penning trap, antimatter alone. Each
  spends energy for as long as it holds something; without energy it takes nothing and lets nothing out, so nothing is
  ever lost; broken, it keeps what it holds in the item it drops, and placed again it holds it again. Antihydrogen
  joins the catalogue, and a material's mark says whether it is antimatter. A matter tank can be set to what it holds,
  as one placed from its item is.
- The ends of a line: each end where a pipe or a line of items plugs into a block is set on its own: normal, push,
  pull or off; a priority, the highest filling first and ends of one priority taking turns; a rate; one of sixteen
  channels; and a filter by item, tag, mod or components, or by fluid, whose amounts keep that much in the block and
  no more. Every default keeps a pipe from an output to an input working as before; an end offers the first thing some
  other end would take, so nothing holds the line up.
- Lines of items: a grid that moves items between inventories as the pipes move fluids, for a mod's tubes.
- Lines that group: a line that carries nothing, each run of it a group of the machines it joins, which hear
  the group they are in, as research computers or a ground station's devices; dyed runs stay groups of their own.
- Beyond a line's limits: a pipe's rule says what follows when a fluid it is not made for reaches it, by
  default nothing; the Core gives a rule that breaks the pipe after a while and lets the liquid spill.
- The Basic Wrench: the series' shared wrench, in the common wrench tag; on a cable it goes round the modes of
  the end it points at, and says which it set; sneaking, it opens the end's screen. Items can be declared in tags of
  their own, besides their tab's.
- The screen of a line's end: on an end of a pipe or a line of items, a screen in the game's own look sets
  everything the end has: its mode; its priority and its rate, typed or stepped, ten at a time with Shift; its channel,
  one of sixteen in the dyes' colours; and its filter, only or all but, whose nine slots take a stack clicked onto them
  (the item itself, the liquid in a bucket or a tank, the gas in a cylinder), go round the thing, its tags and its mod
  with a right-click, and keep in the block the amount set by scrolling on them. A line under the filter says what
  the end does. Any mod's wrench opens it with `EndSettings.open`. A filter's rules can be replaced, removed and given
  amounts one by one, and a grid's policy says whether its ends are set one by one (`routesEnds`).
- Seeing what flows: each wire of a pipe or a line knows what it carries, which way and how full it is. A pipe
  with sight glasses shows its liquid or gas moving through them at its level, and a line of items with windows its
  items sliding through; a player holding a wrench, or wearing goggles of the `jscore:sees_flow` tag, reads beside the
  crosshair what the wire they look at carries, which way, how much a tick, and how its end is set. A cable type can
  have windows and a fitting of its own at its joints.
- The network probe: a handheld instrument of the clamp meter's family that reads a whole line of any kind.
  Linked to a wire, held up, its display shows the line on four pages: the summary (the flow of the last minute
  against the narrowest cable and whether it runs full, what the consumers want and how much of it they get, the
  losses, and how many sources, consumers and stores it joins), the members (every block it plugs into, by what it is
  to the line and by kind, with what each gives or takes), the flow (each second of the last minute, with the most,
  the least and the mean) and the losses. OK outlines the chosen blocks in the world, through walls, for ten seconds;
  HOLD keeps the reading still, SHOW puts it beside the crosshair, LIGHT lights the display and POWER switches it off.
- Overhead lines, as in Immersive Engineering: bare wire hung from insulator to insulator, within the line's
  longest span and along a way nothing stands in, by using the line's item on one insulator and then the next. Any
  line can be hung, as its mod decides. Connectors hand what the wire carries to their block, relays pass it along,
  and a feedthrough takes it through a wall. A hung wire is drawn sagging along its curve, loses as much as its length,
  and falls when its insulator goes. Blocks offer their insulators through `OverheadLines.ANCHORS`; the poles, towers
  and insulators are a mod's.
- Shocks and surges: touching a live overhead wire hurts, more the higher its class, and kills from high voltage
  up, with the Core's electric damage, which no armour stops; lightning on an overhead line or its insulator sends a
  surge along it that burns the machines on it, unless a fuse in each blows first or a surge arrester on the line
  takes it to earth. Each can be turned off in `jstech-energy.toml`.
- Dragging a run of cable: holding the click with a cable and looking further along outlines the run from the
  first block to the one looked at, up to 32 blocks away, along the longest axis first, white where it can be laid
  and red where something is in the way; let go, the whole run is laid at once, each block joined to the next, up to
  64 blocks. It stops at the first block something is in the way of, a protected place, or when no cable is left in
  hand, and says where. A cable that runs only straight is dragged straight.
- Reading a line: a grid's mover tells what a part of it did in the last tick (`IGridMover.survey`): each block
  it plugs into with what it gave and took, what it moved, what its consumers asked, what it lost and its narrowest
  cable. The energy, fluid, matter and item lines all keep it.
- The basic pipes: the Copper Pipe for liquids up to 200 °C, the Iron Gas Pipe, and the Vacuum-Jacketed Pipe
  for cryogenic liquids, thick pipes with sight glasses and solid fittings, made of the game's own materials so a mod
  used alone carries liquids and gases. A pipe carrying a liquid hotter or colder than it stands bursts after five
  seconds and spills it. With J's Industrial installed they are its pipes, made with its recipes.
- Sensors that speak redstone: a sensor or a machine says what it reads as readings, each with its span, and
  gives the one chosen in redstone through one capability, `Signals.OUTPUT`, which anything that reads sensors asks; a
  comparator reads it too. A sensor's neighbours are told only when what it gives changes. A mod writes only what its
  sensor reads.
- Signals by frequency, redstone without wire: a transmitter sends what is fed into it, from 0 to 15, on a
  numbered frequency of its owner's or of its owner's team, or to a pair of its own, and every receiver on it gives it
  out. Two transmitters of one owner on one frequency clash, and their receivers give nothing until one is taken off.
  A transmitter is heard as far as its power, less four blocks for every solid block in the way, its value whole
  within and nothing beyond; a relay of its team sends it again, and a relay's partner sends it again elsewhere, in
  another dimension too. Nothing is worked out on every tick, and a receiver's neighbours are told only of a change.
  The transmitters, receivers and relays are a mod's, on the Core's kit.
- The signal probe: a handheld instrument of the network probe's family. Used on a block it reads the redstone
  on it, now and its peak since; held up, its display lists the frequencies heard where the player stands, each with
  its number, whose it is, where it comes from, how well it arrives and the value it carries, clashes marked in red,
  and the frequencies of the player's team that are not heard there; on its radio's page one is told in full, and OK
  outlines where it comes from.
- What every machine shares: its state always in view (working, waiting for input, output full, no energy,
  undervoltage, fuse blown, damaged, off by redstone, switched off), on its lamps and in Jade, with a puff as it starts
  and smoke as it works and stops, and the sounds its mod gives it; an ON/OFF switch; redstone modes (ignore, with a
  signal, without one, one recipe a pulse); a comparator that reads what the player picks (progress, input, output or
  energy); energy of a mod's own through a port of a class, with its fuse and battery; tanks of gases,
  supercritical fluids, slurries, plasmas and exotic matter; damage instead of wear, from overvoltage, explosions
  (which no longer break a machine) and sabotage, shown as cracks and working slower, mended by a repair kit held on
  it for a few seconds; values the outside reads and sets, for controllers and networks; its owner, whoever placed it;
  and rest while it has nothing to do, so idle factories cost the server nothing.
- Machine sides: each face of a machine takes in, gives out, does both or nothing with each of its resources
  (items, fluids, each state of matter, energy), and pipes, cables and hoppers find only what the face lets through. A
  machine pushes what it makes into the block beside its giving faces and pulls what it works from the block beside its
  taking faces by itself, at a rate and through a filter.
- Machines that never jam: an input slot locked to an item takes nothing else; the player picks the recipe when
  the inputs make several; and factories of several lanes work side by side, sorting what is put in among them.
- The whole recipe format for machines: catalysts that are not used up, gases and the other states of matter in
  and out, byproducts by chance, and the temperature and pressure a recipe runs between, all in the recipe files and
  shown in JEI and EMI.
- The machine screen: every machine of every mod opens the same screen, in the game's own look, five tabs over a
  steel face with the machine's name, class and lamps. Main shows the machine's own face, or the standard one (a meter
  of what it draws, an LCD of what it made the last minute, the process window with each lane's arrow, its switch and
  a sight glass of its first tank); Sides shows the machine as two cubes whose faces are picked and turned, with the
  switches, thumbwheels and filter of what it pushes and pulls; Redstone has the knobs of how it answers redstone and
  what a comparator reads, the lamps of the signal in and the meter out; Upgrades has its bays and the bars of what
  they do; Protection has its port and line, its fuse, its battery and the key of who may use it. Ctrl and a click
  locks an input slot to what it holds.
- JEI's "+" and EMI's fill button move a recipe into the open machine: its ingredients from the player's
  inventory into the inputs, once or a stack's worth, and the recipe picked among those its inputs make.
- Upgrade effects: besides speed and energy, an upgrade can make a machine work several recipes at once, make
  more items, make byproducts likelier, spend less fluid and matter, give off more or less heat, run quieter, and store
  more energy; each upgrade says how many of it fit in one machine. Upgrades are declared with no class of their own,
  and say what they do in their tooltip.
- A wrench on a machine: any mod's wrench turns what the face clicked does with the resource it shows, shows each
  face of the machine looked at in its colour, and, sneaking, takes the machine away whole, contents and settings
  going with it in its item.
- The Configuration Card copies a machine's setup and pastes it into others, whole into the same kind and what
  fits into another, or into every machine of a box at once.
- Machines grow in place: the tiers of one machine are declared as one family, and an installer makes a machine
  the next tier without breaking it, its contents and settings kept.
- Heat with real physics: blocks that hold heat have a temperature in kelvin and a heat capacity from their
  material, conduct heat to their neighbours by the material's real conductivity, give heat to the air as thick as it
  is and radiate, so in a vacuum only radiators cool them. Heat pipes carry heat between the blocks they join. The
  everyday elements carry their real density, specific heat, conductivity, emissivity and melting and boiling points.
  Only blocks out of balance are worked, a few hundred a tick, and the rest sleep.
- Boilers with the real enthalpy: a hot wall boils water into steam, each litre taking the heat that brings it
  to its boiling point and then 2.26 MJ a kilogram, and lower where the air is thin or higher in a pressure vessel.
  Heaters and coolers run at their power up to their limit, and a loop's liquid in its cool and hot forms passes
  through a wall to give or take its heat, so two passes make a heat exchanger. The game's water is the series' water,
  and water and the liquefied gases carry their real heat of boiling and molar mass.
- Cold has its consequences: a pipe of water freezes and carries nothing where the place is below freezing,
  unless it is insulated, and liquid nitrogen and the other cold liquids boil off their tanks as the place's heat
  leaks in, the slower the better the tank is insulated; a server can turn either off. Blocks of the world conduct
  heat as their real materials do, so wool keeps a hot block hot and metal draws its heat away, and mods can declare
  places with air of their own.
- Machines that make heat: a machine's casing warms as it works and is its heat port, through the faces its
  Sides tab opens to heat; past the hottest it stands it stops to cool and loses health, and starts again once cooler.
  A block past 300 °C sets alight what burns against it, and one past 150 °C burns whoever stands against it; a
  server can turn either off. Jade shows every hot block's temperature, and sensors read it as a thermometer or a
  thermocouple does.
- Mechanical power in real units: shafts, gears and gearboxes join into networks that turn as one, each part
  at its ratio, gears trading speed for torque and turning the other way; a network with too little power turns
  slower, where its sources' torque meets what its loads ask, and one whose gear ratios disagree jams. Sources
  declare their torque curves, and a machine becomes an electric motor that spends its energy to turn its shaft, or
  a dynamo that makes energy of a turn. Networks are worked out only when they change, and turned whole each tick.
- The rest of the mechanical kit: flywheels that smooth a line and coast after the power is cut, clutches that
  let go while powered, brakes that hold as hard as their redstone says, speed controllers whose end turns up to
  twice as fast by their signal, shear pins that shear under overload and spare the rest, and parts that break past
  their rating where the server lets them. Belts and chains join two pulleys across a distance, the smaller turning
  faster. Machines can be driven by a shaft, working as fast as it turns and stopping below their least speed.
- Reading mechanical power: every shaft, gear and gearbox is a sensor of its speed for redstone and
  comparators, a torque meter reads the torque through it, Jade shows a part's speed and its network's power, and
  the network probe reads a network of shafts in watts, with its sources, its loads and what friction takes.
- Multiblock shapes with rules: a place can count its blocks ("from one to four energy hatches") and give the
  machine a number, as a blast furnace is as hot as its coolest coil; a pattern can be found mirrored or lying on its
  side, and can grow by a repeated layer, row or column, a tower as tall as the player builds it. Shapes need no
  drawing at all: any box between a least and a most size with its frame, faces and inside, as a dynamic tank; a
  floor of any outline with walls round it, as a smeltery; any filled cuboid of units. What stands inside counts,
  with neighbour rules: a cell gives more beside a moderator, a cooler works only beside a working cell. A failed
  check names the wrong place of the way the player most likely built it, and every rule goes to the pattern's file,
  so a data pack changes it with no code.
- Multiblock machines built in the world: a machine becomes a structure's controller and forms when the player
  has built it whole, in any order, standing "Not formed" until then; breaking any block undoes it and the controller
  keeps what it holds. Jade shows whether it is formed, its size and how many recipes it runs at once, or what it
  needs and where. Only its owner and those trusted break its blocks. It is checked only when a block near it
  changes, never every tick, and never while a chunk it reaches is unloaded.
- Multiblock ports as blocks of their own, placed anywhere on the shell: items, liquid, energy (meeting J's
  Energy cables with the controller's voltage class), each state of matter, heat, and a mod's own lines such as
  compressed air; and a redstone port that reads the signal round it for the controller or gives the controller's
  signal out. A dye puts a port in the group of its colour, and each group feeds a lane of its own, so two processes
  never mix. A structure runs as many recipes at once as it says, and Jade shows the exact number.
- Multiblock machines entirely as data: a file gives a machine its shape (a pattern by id or drawn, a box,
  walls or a cuboid), its slots, lanes and tanks, its energy, the kind of recipe it works and how many at once, and a
  controller block built on the Core's data machine block names the file. The files reach every player, and a data
  pack adds or changes a machine with no code.
- The Structure Projector: shows any multiblock of any installed mod in the world before it is built, the
  blocks still missing as pale ghosts, what is wrong outlined in red through walls, and above the structure its name,
  its size and how much is in place, missing and wrong; looking at a ghost says what goes there and on which layer.
  Its screen previews the multiblock in 3D layer by layer and chooses it from the list of every multiblock, its turn,
  its mirror, its layer and whether only what is missing shows. Every multiblock is listed with nothing more to write,
  and a mod can hide one.
- Multiblocks in JEI and EMI: every multiblock that can be shown has a page of its own, with the multiblock in
  3D turned by a drag or a click, a button for each layer, the blocks that will do where more than one does, how it
  may stand, its materials with how many of each, its size, and buttons that show its ghosts in front of the player
  and open its page in the manual. Looking up a block's uses finds the multiblocks built of it, and looking up a
  controller finds how to build it.
- A multiblock pattern names its controller's block, in code and in its file, so the projector and the recipe
  viewers show the controller in its place and count it; a machine declared as data has its controller block found by
  itself.
- Multiblocks in the manuals: a page names a multiblock by its id and it is drawn as a scene the reader turns
  and peels layer by layer, laid out from the multiblock in force when the manual is read, its controller in place.
- The Multiblock Builder, a handheld instrument used on a multiblock's controller: its display says what is
  missing and how much of each block the player has, lets them choose what goes where a place takes several blocks
  and where the blocks come from, and OK builds the multiblock a block at a time from the inventory, as the player
  would place each block, stopping and saying why when it runs out, the player walks away or a place is protected.
  HOLD holds it, SHOW shows the multiblock's ghosts, LIGHT lights the display. Servers set how fast it builds and how
  far the player may walk; other mods can add a source of blocks of their own.
- Microblocks of any full block of any mod: covers, panels, slabs and the thicknesses between, hollow pieces,
  posts and corners, drawn from the block's own textures. Several share one block space, a cable's included, each
  placed where the click puts it, hit, outlined and broken on its own with its own drop, and a block space of pieces
  alone is made where nothing stands. A whole cover closes a cable's face; a hollow one lets the cable through.
- The saw, a tool of the series' shared content, of a stick and two iron ingots: in the crafting grid, above a
  block or a piece it cuts two halves, beside one it cuts posts and corners, at a corner of one it hollows it; each
  cut wears it. Two pieces of one shape and block join into one as thick as both, and two slabs make the block again.
  Any mod's saw cuts alike, and a pack can keep a block from being cut.
- Facades: a cover joined with any cable hides the cable behind it on its face, while the cable keeps working.
- Microblocks drawn as cleanly as blocks: where two pieces on faces side by side meet along their edge, one
  gives way, so two blocks never flicker in one place; a face lying flat on an opaque piece is not drawn, nor a
  neighbour's face behind an opaque cover.
- Parts that move: a kind of part with a fan's blades or a gauge's needle is drawn each frame, only in the
  spaces that hold one, while the rest of every part stays in the world's mesh.
- What a part does on its own face: give redstone out of it and into the block there, give a comparator
  reading, give light, hear the redstone reaching it, open a menu that closes when the part is taken off, and take a
  room of its own shape. A cable or a space of parts is ticked only while one of its parts needs it, so a world of
  plain cables costs nothing each tick, and redstone wire joins a cable only on a face whose part deals in redstone.

### Changed
- Telemetry leaves the peripheral links: the point-to-point links are the computer's peripherals and the
  industrial control link; telemetry becomes a line of its own, which J's Space lays.
- Two layers: what is the series' own (the eras and tiers, the data network's model, the Operations, J's
  Energy, the shared content) gets a package of its own, `dev.jstech.core.jstechseries`, and the rest of the Core,
  the platform any technology mod can build on, never reaches into it. What is new of the series is born there; what
  the Core already has of it moves there a domain at a time.
- Listeners declared: every class of the Core that answers the game's events is handed to the buses from the
  Core's constructor, through a `listen` method of its own, rather than found by an annotation, so what runs is what
  the constructor says.
- Integrations behind one guard: the work with Mekanism, FTB Teams, Accessories and Curios starts through
  `OptionalMod`, so one of them failing to start no longer stops the game.
- The style chosen for J's Computers' screens, or for every screen, is worn by its computing blocks' screens
  too, in place of their era's.
- A setting can be picked on a chooser of its own, which shows what each value means, from the settings
  screen.
- Using what others own: the permission that let a player pass every owner, `jscore.ownership.bypass`, is
  now four, `jscore.owned.use_others`, `open_others`, `set_up_others` and `break_others`, and changing a server's
  settings from afar asks `jscore.config.edit` rather than the operator level alone.
- Menus in every style: a style draws a menu's ground, its lists and its footer's line, and gives a menu's text
  colours of its own (headings, notices, links, warnings), each falling back to a window's; what is typed in a field
  takes the style's field colour in every style.
- The fixed catalogue of five metals in seven forms gives way to the open one. Iron dust, iron plate and copper
  plate keep their ids and are things of the open catalogue, drawn as the game loads instead of from textures of
  their own; their common tags come from the Core's data made as the server loads, no longer from its generated files.
- Loss by distance in millionths: a cable's loss is now counted in millionths of what crosses each block, so a
  line can lose as little as a hundredth of a percent over a hundred blocks; a cable that lost a tenth now says
  `loses(100_000)`.
- Machines short of energy work slower in the same proportion, spending what there is, instead of waiting for a
  whole tick's energy; looked at, they say they run on low power.

## [0.5.2a] - 2026-10-14

Codename: Boron.

## [0.5.1a] - 2026-10-14

Codename: Boron.

## [0.5.0a] - 2026-10-13 - The Start Me Up Update

Codename: Boron.

### API
- Versions: J's Core's API is at version 4 and J's Computers' at version 7. Everything this release
  adds to either, or changes, carries `@ApiStatus.Experimental`: it keeps the mark through this release and loses it
  when the next cycle begins. Each mod's API is now kept line by line, every type and member a mod can reach with the
  version that brought it, and the tests fail when the code and that list disagree; `docs/API.md` says how.
- Added: `GuideBlockRenderers` and `IGuideBlockRenderer` in J's Core's client API: what draws a mod's own kind
  of block on a manual's page, or a picture drawn as the page is shown, registered once from its client setup, handed
  the block's data as a tag.
- Added: a build can ask a language for an instruction set and a version of the language at once, with
  `IProgrammingLanguage.compile(sources, CompileOptions)`; a language without versions builds as it did for the
  instruction set alone, which is what the method does unless overridden.
- Changed: `IProgrammingLanguage.Complaint` carries its message as a `Text`, read in the language of whoever is
  shown it, with the `arguments` the message was written around; `text()` is the line a person reads, and `format()`
  is still the same line in English.
- Added: J's Core's client API, `dev.jstech.core.api.client`: `ISkin`, the look a mod draws through inside a
  machine's screen (moved here from the Core's own GUI package); `SurfaceRenderer`, what draws frames onto a surface
  of its size, asked no faster than its frame cap and only while it is shown; and the two surfaces, `PixelSurface`, an
  array of colours uploaded when it changed, and `GpuSurface`, a render target drawn into with the game's rendering.
- Added, outside the versioned API for now: the parts of a general library for technology mods.
  Progression axes (`ProgressionAxis`, `ProgressionAxes`, `PlayerProgress`, `ProgressionGate`) that the hardware
  eras and the industrial tiers are now two of; multiblock patterns from data files or code, with ports on their
  parts (`MultiblockPattern`, `MultiblockPatterns`, `PortKind`); chunk and region data with a spatial index
  (`SpatialIndex`, `RegionIndex`) and chunk loading by owner (`ChunkLoaders`); ores, structures and dimensions
  declared for data generation (`WorldGen`, `WorldGenProvider`); owners, access and teams (`Ownership`, `IOwned`,
  `CorePermissions`), with FTB Teams answering for the teams when it is installed; processing machines and their
  recipes (`MachineBlockEntity`, `ProcessingMachineBlockEntity`, `ProcessingRecipe`, `ProcessingKind` declared with
  `ModContent.processing`, `ProcessingRecipeBuilder`, `IUpgrade`, `UpgradeEffect`); the recipe viewers' bridge
  (`IKeepsViewersClear`); advancements (`EventTrigger`, `AxisStepTrigger`, `Advancements`, and the data generation's
  `AdvancementTab` and `ConditionalAdvancementProvider`, moved up from J's Computers); commands under `/jstech`
  (`SeriesCommands`); a GameTest kit (`ScenarioBuilder`, `GameTestPlayers`); HUD elements (`HudElements`,
  `IHudElement`, `HudStack`); holograms (`Holograms`); what a block entity says to Jade (`IDescribed`); what an
  entity wears (`WornItems`, `IWornSource`); dimensions made while the game runs (`RuntimeDimensions`) and the rules
  of each (`DimensionRules`, `DimensionRulesData`, `DimensionHazardEvent`); declared entities (`ModContent.entity`),
  vehicles (`VehicleEntity`, `VehicleSpec`, `DriverInput`, `IDriver`), robots (`RobotEntity`, `IRobotTask`,
  `RobotTasks`, `CoreRobotTasks`) and projectiles (`CoreProjectile`, `ProjectileSpec`, `Projectiles`); and the
  settings screen (`CoreConfigScreen`, `ConfigDraft`).

### Added
- Translation: Brazilian Portuguese (pt_br) for J's Core, J's Computers and J's
  Industrial: every word the mods show, from the blocks and items to the desktops, the programs, the terminals and the
  installers. The names of the fictional products and makers, the commands a player types and the files on a virtual
  disk stay as they are.
- Logos: Logos and icons for the series and for each mod. The mods list shows each mod's
  logo over the series logo, mod list screens that show an icon show each mod's own, and the READMEs carry the logos.
- Manuals: Manuals, for every mod to write in: a mod declares its chapter once, as entries of ready blocks
  (paragraphs, figures, numbered tables whose amounts are read from the code, the recipes the world holds, steps,
  warnings, problems with their fixes, words explained, links), and they show in its own manual and in any manual
  holding every chapter. A manual opens at its cover the first time and after that at the page it was closed at, turns
  its pages with the arrows, the keys or the wheel, numbers everything as technical manuals do (3.2.6, Figure 3-9, page
  3-14), builds its contents and its index, and searches the index as the player types. Holding M for a second and a
  half over an item in any inventory, or with the item in hand, opens its page in its mod's own manual, while a small
  bar fills in the item's tooltip, under the line that says to hold the key, or under the crosshair, in that manual's
  look. Styles are data a resource pack can replace; the Core brings a ring binder with navy covers. An entry follows
  five parts (what it is, what it is for, how to get it, how to use it, and what can go wrong) or reads as running text
  ending in what to do if something goes wrong. Its sentences lead to other pages in their own words, the link's number
  written after them; the items it talks about stand on a plate under its title, turning over a row at a time when
  they are many, each showing its tooltip and leading to its own page; and its pictures are images or drawings a mod
  makes as the page is shown, numbered with the figures.
- Manuals: The Technical Reference, the series' manual: a navy binder holding a chapter for every mod installed
  (the series, J's Core, J's Computers, J's Industrial), handed to each player once, the first time they join a world,
  above their hotbar, and in J's Core's new creative tab. Every item of the three mods is the page of an entry, in
  English and Brazilian Portuguese, and every number on them is the mods' own.
- Manuals: Manuals open each chapter on two facing pages, its number large in the chapter's colour with its
  title and what it is about, and its sections with their icons and pages; a page is left blank before a chapter that
  would open on a right page, as printed manuals do. Recipes show their time over the arrow, their energy under it, and
  the name of what they make. A mod writing a chapter can add notes, a block's three views with balloons and their
  legend, plans of blocks seen from above, and breaks to the next column or page; can make one entry the page of a
  whole family of items; can print a mark on a manual's cover and hand a manual to every player once; and can draw its
  manual as a binder or as a folder of drawings, in two columns, with its own palette.
- Manuals: A mod's manuals can be books made of chapters: a mod writes as many chapters as its books need, a
  book prints the ones it names in the order it names them and numbers them by that order, and one chapter can be bound
  into several books, a tab for each. A chapter written for one book only stays out of the Technical Reference, whose
  tabs are one a mod. A link to a page another book holds names that book, "(in The Handbook)", and a click opens it
  there; and the manual key over a stack whose page its mod chose, such as an installation disc, opens that page.
- Manuals: Tables in the manuals with columns of their own, such as a command and what it does. Their words are
  written small, and their commands, codes and figures in Misc Fixed 5x8, a fourth size of the terminal font the Core
  now carries. A word is never cut in two: every column is at least as wide as its longest word. A table whose columns
  cannot stand side by side, such as a list of error codes with what each means and how to fix it, lists each row under
  the one before. A table that fits on a page moves to the next page whole, unless that would leave more than half the
  page empty; one that parts carries its caption, marked "(continued)", and its head on to each page, and leaves at
  least three rows on either side of where it parts. A heading goes on with what it heads, never left alone at the foot
  of a page.
- Manuals: A mod can have its manuals read their figures from the mod itself as the book is opened, rather than
  print numbers written by hand: a figure in a sentence, written the reader's way, and the rows of a whole table, such
  as every part of a family. A part another mod adds, or a setting changed, is in the next book opened.
- Manuals: The manuals have Back and Forward beside the contents on their top edge: Back returns to where the
  reader followed a link, a tab, the contents or a line of the search from, and so do the mouse's back button and
  Backspace. A book opens again at the page it was closed at after the game is closed and opened. The search finds
  words anywhere in the manual, a command in a table or a word in a paragraph, and shows the line it found them on.
  Every button and arrow lights and is framed under the pointer, sinks while held and fades when it leads nowhere; the
  pointer turns into a hand over what takes a click; the page-turn arrows are bigger; and a book can hold thirteen
  chapter tabs, the shown chapter's standing out.
- Manuals: The manuals sound like the game's books: a page is heard turning, the book opening and being laid
  down, and the buttons and tabs click as the game's buttons do. A Manuals volume on the Sound Mixer turns them down
  apart from everything else.
- Manuals: Listings of code in the manuals: numbered, headed by their file's name, their lines numbered and
  coloured as the game's editors colour Sigma, Sigma# and the network's query language, a long line going on under
  itself without a word cut in two, at a space, a bracket, a comma or the point of a name such as Console.PrintLine. A
  Copy button puts the whole listing on the clipboard for the game's editors, and a click opens it over the whole book
  with no line broken.
- Manuals: Sessions at a terminal in the manuals, printed as each system draws its console: MC-DOS white on
  black, a Linux console with its green prompt, the Arch Linux media's red root prompt, FreeBSD's sh on its warm dark
  ground; the prompt, what was typed and what the system answered each in its colour. Keys named in a sentence, such as
  Ctrl+S, are drawn as keys.
- Manuals: A glossary at the back of every manual, before its index: each word a page explains, alphabetical
  under its letter, with what it means and the page that explains it. The first time another page uses one of those
  words, the word leads to where it is explained, unless it is part of a name (the Network Manager) or of a path or a
  name in code. A title too long for its line in the contents, on a chapter's opening pages or in the index runs on
  under itself, its page beside its last words, rather than being cut short; a name too long for any line, such as a
  setting's, breaks where its points and underscores are; and a link to a page of another book reads that page's
  title.
- Manuals: Scenes in the manuals: a few blocks drawn on the page as the game draws them, block entities and
  GeckoLib models included, which the reader turns by dragging and brings nearer with the wheel, with Reset to go back.
  Numbered balloons point at their parts, a legend under the scene says what each is, and pointing at a number outlines
  its part and names it. A multiblock can be shown layer by layer to see inside it. A scene is framed round its blocks
  wherever they stand, is drawn shorter when a page cannot hold it with its caption, draws its machines as a client
  sees them (lamps lit, parts in their seats), and can show a block of another mod, which stays out of the scene when
  that mod is not installed. A cable's plugs are chosen from the world the cable is drawn in, so a cable in a scene
  ends in the plug of the device beside it there.
- Manuals: Manuals bound as the books of their time were, not only in binders: a paperback's glued spine, a
  hardback sewn in sections, or a wire coil through holes down the middle; boards covered in cloth or flecked kraft card
  round the open pages; plain or squared paper with a red margin line; chapters found by a thumb index printed down the
  pages' edge or by coloured paper flags stuck under it, besides a binder's tabs, in each book's own colours; and a
  bookmark ribbon that lies across the pages the reader left open and hangs out under the book. A book's cover is drawn
  from the parts its style lists: panels, frames, pictures, its title and lines in any size, a rubber stamp, cloth,
  kraft card, a wire coil and the holes punched for it.
- Settings: One way to write a settings file, for any mod built on J's Core. A mod declares a file once (its
  name, whether it is a player's, common to every game or a world's, its format and the version of its layout) and each
  setting in it: a flag, a number held to a range, a word held to a list, or anything with a codec, with the comment a
  person reads above it. Files are written in TOML, JSON, JSON5, YAML or NBT, with their comments, ranges and defaults
  wherever the format has room for comments. A TOML file is NeoForge's own, so NeoForge keeps it in step with the
  players and reads it again when it is edited; the others the Core reads and writes whole. Every file carries the
  version of its layout: an older one is upgraded by the steps its mod declares before it is read, one from a newer mod
  is read and never written over, and one that cannot be read is kept aside as `.unreadable` while the defaults are
  used. YAML is read through SnakeYAML 2.7, which J's Core now carries inside its jar, with its safe reader only, so a
  file can never name a class for the game to build.
- Settings: The settings screen of the series' mods, opened from the mods list for J's Core and J's Computers,
  in its own look: a tab for the world's settings, one for the player's and one for every game's, each with a line
  saying where they are kept; each section with its count of settings, a mark while one of them is changed and not
  saved, and a line saying what it is for; a card for each setting with its whole description (the rest where the
  pointer rests on it), its default, its bounds and its unit, and a switch, a number to type or step, a word to go
  through or a text to type; a Default button on a setting that is off its default; a search through every section by
  name and description; and how many changes wait, beside Save and Cancel. A world's settings are changed from inside
  it, and opened from outside it are shown dimmed under a line saying why. Every setting and section has a name of its
  own, short enough to be read whole, with its comment as the tooltip; a mod built on the Core names each setting and
  section where it declares it, and the names go into the mod's English language file with everything else.
- Saves: Saves that carry the version of their layout, for any mod built on J's Core. A mod declares once, for
  each kind of thing it saves (a block entity's tag, an attachment, a value saved whole), the version of its layout and
  the steps that bring an older save up to it, and every save then carries the version it was written in. A save from
  before there were versions is read as the version before the first; one written by a newer version of the mod is
  read as far as it can be, and the log says so. Settings files count their versions the same way.
- Saves: States, for any mod built on J's Core: a value a mod keeps with a world for the whole server, for each
  dimension, for each player (read and changed whether they are online or not) or for each team. A state is declared
  once, with its codec, its default and the version of its layout, and saved with the world in a file of its own. A
  synced state sends each player the value that is theirs to see: the server's to everyone, a dimension's to whoever is
  in it, a player's to that player alone, a team's to the players on it. Teams are the game's scoreboard teams unless a
  mod hands the Core a source of teams of its own. A state's file saved by a newer version of its mod is read as far as
  it can be, and the file as that version left it is kept beside it once, as `<name>.newer-v<N>.dat`.
- Blocks: Blocks that know their neighbours: a block declares once, as ports, which lines it takes on which of
  its faces (its back, its front, every face), named from its own point of view so they turn with it, and in which
  generations, a port taking its own and every earlier one. A cable that shows a connection and the device that joins
  through it ask the same ports, so they never disagree. A block entity hears which of its faces saw its neighbour
  change, and a block's faces can tell which of the eight blocks around them in their plane they join, for textures
  that run across many blocks.
- Blocks: Multipart blocks: thin parts mounted on a block's six faces, one a face, whose kinds any mod registers
  with the Core, and a bundle of up to nine wires through the block's middle beside them, each wire a line in one
  generation and one colour or none. A block keeps its parts and wires as fields of its block entity; they are saved
  whole, and the players who see the block are sent only what draws it. Which part a player points at is worked out
  the same way for every block. J's Computers' buses are now parts of the Core's. A part can have a model for while it
  is at work, its lamps lit and blinking by an animated texture; the block is drawn again only when the part starts or
  stops working.
- Blocks: Models put together as the game runs: a multipart block draws its parts and the pieces of its wires on
  top of its own model, and a block can have faces that run on into the blocks it joins, drawn from five tiles a
  quarter of a face at a time. Both are drawn into the world's mesh, built again only when a block changes.
- Blocks: J's Core gives a block entity its state as fields declared once, each saying where it goes: into the
  save, to the players who see the block, to the menu open on it. The saving, the update the players are sent, the
  client's reading of it and the menu's data all follow from those declarations, and the players are sent one update
  per block entity a tick however many fields changed. Inventories, energy stores and fluid tanks are declared the same
  way, with whether pipes and cables reach them and whether they spill when the block is broken; so are a value of any
  kind a codec writes, a part that writes itself, a property of the block's state kept at what a value says, a
  peripheral's link to the computer at the other end of its cable, and what happens when the block is broken.
- Blocks: A block declared with `Drops.SELF_WITH_CONTENTS` drops its item carrying what its block entity keeps,
  as a shulker box keeps its items, and is placed again as it was.
- Menus: J's Core declares a menu once: its slots in groups, placed from the screen's layout; where a
  shift-click in each group sends the stack; its buttons; and that it stays open while its block entity stands and the
  player can reach it, which holds for every variant of a block that makes the same block entity; the values it shows
  that the server works out; and the screen that draws it, which frames its slots and presses its buttons. A block that
  makes a block entity is declared the same way, with what it ticks and which menu it opens, and faces whoever placed
  it. A menu's player inventory can be shown on some pages of a screen and hidden on the others.
- Menus: J's Core registers every payload a client sends with a gate that checks, on the server, that the player
  really has that screen open on what the payload names; a payload that acts on the menu the player has open is handed
  that menu, already checked, and a refused one is dropped and noted in the server log.
- Menus: The menus of J's Core write an item's keys at the right of its row. A tab strip can have tabs that
  cannot be chosen, drawn dim with a word after them, and tabs that do not fit their strip give up some of their
  padding instead of running off its end.
- Grids: One grid of each kind (power, fluid, heat, gas, motion, data) in each dimension, shared by every line
  of that kind. Cables of one line join when they are of the same generation and their colours agree (the same colour,
  or either in none); devices such as routers join every line. The slowest cable on the best way between two places,
  and the length of each run of one cable against how far it reaches, are worked out when the grid changes and kept
  until it changes again, never on every tick. Only the data grid carries the identity of a network; the data
  network's index is now the data grid.
- Cables: The shared cable block: every mod's cables are laid in one block, each line in a lane of its own on a
  grid of three by three, and the wires of one block never join one another. A wire crosses a face alone in the middle
  and beside others in its lane; where a wire changes place, or two would cross inside the block, the block becomes a
  junction box the wires enter and leave each in its place. A cable dyed one of the sixteen colours wears a ring of it
  on every block and joins only its own colour and the uncoloured, which joins every colour. A cable that never shares
  a block, as a long-distance line does, holds its block alone. Using a cable on a cable block lays it there when its
  lane is free, and against the face clicked otherwise or while sneaking; breaking takes out only the wire or the part
  looked at, the block going with its last piece; a dye colours the wire looked at. A mod declares its cables with its
  content, each with its line, its lane, its thickness, how much it carries, how far it reaches, its jacket and the
  plug it ends in where it meets a device that takes it, and what the cable is for and the era it belongs to, which its
  item's tooltip says, the era in that era's colour. A cable block the player's game has not yet been told the wires of
  is outlined as a wire's core, so laying a cable never flashes a whole block's outline.
- Cables: Cables that keep a shape: a cable can be declared to run only straight or to join at most so many faces
  in a block, and it keeps the joins it already has when another cable is laid against it.
- Network: The data network knows the slowest cable between any two points of it, for any mod or addon to ask:
  of every way between them, over cables and routers and through the Mainframes and racks that join cable runs, it takes
  the one whose slowest cable is fastest, which is as fast as data can go between them. A router slows nothing down,
  and a Mainframe that is switched off passes nothing through. It also tells what lies cut off beyond a run too long,
  how long each run is, and which cables a device joins.
- Peripherals: Port kinds for peripherals: an owner has so many ports of each kind and a peripheral takes one of
  its own; an owner that loses ports unlinks the peripherals it linked last. A peripheral can offer its owner more
  ports, as a hub does, and the link validator finds each peripheral's way through the hubs linked to its owner,
  keeping with every link the hub it hangs from. A cable can end in a different plug for each kind of port, and the
  peripheral links follow the peripheral wires of the shared cable block, each run as far as its cable reaches.
- Energy: Energy units: FE is registered as the unit `jscore:fe` in a new synced registry of energy units, where
  a mod adds its own with what it is worth in FE: an exact ratio of two whole numbers, rounding down so no energy is
  made out of a rounding. An item can hold energy in the `jscore:energy` component, given the game's energy capability
  with `CoreEnergy.holds`.
- Energy: The energy grid moves energy. An energy wire plugs into every block beside it that offers the game's
  energy on that face, and each tick generators feed consumers, what they have left fills storage, and storage feeds
  what consumers still want, each share in proportion to what is wanted. A cable can lose thousandths of what crosses
  it, and energy takes the way that loses least, worked out once while the grid keeps its shape.
- Fluids: Fluids declared once: a mod declares a fluid with its name, its textures and tint, how hot, heavy and
  thick it is and how bright it glows, and the Core registers its type, its still and flowing fluid and, for a liquid,
  the block it pours as and its bucket, drawn with the fluid inside; the generator writes their names, the bucket's
  model and the fluid's tags. A gas is lighter than air, never poured into the world nor held in a bucket, only kept in
  tanks; it is tagged a gas for the Core and for other mods. A fluid can be marked corrosive.
- Fluids: Pipes: a cable of the fluid grid plugs into every block beside it that holds fluids. Pipes are
  passive: a connected run moves, each tick, at most what its slowest pipe carries, which is the pressure it holds, and
  only the fluids every pipe of it is made for (the temperatures it stands, and whether it takes gases and corrosive
  fluids), leaving the rest where they are. An output feeds the inputs, what it has left fills the tanks, and the tanks
  feed what the inputs still want, each share in proportion to what is wanted, nothing lost on the way.
- Items: Items with state. An item says where it is declared what it holds besides itself: modes it switches
  between, energy, one fluid, stacks, and components it starts with (a mod declares its own components with its
  content). The Core gives it the game's capability for each, keeps each in a component that is saved and sent with the
  item, makes it stack alone when it keeps anything inside, and lists what it holds in its tooltip. An item that holds
  stacks takes no other item that does. The new Change Item Mode key moves the item in the main hand on to its next
  mode, or back one with shift held, and says the new mode on the action bar; it has no key until the player gives it
  one.
- Inventories: Item filters that let pass only what they list or everything but it, each rule naming an item
  exactly (a worn tool is not a new one), an item whatever its damage and components, or a tag, and carrying an amount a
  bus can read; what each face of a block lets through (closed, in, out, or both), named from the block's own point of
  view so it turns with the block; and moves of items, fluid and energy from one store to another that never lose
  anything on the way, into an item's own inventory as well.
- Inventories: Batches of moves that go through together or not at all: each step is asked first whether it
  would move all it wants and nothing moves when any would come short; when a store takes less than it promised as the
  batch moves, everything already moved is put back, last first. Steps come ready for putting items into and taking
  them out of an item store (put back slot by slot), filling and draining a fluid store, and giving and taking energy.
- Data: Content from datapacks. A mod declares a registry of JSON files, every file under
  `data/<namespace>/<folder>/` one value read with its codec, read again whenever the server reloads its data; a file
  that does not read is left out with a line in the log saying why. A synced one is sent to every player as they join
  and after every reload, and their game keeps it apart from the server's. A mod can also declare a registry datapacks
  fill, sent to every player with the world's registries.
- Data: Large values sent in pieces: a mod declares a kind of value with its codec, and the Core writes it, packs
  it, sends it in pieces either way (the server a share of each player's pieces every tick, so a large sending never
  holds the others up) and puts it back together on the other side. A side keeps only so many sendings from one sender
  open at once and refuses pieces out of order, sendings larger than allowed and values that unpack into more than
  allowed. The sound system's recordings are cut and checked by the same pieces.
- Ticking: A tick scheduler: a block that runs many things in a tick shares the tick's work among them in turns
  of a fixed size, gives what one leaves to the others, and stops at a deadline by the clock, the next tick starting
  with the first that went without. J's Computers' machines run their programs with it, as they did with their own.
- Keys: Key actions: a mod declares a key once, with its name, the key it starts on and what the server does
  when it is pressed; the player's game makes the key binding from it and sends the press to the server by itself. Turn
  Off Last Sound and Change Item Mode are key actions, so a key a player gave them stays given.
- Debug screen: The series on the debug screen (F3): each part of the series adds a panel of lines under its
  name, the sound system's first, and the pieces of work the series times are listed with their average and longest
  time over the last hundred runs, the energy grid's tick among them.
- Calendar: The world's calendar: the day, the hour and the minute, the day of the week and the week, the season,
  the day of the season and the year, all read from the world's own clock. A year is four seasons (spring, summer,
  autumn, winter) of the same number of days, 28 unless the series' balance file says otherwise. J's Computers' cron
  and `at` jobs and its desktop clock read the time through it.
- Models: Machines drawn with GeckoLib: a mod says once how a family of them looks (which model each is drawn
  with, by era or by kind, the folder of their textures and the animation they share) and declares it with the blocks,
  and one model of the Core draws every family, where each used to need a model class of its own. The Core runs
  without GeckoLib; a mod that draws with it brings it. J's Computers' Mainframes, racks, Pattern Encoders and drives,
  and the items that show them in a slot, are drawn this way.
- Screens: J's Core can show a picture in the world drawn from anything a screen can draw, into a texture of its
  own, as often as the viewer's distance calls for, and paint it, or the player's own screen, as a monitor's tube would;
  and it sends what a thing in the world shows only to the players near enough to see it, only when it changes.
- Fonts: Fonts of their own for every mod built on J's Core. A mod declares a font once, with its source, its
  cell, its baseline, its licence and who made it; the data generation reads the source, a BDF bitmap font (the first
  format read), and writes the picture and the font file the game draws from, with the game's font for every character
  the font lacks. J's Core carries Misc Fixed, the fixed font of the old Unix terminals, in the public domain, in three
  sizes: 6x10 with nearly sixteen hundred characters, and 9x15 and 10x20 with several thousand, for any mod's
  terminal-like views; the mods list credits the font on J's Core's page and on J's Computers'.
- Fonts: A grid painter puts text on a monospace grid, every character in a cell of its own, in a declared font
  or the game's: characters the font lacks are drawn in the game's font in the middle of their cells, and the box lines
  and blocks are drawn by the painter to fill their cells, so frames and bars join from cell to cell and from row to row
  at any spacing. `TextScreen` is a text-mode screen of cells, each a character with an ink and a ground, in the colour
  adapter's sixteen colours or any other, with boxes in line characters and the shadow a dialog of that age threw.
- Motion: Motion, for any mod built on J's Core: curves written as CSS and the desktops' own toolkits write them
  (`ease-out-expo`, `cubic-bezier(0, 0, 0, 1)`, `steps(16)`), a motion of each kind for each system (growing, sliding,
  going down to a place and back, an outline travelling there, a flat colour giving way), read against one clock that
  runs smooth between ticks. A mod declares each system's profile once; the data generation writes it to
  `assets/<mod>/motions/<system>.json`, where a resource pack puts its own to slow a system down, speed it up or keep it
  still.
- Motion: For any mod's screens: a thing can fade in or out as one picture (`FadeLayer`), a region of the screen
  can lose its colour part of the way or all of it (`GreyFilter`), the motions that go round and round (a cursor
  blinking, a throbber turning, a bar with no known end) are read by `Rhythm`, a screen can draw everything inside it
  within one system's motions (`MotionScope`), a `WaitBar` runs the bar of whichever system it is drawn on, a
  `ProgressBar` glides on a system whose bars do, and a skin can give its progress bars a colour of their own. New
  styles of motion (appear, caption, blink, loop, blocks, segment, grow, bounce, ease, grey) and new kinds (tooltips,
  notices, the dim behind a dialog, the text cursor, a progress bar's fill and wait, the busy sign, the pointer's busy,
  working and launch states, and a copy's animation).
- Sound: A sound system for the whole series and its addons. A mod declares a sound once (where it is heard from,
  whether it loops, its channel, its files, how far it carries, its subtitle) and its registration, `sounds.json` entry
  and translatable subtitle follow from that one line. Sounds are mixed in channels (machines, devices, interface,
  alerts, ambience, music, voice) with a volume each, and any sound of the game, not only the series', can be turned off
  in `config/jstech-audio.json`. The same sound from the same place plays once in a short while, however many times it
  is asked for.
- Sound: The sound system keeps the world's running sounds: a machine says what it wants heard (a fan, a disk) at
  the volume and pitch its state calls for, and the client starts, retunes, fades and stops the sounds to match. It
  keeps to a share of the game's sound channels, the sounds that matter most and are nearest first; many machines of
  one kind close together are heard as one room, louder the more there are; and walls between a sound and the listener
  muffle it, which the player may turn off.
- Sound: The sound system plays sounds made as they play: a synthesiser (square, pulse, triangle, sawtooth, sine
  and noise, note by note, the way a PC speaker or an early sound card makes a tune), with two voices more, the bright,
  metallic ring of frequency modulation and the rounder note played from a table of harmonics, which a sound device
  chooses between; and recordings read from files that are not the mods' own, WAV and Ogg Vorbis to begin with and any
  other kind a mod registers a decoder for. The server sends a tune as its notes, heard by the players near where it
  plays, and each client makes the sound itself; a tune can be stopped before its end. Such a sound has a subtitle and
  a channel like any other and the player can turn it off the same way; heard from a place in the world, a stereo
  recording is played in mono so it comes from that place.
- Sound: A sound can come out of several places at once, each playing one side of a stereo recording or both,
  and each as well as the speaker there reproduces it: no faster a sample rate and no more bits than it manages, with
  its bass and treble cut where it cuts them. A mod marks a recording as stereo, and a sound device's own limits (a card
  that plays in mono at 8 bits) apply on top of the speaker's.
- Sound: A "Turn Off Last Sound" key in the game's controls, under J's Tech Series, with no key until the player
  picks one: it turns off the last sound heard around the player (never their own footsteps or a click of the screen)
  and says which on the action bar; pressed again within five seconds, it brings that sound back.
- Sound: While an alert plays, the series' other sound channels are lowered so the alert is heard over them, and
  come back up over a second once it ends; the player may turn this off in `config/jstech-audio.json`. A player who
  turns on alerts on screen sees each alert as a sign at the top of the screen, saying what it is and pointing to the
  side it comes from the way the game's subtitles do; it blinks three times as it comes up and goes after four
  seconds.
- Sound: Sound cues: a mod says what happened (a computer powering on) and the sound heard is picked on each
  client from `assets/<namespace>/sound_cues/`, by the context it happened in (the machine's era, its audio device, its
  system's family). A resource pack binds a cue to other sounds, the game's own included, by shipping a file of the
  same name. A machine with sound hardware of its own plays cues and tunes from each of its speakers, at its own volume,
  as its device allows: a PC speaker turns every note into a square wave, and a machine with no sound hardware stays
  silent.
- Sound: The Sound Mixer, reached from Sound Mixer... beside Done in the game's Music & Sound Options. Its
  Channels tab has a slider for each channel (machines, devices, interface, alerts, ambience, music, voice), with a
  tooltip saying what the channel carries and which of the game's volumes it also follows, and moving one reaches the
  sounds already playing; a song or a loop playing while its channel is turned all the way down goes quiet and is heard
  again when the channel comes back up, where the game would have stopped it for good. Its Sounds tab lists every sound
  the game knows, the game's own and every mod's, searchable by name or id and filtered to all, recent, the series', the
  game's or those turned off, each with Play to hear it once and ON/OFF to turn it off, and Turn All Back On. Its
  Options tab turns walls muffling sounds, alerts shown on screen and the lowering under alerts on and off, and opens
  the game's controls to pick the Turn Off Last Sound key. The debug screen (F3) shows the sound system under Sound
  Mixer: the running sounds out of their budget, the rooms many machines make, what walls muffle, how far the other
  channels are lowered under an alert, the last sound heard and how many sounds are turned off.
- Sound: Recordings a server keeps, for any mod or addon to play. A player can bring a WAV or Ogg Vorbis file
  from their own computer to the server, which keeps it once under a name taken from its bytes, however many bring it,
  and reads its length, its title, artist and album, and the picture an Ogg Vorbis file or a Wave file's ID3 tag
  carries; whoever takes a recording says whether it was put to use, and the player is told it went in only when it
  was. A recording played in the world is fetched by the players near it, kept in a cache on their own computer
  (`jstech/media-cache`, 512 MB at most, the longest unplayed going first) and heard out of the places it plays from,
  from wherever it has got to: a player who walks up to one already playing hears it from there, and one who walks away
  lets it go. It can be paused, taken up again and stopped. The server owner sets how fast recordings go to each player
  and come from them, and how big a file the server takes, or that it takes none, under `media` in
  `jstech-balance.toml`. The server remembers who brought each recording and when anything last used it: the recordings
  one player brought may take up to `player_quota_megabytes` of it together (512 unless the owner says otherwise, a
  recording it already kept costing nothing), `/jstech media` tells an operator how much it keeps, and `/jstech media
  prune <days>` takes out every recording nothing has used for that many days, leaving what a mod still offers and what
  is playing, and gives each player back the room theirs took.
- Machines: J's Core shows every processing machine's recipes in JEI and in EMI, under the machine's name with
  the machine beside them, items and fluids, the time and the energy a tick; a computer's monitor keeps both viewers'
  lists beside it in either. Jade shows what a J's Core machine is doing ("Working: 40%", "Idle") and whose it is.
- World: Dimensions: a mod declares one and the world has it; copies of it are made and taken away while the game
  runs (`/jstech dimension create` and `remove`) and come back each time the server starts. A dimension declared only to
  be copied adds nothing to the world itself, which the game then opens without its warning about experimental
  settings. Each dimension has rules, read from a datapack: how hard it pulls, whether its air can be breathed, how hot
  or cold it is, its weather and its pressure. A player choking, burning or freezing there is spared by whatever another
  mod makes for it.
- World: Commands under `/jstech`: `progress` to see or set where players stand along the hardware eras and the
  industrial tiers, and `chunks` to see and let go of the chunks their machines keep loaded. Machines can keep chunks
  loaded for their owners, up to the "Chunks loaded per owner" setting.
- World: Holograms, words floating in the world turned towards whoever reads them, and HUD elements that share
  the corners of the screen, for the series' mods to show things with.
- Modpacks: A mod built on J's Core can hand a scripting mod's scripts a class of its own under a name, declared
  once with the rest of its content; J's Core's plugin gives KubeJS every one.
- Documentation: J's Core's documentation, in its `docs` folder, explains each part of the library to
  programmers, from adding it to a project to machines, multiblocks, networks, Operations, energy and fluids, cables,
  the world and dimensions, saved state, ownership, progression, commands, entities, overlays, settings, sound, screens
  and testing, with what can go wrong at the end of every page; the pages on fonts, motion and UI components moved
  there. The API's own documentation in the code explains, with examples, what a network category, an Operation and its
  handler, a language, an instruction set and a kernel are. Its README lists what the library holds and says in plain
  words that anyone may use the Core in their own project, open or closed, free or paid, with credit, and what its
  licence, the LGPL 3.0, asks in return.
- Design: The design of J's Core, in `core/docs/design`: what the library is (the platform any technology mod
  needs, the model the series' mods share, and the content several of them need as one), and one page per subject, from
  the platform, progression, the API, materials, states of matter and energy to cables, redstone, multipart, conveyors,
  machines, mechanical power, heat, multiblocks, networks, the world, hazards, explosions and fire, sealed rooms, moving
  structures, entities, tools, the interface, models, lighting and effects, manuals, modpacks, the tools for those who
  make mods and the low level; each page says what is built, links to the programmers' documentation, and ends with
  what is designed and still to be built, and the first page gives the order of building. J's Computers' design follows
  it: its programming languages move to its own API, and its numbers to its own settings file.

### Changed
- Integrations: The bridge to Mekanism's chemicals lives in J's Core now, still behind its guard:
  every mod of the series reaches Mekanism's gases, infusions, pigments and slurries through the Core, and none needs
  Mekanism to run. A check of the sources keeps every optional mod (Mekanism, JEI, EMI, FTB, ComputerCraft) named only
  inside an integration, and out of the class that starts it.
- Saves: What the series kept with a world in files of its own is kept as states, each
  file carrying the version of its layout: the data networks of each dimension and the awards players earned while
  away stay in the files they were in, and the ledger of a world's recordings moves from the text file beside them into
  the world's saved data, as `jstech_media_ledger.dat`. A world saved before is read as it was; the text ledger is read
  once and goes once the world has been saved with it.
- Saves: Everything else the series saves carries the version of its layout too: every
  block entity of J's Core, J's Computers and J's Industrial, the networks a chunk is on, the operator of a machine,
  the programs a player first ran and the store of what the drives hold. A save from before is read as it was, and
  saved again with its version.
- Settings: The series' settings files are written by J's Core's settings files:
  `jstech-balance.toml` (the balance of the Operations engine, the programs' time and the recordings),
  `jscomputers-server.toml` (the computers and Soundfoundry) and the player's `jstech-audio.json`. They keep their names
  and their settings, so nothing set in them is lost; each now says the version of its layout, and every balance
  setting has a comment saying what it does along with its range. J's Core's "Operations and programs" section is now
  "Engine and programs". A sound preferences file with one volume written wrongly keeps the others.
- Creative tabs: A mod built on J's Core has its creative tabs side by side in the order it declares them.

### Removed
- Autocrafting: The machine picker of the Pattern Studio, and with it, in J's Core, a block's list of
  the recipe types it is the machine for and the data files that gathered them: a machine recipe belongs to the
  interface of its machine.
- Manuals: The unused hook for the Modonomicon guidebook: the series' manuals are drawn by J's Core
  itself.

### Fixed
- Interface: Clicking another title while a menu of a menu bar is open opens that one; a checkbox changes only
  on a left click; clicking into a text field puts the caret where it was clicked; the last tab of a tabbed screen
  takes the pixels left over, and a click there selects it; a dialog's answer can open another screen.
- Interface: A compact number never shows as 1,000 of one scale: 999,999 FE is 1M FE, not 1,000K FE.
