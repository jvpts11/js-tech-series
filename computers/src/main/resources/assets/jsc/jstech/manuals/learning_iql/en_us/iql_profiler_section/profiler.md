---
section: iql_profiler_section
order: 397
---
# The IQL Server Profiler

The IQL Server Profiler records the network's work as it happens, one row for each event. When a craft or a query does something you did not expect, the Profiler shows you what the network did, row by row.

Open it from the studio's Tools menu, with the item IQL Server Profiler, or from the desktop. It opens in its own window, not as a tab of the studio.

Recording is done in a trace: one session that you start, pause, stop and save as a file. Each new trace is numbered.

> [!DEFINE] Trace
> One recording session in the Profiler. It starts, pauses and stops, and its rows can be saved as a file.

## What it records

Table: The events the Profiler records

| Event | Group | What it records |
| --- | --- | --- |
| `StatementStarting` | Statements | a statement comes through the network's door |
| `StatementCompleted` | Statements | a statement came back, with the rows it read |
| `OperationCreated` | Operations | the Mainframe took an Operation on |
| `OperationSettled` | Operations | an Operation ended: its moves, its time, and why it failed if it did |
| `LockAcquired` | Locks | an Operation took a lock, or an item was held by hand |
| `PlanChosen` | Plans | the engine chose how a craft is made |
| `BusMoved` | Buses | an Import Bus or an Export Bus moved items |

Each row has the columns EventClass, TextData, Requester, Computer, Items, Duration and StartTime.

## Starting, pausing and stopping

Table: The buttons of the Profiler

| Button | What it does |
| --- | --- |
| New Trace | Stops the trace, empties the rows and numbers the new trace. [[Ctrl]]+[[N]] does the same. |
| Open | Reads a trace file back into the window. |
| Save | Writes the rows to a file. |
| Start | Begins the trace, or continues it, with the groups you chose. |
| Pause | Stops listening and keeps the rows. The server stops sending rows. |
| Stop | Ends the trace. The rows stay until you choose New Trace or Clear. |
| Clear | Empties the rows. |
| Find | Searches the rows as text. |
| Events | Chooses the groups to record: Statements, Operations, Locks, Plans and Buses. |

Changing the groups while a trace runs restarts it with the new groups. A group that is not chosen records nothing.

The status line reads "Trace is running.", "Trace is paused." or "Trace is stopped." A new trace with no rows says "No events yet. Start the trace to record what the network does."

## Reading the rows

Under the grid, the detail lines of the row you choose appear. An event shows who asked for it, as "asked by: <requester> on <computer>", and how long it took, as "took <n> ticks (<n> s)". Twenty ticks make one second. When nobody asked, the requester reads "network".

The Profiler keeps at most 2,000 rows. When the trace is full, the oldest rows are dropped. The bottom bar shows "Ln <n>, Col 1" for the selected row, where Ln is the row number, and "Rows: <n>" for the count of rows.

## Finding and replaying

Find searches the rows as text. Find Next moves to the next match, and Escape closes the find strip. When nothing else matches, it says "Nothing more holds <text>."

Replay Statement, in the Replay menu, runs a recorded statement again. It is on only when you have selected a StatementStarting or a StatementCompleted row. It opens the studio and runs the statement in a new query.

## Saving and opening

Save As... writes the rows to a file. The file name is the trace's name, with spaces replaced by underscores, and the extension .log. The file has a header line with the seven column names, then one line for each row, and its values are separated by tabs.

The detail lines of a row are joined with " | ", and a line break inside a row becomes a space.

## If something goes wrong

> [!PROBLEM] The window says "No events yet. Start the trace to record what the network does."
> Press Start. The Profiler records only what happens after the trace starts.

> [!PROBLEM] The trace has stopped by itself.
> A trace ends when the player who started it leaves the game, and when the server stops. Use New Trace to start a new one.

> [!PROBLEM] The save says "The trace was saved to <file>, but it was too long: it was cut to <n> rows and <n> rows were left out."
> Record a shorter trace, or choose fewer groups, so that fewer rows are kept.

> [!PROBLEM] Replay Statement is off in the Replay menu.
> Select a StatementStarting or a StatementCompleted row first.

> [!PROBLEM] The Save button is off.
> Save is off until the trace has rows. Start the trace and let some events come in.

> [!SEE] jsc:operations_catalogue, jsc:buses
