---
section: iql_settings
order: 375
---
# SET BUS

SET BUS changes the settings of a [bus](jsc:buses) from the prompt. It does what the bus's window does: it switches the bus on or off, choose the items it moves, how many, and when ([](jsc:bus_window)).

Write the bus's name as its window shows it, in quotes when it has spaces. Each statement changes one setting, so you type one statement for each setting you need.

```iql bus.iql
SET BUS 'Ore in' ON
SET BUS 'Ore in' KEEP 16 MAX 64
```

Table: The settings of SET BUS

| Statement | What it does |
| --- | --- |
| `SET BUS 'x' ON` | Switches the bus on. |
| `SET BUS 'x' OFF` | Switches the bus off. |
| `SET BUS 'x' MODE CONTINUOUS` | The bus moves all the time. |
| `SET BUS 'x' MODE ON DEMAND` | The bus moves only while its cable carries a redstone signal. |
| `SET BUS 'x' FILTER ONLY a, b` | The filter lists these items. |
| `SET BUS 'x' FILTER ALL BUT a, b` | The filter lists every item except these. |
| `SET BUS 'x' FILTER NONE` | The filter lists no item. |
| `SET BUS 'x' FILTER TAG c:ores, c:logs` | The filter lists the items with these tags. |
| `SET BUS 'x' MATCH FUZZY` | A loose match: any damage or components. |
| `SET BUS 'x' MATCH EXACT` | An exact match. |
| `SET BUS 'x' KEEP 16 MAX 64` | Keeps 16 in the chest, and moves at most 64 at a time. |
| `SET BUS 'x' MAX 64` | Moves at most 64 at a time. |
| `SET BUS 'x' KEEP iron_ore 16 MAX iron_ore 64` | The same, for one listed item. |
| `SET BUS 'x' PRIORITY 5` | Which bus goes first. A whole number, which may be negative. |
| `SET BUS 'x' WHEN STOCK iron_ore < 512` | Waits while the network holds fewer than 512 of the item. |
| `SET BUS 'x' WHEN STOCK TAG c:ores < 4096` | The same, for a tag. |
| `SET BUS 'x' WHEN TIME BETWEEN 18 AND 6` | Moves only between these hours. The range may cross midnight. |
| `SET BUS 'x' AFTER BUS 'Coal in'` | Waits until the named bus finishes. |
| `SET BUS 'x' ACCESS READ AND WRITE` | The network may take from and put into the inventory. |
| `SET BUS 'x' ACCESS READ ONLY` | The network may only take from the inventory. |
| `SET BUS 'x' ACCESS WRITE ONLY` | The network may only put into the inventory. |

After WHEN STOCK, the only comparison allowed is the < sign. KEEP and MAX take a whole number that is not negative. The hours after WHEN TIME are whole numbers from 0 to 23. A list of items is separated by commas, and a fluid is written with its kind first, as in FLUID water.

Table: The replies of SET BUS

| Reply | What it means |
| --- | --- |
| `bus <name> set` | The setting is saved. |
| `no bus named <name> on this network` | No bus of that name is on the network. |
| `the bus <name> cannot be set so: its era or its kind has no such setting` | This bus does not offer that setting. |
| `<item> is not in the filter of the bus <name>` | The item must be one the bus's filter lists. |
| `no item or fluid named <item>` | The name is not an item or a fluid. |

## If something goes wrong

> [!PROBLEM] SET BUS needs the bus's name, in quotes when it has spaces
> Type the name after BUS, and quote it when it has spaces.

> [!PROBLEM] The reply says "SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY, WHEN, AFTER BUS or ACCESS".
> Add one setting word after the name.

> [!PROBLEM] a bus has no setting <word>
> Use one of the settings in the table above.

> [!PROBLEM] unexpected token: <word>
> A statement has one setting. Type the next setting as a new statement.

> [!PROBLEM] no bus named <name> on this network
> Check the name in the bus's window.

> [!PROBLEM] expected a count, got: <text>
> Type a whole number after KEEP or MAX.

> [!PROBLEM] not an hour of the day: <text>
> Type an hour from 0 to 23.

> [!SEE] jsc:bus_filters_and_tags, jsc:bus_keep_max_and_priority, jsc:bus_conditions
