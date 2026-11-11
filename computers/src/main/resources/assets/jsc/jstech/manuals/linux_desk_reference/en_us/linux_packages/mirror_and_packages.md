---
section: linux_packages
order: 293
---
# The Mirror and the packages on it

The Mirror is the network's package server. It holds the programs and desktops that apt and dnf fetch. It runs as a service on the network's Mainframe ([](jsc:mainframes)). A Linux computer reaches it through a data cable to that Mainframe. The Guide to Operations has its own page on the Mirror ([](jsc:mirror)).

## Putting the Mirror on the Mainframe

1. Stand at the Mainframe's console.
2. Type mirror install, then press ENTER.
3. Read the reply: "Mirror installed on the Mainframe and serving packages".

Type mirror status at any time to see whether the Mirror runs. If you see "could not resolve mirror://", check it first.

Table: The Mirror's status line

| Status line | What it means |
| --- | --- |
| `Mirror: not installed` | The Mirror is not on this Mainframe yet. |
| `Mirror: serving` | The Mirror runs and answers the computers of the network. |
| `Mirror: installed (Mainframe off)` | The Mirror is installed, but the Mainframe is off. |

When the Mainframe is serving, the boot prints "Found package mirror on" and the Mainframe's name.

## What there is to install

The table lists the programs that apt and dnf offer on Linux. For each one it gives the disk it takes, the memory it holds while it runs, and the oldest era that can run it. The name in the first column is the one you type after install. Programs that come with every system, such as the Files program and the settings, are not in the table.

Table: Programs the Mirror offers to Linux

| Command | Program | Disk and RAM | Minimum era |
| --- | --- | --- | --- |
| `kde-plasma` | KDE Plasma desktop | 256 MB disk, 224 MB RAM | Legacy |
| `gnome` | GNOME desktop | 192 MB disk, 256 MB RAM | Legacy |
| `cinnamon` | Cinnamon desktop | 160 MB disk, 160 MB RAM | Standard |
| `vim` | Vim editor | 8 MB disk, 4 MB RAM | Vintage |
| `emacs` | Emacs editor | 24 MB disk, 12 MB RAM | Vintage |
| `screenfetch` | screenfetch, the system's identity with its logo | 4 MB disk, 1 MB RAM | Vintage |
| `sgsc` | Sigma# compiler | 8 MB disk, 16 MB RAM | Legacy |
| `scc` | Sigma compiler | 4 MB disk, 2 MB RAM | Vintage |
| `sigma` | Sigma runtime, a background service | 12 MB disk, 24 MB RAM | Legacy |
| `mirror` | Mirror service, Mainframe only | 64 MB disk, 32 MB RAM | Vintage |
| `mines` | Minesweeper | 16 MB disk, 1 MB RAM | Vintage |
| `solitaire` | Solitaire | 16 MB disk, 1 MB RAM | Vintage |
| `snake` | Snake | 8 MB disk, 1 MB RAM | Vintage |
| `ark` | 67ark, the archiver | 24 MB disk, 16 MB RAM | Vintage |
| `paint` | Paint | 48 MB disk, 32 MB RAM | Vintage |
| `soundfoundry` | Soundfoundry, for music | 4 MB disk, 16 MB RAM | Legacy |
| `exceed` | Exceed, the sheet of cells | 64 MB disk, 64 MB RAM | Vintage |
| `messenger` | Midsoft Messenger | 48 MB disk, 32 MB RAM | Vintage |
| `knot` | Knot, the repository tool | 48 MB disk, 32 MB RAM | Vintage |
| `insights` | Storage Insights | 64 MB disk, 64 MB RAM | Vintage |
| `planner` | Craft Planner | 64 MB disk, 64 MB RAM | Vintage |
| `autoeng` | Automation Engine, a service | 32 MB disk, 64 MB RAM | Vintage |
| `msgsvc` | Messenger Service, a service | 48 MB disk, 8 MB RAM | Vintage |
| `iqlengine` | IQL engine, Mainframe only | 32 MB disk, 24 MB RAM | Vintage |
| `nextgreiql` | NextgreIQL engine, Mainframe only | 32 MB disk, 48 MB RAM | Legacy |
| `youriql` | Prophet YourIQL engine, Mainframe only | 32 MB disk, 32 MB RAM | Legacy |

The numbers are the game's own, not exact requirements. A program installs only on a computer that has the disk and the hardware for it. The install time is the program's size divided by 8 MB a second, multiplied by the era's factor: 1 on Vintage, 2 on Legacy, 3 on Transition, 4 on Standard, 8 on Advanced, 16 on Exa and 32 on Singularity. The time stays between 3 and 90 seconds.

Removing a program takes a fifth of the time that installing takes. Programs that players publish to the Mirror install the same way. A player's program written in Sigma# needs the Sigma# runtime first: apt install sigma. Publishing is described in [](jsc:sigma_publish_mirror).

## If something goes wrong

> [!PROBLEM] E: could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service
> Connect the computer's data cable to the network's Mainframe. If the Mirror is not installed yet, type mirror install at the Mainframe's console.

> [!PROBLEM] the Mirror is full (64 packages)
> The Mirror holds at most 64 packages that players publish, so one must come off before another can go on ([](jsc:sigma_unpublish)).

> [!PROBLEM] this computer cannot store installed programs
> This computer has no store for installed programs. Install the program on a computer with a disk.

> [!SEE] jsc:apt, jsc:dnf, jsc:mirror, jsc:mainframes
