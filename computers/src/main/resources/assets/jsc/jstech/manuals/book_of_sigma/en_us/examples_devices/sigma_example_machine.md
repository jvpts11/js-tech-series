---
section: examples_devices
order: 505
---
# Machine

This example reads the machine it runs on: its name, its processor, its system, its memory, its disks and its processes. It also reads and writes files, with the paths the shell uses.

The records the machine gives are pictures, taken when the program asks. A program that needs the state again asks again, and keeps what it was told in the meantime. If you change the machine, ask again to see the new state.

## The program

```sgs Machine.sgs
using System.*;
using System.IO.*;
using System.Collections.*;
using System.Machine.*;
namespace Corpus;
class MachineCorpus {
    static void Main() {
        bool there = File.Exists("a.txt");
        string text = File.Read("a.txt");
        if (File.TryRead("a.txt", out string held)) {
            Console.PrintLine(held);
        }
        bool wrote = File.Write("a.txt", "x") && File.Append("a.txt", "y");
        bool tidied = File.Delete("a.txt") && File.MkDir("d");
        foreach (string entry in File.List("C:\\")) {
            Console.PrintLine(entry);
        }
        Console.PrintLine("" + there + text + wrote + tidied);

        string name = Computer.Name;
        CpuInfo cpu = Computer.Cpu;
        OsInfo os = Computer.Os;
        int ram = Computer.RamMb + Computer.FreeRamMb;
        bool online = Computer.Online;
        Console.PrintLine(name + cpu.Mhz + cpu.Cores + cpu.Era + cpu.Architecture
            + os.Id + os.Name + ram + online);
        foreach (DiskInfo disk in Computer.Disks()) {
            Console.PrintLine(disk.Mount + disk.UsedMb + disk.CapacityMb);
        }
        List<string> programs = Computer.Programs();
        foreach (ProcessInfo process in Computer.Processes()) {
            Console.PrintLine("" + process.Id + process.Name + process.State + process.HeldBytes + programs.Count);
        }
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `File.Exists("a.txt")` | Tells whether the file is there. |
| `File.Read("a.txt")` | Reads the whole file as text. The program stops when the file is not there. |
| `File.TryRead("a.txt", out held)` | Reads the file and answers true when it could. The text goes to held. |
| `File.Write, File.Append` | Write text to the file, or add text to its end. Each answers true when it happened. |
| `File.Delete, File.MkDir` | Delete a file, or make a folder. Each answers true when it happened. |
| `File.List(...)` | Lists the entries of a folder, one at a time. |
| `Computer.Name, Cpu, Os` | The machine's name, its processor (speed, cores, era and architecture), and its system. |
| `Computer.RamMb, FreeRamMb` | The memory of the machine in megabytes, and the part of it that is free. |
| `Computer.Disks()` | Gathers each disk, with its mount, the megabytes used and its capacity. |
| `Computer.Programs()` | Lists the programs on the machine. |
| `Computer.Processes()` | Gathers each process, with its number, name, state and the bytes it holds. |

> [!DEFINE] Path
> The place of a file or a folder, written with the folders that hold it.

> [!DEFINE] Block
> The unit a file is read or written in. The cost of reading or writing grows with each block.

The processor record names its era, which is the hardware era of the machine ([](jsc:eras)).

## If something goes wrong

> [!PROBLEM] File.Read is asked for a file that is not there, and the program stops.
> Use File.TryRead instead, which answers false when the file is not there.

> [!PROBLEM] A write does not happen, because the disk is full, and the write answers false.
> Check the answer of each write, and free some space on the disk.

> [!PROBLEM] A read or a write of a big file costs more, block by block.
> Read the file once, and keep its text in a variable.

> [!SEE] jsc:sigma_library, jsc:eras
