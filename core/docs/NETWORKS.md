# Networks

Two kinds of connection J's Core keeps between blocks: the **data network**, every device joined by data cable
around one Mainframe, which can be asked to do things ([Operations](OPERATIONS.md)); and **peripheral links**, a
short cable from one machine to its own screens, speakers and drives. The cables themselves are on
[Cables](CABLES.md); this page is what they join.

## The data network

*Added 2026-04-29.*

A data network is every block joined by data cable to one Mainframe, the computer that runs it. It has an
identity of its own, a `NetworkUuid`, which every member carries, and each member has its own `NodeUuid`. The two
are kept as types of their own, not bare `UUID`s, so a network's id can never be handed where a node's is wanted.

What the network may do with each block is its **category** (`NetworkCategory`):

- **A**: carries the network but is not a member of it, the way a cable is. It has no identity, so the network
  cannot address it.
- **B**: a member the network can read but never give work to, such as a block it inspects for its state.
- **C**: a member that carries out Operations: the computers. Only these receive work from the Mainframe.

The members are kept as a closed family of records (`INetworkNode`: computer nodes such as the Mainframe and
Subframes, and service nodes such as storage servers) that J's Computers fills in. Routers and switches are not
members at all: they are **topology elements**, which shape the network and never receive work.

### How cable runs join

When a cable is placed, the Core looks at what it touches:

| It touches | It becomes |
| --- | --- |
| nothing registered | a run of its own |
| runs with no network | one run, still with no network |
| runs of one network | part of that network |
| runs of two networks | a conflict: they are joined under one of the two |

Cutting a run in two leaves the part away from the Mainframe without a network. A run longer than its cable's
range carries nothing past the range, so blocks only beyond it are off the network even though the cables touch.

### How fast data goes

*Added 2026-10-02.*

Each data cable belongs to a **line** (its job) and an **era**, which give it a throughput (items a tick) and a
range (cables in one run, before a router or repeater starts it over):

| Line | Vintage | Legacy | Transition | Standard | Advanced |
| --- | --- | --- | --- | --- | --- |
| access | 4 / 32 | 16 / 48 | 64 / 64 | 128 / 80 | 256 / 96 |
| backbone | 16 / 96 | 64 / 160 | 256 / 200 | 512 / 512 | 2048 / 1024 |
| long distance | 1 / 2000 | 8 / 5000 | 24 / 7500 | 64 / 10000 | 256 / 20000 |
| high compute | none | none | 1024 / 24 | 2048 / 32 | 8192 / 48 |
| crafting | 32 / 16 in every era | | | | |

The network knows, between any two places, the **slowest cable on the fastest way**:
`ConnectivityIndex.slowestBetween(from, to)`. That is as fast as data can go between them. Routers and other
devices are not cables and slow nothing down.

### A block of yours on the network

A block takes part by saying which lines each of its faces takes:

```java
private final FacePorts ports = FacePorts.builder()
        .port(FaceRule.BACK, DataLines.upTo(HardwareEra.STANDARD, DataLine.ACCESS))
        .build();
```

`DataLines.upTo(era, line)` is what a port of that era takes: its own era's cable and every earlier one, never a
newer one. The block implements `IFaceConnector` and answers these ports; the cable and the block ask the same
question, so they never disagree about a face. A block that implements `INetworkBridge` joins the runs it touches
into one, as a router does.

## Peripheral links

*Added 2026-05-05.*

A **peripheral link** joins one machine (the **owner**, a computer) to its devices (the **endpoints**: a monitor, a
speaker, a drive) through peripheral cable. It is not part of the data network and carries no network identity.
Each endpoint has at most one owner; an owner has a number of **ports** of each kind (`DEVICE`, `VIDEO` for a
screen, `AUDIO` for a speaker) and links no more than it has.

An endpoint's block entity holds its link as a field, and ticks it:

```java
public class SpeakerBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING, MyLinks.WORLD);

    @Override
    public PortKind portKind() {
        return PortKind.AUDIO;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpeakerBlockEntity speaker) {
        speaker.link.tick((ServerLevel) level, pos);
    }
}
```

The link finds its owner by walking the cable, saves and sends which owner it has, and frees the owner's port when
the endpoint is broken. When a link cannot be made, it says why (`ILinkResult`): already linked to another owner,
the owner has no free port of that kind, no cable path, the path is longer than the cable's range, or a cable of
another system lies on the way. A **hub** (`IPeripheralHub`) takes one of its owner's device ports and offers
several of its own; the range starts over after it.

The peripheral cable's range is 8 blocks in the Vintage, 12 in the Legacy, 14 in the Transition, 16 in the
Standard and 20 in the Advanced. A device placed right against its owner needs no cable.

## What can go wrong

- **Two networks join into one, and one Mainframe is left out.** A cable touched runs of two networks; they are
  joined under one. Keep networks apart, or dye their cables differently ([Cables](CABLES.md)).
- **A block past a long run is off the network.** The run is longer than its cable's range: put a router or a
  repeater on the way.
- **A device will not link.** Read the reason: its owner is out of ports of that kind (a monitor needs a video
  output), the cable is too long, or it already has another owner.
