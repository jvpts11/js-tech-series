---
section: sigma_start
order: 406
---
# Installing the compilers and the runtime

Sigma and Sigma# do not come with the computer. You install three packages: the Σ Compiler (scc), the Σ# Compiler (sgsc) and the Sigma Runtime (sigma). A package is installed by its name, the way every program is ([](jsc:programs)).

> [!DEFINE] Σ Compiler
> The package scc, which turns your Sigma source into a listing.

> [!DEFINE] Σ# Compiler
> The package sgsc, which turns your Sigma# source into a listing.

> [!DEFINE] Sigma Runtime
> The package sigma, which runs your listings on the computer.

The Sigma compiler is the smallest of the three. It is the only one a Vintage computer can hold, because the Sigma# compiler and the runtime need a Legacy computer or later.

Table: The three packages

| Package | Name | Earliest era | Disk and memory |
| --- | --- | --- | --- |
| `scc` | Σ Compiler | Vintage | 4 MB of disk, 2 MB of memory while it runs |
| `sgsc` | Σ# Compiler | Legacy | 8 MB of disk, 16 MB of memory while it runs |
| `sigma` | Sigma Runtime | Legacy | 12 MB of disk, 24 MB of memory while it runs |

On Frames, the edition matters too. Frames 95 can install the Sigma compiler only: the Sigma# compiler and the runtime need Frames XP or later.

The packages come from the Mirror, the network's package repository. It is a service on a Standard Mainframe, so your computer must be on a network whose Mainframe runs the Mirror ([](jsc:mirror)).

Table: Installing from the Mirror

| System | Command to type |
| --- | --- |
| Frames | `pckmgr install <name>` |
| Debian and Ubuntu | `apt install <name>` |
| Fedora | `dnf install <name>` |
| Arch | `pacman -S <name>` |
| Gentoo, which builds each package from source | `emerge <name>` |
| FreeBSD | `pkg install <name>` |

The name is the package's own name, such as scc or sgsc. On MC-DOS, MC-NET and UNIX System V, programs install from install media only, not from the Mirror ([](jsc:programs)).

## Installing on Frames

1. Open the Command Prompt.
2. Type pckmgr install scc and press [[Enter]].
3. Type pckmgr install sgsc and press [[Enter]].
4. Type pckmgr install sigma and press [[Enter]].

Once a package is installed, its command appears at the prompt. The prompt is the line where you type a command, and the system answers there. Each command word is a verb: a word the system knows, such as install or run.

> [!DEFINE] prompt
> The line where you type a command, and where the system answers it.

> [!DEFINE] verb
> A command word the system knows, such as install or run.

Until a package is installed, its verbs do not exist at the prompt. The runtime adds the sigma verb, and the packaging tool sgpack comes with it ([](jsc:sigma_sharing)).

## If something goes wrong

> [!PROBLEM] "sgsc needs Legacy hardware or later" appears.
> Install the Sigma compiler, scc, on this Vintage computer. Or use a Legacy computer or later.

> [!PROBLEM] "unable to locate package sgsc" appears.
> The Mirror does not offer the package to this computer. Check that a Mainframe runs the Mirror. On Frames, search for the name with pckmgr search.

> [!PROBLEM] "could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service" appears.
> Connect this computer to a network whose Mainframe runs the Mirror service.

> [!PROBLEM] "this system installs programs from install media, not a package manager" appears.
> Use Frames, Linux or FreeBSD for this package, since they have a package manager.

> [!PROBLEM] "unmet requirements (hardware or free disk space)" appears.
> The computer lacks the era or the free disk space the package needs. Free some disk space, or use a computer of a newer era.

> [!PROBLEM] "this verb needs a package name" appears after pckmgr install.
> Type the name of the package after install, for example pckmgr install scc.

> [!SEE] jsc:programs, jsc:mirror
