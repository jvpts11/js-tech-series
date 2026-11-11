---
section: examples_intro
order: 501
---
# Running the examples

Every example in this chapter is a whole program in Sigma#, the full language. Each one is a single file, and you can type it in, build it and run it on a computer of the mod.

An example starts with using lines, which bring the library's names into the file. Its class has a method called Main, and the program starts there ([](jsc:sigma_entry_points)).

## Building and running an example

1. Save the program as a file that ends in .sgs, such as Buses.sgs.
2. Type sgsc Buses.sgs at the prompt and press [[Enter]].
3. Read the answer. When it says wrote Buses.asm, the program is built.
4. Type sigma run Buses.asm and press [[Enter]] to start the program.

The compiler writes the built program beside the source file, with the ending .asm. After a good build it prints wrote Buses.asm, then a line that begins run it with: sigma run Buses.asm. The compiler is explained in ([](jsc:sigma_compile)).

## The options of the compiler

Table: Options of sgsc

| Option | What it does |
| --- | --- |
| `-o <out.asm>` | Names the built file, in place of the source's name with .asm. |
| `--arch <isa>` | Chooses the instruction set. Without it, the default jsc:x86 is used. |
| `--lang <version>` | Chooses the language version the program is checked against: 1 or 2. |

The version decides which names a program may use. A program that uses a name from version 2 needs version 2. The table below gives the version each example needs.

Table: The examples

| Example | Source file | Version needed |
| --- | --- | --- |
| Network | `Network.sgs` | 1 |
| Buses | `Buses.sgs` | 2 |
| Crafting | `Crafting.sgs` | 2 |
| Machine | `Machine.sgs` | 1 |
| Redstone | `Redstone.sgs` | 2 |
| Sound | `Sound.sgs` | 2 |
| Gateway | `Gateway.sgs` | 1 |
| Values | `Values.sgs` | 1 |
| Steps | `Steps.sgs` | 1 |
| Programs | `Programs.sgs` | 1 |
| Old names | `OldNames.sgs` | 2 |
| Windows | `Ui.sgs` | 1 |
| Widgets | `Widgets.sgs` | 2 |

> [!DEFINE] Language version
> A numbered stage of Sigma#. A later stage adds names, and a program that uses one of them needs that stage.

## If something goes wrong

> [!PROBLEM] The compiler prints an error and says nothing was written.
> Fix the first error it prints, then build the file again. A file with an error gets no .asm file.

> [!PROBLEM] The message reads "'abc' is not a version of Σ#; there are 1 to 2", because the text after --lang is not a version.
> Type 1 or 2 after --lang.

> [!PROBLEM] The compiler refuses a name that the chosen version does not have, with code S3057.
> Build with the version the name needs, or take the name out of the program.

> [!PROBLEM] The compiler says a name is in a namespace and asks for a using line, with code S3040.
> Add the using line it asks for above the namespace line, then build again.

> [!SEE] jsc:sigma_compile, jsc:sigma_run, jsc:sigma_errors
