# Sound

Computers sound like what they are made of: the case, the disks, the media, the system that booted, the sound card of
their era. The sounds are real recordings chosen to fit (generating a sound is the last resort), and each is in the
repository's asset registry with its source and its licence.

## The sounds of the world

- **The computer:** the power button, the POST beep (and the error one), the disk spinning up, seeking, idling and
  spinning down, and a Vintage machine starting.
- **Media:** the floppy disk going in, coming out and being read; an optical disc's tray and reading; a USB stick going
  in and out.
- **The monitor** turning on and off; **the printer** ([Peripherals](peripherals.md)).
- **The rack:** a chassis sliding in and out; each server's fans and the room's hum; the Mainframe's tape.
- **The systems:** each desktop's startup, shutdown and warning sounds, in their era's version (the Legacy's KDE and
  GNOME sound as they did then).
- The games (Minesweeper).

**How a sound is chosen:** through J's Core. Every sound belongs to a **channel** (Machines, Devices, Interface, Alerts,
Ambience, Music, Voice), and each player sets each channel in the Sound Mixer, where they can also mute a single sound,
turn on on-screen cues for alerts, turn off muffling through walls and lower the rest under an alert. A moment ("a
system boots") is a **cue** whose sound depends on the context (the machine's era, its sound device, its system): the
code says what happened, and a data file chooses the sound, which a resource pack can replace.

## Sound hardware

| Device | Eras | Synthesis | Voices |
| --- | --- | --- | ---: |
| The speaker in the case | all | square wave | 1 |
| Artisan Tone Blaster (FM) | Vintage | FM | 9 |
| Artisan Tone Blaster (wavetable) | Legacy, Transition | wavetable | 32 |
| The motherboard's sound | from the Transition on | wavetable | 64 |

The cards are in [the catalogue](catalogue.md). One sound card per machine, of the motherboard's era.

- **Outputs:** sound comes out of the speakers of the **monitor** linked to the computer or out of the **speakers**
  ([Peripherals](peripherals.md); there are no Vintage speakers), and the player chooses whether it comes from the
  monitor, the speakers or both. Each speaker takes half an audio output.
- **Voices:** everything a machine plays shares its hardware's voices. A stereo song takes two voices on a card that
  plays both sides, and a mono one takes one; each note of a chord takes one, and so does each system sound. A sound
  that finds no free voice takes those of the sound that started first, which stops.
- **With no card**, the machine plays melodies through the case's speaker, in square notes, and only the first note of
  each chord. A Legacy machine needs a sound card to play a recording, and every machine needs a monitor linked.
- Everything follows the system's volume and mute, and everything stops when the machine turns off.

## Soundfoundry

Soundfoundry (Voidsoft, from the Legacy on) is the music player, in a skin of its own that looks the same on any desktop
of its era.

- **Songs:** the player brings their own from their own computer (Ogg Vorbis and Wave). The server keeps each one
  **once**, by the SHA-256 of its bytes, in the world's folder; a computer's disk keeps a small file that names it and
  weighs the song's size. Players who listen to it fetch it from the server and keep it in a cache on their computer
  (512 MB at most, the one heard longest ago goes first).
- **The catalogue** (Voidsoft Music): the albums the server's owner offers everyone, in a config folder or a data pack
  (title, artist, year, cover, all optional); `/soundfoundry catalog reload` reads it again without a restart, and the
  settings can turn it off. The mod brings no music: what a server offers is its owner's, as in a resource pack.
- **Sharing on the network:** the NET window searches the catalogue and the Shared folders of the other computers on the
  network with Soundfoundry; a song comes at the speed of the slowest cable on the path
  ([Implementation](implementation.md)).
- **In the Standard**, Soundfoundry is a streaming player and reads from a **Soundfoundry Server**, a service on a rack
  server: the network's library is the songs in that server's music folder (a song sent goes into a folder named after
  the computer that sent it); every computer listening holds 4 MB of the server's memory, and the server refuses when it
  has no more; several servers can live side by side, and the player chooses which to listen to. With no server, it
  plays only the machine's own files.
- Playlists in `.m3u` in the Playlists folder, the favourites being one of them.
- **Cleaning up:** the server remembers when each song was used; `/jstech media` shows how much it keeps and
  `/jstech media prune <days>` deletes what nobody used in that many days (the catalogue's and the ones playing stay).
  The limits on size, per-player quota and speed are in `[media]` ([Implementation](implementation.md)).

## Programs

A Σ# program plays through the machine it runs on ([Σ and Σ#](sigma.md)):

- `Sound.Beep`: through the case's speaker, from 20 Hz to 20 kHz, up to a minute;
- `Sound.Tones`: a melody through the sound card: notes by name, in hertz or rests, chords with `+`, durations in
  milliseconds; up to 1,024 items and 16 notes per chord;
- `Sound.Play`: a song from the disks, through the monitors and speakers;
- `Sound.Stop`, and `Speaker.Named` for a single speaker.

The notes have the card's voice: FM's metallic ring, wavetable's round sound.

## To build

- **Legacy sound cards on PCI**, as the real ones were, with motherboards whose slots come in groups
  ([Hardware](hardware.md), [Eras](eras.md)). Today the Legacy cards sit on AGP 8x and PCIe 1.0.
