# The chain

*Added 2026-11-09.*

J's Computers has a chain of its own, from the ore in the ground to the parts a computer is built from, so it needs no
other mod to be played in survival. This page covers where its ores lie, the Electronics Workbench, the bench the
parts are assembled at, and the machines that make them in numbers. The design behind it is in
[Survival](design/survival.md).

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
  To make parts on their own, each era has its Electronics Assembly Line (below).

Breaking a bench drops it with everything in its trays.

## The machines

The chain's machines are multiblocks, each built block by block in a shape of its own. A Structure Projector shows
the shape of any of them before you build it.

| Group | Machines |
| --- | --- |
| Ore and metals | Crusher, Grinder, Induction Furnace, Electrolytic Cell, Rolling Mill, Wire Drawer |
| Chemistry and gases | Coke Oven, Chemical Reactor, Air Separation Unit |
| Silicon | Siemens Reactor, Crystal Puller, Wafer Saw |
| The chip factory | the Lithography Stations, Plasma Etcher, Ion Implanter, Deposition Chamber, Packaging Line |
| Boards, parts, glass and screens | Laminator, Board Etcher, Ceramic Kiln, Coil Winder, Optical Polisher, Display Line |
| Assembly | an Electronics Assembly Line for each era |
| Whole computers | the Prebuilder, the Preinstaller |

- **What a machine is built of.** Its casing, one of five by the work it stands (the Machine Casing for the mechanical
  machines, the Refractory Casing for heat, the Lined Casing for what corrodes, the Vacuum Casing for vacuum and gases
  and the Clean Room Casing for the Lithography Stations), its working blocks, which make it what it is, its entries
  and the Machine Terminal. The entries are told apart by their connector and their band: green takes in, orange gives
  out, yellow is energy, cyan is the Peripheral Port.
- **How it forms.** Place the Machine Terminal at the terminal's place, facing the way the machine's front should. It
  looks for the shape standing whole round it as it is placed, as a block beside it changes, and as you use it, and
  becomes that machine's own terminal. Formed, the machine is drawn as one, its entries drawing nothing of their own,
  and it shows what it works on in the colours of what goes in and comes out. Breaking any block takes it apart until
  the block is back; breaking the terminal gives back the Machine Terminal.
- **What reaches it.** Things go in through its inlets and come out through its outlets: items, liquids and gases each
  by their own. A hopper, a pipe, the network's buses or a Crafting Interface works it as any machine.
- **Energy.** Every machine but the Coke Oven draws J's Energy at low voltage through its Power Entry, only while it
  works; the Coke Oven burns part of its own coal. `power.draw_energy` off lets the machines run free too.
- **The Lithography Stations.** One for the Vintage (mercury lamp), the Legacy (KrF laser), the Transition and the
  Standard together (the ArF Lithography Station, its argon fluoride laser with immersion) and the Advanced (EUV).
  From the Legacy on, a station works only linked by peripheral cable, on its Peripheral Port, to a running computer
  of the era before its recipe's or a later one. The computer runs the exposure as its `litho` process, holding 2 MB
  for the Legacy's recipes, 32 MB for the Transition's, 256 MB for the Standard's and 2,048 MB for the Advanced's; a
  computer two eras newer than the one asked for exposes in half the time.
- **The Electronics Assembly Lines.** Each works the bench recipes of its era and of the older ones, in the time the
  recipe takes and at its era's bench's draw, its ingredients through its Item Inlet in any order.
- **The Prebuilder.** It takes one empty case (of a Personal, Crafting or Cluster Management Computer, or a rack
  server) and up to sixteen parts in any order, and puts every part in its seat by the assembly screen's rules, the
  board first, giving the computer with every part inside; a part that does not fit builds nothing, and it says why
  on its terminal's face and in its window. Formed, its two arms take each part from its tray and set it in the case
  on its lift, through the open side.
