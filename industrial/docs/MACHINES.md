# The machines

Everything J's Industrial adds today, explained for a player who has never used a machine mod. Every number
here is read from the mod itself. They are first numbers, picked to make the machines run, and they will change
as the mod is balanced.

## The words you need

- **FE** (Forge Energy) is energy, the way most machine mods count it. A generator makes it, a cable carries it,
  a machine spends it. Every mod that uses FE works with every other, so a generator from another mod can run
  these machines and the other way round.
- A **tick** is the game's beat: 20 ticks make a second. "40 FE a tick" is 800 FE a second.
- A machine's **buffer** is the energy it can store, like a small battery inside it. It fills up while energy
  comes in, so a machine keeps working for a while after its power goes.
- **Dust** is a metal ground into powder; a **plate** is a metal pressed flat. They are the parts later
  machines and recipes are made from.

## Getting started

1. Take the machines from the creative menu, tab "J's Industrial" (there are no crafting recipes for them yet).
2. Place a **Coal Generator**, and a **Macerator** right against it: a generator gives its energy to every
   machine touching it, no cable needed.
3. Open the generator (use it) and put coal in its slot. Its flame lights and its energy bar fills.
4. Open the Macerator and put a raw iron in its left slot. The arrow fills and two iron dusts come out on the
   right.
5. Smelt the dust in a furnace or in the **Electric Furnace**: each dust is an ingot. One raw iron, two
   ingots.

To power a machine that is not touching the generator, lay **Energy Cable** from one to the other.

## Coal Generator

*Added 2026-06-05.*

**What it is.** A block that burns fuel to make energy.

**What it is for.** Powering the other machines, at the start, before anything better exists.

**How to use it.** Put furnace fuel in its slot: coal, charcoal, planks, logs. It takes one item at a time and
burns it for as long as a furnace would, making **20 FE a tick** the whole time. One coal burns for 1,600 ticks,
so it makes 32,000 FE. Its buffer holds **16,000 FE**, and every tick it gives energy to every machine touching
it, up to 1,000 FE a tick; Energy Cable carries it to machines further away. (Do not burn a lava bucket in it:
it takes the whole bucket and gives no empty one back.)

Its screen shows the fuel slot, a flame that shrinks as the fuel burns down, and its energy bar.

**What can go wrong.**
- *The flame is lit but nothing gets energy.* No machine or cable touches it. Put the machine against one of its
  faces, or lay Energy Cable from it.
- *It burns fuel but its bar stays full.* Nothing is taking its energy. While its buffer is full, the item
  already burning keeps burning and its energy is lost; it does not start a new item until there is room.
- *A machine works in bursts.* One generator makes 20 FE a tick, and a Macerator spends 40. The machine runs
  until its buffer empties and waits for it to fill again. Put a second generator against it.

## Macerator

*Added 2026-06-05.*

**What it is.** A machine that grinds ores and metals into dust.

**What it is for.** Getting more from each ore: one ore or raw ore becomes two dusts, and each dust smelts into
an ingot.

**How to use it.** Give it energy, put what to grind in its left slot and take the dust from the right one.

| Put in | Get out | Time | Energy |
| --- | --- | --- | --- |
| Any iron ore | 2 Iron Dust | 200 ticks (10 s) | 40 FE a tick, 8,000 FE in all |
| Raw Iron | 2 Iron Dust | 200 ticks | 8,000 FE |
| Iron Ingot | 1 Iron Dust | 200 ticks | 8,000 FE |

It holds **16,000 FE** and takes up to **1,000 FE a tick**.

## Compressor

*Added 2026-06-05.*

**What it is.** A machine that presses metal into plates.

**How to use it.** Give it energy, put ingots in its left slot and take the plates from the right one.

| Put in | Get out | Time | Energy |
| --- | --- | --- | --- |
| Iron Ingot | Iron Plate | 120 ticks (6 s) | 30 FE a tick, 3,600 FE in all |
| Copper Ingot | Copper Plate | 120 ticks | 3,600 FE |

It holds **12,000 FE** and takes up to **600 FE a tick**.

## Electric Furnace

*Added 2026-06-05.*

**What it is.** A furnace that runs on energy instead of fuel.

**How to use it.** Give it energy and put in anything a furnace smelts. It smelts it by the same recipes as a
furnace, in **160 ticks** (8 s, a little faster than a furnace), spending **30 FE a tick**, 4,800 FE an item.

It holds **12,000 FE** and takes up to **600 FE a tick**.

## Energy Cable

*Added 2026-10-01.*

**What it is.** A cable that carries energy from generators to machines.

**How to use it.** Place it like a block, in a line from a generator to a machine. It carries any amount of
energy, any distance, and loses none on the way. It is laid in the shared cable block of the series, so it can
run through the same block as the cables of other mods of the series, each in its own lane, without joining
them.

## Iron Dust, Iron Plate, Copper Plate

*Added 2026-06-15.*

The materials the machines make. They belong to J's Core, so every mod of the series uses the same ones; this
mod shows them in its tab and makes them. Iron Dust smelts into an Iron Ingot in a furnace (200 ticks) or a
blast furnace (100 ticks).

## What every machine has in common

- It faces you when you place it, and opens its screen when you use it.
- Its screen shows its slots, an arrow that fills as it works, and its energy bar.
- **No energy:** it stops where it is and goes on when energy comes back; nothing is lost.
- **Output slot full:** it does not start, because it only starts what it can finish. Empty the output slot.
- **The input taken out halfway:** its progress starts over.
- Hoppers and pipes can put items in and take them out. Its output slot only gives.
- Breaking it drops what is inside.
- With J's Computers, the network can drive these machines and plan crafts through them.
