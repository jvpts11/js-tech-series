# Particles and effects

*Added 2026-10-16.*

The smoke, steam, sparks, beams and trails that show a world at work, as data: an effect is a file of emitters, its
particles moved by the physics of where they are (the dimension's pull, the air's density, the wind, the water they
are in) rather than by curves drawn by hand. The server fires an effect for whoever can see it; a player's game plays
it from its file, thousands of particles at once. An effects editor in the world changes an effect where it plays.

## An effect's file

An effect is `assets/<namespace>/effects/<path>.json`, the effect `<namespace>:<path>`; a resource pack writes over
it.

```json
{
  "anchor": {"face": "up"},
  "reach": 64,
  "emitters": [
    {
      "name": "steam",
      "emission": {"shape": {"type": "disc", "radius": 0.35}, "rate": 40, "life": [3, 5], "speed": [1.0, 1.4]},
      "look": {"sprite": "jscore:steam", "frames": 12, "size": [0.4, 2.0],
               "colour": ["#F4F8FCC8", "#F4F8FC00"], "spin": 20},
      "motion": {"buoyancy": 1.6, "drag": 1.2, "expands": true, "medium": "air"}
    }
  ]
}
```

| Part | What it says |
| --- | --- |
| `anchor` | where on a block it plays when played on one: a `face`, or a `locator` of the block's model, and an `offset` |
| `reach` | how far a player still sees it, in blocks |
| `kind` | `particles` (the plain value), `beam`, `arc`, `mark` or `screen` |
| `emission.shape` | `point`, `sphere` (`radius`), `box` (`size`), `disc` (`radius`), `cone` (`angle`), `line` (`size`), `mesh` (`model`, one of the Core's models: points over its faces) |
| `emission.rate`, `bursts`, `duration` | how many a second; how many at once at times (`{"at": 0.5, "count": 20}`); how long it emits, 0 for as long as it is held (an effect fired once emits its bursts) |
| `emission.life`, `speed` | each particle's life in seconds and the speed it leaves at, metres a second: a number or a range `[min, max]` |
| `emission.children` | the emitters each particle carries, emitting from it: sparks that leave wisps of smoke |
| `look.sprite`, `frames` | a picture of the particle sheet by name, its frames `<name>_0` and on shown over its life; none for the game's plain particle |
| `look.size`, `colour` | keys over its life, spread evenly: blocks across, and `#RRGGBB` or `#RRGGBBAA` |
| `look.spin`, `blend`, `light` | degrees a second either way; `translucent` or `additive`; a light of its own, 15 for full light |
| `look.trail`, `width` | the ribbon it leaves, in seconds of its path, and its width; a beam's or an arc's width |
| `motion.gravity`, `drag`, `buoyancy` | how much of the pull acts (1 falls as a stone); how quickly the air brings it to the wind's speed at sea level, a second; how hard it is lifted at sea level |
| `motion.moved_by` | `{"gravity": true, "air": true, "wind": true}`: what acts on it |
| `motion.collide`, `expands`, `medium` | whether it stops at blocks; whether it widens as the air thins, as a plume of gas does, up to six times in a vacuum; `any`, `air` (dies in water) or `water` (bubbles) |
| `extra` | an arc's `jagged` and `branches`; a mark's or the screen's `radius`; a mark's `fade`, a screen effect's or a beam's time; the screen's `flash`, `shake` and `shock`, 0 to 1; a beam's or an arc's `length` when it is aimed nowhere |

A particle in air drags towards the wind and is lifted by as much as the air is dense, so what is lighter than the
air settles rising at `(buoyancy - pull) / drag`, and nothing rises in a vacuum; in water it settles at once to the
water's stillness, so bubbles rise at `buoyancy / drag`. The air's density comes from the place's pressure and
warmth ([Heat](HEAT.md)), the pull from the dimension's rules ([World](WORLD.md)), and the wind from `Winds`: a few
metres a second turning over the days, stronger in rain and storms and higher up, none in a vacuum, or what a mod's
`IWindSource` says.

## The Core's effects

`jscore:smoke`, `steam`, `sparks`, `beam`, `trail`, `arc_flash` (fired by an arc flash of overvoltage), `explosion`,
`gas_cloud` (coloured by whoever fires it: chlorine's green), `bubbles`, `rocket_plume`, `reentry_plasma`, `scorch`,
`soot`, `laser` and `lightning`. Steam is the game's smoke brightened towards white as the particle sheet is made
(`jscore:brightened` in `atlases/particles.json`), with no picture of its own.

## Playing effects

From the server, for every player within its reach:

```java
Effects.at(level, rl("arc_flash"), Vec3.atCenterOf(pos)).send();
Effects.at(level, rl("laser"), muzzle).to(target).colour(0xFFFF3020).send();
Effects.at(level, rl("gas_cloud"), leak).colour(0xFF60C040).scale(2).send();
Effects.onBlock(level, rl("cooling_tower_steam"), towerTop).send();
```

On the player's own game, once or held while something goes on:

```java
ClientEffects.play(rl("sparks"), at, new Vec3(0, 1, 0));
ClientEffects.holdOnBlock(this, rl("cooling_tower_steam"), worldPosition);   // while it works
ClientEffects.hold(this, rl("rocket_plume"), engine::position, engine::down); // a place that moves
ClientEffects.release(this);                                                 // when it stops
```

An animation's particle event ([Models](MODELS.md)) that names one of the Core's effects plays it at its locator;
one that names a particle of the game shows that. What the Core does to the screen is `ScreenEffects`: `fire` for a
flash, a shake and a shockwave, and `vignette(key, colour, strength)` said each tick a hazard lasts.

## The screen and the world

- **Flash and shake**: from an effect's `screen` emitter, fading over its time, as strong as the player is near.
- **Shockwave**: a ring that bends the view as it goes out across the screen.
- **Heat haze**: the air shimmers above blocks hot enough to glow ([Heat](HEAT.md)), the nearest four.
- **Vignette**: a hazard's colour closing in from the screen's edges.
- **Marks**: scorch and soot laid on the faces of solid blocks around, darkest nearest the middle, fading over their
  time.

The bends of the view are a shader over the world as it was drawn; the HUD stays sharp over them.

## The effects editor

`/jstech effect edit <effect>` opens the effects editor on the block looked at: the effect plays there, where it will
play, with the world's wind and air, and a card beside it turns each emitter on and off and changes the chosen one's
rate, life, speed and size (a beam's or a mark's width, time, reach and strength), and what moves it. The game's
debug lines in the corner say what it costs (its particles against the budget, its time a frame) and what acts on it.
Save writes the effect's file in the mod's own source, in a development run.

`/jstech effect new` opens the card of a new effect at the block looked at: where on it the effect starts (a face, or
a locator of its model), its file, and a starting point (smoke, steam, sparks, a beam, a trail, nothing) that plays
there at once. Create writes the file, under the block's mod, and opens it in the editor. `/jstech effect play
<effect>` plays one there.

## Cost and settings

Every particle of a layer is drawn in one batch, the see-through ones back to front. *Added 2026-10-22:* the
graphics card draws them: one square is sent to it once, and each particle hands over only where it is, its size,
its picture, its colour, its turn and its light, so thousands cost the game little. Under a shader pack, or with the
player's *Draw alike things together* turned off ([Models](MODELS.md)), they are laid out one by one as before. The
player's
`jstech-effects.toml` says how many particles may live at once (6000 unless they say), how hard the screen shakes,
how bright a flash is, and whether heat haze and shockwaves bend the view; the game's own particle setting (all,
decreased, minimal) thins out what is emitted, and near the budget the farthest effects emit fewer. An effect too far
to be seen emits nothing.

## What can go wrong

- **An effect plays nothing.** No file holds it (the log says which files did not read and why), or it is fired once
  with a rate and no bursts: a one-shot effect emits its bursts, and its rate only for its `duration`.
- **A particle shows as the game's plain one.** Its sprite is not on the particle sheet: pictures go in
  `textures/particle/`, and frames are named `<name>_0` and on.
- **Smoke does not rise.** Its `moved_by` leaves out the air, or it is in a vacuum, where nothing rises.
- **Save says the mod is not loaded from its folders.** The effect's mod is a jar in this run, not a project.
