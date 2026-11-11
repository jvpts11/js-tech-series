---
section: credits_and_costs
order: 481
---
# What each call costs

A program reaches the computer, the network and other computers through calls. Each call you make costs its one instruction, plus a price on top. The price depends on how much work the call does, so a call that reads a whole file costs more than one that reads a name.

The price has three parts. The fixed part is always paid. A row is one entry of a list that the call brings back, such as one file name, and each row adds the second part. A block is 4,096 bytes, or 4 kilobytes (KB), of memory that the call reads or writes. Each block adds the third part, and a part of a block counts as a whole block.

A call that only touches the program itself is free, so only its instruction is paid. Printing to the console and reading typed lines are free, and so are the calls that look at the program's own name and arguments.

## The price levels

Every call is priced at one of these levels. The same levels apply on every computer.

Table: The price levels

| Level | Price in instructions | What it is for |
| --- | --- | --- |
| Glance | 5 | a look at something the computer already knows about itself |
| Glance at the network | 10 | a look at something the network already knows |
| Gather | 30 | gathering what the computer holds: its disks, its programs and what it runs |
| Read | 50 | reading from a disk or the network |
| Read, per 4 KB | 1 | added to a read for every block read |
| Write | 100 | writing, which cannot be taken back |
| Write, per 4 KB | 2 | added to a write for every block written |
| Draw | 50 | changing what a window shows, which the computer then draws again |
| Draw, per KB | 10 | added for every kilobyte of a value given to a window component |
| Submit | 200 | asking the network to do work for the whole network |
| Start a thread | 49 | starting a thread, on top of the call |
| Call across a Gateway | 100 | calling a computer of another mod through a Gateway |
| Each extra argument across a Gateway | 5 | for each thing handed across a Gateway |
| Send across a Gateway | 20 | saying something to a computer across a Gateway, or turning one on or off |

File calls use the same levels. Reading a file costs the read price, plus the price for each block it reads.

## Calls for programs and processes

Table: Program and process calls

| Call | Price in instructions |
| --- | --- |
| `Program.SetName(name)` | free |
| `Program.Name, Program.Args, Program.Current` | free |
| `Program.Exit(code)` | free |
| `Program.OnMessage(handler)` | free |
| `Program.Start(path)` | 200, and the longer forms, with arguments and a priority, cost the same. |
| `Program.Shell(line)` | 200, plus 1 per row |
| `Process.Id, Process.Name, Process.Host` | free |
| `Process.Running` | 5 |
| `Process.ExitCode` | 5 |
| `Process.Wait()` | free, and so is the form with ticks. |
| `Process.Kill()` | 10 |
| `Process.Output()` | 50, plus 1 per row |
| `Process.Send(number, text)` | 10 |
| `Thread.Start(action)` | 49 |
| `Thread.Sleep, Thread.Yield, Thread.Join, Thread.Stop` | free |
| `Thread.Id, Thread.Current` | free |

## Calls for the computer and the network

Table: Computer, network and remote calls

| Call | Price in instructions |
| --- | --- |
| `Computer.Name, Cpu, Os, RamMb, FreeRamMb, Online` | 5 |
| `Computer.Disks, Programs, Processes` | 30 |
| `Network.Online, Network.Current, Network.Computer(name)` | 10 |
| `Network.Capacity, Network.Used, Network.Total(name)` | 50 |
| `Network.Computers, Network.Find(name), Network.Servers` | 50, plus 1 per row |
| `RemoteComputer.Start(path)` | 200 |
| `RemoteComputer.Shell(line)` | 200 |
| `RemoteComputer.Send(number, text)` | 10 |
| `RemoteComputer.Processes()` | 50, plus 1 per row |
| `Gateway.Computers` | 30, plus 1 per row |
| `Gateway.Send(number, text)` | 20 |
| `Gateway.OnMessage(handler)` | free |

## If something goes wrong

> [!PROBLEM] A program slows down when it reads a long list.
> A call that brings back a list costs more as the list grows, one row at a time. Ask for less, or make the call once and keep its answer.

> [!PROBLEM] A call that reads a big file costs more than expected.
> Reading a file costs 50 instructions, plus 1 for each 4 KB block. A 40 KB file is ten blocks, so reading it with File.Read costs 60 instructions.

> [!SEE] jsc:credits_per_tick, jsc:what_operations_are
