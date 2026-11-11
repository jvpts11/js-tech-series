---
section: the_achievements
order: 172
icon: jsc:mainframe
---
# Network and Operations achievements

The Networks & Operations tab follows the work of a network: a Mainframe on a network, computers joining it, Operations and IQL (the network's query language), autocrafting, and the trouble a network can get into. The table says what each one asks, and how the game checks it.

Table: The network and Operations achievements

| Achievement | What it asks | How it is earned |
| --- | --- | --- |
| Networks & Operations (root) | Bring a Mainframe onto a network. | A Mainframe joins a network for the first time. |
| Series of Tubes (task) | Join a computer to a Mainframe's network. | A computer joins a network that has a Mainframe. |
| SELECT \* FROM chest (task) | Take items out of network storage with an Operation. | A SELECT Operation completes. |
| DROP DATABASE production_database (task) | Run an IQL statement. | An IQL statement you type completes without error. |
| Set It and Forget It (goal) | Have an IQL JOB run on its own. | An IQL job runs on its own, and the Mainframe's operator is credited. |
| Two Out of Three Ain't Bad (secret) | Have an Operation finish only partly done. | An Operation ends PARTIAL, as the terminal shows it. |
| Deadlock (secret) | Have an Operation give up waiting on a locked resource. | An Operation ends RESOURCE LOCKED, as the terminal shows it. |
| Recipe for Success (task) | Encode a crafting pattern. | A Pattern Encoder encodes a pattern. |
| The Factory Must Grow (task) | Finish an autocraft. | An autocraft finishes. |
| Machine Learning (goal) | Finish an autocraft through a machine. | A machine finishes a step of an autocraft, or an autocraft runs one of its steps in a machine. |
| Supply Chain Issues (challenge) | Finish an autocraft of ten stages or more. | An autocraft with ten or more stages finishes. |
| Just-in-Time (task) | Put an Import Bus and an Export Bus on one network. | A bus cable part is placed on a network that already has the other kind of bus. |
| Who Unplugged the Server? (secret) | Cut the last cable between a running computer and its Mainframe. | A running computer loses its last route to a Mainframe. |
| There Can Be Only One (secret) | Put two Mainframes on one network. | A network conflict starts. The chat announces it too. |
| Talking to the Neighbours (goal, with ComputerCraft only) | Hear a ComputerCraft computer through a Network Gateway. | A Network Gateway receives a message from a ComputerCraft computer. |
| Information Superhighway (goal) | Join two networks through their WAN Gateways. | Two WAN Gateway Computers bring their line up in federation. |
| Pretty Fly for a Wi-Fi (task) | Join a computer or a portable to an access point. | A computer or a portable links through a wireless access point. |
| Remote Work (task) | Request items from the network on a portable. | A portable's network app asks the network for items. |
| Pics or It Didn't Happen (task) | Take a photo on a portable and send it to a computer. | A photo from a portable's gallery is written on a computer. |
| Data Mining (task) | Find a vein with the prospecting app. | The prospecting app finds a vein round you. |
| a/s/l? (task) | Send a message on the Messenger. | You say something in a Messenger room, at a computer or on a portable. |
| Thin Client (task) | Boot a computer over the network. | A computer starts a system the Mirror serves, through PXE or HTTP Boot. |

An Operation you asked for credits you. A scheduled Operation credits the operator of the Mainframe ([](jsc:mainframes)), and so does an IQL job that runs on its own.

Two Out of Three Ain't Bad and Deadlock name two of the ways an Operation can end: [the eight states of an Operation](jsc:operation_states) explains all of them.

Talking to the Neighbours appears only when the ComputerCraft mod is installed with J's Computers, and the achievement needs a [Network Gateway](jsc:network_gateway).

## If something goes wrong

> [!PROBLEM] Machine Learning does not appear although autocrafts finish.
> It needs a machine to run a step of the craft. The Factory Must Grow counts any autocraft.

> [!PROBLEM] Just-in-Time does not appear with one bus.
> Place the other kind of bus on the same network: an Import Bus with an Export Bus.

> [!PROBLEM] Talking to the Neighbours is missing.
> It needs ComputerCraft and a Network Gateway that hears one of its computers.

> [!SEE] jsc:what_a_network_is, jsc:what_operations_are, jsc:how_autocrafting_works
