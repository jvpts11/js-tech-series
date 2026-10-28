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
- The kits promised: J's Core's API is at version 10, and 66 more of the Core's packages join
  `JsCoreApi.PROMISED_PACKAGES`, all experimental: the content builders, block entities and their fields, menus and
  inventories, machines and their recipes, multiblocks and block spaces, cables and grids, energy, fluids and matter,
  heat and mechanical power, signals, screens and their components, themes and keys, settings, state and saves, data,
  teams and regions, sound, manuals, materials, moving structures, ores and reservoirs, progression, and the series'
  tiers, networks and kinds of Operation. That is what a mod building a machine is handed, followed to its end; the
  photo of the API now says it line by line. A kit that enters whole is marked experimental once, on its
  `package-info`. `IField`, what every field of a block entity is, is public and sealed, and its `flags()` internal.
- The photo travels: J's Core's jar carries its API photo, `META-INF/jstech/api/core.txt`, so a mod checks what
  it uses against the very Core it is built on.
- What the kits still owed: J's Core's API is at version 11, all experimental. `ChunkLoaders.load` takes why
  the chunks are kept, a `TextKey`, between the owner and the chunk, and `ChunkLoaders.reasons` reads them back; the
  form without a reason is gone. `MovingStructures.assembleFor`, `assembleJoinedFor` and `bornFor` make a structure
  for a player, counted against their limit, `born` makes one from a `StructureTemplate`, the `StructureAssembly`
  forms take the maker, and `MovingStructure.maker()` says whose it is. `MachineHistory`, from `MachineMenu.history()`
  on the player's side and `MachineBlockEntity.history()` on the server's, is what a machine did over its last ten
  minutes. `DataScreens.menuOf` gives the menu a screen file is drawn over, and `CoreConfigKeys` has the two new
  limits per player. `ScreenEditorLayout` is the editor's own and leaves the API.
