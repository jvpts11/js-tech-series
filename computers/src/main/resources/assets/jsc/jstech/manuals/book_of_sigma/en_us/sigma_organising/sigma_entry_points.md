---
section: sigma_organising
order: 422
---
# Where a program starts: Main and resident programs

Every program has exactly one place to start. When you run a program, the machine runs that place first. There are two kinds of start. A console program runs a method named Main once and ends. A resident program stays up after it starts, and runs code each game tick until it is stopped.

> [!DEFINE] Entry point
> The place where a program starts running.

> [!DEFINE] Console program
> A program that runs its Main method once, and ends when Main returns.

> [!DEFINE] Resident program
> A program that starts and then stays up, running its methods each game tick until it is stopped.

Table: The two kinds of program

| Kind | Starts with | Ends when |
| --- | --- | --- |
| Console program | `static void Main()` | Main returns. |
| Resident program | A class that stands on Script, or, in Sigma#, a class that implements IScript. | It calls Program.Exit, or the program is removed. |

A class that both stands on Script and implements IScript is a resident program, because writing the interface is a deliberate act. A file with no start place is refused, and so is a file with more than one.

Table: The forms of Main

| Form | Version | Meaning |
| --- | --- | --- |
| `static void Main()` | 1 | Runs once, takes nothing and gives nothing back. |
| `static int Main()` | 2 | Gives back an exit code, a whole number, when it ends. |
| `static void Main(string[] args)` | 2 | Receives the arguments that the program was started with. |
| `static int Main(string[] args)` | 2 | Receives the arguments, and gives back an exit code. |

Main must be static, in the class that holds it. A project is held to the version it names, so a form of Main that a later version adds is refused in an earlier project.

A resident program has three methods that the machine calls. OnInit runs once when the program starts. OnTick runs once per game tick, and a new tick waits until the last OnTick has finished, so a slow OnTick never piles up. OnDestroy runs when the program is stopped, if the machine still has time to run it. In Sigma#, write public void for each of them in an IScript class. In Sigma, write public override void for the ones you use, in a class that stands on Script.

A console program in Sigma looks like this, with one line that prints when it runs.

```sg Greeter.sg
using Standard.*;

namespace Hello;

class Greeter
{
    static void Main()
    {
        printf("Hello");
    }
}
```

A resident program in Sigma stands on Script, and it overrides only the methods it needs. Each game tick, the machine calls OnTick, as the [credits page](jsc:credits_per_tick) explains.

```sg Light.sg
using Standard.*;

namespace Lamp;

class Light : Script
{
    public override void OnTick()
    {
        // runs every game tick
    }
}
```

## If something goes wrong

> [!PROBLEM] The compiler says the program needs exactly one place to start, and this one has none.
> Add a class with a static method named Main, or a class that stands on Script, or implements IScript in Sigma#.

> [!PROBLEM] The compiler says 'Main' needs version 2; this project is version 1.
> Write the Main that your project's version has, or change the version the project names.

> [!SEE] jsc:sigma_namespaces_using, jsc:sigma_inheritance
