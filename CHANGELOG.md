# Changelog

All notable changes to the J's Tech Series are recorded here, newest first. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); version numbers and phase letters follow
[docs/RELEASING.md](docs/RELEASING.md).

## [Unreleased]

### API
- J's Core's API is at version 2 and J's Computers' at version 3. Everything this release adds to either, or
  changes, carries `@ApiStatus.Experimental`: it keeps the mark through this release and loses it when the next
  cycle begins. Each mod's API is now kept line by line, every type and member a mod can reach with the version
  that brought it, and the tests fail when the code and that list disagree; `docs/API.md` says how.
- Added: a build can ask a language for an instruction set and a version of the language at once, with
  `IProgrammingLanguage.compile(sources, CompileOptions)`; a language without versions builds as it did for the
  instruction set alone, which is what the method does unless overridden.
- Added: `IsaSpec`, `ComputersRegisterEvent.isa` and `JsComputersApi.registerIsa`, which take over from
  `ArchitectureSpec`, `architecture` and `registerArchitecture`.
- Added: `EngineDef`, `EngineCapability`, `ComputersRegisterEvent.engine` and `JsComputersApi.registerEngine`. A mod
  registers a Network Operations Engine by its package (a program registered like any other), the dialect it
  speaks, the version it ships in for each age of Mainframe and the extras it offers; until it brings a planner of
  its own it plans the way the Midsoft IQL Server does. Its package is of the new kind `ProgramKind.NETWORK_ENGINE`.
- Added: `ProgramRequirement` and `ProgramSpec.requires`, with `requiring`: a program says what it needs of the
  network's engine (the engine it is written for and the oldest version of it, or the capabilities it uses), which
  is checked when it is opened rather than when it is installed.
- Changed: `IProgrammingLanguage.Complaint` carries its message as a `Text`, read in the language of whoever is
  shown it, with the `arguments` the message was written around; `text()` is the line a person reads, and
  `format()` is still the same line in English.
- Changed: `OsDef` says its shell with a `ShellKind` (`shell`, where it had the id `shellId`), and so do
  `linuxDistro` and `terminalSystem`. `ProgramSpec` has a `description`, `of` no longer takes the display name
  (`named` and `described` give the English words), and `name()` is the name to translate. A
  `DesktopEnvironmentDef`'s native names are `TextKey`s, and so are what `nameOf` and `launcherLabel` give.
- Changed: `ComputerTerminalMenu` stands on the Core's `CoreMenu`, which now holds what it had from the menu it
  stood on before. Its layout constants (`INV_X`, `INV_Y`, `HOTBAR_Y` and the `STORAGE_` ones) and
  `storageSlotCount()` are gone: `invY()` and `hotbarY()`, with the new `invX()`, give where the inventory is
  drawn. `openingTab` says which tab a terminal opens on.
- Deprecated: `ComputersRegisterEvent.architecture`, `JsComputersApi.registerArchitecture` and
  `ArchitectureSpec`. They still work, handing what they are given to the instruction set registration, and go
  in the next cycle; `ArchitectureSpec.toIsa()` gives the same values under the new name.
- Removed: `ArchitectureSpec.runs` (ask the `IsaSpec` from `toIsa()`), and `DesktopEnvironmentDef.programFor`.

### Added
- One way to write a settings file, in J's Core, for any mod built on it. A mod declares a file once (its name,
  whether it is a player's, common to every game or a world's, its format and the version of its layout) and each
  setting in it: a flag, a number held to a range, a word held to a list, or anything with a codec, with the comment
  a person reads above it. Files are written in TOML, JSON, JSON5, YAML or NBT, with their comments, ranges and
  defaults wherever the format has room for comments. A TOML file is NeoForge's own, so NeoForge keeps it in step
  with the players and reads it again when it is edited; the others the Core reads and writes whole. Every file
  carries the version of its layout: an older one is upgraded by the steps its mod declares before it is read, one
  from a newer mod is read and never written over, and one that cannot be read is kept aside as `.unreadable` while
  the defaults are used. YAML is read through SnakeYAML 2.7, which J's Core now carries inside its jar, with its
  safe reader only, so a file can never name a class for the game to build.
- J's Core and J's Computers open NeoForge's settings screen from the mods list. Their world settings are listed
  there in sections, each setting and section under a name of its own, short enough to be read whole, with its
  comment as the tooltip, in English and in Portuguese. A mod built on the Core names each setting and section of a
  TOML file where it declares it, and the names go into the mod's English language file with everything else.
- Saves that carry the version of their layout, in J's Core, for any mod built on it. A mod declares once, for each
  kind of thing it saves (a block entity's tag, an attachment, a value saved whole), the version of its layout and
  the steps that bring an older save up to it, and every save then carries the version it was written in. A save from
  before there were versions is read as the version before the first; one written by a newer version of the mod is
  read as far as it can be, and the log says so. Settings files count their versions the same way.
- States, in J's Core, for any mod built on it: a value a mod keeps with a world for the whole server, for each
  dimension, for each player (read and changed whether they are online or not) or for each team. A state is declared
  once, with its codec, its default and the version of its layout, and saved with the world in a file of its own. A
  synced state sends each player the value that is theirs to see: the server's to everyone, a dimension's to whoever
  is in it, a player's to that player alone, a team's to the players on it. Teams are the game's scoreboard teams
  unless a mod hands the Core a source of teams of its own. A state's file saved by a newer version of its mod is read
  as far as it can be, and the file as that version left it is kept beside it once, as `<name>.newer-v<N>.dat`.
- Blocks that know their neighbours, in J's Core: a block declares once, as ports, which lines it takes on which of
  its faces (its back, its front, every face), named from its own point of view so they turn with it, and in which
  generations, a port taking its own and every earlier one. A cable that shows a connection and the device that
  joins through it ask the same ports, so they never disagree. A block entity hears which of its faces saw its
  neighbour change, and a block's faces can tell which of the eight blocks around them in their plane they join, for
  textures that run across many blocks.
- Multipart blocks, in J's Core: thin parts mounted on a block's six faces, one a face, whose kinds any mod registers
  with the Core, and a bundle of up to nine wires through the block's middle beside them, each wire a line in one
  generation and one colour or none. A block keeps its parts and wires as fields of its block entity; they are
  saved whole, and the players who see the block are sent only what draws it. Which part a player points at is
  worked out the same way for every block. J's Computers' buses are now parts of the Core's.
- Grids, in J's Core: one grid of each kind (power, fluid, heat, gas, motion, data) in each dimension, shared by
  every line of that kind. Cables of one line join when they are of the same generation and their colours agree (the
  same colour, or either in none); devices such as routers join every line. The slowest cable on the best way
  between two places, and the length of each run of one cable against how far it reaches, are worked out when the
  grid changes and kept until it changes again, never on every tick. Only the data grid carries the identity of a
  network; the data network's index is now the data grid.
- Models put together as the game runs, in J's Core: a multipart block draws its parts and the pieces of its wires on
  top of its own model, and a block can have faces that run on into the blocks it joins, drawn from five tiles a
  quarter of a face at a time. Both are drawn into the world's mesh, built again only when a block changes.
- The shared cable block, in J's Core: every mod's cables are laid in one block, each line in a lane of its own on a
  grid of three by three, and the wires of one block never join one another. A wire crosses a face alone in the
  middle and beside others in its lane; where a wire changes place, or two would cross inside the block, the block
  becomes a junction box the wires enter and leave each in its place. A cable dyed one of the sixteen colours wears a
  ring of it on every block and joins only its own colour and the uncoloured, which joins every colour. A cable that
  never shares a block, as a long-distance line does, holds its block alone. Using a cable on a cable block lays it
  there when its lane is free, and against the face clicked otherwise or while sneaking; breaking takes out only the
  wire or the part looked at, the block going with its last piece; a dye colours the wire looked at. A mod declares
  its cables with its content, each with its line, its lane, its thickness, how much it carries, how far it reaches,
  its jacket and the plug it ends in where it meets a device that takes it.
- Energy in J's Core. FE is registered as the unit `jscore:fe` in a new synced registry of energy units, where a mod
  adds its own with what it is worth in FE: an exact ratio of two whole numbers, rounding down so no energy is made
  out of a rounding. An item can hold energy in the `jscore:energy` component, given the game's energy capability
  with `CoreEnergy.holds`.
- The energy grid moves energy, in J's Core. An energy wire plugs into every block beside it that offers the game's
  energy on that face, and each tick generators feed consumers, what they have left fills storage, and storage feeds
  what consumers still want, each share in proportion to what is wanted. A cable can lose thousandths of what crosses
  it, and energy takes the way that loses least, worked out once while the grid keeps its shape.
- J's Industrial's Energy Cable, laid in the shared cable block's energy lane: it carries any amount of energy any
  distance and loses none of it.
- Items with state, in J's Core. An item says where it is declared what it holds besides itself: modes it switches
  between, energy, one fluid, stacks, and components it starts with (a mod declares its own components with its
  content). The Core gives it the game's capability for each, keeps each in a component that is saved and sent with
  the item, makes it stack alone when it keeps anything inside, and lists what it holds in its tooltip. An item that
  holds stacks takes no other item that does. The new Change Item Mode key moves the item in the main hand on to its
  next mode, or back one with shift held, and says the new mode on the action bar; it has no key until the player
  gives it one.
- Content from datapacks, in J's Core. A mod declares a registry of JSON files, every file under
  `data/<namespace>/<folder>/` one value read with its codec, read again whenever the server reloads its data; a file
  that does not read is left out with a line in the log saying why. A synced one is sent to every player as they
  join and after every reload, and their game keeps it apart from the server's. A mod can also declare a registry
  datapacks fill, sent to every player with the world's registries.
- A tick scheduler, in J's Core: a block that runs many things in a tick shares the tick's work among them in turns
  of a fixed size, gives what one leaves to the others, and stops at a deadline by the clock, the next tick starting
  with the first that went without. J's Computers' machines run their programs with it, as they did with their own.
- Batches of moves that go through together or not at all, in J's Core: each step is asked first whether it would
  move all it wants and nothing moves when any would come short; when a store takes less than it promised as the
  batch moves, everything already moved is put back, last first. Steps come ready for putting items into and taking
  them out of an item store (put back slot by slot), filling and draining a fluid store, and giving and taking
  energy.
- Key actions, in J's Core: a mod declares a key once, with its name, the key it starts on and what the server does
  when it is pressed; the player's game makes the key binding from it and sends the press to the server by itself.
  Turn Off Last Sound and Change Item Mode are key actions now, under the same names, so a key a player gave them
  stays given.
- The series on the debug screen (F3), in J's Core: each part of the series adds a panel of lines under its name, the
  sound system's first, and the pieces of work the series times are listed with their average and longest time over
  the last hundred runs, the energy grid's tick among them.
- Inventories, in J's Core: item filters that let pass only what they list or everything but it, each rule naming an
  item exactly (a worn tool is not a new one), an item whatever its damage and components, or a tag, and carrying an
  amount a bus can read; what each face of a block lets through (closed, in, out, or both), named from the block's
  own point of view so it turns with the block; and moves of items, fluid and energy from one store to another that
  never lose anything on the way, into an item's own inventory as well.
- Fluids declared in J's Core: a mod declares a fluid once, with its name, its textures and tint, how hot, heavy and
  thick it is and how bright it glows, and the Core registers its type, its still and flowing fluid and, for a liquid,
  the block it pours as and its bucket, drawn with the fluid inside; the generator writes their names, the bucket's
  model and the fluid's tags. A gas is lighter than air, never poured into the world nor held in a bucket, only kept
  in tanks; it is tagged a gas for the Core and for other mods. A fluid can be marked corrosive.
- Pipes, in J's Core: a cable of the fluid grid plugs into every block beside it that holds fluids. Pipes are
  passive: a connected run moves, each tick, at most what its slowest pipe carries, which is the pressure it holds,
  and only the fluids every pipe of it is made for (the temperatures it stands, and whether it takes gases and
  corrosive fluids), leaving the rest where they are. An output feeds the inputs, what it has left fills the tanks,
  and the tanks feed what the inputs still want, each share in proportion to what is wanted, nothing lost on the way.
- Large values sent in pieces, in J's Core: a mod declares a kind of value with its codec, and the Core writes it,
  packs it, sends it in pieces either way (the server a share of each player's pieces every tick, so a large sending
  never holds the others up) and puts it back together on the other side. A side keeps only so many sendings from one
  sender open at once and refuses pieces out of order, sendings larger than allowed and values that unpack into more
  than allowed. The sound system's recordings are cut and checked by the same pieces.
- The world's calendar, in J's Core: the day, the hour and the minute, the day of the week and the week, the season,
  the day of the season and the year, all read from the world's own clock. A year is four seasons (spring, summer,
  autumn, winter) of the same number of days, 28 unless the series' balance file says otherwise. J's Computers' cron
  and `at` jobs and its desktop clock read the time through it.
- Machines drawn with GeckoLib, in J's Core: a mod says once how a family of them looks (which model each is drawn
  with, by era or by kind, the folder of their textures and the animation they share) and declares it with the
  blocks, and one model of the Core draws every family, where each used to need a model class of its own. The Core
  runs without GeckoLib; a mod that draws with it brings it. J's Computers' Mainframes, racks, Pattern Encoders and
  drives, and the items that show them in a slot, are drawn this way.
- The small computers drawn as the cases of their age: the Personal Computer, the Crafting Computer and the Cluster
  Management Computer fill their block as a real tower of each age, the left side a panel of its own: a beige AT
  minitower with its MHz display for the Vintage, a beige ATX tower for the Legacy, a silver and gloss black mid tower
  for the Transition. From the Standard age on each comes in three cases, a block each, that differ in look alone:
  Neutral, closed and quiet; High Performance, mesh and more fans; Aesthetic, glass and light, with red LEDs in the
  Standard and addressable light in the Advanced. A plate at the foot of the front tells the machines apart. The
  Transition and Advanced machines are new blocks, and so are the High Performance and Aesthetic cases of the Standard.
