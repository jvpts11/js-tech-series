---
section: shell_commands
order: 258
---
# The network from the prompt

These commands use the data network of your computer, the cables and the storage that join the computers ([](jsc:what_a_network_is)). Without a network, the net command says the computer is not on a network.

The network answers queries in IQL, its own query language ([](jsc:iql)). The iql command runs one IQL statement, and prints the answer, up to 64 rows.

```command
id: jsc:command/linux/net
```

```command
id: jsc:command/linux/iql
```

The interac command works the network's items. Alone, it takes the whole terminal and shows the network in three tabs: Network, Servers and Locked. With words, it does one thing and gives the prompt back.

Table: Options of interac

| Option | What it does |
| --- | --- |
| `--to` | Leaves what you take in this computer, not in your hands. |
| `--sort name` | Sorts the lines by name instead of by amount, which is the default. |

```command
id: jsc:command/linux/interac
```

The ssh command opens a shell on another computer of the network, and the exit command closes that remote shell and returns you to this computer. Typed with no name, ssh lists the computers it can reach.

```command
id: jsc:command/linux/ssh
```

```command
id: jsc:command/linux/exit
```

## If something goes wrong

> [!PROBLEM] The net command says not on a network.
> Connect the computer to a data cable ([](jsc:data_cables)).

> [!PROBLEM] The iql command says syntax, followed by a message.
> The message says what is wrong with the statement. Check it against the IQL pages ([](jsc:iql)).

> [!PROBLEM] The ssh command says no other computers are reachable on this network.
> Turn on the other computer, and check that it is on the same network.

> [!SEE] jsc:file_commands, jsc:devices_commands
