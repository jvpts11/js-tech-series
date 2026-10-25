# Changelog of J's Computers

Every change to J's Computers, newest first, taken from the series' changelog
([CHANGELOG.md](../CHANGELOG.md)), which records each change once with the mods it concerns and keeps
the releases before 0.5.0a. This file is written from it by `./gradlew changelogs`.

## [Unreleased]

### API
- Each thing where it belongs: J's Core's API stays at version 12 and J's Computers' is at version
  8. The series' model has its own package: `dev.jstech.core.network`, `operation`, `peripheral` and `tier` are now
  `dev.jstech.core.jstechseries.network`, `.operation`, `.peripheral` and `.tier`, and the client side of the series'
  look `dev.jstech.core.jstechseries.theme.client`; `IMonitorable` and `INetworkNodeCapability` go to the network's
  package, `Era` and `Eras` to the tiers', the progression axes of eras and tiers to `SeriesAxes`, and the Operations
  engine's settings to `OperationSettings`. The platform names none of it: a cable's era is any `IAxisStep`
  (`CableType.step`, `Builder.describe(TextKey, IAxisStep)`, `IAxisStep.named` and `colour`), its plugs are by the kind
  of port they enter (`CableType.plug(String)`, `IPlugged`), a mod's networks keep their own grid
  (`INetworkGrids`, `CoreGrids.keepNetworksIn`, `networkOf`, `joinedNetwork` and `assignNetwork`), and a manual's
  highlighters are found through `GuideHighlighters.findWith`. `CoreRegisterEvent` gives way to
  `RegisterOperationTypesEvent`, for kinds of Operation, and the programming languages move to J's Computers' API,
  `dev.jstech.computers.api.language` (`IProgrammingLanguage`, `ILanguageProcess`, `IMachineView`, `LanguageRegistry`,
  `ExecutionBalance`), registered through `ComputersRegisterEvent.languages()` and read through
  `JsComputersApi.languages()`. `Device.fillsFromContainers` lets a bucket fill and empty a device's block, and
  `FluidBlockItem` is the item of a block that keeps its fluid in it. The old fixed catalogue of materials,
  `ModMaterial`, `MaterialForm` and `MaterialItems`, is gone: a mod declares the forms it uses with `MaterialUses.use`.
- Multiblocks worked from their terminal, entries where they say, and big blocks apart: J's Core's
  API stays at version 12, all experimental. The block a multiblock is worked from is its terminal, where it was its
  controller: `MultiblockPattern.Builder.terminal`, `terminalBlock()`, `terminalX()`, `terminalY()`, `terminalZ()`
  and `TERMINAL_CHAR`, `IStructureTerminal`, `LevelStructures.terminalHolding`, `MultiblockPartBlockEntity.terminalPos`
  and `setTerminal`, `FormedPartBlockEntity.terminal()`, `Projectables.terminalOf` and `addTerminals`,
  `ShapePreview.terminalBlock` and `withTerminal`, and `"terminal"` in a pattern's file. A place takes entries
  (`Place.entry`, `Place.Entry`, `MultiblockPattern.Builder.entry`, `"entries"` in a file), counted under their name
  and making their place a port of their kind, and two counts may go together (`MultiblockPattern.Builder.together`,
  `MultiblockPattern.Together`, `"together"` in a file, `ShapeMatch.Wrong.APART`, `MachineStructure.NEEDS_TOGETHER`).
  A block bigger than a block, placed whole, is a big block, in the new package `dev.jstech.core.bigblock`
  (`AbstractBigBlock`, `IBigBlock`, `IBigBlockGeometry`, `BigBlockFootprint`, and `BigBlockPartBlockEntity` with its
  `originPos`), which takes the place of `AbstractMultiblockControllerBlock`, `IMultiblockBlock`,
  `IMultiblockGeometry` and `MultiblockPatternGeometry`; J's Computers' Mainframe and racks stand on it.
- Hardware and computers as registries: J's Computers' API stays at version 8, all experimental.
  `ComputersRegisterEvent` declares parts (`processor`, `graphicsCard`, `memory`, `disk`, `motherboard`,
  `powerSupply`, `expansionCard`), the kinds they are made of (`memoryGeneration`, `slotGeneration`, `formFactor`,
  `cardKind`), what an era's software ships on (`eraMedia`), computers (`computer`), servers (`server`) and machines
  (`machine`), and `JsComputersApi` the same. `RamGeneration`, `PcieGeneration`, `FormFactor`, `ExpansionCardKind` and
  `HostScope` are records open as ids where they were enums; `PlainCardSpec` is a card of a kind of a mod's own;
  `ComputerDef` and `CaseParts` say what a computer is; `RackChassis` is a record of a server's `ComputerDef`, its
  height, its cabinet and its `Look`; `SmallComputerBlock` is the block of a small computer another mod brings. The
  part item classes (`CpuItem` and the rest) are gone: a part is an item and its entry. `IOperatingSpace.era()` gives
  an `Era`.

### Added
- Eras of other mods: an era another mod declares between the series' own is an era of hardware
  everywhere, its machines, its tables and its progress. Whatever a rule of the series says era by era, an era that
  says nothing takes the nearest earlier era's, as the Exa and the Singularity take the Advanced's.
- Hardware of other mods: any mod's item is a processor, a graphics card, memory, a disk, a board, a supply
  or a card because it is declared one with its sheet, and J's Computers' own four hundred parts are declared the same
  way. Its tooltip is written from the sheet, and it fits by the same rules as every other part. The generations of
  memory and of slot, the form factors and the kinds of card are open too.
- Computers of other mods: every computer is an entry saying its era, the boards its case takes, the
  machine it is and how many parts its case counts, the Personal, Crafting and Cluster Management Computers, the
  Mainframes and the servers among them. Another mod brings a small computer in the cases J's Computers ships, and a
  machine of its own, which only the programs made for it install on.
- Hardware from a modpack: KubeJS's startup scripts declare parts and their kinds, each as an object of
  its sheet's fields (`JsComputers.processor`, `memory`, `motherboard` and the rest).

### Changed
- Listeners declared: every class of the three mods that answers the game's events is
  handed to the buses from its mod's constructor, through a `listen` method of its own, rather than found by an
  annotation, so what runs is what the constructor says; a mod's mistake in what it declares stops the load saying
  what to change.
- Its own settings: how long the disks take to answer, the share of its capacity a Subframe lends and the
  time the computers' programs may take each tick leave J's Core's balance file for J's Computers' own,
  `jscomputers-server.toml`, in a "Disks and programs" section, where a server that changed them sets them again; the
  Operations engine's keep the series' file, `jstech-operations.toml`.
- Programming languages are J's Computers': a mod adds one through J's Computers' registration event,
  as Sigma and Sigma Sharp are added.
- Drawn by J's Core's models: the Mainframes, the racks, the drives, the Dock Station, the Pattern
  Encoders, the printers, the Redstone Interfaces and the small computers' cases and parts are drawn by J's Core's
  models from the same files, with their lamps, trays, discs and parts as before, and GeckoLib is no longer needed by
  any mod of the series.
- The Redstone Interface speaks the Core's signals: whatever reads sensors (a comparator's reader, a
  controller, another computer's interface) reads the strength its lens shows, read or emitted, and an interface
  whose lens is on a sensor or a machine reads what that says, as it reads redstone.
- Reduce motion is the Core's: the setting is J's Core's accessibility one, `motion.reduce` in
  `jstech-accessibility.toml`, and J's Computers' own client setting of that name is gone.
- Permissions: reading Soundfoundry's music catalogue again and acting on a Network Gateway from a
  computer are permission nodes of their own, which a server's permission mod grants by player, dimension and claim;
  without one, reloading the catalogue takes an operator and every player may act on a Gateway, as before.
- Eras in saves: an era is saved by its id (`jscore:legacy`), and progress along every axis by the
  step's name, so an era another mod inserts moves nothing a world saved. A world saved before starts its progress
  over, and the era advancements wait for the era by its id.
- Tooltips of the slots a card fits are one sentence for every card ("Fits a PCI-e slot").

### Fixed
- The Exa and the Singularity draw what the Advanced draws: the supercomputer rack, the server rack, the
  speaker and the Pattern Encoder's USB-C took the Standard's look in those eras.

## [0.5.2a] - 2026-10-14

Codename: Boron.

### Changed
- Racks and servers: Every storage server has two expansion slots, so its storage card has room beside a
  graphics card, which a board with no video of its own needs for a monitor. Only the Legacy storage server had two;
  the others had one, and a graphics card in it left no room for the Vaultis Array or the Vaultis Cache.

### Fixed
- Redstone Interfaces: A Redstone Interface placed facing up or down stands in its own block, its lens
  toward the block above or below it, without its feet. It turned on the bottom of its block instead and lay half
  inside the block under it. Its back is closed too: the gland where the cable plugs in and the bends of its wires
  showed their insides.
- Cases and parts: The fans of the cases, the coolers, the processors and the graphics cards no longer
  flicker where their blades cross, and a card or an M.2 drive no longer flickers along the edges of its slot. In the
  Advanced High Performance and Aesthetic cases, the frames of the two front fans no longer flicker against the water
  cooler's radiator behind them, and their outer sides are closed.
- Buses: The arrows on the External Storage Bus no longer flicker where the two of them meet.

## [0.5.1a] - 2026-10-14

Codename: Boron.

### Changed
- Hardware: A motherboard's tooltip says whether it drives a screen by itself: a server board has a screen
  output of its own, for its console, and every other board needs a graphics card, or a processor with graphics, for
  a monitor. The manual's page on motherboards says so too, and what to do when a card will not go into a server.

### Fixed
- Programs: A server in a rack runs Σ# programs as any computer does, each server its own: `sigma run`
  at its prompt starts one there, where it said the machine could not run programs, and a script runs on by itself,
  listed and ended by its number with `sigma ps` and `sigma stop`. What a server runs is kept on its Server item with
  the rest of its run, so it is still running when the world comes back, and it ends when the server is switched off,
  restarted or pulled out of its rack. A server's programs reach its drives, its network and the operations they ask
  of it; Gateways, Redstone Interfaces, sound and windows are still a computer's alone.
- Cases: The water cooler of the Advanced High Performance and Advanced Aesthetic cases lays its radiator
  across the two fans at the front, behind both of them, with its tubes coming down to it from the pump. It stood on
  end behind one of the fans, well above and below them.
- Assembly: Once a board is in, the window of a computer or a server draws only the slots the board and
  the case wire. The others were drawn dimmed and looked like slots waiting for a part, but took none, so a card held
  over one went nowhere without a word: a storage server whose one card slot held a graphics card seemed to have five
  more. Before a board goes in, every slot still shows dimmed where its part goes.

## [0.5.0a] - 2026-10-13 - The Start Me Up Update

Codename: Boron.

### API
- Versions: J's Core's API is at version 4 and J's Computers' at version 7. Everything this release
  adds to either, or changes, carries `@ApiStatus.Experimental`: it keeps the mark through this release and loses it
  when the next cycle begins. Each mod's API is now kept line by line, every type and member a mod can reach with the
  version that brought it, and the tests fail when the code and that list disagree; `docs/API.md` says how.
- Added: `ComputersRegisterEvent.graphicsProgram` and `JsComputersApi.registerGraphicsProgram`: a program
  whose windows draw through a surface says so on both sides, so the server counts the video memory they hold too. An
  ISA or a file opener registered after the game has loaded is refused with a line in the log.
- Added: `IsaSpec`, `ComputersRegisterEvent.isa` and `JsComputersApi.registerIsa`, which take over from
  `ArchitectureSpec`, `architecture` and `registerArchitecture`.
- Added: `EngineDef`, `EngineCapability`, `ComputersRegisterEvent.engine` and
  `JsComputersApi.registerEngine`. A mod registers a Network Operations Engine by its package (a program registered
  like any other), the dialect it speaks, the version it ships in for each age of Mainframe and the extras it offers;
  until it brings a planner of its own it plans the way the Midsoft IQL Server does. Its package is of the new kind
  `ProgramKind.NETWORK_ENGINE`.
- Added: the package `dev.jstech.computers.api.planner`, for the engines that take extensions (NextgreIQL
  among them): `IPlannerRule` weighs every plan the planner considers and is switched on or off per Mainframe;
  `IPlannerOperator` is a hint the dialect accepts after a statement; `IPlannerStatistic` shows among the planner's
  statistics and may say how long a step takes; `IExplainNode` adds notes under a plan's steps. A plan is a
  `PlanCandidate` of `PlanStep`s, whose cost a rule or a hint may change or which it may set aside, saying why. They
  are registered with `ComputersRegisterEvent.plannerRule`, `plannerOperator`, `plannerStatistic` and `explainNode`
  (or the `JsComputersApi.register...` methods of the same names).
- Added: `ProgramRequirement` and `ProgramSpec.requires`, with `requiring`: a program says what it needs
  of the network's engine (the engine it is written for and the oldest version of it, or the capabilities it uses),
  which is checked when it is opened rather than when it is installed.
- Added: kinds of component for Σ# programs' windows: `ComponentKind` and `IComponentValidator`,
  registered with `ComputersRegisterEvent.componentKind` (or `JsComputersApi.registerComponentKind`) in the adding
  mod's namespace; on the client, `ComponentRenderers.register` with an `IComponentRenderer`, which reports what a
  player did through `IComponentActions`.
- Added: programs with windows written in Java: `DesktopApps.register` with an `IDesktopProgramFactory`
  making a `DesktopProgram`, which draws itself or through a `SurfaceRenderer`, for whoever opened it; this replaces
  registering a window factory with the mod's own desktop code.
- Changed: what a program declares with `ProgramSpec.withRam` is what it holds on a machine of the Legacy
  era, grown on the later eras and shrunk on the Vintage; `ProgramSpec.legacyMb()` says it, with what a program of its
  kind holds when it declared nothing, and `ProgramSpec.ramMbOn(HardwareEra)` and `ramMbOn(HardwareEra, boolean)` say
  what it holds on a machine of that era. The forms of `ramMbOn` that took the system an `OsDef` names are gone.
- Changed: `OsDef` says its shell with a `ShellKind` (`shell`, where it had the id `shellId`), and so do
  `linuxDistro` and `terminalSystem`. `ProgramSpec` has a `description`, `of` no longer takes the display name
  (`named` and `described` give the English words), and `name()` is the name to translate. A
  `DesktopEnvironmentDef`'s native names are `TextKey`s, and so are what `nameOf` and `launcherLabel` give.
- Changed: `ComputerTerminalMenu` stands on the Core's `CoreMenu`, which now holds what it had from the
  menu it stood on before. Its layout constants (`INV_X`, `INV_Y`, `HOTBAR_Y` and the `STORAGE_` ones) and
  `storageSlotCount()` are gone: `invY()` and `hotbarY()`, with the new `invX()`, give where the inventory is drawn.
  `openingTab` says which tab a terminal opens on.
- Changed: an operating space's screen is handed an `IOperatingSpace`, a narrow face of the machine (its
  menu as the game's kind of menu, the machine, the monitor, the space and the era), in place of
  `ComputerTerminalMenu`, which is no longer part of the API. `IOperatingSpaceScreen.open` takes it and gives back the
  screen built on its menu.
- Deprecated: `ComputersRegisterEvent.architecture`, `JsComputersApi.registerArchitecture` and
  `ArchitectureSpec`. They still work, handing what they are given to the instruction set registration, and go in the
  next cycle; `ArchitectureSpec.toIsa()` gives the same values under the new name.
- Removed: `ArchitectureSpec.runs` (ask the `IsaSpec` from `toIsa()`), and
  `DesktopEnvironmentDef.programFor`.
- Changed: its advancements stand on J's Core's event trigger, `jscore:event`, with the events named in
  the mod's namespace (`jsc:post_passed`); `JscEventTrigger`, `JscTriggers.EVENT` and the mod's own `AdvancementTab`,
  `AdvancementSpec` and `ConditionalAdvancementProvider` are gone.

### Added
- Eras: Two hardware eras join the ladder: the Transition, between Legacy and Standard (the late 2000s,
  64-bit, blue BIOS, flat panels, systems and programs on DVD), and the Advanced, after Standard. Each has its own look
  on the computing screens: the Transition in navy glass, its headers, buttons and selected tabs in two bands over a
  line of sky blue; the Advanced light and flat, white panels on grey under a thin blue line. Both palettes can be
  recoloured by a resource pack like the others.
- Eras: Blu-ray discs, the Advanced era's medium: the BD-ROM and the rewritable BD-RE, each holding four
  times a USB stick. Advanced programs ship on Blu-ray, and its systems and services on the stick.
- Hardware: The Vintage era fills out, so a weak machine and a top one can be built in it: the Velocion
  5x86-133 on Socket 3; the Integra Pentix 75, 133 and MMX 233 and the Velocion K5 PR133 on Socket 7; the Integra
  Pentix Pro 150, 180 and 200 on Socket 8; and, on Slot 1, the Integra Celer 300A, Pentix II 300 and 450 and Pentix III
  600, with the MF AT Slot 1 Motherboard, the top of the era, that takes them with SDRAM and the first AGP. New cards:
  the Atrion Wonder VGA, the Atrion Rave Pro and, on the AGP, the Envya Prism TNT. New memory: the SIMM-16, the EDO-32
  and EDO-64, and the SDRAM-32 and SDRAM-64 the Slot 1 board takes. And the MF PowerBasic 200 and the Vaultis Trench
  HDD 200M.
- Hardware: The Legacy era fills out the same way, a socket at a time: the Velocion Duro 1300 on Socket A;
  the Integra Celer 2.0 and the Pentix 4 2.4C, 3.2C and EE 3.4 on Socket 478; the Celer D 325J and the Pentix 4 520,
  540 and 560 on LGA 775; the Velocion Semper 3100+ and Sprint 64 3200+ and 3700+ on Socket 754; the Sprint 64 3500+,
  4000+ and FX-55 on Socket 939; and the Integra Servo 2800, 3060 and 3200 for the servers on Socket 604, with an ATX
  board for each new socket. The Pentix 4 chips and the Servos run two threads a core. Thirteen new graphics cards run
  from the Envya Prism TNT2 M64 and the Atrion Radiance 7000 through the Vertex 4 Ti 4200 and the Radiance 9600 XT to
  the Vertex 6800 Ultra on AGP, and on the first PCIe boards the Vertex 6200 and 6600 GT and the Radiance X300 and X800
  XT, the top of the era. And the SDRAM-256, DDR-256, DDR-1024 and DDR2-512, the MF PowerBasic 350 and the Vaultis Link
  IDE-HDD 40G.
