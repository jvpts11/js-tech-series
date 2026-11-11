---
section: midsoft_installs
order: 201
icon: jsc:cd_rom
---
# The Midsoft Boot Manager

The Midsoft Boot Manager is the list that a Frames machine shows when it starts. It shows up only when there is a choice, which means two or more entries. With one system, the machine starts that system at once.

The list names each system with its plain name. The other systems also show the number of their disk in brackets. The system the computer is running is the default: if you press nothing, the countdown ends and that system starts.

Table: The keys in the list

| Key | What it does |
| --- | --- |
| `Up` | Moves the cursor up the list. |
| `Down` | Moves the cursor down the list. |
| `Enter` | Starts the system under the cursor. |
| `Esc` | Leaves the monitor, and the list keeps its place for the next opening. |

The countdown lasts 5 seconds, which is 100 ticks ([](jsc:units_and_ticks)). The first key you press stops the countdown, and the list then waits for you. Any key counts, not only the arrows and Enter.

> [!DEFINE] Tick
> One beat of the game: 20 ticks make a second.

On Frames 10 and 11 the list ends with "Change firmware settings", which opens the firmware ([](jsc:firmware)). On Frames 95, XP and 7 the list has no such entry. The number keys do not choose from this list; the arrows do, and the list offers no restart.

## If something goes wrong

> [!PROBLEM] The screen says "kickmgr is missing", and the computer will not start that system.
> The manager's file is gone from the system's folder, Frames/kickmgr.sys. Install the system again on the same disk to repair it ([](jsc:two_systems)).

> [!PROBLEM] The countdown starts the system you did not want.
> Press a key early in the countdown to stop it, then choose with the arrows and Enter.

> [!PROBLEM] The list does not appear, although two systems are installed.
> The server's boot menu setting is off. Turn it on, with the setting boot.show_boot_menu.

> [!SEE] jsc:two_systems, jsc:firmware, jsc:units_and_ticks
