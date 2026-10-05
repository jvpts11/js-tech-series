# Sound

How a mod on J's Core makes sound: sounds declared once, played from the server with one call, sounds that follow
a machine's state, cues that pick a sound by where it happens, sounds made as they play, and recordings players
bring. The player's side handles the rest: the volume of each channel, muting, walls between the player and the
sound, and how many sounds play at once.

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
game's sound options): Machines, Devices, Interface, Alerts, Ambience, Music and Voice. A mod adds its own with
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

## What can go wrong

- **"needs a subtitle".** Every sound needs one.
- **A sound plays once, not as often as called.** The same sound at the same block within 2 ticks plays once.
- **A loop never stops.** It is played by hand rather than asked for through `IAudible`, or the block entity was
  never untracked.
- **A pack's sound does not replace yours.** Its file is not at `assets/<mod>/sounds/<path>.ogg` (or `_n` for
  variants), or it rebinds a cue with a file that does not read (the log says so).
- **A player hears nothing.** Their channel is down or the sound is muted on their Sound Mixer.
