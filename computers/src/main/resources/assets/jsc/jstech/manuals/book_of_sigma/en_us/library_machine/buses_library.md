---
section: library_machine
order: 450
---
# Buses from a program

A bus is a block that moves items between the network and a machine. Its settings say which items it handles, how many it keeps and moves, and when it works. The bus itself is explained in [](jsc:buses).

If you want to set a bus, Bus.Named finds it by its name on the network. It gives null when no bus of that name is on the network. Bus came in version 2.

Table: Bus members

| Member | What it does |
| --- | --- |
| `Bus.Named(name)` | The bus with that name, or null when there is none. |
| `Bus.Name` | The name of the bus. |
| `Bus.On()` | Turns the bus on. |
| `Bus.Off()` | Turns the bus off. |
| `Bus.Continuous()` | Sets the mode in which the bus runs. |
| `Bus.OnDemand()` | Sets the other mode, on demand. |
| `Bus.ReadWrite()` | Sets the bus to move items both ways. |
| `Bus.ReadOnly()` | Sets the bus to take items out only. |
| `Bus.WriteOnly()` | Sets the bus to put items in only. |
| `Bus.Only(items)` | The bus handles only the items listed. |
| `Bus.AllBut(items)` | The bus handles every item except the ones listed. |
| `Bus.Tag(tag)` | Adds a tag to the bus. |
| `Bus.Keep(number)` | How many of an item the bus keeps in place. |
| `Bus.Max(number)` | The most of an item the bus moves at once. |
| `Bus.Priority(number)` | Which bus goes first. |
| `Bus.Fuzzy(yes or no)` | The fuzzy setting of the bus. |
| `Bus.WhenStock(item, number)` | A condition on the stock of an item, explained in [](jsc:bus_conditions). |
| `Bus.WhenStockTag(tag, number)` | The same condition, on the stock of a tag. |
| `Bus.Between(from, to)` | The hours of the day the bus works, from 0 to 23. |
| `Bus.After(bus)` | Names another bus that this bus comes after, as a condition. |
| `Bus.Item(name)` | The settings of one item on this bus, as a BusItem. |

Each setting gives the bus back, so settings can follow one another. For example, bus("Ore in").Keep(16).Max(64) keeps 16 of the item and moves at most 64 at once.

Only and AllBut take the items separated by commas. Blank items are dropped. Between takes hours from 0 to 23.

A setting goes through the network's own language. When the bus cannot take it, the program stops with the engine's answer. Each setting is marked with the program that made it: the class the program starts from, or Sigma when it has none.

## A BusItem

A BusItem is one item of a bus. Its fields are BusItem.Bus and BusItem.Item, the names of the bus and the item. Its settings are BusItem.Keep and BusItem.Max, and they work as the same settings on the bus do.

## If something goes wrong

> [!PROBLEM] The program stops and says there is no bus with that name on this network.
> Check the name in the bus's window. Bus.Named gives null when the bus is not on the network.

> [!PROBLEM] The program stops and says the bus cannot be set so.
> The bus's era or its kind has no such setting. Choose another setting, or use another bus.

> [!PROBLEM] The program stops and says the item is not in the filter of the bus.
> Add the item to the bus's filter, or set the bus to take every item.

> [!PROBLEM] The program stops and says there is no item or fluid with that name.
> Check the name of the item. Names are the ones the network uses.

> [!PROBLEM] The program stops and says the hour is not an hour of the day.
> Use hours from 0 to 23.

> [!SEE] jsc:buses, jsc:bus_conditions, jsc:bus_filters_and_tags
