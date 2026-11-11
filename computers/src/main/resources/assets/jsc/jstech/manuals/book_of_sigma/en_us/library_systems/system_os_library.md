---
section: library_systems
order: 475
---
# System.OS: an operating system of your own

You can write an operating system of your own in Sigma#. It is a class that implements ISystem, in the namespace System.OS. The machine runs it as its process 1, and draws its pages, its files and its windows for it, so even a weak machine keeps up.

> [!DEFINE] System
> The class that implements ISystem: it declares what the system is, and its events run the machine.

> [!DEFINE] Kernel
> The rules a system runs by: how it shares the processor, its file system, its drivers, what it does when the memory runs out, and its shell.

Table: What a system declares

| Property | What it says |
| --- | --- |
| `Name, Version, Author` | The system's name, its version and who wrote it, as text. |
| `Era` | The oldest era of hardware the system installs on. |
| `Isa` | The instruction set the system is built for. |
| `MemoryMb` | The memory the system keeps for itself, in megabytes (MB). |
| `Kernel` | One of the mod's kernels, or one of the system's own. |

You write each of these with => and a value the compiler knows: one written out, or one the library names. The compiler reads them to know the system before it runs.

Table: Events of a system

| Event | When it runs |
| --- | --- |
| `OnBoot()` | Once, as the machine comes up. |
| `OnTick()` | On every tick while the system is up. |
| `OnInput(InputEvent e)` | When a key is pressed or the mouse is clicked on the system's own screen. |
| `OnShutdown()` | Once, as the machine goes down. |
| `int OnSchedule(List<int> ready)` | Optional. Before each slice, with the numbers of the processes ready to run, on a kernel of its own. It gives the number of the process that runs next. |
| `int OnOutOfMemory(string asker, int mb)` | Optional. When a program asks for more memory than the machine has, on a kernel of its own. It gives Memory.Refuse, Memory.GoDown, or the number of a process to end. |

Table: Era and Isa members

| Member | What it is |
| --- | --- |
| `Era.Vintage, Era.Legacy, Era.Transition, Era.Standard, Era.Advanced` | The eras of hardware. |
| `Isa.IA16, Isa.IA32, Isa.X86_64, Isa.M68K, Isa.PowerPC, Isa.ARM64` | The instruction sets. |
| `Era.Name, Isa.Name` | The era or the instruction set, as text. |

Table: Kernel members

| Member | What it does |
| --- | --- |
| `Kernel.Of(name)` | One of the mod's kernels, with all its rules: dos, win9x, nt, linux, freebsd, unix or net_min. |
| `Kernel.Own()` | A kernel of the system's own, which you choose the rules of with the members below. |
| `Kernel.Scheduler(kind), Kernel.Scheduler(kind, slice)` | How the processor is shared: none, cooperative or preemptive, with a slice of 16 to 256 instructions. A slice is 64 instructions when you give none. |
| `Kernel.FileSystem(kind)` | The file system: dos, posix or flat. |
| `Kernel.Drivers(classes)` | The devices the system drives, named in one text with commas: text, graphics, sound, network, disks, optical, usb and printer. |
| `Kernel.MemoryRule(rule)` | What happens when the memory runs out: refuse, end or down. |
| `Kernel.Shell(family)` | The shell: dos, posix, mc-net, or own for a screen the system writes itself. |
| `Kernel.Name` | The kernel's name, as text. |

## The rules of a kernel of its own

With no scheduler, only the system and the program in front run, and the others wait. A cooperative program runs until it calls Processes.Yield, waits or ends. A preemptive kernel shares the processor in slices, and a program of high priority gets two slices a round, a low one sits out one round in two, and an idle one runs only when every other program waits.

A device with no driver is not there for the system: with no sound driver it plays nothing, and with no network driver it is on no network.

```sgs LanternOs.sgs
using System.OS.*;
namespace Lantern;

class LanternOs : ISystem {
    public string Name => "Lantern OS";
    public string Version => "1.2";
    public string Author => "Alex";
    public Era Era => Era.Legacy;
    public Isa Isa => Isa.IA32;
    public int MemoryMb => 48;
    public Kernel Kernel => Kernel.Of("linux");

    public void OnBoot() {
        Boot.Line("Lighting the lantern...");
        Boot.Done();
        Desktop.Start("lantern-desktop");
    }
    public void OnTick() { }
    public void OnInput(InputEvent e) { }
    public void OnShutdown() { }
}
```

sigma build --os writes the system onto a blank medium of its era in one of the machine's drives, and sgpack --publish puts it on the network's Mirror, where every machine of the network can boot its installer through the firmware.

## If something goes wrong

> [!PROBLEM] The build stops with S3062.
> The system leaves out a property it has to declare. Add it, written with =>.

> [!PROBLEM] The system goes down on its own page as soon as it starts.
> The page names the error and its line. Fix that line, build again and install again.

> [!SEE] jsc:system_screen_library, jsc:system_processes_library, jsc:system_desktop_library
