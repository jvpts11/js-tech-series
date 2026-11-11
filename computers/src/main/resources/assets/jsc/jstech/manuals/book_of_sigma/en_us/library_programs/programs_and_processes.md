---
section: library_programs
order: 443
---
# Programs, processes and messages

A program can start other programs, ask which programs run on its machine, and hear from them. A program you start becomes a process ([](jsc:running_programs)), with its own number, its own memory and its own work to do.

Program is the type for the program itself and for the programs it starts. Program.Start gives you a handle: a value that refers to the new process by its number.

Table: Program members

| Member | What it does |
| --- | --- |
| `Program.SetName(name)` | Names the program. The machine's process list shows that name. A program without a name gets one from the runtime. |
| `Program.Name` | The name the program has, as text. |
| `Program.Args` | The arguments the program was started with, as a list of text. |
| `Program.Exit(status)` | Ends the program with that status. |
| `Program.DroppedEvents` | How many events the program lost, because its queue was full. Sigma# only. |
| `Program.Start(path)` | Starts the program at the path, with optional arguments and priority, and gives a Process. Sigma# only. |
| `Program.Current` | The program itself, as a Process. Sigma# only. |
| `Program.Shell(line)` | Runs one line at this machine's own prompt, waits for it to end, and gives back the lines it printed. Sigma# only. |
| `Program.OnMessage(handler)` | Sets the handler that runs for each message other programs send this one. Sigma# only. |

Program.Start compiles a file of source (.sg or .sgs) before it starts it. A compiled file runs as it is.

Start takes the path, then optional arguments, then an optional priority. The priority is a name: low, medium or high, in any case. Any other name, or none, gives medium. A low program gets a turn only every other round, and a high program runs like a medium one.

A program started from another program gets 1 megabyte (MB) of memory, its heap. The most a heap can be is 64 MB.

## Process

A Process is a handle to a program on a machine. You get one from Start, from Current, and from Processes on another machine.

Table: Process members

| Member | What it does |
| --- | --- |
| `Process.Id` | The number of the process. |
| `Process.Name` | The name of the process, as text. |
| `Process.Host` | Empty on this machine, and the host name of a machine on the network for a remote one. Sigma# only. |
| `Process.Running` | Yes while the process runs. |
| `Process.ExitCode` | The status the process ended with. |
| `Process.Wait()` | Waits until the process ends. |
| `Process.Wait(ticks)` | Waits at most that many ticks. Gives yes when the process ended in that time. |
| `Process.Kill()` | Stops the process. |
| `Process.Output()` | The lines the process printed, as a list. |
| `Process.Send(id, text)` | Sends a message to the process with that number. Gives false when its queue has no room. |

A wait ends early when the program that waits, or the one it waits for, ends. Times are in ticks, and a tick is explained in [](jsc:credits_per_tick).

## Messages and events

A message is text one program sends to another. The receiver gets it as a ProcessMessage, with the number of the sender (ProcessMessage.From), the text (ProcessMessage.Text) and the tick it was sent (ProcessMessage.Tick).

A program does not act on a message at the moment it arrives. Each message, each click, and each change to a watched total waits in a queue as an event. The program runs the handlers in the order the events arrived.

Table: Process states

| State | What it means |
| --- | --- |
| `running` | The process has turns to run. |
| `parked` | The process waits for something outside itself. |
| `finished` | The process ended. |
| `halted` | An error stopped the process. |

A program keeps at most 256 waiting calls. An event that finds no room is dropped and counted in Program.DroppedEvents. A message is the exception: it is refused, and its sender is told. Closing a window always gets in.

> [!DEFINE] Handle
> A value that refers to a process by its number.

> [!DEFINE] Heap
> The memory a program has for its own values. 1 MB by default when another program starts it.

> [!DEFINE] Handler
> The part of your program that runs when an event arrives.

> [!DEFINE] Event
> Something the program is told about: a message, a click, or a change it watches.

## If something goes wrong

> [!PROBLEM] Program.Start stops the program: nothing installed runs a file of that kind.
> Start a compiled program, or compile the source file first.

> [!PROBLEM] Program.Start stops the program: the program needs more memory than is free.
> Close a program to free memory, then start it again.

> [!PROBLEM] The start stops with the compiler's text, because the source file does not compile.
> Read the error the compiler gives, and fix the line it names. The errors are explained in [](jsc:sigma_reading_an_error).

> [!PROBLEM] Process.Output or Process.ExitCode stops the program: there is no process here.
> The process has ended, so it is no longer on the machine. Read its output while the process is still running.

> [!PROBLEM] Process.Send gives false.
> The receiving program has no room in its queue. Send again later.

> [!PROBLEM] The program stops with an error, and its files close.
> Read the error in the console. The program's work stops with that error.

> [!SEE] jsc:running_programs, jsc:threads_and_locks, jsc:sigma_runtime_stops
