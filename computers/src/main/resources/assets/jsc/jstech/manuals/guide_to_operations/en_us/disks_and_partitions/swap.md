---
section: disks_and_partitions
order: 34
icon: jsc:disk_vaultis_trench_20m
---
# Swap and the page file

When the programs want more memory than the machine has, the system lends them disk space instead: the swap on Linux, the BSDs and UNIX, and the page file on Frames. A machine promises its memory and its swap together, so more programs fit.

The part of a program that sits in the swap runs slower, by the disk you put the swap on.

Table: How fast the part in the swap runs

| Disk | Speed |
| --- | --- |
| Hard disk | A quarter of its speed |
| SSD | 60% of its speed |
| NVMe | 90% of its speed |

Frames sizes its page file as it starts: one and a half times the memory on Frames 95 and XP, and the same as the memory from Frames 7 on. The page file weighs on the system's disk like its files do. The other systems swap to a swap partition, which their installers make as big as the memory.

> [!DEFINE] Swap
> Disk space a system lends its programs when the memory runs out.

> [!DEFINE] Page file
> The swap of Frames: a file on the system's disk.

## Where to see it

Table: The swap on each system

| System | Where |
| --- | --- |
| Frames XP | The Task Manager's Performance tab: PF Usage, and Commit in the status bar |
| Frames 10 and 11 | The Task Manager's Memory page, with what each program has in the page file |
| Linux desktops | The System Monitor's swap line |
| Linux | `free -h, swapon --show, top` |
| FreeBSD | `swapinfo, sysctl vm.swap_total` |

```session
{"lines": [{"prompt": "player@pc:~$ ", "text": "swapon --show", "typed": true}, {"text": "NAME      TYPE      SIZE USED PRIO"}, {"text": "/dev/sda2 partition    4G   0B   -2"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

## If something goes wrong

> [!PROBLEM] Programs run slowly when many are open.
> Part of them is in the swap. Add memory, or put the swap on a faster disk.

> [!PROBLEM] A program is refused for want of memory.
> The memory and the swap are both promised. Close a program, or add memory.

```command
id: jsc:command/linux/free
```

```command
id: jsc:command/linux/swapon
```

```command
id: jsc:command/freebsd/swapinfo
```

> [!SEE] jsc:memory, jsc:system_monitor_and_task_manager
