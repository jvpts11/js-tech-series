# Cables and lines

How everything that travels along the ground gets from one block to another: one shared cable block in which every mod
lays its own lines, and, for high voltage, wires hung between poles and towers. Each mod's design says which lines it
adds and what they do; this page is the system they share.

## What exists today

**One cable block for every cable**, `jscore:cable` ([Cables](../CABLES.md)):

- A block holds up to **nine wires**, each in a **lane** of a 3 by 3 grid seen end on, so a data cable, a peripheral
  cable and an energy cable run through the same block side by side without ever joining. A cable that must be alone
  holds the block by itself.
- A **line** is a job a cable does, with a **generation** that counts eras: a port takes cables of its own generation
  and every earlier one, never a newer one. One line owns one lane.
- A mod declares a **cable type**: its line, the **grid** that carries it (power, fluid, heat, gas, motion, data or
  peripheral), its lane, what it carries a tick and how far, what it loses, the temperatures and marks of the fluids it
  takes, whether it can turn inside a block, how many neighbours it joins, and its look.
- **Joining**: two wires join when they are of the same line and generation and their colours agree. A dye colours a
  wire; two colours never join, and an undyed wire joins every colour. Where a wire changes lane or two would cross, the
  block becomes a **junction box**. Breaking takes out only the wire or the part looked at.
- **Parts** sit on the block's faces (a bus, a cover, a sensor), each a registered type that saves, ticks and answers a
  player on its own; a part closes its face to wires.
- **Only the data grid carries a network's identity** ([Networks and Operations](networks-and-operations.md)).

## To build

### Every cable is a line of the block

Everything a mod lays along the ground is a line of the cable block, and each new line is a deliberate decision of the
mod's design, never added in passing. The series' lines:

| Mod | Its lines |
| --- | --- |
| **J's Core** | the **basic lines** (below) |
| **J's Computers** | the data lines (access, backbone, long distance, high compute, crafting), the peripheral cable |
| **J's Industrial** | energy cables by class; a line per state of matter: liquid pipes, gas pipes, high-pressure pipes for supercritical fluids, lined pipes for slurries, plasma conduits, exotic matter conduits, heat pipes; the compressed air line; the capsule tubes of pneumatic post; the Research Link Cable; the industrial control link |
| **J's Space** | the telemetry line; the Space Research Link Cable |

**Lines of different states, and energy cables of different classes, never join.**

### Grids are a registry

The seven grids are a closed list today; with a line per state of matter and lines that only group what they join, grids
become a **registry**, each with an id, and a mod adds the grid its line needs. Each grid has its **policy**: how what
it carries is shared along a run. The Core's energy and fluid policies are the defaults.

### Lines that group

Some lines carry nothing: they **group** the machines they join. Each connected run is one group, and the machines on it
work together: J's Industrial's Research Link Cable joins research computers, J's Space's Space Research Link Cable its
Space Research Computers, and its telemetry line makes one ground system of the antennas and devices it joins. A group
line may take colours, so two groups run side by side without joining. The Core gives the group (which machines are on
the run, and when it changes); each mod says what its group does.

### The basic lines

So that any mod used alone can carry energy and fluids, the Core has four lines with recipes of vanilla materials:

| Basic line | What it carries | With J's Industrial installed, it is |
| --- | --- | --- |
| Low-voltage cable | J's Energy, at low voltage | the Insulated Copper Cable |
| Liquid pipe | liquids | the Copper Pipe |
| Gas pipe | gases | the Iron Gas Pipe |
| Cryogenic pipe | liquefied gases | the Vacuum-Jacketed Pipe |

They are the same items as J's Industrial's first rung, which gives them its industrial recipes; every other cable and
pipe is J's Industrial's.

### How each end works

For lines that carry items, liquids and gases, in the spirit of Mekanism, EnderIO, Pipez and XNet. Every option has a
default that keeps a mod's own design (J's Industrial's machines push what they make, and a pipe from an output to an
input just works); the rest is there when a player wants it.

- **Modes per end**: normal, push, pull or off, set in the world with the wrench.
- **Priorities on destinations**: the highest fills first, and taking turns happens only among destinations of the same
  priority, so a lower one never gets what a higher one still wants.
- **Filters per end**: by item, tag, mod or data components, by fluid or tag; allow or deny; and **keep N in the
  destination**, which regulates instead of filling.
- **A rate** per end.
- **Channels within a line**: one cable carries up to sixteen separate channels, and each end picks its own, so one
  cable does the work of several. Colours still keep whole lines apart.

### Seeing what flows

- **Flow in sight**: items seen moving through transparent tubes, liquids showing their level in glass pipes, and, with
  goggles or the wrench, the direction and amount of what a line carries.
- **A network probe**: a network's members, what goes through it, **its bottleneck** (the narrowest cable) and its
  losses.

### Laying and keeping

- **Laying a run by dragging**: a whole stretch of cable at once, with joins made by themselves and turned off face by
  face.
- **Hiding cables** behind facades ([Multipart](multipart.md)).
- **Networks are worked out again only when they change**, with no tick per cable and transfers done in batches, so a
  huge network costs no more than a small one.

### Overhead lines

From medium voltage up, energy also travels in **overhead lines**: bare wire hung between insulators on poles and
towers, as in Immersive Engineering. They are **the only link outside the cable block**.

- A wire is hung from insulator to insulator, within its line's **longest span**, and its loss counts its length in
  blocks ([Energy](energy.md)). A wire can't pass through blocks: its path is checked when it is hung.
- **Connectors, relays and feedthroughs**: a connector is where what the wire carries enters or leaves, a relay only
  passes it along, and a feedthrough insulator takes a wire through a wall one block thick.
- Insulators go on poles, towers and the ports of blocks; a consumer that takes high voltage directly has an insulator
  where the line's wire ends.
- **Any line can be hung**, not only energy (redstone in sixteen channels between connectors, for one). The kit gives
  the ability; each line that uses it is still a decision of its mod's design.
- The Core gives the hanging, the spans, the drawing of the wire's sag and the touch of a live wire; J's Industrial
  gives the energy lines, poles, towers and insulators.

### Nothing by radius

What serves many machines reaches each one through a line, as in a real factory, never by a radius around a block.
Effects over an area exist only as immersion (zero gravity inside a chamber, the laws of another dimension), never as a
bonus to machines.

### Telemetry leaves the peripheral types

The point-to-point telemetry type the Core has today (256 blocks) gives way to J's Space's **telemetry line**, a line of
its own that groups antennas, ground-station devices and research computers, and carries data only.