- Hardware: The Transition gets its hardware catalogue: on LGA 775 the Integra Pentix D 805, the Celer
  E1200 and the Centro 2 Duo, Quad Q6600 and Extreme QX9650; on AM2 the Velocion Sprint 64 X2 and the Ascent X4 9850; on
  AM3 the Sprint II and the Ascent X4 and X6, up to the X6 1100T; on LGA 1156 the Pentix G6950 and the first Centro c3,
  c5 and c7; on LGA 1366 the Centro c7 920, 960 and 980X and the Integra Servo 5520, 5570 and 5680; the Servo 5450 on
  LGA 771; and the Velocion Optera 2218, 8356, 8384 and 8435 on Socket F. A board for each: the ATX boards for LGA 775,
  AM2, AM3 and LGA 1156, the LGA 1366 workstation board, the two-way server boards and the Mainframe's MF MTX-T.
  Fourteen new graphics cards run from the Envya Vertex 7300 GS and the Atrion Radiance X1300 to the Vertex GTX 480 and
  the Radiance HD 5870. And DDR2 and DDR3 in three sizes each, a registered DDR2 for the servers, and the MF PowerBasic
  450B.
- Hardware: The Standard fills out: on LGA 1155 the Integra Celer G530 and the Centro c3 2120, c5 2500K and
  c7 2600K and 3770K; on LGA 1150 the Pentix G3258, the Centro c3 4160 and the Servo 1231 v3 beside the Centro c5 and
  c7; on LGA 2011 the workstation Centro c7 3820, 3930K, 4930K and 4960X and the Servo 2609, 4650 and 4657L v2; the
  Velocion FX-4300, FX-6300, FX-8350 and FX-9590 on AM3+, the Fuse A4, A8 and A10 on FM2+, and the Optera 6212, 6272
  and 6380 on G34. A board for each new socket: the ATX boards for LGA 1155, AM3+ (on the PCIe 2.0 its chipset had) and
  FM2+, and the two-way and four-way G34 server boards. Thirteen new graphics cards run from the Envya Vertex GT 730 and
  the Velocion Radiance HD 7750 to the Vertex GTX 1080 and the Radiance R9 Fury X, on the new Maxwell and Pascal
  designs, with the Envya Tessera K40 compute card for the servers. And the Stratix Layer DDR3L-4096 and the
  DDR3-16384 RDIMM of the servers.
- Hardware: The Advanced era gets its hardware, from 2017 to today. Sixty-two processors: the Velocion
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
- Hardware: Seventeen Advanced graphics cards run from the Velocion Radiance RX 550 and the Envya Vertex GTX
  1650 to the Radiance RX 7900 XTX and the Vertex RTX 5090, on the Turing, Ampere, Ada Lovelace and Blackwell designs
  and on RDNA 1 to 4, with the Envya Tessera V100, A100 and H100 compute cards of the servers. The Forge Logic Crafting
  Card T4 is the crafting card of the Advanced boards, and the Fabric DPU the cluster card that reaches eight nodes at
  once.
- Hardware: Processors and graphics cards name the architecture they are built on in their tooltip, with
  the chip's codename: "Architecture: Centro Conroe", "Architecture: Kepler GK110".
- Hardware: The graphics card is a resource. Each lit monitor holds video memory by its size in blocks and
  its era's colours (64 KB for a Vintage monochrome tube up to 256 MB for an Advanced panel, a big screen its every
  monitor), and a graphics program's window holds a quarter of a block, a whole one while it fills the screen. A
  monitor the card has no room for stays dark and says why on its own menu, and lights the moment there is room; a
  graphics program that does not fit does not open, and the notification area says how much it needs and how much is
  free.
- Hardware: The Integra Centro c5 4590, c5 4690K and c7 4790K carry graphics on the die, as their real
  counterparts do: they give the board one video output, so a machine with one drives a screen without a graphics card,
  and the Device Manager lists it. Those graphics borrow the system's memory, up to a quarter of it and 1792 MB at most,
  and hold of the RAM what the screens they drive need.
- Hardware: A Mainframe's graphics card queues run at most at the processor's speed and no faster than the
  card can (its cores, their clock and its design, cut by a slot older than the card). The queues tile turns amber while
  a card holds its queue back, and its tooltip lists each queue's speed and names the card behind each.
- Hardware: A server's graphics cards add a twentieth of an item a tick for each of their threads to what
  it sends out; its assembly shows the bonus beside its processor's capacity.
- Hardware: Four sound cards, one for each bus of the Vintage and Legacy boards: the Artisan Tone Blaster
  (ISA) and Tone Blaster 128 (PCI), which make their notes by FM and play recordings at 8 bits in mono at 22 kHz, and
  the Tone Blaster Live (AGP) and Tone Blaster Hi-Fi (PCIe), wavetable cards playing recordings at 16 bits in stereo at
  44.1 kHz; and the Tone Blaster Audigy, the sound card of the Legacy's PCIe boards. A sound card sits only on a board
  of its own era, in a slot of that board's bus, and a machine takes one. From the Transition on, a board has its sound
  built in, and its tooltip says so. A computer's tooltip says whether its case only beeps or its board plays
  everything, and a monitor's that its computer's sound comes out of it.
- Hardware: The Optical Network Card, from the Standard. The backbone's fibre enters a Mainframe or a
  Cluster Management Computer only while the machine holds one; without one the machine takes the copper of its
  backbone alone. It fits a PCIe slot and is drawn inside the computers that seat it. A server never seats it.
- Computers: The small computers drawn as the cases of their age: the Personal Computer, the Crafting
  Computer and the Cluster Management Computer fill their block as a real tower of each age, the left side a panel of
  its own: a beige AT minitower with its MHz display for the Vintage, a beige ATX tower for the Legacy, a silver and
  gloss black mid tower for the Transition. From the Standard age on each comes in three cases, a block each, that
  differ in look alone: Neutral, closed and quiet; High Performance, mesh and more fans; Aesthetic, glass and light,
  with red LEDs in the Standard and addressable light in the Advanced. A plate at the foot of the front tells the
  machines apart. The Transition and Advanced machines are new blocks, and so are the High Performance and Aesthetic
  cases of the Standard.
- Computers: A small computer's left side comes off, to see what is inside: sneak and use the case with an
  empty hand, or throw the SIDE PANEL switch beside the inventory on its assembly screen. It stays as it was left,
  through a save and for every player.
- Computers: Inside a small computer, each part the player installed is drawn by a model of its own, the
  one of that item: the board on the tray, the processor in its socket, the memory in its slots, the cards hanging in
  theirs, the supply and the disks where the case of that age keeps them, an M.2 drive on the board; and on the
  processor the cooler the case brings, unless the processor is a Slot 1 cartridge with its own. A part of another age
  sits where this case puts it, and a supply made for the other end of the case is turned over, without the leads made
  for its own board. In an Advanced case the hard disks sit in the cage under the shroud, hidden like the supply, and
  the helium disks above 8 TB are drawn each with its own sealed lid and label. The parts are drawn when the side is
  off, or through a case's glass or mesh. A board for two processors has no place in a small case yet and is not drawn.
- Computers: A small computer's power lamp is lit while it runs, and its disk lamp blinks while it works its
  disk, whatever the disk. Its fans turn while it runs: the case's own, and those of the parts in it, the cooler's and
  the cards'.
- Computers: The front of a computer's case. The Personal, Crafting and Cluster Management Computers have
  the 5.25-inch bays of their era's case, one on the Vintage and Standard cases, two on the Legacy and Transition ones,
  none on the Advanced. Four parts go in them, each in its era's tab: the Hot-Swap Drive Bay (Vintage), the Internal CD
  Drive (Legacy), the Internal DVD Drive (Transition) and the Internal Blu-ray Drive (Advanced), which is there to read
  the newest discs in an older computer. Any of the four goes in any bay, one to a bay. The cases are drawn with their
  fronts as they now are: the power button, the lamps and, from the Transition on, two USB ports on the left of the
  face, an empty bay showing its cover and a part in a bay drawn in it, a drive with its tray and the hot-swap bay with
  its caddy; a disc lies on a drive's tray and a USB flash drive plugs into a port, each drawn as the item it is. What
  the bays and ports hold falls out when the case is broken, or when the part holding it is taken out of its bay.
- Computers: Working the front of a case. A click on the power button switches the machine on and off,
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
- Computers: What the front holds, to the system. The hot-swap disk is one of the machine's own, with a
  letter, taking a system and booting it, keeping the machine's items (with its own share offered to the network on a
  Personal Computer's Storage heading), and counting in its capacity and its power draw as the disks inside do, though
  it takes none of the board's disk slots; taken out, it takes its items with it, and with the machine running from
  it, its system too, as on the assembly screen. Each drive and port on the front is a drive like the external ones: a
  letter, This PC (whose Eject opens a drive's tray), the explorers, installing systems and programs, the firmware and
  its boot menu, the Crafting Manager, the Pattern Studio and live media.
- Mainframes: The Transition and Advanced Mainframes. The Transition's cabinet is gloss black after the IBM
  z9 and z10, with brushed-silver fins down its sides, a honeycomb service door, a silver badge and a blue slit of light
  down the middle of its front; the Advanced's is near black after the z14 and z15, its door a perforated sheet folded
  into facets, with a line of white light up each side. Each takes the MTX board of its own age, and their lights come
  on while the machine runs on a build that makes a computer.
- Mainframes: A Mainframe has a power button on the sill in the middle of its cabinet, in every era: a click
  switches the machine on and off, whatever the hand holds, as the button on its assembly screen does, and it is
  outlined on its own while looked at; a click anywhere else on the cabinet still opens that screen.
- Racks and servers: The Transition and Advanced Server Racks and servers. The Transition rack is gloss
  black with brushed-silver posts, honeycomb rear doors and a header with a silver badge and a blue lamp; the Advanced
  rack is the era's white with hidden screws, finely perforated rear doors and a white line on the header. The
  Transition 1U server, after the HP DL360 G5 and the Dell PowerEdge 1950, has a brushed-silver face with four 2.5"
  drives, a slim DVD and a blue power light; the Advanced one, after the HPE DL360 Gen10 and the Dell R650, is black
  with slim NVMe carriers, a honeycomb vent and a white light. Both take two processors and five drives, the
  Transition's two cards and the Advanced's any number; each seats in a rack of its age or later, and every rack draws
  every server it can seat in that server's own shape. Their items and their empty cases show the server's face. A
  server's assembly screen is drawn in its chassis's era, and a server with no board yet in its chassis's era too.
- Racks and servers: The Advanced Supercomputer Rack and its node. The rack keeps the Standard one's shape
  (the hollow shell, the cooling unit on its roof with the coolant going in and out on different blocks, the livery
  panel over the service opening, a lit band per row) in the era's near black with white light, its livery a white
  trace stepping across it like a circuit. The Advanced Supercomputer Node, 2U, has four accelerator sleds with white
  latches, two NVMe carriers, a honeycomb vent and a white line on its ear; it takes the Advanced parts, up to four
  cards (the co-processor and three GPUs), and seats only in its era's rack, which seats the Standard node too and draws
  each node as it is.
- Racks and servers: A Storage Server and a Compute Server, with their cases, for the Vintage, the Legacy,
  the Transition and the Advanced, 2U each and drawn in their era's faceplate. A storage server has 5 drive bays in the
  Vintage, 7 in the Legacy, 8 in the Transition and 10 in the Advanced; a compute server 1, 2, 2 and 3, with up to 2, 2,
  4 and 4 processors and 1, 2, 2 and any number of cards. Each takes its era's server boards and seats in a rack of its
  era or a later one. The Legacy storage server has two card slots, one for its RAID controller and one for a graphics
  card, since its board has no video of its own and a monitor needs one to show the firmware.
- Racks and servers: The Vintage server board has ISA slots beside its PCI ones, and the Legacy server
  boards PCI slots beside their PCI Express, as the boards of their time did, so the era's storage cards, the ISA and
  the PCI Vaultis, seat in the servers they are for. A card in the older slot runs at that slot's own speed.
- Racks and servers: A KVM Switch, a Rack UPS and a Cooling Unit for every era, from the Vintage to the
  Advanced, each in its era's faceplate; the ones there were are the Standard's. A rack seats the units of its own era
  and of the earlier ones, as it does servers, and says so when a unit is too new for it. A Cooling Unit adds its era's
  watts to the rack's heat budget: 300 W for the Vintage one, 600 W, 1,000 W and 1,500 W up to the Standard, 2,500 W
  for the Advanced.
- Racks and servers: The Fibre Optic Interface and the Advanced Fibre Optic Interface, 1U rack units: a
  server rack takes the fibre backbone only through one mounted in any of its rows. The Standard one takes the Fibre
  Optic Cable; the Advanced one, which only an Advanced rack seats, takes the OM5 Fibre Cable as well.
- Racks and servers: The Advanced HBW Interface, the Advanced supercomputer's uplink, in the era's white
  with finned OSFP cages: it takes the OSFP fabric and every earlier high compute cable, and the backbone up to OM5. A
  Cluster Management Computer reaches the Advanced supercomputer through the Fabric DPU. An HBW Interface finds its
  nodes along any high compute cable of its era or an earlier one, in each Supercomputer Rack that takes that cable:
  the Transition's InfiniBand reaches the Standard's racks, and the OSFP only the Advanced's.
- Network: The data cables are lines by job, with a cable of each line for each era. Access, which joins
  the small computers to a router: Thin Coaxial, Ethernet, Cat 5e, Gigabit Ethernet and Cat 6a. Backbone, which joins
  the routers, the Mainframe and the racks: Thick Coaxial, HBW, 10GBASE-CX4, Fibre Optic and OM5 Fibre. Long distance,
  which will join two networks between Gateway computers: the Telephone Line, the Leased Line, the T3 Line, VLDC and
  Dark Fibre. High compute, for a supercomputer's nodes: InfiniBand from the Transition, High Compute and OSFP. And one
  Crafting Cable for every era. Each wears its own jacket and ends in its era's plug where it meets a device.
- Network: A machine takes its own era's cable of a line and every earlier era's, never a later one's, on
  every block of it, and two eras of one line touching do not join: they meet at a router of the newer era.
- Network: Speed and range count on every line. A run longer than its cable reaches carries nothing, so
  whatever is only beyond it is off the network; an Operation reading a server moves no faster than the slowest cable
  between the Mainframe and that server. Every cable's tooltip says how many cables one run of it reaches and what
  starts a new run (a Repeater, a Router or a Hub), and the manual's tables of reaches and speeds are read from the
  cables. The speeds and ranges are first estimates.
- Network: The backbone's fibre runs only straight: a cable laid against its side is left out, and the
  optical router is what turns it. A long distance line runs between two ends and takes no third, is thicker than the
  other cables, and shares no block. The long distance cables have no use until the Gateway computers come.
- Network: A router for every era: the Vintage Router, the Personal Router of the Legacy, and the
  Transition, Standard and Advanced Routers. Each joins its era's access line to its backbone, and takes the cables of
  both lines of its era and of every earlier one on any of its six faces, all of them one network. The small computers
  reach the network through a router: the access line goes from them to a router, and the Mainframe and the Server
  Racks take only the backbone.
- Network: The Optical Router, from the Standard, and the Advanced Optical Router take only the backbone's
  fibre, from the Standard's to their own era's, and are where it turns and branches.
- Network: A repeater for every era, from the Vintage Repeater to the Advanced Repeater. The access,
  backbone, high compute and crafting cables run through it each on its own, never joining one another there, and each
  run starts its reach over at it, so a cable can go twice as far with a repeater halfway. The routers, optical routers
  and repeaters wear the same face on all six sides, the connector of their era in the middle and two blinking lamps
  above it.
- Network: The Network Manager shows how each node is linked. The Devices tab has a LINK column with the
  cable and its speed, and a square for a fibre port; a machine joined to the network that lost its link is listed as
  "no link", and the line under the list says why: its fibre bends where no Optical Router turns it, or a run is longer
  than its cable reaches. The Map draws each link by its line (the fibre thick and aqua, an earlier era's cable in
  dashes, a link down red), with its speed, the Optical Routers the fibre goes through and a legend; a node's card has a
  NETWORK section. The Hardware tab, which now scrolls, adds the optical links up and down, the backbone and the slowest
  link in use.
- Operations engines: The Network Operations Engines. What makes a storage network work is software its
  Mainframe runs: every new Mainframe, of every age and system, ships with the Midsoft IQL Server installed and running,
  in the version of its age (4.2 on a Vintage Mainframe, 2000 on a Legacy, 2008 on a Transition, 2012 on a Standard,
  2022 on an Advanced). It is the IQL Engine of before under its own name, and it keeps the views and procedures.
  Everything that asks the network for work (the Network Interactor, the terminals, the buses, the gateways, the craft
  dialogs, the programs and the network's language) comes in by one door, the Network Operations Service, which hands
  it to the engine running; computers bind to the network, not to an engine. With no engine running the network still
  pulls, pushes, moves, exports and fills, but crafting, a craft's plan and the language are refused with "Network
  Operations Service unavailable", and the Network Interactor offers nothing to craft. A running engine weighs its
  memory on the Mainframe.
- Operations engines: An engine is the Mainframe's choice: a Subframe that runs another engine lends its
  Mainframe neither capacity nor queues until the two run the same one. Changing or stopping the engine hands only new
  work to the new one (or to none): every Operation already made carries on, a craft on the plan it was made with,
  after a reload too.
- Operations engines: A software house's tools are written for its own engine. The IQL Server Management
  Studio opens only on a network running the Midsoft IQL Server, from its 2000 version on; anywhere else it stays
  installed and says "No compatible Midsoft IQL Server was found on this network." An engine is a package of its own
  kind, and `install iqlengine` installs it the way every program is installed, from its disc at the Mainframe, written
  down with the system so the memory it holds is counted.
- Operations engines: The jobs are the Automation Engine's alone: "at this time, or when this happens, do
  that", the same whichever engine plans the network's work. A job is made only where the Automation Engine is
  installed (IQL's CREATE JOB says so where it is not) and fires only while it runs. The Automation Engine and its
  Manager now reach back to Frames XP, so a Legacy network keeps its jobs. The Manager's "Keep Stock" is now "Restock
  below", which says it does not count what is already on its way and points whoever wants a level held, counting
  it, to Prophet YourIQL's KEEP.
- Operations engines: The Network Manager has a Services tab, and opens wider so its seven tabs fit. At the
  top, the engine that plans the network's work: its state, the Mainframe it runs on, its dialect, the memory it takes,
  how long it has been up, how many requests it planned today, its indexes and what it can do, with Stop and Replace
  (Configure is there and does nothing yet). Under it, the engines installed, the Subframes (one that runs another
  engine is marked "takes no work", and a note says what would put it back to work) and the other services, the
  Automation Engine and the Mirror. With no engine running the card says what still works and offers Start.
- Operations engines: Replacing the engine asks first, saying where new requests will go, that the
  Operations in flight finish on their plans, that scripts in the old maker's own statements may stop working, and
  which Subframes will take no work. Then it takes time, more for a network holding more kinds of item (from two
  seconds up to two minutes), and the window shows the steps with a bar; the network has no engine until the new one is
  up. A replacement under way is written down with the Mainframe.
- Operations engines: The IQL Server Management Studio (the command and id `isms`), which takes over from
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
- Operations engines: The IQL Server Profiler, from the studio's Tools, in a window of its own: it records
  what the network's work does as it happens, the statements that come through the network's door, the Operations
  taken on and settled, the locks taken, the plans chosen for a craft and what the buses moved, each with who asked, on
  which computer, how many items and how long it took, a row's detail below the grid. A trace starts, pauses and stops,
  picks the groups of events it records, finds a row, replays a statement in the studio, and is saved to and opened
  from the computer's disk.
- Operations engines: NextgreIQL, a second Network Operations Engine, from the Nextgre house: the explicit
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
- Operations engines: The Nextgre Planner Studio, NextgreIQL's own tool, on any windowed system from the
  Legacy on: on its Explain tab a statement's plan as a tree of boxes, each with the time reckoned against the time it
  took, the hints marked on the boxes they changed, and beside it the plans weighed with their costs and why any was set
  aside; Explain (F7) and Explain Analyze (Shift+F7). Its Statistics tab lists what the planner reckons with, its Rules
  tab its rules, each switched on or off, and the hints it takes, and its History tab the plans made lately, any of
  which opens on the Explain tab. It opens only where NextgreIQL runs.
- Operations engines: Prophet YourIQL, a third Network Operations Engine, from the Prophet house: the
  state-oriented one. A Mainframe from the Legacy on installs it as a package, in the version of its age (3.23 on a
  Legacy, 5.0 on a Transition, 5.6 on a Standard, 8.0 on an Advanced). Told what state the network is to keep, it works
  out the crafts to get there and stay there: `KEEP item >= n` or `KEEP item BETWEEN a AND b` holds a level, counting
  what is already on its way so nothing is asked for twice, and says when nothing on the network can make it or when
  the level is over its band; `WATCH item < n DO statement` runs a statement once when a level crosses a line, and
  again only after it crossed back (`CRAFT item TO n` makes up to a level); `FORGET` lets either go and `SHOW STATES`
  lists them. It looks at the network once a second by default and does only what changed: on a network standing still
  a look costs one comparison. How often it looks, how much it asks for in one craft and whether it reacts at all are
  settings of the Mainframe. The Midsoft IQL Server does not speak these words.
- Operations engines: The Prophet Reactive Console, Prophet YourIQL's own tool, on any windowed system from
  the Legacy on: its States tab lists each state with where it stands, its level against its band, the work on its way
  and the last thing done for it, and under them the selected state's graph (the band, the level as it went, and where
  the work on its way takes it) with the Operations set going for it; its Subscriptions tab the watches, its Reactions
  tab what the engine did, its Settings tab how it looks and reacts. A statement is written at the YourIQL prompt,
  checked and applied. It opens only where Prophet YourIQL runs.
- Operations engines: A shared list of the network's Operations in flight, which the Network Manager's
  Processes tab and the studio's Activity Monitor both show.
- IQL: The network's language reads what it was always written with. A definition (a view, a procedure, a
  job) typed at the prompt's `iql` or written in a file of statements goes to the engine, where it used to answer "iql
  failed: null" or stop the file. An `IF` decides whether an action runs at all, reading the network's holding of its
  item. An `ORDER BY` sorts every row before the `LIMIT` takes the first ones. A `WHERE` on a `SELECT`, a `MOVE`, a
  `DELETE` or a `DROP` picks the variants it touches (a damaged tool, a name), where it used to be read and ignored
  without a word.
- Storage and buses: Buses for every era. The Import and the Export Bus come in a Vintage, a Legacy, a
  Transition, a Standard and an Advanced make. The Vintage bus moves an item a tick, one kind at a time, with no
  filter; the Legacy one eight, with a filter of up to five items (only these, or all but these), what the faced chest
  keeps and how many a move takes; the Transition one sixteen, with a keep and a max for each item it lists; the
  Standard one thirty-two, with a priority over the network's other buses and conditions to wait for (the network's
  stock of an item, the hours of the day, another bus having finished); the Advanced one sixty-four, filtering by tag
  as well and matching loosely, an item whatever its damage and components. The speeds are first estimates, and no bus
  moves faster than its cable carries.
