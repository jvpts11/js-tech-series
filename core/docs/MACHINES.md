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
- **A recipe file does nothing.** It failed to load: the log says why (no input, no output, an unknown item). A
  data pack's recipe with the same path as one of yours replaces yours.
- **The kind has no page in JEI or EMI.** Its machine has no item, and a viewer needs one to show what works the
  kind.
