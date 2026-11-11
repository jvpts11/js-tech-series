---
section: prompt_programs_and_network
order: 247
---
# Editors and Sigma at the prompt

Some programs work inside the Command Prompt window instead of opening their own. Two editors take over the window while you change a file, and the tools of Sigma compile and run your programs there. Install each one first ([](jsc:software_verbs)).

## vim and emacs

Type vim or emacs, a space and the name of a file. The editor takes the whole window, and gives it back when you leave. The file does not have to exist: it is made when you first save it.

Table: Saving and leaving

| Editor | To save | To leave |
| --- | --- | --- |
| `vim` | Press Esc, then type :w and press Enter. | Press Esc, then type :q and press Enter. :wq saves and leaves at once. |
| `emacs` | Press Ctrl+X, then Ctrl+S. | Press Ctrl+X, then Ctrl+C. |

vim starts in normal mode, where a letter is a command. Press i to type into the file, and Esc to stop typing. The Linux Desk Reference lists every key of both editors ([](jsc:vim)).

```command
id: jsc:command/mc_dos/vim
```

```command
id: jsc:command/mc_dos/emacs
```

## Sigma

Sigma and Sigma# are the programming languages of these computers. The Book of Sigma teaches them ([](jsc:sigma_two_languages)). Their tools are verbs of the prompt, once you install their packages.

Table: The verbs of Sigma

| Verb | Comes with the package | What it does |
| --- | --- | --- |
| `scc` | `scc` | Compiles a Sigma file, which ends in .sg, into a listing that ends in .asm. |
| `sgsc` | `sgsc` | Compiles a Sigma# file, which ends in .sgs, into a listing. |
| `sigma` | `sigma` | Runs a listing with sigma run, builds one for several instruction sets with sigma build, stops one with sigma stop, and lists what runs with sigma ps. |
| `sgpack` | `sigma` | Starts a package, builds it, and puts it on the Mirror or takes it back. |
| `sgdb` | `sgsc` | Debugs a listing built with sgsc --debug, a line at a time. |

On the Frames editions, pckmgr install fetches these packages from the Mirror. MC-DOS installs them from their media only. The Sigma# compiler and the runtime need a Legacy computer or later, and on Frames they need Frames XP or later, so Frames 95 holds the Sigma compiler alone ([](jsc:sigma_installing)).

1. Install the packages you need, such as sgsc and sigma.
2. Write your program in a file that ends in .sgs, such as HELLO.SGS.
3. Type sgsc HELLO.SGS to compile it. The compiler writes the listing beside it.
4. Type sigma run and the listing's name to run it.

```command
id: jsc:command/mc_dos/scc
```

```command
id: jsc:command/mc_dos/sgsc
```

```command
id: jsc:command/mc_dos/sigma
```

```command
id: jsc:command/mc_dos/sgpack
```

```command
id: jsc:command/mc_dos/sgdb
```

## If something goes wrong

> [!PROBLEM] The prompt says it does not know vim, emacs or a verb of Sigma.
> Install the program or the package that brings it, then type it again.

> [!PROBLEM] pckmgr says it could not resolve mirror://.
> Put the computer on a network whose Mainframe runs the Mirror ([](jsc:mirror)).

> [!SEE] jsc:software_verbs, jsc:sigma_installing, jsc:sigma_compile
