# Peripherals

The peripheral cable, monitors, the printer, drives, the other devices a computer can have, and expansion cards.

## The peripheral cable

A short cable made only to link peripherals to a computer. It has nothing to do with the network's cables: it carries no
network id, no network data and has no throughput. It is a point-to-point link, checked by type and by reach. It lives
in J's Core's shared cable block, in a lane of its own, and takes a colour like the other cables.

There is one cable per era, with the plugs of its era:

| Era | Cable | Plugs | Reach |
| --- | --- | --- | ---: |
| Vintage | beige | DE-9 on a screen, DB-25 on a device | 8 |
| Legacy | black | VGA and USB | 12 |
| Transition | white | DVI and the white USB | 14 |
| Standard | braided | HDMI and USB 3 | 16 |
| Advanced | space grey | DisplayPort and USB-C | 20 |

- A port takes the cable of its era and of every era before it; cables of two eras never join.
- A device takes the cable at its back, where its model has the port; a computer, on any face. A device right against
  its computer needs no cable.
- **Ports by kind**, as on a real computer ([Hardware](hardware.md)): a monitor takes a video output of a graphics card
  (or an EEB board's console output, or a processor's with graphics on the die); speakers take an audio output of the
  sound card (or of the motherboard from the Transition on), one output per pair; every other device takes a device port
  of the motherboard (2, 4, 6, 8 or 10, by era).
- Every system's **Device Manager** ([Operating systems](operating-systems.md)) shows what is on each port, with an icon
  per device, and turns each one off and on: a monitor turned off goes dark, a speaker goes quiet, a drive is no longer
  seen, a Network Gateway stops answering. The computer remembers.

### Hubs

A hub per era: the Vintage switch box (+2 ports), the Legacy and Transition hubs (+4), and the Standard and Advanced
hubs (+7). A simple block with six identical faces. It takes one device port of the computer; the cable after the hub
reaches as far again, and hubs hang from hubs. Monitors and speakers don't go through hubs. When a hub loses its link,
the devices behind it drop.

## Monitors

A block that shows the screen of the computer it is linked to: firmware, system, programs. It can sit against the
computer (no cable) or be linked by a peripheral cable. A computer without a monitor runs just the same, but nobody sees
it; the computer's block only opens its case ([Computers](computers.md)).

Eight monitors, all of their time:

| Monitor | Era | Screen |
| --- | --- | --- |
| Mono I Monitor | Vintage | white phosphor |
| Mono II Monitor | Vintage | green phosphor |
| Amber Monitor | Vintage | amber phosphor |
| CGA Monitor | Vintage | 16 fixed colours (the CGA palette) |
| Legacy Monitor | Legacy | colour tube |
| Transition Monitor | Transition | 19" flat panel |
| Monitor | Standard | flat panel |
| Color Monitor | Advanced | 27" IPS panel |

The tube paints what reaches the glass: a phosphor monitor shows the screen in its own colour. The Holographic Monitor
(volumetric projection) waits for the Exa to have hardware.

- A monitor takes a video output ([Hardware](hardware.md)). Without a graphics card, a processor with graphics on the
  die or an EEB board, there is nowhere to plug a monitor.
- Every monitor that is on holds video memory: one block of its era (64 KB for a one-colour tube, 256 KB for the
  sixteen-colour one, 4, 16, 64 and 256 MB from the Legacy to the Advanced). A monitor that doesn't fit in the video
  memory stays dark and says how much it needs.
- **Panels:** identical flat monitors side by side join, up to 8 × 6, into a single screen on a single video output,
  holding the video memory of every block.
- The number of monitors has no effect on the network's performance; it is limited by video outputs and video memory.

**Its face in the world** shows live what its screen shows (the POST, the installer, booting, the prompt, the desktop
with its windows), at about 10 frames a second up to 16 blocks away and 2 up to 32; further away, only the power light.
A monitor is used by one player at a time.

**The power button:** the monitor has a real power button on the block. Clicking it turns the computer on and off;
clicking the rest opens the screen. With the screen open, Power and Restart are on the left. JEI's (or EMI's) list of
ingredients is always beside the screen.

## The printer