- A small computer's left side comes off, to see what is inside: sneak and use the case with an empty hand, or press
  SIDE beside the inventory on its assembly screen. It stays as it was left, through a save and for every player.
- Inside a small computer, each part the player installed is drawn by a model of its own, the one of that item: the
  board on the tray, the processor in its socket, the memory in its slots, the cards hanging in theirs, the supply
  and the disks where the case of that age keeps them, an M.2 drive on the board; and on the processor the cooler
  the case brings, unless the processor is a Slot 1 cartridge with its own. A part of another age sits where this
  case puts it, and a supply made for the other end of the case is turned over, without the leads made for its own
  board. In an Advanced case the hard disks sit in the cage under the shroud, hidden like the supply, and the helium
  disks above 8 TB are drawn each with its own sealed lid and label. The parts are drawn when the side is off, or
  through a case's glass or mesh. A board for two processors has no place in a small case yet and is not drawn.
- A small computer's power lamp is lit while it runs, and its disk lamp blinks while it works its disk, whatever the
  disk. Its fans turn while it runs: the case's own, and those of the parts in it, the cooler's and the cards'.
- The Transition and Advanced Mainframes. The Transition's cabinet is gloss black after the IBM z9 and z10, with
  brushed-silver fins down its sides, a honeycomb service door, a silver badge and a blue slit of light down the
  middle of its front; the Advanced's is near black after the z14 and z15, its door a perforated sheet folded into
  facets, with a line of white light up each side. Each takes the MTX board of its own age, and their lights come on
  while the machine runs on a build that makes a computer.
- The Transition and Advanced Server Racks and servers. The Transition rack is gloss black with brushed-silver posts,
  honeycomb rear doors and a header with a silver badge and a blue lamp; the Advanced rack is the era's white with
  hidden screws, finely perforated rear doors and a white line on the header. The Transition 1U server, after the HP
  DL360 G5 and the Dell PowerEdge 1950, has a brushed-silver face with four 2.5" drives, a slim DVD and a blue power
  light; the Advanced one, after the HPE DL360 Gen10 and the Dell R650, is black with slim NVMe carriers, a honeycomb
  vent and a white light. Both take two processors, four drives and a gadget, the Transition's two cards and the
  Advanced's any number; each seats in a rack of its age or later, and every rack draws every server it can seat in
  that server's own shape. Their items and their empty cases show the server's face.
- A Server Rack's roof fans turn as fans: the housing stays still and only the rotor turns in it.
- The Advanced Supercomputer Rack and its node. The rack keeps the Standard one's shape (the hollow shell, the cooling
  unit on its roof with the coolant going in and out on different blocks, the livery panel over the service opening,
  a lit band per row) in the era's near black with white light, its livery a white trace stepping across it like a
  circuit. The Advanced Supercomputer Node, 2U, has four accelerator sleds with white latches, two NVMe carriers, a
  honeycomb vent and a white line on its ear; it takes the Advanced parts, up to four cards (the co-processor and
  three GPUs), and seats only in its era's rack, which seats the Standard node too and draws each node as it is.
- A rack refusing a chassis of a later era than its own now says so, instead of telling the player to take it to the
  kind of rack they are already at.
- The Network Operations Engines. What makes a storage network work is software its Mainframe runs: every new
  Mainframe, of every age and system, ships with the Midsoft IQL Server installed and running, in the version of its
  age (4.2 on a Vintage Mainframe, 2000 on a Legacy, 2008 on a Transition, 2012 on a Standard, 2022 on an Advanced).
  It is the IQL Engine of before under its own name, and it keeps the views and procedures. Everything that asks the
  network for work (the Network Interactor, the terminals, the buses, the gateways, the craft dialogs, the programs
  and the network's language) comes in by one door, the Network Operations Service, which hands it to the engine
  running; computers bind to the network, not to an engine. With no engine running the network still pulls, pushes,
  moves, exports and fills, but crafting, a craft's plan and the language are refused with "Network Operations
  Service unavailable", and the Network Interactor offers nothing to craft. A running engine weighs its memory on
  the Mainframe.
- An engine is the Mainframe's choice: a Subframe that runs another engine lends its Mainframe neither capacity nor
  queues until the two run the same one. Changing or stopping the engine hands only new work to the new one (or to
  none): every Operation already made carries on, a craft on the plan it was made with, after a reload too.
- A software house's tools are written for its own engine. The Network Management Studio opens only on a network
  running the Midsoft IQL Server, from its 2000 version on; anywhere else it stays installed and says "No compatible
  Midsoft IQL Server was found on this network." An engine is a package of its own kind.
- The jobs are the Automation Engine's alone: "at this time, or when this happens, do that", the same whichever
  engine plans the network's work. A job is made only where the Automation Engine is installed (IQL's CREATE JOB says
  so where it is not) and fires only while it runs. The Automation Engine and its Manager now reach back to Frames
  XP, so a Legacy network keeps its jobs. The Manager's "Keep Stock" is now "Restock below", and it says it does
  not count what is already on its way.
- The network's language reads what it was always written with. A definition (a view, a procedure, a job) typed at
  the prompt's `iql` or written in a file of statements goes to the engine, where it used to answer "iql failed:
  null" or stop the file. An `IF` decides whether an action runs at all, reading the network's holding of its item.
  An `ORDER BY` sorts every row before the `LIMIT` takes the first ones. A `WHERE` on a `SELECT`, a `MOVE`, a
  `DELETE` or a `DROP` picks the variants it touches (a damaged tool, a name), where it used to be read and ignored
  without a word.
- `install iqlengine` installs the engine the way every program is installed, from its disc at the Mainframe,
  written down with the system so the memory it holds is counted; it used to switch it on from any computer with no
  disc.
- The Network Manager has a Services tab. At the top, the engine that plans the network's work: its state, the
  Mainframe it runs on, its dialect, the memory it takes, how long it has been up, how many requests it planned
  today, its indexes and what it can do, with Stop and Replace (Configure is there and does nothing yet). Under it,
  the engines installed, the Subframes (one that runs another engine is marked "takes no work", and a note says what
  would put it back to work) and the other services, the Automation Engine and the Mirror. With no engine running
  the card says what still works and offers Start.
- Replacing the engine asks first, saying where new requests will go, that the Operations in flight finish on their
  plans, that scripts in the old maker's own statements may stop working, and which Subframes will take no work.
  Then it takes time, more for a network holding more kinds of item (from two seconds up to two minutes), and the
  window shows the steps with a bar; the network has no engine until the new one is up. A replacement under way is
  written down with the Mainframe.
- The Network Manager opens wider, so its seven tabs fit, and tabs that do not fit their strip give up some of
  their padding instead of running off its end (J's Core).
- The data cables are lines by job, with a cable of each line for each era. Access, which joins the small computers
  to a router: Thin Coaxial, Ethernet, Cat 5e, Gigabit Ethernet and Cat 6a. Backbone, which joins the routers, the
  Mainframe and the racks: Thick Coaxial, HBW, 10GBASE-CX4, Fibre Optic and OM5 Fibre. Long distance, which will join
  two networks between Gateway computers: the Telephone Line, the Leased Line, the T3 Line, VLDC and Dark Fibre.
  High compute, for a supercomputer's nodes: InfiniBand from the Transition, High Compute and OSFP. And one Crafting
  Cable for every era. Each wears its own jacket and ends in its era's plug where it meets a device.
- A machine takes its own era's cable of a line and every earlier era's, never a later one's, and two eras of one
  line touching do not join: they meet at a router of the newer era.
- Speed and range count on every line. A run longer than its cable reaches carries nothing, so whatever is only
  beyond it is off the network; an Operation reading a server moves no faster than the slowest cable between the
  Mainframe and that server. The speeds and ranges are first estimates.
- The backbone's fibre runs only straight: a cable laid against its side is left out, and the optical router is
  what turns it. A long distance line runs between two ends and takes no third, is thicker than the other cables,
  and shares no block. The long distance cables have no use until the Gateway computers come.
- Cables that keep a shape, in J's Core: a cable can be declared to run only straight or to join at most so many
  faces in a block, and it keeps the joins it already has when another cable is laid against it.
- A router for every era: the Vintage Router, the Personal Router of the Legacy, and the Transition, Standard and
  Advanced Routers. Each joins its era's access line to its backbone, and takes the cables of both lines of its era
  and of every earlier one on any of its six faces, all of them one network.
- The Optical Router, from the Standard, and the Advanced Optical Router take only the backbone's fibre, from the
  Standard's to their own era's, and are where it turns and branches.
- A repeater for every era, from the Vintage Repeater to the Advanced Repeater. The access, backbone, high compute
  and crafting cables run through it each on its own, never joining one another there, and each run starts its
  reach over at it, so a cable can go twice as far with a repeater halfway.
- The routers, optical routers and repeaters wear the same face on all six sides, the connector of their era in the
  middle and two blinking lamps above it.
- The small computers reach the network through a router: the access line goes from them to a router, and the
  Mainframe and the Server Racks take only the backbone.
- The Advanced HBW Interface, the Advanced supercomputer's uplink, in the era's white with finned OSFP cages: it takes
  the OSFP fabric and every earlier high compute cable, and the backbone up to OM5. A Cluster Management Computer
  reaches the Advanced supercomputer through the Fabric DPU.
- An HBW Interface finds its nodes along any high compute cable of its era or an earlier one, in each Supercomputer
  Rack that takes that cable: the Transition's InfiniBand reaches the Standard's racks, and the OSFP only the
  Advanced's.
- The Optical Network Card, from the Standard. The backbone's fibre enters a Mainframe, a Server Rack or a Cluster
  Management Computer only while the machine holds one, a rack when any server seated in it does; without one the
  machine takes the copper of its backbone alone. It fits a PCIe slot and is drawn inside the computers that seat it.
- The Network Manager shows how each node is linked. The Devices tab has a LINK column with the cable and its speed,
  and a square for an Optical Network Card; a machine joined to the network that lost its link is listed as "no
  link", and the line under the list says why: its fibre bends where no Optical Router turns it, or a run is longer
  than its cable reaches. The Map draws each link by its line (the fibre thick and aqua, an earlier era's cable in
  dashes, a link down red), with its speed, the Optical Routers the fibre goes through and a legend; a node's card
  has a NETWORK section. The Hardware tab, which now scrolls, adds the optical links up and down, the backbone and the
  slowest link in use.
- The data network tells what lies cut off beyond a run too long, and how long each run is, in J's Core.
- Buses for every era. The Import and the Export Bus come in a Vintage, a Legacy, a Transition, a Standard and an
  Advanced make. The Vintage bus moves an item a tick, one kind at a time, with no filter; the Legacy one eight, with a
  filter of up to five items (only these, or all but these), what the faced chest keeps and how many a move takes; the
  Transition one sixteen, with a keep and a max for each item it lists; the Standard one thirty-two, with a priority
  over the network's other buses and conditions to wait for (the network's stock of an item, the hours of the day,
  another bus having finished); the Advanced one sixty-four, filtering by tag as well and matching loosely, an item
  whatever its damage and components. The speeds are first estimates, and no bus moves faster than its cable carries.
- A storage bus mounts on an access or a backbone cable of its era or an earlier one.
- A bus can be switched off, and keeps a log of what it did lately: each move it made, and each time it held back and
  why (what the chest keeps, no room, a condition). What a program set on a bus is marked with the program's name.
- Each bus's window has three tabs, in its era's skin: Configure, the rows its era can be set to, which scroll above
  the inventory when they do not all fit; Activity, what it moved and why it held back, at the hour it happened; and
  Software, its address and the IQL statements and the calls that set it as it is set, with which programs set what. A
  setting a program set carries the program's mark until a hand changes it in the window.
- Conditions and tags are written in the bus's window: the network's stock of an item or a tag, the hours of the day,
  another bus having finished.
- A Crafting Input or Receiving Bus routes its face by every item its filter lists, up to five, and its window wears
  the skin of the Mainframe that commands it.
- A menu's player inventory can be shown on some pages of a screen and hidden on the others, in J's Core.
- The External Storage Bus, of every era: it moves nothing itself, the network uses the inventory it faces as
  storage of its own, ten times slower than its servers. The Vintage one shows the network all of it; from the
  Legacy, a filter decides what the network sees, and the network may read and write it, only read it, or only write
  to it; from the Standard, a priority decides which storage the network fills first; the Advanced one filters by
  tag and matches loosely as well. Its window tells how many slots the inventory has and how many are in use.
- What comes into the network fills the storage of the highest priority first, then the fastest.
- IQL sets a bus by its name: `SET BUS 'Ore in' KEEP 16 MAX 64`, and the filter (only these, all but these, by
  tag), the loose match, each item's own quantities, the mode, the priority, on or off, an External Storage Bus's
  access, and what it waits for (the network's stock of an item or a tag, another bus); a job that switches a bus on
  between two hours of the day keeps it to those hours. A name in single quotes is read as one, as in SQL.
- Programs set buses too, in both languages: `bus("Ore in").Keep(16).Max(64);`, with a call for every setting. A
  setting set from software carries the name of the program or the job that set it, which the bus's window marks,
  and what the bus's era cannot be set to is refused as it is in the window.
- Each bus has a model of its own: a metal clamp round the cable, a ring in its kind's colour (green the Import,
  orange the Export, violet the External Storage, brown the crafting buses), a funnel opening to the inventory for
  the buses that take from it, a nozzle for the ones that put into it and a wide thin panel for the External Storage
  Bus, chevrons along its sides pointing where the items go, and a plate against the inventory with its mark over its
  era's grille. Its housing is in its era's colours, the crafting buses' in the crafting line's amber, and so is its
  item.
