---
section: operations
order: 135
icon: jsc:mainframe
---
# Queues and graphics cards

A queue is one lane that moves items for one Operation at a time. The Mainframe has one queue for its processor, and one more for each graphics card in it. A Mainframe with a processor and three graphics cards works four queues at the same time.

> [!DEFINE] engine
> The program on the Mainframe that reads IQL and plans the network's work.

Subframes on the same network lend their graphics card queues too, if they run the same engine. A Subframe that runs another engine lends none ([](jsc:subframes)).

> [!DEFINE] Items a tick (it/t)
> How many items a queue or the Mainframe moves in one game tick.

Each queue runs at its own speed, measured in items a tick (it/t). The processor's queue runs at the processor's capacity. A graphics card's queue runs at the lower of the processor's capacity and the card's own speed. A weak card, or a card in an older slot, is slower.

Table: The speed of each queue

| Queue | Runs at |
| --- | --- |
| The processor's queue | The processor's capacity. |
| Each graphics card's queue | The lower of the processor's capacity and the card's speed. |

The queues do not share their capacity among themselves. A second graphics card gives you a second lane. It does not make the first lane faster.

The pace of the whole Mainframe is the lower of two figures. One is its orchestration capacity: how many items a tick the Mainframe, with its Subframes, can move. The other is its random-access memory (RAM) buffer, the working memory of the Mainframe in items a tick ([](jsc:memory)).

The Network Manager shows the queues in use, as Parallel queues, and the Orchestration capacity, in items a tick. A machine step of an autocraft does not take one of these queues ([](jsc:how_autocrafting_works)).

You can hot-swap a graphics card without losing work. The number of queues changes in place, and the Operations keep running.

## If something goes wrong

> [!PROBLEM] The network feels slow.
> Look at the Mainframe first. Its processor and its graphics cards set how many Operations run at once, and how fast ([](jsc:mainframes)).

> [!SEE] jsc:operation_states, jsc:priorities
