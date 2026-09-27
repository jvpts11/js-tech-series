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

## Settings

Under `[media]` in `jstech-balance.toml`:

| Setting | Default | What it bounds |
|---|---|---|
| `download_kilobytes_per_second` | 1024 | How fast the server sends songs to each player. |
| `upload_kilobytes_per_second` | 512 | How fast each player sends a song they bring. |
| `max_file_megabytes` | 32 | The largest song a player may bring; 0 takes none from players at all. |

These bound the server's own traffic. How fast a song moves from one computer to another inside the
game is the game's business, and is set by the cables between them.
