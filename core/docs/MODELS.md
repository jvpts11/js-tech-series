# Models

*Added 2026-10-16.*

Animated models for blocks, items, entities and armour, read from the files artists already make: a Blockbench
project, a Bedrock or GeckoLib geometry, glTF from Blender, FBX from Maya, 3ds Max, Cinema 4D or Blender, and OBJ from
anything. Every reader is the Core's own, written from each format's description; none needs the program that made the
file. A model has bones in a tree, cubes and meshes on them, locators, skins and morph targets; it plays animations
with every easing Blockbench has, several at once, blending from one to the next; and the server can tell everyone who
sees it what to play.

The design behind this page is [Models](design/models.md).

## The formats

| File | Ending | What the Core reads of it |
| --- | --- | --- |
| Blockbench project | `.bbmodel` | groups as bones, cubes (box or per-face pictures, on any of its textures), meshes, locators, the textures it carries, its animations with bezier handles and events |
| Bedrock or GeckoLib geometry | `.geo.json` | bones, cubes, per-face or box pictures, locators, poly meshes; every model of the series' mods reads as it is |
| Bedrock or GeckoLib animations | `.animation.json` | keyframes of numbers or Molang, every easing, loop modes, sound, particle and timeline events |
| glTF 2.0 | `.gltf`, `.glb` | nodes as bones, meshes with normals, pictures, skins and morph targets (sparse ones too), the textures of their materials, animations stepped, straight or by cubic spline |
| FBX 7.x, binary or text | `.fbx` | models with their pivots and pre- and post-rotations, meshes split by material, skins, blend shapes, every animation stack |
| Wavefront OBJ | `.obj` | each object or group a bone, faces of any number of corners, the textures its material library names |

Every format is turned into the same space, the game's: Y up, a model facing north (toward -Z) as it is drawn, sixteen
pixels to a block. Bedrock's files count X the other way, and are turned round as they are read. glTF and OBJ count in
metres, a block each; FBX says its own unit and which way is up, and its model hangs from one bone, `fbx_axes`, that
turns its axes into the game's.

## Declaring a model

A model is declared once, with the mod's content:

```java
public static final DeclaredModel PRESS = CONTENT.model("press")
        .file("jsmodels/press.bbmodel")                       // any format above
        .texture("textures/block/press.png")                  // the first for every face that names none
        .animations("animations/press.animation.json")        // besides the animations its file carries
        .glow("lamp")                                         // besides the bones named ..._glow
        .variant(off -> off.when("powered", "false").hide("lamp"))
        .register();
```

| Method | What it says |
| --- | --- |
| `file(path)` | the model file, under the mod's `assets` folder |
| `texture(path)` | a texture it is painted with; none to use the textures the file carries or names |
| `animations(paths...)` | animation files it plays from |
| `glow(bones...)` | bones drawn at full light, and everything hanging from them |
| `glowLight(level, colour)` | its glowing parts cast light of `level` (1 to 15) in `colour` on the world around them as they move, with a halo ([Lighting](LIGHTING.md)) |
| `translucent()` | it is drawn see-through, every face sorted back to front |
| `noCracks()` | a block drawn with it shows no cracks while it is broken |
| `variant(...)` | chosen by the block's state: another file (`model`), another texture, bones hidden (`hide`) or shown (`show`) |

The data run writes its definition to `assets/<mod>/jsmodels/<path>.model.json`, after reading the model: a file that
is not there, a model that does not read, or a bone named that the model has none of stops the run with what to fix.
Add the provider when your mod does not gather its data through `ContentData`:

```java
generator.addProvider(event.includeClient(), new ModelDefinitionProvider(output, CONTENT, existingFiles));
```

## Drawing a block

```java
event.registerBlockEntityRenderer(PRESS_ENTITY.get(), context -> KitBlockEntityRenderer
        .<PressEntity>of(PRESS.id())
        .driver((press, animator, seconds) -> animator.play("main", press.working() ? "strike" : "idle", seconds))
        .held((press, locator) -> locator.equals("plate") ? press.workpiece() : ItemStack.EMPTY)
        .build());
```

