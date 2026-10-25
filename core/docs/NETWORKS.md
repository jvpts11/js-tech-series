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

### Machines of any mod on the network

*Added 2026-10-16.*

A machine of any mod becomes a member of the network it touches through the Core, with no tie to J's Computers' own
classes: its block entity implements `INetworkMember` and keeps itself on the network from its constructor. It says
what the network may do with it, the kinds of Operation it carries out, and what the network may read of it:

```java
public class AcceleratorBlockEntity extends SyncedBlockEntity implements INetworkMember {

    private final NetworkMembership membership = NetworkMembers.keep(this);

    @Override
    public NetworkCategory category() {
        return NetworkCategory.C;                   // read, and given work
    }

    @Override
    public List<OperationType<?>> carriesOut() {
        return List.of(MyOperations.ACCELERATE);    // registered from RegisterOperationTypesEvent, open to C
    }
}
```

From then on the machine is on the network of a data wire crossing into it from a cable block beside it: worked out
as it loads, as the blocks beside it change, and once a second besides, as networks form and part around it; off it
once it is broken or unloaded. `membership.node()` is its identity on the network, kept with it in the world, and
`membership.network()` the network it is on now.

A cable joins only a block that takes it on that face, so the machine's block says which faces take the data lines,
as an `IFaceConnector`. A block made with the Core's `Device` gets its ports from it (changed 2026-10-25; before, a
machine joined only a network whose cables were indexed by where they stand, which real cables never are):

```java
public static final BlockEntry<DeviceBlock> ACCELERATOR = CONTENT.block("accelerator",
        Device.of(() -> MyBlocks.ACCELERATOR_ENTITY.get())
                .ports(FacePorts.everyFace(DataLines.upTo(HardwareEra.SINGULARITY, DataLine.ACCESS,
                        DataLine.BACKBONE)))::block)
        .register();
```

The crafting line, which runs from a Crafting Computer to its interfaces, never joins a member to a network.

The network gives a machine more; it never makes it work: a member works through its own screens with no network at
all, and a world without J's Computers simply has no network for it to join.

What a member declares is checked as it is made, so one that could never be what it says stops at once, saying why:
a block that only carries the network (category A) is no member; a member only read (B) carries out no kinds of
Operation; and each kind it carries out must be registered and open to its category.

Whatever runs the network reads its members through `NetworkSystem`: `membersOf(network)`, `memberAt(pos)`, and
`membersCarryingOut(network, kind)`, the machines that can be given a kind of Operation. The work itself reaches the
machine as a request its kind's handler is handed, with the machine in it
([Operations](OPERATIONS.md#doing-the-work-the-request)).

A machine is called on the network by the name a player gave it on its own screen, when its block entity is a
vanilla `Nameable` with a custom name; otherwise by its block's name and the first six characters of its identity on
the network (`Accelerator-3fa2c1`). J's Computers' network language names it that way, or by where it stands.

### State as tables

*Added 2026-10-16.*

A mod declares the state of its machines as **tables**: a row for each machine on a network, a column for each value
it shows. Declare each table once, while the game loads, and keep it in a constant:

```java
public static final StateTable REACTORS = StateTables.declare(StateTable.builder(
                ResourceLocation.fromNamespaceAndPath("mymod", "reactors"), MyTexts.REACTORS)
        .number("temperature", MyTexts.TEMPERATURE, "°C")   // a number, in its unit
        .number("power", MyTexts.POWER, "W")
        .flag("running", MyTexts.RUNNING)                   // a yes or a no
        .text("name", MyTexts.NAME)                         // a piece of text
        .build());
```

A member says which tables it has a row in, and writes its row when asked, as it is at that moment:

```java
@Override
public List<StateTable> tables() {
    return List.of(MyTables.REACTORS);
}

@Override
public void fill(StateTable table, StateRow.Writer row) {
    row.set("temperature", temperature).set("power", power).set("running", running).set("name", name);
}
```

`NetworkTables.rows(level, network, table)` reads a table across a network, and `NetworkTables.tablesOf(level,
network)` lists the tables a network has rows in. A row says whose it is (`member()`, `pos()`) and gives each value
by its column's name (`number`, `text`, `flag`), or nothing where the machine had nothing to say. J's Computers'
QUERY reads them this way (`QUERY mymod:reactors WHERE temperature > 90`), so a player asks the network about a
reactor or a satellite the same way as about storage, and no mod depends on another to be read. A member whose chunk
is not in memory has no row, and reading the table never loads its chunk.

A column is named in lower case; its name is kept in saved queries, so it never changes once released. A row refuses
a value its column does not hold, and a column the table has not. After the game has loaded, the list of tables is
closed: a table declared later stops the game, and so does a name declared twice.

### Instruments of world events

*Added 2026-10-25.*

A member can be the **instrument** of kinds of world event ([World](WORLD.md)), as a seismograph is of earthquakes or a
ground station of solar storms: a network with it on sees those events coming, as a player carrying the event's
instrument is warned of it.

```java
@Override
public List<ResourceLocation> instrumentOf() {
    return List.of(MyEvents.SOLAR_STORM);
}
```

`NetworkTables.hasInstrument(level, network, kind)` answers whether a network has one, and
`NetworkSystem.networks()` lists the networks of a level that run.

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
- **A machine is not on the network.** No data cable of a network touches it, the face the cable touches takes no
  such cable, or the run between it and the Mainframe is too long. A machine joins within a second of a network
  reaching it.
- **The game stops as a machine is placed, saying it "is no member" or "is only read".** Its `category()` and
  `carriesOut()` disagree: answer C for a machine given work, and carry out kinds of Operation open to C.