A printer per era, of its time: the Epsilon FX-80 (dot matrix, Vintage), the Pakard DeskJot 940 (Legacy), the Pakard
FotoSmart C4280 (Transition), the Pakard LaserJot 1102 (Standard) and the Epsilon EcoTonk ET-2720 (Advanced). It takes a
device port.

The printer is **generic**: it prints whatever programs send it through their Print action (the Editor, Exceed, IQL
results, the Network Manager's log, the Network Interactor's list, Exposure, an image from Paint). Every system has its
own Print: the dialog on Frames, PRINT on MC-DOS, lp and lpstat on the Unix systems.

- Paper: vanilla paper from the tray, one sheet per page, no ink.
- 3 output slots; a queue showing the machine each job came from; Pause and Cancel.
- What comes out is the "Printed Paper" item (title, pages, first lines), read page by page like a book. The sheet looks
  like the printer that printed it, and a printed image shows in an item frame.

It is for the network's documentation, evidence on roleplay servers, material lists, physical guides inside the game,
and reports for other players.

## Drives

Drives are peripherals linked by the peripheral cable, and each takes a device port. A drive reads its own medium and
the older optical ones.

| Drive | Era | Reads |
| --- | --- | --- |
| Floppy Drive | Vintage | floppy disks |
| CD Drive | Legacy | CDs |
| DVD Drive | Standard | DVDs and CDs |
| Blu-ray Drive | Advanced | Blu-rays, DVDs and CDs |
| Dock Station | Standard | USB sticks, and disks |

A drive's era is the era of the port at its back; by the port rule, it takes the cable of its era and of the eras before
(a DVD Drive serves a Transition computer through a Transition cable).

The **Dock Station** has three trays (a 3.5" hard disk, a 2.5" SSD and an M.2 NVMe disk) and the USB port for a stick. A
disk in a tray is an external drive of the linked computer (a letter on Frames, in This PC, mounted under `/media` or
`/mnt` on the Unix systems), with Eject. It is also where a disk whose system no longer starts goes to be repaired
([Storage](storage.md)).

The media themselves (formats, eras, pressed and rewritable) are in [Storage devices](storage-devices.md). A crafting
pattern (`.craft`) is an ordinary file on a writable medium, written by the Pattern Encoder
([Autocrafting](autocrafting.md)).

## Other devices

**Speakers:** a line per era (none in the Vintage, which only has the speaker in the case): the Artisan ToneWorks
(Legacy), the Artisan Inspira 2.1 with Subwoofer (Transition), the Artisan WattWorks T20 (Standard) and the Artisan
Cobble (Advanced). Each speaker takes half an audio output, and the player chooses whether sound comes out of the
monitor, the speakers or both ([Sound](sound.md)).

