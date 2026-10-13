# Models

The Core's own animated models, for blocks, items, entities and armour, in place of GeckoLib, as the series' README
promises for every required dependency. They read the formats artists already work in, from Blockbench to Blender,
Maya, 3ds Max and Cinema 4D.

## What exists today

The game's own block and item models, which the Core's data generation writes. Animated models (J's Computers'
Mainframes, racks, drives and encoders) are drawn with GeckoLib, a required dependency of J's Computers; the Core has an
optional integration with it.

## To build

The kit gathers what GeckoLib does, and what game engines do with the models of the big 3D programs, in one place, for
any mod, written natively.

### Formats

The files of each 3D program are its own, change with its versions, and are partly closed; no game reads them directly.
The kit reads instead the **formats those programs export**, each with a reader of the Core's own:

| Format | Exported by | What it carries |
| --- | --- | --- |
| **Blockbench**, its own format and the geo format of GeckoLib and Bedrock | Blockbench | bones, cubes, animations; today's models come across unchanged |
| **glTF 2.0**, the main one | Blender natively; Maya, 3ds Max and Cinema 4D with an exporter | meshes, skeletons with weights, animations, morph targets, materials |
| **FBX** | all four | the same, read by a reader of the Core's own |
| **OBJ** | all four | still meshes |
| **USD**, later | all four, more and more | whole scenes |

### What a model holds

- **Bones in a hierarchy**, cubes with UVs per face, meshes, pivots and rotations, and **locators**: named points for
  particles and for things held.
- **Skinned meshes**, which bend smoothly with the bones' weights, and **morph targets** (Blender's shape keys), besides
  rigid bones.
- **Things held at a locator**: an item or a block drawn at a point of the model, as the player's own disk in a drive's
  bay.
- **Variants by state**, which swap textures or show and hide bones.

### Animation

- **Keyframes** with every easing curve Blockbench has, loop modes (once, loop, hold the last frame) and speed.
- **Several controllers at once**, on different bones, in order, with **smooth transitions** between animations.
- **Animations triggered by the server**, seen by everyone watching: a door opening, a drive ejecting.
- **Expressions in keyframes**, driven by the state of what the model shows: a fan faster with the load, a lamp blinking
  with the work. The expression language is the Core's own, reading the Molang of Bedrock files that are brought in.
- **Events inside an animation**: keyframes of sound (through the Core's sound), of particles, and instructions to code.
- **Bones moved by code**, as a layer over the animation: a turret aimed at a target, a gauge's needle at its value.

### Drawing

- **Parts that glow in the dark**, with shaders too, feeding the bloom and the light
  ([Lighting and effects](lighting-and-effects.md)).
- **Layers**: overlays such as the **cracks of a damaged machine**, armour layers, and sorted transparency.
- **One model for a block, an item (its icon still or animated), an entity, armour and the hand.** What a block shows in
  the world never leaks into its item, and the other way round.
- **Cost**: meshes baked once; animations worked out only for what is seen; **no animation far away**; and
  **instancing**, so a rack of a hundred identical drives is drawn as one.
- **Works with shaders** (Iris, Oculus).
- **Formed multiblocks** become one model of this kind ([Multiblocks](multiblocks.md)).

### Tools and moving over

- **Live reload** of models and animations, and a **preview screen** in the game, for development.
- **Models declared once**, with the data generation.
- **Moving over**: J's Computers' Mainframes, racks, drives and encoders come across to the kit, and the Core's optional
  integration with GeckoLib goes away.

### Connected textures

In the spirit of CTM and Continuity:

- **Textures that join between alike blocks**: glass with no frame between panes, multiblock casings that read as one,
  floors and walls in large patterns.
- **Declared as data**, and reading the format of Continuity and CTM, for resource packs.
- **Joins by rule**: the same block, the same tag or a list, diagonals too.
- **They work in multipart spaces and on microblocks** ([Multipart](multipart.md)).
- **They step aside for Continuity and CTM** when those are installed ([Modpacks](modpacks.md)).
