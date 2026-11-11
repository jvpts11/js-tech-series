---
section: sigma_start
order: 405
---
# Two languages, one family

Sigma and Sigma# are the two programming languages of the computers here. They are one family: everything written in Sigma is also a valid Sigma# program. You learn one, and you can read the other.

> [!DEFINE] Sigma
> The smaller of the two programming languages. Its source files end in .sg, and its compiler is scc.

> [!DEFINE] Sigma#
> The full programming language, which accepts everything Sigma accepts. Its source files end in .sgs, and its compiler is sgsc.

Table: The two languages

| Language | Compiler | Source file ending |
| --- | --- | --- |
| Sigma | `scc` | `.sg` |
| Sigma# | `sgsc` | `.sgs` |

Sigma keeps a [class](jsc:sigma_classes), a struct, an array, a loop and a call, and nothing else of the advanced kind. Sigma# adds what Sigma leaves out, such as threads, collections, interfaces and generics. A program written in Sigma compiles unchanged in Sigma#.

Which one should you choose? Your computer decides first. A Vintage computer can hold only the Sigma compiler, so Sigma is its only language. A Legacy computer or later holds the whole toolchain, and then Sigma# gives you the full library.

A compiler turns a source file into a listing: the assembly text that a computer runs, in a file that ends in .asm. Each listing is built for one instruction set.

> [!DEFINE] compiler
> A program that turns source text into a listing. When the source has an error, it prints the errors and writes nothing.

> [!DEFINE] listing
> The assembly text a compiler writes, in a file that ends in .asm.

> [!DEFINE] source file
> A text file that holds a program in Sigma or Sigma#, written before it is compiled.

> [!DEFINE] project
> A folder that holds the source files of one program, and the settings that build it.

The runtime runs the listings. It is a package of its own, and once it is installed it gives the computer the sigma command ([](jsc:sigma_installing)).

> [!DEFINE] runtime
> The program that runs listings on a computer, and that gives the computer the sigma command.

Each computer runs one instruction set (ISA): the basic operations its processor knows. A listing is built for one set, and a computer runs it only if its processor knows that set. This mod has three: IA-16, x86 and x86-64.

> [!DEFINE] instruction set (ISA)
> The basic operations a processor knows. A listing is built for one set, and a computer runs it only if its processor knows that set.

Table: The three instruction sets

| Name | Width | Runs listings built for |
| --- | --- | --- |
| `IA-16` | 16 bits | IA-16 only |
| `x86` | 32 bits | x86 and IA-16 |
| `x86-64` | 64 bits | x86-64, x86 and IA-16 |

A program built for the oldest instruction set keeps working on every newer one. That is why IA-16 listings run everywhere, and x86-64 listings run only on x86-64 machines.

Every computer also has an era, its hardware generation: Vintage, Legacy, Transition, Standard, Advanced, Exa or Singularity. The era decides which instruction set the computer has, and which software it can install. The eras are explained in [](jsc:eras).

> [!DEFINE] era
> The hardware generation of a computer. The era decides its instruction set and which software it can install.

Table: The instruction set of each era

| Era | Instruction set |
| --- | --- |
| Vintage | `IA-16` |
| Legacy | `x86` |
| Transition, Standard, Advanced, Exa and Singularity | `x86-64` |

When you do not choose an instruction set, the build takes the oldest one the language runs on: IA-16 for Sigma, and x86 for Sigma#. So a Sigma# program built by default runs from a Legacy computer up, and never on a Vintage one.

Sigma has a smaller library than Sigma#. Its classes live in the namespace Standard, with Console, File, Math and Convert among them. The chapter [](jsc:sigma_library) lists what each one does.

Table: What Sigma leaves out

| Missing in Sigma | What the compiler suggests you write |
| --- | --- |
| Interfaces | A class whose methods are virtual, and build on it instead. |
| Records | A struct with the same fields. |
| Abstract classes | A class with virtual methods that do nothing, and build on that. |
| Abstract methods | Give it a body that does nothing, and write it virtual. |
| Delegates | Call the method by name, or keep the object and call it through that. |
| Events | Keep the object that wants telling, and call a method on it. |
| Properties | A field, or a method that gives the value back. |
| Generics, such as List<T> | An array, or a class that holds exactly what you need. |
| List | An array, and a count of your own beside it. |
| Map | Two arrays, or one array searched for the key. |

## If something goes wrong

> [!PROBLEM] The build stops with "IA-16 runs Σ only; write it in Σ, in a Σ project".
> Write the program in Sigma, in a Sigma project. Or set its platform target to x86 or x86-64.

> [!PROBLEM] A listing is refused: "built for x86; this machine is IA-16".
> The computer is a Vintage one, which runs only IA-16 listings. Build with scc, which builds for IA-16, or run the listing on a Legacy computer or later.

> [!SEE] jsc:sigma_language
