---
section: frames_machine_tools
order: 223
---
# The Task Manager

The Task Manager shows what the machine is running and what each thing uses: the windows open, the processes, the processor (CPU) and the memory, and the disks. You use it to end a program that will not close.

Open it from the taskbar ([](jsc:taskbar)): right-click the taskbar and choose Task Manager. It is kept off the desktop and off the Start menu.

## Its look on each edition

Table: The Task Manager on each edition

| Edition | Look | Pages |
| --- | --- | --- |
| Frames 95 | A Close Program box, with two warnings | None: the box has no pages |
| Frames XP and Frames 7 | A window with a menu bar and a status bar | Applications, Processes, Performance, Networking and Power |
| Frames 10 and Frames 11 | A window with graphs | Processes, Performance, Services, Storage, Network, the graphics processor (GPU) and Power |

## Ending a program or a process

1. Click a row that is a window or a process.
2. Press End Task. In the process list the button reads End Process.

End Task is unavailable when the selected row is neither a window nor a process. Ending a window closes that program. Ending a process stops the script or service it names.

## The process list

Table: Columns of the process list

| Column | What it shows |
| --- | --- |
| Image Name or Process Name | The name of the program or the process. |
| Mem Usage or Memory | How much memory the row uses. |
| CPU | How much of the processor the row uses. |
| Kind | What sort of row it is: system, desktop, service, program, script or graphics. |

## Performance and networking

The Performance page shows CPU Usage and Memory Usage as figures and as histories. The graphics part shows video memory, with its dedicated and shared parts. A computer with no graphics says "This computer has no graphics."

The Networking page says whether the machine is Connected or Not connected, and shows the link activity over the last minute.

> [!DEFINE] Process
> A running script or service on the machine.

The Task Manager does not show the Operations of a network. You watch those in the Network Manager ([](jsc:network_manager)). The eight states an Operation can be in are on their own page ([](jsc:operation_states)).

The Power page shows what the machine's parts draw against its power supply's rating, what it takes from the line in watts and in FE a tick, and its processor's temperature, each with its history over the last hour, the last day or the last 7 days of the game, and the energy it took from the line today. On Frames XP and Frames 7, click the status bar to move to the next period; on Frames 10 and Frames 11, pick it with the buttons on the page.

## If something goes wrong

> [!PROBLEM] End Task is unavailable.
> Click a row that is a window or a process before you press it.

> [!PROBLEM] The Close Program box says "Ending a program may lose unsaved work."
> Save your work in that program first, then end it.

> [!PROBLEM] The Close Program box says "Ending the system restarts the machine."
> Choose Cancel, unless you want the machine to restart.

> [!PROBLEM] You look for a network job here and do not find it.
> Open the Network Manager ([](jsc:network_manager)).

> [!SEE] jsc:taskbar, jsc:network_manager
