# Changelog

All notable changes to the J's Tech Series are recorded here, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); version numbers and phase letters follow
[docs/RELEASING.md](docs/RELEASING.md). From 0.5.0a on, every entry opens with the mods it concerns and its
subject, as in "(Computers) Racks and servers:", and each mod has a changelog of its own written from this one:
[J's Core](core/CHANGELOG.md), [J's Computers](computers/CHANGELOG.md) and
[J's Industrial](industrial/CHANGELOG.md).

## [Unreleased]

### API
- (Core) The promise: J's Core's API is at version 5. Beside its api packages, `JsCoreApi.PROMISED_PACKAGES` lists
  the packages of the Core that are part of the API, each a kit a mod may build on; a kit enters the list once it is
  documented for a reader who knows nothing of it. Every part of a mod is internal, experimental or stable, and a part
  is stable only once two real mods use it, it has real tests, an example that compiles, what it saves and sends
  defined, and a whole cycle without changing shape; `@ApiStatus.Experimental` comes off then, not on a date. The
  test of the API follows every name a public or protected signature says to where it comes from, so the API can hand
  a mod no type it does not promise; `docs/API.md` says how.
- (Core) The first kits promised: J's Core's API is at version 6, and four of the Core's packages join the promise,
  experimental: `dev.jstech.core.call` (calls from a screen to the server), `dev.jstech.core.task` (work away from
  the tick), `dev.jstech.core.codec` (codecs for records) and `dev.jstech.core.optional` (other mods behind a
  guard). What is marked `@ApiStatus.Internal` in them is public only so the Core can wire itself up, and is no
  promise.
- (Core) Looks that draw everything: J's Core's API is at version 7. `ISkin` draws a whole window, a slot, a check
  box, a progress bar, a separator, a tooltip and a button that cannot be pressed, and draws and measures text in its
  own font, each with a default that keeps a skin written before as it was; all experimental.
- (Core) The richer controls in every look: J's Core's API is at version 8. `ISkin` draws a slider, a switch, an
  option button, a drop-down list and its open list, a chart's ground, lines and series, and gives stored energy its
  colour, each with a default for a skin written before; all experimental.
- (Core) Fields in every look: J's Core's API is at version 9. `ISkin.fieldText()` is the colour of what is typed in a
  field, the primary text unless a look's fields differ; experimental.

### Added
- (Core) Errors that teach: when something a mod declared cannot stand (a block with no name, a setting whose
  default is outside its range, two things under one id), the game stops loading with one paragraph saying which
  mod, which declaration, what is wrong and how to fix it, from the content builders, the settings, the registries
  and the data generation alike.
- (Core) A data run that checks itself: ending a mod's data generation with `check()` makes the run fail, saying what
  to add, when a block or an item the mod registered has no English name, no block state, no model, a model whose
  texture is nowhere, or no loot table while it drops something. The Core's own run does.
- (Core) The log by system: each part of the Core writes to the log under a name of its own (`jscore/media`,
  `jscore/saves` and the rest), any mod can declare its own, and `/jstech log` lists them and sets how much each
  writes while the game runs, for the server's administrators.
- (Core) Live reload in development: a development run watches the `assets` and `data` folders its mods are loaded
  from and, once changed files stay as they are for a moment, reloads the player's resources and the server's data
  packs by itself. A file whose name carries a run's mark (`LiveReload.ownFile`) is that run's alone, so runs that
  share the folders, as test clients side by side, do not reload for one another's. A packed game watches nothing.
- (Core) Capabilities of your own: `CapabilityKinds.declare` makes a capability for blocks, items and entities in one
  line, and `CapabilityCache` keeps a neighbour's answer between ticks, worked out again only when the game says it
  changed. A part on a cable answers capabilities on its face by overriding one method, and any block that holds
  parts gets the same with one line.
- (Core) Calls from a screen to the server: a call is declared once with what it carries and what the server does
  with it, and the server handles it only while the player still has that kind of menu open and in reach, all in
  one payload the Core registers.
- (Core) Work away from the tick: `Tasks.onServer` runs long work on a virtual thread and hands its result back on
  the server's thread, within 2 ms of each tick; whatever is still out when the server stops is cancelled.
- (Core) Codecs for records: a record's codec is written one line a component, with no limit on how many, its
  names checked against the record's as the game loads, and the codec it travels the network with is taken from it.
- (Core) Other mods behind a guard: `OptionalMod` starts the work with another mod only when it is installed, and
  leaves it out with a line in the log when it fails to start; `OptionalDependencyCheck` lets a mod's own unit test
  fail on an import of the other mod outside its adapters.
- (Core) Screens declared in files: a screen is a JSON file in a resource pack saying what it holds and where (rows,
  columns, grids, panels, titles, labels, buttons, slots, the player's inventory, bars, pictures, check boxes,
  fields), and its Java keeps only what it does. The menu's slots are moved under the frames the file placed. An
  edited file rebuilds the open screen at once, and one that cannot be read says what is wrong in its place.
- (Core) A layout engine: rows and columns that share their room and give it up down to their smallest sizes, wrap
  and justify; grids of fixed, fitted and shared columns; stacks. Every size is a whole pixel, the shares add up, and
  the overlap check runs on the layout the game draws.
- (Core) Values a screen shows by name: a menu declares a value under a name and a screen's file binds to it, with
  no packet written for it; a value of any kind its codec writes, a number past 32767 included, is sent as the menu
  opens and then only when it changes.
- (Core) Styles the player picks: everything a screen declared in a file draws comes from a style, chosen under This
  player for every screen and per mod, with a preview. The Core brings the game's own (the default, drawn from the
  game's widgets, so a resource pack that repaints them repaints the Core's), J's Tech, high contrast, three for
  colour blindness and the eras'; a resource pack replaces any of them or adds its own as a file.
- (Core) New components, in code and in screens' files: sliders free or in steps, switches, option buttons, drop-down
  lists, a colour picker; trees, tables that sort and filter, line, bar and pie charts, a node graph editor; a 3D view
  of a block, an item or an entity, a map in the game's colours, tank gauges in the fluid's own texture, segmented
  energy bars and ghost slots; and tooltips whose lines hold items and pictures.
- (Core) Settings for every mod: the settings screen, rebuilt for any mod on the Core, is drawn in the player's style
  (the game's own by default: its tabs, buttons, sliders, fields and lists over the blurred world). Each kind of
  value has its control: sliders with the number typed beside them, drop-down lists, a colour picker, editors of
  lists and maps, and a picker of items, blocks and fluids with a search. Groups fold away, the search looks through
  every tab, a filter shows only what is off its default, a setting has a button back to its default, Undo and Redo
  take changes back and again, a description shows its picture and a link into the manual, and the keyboard reaches
  everything. A name too long for one line, on a small screen or in a longer language, takes two.
- (Core) Settings that say when they take hold: a setting takes hold at once, on a reload or on a restart, and the
  screen marks it and counts the changes that wait for a restart. A setting can be active, or shown, only while
  another is on, and rules between settings say what is wrong over them and keep Done from saving it.
- (Core) A server's settings from afar: a world's settings reach every player as they join and whenever one changes.
  An operator changes them on their own game's settings screen, the server checking the permission; other players
  see them, read only. `/jstech config get`, `set`, `reset` and `preset` change them by command.
- (Core) Presets and settings as text: a mod ships named presets with a file, a player saves their own and a server
  owner picks one. Export writes a file's settings as text to share, and Import takes them back. A world's settings
  file that does not exist yet starts as the modpack's copy in `defaultconfigs`, whatever its format.
- (Core) Told of a change: a mod is told when a setting changes, at once, wherever the change came from.
- (Core) One question before changing the world: every mod of the series asks the Core whether an actor may do
  something to a place (break, place, use, explode, burn, flow, move into, shoot), and every protection installed
  answers: the game's spawn protection, FTB Chunks and Open Parties and Claims when they are there, and any a mod
  registers. A machine, a robot or a moving structure is weighed as its owner, online or not, never as a generic fake
  player; the world's own work stays inside the claim it comes from. Robots ask before they break a block.
- (Core) Permissions for everything: every command of the series, opening, using, setting up and breaking what others
  own, changing a server's settings from afar, building multiblocks and going past the limits have a permission
  node, which LuckPerms and FTB Ranks grant or deny; without them the operator levels apply. `/jstech dump permissions`
  writes them all out, with two LuckPerms groups ready to paste into the console.
- (Core) Trust by name: the owner of a thing trusts players by name to use it, to set it up as well, or to break it as
  well. An owned block or entity is kept from whoever its owner does not let break or use it.
- (Core) Limits per player: how many of a kind of thing one player may have at once, kept with the world, starting
  with robots (16 each by default).
- (Core) The audit log: when the server keeps it, who used, changed or broke what another player owns is written down,
  and `/jstech audit` reads it, of everything, of one player or of one place.
- (Core) Damage between players follows the server's PvP rule, the claim the victim stands in, and the teams:
  teammates do not hurt each other unless the server lets them, and a machine or robot hurts as its owner would.
- (Core) Sections of work: a mod declares a named section and runs its work through it. The section is timed for the
  F3 screen, runs in a frame of its own name, so Spark and any profiler that samples the threads show it by name,
  and is watched: a run that goes on too long is written to its system's log with the thread's stack, the first time,
  and the operators are told. A section with a budget slows itself while it takes more than its budget a tick, to one
  tick in two, four and so on, says so, and speeds back up once well within it. The energy and fluid grids and the
  results of work away from the tick are sections.
- (Core) Network use: what the server sends each player is counted by kind, by mod and by the block whose updates took
  it, and `/jstech bandwidth` shows the last minute for everyone or one player, with a graph and the biggest senders.
- (Core) What weighs: `/jstech cost` measures what the series' blocks cost the server for some seconds and lists the
  worst places with their owners, and the cost of each kind; nothing is measured between measures.
- (Core) `/jstech health`: the state of the series' systems and their warnings (the ticks, sections slowed or stalled,
  network use, work away from the tick, each dimension's networks and grids, the chunks each owner keeps loaded, the
  protections, the audit log, the robots), each mod adding its part.
- (Core) Crash reports and a diagnostic file: a crash report has a section of the Core with what was running, what was
  slowed or stalled and the state of the systems, and `/jstech diagnostics` writes the mods, the settings, the
  timings, the sections, the health, the network use and the log's levels to a file to attach to a bug report.
- (Core) Notifications on the HUD: a mod declares its kinds of notification once and sends them from the server; each
  comes in the shape of the game's toast, in the player's style, with its level's icon, colour and sound (information,
  warning, alert), the count of alike ones, its mod, what a click does and a line that shortens as its time runs out.
  A click while the chat is open opens a manual's page or shows the place on a map. In the top right corner they stand
  below the game's own toasts. The operators' warnings of stalls and slowed work come this way.
- (Core) The history of notifications, on N: filtered by level and by mod, each line clicked to act on it, taken off
  the list, or, with the right button, its kind or its mod silenced. The player chooses the corner they appear in and
  can ask not to be disturbed, which holds the game's own toasts too.
- (Core) The J's Tech creative tab: the series' one tab of what its mods share, in three sections (materials, fluids
  and components, shared blocks and tools), showing only what the installed mods use; what it shows is left out of the
  other tabs of the mods that declare their uses. It takes the place of the J's Core tab.
- (Core) The open catalogue of materials: materials, forms, properties, mineral families and host rocks are
  registries any mod adds to. A material has a kind (metal, crystal, gem, non-metal solid, mineral, liquid, gas,
  exotic matter) that says the forms it can take, the states it can be in, a colour and the real data it carries; it
  has no tier. The Core brings the 21 base forms, five properties (symbol, atomic number, formula, colour, state at room
  temperature), eleven mineral families and the game's twelve rocks; the series' catalogue starts with iron, copper,
  gold, tin and bronze, the game's own ingots, nuggets, blocks, raw ores and ores kept as theirs.
- (Core) Only what is used exists: a mod declares the materials and forms it uses, and the Core registers the union,
  each thing once, named from its form and its material ("Tin Plate", "Deepslate Tin Ore"), in its common tag and its
  form's (`c:plates/tin`), each block mined with a pickaxe and with its drops; an ore is declared with the rocks its
  veins cross, one block in each, every one dropping the same raw item.
- (Core) Art made as the game loads: every thing of a material is drawn in its material's colour as the game loads,
  from the game's own texture for the forms the game has, recoloured texel by texel, and from the Core's templates for
  the rest; an ore is its rock's own texture with the specks of one of the game's ores in the mineral's colour. No
  texture of the game ships in the Core, and a resource pack can replace any of them.
- (Core) A material's data in its tooltip: each property shows always, only with Shift held, or never, as the property
  says unless the material or a data pack says otherwise, the game's own iron ingot included; a data pack gives values
  over a material's own, one file per material.
- (Core) Packs made as the game loads: a mod declares a resource pack or a data pack whose files it makes from what the
  installed mods declared, each made only when first read, always on and below every pack a player or a server adds.
- (Core) The 118 elements: every known element is a material of the series' catalogue, with its symbol, its atomic
  number, the kind it is at room temperature and the states it can take; their forms exist only when a mod uses them.
- (Core) Isotopes: materials of their own tied to their element by their mass number, named after it ("Uranium-235")
  and written as chemists write them (²³⁵U); deuterium, tritium, helium-3, the reactor's fuels and targets, the
  sources of radiation, and the stable isotope of each superheavy element of the known table.
- (Core) Beyond the known table: the elements 119 to 250, under their systematic names (ununennium, Uue) until they
  are discovered. On a server, the player who first discovers one gives it its name and its symbol, for everyone and
  in every language, kept with the world; a name holds letters only, repeats no other and can be asked to end in
  "-ium", a symbol has one or two letters, and an operator can take a name back. Where discoverers do not name them,
  seventeen take the series' own names, Genevium to Aeternium.
- (Core) The exotic forms of matter, a kind of their own, each in the state it calls for: metallic hydrogen,
  neutronium, quark-gluon plasma, strange matter, dark matter, Bose-Einstein condensate, negative-mass matter,
  degenerate matter, time crystals, photonic matter and computronium.
- (Core) Minerals: a mineral knows its main element and its byproducts in fixed shares, the family that decides how it
  is processed and the rocks its veins cross. Every element mined as such has a generic ore in `c:ores/<element>`.
- (Core) Naturally radioactive minerals: every mineral with uranium or thorium in it is marked so in its tooltip, and a
  world's setting turns that off; the dose comes with the Core's radiation. Any mod can give materials marks of its
  own.
- (Core) Liquids made from materials: a material's liquid exists only when a mod uses it, molten for a solid,
  liquefied for a gas, at the temperature the mod says; it pours into the world, fills a bucket and is drawn in its
  material's colour.
- (Core) The shared catalogue: the common fuels (gasoline, kerosene, diesel) and the generic components (servomotor,
  electric motor, LED, sensor, oscillator, antenna, magnetic head), each existing only when a mod uses it, in a
  catalogue any mod adds to. A mod's simpler recipe for one carries a condition that, where J's Industrial is
  installed, the mod's own server rule can close, making the industrial chain the only path.
- (Core) Grids of a mod's own: the seven grids become a registry any mod adds to, each with its policy, how what it
  carries is shared along a run and what its wires plug into. The Core's energy and fluid policies are the defaults,
  so a mod's own grid, as a voltage class that never joins another, moves energy or fluids as the Core's grids do.
- (Core) States of matter: gases, supercritical fluids, slurries, plasma and exotic matter are each a kind of storage
  of their own, with a capability, tanks that hold their volume times their pressure, and a line of their own that
  never joins another's, moving matter as pipes move fluids and losing nothing. Plasma and exotic matter are held in
  containments that spend energy while they hold: without it plasma dissipates and exotic matter stays sealed in. A
  gas let out is lost. A gas line also fills and empties Mekanism's tanks and the tanks of mods that keep gases as
  fluids, hydrogen being hydrogen in each, and a multiblock's ports pass every state.
- (Core) The energy kit: a mod declares its energy in a few lines (its unit and what it is worth in FE, its classes of
  line with their ratings, what each class loses by distance, and what befalls a port of another class) and gets the
  rest: a grid for each class whose lines never join another's, stores and ports, the sharing along the lines, the
  conversion to and from FE that never makes energy, and tooltips naming a port's class. A port one class below a
  live line burns and stays damaged, two or more below is hit by an arc flash, and one above is short of voltage and
  does not start; nothing happens on a dead line. A mod that only wants FE has it with nothing to declare.
- (Core) Generators that follow the load: a generator makes only what its line draws, so it burns only that much, and
  may declare a minimum it always makes and how fast its output changes, as a reactor does.
- (Core) J's Energy: the series' energy, counted in joules and shown in watts, its lines in five voltage classes from
  400 V to ±1,100 kV direct current, each its own grid; transformers join neighbouring classes and an HVDC station
  the direct current one. A server's settings turn it on (off until a mod builds on it, when every block of the
  series speaks FE), let a block on a class too high only refuse to start, let every block take every class, let its
  blocks take FE directly, and set what a joule is worth in FE.
- (Core) The wrong voltage in the world: a burnt machine smokes and sparks and stays damaged; an arc flash destroys
  it, sets fire around it and burns whoever is close.
- (Core) The clamp meter: a handheld instrument that clamps on a wire of a line or on a block. Held, it shows the
  line's power and which way it runs beside the crosshair; held up, its display gives the details: what crossed
  since it was clamped, the line's ceiling and whether it is full, the most and the least, or a block's store and what
  goes in and out of each face, in the energy's own unit or in FE.
- (Core) Energy as a player reads it: amounts in SI, "7.9 kW", "1.92 MJ"; and a store can show its charge on its own
  model.
- (Core) The protection slot: a fuse blows on the overvoltage it is rated for, cutting the energy with no damage until
  a new one is put in, and lets an arc it cannot break jump over it; a battery keeps a machine going as a UPS,
  charging while the grid has energy and feeding the machine when it runs low.
- (Core) The basic cable and battery: the Insulated Copper Cable, a low voltage line of J's Energy carrying 10 kW and
  losing a tenth over a hundred blocks, and the Lead-Acid Battery Bank, 50 MJ, its charge on the gauge of its sides,
  both made of the game's own materials, so a mod used alone can carry and keep energy; with J's Industrial installed
  they are its first cable and energy bank, made with its recipes. While J's Energy is off they carry and keep FE for
  any mod's machine.
- (Core) The FE converters: a cabinet for each voltage class, from 100 kW at low voltage to 12 GW at ultra-high, and
  the Configurable Energy Converter, whose class the player sets. Each turns FE into J's Energy or J's Energy into FE,
  FE on the left of whoever faces it and J's Energy on the right, up to a limit set on its thumbwheels that never
  goes above its class's ceiling, and never makes energy. Its screen is the cabinet's face: the display, the RUN,
  LIMIT and FAULT lamps, the FE and J's Energy meters, the bridge seen through a window with the energy running
  through it, the class keys or the ceiling, the knob that sets the way and the switch. A line of the wrong class
  stops it. A mod declares a menu its blocks open with `ModContent.menu`.
- (Core) The containment of exotic matter and the Antimatter Trap: the Core's shared blocks where the series' mods keep
  exotic matter, the containment every exotic form but antimatter and the trap, a Penning trap, antimatter alone. Each
  spends energy for as long as it holds something; without energy it takes nothing and lets nothing out, so nothing is
  ever lost; broken, it keeps what it holds in the item it drops, and placed again it holds it again. Antihydrogen
  joins the catalogue, and a material's mark says whether it is antimatter. A matter tank can be set to what it holds,
  as one placed from its item is.
- (Core) The ends of a line: each end where a pipe or a line of items plugs into a block is set on its own: normal, push,
  pull or off; a priority, the highest filling first and ends of one priority taking turns; a rate; one of sixteen
  channels; and a filter by item, tag, mod or components, or by fluid, whose amounts keep that much in the block and
  no more. Every default keeps a pipe from an output to an input working as before; an end offers the first thing some
  other end would take, so nothing holds the line up.
- (Core) Lines of items: a grid that moves items between inventories as the pipes move fluids, for a mod's tubes.
- (Core) Lines that group: a line that carries nothing, each run of it a group of the machines it joins, which hear
  the group they are in, as research computers or a ground station's devices; dyed runs stay groups of their own.
- (Core) Beyond a line's limits: a pipe's rule says what follows when a fluid it is not made for reaches it, by
  default nothing; the Core gives a rule that breaks the pipe after a while and lets the liquid spill.
- (Core) The Basic Wrench: the series' shared wrench, in the common wrench tag; on a cable it goes round the modes of
  the end it points at, and says which it set; sneaking, it opens the end's screen. Items can be declared in tags of
  their own, besides their tab's.
- (Core) The screen of a line's end: on an end of a pipe or a line of items, a screen in the game's own look sets
  everything the end has: its mode; its priority and its rate, typed or stepped, ten at a time with Shift; its channel,
  one of sixteen in the dyes' colours; and its filter, only or all but, whose nine slots take a stack clicked onto them
  (the item itself, the liquid in a bucket or a tank, the gas in a cylinder), go round the thing, its tags and its mod
  with a right-click, and keep in the block the amount set by scrolling on them. A line under the filter says what
  the end does. Any mod's wrench opens it with `EndSettings.open`. A filter's rules can be replaced, removed and given
  amounts one by one, and a grid's policy says whether its ends are set one by one (`routesEnds`).
- (Core) Seeing what flows: each wire of a pipe or a line knows what it carries, which way and how full it is. A pipe
  with sight glasses shows its liquid or gas moving through them at its level, and a line of items with windows its
  items sliding through; a player holding a wrench, or wearing goggles of the `jscore:sees_flow` tag, reads beside the
  crosshair what the wire they look at carries, which way, how much a tick, and how its end is set. A cable type can
  have windows and a fitting of its own at its joints.
- (Core) The network probe: a handheld instrument of the clamp meter's family that reads a whole line of any kind.
  Linked to a wire, held up, its display shows the line on four pages: the summary (the flow of the last minute
  against the narrowest cable and whether it runs full, what the consumers want and how much of it they get, the
  losses, and how many sources, consumers and stores it joins), the members (every block it plugs into, by what it is
  to the line and by kind, with what each gives or takes), the flow (each second of the last minute, with the most,
  the least and the mean) and the losses. OK outlines the chosen blocks in the world, through walls, for ten seconds;
  HOLD keeps the reading still, SHOW puts it beside the crosshair, LIGHT lights the display and POWER switches it off.
- (Core) Overhead lines, as in Immersive Engineering: bare wire hung from insulator to insulator, within the line's
  longest span and along a way nothing stands in, by using the line's item on one insulator and then the next. Any
  line can be hung, as its mod decides. Connectors hand what the wire carries to their block, relays pass it along,
  and a feedthrough takes it through a wall. A hung wire is drawn sagging along its curve, loses as much as its length,
  and falls when its insulator goes. Blocks offer their insulators through `OverheadLines.ANCHORS`; the poles, towers
  and insulators are a mod's.
- (Core) Shocks and surges: touching a live overhead wire hurts, more the higher its class, and kills from high voltage
  up, with the Core's electric damage, which no armour stops; lightning on an overhead line or its insulator sends a
  surge along it that burns the machines on it, unless a fuse in each blows first or a surge arrester on the line
  takes it to earth. Each can be turned off in `jstech-energy.toml`.
- (Core) Dragging a run of cable: holding the click with a cable and looking further along outlines the run from the
  first block to the one looked at, up to 32 blocks away, along the longest axis first, white where it can be laid
  and red where something is in the way; let go, the whole run is laid at once, each block joined to the next, up to
  64 blocks. It stops at the first block something is in the way of, a protected place, or when no cable is left in
  hand, and says where. A cable that runs only straight is dragged straight.
- (Core) Reading a line: a grid's mover tells what a part of it did in the last tick (`IGridMover.survey`): each block
  it plugs into with what it gave and took, what it moved, what its consumers asked, what it lost and its narrowest
  cable. The energy, fluid, matter and item lines all keep it.
- (Core) The basic pipes: the Copper Pipe for liquids up to 200 °C, the Iron Gas Pipe, and the Vacuum-Jacketed Pipe
  for cryogenic liquids, thick pipes with sight glasses and solid fittings, made of the game's own materials so a mod
  used alone carries liquids and gases. A pipe carrying a liquid hotter or colder than it stands bursts after five
  seconds and spills it. With J's Industrial installed they are its pipes, made with its recipes.
- (Core) Sensors that speak redstone: a sensor or a machine says what it reads as readings, each with its span, and
  gives the one chosen in redstone through one capability, `Signals.OUTPUT`, which anything that reads sensors asks; a
  comparator reads it too. A sensor's neighbours are told only when what it gives changes. A mod writes only what its
  sensor reads.
- (Core) Signals by frequency, redstone without wire: a transmitter sends what is fed into it, from 0 to 15, on a
  numbered frequency of its owner's or of its owner's team, or to a pair of its own, and every receiver on it gives it
  out. Two transmitters of one owner on one frequency clash, and their receivers give nothing until one is taken off.
  A transmitter is heard as far as its power, less four blocks for every solid block in the way, its value whole
  within and nothing beyond; a relay of its team sends it again, and a relay's partner sends it again elsewhere, in
  another dimension too. Nothing is worked out on every tick, and a receiver's neighbours are told only of a change.
  The transmitters, receivers and relays are a mod's, on the Core's kit.
- (Core) The signal probe: a handheld instrument of the network probe's family. Used on a block it reads the redstone
  on it, now and its peak since; held up, its display lists the frequencies heard where the player stands, each with
  its number, whose it is, where it comes from, how well it arrives and the value it carries, clashes marked in red,
  and the frequencies of the player's team that are not heard there; on its radio's page one is told in full, and OK
  outlines where it comes from.
- (Core) What every machine shares: its state always in view (working, waiting for input, output full, no energy,
  undervoltage, fuse blown, damaged, off by redstone, switched off), on its lamps and in Jade, with a puff as it starts
  and smoke as it works and stops, and the sounds its mod gives it; an ON/OFF switch; redstone modes (ignore, with a
  signal, without one, one recipe a pulse); a comparator that reads what the player picks (progress, input, output or
  energy); energy of a mod's own through a port of a class, with its fuse and battery; tanks of gases,
  supercritical fluids, slurries, plasmas and exotic matter; damage instead of wear, from overvoltage, explosions
  (which no longer break a machine) and sabotage, shown as cracks and working slower, mended by a repair kit held on
  it for a few seconds; values the outside reads and sets, for controllers and networks; its owner, whoever placed it;
  and rest while it has nothing to do, so idle factories cost the server nothing.
- (Core) Machine sides: each face of a machine takes in, gives out, does both or nothing with each of its resources
  (items, fluids, each state of matter, energy), and pipes, cables and hoppers find only what the face lets through. A
  machine pushes what it makes into the block beside its giving faces and pulls what it works from the block beside its
  taking faces by itself, at a rate and through a filter.
- (Core) Machines that never jam: an input slot locked to an item takes nothing else; the player picks the recipe when
  the inputs make several; and factories of several lanes work side by side, sorting what is put in among them.
- (Core) The whole recipe format for machines: catalysts that are not used up, gases and the other states of matter in
  and out, byproducts by chance, and the temperature and pressure a recipe runs between, all in the recipe files and
  shown in JEI and EMI.
- (Core) The machine screen: every machine of every mod opens the same screen, in the game's own look, five tabs over a
  steel face with the machine's name, class and lamps. Main shows the machine's own face, or the standard one (a meter
  of what it draws, an LCD of what it made the last minute, the process window with each lane's arrow, its switch and
  a sight glass of its first tank); Sides shows the machine as two cubes whose faces are picked and turned, with the
  switches, thumbwheels and filter of what it pushes and pulls; Redstone has the knobs of how it answers redstone and
  what a comparator reads, the lamps of the signal in and the meter out; Upgrades has its bays and the bars of what
  they do; Protection has its port and line, its fuse, its battery and the key of who may use it. Ctrl and a click
  locks an input slot to what it holds.
- (Core) JEI's "+" and EMI's fill button move a recipe into the open machine: its ingredients from the player's
  inventory into the inputs, once or a stack's worth, and the recipe picked among those its inputs make.
- (Core) Upgrade effects: besides speed and energy, an upgrade can make a machine work several recipes at once, make
  more items, make byproducts likelier, spend less fluid and matter, give off more or less heat, run quieter, and store
  more energy; each upgrade says how many of it fit in one machine. Upgrades are declared with no class of their own,
  and say what they do in their tooltip.
- (Core) A wrench on a machine: any mod's wrench turns what the face clicked does with the resource it shows, shows each
  face of the machine looked at in its colour, and, sneaking, takes the machine away whole, contents and settings
  going with it in its item.
- (Core) The Configuration Card copies a machine's setup and pastes it into others, whole into the same kind and what
  fits into another, or into every machine of a box at once.
- (Core) Machines grow in place: the tiers of one machine are declared as one family, and an installer makes a machine
  the next tier without breaking it, its contents and settings kept.
- (Core) Heat with real physics: blocks that hold heat have a temperature in kelvin and a heat capacity from their
  material, conduct heat to their neighbours by the material's real conductivity, give heat to the air as thick as it
  is and radiate, so in a vacuum only radiators cool them. Heat pipes carry heat between the blocks they join. The
  everyday elements carry their real density, specific heat, conductivity, emissivity and melting and boiling points.
  Only blocks out of balance are worked, a few hundred a tick, and the rest sleep.
- (Core) Boilers with the real enthalpy: a hot wall boils water into steam, each litre taking the heat that brings it
  to its boiling point and then 2.26 MJ a kilogram, and lower where the air is thin or higher in a pressure vessel.
  Heaters and coolers run at their power up to their limit, and a loop's liquid in its cool and hot forms passes
  through a wall to give or take its heat, so two passes make a heat exchanger. The game's water is the series' water,
  and water and the liquefied gases carry their real heat of boiling and molar mass.
- (Core) Cold has its consequences: a pipe of water freezes and carries nothing where the place is below freezing,
  unless it is insulated, and liquid nitrogen and the other cold liquids boil off their tanks as the place's heat
  leaks in, the slower the better the tank is insulated; a server can turn either off. Blocks of the world conduct
  heat as their real materials do, so wool keeps a hot block hot and metal draws its heat away, and mods can declare
  places with air of their own.
- (Core) Machines that make heat: a machine's casing warms as it works and is its heat port, through the faces its
  Sides tab opens to heat; past the hottest it stands it stops to cool and loses health, and starts again once cooler.
  A block past 300 °C sets alight what burns against it, and one past 150 °C burns whoever stands against it; a
  server can turn either off. Jade shows every hot block's temperature, and sensors read it as a thermometer or a
  thermocouple does.
- (Core) Mechanical power in real units: shafts, gears and gearboxes join into networks that turn as one, each part
  at its ratio, gears trading speed for torque and turning the other way; a network with too little power turns
  slower, where its sources' torque meets what its loads ask, and one whose gear ratios disagree jams. Sources
  declare their torque curves, and a machine becomes an electric motor that spends its energy to turn its shaft, or
  a dynamo that makes energy of a turn. Networks are worked out only when they change, and turned whole each tick.
- (Core) The rest of the mechanical kit: flywheels that smooth a line and coast after the power is cut, clutches that
  let go while powered, brakes that hold as hard as their redstone says, speed controllers whose end turns up to
  twice as fast by their signal, shear pins that shear under overload and spare the rest, and parts that break past
  their rating where the server lets them. Belts and chains join two pulleys across a distance, the smaller turning
  faster. Machines can be driven by a shaft, working as fast as it turns and stopping below their least speed.
- (Core) Reading mechanical power: every shaft, gear and gearbox is a sensor of its speed for redstone and
  comparators, a torque meter reads the torque through it, Jade shows a part's speed and its network's power, and
  the network probe reads a network of shafts in watts, with its sources, its loads and what friction takes.
- (Core) Multiblock shapes with rules: a place can count its blocks ("from one to four energy hatches") and give the
  machine a number, as a blast furnace is as hot as its coolest coil; a pattern can be found mirrored or lying on its
  side, and can grow by a repeated layer, row or column, a tower as tall as the player builds it. Shapes need no
  drawing at all: any box between a least and a most size with its frame, faces and inside, as a dynamic tank; a
  floor of any outline with walls round it, as a smeltery; any filled cuboid of units. What stands inside counts,
  with neighbour rules: a cell gives more beside a moderator, a cooler works only beside a working cell. A failed
  check names the wrong place of the way the player most likely built it, and every rule goes to the pattern's file,
  so a data pack changes it with no code.
- (Core) Multiblock machines built in the world: a machine becomes a structure's controller and forms when the player
  has built it whole, in any order, standing "Not formed" until then; breaking any block undoes it and the controller
  keeps what it holds. Jade shows whether it is formed, its size and how many recipes it runs at once, or what it
  needs and where. Only its owner and those trusted break its blocks. It is checked only when a block near it
  changes, never every tick, and never while a chunk it reaches is unloaded.
- (Core) Multiblock ports as blocks of their own, placed anywhere on the shell: items, liquid, energy (meeting J's
  Energy cables with the controller's voltage class), each state of matter, heat, and a mod's own lines such as
  compressed air; and a redstone port that reads the signal round it for the controller or gives the controller's
  signal out. A dye puts a port in the group of its colour, and each group feeds a lane of its own, so two processes
  never mix. A structure runs as many recipes at once as it says, and Jade shows the exact number.
- (Core) Multiblock machines entirely as data: a file gives a machine its shape (a pattern by id or drawn, a box,
  walls or a cuboid), its slots, lanes and tanks, its energy, the kind of recipe it works and how many at once, and a
  controller block built on the Core's data machine block names the file. The files reach every player, and a data
  pack adds or changes a machine with no code.
- (Core) The Structure Projector: shows any multiblock of any installed mod in the world before it is built, the
  blocks still missing as pale ghosts, what is wrong outlined in red through walls, and above the structure its name,
  its size and how much is in place, missing and wrong; looking at a ghost says what goes there and on which layer.
  Its screen previews the multiblock in 3D layer by layer and chooses it from the list of every multiblock, its turn,
  its mirror, its layer and whether only what is missing shows. Every multiblock is listed with nothing more to write,
  and a mod can hide one.
- (Core) Multiblocks in JEI and EMI: every multiblock that can be shown has a page of its own, with the multiblock in
  3D turned by a drag or a click, a button for each layer, the blocks that will do where more than one does, how it
  may stand, its materials with how many of each, its size, and buttons that show its ghosts in front of the player
  and open its page in the manual. Looking up a block's uses finds the multiblocks built of it, and looking up a
  controller finds how to build it.
- (Core) A multiblock pattern names its controller's block, in code and in its file, so the projector and the recipe
  viewers show the controller in its place and count it; a machine declared as data has its controller block found by
  itself.
- (Core) Multiblocks in the manuals: a page names a multiblock by its id and it is drawn as a scene the reader turns
  and peels layer by layer, laid out from the multiblock in force when the manual is read, its controller in place.
- (Core) The Multiblock Builder, a handheld instrument used on a multiblock's controller: its display says what is
  missing and how much of each block the player has, lets them choose what goes where a place takes several blocks
  and where the blocks come from, and OK builds the multiblock a block at a time from the inventory, as the player
  would place each block, stopping and saying why when it runs out, the player walks away or a place is protected.
  HOLD holds it, SHOW shows the multiblock's ghosts, LIGHT lights the display. Servers set how fast it builds and how
  far the player may walk; other mods can add a source of blocks of their own.
- (Core) Microblocks of any full block of any mod: covers, panels, slabs and the thicknesses between, hollow pieces,
  posts and corners, drawn from the block's own textures. Several share one block space, a cable's included, each
  placed where the click puts it, hit, outlined and broken on its own with its own drop, and a block space of pieces
  alone is made where nothing stands. A whole cover closes a cable's face; a hollow one lets the cable through.
- (Core) The saw, a tool of the series' shared content, of a stick and two iron ingots: in the crafting grid, above a
  block or a piece it cuts two halves, beside one it cuts posts and corners, at a corner of one it hollows it; each
  cut wears it. Two pieces of one shape and block join into one as thick as both, and two slabs make the block again.
  Any mod's saw cuts alike, and a pack can keep a block from being cut.
- (Core) Facades: a cover joined with any cable hides the cable behind it on its face, while the cable keeps working.
- (Core) Microblocks drawn as cleanly as blocks: where two pieces on faces side by side meet along their edge, one
  gives way, so two blocks never flicker in one place; a face lying flat on an opaque piece is not drawn, nor a
  neighbour's face behind an opaque cover.
- (Core) Parts that move: a kind of part with a fan's blades or a gauge's needle is drawn each frame, only in the
  spaces that hold one, while the rest of every part stays in the world's mesh.
- (Core) What a part does on its own face: give redstone out of it and into the block there, give a comparator
  reading, give light, hear the redstone reaching it, open a menu that closes when the part is taken off, and take a
  room of its own shape. A cable or a space of parts is ticked only while one of its parts needs it, so a world of
  plain cables costs nothing each tick, and redstone wire joins a cable only on a face whose part deals in redstone.
- (Core) The game's torches, levers, buttons and pressure plates share a block space: used on the face of the block
  beside a cable or a space of parts, the item hangs its block in the space, beside the wires and the other pieces,
  where it lights, powers, is pulled, pressed and falls with its wall as the game's own does.
- (Core) A wrench of any mod on a part of a cable or a space of parts turns it, a device a quarter turn about its face,
  a lever or a button on a floor a quarter turn, a post through the middle to its next axis, and sneaking takes the
  part off into the player's hands. Jade names the part looked at and shows what it says it is doing.
- (Core) The microblocks' rules in JEI and EMI: the saw's cuts, two pieces joined and a facade are one crafting recipe
  each, going through every block and piece rather than thousands of entries; looking up a block in JEI shows the
  cuts of that block.
- (Core) Parts declared once, as blocks are: a mod names a kind of part, gives it a look and says what it does, and
  gets its item, which mounts it on a cable, a space of parts or the face of any block, its name and its models from
  the data generation, a plate's included. The GameTest kit builds spaces of microblocks, parts and small blocks.
- (Core) Conveyors: a mod declares its kinds of belt, with their speed, how many items a block holds, what makes them
  move and what they do at an end with nowhere to go. Belts of a kind one behind another move as one run, and only
  its end ticks, so a long line costs one belt; the items on a belt are a queue, never entities, drawn moving smoothly.
  A run hands its items into a belt or an inventory in front, or holds them there, or lets them fall off; hoppers,
  pipes and items that land on a belt put items on. A belt moves by itself, as a shaft at any of its run's belts
  turns it, or on energy any of them takes in, and carries the players and mobs that stand on it.
- (Core) Conveyors that climb, fall, deal and sort: belts that rise or fall a block, laid to meet one another as rails
  do and turned by a wrench; chutes that drop items into what is under them; bucket elevators that lift items and tip
  them out to their front; splitters that deal items out in turn, joins that take from each side in turn, and
  diverters that turn aside what their filter names, learning it from a click with the item. A machine on top of a
  belt works the items that pass under it, holding each still while it works. A broken piece lets fall what was on it,
  and a mod declares its own pieces on the same kit.
- (Core) Machines of any mod on the data network: a block entity says what the network may do with it, the kinds of
  Operation it carries out and what the network may read of it, and the Core keeps it on the network of the data
  cable it touches, with no tie to J's Computers; a member that could never be what it says stops as it is made. Its
  state is read as tables a mod declares, a row for each machine and a column for each value, numbers in their units,
  text and flags, so whatever runs the network reads a reactor the way it reads anything else.
- (Core) Progression kept per team: the eras of hardware are a registry an add-on's era goes into the middle of; the
  axes move by playing, as a player first makes an item, at a table, in a furnace or in a machine they own, or places
  a block; what a team reaches is the team's alone, and two teams that merge keep everything either had. Mods declare
  ordered milestones a team reaches once, and knowledge a team comes to know whole or a share at a time. A machine
  recipe may ask for a piece of knowledge: the machine will not run it until its owner's team knows it, saying "Needs
  knowledge", while JEI, EMI and the manuals always show the recipe, with a padlock. Operators give and take
  knowledge and milestones with `/jstech knowledge` and `/jstech milestone`.
- (Core) Worlds of the series open with no warning about experimental settings: the data and the dimensions of the
  series' mods are taken as stable, and any mod may say the same of its own namespace; another mod's dimension still
  warns. Every place the Core reaches into the game is listed with its reason and a test that fails if the game moves
  what it reaches.
- (Core) Models: animated models for blocks, items, entities and armour, read by the Core's own readers from
  Blockbench projects, Bedrock and GeckoLib geometry and animation files, glTF (text and binary), FBX (binary and
  text, version 7 on) and OBJ, with bones in a tree, cubes, meshes, locators, skins and morph targets. A model is
  declared once, with its textures, its animation files, the bones that glow, whether it is see-through, and variants
  chosen by the block's state; the data run writes its definition and stops at a file that is missing or does not
  read. Controllers play animations with every Blockbench easing, loop, hold or once, at any speed, several at once,
  blending from one to the next; Molang in keyframes reads what the code gives it; sounds, particles and words to code
  happen on their times; code moves bones over the animations; items are held at locators; and the server can play an
  animation for everyone who sees the model. A model is worked out once and shared, animated only in sight and near,
  and drawn with the game's own shaders, so shader packs draw it. Families of files laid out for GeckoLib are drawn by
  the Core's models as they are, alike to the pixel.
- (Core) The preview of a model: `/jstech model preview <model>` opens it, a declared model or any model file by its
  path, turned, zoomed and moved with the mouse over a floor of quarter blocks, its animations played or paused from
  their buttons, its bones in a tree, the chosen one shown in yellow, its locators by name, its glowing parts shown in
  the dark, its faces as wire, a timeline to drag and a speed to step. In a development run a changed model file
  reads the models alone again, at once, and an open preview shows the change; `/jstech model reload` does the same by
  hand.
- (Core) Formed multiblocks: a structure can turn into one detailed model once it forms, a turbine's casing with its
  rotor turning in place of its iron, turned, mirrored or laid down as it was built. Its plain blocks merge into parts
  that draw nothing and stand in for what they were, breaking as slowly and dropping the block they were; the
  controller, the ports and every block with a block entity stay, so pipes and cables still meet them. Forming puffs
  where each block merged and can play a sound and an animation; breaking any block gives every one back, and a part
  whose controller is gone turns back by itself. A damaged machine's cracks show across its whole structure.
- (Core) Turning parts: shafts, gears and pulleys drawn turning at their real speed by the Core's models, the angle
  following the speed frame by frame, meshed gears half a tooth apart so their teeth meet, every part of one model
  sharing its shape; a belt or chain runs between the pulleys it joins as fast as their rims. A part drawn turning
  hides nothing beside it and is lit as its place is. A part can be heard turning, louder and higher the faster.
- (Core) Hot things glow: a block that holds heat past 798 K, the Draper point, glows in the colour of a black body at
  its temperature, from Planck's law and the eye's colour matching, dull red to orange to yellow going white, as
  bright by night as by day.
- (Core) Thermal goggles: an item in `jscore:sees_heat`, worn or held, shows the world as a thermal camera does, what
  is at the air's temperature under a dark veil and, in false colour, the blocks that hold heat, liquids far from the
  air's temperature such as lava, and living things at a body's warmth (the undead as cold as the air, anything
  burning hotter); beside the crosshair, how hot the thing looked at is, in kelvin and degrees Celsius.
- (Core) Models drawn from a whole block take the light around the block, where none reaches inside it.
- (Core) Connected textures of resource packs: textures that join between alike blocks, read from both formats packs
  write, so a pack made for OptiFine, Continuity or CTM works as it is and a mod declares its own as data. OptiFine's
  and Continuity's `.properties` rules with every method (the full 47 tiles, compact, horizontal, vertical, both ways,
  top, random with weights and symmetry, repeat, fixed, and overlays of every kind with their tint and layer), the
  textures and blocks they match with their properties, faces, biomes, heights, weights, names and inner seams; CTM's
  metadata with every type (ctm, horizontal, vertical, pillar, simple, random, pattern, edges, full edges, eldritch),
  its light, layer, proxies, conditions with predicates and faces of their own, its overrides in block models, and
  logics written as data, OptiFine's full sheet among them. Blocks may be named by tag. Faces join by how their
  texture lies on them, partial faces take the same part of their tile, and microblocks join their like in the spaces
  around. While Continuity or CTM is installed the Core steps aside and says so, unless the player's setting keeps it.
- (Core) Lamps: kinds of light as data, declared by a mod and written over by a data pack (colour, intensity, reach, a
  point, a spotlight's cone or a narrow beam, a flicker by name or by its own letters, what turns it on, the watts it
  draws, whether it breaks), and lamps of a kind that shine always, by redstone or while they get their watts, flicker
  each from its own point, and go dark broken until mended. Their light is the game's own, so it counts for what
  spawns and grows.
- (Core) Spotlights and beams reach far past fifteen blocks: their rays go on through air and water and light the
  place where they meet something, as bright as the light still is there, with light that counts in the game; it is
  given back when the lamp goes off, and light left where no lamp holds it goes by itself.
- (Core) Coloured light: the colour of the light of blocks, of what is held and of mobs is data, the game's own filled
  in (soul fire, redstone, the sea lantern, amethyst, the froglights, lava, blazes and more); light of several colours
  mixes and tints the world it lights, fading smoothly across faces, only where its light reaches, less under open
  sky. It rides in the world's mesh, so it looks the same with Sodium, Embeddium and Iris as without them.
- (Core) Dynamic light: a torch held lights the way, a mob on fire its surroundings, an item that gives light the
  ground it lies on, a model's glowing part that casts light the world as it moves, and a mod's own moving lights;
  drawn on the player's game only, nothing of the world changed.
- (Core) Glow without a shader: soft halos about bright lamps, blocks of coloured or bright light and glowing parts,
  in their colour, and the shaft of a spotlight through the air, fading along its reach.
- (Core) Light sensors read a place's light, from 0 to 15, and the hue of the light there.
- (Core) The player's lighting settings: coloured light and dynamic light on or off, the glow's strength, how far it
  all reaches. Dynamic light steps aside for LambDynamicLights and Sodium Dynamic Lights, coloured light and glow for
  Shimmer, unless the player's setting keeps the Core's on as well.
- (Core) Particles and effects as data: an effect is a file of emitters with shapes (a point, a sphere, a box, a disc,
  a cone, a line, the faces of a model), a rate and bursts, particles whose size, colour, picture and spin change
  over their life, with a light of their own, a ribbon behind them, collisions with blocks, and emitters they carry.
  They move by the physics of where they are: the dimension's pull, drag and lift by the air's density, the wind, the
  water they are in; steam rises and drifts, nothing rises in a vacuum, a plume widens as the air thins, bubbles
  live only in water.
- (Core) The wind: a few metres a second in a world with air, turning over the days, stronger in rain and storms and
  higher up, the same on the server and every player's game; a mod's weather says its own.
- (Core) Beams, electric arcs that zigzag and branch, ribbons and tracers; marks that fade on the world, scorch and
  soot; and what effects do to the screen: a flash, a shake, the ring of a shockwave bending the view, the air
  shimmering over hot things, a hazard's vignette.
- (Core) Effects fired from the server reach whoever can see them in a few bytes, aimed, coloured, scaled, following
  an entity or played on a block where its file anchors it; a model's animations play them at their locators. The
  Core brings smoke, steam, sparks, a beam, a trail, an arc flash (now fired by overvoltage), an explosion, a cloud of
  gas, bubbles, a rocket's plume, a jet's exhaust, a contrail, re-entry plasma, scorch, soot, a laser and lightning.
  An effect held by something moving fast lays what it gives off along the way it went each tick, so a racing
  engine leaves a line, not beads; and a held effect can be scaled, an engine's flame as big as it pushes.
- (Core) The effects editor in the world: the effect plays on the real block with the world's wind and air while a
  card turns its emitters on and off and changes their numbers, the game's debug lines saying what it costs and what
  acts on it; Save writes its file in the mod's source. A new effect starts at the block looked at, from a face or a
  locator of its model and from smoke, steam, sparks, a beam, a trail or nothing, playing there at once.
- (Core) The player's effects settings: particles at once, how hard the screen shakes, how bright a flash is, and
  whether heat haze and shockwaves bend the view; the game's own particle setting thins them out.
- (Core) Terrain declared in code: how high the land lies, how far and how wide its hills, how rough, its rock, the
  liquid of its seas up to their level, a floor of bedrock and its surface biome by biome, written as the noise
  settings a data pack can change; a dimension takes it with its biomes spread by the terrain's warmth and wetness.
- (Core) Biomes declared in code, with their climate in real values: degrees Celsius over the year, how far the
  seasons swing it, millimetres of rain a year, kept in a data map a data pack can fill for any biome; every other
  biome's read from the game's own values. A place's warmth follows the calendar's seasons and cools with height,
  and heat meets it where a biome has a climate of its own.
- (Core) Landforms as data, laid a chunk at a time from the world's seed so any size draws whole: craters with
  their ejecta, rays and central peaks, shield volcanoes and stratovolcanoes with lava in their summits, mesas in
  bands, dunes, canyons, lava tubes open to the sky here and there, ice sheets, geysers that bubble, sinkholes and
  cracked salt flats.
- (Core) Lakes, rivers and seas of any liquid, which freeze over where the place is colder than their liquid
  freezes and boil away where it is hotter.
- (Core) Rock in layers by depth, each region of the land with its own sequence, the layers rising and falling
  gently and the ores lying in them.
- (Core) Ores lie in veins: zones three chunks wide hold at most one, dense at its middle and thinning outwards, of a
  kind declared as data (its materials and their weights, its heights, how often, how large and dense, its biomes and
  rocks), which a data pack can change or add to. A vein crosses rocks and each block takes its rock's own ore, laid
  only where some installed mod uses that ore; the same for the same seed on any number of threads.
- (Core) The game's iron, copper and gold now lie in veins (the Nether's gold too), with tin once a mod uses it; their
  scattered placement stands aside while veins are on.
- (Core) Prospecting: an instrument learns which way the veins near it lie and how strongly each shows, never where,
  and standing in one, what it is made of and how large it is.
- (Core) The world's vein settings: veins on or off, how often, how large and how dense each kind is, the materials
  whose ore is never laid, and trying to move other mods' scattered ores into veins of their own.
- (Core) Reservoirs of oil, gas and brines as things in the ground: a lens of soaked rock under a cap rock, of a kind
  declared as data (what it holds, its rocks, how much a block holds, where, how often and how large). A well drains
  it a block at a time, a drained block turns back into plain rock, and nothing floods; what was drawn is kept in the
  world's data over areas, so every mod reads the same reservoirs.
- (Core) The biome modifier `jscore:everywhere` adds features to every biome of every dimension.
- (Core) Structures that stand where the land suits them: of one template or a layout of many joined by jigsaws,
  spread as often as they say, none near the world's origin and none within reach of another structure of any mod, or
  at fixed places only; refused on ground too steep, too low or high, or under water; sunk as deep as they say, on
  foundations reaching down to the ground, and laid out like the game's villages, streets holding their houses.
- (Core) Structures worn by rule as they are placed: iron rusts, glass cracks, as often as each rule and the
  structure's age say, keeping each block's facing, the same each time.
- (Core) Settlements that grow: the whole layout is planned as it generates and built a stage at a time as the
  world's days pass, into the chunks that are loaded, a chunk loaded later catching up.
- (Core) Placing a structure where a mod says, as the world would have, and kept by the world afterwards: found by
  instruments, remembering its state, growing if it is a settlement.
- (Core) What each structure remembers of itself, by any place inside it, each change heard on the game's bus; and
  seals that open a chamber only to a team that knows what they ask.
- (Core) Loot of another mod's item that gives it only while that mod is installed.
- (Core) Landmarks a player discovers by coming near, for themselves and their team, with a notification that points
  at them, declared in code or by data packs.
- (Core) Finding the nearest structure, biome, landmark or vein with a precision: roughly at first, within 512 blocks,
  then each level twice as close, never further off than it says and the same each time; structures at fixed places
  included.
- (Core) Markers on the maps, each player's own and their team's, in layers, the landmarks discovered among them,
  shown on JourneyMap, Xaero's Minimap and World Map, FTB Chunks, or the series' own maps.
- (Core) Generation laid into the chunks made before it, a few each tick once their neighbours are loaded: a mod
  added to a world lays its ores into the land already explored. Each chunk remembers what it has had.
- (Core) The surface changing by itself as the climate and the weather say (ice thawing where it is warm, water
  gathering in basins in the rain), each change declared by its mod or a data pack; and a place's biome changed, its
  colours and its climate with it.
- (Core) Large edits planned away from the server's thread from a snapshot of the world, and set a share of each tick:
  a crater or a levelled city never stalls the server.
- (Core) World events by schedule or by chance (storms, showers, quakes): warned of to those carrying their
  instrument, told to everyone they cover, their effects played in sync, kept through restarts, started, put off and
  stopped by operators with `/jstech event`, and switched off, made rarer or made harmless by the server's settings.
- (Core) Fields over the chunks that spread and fade (fallout, pollution), worked out away from the tick and sent to
  the players near them.
- (Core) The Space Persistor, the series' chunk loader: it keeps an area round it loaded and working for its owner,
  nearest chunk first. Its screen is a map of the land round it, the chunks it keeps outlined and another owner's
  hatched, a click adding a chunk or taking it off, zoomed from 16 to 128 chunks across; keys for a square, a circle, a
  line or only the chunks clicked, drums for the radius, an LCD of what it keeps, its owner's count and the server's,
  and a plate to name it. Each chunk costs power (200 W of J's Energy by default, set by the server, or nothing at
  all, when it has no port); short of power, switched off or broken, it lets every chunk go.
- (Core) A server's budget of loaded chunks: every owner together keeps at most 400 by default, besides each owner's
  own number; `/jstech chunks` counts them, and `/jstech chunks map` shows an operator every place keeping chunks,
  and whose it is, on their own map. The chunks a machine keeps are an area on its owner's team's maps.
- (Core) Each dimension's own day and year: a dimension's rules can give it a day of any length, a world locked to its
  star with its sun held still, and a year of its own days, its seasons a quarter of it and its world going round its
  orbit; the overworld's day is never touched. Its wind can blow harder or softer than the overworld's.
- (Core) The sky kit: a dimension's sky drawn from a data file, with suns coloured and sized by their kind of star and
  dimmer the farther they are; planets and moons lit by the real geometry, so their phases and eclipses come of where
  they stand, their surfaces turning and their rings leaning; square stars, the band of the galaxy, nebulae and an
  aurora; the colour of its sky, fog and sunsets, its own clouds and haze. The day's light dims with the sun's
  distance and in an eclipse, and takes the colour of its suns.
- (Core) The weather kit: kinds of weather any mod declares, or a data pack writes, brought by world events: rain of any
  liquid, snow of any ice and dust storms in their colour, fog that closes the view in, wind that blows harder, harm
  and effects on what lives out in them, and lightning. A server that makes an event harmless makes its weather
  harmless too.
- (Core) Machine screens of their own: a mod's machine whose screen is unlike every other's opens its own on the same
  menu, keeping the tabs, plates, lamps and Protection tab of every machine; a machine that holds no items leaves the
  inventory off its Main tab, and one whose energy a setting turns off offers no port.
- (Core) A dimension's air by height: its rules can say what its air is made of, breathed where the oxygen's own
  pressure is enough, and how its air, pressure and temperature change with height in bands, so a land can burn at
  the ground and be cool and breathable high in its clouds; a player is weighed by the air at their eyes.
- (Core) Laws of their own: a place, a dimension or a body can keep laws of physics of its own. Gravity scaled,
  turned along a direction or pulling to a point, felt by players, mobs, items, falling blocks and arrows alike; time
  running slower or faster for what lives there and for the series' machines; light tinted, shifted towards red or
  blue, shimmering, darkened or glowing; space that wraps round, shortcuts, small doors into large rooms of another
  dimension, and portals with no edge that show their far side; explosions that go out slowly and liquids that rise. A
  mass curves space round it, pulling things in, slowing time and bending the view round a black horizon.
- (Core) Sealed rooms: closed spaces are found where a block asks for them and worked out again whenever a block near
  them changes, by any means, a little each tick; their air is real gas, its pressure an ideal gas's, breathed by the
  oxygen's own pressure. Gas leaks through cracks slowly and holes fast, and between rooms joined by doors, their
  pressures evening out at once and their airs mixing over time; how each block seals and what it stands is data, and
  a block facing more than it stands bursts. Inside a room its air is breathed and its warmth kept; carbon dioxide
  gives a headache and then stifles, pure oxygen poisons slowly, fire goes out without oxygen and turns into an
  inferno in pure oxygen, and air rushing out of a hole pulls things after it. A room under water is pumped dry and
  floods through a hole as fast as the depth pushes. Mobs breathe by their kind, plants need air and breathe a room's
  carbon dioxide out as oxygen, vents join rooms to the gas lines, goggles show a room's outline and leaks, and mods
  are told when rooms form, change, fill and leak. Each consequence can be turned off. Finding a room reads only
  chunks already loaded, never loading one, and a mod that puts a whole area back as it was forgets its rooms with it.
- (Core) The Atmosphere Detector, a block of the series' shared content: a pressure gauge, each gas with its share and
  its own pressure, the room drawn with its leaks blinking, four lamps, and a key that shows the room in the world.
- (Core) Carbon dioxide joins the shared catalogue, a gas every body and fire breathes out.
- (Core) How sound travels: every sound of the world, the game's own and other mods' included, echoes in the place
  the player stands in by its size and its walls, a sealed room by its exact size; walls muffle it by what they are
  made of, wool little and metal much, and the air takes its high notes away with distance. Through a vacuum only
  what the player touches is heard; under water everything is dull; thin air carries sounds weaker. A far sound
  arrives as late as the speed of sound brings it, and what moves changes pitch as it passes. A sound can say it
  matters, to be shown on screen with where it comes from, and what it becomes heard from far away.
- (Core) Resource packs give blocks their sounds by material (steps, blows, breaking, placing, falls, and how much
  they drink and let through), a blow on a living thing its sound by the kind of thing struck, the ambience of places
  by biome, weather, time, depth, roof and what is near, and music in layers that come in and go out with the moment
  while the game's own music waits.
- (Core) Ringing ears after a blast close by, unless something worn protects them; radio and intercom bands for
  sound played through devices, a helmet's radio heard even through a vacuum; and, with Simple Voice Chat, voices
  muffled by walls and silent through a vacuum like every other sound.
- (Core) Sound made visible: the debug screen tells the place's echo, its walls and the air sound goes through, and a
  key draws the rays the place is heard by and a line to every sound playing. The Sound Mixer's Options tab adds
  Echoes and Distance and Ringing Ears.
- (Core) Travelling between dimensions with what one rides: a vehicle and everyone in it go together and arrive at a
  safe place near where they were sent, on firm ground with room above, a platform laid where there is no ground;
  a mod shows the passage through a screen of its own instead of the game's "loading terrain".
- (Core) Hazards: one kit for whatever harms a body from where it is. Kinds of harm every mod shares (radiation,
  electrical, cryogenic, acid, heat, blast, pressure, dust, toxic, glare), any damage of the game or of a mod made one
  of them by a tag; what worn things and each kind of creature stop of each, as data, adding up by kind and cutting
  the damage as it lands, shown in the tooltip. A hazard builds up in a body from its sources, cut by what is worn
  against it, harms in bands, goes away away from its sources, faster with a medicine or a shower a mod gives it,
  kills with a death message of its own, and creatures
  suffer the kinds that harm them. The dimensions' bad air, heat and cold are kinds of it.
- (Core) Hazards of carried things, as data: a hot ingot burns the hand, sodium goes off in the rain, a pyrophoric
  powder catches fire, an unstable charge goes off in a fall, a toxic or corrosive thing harms its carrier and a
  blinding one blinds, each named in the tooltip with what protects from it.
- (Core) The lungs: coal dust, silica and asbestos breathed, breaking their blocks or near a machine, build up black
  lung, silicosis and asbestosis, a lasting harm that heals slowly or hardly at all, from a cough to the lungs
  failing; masks and respirators stop their share.
- (Core) The hazard devices, items of the series' shared content in yellow cases with black faces: the Geiger counter,
  its needle on a scale of tens, its range and a lamp for every click, clicking faster as the rate climbs and named in
  the subtitles; the pocket dosimeter, the dose taken and its band; and the hazard map, the land round the player with
  a hazard on each chunk measured. Carried, each shows on the HUD in the corner and at the size the player chose;
  used, it is held up full size, or the map opens whole. The body's screen, on its own key, a worn panel of green
  enamel with the dose, the rate here, a shower lamp, the lungs, dissonance and the other hazards.
- (Core) Maps of hazards: an instrument carried reads its hazard at each chunk onto the player's map, kept through
  death, and a mod's sensor adds its own readings; what was never read stays grey.
- (Core) The land withering: where radiation or pollution is high grass dies into withered grass, plants wither to
  dead bushes and crops stop growing, and they come back slowly where it falls.
- (Core) Hazard settings: radiation can be turned off as a whole, and so can creatures suffering hazards, the lungs,
  the hazards of carried things and the land withering.
- (Core) Radiation: a family of hazards any mod declares kinds of, given off by blocks, weaker with the square of the
  distance and weakened by every block on its way by the block's density, from the data map of block densities the
  Core fills for the game's blocks; what is everywhere in a place and what a body carries add to it.
- (Core) Ionizing radiation, in real units: a dose rate in millisieverts an hour and a dose in sieverts, from the
  sources round a body through the blocks between, so lead stops nearly all and stone about half, worn shielding
  taking its share; and contamination, radioactive dust on the skin in kilobecquerels that keeps dosing until water or
  the rain washes it off, which a suit keeps from settling. The bands of radiation sickness, Rad I to V, from nausea to
  close to deadly; slow recovery away from the sources and under the background. The sky gives off a little, more as
  the air thins and the most with none, roofs stopping it; minerals with uranium or thorium in them give off a little
  more; a mod sets a dimension's radiation belts. The Geiger counter, the dosimeter, the hazard map and the body's
  screen read it, and the land withers where it is high.
- (Core) Dissonance: where the laws of physics depart from ours (time at another pace, a pull that is not down, light
  that bends, space that wraps) it builds up in a body, from warped sight through drifting gravity, stuttering time
  and jumps through space to coming apart, and falls back in normal laws; a bubble of normal laws a mod gives keeps it
  off. It has a setting of its own.
- (Core) Pollution: kinds any mod declares, an amount over each chunk given off by blocks, which spreads, fades, is
  taken up where plants grow, harms the bodies on the chunk as a hazard of its own, does what it does to the land, and
  withers it under enough; the hazard map device measures them.
- (Core) The series' pollution: soot hangs as a haze and is breathed as smog, a cough that a mask stops; sulfur and
  nitrogen oxides fall as acid rain that kills leaves, weathers copper and calcite and stunts crops; heavy metals foul
  the crops and fish of their ground, which poison whoever eats them until the soil is dug out; radioactive fallout
  raises the dose rate and settles on whoever stands in it; and the world's carbon dioxide is one number that rises by
  what is burnt, shown on the map of hazards. Pollution has a setting of its own.
- (Core) Explosions: blasts decided by their energy, in joules and its TNT equivalent, with no radius of their own.
  The shock wave goes out at the speed of sound of the air or the water and reaches each place in its own time; it
  breaks each block by what that block stands (a window gives at a few kilopascals, concrete holds hundreds), and a
  wall it cannot break shields what is behind it. Bodies take the overpressure where they stand, hurt and thrown by
  it, and catch fire in the fireball; a casing breaks into fragments of its own material that strike as shrapnel;
  earth and stone fly as debris; explosives it breaks go off; the ground round the crater is scorched. A vacuum
  carries no wave, and water carries it far. The breaking is spread over ticks within a budget, claimed land is
  respected, and what each mod's explosions do to the world is a setting of the server's. The game's own explosions
  show the fireball, the smoke and the scorch marks too.
- (Core) Fire: the game's fire spreads, and eats what burns, by the oxygen of the air (never with too little, an
  inferno in air half oxygen) and by the wind (faster downwind, slower upwind, leaping ahead in a gale), and gives off
  smoke that rises and drifts. What burned stays burned, as a data map says. A fire's class is the worst of what feeds
  it (solids, flammable liquids, cooking oils, electrical, metals), and each agent does on each class what the real
  one does: water shocks whoever sprays live equipment, spreads burning oil and makes a burning metal burst. Sensors
  read the heat and the smoke where they hang. It has settings of its own.
- (Core) The series' fire safety: extinguishers of water, foam, carbon dioxide and dry powder, their bands in the
  colours of the European code; a sprinkler fed by a water pipe whose bulb bursts in the heat of a fire; smoke and heat
  detectors that call and give redstone; and charred logs, charred planks and ash where wood and cloth burnt. The
  series' fires give off soot.
- (Core) Blocks laid along an axis as logs are, and blocks whose every state picks its own model, for any mod's
  content; and a texture a pack draws as the game loads may be named by a mod's own models.
- (Core) Moving structures: structures of real blocks that move through the world and go on working as they move,
  machines running and crops growing, any mod's blocks among them. A structure is made of a box of blocks or of the
  blocks joined to one, and moved by rules: toward a place, spinning, swinging on a hinge, through a path, turning to
  face a direction, running on the game's rails. Entities collide with it however it is turned, stand on it and are
  carried as it moves; a player walks a moving deck as the ground, on a server too; arrows and every ray hit it, and
  its blocks are opened, used, broken and built on as blocks of the ground. Its sounds, particles (a torch's flame,
  a block's pieces), drops and news go where its blocks are, and the rain falls on it where it is. It is drawn as
  cached meshes in the light of the world where it is, as the server lights it, each face shaded as it faces however
  the structure is turned, with the glow and the coloured light of its lamps, the same under a shader pack; its lamps
  light the world round it as it goes, and its blocks change hands with the world's in a single frame as it is made
  and as it is set down, never a frame of neither. It is saved in the middle of a move, set down on the world's grid
  turned as it was, taken to another dimension with everyone aboard, and kept working far from players by a chunk
  loader aboard. Nothing of it reads a chunk that is not loaded: a structure made where its plot of the yard has not
  loaded yet keeps its blocks on the ground for the few ticks it takes, and one sent to another dimension goes once the
  land it goes to has loaded. Blocks that remember other blocks are told where they moved, and a block entity's fields
  follow on their own, with `whenMoved` for the rest. The Core's own kits work aboard: a formed multiblock crosses
  formed and stands formed set down turned, cables, mechanical networks, spotlights, peripheral links, radio relays,
  the Atmosphere Detector and radiation leave nothing where they stood, and an overhead wire goes with the two
  insulators it hangs from. It has commands and a setting of its own.
- (Core) Moving structures by their physics: a structure let go moves as a body of real mass, its mass, centre and
  inertia following every block set on it or broken off it. It falls under the pull of the place, a dimension's pull
  thinning with height where its rules say so; the air, as dense as the height makes it, and the wind drag its faces;
  its engines push (rockets anywhere, jets and propellers by the air), its wings lift and stall, its wheels hang on
  springs and grip, steer, drive and brake by their signals, a car on a ship's deck as well as on a road; it floats
  by the blocks under a liquid and by the air its hull holds, so a ship floats as a real one does, and settles; its
  sealed rooms lift as balloons, of hydrogen or helium or of air warmed by a burner, and climb until the thin air
  lifts them no more; levitators hold it up and magnets pull it to the iron of the world; a face heading fast into
  the air heats, and what burns on it catches fire. It meets the world's blocks and other structures' by their real
  shapes, ahead of time so a fast one stops where it touches, rests still on many points, slides as the blocks'
  slipperiness lets it, bounces a little from a hard blow, pushes and is pushed by another structure moving by its
  physics, rides one moved by a rule, and breaks a block whose blow was more than it holds. Still for a second, it
  falls asleep and costs next to nothing, and wakes as soon as anything about it changes. What each block is, its mass
  and the forces it gives, is the data map `jscore:block_physics`; blocks of iron are `#jscore:magnetic`; the command
  `/jstech structure physics` lets a structure go; each structure's tick of physics is timed in the diagnostics, and
  a dimension's physics takes no more of a tick than the setting `moving_structures.physics_millis` lets it, past
  which a structure waits a tick.
- (Core) Rooms aboard moving structures: a sealed room aboard has round it the air of where its structure is, taken
  again each second, so a cabin climbs into thin air and a sealed balloon swells until its walls burst; a hole in a
  room aboard pulls whoever is in it towards the hole as its air rushes out.
- (Core) Joints between moving structures, and to the world: ropes and chains that sag with their slack, springs,
  tow bars, hinges and sliders that turn and slide within limits and can be driven, ball joints and welds, each
  holding as a real one does and breaking past the load it is given, a winch reeling a rope in and out by its signals.
  Structures joined move by their physics together, and every player sees the ropes, chains, springs and bars drawn
  between their ends.
- (Core) Structures held by structures: a turret on a ship, a cab on a crane, turned on its parent by a rule of its
  own and going wherever the parent goes; a parent moving by its physics carries what it holds as part of its body,
  its mass, its engines and its wheels its own, and lets it fall when the block holding it breaks.
- (Core) Structures parting and joining: one whose middle breaks out becomes as many structures as it now is, each
  going its own way at the speed it had, unless the setting `moving_structures.split` holds them together; a decoupler
  parts a rocket's stages at a signal and pushes them apart; two structures square to each other join for good.
- (Core) Docking: docking clamps meeting face to face at a signal take hold of the other structure where it is and draw
  it in square over a second or so, however the two go, their momentum shared; at the signal's end they let go, the
  visitor pushed gently away.
- (Core) Tools on the move: a drill mines, a harvester takes ripe crops and plants them again (a melon or a pumpkin
  whole, sugar cane, cactus and bamboo above their foot), a plough tills and a placer sets the blocks it carries,
  each as the structure passes, into and out of the containers aboard. Seats, helms, clamps, decouplers and tools are
  the data map `jscore:block_aboard`.
- (Core) Living aboard: a player sits on a seat aboard and is carried with it, steady in their seat; at a helm they
  steer the structure with the keys they walk with, its engines and wheels answering by the way each looks; an
  autopilot flies a structure to a place, at a height, along a route or to a docking clamp, any structure alike, by
  how fast its engines can speed it up; an arrow loosed from a moving deck flies on with it; a bed aboard is slept
  in as one on the ground, its sleeper carried and drawn along it however the structure is turned, and is the place
  its sleeper comes back to; and a player takes hold of a rope and rides it down.
- (Core) What a moving structure shows going: its engines' flames, a rocket's or a jet's, as big as they push; its
  faces glowing hot from the air they head into, in the colour of their heat, wrapped in plasma on a re-entry; and
  white trails behind its engines and wingtips where the air is cold enough. An engine pushing keeps its structure
  awake, so a rocket too heavy for its engines stands burning on its pad.
- (Core) The diagram of forces: a key, K, opens for the moving structure the player stands on, sits in or looks at
  the structure itself over the ground below it, with an arrow for each force where it acts and for their sum at its
  centre of mass, its height and its axes; each force listed with what it gives and a switch that hides its arrow; and
  its mass, the sum's way, its climb, its speed and the air round it, seen in 3D, from the side or from above.
- (Core) Physics seen: a block's tooltip shows its mass and the forces it gives while the diagram's key is held, or
  while an item of `#jscore:sees_physics` is held or worn; another key shows in the world the box, centre of mass and
  forces of every structure near.
- (Core) The physics tool: it grabs a moving structure where it is aimed and holds it before the player, pushes it
  with a blow, or freezes it where it stands and lets it go, as strong as it is set, its mode and strength changed with
  the wheel; its panel shows both, and while it holds a structure a beam runs to the point it holds. It is made at a
  crafting table while the setting `moving_structures.physics_tool_recipe` says so, by
  `jscore:crafting_shaped_switch`, a shaped recipe a setting turns on and off that any mod can use.
- (Core) Every force a setting: gravity, drag, lift, thrust, buoyancy with the balloons, levitation, magnetism and the
  wind on moving structures each have a setting that turns them off, and the harm of blows one of its own.
- (Core) Moving structures by hand and by ear: `MovingStructures.push`, `freeze`, `hold` and `letGo` move a structure
  as the physics tool does, and a `MovingStructureEvent` on the game's bus tells of a hard blow, a parting, a docking
  and a letting go.
- (Core) Reading a flight: `StructureReadings` gives what a sensor aboard reads, its altitude, the air's pressure, its
  speed, climb, pitch, roll and heading, and how far the ground is below it and the first block ahead, as readings a
  sensor speaks in redstone and to programs.
- (Core) Propellers on shafts: a propeller's thrust can follow a shaft, `"shaft"` in its physics, growing with the
  speed the shaft turns it.
- (Core) Aboard as on the ground: what a block aboard asks of the air, the weather, the wind and the biome's warmth is
  answered for where its structure is in the world; a spotlight aboard shines on the world from where it is, sweeping
  the ground as the structure goes; a radio aboard is heard from where the structure carries it; and belts, spaces of
  parts and shafts work aboard, in another dimension too.
- (Core) Moving structures beside Create and Create Aeronautics: with Create installed, a moving structure meets its
  contraptions as obstacles that move, and lands and rides on one turning on a bearing; with Sable, which Create
  Aeronautics flies, it meets its ships the same way, as fast as they fly. Create moves no block a moving structure
  keeps, so a bearing or a ship's assembler never takes one out of it; a moving structure is never made of a ship's
  blocks; and a place aboard a ship is asked of the world where the ship has it. The work starts only on the versions
  of Create and Sable it is tested against, and says in the log when it is left off. Another mod tells the kit of its
  own moving structures through `ForeignMovers`.
- (Core) Vehicles as data: a vehicle is a kind, its seats, the places of its parts, its boot, its cargo tanks, its
  paints, its shapes and its key, and the parts fitted in it, engines, wheels, tanks, batteries, gearboxes, lights,
  horns, rotors and propellers; kinds, parts and fuels are declared by a mod or written by a data pack, with no code.
  It goes by real physics: an engine's power through a gearbox that changes gear by itself or by hand, held to what
  its tyres grip, less on ice; leaning over its wheels, sliding wide in a fast turn, slowed by the air and the road,
  on the road, in the air on its rotors, or afloat on its propellers. It burns the fuel its engine burns, a liquid or
  a gas, or draws on its battery. Its parts wear against a wall, on a hard landing, from heat, and a holed tank
  leaks; a wrench takes them out and mends them, a dye paints it. It runs over what stands in its way. Seats have
  roles; its owner and whoever carries its key drive it, and its key locks it. A trailer hitches behind and follows.
  Its boot and tanks are reached by hoppers and pipes, and a fuel pump fills every vehicle beside it. Its lights
  light the world; an engine or a horn that names a sound sounds it, the engine as it turns and works. Parked on a
  moving structure's deck it goes with it, a player drives it on a deck in flight as on the ground, and through a
  portal it takes its riders along.
- (Core) Robots that fly, swim and think: a mod's robot walks, flies in three dimensions or swims, carries as much as
  its size lets it, and digs with whatever tool it carries, of any mod, at a player's pace, the tool wearing and its
  enchantments working on what drops. One with a battery spends it getting about and digging, stands still when it is
  empty, and recharges at a charging station (`RobotCharger`, for a mod's block). Its tasks repeat, choose by a
  condition, wait for one or for a time of day, and find the nearest block of a kind to go to or break; its conditions
  sense what it carries, blocks, items and creatures near, the time and its charge. `Robots` finds robots by their id
  and their owner, gives them a program written as data and says how they stand, and the end of each task is told on
  the Core's events, for J's Computers to program them.
- (Core) Creatures as data: a mod's `CreatureEntity` lives as its data says, its health, speed and attack, what it
  does in order of what matters most (wandering, panicking, avoiding, following food, fighting), where it appears by
  biome, dimension and placement, and the air it breathes: it appears only where that air suits it, and chokes out
  of it. A data pack changes how a creature lives with no code.
- (Core) Projectiles by real ballistics: a projectile falls by the pull of the dimension it flies in; one with a mass
  is held back by the air's density where it flies, none in a vacuum, and by water, and is carried by the wind; one
  that gets through blocks passes a block as hard as it can still get through, slowed by it.
- (Core) Modules in worn things: a worn item with sockets takes modules, each into a socket of its kind, and each
  module on does what it does for its wearer (changes an attribute, gives an effect), spending the energy the item
  holds; hosts, modules and their effects are declared in code or by data packs.
- (Core) Tools that mine an area or a vein: a tool declares, mode by mode, an area of the face it breaks into (three by
  three, five by five, with depth) or the vein of the block it breaks, the same block or the same ore; the rest breaks
  with it as the player would break each block, its drops, the tool's wear and every protection's say included,
  leaving what the tool is not made for, what is far harder than the block broken and what the player may not break.
  The server sets the most blocks broken in one blow and how much harder they may be.
- (Core) Tools on energy, fuel or gas: a tool spends the energy it holds, a fuel it burns from the fluid it holds, or
  a gas from its tank, each block and each use; out of it, it does no more than a bare hand. A tool that heats
  overheats and cools down by the world's time, one held to charge acts let go with how charged it was
  (`ChargingItem`), and one that waits between uses shows the wait over its icon.
- (Core) Building tools: a tool lays copies of a block out along a line or over a plane from the face it is used on,
  copies the region between two corners and pastes it turned as the player has turned, every block from the player's
  inventory or from a supply a mod adds (`IBuildSupply`, for J's Computers' network), placed where the player may
  place it.
- (Core) Modules in tools: a tool whose modules work while it is held takes them as worn things do, and they make it
  mine an area or a vein, work as though enchanted (silk touch, fortune), or change the reach and speed of the hand
  holding it, spending its energy each use.
- (Core) What a tool shows: holding the mode key opens a ring of the item's modes, chosen with the mouse; the item's
  name above the hotbar says its mode and what it works on; bars under the crosshair show a tool's charge and heat;
  an icon carries a bar for each thing the item holds and its mode's letter; and in the world a tool outlines every
  block its area or vein will take, and a building tool shows the ghosts of the blocks it will place.
- (Core) The camera: lenses that magnify by real powers (`ZoomItem`, or a block through `CameraZoomClient`), the
  mouse wheel stepping between them and the mouse turning as much slower; the view turning with a ridden vehicle and
  rolling with it or with a moving structure's seat; free look, the view turned and the player not; scenes along
  camera paths declared in code or written by resource packs, the view of an entity and the view from a point, which
  the server starts and the player skips by sneaking, their ending told on the Core's events; and cameras in the
  world whose view a screen shows (`CameraFeeds`), drawn before the frame at a rate, a width and a number at once the
  player sets.
- (Core) Items: an item holds a gas, in a tank of a volume at a pressure the Core's gas capability reaches; a key works
  an item wherever its player carries it, in a hand, worn or in the inventory, and turns one on and off
  (`ItemActions`); an item lasts as many uses as declared, or as many as its mod makes of a property of its material;
  the mouse wheel turned while sneaking changes the mode of the item in hand.
- (Core) Tooltips: each mod's lines kept together, the item's own mod first and the others by name, and the item's mod
  named last in blue unless another mod already names it (`ItemTooltips`); parts shown only with Shift (what an item
  does) and with Ctrl (its numbers), a hint at the foot saying which key shows what; bars of energy, gas, wear and a
  fluid in its own texture (`TooltipBar`), pictures (`TooltipPicture`), turning 3D previews of a block or a structure
  (`TooltipPreview`) and what an item holds in the slots of the game's bundle (`TooltipContents`). Items that hold
  things show their bars and slots, and a worn item its wear, in every tooltip; the player can turn the mod's name and
  the wear off in `jstech-tooltips.toml`.
- (Core) The keyboard, a controller and the narrator on screens declared in files: Tab and the arrows move among the
  components in the order they read, Enter and Space press, a controller moves among them as among the game's
  buttons, and the game's narrator reads each in the game's own words; a file names a component for the narrator with
  `narration` (`KeyboardFocus`, `UiComponent.narration`).
- (Core) Less movement for a player who asks, in the Core's own settings (`jstech-accessibility.toml`): what slides,
  turns round or blinks on the screens of the series is drawn still, and a tooltip's 3D preview stands still.
- (Core) The HUD editor: every mod's HUD elements in dashed boxes over the dimmed game, dragged where the player wants
  them and settling against each other or on a grid, resized by the chosen box's corners and hidden by its eye; Reset
  all, Cancel and Done. Opened from the settings screen or by its key; what the player did is kept in
  `jstech-hud.toml`, and an element placed leaves its corner's stack and stays by the corner nearest it.
- (Core) Screens in the world that answer the crosshair: a block declares which face shows a screen, where on it and
  its pixels (`ScreenTouches`, `ITouchScreen`); a screen is built of the Core's components (`TouchPanel`), the one
  under the crosshair lit as the mouse lights it, and while the crosshair rests on a button the action bar says what a
  right click does ("Right click: Start"); a right click presses the screen there on the server, by the pixel and by
  the name of the part pressed (`TouchPress`).
- (Core) The size of the text apart from the GUI scale, in the Core's own settings (`jstech-accessibility.toml`, Text
  size: Normal, Larger, Largest): each step draws a letter's pixels a screen pixel larger, so it stays sharp. The
  screens declared in files, the settings screen and the notifications grow with their text, while the icons, frames,
  slots and the game's buttons keep the GUI scale (`TextScale`). A text bound to a value that grows past its box lays
  its screen out again instead of running past the window's edge.
- (Core) Screens declared in files draw less: their window and what never changes are drawn once into a picture laid
  down each frame, and only what moves is drawn over it (`UiComponent.still`, `StillLayer`).
- (Core) Tools for building screens declared in files, in a development run: F12 opens the inspector, with the tree
  of what the screen is built of, the problems found (a text with no translation, no words for the narrator, an
  action nobody handed over, overlaps) and a card of the component under the pointer with its file and line, which
  Ctrl+C copies; it draws the screen at another GUI scale, in the other language and in each state its code offers
  (`ScreenWiring.preview`). Ctrl+E opens the editor on the screen, which moves, resizes, adds, copies and takes out
  components and writes each change to the mod's source at once, with Undo and Redo; `/jstech screen new` makes the
  file of a new screen for the block looked at, from a machine's screen, a tab, a HUD element or a copy.
- (Core) Manuals: scenes that explain step by step: what each step puts in and takes away is data (`step`, `take`),
  its sentence stands over the picture, and a timeline under it has the step before, play and pause, the next step
  and a mark for each step. Every scene has an In world button that shows the step, or the whole scene, as ghosts in
  front of the reader through the Structure Projector.
- (Core) Manuals: a building saved with the game's structure block is a scene of a page, read from the mod's assets
  (`structure`), and a picture can move, its frames shown in turn as an animated texture's (`animation`).
- (Core) Manuals: bookmarks and notes: ribbons of three colours hang over the pages they mark, listed under their
  button beside the search with "+ Mark this page"; a right click on a page writes a note in its margin, on yellow
  paper. They are the player's own, kept by where their page stands in its entry, so they stay on their words in
  another language.
- (Core) Manuals: the contents say "NEW" beside what came into the book since the player last read it and "not yet"
  beside what their team cannot do yet (`needs`), and a chapter's tab holding something new has a gold dot.
- (Core) Manuals: the search finds the recipes the pages show, those making what was typed and those it goes into,
  each with what it makes and what works it. With JEI or EMI installed, the manual key held over an item in the
  viewer's lists opens its page, and a click on a recipe in a page opens it in the viewer, which comes back to the
  page when it is closed.
- (Core) Manuals: in a development run, a manual's file saved is read again at once, alone, and the manual open lays
  itself out anew with its reader on the page they were reading. `/jstech manual site <manual>` writes a manual as a
  website of plain files, its front page with the contents and a search that runs with no server, a page an entry,
  its recipes in words, in the player's language (`GuideSite`).
- (Core) Manuals: a page torn out as a Torn Sheet, a copy the manual keeps, from a button on the binder's top edge; in
  an item frame the sheet shows its page as a map is shown, and its tooltip says where it was torn from.
- (Core) Manuals: every mod's manuals go on the game's lectern as its books do, and are read from it with a strip
  under the binder saying where the lectern is, with Take and Close.
- (Core) Manuals: the first time a player picks up an item a manual explains, a quiet notification offers its page,
  once an item; the player's own settings (`jstech-manuals.toml`) turn it off, or on in creative too.
- (Core) Manuals: the Technical Reference has a section on things that move, with the pages of the vehicles and
  their parts, and of the physics tool.
- (Core) Content packs: blocks, items, materials with their forms and ores, kinds of processing machine and the
  controllers of machines declared as data, written in JSON files under `jstech/content/` in the game's folder or
  `jstech_content/` in a mod's, and registered as the game starts with no Java, each namespace with its creative tab.
  The Core makes their English names, block states, models, drops and tool tags as the game loads, under every pack,
  so a resource pack or a data pack can replace any of them; a file that cannot stand stops the game loading with a
  paragraph saying which file, which declaration and how to put it right.
- (Core) Conditions on data: `jscore:setting` loads a recipe, a loot table or any data NeoForge loads with conditions
  only while a setting has the value asked for (a switch, a number, a word or a list), reading a world's setting from
  the world's own file from its very first load.
- (Core) Crafting recipes that ask for knowledge: `jscore:crafting_shaped_knowledge` and
  `jscore:crafting_shapeless_knowledge` craft at the crafting table and the inventory's grid only for a player whose
  team knows what they ask, never where no player is at the grid, and show in JEI, EMI and the manuals with a
  padlock while the team does not know it.
- (Core) Unification: when several mods make the same material, one item stands for all of them, the game's own first
  and the Core's next by default, in every recipe of every mod and in what blocks, creatures and chests drop; the
  world's `jstech-unification.toml` says which mods come first and which tags, items and recipes are left alone, and
  it steps aside for Almost Unified.
- (Core) KubeJS: scripts reach the Core as `JsCore` (teams, what they know, milestones, steps along an axis,
  materials and their properties, which item stands for which, every kind of data the Core's registries read and
  every mod's data maps, as JSON), hear its events under `JsCoreEvents` (knowledge gained, milestones and steps
  reached, the life of Operations, machines joining and leaving data networks, sealed rooms, moving structures, and
  a dimension's hazard, which a script cancels), and write a recipe of any mod's processing machine with a function
  under the machine kind's id. The Core is made before KubeJS, so its bindings are there for startup scripts.
- (Core) Data networks: a machine joining or leaving one is told on the game's bus, `NetworkMemberEvent.Joined`
  and `NetworkMemberEvent.Left`.
- (Core) Reloading clearly: after `/reload` the server's operators and the log are told how many recipes and loot
  tables the data holds, what the unification made of them, and what waits for a restart: content packs changed on
  disk, and settings that take hold only then.
- (Core) `/jstech dump materials`, `forms`, `recipes`, `tags`, `hazards`, `ids`, `reference` and `all` write what
  the game holds to `jstech-dump/`, each as a page of Markdown.
- (Core) The generated reference: `/jstech dump reference` writes a website to put online with every kind of data
  the Core reads, every mod's data maps and every kind of recipe, each with a real example the game's own codecs
  wrote, the Core's conditions, what content packs declare, and every id.
- (Core) Ores in other mods' rocks: every material with an ore in the game's Overworld or Nether rocks has one in the
  rocks other mods bring to the same world, found by the common tags with no list kept by hand (Create's limestone,
  crimsite and the rest among them): as hard as the rock and a little harder, named after it ("Tin Ore in
  Limestone"), filed with the material's ores, dropping what they drop and laid by the veins. `jstech-variants.toml`
  turns it off, or leaves out mods, rocks and materials.
- (Core) The materials' art is drawn once and kept on disk under `jstech/cache/`, read back on the next load while
  nothing it is drawn from changed; an ore in another mod's rock is drawn on the texture the rock's own model names.
- (Core) Variants of other mods' materials: a mod declares a kind of variant once (`Variants.kind`), and one is made
  for every wood, stone or metal of every installed mod, found by the common tags (`minecraft:planks`, the world's
  stones, `c:storage_blocks` with their ingots): it copies its material's block, is named after it in the player's
  language ("Oak Frame"), is drawn with the kind's model and the material's own texture, drops itself and is in its
  tool's tag. `jstech-variants.toml` leaves out mods, materials and kinds, or all of them.
- (Core) Changing drops as data: the modifiers of loot `jscore:add_drops`, `jscore:remove_drops` and
  `jscore:swap_drops` add, take away and swap drops, with the game's conditions of loot (the table, the tool, the
  dimension, the biome) and NeoForge's on the file.
- (Core) Changing recipes as data: files under `jstech/recipe_changes/` in a data pack take recipes away (by id, all
  of a mod's, by what they make or by their kind), put one item for another in every recipe, and put a recipe in
  another's place, before the game reads them, so recipes of every mod and kind are reached; a file may carry
  NeoForge's conditions.
- (Core) Choosing between recipes: when more than one recipe fits a crafting grid or a furnace's input, a chooser
  stands beside the result, a click for the next, a right-click for the one before, its tooltip naming each; the
  choice is kept with the player for the grid and with the furnace for what it smelts. The world's
  `jstech-recipe-choice.toml` turns it off, and it steps aside for Polymorph.
- (Core) CraftTweaker: `mods.jscore.Processing` adds a recipe of any processing machine, `mods.jscore.Core` reaches
  the same as KubeJS's `JsCore`, and the Core's events are heard under `mods.jscore.event`.
- (Core) FTB Quests: quests have the Core's tasks `jscore:knowledge`, filled as a team learns a piece of knowledge,
  `jscore:milestone` and `jscore:axis_step`.

### Changed
- (Core) Vehicles: the one entity `jscore:vehicle` of a kind given as data takes the place of a class extending
  `VehicleEntity` with a `VehicleSpec` for each vehicle; `VehicleSpec` is gone. A robot still drives one with its
  `Drive` task.
- (Core) A robot's inventory is as big as the robot, and `RobotEntity.INVENTORY_SIZE` is gone; a robot as wide as a
  player and two thirds as tall still carries nine stacks. `ProjectileSpec` gains a mass, a drag area and what it gets
  through, its old constructor kept.
- (Core) A host of modules says whether its modules work worn or held (`ModuleHostDef.worn`, `ModuleHostDef.tool`, or
  `"held"` in data), and a module's energy is spent each second worn, each use in a tool. `ItemState` gains a gas
  tank. A module effect does nothing where it has no part, in what is worn or in a tool, unless its kind says.
- (Core) The mode key no longer writes the new mode on the action bar: the item's name above the hotbar shows it.
- (Core) What an item holds is shown in bars and slots rather than in lines, how to change its mode moves to the part
  Shift shows, and the Core's own lines of an item (its material, hazards, physics, the manual's key) are a section of
  the Core's tooltips, kept together. A slider is stepped with left and right only, as the game's are, so up and down
  move the keyboard on; buttons and check boxes of a screen's file are reached by the keyboard.
- (Core) The world drawn from somewhere else, the far side of a seamless portal, is drawn by `WorldViews`, which
  cameras on screens draw with too; the portal shows what it showed.
- (Core) Telemetry leaves the peripheral links: the point-to-point links are the computer's peripherals and the
  industrial control link; telemetry becomes a line of its own, which J's Space lays.
- (Core) Two layers: what is the series' own (the eras and tiers, the data network's model, the Operations, J's
  Energy, the shared content) gets a package of its own, `dev.jstech.core.jstechseries`, and the rest of the Core,
  the platform any technology mod can build on, never reaches into it. What is new of the series is born there; what
  the Core already has of it moves there a domain at a time.
- (Core) Listeners declared: every class of the Core that answers the game's events is handed to the buses from the
  Core's constructor, through a `listen` method of its own, rather than found by an annotation, so what runs is what
  the constructor says.
- (Core) Integrations behind one guard: the work with Mekanism, FTB Teams, Accessories and Curios starts through
  `OptionalMod`, so one of them failing to start no longer stops the game.
- (Core) The style chosen for J's Computers' screens, or for every screen, is worn by its computing blocks' screens
  too, in place of their era's.
- (Core) A setting can be picked on a chooser of its own, which shows what each value means, from the settings
  screen.
- (Core) Using what others own: the permission that let a player pass every owner, `jscore.ownership.bypass`, is
  now four, `jscore.owned.use_others`, `open_others`, `set_up_others` and `break_others`, and changing a server's
  settings from afar asks `jscore.config.edit` rather than the operator level alone.
- (Core) Menus in every style: a style draws a menu's ground, its lists and its footer's line, and gives a menu's text
  colours of its own (headings, notices, links, warnings), each falling back to a window's; what is typed in a field
  takes the style's field colour in every style.
- (Core) The fixed catalogue of five metals in seven forms gives way to the open one. Iron dust, iron plate and copper
  plate keep their ids and are things of the open catalogue, drawn as the game loads instead of from textures of
  their own; their common tags come from the Core's data made as the server loads, no longer from its generated files.
- (Core) Loss by distance in millionths: a cable's loss is now counted in millionths of what crosses each block, so a
  line can lose as little as a hundredth of a percent over a hundred blocks; a cable that lost a tenth now says
  `loses(100_000)`.
- (Core) Machines short of energy work slower in the same proportion, spending what there is, instead of waiting for a
  whole tick's energy; looked at, they say they run on low power.

### Fixed
- (Core) A dimension made while the game runs keeps loaded again the chunks it kept loaded, each time it is made
  again; it held them as kept but loaded none after the server restarted.
- (Core) A multiblock's controller lifted whole, as by a piston or by a mod that moves blocks as Create's
  contraptions do, is moved, not broken: its parts are no longer taken down where it stood, and what it holds no
  longer drops.
- (Core) A formed machine moved by a mod that does not tell it where its blocks went forgets the places it stood at
  and finds its structure again where it is, where it could stop the server reaching across the world for them.
- (Core) A fire lit in a chunk that went away before the sealed rooms looked at its oxygen no longer loads that chunk
  again to look.
- (Core) What reads the world only where it is loaded (a block entity read at the place it is remembered at, a fire
  spreading, the sealed rooms' fires) no longer reads a chunk still on its way to loading, which made the server wait
  for it in the middle of a tick.
- (Core) A chunk let go and wanted again before it unloaded, then needed by the generation of a chunk beside it, no
  longer holds the server: the game's unloading of it went round and round for all the server's spare time, so the
  generation never went on, new land never came and the server could not stop. The Core drops that unloading, as
  the chunk is kept; this was the game's own, made likelier by anything that loads chunks a while and lets them go.

## [0.5.2a] - 2026-10-14

Codename: Boron.

### Changed
- (Computers) Racks and servers: Every storage server has two expansion slots, so its storage card has room beside a
  graphics card, which a board with no video of its own needs for a monitor. Only the Legacy storage server had two;
  the others had one, and a graphics card in it left no room for the Vaultis Array or the Vaultis Cache.

### Fixed
- (Computers) Redstone Interfaces: A Redstone Interface placed facing up or down stands in its own block, its lens
  toward the block above or below it, without its feet. It turned on the bottom of its block instead and lay half
  inside the block under it. Its back is closed too: the gland where the cable plugs in and the bends of its wires
  showed their insides.
- (Computers) Cases and parts: The fans of the cases, the coolers, the processors and the graphics cards no longer
  flicker where their blades cross, and a card or an M.2 drive no longer flickers along the edges of its slot. In the
  Advanced High Performance and Aesthetic cases, the frames of the two front fans no longer flicker against the water
  cooler's radiator behind them, and their outer sides are closed.
- (Computers) Buses: The arrows on the External Storage Bus no longer flicker where the two of them meet.

## [0.5.1a] - 2026-10-14

Codename: Boron.

### Changed
- (Computers) Hardware: A motherboard's tooltip says whether it drives a screen by itself: a server board has a screen
  output of its own, for its console, and every other board needs a graphics card, or a processor with graphics, for
  a monitor. The manual's page on motherboards says so too, and what to do when a card will not go into a server.

### Fixed
- (Computers) Programs: A server in a rack runs Σ# programs as any computer does, each server its own: `sigma run`
  at its prompt starts one there, where it said the machine could not run programs, and a script runs on by itself,
  listed and ended by its number with `sigma ps` and `sigma stop`. What a server runs is kept on its Server item with
  the rest of its run, so it is still running when the world comes back, and it ends when the server is switched off,
  restarted or pulled out of its rack. A server's programs reach its drives, its network and the operations they ask
  of it; Gateways, Redstone Interfaces, sound and windows are still a computer's alone.
- (Computers) Cases: The water cooler of the Advanced High Performance and Advanced Aesthetic cases lays its radiator
  across the two fans at the front, behind both of them, with its tubes coming down to it from the pump. It stood on
  end behind one of the fans, well above and below them.
- (Computers) Assembly: Once a board is in, the window of a computer or a server draws only the slots the board and
  the case wire. The others were drawn dimmed and looked like slots waiting for a part, but took none, so a card held
  over one went nowhere without a word: a storage server whose one card slot held a graphics card seemed to have five
  more. Before a board goes in, every slot still shows dimmed where its part goes.

## [0.5.0a] - 2026-10-13 - The Start Me Up Update

Codename: Boron.

### API
- (Core, Computers) Versions: J's Core's API is at version 4 and J's Computers' at version 7. Everything this release
  adds to either, or changes, carries `@ApiStatus.Experimental`: it keeps the mark through this release and loses it
  when the next cycle begins. Each mod's API is now kept line by line, every type and member a mod can reach with the
  version that brought it, and the tests fail when the code and that list disagree; `docs/API.md` says how.
- (Computers) Added: `ComputersRegisterEvent.graphicsProgram` and `JsComputersApi.registerGraphicsProgram`: a program
  whose windows draw through a surface says so on both sides, so the server counts the video memory they hold too. An
  ISA or a file opener registered after the game has loaded is refused with a line in the log.
- (Computers) Added: `IsaSpec`, `ComputersRegisterEvent.isa` and `JsComputersApi.registerIsa`, which take over from
  `ArchitectureSpec`, `architecture` and `registerArchitecture`.
- (Computers) Added: `EngineDef`, `EngineCapability`, `ComputersRegisterEvent.engine` and
  `JsComputersApi.registerEngine`. A mod registers a Network Operations Engine by its package (a program registered
  like any other), the dialect it speaks, the version it ships in for each age of Mainframe and the extras it offers;
  until it brings a planner of its own it plans the way the Midsoft IQL Server does. Its package is of the new kind
  `ProgramKind.NETWORK_ENGINE`.
- (Computers) Added: the package `dev.jstech.computers.api.planner`, for the engines that take extensions (NextgreIQL
  among them): `IPlannerRule` weighs every plan the planner considers and is switched on or off per Mainframe;
  `IPlannerOperator` is a hint the dialect accepts after a statement; `IPlannerStatistic` shows among the planner's
  statistics and may say how long a step takes; `IExplainNode` adds notes under a plan's steps. A plan is a
  `PlanCandidate` of `PlanStep`s, whose cost a rule or a hint may change or which it may set aside, saying why. They
  are registered with `ComputersRegisterEvent.plannerRule`, `plannerOperator`, `plannerStatistic` and `explainNode`
  (or the `JsComputersApi.register...` methods of the same names).
- (Computers) Added: `ProgramRequirement` and `ProgramSpec.requires`, with `requiring`: a program says what it needs
  of the network's engine (the engine it is written for and the oldest version of it, or the capabilities it uses),
  which is checked when it is opened rather than when it is installed.
- (Computers) Added: kinds of component for Σ# programs' windows: `ComponentKind` and `IComponentValidator`,
  registered with `ComputersRegisterEvent.componentKind` (or `JsComputersApi.registerComponentKind`) in the adding
  mod's namespace; on the client, `ComponentRenderers.register` with an `IComponentRenderer`, which reports what a
  player did through `IComponentActions`.
- (Computers) Added: programs with windows written in Java: `DesktopApps.register` with an `IDesktopProgramFactory`
  making a `DesktopProgram`, which draws itself or through a `SurfaceRenderer`, for whoever opened it; this replaces
  registering a window factory with the mod's own desktop code.
- (Computers) Changed: what a program declares with `ProgramSpec.withRam` is what it holds on a machine of the Legacy
  era, grown on the later eras and shrunk on the Vintage; `ProgramSpec.legacyMb()` says it, with what a program of its
  kind holds when it declared nothing, and `ProgramSpec.ramMbOn(HardwareEra)` and `ramMbOn(HardwareEra, boolean)` say
  what it holds on a machine of that era. The forms of `ramMbOn` that took the system an `OsDef` names are gone.
- (Computers) Changed: `OsDef` says its shell with a `ShellKind` (`shell`, where it had the id `shellId`), and so do
  `linuxDistro` and `terminalSystem`. `ProgramSpec` has a `description`, `of` no longer takes the display name
  (`named` and `described` give the English words), and `name()` is the name to translate. A
  `DesktopEnvironmentDef`'s native names are `TextKey`s, and so are what `nameOf` and `launcherLabel` give.
- (Computers) Changed: `ComputerTerminalMenu` stands on the Core's `CoreMenu`, which now holds what it had from the
  menu it stood on before. Its layout constants (`INV_X`, `INV_Y`, `HOTBAR_Y` and the `STORAGE_` ones) and
  `storageSlotCount()` are gone: `invY()` and `hotbarY()`, with the new `invX()`, give where the inventory is drawn.
  `openingTab` says which tab a terminal opens on.
- (Computers) Changed: an operating space's screen is handed an `IOperatingSpace`, a narrow face of the machine (its
  menu as the game's kind of menu, the machine, the monitor, the space and the era), in place of
  `ComputerTerminalMenu`, which is no longer part of the API. `IOperatingSpaceScreen.open` takes it and gives back the
  screen built on its menu.
- (Computers) Deprecated: `ComputersRegisterEvent.architecture`, `JsComputersApi.registerArchitecture` and
  `ArchitectureSpec`. They still work, handing what they are given to the instruction set registration, and go in the
  next cycle; `ArchitectureSpec.toIsa()` gives the same values under the new name.
- (Computers) Removed: `ArchitectureSpec.runs` (ask the `IsaSpec` from `toIsa()`), and
  `DesktopEnvironmentDef.programFor`.
- (Computers) Changed: its advancements stand on J's Core's event trigger, `jscore:event`, with the events named in
  the mod's namespace (`jsc:post_passed`); `JscEventTrigger`, `JscTriggers.EVENT` and the mod's own `AdvancementTab`,
  `AdvancementSpec` and `ConditionalAdvancementProvider` are gone.
- (Industrial) Changed: its machines stand on J's Core's; `AbstractMachineBlockEntity`,
  `ProcessingMachineBlockEntity`, `MaceratingRecipe` and `CompressingRecipe` are gone, and its two kinds of recipe are
  J's Core's processing recipes.
- (Core) Added: `GuideBlockRenderers` and `IGuideBlockRenderer` in J's Core's client API: what draws a mod's own kind
  of block on a manual's page, or a picture drawn as the page is shown, registered once from its client setup, handed
  the block's data as a tag.
- (Core) Added: a build can ask a language for an instruction set and a version of the language at once, with
  `IProgrammingLanguage.compile(sources, CompileOptions)`; a language without versions builds as it did for the
  instruction set alone, which is what the method does unless overridden.
- (Core) Changed: `IProgrammingLanguage.Complaint` carries its message as a `Text`, read in the language of whoever is
  shown it, with the `arguments` the message was written around; `text()` is the line a person reads, and `format()`
  is still the same line in English.
- (Core) Added: J's Core's client API, `dev.jstech.core.api.client`: `ISkin`, the look a mod draws through inside a
  machine's screen (moved here from the Core's own GUI package); `SurfaceRenderer`, what draws frames onto a surface
  of its size, asked no faster than its frame cap and only while it is shown; and the two surfaces, `PixelSurface`, an
  array of colours uploaded when it changed, and `GpuSurface`, a render target drawn into with the game's rendering.
- (Core) Added, outside the versioned API for now: the parts of a general library for technology mods.
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
- (Computers) Eras: Two hardware eras join the ladder: the Transition, between Legacy and Standard (the late 2000s,
  64-bit, blue BIOS, flat panels, systems and programs on DVD), and the Advanced, after Standard. Each has its own look
  on the computing screens: the Transition in navy glass, its headers, buttons and selected tabs in two bands over a
  line of sky blue; the Advanced light and flat, white panels on grey under a thin blue line. Both palettes can be
  recoloured by a resource pack like the others.
- (Computers) Eras: Blu-ray discs, the Advanced era's medium: the BD-ROM and the rewritable BD-RE, each holding four
  times a USB stick. Advanced programs ship on Blu-ray, and its systems and services on the stick.
- (Computers) Hardware: The Vintage era fills out, so a weak machine and a top one can be built in it: the Velocion
  5x86-133 on Socket 3; the Integra Pentix 75, 133 and MMX 233 and the Velocion K5 PR133 on Socket 7; the Integra
  Pentix Pro 150, 180 and 200 on Socket 8; and, on Slot 1, the Integra Celer 300A, Pentix II 300 and 450 and Pentix III
  600, with the MF AT Slot 1 Motherboard, the top of the era, that takes them with SDRAM and the first AGP. New cards:
  the Atrion Wonder VGA, the Atrion Rave Pro and, on the AGP, the Envya Prism TNT. New memory: the SIMM-16, the EDO-32
  and EDO-64, and the SDRAM-32 and SDRAM-64 the Slot 1 board takes. And the MF PowerBasic 200 and the Vaultis Trench
  HDD 200M.
- (Computers) Hardware: The Legacy era fills out the same way, a socket at a time: the Velocion Duro 1300 on Socket A;
  the Integra Celer 2.0 and the Pentix 4 2.4C, 3.2C and EE 3.4 on Socket 478; the Celer D 325J and the Pentix 4 520,
  540 and 560 on LGA 775; the Velocion Semper 3100+ and Sprint 64 3200+ and 3700+ on Socket 754; the Sprint 64 3500+,
  4000+ and FX-55 on Socket 939; and the Integra Servo 2800, 3060 and 3200 for the servers on Socket 604, with an ATX
  board for each new socket. The Pentix 4 chips and the Servos run two threads a core. Thirteen new graphics cards run
  from the Envya Prism TNT2 M64 and the Atrion Radiance 7000 through the Vertex 4 Ti 4200 and the Radiance 9600 XT to
  the Vertex 6800 Ultra on AGP, and on the first PCIe boards the Vertex 6200 and 6600 GT and the Radiance X300 and X800
  XT, the top of the era. And the SDRAM-256, DDR-256, DDR-1024 and DDR2-512, the MF PowerBasic 350 and the Vaultis Link
  IDE-HDD 40G.
- (Computers) Hardware: The Transition gets its hardware catalogue: on LGA 775 the Integra Pentix D 805, the Celer
  E1200 and the Centro 2 Duo, Quad Q6600 and Extreme QX9650; on AM2 the Velocion Sprint 64 X2 and the Ascent X4 9850; on
  AM3 the Sprint II and the Ascent X4 and X6, up to the X6 1100T; on LGA 1156 the Pentix G6950 and the first Centro c3,
  c5 and c7; on LGA 1366 the Centro c7 920, 960 and 980X and the Integra Servo 5520, 5570 and 5680; the Servo 5450 on
  LGA 771; and the Velocion Optera 2218, 8356, 8384 and 8435 on Socket F. A board for each: the ATX boards for LGA 775,
  AM2, AM3 and LGA 1156, the LGA 1366 workstation board, the two-way server boards and the Mainframe's MF MTX-T.
  Fourteen new graphics cards run from the Envya Vertex 7300 GS and the Atrion Radiance X1300 to the Vertex GTX 480 and
  the Radiance HD 5870. And DDR2 and DDR3 in three sizes each, a registered DDR2 for the servers, and the MF PowerBasic
  450B.
- (Computers) Hardware: The Standard fills out: on LGA 1155 the Integra Celer G530 and the Centro c3 2120, c5 2500K and
  c7 2600K and 3770K; on LGA 1150 the Pentix G3258, the Centro c3 4160 and the Servo 1231 v3 beside the Centro c5 and
  c7; on LGA 2011 the workstation Centro c7 3820, 3930K, 4930K and 4960X and the Servo 2609, 4650 and 4657L v2; the
  Velocion FX-4300, FX-6300, FX-8350 and FX-9590 on AM3+, the Fuse A4, A8 and A10 on FM2+, and the Optera 6212, 6272
  and 6380 on G34. A board for each new socket: the ATX boards for LGA 1155, AM3+ (on the PCIe 2.0 its chipset had) and
  FM2+, and the two-way and four-way G34 server boards. Thirteen new graphics cards run from the Envya Vertex GT 730 and
  the Velocion Radiance HD 7750 to the Vertex GTX 1080 and the Radiance R9 Fury X, on the new Maxwell and Pascal
  designs, with the Envya Tessera K40 compute card for the servers. And the Stratix Layer DDR3L-4096 and the
  DDR3-16384 RDIMM of the servers.
- (Computers) Hardware: The Advanced era gets its hardware, from 2017 to today. Sixty-two processors: the Velocion
  Sprint 200GE and the Awayken 3, 5, 7 and 9 on AM4 and AM5, up to the 9950X; the Integra Pentix G4560 and the Centro c3
  to c9 on LGA 1151, 1200 and 1700, where the hybrid Centro count their efficiency cores beside the performance ones,
  and the Centro Ultra on LGA 1851; the workstation Centro c9 on LGA 2066 and the Velocion Threadkiller on sTR4 and
  sTR5, up to ninety-six cores; and for the servers the Velocion Epic on SP3 and SP5 and the Integra Servo Bronzo,
  Plata, Oro and Platina on LGA 3647, 4189 and 4677, with the Servo W-3175X and w9-3495X of the workstations. A board
  for each socket: six ATX boards, four workstation boards, five two-way server boards and the Mainframe's four-way
  boards on SP3, SP5, LGA 3647 and 4189. DDR4 begins here and DDR5 follows it, in the Stratix Layer DDR4-8192 to
  DDR5-131072 RDIMM, and the LGA 1700 board takes either. The MF PowerGold 1000G, the PowerPlat 1200P and 1600P, and
  the ServerPSU 2000P and 3000P (Redundant) of the servers. And the Vaultis Keep HDD 12T, 16T, 20T and 24T, the helium
  drives, which are made only as hard disks.
- (Computers) Hardware: Seventeen Advanced graphics cards run from the Velocion Radiance RX 550 and the Envya Vertex GTX
  1650 to the Radiance RX 7900 XTX and the Vertex RTX 5090, on the Turing, Ampere, Ada Lovelace and Blackwell designs
  and on RDNA 1 to 4, with the Envya Tessera V100, A100 and H100 compute cards of the servers. The Forge Logic Crafting
  Card T4 is the crafting card of the Advanced boards, and the Fabric DPU the cluster card that reaches eight nodes at
  once.
- (Computers) Hardware: Processors and graphics cards name the architecture they are built on in their tooltip, with
  the chip's codename: "Architecture: Centro Conroe", "Architecture: Kepler GK110".
- (Computers) Hardware: The graphics card is a resource. Each lit monitor holds video memory by its size in blocks and
  its era's colours (64 KB for a Vintage monochrome tube up to 256 MB for an Advanced panel, a big screen its every
  monitor), and a graphics program's window holds a quarter of a block, a whole one while it fills the screen. A
  monitor the card has no room for stays dark and says why on its own menu, and lights the moment there is room; a
  graphics program that does not fit does not open, and the notification area says how much it needs and how much is
  free.
- (Computers) Hardware: The Integra Centro c5 4590, c5 4690K and c7 4790K carry graphics on the die, as their real
  counterparts do: they give the board one video output, so a machine with one drives a screen without a graphics card,
  and the Device Manager lists it. Those graphics borrow the system's memory, up to a quarter of it and 1792 MB at most,
  and hold of the RAM what the screens they drive need.
- (Computers) Hardware: A Mainframe's graphics card queues run at most at the processor's speed and no faster than the
  card can (its cores, their clock and its design, cut by a slot older than the card). The queues tile turns amber while
  a card holds its queue back, and its tooltip lists each queue's speed and names the card behind each.
- (Computers) Hardware: A server's graphics cards add a twentieth of an item a tick for each of their threads to what
  it sends out; its assembly shows the bonus beside its processor's capacity.
- (Computers) Hardware: Four sound cards, one for each bus of the Vintage and Legacy boards: the Artisan Tone Blaster
  (ISA) and Tone Blaster 128 (PCI), which make their notes by FM and play recordings at 8 bits in mono at 22 kHz, and
  the Tone Blaster Live (AGP) and Tone Blaster Hi-Fi (PCIe), wavetable cards playing recordings at 16 bits in stereo at
  44.1 kHz; and the Tone Blaster Audigy, the sound card of the Legacy's PCIe boards. A sound card sits only on a board
  of its own era, in a slot of that board's bus, and a machine takes one. From the Transition on, a board has its sound
  built in, and its tooltip says so. A computer's tooltip says whether its case only beeps or its board plays
  everything, and a monitor's that its computer's sound comes out of it.
- (Computers) Hardware: The Optical Network Card, from the Standard. The backbone's fibre enters a Mainframe or a
  Cluster Management Computer only while the machine holds one; without one the machine takes the copper of its
  backbone alone. It fits a PCIe slot and is drawn inside the computers that seat it. A server never seats it.
- (Computers) Computers: The small computers drawn as the cases of their age: the Personal Computer, the Crafting
  Computer and the Cluster Management Computer fill their block as a real tower of each age, the left side a panel of
  its own: a beige AT minitower with its MHz display for the Vintage, a beige ATX tower for the Legacy, a silver and
  gloss black mid tower for the Transition. From the Standard age on each comes in three cases, a block each, that
  differ in look alone: Neutral, closed and quiet; High Performance, mesh and more fans; Aesthetic, glass and light,
  with red LEDs in the Standard and addressable light in the Advanced. A plate at the foot of the front tells the
  machines apart. The Transition and Advanced machines are new blocks, and so are the High Performance and Aesthetic
  cases of the Standard.
- (Computers) Computers: A small computer's left side comes off, to see what is inside: sneak and use the case with an
  empty hand, or throw the SIDE PANEL switch beside the inventory on its assembly screen. It stays as it was left,
  through a save and for every player.
- (Computers) Computers: Inside a small computer, each part the player installed is drawn by a model of its own, the
  one of that item: the board on the tray, the processor in its socket, the memory in its slots, the cards hanging in
  theirs, the supply and the disks where the case of that age keeps them, an M.2 drive on the board; and on the
  processor the cooler the case brings, unless the processor is a Slot 1 cartridge with its own. A part of another age
  sits where this case puts it, and a supply made for the other end of the case is turned over, without the leads made
  for its own board. In an Advanced case the hard disks sit in the cage under the shroud, hidden like the supply, and
  the helium disks above 8 TB are drawn each with its own sealed lid and label. The parts are drawn when the side is
  off, or through a case's glass or mesh. A board for two processors has no place in a small case yet and is not drawn.
- (Computers) Computers: A small computer's power lamp is lit while it runs, and its disk lamp blinks while it works its
  disk, whatever the disk. Its fans turn while it runs: the case's own, and those of the parts in it, the cooler's and
  the cards'.
- (Computers) Computers: The front of a computer's case. The Personal, Crafting and Cluster Management Computers have
  the 5.25-inch bays of their era's case, one on the Vintage and Standard cases, two on the Legacy and Transition ones,
  none on the Advanced. Four parts go in them, each in its era's tab: the Hot-Swap Drive Bay (Vintage), the Internal CD
  Drive (Legacy), the Internal DVD Drive (Transition) and the Internal Blu-ray Drive (Advanced), which is there to read
  the newest discs in an older computer. Any of the four goes in any bay, one to a bay. The cases are drawn with their
  fronts as they now are: the power button, the lamps and, from the Transition on, two USB ports on the left of the
  face, an empty bay showing its cover and a part in a bay drawn in it, a drive with its tray and the hot-swap bay with
  its caddy; a disc lies on a drive's tray and a USB flash drive plugs into a port, each drawn as the item it is. What
  the bays and ports hold falls out when the case is broken, or when the part holding it is taken out of its bay.
- (Computers) Computers: Working the front of a case. A click on the power button switches the machine on and off,
  whatever the hand holds, as the button on its assembly screen does; a click anywhere else on the case still opens
  that screen. A drive in a bay works as an external drive does: its eject button opens and closes its tray, which stays
  in while the machine reads the disc, a disc goes on the open tray with a click and comes off it with a sneak-click,
  and its lamp blinks while it reads. A USB flash drive plugs into a front port with a click and comes out with a
  sneak-click, lying as the port does. A disk slides into the hot-swap bay with a click and comes out at a click of the
  empty hand, with the machine on or off, its caddy riding in and out and its lamp blinking while the machine runs its
  system from that disk and works it; an empty hot-swap bay shows its opening and the board at its back, and an empty
  hand on it opens nothing. A stick standing in a port and a drive's open tray are aimed at as the parts they are, out
  of the case, outlined on their own, and worked there: a sneak-click on the stick pulls it out, and the tray takes a
  disc and gives it back as from the front, and slides back in when pushed with an empty hand. The power button, each
  drive's eject button, each port and the hot-swap bay are outlined on their own while looked at, exactly where a click
  works them.
- (Computers) Computers: What the front holds, to the system. The hot-swap disk is one of the machine's own, with a
  letter, taking a system and booting it, keeping the machine's items (with its own share offered to the network on a
  Personal Computer's Storage heading), and counting in its capacity and its power draw as the disks inside do, though
  it takes none of the board's disk slots; taken out, it takes its items with it, and with the machine running from
  it, its system too, as on the assembly screen. Each drive and port on the front is a drive like the external ones: a
  letter, This PC (whose Eject opens a drive's tray), the explorers, installing systems and programs, the firmware and
  its boot menu, the Crafting Manager, the Pattern Studio and live media.
- (Computers) Mainframes: The Transition and Advanced Mainframes. The Transition's cabinet is gloss black after the IBM
  z9 and z10, with brushed-silver fins down its sides, a honeycomb service door, a silver badge and a blue slit of light
  down the middle of its front; the Advanced's is near black after the z14 and z15, its door a perforated sheet folded
  into facets, with a line of white light up each side. Each takes the MTX board of its own age, and their lights come
  on while the machine runs on a build that makes a computer.
- (Computers) Mainframes: A Mainframe has a power button on the sill in the middle of its cabinet, in every era: a click
  switches the machine on and off, whatever the hand holds, as the button on its assembly screen does, and it is
  outlined on its own while looked at; a click anywhere else on the cabinet still opens that screen.
- (Computers) Racks and servers: The Transition and Advanced Server Racks and servers. The Transition rack is gloss
  black with brushed-silver posts, honeycomb rear doors and a header with a silver badge and a blue lamp; the Advanced
  rack is the era's white with hidden screws, finely perforated rear doors and a white line on the header. The
  Transition 1U server, after the HP DL360 G5 and the Dell PowerEdge 1950, has a brushed-silver face with four 2.5"
  drives, a slim DVD and a blue power light; the Advanced one, after the HPE DL360 Gen10 and the Dell R650, is black
  with slim NVMe carriers, a honeycomb vent and a white light. Both take two processors and five drives, the
  Transition's two cards and the Advanced's any number; each seats in a rack of its age or later, and every rack draws
  every server it can seat in that server's own shape. Their items and their empty cases show the server's face. A
  server's assembly screen is drawn in its chassis's era, and a server with no board yet in its chassis's era too.
- (Computers) Racks and servers: The Advanced Supercomputer Rack and its node. The rack keeps the Standard one's shape
  (the hollow shell, the cooling unit on its roof with the coolant going in and out on different blocks, the livery
  panel over the service opening, a lit band per row) in the era's near black with white light, its livery a white
  trace stepping across it like a circuit. The Advanced Supercomputer Node, 2U, has four accelerator sleds with white
  latches, two NVMe carriers, a honeycomb vent and a white line on its ear; it takes the Advanced parts, up to four
  cards (the co-processor and three GPUs), and seats only in its era's rack, which seats the Standard node too and draws
  each node as it is.
- (Computers) Racks and servers: A Storage Server and a Compute Server, with their cases, for the Vintage, the Legacy,
  the Transition and the Advanced, 2U each and drawn in their era's faceplate. A storage server has 5 drive bays in the
  Vintage, 7 in the Legacy, 8 in the Transition and 10 in the Advanced; a compute server 1, 2, 2 and 3, with up to 2, 2,
  4 and 4 processors and 1, 2, 2 and any number of cards. Each takes its era's server boards and seats in a rack of its
  era or a later one. The Legacy storage server has two card slots, one for its RAID controller and one for a graphics
  card, since its board has no video of its own and a monitor needs one to show the firmware.
- (Computers) Racks and servers: The Vintage server board has ISA slots beside its PCI ones, and the Legacy server
  boards PCI slots beside their PCI Express, as the boards of their time did, so the era's storage cards, the ISA and
  the PCI Vaultis, seat in the servers they are for. A card in the older slot runs at that slot's own speed.
- (Computers) Racks and servers: A KVM Switch, a Rack UPS and a Cooling Unit for every era, from the Vintage to the
  Advanced, each in its era's faceplate; the ones there were are the Standard's. A rack seats the units of its own era
  and of the earlier ones, as it does servers, and says so when a unit is too new for it. A Cooling Unit adds its era's
  watts to the rack's heat budget: 300 W for the Vintage one, 600 W, 1,000 W and 1,500 W up to the Standard, 2,500 W
  for the Advanced.
- (Computers) Racks and servers: The Fibre Optic Interface and the Advanced Fibre Optic Interface, 1U rack units: a
  server rack takes the fibre backbone only through one mounted in any of its rows. The Standard one takes the Fibre
  Optic Cable; the Advanced one, which only an Advanced rack seats, takes the OM5 Fibre Cable as well.
- (Computers) Racks and servers: The Advanced HBW Interface, the Advanced supercomputer's uplink, in the era's white
  with finned OSFP cages: it takes the OSFP fabric and every earlier high compute cable, and the backbone up to OM5. A
  Cluster Management Computer reaches the Advanced supercomputer through the Fabric DPU. An HBW Interface finds its
  nodes along any high compute cable of its era or an earlier one, in each Supercomputer Rack that takes that cable:
  the Transition's InfiniBand reaches the Standard's racks, and the OSFP only the Advanced's.
- (Computers) Network: The data cables are lines by job, with a cable of each line for each era. Access, which joins
  the small computers to a router: Thin Coaxial, Ethernet, Cat 5e, Gigabit Ethernet and Cat 6a. Backbone, which joins
  the routers, the Mainframe and the racks: Thick Coaxial, HBW, 10GBASE-CX4, Fibre Optic and OM5 Fibre. Long distance,
  which will join two networks between Gateway computers: the Telephone Line, the Leased Line, the T3 Line, VLDC and
  Dark Fibre. High compute, for a supercomputer's nodes: InfiniBand from the Transition, High Compute and OSFP. And one
  Crafting Cable for every era. Each wears its own jacket and ends in its era's plug where it meets a device.
- (Computers) Network: A machine takes its own era's cable of a line and every earlier era's, never a later one's, on
  every block of it, and two eras of one line touching do not join: they meet at a router of the newer era.
- (Computers) Network: Speed and range count on every line. A run longer than its cable reaches carries nothing, so
  whatever is only beyond it is off the network; an Operation reading a server moves no faster than the slowest cable
  between the Mainframe and that server. Every cable's tooltip says how many cables one run of it reaches and what
  starts a new run (a Repeater, a Router or a Hub), and the manual's tables of reaches and speeds are read from the
  cables. The speeds and ranges are first estimates.
- (Computers) Network: The backbone's fibre runs only straight: a cable laid against its side is left out, and the
  optical router is what turns it. A long distance line runs between two ends and takes no third, is thicker than the
  other cables, and shares no block. The long distance cables have no use until the Gateway computers come.
- (Computers) Network: A router for every era: the Vintage Router, the Personal Router of the Legacy, and the
  Transition, Standard and Advanced Routers. Each joins its era's access line to its backbone, and takes the cables of
  both lines of its era and of every earlier one on any of its six faces, all of them one network. The small computers
  reach the network through a router: the access line goes from them to a router, and the Mainframe and the Server
  Racks take only the backbone.
- (Computers) Network: The Optical Router, from the Standard, and the Advanced Optical Router take only the backbone's
  fibre, from the Standard's to their own era's, and are where it turns and branches.
- (Computers) Network: A repeater for every era, from the Vintage Repeater to the Advanced Repeater. The access,
  backbone, high compute and crafting cables run through it each on its own, never joining one another there, and each
  run starts its reach over at it, so a cable can go twice as far with a repeater halfway. The routers, optical routers
  and repeaters wear the same face on all six sides, the connector of their era in the middle and two blinking lamps
  above it.
- (Computers) Network: The Network Manager shows how each node is linked. The Devices tab has a LINK column with the
  cable and its speed, and a square for a fibre port; a machine joined to the network that lost its link is listed as
  "no link", and the line under the list says why: its fibre bends where no Optical Router turns it, or a run is longer
  than its cable reaches. The Map draws each link by its line (the fibre thick and aqua, an earlier era's cable in
  dashes, a link down red), with its speed, the Optical Routers the fibre goes through and a legend; a node's card has a
  NETWORK section. The Hardware tab, which now scrolls, adds the optical links up and down, the backbone and the slowest
  link in use.
- (Computers) Operations engines: The Network Operations Engines. What makes a storage network work is software its
  Mainframe runs: every new Mainframe, of every age and system, ships with the Midsoft IQL Server installed and running,
  in the version of its age (4.2 on a Vintage Mainframe, 2000 on a Legacy, 2008 on a Transition, 2012 on a Standard,
  2022 on an Advanced). It is the IQL Engine of before under its own name, and it keeps the views and procedures.
  Everything that asks the network for work (the Network Interactor, the terminals, the buses, the gateways, the craft
  dialogs, the programs and the network's language) comes in by one door, the Network Operations Service, which hands
  it to the engine running; computers bind to the network, not to an engine. With no engine running the network still
  pulls, pushes, moves, exports and fills, but crafting, a craft's plan and the language are refused with "Network
  Operations Service unavailable", and the Network Interactor offers nothing to craft. A running engine weighs its
  memory on the Mainframe.
- (Computers) Operations engines: An engine is the Mainframe's choice: a Subframe that runs another engine lends its
  Mainframe neither capacity nor queues until the two run the same one. Changing or stopping the engine hands only new
  work to the new one (or to none): every Operation already made carries on, a craft on the plan it was made with,
  after a reload too.
- (Computers) Operations engines: A software house's tools are written for its own engine. The IQL Server Management
  Studio opens only on a network running the Midsoft IQL Server, from its 2000 version on; anywhere else it stays
  installed and says "No compatible Midsoft IQL Server was found on this network." An engine is a package of its own
  kind, and `install iqlengine` installs it the way every program is installed, from its disc at the Mainframe, written
  down with the system so the memory it holds is counted.
- (Computers) Operations engines: The jobs are the Automation Engine's alone: "at this time, or when this happens, do
  that", the same whichever engine plans the network's work. A job is made only where the Automation Engine is
  installed (IQL's CREATE JOB says so where it is not) and fires only while it runs. The Automation Engine and its
  Manager now reach back to Frames XP, so a Legacy network keeps its jobs. The Manager's "Keep Stock" is now "Restock
  below", which says it does not count what is already on its way and points whoever wants a level held, counting
  it, to Prophet YourIQL's KEEP.
- (Computers) Operations engines: The Network Manager has a Services tab, and opens wider so its seven tabs fit. At the
  top, the engine that plans the network's work: its state, the Mainframe it runs on, its dialect, the memory it takes,
  how long it has been up, how many requests it planned today, its indexes and what it can do, with Stop and Replace
  (Configure is there and does nothing yet). Under it, the engines installed, the Subframes (one that runs another
  engine is marked "takes no work", and a note says what would put it back to work) and the other services, the
  Automation Engine and the Mirror. With no engine running the card says what still works and offers Start.
- (Computers) Operations engines: Replacing the engine asks first, saying where new requests will go, that the
  Operations in flight finish on their plans, that scripts in the old maker's own statements may stop working, and
  which Subframes will take no work. Then it takes time, more for a network holding more kinds of item (from two
  seconds up to two minutes), and the window shows the steps with a bar; the network has no engine until the new one is
  up. A replacement under way is written down with the Mainframe.
- (Computers) Operations engines: The IQL Server Management Studio (the command and id `isms`), which takes over from
  the Network Management Studio, installs from the Legacy age on, and leaves no `nms` behind. It is complete: a menu bar
  where every item does something, with its keys (File with Recent Files, Save All and Print; Edit with undo, find and
  replace, go to line, comments, capitals and IntelliSense; View; Query; Tools; Window; Help); a toolbar; query tabs,
  each with its text, its results, its messages and its plan; a script run a statement at a time, each in its own
  packet, its tables stacked one under another with each table's real columns, to a grid, to text or to a file;
  Execute Selection, Cancel Executing Query (which stops the Operations the run set going) and Parse, which underlines
  the word the language cannot read; the status bar with the engine, its version, the computer, the rows read and the
  time taken. The Object Explorer lists the tables with their columns and row counts, the views, the stored
  procedures, the servers, the Management folder (the index and its health, the items held by hand, the Operations log)
  and the Automation Agent's jobs, each node with a menu of its own: a table's first rows or its script, a view or a
  procedure scripted as CREATE or DROP, a procedure run or modified, a server's items, the index analysed, rebuilt or
  vacuumed, an item let go, a job started, paused or deleted, the engine started, stopped or restarted. Display
  Estimated Plan shows how a CRAFT would be made, without making it; the Activity Monitor shows the network's
  Operations with the Network Manager's own list; the Object Explorer Details (F7) list what the node picked holds, with
  its columns and a search; the Template Explorer, template parameters filled in a dialog, the Properties Window, the
  IQL Reference and the Keyboard Shortcuts. Scripts, results and the studio's settings are files on the disk of the
  computer that opens it, through the system's file window, as the prompt's `iql` and Σ's `Iql.RunFile` keep theirs. A
  statement that cannot be undone (items dropped, everything of an item sent out, a saved object dropped) is asked
  about first, with "Don't ask again". The studio wears the look of its computer's age: the IQL Query Analyzer of the
  Midsoft IQL Server 2000, grey, with its Object Browser and Maintenance, on a Legacy computer, and the studios of
  2008, 2012 and 2022 after; on a Vintage network the engine is used from the prompt.
- (Computers) Operations engines: The IQL Server Profiler, from the studio's Tools, in a window of its own: it records
  what the network's work does as it happens, the statements that come through the network's door, the Operations
  taken on and settled, the locks taken, the plans chosen for a craft and what the buses moved, each with who asked, on
  which computer, how many items and how long it took, a row's detail below the grid. A trace starts, pauses and stops,
  picks the groups of events it records, finds a row, replays a statement in the studio, and is saved to and opened
  from the computer's disk.
- (Computers) Operations engines: NextgreIQL, a second Network Operations Engine, from the Nextgre house: the explicit
  one, which shows how it built a plan and lets the player take part in it. A Mainframe from the Legacy on installs it
  as a package, in the version of its age (7.0 on a Legacy, 8.3 on a Transition, 9.0 on a Standard, 16 on an Advanced),
  and it plans every craft it is asked for by weighing the plans it could make: bench recipes first, machine recipes
  first, each other bench recipe of the result, the raw materials from the fastest servers, one stage at a time. Each
  costs the time it is reckoned to take, built up the plan's tree: a bench step at the speed of the network's crafting
  computers, a machine step at the time that recipe was measured to take on this network, a pull at what each server
  and the cable to it carry; the cheapest not set aside is the one that runs, and how it ran is measured for the next
  plans. Its dialect is the network's language with more: `EXPLAIN` before a CRAFT or a SELECT shows the plan and runs
  nothing, `EXPLAIN ANALYZE` runs it and fills each step in with what it took, and hints after a CRAFT change the plan
  (`PREFER SOURCE`, `AVOID SOURCE`, `MAX PARALLEL`, `PREFER MACHINE`, `PREFER BENCH`). `ANALYZE` gathers its
  statistics. Its rules can be switched off for each Mainframe, and other mods add rules, hints, statistics and notes
  to its planner. The Midsoft IQL Server does not speak these words.
- (Computers) Operations engines: The Nextgre Planner Studio, NextgreIQL's own tool, on any windowed system from the
  Legacy on: on its Explain tab a statement's plan as a tree of boxes, each with the time reckoned against the time it
  took, the hints marked on the boxes they changed, and beside it the plans weighed with their costs and why any was set
  aside; Explain (F7) and Explain Analyze (Shift+F7). Its Statistics tab lists what the planner reckons with, its Rules
  tab its rules, each switched on or off, and the hints it takes, and its History tab the plans made lately, any of
  which opens on the Explain tab. It opens only where NextgreIQL runs.
- (Computers) Operations engines: Prophet YourIQL, a third Network Operations Engine, from the Prophet house: the
  state-oriented one. A Mainframe from the Legacy on installs it as a package, in the version of its age (3.23 on a
  Legacy, 5.0 on a Transition, 5.6 on a Standard, 8.0 on an Advanced). Told what state the network is to keep, it works
  out the crafts to get there and stay there: `KEEP item >= n` or `KEEP item BETWEEN a AND b` holds a level, counting
  what is already on its way so nothing is asked for twice, and says when nothing on the network can make it or when
  the level is over its band; `WATCH item < n DO statement` runs a statement once when a level crosses a line, and
  again only after it crossed back (`CRAFT item TO n` makes up to a level); `FORGET` lets either go and `SHOW STATES`
  lists them. It looks at the network once a second by default and does only what changed: on a network standing still
  a look costs one comparison. How often it looks, how much it asks for in one craft and whether it reacts at all are
  settings of the Mainframe. The Midsoft IQL Server does not speak these words.
- (Computers) Operations engines: The Prophet Reactive Console, Prophet YourIQL's own tool, on any windowed system from
  the Legacy on: its States tab lists each state with where it stands, its level against its band, the work on its way
  and the last thing done for it, and under them the selected state's graph (the band, the level as it went, and where
  the work on its way takes it) with the Operations set going for it; its Subscriptions tab the watches, its Reactions
  tab what the engine did, its Settings tab how it looks and reacts. A statement is written at the YourIQL prompt,
  checked and applied. It opens only where Prophet YourIQL runs.
- (Computers) Operations engines: A shared list of the network's Operations in flight, which the Network Manager's
  Processes tab and the studio's Activity Monitor both show.
- (Computers) IQL: The network's language reads what it was always written with. A definition (a view, a procedure, a
  job) typed at the prompt's `iql` or written in a file of statements goes to the engine, where it used to answer "iql
  failed: null" or stop the file. An `IF` decides whether an action runs at all, reading the network's holding of its
  item. An `ORDER BY` sorts every row before the `LIMIT` takes the first ones. A `WHERE` on a `SELECT`, a `MOVE`, a
  `DELETE` or a `DROP` picks the variants it touches (a damaged tool, a name), where it used to be read and ignored
  without a word.
- (Computers) Storage and buses: Buses for every era. The Import and the Export Bus come in a Vintage, a Legacy, a
  Transition, a Standard and an Advanced make. The Vintage bus moves an item a tick, one kind at a time, with no
  filter; the Legacy one eight, with a filter of up to five items (only these, or all but these), what the faced chest
  keeps and how many a move takes; the Transition one sixteen, with a keep and a max for each item it lists; the
  Standard one thirty-two, with a priority over the network's other buses and conditions to wait for (the network's
  stock of an item, the hours of the day, another bus having finished); the Advanced one sixty-four, filtering by tag
  as well and matching loosely, an item whatever its damage and components. The speeds are first estimates, and no bus
  moves faster than its cable carries.
- (Computers) Storage and buses: A storage bus mounts on an access or a backbone cable of its era or an earlier one, on
  any cable block that holds a data or a crafting cable, on a face no wire crosses; clicked where it cannot go, it says
  why on the action bar.
- (Computers) Storage and buses: A bus can be switched off, and keeps a log of what it did lately: each move it made,
  and each time it held back and why (what the chest keeps, no room, a condition). What a program set on a bus is
  marked with the program's name.
- (Computers) Storage and buses: Each bus's window has three tabs, in its era's skin: Configure, the rows its era can be
  set to, which scroll above the inventory when they do not all fit; Activity, what it moved and why it held back, at
  the hour it happened; and Software, its address and the IQL statements and the calls that set it as it is set, with
  which programs set what. A setting a program set carries the program's mark until a hand changes it in the window.
  Conditions and tags are written in the window: the network's stock of an item or a tag, the hours of the day (one
  window of hours a bus), another bus having finished. A lamp at the right end of the title bar says whether the bus
  reaches the network, green or red, the word in its tooltip.
- (Computers) Storage and buses: The External Storage Bus, of every era: it moves nothing itself, the network uses the
  inventory it faces as storage of its own, ten times slower than its servers. The Vintage one shows the network all of
  it; from the Legacy, a filter decides what the network sees, and the network may read and write it, only read it, or
  only write to it; from the Standard, a priority decides which storage the network fills first; the Advanced one
  filters by tag and matches loosely as well. Its window tells how many slots the inventory has and how many are in
  use.
- (Computers) Storage and buses: What comes into the network fills the storage of the highest priority first, then the
  fastest.
- (Computers) Storage and buses: IQL sets a bus by its name: `SET BUS 'Ore in' KEEP 16 MAX 64`, and the filter (only
  these, all but these, by tag), the loose match, each item's own quantities, the mode, the priority, on or off, an
  External Storage Bus's access, and what it waits for (the network's stock of an item or a tag, another bus); a job
  that switches a bus on between two hours of the day keeps it to those hours. A name in single quotes is read as one,
  as in SQL.
- (Computers) Storage and buses: Programs set buses too, in both languages: `bus("Ore in").Keep(16).Max(64);`, with a
  call for every setting. A setting set from software carries the name of the program or the job that set it, which
  the bus's window marks, and what the bus's era cannot be set to is refused as it is in the window.
- (Computers) Storage and buses: Each bus has a model of its own: a metal clamp round the cable, a ring in its kind's
  colour (green the Import, orange the Export, violet the External Storage, blue the Crafting Input Router, yellow the
  Crafting Receiving Bus, red the Crafting Interface, whose plate shows a slot for each pattern it holds), a funnel
  opening to the inventory for the buses that take from it, a nozzle for the ones that put into it and a wide thin
  panel for the External Storage Bus, chevrons along its sides pointing where the items go, and a plate against the
  inventory with its mark over its era's grille. Its housing is in its era's colours, the router's and the Receiving
  Bus's in the crafting line's amber, and so is its item. The buses are drawn with the cable in the world's mesh, their
  faces shaded as they are turned.
- (Computers) Storage and buses: A bus's four lamps blink while it moves items, the External Storage Bus's while the
  network reads or writes through it and the crafting parts' while a machine is fed or emptied through them, and go
  dark a few seconds after.
- (Computers) Autocrafting: The Crafting Interface, of every era, is where a network's machine recipes live: a part on
  the crafting cable that holds 3 patterns in the Vintage, 6 in the Legacy, 8 in the Transition, 9 in the Standard and
  12 in the Advanced, and feeds one machine. Set against the machine it feeds it directly, through that face; standing
  apart, it feeds it through a crafting cable of its own, dyed apart from the main one, with a Crafting Input Router
  against each face of the machine that takes an input. Its window shows the machine it feeds and the way there, its
  patterns and each pattern's inputs with the router each one goes through and why, its mode, its state and how many
  jobs it runs at once, what it is doing now and its last jobs, and its address and the IQL and calls that set it.
- (Computers) Autocrafting: The Crafting Input Router, a part that does nothing of its own: an interface puts an input
  into the machine through it. Which router an input goes through is the one chosen for it in the interface's window,
  else the first whose filter takes it, else the first that takes anything; a pattern with an input no router takes
  cannot run, and the window says so. A router filters by every item its filter lists, up to five.
- (Computers) Autocrafting: An interface is exclusive when it feeds through routers (one recipe at a time on its
  machine, jobs of the same pattern running together) and shared when it sits against the machine (several jobs at
  once, of different recipes too, as far as the machine takes them); either can be changed in its window, and so can
  the most jobs it runs at once (Auto, or 1 to 64) and whether it is paused. Two interfaces on one machine take turns.
- (Computers) Autocrafting: The Crafting Receiving Bus ties itself to the interfaces of the machine it faces, or is tied
  by hand to an interface anywhere on the crafting cable. What the machine gives back is credited to the jobs that fed
  it, never more than the lots each was fed, in the order they were fed; an output that comes after its job settled is
  still owed to it, and an item no pattern declared goes to the network as unexpected; a fluid or chemical no pattern
  declared is the machine's own (an infuser's infusion, a generator's fuel) and stays. Its window shows what it
  credited, what came late and what was unexpected. It filters by every item its filter lists, up to five, and its
  window, like a Crafting Input Router's, wears the skin of the Mainframe that commands it.
- (Computers) Autocrafting: A Crafting Card drives Crafting Interfaces and keeps a ROM of bench recipes of its own, both
  by its era: 2 of each in the Vintage, 4 in the Legacy, 5 in the Transition, 6 in the Standard and 8 in the Advanced.
  The Crafting Computers on a crafting cable drive its interfaces in the order the cable reaches them, as many as their
  cards drive between them; the rest wait for another card. The numbers are first estimates.
- (Computers) Autocrafting: A craft whose recipe an interface holds waits for as long as that interface cannot feed it
  (its machine gone, the interface paused, no router taking an input); one whose recipe no interface holds fails after
  its timeout, and says so.
- (Computers) Autocrafting: IQL sets the crafting network by name: `SET INTERFACE 'Mixer' EXCLUSIVE OFF`, `MAX JOBS 3`,
  `PAUSE INTERFACE` and `RESUME INTERFACE`, `SET INTERFACE 'Mixer' ROUTE 'Coarse dirt' INPUT gravel TO ROUTER 'North'`
  (or `AUTO`), a router's filter with `SET ROUTER 'North' FILTER ONLY gravel`, and `RENAME INTERFACE` and `RENAME
  ROUTER`. Programs do the same with `craftInterface("Mixer").Exclusive(true).MaxJobs(2).Pause()` and
  `craftRouter("North").Only("gravel")`, new in Σ 2 as `CraftInterface` and `CraftRouter`.
- (Computers) Personal-use cards: The Crafting Table Card, the Furnace Card, the Enchanting Card and the Anvil Card, one
  of each for every era from the Legacy on, seated in any PCI, AGP or PCI Express slot of a Personal Computer. In the
  Device Manager each wears its own picture.
- (Computers) Personal-use cards: The Workshop, by Autodeck, the program that works them on a Personal Computer, on
  every windowed system from the Legacy on but Frames 95: a tab for each card (dim, with "no card", when the card is
  not in), the station on top with what is on it beside, the player's inventory below and a status bar with the cards,
  the furnace and the player's level. Items go in and out with the cursor as in any container, and a shift-click on the
  inventory sends a stack to the station. Crafting: the grid with the game's recipes, Craft, Craft all (which refills
  the grid from the inventory) and Clear grid; the grid goes back to the player when the window closes. Furnace: no
  fuel, two times a furnace's pace on a Legacy computer, three on a Transition, four on a Standard and six on an
  Advanced; it goes on smelting with the window closed while the computer is on, keeps what goes in and comes out in
  the computer, pays the experience when its output is taken, shows in the panel's tray while it works and says in a
  balloon when it is done. Enchanting: the three offers of a table with every bookshelf around it, for 1, 1 and 2
  levels and no lapis. Anvil: the anvil's repairs, combinations and names for two thirds of its levels, and it never
  wears. What the cards hold falls out of a broken computer. The numbers are first estimates.
- (Computers) Personal-use cards: UPDATE, the network's door to the personal-use cards, with the same rules, price,
  pace and queue as the Workshop: an item the network holds goes to a card of the Personal Computer that asks, the card
  works on it, and it goes back to the network, to the server it came from when that server has room. SMELT through the
  Furnace Card (free, a stack at a time, in the same queue as the Workshop's own smelting, which goes first); ENCHANT
  through the Enchanting Card, OFFER 1, 2 or 3 picking one of the table's three offers and no OFFER listing them;
  REPAIR, COMBINE and NAME through the Anvil Card. Whoever asks pays the card's levels and has to be there; only the
  asking computer's own cards do it, so a job of the network's language, which runs as the Mainframe, cannot. In the
  network's language, `UPDATE [qty] <item> [FROM <server>] SET <action> [args] [WITH <item>] [WHERE ...] [PRIORITY
  ...]`, the WHERE picking which of the item's variants (the most worn first for a repair); in Σ 2,
  `Operations.Update(item, action, qty)`. It is a new kind of Operation, with the item before and after and its three
  steps (SUB_SELECT, SUB_UPDATE, SUB_INSERT) in the log; it needs an engine, as a craft does. While the card works,
  the item waits in a drawer no query sees, kept with the computer and dropped with it; what the network has no room
  for afterwards waits at the card's own slot in the Workshop. If the computer stops or loses its card, the UPDATE is
  discarded and what the card held is left there too. No craft ever uses a card.
- (Computers) Personal-use cards: "Update..." in the Network Interactor, in an item's dialog and in the details panel:
  a window with a tab for each action (Enchant, Smelt, Repair), dim where the computer has no card or the item does not
  take it, the item with how many the network holds and where, the card and the computer, the three offers with their
  clue and price, the amount to smelt with the wait behind the Workshop's own smelting, and the repair of the most worn
  of the item with its material from the network, its name and its price beside an anvil's.
- (Computers) Peripherals: Peripherals take ports by kind, as on a real computer, in place of one count of ports on the
  board: a monitor takes a video output of a graphics card (one on a Vintage card, two on a Legacy or Transition one,
  four on a Standard or Advanced one), a pair of speakers the audio output of the sound card, or of the board from the
  Transition on, and every other peripheral a device port of the board (two on the Vintage, then four, six, eight and
  ten). A server board has a video output of its own for its console. A machine without a graphics card has nowhere to
  plug a monitor. A peripheral with no free port of its kind waits unlinked and links when one frees; taking out a card
  unlinks the monitors it fed. The numbers are first estimates.
- (Computers) Peripherals: A peripheral cable for each era, laid in the shared cable block beside the data cables: the
  Vintage beige cable (DE-9 at a screen, DB-25 at a device), the Legacy black one (VGA and USB), the Transition white
  one (DVI and the white USB), the Standard braided one (HDMI and USB 3) and the Advanced space grey one (DisplayPort
  and USB-C). Each ends in the plug of the port it enters. A run reaches 8 cables on the Vintage, then 12, 14, 16 and
  20. A port takes its era's cable and every earlier one, so a newer computer takes an older device's cable and an
  older device never takes a newer one; cables of two eras never join. A peripheral takes the cable on its back, where
  its model has the port, and a computer on any face; a peripheral against its computer still needs no cable. The one
  Peripheral Cable block is gone: the Standard's cable keeps its id.
- (Computers) Peripherals: The Network Gateway's back has the Vintage serial port, the DE-9, in its middle, and it takes
  the Vintage serial cable, which every computer takes, so a computer of any era links it.
- (Computers) Peripherals: A hub for each era, a plain block with six equal faces in the network tab: cabled to a
  computer, it takes one of its device ports and offers its own, two on the Vintage switch box, four on the Legacy and
  Transition USB hubs and seven on the Standard and Advanced ones. Every device cabled to a hub takes one of the hub's
  ports; when they are all in use the next device waits, or links another way that has a port free. Screens and
  speakers do not pass through a hub. The cable after a hub reaches as far again as the cable before it, and a hub may
  hang from another hub. A hub taken away drops the devices behind it.
- (Computers) Peripherals: Disabled devices: a computer can disable a device on its ports, which stays linked and keeps
  its port while the computer neither reads nor writes it, and everything hanging from a disabled hub with it. A
  disabled monitor goes dark and opens none of its computer's screens, a disabled speaker plays nothing, a disabled
  drive's disc is not seen, a disabled Redstone Interface neither reads nor emits and no program finds it, and a
  disabled Gateway answers nothing. The computer remembers what it disabled.
- (Computers) Peripherals: A Redstone Interface for each era, in the devices tab: a small sensor on the peripheral
  cable, drawn as the sensor of its day, that lets its computer read a redstone signal or emit one. It is placed as an
  observer is, its lens toward what the player looks at, any of six ways, and only its lens reads or emits; emitting,
  it powers the block in front as a lever does. Its lens shows the strength it reads or emits, its green lamp that it
  reads and its amber one that it emits. It takes a device port, or a hub's, and with no computer to power it reads and
  emits nothing.
- (Computers) Peripherals: The Redstone Interface's window, in the skin of its era: its name, unique among its
  computer's interfaces, the computer it hangs from and the signal at its lens, the IN and OUT buttons, the strength it
  emits chosen by clicking one of sixteen cells, and the same setting written in IQL and in Sigma. Programs set the
  Redstone Interfaces of the computer they run on: `redstone("Gate").Out(15)`, `.In()` and `.Level()` in Sigma and
  Sigma#, and `SET REDSTONE 'Gate' IN` or `SET REDSTONE 'Gate' OUT 15` in IQL. What a program set is marked with its
  name, as on a bus, and a setting made in the window clears the mark.
- (Computers) Peripherals: Two Pattern Encoders for the new eras: the Transition one, a LightScribe DVD burner in silver
  and gloss black that writes DVDs and CDs, and the Advanced one, a Blu-ray writer in space grey with a white front that
  writes Blu-ray discs and, in its USB-C port, sticks. And the Blu-ray Drive, white all over with its slim tray high on
  the front, which reads Blu-ray discs and the older DVDs and CDs. Each takes its era's cable on its back, and each
  opens and closes its tray with its eject button and the sound the other disc drives make.
- (Computers) Peripherals: Speakers, linked to a computer over the peripheral cable like a monitor, one for every era
  from the Legacy on: the Artisan ToneWorks (Legacy); the Artisan Inspira 2.1 (Transition), gloss black satellites with
  a silver-framed mesh front, and its subwoofer, a block of its own with the bass port in front and the driver on its
  sides; the Artisan WattWorks T20 (Standard), tall and gloss black with the tweeter over a silver-ringed woofer; and
  the Artisan Cobble (Advanced). Each takes half of an audio output. A computer's sound comes out of its monitors and
  its speakers; with two or more speakers, the one to the left of whoever sits at its monitor plays the left side of a
  stereo recording and the one to the right the right, and a speaker alone plays both. The Inspira's satellites play
  everything but the bass, and a subwoofer set against one of them, with no cable of its own, gives all of its
  computer's satellites the whole range. A ToneWorks plays at 22 kHz with its bass and treble cut, a Cobble the whole
  range. Using a speaker opens its screen, in its era's look: its model, the name a program finds it by, taken when the
  screen closes and refused while another speaker of the same computer has it, whatever the case of its letters; the
  computer it plays for; the side it plays; how well; and whether a subwoofer joins it. A speaker's power light is lit
  while its computer runs. Set down, stepped on or broken, a speaker sounds like metal, as the other devices do.
- (Computers) Peripherals: Printers, one for each era, each the printer of its day on a period base: the Epsilon FX-80
  dot matrix, the Pakard DeskJot 940 and FotoSmart C4280 inkjets, the Pakard LaserJot 1102 laser and the Epsilon EcoTonk
  ET-2720 ink tank printer. A printer is a peripheral of a computer, on the port of its era in the middle of its back,
  and prints whatever the computer's programs send it: a sheet of paper from its tray for each page, a page taking as
  long as the printer's sound lasts, the page sliding out and the busy lamp blinking while it prints. Documents wait in
  its queue in the order they came; with the tray empty or no room for the next sheet in the output the queue waits,
  and paper clicked into a full tray says the tray is full. Its window, in the skin of its era, shows the paper, what
  prints now and its page, the queue with the machine each document came from, the sheets that came out, and Pause and
  Cancel job. A printer's tooltip says whether it prints in colour.
- (Computers) Peripherals: The Printed Paper, the sheet a printer turns out: its tooltip gives the title, the pages, the
  first lines and the printer, and a right click reads it page by page, on the fanfold paper with its green bars and
  tractor holes from the dot matrix and a plain sheet from the others. A printed picture shows on it as that printer put
  it on paper, and in an item frame it shows on the frame, as a map does.
- (Computers) Peripherals: Print in the programs: the Editor, Exceed, the IQL results of the management studio, the
  Network Manager's log, the Network Interactor's list, Exposure's open file, and Paint's picture. Each printer prints a
  picture in its own hand, at the resolution a real one prints at, drawn smoothed as paper is seen: the Epsilon FX-80
  in the round dots of its ribbon, the Pakard DeskJot 940 in grainy dots of four inks with paler rows from its head's
  passes, the FotoSmart C4280 in dots of ink too fine to see, the LaserJot 1102 in a fine halftone of toner, its greys
  growing a dot at a time, and the EcoTonk ET-2720 as it is. The Print window is each system's own: the classic one of
  Frames 95 and XP and the older desktops, with the printer, its status, where it is and its paper, the pages, the
  copies and the way the page lies, and on Frames 11 and the flat desktops of today the same beside a preview of the
  page; it says beside each printer's name whether it prints Mono or Color, and counts the sheets before anything is
  sent.
- (Computers) Peripherals: The Dock Station is redone as a full block, a black aluminium enclosure on four feet with
  three trays and the USB port: a hard disk in the 3.5" bay, a SATA disk in the 2.5" bay and an NVMe in the M.2 tray, of
  any era, each shown in its tray's window with its lamp, and the flash drive in the port, lying across as the port
  does. A disk going in or out of a tray sounds like a server on its rails. Its window lists each tray and the port
  with what it holds, the letter the computer gives it, its era and how full it is, whether it is mounted, and Eject.
  It takes the peripheral cable of every era, so an Advanced computer reaches it with its own cable and installs Frames
  11 from a stick in it, and a stick that installs a system or a program says what it installs.
- (Computers) Peripherals: A disk in a Dock Station's tray is an external drive of the computer the dock is linked to,
  lettered after the dock's stick: This PC and the explorers list it and open it, the shells read and write it by its
  letter, and on Linux, UNIX and FreeBSD it is mounted under `/media` or `/mnt`.
- (Computers) Monitors: Eight monitors, each drawn from a real monitor of its time: on the Vintage the Mono I
  (paper-white), the Mono II (green, the Vintage Monitor that was), the Amber and the sixteen-colour CGA; the Legacy
  Monitor, a colour picture tube; the Transition Monitor, a 19-inch flat panel; the Monitor; and the Color Monitor, a
  27-inch IPS panel, for the Advanced. Every screen has the desktop's shape, and the era's video port in the middle of
  the back.
- (Computers) Monitors: A monitor's face shows live what a player standing at its machine sees: its self-test, its
  system coming up or going down, its boot manager, its firmware and its installer, each drawn whole by the very screen
  that shows it at the machine; its prompt with the last lines printed, in the terminal's own font; or its desktop with
  the folders, files and pinned programs the machine keeps and the windows it has open, each drawn by the program
  itself when the player's game has it open or kept from the last visit. The server sends the screen a phase opens with
  once, when the phase begins, and only to the players near it; each player's game draws it, often close up, slowly a
  little further off, once a second past thirty-two blocks, and as far as the game draws, sixty-four blocks; a room of
  many monitors draws each one's picture in turn, so a room of screens costs the server nothing.
- (Computers) Monitors: The monitor's tube colours what reaches its glass: on the Mono I, Mono II and Amber every
  system comes out in that one phosphor, brighter or dimmer, and on the CGA as the nearest of its sixteen colours, both
  on the opened screen and on the face in the world. The Vintage screens no longer paint themselves green: the monitor
  does. A program that takes a terminal's whole glass (an editor, the diagnostics, the shells) reaches the player
  through the tube as the console does: on a green or amber monitor it is green or amber too.
- (Computers) Monitors: Flat monitors of one kind side by side (Transition, Standard and Color) join into one screen,
  up to eight wide and six tall, when together they fill a whole rectangle: one bezel round all of it, one chin, one
  power button, and the picture across the whole glass. It counts as one monitor on one video output.
- (Computers) Monitors: A monitor has a real power button standing out of its bezel, outlined on its own in the series'
  accent while it is looked at: it switches the monitor alone, and the computer behind it runs on, switched on and off
  at its own case or on its assembly screen. Switched off, the face goes dark and its lamp goes out, every screen open
  on it closes, and a click on its glass opens nothing and says "The monitor is off". Switched on with no picture, a
  monitor from the Legacy on sleeps with its lamp amber, as monitors with power saving did, and a Vintage one keeps its
  lamp lit. A big screen switches whole from its corner's button. A click anywhere else opens the screen. An opened
  screen has a strip beside its frame on the left with the computer's Power and Restart, each saying what it does under
  the pointer, on every screen the monitor shows, its start, its firmware's setup and an installer included.
- (Computers) Operating systems: Frames 7, the Frames of the Transition, installed from its DVD through its own setup
  (Install now, the two phases and their steps, Set Up Frames). Its windows are tinted glass with a glowing title, a
  red close button and rounded top corners. The superbar keeps programs as icons, pinned and open together, with the
  round orb that opens a Start of two columns (the programs, All Programs and the search on the left; the folders,
  Computer, Control Panel, Devices and Printers and Shut down on the glass on the right), a notification area with the
  network, the sound and the flag, a clock over the date and the corner that shows the desktop. It comes up with four
  lights that close into its flag over "Starting Frames", then Welcome beside the ring, and goes down the same way.
  This PC is Computer, the settings are the Control Panel, which opens on its categories (System and Security, User
  Accounts, Network and Internet, Appearance and Personalization, Hardware and Sound, Programs, Ease of Access), each a
  link to its page. The Calculator and Minesweeper are glass, the Command Prompt says "Midsoft Frames [Version
  7.0.7600]", the explorer carries a command bar (Organize, which opens the selection's or the folder's menu, Open and
  New folder), and the file dialogs list Favorites and Computer. It has its own icons for every program, file type and
  device, its emblem and orb, its wallpaper of blocks, its pointers, the Device Manager in glass, the copy window's "N%
  complete" over a green bar, its volume popup of glass (the outputs listed beside the slider, the Mixer opening the
  sound settings), its screenfetch flag, its own way of moving, and Ease of Access with Performance Options, either of
  which turns the animations off. Soundfoundry wears its Legacy form there. It starts with its own chime, goes down with
  the same one, and keeps Frames XP's error sound. Installing it and booting it earns "Never Upgrading" in the
  Operating Systems tab.
- (Computers) Operating systems: Frames 10, the Frames of the Standard, whose Command Prompt opens on "Midsoft Frames
  [Version 10.0.10240]". Its windows are square and white with a line of the accent round them and three wide buttons;
  the taskbar, Start and the Action Center are dark. The taskbar carries the white mark of Start, a search box that
  opens Start to be typed into, Task View (every open window side by side, a click bringing one forward), the programs
  with a line under the open ones, the notification area, the clock and the Action Center's button. Start has its rail,
  Most used and every program from A to Z beside the tiles, which the machine keeps: the right button on a program pins
  it, on a tile unpins it or makes it small, medium or wide, and a tile dragged onto another takes its place. The
  Network Manager's tile turns between its glyph and the state of the network and the System Monitor's shows the
  processor, while animations are on. The Action Center lists the notices with Clear all, and its quick buttons open the
  network, the sound and every setting, or keep the notices quiet. Notices also rise in the corner. The settings open on
  a grid of their eight pages, the File Explorer has the ribbon folded over its address (Home: Cut, Copy, Paste, Delete,
  Rename, New folder, Open and Properties; View: Large icons and Details; File: the prompt in the folder), the
  Calculator is dark, and Get started greets on a band of the accent above the machine's programs, each on its tile. It
  installs from its medium with a blue first setup that asks who will use the PC, comes up with its mark and the dots,
  shows the lock screen with the time large in its corner, then signs in with a round picture of a grass block and
  Welcome; the first start says Hi instead. It has the icons with white glyphs for its tiles, its wallpaper, a flat
  Device Manager, the copy window with its speed graph, a dark volume flyout whose arrow opens the outputs, the classic
  Print window in flat controls, Save As with Quick access, the screenfetch panes in blue, and an Effects section in
  Personalize (transparency, and animations, off with which windows open at once and the tiles stop turning).
  Soundfoundry wears its Standard form there. It starts with its own chime, goes down with the same one, and shares
  Frames 11's error sound. Installing it and booting it earns "What Happened to Frames 9?".
- (Computers) Operating systems: KDE and GNOME have the faces they had on Transition hardware. KDE is KDE 4's Plasma: a
  light, see-through panel with the orb that opens Kickoff (Favorites, Applications with each program's description,
  Computer, Recently Used and Leave, which shuts down or restarts), the pager of four desktops, the window list, the
  tray and the clock; the Folder View on the desktop holding its files, the cashew in the corner offering the desktop's
  settings, and Oxygen windows with rounded top corners. GNOME is GNOME 2 with Clearlooks: Applications (the programs by
  what they are for), Places and System along a top panel with the launchers, the clock, the user and the power; a
  bottom panel with the show-desktop button, the window list, the switcher of four workspaces and the trash; and blue
  titles. Both keep the desktop's wallpaper and the icons with Frames XP's gloss, and Soundfoundry wears its Legacy form
  on them, as on FreeBSD there.
- (Computers) Operating systems: The Experience Index of Frames 7: five scores from 1.0 to 7.9 read off the machine's
  parts (the processor, the memory, the desktop's graphics, the graphics of games and the main disk), each growing by
  about a point when the part doubles, and the base score, the lowest of the five. The System page of the Control Panel
  shows them under the machine's parts.
- (Computers) Operating systems: The basic look. A desktop that drew its effects on the graphics card (Frames 7, 10 and
  11, Cinnamon, KDE from its Transition face and GNOME from its Standard one) needs a graphics score of 3.0 for them;
  under that, and always on graphics that are part of the processor, it runs with every effect off whatever its
  settings say. Frames 7 drops to Frames 7 Basic, opaque windows in its own colours, and says so in its notification
  area; every effects page keeps the choices and says why none of them shows.
- (Computers) Operating systems: Every era's creative tab has, after its installers, a disk with each of its systems
  already on it, the way a computer comes from the shop: put in a machine of that era, it comes up at once as "My
  Computer". Each is the era's biggest disk, so the most room is left once the system is on it.
- (Computers) Operating systems: A part taken out of a running computer brings its system down: a stick of memory, an
  expansion card or the disk the system runs from puts Frames on its red page, naming the part on Frames 10 and 11;
  Linux and FreeBSD panic on a machine check, and UNIX on a memory parity error. A machine left with what it needs (the
  other stick of two) starts over once the page has stood its time; one left with no computer goes dark. The processor,
  the board or the power supply taken out switch the machine off at once, with no page, and a disk the system does not
  run from or a drive in a hot-swap bay is only a disk gone.
- (Computers) Operating systems: `ps` on Linux, FreeBSD and UNIX lists the system's own processes before the programs:
  its first process at number 1, systemd on the distributions that start it and init on Gentoo, FreeBSD and UNIX, then
  each service it runs, at a number that service keeps. The programs a player starts are numbered from 1000, after
  them, on every system. `kill` takes a signal before the number (`-9`, `-KILL`, `-SIGKILL` or `-s KILL`, TERM when none
  is given) and `kill -l` lists them. A signal to a service's process stops the service until the machine starts over,
  and one to process 1 ends the system's init: the kernel panics in its family's own words, `Attempted to kill init!`
  with the signal's exit code on Linux, `Going nowhere without my init!` on FreeBSD, `init died` on UNIX, with the line
  that did it at the top of the page, at the prompt it was typed at.
- (Computers) Operating systems: FreeBSD installs through bsdinstall, in its own grey dialogs on navy. It opens with
  Install, then asks for the machine's hostname, whether to add the ports tree, the disk (the whole of an empty one,
  beside a system already on one, or the whole of one that is erased first, after asking), where `pkg` and the ports
  fetch from (the network's Mirror, or nowhere), which services start at boot (`cron` and `sshd`) and, when the Mirror
  answers, a desktop to install with it, None always offered. It extracts the system with a gauge and ends with Reboot.
  With no Mirror it fetches nothing, and a ports tree asked for is laid down empty for `portsnap fetch` to fill.
- (Computers) Operating systems: UNIX installs from its own console installer, which names the parts of its one copy as
  it writes them, offers to erase a disk that already holds a system or to install beside it, and ends with Reboot.
- (Computers) Operating systems: `cron` is a switch, FreeBSD's `cron_enable` in `/etc/rc.conf`, on unless bsdinstall
  turned it off. Off, the jobs in a machine's crontab wait instead of running, while a job put in the background with
  `&` runs anyway. FreeBSD says "Starting cron." as it comes up and FreeBSD and UNIX say "Stopping cron." as they go
  down, while it is on.
- (Computers) Operating systems: On a FreeBSD installed through bsdinstall, other computers reach the machine only when
  `sshd` was ticked on the services page; it shows as `sshd_enable` in `/etc/rc.conf` and is the same switch as `config
  remote`. Every other system keeps letting them in, as before.
- (Computers) Operating systems: Choosing a faster disk on an installer's disk page makes the copy quicker, and the
  time left changes to match.
- (Computers) Operating systems: `ee` on FreeBSD, the editor it gives a newcomer: its five rows of shortcuts along the
  top, the row saying where the caret stands, and its Esc menu drawn over the text, whose items all work (leave the
  editor, saving or not; a page of every key, closed by any key; read a file in or save the one open; show or hide the
  rows of shortcuts; search). `vi` on FreeBSD and UNIX speaks each system's own, with no Vim status line. Both come with
  the system and are listed in `/usr/bin`.
- (Computers) Operating systems: FreeBSD's console welcome names the three ways to find things, `apropos` for a command
  by what it does, `man intro` to learn the system and Tab twice for every command here, above the day's tip. `man
  intro` is a real page, and on every Unix console a second Tab in a row lists what the first could complete to, every
  command on an empty line, as sh and bash do. FreeBSD has no `help`: typed there, it is not found like any other
  unknown word, and the line points to `apropos` and `man intro` instead.
- (Computers) Operating systems: KDE Plasma, GNOME and Cinnamon on FreeBSD hang FreeBSD's own wallpaper for each, in
  FreeBSD's reds with its orb, in place of the one they bring on a Linux.
- (Computers) Operating systems: This PC on KDE Plasma, GNOME and Cinnamon, on FreeBSD and on the Linux distributions
  alike, is the page each desktop has for it: KDE's Info Center, GNOME's About and Cinnamon's System Info, each laid out
  its own way. They show the system with its release, the kernel and its build, the architecture (vel64 or IA-32 on
  FreeBSD, x86_64 on a Linux), the desktop, the processor, the memory, the graphics card, the system disk and how much
  of it is used, and the machine's host name.
- (Computers) Operating systems: The ports tree on FreeBSD. `portsnap fetch extract` brings the tree from the network's
  Mirror and lays it out under `/usr/ports`, a folder for every program the Mirror serves, filed by category, as real
  files that take room on the disk; `portsnap fetch update` brings it up to date. In a port's folder, `make install
  clean` fetches the program's source from the Mirror and builds it on the machine, for as long as its processor takes
  with all of its cores, then installs it and cleans up. `make` on its own builds, a later `make install` finds that
  build instead of making it again, and `make clean`, `make reinstall` and `make deinstall` do what they do on FreeBSD.
  A program that installs only on one kind of machine, as the Crafting Manager does on a Crafting Computer, is refused
  elsewhere. Building a port earns the new Built from Ports advancement.
- (Computers) Operating systems: A program built from source on the machine that runs it asks ten percent less of it
  than the package does: less memory while it runs, a lesser processor, and less free disk to install. That goes for a
  port on FreeBSD and for everything Gentoo builds. The machine remembers which of its programs it built until the
  program is removed or the disk it was on is formatted; the same program installed again as a package asks what a
  package asks.
- (Computers) Operating systems: The Vintage systems get their text-mode shells, drawn in the sixteen colours of the
  colour adapter in the terminal font, which a phosphor monitor shows in its own tones. MC-DOS has the MC-DOS Shell
  (`DOSSHELL`), Midsoft's menu shell in the manner of the DOS 4 and 5 Shell: the title, the menus File, Options, View,
  Tree and Help with each one's letter lit, the path and the drives, the directory tree and the file list over the
  Main group and the Active Task List, and the key line with the clock. Every action of its menus is one of MC-DOS's
  own commands (TYPE to view a file, COPY, MOVE, DEL, REN, MKDIR, PRINT, FORMAT from the Disk Utilities), what the
  machine lacks is greyed (Print with no printer linked), and its menus and dialogs throw the DOS shadow. Its task
  switcher keeps the command prompts (Shift+F9) and programs it started, each with its own screen: Alt+Tab, with Alt
  held, names the next task in a banner, and EXIT ends a prompt. UNIX System V has PACE (`pace`), Bellwether Labs'
  Panelled Access Command Environment: numbered frames in cascade, the Office of the player with its Filecabinet listing
  each file's type, the Programs, the System Administration, and the UNIX System, a shell of its own that exit brings
  back to PACE, with the `-->` command line and the function-key labels along the bottom. Both list a folder of some
  hundreds of files, leaving out the far end of the folder tree rather than any file.
- (Computers) Operating systems: A Vintage machine runs what `scc` compiles, though it can never hold the Sigma Runtime:
  a compiled listing runs by its bare name at the prompt, on MC-DOS without its extension the way DOS finds a program,
  and on UNIX from the current directory or the PATH. `scc` says how to run what it just built, and UNIX answers a word
  it cannot find in its own shell's words, `hello: not found`.
- (Computers) Operating systems: MC-DOS prints from its prompt with `PRINT`, which installs its resident part the first
  time and lists what is printing and what waits; UNIX, Linux and FreeBSD with `lp`, which answers with a request id
  (`-d` for the printer, `-n` for the copies), and `lpstat`, which lists the requests and, with `-p`, the printers.
- (Computers) Operating systems: The Unix shells' `echo` takes `-e`, reading `\a` (the bell), `\n`, `\t` and `\\` in
  its text.
- (Computers) Operating systems: screenfetch on Frames 95, XP and 11: a package of Frames' own manager, run at the
  Command Prompt, drawing the Frames flag with the version each gives for itself. Professional Larper now asks for all
  nine systems it installs on.
- (Computers) Devices: The device tools at a Unix prompt: on Linux `lspci` lists what sits on the board (the board
  itself, its port controller, its audio and network, the graphics and expansion cards) and `lsusb` what is plugged
  into its ports, `lsusb -t` as a tree with each hub's ports under the hub and the free ports too; on FreeBSD `pciconf
  -lv` and `usbconfig`. A device the computer disabled is marked by each.
- (Computers) Devices: The Device Manager on the Frames desktops, with an icon of its own, a monitor with a gear before
  it: the machine's hardware and every port it has, what is plugged into each and which are free, hubs with their own
  ports under them, and how many device ports are in use. Devices are listed by port, by type or by connection, and a
  device is disabled or enabled again from it, crossed out while disabled. Frames 95 keeps it in System Properties with
  the views as radio buttons and the actions as buttons under the tree; Frames XP has its console window, the views in
  the View menu, the actions on a right-click and in the Action menu, and a status bar that says what disabling a
  device does; Frames 11 has the views as a switch in its menu row.
- (Computers) Devices: The Midsoft Diagnostics on MC-DOS and MC-NET, which have no Device Manager: `msd` takes the
  whole terminal and shows what the machine is made of (its processor, memory, graphics card, network, system and
  drives), and LPT Ports and COM Ports open the list of the board's ports with what is attached to each and whether it
  is on, where a device is enabled or disabled from the keyboard.
- (Computers) Devices: KDE's Info Center has a second page, Devices by port, beside About this System: every graphics
  card with its outputs, the audio and the board's ports, each port beside what is plugged into it or free, a hub with
  how many of its own ports are in use, and a button that disables or enables the device selected.
- (Computers) Devices: CDE's Workstation Info has a Devices... button that opens the workstation's devices in a dialog
  of their own: Video, Audio and every port by its name (COM1 and LPT1 on a Vintage machine) with what is plugged into
  it or free, and Disable and Enable for the device selected.
- (Computers) Devices: Every row of the Device Manager and of the Info Center has its icon, drawn in the look of the
  desktop it is on (the period one of Frames 95 and the older Linux desktops, Frames XP's with its gloss, the flat
  modern one): the board's parts, every port, and every device that stands on one, the monitors, speakers and drives of
  each era with their own.
- (Computers) Desktops: The Task Manager of every system shows the graphics card: Frames XP a Video Memory meter, its
  history and a box of its figures; Frames 11 a GPU page with how busy it is, how full its memory is, its cores and
  slot, and what holds its memory; the Plasma monitor a GPU card beside memory and processor; GNOME its video memory
  history; the System Monitor the card's name, its memory and its load.
- (Computers) Desktops: The desktops move, each with the timings of the system it imitates. Frames 11, Plasma, GNOME and
  Cinnamon grow a window in out of clear as it opens and shrink it away fading as it closes (the window is gone from
  the desktop at once, and drawn going away for as long as that takes), each window fading as one picture, and carry it
  down to its button and back when it is minimized and restored; on Frames 95 and Frames XP only the title bar flies
  down to the taskbar and back, and windows open at once, as they did; KDE 2 and 3 and GNOME 1 send its outline to the
  button. The launchers slide out of their panels, fading in on the modern desktops (KDE 3's menus roll), Frames XP's
  menus and tooltips fade in, and GNOME's overview grows in. A desktop that comes straight up from its boot picture
  comes up under that picture's colour, which gives way to it. A monitor's face in the world shows everything where it
  ends.
- (Computers) Desktops: More of each system's own motion. Frames XP's balloons fade in and out, and the desktop behind
  its Turn Off dialog drains to grey over a second and a half (a veil deepens instead where the graphics card cannot
  grey it). CDE's Front Panel has its busy light, which blinks while a program starts and while a copy runs. A wait
  with no known end (looking for printers, getting through to the Mainframe or a gateway, loading the network) has each
  system's bar: Frames XP's three green blocks crossing it, Frames 11's segment growing and shrinking, GTK 1's and KDE
  2's block going end to end, Breeze's and Cinnamon's sliding segment and Adwaita's bouncing one; Frames 95 and CDE had
  none and show the empty trough. The file managers of the period have their throbber in the toolbar's corner, Frames
  95's mark rippling like a flag, XP's with a light crossing it, Konqueror's gear and Nautilus's ring of dots, turning
  only while a place is slow to answer. Each console blinks its cursor to its own hardware's beat, on its screen and on
  the monitor's face alike: a VGA card's every 229 milliseconds (MC-DOS, MC-NET, UNIX and FreeBSD's syscons), Linux's
  framebuffer console every 200, the Frames console every 530 and CDE's dtterm every 250. The progress bars of the
  modern desktops glide to each new amount. Frames XP's Performance Options gains "Fade or slide ToolTips into view".
- (Computers) Desktops: The desktops draw the player's pointer over the monitor's glass in their own system's cursors,
  as big as the computer's own pointer however large the desktop is drawn: Frames 95's arrow and hourglass, Frames XP's
  with the sand falling, the Aero ring of Frames 11, the X core cursors of CDE, GNOME 1 and KDE 2 and 3, Plasma's
  Breeze, GNOME's Adwaita and Cinnamon's DMZ-White, the later ones with their soft shadow blended into what is under
  them, never over a program's dialog. It turns busy while the program in front waits on the machine, and to its
  working pointer while a program starts or a copy runs (busy on a system that had no working pointer, and KDE bounces
  the program's icon beside the arrow as it starts); each turns at its system's pace, and stands in its first picture
  with motion reduced. Off the glass the game's own pointer is back, and the Desktop cursors setting turns the
  desktops' pointers off. A program's start, which the working pointer, KDE's bouncing icon and CDE's busy light show,
  lasts as long as the program's weight takes to load from the machine's disk at its processor's pace: about a second
  for a light program on a quick machine, up to five for a heavy one on a slow machine.
- (Computers) Desktops: A file copy takes time: its size read at the pace of the slower of the two volumes (a hard
  disk, a solid-state or NVMe disk, a floppy, a CD, a DVD, a stick), or to another machine at the slowest cable on the
  way, and the file arrives when that time is up. A machine copies one file after another; a move to another volume
  takes as long, and putting a big file in the trash takes its disk's time. A copy that outlasts a moment shows each
  system's copy window: Frames 95's and XP's "Copying..." with the paper flying from folder to folder over a bar of
  blocks and the seconds remaining ("Deleting..." with the paper flying into the Recycle Bin), Frames 11's window with
  its speed graph and details, KDE 2 and 3's KIO progress dialog (which can be kept open), GNOME 1's gmc dialog and
  Cinnamon's File Operations; Plasma shows a notification over the panel and a ring filling at the tray, GNOME its
  operations pie and its popover, and CDE no window, only its busy pointer and busy light. Cancel calls off whatever of
  the copy is left, and the pause button of Frames 11's window, Plasma's notification and Cinnamon's File Operations
  holds the copies where they are until it is pressed again. The numbers are first estimates.
- (Computers) Desktops: Each system's own page for its visual effects, in the Settings window, kept by the machine:
  Frames 95's Effects from the Display page, Frames XP's Performance Options from the System page (its presets, and a
  box for each effect, the shadows under the icons' names among them), Frames 11's Visual effects, Plasma's animation
  speed and desktop effects, KDE 2 and 3's window behaviour and menu effect, GNOME 1's wireframe, GNOME's Reduce
  Animation, and Cinnamon's effects, speed and the style of opening, closing and minimizing windows, each from
  Personalize. The pages that were dialogs keep their OK, Cancel and Apply. Only the effects there are on these desktops
  are listed; CDE and the text consoles have none. `config effect <name> on|off` and `config effectspeed <percent>` set
  the same from a prompt.
- (Computers) Desktops: J's Computers' own client settings, beside the game's options: Reduce motion, off by default,
  which draws every desktop's windows, menus and boots where they end and holds every blinking and turning thing still;
  and Desktop cursors, on by default, for the desktops' own pointer.
- (Computers) Desktops: New wallpapers, built from blocks the way the game's own art is: a stepped grass hill under slab
  clouds for Frames XP, a blue flower drawn like an item for Frames 11, a wall of Breeze-blue blocks for KDE Plasma, a
  blocky dusk with a square sun for GNOME and green block stairs for Cinnamon; Frames 95 keeps its plain teal. Frames 11
  hangs a dark version of its flower while its dark theme is on. They are pictures a resource pack can replace, under
  `assets/jsc/textures/gui/wallpaper/`, and the thumbnails in Personalize, which scrolls when it holds more than its
  window does, are the pictures themselves.
- (Computers) Desktops: The rest of what the desktops drew out of rectangles is pictures too, in the same block style:
  each Frames edition's mark on its Start button, the sky Frames 95 starts on and the bands Frames XP welcomes and
  closes on, the marks KDE Plasma and Cinnamon come up behind, the pictures on CDE's front panel, the network and
  speaker icons of every panel, and the picture down the side of the Frames 95 wizard: a computer, a boxed program and
  a disk. The clock's hands, the calendar's day and everything that moves are still drawn over them.
- (Computers) Desktops: File icons are pictures in each desktop's own style: a Frames 95 folder is not a GNOME one.
  Seventeen kinds of file, from folders and documents to Sigma source, programs, packages and pictures, each drawn at 16
  by 16 for every desktop, in the explorer, the file dialog, the studios' trees and on the desktop itself. They are
  files a resource pack can replace, under `assets/jsc/textures/gui/file/`.
- (Computers) Desktops: Icons for every program on CDE: twenty-eight programs that run under it, from the Network
  Management Studio to Vim, Emacs and the desktops themselves, wore the Frames 95 icon there and now wear CDE's. The
  Help Viewer has an icon on every desktop it runs on, Workstation Info has one outside CDE as well, and a setup, the
  welcome and a window a Sigma program opens show their own icon on the panel and the title bar instead of the plain
  one.
- (Computers) Desktops: A resource pack can recolour the computing screens. Every colour of them is a palette, under
  `assets/jscore/palettes/` and `assets/jsc/palettes/`: each hardware era's skin; each desktop's windows, panel, start
  menu and launcher, and the chrome each style of desktop draws its title bars, buttons, fields and tabs with; CDE's
  eight colour schemes; the terminals' inks, on glass and on paper, and their popups; the code and query editors and
  their highlighting; the accents Settings offers; the firmware's setup, self-test, boot menus and install screen, the
  installers' frames and pages, the splash screens and the systems' start and shutdown pictures; the monitor frames and
  the Server Rack's cabinet of each era; every program and game (the Network Interactor and Manager, the studios,
  Messenger, Files, Knot, Storage Insights, the Gateway, Cluster, Automation and Craft Planner managers, Paint, the
  Style Manager, Minesweeper, Snake, Solitaire and its cards); the Operation types' colours; the desktop's own colours
  (the crash page, a dragged label, the selection band, the tray balloon, the power shade); J's Core's components,
  widgets and dialogs, the green of a Vintage tube and the toasts' accents; and the Industrial machines' screens. A file
  gives a colour for each named role (`text`, `accent`, `panel` and so on), written `#AARRGGBB`. A pack's file may name
  only the roles it changes, and the others keep the colours the mod ships; a file that cannot be read is logged, and
  its screens keep those colours too. A change of pack reaches a screen that is already open. Only the colours an image
  file stores stay in it. A theme preset still puts on the accent the mod ships, since the machine that keeps it knows
  no resource pack.
- (Computers) Desktops: Text is drawn plain, with no shadow, on every screen of the series: the labels, fields, menus,
  lists, buttons, tabs, terminals and the machine screens, which used the game's dark shadow and smeared on the light
  panels, and the last that kept one, Frames XP's window titles, its Start button and its Start menu's name and
  buttons, and the labels and check boxes of a window a Σ program opens. In J's Core, whoever paints a background still
  says which colour it is, and the rule for a shadow worked out from the letter and that ground is kept, so a shadow can
  come back where it reads better.
- (Computers) Programs: Soundfoundry, Voidsoft's music player, for the desktops of the Legacy era and later. Its
  playlist and how it plays belong to the machine, so the music plays on with the screen closed, the next song follows
  when one ends, and it stops when the machine goes off. It plays out of the machine's monitors and speakers as its
  sound hardware plays them, at its own volume on top of the system's, with a balance that turns the left or right
  speaker down. It shuffles through the whole list before a song comes round again, repeats the list or stops after the
  last, and keeps playlists as `.m3u` files. Songs open in it, and songs brought from the player's own computer can go
  straight onto its playlist. A machine whose sound only beeps has nothing to play a song on, and says so. Its window
  wears a skin of its own, the same on every desktop: dark iron plates with rivets and displays lit in amber, the time
  in seven-segment figures, an analyser whose bars move with what is being heard, the song scrolling by with its rate
  and whether it is stereo, where the sound comes out, sliders for the volume, the balance and the point in the song,
  and the buttons to play, pause, stop and skip. The eject button opens a song or a folder of them, or imports songs
  from the player's own computer; the playlist under it adds, removes, picks, sorts and keeps lists from its buttons,
  plays a song on a double click, and can be put away with PL, and either part folds down to its bar. The keys Z, X, C,
  V and B go back, play, pause, stop and go on.
- (Computers) Programs: Soundfoundry's sharing window, which NET opens, in the same skin: a search finds songs by their
  title, artist, album or file name among Voidsoft Music, the server's catalogue sold as the online store of the time,
  and the songs the other computers of the network running Soundfoundry keep in the `Shared` folder of their music
  folder, each listed with its size, the computer it is on and the slowest cable on the way to it, its signal bars
  following how fast a song really comes over that cable. A song picked and downloaded comes in at a speed in proportion
  to what that cable carries, the songs coming in at once sharing it (a catalogue song at the speed of the computer's
  own cable), with its progress and time left, and is kept in the music folder, and on the playlist when asked. It
  waits while the computer sharing it is off, goes on after the world is loaded again, and is given up on, saying why,
  when the song stops being shared. The downloads and the songs this computer shares have a tab each, and the bar along
  the foot says how many songs it shares, how many computers it reaches and how many songs are on their way. The
  Ethernet speed under `[soundfoundry]` in `jscomputers-server.toml` sets the scale. A window now opens inside the
  desktop's work area wherever it fits.
- (Computers) Programs: Soundfoundry Server, a Voidsoft service for a server in a rack: it streams the music catalogue
  and the network's own library, which is the songs in the server's music folder, to the computers of the network. A
  song a computer sends it lands in a folder named after that computer. Each computer listening holds 4 MB of the
  server's memory, which the server's memory listings show, and a server with none left turns the next one away; a song
  playing from a server that goes off or leaves the network stops and says why. Several servers can serve one network.
- (Computers) Programs: Soundfoundry on the Standard desktops, Frames 11 and the Linux desktops of that era, is
  Voidsoft's streaming player: a home page with the catalogue's albums, the network's songs and those downloaded; an
  album's page with its songs, each marked as on the disk or only streamed, and a button that downloads the album; a
  search through the catalogue, the network's library and the computer's own files; the computer's own files, brought
  from the player's computer or sent to the network's library; playlists, the liked songs first, kept as `.m3u` files;
  and the Soundfoundry Server streamed from, picked when there are several. Playing an album, a playlist or a page's
  songs puts them on the list; a song on the disk plays without a server. Songs download and go up at the speed of the
  slowest cable between the computer and the server. Its window wears Voidsoft's graphite and orange on every desktop,
  and is the first window drawn in its own skin that can fill the desktop.
- (Computers) Programs: Covers on the Standard Soundfoundry: a catalogue album shows the `cover.png` (or `cover.jpg`)
  beside its songs, any other song the picture its own file carries, and a song with neither a cover made of its
  album's colours and initials.
- (Computers) Programs: Music files on a computer's disk: Ogg Vorbis (`.ogg`) and Wave (`.wav`), with an icon of their
  own in every desktop's style. A song is brought from the player's own computer, picked in their system's file dialog,
  into the system's music folder (`Users/Public/Music` on Frames, `Music` in the home folder on Linux, FreeBSD and
  UNIX), and is refused before it is sent when the disk has no room for it; one that finds the room gone by the time it
  has arrived is said not to have gone in, rather than to have. The recording stays on the server and the disk keeps a
  file naming it, but that file weighs what the song weighs, by the disk's era like any other file, and an archive with
  songs packed in it weighs them too; the Archiver shows their sizes in megabytes. A song cannot be edited or added to,
  renamed into text, or made out of text.
- (Computers) Programs: The server's music catalogue: the albums its owner puts in
  `config/jstech/soundfoundry/catalog/`, a folder each, and those a data pack carries in `soundfoundry/catalog/`, named
  by an optional `album.json`, else by what their songs' tags say, else by the folder. It is read when the server starts
  and when its data packs are reloaded, and `/soundfoundry catalog reload` reads it again for an operator, saying how
  many albums and songs it found; a file it cannot read is passed over and named in the server log. `catalog` under
  `[soundfoundry]` in `jscomputers-server.toml` turns it off. The documentation has a page on music on a server.
- (Computers) Sound: J's Computers' machines make their sounds, from real recordings. A computer's power button clicks;
  a computer beeps once when its self-test passes, through the Transition, whose firmware is still a BIOS, while from
  the Standard on, with a UEFI, a machine comes up quiet; a Vintage computer comes on with the noise of its fan and
  drives; a computer with a hard drive hears it spin up, turn while it runs and wind down when it goes off, while one
  with a solid-state disk stays quiet. A monitor sounds as it lights. The drives sound as media go in and come out: a
  floppy disk slides in and is ejected, the tray of a disc drive and of a Pattern Encoder rides out and in, a USB drive
  is plugged into and pulled out of a Dock Station, and a Floppy Drive's head is heard stepping while a system or a
  program installs from its disk. Each running server in a rack is heard by its fans, and five or more running close
  together, in any number of racks, are heard as the hum of the room instead.
- (Computers) Sound: More machine sounds: a Vintage or Legacy computer switched on with parts that do not make a
  computer (no processor, no memory, parts its board does not take) beeps its self-test failing; a hard drive is heard
  seeking while its machine boots, installs, or reads and writes files on it; a picture tube is heard going dark, and
  a flat panel goes dark silently; a server or a rack unit slides into and out of its bay on its rails, and a rack that
  is broken drops them quietly; a CD or DVD drive is heard turning while a system or a program installs from its disc;
  and a running Vintage Mainframe's tape reels are heard turning.
- (Computers) Sound: Frames 95, XP and 11 have their own sounds: a chime when the desktop comes up, the same chime when
  the system shuts down, and an error sound when it raises an error box. They come out of the computer's monitors and
  speakers, heard by everyone near them, and only through a sound card or the sound on the board: a machine with
  neither, or with no monitor and no speaker, only beeps. They are sound cues picked by the system a machine runs, so a
  resource pack can give any other system chimes of its own.
- (Computers) Sound: The Unix desktops have their own sounds, taken from the sound themes they ship: GNOME and Cinnamon
  the freedesktop.org theme's error, notice, bell and device sounds, and GNOME on a Legacy machine the start-up,
  shut-down, error and notice of its second series; KDE Plasma the Ocean theme's chimes, error, notice, bell and device
  sounds, and on a Legacy machine the Oxygen theme's. GNOME today, like the real one, makes no sound coming up or going
  down. The Frames editions gain a notice and a bell, and XP and 11 their device sounds. A system sounds its notice
  when its desktop raises a notice, and when a craft, a program's setup window or a build in Virtual Studio finishes;
  its error when a craft, a query or a build fails; its bell when a click lands beside a box waiting for an answer, or
  a Unix shell prints the bell character; and its device sounds when a USB drive goes into or comes out of a Dock
  Station of a computer up at its desktop, or when its network comes up or goes down. The systems with no bell of their
  own (MC-DOS, MC-NET, FreeBSD and UNIX at their console, and CDE) ring the speaker in the case instead, as does a
  machine with no sound to play one.
- (Computers) Sound: The minefield clicks as its cells are opened and flagged, goes off when a mine is opened and plays
  a jingle when it is cleared, out of the computer's monitors like the system's sounds.
- (Computers) Sound: Each system keeps its own sound settings on its disk: how loud it plays, whether it is muted, and
  where its sound goes, out of the monitors, the speakers or both (both on a fresh system). A choice with nothing linked
  to play it gives way to what is linked, so a machine with somewhere to play is never silenced by it. On every system,
  `config volume 60`, `config mute on` and `config output speakers` change them and `config` lists them. They touch
  only what the system plays; a machine's own noises, its drives and fans, stay as they are.
- (Computers) Sound: Every desktop turns its sound from the speaker on its panel, each in its own way: Frames 95 and XP
  open the small Volume popup with an upright slider and a Mute box; Frames 11 its quick settings, with an arrow that
  opens the choice of output; KDE Plasma its Audio Volume applet; GNOME its system menu, dropped from the top bar;
  Cinnamon its sound applet with a Mute output switch; and the period KDE and GNOME of a Legacy machine the mixer popup,
  whose Mixer button goes to the settings. The speaker wears a red cross while the system is muted, tells the volume
  when the pointer rests on it, turns it a step for each notch of the wheel, and opens a menu with Sound settings on
  the right button. CDE, which keeps no speaker on its panel, has an Audio page in its Style Manager: the volume on a
  scale, Mute, and the monitor and the speakers as toggles that may both be on.
- (Computers) Sound: The Settings window's Sound page: the volume, Mute, the output, what plays the sound (the sound
  card by its name, the board's own sound or the speaker in the case), the speakers linked with the side each plays,
  and Test, which plays the system's startup sound the way the settings now have it. A speaker's screen says it is off
  while its computer plays only out of the monitor.
- (Computers) Sound: A computer's sound hardware plays as many sounds at once as it has voices: 9 on an FM card, 32 on
  a wavetable card, 64 on a board's own sound, and one in the case. A stereo song takes two voices of a card that plays
  both sides and a mono one takes one, each note of a chord one and each chime of the system one. A sound that finds no
  voice free takes those of the sound that started first, which stops, and Soundfoundry says so when it was its song; a
  new beep cuts the one before it off. A paused song holds no voice.
- (Computers) Σ and Σ#: Σ and Σ# have versions, one number for both: 1 is the language as 0.4.0a shipped it, and this
  release brings 2. The version lives in the compiler's package, sgsc 2.0 being Σ# 2, so a machine whose compiler is
  still 1.0 builds Σ# 1 until an upgrade brings it up (`pckmgr upgrade`, the upgrade verb of each Linux and BSD
  manager, or on MC-DOS installing the compiler again from newer media). A build can be held to an older version with
  `--lang N` on sgsc and scc, or with a `langversion: N` line in a project, and what came later is refused where it was
  written: "'Sound' needs Σ# 2; this project is Σ# 1". Both compilers say which they are before anything else ("Σ#
  Compiler 2.0"). The editors follow the machine's compiler, and Virtual Studio the project as well: the list of
  suggestions offers only what that version has, and the margins and the builds hold a file to it.
- (Computers) Σ and Σ#: Virtual Studio's Project Properties has a Language version under the Platform target: Default,
  which follows the installed compiler and says which one it is ("sgsc 2.0"), and a button for each version of the
  language. A version's button writes `langversion: N` into the project and Default takes the line out again; under
  them, the error a lower version gives for what came after it, and the line the choice wrote.
- (Computers) Σ and Σ#: Programs make sound. Σ# and Σ have `Sound.Beep`, a beep out of the speaker inside the case;
  `Sound.Tones`, a tune through the sound card, written as notes by name or pitch (`C4:250 E4 G4 C4+E4+G4:500`) with
  rests, chords and lengths; `Sound.Play` and `Sound.Stop`, a song from the machine's disks out of its monitors and
  speakers; and `Speaker.Named`, a linked speaker found by its name, which plays a song out of itself alone. A tune
  holding something that is no note stops the program, naming it. A machine with no sound card plays its tunes out of
  the case, one square note at a time. The documentation of the language has a section on it.
- (Computers) Σ and Σ#: Σ 2 brings the old names the languages of those machines used, written with no type in front
  of them, for what the library already does: `puts`, `gets`, `exit`, `abs`, `sqrt`, `pow`, `floor`, `min`, `max`,
  `strlen`, `strstr`, `atof`, `itoa`, `sprintf`, `rand` and `srand`. Each is the library's own call written the short
  way (`puts(s)` is `Console.PrintLine(s)`, `strlen(s)` is `s.Length`, `rand()` is `Random.Next(32768)`), so a program
  compiles to the very listing the long way gives, and Σ# has them too. `sprintf` reads its format as `printf` does and
  gives the text back instead of printing it. A method or a variable of the program's own under one of those names is
  still the one called, so no program changes. Σ's `Standard` library gains `Random` in the same version, with `Next`
  and `Seed`. The editors offer the old names, each way it is written, priced as the call it stands for, and only to a
  project whose version has them; `computers/docs/SIGMA.md` lists them with the long way of each.
- (Computers) Σ and Σ#: `printf` and `sprintf` in Σ 2 read the rest of what C wrote in a hole: flags, widths,
  precisions, `%u`, `%x`, `%X`, `%o`, `%e` and `%E`, and put the value in the hole the way C does (`%05d` of -42 is
  `-0042`, `%.2f` rounds the number the machine holds half to even). A number with a fraction and no precision is still
  written as the language writes it. A plain hole compiles to the same listing as before; one of these is written down
  as a single call that formats the value, and the first version refuses it for the version it needs.
- (Computers) Σ and Σ#: Σ 2's small helpers: `strcmp`, `toupper`, `tolower`, `isdigit`, `isalpha`, `isspace`, `itoa(n,
  base)` for a base from 2 to 36, and `atoi`, which reads text that is not a number as 0. They stand for calls the
  library gained in the same version and that Σ# can write the long way: `string.Compare`, the `char` type's `ToUpper`,
  `ToLower`, `IsDigit`, `IsLetter` and `IsWhiteSpace`, `Convert.ToString(n, base)` and `Convert.ToInt(text, fallback)`.
  A member added to a type the first version had is refused there the way a later type is ("'string.Compare' needs Σ#
  2"), and the editors do not offer it to a project held to that version.
- (Computers) Σ and Σ#: A program's Main in Σ 2 may be `static int Main()`, whose answer is the code the program ends
  with (what a parent reads from `Process.ExitCode`, unless the program ended itself with `Program.Exit`), and may take
  `string[] args`, handed the words the program was started with. The first version refuses both shapes for the version
  they need.
- (Computers) Σ and Σ#: A program's console keeps an open line, the way the consoles of the old machines did:
  `Console.Print` and a `printf` with no line break leave the line open for what follows, `PrintLine` and a break end
  it, and it ends when the program does. A program that waits for a line asks with what it left open, which stands
  where the prompt was at a desktop's terminal and at a machine's own prompt, and the answer typed goes on the same line
  (`How many ingots? 16`). What is typed is written into the program's console as it is typed, so every terminal looking
  at it shows it once and a save keeps the whole exchange, the open line included. Σ 2's `putchar` prints one character
  on the open line, the `Console.Print(char)` it stands for.
- (Computers) Σ and Σ#: Reads of less than a line in Σ 2: `getchar` takes the next character typed, the line break at
  its end included, and `scanf("%d", out n)` reads one value into the variable handed with out (`%d`, `%i` and `%u` a
  whole number, `%f` and `%e` a number, `%s` a word, `%c` a character), giving 1 when there was one and 0 when what was
  typed was not one; what is left of the line waits for the next read. They stand for `Console.Read()` and
  `Console.Scan(out value)`, which Σ# can write the long way. Text that is not the value asked for lets its line go
  rather than stopping every read after it.
- (Computers) Σ and Σ#: Files as C's stdio works them, in Σ 2 and Σ# 2: `FILE f = fopen(path, mode)` with C's six
  modes, `fclose`, `fgets(out line, f)`, `fputs`, `fprintf`, `fscanf` of one value, `fgetc`, `fputc`, `feof`, `rewind`,
  `fseek` and `ftell` counted in characters, `remove` and `rename`, on the machine's own files and the same terms as
  `File.*`. A file opened is one of the program's own objects, kept with it in a save; what it writes reaches the disk
  when the file is closed or when the program ends with it open. They stand for `File.Open`, `File.Move` and the new
  `FILE` type's `Close`, `ReadLine`, `Read`, `Scan`, `Write`, `Seek`, `Position` and `AtEnd`. A program holds only so
  many files open at once, by its machine's era: 8 on a Vintage machine, 20 on a Legacy one and 64 from the Transition
  on, after which `fopen` gives `null`, as C's does when a table of open files is full. The count is of the files the
  program holds, so it is the same after a save and a load.
- (Computers) Σ and Σ#: `strcpy(out dest, src)` and `strcat(ref dest, src)` in Σ 2, in C's order, compile to the
  assignment and the joining written by hand (`dest = src`, `dest = dest + src`), to the same listing. `ref` is written
  for `strcat` alone; a variable called `ref` is still a variable.
- (Computers) Σ and Σ#: The widgets of Σ# 2 for programs' windows: `TextArea`, `NumberBox`, `Slider`, `RadioGroup`,
  `ComboBox`, `TabView`, `GroupBox`, `ScrollView`, `Table`, `TreeView`, `MenuBar`, `ContextMenu`, `StatusBar`, `Image`,
  `Chart`, `LogView`, the system's own `OpenFileDialog` and `SaveFileDialog`, and the computers' own `ItemSlot`,
  `ItemPicker` and `OperationView`. Each system draws them in its own look, Frames 95, XP and 11, KDE 2 and Plasma,
  GNOME 1 and GNOME, Cinnamon and CDE: a radio button is round or a diamond, a tree opens with a boxed plus or a
  chevron, a slider's thumb is a raised block or a dot of the accent. Each is saved with its program like the widgets
  before it.
- (Computers) Σ and Σ#: A Σ# program on a machine that only has its terminal, as the Vintage systems do, MC-DOS among
  them, opens its windows there in letters, as the full-screen programs of the age drew their dialogs, instead of
  stopping for want of a desktop: the machine's ground in blue, a menu bar along the top, the window in a double frame
  with its shadow, the status line along the bottom. Tab moves between the widgets, the arrows move inside one, Enter or
  Space presses, Escape shuts what is open, F10 opens the menu bar, and a click lands where it is. A picture, a chart,
  an item, an operation, a canvas or a component that draws itself has no letters to be drawn in, and a program making
  one there stops, saying so; a machine with neither a desktop nor a terminal still stops it, saying so.
- (Computers) Σ and Σ#: `GenericComponent`, a component of a kind another mod adds: it holds any value of the language,
  copied when it is handed over and charged by its size (a draw and the price of every started 4 KB, in the editor's
  tooltip and when it runs alike), and tells the program what a player did as a `ComponentAction`, a name and a value,
  through `OnAction`. What it holds is checked against the bounds every component shares (no handlers, files, threads,
  windows or widgets, nothing that holds itself, at most 32 KB, sixteen levels deep and 4096 parts) and against what
  its kind takes. A game without the mod shows a placeholder naming the kind. A kind that reaches outside the game is
  off unless the server's settings and the player's own both turn it on (Outside components, in each).
- (Computers) Σ and Σ#: A program a mod writes in Java can open a window of its own on a desktop, drawing itself or a
  surface of pixels or of the graphics card, for whoever opened it; another player at the machine and the monitor's
  face in the world see a placeholder in its window, and one whose drawing fails shows the placeholder from then on. A
  window with a surface holds video memory on the machine, as a paint program's does.
- (Computers) Manuals: The Guide to Operations, J's Computers' own manual: a beige binder with a cyan band, in the
  mod's tab among the programs, whose bar lights blue blocks one after another as a desktop of the 2000s did while it
  loaded. It is a guide written to be read, in thirteen chapters, from starting a first computer to the achievements
  and the other books: what a computer is in the mod, a first computer built part by part, the firmware with pictures
  of the real BIOS and UEFI screens of each age, the parts inside a computer, the computers, their systems and
  programs, the network, Operations and their catalogue, storage, autocrafting and the devices, each idea before the
  parts that serve it, and what a first network needs (a Mainframe runs nothing until a system such as MC-NET is
  installed on it, so it needs a disk and a graphics card too).
- (Computers) Manuals: Six books beside the Guide to Operations, each an item among the programs of the J's Computers
  tab, and each dressed as the manuals of its systems' time were: the Frames Resource Kit, for MC-DOS and every Frames,
  a white paperback with Midsoft's blue spine and band; the Linux Desk Reference, for Fedora, Debian and Ubuntu,
  graphite on a wire coil with a green prompt; Linux by Hand, for installing Gentoo and Arch Linux by hand, a kraft lab
  notebook of squared paper with paper flags, a pasted label and an INSTALL LOG stamp; the Daemon Handbook, for FreeBSD
  and UNIX System V, a red cloth hardback in gold with a ribbon; Learning IQL, purple, with a table of results on its
  cover; and the Book of Sigma, white with a large blue Sigma. The three books of the UNIX-like systems share their
  chapters on the shell, the desktops, the editors at the terminal and finding help. They are written whole: the
  Midsoft systems, MC-DOS, the Frames desktop, programs and editions and the Command Prompt; the Linux distributions
  and their install; Arch Linux and Gentoo by hand, the disks and what to do when the reboot refuses; FreeBSD and UNIX
  System V; IQL from a first query to its engines, tools and recipes; and Sigma from a first program to its library,
  errors, packages and worked examples. Every command a system runs has its page in that system's book, every program
  and system is named or explained, every verb and clause of IQL is in Learning IQL, and every member of the Sigma
  library and every error of its compiler is in the Book of Sigma. Every page speaks to its reader, says what its
  numbers count, and a word used before its own page leads there. In English and Brazilian Portuguese.
- (Computers) Manuals: Twenty-one scenes in the Guide to Operations, each beside the page it shows, with numbered
  balloons: the first computer and its monitor, the front of a case and a case open with every part in its seat, a
  running Mainframe with its service panel off, a Server Rack, a supercomputer, a datacenter, a Cluster Management
  Computer and its racks, monitors joined into one screen, devices on a hub, the Dock Station, a first network, routers
  and fibre, failover, storage in a rack, buses at a chest and a furnace, the whole autocrafting line, the Pattern
  Encoder, an Input Router and a Receiving Bus, a Redstone Interface lighting a lamp, and the Network Gateway beside a
  ComputerCraft computer.
- (Computers) Manuals: The manuals inside the computers, beside the manual pages of each machine's commands, in each
  system's own help. Every desktop has Help: Frames 95's Help Topics with its Contents, Index and Find and a topic
  window of its own, XP's Help and Support Center, 7's Help and Support, Get Help on 10 and 11, KDE's Help Center, the
  Help of GNOME and Cinnamon, and CDE's Help Viewer, each with the books of the Technical Reference, the Guide to
  Operations and the Plant Drawings, a search over the entries and the commands, links between pages, and Back. At a
  prompt, `help` on MC-DOS and MC-NET takes the whole screen in the sixteen colours, with its contents, index, links
  and Find; `info` on a Linux distribution reads every chapter, section and entry as a node with its menu; and `man` on
  UNIX and FreeBSD pages an entry as a page of section 7 (`man graphics-cards`). A system's help opens first on the
  book written for that system, whose top page lists the other manuals after its chapters, so a Help with no tree of
  its books, as GNOME's and CDE's, still reaches every one.
- (Computers) Manuals: The computers' help programs read the manuals' new parts: a table of several columns as columns
  of text, set out row by row on a screen too narrow for it; listings and sessions as their lines; keys by their names;
  scenes as their captions and the list of their balloons. A command whose page a manual prints shows that page at
  `man`, at `HELP` on MC-DOS and in each desktop's Help.
- (Computers) Manuals: Pages for the commands in the manuals, made from each command itself: what it is for, how it is
  typed, what it does, a table of its options and one of its examples, every system it runs on (with the package to
  install first, where one is needed, said once when every system needs the same one) and the commands to read next,
  each leading to its own page. They say what `man`, `help` and `/?` say at a prompt, word for word, so the book and the
  machine never disagree.
- (Computers) Modpacks: With KubeJS installed, a modpack's scripts reach J's Computers as `JsComputers`:
  `JsComputers.installer('frames_95')` and `JsComputers.programInstaller(...)` give an installer with its software
  written on it, which no plain item id can name, `JsComputers.readyDisk(...)` a disk with a system already on it, and
  `JsComputers.systems('legacy')`, `JsComputers.programs(...)` and `JsComputers.eras()` list what there is. With the
  item tags below, a modpack can give J's Computers recipes and chest loot of its own. KubeJS stays optional: without
  it, nothing of it loads. The page Recipes and loot (`computers/docs/MODPACKS.md`) shows how, with a datapack or with
  KubeJS, and the Guide's first page and its page on the books say a modpack can give the mod recipes, which the recipe
  viewer then shows.
- (Computers) Modpacks: Item tags for J's Computers, so a datapack or a script can ask for a whole group of its things:
  `#jsc:era/vintage`, `#jsc:era/legacy`, `#jsc:era/transition`, `#jsc:era/standard` and `#jsc:era/advanced` hold
  everything of an era, and one tag per shelf of the creative tabs holds that shelf in every era:
  `#jsc:personal_computers`, `#jsc:crafting_computers`, `#jsc:cluster_management_computers`, `#jsc:mainframes`,
  `#jsc:racks_and_servers`, `#jsc:rack_bays`, `#jsc:peripherals`, `#jsc:network`, `#jsc:components` and
  `#jsc:programs`. J's Core writes them from the tabs, which a mod's tab or shelf asks for when it is declared.
- (Computers) Creative tabs: J's Computers shows its things in six creative tabs: one for what every era shares, then
  one for each era from the Vintage to the Advanced, side by side in that order, with the era's own icon (a floppy, a
  CD, a GeForce 8800 GT, a Haswell board and the RTX 5090). Each tab has the same shelves: Personal Computers, Crafting
  Computers, Cluster Management Computers, Mainframes, the server rack with its servers, the rack's bays, the
  peripherals, the network, the components, and the programs (blank media, then the installer of every system and
  program of that era). A thing is in the tab of the era it carries (a machine's chassis, a part's specification, a
  cable's generation, a medium's format), and in one tab only.
- (Computers) Advancements: Sixty-six advancements in four tabs. Hardware follows the machines, from the first
  self-test and the eras a computer can be built in up to racks, datacenters, clusters and supercomputers; the Time
  Traveller challenge asks for a working computer of every era, from the Vintage to the Advanced. Operating Systems has
  one for the first install of each system, the prompts and desktops, and challenges for installing every distribution
  and running screenfetch on every system that takes it; the three advancements of the old Computers tab moved into
  it, so a world that had earned them shows them unearned there. Networks & Operations covers joining a Mainframe,
  Operations, IQL, autocrafting and the ways a network goes wrong, and Sigma goes from installing the compiler to a
  program that stays up for a week. Some stay hidden until earned, and the one for hearing a ComputerCraft computer
  through a Network Gateway exists only when ComputerCraft is installed.
- (Computers) Advancements: An advancement goes to the player who caused it. What a machine does on its own, such as a
  self-test ending, a job firing or an Operation finishing, goes to whoever works that machine: the last player to place
  it, open its screen or build it, which the machine keeps across restarts. An Operation typed at a terminal goes to
  the player who typed it, however long it takes to finish and across a restart of the world. What a player earns while
  they are away is given to them the next time they join.
- (Computers) Documentation: J's Computers' documentation, in its `docs` folder, written for a reader who knows nothing
  of the mod yet, with what can go wrong at the end of every page: to players their first computer and network, the
  hardware and what every number on a part means, the systems and how to install them, the network and IQL, and
  autocrafting; and to addon authors everything a computer is made of, with the code that adds each. The cables are
  explained in `computers/docs/CABLES.md`: the shared cable block and its colours, each data line with its cables,
  speeds and ranges by era, the routers and repeaters, the buses and crafting parts, and the peripheral cables. The
  pages on cables, Σ#, ComputerCraft and music moved to J's Computers' folder.
- (Computers) Design: The design of J's Computers, in `computers/docs/design`: one page per subject, from the eras and
  the hardware to the network, Operations, the systems, Σ#, sound and the API, each saying how its system works and
  ending with what is designed and still to be built, gathered on the first page; with the formulas in one place and a
  catalogue of every part of every era with its figures. It follows J's Industrial's design where the two meet: how
  hardware is made with and without J's Industrial, the energy computers draw, the cable systems, storage for every
  state of matter, research groups in the Cluster Manager, and recipes that stay visible before they are researched;
  and J's Space's: the computers J's Space adds, its telemetry line, the Enigmatic hardware and networks across worlds.
  The series' README now says plainly that every mod of the series depends only on J's Core and never on another mod of
  the series.
- (Core, Computers, Industrial) Translation: Brazilian Portuguese (pt_br) for J's Core, J's Computers and J's
  Industrial: every word the mods show, from the blocks and items to the desktops, the programs, the terminals and the
  installers. The names of the fictional products and makers, the commands a player types and the files on a virtual
  disk stay as they are.
- (Core, Computers, Industrial) Logos: Logos and icons for the series and for each mod. The mods list shows each mod's
  logo over the series logo, mod list screens that show an icon show each mod's own, and the READMEs carry the logos.
- (Industrial) Energy: J's Industrial's Energy Cable, laid in the shared cable block's energy lane: it carries any
  amount of energy any distance and loses none of it.
- (Industrial) Manuals: The Plant Drawings, J's Industrial's own manual, a slate folder of blueprints in its tab whose
  bar runs in black and yellow stripes: a drawing list, then every machine drawn from above, the front and the side,
  traced from its own block, with numbered balloons, set up in a plan seen from above, on sheets with a frame, zones and
  a title block (JI-102, sheet 1 of 2).
- (Industrial) Documentation: J's Industrial's documentation, in its `docs` folder, explains every machine with its real
  numbers, and its recipes, tags and capabilities to mod and pack authors, with what can go wrong at the end of every
  page.
- (Industrial) Design: The design of J's Industrial, in `industrial/docs/design`: one page per subject, from the
  materials, minerals and the world to metallurgy, the tiers and their circuits, energy, machines, fluids and gases,
  chemistry, conveyors, the Clean Room, science, nuclear power, the exotic materials of the last tiers and the Aleph,
  tools and armour, and what it does with J's Computers; each page says how its system works and ends with what is
  designed and still to be built, gathered on the first page, with the formulas and every setting in one place.
- (Space) Design: The design of J's Space, in `space/docs/design`, before any of its code: one page per subject, from
  the universe, its twenty-six star systems and every world to minerals, travel, propellants, rockets, energy, life
  support, satellites, exploration, starships, ruins and Enigmatic sites, mining, cargo, terraforming and
  megastructures, and what it does with J's Computers; each page says how its system works, and the first page gives
  the order of building. J's Core's design follows it where they meet: its basic cable, pipes and battery, which any
  mod uses on its own, its containment of exotic matter, the dead stars that give exotic matter, and the space module
  of the closed exo-suits.
- (Core) Manuals: Manuals, for every mod to write in: a mod declares its chapter once, as entries of ready blocks
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
- (Core) Manuals: The Technical Reference, the series' manual: a navy binder holding a chapter for every mod installed
  (the series, J's Core, J's Computers, J's Industrial), handed to each player once, the first time they join a world,
  above their hotbar, and in J's Core's new creative tab. Every item of the three mods is the page of an entry, in
  English and Brazilian Portuguese, and every number on them is the mods' own.
- (Core) Manuals: Manuals open each chapter on two facing pages, its number large in the chapter's colour with its
  title and what it is about, and its sections with their icons and pages; a page is left blank before a chapter that
  would open on a right page, as printed manuals do. Recipes show their time over the arrow, their energy under it, and
  the name of what they make. A mod writing a chapter can add notes, a block's three views with balloons and their
  legend, plans of blocks seen from above, and breaks to the next column or page; can make one entry the page of a
  whole family of items; can print a mark on a manual's cover and hand a manual to every player once; and can draw its
  manual as a binder or as a folder of drawings, in two columns, with its own palette.
- (Core) Manuals: A mod's manuals can be books made of chapters: a mod writes as many chapters as its books need, a
  book prints the ones it names in the order it names them and numbers them by that order, and one chapter can be bound
  into several books, a tab for each. A chapter written for one book only stays out of the Technical Reference, whose
  tabs are one a mod. A link to a page another book holds names that book, "(in The Handbook)", and a click opens it
  there; and the manual key over a stack whose page its mod chose, such as an installation disc, opens that page.
- (Core) Manuals: Tables in the manuals with columns of their own, such as a command and what it does. Their words are
  written small, and their commands, codes and figures in Misc Fixed 5x8, a fourth size of the terminal font the Core
  now carries. A word is never cut in two: every column is at least as wide as its longest word. A table whose columns
  cannot stand side by side, such as a list of error codes with what each means and how to fix it, lists each row under
  the one before. A table that fits on a page moves to the next page whole, unless that would leave more than half the
  page empty; one that parts carries its caption, marked "(continued)", and its head on to each page, and leaves at
  least three rows on either side of where it parts. A heading goes on with what it heads, never left alone at the foot
  of a page.
- (Core) Manuals: A mod can have its manuals read their figures from the mod itself as the book is opened, rather than
  print numbers written by hand: a figure in a sentence, written the reader's way, and the rows of a whole table, such
  as every part of a family. A part another mod adds, or a setting changed, is in the next book opened.
- (Core) Manuals: The manuals have Back and Forward beside the contents on their top edge: Back returns to where the
  reader followed a link, a tab, the contents or a line of the search from, and so do the mouse's back button and
  Backspace. A book opens again at the page it was closed at after the game is closed and opened. The search finds
  words anywhere in the manual, a command in a table or a word in a paragraph, and shows the line it found them on.
  Every button and arrow lights and is framed under the pointer, sinks while held and fades when it leads nowhere; the
  pointer turns into a hand over what takes a click; the page-turn arrows are bigger; and a book can hold thirteen
  chapter tabs, the shown chapter's standing out.
- (Core) Manuals: The manuals sound like the game's books: a page is heard turning, the book opening and being laid
  down, and the buttons and tabs click as the game's buttons do. A Manuals volume on the Sound Mixer turns them down
  apart from everything else.
- (Core) Manuals: Listings of code in the manuals: numbered, headed by their file's name, their lines numbered and
  coloured as the game's editors colour Sigma, Sigma# and the network's query language, a long line going on under
  itself without a word cut in two, at a space, a bracket, a comma or the point of a name such as Console.PrintLine. A
  Copy button puts the whole listing on the clipboard for the game's editors, and a click opens it over the whole book
  with no line broken.
- (Core) Manuals: Sessions at a terminal in the manuals, printed as each system draws its console: MC-DOS white on
  black, a Linux console with its green prompt, the Arch Linux media's red root prompt, FreeBSD's sh on its warm dark
  ground; the prompt, what was typed and what the system answered each in its colour. Keys named in a sentence, such as
  Ctrl+S, are drawn as keys.
- (Core) Manuals: A glossary at the back of every manual, before its index: each word a page explains, alphabetical
  under its letter, with what it means and the page that explains it. The first time another page uses one of those
  words, the word leads to where it is explained, unless it is part of a name (the Network Manager) or of a path or a
  name in code. A title too long for its line in the contents, on a chapter's opening pages or in the index runs on
  under itself, its page beside its last words, rather than being cut short; a name too long for any line, such as a
  setting's, breaks where its points and underscores are; and a link to a page of another book reads that page's
  title.
- (Core) Manuals: Scenes in the manuals: a few blocks drawn on the page as the game draws them, block entities and
  GeckoLib models included, which the reader turns by dragging and brings nearer with the wheel, with Reset to go back.
  Numbered balloons point at their parts, a legend under the scene says what each is, and pointing at a number outlines
  its part and names it. A multiblock can be shown layer by layer to see inside it. A scene is framed round its blocks
  wherever they stand, is drawn shorter when a page cannot hold it with its caption, draws its machines as a client
  sees them (lamps lit, parts in their seats), and can show a block of another mod, which stays out of the scene when
  that mod is not installed. A cable's plugs are chosen from the world the cable is drawn in, so a cable in a scene
  ends in the plug of the device beside it there.
- (Core) Manuals: Manuals bound as the books of their time were, not only in binders: a paperback's glued spine, a
  hardback sewn in sections, or a wire coil through holes down the middle; boards covered in cloth or flecked kraft card
  round the open pages; plain or squared paper with a red margin line; chapters found by a thumb index printed down the
  pages' edge or by coloured paper flags stuck under it, besides a binder's tabs, in each book's own colours; and a
  bookmark ribbon that lies across the pages the reader left open and hangs out under the book. A book's cover is drawn
  from the parts its style lists: panels, frames, pictures, its title and lines in any size, a rubber stamp, cloth,
  kraft card, a wire coil and the holes punched for it.
- (Core) Settings: One way to write a settings file, for any mod built on J's Core. A mod declares a file once (its
  name, whether it is a player's, common to every game or a world's, its format and the version of its layout) and each
  setting in it: a flag, a number held to a range, a word held to a list, or anything with a codec, with the comment a
  person reads above it. Files are written in TOML, JSON, JSON5, YAML or NBT, with their comments, ranges and defaults
  wherever the format has room for comments. A TOML file is NeoForge's own, so NeoForge keeps it in step with the
  players and reads it again when it is edited; the others the Core reads and writes whole. Every file carries the
  version of its layout: an older one is upgraded by the steps its mod declares before it is read, one from a newer mod
  is read and never written over, and one that cannot be read is kept aside as `.unreadable` while the defaults are
  used. YAML is read through SnakeYAML 2.7, which J's Core now carries inside its jar, with its safe reader only, so a
  file can never name a class for the game to build.
- (Core) Settings: The settings screen of the series' mods, opened from the mods list for J's Core and J's Computers,
  in its own look: a tab for the world's settings, one for the player's and one for every game's, each with a line
  saying where they are kept; each section with its count of settings, a mark while one of them is changed and not
  saved, and a line saying what it is for; a card for each setting with its whole description (the rest where the
  pointer rests on it), its default, its bounds and its unit, and a switch, a number to type or step, a word to go
  through or a text to type; a Default button on a setting that is off its default; a search through every section by
  name and description; and how many changes wait, beside Save and Cancel. A world's settings are changed from inside
  it, and opened from outside it are shown dimmed under a line saying why. Every setting and section has a name of its
  own, short enough to be read whole, with its comment as the tooltip; a mod built on the Core names each setting and
  section where it declares it, and the names go into the mod's English language file with everything else.
- (Core) Saves: Saves that carry the version of their layout, for any mod built on J's Core. A mod declares once, for
  each kind of thing it saves (a block entity's tag, an attachment, a value saved whole), the version of its layout and
  the steps that bring an older save up to it, and every save then carries the version it was written in. A save from
  before there were versions is read as the version before the first; one written by a newer version of the mod is
  read as far as it can be, and the log says so. Settings files count their versions the same way.
- (Core) Saves: States, for any mod built on J's Core: a value a mod keeps with a world for the whole server, for each
  dimension, for each player (read and changed whether they are online or not) or for each team. A state is declared
  once, with its codec, its default and the version of its layout, and saved with the world in a file of its own. A
  synced state sends each player the value that is theirs to see: the server's to everyone, a dimension's to whoever is
  in it, a player's to that player alone, a team's to the players on it. Teams are the game's scoreboard teams unless a
  mod hands the Core a source of teams of its own. A state's file saved by a newer version of its mod is read as far as
  it can be, and the file as that version left it is kept beside it once, as `<name>.newer-v<N>.dat`.
- (Core) Blocks: Blocks that know their neighbours: a block declares once, as ports, which lines it takes on which of
  its faces (its back, its front, every face), named from its own point of view so they turn with it, and in which
  generations, a port taking its own and every earlier one. A cable that shows a connection and the device that joins
  through it ask the same ports, so they never disagree. A block entity hears which of its faces saw its neighbour
  change, and a block's faces can tell which of the eight blocks around them in their plane they join, for textures
  that run across many blocks.
- (Core) Blocks: Multipart blocks: thin parts mounted on a block's six faces, one a face, whose kinds any mod registers
  with the Core, and a bundle of up to nine wires through the block's middle beside them, each wire a line in one
  generation and one colour or none. A block keeps its parts and wires as fields of its block entity; they are saved
  whole, and the players who see the block are sent only what draws it. Which part a player points at is worked out
  the same way for every block. J's Computers' buses are now parts of the Core's. A part can have a model for while it
  is at work, its lamps lit and blinking by an animated texture; the block is drawn again only when the part starts or
  stops working.
- (Core) Blocks: Models put together as the game runs: a multipart block draws its parts and the pieces of its wires on
  top of its own model, and a block can have faces that run on into the blocks it joins, drawn from five tiles a
  quarter of a face at a time. Both are drawn into the world's mesh, built again only when a block changes.
- (Core) Blocks: J's Core gives a block entity its state as fields declared once, each saying where it goes: into the
  save, to the players who see the block, to the menu open on it. The saving, the update the players are sent, the
  client's reading of it and the menu's data all follow from those declarations, and the players are sent one update
  per block entity a tick however many fields changed. Inventories, energy stores and fluid tanks are declared the same
  way, with whether pipes and cables reach them and whether they spill when the block is broken; so are a value of any
  kind a codec writes, a part that writes itself, a property of the block's state kept at what a value says, a
  peripheral's link to the computer at the other end of its cable, and what happens when the block is broken.
- (Core) Blocks: A block declared with `Drops.SELF_WITH_CONTENTS` drops its item carrying what its block entity keeps,
  as a shulker box keeps its items, and is placed again as it was.
- (Core) Menus: J's Core declares a menu once: its slots in groups, placed from the screen's layout; where a
  shift-click in each group sends the stack; its buttons; and that it stays open while its block entity stands and the
  player can reach it, which holds for every variant of a block that makes the same block entity; the values it shows
  that the server works out; and the screen that draws it, which frames its slots and presses its buttons. A block that
  makes a block entity is declared the same way, with what it ticks and which menu it opens, and faces whoever placed
  it. A menu's player inventory can be shown on some pages of a screen and hidden on the others.
- (Core) Menus: J's Core registers every payload a client sends with a gate that checks, on the server, that the player
  really has that screen open on what the payload names; a payload that acts on the menu the player has open is handed
  that menu, already checked, and a refused one is dropped and noted in the server log.
- (Core) Menus: The menus of J's Core write an item's keys at the right of its row. A tab strip can have tabs that
  cannot be chosen, drawn dim with a word after them, and tabs that do not fit their strip give up some of their
  padding instead of running off its end.
- (Core) Grids: One grid of each kind (power, fluid, heat, gas, motion, data) in each dimension, shared by every line
  of that kind. Cables of one line join when they are of the same generation and their colours agree (the same colour,
  or either in none); devices such as routers join every line. The slowest cable on the best way between two places,
  and the length of each run of one cable against how far it reaches, are worked out when the grid changes and kept
  until it changes again, never on every tick. Only the data grid carries the identity of a network; the data
  network's index is now the data grid.
- (Core) Cables: The shared cable block: every mod's cables are laid in one block, each line in a lane of its own on a
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
- (Core) Cables: Cables that keep a shape: a cable can be declared to run only straight or to join at most so many faces
  in a block, and it keeps the joins it already has when another cable is laid against it.
- (Core) Network: The data network knows the slowest cable between any two points of it, for any mod or addon to ask:
  of every way between them, over cables and routers and through the Mainframes and racks that join cable runs, it takes
  the one whose slowest cable is fastest, which is as fast as data can go between them. A router slows nothing down,
  and a Mainframe that is switched off passes nothing through. It also tells what lies cut off beyond a run too long,
  how long each run is, and which cables a device joins.
- (Core) Peripherals: Port kinds for peripherals: an owner has so many ports of each kind and a peripheral takes one of
  its own; an owner that loses ports unlinks the peripherals it linked last. A peripheral can offer its owner more
  ports, as a hub does, and the link validator finds each peripheral's way through the hubs linked to its owner,
  keeping with every link the hub it hangs from. A cable can end in a different plug for each kind of port, and the
  peripheral links follow the peripheral wires of the shared cable block, each run as far as its cable reaches.
- (Core) Energy: Energy units: FE is registered as the unit `jscore:fe` in a new synced registry of energy units, where
  a mod adds its own with what it is worth in FE: an exact ratio of two whole numbers, rounding down so no energy is
  made out of a rounding. An item can hold energy in the `jscore:energy` component, given the game's energy capability
  with `CoreEnergy.holds`.
- (Core) Energy: The energy grid moves energy. An energy wire plugs into every block beside it that offers the game's
  energy on that face, and each tick generators feed consumers, what they have left fills storage, and storage feeds
  what consumers still want, each share in proportion to what is wanted. A cable can lose thousandths of what crosses
  it, and energy takes the way that loses least, worked out once while the grid keeps its shape.
- (Core) Fluids: Fluids declared once: a mod declares a fluid with its name, its textures and tint, how hot, heavy and
  thick it is and how bright it glows, and the Core registers its type, its still and flowing fluid and, for a liquid,
  the block it pours as and its bucket, drawn with the fluid inside; the generator writes their names, the bucket's
  model and the fluid's tags. A gas is lighter than air, never poured into the world nor held in a bucket, only kept in
  tanks; it is tagged a gas for the Core and for other mods. A fluid can be marked corrosive.
- (Core) Fluids: Pipes: a cable of the fluid grid plugs into every block beside it that holds fluids. Pipes are
  passive: a connected run moves, each tick, at most what its slowest pipe carries, which is the pressure it holds, and
  only the fluids every pipe of it is made for (the temperatures it stands, and whether it takes gases and corrosive
  fluids), leaving the rest where they are. An output feeds the inputs, what it has left fills the tanks, and the tanks
  feed what the inputs still want, each share in proportion to what is wanted, nothing lost on the way.
- (Core) Items: Items with state. An item says where it is declared what it holds besides itself: modes it switches
  between, energy, one fluid, stacks, and components it starts with (a mod declares its own components with its
  content). The Core gives it the game's capability for each, keeps each in a component that is saved and sent with the
  item, makes it stack alone when it keeps anything inside, and lists what it holds in its tooltip. An item that holds
  stacks takes no other item that does. The new Change Item Mode key moves the item in the main hand on to its next
  mode, or back one with shift held, and says the new mode on the action bar; it has no key until the player gives it
  one.
- (Core) Inventories: Item filters that let pass only what they list or everything but it, each rule naming an item
  exactly (a worn tool is not a new one), an item whatever its damage and components, or a tag, and carrying an amount a
  bus can read; what each face of a block lets through (closed, in, out, or both), named from the block's own point of
  view so it turns with the block; and moves of items, fluid and energy from one store to another that never lose
  anything on the way, into an item's own inventory as well.
- (Core) Inventories: Batches of moves that go through together or not at all: each step is asked first whether it
  would move all it wants and nothing moves when any would come short; when a store takes less than it promised as the
  batch moves, everything already moved is put back, last first. Steps come ready for putting items into and taking
  them out of an item store (put back slot by slot), filling and draining a fluid store, and giving and taking energy.
- (Core) Data: Content from datapacks. A mod declares a registry of JSON files, every file under
  `data/<namespace>/<folder>/` one value read with its codec, read again whenever the server reloads its data; a file
  that does not read is left out with a line in the log saying why. A synced one is sent to every player as they join
  and after every reload, and their game keeps it apart from the server's. A mod can also declare a registry datapacks
  fill, sent to every player with the world's registries.
- (Core) Data: Large values sent in pieces: a mod declares a kind of value with its codec, and the Core writes it, packs
  it, sends it in pieces either way (the server a share of each player's pieces every tick, so a large sending never
  holds the others up) and puts it back together on the other side. A side keeps only so many sendings from one sender
  open at once and refuses pieces out of order, sendings larger than allowed and values that unpack into more than
  allowed. The sound system's recordings are cut and checked by the same pieces.
- (Core) Ticking: A tick scheduler: a block that runs many things in a tick shares the tick's work among them in turns
  of a fixed size, gives what one leaves to the others, and stops at a deadline by the clock, the next tick starting
  with the first that went without. J's Computers' machines run their programs with it, as they did with their own.
- (Core) Keys: Key actions: a mod declares a key once, with its name, the key it starts on and what the server does
  when it is pressed; the player's game makes the key binding from it and sends the press to the server by itself. Turn
  Off Last Sound and Change Item Mode are key actions, so a key a player gave them stays given.
- (Core) Debug screen: The series on the debug screen (F3): each part of the series adds a panel of lines under its
  name, the sound system's first, and the pieces of work the series times are listed with their average and longest
  time over the last hundred runs, the energy grid's tick among them.
- (Core) Calendar: The world's calendar: the day, the hour and the minute, the day of the week and the week, the season,
  the day of the season and the year, all read from the world's own clock. A year is four seasons (spring, summer,
  autumn, winter) of the same number of days, 28 unless the series' balance file says otherwise. J's Computers' cron
  and `at` jobs and its desktop clock read the time through it.
- (Core) Models: Machines drawn with GeckoLib: a mod says once how a family of them looks (which model each is drawn
  with, by era or by kind, the folder of their textures and the animation they share) and declares it with the blocks,
  and one model of the Core draws every family, where each used to need a model class of its own. The Core runs
  without GeckoLib; a mod that draws with it brings it. J's Computers' Mainframes, racks, Pattern Encoders and drives,
  and the items that show them in a slot, are drawn this way.
- (Core) Screens: J's Core can show a picture in the world drawn from anything a screen can draw, into a texture of its
  own, as often as the viewer's distance calls for, and paint it, or the player's own screen, as a monitor's tube would;
  and it sends what a thing in the world shows only to the players near enough to see it, only when it changes.
- (Core) Fonts: Fonts of their own for every mod built on J's Core. A mod declares a font once, with its source, its
  cell, its baseline, its licence and who made it; the data generation reads the source, a BDF bitmap font (the first
  format read), and writes the picture and the font file the game draws from, with the game's font for every character
  the font lacks. J's Core carries Misc Fixed, the fixed font of the old Unix terminals, in the public domain, in three
  sizes: 6x10 with nearly sixteen hundred characters, and 9x15 and 10x20 with several thousand, for any mod's
  terminal-like views; the mods list credits the font on J's Core's page and on J's Computers'.
- (Core) Fonts: A grid painter puts text on a monospace grid, every character in a cell of its own, in a declared font
  or the game's: characters the font lacks are drawn in the game's font in the middle of their cells, and the box lines
  and blocks are drawn by the painter to fill their cells, so frames and bars join from cell to cell and from row to row
  at any spacing. `TextScreen` is a text-mode screen of cells, each a character with an ink and a ground, in the colour
  adapter's sixteen colours or any other, with boxes in line characters and the shadow a dialog of that age threw.
- (Core) Motion: Motion, for any mod built on J's Core: curves written as CSS and the desktops' own toolkits write them
  (`ease-out-expo`, `cubic-bezier(0, 0, 0, 1)`, `steps(16)`), a motion of each kind for each system (growing, sliding,
  going down to a place and back, an outline travelling there, a flat colour giving way), read against one clock that
  runs smooth between ticks. A mod declares each system's profile once; the data generation writes it to
  `assets/<mod>/motions/<system>.json`, where a resource pack puts its own to slow a system down, speed it up or keep it
  still.
- (Core) Motion: For any mod's screens: a thing can fade in or out as one picture (`FadeLayer`), a region of the screen
  can lose its colour part of the way or all of it (`GreyFilter`), the motions that go round and round (a cursor
  blinking, a throbber turning, a bar with no known end) are read by `Rhythm`, a screen can draw everything inside it
  within one system's motions (`MotionScope`), a `WaitBar` runs the bar of whichever system it is drawn on, a
  `ProgressBar` glides on a system whose bars do, and a skin can give its progress bars a colour of their own. New
  styles of motion (appear, caption, blink, loop, blocks, segment, grow, bounce, ease, grey) and new kinds (tooltips,
  notices, the dim behind a dialog, the text cursor, a progress bar's fill and wait, the busy sign, the pointer's busy,
  working and launch states, and a copy's animation).
- (Core) Sound: A sound system for the whole series and its addons. A mod declares a sound once (where it is heard from,
  whether it loops, its channel, its files, how far it carries, its subtitle) and its registration, `sounds.json` entry
  and translatable subtitle follow from that one line. Sounds are mixed in channels (machines, devices, interface,
  alerts, ambience, music, voice) with a volume each, and any sound of the game, not only the series', can be turned off
  in `config/jstech-audio.json`. The same sound from the same place plays once in a short while, however many times it
  is asked for.
- (Core) Sound: The sound system keeps the world's running sounds: a machine says what it wants heard (a fan, a disk) at
  the volume and pitch its state calls for, and the client starts, retunes, fades and stops the sounds to match. It
  keeps to a share of the game's sound channels, the sounds that matter most and are nearest first; many machines of
  one kind close together are heard as one room, louder the more there are; and walls between a sound and the listener
  muffle it, which the player may turn off.
- (Core) Sound: The sound system plays sounds made as they play: a synthesiser (square, pulse, triangle, sawtooth, sine
  and noise, note by note, the way a PC speaker or an early sound card makes a tune), with two voices more, the bright,
  metallic ring of frequency modulation and the rounder note played from a table of harmonics, which a sound device
  chooses between; and recordings read from files that are not the mods' own, WAV and Ogg Vorbis to begin with and any
  other kind a mod registers a decoder for. The server sends a tune as its notes, heard by the players near where it
  plays, and each client makes the sound itself; a tune can be stopped before its end. Such a sound has a subtitle and
  a channel like any other and the player can turn it off the same way; heard from a place in the world, a stereo
  recording is played in mono so it comes from that place.
- (Core) Sound: A sound can come out of several places at once, each playing one side of a stereo recording or both,
  and each as well as the speaker there reproduces it: no faster a sample rate and no more bits than it manages, with
  its bass and treble cut where it cuts them. A mod marks a recording as stereo, and a sound device's own limits (a card
  that plays in mono at 8 bits) apply on top of the speaker's.
- (Core) Sound: A "Turn Off Last Sound" key in the game's controls, under J's Tech Series, with no key until the player
  picks one: it turns off the last sound heard around the player (never their own footsteps or a click of the screen)
  and says which on the action bar; pressed again within five seconds, it brings that sound back.
- (Core) Sound: While an alert plays, the series' other sound channels are lowered so the alert is heard over them, and
  come back up over a second once it ends; the player may turn this off in `config/jstech-audio.json`. A player who
  turns on alerts on screen sees each alert as a sign at the top of the screen, saying what it is and pointing to the
  side it comes from the way the game's subtitles do; it blinks three times as it comes up and goes after four
  seconds.
- (Core) Sound: Sound cues: a mod says what happened (a computer powering on) and the sound heard is picked on each
  client from `assets/<namespace>/sound_cues/`, by the context it happened in (the machine's era, its audio device, its
  system's family). A resource pack binds a cue to other sounds, the game's own included, by shipping a file of the
  same name. A machine with sound hardware of its own plays cues and tunes from each of its speakers, at its own volume,
  as its device allows: a PC speaker turns every note into a square wave, and a machine with no sound hardware stays
  silent.
- (Core) Sound: The Sound Mixer, reached from Sound Mixer... beside Done in the game's Music & Sound Options. Its
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
- (Core) Sound: Recordings a server keeps, for any mod or addon to play. A player can bring a WAV or Ogg Vorbis file
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
- (Core) Machines: J's Core shows every processing machine's recipes in JEI and in EMI, under the machine's name with
  the machine beside them, items and fluids, the time and the energy a tick; a computer's monitor keeps both viewers'
  lists beside it in either. Jade shows what a J's Core machine is doing ("Working: 40%", "Idle") and whose it is.
- (Core) World: Dimensions: a mod declares one and the world has it; copies of it are made and taken away while the game
  runs (`/jstech dimension create` and `remove`) and come back each time the server starts. A dimension declared only to
  be copied adds nothing to the world itself, which the game then opens without its warning about experimental
  settings. Each dimension has rules, read from a datapack: how hard it pulls, whether its air can be breathed, how hot
  or cold it is, its weather and its pressure. A player choking, burning or freezing there is spared by whatever another
  mod makes for it.
- (Core) World: Commands under `/jstech`: `progress` to see or set where players stand along the hardware eras and the
  industrial tiers, and `chunks` to see and let go of the chunks their machines keep loaded. Machines can keep chunks
  loaded for their owners, up to the "Chunks loaded per owner" setting.
- (Core) World: Holograms, words floating in the world turned towards whoever reads them, and HUD elements that share
  the corners of the screen, for the series' mods to show things with.
- (Core) Modpacks: A mod built on J's Core can hand a scripting mod's scripts a class of its own under a name, declared
  once with the rest of its content; J's Core's plugin gives KubeJS every one.
- (Core) Documentation: J's Core's documentation, in its `docs` folder, explains each part of the library to
  programmers, from adding it to a project to machines, multiblocks, networks, Operations, energy and fluids, cables,
  the world and dimensions, saved state, ownership, progression, commands, entities, overlays, settings, sound, screens
  and testing, with what can go wrong at the end of every page; the pages on fonts, motion and UI components moved
  there. The API's own documentation in the code explains, with examples, what a network category, an Operation and its
  handler, a language, an instruction set and a kernel are. Its README lists what the library holds and says in plain
  words that anyone may use the Core in their own project, open or closed, free or paid, with credit, and what its
  licence, the LGPL 3.0, asks in return.
- (Core) Design: The design of J's Core, in `core/docs/design`: what the library is (the platform any technology mod
  needs, the model the series' mods share, and the content several of them need as one), and one page per subject, from
  the platform, progression, the API, materials, states of matter and energy to cables, redstone, multipart, conveyors,
  machines, mechanical power, heat, multiblocks, networks, the world, hazards, explosions and fire, sealed rooms, moving
  structures, entities, tools, the interface, models, lighting and effects, manuals, modpacks, the tools for those who
  make mods and the low level; each page says what is built, links to the programmers' documentation, and ends with
  what is designed and still to be built, and the first page gives the order of building. J's Computers' design follows
  it: its programming languages move to its own API, and its numbers to its own settings file.

### Changed
- (Computers) Operating systems: Every edition of Frames goes down when a program it has no memory left for is opened,
  a window, a program started at its prompt or a service, instead of refusing it: Frames 95 at once, and Frames XP, 7,
  10 and 11 after a balloon that warns, once the memory passes nine tenths, naming the programs that hold the most. The
  crash brings the machine itself down: its blue page, in the style of each edition's real one, is on the monitor for
  everyone looking at it, in the world too, and nothing typed or clicked at it reaches anything. The programs the
  machine was running die with it, its session ends, and once the page has stood its time the machine starts over from
  its self-test into an empty session. Frames 95's crash was a picture on the player's own desktop only: the keyboard
  still reached the program behind it, and a player who left the monitor during it came back to every window open
  before the crash. Linux, FreeBSD and UNIX have their own crash pages too, their kernels' panics on the console.
- (Computers) Operating systems: Linux, FreeBSD and UNIX no longer refuse a program they have no memory left for: as the
  real kernels do, they let it start and then end what holds the most, a window, a program or a service, until
  everything fits. Each one is logged in the kernel's own words (`Out of memory: Killed process 1234 (calc)` on Linux,
  `pid 1234 (calc), jid 0, uid 0, was killed: out of swap space` on FreeBSD) and printed on the console, and the new
  `dmesg` lists those lines after what the kernel found as it came up. A desktop closes the window that went and says
  why in a balloon; a program ended in front of a terminal leaves the shell's `Killed` and gives the prompt back; a
  service ended stays down until the machine starts over, a Mainframe's network engine, Automation Engine and Mirror
  included. Only when nothing is left to end does the kernel panic, on its own page, and the machine starts over: a
  machine whose system and desktop alone hold more than it has panics as soon as it is up.
- (Computers) Operating systems: What a program holds in memory grows with the era of the machine it runs on, as
  software grew with the machines it was written for: each program holds what it declares on a Legacy machine, a
  sixteenth of that on a Vintage one, eight times on a Transition, thirty-two on a Standard and a hundred and
  twenty-eight on an Advanced, the tools a system ships included (16 MB on a Legacy machine). Each edition of Frames
  holds more of itself, Frames 95 32 MB, XP 128 MB, 7 1 GB, 10 3 GB and 11 8 GB, so the typical machine of each era,
  two sticks of its middle size, holds its system and about eight programs, and filling the board pays. The Linux
  distributions, FreeBSD and UNIX stay light, and a Linux desktop holds a little more than one program: KDE Plasma 80 MB
  on a Legacy machine, GNOME 72, Cinnamon 56 and CDE 8.
- (Computers) Operating systems: Frames 11 installs from the Advanced era on, and the Standard era has Frames 10. The
  family now runs Frames 95, XP, 7, 10 and 11, and a program asking for a newer Frames reads it in that order. "More
  like, Bloat 11" follows "What Happened to Frames 9?" in the Operating Systems tab, as "Never Upgrading" follows "The
  goat".
- (Computers) Operating systems: Frames 11 calls its file manager File Explorer, as Frames 10 does, rather than Files.
- (Computers) Operating systems: FreeBSD's prompt is root's, as a machine fresh from its installer stands at:
  `root@host:~ #`, with `root@host` in red and the rest in the prompt's own colour.
- (Computers) Operating systems: `apt upgrade`, `dnf update` and `upgrade`, `pacman -Syu`, `emerge --update @world` and
  `pkg upgrade` bring the installed packages up to the versions the Mirror serves, as `pckmgr upgrade` does; `apt
  update`, `pacman -Sy`, `emerge --sync` and `pkg update` still only read the lists.
- (Computers) Operating systems: A manual page's paragraphs wrap to the width of whatever shows them, at a terminal,
  under `--help` and `/?`, and in the Help Viewer, with the lines they wrap onto set in under the first, instead of
  breaking at fixed places. The Help Viewer files every command under what the command says it is for, so each command
  sits under the same heading wherever it is listed.
- (Computers) Terminals: The terminals write in the game's font, one character to a cell, and a monitor's console draws
  it at the machine's display scale, the same scale its desktop is drawn at.
- (Computers) Terminals: A desktop's terminal window opens eighty columns by twenty-four rows, as terminals do, and
  smaller only when the desktop has no room for that. Its rows, and those of the terminal inside an editor, are as far
  apart as the full-screen terminal's, so what is drawn in characters keeps its shape instead of being squashed flat.
- (Computers) Desktops: Save in the Editor, or Ctrl+S, on a page never saved opens Save As to ask where and under what
  name, rather than writing untitled.txt without a word.
- (Computers) Desktops: The explorer's rows, the file dialog's and those of the file trees in Virtual Studio, Virtual
  Studio Code and Exposure are tall enough for a 16-pixel icon, as a list of small icons on a real desktop is. The file
  dialog opens taller to make room for them.
- (Computers) Desktops: What moves while a machine comes up and goes down (Frames 95's running bar, Frames XP's blocks,
  the rings and running dots of Frames 11, GNOME and Cinnamon) moves smoothly between ticks, on the clock every motion
  is read against, and stands still in its first position for a player who reduced motion, Frames XP's blocks standing
  at the left end of their trough rather than outside it.
- (Computers) Desktops: A desktop's windows are known by the program they belong to rather than by the name they show,
  so a machine's layout comes back the same whatever desktop or language it is looked at under, and the Task Manager
  and the System Monitor list each window by the name the desktop gives its program.
- (Computers) Desktops: The desktop's code is split into parts by what each one does, apart from the screen that shows
  them: where it sits and how big it draws, how its owner chose it should look, what it tells the player, the power
  dialog, the machine's memory as it weighs it, its menus, the inventory laid over a window, what it can start, its
  launcher and panel, the window manager, the layout the machine remembers, how it opens a program or a file, what the
  pointer drags, where a click, a key or the wheel goes, and how it is painted. The desktop draws onto any surface that
  gives it a size, so it can be drawn with no screen open.
- (Computers) Σ and Σ#: A box typed into tells its program's handler at most once a tick, holding every letter typed by
  then, so a player typing fast no longer fills the program's queue with a call a key. A window carries at most 128 KB
  to the screens showing it, whatever its program holds; past that, what its last widgets hold is left out and they say
  so.
- (Computers) Σ and Σ#: A widget asking for a width or a height gets it down to 8 pixels; anything under 60 used to be
  raised to 60, the least a window may be, so a short widget pushed what came after it out of its window.
- (Computers) Σ and Σ#: try, catch, finally, throw, goto and const, and comefrom, abstainfrom and reinstate after
  INTERCAL, are kept for a later version of Σ and Σ#: written as a statement or as a name, each is refused at the word
  with what to write until then (S2014), where it failed with a message about something else.
- (Computers) Σ and Σ#: The words of access hold in Σ and Σ# as in C#: a private member, which is what a member with
  no word is, is reached only from its own type and the types nested in it, a protected one also from the types built
  on it, and reaching one from anywhere else is the error S3060. They were read and ignored. internal is taken as public
  is. The Book of Σ and the description of Σ# say so, and how shifts work.
- (Computers) Σ and Σ#: The instruction set of the Vintage processors is called IA-16 (it was x86-16): a project that
  names `jsc:x86_16` names `jsc:ia_16`. What a processor understands (IA-16, x86, x86-64) is its instruction set: the
  firmware lists it as the Instruction Set, a build of mixed processors is refused for mixing instruction sets, and
  `sgsc --arch` asks for an instruction set by the same ids and names as before. The word architecture is left for the
  design of a chip.
- (Computers) Hardware: How much a processor orchestrates counts the design of its cores as well as how many there are
  and how fast they run: a newer design does more in each tick of its clock, and two threads a core add a fifth. An old
  chip at a high clock no longer outruns a better one that came after it. Vintage machines orchestrate less than before
  (a 486DX2 one item a tick, a K6-II 11) and Standard ones up to three times more (a 4790K 2,074), and no processor does
  less than one item a tick. A Crafting Computer that does less than one run a tick keeps what a tick's work leaves over
  for the next, so a run takes as many ticks as it costs.
- (Computers) Hardware: A server's RAID controller and read cache are expansion cards now, not gadgets in the rack's
  front bays: Vaultis makes an Array and a Cache for each era's slot, from ISA to PCI-e 4.0. The Array joins the drives
  of the server it sits in and keeps the array's mode on itself, so a server pulled from its rack takes its array with
  it; the Cache cuts the server's reads by 10% on the ISA card, 15% on the PCI one, 20%, 25% and 30% on the PCI-e 2.0,
  3.0 and 4.0 ones. In any other computer a storage card sits in its slot and does nothing. The front bays that took
  gadgets take drives: a 1U server has 1 drive bay in the Vintage, 3 in the Legacy, 5 in the Transition, 4 in the
  Standard and 5 in the Advanced; the Storage Server 10, the Compute Server 2, the Supercomputer Node 2 and the Advanced
  one 3.
- (Computers) Hardware: The Legacy 1U server takes two expansion cards rather than one: its board has no video of its
  own, so one slot goes to the graphics card a monitor needs and the other is free for a storage card.
- (Computers) Hardware: A disk goes in a computer, and in a Server Rack's drive bay, of its era or a newer one: an NVMe
  made for the Standard age no longer fits a Vintage computer, while an old disk still goes in a newer machine. A disk
  already in a computer stays.
- (Computers) Hardware: A part's tooltip names its slot and its memory as the parts' own sheets do: PCI-e 3.0, AGP 8x,
  DDR3. A medium's tooltip names its format in the player's language ("Floppy", "Blu-ray") instead of the code's name
  for it.
- (Computers) Hardware: A compute card is counted at the clock it is rated at, the one a card with no fan of its own
  holds in a server: the Tessera K40 does as much as the Vertex GTX 780 Ti on the same chip. The Integra Phi 9000 has a
  new icon.
- (Computers) Hardware: The Vintage server boards hold the Integra Pentix Pro, as the boards of the time did: the MF
  MTX-V of the Mainframe takes four on Socket 8 and the MF EEB-V two. The Socket 7 chips stay on the desktop boards.
- (Computers) Hardware: Vintage names, fixed to their years: the MF AT Standard Motherboard is the MF AT Classic, the MF
  PowerBasic 300B is the MF PowerBasic 300 (its id follows, `jsc:psu_300`; the Bronze seal came a decade later), and
  the graphics cards carry their makers of the time: the Artisan 3D Blaster, the Envya Prism 4 and the Tridex Voodoo
  GFX, while the VGA-256 is IBM's own and carries none. Every Vintage part has a new icon drawn from the real one.
- (Computers) Hardware: Legacy parts that never existed as named give way to real ones, and their ids follow: the
  Velocion Sprint XP 3800+ is the Sprint XP 2800+ (`jsc:cpu_velocion_sprint_xp_2800`), the dual-core Velocion Dual
  240, 280 and 285 are the single-core Velocion Optera 244, 248 and 250 of their years, and the two-way LGA 775 server
  board is the MF EATX Legacy Motherboard (2x Socket 604). The Integra Vertex processors are the Integra Pentix, the
  name of their line (`jsc:cpu_integra_pentix_700` and on), which leaves Vertex to the graphics cards.
- (Computers) Hardware: Legacy boards take the memory and the bus their chipsets had: the Socket 370 board SDRAM on AGP
  4x, the Socket A board SDRAM and DDR, and the Socket 940 server and Mainframe boards DDR. The Radiance 9200 SE is an
  AGP 8x card with four pipelines and 128 MB, and the Legacy graphics cards carry the makers of their time, Envya and
  Atrion. Every Legacy part has a new icon drawn from the real one.
- (Computers) Hardware: Parts move to the era of their years. From the Legacy to the Transition: the Integra Duo
  processors, now the Integra Centro 2 Duo (`jsc:cpu_integra_centro_2_duo_e4300` and on), the Servo 5100 and 5160, the
  Servo 5365, now the Servo 5335 it really was, the DDR2-2048, the Vertex 8800 GT and GTX 280 on PCIe 2.0 (the GTX 280
  draws 236 W) and the SATA-SSD 64G, which holds 256 items at the Transition's weight. From the Standard to the
  Transition: the Velocion Ascent X4 955, X4 965 and X6 1090T (`jsc:cpu_velocion_ascent_x4_965` and on), the AM3 board,
  now the MF ATX Transition Motherboard (AM3) on PCIe 2.0, the Radiance HD 6850, a Velocion card of 1 GB, the hard
  disks up to 2T, and the DVD. The disks read as their catalogue writes them, 500G to 8T, and their ids follow
  (`jsc:disk_hdd_500g` and on). A Personal Computer from the Transition on takes the workstation's EATX board as well as
  an ATX one.
- (Computers) Hardware: Standard names follow the catalogue, and their ids follow them: the Integra Apex processors are
  the Integra Centro c5 and c7 (`jsc:cpu_integra_centro_c7_4790k` and on); the Servos are `jsc:cpu_integra_servo_2620`
  and `jsc:cpu_integra_servo_2690`, and the Servo 2699, a chip of the next socket and of DDR4, gives way to the Servo
  2697 v2 (`jsc:cpu_integra_servo_2697_v2`); the Radiance HD 7970 is a Velocion card (`jsc:gpu_radiance_hd_7970`) and
  the Vertex GTX 550 Ti and 780 Ti are Envya's; the memory is the Stratix Layer DDR3-8192; the MTX-P Motherboard and
  EEB-P Server Board are the MF MTX-S Motherboard (4x LGA 2011) (`jsc:motherboard_mtx_s_2011`) and the MF EEB-S Server
  Board (2x LGA 2011) (`jsc:motherboard_eeb_s_2011`); and the MF EATX Standard Workstation Board is the MF EATX
  Standard Motherboard (LGA 2011) (`jsc:motherboard_eatx_standard_2011`). Every Standard part has a new icon drawn from
  the real one.
- (Computers) Hardware: The Standard server boards have the memory slots of their kind: 16 on the two-way board and 48
  on the Mainframe's four-way one, where a machine still counts only as many as its case holds; a rack server reports
  the slots its case has rather than the board's.
- (Computers) Hardware: The Vaultis Swift SSD 8T and the Bolt NVMe 4T and 8T move to the Advanced, the years they sold
  in, with new icons.
- (Computers) Computers: The assembly screens are drawn as the machine they assemble. On the left, the open case of its
  age, the board on its tray once there is one, and every part's seat where that part sits: the processor in its
  socket, the memory beside it, the cards along their slots, each drawn as its own model shows it, with its bracket on
  the back panel, the supply where that age's case keeps it, the bays and the disk cage at the front. An empty seat
  shows the shape of the part it takes. The Mainframe and a rack server carry their cards in two I/O cages. On the
  right, the machine's front panel, its power button over its readouts; beside the inventory, the AUTO START and SIDE
  PANEL switches (FAILOVER on the Mainframe) and the PWR, DISK and NET lamps. A server's panel shows its checks where
  the power button would be, since it is switched on in its rack, with every problem in their tooltip. The Personal
  Computer's servers, the Crafting Computer's throughput and the Cluster Management Computer's RAM buffer and kinds of
  cluster show in their readouts' tooltips. A part held over a slot that keeps it out says which rule does, on the
  assembly screens and on a rack's rows and bays: the board's socket, a generation of memory, a card's bus, a disk of a
  later era, a socket the board does not have, the cabinet a chassis belongs in.
- (Computers) Computers: A computer finds its removable drives in one place, and everything that asks for them gets
  the same answer in the same order: the drive letters, This PC, the explorers, the firmware's boot menu and
  installer, the Setup of a program, the Cluster Management Computer, the Crafting Manager, the Pattern Studio and the
  live media. With several drives holding installers, each of them now takes the first in the order the drive letters
  go, where some took the first one cabled. A system being copied from a drive stops if that drive is turned off for
  the computer.
- (Computers) Computers: A Standard computer with nothing to boot shows its dialog over the maker's mark and the
  machine's name, and the self-test's bar is gone, where it showed below a short dialog and hid under a tall one.
- (Computers) Computers: J's Computers makes an installer, a blank medium with a system or a program written on it, in
  one place, which the creative tabs share with anything else that hands one out.
- (Computers) Racks and servers: A server rack's screen shows the cabinet from the front: its posts, each mounted unit
  as its faceplate with its own slot, its drive caddies, vents over what its chassis does not cable and, on a computer,
  its power button with its lamp; an empty unit is a blank panel, and a KVM Switch, a UPS, a cooling unit or a fibre
  interface is a unit too. A drive caddy's lamp is lit while its drive sits in a switched-on unit, and an array being
  rebuilt fills a bar across its unit. A click on a computer's faceplate puts it on the card beside the cabinet: its
  name, its state, the disk it boots from, its system, its network, its array and what a linked monitor shows. A front
  slot still holding a drive after its unit came out shows, so the drive can be taken back, and says why it takes
  nothing more.
- (Computers) Racks and servers: A server in a rack follows the rule of its system when it runs short, as every
  computer does, each server on its own: a Frames server goes down on its blue page and starts over, the kernel of a
  Linux, FreeBSD or UNIX server ends what holds the most and logs it for `dmesg`, and MC-NET refuses as it always did.
  At a server's prompt `ps` lists its init and its services, `kill -9 1` panics it, and a signal to a service stops it
  until the server starts over; a service ended on a server, by a signal or for its memory, serves the network no more
  until then. The page is on the monitor showing that server, and a key holds FreeBSD's reboot there as on any
  computer. A server used only to refuse what did not fit, and nothing ended it.
- (Computers) Racks and servers: A Server Rack's roof fans turn as fans: the housing stays still and only the rotor
  turns in it.
- (Computers) Network: The Server Router wears the look of the other network devices: a bank of four ports on every
  face, framed in router blue, and the routers' two blinking lamps.
- (Computers) Network: J's Computers' four data cables (Ethernet, HBW, High Compute and Crafting) are laid in the Core's
  shared cable block, each in the lane of its line: access top left, backbone top middle, compute in the middle and
  crafting middle right. They are items now rather than blocks of their own, drawn four pixels thick in their new
  jackets with the plug of their kind where they meet a device, and they can share a block. A world's old cable blocks
  are gone from it.
- (Computers) Mainframes: The Standard Mainframe's roof fans look like fans: each is a square housing that stays still,
  with a dark well in its opening and a five-bladed rotor turning in it, where before the whole square turned with a
  cross painted on it.
- (Computers) Mainframes: The Vintage Mainframe's tape reels can be seen: the window in front of them is a clear pane,
  where a handle stood over one reel and a painted glint over the rest, and the reels are aluminium reels with three
  windows and the tape showing through them. They run in bursts, as a tape drive reads: forward, a stop, a short rewind,
  a stop, forward again, the take-up reel a little faster.
- (Computers) Peripherals: The Pattern Encoders are the devices of their day, each a full block: the Vintage one a beige
  floppy drive, the Legacy one a grey CD writer with a tray, the Standard one a black writer with a tray and a USB port,
  a USB 3 socket standing on end. The medium in the bay is the very item the player put in, seen where it sits: a
  floppy slides in and out of the slot, a stick goes into the port and comes out of it at once, as on every other
  device with a port. A disc lies on the tray, which the eject button on the front opens and closes as on a real
  burner: the button is outlined when looked at, a disc is laid on the open tray with a click and lifted off it with a
  sneak-click, nothing is written on it until the tray is closed, and the tray stays shut while a pattern is written.
  The panel's Eject is the same button, and its slot takes a disc only while the tray is out. Two lamps on the front say
  what the encoder does: the power lamp is lit while a computer is at the other end of its cable, and the activity lamp
  blinks while a pattern is written and stays lit on an error. The body no longer has a status screen, and the
  encoder's item shows it switched off.
- (Computers) Peripherals: The Floppy, CD and DVD Drives are the drives of their day, each a full block in the colours
  of the Pattern Encoder of its era: the Floppy Drive a cream external drive with its bezel low on the front, the CD
  Drive a pale grey external CD-ROM drive with darker end caps, the tray at the top and the headphone jack, the volume
  slider and the eject button under it, and the DVD Drive a black writer with a diamond-cut top and a silver trim. The
  medium in a drive is the very item the player put in: a floppy slides in and out of the slot, and a disc lies on the
  tray, which the eject button opens and closes. A disc goes on the tray and comes off it only while the tray is out, a
  computer sees the drive empty while it is, and the tray stays shut while the computer reads the disc. Ejecting a disc
  from This PC or the Pattern Studio opens the tray, and the disc waits on it for the player's hand. The power lamp is
  lit while a computer is linked, and the activity lamp blinks while the computer reads the drive.
- (Computers) Peripherals: Every disc tray, on a drive of its own, in a case's bay or on a Pattern Encoder, is moulded:
  the well a disc lies in, the smaller well inside it, the slot to the back the laser reads through and the spindle's
  hub, where it was a plain slab. An open tray is aimed at and outlined on its own, out of the block, and pushed with an
  empty hand it slides back in, as a real one does: from above, the tray stands in front of its button.
- (Computers) Peripherals: The drives and the Pattern Encoders take their cable in the middle of their back, where a
  cable comes up to them, instead of low down, and the port there is their era's: a DB-25 on the Vintage ones, a USB
  port on the Legacy ones, a blue USB 3 port on the Standard ones.
- (Computers) Monitors: A monitor's own sessions (the self-test, the boot menu, the firmware setup, the installers and
  the KVM) take from a player only what the session they have open sends, and only for the monitor it is on; a system's
  settings asking for the firmware setup must be on the monitor they name.
- (Computers) IQL: Every table of the network's language reads every column it has: a WHERE tests any of them, and an
  ORDER BY sorts by any of them.
- (Computers) Autocrafting: A machine recipe no longer names a machine: it lives in the Crafting Interface of the
  machine that makes it, so the Pattern Studio's machine draft has only its inputs, its outputs, their chances and its
  timeout, and loading one puts it in the first interface the Crafting Computer drives with room for it. A bench recipe
  goes into the ROM of one of the computer's Crafting Cards. The Crafting Manager lists the interfaces the computer
  drives, what each holds and what it is doing, and moves a pattern from one to another.
- (Computers) Autocrafting: The crafting catalogue is planned once and kept while the network's patterns and storage
  are as they were, for a second at most, where it planned every recipe again on each refresh.
- (Computers) Peripherals: An unlinked peripheral looks for a computer twice a second rather than every tick; a linked
  one still sees a cut cable at once.
- (Computers) Saves: J's Computers' blocks keep their state as J's Core's declared fields: the computers, the server
  racks, the drives, the Pattern Encoders, monitors, speakers, Network Gateways, data cables, Server Routers, HBW
  Interfaces and tanks. The players who see one are sent one update a tick however much of it changed.
- (Computers) Menus: J's Computers' menus are J's Core's declared menus: the computers', the devices', the buses', the
  terminal's, the Command Prompt's and the monitors' sessions. Each stays open only while its block stands and the
  player can reach it, for every variant of the block, and every screen draws its slots and answers its buttons from
  the one layout its menu places them by. What a client sends from one of them acts on the menu the player has open,
  never on a block the message names that the player is not looking at.
- (Computers) Advancements: J's Computers' advancements are earned through J's Core's event trigger; what a world's
  players earned before stays earned.
- (Computers) Translation: What the machines say is text a language file can translate, the way a real system's
  messages are on a machine set to another language: every command's manual and messages, and what the machine
  answers about Operations, IQL, packages, ports, the Mirror, installs, programs, the Network Gateway, remote computers,
  jobs and clusters; every error and warning of `scc` and `sgsc`, the problems of a listing and of a package, the Error
  List of the studios, the cost of a call shown beside a suggestion, and why a program was stopped; and what the
  installers and the tools of an installation say, from the guided installers' titles, headings, hints and steps,
  Setup's progress and refusals, the boot managers' menus, the Frames editions' start and shutdown screens and the
  welcome tips to what fdisk, pacman, emerge, the ports and the kernel build ask and answer at a live medium's prompt.
  What is typed, names, paths and code, the file, line, column and code of an error, and what real systems print the
  same in every language (kernel and init logs, compiler and build lines, file listings, package names, the contents
  of files) stay as they are; what goes down a pipe or into a file, and what a program reading another program's
  output or a ComputerCraft computer is handed, is English, the machine's own language. The columns of a table, such
  as `cluster list`, stay in line under their headings whatever length the words come out in.
- (Computers) Translation: Every screen is text a language file can translate: the hardware's tooltips, the names of
  the eras, the firmware's setup, the self-test, the boot menus and every page of a guided installer, with the parts a
  firmware lists named in each player's language; the Pattern Studio and the Pattern Encoder; the Network Interactor,
  Storage Insights, the Crafting Manager, the Craft Planner and the Crafting Computer's screen, with a craft's recipe
  choices and what a plan is short of; the Gateway Manager, the Gateway's screen and its log; the assembly screens and
  the reasons a build is not a working computer; a network machine's space, heading by heading, and the Operations
  list, an Operation's status included; the IQL Server Management Studio; the Cluster Manager, the Automation Manager,
  the Messenger and Knot; the code editors and the programs that open files; the explorer, This PC, the trash and Open
  with; the Task Manager, the System Monitor, Settings, the welcome, the Network Manager, Setup and Remote Control;
  CDE's own windows; the panels and start menus; and the desktop's own words, dialog buttons and search field. Items,
  machines and recipe results are named in each player's language, and so are clusters, routers and nodes nobody named
  and the programs a desktop bundles, while Dolphin, Kate and Nemo keep their names; the names a player gave, the query
  language's own words and the Operation keywords stay as they are, and the network's categories keep their English
  names underneath, so a saved filter survives a change of language.
- (Computers, Industrial) Storage: The Tank belongs to J's Industrial now (`jsindustrial:tank`), on its own Storage
  shelf and in its manual. A Tank placed under the old name is gone from a world made before. J's Computers' manual
  explains fluids on a page of their own, with a tank of any mod.
- (Industrial) Machines: J's Industrial's Macerator and Compressor run on J's Core's processing machines, and their
  recipe files take a list of inputs, each with its count, a list of outputs, and the time and the energy a tick.
- (Core, Computers) Integrations: The bridge to Mekanism's chemicals lives in J's Core now, still behind its guard:
  every mod of the series reaches Mekanism's gases, infusions, pigments and slurries through the Core, and none needs
  Mekanism to run. A check of the sources keeps every optional mod (Mekanism, JEI, EMI, FTB, ComputerCraft) named only
  inside an integration, and out of the class that starts it.
- (Core, Computers, Industrial) Saves: What the series kept with a world in files of its own is kept as states, each
  file carrying the version of its layout: the data networks of each dimension and the awards players earned while
  away stay in the files they were in, and the ledger of a world's recordings moves from the text file beside them into
  the world's saved data, as `jstech_media_ledger.dat`. A world saved before is read as it was; the text ledger is read
  once and goes once the world has been saved with it.
- (Core, Computers, Industrial) Saves: Everything else the series saves carries the version of its layout too: every
  block entity of J's Core, J's Computers and J's Industrial, the networks a chunk is on, the operator of a machine,
  the programs a player first ran and the store of what the drives hold. A save from before is read as it was, and
  saved again with its version.
- (Core, Computers) Settings: The series' settings files are written by J's Core's settings files:
  `jstech-balance.toml` (the balance of the Operations engine, the programs' time and the recordings),
  `jscomputers-server.toml` (the computers and Soundfoundry) and the player's `jstech-audio.json`. They keep their names
  and their settings, so nothing set in them is lost; each now says the version of its layout, and every balance
  setting has a comment saying what it does along with its range. J's Core's "Operations and programs" section is now
  "Engine and programs". A sound preferences file with one volume written wrongly keeps the others.
- (Core) Creative tabs: A mod built on J's Core has its creative tabs side by side in the order it declares them.

### Removed
- (Computers) Racks and servers: The RAID Controller and the Cache Card, which went in a rack's gadget bays: the Vaultis
  storage cards take their place in the server itself.
- (Computers) Autocrafting: The Crafting Switch and the Crafting Input Bus. The Crafting Interface holds a machine's
  recipes and feeds it, and the Crafting Input Router puts each input in through its face.
- (Core, Computers) Autocrafting: The machine picker of the Pattern Studio, and with it, in J's Core, a block's list of
  the recipe types it is the machine for and the data files that gathered them: a machine recipe belongs to the
  interface of its machine.
- (Computers) Operations engines: The script a Mainframe kept for the Network Management Studio's editor: a studio's
  scripts are files on its computer's disk.
- (Core, Computers) Manuals: The unused hook for the Modonomicon guidebook: the series' manuals are drawn by J's Core
  itself.

### Fixed
- (Computers) Installers: Arch's pacstrap and Gentoo's tar count the room on the root partition before they lay the
  base system out: on a root too small for it pacman says the partition is too full and installs nothing, and tar runs
  out of space. Both laid the whole system out on any partition, however small.
- (Computers) Installers: Arch's and Gentoo's live installs take the firmware's partition mounted at /boot/efi, besides
  /boot and /efi: mount refused that place, though grub-install looks there when it is not told where.
- (Computers) Installers: A machine that already had a name takes the one typed in a system's installer: the old name
  came back as soon as the disk's console was read again, so the machine went on answering to it after the install.
- (Computers) Installers: A guided installer's progress follows the disk the system goes on. Picking another disk, or
  erasing one to install over it, timed the copy for that disk but left the steps on the page, and when they unlock,
  sharing out the time of the first disk the installer had suggested.
- (Computers) Installers: Choosing another disk in a system's installer after agreeing to erase one takes the erase
  back; the copy erased the disk first agreed to while installing onto the other.
- (Computers) Installers: A system's copy that fails with a long reason shows as much of it as fits above the buttons,
  the last line ending in dots, where it ran on under them.
- (Computers) Installers: A Linux installed by hand is only offered software its hardware era runs, as every other way
  of installing already was; a Vintage machine could be given a desktop that needs Legacy hardware while it was being
  built.
- (Computers) Installers: An operating system installs only when the disk has room for it beside the systems already
  there, a disk refuses a seventeenth system instead of dropping it, and the DOS boot screen shows the real free space.
- (Computers) Installers: A system's setup is saved while it runs, so a reload in the middle of it goes on from where it
  was; formatting the disk it runs from stops it.
- (Computers) Installers: `fdisk` keeps the partition number and first sector typed, a partition that takes the rest of
  the disk reserves its room, and `mount -t` and `-o` are read right; a huge `MAKEOPTS` no longer stops every build
  step.
- (Computers) Installers: `fdisk` answers a size past any disk with "Value out of range." A long run of digits threw out
  of the tool, and was played again on every load of the world; a size in terabytes went round to below nothing.
- (Computers) Installers: The files of an install by hand hold 131,072 characters between them, and a line that adds
  past that answers `No space left on device`; a line such as `cat f >> f`, run again and again, grew the machine's save
  without end.
- (Computers) Installers: The setup on an installer medium, opened from the explorer, runs for a drive west or north of
  the world's origin; it did nothing there.
- (Computers) Installers: The steps pacman counts through while it installs are no longer cut short at a fixed width.
- (Computers) Operating systems: A program that only installs on a Crafting Computer is refused on any other machine
  however it is asked for: Gentoo's emerge built the Crafting Manager on a Personal Computer, where pacman, apt and pkg
  refused it.
- (Computers) Operating systems: A program that shows itself only in windows, typed at the prompt of a Linux or a
  FreeBSD with no desktop, says it cannot open a display, as on a real one: it was a command not found, though it was
  installed.
- (Computers) Operating systems: A restart closes every program a computer was running, as on any machine. The
  programs a player started at a prompt kept running through a reboot, as if the machine had never stopped; only
  switching it off ended them.
- (Computers) Operating systems: Shutting a computer down from its desktop shows the system's own goodbye, and the
  machine goes dark only when it has finished; it used to cut the power at once, dropping the player out of the
  computer with no screen at all. Restarting from the desktop shows the goodbye too before the self-test, as restarting
  from a prompt already did.
- (Computers) Operating systems: The help command's manual page gives first a line that works on every system that has
  help, help on help itself; its only example was help dir, a command of the DOS family alone, which answered with an
  error on every Unix.
- (Computers) Operating systems: Settings, the System Monitor and the Network Manager call a machine's system by its
  name. A Linux, FreeBSD or UNIX system was shown by its id, `ubuntu` rather than Ubuntu, and the System Monitor showed
  every system that way, the Frames editions included.
- (Computers) Operating systems: Every program says what it does on its install disc, in the package manager and in the
  installed-programs list. CDE, Cluster Manager, Emacs, Exposure, Gateway Manager, Help Viewer, Vim, Virtual Studio,
  Virtual Studio Code and Workstation Info had nothing to say there.
- (Computers) Operating systems: screenfetch draws every logo as neofetch draws it. Ubuntu's was another, older logo
  copied with mistakes, and Debian's had a stray quote. Each was printed in one colour, readout included; now the art
  is in its own colours (Ubuntu red and white, Debian white with a red centre, Gentoo magenta and white) and the user,
  host and labels in the logo's. In a terminal narrower than the logo and its readout the rows used to wrap, which
  broke the logo in two with the readout pushed into it; they are now cut at the edge, as neofetch does. FreeBSD's is
  unchanged. The art is neofetch's, credited in THIRD_PARTY_NOTICES.md.
- (Computers) Operating systems: `pckmgr` and the Unix package managers are there off a network too, and say they
  cannot reach the Mirror.
- (Computers) Operating systems: A player's package taken off a machine can be installed on it again. Removing it
  deleted its files but left its folder, and the next install found the folder there and gave up as if the system had
  no folders.
- (Computers) Operating systems: Erasing a system disk forgets the programs a player installed from the Mirror, which
  lived on it; the desktop went on showing icons for programs whose files were gone.
- (Computers) Shells: MC-DOS's write puts in the file everything typed after its name, as it is typed: quotes and the
  spaces between words were taken out, so a Sigma program written at the prompt lost the quotes of its strings and did
  not compile.
- (Computers) Shells: A program in front of a machine's own prompt, on a machine with no desktop, reads what is typed
  there: the line went to the shell as a command instead, so such a program could never be answered.
- (Computers) Shells: A command that takes the whole terminal, such as `less` given a file, `interac` or an editor, is
  refused on a line that pipes or redirects, instead of printing nothing and leaving the file it was sent to empty. A
  line that ends in `clear` or `cls` now clears the screen, as the command does on its own.
- (Computers) Shells: A shell takes its pipes and arrows only where they were typed bare. An arrow in quotes (`grep ">"
  notes.txt`), a file name a star opened out into, and a name whose value is a pipe are words for the command, where
  they could empty a file nobody named. A line sent to the background is kept as it was typed, its quotes too.
- (Computers) Shells: A star in the last part of a path opens out in that folder: `ls data/*.sgs` lists the Σ files in
  `data`.
- (Computers) Shells: `touch` makes a file that is not there and leaves one that is as it was, where it emptied it.
- (Computers) Shells: The text tools of the Unix systems (`grep`, `wc`, `head`, `tail`, `sort`) read a file named from
  the root, such as `grep -i error /var/log/cron`; the slash was taken for a DOS switch and the file left out.
- (Computers) Shells: `mv` with a bare new name moves the file into the current folder, as on a real shell; a move
  across drives that cannot delete its source takes the copy back; a copy never overwrites a file when every copy name
  is taken.
- (Computers) Shells: Sharing a folder whose name another share already has is refused instead of replacing it, an
  unknown theme name is an error, a bare `&` does nothing, and a live installer keeps the quotes and spaces of its
  lines.
- (Computers) Shells: `AT` schedules a command that has `/DELETE` among its own words. The word anywhere on the line
  made it a delete, which then looked for a job numbered by the time.
- (Computers) Shells: A terminal tool that fails on an answer lets go of the terminal, and is not played the answer
  again when the world is loaded.
- (Computers) Files: A file cut and pasted where a file of its name already is makes the desktop ask whether to replace
  it: Yes moves it over the other, No leaves it where it was. The paste did nothing and said nothing, and the cut was
  gone with it.
- (Computers) Files: The system's Save As window opens on the kind of the name the program offers, or on every file
  when none of its kinds is that one. It always opened on the first kind listed, so a note the Editor offered as
  untitled.txt and the player named journey.txt was saved as journey.txt.sgs, the first language's sources.
- (Computers) Files: A file can no longer be written, renamed or copied onto the name of a folder. The disk then held a
  file and a folder of the same name, which a listing showed twice and nothing could tell apart.
- (Computers) Files: Copying or moving a file to another drive or medium, or to another machine's shared folder, no
  longer writes over a file of the same name already there. A copy on the same drive always refused; across drives it
  was a plain write, which replaces, and a move then deleted the original as well.
- (Computers) Files: A file can no longer be made under a path whose parent is a file, and `rmdir` and `copy` respect
  an installer medium as the other commands do.
- (Computers) Files: A file on a disk holds at most 32,767 characters, which is what a disk can carry to a player's
  game: a disk with a longer file dropped whoever opened the computer it was in. A save, a copy, an addition or a burn
  past that is refused, and says why; a file already longer is cut to it when its disk is loaded.
- (Computers) Files: A disk holds at most 16,384 files and 4,096 folders, so that what it holds can always be listed; a
  write, a copy or a new folder past that is refused like one on a full disk.
- (Computers) Files: Saving a file whose path is near the longest one allowed no longer fails to send its answer, which
  left the editor waiting: the answer names the path, and with it ran past what the answer could carry. It is now text
  of any length.
- (Computers) Files: Text cut to fit a message, and a volume label, are never cut between the two halves of an emoji or
  a rare character, which reached the other side as a question mark.
- (Computers) Desktops: A desktop opens on a machine with more than sixteen programs installed, more than 256 entries
  or more than 256 icons pinned to cells, and the network, cluster and plan screens send at most what their lists hold:
  the lists a desktop is sent were capped and refused whole past their cap, which took the desktop away, and a big
  network could fail to open them.
- (Computers) Desktops: On a desktop, a window's buttons light only under the window in front, the minimize, maximize
  and close buttons of a window behind another answer the first click and bring it forward, a click on the bare desktop
  takes the keyboard away from the window that had it, and a click on the monitor's frame below the glass no longer
  reaches the taskbar. The desktop's pointer is drawn where the mouse is to a fraction of a pixel, so it moves as
  finely.
- (Computers) Desktops: Items dragged in a desktop window's inventory land in the cell they are let go over, the
  hotbar's row included, and the player's items in the Network Interactor and the Pattern Studio stay on their cells
  while the window is resized.
- (Computers) Desktops: A chest in a desktop window, in an inventory, a grid or on the pointer, no longer shows a white
  line down its front corner: an item drawn at the desktop's three-quarter size is laid on the screen's pixels, as at
  the game's own sizes, so its faces meet without a gap.
- (Computers) Desktops: This PC fits its hardware list's names to the longest of them, keeps a drive's bar clear of the
  words saying what is free, and marks a line it shortens.
- (Computers) Desktops: Cinnamon's menu searches as you type, lists the category chosen, and has its Quit button at the
  foot of the favourites.
- (Computers) Desktops: Open with's Choose another program lists every program on the computer that opens files.
- (Computers) Desktops: Refreshing a folder no longer doubles its path in the title, a link clears the message at the
  foot of a help page, and a copied selection keeps its line breaks.
- (Computers) Desktops: The editors save the document they were asked to, and close only once the save is confirmed;
  the explorer keeps a listing asked for while another was on its way; Write Out with a new file name writes to that
  name.
- (Computers) Desktops: A folder being typed into the Settings' sharing field is no longer lost when the page refreshes
  before it is shared, and the field keeps the keyboard through the refresh, so what is typed after it lands there too.
- (Computers) Desktops: The amber of cautions on the Legacy computing screens was hard to read on their light panels; it
  is a darker goldenrod now, which reads on every ground those screens draw.
- (Computers) Desktops: The JSC Technologies mark on a modern machine's self-test and Frames 11's start, and the logos
  Frames 95 and Frames XP come up behind, stand in the middle of the glass, where they stood well left of it. In Frames
  XP's setup the lockup in the side panel's header starts in line with the steps listed under it.
- (Computers) Desktops: The cursor stays where the player left it when a monitor moves on by itself from one screen to
  the next: a system coming up hands its start to the desktop, a restart goes to the self-test, an installer turns its
  page. Each change used to put the cursor in the middle of the window. J's Core does this for every window built on it.
- (Computers) Desktops: The terminal's scrollback is let go when a world is left.
- (Computers) Programs: Paint saves its pictures, under the `.pix` name a file of their kind takes, and Files calls them
  pictures. A Save As dialog adds the kind of file to a name typed without one, makes a new folder where it is and
  names it at once, and its list has a menu on the right button, with New folder and Rename.
- (Computers) Programs: Exceed asks before opening a sheet over one with changes that are not saved, which it threw
  away without a word.
- (Computers) Programs: Minesweeper no longer fails on the first open area of a board.
- (Computers) Programs: The Knot client reaches every revision.
- (Computers) Σ and Σ#: A Sigma# program's windows leave the desktop when it ends, stopped or done, though it was the
  last program the machine ran: with nothing left to run the machine sent nothing more, and the windows stayed on the
  screen.
- (Computers) Σ and Σ#: The code editors go on suggesting while an if, a while or a for is typed as far as its
  condition: the body not yet written stopped the reading of the whole file, and nothing was offered until it was.
- (Computers) Σ and Σ#: A Sigma script that overrides only some of Script's methods runs: the ones it leaves alone are
  Script's own, which do nothing. One with no OnInit of its own was halted the moment it started, as the manual's own
  example would have been.
- (Computers) Σ and Σ#: A shift in Σ and Σ# takes only the low bits of its count, as C# does, five for an int and six
  for a long, and is of the kind of its left side: 1 << 32 was 0, and an int shifted by a long count came out a long.
- (Computers) Σ and Σ#: Σ, the smaller language, refuses what it does not have wherever it is written: in what a field
  starts as, in what a constructor hands to the one it calls first and in what an enum's names are numbered with, which
  it let through.
- (Computers) Σ and Σ#: An array in Σ and Σ# answers to `Length`, the number of places it has, which Σ's own advice for
  `foreach` told a program to use and nothing let it read. It is only read: an array is as long as it was made.
- (Computers) Σ and Σ#: A `break` out of a `switch` no longer lets go of a lock the switch sits inside. Written in a
  loop, inside a `lock`, it let go of the lock at the break and again where the lock ends, and the second letting go
  halted the program for giving up a lock it did not hold. It now lets go only of the locks taken inside the switch.
- (Computers) Σ and Σ#: An `out` parameter given a value in every section of a `switch` that has a `default` counts as
  given. The compiler threw away what each section did and refused a correct program.
- (Computers) Σ and Σ#: The compiler catches two `switch` labels that pick the same value written two ways, such as
  `65` and `'A'` in a numeric switch. They were compared as written and both accepted.
- (Computers) Σ and Σ#: A source nested more deeply than anyone writes, a few thousand brackets, blocks or operators
  inside each other, is refused with an error saying so instead of crashing the compiler with a stack overflow; and
  classes inside each other thousands deep, and a class or an interface that stands behind itself (error S3059), are
  refused, where either stopped the game.
- (Computers) Σ and Σ#: The Σ compiler clears the field `dispose` names, not a field of the object it held; reads a
  variable a lambda uses only inside a string's `{...}` from where the lambda keeps it; and no longer stops on a `$"`
  string whose last character is a backslash at the end of the file.
- (Computers) Σ and Σ#: The Σ compiler builds what it used to get wrong: a constructor chained to another picks the
  overload whose types fit, not the first of that many arguments, and its arguments are compiled; a struct captured by
  a lambda before it is given a value starts empty; an `out` argument written to a field keeps its object. A float or
  double literal too large or too small for its type is an error rather than infinity or zero, an unclosed `{` in a
  string is reported once, a `switch` whose section breaks out is checked for its missing `return`, and a record's own
  constructor, `ToString` and `Equals` are left out only for a written one with the same parameters.
- (Computers) Σ and Σ#: A program asking for an array past its memory is stopped with "out of memory" before the array
  is made, where an array of two billion places ran the game itself out of memory.
- (Computers) Σ and Σ#: A file a program writes a little at a time costs its memory only what the file holds; each
  write kept the whole file before it too, so a small file ran a machine out of memory.
- (Computers) Σ and Σ#: A program that waits a very long time no longer wakes at once, a delegate bound to a disposed
  object stops the program, and the calls that return lists cost by the rows they return.
- (Computers) Σ and Σ#: A package keeps a file whose text has a line starting with `--- `. That line was read as the
  start of another file, so the package came back from the Mirror with the file cut in two.
- (Computers) Σ and Σ#: A Σ package (`.pkg`) is read whole and only as it was written: a `#` inside a value is kept, a
  file with no final newline comes back without one, a name given twice or a name reaching outside the package's folder
  refuses the package, and only a file that starts exactly with the format's head is taken for one.
- (Computers) Σ and Σ#: An assembly listing that names a slot a method does not have, labels two lines the same, leaves
  text unquoted or writes instructions after a method with no slots line is refused with the line, where it ran or
  dropped them.
- (Computers) Σ and Σ#: A Virtual Studio project's name is one word of letters, digits and underscores, so a comma or a
  quote in it can no longer split or break its sources when it is opened again.
- (Computers) Σ and Σ#: The Virtual Studio no longer drops the connection when a program's listing is longer than a file
  holds: the build fails and says so. The code editor and the terminal editors refuse to save a file that long, with the
  reason, where they dropped the connection too.
- (Computers) Σ and Σ#: Text a save keeps whole at any length: what a running program holds, its listing, a hosted
  program's binary, the files of an install by hand, a file's revisions in Knot, a job's script and a pattern waiting
  at the encoder. Past 65,535 bytes a save wrote such text as nothing, so it came back empty.
- (Computers) Σ and Σ#: On the client, a line drawn far off a Sigma window, a bar or a scroll with values near the
  largest a number holds, deleting a selection in a text field whose anchor had been left behind, and a calculator or a
  spreadsheet formula nested hundreds deep no longer crash the game. The calculator takes 256 characters, and a formula
  past that depth is an error in its cell.
- (Computers) Σ and Σ#: A text box laid out with no width in a program's window no longer hangs the game, a value a
  program's widget cannot show leaves it working, a click just above a list no longer picks its first row, and hover
  and click agree on the edge of a grid cell.
- (Computers) IQL: Opening a large `.iql` in the studio and running it no longer drops the connection: a script goes a
  statement at a time, and a statement longer than a packet carries is refused before it is sent, its line named.
- (Computers) IQL: The disks table answers, with each server's disks, and the columns the studio's explorer lists are
  the ones a query reads, rather than names that read nothing; the explorer says when the index is not there instead of
  failing.
- (Computers) IQL: A second studio window no longer takes the answers meant for the first: every answer names the window
  and the tab that asked.
- (Computers) IQL: Saving an IQL file from the studio to a full disk no longer fails to send its answer: the reason was
  longer than the answer could carry, which is now text of any length. A long answer to a statement is carried whole
  for the same reason.
- (Computers) IQL: A file of IQL statements run at the prompt with `run` runs every statement in it. It was read as one
  statement, so any file of two, or one with a comment line, failed as a syntax error, although the same file run by a
  program worked. Both now read a file the one way the studio writes it: a statement a line, with blank lines and `--`
  comments left out, and the whole file is checked before any of it runs.
- (Computers) IQL: An IQL quantity below zero, a day name it does not know and a number past what it holds are refused
  instead of read as something else; a job's trigger words and a procedure's semicolons inside quotes stay text; a
  schedule goes on after the world's clock is set back; and `UPDATE ... FROM` with `ALL` takes all the named server
  holds.
- (Computers) IQL: `SELECT ... TO HAND` from the prompt hands the items over; it reported a failure every time.
- (Computers) IQL: An IQL statement whose `LIMIT` asks for more than 256 kinds of item says that 256 were shown.
- (Computers) IQL: `QUERY` of servers, computers, recipes and operations reads the same tables as `SELECT`, with its
  columns, and honours `WHERE`.
- (Computers) IQL: An automation job is paused only when there is such a job, and deleting one forgets its pause and its
  trigger, so a new job of the same name runs.
- (Computers) IQL: A long name no longer drops the player looking at it. A job named with more than 48 letters broke the
  Processes tab of its Mainframe, a supercomputer node with many programs installed broke the Cluster Manager, and more
  than 64 jobs broke the Automation Manager's list. A job, a view or a procedure takes a name of at most 64 letters,
  from the Automation Manager as from IQL's `CREATE`.
- (Computers) Operations: Cancelling by a short id that several Operations start with asks for more of it instead of
  stopping one of them.
- (Computers) Operations: A Mainframe whose system disk is erased or taken out while it runs lets go of the Operations
  it was carrying, as switching it off does: items are conserved and every hold on the storage is released. They used to
  stand frozen, holding the storage, for as long as the Mainframe went without a system.
- (Computers) Operations: An Operation step that fails on the server's thread no longer takes the tick down with it, and
  the Operation ends as crashed with its real message, where it showed as cancelled.
- (Computers) Autocrafting: What a machine gives back after its last job settled reaches the network. Nothing collected
  it once no job was running, so a job given up on, or one whose machine was slower than its timeout, left its late
  outputs in the machine.
- (Computers) Autocrafting: A machine's output is no longer lost when the network has no room for it. A machine step
  pulled everything finished out of the machine and stored what fitted, and whatever a full network refused was gone.
  Only what is stored leaves the machine now, and the rest waits in it until there is room.
- (Computers) Autocrafting: An import bus broken mid-move drops only what the network has not taken, and a reload sends
  again only that part; items a craft makes that the network cannot take are dropped above the machine, and a fluid or a
  chemical that cannot be stored is said in the log, where they were lost.
- (Computers) Autocrafting: A craft whose planning failed no longer waits in the Mainframe's list for ever; it is turned
  down as a craft with no way to make it.
- (Computers) Autocrafting: A craft asks for at most 2,147,483,647 of anything, and a plan's sums stop at the largest
  number they hold rather than going round to nothing, which showed a huge request as already covered.
- (Computers) Autocrafting: The crafting screen requests the quantity whose plan it shows, never zero, and an amount of
  more than nineteen digits is refused instead of read as 1.
- (Computers) Autocrafting: A machine recipe's draft keeps no cell of nothing or less, and holds each amount to what one
  run can move, however it was filled; a pattern file read back is held to the same.
- (Computers) Autocrafting: A pipeline in the Pattern Studio holds at most 16 stages, what its window shows, and a
  pattern too large for a file is refused before it is sent to the encoder.
- (Computers) Autocrafting: Two recipes for the same result are mirrored on a Crafting Computer's disk under names of
  their own (`torch.craft`, `torch_2.craft`), and removing one removes only its own file.
- (Computers) Autocrafting: A Pattern Encoder's Cancel queue and Eject buttons could be clicked on the first frame of
  its screen, before the screen knew whether they had anything to do.
- (Computers) Storage: A computer on no network hands over what its own storage holds through the Network Interactor.
- (Computers) Storage: A degraded RAID 1 or RAID 5 array keeps the size it was formed with, and a drive whose contents
  cannot be read in full keeps them until it is written again.
- (Computers) Storage: A simulated insert into an inventory counts every slot, and a volume that cannot be written takes
  nothing.
- (Computers) Storage: A rack with no addressable computer offers no storage of its own, and a supercomputer survey
  reaches every node.
- (Computers) Storage: The Network Interactor's Category button keeps its word clear of its arrow, "Nothing on the
  network." no longer fades where the pointer passes, and the Status tab fills the window above the inventory.
- (Computers) Storage: Setting a RAID mode in the firmware of a rack with no single server to set it on no longer drops
  the connection.
- (Computers) Computers: A computer, a Mainframe or a rack taken by anything but a player, an explosion, a command or
  another mod's machine, gives back what it holds: its parts, its servers and drives, and a Mainframe or a rack its own
  cabinet too. Only a player's break in survival gave them back; anything else lost them, and a creeper took a whole
  Mainframe. A player in creative still takes the machine and leaves nothing.
- (Computers) Computers: A computer has one name. Renaming it in its system's settings, at its prompt or in its
  installer renames it on the assembly screen, and the other way round; the two kept names of their own and showed
  different ones. A disk put into a machine with no name of its own names it, as a disk sold with a system does, and a
  machine with a name keeps it over the disk's. A server in a rack and its system are named the same way.
- (Computers) Computers: A monitor on a computer switched on with parts that do not make a computer says so, and that
  its assembly screen lists why; it said the computer was powered off, so a player who had pressed the switch thought it
  had not taken.
- (Computers) Computers: Switching a machine off and on while it stood at a failed self-test no longer brings it back
  standing at the old failure, which outlived the power cycle and could send the machine past the new self-test it owed.
- (Computers) Computers: The Legacy's PCI Crafting Card seats in the Legacy's own boards, AGP and PCI-e, as the
  personal-use cards do, and in the later ones: it fitted only a Vintage PCI board, so no Legacy Crafting Computer could
  take it.
- (Computers) Computers: An Integra Phi stays out of a computer's card slots, as the manual says: only a supercomputer
  node seats one. A Personal Computer or a Mainframe took it, where it added nothing.
- (Computers) Computers: Nothing in the mod loads a chunk any more by looking at a place it remembers. A monitor asking
  after its computer, a computer counting its screens, speakers and drives, a Mainframe reaching the servers, racks and
  computers of its network, a cluster's nodes and the file and cluster screens all looked at the block at the
  remembered place, and the game answered a place whose chunk was not loaded by loading it from the disk, or making
  it, in the middle of the tick. Besides the stutter, a chunk caught that way while the game was letting it go could
  stop every chunk from loading until the server restarted. A place whose chunk is not loaded now counts as away, and an
  ssh session whose far machine is away ends as one whose machine has stopped.
- (Computers) Computers: Nothing a player can save, type or build sends the game more than a packet carries any more,
  which dropped the connection. A Save As or Open dialog (Exceed's and Exposure's among them), an editor's Save and the
  Virtual Studio refuse a name or a folder too long for one, and say so; the print dialog keeps its fields to their
  length. A desktop listing a folder of a great many files, a terminal with a long scroll behind it, many windows open
  at once and a Mainframe with a long list of operations reach the screen cut to what it can show.
- (Computers) Racks and servers: A server's slots take only the parts its board takes, as a computer's do: a processor
  of the board's socket and era, memory of its generation and a card of its bus. They took any processor, memory or
  card, and the server then failed to start.
- (Computers) Racks and servers: A rack refusing a chassis of a later era than its own says so, instead of telling the
  player to take it to the kind of rack they are already at.
- (Computers) Racks and servers: A supercomputer node's assembly screen is titled Supercomputer Node, its name field
  asking for a node's name; it said Server.
- (Computers) Racks and servers: A server with nothing to boot no longer costs its rack time on every tick. A machine
  whose self-test found no system stands at that failure and goes on by itself once one is installed, and to notice that
  it was asking about its disks, and about every drive it is cabled to, twenty times a second; a datacenter of servers
  kept only for storage spent half of an idle tick on the question. It now asks once a second, each machine on its own
  tick of that second. The rack itself also answers questions about its bays without working out afresh, each time,
  which units are mounted in it.
- (Computers) Racks and servers: A server rack's screen shows how far the cabinet is throttling even when the rack is
  not on a data network. The reading was only written for servers registered on a network, so a rack off the network,
  and every supercomputer rack, showed a cabinet running free however hot it ran.
- (Computers) Racks and servers: A rack's KVM switch keeps the machine it was switched to through a save. It came back
  from every reload showing the first machine in the rack.
- (Computers) Racks and servers: The Mainframes' and the racks' items show the machine as it comes, empty and switched
  off. They are drawn with the same models as the machines in the world, and took on the hardware, the servers and the
  lamps of the last one drawn, or showed every part at once, every kind of server in every row, before any was.
- (Computers) Mainframes: The service panel of every Mainframe no longer flickers where it meets the edges of its
  opening: its edges lay on the walls of the opening and fought them for the surface, which showed through the grille,
  the louvres and the glass.
- (Computers) Mainframes: A Mainframe of any era is mined at a pickaxe's pace. It needs the right tool, and no tool
  counted, so taking one down in survival took far longer than a block of metal should.
- (Computers) Network: Breaking a router in the same tick it was placed no longer throws. What carries the network is
  put in the network's map when it first loads, which comes after it is placed, and breaking a router before that asked
  the map to take out something it never had. Cables already allowed for it; all three now ask the same way.
- (Computers) Network: Typing a Server Router's name no longer closes its screen at the letter E, the key that closes an
  inventory: while the name is typed every key goes to it, and Escape leaves the field.
- (Computers) Network: The Import and Export Buses, the Supercomputer Node and the HBW Interface say in their tooltip
  what they are for, like the rest of the network and rack equipment. The words were written but never shown.
- (Computers) Monitors: A monitor is used by one player at a time. A second player who used it while somebody was at it
  was handed a session over the one being typed into, and ended the remote session the screen was holding; now they are
  told who is using the monitor, and the one at it keeps it until they walk away or close it.
- (Computers) Monitors: A self-test, installer or KVM screen closes when its monitor is broken, rather than staying open
  on nothing, and a terminal opens on a monitor whose machine is beyond what the player's game has loaded, where it
  failed.
- (Computers) Monitors: A terminal reopened on a tab that is no longer there, the Craft tab after the last Crafting
  Computer left or the Patterns tab after the card came out, opens on Network for the player too. Only the server moved,
  so the screen showed an empty tab the server was no longer filling.
- (Computers) Network Gateway: What a Gateway spent on a machine's behalf is still owed after a save when the machine
  runs no program; the save left it out, so a reload wiped the debt the machine was still paying down.
- (Computers) Network Gateway: A Gateway cuts what a ComputerCraft program hands it, the level of a log line and the
  names in a request, to what its log keeps. A long one stopped the chunk with the Gateway in it from saving. A sentence
  a machine keeps in its save is held to the 8,192 letters it travels in.
- (Computers) Network Gateway: Two Gateways on one ComputerCraft wired network can no longer publish the same name: a
  rename to a name taken is refused and a default name is numbered until it is free; `a-b` and `a_b` count as the same
  name, as they do there.
- (Industrial) Machines: An Industrial machine spills what it holds however it goes: blown up, replaced, or broken in
  creative mode, where before only a player breaking it in survival got its contents back.
- (Industrial) Machines: The Macerator, Electric Furnace, Compressor and Coal Generator can be mined in survival. They
  need the right tool to come away, but no tool counted as right for them, so breaking one gave nothing back and took a
  long time doing it. A pickaxe is now their tool: it mines them at a pickaxe's pace, and they drop themselves.
- (Industrial) Machines: The Coal Generator gives the empty bucket back when it burns a lava bucket, as a furnace does.
- (Industrial) Storage: A Tank broken keeps its fluid in its item, which says what it holds, and gives it back when it
  is placed.
- (Core) Interface: Clicking another title while a menu of a menu bar is open opens that one; a checkbox changes only
  on a left click; clicking into a text field puts the caret where it was clicked; the last tab of a tabbed screen
  takes the pixels left over, and a click there selects it; a dialog's answer can open another screen.
- (Core) Interface: A compact number never shows as 1,000 of one scale: 999,999 FE is 1M FE, not 1,000K FE.

### Security
- (Computers) Payloads: The server reads no more text from a client than each message is meant to carry. An automation
  job's fields, a terminal's server names, the program to uninstall and the name help is asked for were read at
  whatever length a client sent, where the messages beside them were already held to a size.
- (Computers) Payloads: The Network Interactor, the craft manager and the file windows ignore requests only a tampered
  client sends (a count below none, a recipe before the first, a file renamed from a medium onto a disk), which failed
  on the server.
- (Computers) Payloads: A crafting pattern whose grid is not nine cells, which a tampered item or file could carry, is
  refused as it is read, and a `.craft` file nested over a hundred deep is refused rather than read to the end of the
  stack.
- (Computers) Messenger: Nobody can read or write another pair's private conversation in the Messenger by naming its
  room, and nobody is shown the names of the private rooms they are not in, which told who talks to whom.
- (Computers) Cluster Manager: The Cluster Manager installs on and switches only the nodes of the cluster chosen. A
  modified client could name any reachable rack anywhere, and a row past a cabinet's last stopped the manager's job.
- (Computers) Monitors: Taking over a remote machine changes only the monitor the player is at.
- (Computers) Firmware: Booting a live medium from the firmware setup takes only a reader the machine is linked to, and
  only a system its hardware runs, as installing from one already did.
- (Computers) Terminals: The Interac screen a machine draws is at most 256 columns by 128 rows: a size asked for without
  a limit could take all of the server's memory.
- (Computers) Shells: A pattern of many stars no longer freezes the server. A shell's file names and IQL's `LIKE` match
  a pattern in a single pass however many stars it holds; each tried every way the stars could split, which a pattern
  of ten of them against a long name made last for hours.
- (Computers) IQL: An IQL condition nests at most 64 NOTs and brackets deep and compares at most 256 things, and a
  longer one is refused with the reason, where thousands of brackets from a program or a file crashed the server.
- (Computers) IQL: IQL stops a request after 4,096 statements, its procedures' and views' counted, and says why:
  procedures running one another over and over held the server's thread.
- (Computers) Σ and Σ#: A Sigma program no longer stops the server's thread, or takes the machine's tick down with it,
  when it copies an object that holds itself, nests thousands of `=` or `? :` in one line, writes thirty calls inside
  one another, calls itself deep with a method of a great many locals, is sent a message with its memory full, or stops
  another program as it leaves. A listing whose method claims more places than any method has is refused when it is
  read.

## [0.4.0a] - 2026-09-21 - The Booting Update

Codename: Beryllium.

### Added
- **Solitaire** and **Snake**, the two games these desktops always shipped with. Klondike drawing one card,
  dealt from a number so the same number deals the same game and one player can hand another the deal they
  are stuck on; and a snake in an arena whose walls can be turned off, which takes away the easy way to die
  and leaves only running into yourself. Both are as light as Minesweeper and run on any desktop. Once every
  card in a Klondike is face up and the stock is gone, the table offers to play itself home rather than
  asking for another forty clicks in the one order they can be made.
- **67ark**, which packs many files into one that really weighs less. A disk here counts the bytes of what
  is on it, so archiving a folder of logs is how a small disk is made to stretch; text that repeats packs
  hardest, so a log collapses much further than a config does, and the program shows what each one saved.
  Opening an archive lists what is inside it without unpacking anything. It is also on the right button,
  where an archiver belongs: any file or folder, in the explorer or on the desktop, offers to compress into
  an archive beside it, and an archive offers to take everything back out where it stands. Compressing a
  folder packs what is in it, however deep it goes. Taking things out never writes over a file that is
  already there; it leaves that one alone and says how many it left.
- **Paint**, a real picture rather than a grid of blocks: a canvas up to 128 by 128 in a palette of 256
  colours, every tool the program it is named after had, zoom and undo. What it writes is about a kilobyte
  where a colour per pixel would be twenty-four, which is what makes keeping pictures on a disk possible at
  all. A picture can be hung on the desktop as its wallpaper, so something a player drew ends up on every
  screen of that machine. Its files, like the archiver's, open on a double-click and carry an icon of their
  own in every listing. All two hundred and fifty-six colours can be reached: the strip shows two rows at a
  time and the wheel runs it through the rest, the dropper brings whatever it picked up back onto it, and
  no two of them are the same colour. The pencil draws from where the hand was to where it is, so drawing
  quickly leaves a line rather than a row of dots.
- **Exceed**, a sheet of cells. Besides the arithmetic a spreadsheet has always done, a cell can ask the
  network what it is holding: how many of a thing, how much room is left, how many servers there are. Those
  cells are drawn apart from the ones the player typed, so nobody wonders why a number moved by itself, and
  they are worked out from what the machine last said rather than from asking the world. It reads and
  writes the comma separated files the mod already had, so a sheet is a file any other program can open.
  However many cells read each other, the whole sheet is worked out in one pass down.
- **Midsoft Messenger** and its **Messenger Service**, the first program here where the other end is
  another player: who is on the network, a window per conversation, and the nudge. The service runs on a
  server mounted in a rack, which is what a server is for, and is reached from any computer on that
  network, so a conversation is there when you next sit down at any machine on it. The history belongs to
  that server and rides on the Server item with everything else it holds: pull the machine out of the
  cabinet and the conversations go with it. The service is also the first in the mod that does not cost a
  fixed amount: it grows on the disk as it keeps what people said, and in the machine's memory as more of
  them are connected, and the window shows both. With no server on the network running it, the window says
  so and will not pretend to send anything.
- **Knot** and **KnotHub**, which keep the source a network is still arguing over. Push a file from the
  machine you are at, pull a revision back onto it, and read who changed what with the changes marked line
  by line. KnotHub runs on a server beside the Messenger Service, and the history belongs to that machine
  the same way. It sits under the package manager rather than beside it: a package is a finished thing one
  player hands to another, and this is the code before it became one.
- The eight programs and the two services carry icons of their own, in the three styles the desktops here
  are drawn in: the outlined artwork of the earliest edition, the glossy one after it, and the flat one the
  later editions and the Linux desktops wear. A card and a heart, a coiled snake round an apple, a parcel,
  a painter's palette, a sheet of cells, two speech bubbles, the fork of a history, and the two services as
  the server that runs them with what they serve badged on the corner.
- The editor grew into a real one. It was a name, a text area and a status line; it now has line numbers,
  find and replace, go to line, the line and column in the bar, Open and Save As through the system's own
  file window, and more than one file open at a time. A page with unsaved work on it asks once before it
  closes, and the page that was saved is the one marked saved even if you moved to another meanwhile. It
  stays a text editor: highlighting, completion, projects and a compiler are what tell the five development
  environments apart, and they stay theirs.
- MC-NET draws the whole monitor, as every other system does. Its interface was a 244 by 230 window with the
  player's inventory under it, throwing away 140 columns of a screen the desktops fill, which is why the one
  interface in the mod that is a whole system read as an inventory panel. It is now the glass: a status bar
  across the top that says which network the machine is on, how many servers it has, how much they hold and
  what is in flight; the headings down the left as words rather than icons; the network's store nine wide and
  seven deep beside a panel that says everything the machine knows about whatever is picked out; and the
  player's own rows glued over the bottom. It keeps the era skins, so a Vintage machine shows it in green
  phosphor with scanlines, a Legacy one in beige and system blue, and the later eras in the dark it had.
- Operating spaces: what a desktop environment is to a Linux, for the system that has no desktop. MC-NET
  ships with one, called the Interactor; take it off and the machine is a prompt and nothing else, exactly
  as a Linux with its desktop removed is a TTY. What a monitor opens is now decided by the space installed on
  the machine rather than by what the system is capable of. An add-on registers a space of its own and ships
  the whole screen behind it, so it can invent a way of working a network that nothing here imagined; ours
  goes in through the same door rather than being a special case behind it.
- The prompt is a heading inside MC-NET's interface. Clicking Console used to throw a separate Command Prompt
  window over the screen, which is a thing a screen that is the whole machine has no business doing to
  itself. It is a view of the machine's own console, so everything the prompt can do on a machine with no
  interface at all it can do here, editors and compilers included. The Command Prompt is no longer offered as
  a program on a network system, because there is nowhere for a window to go.
- MC-NET can be taught a recipe. `crafting_manager` and `pattern_studio` are both desktop programs, so a
  Crafting Computer running MC-NET could not put a single pattern into its own Recipe ROM: the one player
  that system exists for could not autocraft with recipes of their own. A Patterns heading now does that
  work in the system's own shape, under the same condition the Crafting Manager installs under, a Crafting
  Computer with a Crafting Card: the draft with the three destinations the Pattern Studio's bar sends to, the
  `.craft` files on a medium in a linked drive with Load all and Load one, and the ROM with Unload and
  Download beside it. A pattern is still born at a Pattern Encoder and nowhere else.
- The network's store can be narrowed by mod, and what is picked out has a panel of its own: what it is, what
  holds it and how much each of them holds, whether the network has a pattern for it, and the two things
  worth doing with it. It is the same answer the prompt gives for a thing, so the screen and the prompt never
  disagree about what the network is holding.
- MC-NET keeps files. Its kernel declared no filesystem at all while a dozen programs were declared for it,
  the two text editors and both compilers among them: a machine with nowhere to read or write, asked to run
  things that do nothing else. It now keeps a flat store, files at the root and no folders, which is what a
  network appliance of that age had and what tells it apart from the personal computer of the same year. So
  there is no `cd` to change folder, no folders to make or remove, and no tree to draw.
- MC-NET stops speaking DOS. It is its own shell family now, beside the DOS one and the Unix one, and it says
  what it does in whole words: `listfiles` for the disk, `seefile` to put a file on the glass, and `delete`,
  `copy`, `rename` and `write` for the rest. There is no `cd`, no `mkdir`, no `rmdir` and no `tree`, because
  a flat disk has nowhere to change into and no folders to make. Its prompt is `SYSTEM:>`, naming the machine
  rather than a place, since there is no path to put there and no drive letter to carry. A name stands for
  something the way the Unix shells write it, and `/?` goes back to being the DOS family's alone.
- The rest of MC-NET's words, said the same way: `read` for a long file a page at a time, `findtext` and
  `sortlines` for lines, `memory`, `tasklist` and `end` for what the machine is doing, `worldtime`,
  `findcommand`, `clear`, `run` and `format`, `runbackground` and `schedule` for work left behind, and
  `netgetter` for the network's mirror. Where the DOS family shouted a switch this says a word: `end 4`
  instead of `TASKKILL /PID 4`, `format d yes` instead of `/y`, `schedule forget 2` instead of `/DELETE`.
- MC-NET's install has a face of its own instead of the generic one. One page that says what is about to
  happen and waits, then five lines of work, then the server is ready: it asks nothing about the machine,
  because there is no tree to lay down, no folder to choose and no name to type. It is written in the same
  green phosphor its screens wear on a Vintage machine, so the install and the system look like one thing.
  What it does still waits for the player to say go, because no installer here writes to a disk unasked.
- `showcommands` is how MC-NET teaches. An appliance keeps no manuals, so it says everything it can run at
  once, gathered by what a thing is for. It reads the same filter every other listing reads, so it never
  offers a word the machine would then refuse: on a computer with no cable it says nothing of the network.
- MC-NET can be wrecked like every other system. Installing it writes `netstart.sys` at the root of the disk,
  and a machine that no longer finds it says `netstart.sys is missing` and will not start until an
  installation medium writes it back. With no folders it has no system folder to lose, so it has one way of
  breaking where the others have two.

- A name can now be given a value at the prompt and it stays given: `set NAME=value` on the Frames and MC-DOS
  family, `export NAME=value` and a bare `NAME=value` on the Unix shells, `unset` to forget one, and `set` or
  `export` on their own to list the names there are. The names belong to the machine rather than to the window
  they were typed in, so one set at a monitor is there in a window on the desktop, in a session opened from
  another machine, and after the machine has been off. A name the player set stands over one the machine
  answers for, so `HOME` and `USER` can be said to mean something else.
- `interac` on its own now takes the terminal whole and shows the network as a full screen: headings for the
  network, the servers, what is held back, the work in flight and what has been starred, a list under them, and
  a panel beside it saying what the picked row is, who is holding it, what makes it and what it goes into. The
  arrows and Page Up and Page Down move through it, Tab goes to the next heading, typing looks for something,
  the mouse picks a row or presses a key, and the ten keys along the foot do the ten things there are to do:
  take into your hands, put out of them, ask for some made, hold some back, let it go, star it, call off an
  operation, start the search again, the help page, and give the terminal back. The machine draws the screen and
  the terminal only shows it, so it is the same screen at a monitor, in a window on a desktop and over a session
  opened on a machine on the other side of the world, and none of the network is ever held in the terminal.
  `interac` with words after it still does the one thing asked at the prompt.
- Restarting a computer now shows the system closing down first, and the self-test begins when it has finished.
  The one screen a system of any age put up on its way down was the one screen nobody could see: switching a
  machine off showed it and restarting one did not, although restarting is how anybody reboots a computer here.
  A system with nothing to show on its way down still starts over where it stands. Opening the monitor halfway
  through joins the goodbye where it is, instead of handing the player the desktop of a system being closed.
- The Linux family says goodbye too, each init in its own hand: systemd stops its targets and OpenRC stops its
  services with the same stars and column it started them with, ending on the line that powers the machine down
  or starts it over.
- The oldest Frames edition ends where that era ended: black, with the one sentence saying it is now safe to
  turn the computer off, held for a moment before the monitor goes dark.
- Each Linux desktop now has a loading screen of its own between the system's last line and the desktop itself.
  KDE Plasma, GNOME and Cinnamon each have one, and each wears the look of the generation it is running on: the
  modern ones fill the glass and say almost nothing, and the older ones come up in a bordered box with a
  coloured band and a row of squares lighting up as the panel, the desktop and the file manager start.
- A self-test now reads out the board the machine is built on and the video card in it, and names the memory
  modules it counted over. It was being told all three and drawing none of them.
- A drive is now listed the way a firmware listed one: the model, how much it holds, and what is on it, in
  columns. The three travel apart because the self-test, the boot menu and the setup page each put them
  together differently, and one composed sentence cannot be laid out in columns by any of them.
- A modern machine with nothing to boot now says what each device holds and what to do about it, instead of two
  lines saying nothing was found.
- Frames 95's Setup checks the machine before it starts, line by line: the generation, the processor, the
  memory, the room on the disk and the medium in the drive, each ticked as it passes.
- Frames XP greets a new machine from the corner of its own desktop rather than with a window, and a click on
  the notice opens Welcome, which is how that edition said hello.
- A 32-bit x86 machine now runs what was built for the 16-bit one, as the real 386 ran what an 8086 ran, and a
  64-bit machine runs both. Nothing runs what was built for a machine after it. This is what lets one program
  serve every age: `scc` builds for the oldest machine there is unless told otherwise, so a program written in
  Sigma runs on a Vintage computer and on every computer that came later. `sgsc` still builds for the 32-bit
  machines, since the full language's library is not something a Vintage computer has.
- `scc`, the Sigma Compiler Collection, compiles a `.sg` program into the assembly a machine runs. It is a package
  of its own, 4 MB of disk and 2 MB of memory, and the earliest machines can hold it: it is the whole toolchain
  there, since what it writes is the assembly the machine already runs and there is no runtime to install beside
  it. A Legacy machine or later can use it too, and a program built with it runs everywhere. The same program
  compiled by `scc` and by `sgsc` gives the same listing, which is what makes Sigma a subset in fact.
- Sigma has a library of its own, `Standard`, and it is the only one it can reach. Eight types with a handful of
  members each: `Console`, `File`, `Program`, `Math`, `Convert`, `Time`, `Computer` and `Script`. They are the same
  types the full language has, under a second namespace rather than copies of them, so a call written in Sigma
  compiles to exactly the line of assembly Sigma Sharp would write for it. What Sigma does not get is the other
  thirty-eight types and most of the members of these eight. `Script` lives there too, since it is how a program
  that stays up is written and Sigma cannot reach `System` at all.
- The compiler now knows two languages: Sigma Sharp, which is everything it has always taken, and Sigma, a smaller
  one for the machines that could never be programmed at all. Sigma is a true subset, so anything written in it is
  also Sigma Sharp and compiles on a newer machine untouched. It keeps classes with inheritance, `virtual` and
  `override`, structs, enums, arrays, the loops, `out`, `is` and `as`; it has no interfaces, records, delegates,
  events, properties, lambdas, `foreach`, `List`, `Map`, generics, `var`, `lock`, threads, windows, `abstract`, or
  strings with holes in them. Every refusal says what to write instead, because there is a way to write all of it:
  an interface is a class whose methods are virtual, a record is a struct, a `foreach` is a `for` over the array's
  `Length`, a `Map` is two arrays. Nothing already written changes: the full language is what every compiler and
  editor still uses.
- Sigma is written with everything Sigma Sharp is written with. It is a language of its own to the machines, so a
  `.sg` file is one the systems know: it has its kind in the explorer, it can be made from the New menu, and it
  opens in the code editors, where it is coloured, checked as it is typed and completed from what Sigma really
  has (its one namespace, its eight types and the members each of them kept), so nothing is offered that the
  compiler would then refuse. Virtual Studio's New Project lists every shape of project in both languages, with
  the language at the end of each line and in the language filter. A Sigma project keeps itself in a `.sgproj`
  file, holds `.sg` sources, starts from a program that opens `Standard` and a script that stands on `Script`,
  and is built for the oldest machines unless its Platform target says otherwise. A Sigma Sharp project can
  reference a Sigma library. Virtual Studio Code and Exposure build a `.sg` file with `scc`, and `sigma run`
  takes one. An old language is no reason to write it the hard way.
- `printf`, the way the languages of those machines printed: `printf("%s has %d items\n", name, count);`, the
  one call written with no type in front of it. The format is read while the program is compiled, so it has to
  be written out in quotes, and what is left to run is the pieces joined and handed to the console, the very
  listing adding them up by hand would have given. `%d` and `%i` take a whole number, `%f` a number, `%s` text,
  `%c` a character, `%%` is the sign itself, and an `l` before the letter is taken and means nothing. A hole
  with no value, a value with no hole and a value of the wrong kind are compile errors that say which, and so
  is a width or a precision, which this `printf` does not have. Sigma's project templates print with it, the
  editors offer it, and Sigma Sharp has it too, since it reads whatever Sigma does. A program with a `printf`
  of its own calls its own.
- The two compilers have marks of their own, in the manner of the languages they are named after: a blue
  hexagon with a Σ for `scc` and a purple one with Σ# for `sgsc`, each drawn for every desktop. `sgsc` still
  wore the cannon of the language's old name, and `scc` had no icon at all.
- A program that stays up can now be written by standing on the class `Script` instead of implementing `IScript`.
  `Script` already has `OnInit`, `OnTick` and `OnDestroy`, doing nothing, so a script written this way fills in only
  the ones it uses and says `override` on each, while one written on the interface goes on writing all three with no
  word at all. Both are the same program to the machine, and `IScript` is not going anywhere.
- A class can now say which of its methods another class may replace, with `virtual`, `override` and `abstract`.
  A method marked `virtual` may be replaced by one below it written with `override`, and that one may be replaced
  in its turn without anyone writing `virtual` again. A method marked `abstract` has no body at all, only an
  `abstract` class may hold one, such a class cannot be made with `new`, and the first class below it that can be
  made has to give every one of them a body. Giving an interface the method it asked for needs no word, since that
  replaces nothing. `static` methods, constructors and everything in a `struct` take none of the three.
- F12 during the power-on self-test opens a one-time boot menu: every disk with a system and every drive holding
  something bootable, picked with the arrow keys and booted with Enter. What is picked there is for that boot only,
  and the boot order saved in the firmware setup is left exactly as it was.
- `sgsc --arch <architecture>` builds a program for a named architecture, by its id (`jsc:x86_64`) or by the name it
  is written under (`x86-64`). Left alone, the compiler builds for the oldest architecture that runs the program, so
  it runs on every machine it could have. An architecture nothing answers to is refused before anything is written.
- Virtual Studio's Project Properties has a Platform target: one button per architecture, and a line under them
  saying which machines the program will run on, amber once the choice leaves machines behind. A project keeps it on
  a `platform:` line, and a project file without one builds as it always did. A library project has no platform of
  its own, since it is compiled into the program that references it.
- `Computer.Cpu.Architecture` gives a program the architecture the machine it runs on is built on, written the way a
  listing names its own, so a program can compare the two.
- Every mod's jar now carries its licence texts under `META-INF`: the GNU LGPL 3.0 the series is licensed
  under, and the GNU GPL 3.0 it builds on.
- `Program.DroppedEvents` counts the clicks and watch alerts a program missed because too many of its calls were
  already waiting.
- Open with: a double-click on a file of a kind the computer does not know asks which program opens it, among the
  programs on the computer that open files. Only this time opens it once; Always makes that program open every file
  with the same extension on that computer. Choose another program..., at the end of Open with on the desktop and
  in Files (where Open with is now a submenu), brings the same choice up for any file. An addon registers a program
  that opens any file with `JSComputersAPI.registerFileOpener`.

- A machine in a rack now comes up the way any other machine does: its own power-on self-test, its own stop at a
  boot manager, and its own system taking the time it takes, all on the machine's clocks. Its bay switch is its
  power button, so flipping it off stops whatever was under way and flipping it on starts a self-test. The pages
  only reach a monitor while the KVM switch is on that bay, and the machine goes on regardless, so whoever
  switches over is put wherever it has got to.
- A machine in a rack now installs a system the way any other machine does. The bay keeps the copy and the
  installer it is in, written onto the Server item with the rest of its session, so the work goes on with nobody
  watching, takes the time it costs and is still going when the world comes back. It used to be written to the
  bay's disk there and then, in no time at all, with no installer and nothing to watch. Where a rack holds more
  than one machine, the installer's pages only reach the monitor while the KVM switch is on that machine's
  channel, so a page meant for one bay never lands in front of somebody looking at another.
- What was chosen while installing a system by hand now survives into it. The machine answers to the name the
  installer was given, and its prompt and the network use it. The packages asked for inside the new system are
  really installed on it. The filesystem table goes onto the disk it describes, so the installed system can read
  back the line its own bootloader was pointed at. A restart is refused while something is still arriving,
  rather than leaving the system half of what was asked for.
- Fetching a system by hand takes time, and what needs it waits. `pacstrap` and the stage 3 bring a base system
  over the Mirror at the network's speed, printing what they are fetching a package at a time, and nothing
  enters a system that is still arriving. Extra packages afterwards, with `pacman -S` or `emerge`, take their
  own time the same way.
- A hand-installed distribution now goes through the steps it was missing. `grub-install` installs the
  bootloader for the firmware the machine really has: on an older one it goes on the disk and says so, on a
  modern one it goes in the boot partition and refuses with nowhere to put it. `grub-mkconfig` writes the list
  of what to start, generated from the system that is really installed, and `mkinitcpio` builds what the kernel
  is handed at boot. `hostname` names the machine and writes it into the new system's own file. A reboot before
  any of those says which one is missing, since a bootloader with nothing to start starts nothing.
- A disk can be partitioned by hand before a distribution is installed on it. `fdisk /dev/sdX` opens the
  partition editor, which takes the terminal over and asks its way through everything the way the real one
  does: `g` for a new table, `n` for a partition, which asks for its number, where it starts and where it ends,
  each with a default that Enter takes and the last answered with a plus and a size (`+512M`), `t` to mark the
  one the firmware boots from, and `p`, `d`, `m`, `w` and `q`. Somebody who has done this before can put the
  answer on the command's own line (`n 512M`, `t 1 uefi`). Nothing reaches the disk until `w`, and `q` or
  Ctrl+C throws away everything typed since it opened. `lsblk` lists the disks at the size they really are with
  their partitions underneath, `mkfs.ext4` refuses a disk that someone has partitioned instead of wiping the
  table, `mkfs.fat` makes the boot partition, and that one mounts at `boot` or `efi` under the root, after it.
- A command at a terminal can leave a tool running in front of it, the way a real one does. While the tool runs
  the prompt is away and what it prints arrives as it happens: lines one after another, a bar that fills in
  place, a flood of paths going by. A tool can stop and ask, with its question standing where the prompt would
  and Enter by itself taking the default, and an answer it asks for unseen is not shown. Ctrl+C stops it, and what
  it had not finished stays not done. It keeps running with nobody at the screen, and it is still running after
  the world has been saved and loaded. Both terminals do this, the prompt that fills a monitor and the window
  on a desktop, and any command can start one. A server mounted in a rack does the same, and goes on with
  what it was left running while the rack's switch is turned to one of its neighbours.
- The Gentoo install follows its handbook. The disk mounts at `/mnt/gentoo`, the stage 3 is fetched into it and
  unpacked there, and inside the chroot the package tree comes with `emerge-webrsync` before anything merges.
  `eselect profile`, `emerge --update --deep --newuse @world` and `eselect kernel` are all there to be run, and
  `MAKEOPTS` in `/etc/portage/make.conf` is read. The chooser remembers the profile it was set to, by its
  number or its name, and stars it; a profile says how things are built and installs nothing, as it does not
  on a real one. The kernel can be built either way, `genkernel all` or `make` in `/usr/src/linux`. The
  filesystem table is written by hand from what `blkid` prints, and `blkid >> /etc/fstab` puts the identifiers
  into it to be cut down in the editor, so nobody copies one off the glass. The bootloader is a package that
  has to be merged before `grub-install` exists. Arch is the same in its own words: it mounts at `/mnt`,
  `pacman -S grub efibootmgr` comes before `grub-install`, and `hwclock --systohc` is there to be run.
- What is installed inside the system being built is a program of the Mirror's. `emerge --ask
  kde-plasma/plasma-meta` in the chroot builds everything the desktop is made of, the way it does on an
  installed Gentoo, `pacman -S` installs by the Mirror's own names and versions, and the machine that comes up
  has what was asked for. A name the Mirror has nothing by is refused in the tool's own words.
- A by-hand install sets no root password, no time zone and no locale, and the medium carries no tool that
  does. Nothing in this world logs in with a password yet, the game has no time zones, and the language a
  player reads in is the one chosen in the game's own options, so those steps set nothing.
- The live medium of a by-hand install carries `nano`, and it is nano: the title row with the file's name and
  `Modified`, what it has to say in brackets above the two rows of keys, `^O` asking for the name before it
  writes, `^X` asking about a file that has changed, `^W` and `^\` to search and to replace, `^K` and `^U` to
  cut lines and paste them, `^R` to pull another file in, `^C` for where the cursor is, and `^G` for its help.
  It edits the files of the installation, `make.conf`, `fstab`, the machine's name, and what is saved is what the
  later steps read: `-j4` in the build options is what makes a compile four jobs wide. It is written on the
  black of the terminal it took over, at the terminal's size. Escape is looking away from the monitor and not
  closing the editor: the next look at that machine finds it open on the same file, with what was typed and
  never written still in it.
- Two settings in the server configuration, `install_by_hand.gentoo_every_step` and
  `install_by_hand.arch_every_step`, decide how much of the handbook a world asks for. Off, which is the
  default, a restart only refuses what a system cannot boot without: a base system, a filesystem table, a
  kernel, and a bootloader with its list of what to start. On, it asks for every step of that distribution's
  handbook that means something here and says which are missing. Every step answers the way the real tool does
  either way.
- The live medium of a hand-installed distribution carries files, and `ls`, `cd`, `cat` and `less` to read them.
  The guide the real medium ships with is in `/root/install.txt`, written from the steps the shell really
  accepts. What a step wrote is what reading it back shows: the filesystem table is not there until `genfstab`
  writes it, and then it names the disk the install actually used. Inside the chroot a path is the new system's
  own, so `/etc/fstab` in there is the file written to `/mnt/etc/fstab` outside, and the prompt says where you
  are standing.
- The Frames editions put up a welcome the first time they come up, in the shape each of them used: a tip that
  changes with a column of buttons beside it, a pane of places to go with the machine written out next to it, and
  four cards. It asks nothing, because the installer already asked. Everything on it is read off the computer it
  is running on, down to the tips: the one about installing software names the Mirror only where a Mirror is
  answering, and the one about the network only appears on a machine that is on one. Every button opens a program
  that is really there, under the name that edition gives it. Unticking "Show this at startup" is remembered with
  the system.
- FreeBSD, from the Daemon Foundation: a system of its own beside the Linux distributions, for machines of the
  Legacy generation and later. It comes up at a terminal running `sh`, with the prompt its home directory sets
  up (`player@desk:~ $`), and takes KDE Plasma, GNOME or Cinnamon from the Mirror like the distributions do. Its
  manager is `pkg`, in pkg's own words: `pkg install`, `pkg delete`, `pkg search`, `pkg info` and `pkg update`,
  looking at the repository catalogue before it does anything and naming itself when something goes wrong. What
  is installed goes under `/usr/local`, apart from the base system, and `/etc/rc.conf` names the machine it is
  on. Until its own installer arrives it installs through the plain guided one.
- FreeBSD starts and stops in its own words. The kernel names its release and the architecture of the processor
  under it, which in this world is Velocion's `vel64` on a 64-bit processor and the Integra Architecture,
  `IA-32`, on a 32-bit one; then the memory and the disks that are really in the machine as `ada0`, `ada1`, the
  network only when a cable reaches one, the Mirror only when one answers, and a display manager only when a
  desktop is installed. It stops down to "The operating system has halted.", or "Rebooting..." when it is coming
  straight back. The console greets as FreeBSD does: `FreeBSD/vel64 (desk) (ttyv0)`, the login, the release.
- UNIX System V, from Bellwether Labs: the one system of the first age that is met at a Unix prompt, in ten
  megabytes of disk and two of memory, for a Vintage machine and every one after. It comes up at a terminal
  running `sh` and keeps the habits of its day: the home is `/usr/player` and `~` means that, other drives are
  mounted under `/mnt` by their letter, the kernel is the file `unix` at the root with `init`'s table in
  `/etc/inittab`, and a program added to it goes in `/usr/bin` with what it brings under `/usr/lib`. It has no
  network to install from. `uname -a` answers the way System V did, the system, the node, the release, the
  version and the machine, and names the Integra Architecture down to `IA-16` on a 16-bit processor. Until its own
  installer arrives it installs through the plain guided one.
- CDE, from the Open Desk Consortium: the desktop of the Unix workstations, and the only one UNIX has. It asks
  for a machine of the Legacy generation, 32 MB of disk and 16 MB of memory, a fraction of what the later
  desktops weigh. UNIX takes it from its medium with `installpkg`; FreeBSD takes it with `pkg install cde`,
  beside the desktops it already could. It keeps its own plain names for what it bundles: File Manager, Text
  Editor, Terminal, Calculator, Performance Meter, Style Manager and Workstation Info. With it, UNIX runs the
  desktop programs that ask for Frames XP or a Linux desktop (Network Management Studio, the Crafting Manager,
  Pattern Studio, Storage Insights, the Craft Planner, the Cluster and Gateway Managers, Remote Control,
  Minesweeper); the Automation Manager, Virtual Studio Code and Exposure stay with the later desktops.
- CDE looks like CDE. Windows wear Motif frames: a thick raised border, the menu button at the left of the
  title, minimise and maximise at the right, and the title centred on a strip that takes the active colour
  only for the window in front. Buttons, wells, tabs and lists are all the same grey told apart by light and
  shade, read from a palette of eight colours, and the backdrop is a hatch in two of them. Where the others
  have a taskbar it has the Front Panel, a raised slab at the bottom centre: a clock with hands that tell the
  world's time, the day of the world on a calendar page, the File Manager, the Text Editor, the four workspaces
  with EXIT beside them, the Style Manager, and the Applications control, whose arrow raises a subpanel listing
  every program on the machine. It lists no open windows, because CDE never did. The right button on the slab
  opens the panel's own menu, where the Task Manager is.
- CDE's four workspaces are real. A program opens on the workspace that is up and stays there; pressing
  One, Two, Three or Four on the Front Panel shows only what belongs to that workspace, and the rest stay
  exactly as they were left. Which workspace each window is on, and which one is up, are the machine's own
  state like the windows themselves: the next person to look at that monitor finds the same workspace up, a
  saved world brings it back, and a machine that is switched off or restarted comes up again on the first.
  A window that is not on the workspace that is up is not drawn at all.
- A window minimised on CDE stands as an icon at the top left of its workspace, the program's picture on a
  raised tile with the window's name under it, and a double click on the icon brings the window back. CDE has
  no list of open windows, so this is how a window that was put away is found again. What is kept on the
  desktop itself (the programs' icons, the files of the Desktop folder) is laid out from the right edge on CDE,
  as CDE did, which leaves the top left to those icons.
- The button at the left of a CDE title bar opens the window's menu, as it does on Motif, and a double click on
  it closes the window. The menu lists what can really be done: Restore, Minimize, Maximize, Lower (send the
  window behind the others), Occupy Workspace, Occupy All Workspaces and Close. Alt+F5, Alt+F9 and Alt+F10
  restore, minimise and maximise the window that has the keyboard. The right button on the icon of a minimised
  window raises the same menu.
- Occupy Workspace asks which of the four workspaces a window is on, each with a box to tick, so a window can be
  moved to another workspace or kept on several at once; the last box cannot be cleared, since a window on no
  workspace could not be found again. Which workspaces a window is on is kept with the machine like the rest.
- EXIT on CDE's Front Panel asks the way CDE did: it says how many programs are still open on the workstation,
  across all four workspaces, warns that what is not saved will be lost, and offers Shut Down, Restart and
  Cancel. There is no logging out, since nobody logs in. Enter takes Shut Down, and a click beside the buttons
  answers nothing.
- CDE's Application Manager. The Applications control on the Front Panel opens it: every program on the machine
  sorted into Desktop_Apps, Desktop_Tools, Network and Games, so nobody has to know a program's name to find it.
  A double click on a group opens it in a window of its own and a double click on a program starts it. A group
  with nothing in it is not drawn, and a program installed while a window is open appears in it at once.
- Three controls of CDE's Front Panel carry a subpanel, raised by the arrow at their head: Files (Home, Desktop
  and each medium in one of the machine's drives, each opening a File Manager there), the Text Editor (Personal
  Applications: Text Editor, Terminal, Calculator) and Applications (the Application Manager, the Performance
  Meter and Workstation Info). A subpanel lists only what the machine has, starts what is chosen on it and stays
  up, and goes down when its arrow is pressed again or another arrow raises its own.
- CDE's Terminal is a Unix terminal: it opens on the shell's own prompt, `player@host:~ $`, and greets nobody.
  A desktop now says for itself whether it stands on a Unix family, so the next desktop cannot be left off a
  list the way CDE was.
- CDE runs on the Linux distributions too, as it can on a real one today: each distribution takes it from the
  Mirror with its own package manager, `apt install cde` on Debian and Ubuntu and the same word to the others,
  beside the desktops it already could. Under the Front Panel the machine is still that distribution, with its
  own shell, its own tree and the player's home in `/home/player`. Gentoo builds it from source in the real
  order: Motif, the Korn shell, then CDE.
- CDE has icons of its own, drawn the way CDE's were, with a black keyline, Motif's greys and few colours: a
  filing cabinet for the File Manager, a written sheet and pencil for the Text Editor, a terminal with cream glass,
  the Calculator, the Performance Meter's rising bars, a painter's palette for the Style Manager, a workstation
  for Workstation Info and four coloured squares for the Application Manager. The network programs keep their
  shapes in CDE's colours, and the CDE package has an icon of its own on every desktop.
- CDE's Front Panel names a control after the pointer has rested on it for half a second, in a small raised
  plate just above the panel: Clock, Calendar, File Manager, Text Editor, Style Manager, Applications, Trash
  Can. The panel is pictures and nothing else, so this is how a player learns what each one opens.
- Workstation Info, CDE's window about the machine, on the third line of the Applications subpanel and in the
  Application Manager's Desktop_Tools. In three wells it shows who is at the workstation, its host name and the
  id of the data network it is on (or that it is on none); the system with its release, the architecture and
  CDE's version; and the processor, the memory, the memory in use with a meter beside it, the video memory and
  the system disk. It asks the machine again every two seconds while it is open, so the memory in use stays
  current. Only CDE bundles it, on every system that takes CDE.
- CDE's Style Manager. The Style control on the Front Panel opens it where the other desktops have their
  settings: a strip of two pages. Color lists the eight palettes under the names they had, shows the colours of
  the one picked, and puts it on the whole desktop the moment it is picked (frames, the active title, the Front
  Panel and the backdrop); OK keeps it and Cancel puts the old one back. Backdrop gives the workspace that is up
  one of six patterns (Hatch, Pinstripe, Tiles, Weave, Dots or Plain) in the palette's own backdrop colours, so
  the four workspaces can look different; each starts with a different one. The choice is kept with the machine
  and CDE's loading screen already wears it. Everything else a machine keeps about itself is set at its prompt
  with `config`.
- CDE comes up behind a screen of its own, as the Linux desktops do. With no login to show, the console's lines
  are followed by a raised plate with the desktop's name and its maker's over the backdrop the desktop is about
  to stand on, and a line naming the workstation it is starting on, which is the host name its prompt says.
- CDE's Terminal window is paper, as a workstation's was: a warm white ground written on in dark inks, each of
  the console's colours (success, error, warning, the hints) given an ink that reads on it. A terminal panel
  inside an editor keeps its dark glass.
- The trash. A file or folder deleted on a desktop is no longer gone for good: it waits in that desktop's trash,
  still taking its room on the disk, until it is put back or the trash is emptied. Each family keeps it its own
  way and under its own name: the Recycle Bin on Frames, a `RECYCLER` folder at the root of the disk with an
  `INFO2` index of where each file came from; the Trash on the Linux and FreeBSD desktops, in
  `~/.local/share/Trash` with the files under `files` and a note for each under `info`; and the Trash Can on CDE,
  in `~/.dt/Trash`, on whichever system runs it. `rm` and `del` at a prompt still delete for good, as they do on
  the real systems.
- The trash is the first icon on every desktop, with a picture for empty and one for full drawn for every desktop
  and period, and a place in the file manager's sidebar. On CDE it is a control at the right end of the Front
  Panel instead. A file dropped on any of them is deleted, and the icon's own menu empties the trash without
  opening it.
- The trash opens in the look of its desktop's file manager. On Frames it is the explorer with the Recycle Bin's
  tasks and details down the side and a list giving each file's name, original location and size. On KDE
  Plasma, GNOME and Cinnamon it is a bar with Empty Trash over the places and the files as icons, each file's
  menu in its own desktop's words (Restore to Former Location in Dolphin). On CDE it is the Trash Can, with its
  File, Selected and View menus and CDE's own Put Back and Shred. Restoring puts a thing back where it came
  from, making again any folder on the way that has gone since. Emptying the trash, or deleting something in it
  for good, asks first.
- A file on a disc in a drive or on another machine's share has no trash to go to: deleting it asks first, in a
  Confirm File Delete question, and then deletes it for good, as Windows did.
- Escape puts the power dialog away on every desktop instead of closing the monitor behind it, and no key
  reaches a program while that dialog is up.
- The File Manager and the file dialogs open a desktop's Desktop folder and Home where the system really
  keeps them, which is under `/usr/player` on UNIX and not under `/home/player`.
- `installpkg`, the way UNIX installs a program: put the medium the package came on in a linked drive and say
  `installpkg`. With one medium in it takes what that medium carries; with several it lists them and asks to be
  told which by name, and with none it says so. The medium itself is met under `/mnt` by its drive letter. A
  program that runs on UNIX says `installpkg` among the ways of installing it on its medium.
- UNIX starts and stops in very few words. It signs itself, counts the machine's memory in bytes and says how
  much is left after what it keeps, checks its root filesystem and says it is ready; the console then signs
  the player in as `desk Console Login: player` and says where its help is. Stopping, it warns the console in
  capitals, changes run level, 0 to stop and 6 to come straight back, and ends on "The system is down.". It
  never had a boot menu and has none here.
- MC-DOS has files of its own. The root of its disk holds `MCDOS.SYS`, `COMMAND.COM`, `CONFIG.SYS` and
  `AUTOEXEC.BAT`, with the system's tools in a `DOS` directory, and `type` reads the ones that are text. A program
  installed on it gets a directory of its own at the root, named in capitals the way that filesystem named
  things, with its executable and its notes inside, and the `PATH` line of `AUTOEXEC.BAT` names exactly the
  directories of what is installed. Like the system files of the other families they are generated from the
  machine and cannot be edited or deleted. `dir` at the root of a fresh MC-DOS used to say File Not Found.
- FreeBSD's boot loader. After the self-test the machine stops at a ruled box headed "Welcome to FreeBSD" with
  FreeBSD's mark beside it, the red sphere with its two horns over the name, counting down to a boot on every
  start. It lists only what it can do here: `Boot`, which is what Enter does, `Reboot`, which runs the self-test
  again, and the firmware settings on the machines whose firmware is reached that way. An entry is chosen by its
  number, and any other key stops the count. The setting that hides GRUB's menu hides this one too.
- `screenfetch` on FreeBSD draws FreeBSD's mark, the sphere with its two horns, in red, and reports the system
  in FreeBSD's words: the architecture as `vel64` or `IA-32`, the packages counted by `pkg`, and a machine with
  no desktop at `ttyv0`.
- `uname` takes `-s`, `-n`, `-r` and `-m` as well as `-a`, alone or together (`uname -sr`), and answers for the
  system it runs on: a Linux says `Linux` and `x86_64` or `i686`, FreeBSD says `FreeBSD` and `vel64` or `IA-32`.

- CDE has its Help Viewer, and the Front Panel has the control that opens it. What it shows is not a second
  body of text: it is the very manual pages `man` prints at a terminal, asked of the machine through the same
  filter that decides what that machine can run, so the Help never teaches a command that is not there. The
  list on the left is grouped by what a person would be looking for (The Network, Files, Software, Writing
  programs, Finding your way, The Machine), typing in the Search field narrows it the way `apropos` does, and
  Backtrack is greyed until there is somewhere to go back to.
- `less` is a real pager: it takes the terminal and shows a file a page at a time, with Space and Page Down
  going on, `b` and Page Up going back, the arrows a line, `/` looking for something, `n` finding the next one
  and `q` giving the terminal back, and a line at the foot saying how far through it you are. `MORE` on the
  DOS family is the same pager under its own name. A program holding the whole glass is now handed the cell
  that was clicked, so the pointer works inside one the way it does everywhere else.
- A system is a real thing on a real disk, and a machine can be wrecked. Installing writes the file that
  starts the system, and nothing protects it: delete it and the machine carries on, because the system is
  already in memory, exactly as a real one would. The reckoning comes at the next start, and what it costs
  depends on what is missing. The whole system folder gone leaves nothing to find, so the firmware looks
  elsewhere as it would at an empty disk. The file that starts it gone leaves a system that is found and will
  not run, and the machine says so in its own family's words: `kickmgr is missing` on the Frames, `Bad or
  missing command interpreter` on MC-DOS, `kernel panic - not syncing: no init found` on the Unix systems.
  Installing over it puts the system back and leaves the player's own files where they are; only formatting
  takes those.
- A computer can be left with work and it carries on with nobody at it. On the Unix systems a line ending in
  `&` is left running and the prompt comes straight back, `jobs` lists what the machine is doing, `kill %1`
  stops one, and `crontab 06:00 <command>` leaves a line for an hour of the world's own day, or for the days
  named (`crontab 18:00 M,W,F ...`). On the DOS family the same list under its own words: `START <command>`,
  `AT hh:mm [/EVERY:M,W,F] <command>`, `AT` alone to see what the computer is set to do, and `AT <id> /DELETE`
  to take one off. A job costs the machine a megabyte while it has it, so how many a computer can be left with
  is its memory's answer and not a number anybody invented, and what it was left with goes with it through a
  save. The shortest anything repeats is an hour of the world's clock, a little under a minute of real time.
- The small tools a person reaches for without thinking. On the Unix systems: `ps` and `kill` for what is
  running and stopping one, `which` for where a command came from, `du` for what the files here take, and
  `date` for the world's own clock. On the DOS family, the same answers under its own names: `TASKLIST`,
  `TASKKILL /PID`, `WHERE`, `MEM` (which now says what the memory really holds as well as what it promised),
  `DATE`, `TIME` and `TREE`. A process is listed with the file it was started from, since a program that gave
  itself no name is listed under the runtime that runs it and the file is what tells two of them apart.
- Commands have real manual pages now, and there is one body of text about each of them. A page has NAME,
  SYNOPSIS, DESCRIPTION, OPTIONS, EXAMPLES and SEE ALSO, written by the command itself, and every way of
  asking reads that same page: `man` at a Unix prompt, `help <command>` anywhere, `<command> --help`, and
  `<command> /?` on the DOS family, which answers it in its own voice. `whatis` says a command in one line and
  `apropos` searches what every command on this machine is for, which is how to find one whose name you do not
  know. None of them will teach a command the computer in front of you cannot run.
- `listcmd` lists absolutely everything a computer can run right now, commands and installed programs
  together, whichever system it runs. It is off until a server turns it on (`list_commands` in the server
  config): off it is nowhere at all, not in help, not in a manual, and typing it is an unknown command, so a
  world that wants each system's own way of teaching keeps it.
- A prompt now reads a whole line the way a shell does. Commands can be joined with `|`, each handed what the
  one before it printed; `>` puts what came out into a file and `>>` adds to it; `<` feeds the first command a
  file. A name stands for something before the command sees it (`$HOME` and `$HOSTNAME` on the Unix systems,
  `%CD%` and `%COMPUTERNAME%` on the DOS ones), and on the Unix systems a word with a star in it is opened out
  into the names it matches, as that family's shell has always done, while the DOS family still hands the star
  to the command, as that one always did. A live medium's installer is left alone: there the arrow is part of
  the step being taught.
- The tools a pipe is for: `grep`, `wc`, `head`, `tail` and `sort` on the Unix systems, and `FIND`, `SORT` and
  `MORE` on the DOS ones, all of them reading what a pipe hands them or the file they are named. `cat` now
  reads every file it is given rather than the first.
- `interac` works the data network from any prompt, without writing a line of IQL. `interac` on its own says
  whose network it is, whether a Mainframe is orchestrating it and what it holds; `list` shows what is in
  there and narrows to a word; `where` names the servers holding a thing; `info` says what it is, how much
  there is, what makes it and what it goes into; `craft` asks for some to be made; and `ops`, `cancel`,
  `lock`, `unlock`, `locks` and `stats` are the network's work and holds. It comes with every system that has
  a prompt and a cable, with nothing to install, and takes its words either way its family writes them:
  `interac list stone --sort count` on a Unix system, `INTERAC LIST STONE /S:COUNT` on a DOS one, in any case,
  with `INTERAC /?` for what it does. A verb may also be written as an option, `interac --where diamond`.
- `interac get 42 cobblestone` takes items out of the network and into your own hands, through the computer
  you are typing at, the way the graphical program does it; whatever your inventory has no room for stays in
  the computer rather than being lost. `--to local` (`/LOCAL`) leaves it all in the computer instead, and that
  is the only way to ask from a session opened on another machine, where nobody is standing at the keyboard.
  `interac put hand` hands over what you are holding, `interac fill water` fills a held bucket from the
  network, and `interac fav` stars a thing on this computer, the same stars the Network Interactor shows.
- A thing is named the way a player says it: `cobblestone`, `minecraft:cobblestone` or `"oak log"` all find
  it, and what the network is holding is looked at before the registry of everything there is. A name that
  fits several things is answered with the several and how much of each there is, rather than guessed at.
- Text can be picked out of a terminal with the pointer and copied. Drag across what a machine printed and it
  is picked out; a second click takes the word under the pointer and a third the whole row. Ctrl+C copies what
  is picked out to the clipboard, and with nothing picked out it is still the interrupt it has always been.
  Ctrl+V types what is on the clipboard, and a paste of several lines runs them one after another, up to
  sixteen of them. It works the same at a machine's own prompt and in a terminal window on a desktop, and a
  copied row is cut at what it says rather than padded out to the width of the glass, so a listing pastes as a
  listing. At the prompt, Shift with the arrows picks out what is being typed.

### Changed
- The network's loose verbs are words of `interac` now. `find`, `lock`, `unlock`, `locks`, `ops`, `cancel` and
  `stats` are gone as commands of their own and are `interac where`, `interac lock` and the rest, so there is
  one program for the network at a prompt just as there is one on a desktop. The name `find` goes back to
  meaning what it means everywhere else, text in files. IQL is `iql` now, and still answers to `operation`,
  `op` and `sql`.
- A computer's memory now says what is really in it. A running program was listed by the room it had been
  promised, a number that never moved however much the program went on to hold, so nothing a program did
  showed anywhere. Each thing now carries both sizes: the room it was given, which is what says whether one
  more program fits and is still what a machine runs out of, and what it is holding this moment, which is what
  the Task Manager's list, its memory meter and its history, and the System Monitor now show. A program filling
  memory finally draws a rising line.
- A service holds memory while it is running and not while it merely sits on the disk. Stopping the IQL Engine
  gives its memory back, and a language's runtime is in memory while it has a program to run; installing one
  costs nothing until it does.
- A prompt now offers only what the computer in front of you can actually do. Every command says which systems
  have it and what the machine must have for it to be there: a filesystem, a network under it, ports for
  peripherals, a package installed, a Mainframe or a Cluster Management Computer. So MC-NET, which keeps no
  files, no longer lists `dir` and `copy`; a machine off the network no longer offers to search the storage or
  hold items; `cls` and `ver` belong to the DOS-speaking systems and are gone from the Unix ones; the package
  manager of the Frames family is offered where that family is; and the upkeep of the storage index, the IQL
  Engine, the Mirror and the network's services are worked from the Mainframe that runs them, not from any
  computer that can reach it. Everything that shows commands to a player reads that same answer, so a machine
  never names something it cannot run.
- A terminal is now a grid. Every character sits in a cell of its own, all the cells the same width, on the
  prompt that fills a monitor and in the terminal window on a desktop alike. The game's letters are as wide as
  they need to be, so a column of figures never lined up under another and a bar made of one character came
  out a different length from the same bar made of another; on a grid a listing lines up, a bar holds still
  while it fills, and the right-hand edge of a status column is an edge. A monitor's terminal is eighty
  columns wide, and what runs at it lays its output out to that. The line being typed is in the same cells as
  everything above it, and a question wider than the glass carries on at the start of the next row with the
  answer typed after it.
- Everything a machine shows on a monitor is one size. The terminal, the setup, the boot manager, a system
  reading its start out, the installers and the desktop all fill the same glass, and a terminal writes at
  three quarters of the game's font the way a desktop does. The terminal used to be a smaller screen than the
  desktop it led to, written at full size.
- A live medium's prompt has the colours of the real one: Gentoo's `livecd` in red with the path and the `#`
  in blue, Arch's `root` in red with `@archiso ~ #` in white. A prompt is a coloured line the machine sends
  like any other, and a terminal that has just opened shows the machine's own prompt from the first frame
  rather than a guess at it.
- Text at a terminal and in the editors that run at one has a shadow under it, and the shadow is worked out
  from the colour of the letter and the colour of what it is written on: the letter's own colour most of the
  way to the ground's. It is never the letter's colour, never the ground's, and never a colour foreign to the
  letter, so a red word has a dim red under it on a black glass and a dark word would have a pale tone of
  itself under it on a light panel.
- The Gentoo stage 3 is `stage3-vel64`, for the Velocion processors these machines have.
- What a build prints is a Sigma toolchain at work. `emerge`, the kernel build and the archives name `scc`
  compiling `.sg` sources into `.asm`, check for namespaces where they checked for headers, and unpack
  `libsigma` and `scc-libs`. There is no C in this world, and no tool pretends there is.
- A line at a terminal can be coloured in parts, the way real tools colour theirs: an arrow in green before
  plain text, brackets in one colour round a word in another. Any command can write one.
- The tools of a by-hand Arch or Gentoo install now print what the real ones print. `mke2fs` reports the block
  count, the inode count, where the superblock backups landed and how big the journal is, all worked out from
  the disk by the rules the real tool uses rather than written down; `pacstrap` and `pacman` resolve, list what
  they are about to install with its versions and its two totals, ask, retrieve a package at a time and run the
  post-transaction hooks; `emerge` announces every phase it passes through; `genkernel` builds the kernel and
  then the boot image and disclaims the result the way it really does; `fdisk`, `mkfs.fat`, `mkinitcpio`,
  `grub-mkconfig` and `lsblk` likewise. `mount` says nothing, because it does not.
- The Gentoo sequence now fetches the stage 3 and unpacks it as two steps, `wget` then `tar`, which is how the
  handbook has it and what makes the fetcher's counting and the archiver's list of paths two different things
  to watch.
- The steps of a by-hand install that take time now hold the terminal while they do, and what they print
  arrives while they run. `mkfs.ext4` writes its inode tables as a counter that climbs, `wget` fills its bar,
  `tar xpvf` names every path it lays out, `pacstrap` and `pacman` retrieve a package at a time, `emerge` lists
  what it would merge, asks, and goes through its phases with the compiler's lines going by, and the kernel
  builds for as long as the machine takes over it. Each does what it is for when it ends, so a step stopped
  with Ctrl+C has not happened.
- How long a by-hand step takes is the machine's doing. A fetch is as long as the file is big over the
  connection the machine has, making a filesystem and unpacking onto it go at the disk's speed, and compiling
  is a fixed amount of work got through at the rate the processor manages with as many of its cores as
  `MAKEOPTS` lets it use, never more than it has. A build left alone uses one core.
- `emerge` on an installed Gentoo is the same tool it is in the installer. It takes the terminal, works out
  the dependencies, lists what it would merge and asks when it was told `--ask`, fetches the source, and goes
  through every phase with the compiler's lines going by, and the program is on the machine when the build ends
  and not before. It used to print one line naming how many seconds were left and finish out of sight.
  Ctrl+C stops a build and installs nothing. The build options the system was installed with come with it, so
  `MAKEOPTS="-j4"` in `/etc/portage/make.conf` makes every later build four jobs wide, up to the cores the
  machine has. `emerge --status` and the notice of a build finished in the background are gone, since nothing
  builds in the background any more.
- A desktop can be asked for on Gentoo the way its package tree really names it, `emerge --ask
  kde-plasma/plasma-meta`, `gnome-base/gnome` or `gnome-extra/cinnamon`, and merging one builds everything it
  is made of, the toolkit first and the desktop itself last, each package through every phase. The short
  names (`kde-plasma`, `gnome`, `cinnamon`) still work.
- The walkthrough a live medium carries in `/root/install.txt` is laid out for the terminal it is read on: a
  step on a line of its own and what it is for underneath. Its longer lines used to be broken in two
  wherever the edge of the terminal fell, commands included.
- `wget` fetches from `mirror://mainframe`, the one thing on a world's network there is to fetch from, and
  stamps what it fetched with the world's own clock: the day the world is on and the time of that day
  (`--Day 214 12:00:00--`). A machine with no Mirror on its network is told the host could not be resolved.
- Building a Gentoo kernel now takes the time it really takes, and getting the sources takes the time getting
  sources takes. It was the other way round.
- A machine's filesystems are named by real identifiers in the table it writes and in the bootloader's list,
  the root's a full one and the boot partition's the four-byte serial that filesystem has room for.
- Everything a machine writes on its own glass is drawn at three quarters of the game's font: the self-test,
  the boot manager's list, a system reading its start out, and the installers that ran in text. Those machines
  fitted a screenful of text on a screen this size, and at full size a self-test that listed a board, a video
  card and four drives ran off the bottom of the monitor.
- systemd and OpenRC print the way they really print. The kernel stamps each of its lines with the time it
  happened, systemd marks every target it reaches, and OpenRC uses its own star and its own column, in its own
  words: it stops services and brings interfaces up, where it used to borrow systemd's sentences and read as
  the same system in another colour.
- MC-DOS and MC-NET read out what those systems really read out: the drivers naming themselves as they load,
  the memory above the line, the drive letters with what is on them, and, on the network machines, the link,
  the Mainframe, the index, the storage and the services it runs, each dotted across to its answer. A network
  with nobody answering says so in words rather than printing a column of "skipped".
- Every installer's copy page is its own again. One put a single bar in a box and named the drive it was
  reading, another dotted every step across to a "done", another had one gauge and a figure on it. Drawing one
  list and one bar for all of them made the most-watched minute of every installation the same minute.
- The server installer wears its heading in its own coloured band across the top, with its help at the other
  end of it and its two buttons written out at the foot, which is where that installer put all three.
- Each firmware's one-time boot menu is drawn the way that firmware drew one: a double-ruled box on the
  earliest tube in the tube's own green, the setup's blue box with a grey title bar on the boards after it, and
  a dialog on the newest machines with a mark beside each entry saying whether it boots a system or installs
  one. All three were the same blue box before, which on a green-phosphor monitor was a box of a colour that
  monitor could not show.
- The boot manager's list sits in a ruled box with its help underneath, as that manager drew it, and the entry
  the machine would boot by itself is marked.
- The newest Frames edition comes up on the same ground its firmware posted on, so the moment the firmware
  hands the machine over passes without a flash of a different black.
- Frames 95's bar runs rather than fills. That bar never told anybody how far along the load was: it was one
  band of colour running left to right and starting over, for as long as the machine took.
- Frames XP goes down on the blue bands it welcomed you on, with its mark to one side and the message beside
  it, and says a different thing when it is restarting. It used to go down on the black screen that only ever
  belonged to coming up.
- The firmware's version comes from one place: the setup's header now reads the same version the self-test
  that ran a moment earlier printed.
- The hardware page names the system on the boot disk beside the slot, since a slot number on a machine with a
  system on each disk says which disk the machine reaches for and not what it will get.
- The marks a machine puts on its glass are drawn at four times the size they are shown at and shrunk to fit.
  The game draws the whole interface at whatever scale the player chose, so a picture made at the size it
  occupies there is stretched two, three or four times before anybody sees it, and lettering stretched like
  that turns to mush. Made large and shrunk, it is sharp at every scale. The installer that shows the maker's
  name in its header now shows the mark itself there too, as the rest of that system does.
- The marks on a machine's screens are pictures now rather than words set in the game's own font: the maker's
  on every modern self-test and on the badge an older board wore, and each Frames edition's on its start, its
  shutdown and the installer that puts it there. A lockup is lettering with weights and a face of its own, and
  four filled squares with the name typed beside them were the words of it without the thing itself. They are
  drawn at the size they were made and never scaled, since lettering put through anything but a whole multiple
  comes out as a smear.
- Frames XP ends its start the way it did: the logo over its running trough, and then the blue ground with one
  word on it while the desktop is made ready.
- A disk carries as many systems as it has room for, rather than one. Installing a second used to write over
  the first, with nothing anywhere saying what had been lost; it is installed beside it now and becomes what
  the disk boots. Every system on a disk takes its own room, so a disk carrying two is charged for two, and
  each of them remembers having been met on its own, so a system installed beside one you have already seen
  still greets you the first time it comes up.
- The Frames editions have a boot manager of their own, the Midsoft Boot Manager, whose file on the disk is
  `kickmgr`. Only the Linux family had one, so a Frames edition installed beside another could not be chosen
  between. Each family speaks in its own words: GRUB names the device an installation sits on, the Midsoft one
  names the edition and its disk. A boot manager lists every system on every disk rather than one for each
  disk, and the Frames one stays out of the way with a single installation, as it did, appearing once there is
  a second system to choose between. What is picked there is for that boot only; what a disk boots by default
  is written on the disk.
- The Frames editions now come up behind the picture each of them really came up behind, in place of the one
  screen every system shared: the sky with the logo over it and the bar along its foot, the black one with
  the three blocks running through their trough and the small print at its feet, and the maker's mark over a
  turning ring of dots. Each of them goes down behind the same picture, saying what it is doing. Every other
  system goes on reading out its own start, which is what those really did, and a system an addon brings gets
  that too without having to say anything.
- How long a system takes to install is now read off the machine's own parts rather than off its generation.
  Three things decide it and a player chose all three: the medium it is read from, the disk it is written to,
  and the processor that unpacks it in between. It used to be the medium and the generation alone, so every
  machine of an age took exactly as long as every other one and nothing anybody put in a computer ever showed.
  The disk is the one still being chosen while the installer is open, so the time is worked out again the
  moment a different disk is picked: putting a solid-state disk in a Legacy machine really does cut the wait,
  and the page says so before the copy starts. The floor and the ceiling are where they were, three seconds
  and ninety.
- A program is now built for the oldest architecture that has what it turned out to need, rather than for a fixed
  one. The compiler reads the listing back for the instructions it actually used and stamps the oldest machine of
  the line that has all of them, so a program runs everywhere it could have run instead of only on the newest
  chip. `--arch` still forces one, and forcing is left alone even where an older one would have done. Today every
  program is x86, because the 32-bit and the 64-bit x86 have exactly the same instruction set; whatever a later
  update adds to the newer one is what will start moving programs up.
- `sgsc` now refuses to build for x86-16, naming `scc` instead. Those machines run the smaller language only, and
  the way that stays true is at the compiler: a listing they can load can only have come from a source they could
  have held. A Sigma Sharp project whose Platform target is x86-16 is told the same by the studio (`S4012`), where
  it used to build.
- A `using` line is offered `Standard` beside `System`, since both languages have it.
- Writing a method with the name and parameters of one a base class already has is now an error rather than a
  silent replacement. Either the one above is `virtual` and the new one wanted `override`, or it is not and the two
  are a collision; the message says which, and says what to add. There is no way to hide a base method. Which
  method runs has always been decided by what the object really is, and still is: what changed is that replacing
  one has to be written down, so nothing already compiled behaves differently.
- Compiling the Gentoo kernel now depends on the machine and on the player. The processor's cores and clock
  decide how much work it gets through, and how much of it happens at once comes from `MAKEOPTS` in
  `/etc/portage/make.conf`, capped at the cores the machine really has: left alone it builds one thing at a
  time, `-j4` on a four-core machine cuts the wait to a quarter, and asking for sixty-four of them on that same
  machine is still four. `echo` writes the line that sets it.
- How long an Operation waited and how long it ran are counted without a ceiling, so a craft left running for
  days still reads correctly instead of turning over.
- A mod built on this one now adds what it brings at one named moment, by listening for `CoreRegisterEvent` or
  `ComputersRegisterEvent`, and the two mods of the series add their own the same way rather than by a path of
  their own. After the loading is done every registry is closed, so what a world can install and knows how to do
  does not change under somebody playing it. Two things of the same id are refused rather than one quietly
  replacing the other. What a mod may use is gathered in `dev.jstech.core.api` and `dev.jstech.computers.api`, and
  `docs/API.md` says what is promised, what is not, and how long anything lives once it is marked as going.
  `JSComputersAPI` is now `JsComputersApi` in that package.
- The Start button carries its edition's own mark, four panes in that edition's colours, instead of the same
  four-coloured flag on all of them. It is the mark the setup, the installer and the screen the system comes up
  behind already wear, so a desktop now looks like the thing that installed it.
- The newest edition greets the machine by name the first time it comes up, in place of the maker's name and
  inside the same wait, so a first start says something different rather than taking longer.
- A system remembers having been met, and the mark rides on the disk it is installed on rather than on the
  machine. A computer with two systems therefore meets each of them once, a disk carried to another computer
  arrives already met, and erasing a disk and installing again is a first meeting all over. The advancement for
  booting a system for the first time used to fire on every boot; it now fires once, the first time that system
  comes up in front of somebody.
- Installing a system now goes through that system's own installer instead of one screen for all of them. It asks
  only what the game has: which disk, with what each disk already holds and the room the system needs; what the
  computer is called, which becomes the name the prompt and the network use; and, where a Mirror answers, a
  desktop to install along with it. A disk that already carries a system is erased first, after a confirmation
  naming what is lost, and the erasing happens when the copying starts, so leaving before that still leaves
  everything as it was. The installer belongs to the machine: closing the monitor does not cancel it, it is saved
  with the world, and opening the monitor again returns to the page it had reached.
- A machine running a Linux system now stops at its boot manager on the way up, listing every disk that carries a
  system, so the second one is something a player finds rather than something they have to know about. It boots the
  usual one by itself after five seconds; any key stops the count and the machine waits there. On the modern
  machines the firmware setup is one of the entries. Whatever is chosen there is for that start only. A new
  settings file beside the world save, `jscomputers-server.toml`, holds the switch that hides the menu.
- Switching a machine off now shows the system closing itself, on the systems that had such a screen. The earliest
  ones have none, as they had none in life: the monitor goes dark where it stands. A machine that goes down because
  its parts no longer make a computer says nothing either, since that is a plug coming out and not a shutdown.
- A Linux machine now reads out its kernel and its services as it starts: the architecture its processor really
  understands (i686 on the 32-bit machines, x86_64 on the others), the processor and the memory it found, a line
  per drive, and then the services this machine has, with the network only when a cable reaches one and a display
  manager only when a desktop is installed. Gentoo says it in OpenRC's words rather than systemd's.
- A starting system now says what it is finding, and every line of it is read off the machine: MC-DOS counts the
  memory above the line and gives a letter to each drive that is really in, naming it, and mentions a network only
  when a cable reaches one; MC-NET asks the network for the link, the Mainframe, the index and the storage, and
  says that nothing answered rather than opening on an empty list.
- A system now takes time to come up. Between the self-test ending and the desktop or the prompt opening, the
  monitor shows the system starting, for as long as that system's size, the disk it sits on and the machine's
  generation say it should: seconds from a solid-state drive, the better part of half a minute for a large system
  on a mechanical one. Like the self-test, it belongs to the machine: closing the monitor does not stop it, and
  opening one again joins it where it has got to.
- A Command Prompt window now opens with the name of the system it belongs to and that system's own copyright, the
  way the terminal on a machine with no desktop already did, instead of greeting the player with the mod's name.
- A self-test now ends by naming what it is about to boot ("Booting from Disk 0: Frames XP"), and a machine with
  nothing to boot ends on its own era's way of saying so and waits for a key, instead of dropping the player into
  the firmware setup without a word. A Legacy machine posts on black with its maker's badge, the way the boards of
  that time did; the setup it opens with DEL is still the blue one.
- The power-on self-test and the firmware's hardware page now show the machine that is actually there: the name its
  owner gave it, the processor by model with its cores and its architecture, the memory in megabytes with the slots
  it fills, the board, the video card, and the monitors really linked rather than a "connected" that was always
  true. No screen inside the fiction names the mod any more: the maker is JSC Technologies and the firmware version
  comes from one place.
- Putting a system on a disk is now the machine's work rather than the screen's. It takes as long as the system is
  big and the medium is slow, on the same rule a program's setup follows, where it used to be three and a half
  seconds for every system on every machine. Closing the monitor no longer throws the install away: it carries on,
  it is saved with the machine, and opening the monitor again joins it where it has got to. Taking the medium out
  part-way through stops it with nothing written, and a machine that cannot take the system says so before the copy
  starts instead of after it.
- The power-on self-test now belongs to the machine instead of to the screen watching it. It takes as long as the
  machine gives it reason to, growing with the memory and the devices seated and shrinking with the era, where it
  used to be three and a half seconds on every computer. Closing the monitor part-way through no longer stops the
  machine coming up or starts the test over: reopening shows what is left of it, and a machine nobody is watching
  boots all the same.
- A compiled listing now names the processor architecture it was built for, on a `.arch` line under the version, and
  the format's version is 3. A listing compiled before this still loads and runs as it did, and is read as having
  been built for the 32-bit machines.
- A machine now refuses a program built for an architecture its processor does not run, naming both (`A4015`, "built
  for x86-64; this machine is x86"). The 64-bit machines run everything written for the 32-bit ones, as they do in
  life; the earliest machines run only their own.
- A processor's tooltip now names its architecture, "x86-64, 64-bit", where it used to give the word size alone, and
  This PC, System Monitor and Settings say what the machine in front of you is built on.
- A computer now settles whether its installed parts can run it when those parts change, instead of working it out
  again for every machine on every tick.
- A computer now sends one player only so much in a tick, and whatever is left over goes first on the next one, so
  that a machine whose windows all changed at once, or one whose desktop has just been opened onto a full canvas,
  no longer puts the lot on the wire in a single tick. A window that has closed is always told of at once.
- A canvas now sends only what has been drawn on it since the last time it went over, instead of its whole picture
  again whenever anything in its window changes. Clearing one sends it whole again.
- A running computer now asks after the cable it is already attached to, one block, instead of looking round all six
  of its sides every tick and building a list of them to do it.
- A computer whose desktop somebody is watching now writes a window out for the wire only when something in it has
  moved. It used to write every open window out in full each tick and compare all of it with what it had last sent,
  so a desktop sitting still cost the server the same as one being used.
- The programming language is now called Σ# (Sigma Sharp). Its sources end in `.sgs` and its projects in
  `.sgsproj`; `sgsc` compiles, `sigma run` runs, and `sgpack` packs a program for the Mirror. The programs to
  install are the Σ# Compiler and the Sigma Runtime, from the Sigma Foundation. The compiler's error codes start
  with S (`S2001`) and a listing's with A (`A4012`), their numbers unchanged. Files saved as `.can` and projects
  as `.canproj` are no longer taken for programs; renaming them brings them back. For addon authors and for
  project files, the two languages are registered as `jsc:sigma` (Sigma) and `jsc:sigma_sharp` (Sigma Sharp); a
  project file written before this names Sigma Sharp as `jsc:sigma` on its `language:` line and has to be told
  `jsc:sigma_sharp`.
- A listing that calls, makes or reads something no computer has is refused before it starts, where it used to start
  and stop at that line: the terminal says what nothing answers and on which line (`A4013`), or that the listing
  writes a value that can only be read (`A4014`). A listing the machine cannot read at all now says why, rather than
  only that the file is not a Σ# program, and a program saved running from a refused listing does not come back.
- The strip under an editor's completion list now prices every call of the library and the machine, the free ones
  included, and the Gateway's and the windows' calls as well. A call written several ways, such as `Gateway.Call`,
  shows the price of the way the list is on, and a value a program may write says what writing it costs.
- A program pays for the size of the files it reads and writes. `File.Read` and `File.TryRead` cost 50 plus one for
  every 4 KB they read, and `File.Write` and `File.Append` 100 plus two for every 4 KB they write, where each used to
  cost the same for any size. `File.Append` adds to the end of a file without reading it first and is priced on what it
  adds; it used to read the whole file back and write all of it again.
- Every call a program makes to the machine costs one instruction more than it did. The price a call is given is now
  what it costs on top of the instruction that makes it, for the machine's calls as it already was for a program's
  own, so a price in the strip under an editor's completion list means the same thing for every call.
- A compiled `.asm` listing belongs to the computers rather than to Σ#: the machines run listings themselves, and Σ#
  only compiles. For addon authors, a language may now only compile (no binary extensions, and `start` and `restore`
  left alone): the machine runs the listing it compiles to, and compiles a source file on the way in when one is
  run. No language may claim `.asm`; `LanguageRegistry.reserve` keeps an extension back from every language, and
  `sourceOf` finds the language a file is written in. A listing opened in a terminal editor is coloured the way the
  desktop editors colour it.
- For addon authors, a language that runs its own files is handed a view of the machine: `start(binary, view,
  arguments)` and `restore(binary, saved, view)` take an `IMachineView` in place of the block entity and the memory
  size. The view tells the machine's time and how much memory the program may hold, and takes the lines the program
  prints, which the machine keeps; a language's processes no longer report `console()`, `written()` or `heapBytes()`.
- For addon authors, `ICliComputer`, the computer a shell command is handed, is made of one smaller interface for each
  thing a command reaches: `ICliMachine`, `ICliFiles`, `ICliNetwork`, `ICliOperations`, `ICliPackages`,
  `ICliInstallation`, `ICliConfig`, `ICliRemote` and `ICliProcesses`. Every member answers as a computer without that
  part does unless the computer answers it, so a computer made to test a command writes only what the command uses.
- A computer saves its running programs in a new form: a listing several programs run is saved once, and a program in
  an addon's language is saved with the language's id and the version of what it wrote. A program whose language is
  no longer installed is left out alone, and a save the computer cannot read brings no program back, which its
  terminal says the next time it is used. Programs running in a world saved before this version do not come back.
  For addon authors, `restore` is given the `stateVersion()` the program was saved with.
- Programs run faster. What a program's calls, branches and `new` reach is worked out once, when the program
  loads, instead of on every line it runs, and so is which of the language's own functions (text, `List`,
  `Map`, `Math`, `Convert`) a call means. Depending on what a program does, each instruction takes between 13
  and 34 percent less time.
- Compiled listings use format version 2. A listing compiled by 0.3.0a is refused with A4012; compiling its
  source again brings it up to date.
- `Random` draws from a generator of its own, so a given `Random.Seed` draws different numbers than it did in
  0.3.0a.
- A program keeps at most 256 calls waiting for their turn, holding at most 64 KB between them. A click or a watch
  alert that finds no room is dropped and counted in `Program.DroppedEvents`; a message that finds no room is
  refused, and `Process.Send` returns false. Closing a window and stopping a script always get in, ahead of
  everything already waiting.
- A program's windows count against its memory as they grow: rows added to a list, strokes drawn on a canvas and
  widgets placed by hand. Clearing a canvas or a list gives that memory back, and a text box keeps only its latest
  text however much a player types.
- Saves, network packets and menu data carry each setting by an id of its own instead of by its place in a
  list, so adding a setting never changes what an old one means. Machine states that 0.3.0a worlds stored the
  old way are not read back.
- Programs that were running when a world was saved by an earlier version do not carry on when it loads: they are
  left out, with a line in the server log, and can be started again.
- The programs a computer stops in one tick share 4,096 instructions of farewell (`OnDestroy`) between them, where
  each used to get 4,096 of its own. A computer switched off or broken shares that budget evenly among its programs,
  and a program stopped after the tick's budget is spent is stopped without its farewell.
- A computer checks the server's clock once every 512 instructions its programs run, or at once after a call into the
  machine, instead of after every 64. A computer that runs out of time in a tick can go that much further before it
  stops, and running its programs costs the server less.
- A file can have any extension. One of a kind the computer does not know, such as `thing.fk` written by a
  program, is saved, copied, moved and renamed like any other. The prompt, programs and the editor used to refuse
  it, and copying or renaming a file to such a name turned it into a text file.
- A language an addon registers is refused, with a line in the log, when it claims a file extension another
  language already has. Registering one again under the same id replaces the one before, and the log says so.
  Languages can be added or removed only while the game loads.
- The handlers behind the computers' screens and programs are split into packages by feature, with nothing changing
  in how they behave.
- The virtual machine that runs programs lives in packages of its own, with nothing of Minecraft in them, and reads
  what is wrong with a listing without the compiler.
- A running program is made of parts that each do one job: who it is, its console, its random numbers, the lines
  typed at it, the calls waiting their turn, its windows, its watches, who it tells when something is said to it,
  which of its threads runs next, the locks they hold, comparing its values, checking their types, reading and
  writing its fields, making its calls and its objects, carrying out its instructions, saving it and reading it
  back, and turning what is said to it into calls waiting their turn. Nothing a program does changes.
- Each of a program's threads holds what it is waiting for as one value. Every change to a window or a widget goes
  through one place, a window marks each change to what it shows and a canvas each time it is cleared, and a
  program's memory takes in whatever the computer hands the program and checks what the program reaches into.
- A computer keeps the programs it runs in a table of its own, which knows no language and finds a program by its
  number without building anything. It starts every program in one place, keeps the one in front of its terminal
  apart from the rest, and decides in one place, for its terminal too, whether a program is still going; screens
  list a computer's programs without reaching into them.
- Running programs costs the server less. A computer looks up what its programs watch only when one of them watches
  something, as one count for each item; it knows who is looking at it from the screens they open and close, instead
  of going through every player on every tick; it looks for its Gateways only after one of them is spoken to, and
  hands its programs what a Gateway heard without copying their list; and it claims its share of each tick without
  building anything. A program waiting on another one is told when that one ends, or asks at most once a tick when
  the other is on another computer.
- The rate at which a processor's clock buys instructions is named beside the time the server lends a computer's
  programs.
- Each call the system answers says who answers it (the language, the program's own process or the computer) and
  what it costs, and the compiler takes the types of the language's library from those declarations, where they are
  written once.
- A program's calls are matched to the system's declarations when it loads, and answered through them: its console,
  its random numbers, its threads, what it says about itself, its waits on other programs, the calls on its windows
  and widgets and what they hold, the values it reads of itself, of the world's clock and of the language's own text
  and collections, the widgets, lists and maps it makes, the watches it sets on the network, its calls on the
  computer's drives, what it reads of the computer itself, its network and its Mainframe, the Operations it asks the
  network for, the IQL statements it runs, the programs it starts and asks after, on its own computer or another of
  its network, and its calls through the computer's Gateways, which services the computer keeps answer. A computer
  keeps the shell its programs reach its drives and its network through, instead of making one for each call.
- A computer installs programs in one place, whether the prompt or a program asks: which programs it has, installing
  one from the disc in a linked drive, and the manual installation a live medium walks through, which ends by writing
  the new system to the chosen disk and rebooting into it.
- A computer installs packages in one place, whether the prompt or a program asks: what its package manager is, what
  the network's Mirror can serve it, installing, removing, updating, and publishing a package of your own for the
  rest of the network to install. The Mirror itself is kept there too, so the list of the network's services has each
  row answered by whoever keeps that service.
- A computer speaks the network's own language in one place, whether the prompt, a program or the management studio
  asks: installing the engine on the Mainframe and starting or stopping it, and carrying out every statement that
  changes something, buses included. The engine that runs a statement is now handed only what it uses, which is one
  way to read and one way to act, instead of a whole computer.
- A computer asks its network for work in one place, whether the prompt or a program asks: pulling an item in, pushing
  one out, asking for a craft, holding an item where it is and letting it go again, listing what is in flight and
  stopping or hurrying one of them, and the Mainframe's own jobs on its index. What the Mainframe measured of that
  work is read beside it, from the Mainframe itself.
- A computer reads its data network in one place, whether the prompt or a program asks: whether it is on a network and
  which, what it holds and where, its servers, and the rows a query brings back about items, servers, operations,
  computers and recipes. The verb and the state of an Operation are now named by the record that carries them, so the
  prompt, the terminal and a program's queries all read them the same way.
- A computer reads and writes its drives in one place, whether the prompt or a program asks. The drives it can see are
  gathered from the machine itself, a path that leads to another machine of the network is followed by one resolver,
  and opening, listing, writing, moving and formatting all go through one service over both.
- Every source file's header names the mod it belongs to.

### Fixed
- A file cannot be given a path longer than the machine can then ask about. Each folder and file name was
  held to sixty-four characters and the path they built to nothing at all, so folders nested deeply enough
  made a file that existed on the disk and that no window could open, list, copy or pack away again, because
  every message that carries a path refuses a longer one outright. A whole path is now held to what those
  messages carry, and a thing put back out of the trash is numbered within that too.
- A service is listed as running and weighs what it asks for. Every installed service was looked up by the
  bare name of its program while every way of installing one writes down the whole name, so no service ever
  matched: none was listed on the machine it was on and none of the memory they hold was counted against it.
  Installing one from a package manager also only flipped its switch without recording it at all, and a
  package built from source was recorded without the version it was built at, which listed it as out of date
  the moment it finished building.
- The Patterns heading asks the machine once a second instead of once a frame. It asked for both halves of
  what it shows every time it drew, which is several messages a tick for as long as an answer is on its way
  and an unending run of them on a machine that never answers, which is any machine further than eight
  blocks from the monitor being looked at. It also no longer takes an answer meant for a desktop's Pattern
  Studio after its own screen has closed, keeps what is picked out pointing at a row that still exists when a
  list shrinks under it, turns only over the lists it can scroll, and no longer writes how many more files
  there are across the last row of them.
- A craft is planned once the number has stopped changing. Every digit typed into the quantity asked the
  machine for a fresh plan, and planning walks the whole recipe against everything the network holds, so
  asking for a thousand of something asked for four of them in the time it takes to type it; holding a step
  button did the same on every click. The machine is asked a tenth of a second after the last change.
- The question that asks the network for a thing is centred on the panel it actually draws rather than on a
  fixed height, so the short shape no longer sits high on the glass with a gap under it, and opening the
  question out no longer leaves the quantity field where the shorter panel had it.
- A terminal reopened on the Craft or the Patterns heading stays on it. The rail is built from what the
  machine says it offers and that answer arrives a tick or two behind the window, so the screen saw a rail
  without those headings on it and moved to the network before the machine had a chance to answer.
- A disk's name no longer has its own slider handle drawn through it: a disk offering nothing puts its handle
  at the left end of the track, which is exactly where its name is written. The two buttons under the panel
  beside the grid also no longer sit two pixels under the line above them.
- The rail and the pattern behind a picked-out thing are worked out when they change rather than several
  times in every frame, which is what building a list and walking the whole craft catalogue per draw was.
- Two add-ons registering an operating space at the same moment can no longer leave the table with neither of
  them in it. Client setup is handed to every mod at once, on as many threads as the loader cares to use.
- Asking the network for a thing works again. The field a quantity is typed into was built with the screen's
  own font at a moment when the screen has none, so the first thing that measured a string in it brought the
  game down the instant the question was opened.
- The inventory key is a letter at a prompt. Pressing it at a network machine's Console shut the machine's
  whole interface, because a key the prompt had no use for fell through to the game behind it. Every key
  belongs to the prompt while the prompt is what is showing, Escape excepted, which still closes.
- A network item shows how much of it there is again, in the Network Interactor on a desktop. The total was
  being drawn with the offset meant for the game's own item count, which cancels a lift that a plain label
  never applies, so every total landed two hundred deep behind the window it belonged to and only the
  tooltip could say the number. The star on a favourite and the availability mark on a craftable were lost
  in the same place.
- A terminal says what its network is holding whatever machine it is on. Only the orchestrator could answer
  the question, so a terminal on any other computer read "0 held" while showing a grid full of things. Every
  machine asks the orchestrator of the network it is on, which is where the index of what is where lives.
- A network machine's prompt greets with the name of the system it is on. It was read off the machine's own
  disks by the client, which is not holding them, so the prompt came up on a machine that could not say what
  it was running and greeted nobody. The name goes with the window now, from the side that knows it.
- The deposit button no longer sits on the rule above the player's own rows, and a disk's slider no longer
  has the next disk's name drawn across its handle: a disk's row needs twenty-one pixels and was given
  fourteen. The numbers behind both were written out in two files that had drifted apart, and are one set
  now, with the arithmetic held to account by the layout's own tests.
- A wrecked machine really stays wrecked. One whose system file had been deleted stopped at its self-test for
  a single tick and then started the system anyway, because the check that asks whether there is anywhere to
  go only looked for a machine with nothing installed at all, and a wrecked machine still has a system
  installed: that is the very reason it is found and refused. It now stays where it stands until there is
  somewhere to go, which is what putting an installation medium in gives it.
- A machine that will not start now says why. Standing at its failed self-test, it went on announcing
  "Booting from ..." over the top of its own refusal and then sat there for ever, because the screen decided
  what to show by asking the firmware whether it had found anything bootable, and a wrecked machine's disk
  still declares its system. A halted machine is now booting from nothing whatever the firmware made of the
  drives, so the failure reaches both the older screens and the modern one. A modern machine also said "No
  bootable device", which is true of an empty computer and a lie about a wrecked one whose device is right
  there; it now heads with what the machine actually found, in the words that family used.
- What a machine prints now fits the window it is read in. A terminal window on a desktop is narrower than a
  monitor, and the machine was writing to a monitor's width whatever it was talking to, so every wide line
  folded in half: a directory listing came out with a row of leader dots on its own under each name. The
  window now tells the machine how wide its glass is with every line.
- `dir`, `ls -l` and `df` print columns again instead of pushing their last value to the right edge with a
  run of dots between. The name goes last, where a name belongs, because it is the one column nothing can
  plan a width for, and the columns before it are as narrow as what they hold.

- A window is called what its desktop calls the program, on its title bar, on its panel button and on the cards
  the panel shows: Dolphin, Kate and KCalc on KDE Plasma, Nemo and xed on Cinnamon, the File Manager and the Text
  Editor on CDE. Their title bars said Files, Editor and Calculator on every desktop. A title a program writes
  for itself, such as a dialog's, is kept.
- The Save and Open dialogs of KDE Plasma, GNOME and Cinnamon show the machine's one tree from `/`, with Home and
  Desktop among their places. Only the two period desktops did; every other Linux desktop showed drive letters, a
  Local Disk (C:) and This PC, because the dialog told a Unix desktop by the look of its windows.
- A character joined to text reads as the character. A program that wrote `"<" + c + ">"` with `c` holding
  `'a'` printed `<97>`, the number the character runs as.
- Text a program prints with line breaks in it is printed as that many lines, and a break at the very end only
  ends the last one. A break used to be kept inside the line as a character no terminal knows how to show.
- A key pressed at a machine's screen goes to the machine before it goes to any other mod. With a recipe viewer
  installed, Ctrl+O at a terminal editor hid the viewer's overlay and never reached the editor, so a file could
  not be written. A desktop takes a key first only while something on it is there to use it, so the other
  mod's keys still work over a desktop with nothing open.
- Tab at a monitor's terminal goes round every command that starts with what was typed, one a press. It stopped
  at the first, because the second press completed the command the first press had just put on the line. It
  offers the machine's drives to `fdisk` and `mkfs.fat` as well.
- Booting a disk or a medium from the setup, or choosing a machine on a rack's switch, could leave the player
  at a screen the machine did not know they were looking at. The screen closed itself right behind the request,
  so the server opened the next screen and then closed it again while the client went on showing it: a
  self-test reached that way ended and handed over to nobody until the monitor was left and opened again, and a
  terminal reached that way answered nothing. The machine now puts the next screen up and the old one simply
  gives way to it.
- A step of a by-hand install printed its first lines and then stopped for good once any earlier step had run
  to its end, so a merge stood at "Unpacking source..." with the rest of what it had to say never arriving.
- A machine running a live medium could throw the whole installation away without a word. Whether the medium
  was still in the drive was asked on every line typed and every tick, and the question ended the session the
  moment it did not like the answer, so a drive one tick late to load read as an empty drive: the installation
  was discarded, the terminal stayed on the screen, and from then on every command typed into it vanished with
  nothing printed under it. The question now only answers, a drive nobody can see is no longer read as a drive
  with nothing in it, and ending a session is the machine's own step, which restarts it so the player watches
  it happen.
- A terminal the machine has stopped listening to now says so. It writes what was typed the moment it is typed
  and waits for an answer, so a line refused in silence left a command on the glass with nothing under it, over
  and over, which reads as a machine that has broken rather than one this window no longer reaches.
- Booting any live medium that was not Arch started a Gentoo session, whatever the medium actually was.
- The newest Frames edition's first-boot greeting was worked out on every first start and never drawn. The
  machine built the words, and the screen returned to draw that edition's picture before it reached them, so
  the one start that edition says anything on said nothing.
- The Legacy self-test's key hints said the wrong thing and blinked. The boards of that age printed a sentence
  with the two keys lifted out of it and left it there; only the earliest ones blinked.
- A command the machine refuses to run now says so instead of being dropped without a word. The terminal
  writes what was typed the moment it is typed and waits for an answer, so a refused line left the command on
  the glass with nothing under it: a prompt, a command and silence, over and over, which reads as a machine
  that has broken rather than one that is no longer listening to that window.
- An installation the machine was restarted for comes before the machine's own system. A computer that already
  had one played that system's whole start before showing the installer it had been restarted for, and a
  computer with two stopped at its boot manager on the way, asking which of them to boot when the answer was
  neither.
- Choosing what to boot for one boot only now travels as the disk and the system it names, rather than as a
  place in a list. Two menus offer that choice, they list different things in different orders, and a position
  meant a different thing in each: one was sending a disk number where the other was sending a row, and the
  machine read both the same way, so one of them always booted the wrong system.
- Starting from a live medium restarts the machine, as starting from anything else does. It jumped straight to
  the medium's shell, so choosing one in the setup was the one way of starting a machine that never looked
  like starting one.
- A restart asked for at a terminal or a desktop now puts that player in front of the system closing down.
  The goodbye was only ever sent to players holding a monitor session, and whoever types `reboot` is at a
  prompt, so nothing moved until they left the machine and came back to it.
- What a terminal has printed now belongs to the run of the machine that printed it, and a restart ends that
  run. A machine came up showing the whole installation that had just been typed into it, and a disk swapped
  for a blank one came up showing the session of the disk that had been taken out.
- Installing a system from the firmware restarts the machine into the installation instead of opening it over
  the setup. A computer puts a system on a disk by starting from the medium that carries it: the self-test
  runs again and what comes up after it is the installer. Pressing Install used to change the screen and
  nothing else, so the machine never appeared to restart at all.
- The restart at the end of an installation goes through the self-test and brings the system up from the
  beginning, which is what a restart is. It went straight to the boot target, so the one moment a player most
  expects to watch a machine start over was the one moment it did not.
- A machine that finishes its self-test now shows whoever is watching whatever it is actually doing, rather
  than always its boot target. A machine that was told to install something is in its installer by then, and
  sending the player to the boot target dropped them into the firmware with the installation waiting behind a
  screen nobody was shown.
- The self-test lasts at least two and a half seconds. It was one, which on a modern machine is the whole of
  it: the lines appeared and were gone before anybody had read the first, so those machines looked as though
  they never tested themselves.
- Typing an E no longer closes an installer. The game closes a container screen on its own inventory key, and
  that key is a letter, so the one letter a player could not put in a machine's name was whichever letter they
  had it bound to.
- A Frames machine no longer finishes its start behind a Linux desktop's loading screen. Those screens belong
  to the desktops that are installed and replaced on their own; a system that comes with its desktop built in
  has no such moment, and every one of them was being given another system's.
- The firmware's hardware page keeps its values inside its own panel. A board and a graphics card are named by
  whoever made them, at whatever length they chose, and at full length they ran across the help panel beside
  them.
- The buttons of the grey-machine installers go in when they are pressed, and act when they are let go, which
  is what a button of those machines did. They were washing lighter instead, which on a grey panel reads as a
  highlight and not a press, and they acted on the way down, so the pressed face was never on the glass.
- The newest installer's closing line no longer runs off its card, and its pages show no Back or Next while
  the copy is running, since there is nothing to choose there.
- A terminal fills the same glass the firmware, the self-test and the installers do, rather than the larger
  one the graphical desktops use. On a machine of the earliest age, whose monitor has the thickest shell of
  any of them, the larger glass left too little of the window beside it for the recipe viewer's list.
- A monitor opened during a restart no longer shows the desktop of the system that is being closed.
- A long drive or system name no longer runs through the column beside it or off the edge of the glass, on
  the self-test, on any of the boot menus, or in a system reading its own start out.
- A machine with more drives or more systems than a screen has rows for no longer grows its list past the
  bottom of the monitor. The self-test says how many it is not showing, so the lines that matter most (what
  it is about to boot, or why it cannot) stay on the glass; the boot menus walk their list with the cursor,
  so every entry can still be reached and booted.
- The self-test opened by a restart is drawn for as long as that machine's self-test actually takes, rather
  than for a fallback length it had no way of knowing was wrong.
- A run of the client tests came back saying all was well whatever had happened in it. Every test could fail
  and the process still ended the way a clean run ends, so nothing watching one ever learned anything from it
  and the nightly run could not have reported a failure. A run with a failed test now ends with a code that
  fails whatever started it. Asking by name for a test that does not exist is the same: it used to run nothing
  and end on "ALL PASSED", which reads exactly like a suite that ran and was fine. It says which name found
  nothing, and that the suite is split over its shards before the name is matched.
- Installing a system from the firmware did nothing on the glass. The machine set the installer going and the
  player was left looking at the setup they had pressed Install on; leaving the monitor and opening it again
  showed the installer, and every answer after that needed the same, because each one put the page back where
  it had been. Two halves of the same mistake: what is on the glass is sent and the session that shows it is
  opened, and the first was being sent without the second, while the second was being done again for a player
  already in that session, which tore the screen down and built it back out of the page before the one that
  had just arrived. A copy ending and a machine going down were quiet for the same reason.
- A screen the monitor put up that was not a system (the self-test, the boot manager, a system coming up, the
  firmware setup, an installer, a rack's channel switch) turned up in front of a player who had walked away,
  interrupting whatever they were doing. The server was never told when one of those closed, so it went on
  counting that player as watching the machine and put the next page in front of them. Those screens are menus
  now, like every other computer screen, so closing one tells the machine; they take the monitor of the machine's
  own generation instead of always the newest; and a recipe viewer sits beside them as it does beside the rest.
- A drive's name ran over the figures beside it on an installer's disk page. The name was cut to a fixed
  width that took no account of how wide those figures had turned out, so a long name and the size it was
  next to were drawn on top of each other. The room is measured now, on both the table and the wizard.
- A name being typed in an installer had a caret that never blinked and a field a long name ran out of. The
  caret blinks, and a name longer than the box scrolls under it the way a text field does.
- The buttons of an installer said nothing about the cursor being over them. The one under it is lit now, in
  every one of the five shapes an installer can wear.
- Frames 11 Setup opened on its table of disks, so the first thing it showed was a question about erasing
  something, with nothing having said what was about to happen. It opens on a word first, as the others do.
- A machine standing at a self-test that found nothing to boot now carries on when something to boot turns up,
  rather than standing there for good. A machine is powered the moment it is built and its self-test ends
  seconds later, which is usually before anybody has plugged a drive into it, so it would otherwise be found
  at a failure that had stopped being true. Switching it off and on again would have done the same and still
  does; this only spares doing it to a machine that is plainly ready.
- The power-on self-test no longer sees itself out. It used to close after a few seconds as a way out if the
  machine never swapped the screen, and that became the reason the machine could not: the screen a player
  holds is how the machine knows who is watching, so closing it was leaving, and leaving guaranteed nothing
  came. Opening the monitor of a machine with nothing to boot left the player looking at the world.
- A machine whose self-test found nothing to boot only stood at its failure for whoever was already watching.
  Opening the monitor afterwards dropped the player into the firmware setup with no word about why the machine
  had not started, and the failure closed itself after three seconds even for somebody looking straight at it.
  Standing there is now something the machine is doing, so every monitor opened on it finds it there, and it
  waits for a key however long that takes.
- The firmware setup read the machine once, when it opened. Taking an installation disc out of a drive and
  putting another in left the boot list naming the disc that had been removed, so booting it installed the
  system that was no longer in the machine. The setup asks the machine what it holds while it is open.
- The boot manager could not be left. Every key it did not use it swallowed, Escape included, so the only way
  off the list was to boot something: looking at what the other disk held was a decision with no way back.
  Escape leaves the monitor now, and the machine goes on standing at its boot manager, where opening the
  monitor again finds it.
- The shell of a live installation medium turned away `echo` and `pacman`, although the sequence behind it
  answered both. That made two of the steps impossible to take: the line that sets the build options in
  `/etc/portage/make.conf`, which is how a Gentoo kernel is told to compile more than one thing at a time,
  and installing a package into the new system afterwards. The shell now takes its verbs from the sequence
  itself, so there is no second list to forget a verb in.
- A computer's name could be fifteen letters, which is what the machines of the first age allowed and no fun
  to be held to. It is 256 now, and the three packets that carry a name onto a screen were all cut to 48,
  two of them by refusing a longer one rather than by trimming it.
- Installing a system from the firmware showed the same grey box whatever system it was, so the installer each
  one was drawn for was never reached: Frames XP Setup, the Debian and Fedora wizards, the pages that ask where
  the system goes and what the computer is called. The system was written all the same, which is why it was easy
  to miss. The firmware now sends the machine off and shows whatever the machine answers with, which is that
  system's own installer, a plain copy, or nothing at all when the machine wrote it there and then. Arch and
  Gentoo are unaffected: installing those means booting the medium and doing it by hand, which never went that
  way.
- A machine refused to start any program that reads or writes a field of one of its own classes, saying nothing
  answered it. That was every program with a field, and every program with a lambda, since what a lambda keeps hold
  of is a field too. Programs written before this run now with no change to them.
- A hold a player put on the storage with `LOCK` was let go of by closing and reopening the world, although it
  promised to last until somebody unlocked it. It is written down with the Mainframe now and taken again when the
  world comes back.
- Two readings of the storage under way at once were believed in the order they finished, so a reading started
  first could arrive last and put the network back to an older picture of itself: items taken in between came
  back, and items put in went missing until the next reading. Only the newest reading is believed now.
- A failed Operation said only the word `failed`, whatever had gone wrong. Opening it now says why in a line
  anybody can act on: what the network had run out of, what another Operation was holding, which stage of a
  pattern gave up, or that nothing on the network knows how to make the thing at all.
- Switching a Mainframe off left the Operations it was running reading as still running, for good: a terminal
  went on showing a craft under way by a machine that had no power, and nothing ever moved it off that. They
  now read as discarded, which is what happened to them.
- A craft, a pipeline or a machine run whose saved pattern could not be read came back as nothing at all, with
  no word anywhere about what had been lost. The same for a slot of a saved pattern and a row of the
  Operations log. What could not be read is now named in the log, so a world that comes back short says why.
- An amount too large to weigh came back as less than nothing, and everything after it believed that: a
  request for it passed every check that it fitted, putting it away added room to a disk rather than using it,
  and free space read as more than the disk holds. Asking for more than there could ever be now gets
  everything there is, the same answer a merely large number gets.
- An element written to and read in the same breath now names its place once. `n[Next()] += 5` and `n[Next()]++`
  worked out the index twice, so anything the index did on its way to a number happened twice, and if it did not
  give the same number both times the value was read from one element and written to another.
- A server in a rack no longer sits owing a power-on self-test that nothing ever runs. The flag was there and
  nothing carried it along, so the only thing that could end one was a screen open on that bay reporting it done:
  a rack nobody was looking at never came up, and opening the monitor was what made a machine start.
- A Linux machine on a cable now says its network is up as it comes up. The line was asked of the shell running on
  the machine rather than of the machine, so one that had no shell to ask came up silent about a network it was
  plainly on. A machine is on a network the moment a cable reaches one, which is before anything on it is running.
- An MC-NET machine that has nobody to ask about the network no longer comes up reporting that its link is down.
  Every line of that system's start is a claim about the network, and a machine that cannot put the question has no
  grounds for any of them, least of all for the one saying the cable is dead; it says nothing instead.
- A machine built west or north of the world's origin no longer ignores the drive an install was told to read from,
  and no longer carries on copying a system after the medium has been taken out of it. A drive is named by the
  position it stands at, and a position out there is a negative number, which both of those read as "no drive
  named".
- A shell window on a desktop now sends what you type to the machine you are connected to. Only the terminal that
  takes the whole screen of a monitor did: in a window, `ssh` opened the session and said it had, and then every
  command ran on the computer standing in front of you, with a prompt that named no machine either.
- A second person opening a computer's desktop now sees the windows its programs already have open. The machine kept
  one record of what it had sent, not one per player, so whoever opened after somebody else was shown an empty desktop
  until a window happened to change, and on a machine where nothing was moving it stayed empty.
- A prompt connected to another computer now says so: it shows the name of the machine the line is going to, ahead of
  that machine's own prompt. On MC-DOS and Frames the prompt is only the drive and folder, so a session looked exactly
  like sitting at your own keyboard and there was no way to tell the command had left the computer in front of you.
  The commands did run on the far machine all along; only the prompt kept quiet about it.
- `Program.Shell` runs a line at the computer's own prompt and hands back what it printed. It used to stop the
  program at the call.
- A program can no longer be compiled with `Program.RunSource`, which no computer answered, so the program stopped at
  the call. A listing that still uses it is refused when it loads, and the terminal says so (`A4013`).
- A program can no longer be compiled with the Gateway calls that reached into a ComputerCraft computer
  (`HasAgent`, `Run`, `Shell`, `Read`, `Write`, `List`, `Serve`, `Program` and `Programs`). They went away with
  the agent that answered them, but the compiler still accepted them, so the program stopped at the call.
- Cutting the last data cable between a Mainframe and the rest of its network now takes the network away
  from everything past the cut. The computers there no longer keep working through a Mainframe they are not
  connected to, and the network does not come back when the world is loaded again.
- Constructors can chain to another with `: base(...)` or `: this(...)`. The first chained call used to stop
  the program.
- `Map.TryGet` and `Map.Remove` treat a key that holds `null` as present, as `ContainsKey` does; they used to
  answer false.
- `Map.TryGet` on a missing key fills its value with what a variable of the map's value type starts with
  (nothing for text and objects, zero or false for numbers and bools) instead of 0 in every map.
- `Program.Current.Name` follows `Program.SetName`. It used to keep the name the program had when
  `Program.Current` was first read.
- A program that stays up for a very long time no longer sees its count of instructions run, or of lines
  written, wrap into negative numbers.
- A program's random numbers carry on where they were when the world is saved and loaded, instead of starting
  the sequence over.
- Lines typed at a program's terminal before the program asked for them are still there after a save.
- A line read with `Console.ReadLine` is still there after the world is saved and loaded, and it counts against the
  program's memory like any other text. It used to come back empty.
- An event with several handlers joined to it with `+=` runs all of them, in the order they were joined, when a
  player clicks or closes a window, a message arrives or a watched stock changes. Only the first handler used to
  run.
- A program that calls itself without end halts with a message once its calls go more than 1,024 deep. It used to
  keep adding calls, held in the server's memory, for as long as it ran.
- A thread that ends while it holds a lock, stopped with `Stop` or taken down with its program, lets the lock go,
  and whoever waited for it carries on. The program used to halt with "the runtime could not carry this out".
- A script whose `OnTick` runs longer than a tick, or that is still busy with a handler when the next tick comes,
  skips that tick. It used to run one extra `OnTick` as soon as it was free.
- A program whose last window a player closed ends even when the world is saved before the program has heard about
  it. It used to keep running with no window after the load.
- What a program drew on a canvas and the widgets it placed in a window by hand come back after the world is saved
  and loaded, and so do what a player typed into a text box and any text the program read from one. They used to
  come back empty.
- A row or a column can no longer be put inside itself, and a window holds at most 256 widgets however deep they sit
  in its rows and columns. A row inside itself used to fill its window with copies of the same widgets.
- A program's widgets take from a player only what they could really receive: a row the list has, a point inside
  the canvas, a line of up to 256 characters, and nothing at all while hidden. A forged request used to set a
  selection past the last row or a click far outside the canvas.
- The Cluster Manager shows a section balancing round-robin as round-robin. It used to say MANUAL.
- A listing edited by hand in which a type stands on itself no longer sends the server into an endless loop
  when the program loads.
- A saved program whose state is damaged, or was taken from a listing that has changed since, is left out when the
  world loads, with a line in the server log. It used to come back with parts of it wrong, or keep the computer
  holding it from loading at all.
- A program holding something its save cannot write is left out of the save, with a line in the server log, instead
  of being saved with that part silently empty.
- A machine that has started more than two billion programs numbers the next one from 1 again, passing over numbers
  still in use. The next program used to get a number below zero and be reported as not started while it ran.
- A program run at the prompt from a path written with backslashes, such as `sigma run C:\progs\game.asm`, is
  listed by its file. It used to be listed under the whole path.
- A computer whose terminal had a program in front of it that could not be loaded back, because its language is no
  longer installed or its saved state was refused, gives its prompt back after the load. Every line typed there used
  to go nowhere until Ctrl+C.
- A computer that a Gateway kept busy still owes that work after the world is saved and loaded, and goes on paying
  it back out of its programs' share of each tick. The debt used to vanish with the save.
- A machine fed a gas or a fluid through the network, such as a Purification Chamber burning oxygen, no longer
  stalls on the last lot of a request when it uses a little more than its pattern says, and no longer settles that
  request as partial: when it runs dry while the request is short, the network gives it another lot's worth.
- `Program.Send` answers false for a program that has already returned. It used to answer true while the
  program was still listed for its terminal or its parent to read, though nothing ever read the line.
- A Linux console's welcome, `uname -a` and the OS and kernel lines of `screenfetch` name the architecture the
  processor really understands, `i686` on a 32-bit machine, as the boot log beside them already did. They used to say
  `x86_64` on every machine.
- An install disc of a system lists the files its own family boots from. FreeBSD's carried a Linux kernel and
  its first filesystem, `boot/vmlinuz` and `boot/initrd.img`; it now carries `boot/loader` and
  `boot/kernel/kernel`, and a UNIX medium carries `unix`.
- A file the system generates is found whatever case its name is typed in on MC-DOS and Frames, which never
  told one case from the other: `type frames\system\FRAMES.INI` reads the file listed as `frames.ini`. The
  systems met at a Unix prompt still tell them apart. An installed program's notes on a system without This PC
  no longer send the reader to This PC to remove it.
- The notes on the two `install_by_hand` settings in the server configuration no longer list a password, a time
  zone and a locale among the steps, which the by-hand installs stopped asking for.

## [0.3.0a] - 2026-09-13 - The Programming Update

Codename: Lithium.

The computers became programmable. Cannon, the series' own language, is written, compiled and run on
the computers themselves, with five editors to write it in, and ComputerCraft's computers reach the
data network through the Network Gateway. The computing mod is now called J's Computers.

### Added
- Computers can now be programmed with the series' own programming language, Cannon. You write it at any computer, compile
  it there with `cannonc` down to assembly, and run what comes out. The compiler produces a listing you can open and read a
  line at a time, so any output can be verified.
- For now, a program is one of two things, and says which by how it is written. One with a `Main` runs at the
  terminal that started it, holds the prompt, prints as it goes and is gone when it returns. One that
  implements `IScript` stays up: set up once, called every tick, told when it is stopped, and still running
  after the world has been away and come back.
- A program spends only what the machine's processors are worth in a tick, so an old computer really does
  print line by line where a fast one finishes at once, and one that loops forever costs its machine the
  same tick as one that does nothing. There is no ceiling on that worth: a faster machine gets through
  more, however fast it is. What protects the server is the clock instead: each machine may run its
  programs for so much real time a tick, and all machines together for so much, both set in
  `jstech-balance.toml`. A machine the server had no time for goes first on the next tick, so a busy
  server slows every computer evenly and never stops one.
- A program can do more than one thing at once. `Thread.Start` runs a body beside the rest of the
  program, the two taking turns a few instructions at a time over the same memory; a thread can sleep
  for so many ticks, yield its turn, wait for another to end (or give up after a while) and be stopped.
  Because nothing runs at the same instant, a single expression is never torn; a run of them can be,
  and `lock (thing) { ... }` keeps it together, held on every way out of the block. Threads, their waits
  and their locks come back from a save where they were.
- Programs can start programs. `Program.Start` runs a compiled program from the same disks, with
  arguments the other reads through `Program.Args`, and hands back a handle to wait on, read the output
  of, ask the exit code of, or stop. A program ends itself with `Program.Exit(code)`; what another program
  started keeps its output and code for that program to read until it goes. Programs on one machine
  can send each other lines, heard through `Program.OnMessage`. At the prompt, `cannon run` passes on
  whatever follows the file name.
- Computers on one data network can share folders with each other. `config share C:\pub` opens a folder
  for reading, `config share C:\pub write` for writing too, and `config unshare pub` closes it. Every
  other machine on the network reaches it as `\\host\pub` at the prompt (`/net/host/pub` on the Linux
  systems), from a program through `File`, and in the file explorer under Network, where hosts, their
  shares and the files inside can be browsed, opened and copied.
- A program can reach the other computers on its network. `Network.Computers()` lists them and
  `Network.Computer("desk")` picks one; on it a program can start a program from that machine's disks
  (the same handle as a local one comes back, to wait on, read and stop), run a line at its prompt, send
  a line to one of its programs and list what it runs. Everything runs on the other machine, out of its
  own budget. A machine that would rather not take any of that says `config remote off`.
- A program can speak the network's own language. `Iql.Run` sends a statement to the Mainframe as the
  prompt would and hands back whether it went, what it said and the rows it read; `Iql.Query` gives
  the rows alone and stops the program on a refusal; `Iql.Exec` runs a saved procedure and `Iql.RunFile`
  a file of statements, one a line.
- What a program can reach: the machine's own drives, with the same paths the prompt uses; what the machine
  is made of and what it is running; what the network holds, could hold, and which servers hold it; what the
  Mainframe has been doing this past hour; and the network itself, to pull, push, craft and cancel. Every row
  it leaves in the network's log names the program that asked.
- A program can ask to be told instead of asking. It says once that it wants to know when something runs
  low, and is called when it does: on the crossing, not for as long as it stays crossed.
- Programs can be handed to other people. `canpack` wraps one up with everything it needs into a single
  readable file, publishes it to the network's Mirror, and anyone on that network installs it like any other
  package, marked as a player's own. An installed one gets an icon on the desktop and a terminal to run in.
- A program's memory is counted like everything else the machine holds, so the Task Manager lists it beside
  the windows and services, and a machine without the memory for one says so instead of trying.
- The file explorer knows a source file, a compiled program and a package on sight, and running one is a
  double-click.
- The Network Gateway, a peripheral that puts the data network within reach of ComputerCraft's computers
  when CC: Tweaked is installed. It is placed facing away from the computer it serves: the peripheral cable
  plugs into its back, a ComputerCraft computer, wired modem or networking cable meets its front, and
  there it is a `jsc_gateway` peripheral under its own name. Between the two sides sits a buffer of nine
  slots, reachable from the other faces by a chest, a hopper or a turtle, and shown on the block's own
  screen with a status line and two lights. Several Gateways may serve one computer.
- The Gateway Manager, a program for the computer that has Gateways on its ports: the rail lists them
  by name, Rename and Identify (the block's lights blink) act on the selected one, and four tabs show it.
  Status puts this side and the ComputerCraft side as two cards, says how CC names the Gateway, shows
  the buffer with a button that empties it into the network as operations, and lists the last requests.
  Permissions holds the switches for reading the network and running operations, a priority ceiling for
  requests from CC, and how many calls a tick the Gateway answers. Computers lists the ComputerCraft
  computers attached and can send them a test event; Log keeps the last forty things the Gateway did.
  The `gateway` command does the same at any prompt.
- Through a Gateway, a ComputerCraft program reads the network (what it holds and where, how much room is
  left, which computers are on it), pulls items into the Gateway's buffer and pushes them back, asks for
  a craft, follows and cancels operations, watches a total and is told when it moves, writes a line in
  the Gateway's log, and starts a program on one of our computers. Every call is paid for out of the host
  computer's tick, counted against the Gateway's call cap, kept under its permissions and written in its
  log; an operation that settles and a watched total that moves come back as events.
- `cannon run hello.can` runs a Cannon source file as it is, compiling it on the way in.
- A Cannon program reaches the ComputerCraft side through its machine's Gateways. `Gateway.Names` lists them
  and `Gateway.Select` picks the one a program means, which stays with the program; `Gateway.Computers` and
  `Gateway.Peripherals` say what is on that Gateway's wire and what each thing answers to; `Gateway.Call`
  calls one of those methods and brings back what it said (a table comes back as a list or a map, a whole
  number whole); `Gateway.TurnOn`, `Shutdown` and `Reboot` work the computers over there; `Gateway.Send`
  says something to one of them, and `Gateway.OnMessage` hears what they say back through the Gateway's own
  `send`. A machine with no Gateway, or a Gateway with no ComputerCraft behind it, says so.
- A Cannon program can open windows of its own on the desktop of the machine it runs on. `System.UI` gives it
  a `Window` and the widgets to put in it (`Label`, `Button`, `TextBox`, `CheckBox`, `ProgressBar`, `ListBox`
  and a `Canvas` it draws on itself), laid out in rows and columns where a weight takes a share of the room
  left over, or placed exactly where the program says. It hears what the player does through `OnClick`,
  `OnChange`, `OnSubmit`, `OnToggle`, `OnSelect` and the window's `OnClose`. The machine's own system draws
  it, so the same program is a Frames 95 window on 95 and an XP one on XP, and it has a place on the taskbar
  under the program's name. A window is saved with the program and comes back with it; shutting the last one
  ends the program unless it opens another. A machine that boots to a prompt has nowhere to put a window and
  says so.
- A number, a bool or a character can now go where an `object` goes, and a cast takes the number back out
  as whichever kind is asked for: `int n = (int) held;`.
- Mods may add a programming language of their own, and remove this one. A language that registers itself
  gets the prompt, the terminal, the task manager, saving and the tick budget without writing any of them.
- Five editors to write a program in, each a different bargain between what it shows you and what it costs
  the machine to keep open. Virtual Studio is the whole workshop in one window, and the only one that tells you
  what a line will cost the program before you write it. Virtual Studio Code offers the same suggestions in a
  fifth of the memory, with the machine's own console welded into the bottom of the window. Exposure suggests
  nothing and instead compiles every program on the disk, so changing something shared shows which of the
  others stopped building. Vim and Emacs open no window at all: they take over the terminal they were started
  from, which is what lets a rack server with no screen be programmed, or one reached from another machine.
  Emacs splits that terminal, so a program and what the compiler said about it are readable at once.
- Whatever language a file is written in colours it, marks in the margin where the compiler stopped, and
  suggests what can follow a name, in every one of the editors. A language a mod adds gets all of it.
- Virtual Studio works in solutions. It opens on a Start Window: what you opened lately, or a project or
  folder or file to open, or a new project to create. Creating one picks a template first, filtered by
  language, platform and kind, then names the project and its solution, and writes a solution file, a
  project file and a first source that already builds. The Solution Explorer shows every project with its
  dependencies, properties, sources and what it built; Build compiles a project with the libraries it
  references and writes the listing where the project file says; Start builds the startup project and
  runs it at the terminal; Package hands the project to `canpack`.
- Virtual Studio Code works the way its namesake does: a Welcome page, a folder opened as a tree beside
  the editor, a command palette on Ctrl+Shift+P, F5 to build and run the open file at the terminal welded
  into the window, and an Extensions page listing every language the machine knows.
- Installing takes time. Setup runs as a job on the machine: a window with a progress bar on a desktop,
  lines at the prompt, cancellable from either, and how long it takes depends on where the program comes
  from (a floppy is slow, a DVD faster, the network faster still) and how big the program is. A machine
  that goes away mid-setup picks it up where it was.
- `setup.exe` on an installation disc, double-clicked in the explorer, starts that setup.
- An installed program is on the disk. Its folder under Program Files (or Program Files (x86), for one
  older than the system) holds the program, its settings file, its libraries, a font when it brought one
  and what came on its disc, and the system's own folder is called Frames, with the same kind of contents.
  The explorer, the prompt and the studios read them like any other file.
- An installation disc reads as a disc: its sources folder holds the setup, the packed program and its
  checksums, its support folder the read-me and the checksum list.
- Every terminal on a machine is its own session, with its own place on the disk: two Command Prompts
  can be in two folders, and what one prints is only in that one. Closing a Fedora terminal and opening
  it again shows what it printed before.
- Cannon strings can carry values: `$"Total {a + b}"` fills the braces with what the expression comes to.
  Two braces in a row are one brace of text.
- The code editors in both studios grew up: text can be selected with the mouse or with Shift and the
  arrows, cut, copied and pasted; Ctrl+Z and Ctrl+Y undo and redo; the font zooms with Ctrl and the
  wheel; a new line keeps the indentation of the one before; a bracket or quote closes itself; Tab and
  Shift+Tab indent the selected lines; and the marks in the margin say what the compiler said when the
  cursor rests on them.
- Files close. Ctrl+W, the cross on the tab or File > Close closes the open file, asking first when it
  has changes. Ctrl+W in Virtual Studio Code does the same.
- The explorers of both studios show the same icons the file explorer shows, and the file explorer's
  columns can be dragged wider or narrower.
- Implement Interface: with the cursor on a class that says it implements one, Edit > Implement Interface
  (or Ctrl+.) writes the methods the interface asks for that the class does not have yet.
- Virtual Studio's dock splits into Error List, Output and Terminal; the Error List is a table of code,
  description, file and line, and the panels and the Solution Explorer resize by dragging their edges.
  Starting a console program runs it in the dock's terminal.
- The Create a new project page shows the templates used lately beside the list, filters by language,
  platform and project type, and the Configure page asks where the project goes, with a "..." to browse.
- Cannon has namespaces and usings, and every type lives in one. `namespace Tools;` at the top of a file
  puts its types there, or a `namespace Tools { }` block does, and blocks nest. Types in the same
  namespace see each other plainly; anything else is brought in with a using: `using Tools.*;` for the
  whole namespace, `using Tools.Counter;` for one type, and `Tools.Counter` names one from anywhere. A
  file with no namespace, or a name used without its using, is told so by the compiler.
- The language's own library is the `System` namespace, with a namespace inside it per subject: the
  console and files under `System.IO`, lists and maps under `System.Collections`, `Math` under
  `System.Utils`, the machine under `System.Machine`, the network under `System.Network`, what the
  Mainframe has been doing under `System.Operations`. A program says which of it it uses.
- Structs and records. A struct is a value: assigning one copies it, two are equal when their fields
  are. A record is written as a line, `record Point(int X, int Y);`, and gets its fields, its
  constructor, its `ToString` and its equality from that line.
- Classes nest: a class declared inside another is named through it, `Farm.Counter`, and is a type in
  every way the outer one is.
- Programs take input. `Console.ReadLine()` waits for a line typed at the terminal the program runs in,
  and `Console.HasLine()` says whether one is waiting; a program waiting reads as "input" in the Task
  Manager. Typing at a terminal with a program in front goes to the program. `ReadInt`, `ReadLong`,
  `ReadDouble` and `ReadBool` wait the same way and hand the line back as a value, and stop the program
  with the text they could not read when it was not one.
- A program can say what it is called. `Program.SetName("Sorter")`, under `System.Execution`, is the
  name the Task Manager, `cannon ps` and the machine's own process list show for it, and `Program.Name`
  reads it back. A program that gives itself no name is listed as `cannonrt`, the runtime running it,
  the way an interpreted program shows up under its interpreter on any machine. The name survives the
  world being away and back.
- `Convert` turns text into values the other way round: `ToInt`, `ToLong`, `ToFloat`, `ToDouble`,
  `ToBool` and `ToString`, which stop on text that is not one, and `TryInt`, `TryLong`, `TryDouble` and
  `TryBool`, which say whether it was and hand the value out sideways, for a program that would rather
  ask again.
- The file explorer's address bar is a text field once it is clicked into, with the whole path
  selected: Ctrl+C copies it, typing replaces it, a click in the text puts the caret there, and Shift
  with the arrows (or a drag) selects part of it to copy. Every text field on every desktop selects
  the same way, and Ctrl+A selects all of one.
- The file explorer's right button, on an empty part of a folder, offers "Open in" the desktop's
  terminal, which comes up with its prompt in that folder.
- The Solution Explorer works with the right button: a source offers Open, Exclude From Project and
  Delete (which asks first, then takes the file off the disk, off its tab and out of the project); a
  project offers Build, Set as Startup Project, Add New Item, Add Existing Item, Add Project Reference
  and Properties; the solution offers Build, Clean and Add New Project.
- A machine remembers what its windows had open, not only that they were open: after the game itself
  was closed, Virtual Studio comes back on its solution with the same files on its tabs, Virtual
  Studio Code and Exposure on their folder and files, and the explorer on its folder.
- The editors suggest the program's own names, not only the language's: after a dot, what the variable,
  the field, `this`, `base` or the type before it has, read one name at a time through a chain; on a
  bare name, the variables in reach, the members of the class around the caret and every type the
  program declares, in the other files of the folder and in the libraries the project references too.
  The suggestions come while the line is still broken, which is when they are asked for.
- Every terminal moves the caret within the line: Left and Right, Home and End, Ctrl with Left or Right
  by a word, and Delete.
- Vim knows more of itself: w, b, e, ^, G and gg to move, dw and D to cut, yy, p and P to copy a line
  and put it back, u and Ctrl+R to undo and redo, and `:12` to go to a line. Emacs answers C-f, C-b,
  C-n, C-p, C-a, C-e, C-d, C-k, C-y, C-x u, M-< and M->, and asks before leaving with changes unwritten.
  Both slide a long line sideways to keep the caret in view.
- The code editors have a scroll bar down the side and one along the bottom, slide sideways with Shift
  and the wheel, and take a whole step of indentation back on Backspace. The right button on the code
  opens Cut, Copy, Paste, Toggle Line Comment and the refactorings, which is where Implement Interface
  lives now (Ctrl+. still works). Virtual Studio's zoom is a control in the status bar and under View;
  Virtual Studio Code keeps it under View > Appearance.
- The explorer's New entry opens a menu of what can be made, a folder first. A click past the last crumb
  of the address bar turns it into a path to type, copy or paste; every text field answers Ctrl+C, X
  and V.
- The desktop's right button offers what a desktop offers: Open, Open with, Rename, Delete and
  Properties on an icon; New, Refresh, Display settings, Personalize and Properties on the wallpaper.
- The system has a file window of its own, the one every program opens to choose a file, a folder or
  where to save: the places on the left as the explorer lists them, the way back, forward and up, the
  address as crumbs that can be typed over, the folder's contents with the explorer's icons and columns
  and only the files of the kind asked for, then the name, the kind and Open, Save or Select Folder. A
  double click enters a folder or takes a file, Save asks before replacing, and it wears each desktop's
  clothes. Virtual Studio, Virtual Studio Code and Exposure open and save through it; any program can.
  It is a window of its own: it comes up over the program that asked, listed with that program on the
  panel rather than as a program of its own, and holds the program until it is answered or put away.
- The panel lists programs, not windows. A program opened twice has one entry, and the entry says how
  the program stands: pinned with nothing open, open, in front, or put away, each in the panel's own
  language (Frames 11 marks the icon underneath and splits the mark for several windows; Frames XP
  pushes the button in and groups several under a count; KDE and Cinnamon underline, fill and stack).
  Resting the cursor on an entry of Frames 11, KDE or Cinnamon shows a card per window with its live
  picture; a click on a card brings that window forward, its cross closes it alone. Frames XP and 95
  list the titles instead, the way those desktops did.
- Programs pin to the panel. The right button on an entry, on a desktop icon or in Start offers Pin to
  taskbar, and a pinned program keeps its place with nothing open (on the quick launch beside Start on
  Frames XP; in place on Frames 11, KDE and Cinnamon). The pins are the machine's, kept with its other
  settings, and a fresh machine pins its file explorer. `settings pin files` and `unpin` at the prompt
  do the same.
- Exposure is an editor: File makes, opens, saves and closes files and picks the folder, Source comments,
  goes to a line and refactors, Project rebuilds the folder and runs the open program at the terminal.
- Settings > Display has a Scale: the desktop draws everything at 100, 90, 80, 75, 66 or 50 percent of
  its designed size, so a machine can fit more on its glass. Every machine starts at 75, which is the
  size that reads best on the monitor. The `settings guiscale` line at the prompt sets it too.

- Every system, desktop, service and program holds a share of the computer's RAM, in megabytes, and a program
  opens only while it still fits: a bundled program weighs a share of the system it ships with, an installed one
  what its generation weighs, so a modern tool needs the gigabytes a modern machine has. The System Monitor lists
  who holds what.
- A balloon over the notification area carries the notices a computer raises by itself, the first being the
  one about memory.
- The Task Manager, on every desktop, in the shape that desktop really had: the Close Program box on Frames
  95, the four-tab manager with its menu and status bars on Frames XP, the page rail on Frames 11, and the
  system monitor each Linux desktop's own package brings. It reads this machine (its processes and what
  each holds, its memory, processor, disks and network link) and ends the program you pick. It has no icon
  of its own: right-click the panel and it is one entry on the menu, the way these desktops offered it.
- Every panel's notification area shows whether the computer is on a data network, alongside a speaker and
  the memory bar; resting the cursor on it reads out the link and the figures.
- The Network Interactor's grid is driven from the keyboard: a dotted cell marks where it is, the arrows
  move it, Enter opens the request dialog, C the craft dialog, F stars the item and the slash puts the
  keyboard in the search. A line under the status says so.
- A click on the grid selects an item and a double click opens it. Several are selected at once by
  dragging a band across them, with Shift and the arrows, with Control and a click, or with Control+A;
  a click on empty grid drops the selection. Enter or Request then asks for all of them in one dialog, a
  quantity each, and F stars them together. The details panel lists what is selected and what it weighs.
- Two drop-downs beside the search narrow the grid to the mod that made the item and to what the item is
  (ingots, ores, raw materials, blocks, machines, tools, food and so on); both stack with the search.
- Items can be starred. A star sits on the cell, a Favourites tab at the strip's right end shows the starred
  items alone (a craftable one even when none is in stock), and the machine keeps the stars, so every window
  on it shows the same.
- Two grips reshape the window's insides: the one between the grid and the details panel gives the grid
  more columns (the inventory keeps its nine and sits centred under the wider grid), and the one above the
  inventory folds its top rows away so the grid gets their room. Where they were put, the search, the
  filters, the sort and the tab all come back with the window.
- The details panel says what the network holds of the item and where, which recipes make it and through
  which machines, what other patterns use it in, and offers Request, Craft and the star. The item's id,
  weight, tags and components follow below.
- An item the network makes more than one way (a processing recipe and a multi-stage one, say) shows the
  recipes side by side in the craft dialog, each with its machines, stages, time and inputs against the
  stock. Picking one plans with it and a strip says what it does differently from the other: the ingredients
  swapped, the stages added, the seconds gained or lost for the amount asked, and what it is short of. The
  pick is remembered per item, so the next request opens on it. Left and Right pick from the keyboard.
- The craft dialog's plan reads a short ingredient in red and says under it what the network would craft to
  cover it, or that nothing on the network makes it.
- Every zone of the Network Interactor has a boundary of its own now, drawn by the desktop's skin: the
  toolbar on a band, the items in a captioned, sunken field of drawn cells (empty ones too, so an empty
  network looks like one), the inventory in a second field, the details in a framed panel with a header
  strip, and a status bar with segments and a storage gauge. With nothing chosen, the details panel and the
  Status tab show the network's card: the Mainframe, the storage used of what the servers hold, the counts,
  and what is running.

### Changed
- The computing mod is J's Computers. It was J's Computronics, and Computronics is the name of a mod
  that already exists. The mod id stays `jsc`, so nothing a world holds changes name.
- The Task Manager ends a program you wrote, as it ends a window. What the machine itself is made of still
  cannot be ended, because that is the machine and not something you started.
- The network knows how much it could hold, not only how much it does: a server offers its drives, a
  personal computer the share its owner published.
- Work asked for through IQL says so in the network's log. It was reading as the shell's.
- The desktop's open-program counter became a memory meter, "used/total MB", in every desktop's panel.
- Frames XP wears its own shell again: the Start pill with its flag, task buttons carrying each program's
  icon and showing the window in front as pushed in, a Start menu that opens on the player's own face and
  name, and a wallpaper with clouds over its hill.
- Desktop icons sit on a grid wide enough for their names, wrapped over two lines and written in the smaller
  text, so a long one no longer runs across the icon beside it and the desktop keeps its room.
- Task buttons share the strip out between them instead of each keeping a fixed width, so the open programs
  stay visible however many there are.
- An Operation's provenance rows name the computer that asked and the program it asked through, as in
  "lab-pc (Interactor)"; a bus reads as its kind and its name.
- Installing scales with the machine: the same program takes twice as long on each older generation of
  hardware, so a Vintage machine reading a floppy really waits and a modern one reading the network
  hardly does.
- `apt`, `dnf`, `pacman` and `pckmgr` speak as they do: a package is fetched, unpacked and set up with
  its own version, and the progress bar grows in place on one line instead of printing a new line each
  time. Removing goes the same way.
- Setup on a Linux machine happens at the prompt only; the Setup window is a Frames thing.
- A file whose extension belongs to no language is opened as text, not compiled: an assembly listing is
  read, not built.
- `cannonc` says how to run what it wrote. Compiling and running are two commands, and the prompt now
  names the second one after the first has finished.
- Virtual Studio Code's side panel is the folder and nothing else: the folder's name, then its tree,
  with no caption over it and no list of open editors above it, since the tabs already say what is
  open. A file or a folder in the tree opens on a double click; one click only picks it.

### Removed
- The one-line console at the bottom of the Network Interactor. The window does by itself what it was there
  for, and the Command Prompt is the terminal, with a history.

### Fixed
- The installation disc's setup no longer sits in the local disk's root, and the installed program's
  files are where the shortcut says they are.
- Two open terminals no longer mirror each other's output and folder.
- Virtual Studio's menus open and close on the click; the File menu no longer stays open with the others
  dead beside it.
- The name of a new file starts with the cursor before the extension, and the arrow keys move it.
- The "Open with Virtual Studio" entry of the explorer's menu is one entry, not a row of them.
- Errors are listed in a table with room for the description instead of running into each other.
- Backspace on the indented line under an opening brace no longer pulls the closing brace up onto the
  line above: a space before the caret and the end of a line after it were being taken for a pair.
- The editors read the whole folder when they mark errors, so a class that extends one written in the
  file beside it is no longer told that class does not exist.
- Two methods with the same name and the same parameters are refused as one declared twice.
- Typing a B with Ctrl held at a computer no longer switches the game's narrator on and off: the game
  saw no text box and took the key for itself.
- A running program with a long name no longer drops the player's connection when the machine's
  settings are read: every name in that message is cut to what the message can carry.
- A program with a struct or a record in it runs. The compiler wrote them into the listing under
  their own words and the machine did not know those words, so it said the listing was not something
  Cannon could run.
- A record joined to a string reads as its fields, `Item { Name = iron, Qty = 3 }`, the way its own
  `ToString` writes it, rather than as the name of its type. Any class with a `ToString` of its own
  reads that way in a sentence.
- A click on the empty end of an editor's tab strip no longer closes the last tab. It counted as that
  tab's close mark, so a few stray clicks there emptied the editor.
- Vim and Emacs open the file that was named. A file named from inside its folder (`vim Program.can`
  after `cd progs`) was asked for by its bare name, so the editor opened an empty file of that name
  instead of the one on the disk.
- Emacs takes a command after `M-x`. The letters typed after it were run into it (`M-xcompile`), so
  `M-x compile` and `C-x u` could never be read.
- A terminal program stopped on a read no longer stays on the machine's list for ever when it loses
  the terminal. Only the program in front of the terminal gets what is typed, so one that was left
  behind by another program taking the terminal could never be answered; it sat at no cost and some
  memory, listed as running. It is stopped the moment the terminal moves on, and a machine finding one
  in that state clears it.
- A Cluster Management Computer can be renamed from its own screen. The name typed there was refused.

### Security
- A computer acts only on what a player at its screen sends. A modified client could put any position
  in a request and reach a machine it had never opened. Every request from a client is now checked on
  the server against the screen that player really has open: a desktop, command prompt or terminal on
  that machine within reach, a firmware, self-test, installer or KVM screen the server opened on a
  monitor still showing the machine, or the assembly screen of the block being changed. A request that
  fails the check is dropped and written to the server log.

## [0.2.0a] - 2026-09-06

The mod became a series. The code every module shares now lives in a library mod of its own, J's Core,
and the industrial machines in their own mod, J's Industrial; J's Computers keeps the computers, the
network and the programs. The three ship together at one version. Worlds from 0.1.0a do not carry over:
the industrial blocks and the material items changed their ids.

### Added
- J's Core (`jscore`), the library every mod of the series requires: the hardware eras and the industrial
  tiers, the material catalogue, the data network, the Operations framework, energy, peripheral links,
  multiblocks, configuration, the event bus, persistence, the unit formatter and the screen toolkit.
- J's Industrial (`jsindustrial`), the machines and their recipes, with a creative tab of their own; the
  Pattern Studio pairs its recipes with the Macerator, the Compressor and the Electric Furnace.
- Priorities for network Operations: a level from LOW to HIGH, chosen in the Network Interactor's request
  and craft dialogs or with an IQL `PRIORITY` clause, and changed on a live Operation from the Task Manager.
  A queued Operation gains a level while it waits, so none starves.
- Cancelling an Operation in flight, from the Task Manager or with `cancel <id>` at the prompt; `ops` now
  lists each Operation's id.
- The time every Operation waited and ran on its log entry, the last hour's statistics per type in a Stats
  tab of the Network Manager, and a `stats` prompt command.
- One server config for the engine's balance, `jstech-balance.toml`: the disk latencies, the waiting
  timeout, the priority aging period, the Subframe share and the orphan expiry, clamped on load.
- A registry of Operation types and lifecycle events on the core's event bus, for mods that follow or drive
  the network's work.
- A README for the series and one per mod, and a page on the interface components the programs are built
  from.

### Changed
- Every desktop program and dialog draws from one set of interface components; the desktop's error dialog
  is one of them.
- Subframes lend their share of capacity and their GPUs' queues to the Mainframe orchestrating them.
- A REINDEX reads the disks on its tick and rebuilds the catalog off it; a craft request is listed as
  pending while its plan is made off the tick.
- Saved Operations left unresumed for longer than the orphan expiry are discarded on reload instead of
  resumed.
- The server config `jsc-server.toml` is replaced by `jstech-balance.toml`.

### Fixed
- A pull into a computer that was broken mid-transfer no longer keeps taking items out of the network.

## [0.1.0a] - 2026-09-05

The first numbered version. Everything before it was the unversioned groundwork of the `0.0.x` line.
The mod is in alpha: computing is the only module, and it is still growing.

### Added
- Hardware eras (Vintage, Legacy, Standard) for computers, monitors and drives, with 3D cabinets for
  the mainframes, the server racks and the USB flash drive.
- Removable media (floppies, CDs, DVDs and USB sticks), the drives that read them, and install media
  for systems and programs; This PC installs a program straight from an inserted disc.
- The Frames desktops (95, XP and 11), Linux systems with a boot manager, dual boot, package managers
  and three desktop environments, and the programs that ship with them: Command Prompt, Files, Editor,
  This PC, Settings, System Monitor, Calculator, Network Manager and the Network Interactor; the
  Crafting Manager, the Craft Planner, Storage Insights, the Automation Manager and Minesweeper install
  from their own media.
- Servers that mount in racks, one rack per era, the Supercomputer rack on its HPC fabric, and the
  Cluster Management Computer with its Cluster Manager program.
- Drive contents kept as storage volumes in their own save data, with disks sized by era.
- Machine autocrafting: the Crafting Computer, the crafting cable and switch, input, output and
  receiving buses, machine patterns, multi-stage recipes, parallel stages and crafting-card threads.
- Chemicals and fluids as network data, carried by the buses; Mekanism machines and the Fusion Reactor
  can be fed and driven through the network, including D-T fuel made from network gases.
- Crafting from the command line and from IQL through the same planner the graphical terminal uses.
- The Pattern Studio, where recipes are authored on the computer, and the Pattern Encoder, one per
  era, which burns them onto media.
- JEI: the ingredient list sits beside every monitor screen, and a recipe transfers into the Pattern
  Studio's bench or machine draft.
- Every system and program is credited to its software house, and tooltips colour their era.
- A contribution policy for automated tooling and a public documentation folder.

### Changed
- Requires NeoForge 21.1.248 or newer.

### Fixed
- The idle tick cost of big bases and the autosave freeze their drives caused.
- Desktop windows stay current after a restore.
- The Crafting Manager sees a disc inserted after its window opened.
- Breaking a drive that still holds a disc removes the drive.
- A rack server's desktop no longer crashes the monitor before the rack's era has reached the client.

[Unreleased]: https://github.com/jvpts11/js-tech-series/compare/v0.3.0a...HEAD
[0.3.0a]: https://github.com/jvpts11/js-tech-series/releases/tag/v0.3.0a
[0.2.0a]: https://github.com/jvpts11/js-tech-series/releases/tag/v0.2.0a
[0.1.0a]: https://github.com/jvpts11/js-tech-series/releases/tag/v0.1.0a
