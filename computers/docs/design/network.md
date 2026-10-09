# The network

What a network is, the cables it is made of, how networks meet, what happens when they shouldn't, and how a network
reaches far away.

## What a network is

A network is everything that reaches **one** Mainframe through data cables, routers and repeaters, with **one** network
id. There are no channels to count and no subnetworks: what limits a cable is its speed and its reach. Nothing walks the
cables while the network runs: the network is a graph of what is connected (a union-find over the grid), not a channel
that checks paths on every Operation. The slowest cable of each path and the length of each stretch are worked out when
the network changes and kept, never every tick.

### What can be on a network

J's Core sorts what joins a network into three categories:

- **A:** carries the network without being a member, like a cable, with no node id. The Server Rack is one: only the
  container of its servers.
- **B:** a member that can be read (monitored) and **never** receives work, with a node id. J's Industrial's monitored
  machines go here.
- **C:** a smart node that runs Operations, with its own id: the computers. Every Operation type of J's Computers needs
  a C node.

The hierarchy is sealed in J's Core: a network node is either a computer node or a service node. **Topology elements**
(routers, the Server Router, switches) are something else: they are not nodes, receive no Operations, and only have a
local id to save the topology.

### How the network id spreads

When a cable is placed between two devices, the system checks the neighbours, once, whatever the size of the network:

- one side has an id and the other doesn't: the id spreads to it;
- neither has an id: they join without one;
- both have the same id: nothing happens;
- both have different ids and neither has a Mainframe: they join under the id of one of them.

The real conflict is between Mainframes (below).

## The cable systems

Every cable of the series lives in a single block, J's Core's cable block: up to nine wires per block, each in its lane
of a 3×3 grid seen from the front, so a data cable, a peripheral cable and a crafting cable can run side by side in the
same block without joining. Where a wire changes lane, the block becomes a junction box. A dye on a wire gives it a ring
of that colour: wires of different colours never join, and an undyed wire joins all of them. Breaking takes out only the
wire or the part being looked at. Each mod registers its own cables in the block; the ones that matter to J's Computers:

1. **Data:** the network's lines (below). Only this system carries the network id.
2. **Peripheral:** point to point, links devices to a computer ([Peripherals](peripherals.md)).
3. **Telemetry:** J's Space's dedicated link for satellite data. It exists as a cable type (256 blocks), with no block
   yet; J's Space replaces it with its **telemetry line**, a line of its own that joins antennas, the devices of a
   ground station, observatories and the Space Research Computer, and carries data only, never the network id.
4. **Industrial control:** J's Industrial's point-to-point link between its machines and the Fieldbus Cards of an
   Industrial Controller Computer, a computer J's Industrial adds when both mods are installed. It exists as a type (8
   blocks), with no block yet.
5. **Energy:** J's Core's energy, J's Energy, in real units, through J's Core's basic cable, J's Industrial's energy
   cables and lines, or any mod's FE cable through J's Core's converters ([Power](power.md)).

Other mods add their own: J's Industrial's Research Link Cable, for example, groups research centres, and reaches the
network through J's Industrial's Research Router, a topology element as the Server Router
([Servers and racks](servers-and-racks.md)), and J's Space's Space Research Link Cable groups its Space Research
Computers. They all share space without getting in each other's way.

## The lines of data cable

Each line does one job and has a cable per era it exists in, with the era's plug where it meets a device.

| Line | What it links | Vintage | Legacy | Transition | Standard | Advanced |
| --- | --- | --- | --- | --- | --- | --- |
| Access | small computers to a router | Thin Coaxial | Ethernet | Cat 5e | Gigabit Ethernet | Cat 6a |
| Backbone | routers, the Mainframe and racks | Thick Coaxial | HBW | 10GBASE-CX4 | Fibre Optic | OM5 Fibre |
| Long distance | distant networks, between WAN Gateway Computers | Telephone Line | Leased Line | T3 Line | VLDC | Dark Fibre |
| High compute | a supercomputer's nodes to its HBW Interface | | | InfiniBand | High Compute | OSFP |
| Crafting | a Crafting Computer to the parts that feed its machines | Crafting Cable, the same in every era | | | | |

### Speed and reach

Every data cable has a **speed** (the most items per tick an Operation crossing it moves) and a **reach** (how many
cables a stretch of it can have before a router or a repeater renews it). Both count:

- A stretch longer than its reach carries nothing: whatever is only beyond it is off the network, even if it touches
  cables.
- An Operation between two machines goes no faster than the slowest cable of the best path between them. The limit is
  per Operation, not a bandwidth shared out: two Operations through the same cable each get its full speed.

| Line | Vintage | Legacy | Transition | Standard | Advanced |
| --- | --- | --- | --- | --- | --- |
| Access | 4 / 32 | 16 / 48 | 64 / 64 | 128 / 80 | 256 / 96 |
| Backbone | 16 / 96 | 64 / 160 | 256 / 200 | 512 / 512 | 2,048 / 1,024 |
| Long distance | 1 / 2,000 | 8 / 5,000 | 24 / 7,500 | 64 / 10,000 | 256 / 20,000 |
| High compute | | | 1,024 / 24 | 2,048 / 32 | 8,192 / 48 |
| Crafting | 32 / 16 in every era | | | | |

(speed in items per tick / reach in cables)

### Eras and shapes that are rules

- A port takes the cable of its era of a line and of every earlier one, never a later one. Two eras of the same line
  that touch **don't** join: they meet at a router of the newer era. An old machine on a new network needs a router
  between them; a new machine takes the old cable as it is.
- Wires of the same line join when they are of the same era and their colours agree. Devices like routers join every
  line they take.
- Backbone fibre, from the Standard on, only runs straight: a cable touching its side is left out, and what turns it is
  an Optical Router. A machine only takes the fibre with an Optical Network Card ([Peripherals](peripherals.md)).
- The long-distance line runs between exactly two ends and takes no third; it is thicker than the other cables and never
  shares a block.
- **Who joins where:** the small computers (Personal Computer, Crafting Computer, Cluster Management Computer) join on
  Access, through a router, never on the backbone; the Mainframe and the racks are on the Backbone; Supercomputer Racks
  only on High compute.
- There is no "quantum bridge" and no wireless transmitter of the data network: long-distance links are cables between
  WAN Gateway Computers.

### Routers and repeaters

- **Routers**, one per era (the Vintage Router, the Legacy's Personal Router, and the Transition, Standard and Advanced
  Routers): they join the Access line of their era to its Backbone. They take the cables of both lines of their era and
  of every earlier one on any of their six faces (blocks of six identical faces, with the era's connector and two
  lamps), and all of it is one network.
