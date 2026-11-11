---
section: shell_commands
order: 254
---
# Processes, jobs and scheduling

A process is a program that is running now. Each process has a number, its process ID (PID), and the ps command lists them with it. It lists the system's own processes first. Number 1 is the first process, which starts every other one: systemd on most Linux distributions, and init on Gentoo, FreeBSD and UNIX. Each service the system runs comes next, and the programs you start are numbered from 1000.

> [!DEFINE] Process
> A program that is running. Every running program has a number, which ps lists as its PID.

The kill command stops a process by its number. The process stops where it stands, and any work it had not finished stays not done. A signal can come before the number, such as -9 or -KILL, and kill -l lists the signals. A service you stop stays stopped until the computer starts again. Stopping process 1 takes the whole system down: the kernel panics, prints why on the screen, and the computer starts over by itself.

A job is a background command you started with an ampersand. You list your jobs with jobs, and you stop one with kill %1, where 1 is its number in that list.

```command
id: jsc:command/linux/ps
```

```command
id: jsc:command/linux/kill
```

When the computer runs out of memory, the kernel, the core of the system, ends the process that holds the most, then the next, until everything left fits. The dmesg command prints what the kernel has logged since the computer started: what it found as it came up, and a line for each process it ended. On a desktop, a window the kernel ends closes with a balloon, "Out of memory", that names it. A program ended in front of the terminal leaves the word Killed. Only when nothing is left to end does the kernel panic, and then the computer starts over by itself. A server in a rack does all of this as a computer does, each server on its own, and a service the kernel ends there, or that you stop, serves the network no more until that server starts again.

```command
id: jsc:command/linux/dmesg
```

```command
id: jsc:command/linux/jobs
```

The crontab command sets a line that the machine runs by itself, at an hour you choose, every day or on named days. Such a line is a schedule.

> [!DEFINE] Schedule
> A line that the machine runs by itself, at a set hour, every day or on named days.

Table: Arguments of crontab

| Argument | What it does |
| --- | --- |
| `-l` | Lists the lines the machine runs, and when. |
| `-r <id>` | Removes the line with that number. |
| `06:00` | The hour the line runs. The form 6 also works. |
| `M, T, W, Th, F, Sa, Su` | The days, separated by commas. Leave them out to run every day. |

```command
id: jsc:command/linux/crontab
```

## Mining in the background

The Blockwright miner mines Teracoin at the prompt or behind it ([](jsc:mining)). miner --address and your wallet's address tells it whom to pay; miner watches it mine until you press Q, miner start mines in the background whenever the machine is on, and miner stop ends that.

```command
id: jsc:command/linux/miner
```

## If something goes wrong

> [!PROBLEM] The kill command says kill: not a process id.
> Give the number of the process from the ps list, not its name.

> [!PROBLEM] The kill command says there is no such job.
> The jobs command shows the current job numbers. Use one of them after the percent sign.

> [!PROBLEM] The crontab command says the word you gave is not an hour of the day.
> Write the hour as "06:00" or as "6".

> [!SEE] jsc:pipes_and_variables, jsc:system_commands
