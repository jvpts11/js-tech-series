---
section: network_remote
order: 109
icon: jsc:personal_computer
---
# Remote access: ssh and Remote Control

Being on the same network is what lets you reach another computer. The secure shell (ssh) command opens a shell on another computer of your network, from the terminal you are at.

1. Type ssh on its own to see the machines you can reach.
2. Type ssh and the host name of the machine you want.
3. Type exit to leave it. Closing the window leaves it too.

A machine answers to its host name, the name its owner gave it, the start of its node id, or the name of its system, as long as that name picks out exactly one machine. You can reach the Mainframe, the Personal Computers of the network and the servers in the racks. You cannot reach the computer you sit at.

Access needs no password: being on the same network is what grants it.

Table: Messages of ssh

| Message | What it means |
| --- | --- |
| Connected to <machine>. Type exit to return. | The session is open on the other computer. |
| <name>: host not found on this network | No machine on your network has that name; type ssh alone to see the names. |
| <name> matches <n> machines (<names>) - use the host name or node id | Use the host name or the start of the node id instead. |
| connect to host <name>: machine is powered off | The machine is off. Switch it on. |

Commands you run on another computer use that computer's own budget, and they carry its name. A computer can refuse programs from others: its remote setting decides it.

Table: The remote setting

| Command | What it does |
| --- | --- |
| `config remote on` | Other computers may run programs here. This is the default. |
| `config remote off` | Other computers cannot run programs here. |

Remote Control is the point-and-click way into the network's other machines ([](jsc:remote_control)).

> [!DEFINE] Remote Control
> The desktop program that takes over another machine of the network and shows it on your screen.

## If something goes wrong

> [!PROBLEM] host not found on this network
> Check the name with ssh alone, or in the Devices tab of the Network Manager. The machine must be on the same network.

> [!PROBLEM] machine is powered off
> Switch the machine on.

> [!PROBLEM] cannot run programs
> The other computer has config remote off. Turn it on there with config remote on.
