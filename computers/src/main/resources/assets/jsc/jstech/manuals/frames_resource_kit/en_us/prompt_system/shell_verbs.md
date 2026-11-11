---
section: prompt_system
order: 240
---
# The prompt's own commands

A few commands belong to the prompt itself. help lists the commands and explains each one, cls clears the window, echo prints the words you give it, and set gives a name a value. Most of them are on every system, and the table shows where each one runs.

Table: Where the prompt's own commands run

| Command | Systems |
| --- | --- |
| `help` | MC-DOS, MC-NET, the Frames editions, and the Linux and UNIX systems. Not FreeBSD. |
| `cls` | MC-DOS and the Frames editions. |
| `version` | MC-DOS and the Frames editions. |
| `echo` | Every system. |
| `whoami` | Every system. |
| `exit` | Every system. |
| `set` | Every system. |

## help

help with no words lists the commands this machine can run, one line each, with a summary of each. At the foot it says "'help <command>' for details". The list shows only the commands this machine has, not every command of its family.

help <command> prints the page of one command. On MC-DOS and MC-NET, help takes the whole screen, with the series' manuals beside the commands. On the Frames editions, help prints. help dir opens the page of dir. The page ends with an also line that lists the other names of the command.

Table: Keys in the full-screen help

| Key | What it does |
| --- | --- |
| `Tab` | Picks a button or a link. |
| `Enter` | Follows the button or link that is picked. |
| `Alt+C` | Shows the contents. |
| `Alt+N` | Goes to the next topic. |
| `Alt+B` | Goes to the topic before. |
| `Alt+I` | Shows the index. |
| `Alt+S` | Finds a word. |
| `F3` | Finds the same word again. |
| `Esc` | Leaves the help. |

```command
id: jsc:command/mc_dos/help
```

## cls, version and echo

cls clears the window, and the scrollback goes with it.

```command
id: jsc:command/mc_dos/cls
```

version prints the line "J's Computers Shell v1.0" in the accent colour.

```command
id: jsc:command/mc_dos/version
```

echo prints the words that you give it. On MC-DOS and on the Frames editions, the text prints as you typed it.

```command
id: jsc:command/mc_dos/echo
```

## whoami and exit

whoami prints this computer's name, its type and its node, one row each. A computer with no name shows (unnamed).

```command
id: jsc:command/mc_dos/whoami
```

exit closes a remote session that you opened with ssh ([](jsc:network_verbs)). On the prompt of this computer, exit only says that it is not connected. To leave the window, press [[Esc]].

```command
id: jsc:command/mc_dos/exit
```

## set and variables

> [!DEFINE] Variable
> A name that the machine keeps with a value. You give it a value with set, and read it back between percent signs.

set with no words lists every name this machine knows. set NAME=value gives a name a value. set NAME= with nothing after the equals sign forgets the name. Names use letters, digits and underscores, and do not start with a digit.

A value set here belongs to the machine. It stays set at the monitor, in a session opened from another machine, and after a restart.

1. Type set BASE=C:\\work to give BASE a value.
2. Type set to list the names, and BASE is in the list.
3. Type dir %BASE% to read the value back, which the prompt replaces before dir runs.
4. Type set BASE= to forget the name again.

```command
id: jsc:command/mc_dos/set
```

## If something goes wrong

> [!PROBLEM] help does not take the whole screen.
> The full-screen help is on MC-DOS and MC-NET only. On the Frames editions, help prints the list ([](jsc:help_on_a_computer)).

> [!PROBLEM] help <command> says no such command.
> The command is not on this machine. Type help to see the list, or listcmd if the server allows it ([](jsc:finding_a_command)).

> [!PROBLEM] set says a name is not allowed.
> Start the name with a letter, and use only letters, digits and underscores.

> [!PROBLEM] set says usage: set NAME=value
> Put an equals sign between the name and the value: set NAME=value.

> [!PROBLEM] exit says it is not connected.
> exit only closes an ssh session. To leave this window, press [[Esc]].

> [!SEE] jsc:help_on_a_computer, jsc:network_verbs