- The Core complete: J's Core's API is at version 12, all experimental. `ExpressionContext.value` hands a
  model's expressions a value for the frame, and `nextFrame` begins another, a query of code being worked out once a
  frame. `DrawingSettings` holds whether the player has alike things drawn together. `ModelBuilder` says a model's
  `scale`, `triangles` and `far` detail, and `ModelDefinition` carries it as its `Detail`, with `MOST_UNSAID`, the
  triangles a model may have without saying; `ModelReaders` reads STL, 3MF, STEP and USD, and `StlReader`,
  `ThreeMfReader`, `StepReader` and `UsdReader` each read one alone; `ModelShaping` brings a model to its detail and
  says what keeps it from it, `MeshReducer` brings a mesh down to fewer triangles, and `CadLook` makes a CAD file's
  colours textures in the game's look. `ModelPreviewLayout` lays the preview out for what its column holds, a
  `ModelPreviewLayout.Column`, in place of the count of animations alone. `CameraPathsLayout` lays out the studio's
  page of camera paths, and `CoreKeys.KEEP_KEYFRAME` is the key that keeps a camera path's keyframe while it records.
  `GuideFamilies` names a family of items a manual's entry covers by one id, `CoreGuide.MATERIALS` is the Core's, and
  `GuideCodecs.BLOCK_TYPES` lists the kinds of a manual's blocks. `ScreenSchema` writes the JSON schema of screen
  files and `ScreenSchemaProvider` adds it to a mod's data run. `UiComponent.animate` moves a component by a motion,
  `moving` says whether it still does and `renderMoved` is how its panel draws it; `dev.jstech.core.motion` joins the
  promise. `FontBuilder.outline` declares a font from a TrueType or OpenType file, `CellFont.pixels` is the size it is
  drawn at and `OutlineFontReader` draws one. `ConfigScreenLayout.RAIL_HEADING` and `HEADING_SCALE` lay out the
  headings of the settings screen's list, and `ConfigScreenTexts` has the words for other mods' settings.
  `StructurePlaces` puts right the places a block entity saved as its blocks move, with a fixer or a leave-alone
  declared by its type's id (`IPlaceFixer`). `MovingStructures.export` saves a structure's blocks as a structure file,
  and `StructureTexts` has the words of the commands that freeze, free, remove and export one. `ContactSolver.add`
  takes the group of points two shapes meet at, `Contact.group` gives it, `forget` and `forgetBut` let go of the pushes
  kept from the step before. `MultiblockRecorder` reads a multiblock's pattern off one built (`Recording`, `Letter`).
  `IPartHost.takesFacades` is true unless a host says otherwise, and `CableBlockEntity` no longer declares its own.
  `MatterTank.bursts`, `relief`, `push`, `relieve`, `bursting`, `burstEnergy` and `pressureNow` let a tank burst and
  relieve itself, `IMatterHandler.pressureNow(tank)` says what each tank holds its matter at, `PressureBursts.hold` sees
  to a tank each tick (`Held`) and `PressureBursts.pipe` bursts a pipe, and `BlastMath.burstEnergy` is the energy of a
  burst vessel. `CableType.Builder.holds` gives a pipe the pressure it holds, `LineBreach` carries the pressure that
  reached a pipe and has the reason `OVERPRESSURE`. `CableEnd` gains `redstone`, an `EndRedstone`, with `withRedstone`
  and `modeAt`, the mode an end has with the redstone where it is; `CableEndMenu.REDSTONE`, `CableEndLayout.REDSTONE_X`,
  `REDSTONE_Y` and `onRedstone` place its key on the end's screen, and `EndTexts.REDSTONE_USE` says what it does.
  `HeatClimate.own` gives the air a source gives a place, and `CoreConfigKeys.HAZARDS_BODY_TEMPERATURE` turns a body's
  warmth off; `BlockTemperatureProvider` and `ClothingProvider` write the game's own blocks' and armour's data.
  `CoreElements.ITEM` shows in a screen's file an item the code hands over (`ItemProps`, `ItemView`), and
  `LabelProps.colour` gives a label a style's role, a written colour or a value's; `ControlElements.SCALE` shows a value
  on a bar of bands (`ScaleProps`, `Pointer`, and `ScaleView` with its `Reading`, `Band` and `Mark`). `FlexBox.gone`
  leaves a box out of a layout, and `DataScreenView.reshape` shows and hides a file's parts by their `visible_if`.
  `BodyScreen` is now the code of a screen declared in a file: `FILE`, `open`, `wire`, `STOP_ROWS` and its palette
  `PALETTE` (`Colours`). `GameLocale.numbers` writes numbers the way the game's language does, which the body's warmth
  on the HUD and its screen and the hazard devices' figures now follow, and `Clothing.covered` is the share of the body
  worn pieces cover. `CoreDensities` registers and builds the new kinds of density function (`TerraceDensity`,
  `RidgeDensity`, `PowerDensity`, `RemapDensity`, `SmoothJoinDensity`, `DistanceDensity`, `OriginalDensity`,
  `ITransformDensity`, and the pure `DensityMath`). `WorldgenModifiers` reads and makes the datapacks' modifiers
  (`IWorldgenModifier`, `DensityFunctionModifier`, `StructureSetModifier`, `declareKind`, `rebind`, `made`), and
  `OperatorStructures` and `StructureReport` are the operator's structures and their dump. `EditPlan` carries what a
  block's entity holds (`data`) and is made `undoable()` (with `flags`), `WorldEdit.undo` gives the plan that puts back
  what it replaced, and `WorldEdits` gains `export`, `planFrom` and `lastFor`; `EditCommands` and `EditTexts` are
  `/jstech edit`. `AwayOwners` lets go of an away owner's chunks by the server's rule, `ChunkLoaders.LoadOutcome`
  gains `OWNER_AWAY`, `ChunkLoadingBalance` gains `keepWhileAway`, `awayHours` and `letsGoAfter`, and
  `CoreConfigKeys` gains `WORLD_CHUNKS_WHILE_AWAY` and `WORLD_CHUNKS_AWAY_HOURS`. `CorePermissions` asks with the
  place's contexts, `DIMENSION` and `CLAIM` (`NO_CLAIM` outside one), through `has(player, node, level, pos)`,
  `has(uuid, node, level, pos)` and `contextsAt`; `ITeamSource.membersOf`, `CoreTeams.membersOf` and `soloPlayer`
  name a team's players. `DiagnosticPage` writes the diagnostic file as a page (`sections`, `marked`, `Section`).
  `ModelDefinitionProvider.check` checks a declared model against its files as the data run does, and
  `KitLayers.remove` takes a layer off the models again.
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
- Machines that fail safe, and J's Industrial's own API: J's Core's API stays at version 12 and
  J's Industrial's is at version 1, all experimental. A machine declares from its constructor that it fails safe
  (`MachineBlockEntity.failsSafe`, and its port's `EnergyStore.failsSafe`). `MachineStatus.OVERVOLTAGE` is a machine
  that refused a line above its class rather than burn, read through `MachinePower.overvoltage` and shown on its face
  with `MachineScreenTexts.LCD_OVERVOLTAGE`. J's Industrial's API is the package `dev.jstech.industrial.api`:
  `JsIndustrialApi.VERSION`, `Consequences.of` (the switch every consequence of the mod has, on unless a server turns
  it off, saying what happens instead), `IndustrialAdvancementTab` (the four tabs an add-on hangs its advancements
  under) and `IndustrialSoundGroup` (the groups its sounds are mixed in, each in a channel of the Core's).
- Figures drawn more than one way, and sections lettered: J's Core's API stays at version 12, all
  experimental. `GuideBlock.Modes` is one figure with several ways of drawing it, declared with
  `ModGuide.EntryBuilder.modes(words...)` before its ways and laid out as `GuidePiece.Switch` over the
  `GuidePiece.Way` pieces of each way, in the room `GuideLayout.SWITCH_ROW` leaves for the switch. `GuideSection`
  has a `code`, set with `ModGuide.SectionBuilder.code(letters)` (or `code:` in a Markdown section's header), that
  numbers a set's drawings by those letters.
- Counted byproducts, wearing catalysts, formulas and the hazards of materials: J's Core's API stays at
  version 12, all experimental. A `Byproduct` comes by a chance or by a counted share (`Byproduct.Draw`,
  `Byproduct.counted`, `ProcessingRecipeBuilder.countedByproduct`, the field `"share"`), counted by a `Tally`; the
  `Processing` of a recipe names its catalysts' slots (`ProcessingRecipe.catalystSlotsFor`), and `CatalystWear` wears a
  damageable catalyst into the spent form its data map `jscore:spent_catalysts` names. `ChemicalFormula` reads a
  formula, with its atoms, molar mass and mass fractions; `MaterialProperties.ATOMIC_WEIGHT` is an element's weight,
  `Elements.atomicWeight` and `bySymbol` find it by symbol. The package `dev.jstech.core.hazard` adds `HazardClass`,
  `GhsPictogram`, `GhsPictograms`, `GasHazard` and `MaterialHazards`, the hazard classes of a material and what follows
  from them. `CompressionMath` is the work of compressing a gas. `MineralBuilder` and `TraceGrade` declare a mineral
  from its formula, `SeriesMinerals` holds the catalogue's minerals and `SharedCatalogue.AMMONIA` ammonia; a mineral's
  byproduct may weigh more than its main element (`MineralShares.Problem.BYPRODUCT_OVER_MAIN` is gone).
  `ProgressionTriggers.whenMade(IMadeListener)` tells a mod of everything made, and
  `ElementNaming.discover(server, player, element)` records a discoverer who is away, told when they join
  (`Discovery.told`).
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
- Research: the new package `dev.jstech.core.research`, experimental, research for any mod. A `Discovery` is
  declared with its builder (`when`, `year`, `field`, `costs`, `needs`, `after`) and is a piece of knowledge a team
  holds in part while its research goes; `Discoveries` holds them, their costs in data packs
  (`data/<namespace>/jstech/research/`, written by `DeclaredFilesProvider.researchCosts`), which are there to research
  (`existing`, `exists`) and whether a mod's setting makes its research lock anything (`gate`, `required`). A kind of
  data is a `DataKind`, declared in `DataKinds`, with an item that is `IResearchData`, such as `ResearchDataItem`,
  whose `from()` says what gives it. `Research` checks and works a discovery on for a team (`check`, `work`, `share`,
  `made`, `ready`, `missing`, `IDataStore`, `Outcome`), and `ResearchMath` is its pure arithmetic, with the power of
  research computers working together (`groupShares`, `groupPower`). `KnowledgeGate.openWhen` takes an
  `IKnowledgeOpener` that opens knowledge to everyone, `KnowledgeGate.opened` asks it, and `KnowledgeGate.opens`
  names what the recipes that ask for a piece of knowledge make.
- A machine's own screen, wider and with tabs of its own: `MachineBlockEntity.inventoryOn(tab)` takes the place
  of `inventoryOnMain()`, `screenWidth()` gives a wider screen, under which the menu centres the player's inventory and
  the standard tabs' slots, `ownTabs()` gives it tabs after the five every machine has, and `screenData()` gives what
  the screen shows that only the server knows, a `MachineScreenData`, carried while the screen is open and read with
  `MachineMenu.screenData(Class)`; `MachineMenu.tabs()`, and `MachineScreenLayout.MOST_TABS`, `kitShift(width)`,
  `playerInventory(width)` and `tabAt(x, y, count)`. A device's block entity that is `IMenuTitle` opens its menu under
  a title of its own, as a multiblock's terminal opens under its machine's name.
- `CableType.THICKEST`, the thickest a cable is, and `CableItemModelProvider`, which writes the length of cable
  of every thickness that every mod's cable items are drawn from.
- A machine's own rules for its upgrades and its damage, recipes in their own energy, and batteries of any
  energy: J's Core's API stays at version 12, all experimental. `ProcessingMachineBlockEntity.combineUpgrades` is how
  a machine's upgrades come together, `UpgradeEffect.combine` as before or `UpgradeEffect.addGains`, which adds what
  each gains and multiplies what each saves. `MachineBlockEntity.damageRule` is what damage does to a machine, an
  `IMachineDamage`: `GRADUAL` as before, or `flat(speed, energy)`, a set speed and draw while damaged at all, its
  upgrades idle. `ProcessingKind.runsOn` names the energy a kind's machines run on, `energy()` reads it, and its
  recipes show the power they draw in it. `ItemEnergyPort` is the energy an item holds of a mod's own energy, in one
  class, and `CoreEnergy.holdsPort` makes items batteries of it.
- Pipes that wear, gas clouds, valves and the pneumatic post: J's Core's API stays at version 12, all
  experimental. The marks of what pipes carry are an open list, `PipeMarks`, each declared once with the materials it
  covers: `madeFor`, a mark a pipe has to be made for, and `wears`, one that wears a pipe through; `CoreFluidTags.GASES`
  and `CORROSIVE` are two of it. `PipeLimits` gives up its fixed `MARKS`, carries the marks that wear a pipe
  (`wornBy`, `wearsIn`, `wears`) and reads materials too (`carries(Material)`, `beyond(Material)`).
  `CableType.Builder.wornBy(mark, ticks)` says how long a pipe stands carrying a mark, `LineBreach.Reason.WORN_OUT`
  reaches it worn through, and `LineWear` keeps each pipe's wear with the world. `GasLimits` gives a gas its explosive
  limits and what of it is immediately dangerous to life, and `GasClouds` lets a gas out into the open as a cloud.
  `LineValveBlockEntity`, with `IValveRule`, is a valve on a line of liquid or gas, and `ReliefValveBlockEntity` a
  relief valve against a vessel. The new package `dev.jstech.core.transit`, promised, is the pneumatic post:
  `CapsuleStationBlockEntity`, `CapsuleRules`, `CapsuleRoute`, `Capsule` and `CapsuleTransit`. What a machine's faces
  offer of a state of matter is its `matterHandler(state)`, for any machine.
- Zoned veins and modules by trait: J's Core's API stays at version 12, all experimental. A vein's
  `DepositType.Mineral` says the part of the vein it lies in (`DepositType.Zone`: all, upper, lower) and the biomes it
  lies in, `DepositType.zoned()` says whether any does, and `Deposits.DepositBuilder` takes them (`mineral(material,
  weight, zone)`, `mineralIn`, `mineral(Mineral)`); `Deposits.supersede` lays a kind no more, file and all, and
  `Deposits.superseded` reads them. `ModuleDef` says how many of it one host takes (`most`, `upTo`) and what a host
  needs to take it (`needs`, `needing`), and `ModuleHostDef` its traits (`traits`, `with`, `has`); `SeriesModules`
  names the series' sockets, pieces and seal.
- Recipes from a machine's screen and a symbol set apart: J's Core's API stays at version 12, all experimental.
  `MachineScreenLayout.onProgress` is where a click on a machine's progress lands, with `ARROW_REACH` and
  `BAR_REACH`; `MaterialProperty` carries `headline`, which `MaterialProperties.Builder.headline()` sets, a record
  component of its own beside the constructor without it; `CoreConfigScreen.buttonCentre` is where a test clicks a
  button of the settings screen.
- Cables laid by hand: J's Core's API stays at version 12, all experimental. `CableType.sound()` and
  `CableType.Builder.sounds(SoundType)` say how a cable sounds, wool unless said; `CableItem.landing` is where a click
  with a cable lays it, the same for the player's game and the server; `CableItem.showNow` shows the players cables
  laid or taken out by hand, with their neighbours, at once; `ClientUpdates.sendNow` sends any block and its block
  entity to the players watching it without waiting for the end of the tick.
- Multiblocks built whole and worked from their front: J's Core's API stays at version 12, all experimental.
  `IStructureShape.quotas()` gives the counts a shape limits as `Quota`s (a name, the blocks counted, a `Limit`), which
  `MultiblockPattern` works out from its limits; `BuildOrder.plan` takes them and how many of each already stand;
  `MachineBlockEntity.structureChecked(level)` runs each time a terminal's structure was checked again.
- Bodies pushed out without being thrown: J's Core's API stays at version 12, all experimental.
  `RigidBody.pushOut` moves a body out of what it sank into over its next step, with nothing added to how fast it
  goes or turns, and `RigidBody.pushedOutAt` says how fast a point of it is pushed out so.
- Contacts read before they start: J's Core's API stays at version 12, all experimental.
  `ContactSolver.ready` readies a step's contacts and `warm` starts them from their pushes of the step before, so
  several solvers on the same bodies read them before any pushes them; `begin` does both, as before.
- Tools that strike: J's Core's API stays at version 12, all experimental. `ItemBuilder.strikes(damage,
  speed)` makes an item a weapon in the main hand, with the harm a blow and the blows a second its tooltip shows,
  counted as the game counts a tool's; `IItemLook.HANDHELD` holds its flat texture slanted up in the hand, as an axe.
- Sources spread over their blocks: J's Core's API stays at version 12, all experimental.
  `Radiation.transmission(level, from, to, kind, source)` lets through the blocks `source` names, the source's own,
  so a source read from the middle of its blocks is not shielded by them.
- Formed multiblocks worked by hand: J's Core's API stays at version 12, and `dev.jstech.core.multiblock.hands`
  joins the promise, experimental. An `InteractiveModel` names the doors, places and controls of a formed model by
  its bones, `MachineStructure.handsOn` gives it to a structure, and `HandGeometry` says, free of the game, which part
  a line of sight meets, the box it is outlined by and how the model stands round its terminal.
- Items drawn in three dimensions: J's Core's API stays at version 12, all experimental. `IItemLook.HELD`
  shows an item's flat texture in a slot and an item frame and its model `held/<id>` in the hand, on the ground and on
  the head, as the game's spyglass and trident are.
- One type of era: J's Core's API stays at version 12, all experimental. `Era` is the one type of era in every
  mod, a record of its id, its name, its colour and the year its machines are made in, compared by its place in the
  order (`isAtLeast`, `isAtMost`, `next`, `prev`). `HardwareEra` is no longer an enum but the class holding the
  series' seven as `Era` constants, with `SERIES`, `WITH_PARTS` and `withParts`; `Eras` declares an era at the end or
  after another and finds one by its id or a bare name; `EraTable` gives values era by era, an era without its own
  taking the nearest earlier era's; `EraCodecs` saves an era by its id and sends it by its place. An axis whose steps
  grow while the game loads is `ProgressionAxis.following`, and `PlayerProgress` and `TeamProgress` keep every axis's
  step by its name.
- Machines on the network that see the world: J's Core's API stays at version 12, all experimental.
  `INetworkMember.instrumentOf()` names the world events a member is the instrument of, `NetworkTables.hasInstrument`
  asks whether a network has one, and `NetworkSystem.networks()` lists the networks of a level that run.
  `Device.ports(FacePorts)` gives a device's block the ports a cable joins it through, and `DeviceBlock` is an
  `IFaceConnector` answering by them.
- Work given to a machine: J's Core's API stays at version 12, all experimental. `OperationType` has a `name`,
  the `TextKey` a player reads the kind by, and `text`, the `MapCodec` a request of it written as text is read with
  (null, and the constructor without it, for a kind only code asks for), with `readsText()`. A handler is handed an
  `IOperationRequest` in place of the arguments alone: what it carries, the machine of the network given it, `report`
  and `fail` for how the work goes until it is over, and `cancelled` and `whenCancelled` for a request called off.
  `OperationRequest` is that request as whoever gives it holds it: `start()` hands it to the handler, a handler that
  throws failing it, `status()`, `failure()` and `cancel()`. The test kit's `NetworkScenes.request` gives one to a
  member and hands it back to follow.
- What a controller reads and how it is wired: J's Core's API stays at version 12, all experimental.
  `IMachineParameters` gives `statusNow()`, `redstoneMode()` and `setRedstoneMode(mode)`, and `powerDrawn()`
  (`PowerDrawn`: the energy, what was drawn in the last tick and the port's class); a machine on a port of the series'
  energy declares `fuse` and `battery` (`EnergyProtection.batteryShare()`). The Core's valves offer the capability
  (`open`, `flow`; a relief valve's `set_pressure`, with `ReliefValveBlockEntity.register`). `ControlLinks` is the
  industrial control cable's line, grid and links: its wires plug into any block offering its values, and a controller
  keeps its links (`keep`, `release`, `controllerOf`, `reached`). `IPeripheralOwner.linksOver(type)` lets an owner own
  endpoints on more than one cable system, and `PortKind.CONTROL` is a controller's port. `INetworkMember.fillRows`
  writes a row of a table for each machine a member reads (`StateRows`, each row where its machine stands), and
  `INetworkMember.refuses` says why a member will not take a request, which the network then gives to one that will.
- A family's model in another mod's files: J's Core's API stays at version 12. A `GeoLook` model named with
  its mod (`othermod:computer_vintage_reader`) is read from that mod's `geo/` and its atlas from that mod's textures,
  so a mod brings a member to another mod's family of machines.
- The pages of another mod's things: J's Core's API stays at version 12. `GuideEntry.aboutAbsentItems(present)`
  says whether an entry is the page of items the game has none of, which the manuals leave out.
- A capsule station a controller drives: J's Core's API stays at version 12. `CapsuleStationBlockEntity` offers
  `IMachineParameters` (`address` and `otherwise` set, `waiting` read; `MOST_ADDRESS`), registered with its other
  faces by `register`, and `sendNow(level, to)` sends what waits in it to an address at once, answering a
  `SendOutcome` (`SENT`, `NOT_YET`, `NOTHING`, `NO_STATION`).
- A picture on a creative tab: J's Core's API is at version 13. `ContentTab.picture(texture)` shows a 16-pixel
  picture on a tab's button in place of its icon item, `ContentTab.pictureOf(tab)` says which a tab shows, and
  `ModContent.tab(id, title, picture)` declares a tab with one; all experimental.
- Manuals made as a mission's documents were: J's Core's API is at version 14. The blocks
  `GuideBlock.Caution`, `GuideBlock.Checklist` with its `Check` lines and `GuideBlock.Card` with its `CardStep`s, the
  entry builder's `caution`, `checklist` and `card`, and `ChecklistLines`, which reads a line or a step as Markdown
  and code write it. `GuideStyle.Decor` gains `boxedHeads`, `headedNotices` and `shortFigures` (and
  `missionDocument()`), `Folios.SECTION_PAGE`, `Marks.SECTION_TABS`, the roles `CAUTION`, `CAUTION_PAPER`,
  `WARNING_PAPER`, `CARD` and `MARK` with `standIn(role)`; a `GuideSection` has its `tab` colour
  (`SectionBuilder.tab`); `GuideTexts` gains `CAUTION`, `CAUTION_LINE`, `CHECK_LINE` and `FIGURE_SHORT`. All
  experimental.
- Typed lines on a cover: J's Core's API is at version 15. `GuideStyle.PartKind` gains `CENTRED_CODE` and
  `RIGHT_CODE`, a cover line in the style's code font centred in its part or set against its right edge.
  Experimental.

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
  player; the world's own work stays inside the claim it comes from. Robots ask before they break a block. The Core's
  own liquids ask before they spread into a claim, its projectiles before they strike there, and a moving structure
  crashing into a claim breaks only what its maker may break.
- Permissions for everything: every command of the series, opening, using, setting up and breaking what others
  own, changing a server's settings from afar, building multiblocks and going past the limits have a permission
  node, which LuckPerms and FTB Ranks grant or deny; without them the operator levels apply. `/jstech dump permissions`
  writes them all out, with two LuckPerms groups ready to paste into the console.
- Trust by name: the owner of a thing trusts players by name to use it, to set it up as well, or to break it as
  well. An owned block or entity is kept from whoever its owner does not let break or use it.
- Limits per player: how many of a kind of thing one player may have at once, kept with the world, starting
  with robots (16 each by default), moving structures made and not yet set down (32) and large edits of the world
  under way at once (4), each a setting of the server's.
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
  the rest, a plate, a sheet and a foil outlined dark at both their ends so the slot's edge never seems to cut one
  off; an ore is its rock's own texture with the specks of one of the game's ores in the mineral's colour. No
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
- The ends of a line: each end where a pipe or a line of items plugs into a block is set on its own: normal,
  push, pull or off; a priority, the highest filling first and ends of one priority taking turns; a rate; one of sixteen
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
  their own, besides their tab's. Held as a tool, it strikes as a bar of iron: 5 a blow, 0.8 blows a second.
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
  locks an input slot to what it holds. A click on the LCD turns it to the machine's last ten minutes: how much of
  the last minute it worked, and a chart of what it made a minute and what it drew, told in full in its tooltip with
  the last thing that jammed it.
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
  Any mod's saw cuts alike, and a pack can keep a block from being cut. Held as a tool, it strikes as a light blade:
  4 a blow, 1.2 blows a second.
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
  A part that is a sensor is read on its face by anything that reads sensors, as a sensor block is.
- The game's torches, levers, buttons and pressure plates share a block space: used on the face of the block
  beside a cable or a space of parts, the item hangs its block in the space, beside the wires and the other pieces,
  where it lights, powers, is pulled, pressed and falls with its wall as the game's own does.
- A wrench of any mod on a part of a cable or a space of parts turns it, a device a quarter turn about its face,
  a lever or a button on a floor a quarter turn, a post through the middle to its next axis, and sneaking takes the
  part off into the player's hands. Jade names the part looked at and shows what it says it is doing.
- The microblocks' rules in JEI and EMI: the saw's cuts, two pieces joined and a facade are one crafting recipe
  each, going through every block and piece rather than thousands of entries; looking up a block in JEI shows the
  cuts of that block.
- Parts declared once, as blocks are: a mod names a kind of part, gives it a look and says what it does, and
  gets its item, which mounts it on a cable, a space of parts or the face of any block, its name and its models from
  the data generation, a plate's included. The GameTest kit builds spaces of microblocks, parts and small blocks.
- Conveyors: a mod declares its kinds of belt, with their speed, how many items a block holds, what makes them
  move and what they do at an end with nowhere to go. Belts of a kind one behind another move as one run, and only
  its end ticks, so a long line costs one belt; the items on a belt are a queue, never entities, drawn moving smoothly.
  A run hands its items into a belt or an inventory in front, or holds them there, or lets them fall off; hoppers,
  pipes and items that land on a belt put items on. A belt moves by itself, as a shaft at any of its run's belts
  turns it, or on energy any of them takes in, and carries the players and mobs that stand on it.
- Conveyors that climb, fall, deal and sort: belts that rise or fall a block, laid to meet one another as rails
  do and turned by a wrench; chutes that drop items into what is under them; bucket elevators that lift items and tip
  them out to their front; splitters that deal items out in turn, joins that take from each side in turn, and
  diverters that turn aside what their filter names, learning it from a click with the item. A machine on top of a
  belt works the items that pass under it, holding each still while it works. A broken piece lets fall what was on it,
  and a mod declares its own pieces on the same kit.
- Machines of any mod on the data network: a block entity says what the network may do with it, the kinds of
  Operation it carries out and what the network may read of it, and the Core keeps it on the network of the data
  cable it touches, with no tie to J's Computers; a member that could never be what it says stops as it is made. Its
  state is read as tables a mod declares, a row for each machine and a column for each value, numbers in their units,
  text and flags, so whatever runs the network reads a reactor the way it reads anything else.
- Progression kept per team: the eras of hardware are a registry an add-on's era goes into the middle of; the
  axes move by playing, as a player first makes an item, at a table, in a furnace or in a machine they own, or places
  a block; what a team reaches is the team's alone, and two teams that merge keep everything either had. Mods declare
  ordered milestones a team reaches once, and knowledge a team comes to know whole or a share at a time. A machine
  recipe may ask for a piece of knowledge: the machine will not run it until its owner's team knows it, saying "Needs
  knowledge", while JEI, EMI and the manuals always show the recipe, with a padlock. Operators give and take
  knowledge and milestones with `/jstech knowledge` and `/jstech milestone`.
- Worlds of the series open with no warning about experimental settings: the data and the dimensions of the
  series' mods are taken as stable, and any mod may say the same of its own namespace; another mod's dimension still
  warns. Every place the Core reaches into the game is listed with its reason and a test that fails if the game moves
  what it reaches.
- Models: animated models for blocks, items, entities and armour, read by the Core's own readers from
  Blockbench projects, Bedrock and GeckoLib geometry and animation files, glTF (text and binary), FBX (binary and
  text, version 7 on) and OBJ, with bones in a tree, cubes, meshes, locators, skins and morph targets. A model is
  declared once, with its textures, its animation files, the bones that glow, whether it is see-through, and variants
  chosen by the block's state; the data run writes its definition and stops at a file that is missing or does not
  read. Controllers play animations with every Blockbench easing, loop, hold or once, at any speed, several at once,
  blending from one to the next; Molang in keyframes reads what the code gives it; sounds, particles and words to code
  happen on their times; code moves bones over the animations; items are held at locators; and the server can play an
  animation for everyone who sees the model. A model is worked out once and shared, animated only in sight and near,
  and under a shader pack drawn with the game's own shaders, so the pack draws it. Families of files laid out for
  GeckoLib are drawn by
  the Core's models as they are, alike to the pixel.
- What entities need from a model, ready-made: an entity's model, and armour on whoever wears it, reads the
  entity's own queries under Bedrock's names (how fast it goes, how its limbs swing, whether it is on the ground, in
  water or on fire, how it turns its head, how hurt it is, the time of day), so a walk is written in the model's file
  with no code; and a query of code is worked out once a frame. A resource pack can draw any of the game's creatures
  with a model of the Core's, its animation reading those queries.
- Textures that move and glow on the Core's models: a texture of frames with its `.mcmeta`, as the game's block
  textures are, shows the frame of the tick, every model in step; a glowing texture beside it, the same name with
  `_e`, is drawn over it at full light. Mods lay layers of their own over any model (a glow, an outline, a shield), and
  an item's model can have the player's own arms drawn at two bones in first person, holding it.
- Alike things drawn together: every copy of a model of the Core's in the world (block entities, entities,
  turning parts) is drawn in one call for each of its pictures, the graphics card holding its faces and each copy
  handing over only its light and where its bones are, so a rack of a hundred alike drives costs about what one does;
  the effects' particles are drawn the same way. Under a shader pack, and for a player who turns *Draw alike things
  together* off, everything is drawn one by one as before, and the two ways look the same.
- Models from CAD programs and scenes: STL (text and binary), 3MF, STEP (AP203, AP214 and AP242) and USD (text,
  binary and packed, with the files it references) read by the Core's own readers, each part of a file a bone, so an
  animation turns the impeller of a pump by naming it. A CAD file's colours become textures in the game's look, laid
  on its faces from the sides as a block's are; its size is its real one, a metre a block, or as many times bigger as
  the model says; STEP's solids are laid out as they are read, every face on its own surface (planes, cylinders,
  cones, spheres, tori, B-splines). A model says the most triangles it is drawn with, near and from a distance on, its
  meshes brought down to them as they are read with the same bones, so it moves the same far away; one that says
  nothing may have 20 000, and the data run stops a file with more.
- The preview of a model: `/jstech model preview <model>` opens it, a declared model or any model file by its
  path, turned, zoomed and moved with the mouse over a floor of quarter blocks, its animations played or paused from
  their buttons, its bones in a tree with each one's triangles, the chosen one shown in yellow, its triangles against
  the most it may have, how it is drawn far away, its size and its colours, its model far away, its locators by name,
  its glowing parts shown in the dark, its faces as wire, a timeline to drag and a speed to step. In a development run
  a changed model file reads the models alone again, at once, and an open preview shows the change; `/jstech model
  reload` does the same by hand.
- Formed multiblocks: a structure can turn into one detailed model once it forms, a turbine's casing with its
  rotor turning in place of its iron, turned, mirrored or laid down as it was built. Its plain blocks merge into parts
  that draw nothing and stand in for what they were, breaking as slowly and dropping the block they were; the
  controller, the ports and every block with a block entity stay, so pipes and cables still meet them. Forming puffs
  where each block merged and can play a sound and an animation; breaking any block gives every one back, and a part
  whose controller is gone turns back by itself. A damaged machine's cracks show across its whole structure.
- Formed multiblocks worked by hand: a formed model's doors, places and buttons are worked in the world,
  without its screen. What the crosshair is on is outlined by one box in place of the block; a click opens or shuts a
  door, drawn swinging on its hinge, sets the part in hand in a place behind it, or takes out what the place holds
  with an empty hand, and presses a button. A place is reached only through its door's doorway once the door is
  wholly open, never through the walls. The server works a part only for whoever may open the screen and whose own
  line of sight meets it; whether each door is open is saved with the machine.
- Turning parts: shafts, gears and pulleys drawn turning at their real speed by the Core's models, the angle
  following the speed frame by frame, meshed gears half a tooth apart so their teeth meet, every part of one model
  sharing its shape; a belt or chain runs between the pulleys it joins as fast as their rims. A part drawn turning
  hides nothing beside it and is lit as its place is. A part can be heard turning, louder and higher the faster.
- Hot things glow: a block that holds heat past 798 K, the Draper point, glows in the colour of a black body at
  its temperature, from Planck's law and the eye's colour matching, dull red to orange to yellow going white, as
  bright by night as by day.
- Thermal goggles: an item in `jscore:sees_heat`, worn or held, shows the world as a thermal camera does, what
  is at the air's temperature under a dark veil and, in false colour, the blocks that hold heat, liquids far from the
  air's temperature such as lava, and living things at a body's warmth (the undead as cold as the air, anything
  burning hotter); beside the crosshair, how hot the thing looked at is, in kelvin and degrees Celsius.
- Models drawn from a whole block take the light around the block, where none reaches inside it.
- Connected textures of resource packs: textures that join between alike blocks, read from both formats packs
  write, so a pack made for OptiFine, Continuity or CTM works as it is and a mod declares its own as data. OptiFine's
  and Continuity's `.properties` rules with every method (the full 47 tiles, compact, horizontal, vertical, both ways,
  top, random with weights and symmetry, repeat, fixed, and overlays of every kind with their tint and layer), the
  textures and blocks they match with their properties, faces, biomes, heights, weights, names and inner seams; CTM's
  metadata with every type (ctm, horizontal, vertical, pillar, simple, random, pattern, edges, full edges, eldritch),
  its light, layer, proxies, conditions with predicates and faces of their own, its overrides in block models, and
  logics written as data, OptiFine's full sheet among them. Blocks may be named by tag. Faces join by how their
  texture lies on them, partial faces take the same part of their tile, and microblocks join their like in the spaces
  around. Glowing textures in OptiFine's form too: where a pack says their ending (`suffix.emissive=_e`), a block's
  texture with a glowing one beside it, and a tile's, is drawn with it over it at full light, shining in the dark.
  While Continuity or CTM is installed the Core steps aside and says so, unless the player's setting keeps it.
- Lamps: kinds of light as data, declared by a mod and written over by a data pack (colour, intensity, reach, a
  point, a spotlight's cone or a narrow beam, a flicker by name or by its own letters, what turns it on, the watts it
  draws, whether it breaks), and lamps of a kind that shine always, by redstone or while they get their watts, flicker
  each from its own point, and go dark broken until mended. Their light is the game's own, so it counts for what
  spawns and grows.
- Spotlights and beams reach far past fifteen blocks: their rays go on through air and water and light the
  place where they meet something, as bright as the light still is there, with light that counts in the game; it is
  given back when the lamp goes off, and light left where no lamp holds it goes by itself.
- Coloured light: the colour of the light of blocks, of what is held and of mobs is data, the game's own filled
  in (soul fire, redstone, the sea lantern, amethyst, the froglights, lava, blazes and more); light of several colours
  mixes and tints the world it lights, fading smoothly across faces, only where its light goes, round walls and never
  through them, less under open sky. Light that passes stained glass takes the glass's colour, white light through a
  red pane lighting the far side red, and daylight under a roof of stained glass takes the roof's; the colour a block
  gives the light passing it is data too. The light is worked out away from the main thread, a batch of sections a
  tick. It rides in the world's mesh, so it looks the same with Sodium, Embeddium and Iris as without them, and a
  shader pack such as Complementary multiplies it into what it draws.
- Dynamic light: a torch held lights the way, a mob on fire its surroundings, an item that gives light the
  ground it lies on, a model's glowing part that casts light the world as it moves, and a mod's own moving lights;
  drawn on the player's game only, nothing of the world changed.
- Glow without a shader: soft halos about bright lamps, blocks of coloured or bright light and glowing parts,
  in their colour, and the shaft of a spotlight through the air, fading along its reach, with the clouds behind them.
  Under a shader pack the halos step aside for the pack's own bloom.
- Light sensors read a place's light, from 0 to 15, and the hue of the light there.
- The player's lighting settings: coloured light and dynamic light on or off, the glow's strength, how far it
  all reaches. Dynamic light steps aside for LambDynamicLights and Sodium Dynamic Lights, coloured light and glow for
  Shimmer, unless the player's setting keeps the Core's on as well.
- Particles and effects as data: an effect is a file of emitters with shapes (a point, a sphere, a box, a disc,
  a cone, a line, the faces of a model), a rate and bursts, particles whose size, colour, picture and spin change
  over their life, with a light of their own, a ribbon behind them or drawn out along their speed as sparks and
  tracers streak, collisions with blocks, and emitters they carry. They move by the physics of where they are: the
  dimension's pull, drag and lift by the air's density, the wind, the water they are in; steam rises and drifts,
  nothing rises in a vacuum, a plume widens as the air thins, bubbles live only in water. They are drawn after the
  clouds, so a cloud lies behind a plume of steam rather than across it.
- The wind: a few metres a second in a world with air, turning over the days, stronger in rain and storms and
  higher up, the same on the server and every player's game; a mod's weather says its own.
- Beams, electric arcs that zigzag and branch, ribbons and tracers; marks that fade on the world, scorch and
  soot; and what effects do to the screen: a flash, a shake, the ring of a shockwave bending the view, the air
  shimmering over hot things, a hazard's vignette.
- Effects fired from the server reach whoever can see them in a few bytes, aimed, coloured, scaled, following
  an entity or played on a block where its file anchors it; a model's animations play them at their locators. The
  Core brings smoke, steam, sparks, a beam, a trail, an arc flash (now fired by overvoltage), an explosion, a cloud of
  gas, bubbles, a rocket's plume, a jet's exhaust, a contrail, re-entry plasma, scorch, soot, a laser and lightning.
  An effect held by something moving fast lays what it gives off along the way it went each tick, so a racing
  engine leaves a line, not beads; and a held effect can be scaled, an engine's flame as big as it pushes.
- The effects editor in the world: the effect plays on the real block with the world's wind and air while a
  card turns its emitters on and off and changes their numbers, the game's debug lines saying what it costs and what
  acts on it; Save writes its file in the mod's source. A new effect starts at the block looked at, from a face or a
  locator of its model and from smoke, steam, sparks, a beam, a trail or nothing, playing there at once.
- The player's effects settings: particles at once, how hard the screen shakes, how bright a flash is, and
  whether heat haze and shockwaves bend the view; the game's own particle setting thins them out.
- Terrain declared in code: how high the land lies, how far and how wide its hills, how rough, its rock, the
  liquid of its seas up to their level, a floor of bedrock and its surface biome by biome, written as the noise
  settings a data pack can change; a dimension takes it with its biomes spread by the terrain's warmth and wetness.
  Beneath that, a mod declares the pieces themselves: noises, density functions of its own, and surface rules read
  in order (bedrock, a cap on the peaks, bare cliffs, a sea floor, each biome's layers), a terrain built of them.
- Biomes declared in code, with their climate in real values: degrees Celsius over the year, how far the
  seasons swing it, millimetres of rain a year, kept in a data map a data pack can fill for any biome; every other
  biome's read from the game's own values. A place's warmth follows the calendar's seasons and cools with height,
  and heat meets it where a biome has a climate of its own.
- Landforms as data, laid a chunk at a time from the world's seed so any size draws whole: craters with
  their ejecta, rays and central peaks, shield volcanoes and stratovolcanoes with lava in their summits, mesas in
  bands, dunes, canyons, rilles, lava tubes open to the sky here and there, ice sheets, glaciers with their
  crevasses and moraines, geysers that bubble, cryovolcanoes, sinkholes and cracked salt flats.
- Lakes, rivers and seas of any liquid, which freeze over where the place is colder than their liquid
  freezes and boil away where it is hotter.
- Rock in layers by depth, each region of the land with its own sequence, the layers rising and falling
  gently and the ores lying in them.
- Ores lie in veins: zones three chunks wide hold at most one, dense at its middle and thinning outwards, of a
  kind declared as data (its materials and their weights, its heights, how often, how large and dense, its biomes and
  rocks), which a data pack can change or add to. A vein crosses rocks and each block takes its rock's own ore, laid
  only where some installed mod uses that ore; the same for the same seed on any number of threads.
- The game's iron, copper and gold now lie in veins (the Nether's gold too), with tin once a mod uses it; their
  scattered placement stands aside while veins are on.
- Prospecting: an instrument learns which way the veins near it lie and how strongly each shows, never where,
  and standing in one, what it is made of and how large it is.
- The world's vein settings: veins on or off, how often, how large and how dense each kind is, the materials
  whose ore is never laid, and trying to move other mods' scattered ores into veins of their own.
- Reservoirs of oil, gas and brines as things in the ground: a lens of soaked rock under a cap rock, of a kind
  declared as data (what it holds, its rocks, how much a block holds, where, how often and how large). A well drains
  it a block at a time, a drained block turns back into plain rock, and nothing floods; what was drawn is kept in the
  world's data over areas, so every mod reads the same reservoirs.
- The biome modifier `jscore:everywhere` adds features to every biome of every dimension.
- Structures that stand where the land suits them: of one template or a layout of many joined by jigsaws,
  spread as often as they say, none near the world's origin and none within reach of another structure of any mod, or
  at fixed places only; refused on ground too steep, too low or high, or under water; sunk as deep as they say, on
  foundations reaching down to the ground, and laid out like the game's villages, streets holding their houses.
- Structures worn by rule as they are placed: iron rusts, glass cracks, as often as each rule and the
  structure's age say, keeping each block's facing, the same each time.
- Settlements that grow: the whole layout is planned as it generates and built a stage at a time as the
  world's days pass, into the chunks that are loaded, a chunk loaded later catching up.
- Placing a structure where a mod says, as the world would have, and kept by the world afterwards: found by
  instruments, remembering its state, growing if it is a settlement.
- What each structure remembers of itself, by any place inside it, each change heard on the game's bus; and
  seals that open a chamber only to a team that knows what they ask.
- Loot of another mod's item that gives it only while that mod is installed.
- Landmarks a player discovers by coming near, for themselves and their team, with a notification that points
  at them, declared in code or by data packs.
- Finding the nearest structure, biome, landmark or vein with a precision: roughly at first, within 512 blocks,
  then each level twice as close, never further off than it says and the same each time; structures at fixed places
  included.
- Markers on the maps, each player's own and their team's, in layers, the landmarks discovered among them,
  shown on JourneyMap, Xaero's Minimap and World Map, FTB Chunks, or the series' own maps.
- Generation laid into the chunks made before it, a few each tick once their neighbours are loaded: a mod
  added to a world lays its ores into the land already explored. Each chunk remembers what it has had.
- The surface changing by itself as the climate and the weather say (ice thawing where it is warm, water
  gathering in basins in the rain), each change declared by its mod or a data pack; and a place's biome changed, its
  colours and its climate with it.
- Large edits planned away from the server's thread from a snapshot of the world, and set a share of each tick:
  a crater or a levelled city never stalls the server.
- World events by schedule or by chance (storms, showers, quakes): warned of to those carrying their
  instrument, told to everyone they cover, their effects played in sync, kept through restarts, started, put off and
  stopped by operators with `/jstech event`, and switched off, made rarer or made harmless by the server's settings.
- Fields over the chunks that spread and fade (fallout, pollution), worked out away from the tick and sent to
  the players near them.
- The Space Persistor, the series' chunk loader: it keeps an area round it loaded and working for its owner,
  nearest chunk first. Its screen is a map of the land round it, the chunks it keeps outlined and another owner's
  hatched, a click adding a chunk or taking it off, zoomed from 16 to 128 chunks across; keys for a square, a circle, a
  line or only the chunks clicked, drums for the radius, an LCD of what it keeps, its owner's count and the server's,
  and a plate to name it. Each chunk costs power (200 W of J's Energy by default, set by the server, or nothing at
  all, when it has no port); short of power, switched off or broken, it lets every chunk go.
- A server's budget of loaded chunks: every owner together keeps at most 400 by default, besides each owner's
  own number; `/jstech chunks` counts them, and `/jstech chunks map` shows an operator every place keeping chunks,
  and whose it is, on their own map. The chunks a machine keeps are an area on its owner's team's maps. Each place
  says why it keeps its chunks, and `/jstech chunks` lists a player's places under their count: how many chunks
  each keeps, where it is, and why.
- Each dimension's own day and year: a dimension's rules can give it a day of any length, a world locked to its
  star with its sun held still, and a year of its own days, its seasons a quarter of it and its world going round its
  orbit; the overworld's day is never touched. Its wind can blow harder or softer than the overworld's.
- The sky kit: a dimension's sky drawn from a data file, with suns coloured and sized by their kind of star and
  dimmer the farther they are; planets and moons lit by the real geometry, so their phases and eclipses come of where
  they stand, their surfaces turning and their rings leaning; square stars, the band of the galaxy, nebulae and an
  aurora; the colour of its sky, fog and sunsets, its own clouds and haze. The day's light dims with the sun's
  distance and in an eclipse, and takes the colour of its suns.
- The weather kit: kinds of weather any mod declares, or a data pack writes, brought by world events: rain of any
  liquid, snow of any ice and dust storms in their colour, fog that closes the view in, wind that blows harder, harm
  and effects on what lives out in them, and lightning. A server that makes an event harmless makes its weather
  harmless too.
- Machine screens of their own: a mod's machine whose screen is unlike every other's opens its own on the same
  menu, keeping the tabs, plates, lamps and Protection tab of every machine; a machine that holds no items leaves the
  inventory off its Main tab, and one whose energy a setting turns off offers no port.
- A dimension's air by height: its rules can say what its air is made of, breathed where the oxygen's own
  pressure is enough, and how its air, pressure and temperature change with height in bands, so a land can burn at
  the ground and be cool and breathable high in its clouds; a player is weighed by the air at their eyes.
- Laws of their own: a place, a dimension or a body can keep laws of physics of its own. Gravity scaled,
  turned along a direction or pulling to a point, felt by players, mobs, items, falling blocks and arrows alike; time
  running slower or faster for what lives there and for the series' machines; light tinted, shifted towards red or
  blue, shimmering, darkened or glowing; space that wraps round, shortcuts, small doors into large rooms of another
  dimension, and portals with no edge that show their far side; explosions that go out slowly and liquids that rise. A
  mass curves space round it, pulling things in, slowing time and bending the view round a black horizon.
- Sealed rooms: closed spaces are found where a block asks for them and worked out again whenever a block near
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
- The Atmosphere Detector, a block of the series' shared content: a pressure gauge, each gas with its share and
  its own pressure, the room drawn with its leaks blinking, four lamps, and a key that shows the room in the world.
- Carbon dioxide joins the shared catalogue, a gas every body and fire breathes out.
- How sound travels: every sound of the world, the game's own and other mods' included, echoes in the place
  the player stands in by its size and its walls, a sealed room by its exact size; walls muffle it by what they are
  made of, wool little and metal much, and the air takes its high notes away with distance. Through a vacuum only
  what the player touches is heard; under water everything is dull; thin air carries sounds weaker. A far sound
  arrives as late as the speed of sound brings it, and what moves changes pitch as it passes. A sound can say it
  matters, to be shown on screen with where it comes from, and what it becomes heard from far away. The walls between
  a sound and the player are weighed away from the game's thread and remembered for a moment by where they stand, so
  a place full of sounds costs the frame nothing.
- Resource packs give blocks their sounds by material (steps, blows, breaking, placing, falls, and how much
  they drink and let through), a blow on a living thing its sound by the kind of thing struck, the ambience of places
  by biome, weather, time, depth, roof and what is near, and music in layers that come in and go out with the moment
  while the game's own music waits.
- Ringing ears after a blast close by, unless something worn protects them; radio and intercom bands for
  sound played through devices, a helmet's radio heard even through a vacuum; and, with Simple Voice Chat, voices
  muffled by walls and silent through a vacuum like every other sound.
- Sound made visible: the debug screen tells the place's echo, its walls and the air sound goes through, and a
  key draws the rays the place is heard by and a line to every sound playing. The Sound Mixer's Options tab adds
  Echoes and Distance and Ringing Ears.
- Travelling between dimensions with what one rides: a vehicle and everyone in it go together and arrive at a
  safe place near where they were sent, on firm ground with room above, a platform laid where there is no ground;
  a mod shows the passage through a screen of its own instead of the game's "loading terrain".
- Hazards: one kit for whatever harms a body from where it is. Kinds of harm every mod shares (radiation,
  electrical, cryogenic, acid, heat, blast, pressure, dust, toxic, glare), any damage of the game or of a mod made one
  of them by a tag; what worn things and each kind of creature stop of each, as data, adding up by kind and cutting
  the damage as it lands, shown in the tooltip. A hazard builds up in a body from its sources, cut by what is worn
  against it, harms in bands, goes away away from its sources, faster with a medicine or a shower a mod gives it,
  kills with a death message of its own, and creatures
  suffer the kinds that harm them. The dimensions' bad air, heat and cold are kinds of it. Cold burns and crushing
  pressure are damage of their own, each with its death message, and radiation sickness is radiation damage.
- Hazards of carried things, as data: a hot ingot burns the hand, sodium goes off in the rain, a pyrophoric
  powder catches fire, an unstable charge goes off in a fall, a toxic or corrosive thing harms its carrier and a
  blinding one blinds, each named in the tooltip with what protects from it.
- The lungs: coal dust, silica and asbestos breathed, breaking their blocks or near a machine, build up black
  lung, silicosis and asbestosis, a lasting harm that heals slowly or hardly at all, from a cough to the lungs
  failing; masks and respirators stop their share.
- The hazard devices, items of the series' shared content in yellow cases with black faces: the Geiger counter,
  its needle on a scale of tens, its range and a lamp for every click, clicking faster as the rate climbs and named in
  the subtitles; the pocket dosimeter, the dose taken and its band; and the hazard map, the land round the player with
  a hazard on each chunk measured. Carried, each shows on the HUD in the corner and at the size the player chose;
  used, it is held up full size, or the map opens whole. The body's screen, on its own key, a worn panel of green
  enamel with the dose, the rate here, a shower lamp, the lungs, dissonance and the other hazards.
- Maps of hazards: an instrument carried reads its hazard at each chunk onto the player's map, kept through
  death, and a mod's sensor adds its own readings; what was never read stays grey.
- The land withering: where radiation or pollution is high grass dies into withered grass, plants wither to
  dead bushes and crops stop growing, and they come back slowly where it falls.
- Hazard settings: radiation can be turned off as a whole, and so can creatures suffering hazards, the lungs,
  the hazards of carried things and the land withering.
- Radiation: a family of hazards any mod declares kinds of, given off by blocks, weaker with the square of the
  distance and weakened by every block on its way by the block's density, from the data map of block densities the
  Core fills for the game's blocks; what is everywhere in a place and what a body carries add to it.
- Ionizing radiation, in real units: a dose rate in millisieverts an hour and a dose in sieverts, from the
  sources round a body through the blocks between, so lead stops nearly all and stone about half, worn shielding
  taking its share; and contamination, radioactive dust on the skin in kilobecquerels that keeps dosing until water or
  the rain washes it off, which a suit keeps from settling. The bands of radiation sickness, Rad I to V, from nausea to
  close to deadly; slow recovery away from the sources and under the background. The sky gives off a little, more as
  the air thins and the most with none, roofs stopping it; minerals with uranium or thorium in them give off a little
  more; a mod sets a dimension's radiation belts. The Geiger counter, the dosimeter, the hazard map and the body's
  screen read it, and the land withers where it is high.
- Dissonance: where the laws of physics depart from ours (time at another pace, a pull that is not down, light
  that bends, space that wraps) it builds up in a body, from warped sight through drifting gravity, stuttering time
  and jumps through space to coming apart, and falls back in normal laws; a bubble of normal laws a mod gives keeps it
  off. It has a setting of its own. In its first band the colours of the world shift, and in its second what the
  player hears goes off pitch.
- Pollution: kinds any mod declares, an amount over each chunk given off by blocks, which spreads, fades, is
  taken up where plants grow, harms the bodies on the chunk as a hazard of its own, does what it does to the land, and
  withers it under enough; the hazard map device measures them.
- The series' pollution: soot hangs as a haze and is breathed as smog, a cough that a mask stops; sulfur and
  nitrogen oxides fall as acid rain that kills leaves, weathers copper and calcite and stunts crops; heavy metals foul
  the crops and fish of their ground, which poison whoever eats them until the soil is dug out; radioactive fallout
  raises the dose rate and settles on whoever stands in it; and the world's carbon dioxide is one number that rises by
  what is burnt, shown on the map of hazards. Pollution has a setting of its own.
- Explosions: blasts decided by their energy, in joules and its TNT equivalent, with no radius of their own.
  The shock wave goes out at the speed of sound of the air or the water and reaches each place in its own time; it
  breaks each block by what that block stands (a window gives at a few kilopascals, concrete holds hundreds), and a
  wall it cannot break shields what is behind it. Bodies take the overpressure where they stand, hurt and thrown by
  it, and catch fire in the fireball; a casing breaks into fragments of its own material that strike as shrapnel;
  earth and stone fly as debris; explosives it breaks go off; the ground round the crater is scorched. A vacuum
  carries no wave, and water carries it far. The breaking is spread over ticks within a budget, claimed land is
  respected, and what each mod's explosions do to the world is a setting of the server's. The game's own explosions
  show the fireball, the smoke and the scorch marks too.
- Fire: the game's fire spreads, and eats what burns, by the oxygen of the air (never with too little, an
  inferno in air half oxygen) and by the wind (faster downwind, slower upwind, leaping ahead in a gale), and gives off
  smoke that rises and drifts. What burned stays burned, as a data map says. A fire's class is the worst of what feeds
  it (solids, flammable liquids, cooking oils, electrical, metals), and each agent does on each class what the real
  one does: water shocks whoever sprays live equipment, spreads burning oil and makes a burning metal burst. Sensors
  read the heat and the smoke where they hang. It has settings of its own.
- The series' fire safety: extinguishers of water, foam, carbon dioxide and dry powder, their bands in the
  colours of the European code; a sprinkler fed by a water pipe whose bulb bursts in the heat of a fire; smoke and heat
  detectors that call and give redstone; and charred logs, charred planks and ash where wood and cloth burnt. The
  series' fires give off soot.
- Blocks laid along an axis as logs are, and blocks whose every state picks its own model, for any mod's
  content; and a texture a pack draws as the game loads may be named by a mod's own models.
- Moving structures: structures of real blocks that move through the world and go on working as they move,
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
  insulators it hangs from. It has commands and a setting of its own. A structure can also be born whole from a
  template, as a rocket is put together in its hangar, its blocks laid straight into its plot without ever standing
  in the world; and one beyond the player's render distance is not drawn.
- Moving structures by their physics: a structure let go moves as a body of real mass, its mass, centre and
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
  which a structure waits a tick. Structures far apart from one another work out their physics side by side, away
  from the server's thread, and only what they do to the world is done on it.
- Rooms aboard moving structures: a sealed room aboard has round it the air of where its structure is, taken
  again each second, so a cabin climbs into thin air and a sealed balloon swells until its walls burst; a hole in a
  room aboard pulls whoever is in it towards the hole as its air rushes out.
- Joints between moving structures, and to the world: ropes and chains that sag with their slack, springs,
  tow bars, hinges and sliders that turn and slide within limits and can be driven, ball joints and welds, each
  holding as a real one does and breaking past the load it is given, a winch reeling a rope in and out by its signals.
  Structures joined move by their physics together, and every player sees the ropes, chains, springs and bars drawn
  between their ends.
- Structures held by structures: a turret on a ship, a cab on a crane, turned on its parent by a rule of its
  own and going wherever the parent goes; a parent moving by its physics carries what it holds as part of its body,
  its mass, its engines and its wheels its own, and lets it fall when the block holding it breaks.
- Structures parting and joining: one whose middle breaks out becomes as many structures as it now is, each
  going its own way at the speed it had, unless the setting `moving_structures.split` holds them together; a decoupler
  parts a rocket's stages at a signal and pushes them apart; two structures square to each other join for good.
- Docking: docking clamps meeting face to face at a signal take hold of the other structure where it is and draw
  it in square over a second or so, however the two go, their momentum shared; at the signal's end they let go, the
  visitor pushed gently away.
- Tools on the move: a drill mines, a harvester takes ripe crops and plants them again (a melon or a pumpkin
  whole, sugar cane, cactus and bamboo above their foot), a plough tills and a placer sets the blocks it carries,
  each as the structure passes, into and out of the containers aboard. Seats, helms, clamps, decouplers and tools are
  the data map `jscore:block_aboard`.
- Living aboard: a player sits on a seat aboard and is carried with it, steady in their seat; at a helm they
  steer the structure with the keys they walk with, its engines and wheels answering by the way each looks; an
  autopilot flies a structure to a place, at a height, along a route or to a docking clamp, any structure alike, by
  how fast its engines can speed it up; an arrow loosed from a moving deck flies on with it; a bed aboard is slept
  in as one on the ground, its sleeper carried and drawn along it however the structure is turned, and is the place
  its sleeper comes back to; and a player takes hold of a rope and rides it down.
- What a moving structure shows going: its engines' flames, a rocket's or a jet's, as big as they push; its
  faces glowing hot from the air they head into, in the colour of their heat, wrapped in plasma on a re-entry; and
  white trails behind its engines and wingtips where the air is cold enough. An engine pushing keeps its structure
  awake, so a rocket too heavy for its engines stands burning on its pad.
- The diagram of forces: a key, K, opens for the moving structure the player stands on, sits in or looks at
  the structure itself over the ground below it, with an arrow for each force where it acts and for their sum at its
  centre of mass, its height and its axes; each force listed with what it gives and a switch that hides its arrow; and
  its mass, the sum's way, its climb, its speed and the air round it, seen in 3D, from the side or from above.
- Physics seen: a block's tooltip shows its mass and the forces it gives while the diagram's key is held, or
  while an item of `#jscore:sees_physics` is held or worn; another key shows in the world the box, centre of mass and
  forces of every structure near.
- The physics tool: it grabs a moving structure where it is aimed and holds it before the player, pushes it
  with a blow, or freezes it where it stands and lets it go, as strong as it is set, its mode and strength changed with
  the wheel; its panel shows both, and while it holds a structure a beam runs to the point it holds. It is held with
  its claws ahead and its grip in the hand, in either hand, seen from the eye or from outside. It is made at a
  crafting table while the setting `moving_structures.physics_tool_recipe` says so, by
  `jscore:crafting_shaped_switch`, a shaped recipe a setting turns on and off that any mod can use.
- Every force a setting: gravity, drag, lift, thrust, buoyancy with the balloons, levitation, magnetism and the
  wind on moving structures each have a setting that turns them off, and the harm of blows one of its own.
- Moving structures by hand and by ear: `MovingStructures.push`, `freeze`, `hold` and `letGo` move a structure
  as the physics tool does, and a `MovingStructureEvent` on the game's bus tells of a hard blow, a parting, a docking
  and a letting go.
- Reading a flight: `StructureReadings` gives what a sensor aboard reads, its altitude, the air's pressure, its
  speed, climb, pitch, roll and heading, and how far the ground is below it and the first block ahead, as readings a
  sensor speaks in redstone and to programs.
- Propellers on shafts: a propeller's thrust can follow a shaft, `"shaft"` in its physics, growing with the
  speed the shaft turns it.
- Aboard as on the ground: what a block aboard asks of the air, the weather, the wind and the biome's warmth is
  answered for where its structure is in the world; a spotlight aboard shines on the world from where it is, sweeping
  the ground as the structure goes; a radio aboard is heard from where the structure carries it; and belts, spaces of
  parts and shafts work aboard, in another dimension too.
- Moving structures beside Create and Create Aeronautics: with Create installed, a moving structure meets its
  contraptions as obstacles that move, and lands and rides on one turning on a bearing; with Sable, which Create
  Aeronautics flies, it meets its ships the same way, as fast as they fly. Create moves no block a moving structure
  keeps, so a bearing or a ship's assembler never takes one out of it; a moving structure is never made of a ship's
  blocks; and a place aboard a ship is asked of the world where the ship has it. The work starts only on the versions
  of Create and Sable it is tested against, and says in the log when it is left off. Another mod tells the kit of its
  own moving structures through `ForeignMovers`.
- Vehicles as data: a vehicle is a kind, its seats, the places of its parts, its boot, its cargo tanks, its
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
- Robots that fly, swim and think: a mod's robot walks, flies in three dimensions or swims, carries as much as
  its size lets it, and digs with whatever tool it carries, of any mod, at a player's pace, the tool wearing and its
  enchantments working on what drops. One with a battery spends it getting about and digging, stands still when it is
  empty, and recharges at a charging station (`RobotCharger`, for a mod's block). Its tasks repeat, choose by a
  condition, wait for one or for a time of day, and find the nearest block of a kind to go to or break; its conditions
  sense what it carries, blocks, items and creatures near, the time and its charge. `Robots` finds robots by their id
  and their owner, gives them a program written as data and says how they stand, and the end of each task is told on
  the Core's events, for J's Computers to program them.
- Creatures as data: a mod's `CreatureEntity` lives as its data says, its health, speed and attack, what it
  does in order of what matters most (wandering, panicking, avoiding, following food, fighting), where it appears by
  biome, dimension and placement, and the air it breathes: it appears only where that air suits it, and chokes out
  of it. A data pack changes how a creature lives with no code.
- Projectiles by real ballistics: a projectile falls by the pull of the dimension it flies in; one with a mass
  is held back by the air's density where it flies, none in a vacuum, and by water, and is carried by the wind; one
  that gets through blocks passes a block as hard as it can still get through, slowed by it.
- Modules in worn things: a worn item with sockets takes modules, each into a socket of its kind, and each
  module on does what it does for its wearer (changes an attribute, gives an effect), spending the energy the item
  holds; hosts, modules and their effects are declared in code or by data packs.
- Tools that mine an area or a vein: a tool declares, mode by mode, an area of the face it breaks into (three by
  three, five by five, with depth) or the vein of the block it breaks, the same block or the same ore; the rest breaks
  with it as the player would break each block, its drops, the tool's wear and every protection's say included,
  leaving what the tool is not made for, what is far harder than the block broken and what the player may not break.
  The server sets the most blocks broken in one blow and how much harder they may be.
- Tools on energy, fuel or gas: a tool spends the energy it holds, a fuel it burns from the fluid it holds, or
  a gas from its tank, each block and each use; out of it, it does no more than a bare hand. A tool that heats
  overheats and cools down by the world's time, one held to charge acts let go with how charged it was
  (`ChargingItem`), and one that waits between uses shows the wait over its icon.
- Building tools: a tool lays copies of a block out along a line or over a plane from the face it is used on,
  copies the region between two corners and pastes it turned as the player has turned, every block from the player's
  inventory or from a supply a mod adds (`IBuildSupply`, for J's Computers' network), placed where the player may
  place it.
- Modules in tools: a tool whose modules work while it is held takes them as worn things do, and they make it
  mine an area or a vein, work as though enchanted (silk touch, fortune), or change the reach and speed of the hand
  holding it, spending its energy each use.
- What a tool shows: holding the mode key opens a ring of the item's modes, chosen with the mouse; the item's
  name above the hotbar says its mode and what it works on; bars under the crosshair show a tool's charge and heat;
  an icon carries a bar for each thing the item holds and its mode's letter; and in the world a tool outlines every
  block its area or vein will take, and a building tool shows the ghosts of the blocks it will place.
- The camera: lenses that magnify by real powers (`ZoomItem`, or a block through `CameraZoomClient`), the
  mouse wheel stepping between them and the mouse turning as much slower; the view turning with a ridden vehicle and
  rolling with it or with a moving structure's seat; free look, the view turned and the player not; scenes along
  camera paths declared in code or written by resource packs, the view of an entity and the view from a point, which
  the server starts and the player skips by sneaking, their ending told on the Core's events; and cameras in the
  world whose view a screen shows (`CameraFeeds`), drawn before the frame at a rate, a width and a number at once the
  player sets.
- Shakes and leans of the view, one service for every mod (`CameraMotion`): shakes from any number of mods add
  up, each wandering smoothly and dying away, felt less the farther from where it comes, the player's shake setting
  scaling them and less movement stilling them; a lean holds the view turned as its owner asks, a cockpit banking or
  a recoil, and eases it back as fast as it can without swinging past. A blast's shake goes through it.
- Riders lean with what they ride: each sits where its seat is on a leaning vehicle, on the server and on every
  player's game, and an aircraft's pilot tilts with its bank as other players see them.
- A mod's camera in the third person (`ThirdPersonCameras`): how far back it stands, over which shoulder and
  round which place, the middle of a long vehicle; brought nearer by walls as the game's own is, eased from one view
  to another, the highest priority first and the game's own when none gives a view.
- The studio's Cameras page: every camera path, and a new one named in place; fly where the camera goes and
  keep each place with the Keep a camera keyframe key (K), or add one where you stand; change each keyframe's time,
  field of view, roll and easing in its table; see the path from above, numbered along the way the camera takes;
  play it as a scene on your game alone before it is saved, and save it to your mod's file.
- Motion: springs among the curves (`spring`, `spring-soft`, `spring(damping)`), which go past their end and
  settle there, as phones move things.
- Items: an item holds a gas, in a tank of a volume at a pressure the Core's gas capability reaches; a key works
  an item wherever its player carries it, in a hand, worn or in the inventory, and turns one on and off
  (`ItemActions`); an item lasts as many uses as declared, or as many as its mod makes of a property of its material;
  the mouse wheel turned while sneaking changes the mode of the item in hand.
- Tooltips: each mod's lines kept together, the item's own mod first and the others by name, and the item's mod
  named last in blue unless another mod already names it (`ItemTooltips`); parts shown only with Shift (what an item
  does) and with Ctrl (its numbers), a hint at the foot saying which key shows what; bars of energy, gas, wear and a
  fluid in its own texture (`TooltipBar`), pictures (`TooltipPicture`), turning 3D previews of a block or a structure
  (`TooltipPreview`) and what an item holds in the slots of the game's bundle (`TooltipContents`). Items that hold
  things show their bars and slots, and a worn item its wear, in every tooltip; the player can turn the mod's name and
  the wear off in `jstech-tooltips.toml`.
- The keyboard, a controller and the narrator on screens declared in files: Tab and the arrows move among the
  components in the order they read, Enter and Space press, a controller moves among them as among the game's
  buttons, and the game's narrator reads each in the game's own words; a file names a component for the narrator with
  `narration` (`KeyboardFocus`, `UiComponent.narration`).
- Less movement for a player who asks, in the Core's own settings (`jstech-accessibility.toml`): what slides,
  turns round or blinks on the screens of the series is drawn still, and a tooltip's 3D preview stands still.
- The HUD editor: every mod's HUD elements in dashed boxes over the dimmed game, dragged where the player wants
  them and settling against each other or on a grid, resized by the chosen box's corners and hidden by its eye; Reset
  all, Cancel and Done. Opened from the settings screen or by its key; what the player did is kept in
  `jstech-hud.toml`, and an element placed leaves its corner's stack and stays by the corner nearest it.
- Screens in the world that answer the crosshair: a block declares which face shows a screen, where on it and
  its pixels (`ScreenTouches`, `ITouchScreen`); a screen is built of the Core's components (`TouchPanel`), the one
  under the crosshair lit as the mouse lights it, and while the crosshair rests on a button the action bar says what a
  right click does ("Right click: Start"); a right click presses the screen there on the server, by the pixel and by
  the name of the part pressed (`TouchPress`).
- The size of the text apart from the GUI scale, in the Core's own settings (`jstech-accessibility.toml`, Text
  size: Normal, Larger, Largest): each step draws a letter's pixels a screen pixel larger, so it stays sharp. The
  screens declared in files, the settings screen and the notifications grow with their text, while the icons, frames,
  slots and the game's buttons keep the GUI scale (`TextScale`). A text bound to a value that grows past its box lays
  its screen out again instead of running past the window's edge.
- Screens declared in files draw less: their window and what never changes are drawn once into a picture laid
  down each frame, and only what moves is drawn over it (`UiComponent.still`, `StillLayer`).
- Tools for building screens declared in files, in a development run: F12 opens the inspector, with the tree
  of what the screen is built of, the problems found (a text with no translation, no words for the narrator, an
  action nobody handed over, overlaps) and a card of the component under the pointer with its file and line, which
  Ctrl+C copies; it draws the screen at another GUI scale, in the other language and in each state its code offers
  (`ScreenWiring.preview`). Ctrl+E opens the editor on the screen, which moves, resizes, adds, copies and takes out
  components and writes each change to the mod's source at once, with Undo and Redo; `/jstech screen new` makes the
  file of a new screen for the block looked at, from a machine's screen, a tab, a HUD element or a copy. While a
  component is dragged, lines show where its edges and middle meet the others' and the window's, and it snaps to
  them; its card holds the fields its kind has and no others.
- Manuals: scenes that explain step by step: what each step puts in and takes away is data (`step`, `take`),
  its sentence stands over the picture, and a timeline under it has the step before, play and pause, the next step
  and a mark for each step. Every scene has an In world button that shows the step, or the whole scene, as ghosts in
  front of the reader through the Structure Projector.
- Manuals: a building saved with the game's structure block is a scene of a page, read from the mod's assets
  (`structure`), and a picture can move, its frames shown in turn as an animated texture's (`animation`).
- Manuals: bookmarks and notes: ribbons of three colours hang over the pages they mark, listed under their
  button beside the search with "+ Mark this page"; a right click on a page writes a note in its margin, on yellow
  paper. They are the player's own, kept by where their page stands in its entry, so they stay on their words in
  another language.
- Manuals: the contents say "NEW" beside what came into the book since the player last read it and "not yet"
  beside what their team cannot do yet (`needs`), and a chapter's tab holding something new has a gold dot.
- Manuals: the search finds the recipes the pages show, those making what was typed and those it goes into,
  each with what it makes and what works it. With JEI or EMI installed, the manual key held over an item in the
  viewer's lists opens its page, and a click on a recipe in a page opens it in the viewer, which comes back to the
  page when it is closed.
- Manuals: in a development run, a manual's file saved is read again at once, alone, and the manual open lays
  itself out anew with its reader on the page they were reading. `/jstech manual site <manual>` writes a manual as a
  website of plain files, its front page with the contents and a search that runs with no server, a page an entry,
  its recipes in words, in the player's language (`GuideSite`).
- Manuals: a page torn out as a Torn Sheet, a copy the manual keeps, from a button on the binder's top edge; in
  an item frame the sheet shows its page as a map is shown, and its tooltip says where it was torn from.
- Manuals: every mod's manuals go on the game's lectern as its books do, and are read from it with a strip
  under the binder saying where the lectern is, with Take and Close.
- Manuals: the first time a player picks up an item a manual explains, a quiet notification offers its page,
  once an item; the player's own settings (`jstech-manuals.toml`) turn it off, or on in creative too.
- Manuals: the Technical Reference has a section on things that move, with the pages of the vehicles and
  their parts, and of the physics tool.
- Content packs: blocks, items, materials with their forms and ores, kinds of processing machine and the
  controllers of machines declared as data, written in JSON files under `jstech/content/` in the game's folder or
  `jstech_content/` in a mod's, and registered as the game starts with no Java, each namespace with its creative tab.
  The Core makes their English names, block states, models, drops and tool tags as the game loads, under every pack,
  so a resource pack or a data pack can replace any of them; a file that cannot stand stops the game loading with a
  paragraph saying which file, which declaration and how to put it right.
- Conditions on data: `jscore:setting` loads a recipe, a loot table or any data NeoForge loads with conditions
  only while a setting has the value asked for (a switch, a number, a word or a list), reading a world's setting from
  the world's own file from its very first load.
- Crafting recipes that ask for knowledge: `jscore:crafting_shaped_knowledge` and
  `jscore:crafting_shapeless_knowledge` craft at the crafting table and the inventory's grid only for a player whose
  team knows what they ask, never where no player is at the grid, and show in JEI, EMI and the manuals with a
  padlock while the team does not know it.
- Unification: when several mods make the same material, one item stands for all of them, the game's own first
  and the Core's next by default, in every recipe of every mod and in what blocks, creatures and chests drop; the
  world's `jstech-unification.toml` says which mods come first and which tags, items and recipes are left alone, and
  it steps aside for Almost Unified. Only copies are made one: a mod with two items of its own in a tag (AE2's certus
  quartz crystal and its charged one) means them apart, and neither is replaced.
- KubeJS: scripts reach the Core as `JsCore` (teams, what they know, milestones, steps along an axis,
  materials and their properties, which item stands for which, every kind of data the Core's registries read and
  every mod's data maps, as JSON), hear its events under `JsCoreEvents` (knowledge gained, milestones and steps
  reached, the life of Operations, machines joining and leaving data networks, sealed rooms, moving structures, and
  a dimension's hazard, which a script cancels), and write a recipe of any mod's processing machine with a function
  under the machine kind's id. The Core is made before KubeJS, so its bindings are there for startup scripts.
- Data networks: a machine joining or leaving one is told on the game's bus, `NetworkMemberEvent.Joined`
  and `NetworkMemberEvent.Left`.
- Reloading clearly: after `/reload` the server's operators and the log are told how many recipes and loot
  tables the data holds, what the unification made of them, and what waits for a restart: content packs changed on
  disk, and settings that take hold only then.
- `/jstech dump materials`, `forms`, `recipes`, `tags`, `hazards`, `ids`, `reference` and `all` write what
  the game holds to `jstech-dump/`, each as a page of Markdown.
- The generated reference: `/jstech dump reference` writes a website to put online with every kind of data
  the Core reads, every mod's data maps and every kind of recipe, each with a real example the game's own codecs
  wrote, the Core's conditions, what content packs declare, and every id.
- Ores in other mods' rocks: every material with an ore in the game's Overworld or Nether rocks has one in the
  rocks other mods bring to the same world, found by the common tags with no list kept by hand (Create's limestone,
  crimsite and the rest among them): as hard as the rock and a little harder, named after it ("Tin Ore in
  Limestone"), filed with the material's ores, dropping what they drop and laid by the veins. `jstech-variants.toml`
  turns it off, or leaves out mods, rocks and materials.
- The materials' art is drawn once and kept on disk under `jstech/cache/`, read back on the next load while
  nothing it is drawn from changed; an ore in another mod's rock is drawn on the texture the rock's own model names.
- Variants of other mods' materials: a mod declares a kind of variant once (`Variants.kind`), and one is made
  for every wood, stone or metal of every installed mod, found by the common tags (`minecraft:planks`, the world's
  stones, `c:storage_blocks` with their ingots): it copies its material's block, is named after it in the player's
  language ("Oak Frame"), is drawn with the kind's model and the material's own texture, drops itself and is in its
  tool's tag. `jstech-variants.toml` leaves out mods, materials and kinds, or all of them.
- Changing drops as data: the modifiers of loot `jscore:add_drops`, `jscore:remove_drops` and
  `jscore:swap_drops` add, take away and swap drops, with the game's conditions of loot (the table, the tool, the
  dimension, the biome) and NeoForge's on the file.
- Changing recipes as data: files under `jstech/recipe_changes/` in a data pack take recipes away (by id, all
  of a mod's, by what they make or by their kind), put one item for another in every recipe, and put a recipe in
  another's place, before the game reads them, so recipes of every mod and kind are reached; a file may carry
  NeoForge's conditions.
- Choosing between recipes: when more than one recipe fits a crafting grid or a furnace's input, a chooser
  stands beside the result, a click for the next, a right-click for the one before, its tooltip naming each; the
  choice is kept with the player for the grid and with the furnace for what it smelts. The world's
  `jstech-recipe-choice.toml` turns it off, and it steps aside for Polymorph.
- Stepping aside for other mods: the echoes of places and the muffling through walls step aside for Sound
  Physics Remastered and Dynamic Surroundings, the soundscapes of places for Dynamic Surroundings and AmbientSounds,
  footsteps by material for Presence Footsteps, and the glow of bright things for Shimmer with coloured light, each
  with its switch in `jstech-step-aside.toml`; no sound in a vacuum, the speed of sound and the hum of machines stay.
  The log says, as the game finishes loading, which system steps aside for which mod, and LambDynamicLights is
  handed the light the series' items give held.
- Recipe viewers for every kind of recipe: a mod declares a kind once with `RecipeViews` (a type of the game's
  recipes, or any values it lists), lays out its page with slots, fluids, lines, sprites and an arrow, and it is shown
  in JEI, EMI and REI alike; the processing machines' pages are made the same way. REI now shows everything JEI and
  EMI do: the machines' recipes with the "+" that moves one into the open machine, the multiblocks with their
  previews, the microblocks' rules, and the areas screens keep clear. A viewer opened from a manual comes back to it
  when it closes, EMI's too.
- Inspecting a place: `/jstech inspect` says in chat what every system holds at the block looked at or at a
  place named (the block, its state, its block entity's fields as they are saved, what it offers on each face, its
  data network, the grids laid there, its sealed room and its multiblock), and each mod adds its part with
  `Inspectors`; `/jstech inspect watch` shows it on the debug screen, read again every second, with what the server
  sent the player in its last tick. `/jstech inspect payloads` lists every kind of packet of the last tick and of the
  busiest of the last 100, each with its size. `/jstech health` counts, in each dimension, the bodies of heat awake,
  the networks of shafts, the sealed rooms, the moving structures and the multiblocks watched; the heat, the shafts,
  the rooms and the moving structures are timed sections of work, and `/jstech cost` measures each moving structure.
- J's Core Test Kit: a mod of its own, `jstestkit`, that a mod built on the Core writes its tests with and
  depends on in its tests only, published to the local Maven repository for now. For GameTests: arenas made in code,
  a scene for every kit of the Core (the data network and its Operations, energy, machines and their recipes,
  multiblocks, sealed rooms, moving structures, hazards, menus), round trips through the save, ticks run at once and
  chance with a seed, assertions that say what was expected, what was found and where, tests of a tick's cost with a
  limit, old saves read back through the game's upgrades, optional mods seen absent, players who log in, and a report
  of each test's time and a stall monitor for long runs. For client tests: the runner, its helper, reference pictures
  and shards balanced by duration, which any mod registers its tests with. For unit tests: the test of the API's
  surface the series keeps its promise with, and the data generation kept up to date (`checkGeneratedData`).
- The Core's Gradle plugin, `dev.jstech.core`, published to the local Maven repository for now: applied
  beside ModDevGradle, it gives a mod the Core as a dependency, a `gametest` source set for its tests with the test
  kit, its runs (client, server, data, GameTests and client tests), the data generation into
  `src/generated/resources` with its check, the check of its API against its photo, and its licence headers checked
  and written (`checkLicenceHeaders`, `addLicenceHeaders`).
- Developer mode and the studio: the setting "Developer mode", the player's, and "Developer mode on this
  world", the world's, both on in a development run and off in a packed game, open the tools for whoever makes a mod
  or a modpack, and the tools that read or change the world are for the world's owner or the holders of
  `jscore.developer.tools`. `/jstech studio`, or its key, opens the studio: every thing of every mod on the Core and
  of every content pack by mod and kind, each with its problems and a search; the thing on a bench in a small world,
  walked round and brought near, a machine working, idle or jammed; what it is made of (its model, screen, effects,
  sounds, names, recipes, manual page, tags, drops, settings and GameTests), each opening its file; the checks across
  its parts (a name missing in a language, a file or a model missing, a texture that is not there, an animation
  moving a bone no model has, a screen file that does not load, an item in no creative tab, a loot table missing),
  each opening the file that mends it; Reload all, and Run its tests, which runs near the player the GameTests that
  name it. A mod adds a part of its own with `StudioReaders`. The checks of a screen file open the menu it is drawn
  over and say which of the menu's slots the file shows nowhere, and which slots the file shows that the menu does
  not have.
- The studio's tools: a strip of tabs under the studio's bar opens twelve tools beside the things of the game.
  Block reads a block entity's fields live, what each face offers, and what crossed each face each second, counted
  for a block entity of the Core while the tab watches it, at no cost to any other; Cost times every machine,
  dearest first, with a heat map in the world; Payloads shows the packets sent tick by tick, by kind and mod;
  Recipes says where each recipe came from and what a pack
  changed, draws it as the recipe viewers do, lists the recipes with the same inputs, and writes, tries and saves a
  recipe; Tags says which pack put each entry in a tag; Loot rolls a table up to ten thousand times with a tool,
  Fortune or Silk Touch; Translations shows what each language lacks and the texts too long for their place; Settings
  lists every setting with its default and range and exports them as the preset `pack`; Structures catches the
  blocks between two corners as a structure file, saves it to the mod and shows it as the Structure Projector shows a
  multiblock; Tests records a client test as you play and keeps it as a file, and shows every mod's reference
  pictures beside what the last run drew and the difference; Save saves a block and reads it back, field by field;
  Logs shows every log system's level and last lines, and the errors that teach. A mod's own tool asks the server
  through `StudioServices` and `StudioAnswers`.
- Recorded client tests: the files `assets/<mod>/jstech/client_tests/<name>.json`, which the studio writes, run
  as a suite of client tests of their mod with no code.
- The MCP server: the studio's tools open to agents and to tools outside the game over the Model Context
  Protocol, at `http://127.0.0.1:25599/mcp`. Its key, "MCP server", is never in a file: the player turns it on from
  the game, in the studio's new Agents tab or with `/jstech mcp on`, for that run only. It listens on this computer
  alone and refuses any request a web page sends or one that names another host; every client is let in by the
  player, who is asked on the screen whether it may read, or read and act, and is given a key of its own that dies
  with the game; reading and acting are apart; files are read and written only in the mods' and the pack's folders,
  and never code; commands come from a safe list, only in a test world or for an operator; what players wrote comes
  back marked as theirs; a mark on the screen shows a client is in, and what it does while it acts; any key takes the
  controls back and the key "Cut every agent off" (End) takes back every key; and every call is written down in the
  Agents tab. A dedicated server has one of its own on its computer, turned on by an operator in the game with
  `/jstech mcp server on` and asking the operators about each client. A mod adds tools of its own with `McpTools`.
- Tools to play the game for automated tests, on the MCP server of the player's game, through the input the
  player uses: seeing (where the player is and looks, what the crosshair is on, the inventory, the screen open and its
  components by name, a picture of the window, the blocks round the player, the chat and the game's log); acting
  (walking to a place, jumping, sneaking, looking at a block or an entity, picking a hotbar slot, using, placing,
  breaking, opening a block, clicking a component by name, typing, pressing a key, closing the screen); the test
  world (giving items, setting blocks, saving and opening the world again, running GameTests by name, playing a
  client test kept as a file with its pictures compared); waiting for ticks or until a screen, a block, a component's
  words or a line of chat; and recording what the agent did as a client test. A tool that acts over many ticks stops
  when the player presses a key.
- CraftTweaker: `mods.jscore.Processing` adds a recipe of any processing machine, `mods.jscore.Core` reaches
  the same as KubeJS's `JsCore`, and the Core's events are heard under `mods.jscore.event`.
- FTB Quests: quests have the Core's tasks `jscore:knowledge`, filled as a team learns a piece of knowledge,
  `jscore:milestone` and `jscore:axis_step`.
- J's Example, the Core's example mod: a mod built on the Core's API alone, to read as a tutorial. A generator
  that burns fuel into energy, as much a tick as its setting says; a battery; a crusher whose recipes are files of
  data; a tank of sixteen buckets filled by buckets and pipes, and oil with its block and bucket; an energy cable and a
  fluid pipe in the Core's cable block; a level sensor mounted on a cable, giving out how full the block beside it is
  as a redstone signal; a settings file; the energy made kept for the whole server and the logbook's openings kept for
  each player; and the logbook's screen, declared in a file with no code of its own. Its blocks wear the game's own
  textures.
- Holding a mod to the promise: the test kit's `ApiUse` reads a mod's compiled classes and fails when they reach
  a class, field or method of the series its photos do not promise, or anything of a mod it must never reach,
  following a call through the mod's own class to where it is declared and a method it overrides to the one it
  overrides. J's Example runs it on every build, against the Core's photo, and forbids J's Computers and J's
  Industrial.
- Manuals written in Markdown: a manual is a folder of Markdown files under `assets/<mod>/jstech/manuals/`, a
  folder for each language with English where a language lacks a file, with no Java. A short header gives an entry
  its section, order, icon and what it covers, the first heading its title; a note, a warning, a problem, a
  definition and a see-also are the alerts GitHub writes, and the manual's own blocks (recipes, a multiblock, a
  structure, a command, a table read from the code, a terminal session, a plan, views, drawings) are fenced blocks,
  any block also as its JSON. A resource pack replaces any file, a change of the game's language reads them again, and
  in a development run saving a file reads it again at once.
- A schema of screen files: the data run writes `assets/jscore/jstech/schemas/screen.json`, from the codecs the
  game reads screen files with, so an editor checks a screen file and offers its names as it is typed.
- A component moved in one call: `animate` grows, slides or fades a component in by the motion kit's curves,
  with nothing more to do each frame.
- Fonts from TrueType and OpenType files, drawn at a size into the game's font by the data run, with their
  licence and credit asked for as for the bitmap ones.
- Jade in the player's style: what Jade shows takes the ground, the edge and the text colours of the style the
  player picked, and goes back to Jade's own theme when no style is chosen.
- The settings of other mods: the Core's settings screen also shows the settings of every mod that keeps them
  NeoForge's way, under "Other mods" in each tab, with the controls their values call for, their names and
  descriptions as each mod gives them, and when they take hold; on a server, another mod's world settings are shown
  and not changed.
- Places kept through every move: a block entity of any mod that keeps the places of other blocks in what it
  saved, a beehive's flower too, has them follow the blocks they name as its structure is made, turned and set down;
  a mod whose block entities keep their places some other way declares a fixer for their type.
- Running structures by command: `/jstech structure freeze` holds one where it stands, in the air too,
  `unfreeze` lets it go again, `remove` takes it away, its blocks with it, and `export <name>` saves its blocks as a
  structure file the game places again.
- The studio's recorder of patterns: on its page of structures, a structure caught is made a multiblock in one
  step, its controller the block looked at; the page lists the letter of each kind of block with how many, its ports
  and its layers, it may be mirrored or lie down, and beside it is the page the recipe viewers will show; writing it
  makes the pattern's file, in the mod's source or in a data pack of the world's own, and the projector shows it where
  it was built.
- Pressure that bursts: a tank of gas or of supercritical fluid may say what it stands, and forced past it, by a
  compressor stronger than it, it bursts, its block gone, what it held let out, with a blast of the energy the gas
  held above the air around it; its relief valve lets a little out before then, and one too small for what is forced
  in does not save it. A pipe of gas may say the pressure it holds, carries nothing from a store holding more, and by
  its own rule bursts with the force of the gas it held.
- Redstone at each end of a line: an end of a pipe or a line of items works whatever reaches it, only while a
  redstone signal reaches its cable block, or only while none does, set by a key on the end's screen.
- A body's warmth: a player's core warms and cools in kelvin, as a real body's does, by the place (the air of
  the biome, the room or the dimension, the sun, the wind, the rain, the water they are in, hot and cold blocks near)
  through what they wear, each worn thing's insulation given as data, and by what they do. Below 35 °C comes
  hypothermia, mild, moderate and severe, and above 37.5 °C heat exhaustion and then heatstroke, each with its effects
  and death message; a body that stays in a cold or a hot climate gets used to it. The HUD shows the core while it is
  out of its normal band, and the screen's edge frosts or hazes; a setting turns it off.
- Shielded containers: what a shulker box or a bundle holds is carried, and its radiation reaches its carrier
  through the container's walls, which a container given lead walls five centimetres thick by data stops all but a
  thirtieth of.
- Cleaning pollution: a scrubber or a filter of any mod takes so much a second of one kind from the chunk it
  stands in, and keeps what it took.
- Worldgen modifiers: a datapack changes the world generation of the game and of any mod, in
  `data/<namespace>/jstech/worldgen_modifiers/`, as the server starts: a new density function in the place of one,
  wrapping the old one if it likes, and a structure set spaced out, closer, or holding other structures.
- Six more kinds of density function for terrain, in code and in files: terraces, ridges, powers, a value moved
  from one range to another, two shapes joined smoothly, and the distance from a point.
- Structures tuned by an operator: `jstech-structures.toml` turns any mod's structures off, spaces out its
  structure sets or draws them closer, and moves a structure to other biomes; `/jstech dump structures` lists every
  structure set with its placement and its structures.
- A field manual of the veins in the Technical Reference: how they lie, how to follow and find them, and the
  series' veins in a table.
- Large edits undone, saved and read back: an edit made undoable keeps what it replaces, a chest's contents and
  all, and is undone; a box of the world is saved as a structure file, and a structure file is set again as a large
  edit, turned; `/jstech edit export`, `import` and `undo` do it from the chat.
- Chunks kept while their owner is away, by a rule of the server's: always, as before, never, or for so many
  hours of its running, after which they wait, still the owner's, until the owner comes back.
- A test proves the Core's generation the same for the same seed on any number of threads: the test land's
  density worked out on one thread and on eight, and its chunks made all at once and one at a time, come out alike.
- Permissions by place: every permission of the series is asked about with the dimension the action is in and
  the claim it lies in, as NeoForge's contexts, so a permission mod grants a node in one dimension or one claim and
  not elsewhere.
- Who may do what, in the studio: its Access tab shows, for the block looked at, whether you, your team, a
  player you name and that player's machines may break, place, use, explode, burn, flow into, move a structure into
  and shoot there, with what each protection answered, the claim and the server's PvP rule.
- A page to read the diagnostic file: `/jstech diagnostics` writes an HTML page beside the file, each part
  folded under its title, the lines that warn marked, and a box that keeps only the lines holding what is typed.
- Typings for scripts: `/jstech dump typings` writes TypeScript declarations of every script binding of the
  series and of `JsCoreEvents`, so an editor completes KubeJS scripts as they are typed.
- Unification seen before it happens: the studio's Recipes tab shows what unification changes tag by tag, the
  item kept, those replaced and how many recipes name them, with one recipe before and after; with unification off,
  what turning it on would do, and a button that turns it on.
- The page of the MCP tools: the MCP server hands a browser on the same computer the page of its tools at
  `/tools`, each with what it does, whether it reads or acts and its arguments and schema, and `/jstech dump mcp`
  writes the page of every tool the game knows.
- The scene in numbers: the MCP tool `scene_describe` tells an agent what the player sees, read along lines of
  sight across the view: the kinds of block with the nearest of each and where it lies, the creatures, players and
  items in sight, the way the player faces, the time, weather, light and biome.
- Machines that fail safe: a source declared to fail safe, as the ones a player builds to forget are, is never
  harmed by a fault of its own. A line however high only keeps it from starting, heat past its limit stops it to cool
  without taking its health, and an arc flash neither burns it nor sets fire around it; an explosion from outside
  still damages it.
- Overvoltage, said: a machine that refuses a line above its class rather than burn says Overvoltage (HI LINE
  on its display), and starts again by itself once the line is gone.
- Manuals: a figure drawn more than one way, with a switch over it, a click on a word showing it that way: a
  diagram and the scene of its blocks, a section of the ground in lines and in colours.
- Manuals: a set of drawings numbers each section's drawings by the section's letters, as a real set numbers a
  discipline's sheets (JI-GS-001), and its drawing list heads each section's drawings with its title.
- Minerals: hematite, magnetite, siderite, pyrite, chalcopyrite, cuprite, malachite, electrum and cassiterite,
  each made of what its formula says, by its elements' atomic weights, its trace elements counted at a thousand times
  their real grade; a mineral's tooltip shows its formula with its counts small, CuFeS₂.
- The hazards of materials: every element and shared compound carries its hazard classes as a real safety data
  sheet gives them, and its things' tooltips list the GHS pictograms they call for (the gas cylinder only for a gas
  held under pressure) and the hazards beyond them, an asphyxiant gas and radioactivity; what a cloud of a gas does
  follows from the same.
- Every element's standard atomic weight, shown with Shift; and ammonia in the shared catalogue.
- Machines: a byproduct can come by count, never by chance, a quarter of one each recipe being one every
  fourth recipe exactly; a catalyst that wears loses a use each recipe and becomes its spent form.
- A player whose machine discovered an element beyond the known table while they were away is asked for its
  name when they join, and the right to name it waits for them.
- Multiblocks: entries where a multiblock says. A multiblock declares the places where its entries may stand,
  the blocks of its own through which something comes in or goes out, and the player puts each where it suits among
  them; each is counted, so a multiblock takes exactly one power entry or at most one inlet, each makes its place the
  port of its kind, and an inlet and its outlet can be made to go together, which Jade says when one is missing.
- Research: discoveries a team makes with research computers, each costing research and data and starting once
  the team knows what it comes after, known in part while its research goes and opening what asks for it once made.
  What each costs can be changed by a data pack, and a mod's setting can turn its research off, opening everything
  its discoveries lock.
- Pipes that wear: a mark of what a pipe carries may wear a pipe through instead of keeping it out, after so
  long carrying it, the wear kept with the world and forgotten when the pipe goes; lines of gas, slurry and plasma keep
  the marks as pipes of liquid do, so a corrosive gas stays out of a gas line not made for it.
- Gas clouds: a gas let out into the open is a cloud that spreads and thins until it is gone, kept with the
  world, or goes into a sealed room's air. A toxic or corrosive gas poisons by how far it is past what is immediately
  dangerous to life, which a gas mask stops; an asphyxiant takes the oxygen's place; a flammable gas between its
  explosive limits goes off from a flame, lava or a fire in it. The limits are real data of each gas: hydrogen
  explodes between 4% and 75% of the air, chlorine is dangerous from 10 ppm, ammonia from 300.
- Valves for any mod's pipes: a check valve lets through one way, others open by hand or by a redstone signal
  or let one fluid through alone, each counting its flow for a comparator; and a relief valve lets a vessel's excess
  out above its setting, into a discharge tank or the air.
- The pneumatic post for any mod: stations with an address on a line of tubes, sending what they are given to
  the address their routes give each item, pushing only, each capsule taking its trip's time and kept with the world
  on its way, unloading into the station it reaches and from it into the block that station faces.
- Air, in the shared catalogue: the mixture a compressor draws in, for every mod that compresses or separates
  it.
- Zoned veins: a mineral of a vein may lie only in its upper half, as an enriched zone, only in its lower
  half, or only in some biomes, in code or in the vein's data file.
- Modules by trait: a module may say how many of it one item takes and what the item must be, the piece of
  armour it is or a seal, with names the series' mods share, so one mod's module fits another's suit with neither
  knowing the other.
- Eras of other mods: an era another mod declares between the series' own is an era of hardware
  everywhere, its machines, its tables and its progress. Whatever a rule of the series says era by era, an era that
  says nothing takes the nearest earlier era's, as the Exa and the Singularity take the Advanced's.
- A manual leaves out the page of things the game has none of, such as what a mod brings with another mod that
  is not installed.
- A capsule station is read and set from outside as a machine is (its address, where it sends the rest, what
  waits in it), and can be told to send what waits to an address at once. Its pace counts the capsules that left, so
  a look that sent nothing never holds one back.
- Wide advancement backgrounds: a mod declares a wide picture for a tab's tile (`WideBackgrounds.declare`),
  drawn once behind the tree in place of the 16-pixel tiles and sliding at half the tree's speed as the player drags
  it, so it never repeats and the window never reaches its edge; a tab whose tile has none is tiled as before.
- Manuals dressed as a mission's documents: a caution in a box of its own colour beside the warning
  (`> [!CAUTION]` in Markdown, which drew a warning before); checklists, a box to tick, a number, what to look at
  and what it should be past a row of dots (`- [ ] Rocket ... on pad`), a line the revision changed marked by a bar
  in the margin (`[!]`); cards on rings as a crew wore on their cuff, numbered on from card to card (a `card` fence);
  and, for a style that asks, heads ruled into boxes with the manual and its edition or the section and its folio,
  notices under a bar of their colour, figures numbered short (Fig. 1-1), pages numbered by section after its
  letters (LO 2-3) and tabs one a section, lettered and coloured. A checklist, a card's title, a notice's word
  and the boxed heads are typed, in the style's code font. Every manual written before reads as it did.

### Changed
- The saw, the basic wrench, the four extinguishers, the network and signal probes, the Clamp Meter, the Geiger
  counter and the pocket dosimeter are drawn in three dimensions in the hand and on the ground, each held as its own
  is: the saw and the wrench as tools, the extinguishers and the Geiger counter carried by their handles, the
  instruments upright, their screens turned to the player. Their icons stay; the pocket dosimeter is the HUD's yellow
  device everywhere now, its icon too.
- A halo is the glare the eye sees: drawn whole over the picture, never cut by the floor a lamp stands on or
  the wall beside it, and fading, smoothly, only as much as the blocks between hide the light itself, glass hiding
  none of it. A block's glow follows its drawing: a lantern or a torch glows about its own shape and as big as it, and
  glow lichen glows in its own drawing with no round halo about it.
- A click on a machine's progress arrow opens the recipes it works in JEI, EMI or REI, as "Show recipes" says
  over it; the bar under the window takes the click when the lanes do not fit.
- An element's symbol shows alone at the head of its tooltip, in gold, rather than as "Symbol: Fe".
- Pipes sound as metal, laid, taken out, struck and walked on, rather than as wool; cables still sound as wool,
  and a mod may give its own cable or pipe another sound.
- A cable laid beside another shows joined to it the moment it appears, and a wire taken out lets go of its
  neighbours at once, rather than a tick or two later.
- A click anywhere on the front of a formed multiblock opens its screen, as a click on its terminal does.
- The Multiblock Builder puts in what a multiblock must have, each in a place that takes it, and never more
  than it takes, whatever block was chosen for those places, so what it builds forms.
- The tabs of every machine's screen have new icons, drawn as the game's items are: a block with its input and
  output faces for Sides, the redstone torch on its dust for Redstone, a module card for Upgrades and a brass
  padlock for Protection.
- With J's Industrial installed, its deposit types take the place of the Core's plain veins of
  iron, copper, gold and tin, and the game's ores of those metals lie in its deposits beside their minerals; the
  Nether's gold stays the Core's.
- The body's screen is declared in a file, so its look can change: a resource pack lays it out again and the
  style the player picks draws it, in the game's look by default. Beside what is worn and what it stops, it now holds
  how much heat the clothes let out and whether they keep the wind and the water out, and the core's warmth on a scale
  of its bands with what the place does to it; the dose, the lungs and dissonance each on a scale of their bands, a
  word asking for a shower while the body is contaminated.
- In a screen declared in a file, a part hidden by its `visible_if` gives its room to the rest, and the window
  takes the height of what it shows; the game's style draws a well in a lighter grey, so its text reads well.
- A steadier solver for moving structures: structures that may meet in a tick are worked as one, with more
  passes and shorter steps where they are stacked, and fall asleep together, so a stack of crates settles and sleeps
  instead of sinking and swaying; a contact starts from its whole push of the step before, so a great hull on the
  ground holds still; a fast body stops at the wall it meets and bounces as it touches, never before; a bounce comes
  only as bodies meet, never while they rest; the ways a contact grips along stay the same as it leans by a hair; and
  a structure asleep looks about without kicking what rests on it.
- Facades stand over any face of a space, not only where a cable runs.
- Mekanism's infusions and pigments are no longer carried by the Core's gas lines as if they were gases: each
  of Mekanism's four kinds of chemical finds its state, its gases and slurries their lines, and its infusions and
  pigments none, which Mekanism's own tubes still carry.
- Belts keep the run they walked: the end of a run walks its pieces again only once a piece is laid, broken
  or turned, so a long run costs its items and not a search of its pieces every tick. Measured, three thousand six
  hundred items riding two thousand belts cost the server about a millisecond a tick.
- The Core's chapter of the Technical Reference and the series' are written in Markdown, in English and
  Portuguese, in place of the Java that declared them.
- Vehicles: the one entity `jscore:vehicle` of a kind given as data takes the place of a class extending
  `VehicleEntity` with a `VehicleSpec` for each vehicle; `VehicleSpec` is gone. A robot still drives one with its
  `Drive` task.
- A robot's inventory is as big as the robot, and `RobotEntity.INVENTORY_SIZE` is gone; a robot as wide as a
  player and two thirds as tall still carries nine stacks. `ProjectileSpec` gains a mass, a drag area and what it gets
  through, its old constructor kept.
- A host of modules says whether its modules work worn or held (`ModuleHostDef.worn`, `ModuleHostDef.tool`, or
  `"held"` in data), and a module's energy is spent each second worn, each use in a tool. `ItemState` gains a gas
  tank. A module effect does nothing where it has no part, in what is worn or in a tool, unless its kind says.
- The mode key no longer writes the new mode on the action bar: the item's name above the hotbar shows it.
- What an item holds is shown in bars and slots rather than in lines, how to change its mode moves to the part
  Shift shows, and the Core's own lines of an item (its material, hazards, physics, the manual's key) are a section of
  the Core's tooltips, kept together. A slider is stepped with left and right only, as the game's are, so up and down
  move the keyboard on; buttons and check boxes of a screen's file are reached by the keyboard.
