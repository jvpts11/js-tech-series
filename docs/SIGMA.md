# Σ#

Σ# is the programming language of the series' computers. You write it at any computer in one of the
editors the systems ship with, compile it there with `sgsc`, and run what comes out with `sigma run`.
This page is the reference for what a program can be, what it can reach, and what it costs.

## Files and shapes

A source file ends in `.sgs`. `sgsc file.sgs` writes `file.asm`, a listing of the assembly the
machine runs; the listing is text you can open and read a line at a time. `sigma run file.asm [args]`
runs it, and whatever follows the file name is the program's `Program.Args`. `sgpack` wraps a program
up for the network's Mirror so other players can install it.

A program is one of two shapes, and says which by how it is written:

- A class with a `static void Main()` runs at the terminal that started it. It holds the prompt, prints
  as it goes, and is gone when `Main` returns (or when it calls `Program.Exit`).
- A class that implements `IScript` stays up. `OnInit` runs once, `OnTick` every tick, `OnDestroy` when
  it is stopped, and it is still running after the world has been away and come back. It writes all three.

A script can also be written by standing on the class `Script` instead of implementing the interface.
`Script` already has the three, doing nothing, so a script written this way fills in only the ones it
uses and says `override` on each. The two are the same program to the machine; the class is the form
Σ uses, since that language has no interfaces at all.

Every file starts with the namespaces it uses (`using System.IO.*;`) and its own (`namespace Mine;`).

## Σ, the smaller language

Σ is what the earliest machines are programmed in, and it is a true subset of Σ#: anything written in it is
also Σ#, and compiles on a newer machine untouched. A source ends in `.sg`, `scc file.sg` compiles it, and
what comes out is built for the 16-bit machines unless told otherwise, so it runs on every machine there is.
`sgsc` does not build for those machines at all.

It keeps classes with inheritance, `virtual` and `override`, structs, enums, arrays, the loops, `out`, `is`
and `as`. It has no interfaces, records, delegates, events, properties, lambdas, `foreach`, `List`, `Map`,
generics, `var`, `lock`, threads, windows, `abstract`, or strings with holes in them, and every refusal says
what to write instead. Its whole library is one namespace, `Standard` (`using Standard.*;`), with
`Console`, `File`, `Program`, `Math`, `Convert`, `Time`, `Computer` and `Script`, each a handful of members, and
from version 2 (see Versions below) `Sound`, `Speaker` and `Random`, whose `Next` and `Seed` are the whole of it.

It prints the way the languages of those machines printed, with `printf`, a call written with no type in
front of it: `printf("%s has %d items\n", name, count);`. The format has to be written out in quotes,
because it is read while the program is compiled and never while it runs; what is left is the pieces joined
and handed to the console, the very line adding them up by hand would have given. The holes are `%d` and `%i`
for a whole number, `%f` for a number, `%s` for text, `%c` for a character and `%%` for the sign itself, an
`l` before the letter is taken and means nothing, and there are no widths or precisions. A hole with no
value, a value with no hole, and a value of the wrong kind are all errors when the program is compiled. A
line break written into the format ends the line; the console keeps whole lines, so a `printf` that does not
end in one still ends its line. Σ# has `printf` as well, since it reads whatever Σ does.

Version 2 brings the other names those languages used, each written with no type in front of it. None of them
is anything new: each stands for a call the library already has, the compiler writes that call down, and the
listing is the very one writing it the long way gives.

| The old name | The long way |
|---|---|
| `puts(text)` | `Console.PrintLine(text)` |
| `gets()` | `Console.ReadLine()` |
| `exit(status)` | `Program.Exit(status)` |
| `abs(n)`, `sqrt(x)`, `pow(x, y)`, `floor(x)`, `min(a, b)`, `max(a, b)` | `Math.Abs`, `Math.Sqrt`, `Math.Pow`, `Math.Floor`, `Math.Min`, `Math.Max` |
| `atof(text)` | `Convert.ToDouble(text)` |
| `itoa(n)` | `Convert.ToString(n)` |
| `srand(seed)` | `Random.Seed(seed)` |
| `rand()` | `Random.Next(32768)`: a whole number from 0 to 32767 |
| `strlen(text)` | `text.Length` |
| `strstr(haystack, needle)` | `haystack.IndexOf(needle)`: where the needle starts, or -1 when it is not there |
| `sprintf(format, ...)` | the text `printf` would print, given back instead of printed |

