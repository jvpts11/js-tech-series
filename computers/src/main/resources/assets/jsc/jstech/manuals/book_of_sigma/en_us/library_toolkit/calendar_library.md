---
section: library_toolkit
order: 466
---
# Calendar and Stopwatch

Calendar reads the world's calendar where the machine stands, and a Stopwatch times a stretch of your program. Both live in System.Utils and came with Sigma# 3.

Table: Calendar members

| Member | What it gives |
| --- | --- |
| `Calendar.Hour, Calendar.Minute` | The time the clock shows. A day starts at six in the morning. |
| `Calendar.Day, Calendar.Week, Calendar.Year` | Which day, week and year it is, counted from 0. |
| `Calendar.DayOfWeek, Calendar.DayOfSeason` | The day within the week and within the season, from 0. |
| `Calendar.Season` | spring, summer, autumn or winter. |

Table: Stopwatch members

| Member | What it gives |
| --- | --- |
| `Stopwatch.Start()` | A stopwatch started now. |
| `Stopwatch.Ticks` | The world ticks gone by since it started. |
| `Stopwatch.Credits` | The credits your program spent since it started. |
| `Stopwatch.Restart()` | Starts it again from now. |

```sgs Timing.sgs
Stopwatch watch = Stopwatch.Start();
Report();
Console.PrintLine("The report cost " + watch.Credits + " credits");
```

> [!DEFINE] Credit
> A unit of a program's work: an instruction costs one, and asking the machine costs more.

## If something goes wrong

> [!PROBLEM] Stopwatch.Ticks stays at 0 ticks.
> Your program has not let a tick go by: the clock moves between ticks, not inside one.

> [!SEE] jsc:numbers_and_time, jsc:credits_per_tick