- A bus's four lamps blink while it moves items, the External Storage Bus's while the network reads or writes
  through it and the crafting buses' while a machine is fed or emptied through them, and go dark a few seconds after.
- A part can have a model for while it is at work, its lamps lit and blinking by an animated texture, in J's Core;
  the block is drawn again only when the part starts or stops working.
- Peripherals take ports by kind, as on a real computer, in place of one count of ports on the board: a monitor takes
  a video output of a graphics card (one on a Vintage card, two on a Legacy or Transition one, four on a Standard or
  Advanced one), a pair of speakers the audio output of the sound card, or of the board from the Transition on, and
  every other peripheral a device port of the board (two on the Vintage, then four, six, eight and ten). A server
  board has a video output of its own for its console. A machine without a graphics card has nowhere to plug a
  monitor. A peripheral with no free port of its kind waits unlinked and links when one frees; taking out a card
  unlinks the monitors it fed. The numbers are first estimates.
- Port kinds for peripherals, in J's Core: an owner has so many ports of each kind and a peripheral takes one of its
  own; an owner that loses ports unlinks the peripherals it linked last.
- A peripheral cable for each era, laid in the shared cable block beside the data cables: the Vintage beige cable
  (DE-9 at a screen, DB-25 at a device), the Legacy black one (VGA and USB), the Transition white one (DVI and the
  white USB), the Standard braided one (HDMI and USB 3) and the Advanced space grey one (DisplayPort and USB-C). Each
  ends in the plug of the port it enters. A run reaches 8 cables on the Vintage, then 12, 14, 16 and 20. A port takes
  its era's cable and every earlier one, so a newer computer takes an older device's cable and an older device never
  takes a newer one; cables of two eras never join. A peripheral takes the cable on its back, where its model has the
  port, and a computer on any face; a peripheral against its computer still needs no cable. The one Peripheral Cable
  block is gone: the Standard's cable keeps its id.
- The Network Gateway takes the Vintage serial cable, which every computer takes.
- The peripheral line in J's Core: a cable can end in a different plug for each kind of port, and the peripheral
  links follow the peripheral wires of the shared cable block, each run as far as its cable reaches.
- A hub for each era, a plain block with six equal faces in the network tab: cabled to a computer, it takes one of
  its device ports and offers its own, two on the Vintage switch box, four on the Legacy and Transition USB hubs and
  seven on the Standard and Advanced ones. Every device cabled to a hub takes one of the hub's ports; when they are all
  in use the next device waits, or links another way that has a port free. Screens and speakers do not pass through a
  hub. The cable after a hub reaches as far again as the cable before it, and a hub may hang from another hub. A hub
  taken away drops the devices behind it.
- Hubs in J's Core: a peripheral can offer its owner more ports, and the link validator finds each peripheral's way
  through the hubs linked to its owner, keeping with every link the hub it hangs from.
- A Redstone Interface for each era, in the devices tab: a small sensor on the peripheral cable, drawn as the sensor of
  its day, that lets its computer read a redstone signal or emit one. It is placed as an observer is, its lens toward
  what the player looks at, any of six ways, and only its lens reads or emits; emitting, it powers the block in front
  as a lever does. Its lens shows the strength it reads or emits, its green lamp that it reads and its amber one that
  it emits. It takes a device port, or a hub's, and with no computer to power it reads and emits nothing.
- The Redstone Interface's window, in the skin of its era: its name, unique among its computer's interfaces, the
  computer it hangs from and the signal at its lens, the IN and OUT buttons, the strength it emits chosen by clicking
  one of sixteen cells, and the same setting written in IQL and in Sigma. What a program set is marked with its name,
  and a setting made in the window clears the mark.
- Programs set the Redstone Interfaces of the computer they run on: `redstone("Gate").Out(15)`, `.In()` and
  `.Level()` in Sigma and Sigma#, and `SET REDSTONE 'Gate' IN` or `SET REDSTONE 'Gate' OUT 15` in IQL. A setting is
  marked on the interface with the program's name, as on a bus.
- The Sigma documentation describes the buses and the Redstone Interfaces.
- Two Pattern Encoders for the new eras: the Transition one, a LightScribe DVD burner in silver and gloss black that
  writes DVDs and CDs, and the Advanced one, a Blu-ray writer in space grey with a white front that writes Blu-ray
  discs and, in its USB-C port, sticks. And the Blu-ray Drive, white all over with its slim tray high on the front,
  which reads Blu-ray discs and the older DVDs and CDs. Each takes its era's cable on its back, and each opens its
  tray with the sound the other disc drives make.
- A speaker for every era from the Legacy on. The Transition has the Artisan Inspira 2.1, gloss black satellites
  with a silver-framed mesh front, and its subwoofer, a block of its own with the bass port in front and the driver
  on its sides. The satellites play everything but the bass, and a subwoofer set against one of them, with no cable
  of its own, gives all of its computer's satellites the whole range; the speaker's window says which. The Standard
  has a new pair, the Artisan WattWorks T20, tall and gloss black with the tweeter over a silver-ringed woofer, and
  the Cobble moves to the Advanced. Each speaker takes half of an audio output, the window names its model.
- Disabled devices, in J's Core and J's Computers: a computer can disable a device on its ports, which stays linked
  and keeps its port while the computer neither reads nor writes it, and everything hanging from a disabled hub with
  it. A disabled monitor goes dark and opens none of its computer's screens, a disabled speaker plays nothing, a
  disabled drive's disc is not seen, a disabled Redstone Interface neither reads nor emits and no program finds it,
  and a disabled Gateway answers nothing. The computer remembers what it disabled.
- The device tools at a Unix prompt: on Linux `lspci` lists what sits on the board (the board itself, its port
  controller, its audio and network, the graphics and expansion cards) and `lsusb` what is plugged into its ports,
  `lsusb -t` as a tree with each hub's ports under the hub and the free ports too; on FreeBSD `pciconf -lv` and
  `usbconfig`. A device the computer disabled is marked by each.
- The Device Manager on the Frames desktops: the machine's hardware and every port it has, what is plugged into each
  and which are free, hubs with their own ports under them, and how many device ports are in use. Devices are listed
  by port, by type or by connection, and a device is disabled or enabled again from it, crossed out while disabled.
  Frames 95 keeps it in System Properties with the views as radio buttons and the actions as buttons under the tree;
  Frames XP has its console window, the views in the View menu, the actions on a right-click and in the Action menu,
  and a status bar that says what disabling a device does; Frames 11 has the views as a switch in its menu row.
- The Midsoft Diagnostics on MC-DOS and MC-NET, which have no Device Manager: `msd` takes the whole terminal and
  shows what the machine is made of (its processor, memory, graphics card, network, system and drives), and LPT
  Ports and COM Ports open the list of the board's ports with what is attached to each and whether it is on, where a
  device is enabled or disabled from the keyboard.
- KDE's Info Center has a second page, Devices by port, beside About this System: every graphics card with its
  outputs, the audio and the board's ports, each port beside what is plugged into it or free, a hub with how many of
  its own ports are in use, and a button that disables or enables the device selected.
- Text shadowed by the ground it is written on, in J's Core: whoever paints a background says which colour it is,
  and text drawn over it takes its shadow from that ground: never the letter's colour, never the ground's, and never
  a colour foreign to the letter. Where nobody said, the text is taken to be on the ground it reads best on. The
  Core's components, era themes, dialogs and tables and J's Computers' desktop skins say what they paint, so every
  label, field, menu, list, button and tab drawn through them has its shadow. Centred text on the machine screens
  no longer uses the game's dark shadow, which smeared on the light panels of the Legacy and Advanced eras.
- A sound system for the whole series and its addons, in J's Core. A mod declares a sound once (where it is heard
  from, whether it loops, its channel, its files, how far it carries, its subtitle) and its registration,
  `sounds.json` entry and translatable subtitle follow from that one line. Sounds are mixed in channels (machines,
  devices, interface, alerts, ambience, music, voice) with a volume each, and any sound of the game, not only the
  series', can be turned off in `config/jstech-audio.json`. The same sound from the same place plays once in a
  short while, however many times it is asked for.
- The sound system keeps the world's running sounds: a machine says what it wants heard (a fan, a disk) at the
  volume and pitch its state calls for, and the client starts, retunes, fades and stops the sounds to match. It
  keeps to a share of the game's sound channels, the sounds that matter most and are nearest first; many machines
  of one kind close together are heard as one room, louder the more there are; and walls between a sound and the
  listener muffle it, which the player may turn off.
- The sound system plays sounds made as they play: a synthesiser (square, pulse, triangle, sawtooth, sine and
  noise, note by note, the way a PC speaker or an early sound card makes a tune) and recordings read from files
  that are not the mods' own, WAV and Ogg Vorbis to begin with and any other kind a mod registers a decoder for.
  The server sends a tune as its notes, heard by the players near where it plays, and each client makes the
  sound itself. Such a sound has a subtitle and a channel like any other and the player can turn it off the same
  way; heard from a place in the world, a stereo recording is played in mono so it comes from that place.
- A sound can come out of several places at once, each playing one side of a stereo recording or both, and each
  as well as the speaker there reproduces it: no faster a sample rate and no more bits than it manages, with its
  bass and treble cut where it cuts them. A mod marks a recording as stereo, and a sound device's own limits (a
  card that plays in mono at 8 bits) apply on top of the speaker's.
- A "Turn Off Last Sound" key in the game's controls, under J's Tech Series, with no key until the player picks
  one: it turns off the last sound heard around the player (never their own footsteps or a click of the screen)
  and says which on the action bar; pressed again within five seconds, it brings that sound back.
- While an alert plays, the series' other sound channels are lowered so the alert is heard over them, and come back
  up over a second once it ends; the player may turn this off in `config/jstech-audio.json`.
