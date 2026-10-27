# Changelog of J's Industrial

Every change to J's Industrial, newest first, taken from the series' changelog
([CHANGELOG.md](../CHANGELOG.md)), which records each change once with the mods it concerns and keeps
the releases before 0.5.0a. This file is written from it by `./gradlew changelogs`.

## [Unreleased]

### API
- Machines that fail safe, and J's Industrial's own API: J's Core's API stays at version 12 and
  J's Industrial's is at version 1, all experimental. A machine declares from its constructor that it fails safe
  (`MachineBlockEntity.failsSafe`, and its port's `EnergyStore.failsSafe`). `MachineStatus.OVERVOLTAGE` is a machine
  that refused a line above its class rather than burn, read through `MachinePower.overvoltage` and shown on its face
  with `MachineScreenTexts.LCD_OVERVOLTAGE`. J's Industrial's API is the package `dev.jstech.industrial.api`:
  `JsIndustrialApi.VERSION`, `Consequences.of` (the switch every consequence of the mod has, on unless a server turns
  it off, saying what happens instead), `IndustrialAdvancementTab` (the four tabs an add-on hangs its advancements
  under) and `IndustrialSoundGroup` (the groups its sounds are mixed in, each in a channel of the Core's).

### Added
- The four tabs of its advancements: Foundation opens with the first machine, Industrialisation at the
  third tier, Discovery with a team's first discovery and Transcendence at the sixth tier.
- The Plant Drawings by the sections of the mod's design, each lettered for its trade: Reading the
  drawings, Getting Started, The World, Metallurgy, Power, Machines, Fluids and Gases, Chemistry, Conveyors, Clean
  Room, Science, Nuclear, Transcendence, The Aleph and Tools and Armour; a section shows once it has its first
  drawing. Reading the drawings has a sheet for each kind of drawing the sections use, its symbols and a small
  example: single-line diagrams and P&IDs, which a switch shows as the scene of their blocks, and geological
  sections, which it shows in the rocks' own colours.
- The ores of its first nine minerals, in the rocks their veins cross, each dropping its raw mineral, and
  tin's ore.
- The Macerator grinds every ore and raw mineral into two dusts of what it is mined for, with a counted
  share of its byproducts: chalcopyrite gives sulfur, and a little gold, silver, selenium and tellurium.
- A furnace smelts the simple minerals raw (native metals, oxides, carbonates) into one ingot, never a
  sulfide; and every metal's dust melts back into its ingot where a furnace reaches its melting point.
- The forms of iron, copper, gold, tin and bronze: plates, wire, dust, nuggets, blocks and their scraps.
- Hand tools for the start: the Forge Hammer beats three ingots into two plates, the Draw Plate pulls an
  ingot into a wire and leaves a scrap every eighth, the Mortar grinds three ingots into two dusts; each stays in the
  grid, a use worn each time. A scrap melts into a nugget. The Forge Hammer is a head of four iron ingots on a stick,
  not an axe's three, held as a tool, and strikes as a heavy one: 7 a blow, 0.9 blows a second.
- Elements discovered: a team's first synthesis of an element science discovered by making it is its
  discovery, the team knows it from then on, and its players earn the Discovery advancement, the machine's owner too
  when they join.
- Plant Drawings: a page for the hand tools.
- Circuits: the ten circuits, one for each tier from the mechanical to the omniversal, each known by its
  item tag; making any item of one moves its maker's team to its tier.
- Research: four kinds of data, sample, crystal, spectral and particle, an item each, its tooltip saying
  what gives it; and a world setting, on unless turned off, whether the research locks what the discoveries open.
- Research Link Cable: a line of the series' cable block that joins research computers into a group,
  one group for each connected run; a dyed run is a group of its own.
- Hot water, what the coolant's water becomes as it takes heat away.
- Computational Research Centre: a research computer of nine blocks deep and three by three across, built
  of CRC Casings, Gates and Buses with its CRC Terminal in front and its entries in the lower two rows of its rear, a
  CRC Power Entry at 11 kV, a CRC Link Socket, a CRC Coolant Inlet and Outlet, and a CRC Heat Port. Formed, it is
  drawn as one machine, its places seen through its doors and its entries in its rear. Its 27 places take research
  processors of three tiers, which compute and draw power that all becomes heat, and what takes the heat away: heat
  sinks to the air, liquid coolers into the water of its coolant loop, which comes out hot, and heat pipe coolers out
  through its Heat Port. Past 100 °C the hungriest processor burns, and its owner is told. Its four data places hold
  the data its discoveries spend. CRCs on one run of Research Link Cable work one discovery, which any of them may
  choose or queue; each team counts its own, the strongest whole and each other at 90%, and keeps its own progress,
  so cutting the cable loses nothing.
- Computational Research Centre: its screen, wider than every machine's, with three tabs of its own.
  Processors holds its three sections of places and its data, the lamps of its coolant and Heat Port, what it
  computes, draws and sheds on mechanical counters, a thermometer, its switch and its RUN, WAIT and HOT lamps, the
  part that would burn first rimmed in red past 90 °C. Research charts the discoveries by the year each was made, a
  lane for each field, what its team knows, works now, has begun, may start and may not start yet; the card of the
  one picked says what it comes after, the data it asks for against what the team holds and where that data comes
  from, how far the computing is and how long the rest takes, and researches it now or queues it; under the chart
  are what the group works now, its pace and the queue. Group draws the CRCs on their cable and tables what each
  computes, how much its team counts and each team's pace. Sides, Redstone and Protection are every machine's.
- Computational Research Centre by hand: formed, each cabinet's glass door opens with a click, swinging
  out a quarter turn, and each of the nine places behind it takes the part in hand or gives back what it holds, as its
  screen's places do; the green button on its desk turns it on and the red one off.
- The contract of its machines: each says its tier, the class of J's Energy of its port and its power,
  and has the upgrade slots of its tier, two on a machine of the T1, three on the T2 and T3, four from the T4. What
  its upgrades gain adds up and what they save multiplies, so two upgrades that each make it half again as fast make
  it twice as fast. Damaged, it runs at half speed and draws a quarter more, its upgrades idle until it is repaired.
  As it works it gives off what it declares, a pollution on its chunk each second and a radiation while it runs,
  which a shielding in its upgrade slots holds in; none of its machines gives off anything yet. Its generators follow
  their load, burning only what is drawn from them.
- The rules of its multiblocks: each is built of a material of its time, brick, wood or iron before the
  T3 and steel from it, which sets how its blocks stand, sound and break; each says what its size gives, a longer one
  more recipes at once; and its terminal is the whole machine screen, keeping what it holds while the structure
  stands apart. The Computational Research Centre is one, of steel.
- The kit of its grid equipment and its batteries: a transformer joins two neighbouring classes of
  alternating current and an HVDC converter station the extra-high class to the ultra-high, each carrying up to the
  ceiling of the higher class it joins, from the side the energy comes from, either way, making and losing nothing;
  and a battery, an item of J's Energy at low voltage, which a machine's battery slot charges and draws on.
- The wrench takes its machines away whole, and a multiblock's terminal with what it holds, the
  structure forming again once the terminal is back.
- Plant Drawings: the Science section opens, with drawings of the Computational Research Centre and its
  parts, of the research data and of research groups; and Fluids and Gases has one of hot water.
- The marks of what its pipes carry: acids, molten metal, molten salt, ultra-pure water and the corrosive
  gases ask a pipe made for them; hydrogen wears plain steel through, and a slurry an unlined pipe. Fluid tags, so a
  data pack marks another mod's fluids.
- What befalls its pipes: too hot or too cold, a pipe bursts or cracks after five seconds; a corrosive it
  is not made for eats it through in about a minute; past its pressure it bursts at once; hydrogen wears plain steel
  through in an hour, a slurry an unlined pipe in ten minutes. A broken pipe falls as its scrap and leaks five seconds
  of its flow: a liquid spills, a slurry splashes out, a gas comes out as a cloud. Each is a server setting, on as it
  comes: pipes fail, leaks are hazardous, hydrogen embrittles. Mekanism's hydrogen wears steel as the series' does.
- The contract of its tanks and pumps: a tank of several blocks holds the room inside its walls, a cubic
  metre, 1,000 mB, a block; a gas vessel its volume times its pressure; one content at a time, refusing more full and
  what it is not made for, kept while the tank stands apart; a comparator reads it; a cold liquid boils off by the
  tank's insulation. A water pump draws water without end from a source beside it, a drainage pump the fluid blocks
  around it, a bucket a block, and both push what they pump into their pipes.
- Compressed air: its own line, the Compressed Air Line, which never joins a gas pipe; a machine with an
  air port spends its air and works 15% faster, added beside its upgrades' gains.
- The rules of its pneumatic post: capsule tubes of their own line, a capsule of up to four stacks a
  second from each station, about twenty blocks a second, each shot by compressed air, without which the stations
  stop.
- Its deposit types, real kinds of deposit holding the minerals that form together and the ore of their
  elements: porphyry copper, banded iron, sedimentary iron, the oxidised zone, epithermal gold, tin greisen, massive
  sulfide and skarn, each with the minerals that exist so far, its data file a pack can change.
- Prospecting in words, never coordinates: the direction and strength of the veins near, traces, a vein
  or a rich vein, and the reservoirs below with about how deep each lies; and the scanner's reading of a vein's ore
  and a reservoir's size. The rule of a well pump, which drains the reservoir straight below it block by block.
- The rules of its tools and armour: modules in sockets by tier, two up to the T3, three at the T4,
  four at the T5 and T6, five from the T7; a module one piece takes at most so many of; pieces that say which they
  are and suits that say they are sealed, so a space module fits only a sealed suit; nothing of the mod enchanted at
  the table or the anvil; and a server setting for each kind of tool, which off takes its recipes and its place in the
  creative tab away.
- Tanks read from outside: the tank and the tanks of a size the player chooses say what they hold, and a
  gas tank its pressure, to a controller on the industrial control cable.
- With J's Computers installed, the Industrial Controller Computer: one in each case J's Computers ships
  (the Vintage, the Legacy and the Transition, and the Standard and the Advanced in their Neutral, High Performance
  and Aesthetic cases), built as a Personal Computer of its era and drawn with a plant's control panel under its
  front, its RUN, BUS and FAULT lamps, its LOCAL/REMOTE key and its terminal strip. It is a machine of its own, the
  Industrial Controller, and only the Fieldbus Cards go in it: RS-485 for the Vintage on ISA, PROFIBUS for the Legacy
  and PROFINET for the Transition on PCI, EtherNet/IP for the Standard and EtherCAT for the Advanced on PCIe, reading 2,
  4, 6, 8 and 12 machines, each drawn in its slot with its bus's connectors. The Industrial Control Cable, violet in
  the bottom left lane of the series' cable block, links it the machines it reaches, the nearest first, up to eight
  blocks of cable away; it ends in the plug of the card's connector at the controller and in an M12 at a machine.
  The Plant Drawings have a Control section with their pages. Without J's Computers none of them exist, and the mod
  is the same.
- The Industrial Controller Computer reads and commands its machines from the data network. `QUERY
  machines` has a row for each machine a switched-on controller reads, from every controller on the network: its name
  (`Compressor #1`), kind and state, its temperature and its limit in degrees Celsius and how hot it is as a share of
  that limit (`heat`), what it draws and its port, its progress, and the controller and the card's node that read it
  (`PROFIBUS, node 5`); `QUERY controllers` says, for each controller, how many machines its cards can read, how many
  it reads and how many its cables reach past them. `RUN jsindustrial:machine_set` sets a value a machine lets be set
  or what it does with redstone, `RUN jsindustrial:valve_set` opens or closes a valve opened by hand, and `RUN
  jsindustrial:station_send` has a capsule station send what waits in it to an address, the request waiting while the
  station has no air. `ON` names the controller; without it the request goes to the controller that reads the machine,
  and one switched off, or that reads no machine of that name, refuses it, saying why.
- Machine alerts: a switched-on Industrial Controller Computer looks at its machines once a second and
  raises an alert the moment one falls into a fault, marking it over once the machine is out of it. Overheating and a
  blown fuse are critical, undervoltage and a gas leak (a poison past the share dangerous to breathe, or a fuel past
  the share that can burn, at the machine) are warnings, and a damaged machine is a notice. Each goes into the
  network's event log as it is raised, and `QUERY alerts` lists the latest 64 of each controller.
- The Machine Monitor, by Wonderwork Systems, from the Legacy on: the alerts of every Industrial
  Controller Computer on the network, newest first, with the lamp of each one's grade, the count of each kind, a
  choice of the kind shown, and the panel of the machine chosen with what it reads now. Acknowledge dims the alert
  chosen and Acknowledge all every one; nothing is taken away. On Frames 95, XP and 7 it opens in its classic look,
  with its menus and the panel under the list; from Frames 10 on, in its modern look, with a search by machine and
  the panel beside the list. The Plant Drawings have its page, and pages on reading the plant from the network and on
  the alerts.

### Changed
- The Forge Hammer, the Draw Plate and the Mortar are drawn in three dimensions in the hand and on the
  ground: the hammer and the plate held as tools, the mortar with its pestle in the palm.
- The Computational Research Centre's terminal stands in the middle of its front, at a player's eyes,
  one layer up: a CRC built before stands unformed until its terminal is moved there.
- The CRC's Research chart draws its years twice as wide and is dragged with the mouse: across along the
  years, up and down through its fields.
- The CRC's Research and Group tabs have new icons: a corked flask, and three CRCs on their link cable.
- The CRC's formed model has an underside: a steel base frame with its rails, cross members, rivets,
  levelling feet and a cable trough, where it was a plain dark plane seen from below.
- With J's Industrial installed, its deposit types take the place of the Core's plain veins of
  iron, copper, gold and tin, and the game's ores of those metals lie in its deposits beside their minerals; the
  Nether's gold stays the Core's.
- Its machines run on J's Energy at low voltage, all of the T1: the Coal Generator makes up to 5 kW,
  burning only what is drawn from it, so a coal lasts 80 seconds at its full power and longer when less is drawn; the
  Macerator draws 4 kW, the Compressor and the Electric Furnace 3 kW. A machine holds two seconds of its work and takes
  half a second of it at once, and a generator holds what it gives the machines touching it each half second. As it
  comes, the world counts J's Energy as FE, a joule an FE, so the machines of other mods still meet them. Their
  recipes, the recipe viewers and the Plant Drawings say their power in kW.
- Plant Drawings: every machine is shown as its block, which the reader turns and brings closer, with
  its balloons on it, in place of its three line views.
- The Macerator has six output boxes, for the dusts and the byproducts.
- Its recipes are written from its data and named by what makes them: `compressor/`, `macerator/`,
  `macerating/` for ores, `hand/`, `crafting/`, `smelting/` and `blasting/`; a data pack that replaced one by its old
  path replaces it by the new.
- Listeners declared: every class of the three mods that answers the game's events is
  handed to the buses from its mod's constructor, through a `listen` method of its own, rather than found by an
  annotation, so what runs is what the constructor says; a mod's mistake in what it declares stops the load saying
  what to change.
- The standard machine screen: the Macerator, the Compressor, the Electric Furnace and the Coal Generator
  open the screen every machine of the series has, its Main tab with their slots, their progress and their energy,
  and the Sides, Redstone, Upgrades and Protection tabs; their own screens are gone.
- The Coal Generator is a machine of the Core's: it gives its energy to the machines touching it twice
  a second, through the faces its Sides tab opens to energy, and while its store is full it stops burning, keeping
  what is left of the fuel, instead of burning it away.
- The series' basic cable: its machines run on J's Core's Insulated Copper Cable, a low-voltage line
  that carries 10 kW and loses a tenth over a hundred blocks, and the mod's own Energy Cable is gone. The cable and the Iron Dust,
  Iron Plate and Copper Plate are on the J's Tech tab, with the rest of what the mods of the series share.
- The Tank is a block of the Core's: a bucket fills and empties it, and its item says what it carries.

### Fixed
- The CRC's formed model draws each entry where it stands: the rear's columns were the wrong way round,
  so an entry put in the west place showed in the east one. An entry put in is drawn as soon as the structure sees it,
  not on the CRC's next second.
- A cable run to a CRC entry meets it square on: each entry is centred on its block's face, where a cable
  laid behind it runs (the side columns' stood off to the middle, the bottom row's up), and its gland, socket or pipe
  stub reaches the edge of the block. And no slit of sky shows along the CRC's foot under an entry or its terminal,
  which hid the ground's face.

## [0.5.2a] - 2026-10-14

Codename: Boron.

## [0.5.1a] - 2026-10-14

Codename: Boron.

## [0.5.0a] - 2026-10-13 - The Start Me Up Update

Codename: Boron.

### API
- Changed: its machines stand on J's Core's; `AbstractMachineBlockEntity`,
  `ProcessingMachineBlockEntity`, `MaceratingRecipe` and `CompressingRecipe` are gone, and its two kinds of recipe are
  J's Core's processing recipes.

### Added
- Translation: Brazilian Portuguese (pt_br) for J's Core, J's Computers and J's
  Industrial: every word the mods show, from the blocks and items to the desktops, the programs, the terminals and the
  installers. The names of the fictional products and makers, the commands a player types and the files on a virtual
  disk stay as they are.
- Logos: Logos and icons for the series and for each mod. The mods list shows each mod's
  logo over the series logo, mod list screens that show an icon show each mod's own, and the READMEs carry the logos.
- Energy: J's Industrial's Energy Cable, laid in the shared cable block's energy lane: it carries any
  amount of energy any distance and loses none of it.
- Manuals: The Plant Drawings, J's Industrial's own manual, a slate folder of blueprints in its tab whose
  bar runs in black and yellow stripes: a drawing list, then every machine drawn from above, the front and the side,
  traced from its own block, with numbered balloons, set up in a plan seen from above, on sheets with a frame, zones and
  a title block (JI-102, sheet 1 of 2).
- Documentation: J's Industrial's documentation, in its `docs` folder, explains every machine with its real
  numbers, and its recipes, tags and capabilities to mod and pack authors, with what can go wrong at the end of every
  page.
- Design: The design of J's Industrial, in `industrial/docs/design`: one page per subject, from the
  materials, minerals and the world to metallurgy, the tiers and their circuits, energy, machines, fluids and gases,
  chemistry, conveyors, the Clean Room, science, nuclear power, the exotic materials of the last tiers and the Aleph,
  tools and armour, and what it does with J's Computers; each page says how its system works and ends with what is
  designed and still to be built, gathered on the first page, with the formulas and every setting in one place.

### Changed
- Storage: The Tank belongs to J's Industrial now (`jsindustrial:tank`), on its own Storage
  shelf and in its manual. A Tank placed under the old name is gone from a world made before. J's Computers' manual
  explains fluids on a page of their own, with a tank of any mod.
- Machines: J's Industrial's Macerator and Compressor run on J's Core's processing machines, and their
  recipe files take a list of inputs, each with its count, a list of outputs, and the time and the energy a tick.
- Saves: What the series kept with a world in files of its own is kept as states, each
  file carrying the version of its layout: the data networks of each dimension and the awards players earned while
  away stay in the files they were in, and the ledger of a world's recordings moves from the text file beside them into
  the world's saved data, as `jstech_media_ledger.dat`. A world saved before is read as it was; the text ledger is read
  once and goes once the world has been saved with it.
- Saves: Everything else the series saves carries the version of its layout too: every
  block entity of J's Core, J's Computers and J's Industrial, the networks a chunk is on, the operator of a machine,
  the programs a player first ran and the store of what the drives hold. A save from before is read as it was, and
  saved again with its version.

### Fixed
- Machines: An Industrial machine spills what it holds however it goes: blown up, replaced, or broken in
  creative mode, where before only a player breaking it in survival got its contents back.
- Machines: The Macerator, Electric Furnace, Compressor and Coal Generator can be mined in survival. They
  need the right tool to come away, but no tool counted as right for them, so breaking one gave nothing back and took a
  long time doing it. A pickaxe is now their tool: it mines them at a pickaxe's pace, and they drop themselves.
- Machines: The Coal Generator gives the empty bucket back when it burns a lava bucket, as a furnace does.
- Storage: A Tank broken keeps its fluid in its item, which says what it holds, and gives it back when it
  is placed.
