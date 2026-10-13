# Systems written by players

A player can write an operating system of their own, and a desktop environment of their own, in Σ or Σ#, as they would
write any program, and boot it on any computer. Nothing of it exists yet.

## To build

### The idea

Writing a system is writing a large program, not simulating hardware: no memory tables, no interrupt handlers, no weeks
of low-level work. A new namespace of the standard library, `System.OS`, gives a system what it needs to declare how it
works and to run, and the mod does the heavy part in its own code (drawing, files, windows), so a system written in Σ#
stays fast enough on a weak machine. The mod's own systems are not rewritten: they keep working as they do.

### The namespace `System.OS`

A system is a class that implements `ISystem`: `OnBoot`, `OnTick`, `OnInput` and `OnShutdown`. It declares its name,
its version, its author, the era and instruction set it targets, and the memory it keeps for itself, as the mod's own
systems do ([Operating systems](operating-systems.md)). In Σ, the same calls are functions of `Standard`
([Σ and Σ#](sigma.md)).

| Type | What it gives | Its members |
| --- | --- | --- |
| `ISystem` | the system itself | `Name`, `Version`, `Author`, `Era`, `Isa`, `MemoryMb`, `Kernel`; `OnBoot()`, `OnTick()`, `OnInput(e)`, `OnShutdown()` |
| `Kernel` | the kernel's policies, below | `Kernel.Of("linux")`, or `Kernel.Own()` with `.Scheduler(kind, slice)`, `.FileSystem(kind)`, `.Drivers(classes)`, `.MemoryRule(rule)`, `.Shell(family or its own)`; the events `OnSchedule` and `OnOutOfMemory` |
| `Console` | the monitor's text screen | `Write`, `WriteLine`, `Clear`, `Cursor(x, y)`, `Colours(fore, back)` in the era's palette, `Size` (the columns and rows of the monitor's text mode) |
| `Input` | the keyboard and the mouse, as they come from the monitor | events for a key, a character, the mouse moving and a click; `Focus(process)` chooses who gets the input |
| `Processes` | Σ and Σ# programs, their priorities and signals | `Start(path, args)`, `Stop(pid, signal)`, `List()`, `SetPriority(pid, level)`, `Yield()`, `Current` |
| `Disk` | the file system of the disk the system is on | open, read, write, delete, list, move, make folders; `Free`, `Size`; `Mount` for the other disks and media |
| `Memory` | what is held and what is free | `Total`, `Free`, `HeldBy(pid)`, `Ledger()` (the Task Manager's list) |
| `Services` | programs run as services, up while the system is | `Register(name, path)`, `Start`, `Stop`, `Status`; the registered ones come up at boot, in order |
| `Drivers` | the devices the machine has, and which of them the system drives | `Devices()` (each with its class), `Has(class)` |
| `Boot` | what is shown while the system comes up | `Line(text)`, `Logo(image)`, `Progress(percent)`, `Done()` |
| `Panic` | brings the machine down on a crash page with the system's own message | `Halt(message)`; the page: one of the mod's (the blue one, the black text of a kernel panic, the red one) or the system's own colours; `RebootAfter(seconds)` |
| `Desktop` | the desktop the system starts, below | `Start(package)`, `Stop()`, `Current` |

The windows of a graphical system are `System.UI`'s, which every program already uses.

### The kernel

The mod describes each of its kernels by a scheduler, a file system and a shell family. A player system uses the same
model:

- **One of the mod's kernels or its own.** A system can stand on one of the mod's kernels (`dos`, `win9x`, `nt`,
  `linux`, `freebsd`, `unix`, `net_min`) and take all its behaviour, or declare its own kernel by choosing each policy
  below and answering the events it wants. Its own kernel is a set of choices and answers, never low-level code.
- **The scheduler** hands out the machine's instruction credits ([Σ and Σ#](sigma.md)):
  - **none:** only the program in front gets credits, and the others wait, as on DOS;
  - **cooperative:** a process runs until it calls `Yield()`, waits or ends; a program that never yields freezes the
    machine, as on Frames 3.1, and never the server, which the clock protects;
  - **preemptive:** in slices of 16 to 256 instructions (64 by default, the round the mod's systems use), with four
    priorities: high (two slices a round), normal, low (it skips one round in two, as today) and idle (it only runs
    when every other process waits).

  A system may answer `OnSchedule`, which gets the processes ready to run and returns the next, once a slice; what it
  spends comes from process 1's credits. Without it, the mod schedules as the chosen kind does.
- **The file system:** one of the mod's formats, of the DOS kind (drive letters, names in any case), the POSIX kind
  (one tree from `/`, permissions) or flat. Other systems read its disks when they know the format.
- **The drivers:** the system says which classes of device it drives: the text screen, the graphics screen, sound,
  the network, disks, optical drives, USB, the printer. The drivers are the mod's: a system chooses them, never writes
  them. A device with no driver does not exist for that system: one with no sound driver plays nothing.
- **The memory rule:** refuse, end what holds the most, or go down, as the mod's systems do
  ([Operating systems](operating-systems.md)); or answer `OnOutOfMemory`, which gets who asked and how much, and returns
  one of three answers: refuse; end a process (asked again if that frees too little, at most once per process, then it
  refuses); or go down, through `Panic`. A fault in the handler itself brings the machine down.
- **The shell:** one of the mod's families (DOS, POSIX, MC-NET), with its commands, or one of the system's own,
  written in Σ#. Commands the system adds are programs.

### Running

- The system is the machine's process 1 and spends the machine's instruction credits, as any program does; the work
  the mod does for it (drawing, files, windows) costs it nothing more. The programs it starts are its children.
- **Memory:** the system's process may hold up to a quarter of the machine's memory (a program, 64 MB, as today), and
  the `MemoryMb` it declares goes into the memory ledger as the mod's systems' does.
- **A fault:** an exception the system's code doesn't catch is a panic, with the exception and its line, on the
  system's panic page; the page stays until the player restarts the machine or `RebootAfter` does. It never reaches
  the game's server.
- **What it runs, the trade between its own and compatible:** a system on one of the mod's kernels counts as that
  kernel's family for the catalogue, so a system on `linux` installs what Linux installs, the mod's services and a
  Mainframe's network engines included ([Programs](programs.md)). A system with a kernel of its own runs only Σ and Σ#
  programs and services, and the mod's desktop packages: a Mainframe with a player system runs the network engine only
  when that system stands on one of the mod's kernels. The programs built into the mod's systems (the Calculator, Paint
  and the rest) belong to those systems.
- **What it may reach:** the disks and devices of its own machine, and the network only through the calls every
  program has. Never another machine's disk, never the game.

### The desktop

- **A built-in desktop:** a player system can start one of the mod's desktop packages (KDE Plasma, GNOME, Cinnamon,
  CDE) with its own programs, as a Linux distribution does.
- **A desktop of the player's own,** made with the skins framework of the mod's desktops: the desktop declares its
  look (its palette, its panel, its Start menu, the style of its window frames, its icons and wallpaper from image files
  on the disk) and brings its own programs in Σ#. Moving windows and the focus stay in the mod's code, on the player's
  side, so a player's desktop is as smooth as the mod's. A player's desktop is a package, installed on the player's
  systems and on the mod's systems that take desktop packages, as KDE Plasma is.

### Building and sharing

- `sigma build --os` writes a system onto an install medium of its era (a floppy, a CD or a USB stick, by its size);
  `sigma build --desktop` makes a desktop package.
- A player system installs as any other: from the medium through the firmware, with the Preinstaller
  ([Survival](survival.md)), or from the Mirror through the firmware's network boot
  ([Operating systems](operating-systems.md)). The boot manager lists it by its name.
- Systems and desktops are published to the Mirror as packages, and any machine of the network installs them from there
  ([Programs](programs.md)); media copy them too.
- **Trust:** a package shows its author on the Mirror and on its medium, and only a machine's owner installs a system
  on it. Signatures and real trust come with security ([Security](security.md)).
