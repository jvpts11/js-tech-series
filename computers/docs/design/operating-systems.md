# Operating systems

The operating system is the layer under the programs. Programs are optional ([Programs](programs.md)); what a computer
needs is **some** system. Without one, a computer falls back to its firmware: it shows the hardware and the boot order,
and installs a system, and nothing more. The Mainframe only orchestrates the network with a system booted: without one,
it keeps the network and the topology but doesn't dispatch, and the Operations asked for stay PENDING until there is a
system, when the dispatch picks them up on its own.

## What the systems are meant to be

- **Not Lua.** Talking to the network is IQL (declarative, [Operations](operations.md)); programs are written in Σ#
  ([Σ and Σ#](sigma.md)). IQL is not the programs' language.
- **Nobody is locked out.** A system makes things possible; it never forces anyone to run Debian from the command line.
  Complexity is optional and rewards whoever wants it: there is always a guided way, and doing it by hand is a choice.
- **Real systems, not skins.** Every system is the parody of a real one, made by a software house of the mod's world
  ([Programs](programs.md)), and behaves like the one it stands for: its firmware, its boot, its installer, its shell,
  its commands, its desktop, its help.
- **As rich as the mod**, not the fantasy of a simple old computer.

## Three layers

- **Firmware:** the state with no system. It shows the hardware, sets the boot order, installs and formats. It doesn't
  run the network.
- **System:** the platform. It lives on the **disk** (not on the monitor) and takes room there.
- **Programs:** they run on the system.

## Firmware and booting

| Era | Firmware |
| --- | --- |
| Vintage | text BIOS |
| Legacy | blue BIOS |
| Transition | blue BIOS |
| Standard | UEFI |
| Advanced | UEFI |

- A real **POST** when the computer is turned on and at every restart: counting the memory and finding the drives; DEL
  during the POST enters the setup.
- A saved boot order, with entries per disk and per medium; installing to a disk; **formatting** a disk (F, with a
  double confirmation: it erases the system, the files, the programs and the services).
- With two systems on different disks, the computer stops at a **boot manager** at every boot: the Midsoft Boot Manager,
  GRUB on Linux, FreeBSD's loader (the `show_boot_menu` setting lets it boot straight through).
- Every system's boot shows its own (System V's and FreeBSD's lines, Frames' screen).
- A computer that is off is a monitor with no signal; READY gives access to nothing. `reboot` runs the sequence again on
  the same monitor; `reboot --firmware` enters the setup.
- The Server Router and the switches are firmware appliances: they never install a system.

## Kernels

A system sits on a **kernel**, which decides how time is shared, how files are kept and which family of shell it speaks.

| Kernel | Scheduler | Files | Shell | Systems |
| --- | --- | --- | --- | --- |
| `dos` | none | hierarchical | DOS | MC-DOS |
| `win9x` | cooperative | hierarchical | DOS | Frames 95 |
| `nt` | preemptive | hierarchical | DOS | Frames XP to 11 |
| `net_min` | none | flat | network | MC-NET |
| `linux` | preemptive | hierarchical | POSIX | the distributions |
| `freebsd` | preemptive | hierarchical | POSIX | FreeBSD |
| `unix` | preemptive | hierarchical | POSIX | UNIX System V |

MC-NET's kernel keeps files without folders: a network appliance has to keep the pattern that teaches the network a
recipe and the file that starts on its own, but it is not a personal computer with folders. FreeBSD's kernel reads the
same as Linux's at a prompt, and that is all they share: nothing made for one runs on the other.

## The systems

| System | First era | Kernel | Boots to | Disk | Memory | Install | Packages |
| --- | --- | --- | --- | ---: | ---: | --- | --- |
| MC-DOS | Vintage | `dos` | terminal | 4 | 1 | guided | media |
| MC-NET | Vintage | `net_min` | Interactor | 8 | 2 | guided | media |
| UNIX System V | Vintage | `unix` | terminal | 10 | 2 | guided | media |
| Frames 95 | Legacy | `win9x` | desktop | 48 | 16 | guided | `pckmgr` |
| Frames XP | Legacy | `nt` | desktop | 1,536 | 64 | guided | `pckmgr` |
| FreeBSD | Legacy | `freebsd` | terminal | 2,048 | 16 | guided | `pkg` |
| Debian | Legacy | `linux` | terminal | 4,096 | 24 | guided | `apt` |
| Ubuntu | Legacy | `linux` | terminal | 8,192 | 48 | guided | `apt` |
| Fedora | Legacy | `linux` | terminal | 8,192 | 48 | guided | `dnf` |
| Arch Linux | Legacy | `linux` | terminal | 2,048 | 12 | by hand | `pacman` |
| Gentoo | Legacy | `linux` | terminal | 4,096 | 12 | compiled | `emerge` |
| Frames 7 | Transition | `nt` | desktop | 16,384 | 384 | guided | `pckmgr` |
| Frames 10 | Standard | `nt` | desktop | 16,384 | 512 | guided | `pckmgr` |
| Frames 11 | Advanced | `nt` | desktop | 20,480 | 768 | guided | `pckmgr` |

Disk and memory are in MB, as the box would print them; they are estimates. What a system costs in items depends on the
disk it is on ([Storage devices](storage-devices.md)): MC-DOS fills a fifth of a 20 MB Vintage disk; Frames 11 takes 80
items of a 64-bit disk and doesn't fit on a Vintage one. The shells: `cmd` in the DOS family, `bash` on the
distributions, `zsh` on Arch, `sh` on FreeBSD and UNIX.

**The era is a minimum:** a system runs on hardware of its era or newer, never older; weak hardware is never left
without a system (there is always MC-DOS, MC-NET and UNIX). UNIX System V is the only system of the first era that runs
several programs at once, in 10 MB of disk and 2 of memory; the price is installing only from media.

**What a system can show:**

| Capability | What the system does |
| --- | --- |
| `TERMINAL_ONLY` | a full-screen prompt (MC-DOS; Linux, FreeBSD and UNIX until a desktop is installed) |
| `NETWORK_GUI` | a full-screen operating space (MC-NET, below) |
| `FULL_DESKTOP` | windows, a panel and a menu (Frames; Linux, FreeBSD and UNIX with a desktop) |

What can be installed and run on each machine (system, machine, Frames version, era, disk) is in
[Programs](programs.md).

## Installing

1. The system's medium in a drive linked to the computer ([Programs](programs.md)).
2. The monitor shows the firmware; choose the drive and boot from it.
3. The installer starts, in its system's style (every system has its own).
4. Follow it: most ask for the disk and a confirmation.

The installer puts the system on the first disk that has none, unless another is chosen. A second system on another disk
gives a dual boot (above).

There are three ways:

- **Guided:** the system's installer, with its pages.
- **By hand** (Arch Linux): the live system's root shell, where the player types the real steps of the guide
  (partitioning, `mkfs`, `mount`, `pacstrap`, `genfstab`, `chroot`, the boot loader, the password).
- **Compiled** (Gentoo): stage 3, `emerge --sync` and compiling the kernel, a job of many ticks scaled by the processor.

By default, Arch and Gentoo only ask for the steps without which the system won't boot; the server's settings can make
them ask for every step of the guide.

## Files, shells and the terminal

- **Files:** the DOS family has drives and folders (`C:\`), and Frames the whole tree of a Windows; the POSIX systems
  have a single tree with a root (`/`); MC-NET, a flat store. What is written weighs on the disk by its era
  ([Storage devices](storage-devices.md)).
- **Network shares:** `config share C:\pub`; other computers reach it at `\\host\pub` (or `/net/host/pub` on Linux), in
  Files and in programs.
- The **shell** speaks its family's language: the DOS verbs or the POSIX commands, and every system has its own commands
  (the package managers, `uname`, `df`, `mkfs` or `format` with the system disk protected, `screenfetch`).
- The **terminal:** Tab completion (commands and devices), history kept **per computer**, line wrapping, the extended
  colour palette.
- **Manual pages:** `help` on DOS, `man` on UNIX and FreeBSD, `info` on Linux (below).

## MC-NET and operating spaces

MC-NET has no desktop: it draws an **operating space**, a full-screen program in place of the prompt. What it brings is
the **Interactor** (Nouvell Networks), the Network Interactor full screen, with the machine's rail of headers
([Interface](interface.md)). The Interactor is a package like the others: taken out, the machine is a prompt and nothing
more; put back, it comes back. An add-on can register another operating space, a whole other way of working the network
([The API](api.md)). MC-NET doesn't open the Command Prompt in a window: the prompt is a header inside the space.

## Desktops

- Every Frames brings its desktop. On Linux, FreeBSD and UNIX, a desktop is a **package** ([Programs](programs.md)): KDE
  Plasma, GNOME and CDE from the Legacy, Cinnamon from the Standard (CDE runs on UNIX, FreeBSD and Linux; the others on
  Linux and FreeBSD). Every desktop has its era's face: the Transition's KDE is the KDE of that time.
- They all have the same programs, with the names their system gives them: Files is Dolphin on KDE and the File Manager
  on CDE; Frames adds the Device Manager, and CDE the Workstation Info.
- **Open windows are the machine's state:** the list (program, position, size, order) lives on the server. Closing the
  monitor, changing monitor or leaving the game closes nothing; turning off, restarting or formatting clears it. Each
  program's inside asks the server for its state when it opens again.
- **The inventory band:** a program can ask for the player's real inventory under its window (the Network Interactor,
  the Pattern Studio); the slots work like a chest's.
- JEI sits beside the monitor on every monitor screen, and the frame is an exclusion area.
- Desktop effects drawn on the graphics card, and the Experience Index, are in [Hardware](hardware.md). The cursors are
  the system's, at the size of the real ones.
- **Settings:** personalising (wallpaper, accent colour, light or dark theme, clock, taskbar), system, network (shares,
  remote programs), disks, display, programs (uninstall), sound (volume and system sounds) and users, each desktop in
  its own form (Frames 7's Control Panel by category, the Settings of Frames 10 and 11, CDE's Style Manager).
- **Device Manager** (Frames): the machine's hardware and each port it has, what is linked to each, what is free, and a
  device turned off or on again.
- Each desktop's **Task Manager**: [Interface](interface.md).

## Help

Every computer reads the series' manuals (the Technical Reference, the Guide to Operations and the Plant Drawings, the
same entries the books print, in the game's language, [Manuals](manuals.md)) beside the manual pages of its commands, in
its system's form:

| System | Help |
| --- | --- |
| Frames 95 | Help Topics: Contents, Index and Find |
| Frames XP | the Help and Support Center |
| Frames 7 | Help and Support, with Search Help and Browse Help |
| Frames 10 and 11 | Get Help: a search, the books and the page |
| KDE Plasma | the Help Center |
| GNOME, Cinnamon | Help, with no tree: each page lists what it has |
| CDE | the Help Viewer |
| MC-DOS, MC-NET | `help`, full screen in the sixteen colours |
| Linux (shell) | `info`, with nodes and menus |
| UNIX, FreeBSD | `man <topic>`, like a page of section 7 |

A topic is called by the last part of its id or by its title; the search finds the entries with all the words, and the
commands that have them in their name or summary.

## Special cases

- **The Mainframe needs a system** to orchestrate (above). The smallest systems (MC-DOS, MC-NET) are cheap and quick to
  install: a ritual, not a wall. The network engine is a program of that system ([Operations](operations.md)).
- **Rack servers** install a real system on the disk in their bay (MC-NET, any Linux, FreeBSD or a Frames), every bay
  with its own POST, boot manager and installer; they are reached through a monitor (with a KVM Switch on a rack with
  several machines) or through the Cluster Management Computer ([Servers and racks](servers-and-racks.md)).
- **Appliances** (the Server Router, the switches) only have firmware.
- The registries of kernels, systems, desktops and operating spaces are open to add-ons ([The API](api.md)).

## To build

- **Accounts and passwords:** every system has real user accounts and its `passwd`; with a directory on the network, the
  login is the network's ([Security](security.md)). Today they only exist as pages.
- **The portable devices' mobile systems** ([Wireless](wireless.md)): real systems, from the factory, with no installer,
  parodies of the real ones, with the registry's apps and the Mirror's store. With them comes a **third family of
  systems and hardware**, next to Frames and Linux: a maker and software house in the manner of Apple, with its
  computers (like the Macintosh), its phones and tablets, and its systems, desktop and mobile. The brand's name and the
  names of its systems and machines are set when we get there; the instruction sets they bring (as the real ones did:
  68k, PowerPC, ARM) register like any other ([Σ and Σ#](sigma.md)).
- **The web of the world:** **sites** are files (pages, images) on a rack server running a **web server** service (a
  parody of a real one, like the Messenger Service); the **browser** is a program per era, a parody of the real ones
  (Netscape and Internet Explorer in the Legacy, Firefox and Chrome later), and shows the network's sites. Through the
  WAN Gateway Computer's federations ([The network](network.md)), other networks' sites come within reach: an "internet"
  of the server, between players. Later, a search engine, shops tied to Teracoin ([Teracoin](teracoin.md)) and pages
  with forms that run Σ# on the server. The whole design (what language pages are written in, what each era's browser
  can draw) is made in a cycle of its own.
