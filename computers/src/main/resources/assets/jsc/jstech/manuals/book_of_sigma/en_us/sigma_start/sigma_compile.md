---
section: sigma_start
order: 408
---
# Compiling with sgsc and scc

Compiling turns your source file into a listing that a computer can run. You compile on the command line with the compiler of your language: sgsc for Sigma#, and scc for Sigma.

Table: The two compilers

| Command | Language | Instruction set by default |
| --- | --- | --- |
| `sgsc` | Sigma# | `x86` |
| `scc` | Sigma | `IA-16` |

Both commands take the same options. Type the command with no file, and it shows its usage line.

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sgsc", "typed": true}, {"text": "usage: sgsc <file.sgs> [more.sgs ...] [-o <out.asm>] [--arch <isa>] [--lang <version>]"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Table: The options of sgsc and scc

| Option | What it does |
| --- | --- |
| `-o <file>` | Names the listing it writes; without it, the listing takes the first source file's name with .asm in place of its ending. |
| `--arch <isa>` | Builds for an instruction set: IA-16, x86, x86-64, 68k, PowerPC or ARM64, in any letter case; without it, the language's default applies. |
| `--lang <version>` | Holds the build to a language version, 1 or 2; without it, the newest version the installed compiler knows is used. |

The full page of each compiler follows.

```command
id: jsc:command/linux/sgsc
```

```command
id: jsc:command/linux/scc
```

A successful compile prints the compiler's name and version first, then what it wrote. Here it compiles a file called hello.sgs:

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sgsc hello.sgs", "typed": true}, {"text": "Σ# Compiler 2.0"}, {"text": "sgsc: wrote hello.asm"}, {"text": "run it with: sigma run hello.asm"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Compiling does not run the program. The last line says how to run it, with the sigma command ([](jsc:sigma_run)).

Table: How an error reads

| Part | What it means |
| --- | --- |
| hello.sgs | The source file that has the error. |
| (3,5) | The line and the column where the problem is. |
| error CODE: message | The kind of message, a code that names the problem, and its words. |

A warning looks the same, but it says warning instead of error, and it is shown in dim text. When the build fails, the compiler prints the number of errors last, such as "sgsc: 1 error, nothing was written". You get no listing from a failed build.

## Building for several instruction sets

A program runs only on the line of processors it was built for. Quince's machines have lines of their own (68k, PowerPC and ARM64), so a program for them and for the others is built with sigma build, the runtime's own command. With several instruction sets after --isa, it makes a universal build: one listing for each, in one file, and every machine runs the listing for its own processor. The file is as big as all of them together.

Table: The sigma build command

| Command | What it does |
| --- | --- |
| `sigma build <source> [-o <out.asm>] [--isa <isa>[,<isa>...]] [--lang <version>]` | Builds a source file for the instruction sets named, or for its language's default with no --isa. The source's ending names the language. |

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "sigma build hello.sgs --isa powerpc,x86-64", "typed": true}, {"text": "sigma: wrote hello.asm"}, {"text": "a universal build for PowerPC, x86-64, which runs on each"}, {"text": "run it with: sigma run hello.asm"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

## If something goes wrong

> [!PROBLEM] "sgsc: 1 error, nothing was written" appears, or the same with more errors.
> Read each error above it, fix the line it names, and compile again.

> [!PROBLEM] The compiler prints the command's name and a read error when a source file is missing.
> Check the file's name, and that the file is in the folder you named.

> [!PROBLEM] The usage line appears, with the options listed.
> Give at least one source file.

> [!PROBLEM] "IA-16 runs Σ only; write it in Σ and build it with scc" appears.
> Build the Sigma program with scc, or choose x86 or x86-64 with --arch.

> [!PROBLEM] "no instruction set is called" appears, followed by the names there are.
> Use one of the names IA-16, x86, x86-64, 68k, PowerPC or ARM64.

> [!PROBLEM] "'3' is not a version of Σ#; there are 1 to 2" appears.
> Use 1 or 2 after --lang.

> [!PROBLEM] "this compiler knows Σ# 1 at most; Σ# 2 needs a newer one" appears.
> Update the compiler package, or use a lower version after --lang.

> [!SEE] jsc:sigma_errors, jsc:sigma_run
