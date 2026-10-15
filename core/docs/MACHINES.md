# Machines

How to build a machine that turns some things into others on J's Core: a macerator, a press, a chemical
reactor. The Core gives you the machine's whole working (slots, tanks, energy, progress, upgrades), a recipe
format players and data packs can add to, and the pages of JEI and EMI that show those recipes. What you write
is what is particular to your machine.

J's Industrial's Macerator, Compressor and Electric Furnace are built exactly this way, and are the examples
below.

## The words

- A **processing machine** takes **inputs** (items, fluids), works on them for a number of **ticks** (the game
  runs 20 ticks a second), spending **energy** every tick, and puts out **outputs**.
- A **recipe** says one such transformation: what goes in, what comes out, how long it takes and what it costs.
- A **recipe kind** is a family of recipes worked by the same kind of machine: every macerating recipe is
  worked by a macerator. Each kind has its own recipe files, its own page in JEI and EMI, and its own name.
- **FE** (Forge Energy) is the energy unit every NeoForge mod shares. A machine takes it from a cable or a
  generator beside it.
- An **upgrade** is an item put in a machine's upgrade slot that changes how fast it works and what it spends.

## Declaring a recipe kind

*Added 2026-10-05.*

```java
public static final ProcessingKind PRESSING = CONTENT.processing("pressing", "Pressing", () -> MyContent.PRESS);
```

That one line registers a recipe type and its serializer named `mymod:pressing`, gives the kind the English
title "Pressing" (written to your language file as `mymod.recipe_kind.pressing`), and names the press as the
machine that works it. Another machine can work the same kind too: `PRESSING.alsoWorkedBy(() -> BIG_PRESS)`.

## The machine

*Added 2026-10-05.*

A processing machine is a block entity extending `ProcessingMachineBlockEntity`. It says how its slots are laid
out, how much energy it holds and takes, what it spends a tick when a recipe does not say, and how it finds the
recipe for what is in it:

```java
public class PressBlockEntity extends ProcessingMachineBlockEntity {

    public static final Layout LAYOUT = Layout.items(2, 2).withUpgrades(1).withTanks(1, 1, 4_000);

    public PressBlockEntity(final BlockPos pos, final BlockState state) {
        super(MyContent.PRESS_BE.get(), pos, state, LAYOUT,
                16_000,     // the energy it holds, in FE
                1_000,      // the most it takes in a tick
                40);        // what it spends a tick when a recipe names no energy of its own
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return byKind(level, MyContent.PRESSING, input);
    }
}
```

- `Layout.items(inputs, outputs)` gives that many input and output slots; `withUpgrades(n)` adds upgrade slots;
  `withTanks(in, out, capacity)` adds fluid tanks, each holding `capacity` millibuckets (1000 is a bucket).
- `process` answers what the machine would do with its current inputs. `byKind` looks the recipes of a kind up
  and picks the one that fits. A machine that works some other recipe can answer for itself instead: the
  Electric Furnace answers from the game's own smelting recipes with `Processing.single(slot, output, ticks)`.
