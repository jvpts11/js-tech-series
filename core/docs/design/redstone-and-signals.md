# Redstone and signals

Signals carried by the game's redstone, sent without wire by frequency, and read from every sensor. The signals by
frequency are in the spirit of Create's redstone links and the wireless redstone mods.

The series has no logic gates, no integrated circuits and no cables of many channels: its logic lives in J's Computers
(programs, the network, Operations), and the mods that do those things well are left to do them.

## What exists today

The game's own redstone. Machines answer redstone and give a comparator signal by design ([Machines](machines.md)).

## To build

### Signals

- **Every sensor speaks redstone**: light, heat, speed, a room's pressure, radiation, altitude, the declared parameters
  of machines, through one capability of signal output. J's Computers also reads every sensor through its peripheral
  links.

### Without wire

- **Signals by frequency**: transmitters and receivers on a numbered frequency, private or the team's, and **point to
  point pairs**.
- **One line, one value**: a frequency carries one signal, the strength fed into its transmitter, from 0 to 15; it has
  no channels. Two signals take two frequencies.
- **One transmitter, any number of receivers**: every receiver on the frequency gives out the same value. A second
  transmitter on the same frequency of the same owner is a **conflict**: the receivers give out nothing until one of
  the two is taken off, and the transmitters and the signal probe show the conflict.
- **Real radio reach**: by the transmitter's power and the obstacles in the way; between dimensions only through relays
  (J's Space's satellites). Within reach the value arrives whole, whatever the distance; beyond it nothing arrives and
  the receiver gives out 0. It is communication, not a bonus over a radius.

### Tools and bridges

- **A signal probe**, a handheld instrument of the same family as the network probe ([Cables and
  lines](cables-and-lines.md#seeing-what-flows)): the strength of the signal on a wire, and the frequencies heard where
  it stands, each with whose it is, where it comes from, how well it arrives and the value it carries, conflicts
  marked; it finds where reception is good enough for a receiver or a relay, and points out a transmitter in the
  world.
- **Bridges to J's Computers' network**: redstone in and out of the network.
- **It works on moving structures and in multipart spaces.**
- **No storms of updates**: signals by frequency and sensors change only when what they carry changes, so a large build
  doesn't weigh on the game.
