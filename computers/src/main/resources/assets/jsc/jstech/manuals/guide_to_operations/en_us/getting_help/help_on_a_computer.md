---
section: getting_help
order: 93
icon: jsc:guide_to_operations
---
# Help on a computer

Every desktop has a Help program, under a name of its own. It ships with the desktop, so you do not install it. It reads the manuals of the series, and beside them the manual pages of the machine's own commands. This manual is not only in your hands: every computer can read it too.

Table: Help on each desktop

| Desktop | Its Help is called |
| --- | --- |
| Frames 95 | Help Topics |
| Frames XP and 7 | Help and Support |
| Frames 10 and 11 | Get Help |
| KDE | Help Center |
| GNOME and Cinnamon | Help |
| CDE | Help Viewer |

At a prompt, several commands give you help. Each one works on a different set of systems.

The help command lists the commands, or shows how one of them is used. Type help alone to list them, or help and a command's name to see its use. It works on every system except FreeBSD. On MC-DOS and MC-NET it takes the whole screen. On the other systems it prints its text ([](jsc:help_command)).

The man command opens the manual page of a command, on Linux, UNIX System V and FreeBSD. A topic such as graphics-cards opens as a page of section 7 ([](jsc:man_pages)). The whatis and apropos commands search the manual ([](jsc:whatis_and_apropos)).

On Linux the info command reads the manuals as a tree of nodes. Its keys are these:

Table: Keys of info

| Key | What it does |
| --- | --- |
| `n` | Goes to the next node. |
| `p` | Goes to the previous node. |
| `u` | Goes up to the node above. |
| `l` | Goes back to the node you were at. |
| `Tab` | Picks a link. |
| `Enter` | Follows the picked link. |
| `m` | Goes to a menu item, by its name. |
| `s` | Searches the manuals. |
| `h` | Shows the keys. |
| `q` | Leaves info. |

With no name, info opens the directory of every manual.

## If something goes wrong

> [!PROBLEM] man says No manual entry for a name.
> Check the name. For the entry on graphics cards, type man graphics-cards, not man graphics.

> [!PROBLEM] help does not work on FreeBSD.
> Use man on FreeBSD instead ([](jsc:man_pages)).

> [!PROBLEM] info does not work on your system.
> info is for Linux only. Use man instead ([](jsc:man_pages)).

> [!SEE] jsc:library, jsc:finding_help_overview, jsc:help_command, jsc:info_keys
