---
section: gateway
order: 167
---
# The gateway command

The gateway command manages the Gateways from the command line of a computer. It works on any computer with peripheral ports, even one with no Gateway, so it can tell you what is missing.

1. Type gateway list to see the Gateways on this computer's ports.
2. Type the Gateway's name, then status, perms, computers or log, to see its details.

Table: The forms of the command

| You type | What it does |
| --- | --- |
| `gateway list` | Lists the Gateways on this computer's ports, with NAME, LINK, CC and WHERE. |
| `<name> status` | Shows the link, the network, the budget, the buffer and the latest log lines. |
| `<name> perms` | Shows the read and operations switches, the priority ceiling and the calls a tick. |
| `<name> computers` | Lists the ComputerCraft computers attached, with ID, LABEL, STATE, AGENT and LAST SEEN. |
| `<name> log` | Shows what the Gateway did, oldest first at the bottom. |
| `<name> rename <new>` | Gives the Gateway a new name, under the same rules as the Gateway Manager. |
| `<name> identify` | Blinks the Gateway's lamps. |
| `<name> clear` | Sends the items in the buffer to the network. |
| `<name> test` | Sends the jsc_test event to the computers that listen for it. |
| `<name> set read on\|off` | Turns the read switch on or off. |
| `<name> set operations on\|off` | Turns the operations switch on or off. |
| `<name> set priority low\|medium\|high` | Sets the priority ceiling. |
| `<name> set cap 4\|8\|16` | Sets the calls a tick. |

Names are matched without regard to capitals, so Gateway-1 and gateway-1 name the same Gateway.

## If something goes wrong

> [!PROBLEM] The command says no gateways on this computer's ports.
> No Gateway is linked to a port of this computer. Plug the cable in the back of the Gateway ([](jsc:network_gateway)).

> [!PROBLEM] The command says CC: Tweaked is not installed.
> Install CC: Tweaked to use the ComputerCraft side. The rest of the Gateway still works.

> [!PROBLEM] The command says no gateway named <name> on this computer.
> Check the name with gateway list. Names are not case-sensitive, but the spelling must match.

> [!PROBLEM] The command says this terminal is not on a computer.
> Run the command from the command line of a computer.
