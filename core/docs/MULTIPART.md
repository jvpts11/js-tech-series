# Block spaces and microblocks

*Added 2026-10-16.*

How several things share one block space: a cable's wires, the devices on its faces, and **microblocks**, pieces of
any full block cut with the saw. For the cable block and the devices on its faces, see [Cables](CABLES.md); this page
is about the space they all share, and about the microblocks.

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
or a sensor) take the middle of their face, two pixels from each edge and five deep.

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

## Placing

`SpacePlacement.target(level, clicked, location, face, piece)` works out where a player's click puts a piece:
against the face of the block clicked, in the space beyond it; or, clicked on a piece's own side inside a space, in
that same space, as a slab placed on a slab's top. A post goes on the edge of the face nearest the click, or through
the centre when the click is near the face's middle; a corner in the nearest corner. `SpacePlacement.place` then
places it, making a space of parts alone where nothing stands, and refuses where a player or a mob stands.

## Breaking

A click hits exactly the piece under the crosshair (`SpaceParts.aim`), the outline shows only it, and breaking takes
only it, with its drop; how long it takes is its block's. The middle-click picks its item.

## Drawing

Every piece of a space is drawn into the world's mesh, which is built again only when a piece changes. Where two
pieces on faces side by side meet along their edge, the thinner gives way there, or, when they are as thick, the one
on the later face (`SpaceDrawing`), so two blocks never fight to be seen in one place. A face of a piece lying flat
against a piece of an opaque block is not drawn.

A space whose face a piece of an opaque block covers whole says so in its state (`covered_down` to `covered_east`),
and the neighbour's face behind it is not drawn either. The game keeps what one block hides of another by the two
blocks' states alone, so what a space hides is in its state and never read from its block entity; for the same
reason the cable block says it hides nothing.

## Parts that move

A kind of part with pieces that move, a fan's blades or a gauge's needle, is marked when it is made,
`new PartType<>(...).moving()`, and its mod hands the Core what draws them on the player's game (`IPartMotion`):

```java
modBus.addListener(FMLClientSetupEvent.class, event -> PartMotions.register(MyParts.FAN.get(),
        (pose, buffers, busy, time, light) -> drawBlades(pose, buffers, busy ? time * 18F : 0F, light)));
```

Each frame, only the spaces in sight that hold such a part are drawn so (`MovingParts`), each part turned to its face
as its model is; a world of still parts draws nothing here. The rest of the part is its model, in the mesh.

## What can go wrong

- **A piece of your block cannot be cut.** It has a block entity, no item of its own, a model that is not a plain
  block model, or a shape that does not fill the block; or a pack put it in `jscore:not_cuttable`.
- **A piece is refused.** Its place is taken, it overlaps another piece, or it would cross the wires in the middle;
  only pieces against faces share the middle's room, and only five pixels deep: on a cable, use a cover, a panel or a
  hollow piece.
- **A facade is refused.** The space has no cable, whose wires it hides; place a cover there instead.
- **Your saw does not cut.** It is not in the item tag `jscore:saws`.
- **`PartMotions.register` refuses a kind.** The kind was not marked with `moving()` where it is made.
- **A neighbour's face shows behind a cover.** The cover's block is not opaque, as glass is not; only a piece of a
  block the game draws solid hides what lies behind it.
