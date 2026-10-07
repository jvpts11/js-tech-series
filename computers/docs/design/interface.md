# Interface

Where every part of the interface lives. There are two places:

- **The assembly window**, which opens when the computer's block is used: the parts and the machine's physical state.
- **The monitor**, where the operating system runs ([Operating systems](operating-systems.md)): the firmware, the
  desktop and its programs, or MC-NET's full screen. Everything the network shows and does lives here, in **programs**:
  the Network Interactor, the Network Manager, each system's Task Manager, the Craft Planner, the Crafting Manager, the
  Storage Insights, the ISMS, Remote Control ([Programs](programs.md)).

On MC-NET the screen is the Interactor itself, with a rail of **headers** that changes with the machine: Local, Storage,
Network, Craft (if the network has Crafting Computers), Patterns (if the machine can learn patterns), Ops, Tasks and
Upkeep (only on the Mainframe), Processes and Console (always).

## The local computer

**The assembly window** (every computer): the parts, the draw and the power (DRAW / POWER), the name (changed here, not
on an anvil), the state (ONLINE, OFFLINE, CONFLICT, READY, STANDBY), the power button and automatic start (AUTO). On the
Mainframe also failover (FAIL ON/OFF) and the queues. On the Crafting Computer, each card's memory (x/y).

**The Local header** (in the Interactor): blocks with the capacity (items per tick), the queues and the memory buffer;
bars for processors, memory, graphics cards and disks (installed / slots) and for storage used / capacity; a "The
network" panel with the state (JOINED, TWO ORCHESTRATORS, NOT ON ONE), how many servers, computers and Subframes, what
is stored and the room, and the Operations under way.

**The Storage header:** the public/private sliders per disk (Personal Computer, [Storage](storage.md)).

## The network: the Network Interactor

The **Network Interactor** is the "Network" program of every desktop and MC-NET's full screen. Its tabs are Status,
Local, Network, Crafting, Ops and Favourites.

- A grid of the whole network's items, navigable by keyboard; multiple selection (Ctrl, Shift); double-click opens.
- Filters by mod and by category; favourites (a star).
- **Request:** an amount and a **priority** (LOW to HIGH).
- **Deposit:** the window shows the player's real inventory (real slots); shift-click inserts; handing over by hand
  follows the single rule ([Storage](storage.md)).
- **An item's details:** where it is stored (server by server), the recipes that make it (MADE BY) and what uses it
  (USED IN); the chosen recipe is remembered; what is missing shows in red.
- A status bar with the network's storage gauge.
- The window changes shape by its handles; Print prints the list ([Peripherals](peripherals.md)).

The terminal's Network header shows the same grid with a detail panel, the same answer the prompt gives. `interac` is
the Interactor on the command line, full screen with F keys.

## Crafting

**The Craft header:** the catalogue on the left; what is running and what just finished on the right.

**The craft dialog:** the amount in steps, the plan, the estimate in seconds (or "no estimate"), the Craft and Partial
buttons (Partial amber only when it helps, [Autocrafting](autocrafting.md)) and the priority; an item with several
recipes shows them side by side.

**The Craft Planner** (a program) shows the whole plan before crafting ([Autocrafting](autocrafting.md)).

## Operations

**The Ops header:** the log of the network's recent Operations, with the provenance panel (who asked, from where). An
Operation's **detail** shows its SubOperations and sources (SOURCES), its stages (STAGES), "waited X, ran Y" and its
priority. An Operation under way can be **cancelled** (also `cancel <id>` at the prompt).

## Programs and the local Task Manager

Programs are real software installed on disk ([Programs](programs.md)), run by the system. Every desktop has the **Task
Manager** of its style (Frames 95's close program box, Frames XP's tabs, Frames 11's rail, the Linux desktops' System
Monitor), opened from the panel's menu. All of them read the same memory ledger ([Hardware](hardware.md)), the disks,
the processor, the video memory and the link, and end programs and Σ# processes (End Task). The **System Monitor** shows
the parts and bars of use. The **Processes header** is the task manager of the current machine (the engine's service,
the jobs, Start, Stop, Restart). Settings > Programs lists the installed programs.

