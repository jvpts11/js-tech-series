---
section: engine_choice
order: 383
icon: jsc:mainframe
---
# What an engine is

When you type an IQL statement, something has to read it and decide how the network does the work. On the network, that something is an engine. It runs as a program on the Mainframe ([](jsc:mainframes)), reads the statement, plans the steps, and hands the plan to the part of the network that carries the work out.

> [!DEFINE] Engine
> A program on the Mainframe that reads IQL, plans how the network does the work, and hands the plan on. Only one installed program of this kind plans at a time.

> [!DEFINE] Mainframe
> The one computer of a network that runs the network's own software, such as the engine that plans its work.

A new Mainframe comes with the Midsoft IQL Server installed and running, in the version of its age. Two more engines exist: NextgreIQL and Prophet YourIQL. Each speaks its own dialect of the language, and each offers extras beyond the core.

> [!DEFINE] Dialect
> The flavour of IQL an engine speaks. Every dialect accepts the core language, so the same core statements work on every engine.

Table: The engines

| Engine | Dialect | Ages | Extras |
| --- | --- | --- | --- |
| Midsoft IQL Server | IQL | Vintage to Advanced | procedures and views |
| NextgreIQL | NextgreIQL | Legacy to Advanced | explain, planner hints, extensions |
| Prophet YourIQL | YourIQL | Legacy to Advanced | declared states, subscriptions |

You manage an engine in the Services tab of the Network Manager ([](jsc:network_manager)), and a program that needs a particular engine says so when you open it. Every engine answers the same core: transfers, crafts and the statements of the core language.

Only one engine plans at a time, the active engine. When you stop or replace it, the Operations already running keep going on the plans they started with. You can still browse, deposit and withdraw items with no engine running.

With no engine running, transfers keep working, but crafts and IQL queries are refused. You start an engine to plan again.

## If something goes wrong

> [!PROBLEM] A craft or a query says the Network Operations Service is unavailable.
> No engine runs. Start one in the Services tab ([](jsc:engine_services)).

> [!SEE] jsc:engines, jsc:engine_services
