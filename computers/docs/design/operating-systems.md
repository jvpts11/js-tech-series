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
| Frames 95 | Legacy | `win9x` | desktop | 48 | 32 | guided | `pckmgr` |
| Frames XP | Legacy | `nt` | desktop | 1,536 | 128 | guided | `pckmgr` |
| FreeBSD | Legacy | `freebsd` | terminal | 2,048 | 16 | guided | `pkg` |
| Debian | Legacy | `linux` | terminal | 4,096 | 24 | guided | `apt` |
| Ubuntu | Legacy | `linux` | terminal | 8,192 | 48 | guided | `apt` |
| Fedora | Legacy | `linux` | terminal | 8,192 | 48 | guided | `dnf` |
| Arch Linux | Legacy | `linux` | terminal | 2,048 | 12 | by hand | `pacman` |
| Gentoo | Legacy | `linux` | terminal | 4,096 | 12 | compiled | `emerge` |
| Frames 7 | Transition | `nt` | desktop | 16,384 | 1,024 | guided | `pckmgr` |
| Frames 10 | Standard | `nt` | desktop | 16,384 | 3,072 | guided | `pckmgr` |
| Frames 11 | Advanced | `nt` | desktop | 20,480 | 8,192 | guided | `pckmgr` |

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

## Memory and crashes

A program holds what it declares on a Legacy machine, a sixteenth of that on a Vintage one, 8 times on a Transition, 32
on a Standard and 128 on an Advanced, each era four times the last past that: the ratio between the typical machines'
memories, so the typical machine of each era (two modules of its middle size: 32 MB, 512 MB, 4 GB, 16 GB, 64 GB) holds
its system and about eight average programs, and a full board five to ten times that. A program that declares nothing
holds 48 MB on a Legacy machine, a tool the system ships 16, a service 24, a desktop 64. The numbers are estimates, to
tune in play.

When a program needs more than is free, each system answers as its real one did:

- **Frames** goes down, whatever asked: a window, a program started at the prompt, a service. Frames 95, with no memory
  protection, at once; XP, 7, 10 and 11 first warn with a balloon (*Low on memory*) once the memory passes nine tenths,
  naming the three programs that hold the most, and go down on the program that doesn't fit. Each edition has its own
  page: the fatal exception of 95, the STOP screen of XP and 7, the sad face of 10 and 11 with a real scannable code and
  the stop code (`MEMORY_MANAGEMENT`).
- **Linux, FreeBSD and UNIX** let the program in and their kernel's out-of-memory killer ends what holds the most (a
  window, a program, a service; of equals, the latest), until everything fits, each logged in the kernel's own words
  (`Out of memory: Killed process 1234 (calc)` on Linux, `pid 1234 (calc), jid 0, uid 0, was killed: out of swap space`
  on FreeBSD, `NOTICE: out of memory: process 1234 (calc) killed` on UNIX), printed on the console and kept for
  `dmesg`. A desktop closes the window that went with a balloon (*Out of memory*); a program ended in front of a terminal
  leaves the shell's `Killed`; a service ended stays down until the machine starts over (a Mainframe's network engine,
  Automation Engine and Mirror included). Only when nothing is left to end does the kernel panic.
- **MC-DOS and MC-NET** refuse the program and say why.

A crash is the machine's, not the screen's: the page is on every monitor showing the machine, nothing typed or clicked
reaches anything, everything the machine ran dies, and once the page has stood its time the machine starts over from its
self-test into an empty session (FreeBSD's countdown stops on a key, and the next key reboots). A **red page** is a fault
of the hardware: a memory module, an expansion card or the system's disk taken out of a running machine (the hot-swap
bays excepted, they are made for it); Linux and FreeBSD panic on a machine check and UNIX on a memory parity error. A
machine left whole starts over; one left with no computer goes dark. The processor, the board or the power supply taken
out switch it off at once, with no page.

`ps` on a Unix lists the system's own processes before the programs: its first process at number 1 (systemd on the
distributions that start it, init on Gentoo, FreeBSD and UNIX) and each running service at a number it keeps; the
programs are numbered from 1000, on every system. `kill` takes a signal (`-9`, `-KILL`, `-s KILL`; TERM when none); a
signal to a service stops it until the machine starts over, and one to process 1 ends init: the kernel panics
(*Attempted to kill init!* on Linux, *Going nowhere without my init!* on FreeBSD, *init died* on UNIX). On a real Linux
the kernel ignores a kill of init from the system; here it is what it would do if it didn't, by choice.

## Special cases

- **The Mainframe needs a system** to orchestrate (above). The smallest systems (MC-DOS, MC-NET) are cheap and quick to
  install: a ritual, not a wall. The network engine is a program of that system ([Operations](operations.md)).
