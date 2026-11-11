---
section: engine_choice
order: 387
icon: jsc:mainframe
---
# Hints: changing NextgreIQL's plan

A hint changes how NextgreIQL plans a CRAFT. When you add one, you choose where the raw materials come from, how many steps run at once, and whether a step is made on a machine or at a bench.

Write the hints after the CRAFT, one after another. The engine reads them at the end of the statement.

> [!DEFINE] Source
> A storage server that holds raw materials for a craft.

Table: The hints

| Hint | Takes | What it does |
| --- | --- | --- |
| `PREFER SOURCE 'server'` | The name of a server | Takes the raw materials from the named server first. |
| `AVOID SOURCE 'server'` | The name of a server | Takes nothing from the named server while another server has them. |
| `MAX PARALLEL n` | A whole number | Runs at most that many steps of the craft at once. |
| `PREFER MACHINE` | Nothing | Makes on a machine whatever a machine and a bench both make. |
| `PREFER BENCH` | Nothing | Makes at a bench whatever a machine and a bench both make. |

A craft with two hints looks like this. The server named Storage B must be on the network.

```iql hints_example.iql
CRAFT 64 iron_ingot PREFER SOURCE 'Storage B' MAX PARALLEL 2
```

Capitals do not matter.

MAX PARALLEL takes a whole number above zero. Hints go after a CRAFT only, because the other statements refuse them.

Other mods can add hints of their own.

## If something goes wrong

> [!PROBLEM] The answer is "no server on the network is called" and the name you typed.
> Write the server's name as the network shows it.

> [!PROBLEM] The answer is "MAX PARALLEL takes a whole number above nought".
> Write a whole number of 1 or more after MAX PARALLEL.

> [!PROBLEM] The answer is "hints change the plan of a CRAFT".
> Remove the hint, or make the statement a CRAFT.

> [!SEE] jsc:nextgre_iql, jsc:engine_problems