- The world drawn from somewhere else, the far side of a seamless portal, is drawn by `WorldViews`, which
  cameras on screens draw with too; the portal shows what it showed.
- Telemetry leaves the peripheral links: the point-to-point links are the computer's peripherals and the
  industrial control link; telemetry becomes a line of its own, which J's Space lays.
- Two layers: what is the series' own (the eras and tiers, the data network's model, the Operations and their
  kinds, the peripheral links, the `/jstech` command, the series' look, J's Energy, the shared content) has a package
  of its own, `dev.jstech.core.jstechseries`, and the rest of the Core, the platform any technology mod can build on,
  never reaches into it: the platform offers hooks (a command root, a cable's own eras and ports, listeners on lines,
  axes and styles declared from outside) and the series plugs into them.
- Listeners declared: every class of the three mods that answers the game's events is
  handed to the buses from its mod's constructor, through a `listen` method of its own, rather than found by an
  annotation, so what runs is what the constructor says; a mod's mistake in what it declares stops the load saying
  what to change.
- Models: a block entity's renderer of the Core draws what a machine holds besides its model, each part on the
  bone it rides on, gives each part a model of its own to move, says the box it is drawn within and when it is drawn,
  and lets a driver move bones after the animations.
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
- A panel of the series on the debug screen with nothing to show leaves its name out too.
- `ScenarioBuilder` and `GameTestPlayers` move out of the Core's jar into the test kit,
  `dev.jstech.testkit.gametest`, so the Core a player installs carries no test code.
