# Music on a server

The computers of J's Computers play music: songs their players bring from their own computers, and the
albums a server's owner offers everyone in the server's catalogue, which Soundfoundry lists as
**Voidsoft Music**. This page is for whoever runs the server: where the music lives, how to offer
albums, and the settings that bound it.

J's Computers ships no music. What a server offers is its owner's to have the right to offer, the same
as with a resource pack.

## Where songs are kept

A song is kept once on the server, however many computers hold it, in the world's folder under
`jstech/media/`, named by the SHA-256 of its bytes. A computer's disk keeps a small file naming the
song, which still takes the song's size on the disk. When a song plays in the world, the players near it
fetch it from the server and keep it in a cache on their own computer (`jstech/media-cache/` in the game
folder, 512 MB at most, the song played longest ago going first), so they fetch each song once.

Ogg Vorbis (`.ogg`) and Wave (`.wav`) files are taken.

## The catalogue

Put each album in a folder of its own under `config/jstech/soundfoundry/catalog/`:

```
config/jstech/soundfoundry/catalog/
  Harbour Lights/
    01 - Low Tide.ogg
    02 - Signal Fires.ogg
    album.json
```

The folder is made the first time the server starts. Nothing has to be filled in:

- The album's title, artist and year are what its `album.json` says, else what most of its songs say
  about themselves in their tags, else the folder's name for the title.
- A song is listed under its own title, else its file's name, and by its own artist, else the album's.
- Songs that give their track number play in that order, and the rest after them by file name.

`album.json` is optional, and each of its parts is too:

```json
{ "title": "Harbour Lights", "artist": "The Tin Radios", "year": "2004" }
```

A `cover.png` (or `cover.jpg`) beside the songs is the album's cover, of any size. An album with none
shows the picture its songs carry in their own files, when they carry one, and otherwise a cover made of
its colours and initials.

A data pack can carry albums in the same shape, in `data/<namespace>/soundfoundry/catalog/<album>/`.
Its folders and files follow the rules of any data pack path: lower case, digits, `_`, `-` and `.`,
with no spaces, so their titles come from their tags or their `album.json`.

The catalogue is read when the server starts and whenever its data packs are reloaded. After adding an
album, an operator runs:

```
/soundfoundry catalog reload
```

which reads it again without a restart and says how many albums and songs it found. A file that cannot
be read is passed over, and the server log says which one and why.

To offer no catalogue at all, set `catalog = false` under `[soundfoundry]` in
`jscomputers-server.toml`.

## Sharing between computers

Soundfoundry's NET window searches Voidsoft Music, which is the catalogue, and the songs the other
computers of the network running Soundfoundry keep in the `Shared` folder of their music folder. A
song downloaded comes in at the speed of the slowest cable on its way, and a catalogue song at the
speed of the cable the computer itself is plugged into. Under `[soundfoundry]` in
`jscomputers-server.toml`:

| Setting | Default | What it sets |
|---|---|---|
| `ethernet_kilobytes_per_second` | 512 | How fast a song comes over Ethernet, and over a cable with no speed of its own. |
| `hbw_kilobytes_per_second` | 2048 | How fast a song comes over HBW. |
| `hpc_kilobytes_per_second` | 8192 | How fast a song comes over the HPC fabric. |

The songs coming into one computer at once share its speed.

## The Soundfoundry Server

On the Standard desktops (Frames 11 and the Linux desktops of that era) Soundfoundry is a streaming
player, and it streams from a **Soundfoundry Server**: a service installed on a server in a rack of the
network, like the Messenger Service. With none on the network, it plays the computer's own files and
nothing else; the catalogue reaches a Standard computer only through one.

- **The network's library** is the songs in the server machine's own music folder. A song a computer
  sends it is kept in a folder named after that computer, which is how the library says who it is from,
  and takes room on the server's disk like any file; the same song is not sent twice.
- **Listening costs the server memory.** Each computer streaming from it holds 4 MB of the server's
  memory for as long as it plays, and the server turns a computer away when it has none left.
- **Streamed or downloaded.** A song of the catalogue or of the library plays through the server, or from
  the computer's own disk once it is downloaded, which needs no server. Downloads and songs sent to the
  server go at the speed of the slowest cable between the computer and the server; a catalogue song at the
  speed of the computer's own cable.
- **Several servers** on one network are fine: the player picks the one to stream from in the box at the
  foot of Soundfoundry's sidebar, the first found until they do.

Playlists are `.m3u` files in the `Playlists` folder of the computer's music folder, the liked songs one
of them, and a playlist can hold streamed songs as well as files.

## Settings

Under `[media]` in `jstech-balance.toml`:

| Setting | Default | What it bounds |
|---|---|---|
| `download_kilobytes_per_second` | 1024 | How fast the server sends songs to each player. |
| `upload_kilobytes_per_second` | 512 | How fast each player sends a song they bring. |
| `max_file_megabytes` | 32 | The largest song a player may bring; 0 takes none from players at all. |
| `player_quota_megabytes` | 512 | How much the songs one player brought may take together; 0 sets no limit. |

These bound the server's own traffic. How fast a song moves from one computer to another inside the
game is the game's business, and is set by the cables between them.

A song counts towards the player who brought it first; bringing a song the server already keeps costs
nothing. A song taken out of a disk stays on the server, since the disk may have been copied, and
counts until the server's owner clears it out.

## Clearing out songs nobody uses

The server remembers when each song was last used: played, fetched by a player, put on a disk or
offered in the catalogue. An operator sees how much the server keeps with:

```
/jstech media
```

and clears out every song nothing has used for a number of days with:

```
/jstech media prune <days>
```

The catalogue's songs and those playing stay whatever their age. The player who brought a song that
goes gets its room back. A disk that still names it says the server does not keep that song when it
is played.
