---
section: library_machine
order: 453
---
# Sound: beeps, tunes, recordings and speakers

A machine makes sound through the speaker in its case, and through the sound cards you fit. Sound is the type for the machine as a whole, and Speaker is one speaker you can name. The hardware is explained in [](jsc:sound_cards) and [](jsc:speakers).

> [!DEFINE] Hertz
> The number of times a sound wave repeats each second. A higher number is a higher pitch.

> [!DEFINE] Voice
> One sound that the sound card can play at once.

> [!DEFINE] Recording
> A song file on the machine's disk.

> [!DEFINE] Note
> A named pitch: a letter from A to G, an optional sharp or flat, and an octave from 0 to 8.

Table: Sound members

| Member | What it does |
| --- | --- |
| `Sound.Beep(hertz, millis)` | Beeps at that pitch, for that many milliseconds (ms). |
| `Sound.Tones(tune)` | Plays a tune of notes. Gives true when it plays. |
| `Sound.Play(path)` | Plays a recording from the machine's disk. Gives true when it plays. |
| `Sound.Stop()` | Stops the recordings the programs are playing. |

Table: Speaker members

| Member | What it does |
| --- | --- |
| `Speaker.Named(name)` | The speaker with that name, linked to this machine, or null when there is none. |
| `Speaker.Name` | The name of the speaker. |
| `Speaker.Play(path)` | Plays a recording out of that speaker alone. Gives true when it plays. |

Beep comes out of the speaker in the machine's case. A new beep cuts off the one before it. Its pitch stays between 20 and 20,000 hertz (Hz), and its length between 1 and 60,000 milliseconds (ms).

## Tunes

A tune is written as items separated by spaces or commas. A tune such as C4:250 E4 G4 C4+E4+G4:500 plays three notes and then a chord.

Table: Items of a tune

| Item | What it means |
| --- | --- |
| `C4` | A note: a letter from A to G, then its octave from 0 to 8. |
| `F#3, Bb5` | A sharp with #, or a flat with b, in a note. |
| `440` | A pitch in hertz, above 0 and up to 20000. |
| `R` | A rest: silence for the item's length. |
| `C4+E4+G4` | Notes played together as a chord, up to 16 notes. |
| `:250` | A length in milliseconds, from 1 to 60,000, after the item. |

An item with no length lasts as long as the item before it. The first item lasts 250 milliseconds. A tune holds at most 1,024 items.

Each note of a chord takes a voice of the sound card. A machine with no sound card plays only the first note of each chord, through the speaker in its case.

## Recordings

Play reads a recording from the machine's disks. It gives false when the file is not a recording, when the machine plays no recordings, when the volume is 0, or when the server does not keep the recording. A new Play replaces the recording the programs were playing.

A sound comes out at the machine's volume. A sound that finds no voice free takes the voice of the oldest sound.

A speaker is found by its exact name. A speaker with no name cannot be found this way.

## If something goes wrong

> [!PROBLEM] The program stops and says that the item is not a note.
> Write each item as a note, a pitch in hertz, or R for a rest.

> [!PROBLEM] Sound.Tones gives false.
> The machine's sound is off, or the machine makes no sound. Check that its sound is on.

> [!PROBLEM] Sound.Play gives false.
> Check that the file is a recording, that the volume is above 0, and that the server keeps the recording.

> [!PROBLEM] Speaker.Named gives null.
> No speaker linked to this machine has that exact name. Check the name, because it must match exactly.

> [!SEE] jsc:sound_cards, jsc:speakers