- Storage and buses: A storage bus mounts on an access or a backbone cable of its era or an earlier one, on
  any cable block that holds a data or a crafting cable, on a face no wire crosses; clicked where it cannot go, it says
  why on the action bar.
- Storage and buses: A bus can be switched off, and keeps a log of what it did lately: each move it made,
  and each time it held back and why (what the chest keeps, no room, a condition). What a program set on a bus is
  marked with the program's name.
- Storage and buses: Each bus's window has three tabs, in its era's skin: Configure, the rows its era can be
  set to, which scroll above the inventory when they do not all fit; Activity, what it moved and why it held back, at
  the hour it happened; and Software, its address and the IQL statements and the calls that set it as it is set, with
  which programs set what. A setting a program set carries the program's mark until a hand changes it in the window.
  Conditions and tags are written in the window: the network's stock of an item or a tag, the hours of the day (one
  window of hours a bus), another bus having finished. A lamp at the right end of the title bar says whether the bus
  reaches the network, green or red, the word in its tooltip.
- Storage and buses: The External Storage Bus, of every era: it moves nothing itself, the network uses the
  inventory it faces as storage of its own, ten times slower than its servers. The Vintage one shows the network all of
  it; from the Legacy, a filter decides what the network sees, and the network may read and write it, only read it, or
  only write to it; from the Standard, a priority decides which storage the network fills first; the Advanced one
  filters by tag and matches loosely as well. Its window tells how many slots the inventory has and how many are in
  use.
- Storage and buses: What comes into the network fills the storage of the highest priority first, then the
  fastest.
- Storage and buses: IQL sets a bus by its name: `SET BUS 'Ore in' KEEP 16 MAX 64`, and the filter (only
  these, all but these, by tag), the loose match, each item's own quantities, the mode, the priority, on or off, an
  External Storage Bus's access, and what it waits for (the network's stock of an item or a tag, another bus); a job
  that switches a bus on between two hours of the day keeps it to those hours. A name in single quotes is read as one,
  as in SQL.
- Storage and buses: Programs set buses too, in both languages: `bus("Ore in").Keep(16).Max(64);`, with a
  call for every setting. A setting set from software carries the name of the program or the job that set it, which
  the bus's window marks, and what the bus's era cannot be set to is refused as it is in the window.
- Storage and buses: Each bus has a model of its own: a metal clamp round the cable, a ring in its kind's
  colour (green the Import, orange the Export, violet the External Storage, blue the Crafting Input Router, yellow the
  Crafting Receiving Bus, red the Crafting Interface, whose plate shows a slot for each pattern it holds), a funnel
  opening to the inventory for the buses that take from it, a nozzle for the ones that put into it and a wide thin
  panel for the External Storage Bus, chevrons along its sides pointing where the items go, and a plate against the
  inventory with its mark over its era's grille. Its housing is in its era's colours, the router's and the Receiving
  Bus's in the crafting line's amber, and so is its item. The buses are drawn with the cable in the world's mesh, their
  faces shaded as they are turned.
- Storage and buses: A bus's four lamps blink while it moves items, the External Storage Bus's while the
  network reads or writes through it and the crafting parts' while a machine is fed or emptied through them, and go
  dark a few seconds after.
- Autocrafting: The Crafting Interface, of every era, is where a network's machine recipes live: a part on
  the crafting cable that holds 3 patterns in the Vintage, 6 in the Legacy, 8 in the Transition, 9 in the Standard and
  12 in the Advanced, and feeds one machine. Set against the machine it feeds it directly, through that face; standing
  apart, it feeds it through a crafting cable of its own, dyed apart from the main one, with a Crafting Input Router
  against each face of the machine that takes an input. Its window shows the machine it feeds and the way there, its
  patterns and each pattern's inputs with the router each one goes through and why, its mode, its state and how many
  jobs it runs at once, what it is doing now and its last jobs, and its address and the IQL and calls that set it.
- Autocrafting: The Crafting Input Router, a part that does nothing of its own: an interface puts an input
  into the machine through it. Which router an input goes through is the one chosen for it in the interface's window,
  else the first whose filter takes it, else the first that takes anything; a pattern with an input no router takes
  cannot run, and the window says so. A router filters by every item its filter lists, up to five.
- Autocrafting: An interface is exclusive when it feeds through routers (one recipe at a time on its
  machine, jobs of the same pattern running together) and shared when it sits against the machine (several jobs at
  once, of different recipes too, as far as the machine takes them); either can be changed in its window, and so can
  the most jobs it runs at once (Auto, or 1 to 64) and whether it is paused. Two interfaces on one machine take turns.
- Autocrafting: The Crafting Receiving Bus ties itself to the interfaces of the machine it faces, or is tied
  by hand to an interface anywhere on the crafting cable. What the machine gives back is credited to the jobs that fed
  it, never more than the lots each was fed, in the order they were fed; an output that comes after its job settled is
  still owed to it, and an item no pattern declared goes to the network as unexpected; a fluid or chemical no pattern
  declared is the machine's own (an infuser's infusion, a generator's fuel) and stays. Its window shows what it
  credited, what came late and what was unexpected. It filters by every item its filter lists, up to five, and its
  window, like a Crafting Input Router's, wears the skin of the Mainframe that commands it.
- Autocrafting: A Crafting Card drives Crafting Interfaces and keeps a ROM of bench recipes of its own, both
  by its era: 2 of each in the Vintage, 4 in the Legacy, 5 in the Transition, 6 in the Standard and 8 in the Advanced.
  The Crafting Computers on a crafting cable drive its interfaces in the order the cable reaches them, as many as their
  cards drive between them; the rest wait for another card. The numbers are first estimates.
- Autocrafting: A craft whose recipe an interface holds waits for as long as that interface cannot feed it
  (its machine gone, the interface paused, no router taking an input); one whose recipe no interface holds fails after
  its timeout, and says so.
