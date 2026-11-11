---
section: network_tools
order: 87
icon: jsc:monitor
---
# Remote Control

Remote Control takes over another machine of the network, and puts its screen on this monitor. It is the point-and-click twin of [ssh](jsc:remote_access), the secure shell. It matters most for a server in a rack, which has no screen of its own ([](jsc:server_racks)).

The window lists the machines on this network under the heading Machines on this network. Each row shows the machine's kind and its system, and whether it is up or off. Select a machine, and press Take over. Taking over puts the machine's session on this monitor: its power-on self-test, its firmware, its terminal, or its full desktop ([](jsc:monitors)).

Table: Parts of Remote Control

| Part | What it shows |
| --- | --- |
| Heading | Machines on this network. |
| Rows | The machine's kind and its system, and whether it is up or off. |
| Button | Take over, or Select a machine when no row is chosen. |

## If something goes wrong

> [!PROBLEM] A machine is listed as off.
> Switch the machine on before you can take it over.

> [!PROBLEM] The list says No other machine is reachable.
> Check that this computer is on the network whose machines you want to reach.

> [!SEE] jsc:mainframes, jsc:remote_access, jsc:monitors
