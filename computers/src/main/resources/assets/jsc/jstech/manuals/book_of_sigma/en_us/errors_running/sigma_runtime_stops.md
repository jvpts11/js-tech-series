---
section: errors_running
order: 492
---
# The stops while a program runs

A program can stop while it runs, when it meets an error it cannot go on from. This book calls that kind of stop a halt, and there are 15 of them. The program ends its run at once.

> [!DEFINE] halt
> An error during a program's run, which stops the program at once and writes one message to its console.

When a halt happens, the program's threads stop and the program lets go of its locks. The message is all you see of the stop. Most messages do not say which line of the program caused it.

The table below numbers the 15 stops, so you can find each one. The game shows you only the message, not the number. In this chapter a program's memory is counted in megabytes (MB).

Table: The 15 stops

| Stop | What causes it | What to do |
| --- | --- | --- |
| 1 | A division or a remainder by zero, with whole numbers or with real numbers. | Check the number you divide by before the division. |
| 2 | A value is nothing where an object, a list, a map, an array, a thread or a process is needed. | Make sure the value is set before you use it. |
| 3 | An object was disposed and then used again. | Do not use an object after it is disposed. |
| 4 | The program holds more memory than it was given. | Make it hold fewer or smaller values, or give it more memory when it starts, up to 64 MB. |
| 5 | A conversion gets a text or a value of the wrong kind. | Check the text you convert: a number conversion needs a number, and a true or false conversion needs true or false. |
| 6 | A number, a place or a length is outside the range it may have. | Use a value inside the limit the message gives. |
| 7 | A name is wrong for that kind of value: the value has no such member. | Use a member that the value has. The message names the value and the member. |
| 8 | A program calls the network, but the computer is not on one. | Join the computer to a network first. |
| 9 | The thread lets go of a lock it does not hold. | Take the lock before you let it go. |
| 10 | The machine cannot start the program: no runner exists for that kind of file, there is not enough free memory, or the file cannot be read or started. | The message says why. Compile a source file first, free some memory on the machine, or check the file. |
| 11 | A value given to a component breaks a rule, or a computer is powered off. | The message gives the rule. Keep the value within it, and check the file or the computer. |
| 12 | A method calls itself without end, so the calls go too deep. | Give the recursion an end, or use a loop. The message names the method. |
| 13 | The runtime itself failed while it ran a step of the program. | This is not your program's fault. Report the line from the server log. |
| 14 | A Sigma# program threw an exception, and nothing caught it. | Catch it where the program can go on. The lines under the message say where it was thrown. |
| 15 | A pointer was read or written outside every block of raw memory: past its end, in a block already freed, or through null. | Check the index and the size you asked for, and use a block only until it is freed. |

In the messages below, the words in angle brackets stand for the names and the numbers the game fills in.

Table: The messages of the stops

