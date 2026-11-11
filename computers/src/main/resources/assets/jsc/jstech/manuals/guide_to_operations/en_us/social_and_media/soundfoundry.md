---
section: social_and_media
order: 90
icon: jsc:cd_rom
---
# Soundfoundry

Soundfoundry plays songs on the computer, from a file or from the network. It is on the Frames, Linux and FreeBSD desktops, but not on UNIX. It needs a Legacy computer or newer.

Soundfoundry needs a sound card. Without one, the song line says NO SOUND CARD INSTALLED, and the volume, balance and playlist buttons turn grey. Install a sound card in the computer ([](jsc:sound_cards)), and choose where the sound goes ([](jsc:speakers)).

## The player

Table: Output button

| Label | Plays the sound through |
| --- | --- |
| `OUT -` | Nothing. |
| `OUT MON` | The monitor. |
| `OUT SPK` | The speakers. |
| `OUT MON+SPK` | The monitor and the speakers. |

The window also has SHUFFLE and REPEAT buttons, and a PL button that shows the playlist. ADD, REM and SEL work on the playlist. NET opens the network share window. The volume and the balance are sliders, and the song line shows MONO or STEREO, and its bit rate and sample rate.

The menu holds the ways to fill the playlist: open a file or a folder, add to the playlist, remove or keep the selected songs, sort the list, and save or open a playlist. A playlist holds at most 500 songs, and its name is at most 32 characters.

> [!DEFINE] Playlist
> The list of songs queued to play, in order.

## Songs from the network

The network tabs are SEARCH, DOWNLOADS and SHARED. A search finds every word you type in a song's title, artist, album or file name, in any order and any case. A query holds at most 64 characters. Songs from the network arrive at the speed of the slowest cable on the way, and several downloads at once share that speed.

A song plays from one of three places: a file on this computer's disk, a song of the library that this computer already has on its own disk, or a stream from the Soundfoundry Server the computer picked. A downloaded or imported song is kept in the music folder of the computer.

> [!DEFINE] Stream
> A song sent while it plays, instead of kept on the computer first.

> [!DEFINE] Share
> A song sent to the server's library, so that the others on the network can play it.

## Soundfoundry Server

The Soundfoundry Server runs on a server in a rack. It streams the music catalogue to every Soundfoundry on the network ([](jsc:server_racks)). Its library is the songs in its own music folder, and each computer's shared songs sit in a folder named after that computer.

> [!DEFINE] Soundfoundry Server
> The service on a rack server that streams the network's music to every Soundfoundry you play it on.

The server streams only while it has the memory to hold the stream open. So a computer asks to listen before it plays, and asks again every little while.

## Vinyl

With a Vinyl RW Drive on the computer's cable ([](jsc:vinyl_rw_drives)), Soundfoundry records the music discs a jukebox plays and cuts them onto vinyl. On the Legacy and Transition looks, the VNL button opens the drive's plate, with Record, Stop, Cut and Eject and the list of recordings; on the Standard and the Advanced, the Vinyl page in the sidebar does the same. The recordings are kept in Music/Vinyl.

## If something goes wrong

> [!PROBLEM] The song line says NO SOUND CARD INSTALLED.
> Install a sound card in the computer.

> [!PROBLEM] A network song does not play.
> No Soundfoundry Server is on the network, or the server has no memory to spare for the stream. Install a Soundfoundry Server on a server in a rack of the network.

> [!SEE] jsc:sound_cards, jsc:speakers