- The tools for screens (the inspector, the editor and `/jstech screen new`) open while developer mode is on,
  in any game where a player turns it on, rather than only in a development run.
- Client tests: a run leaves what it drew for a reference picture in `reference-pictures/<mod>`, a folder for
  each mod, and `ClientTests.Test` holds the body that builds a test's steps in place of its method, so a test need
  not be a method.
- The packets counted tick by tick keep the biggest one of each kind too.
- A multiblock's terminal: the block a multiblock is worked from is called its terminal, where it was its
  controller, in the Structure Projector, the Multiblock Builder, the recipe viewers, the manuals, the studio and Jade.
- Eras in saves: an era is saved by its id (`jscore:legacy`), and progress along every axis by the
  step's name, so an era another mod inserts moves nothing a world saved. A world saved before starts its progress
  over, and the era advancements wait for the era by its id.

### Fixed
- A tall stack of moving structures set down touching stands still and is heard striking nothing, its
  dimension's physics budget spent or not. A stack let go at once is worked as one from its first tick, where each of
  it went alone that tick and fell onto what had gone already; two touching settle into the hair's depth they rest at
  slowly, where a stack of twelve settled all its joints at once and its top fell at a metre a second, as fast as a
  blow; structures worked together wait on the budget and go together after, where some of a stack went on while the
  rest waited and it leaned and swayed; and one that took its turn already, waiting or gone, is held as it goes by
  those that go after it in the tick, where a crate landing on a stack that waited shoved its top crate down with
  nothing of the crate below holding it up.