- **Rack servers** install a real system on the disk in their bay (MC-NET, any Linux, FreeBSD or a Frames), every bay
  with its own POST, boot manager and installer; they are reached through a monitor (with a KVM Switch on a rack with
  several machines) or through the Cluster Management Computer ([Servers and racks](servers-and-racks.md)). Each
  follows its system's rule when it runs short, as every computer does, on its own: the crash pages, the out-of-memory
  killer, `ps`, `kill` and `dmesg`; a service ended on a server stops serving the network until that server starts
  over. Its drive bays are hot-swap, so only a crash of its system brings a server down onto a page.
- **Appliances** (the Server Router, the switches) only have firmware.
- The registries of kernels, systems, desktops and operating spaces are open to add-ons ([The API](api.md)).

## To build

- **More triggers of the red page,** on every computer and rack server: a thermal trip, a failing power supply, unstable
  power ([Temperature](temperature.md), [Power](power.md)).
- **The firmware lives on the motherboard.** A board made in survival comes out with no firmware, and a computer with a
  blank board shows nothing; the Preinstaller writes the firmware of the board's era onto it ([Survival](survival.md)).
- **Network boot from the Mirror.** From the Legacy, the firmware's setup can boot from the network (PXE); from the
  Standard, UEFI boots over HTTP; the Vintage cannot. On a network whose Mainframe serves the Mirror, the firmware lists
  the systems published there, the players' own included, and boots the installer of the one chosen; the install then
  runs as from a medium. Today a system is only installed from the Mirror from inside a running one
  ([Programs](programs.md)).
- **Systems and desktops written by players**, in Σ or Σ#, through the `System.OS` namespace
  ([Systems written by players](player-systems.md)).
- **OpenBSD,** decided for Carbon: the proactively secure Unix, from the Legacy, on a kernel of its own (`openbsd`),
  booting to the terminal, 1,024 MB of disk and 12 of memory, packages with `pkg_add` and the ports.
  - Its installer is the real one, questions and answers at the terminal: *(I)nstall, (U)pgrade, (A)utoinstall or
    (S)hell?*, the host name, the network, root's password, sshd, X, the disk and the sets (`bsd`, `base`, `comp`,
    `man`...), from the medium or from the Mirror over the network.
  - It boots through `boot>`, waiting five seconds, with its own lines (*Automatic boot in progress: starting file
    system checks*, *starting network daemons*), greets with *Welcome to OpenBSD: The proactively secure Unix-like
    operating system* and runs `ksh`.
  - Secure by default: a service installed comes switched off and is switched on with `rcctl enable`; it has `doas`
    and `syspatch`. The player stays root until accounts come ([Security](security.md)).
  - Its panic stops in the kernel debugger, at `ddb>`: the machine waits until the player types `boot reboot`, with
    `trace`, `ps` and `show panic` there too.
  - Its memory follows the Unix family's rule, with OpenBSD's own line; it takes the desktop packages FreeBSD takes.
  - With security, it is the system of the network's defence: `pf` is the network's firewall, OpenBSD machines are
    the hardest to attack, and `pledge` and `unveil` restrict what a program may do; designed with security.
- **Linux Mint,** decided for Carbon: from the Standard (the era of the mod's Cinnamon), on the `linux` kernel, the one
  distribution that boots to its desktop, Cinnamon; 12,288 MB of disk and 64 of memory. Its medium boots a live
  desktop with *Install Linux Mint* on it, which runs the guided installer. Packages through `apt`, the store, and
  two programs of its own, by Spearmint Linux: the **Software Manager** and the **Update Manager**
  ([Programs](programs.md)).
- **Accounts and passwords:** every system has real user accounts and its `passwd`; with a directory on the network, the
  login is the network's ([Security](security.md)). Today they only exist as pages.
- **The portable devices' mobile systems** ([Wireless](wireless.md)): real systems, from the factory, with no installer,
  parodies of the real ones, with the registry's apps and the Mirror's store: Midsoft's **Frames Pocket** on the PDAs,
  **qOS** on Quince's phones and tablets, and **Droidix**, open and on the Linux kernel, on the other makers'. With them
  comes the **third family of systems and hardware**, next to Frames and Linux: **Quince**, in the manner of Apple,
  with its computers, servers and Mainframe, its phones and tablets, its systems (the Quince System, Quince OS 9,
  Quince OS X, Quince OS X Server, quinceOS and qOS) and the instruction sets they bring (68k, PowerPC, ARM64), decided
  for Carbon ([Quince](quince.md)).
- **The web of the world:** **sites** are files (pages, images) on a rack server running a **web server** service (a
  parody of a real one, like the Messenger Service); the **browser** is a program per era, a parody of the real ones
  (Netscape and Internet Explorer in the Legacy, Firefox and Chrome later), and shows the network's sites. Through the
  WAN Gateway Computer's federations ([The network](network.md)), other networks' sites come within reach: an "internet"
  of the server, between players. Later, a search engine, shops tied to Teracoin ([Teracoin](teracoin.md)) and pages
  with forms that run Σ# on the server. The whole design (what language pages are written in, what each era's browser
  can draw) is made in a cycle of its own.
