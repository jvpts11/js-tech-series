---
section: iql_recipe_list
order: 401
icon: jsc:import_bus
---
# Store items by type

Sometimes a network should take in only some kinds of item, such as iron ore and coal and nothing else. You set that with an Import Bus, which brings items into the network from the inventory it faces ([](jsc:buses)).

> [!DEFINE] Import Bus
> A part on a data cable that moves items from the inventory it faces into the network.

A bus moves only the items on its filter. You set the filter with a SET BUS line ([](jsc:set_bus)). Write the bus's name in single quotes when it has a space in it.

> [!DEFINE] Filter
> The list of items a bus may move.

Table: What the recipe needs

| Need | What it takes |
| --- | --- |
| Era | Legacy and newer for a filter; Advanced for tags |
| Program | None: you type the line at the prompt |
| Engine | The Mainframe's engine running; every Mainframe has the Midsoft IQL Server |

1. Place an Import Bus on a data cable, facing the inventory it takes items from.
2. Give the bus a name, as the buses page explains ([](jsc:buses)).
3. Type iql SET BUS 'Ore in' FILTER ONLY iron_ore, coal at the prompt.
4. Open the bus's window to check it ([](jsc:bus_window)).

Table: Lines for a bus named Ore in

| Line | What the bus does |
| --- | --- |
| `SET BUS 'Ore in' FILTER ONLY iron_ore, coal` | Moves only iron ore and coal. |
| `SET BUS 'Ore in' FILTER ALL BUT dirt` | Moves everything except dirt. |
| `SET BUS 'Ore in' FILTER NONE` | Empties the filter, so the bus moves nothing. |
| `SET BUS 'Ore in' FILTER TAG c:ores, c:logs` | Moves the items that carry these tags. |
| `SET BUS 'Ore in' WHEN STOCK iron_ore < 512` | Waits while the network holds 512 or more iron ore. |

Advanced buses can filter by tag. The c: prefix marks the tags that many mods share.

> [!DEFINE] Tag
> A group of items that share a label, such as c:ores for ores.

Table: What each era of Import Bus can set

| Era | Filter | Tags | Conditions |
| --- | --- | --- | --- |
| Vintage | No | No | No |
| Legacy | Yes | No | No |
| Transition | Yes | No | No |
| Standard | Yes | No | Yes |
| Advanced | Yes | Yes | Yes |

A filter holds 5 items, and a bus holds 4 tags. Conditions are the WHEN lines: they keep a bus waiting until the network meets them ([](jsc:bus_conditions)).

A filter decides what a bus moves, not where the items go. The network decides which server keeps them.

## If something goes wrong

> [!PROBLEM] The reply reads "the bus Ore in cannot be set so: its era or its kind has no such setting".
> The bus is a Vintage bus, which has no filter, or you asked for tags on a bus below Advanced. A filter of more than 5 items is refused as well, so split the list.

> [!PROBLEM] The reply reads "no item or fluid named steel_ingt".
> Check the spelling of the item's id, such as iron_ore.

> [!PROBLEM] The reply reads "no bus named Ore in on this network".
> Check the bus's name and its spelling. The bus must be on this network.

> [!PROBLEM] The bus moves nothing.
> Check that the bus is on (SET BUS 'Ore in' ON), that its filter names items the chest holds, and that its WHEN condition holds.

> [!SEE] jsc:buses, jsc:bus_filters_and_tags, jsc:bus_conditions, jsc:set_bus
