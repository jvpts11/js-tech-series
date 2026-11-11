---
section: freebsd_software
order: 337
---
# The ports tree: portsnap

FreeBSD also builds programs from source, on your own computer. The recipes for them sit in the ports tree, a folder on the system disk. You fetch the tree with portsnap, and the Mirror serves it as one copy.

> [!DEFINE] Ports tree
> The folder of recipes, one per program, that builds programs on this computer.

> [!DEFINE] Port
> The recipe for one program in the ports tree.

> [!DEFINE] Snapshot
> The copy of the ports tree that the Mirror serves, fetched as one unit.

The tree is real files. Each port has a folder at /usr/ports/<category>/<name>/ with three files. Makefile holds the recipe, distinfo holds the checksum and size of its source, and pkg-descr holds the one-line description.

## The words of portsnap

Table: The words of portsnap

| Word | What it does |
| --- | --- |
| `fetch` | Brings the snapshot from the Mirror without laying out a tree. |
| `extract` | Lays out the tree afresh from the snapshot, replacing any tree there. |
| `update` | Brings a tree that is laid out already up to date. |
| `auto` | Fetches, then extracts or updates, whichever the computer needs. |

```command
id: jsc:command/freebsd/portsnap
```

1. The first time, type portsnap fetch extract.
2. Afterwards, type portsnap fetch update.

The words combine in the order you type them. While it runs, portsnap prints its progress, and its last line gives the number of ports and the disk they take.

> [!WARNING] portsnap extract replaces the whole ports tree, and the build folders inside it go too.

Table: Where some programs sit in the tree

| Program | Category |
| --- | --- |
| screenfetch | `sysutils` |
| emacs | `editors` |
| minesweeper, solitaire, snake | `games` |
| scc, sgsc, sigma | `lang` |
| knot, knothub | `devel` |
| kde_plasma | `x11` |
| gnome, cinnamon | `x11` |
| cde | `x11` |

## Messages of portsnap

```table
{"caption": "Messages of portsnap", "heads": ["Message", "What it means"], "rows": [[{"literal": "portsnap: this computer has no system disk"}, {"words": "The computer has no disk to hold the tree."}], [{"literal": "Looking up the Mirror for the ports tree... none found."}, {"words": "fetch found no Mirror, so no snapshot comes; it then prints No mirrors remaining, giving up."}], [{"literal": "No snapshot available.  Try running portsnap fetch"}, {"words": "extract and update need a snapshot first."}], [{"literal": "/usr/ports was not created by portsnap."}, {"words": "update works only on a tree that portsnap laid out, so run portsnap extract once."}], [{"literal": "portsnap: No space left on device"}, {"words": "The disk is full."}]]}
```

## If something goes wrong

> [!PROBLEM] portsnap says none found for the Mirror.
> Connect the network to a Mainframe that runs the Mirror service.

> [!PROBLEM] portsnap says there is no snapshot available.
> Type portsnap fetch first, then extract or update.

> [!PROBLEM] portsnap says /usr/ports was not created by portsnap.
> Type portsnap extract once. Afterwards, update works.

> [!PROBLEM] portsnap says there is no space left on the device.
> Free some disk space.

> [!SEE] jsc:ports_build, jsc:pkg, jsc:disks
