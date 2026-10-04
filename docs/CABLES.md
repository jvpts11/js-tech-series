# Cables

How the cables of the J's Tech Series work: what each kind is for, how they join, how far and how fast they
go, and the parts and devices that hang from them. Everything here is about the cables you lay by hand; the
numbers are first estimates and are tuned in play.

## One block for every cable

Every mod of the series lays its cables in one shared block, the cable block of J's Core. A block holds up to
nine wires, each in a lane of its own on a three by three grid seen end on, so a data cable, a peripheral cable
and a crafting cable can run through the same block side by side without ever joining one another. A cable
that has to be alone, the long distance line, holds its block by itself.

- **Laying**: use a cable on an empty spot to place it, or on a cable block to add it there when its lane is
  free. Sneaking, or a lane already taken, lays it against the face you clicked instead.
- **Breaking** takes out only the wire or the part you look at; the block goes with its last piece.
- **Colours**: a dye used on a wire colours it, and it wears a ring of that colour on every block. Two wires of
  different colours never join; an uncoloured wire joins every colour. This is how two runs of the same cable
  are kept apart where they touch.
- **Junction boxes**: where a wire changes lane, or two would cross inside a block, the block becomes a junction
  box that the wires enter and leave each in its place.

A wire belongs to a **line** (what job it does) and a **generation** (which era of that line it is). Wires of
one line join when they are of the same generation and their colours agree. Devices such as routers join every
line they take.

## The data lines

The data cables are the computer network. Each line has one cable per era it exists in, and each ends in its
era's plug where it meets a device.

| Line | What it joins | Vintage | Legacy | Transition | Standard | Advanced |
|---|---|---|---|---|---|---|
| Access | the small computers to a router | Thin Coaxial | Ethernet | Cat 5e | Gigabit Ethernet | Cat 6a |
| Backbone | the routers, the Mainframe and the racks | Thick Coaxial | HBW | 10GBASE-CX4 | Fibre Optic | OM5 Fibre |
| Long distance | two networks, between two Gateway computers | Telephone Line | Leased Line | T3 Line | VLDC | Dark Fibre |
| High compute | a supercomputer's nodes to its HBW Interface | | | InfiniBand | High Compute | OSFP |
| Crafting | a Crafting Computer to the parts that feed its machines | Crafting Cable, the same in every era | | | | |

Splitting the work this way is what keeps a network free of channels and subnetworks: you lay the line the job
asks for, and you never count what a cable holds.

### Speed and range

Every data cable has a **speed**, the items a tick an Operation crossing it moves at most, and a **range**, how
many cables a run of it can be before a router or a repeater renews it. Both count:

- A run longer than its range carries nothing, so whatever lies only beyond it is off the network.
- An Operation between two machines moves no faster than the slowest cable on the best way between them.

| Line | Vintage | Legacy | Transition | Standard | Advanced |
|---|---|---|---|---|---|
| Access | 4 / 32 | 16 / 48 | 64 / 64 | 128 / 80 | 256 / 96 |
| Backbone | 16 / 96 | 64 / 160 | 256 / 200 | 512 / 512 | 2,048 / 1,024 |
| Long distance | 1 / 2,000 | 8 / 5,000 | 24 / 7,500 | 64 / 10,000 | 256 / 20,000 |
| High compute | | | 1,024 / 24 | 2,048 / 32 | 8,192 / 48 |
| Crafting | 32 / 16 | | | | |

Speed in items a tick, then range in cables. The slowest cable and the length of each run are worked out when
the network changes and kept until it changes again, never on every tick.

### Eras

A port takes its own era's cable of a line and every earlier one, never a later one. Two eras of one line that
touch do not join: they meet at a router of the newer era. So an old machine on a new network needs a router
between them, and a new machine takes the old cable as it is.

### Shapes that are rules

- The backbone's fibre, from the Standard on, runs only straight. A cable laid against its side is left out; an
  optical router is what turns it.
- The long distance line runs between exactly two ends, takes no third, is thicker than the other cables and
  never shares a block.

## Routers and repeaters

- **Routers**, one per era (the Vintage Router, the Personal Router of the Legacy, then the Transition, Standard
  and Advanced Routers), join their era's access line to its backbone. A router takes the cables of both lines of
  its era and of every earlier era on any of its six faces, and all of them are one network.
- **Optical routers** (from the Standard, and the Advanced Optical Router) take only the backbone's fibre, from
  the Standard's to their own era's. They are where fibre runs meet, turn and branch.
- **Repeaters**, one per era, let the access, backbone, high compute and crafting cables run through them each on
  its own, never joining one another there. Each run starts its range over at the repeater, so a cable can go
  twice as far with a repeater halfway.

A small computer (a Personal Computer, a Crafting Computer) reaches the network over its access line, through a
router to the backbone. The Mainframe and the racks sit on the backbone.

## Parts on the cables

A data cable or a crafting cable can carry thin parts on its faces, one part a face. A part faces the block
beside it, and works with what is there.

### Buses

- The **Import Bus** brings items from the inventory it faces into the network, and the **Export Bus** sends them
  out to it. Each has a filter, quantities to keep and to stop at, a mode (always, or only on a redstone signal),
  a priority and conditions, more of them the newer its era.
- The **External Storage Bus** moves nothing itself: the network uses the inventory it faces as storage of its
  own, ten times slower than its servers.

A bus's window has three tabs: what it is set to, what it did, and the IQL and Σ# lines that set it the same way,
since programs can set buses too.

### The crafting network

Machine recipes live on the crafting cable, in three parts:

- The **Crafting Interface** holds a machine's recipes (3 to 12 patterns by its era) and feeds that machine:
  directly when it sits against it, or through a crafting cable of its own, dyed apart from the main one.
- The **Crafting Input Router** sits on an interface's own cable against one input face of the machine. An input
  goes through the router chosen for it in the interface's window, else the first whose filter takes it, else
  the first that takes anything.
- The **Crafting Receiving Bus** sits against the machine's output and credits what comes back to the jobs that
  fed it, never more than they fed, in the order they were fed. What a job is still owed when it settles keeps
  being collected; an item no pattern lists goes to the network as unexpected.

The Crafting Computers on a crafting cable drive its interfaces, as many as their Crafting Cards drive, in the
order the cable reaches them. A computer that is off drives none.

## Peripheral cables

A peripheral cable joins a computer to a device: a monitor, speakers, a drive, a printer, a hub, a Redstone
Interface. It is a different line from the data cables and carries no network.

| Era | Cable | Plugs | Range |
|---|---|---|---|
| Vintage | beige | DE-9 at a screen, DB-25 at a device | 8 |
| Legacy | black | VGA and USB | 12 |
| Transition | white | DVI and the white USB | 14 |
| Standard | braided | HDMI and USB 3 | 16 |
| Advanced | space grey | DisplayPort and USB-C | 20 |

- A port takes its era's cable and every earlier one; cables of two eras never join.
- A device takes the cable on its back, where its model has the port; a computer on any face. A device placed
  against its computer needs no cable at all.
- Ports go by kind, as on a real computer: a monitor takes a video output of a graphics card, speakers the audio
  output of the sound card (or of the board from the Transition on), and every other device one of the board's
  device ports.
- A **hub** takes one device port and offers more of its own (two on the Vintage switch box, four on the Legacy and
  Transition hubs, seven on the Standard and Advanced ones). The cable after a hub reaches as far again as the cable
  before it, and hubs can hang from hubs. Screens and speakers do not pass through a hub.

The Device Manager of each system shows what is on every port.
