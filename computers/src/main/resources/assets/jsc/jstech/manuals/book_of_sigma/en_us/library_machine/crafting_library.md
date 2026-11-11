---
section: library_machine
order: 451
---
# Crafting interfaces and routers from a program

A crafting interface holds patterns and asks the network for crafts. A crafting router, on the same crafting cable, decides which inputs it carries, by its filter. The machines themselves are explained in [](jsc:crafting_interfaces) and [](jsc:pattern_encoders).

Table: CraftInterface members

| Member | What it does |
| --- | --- |
| `CraftInterface.Named(name)` | The interface with that name, or null when there is none. |
| `CraftInterface.Name` | The name of the interface. |
| `CraftInterface.Exclusive(yes or no)` | The exclusive setting of the interface. |
| `CraftInterface.MaxJobs(number)` | The most jobs the interface takes on. |
| `CraftInterface.Pause()` | Pauses the interface. |
| `CraftInterface.Resume()` | Resumes the interface. |
| `CraftInterface.Route(pattern, input, router)` | Sends one input of a pattern through one router of the interface's own cable. A null router routes that input by the routers' filters. |

Table: CraftRouter members

| Member | What it does |
| --- | --- |
| `CraftRouter.Named(name)` | The router with that name, or null when there is none. |
| `CraftRouter.Name` | The name of the router. |
| `CraftRouter.Only(items)` | The router takes only the items listed. |
| `CraftRouter.AllBut(items)` | The router takes every item except the ones listed. |
| `CraftRouter.Tag(tag)` | Adds a tag to the router. |
| `CraftRouter.Any()` | Clears the filter, so the router takes anything. |
| `CraftRouter.Fuzzy(yes or no)` | The fuzzy setting of the router. |

The interface and the router are found on the machine's crafting network. The name your program finds them by is the name their windows give, and a window can rename them.

Each setting goes through the same engine as the bus settings. A refused setting stops the program with the engine's text. Each setting is recorded under the program's class name, or under Sigma when the program has none.

## If something goes wrong

> [!PROBLEM] The program stops and says there is no Crafting Interface with that name on this network.
> Check the name in the interface's window.

> [!PROBLEM] The program stops and says there is no Crafting Input Router with that name on this network.
> Check the name in the router's window.

> [!PROBLEM] The program stops and says the interface holds no pattern with that name.
> Check the pattern's name in the interface's window.

> [!PROBLEM] The program stops and says the input is not an input of the pattern.
> Use one of the inputs that the pattern holds.

> [!PROBLEM] The program stops and says there is no router with that name on the cable of the interface.
> Use a router on the same cable as the interface.

> [!SEE] jsc:crafting_interfaces, jsc:pattern_encoders, jsc:asking_for_a_craft
