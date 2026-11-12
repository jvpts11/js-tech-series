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
| OpenBSD | Legacy | a shell; a desktop comes up once xenodm is switched on | guided, question by question |
| Debian, Ubuntu, Fedora | Legacy | a shell until a desktop is added | guided |
| Linux Mint | Standard | the Cinnamon desktop; its medium starts a live session | guided, from the live desktop |
| Arch Linux | Legacy | a shell until a desktop is added | by hand, step by step, as Arch is |
| Gentoo | Legacy | a shell until a desktop is added | compiled from source, as Gentoo is |
| Frames 7 | Transition | the Frames 7 desktop | from DVD, guided |
| Frames 10 | Standard | the Frames 10 desktop | from USB stick, guided |
| Frames 11 | Advanced | the Frames 11 desktop | from USB stick, guided |
| Quince System | Vintage | the Quince System desktop, on the Russet 128K's 68k | from three floppies, guided |
| Quince OS 9 | Legacy | the Quince OS 9 desktop, on the PowerQ G3 and G4 | from CD, guided |
| Quince OS X | Legacy | the Quince OS X desktop and its Dock, on PowerPC and Integra | from CD, guided |
| Quince OS X Server | Transition | Quince OS X with Server Admin, for servers | from CD, guided |
| quinceOS | Advanced | the quinceOS desktop, on Quince's own chips | guided, and from its recovery |

The space a system takes is counted in items of the disk's era: Frames 11 takes 80 items of a 64-bit disk, and
MC-DOS a fifth of a 20 MB Vintage drive.

### Quince's systems

*Added 2026-11-09.*

Quince's systems run on Quince's computers ([Hardware](HARDWARE.md)), each on the processors it was built for: the
Quince System on the 68k, Quince OS 9 on the PowerPC, Quince OS X on the PowerPC and on Integra, its Server on
Integra and on Quince's own chips, and quinceOS on Quince's own chips. Quince OS X carries Champollion, which runs a
program built for the PowerPC on an Integra machine, more slowly; the Server and quinceOS carry Champollion 2, which
does the same for Integra programs on Quince's chips.

- **The desktop.** A menu bar across the top, the fruit's menu at its left and the program in front named beside it;
  the disks and the Trash on the desktop; the Seeker for files. From Quince OS X, the Dock along the bottom holds the
  programs and what is open. The classic systems have no command line; from Quince OS X the Terminal opens zsh, and
  The Daemon Handbook has a chapter on Quince's systems.
- **Installing.** The Quince System comes on three floppies: its installer copies the first, asks for the next, and
  goes on once it is in the drive. The others install from one disc. On Quince's own chips, Options on the start-up
  screen opens quinceOS's recovery, to reinstall it, repair its disk with First Aid, or choose the startup disk.
- **Memory.** The Quince System and Quince OS 9 open each program in a block of its own, set in the program's Get
  Info: one size on the Quince System; on Quince OS 9 a minimum and a preferred size, the program opening in its
  preferred block while that much is free, in what is left above its minimum otherwise, and not at all below it. About This Computer shows the blocks. Quince OS X and
  quinceOS share the memory as the other systems do, but when it runs out they end nothing themselves: the programs
  pause and Force Quit Applications asks which to quit, and they go on once there is room.
- **When it crashes.** The classic systems show the bomb with its error's number (Quince OS 9 names the program that
  was running), and the later ones the grey page asking you to restart in four languages. Both stand until you press
  Restart or a key; the machine then starts over.
