# Networks and Operations

The model of the series' data network and the framework of Operations, the requests a network carries out. They are in
J's Core, not in J's Computers, so that a machine of any mod can be a member of a network and add the kinds of work it
does, without depending on J's Computers; J's Computers is what runs the network.

## What exists today

### The data network

([Networks](../NETWORKS.md).)

- A network is every block joined by data cable around **one Mainframe**, with an identity of its own; each member has
  an identity too. Both are types of their own, never bare ids, so one can't be handed where the other is wanted.
- **What the network may do with each block** is its category: **A** carries the network but is not a member (a cable);
  **B** is a member the network reads but never gives work to; **C** carries out Operations (the computers).
- The members are a **closed family** (computer nodes such as the Mainframe and Subframes, service nodes such as storage
  servers), whose leaves the mods fill in. **Topology elements** (routers, switches) shape the network and never receive
  work.
- **How runs join**: a cable touching runs of one network joins it; touching runs of two networks is a **conflict**, and
  they are joined under one of the two. Cutting a run leaves the part away from the Mainframe with no network.
- **How fast data goes**: each data cable has a line and an era, which give it a throughput and a range; between any two
  places, the network knows the **slowest cable on the fastest way**, which is as fast as data can go.

### Peripheral links

A **peripheral link** joins one machine (the owner, a computer) to its devices (a monitor, a speaker, a drive) by
peripheral cable. It is not part of the data network and carries no network identity. An owner has a number of ports of
each kind and links no more; a hub takes one port and offers several. When a link can't be made, it says why.

### Operations

([Operations](../OPERATIONS.md).)

- An **Operation** is a request with a **kind**, the arguments that kind takes, and a **status**. A mod adds kinds while
  the game loads; afterwards the list closes, because a world saved yesterday has to mean the same today.
- **Eight states**: pending, processing and waiting are active; completed, completed partially, failed, refused because
  something is locked, and discarded are final. Each has a fixed number, because states are saved and sent. A failure
  carries its reason as translatable text.
- **Priority** (five levels, medium by default) and **queues**: the Mainframe works several at once; the highest
  priority goes first, equal ones in the order they came, and an Operation that waits climbs a level after a while, so
  nothing waits forever.
- **Every Operation's life is posted** on the Core's event bus (created, started, then completed, failed or discarded),
  so any mod can watch.

## To build

### Machines of any mod on the network

A machine of any mod becomes a member of a network through the Core, never through J's Computers' classes: it declares
its category, the kinds of Operation it carries out, and what the network may read of it. This is how J's Industrial's
and J's Space's machines that become computers join the network when J's Computers is installed, while working through
their own screens without it: **the network gives a machine more; it never makes it work**.

### Kinds of Operation from every mod

Each mod registers the kinds of Operation its machines carry out: J's Computers its storage and crafting, J's Industrial
its accelerators, research and miners, J's Space its rockets, satellites, probes and cargo, and so on. They are
dispatched by the Mainframe like any other, with queues, priority and the log.

### State as tables

A mod declares **the state of its machines as tables** through the Core: a row per machine, a column per value it shows.
J's Computers' SELECT reads them, so a player asks the network about a reactor or a satellite the same way as about
storage, and no mod depends on another to be read.

### Telemetry leaves the peripheral links

Today the Core has three kinds of peripheral cable: computing, telemetry and industrial control. Telemetry becomes J's
Space's own line, a group of the devices it joins ([Cables and lines](cables-and-lines.md)); computing and industrial
control stay point-to-point links.
