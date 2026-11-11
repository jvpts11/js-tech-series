---
section: network_basics
order: 95
icon: jsc:server_rack
---
# Members, and how the network sees them

Not every block that touches a cable is a member of the network. The network sorts every block it can see into one of three categories, by what it may do with that block.

A block of Category A carries the network but is not a member of it, the way a cable is. A block of Category B is one the network can read for its state, but never give work to. A block of Category C is where the work happens: only these receive work from the Mainframe.

Table: The three categories

| Category | Has an identity | Gets Operations | Examples |
| --- | --- | --- | --- |
| A | No | No | Cables, the Server Rack |
| B | Yes | No | none |
| C | Yes | Yes | Mainframe, Server, Subframe, Personal Computer, Crafting Computer |

The Server Rack is a passive container. It holds the servers, and the servers are the members: each one has its own network identity and receives work ([](jsc:server_racks)).

A member is a block with a network identity, so a block of Category B or C. The network can name it, read it and, for Category C, send it work. You do not need to remember the categories to build a network; the table tells you what each block does.

> [!DEFINE] Network category
> The network's rule for a block: Category A carries the network, Category B is read by it, and Category C carries out its Operations.

> [!DEFINE] Network identity
> The identity of a network, which tells it apart from every other network. A Mainframe that is wired into a segment adopts the network on it.

## If something goes wrong

> [!PROBLEM] A computer is not on the network.
> The net command says "not on a network". Its cable does not reach the Mainframe. Follow the cables from it to the Mainframe, and check that no run is longer than its range ([](jsc:data_cables)).