- Autocrafting: IQL sets the crafting network by name: `SET INTERFACE 'Mixer' EXCLUSIVE OFF`, `MAX JOBS 3`,
  `PAUSE INTERFACE` and `RESUME INTERFACE`, `SET INTERFACE 'Mixer' ROUTE 'Coarse dirt' INPUT gravel TO ROUTER 'North'`
  (or `AUTO`), a router's filter with `SET ROUTER 'North' FILTER ONLY gravel`, and `RENAME INTERFACE` and `RENAME
  ROUTER`. Programs do the same with `craftInterface("Mixer").Exclusive(true).MaxJobs(2).Pause()` and
  `craftRouter("North").Only("gravel")`, new in Σ 2 as `CraftInterface` and `CraftRouter`.
- Personal-use cards: The Crafting Table Card, the Furnace Card, the Enchanting Card and the Anvil Card, one
  of each for every era from the Legacy on, seated in any PCI, AGP or PCI Express slot of a Personal Computer. In the
  Device Manager each wears its own picture.
- Personal-use cards: The Workshop, by Autodeck, the program that works them on a Personal Computer, on
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
- Personal-use cards: UPDATE, the network's door to the personal-use cards, with the same rules, price,
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
- Personal-use cards: "Update..." in the Network Interactor, in an item's dialog and in the details panel:
  a window with a tab for each action (Enchant, Smelt, Repair), dim where the computer has no card or the item does not
  take it, the item with how many the network holds and where, the card and the computer, the three offers with their
  clue and price, the amount to smelt with the wait behind the Workshop's own smelting, and the repair of the most worn
  of the item with its material from the network, its name and its price beside an anvil's.
- Peripherals: Peripherals take ports by kind, as on a real computer, in place of one count of ports on the
  board: a monitor takes a video output of a graphics card (one on a Vintage card, two on a Legacy or Transition one,
  four on a Standard or Advanced one), a pair of speakers the audio output of the sound card, or of the board from the
  Transition on, and every other peripheral a device port of the board (two on the Vintage, then four, six, eight and
  ten). A server board has a video output of its own for its console. A machine without a graphics card has nowhere to
  plug a monitor. A peripheral with no free port of its kind waits unlinked and links when one frees; taking out a card
  unlinks the monitors it fed. The numbers are first estimates.
- Peripherals: A peripheral cable for each era, laid in the shared cable block beside the data cables: the
  Vintage beige cable (DE-9 at a screen, DB-25 at a device), the Legacy black one (VGA and USB), the Transition white
  one (DVI and the white USB), the Standard braided one (HDMI and USB 3) and the Advanced space grey one (DisplayPort
  and USB-C). Each ends in the plug of the port it enters. A run reaches 8 cables on the Vintage, then 12, 14, 16 and
  20. A port takes its era's cable and every earlier one, so a newer computer takes an older device's cable and an
  older device never takes a newer one; cables of two eras never join. A peripheral takes the cable on its back, where
  its model has the port, and a computer on any face; a peripheral against its computer still needs no cable. The one
  Peripheral Cable block is gone: the Standard's cable keeps its id.
- Peripherals: The Network Gateway's back has the Vintage serial port, the DE-9, in its middle, and it takes
  the Vintage serial cable, which every computer takes, so a computer of any era links it.
- Peripherals: A hub for each era, a plain block with six equal faces in the network tab: cabled to a
  computer, it takes one of its device ports and offers its own, two on the Vintage switch box, four on the Legacy and
  Transition USB hubs and seven on the Standard and Advanced ones. Every device cabled to a hub takes one of the hub's
  ports; when they are all in use the next device waits, or links another way that has a port free. Screens and
  speakers do not pass through a hub. The cable after a hub reaches as far again as the cable before it, and a hub may
  hang from another hub. A hub taken away drops the devices behind it.
- Peripherals: Disabled devices: a computer can disable a device on its ports, which stays linked and keeps
  its port while the computer neither reads nor writes it, and everything hanging from a disabled hub with it. A
  disabled monitor goes dark and opens none of its computer's screens, a disabled speaker plays nothing, a disabled
  drive's disc is not seen, a disabled Redstone Interface neither reads nor emits and no program finds it, and a
  disabled Gateway answers nothing. The computer remembers what it disabled.
- Peripherals: A Redstone Interface for each era, in the devices tab: a small sensor on the peripheral
  cable, drawn as the sensor of its day, that lets its computer read a redstone signal or emit one. It is placed as an
  observer is, its lens toward what the player looks at, any of six ways, and only its lens reads or emits; emitting,
  it powers the block in front as a lever does. Its lens shows the strength it reads or emits, its green lamp that it
  reads and its amber one that it emits. It takes a device port, or a hub's, and with no computer to power it reads and
  emits nothing.
- Peripherals: The Redstone Interface's window, in the skin of its era: its name, unique among its
  computer's interfaces, the computer it hangs from and the signal at its lens, the IN and OUT buttons, the strength it
  emits chosen by clicking one of sixteen cells, and the same setting written in IQL and in Sigma. Programs set the
  Redstone Interfaces of the computer they run on: `redstone("Gate").Out(15)`, `.In()` and `.Level()` in Sigma and
  Sigma#, and `SET REDSTONE 'Gate' IN` or `SET REDSTONE 'Gate' OUT 15` in IQL. What a program set is marked with its
  name, as on a bus, and a setting made in the window clears the mark.
- Peripherals: Two Pattern Encoders for the new eras: the Transition one, a LightScribe DVD burner in silver
  and gloss black that writes DVDs and CDs, and the Advanced one, a Blu-ray writer in space grey with a white front that
  writes Blu-ray discs and, in its USB-C port, sticks. And the Blu-ray Drive, white all over with its slim tray high on
  the front, which reads Blu-ray discs and the older DVDs and CDs. Each takes its era's cable on its back, and each
  opens and closes its tray with its eject button and the sound the other disc drives make.
- Peripherals: Speakers, linked to a computer over the peripheral cable like a monitor, one for every era
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
- Peripherals: Printers, one for each era, each the printer of its day on a period base: the Epsilon FX-80
  dot matrix, the Pakard DeskJot 940 and FotoSmart C4280 inkjets, the Pakard LaserJot 1102 laser and the Epsilon EcoTonk
  ET-2720 ink tank printer. A printer is a peripheral of a computer, on the port of its era in the middle of its back,
  and prints whatever the computer's programs send it: a sheet of paper from its tray for each page, a page taking as
  long as the printer's sound lasts, the page sliding out and the busy lamp blinking while it prints. Documents wait in
  its queue in the order they came; with the tray empty or no room for the next sheet in the output the queue waits,
  and paper clicked into a full tray says the tray is full. Its window, in the skin of its era, shows the paper, what
  prints now and its page, the queue with the machine each document came from, the sheets that came out, and Pause and
  Cancel job. A printer's tooltip says whether it prints in colour.
- Peripherals: The Printed Paper, the sheet a printer turns out: its tooltip gives the title, the pages, the
  first lines and the printer, and a right click reads it page by page, on the fanfold paper with its green bars and
  tractor holes from the dot matrix and a plain sheet from the others. A printed picture shows on it as that printer put
  it on paper, and in an item frame it shows on the frame, as a map does.
- Peripherals: Print in the programs: the Editor, Exceed, the IQL results of the management studio, the
  Network Manager's log, the Network Interactor's list, Exposure's open file, and Paint's picture. Each printer prints a
  picture in its own hand, at the resolution a real one prints at, drawn smoothed as paper is seen: the Epsilon FX-80
  in the round dots of its ribbon, the Pakard DeskJot 940 in grainy dots of four inks with paler rows from its head's
  passes, the FotoSmart C4280 in dots of ink too fine to see, the LaserJot 1102 in a fine halftone of toner, its greys
  growing a dot at a time, and the EcoTonk ET-2720 as it is. The Print window is each system's own: the classic one of
  Frames 95 and XP and the older desktops, with the printer, its status, where it is and its paper, the pages, the
  copies and the way the page lies, and on Frames 11 and the flat desktops of today the same beside a preview of the
  page; it says beside each printer's name whether it prints Mono or Color, and counts the sheets before anything is
  sent.
- Peripherals: The Dock Station is redone as a full block, a black aluminium enclosure on four feet with
  three trays and the USB port: a hard disk in the 3.5" bay, a SATA disk in the 2.5" bay and an NVMe in the M.2 tray, of
  any era, each shown in its tray's window with its lamp, and the flash drive in the port, lying across as the port
  does. A disk going in or out of a tray sounds like a server on its rails. Its window lists each tray and the port
  with what it holds, the letter the computer gives it, its era and how full it is, whether it is mounted, and Eject.
  It takes the peripheral cable of every era, so an Advanced computer reaches it with its own cable and installs Frames
  11 from a stick in it, and a stick that installs a system or a program says what it installs.
- Peripherals: A disk in a Dock Station's tray is an external drive of the computer the dock is linked to,
  lettered after the dock's stick: This PC and the explorers list it and open it, the shells read and write it by its
  letter, and on Linux, UNIX and FreeBSD it is mounted under `/media` or `/mnt`.
- Monitors: Eight monitors, each drawn from a real monitor of its time: on the Vintage the Mono I
  (paper-white), the Mono II (green, the Vintage Monitor that was), the Amber and the sixteen-colour CGA; the Legacy
  Monitor, a colour picture tube; the Transition Monitor, a 19-inch flat panel; the Monitor; and the Color Monitor, a
  27-inch IPS panel, for the Advanced. Every screen has the desktop's shape, and the era's video port in the middle of
  the back.
- Monitors: A monitor's face shows live what a player standing at its machine sees: its self-test, its
  system coming up or going down, its boot manager, its firmware and its installer, each drawn whole by the very screen
  that shows it at the machine; its prompt with the last lines printed, in the terminal's own font; or its desktop with
  the folders, files and pinned programs the machine keeps and the windows it has open, each drawn by the program
  itself when the player's game has it open or kept from the last visit. The server sends the screen a phase opens with
  once, when the phase begins, and only to the players near it; each player's game draws it, often close up, slowly a
  little further off, once a second past thirty-two blocks, and as far as the game draws, sixty-four blocks; a room of
  many monitors draws each one's picture in turn, so a room of screens costs the server nothing.
- Monitors: The monitor's tube colours what reaches its glass: on the Mono I, Mono II and Amber every
  system comes out in that one phosphor, brighter or dimmer, and on the CGA as the nearest of its sixteen colours, both
  on the opened screen and on the face in the world. The Vintage screens no longer paint themselves green: the monitor
  does. A program that takes a terminal's whole glass (an editor, the diagnostics, the shells) reaches the player
  through the tube as the console does: on a green or amber monitor it is green or amber too.
- Monitors: Flat monitors of one kind side by side (Transition, Standard and Color) join into one screen,
  up to eight wide and six tall, when together they fill a whole rectangle: one bezel round all of it, one chin, one
  power button, and the picture across the whole glass. It counts as one monitor on one video output.
- Monitors: A monitor has a real power button standing out of its bezel, outlined on its own in the series'
  accent while it is looked at: it switches the monitor alone, and the computer behind it runs on, switched on and off
  at its own case or on its assembly screen. Switched off, the face goes dark and its lamp goes out, every screen open
  on it closes, and a click on its glass opens nothing and says "The monitor is off". Switched on with no picture, a
  monitor from the Legacy on sleeps with its lamp amber, as monitors with power saving did, and a Vintage one keeps its
  lamp lit. A big screen switches whole from its corner's button. A click anywhere else opens the screen. An opened
  screen has a strip beside its frame on the left with the computer's Power and Restart, each saying what it does under
  the pointer, on every screen the monitor shows, its start, its firmware's setup and an installer included.
- Operating systems: Frames 7, the Frames of the Transition, installed from its DVD through its own setup
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
- Operating systems: Frames 10, the Frames of the Standard, whose Command Prompt opens on "Midsoft Frames
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
- Operating systems: KDE and GNOME have the faces they had on Transition hardware. KDE is KDE 4's Plasma: a
  light, see-through panel with the orb that opens Kickoff (Favorites, Applications with each program's description,
  Computer, Recently Used and Leave, which shuts down or restarts), the pager of four desktops, the window list, the
  tray and the clock; the Folder View on the desktop holding its files, the cashew in the corner offering the desktop's
  settings, and Oxygen windows with rounded top corners. GNOME is GNOME 2 with Clearlooks: Applications (the programs by
  what they are for), Places and System along a top panel with the launchers, the clock, the user and the power; a
  bottom panel with the show-desktop button, the window list, the switcher of four workspaces and the trash; and blue
  titles. Both keep the desktop's wallpaper and the icons with Frames XP's gloss, and Soundfoundry wears its Legacy form
  on them, as on FreeBSD there.
- Operating systems: The Experience Index of Frames 7: five scores from 1.0 to 7.9 read off the machine's
  parts (the processor, the memory, the desktop's graphics, the graphics of games and the main disk), each growing by
  about a point when the part doubles, and the base score, the lowest of the five. The System page of the Control Panel
  shows them under the machine's parts.
- Operating systems: The basic look. A desktop that drew its effects on the graphics card (Frames 7, 10 and
  11, Cinnamon, KDE from its Transition face and GNOME from its Standard one) needs a graphics score of 3.0 for them;
  under that, and always on graphics that are part of the processor, it runs with every effect off whatever its
  settings say. Frames 7 drops to Frames 7 Basic, opaque windows in its own colours, and says so in its notification
  area; every effects page keeps the choices and says why none of them shows.
- Operating systems: Every era's creative tab has, after its installers, a disk with each of its systems
  already on it, the way a computer comes from the shop: put in a machine of that era, it comes up at once as "My
  Computer". Each is the era's biggest disk, so the most room is left once the system is on it.
- Operating systems: A part taken out of a running computer brings its system down: a stick of memory, an
  expansion card or the disk the system runs from puts Frames on its red page, naming the part on Frames 10 and 11;
  Linux and FreeBSD panic on a machine check, and UNIX on a memory parity error. A machine left with what it needs (the
  other stick of two) starts over once the page has stood its time; one left with no computer goes dark. The processor,
  the board or the power supply taken out switch the machine off at once, with no page, and a disk the system does not
  run from or a drive in a hot-swap bay is only a disk gone.
- Operating systems: `ps` on Linux, FreeBSD and UNIX lists the system's own processes before the programs:
  its first process at number 1, systemd on the distributions that start it and init on Gentoo, FreeBSD and UNIX, then
  each service it runs, at a number that service keeps. The programs a player starts are numbered from 1000, after
  them, on every system. `kill` takes a signal before the number (`-9`, `-KILL`, `-SIGKILL` or `-s KILL`, TERM when none
  is given) and `kill -l` lists them. A signal to a service's process stops the service until the machine starts over,
  and one to process 1 ends the system's init: the kernel panics in its family's own words, `Attempted to kill init!`
  with the signal's exit code on Linux, `Going nowhere without my init!` on FreeBSD, `init died` on UNIX, with the line
  that did it at the top of the page, at the prompt it was typed at.
- Operating systems: FreeBSD installs through bsdinstall, in its own grey dialogs on navy. It opens with
  Install, then asks for the machine's hostname, whether to add the ports tree, the disk (the whole of an empty one,
  beside a system already on one, or the whole of one that is erased first, after asking), where `pkg` and the ports
  fetch from (the network's Mirror, or nowhere), which services start at boot (`cron` and `sshd`) and, when the Mirror
  answers, a desktop to install with it, None always offered. It extracts the system with a gauge and ends with Reboot.
  With no Mirror it fetches nothing, and a ports tree asked for is laid down empty for `portsnap fetch` to fill.
- Operating systems: UNIX installs from its own console installer, which names the parts of its one copy as
  it writes them, offers to erase a disk that already holds a system or to install beside it, and ends with Reboot.
- Operating systems: `cron` is a switch, FreeBSD's `cron_enable` in `/etc/rc.conf`, on unless bsdinstall
  turned it off. Off, the jobs in a machine's crontab wait instead of running, while a job put in the background with
  `&` runs anyway. FreeBSD says "Starting cron." as it comes up and FreeBSD and UNIX say "Stopping cron." as they go
  down, while it is on.
- Operating systems: On a FreeBSD installed through bsdinstall, other computers reach the machine only when
  `sshd` was ticked on the services page; it shows as `sshd_enable` in `/etc/rc.conf` and is the same switch as `config
  remote`. Every other system keeps letting them in, as before.
- Operating systems: Choosing a faster disk on an installer's disk page makes the copy quicker, and the
  time left changes to match.
- Operating systems: `ee` on FreeBSD, the editor it gives a newcomer: its five rows of shortcuts along the
  top, the row saying where the caret stands, and its Esc menu drawn over the text, whose items all work (leave the
  editor, saving or not; a page of every key, closed by any key; read a file in or save the one open; show or hide the
  rows of shortcuts; search). `vi` on FreeBSD and UNIX speaks each system's own, with no Vim status line. Both come with
  the system and are listed in `/usr/bin`.
- Operating systems: FreeBSD's console welcome names the three ways to find things, `apropos` for a command
  by what it does, `man intro` to learn the system and Tab twice for every command here, above the day's tip. `man
  intro` is a real page, and on every Unix console a second Tab in a row lists what the first could complete to, every
  command on an empty line, as sh and bash do. FreeBSD has no `help`: typed there, it is not found like any other
  unknown word, and the line points to `apropos` and `man intro` instead.
- Operating systems: KDE Plasma, GNOME and Cinnamon on FreeBSD hang FreeBSD's own wallpaper for each, in
  FreeBSD's reds with its orb, in place of the one they bring on a Linux.
- Operating systems: This PC on KDE Plasma, GNOME and Cinnamon, on FreeBSD and on the Linux distributions
  alike, is the page each desktop has for it: KDE's Info Center, GNOME's About and Cinnamon's System Info, each laid out
  its own way. They show the system with its release, the kernel and its build, the architecture (vel64 or IA-32 on
  FreeBSD, x86_64 on a Linux), the desktop, the processor, the memory, the graphics card, the system disk and how much
  of it is used, and the machine's host name.
- Operating systems: The ports tree on FreeBSD. `portsnap fetch extract` brings the tree from the network's
  Mirror and lays it out under `/usr/ports`, a folder for every program the Mirror serves, filed by category, as real
  files that take room on the disk; `portsnap fetch update` brings it up to date. In a port's folder, `make install
  clean` fetches the program's source from the Mirror and builds it on the machine, for as long as its processor takes
  with all of its cores, then installs it and cleans up. `make` on its own builds, a later `make install` finds that
  build instead of making it again, and `make clean`, `make reinstall` and `make deinstall` do what they do on FreeBSD.
  A program that installs only on one kind of machine, as the Crafting Manager does on a Crafting Computer, is refused
  elsewhere. Building a port earns the new Built from Ports advancement.
- Operating systems: A program built from source on the machine that runs it asks ten percent less of it
  than the package does: less memory while it runs, a lesser processor, and less free disk to install. That goes for a
  port on FreeBSD and for everything Gentoo builds. The machine remembers which of its programs it built until the
  program is removed or the disk it was on is formatted; the same program installed again as a package asks what a
  package asks.
- Operating systems: The Vintage systems get their text-mode shells, drawn in the sixteen colours of the
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
- Operating systems: A Vintage machine runs what `scc` compiles, though it can never hold the Sigma Runtime:
  a compiled listing runs by its bare name at the prompt, on MC-DOS without its extension the way DOS finds a program,
  and on UNIX from the current directory or the PATH. `scc` says how to run what it just built, and UNIX answers a word
  it cannot find in its own shell's words, `hello: not found`.
- Operating systems: MC-DOS prints from its prompt with `PRINT`, which installs its resident part the first
  time and lists what is printing and what waits; UNIX, Linux and FreeBSD with `lp`, which answers with a request id
  (`-d` for the printer, `-n` for the copies), and `lpstat`, which lists the requests and, with `-p`, the printers.
- Operating systems: The Unix shells' `echo` takes `-e`, reading `\a` (the bell), `\n`, `\t` and `\\` in
  its text.
- Operating systems: screenfetch on Frames 95, XP and 11: a package of Frames' own manager, run at the
  Command Prompt, drawing the Frames flag with the version each gives for itself. Professional Larper now asks for all
  nine systems it installs on.
- Devices: The device tools at a Unix prompt: on Linux `lspci` lists what sits on the board (the board
  itself, its port controller, its audio and network, the graphics and expansion cards) and `lsusb` what is plugged
  into its ports, `lsusb -t` as a tree with each hub's ports under the hub and the free ports too; on FreeBSD `pciconf
  -lv` and `usbconfig`. A device the computer disabled is marked by each.
- Devices: The Device Manager on the Frames desktops, with an icon of its own, a monitor with a gear before
  it: the machine's hardware and every port it has, what is plugged into each and which are free, hubs with their own
  ports under them, and how many device ports are in use. Devices are listed by port, by type or by connection, and a
  device is disabled or enabled again from it, crossed out while disabled. Frames 95 keeps it in System Properties with
  the views as radio buttons and the actions as buttons under the tree; Frames XP has its console window, the views in
  the View menu, the actions on a right-click and in the Action menu, and a status bar that says what disabling a
  device does; Frames 11 has the views as a switch in its menu row.
- Devices: The Midsoft Diagnostics on MC-DOS and MC-NET, which have no Device Manager: `msd` takes the
  whole terminal and shows what the machine is made of (its processor, memory, graphics card, network, system and
  drives), and LPT Ports and COM Ports open the list of the board's ports with what is attached to each and whether it
  is on, where a device is enabled or disabled from the keyboard.
- Devices: KDE's Info Center has a second page, Devices by port, beside About this System: every graphics
  card with its outputs, the audio and the board's ports, each port beside what is plugged into it or free, a hub with
  how many of its own ports are in use, and a button that disables or enables the device selected.
- Devices: CDE's Workstation Info has a Devices... button that opens the workstation's devices in a dialog
  of their own: Video, Audio and every port by its name (COM1 and LPT1 on a Vintage machine) with what is plugged into
  it or free, and Disable and Enable for the device selected.
- Devices: Every row of the Device Manager and of the Info Center has its icon, drawn in the look of the
  desktop it is on (the period one of Frames 95 and the older Linux desktops, Frames XP's with its gloss, the flat
  modern one): the board's parts, every port, and every device that stands on one, the monitors, speakers and drives of
  each era with their own.
- Desktops: The Task Manager of every system shows the graphics card: Frames XP a Video Memory meter, its
  history and a box of its figures; Frames 11 a GPU page with how busy it is, how full its memory is, its cores and
  slot, and what holds its memory; the Plasma monitor a GPU card beside memory and processor; GNOME its video memory
  history; the System Monitor the card's name, its memory and its load.
- Desktops: The desktops move, each with the timings of the system it imitates. Frames 11, Plasma, GNOME and
  Cinnamon grow a window in out of clear as it opens and shrink it away fading as it closes (the window is gone from
  the desktop at once, and drawn going away for as long as that takes), each window fading as one picture, and carry it
  down to its button and back when it is minimized and restored; on Frames 95 and Frames XP only the title bar flies
  down to the taskbar and back, and windows open at once, as they did; KDE 2 and 3 and GNOME 1 send its outline to the
  button. The launchers slide out of their panels, fading in on the modern desktops (KDE 3's menus roll), Frames XP's
  menus and tooltips fade in, and GNOME's overview grows in. A desktop that comes straight up from its boot picture
  comes up under that picture's colour, which gives way to it. A monitor's face in the world shows everything where it
  ends.
- Desktops: More of each system's own motion. Frames XP's balloons fade in and out, and the desktop behind
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
- Desktops: The desktops draw the player's pointer over the monitor's glass in their own system's cursors,
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
- Desktops: A file copy takes time: its size read at the pace of the slower of the two volumes (a hard
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
- Desktops: Each system's own page for its visual effects, in the Settings window, kept by the machine:
  Frames 95's Effects from the Display page, Frames XP's Performance Options from the System page (its presets, and a
  box for each effect, the shadows under the icons' names among them), Frames 11's Visual effects, Plasma's animation
  speed and desktop effects, KDE 2 and 3's window behaviour and menu effect, GNOME 1's wireframe, GNOME's Reduce
  Animation, and Cinnamon's effects, speed and the style of opening, closing and minimizing windows, each from
  Personalize. The pages that were dialogs keep their OK, Cancel and Apply. Only the effects there are on these desktops
  are listed; CDE and the text consoles have none. `config effect <name> on|off` and `config effectspeed <percent>` set
  the same from a prompt.
- Desktops: J's Computers' own client settings, beside the game's options: Reduce motion, off by default,
  which draws every desktop's windows, menus and boots where they end and holds every blinking and turning thing still;
  and Desktop cursors, on by default, for the desktops' own pointer.
- Desktops: New wallpapers, built from blocks the way the game's own art is: a stepped grass hill under slab
  clouds for Frames XP, a blue flower drawn like an item for Frames 11, a wall of Breeze-blue blocks for KDE Plasma, a
  blocky dusk with a square sun for GNOME and green block stairs for Cinnamon; Frames 95 keeps its plain teal. Frames 11
  hangs a dark version of its flower while its dark theme is on. They are pictures a resource pack can replace, under
  `assets/jsc/textures/gui/wallpaper/`, and the thumbnails in Personalize, which scrolls when it holds more than its
  window does, are the pictures themselves.
- Desktops: The rest of what the desktops drew out of rectangles is pictures too, in the same block style:
  each Frames edition's mark on its Start button, the sky Frames 95 starts on and the bands Frames XP welcomes and
  closes on, the marks KDE Plasma and Cinnamon come up behind, the pictures on CDE's front panel, the network and
  speaker icons of every panel, and the picture down the side of the Frames 95 wizard: a computer, a boxed program and
  a disk. The clock's hands, the calendar's day and everything that moves are still drawn over them.
- Desktops: File icons are pictures in each desktop's own style: a Frames 95 folder is not a GNOME one.
  Seventeen kinds of file, from folders and documents to Sigma source, programs, packages and pictures, each drawn at 16
  by 16 for every desktop, in the explorer, the file dialog, the studios' trees and on the desktop itself. They are
  files a resource pack can replace, under `assets/jsc/textures/gui/file/`.
- Desktops: Icons for every program on CDE: twenty-eight programs that run under it, from the Network
  Management Studio to Vim, Emacs and the desktops themselves, wore the Frames 95 icon there and now wear CDE's. The
  Help Viewer has an icon on every desktop it runs on, Workstation Info has one outside CDE as well, and a setup, the
  welcome and a window a Sigma program opens show their own icon on the panel and the title bar instead of the plain
  one.
- Desktops: A resource pack can recolour the computing screens. Every colour of them is a palette, under
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
- Desktops: Text is drawn plain, with no shadow, on every screen of the series: the labels, fields, menus,
  lists, buttons, tabs, terminals and the machine screens, which used the game's dark shadow and smeared on the light
  panels, and the last that kept one, Frames XP's window titles, its Start button and its Start menu's name and
  buttons, and the labels and check boxes of a window a Σ program opens. In J's Core, whoever paints a background still
  says which colour it is, and the rule for a shadow worked out from the letter and that ground is kept, so a shadow can
  come back where it reads better.
- Programs: Soundfoundry, Voidsoft's music player, for the desktops of the Legacy era and later. Its
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
- Programs: Soundfoundry's sharing window, which NET opens, in the same skin: a search finds songs by their
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
- Programs: Soundfoundry Server, a Voidsoft service for a server in a rack: it streams the music catalogue
  and the network's own library, which is the songs in the server's music folder, to the computers of the network. A
  song a computer sends it lands in a folder named after that computer. Each computer listening holds 4 MB of the
  server's memory, which the server's memory listings show, and a server with none left turns the next one away; a song
  playing from a server that goes off or leaves the network stops and says why. Several servers can serve one network.
- Programs: Soundfoundry on the Standard desktops, Frames 11 and the Linux desktops of that era, is
  Voidsoft's streaming player: a home page with the catalogue's albums, the network's songs and those downloaded; an
  album's page with its songs, each marked as on the disk or only streamed, and a button that downloads the album; a
  search through the catalogue, the network's library and the computer's own files; the computer's own files, brought
  from the player's computer or sent to the network's library; playlists, the liked songs first, kept as `.m3u` files;
  and the Soundfoundry Server streamed from, picked when there are several. Playing an album, a playlist or a page's
  songs puts them on the list; a song on the disk plays without a server. Songs download and go up at the speed of the
  slowest cable between the computer and the server. Its window wears Voidsoft's graphite and orange on every desktop,
  and is the first window drawn in its own skin that can fill the desktop.
- Programs: Covers on the Standard Soundfoundry: a catalogue album shows the `cover.png` (or `cover.jpg`)
  beside its songs, any other song the picture its own file carries, and a song with neither a cover made of its
  album's colours and initials.
- Programs: Music files on a computer's disk: Ogg Vorbis (`.ogg`) and Wave (`.wav`), with an icon of their
  own in every desktop's style. A song is brought from the player's own computer, picked in their system's file dialog,
  into the system's music folder (`Users/Public/Music` on Frames, `Music` in the home folder on Linux, FreeBSD and
  UNIX), and is refused before it is sent when the disk has no room for it; one that finds the room gone by the time it
  has arrived is said not to have gone in, rather than to have. The recording stays on the server and the disk keeps a
  file naming it, but that file weighs what the song weighs, by the disk's era like any other file, and an archive with
  songs packed in it weighs them too; the Archiver shows their sizes in megabytes. A song cannot be edited or added to,
  renamed into text, or made out of text.
- Programs: The server's music catalogue: the albums its owner puts in
  `config/jstech/soundfoundry/catalog/`, a folder each, and those a data pack carries in `soundfoundry/catalog/`, named
  by an optional `album.json`, else by what their songs' tags say, else by the folder. It is read when the server starts
  and when its data packs are reloaded, and `/soundfoundry catalog reload` reads it again for an operator, saying how
  many albums and songs it found; a file it cannot read is passed over and named in the server log. `catalog` under
  `[soundfoundry]` in `jscomputers-server.toml` turns it off. The documentation has a page on music on a server.
- Sound: J's Computers' machines make their sounds, from real recordings. A computer's power button clicks;
  a computer beeps once when its self-test passes, through the Transition, whose firmware is still a BIOS, while from
  the Standard on, with a UEFI, a machine comes up quiet; a Vintage computer comes on with the noise of its fan and
  drives; a computer with a hard drive hears it spin up, turn while it runs and wind down when it goes off, while one
  with a solid-state disk stays quiet. A monitor sounds as it lights. The drives sound as media go in and come out: a
  floppy disk slides in and is ejected, the tray of a disc drive and of a Pattern Encoder rides out and in, a USB drive
  is plugged into and pulled out of a Dock Station, and a Floppy Drive's head is heard stepping while a system or a
  program installs from its disk. Each running server in a rack is heard by its fans, and five or more running close
  together, in any number of racks, are heard as the hum of the room instead.
- Sound: More machine sounds: a Vintage or Legacy computer switched on with parts that do not make a
  computer (no processor, no memory, parts its board does not take) beeps its self-test failing; a hard drive is heard
  seeking while its machine boots, installs, or reads and writes files on it; a picture tube is heard going dark, and
  a flat panel goes dark silently; a server or a rack unit slides into and out of its bay on its rails, and a rack that
  is broken drops them quietly; a CD or DVD drive is heard turning while a system or a program installs from its disc;
  and a running Vintage Mainframe's tape reels are heard turning.
- Sound: Frames 95, XP and 11 have their own sounds: a chime when the desktop comes up, the same chime when
  the system shuts down, and an error sound when it raises an error box. They come out of the computer's monitors and
  speakers, heard by everyone near them, and only through a sound card or the sound on the board: a machine with
  neither, or with no monitor and no speaker, only beeps. They are sound cues picked by the system a machine runs, so a
  resource pack can give any other system chimes of its own.
- Sound: The Unix desktops have their own sounds, taken from the sound themes they ship: GNOME and Cinnamon
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
- Sound: The minefield clicks as its cells are opened and flagged, goes off when a mine is opened and plays
  a jingle when it is cleared, out of the computer's monitors like the system's sounds.
- Sound: Each system keeps its own sound settings on its disk: how loud it plays, whether it is muted, and
  where its sound goes, out of the monitors, the speakers or both (both on a fresh system). A choice with nothing linked
  to play it gives way to what is linked, so a machine with somewhere to play is never silenced by it. On every system,
  `config volume 60`, `config mute on` and `config output speakers` change them and `config` lists them. They touch
  only what the system plays; a machine's own noises, its drives and fans, stay as they are.
- Sound: Every desktop turns its sound from the speaker on its panel, each in its own way: Frames 95 and XP
  open the small Volume popup with an upright slider and a Mute box; Frames 11 its quick settings, with an arrow that
  opens the choice of output; KDE Plasma its Audio Volume applet; GNOME its system menu, dropped from the top bar;
  Cinnamon its sound applet with a Mute output switch; and the period KDE and GNOME of a Legacy machine the mixer popup,
  whose Mixer button goes to the settings. The speaker wears a red cross while the system is muted, tells the volume
  when the pointer rests on it, turns it a step for each notch of the wheel, and opens a menu with Sound settings on
  the right button. CDE, which keeps no speaker on its panel, has an Audio page in its Style Manager: the volume on a
  scale, Mute, and the monitor and the speakers as toggles that may both be on.
- Sound: The Settings window's Sound page: the volume, Mute, the output, what plays the sound (the sound
  card by its name, the board's own sound or the speaker in the case), the speakers linked with the side each plays,
  and Test, which plays the system's startup sound the way the settings now have it. A speaker's screen says it is off
  while its computer plays only out of the monitor.
- Sound: A computer's sound hardware plays as many sounds at once as it has voices: 9 on an FM card, 32 on
  a wavetable card, 64 on a board's own sound, and one in the case. A stereo song takes two voices of a card that plays
  both sides and a mono one takes one, each note of a chord one and each chime of the system one. A sound that finds no
  voice free takes those of the sound that started first, which stops, and Soundfoundry says so when it was its song; a
  new beep cuts the one before it off. A paused song holds no voice.
- Σ and Σ#: Σ and Σ# have versions, one number for both: 1 is the language as 0.4.0a shipped it, and this
  release brings 2. The version lives in the compiler's package, sgsc 2.0 being Σ# 2, so a machine whose compiler is
  still 1.0 builds Σ# 1 until an upgrade brings it up (`pckmgr upgrade`, the upgrade verb of each Linux and BSD
  manager, or on MC-DOS installing the compiler again from newer media). A build can be held to an older version with
  `--lang N` on sgsc and scc, or with a `langversion: N` line in a project, and what came later is refused where it was
  written: "'Sound' needs Σ# 2; this project is Σ# 1". Both compilers say which they are before anything else ("Σ#
  Compiler 2.0"). The editors follow the machine's compiler, and Virtual Studio the project as well: the list of
  suggestions offers only what that version has, and the margins and the builds hold a file to it.
- Σ and Σ#: Virtual Studio's Project Properties has a Language version under the Platform target: Default,
  which follows the installed compiler and says which one it is ("sgsc 2.0"), and a button for each version of the
  language. A version's button writes `langversion: N` into the project and Default takes the line out again; under
  them, the error a lower version gives for what came after it, and the line the choice wrote.
- Σ and Σ#: Programs make sound. Σ# and Σ have `Sound.Beep`, a beep out of the speaker inside the case;
  `Sound.Tones`, a tune through the sound card, written as notes by name or pitch (`C4:250 E4 G4 C4+E4+G4:500`) with
  rests, chords and lengths; `Sound.Play` and `Sound.Stop`, a song from the machine's disks out of its monitors and
  speakers; and `Speaker.Named`, a linked speaker found by its name, which plays a song out of itself alone. A tune
  holding something that is no note stops the program, naming it. A machine with no sound card plays its tunes out of
  the case, one square note at a time. The documentation of the language has a section on it.
- Σ and Σ#: Σ 2 brings the old names the languages of those machines used, written with no type in front
  of them, for what the library already does: `puts`, `gets`, `exit`, `abs`, `sqrt`, `pow`, `floor`, `min`, `max`,
  `strlen`, `strstr`, `atof`, `itoa`, `sprintf`, `rand` and `srand`. Each is the library's own call written the short
  way (`puts(s)` is `Console.PrintLine(s)`, `strlen(s)` is `s.Length`, `rand()` is `Random.Next(32768)`), so a program
  compiles to the very listing the long way gives, and Σ# has them too. `sprintf` reads its format as `printf` does and
  gives the text back instead of printing it. A method or a variable of the program's own under one of those names is
  still the one called, so no program changes. Σ's `Standard` library gains `Random` in the same version, with `Next`
  and `Seed`. The editors offer the old names, each way it is written, priced as the call it stands for, and only to a
  project whose version has them; `computers/docs/SIGMA.md` lists them with the long way of each.
- Σ and Σ#: `printf` and `sprintf` in Σ 2 read the rest of what C wrote in a hole: flags, widths,
  precisions, `%u`, `%x`, `%X`, `%o`, `%e` and `%E`, and put the value in the hole the way C does (`%05d` of -42 is
  `-0042`, `%.2f` rounds the number the machine holds half to even). A number with a fraction and no precision is still
  written as the language writes it. A plain hole compiles to the same listing as before; one of these is written down
  as a single call that formats the value, and the first version refuses it for the version it needs.
- Σ and Σ#: Σ 2's small helpers: `strcmp`, `toupper`, `tolower`, `isdigit`, `isalpha`, `isspace`, `itoa(n,
  base)` for a base from 2 to 36, and `atoi`, which reads text that is not a number as 0. They stand for calls the
  library gained in the same version and that Σ# can write the long way: `string.Compare`, the `char` type's `ToUpper`,
  `ToLower`, `IsDigit`, `IsLetter` and `IsWhiteSpace`, `Convert.ToString(n, base)` and `Convert.ToInt(text, fallback)`.
  A member added to a type the first version had is refused there the way a later type is ("'string.Compare' needs Σ#
  2"), and the editors do not offer it to a project held to that version.
- Σ and Σ#: A program's Main in Σ 2 may be `static int Main()`, whose answer is the code the program ends
  with (what a parent reads from `Process.ExitCode`, unless the program ended itself with `Program.Exit`), and may take
  `string[] args`, handed the words the program was started with. The first version refuses both shapes for the version
  they need.
- Σ and Σ#: A program's console keeps an open line, the way the consoles of the old machines did:
  `Console.Print` and a `printf` with no line break leave the line open for what follows, `PrintLine` and a break end
  it, and it ends when the program does. A program that waits for a line asks with what it left open, which stands
  where the prompt was at a desktop's terminal and at a machine's own prompt, and the answer typed goes on the same line
  (`How many ingots? 16`). What is typed is written into the program's console as it is typed, so every terminal looking
  at it shows it once and a save keeps the whole exchange, the open line included. Σ 2's `putchar` prints one character
  on the open line, the `Console.Print(char)` it stands for.
- Σ and Σ#: Reads of less than a line in Σ 2: `getchar` takes the next character typed, the line break at
  its end included, and `scanf("%d", out n)` reads one value into the variable handed with out (`%d`, `%i` and `%u` a
  whole number, `%f` and `%e` a number, `%s` a word, `%c` a character), giving 1 when there was one and 0 when what was
  typed was not one; what is left of the line waits for the next read. They stand for `Console.Read()` and
  `Console.Scan(out value)`, which Σ# can write the long way. Text that is not the value asked for lets its line go
  rather than stopping every read after it.
- Σ and Σ#: Files as C's stdio works them, in Σ 2 and Σ# 2: `FILE f = fopen(path, mode)` with C's six
  modes, `fclose`, `fgets(out line, f)`, `fputs`, `fprintf`, `fscanf` of one value, `fgetc`, `fputc`, `feof`, `rewind`,
  `fseek` and `ftell` counted in characters, `remove` and `rename`, on the machine's own files and the same terms as
  `File.*`. A file opened is one of the program's own objects, kept with it in a save; what it writes reaches the disk
  when the file is closed or when the program ends with it open. They stand for `File.Open`, `File.Move` and the new
  `FILE` type's `Close`, `ReadLine`, `Read`, `Scan`, `Write`, `Seek`, `Position` and `AtEnd`. A program holds only so
  many files open at once, by its machine's era: 8 on a Vintage machine, 20 on a Legacy one and 64 from the Transition
  on, after which `fopen` gives `null`, as C's does when a table of open files is full. The count is of the files the
  program holds, so it is the same after a save and a load.
- Σ and Σ#: `strcpy(out dest, src)` and `strcat(ref dest, src)` in Σ 2, in C's order, compile to the
  assignment and the joining written by hand (`dest = src`, `dest = dest + src`), to the same listing. `ref` is written
  for `strcat` alone; a variable called `ref` is still a variable.
- Σ and Σ#: The widgets of Σ# 2 for programs' windows: `TextArea`, `NumberBox`, `Slider`, `RadioGroup`,
  `ComboBox`, `TabView`, `GroupBox`, `ScrollView`, `Table`, `TreeView`, `MenuBar`, `ContextMenu`, `StatusBar`, `Image`,
  `Chart`, `LogView`, the system's own `OpenFileDialog` and `SaveFileDialog`, and the computers' own `ItemSlot`,
  `ItemPicker` and `OperationView`. Each system draws them in its own look, Frames 95, XP and 11, KDE 2 and Plasma,
  GNOME 1 and GNOME, Cinnamon and CDE: a radio button is round or a diamond, a tree opens with a boxed plus or a
  chevron, a slider's thumb is a raised block or a dot of the accent. Each is saved with its program like the widgets
  before it.
- Σ and Σ#: A Σ# program on a machine that only has its terminal, as the Vintage systems do, MC-DOS among
  them, opens its windows there in letters, as the full-screen programs of the age drew their dialogs, instead of
  stopping for want of a desktop: the machine's ground in blue, a menu bar along the top, the window in a double frame
  with its shadow, the status line along the bottom. Tab moves between the widgets, the arrows move inside one, Enter or
  Space presses, Escape shuts what is open, F10 opens the menu bar, and a click lands where it is. A picture, a chart,
  an item, an operation, a canvas or a component that draws itself has no letters to be drawn in, and a program making
  one there stops, saying so; a machine with neither a desktop nor a terminal still stops it, saying so.
- Σ and Σ#: `GenericComponent`, a component of a kind another mod adds: it holds any value of the language,
  copied when it is handed over and charged by its size (a draw and the price of every started 4 KB, in the editor's
  tooltip and when it runs alike), and tells the program what a player did as a `ComponentAction`, a name and a value,
  through `OnAction`. What it holds is checked against the bounds every component shares (no handlers, files, threads,
  windows or widgets, nothing that holds itself, at most 32 KB, sixteen levels deep and 4096 parts) and against what
  its kind takes. A game without the mod shows a placeholder naming the kind. A kind that reaches outside the game is
  off unless the server's settings and the player's own both turn it on (Outside components, in each).
- Σ and Σ#: A program a mod writes in Java can open a window of its own on a desktop, drawing itself or a
  surface of pixels or of the graphics card, for whoever opened it; another player at the machine and the monitor's
  face in the world see a placeholder in its window, and one whose drawing fails shows the placeholder from then on. A
  window with a surface holds video memory on the machine, as a paint program's does.
- Manuals: The Guide to Operations, J's Computers' own manual: a beige binder with a cyan band, in the
  mod's tab among the programs, whose bar lights blue blocks one after another as a desktop of the 2000s did while it
  loaded. It is a guide written to be read, in thirteen chapters, from starting a first computer to the achievements
  and the other books: what a computer is in the mod, a first computer built part by part, the firmware with pictures
  of the real BIOS and UEFI screens of each age, the parts inside a computer, the computers, their systems and
  programs, the network, Operations and their catalogue, storage, autocrafting and the devices, each idea before the
  parts that serve it, and what a first network needs (a Mainframe runs nothing until a system such as MC-NET is
  installed on it, so it needs a disk and a graphics card too).
- Manuals: Six books beside the Guide to Operations, each an item among the programs of the J's Computers
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
- Manuals: Twenty-one scenes in the Guide to Operations, each beside the page it shows, with numbered
  balloons: the first computer and its monitor, the front of a case and a case open with every part in its seat, a
  running Mainframe with its service panel off, a Server Rack, a supercomputer, a datacenter, a Cluster Management
  Computer and its racks, monitors joined into one screen, devices on a hub, the Dock Station, a first network, routers
  and fibre, failover, storage in a rack, buses at a chest and a furnace, the whole autocrafting line, the Pattern
  Encoder, an Input Router and a Receiving Bus, a Redstone Interface lighting a lamp, and the Network Gateway beside a
  ComputerCraft computer.
- Manuals: The manuals inside the computers, beside the manual pages of each machine's commands, in each
  system's own help. Every desktop has Help: Frames 95's Help Topics with its Contents, Index and Find and a topic
  window of its own, XP's Help and Support Center, 7's Help and Support, Get Help on 10 and 11, KDE's Help Center, the
  Help of GNOME and Cinnamon, and CDE's Help Viewer, each with the books of the Technical Reference, the Guide to
  Operations and the Plant Drawings, a search over the entries and the commands, links between pages, and Back. At a
  prompt, `help` on MC-DOS and MC-NET takes the whole screen in the sixteen colours, with its contents, index, links
  and Find; `info` on a Linux distribution reads every chapter, section and entry as a node with its menu; and `man` on
  UNIX and FreeBSD pages an entry as a page of section 7 (`man graphics-cards`). A system's help opens first on the
  book written for that system, whose top page lists the other manuals after its chapters, so a Help with no tree of
  its books, as GNOME's and CDE's, still reaches every one.
- Manuals: The computers' help programs read the manuals' new parts: a table of several columns as columns
  of text, set out row by row on a screen too narrow for it; listings and sessions as their lines; keys by their names;
  scenes as their captions and the list of their balloons. A command whose page a manual prints shows that page at
  `man`, at `HELP` on MC-DOS and in each desktop's Help.
- Manuals: Pages for the commands in the manuals, made from each command itself: what it is for, how it is
  typed, what it does, a table of its options and one of its examples, every system it runs on (with the package to
  install first, where one is needed, said once when every system needs the same one) and the commands to read next,
  each leading to its own page. They say what `man`, `help` and `/?` say at a prompt, word for word, so the book and the
  machine never disagree.
- Modpacks: With KubeJS installed, a modpack's scripts reach J's Computers as `JsComputers`:
  `JsComputers.installer('frames_95')` and `JsComputers.programInstaller(...)` give an installer with its software
  written on it, which no plain item id can name, `JsComputers.readyDisk(...)` a disk with a system already on it, and
  `JsComputers.systems('legacy')`, `JsComputers.programs(...)` and `JsComputers.eras()` list what there is. With the
  item tags below, a modpack can give J's Computers recipes and chest loot of its own. KubeJS stays optional: without
  it, nothing of it loads. The page Recipes and loot (`computers/docs/MODPACKS.md`) shows how, with a datapack or with
  KubeJS, and the Guide's first page and its page on the books say a modpack can give the mod recipes, which the recipe
  viewer then shows.
- Modpacks: Item tags for J's Computers, so a datapack or a script can ask for a whole group of its things:
  `#jsc:era/vintage`, `#jsc:era/legacy`, `#jsc:era/transition`, `#jsc:era/standard` and `#jsc:era/advanced` hold
  everything of an era, and one tag per shelf of the creative tabs holds that shelf in every era:
  `#jsc:personal_computers`, `#jsc:crafting_computers`, `#jsc:cluster_management_computers`, `#jsc:mainframes`,
  `#jsc:racks_and_servers`, `#jsc:rack_bays`, `#jsc:peripherals`, `#jsc:network`, `#jsc:components` and
  `#jsc:programs`. J's Core writes them from the tabs, which a mod's tab or shelf asks for when it is declared.
