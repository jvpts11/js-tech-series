---
section: examples_devices
order: 506
---
# Redstone

Redstone is the power signal of the world, with a strength from 0 to 15. A Redstone Interface links the machine to the redstone of the world, so a program can read a signal or emit one ([](jsc:redstone_interfaces)).

This example finds an interface called Gate, sets it to read and to emit strength 15, and prints its name and its strength. You can match each line with the table below. It then finds an interface called Lever and prints its name.

## The program

```sgs Redstone.sgs
using System.*;
using System.IO.*;
using System.Machine.*;
namespace Corpus;
class RedstoneCorpus {
    static void Main() {
        Redstone gate = redstone("Gate");
        if (gate != null) {
            gate.In().Out(15);
            Console.PrintLine(gate.Name + Convert.ToString(gate.Level()));
        }
        Redstone lever = Redstone.Named("Lever");
        if (lever != null) {
            Console.PrintLine(lever.Name);
        }
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `redstone("Gate")` | Finds the Redstone Interface called Gate. It gives null when none is linked under that name. |
| `gate.In()` | Makes the interface read the signal. |
| `gate.Out(15)` | Makes the interface emit strength 15, the strongest. |
| `gate.Level()` | The strength the interface reads or emits. It is 0 when nothing powers it. |
| `gate.Name` | The name of the interface. |
| `Redstone.Named("Lever")` | Finds a second interface by its name, in the same way. |

> [!DEFINE] Redstone Interface
> A block that links the machine to the redstone of the world, so a program can read or emit a signal.

> [!DEFINE] Strength
> The power of a redstone signal, from 0 to 15: strength 0 is off, and strength 15 is the strongest.

## If something goes wrong

> [!PROBLEM] No interface has the name, so the value is null.
> Give the interface that name, and test for null before you use it.

> [!PROBLEM] Out is given a number outside 0 to 15, and the program stops with "16 is not a redstone strength, which runs from 0 to 15".
> Use a strength from 0 to 15.

> [!PROBLEM] The interface is no longer linked to the machine, and the program stops with "no Redstone Interface named 'Gate' is linked to this machine".
> Link the interface to the machine again.

> [!SEE] jsc:redstone_interfaces, jsc:sigma_library
