---
section: examples_network
order: 503
---
# Buses

A bus is a part of the network that moves items, and each bus has a name. This example finds two buses by their names and sets them, one call at a time.

Each setting gives back the same bus, so the calls chain: one setting is followed by the next on the same line. The buses themselves, with their filters and tags, are explained in ([](jsc:buses)).

## The program

```sgs Buses.sgs
using System.*;
using System.IO.*;
using System.Network.*;
namespace Corpus;
class BusCorpus {
    static void Main() {
        Bus ore = bus("Ore in");
        if (ore != null) {
            ore.On().Continuous().Only("iron_ore, coal").Keep(16).Max(64).Priority(5);
            ore.AllBut("dirt").Tag("c:ores").Fuzzy(true).WhenStock("iron_ore", 512).WhenStockTag("c:ores", 4096);
            ore.Between(18, 6).After(Bus.Named("Coal in")).OnDemand().Off();
            BusItem iron = ore.Item("iron_ore").Keep(16).Max(64);
            Console.PrintLine(ore.Name + iron.Bus + iron.Item);
        }
        Bus wall = Bus.Named("Chest wall");
        if (wall != null) {
            wall.ReadWrite().ReadOnly().WriteOnly();
        }
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `bus("Ore in")` | Finds the bus called Ore in, or gives null when no bus has that name. |
| `if (ore != null)` | Checks that the bus was found before any setting is used. |
| `On(), Continuous(), Only(...)` | Powers the bus, keeps it moving, and accepts only the listed items. |
| `Keep(16), Max(64), Priority(5)` | Sets how many to keep, the most to move, and the priority number. |
| `AllBut(...), Tag(...), Fuzzy(true)` | Refuses the listed items, adds a tag, and turns on fuzzy matching of items. |
| `WhenStock("iron_ore", 512)` | Acts only when the stock of iron ore reaches 512. |
| `WhenStockTag("c:ores", 4096)` | Acts only when the stock of the tag reaches 4,096. |
| `Between(18, 6)` | Sets two hours of the day, each from 0 to 23. |
| `After(Bus.Named("Coal in"))` | Runs this bus after the bus called Coal in. |
| `OnDemand(), Off()` | Makes the bus move only when the network asks, and turns it off. |
| `ore.Item("iron_ore")` | Gives the settings of one item on the bus, so Keep and Max can be set for that item alone. |
| `Console.PrintLine(...)` | Prints the bus name, then the bus and the item that these settings belong to. |
| `wall.ReadWrite(), ReadOnly(), WriteOnly()` | Shows the three access settings, one after another, on the second bus. |

## If something goes wrong

> [!PROBLEM] The name gives nothing: no bus has that name, so the bus is null.
> Check the spelling of the name, and test for null before you use the bus.

> [!PROBLEM] Between gets an hour outside 0 to 23, and the program stops with a message such as "25 is not an hour of the day".
> Give hours from 0 to 23 only.

> [!PROBLEM] A bus cannot take some settings, and the program stops on that line with the bus's message.
> Remove that line, or use a bus that takes the setting.

> [!SEE] jsc:buses, jsc:sigma_library