- **The Preinstaller.** It writes onto one thing at a time: a computer with its parts, a loose motherboard, a loose
  disk (how a rack server's bay drives get their system) or a rack server. It writes the firmware of the board's era,
  then a system and its programs from the install media among its inputs, read and never used up, by the
  installer's rules. A job takes its step and a second more for every 100 MB it writes. A motherboard can come with
  no firmware; a computer built on one powers up and turns its fans, and nothing more, until it is written.
- **A computer broken.** A one-block computer gives itself back whole: one item with its parts in their seats and its
  name, which stands again as it was. Only the media in its front drives come out on their own.
- **Only what the recipe takes.** A machine, as a bench, makes a recipe only from exactly what it takes: while an inlet
  holds anything the recipe does not take, it makes nothing of it.

What each machine draws and how long a step takes, as first estimates:

| Machine | A step | Draw | Machine | A step | Draw |
| --- | ---: | ---: | --- | ---: | ---: |
| Crusher | 8 s | 2 kW | Crystal Puller | 120 s | 6 kW |
| Grinder | 4 s | 1 kW | Wafer Saw | 20 s | 2 kW |
| Induction Furnace | 8 s | 4 kW | Lithography Station | 20 s | 2, 3, 4, 6 and 10 kW by era |
| Electrolytic Cell | 10 s | 3 kW | Plasma Etcher | 10 s | 3 kW |
| Rolling Mill | 4 s | 1.5 kW | Ion Implanter | 10 s | 4 kW |
| Wire Drawer | 4 s | 1 kW | Deposition Chamber | 10 s | 3 kW |
| Coke Oven | 30 s | none | Packaging Line | 20 s | 2 kW |
| Chemical Reactor | 10 s | 2 kW | Laminator, Board Etcher | 10 s | 1.5 kW |
| Air Separation Unit | 10 s | 3 kW | Ceramic Kiln | 20 s | 3 kW |
| Siemens Reactor | 60 s | 5 kW | Coil Winder | 4 s | 0.5 kW |
| Optical Polisher | 10 s | 1 kW | Display Line | 30 s | 3 kW |
| Prebuilder | 10 s | 1 kW | Preinstaller | 10 s, and 1 s every 100 MB | 1 kW |

## The recipes

*Added 2026-11-10.*

Everything J's Computers has is made from the chain: the materials and parts by the chain's machines, and the rest at
the bench of its era or by that era's Electronics Assembly Line. Each kind of thing is made by one rule, written out in
[Recipes](design/recipes.md), so of two alike the bigger takes more, and no two recipes a machine or a bench runs take
the same things. JEI and EMI show every recipe.

- **The parts.** A motherboard is its populated board, two for the big form factors, and its components: a socket for
  each processor, its memory and expansion slots, its drive connectors and its power connectors, each made at the
  bench. A processor is its microarchitecture's chips, one for every eight cores and one more for each faster model of
  its family, under its heat spreader; a graphics card is its chips, its memory, its capacitors and its heatsink.
- **The chips.** Every chip is made on a wafer, layer by layer. A Lithography Station exposes a layer through the
  chip's mask, then the wafer goes to the Plasma Etcher, the Ion Implanter and the Deposition Chamber, and back to
  the station for the next layer: two layers in the Vintage, three in the Legacy, four in the Transition, six in the
  Standard and eight in the Advanced. A wafer's tooltip names its chip, the layer it is on and the machine it goes to
  next. Once its last layer is laid, the Packaging Line cuts it into its chips, eight small ones or four big ones. A
  mask is a catalyst, never used up; in the Vintage the station writes it from the chip's Layout on a mask blank.
- **The gases.** The Air Separation Unit takes in the air itself and gives nitrogen, oxygen, argon and krypton, with no
  inputs.
- **The computers.** A computer is made as its case, empty, and its parts go in at its assembly window or by a
  Prebuilder. A Crafting Computer or a Cluster Management Computer is an empty Personal Computer of the same case with
  its era's card in it for good; a Personal Computer with parts inside is not taken. A server is its chassis with its
  fans and its rails.
- **The rest.** Every cable recipe gives 8. The machines' blocks, the Vent Panel and the Guide to Operations are made
  at the Crafting Table when there is nothing electronic in them, and at a bench when there is. Each era's bench is
  made at the bench of the era before, the Vintage's at the Crafting Table.

## What can go wrong

- **The bench does not start.** A tray holds something the part does not take, an ingredient is short of its count,
  the part is of a later era than the bench, or no energy reaches its back. The lamp on the power strip is dark while
  the bench has no energy.
- **The bench stopped with the part half done.** The part's tray is full: take the finished parts out.
- **A hopper puts nothing in the bench.** It takes nothing from its sides; lay the ingredients in by hand.
- **The terminal says no machine stands round it.** A block is missing, out of its place, or another stands where the
  shape wants its own; a Structure Projector shows which.
- **A machine stands formed but does nothing.** No energy reaches its Power Entry, what is in its inlets makes nothing
  it knows, an inlet holds something besides what the recipe takes, or its outlets are full.
- **A wafer goes nowhere.** Its tooltip names the machine it goes to next; another machine does nothing with it. A
  Lithography Station needs the chip's mask among its inputs.
- **A Personal Computer makes no Crafting Computer.** It has parts inside: take them out at its assembly window.
- **A Lithography Station exposes nothing.** Its computer is off, of an older era than the recipe asks for, not linked
  to its Peripheral Port, or short of the memory the exposure holds.
- **The Preinstaller says there is nothing to write.** The board's firmware is written, and no medium brings a system
  or a program the computer lacks.
- **A computer switches on and only turns its fans.** Its board has no firmware: its window says NO FIRMWARE. A
  Preinstaller writes it.
