# Cannon

Cannon is the programming language of the series' computers. You write it at any computer in one of the
editors the systems ship with, compile it there with `cannonc`, and run what comes out with `cannon run`.
This page is the reference for what a program can be, what it can reach, and what it costs.

## Files and shapes

A source file ends in `.can`. `cannonc file.can` writes `file.asm`, a listing of the assembly the
machine runs; the listing is text you can open and read a line at a time. `cannon run file.asm [args]`
runs it, and whatever follows the file name is the program's `Program.Args`. `canpack` wraps a program
up for the network's Mirror so other players can install it.

A program is one of two shapes, and says which by how it is written:

- A class with a `static void Main()` runs at the terminal that started it. It holds the prompt, prints
  as it goes, and is gone when `Main` returns (or when it calls `Program.Exit`).
- A class that implements `IScript` stays up. `OnInit` runs once, `OnTick` every tick, `OnDestroy` when
  it is stopped, and it is still running after the world has been away and come back.

Every file starts with the namespaces it uses (`using System.IO.*;`) and its own (`namespace Mine;`).

## The budget and the clock

A program never blocks the game. Each tick a machine is worth a number of instructions, its credits:
the cores of its processors times their megahertz, divided by eight, never fewer than 32 and with no
ceiling. A faster machine gets through more of a program in the same second, and a loop that never ends
costs its machine the same tick as a program that does nothing.

What protects the server is the clock, not a cap on the credits. Two limits live in
`jstech-balance.toml`: how much real time one machine may spend on its programs in a tick
(`program_machine_micros`, 1000 by default) and how much every machine together may
(`program_server_micros`, 8000). A machine that finds the server's time already spent runs nothing that
tick and goes first the next, so a busy server slows every computer evenly and never stops one.

Credits are dealt out in turns of at most 64 instructions, going round every program on the machine
and every thread in each of them, so none finishes its share before another has begun. A program started
at low priority sits out every other round.

Reaching out of a program costs more than moving numbers about inside it. A look at something the
computer knows about itself costs 5; a look at something the network knows, 10; gathering a list from
the computer, 30; a read, 50; a write, 100; asking the network to do something, 200. A call that brings
back rows costs one more per row. The editors show the price of a line as you write it.

## Errors

There are no exceptions. A mistake (dividing by zero, reaching into nothing, using what was disposed,
running out of memory, asking for what is not there) halts the program where it stands, and the
message is what the console shows. Calls that can fail for ordinary reasons answer instead of halting:
`File.Write` returns false on a full disk, `Convert.TryInt` says whether the text was a number.

## Namespaces

| Namespace | What is in it |
|---|---|
| `System` | `IScript`, the delegates `Action`, `Action<T>` and `Func<T, R>` |
| `System.IO` | `Console` (print, read a line), `File` (the machine's disks and the network's shares) |
| `System.Collections` | `List<T>`, `Map<K, V>` |
| `System.Utils` | `Math`, `Convert`, `Random`, `Time` |
| `System.Machine` | `Computer`: what the machine is made of and what it runs |
| `System.Network` | `Network`, `Mainframe`, `Operations`' rows, `RemoteComputer`, `Iql` |
| `System.Operations` | `Operations`: pull, push, craft, cancel, ask after an operation |
| `System.Execution` | `Program`, `Process`, `ProcessMessage` |
| `System.Threading` | `Thread`, and the `lock` statement |

## Programs starting programs: `System.Execution`

```
Program.Name / Program.SetName(string)     what the machine lists this program as
Program.Args -> List<string>               what it was started with
Program.Current -> Process                 this program, as a handle
Program.Exit(int code)                     ends the program, every thread with it
Program.Start(string path) -> Process
Program.Start(string path, List<string> args) -> Process
Program.Start(string path, List<string> args, string priority) -> Process
Program.OnMessage(Action<ProcessMessage>)  hears lines other programs send

Process { Id, Name, Host, Running, ExitCode }
Process.Wait()                             parks until the program ends
Process.Wait(long ticks) -> bool           the same, giving up after so many ticks (false)
Process.Kill()
Process.Output() -> List<string>           what it printed
Process.Send(int id, string text) -> bool  a line to a program on this machine

ProcessMessage { From, Text, Tick }
```

