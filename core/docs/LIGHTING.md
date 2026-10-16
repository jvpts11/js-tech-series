# Lighting

*Added 2026-10-16.*

Light in the Core is the game's own light, made to do more: lamps that turn on by redstone or by power, flicker and
break; spotlights and beams that reach far past the game's fifteen blocks; light of a colour that tints the world it
lights; a torch held lighting the way; and the glow of bright things, drawn without a shader. The brightness is always
the game's, so everything that counts light counts it: what spawns, what grows, a daylight sensor. The colour and the
glow are drawn on each player's game, and look the same with Sodium, Embeddium and Iris as without them.

## Kinds of light

A kind of light says what a lamp gives. It is data: declared in code with what it is when nothing else says, and a
file under `data/<namespace>/jstech/lights/` a data pack can write over, sent to every player.

```java
public static final ResourceLocation ALARM = LightKinds.declare(rl("alarm"), new LightKind(
        0xFF2010,                      // colour
        15,                            // intensity, 1 to 15
        15,                            // reach of a spotlight or beam, in blocks, up to 256
        LightKind.Shape.POINT,         // POINT, SPOT or BEAM
        0,                             // a spotlight's cone, in degrees; 0 for none
        Flicker.PULSE,                 // how it moves in time
        LightKind.Control.REDSTONE,    // ALWAYS, REDSTONE or POWER
        0,                             // watts it draws, for POWER
        false));                       // whether it can break
```

```json
{ "colour": "#FFF6E8", "intensity": 15, "shape": "spot", "reach": 48, "angle": 30, "control": "power", "watts": 200 }
```

Every field but `intensity` may be left out of a file for its plain value. The data run writes a mod's declared kinds
as its files: `generator.addProvider(event.includeServer(), new LightKindProvider(output, MODID))`.

| Flicker | How it moves |
| --- | --- |
| `steady` | always as bright as declared |
| `candle` | a flame's gentle wavering |
| `starting` | a fluorescent tube's stutters as it starts, then steady |
| `failing` | now and then it drops out |
| `pulse` | a slow beat, as a warning light breathes |
| `strobe` | on and off |
| `{ "pattern": "mmamma", "rate": 10, "loops": true }` | its own letters, `a` dark, `m` as declared, `z` about twice, `rate` letters a second |

Each lamp starts its flicker at its own point, so a row of lamps does not flicker as one.

## Lamps

A lamp is a `LampBlock` of one kind and a `LampBlockEntity`. Its light is set through NeoForge's light of block
entities, so it turns on, off and flickers with no block state of its own. Its model and recipe are its mod's.

```java
public static final BlockEntry<LampBlock> ALARM_LAMP = CONTENT.block("alarm_lamp",
        properties -> new LampBlock(properties, MyLights.ALARM, () -> LAMP.get()))
        .named("Alarm Lamp").register();
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LampBlockEntity>> LAMP =
        CONTENT.blockEntity("lamp", (pos, state) -> new LampBlockEntity(LAMP.get(), pos, state), ALARM_LAMP);
```

- **Control.** `ALWAYS` is always on; `REDSTONE` while it gets a signal; `POWER` while it gets the watts its kind
  draws, a joule an FE, from its energy store (`lamp.energy()`, exposed as the energy capability), which holds ten
  seconds of what it draws.
- **Breaking.** A lamp of a kind that `breaks` is broken by `lamp.breakLamp()` and dark until `lamp.mend()`. What
  breaks it (an explosion, a shot, a surge) is its mod's.
- **Facing.** A lamp faces the way it was placed against; a spotlight shines that way.

## Spotlights and beams

A `SPOT` lamp casts a cone of rays the way it faces, a `BEAM` a narrow one. Each ray goes on through air, water and
what light passes (glass) until it meets what light does not, and the last open place before that is lit, as bright
as the light still is there: its whole level near the lamp, fading to half at its reach. The server lays that light
as light standing in the air (`jscore:light`, `LightBlock`), so it counts for everything light counts for, however
far the lamp reaches. The light it lays is held by the lamp, looked at again every second, and given back when the
lamp goes off, breaks or is taken away; light left standing where no lamp holds it goes by itself within two
seconds, so none is left behind by a world closed with a lamp on. `Beams.lamp(...)` gives the places a lamp of a
kind lights.

