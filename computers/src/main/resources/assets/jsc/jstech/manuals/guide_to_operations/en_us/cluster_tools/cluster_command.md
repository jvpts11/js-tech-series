---
section: cluster_tools
order: 156
icon: jsc:cluster_management_computer
---
# The cluster command

The cluster command does the same work as the Cluster Manager, for when you prefer the prompt. Type cluster and one of the words below at the prompt of a system on a Cluster Management Computer ([](jsc:cluster_management_computers)).

> [!DEFINE] R:U
> The address of one node: its rack number in the cluster's order, counted from 1, then its unit row, counted from 1. R2:U5 is unit row 5 of the second rack.

The command needs a Cluster Interface Card in the computer. Without one, the answer is "no cluster interface card installed". The usage line reads: list | nodes <name> | power <name> [R:U] on|off | install <name> system|program | status | cancel.

Table: The subcommands

| Command | What it does |
| --- | --- |
| `list` | Lists the supercomputers and datacenter sections the card reaches; ls or no word at all does the same. |
| `nodes <name>` | Lists the nodes of one cluster, with their rack, unit, system and power. |
| `power <name> on\|off` | Switches every node of the cluster on or off, without installing anything. |
| `power <name> R:U on\|off` | Switches one node on or off. |
| `install <name> system\|program` | Starts a bulk install of a system or a program on the cluster ([](jsc:cluster_installs)). |
| `status` | Shows the install running now, or the last one. |
| `cancel` | Stops the running install once the nodes now being written are done. |

Names are matched without regard to capitals. The install command answers with the job it started, how many nodes it writes at once, and a reminder to follow it with cluster status. The status line lists the job, then how many nodes are done, writing, queued and skipped.

Table: The list columns

| Column | What it shows |
| --- | --- |
| `KIND` | Supercomputer or datacenter. |
| `NAME` | The name the Cluster Manager shows for the cluster. |
| `STATE` | Online or offline for a supercomputer; online or empty for a section. |
| `LOAD` | The crafts in use of the supercomputer's parallel crafts, or the number of servers for a section. |

A cluster the card cannot reach ends its row with "(out of this card's reach)".

Table: The nodes columns

| Column | What it shows |
| --- | --- |
| `RACK` | The rack's number, counted from 1 in the cluster's order. |
| `U` | The unit row, counted from 1. |
| `NODE` | The node's name. |
| `SYSTEM` | The system on the node, or none. |
| `POWER` | on or off. |

## If something goes wrong

> [!PROBLEM] It says "this command only exists on a Cluster Management Computer".
> Run it on a Cluster Management Computer, not on another computer.

> [!PROBLEM] It says "no cluster interface card installed".
> Install a Cluster Interface Card in the computer.

> [!PROBLEM] It says "no cluster named" followed by the name you typed.
> Use a name from the list that the command shows.

> [!PROBLEM] It says "no node at" followed by an address.
> Check the R:U address in the nodes listing.

> [!PROBLEM] It shows the usage for power.
> Add the R:U address, then on or off.

> [!PROBLEM] It says "already on" or "already off".
> The node is already in that state, or the card does not reach it.

> [!PROBLEM] It says "no job running".
> Start a job with install first, or read status again after it starts.

> [!SEE] jsc:cluster_manager, jsc:cluster_installs
