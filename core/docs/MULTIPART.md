# Block spaces and microblocks

*Added 2026-10-16.*

How several things share one block space: a cable's wires, the devices on its faces, **microblocks**, pieces of any
full block cut with the saw, and the game's own torches, levers, buttons and pressure plates. For the cable block
and the devices on its faces, see [Cables](CABLES.md); this page is about the space they all share, and about the
microblocks.

## A block space and its places

A block space has 27 **places** (`SpaceSlots`): the **centre**, the **six faces**, the **twelve edges** and the
**eight corners**. A face's place is `SpaceSlots.face(direction.get3DDataValue())`; an edge's is named by the axis it
runs along and which end of each other axis it lies at, `SpaceSlots.edge(axis, firstFar, secondFar)`; a corner's by
which end of each axis it lies at, `SpaceSlots.corner(farX, farY, farZ)`. Each place holds one thing at most.

Two kinds of blocks are spaces today: the Core's cable block, whose wires run through the middle, and the **space
of parts alone** (`jscore:multipart`), which a microblock makes when it is placed where nothing stands and which
goes away when its last piece is taken. Both hold their things in a `SpaceParts`, and their block entities implement
`ISpaceHolder`, so a microblock goes into either the same way.

The block entity that owns the space is its `IPartHost`: it says where it is, saves and sends when a part changes,
and says what runs through its middle (`middle()`, the boxes no piece but one against a face may share) and whether
its faces take facades (`takesFacades()`, true only where a cable runs).

## What fits

A piece fits a space when its place is free and its boxes overlap no other piece's (`SpaceFit`). Pieces against
faces side by side may meet at the block's edges, as two slabs on adjacent faces do; pieces against opposite faces
may not reach into each other, so a slab and five eighths do not fit one space. Devices on faces (`IFacePart`, a bus
or a sensor) take the middle of their face, two pixels from each edge and five deep, unless their kind says otherwise
(see below).

Nothing but a piece against a face may share room with what runs through the middle, and that piece only within five
pixels of its face: a cover, a panel or a device closes a cable's end, while a slab would reach into the cable and
does not fit. `SpaceParts.fits(piece)` asks before anything is placed; `add` places it and answers whether it did.

## Microblocks

A microblock (`Microblock`) is a piece (`MicroblockPiece`: a shape, a thickness in eighths, a block) in a place
of the space, turned along an axis when its shape runs along one:

| Shape | Where | Thickness |
| --- | --- | --- |
| `FACE` | Against a face: the cover (1/8), the panel (2/8), the slab (4/8) and those between | 1 to 7 |
| `HOLLOW` | Against a face, with an eight-pixel hole in its middle | 1 to 7 |
| `FACADE` | Against a face of a cable, hiding it | 1 |
| `POST` | Along an edge, or through the centre along an axis | 1 to 7 |
| `CORNER` | In a corner | 1 to 7 |

Only a whole `FACE` piece cuts the link of the wires on its face; a hollow piece lets them through, and a facade
hides them while they keep working. A microblock's item is the Core's one microblock item with its piece in the
`jscore:microblock` component: `CoreSpaces.stack(piece)` makes one.

Microblocks are cut from any full block of any mod that is drawn by a model, has no block entity and has an item
(`MicroblockMaterials.cuttable`). A pack keeps a block out with the block tag `jscore:not_cuttable`. Each piece is
drawn from its block's own textures and render type, and its faces at the block's edges are hidden where a full
block stands beside them.

## Cutting and joining

The recipes are rules in code, true of every block, so there is one of each and not one per material
(`MicroblockRecipes`):

- **The saw's cut.** A tool of the item tag `jscore:saws` beside a block or a piece in the crafting grid: above it,
  two halves (a block gives two slabs, a slab two panels, a panel two covers); beside it, cut across (a face piece
  gives two posts, a post two corners); at a corner of it, the face piece hollowed. Each cut wears the saw by one and
  leaves it in the grid. The Core's own saw is in the tag; a mod's saw joins it to cut the same way.
- **Joining.** Two pieces of one shape and one block make one as thick as both, the thicknesses between the halves
  among them; two face pieces that make eight eighths make the block again.
- **Facades.** A cover with a piece of any cable (`CableItem`) makes a facade.

The thinking behind each rule is pure (`Cuts`), so it is tested without the game.

JEI and EMI show each rule as one crafting recipe, not one for every block: its grid and what it makes go through
every block microblocks are cut from, every piece and every way the rule works, all its places showing the same frame
at once (`MicroblockShowcase`, worked out by the rules themselves). Looking up a block, or the microblock, finds the
rules; in JEI only the frames of the block looked up are shown. A mod's own cuttable blocks join them with nothing
written.

## Placing

