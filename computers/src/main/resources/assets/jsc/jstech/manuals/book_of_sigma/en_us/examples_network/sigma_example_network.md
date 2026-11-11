---
section: examples_network
order: 502
---
# Network

This example reads the network: how much it holds, which servers keep an item, and which computers are on it. It also watches a stock, asks the network for jobs and starts a program on another computer.

The network is every computer and device joined by data cable to one Mainframe ([](jsc:what_a_network_is)). The example reads it through the library's Network type, and asks the Mainframe through its Mainframe type.

## The program

```sgs Network.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Network.*;
using System.Operations.*;
using System.Execution.*;
namespace Corpus;
class NetworkCorpus {
    static void Main() {
        bool online = Network.Online;
        string current = Network.Current;
        long stored = Network.Capacity + Network.Used + Network.Total("minecraft:iron_ingot");
        List<string> types = Network.Types();
        Console.PrintLine("" + online + current + stored + types.Count);
        foreach (HoldingInfo holding in Network.Find("minecraft:iron_ingot")) {
            Console.PrintLine(holding.Server + holding.Quantity);
        }
        foreach (ServerInfo server in Network.Servers()) {
            Console.PrintLine(server.Name + server.Stored + server.Capacity);
        }
        Subscription any = Network.Watch("minecraft:iron_ingot", Told);
        Subscription below = Network.WatchBelow("minecraft:iron_ingot", 10, Told);
        Subscription above = Network.WatchAbove("minecraft:iron_ingot", 100, Told);
        Console.PrintLine("" + any.Id + any.Item + below.Id + above.Id);

        RemoteComputer desk = Network.Computer("desk");
        List<RemoteComputer> all = Network.Computers();
        Console.PrintLine(desk.Host + desk.Name + desk.Type + desk.Os + desk.Online + all.Count);
        List<string> args = new List<string>();
        Process one = desk.Start("C:\\tool.asm");
        Process two = desk.Start("C:\\tool.asm", args);
        Process three = desk.Start("C:\\tool.asm", args, "lab");
        List<string> shell = desk.Shell("dir");
        bool sent = desk.Send(one.Id, "hi");
        List<Process> processes = desk.Processes();
        Console.PrintLine("" + two.Id + three.Id + shell.Count + sent + processes.Count);

        IqlResult result = Iql.Run("QUERY items");
        List<Map<string, object>> rows = Iql.Query("QUERY items");
        IqlResult exec = Iql.Exec("restock");
        IqlResult execWith = Iql.Exec("restock", args);
        IqlResult file = Iql.RunFile("C:\\job.iql");
        Console.PrintLine("" + result.Ok + result.Message + result.Rows.Count + rows.Count);
        Console.PrintLine("" + exec.Ok + execWith.Ok + file.Ok);

        bool mainframe = Mainframe.Online;
        int peak = Mainframe.PeakToday;
        WorkStat stat = Mainframe.Stats("PULL");
        List<WorkStat> work = Mainframe.Work();
        Console.PrintLine("" + mainframe + peak + stat.Type + stat.Count + stat.AverageWait + stat.AverageRun);
        Console.PrintLine("" + stat.ShortfallPercent + stat.Moved + work.Count);

        AskResult pulled = Operations.Pull("minecraft:oak_log", 64);
        AskResult pushed = Operations.Push("minecraft:oak_log", 64);
        AskResult crafted = Operations.Craft("minecraft:stick", 4);
        AskResult cancelled = Operations.Cancel("op-1");
        AskResult raised = Operations.Reprioritise("op-1", "HIGH");
        OperationInfo info = Operations.Get("op-1");
        List<OperationInfo> list = Operations.List();
        Console.PrintLine("" + pulled.Ok + pulled.Message + pushed.Ok + crafted.Ok + cancelled.Ok + raised.Ok);
        Console.PrintLine(info.Id + info.Type + info.Item + info.Moved + info.Requested + info.Status + info.Priority);
        Console.PrintLine("" + list.Count);
    }

    static void Told(StockEvent told) {
        Console.PrintLine(told.Item + told.Total + told.Previous);
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `Network.Online` | Tells whether this computer is on a network. |
| `Network.Capacity, Used, Total(...)` | How much the network can hold, how much it holds, and how much of one item. |
| `Network.Types()` | The kinds of item the network holds. |
| `Network.Find(...)` | Each server that holds the item, with the quantity it holds. |
| `Network.Servers()` | Each server with its name, what it stores and its capacity. |
| `Network.Watch, WatchBelow, WatchAbove` | Ask to be told when a stock changes, or goes below or above a number. |
| `Network.Computer(...)` | Finds one computer on the network by its name. |
| `Network.Computers()` | Lists every computer on the network. |
| `desk.Start(...)` | Starts a program on that computer, with or without arguments. |
| `desk.Shell(...)` | Runs one line at that computer's prompt, and gives back what it printed. |
| `desk.Send(...)` | Sends a text to one of its processes. |
| `desk.Processes()` | Lists its processes. |
| `Iql.Run(...)` | Runs one statement of the network's query language (IQL). It does not stop the program when the statement is refused. |
| `Iql.Query(...)` | Runs a query and gives back its rows. It stops the program when the query is refused. |
| `Iql.Exec(...)` | Runs a procedure by its name, with or without a list of arguments. |
| `Iql.RunFile(...)` | Runs each statement of a file. |
| `Mainframe.Online, PeakToday` | Whether the Mainframe is online, and its peak for the day. |
| `Mainframe.Stats(...), Work()` | The work done for one kind, and the work done for every kind. |
| `Operations.Pull, Push, Craft` | Ask the network for a job of one kind. Each one answers with an Ok flag and a Message. |
| `Operations.Cancel, Reprioritise` | Cancel a job, or change its priority. |
| `Operations.Get, List()` | Read one job, or every job. |

Watching is better than asking. The handler Told is called when the stock changes, so the program does not have to ask every tick. Told prints the item, the total and the stock before the change.

> [!DEFINE] Holding
> The stock of one item that one server holds, with the quantity.

> [!DEFINE] Subscription
> A request for the network to call a handler when a stock changes.

> [!DEFINE] Ask
> A request sent to the network. It answers whether it was taken, and why not, without stopping the program.

> [!DEFINE] IQL
> The network's query language: short statements, written as text, that read or change the network.

## If something goes wrong

> [!PROBLEM] Online is false: this computer is not on a network.
> Join the computer to the network with a data cable to a Mainframe ([](jsc:first_network)).

> [!PROBLEM] A query is refused, and the program stops with a message from the network's engine.
> Read the message, or use Iql.Run, which does not stop on a refused statement.

> [!PROBLEM] Get gives null for a job that has finished.
> Test the answer for null before you read its fields.

> [!SEE] jsc:iql, jsc:mainframes, jsc:sigma_library
