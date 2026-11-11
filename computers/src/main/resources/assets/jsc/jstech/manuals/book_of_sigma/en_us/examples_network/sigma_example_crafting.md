---
section: examples_network
order: 504
---
# Crafting

Crafting is the network's work of making items from patterns. This example sets up two crafting interfaces and two routers, then asks the network to smelt raw iron.

The network's own language, IQL, carries crafting too ([](jsc:iql)). The words of that language are not needed here: this program uses only the library's calls.

Patterns and jobs are explained in ([](jsc:how_autocrafting_works)). Routers are explained in ([](jsc:input_routers_and_receiving_buses)).

## The program

```sgs Crafting.sgs
using System.*;
using System.IO.*;
using System.Network.*;
using System.Operations.*;
namespace Corpus;
class CraftingCorpus {
    static void Main() {
        CraftInterface kiln = craftInterface("Kiln");
        if (kiln != null) {
            kiln.Exclusive(true).MaxJobs(4).Pause();
            kiln.Resume();
            Console.PrintLine(kiln.Name);
        }
        CraftRouter north = craftRouter("North");
        if (north != null) {
            north.Only("gravel").Fuzzy(false);
            north.AllBut("dirt");
            north.Tag("c:sands");
            north.Any();
            Console.PrintLine(north.Name);
        }
        CraftInterface mixer = CraftInterface.Named("Mixer");
        CraftRouter west = CraftRouter.Named("West");
        if (mixer != null && west != null) {
            mixer.Route("Coarse dirt", "gravel", west);
            mixer.Route("Coarse dirt", "dirt", null);
        }
        AskResult smelted = Operations.Update("minecraft:raw_iron", "SMELT", 8);
        Console.PrintLine(smelted.Message);
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `craftInterface("Kiln")` | Finds the crafting interface by the name its window gives it, or gives null when none has that name. |
| `Exclusive(true), MaxJobs(4), Pause()` | Keeps the interface for this program, allows four jobs at once, and stops the interface taking jobs. |
| `Resume()` | Lets the interface take jobs again. |
| `craftRouter("North")` | Finds a router by its name, in the same way. |
| `Only("gravel"), Fuzzy(false)` | Sets the router to accept only gravel, and turns fuzzy matching off. |
| `AllBut("dirt"), Tag("c:sands")` | Sets the router to refuse dirt, and to accept the items of the tag c:sands. |
| `Any()` | Makes the router take anything. |
| `CraftInterface.Named, CraftRouter.Named` | Find the interface Mixer and the router West by their names. |
| `Route("Coarse dirt", "gravel", west)` | Sends the gravel input of the Coarse dirt pattern through the router West. |
| `Route("Coarse dirt", "dirt", null)` | Routes the dirt input by the routers' own filters, since no router is given. |
| `Operations.Update(...)` | Asks the network to smelt eight raw iron, and gives back an answer with Ok and Message. |
| `Console.PrintLine(...)` | Prints the message of that answer. |

> [!DEFINE] Pattern
> A stored description of an item the network can make, and of the items it takes to make it.

> [!DEFINE] Job
> One crafting task that the network carries out, such as making one item.

> [!DEFINE] Router
> A filter that sends an input of a pattern on to a crafting cable.

## If something goes wrong

> [!PROBLEM] A name gives nothing: no interface or router has that name, so the value is null.
> Use the name the interface's window shows, and test for null before you use it.

> [!PROBLEM] Operations.Update runs on a computer with no network, and the program stops with "this computer is not on a network".
> Run the program on a computer that is on a network.

> [!PROBLEM] A Route has no router, so the input follows the routers' filters instead of one router.
> Give the Route a router when you want to choose the path yourself.

> [!PROBLEM] A setting is refused, and the program stops with the message of the interface or the router.
> Remove that line, or use an interface or router that takes the setting.

> [!SEE] jsc:iql, jsc:how_autocrafting_works, jsc:sigma_library