A method or a variable of the program's own under one of these names is still the one called, so a program
that already had its own `abs` goes on calling it. Σ# has all of them too.

An old language is no reason to write it the hard way. On a machine that runs them, the editors treat a
`.sg` file as they treat a `.sgs` one: coloured, checked as it is typed, and completed from what Σ really
has, so nothing is offered that the compiler would then refuse. Virtual Studio's New Project lists every
shape of project in both languages; a Σ project keeps itself in a `.sgproj` file, holds `.sg` sources, and
starts out built for the oldest machines. A Σ# project may reference a Σ library, since Σ# reads it; a Σ
project cannot reference a Σ# one.

## Versions

Σ and Σ# have versions, one number for both, since Σ N is the subset of Σ# N. Version 1 is the language as it
first shipped under these names; version 2 brings `Sound` and `Speaker`, the old names above, and `Random` in
Σ's `Standard`. A version only ever adds: whatever version 1 takes, version 2 takes too and compiles to the
same listing, so a newer compiler never breaks an older program.

The version a machine builds is its compiler's. The package `sgsc` 2.0 is Σ# 2 and `scc` 2.0 is Σ 2, and a
machine whose compiler is older builds the older version until an upgrade brings it up: `pckmgr upgrade`, the
upgrade verb of each Linux and BSD package manager, or on MC-DOS installing the compiler again from newer
media. Both compilers say which they are before anything else (`Σ# Compiler 2.0`).

A build can be held to an older version, never a newer one: `sgsc --lang 1 file.sgs` (and the same on `scc`),
or a `langversion: 1` line in the project file. Whatever came later is refused where it was written:

```
Mine.sgs(4,5): error S3057: 'puts' needs Σ# 2; this project is Σ# 1
```

The editors follow the machine's compiler, and Virtual Studio the project as well: the suggestions offer only
what that version has, and the checks as you type hold a file to it.

## Classes standing on other classes

A class may stand on one other class and on as many interfaces as it likes, written after a colon.
Which method runs is always decided by what the object really is, not by the name it is held under,
and a class that replaces one of its base's methods has to say so:

- `virtual` on a method means a class below may put its own in its place.
- `override` on a method means it is taking the place of a `virtual` one above it. An `override` may
  itself be replaced further down, without writing `virtual` again.
- Writing a method that has the name and parameters of a base method, without `override`, is an error:
  either the one above is `virtual` and you meant `override`, or it is not and the two are a collision
  rather than a replacement. There is no way to hide a base method.
- `abstract` on a method leaves it without a body, for the classes below to fill in; only an `abstract`
  class may hold one, an `abstract` class cannot be made with `new`, and the first class below it that
  can be made has to give every one of them a body.
- Giving an interface the method it asked for is not replacing anything, so it needs no word.
- `static` methods, constructors and everything in a `struct` take none of the three: a struct is
  copied rather than pointed at, so there is no object whose real type could decide.

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
running out of memory, asking for what is not there, calls going more than 1,024 deep) halts the
program where it stands, and the message is what the console shows. Calls that can fail for ordinary
reasons answer instead of halting: `File.Write` returns false on a full disk, `Convert.TryInt` says
whether the text was a number.

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
| `System.Sound` | `Sound` (beeps, tunes, songs), `Speaker` (one speaker by its name) |

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
Program.DroppedEvents -> long              clicks and watch alerts missed while too many calls waited

Process { Id, Name, Host, Running, ExitCode }
Process.Wait()                             parks until the program ends
Process.Wait(long ticks) -> bool           the same, giving up after so many ticks (false)
Process.Kill()
Process.Output() -> List<string>           what it printed
Process.Send(int id, string text) -> bool  a line to a program on this machine; false when it cannot take it

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

## Windows of its own: `System.UI`

A program is not only a stream of printed lines. It can open a window on the desktop of the machine it
runs on, and the machine's own system draws it, so the same program looks like a Frames 95 program on a
Frames 95 machine and like a KDE one on Linux.

```
Window made = new Window("Reactor");
Label heat = new Label("holding at 900");
made.Content = new Column(new Row(heat, new Button("Scram", Scram)), new ProgressBar(0.62));
made.Show();
```

