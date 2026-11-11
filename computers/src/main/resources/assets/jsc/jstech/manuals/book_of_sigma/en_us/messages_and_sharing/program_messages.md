---
section: messages_and_sharing
order: 484
---
# Programs talking to each other

Programs talk to each other with messages. A message is a line of text that one program sends to another program's number. The other program can run on the same computer, on another computer of the network, or be a [ComputerCraft](jsc:computercraft_side) computer reached through a [Network Gateway](jsc:network_gateway).

## On one computer

A program sends a message with Process.Send, giving the number of the other program and the text. To receive messages, a program sets a handler with Program.OnMessage: the steps that run for each message. The handler gets the number of the sender (From), the text (Text) and the tick it arrived on (Tick).

Table: Messages

| Call | Price in instructions | What it does |
| --- | --- | --- |
| `Process.Send(number, text)` | 10 | Sends the text to the program with that number on this computer. |
| `Program.OnMessage(handler)` | free | Runs the handler for each message that arrives. |
| `Gateway.Send(number, text)` | 20 | Sends the text to the ComputerCraft computer with that number. |
| `Gateway.OnMessage(handler)` | free | Runs the handler for each message that comes through a Gateway. |

If the program you send to has no handler, it takes the message and throws it away, so Process.Send still gives true. Process.Send gives false when no running program has that number.

A message is refused when the program it is sent to has finished or halted, or when its waiting calls are full: 256 calls, or 64 KB of text. Then Process.Send gives false.

A message to a Gateway reaches every program on the computer that is listening for it. A computer where no program listens drops the message.

## Other computers on the network

Network.Computers lists the computers on the network. Network.Computer(name) gives one of them by its name. Each one has a host name, a name, a type, an operating system and an online state.

A program on another computer is reached with the remote calls below. Before any of them works, the other computer must be on the network and switched on. It must also take programs from other computers: the remote setting decides that, and it is on by default. Set it to on or off. A remote Start also needs a computer that runs programs.

Table: Calls on another computer

| Call | Price in instructions | What it does |
| --- | --- | --- |
| `RemoteComputer.Start(path)` | 200 | Starts a program there, the way Program.Start does here. |
| `RemoteComputer.Shell(line)` | 200 | Runs one line at that computer's prompt. |
| `RemoteComputer.Send(number, text)` | 10 | Sends the text to the program with that number there. |
| `RemoteComputer.Processes()` | 50, plus 1 per row | Lists the programs running there. |

A program started on another computer records the caller as its parent. When it ends, the caller is told. The caller reads its output and its exit code with Process.Output and Process.ExitCode, while that program is still there to be read.

A remote program's Process value carries the other computer's host name in Process.Host, and calls on that value go to that computer. If the host is unknown or switched off, Process.Running gives false and Process.Kill does nothing.

## Shared folders

A computer shares a folder with the network from its prompt, with the config command. The share takes the folder's own name. It is read only, unless you add write.

Table: Sharing commands

| Command | What it does |
| --- | --- |
| `config share <folder> [read\|write]` | Opens the folder to the network, which can only read it unless write is given. |
| `config unshare <name>` | Closes the share with that name. |

The prompt answers with the address of the share, such as \\\\host\\name, and says whether it is read only or read and write. A path on another computer starts with two backslashes, then the host name, then the share and the rest of the path: \\\\host\\share\\folder\\file. The other computer answers for its own disks.

A copy may use a shared folder at either end. The file is read where it is and written where it goes. The shares of a computer are also covered in [Shared folders](jsc:shared_folders).

## If something goes wrong

> [!PROBLEM] Process.Send gives false.
> The program has finished or halted, or its waiting calls are full. Try again later, or send less text.

> [!PROBLEM] The prompt says that a computer is powered off.
> Switch that computer on, then try again.

> [!PROBLEM] The prompt says that a computer does not take programs from other computers.
> Its remote setting is off. Turn that setting on at that computer. It is on by default.

> [!PROBLEM] The prompt says that a computer cannot run programs.
> That computer does not run programs, so no program starts there. Start the program on a computer that runs programs.

> [!PROBLEM] The prompt says that no such computer is on this network.
> The name is wrong, or the computer is not on this network. Network.Computers lists the computers on it.

> [!PROBLEM] The prompt says that a host name matches more than one machine.
> Use the host name of one machine, or its node id, which is its [network identity](jsc:network_identity).

> [!PROBLEM] The prompt says that a folder is shared read-only.
> The folder was shared without write. Its owner runs config unshare on it, then config share with write.

> [!PROBLEM] The prompt says that this computer already shares 16 folders.
> Unshare a folder with config unshare to make room for another share.

> [!PROBLEM] The prompt says that another folder is already shared under that name.
> A share takes the folder's name, so only one folder can have it. Unshare the other folder first.

> [!PROBLEM] The prompt says that a host is not found on this network.
> Check the host name. Network.Computers lists the computers on the network.

> [!PROBLEM] The prompt says that the machine of a path is powered off.
> Switch that computer on.

> [!PROBLEM] The prompt says that there is no such share on that host.
> That computer shares no folder with that name. Ask its owner for the name of the share.

> [!PROBLEM] The prompt says that a share has to be named.
> Write the path in the form host, then share, then the rest of the path: \\\\host\\share\\folder.

> [!SEE] jsc:network_gateway, jsc:shared_folders, jsc:remote_access
