---
section: desktop_apps
order: 81
icon: jsc:monitor
---
# The System Monitor and the Task Manager

The System Monitor and the Task Manager show what the machine is doing. The first only reads: it shows the load on the processor, the memory, the graphics and the storage. The second lists what is running, and it lets you end a program.

## The System Monitor

The System Monitor is on the desktop systems. It reads the machine as it works, and it changes as the load changes.

Table: Parts of the System Monitor

| Part | What it shows |
| --- | --- |
| Processor | The load on the processor. |
| Memory | The megabytes held and the total, then the megabytes that are free, and the swap with what of it is used. |
| Graphics | The video memory in use and the load on the card. Without a card it says no GPU. |
| Storage | How many programs are installed, and each disk, with the system's disk marked (system). |
| Power and Temperature | What the parts draw and the line gives, and the processor's heat, each over a small graph of the period its menu picks. |

## The Task Manager

Right-click the panel and open the Task Manager from its menu. It is not on the Start menu or the desktop. Each desktop has its own manager, so what you see depends on your system.

Table: The Task Manager by desktop

| Desktop | Its manager |
| --- | --- |
| Frames 95 | A Close Program box. |
| Frames XP | Five tabs, with a menu and a status bar. |
| Frames 11 | A rail at the side and a wide table. |
| Linux desktops | Each desktop's own system monitor. |

On Frames XP the tabs are Applications, Processes, Performance, Networking and Power. The Performance tab shows the use of the processor and the memory, as numbers and as a history, and the graphics card when there is one. The Networking tab shows whether the machine is Connected or Not connected, and the link activity over the last minute. The Power tab shows the draw, the line and the temperature with their histories ([](jsc:task_manager)).

The Performance tab of Frames XP also shows PF Usage, what the programs keep in the page file, and the status bar the Commit: what is promised, against the memory and the page file together. On Frames 10 and 11 the Memory page shows the same, and each program with part of it in the page file, and how fast that part runs ([](jsc:swap)).

Table: Buttons of the Task Manager

| Button | What it does |
| --- | --- |
| `End Task` | Ends the selected window or program. |
| `End Process` | Ends the selected program. |
| `Switch To` | Brings the selected window to the front. |

Next to the buttons the window warns: "Ending a program may lose unsaved work." and "Ending the system restarts the machine." A system, a desktop or a service cannot be ended from here.

1. Right-click the panel, and open the Task Manager.
2. Select the window or the program in the list.
3. If the program still lets you, save your work in it first.
4. Press End Task for a window, or End Process for a program.

## If something goes wrong

> [!PROBLEM] End Task stays greyed out.
> Select a window or a program's row first.

> [!PROBLEM] The row you want to close is the system, a desktop or a service.
> The machine itself cannot be ended from here. Close the program that owns that row instead.

> [!SEE] jsc:programs, jsc:help_on_a_computer, jsc:swap
