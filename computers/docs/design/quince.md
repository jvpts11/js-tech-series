# Quince

The third family of hardware and systems, next to Midsoft's Frames and Linux: **Quince**, a maker and software house in
the manner of Apple (the quince is the apple's cousin), with its own computers, servers, Mainframe, phones and tablets,
and its own systems, desktop and mobile. It follows the real history of its model, as parody. Nothing of it exists yet;
it is decided for Carbon.

## To build

### The machines

| Era | Computer | Processor (instruction set) | System | Firmware |
| --- | --- | --- | --- | --- |
| Vintage | **Russet 128K**, all-in-one with its own screen | Motoroller 68000 (68k) | Quince System | a ROM, with the start-up chime and the Happy Quince |
| Legacy | **qRusset G3**, the translucent all-in-one, and the **Russet Power G4** tower | PowerQ G3 and G4 (PowerPC) | Quince OS 9, then Quince OS X at the end of the era | Open Firmware, with its `ok` prompt |
| Transition | **Russet Pro**, the aluminium tower, and the white qRusset | Integra (x86-64), the real switch of 2006 | Quince OS X | EFI, with the Startup Manager |
| Standard | the thin qRusset and the Russet Pro | Integra (x86-64) | Quince OS X | EFI |
| Advanced | **Russet Studio** and the coloured qRusset | Quince Q1, Q2 and Q3 (ARM64, memory unified with graphics) | quinceOS | the Quince chip's own start |

- **The all-in-ones** (the Russet 128K and every qRusset) carry their screen in their own block: they need no monitor,
  and their face shows the screen as a monitor's does ([Peripherals](peripherals.md)). They and the Russet Studio take
  cables only from behind: every line plugged in shows its socket where it meets the back face, and its cord into
  the machine.
- **Servers:** the **qServe**, a 1U rack server per era from the Legacy to the Advanced, in the ordinary racks, as the
  real one was a 19" unit ([Servers and racks](servers-and-racks.md)).
- **The Quince Mainframe**, from the Transition on, a Mainframe of its own running Quince OS X Server
  ([Computers](computers.md)). Quince had no large machines before.
- **The phones and tablets:** the qPhone and the qPad, with qOS ([Wireless](wireless.md)).

### Closed hardware

A Quince machine takes a Quince logic board, and a Quince logic board takes only the processors its era gives it
(Motoroller, PowerQ, the Integra ones of the Transition and the Standard, and Quince's own); it never goes into another
maker's case, and no other board goes into a Quince case. There are sixteen logic boards: one for each machine, one
for the qServe of each era and one for the Quince Mainframe of each era. Memory modules, disks and graphics cards are
the common ones; the all-in-ones draw with the graphics on their board. Quince's own chips carry memory in their
package, unified with the graphics, and the Advanced logic boards keep the common memory slots besides.

Powering a Quince machine on while sneaking opens its firmware's start-up choices, as holding the key did: the
Startup Manager from the Transition on (its options on the Advanced), Open Firmware's `ok` prompt on the Legacy.

### Instruction sets

| ISA | Word | Runs programs built for | Processors of |
| --- | --- | --- | --- |
| 68k | 32 bits | 68k | Quince, Vintage |
| PowerPC | 32 bits | PowerPC | Quince, Legacy |
| ARM64 | 64 bits | ARM64 | Quince, Advanced |

The Integra years run x86-64, as every other computer of those eras. A Σ or Σ# program builds for one of these with
`sigma build --isa`, or as a **universal** build that carries several at once and runs on each
([Σ and Σ#](sigma.md)). Two translators carry programs across a switch, as Rosetta did: **Champollion** (named after the
man who read the Rosetta Stone) runs PowerPC programs on Quince OS X on Integra, at half their speed, and
**Champollion 2** runs x86-64 programs on quinceOS on Quince's own chips, at 80%.

### The systems

| System | First era | Kernel | Boots to | Disk | Memory | Install | Packages |
| --- | --- | --- | --- | ---: | ---: | --- | --- |
| Quince System | Vintage | `quince_classic` | desktop | 1 | 1 | from its disks | media |
| Quince OS 9 | Legacy | `quince_classic` | desktop | 400 | 32 | guided | media |
| Quince OS X | Legacy | `wallace` | desktop | 2,048 | 128 | guided | `.pkg`; the Quince Store and `keg` from the Standard |
| Quince OS X Server | Transition | `wallace` | desktop | 4,096 | 256 | guided | as Quince OS X |
| quinceOS | Advanced | `wallace` | desktop | 16,384 | 4,096 | guided | as Quince OS X |

Disk and memory in MB, estimates, as for every system ([Operating systems](operating-systems.md)).

- **The kernels:** `quince_classic` is cooperative, with no command line at all, as the classic Mac was: it always
  boots to its desktop. `wallace` is preemptive and POSIX, with `zsh` (Wallace stands beside Darwin, the real kernel's
  name).
- **Packages:** the classic systems install only from media: the Quince System from three floppy disks through the
  slot, the installer asking for the next and ejecting the last, Quince OS 9 guided from its CD. Quince OS X installs
  `.pkg` packages and programs dragged into Applications; from the Standard, the **Quince Store** (served by the
  Mirror) and `keg`, after Homebrew. Everything in the Quince Store is free, and it lists the mod's own programs;
  `keg` installs only from the Mirror (`mirror://mainframe`). The phones' qOS installs only from the Quince Store.
- **The desktop:** Quince's skin, with the menu bar at the top, the **Seeker** (after the Finder) and, from Quince OS X,
  the Dock. Each system has its look: the Quince System in black and white, Quince OS 9 in Platinum, Quince OS X in
  **Pulp** (after Aqua) in the Legacy and the Transition, then flat in the Standard, and quinceOS's own in the
  Advanced. Unlike the rest of the mod, these looks keep the rounded corners and the gel buttons of the systems they
  parody. The Dock holds the system's programs, the Quince Store from the Standard, and the Trash at its end. Emptying
  the Trash asks first only on Quince OS 9.
- **Its programs:** the Seeker, TextPad, Preview, **qTunes** (the music player), **Expedition** (the browser, which
  waits for the web of the world), the Terminal (from Quince OS X), System Preferences and the **QuinceTime Player**,
  Quince's name for the Media Player ([Programs](programs.md)).
- **System Preferences** has the panels of its real one, with Memory and Mirror among them. quinceOS starts into its
  **recovery** from the start-up choices: Startup Disk, Reinstall quinceOS and Disk Utility. **Quince OS X Server**
  runs its services from **Server Admin**: the Mainframe's, which are the Mirror, the IQL Engine and the Automation
  Engine.

### Crashes and memory

- **The crash pages:** the bomb, *Sorry, a system error occurred*, on the Quince System and Quince OS 9, wherever the
  game brings a system down, its Restart losing whatever was open; the grey page in English, French, German and
  Spanish, *You need to restart your computer*, on Quince OS X and quinceOS; the **Sad Quince** at start-up for a
  hardware fault, Quince's red page ([Operating systems](operating-systems.md)).
- **Memory, on the classic systems:** each program has a fixed memory partition, which its Get Info sets (one size on
  the Quince System; the suggested, minimum and preferred sizes on Quince OS 9), and a program that doesn't fit is
  refused: *There is not enough memory to open*, as the real ones did.
- **Memory, on Quince OS X and quinceOS:** the dialog *Your system has run out of application memory* pauses the
  programs and lets the player choose which to force to quit; nothing ends without the player, and the kernel panics
  only when nothing is left.

### File systems

Quince formats as its real one did ([Storage devices](storage-devices.md)): **HFS** on the Quince System, **HFS+** on
Quince OS 9 and Quince OS X, journaled from Quince OS X (back in seconds after a power loss), and **APFS** on
quinceOS, copy-on-write with snapshots, as btrfs and ZFS. Only Quince's systems read and write them. **Disk
Utility** is Quince's disk tool from Quince OS X on; Quince OS 9 has **Drive Setup**, and the Quince System formats
only at its install.
