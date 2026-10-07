# Programs

Programs, the houses that make them, how they are installed, package managers and the Mirror, and backups. The languages
programs are written in are in [Σ and Σ#](sigma.md); the systems they run on in
[Operating systems](operating-systems.md).

## What a program is

Programs are **optional**. Nothing is needed to run a network: they are there for convenience, to see and handle the
network, and for extra features. They are real software, installed on disk and run by the operating system.

A program takes **no** orchestration capacity from Operations. What it spends:

- **Disk:** its size, on the disk it was installed on.
- **Memory:** the memory it holds while it runs, in the same memory ledger that weighs the system, the services, the
  open windows and the Σ# processes ([Hardware](hardware.md)).
- **Video memory:** graphical windows and programs that draw (Paint).
- **Instructions:** a Σ# program spends the machine's instruction credits ([Σ and Σ#](sigma.md)).

A computer that hasn't the memory for a program refuses to open it and says why.

What decides whether a program installs on a machine:

- the **system** (the platform: MC-DOS, MC-NET, Frames, Linux, FreeBSD, UNIX);
- the **machine** (the host scope: any, Mainframe, rack server, Crafting Computer, Cluster Management Computer, Personal
  Computer);
- the version of Frames, when the program asks for one (Frames XP or newer, for example); the other systems have no such
  ladder;
- the minimum hardware **era**, when the program declares one;
- the room on the disk;
- what it needs from the network: the network engines' studios only open with their engine running on the Mainframe, at
  a minimum version ([Operations](operations.md)).

## Media and installing

Every program has the **era** it was written in. The era decides the installer's medium and the year on its banner,
never where it installs ([Eras](eras.md)): Vintage on floppy disk, Legacy on CD, Transition on DVD, Standard on DVD (and
services on a USB stick), Advanced on Blu-ray (and services on a USB stick). Media and drives are in
[Storage devices](storage-devices.md) and [Peripherals](peripherals.md).

The installer is a medium that **projects** the package's files: it opens like an ordinary disk, lists its files, and is
locked (nothing is written to it or deleted from it). Its tooltip gives the package, the house and the requirements.
Installing runs the program's setup from Files, in the system's style. The installation media are in each era's creative
tab, next to the programs.

There are always **two** ways to install:

- from the **medium**, in a drive linked to the computer;
- from the network's **Mirror** (below), with the system's package manager, without a medium.

MC-DOS, MC-NET and UNIX System V only install from media. The physical medium stays the way in at the start of the game,
before the network has a Mirror.

## The catalogue

**One registry.** Every program is an entry of the program registry: its id, command, name and description in English
(translatable), whether it comes with the system, its platforms, its minimum processor, video memory and disk, its kind,
its minimum Frames version, its host scope, its icon, its minimum era, the era it was written in, its house, the memory
it holds and what it needs (a network engine at a version). Everything comes from the registry: the translation key, the
installation medium in the creative tab, the Mirror's package, the desktop launcher, the program's page and the locks by
system, machine, version and hardware. An add-on registers its programs in the same registry ([The API](api.md)).

**Kinds:** a **program** (opens windows), a **service** (no window, in the background), a **hybrid** (a service with a
panel where there is a desktop), a **desktop** (a graphical environment that turns a terminal Linux or FreeBSD into a
desktop), an **operating space** (what a network system draws on the screen instead of a prompt; MC-NET brings the
Interactor) and a **network engine** (the software that plans the network's work, [Operations](operations.md)).

### What comes with the system

These aren't installed. "Desktops" are Frames and the Linux, FreeBSD and UNIX systems with a graphical environment, and
each environment gives them its own names.

| Program | Command | Systems | Machine |
| --- | --- | --- | --- |
| Network | `network` | desktops | any |
| This PC | `thispc` | Frames, Linux, FreeBSD | any |
| Disks | `disks` | Linux | any |
| Workstation Info | `dtwsinfo` | UNIX, FreeBSD, Linux | any |
| Help | `helpview` | desktops | any |
| Settings | `settings` | desktops | any |
| Files | `files` | desktops | any |
| Editor | `editor` | desktops | any |
| Command Prompt | `cmd` | all but MC-NET | any |
| System Monitor | `sysmon` | desktops | any |
| Calculator | `calc` | desktops | any |
| Network Manager | `netmgr` | desktops; Frames XP+ | Mainframe |
| Task Manager | `taskmgr` | desktops | any |
| Device Manager | `devmgmt` | Frames | any |
| vi | `vi` | FreeBSD, UNIX | any |
| ee | `ee` | FreeBSD | any |

The Task Manager is not on the desktop or in the menu: it opens from the panel, as it always has
([Interface](interface.md)). Help reads the series' manuals and the machine's manual pages
([Operating systems](operating-systems.md)).

### What can be installed

The era is the one the program was written in, with the minimum hardware era in brackets when the program declares one;
without it, the system's lock applies. Memory and disk figures are estimates.

| Program | House | Kind | Era (from) | Memory MB | Disk MB | Systems | Machine |
| --- | --- | --- | --- | ---: | ---: | --- | --- |
| IQL Server Management Studio | Midsoft | program | Standard (Legacy) | 128 | 128 | desktops; Frames XP+ | any |
| Midsoft IQL Server | Midsoft | engine | Legacy | 24 | 32 | all | Mainframe |
| NextgreIQL | Nextgre | engine | Legacy | 48 | 32 | all | Mainframe |
| Prophet YourIQL | Prophet | engine | Legacy | 32 | 32 | all | Mainframe |
| Prophet Reactive Console | Prophet | program | Standard (Legacy) | 64 | 96 | desktops; Frames XP+ | any |
| Nextgre Planner Studio | Nextgre | program | Standard (Legacy) | 96 | 96 | desktops; Frames XP+ | any |
| Crafting Manager | Autodeck | program | Legacy | 32 | 128 | desktops; Frames XP+ | Crafting Computer |
| Pattern Studio | Autodeck | program | Legacy | 24 | 96 | desktops; Frames XP+ | any |
| Workshop | Autodeck | program | Legacy | 16 | 64 | desktops; Frames XP+ | Personal Computer |
| Cluster Manager | JSC Technologies | program | Standard | 96 | 96 | desktops; Frames XP+ | Cluster Management Computer |
| Gateway Manager | JSC Technologies | program | Legacy | 32 | 96 | desktops; Frames XP+ | any |
| Minesweeper | Midsoft | program | Vintage | 1 | 16 | desktops | any |
| Solitaire | Midsoft | program | Vintage | 1 | 16 | desktops | any |
| Snake | Midsoft | program | Vintage | 1 | 8 | desktops | any |
| 67ark | Vaultis | program | Legacy | 16 | 24 | desktops | any |
| Paint | Bellwether Labs | program | Legacy | 32 | 48 | desktops | any |
| Soundfoundry | Voidsoft | program | Legacy | 16 | 4 | desktops but CDE | any |
| Exceed | Midsoft | program | Standard | 64 | 64 | desktops; Frames XP+ | any |
| Messenger Service | Midsoft | service | Standard | 8 | 48 | all; Frames XP+ | server |
| Midsoft Messenger | Midsoft | program | Standard | 32 | 48 | desktops; Frames XP+ | any |
| KnotHub | Daylight Foundation | service | Standard | 16 | 64 | all; Frames XP+ | server |
| Knot | Daylight Foundation | program | Standard | 32 | 48 | desktops; Frames XP+ | any |
| Soundfoundry Server | Voidsoft | service | Standard | 16 | 32 | all | server |
| Storage Insights | Vaultis | program | Standard | 64 | 64 | desktops; Frames XP+ | any |
| Craft Planner | Autodeck | program | Standard | 64 | 64 | desktops; Frames XP+ | any |
| Automation Engine | Red Cap | service | Legacy | 64 | 32 | all; Frames XP+ | Mainframe |
| Automation Manager | Red Cap | program | Legacy | 96 | 64 | desktops but CDE; Frames XP+ | any |
| Predictive Cache | Vaultis | service | Standard | 128 | 32 | all | server |
| Load Balancer | JSC Technologies | service | Legacy | 16 | 32 | all | server |
| Integrity Monitor | Vaultis | service | Standard | 32 | 32 | all | server |
| Remote Control | Midsoft | program | Standard | 48 | 48 | desktops; Frames XP+ | any |
| Mirror | JSC Technologies | service | Standard | 32 | 64 | all | Mainframe |
| screenfetch | Arch Collective | program | Legacy | 1 | 4 | Frames, Linux, FreeBSD | any |
| Σ# Compiler | Sigma Foundation | program | Legacy | 16 | 8 | all; Frames XP+ | any |
| Σ Compiler | Sigma Foundation | program | Vintage | 2 | 4 | all | any |
| Sigma Runtime | Sigma Foundation | service | Legacy | 24 | 12 | all; Frames XP+ | any |
| Virtual Studio | Midsoft | program | Standard (Legacy) | 256 | 512 | Frames XP+ | any |
| Virtual Studio Code | Midsoft | program | Standard (Legacy) | 48 | 128 | desktops but CDE; Frames XP+ | any |
| Exposure | Daylight Foundation | program | Legacy | 64 | 192 | desktops but CDE; Frames XP+ | any |
| Vim | (the system's) | program | Vintage | 4 | 8 | all | any |
| Emacs | (the system's) | program | Vintage | 12 | 24 | all | any |
| KDE Plasma | KDE Guild | desktop | Legacy | 224 | 256 | Linux, FreeBSD | any |
| GNOME | GNOME Trust | desktop | Legacy | 256 | 192 | Linux, FreeBSD | any |
| Cinnamon | Spearmint Linux | desktop | Standard | 160 | 160 | Linux, FreeBSD | any |
| CDE | Open Desk Consortium | desktop | Legacy | 16 | 32 | UNIX, FreeBSD, Linux | any |
| Interactor | Nouvell Networks | operating space | Vintage | 1 | 2 | MC-NET | any |

What each one does is in its description in the registry and on the page of its subject: the engines and their studios
in [Operations](operations.md); the Crafting Manager, the Pattern Studio, the Craft Planner and the Workshop in
[Autocrafting](autocrafting.md) and [Peripherals](peripherals.md); the Cluster Manager in
[Servers and racks](servers-and-racks.md); the Gateway Manager in [Peripherals](peripherals.md); the Network Manager and
the Storage Insights in [Interface](interface.md); Soundfoundry in [Sound](sound.md); the server services in
[Servers and racks](servers-and-racks.md) and [Storage](storage.md); the editors and the Σ# toolchain in
[Σ and Σ#](sigma.md). The rest:

- **Automation Engine** and **Automation Manager** (Red Cap): the Mainframe's service that keeps the network's jobs and
  fires them on time, and the window that writes and watches them.
- **Midsoft Messenger** and **Messenger Service:** conversations between whoever is on the network. The service lives on
  a server; it grows on disk with the conversations it keeps and in memory with whoever has the messenger open.
- **Knot** and **KnotHub** (Daylight Foundation): the network's code repository, revision by revision, on a server; Knot
  sends files and shows who changed what.
- **Exceed** (Midsoft): a spreadsheet whose cells ask the network what it stores.
- **Paint** (Bellwether Labs): an image in an indexed palette, which can also become the desktop's wallpaper.
- **67ark** (Vaultis): packs files into one that weighs less, to stretch a small disk.
- **screenfetch:** the system's identity with the distribution's logo; until it is installed, the prompt answers
  "command not found", and that is how the package manager is learnt.
- Games: Minesweeper, Solitaire and Snake.

## Software houses

Every piece of software has a **house**, a parody maker, shown in the installer's tooltip, on the banner and on the
program's page. A program that comes **with** the system takes the system's house (Files is Midsoft's on Frames and the
KDE Guild's on Plasma), except the network tools, which are JSC Technologies' wherever they run.

| House | What it makes |
| --- | --- |
| Midsoft | Frames, MC-DOS, MC-NET, the IQL Server, Virtual Studio |
| Nouvell Networks | the Interactor |
| JSC Technologies | the firmware, the network tools, the Mirror, the Cluster Manager |
| Autodeck | the crafting family |
| Vaultis | storage: the Storage Insights, the cache, 67ark |
| Red Cap | Fedora, automation |
| Axiomatic Ltd. | Ubuntu |
| Debian Circle | Debian |
| Arch Collective | Arch Linux, screenfetch |
| Gentoo Foundry | Gentoo |
| Daemon Foundation | FreeBSD, ee |
| Bellwether Labs | UNIX System V, Paint |
| Open Desk Consortium | CDE |
| KDE Guild | KDE Plasma |
| GNOME Trust | GNOME |
| Spearmint Linux | Cinnamon |
| Sigma Foundation | Σ and Σ# (the compilers and the runtime) |
| Daylight Foundation | Exposure, Knot, KnotHub |
| Voidsoft | Soundfoundry |
| Nextgre | NextgreIQL and its studio |
| Prophet | Prophet YourIQL and its console |

Hardware makers are in [Eras](eras.md).

## Remote Control

Remote Control (Midsoft, Standard, Frames XP or newer and the Linux desktops) lists the network's computers (name,
chassis, system, state) and opens the chosen one's **remote control**: the window shows that machine's screen, its
firmware, its prompt or its whole desktop if it has one, with full interaction. It is the easy way into rack servers
with no monitor ([Servers and racks](servers-and-racks.md)), with the same power as `ssh` and no command line needed.
Being on the same network is access; a computer refuses with `config remote off`.

## Package managers and the Mirror

The **Mirror** is the Mainframe's service (Standard, any system) that keeps the network's packages: the mod's catalogue
and the packages players publish ([Σ and Σ#](sigma.md)). Every package manager resolves against it; with no Mirror in
reach, they say so.

| Manager | System | Verbs |
| --- | --- | --- |
| `pckmgr` | Frames (all) | `install`, `remove`, `search`, `list [--available]`, `update` |
| `apt` | Debian, Ubuntu | `install`, `remove`, `search`, `list`, `update`, `upgrade` |
| `dnf` | Fedora | `install`, `remove`, `search`, `list`, `update`, `upgrade` |
| `pacman` | Arch Linux | `-S`, `-R`, `-Ss`, `-Q`, `-Syu` |
| `emerge` | Gentoo | `--ask`, `--unmerge`, `--search`, `--sync`, `--update @world` |
| `pkg` | FreeBSD | `install`, `delete`, `search`, `info`, `update`, `upgrade` |

Each system only has its own (there is no `apt` on Arch and no `pacman` on Ubuntu). `list --available` shows what isn't
installed and the Mirror offers. On a desktop, the result shows at once on the launchers.

**Versions.** Every program has its version, as a package manager or an About box would give it (screenfetch 3.9.1,
Exceed 12.0), written by hand; a program that gives none is 1.0. Every computer keeps the installed version of each
package, and each manager's update verb brings the out-of-date ones to the registry's version. A compiler's major
version is the version of the language it knows (sgsc 2.0 is Σ# 2). Out-of-date programs keep running; they never break
on their own.

## To build

- **The "update available" badge.** Settings > Programs marks with "update available" every program whose registry
  version is newer than the installed one, and each manager's `list` marks them too (like `apt list --upgradable`).
- **The Backup Service:** a service on a rack server (there is no backup chassis) that keeps **data only**: files,
  settings, `.craft` patterns, IQL and Σ# scripts, Teracoin wallets ([Teracoin](teracoin.md)) and system images.
  - **Nothing can be duplicated through it:** the contents of an **item** never go into a backup, in any form, on any
    medium. The only redundancy for items is RAID ([Servers and racks](servers-and-racks.md)), which mirrors the volume
    without ever copying it out.
  - It keeps backups on another server's disk or in the **Tape Library**, a rack unit per era, huge and slow: cold
    storage.
  - Backups run by hand or as an Automation Engine job (every night, for example).
  - **Restoring** puts files back: a system image fixes a corrupted system ([Storage](storage.md)); a restored wallet
    gives back access to its coins.
- **More programs**, decided and waiting:
  - **The Media Player:** each system's media player, after the real ones (Frames', the Linux ones), playing the
    machine's sound files and image sequences. Its form and era are decided when it is made.
  - **The Data Compressor:** a storage service on a server (Vaultis, Standard). More items per disk (+20%, an estimate)
    at the cost of a longer wait on every read (+2 ticks): good on a hard disk, debatable on an SSD, bad on NVMe, where
    it triples the wait. A trade-off for the player to decide.
  - **The Crypto Wallet** and **the Mining Panel** ([Teracoin](teracoin.md)).
  - **The firewall, the IDS and the antivirus**, with security's second phase ([Security](security.md)).
  - **The Defrag**, which every computer comes with ([Storage](storage.md)).
  - **The AI framework** of each era, the **inference service** and the **AI management program**
    ([Artificial intelligence](ai.md)).
