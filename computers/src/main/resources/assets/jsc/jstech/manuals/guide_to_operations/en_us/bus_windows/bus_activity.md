---
section: bus_windows
order: 130
---
# What a bus did

The Activity tab is the log of one bus. Each line is a move the bus made, or a time it waited, with the reason. The bus keeps the latest 16 lines and shows 8 at a time, so you scroll to see the rest.

Each move is an Operation ([](jsc:what_operations_are)). The terminal and the Network Manager list the same log, so you can read these moves in the Network Manager ([](jsc:network_manager)).

A line shows the time, the item with its amount after an x, a status on the right, and the reason under the status.

Table: Status

| Status | What it means |
| --- | --- |
| `COMPLETED` | The whole move went through. |
| `COMPLETED_PARTIAL` | Part of the move went through. The rest stays in the bus and is sent again. |
| `WAITING` | The bus held back, because the chest kept its KEEP amount or a condition did not hold. |
| `RESOURCE_LOCKED` | No room: the network is full for an Import Bus, or the chest is full for an Export Bus. |

Table: Reason

| Reason | What it means |
| --- | --- |
| into the network | An Import Bus moved items into the network. |
| into the chest | An Export Bus moved items into the chest. |
| the chest keeps 16 | The chest already holds its KEEP amount, so the Export Bus waits. |
| the network is full | An Import Bus found no room in the network. |
| the chest is full | An Export Bus found the chest full. |
| a condition holds it | A condition did not hold, so the bus waited. |

A hold that repeats updates the time of the newest line, instead of adding a line. When the network takes only part of a move, the rest stays in the bus, and the bus sends it when there is room.

## If something goes wrong

> [!PROBLEM] The log says "Nothing moved yet."
> The bus has not moved an item yet. Check what the bus needs before it moves ([](jsc:buses)), and what its filter lets through ([](jsc:bus_filters_and_tags)).

> [!PROBLEM] The status says RESOURCE_LOCKED.
> The network or the chest has no room. Free space in the chest, or add storage to the network ([](jsc:storage)).

> [!PROBLEM] The reason says "a condition holds it".
> A condition did not hold. Read the conditions of the bus in CONFIGURE ([](jsc:bus_conditions)).

> [!SEE] jsc:bus_window, jsc:bus_conditions