- A stack of moving structures comes to rest upright however it landed, and a tall one stands still and falls
  asleep: every point where two meet rests as deep, so none is left leaning on its shallower points, each joint of the
  stack adding its lean; a face lying square or a hair turned on another rests on its four corners, not on points
  along its edges that wandered from step to step, and two points known alike start from what they gave once, not
  twice; and each pass holds the stack up from the lowest structure to the highest and back down, not in the order
  the stack happened to be gathered in.
- A stack of moving structures standing still reads how fast its structures come together as they do: each
  read them after those below had already started from their pushes of the step before, so a stack at rest seemed to
  close several times faster than it did, as if it were landing.
- Moving structures falling onto one another never sink into each other as they land: one standing still that
  works their meeting looked for the one falling where it stood as the tick began, and met it only once it was
  already inside.
- A vein of a mineral with uranium or thorium in it reads near the same wherever it lies, and a great vein
  gives off more: its rock is all of it the source, and no longer shields itself. A vein cut by the edges of the
  world's sections of sixteen blocks read less than half as much, the rock between each part's middle and the reader
  taken for a wall.
- A stack of moving structures that sank into itself, as one landing on another can, is pushed apart without
  its top being thrown: a structure into another is moved out of it rather than given a speed to come out, which
  stayed with it once out, so the speeds of each sinking added up the stack and could throw the top off it.
