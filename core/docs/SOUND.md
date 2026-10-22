# Sound

How a mod on J's Core makes sound: sounds declared once, played from the server with one call, sounds that follow
a machine's state, cues that pick a sound by where it happens, sounds made as they play, and recordings players
bring. The player's side handles the rest: the volume of each channel, muting, how many sounds play at once, and how
every sound travels to the player through the place, its walls, its air and its echo; with the ambience, music and
block sounds resource packs give.

## Declaring a sound

*Added 2026-09-24.*

```java
public static final SoundKey KILN_ROAR = CONTENT.sound("kiln/roar")
        .loop()                        // runs until it is stopped
        .range(12)                     // heard this many blocks away (16 unless said)
        .subtitle("Kiln roars")        // required: what a player with subtitles on reads
        .register();
```

The sound's id is `<mod>:<path>`, and its file is `assets/<mod>/sounds/<path>.ogg`. The data generation writes the
`sounds.json` entry and the subtitle; a sound without a subtitle is refused, so nobody who reads subtitles misses
it.

| Step | What it does |
| --- | --- |
| `world()` / `onScreen()` | Heard from a place in the world (the default), or from the player's own screen (a click, a window's chime). |
| `loop()` | Runs until stopped. |
| `channel(...)` | Which of the player's volume channels it belongs to (below). Machines unless said. |
| `variants(n)` | `n` files, `<path>_1` to `<path>_n`, one picked at random each time. |
| `stream()` | Read as it plays, for a long sound. |
| `range(blocks)` | How far it carries. |
| `priority(n)` | Which sounds are kept when too many play at once; 50 is normal. |
| `stereo()` | Stereo files; mixed to one channel when placed in the world. |
| `file(id)` | Use a file that exists already, the game's own included. |
| `made()` | No file: the sound is made as it plays (tones) or is a recording, handed over when it is played. |

## Playing it

*Added 2026-09-24.*

From the server:

```java
Audio.at(level, pos, MySounds.KILN_DOOR);                 // once, at a block
Audio.onScreen(player, MySounds.CLICK, 1.0F, 1.0F);       // on one player's screen
```

The same sound at the same block within 2 ticks is played once, so a burst of calls does not stack into noise.

A sound that runs while something is true (a fan while a machine works) is not started and stopped by hand.
The block entity implements `IAudible` and says which loops it wants now; the player's side asks it every tick
and starts or stops each loop as the list changes:

```java
@Override
public List<LoopRequest> loops() {
    return this.burning ? List.of(LoopRequest.of(MySounds.KILN_ROAR)) : List.of();
}
// on the client, when the block entity loads and unloads:
SoundDirector.track(this);
SoundDirector.untrack(this);
```

Many machines of one kind together can be heard as one room: an `AmbientField` replaces their loops with one bed
in the middle when at least that many are close together, as a server room sounds like a room, not like forty fans.

## Channels and the player's mixer

*Added 2026-09-24.*

Every sound belongs to a channel, and each player sets each channel's volume on the **Sound Mixer** screen (in the
game's sound options): Machines, Devices, Interface, Alerts, Ambience, Music, Voice and Manuals (the books' pages,
covers and buttons, which play the game's own book and click sounds). A mod adds its own with
`AudioChannels.register(new AudioChannel(id, source, name, description))`. On the mixer a player can also mute any
single sound of the game, turn on signs on screen for alerts, turn off muffling by walls, and have other sounds
duck under an alert. A key ("Turn Off Last Sound", unbound until the player binds it) silences the last sound
heard, and a second press brings it back.

## Cues: the sound picked by where it happens

*Added 2026-09-24.*

A cue is a moment ("a system starts") whose sound depends on context: the machine's era, its sound device, its
system. Code says what happened and where; a data file picks the sound, so a resource pack can change it.

```java
public static final SoundCue STARTUP = CONTENT.cue("system/startup")
        .when(SoundContext.ERA, "vintage", MySounds.STARTUP_BEEP)
        .otherwise(MySounds.STARTUP_CHIME)
        .register();

Audio.cue(level, pos, MySounds.STARTUP, SoundContext.EMPTY.with(SoundContext.ERA, "vintage"));
```

The rules are tried in order, the first that matches wins; write the most particular first. The data generation
writes them to `assets/<mod>/sound_cues/<path>.json`, which a resource pack replaces to bind the cue to other
sounds. A pack file that cannot be read is logged and the declared rules stay, so a broken pack never silences a
cue.

