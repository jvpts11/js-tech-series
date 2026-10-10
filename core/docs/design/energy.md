# Energy

The energy every mod of the series shares: how it moves, how it is measured, what happens when a block meets the wrong
voltage, and how it speaks with the FE of every other mod. The generators, cables, storage and grid equipment of the
series are J's Industrial's; the system they run on is the Core's, so every mod and add-on uses one energy.

## What exists today

The Core moves **FE**, the energy unit every NeoForge mod shares ([Energy and resources](../ENERGY_AND_RESOURCES.md)):

- A block is a **generator**, a **consumer** or a **buffer** by what its energy store gives and takes. Items and
  entities (a vehicle's battery, a robot) can hold energy too.
- **Energy moves in the same tick, and cables hold none.** Every tick, after the machines have worked, what the
  generators on a run give is shared among what its consumers want, in proportion, never more than each can take.
- **No energy is made or lost by rounding**: the shares are whole numbers that add up exactly to what was given, the
  remainder going one unit at a time to those whose share was rounded down the most.
- A cable carries at most its throughput a tick, the narrowest cable of a run limits all of it, and a cable may lose a
  share of what it carries, rounded up, so a lossy cable never carries energy for free.
- **Seven energy tiers** exist as a type, with their ceilings, and a mod can register a **unit** of its own with its
  ratio to FE (conversions round down, so converting never makes energy).

## To build

### Two layers: the energy kit and J's Energy

- **The energy kit** is the platform's: everything a mod needs to use energy, its own or FE, **easily**. A mod declares
  an energy (its units, its classes of line if it has any, what a port of each class takes, its loss by distance, what
  happens at the wrong class, how it converts to and from FE) and gets the rest from the Core: stores, ports, cables,
  the sharing among consumers, the protection tab, the converters' working and the tooltips. A mod that only wants FE
  says so in one line and has FE stores, cables and machines with nothing else to learn.
- **J's Energy** is the series' energy, in the series' package ([The API](api.md)), **built only from the kit**, as any
  mod could build it. Nothing the series' energy does is closed to another mod: with the Core alone, anyone can make an
  energy as rich as the series', or a much simpler one.

The rest of this page is J's Energy, the series' use of the kit.

### J's Energy

The series' energy becomes **J's Energy**, measured in **real units**: joules for energy, watts for power, volts for the
class of a line. A PC draws 300 W, an arc furnace tens of MW, a nuclear plant a GW.

- It keeps today's rules: same tick, nothing held in cables, shares that add up exactly.
- A cable carrying more than its ceiling **saturates without a sound**: the excess just doesn't pass.
- When there is not enough, consumers share what there is in proportion and **run slower** in the same proportion, never
  in fits and starts.
- **Generators follow the load**: they burn only what the grid draws, as a power station with a governor does. A
  generator may declare a minimum and a slow change of power, as reactors do.
- The seven energy tiers give way to the **voltage classes**.

### Voltage classes

| Class | Voltage | In real life |
| --- | --- | --- |
| Low | 400 V | up to 1 kV: the socket and the factory |
| Medium | 11 kV | 1 to 35 kV: the neighbourhood's grid and the big factories |
| High | 138 kV | 35 to 230 kV: regional transmission |
| Extra-high | 500 kV | 230 to 800 kV: national transmission |
| Ultra-high | ±1,100 kV, direct current | above 800 kV: lines of thousands of kilometres |

- Every block that takes or gives energy has a **port in one class**, shown in its tooltip.
- **Cables and lines of different classes never join.** A **transformer** joins neighbouring classes and an **HVDC
  converter station** joins the ultra-high class, which is direct current; the Core knows what they are, and J's
  Industrial builds them.
- Voltage is how energy travels, not how the game progresses. There is no amperage and no machine per voltage.

### Loss by distance

Every block of cable or overhead wire loses a fixed share of what it carries, and the shares add up along the way; there
is no length limit, distance costs by itself. The share is counted in **millionths** per block, so a line can lose as
little as 0.01% per hundred blocks. On an overhead line, the loss counts the wire's length in blocks
([Cables and lines](cables-and-lines.md)).

### The wrong class

What happens at a **port** that meets a live cable or line of another class:

- **one class above**: the block **burns**, stops and is left **damaged** until it is repaired
  ([Machines](machines.md));
- **two classes above or more**: an **arc flash**: the block is destroyed, sets fire around it and burns whoever is
  close;
- **below**: the block doesn't start and shows "Undervoltage"; nothing is damaged.

It goes for every block with a port, generators and storage included, and only while the line is live.

**The protection tab.** Every machine's screen has a **Protection** tab, from the Core, with:

- a **fuse slot**: on overvoltage the fuse blows, cuts the energy and shuts the machine down with no damage. A fuse has
  a rated voltage, and one facing a voltage it can't break lets the arc jump over it. The fuses themselves are a mod's
  items (J's Industrial's);
- a **battery slot**: the battery charges from the grid and powers the machine when the grid goes down, as a UPS does.

### Speaking FE

- The **FE converters** are J's Core's blocks, with recipes of vanilla materials, so any mod alone can use them: one per
  class (from 100 kW at low voltage to 12 GW at ultra-high) and a **Configurable Energy Converter** whose class,
  direction and limits the player sets.
- **1 FE = 1 J** by default: 1 FE a tick is 20 W, so a real 10 kW machine draws 500 FE a tick, the size of other mods'
  machines. The rate is a server setting, never a player's: a converter that changed it would make energy from nothing.
- A server setting lets J's Energy blocks **take and give FE directly**, with no converter, so the series speaks FE with
  the rest of a pack.

### The basic cable and the basic battery

So that a mod used alone can carry and keep energy, the Core has:

- the **basic cable**: a low-voltage cable with a recipe of vanilla materials. With J's Industrial installed it is the
  same item as its first cable, the Insulated Copper Cable, which J's Industrial gives its industrial recipe;
- the **basic battery**: one storage block with a recipe of vanilla materials, so a mod alone (J's Space's solar bases
  through the lunar night) keeps energy. With J's Industrial installed it is the same item as its first energy bank, the
  Lead-Acid Battery Bank.

### Shocks and surges

Overhead lines are bare wire: touching a live one hurts, more the higher its class, and on high voltage it kills.
Lightning on an overhead line or a pole sends a **surge** along it, which burns the machines on it unless a fuse or a
surge arrester stops it. Insulated cables don't shock.

### Seeing the energy

- **Storage shows its charge on its own model**, as bars or a level, without opening a screen.
- **Meters** read the energy through a line and through each face of a block.

### Settings of consequence

In the Core's server settings ([Implementation](implementation.md)):

- **overvoltage damages machines**, on by default; off, a block on a class above its port just doesn't start;
- **accept any voltage**: blocks take any class;
- **accept FE directly**, and the **rate between FE and joules**;
- **electric shock** and **lightning surges**, each can be turned off.
