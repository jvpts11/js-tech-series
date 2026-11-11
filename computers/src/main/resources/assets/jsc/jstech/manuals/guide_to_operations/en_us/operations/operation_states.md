---
section: operations
order: 134
icon: jsc:mainframe
---
# The eight states of an Operation

Every Operation is in one of eight states at any time. The Processes tab of the [Network Manager](jsc:network_manager) shows you each one, in the words of the table below.

Three states mean that the Operation is still going: queued, running and waiting. The other five are final. The Operation is then done, and it is kept in the log ([](jsc:cancelling_and_the_log)).

> [!DEFINE] log
> The Mainframe's list of the last 32 Operations that settled, newest first.

> [!DEFINE] hold
> A reservation on items. Other Operations that need those items wait until the reservation ends.

Table: The eight states

| State | The Network Manager says | What it means |
| --- | --- | --- |
| `PENDING` | queued | Ready, and waiting for a free queue. |
| `PROCESSING` | running | Holds a queue and moves items. |
| `WAITING` | waiting | Blocked by another Operation's hold. It takes no queue while it waits. |
| `COMPLETED` | done | Moved all that was asked. |
| `COMPLETED_PARTIAL` | partial | Moved some of it, less than was asked. |
| `FAILED` | failed | Moved nothing, and was not done. |
| `RESOURCE_LOCKED` | locked | Gave up waiting for a hold, after the waiting time ran out. |
| `DISCARDED` | discarded | Stopped before it finished, on purpose or by the machine. |

The list colours each row by its state: going or done, held up, or stopped. A row can also show a line that gives the reason, such as the ones below.

Table: The reason lines

| The line | What it means |
| --- | --- |
| `delivered 40 of 64` | It moved some of what was asked. The two numbers are what moved and what was asked. |
| `timed out waiting on a locked resource` | A hold on the items lasted too long. |
| `failed` | Nothing was moved, and the Operation was not done. |
| `the network was not holding enough` | A SELECT asked for more than the network holds. |
| `the network had no room left for the` | An INSERT found no room left in the network's storage. |

## If something goes wrong

> [!PROBLEM] An Operation stays queued.
> All the queues are busy. A Mainframe with more graphics cards has more queues ([](jsc:expansion_cards)), and a faster processor finishes each one sooner ([](jsc:mainframes)).

> [!PROBLEM] It ends locked.
> The items were held for longer than the waiting time, which is 60 seconds by default. Wait, or UNLOCK the item ([](jsc:lock_unlock)).

> [!PROBLEM] It ends partial.
> The network held less than you asked for. Store more of the item ([](jsc:storage_servers)), or ask for less.

> [!PROBLEM] It ends failed.
> The network held none of the item, or a step was refused. Check the item's name, and that the item is stored.

> [!SEE] jsc:states_and_queues, jsc:cancelling_and_the_log