Every widget is an object the program holds, on the program's own heap, and is saved with it: a machine
that is loaded back opens the same windows with the same words in them. `Window`, `Row`, `Column`,
`Label`, `Button`, `TextBox`, `CheckBox`, `ProgressBar`, `ListBox`, `Canvas` and `MessageBox` are what
there is. What a player does reaches the program as a handler, on the program's own thread and in turn
with everything else it does. At most 256 calls wait their turn at once, holding at most 64 KB between
them: a click that finds no room is dropped and counted in `Program.DroppedEvents`, while closing a window
always gets in, ahead of the rest. A window is a thing of the machine, not of the screen: closing the desktop
does not close it, and a program that ends with a window open ends.

## Sound: `System.Sound`

A program plays through the machine it runs on, and what comes out is what the machine is made of.

```
Sound.Beep(880, 200)                              a beep out of the speaker inside the case
Sound.Tones("C4:250 E4 G4 C4+E4+G4:500") -> bool  a tune through the sound card
Sound.Play("Users/Public/Music/intro.ogg") -> bool a song from the disks, out of the monitors and speakers
Sound.Stop()                                      stops the songs the machine's programs are playing
Speaker.Named("Desk left") -> Speaker             the linked speaker of that name, or null
desk.Name                                         its name
desk.Play("Users/Public/Music/alarm.wav") -> bool a song out of that speaker alone
```

A tune is a line of items, each a note by its name (`C4`, `F#3`, `Bb5`), a pitch in hertz (`440`) or a
rest (`R`). Notes played together are joined by `+`, and how long an item lasts, in milliseconds, follows a
colon; an item that does not say lasts as long as the one before it, the first a quarter of a second. A tune
holds at most 1,024 items and 16 notes to a chord, and an item that is no note halts the program, naming
it. A beep is kept between 20 Hz and 20 kHz and to a minute at most.

The notes have the voice of the card: the bright, metallic ring of an FM card, the rounder one of a
wavetable card or of the sound on a Standard board. A machine with no card plays its tunes out of the speaker
in the case, as square notes, and of each chord only the first note. `Play` answers false when the file is
no song, when the server no longer keeps it, or when the machine has nothing to play a recording through: a
Legacy machine needs a sound card, and every machine a monitor linked to it.

Everything the machine plays shares the voices of its sound hardware: 9 on an FM card, 32 on a wavetable
card, 64 on a Standard board, and one in the case. A stereo song takes two voices of a card that plays both
sides and a mono one takes one, each note of a chord takes one, and so does each of the system's chimes. A
sound that finds no voice free takes the voices of the sound that started first, which stops; Soundfoundry
says so when it is its song. A new beep cuts the one before it off. Beeps, tunes and songs follow the
system's volume and mute, and all of them stop when the machine goes off.

## The ComputerCraft side: `Gateway`

A Network Gateway is a device of a computer, linked to it through a peripheral cable, whose other face
is a ComputerCraft peripheral. Where there is one, a program reaches across it.

```
Gateway.Online -> bool                    Gateway.Names() -> List<string>
Gateway.Select(string name) -> bool       Gateway.Current -> string
Gateway.Computers() -> List<CcComputer>   Gateway.Peripherals() -> List<CcPeripheral>
Gateway.Call(string peripheral, string method[, object ...]) -> object
Gateway.TurnOn(long id) / Shutdown(long id) / Reboot(long id) -> bool
Gateway.Send(long id, string text) -> bool
Gateway.OnMessage(Action<GatewayMessage>)
```

`Call` reaches any peripheral on their wired network by the names that side knows it by. A machine with
several Gateways has a program choose with `Select`; one that chooses nothing gets the first.

What a Gateway allows is set on the Gateway itself, in the Gateway Manager or with `gateway <name> set`:
reading the network and running operations. The bridge goes one way for programs: a ComputerCraft
program uses our network through the Gateway, and a program of ours reaches their devices and their
computers' power switches, but never into a computer's own files or prompt.

## Watching the network

```
Network.Watch(string item, Action<StockEvent>) -> Subscription
Network.WatchBelow(string item, long threshold, Action<StockEvent>) -> Subscription
Network.WatchAbove(string item, long threshold, Action<StockEvent>) -> Subscription
```

Being told beats asking. A threshold watch fires on the crossing, not for as long as the number stays
crossed, and disposing the subscription stops it. Handlers run on the program's main thread, in turn.
