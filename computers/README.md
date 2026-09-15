# J's Computers

Computers for Minecraft that do the logistics of your base. You build the hardware, install a system on
it, and the network of machines you end up with stores your items, moves them, and crafts for you.

This is the computing mod of the [J's Tech Series](../README.md); its id is `jsc`.

## What it is

Most storage mods either give you a very simple network, limited in what you can do beyond just storing and
organizing items, or give you a more capable one, but with much higher difficulty to do things. Not only that,
performance-wise, they tend to be heavy and slow the game's TPS. Besides that, they normally don't feel like
an actual network you're interacting with: you're just interacting with a huge chest that does not have any
depth to it.

J's Computers, however, is different: instead of giving you what most storage mods give, this one gives you
something much more capable, and that goes way beyond just storing your items: computers. Everything in this mod
is done through computers, be it autocrafting, storing, routing or moving; everything is done through computers.
There is, for now, a small but very powerful range of computers: Personal Computer (the most basic one), Crafting
Computer (the computer that performs autocrafting), Mainframe (the network's brain and heart; everything in the
network depends on the mainframe's hardware), Servers (there are three types, normal, storage and compute, each
one dedicated to its obvious task), Supercomputers (computers that are used to parallelize crafting) and the
Cluster Management Computer (computers that are used to manage computers that go in racks, like servers and
supercomputer nodes).

In this mod, items are data: there is no differentiation between what a fluid or an item is, they live on the same
disk as data, and the conversion rate of items to megabytes varies across eras. Disks sit in drives and servers and
they hold your data, be it items, folders, files or programs. Servers and supercomputer nodes are mounted in racks
of their respective type, and the mainframe is the brain behind everything: it does everything from moving items to
indexing the network.

Every computer needs a processor, memory, a system disk and something to show a screen on. Hardware comes
in eras, from the vintage machines with floppy drives to the current ones with USB sticks, and the era of
a computer decides which systems it runs and how fast it does things. Hardware matters to the computer's
performance, and it will affect how the computer does everything: a more powerful processor can execute more
instructions, move more items, and so on, and more RAM means more programs running simultaneously and a bigger
buffer for items, because in this mod there is a memory hierarchy too. Hardware matters the most in the mainframe:
the network speed and capacity to do things faster are determined by the mainframe's hardware, since everything
passes through it. So better processors mean more items that can be handled at once, more RAM means a bigger
buffer to accommodate large amounts of items entering into the network buffer, and more GPUs in the mainframe
mean, besides more monitors, another processing line, since the mainframe itself needs GPUs to parallelize how
many things may happen at once. Speaking of things, this mod also handles things not in batches but in a very
different system, Operations, more on that below.

In order to work, every computer needs an operating system. Operating systems are installed from media. The Frames
desktops (95, XP and 11) and a handful of Linux distros each ship their own programs: a file explorer,
a text editor, a terminal, settings, a system monitor, and the programs that talk to the network. Programs you
install on top come on their own installation media, which need the right media drive; they range from floppy
disks to USB flash drives.

Programs can also be installed through the Mirror, which is a program installed into the network's mainframe. With
this program installed, players may use their computer's package manager to install programs without needing the
installation media.

Everything the network does is an Operation: select, insert, move, craft. The Operations system is conceptually an
SQL-like system that is easy to understand and to abstract from: each operation works like a command in an SQL
database. Operations can also be used with the mod's own query language, IQL, which stands for Item Query Language,
a dialect of SQL created specifically for this mod. So, besides the classic SELECT, INSERT, WHERE, ORDER BY, DELETE
and so on, you also have some specifics, like CRAFT, which starts a crafting operation; IQL is defined and explained
in the mod's own documentation. Operations take time that depends on the hardware doing them, and the mainframe's
task manager shows the queue while the network works through it. The command prompt speaks IQL, a small query
language that compiles to the same Operations the graphical programs use. Operations are logged, so they can be
checked to see if they have failed or if they are running, so nothing is hidden from the player: every single thing
that happens in the network can be checked.

Crafting is machine work. A Crafting Computer runs recipes from its Recipe ROM; recipes are authored on the
Pattern Studio, burned onto media by a Pattern Encoder, and loaded through the Crafting Manager. A
crafting cable, a switch and buses connect the computer to the machines that do the processing, other mods'
machines included, and a request for an item plans every step, from raw stock to the finished product,
across as many machines as the network has.

## What is in the box today

- Data cables, routers, a mainframe multiblock, servers and racks per era, a supercomputer rack, and the
  Cluster Management Computer that installs and monitors racked machines.
- Personal computers, crafting computers, monitors and their peripheral cables; drives for floppies, CDs,
  DVDs and USB sticks; a dock station.
- The Frames 95, XP and 11 desktops and five Linux distributions with three desktop environments, with a
  boot manager, dual boot and package managers.
- The Network Interactor for storage and crafting requests, the Network Manager for the mainframe, the
  Crafting Manager, the Craft Planner, Storage Insights, the Automation Manager, and a few small programs.
- Machine autocrafting with multi-stage recipes, parallel stages and crafting-card threads; fluids and
  chemicals travel through the network like items.
- Σ#, the computers' own programming language, with five editors to write it in: programs that run
  at the prompt or stay up, threads, programs starting programs on the same machine or on another one of
  the network, folders shared between machines, and the network's query language from inside a
  program. The reference is in [docs/SIGMA.md](../docs/SIGMA.md).
- The Network Gateway, a peripheral that puts the data network within reach of ComputerCraft's
  computers when CC: Tweaked is present: our cable on its back, CC's on its front, an item buffer between
  them, and the Gateway Manager on the host computer (or the `gateway` command) to name it, set what the
  other side may do and read its log.
- JEI support: the ingredient list sits beside every monitor screen and recipes transfer straight into the
  Pattern Studio. Mekanism machines can be driven through the network when Mekanism is present.

Recipes for the computing blocks are missing on purpose: they arrive with the industrial chains that
make their parts. Play it in creative for now.

## Requirements

- Minecraft 1.21.1 and NeoForge 21.1.248 or newer.
- [J's Core](../core/README.md) at the same version (required).
- [GeckoLib](https://github.com/bernie-g/geckolib) 4.7 or newer (required).
- [JEI](https://github.com/mezz/JustEnoughItems) (optional, for recipe lookup beside the monitors).
- [Mekanism](https://github.com/mekanism/Mekanism) (optional, its machines and chemicals join the network).
- [CC: Tweaked](https://tweaked.cc) 1.120 or newer (optional, for the Network Gateway; without it the
  block links and holds items but its ComputerCraft side never comes up).

It does not need [J's Industrial](../industrial/README.md): the network drives any machine that exposes
the usual item and energy capabilities, whichever mod it comes from.
