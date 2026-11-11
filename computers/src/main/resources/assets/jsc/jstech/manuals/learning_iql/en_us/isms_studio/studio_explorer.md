---
section: isms_studio
order: 393
---
# The Object Explorer

The Object Explorer is the tree at the side of the studio. Its top node is the network, and folders hang below it. When you open a folder, you see what the network holds.

Every folder holds one kind of thing. Right-click a node, and you see the menu of that kind of node. The next table names the folders.

Table: Folders of the network

| Folder | What it holds |
| --- | --- |
| Tables | The tables of the network. Each table has its columns below it. |
| Views | The views you saved with CREATE VIEW. |
| Stored Procedures | The procedures you saved with CREATE PROCEDURE, under Programmability. |
| Storage | One server for each storage server on the network. |
| Management | The Index, the Locks and the Operations Log. On the Legacy look this folder is called Maintenance. |
| Automation Agent | The Jobs folder. |
| Jobs | The jobs you made with CREATE JOB. A paused job shows (paused) beside its name. |

The Index note tells you how healthy the Index is. The next table shows each note and the statement that fixes it.

Table: Index notes

| Note | What it means | Fix |
| --- | --- | --- |
| healthy | Consistent. | none |
| stale | Hot events left entries the index has not re-read. | `REINDEX;` |
| fragmented | Mass removals left ghost entries pointing at storage that is gone. | `VACUUM;` |

Each entry under Locks shows the item and its quantity, as an item name with x and a number.

> [!DEFINE] Index
> The network's record of what each storage holds. ANALYZE, REINDEX and VACUUM keep it fresh.

> [!DEFINE] Lock
> A hold on items, so that other Operations cannot take them.

Table: The menu of each node

| Node | Menu items | Statement it runs |
| --- | --- | --- |
| Network (the root) | New Query, Refresh, Start, Stop and Restart the engine, Properties | none |
| Table | Query Top 100 Rows, Count Rows, Script Table as, Properties, Refresh | `QUERY <table> LIMIT 100;` |
| View | Query View, Script View as, Properties | `QUERY <view>;` |
| Stored Procedure | Execute Stored Procedure, Modify, Script Procedure as, Properties | `EXEC <name>;` |
| Server | Query What It Holds | `QUERY items WHERE server = "<name>";` |
| Index | Analyze | `ANALYZE;` |
| Index | Rebuild | `REINDEX;` |
| Index | Vacuum | `VACUUM;` |
| Lock | Unlock | none |
| Operations Log | Query the Log | `QUERY operations LIMIT 100;` |
| Job | Start Job, Pause Job, Delete | none |
| Automation Agent | New Job | none |

Script Table as and Script View as open a submenu. Each item writes a statement into a new query tab, or copies it to the clipboard. Delete on a job asks first: "Delete the job <name>?"

Object Explorer Details (F7) lists the children of the picked node in columns: Name, Kind, Rows, Columns, What it holds, Last change, State, Trigger, Quantity and Version. It has a Search box and counts its items. Properties (F4) lists the facts of the picked node. A running job shows as running there.

## If something goes wrong

> [!PROBLEM] The details say "Nothing is picked in the explorer."
> Click a node in the tree, then open the details again with F7.

> [!PROBLEM] Stop and Restart of the engine need a connection to the network.
> Connect the studio to a network first. The studio says "Not connected: <name>" until it is.

> [!SEE] jsc:maintenance_analyze_vacuum_reindex, jsc:iql_jobs
