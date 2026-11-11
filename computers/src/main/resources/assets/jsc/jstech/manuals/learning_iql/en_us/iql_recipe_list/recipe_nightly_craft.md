---
section: iql_recipe_list
order: 402
icon: jsc:mainframe
---
# Craft on an interval

This recipe has the network craft an item again and again, with a gap between each craft. You set the gap as an interval, written as a duration after EVERY, such as 30s or 1h ([](jsc:iql_jobs)).

The interval counts game ticks, so it measures game time, not the time of day. A job fires only while the Automation Engine is installed and the Mainframe is on.

A craft cannot be set to a time of day. The only time window in IQL belongs to a bus ([](jsc:bus_conditions)).

Table: What the recipe needs

| Need | What it takes |
| --- | --- |
| Era | Legacy and newer, with the Automation Engine |
| Program | The Automation Engine installed on the Mainframe, and the Mainframe on |
| Engine | The Midsoft IQL Server running: the other engines keep no jobs |
| Pattern | A pattern for the item on the network |

1. Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).
2. Make sure the network holds a pattern for the item ([](jsc:patterns)).
3. Type the job at the prompt, with iql in front and its number and its interval.
4. Wait for one interval. The first run comes one interval after the job's first check.

```table
{"caption": "A line for a job", "rows": [[{"words": "Craft 64 torches every hour"}, {"literal": "CREATE JOB torch_hourly AS CRAFT 64 torch EVERY 1h"}]]}
```

The Automation Manager has the same feature as a form, called Batch Craft. Its field is labelled Every (30s), and it takes a duration such as 30s or 5m.

Table: Forms of a duration

| Written | Ticks | Seconds |
| --- | --- | --- |
| `100t` | 100 | 5 |
| `30s` | 600 | 30 |
| `5m` | 6,000 | 300 |
| `1h` | 72,000 | 3,600 |

A bare number counts seconds. A letter after the number changes the unit: s for seconds, m for minutes, h for hours and t for ticks. There are 20 ticks in a second.

Always write the number. A CRAFT with no number asks the network for as much as it can make, every time the job runs.

## If something goes wrong

> [!PROBLEM] The reply reads "no pattern crafts torch".
> The network holds no pattern for the item. Give it one first ([](jsc:patterns)).

> [!PROBLEM] The reply reads "unknown item: torches".
> Use the item's id, such as torch, and not its name.

> [!PROBLEM] The reply reads "the network has no running Mainframe".
> Turn the Mainframe on. The job's craft cannot start without it.

> [!PROBLEM] The reply reads "jobs are kept by the Automation Engine; install it on the Mainframe first".
> Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).

> [!PROBLEM] The reply reads "the network's engine keeps no views, procedures or jobs".
> The Automation Manager says "Batch Craft needs an item and a valid interval (e.g. 30s, 5m)."

> [!PROBLEM] Fill in the item, and an interval above zero such as 30s or 5m.
> Use the Midsoft IQL Server for the job ([](jsc:engines)).

> [!SEE] jsc:iql_jobs, jsc:automation_manager, jsc:how_autocrafting_works, jsc:asking_for_a_craft
