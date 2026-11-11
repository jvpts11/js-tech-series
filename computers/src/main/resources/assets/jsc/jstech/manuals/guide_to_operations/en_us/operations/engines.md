---
section: operations
order: 139
icon: jsc:mainframe
---
# Engines

What reads IQL and plans the network's work is the engine. Every new Mainframe comes with the Midsoft IQL Server, installed and running. Other engines can be installed on the Mainframe too, and each one speaks its own version of IQL with its own extras.

Table: The engines

| Engine | What it offers | Versions by era |
| --- | --- | --- |
| Midsoft IQL Server | IQL, with views and procedures. | Vintage 4.2, Legacy 2000, Transition 2008, Standard 2012, Advanced 2022 |
| NextgreIQL | Shows the plan it made and how it ran, takes hints that change the plan, and takes extras from other mods. | Legacy 7.0, Transition 8.3, Standard 9.0, Advanced 16 |
| Prophet YourIQL | You declare the state the network keeps; it works out the Operations to hold it, and answers subscriptions. | Legacy 3.23, Transition 5.0, Standard 5.6, Advanced 8.0 |

NextgreIQL and Prophet YourIQL come from the Legacy era on. Prophet works differently from the others: you say what state the network must keep, and it does the work to keep it. The book Learning IQL explains each engine ([](jsc:iql_engines_overview)).

A program names the engine it needs. The IQL Server Management Studio needs the Midsoft IQL Server, version 2000 or newer. The Nextgre Planner Studio needs NextgreIQL, and the Prophet Reactive Console needs Prophet YourIQL, version 3.23 or newer.

Replacing an engine takes at least 2 seconds, and up to 2 minutes. A network with more kinds of item takes longer. While the engine is replaced, the work already running carries on, and new plans are refused.

Without a working engine, only the transfers still work, such as moving items with SELECT, INSERT, MOVE and DELETE. CRAFT, QUERY and UPDATE are refused.

When a program says it needs an engine, you install that engine on the Mainframe, or start it if it is stopped ([](jsc:engine_services)).

## If something goes wrong

> [!PROBLEM] A program says that no compatible engine was found on this network.
> The engine is missing or stopped on this Mainframe. Install it or start it ([](jsc:engine_services)).

> [!PROBLEM] The result says Network Operations Service unavailable.
> An engine is being replaced. Wait until the replacement ends. The work already running carries on.

> [!PROBLEM] A program says it needs an engine that offers a feature the network does not have.
> The engine on this Mainframe lacks that extra. Use an engine that has it ([](jsc:engine_problems)).

> [!SEE] jsc:iql, jsc:automation_engine
