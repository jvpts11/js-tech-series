---
section: automation_jobs
order: 152
icon: jsc:mainframe
---
# The Automation Engine and jobs

Some work should happen by itself, so you do not have to watch it: restock an item when the network runs short, or move items every hour. A job is a saved statement of IQL, the network's query language, with a trigger that says when the statement runs. The Automation Engine is the service that keeps the network's jobs and fires them on time, or when a condition holds.

> [!DEFINE] Job
> A saved IQL statement that the network runs by itself, at a set time or when a condition holds.

> [!DEFINE] Automation Engine
> The Mainframe's service that keeps the jobs and fires them. It has no window of its own.

Install the Automation Engine on the Mainframe, as you install any service ([](jsc:mainframe_services)). It has no on or off switch of its own. It fires jobs while it is installed and the Mainframe is running, with a valid build and power on.

If the Mainframe is off, the jobs stay in the list and none of them fires. Turn the Mainframe back on, and they fire again.

## Triggers

You give a trigger after EVERY, with an interval, or after WHEN, with a condition. The Automation Engine checks the jobs every 10 ticks, which is half a second.

> [!DEFINE] Trigger
> The part of a job that says when it fires: an interval after EVERY, or a condition after WHEN.

EVERY fires the job again after each interval. The first check only starts the count, so a new EVERY job waits one full interval before its first run.

WHEN fires the job on the rising edge of its condition: the moment the condition turns from false to true. It does not fire again while the condition stays true. A condition that is already true at the first check fires the job once, at that check.

> [!DEFINE] Rising edge
> The moment a condition turns from false to true. A WHEN job fires once for each such moment.

A WHEN condition tests the network's stock of an item, with qty(<item>), against a number, or the world's clock, with clock = '02:00' and weekday = 'Sunday'. The operators are =, ==, !=, <, >, <=, and >=. An item name without a colon means minecraft:, so iron_ingot is minecraft:iron_ingot. A modded item needs its full name, with its mod's name before the colon.

WHEN EVENT waits for a world event the network sees: WHEN EVENT mymod:solar_storm COMING fires as the network first sees it coming, which needs a machine on the network that is the event's instrument; UNDER WAY fires as it begins where the network stands; ENDED fires as it ends. Name the event by its id, or by the part after the colon.

```iql storm_shelter.iql
CREATE JOB shelter AS SET BUS 'Ore in' OFF WHEN EVENT mymod:solar_storm COMING
```

Table: Forms of a duration

| Form | Meaning | Ticks |
| --- | --- | --- |
| `30s` | 30 seconds | 600 |
| `5m` | 5 minutes | 6,000 |
| `1h` | 1 hour | 72,000 |
| `100t` | 100 ticks | 100 |
| `30` | 30 seconds, a bare number | 600 |

The unit letters are s for seconds, m for minutes, h for hours and t for ticks. A number with no letter counts seconds. There are 20 ticks in a second.

## Writing a job

1. Install the Automation Engine on the Mainframe.
2. At the prompt of a computer on the network, type a CREATE JOB line, as the listing shows.
3. Press [[Enter]]. The answer ends with "created" and names the job.
4. Check the job on the Processes tab, or in the Automation Manager.

```iql keep_iron.iql
CREATE JOB keep_iron AS CRAFT 64 iron_ingot EVERY 5m
```

Write the name, then AS and the statement the job runs, then EVERY with a duration or WHEN with a condition. Names are up to 64 characters long. DROP JOB followed by the name removes a job. The IQL book explains the job statements in full ([](jsc:iql_jobs)).

## Watching the jobs

The Processes tab of the Task Manager ([](jsc:system_monitor_and_task_manager)) lists the network's engine first, then each job with its state.

Table: States of a job

| State | What it means |
| --- | --- |
| `ACTIVE` | The Automation Engine is running, so the job can fire. |
| `IDLE` | The Automation Engine is not running, so the job does not fire. |
| `PAUSED` | The job is paused, and it does not fire until it is restarted. |

The detail beside a job shows its trigger, such as "every 5m" or "when qty(iron_ingot) < 64". On that tab, Start and Stop act on the network's engine, not on the jobs.

## Pausing and restarting

Pausing a job keeps it in the list and stops it from firing. Restarting a job resumes it, and an EVERY job counts its interval again from now. DROP JOB removes the job and its pause with it.

## Timed backups

A backup you give an hour in the Backup Console is a job here too, named backup_ and the backup's name, such as backup_Nightly: it runs BACKUP Nightly WHEN clock = '02:00'. The console makes it, changes it and drops it with the backup, so change the hour in the console rather than here ([](jsc:backups)).

## If something goes wrong

> [!PROBLEM] It says "jobs are kept by the Automation Engine; install it on the Mainframe first".
> Install the Automation Engine on the Mainframe, then type the job again.

> [!PROBLEM] It says "the network's engine keeps no views, procedures or jobs".
> Use an engine that keeps saved objects, such as the Midsoft IQL Server ([](jsc:engines)).

> [!PROBLEM] It says "CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>".
> Add EVERY with a duration, or WHEN with a condition, at the end of the line.

> [!PROBLEM] It says "CREATE JOB needs a body before its trigger".
> Write the statement between AS and EVERY or WHEN.

> [!PROBLEM] It says "CREATE JOB trigger needs a value".
> Write the duration or the condition after EVERY or WHEN.

> [!PROBLEM] It says "CREATE expects VIEW, PROCEDURE, or JOB".
> Write VIEW, PROCEDURE or JOB after CREATE.

> [!PROBLEM] It says that CREATE needs a name and an AS body, or that AS must follow the name.
> Write CREATE JOB, the name, then AS and the statement.

> [!PROBLEM] It says "a name is at most 64 characters long".
> Choose a shorter name, of no more than 64 characters.

> [!PROBLEM] A job made in IQL with a bad interval never fires.
> IQL does not check an EVERY duration when you type the job, so a bad one never fires. Write a number with an optional unit: s, m, h or t, such as 30s or 100t.

> [!PROBLEM] A job is listed as PAUSED.
> Restart it with the > control in the Automation Manager.

> [!PROBLEM] The job fires, but nothing moves.
> The job runs as ordinary Operations. Look at the Operations it started ([](jsc:operations)), where each one shows its status.

> [!SEE] jsc:automation_manager, jsc:engines, jsc:operations