`SpacePlacement.target(level, clicked, location, face, piece)` works out where a player's click puts a piece:
against the face of the block clicked, in the space beyond it; or, clicked on a piece's own side inside a space, in
that same space, as a slab placed on a slab's top. A post goes on the edge of the face nearest the click, or through
the centre when the click is near the face's middle; a corner in the nearest corner. `SpacePlacement.place` then
places it, making a space of parts alone where nothing stands, and refuses where a player or a mob stands.

## Breaking

A click hits exactly the piece under the crosshair (`SpaceParts.aim`), the outline shows only it, and breaking takes
only it, with its drop; how long it takes is its block's. The middle-click picks its item.

## Declaring a kind of part

A kind of part is declared once, as a block is, with its item under the same id:

```java
public static final PartEntry<SensorPart> SENSOR = CONTENT.part("sensor", SensorPart::new)
        .named("Sensor")
        .look(PartLook.plate("block/part/sensor_front", "block/part/sensor_side"))
        .redstone()
        .tab(MyTab.TOOLS)
        .register();
```

`register()` registers the kind in the Core's part registry and its item, a `PartItem`, which mounts the part on the
face of a cable or a space of parts it is used on, or, used on any other block, on the face of the space beside it
toward that block, making the space where nothing stands. The kind is called what its item is. The data generation
writes the item's model, which shows the part's own, and its English name; a part drawn with `PartLook.plate(front,
side)`, or `plate(front, busyFront, side)` with a front of its own at work, has its model written too, a plate twelve
pixels across and five deep facing north. A part drawn with `PartLook.handmade(model)` uses the mod's own model file.
`shaped`, `redstone`, `turns` and `moving` say what the kind says ([What a device can do](#what-a-device-can-do)). A
mod with its own data generation adds `ContentPartModelProvider` beside its other providers.

## What a device can do

A device on a face (`IFacePart`, see [Cables](CABLES.md#parts-on-the-cable)) does each of these on its own face:

| Method | What it does |
| --- | --- |
| `ticks()` | Whether it needs its tick. A space none of whose devices needs one is never ticked; the default is true. |
| `signal()` | The redstone strength it gives out of its face, to the block there and to wire beside it. |
| `directSignal()` | The strength it gives into the block on its face, which then powers its own neighbours, as a lever does. |
| `comparator()` | What a comparator standing against its face reads. |
| `light()` | The light it gives. |
| `neighbourChanged()` | Called on the server when the block on its face, or the redstone reaching it, changes. |
| `use(player)` | A player used it: it opens its menu or does what it does. |
| `turned(quarterTurns)` | A wrench turned it about its face, for a kind marked `turns()`. |
| `describe(lines, details)` | The lines a player looking at it reads, in Jade's tooltip. |

A device whose light or need of a tick changes calls its host's `partChanged()`; one whose redstone or comparator
reading changes calls `partSignalChanged(face)`, and the block's neighbours hear of it. Strengths are kept within 0
and 15. A device that is a sensor, an `ISignalOutput`, is read on its face by anything that reads sensors
([Redstone and signals](SIGNALS.md)). A menu opened on a device stays open while
`MenuValidity.part(level, pos, face, part)` holds: the very device still on that face, and the player within reach.

The light and the need of a tick are kept in the block's state (`light`, `ticking`, in `PartStates`), set again each
time a device changes, because the game reads both from a block's state, and the light engine may reach a block before
its block entity is loaded. A block read from an older save whose state does not show its devices is set right on its
first tick.

Its kind says the rest, which every player's game knows:

```java
() -> new PartType<>(LampPart::new, MyTexts.LAMP, rl("block/part/lamp"))
        .redstone()                                          // wire beside its face joins it
        .shaped(List.of(new PartBox(5, 5, 0, 11, 11, 3)))    // its own room, facing north, in pixels
        .turns()                                             // a wrench turns it about its face
```

Without `shaped`, a device takes a plate twelve pixels across and five deep. Its room is what it collides with, is
aimed at and outlined by, and what keeps other parts out of its way; it is turned to each face as its model is
(`PartBox.turnedTo`). Two devices on faces side by side may share room at the corner between their plates, and two
microblocks along their edge, but a device never reaches into a microblock.

## The game's small blocks

Torches, levers, buttons and pressure plates, the game's own and any mod's made of the game's kinds, share a block
space too (`SmallBlocks`). Each hangs on the face of the space toward what holds it up, as it would in a block of its
own, and takes that face's place. A player places one by using its item on the face of the block beside a cable or a
space of parts: the item decides how it stands, as it always does, and it goes into the space when it fits there.
On a cable, a torch hangs beside the wires; it cannot stand where the wires run.

They are the game's own blocks, run at the space's place: their shape, signal, light and particles are their own, a
lever is pulled and a button pressed when used, a pressure plate is pressed by what stands on it, and a wooden button
by an arrow. Wire beside the space joins it while it holds a lever, a button or a plate. A pressed button or plate is
looked at again on the space's tick, which it has only while one is pressed. One whose block beside the space goes
falls off with its item, as the game's own does. They have no collision, and break as fast as the game's own.

`SpaceParts.addSmall(state)` hangs one from code, `small(face)` reads it and `removeSmall(face)` takes it off.

## The wrench and the look-at tooltip

A wrench of any mod (the item tag `c:tools/wrench`) works on the part a player looks at in a cable or a space of
parts (`PartWrenching`):

- **Used**, it turns the part, when it turns: a device of a kind marked `turns()` a quarter turn about its face, its
  model and its room together (`PartBox.turnedAbout`); a lever or a button on a floor or a ceiling a quarter turn; a
  post through the middle to the next axis. A part that does not turn, or would not fit turned, is used as by hand.
- **Used sneaking**, it takes the part off into the player's hands.
- A wire looked at is left to the wrench's own work on wires, and a place the player may not touch is left alone.

The look-at tooltip, Jade's when it is installed, names the part looked at and shows its item, and a block entity of
several pieces (`IDescribedAt`, as the cable block and the space of parts are) adds the lines of the piece looked at:
what its device says in `describe`, or the redstone a lever, a button or a plate gives.

## Drawing

Every piece of a space is drawn into the world's mesh, which is built again only when a piece changes. Where two
pieces on faces side by side meet along their edge, the thinner gives way there, or, when they are as thick, the one
on the later face (`SpaceDrawing`), so two blocks never fight to be seen in one place. A face of a piece lying flat
against a piece of an opaque block is not drawn.

A space whose face a piece of an opaque block covers whole says so in its state (`covered_down` to `covered_east`),
and the neighbour's face behind it is not drawn either. The game keeps what one block hides of another by the two
blocks' states alone, so what a space hides is in its state and never read from its block entity; for the same
reason the cable block says it hides nothing.

*Added 2026-10-16:* a microblock wears the connected textures resource packs give its material, joining a whole block
of it and microblocks like it, of the same shape and place, in the spaces around ([Connected
textures](CONNECTED_TEXTURES.md)).

## Parts that move

A kind of part with pieces that move, a fan's blades or a gauge's needle, is marked when it is made,
`new PartType<>(...).moving()`, and its mod hands the Core what draws them on the player's game (`IPartMotion`):

```java
modBus.addListener(FMLClientSetupEvent.class, event -> PartMotions.register(MyParts.FAN.get(),
        (pose, buffers, busy, time, light) -> drawBlades(pose, buffers, busy ? time * 18F : 0F, light)));
```

Each frame, only the spaces in sight that hold such a part are drawn so (`MovingParts`), each part turned to its face
as its model is; a world of still parts draws nothing here. The rest of the part is its model, in the mesh.

## Testing

The Core's GameTest kit builds spaces of parts as a test needs them (`ScenarioBuilder`): `placeMicroblock(pos,
microblock)`, `mountPart(pos, face, type)`, which gives back the part mounted, `hangSmall(pos, state)` and
`partsAt(pos)`, each into a cable's space or into one made where nothing stands, failing the test when it cannot.

```java
final ScenarioBuilder build = ScenarioBuilder.forGameTest(helper);
final SensorPart sensor = build.mountPart(new BlockPos(2, 2, 2), Direction.NORTH, MyParts.SENSOR.get());
```

## What can go wrong

- **A piece of your block cannot be cut.** It has a block entity, no item of its own, a model that is not a plain
  block model, or a shape that does not fill the block; or a pack put it in `jscore:not_cuttable`.
- **A piece is refused.** Its place is taken, it overlaps another piece, or it would cross the wires in the middle;
  only pieces against faces share the middle's room, and only five pixels deep: on a cable, use a cover, a panel or a
  hollow piece.
- **A facade is refused.** The space has no cable, whose wires it hides; place a cover there instead.
- **Your saw does not cut.** It is not in the item tag `jscore:saws`.
- **A declared part stops loading with "has no look".** Give it `look(PartLook.plate(...))` or
  `look(PartLook.handmade(...))` before `register()`.
- **A plate part shows as a purple and black box.** The mod's data generation lacks `ContentPartModelProvider`, or
  has not been run since the part was declared.
- **`PartMotions.register` refuses a kind.** The kind was not marked with `moving()` where it is made.
- **A device never ticks.** Its `ticks()` says false, or its light or need of a tick changed without a call to
  `partChanged()`, so the block's state still says it needs none.
- **Wire beside a device does not join it.** Its kind is not marked with `redstone()`.
- **A torch goes into the air beside a cable instead of onto it.** It was used on the cable itself; use it on the
  face of the block beside the cable, where it hangs.
- **A small block will not go into a space.** It would cross the wires or another part; on a cable, hang it on a face
  the wires do not run out of.
- **A device is refused beside a slab.** Its plate would reach into the slab; give its kind a smaller room with
  `shaped`, or use a thinner piece.
- **A neighbour's face shows behind a cover.** The cover's block is not opaque, as glass is not; only a piece of a
  block the game draws solid hides what lies behind it.