Programs take no capacity from Operations: they weigh on memory and on the processor's instruction budget
([Σ and Σ#](sigma.md)).

## The whole network: the Network Manager

The **Network Manager** (by JSC) is the program of the network's control room, and only runs on the Mainframe. Seven
tabs:

- **Devices:** every machine on the network, with the LINK column (the cable line and the speed of its link, the square
  of an Optical Network Card, "no link" with the reason).
- **Processes:** the live Operations and the craft slots x/y; changing the priority and cancelling.
- **Hardware:** the orchestration capacity, the parallel queues, the memory buffer, the network's storage, how many
  Mainframes, servers, Subframes, supercomputers, Crafting Computers, Personal Computers and Cluster Managers, the fibre
  links, the backbone and the slowest link.
- **Map:** the network's topology, draggable and zoomable, every link drawn by its cable line.
- **Log** and **Stats** (below).
- **Services:** the network engine and the network's software (Subframes, the Automation Engine, the Mirror).

On the Mainframe's terminal, the **Tasks** header does the same (Processes, Hardware, Devices) and **Upkeep** has the
index's upkeep (ANALYZE, VACUUM, REINDEX, DROP) with its health view ([Operations](operations.md)). To use another
machine from afar: the Remote Control program and `ssh` ([Servers and racks](servers-and-racks.md)).

Who sees other players' Operations and every machine's hardware, and who cancels, will be decided in the directory
([Security](security.md)); without it, everything is allowed.

### The log

The Network Manager's Log tab is the feed of **Operations**, the most recent first; each opens its detail; Print sends
the table to the computer's printer. The Crafting Log records the craft events (COMPLETED, PARTIAL, FAILED, DRAINING).

### The statistics

The Network Manager's Stats tab covers the **last hour**, per Operation type: Operations per hour, average wait, average
run and failures (also the `stats` command). It also shows the **peak** of simultaneous Operations of the last game day
(`Mainframe.PeakToday` in Σ#).

The **Storage Insights** (a Vaultis program) is the storage dashboard: the totals, the biggest types in bars (with
search and favourites), the types under a threshold, how full each server is, and each type's detail (where it is, what
makes it, which buses filter it).

## To build

- **The Local header shows the draw** (the parts' watts and the FE per tick taken from the grid, [Power](power.md)) and
  the machine's **id**, which tells it apart from machines with the same name (each machine's detail in the Network
  Manager shows it too). Temperature comes with [Temperature](temperature.md).
- **Graphs:** the Task Manager and the System Monitor show the **processor's use**, the share of the instruction credits
  per tick ([Σ and Σ#](sigma.md)) that programs spend, in a graph like a real system's performance tab, and the
  **energy** one (watts and FE per tick); temperature's comes with temperature.
- **The shopping list:** the Craft Planner's Print prints the plan, with what is missing highlighted. The **history** of
  crafts is the Operations log, filtered by CRAFT in the Network Manager.
- **The network's event log** in the Log tab, with levels INFO, WARN and ERROR per machine, filters by level, machine
  and period, Print, and export to a file on a disk ([Operations](operations.md)).
- **Longer statistics:**
  - windows of the last hour, the last game day and the last 7 game days, kept as counters per interval (O(1));
  - each server's **uptime** (the time since its last boot), in the Devices tab;
  - **the network's graph:** the use of the orchestration capacity over the last 2 hours. Each machine's processor graph
    is in the Task Manager; temperature's comes with temperature.
- **Mining:** the Mining Panel program and the Crypto Wallet ([Teracoin](teracoin.md)).
- **Portable devices** have no tabs: they run their mobile system and its apps; the network app is a simpler Network
  Interactor ([Wireless](wireless.md)). The **notifications** are in [Wireless](wireless.md) too.
