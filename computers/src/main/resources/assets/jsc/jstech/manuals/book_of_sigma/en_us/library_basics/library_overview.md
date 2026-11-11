---
section: library_basics
order: 437
---
# The library, and the two languages

A program reaches the machine through the library. The library is a set of named types, and each type has members you call, such as the terminal, files, the clock and the network.

Sigma and Sigma# both use the library, but they do not reach the same part of it. Sigma is the smaller language, and it reaches only the library's Standard namespace. Sigma# is the whole language, and it reaches every System namespace, such as System.IO and System.Sound.

Every name in Standard is a type Sigma# also has, under the namespace it has always had. A using line, explained in [](jsc:sigma_namespaces_using), brings a namespace in.

## Bringing names into a program

1. Write using Standard.\*; at the top of the file, to bring in everything in Standard.
2. Below the using line, write namespace, then a name of your own and a semicolon, such as namespace MyProgram;.
3. Write the entry point of your kind of program, as the table below shows.

Table: Where a program starts

| Kind of program | What you write |
| --- | --- |
| A console program | `static void Main()` |
| A script | A class that extends Script, with OnInit, OnTick or OnDestroy written in it. |

Script is a base class in Standard. Its three methods do nothing until your class writes its own.

Table: The two languages

| Language | Names it reaches | Compiler | Source file |
| --- | --- | --- | --- |
| Sigma | Standard | `scc` | `.sg` |
| Sigma# | Every System namespace, such as System.IO | `sgsc` | `.sgs` |

A version is the level of the language a build is held to. There are two versions, 1 and 2, and version 2 is the newest. A build that asks for no version is held to the newest one.

This book marks each name that came in version 2. A version 1 build refuses those names, so a program that uses them needs the newest version.

## Names Sigma refuses

Sigma refuses some names that Sigma# has. The compiler names the replacement it expects you to use.

Table: Names Sigma refuses

| Name | What to use instead |
| --- | --- |
| `List` | An array, and a count of your own beside it. |
| `Map` | Two arrays, or one array you search for the key. |
| `Thread` | Nothing: these machines run one thing at a time. |

A member that Sigma does not offer is refused too. The compiler lists the members the type does have, in alphabetical order, so you can pick one of them.

> [!DEFINE] Namespace
> A group of types with a dotted name, such as System.IO. A using line brings it in, so its types can be written by bare name.

> [!DEFINE] Library
> The set of types and their members that a program can call, such as Console, File and Network.

> [!DEFINE] Member
> A method, value or event that belongs to a type.

> [!DEFINE] Version
> The level of the language a build is held to: 1 is the first level, and 2 is the newest.

> [!DEFINE] Subset
> The smaller language, Sigma, which offers fewer names than Sigma#.

## If something goes wrong

> [!PROBLEM] The compiler says a name is in a namespace you did not bring in, and it asks for a using line.
> Add the using line it names at the top of the file: using System.IO.\*; for everything in the namespace, or using System.IO.<name>; for one type.

> [!PROBLEM] The compiler says a name is a namespace and refuses it alone.
> Write .\* after the namespace to bring in everything in it.

> [!PROBLEM] The compiler says Sigma has no System library, and that everything Sigma has is in Standard.
> Write using Standard.\*; and use the names it holds, or write the program in Sigma#.

> [!PROBLEM] The compiler refuses List, Map or Thread.
> Use the replacement in the table of names Sigma refuses, or write the program in Sigma#, which has all of them. Threads have their own page ([](jsc:threads_and_locks)).

> [!PROBLEM] The compiler refuses a member that Sigma does not offer.
> Use one of the members the compiler lists in its message.

> [!PROBLEM] A version 1 build refuses a name that came in version 2.
> Use the newest version of the compiler, or write the program without that name.

> [!SEE] jsc:sigma_first_program, jsc:sigma_reading_an_error
