# Connected textures

*Added 2026-10-16.*

A texture that joins between alike blocks reads as one surface: glass with no frame between its panes, a structure's
casing as one wall, a floor laid in a pattern bigger than a block. The Core draws them from what resource packs already
write, in both formats packs write them in, so a pack made for OptiFine, Continuity or CTM works as it is, and a mod
declares its own connected textures the same way, as data in its resources. Everything here is on the player's game.

## The formats read

**OptiFine's and Continuity's**: a `.properties` file under `optifine/ctm` (or `mcpatcher/ctm`) of any namespace, its
tiles beside it.

```properties
# assets/mymod/optifine/ctm/glass/glass.properties
method=ctm
matchBlocks=mymod:clear_glass
tiles=0-46
```

| Key | What it says |
| --- | --- |
| `method` | `ctm` (47 tiles), `ctm_compact` (5), `horizontal`, `vertical` (4 each), `horizontal+vertical`, `vertical+horizontal` (7), `top`, `random`, `repeat`, `fixed`, `overlay` (17), `overlay_ctm`, `overlay_random`, `overlay_repeat`, `overlay_fixed`; and the old `glass`, `bookshelf` and `sandstone` |
| `tiles` | the tiles in order: `0`, a range `8-11`, a name, `name.png`, a path (`textures/block/stone.png`, `mymod:block/x.png`, `~/ctm/...` for `optifine/ctm/...`), `<skip>` for none, `<default>` for the face's own texture |
| `matchTiles`, `matchBlocks` | the textures or the blocks it is drawn on; a block with the values of its properties, `oak_log:axis=x,z`. A file that names neither is named for what it draws on: `glass.properties` the texture `glass`, `block_stone.properties` the block `stone` |
| `connect` | what a neighbour must share to join: `block`, `tile` (the same texture on its face) or `state` |
| `connectBlocks`, `connectTiles` | the blocks and textures an overlay is laid for |
| `faces`, `biomes`, `heights` | where it is drawn: `top`, `bottom`, `sides`, `north` and the rest; biomes, `!` for all but those; heights, `(-64)-0 100` (and the old `minHeight`, `maxHeight`) |
| `weight` | which of two rules for one face wins, the higher |
| `innerSeams` | whether a face shows a seam where it meets a neighbour whose own face is covered |
| `weights`, `randomLoops`, `symmetry`, `linked` | how `random` picks: each tile's weight, how much more the place is mixed, which faces pick alike (`none`, `opposite`, `all`), and the halves of a two-high block alike |
| `width`, `height` | `repeat`'s pattern, in tiles |
| `ctm.<case>` | for `ctm_compact`, a tile drawn whole for one of the 47 cases |
| `tintIndex`, `tintBlock`, `layer` | how an overlay is tinted, by which block's colours, and its layer (`cutout`, `translucent`) |
| `name` | for a block that can be named, the name it must have: as it is, `pattern:` with `*` and `?`, `ipattern:`, `regex:`, `iregex:` |

Rules by texture are tried before rules by block, the heavier first, then by file name, and the first that draws a face
wins; every overlay that applies is laid over it as well.

**CTM's**: a `ctm` section in a texture's `.mcmeta`, and `ctm_overrides` in a block model for its faces of one tint
index.

```json
{ "ctm": { "ctm_version": 1, "type": "ctm", "layer": "CUTOUT", "textures": ["mymod:block/clear_glass-ctm"] } }
```