- Sound cues: a mod says what happened (a computer powering on) and the sound heard is picked on each client from
  `assets/<namespace>/sound_cues/`, by the context it happened in (the machine's era, its audio device, its
  system's family). A resource pack binds a cue to other sounds, the game's own included, by shipping a file of the
  same name. A machine with sound hardware of its own plays cues and tunes from each of its speakers, at its own
  volume, as its device allows: a PC speaker turns every note into a square wave, and a machine with no sound
  hardware stays silent.
- The Sound Mixer, reached from Sound Mixer... beside Done in the game's Music & Sound Options. Its Channels tab has
  a slider for each channel (machines, devices, interface, alerts, ambience, music, voice), with a tooltip saying
  what the channel carries and which of the game's volumes it also follows, and moving one reaches the sounds
  already playing; a song or a loop playing while its channel is turned all the way down goes quiet and is heard
  again when the channel comes back up, where the game would have stopped it for good. Its Sounds tab lists every sound the game knows, the game's own and every mod's, searchable by
  name or id and filtered to all, recent, the series', the game's or those turned off, each with Play to hear it
  once and ON/OFF to turn it off, and Turn All Back On. Its Options tab turns walls muffling sounds, alerts shown on
  screen and the lowering under alerts on and off, and opens the game's controls to pick the Turn Off Last Sound
  key.
- Alerts on screen: a player who turns them on sees each alert as a sign at the top of the screen, saying what it
  is and pointing to the side it comes from the way the game's subtitles do; it blinks three times as it comes up
  and goes after four seconds.
- The game's debug screen (F3) shows the sound system under Sound Mixer: the running sounds out of their budget,
  the rooms many machines make, what walls muffle, how far the other channels are lowered under an alert, the last
  sound heard and how many sounds are turned off.
- J's Computers' machines make their sounds, from real recordings. A computer's power button clicks; a Vintage or
  Legacy computer beeps once when its self-test passes; a Vintage computer comes on with the noise of its fan and
  drives; a computer with a hard drive hears it spin up, turn while it runs and wind down when it goes off, while
  one with a solid-state disk stays quiet. A monitor sounds as it lights. The drives sound as media go in and come
  out: a floppy disk slides in and is ejected, a disc rides the tray of a CD or DVD drive and of the Pattern
  Encoder, a USB drive is plugged into and pulled out of a Dock Station, and a Floppy Drive's head is heard
  stepping while a system or a program installs from its disk. Each running server in a rack is heard by its fans,
  and five or more running close together, in any number of racks, are heard as the hum of the room instead.
- Four sound cards, one for each bus of the Vintage and Legacy boards: the Artisan Tone Blaster (ISA) and Tone
  Blaster 128 (PCI), which make their notes by FM and play recordings at 8 bits in mono at 22 kHz, and the Tone
  Blaster Live (AGP) and Tone Blaster Hi-Fi (PCIe), wavetable cards playing recordings at 16 bits in stereo at
  44.1 kHz. A sound card sits only on a board of its own era, in a slot of that board's bus, and a machine takes
  one; a Standard board has its sound built in, and its tooltip says so. A computer's tooltip says whether its case only beeps or its board
  plays everything, and a monitor's that its computer's sound comes out of it.
- Frames 95, XP and 11 have their own sounds: a chime when the desktop comes up, the same chime when the system
  shuts down, and an error sound when it raises an error box. They come out of the computer's monitors and
  speakers, heard by everyone near them, and only through a sound card or the sound on a Standard board: a machine
  with neither, or with no monitor and no speaker, only beeps. They are sound cues picked by the system a machine
  runs, so a resource pack can give any other system chimes of its own.
- The Unix desktops have their own sounds, taken from the sound themes they ship: GNOME and Cinnamon the
  freedesktop.org theme's error, notice, bell and device sounds, and GNOME on a Legacy machine the start-up,
  shut-down, error and notice of its second series; KDE Plasma the Ocean theme's chimes, error, notice, bell and
  device sounds, and on a Legacy machine the Oxygen theme's. GNOME today, like the real one, makes no sound coming up
  or going down. The Frames editions gain a notice and a bell, and XP and 11 their device sounds. A system sounds its
  notice when its desktop raises a notice, and when a craft, a program's setup window or a build in Virtual Studio
  finishes; its error when a craft, a query or a build fails; its bell when a click lands beside a box waiting for an
  answer, or a Unix shell prints the bell character; and its device sounds when a USB drive goes into or comes out of
  a Dock Station of a computer up at its desktop, or when its network comes up or goes down. The systems with no
  bell of their own (MC-DOS, MC-NET, FreeBSD and UNIX at their console, and CDE) ring the speaker in the case
  instead, as does a machine with no sound to play one.
- More machine sounds: a Vintage or Legacy computer switched on with parts that do not make a computer (no
  processor, no memory, parts its board does not take) beeps its self-test failing; a hard drive is heard seeking
  while its machine boots, installs, or reads and writes files on it; a Vintage or Legacy monitor is heard going
  dark; a server or a rack unit slides into and out of its bay on its rails, and a rack that is broken drops them
  quietly; a CD or DVD drive is heard turning while a system or a program installs from its disc; and a running
  Vintage Mainframe's tape reels are heard turning.
- The minefield clicks as its cells are opened and flagged, goes off when a mine is opened and plays a jingle when it
  is cleared, out of the computer's monitors like the system's sounds.
- The Unix shells' `echo` takes `-e`, reading `\a` (the bell), `\n`, `\t` and `\\` in its text.
- Speakers: the Artisan ToneWorks (Legacy) and the Artisan Cobble (Standard), linked to a computer over the
  peripheral cable like a monitor, each taking one of its board's peripheral ports. A computer's sound comes out of
  its monitors and its speakers; with two or more speakers, the one to the left of whoever sits at its monitor plays
  the left side of a stereo recording and the one to the right the right, and a speaker alone plays both. A
  ToneWorks plays at 22 kHz with its bass and treble cut, a Cobble the whole range. Using a speaker opens its
  screen, in its era's look: the name a program finds it by, taken when the screen closes and refused while another
  speaker of the same computer has it, whatever the case of its letters; the computer it plays for; the side it
  plays; and how well. Set down, stepped on or broken, a speaker sounds like metal, as the other devices do.
- Each system keeps its own sound settings on its disk: how loud it plays, whether it is muted, and where its sound
  goes, out of the monitors, the speakers or both (both on a fresh system). A choice with nothing linked to play it
  gives way to what is linked, so a machine with somewhere to play is never silenced by it. On every system,
  `config volume 60`, `config mute on` and `config output speakers` change them and `config` lists them. They
  touch only what the system plays; a machine's own noises, its drives and fans, stay as they are.
- Every desktop turns its sound from the speaker on its panel, each in its own way: Frames 95 and XP open the small
  Volume popup with an upright slider and a Mute box; Frames 11 its quick settings, with an arrow that opens the
  choice of output; KDE Plasma its Audio Volume applet; GNOME its system menu, dropped from the top bar; Cinnamon its
  sound applet with a Mute output switch; and the period KDE and GNOME of a Legacy machine the mixer popup, whose
  Mixer button goes to the settings. The speaker wears a red cross while the system is muted, tells the volume when
  the pointer rests on it, turns it a step for each notch of the wheel, and opens a menu with Sound settings on the
  right button. CDE, which keeps no speaker on its panel, has an Audio page in its Style Manager: the volume on a
  scale, Mute, and the monitor and the speakers as toggles that may both be on.
- The Settings window's Sound page: the volume, Mute, the output, what plays the sound (the sound card by its name,
  the board's own sound or the speaker in the case), the speakers linked with the side each plays, and Test, which
  plays the system's startup sound the way the settings now have it. A speaker's screen says it is off while its
  computer plays only out of the monitor.
- Recordings a server keeps, in J's Core, for any mod or addon to play. A player can bring a WAV or Ogg Vorbis file
  from their own computer to the server, which keeps it once under a name taken from its bytes, however many bring
  it, and reads its length and its title, artist and album from it; whoever takes a recording says whether it was
  put to use, and the player is told it went in only when it was. A recording played in the world is fetched by
  the players near it, kept in a cache on their own computer (`jstech/media-cache`, 512 MB at most, the longest
  unplayed going first) and heard out of the places it plays from, from wherever it has got to: a player who walks up
  to one already playing hears it from there, and one who walks away lets it go. It can be paused, taken up again
  and stopped. The server owner sets how fast recordings go to each player and come from them, and how big a file
  the server takes, or that it takes none, under `media` in `jstech-balance.toml`. The server remembers who brought
  each recording and when anything last used it: the recordings one player brought may take up to
  `player_quota_megabytes` of it together (512 unless the owner says otherwise, a recording it already kept costing
  nothing), `/jstech media` tells an operator how much it keeps, and `/jstech media prune <days>` takes out every
  recording nothing has used for that many days, leaving what a mod still offers and what is playing, and gives
  each player back the room theirs took.
- Music files on a computer's disk: Ogg Vorbis (`.ogg`) and Wave (`.wav`), with an icon of their own in every
  desktop's style. A song is brought from the player's own computer, picked in their system's file dialog, into
  the system's music folder (`Users/Public/Music` on Frames, `Music` in the home folder on Linux, FreeBSD and
  UNIX), and is refused before it is sent when the disk has no room for it; one that finds the room gone by the
  time it has arrived is said not to have gone in, rather than to have. The recording stays on the server and
  the disk keeps a file naming it, but that file weighs what the song weighs, by the disk's era like any other
  file, and an archive with songs packed in it weighs them too; the Archiver shows their sizes in megabytes. A song
  cannot be edited or added to, renamed into text, or made out of text.
- The server's music catalogue: the albums its owner puts in `config/jstech/soundfoundry/catalog/`, a folder
  each, and those a data pack carries in `soundfoundry/catalog/`, named by an optional `album.json`, else by what
  their songs' tags say, else by the folder. It is read when the server starts and when its data packs are
  reloaded, and `/soundfoundry catalog reload` reads it again for an operator, saying how many albums and songs it
  found; a file it cannot read is passed over and named in the server log. `catalog` under `[soundfoundry]` in
  `jscomputers-server.toml` turns it off. The documentation has a page on music on a server.
- Soundfoundry, Voidsoft's music player, for the desktops of the Legacy era and later. Its playlist and how it
  plays belong to the machine, so the music plays on with the screen closed, the next song follows when one ends,
  and it stops when the machine goes off. It plays out of the machine's monitors and speakers as its sound hardware
  plays them, at its own volume on top of the system's, with a balance that turns the left or right speaker down.
  It shuffles through the whole list before a song comes round again, repeats the list or stops after the last,
  and keeps playlists as `.m3u` files. Songs open in it, and songs brought from the player's own computer can go
  straight onto its playlist. A machine whose sound only beeps has nothing to play a song on, and says so.
  Its window wears a skin of its own, the same on every desktop: dark iron plates with rivets and displays lit in
  amber, the time in seven-segment figures, an analyser whose bars move with what is being heard, the song
  scrolling by with its rate and whether it is stereo, where the sound comes out, sliders for the volume, the
  balance and the point in the song, and the buttons to play, pause, stop and skip. The eject button opens a song
  or a folder of them, or imports songs from the player's own computer; the playlist under it adds, removes, picks,
  sorts and keeps lists from its buttons, plays a song on a double click, and can be put away with PL, and either
  part folds down to its bar. The keys Z, X, C, V and B go back, play, pause, stop and go on.
- Soundfoundry's sharing window, which NET opens, in the same skin: a search finds songs by their title, artist,
  album or file name among Voidsoft Music, the server's catalogue sold as the online store of the time, and the
  songs the other computers of the network running Soundfoundry keep in the `Shared` folder of their music folder,
  each listed with its size, the computer it is on and the slowest cable on the way to it. A song picked and
  downloaded comes in at that cable's speed (Ethernet half a megabyte a second, HBW two, HPC eight, the songs coming
  in at once sharing it; a catalogue song at the speed of the computer's own cable), with its progress and time left,
  and is kept in the music folder, and on the playlist when asked. It waits while the computer sharing it is off,
  goes on after the world is loaded again, and is given up on, saying why, when the song stops being shared. The
  downloads and the songs this computer shares have a tab each, and the bar along the foot says how many songs it
  shares, how many computers it reaches and how many songs are on their way. The speeds are under `[soundfoundry]`
  in `jscomputers-server.toml`. A window now opens inside the desktop's work area wherever it fits.
- The data network knows the slowest cable between any two points of it, in J's Core, for any mod or addon to ask:
  of every way between them, over cables and routers and through the Mainframes and racks that join cable runs, it
  takes the one whose slowest cable is fastest, which is as fast as data can go between them. A router slows
  nothing down, and a Mainframe that is switched off passes nothing through.
- Soundfoundry Server, a Voidsoft service for a server in a rack: it streams the music catalogue and the network's
  own library, which is the songs in the server's music folder, to the computers of the network. A song a computer
  sends it lands in a folder named after that computer. Each computer listening holds 4 MB of the server's memory,
  which the server's memory listings show, and a server with none left turns the next one away; a song playing
  from a server that goes off or leaves the network stops and says why. Several servers can serve one network.
- Soundfoundry on the Standard desktops, Frames 11 and the Linux desktops of that era, is Voidsoft's streaming player:
  a home page with the catalogue's albums, the network's songs and those downloaded; an album's page with its songs,
  each marked as on the disk or only streamed, and a button that downloads the album; a search through the
  catalogue, the network's library and the computer's own files; the computer's own files, brought from the
  player's computer or sent to the network's library; playlists, the liked songs first, kept as `.m3u` files; and
  the Soundfoundry Server streamed from, picked when there are several. Playing an album, a playlist or a page's
  songs puts them on the list; a song on the disk plays without a server. Songs download and go up at the speed of
  the slowest cable between the computer and the server. Its window wears Voidsoft's graphite and orange on every
  desktop, and is the first window drawn in its own skin that can fill the desktop.
- Covers on the Standard Soundfoundry: a catalogue album shows the `cover.png` (or `cover.jpg`) beside its songs,
  any other song the picture its own file carries, and a song with neither a cover made of its album's colours and
  initials. J's Core reads the picture an Ogg Vorbis file or a Wave file's ID3 tag carries.
- The data network tells which cables a device joins, in J's Core, which is how the way to a server in a rack is
  measured.
- Programs make sound. Sigma Sharp and Sigma have `Sound.Beep`, a beep out of the speaker inside the case;
  `Sound.Tones`, a tune through the sound card, written as notes by name or pitch (`C4:250 E4 G4 C4+E4+G4:500`)
  with rests, chords and lengths; `Sound.Play` and `Sound.Stop`, a song from the machine's disks out of its monitors
  and speakers; and `Speaker.Named`, a linked speaker found by its name, which plays a song out of itself alone. A
  tune holding something that is no note stops the program, naming it. A machine with no sound card plays its tunes
  out of the case, one square note at a time. The documentation of the language has a section on it.
- A computer's sound hardware plays as many sounds at once as it has voices: 9 on an FM card, 32 on a wavetable
  card, 64 on a Standard board, and one in the case. A stereo song takes two voices of a card that plays both sides
  and a mono one takes one, each note of a chord one and each chime of the system one. A sound that finds no voice
  free takes those of the sound that started first, which stops, and Soundfoundry says so when it was its song; a
  new beep cuts the one before it off. A paused song holds no voice.
