---
section: isms_studio
order: 395
---
# The Activity Monitor

The Activity Monitor shows the Operations in flight on the network: the work that the network is doing right now. Open it from the Tools menu, and you see the list refresh by itself while it stays open.

The top line reads "Craft slots <used> / <total>": the craft slots in use out of the total. On the right it reads "<n> running". When nothing runs, the list says "No Operations in flight."

Table: The status of each Operation

| Status word | Group |
| --- | --- |
| done, running | going |
| partial, waiting, queued | held up |
| failed, locked, discarded | stopped |

Each group has a colour of its own, so you can see at a glance which Operations go on and which are held up or stopped. The statuses are explained in [the Operation states](jsc:operation_states).

Click an Operation row to open a menu with one item, Cancel Operation. The item works only for an Operation that has an id. Cancel Operation asks the network to stop that Operation.

## If something goes wrong

> [!PROBLEM] The list says "No Operations in flight."
> Start a query or a craft. Its Operation appears in this list.

> [!PROBLEM] Cancel Operation cannot be chosen on a row.
> That Operation has no id yet. Wait for it to get one, or press [[Alt]]+[[Break]] in the query tab that started it.

> [!SEE] jsc:operations_catalogue, jsc:cancelling_and_the_log
