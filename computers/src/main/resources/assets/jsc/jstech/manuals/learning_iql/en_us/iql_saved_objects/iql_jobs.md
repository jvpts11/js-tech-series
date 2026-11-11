---
section: iql_saved_objects
order: 380
---
# Jobs, EVERY and WHEN

> [!DEFINE] job
> A statement that the network runs on its own, each time its trigger fires.

> [!DEFINE] tick
> One step of the game's clock. 20 ticks make one second.

A job is a statement that the network runs on its own. You give it a trigger: a time interval with EVERY, or a condition with WHEN. Each time the trigger fires, the statement runs again.

Jobs are kept by the [Automation Engine](jsc:automation_engine), which must be installed on the Mainframe. Jobs also need the [Midsoft IQL Server](jsc:midsoft_iql_server).

```iql jobs.iql
CREATE JOB restock AS SELECT 64 iron_ingot EVERY 30s
CREATE JOB low_gold AS SELECT 64 gold_ingot WHEN qty(gold_ingot) < 100
```

The trigger word is the last EVERY or WHEN that stands outside quotes and braces. Jobs run only while the Automation Engine is installed and running. A paused job stays idle until you restart it from the Processes tab. A job is credited to the player who created it.

## EVERY: a time interval

Table: Writing a duration

| Written as | Means | Ticks |
| --- | --- | --- |
| `30` | 30 seconds, since a bare number counts seconds | 600 |
| `100t` | 100 ticks | 100 |
| `30s` | 30 seconds | 600 |
| `5m` | 5 minutes | 6,000 |
| `1h` | 1 hour | 72,000 |

The network checks the jobs every 10 ticks, so an interval is exact to about 10 ticks. The first check starts the clock, so a job does not fire at once.

## WHEN: a condition

WHEN fires the job once, when its condition turns from false to true. It does not fire again while the condition stays true. A condition that is already true fires at the first check.

A WHEN reads three fields: qty(item), which is the network's total of that item; clock, the world's clock as 'HH:MM'; and weekday, the day of the week by its name. A WHEN with any other field never fires.

A CREATE JOB whose statement is SET BUS with WHEN TIME does not make a job. It sets the hours of the bus instead ([](jsc:set_bus)).

## WHEN on the clock, and BACKUP

WHEN clock = '02:00' fires the job at two in the morning by the world's clock, once a day: the clock reads that minute only, so the condition turns true once. Add AND weekday = 'Sunday' to fire it on Sundays only. The days are Monday to Sunday, and the world's day, like its clock, starts at six in the morning.

BACKUP runs one of the Backup Service's jobs now, by its name: BACKUP Nightly. A backup you give an hour in the Backup Console is such a job, which the console makes for you ([](jsc:backups)).

```iql nightly.iql
CREATE JOB backup_Nightly AS BACKUP Nightly WHEN clock = '02:00'
```

Table: The replies for jobs

| Reply | What it means |
| --- | --- |
| `job <name> dropped` | The job no longer runs. |
| `no job named <name>` | No job has that name. |
| `DROP JOB needs a name` | Type the name after DROP JOB. |

## If something goes wrong

> [!PROBLEM] CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>
> Add EVERY and a duration, or WHEN and a condition, after the statement.

> [!PROBLEM] CREATE JOB needs a body before its trigger
> Put the statement before EVERY or WHEN.

> [!PROBLEM] CREATE JOB trigger needs a value
> Type the duration or the condition after the trigger word.

> [!PROBLEM] jobs are kept by the Automation Engine; install it on the Mainframe first
> Install the Automation Engine on the Mainframe, then create the job.

> [!PROBLEM] not a duration: <text>
> Type a number, with t, s, m or h after it if you want, such as 30s.

> [!PROBLEM] empty duration
> Type a duration after EVERY, such as 30s.

> [!PROBLEM] duration must be >= 0: <text>
> Type a duration of 0 or more.

> [!PROBLEM] unknown duration unit '<unit>' in: <text>
> Use t, s, m or h as the unit, or no unit for seconds.

> [!PROBLEM] The job never fires, and its interval is 0.
> Use an interval above zero, such as 30s.

> [!PROBLEM] The job never fires, and no error appears.
> Its WHEN condition does not read. Check the words and the spelling of the condition.

> [!PROBLEM] The job does not fire at all.
> Check that the Automation Engine is installed and running, and that the job is not paused.

> [!PROBLEM] no job named <name>
> Check the name, or create the job first.

> [!SEE] jsc:automation_engine, jsc:iql_procedures, jsc:set_bus