## Sounds made as they play

*Added 2026-09-24.*

A `made()` sound plays notes the game makes itself, as old computers did:

```java
Audio.tones(level, pos, MySounds.BEEPER, List.of(Tone.beep(880, 120), Tone.rest(60), Tone.beep(660, 200)));
```

A `Tone` is a wave (`SQUARE`, `TRIANGLE`, `SAWTOOTH`, `SINE`, `NOISE`, `FM`, `WAVETABLE`), a frequency, a length
in milliseconds and a volume. A **sound device** (`AudioDevice`) is what a machine plays them through, with its
own limits: the waves it can make, how many notes at once, its bits and sample rate, and how it colours the sound.
A tone a device cannot make is played with the nearest it can. `AudioDecoders.register(extension, decoder)` adds a
file format; WAV and Ogg Vorbis are there already.

## Recordings

*Added 2026-09-27.*

Players can bring their own recordings (music for a computer's player, for example). The server keeps each once,
named by its content, in the world's `jstech/media` folder; players who hear it fetch it and keep it in a cache on
their computer.

- Uploads go through `MediaUploads.handle(purpose, handler)` on the server, which may refuse one; the server's
  settings bound the size of a file, each player's share, and the speed both ways ([Settings](SETTINGS.md)).
- `MediaSessions.play(...)` plays one at a place, or at several at once (a room of speakers), and pauses, resumes,
  moves and stops it.
- A mod names the recordings it still needs with `MediaKeepers.register(...)`, so clearing out old ones never
  deletes them. `/jstech media prune <days>` clears what nobody used in that many days.

## How sound travels

*Added 2026-10-17.*

Every sound of the world, the game's own and other mods' included, reaches the player through the place between:

- **The place echoes.** A few rays a tick go out from the player's head to the first wall each meets; a whole round
  says how far the walls are, how much of the space is walled in and how much the walls drink, and the echo follows
  as a hall's does (Eyring's formula): a cave of stone rings for seconds, a room of wool barely, the open air not at
  all. In a [sealed room](ROOMS.md) the room's exact size is used.
- **Walls muffle by what they are made of.** Each block between the player and a sound lets some of it through, and
  takes its high notes first: wool little, stone some, metal much. The air takes the high notes away with distance,
  so a far sound comes muffled. What the walls between two places let through is kept by the two blocks for two
  seconds, so a machine heard again from the same spot is weighed at once, and the sounds that go on playing are
  weighed again on a thread of their own: the game never waits on the walls.
- **The medium counts.** Through a vacuum nothing carries a sound: only what the player touches is heard, softly,
  through the body. Under water everything is dull. In thin air sounds are as much weaker as the air is thinner (on
  Mars, about a thirteenth as loud). The server tells each player the air round their head once a second.
- **Sound takes time.** A sound farther than about thirty blocks is held back as long as it takes to arrive at the
  speed of sound (in air by its temperature, faster in water), so the flash of a far blast comes before its boom.
- **What moves changes pitch.** Everything that moves as it plays (a minecart, a vehicle, a shot) is heard higher as
  it comes and lower as it goes, by the Doppler effect, the player's own movement counted too.

A declared sound says more of itself:

```java
public static final SoundKey LAUNCH = CONTENT.sound("rocket/launch").range(512)
        .matters()                          // shown on screen, with where it comes from, to a player who asked
        .distant(MySounds.LAUNCH_RUMBLE, 128) // from 128 blocks away it plays its low rumble instead
        .subtitle("Rocket launches").register();
```

On the Sound Mixer's Options tab a player can turn off **Echoes and Distance** (echoes, the delay and the Doppler
effect), **Muffle Behind Walls** and **Ringing Ears**. With Sound Physics Remastered installed, the Core leaves the
echoes to it.

## What blocks sound like

*Added 2026-10-17.*

A resource pack says, by what each block is made of, what its steps, blows, breaking, placing and falls play instead
of the game's, how much of a sound it drinks and how much one block of it lets through, in
`assets/<namespace>/block_sounds/<path>.json`:

```json
{
  "#minecraft:wool": {"absorption": 0.7, "through": 0.3},
  "minecraft:chest": {"hit": "mypack:chest_creak", "step": "mypack:wood_creak"}
}
```