| Type | What it draws |
| --- | --- |
| `normal` | the texture as it is (to give it `light` or a `layer`) |
| `ctm` | a quarter of a face at a time, from the texture itself and its companion, two by two: joined all round, up and down, sideways, both edges without the corner |
| `ctm_horizontal` (`ctmh`), `ctm_vertical` | joined one way only, from the texture's two by two: alone, the middle and the two ends |
| `pillar` (`ctmv`) | a column, standing or lying as its like join it: its ends the texture, its sides its companion's two by two |
| `ctm_simple` (`sctm`) | a simple two by two: not joined, sideways, up and down, all round |
| `random` (`r`), `pattern` (`v`) | a tile of a grid (`size`, or `width` and `height`; `x_offset`, `y_offset`) at random, or laid over the world |
| `edges`, `edges_full` | changed where the blocks it changes for meet it, a quarter at a time or whole from four by four, and wholly by its second companion where one covers it |
| `eldritch` | its quarters' meeting point moved a little at random, warping the face |
| a logic by id, `ctm:optifine_full` | a logic written as data under `ctm_logic`: named neighbours, cells, a face's cells and rules, the first rule that holds giving a cell. The Core brings `ctm:optifine_full` (OptiFine's 47 tiles as one sheet of twelve by four) |

Its `extra` holds `light` (a number or `block` and `sky`, from 0 to 15), `ignore_states`, `connect_inside` and
`connect_to`: blocks with predicates on their properties (`=`, `!=`, `>`, `<`, `>=`, `<=`), or `default` with faces
of their own that `defer` with `and` or `or`. A `proxy` draws another texture with what it says in its place. A face
that both formats speak for is drawn as CTM says.

**Besides the formats**, the Core reads a tag of blocks wherever blocks are named: `#c:glass_blocks` in
`matchBlocks` or `connectBlocks`, and `{"tag": "c:glass_blocks"}` in `connect_to`. A rule that names
`connectBlocks` or `connectTiles` joins those, whatever its method.

## How it is drawn

The tiles of every rule go into the blocks' atlas with the game's own textures. Every block model a rule may draw is
drawn through them: as the world's mesh is built it reads the 26 blocks around each block, and each face takes its
tile from how it joins, by the way its texture lies on it, so a turned or mirrored model joins as it is drawn. A face
that only covers part of its block, a slab's or a microblock's, takes the same part of its tile. In a hand or a screen
a block is drawn as its own model.

- **Microblocks** of a material join a whole block of it and microblocks like them, of the same shape and place, in
  the spaces around, as whole blocks of their material would ([Block spaces and microblocks](MULTIPART.md)).
- **The Core's own connected blocks**, declared in code (`CoreModels.connected(block, rule, tiles)`,
  [Screens](SCREENS.md)), are drawn as they always were.

*Added 2026-10-22:* **glowing textures**, OptiFine's emissive form, as Continuity reads it. A pack that says the
ending of a glowing texture's name in `assets/minecraft/optifine/emissive.properties`
(`suffix.emissive=_e`) has every block texture with one of that name beside it drawn with it over it at full light,
unshaded: `textures/block/furnace_front_on_e.png` beside `furnace_front_on.png` makes the glowing pixels of the
furnace shine in the dark. A connected texture's tile glows the same way, its glowing tile beside it. A face of the
solid or cut-out layers has its glowing texture drawn in the cut-out layer, so the clear pixels show the face
beneath; a see-through face, in its own. A pack that says no ending has no glowing textures, as in OptiFine. Blocks
in the world glow so; items are drawn as they are.

## Stepping aside

While Continuity or CTM is installed it draws the packs' connected textures and glowing textures, and the Core leaves
them to it: it reads no rule, puts no tile into the atlas and glows no texture, and the log says once what it stepped
aside for. The player's setting
`step_aside.connected_textures` in `jstech-step-aside.toml` keeps the Core's own on as well. The Core's connected
blocks declared in code are drawn either way, and a mod that writes its connected textures as data in either format
has them drawn by whichever mod draws them.

## What can go wrong

- **A face does not join.** The rule matches by texture and the block's face has another (a model with several
  textures), or by block and the neighbour is another block: `connect=block` joins the same block, `tile` the same
  texture, `state` the same state. A CTM texture joins its own state unless `ignore_states`.
- **A rule is left out.** The log says which file and why: a method or type no pack knows, too few tiles, a value
  that does not read.
- **A tile shows the missing texture.** Its file is not where the rule says: tiles are beside the `.properties` file
  unless the rule gives a path.
- **Nothing is drawn at all.** Continuity or CTM is installed, and the Core stepped aside.
