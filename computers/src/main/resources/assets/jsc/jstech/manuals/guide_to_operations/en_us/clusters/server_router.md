---
section: clusters
order: 65
icon: jsc:server_router
covers_family: jsc:server_router
---
# Server Router

The Server Router is how you organise a datacenter. It groups Server Racks into sections, one for each of its faces besides the uplink ([](jsc:datacenters)). Each era from the Legacy on has its own, and its faces take the data cables of its era and of every earlier one ([](jsc:data_cables)).

1. Join its uplink face to the network.
2. Join a section of racks to each of its other faces.
3. Open it to see each section and its rack count.

A Server Router manages a number of Server Racks in all, across sections, which its era sets: 4 in the Legacy, 8 in the Transition, 16 in the Standard and 32 in the Advanced. Its window shows its era in the corner and its rack count against that budget. Racks past it are counted as unmanaged, and the router shows its rack count in red.

The Cluster Manager runs each section as one machine, as far as its card reaches ([](jsc:cluster_management_computers)).

## If something goes wrong

> [!PROBLEM] The rack count shows in red.
> More racks sit on the router than its era manages. Move some to a second Server Router, which has its own budget, or use a router of a newer era.

> [!SEE] jsc:datacenters, jsc:load_balancing