- Creative tabs: J's Computers shows its things in six creative tabs: one for what every era shares, then
  one for each era from the Vintage to the Advanced, side by side in that order, with the era's own icon (a floppy, a
  CD, a GeForce 8800 GT, a Haswell board and the RTX 5090). Each tab has the same shelves: Personal Computers, Crafting
  Computers, Cluster Management Computers, Mainframes, the server rack with its servers, the rack's bays, the
  peripherals, the network, the components, and the programs (blank media, then the installer of every system and
  program of that era). A thing is in the tab of the era it carries (a machine's chassis, a part's specification, a
  cable's generation, a medium's format), and in one tab only.
- Advancements: Sixty-six advancements in four tabs. Hardware follows the machines, from the first
  self-test and the eras a computer can be built in up to racks, datacenters, clusters and supercomputers; the Time
  Traveller challenge asks for a working computer of every era, from the Vintage to the Advanced. Operating Systems has
  one for the first install of each system, the prompts and desktops, and challenges for installing every distribution
  and running screenfetch on every system that takes it; the three advancements of the old Computers tab moved into
  it, so a world that had earned them shows them unearned there. Networks & Operations covers joining a Mainframe,
  Operations, IQL, autocrafting and the ways a network goes wrong, and Sigma goes from installing the compiler to a
  program that stays up for a week. Some stay hidden until earned, and the one for hearing a ComputerCraft computer
  through a Network Gateway exists only when ComputerCraft is installed.