| Stop | The message as the player sees it |
| --- | --- |
| 1 | divided by zero |
| 1 | took the remainder of a division by zero |
| 2 | there is no handler to call |
| 2 | there is no list here |
| 2 | there is no map here |
| 2 | there is no array here |
| 2 | there is no object to write <field> on |
| 2 | there is nothing here to reach into |
| 2 | there is no body to run on the thread |
| 2 | there is no handler to call for <name> |
| 2 | there is no <thing> here to <action> |
| 2 | there is no process here |
| 2 | there is no thread here |
| 3 | this was disposed and cannot be used |
| 4 | out of memory: <live> of <total> bytes are live and <asked> more were asked for |
| 4 | line <line> holds <bytes> bytes, one such part for each line that holds memory, joined by semicolons |
| 5 | this is not a <type> |
| 5 | there is nothing here to make a <type> |
| 5 | '<text>' is not a number |
| 5 | '<text>' is not true or false |
| 6 | there is no base <base>; a number is written in a base from 2 to 36 |
| 6 | an array cannot have <count> places |
| 6 | a program may have <count> windows open at once |
| 6 | there is no place <place> to start from in a string of <length> |
| 6 | there are no <count> characters from place <place> in a string of <length> |
| 6 | a window holds at most <count> widgets |
| 6 | a window holds at most <count> widgets, counting the ones inside rows and columns |
| 6 | a row or a column holds at most <count> widgets |
| 6 | a list holds at most <count> rows |
| 6 | a canvas holds at most <count> strokes |
| 6 | a menu holds at most <count> entries |
| 6 | a tab view holds at most <count> pages |
| 6 | a table has at most <count> columns |
| 6 | a status bar shows at most <count> sections |
| 7 | the runtime does not answer for <name> |
| 7 | there is no <name> to call |
| 7 | <name> has no body to run |
| 7 | <name> has no <member> |
| 7 | there is no <member> to read here |
| 7 | <operator> does not apply to a real number |
| 7 | <name> has no <member> to run |
| 7 | this computer has no desktop to open a window on |
| 7 | there is no <name> to run on the thread |
| 8 | this computer is not on a network |
| 9 | this thread is letting go of a lock it does not hold |
| 10 | <name>: nothing installed runs a program of this kind (compile a source file first) |
| 10 | <name>: <size> MB will not fit in <free> MB of free memory |
| 10 | <name> cannot run programs |
| 11 | a component's value nests at most <count> deep |
| 11 | a component's value has at most <count> parts |
| 11 | a component's value weighs at most <count> bytes |
| 11 | a component holds values, not handlers |
| 11 | a component holds values, and a <type> is not one |
| 11 | a value that holds itself cannot be handed to a component |
| 11 | '<name>' is closed |
| 11 | '<name>' was opened with "<mode>", which does not read |
| 11 | '<name>' was opened with "<mode>", which does not write |
| 11 | '<text>' is no printf hole this machine writes |
| 11 | <name> is powered off |
| 11 | <name> does not take programs from other computers |
| 12 | calls went <count> deep calling <name>: a method may be calling itself without end |
| 13 | the runtime could not carry this out (<exception>) |
| 2 | there is nothing here to throw |
| 4 | a block of <bytes> bytes is more than one block may hold |
| 6 | a count of bytes cannot be negative: <count> |
| 14 | an exception of the type <type> was thrown, and nothing caught it, then its message and a line for each call it went through |
| 15 | attempted to read or write protected memory, or in Sigma the system's own words: Segmentation fault (core dumped) on Linux, the BSDs and Quince OS X, This program has performed an illegal operation and will be shut down on Frames 95, and Sorry, a system error occurred on the classic Quince systems |

Stops 10 and 11 can also show a message that the machine writes itself, for a bus, a crafting interface, a file or an [IQL](jsc:iql_getting_started) query. IQL, the Item Query Language, is the network's query language. Those messages come from the part of the machine that ran the step, so they are not in the table.

Stop 13 is the one that is not your program's fault. The runtime failed, and a line goes to the server log with the line of the program where it happened.

```table
{"caption": "Limits", "heads": ["Limit", "Amount"], "rows": [[{"words": "Calls deep on one thread"}, {"words": "1,024 frames"}], [{"words": "Stack places in one thread"}, {"words": "1,048,576 places"}], [{"words": "Memory of one program, by default"}, {"amount": 1, "unit": "MB"}], [{"words": "Memory of one program, at most"}, {"amount": 64, "unit": "MB"}], [{"words": "Nesting of a value given to a component"}, {"words": "16 levels"}], [{"words": "Parts of a value given to a component"}, {"words": "4,096 parts"}], [{"words": "Size of a value given to a component"}, {"words": "32,768 bytes"}], [{"words": "Windows open at once in one program"}, {"words": "8 windows"}], [{"words": "Widgets in one window"}, {"words": "256 widgets"}]]}
```

The stack places are counted as the frames times the places each frame uses. The memory you ask for is kept between 1 MB and 64 MB.

> [!DEFINE] frame
> One active method call on a thread. A thread keeps one frame for each call that has not returned yet.

## If something goes wrong

> [!PROBLEM] It says divided by zero.
> Check the number you divide by, before the division.

> [!PROBLEM] It says out of memory.
> Make the program hold fewer or smaller values, or give it more memory when it starts, up to 64 MB.

> [!PROBLEM] It says calls went deep, and a method may be calling itself without end.
> Give the calling method an end, or use a loop instead.

> [!PROBLEM] It says a text is not a number, or not true or false.
> The text you convert is not a number, nor true or false. Check the input.

> [!PROBLEM] It says a number, a place or a length is out of range, such as "a window holds at most 256 widgets".
> Use a value inside the limit the message gives.

> [!PROBLEM] It says there is no such member, or the runtime does not answer for a name.
> The name is wrong for that kind of value. Use a member the value has.

> [!PROBLEM] It says this computer is not on a network.
> Join the computer to a network first.

> [!PROBLEM] It says this thread is letting go of a lock it does not hold.
> Take the lock before you let it go.

> [!PROBLEM] It says nothing installed runs a program of this kind.
> Compile the source file first, so that a listing exists to run.

> [!PROBLEM] It says the runtime could not carry this out.
> This is not your program's fault. Report the line from the server log.

> [!SEE] jsc:sigma_listing_errors, jsc:sigma_compile_errors, jsc:sigma_running
