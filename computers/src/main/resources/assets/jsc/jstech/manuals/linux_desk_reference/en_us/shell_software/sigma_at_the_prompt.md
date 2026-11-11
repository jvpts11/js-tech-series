---
section: shell_software
order: 261
---
# Sigma at the prompt

Sigma and Sigma# are the programming languages of these computers. Their tools are commands at the prompt, once you install their packages. The Book of Sigma teaches the languages themselves ([](jsc:sigma_two_languages)).

Table: The commands of Sigma

| Command | Comes with the package | What it does |
| --- | --- | --- |
| `scc` | `scc` | Compiles a Sigma file, which ends in .sg, into a listing that ends in .asm. |
| `sgsc` | `sgsc` | Compiles a Sigma# file, which ends in .sgs, into a listing. |
| `sigma` | `sigma` | Runs a listing with sigma run, builds one for several instruction sets with sigma build, stops one with sigma stop, and lists what runs with sigma ps. |
| `sgpack` | `sigma` | Starts a package, builds it, and puts it on the Mirror or takes it back. |
| `sgdb` | `sgsc` | Debugs a listing built with sgsc --debug, a line at a time. |

You install a package by its name, with your system's package manager ([](jsc:software_commands)). The packages come from the Mirror, so your computer must be on a network whose Mainframe runs it.

1. Install the packages you need, such as sgsc and sigma.
2. Write your program in a file that ends in .sgs, such as hello.sgs.
3. Type sgsc hello.sgs to compile it. The compiler writes hello.asm beside it.
4. Type sigma run hello.asm to run the listing.

```command
id: jsc:command/linux/scc
```

```command
id: jsc:command/linux/sgsc
```

```command
id: jsc:command/linux/sigma
```

```command
id: jsc:command/linux/sgpack
```

```command
id: jsc:command/linux/sgdb
```

## If something goes wrong

> [!PROBLEM] The prompt says it does not know sgsc, scc, sigma or sgpack.
> Install the package that brings the command, as the table shows, then type it again.

> [!PROBLEM] The package manager says it could not resolve mirror://.
> Put the computer on a network whose Mainframe runs the Mirror ([](jsc:mirror)).

> [!SEE] jsc:sigma_installing, jsc:sigma_compile, jsc:sigma_run