- Advancements: An advancement goes to the player who caused it. What a machine does on its own, such as a
  self-test ending, a job firing or an Operation finishing, goes to whoever works that machine: the last player to place
  it, open its screen or build it, which the machine keeps across restarts. An Operation typed at a terminal goes to
  the player who typed it, however long it takes to finish and across a restart of the world. What a player earns while
  they are away is given to them the next time they join.
- Documentation: J's Computers' documentation, in its `docs` folder, written for a reader who knows nothing
  of the mod yet, with what can go wrong at the end of every page: to players their first computer and network, the
  hardware and what every number on a part means, the systems and how to install them, the network and IQL, and
  autocrafting; and to addon authors everything a computer is made of, with the code that adds each. The cables are
  explained in `computers/docs/CABLES.md`: the shared cable block and its colours, each data line with its cables,
  speeds and ranges by era, the routers and repeaters, the buses and crafting parts, and the peripheral cables. The
  pages on cables, Σ#, ComputerCraft and music moved to J's Computers' folder.
- Design: The design of J's Computers, in `computers/docs/design`: one page per subject, from the eras and
  the hardware to the network, Operations, the systems, Σ#, sound and the API, each saying how its system works and
  ending with what is designed and still to be built, gathered on the first page; with the formulas in one place and a
  catalogue of every part of every era with its figures. It follows J's Industrial's design where the two meet: how
  hardware is made with and without J's Industrial, the energy computers draw, the cable systems, storage for every
  state of matter, research groups in the Cluster Manager, and recipes that stay visible before they are researched;
  and J's Space's: the computers J's Space adds, its telemetry line, the Enigmatic hardware and networks across worlds.
  The series' README now says plainly that every mod of the series depends only on J's Core and never on another mod of
  the series.
- Translation: Brazilian Portuguese (pt_br) for J's Core, J's Computers and J's
  Industrial: every word the mods show, from the blocks and items to the desktops, the programs, the terminals and the
  installers. The names of the fictional products and makers, the commands a player types and the files on a virtual
  disk stay as they are.
- Logos: Logos and icons for the series and for each mod. The mods list shows each mod's
  logo over the series logo, mod list screens that show an icon show each mod's own, and the READMEs carry the logos.

### Changed
- Operating systems: Every edition of Frames goes down when a program it has no memory left for is opened,
  a window, a program started at its prompt or a service, instead of refusing it: Frames 95 at once, and Frames XP, 7,
  10 and 11 after a balloon that warns, once the memory passes nine tenths, naming the programs that hold the most. The
  crash brings the machine itself down: its blue page, in the style of each edition's real one, is on the monitor for
  everyone looking at it, in the world too, and nothing typed or clicked at it reaches anything. The programs the
  machine was running die with it, its session ends, and once the page has stood its time the machine starts over from
  its self-test into an empty session. Frames 95's crash was a picture on the player's own desktop only: the keyboard
  still reached the program behind it, and a player who left the monitor during it came back to every window open
  before the crash. Linux, FreeBSD and UNIX have their own crash pages too, their kernels' panics on the console.
- Operating systems: Linux, FreeBSD and UNIX no longer refuse a program they have no memory left for: as the
  real kernels do, they let it start and then end what holds the most, a window, a program or a service, until
  everything fits. Each one is logged in the kernel's own words (`Out of memory: Killed process 1234 (calc)` on Linux,
  `pid 1234 (calc), jid 0, uid 0, was killed: out of swap space` on FreeBSD) and printed on the console, and the new
  `dmesg` lists those lines after what the kernel found as it came up. A desktop closes the window that went and says
  why in a balloon; a program ended in front of a terminal leaves the shell's `Killed` and gives the prompt back; a
  service ended stays down until the machine starts over, a Mainframe's network engine, Automation Engine and Mirror
  included. Only when nothing is left to end does the kernel panic, on its own page, and the machine starts over: a
  machine whose system and desktop alone hold more than it has panics as soon as it is up.
- Operating systems: What a program holds in memory grows with the era of the machine it runs on, as
  software grew with the machines it was written for: each program holds what it declares on a Legacy machine, a
  sixteenth of that on a Vintage one, eight times on a Transition, thirty-two on a Standard and a hundred and
  twenty-eight on an Advanced, the tools a system ships included (16 MB on a Legacy machine). Each edition of Frames
  holds more of itself, Frames 95 32 MB, XP 128 MB, 7 1 GB, 10 3 GB and 11 8 GB, so the typical machine of each era,
  two sticks of its middle size, holds its system and about eight programs, and filling the board pays. The Linux
  distributions, FreeBSD and UNIX stay light, and a Linux desktop holds a little more than one program: KDE Plasma 80 MB
  on a Legacy machine, GNOME 72, Cinnamon 56 and CDE 8.
- Operating systems: Frames 11 installs from the Advanced era on, and the Standard era has Frames 10. The
  family now runs Frames 95, XP, 7, 10 and 11, and a program asking for a newer Frames reads it in that order. "More
  like, Bloat 11" follows "What Happened to Frames 9?" in the Operating Systems tab, as "Never Upgrading" follows "The
  goat".
- Operating systems: Frames 11 calls its file manager File Explorer, as Frames 10 does, rather than Files.
- Operating systems: FreeBSD's prompt is root's, as a machine fresh from its installer stands at:
  `root@host:~ #`, with `root@host` in red and the rest in the prompt's own colour.
- Operating systems: `apt upgrade`, `dnf update` and `upgrade`, `pacman -Syu`, `emerge --update @world` and
  `pkg upgrade` bring the installed packages up to the versions the Mirror serves, as `pckmgr upgrade` does; `apt
  update`, `pacman -Sy`, `emerge --sync` and `pkg update` still only read the lists.
- Operating systems: A manual page's paragraphs wrap to the width of whatever shows them, at a terminal,
  under `--help` and `/?`, and in the Help Viewer, with the lines they wrap onto set in under the first, instead of
  breaking at fixed places. The Help Viewer files every command under what the command says it is for, so each command
  sits under the same heading wherever it is listed.
- Terminals: The terminals write in the game's font, one character to a cell, and a monitor's console draws
  it at the machine's display scale, the same scale its desktop is drawn at.
- Terminals: A desktop's terminal window opens eighty columns by twenty-four rows, as terminals do, and
  smaller only when the desktop has no room for that. Its rows, and those of the terminal inside an editor, are as far
  apart as the full-screen terminal's, so what is drawn in characters keeps its shape instead of being squashed flat.
- Desktops: Save in the Editor, or Ctrl+S, on a page never saved opens Save As to ask where and under what
  name, rather than writing untitled.txt without a word.
- Desktops: The explorer's rows, the file dialog's and those of the file trees in Virtual Studio, Virtual
  Studio Code and Exposure are tall enough for a 16-pixel icon, as a list of small icons on a real desktop is. The file
  dialog opens taller to make room for them.
- Desktops: What moves while a machine comes up and goes down (Frames 95's running bar, Frames XP's blocks,
  the rings and running dots of Frames 11, GNOME and Cinnamon) moves smoothly between ticks, on the clock every motion
  is read against, and stands still in its first position for a player who reduced motion, Frames XP's blocks standing
  at the left end of their trough rather than outside it.
- Desktops: A desktop's windows are known by the program they belong to rather than by the name they show,
  so a machine's layout comes back the same whatever desktop or language it is looked at under, and the Task Manager
  and the System Monitor list each window by the name the desktop gives its program.
- Desktops: The desktop's code is split into parts by what each one does, apart from the screen that shows
  them: where it sits and how big it draws, how its owner chose it should look, what it tells the player, the power
  dialog, the machine's memory as it weighs it, its menus, the inventory laid over a window, what it can start, its
  launcher and panel, the window manager, the layout the machine remembers, how it opens a program or a file, what the
  pointer drags, where a click, a key or the wheel goes, and how it is painted. The desktop draws onto any surface that
  gives it a size, so it can be drawn with no screen open.
- Σ and Σ#: A box typed into tells its program's handler at most once a tick, holding every letter typed by
  then, so a player typing fast no longer fills the program's queue with a call a key. A window carries at most 128 KB
  to the screens showing it, whatever its program holds; past that, what its last widgets hold is left out and they say
  so.
- Σ and Σ#: A widget asking for a width or a height gets it down to 8 pixels; anything under 60 used to be
  raised to 60, the least a window may be, so a short widget pushed what came after it out of its window.
- Σ and Σ#: try, catch, finally, throw, goto and const, and comefrom, abstainfrom and reinstate after
  INTERCAL, are kept for a later version of Σ and Σ#: written as a statement or as a name, each is refused at the word
  with what to write until then (S2014), where it failed with a message about something else.
- Σ and Σ#: The words of access hold in Σ and Σ# as in C#: a private member, which is what a member with
  no word is, is reached only from its own type and the types nested in it, a protected one also from the types built
  on it, and reaching one from anywhere else is the error S3060. They were read and ignored. internal is taken as public
  is. The Book of Σ and the description of Σ# say so, and how shifts work.
- Σ and Σ#: The instruction set of the Vintage processors is called IA-16 (it was x86-16): a project that
  names `jsc:x86_16` names `jsc:ia_16`. What a processor understands (IA-16, x86, x86-64) is its instruction set: the
  firmware lists it as the Instruction Set, a build of mixed processors is refused for mixing instruction sets, and
  `sgsc --arch` asks for an instruction set by the same ids and names as before. The word architecture is left for the
  design of a chip.
- Hardware: How much a processor orchestrates counts the design of its cores as well as how many there are
  and how fast they run: a newer design does more in each tick of its clock, and two threads a core add a fifth. An old
  chip at a high clock no longer outruns a better one that came after it. Vintage machines orchestrate less than before
  (a 486DX2 one item a tick, a K6-II 11) and Standard ones up to three times more (a 4790K 2,074), and no processor does
  less than one item a tick. A Crafting Computer that does less than one run a tick keeps what a tick's work leaves over
  for the next, so a run takes as many ticks as it costs.
- Hardware: A server's RAID controller and read cache are expansion cards now, not gadgets in the rack's
  front bays: Vaultis makes an Array and a Cache for each era's slot, from ISA to PCI-e 4.0. The Array joins the drives
  of the server it sits in and keeps the array's mode on itself, so a server pulled from its rack takes its array with
  it; the Cache cuts the server's reads by 10% on the ISA card, 15% on the PCI one, 20%, 25% and 30% on the PCI-e 2.0,
  3.0 and 4.0 ones. In any other computer a storage card sits in its slot and does nothing. The front bays that took
  gadgets take drives: a 1U server has 1 drive bay in the Vintage, 3 in the Legacy, 5 in the Transition, 4 in the
  Standard and 5 in the Advanced; the Storage Server 10, the Compute Server 2, the Supercomputer Node 2 and the Advanced
  one 3.
- Hardware: The Legacy 1U server takes two expansion cards rather than one: its board has no video of its
  own, so one slot goes to the graphics card a monitor needs and the other is free for a storage card.
- Hardware: A disk goes in a computer, and in a Server Rack's drive bay, of its era or a newer one: an NVMe
  made for the Standard age no longer fits a Vintage computer, while an old disk still goes in a newer machine. A disk
  already in a computer stays.
- Hardware: A part's tooltip names its slot and its memory as the parts' own sheets do: PCI-e 3.0, AGP 8x,
  DDR3. A medium's tooltip names its format in the player's language ("Floppy", "Blu-ray") instead of the code's name
  for it.
- Hardware: A compute card is counted at the clock it is rated at, the one a card with no fan of its own
  holds in a server: the Tessera K40 does as much as the Vertex GTX 780 Ti on the same chip. The Integra Phi 9000 has a
  new icon.
- Hardware: The Vintage server boards hold the Integra Pentix Pro, as the boards of the time did: the MF
  MTX-V of the Mainframe takes four on Socket 8 and the MF EEB-V two. The Socket 7 chips stay on the desktop boards.
- Hardware: Vintage names, fixed to their years: the MF AT Standard Motherboard is the MF AT Classic, the MF
  PowerBasic 300B is the MF PowerBasic 300 (its id follows, `jsc:psu_300`; the Bronze seal came a decade later), and
  the graphics cards carry their makers of the time: the Artisan 3D Blaster, the Envya Prism 4 and the Tridex Voodoo
  GFX, while the VGA-256 is IBM's own and carries none. Every Vintage part has a new icon drawn from the real one.
- Hardware: Legacy parts that never existed as named give way to real ones, and their ids follow: the
  Velocion Sprint XP 3800+ is the Sprint XP 2800+ (`jsc:cpu_velocion_sprint_xp_2800`), the dual-core Velocion Dual
  240, 280 and 285 are the single-core Velocion Optera 244, 248 and 250 of their years, and the two-way LGA 775 server
  board is the MF EATX Legacy Motherboard (2x Socket 604). The Integra Vertex processors are the Integra Pentix, the
  name of their line (`jsc:cpu_integra_pentix_700` and on), which leaves Vertex to the graphics cards.
- Hardware: Legacy boards take the memory and the bus their chipsets had: the Socket 370 board SDRAM on AGP
  4x, the Socket A board SDRAM and DDR, and the Socket 940 server and Mainframe boards DDR. The Radiance 9200 SE is an
  AGP 8x card with four pipelines and 128 MB, and the Legacy graphics cards carry the makers of their time, Envya and
  Atrion. Every Legacy part has a new icon drawn from the real one.
- Hardware: Parts move to the era of their years. From the Legacy to the Transition: the Integra Duo
  processors, now the Integra Centro 2 Duo (`jsc:cpu_integra_centro_2_duo_e4300` and on), the Servo 5100 and 5160, the
  Servo 5365, now the Servo 5335 it really was, the DDR2-2048, the Vertex 8800 GT and GTX 280 on PCIe 2.0 (the GTX 280
  draws 236 W) and the SATA-SSD 64G, which holds 256 items at the Transition's weight. From the Standard to the
  Transition: the Velocion Ascent X4 955, X4 965 and X6 1090T (`jsc:cpu_velocion_ascent_x4_965` and on), the AM3 board,
  now the MF ATX Transition Motherboard (AM3) on PCIe 2.0, the Radiance HD 6850, a Velocion card of 1 GB, the hard
  disks up to 2T, and the DVD. The disks read as their catalogue writes them, 500G to 8T, and their ids follow
  (`jsc:disk_hdd_500g` and on). A Personal Computer from the Transition on takes the workstation's EATX board as well as
  an ATX one.
- Hardware: Standard names follow the catalogue, and their ids follow them: the Integra Apex processors are
  the Integra Centro c5 and c7 (`jsc:cpu_integra_centro_c7_4790k` and on); the Servos are `jsc:cpu_integra_servo_2620`
  and `jsc:cpu_integra_servo_2690`, and the Servo 2699, a chip of the next socket and of DDR4, gives way to the Servo
  2697 v2 (`jsc:cpu_integra_servo_2697_v2`); the Radiance HD 7970 is a Velocion card (`jsc:gpu_radiance_hd_7970`) and
  the Vertex GTX 550 Ti and 780 Ti are Envya's; the memory is the Stratix Layer DDR3-8192; the MTX-P Motherboard and
  EEB-P Server Board are the MF MTX-S Motherboard (4x LGA 2011) (`jsc:motherboard_mtx_s_2011`) and the MF EEB-S Server
  Board (2x LGA 2011) (`jsc:motherboard_eeb_s_2011`); and the MF EATX Standard Workstation Board is the MF EATX
  Standard Motherboard (LGA 2011) (`jsc:motherboard_eatx_standard_2011`). Every Standard part has a new icon drawn from
  the real one.
- Hardware: The Standard server boards have the memory slots of their kind: 16 on the two-way board and 48
  on the Mainframe's four-way one, where a machine still counts only as many as its case holds; a rack server reports
  the slots its case has rather than the board's.
- Hardware: The Vaultis Swift SSD 8T and the Bolt NVMe 4T and 8T move to the Advanced, the years they sold
  in, with new icons.
- Computers: The assembly screens are drawn as the machine they assemble. On the left, the open case of its
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
- Computers: A computer finds its removable drives in one place, and everything that asks for them gets
  the same answer in the same order: the drive letters, This PC, the explorers, the firmware's boot menu and
  installer, the Setup of a program, the Cluster Management Computer, the Crafting Manager, the Pattern Studio and the
  live media. With several drives holding installers, each of them now takes the first in the order the drive letters
  go, where some took the first one cabled. A system being copied from a drive stops if that drive is turned off for
  the computer.
