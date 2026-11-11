---
section: prompt_programs_and_network
order: 244
---
# Installing and removing programs

A program reaches the computer in two ways: from a medium, such as a disc in a linked drive ([](jsc:installing)), or from the Mirror, the network's package service. programs lists the programs installed here. store lists the ones you can install, install and uninstall change them, and on the Frames editions pckmgr fetches packages from the Mirror.

> [!DEFINE] Mirror
> The network's package service, which offers programs that you can install.

> [!DEFINE] Package
> A program that the Mirror offers, ready to fetch and set up.

Table: Verbs for programs

| Verb | Words after it, and what it does |
| --- | --- |
| `programs (apps)` | No words. Lists the programs installed on this computer, each with its id. |
| `install` | A program's id. Installs the program from its medium. |
| `store (available)` | No words. Lists the programs that you can install on this computer. |
| `uninstall` | A program's id. Removes an installed program. |
| `pckmgr` | install, remove, search, list or update, and a name. Works from the Mirror, on the Frames editions only. |

## Installing from a medium

1. Type programs to see the programs already installed.
2. Type store to see the programs that you can install.
3. Put the program's disc in a linked drive, if the program needs one.
4. Type install and the program's id.
5. Wait for the lines that say Setting up. The last line says Installation complete. Rebooting into the new system.

When a program needs a disc that is not in a linked drive, install says that the program needs its install disc in a linked drive.

```command
id: jsc:command/mc_dos/programs
```

```command
id: jsc:command/mc_dos/install
```

```command
id: jsc:command/mc_dos/store
```

```command
id: jsc:command/mc_dos/uninstall
```

## pckmgr on the Frames editions

pckmgr fetches packages from the Mirror on the Frames editions. MC-NET calls the same manager netgetter. MC-DOS has no pckmgr: it installs programs from media only.

Table: Verbs of pckmgr

| Verb | What it does |
| --- | --- |
| `install NAME` | Fetches and sets up a package. |
| `remove NAME` | Uninstalls a package. |
| `search [text]` | Finds the packages that the Mirror offers. With no text, it lists what is offered. |
| `list [--available]` | Lists the installed packages, or everything on offer with --available. |
| `update` | Brings the installed packages to the current build. |

```command
id: jsc:command/frames/pckmgr
```

## screenfetch on the Frames editions

screenfetch is a small program for the Frames editions. Once you install it, typing screenfetch at the Command Prompt prints the logo of Frames, with a summary of your machine beside it: the system, its kernel, its uptime, its packages and its shell.

```command
id: jsc:command/frames/screenfetch
```

## If something goes wrong

> [!PROBLEM] no such program: NAME
> Type programs or store to see the ids, and check the spelling.

> [!PROBLEM] NAME needs its install disc in a linked drive
> Link a drive and put the disc in it ([](jsc:drives)).

> [!PROBLEM] could not resolve mirror:// - no package source reachable
> The computer is not on a network with a Mirror. Connect it to one ([](jsc:mirror)).

> [!PROBLEM] No package matches NAME.
> Type search with no text to list the packages the Mirror offers.

> [!PROBLEM] this verb needs a package name
> Type a package name after install or remove.

> [!PROBLEM] The installation could not be written to the disk (no space or no disk).
> Free some space on the disk, or use a larger disk.

> [!SEE] jsc:programs, jsc:mirror
