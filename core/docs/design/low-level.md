# Low level

Reaching into the game where an event or an API does not, and drawing animated models with the Core's own code. Both
live in J's Core alone, so the other mods of the series ask the Core instead of patching the game themselves.

## What exists today

Nothing reaches into the game: the Core works through NeoForge's events and APIs only. Animated models (J's Computers'
Mainframes, racks, drives and encoders) are drawn with GeckoLib, a required dependency of J's Computers.

## To build

### Reaching into the game

- **Mixins and access transformers live only in the Core**, never in another mod of the series. A mod that needs the
  game to behave otherwise asks the Core for it.
- **Every reach has its reason and its test**: it is listed with why an event or an API couldn't do it, and a test that
  fails if a new version of the game or of NeoForge moves what it reaches.
- **As few and as narrow as possible**, so other mods keep working beside the Core. A kit that patches deep (moving
  structures most of all) is written to stay out of other mods' way.
- **The list of what to reach**, and why each, is made when the work starts, on a proposal page. The first known needs:
  - a world whose extra dimensions are the series' own, declared ahead (J's Space's Moon), opening without the game's
    warning about experimental settings;
  - what the deep kits need from the game's insides: moving structures (drawing and colliding blocks somewhere other
    than where they are kept, walking on them), each dimension's day, sky and light, fire that needs oxygen, mobs that
    breathe.

### Native animated models

GeckoLib is replaced by the Core's own models, as the series' README promises for every required dependency. The
kit has a page of its own: [Models](models.md).