- Computers: A Standard computer with nothing to boot shows its dialog over the maker's mark and the
  machine's name, and the self-test's bar is gone, where it showed below a short dialog and hid under a tall one.
- Computers: J's Computers makes an installer, a blank medium with a system or a program written on it, in
  one place, which the creative tabs share with anything else that hands one out.
- Racks and servers: A server rack's screen shows the cabinet from the front: its posts, each mounted unit
  as its faceplate with its own slot, its drive caddies, vents over what its chassis does not cable and, on a computer,
  its power button with its lamp; an empty unit is a blank panel, and a KVM Switch, a UPS, a cooling unit or a fibre
  interface is a unit too. A drive caddy's lamp is lit while its drive sits in a switched-on unit, and an array being
  rebuilt fills a bar across its unit. A click on a computer's faceplate puts it on the card beside the cabinet: its
  name, its state, the disk it boots from, its system, its network, its array and what a linked monitor shows. A front
  slot still holding a drive after its unit came out shows, so the drive can be taken back, and says why it takes
  nothing more.
- Racks and servers: A server in a rack follows the rule of its system when it runs short, as every
  computer does, each server on its own: a Frames server goes down on its blue page and starts over, the kernel of a
  Linux, FreeBSD or UNIX server ends what holds the most and logs it for `dmesg`, and MC-NET refuses as it always did.
  At a server's prompt `ps` lists its init and its services, `kill -9 1` panics it, and a signal to a service stops it
  until the server starts over; a service ended on a server, by a signal or for its memory, serves the network no more
  until then. The page is on the monitor showing that server, and a key holds FreeBSD's reboot there as on any
  computer. A server used only to refuse what did not fit, and nothing ended it.
- Racks and servers: A Server Rack's roof fans turn as fans: the housing stays still and only the rotor
  turns in it.
- Network: The Server Router wears the look of the other network devices: a bank of four ports on every
  face, framed in router blue, and the routers' two blinking lamps.
- Network: J's Computers' four data cables (Ethernet, HBW, High Compute and Crafting) are laid in the Core's
  shared cable block, each in the lane of its line: access top left, backbone top middle, compute in the middle and
  crafting middle right. They are items now rather than blocks of their own, drawn four pixels thick in their new
  jackets with the plug of their kind where they meet a device, and they can share a block. A world's old cable blocks
  are gone from it.
- Mainframes: The Standard Mainframe's roof fans look like fans: each is a square housing that stays still,
  with a dark well in its opening and a five-bladed rotor turning in it, where before the whole square turned with a
  cross painted on it.
- Mainframes: The Vintage Mainframe's tape reels can be seen: the window in front of them is a clear pane,
  where a handle stood over one reel and a painted glint over the rest, and the reels are aluminium reels with three
  windows and the tape showing through them. They run in bursts, as a tape drive reads: forward, a stop, a short rewind,
  a stop, forward again, the take-up reel a little faster.
- Peripherals: The Pattern Encoders are the devices of their day, each a full block: the Vintage one a beige
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
- Peripherals: The Floppy, CD and DVD Drives are the drives of their day, each a full block in the colours
  of the Pattern Encoder of its era: the Floppy Drive a cream external drive with its bezel low on the front, the CD
  Drive a pale grey external CD-ROM drive with darker end caps, the tray at the top and the headphone jack, the volume
  slider and the eject button under it, and the DVD Drive a black writer with a diamond-cut top and a silver trim. The
  medium in a drive is the very item the player put in: a floppy slides in and out of the slot, and a disc lies on the
  tray, which the eject button opens and closes. A disc goes on the tray and comes off it only while the tray is out, a
  computer sees the drive empty while it is, and the tray stays shut while the computer reads the disc. Ejecting a disc
  from This PC or the Pattern Studio opens the tray, and the disc waits on it for the player's hand. The power lamp is
  lit while a computer is linked, and the activity lamp blinks while the computer reads the drive.
- Peripherals: Every disc tray, on a drive of its own, in a case's bay or on a Pattern Encoder, is moulded:
  the well a disc lies in, the smaller well inside it, the slot to the back the laser reads through and the spindle's
  hub, where it was a plain slab. An open tray is aimed at and outlined on its own, out of the block, and pushed with an
  empty hand it slides back in, as a real one does: from above, the tray stands in front of its button.
- Peripherals: The drives and the Pattern Encoders take their cable in the middle of their back, where a
  cable comes up to them, instead of low down, and the port there is their era's: a DB-25 on the Vintage ones, a USB
  port on the Legacy ones, a blue USB 3 port on the Standard ones.
- Monitors: A monitor's own sessions (the self-test, the boot menu, the firmware setup, the installers and
  the KVM) take from a player only what the session they have open sends, and only for the monitor it is on; a system's
  settings asking for the firmware setup must be on the monitor they name.
- IQL: Every table of the network's language reads every column it has: a WHERE tests any of them, and an
  ORDER BY sorts by any of them.
- Autocrafting: A machine recipe no longer names a machine: it lives in the Crafting Interface of the
  machine that makes it, so the Pattern Studio's machine draft has only its inputs, its outputs, their chances and its
  timeout, and loading one puts it in the first interface the Crafting Computer drives with room for it. A bench recipe
  goes into the ROM of one of the computer's Crafting Cards. The Crafting Manager lists the interfaces the computer
  drives, what each holds and what it is doing, and moves a pattern from one to another.
- Autocrafting: The crafting catalogue is planned once and kept while the network's patterns and storage
  are as they were, for a second at most, where it planned every recipe again on each refresh.
- Peripherals: An unlinked peripheral looks for a computer twice a second rather than every tick; a linked
  one still sees a cut cable at once.
- Saves: J's Computers' blocks keep their state as J's Core's declared fields: the computers, the server
  racks, the drives, the Pattern Encoders, monitors, speakers, Network Gateways, data cables, Server Routers, HBW
  Interfaces and tanks. The players who see one are sent one update a tick however much of it changed.
- Menus: J's Computers' menus are J's Core's declared menus: the computers', the devices', the buses', the
  terminal's, the Command Prompt's and the monitors' sessions. Each stays open only while its block stands and the
  player can reach it, for every variant of the block, and every screen draws its slots and answers its buttons from
  the one layout its menu places them by. What a client sends from one of them acts on the menu the player has open,
  never on a block the message names that the player is not looking at.
- Advancements: J's Computers' advancements are earned through J's Core's event trigger; what a world's
  players earned before stays earned.
- Translation: What the machines say is text a language file can translate, the way a real system's
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
- Translation: Every screen is text a language file can translate: the hardware's tooltips, the names of
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
- Storage: The Tank belongs to J's Industrial now (`jsindustrial:tank`), on its own Storage
  shelf and in its manual. A Tank placed under the old name is gone from a world made before. J's Computers' manual
  explains fluids on a page of their own, with a tank of any mod.
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

### Removed
- Racks and servers: The RAID Controller and the Cache Card, which went in a rack's gadget bays: the Vaultis
  storage cards take their place in the server itself.
- Autocrafting: The Crafting Switch and the Crafting Input Bus. The Crafting Interface holds a machine's
  recipes and feeds it, and the Crafting Input Router puts each input in through its face.
- Autocrafting: The machine picker of the Pattern Studio, and with it, in J's Core, a block's list of
  the recipe types it is the machine for and the data files that gathered them: a machine recipe belongs to the
  interface of its machine.
- Operations engines: The script a Mainframe kept for the Network Management Studio's editor: a studio's
  scripts are files on its computer's disk.
- Manuals: The unused hook for the Modonomicon guidebook: the series' manuals are drawn by J's Core
  itself.

### Fixed
- Installers: Arch's pacstrap and Gentoo's tar count the room on the root partition before they lay the
  base system out: on a root too small for it pacman says the partition is too full and installs nothing, and tar runs
  out of space. Both laid the whole system out on any partition, however small.
- Installers: Arch's and Gentoo's live installs take the firmware's partition mounted at /boot/efi, besides
  /boot and /efi: mount refused that place, though grub-install looks there when it is not told where.
- Installers: A machine that already had a name takes the one typed in a system's installer: the old name
  came back as soon as the disk's console was read again, so the machine went on answering to it after the install.
- Installers: A guided installer's progress follows the disk the system goes on. Picking another disk, or
  erasing one to install over it, timed the copy for that disk but left the steps on the page, and when they unlock,
  sharing out the time of the first disk the installer had suggested.
- Installers: Choosing another disk in a system's installer after agreeing to erase one takes the erase
  back; the copy erased the disk first agreed to while installing onto the other.
- Installers: A system's copy that fails with a long reason shows as much of it as fits above the buttons,
  the last line ending in dots, where it ran on under them.
- Installers: A Linux installed by hand is only offered software its hardware era runs, as every other way
  of installing already was; a Vintage machine could be given a desktop that needs Legacy hardware while it was being
  built.
- Installers: An operating system installs only when the disk has room for it beside the systems already
  there, a disk refuses a seventeenth system instead of dropping it, and the DOS boot screen shows the real free space.
- Installers: A system's setup is saved while it runs, so a reload in the middle of it goes on from where it
  was; formatting the disk it runs from stops it.
- Installers: `fdisk` keeps the partition number and first sector typed, a partition that takes the rest of
  the disk reserves its room, and `mount -t` and `-o` are read right; a huge `MAKEOPTS` no longer stops every build
  step.
- Installers: `fdisk` answers a size past any disk with "Value out of range." A long run of digits threw out
  of the tool, and was played again on every load of the world; a size in terabytes went round to below nothing.
- Installers: The files of an install by hand hold 131,072 characters between them, and a line that adds
  past that answers `No space left on device`; a line such as `cat f >> f`, run again and again, grew the machine's save
  without end.
- Installers: The setup on an installer medium, opened from the explorer, runs for a drive west or north of
  the world's origin; it did nothing there.
- Installers: The steps pacman counts through while it installs are no longer cut short at a fixed width.
- Operating systems: A program that only installs on a Crafting Computer is refused on any other machine
  however it is asked for: Gentoo's emerge built the Crafting Manager on a Personal Computer, where pacman, apt and pkg
  refused it.
- Operating systems: A program that shows itself only in windows, typed at the prompt of a Linux or a
  FreeBSD with no desktop, says it cannot open a display, as on a real one: it was a command not found, though it was
  installed.
- Operating systems: A restart closes every program a computer was running, as on any machine. The
  programs a player started at a prompt kept running through a reboot, as if the machine had never stopped; only
  switching it off ended them.
- Operating systems: Shutting a computer down from its desktop shows the system's own goodbye, and the
  machine goes dark only when it has finished; it used to cut the power at once, dropping the player out of the
  computer with no screen at all. Restarting from the desktop shows the goodbye too before the self-test, as restarting
  from a prompt already did.
- Operating systems: The help command's manual page gives first a line that works on every system that has
  help, help on help itself; its only example was help dir, a command of the DOS family alone, which answered with an
  error on every Unix.
- Operating systems: Settings, the System Monitor and the Network Manager call a machine's system by its
  name. A Linux, FreeBSD or UNIX system was shown by its id, `ubuntu` rather than Ubuntu, and the System Monitor showed
  every system that way, the Frames editions included.
- Operating systems: Every program says what it does on its install disc, in the package manager and in the
  installed-programs list. CDE, Cluster Manager, Emacs, Exposure, Gateway Manager, Help Viewer, Vim, Virtual Studio,
  Virtual Studio Code and Workstation Info had nothing to say there.
- Operating systems: screenfetch draws every logo as neofetch draws it. Ubuntu's was another, older logo
  copied with mistakes, and Debian's had a stray quote. Each was printed in one colour, readout included; now the art
  is in its own colours (Ubuntu red and white, Debian white with a red centre, Gentoo magenta and white) and the user,
  host and labels in the logo's. In a terminal narrower than the logo and its readout the rows used to wrap, which
  broke the logo in two with the readout pushed into it; they are now cut at the edge, as neofetch does. FreeBSD's is
  unchanged. The art is neofetch's, credited in THIRD_PARTY_NOTICES.md.
- Operating systems: `pckmgr` and the Unix package managers are there off a network too, and say they
  cannot reach the Mirror.
- Operating systems: A player's package taken off a machine can be installed on it again. Removing it
  deleted its files but left its folder, and the next install found the folder there and gave up as if the system had
  no folders.
- Operating systems: Erasing a system disk forgets the programs a player installed from the Mirror, which
  lived on it; the desktop went on showing icons for programs whose files were gone.
- Shells: MC-DOS's write puts in the file everything typed after its name, as it is typed: quotes and the
  spaces between words were taken out, so a Sigma program written at the prompt lost the quotes of its strings and did
  not compile.
- Shells: A program in front of a machine's own prompt, on a machine with no desktop, reads what is typed
  there: the line went to the shell as a command instead, so such a program could never be answered.
- Shells: A command that takes the whole terminal, such as `less` given a file, `interac` or an editor, is
  refused on a line that pipes or redirects, instead of printing nothing and leaving the file it was sent to empty. A
  line that ends in `clear` or `cls` now clears the screen, as the command does on its own.
- Shells: A shell takes its pipes and arrows only where they were typed bare. An arrow in quotes (`grep ">"
  notes.txt`), a file name a star opened out into, and a name whose value is a pipe are words for the command, where
  they could empty a file nobody named. A line sent to the background is kept as it was typed, its quotes too.
- Shells: A star in the last part of a path opens out in that folder: `ls data/*.sgs` lists the Σ files in
  `data`.
- Shells: `touch` makes a file that is not there and leaves one that is as it was, where it emptied it.
- Shells: The text tools of the Unix systems (`grep`, `wc`, `head`, `tail`, `sort`) read a file named from
  the root, such as `grep -i error /var/log/cron`; the slash was taken for a DOS switch and the file left out.
- Shells: `mv` with a bare new name moves the file into the current folder, as on a real shell; a move
  across drives that cannot delete its source takes the copy back; a copy never overwrites a file when every copy name
  is taken.
- Shells: Sharing a folder whose name another share already has is refused instead of replacing it, an
  unknown theme name is an error, a bare `&` does nothing, and a live installer keeps the quotes and spaces of its
  lines.
- Shells: `AT` schedules a command that has `/DELETE` among its own words. The word anywhere on the line
  made it a delete, which then looked for a job numbered by the time.
- Shells: A terminal tool that fails on an answer lets go of the terminal, and is not played the answer
  again when the world is loaded.
- Files: A file cut and pasted where a file of its name already is makes the desktop ask whether to replace
  it: Yes moves it over the other, No leaves it where it was. The paste did nothing and said nothing, and the cut was
  gone with it.
- Files: The system's Save As window opens on the kind of the name the program offers, or on every file
  when none of its kinds is that one. It always opened on the first kind listed, so a note the Editor offered as
  untitled.txt and the player named journey.txt was saved as journey.txt.sgs, the first language's sources.
- Files: A file can no longer be written, renamed or copied onto the name of a folder. The disk then held a
  file and a folder of the same name, which a listing showed twice and nothing could tell apart.
- Files: Copying or moving a file to another drive or medium, or to another machine's shared folder, no
  longer writes over a file of the same name already there. A copy on the same drive always refused; across drives it
  was a plain write, which replaces, and a move then deleted the original as well.
- Files: A file can no longer be made under a path whose parent is a file, and `rmdir` and `copy` respect
  an installer medium as the other commands do.
- Files: A file on a disk holds at most 32,767 characters, which is what a disk can carry to a player's
  game: a disk with a longer file dropped whoever opened the computer it was in. A save, a copy, an addition or a burn
  past that is refused, and says why; a file already longer is cut to it when its disk is loaded.
- Files: A disk holds at most 16,384 files and 4,096 folders, so that what it holds can always be listed; a
  write, a copy or a new folder past that is refused like one on a full disk.
- Files: Saving a file whose path is near the longest one allowed no longer fails to send its answer, which
  left the editor waiting: the answer names the path, and with it ran past what the answer could carry. It is now text
  of any length.
- Files: Text cut to fit a message, and a volume label, are never cut between the two halves of an emoji or
  a rare character, which reached the other side as a question mark.
- Desktops: A desktop opens on a machine with more than sixteen programs installed, more than 256 entries
  or more than 256 icons pinned to cells, and the network, cluster and plan screens send at most what their lists hold:
  the lists a desktop is sent were capped and refused whole past their cap, which took the desktop away, and a big
  network could fail to open them.
- Desktops: On a desktop, a window's buttons light only under the window in front, the minimize, maximize
  and close buttons of a window behind another answer the first click and bring it forward, a click on the bare desktop
  takes the keyboard away from the window that had it, and a click on the monitor's frame below the glass no longer
  reaches the taskbar. The desktop's pointer is drawn where the mouse is to a fraction of a pixel, so it moves as
  finely.
- Desktops: Items dragged in a desktop window's inventory land in the cell they are let go over, the
  hotbar's row included, and the player's items in the Network Interactor and the Pattern Studio stay on their cells
  while the window is resized.
- Desktops: A chest in a desktop window, in an inventory, a grid or on the pointer, no longer shows a white
  line down its front corner: an item drawn at the desktop's three-quarter size is laid on the screen's pixels, as at
  the game's own sizes, so its faces meet without a gap.
- Desktops: This PC fits its hardware list's names to the longest of them, keeps a drive's bar clear of the
  words saying what is free, and marks a line it shortens.
- Desktops: Cinnamon's menu searches as you type, lists the category chosen, and has its Quit button at the
  foot of the favourites.
- Desktops: Open with's Choose another program lists every program on the computer that opens files.
- Desktops: Refreshing a folder no longer doubles its path in the title, a link clears the message at the
  foot of a help page, and a copied selection keeps its line breaks.
- Desktops: The editors save the document they were asked to, and close only once the save is confirmed;
  the explorer keeps a listing asked for while another was on its way; Write Out with a new file name writes to that
  name.
- Desktops: A folder being typed into the Settings' sharing field is no longer lost when the page refreshes
  before it is shared, and the field keeps the keyboard through the refresh, so what is typed after it lands there too.
- Desktops: The amber of cautions on the Legacy computing screens was hard to read on their light panels; it
  is a darker goldenrod now, which reads on every ground those screens draw.
- Desktops: The JSC Technologies mark on a modern machine's self-test and Frames 11's start, and the logos
  Frames 95 and Frames XP come up behind, stand in the middle of the glass, where they stood well left of it. In Frames
  XP's setup the lockup in the side panel's header starts in line with the steps listed under it.
- Desktops: The cursor stays where the player left it when a monitor moves on by itself from one screen to
  the next: a system coming up hands its start to the desktop, a restart goes to the self-test, an installer turns its
  page. Each change used to put the cursor in the middle of the window. J's Core does this for every window built on it.
- Desktops: The terminal's scrollback is let go when a world is left.
- Programs: Paint saves its pictures, under the `.pix` name a file of their kind takes, and Files calls them
  pictures. A Save As dialog adds the kind of file to a name typed without one, makes a new folder where it is and
  names it at once, and its list has a menu on the right button, with New folder and Rename.