- A block that gives light of a colour or a bright glow shows its colour and its halo in the tick it is placed,
  and loses them in the tick it goes, as does a lamp turned on or off: they waited for the turn of their part of the
  world, a few ticks later. Stained glass placed or taken away tints the light about it at once too.
- The settings screen's Done, Undo, Redo, Defaults, Export and Import buttons and its search field answer the
  mouse: the panel they sit in had no size, so a click never reached them and a change could not be kept.
- A click with a cable on a snow layer, or on anything else that gives way, lays one cable in its place: the
  player's game started the run above it, so letting go of the click laid a second cable on top of the first.
- Breaking a formed multiblock's terminal gives its merged blocks back at once, not up to five seconds later.
- The screens of the network probe, the signal probe and the clamp meter no longer show dark stairs outside
  their corners: the dark rim round the instrument follows its rounded corner.
- A cable's item is drawn whatever its thickness: only cables four, six or ten pixels thick had a model for
  their item, so the item of any other was drawn as the missing model; the Core now has one for every thickness.
- A limited cable two machines are fed through is shared between them: the first one in order took the whole
  of what it carried, and the other got nothing until the first was full.
- A torus in a STEP file is drawn whole: a face of one bounded by both of its seams was left out.
- A surface a STEP file sweeps from a closed curve, turned round an axis or drawn along a line, is drawn: its
  faces were left out, the curve's seam not taken for one.
