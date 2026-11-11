---
section: examples_devices
order: 508
---
# Gateway

A Gateway links the network to ComputerCraft: the computers and peripherals of another mod, on the wires they use. This example reads the Gateway, calls a peripheral, switches a computer on and off, sends a message, and receives messages.

If you do not have ComputerCraft installed, the Gateway has no side that reaches its computers.

A Gateway's permissions limit what it lets through ([](jsc:gateway_permissions)).

## The program

```sgs Gateway.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Network.*;
namespace Corpus;
class GatewayCorpus {
    static void Main() {
        bool online = Gateway.Online;
        string current = Gateway.Current;
        List<string> names = Gateway.Names();
        bool chosen = Gateway.Select("east");
        Console.PrintLine("" + online + current + names.Count + chosen);
        foreach (CcComputer computer in Gateway.Computers()) {
            Console.PrintLine("" + computer.Id + computer.Name + computer.Label + computer.Online);
        }
        foreach (CcPeripheral device in Gateway.Peripherals()) {
            Console.PrintLine(device.Name + device.Type + device.Methods.Count);
        }

        object none = Gateway.Call("left", "getName");
        object one = Gateway.Call("left", "setOutput", true);
        object two = Gateway.Call("left", "setOutput", "top", true);
        object three = Gateway.Call("left", "setOutput", "top", true, 1);
        bool powered = Gateway.TurnOn(1) && Gateway.Shutdown(1) && Gateway.Reboot(1);
        bool sent = Gateway.Send(1, "hello");
        Gateway.OnMessage(Heard);
        Console.PrintLine("" + none + one + two + three + powered + sent);
    }

    static void Heard(GatewayMessage message) {
        Console.PrintLine("" + message.From + message.Text + message.Tick);
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `Gateway.Online` | Tells whether the Gateway is online. |
| `Gateway.Current` | The name of the Gateway that is selected. |
| `Gateway.Names()` | The names of the Gateways on the machine. |
| `Gateway.Select("east")` | Selects the Gateway called east, and answers true when it is chosen. |
| `Gateway.Computers()` | Lists the ComputerCraft computers, each with its number, name, label and whether it is online. |
| `Gateway.Peripherals()` | Lists the peripherals, each with its name, its type and its number of methods. |
| `Gateway.Call("left", "getName")` | Calls the method getName on the peripheral left, with no argument. |
| `Gateway.Call(...)` | Calls the same method with one argument, with two, and with three. |
| `Gateway.TurnOn(1), Shutdown(1), Reboot(1)` | Turns on, shuts down, or restarts the ComputerCraft computer with number 1, and answers true or false. |
| `Gateway.Send(1, "hello")` | Sends the text hello to the computer with number 1. |
| `Gateway.OnMessage(Heard)` | Sets Heard as the method called with each message that arrives. |
| `Heard(GatewayMessage message)` | Prints the sender's number, the text and the tick it arrived on. |

> [!DEFINE] ComputerCraft
> Another mod with computers and peripherals of its own, which a Gateway reaches.

> [!DEFINE] Peripheral
> A device on a computer that the computer controls through its methods.

> [!DEFINE] Priority ceiling
> The highest priority of work that a Gateway lets through to the network.

## If something goes wrong

> [!PROBLEM] The machine has no Gateway, and the program stops with "this computer has no Gateway".
> Place a Gateway on the machine's network, then select it with Select.

> [!PROBLEM] The name given to Select is not a Gateway, and the program stops with "this computer has no Gateway called east".
> Use a name from the list that Names gives.

> [!PROBLEM] A call comes before any Gateway is selected, and the program stops with "select a gateway first".
> Call Select first, with the name of a Gateway.

> [!PROBLEM] ComputerCraft is not installed, so the Gateway has no side for it, and the program stops.
> Install ComputerCraft, because the Gateway works only with it.

> [!PROBLEM] The Gateway has no Mainframe to work with, and the program stops with "no Mainframe on this network".
> Put a Mainframe on the network.

> [!SEE] jsc:network_gateway, jsc:gateway_permissions, jsc:sigma_library
