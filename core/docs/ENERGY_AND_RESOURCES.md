# Energy and resources

How energy, fluids, chemicals and items move between blocks on J's Core. Every one of them goes through the
standard capabilities of NeoForge, so they work with the machines and pipes of any mod, not only those of the
series.

## The words

- **FE** (Forge Energy) is the energy unit every NeoForge mod shares. A **generator** gives it, a **consumer** takes
  it, a **buffer** does both.
- A **capability** is how NeoForge lets one block reach what another holds: "give me your item store, on this
  face". A pipe asks a machine for its items; a cable asks a generator for its energy.
- **mB** (millibuckets): fluid amounts; 1000 mB is one bucket.

## Energy

*Added 2026-05-05.*

A generator, a consumer or a buffer is a block entity with an energy store marked `exposed()`
([Content](CONTENT.md#block-entities)):

```java
this.energy = fields().energy("Energy", 16_000, 1_000, 0).save().toMenu().exposed();   // a generator: gives only
```

What it can do follows from the store: one that gives but does not take is a generator, one that takes but does
not give a consumer, one that does both a buffer. A generator either pushes into what touches it, as J's
Industrial's Coal Generator does, or is reached by an energy cable.

### Energy cables

*Added 2026-10-01.*

An energy cable is a cable of the `POWER` grid ([Cables](CABLES.md)). Every tick, after the machines have worked,
the Core shares what the generators on a run give among what its consumers want:

- each consumer gets its share in proportion to what it wants, never more than it can take;
- the shares are whole numbers that add up exactly to what was given: what is left after rounding down goes, one FE
  at a time, to those whose share was rounded down the most, so no energy is ever made or lost by rounding;
- a cable carries at most its throughput a tick, and the narrowest cable of a run limits all of it;
- a cable with a loss loses that many thousandths of what it carries, rounded up, so a lossy cable never carries
  energy for free.

```java
CableType.builder(MY_ENERGY_LINE)
        .grid(GridKind.POWER)
        .lane(Lane.BOTTOM_LEFT)
        .carries(2_000, 0)     // 2,000 FE a tick, any length
        .loses(5)              // 0.5% lost
        // the jacket, the plug, its name...
```

### Energy in items and entities

`.holdsEnergy(capacity, in, out)` on an item's declaration gives it a store chargers and machines reach
([Content](CONTENT.md#items-that-hold-things)); `.holdsEnergy()` on an entity's, for one that implements
`IEnergyHolder` (a vehicle's battery, a robot), does the same for entities.

### Other units

A mod that counts energy in a unit of its own registers an `EnergyUnit` in `CoreEnergy.UNITS`, with its name, its
symbol and its ratio to FE; conversions round down, so converting never makes energy. Underneath, everything is FE.

## Fluids

*Added 2026-10-01.*

A tank is a block entity's `fields().fluid("Tank", capacity)` marked `exposed()`. A pipe is a cable of the `FLUID`
grid; its throughput is the mB it moves a tick.

```java
CableType.builder(MY_PIPE_LINE)
        .grid(GridKind.FLUID)
        .lane(Lane.BOTTOM)
        .carries(1_000, 0)
        .withstands(0, 1_800)          // the coldest and hottest fluid it takes, in kelvin
        .takes(CoreFluidTags.GASES)    // it holds pressure, so gases may go in it
        // ...
```

What moves where: what only gives (a pump, a machine's output) feeds what only takes, in proportion to what each
wants; what is left fills what both takes and gives (tanks), and tanks then feed what still wants. Two tanks alone
never pour into each other. A pipe carries only what every pipe of its run is made for: a fluid too hot or too cold
for it, or marked with a tag the pipe does not take, stays where it is. The Core's two marks are `jscore:gases`
(needs a pipe that holds pressure) and `jscore:corrosive`; they are data tags, so a data pack can mark any mod's
fluids.

Fluids themselves are declared on `ModContent` with `fluid(id)`, with their bucket and their block.

## Chemicals

*Added 2026-08-27.*

Some mods have chemicals that are neither items nor fluids (Mekanism's gases, for example). The Core handles them
as an id and an amount in mB, so the series' machines and network can move and store them like fluids, without
naming any such mod. A mod that brings chemicals registers a bridge (`ChemicalBridges.register(...)`, an
`IChemicalBridge` that finds a block's chemical store and names its chemicals). Without one, there are no chemicals,
and nothing breaks.

## Items and faces

*Added 2026-10-01.*

A machine that lets its owner choose what each face does (in, out, both, closed) keeps a `FaceConfig`:

```java
private final FaceConfig faces = new FaceConfig(FaceMode.BOTH, () -> this.facesField.changed());
private final PartField facesField = fields().part("Faces", this.faces).save().toClient();

IItemHandler itemsOn(Direction side) {
    return this.faces.handler(getBlockState(), side, this.inventory);   // null for a closed face
}
```

Faces are named from the block's own front (front, back, left, right, top, bottom), so turning the block keeps what
each face was set to. An `ItemFilter` lets only some items through: `ItemFilter.only(rules)` or
`ItemFilter.allBut(rules)`, each rule an exact item, an item by id, or a tag, with an amount.

`Transfers.moveItems(from, to, filter, max)`, `moveFluid` and `moveEnergy` move between two stores, all or nothing
([State and saves](STATE_AND_SAVES.md#several-moves-all-or-none)).

## What can go wrong

- **A machine gets no energy from the cable.** Its store is not `exposed()`, or it is full, or the cable's run is cut
  or longer than its range.
- **Energy goes missing.** A cable of the run has a loss: it is lost on the way, as said.
- **A fluid will not enter a pipe.** It is too hot or too cold for some pipe of the run, or marked as a gas or
  corrosive and the pipe does not take that mark.
- **A face takes nothing.** Its `FaceConfig` closes it, or the filter turns the item away.
