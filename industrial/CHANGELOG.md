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
  grid, a use worn each time. A scrap melts into a nugget.
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
- Plant Drawings: the Science section opens, with drawings of the Computational Research Centre and its
  parts, of the research data and of research groups; and Fluids and Gases has one of hot water.

### Changed
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
- The series' basic cable: its machines run on J's Core's Insulated Copper Cable, which carries 500 FE a
  tick and loses a tenth over a hundred blocks, and the mod's own Energy Cable is gone. The cable and the Iron Dust,
  Iron Plate and Copper Plate are on the J's Tech tab, with the rest of what the mods of the series share.
- The Tank is a block of the Core's: a bucket fills and empties it, and its item says what it carries.

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