- The sound system's synthesiser has two voices more, for any mod: the bright, metallic ring of frequency
  modulation and the rounder note played from a table of harmonics. A sound device says which one its notes take
  (an FM card the first, a wavetable card and a Standard board's sound the second), and a tune the server sent can
  be stopped before its end.
- Brazilian Portuguese (pt_br) for J's Core, J's Computers and J's Industrial: every word the mods show, from the
  blocks and items to the desktops, the programs, the terminals and the installers. The names of the fictional
  products and makers, the commands a player types and the files on a virtual disk stay as they are.
- Sixty-six advancements in four tabs. Hardware follows the machines, from the first self-test and the eras a
  computer can be built in up to racks, datacenters, clusters and supercomputers. Operating Systems has one
  for the first install of each system, the prompts and desktops, and challenges for installing every
  distribution and running screenfetch on every system that takes it. Networks & Operations covers joining a
  Mainframe, Operations, IQL, autocrafting and the ways a network goes wrong, and Sigma goes from installing the
  compiler to a program that stays up for a week. Some stay hidden until earned, and the one for hearing a
  ComputerCraft computer through a Network Gateway exists only when ComputerCraft is installed.
- An advancement goes to the player who caused it. What a machine does on its own, such as a self-test ending,
  a job firing or an Operation finishing, goes to whoever works that machine: the last player to place it,
  open its screen or build it, which the machine keeps across restarts. An Operation typed at a terminal goes
  to the player who typed it, however long it takes to finish and across a restart of the world. What a player
  earns while they are away is given to them the next time they join.
- Logos and icons for the series and for each mod. The mods list shows each mod's logo over the series logo,
  mod list screens that show an icon show each mod's own, and the READMEs carry the logos.
- screenfetch on Frames 95, XP and 11: a package of Frames' own manager, run at the Command Prompt, drawing the
  Frames flag with the version each gives for itself. Professional Larper now asks for all nine systems it
  installs on.
- The ports tree on FreeBSD. `portsnap fetch extract` brings the tree from the network's Mirror and lays it out
  under `/usr/ports`, a folder for every program the Mirror serves, filed by category, as real files that take
  room on the disk; `portsnap fetch update` brings it up to date. In a port's folder, `make install clean`
  fetches the program's source from the Mirror and builds it on the machine, for as long as its processor
  takes with all of its cores, then installs it and cleans up. `make` on its own builds, a later `make install`
  finds that build instead of making it again, and `make clean`, `make reinstall` and `make deinstall` do what
  they do on FreeBSD. Building a port earns the new Built from Ports advancement.
- A program built from source on the machine that runs it asks ten percent less of it than the package does: less
  memory while it runs, a lesser processor, and less free disk to install. That goes for a port on FreeBSD and
  for everything Gentoo builds. The machine remembers which of its programs it built until the program is removed
  or the disk it was on is formatted; the same program installed again as a package asks what a package asks.
- FreeBSD installs through bsdinstall, in its own grey dialogs on navy. It opens with Install, then asks for the
  machine's hostname, whether to add the ports tree, the disk (the whole of an empty one, beside a system already
  on one, or the whole of one that is erased first, after asking), where `pkg` and the ports fetch from (the
  network's Mirror, or nowhere), which services start at boot (`cron` and `sshd`) and, when the Mirror answers, a
  desktop to install with it, None always offered. It extracts the system with a gauge and ends with Reboot.
  With no Mirror it fetches nothing, and a ports tree asked for is laid down empty for `portsnap fetch` to fill.
- UNIX installs from its own console installer, which names the parts of its one copy as it writes them, offers
  to erase a disk that already holds a system or to install beside it, and ends with Reboot.
- `cron` is a switch, FreeBSD's `cron_enable` in `/etc/rc.conf`, on unless bsdinstall turned it off. Off, the jobs
  in a machine's crontab wait instead of running, while a job put in the background with `&` runs anyway. FreeBSD
  says "Starting cron." as it comes up and FreeBSD and UNIX say "Stopping cron." as they go down, while it is on.
- On a FreeBSD installed through bsdinstall, other computers reach the machine only when `sshd` was ticked on the
  services page; it shows as `sshd_enable` in `/etc/rc.conf` and is the same switch as `config remote`. Every
  other system keeps letting them in, as before.
- Choosing a faster disk on an installer's disk page makes the copy quicker, and the time left changes to match.
- `ee` on FreeBSD, the editor it gives a newcomer: its five rows of shortcuts along the top, the row saying where
  the caret stands, and its Esc menu drawn over the text, whose items all work (leave the editor, saving or not;
  a page of every key, closed by any key; read a file in or save the one open; show or hide the rows of
  shortcuts; search). `vi` on FreeBSD and UNIX speaks each system's own, with no Vim status line. Both come with
  the system and are listed in `/usr/bin`.
- FreeBSD's console welcome names the three ways to find things, `apropos` for a command by what it does,
  `man intro` to learn the system and Tab twice for every command here, above the day's tip. `man intro` is a
  real page, and on every Unix console a second Tab in a row lists what the first could complete to, every
  command on an empty line, as sh and bash do. FreeBSD has no
  `help`: typed there, it is not found like any other unknown word, and the line points to `apropos` and
  `man intro` instead.
- KDE Plasma, GNOME and Cinnamon on FreeBSD hang FreeBSD's own wallpaper for each, in FreeBSD's reds with its
  orb, in place of the one they bring on a Linux.
- This PC on KDE Plasma, GNOME and Cinnamon, on FreeBSD and on the Linux distributions alike, is the page each
  desktop has for it: KDE's Info Center, GNOME's About and Cinnamon's System Info, each laid out its own way. They
  show the system with its release, the kernel and its build, the architecture (vel64 or IA-32 on FreeBSD, x86_64
  on a Linux), the desktop, the processor, the memory, the graphics card, the system disk and how much of it is
  used, and the machine's host name.
- The Personalize page of Settings scrolls when it holds more than its window does, such as every wallpaper a
  system offers along with the flat skin's taskbar and appearance rows.
- A Vintage machine runs what `scc` compiles, though it can never hold the Sigma Runtime: a compiled listing runs
  by its bare name at the prompt, on MC-DOS without its extension the way DOS finds a program, and on UNIX from
  the current directory or the PATH. `scc` says how to run what it just built, and UNIX answers a word it cannot
  find in its own shell's words, `hello: not found`.
- A resource pack can recolour the computing screens. Their colours are files under `assets/jscore/palettes/` and
  `assets/jsc/palettes/`: each hardware era's skin; each desktop's windows, panel and launcher; the chrome each style
  of desktop draws its title bars, buttons, fields and tabs with; CDE's eight colour schemes; the Network Management
  Studio; the terminals' inks, on glass and on paper; the code editors; and the accents Settings offers. A file gives
  a colour for each named role (`text`, `accent`, `panel` and so on), written `#AARRGGBB`. A pack's file may name
  only the roles it changes, and the others keep the colours the mod ships; a file that cannot be read is logged, and
  its screens keep those colours too. A change of pack reaches a screen that is already open. A theme preset still
  puts on the accent the mod ships, since the machine that keeps it knows no resource pack.

- Icons for every program on CDE: twenty-eight programs that run under it, from the Network Management Studio to
  Vim, Emacs and the desktops themselves, wore the Frames 95 icon there and now wear CDE's. The Help Viewer has an
  icon on every desktop it runs on, Workstation Info has one outside CDE as well, and a setup, the welcome and a
  window a Sigma program opens show their own icon on the panel and the title bar instead of the plain one.
- New wallpapers, built from blocks the way the game's own art is: a stepped grass hill under slab clouds for
  Frames XP, a blue flower drawn like an item for Frames 11, a wall of Breeze-blue blocks for KDE Plasma, a blocky
  dusk with a square sun for GNOME and green block stairs for Cinnamon; Frames 95 keeps its plain teal. Frames 11
  hangs a dark version of its flower while its dark theme is on. They are pictures a resource pack can replace,
  under `assets/jsc/textures/gui/wallpaper/`, and the thumbnails in Personalize are the pictures themselves.
- The rest of what the desktops drew out of rectangles is pictures too, in the same block style: each Frames
  edition's mark on its Start button, the sky Frames 95 starts on and the bands Frames XP welcomes and closes on,
  the marks KDE Plasma and Cinnamon come up behind, the pictures on CDE's front panel, the network and speaker
  icons of every panel, and the picture down the side of the Frames 95 wizard: a computer, a boxed program and a
  disk. The clock's hands, the calendar's day and everything that moves are still drawn over them.
- File icons are pictures in each desktop's own style: a Frames 95 folder is not a GNOME one. Seventeen kinds of
  file, from folders and documents to Sigma source, programs, packages and pictures, each drawn at 16 by 16 for
  every desktop, in the explorer, the file dialog, the studios' trees and on the desktop itself. They are files a
  resource pack can replace, under `assets/jsc/textures/gui/file/`.
- J's Core gives a block entity its state as fields declared once, each saying where it goes: into the save, to the
  players who see the block, to the menu open on it. The saving, the update the players are sent, the client's
  reading of it and the menu's data all follow from those declarations, and the players are sent one update per
  block entity a tick however many fields changed. Inventories, energy stores and fluid tanks are declared the same
  way, with whether pipes and cables reach them and whether they spill when the block is broken; so are a value of
  any kind a codec writes, a part that writes itself, a property of the block's state kept at what a value says, a
  peripheral's link to the computer at the other end of its cable, and what happens when the block is broken.
- J's Core declares a menu once: its slots in groups, placed from the screen's layout; where a shift-click in each
  group sends the stack; its buttons; and that it stays open while its block entity stands and the player can reach
  it, which holds for every variant of a block that makes the same block entity; the values it shows that the server
  works out; and the screen that draws it, which frames its slots and presses its buttons. A block that makes a block
  entity is declared the same way, with what it ticks and which menu it opens, and faces whoever placed it.
- J's Core registers every payload a client sends with a gate that checks, on the server, that the player really has
  that screen open on what the payload names; a payload that acts on the menu the player has open is handed that
  menu, already checked, and a refused one is dropped and noted in the server log.
- Two hardware eras join the ladder: the Transition, between Legacy and Standard (the late 2000s, 64-bit, blue
  BIOS, flat panels, systems and programs on DVD), and the Advanced, after Standard. Each has its own look on the
  computing screens: the Transition in navy glass, its headers, buttons and selected tabs in two bands over a line
  of sky blue; the Advanced light and flat, white panels on grey under a thin blue line. Both palettes can be
  recoloured by a resource pack like the others.
- Blu-ray discs, the Advanced era's medium: the BD-ROM and the rewritable BD-RE, each holding four times a USB
  stick. Advanced programs ship on Blu-ray, and its systems and services on the stick.
- Processors and graphics cards name the architecture they are built on in their tooltip, with the chip's
  codename: "Architecture: Centro Conroe", "Architecture: Kepler GK110".
- The Vintage era fills out, so a weak machine and a top one can be built in it: the Velocion 5x86-133 on Socket 3;
  the Integra Pentix 75, 133 and MMX 233 and the Velocion K5 PR133 on Socket 7; the Integra Pentix Pro 150, 180
  and 200 on Socket 8; and, on Slot 1, the Integra Celer 300A, Pentix II 300 and 450 and Pentix III 600, with the
  MF AT Slot 1 Motherboard, the top of the era, that takes them with SDRAM and the first AGP. New cards: the Atrion
  Wonder VGA, the Atrion Rave Pro and, on the AGP, the Envya Prism TNT. New memory: the SIMM-16, the EDO-32 and
  EDO-64, and the SDRAM-32 and SDRAM-64 the Slot 1 board takes. And the MF PowerBasic 200 and the Vaultis Trench
  HDD 200M.
- The Legacy era fills out the same way, a socket at a time: the Velocion Duro 1300 on Socket A; the Integra Celer
  2.0 and the Pentix 4 2.4C, 3.2C and EE 3.4 on Socket 478; the Celer D 325J and the Pentix 4 520, 540 and 560 on
  LGA 775; the Velocion Semper 3100+ and Sprint 64 3200+ and 3700+ on Socket 754; the Sprint 64 3500+, 4000+ and
  FX-55 on Socket 939; and the Integra Servo 2800, 3060 and 3200 for the servers on Socket 604, with an ATX board
  for each new socket. The Pentix 4 chips and the Servos run two threads a core. Thirteen new graphics cards run from
  the Envya Prism TNT2 M64 and the Atrion Radiance 7000 through the Vertex 4 Ti 4200 and the Radiance 9600 XT to
  the Vertex 6800 Ultra on AGP, and on the first PCIe boards the Vertex 6200 and 6600 GT and the Radiance X300 and
  X800 XT, the top of the era. And the SDRAM-256, DDR-256, DDR-1024 and DDR2-512, the MF PowerBasic 350, the
  Vaultis Link IDE-HDD 40G and the Artisan Tone Blaster Audigy, the sound card of the PCIe boards.
- The Transition gets its hardware catalogue: on LGA 775 the Integra Pentix D 805, the Celer E1200 and the Centro 2
  Duo, Quad Q6600 and Extreme QX9650; on AM2 the Velocion Sprint 64 X2 and the Ascent X4 9850; on AM3 the Sprint II
  and the Ascent X4 and X6, up to the X6 1100T; on LGA 1156 the Pentix G6950 and the first Centro c3, c5 and c7; on
  LGA 1366 the Centro c7 920, 960 and 980X and the Integra Servo 5520, 5570 and 5680; the Servo 5450 on LGA 771;
  and the Velocion Optera 2218, 8356, 8384 and 8435 on Socket F. A board for each: the ATX boards for LGA 775, AM2,
  AM3 and LGA 1156, the LGA 1366 workstation board, the two-way server boards and the Mainframe's MF MTX-T. Fourteen
  new graphics cards run from the Envya Vertex 7300 GS and the Atrion Radiance X1300 to the Vertex GTX 480 and the
  Radiance HD 5870. And DDR2 and DDR3 in three sizes each, a registered DDR2 for the servers, and the MF PowerBasic
  450B.
- The Standard fills out: on LGA 1155 the Integra Celer G530 and the Centro c3 2120, c5 2500K and c7 2600K and
  3770K; on LGA 1150 the Pentix G3258, the Centro c3 4160 and the Servo 1231 v3 beside the Centro c5 and c7; on LGA
  2011 the workstation Centro c7 3820, 3930K, 4930K and 4960X and the Servo 2609, 4650 and 4657L v2; the Velocion
  FX-4300, FX-6300, FX-8350 and FX-9590 on AM3+, the Fuse A4, A8 and A10 on FM2+, and the Optera 6212, 6272 and
  6380 on G34. A board for each new socket: the ATX boards for LGA 1155, AM3+ (on the PCIe 2.0 its chipset had)
  and FM2+, and the two-way and four-way G34 server boards. Thirteen new graphics cards run from the Envya Vertex GT
  730 and the Velocion Radiance HD 7750 to the Vertex GTX 1080 and the Radiance R9 Fury X, on the new Maxwell and
  Pascal designs, with the Envya Tessera K40 compute card for the servers. And the Stratix Layer DDR3L-4096 and the
  DDR3-16384 RDIMM of the servers.
- The Advanced era gets its hardware, from 2017 to today. Sixty-two processors: the Velocion Sprint 200GE and the
  Awayken 3, 5, 7 and 9 on AM4 and AM5, up to the 9950X; the Integra Pentix G4560 and the Centro c3 to c9 on LGA
  1151, 1200 and 1700, where the hybrid Centro count their efficiency cores beside the performance ones, and the
  Centro Ultra on LGA 1851; the workstation Centro c9 on LGA 2066 and the Velocion Threadkiller on sTR4 and sTR5, up
  to ninety-six cores; and for the servers the Velocion Epic on SP3 and SP5 and the Integra Servo Bronzo, Plata, Oro
  and Platina on LGA 3647, 4189 and 4677, with the Servo W-3175X and w9-3495X of the workstations. A board for each
  socket: six ATX boards, four workstation boards, five two-way server boards and the Mainframe's four-way boards
  on SP3, SP5, LGA 3647 and 4189. DDR4 begins here and DDR5 follows it, in the Stratix Layer DDR4-8192 to DDR5-131072
  RDIMM, and the LGA 1700 board takes either. The MF PowerGold 1000G, the PowerPlat 1200P and 1600P, and the
  ServerPSU 2000P and 3000P (Redundant) of the servers. And the Vaultis Keep HDD 12T, 16T, 20T and 24T, the helium
  drives, which are made only as hard disks.
- Σ and Σ# have versions, one number for both: 1 is the language as 0.4.0a shipped it, and this release brings 2,
  with the Sound and Speaker types. The version lives in the compiler's package, sgsc 2.0 being Σ# 2, so a machine
  whose compiler is still 1.0 builds Σ# 1 until an upgrade brings it up (`pckmgr upgrade`, the upgrade verb of each
  Linux and BSD manager, or on MC-DOS installing the compiler again from newer media). A build can be held to an
  older version with `--lang N` on sgsc and scc, or with a `langversion: N` line in a project, and what came later
  is refused where it was written: "'Sound' needs Σ# 2; this project is Σ# 1". Both compilers say which they are
  before anything else ("Σ# Compiler 2.0"). The editors follow the machine's compiler, and Virtual Studio the
  project as well: the list of suggestions offers only what that version has, and the margins and the builds hold
  a file to it.
