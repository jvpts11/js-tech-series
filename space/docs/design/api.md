# The API

What J's Space opens to other mods, add-ons and datapacks. Its API is versioned as every mod's of the series: an
integer, what is new marked experimental in its first release, an "API" section in the changelog, and a test of its
public surface.

## What exists today

J's Space's API is at version 18, in `dev.jstech.space.api`, and all of it is experimental. It holds the registries an
add-on adds to, each with the model it holds, and a data pack's files can add to most of them:

- The tabs of its advancements (`SpaceAdvancementTab`: Leaving the Earth, The Moon and Mars), with each one's root,
  which an add-on hangs its own advancements under.
- Worlds (`SpaceWorld`, `SpaceWorlds`), the generators that shape their ground and the modifiers laid over it
  (`WorldGenerators`, `WorldModifiers`), what can be known of a world (`WorldProperties`), what kills there
  (`WorldDangers`) and its landmarks (`SpaceLandmark`, `SpaceLandmarks`).
- Life support: pressure suits (`SpaceSuit`, `SpaceSuits`), the air their tanks hold and the cold and heat they stand,
  and the suit modules (`SuitModule`, `SuitModules`), which may also cut the Sun's glare where no air does.
- Rocket parts (`RocketPart`, `RocketParts`), the kits of blocks they are built of (`RocketKit`, `RocketKits`) and the
  propellants they burn (`Propellant`, `Propellants`).
- Starship parts (`ShipPart`, `ShipParts`), their kits of blocks (`ShipKit`, `ShipKits`), the finishes a ship is
  painted in (`ShipFinish`, `ShipFinishes`), the makers of its parts (`ShipMaker`, `ShipMakers`), and each part's
  figures: mass, thrust, lift, power, crew, the weight its wings hold up and whether its gear rolls.
- The kinds of satellite and of probe (`SatelliteTypes`, `ProbeTypes`), the orbits they take (`OrbitKind`) and their
  sizes (`SpacecraftSize`).

An add-on checks the number, `JsSpaceApi.VERSION`, before it uses what a later version adds.

## To build

### Registries

Everything is declared in explicit builders; nothing registers itself by annotation.

| Registry | What it holds | Who adds to it |
| --- | --- | --- |
| **Worlds** | a world as data: its system, its generator and modifiers, its parameters, biomes, minerals, landmarks, sky and conditions | datapacks and add-ons; a pack removes or adds worlds by data, not by setting |
| **Generators** | the processes that shape a world's ground, including the **ocean world generator** the series' own worlds don't use | add-ons |
| **Modifiers** | what is laid over a generator | add-ons |
| **Landmarks** | unique named landforms | datapacks and add-ons |
| **Terraforming processes** | the families of terraforming and their stages | add-ons |
| **Satellite kinds** | mission modules | add-ons; J's Warfare adds its defence satellites |
| **Probe kinds** | | add-ons |
| **Rocket parts** | engines, tanks and the other multiblock parts | add-ons |
| **Starship sections** | including weapons | add-ons; J's Warfare adds its weapons |
| **Discovery properties** | what can be known of a world, and which instrument reveals it | add-ons |

Kinds of Operation are registered through J's Core's registry; computers and hardware through J's Computers' registries.

### Events

On J's Core's event bus, for any mod to react to: a launch, a landing, a stage lost, a world's property discovered, a
milestone reached, a structure found, a terraforming stage completed, a satellite released.

### Data

Each world's dimension rules are J's Core's datapack files; materials, minerals and fluids live in J's Core's catalogue,
which J's Space only says it uses.