- **Programs.** qTunes (the machine's music) and Expedition (a browser, waiting for a web the world has yet to
  build) come from Quince OS 9; Preview (pictures), the Terminal and the Quince Store from Quince OS X; Server Admin
  (start and stop a Mainframe's services) with the Server. From the Standard, Quince OS X and quinceOS install
  programs from the Mirror with `keg` at the Terminal or from the Quince Store, free.

### OpenBSD

*Added 2026-11-10.*

OpenBSD, by the Blowfish Project, at release 7.5, is secure by default: it comes up at its terminal with only what it
needs switched on. Its prompt is the computer's name and a hash, in `ksh`; The Daemon Handbook has a chapter on it.

- **Installing.** Its installer asks at the terminal, question by question: the name, sshd, X and xenodm, the root
  disk (`wd0` for an IDE disk, `sd0` for a SATA one) and the sets. `bsd` and `base` always go; `bsd.rd`, `comp` and
  `man` may be left out, and each one left out keeps its room off the disk, `comp` taking `make` with it and `man`
  taking `man` and `apropos`.
- **Starting.** It starts through `boot>`, which waits five seconds without a count: any key stops the wait, and
  Enter or `boot` starts.
- **Its daemons.** `rcctl` switches them: `enable` and `disable` for every start, `start` and `stop` now. pf, pflogd
  and syslogd always run; a service you install comes switched off; a desktop installed still comes up at the
  terminal until `rcctl enable xenodm`.
- **Software.** Packages with `pkg_add`, `pkg_info` and `pkg_delete`; the ports tree with `cvs` and `make`; the
  release's patches with `syspatch`. `pfctl` reports on the firewall and `doas` runs a command as another user.
- **When it panics.** It stops in its debugger at `ddb>`, with `show panic`, `trace` and `ps`, and starts over only on
  `boot reboot`.

### Linux Mint

*Added 2026-11-10.*

Linux Mint, by Spearmint Linux, from the Standard, is the distribution that comes up at its desktop, Cinnamon, behind
its own mark. Its medium starts a live session that writes nothing to the disk, with Install Linux Mint on it: a
guided installer in five screens that puts it on ext4, with a swap as big as the memory, beside another system or on
the whole disk. Besides `apt`, it has two programs of its own: the **Software Manager**, the Mirror's programs by
Mint's categories with Network and Desktops, and the **Update Manager**, which installs what you tick, a program at
once and the desktop the next time it starts, its shield on the panel while there are updates.

### Systems that players write

*Added 2026-11-10.*

A player can write an operating system of their own in Σ#: a class that implements `ISystem` in the namespace
`System.OS`, which declares its name, version, author, era, instruction set, memory and kernel. The machine runs it as
its process 1; it draws its own boot page and its own panic page, starts its services and a desktop, and stands on one
of the mod's kernels or on one of its own, whose scheduler, file system, drivers, memory rule and shell it chooses.
`sigma build --os` writes its installer onto a blank medium of its era, and `sgpack --publish` puts it on the Mirror,
marked with its author and *community*; it then installs like any other system, from its medium or through the network
([Starting through the network](#starting-through-the-network)). A desktop of a player's own implements `IDesktop`
(its panel, its menu, its frames, its colours, its wallpaper and its programs), is built with `sigma build --desktop`,
comes with the system that starts it, and the package managers of the mod's systems install it from the Mirror. The
Book of Sigma has a section on both ([design](design/player-systems.md)).

## Desktops

*Added 2026-08-26.*

A **desktop** is the windows, the taskbar or panel, and the start menu. Each Frames comes with its own. On Linux,
the BSDs and UNIX a desktop is a package you add: **KDE Plasma**, **GNOME** and **CDE** from the Legacy, and
**Cinnamon** from the Standard (CDE runs on UNIX, the BSDs and Linux; the others on Linux and the BSDs). Linux Mint
comes with Cinnamon, and OpenBSD brings a desktop up only once xenodm is switched on. Each desktop looks like its
era: KDE in the Transition is the KDE of that time, not of today.

A launcher with more programs than it has room for stays on the screen (*added 2026-11-12*): Frames XP's All
Programs opens the list of every program beside its Start menu, Frames 7's search finds what its column has no room
for, and the other launchers' lists scroll with the wheel.

Every desktop has the same programs under the names its system gives them: Network, This PC, Settings, Files,
Editor, Command Prompt, System Monitor, Calculator, Network Manager and Help ([Help on a computer](#help-on-a-computer)).
Frames adds the Device Manager; CDE adds Workstation Info.

## Help on a computer

*Added 2026-10-06.*

Every computer can read the series' manuals (the Technical Reference, the Guide to Operations and the Plant
Drawings: the same entries the binders print, in your language) beside the manual pages of its own commands. How it
reads them is its system's own:

| System | Where | How |
| --- | --- | --- |
| Frames 95 | Help, on the Start menu | Help Topics: Contents (the books), Index and Find; Display shows a topic in a window of its own, with Help Topics and Back. |
| Frames XP | Help and Support | The Help and Support Center: Search in the blue band; Back, Home, Index, Favorites and History; the books on the left. |
| Frames 7 | Help and Support | Back and forward, Search Help and Browse Help over the books and the page. |
| Frames 10 and 11 | Get Help | One search across the top, the books, and the page. |
| KDE Plasma | Help Center | The books on the left, the page on the right. |
| GNOME, Cinnamon | Help | No tree: each page lists what it holds, and the line above it says where it is. |
| Quince's systems | Help Center, Help Viewer from Quince OS X | As GNOME's: each page lists what it holds. |
| CDE | Help Viewer | The topic hierarchy above the page; Backtrack, History..., Index... and Top Level. |
| MC-DOS, MC-NET | `help`, `help <topic>` | The whole screen in the sixteen colours: Tab picks a link and Enter follows it; Alt+C the contents, Alt+N the next topic, Alt+B back, Alt+I the index; Alt+S finds, Escape leaves. |
| Linux (a shell) | `info`, `info <topic>` | info: every chapter, section and entry is a node; n, p and u move, Tab and Enter follow a link, m goes to a menu item, s searches, l goes back, q leaves. |
| UNIX, FreeBSD, OpenBSD (a shell) | `man <topic>` | A manual's entry as a page of section 7 in the pager, when no command has that name: `man graphics-cards`. |

A topic is named by the last part of its id (`graphics-cards`, with hyphens or underscores) or by its title. A
search finds every entry holding all the words typed, and every command whose name or summary holds them. Every
system but the BSDs still has `help` for its commands at the prompt; on Frames it prints, as it always did.

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

*Added 2026-11-11.* Each installer asks where the system goes as its real one did
(computers/docs/design/storage-devices.md): Frames XP lists the partitions and the free space in text mode and then
asks how to format; Frames 7 to 11 show a table with New, Delete, Format and Extend; Ubuntu and Linux Mint offer to
install alongside another system, shrinking its partition with a divider; Debian and Fedora show each disk's
partitions; FreeBSD offers Auto (ZFS), with a mirror on a second disk, and Auto (UFS). MC-DOS, UNIX System V and
Frames 95 take their disk and format it. Under UEFI the installer makes the EFI partition first, and on Linux, the BSDs
and UNIX a swap partition as big as the memory ([Hardware](HARDWARE.md#memory-ram)).

### Starting through the network

*Added 2026-11-10.*

From the Legacy on, a computer on a network whose Mainframe runs the Mirror needs no medium: the firmware starts the
installer through the network. On the Legacy and the Transition, choose **Network (PXE)** on the Boot page of the
Setup; the network card's agent asks the Mainframe, and the Mirror's menu opens in the BIOS's blue. On the Standard
and the Advanced, choose **HTTP Boot** in the Boot Manager, and the Mirror's list opens on the firmware's own page.
Only the systems the computer runs are listed, each with its house, and *Boot from the local disk* is last. With no
Mirror on the network, the firmware says so and goes back.

### Two systems on one computer

Install a second system on another disk, or on another partition of the same one. The computer then stops at a boot
manager each time it starts (the Midsoft Boot Manager on Frames, listing each Frames with its partition; GRUB on
Linux, listing every other system and the Frames Boot Manager of the EFI partition; FreeBSD's own loader) so you
choose which to start. The firmware's boot order says which disk starts first, and its boot menu (F12) has a line for
every system on every partition. On the Vintage the disk starts its active partition: set it with option 2 of
FDISK on MC-DOS, or of `fdisk` on UNIX.

### The disk tools

*Added 2026-11-11.*

| System | In a window | At the prompt |
| --- | --- | --- |
| MC-DOS | | `FDISK` |
| Frames XP to 11 | Disk Management (no resizing on XP) | `diskpart` |
| Linux | the KDE Partition Manager on KDE, Disks elsewhere | `fdisk -l`, `lsblk`, `mkfs.ext4` and the other `mkfs.*`, `btrfs` |
| UNIX System V | | `fdisk` |
| FreeBSD | | `gpart show`, `zfs`, `zpool` |
| OpenBSD | | `fdisk`, `disklabel -h` |
| The Quince System, Quince OS 9 | the Special menu's Erase Disk and Set Startup | |

On the classic Quince systems, **Erase Disk...** lists the volumes the system reads, the startup disk among them and
never chosen, and asks before it erases the one picked, formatting it again in its own file system. **Set
Startup...** lists the volumes with a system on them; the machine starts from the one picked the next time it starts.

Every tool refuses to delete or format the partition the running system runs from, or a swap it swaps to; a
partition resizes by its file system's rules and never below what it holds; a disk changes between MBR and GPT only
when it is empty, and a new table erases the disk ([Hardware](HARDWARE.md#partitions-and-file-systems)).

### After a sudden shutdown

*Added 2026-11-08.*

A computer that went dark with its system up (a power loss, unstable power, a failing power supply,
[Hardware](HARDWARE.md#power-supplies-psu)) leaves its disks unclean, and the next system to start from them checks
them first, as its file system needs. The mark stays on each disk, so a disk moved to another computer is checked
there; nothing is erased, and a clean shutdown leaves no mark.

| System | Formats with | What it shows | How long |
| --- | --- | --- | --- |
| MC-DOS, MC-NET | FAT16 (FAT32 from the Legacy) | nothing: they never checked, and the mark waits for a system that does | none |
| Frames 95 | FAT32 | ScanDisk on its blue screen, area by area | a second for every 64 items in use, 5 seconds to a minute |
| Frames XP, Frames 7 | NTFS | chkdsk's three stages, on blue on XP and on black on 7 | 2 seconds (its journal) |
| Frames 10, Frames 11 | NTFS | "Scanning and repairing drive" under the mark | 2 seconds |
| Linux | ext3 on the Legacy, ext4 later, btrfs on Fedora from the Standard; ext4 when the root is formatted by hand | the journal replayed on the console; btrfs nothing | 2 seconds; none on btrfs |
| FreeBSD, OpenBSD, UNIX | UFS | fsck's five passes on the console | a second for every 64 items in use, 5 seconds to a minute |
| Quince's systems | HFS on the Quince System, HFS+ on Quince OS 9, HFS+ journaled from Quince OS X, APFS on quinceOS | nothing at start-up; Disk Utility's First Aid checks a disk when asked | as fsck |

Each partition the system has in use is checked as its own file system, and the check takes as long as all of them.

## Programs

*Added 2026-08-26.*

Every system comes with its programs (the list under Desktops, and Task Manager: what the machine is running, what
it spends, and how to end it). Others are installed:

- **From media**, like a system: the program's disc in a joined drive, then its setup from Files.
- **From the Mirror**, with the system's package manager (`apt`, `dnf`, `pacman`, `emerge`, `pkg`, Frames'
  `pckmgr` or Quince's `keg`, from the Standard), with no media at all, when the network has a Mirror (below).
  MC-DOS, MC-NET, UNIX System V and Quince's classic systems install from media only.

Some programs only run on one kind of computer: the Network Manager on the Mainframe, the Crafting Manager on a
Crafting Computer, the Cluster Manager on a Cluster Management Computer, the Workshop on a Personal Computer, Nouvell
BorderLink on a WAN Gateway Computer. Some
need the network to run a particular engine ([The network](NETWORK.md#engines)), and say so when opened.

| Program | First era | What it does |
| --- | --- | --- |
| Crafting Manager | Legacy | Loads crafting patterns and shows the jobs the network is working through. |
| Pattern Studio | Legacy | Writes crafting table, machine and many-step recipes, and burns them onto media at an encoder. |
| Workshop | Legacy | Crafts, smelts, enchants and repairs with the workshop cards. |
| Automation Manager | Legacy | Writes and watches the jobs the network runs on its own. |
| Gateway Manager | Legacy | Manages the Network Gateways on this computer's ports, and what ComputerCraft may do through them. |
| Nouvell BorderLink | Vintage | A WAN Gateway Computer's service: its long-distance lines, what each far side may do here, and its log; a window on Frames, its own operating space on MC-NET, `borderlink` at the prompt (added 2026-11-09). |
| Storage Insights | Standard | The biggest stocks, what is running low, how full each server is. |
| Craft Planner | Standard | Shows what a craft needs, what is missing and what it costs, before it starts. |
| Cluster Manager | Standard | Runs every supercomputer and datacenter section as one machine. |
| IQL Server Management Studio | Standard | Writes and runs IQL against the network's engine. |
| Nextgre Planner Studio | Standard | Shows and steers how NextgreIQL plans the network's work. |
| Prophet Reactive Console | Standard | Declares the state the network keeps with Prophet YourIQL. |
| Remote Control | Standard | Takes over another machine of the network and uses it on this screen. |
| Backup Console | Legacy | Sets the network's backups, watches them run and restores them (added 2026-11-11). |
| Crypto Wallet | Transition | Blockwright's Teracoin wallet: makes wallets, sends and receives TRC, and prints items on the 3D printers (added 2026-11-11, [Teracoin](#teracoin)). |
| Mining Panel | Transition | Watches the network's Mining Pools: their hashrate, miners and blocks, and their owner's settings (added 2026-11-11). |
| Easy Disc Creator | Legacy | Roxa's disc copier for the Frames systems, from Frames 95 on: copies a floppy, a CD, a DVD, a Blu-ray disc, a USB stick or, through a Vinyl RW Drive, a vinyl onto a blank of the same format or a larger one, at the medium's write speed; with one drive it reads the source, then asks for the blank. Install discs copy like any other; the disks of the network's storage and the .dat files never do (added 2026-11-11). |

And editors (Vim, Emacs, Virtual Studio, Virtual Studio Code, Knot), the Σ# and Σ compilers ([Σ#](SIGMA.md)),
Soundfoundry (music), Paint, Exposure, Exceed, Midsoft Messenger, screenfetch, and games (Minesweeper, Solitaire,
Snake, 67ark).

Every desktop also comes with a **Media Player** (added 2026-11-11), under its own name there: Media Player on
Frames 95 and 11, Frames Media Player on XP, 7 and 10, Dragon Player on KDE Plasma, Videos on GNOME, Celluloid on
Cinnamon, Audio on CDE and the QuinceTime Player on Quince's systems. It plays the computer's songs through the same
player Soundfoundry plays through, and image sequences: a folder of pictures from Paint or a camera, shown in the
order of their names at 12 a second (1 to 20 in its options). A folder's menu in Files has Play and Add to its list.
A song needs a sound card; pictures play without one.

Scanners (added 2026-11-11, [Hardware](HARDWARE.md#peripherals)) put their files in the system's scans folder,
`Users/Public/Documents/Scans` on Frames and MC-DOS and `Documents/Scans` in the home folder on Linux, FreeBSD and
UNIX System V: a written book or a book and quill as a **.book** (its title and author, then each page under its
heading, which the Editor opens and changes), an enchanted book as a **.ench** (the Editor), a map as a **.map**
(Paint, which saves it as a .pix), and a Printed Paper sheet as a .txt or a .pix. A .ench and a .map are sealed to
the world that scanned them: changed, or carried to another world, they no longer print. A .map prints onto an
empty map in a printer's tray, from the computer, `PRINT` or `lp`; a .book and a .ench print only on the Large Scale
Printer, from the Print dialog, as do the systems' and languages' own books from the help (Frames 95's Print, CDE's
File > Print, Ctrl+P).

### Backups

*Added 2026-11-11.*

The **Backup Service** (Vaultis, Legacy) runs on a server in a rack and backs the network up: its items, the files
in every computer's home folder, the computers' settings, the network's .craft patterns, the IQL and Σ# scripts and
the computers' system images. A job writes to a **Tape Library** ([Hardware](HARDWARE.md#servers-and-their-racks)) or
to a file in the Backups folder of another server's drive, by hand or every day or week at an hour, as an Automation
Engine job (`BACKUP <job>` in IQL, `WHEN clock = '02:00'`). The **Backup Console** on any desktop sets the jobs and
restores the backups.

Items come back only as far as they stopped existing: the network keeps a book of losses (a dead disk, a Defrag that
gave up, a RAID 0 that lost a drive, a DROP), and a restore gives back, item by item, the lesser of what the backup
holds and what the book lost since, each loss once. What was taken out, exported or crafted never comes back.

### Teracoin

*Added 2026-11-11.*

**Teracoin** (TRC) is the world's cryptocurrency: one chain of blocks for the whole server, saved with the world. A
block comes about every 10 minutes (the difficulty adjusts every 10 blocks, over a floor that grows with the world's
days), pays 50 TRC, halving every 2,100 blocks, and one block in twenty also brings a rare item into the network
storage of the machine that found it. A TRC divides into eight decimal places.

Blockwright's four programs come from the Transition:

- The **Crypto Wallet** keeps each wallet as a file, `Users/Public/Documents/Wallets` on Frames and MC-DOS and
  `Documents/Wallets` in the home folder elsewhere; the file holds the key of one address, so whoever has a copy spends
  the same balance, and losing every copy loses the TRC. Its tabs: Overview, Send (no fee; a send waits for the next
  block, or 10 minutes when none comes), Receive (the address, also as a square of blocks), Transactions, Print and
  Wallets. A portable keeps one wallet of its own in its Wallet app.
- The **Blockwright miner** (`miner`) mines at every prompt: `miner --address <address>` sets whom it pays, `miner`
  mines in front of the terminal until Q, `miner start` and `miner stop` mine in the background, `miner status` tells
  how it goes, and `--pool <name>` or `--alone` choose a pool or none. A processor mines at its capacity and a
  graphics card at ten times its power ([Hardware](HARDWARE.md#teracoins-hardware)).
- The **Mining Pool** is a service on a rack server: it mines with the miners that choose it and shares each block's
  reward by the work each gave since its last block (hashrate times time), less its owner's fee, 0 to 5% (1% at
  first). Its owner may take only the miners they accept. The rack's window shows its card.
- The **Mining Panel** shows the network's pools: Pool, Miners, Blocks and, at the pool's own server, Settings.

The **Print** tab of the wallet sends items to the 3D printers linked to the computer, at the prices of the server's
list (`teracoin.printer.prices`), each item paid from the wallet as it starts; the TRC paid leave the world. **Auto
print** prints one item by itself whenever the balance passes what the wallet keeps by its price, while the computer
holding the wallet's file is on.

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
- **Help says "Asking the machine..." under Commands.** The machine has not answered yet, or the terminal is not
  joined to one; the manuals still read, since they are in your game.
- **`man` says "No manual entry" for a topic.** On Linux the manuals are read with `info`; elsewhere, check the
  topic's name (`man graphics-cards`, not `man graphics`), or search for it in a desktop's Help.