`Start` runs a compiled file from the same disks (`C:\tools\count.asm` on Frames, `/home/tools/count.asm`
on Linux) without handing it the terminal: what it prints goes to its own output, for the parent to read.
Priority is `low`, `medium` or `high`; `medium` is what a program gets when nobody says. A program
started by another keeps its output and exit code for that program to read until the parent is gone;
a running child outlives its parent. `Running` and `ExitCode` are the machine's word, never a stale
field. A halt reads as exit code 1.

## More than one thing at once: `System.Threading`

```
Thread.Start(Action body) -> Thread        a thread running the body, beside the rest
Thread.Current -> Thread
Thread.Sleep(long ticks)
Thread.Yield()                             gives the rest of the turn away
Thread { Id, Running }
Thread.Join()                              parks until the thread ends
Thread.Join(long ticks) -> bool            the same, giving up after so many ticks (false)
Thread.Stop()

lock (thing) { ... }                       holds the object's lock while the block runs
```

Threads take turns a few instructions at a time over the same memory, so nothing runs at the same
instant and a single expression is never torn. A run of them can be: `lock` keeps it together. The lock
is held on every way out of the block (the end, a `return`, a `break`, a `continue`), a thread may take
the same lock more than once, and a thread waiting on a held lock spends nothing. Threads, their waits
and their locks come back from a save where they were. A program that runs at a terminal takes its
threads with it when `Main` returns; a program that stays up keeps them between ticks.

## The network's shares

Any computer opens a folder to the others on its network with `config share C:\pub` (reading) or
`config share C:\pub write` (writing too), and closes it with `config unshare pub`. Every other machine
on the network reaches it as `\\host\pub` at the prompt (`/net/host/pub` on Linux), in the file
explorer under Network, and in a program through `File` with the same path:

```
File.Read("\\\\lab\\pub\\note.txt")
File.Read("//lab/pub/note.txt")            the same, easier to write
```

A read-only share refuses writes and lists everything as read-only. Files on another machine are copied,
never moved or renamed from afar.

## The other computers: `System.Network`

```
Network.Computers() -> List<RemoteComputer>
Network.Computer(string name) -> RemoteComputer          null when there is no such computer

RemoteComputer { Host, Name, Type, Os, Online }
RemoteComputer.Start(string path[, List<string> args[, string priority]]) -> Process
RemoteComputer.Shell(string line) -> List<string>        one line at its prompt, and what it printed
RemoteComputer.Send(int processId, string text) -> bool
RemoteComputer.Processes() -> List<Process>
```

`Start` runs a compiled file from the other machine's disks, on that machine, out of its budget. The
handle that comes back is the same `Process` as a local one, and its `Host` says where the program is.
A machine that would rather not take any of this says `config remote off`; a program that asks it
anyway halts with the refusal. So does one that names a computer that is off, or not on the network.

## The network's language: `Iql`

```
Iql.Run(string statement) -> IqlResult { Ok, Message, Rows }
Iql.Query(string statement) -> List<Map<string, object>>     halts when the network refuses
Iql.Exec(string procedure[, List<string> args]) -> IqlResult
Iql.RunFile(string path) -> IqlResult                         one statement a line
```

A statement goes to the Mainframe as it would from the prompt or the Network Management Studio, under
the program's name. Rows are maps keyed by their columns (`name`, `quantity`, `detail`). Plain
statements need only a Mainframe; views, procedures and jobs need the IQL Engine installed on it.

## Watching the network

```
Network.Watch(string item, Action<StockEvent>) -> Subscription
Network.WatchBelow(string item, long threshold, Action<StockEvent>) -> Subscription
Network.WatchAbove(string item, long threshold, Action<StockEvent>) -> Subscription
```

Being told beats asking. A threshold watch fires on the crossing, not for as long as the number stays
crossed, and disposing the subscription stops it. Handlers run on the program's main thread, in turn.