- Seventeen Advanced graphics cards run from the Velocion Radiance RX 550 and the Envya Vertex GTX 1650 to the
  Radiance RX 7900 XTX and the Vertex RTX 5090, on the Turing, Ampere, Ada Lovelace and Blackwell designs and on
  RDNA 1 to 4, with the Envya Tessera V100, A100 and H100 compute cards of the servers. The Forge Logic Crafting Card
  T4 is the crafting card of the Advanced boards, and the Fabric DPU the cluster card that reaches eight nodes at
  once.
- Σ 2 brings the old names the languages of those machines used, written with no type in front of them, for what
  the library already does: `puts`, `gets`, `exit`, `abs`, `sqrt`, `pow`, `floor`, `min`, `max`, `strlen`,
  `strstr`, `atof`, `itoa`, `sprintf`, `rand` and `srand`. Each is the library's own call written the short way
  (`puts(s)` is `Console.PrintLine(s)`, `strlen(s)` is `s.Length`, `rand()` is `Random.Next(32768)`), so a program
  compiles to the very listing the long way gives, and Σ# has them too. `sprintf` reads its format as `printf`
  does and gives the text back instead of printing it. A method or a variable of the program's own under one of
  those names is still the one called, so no program changes. Σ's `Standard` library gains `Random` in the same
  version, with `Next` and `Seed`. The editors offer the old names, each way it is written, priced as the call it
  stands for, and only to a project whose version has them; `docs/SIGMA.md` lists them with the long way of each.
- `printf` and `sprintf` in Σ 2 read the rest of what C wrote in a hole: flags, widths, precisions, `%u`, `%x`,
  `%X`, `%o`, `%e` and `%E`, and put the value in the hole the way C does (`%05d` of -42 is `-0042`, `%.2f`
  rounds the number the machine holds half to even). A number with a fraction and no precision is still written
  as the language writes it. A plain hole compiles to the same listing as before; one of these is written down as
  a single call that formats the value, and the first version refuses it for the version it needs.
- Σ 2's small helpers: `strcmp`, `toupper`, `tolower`, `isdigit`, `isalpha`, `isspace`, `itoa(n, base)` for a base
  from 2 to 36, and `atoi`, which reads text that is not a number as 0. They stand for calls the library gained in
  the same version and that Σ# can write the long way: `string.Compare`, the `char` type's `ToUpper`, `ToLower`,
  `IsDigit`, `IsLetter` and `IsWhiteSpace`, `Convert.ToString(n, base)` and `Convert.ToInt(text, fallback)`. A
  member added to a type the first version had is refused there the way a later type is ("'string.Compare' needs
  Σ# 2"), and the editors do not offer it to a project held to that version.
- A program's Main in Σ 2 may be `static int Main()`, whose answer is the code the program ends with (what a
  parent reads from `Process.ExitCode`, unless the program ended itself with `Program.Exit`), and may take
  `string[] args`, handed the words the program was started with. The first version refuses both shapes for the
  version they need.
- A program's console keeps an open line, the way the consoles of the old machines did: `Console.Print` and a
  `printf` with no line break leave the line open for what follows, `PrintLine` and a break end it, and it ends
  when the program does. A program that waits for a line asks with what it left open, which stands where the prompt
  was at a desktop's terminal and at a machine's own prompt, and the answer typed goes on the same line
  (`How many ingots? 16`). What is typed is written into the program's console as it is typed, so every terminal
  looking at it shows it once and a save keeps the whole exchange, the open line included. Σ 2's `putchar` prints
  one character on the open line, the `Console.Print(char)` it stands for.
- Reads of less than a line in Σ 2: `getchar` takes the next character typed, the line break at its end included,
  and `scanf("%d", out n)` reads one value into the variable handed with out (`%d`, `%i` and `%u` a whole number,
  `%f` and `%e` a number, `%s` a word, `%c` a character), giving 1 when there was one and 0 when what was typed
  was not one; what is left of the line waits for the next read. They stand for `Console.Read()` and
  `Console.Scan(out value)`, which Σ# can write the long way. Text that is not the value asked for lets its line go
  rather than stopping every read after it.
- Files as C's stdio works them, in Σ 2 and Σ# 2: `FILE f = fopen(path, mode)` with C's six modes, `fclose`,
  `fgets(out line, f)`, `fputs`, `fprintf`, `fscanf` of one value, `fgetc`, `fputc`, `feof`, `rewind`, `fseek` and
  `ftell` counted in characters, `remove` and `rename`, on the machine's own files and the same terms as `File.*`.
  A file opened is one of the program's own objects, kept with it in a save; what it writes reaches the disk when
  the file is closed or when the program ends with it open. They stand for `File.Open`, `File.Move` and the new
  `FILE` type's `Close`, `ReadLine`, `Read`, `Scan`, `Write`, `Seek`, `Position` and `AtEnd`.
- A program holds only so many files open at once, by its machine's era: 8 on a Vintage machine, 20 on a Legacy one
  and 64 from the Transition on, after which `fopen` gives `null`, as C's does when a table of open files is full.
  The count is of the files the program holds, so it is the same after a save and a load.
- `strcpy(out dest, src)` and `strcat(ref dest, src)` in Σ 2, in C's order, compile to the assignment and the
  joining written by hand (`dest = src`, `dest = dest + src`), to the same listing. `ref` is written for `strcat`
  alone; a variable called `ref` is still a variable.
- Virtual Studio's Project Properties has a Language version under the Platform target: Default, which follows the
  installed compiler and says which one it is ("sgsc 2.0"), and a button for each version of the language. A
  version's button writes `langversion: N` into the project and Default takes the line out again; under them, the
  error a lower version gives for what came after it, and the line the choice wrote.

### Changed
- The bridge to Mekanism's chemicals lives in J's Core now, still behind its guard: every mod of the series reaches
  Mekanism's gases, infusions, pigments and slurries through the Core, and none needs Mekanism to run. A check of the
  sources keeps every optional mod (Mekanism, JEI, EMI, FTB, ComputerCraft) named only inside an integration, and
  out of the class that starts it.
- The `recipe_machines` files that tell the Pattern Studio which machines run which recipe types are read by the
  Core's registry of datapack files: a file with a value that is neither a machine id nor a list of them is now left
  out whole, with a line in the log saying why.
- J's Computers' four data cables (Ethernet, HBW, High Compute and Crafting) are laid in the Core's shared cable
  block, each in the lane of its line: access top left, backbone top middle, compute in the middle and crafting
  middle right. They are items now rather than blocks of their own, drawn four pixels thick in their new jackets with
  the plug of their kind where they meet a device, and they can share a block. The buses mount on any cable block that
  holds a data or a crafting cable, on a face no wire crosses. A world's old cable blocks are gone from it.
- The buses on a cable are drawn with the cable in the world's mesh, rather than on their own every frame, and their
  faces are shaded as they are turned.
- A data cable beside a rack cabinet shows a connection only on the cabinet's back, where the cabinet links its
  cables; it used to show one on any face of the cabinet without linking there.
- What the series kept with a world in files of its own is kept as states, each file carrying the version of its
  layout: the data networks of each dimension and the awards players earned while away stay in the files they were
  in, and the ledger of a world's recordings moves from the text file beside them into the world's saved data, as
  `jstech_media_ledger.dat`. A world saved before is read as it was; the text ledger is read once and goes once the
  world has been saved with it.
- Everything else the series saves carries the version of its layout too: every block entity of J's Core, J's
  Computers and J's Industrial, the networks a chunk is on, the operator of a machine, the programs a player first ran
  and the store of what the drives hold. A save from before is read as it was, and saved again with its version.
- The series' settings files are written by J's Core's settings files: `jstech-balance.toml` (the balance of the
  Operations engine, the programs' time and the recordings), `jscomputers-server.toml` (the computers and
  Soundfoundry) and the player's `jstech-audio.json`. They keep their names and their settings, so nothing set in them
  is lost; each now says the version of its layout, and every balance setting has a comment saying what it does
  along with its range. A sound preferences file with one volume written wrongly keeps the others.
- The Vintage server boards hold the Integra Pentix Pro, as the boards of the time did: the MF MTX-V of the
  Mainframe takes four on Socket 8 and the MF EEB-V two. The Socket 7 chips stay on the desktop boards.
- Vintage names, fixed to their years: the MF AT Standard Motherboard is the MF AT Classic, the MF PowerBasic 300B
  is the MF PowerBasic 300 (its id follows, `jsc:psu_300`; the Bronze seal came a decade later), and the graphics
  cards carry their makers of the time: the Artisan 3D Blaster, the Envya Prism 4 and the Tridex Voodoo GFX, while
  the VGA-256 is IBM's own and carries none. Every Vintage part has a new icon drawn from the real one.
- Legacy parts that never existed as named give way to real ones, and their ids follow: the Velocion Sprint XP
  3800+ is the Sprint XP 2800+ (`jsc:cpu_velocion_sprint_xp_2800`), the dual-core Velocion Dual 240, 280 and 285
  are the single-core Velocion Optera 244, 248 and 250 of their years, and the two-way LGA 775 server board is the
  MF EATX Legacy Motherboard (2x Socket 604). The Integra Vertex processors are the Integra Pentix, the name of
  their line (`jsc:cpu_integra_pentix_700` and on), which leaves Vertex to the graphics cards.
- Legacy boards take the memory and the bus their chipsets had: the Socket 370 board SDRAM on AGP 4x, the Socket A
  board SDRAM and DDR, and the Socket 940 server and Mainframe boards DDR. The Radiance 9200 SE is an AGP 8x card
  with four pipelines and 128 MB, and the Legacy graphics cards carry the makers of their time, Envya and Atrion.
  Every Legacy part has a new icon drawn from the real one.
- Parts move to the era of their years. From the Legacy to the Transition: the Integra Duo processors, now the
  Integra Centro 2 Duo (`jsc:cpu_integra_centro_2_duo_e4300` and on), the Servo 5100 and 5160, the Servo 5365, now
  the Servo 5335 it really was, the DDR2-2048, the Vertex 8800 GT and GTX 280 on PCIe 2.0 (the GTX 280 draws 236 W),
  the Tone Blaster Hi-Fi and the SATA-SSD 64G, which holds 256 items at the Transition's weight. From the Standard to
  the Transition: the Velocion Ascent X4 955, X4 965 and X6 1090T (`jsc:cpu_velocion_ascent_x4_965` and on), the
  AM3 board, now the MF ATX Transition Motherboard (AM3) on PCIe 2.0, the Radiance HD 6850, a Velocion card of 1 GB,
  the hard disks up to 2T, and the DVD. The disks read as their catalogue writes them, 500G to 8T, and their ids
  follow (`jsc:disk_hdd_500g` and on).
- The sound built into a board arrives with the Transition: its boards play everything without a sound card, as the
  Standard ones do, and their tooltips say so. The self-test's beep lasts through the Transition, whose firmware is
  still a BIOS; from the Standard on, with a UEFI, a machine comes up quiet. And a Personal Computer from the
  Transition on takes the workstation's EATX board as well as an ATX one.
- Standard names follow the catalogue, and their ids follow them: the Integra Apex processors are the Integra
  Centro c5 and c7 (`jsc:cpu_integra_centro_c7_4790k` and on); the Servos are `jsc:cpu_integra_servo_2620` and
  `jsc:cpu_integra_servo_2690`, and the Servo 2699, a chip of the next socket and of DDR4, gives way to the Servo
  2697 v2 (`jsc:cpu_integra_servo_2697_v2`); the Radiance HD 7970 is a Velocion card
  (`jsc:gpu_radiance_hd_7970`) and the Vertex GTX 550 Ti and 780 Ti are Envya's; the memory is the Stratix Layer
  DDR3-8192; the MTX-P Motherboard and EEB-P Server Board are the MF MTX-S Motherboard (4x LGA 2011)
  (`jsc:motherboard_mtx_s_2011`) and the MF EEB-S Server Board (2x LGA 2011) (`jsc:motherboard_eeb_s_2011`); and the
  MF EATX Standard Workstation Board is the MF EATX Standard Motherboard (LGA 2011)
  (`jsc:motherboard_eatx_standard_2011`). Every Standard part has a new icon drawn from the real one.
- The Standard server boards have the memory slots of their kind: 16 on the two-way board and 48 on the Mainframe's
  four-way one, where a machine still counts only as many as its case holds; a rack server reports the slots its
  case has rather than the board's.
