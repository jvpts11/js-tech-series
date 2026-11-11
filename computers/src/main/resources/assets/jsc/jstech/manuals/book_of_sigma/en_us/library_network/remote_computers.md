---
section: library_network
order: 448
---
# Other computers on the network

A program can start a program on another computer of the same network, and ask that computer what it runs. The other computer is a RemoteComputer, and you get one from Network.Computer or Network.Computers.

The computer must be on the same network. A host name that matches several machines is refused, so use the node id or the full host name instead.

> [!DEFINE] Host name
> The name a machine answers to on the network.

> [!DEFINE] Node id
> The machine's own identifier. Use it in place of a host name that matches several machines.

Table: Fields of RemoteComputer

| Field | What it holds |
| --- | --- |
| `RemoteComputer.Host` | The host name. |
| `RemoteComputer.Name` | The name of the computer. |
| `RemoteComputer.Type` | Mainframe, Computer, Crafting Computer, Personal Computer or Cluster Management Computer. |
| `RemoteComputer.Os` | The system the computer runs. |
| `RemoteComputer.Online` | Yes when the computer is on. |

Table: Calls on a RemoteComputer

| Call | What it does |
| --- | --- |
| `RemoteComputer.Start(path)` | Starts a program on that computer, as Program.Start does, and gives a Process; Start also takes (path, args) or (path, args, priority). |
| `RemoteComputer.Shell(line)` | Runs a line at that computer's prompt, and gives back the lines it printed. |
| `RemoteComputer.Send(id, text)` | Sends a message to a process on that computer. Gives false when it cannot. |
| `RemoteComputer.Processes()` | The processes on that computer, as a list of Process. |

Table: Refusals from another computer

| Message | What it means |
| --- | --- |
| The computer cannot run programs | That machine does not run programs. |
| The computer does not take programs from other computers | That machine refuses programs sent from others. |
| The computer is powered off | That machine is off. |
| No such computer on this network | No machine of that name is on this network. |

## If something goes wrong

> [!PROBLEM] The program stops and says the computer is powered off.
> Turn the computer on, then call it again.

> [!PROBLEM] The program stops and says the computer cannot run programs.
> Choose another computer on the network for the program.

> [!PROBLEM] The program stops and says the computer does not take programs from other computers.
> Start the program on another computer, or run it on that computer yourself.

> [!PROBLEM] Network.Computer gives null.
> No computer of that name is on the network. Look at the names in Network.Computers.

> [!PROBLEM] The host name matches several machines.
> Use the node id, or the full host name.

> [!SEE] jsc:remote_access, jsc:what_a_network_is
