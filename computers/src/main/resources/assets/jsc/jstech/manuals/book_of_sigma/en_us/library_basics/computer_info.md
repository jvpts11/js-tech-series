---
section: library_basics
order: 442
---
# Computer: what the machine is

A program can ask the machine it runs on what it is: its name, its processor, its system, its memory and its disks. The answers are pictures taken when you ask, not views that update by themselves.

You read the machine through Computer, a built-in type whose members you read directly. Write Computer.Name, not the name of a variable that holds a computer.

Table: Computer members

| Member | What it gives |
| --- | --- |
| `Computer.Name` | The name of the machine, as text. |
| `Computer.Cpu` | The processor (CPU), as a CpuInfo record. Sigma# only. |
| `Computer.Os` | The system, as an OsInfo record. Sigma# only. |
| `Computer.RamMb` | The memory in megabytes (MB), as a whole number. |
| `Computer.FreeRamMb` | The memory free now, in megabytes (MB). Sigma# only. |
| `Computer.Online` | Yes or no. |
| `Computer.Disks()` | A list of DiskInfo records. Sigma# only. |
| `Computer.Programs()` | The names of the programs. Sigma# only. |
| `Computer.Processes()` | A list of ProcessInfo records. Sigma# only. |

Name, RamMb and Online work in Sigma too. The rest are Sigma# only.

## The records

A record is a value with fixed fields, and you read each field by its name. These are the records the Computer members give back.

Table: Records of Computer

| Record | Fields | What each field holds |
| --- | --- | --- |
| `CpuInfo` | `CpuInfo.Mhz, CpuInfo.Cores, CpuInfo.Era, CpuInfo.Architecture` | Mhz: the fastest clock of all the cores, in megahertz (MHz). Cores: all the cores together. Era: the era of the first processor. Architecture: the instruction set it runs. |
| `DiskInfo` | `DiskInfo.Mount, DiskInfo.UsedMb, DiskInfo.CapacityMb` | Mount: the drive letter. UsedMb and CapacityMb: megabytes used and in all. |
| `OsInfo` | `OsInfo.Id, OsInfo.Name` | The system's id and its name. |
| `ProcessInfo` | `ProcessInfo.Id, ProcessInfo.Name, ProcessInfo.State, ProcessInfo.HeldBytes` | The process number, its name, its state as text, and the bytes of memory it holds. |

The era names are vintage, legacy, transition, standard, advanced, exa and singularity, written in lower case. A machine has seven eras to choose from, and an era is not the same thing as a hardware tier.

> [!DEFINE] Architecture
> The instruction set a processor runs. A program built for one runs on that processor and on every newer one.

> [!DEFINE] Mount
> The drive letter a disk is reached by.

> [!DEFINE] Property
> A value that belongs to an object or a record, and that you read by its name, such as Computer.Name.

## If something goes wrong

> [!PROBLEM] A value looks out of date.
> Ask again. Each read takes a new picture of the machine.

> [!PROBLEM] A Sigma program does not see Computer.Cpu.
> Use Sigma#, which offers it, or use the members Sigma offers here.

> [!SEE] jsc:the_systems, jsc:processors, jsc:eras
