# The chain

*Added 2026-11-09.*

J's Computers has a chain of its own, from the ore in the ground to the parts a computer is built from, so it needs no
other mod to be played in survival. This page covers where its ores lie and the Electronics Workbench, the bench the
parts are assembled at. The design behind it is in [Survival](design/survival.md).

## Where the ores are

The chain's metals each have an ore of their own, which J's Core makes because J's Computers uses it. Iron, copper and
gold keep the game's own ores. Fluorine comes from fluorspar, a purple mineral, and lithium from spodumene, which is
pale lilac.

Every vein is in the Overworld. The metals of the later eras lie deeper and further apart, the last ones only in the
deepslate, so each era takes you further across the world and further down:

| Era | Ores | Height | A vein of each, about every |
| --- | --- | --- | --- |
| Vintage | lead, zinc, nickel, aluminium, tantalum, chromium, manganese, mercury (cinnabar), fluorspar | y 0 to 80 | 12 chunks |
| Legacy | cobalt, silver, lithium (spodumene) | y −16 to 40 | 16 chunks |
| Standard | zirconium, tungsten | y −64 to 0, deepslate only | 24 chunks |
| Advanced | molybdenum | y −64 to 0, deepslate only | 24 chunks |

A vein is about 3 by 3 chunks across and runs through the stone and the deepslate; fluorspar's also through granite,
andesite and tuff. Tin, the Vintage's too, lies in J's Core's own veins, from y −16 to 80. The Transition brings no
ore of its own: its indium comes from the zinc already mined.

Mining an ore gives its raw ore, as the game's ores do.

## The Electronics Workbench

There is one Electronics Workbench for each era: Vintage, Legacy, Transition, Standard and Advanced. A bench assembles
the parts of its era and of every era before it, and the bench of the next era. Each era's parts are a kind of recipe
of their own, and JEI and EMI show each kind with the benches that make it.

- **Lay the ingredients in the trays.** The window has nine trays. Put each ingredient the part asks for in a tray of
  its own, in any tray and any order, with at least the count the recipe asks for. The part shows faded beside the
  trays once the bench knows what you are making.
- **Leave nothing else in the trays.** A tray holding anything the part does not take keeps the bench from starting.
- **It works as a furnace does.** Each part takes its time; you may close the window meanwhile. The finished part waits
  in the part's tray until you take it, and the bench starts the next one while the trays still hold enough.
- **It draws energy through the socket on its back,** and nowhere else: 40 W for the Vintage bench, 60 W for the
  Legacy's, 300 W for the Transition's, 80 W for the Standard's and 120 W for the Advanced's, while it works. The power
  strip on the window has its lamp, what the bench draws and how much energy it holds. A server that turns the
  computers' draw off (`power.draw_energy`) turns the benches' off too.
- **It is the player's, by hand.** Nothing reaches its trays through its sides: no hopper, pipe or Crafting Interface.
  To make parts on their own, each era has a machine of its own.

Breaking a bench drops it with everything in its trays.

## What can go wrong

- **The bench does not start.** A tray holds something the part does not take, an ingredient is short of its count,
  the part is of a later era than the bench, or no energy reaches its back. The lamp on the power strip is dark while
  the bench has no energy.
- **The bench stopped with the part half done.** The part's tray is full: take the finished parts out.
- **A hopper puts nothing in the bench.** It takes nothing from its sides; lay the ingredients in by hand.
