# Diagnostics

How a mod on J's Core lets a server's administrator see what its work costs and find what goes wrong: sections of work
that profilers show by name, a watchdog for stalls, budgets a system keeps to by slowing itself, what is sent over the
network, a measure of what each block costs, `/jstech health`, crash reports and a diagnostic file.

The Core does not make a profiler of its own. Spark does that very well; the Core makes its work show up in Spark's
profiles by name, and does the rest, which Spark does not.

## Sections of work

*Added 2026-10-15.*

A section is a named piece of a system's work: moving a dimension's energy through its wires, spreading a field,
working out a network. A mod declares each once, as a constant, and runs the work through it:

```java
public static final Section SPREAD = Section.builder(rl("fallout_spread"), LOG).declare();

SPREAD.run(() -> fallout.spread(level));
int found = SPREAD.call(() -> network.search(start));
```

The second argument is the `LogSystem` the work belongs to ([While you build](DEVELOPING.md#the-log-one-part-at-a-time)),
whose log the watchdog writes to. Around the work, a section:

- **is timed**, and its average and longest time show in the right column of the F3 screen, with the other timings;
- **runs in a frame of its own name.** The work is called from a small class made for the section, so a profiler
  that samples the threads' stacks shows it: in Spark, `jscore:energy_grid` appears as
  `Section$jscore$energy_grid.run()` inside whatever called it. It needs nothing from Spark, so it works the same in
  Java Flight Recorder, VisualVM, a thread dump and a crash report's stack;
- **is watched** by the watchdog, and **kept to its budget** when it has one (below).

The Core's own sections are the energy grid (`jscore:energy_grid`), the fluid grid (`jscore:fluid_grid`) and the
results of work away from the tick coming back (`jscore:task_results`).

## The watchdog

*Added 2026-10-15.*

A thread of the Core looks at every thread running a section four times a second. A run that goes on longer than it
may, one second unless the section or the server says otherwise, is a stall:

- the first stall of each section is written to its system's log, at warning level, with the stack of the thread it
  runs on, and the operators who hold `jscore.diagnostics.warnings` get a notification
  ([Notifications](NOTIFICATIONS.md));
- every stall is counted, and its length kept once it ends, for `/jstech health`.

```java
public static final Section REBUILD = Section.builder(rl("rebuild"), LOG)
        .stallAfter(Duration.ofSeconds(5)).declare();   // a rebuild may take longer
```

The game's own watchdog stops a server whose tick never ends; this one says which part of the series' work a long
tick was in, and while the server is still running.

## Budgets: slowing down gracefully

*Added 2026-10-15.*

A system whose work can be spread out (a field that spreads, a search that can wait) declares a budget: the most it
may take a tick, averaged over a second. While it takes more, it slows itself to run one tick in two, then in four,
up to one in 64, and says so in its log and to the operators; once it takes less than half its budget, it speeds back
up a step a second.

```java
public static final Section SPREAD = Section.builder(rl("fallout_spread"), LOG)
        .budget(Duration.ofMillis(1)).declare();

if (SPREAD.due(level.getGameTime())) {
    SPREAD.run(() -> fallout.spread(level, SPREAD.stride()));   // stride() ticks' worth at once
}
```

`due` says whether the work runs this tick; `stride` is how many ticks it is spread over now, so the system makes up
the ticks it skipped and goes on at its pace, only coarser. A section with no budget runs every tick.

## Network use

*Added 2026-10-15.*

What the server sends each player is counted as it is sent, at the bytes the game writes each packet as (before the
connection compresses them): by kind, a mod's payloads by their id and the game's packets by theirs; by mod; and by
the block entity whose updates took it.

`/jstech bandwidth` shows the last minute for everyone, `/jstech bandwidth <player>` for one player:

```
What the server sent everyone over the last minute: 18.4KB/s
▁▁▂▂▃▅▇█▆▄▃▂▂▁▁▁▂▂▃▃▂▂▁▁▁▁▂▂▁▁  the tallest bar is 61.2KB/s
By mod: minecraft 15.1KB/s, jsc 3.1KB/s, jscore 210B/s
What took the most:
minecraft:level_chunk_with_light: 9.8KB/s
...
The blocks whose updates took the most:
Mainframe at 12, 64, -3 in minecraft:overworld: 1.9KB/s (Alex's)
```

Each bar of the graph is two seconds. A player playing in their own game is not counted, as nothing they are sent is
written as bytes; whoever joins it over the network is. `diagnostics.count_network` turns the counting off.

`Bandwidth.everyone()` and `Bandwidth.of(player)` hand a mod the same numbers.

## What weighs

*Added 2026-10-15.*

`/jstech cost [seconds]` measures, for 10 seconds unless it says otherwise, what each of the series' blocks costs the
server, and then lists the ten worst places with what stands there and whose it is, and the cost of each kind of block
across its places. Between measures nothing is measured, so it costs nothing while nobody looks.

Every block of the series whose ticker comes from `BlockEntityTickers.create` is measured. A kit with work of its own
in one place, such as a structure, adds it while a measure runs:

```java
if (CostMap.measuring()) {
    CostMap.add(level, controllerPos, rl("crane"), nanos);
}
```

The last measure stays, `CostMap.last()`, for the map of costs drawn in the world.

## `/jstech health`

*Added 2026-10-15.*

The state of the series' systems, each under its system's name, with what is wrong as warnings. The Core's part: how
long the server's ticks take, the sections of work and the costliest, the ones slowed or stalled, what is sent over
the network, the work away from the tick, each dimension's data networks, wires and grids, the chunks each owner keeps
loaded, the protections asked, the audit log and the robots of the players on.

Each kit adds its part from its mod's constructor:

```java
Health.provide(LOG, (server, report) -> {
    report.line(TEXTS.SPREADING.with(fallout.size(server)));
    if (fallout.behind(server)) {
        report.warn(TEXTS.BEHIND);
    }
});
```

A part is read on the server's thread. A part that fails is shown as a warning saying so, and the rest are read.

## The diagnostic file and crash reports

*Added 2026-10-15.*

`/jstech diagnostics` writes `jstech-diagnostics/diagnostics-<date>-<time>.txt` beside the game, for a player to
attach to a bug report: the versions of the game, NeoForge and Java, the memory, every mod and its version, every
settings file as it holds its values, the timings, the sections, the health, the network use and the log's levels.

A crash report has a section, "J's Core", with what each thread was running as a section when it crashed, the
sections slowed or stalled, and, when the crash is on the server's thread, the state of the systems.

## What can go wrong

- **A section is declared twice.** The game stops loading, naming the section: declare it once, as a constant.
- **Spark shows the Core's work without names.** The frames could not be made (the log says so once per section); the
  work still runs, timed and watched.
- **`/jstech bandwidth <player>` says the player is not counted.** They play in their own game, which sends nothing as
  bytes, or the counting is off in the world's settings.
- **The operators are not told of a stall.** `diagnostics.warn_operators` is off, they do not hold
  `jscore.diagnostics.warnings`, or they silenced the kind; the log has it either way.