- The Vaultis Swift SSD 8T and the Bolt NVMe 4T and 8T move to the Advanced, the years they sold in, with new icons.
- `apt upgrade`, `dnf update` and `upgrade`, `pacman -Syu`, `emerge --update @world` and `pkg upgrade` bring the
  installed packages up to the versions the Mirror serves, as `pckmgr upgrade` does; `apt update`, `pacman -Sy`,
  `emerge --sync` and `pkg update` still only read the lists.
- A compute card is counted at the clock it is rated at, the one a card with no fan of its own holds in a server:
  the Tessera K40 does as much as the Vertex GTX 780 Ti on the same chip. The Integra Phi 9000 has a new icon.
- A medium's tooltip names its format in the player's language ("Floppy", "Blu-ray") instead of the code's name
  for it.
- A monitor going dark is heard only from a picture tube; the flat panels of the Transition and every later era
  go dark silently, as the Standard ones always did.
- What a processor understands (x86-16, x86, x86-64) is its instruction set: the firmware lists it as the
  Instruction Set, a build of mixed processors is refused for mixing instruction sets, and `sgsc --arch` asks for
  an instruction set by the same ids and names as before. The word architecture is left for the design of a chip.
- How much a processor orchestrates counts the design of its cores as well as how many there are and how fast
  they run: a newer design does more in each tick of its clock, and two threads a core add a fifth. An old chip at
  a high clock no longer outruns a better one that came after it. Vintage machines orchestrate less than before
  (a 486DX2 one item a tick, a K6-II 11) and Standard ones up to three times more (a 4790K 2,074), and no
  processor does less than one item a tick.
- The desktop's code is being split into parts by what each one does, apart from the screen that shows them: where
  the desktop sits on the game's screen, how big it draws and the work area its panel leaves to windows; and how its
  owner chose it should look (accent, brightness, clock, wallpaper, dark theme, CDE's palette) with the skin, the
  clock and the era that follow from it; what it tells the player, in a dialog or in a balloon over the notification
  area; the power dialog; the machine's memory as the desktop weighs it, with the crash of a cooperative kernel that
  runs out of it; the two right-click menus, the panel's and the wallpaper's; the player's inventory laid over the
  window in front; what the desktop can start, built from the program registry and the machine's listing; and the
  launcher the panel opens, a Start menu, Kickoff, the Mint menu or the Activities overview, with its search; and the
  programs on the panel, their pins, where their buttons sit and a program's own menu; the window manager, with the
  windows back to front and the workspace that is up; the layout the machine remembers, with the programs' insides
  this client keeps; how the desktop opens a program or a file, with the requests other screens make of it now
  typed rather than packed into strings; what the pointer drags across the wallpaper, icons, files out of a file
  manager and the rubber band; where a click, a key or the wheel goes, layer by layer; how the desktop is painted,
  back to front; and what the client tests read of a desktop and where they click on it. The desktop itself, all of
  those parts and what the machine last said about it, is now apart from the screen that shows it, and draws onto
  any surface that gives it a size, so it can be drawn with no screen open. What programs and the machine's replies
  ask of the desktop that is up goes through a class of its own, and the screen is left with what only a screen
  does.
- J's Computers' blocks keep their state as J's Core's declared fields: the computers, the server racks, the drives,
  the Pattern Encoders, monitors, speakers, Network Gateways, data cables, Crafting Switches, Server Routers, HBW
  Interfaces and tanks. The players who see one are sent one update a tick however much of it changed, and the
  settings of a Crafting Switch's faces reach everyone looking at it as soon as they are set.
- A monitor's own sessions (the self-test, the boot menu, the firmware setup, the installers and the KVM) take from a
  player only what the session they have open sends, and only for the monitor it is on; a system's settings asking
  for the firmware setup must be on the monitor they name.
- J's Computers' menus are J's Core's declared menus: the computers', the devices', the buses', the terminal's, the
  Command Prompt's and the monitors' sessions. Each stays open only while its block stands and the player can reach
  it, for every variant of the block, and every screen draws its slots and answers its buttons from the one layout
  its menu places them by. What a client sends from one of them acts on the menu the player has open, never on a
  block the message names that the player is not looking at.
- The buses say whether they reach the network with a lamp at the right end of their window's title bar, green or
  red, the word in its tooltip. The Crafting Receiving Bus's title no longer runs into it.
- The drives and the Pattern Encoders take their cable in the middle of their back, where a cable comes up to them,
  instead of low down, and the port there is their era's: a DB-25 on the Vintage ones, a USB port on the Legacy ones,
  a blue USB 3 port on the Standard ones.
- A Standard computer with nothing to boot shows its dialog over the maker's mark and the machine's name, and the
  self-test's bar is gone, where it showed below a short dialog and hid under a tall one.
- The Standard Mainframe's roof fans look like fans: each is a square housing that stays still, with a dark well
  in its opening and a five-bladed rotor turning in it, where before the whole square turned with a cross painted
  on it.
- The Vintage Mainframe's tape reels can be seen: the window in front of them is a clear pane, where a handle stood
  over one reel and a painted glint over the rest, and the reels are aluminium reels with three windows and the
  tape showing through them. They run in bursts, as a tape drive reads: forward, a stop, a short rewind, a stop,
  forward again, the take-up reel a little faster.
- The Pattern Encoders are the devices of their day, each a full block: the Vintage one a beige floppy drive, the
  Legacy one a grey CD writer with a tray, the Standard one a black writer with a tray and a USB port. The medium in
  the bay is the very item the player put in, seen where it sits: a floppy slides in and out of the slot, a disc
  rides in and out on the tray, the eject button pressed as it comes out, a stick goes into the port, and one taken
  out is seen on its way out. Two lamps on the front say what the encoder does: the power lamp is lit while a computer is
  at the other end of its cable, and the activity lamp blinks while a pattern is written and stays lit on an error.
  The body no longer has a status screen.
- The Floppy, CD and DVD Drives are the drives of their day, each a full block in the colours of the Pattern Encoder
  of its era: the Floppy Drive a cream external drive with its bezel low on the front, the CD Drive a pale grey
  external CD-ROM drive with darker end caps, the tray at the top and the headphone jack, the volume slider and the
  eject button under it, and the DVD Drive a black writer with a diamond-cut top and a silver trim. The medium in a
  drive is the very item the player put in: a floppy slides in and out of the slot, and a disc rides in and out on
  the tray. The power lamp is lit while a computer is linked, and the activity lamp blinks while the computer reads
  the drive.
- J's Core's page lists everything it holds today (the sound system, recordings, translatable text, palettes, the
  declaration of blocks and items, the slowest cable of a network) and says in plain words that anyone may use
  the Core in their own project, open or closed, free or paid, with credit, and what its licence, the LGPL 3.0,
  asks in return.
- The three advancements of the old Computers tab moved into the Operating Systems tab, so a world that had
  earned them shows them unearned there.
- A desktop's terminal window opens eighty columns by twenty-four rows, as terminals do, and smaller only when
  the desktop has no room for that. Its rows, and those of the terminal inside an editor, are as far apart as
  the full-screen terminal's, so what is drawn in characters keeps its shape instead of being squashed flat.
- FreeBSD's prompt is root's, as a machine fresh from its installer stands at: `root@host:~ #`, with `root@host`
  in red and the rest in the prompt's own colour.
- The explorer's rows, the file dialog's and those of the file trees in Virtual Studio, Virtual Studio Code and
  Exposure are tall enough for a 16-pixel icon, as a list of small icons on a real desktop is. The file dialog
  opens taller to make room for them.
- A manual page's paragraphs wrap to the width of whatever shows them, at a terminal, under `--help` and `/?`, and
  in the Help Viewer, with the lines they wrap onto set in under the first, instead of breaking at fixed places.
  The Help Viewer files every command under what the command says it is for, so each command sits under the same
  heading wherever it is listed.
- What the terminal says is text a language file can translate: every command's manual and messages, and what
  the machine answers from behind them about Operations, IQL, packages, ports, the Mirror, installs, programs, the
  Network Gateway, remote computers, jobs and clusters. What is typed, names, paths and code stay as they are, and
  what goes down a pipe or into a file is written in English, the machine's own language. The columns of a table,
  such as `cluster list`, stay in line under their headings whatever length the words come out in.
- What the Sigma compilers and the runtime say is text a language file can translate, the way a real compiler's
  messages are on a machine set to another language: every error and warning of `scc` and `sgsc`, the problems of a
  listing and of a package, the Error List of the studios, the cost of a call shown beside a suggestion, and why a
  program was stopped. The file, the line and column, the error's code and the names in the code stay as they are.
  A program that reads another program's output, and a ComputerCraft computer told why the Network Gateway refused
  it, are handed the English.
- What the installers and the tools of an installation say is text a language file can translate: the guided
  installers' titles, headings, hints and steps, Setup's progress and refusals, the boot managers' menus, the
  Frames editions' start and shutdown screens, the welcome tips, why a machine with a broken system will not start,
  the install media's tooltips, and at the prompt of a live medium what fdisk, pacman, emerge, the ports, the kernel
  build and every other step of an installation by hand asks and answers. What real systems print the same in every
  language stays as it is: kernel and init logs, compiler and build lines, file listings, paths, package names and
  the contents of files such as the live medium's guide.
- The steps pacman counts through while it installs are no longer cut short at a fixed width.
- The hardware's tooltips, the names of the hardware eras, a machine's firmware setup, its self-test, its boot menus
  and every page of a guided installer are text a language file can translate. The parts a firmware lists by model
  are named in each player's own language, and a disc's file of requirements is written in English, like every file.
- The Pattern Studio, the Pattern Encoder's panel and messages, and the Patterns heading of a network machine's
  space are text a language file can translate. A recipe with no name of its own is listed by its result's name in
  each player's language.
- The Network Interactor, Storage Insights, the Crafting Manager, the Craft Planner, the Craft heading and its
  craft question, the Crafting Computer's and the Crafting Switch's screens are text a language file can translate,
  and so are a craft's recipe choices, what they differ in and what a plan is short of. Items, machines and recipe
  results are named in each player's language, a machine that is a block goes by the block's own name, and the
  priority tags and the kinds of bus are translatable too.
- The Gateway Manager, the Network Gateway's own screen and the `gateway` command are text a language file can
  translate, and so is the Gateway's log: what the Gateway did and how each request went read in the language of
  whoever looks at the log, while a request stays written as it was asked. A Gateway's place and its link travel
  apart, so the rail no longer cuts a translated line in two.
- The assembly screens of the Personal Computer, the Mainframe, the Server and the Cluster Management Computer, the
  server rack and the Server Router, the buses, the firmware's own install screen, the boot manager's help, the
  installers' frames, the KVM switch and the splash screens are text a language file can translate, and so are the
  reasons a build is not a working computer and the load-balancing modes. The passive bus's explanation wraps to
  the room it has in any language.
- A network machine's space is text a language file can translate, heading by heading: the rail, the status bar,
  the grids and their tooltips, the Local, Ops, Tasks, Upkeep and Programs headings, the questions that take a
  thing out, open an Operation and drop data, and the Command Prompt's banners and keys. So are what storage
  maintenance and a DROP tell the player when they finish, the names the Operations list gives them, the states
  of a machine's processes, and why a program opened from a desktop did not start. An Operation keyword such as
  ANALYZE or DROP stays as it is typed.
- The Network Management Studio is text a language file can translate: its menus, panes, dialogs and status bar,
  what a statement and a save answer, and why the query language could not read a statement. The names in a
  query's results, and in what the prompt lists of the network, travel as the things' own names, so each player
  reads them in their own language, while a search still matches them in English. The query language's own words,
  its keywords, tables and columns, stay as they are typed.
- The Cluster Manager, the Automation Manager, the Messenger and Knot are text a language file can translate: their
  tabs, lists, tables, buttons and dialogs, and what the machine answers to them, from a cluster's line and a
  bulk install's progress and summary to why a job could not be made and what a push or a pull came to. The names
  of clusters, nodes, people, files and programs stay as they are.
- The code editors and the programs that open files are text a language file can translate: nano's, Vim's, emacs's
  and less's lines and help, the Network Interactor's full-screen help, the text editor, Virtual Studio with its
  Start Window, wizard, menus and build output, Virtual Studio Code with its Welcome page and palette, Exposure,
  the system's file window, 67ark, Exceed and Paint. A project template's name and description are read in the
  player's language too, and the wizard's search matches what the player reads. Code, commands and file names stay
  as they are.
- The file explorer, This PC, the trash in all three of its looks, the Open with window and what a medium's tooltip
  says of its files are text a language file can translate, and so is what the machine answers to a save, to
  packing or unpacking an archive, and to something that cannot go to the trash. A volume nobody has named, the
  kind of machine, its parts and its linked drives are read in the player's language; the names a player gave
  stay as they are.
- The Task Manager in all its shapes, the System Monitor, Settings, the welcome a system puts up, the Network
  Manager, a program's Setup window and Remote Control are text a language file can translate, and so are the
  processor, its architecture and the disks those windows are told of, and each machine's kind.
- CDE's Workstation Info, its Exit dialog, a window's menu, the Style Manager's buttons and the Occupy Workspace
  dialog, the desktop shell's own lines, the editor's find strip, Snake, the Help Viewer's search, the panel's
  tray and window list, the Network Interactor's categories, the Cluster Manager's rack places, FreeBSD's loader
  count, what a terminal says when it lost its machine or cannot open a program, a program's setup progress,
  the config listing and the network conflict notice are text a language file can translate. The categories keep
  their English names underneath, so a saved filter survives a change of language.
- The desktop's own words are text a language file can translate: its errors and notices, the crash of a system
  that ran out of memory, the menus of the wallpaper, of an icon and of a panel entry, the power dialog, the
  Frames start menus' own words, the recipe viewer's hint to open the Pattern Studio and a command palette with
  nothing to show. The desktop's own colours (the crash page, a dragged label, the selection band, the panel
  menu, the tray balloon, the power shade) are the `jsc:desktop/shell` palette, which a resource pack can
  recolour.
