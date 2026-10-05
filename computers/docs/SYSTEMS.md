# Systems and programs

What a computer runs: its operating system, its desktop and its programs; how each is installed; and what the
network adds. Every system here is a parody of a real one, made by a software house of the mod's world (Midsoft
makes Frames, for example), and behaves like the one it stands for.

## The systems

*Added 2026-08-26.*

An **operating system** is what a computer starts into, and what every program runs on. Each needs hardware of its
era or newer (its "first era" below); older hardware refuses it. It takes room on the disk it is installed on, and
memory while it runs.

| System | First era | What you get | Installs |
| --- | --- | --- | --- |
| MC-DOS | Vintage | a command prompt | from floppy, guided |
| MC-NET | Vintage | the Interactor: the whole screen is the network's terminal | from floppy, guided |
| UNIX System V | Vintage | a shell; the CDE desktop can be added | from media only |
| Frames 95 | Legacy | the Frames 95 desktop | from CD, guided |
| Frames XP | Legacy | the Frames XP desktop | from CD, guided |
| FreeBSD | Legacy | a shell until a desktop is added | guided |
| Debian, Ubuntu, Fedora | Legacy | a shell until a desktop is added | guided |
| Arch Linux | Legacy | a shell until a desktop is added | by hand, step by step, as Arch is |
| Gentoo | Legacy | a shell until a desktop is added | compiled from source, as Gentoo is |
| Frames 7 | Transition | the Frames 7 desktop | from DVD, guided |
| Frames 10 | Standard | the Frames 10 desktop | from USB stick, guided |
| Frames 11 | Advanced | the Frames 11 desktop | from USB stick, guided |

The space a system takes is counted in items of the disk's era: Frames 11 takes 80 items of a 64-bit disk, and
MC-DOS a fifth of a 20 MB Vintage drive.

## Desktops

*Added 2026-08-26.*

A **desktop** is the windows, the taskbar or panel, and the start menu. Each Frames comes with its own. On Linux,
FreeBSD and UNIX a desktop is a package you add: **KDE Plasma**, **GNOME** and **CDE** from the Legacy, and
**Cinnamon** from the Standard (CDE runs on UNIX, FreeBSD and Linux; the others on Linux and FreeBSD). Each desktop
looks like its era: KDE in the Transition is the KDE of that time, not of today.

Every desktop has the same programs under the names its system gives them: Network, This PC, Settings, Files,
Editor, Command Prompt, System Monitor, Calculator and Network Manager. Frames adds the Device Manager; CDE adds
Workstation Info and the Help Viewer.

## Installing a system

*Added 2026-08-26.*

1. Put the system's install medium in a drive joined to the computer: a floppy in a Floppy Drive (Vintage), a CD
   in a CD Drive (Legacy), a DVD in a DVD Drive (Transition), a USB stick in a Dock Station (Standard and
   Advanced). The media are in each era's creative tab, among the programs.
2. Use the computer's monitor. With no system installed, it shows the computer's firmware.
3. Choose the drive and boot it. The installer starts, in the style of its system.
4. Follow it. Most installers ask which disk and to confirm. Arch Linux asks you to type the real steps of its
   guide; Gentoo compiles first. (A server setting can make both ask for every step.)

The installer puts the system on the first disk that has none, unless you choose another.

### Two systems on one computer

Install a second system on another disk. The computer then stops at a boot manager each time it starts (the
Midsoft Boot Manager on Frames, GRUB on Linux, FreeBSD's own loader) so you choose which to start. The firmware's
boot order says which disk starts first.

## Programs

*Added 2026-08-26.*

Every system comes with its programs (the list under Desktops, and Task Manager: what the machine is running, what
it spends, and how to end it). Others are installed:

- **From media**, like a system: the program's disc in a joined drive, then its setup from Files.
- **From the Mirror**, with the system's package manager (`apt`, `dnf`, `pacman`, `emerge`, `pkg` or Frames'
  `pckmgr`), with no media at all, when the network has a Mirror (below). MC-DOS, MC-NET and UNIX System V install
  from media only.

Some programs only run on one kind of computer: the Network Manager on the Mainframe, the Crafting Manager on a
Crafting Computer, the Cluster Manager on a Cluster Management Computer, the Workshop on a Personal Computer. Some
need the network to run a particular engine ([The network](NETWORK.md#engines)), and say so when opened.

| Program | First era | What it does |
| --- | --- | --- |
| Crafting Manager | Legacy | Loads crafting patterns and shows the jobs the network is working through. |
| Pattern Studio | Legacy | Writes crafting table, machine and many-step recipes, and burns them onto media at an encoder. |
| Workshop | Legacy | Crafts, smelts, enchants and repairs with the workshop cards. |
| Automation Manager | Legacy | Writes and watches the jobs the network runs on its own. |
| Gateway Manager | Legacy | Manages the Network Gateways on this computer's ports, and what ComputerCraft may do through them. |
| Storage Insights | Standard | The biggest stocks, what is running low, how full each server is. |
| Craft Planner | Standard | Shows what a craft needs, what is missing and what it costs, before it starts. |
| Cluster Manager | Standard | Runs every supercomputer and datacenter section as one machine. |
| IQL Server Management Studio | Standard | Writes and runs IQL against the network's engine. |
| Nextgre Planner Studio | Standard | Shows and steers how NextgreIQL plans the network's work. |
| Prophet Reactive Console | Standard | Declares the state the network keeps with Prophet YourIQL. |
| Remote Control | Standard | Takes over another machine of the network and uses it on this screen. |

And editors (Vim, Emacs, Virtual Studio, Virtual Studio Code, Knot), the Σ# and Σ compilers ([Σ#](SIGMA.md)),
Soundfoundry (music), Paint, Exposure, Exceed, Midsoft Messenger, screenfetch, and games (Minesweeper, Solitaire,
Snake, 67ark).

## The Mirror

*Added 2026-09-23.*

The **Mirror** is a program installed on the Mainframe (Standard era) that keeps the network's packages. With it,
every computer of the network installs programs with its package manager, with no media; and players can publish
their own packages (Σ# programs packed with `sgpack`) for the others to install.

## What can go wrong

- **The installer refuses the system.** The hardware is older than the system's first era, or the disk is too
  small for it.
- **The drive does not show in the firmware.** It is not joined to the computer, or it is not the drive for that
  medium: each medium goes in its own kind of drive.
- **A program says it needs another engine.** It is written for an engine the network does not run. Install that
  engine on the Mainframe.
- **A program will not install from the package manager.** The network has no Mirror, or this system installs
  from media only.
- **The computer starts into the wrong system.** Change the boot order in the firmware, or pick at the boot
  manager.
