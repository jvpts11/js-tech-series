# Reaches into the game

*Added 2026-10-16.*

The few places J's Core reaches into the game itself, with a mixin or an access transformer, where no event or API of
NeoForge could do the job. They live only in the Core, never in another mod of the series: a mod that needs the game
to behave otherwise asks the Core for it. Each is as narrow as it can be, touches one method or one field, and acts
only for what is the series' own, so the rest of the game and other mods pass as before.

Every reach is listed here with why it is needed and the test that fails if a new version of the game or of NeoForge
moves what it reaches. The mixin configuration is required and every injection must apply, so a target that moved stops
the game at start instead of failing quietly; a test of the Core reads this page and fails on a reach not listed, or a
test named here that does not exist.

The design behind this page is [Low level](design/low-level.md).

## The reaches

| Reach | What it is | Why | Test |
| --- | --- | --- | --- |
| `GameRendererMixin` (`GameRenderer#pick`) | mixin, on the player's game | Lets the crosshair land on a part standing out of a block, such as an open drive tray: the game aims only at a block's cube, and offers no event between its aim and the frame. | `ReachClientTests.standingParts_catchTheAim` |
| `AdvancementWidgetMixin` (`AdvancementWidget#<init>`) | mixin, on the player's game | Lets an advancement's title be as wide as it is written; the game cuts it at 163 pixels. | `ReachClientTests.advancementTitles_runAsWideAsTheyAreWritten` |
| `MappedRegistryMixin` (`MappedRegistry#register`) | mixin | Registers the entries of a namespace declared stable as stable: the game marks every entry not from its own pack as experimental, and no event lets a mod say its data is no experiment. | `StableNamespaceGameTests.registries_takeEntriesOfStableNamespacesAsStable` |
| `WorldDimensionsMixin` (`WorldDimensions#checkStability`, `WorldDimensions#bake`) | mixin | Lets a world whose extra dimensions are all of namespaces declared stable open with no warning about experimental settings; another mod's dimension still warns. | `StableNamespaceGameTests.dimensions_ofStableNamespacesKeepAWorldStable` |
| `Slot#x`, `Slot#y` | access transformer, `public-f` | A screen declared in a file lays its slots out on the player's game, from the file, the style and the font; a slot's place is final in the game. | `AccessTransformerGameTests.slotPlace_canBeMovedWhereALayoutPutsIt` |
| `ToastComponent#occupiedSlots` | access transformer | The game's toasts take slots down the top right corner and say nowhere how many; the HUD elements of that corner start under them, so a toast never covers a notification. | `ReachClientTests.toasts_sayWhichSlotsTheyHold` |
| `BlockItem#getPlacementState`, `StandingAndWallBlockItem#getPlacementState`, `GameMasterBlockItem#getPlacementState`, `ButtonBlock#type`, `ButtonBlock#ticksToStayPressed`, `BasePressurePlateBlock#type`, `BasePressurePlateBlock#getPressedTime`, `WeightedPressurePlateBlock#getPressedTime`, `BasePressurePlateBlock#getSignalStrength`, `PressurePlateBlock#getSignalStrength`, `WeightedPressurePlateBlock#getSignalStrength` | access transformer | A cable or a space of parts holds the game's own torches, levers, buttons and pressure plates, each run by the game's own rules at the space's place; where they are placed, how long they stay pressed, what they sound like and what a plate counts are kept in members the game does not open. | `AccessTransformerGameTests.smallBlocks_reachTheGamesOwnRules` |
| `ModelBakery#topLevelModels` | access transformer, on the player's game | A resource pack written for CTM overrides the connected textures of the faces of one tint index in a block model; which models a block's state was baked from the game keeps only in the bakery. | `ConnectedPacksClientTests.modelOverrides_drawTheFacesOfTheirTintIndex` |

## Namespaces whose data is stable

The game takes as stable only its own data, so a world holding a mod's dimension opens with a warning about
experimental settings. A mod whose data is no experiment says so for its own namespace, from its constructor:

```java
StableNamespaces.declare(MyMod.MODID);
```

The Core declares the namespaces of every mod of the series. A world whose dimensions beyond the game's own three are
all of declared namespaces opens with no warning; any other mod's dimension still warns, as it should.

## What can go wrong

| What you see | Why |
| --- | --- |
| The game stops at start with an error from Mixin naming the Core | A new version of the game or of NeoForge moved what a reach changes: the reach and its test are fixed together. |
| A world still opens with the experimental warning | One of its dimensions is of a namespace no mod declared stable. |