**Redstone Interface:** a sensor per era (Vintage to Advanced), linked by the peripheral cable, that reads or gives out
redstone through **one** face, its lens, placed like an observer. It shows the strength from 0 to 15 and takes a device
port or a hub port. A computer can have several, each with a unique name; programs set it by name
(`redstone("Gate").Out(15)` in Σ#, `SET REDSTONE` in IQL).

**Network Gateway:** the bridge with ComputerCraft (CC: Tweaked, an optional dependency). A single block with no era: a
peripheral with no processor, memory or system, with a buffer of 9 slots, linked to a computer through the Vintage
serial port (DE-9) at its back. On the CC side it is the `jsc_gateway` peripheral, named `gateway-N`. The bridge goes
one way: a CC program uses our network, and one of our programs reaches the CC computers' devices and power switch,
never their files or their prompt. The Gateway Manager program (and the `gateway` command) names it, sets what the other
side may do and reads its log ([Programs](programs.md)). Without CC: Tweaked, the block links and keeps items, but the
ComputerCraft side never wakes.

**Pattern Encoder:** the writer of crafting patterns, one per era ([Autocrafting](autocrafting.md)).

Routers and repeaters belong to the data network ([The network](network.md)); the Server Router to the racks
([Servers and racks](servers-and-racks.md)); access points to [Wireless](wireless.md).

## Expansion cards

An expansion card goes in a slot of its bus's family ([Hardware](hardware.md)). Every card's tooltip gives its era, its
slot and its draw.

| Card | Computers | What it does |
| --- | --- | --- |
| Graphics card | all | Monitors, video memory, queues on the Mainframe, the server bonus ([Hardware](hardware.md)). |
| Sound card | all (one per machine, of the motherboard's era) | The Artisan Tone Blaster line, from the Vintage to the Transition; from the Transition on, the motherboard has sound ([Sound](sound.md)). |
| Crafting Card | Crafting Computer | What makes a Crafting Computer craft: speed and threads, and by era the interfaces it drives and the bench recipes in its memory ([Autocrafting](autocrafting.md)). |
| Personal-use cards (the Workshop) | Personal Computer | The Crafting Table, Furnace, Enchanting and Anvil Cards, for the Workshop program and the UPDATE Operation ([Operations](operations.md)). |
| Cluster Interface Card | Cluster Management Computer (one per machine) | The reach and lanes of rack management ([Servers and racks](servers-and-racks.md)). |
| Optical Network Card | Mainframe, rack server, Cluster Management Computer | Lets the machine take the backbone's fibre ([The network](network.md)). Standard, PCIe 3.0, 15 W. |
| Phi coprocessor | Supercomputer Node | The supercomputer's crafting slots ([Autocrafting](autocrafting.md)). |

The sound cards, with their synthesis, voices and recording, are in [the catalogue](catalogue.md).

### Crafting Cards

By Forge Logic. Speed is a multiple of the computer's processor capacity; threads are the steps of a craft run in
parallel. The interfaces a card drives and the bench recipes in its memory follow the era (2, 4, 5, 6 and 8,
[Eras](eras.md)).

| Card | Era | Slot | Speed | Threads | Draw |
| --- | --- | --- | ---: | ---: | ---: |
| Crafting Card ISA | Vintage | ISA | 0.02 | 1 | 10 W |
| Crafting Card PCI | Legacy | PCI | 0.03 | 2 | 25 W |
| Crafting Card | Transition | PCIe 1.0 | 0.05 | 2 | 75 W |
| Crafting Card T3 | Transition | PCIe 2.0 | 0.10 | 4 | 100 W |
| Crafting Card PCI-e 3.0 | Standard | PCIe 3.0 | 0.15 | 6 | 110 W |
| Crafting Card T4 | Advanced | PCIe 4.0 | 0.20 | 8 | 125 W |

### Personal-use cards

One card of each kind serves every era from the Legacy on: it goes in any slot but ISA, and only in a Personal Computer.

- **Furnace Card:** smelts with no fuel, at a multiple of a furnace's pace by the computer's era: 2 (Legacy), 3
  (Transition), 4 (Standard), 6 (Advanced).
- **Enchanting Card:** the offers of a table with fifteen bookshelves, for 1, 1 and 2 levels, with no lapis lazuli.
- **Anvil Card:** two thirds of an anvil's levels, rounded up.
- **Crafting Table Card:** the 3×3 grid, in the Workshop program.

No network craft uses these cards; the network opens them through the UPDATE Operation, with the same rule and the same
price as the Workshop ([Operations](operations.md)).

### Phi coprocessors

By Integra, PCIe 3.0, Standard:

| Coprocessor | Cores | GHz | Slots it serves | Draw |
| --- | ---: | ---: | ---: | ---: |
| Integra Phi 5100 | 60 | 1.05 | 2 | 225 W |
| Integra Phi 7120 | 61 | 1.24 | 3 | 250 W |
| Integra Phi 7290 | 72 | 1.50 | 4 | 270 W |
| Integra Phi 9000 | 96 | 1.80 | 6 | 300 W |

Redstone is not a card: it is the Redstone Interface peripheral (above). RAID is not a card either: it is the RAID
Controller bay gadget ([Servers and racks](servers-and-racks.md)).

## To build

- **The Dock Station only takes disks of its era or older** in its trays ([Eras](eras.md)). Today it takes disks of any
  era.
- **Cards still to build**, each designed with its system and existing per era:
  - the Wi-Fi cards ([Wireless](wireless.md));
  - the WAN Interface Cards and the Network Anchor Card ([The network](network.md));
  - the NPUs ([Artificial intelligence](ai.md));
  - the HSM modules ([Security](security.md));
  - the Simulation Interface ([The cosmological simulator](simulator.md)).

  The Satellite Cards belong to J's Space ([The series](series.md)).
