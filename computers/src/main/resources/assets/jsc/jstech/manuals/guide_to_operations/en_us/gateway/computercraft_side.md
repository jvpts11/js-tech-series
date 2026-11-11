---
section: gateway
order: 164
---
# The Gateway and ComputerCraft

On its front, the Gateway joins a ComputerCraft wired network. A ComputerCraft computer sees it as a peripheral of the type jsc_gateway. Its name is jsc_gateway_ followed by the Gateway's name, with each dash turned into an underscore: gateway-1 becomes jsc_gateway_gateway_1.

A ComputerCraft computer finds the Gateway only when the Gateway is on its wire. Put a networking cable or a wired modem against the Gateway's front. Without CC: Tweaked there is no ComputerCraft side at all.

> [!DEFINE] Budget
> The share of each game tick that the Gateway's calls take from the programs of its host, in percent.

## What the ComputerCraft side can do

A ComputerCraft program calls the Gateway's peripheral. The Gateway answers each call with the same service your own programs use, under the permissions you set on the host ([](jsc:gateway_permissions)). The cost of a call comes out of the host's budget.

The Gateway shows the state of this side in its screen and with the gateway command. It says "reachable" when a ComputerCraft computer is attached or on its wire, and "nothing attached" when none is.

## Events the Gateway sends

The Gateway sends events to the ComputerCraft computers, so a program can wait for them instead of asking again and again.

Table: Events the Gateway sends

| Event | Sent when |
| --- | --- |
| `jsc_operation(id, status)` | An operation the computer started settles. Only that computer hears it. |
| `jsc_stock(name, total, previous)` | The total of a watched name moves. The Gateway checks each watch every 20 ticks. |
| `jsc_test(name)` | You press Test event in the Gateway Manager, or send the test from the gateway command. |

The status of an operation is one of done, partial, failed, running, waiting, locked, pending, discarded or unknown. The operation types are pull, push, delete, move, craft, analyze, reindex, vacuum, drop, update and operation.

## Sending a line back

A ComputerCraft program can call send to say a line to the host. Only a program of J's Computers that is listening hears the line, and each line is written to the Gateway's log as said. With nobody listening, the line reaches no one.

The advancement Talking to the Neighbours is earned when a program hears a ComputerCraft computer through a Network Gateway.

## If something goes wrong

> [!PROBLEM] A ComputerCraft program gets an error back.
> Read its text: it is the Gateway's refusal, such as "denied: operations are off". Each refusal and its fix are in [](jsc:gateway_permissions).

> [!PROBLEM] The error is the message of a CC: Tweaked method itself.
> The text is that method's own message. When the method gives none, the text is the method's name followed by failed.

> [!PROBLEM] The program gets the text "there is no <name> on this Gateway's wire".
> The named peripheral is not on the Gateway's wire. Check its name, and connect it to the wire.

> [!PROBLEM] The program gets the text "no such method <name> on <peripheral>".
> The method name is not one of the peripheral's methods. Check the spelling against [](jsc:computercraft_peripheral).

> [!PROBLEM] A ComputerCraft computer cannot see the Gateway.
> The Gateway is not on its wire. Put a networking cable or a wired modem against its front.

> [!SEE] jsc:gateway_manager, jsc:computercraft_peripheral