- **Optical Routers** (the Standard's and the Advanced Optical Router): only backbone fibre, from the Standard to their
  era. They are where stretches of fibre meet, turn and branch.
- **Repeaters**, one per era: the Access, Backbone, High compute and Crafting lines pass through them each in its own
  lane, without joining. Every stretch starts its reach again at the repeater, so a cable goes twice as far with a
  repeater in the middle. The long-distance line takes no repeater.
- The Network Manager shows, for every machine, the line and speed of its link, and "no link" with the reason (fibre
  turning without an Optical Router, a stretch too long).

The cable of the simulator's cluster is designed with the simulator ([The cosmological simulator](simulator.md)).

### A typical network

```
[Personal Computer] → Access → [Router] → Backbone → [Mainframe]
[Crafting Computer] → Access → [Router] ─┘        |
[Cluster Mgmt Computer] → Access → [Router] ─┘    |
                                          Backbone
                                                  |
                                [Server Router] (back face)
                                 /         |          \
                         [Server Racks] [Server Racks] [Server Racks]
                          section A      section B      section C
[Supercomputer Racks] → High compute → [HBW Interface] → Backbone → (network)
[Crafting Computer] → Crafting → [Crafting Interfaces on the machines]
```

## When Mainframes meet

**One Mainframe per network.** A second owning Mainframe joined to the same network (one that isn't in failover) causes
a **network conflict**. It is decided on the Mainframe, not on the cable. When a Mainframe sees another owning Mainframe
running on the same stretch of network:

- **both** go into conflict (CONFLICTED) and give up the network;
- each one's dispatch closes: the Operations under way are **discarded** (logged as DISCARDED);
- the chat says "NETWORK_CONFLICT: two Mainframes share one network near X", and the conflict's advancement is granted;
- as soon as the Mainframes stop sharing the stretch (take out the cable between them), each goes back to its own
  network, on its own.

Failover, the way a passive Mainframe backs up an active one without a conflict, is in [Computers](computers.md).

**An orphaned network** (its Mainframe destroyed without failover) is ORPHANED. The servers keep their data as usual: no
stored item is lost. A new Mainframe joined to the same physical topology adopts the stretch and brings the network back
by the id on the cables. The index lives in memory and is rebuilt when the Mainframe starts. The persistent Operations
left behind expire once they outlive the time an orphaned Operation is kept (24 hours by default, in
`jstech-balance.toml`), rather than always being discarded.

## Keeping chunks loaded

A computer in an unloaded chunk stops, and resumes when the chunk loads again, without losing anything. Nothing in J's
Computers loads a chunk on its own; J's Core already has chunk loading per owner, which the pieces below will use.

## To build

### Conflicts that cost

- In a network conflict, the items the discarded Operations were carrying **are lost** (configurable,
  [Power](power.md)). Today they go back where they were.
- The **storage of both networks gets corrupted**: part of the stored items (10%, an estimate) become corrupted, through
  the corruption mechanic ([Storage](storage.md); the `network_conflict` key in `[item_loss]`). Each network's owner
  recovers what luck leaves with the **Defrag**. Today a conflict corrupts nothing.
- So joining a cable of another player's network to yours, as an attack, brings down and corrupts **both** networks: the
  attacker pays the same as the victim.

### Two pieces to keep machines working

- **The Network Anchor Card:** an expansion card per era that keeps the chunk of the computer it is in loaded. It uses
  J's Core's chunk loading per owner, within the owner's quota (`world.chunks_per_owner`), and draws a card's watts like
  any other ([Power](power.md)).
- **The Space Persistor:** a block of J's Core, the series' chunk loader (the Chicken Chunks one, with a technological
  look). The player chooses its radius and the rest on the block, within the owner's quota; whether it draws energy, and
  how much, is up to J's Core's settings. It serves any mod, not only computers.

### The WAN Gateway Computer

The **WAN Gateway Computer** links distant places through the **long-distance line**: the Telephone Line, the Leased
Line, the T3 Line, the VLDC and the Dark Fibre, one per era. (The Network Gateway is something else: the bridge with
ComputerCraft, [Peripherals](peripherals.md).) Today only the long-distance line exists, with no function.

**Its shape:** a very wide computer, bigger than the Mainframe, with a model per era like the long-distance line, and 4
ports for that line. It takes hardware like any computer (motherboard, processors, memory, power supply), runs a system,
and its function is **software**: a WAN service installed on it. Hardware is capacity, software is role.

**Its specialised hardware:** the **WAN Interface Cards**, one per port (4 ports, so up to 4 cards), only in the WAN
Gateway Computer, each in its era's bus slot. A card takes the line of its era and the earlier ones, like cable ports:

| Era | Card | Line | The real equivalent |
| --- | --- | --- | --- |
| Vintage | Modem Card | Telephone Line | the internal modem |
| Legacy | T1 Card (with CSU/DSU) | Leased Line | the T1/E1 card |
| Transition | T3 Card | T3 Line | the DS3 card |
| Standard | VLDC Card | VLDC | the VLDC card |
| Advanced | Optical Transport Card | Dark Fibre | the coherent optical transceiver |

The motherboard is its era's server board. The processors count too: what crosses per tick is the **smaller** of the
line's speed and the gateway's capacity, so a gateway with a weak processor is the bottleneck.

**Two modes**, by what is at the other end of the line:

- **Extension:** there is no Mainframe on the other side. The distant place is part of the **same** network, like a
  branch office: its machines join the network's index, at the line's speed.
- **Federation:** there is a Mainframe on each side. They are two networks linked, never merged; each keeps owning its
  index, and one Mainframe per network is still the rule.

**How an Operation crosses a federation:** the asking Mainframe sends the request to the other network's; that one plans
its half in its own storage and sends the items down the line. Each side logs it, and locks stay local to each network.
The Network Interactor shows the other network in a view of its own ("Federated"), never mixed with the local one. If
the line falls in the middle, the items in transit are lost by the rule for a network that splits ([Power](power.md),
configurable).

**Speed:** that of the slowest cable on the path, with the long-distance line at its own figures (1, 8, 24, 64 and 256
items per tick, from the Vintage to the Advanced).

**Permissions, before security exists:** per port, what may cross and in which direction (SELECT, pull, push, craft),
and turning the port on or off. Groups of players come with security ([Security](security.md)).

**Cost:** no merging of indexes every tick; a query to the other network goes to its Mainframe when it is asked.