- The installers' frames, the Server Rack's cabinet in each era, the Frames and Linux panels and their start
  menus, and the tray are palettes a resource pack can recolour (`jsc:installer/*`, `jsc:rack/*`,
  `jsc:panel/*`, `jsc:launcher/*`), and the words on those panels and menus (Start, Apps, Menu, Activities, the
  Linux menus' places and categories, their search hints and session buttons) are text a language file can
  translate.
- The firmware's setup, self-test, boot menus and install screen in each of their looks, the desktops' loading
  screens, the systems' start and shutdown pictures, the monitor frames of each era, This PC, the terminal's
  popups and its Maintenance tab, and the installer's own pages are palettes a resource pack can recolour
  (`jsc:firmware/*`, `jsc:splash/*`, `jsc:boot/*`, `jsc:monitor/*`, `jsc:app/this_pc`, `jsc:terminal/*`,
  `jsc:installer/page`).
- The Network Interactor, the Network Manager, the Task Manager, the Application Manager, the games (Minesweeper,
  Snake, Solitaire and its cards), the desktop's icons and questions, a system's boot log, the Crafting Switch and
  the Industrial machines' screens draw their colours from palettes a resource pack can recolour (`jsc:app/*`,
  `jsc:game/*`, `jsc:desktop/*`, `jsc:boot/system`, `jsc:screen/crafting_switch`, `jsindustrial:machine/screen`).
- The NMS, Setup, Messenger, Files, Knot, Storage Insights, the Gateway, Cluster, Automation and Craft Planner
  managers, the Frames Recycle Bin's task pane, the code editors and their highlighting, the shell view's tags and
  the Operation types' colours are palettes a resource pack can recolour (`jsc:app/*`, `jsc:editor/*`,
  `jsc:desktop/shell_view`, `jsc:operation/types`).
- Every colour left in the code is now a palette a resource pack can recolour: the toolkit's components, widgets
  and dialogs (`jscore:gui/*`), the green of a Vintage tube (`jscore:gui/phosphor`), the toasts' accents
  (`jscore:gui/toast`), the assembly screens' name field, the terminal's popups, the boot menus, the query and code
  editors, Paint, the Style Manager, the System Monitor and the rest of the desktops' windows and programs. Only the
  colours an image file stores stay in the code, since they are the file's and not the look's.
- Every word a player reads is translatable: the dialog buttons, the search field, CDE's Front Panel, the default
  name of a new file or folder, the Application Manager, and what each desktop calls the programs it bundles, so a
  Text Editor or a System Settings reads in the player's language while Dolphin, Kate and Nemo keep their names.
  Launchers and window titles show a program's name in the player's language.
- A desktop's windows are known by the program they belong to rather than by the name they show, so a machine's
  layout comes back the same whatever desktop or language it is looked at under, and the Task Manager and the
  System Monitor list each window by the name the desktop gives its program.

### Removed
- The unused hook for the Modonomicon guidebook: the series' manuals will be drawn by J's Core itself.

### Fixed
- Nothing in the mod loads a chunk any more by looking at a place it remembers. A monitor asking after its
  computer, a computer counting its screens, speakers and drives, a Mainframe reaching the servers, racks and
  computers of its network, a cluster's nodes, a hub's devices and the file and cluster screens all looked at the
  block at the remembered place, and the game answered a place whose chunk was not loaded by loading it from the
  disk, or making it, in the middle of the tick. Besides the stutter, a chunk caught that way while the game was
  letting it go could stop every chunk from loading until the server restarted. A place whose chunk is not loaded
  now counts as away, and an ssh session whose far machine is away ends as one whose machine has stopped.
- The prompt's `iql` command, and a file of statements it runs, no longer fail on a `SET BUS` statement.
- A base with cables ticks as fast as it did before the shared cable block. Machines that look at the blocks round
  them every tick for a cable (the Crafting Switch and the Crafting Computer finding their machines, the racks finding
  their network) asked the game for a block entity at every place, and a place with no cable answered only after a
  lookup, a check of what waits to be loaded and an attempt to make one. They now look at the block first, and a
  loaded base's tick is back to what it was, a third to a half lighter.
- A run of pipes whose pipes stand no temperature in common carries nothing. It used to carry a fluid at exactly
  the coldest temperature of the warmer pipe, which the colder pipe does not stand: a pipe made for 100 to 200 K
  joined to one made for 300 to 400 K let water at 300 K through.
- A program in front of a machine's own prompt, on a machine with no desktop, reads what is typed there: the line
  went to the shell as a command instead, so such a program could never be answered.
- An array in Σ and Σ# answers to `Length`, the number of places it has, which Σ's own advice for `foreach` told
  a program to use and nothing let it read. It is only read: an array is as long as it was made.
- The amber of cautions on the Legacy computing screens was hard to read on their light panels; it is a darker
  goldenrod now, which reads on every ground those screens draw.
- The service panel of every Mainframe no longer flickers where it meets the edges of its opening: its edges lay on
  the walls of the opening and fought them for the surface, which showed through the grille, the louvres and the
  glass. The window over the Vintage Mainframe's tape reels flickered the same way and no longer does.
- A Pattern Encoder's item shows the encoder switched off. It is drawn with the same model as the encoders in the
  world, and it took on their lamps: lit, or blinking, as the last encoder drawn showed them.
- The Mainframes' and the racks' items show the machine as it comes, empty and switched off. They are drawn with the
  same models as the machines in the world, and took on the hardware, the servers and the lamps of the last one
  drawn, or showed every part at once, every kind of server in every row, before any was.
- An Industrial machine spills what it holds however it goes: blown up, replaced, or broken in creative mode, where
  before only a player breaking it in survival got its contents back.
- Shutting a computer down from its desktop shows the system's own goodbye, and the machine goes dark only when it
  has finished; it used to cut the power at once, dropping the player out of the computer with no screen at all.
  Restarting from the desktop shows the goodbye too before the self-test, as restarting from a prompt already did.
- Typing a Server Router's name no longer closes its screen at the letter E, the key that closes an inventory:
  while the name is typed every key goes to it, and Escape leaves the field.
- Saving an IQL file from the Network Management Studio to a full disk no longer fails to send its answer: the
  reason was longer than the answer could carry, which is now text of any length. A long answer to a statement is
  carried whole for the same reason.
- Saving a file whose path is near the longest one allowed no longer fails to send its answer, which left the
  editor waiting: the answer names the path, and with it ran past what the answer could carry. It is now text of
  any length.
- A monitor is used by one player at a time. A second player who used it while somebody was at it was handed a
  session over the one being typed into, and ended the remote session the screen was holding; now they are told
  who is using the monitor, and the one at it keeps it until they walk away or close it.
- The Macerator, Electric Furnace, Compressor and Coal Generator can be mined in survival. They need the right
  tool to come away, but no tool counted as right for them, so breaking one gave nothing back and took a long
  time doing it. A pickaxe is now their tool: it mines them at a pickaxe's pace, and they drop themselves.
- A Mainframe of any era is mined at a pickaxe's pace. It needs the right tool too, and no tool counted, so taking
  one down in survival took far longer than a block of metal should.
- The Import, Export, Crafting Input and Crafting Receiving Buses, the Supercomputer Node and the HBW Interface
  say in their tooltip what they are for, like the rest of the network and rack equipment. The words were written
  but never shown.
- Every program says what it does on its install disc, in the package manager and in the installed-programs list.
  CDE, Cluster Manager, Emacs, Exposure, Gateway Manager, Help Viewer, Vim, Virtual Studio, Virtual Studio Code and
  Workstation Info had nothing to say there.
- Settings, the System Monitor and the Network Manager call a machine's system by its name. A Linux, FreeBSD or
  UNIX system was shown by its id, `ubuntu` rather than Ubuntu, and the System Monitor showed every system that
  way, the Frames editions included.
- The Personalize page shows the Breeze, Adwaita and Mint-Y wallpapers as themselves. Their three thumbnails
  were the same plain blue, so nothing told them apart until one was hung.
- screenfetch draws every logo as neofetch draws it. Ubuntu's was another, older logo copied with mistakes, and
  Debian's had a stray quote. Each was printed in one colour, readout included; now the art is in its own
  colours (Ubuntu red and white, Debian white with a red centre, Gentoo magenta and white) and the user, host
  and labels in the logo's. In a terminal narrower than the logo and its readout the rows used to wrap, which
  broke the logo in two with the readout pushed into it; they are now cut at the edge, as neofetch does.
  FreeBSD's is unchanged. The art is neofetch's, credited in THIRD_PARTY_NOTICES.md.
- A server with nothing to boot no longer costs its rack time on every tick. A machine whose self-test found
  no system stands at that failure and goes on by itself once one is installed, and to notice that it was
  asking about its disks, and about every drive it is cabled to, twenty times a second; a datacenter of
  servers kept only for storage spent half of an idle tick on the question. It now asks once a second, each
  machine on its own tick of that second. The rack itself also answers questions about its bays without
  working out afresh, each time, which units are mounted in it.
- A `break` out of a `switch` no longer lets go of a lock the switch sits inside. Written in a loop, inside a
  `lock`, it let go of the lock at the break and again where the lock ends, and the second letting go halted
  the program for giving up a lock it did not hold. It now lets go only of the locks taken inside the switch.
- Breaking a router in the same tick it was placed no longer throws. What carries the network is put in the
  network's map when it first loads, which comes after it is placed, and breaking a router before that asked
  the map to take out something it never had. Cables already allowed for it; all three now ask the same way.
- A server rack's screen shows how far the cabinet is throttling even when the rack is not on a data network.
  The reading was only written for servers registered on a network, so a rack off the network, and every
  supercomputer rack, showed a cabinet running free however hot it ran.
- A Mainframe whose system disk is erased or taken out while it runs lets go of the Operations it was carrying,
  as switching it off does: items are conserved and every hold on the storage is released. They used to stand
  frozen, holding the storage, for as long as the Mainframe went without a system.
- A machine's output is no longer lost when the network has no room for it. A machine step pulled everything
  finished out of the machine and stored what fitted, and whatever a full network refused was gone. Only what
  is stored leaves the machine now, and the rest waits in it until there is room.
- Switching a machine off and on while it stood at a failed self-test no longer brings it back standing at the
  old failure, which outlived the power cycle and could send the machine past the new self-test it owed.
- A file of IQL statements run at the prompt with `run` runs every statement in it. It was read as one
  statement, so any file of two, or one with a comment line, failed as a syntax error, although the same file
  run by a program worked. Both now read a file the one way the studio writes it: a statement a line, with
  blank lines and `--` comments left out, and the whole file is checked before any of it runs.
- A file can no longer be written, renamed or copied onto the name of a folder. The disk then held a file and
  a folder of the same name, which a listing showed twice and nothing could tell apart.
- Copying or moving a file to another drive, or to another machine's shared folder, no longer writes over a
  file of the same name already there. A copy on the same drive always refused; across drives it was a plain
  write, which replaces, and a move then deleted the original as well.
- A command that takes the whole terminal, such as `less` given a file, `interac` or an editor, is refused on
  a line that pipes or redirects, instead of printing nothing and leaving the file it was sent to empty. A
  line that ends in `clear` or `cls` now clears the screen, as the command does on its own.
- A player's package taken off a machine can be installed on it again. Removing it deleted its files but left
  its folder, and the next install found the folder there and gave up as if the system had no folders.
- Erasing a system disk forgets the programs a player installed from the Mirror, which lived on it; the desktop
  went on showing icons for programs whose files were gone.
- An `out` parameter given a value in every section of a `switch` that has a `default` counts as given. The
  compiler threw away what each section did and refused a correct program.
- A source nested more deeply than anyone writes, a few thousand brackets, blocks or operators inside each
  other, is refused with an error saying so instead of crashing the compiler with a stack overflow.
- A package keeps a file whose text has a line starting with `--- `. That line was read as the start of
  another file, so the package came back from the Mirror with the file cut in two.
- A rack's KVM switch keeps the machine it was switched to through a save. It came back from every reload
  showing the first machine in the rack.
- A guided installer's progress follows the disk the system goes on. Picking another disk, or erasing one to
  install over it, timed the copy for that disk but left the steps on the page, and when they unlock, sharing
  out the time of the first disk the installer had suggested.
- What a Gateway spent on a machine's behalf is still owed after a save when the machine runs no program;
  the save left it out, so a reload wiped the debt the machine was still paying down.
- The compiler catches two `switch` labels that pick the same value written two ways, such as `65` and `'A'`
  in a numeric switch. They were compared as written and both accepted.
- The server reads no more text from a client than each message is meant to carry. An automation job's
  fields, a terminal's server names, the program to uninstall and the name help is asked for were read at
  whatever length a client sent, where the messages beside them were already held to a size.
- A terminal reopened on a tab that is no longer there, the Craft tab after the last Crafting Computer left
  or the Patterns tab after the card came out, opens on Network for the player too. Only the server moved,
  so the screen showed an empty tab the server was no longer filling.
- `AT` schedules a command that has `/DELETE` among its own words. The word anywhere on the line made it
  a delete, which then looked for a job numbered by the time.
- A Linux installed by hand is only offered software its hardware era runs, as every other way of installing
  already was; a Vintage machine could be given a desktop that needs Legacy hardware while it was being built.
- A desktop opens on a machine with more than sixteen programs installed. The list of them it is sent was
  capped at sixteen and refused whole past that, so installing a seventeenth took the desktop away.
- Going from one face to another on a Crafting Switch's screen no longer renames the machine a crafting bus reaches
  with the first letters of the other face's name.
- A system's copy that fails with a long reason shows as much of it as fits above the buttons, the last line ending
  in dots, where it ran on under them.

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
