---
section: engine_choice
order: 390
icon: jsc:mainframe
---
# When an engine refuses

An engine answers a request it cannot do with a short message, and the Services tab shows its own messages. Find your message in the tables below, check when you see it, and follow what to do.

Table: Messages of the engines and the Services tab

| Message | When you see it | What to do |
| --- | --- | --- |
| `Network Operations Service unavailable` | A craft, query or update is asked while no engine runs. | Start an engine in the Services tab, or type iqlengine start. |
| `No Network Operations Engine is running` | The Services tab, when no engine runs. | Press Start... and choose an engine. |
| `No engine is installed on this Mainframe. Install one from its disc to plan the network's work.` | The Services tab, when nothing is installed. | Install an engine from its disc in a drive linked to the Mainframe. |
| `No compatible <engine name> was found on this network.` | A program needs an engine that the network does not run. | Install that engine if it is missing, then start it so the network runs it. |
| `<program> needs an engine that offers <capability>.` | A program needs a capability that the running engine does not offer. | Run an engine that offers it. The capability names are EXPLAIN and PLANNER_HINTS, among others. |
| `No other engine is installed on this Mainframe. Install one from its disc to replace this one.` | The Replace dialog, when only one engine is installed. | Install another engine from its disc. |
| `the Midsoft IQL Server is already installed` | The iqlengine command, when its verb would change nothing, such as starting an engine that already runs. | Read the result of iqlengine status first. |
| `the network has no running Mainframe to host the Midsoft IQL Server` | iqlengine, when the network has no Mainframe. | Build a Mainframe on the network and turn it on. |
| `iqlengine needs its install disc in a linked drive` | An install is asked, and no matching disc is in a linked drive. | Put the disc in a drive linked to the Mainframe. |

Some refusals show no message at all. A press on a Services button that the server does not accept does nothing, and the tab keeps showing the true state. Check the state the tab shows before you press again.

Table: Messages of NextgreIQL and Prophet YourIQL

| Message | When you see it | What to do |
| --- | --- | --- |
| `EXPLAIN needs a statement to explain` | EXPLAIN has nothing after it. | Write the CRAFT or the SELECT after EXPLAIN. |
| `EXPLAIN shows the plans of CRAFT and SELECT` | EXPLAIN comes before another statement. | Put EXPLAIN only before a CRAFT or a SELECT. |
| `hints change the plan of a CRAFT` | A hint follows another statement. | Remove the hint, or make the statement a CRAFT. |
| `MAX PARALLEL takes a whole number above nought` | MAX PARALLEL has no number, or 0. | Write a whole number of 1 or more. |
| `no server on the network is called <name>` | A PREFER or AVOID SOURCE names no server on the network. | Write the server's name as the network shows it. |
| `no item is called <name>` | An item name is not known. | Check the spelling, such as iron_ingot. |
| `the network could not start the craft of <item>` | The planned craft cannot start. | Check the network's storage and machines. |
| `nothing on the network knows how to make <item>` | No recipe makes the item. | Choose an item that some recipe makes. |
| `every plan for <item> was set aside` | The planner's rules rejected every plan. | Look at the Rules tab of the Nextgre Planner Studio. |
| `the network's engine keeps no views, procedures or jobs` | CREATE, DROP or EXEC on NextgreIQL or Prophet YourIQL. | Use the Midsoft IQL Server for views and procedures. |
| `jobs are kept by the Automation Engine; install it on the Mainframe first` | CREATE JOB, when the Automation Engine is not installed. | Install the Automation Engine on the Mainframe. |
| `syntax: <the parser's message>` | A statement cannot be read; the message names the word it stopped at. | Fix the word the message names. |
| `syntax: KEEP item >= n, or KEEP item BETWEEN a AND b` | A KEEP is malformed. | See Prophet YourIQL: keeping a level. |
| `syntax: WATCH item < n DO statement (the comparison one of <, <=, >, >=)` | A WATCH is malformed. | See Prophet YourIQL: watches, forgetting and SHOW STATES. |
| `syntax: FORGET item, or FORGET WATCH n` | A FORGET is malformed. | See Prophet YourIQL: watches, forgetting and SHOW STATES. |
| `<name> is not kept, or no watch is numbered <n>` | FORGET names an item or a watch that is not there. | Check the names and numbers with SHOW STATES. |

> [!SEE] jsc:engine_services, jsc:nextgre_hints, jsc:prophet_youriql
