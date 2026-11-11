---
section: racks
order: 55
---
# Server types

Each server is a chassis of its own kind. The chassis sets its height, its drive bays, and how many processors and expansion slots it has. When you pick a server, this table tells you what you get. Every server and server case comes from the creative tabs of J's Computers.

Every era has three: a 1U server, a 2U Storage Server with the most drive bays and a 2U Compute Server with the most processors. A newer era's has more of what it is built for. A server still runs only as many processors as its board has sockets.

```table
source: jsc:servers
caption: Servers
heads: Server | Height | Drive bays | Processors | Expansion slots | Era
```

A server fits a rack of its own era or a newer one. A Vintage Server seats in a Standard Server Rack, but a Standard server never seats in a Vintage rack. Supercomputer Nodes go in a Supercomputer Rack, and the Advanced Supercomputer Node goes in an Advanced Supercomputer Rack.

A server's expansion slots take its cards, the storage cards among them: a RAID controller that joins its drives into one volume and a read cache that answers them faster ([](jsc:raid_arrays)).

Each rack unit has five front slots. A machine fills the slots of its rows in order with its drive bays, and the rest are blocked. When a slot is blocked, the screen says why, with one of two fixed messages.

## If something goes wrong

> [!PROBLEM] A slot is blocked: "Blocked - no unit in this row cables these slots".
> Mount a unit in that row that cables those slots, or pick a chassis that fits the row.

> [!PROBLEM] A slot is blocked: "Blocked - the chassis does not cable this slot".
> Use a chassis with more drive bays.

> [!SEE] jsc:server_racks, jsc:rack_drive_bays
