---
section: cluster_tools
order: 157
icon: jsc:cluster_management_computer
---
# Installing on many machines

A bulk install writes one system or one program to every node of a cluster at once, or to the nodes you pick, instead of one machine at a time. You start it from the Cluster Manager ([](jsc:cluster_manager)) or with the cluster command ([](jsc:cluster_command)).

The disc comes from a drive linked to the Cluster Management Computer. Cable the drive to the computer with a peripheral cable ([](jsc:peripheral_cables)), and put the disc in the drive ([](jsc:drives)).

1. Install a Cluster Interface Card in the Cluster Management Computer.
2. Cable a drive to the computer, and put the disc in it.
3. Open the Cluster Manager, and press SYSTEM ALL or PROGRAM ALL. Or type cluster install, then the cluster's name, then system or program.
4. Watch the install in the window, or type cluster status.

## The cards and their reach

The card decides which machines a bulk install reaches, and how many nodes it writes at once.

Table: Cluster Interface Cards

| Card | Era | Reaches | Nodes at once |
| --- | --- | --- | --- |
| Serial Console Card | Vintage | Datacenter servers only | 1 |
| Management NIC | Legacy | Datacenters and supercomputers | 2 |
| Fabric Host Adapter | Standard | Datacenters, supercomputers and AI clusters | 4 |
| Fabric DPU | Advanced | Datacenters, supercomputers and AI clusters | 8 |

> [!DEFINE] Lane
> One node being written during a bulk install. The card decides how many lanes run at once.

Each node that is written at once takes one lane. A card placed in a slot older than the card was made for runs fewer lanes, because that slot carries data more slowly.

Writing one node takes 14 seconds on a machine with a clock speed of 2,000 megahertz (MHz). A faster machine takes less time, down to 3 seconds. A slower one takes more, up to 60 seconds.

## Refusals when you start

Table: Messages when you start

| Message | What to do |
| --- | --- |
| no cluster interface card | Install a Cluster Interface Card in the computer. |
| a job is already running | Wait for it to finish, or cancel it with CANCEL JOB or cluster cancel. |
| this card does not reach that cluster | Use a card that reaches it, from the table above. |
| no system disc in a linked reader | Put a system disc in a drive linked to the computer. |
| no program disc in a linked reader | Put a program disc in a drive linked to the computer. |
| that system installs by hand from its own shell | Install that system by hand on each node. A bulk install cannot write it. |
| that disc carries nothing to install | Use a program disc that carries a program. |
| no nodes in that cluster | Put machines in the cluster's racks first. |

## Nodes the install skips

A node that cannot take the install is skipped, and the job goes on with the other nodes. The node's name is followed by the reason in brackets.

Table: Skip reasons

| Reason | What it means and what to do |
| --- | --- |
| bay off | The node is switched off. Switch it on with POWER ON. |
| already installed | The node already runs that system, or already has that program. |
| unknown program | The install does not know the program on the disc. |
| no system | The node has no system, or no console for the program. Install a system with SYSTEM first. |
| does not meet the program's requirements | The node lacks the clock speed, video memory or free system disk space that the program needs. |
| too old for the program | The node is of an older era than the program needs. |
| missing rack | The rack no longer holds the node. Start the install again. |
| changed while writing | The node changed while its lane was writing it. |

## How the job ends

A system lane installs the system and sets the node to restart into it. A program lane installs the program on the node. When the job is over, it leaves one summary line.

Table: Summary lines

| Line | When it appears |
| --- | --- |
| `done: <what> on <nodes>` | The install finished. |
| `cancelled: <what> on <nodes>` | You cancelled the job. |
| `, <n> skipped` | Added to the end of the line above when some nodes were skipped. |

Cancel stops the job after the nodes being written. The nodes still in the queue are left as they were. The ALL ON and ALL OFF buttons, and the power command, only switch power. They never start an install.

## If something goes wrong

> [!PROBLEM] The install does not start, and it says "a job is already running".
> Wait for the job to finish, or cancel it with CANCEL JOB.

> [!PROBLEM] Every node is skipped.
> Read the reason in brackets after each node, and fix that reason. The table above lists them.

> [!PROBLEM] The install is gone after the world loads again.
> Start the install again. A job does not survive a reload.

> [!SEE] jsc:cluster_manager, jsc:cluster_command, jsc:cluster_management_computers, jsc:server_racks
