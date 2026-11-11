---
section: automation_jobs
order: 153
---
# The Automation Manager

The Automation Manager is the window where you write and watch the jobs, without typing IQL. It is a desktop program, so you open it from the desktop, like the other programs ([](jsc:programs)).

It runs on Frames, Linux and FreeBSD. It does not run on UNIX System V. It needs a Mainframe on the network: without one, the window says "no Mainframe".

Table: The parts of the window

| Part | What it does |
| --- | --- |
| The top band | Green, with the engine's name and "online", while the Automation Engine runs; otherwise amber, with "No engine - install the Automation Engine on the Mainframe". |
| The job count | How many jobs the network keeps, such as 3 jobs. |
| The job list | One row for each job, with the columns below. |
| NEW JOB | The form that writes a new job, in four types. |

## The job list

Table: The columns of the job list

| Column | What it shows |
| --- | --- |
| JOB | The job's name. |
| TYPE | The kind of job: Restock below, Batch Craft, Periodic Move or Custom. |
| TRIGGER | The interval, such as every 5m, or the condition. |
| ACT | = pauses the job, > resumes a paused job, and x deletes the job at once, with no question. |

The window works out the kind of a job from its statement and trigger, so a job you type in IQL is listed the same way. A statement that starts with CRAFT is Restock below when its trigger is WHEN, and Batch Craft otherwise. A statement that starts with neither CRAFT nor MOVE is Custom.

## Making a job

1. Type a name in Name. Use one word, with no spaces.
2. Choose the type: Restock below, Batch Craft, Move or IQL.
3. Fill the fields that the type shows.
4. Click Create job. The fields clear, and the new job appears in the list.

Table: The four types

| Type | Fields | What the job does |
| --- | --- | --- |
| Restock below | Name, Item id, When below | Crafts the amount of the item when the network's stock of it falls below the amount. |
| Batch Craft | Name, Item id, Amount, Every | Crafts the amount of the item every interval. |
| Move | Name, Item (blank=all), Amount, From, To, Every | Moves the amount of the item from one place to the other every interval; a blank item moves everything. |
| IQL | Name, Script (on Mainframe disk), Every | Runs one .iql file from the Mainframe's system disk every interval. |

The Script field lists the .iql files on the Mainframe's system disk. If the disk has none, the field says "no .iql files on the Mainframe's system disk".

A Restock below job counts the stock that is in the network now. The window adds the note "Does not count what is already on its way." It also shows a tip that starts "Prophet YourIQL counts it: KEEP", with the item and the amount. Prophet is explained in ([](jsc:prophet_youriql)).

The amount takes digits only, and a blank amount counts as 1 item. An amount below 1 item becomes 1 item. The Every field takes an interval such as 30s or 5m, and it must be more than zero. The forms are the ones in ([](jsc:automation_engine)).

Creating a job with the name of an existing job replaces that job with the new one.

## If something goes wrong

> [!PROBLEM] The chat says "jobs are kept by the Automation Engine; install it on the Mainframe first".
> Install the Automation Engine on the Mainframe.

> [!PROBLEM] The chat says "Give the job a name."
> Type a name in Name.

> [!PROBLEM] The chat says "Restock below needs an item."
> Type an Item id, such as iron_ingot.

> [!PROBLEM] The chat says "Batch Craft needs an item and a valid interval (e.g. 30s, 5m)."
> Type an item and an interval such as 30s or 5m.

> [!PROBLEM] The chat says "Periodic Move needs FROM, TO, and a valid interval (e.g. 30s)."
> Type the From and To places, and an interval such as 30s.

> [!PROBLEM] The chat says "An IQL Script job needs a .iql file and a valid interval (e.g. 30s)."
> Choose a script in the list, and type an interval such as 30s.

> [!PROBLEM] The chat says that the script is not found on the Mainframe disk.
> Save the .iql file on the Mainframe's system disk, then choose it.

> [!PROBLEM] The band says "No engine" although the engine is installed.
> The band shows the engine only while the Mainframe runs. Turn the Mainframe on.

> [!SEE] jsc:automation_engine, jsc:programs
