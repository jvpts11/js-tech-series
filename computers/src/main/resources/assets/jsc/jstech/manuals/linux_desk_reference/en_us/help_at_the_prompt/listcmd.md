---
section: help_at_the_prompt
order: 284
icon: jsc:guide_to_operations
---
# The listcmd command

listcmd lists everything this computer can run: its commands, whichever system they belong to, and the programs installed on it. It prints one list, and nothing is hidden from it.

Normally listcmd does not exist, because each system teaches its own commands in its own way. The server owner can switch it on for every computer, if you would rather read one list.

## Turning it on

1. Open jscomputers-server.toml in the serverconfig folder of the world.
2. Set prompt.list_commands to true.
3. Save the file.

In single-player, it is the same file, in the serverconfig folder of your world.

Table: Where the switch is

| Setting | Value |
| --- | --- |
| File | `jscomputers-server.toml` |
| Folder | serverconfig, inside the world's folder |
| Key | `prompt.list_commands` |
| Default | `false` |

Table: The two parts of the list

| Heading | What it lists |
| --- | --- |
| `COMMANDS` | One row per command this computer has, with its one-line summary. |
| `PROGRAMS` | One row per installed program, with its name and its id. |

The commands depend on the computer's system, and the programs depend on what is installed on it. When the switch is on, listcmd also appears in the list that help prints, like any other command.

```command
id: jsc:command/linux/listcmd
```

## If something goes wrong

> [!PROBLEM] The reply is "command not found: listcmd".
> The switch is off on this server, so the command does not exist. Ask the server owner to set prompt.list_commands to true.

> [!PROBLEM] The list is shorter on one computer than on another.
> Each computer lists only what it has. A program appears under PROGRAMS once it is installed on that computer.

> [!SEE] jsc:help_command, jsc:programs
