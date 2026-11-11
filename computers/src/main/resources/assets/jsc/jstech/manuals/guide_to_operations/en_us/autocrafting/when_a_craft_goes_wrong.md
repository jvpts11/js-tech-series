---
section: autocrafting
order: 151
---
# When a craft goes wrong

A craft can fail before it starts, or while it runs. The network names the step that went wrong, and the messages below tell you what each one means and how to fix it.

Table: Before a craft starts

| What you see | What it means | The fix |
| --- | --- | --- |
| No craft starts. | A craft starts only while the Mainframe is running and has an operating system. | Switch the Mainframe on, and install its operating system. |
| The text "nothing on the network knows how to make", then the item. | No pattern on the network makes the item. | Add a bench, machine or multi-stage pattern for it ([](jsc:patterns)). |
| The text "no crafting computer was free to make", then the item. | No Crafting Computer is switched on with a Crafting Card, or all of them are busy. | Switch a Crafting Computer on, give it a Crafting Card, or wait until one is free. |
| The recipe does not appear in the plan. | The recipe is in the ROM of a Crafting Computer that is not running. | Switch that Crafting Computer on. |

## While a craft runs

Table: While a craft runs

| What you see | What it means | The fix |
| --- | --- | --- |
| The text "the machine making", then the item, and "stopped taking anything". | A machine job gave up after its timeout, while nothing moved on its interface. | Check the machine's power and its output. A Crafting Receiving Bus must face the output. |
| The text "no Crafting Interface holds the recipe for", then the item. | No interface holds the machine's pattern, and it was not placed in time. | Put the machine's pattern into a Crafting Interface that a Crafting Computer drives. |
| The text "the ingredients for", then the item, and "ran out". | The bench craft ran out of ingredients in storage. | Stock more of that ingredient. |
| The text "the pattern has no stages in it". | A multi-stage pattern holds no stages. | Add stages to the pipeline ([](jsc:patterns)). |
| The text "stage", then a number, "of", a count, and "could not be started". | The machine or the interface of that stage is not ready. | Check that stage's machine and its interface, as for a machine that stopped. |
| The text "stage", then a number, "of", a count, and "failed". | The stage timed out. | Check the stage's machine for power and room, and that its output faces a Crafting Receiving Bus. |

## How a craft ends

A craft that makes part of what it asked for is partial, and the part is kept. A craft that makes nothing has failed. If one stage fails, the whole craft fails: bench stages give back the items they took, and machine stages keep what they fed into their machines.

The eight states of an operation are listed in [](jsc:operation_states).

Table: How a craft ends

| The result | What it means | The fix |
| --- | --- | --- |
| The text "delivered", with two amounts. | A partial craft: some of the amount was made. | Ask for the rest once the missing items are in storage. |
| The text "timed out waiting on a locked resource". | Another operation held what this craft needed. | Ask again once that other operation has settled. |
| The text "failed". | The craft made nothing. | Read the step that failed above, and fix it first. |

## Timeouts

A machine job waits for its outputs, and its clock counts only while nothing moves on its interface. When the timeout passes, the job gives up. A machine pattern's timeout is 200 ticks by default, which is 10 seconds ([](jsc:patterns)).

Outputs that come late are still credited for a short while after a job ends. After that, they go to the network as unexpected.

An interface waits until its owed outputs are in before a new recipe goes in. Its log shows draining until then ([](jsc:crafting_interfaces)).

## The interfaces and the cables

Table: Interfaces and cables

| What you see | What it means | The fix |
| --- | --- | --- |
| The screen says "Driven by no Crafting Computer". | The interface is not on a cable that a Crafting Computer drives, or the cards drive fewer interfaces than the cable reaches. | Put it on a cable that a Crafting Computer drives, switch the computer on, or give the computer another card. |
| The screen says "no router on its cable takes it: this pattern cannot run". | No router on the interface's cable takes one of the inputs. | Put a Crafting Input Router on that cable, with a filter that takes the input ([](jsc:input_routers_and_receiving_buses)). |

The other messages of the routers and the buses are listed in [](jsc:input_routers_and_receiving_buses).

## Planner limits

The planner stops at 16 levels of recipes, and it cuts a recipe that depends on itself ([](jsc:craft_planner)).

## If something goes wrong

> [!PROBLEM] A craft does not start at all.
> Check that the Mainframe is running with an operating system, and that a Crafting Computer is switched on with a card.

> [!PROBLEM] A craft makes less than you asked.
> The craft settled as partial. Ask for the rest once the missing items are in storage.

> [!SEE] jsc:asking_for_a_craft, jsc:crafting_interfaces, jsc:input_routers_and_receiving_buses, jsc:craft_planner, jsc:operation_states, jsc:network_manager
