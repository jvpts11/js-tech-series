---
section: running
order: 479
---
# Programs and processes

A program is a file that the computer runs for you. You write it in Sigma or Sigma#, and the compiler turns it into a listing (a file that ends in .asm) that the computer runs. See [Compiling with sgsc and scc](jsc:sigma_compile).

While it runs, the program is a process: one entry in the computer's list of running programs, with its own number, its own name and the memory it was given. The list lives on the computer, not on its disks. A program you have installed sits on a disk and does nothing until it starts.

A process leaves the list when it ends. Switching the computer off stops all of them. What happens to them after a save is in [Programs and the world's save](jsc:saved_with_world).

## The number and the name

Each process has a number, and the first is number 1. The computer counts up from 1 and skips every number that a running process still holds. Past the largest number it can hold, it starts again at number 1. A program can give itself a name with Program.SetName. Until it does, its name is sigma.

## Starting a program at the prompt

Once the runtime is installed ([Installing the compilers and the runtime](jsc:sigma_installing)), the prompt knows the sigma command. Its commands are these:

Table: Commands at the prompt

| Command | What it does |
| --- | --- |
| `sigma run <file>` | Starts the program from its file, and passes the words after the file's name to it as arguments. |
| `sigma run <file> --heap 16M` | Sets the memory the program is given, in megabytes: write 16M or 16. |
| `sigma ps` | Lists the running programs, the same as sigma list or a bare sigma. |
| `sigma stop <id>` | Stops the program with that number, the same as sigma kill. |

1. Type sigma run, the file's name and any arguments, then press [[Enter]].
2. Type sigma ps to see the program's number and what it is doing.

A program that stays up in the background is a service. The prompt does not wait for it, and says which number it started as. Any other program keeps the prompt until it returns, and whatever you type goes to that program.

The list has one row per process. With nothing running, the prompt says that no programs are running.

Table: The columns of sigma ps

| Column | What it shows |
| --- | --- |
| id | The program's number, which sigma stop takes. |
| name | The program's name, cut to 20 characters, with a tilde (~) at the end when it is cut. |
| state | What the process is doing, as the table below explains. |
| memory | The memory it holds, out of the memory it was given: bytes below one kilobyte (KB), kilobytes above. |

Table: The states of a process

| State | What it means |
| --- | --- |
| `input` | It stopped on a read and waits for a line you type. |
| `running` | It has instructions left to run. |
| `parked` | It waits for something outside itself, and uses no turns until that is done. |
| `finished` | It ran to the end. |
| `halted` | It stopped because of a mistake, and its message is written to its console. |

A halt is a stop caused by a mistake in the program's own steps. The reasons for each one are in [The stops while a program runs](jsc:sigma_runtime_stops).

## Ending a program

A program ends in one of three ways: it returns from the method where it starts, it calls Program.Exit with a number, or you stop it. The number it ends with is its exit code. A program that returns on its own ends with exit code 0, unless it set another code. A halted program reports exit code 1.

When you stop a program, it gets a goodbye: it may run its own closing steps before it is removed. Its parents are told that it ended. The goodbye has a budget, described in [How much a machine runs each tick](jsc:credits_per_tick).

A finished process leaves the list once nothing waits for it. A process that another program started keeps its output and its exit code until that parent is gone.

## Console and script

A program has one of two shapes, console or script. A console program starts at its entry method, keeps the prompt while it runs, writes to the terminal that started it, and is gone when it returns.

Any other shape is a script. A script is set up once, is called again on every tick while the computer is on, and is told when it is stopped. After the world loads again, it comes back.

## Threads

A process starts with one thread, the main thread, which always has the number 1. A program makes more threads with Thread.Start, and each new thread gets the next number: thread 2, then thread 3, and so on. The threads of a process share its memory, but each one keeps its own place in the calls it is making.

The computer gives the process its turns, and the process shares them among its threads that are ready to run. Each turn is a slice of at most 64 instructions. A thread that waits for something gets no turn until it can go on.

Starting a thread costs 49 instructions on top of the call, because each thread keeps a stack of calls of its own.

Table: Thread calls

| Call | What it does |
| --- | --- |
| `Thread.Start(action)` | Starts another thread, which runs the action you give it. |
| `Thread.Sleep(ticks)` | Waits that many ticks, then goes on. |
| `Thread.Yield()` | Gives up the rest of its turn. |
| `Thread.Join()` | Waits until that thread ends. |
| `Thread.Join(ticks)` | Waits up to that many ticks, and gives false if the thread has not ended by then. |
| `Thread.Stop()` | Ends that thread. |
| `Thread.Id` | The thread's number, which is 1 for the main thread. |

Each object can have a lock. Releasing a lock that you do not hold halts the program. [Threads and locks](jsc:threads_and_locks) explains how to take and release one.

> [!DEFINE] Process
> A program that is running on a computer, with its own number, name and memory.

> [!DEFINE] Tick
> One beat of the game's clock. Twenty ticks make a second.

> [!DEFINE] Instruction
> One simple step of a program's work. Each step a program takes costs one instruction.

## If something goes wrong

> [!PROBLEM] The prompt answers with the file's name and extension, in the form "name: nothing installed runs a .ext".
> No installed language runs files of that kind. Compile a source file with sgsc or scc first, or install the package that runs that kind of file ([the programs page](jsc:programs)).

> [!PROBLEM] The prompt answers with the file's name and "does not compile", and gives no reason.
> The prompt gave no reason. Run the compiler on the file yourself to see its messages. [Reading an error line](jsc:sigma_reading_an_error) shows how to read them.

> [!PROBLEM] The prompt says "<file> is not a <language> program".
> sigma run starts only files that end in .asm, .sgs or .sg. Start one of those instead.

> [!PROBLEM] The prompt answers "sigma: nothing is running as <id>".
> The number is wrong, or that program has already ended. Type sigma ps to see the numbers that are running.

> [!SEE] jsc:sigma_run, jsc:sigma_entry_points
