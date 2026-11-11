---
section: gateway
order: 165
---
# The Gateway Manager

The Gateway Manager is a desktop program. It lists the Network Gateways on this computer's ports, and it is where you set what ComputerCraft may do through each one. It is a Legacy-era program ([](jsc:eras)).

> [!DEFINE] Ceiling
> The highest priority that any request from a ComputerCraft program may carry.

> [!DEFINE] Call cap
> The number of calls per game tick that the Gateway answers for a ComputerCraft program.

Select a Gateway in the list on the left, then use the tabs and the buttons on the right. Until a Gateway is linked to one of this computer's ports, the list says none linked yet.

Table: The four tabs

| Tab | What it shows or sets |
| --- | --- |
| Status | The link, the Mainframe, the budget, the calls served and the latest log lines. |
| Permissions | The four settings: reading, operations, the priority ceiling and the calls a tick. |
| Computers | The ComputerCraft computers attached to the Gateway, with ID, LABEL, STATE, AGENT and LAST SEEN. |
| Log | What the Gateway did, with WHEN, WHO and RESULT columns. |

The Status tab has two cards. THIS SIDE says "Linked to" and the host's name, or "Not linked", and it says whether the Mainframe is online or offline. COMPUTERCRAFT SIDE says CC: Tweaked with its version, or that CC: Tweaked is not installed.

The budget line shows the share of each tick that the Gateway's calls take from the host's programs, such as "Budget: 12% of a tick".

## The buttons

Table: The buttons

| Button | What it does |
| --- | --- |
| Rename | Opens the RENAME GATEWAY dialog, with APPLY and CANCEL. |
| Identify | Blinks the Gateway's lamps for 3 seconds. |
| Clear buffer to network | Sends the items in the buffer to the network. |
| Open the log | Opens the Gateway's log. |

On the Computers tab, Turn on, Reboot, Shutdown and Test event act on the ComputerCraft computers attached to the Gateway.

1. Open the Gateway Manager on the computer that has the Gateway on its port.
2. Select the Gateway in the list on the left.
3. Open the Permissions tab, and turn on the switches you need.
4. Choose the priority ceiling and the calls a tick.
5. Open the Status tab to check the link and the Mainframe.

## If something goes wrong

> [!PROBLEM] The message "select a gateway first" appears.
> No Gateway is selected. Click one in the list on the left, then press the button again.

> [!PROBLEM] The message "CC: Tweaked is not installed" appears.
> The ComputerCraft side is missing. Install CC: Tweaked to use it.

> [!PROBLEM] The message says an action needs the agent on the ComputerCraft computer.
> The action needs a program on that ComputerCraft computer, and none is there. Try an action that does not need one.

> [!PROBLEM] The message "no Mainframe on this network" appears.
> Operations need a running Mainframe. Put one on the network ([](jsc:mainframes)).

> [!PROBLEM] The message "the buffer is empty" appears after Clear buffer to network.
> Nothing is in the buffer to send. Put items in it yourself, or have a ComputerCraft program pull them in.

> [!SEE] jsc:gateway_permissions, jsc:gateway_command
