---
section: sigma_debugging_section
order: 485
---
# Debugging a program

A debugger runs your program and stops it where you ask: at a breakpoint, which is a line of your source, or a line at a time. While it is stopped you see where it is, the calls it is in, and what its variables hold. You can debug in Virtual Studio, in Virtual Studio Code and at the terminal with sgdb.

The debugger reads lines that only a build for it carries. Build with sgsc --debug at the prompt, with the Debug configuration in Virtual Studio, or with Start Debugging in Virtual Studio Code. Such a build also defines DEBUG, so code inside #if DEBUG is compiled in.

Table: Debugging in the studios

| Key | What it does |
| --- | --- |
| `F9` | Sets or takes away a breakpoint on the caret's line. A click in the margin does the same. |
| `F5` | Builds for the debugger and starts debugging, or goes on to the next breakpoint. |
| `Ctrl+F5` | Runs the program without the debugger. |
| `F10` | Step Over: goes to the next line, running a call on it all the way through. |
| `F11` | Step Into: goes to the next line, into a call. |
| `Shift+F11` | Step Out: goes on until the call it is in returns. |
| `Shift+F5` | Stop Debugging: ends the program at once. |

Virtual Studio shows the variables in Locals and Watch, the calls in Call Stack, and what the program printed in Output. Its Diagnostic Tools show the credits the program has spent, split between the calls it made and the steps themselves. Virtual Studio Code shows the same in its Run and Debug view and its Debug Console. The status bar turns orange while you debug.

Table: The commands of sgdb

| Command | What it does |
| --- | --- |
| `break 11` | Stops the program at line 11 of the file it starts in; a file named before the line, with a colon between, stops it in that file. |
| `run` | Starts the program, or starts it again. |
| `continue` | Goes on to the next breakpoint. |
| `step` | Goes to the next line, into a call. |
| `next` | Goes to the next line, over a call. |
| `finish` | Goes on until the call returns. |
| `print logs` | Shows what the variable logs holds, and its type. |
| `info locals` | Shows the variables of the call it stopped in. |
| `backtrace` | Shows the calls the program is in. |
| `info credits` | Shows the credits the program has spent. |
| `quit` | Ends the program and leaves sgdb. |

Each step spends what running spends: the debugger adds no cost, and a stopped program spends nothing. A program started by a debugger belongs to it, so it ends when you stop debugging or quit sgdb, and it is not saved with the world.

> [!DEFINE] Breakpoint
> A line of the source where the debugger stops the program before it runs it.

> [!DEFINE] Call stack
> The calls a program is in, the one it is running first.

## If something goes wrong

> [!PROBLEM] The program runs past a breakpoint.
> Build it for the debugger, with sgsc --debug or the Debug configuration: a build without the debugger's lines has nowhere to stop.

> [!PROBLEM] sgdb says the program is not being run.
> Type run to start it first.

> [!SEE] jsc:sigma_compile, jsc:call_costs, jsc:sigma_directives
