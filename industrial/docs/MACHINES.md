# The machines

Everything J's Industrial adds today, explained for a player who has never used a machine mod. Every number
here is read from the mod itself. They are first numbers, picked to make the machines run, and they will change
as the mod is balanced.

## The words you need

- **Energy** is counted in **joules** (J), and a thousand joules is a **kilojoule** (kJ). A generator makes it, a
  cable carries it, a machine spends it.
- **Power** is how fast energy flows, in **watts** (W): a watt is a joule a second, and a **kilowatt** (kW) a
  thousand of them. A machine of 4 kW spends 4 kJ every second it works.
- Energy runs at a **voltage class**. Every machine here is of **low voltage**, the class of the series' basic
  cable; higher classes, and the transformers between them, come with the bigger machines.
- A **tick** is the game's beat: 20 ticks make a second, so a machine of 4 kW spends 200 J a tick.
- A machine's **buffer** is the energy it can store, like a small battery inside it. It fills up while energy
  comes in, so a machine keeps working for a while after its power goes.
- **Dust** is a metal ground into powder; a **plate** is a metal pressed flat. They are the parts later
  machines and recipes are made from.

As it comes, a world counts this energy as FE, one FE a joule, so a generator or a cable of another mod that
speaks FE still runs these machines. A server that turns J's Energy on puts J's Core's Energy Converters between
them.

## Getting started

1. Take the machines from the creative menu, tab "J's Industrial" (there are no crafting recipes for them yet).
2. Place a **Coal Generator**, and a **Macerator** right against it: a generator gives its energy to every
   machine touching it, no cable needed.
3. Open the generator (use it) and put coal in its slot.
4. Open the Macerator and put a raw iron in its left slot. The arrow fills and two iron dusts come out on the
   right.
5. Smelt the dust in a furnace or in the **Electric Furnace**: each dust is an ingot. One raw iron, two
   ingots.

To power a machine that is not touching the generator, lay **Insulated Copper Cable** from one to the other.

## Coal Generator

*Added 2026-06-05; on J's Energy since 2026-10-24.*

**What it is.** A block that burns fuel to make energy: up to **5 kW** at low voltage.

**What it is for.** Powering the other machines, at the start, before anything better exists.

**How to use it.** Put furnace fuel in its slot: coal, charcoal, planks, logs. It burns only what the machines on
it draw, as a real power station follows its load: one coal holds **400 kJ**, which lasts 80 seconds at the full
5 kW and longer when less is drawn, and with nothing drawing it rests and keeps its fuel. It holds half a second
of its power, **2.5 kJ**, and twice a second gives it to every machine touching it; Insulated Copper Cable carries
it to machines further away. A lava bucket burns too, and the empty bucket comes back into its slot.

Its screen is the one every machine has: the fuel slot and its energy on the Main tab, and on the Sides tab the
faces energy leaves by and whether it gives it out by itself.

**What can go wrong.**
- *It works but nothing gets energy.* No machine or cable touches it, or the Sides tab closes that face to
  energy. Put the machine against one of its faces, or lay Insulated Copper Cable from it.
- *It is not burning.* Nothing draws its energy: it burns only what the machines take, and goes on as soon as they
  take more; nothing is lost.
- *A machine works in bursts.* One generator makes 5 kW, enough for one machine: a Macerator draws 4 kW, a
  Compressor or an Electric Furnace 3 kW. Two machines on one generator take turns. Put a second generator against
  them.
- *It makes half as much.* It is damaged: a damaged generator makes half its power until it is repaired.

## Macerator

*Added 2026-06-05.*

**What it is.** A machine that grinds ores and metals into dust, drawing **4 kW** at low voltage.

**What it is for.** Getting more from each ore: one ore or raw ore becomes two dusts, and each dust smelts into
an ingot.

**How to use it.** Give it energy, put what to grind in its left slot and take the dust from the right one.

| Put in | Get out | Time | Energy |
| --- | --- | --- | --- |
| Any iron ore | 2 Iron Dust | 200 ticks (10 s) | 4 kW, 40 kJ in all |
| Raw Iron | 2 Iron Dust | 200 ticks | 40 kJ |
| Iron Ingot | 1 Iron Dust | 200 ticks | 40 kJ |

It holds **8 kJ**, two seconds of its work, and takes up to **2 kJ** at once.

## Compressor

*Added 2026-06-05.*

**What it is.** A machine that presses metal into plates, drawing **3 kW** at low voltage.

**How to use it.** Give it energy, put ingots in its left slot and take the plates from the right one.

| Put in | Get out | Time | Energy |
| --- | --- | --- | --- |
| Iron Ingot | Iron Plate | 120 ticks (6 s) | 3 kW, 18 kJ in all |
| Copper Ingot | Copper Plate | 120 ticks | 18 kJ |

It holds **6 kJ** and takes up to **1.5 kJ** at once.

## Electric Furnace

*Added 2026-06-05.*

**What it is.** A furnace that runs on energy instead of fuel, drawing **3 kW** at low voltage.

**How to use it.** Give it energy and put in anything a furnace smelts. It smelts it by the same recipes as a
furnace, in **160 ticks** (8 s, a little faster than a furnace), spending 24 kJ an item.

It holds **6 kJ** and takes up to **1.5 kJ** at once.

## Insulated Copper Cable

*Added 2026-10-23, in place of the mod's own Energy Cable.*

**What it is.** The series' basic cable, J's Core's, a line of low voltage that carries energy from generators to
machines. It is on the "J's Tech" tab of the creative menu, with everything else the mods of the series share.

**How to use it.** Place it like a block, in a line from a generator to a machine. It carries up to **10 kW** and
loses a tenth of what it carries over a hundred blocks. It is laid in the shared cable block of the series, so it
can run through the same block as the cables of other mods of the series, each in its own lane, without joining
them.

## Iron Dust, Iron Plate, Copper Plate

*Added 2026-06-15.*

The materials the machines make. They belong to J's Core, so every mod of the series uses the same ones; this
mod makes them, and they are on the "J's Tech" tab. Iron Dust smelts into an Iron Ingot in a furnace (200 ticks)
or a blast furnace (100 ticks).

## What every machine has in common

- It faces you when you place it, and opens its screen when you use it: the screen every machine of the series
  has, from J's Core. **Main** shows its slots, an arrow that fills as it works, its state and its energy;
  **Sides** sets what each face takes in and gives out; **Redstone** sets how it answers a signal; **Upgrades**
  holds its upgrades and **Protection** its fuse and a battery.
- **Its tier, its class and its power** are its own: every machine here is of the first tier, at low voltage.
- **Upgrades:** a machine of the first tier has two upgrade slots; one of the second or third has three, and one of
  the fourth or later four. What upgrades gain adds up and what they save multiplies: two upgrades that each make a
  machine half again as fast make it twice as fast.
- **Damage:** a damaged machine works at half speed and draws a quarter more, and its upgrades do nothing until it
  is repaired.
- **The battery slot:** a battery charges while the grid keeps the machine full and powers it when the grid goes
  down, as a UPS does.
- **No energy:** it stops where it is and goes on when energy comes back; nothing is lost.
- **Output slot full:** it does not start, because it only starts what it can finish. Empty the output slot.
- **The input taken out halfway:** its progress starts over.
- Hoppers and pipes can put items in and take them out. Its output slot only gives.
- Breaking it drops what is inside; a wrench, used while sneaking, takes it away whole, with its contents, upgrades,
  battery, energy and settings.
- With J's Computers, the network can drive these machines and plan crafts through them.
