# J's Core: the design

This folder contains the design spec of J's Core. The entire library is made based on these documents, with the sole
purpose of not only making every system that will exist sane and easy to build by anyone, but also of leaving no doubt
about the things yet to be made.

Each page covers one subject. Inside a page, what the Core already does comes first, and what is designed but not built
yet is in a section called **To build**. Much of the Core is already built and explained, with code, in its programmers'
documentation ([J's Core documentation](../README.md)); these pages say what each part is for and the rules behind it,
and link to that documentation instead of repeating it. Every number in these pages is a first estimate, to be tuned in
play.

## The pages

| Page | What it covers |
| --- | --- |
| [Overview](overview.md) | What the Core is: the platform, the series' model and shared content; its principles. |
| [The platform](platform.md) | Content, text and colour, state and saves, settings, commands, teams and owners, time and diagnostics. |
| [Progression](progression.md) | Axes, eras as a registry, progression kept per team, milestones, knowledge and the knowledge gate, naming. |
| [The API](api.md) | What the Core promises, the series' package, maturity, the test of everything reachable, the example mod. |
| [Materials](materials.md) | The registries of materials, forms, properties, mineral families and host rocks; elements, isotopes, exotic forms; the shared catalogue; the J's Tech creative tab. |
| [States of matter](states-of-matter.md) | Items, liquids, gases, supercritical fluids, slurries, plasmas and exotic matter, and containment. |
| [Energy](energy.md) | The energy kit and J's Energy: voltage classes, loss, the wrong class, protection, speaking FE, the basic cable and battery. |
| [Cables and lines](cables-and-lines.md) | The cable block, the series' lines, grids as a registry, lines that group, the basic lines, how each end works, overhead lines. |
| [Multipart](multipart.md) | Block spaces with many parts, microblocks and facades. |
| [Conveyors](conveyors.md) | Items on belts, chutes and lifts, worked as they pass. |
| [Mechanical power](mechanical-power.md) | Rotation in real units: shafts, gears, belts, flywheels, shear pins. |
| [Heat](heat.md) | Temperature in kelvin, conduction, radiators in a vacuum, boilers, changes of state, thermal views. |
| [Machines](machines.md) | The machine framework: resources, sides, never jammed, recipes, control, setting up fast, upgrades. |
| [Multiblocks](multiblocks.md) | Shapes by pattern or by rule, the inside that counts, ports, forming, building, multiblocks as data. |
| [Networks and Operations](networks-and-operations.md) | The data network's model, peripheral links, Operations, machines of any mod on the network, state as tables. |
| [Redstone and signals](redstone-and-signals.md) | Sixteen channels in a cable, logic gates and circuits, signals by frequency, every sensor on redstone. |
| [The world](world.md) | Terrain and biomes, veins, reservoirs, structures, sky, day and weather, the physics kit, worlds that change, chunks. |
| [Hazards](hazards.md) | The hazard kit, hazards of items, the lungs, treatment, radiation and its kinds, dissonance, pollution. |
| [Explosions and fire](explosions-and-fire.md) | Blasts through blocks at the speed of sound, fire by material, air and wind, and its classes. |
| [Sealed rooms](sealed-rooms.md) | Rooms that hold real gases, under water too, what the air does, and the Atmosphere Detector. |
| [Moving structures](moving-structures.md) | Structures of real blocks that move, by rule or by physics, and living aboard them. |
| [Entities and vehicles](entities.md) | Vehicles of one piece, robots, creatures, projectiles and worn modules. |
| [Tools and items](tools-and-items.md) | Tools with modes, mining and building over an area, tools on energy, fuel or air, items that answer keys. |
| [The interface](interface.md) | Screens and components, themes, fonts, motion, overlays, sound, accessibility. |
| [Models](models.md) | Animated models of the Core's own, read from the formats of Blockbench, Blender, Maya, 3ds Max and Cinema 4D. |
| [Lighting and effects](lighting-and-effects.md) | Coloured and dynamic light, bloom, particles, beams, smoke and screen effects. |
| [Manuals](manuals.md) | The guide framework, 3D and animated scenes, finding one's way, export to the web. |
| [Modpacks](modpacks.md) | Everything as data, scripts, unification, tools for packs, and stepping aside for other mods. |
| [For those who make mods](developers.md) | Capabilities, calls from screens, work off the tick, codecs, errors that teach, inspectors, the Gradle plugin. |
| [Low level](low-level.md) | Reaching into the game, and native animated models. |
| [Implementation](implementation.md) | Performance, settings and state. |
| [The series](series.md) | Who depends on whom, and what each mod takes from the Core. |

## The order of building

The Core grows with the mods that need it, so each part is built when its first real user starts, by its dependencies:

1. **Order in the house**: the series' package and the wider promise, with the test of everything reachable; J's
   Computers' settings and programming languages move to J's Computers; telemetry leaves the peripheral types; the J's
   Tech creative tab; errors that teach and the checks of the data generation. [The API](api.md),
   [The platform](platform.md), [For those who make mods](developers.md)
2. **Matter**: the registries of materials, the states of matter, the shared catalogue, and containment.
   [Materials](materials.md), [States of matter](states-of-matter.md)
3. **Energy and lines**: the energy kit and J's Energy, protection, the converters, the basic cable and battery; grids
   as a registry, how each end works, lines that group, the basic lines, overhead lines; redstone and signals.
   [Energy](energy.md), [Cables and lines](cables-and-lines.md), [Redstone and signals](redstone-and-signals.md)
4. **Machines**: the machine framework, mechanical power, heat, multiblocks, multipart, conveyors.
   [Machines](machines.md), [Mechanical power](mechanical-power.md), [Heat](heat.md), [Multiblocks](multiblocks.md),
   [Multipart](multipart.md), [Conveyors](conveyors.md)
5. **Progression**: eras as a registry, the axes moving by play, progression per team, milestones, the knowledge gate,
   naming. [Progression](progression.md)
6. **The ground**: veins, reservoirs, strata, terrain and biomes, structures, fields over chunks, world events, chunk
   loading and the Space Persistor. [The world](world.md)
7. **Hazards**: the kits, kinds of damage, ionizing radiation, pollution, hazards of items; explosions and fire.
   [Hazards](hazards.md), [Explosions and fire](explosions-and-fire.md)
8. **Low level and looks**: reaching into the game; the Core's own models in place of GeckoLib, connected textures;
   lighting and effects. [Low level](low-level.md), [Models](models.md),
   [Lighting and effects](lighting-and-effects.md)
9. **Other worlds**: sky, day and weather per dimension, rules by altitude, the physics kit, travelling between
   dimensions; sealed rooms. [The world](world.md), [Sealed rooms](sealed-rooms.md)
10. **Things that move**: moving structures, entity vehicles, robots, creatures, projectiles, worn modules; tools and
    items. [Moving structures](moving-structures.md), [Entities and vehicles](entities.md),
    [Tools and items](tools-and-items.md)

The interface, the manuals, the settings, permissions, the tools for modpacks and for those who make mods grow
alongside every step ([The interface](interface.md), [Manuals](manuals.md), [The platform](platform.md),
[Modpacks](modpacks.md), [For those who make mods](developers.md)).
