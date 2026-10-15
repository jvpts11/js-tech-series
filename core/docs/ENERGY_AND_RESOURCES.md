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
- a cable carries at most its throughput a tick, and the narrowest cable of a run limits all of it; past that it
  saturates without a sound, the excess just not passing;
- each block of a cable loses so many millionths of what it carries, its own share and its energy class's loss by
  distance together, added up along the way and rounded up, so a lossy cable never carries energy for free and a line
  can lose as little as a hundredth of a percent over a hundred blocks.

```java
CableType.builder(MY_ENERGY_LINE)
        .grid(GridKind.POWER)
        .lane(Lane.BOTTOM_LEFT)
        .carries(2_000, 0)     // 2,000 FE a tick, any length
        .loses(5_000)          // 0.5% lost each block
        // the jacket, the plug, its name...
```

### An energy of a mod's own

*Added 2026-10-15.*

A mod that wants more than FE declares its energy once, and the Core gives it the rest:

```java
public static final EnergyClass LOW = EnergyClass.rated("low", LOW_NAME, LOW_RATING);    // "Low", "400 V"
public static final EnergyClass HIGH = EnergyClass.rated("high", HIGH_NAME, HIGH_RATING);
public static final Energy VOLTS = Energies.builder(rl("volts"), VOLTS_NAME)
        .unit(JOULE)                        // an EnergyUnit, whose ratio says what it is worth in FE
        .power(WATT_SYMBOL)                 // a rate shown as power, twenty ticks to the second
        .classes(LOW, HIGH)                 // lowest first; none for an energy with no classes
        .loss(LOW, 100).loss(HIGH, 1)       // millionths lost each block, by class
        .wrongClass(IWrongClassRule.STANDARD)
        .declare();
```

- **Each class has a grid of its own** (`VOLTS.grid(LOW)`), so lines of different classes never join; a cable of a
  class is a cable of that grid.
- **A block's port** is an `IEnergyPort` offered by `EnergyPorts.PORT`: an `EnergyStore` (what it holds, takes and
  gives a tick, saved as a part of the block's fields) or a `GeneratorPort`. Every port is of one class.
- **The wrong class.** A port of another class takes and gives nothing on a line, and, each tick the line is live,
  learns what befalls it by the energy's rule: `IWrongClassRule.STANDARD` burns a port one class below the line
  (its store stays damaged until repaired), hits one two or more below with an arc flash (damaged, and its block's
  `onArcFlash` ruin runs), and leaves one above short of voltage (`undervoltage(gameTime)`, so its block does not
  start). `REFUSES` harms nothing; `ANY` lets every class through.
- **FE.** `Energies.FE` is the game's own energy, whose one grid is `POWER`; a mod that only wants FE needs nothing
  else. A port's `asFe()` (`FeBridge`) is what an FE-only mod sees of it, and `EnergyConversion.convert` is how a
  converter works either way; both round down, so no energy is made.
- **Tooltips.** `EnergyTooltips.port(energy, class)` names a port's class and rating ("Port: Low, 400 V"), and
  `Energy.amount(...)` and `rate(...)` show amounts and power.

### Short of energy, and generators that follow the load

*Added 2026-10-15.*

When a run has less than its consumers want, each gets its share in proportion, and a machine spends what it has
and works slower in the same proportion (`PartialWork`), never in fits and starts; the Core's processing machines
do this and say "Low power" when looked at.

A generator with a `LoadFollower` makes only what its grid draws, so it burns only that much. It may declare a
minimum it always makes and a ramp, the most its output changes in a tick, as reactors do:

```java
private final GeneratorPort port = new GeneratorPort(VOLTS, LOW, new LoadFollower(5_000, 1_000, 200));

// Each tick, in the block entity:
final long made = port.follower().settle();    // what the grid drew last tick, or the floor, if more
burnFuelFor(made);
```

### Energy in items and entities

`.holdsEnergy(capacity, in, out)` on an item's declaration gives it a store chargers and machines reach
([Content](CONTENT.md#items-that-hold-things)); `.holdsEnergy()` on an entity's, for one that implements
`IEnergyHolder` (a vehicle's battery, a robot), does the same for entities.

### Other units

A mod that counts energy in a unit of its own registers an `EnergyUnit` in `CoreEnergy.UNITS`, with its name, its
symbol and its ratio to FE; conversions round down, so converting never makes energy. An energy of a mod's own
(above) moves along its lines in its own unit, and meets FE only through a converter or a port's FE view.

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

## States of matter

*Added 2026-10-15.*

Solids are items and liquids are fluids. Each other state of matter (a gas, a supercritical fluid, a slurry, plasma,
exotic matter) is a kind of storage of its own, with a capability of its own in `MatterCapabilities` (`GAS`,
`SUPERCRITICAL`, `SLURRY`, `PLASMA`, `EXOTIC`, and an item capability of each), offering an `IMatterHandler`. Amounts
are in mB as the matter would be at the pressure of the open air, held as a `MatterStack`: a material of the
catalogue and an amount.

A block keeps a `MatterTank`, which holds one material at a time, its volume times its pressure:

```java
private final MatterTank tank = new MatterTank(MatterState.GAS, 1_000, 200, this::setChanged);  // 200 000 mB

// Offered by its capability, saved with the block entity:
event.registerBlockEntity(MatterCapabilities.GAS, MY_TANK_BE.get(), (entity, side) -> entity.tank());
tank.save(tag);
tank.load(tag);
```

A `ContainmentTank` holds plasma or exotic matter while it is powered: its block calls `tick(upkeep)` each tick
with what pays its upkeep, which it spends for as long as it holds something. Without energy it takes nothing more;
plasma dissipates, and exotic matter stays sealed in, given to no one, until the energy comes back. A broken
containment lets plasma out and keeps exotic matter for its block to drop with it.

`MatterRelease.release(level, pos, state, stack)` lets matter out where it is lost: a gas rises, a supercritical
fluid flashes into gas, a slurry spills, plasma flares. Exotic matter is never let out.

Each state has its own line: a cable of `GridKind.GAS`, `SUPERCRITICAL`, `SLURRY`, `PLASMA` or `EXOTIC` moves
only that state, shared as fluids are (outputs feed inputs, then tanks, then tanks feed inputs), each move all or
nothing. Its throughput is the mB it moves a tick, the pressure it holds. A slurry runs only in slurry lines, so no
other line is worn by one.

Other mods' matter comes in through bridges: a gas line also meets a chemical mod's stores (each chemical in the
state its bridge says) and the tanks of mods that keep gases as fluids marked `jscore:gases`. `MatterBridges` links
a material of the catalogue to such a chemical or fluid, by id, so it moves from one to the other as the same
thing; one with no link moves only between stores of its own kind.

```java
MatterBridges.chemical(hydrogen, ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen"));
MatterBridges.fluid(steam, ResourceLocation.fromNamespaceAndPath("somemod", "steam"));
```

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
- **A tank refuses a gas.** It holds another material already, or it is full, or the material takes no such state
  (iron is no gas).
- **A containment takes nothing.** It has no energy for its upkeep.
- **Another mod's gas will not enter a Core tank.** No `MatterBridges` link makes it a material of the catalogue;
  unlinked, it moves only between that mod's stores.
- **A face takes nothing.** Its `FaceConfig` closes it, or the filter turns the item away.