- Programs: Exceed asks before opening a sheet over one with changes that are not saved, which it threw
  away without a word.
- Programs: Minesweeper no longer fails on the first open area of a board.
- Programs: The Knot client reaches every revision.
- Σ and Σ#: A Sigma# program's windows leave the desktop when it ends, stopped or done, though it was the
  last program the machine ran: with nothing left to run the machine sent nothing more, and the windows stayed on the
  screen.
- Σ and Σ#: The code editors go on suggesting while an if, a while or a for is typed as far as its
  condition: the body not yet written stopped the reading of the whole file, and nothing was offered until it was.
- Σ and Σ#: A Sigma script that overrides only some of Script's methods runs: the ones it leaves alone are
  Script's own, which do nothing. One with no OnInit of its own was halted the moment it started, as the manual's own
  example would have been.
- Σ and Σ#: A shift in Σ and Σ# takes only the low bits of its count, as C# does, five for an int and six
  for a long, and is of the kind of its left side: 1 << 32 was 0, and an int shifted by a long count came out a long.
- Σ and Σ#: Σ, the smaller language, refuses what it does not have wherever it is written: in what a field
  starts as, in what a constructor hands to the one it calls first and in what an enum's names are numbered with, which
  it let through.
- Σ and Σ#: An array in Σ and Σ# answers to `Length`, the number of places it has, which Σ's own advice for
  `foreach` told a program to use and nothing let it read. It is only read: an array is as long as it was made.
- Σ and Σ#: A `break` out of a `switch` no longer lets go of a lock the switch sits inside. Written in a
  loop, inside a `lock`, it let go of the lock at the break and again where the lock ends, and the second letting go
  halted the program for giving up a lock it did not hold. It now lets go only of the locks taken inside the switch.
- Σ and Σ#: An `out` parameter given a value in every section of a `switch` that has a `default` counts as
  given. The compiler threw away what each section did and refused a correct program.
- Σ and Σ#: The compiler catches two `switch` labels that pick the same value written two ways, such as
  `65` and `'A'` in a numeric switch. They were compared as written and both accepted.
- Σ and Σ#: A source nested more deeply than anyone writes, a few thousand brackets, blocks or operators
  inside each other, is refused with an error saying so instead of crashing the compiler with a stack overflow; and
  classes inside each other thousands deep, and a class or an interface that stands behind itself (error S3059), are
  refused, where either stopped the game.
- Σ and Σ#: The Σ compiler clears the field `dispose` names, not a field of the object it held; reads a
  variable a lambda uses only inside a string's `{...}` from where the lambda keeps it; and no longer stops on a `$"`
  string whose last character is a backslash at the end of the file.
- Σ and Σ#: The Σ compiler builds what it used to get wrong: a constructor chained to another picks the
  overload whose types fit, not the first of that many arguments, and its arguments are compiled; a struct captured by
  a lambda before it is given a value starts empty; an `out` argument written to a field keeps its object. A float or
  double literal too large or too small for its type is an error rather than infinity or zero, an unclosed `{` in a
  string is reported once, a `switch` whose section breaks out is checked for its missing `return`, and a record's own
  constructor, `ToString` and `Equals` are left out only for a written one with the same parameters.
- Σ and Σ#: A program asking for an array past its memory is stopped with "out of memory" before the array
  is made, where an array of two billion places ran the game itself out of memory.
- Σ and Σ#: A file a program writes a little at a time costs its memory only what the file holds; each
  write kept the whole file before it too, so a small file ran a machine out of memory.
- Σ and Σ#: A program that waits a very long time no longer wakes at once, a delegate bound to a disposed
  object stops the program, and the calls that return lists cost by the rows they return.
- Σ and Σ#: A package keeps a file whose text has a line starting with `--- `. That line was read as the
  start of another file, so the package came back from the Mirror with the file cut in two.
- Σ and Σ#: A Σ package (`.pkg`) is read whole and only as it was written: a `#` inside a value is kept, a
  file with no final newline comes back without one, a name given twice or a name reaching outside the package's folder
  refuses the package, and only a file that starts exactly with the format's head is taken for one.
- Σ and Σ#: An assembly listing that names a slot a method does not have, labels two lines the same, leaves
  text unquoted or writes instructions after a method with no slots line is refused with the line, where it ran or
  dropped them.
- Σ and Σ#: A Virtual Studio project's name is one word of letters, digits and underscores, so a comma or a
  quote in it can no longer split or break its sources when it is opened again.
- Σ and Σ#: The Virtual Studio no longer drops the connection when a program's listing is longer than a file
  holds: the build fails and says so. The code editor and the terminal editors refuse to save a file that long, with the
  reason, where they dropped the connection too.
- Σ and Σ#: Text a save keeps whole at any length: what a running program holds, its listing, a hosted
  program's binary, the files of an install by hand, a file's revisions in Knot, a job's script and a pattern waiting
  at the encoder. Past 65,535 bytes a save wrote such text as nothing, so it came back empty.
- Σ and Σ#: On the client, a line drawn far off a Sigma window, a bar or a scroll with values near the
  largest a number holds, deleting a selection in a text field whose anchor had been left behind, and a calculator or a
  spreadsheet formula nested hundreds deep no longer crash the game. The calculator takes 256 characters, and a formula
  past that depth is an error in its cell.
- Σ and Σ#: A text box laid out with no width in a program's window no longer hangs the game, a value a
  program's widget cannot show leaves it working, a click just above a list no longer picks its first row, and hover
  and click agree on the edge of a grid cell.
- IQL: Opening a large `.iql` in the studio and running it no longer drops the connection: a script goes a
  statement at a time, and a statement longer than a packet carries is refused before it is sent, its line named.
- IQL: The disks table answers, with each server's disks, and the columns the studio's explorer lists are
  the ones a query reads, rather than names that read nothing; the explorer says when the index is not there instead of
  failing.
- IQL: A second studio window no longer takes the answers meant for the first: every answer names the window
  and the tab that asked.
- IQL: Saving an IQL file from the studio to a full disk no longer fails to send its answer: the reason was
  longer than the answer could carry, which is now text of any length. A long answer to a statement is carried whole
  for the same reason.
- IQL: A file of IQL statements run at the prompt with `run` runs every statement in it. It was read as one
  statement, so any file of two, or one with a comment line, failed as a syntax error, although the same file run by a
  program worked. Both now read a file the one way the studio writes it: a statement a line, with blank lines and `--`
  comments left out, and the whole file is checked before any of it runs.
- IQL: An IQL quantity below zero, a day name it does not know and a number past what it holds are refused
  instead of read as something else; a job's trigger words and a procedure's semicolons inside quotes stay text; a
  schedule goes on after the world's clock is set back; and `UPDATE ... FROM` with `ALL` takes all the named server
  holds.
- IQL: `SELECT ... TO HAND` from the prompt hands the items over; it reported a failure every time.
- IQL: An IQL statement whose `LIMIT` asks for more than 256 kinds of item says that 256 were shown.
- IQL: `QUERY` of servers, computers, recipes and operations reads the same tables as `SELECT`, with its
  columns, and honours `WHERE`.
- IQL: An automation job is paused only when there is such a job, and deleting one forgets its pause and its
  trigger, so a new job of the same name runs.
- IQL: A long name no longer drops the player looking at it. A job named with more than 48 letters broke the
  Processes tab of its Mainframe, a supercomputer node with many programs installed broke the Cluster Manager, and more
  than 64 jobs broke the Automation Manager's list. A job, a view or a procedure takes a name of at most 64 letters,
  from the Automation Manager as from IQL's `CREATE`.
- Operations: Cancelling by a short id that several Operations start with asks for more of it instead of
  stopping one of them.
- Operations: A Mainframe whose system disk is erased or taken out while it runs lets go of the Operations
  it was carrying, as switching it off does: items are conserved and every hold on the storage is released. They used to
  stand frozen, holding the storage, for as long as the Mainframe went without a system.
- Operations: An Operation step that fails on the server's thread no longer takes the tick down with it, and
  the Operation ends as crashed with its real message, where it showed as cancelled.
- Autocrafting: What a machine gives back after its last job settled reaches the network. Nothing collected
  it once no job was running, so a job given up on, or one whose machine was slower than its timeout, left its late
  outputs in the machine.
- Autocrafting: A machine's output is no longer lost when the network has no room for it. A machine step
  pulled everything finished out of the machine and stored what fitted, and whatever a full network refused was gone.
  Only what is stored leaves the machine now, and the rest waits in it until there is room.
- Autocrafting: An import bus broken mid-move drops only what the network has not taken, and a reload sends
  again only that part; items a craft makes that the network cannot take are dropped above the machine, and a fluid or a
  chemical that cannot be stored is said in the log, where they were lost.
- Autocrafting: A craft whose planning failed no longer waits in the Mainframe's list for ever; it is turned
  down as a craft with no way to make it.
- Autocrafting: A craft asks for at most 2,147,483,647 of anything, and a plan's sums stop at the largest
  number they hold rather than going round to nothing, which showed a huge request as already covered.
- Autocrafting: The crafting screen requests the quantity whose plan it shows, never zero, and an amount of
  more than nineteen digits is refused instead of read as 1.
- Autocrafting: A machine recipe's draft keeps no cell of nothing or less, and holds each amount to what one
  run can move, however it was filled; a pattern file read back is held to the same.
- Autocrafting: A pipeline in the Pattern Studio holds at most 16 stages, what its window shows, and a
  pattern too large for a file is refused before it is sent to the encoder.
- Autocrafting: Two recipes for the same result are mirrored on a Crafting Computer's disk under names of
  their own (`torch.craft`, `torch_2.craft`), and removing one removes only its own file.
- Autocrafting: A Pattern Encoder's Cancel queue and Eject buttons could be clicked on the first frame of
  its screen, before the screen knew whether they had anything to do.
- Storage: A computer on no network hands over what its own storage holds through the Network Interactor.
- Storage: A degraded RAID 1 or RAID 5 array keeps the size it was formed with, and a drive whose contents
  cannot be read in full keeps them until it is written again.
- Storage: A simulated insert into an inventory counts every slot, and a volume that cannot be written takes
  nothing.
- Storage: A rack with no addressable computer offers no storage of its own, and a supercomputer survey
  reaches every node.
- Storage: The Network Interactor's Category button keeps its word clear of its arrow, "Nothing on the
  network." no longer fades where the pointer passes, and the Status tab fills the window above the inventory.
- Storage: Setting a RAID mode in the firmware of a rack with no single server to set it on no longer drops
  the connection.
- Computers: A computer, a Mainframe or a rack taken by anything but a player, an explosion, a command or
  another mod's machine, gives back what it holds: its parts, its servers and drives, and a Mainframe or a rack its own
  cabinet too. Only a player's break in survival gave them back; anything else lost them, and a creeper took a whole
  Mainframe. A player in creative still takes the machine and leaves nothing.
- Computers: A computer has one name. Renaming it in its system's settings, at its prompt or in its
  installer renames it on the assembly screen, and the other way round; the two kept names of their own and showed
  different ones. A disk put into a machine with no name of its own names it, as a disk sold with a system does, and a
  machine with a name keeps it over the disk's. A server in a rack and its system are named the same way.
- Computers: A monitor on a computer switched on with parts that do not make a computer says so, and that
  its assembly screen lists why; it said the computer was powered off, so a player who had pressed the switch thought it
  had not taken.
- Computers: Switching a machine off and on while it stood at a failed self-test no longer brings it back
  standing at the old failure, which outlived the power cycle and could send the machine past the new self-test it owed.
- Computers: The Legacy's PCI Crafting Card seats in the Legacy's own boards, AGP and PCI-e, as the
  personal-use cards do, and in the later ones: it fitted only a Vintage PCI board, so no Legacy Crafting Computer could
  take it.
- Computers: An Integra Phi stays out of a computer's card slots, as the manual says: only a supercomputer
  node seats one. A Personal Computer or a Mainframe took it, where it added nothing.
- Computers: Nothing in the mod loads a chunk any more by looking at a place it remembers. A monitor asking
  after its computer, a computer counting its screens, speakers and drives, a Mainframe reaching the servers, racks and
  computers of its network, a cluster's nodes and the file and cluster screens all looked at the block at the
  remembered place, and the game answered a place whose chunk was not loaded by loading it from the disk, or making
  it, in the middle of the tick. Besides the stutter, a chunk caught that way while the game was letting it go could
  stop every chunk from loading until the server restarted. A place whose chunk is not loaded now counts as away, and an
  ssh session whose far machine is away ends as one whose machine has stopped.
- Computers: Nothing a player can save, type or build sends the game more than a packet carries any more,
  which dropped the connection. A Save As or Open dialog (Exceed's and Exposure's among them), an editor's Save and the
  Virtual Studio refuse a name or a folder too long for one, and say so; the print dialog keeps its fields to their
  length. A desktop listing a folder of a great many files, a terminal with a long scroll behind it, many windows open
  at once and a Mainframe with a long list of operations reach the screen cut to what it can show.
- Racks and servers: A server's slots take only the parts its board takes, as a computer's do: a processor
  of the board's socket and era, memory of its generation and a card of its bus. They took any processor, memory or
  card, and the server then failed to start.
- Racks and servers: A rack refusing a chassis of a later era than its own says so, instead of telling the
  player to take it to the kind of rack they are already at.
- Racks and servers: A supercomputer node's assembly screen is titled Supercomputer Node, its name field
  asking for a node's name; it said Server.
- Racks and servers: A server with nothing to boot no longer costs its rack time on every tick. A machine
  whose self-test found no system stands at that failure and goes on by itself once one is installed, and to notice that
  it was asking about its disks, and about every drive it is cabled to, twenty times a second; a datacenter of servers
  kept only for storage spent half of an idle tick on the question. It now asks once a second, each machine on its own
  tick of that second. The rack itself also answers questions about its bays without working out afresh, each time,
  which units are mounted in it.
- Racks and servers: A server rack's screen shows how far the cabinet is throttling even when the rack is
  not on a data network. The reading was only written for servers registered on a network, so a rack off the network,
  and every supercomputer rack, showed a cabinet running free however hot it ran.
- Racks and servers: A rack's KVM switch keeps the machine it was switched to through a save. It came back
  from every reload showing the first machine in the rack.
- Racks and servers: The Mainframes' and the racks' items show the machine as it comes, empty and switched
  off. They are drawn with the same models as the machines in the world, and took on the hardware, the servers and the
  lamps of the last one drawn, or showed every part at once, every kind of server in every row, before any was.
- Mainframes: The service panel of every Mainframe no longer flickers where it meets the edges of its
  opening: its edges lay on the walls of the opening and fought them for the surface, which showed through the grille,
  the louvres and the glass.
- Mainframes: A Mainframe of any era is mined at a pickaxe's pace. It needs the right tool, and no tool
  counted, so taking one down in survival took far longer than a block of metal should.
- Network: Breaking a router in the same tick it was placed no longer throws. What carries the network is
  put in the network's map when it first loads, which comes after it is placed, and breaking a router before that asked
  the map to take out something it never had. Cables already allowed for it; all three now ask the same way.
- Network: Typing a Server Router's name no longer closes its screen at the letter E, the key that closes an
  inventory: while the name is typed every key goes to it, and Escape leaves the field.
- Network: The Import and Export Buses, the Supercomputer Node and the HBW Interface say in their tooltip
  what they are for, like the rest of the network and rack equipment. The words were written but never shown.
- Monitors: A monitor is used by one player at a time. A second player who used it while somebody was at it
  was handed a session over the one being typed into, and ended the remote session the screen was holding; now they are
  told who is using the monitor, and the one at it keeps it until they walk away or close it.
- Monitors: A self-test, installer or KVM screen closes when its monitor is broken, rather than staying open
  on nothing, and a terminal opens on a monitor whose machine is beyond what the player's game has loaded, where it
  failed.
- Monitors: A terminal reopened on a tab that is no longer there, the Craft tab after the last Crafting
  Computer left or the Patterns tab after the card came out, opens on Network for the player too. Only the server moved,
  so the screen showed an empty tab the server was no longer filling.
- Network Gateway: What a Gateway spent on a machine's behalf is still owed after a save when the machine
  runs no program; the save left it out, so a reload wiped the debt the machine was still paying down.
- Network Gateway: A Gateway cuts what a ComputerCraft program hands it, the level of a log line and the
  names in a request, to what its log keeps. A long one stopped the chunk with the Gateway in it from saving. A sentence
  a machine keeps in its save is held to the 8,192 letters it travels in.
- Network Gateway: Two Gateways on one ComputerCraft wired network can no longer publish the same name: a
  rename to a name taken is refused and a default name is numbered until it is free; `a-b` and `a_b` count as the same
  name, as they do there.

### Security
- Payloads: The server reads no more text from a client than each message is meant to carry. An automation
  job's fields, a terminal's server names, the program to uninstall and the name help is asked for were read at
  whatever length a client sent, where the messages beside them were already held to a size.
- Payloads: The Network Interactor, the craft manager and the file windows ignore requests only a tampered
  client sends (a count below none, a recipe before the first, a file renamed from a medium onto a disk), which failed
  on the server.
- Payloads: A crafting pattern whose grid is not nine cells, which a tampered item or file could carry, is
  refused as it is read, and a `.craft` file nested over a hundred deep is refused rather than read to the end of the
  stack.
- Messenger: Nobody can read or write another pair's private conversation in the Messenger by naming its
  room, and nobody is shown the names of the private rooms they are not in, which told who talks to whom.
- Cluster Manager: The Cluster Manager installs on and switches only the nodes of the cluster chosen. A
  modified client could name any reachable rack anywhere, and a row past a cabinet's last stopped the manager's job.
- Monitors: Taking over a remote machine changes only the monitor the player is at.
- Firmware: Booting a live medium from the firmware setup takes only a reader the machine is linked to, and
  only a system its hardware runs, as installing from one already did.
- Terminals: The Interac screen a machine draws is at most 256 columns by 128 rows: a size asked for without
  a limit could take all of the server's memory.
- Shells: A pattern of many stars no longer freezes the server. A shell's file names and IQL's `LIKE` match
  a pattern in a single pass however many stars it holds; each tried every way the stars could split, which a pattern
  of ten of them against a long name made last for hours.
- IQL: An IQL condition nests at most 64 NOTs and brackets deep and compares at most 256 things, and a
  longer one is refused with the reason, where thousands of brackets from a program or a file crashed the server.
- IQL: IQL stops a request after 4,096 statements, its procedures' and views' counted, and says why:
  procedures running one another over and over held the server's thread.
- Σ and Σ#: A Sigma program no longer stops the server's thread, or takes the machine's tick down with it,
  when it copies an object that holds itself, nests thousands of `=` or `? :` in one line, writes thirty calls inside
  one another, calls itself deep with a method of a great many locals, is sent a message with its memory full, or stops
  another program as it leaves. A listing whose method claims more places than any method has is refused when it is
  read.