The model stands on the middle of its block's floor and turns with the block's `facing` (or `horizontal_facing`),
as Bedrock's models of blocks do. Give the block `RenderShape.ENTITYBLOCK_ANIMATED` and a block model of nothing but
its particle. The renderer's box is the model's, whichever way it faces, so a model wider than its block is not cut off.
A model drawn from a whole block, where no light reaches, is lit by the brightest light around the block.

*Added 2026-10-16:* `formed(model)` and `formedOnly(model)` draw a multiblock's model over its whole structure once
it forms ([Multiblocks](MULTIBLOCKS.md)), and parts of mechanical power are drawn turning by `TurningParts`
([Mechanical power](MECHANICAL_POWER.md)).

| Builder | What it says | Unless said |
| --- | --- | --- |
| `driver(...)` | the code that chooses what plays each frame and moves bones | plays what the server says |
| `held(...)` | what the model holds at each locator, drawn as an item in a frame is | nothing |
| `viewDistance(blocks)` | how far away it is drawn | 64 |
| `animateDistance(blocks)` | how far away it is animated; farther, it keeps its last pose | 48 |

A family of files laid out for GeckoLib (a `GeoLook`) is drawn by the Core's models as it is:
`KitBlockEntityRenderer.look(MY_LOOK)`. The model, the texture and the animation file are the ones the look names.

## Drawing an item, an entity, armour

```java
// An item, in every place the game draws one; its item model has builtin/entity for parent, and says its display.
event.registerItem(KitItemRenderer.of(DRILL.id()).extensions(), MyItems.DRILL.get());
// Armour: bones named armorHead, armorBody, armorRightArm ... follow the wearer's limbs, as Blockbench's templates name them.
event.registerItem(KitArmorModel.of(EXOSUIT.id()).extensions(), HELMET.get(), CHESTPLATE.get());
// An entity, turned the way it looks.
event.registerEntityRenderer(ROBOT.get(), context -> KitEntityRenderer.of(context, ROBOT_MODEL.id(),
        (robot, animator, seconds) -> animator.play("main", robot.isWorking() ? "work" : "idle", seconds)));
```

An item moves apart in each place it is drawn (in the hand, in first person, in a slot, on the ground), so the one in
a slot does not play what the one in the hand plays; a stack the server animates moves alike everywhere.

## What moves a model

A model's `ModelAnimator` holds its **controllers**, each playing one animation, in the order they were first asked
for; a later one's angles add to an earlier one's, and its glTF or FBX turns take their place. Each controller plays
at its own speed, round again, held on its last frame or once, as its animation's loop says, and **blends** from where
the bones were to its new animation (0.2 s unless said).

```java
animator.play("main", "working", seconds);                     // an animation by its name or its last part
animator.controller("doors").play(open, seconds, 0.5);          // blending over half a second
animator.controller("fans").speed(load * 2, seconds);
animator.mover((pose, seconds) -> pose.addEuler(pose.index("needle"), 0, 0, gauge * 270));
```

**Bones moved by code** are a layer over the animations: a mover added to the animator turns, moves, sizes or hides
bones every frame (`addEuler`, `setTurn`, `addMove`, `multiplySize`, `setHidden`, `setWeight` for a morph target). A
driver that adds movers says so with `movesBones()`, so its model is never taken to be at rest.

**Expressions** in keyframes are Molang, as Bedrock's files write it: numbers and arithmetic, comparisons, `&&`, `||`,
`?:`, `??`, `math.*` (angles in degrees), statements and `return`, `variable.`/`v.`, `temp.`/`t.`, `context.`/`c.`,
and `query.`/`q.`. `query.anim_time` and `query.life_time` are given; the code gives the rest:

```java
animator.context().query("load", context -> machine.load());   // read as query.load
```

**Events** happen as their times go by: a sound plays where the model is, a particle shows at its locator, and a
timeline word goes to the driver's `instruction(thing, animation, word)`.

## Animations from the server

