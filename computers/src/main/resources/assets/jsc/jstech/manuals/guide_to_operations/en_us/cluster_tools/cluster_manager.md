---
section: cluster_tools
order: 155
icon: jsc:cluster_management_computer
---
# The Cluster Manager

The Cluster Manager is the window that lets you run many machines as one. It is a desktop program for a Cluster Management Computer ([](jsc:cluster_management_computers)). It needs a Cluster Interface Card in that computer ([](jsc:expansion_cards)).

Each machine in a cluster is a node. Which clusters you reach depends on the card's reach.

> [!DEFINE] Cluster
> A group of machines that the Cluster Manager runs as one unit: a supercomputer, or one section of a datacenter.

> [!DEFINE] Node
> One machine in a cluster, which the Cluster Manager can power, and install a system or a program on.

> [!DEFINE] Reach
> What a Cluster Interface Card can drive: the datacenter servers, supercomputers and artificial intelligence (AI) clusters, depending on the card.

The window runs on Frames, Linux, FreeBSD and UNIX System V. Without a card, it says "A Cluster Interface Card is required."

Table: The parts of the window

| Part | What it does |
| --- | --- |
| Top tabs | Supercomputers, Datacenters and AI. Each tab lists the clusters of its kind. |
| Supercomputer tabs | NODES, CLUSTER MAP and QUEUE. |
| Datacenter tabs | SERVERS, INVENTORY and DIAGNOSTICS. |
| Footer buttons | SYSTEM ALL, PROGRAM ALL, ALL ON or ALL OFF, and CANCEL JOB, explained below. |

Table: The footer buttons

| Button | What it does |
| --- | --- |
| SYSTEM ALL | Installs a system on every node of the cluster, from a disc in a linked drive ([](jsc:cluster_installs)). |
| PROGRAM ALL | Installs a program on every node, from a program disc in a linked drive. |
| ALL ON or ALL OFF | Switches every node of the cluster on or off. It installs nothing. |
| CANCEL JOB | Stops the running install after the nodes being written. |

Each node row has its own buttons. SYSTEM and PROGRAM install on that node alone. POWER ON and POWER OFF only switch the node.

## Datacenter sections

A datacenter section is the set of servers behind one face of a Server Router ([](jsc:server_router)). The BALANCE button cycles the router's load-balance mode, from ROUND-ROBIN to LEAST-LOADED, then MANUAL, and back again. The modes are explained in ([](jsc:load_balancing)).

Open INVENTORY to see what the section holds. The hint in the inventory says that you click an item to move it out, and drop a stack on the window to deposit it.

## Names

Press RENAME and type a name. An empty name goes back to the default. Supercomputers are named SC-1, SC-2 and onward, in the order the network registered them. A section takes its router's name and the face it sits on, such as Router · NORTH.

## Node states

Each node row shows its state in capitals. NO SYSTEM means the node has no system installed. BAY OFF means the node is switched off. INSTALLING means a system or a program is being written to it now.

## Diagnostics

Open DIAGNOSTICS to see how a section is doing. At the top are three numbers of the last minute: the latency, the ticks from a request entering the router to its server's answer, beside the section's base; the throughput, the items a second through the section, beside the most it can move; and how many alerts stand.

> [!DEFINE] Base latency
> The ticks a section's servers take to answer when nothing else is in the way, by the drives they have.

Under them you see the last hour, a minute at a time: the latency in amber and the throughput in blue, each on its own scale. Under the chart are the alerts standing now, and for how long.

Table: The alerts of a section

| Alert | Level | When it stands |
| --- | --- | --- |
| Disks nearly full | WARN, then ERROR | A server's disks are 90% full, then 98%. |
| Rack over its thermal budget | WARN | A rack makes more heat than it sheds, and says how much its machines are held back. |
| Router at its budget | WARN | The router manages as many racks as its era allows. |
| Latency over twice the base | WARN | The last minute's latency is more than twice the section's base. |

Every alert also goes into the network's event log as it is raised. A latency over twice the base stands only while it lasts; then the chart keeps it, and the foot of the tab says how long ago it was and how long it lasted.

## If something goes wrong

> [!PROBLEM] The window says "A Cluster Interface Card is required."
> Install a Cluster Interface Card in the computer ([](jsc:expansion_cards)).

> [!PROBLEM] The list shows no supercomputer and no section.
> The card may be too old to reach them. The table in ([](jsc:cluster_installs)) shows what each card reaches.

> [!PROBLEM] The window says "that row is not a node this card reaches".
> The card does not reach that machine. Use a newer card for it.

> [!PROBLEM] The window says "select a cluster first".
> Select a cluster in the window, then press the button again.

> [!SEE] jsc:cluster_command, jsc:cluster_installs, jsc:supercomputers, jsc:datacenters, jsc:server_racks
