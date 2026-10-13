# Explosions and fire

Explosions that push through blocks by their strength and travel at the speed of sound, and fire that spreads by what
burns, by the air and by the wind, with the real ways of putting it out. They are in the spirit of HBM's Nuclear Tech's
explosions, with real physics.

## What exists today

The game's own explosions and fire.

## To build

### Explosions

- **Energy in joules**, with its TNT equivalent, decides an explosion; there is no fixed radius.
- **The blast pushes through blocks by the strength of each**: concrete holds, glass gives, and behind a strong wall one
  is safe.
- **The shock wave travels at the speed of sound**: the harm reaches each place in its own time, glass breaks far away,
  and the boom arrives after the flash ([The interface](interface.md#the-sound-kit)).
- **Overpressure on entities by distance**, as in reality: close by it kills, further off it knocks down and wounds.
- **Fragments** of the casing, of its own material, and **debris** thrown.
- **Destruction spread over several ticks**, within a budget, so a great explosion never stalls the server.
- **The medium counts**: **in a vacuum there is no shock wave**, only fragments, heat and radiation; under water the
  shock carries far.
- **Chains**: explosives struck by a blast go off.
- **Craters and scorch marks** ([Lighting and effects](lighting-and-effects.md)).
- **Claimed land is respected** ([The platform](platform.md#permissions-and-protection)), and each mod has its
  consequence setting.

### Fire

- **Fire spreads by the material, the oxygen and the wind**: wood burns fast, nothing burns in a vacuum, and pure oxygen
  makes an inferno ([Sealed rooms](sealed-rooms.md)).
- **What burned stays burned**: charred blocks and ash, instead of simply vanishing.
- **The real classes of fire**: solids, flammable liquids, **electrical** (water conducts, and is dangerous), **metals**
  (sodium and magnesium need dry powder) and cooking oils.
- **Extinguishers by class**: water, foam, carbon dioxide, dry powder.
- **Sprinklers** joined to water pipes, and **smoke and heat detectors** as sensors, for redstone and the network.
- **The smoke of a fire** rises and drifts with the wind ([Lighting and effects](lighting-and-effects.md)).
