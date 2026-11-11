---
section: credits_and_costs
order: 480
---
# How much a machine runs each tick

The central processing units (CPU) of a computer give its programs their work. Each tick, the computer turns its processors into credits: the number of instructions its programs may spend in that tick. More cores and a faster clock mean more credits.

The computer adds up, for every processor in its build, the number of cores times the clock speed in megahertz (MHz). Cores are the parts of a processor that work at the same time. See [Processors](jsc:processors). If your programs run slowly, check the processors first.

Then it divides that sum by eight, dropping any fraction. A computer with a processor always gets at least 32 credits. A computer with no processor in its build gets no credits, so its programs run nothing. There is no upper limit: a faster processor gets more credits.

Table: Credits for some processors

| Cores times MHz, summed | Credits in one tick |
| --- | --- |
| 0, with no processor in the build | 0 |
| 128 (1 core at 128 MHz) | 32, the least a computer gets |
| 256 (2 cores at 128 MHz) | 32 |
| 2,048 (4 cores at 512 MHz) | 256 |
| 12,000 (6 cores at 2,000 MHz) | 1,500 |

A processor's tooltip also shows a capacity in items a tick (it/t). That number measures the network's work, not the speed of a program. A program's speed is the credits above. See [Processors](jsc:processors).

> [!DEFINE] Credit
> The number of instructions a computer's processors give its programs in one tick.

## Each tick

On every tick the computer checks whether it runs. A computer that is switched on, with a valid build, runs its programs. A computer that is switched off, or whose parts no longer make a valid build, stops all of them. Each one is given its goodbye, within a budget.

## Turns within a tick

The credits of a tick are shared among the programs in rounds. In each round, a program takes up to 64 credits, or an even share of what is left when that is less. A program that finishes or waits for something gives up its place for the rest of the tick.

Every program has a priority. Only the low priority changes anything: a low program sits out every other round. Give the priority as the third argument of Program.Start, after the file and the arguments.

Table: Priorities

| Priority | What it does |
| --- | --- |
| `low` | Sits out every other round of the tick. |
| `medium` | The default, which runs in every round. |
| `high` | Runs in every round, as medium does. |

## Work for other computers

A computer that does work for a [ComputerCraft](jsc:computercraft_side) computer, through a [Network Gateway](jsc:network_gateway), pays for that work out of its own credits. Its programs get fewer credits in the next ticks until the debt is paid.

A debt larger than one tick's credits carries over to the tick after. A computer with no programs still pays its debts, one tick at a time.

## The goodbye budget

A stopped program runs its goodbye out of a budget of 4,096 instructions per tick. All the programs stopped in that tick share the budget. A program that runs past its share stops before it can run its farewell code.

When a computer is switched off, or its build is no longer valid, the budget is split evenly among the programs that are stopped.

## If something goes wrong

> [!PROBLEM] The programs run nothing at all.
> The computer has no processor in its build, or its build is not valid. Put a processor in its build and make the build valid.

> [!PROBLEM] A program is slow on an old computer.
> Its processors have few cores or a slow clock, so it gets fewer credits. Install processors with more cores or a faster clock ([Processors](jsc:processors)).

> [!PROBLEM] Some programs stop without their goodbye.
> Many programs stopped in the same tick share the budget. Stop fewer programs at a time.

> [!SEE] jsc:call_costs, jsc:saved_with_world
