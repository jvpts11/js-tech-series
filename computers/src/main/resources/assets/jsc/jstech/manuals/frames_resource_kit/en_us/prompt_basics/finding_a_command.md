---
section: prompt_basics
order: 235
---
# Finding a command

Every command has a page of its own, and the page is the same text that help and the switch /? print. You can list the commands the machine has, or ask for the page of one command you already know. Type the first letters of a name and press [[Tab]] to finish it.

Each command has a scope. A command works only on the systems it names, and only on a machine with the parts it needs, such as a disk for the file commands or a network for iql. When a part is missing, the command is not in help, and typing it gives a message.

> [!DEFINE] Scope
> The systems and the parts of a machine that a command needs before the machine offers it.

## Listing the commands

Table: Ways to see the commands

| Way | What it shows |
| --- | --- |
| `help` | The commands this machine has, one line each. MC-DOS and MC-NET show them on the whole screen; Frames prints them. |
| `help NAME` | The page of one command. |
| `listcmd` | Every command and every program on this machine, in one list. |

listcmd lists everything this machine can run: the commands it offers and every program installed on it, in one list. Its list has a COMMANDS heading with one line per command, then a PROGRAMS heading with each program's name and id. It is off unless the server's settings turn it on.

```command
id: jsc:command/mc_dos/listcmd
```

On Linux and the other Unix systems, man opens the page of a Unix command. Those commands are explained in the Unix books, in [finding help](jsc:man_pages).

## If something goes wrong

> [!PROBLEM] listcmd says command not found.
> The server has listcmd off. Type help instead to list the commands.

> [!PROBLEM] A command is missing from help.
> This machine lacks something the command needs, such as a disk, a network or a port for peripherals. Or the command belongs to a package you have not installed yet.

> [!PROBLEM] The shell says command not found: NAME
> The word is not a command on this machine, and no program of that name is installed. Check the spelling, or type help to list the commands.

> [!PROBLEM] The page says it works once a package is installed.
> Install that package first ([](jsc:software_verbs)). Then the command is on the machine.

> [!SEE] jsc:help_on_a_computer, jsc:the_systems
