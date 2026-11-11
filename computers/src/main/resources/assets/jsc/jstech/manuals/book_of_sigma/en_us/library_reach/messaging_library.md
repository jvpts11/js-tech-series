---
section: library_reach
order: 467
---
# EventLog and Channel: speaking to other programs

Your program can write to the network's event log, and it can send values to programs on other computers. You do both through EventLog and Channel, built-in types of System.Network. Both came with Sigma# 3, and both need the machine to be on a network: on a machine that is on none, a call stops the program with a NetworkException, which you can catch.

Table: EventLog and Channel members

| Member | What it does |
| --- | --- |
| `EventLog.Write(level, text)` | Writes an entry to the network's event log, from this machine, at the level info, warn or error. The entry is one line of up to 256 characters. Every program listening through Network.OnEvent hears it. |
| `Channel.Listen(name, handler)` | Calls the handler with a ChannelMessage each time a value is sent on the channel of that name. A later call for the same channel takes the place of the one before. |
| `Channel.Close(name)` | Stops hearing the channel. |
| `Channel.Send(name, value)` | Sends a copy of the value on the channel to the programs of the other machines that hear it: on this network, and on the networks federated with it that let it send there. |

Table: Records of Channel

| Record | Fields | What each field holds |
| --- | --- | --- |
| `ChannelMessage` | `ChannelMessage.Channel, ChannelMessage.From, ChannelMessage.Value, ChannelMessage.Tick` | The channel it came on, the machine that sent it, the copy of the value, and the tick it arrived on. |

A channel's name is up to 64 characters. A value travels as a copy, as a GenericComponent's Data does: text, numbers, yes and no, and lists, maps and records of them, up to 32 KB in all. When you change your copy, nothing changes at the sender.

A value takes a tick to cross its network, one more when the computer that sends it is on Wi-Fi, and one for each border between federated networks it crosses. Sending costs 200 credits, as asking for an Operation does; an entry in the log costs 100 credits, as a write to a file does. Listening costs nothing, the handler runs on your program's credits, and what your program hears is kept when the world is saved.

```sgs Door.sgs
Channel.Listen("door", message =>
        Console.PrintLine(message.From + " says " + message.Value));
Channel.Send("door", "open");
EventLog.Write("warn", "The north door is open");
```

## If something goes wrong

> [!PROBLEM] The program stops and says this computer is not on a network.
> EventLog and Channel need a network. Check Network.Online first.

> [!PROBLEM] A program never hears what you send.
> It hears only a channel it listens to, by the same name, and only from another machine. Across a federation, check that it lets the sender's network send to the listener's.

> [!SEE] jsc:network_watches, jsc:wan_gateways
