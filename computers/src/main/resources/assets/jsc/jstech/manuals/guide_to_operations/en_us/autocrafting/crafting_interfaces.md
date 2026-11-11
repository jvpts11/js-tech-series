---
section: autocrafting
order: 145
icon: jsc:crafting_interface
covers_family: jsc:crafting_interfaces
---
# Crafting Interfaces and the machine line

A machine line is how the network runs a machine. You build it on the Crafting Cable, and it has three parts: the Crafting Interface, the Crafting Input Routers and the Crafting Receiving Bus.

> [!DEFINE] Crafting Input Router
> A cable part that sits on an interface's cable against one input face of a machine, and carries the inputs that face takes.

The Crafting Interface sits on the cable against the machine. It holds that machine's patterns, and it feeds the machine directly when it sits against it. It can also feed the machine through a cable of its own, coloured apart from the main one.

Table: Pattern files one interface holds

| Era | Pattern files it holds |
| --- | --- |
| Vintage | 3 |
| Legacy | 6 |
| Transition | 8 |
| Standard | 9 |
| Advanced | 12 |

For a machine that takes inputs on more than one face, a Crafting Input Router goes against each input face, on the interface's own cable ([](jsc:input_routers_and_receiving_buses)). The Crafting Receiving Bus goes against the machine's output: it takes back what comes out, and it credits it to the craft that fed the machine.

1. Lay Crafting Cable from the Crafting Computer to the machine.
2. Put a Crafting Interface on the cable against the machine.
3. For a machine with several input faces, put a Crafting Input Router against each one.
4. Put a Crafting Receiving Bus against the machine's output.
5. Load the machine's pattern into the interface, from the Crafting Manager ([](jsc:crafting_manager)).

An interface works one recipe at a time, or several jobs at once. Its mode decides which.

Table: The modes of an interface

| Mode | What it does |
| --- | --- |
| EXCLUSIVE | One recipe at a time on this machine, as its routers need; jobs of the same pattern run together. |
| NOT EXCLUSIVE | Several jobs at once, different recipes too, as far as the machine takes them. |

MAX JOBS sets how many jobs the interface runs at once, from 0 to 64 jobs. Auto (0) lets the interface take as many jobs as its machine accepts. A paused interface takes no new job. You name the interface too, with a name of up to 32 characters.

An interface is driven when a Crafting Computer drives the cable it sits on. A card drives the interfaces of its cable in cable order.

Interfaces beyond what the cards can drive wait, driven by no computer, until another card goes in. The screen reads "Driven by a Crafting Computer" or "Driven by no Crafting Computer".

Table: The jobs log of an interface

| Line in the log | What it means |
| --- | --- |
| No jobs yet. | The interface has run no job. |
| 3 of 8 made | How many pieces of a job's output the machine has made, out of how many it must make; the numbers shown are an example. |
| the machine gave none of the expected outputs | The job ended with none of the outputs its pattern lists. |
| unexpected outputs, sent to the network | The machine made outputs that its pattern does not list, and they went to the network. |
| draining: the recipe changes | The interface waits for the outputs still owed before a new recipe goes in. |

## If something goes wrong

> [!PROBLEM] A machine is never fed.
> Put the interface on a cable that a Crafting Computer drives, switch the computer on, or give the computer another card.

> [!PROBLEM] The output never comes back.
> Put a Crafting Receiving Bus against the machine's output.

> [!SEE] jsc:input_routers_and_receiving_buses, jsc:crafting_manager, jsc:patterns, jsc:data_cables, jsc:crafting_cards
