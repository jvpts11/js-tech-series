# Low level

Reaching into the game where an event or an API does not, and drawing animated models with the Core's own code. Both
live in J's Core alone, so the other mods of the series ask the Core instead of patching the game themselves.

## What exists today

Everything this page designs is built, as of October 2026: the Core's few reaches into the game, each with its reason
and its test, and its own animated models. How each reach is kept: [Reaches into the game](../REACHES.md). The models
have their own page ([Models](models.md)).

## The design

### Reaching into the game

- **Mixins and access transformers live only in the Core**, never in another mod of the series. A mod that needs the
  game to behave otherwise asks the Core for it.
- **Every reach has its reason and its test**: it is listed with why an event or an API couldn't do it, and a test that
  fails if a new version of the game or of NeoForge moves what it reaches.
- **As few and as narrow as possible**, so other mods keep working beside the Core. A kit that patches deep (moving
  structures most of all) is written to stay out of other mods' way.
- **The test is a GameTest** (or a client test, for what lives on the client): it finds the target by name and checks
  the behaviour. The mixin configuration is required, so a target that moved stops the game at start instead of
  failing quietly.
- **Each reach touches one method, and only for what is the series' own** (its namespaces, its dimensions, the Core's
  structures); the rest of the game and other mods pass as before. MixinExtras, which NeoForge ships, lets most of
  them change only a return value or one call, replacing nothing.

#### What the Core reaches

Ten mixins and three access transformer lines:

- **Slots placed by a screen's file.** A screen declared in a file is laid out on the player's game, from the file,
  the style and the font, and its menu's slots are moved under the frames it places; a slot's place is final in the
  game, set once where the menu is built. Two access transformer lines, `public-f` on `Slot#x` and `Slot#y`, for the
  Core's screens to write; nothing else of the game moves a slot. Test: a slot's place is public, not final, and
  takes a new value.

