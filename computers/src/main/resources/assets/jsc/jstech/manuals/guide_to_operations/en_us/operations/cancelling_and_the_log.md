---
section: operations
order: 137
icon: jsc:mainframe
---
# Cancelling and the log

You can stop an Operation while it runs or waits. The Processes tab of the Network Manager lists the Operations in flight. Click a row, and its popup shows the type, the item, the amount, the timing, the parts of its work and a Cancel button.

1. Open the Network Manager, and its Processes tab.
2. Click the row of the Operation that you want to stop.
3. Press Cancel in the popup.

Cancelling hands back what the Operation held. The Operation then settles as DISCARDED on the next tick, and the log records it.

The Command Prompt has the same control through its interac command. The two forms in the next table work on the Operations in flight.

```table
{"caption": "The Command Prompt commands", "rows": [[{"words": "Lists the Operations in flight, with their identifiers (IDs)"}, {"literal": "interac ops"}], [{"words": "Cancels one, by the start of its ID"}, {"literal": "interac cancel <id>"}]]}
```

The Mainframe keeps a log of the Operations that settled. It holds the last 32 Operations, newest first. The Log tab of the Network Manager shows it under the title "Network log".

Table: What a row of the log shows

| Part of the row | What it tells you |
| --- | --- |
| Type | The kind of Operation, such as SELECT or MOVE. |
| Item | The item that it worked on. |
| Amount | How much moved, out of how much was asked. |
| Status | Its state, such as PENDING, COMPLETED or DISCARDED. |
| Reason | The line with the reason, when there is one. |
| Waited and ran | How many ticks it waited for a queue, and how many it ran. |

The Stats tab shows the Operations that settled in the last hour. It says "No Operations settled in the last hour." when none did.

When the Mainframe is switched off, loses its operating system, or meets a network conflict, everything in flight ends. Each Operation is settled as DISCARDED and logged, and what it held is given back. A network conflict happens when two Mainframes claim the same network ([](jsc:network_identity)).

A save keeps the Operations in flight, and they resume at the next start. A save older than 24 hours is discarded and logged instead, by default. The mod's configuration can change that time.

## If something goes wrong

> [!PROBLEM] The Operation is still in the list after I cancelled it.
> The Operation is settled as DISCARDED on the next tick, not at once. Wait one tick, and the list refreshes.

> [!PROBLEM] My Operation vanished after a reload.
> The save was older than 24 hours, so the Mainframe discarded it and logged it. Look for it in the log.

> [!PROBLEM] The log says "No Operations logged yet."
> The log stays empty until an Operation settles. Wait until one finishes.

> [!SEE] jsc:operation_states, jsc:network_identity
