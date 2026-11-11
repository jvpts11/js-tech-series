---
section: library_machine
order: 454
---
# Gateways: talking to ComputerCraft computers

A Gateway links the network to ComputerCraft computers on the wire. A program can list those computers and their peripherals, call a method of a peripheral, switch a computer on, off or reboot it, and send it a message. The block is explained in [](jsc:network_gateway).

Table: Gateway members

| Member | What it does |
| --- | --- |
| `Gateway.Online` | Yes when this machine has a Gateway that is online. |
| `Gateway.Current` | The name of the Gateway the program chose. |
| `Gateway.Names()` | The names of the Gateways, as a list of text. |
| `Gateway.Select(name)` | Chooses the Gateway that later calls use, by name, in any case. |
| `Gateway.Computers()` | The ComputerCraft computers, as a list of CcComputer. |
| `Gateway.Peripherals()` | The peripherals, as a list of CcPeripheral. |
| `Gateway.Call(peripheral, method, args)` | Asks a peripheral to run a method, with up to three arguments after the two names, and gives back what it returns. |
| `Gateway.TurnOn(id)` | Switches on the computer with that number. |
| `Gateway.Shutdown(id)` | Switches off the computer with that number. |
| `Gateway.Reboot(id)` | Reboots the computer with that number. |
| `Gateway.Send(id, text)` | Sends a message to that computer. |
| `Gateway.OnMessage(handler)` | Sets the handler that runs for each message from a ComputerCraft computer. |

Table: Records of the Gateway

| Record | Fields | What each field holds |
| --- | --- | --- |
| `CcComputer` | `CcComputer.Id, CcComputer.Name, CcComputer.Label, CcComputer.Online` | The number, name and label of the computer, and whether it is on. |
| `CcPeripheral` | `CcPeripheral.Name, CcPeripheral.Type, CcPeripheral.Methods` | The name and type of the peripheral, and the names of its methods. |
| `GatewayMessage` | `GatewayMessage.From, GatewayMessage.Text, GatewayMessage.Tick` | The number of the sender, the text, and the tick it arrived. |

The Gateway a call uses is the one the program chose with Select. If the program chose none, the first Gateway the machine lists is used.

Send gives your message the event name jsc_message on the ComputerCraft side. A call across a Gateway costs more than most things a program does, and each argument adds to its cost.

Every call is written in the Gateway's log: called when it went through, and the refusal text when it did not.

## If something goes wrong

> [!PROBLEM] The program stops and says this computer has no Gateway.
> Use a machine that has a Gateway.

> [!PROBLEM] The program stops and says this computer has no Gateway with that name.
> Check the name with Gateway.Names.

> [!PROBLEM] The program stops and says the machine has no ComputerCraft side, and asks whether ComputerCraft is installed.
> Install ComputerCraft, then start the game again.

> [!PROBLEM] Gateway.Call stops the program with the error text from ComputerCraft.
> The peripheral refused the call. Check the method name and the arguments in Gateway.Peripherals.

> [!SEE] jsc:network_gateway, jsc:computercraft_peripheral
