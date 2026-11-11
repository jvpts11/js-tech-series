---
section: prompt_system
order: 242
---
# Tasks, starts in the background and the schedule

A task is a program that the computer is running right now. A job is a line the computer keeps to run later, or at an hour of every day. These verbs list and stop the tasks, start commands in the background, and keep the schedule of the jobs.

> [!DEFINE] Task
> A program that the computer is running right now.

> [!DEFINE] PID
> The process identifier: the number that names one task.

> [!DEFINE] Job
> A line the computer keeps to run later or at an hour. It holds memory while it waits.

> [!DEFINE] Schedule
> The list of jobs, each with its hour and its days.

## tasklist and taskkill

tasklist lists the tasks the computer is running. Its columns are PID, NAME, FILE, STATE and MEM. MEM shows the memory that each task holds of the memory promised to it. When nothing runs, it says "No tasks are running."

On the Frames editions, tasklist works from Frames XP on. Frames 95 does not have it.

```command
id: jsc:command/frames/tasklist
```

taskkill stops a task. Give it the number with /PID, as in taskkill /PID 12, or write /PID:12. The number is the one that tasklist shows. Like tasklist, it works on Frames XP and later.

```command
id: jsc:command/frames/taskkill
```

## where and start

where looks up a command by its name and reports where the machine has it. If it finds nothing, it says INFO: Could not find "NAME". With no name, it prints its usage. It works on Frames XP and later.

```command
id: jsc:command/frames/where
```

start runs a command and gives you the prompt back at once, while the command keeps running. On a machine that runs one thing at a time, it takes the started commands in turn. Each started command holds 1 MB of memory while it runs.

```command
id: jsc:command/mc_dos/start
```

## at and the schedule

at runs a command at an hour of the day, every day or only on the days that you name. The smallest time step a job can use is one hour of the world's clock.

Table: Forms of at

| Form | What it does |
| --- | --- |
| `at` | Lists the jobs, with their hours and days. |
| `at 06:00 COMMAND` | Runs the command every day at that hour. |
| `at 06:00 /EVERY:M,W,F COMMAND` | Runs the command only on the days named: M, T, W, Th, F, Sa and Su. |
| `at 2 /DELETE` | Takes job 2 off the list. |

For example, AT 06:00 INTERAC GET 64 COAL /LOCAL brings 64 coal into this computer every morning at six. Only the hour counts: the minutes after the colon are not kept, so write 06:00.

When at adds a job, it prints "Added a new job with ID = " and the number. Deleting a job prints "Deleted job" and its number. A job number that does not exist gets "The job ID does not exist." With no jobs, at says "There are no entries in the list."

```command
id: jsc:command/mc_dos/at
```

## Mining in the background

The Blockwright miner mines Teracoin at the prompt or behind it ([](jsc:mining)). MINER --ADDRESS and your wallet's address tells it whom to pay; MINER watches it mine until you press Q, MINER START mines in the background whenever the computer is on, and MINER STOP ends that.

```command
id: jsc:command/mc_dos/miner
```

## If something goes wrong

> [!PROBLEM] tasklist says No tasks are running.
> No program is running on the computer right now. Start a program, then run tasklist again.

> [!PROBLEM] taskkill says /PID takes the number of a task.
> Write /PID, then the number that tasklist shows for the task.

> [!PROBLEM] start says Not enough memory to start another task.
> Stop a task with taskkill, or add memory ([](jsc:frames_memory)).

> [!PROBLEM] at says The time is not in the right format.
> Write the hour as 06:00 or 6. Write the days as M, T, W, Th, F, Sa or Su, separated by commas.

> [!PROBLEM] A job did not run at the minute you asked.
> Only the hour is kept. Schedule the job for the hour you need.

> [!SEE] jsc:task_manager