- **Worlds open without the warning about experimental settings.** The game marks as experimental every registry
  entry that doesn't come from the vanilla pack (and every mod jar counts as one pack), and every list of dimensions
  that isn't exactly the three; NeoForge only softens it to one question when the world is made, and no event lets a
  mod say its data is stable. Three small mixins, for the series' namespaces only: on `MappedRegistry#register` (a
  key of the series registers as stable), on `WorldDimensions#checkStability` (the series' dimensions count as
  stable) and on `WorldDimensions#bake` (the list stays stable when every extra dimension is the series'). Another
  mod's dimension still warns, as it should. Test: a list of dimensions with one of the series' is stable; with one
  from another namespace it is still experimental.
- **Each dimension's own day.** NeoForge has the fractional clock and already sends it to the client per dimension,
  but the game builds every dimension other than the Overworld with a level data that reads the Overworld's time,
  rain and thunder and ignores changes: speeding up one dimension's day would speed up the Overworld's. A mixin on
  `MinecraftServer#createLevels` gives the dimensions with a clock rule of the Core a level data of their own, kept
  in the Core's saved data. Test: in a test dimension whose day runs twice as fast, after 100 ticks the Overworld's
  time hasn't moved and the dimension's has moved twice as far.
- **Gravity for things that aren't alive.** Living things have the `GRAVITY` attribute; dropped items, falling
  blocks, TNT, arrows, minecarts and boats use `Entity#getGravity`, which is final and built from each class's
  constants. A mixin on its return value multiplies it by the dimension's gravity for entities that aren't alive
  (the living already carry the attribute, so nothing counts twice); the client predicts motion too, so the
  dimension's rules are sent to it. Two values the game writes by hand stay as they are: fish, dolphins and turtles
  drifting in water, and the fishing bobber. Test: in a dimension of 0.5 g a dropped item falls with half the
  acceleration.
- **Moving structures.** Drawing them already has API (render events, renderers, extra geometry per section); four
  points don't, each with its own mixin, acting only inside the box of the Core's structures to stay out of the way
  of Create and the other mods that move structures:
  - colliding: `Entity#collide` adds the structure's shapes that fall inside the movement's box. Test: a zombie
    dropped on a structure's floor stands on it;
  - rays: arrows and other rays use `BlockGetter#clip`, so they hit the structure (the player's click already has an
    event). Test: an arrow shot at the structure hits its block;
  - menus: every menu checks its distance with `Player#canInteractWithBlock`, which only knows the world, so a
    block's screen on a structure would close the next tick; the mixin turns the structure's position into where the
    block is. Test: a chest on a structure far from the player stays open after 20 ticks;
  - decks: the server disconnects a player "flying" for 80 ticks and counts only the world's blocks, so a player
    standing on a deck would be thrown out; the mixin is on the return of
    `ServerGamePacketListenerImpl#noBlocksAround`. Test: a test player stands 200 ticks on a structure and stays
    connected.
- **Every block change, for sealed rooms.** Events cover breaking, placing, explosions and pistons, but not commands,
  other mods' machines or blocks set without telling their neighbours, and a room opened by one of those would stay
  sealed. A mixin on `ServerLevel#onBlockStateChange`, which the game calls on every change, only notes the position;
  the room is worked out away from the tick. Test: a block set without notice, and another by command, next to a
  sealed room: the room is worked out again.
- **Effects on a playing sound** (reverberation, filters by material, the Doppler effect). NeoForge hands over each
  sound's channel, but its OpenAL source is private, and without it no effect can be bound to the sound. One access
  transformer line, `public com.mojang.blaze3d.audio.Channel source`; the rest is OpenAL's EFX directly. With Sound
  Physics Remastered installed, the Core's reverberation turns itself off. Silence in vacuum and volume need nothing:
  there are events. Client test: the source of a playing sound reads as a valid OpenAL source.

One more is optional: **fire that checks the air every tick** (and roars in pure oxygen), a mixin at the start of
`FireBlock#tick`. Without it the sealed-rooms kit already puts a fire out when its room's oxygen falls and stops it
being lit there, which is what [Sealed rooms](sealed-rooms.md) and [Explosions and fire](explosions-and-fire.md)
ask.

#### Decided with the mod that needs it

- **A darker day by the distance to the star** (Pluto's): decided by J's Space. How far the sky darkens is a number
  the server works out tied to the day's cycle; without a reach there are only the extremes and the colour of the
  light on the client. It would be a mixin at the end of `Level#updateSkyBrightness` and an access transformer to
  write `Level.skyDarken`.
- **Torches and candles going out in vacuum**, with a setting to turn it on and off: J's Space's rule.
- **Biomes in the Overworld, by weighted regions**: built with J's Overworld. The other dimensions have biome
  modifiers; the Overworld is put together in code, and the only way without a patch is replacing the whole file,
  where one mod wins. A mixin on `MultiNoiseBiomeSourceParameterList$Preset#generateOverworldBiomes` and another for
  the new biomes' surface rules; with TerraBlender installed, the Core gives way to it.

#### Decided when its kit is built

- **Coloured light**: as extra geometry in each section it works but reads as paint, not light (the layers don't add
  colour, and entities and the hand stay untouched). Real coloured light needs a reach where blocks are drawn (the
  path Sodium, Embeddium and Iris replace), or handing the colours to Shimmer when it is installed. Either way it
  works with Sodium, Embeddium and Iris exactly as without them, never falling back to white light.
- **Cameras that see beyond the client's view distance**: the client keeps no chunks there, so the far view needs a
  reach on both sides.
- **Decks faster than 200 m/s**: the server resets a player who moves more than 10 blocks a tick beyond their own
  motion, and a deck's motion doesn't count in it. Up to that speed the collision and deck reaches above are enough;
  faster needs a reach (moving the player's last good position with the deck) or ships that stay put in the world
  while the sky and the planets move.
- **Gravity in another direction** turning the player and the camera (towards a point, upside down) needs a reach on
  the client; pushing entities already has API.

#### No reach needed

Checked against NeoForge's code; each already has an event or an API:

- mobs and players that breathe: `LivingBreatheEvent`, `LivingDrownEvent`;
- the sky, clouds, rain, snow, the light's colour and eclipses per dimension: `RegisterDimensionSpecialEffectsEvent`,
  with fog by `ViewportEvent`;
- zoom, field of view, shaking and rolling the camera: `ViewportEvent.ComputeFov`, `ComputeCameraAngles`,
  `CalculateDetachedCameraDistanceEvent`;
- screen effects, the HUD editor and the notifications in place of toasts: `RenderGuiLayerEvent`,
  `RegisterGuiLayersEvent`, `ToastAddEvent` and a post-processing pass of the Core's own;
- explosions with energy, wave and vacuum: `ExplosionEvent`, `ExplosionDamageCalculator`;
- sound in vacuum, the speed of sound, ringing ears: `PlaySoundEvent`, `PlayLevelSoundEvent`;
- loading chunks (the Space Persistor): `RegisterTicketControllersEvent`;
- ores and structures, and taking out vanilla's iron and copper: biome and structure modifiers;
- plants that need air and where mobs spawn: `CropGrowEvent`, `BlockGrowFeatureEvent`, `FinalizeSpawnEvent`;
- multiparts, microblocks, connected textures and the Core's own models: block entities, model data, `ModelEvent`,
  the renderers and `IClientItemExtensions`;
- the screen between dimensions: `RegisterDimensionTransitionScreenEvent`;
- a camera on a screen: public calls only (the world drawn from the camera before the frame, copied into a texture),
  about 1.2 ms a frame;
- dynamic light, what is held lighting the place: an invisible light block following the player;
- the physics kit's time rate: it applies to entities (an event) and to the series' own machines (their own ticker),
  never to other mods' block entities.

### Native animated models

The Core's own models have replaced GeckoLib, as the series' README promises for every required dependency. The kit
has a page of its own: [Models](models.md).
