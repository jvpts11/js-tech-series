---
section: engine_choice
order: 386
icon: jsc:mainframe
---
# NextgreIQL and EXPLAIN

NextgreIQL is the engine that shows you how it plans. Before any work is done, you can ask for the plan of a CRAFT or a SELECT, and read which steps it would take and where each step runs.

> [!DEFINE] Query plan
> The steps the engine chose to answer a statement, with where each step runs and what it costs in ticks.

A craft is made in steps, and each step runs at a bench or on a machine. A bench is a Crafting Computer, where hand-made recipes run. A machine is a processing machine, and it runs its own recipes.

> [!DEFINE] Bench
> A Crafting Computer, where hand-made crafting recipes run.

> [!DEFINE] Machine
> A processing machine, which runs its own recipes.

You can also change a plan with hints ([](jsc:nextgre_hints)), which are words written after a CRAFT.

> [!DEFINE] Hint
> A setting written after a CRAFT that changes the plan it makes, such as PREFER MACHINE.

Table: NextgreIQL versions by age

| Age of the Mainframe | NextgreIQL version |
| --- | --- |
| Legacy | 7.0 |
| Transition | 8.3 |
| Standard | 9.0 |
| Advanced | 16 |

## Showing a plan with EXPLAIN

EXPLAIN shows the plan without starting it. Write EXPLAIN in front of a CRAFT or a SELECT, and the engine answers with the plan instead of doing the work. Only NextgreIQL offers EXPLAIN; the Midsoft refuses the word with a syntax message.

1. Type EXPLAIN CRAFT, the quantity and the item name.
2. Press [[Enter]].

```iql explain_craft.iql
EXPLAIN CRAFT 64 iron_ingot
```

The answer is a table with one row for each step of the plan. Its columns are these.

Table: The columns of an explained plan

| Column | What it shows |
| --- | --- |
| step | The step's label and its quantity, as the steps table below shows. |
| where | Whether a bench or a machine runs the step, and which one. |
| estimate | The cost in ticks, worked out before anything runs. |
| actual | Blank until the plan runs. Then it shows the time the step took. |
| hints | The hints that shaped the step. |

Table: The steps of a plan

| Step label | What it means |
| --- | --- |
| `Craft <item> x<n>` | Makes the item, in the quantity shown. |
| `Process <item> x<n>` | Processes the item in a machine. |
| `Pull <item> x<n>` | Takes items from one storage server. There is one row for each server. |
| `Index Seek <item> x<n>` | One read for the whole quantity, from the servers named after from. |
| `Missing <item> x<n>` | The network holds none of it and makes none of it. |

Steps that run side by side are grouped. The group says how many steps it has, and how many of them run at once.

## Running a plan with EXPLAIN ANALYZE

EXPLAIN ANALYZE CRAFT does the craft as well. The engine starts it, and the plan fills in the actual time of each step as the step runs.

The answer names the plan the engine chose, how many plans it weighed, and the first eight characters of the Operation. The Nextgre Planner Studio shows the times as they come ([](jsc:nextgre_planner_studio)).

## Where the items come from

EXPLAIN SELECT shows where the items would come from, without moving anything. The answer gives the quantity, the item, how many servers would hand it over, and the cost in ticks.

## ANALYZE on its own

A plain ANALYZE, with no statement after it, does the maintenance of the network's index ([](jsc:operations_catalogue)). NextgreIQL then gathers statistics for the Studio, and the answer gives the item types, servers and recipes it measured.

The Studio shows these statistics: item types indexed, servers, recipes timed, crafts measured, and the time of the last ANALYZE.

## How the engine chooses a plan

The engine does not guess a plan. It gives every candidate plan a cost in ticks, one for each way it found to make the item, and it chooses the cheapest.

The Studio's Rules tab lists the rules that shape the weighing: Weigh machines against benches, Weigh every recipe of the result, Use measured times, Count steps that run at once, and Weigh the fastest servers. You can switch these rules in the Studio.

Every plan is kept on the Mainframe, and the Studio's History tab lists them. Other mods can add rules, hints and statistics to NextgreIQL.

## If something goes wrong

> [!PROBLEM] The answer is "EXPLAIN shows the plans of CRAFT and SELECT".
> Put EXPLAIN only before a CRAFT or a SELECT.

> [!PROBLEM] The answer is "EXPLAIN needs a statement to explain".
> Write the CRAFT or the SELECT after EXPLAIN.

> [!PROBLEM] The answer is "no item is called" and the name you typed.
> Check the spelling, and use the short name, such as iron_ingot.

> [!PROBLEM] The answer says that nothing on the network knows how to make the item.
> No recipe makes it. Choose another item.

> [!PROBLEM] The answer says every plan for the item was set aside.
> The planner's rules rejected every plan. Look at the Rules tab of the Nextgre Planner Studio.

> [!PROBLEM] The answer says the network could not start the craft.
> Check the network's storage and machines.

> [!SEE] jsc:nextgre_hints, jsc:nextgre_planner_studio
