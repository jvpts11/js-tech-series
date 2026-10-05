# Getting started

Your first computer, and then your first network, for a player who has never used J's Computers. Every block
and part here is in the creative menu: the computers have no crafting recipes yet, on purpose, because they will
be made from the parts the industrial chains produce.

## The idea in one paragraph

In J's Computers you build real computers: a case, a motherboard, a processor, memory, a disk, a graphics card,
a monitor. You install an operating system on them from a disc, and use their programs. Join computers with
cables around one big computer, the **Mainframe**, and you have a **network**: it stores your items as data on
its disks, moves them where you ask, and crafts for you. Hardware comes in **eras**, from Vintage (floppy disks,
green screens) to Advanced (today's machines), and a newer era is faster at everything.

## Your first computer

*Added 2026-06-05.*

1. Open the creative tab **J's Computers - Standard** and take a **Personal Computer**, a **Monitor**, a
   **Peripheral Cable** (Standard), a **Dock Station** and the parts below.
2. Place the computer and use it. Its window is the inside of the case: slots for each part.
3. Put in a **motherboard** of the Standard era first (each era's computer takes only its own era's board),
   then a **processor** (CPU) that fits the board's socket (the board's tooltip names its socket, and so does the
   CPU's), at least one **memory** module (RAM) of a kind the board takes, a **graphics card** (GPU), a **disk**
   and a **power supply** (PSU). The window says what is missing or does not fit.
4. Place the **Monitor** and join it to the computer: put it right against the computer, or lay Peripheral Cable
   from the monitor's back to the computer. Do the same with the Dock Station.
5. Turn the computer on and use the **monitor**. Everything you do with a computer's programs, you do at its
   monitor; using the computer block itself always opens the case.
6. With no system on its disk, the monitor shows the computer's firmware (its BIOS). Put the **Frames 10**
   install USB stick (in the same creative tab, among the programs) in the Dock Station and choose to install
   from it. Follow the installer.
7. The computer starts into its desktop. Open **This PC**, **Files**, the **Command Prompt**, the **Settings**:
   it is a computer.

For older eras the steps are the same with that era's parts and media: Vintage systems come on floppy disks, read
by a **Floppy Drive**; Legacy on CDs, read by a **CD Drive**; Transition on DVDs, read by a **DVD Drive**.
[Hardware](HARDWARE.md) explains every part, and [Systems and programs](SYSTEMS.md) every system.

## Your first network

*Added 2026-06-05.*

1. Build a **Mainframe**. It is a block of 3 wide, 2 tall and 2 deep: place its parts in that shape and it forms.
   It takes a motherboard of its own kind (the MTX board of its era), a power supply, up to 4 processors, 8 memory
   modules, 6 graphics cards and 4 disks. It is the network's brain: how fast it is decides how fast the whole
   network works.
2. Build a **Server Rack** (2 wide, 3 tall, 2 deep) and put **Servers** with disks in its bays. The servers' disks
   are where the network keeps your items.
3. Join them with the network's cables: the Mainframe and the racks on the **backbone** line (HBW cable in the
   Legacy, Fibre Optic in the Standard), and the small computers on the **access** line (Ethernet) through a
   **Router**. [Cables](CABLES.md) explains each line, its speed and its range.
4. At a computer on the network, open **Network** (the Network Interactor). Put items into it and they are stored
   on the servers' disks; ask for them back and they come out.

Every request the network carries out (store this, give me that, craft this) is an **Operation**, and you can
watch the queue in the Mainframe's **Task Manager**. You can also write requests yourself in **IQL**, the
network's query language: `SELECT 64 iron_ingot`. [The network](NETWORK.md) explains it all.

## Where to go next

| Page | What it covers |
| --- | --- |
| [Hardware](HARDWARE.md) | The eras, every computer, every part and what the numbers on their tooltips mean. |
| [Systems and programs](SYSTEMS.md) | Every operating system and desktop, installing, dual boot, the programs, the Mirror. |
| [The network](NETWORK.md) | The Mainframe, storage, Operations, IQL, the network's engines and programs. |
| [Autocrafting](AUTOCRAFTING.md) | Crafting Computers, patterns, machines and plans of many steps. |
| [Cables](CABLES.md) | Every cable, its speed and range, routers, buses, the crafting network, peripheral cables. |
| [Σ#](SIGMA.md) | The computers' programming language. |
| [ComputerCraft](COMPUTERCRAFT.md) | Working together with CC: Tweaked's computers. |
| [Music on a server](SOUNDFOUNDRY.md) | For server owners: the music catalogue. |

## What can go wrong

- **Using the computer opens the case, not the system.** That is how it is: the system is at the monitor. Use the
  monitor.
- **The monitor says "No computer found in range over a Peripheral Cable".** It is not joined to the computer.
  Put it against the computer, or lay Peripheral Cable all the way, within the cable's range (16 blocks in the
  Standard).
- **The monitor says "The computer has no video output: install a graphics card".** Put a GPU in the computer,
  or use a CPU with graphics built in (its tooltip says so).
- **The monitor says "Not enough video memory".** Every monitor uses part of the graphics card's video memory,
  bigger monitors more. Use a card with more memory, or fewer or smaller monitors.
- **The window refuses the build.** It says why: a CPU that does not fit the socket, memory the board does not
  take, more parts than the board has slots for, a part from another era, or more power than the power supply
  gives.
- **The installer refuses the system.** A system needs hardware of its own era or newer: Frames 10 will not go on
  a Legacy computer.