- Tick it with `ProcessingMachineBlockEntity::serverTick` (through a `Device`, see [Content](CONTENT.md#devices-a-block-that-runs-its-block-entity-and-opens-a-menu)).

What it does each tick, and why:

- **Nothing to work, or no room for every output at once:** its progress goes back to zero. A recipe is only
  started when all of what it makes can be put out, so a machine never ends a recipe it cannot finish.
- **No energy for this tick:** it waits, keeping its progress, and carries on when energy comes.
- **Otherwise:** it spends the tick's energy and moves one tick on. When the recipe's ticks are done it takes
  the inputs, puts out the outputs and starts again.
- Pipes and cables put into its inputs and upgrade slots and take from anywhere; its output slots only take
  what the machine makes. Its inventory falls out when it is broken.
- It answers what tools like Jade show about it: "Working: 45%" or "Idle" ([Overlays](OVERLAYS.md)).

## What every machine shares

*Added 2026-10-15.*

Every machine built on `MachineBlockEntity`, a processing machine included, has the same things a player meets on
every machine of every mod. Tick it with `MachineBlockEntity::serverTick` (`ProcessingMachineBlockEntity::serverTick`
names the same ticker), and the kit runs them; a machine that ticks itself keeps its own ways.

### Its state, always in view

Each tick the machine says what it is doing, a `MachineStatus`, which its screen and tools like Jade show
("State: Output full"), and which lights one of the three lamps every machine's face has:

| State | Lamp | When |
| --- | --- | --- |
| `WORKING` | RUN | It works. |
| `WAITING` | WAIT | Nothing in it makes anything. |
| `OUTPUT_FULL` | WAIT | Its outputs have no room for everything the recipe makes. |
| `NO_ENERGY` | FAULT | It has no energy for the tick. |
| `UNDERVOLTAGE` | FAULT | A line of a class below its port's meets it: it does not start. |
| `FUSE_BLOWN` | FAULT | Its fuse blew on overvoltage: it is shut down, unharmed, until a good fuse is put in. |
| `DAMAGED` | FAULT | It is damaged: it works slower, or not at all with no health left or a burnt port. |
| `OFF_BY_REDSTONE` | WAIT | Its redstone mode keeps it off. |
| `SWITCHED_OFF` | WAIT | Its ON/OFF switch is off. |

In the world a working machine shows it: a puff as it starts, smoke as it works and as it stops, and the sounds its
mod declares (`MachineSounds`: one as it starts, a loop while it works, one as it stops; a machine that declares none
is silent). Override `sounds()` and `animate(...)` to change them.

### Energy of a class

A machine runs on FE, as above, or on a mod's own energy ([Energy and resources](ENERGY_AND_RESOURCES.md)) through a
port of one class:

```java
super(MyContent.REACTOR_BE.get(), pos, state, LAYOUT,
        MachineEnergy.of(MyEnergies.VOLTS, MyEnergies.LOW, 100_000, 2_000),    // holds, takes a tick
        40);                                                                   // spends a tick, by default
```

Its port is offered by `EnergyPorts.PORT` on every side (and to FE cables as well, where the energy speaks FE), with
nothing to register. It has the **fuse** and the **battery** of its Protection tab (`power().protection()`): a line a
class too high blows the fuse and shuts the machine down unharmed; with no fuse, it burns the port and damages the
machine. `power()` is the machine's energy at work, whichever it runs on: what it holds, what it spends, its faults.

### Tanks of matter

Gases, supercritical fluids, slurries, plasmas and exotic matter go in tanks of their own
([Energy and resources](ENERGY_AND_RESOURCES.md)), as liquids go in fluid tanks:

```java
Layout.items(1, 1).withMatter(MatterState.GAS, 1, 1, 1_000, 10)   // a gas tank in and one out, 1000 mB at 10 times
```

Each state's lines find them through its capability (`MatterCapabilities.GAS`...): they fill the inputs and take
from the outputs. `matter()` holds them.

### The switch and redstone

`control()` holds the machine's ON/OFF switch, which turns it off whatever redstone says, and how it answers
redstone (`RedstoneMode`): `IGNORE` (it always runs), `HIGH` (only with a signal), `LOW` (only without one), or
`PULSE` (one recipe for each pulse). It also holds what a **comparator** reads of it, as the player picks
(`ComparatorReads`): its progress, how full its inputs or its outputs are, or its energy. The machine's block, a
`DeviceBlock`, gives that to a comparator, and anything that reads sensors reads it too (`Signals.OUTPUT`).

### Damage and repair

A machine does not wear. It is damaged only by overvoltage (a port burnt by a line too high), an explosion (which
damages it instead of breaking it), sabotage or overheating (`damage(points)`). A damaged machine shows cracks, and
works slower: at full speed when whole, half speed just before it stops, and not at all with no health left
(`MachineHealth.workShare()`).

A **repair kit** mends it, as Factorio's repair packs do: used on the machine and held, it gives back its rate a second,
spending itself as it goes, until the machine is whole. `RepairKitItem` is one ready to register; any item may be one
by implementing `IRepairKit`.

```java
public static final ItemEntry<RepairKitItem> REPAIR_KIT = CONTENT.item("repair_kit",
        properties -> new RepairKitItem(properties, 200, 20)).register();   // 200 health in all, 20 a second
```

### Values read and set from outside

A machine says which of its values the outside may read and set (`IMachineParameters`, offered on every side): how
J's Industrial's Industrial Controller Computer and J's Computers' network command any machine, and what the
network's tables show. Every machine declares `on` (set), `progress`, `input`, `output`, `energy` and `health`; a
machine adds its own:

```java
@Override
protected void declareParameters(final List<MachineParameter> declared) {
    declared.add(MachineParameter.set("pressure", PRESSURE, BAR, 1, 10, () -> pressure, value -> pressure = value));
}
```

A value set from outside is held to its span, and a value only read is never set.

### Owner

Whoever places a machine owns it; who else may use it is its access ([Ownership](OWNERSHIP.md)).

### It works only when it has something to do

A machine with nothing to do (nothing to work, its output full, no energy, switched or redstoned off) rests: its tick
returns at once until something it waits on changes. Anything saved that changes wakes it (an item put in or taken
out, energy arriving, a setting, its health), and so do its neighbours' changes; at the latest it looks again every
`SLEEP_AT_MOST` ticks. A large factory of idle machines weighs nothing on the server.

## Recipes

*Added 2026-10-05.*

A recipe is a JSON file in a data pack, at `data/<namespace>/recipe/<path>.json`. Your mod ships its own the
same way (generated, below), and players and pack makers add, change or remove recipes with their own data
packs, with no code.

```json
{
  "type": "mymod:pressing",
  "inputs": [ { "tag": "c:ingots/iron", "count": 2 } ],
  "outputs": [ { "id": "jscore:iron_plate", "count": 1 } ],
  "ticks": 120,
  "energy_per_tick": 30
}
```

| Field | What it says | Default |
| --- | --- | --- |
| `type` | The recipe kind: which machine works it. | required |
| `inputs` | Items taken, each `{"item": id}` or `{"tag": tag}` with a `count`. Each input needs a slot of its own. | none |
| `fluid_inputs` | Fluids taken, each a fluid or a fluid tag with an `amount` in millibuckets. | none |
| `outputs` | Items made, each `{"id": id, "count": n}`. | none |
| `fluid_outputs` | Fluids made, each `{"id": id, "amount": mb}`. | none |
| `ticks` | How long it takes, at least 1. | 200 |
| `energy_per_tick` | FE spent each tick; 0 means the machine's own default. | 0 |

A recipe takes at least one input (item or fluid) and makes at least one output, or it is refused with a line
in the log when the data loads. Asking for a tag (`c:ingots/iron`) rather than an item lets the recipe take the
iron of every mod.

From code, in the data generation, a recipe is written with `ProcessingRecipeBuilder`:

```java
ProcessingRecipeBuilder.of(MyContent.PRESSING)
        .input(Ingredient.of(Tags.Items.INGOTS_IRON), 2)
        .output(MaterialItems.get(ModMaterial.IRON, MaterialForm.PLATE).get(), 1)
        .ticks(120)
        .energyPerTick(30)
        .save(output, ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "pressing/iron_plate"));
```

Add the provider that does this to your data generation (`data.server(new MyRecipeProvider(...))`, see
[Getting started](GETTING_STARTED.md#writing-the-files-data-generation)).

## Upgrades

*Added 2026-10-05.*

An item becomes an upgrade by implementing `IUpgrade`, saying what one of it does:

```java
public final class SpeedUpgrade extends Item implements IUpgrade {

    private static final UpgradeEffect EFFECT = new UpgradeEffect(2.0, 1.5);

    public SpeedUpgrade(final Properties properties) {
        super(properties);
    }

    @Override
    public UpgradeEffect effect(final ItemStack stack) {
        return EFFECT;
    }
}
```

`new UpgradeEffect(speed, energy)`: `speed` is how many times as fast the machine works (2.0 halves a recipe's
ticks), `energy` how many times the energy a tick it spends (1.5 spends half as much again). Every item in the
upgrade slots counts, and their effects multiply: two upgrades of 1.5 speed make a machine 2.25 times as fast.
A recipe never takes less than one tick, and the energy a tick is rounded up.

## JEI and EMI

*Added 2026-10-05.*

JEI and EMI are mods that show players every recipe in the game. When either is installed, every recipe kind
declared on the Core gets a page of its own, with no code of yours: the inputs, an arrow with the time and the
energy, the outputs, and the machines that work it (so looking up "how is this used" on a machine lists its
recipes). The Core's integration only loads when the viewer is there, so neither is a dependency of your mod.

Screens can keep the viewers' item lists from covering them: a screen that implements `IKeepsViewersClear`
returns the rectangles (in screen pixels) that must stay clear, and both viewers keep off them.

## What can go wrong

- **The machine never starts.** No recipe of its kind matches what is in it: check the recipe's `type`, and
  that each input has a slot of its own (two inputs need two input slots).
- **It starts and drops back to 0% again and again.** Its outputs have no room for everything the recipe makes.
  Empty the output slots.
- **It stops partway and waits.** It has no energy. A generator or an energy cable must reach it, and it takes
  no more a tick than its most-in-a-tick, so a large cost needs a machine that takes enough.
- **It says "Off by redstone".** Its redstone mode wants a signal it does not have, or one it has not to have; in
  one-recipe-a-pulse, it waits for the next pulse.
- **It says "Fuse blown".** A line of a class too high met it. Put a fuse that works in its Protection tab.
- **It works slower and shows cracks.** It is damaged: use a repair kit on it.
- **A recipe file does nothing.** It failed to load: the log says why (no input, no output, an unknown item). A
  data pack's recipe with the same path as one of yours replaces yours.
- **The kind has no page in JEI or EMI.** Its machine has no item, and a viewer needs one to show what works the
  kind.
