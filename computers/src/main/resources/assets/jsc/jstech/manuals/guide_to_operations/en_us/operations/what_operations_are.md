---
section: operations
order: 132
icon: jsc:mainframe
---
# What an Operation is

In many storage mods, storage works like a service that is always there. You put a disk in a drive, and it serves you whatever is on it. That works, but it makes the whole system feel like one huge chest that you put things in and take things out of.

J's Computers does not work like that. Here the network does not only serve you: its computers do work to get each thing done, and every piece of that work is an Operation.

> [!DEFINE] Operation
> One action the network carries out, queued and then run over game ticks.

So, what is an Operation? It is an action the network is carrying out. Its names come from SQL (Structured Query Language), the language databases speak. When you take items out of storage, you do not fetch them from a disk yourself. You send the network a SELECT, and the network works it.

The request goes into a queue. The Mainframe picks it up, finds where the items are, and moves them to you as fast as its hardware allows. That is why the Mainframe's hardware matters so much ([](jsc:mainframes)).

> [!DEFINE] queue
> A lane on the Mainframe that moves items for one Operation at a time. More queues mean more Operations at once.

Most Operations go through the same stages. An Operation waits in a queue, runs over game ticks (20 ticks make a second), and then settles in one of eight states ([](jsc:operation_states)).

A timed Operation does not finish on the spot. A SELECT first takes a moment to find the items, and then it streams them out over ticks. By default that moment is 10 ticks on a hard disk, 3 on a solid-state drive and 1 on a non-volatile memory express (NVMe) drive.

> [!DEFINE] index
> The network's own list of what each of its servers holds, kept up to date so that the network knows where each item is.

Some Operations settle at once, on the spot: the index upkeep (ANALYZE, REINDEX and VACUUM), LOCK, UNLOCK and DROP.

Operations need a Mainframe. Every computer on the network is joined by data cable to one Mainframe, which runs the work and keeps the list of what is running ([](jsc:what_a_network_is)).

## If something goes wrong

> [!PROBLEM] The result says "the network has no running Mainframe".
> The Mainframe is off, or it is not on this network. Switch it on, or join it to the network ([](jsc:first_network)).

> [!PROBLEM] The Mainframe has no operating system installed, so it refuses new Operations.
> Install an operating system on it ([](jsc:installing)).

> [!SEE] jsc:operations_catalogue, jsc:iql
