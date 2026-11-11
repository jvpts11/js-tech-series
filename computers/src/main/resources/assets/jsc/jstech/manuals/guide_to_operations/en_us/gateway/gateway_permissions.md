---
section: gateway
order: 166
---
# What ComputerCraft may do

A ComputerCraft program can ask the Gateway for two kinds of things: reading what the network holds, and starting operations that move or make items. You decide which kinds it may ask for, with four settings on the host.

Table: The four settings

| Setting | What it allows | Default |
| --- | --- | --- |
| Read the network | The reading calls: capacity, used, types, total, find, servers, computers, operation, operations and watch. | On |
| Operations | The calls pull, push, craft, cancel and run. | On |
| Priority ceiling | The highest priority a request may carry. A higher request is lowered to it. | MEDIUM |
| Calls a tick | How many calls the Gateway answers in one game tick, which is a twentieth of a second. | 8 |

The switches do not cover unwatch, log, send, online, name or host. Unwatch and log still count against the call cap, and send does not.

A request names its priority with one word: low, medium or high. A request with no word, or with a word the Gateway does not know, runs at medium. A ceiling lower than that priority then lowers it. [Priorities](jsc:priorities) explain the order in which the network serves them.

A quantity of 0 or less means all: pull all of a name, or push everything in the buffer of that name. The Gateway Manager shows it as pull all. What a push cannot move goes back into the buffer.

The run call starts a compiled program on another computer of the network. You give it the computer's name, the program's name and any arguments, and it returns a process number. It takes no priority.

## Refusals

A refused request goes back to the program as text, and the Gateway's log writes it as DENIED.

Table: Refusals and what they mean

| Refusal | What it means |
| --- | --- |
| `denied: reading the network is off` | The read switch is off. |
| `denied: operations are off` | The operations switch is off. |
| `busy: 8 calls a tick` | The program called more often than the call cap in one tick. The number is the cap. |
| `which item?` | The request named no item. |
| `unknown item: <name>` | The game knows no item by that name, and fluids and chemicals say unknown fluid or unknown chemical. |
| `the computer is not on a network` | The host has no network to serve the request from. |
| `the network has no running Mainframe` | A running Mainframe must be on the network. |
| `could not start the <type>` | The network refused to start the operation, such as a SELECT or an INSERT. |
| `the buffer holds no <name>` | A push asked for an item that the buffer does not hold. |
| `no pattern crafts <name>` | No pattern makes that item, so no craft can start. |
| `more than one operation starts with <id>; give more of the id` | The id you gave matches two operations. |
| `<name>: no such computer on this network` | The computer you named is not on this network. |
| `<name> is powered off` | The computer you asked to run a program is off. |
| `<name> does not take programs from other computers` | The computer refuses programs that come from another computer. |
| `<name>: nothing installed runs a program of this kind` | The computer has nothing that can run that program. |
| `<name>: <n> MB will not fit in <m> MB of free memory` | The computer has too little free memory for the program. |

## If something goes wrong

> [!PROBLEM] The message "denied: reading the network is off" appears.
> Turn the read switch on in the Permissions tab, or type gateway <name> set read on in the command line.

> [!PROBLEM] The message "denied: operations are off" appears.
> Turn the operations switch on in the Permissions tab, or type gateway <name> set operations on in the command line.

> [!PROBLEM] The message "busy: 8 calls a tick" appears.
> The program called more than the cap in one tick. Raise the cap to 16, or slow the program down.

> [!PROBLEM] A program asks for a priority and gets a lower one.
> The priority ceiling is lower than the request, so the request is lowered to it. Raise the ceiling in the Permissions tab.

> [!SEE] jsc:gateway_manager, jsc:states_and_queues
