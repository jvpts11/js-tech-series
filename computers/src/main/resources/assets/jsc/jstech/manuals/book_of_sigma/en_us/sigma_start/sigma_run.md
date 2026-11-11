---
section: sigma_start
order: 409
---
# Running programs

A listing is ready to run, and the sigma command runs it. The runtime starts the program, keeps it in its list of running programs, and lets you stop it again.

Table: The sigma command

| Command | What it does |
| --- | --- |
| `sigma run <file> [--heap <n>M] [arguments]` | Starts a program. The word start does the same. |
| `sigma build <source> [--isa <isa>[,<isa>...]]` | Builds a program for one instruction set, or for several at once ([](jsc:sigma_compile)). |
| `sigma stop <id>` | Stops a running program. The word kill does the same. |
| `sigma ps` | Lists the running programs. The word list does the same, and so does sigma alone. |

## Running a program

1. Compile the source file, or have a listing ready.
2. Type sigma run hello.asm and press [[Enter]].
3. Type sigma ps to see the program in the list.
4. Type sigma stop 3 with the id from the list to stop it.

Table: The files sigma run takes

| File ending | What happens |
| --- | --- |
| `.asm` | A listing. The runtime starts it. |
| `.sgs or .sg` | A source file. The runtime compiles it first, then runs it. |
| Any other ending | Refused: "hello.txt is not a Σ# program". |

When you run a source file, the runtime compiles it first. It uses the newest language version and the default instruction set of its language. The settings of a project do not apply to that compile.

Before a listing starts, the runtime makes three checks. It checks that the listing fits the computer's instruction set, or that the computer's system carries a translator for it, which runs it more slowly. It checks the listing's assembly version, which is refused when it is too old or too new. And it checks that the memory of the program fits in the free memory of the computer.

A running program gets its own memory, called the heap. The heap is measured in megabytes, and you set it with --heap.

> [!DEFINE] heap
> The memory a running program is given, measured in megabytes.

```table
{"caption": "The heap", "heads": ["Setting", "Value"], "rows": [[{"words": "Default"}, {"amount": 1, "unit": "MB"}], [{"words": "Largest possible"}, {"words": "64 MB. A larger request is cut to 64 MB, with no message."}], [{"words": "Writing it"}, {"words": "--heap 16M, --heap 16MB and --heap 16 all mean 16 megabytes."}]]}
```

A value that is not a number counts as zero, and zero gives the default. A --heap that is the last word is not a setting: it goes to the program as an argument.

Everything after the file, except --heap and its number, goes to the program. A Sigma program reads these words with Program.Args ([](jsc:programs_and_processes)). A Sigma# program can take them as a string array in Main, if its project is version 2.

Table: What sigma ps shows

| Column | What it shows |
| --- | --- |
| id | The number that sigma stop takes. |
| name | The program's name, cut to 20 characters, with a ~ when it is cut. |
| state | running, parked (it waits for something outside it), finished, halted (it stopped on a mistake), or input (it waits for the keyboard). |
| memory | The memory the program holds, out of the heap it was given, in KB (or in bytes when it is under 1,024). |

With no program running, the list says so. Here is that answer:

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sigma ps", "typed": true}, {"text": "no Σ# programs are running"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Stopping a program answers stopped with its id, such as "stopped 3". An id that is not running answers "sigma: nothing is running as 3".

The full page of the sigma command follows.

```command
id: jsc:command/linux/sigma
```

## If something goes wrong

> [!PROBLEM] "hello.asm: 16 MB will not fit in 8 MB of free memory" appears.
> Lower the --heap value, stop another program with sigma stop, or free memory on the computer.

> [!PROBLEM] "built for x86; this machine is IA-16" appears.
> Build with scc, or run the listing on a Legacy computer or later.

> [!PROBLEM] A message names the listing's assembly version and the version this runtime reads.
> Compile the source file again, with the compiler that is installed.

> [!PROBLEM] "hello.txt is not a Σ# program" appears.
> Run a file that ends in .asm, .sgs or .sg.

> [!PROBLEM] "hello.sgs does not compile" appears.
> Compile the source with sgsc or scc, and read the errors it names.

> [!SEE] jsc:sigma_running, jsc:sigma_compile
