---
section: running_limits
order: 482
---
# The limits of a running program

A running program has limits. They stop one program from taking all of a computer's memory or all of its time. Most limits are fixed. Two of the time limits can be changed in the config file jstech-balance.toml.

Memory is the first limit. Each program has a heap: the memory it holds for the objects it makes, such as text and lists. Every object counts against the memory the program was given. Without --heap, a program gets 1 MB. The most you can give it is 64 MB.

Table: Memory

| Limit | Value | What happens past it |
| --- | --- | --- |
| Memory given to a program with --heap | 1 MB by default, 64 MB at most | A larger number is cut down to 64 MB. |
| Memory free on the computer | The computer's memory, less what is in use | The program does not start, and the prompt says it will not fit. |
| Text in the heap | 16 bytes, plus 2 bytes for each character | It counts against the heap. |
| Each object in the heap | 16 bytes, plus 8 bytes for each link to another object | It counts against the heap. |
| A message or a click | 40 bytes, plus its text | It counts against the heap of the program that receives it. |

The config file jstech-balance.toml holds two keys: balance.program_machine_micros, for one computer, and balance.program_server_micros, for all the computers together. Both are times in microseconds.

Table: Time and turns

| Limit | Value | What happens past it |
| --- | --- | --- |
| Instructions in one turn of a thread | 64 | The turn passes to the next thread, which is not a failure. |
| Credits in one round of a tick | 64 | The round passes to the next program, which is not a failure. |
| Work between two checks of the clock | 512 units | The tick ends early when its time runs out. |
| Time per tick, for one computer | 1,000 microseconds (1 millisecond) by default; from 50 to 1,000,000 microseconds in the config | The computer stops its programs for the rest of that tick. |
| Time per tick, for all computers together | 8,000 microseconds (8 milliseconds) by default; from 100 to 10,000,000 microseconds in the config | No computer runs its programs in that tick, and the computers go first on the next tick. |
| Farewell budget per tick | 4,096 instructions, shared | A program over its share stops before it can run its farewell code. |
| Calls one inside another | 1,024 | The program halts, and the message says that a method may be calling itself without end. |

Table: Calls and consoles

| Limit | Value | What happens past it |
| --- | --- | --- |
| Calls waiting for a program | 256 calls | The new call or message is refused. |
| Text of the calls waiting for a program | 64 KB (65,536 bytes) | The new call or message is refused. |
| Lines typed ahead for a program | 16 lines | The typed line is not taken. |
| Lines kept on a console | 200 lines | The oldest lines are dropped. |
| Characters kept on a console | 64,000 characters | The oldest lines are dropped. |
| Length of one console line | 4,096 characters | The line is cut short, and the cut is marked with dots. |
| Windows a program may have open | 8 | The program halts, saying that it may have 8 windows open at once. |
| Folders a computer shares | 16 folders | The prompt says that the computer already shares that many folders. |

Table: Windows

| Limit | Most allowed | What happens past it |
| --- | --- | --- |
| Widgets in a window | 256 | The window refuses the change. |
| Rows in a table widget | 1,024 | The window refuses the change. |
| Columns in a table widget | 8 | The window refuses the change. |
| Tabs in a tab view | 64 | The window refuses the change. |
| Sections in a widget | 4 | The window refuses the change. |
| Points in a drawing | 120 | The window refuses the change. |
| Lines in a log view | 256 | The window refuses the change. |
| Strokes in a canvas | 4,096 | The window refuses the change. |
| Area of a canvas | 4,096 | The window refuses the change. |
| Entries in a menu | 64 | The window refuses the change. |
| Size of a window | 640 wide and 360 tall at most; each side at least 60 | The window refuses the change. |

> [!DEFINE] Heap
> The memory a running program holds for the objects it makes, such as text and lists. Its size is the memory the program was given.

## If something goes wrong

> [!PROBLEM] The prompt says that the program will not fit in the free memory.
> Close a program or a window, add memory to the computer ([Memory](jsc:memory)), or start the program with a smaller --heap.

> [!PROBLEM] The program halts with a message that begins "out of memory:" and names the lines that hold the most memory.
> The program holds too much at once. Make it hold less, or give it more with --heap, up to 64 MB.

> [!PROBLEM] A console loses its oldest lines.
> A console keeps 200 lines or 64,000 characters. Print less, or read the output sooner.

> [!PROBLEM] The program halts, saying that it may have 8 windows open at once.
> Close one of the windows before you open another.

> [!PROBLEM] A program stops for some ticks on a busy server.
> The time allowed per tick is used up. Raise the balance keys in jstech-balance.toml, if the server can spare the time.

> [!SEE] jsc:memory, jsc:credits_per_tick, jsc:sigma_runtime_stops
