# Lighting and effects

Light that has a colour and moves with what makes it, and the smoke, steam, sparks, beams and trails that show a world
at work. Both are kits any mod uses, written natively.

## What exists today

The game's own light (block light and sky light, from 0 to 15) and its own particles. Holograms and screens in the world
are drawn by the Core ([The interface](interface.md)).

## To build

The kits gather what Shimmer, the dynamic-light mods, Immersive Engineering's floodlight, Photon and MadParticle do, in
one place, for any mod.

### Lighting

- **Coloured light**: every light source has a colour, and colours mix in the world: a red alarm lamp tints its room.
  It works without shaders, and with them where they allow.
- **Dynamic light**: what is held lights the way (a torch, a flashlight); mobs that glow and things on fire; vehicles'
  headlights and fired flares; a moving structure's lamps light the world it passes.
- **Spotlights and directional light**: a cone or a beam that reaches far, past the game's fifteen levels, as Immersive
  Engineering's floodlight does.
- **Bloom**: what glows gets a halo, with no shader mod needed.
- **Light as data**: a light declares its colour, intensity, reach and **flicker** (a fluorescent tube starting, a lamp
  failing); it turns on and off by redstone or power, and a lamp can break.
- **The glowing parts of models** feed the bloom and can cast real light ([Models](models.md)).
- **Light counts in the game**: mobs appear and plants grow by its intensity, whatever its colour; grow lights for J's
  Agriculture.
- **Light sensors** read the intensity and the colour, for redstone and the network.
- **The sun's colour tints a world's day**: the reddish day under a red dwarf, the darkening of an eclipse
  ([The world](world.md)).
- **Cost**: light updates in batches and away from the main thread, many dynamic lights cheaply, within a distance.
- **Works with Sodium, Embeddium, Iris and Oculus**, and without them.
- **Each player's settings**: dynamic light on or off, and how strong the bloom is.

### Particles and effects

- **Particle systems as data**: emitters with shapes (from meshes too), a rate and bursts; particles with a speed, **the
  gravity of the dimension, drag by the density of the air, the wind**, colour, size and rotation over their life,
  animated sprites, collision with blocks, light of their own, and emitters inside particles.
- **An effects editor in the game**, for development, as Photon's.
- **Trails**: contrails, tracers.
- **Beams and arcs**: lasers (J's Space's Laser Satellite), electric arcs between points (the arc flash of overvoltage,
  Tesla coils), lightning.
- **Smoke, steam and gas with volume**: plumes that rise and drift with the wind (cooling towers, the chimneys of
  pollution), clouds of gas from a leak in the gas's own colour (chlorine's green), the haze of a dust storm.
- **Effects that follow physics**: a rocket's plume that widens as the air thins and is huge in a vacuum, the plasma of
  a re-entry, bubbles under water.
- **Screen effects**: heat haze near hot things, the shockwave of an explosion, a flash, the screen shaking (with a
  setting), and a vignette for hazards (radiation, too little oxygen).
- **Marks on the world**: scorch after an explosion, soot on walls near chimneys, fading over time.
- **Effects held at a model's locators** and fired by its keyframes, and on the engines of moving structures.
- **Sent compactly**: the server fires an effect for whoever can see it, culled by distance.
- **Cost**: thousands of particles drawn by instancing on the graphics card, a budget per player, and the game's own
  particle setting (all, decreased, minimal) respected.
- **Works with Iris and Oculus**, and without them.
