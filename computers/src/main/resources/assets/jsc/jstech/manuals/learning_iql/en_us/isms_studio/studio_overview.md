---
section: isms_studio
order: 391
---
# The IQL Server Management Studio

The IQL Server Management Studio (ISMS) is the program where you write IQL, run it, and read what the network answers. Use it when you want to query the network by hand.

The studio is a desktop program. Open it from its desktop icon: typing a command at the prompt does not open it. The console answers "Open the ISMS from its desktop icon."

The studio works with the Midsoft IQL Server, the engine that runs on the [Mainframe](jsc:mainframes) and reads your IQL. You need a network with that engine before the studio can work: [the Midsoft IQL Server](jsc:midsoft_iql_server).

The studio runs from the Legacy era on, and it needs 128 MB of memory.

## Its look in each era

The studio's look follows the era of your computer. Every look has the same functions. Only the appearance and a few names change.

Table: Look by era

| Era | Look |
| --- | --- |
| Legacy | The 2000 look. The name in the window is "IQL Query Analyzer". |
| Transition | The 2008 look. |
| Standard | The 2012 look. |
| Advanced and every era after it | The 2022 look. |

## The parts of the window

Table: Part of the window

| Part | What it holds |
| --- | --- |
| Menu bar | The menus File, Edit, View, Query, Tools, Window and Help. |
| Toolbar | The buttons Plan, Grid, Text, File and Cancel. Cancel stops the query that is running. |
| Object Explorer | The tree of the network, described in its own entry. |
| Query tabs | One tab for each query. Each tab holds a script and its results. |
| Status bar | The engine, its version, the computer and the time of the last run. |

The Object Explorer Details, the Activity Monitor, the Template Explorer and the IQL Reference open beside the queries. The Profiler opens in its own window.

## Settings and the start

The studio keeps its settings in the file isms.cfg at the top of the computer's disk. When it starts, it opens an empty query tab, unless you turn off "Open a new query when the studio starts" in Tools > Options.

> [!DEFINE] Script
> The statements in one query tab. The statements are separated by semicolons and run one at a time.

## If something goes wrong

> [!PROBLEM] The studio says "No compatible Midsoft IQL Server was found on this network."
> Put a Mainframe that runs the Midsoft IQL Server on this network ([](jsc:midsoft_iql_server)).

> [!PROBLEM] The studio says "<computer> runs <engine>. Use the tools of that engine, or ask whoever runs the Mainframe to switch it in the Network Manager."
> Use the tools of the engine that runs, or switch the engine in the Network Manager ([](jsc:network_manager)).

> [!PROBLEM] The studio says "<computer> has no Midsoft IQL Server installed."
> Install the engine from the Network Services of the Network Manager ([](jsc:network_manager)).

> [!PROBLEM] The studio says "Not connected: <name>".
> Connect the studio to a network whose Mainframe runs the engine, then try again.

> [!SEE] jsc:what_iql_is, jsc:first_queries
