---
section: iql_verbs
order: 373
---
# RUN: other mods' machines

Machines of other mods can join the network, and some of them do work that the network asks for. RUN asks one of them for a kind of work that its mod adds. Write RUN, then the kind's id: the mod's name, a colon and the kind's name.

> [!DEFINE] kind of Operation
> A kind of work that the network can ask for. Other mods add kinds of their own, and only their machines do them.

```iql beam.iql
RUN mymod:accel_run ON 'Cyclotron-3fa2c1' WITH beam = 'proton', energy = 20 PRIORITY HIGH
```

Table: The parts of RUN

| Part | What it does |
| --- | --- |
| `ON 'name'` | Names the machine that does the work. Without ON, the network gives the work to the machine that has the least of it already. |
| `WITH field = value` | Gives the fields that the kind reads. A comma goes between two fields. Words go in quotes; numbers and true or false do not. |
| `PRIORITY HIGH` | Sets the work's place in the queue, as for any other statement. |

## Naming the machine

A machine answers to the name that you gave it on its own screen. A machine with no name of its own answers to the name of its block, a dash and six letters and digits, such as Cyclotron-3fa2c1. Capitals do not matter.

You can also name a machine by where it stands, as the listing shows. The at column of the machine's table shows that position ([](jsc:query_and_select)).

```iql by_place.iql
RUN mymod:accel_run ON '12, 64, -3'
```

## What happens next

The work waits in a queue of the Mainframe, as other work does. When its turn comes, the Mainframe gives it to the machine and frees the queue at once. The machine then works for as long as the work takes, and the Mainframe goes on with other work.

Until the machine is done, the work is on the Processes tab of the Network Manager. You can stop it there, and the machine then stops. When it is done, it goes into the log under RUN, with its kind, the machine and what you asked.

A job can ask a machine for work too, for example when a world event comes:

```iql shield.iql
CREATE JOB shield AS RUN mymod:shield_up WHEN EVENT mymod:solar_storm COMING
```

Table: The replies of RUN

| Reply | What it means |
| --- | --- |
| `<kind> queued for <machine>` | The work waits for its turn, and then goes to that machine. |
| `no machine on the network carries out <kind>` | No machine on the network does that kind of work. |
| `no machine named <name> is on the network` | No machine on the network has that name or stands there. |

## If something goes wrong

> [!PROBLEM] RUN needs the id of a kind of Operation, as mod:kind
> Type the kind's id after RUN, with the mod's name and a colon before the kind's name.

> [!PROBLEM] no mod registered a kind of Operation <id>
> Check the id. The mod that adds the kind must be installed.

> [!PROBLEM] <machine> does not carry out <kind>
> Name a machine that does that kind of work, or leave ON out.

> [!PROBLEM] <kind> has no field <field>; its fields are <fields>
> Use one of the fields that the reply lists.

> [!PROBLEM] the fields do not serve <kind>: <reason>
> Give each field a value of the right sort, such as a number where the kind reads a number.

> [!PROBLEM] <kind> is asked for from its machine's own screen, never in the network's language
> Ask for that work on the machine's own screen.

> [!SEE] jsc:iql_jobs, jsc:query_and_select, jsc:clauses_priority_if_with
