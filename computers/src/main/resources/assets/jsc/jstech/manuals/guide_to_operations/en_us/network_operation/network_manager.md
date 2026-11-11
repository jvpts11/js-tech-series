---
section: network_operation
order: 104
icon: jsc:monitor
---
# The Network Manager

The Network Manager is the control room of the network. It is a desktop program, and only the Mainframe runs it: you open it on the Mainframe, and no other computer can.

Its seven tabs show the network from seven sides. Each one is a way of asking the network what it holds, what it is doing, and how well it is doing it.

Table: The seven tabs

| Tab | What it shows |
| --- | --- |
| Devices | Every node the network knows, with its type, whether it is online or offline, and how long it has been up. |
| Processes | The Operations in flight, one line each. A click opens one, with its buttons Close and Cancel. |
| Hardware | The orchestration capacity, the parallel queues, the RAM buffer and the network storage, and how many computers of each kind there are. |
| Map | The nodes and their lines, drawn as a map. Drag a node to move it. |
| Log | Two views along its top. Operations: the Operations logged, filtered by kind, by who asked and by period. Events: what happened to every machine on the network, newest first. |
| Stats | How many Operations of each kind settled in the last hour, day or 7 days, how long they waited and ran, the day's peak, and the capacity used. |
| Services | The engines and services of the Mainframe, and the steps to replace an engine ([](jsc:mainframe_services)). |

On the Map, drag a node with the mouse to move it, drag with the middle button to pan, and turn the wheel to zoom. The legend lists the lines in use: access, backbone, long distance and high compute.

The Processes tab lists the work the network is doing right now. Each Operation has its own priority and its own record of how much it has moved ([](jsc:what_operations_are)).

## The network's events

The Log tab's Events view is the network's event log, apart from its Operations. Each line has the time, a level, the machine and what happened. INFO is something that happened, such as a machine switched on or off. WARN is something to look at, such as a world event expected or under way. ERROR is something that went wrong, such as two Mainframes on one network.

Three lists over it filter what it shows: the level, the machine (or the network itself) and how far back, from the last hour to everything it keeps. Print sends what it shows to the computer's printer. Export writes it to a text file on a disk of the Mainframe, one event a line.

The network keeps its last seven days, and at most 2,000 events. Older ones make way for new ones.

## The Operations and the statistics

The Log tab's Operations view lists the Operations the Mainframe logged, with the time each settled, its id, what it was, its status and who asked for it. Three lists over it filter what it shows: the kind (with CRAFT, it is the history of your crafts), who asked, and how far back. Print sends what it shows to the computer's printer.

The Stats tab covers the period you pick: the last hour, the last day or the last 7 days of the game. Each kind of Operation has its count per hour, how long it waited and ran on average, and how many failed. Under the table are the most Operations that ran at once today, with the time of day, and a graph of the orchestration capacity used over the last 2 hours. The Devices tab gives each machine's uptime in days and hours of the game.

> [!DEFINE] Node
> Any block the network knows: a Mainframe, a server, a Subframe or a computer.

> [!DEFINE] Service
> A program installed on a server, or on the Mainframe, that gives the machine its job. You choose the services of each machine.

## If something goes wrong

> [!PROBLEM] The Network Manager is not in your programs.
> It runs only on the Mainframe. Open the Mainframe, not a Personal Computer.

## If something goes wrong

> [!PROBLEM] A node shows as offline in the Devices tab.
> Switch the node on, and check that its cable run reaches the Mainframe ([](jsc:data_cables)).
