---
section: clusters
order: 66
---
# Load balancing

Load balancing decides where a new item goes when several servers could take it. Each section of a Server Router has its own balancing mode, and the button on the router moves a section to the next mode.

> [!DEFINE] Load balancing
> Sharing new items among the servers, so that no single server fills first.

Table: Balancing modes

| Mode | Screen label | What a new item does |
| --- | --- | --- |
| Round robin | ROUND-ROBIN | The next server in turn. The start rotates for each face. |
| Least loaded | LEAST-LOADED | Sorts the servers by free space, most free first, and fills in that order. |
| Manual | MANUAL | Fills the servers in the order of the list. |

Every section starts in ROUND-ROBIN, and you see each section's mode on the router. The router keeps its place per face, so in ROUND-ROBIN each new write takes the next server in turn.

A server can also run the Load Balancer service: a program that spreads writes over the bay's drives, instead of filling them one after another ([](jsc:programs)).

## If something goes wrong

> [!PROBLEM] All the items go to one server.
> If the section is in MANUAL, it fills the first server in the list. Press the button until it reads ROUND-ROBIN or LEAST-LOADED.

> [!PROBLEM] The bay's drives fill one after another.
> Run the Load Balancer service on that server.

> [!SEE] jsc:server_router, jsc:programs