Keys are blocks' ids or tags written `#namespace:path`; a block's own entry wins over its tags', field by field, and a
later pack's entry over an earlier one's. What no pack says follows from the block's sound in the game.

A blow or a shot striking a living thing raises the cue `jscore:impact/entity` where it strikes, with the kind of
thing struck in its context as `entity`; a pack binds it by kind (a golem clangs, a skeleton clacks) in
`assets/jscore/sound_cues/impact/entity.json`. Bound to nothing, it is silent.

## Ambience and music

*Added 2026-10-17.*

A **soundscape** plays all round the player while its conditions hold, fading in and out, in the Ambience channel:
`assets/<namespace>/soundscapes/<path>.json`.

```json
{
  "sound": "mypack:forest_wind",
  "volume": 0.6,
  "when": {
    "dimensions": ["minecraft:overworld"],
    "biomes": ["#minecraft:is_forest"],
    "weather": ["clear", "rain"],
    "time": {"from": 13000, "to": 23000},
    "indoors": false,
    "depth": {"min": 0, "max": 8},
    "near": [{"what": "#minecraft:leaves", "radius": 12, "least": 40}]
  }
}
```

Everything under `when` is optional; what a file leaves out holds everywhere. Hours may run past midnight; depth is
in blocks below the open sky; `near` counts blocks round the player.

A **music set** is layered tracks played together from the same start, each layer coming in and going out with its
own conditions, so the music grows with the moment rather than cutting to another song:
`assets/<namespace>/music_sets/<path>.json`, with a `priority`, `when` it plays and its `layers`, each a `sound`, a
`volume` and its own `when`. The set of highest priority whose conditions hold plays; once started it plays on until
one of higher priority comes, or its own conditions have failed for five seconds. While a set plays, the game's own
music waits.

## Through devices and voices

*Added 2026-10-17.*

A sound played through a device takes the device's band: `FrequencyResponse.RADIO` (a walkie-talkie, a helmet's
radio) and `FrequencyResponse.INTERCOM`, or a mod's own. A cue raised at a speaker's place with a response plays
through it there; a cue of the interface raised for one player with a response plays from inside their helmet, and
carries even through a vacuum:

```java
Audio.cueOnScreen(player, MySounds.RADIO_CALL, SoundContext.EMPTY, FrequencyResponse.RADIO);
```

With **Simple Voice Chat** installed, players' voices go through the same world: muffled behind walls, weaker in thin
air and silent through a vacuum, so in space players hear each other only by radio (a group, a voice from no place).

## Ringing ears

*Added 2026-10-17.*

A blast close by leaves the ears of whoever stands near it ringing: every sound muffled and dull for a few seconds,
the more and the longer the nearer the blast and the stronger. What is worn in the item tag `jscore:protects_ears`
(ear defenders, a helmet that closes over the ears) prevents it. The ring itself is the cue `jscore:ears/ringing`,
silent until a resource pack binds it to a sound.

## Seeing sound

*Added 2026-10-17.*

The game's debug screen (F3), under the Sound Mixer's lines, tells the place's echo (how long it rings and how
loud), the place's walls (how walled in, how far, how much they drink), the room the player stands in, what sound
goes through and how fast, and ringing ears. The key **Show Sound Debugging**, unbound until the player gives it one,
draws in the world the rays the place was heard by (green where they met a wall) and a line to every sound playing,
red when walls muffle it.

## What can go wrong

- **"needs a subtitle".** Every sound needs one.
- **A sound plays once, not as often as called.** The same sound at the same block within 2 ticks plays once.
- **A loop never stops.** It is played by hand rather than asked for through `IAudible`, or the block entity was
  never untracked.
- **A pack's sound does not replace yours.** Its file is not at `assets/<mod>/sounds/<path>.ogg` (or `_n` for
  variants), or it rebinds a cue with a file that does not read (the log says so).
- **A player hears nothing.** Their channel is down or the sound is muted on their Sound Mixer; or they stand in a
  vacuum, where only what they touch is heard.
- **A far sound comes late.** Sound takes time: a third of a second for a hundred blocks. A player who would rather
  not wait turns off Echoes and Distance.
- **A pack's soundscape, music set or block sounds do nothing.** The file could not be read (the log says which), a
  sound it names is not in any `sounds.json`, or its conditions never hold where the player is.
