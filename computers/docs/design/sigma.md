# Σ and Σ#

The design of the mod's programming languages. The full reference of the language, its library and every API object is
the public [Σ# guide](../SIGMA.md); this page keeps the design behind it.

## Why they exist

Other computer mods tend to turn a building and automation game into a session of debugging Lua: the player isn't
playing, they are in a text editor, and when it works, they have rebuilt something the visual interface does in two
clicks.

Σ# exists for the few players who really want to program. Scripts are welcome, but this is how they are written, and
whoever writes them knows well what they are doing. Whoever stays with Σ# stays because they enjoy it. Nothing in the
mod needs a program: everything the network does has its window ([Interface](interface.md)).

## Names and files

| Name | What it is |
| --- | --- |
| Σ# | The language. Source in `.sgs` files. |
| Σ | The subset for the oldest machines. `.sg` files. |
| `sgsc` | The Σ# compiler (`.sgs` to `.asm`). From the Legacy on. |
| `scc` | The Σ compiler (`.sg` to `.asm`). From the Vintage on. |
| Assembly | The listing the machine runs, readable text, in `.asm`. |
| Sigma Runtime | The `sigma` package: runs listings with `sigma run`, stops and lists programs. From the Legacy on. |
| `sgpack` | The packager for the Mirror. |
| Sigma Foundation | The house of the compiler and the runtime. |

## The two languages

- **Σ#** is the full language, with C# syntax: classes with single inheritance, interfaces, `virtual`, `override` and
  `abstract`, structs (copied by value), records, enums, properties, delegates, lambdas, events, `foreach`, `List` and
  `Map`, `var`, `out`, `lock` and threads, windows. It is object-oriented **by rule**: all code lives inside a class. It
  is statically typed. Every file starts with the namespaces it uses (`using System.IO.*;`) and its own
  (`namespace Mine;`).
- **Σ** is a true subset of Σ#: everything written in Σ is Σ# and compiles on a newer machine unchanged. It has classes
  with inheritance, `virtual` and `override`, structs, enums, arrays, the loops, `out`, `is` and `as`. It has no
  interfaces, records, delegates, events, properties, lambdas, `foreach`, `List`, `Map`, generics, `var`, `lock`,
  threads, windows, `abstract` or interpolated strings, and every refusal says what to write instead. Its whole library
  is one namespace, `Standard`. It prints with `printf` and has C's old names (`puts`, `gets`, `atoi`, `fopen`...), each
  a library call written out in full; Σ# reads them too.
- There is no garbage collector: `new` allocates, `dispose x;` frees.
- **Errors:** an error (dividing by zero, reaching nothing, using what was freed, running out of memory, more than 1,024
  nested calls) stops the program where it is, and the message goes to the console. What can fail in normal use answers
  instead of stopping (`File.Write` returns false on a full disk; `Convert.TryInt`).
- **Σ will never have exceptions or generics defined by the player.** That is the right philosophy for the language of
  the oldest machines, which answers or stops.

**Language versions.** Σ and Σ# share a version number (Σ N is the subset of Σ# N). A version only adds: what version 1
accepts, version 2 accepts and compiles to the same listing. The version a machine compiles is that of its installed
compiler; a build can be held to an older version (`sgsc --lang 1`, or `langversion: 1` in the project), never a newer
one, and what came later is refused where it was written. The editors follow the machine's compiler.

## The two forms of a program

- A class with `static void Main()` (or `static int Main(string[] args)`) runs in the terminal that launched it, holds
  the prompt, prints as it goes and ends when `Main` returns or calls `Program.Exit`.
- A class that implements `IScript` (or extends `Script`, the Σ form) **stays**: `OnInit` once, `OnTick` every tick,
  `OnDestroy` when it is stopped. It keeps running after the world is saved and loaded again.

## What the player does

1. Install `sgsc` and `sigma` (from the Mirror or from a medium). On a Vintage machine, `scc` is the whole toolchain.
2. Write `Low.sgs` on a disk of their computer, in one of the editors (below).
3. `sgsc Low.sgs` gives `Low.asm`. Errors come with file, line, column and code (`Low.sgs(4,5): error S3057: ...`).
4. `sigma run Low.asm`. The program is a **process** of that computer: it shows in the Task Manager and the System
   Monitor, takes memory, spends credits every tick, writes to its console, and stops with `sigma stop`, from the Task
   Manager, or when it crashes. On a Vintage machine, which never has the runtime, the listing runs by its own name at
   the prompt, like a program of its time.
5. `sgpack` packs it and publishes it to the Mirror; any computer on the network installs it.

Nothing runs on the Mainframe unless the program is run **on** the Mainframe. A program talks to the network through the
same Operations any window uses; it never goes around the dispatcher.

A script that warns when logs drop under 100:

```csharp
using System.IO.*;
using System.Network.*;
namespace Mine;

class Low : IScript {
    public void OnInit() {
        Network.WatchBelow("minecraft:oak_log", 100, Told);
    }
    public void OnTick() { }
    public void Told(StockEvent e) {
        Console.PrintLine("low: " + e.Previous + " -> " + e.Total);
    }
    public void OnDestroy() { }
}
```

## The assembly

The compiler doesn't put out an opaque binary: it puts out **assembly**, a stack machine listing in text, one
instruction per line, which the player opens in the Editor and follows. The first line is the format's version
(`.asm 1`); a machine refuses a newer version with a clear message.

The instructions: constants (`ldc.i4 ldc.i8 ldc.r4 ldc.r8 ldstr ldnull`), locals and fields
(`ldloc stloc ldfld stfld ldsfld stsfld ldthis`), arithmetic and logic
(`add sub mul div rem neg and or xor not shl shr`), conversions (`conv.i4 conv.i8 conv.r4 conv.r8`), comparing and
jumping (`ceq clt cgt br brtrue brfalse beq bne blt ble bgt bge`), objects
(`newobj newarr ldelem stelem ldlen dispose castclass isinst copy`), locks (`monitor.enter monitor.exit`), calls
(`call callvirt ldfn ret sys`) and the stack (`pop dup`).

Three choices of form that the listing makes visible:

- **Parameters and locals share one set of numbered places**, parameters first, and a method's header says how many it
  needs. `ldloc` and `stloc` are enough. `this` has its own `ldthis`.
- **An out parameter comes back on the stack**, after the return value, and the caller stores it with an ordinary
  `stloc` or `stfld`.
- **A property is a field** in the listing; the rule of who may write it was checked before the listing existed.

**Instruction sets.** A listing is compiled for an instruction set: IA-16 (16 bits, Vintage), x86 (32 bits, Legacy) and
x86-64 (64 bits, from the Transition on). Each also runs the programs of the ones before (x86-64 runs x86 and IA-16).
`scc` compiles for IA-16 by default, so what it writes runs on every machine; `sgsc` doesn't compile for 16-bit
machines. An add-on can bring its own instruction set ([The API](api.md), [Hardware](hardware.md)).

## How programs run: credits and the clock

- A program never blocks the game. Every tick, a machine is worth a number of instructions, its **credits**: the cores
  of its processors times their MHz, divided by 8, never under 32 and with no ceiling. A faster machine gets further
  through a program in the same second.
- Credits are handed out in rounds of at most 64 instructions, across every program of the machine and every thread of
  each, so none finishes its share before another starts. A low-priority program skips one round in two.
- What protects the server is the **clock**, not a ceiling on credits. Two limits in `jstech-balance.toml`: the real
  time a machine may spend on its programs per tick (`program_machine_micros`, 1,000) and what they may all spend
  together (`program_server_micros`, 8,000). A machine that finds the server's time spent runs nothing that tick and
  goes first on the next: a busy server slows every computer evenly and never stops one.
- The interpreter can **resume**: a long loop crosses ticks. A script still inside `OnTick` when the next tick starts
  doesn't get another `OnTick`.
- The world only changes on the main thread. Calls that move data submit Operations and return handles; `Wait()` parks
  the program until the Operation settles, and the others keep their credits.
- **Leaving the program costs:** looking at something the computer knows about itself, 5; something the network knows,
  10; gathering a list from the computer, 30; a read, 50; a write, 100; asking the network to do something, 200. A call
  that brings back rows costs one more per row. Virtual Studio shows a line's price while it is written.
- **Threads** (`System.Threading`) take turns a few instructions at a time over the same memory, so nothing runs at the
  same instant and an expression is never torn; `lock` keeps a sequence together. Threads, waits and locks come back
  from a save where they were.
- Time is in ticks (`Time.Tick`, `Time.DayTime`, `Time.Day`); there is no wall clock.
- A computer runs several programs, each with its own memory and console. A program starts others (`Program.Start`),
  reads what they printed and their exit code, and trades lines with them; and it starts programs on other computers of
  the network (`RemoteComputer.Start`), with **that** machine's credits.

## Memory

- Every program has a **memory** of its own: 1 MB if it asks for no other, up to 64 MB (`sigma run Low.asm --heap 16M`).
  It is an entry in the machine's memory ledger ([Hardware](hardware.md)), with the bytes it holds, next to the system,
  the services and the windows; the Task Manager and the System Monitor show it in the same list.
- `new` allocates: an object costs a 16 B header plus its fields (each reference, 8 B). The count is exact.
- `dispose x;` frees the object; using a reference already freed is a runtime error with the line. Freeing a `List`
  doesn't free its elements: being explicit is the point of the language.
- Running out of memory stops the program, and the console says how much was alive and which lines allocated the most.
  What leaked is freed when the program dies; a computer never needs a reboot for memory.

## Events

Besides the program's own events (`event`, `+=`, `-=`, fired in order of subscription, synchronously), the API offers
these sources, delivered on the program's main thread, taking turns with the rest:

- `Network.Watch(id, handler)`, `WatchBelow(id, threshold, handler)` and `WatchAbove(...)`: an item's total on the
  network changed, or crossed the threshold (it fires on crossing, not while it stays crossed). They return a
  `Subscription`; freeing it stops it.
- `Program.OnMessage`: lines other programs send it.
- Window handlers (clicks, text) and `Gateway.OnMessage`.

At most 256 calls wait their turn, with 64 KB between them; a click that finds no room is dropped and counted in
`Program.DroppedEvents`; closing a window always gets in, at the front. There are no timers and no process events:
`OnTick`, `Thread.Sleep` and `Process.Wait` cover them.

## The library

| Namespace | What it has |
| --- | --- |
| `System` | `IScript`, `Action`, `Action<T>`, `Func<T, R>` |
| `System.IO` | `Console`, `File` (the machine's disks and the shares) |
| `System.Collections` | `List<T>`, `Map<K, V>` |
| `System.Utils` | `Math`, `Convert`, `Random`, `Time` |
| `System.Machine` | `Computer`, `Redstone` |
| `System.Network` | `Network`, `Mainframe`, `RemoteComputer`, `Iql`, `Bus`, `BusItem`, `CraftInterface`, `CraftRouter` |
| `System.Operations` | `Operations`: `Pull`, `Push`, `Craft`, `Update`, `Cancel`, `Get` |
| `System.Execution` | `Program`, `Process`, `ProcessMessage` |
| `System.Threading` | `Thread` and `lock` |
| `System.Sound` | `Sound` (beeps, melodies, songs), `Speaker` |
| `System.UI` | windows and their widgets |

`Console` also reads: `ReadLine`, one character, or one value at a time; a program waiting for a line asks with what it
left open. `File` works on the machine's disks with the shell's paths (`C:\logs\stock.log` on Frames,
`/home/logs/stock.log` on Linux) and on the network's **shares** (`config share C:\pub`; `\\host\pub` or `//host/pub`,
read only or also write); what is written weighs like any file, and files from another machine are copied, never moved.
Σ 2 reads files the way C's stdio does (`fopen` and the rest), with a limit of open files by era (8, 20, 64).

### What a program reaches

- `Network`: queries to the index, the network's computers, the watches.
- `Operations`: Pull, Push, Craft, Update (a Personal Computer's personal-use card changes an item: smelt, enchant,
  repair, combine, rename), Cancel and Get, with the eight states ([Operations](operations.md)). Everything submitted
  goes through the same entry points on the Mainframe that the Interactor uses, with the program's provenance.
- `Mainframe`: Online, today's peak and the statistics per Operation type.
- `Computer`: name, processor, system, free memory, disks, programs and processes.
- `Iql`: an IQL statement as from the prompt, on the program's behalf; views, procedures and jobs need an engine that
  has them.
- `Bus`, `CraftInterface`, `CraftRouter` and `Redstone`: the network's buses ([Storage](storage.md)), the crafting parts
  ([Autocrafting](autocrafting.md)) and the machine's Redstone Interfaces ([Peripherals](peripherals.md)), each by name,
  set up as their window sets them up; what a program set is marked with the program's name.
- `RemoteComputer`: running a program on another machine, a line at its prompt, sending lines to its programs.
- `System.UI`: the program opens windows on the machine's desktop, and the machine's **system** draws them (the same
  program looks like Frames 95 on Frames 95 and like KDE on Linux). Widgets are objects in the program's memory and are
  saved with it: a machine loaded again reopens the same windows. On a terminal-only machine, windows are drawn in
  characters, like the full-screen dialogs of the time. A `GenericComponent` is a widget of a type another mod adds.
- `Sound` and `Speaker` ([Sound](sound.md)).
- `Gateway`: the bridge to ComputerCraft ([Peripherals](peripherals.md)): its peripherals and its computers' power
  switch, never their files or their prompt.

A computer with no network gets `null` or empty on network reads, and a stop on any request to the network.

## Players' packages

`sgpack init` writes the package's manifest; `sgpack build` makes the package; `sgpack publish` sends it to the
network's Mirror, which keeps it next to the mod's catalogue. From then on, the package manager of any computer on the
network with the runtime installs it: every package unpacks into a folder with its name, so two never write over each
other and removing one takes exactly its files. `sgpack unpublish` takes it back.

## Editors

| Editor | Where | What sets it apart |
| --- | --- | --- |
| Editor (the system's) | every desktop | syntax highlighting, nothing more |
| Virtual Studio | Frames XP and newer | the whole workshop: projects (`.sgsproj`, `.sgproj`), properties, language version, completion, and the price of every call as it is written |
| Virtual Studio Code | desktops but CDE | light, with the console welded to the bottom, and completion |
| Exposure | desktops but CDE | compiles every program on the disk at once: a change shows what else broke; it suggests nothing |
| Vim | every system | takes over the terminal; it is what programs a rack server with no graphics |
| Emacs | every system | splits the terminal: the code and what the compiler said, together |
| vi, ee | FreeBSD, UNIX | come with the system |

Highlighting, checking as you type and completion come from the language registry (below): on a machine that runs them,
the editors treat a `.sg` like a `.sgs`, and only offer what the machine's compiler version has. Knot keeps the
network's code revision by revision ([Programs](programs.md)).

## The language registry

Σ# is the language the mod delivers, and the only one. Nothing in the mod assumes another, and nothing about Σ#'s nature
is negotiable through settings: required object orientation, manual memory and credits per tick **are** the language.

J's Core exposes a **language registry** (with no gameplay). A language says its id, its name, its source and binary
extensions; it compiles (for an instruction set and a version, with complaints as
`file(line,column): error CODE: message`), splits text into tokens for the editors, and, if it has a binary of its own,
starts and saves its processes (with a version of what it saves, to read old saves). The registry has `register`,
`unregister`, `reserve` (an extension), `get`, `byExtension`, `runnerOf`, `sourceOf` and `all`, and closes after
startup. Σ# registers from J's Computers. A language that compiles to the machines' listing needs no runtime: the
machine runs it. The editors, highlighting, `run` and the package managers resolve everything through the registry, by
extension, without knowing it is Σ#. An add-on can register its own language, or take Σ# out and live with the
consequences.

**A program's calls are fixed.** The language's functions (`Console.PrintLine`, `Math.Floor`, the API objects) are not a
registry add-ons extend: a call is part of what the language means, and a listing compiled on one machine has to mean
the same on every machine of its line and in every world. What an add-on adds instead is a **language** of its own,
which brings whatever it wants and compiles to the listing the machines already run, an **instruction set** (a new kind
of machine), or a **component type** for programs' windows ([The API](api.md)).

## To build

- **Generics and exceptions in Σ#**, in a future version: **generics** defined by the player (classes and methods with
  type parameters) and **exceptions** (`try`, `catch`, `finally`, a small `Exception` hierarchy; `finally` makes sure
  the `dispose` calls of a block that leaves halfway still run). What the library makes answer keeps answering. Σ stays
  a subset of Σ#. Today Σ# only has the built-in collections as generics, and no exceptions.
- **Two more event sources:**
  - `Operations.OnSettled(handler)`, or `OnSettled(operation, handler)` for just one: an Operation settled ("tell me
    when this craft is done");
  - `Network.OnEvent(handler)`: the network's event log ([Operations](operations.md)), with the level and the machine,
    so a program can react to a UPS on battery, a degraded power supply or a conflict.
- **The language reference in the game:** a section of the Guide to Operations ([Manuals](manuals.md)) with the files
  and forms of a program, the library by namespace, every API object with its price in credits, the errors and the
  versions. It is read in the book and on every system through its help (`man sigma`, `info sigma`, Help Topics, Get
  Help; [Operating systems](operating-systems.md)), written once for both. Today the reference is only the public Σ#
  guide.
