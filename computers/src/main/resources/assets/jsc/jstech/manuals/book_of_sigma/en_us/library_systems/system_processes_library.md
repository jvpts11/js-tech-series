---
section: library_systems
order: 477
---
# Processes, memory, disks, services and drivers

With these you read the machine your system runs on, and run its programs. Any program can read the machine with them, and only a player's system has services of its own.

Table: Processes members

| Member | What it does |
| --- | --- |
| `Processes.Start(path), Processes.Start(path, args)` | Starts a program from the disk, and gives its process. |
| `Processes.Stop(pid, signal)` | Stops the process with that number. |
| `Processes.List()` | Every process the machine runs. |
| `Processes.SetPriority(pid, level)` | high, normal, low or idle. |
| `Processes.Yield()` | Gives the rest of the turn to the next program. |
| `Processes.Current` | The process that asks. |

Table: Disk and Memory members

| Member | What it gives |
| --- | --- |
| `Disk.FreeMb, Disk.SizeMb` | The room left on the system's disk and its size, in megabytes (MB). |
| `Disk.Mounted()` | The other disks and media the machine reads. |
| `Memory.TotalMb, Memory.FreeMb` | The machine's memory and what is free of it, in megabytes (MB). |
| `Memory.HeldBy(pid)` | What one process holds, in megabytes (MB). |
| `Memory.Ledger()` | A line for each thing that holds memory. |
| `Memory.Refuse, Memory.GoDown` | What OnOutOfMemory gives to refuse, and to go down. |
| `Memory.Alloc, AllocZeroed, Resize and Free` | The program's own raw memory, which pointers reach, from version 3, as the page on pointers shows. |
| `Memory.Copy, Fill and Compare` | Bytes of raw memory moved, set or compared, one instruction for every 64 of them. |

Table: Services and Drivers members

| Member | What it does |
| --- | --- |
| `Services.Register(name, path)` | Names a program on the disk as a service, which starts when the system comes up. |
| `Services.Start(name), Services.Stop(name)` | Starts or stops a service. Each gives true when it did. |
| `Services.Status(name)` | running, stopped or unknown. |
| `Drivers.Devices()` | The devices the system drives. |
| `Drivers.Has(class)` | Whether the machine has a device of that class that the system drives. |
| `Device.Name, Device.Class` | A device's name, and its class. |

Sigma reaches the members that take and give plain values through Standard, and leaves what gives a list or a process to Sigma#.

## If something goes wrong

> [!PROBLEM] Services.Start gives false.
> No program of that name is on the disk. Build the service into the system's folder.

> [!SEE] jsc:system_os_library