```java
ModelAnimations.play(this, "door", "open");            // a block entity, on the server
ModelAnimations.replay(this, "main", "strike");        // from its start, whether it plays already or not
ModelAnimations.stop(this, "door");
ModelAnimations.speed(this, "fans", 2.0F);
ModelAnimations.play(entity, "arms", "wave");          // an entity
ModelAnimations.play(holder, stack, "main", "fire");   // an item, as those who see its holder see it
```

Everyone who sees the model sees it play. An order that arrives before the model is first drawn waits for it. A player
who comes into sight later sees what the code plays from then on; what lasts belongs in the driver, read from the
block entity's state.

## Glow, see-through, cracks

A bone whose name ends in `_glow`, or that the declaration names, is drawn at full light, and so is everything hanging
from it. With `glowLight(level, colour)` its glowing parts also light the world around them where they are drawn in
the world, as a lamp's bulb does, moving with them: a lamp on a machine's turning arm lights what it passes. A
see-through model has its faces sorted back to front by the game, as it sorts what it draws see-through. A
block drawn with a model shows the cracks of being broken over it, as the game draws them over any block entity,
unless it says `noCracks()`. The model is drawn with the game's own entity shaders, so shader packs (Iris, Oculus) draw
it as they draw a mob.

## What it costs

A model's shape is worked out once, when it is read, and every copy of it shares it; copies painted with one texture
go to the game in one batch. Only what is in sight is drawn, and only what is near enough to see move is animated. A
thing at rest (nothing playing, no code moving it) is not worked out again until it moves; a thing drawn more than once
in a frame is worked out once. A mesh that bends with a skin or moves toward a morph target is the only part worked out
each frame.

## While you build

| Command | What it does |
| --- | --- |
| `/jstech model preview <model>` | opens the preview of a declared model, or of any model file by its path (`jsc:geo/rack.geo.json`) |
| `/jstech model reload` | reads the models again, and nothing else of the resources |

The preview shows the model on a floor of quarter blocks: drag to turn it, the wheel to zoom, shift and drag to move it.
Each animation has its button, which plays it or pauses it; the tree lists the bones, and the one clicked is drawn in
yellow with those it hangs from; the switches show the bones, the locators, the glowing parts alone in the dark, and
the faces as wire; the timeline is dragged to a time; the speed button steps through a quarter, a half, one, two and
four times.

In a development run, a changed model, animation or definition file in a mod's `jsmodels` folder, or a model of a
format only the Core reads anywhere, has the models read again by themselves, at once and alone; an open preview
shows the change and its animation goes on. A changed Bedrock geometry or animation elsewhere reloads every resource,
since GeckoLib reads those too ([While you build](DEVELOPING.md)).

## Reading a model yourself

`ModelReaders.read(file, bytes, beside)` reads any of the formats into a `ModelData`, and needs nothing of the game;
`ModelReaders.animations(file, bytes)` reads an animation file. `ModelPose` places a model's bones, `ModelAnimator`
moves them, and `GeometryBaker` lays out its faces, all pure: a test runs them without the game.

## What can go wrong

- **The data run stops at a model.** It names the file that is not there, the bone the model has none of, or what does
  not read in the file. Save the file again from its program, or name what it has.
- **The model is not drawn.** Its definition was not written (the data run did not run since it was declared), the
  renderer names another model, or the block's render shape is not `ENTITYBLOCK_ANIMATED`. The log of the `models`
  part says when a model was left out and why.
- **The model is drawn black or magenta.** Its texture is not found: name it with `texture(...)`, or put it where the
  file names it, beside the model.
- **An FBX file is refused.** Files before version 7 (2010) name their objects otherwise; export as FBX 7.4 or later.
- **An animation the server plays is not seen by a player who came later.** Orders from the server are moments; play
  what lasts from the driver, from the block entity's state.
- **A piece of armour is drawn as the game's own.** The item has no `KitArmorModel` registered for it: register the
  extensions for every piece of the set.
- **A piece of armour is not drawn at all.** Its model has no bone of the names the wearer's limbs are followed by
  (`armorHead` for a helmet, `armorBody` and the arms for a chestplate, the legs for leggings, the boots for boots).
