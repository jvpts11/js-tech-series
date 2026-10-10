# Redstone and signals

Signals carried by wire, combined in logic, sent without wire by frequency, and read from every sensor. It is in the
spirit of Project Red, Create's redstone links and the wireless redstone mods.

## What exists today

The game's own redstone. Machines answer redstone and give a comparator signal by design ([Machines](machines.md)).

## To build

### Wires and signals

- **A grid of signals** for any mod's lines: a strength from 0 to 15 per channel, with **sixteen channels in one cable**
  (a bundled cable), still compatible with the game's redstone. The kit gives the ability; a bundled cable of the Core's
  own would be a new line of the cable block, decided when a mod asks for it ([Cables and lines](cables-and-lines.md)).
- **Every sensor speaks redstone**: light, heat, speed, a room's pressure, radiation, altitude, the declared parameters
  of machines, through one capability of signal output.

### Logic

- **Logic gates as multipart parts**: AND, OR, NOT, XOR, latch, timer, counter, comparator, pulse former, random; small,
  on the faces of blocks, as Project Red's are ([Multipart](multipart.md)).
- **Integrated circuits**: a circuit is drawn on a bench and placed as **one part**, as Project Red's fabrication does.

### Without wire

- **Signals by frequency**: transmitters and receivers on a numbered frequency, private or the team's, and **point to
  point pairs**.
- **Real radio reach**: by the transmitter's power and the obstacles in the way; between dimensions only through relays
  (J's Space's satellites). It is communication, not a bonus over a radius.

### Tools and bridges

- **A signal probe** that shows each channel's strength on a wire, and an **oscilloscope**: the signal drawn over time,
  to debug circuits.
- **Bridges to J's Computers' network**: redstone in and out of the network.
- **It works on moving structures and in multipart spaces.**
- **No storms of updates**: signals spread efficiently, so large circuits don't weigh on the game.