- A turn a USD text file writes, as an `orient` or a skeleton's animation, is read real part first, as the text
  writes it: it was read as a binary file keeps it, so a joint at rest was drawn turned half round.
- A dimension made while the game runs is taken away only once what the server still had to do in it has run:
  a block's point of interest set just before could otherwise wait for ever on the dimension's closed storage, and
  the server with it.
- A dimension made while the game runs keeps loaded again the chunks it kept loaded, each time it is made
  again; it held them as kept but loaded none after the server restarted.
- A multiblock's controller lifted whole, as by a piston or by a mod that moves blocks as Create's
  contraptions do, is moved, not broken: its parts are no longer taken down where it stood, and what it holds no
  longer drops.
- A formed machine moved by a mod that does not tell it where its blocks went forgets the places it stood at
  and finds its structure again where it is, where it could stop the server reaching across the world for them.
- A fire lit in a chunk that went away before the sealed rooms looked at its oxygen no longer loads that chunk
  again to look.
- What reads the world only where it is loaded (a block entity read at the place it is remembered at, a fire
  spreading, the sealed rooms' fires) no longer reads a chunk still on its way to loading, which made the server wait
  for it in the middle of a tick.
- A chunk let go and wanted again before it unloaded, then needed by the generation of a chunk beside it, no
  longer holds the server: the game's unloading of it went round and round for all the server's spare time, so the
  generation never went on, new land never came and the server could not stop. The Core drops that unloading, as
  the chunk is kept; this was the game's own, made likelier by anything that loads chunks a while and lets them go.
- Machines of other mods join the data network by its real cables: a member looked for its network where the
  cable beside it stood, which only a hand-built index answered; it now takes the network of the data wire crossing
  into it, and a block made with the Core's `Device` takes cables on the ports its device declares. A member whose
  chunk is not in memory has no row in a state table, and reading the table no longer waits on that chunk.
- A controller keeps the machines on its industrial control cable: a computer's check of the screens and
  devices it holds ports for let them go every five seconds, as a machine keeps nothing of its controller, and they
  came back a second later; in between, the network read none of them and refused a request for any.

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
