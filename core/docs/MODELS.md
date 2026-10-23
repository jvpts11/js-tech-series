# Models

*Added 2026-10-16.*

Animated models for blocks, items, entities and armour, read from the files artists already make: a Blockbench
project, a Bedrock or GeckoLib geometry, glTF from Blender, FBX from Maya, 3ds Max, Cinema 4D or Blender, and OBJ from
anything; and the files of CAD programs and scenes, STL, 3MF, STEP and USD, each part a bone. Every reader is the
Core's own, written from each format's description; none needs the program that made the file. A model has bones in a
tree, cubes and meshes on them, locators, skins and morph targets; it plays animations
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
| STL, text or binary | `.stl` | each solid of a text file a part, the colour of each triangle where a binary file gives one (as VisCAM, SolidView or Materialise write it) |
| 3MF | `.3mf` | the objects of an assembly as parts, placed by its components and its build; colours from base materials or colour groups; textures by the file's own picture places; the parts the production extension keeps in files of their own |
| STEP (ISO 10303-21: AP203, AP214, AP242) | `.step`, `.stp` | solids' faces laid on their own surfaces (planes, cylinders, cones, spheres, tori, B-splines, surfaces of revolution and of extrusion), faceted solids and the triangles AP242 carries, the products of an assembly as parts, the colours of solids and faces |
| USD, text, binary or packed | `.usda`, `.usdc`, `.usdz`, `.usd` | the prims that place things as bones, meshes with their normals, picture places and colours, the colour or texture of a UsdPreviewSurface, prims referenced from the same file or from files beside it or in its package, skeletons with the meshes they bend, transforms and skeletal animations over time |

Every format is turned into the same space, the game's: Y up, a model facing north (toward -Z) as it is drawn, sixteen
pixels to a block. Bedrock's files count X the other way, and are turned round as they are read. glTF and OBJ count in
metres, a block each; FBX says its own unit and which way is up, and its model hangs from one bone, `fbx_axes`, that
turns its axes into the game's. STL says nothing of its unit and is taken in millimetres, as CAD programs write it; 3MF
and STEP say theirs; USD says its own (`metersPerUnit`, centimetres when it does not) and which way is up. A CAD file,
Z up, is turned to stand Y up, on its origin.

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
| `scale(times)` | it is drawn that many times bigger than its file says, a metre a block |
| `triangles(most)` | it is drawn with that many triangles at most, its meshes brought down to them as it is read |
| `far(blocks, most)` | from that many blocks away, it is drawn with that many triangles at most |

The data run writes its definition to `assets/<mod>/jsmodels/<path>.model.json`, after reading the model: a file that
is not there, a model that does not read, a bone named that the model has none of, or a model of more triangles than
it may have (below) stops the run with what to fix.
Add the provider when your mod does not gather its data through `ContentData`:

```java
generator.addProvider(event.includeClient(), new ModelDefinitionProvider(output, CONTENT, existingFiles));
```

`ModelDefinitionProvider.check(model, existingFiles)` makes the same checks of one declared model, for a provider of
your own.

## From CAD programs and scenes

*Added 2026-10-22.* A part drawn in a CAD program (Fusion, SolidWorks, FreeCAD, Onshape, a slicer) or a scene from
Blender, Maya or Houdini is declared as any model is, and comes into the game as one of its own:

```java
public static final DeclaredModel PUMP = CONTENT.model("pump")
        .file("jsmodels/pump.3mf")                       // or .stl, .step, .usdz...
        .animations("jsmodels/pump.animation.json")      // its parts are its bones
        .triangles(2_000)                                // at most, near
        .far(32, 600)                                    // and from 32 blocks away
        .register();
```