## The colour of light

The colour of the light a block gives is a data map, `data/<namespace>/data_maps/block/light_colours.json`; the light
an item gives held or lying on the ground, `item/held_lights.json`; and the light a kind of mob gives,
`entity_type/entity_lights.json`. All are sent to every player.

```json
{ "values": { "minecraft:redstone_torch": "#FF3018", "mymod:alarm_lamp": "#FF2010" } }
{ "values": { "mymod:flashlight": { "level": 13, "colour": "#FFF4E0" } } }
```

A block the map says nothing of gives white light, which tints nothing, as the game's own warm light; a held block
that gives light gives its own level in its colour; anything on fire gives a fire's. The Core fills the maps for the
game's own: soul fire's blue, redstone's red, the sea lantern's cyan, amethyst's violet, the froglights, a bucket of
lava, a blaze rod, the glowing mobs.

Light of several colours mixes, each by the light it brings, and a colour shows only where its light does: a source
the game's light shows did not reach a place (a wall between them) gives it no colour. Open sky takes from a place's
tint, as daylight drowns a lamp. The world's mesh carries the colour in the corners of its faces, blended across them,
so a floor fades smoothly from red to white away from an alarm.

## Dynamic light

A torch held lights the way, a mob on fire lights its surroundings, an item that gives light lights the ground it
lies on, and a model's glowing part that casts light ([Models](MODELS.md): `glowLight(level, colour)` on its
declaration) lights the world as it moves. This is laid as light standing in the air in the player's own copy of the
world, which the game's light engine spreads as any other: on this game only, nothing of the world changed, what stood
there put back as the light moves on. A mod's own moving lights, a vehicle's headlights, are handed in on the player's
game:

```java
DynamicLightsClient.provide(level -> Cars.near(level).stream()
        .map(car -> new IDynamicLightSource.Light(car.front(), 14, 0xFFF4E0)).toList());
```

## Glow

Bright lamps, blocks that give coloured or bright light, and glowing parts that cast light have a soft halo in their
colour, and spotlights a shaft of light through the air, bright at the lamp and fading along its reach, as dust and
haze show it. Both are light added to what lies behind them and hidden behind walls; only the nearest are drawn.

## Readings

A sensor reads light as `LightReadings` speaks it ([Redstone and signals](SIGNALS.md)): how bright a place is, block
or sky light, the brighter, from 0 to 15; and the hue of the light there, from 0 (red) round to 360, from the colours
of the lamps, blocks and beams that light it.

```java
public List<Reading> readings() {   // a light sensor
    return LightReadings.of(level, worldPosition.above());
}
```

## Settings and stepping aside

The player's `jstech-lighting.toml` says whether coloured light and dynamic light are drawn, how strong the glow is (0
for none, up to twice), and how far all of it reaches, from 16 to 128 blocks. While LambDynamicLights or Sodium
Dynamic Lights is installed, dynamic light is left to it; while Shimmer is, coloured light and glow are left to it.
The settings `step_aside.dynamic_light` and `step_aside.coloured_light` in `jstech-step-aside.toml` keep the Core's own
on as well. Light that counts in the game (lamps, spotlights) is never stepped aside.

## What can go wrong

- **A lamp gives no light.** Its control is not met: no redstone, or no energy for one powered; or it is broken. Its
  kind's id names no declared kind and no file, so it is a plain white lamp: check the id.
- **A spotlight lights nothing.** Its rays meet nothing light does not pass within its reach: the place lit is the
  one before what a ray meets, so a spotlight into the open sky, or onto glass with nothing behind it, lights nothing.
- **No colour shows.** The block's colour is not in the map (white tints nothing), the place is under open sky, the
  player turned coloured light off, or Shimmer is installed and the Core stepped aside.
- **A model's glowing part casts no light.** Its declaration gives no `glowLight`, or it is drawn in a hand or a
  screen, where it lights nothing.
