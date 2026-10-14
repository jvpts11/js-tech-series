# Changelog of J's Industrial

Every change to J's Industrial, newest first, taken from the series' changelog
([CHANGELOG.md](../CHANGELOG.md)), which records each change once with the mods it concerns and keeps
the releases before 0.5.0a. This file is written from it by `./gradlew changelogs`.

## [Unreleased]

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
