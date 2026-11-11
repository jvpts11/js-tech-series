---
section: get_programs
order: 79
icon: jsc:cd_rom
---
# Programs

A program is software that does one job for you, such as drawing a picture, playing a game or keeping a list of files. Every system comes with some programs, and you can install others. Each program has a name, runs on some kinds of system, and needs some disk space and memory.

> [!DEFINE] Program
> Software that a system runs for you, in a window or at a prompt.

There are two ways to install a program. You can install it from install media: the disc or stick the program ships on, such as a CD (compact disc), a DVD (digital versatile disc) or a USB (universal serial bus) stick. Or you can install it from [the Mirror](jsc:mirror), by name, with no media at all.

From media, you put the program's install medium in a linked drive of the computer, and then you start its setup.

1. Put the program's install medium in a linked drive of the computer.
2. At a prompt, type install and the program's name, then press [[Enter]].

On a Frames desktop you can also open This PC, where each linked drive shows its program with an Install button beside it. The computer answers with a line that starts with Setting up, then the program's name, then from and the medium, such as the floppy or the CD.

> [!DEFINE] Desktop
> The graphical face of a system: its windows, icons, menus and the programs you start from it.

Table: Install media by era

| Era of the program | Medium | Drive you use |
| --- | --- | --- |
| Vintage | Floppy | Floppy Drive |
| Legacy | CD | CD Drive |
| Transition | DVD | DVD Drive |
| Standard | USB stick for a service that runs in the background, DVD for the rest | Dock Station for the stick, DVD Drive for the disc |
| Advanced | USB stick for a service that runs in the background, Blu-ray for the rest | Dock Station for the stick, Blu-ray Drive for the disc |

The systems come on the same media, by their era: a Vintage system on a floppy, a Legacy system on a CD, a Transition system on a DVD, and a Standard or Advanced system on a USB stick. Read more about the drives on their own page ([](jsc:drives)).

From the Mirror you install by name, and the computer needs a network whose Mainframe runs the Mirror. The Mirror is the package service of that Mainframe: it keeps the network's packages, and each computer installs from it with its package manager.

> [!DEFINE] Package
> A program as the network's repository keeps it, ready to install by name.

> [!DEFINE] Package manager
> The program that fetches a package and records what is installed on the computer, such as apt, dnf, pacman, emerge, pkg or pckmgr.

Table: Package managers by system

| System | Package manager |
| --- | --- |
| Ubuntu and Debian | `apt` |
| Fedora | `dnf` |
| Arch Linux | `pacman` |
| Gentoo | `emerge` |
| FreeBSD | `pkg` |
| Frames 95 to 11 | `pckmgr` |
| MC-DOS, MC-NET and UNIX System V | None: these systems install programs from media only. |

Gentoo builds each program from its source code. When the computer has room for the build, the install runs as a script in front of the terminal, and the answer comes later.

The store command lists every program that is not preinstalled, one line each, with the install command beside it. You can also type available. The list does not depend on your computer: a program in the list may still refuse to install here, and the install says why. When nothing is left, store answers "nothing else to install".

The update command brings the installed programs up to their newest version. It never installs a new program, and it answers "All packages are up to date." when there is nothing to do.

On Frames, the pckmgr command also has the verbs remove, search and list. The list verb with --available shows every program the Mirror offers.

Some programs are services that run on a Mainframe. When you install one there, the service switches on, and the Mainframe keeps a record of its version. Some programs also need an IQL engine on the network ([](jsc:engines)).

You can publish your own packages to the Mirror, for other players to install, with the Sigma package tool ([](jsc:sigma_publish_mirror)).

The programs of the other chapters have their own pages: the Crafting Manager ([](jsc:crafting_manager)), the Pattern Studio ([](jsc:pattern_studio)) and the Workshop ([](jsc:workshop)).

The desktop programs follow on the next pages: the Network window, This PC, Files, the Editor, Settings, the System Monitor, the Task Manager, a Calculator, Paint, Exceed, and the games. The books of the series, one for each family of systems, go deeper into install and use ([](jsc:library)).

## Updates

When the Mirror has a newer version of a program you installed, Settings marks it on its Programs page, on every desktop. Under the program's name you read update available, then the new version and the one you have, and its Update button installs the new version. At the top, the page says how many updates the Mirror has. A package another player published again under the same name is an update too.

Each package manager below also lists the updates, with its own command, and installs nothing while it lists them. On Frames and on UNIX System V, you see the updates in Settings only. On OpenBSD, syspatch -c lists the patches of the system itself ([](jsc:openbsd_patches)).

Table: The command that lists the updates

| Manager | Command |
| --- | --- |
| `apt` | `apt list --upgradable` |
| `dnf` | `dnf check-update` |
| `pacman` | `pacman -Qu` |
| `emerge` | `emerge -pvuD @world` |
| `pkg` | `pkg version -vRL=` |
| `pkg_add` | `pkg_add -un` |
| `keg` | `keg outdated` |

## If something goes wrong

> [!PROBLEM] The install says that the program needs its install disc in a linked drive.
> Put the disc of that program in a linked drive of the same computer, then install again.

> [!PROBLEM] The install says that the program needs a later era of hardware.
> The computer's hardware is older than the program allows. Install it on a newer computer.

> [!PROBLEM] The install says that the program only installs on one kind of computer, such as the Mainframe.
> Install it on the kind of computer the message names.

> [!PROBLEM] The install says unmet requirements (hardware or free disk space).
> The computer lacks a processor or a graphics card the program needs, or it has too little free disk space. Give it what is missing, then install again.

> [!PROBLEM] The install says could not resolve mirror://.
> Connect the computer to a network whose Mainframe runs the Mirror ([](jsc:mirror)).

> [!PROBLEM] The install says this system installs programs from install media, not a package manager.
> Put the program's install medium in a linked drive, and install from it.

> [!PROBLEM] The install says unable to locate package, followed by a name.
> The Mirror does not offer that name for your system. Check the name in the store list, then install again.

> [!PROBLEM] The install says this computer is still setting up a program.
> Wait for that setup to end before you start another install.

> [!PROBLEM] The install says the installation could not be written to the disk.
> There is no space on the disk, or there is no disk. Free some space, or put a disk in the computer, then install again.

> [!PROBLEM] The install says a program is already the newest version.
> Nothing needs doing: the program is installed and up to date.

> [!SEE] jsc:installing, jsc:the_systems, jsc:drives, jsc:mirror
