---
section: bus_programming
order: 131
icon: jsc:export_bus
---
# Programming buses

Programs can set a bus, as well as the window can. A program finds a bus by its name, on the same network. Give the bus its name first, in its window ([](jsc:bus_window)).

Programs set a bus in IQL or in Sigma. IQL is the network's query language ([](jsc:iql)). Sigma is a programming language, and its sister language is Sigma#. The Book of Sigma covers both ([](jsc:sigma_two_languages)).

A name matches regardless of capital letters, and the first bus with that name on the network is the one used. Put a name in quotes when it has spaces.

Vanilla items are written by their short name, such as iron_ore. Items from other mods are written in full, as namespace:path. Fluids are written with the word FLUID before them, as in FLUID water.

## IQL statements

Table: IQL statements for a bus

| Statement | What it sets |
| --- | --- |
| `SET BUS 'Ore in' ON` | Turns the bus on. |
| `SET BUS 'Ore in' OFF` | Turns the bus off. |
| `SET BUS 'Ore in' MODE ON DEMAND` | Moves only while there is a redstone signal. |
| `SET BUS 'Ore in' FILTER ONLY iron_ore, coal` | Lists the items the bus moves. |
| `SET BUS 'Ore in' FILTER ALL BUT dirt` | Moves everything except the listed items. |
| `SET BUS 'Ore in' FILTER NONE` | Clears the filter. |
| `SET BUS 'Ore in' FILTER TAG c:ores, c:logs` | Lists tags, on an Advanced bus. |
| `SET BUS 'Ore in' MATCH FUZZY` | Matches loosely, on an Advanced bus. |
| `SET BUS 'Ore in' KEEP 16 MAX 64` | Sets KEEP and MAX for the whole bus. |
| `SET BUS 'Ore in' KEEP iron_ore 16 MAX iron_ore 64` | Sets KEEP and MAX for one listed item, on a Transition bus. |
| `SET BUS 'Ore in' PRIORITY 5` | Sets the priority, on a Standard or later bus. |
| `SET BUS 'Ore in' WHEN STOCK iron_ore < 512` | Adds a STOCK condition. |
| `SET BUS 'Ore in' WHEN STOCK TAG c:ores < 4096` | Adds a STOCK condition for a tag. |
| `SET BUS 'Ore in' WHEN TIME BETWEEN 18:00 AND 06:00` | Sets the HOURS condition, and replaces the old one. |
| `SET BUS 'Ore in' AFTER BUS 'Coal in'` | Adds an AFTER condition. |
| `SET BUS 'Chest wall' ACCESS READ ONLY` | Sets what the network may do with an External Storage Bus. |

## Sigma calls

Table: Sigma calls for a bus

| Call | What it sets |
| --- | --- |
| `bus("Ore in").Only("iron_ore, coal");` | Lists the items the bus moves. |
| `bus("Ore in").AllBut("dirt");` | Moves everything except the listed items. |
| `bus("Ore in").Tag("c:ores");` | Lists a tag. |
| `bus("Ore in").Fuzzy(true);` | Matches loosely. |
| `bus("Ore in").Keep(16).Max(64);` | Sets KEEP and MAX for the whole bus. |
| `bus("Ore in").Item("iron_ore").Keep(16);` | Sets KEEP for one listed item. |
| `bus("Ore in").Priority(5);` | Sets the priority. |
| `bus("Ore in").WhenStock("iron_ore", 512);` | Adds a STOCK condition. |
| `bus("Ore in").Between(18, 6);` | Adds the HOURS condition, from 18:00 to 06:00. |
| `bus("Ore in").After(bus("Coal in"));` | Adds an AFTER condition. |
| `bus("Ore in").OnDemand();` | Moves only with a redstone signal. |

The SOFTWARE tab of a bus shows the IQL and SIGMA lines that would set it as it is now, so you can copy them ([](jsc:bus_window)). It writes an HOURS condition in job form, such as CREATE JOB bus_hours AS SET BUS 'Ore in' ON WHEN TIME BETWEEN 18:00 AND 06:00. The engine keeps that job as the bus's HOURS condition.

## When a statement is refused

Table: Messages from a bus statement

| Message | What it means |
| --- | --- |
| SET BUS needs the bus's name, in quotes when it has spaces | The statement has no bus name, or a name with spaces and no quotes. |
| SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY, WHEN, AFTER BUS or ACCESS | The statement names no setting. Write one of the words listed. |
| a bus has no setting, and then the word you typed | The word after the bus name is not a setting. Check its spelling. |
| no bus named Ore in on this network | No bus has that name on the network the program runs on. |
| bus Ore in set | The statement worked, and the bus is set. |
| the bus Ore in cannot be set so: its era or its kind has no such setting | The bus has no such setting, such as PRIORITY on a Legacy bus, or POWER on an External Storage Bus. |
| no item or fluid named, and then the name you typed | The filter does not know that name, and items from other mods need their full name. |
| Coal is not in the filter of the bus Ore in | A KEEP or MAX for one item needs that item in the filter first. |
| 25 is not an hour of the day | An hour for Between must be from 0 to 23. |

## If something goes wrong

> [!PROBLEM] A program's line says no bus has the name.
> Check the bus's name in CONFIGURE, and check that the bus is on the network the program runs on.

> [!PROBLEM] The bus does not take a fifth condition.
> A bus holds up to 4 conditions. Take one off first, then set the new one.

> [!PROBLEM] A setting does not change.
> The bus's era does not have that setting. Check which settings each era has in Buses by era ([](jsc:buses_by_era)).

> [!PROBLEM] Setting an hour is refused.
> Use hours from 0 to 23, such as 18 for six in the evening.

> [!SEE] jsc:iql, jsc:what_iql_is, jsc:buses_library
