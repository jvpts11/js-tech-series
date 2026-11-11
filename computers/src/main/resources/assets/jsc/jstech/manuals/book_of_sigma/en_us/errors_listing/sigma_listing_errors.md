---
section: errors_listing
order: 491
---
# Listing errors

A listing is the text file the compiler writes for a program. The machine reads the listing when the program starts, and a listing problem is a line it cannot read. A listing problem stops the program from starting, and the machine shows you the first problem only.

The machine shows the program's name, a colon, and then the problem line. For example: "<program>: (4,1): error A4003: 'nonsense' is not an instruction". A listing problem always shows column 1.

Listing problems come from three causes: a listing you edited by hand, a listing made by another compiler version, and a machine that does not run the instruction set the program was built for. A listing that the editor builds cleanly shows no listing problem.

The runtime reads listing versions 2, 3 and 4. A listing with no version line gives A4001, and the reader stops there. A version line with no number is read as the newest version. After a version problem, the reader reads no further, so that one problem is the only one shown.

> [!DEFINE] label
> A name written in front of a line of a method, which a jump can point to.

> [!DEFINE] directive
> A line that starts with a dot, such as .arch, .field or .method, and names a part of the program.

> [!DEFINE] operand
> The thing written after an instruction's name, such as a number or a name.

> [!DEFINE] place
> One numbered slot in a method, where it keeps one value while it runs.

> [!DEFINE] slots line
> The line that says how many places a method has.

Table: Problems in the text of the listing

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `A4001` | the listing has to begin with a version line | The first line is not a version line. Compile the source again. |
| `A4002` | this runtime reads assembly version <version>, and this listing is version <version> | The listing came from a newer compiler than this runtime reads, so it needs a runtime that reads its version. |
| `A4003` | '<name>' is not an instruction | The name is not an instruction of the listing format. Check the spelling, or compile the source again. |
| `A4004` | '<name>' needs something after it | An instruction needs an operand and has none. Write the operand, or compile the source again. |
| `A4005` | '<name>' takes nothing after it | An instruction has an operand it does not take. Remove the operand. |
| `A4006` | '<text>' is not what '<name>' takes | The operand is the wrong kind of value for that instruction or directive. |
| `A4007` | '<name>' is not a line this format has | A line starts with a dot and names no line the format has. Check the spelling. |
| `A4008` | an instruction has to be inside a method | An instruction line stands outside any method. Put it inside a method. |
| `A4009` | '<name>' has to be inside a type | A directive stands outside any type. Put it inside a type. |
| `A4010` | nothing in this method is labelled '<name>' | A jump points to a label that the method does not have. Check the spelling, or add the label. |
| `A4012` | this listing is assembly version <version> and this runtime reads version <version> and later: compile its source again | The listing is older than the runtime reads. Compile the source again with the current compiler. |
| `A4016` | this method has no slots line, so it has no body | The method does not say how many places it has. Compile the source again. |
| `A4017` | place <place> does not exist: this method has <count> | An instruction uses a place the method does not have. Use a place inside the count the message gives. |
| `A4018` | '<name>' labels more than one line of this method | Two lines of one method have the same label. Keep one label for each line. |
| `A4019` | '<name>' came in version <version> of the format, and this listing is written in version <version> | A listing holds an instruction or a line of a newer format than its version line says. Compile the source again, which writes the version it needs. |

Table: Problems with the program and the machine

| Code | Message | What it means and the fix |
| --- | --- | --- |
| `A4013` | nothing answers '<name>' | A call, a new object or a field names something the machine does not have. Check the spelling of the name. |
| `A4014` | '<name>' can be read but not written | The program writes to a property that can only be read. Read its value, and do not write to it. |
| `A4015` | built for <instruction set>; this machine is <instruction set> | The program was built for another instruction set than this machine runs, so build it for this machine's instruction set or run it on a machine that runs that set. |

The problem with A4015 is different. The listing is fine, and the machine is the wrong one. The names that A4013 refers to are the ones described in [the library](jsc:sigma_library).

## If something goes wrong

> [!PROBLEM] The machine shows an A4001 problem, a missing version line.
> A listing edited by hand can lose its version line. Compile the source again.

> [!PROBLEM] The machine shows an A4002 problem, a listing newer than the runtime reads.
> The listing needs a runtime that reads its version. Use a runtime that does.

> [!PROBLEM] The machine shows an A4012 problem, a listing older than the runtime reads.
> Compile the source again with the current compiler.

> [!PROBLEM] The machine shows an A4015 problem, which says the program was built for another instruction set.
> Build the program for this machine's instruction set, or run it on a machine that runs it.

> [!SEE] jsc:sigma_runtime_stops
