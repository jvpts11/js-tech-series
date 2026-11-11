---
section: library_machine
order: 452
---
# Redstone Interfaces from a program

A Redstone Interface reads redstone signal for a machine, or emits it, and it is linked to the machine by cable. Your program reads and sets it through Redstone. The block itself is explained in [](jsc:redstone_interfaces).

> [!DEFINE] Redstone signal
> A strength from 0 to 15: strength 0 is off, and strength 15 is the strongest.

> [!DEFINE] Live
> Powered and linked to the machine. A live interface shows its strength; one that is not live shows strength 0.

Table: Redstone members

| Member | What it does |
| --- | --- |
| `Redstone.Named(name)` | The interface with that name, or null when no interface linked to this machine answers to it, ignoring capitals. |
| `Redstone.Name` | The name of the interface. |
| `Redstone.In()` | Sets the interface to read the signal. It stops emitting. |
| `Redstone.Out(strength)` | Sets the interface to emit that strength, from 0 to 15. |
| `Redstone.Level()` | The strength the interface shows now. |

In sets the interface to read, and Out sets it to emit. The last one you call wins. Each setting is marked with the name of the program that made it, or with Sigma when that program has no name of its own.

Level shows the strength that is emitted while the interface is live, or the strength it reads when it does not emit. It gives 0 when the interface is not live.

An interface with no name of its own answers to Redstone Interface. A name is up to 32 characters long, and two interfaces of one machine cannot share a name.

## If something goes wrong

> [!PROBLEM] The program stops and says the number is not a redstone strength.
> Give Out a whole number from 0 to 15.

> [!PROBLEM] The program stops and says no Redstone Interface with that name is linked to this machine.
> Link the interface to the machine with cable, and check its name in its window.

> [!PROBLEM] Redstone.Named gives null.
> No enabled interface linked to this machine has that name. Check the name in its window.

> [!PROBLEM] Redstone.Level gives 0.
> The interface is not live. Check that it is powered and linked to this machine.

> [!SEE] jsc:redstone_interfaces, jsc:disable_device