- **Its parts are its bones.** Each part of an assembly (a 3MF object, a STEP product, a USD prim, a text STL's solid)
  is a bone, its pivot at the part's own origin, placed as the file places it and hanging from the part that holds it;
  an animation file turns the impeller by naming it. The preview lists them as *Parts, from the file*.
- **Its colours are textures in the game's look.** A CAD file carries colours, not pictures: each colour becomes a
  texture of sixteen pixels a side, the colour with the lighter and darker pixels of the game's blocks, laid on the
  faces from the sides as a block's are, a pixel a sixteenth of a block at the size it is drawn. A part with no colour
  is a bright steel. A file with pictures of its own (3MF, USD) keeps them.
- **Its size is its real one.** A metre is a block, so a pump 600 mm long is 0.6 blocks long; `scale(times)` draws it
  bigger or smaller.
- **Its triangles are what it says.** `triangles(most)` brings its meshes down to that many as they are read, the
  edges that shape it least folded first, so a cylinder of many sides keeps its outline; `far(blocks, most)` gives it a
  second shape of fewer, drawn from that far away, with the same bones, so it moves the same. Cubes are drawn as they
  are. A model that says nothing may have at most 20 000 triangles (`ModelDefinition.Detail.MOST_UNSAID`): the data run
  stops a file past that, as the export of every screw of a machine would be, and one whose cubes alone are more than
  it says.
- **STEP is laid out, not modelled.** Each face is laid with triangles on its own surface, no farther from it than a
  hundred and fiftieth of the part's size (and never coarser than a sixty-fourth of a block); a curved edge is sampled
  more where it bends, once, so the faces either side of it meet. Nothing is cut, joined or mended: the program that
  exported the file worked out every edge, and the reader lays out what it says. A face on a surface it does not know,
  or whose loops do not close, is left out, and the rest of its solid is drawn; `StepReader.readCounted(file, bytes)`
  says how many faces were laid out and how many left out.
- **USD is composed as far as a model needs.** References to prims in the same file, in files beside it or in its
  package are taken in where they are referenced; sublayers, variants other than the default, payloads and the other
  arcs of composition are let go. What moves over time, transforms and skeletal animations, is one animation,
  `animation`, which loops.

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
| `extras(...)` | what it draws besides its model each frame, where the model stands (`IKitExtras`) | nothing |
| `box(entity -> aabb)` | the box it is drawn within, for a model whose parts ride out of it | the model's own |
| `drawnWhile(entity -> shown)` | draws it only while that holds, as a block whose own model shows some of its forms | always |
| `upAndDownAboutMiddle()` | faced up or down, it turns about the middle of its block, not of its floor | about its floor |
| `viewDistance(blocks)` | how far away it is drawn | 64 |
| `animateDistance(blocks)` | how far away it is animated; farther, it keeps its last pose | 48 |

A family of files laid out for GeckoLib (a `GeoLook`) is drawn by the Core's models as it is:
`KitBlockEntityRenderer.look(MY_LOOK)`. The model, the texture and the animation file are the ones the look names.

*Added 2026-10-23:* **What a machine holds**, each part a model of its own, is drawn by its `extras`. In there,
`KitRenderer.toBone(instance, bone, poses)` brings the drawing onto one of the model's bones as this frame has it, so
a disc rides out with its drive's tray, and `KitAnimations.part(entity, name, model)` gives each such part a model
instance of its own, kept with the block entity, for the code to move as it moves the main one:

```java
.extras((computer, instance, poses, buffers, light, overlay, partialTick) -> {
    for (final InstalledPart part : computer.parts()) {
        poses.pushPose();
        if (KitRenderer.toBone(instance, part.seat(), poses)) {
            KitRenderer.draw(KitAnimations.part(computer, part.name(), part.model()), ModelDefinition.Variant.NONE,
                    computer, poses, buffers, light, overlay, null, computer.getLevel(), true);
        }
        poses.popPose();
    }
})
```

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
driver that adds movers says so with `movesBones()`, so its model is never taken to be at rest. A driver may also move
the bones itself, after the animations, in its `move(thing, pose, seconds)`: a lamp shown while the machine runs, a
part hidden while its slot is empty, a fan turned as fast as the machine goes (saying `movesBones()` too).

**Expressions** in keyframes are Molang, as Bedrock's files write it: numbers and arithmetic, comparisons, `&&`, `||`,
`?:`, `??`, `math.*` (angles in degrees), statements and `return`, `variable.`/`v.`, `temp.`/`t.`, `context.`/`c.`,
and `query.`/`q.`. `query.anim_time` and `query.life_time` are given; the code gives the rest:

```java
animator.context().query("load", context -> machine.load());   // read as query.load
animator.context().value("pressure", tank.pressure());          // a value handed over this frame
```

A query of code is worked out once a frame, however many keyframes read it; the expressions themselves are read into
a tree once, as the model loads.

*Added 2026-10-22:* an entity's model, and armour on whoever wears it, reads the entity's queries ready-made, under
the names Bedrock gives them (`EntityQueries.NAMES` lists them):

| Query | What it reads |
| --- | --- |
| `ground_speed`, `vertical_speed` | blocks a second, across and up |
| `limb_swing` (`modified_distance_moved`), `limb_swing_amount` (`modified_move_speed`) | how far its limbs have swung, and how hard they swing now, 0 to 1 |
| `is_on_ground`, `is_in_water`, `is_in_water_or_rain`, `is_in_lava`, `is_on_fire`, `is_sneaking`, `is_sprinting`, `is_swimming`, `is_riding`, `is_alive`, `is_baby` | 1 or 0 |
| `head_x_rotation`, `head_y_rotation`, `body_y_rotation`, `yaw_speed` | degrees, and degrees a second |
| `hurt_time`, `health`, `max_health` | ticks left of its last hurt, and its health |
| `distance_from_camera`, `time_of_day`, `moon_phase` | blocks to the viewer, the day from 0 to 1, the moon from 0 to 7 |

So a walk is written in the file, with no code:

```json
"rotation": ["math.sin(query.limb_swing * 38) * 40 * query.limb_swing_amount", 0, 0]
```

**The game's own creatures** are drawn with a model of the Core's when a resource pack says so, in
`assets/<namespace>/jstech/entity_models/<name>.json`; their animation reads their queries as above:

```json
{
  "entity": "minecraft:pig",
  "model": "mypack:pig",
  "plays": "animation.pig.move"
}
```

Only creatures, the entities the game draws as living things, can be drawn so; a file naming a model no pack holds
leaves the creature as the game draws it.

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
unless it says `noCracks()`. Under a shader pack (Iris, Oculus) the model is drawn with the game's own entity shaders,
so the pack draws it as it draws a mob; without one, the copies of a model in the world are drawn together by a shader
of the Core's that draws them just as those do (*What it costs*, below).

*Added 2026-10-22:* **textures that move and glow.** A texture of frames laid one under another, with a `.mcmeta`
beside it as the game's block textures have (`"animation": {"frametime": 4}`, and a list of `frames` if it likes),
shows the frame of the tick, every model painted with it in step; frames are not blended into each other. A texture
with a glowing texture beside it, the same name with `_e` before `.png` (`pump.png` and `pump_e.png`), has that drawn
over it at full light, its clear pixels showing nothing: lamps, screens and hot coils that glow in the dark.
`KitTextures.of(texture)` says what a texture was read as.

**Layers** a mod lays over a model are drawn after it, each time it is drawn, in the model's space; they redraw the
model's faces into a way of their own, or draw anything else there:

```java
KitLayers.add(rl("reactor"), (instance, faces, buffers, light, overlay) ->
        faces.into(texture -> RenderType.energySwirl(SHIELD, 0, 0), LightTexture.FULL_BRIGHT, overlay));
KitLayers.everywhere(myOutline);   // over every model of the Core's
KitLayers.remove(myOutline);       // and taken off again
```

A model with a layer over it is drawn by itself, never in one batch with the others like it, so a layer laid only for
a while (an outline as something is chosen, a shield while it is up) is best taken off once it is not needed.

**The player's arms in first person**: an item's model with a bone named `player_right_arm` or `player_left_arm` has
that arm of the player drawn at the bone's pivot while it is held in first person, in the player's own skin and
sleeve, wide or slim, so a tool is held by hands that move with its animation.

## What it costs

A model's shape is worked out once, when it is read, and every copy of it shares it; copies painted with one texture
go to the game in one batch. Only what is in sight is drawn, and only what is near enough to see move is animated. A
thing at rest (nothing playing, no code moving it) is not worked out again until it moves; a thing drawn more than once
in a frame is worked out once. A mesh that bends with a skin or moves toward a morph target is the only part worked out
each frame.

*Added 2026-10-22:* **copies drawn together.** Every copy of a model drawn in the world in a frame (block entities,
entities, turning parts) is drawn in one call for each of its pictures: its faces wait on the graphics card from the
first time it is drawn, and each copy hands over only its light, its overlay and where each of its bones is. A rack of a
hundred alike drives costs about what one does. A copy drawn anywhere else (in a hand, a screen, the inventory) is
drawn one by one, and so is one being broken (its cracks), one with an outline, a see-through model, one with a mesh
that bends or morphs, and one with layers laid over it. A shader pack draws everything one by one, and so does the
player's setting `drawing.instancing` in `jstech-drawing.toml`, *Draw alike things together*, turned off for a graphics
card that draws them wrong. `KitInstancing.drawnLastFrame()` and `callsLastFrame()` say how many copies were drawn
together in the last frame, and in how many calls.

## While you build

| Command | What it does |
| --- | --- |
| `/jstech model preview <model>` | opens the preview of a declared model, or of any model file by its path (`jsc:geo/rack.geo.json`) |
| `/jstech model reload` | reads the models again, and nothing else of the resources |

The preview shows the model on a floor of quarter blocks: drag to turn it, the wheel to zoom, shift and drag to move it.
Each animation has its button, which plays it or pauses it; the tree lists the bones, each with its triangles, and the
one clicked is drawn in yellow with those it hangs from; under it, the model's triangles against the most it may have,
with a bar, how it is drawn far away, how big it is, and its colours or textures; the switches show the bones, the
locators (of a model that has some), the faces as wire, the model drawn far away (of one that has it), and the glowing
parts alone in the dark; the timeline is dragged to a time; the speed button steps through a quarter, a half, one, two
and four times. A short screen keeps one animation's button and lets the lines under the tree go from the last.

In a development run, a changed model, animation or definition file in a mod's `jsmodels` folder, or a model of a
format only the Core reads anywhere, has the models read again by themselves, at once and alone; an open preview
shows the change and its animation goes on. A changed Bedrock geometry or animation elsewhere reloads every resource,
since GeckoLib reads those too ([While you build](DEVELOPING.md)).

## Reading a model yourself

`ModelReaders.read(file, bytes, beside)` reads any of the formats into a `ModelData`, and needs nothing of the game;
`ModelReaders.animations(file, bytes)` reads an animation file. `ModelPose` places a model's bones, `ModelAnimator`
moves them, `GeometryBaker` lays out its faces, `ModelShaping.shape(data, detail, far)` brings it to the triangles its
detail says and `ModelShaping.overBudget(data, detail)` says what keeps it from them, all pure: a test runs them
without the game.

## What can go wrong

- **The data run stops at a model.** It names the file that is not there, the bone the model has none of, or what does
  not read in the file. Save the file again from its program, or name what it has.
- **The model is not drawn.** Its definition was not written (the data run did not run since it was declared), the
  renderer names another model, or the block's render shape is not `ENTITYBLOCK_ANIMATED`. The log of the `models`
  part says when a model was left out and why.
- **The model is drawn black or magenta.** Its texture is not found: name it with `texture(...)`, or put it where the
  file names it, beside the model.
- **An FBX file is refused.** Files before version 7 (2010) name their objects otherwise; export as FBX 7.4 or later.
- **The data run stops at a model of too many triangles.** Say how many it is drawn with, `triangles(...)`, and how
  many far away, `far(...)`; or export it coarser. Its cubes are never brought down: make it of fewer.
- **A CAD model is the size of a grain or of a house.** An STL is taken in millimetres; a file exported in another unit
  without saying so needs `scale(...)` (an STL in metres, `scale(1000)`).
- **A face of a STEP model is missing.** It is on a surface the reader does not know, or its edges do not close;
  `StepReader.readCounted(file, bytes)` says how many were left out. Export it again, or as AP242 with its triangles.
- **An animation the server plays is not seen by a player who came later.** Orders from the server are moments; play
  what lasts from the driver, from the block entity's state.
- **A piece of armour is drawn as the game's own.** The item has no `KitArmorModel` registered for it: register the
  extensions for every piece of the set.
- **A piece of armour is not drawn at all.** Its model has no bone of the names the wearer's limbs are followed by
  (`armorHead` for a helmet, `armorBody` and the arms for a chestplate, the legs for leggings, the boots for boots).
