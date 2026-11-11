---
section: autocrafting
order: 142
---
# Patterns

A pattern is a recipe the network can use, and there are three kinds. Each kind fits a different job, and you write each one in its own tab of the Pattern Studio ([](jsc:pattern_studio)).

## Bench patterns

A bench pattern is a 3x3 grid of nine cells and a result. A cell that the recipe leaves empty stays empty. You can give the pattern a name of up to 64 characters and a note of up to 256 characters, or leave both out: an unnamed pattern goes by the name of its result.

A cell can accept an item tag, such as any kind of planks, instead of one exact item. When a craft is planned, the network uses whatever of that tag it holds in stock. The hint "Right-click a cell: the tag it accepts" tells you what a cell takes.

A bench pattern holds items only. You put fluids and chemicals into machine patterns.

## Machine patterns

A machine pattern says what goes into a machine and what comes out. You write its inputs and its outputs, and each one has an amount per run: an input of 1 means one of that item each time the machine runs.

Table: Parts of a machine pattern

| Part | What it holds |
| --- | --- |
| Input | An item, a fluid or a chemical, with the amount it takes per run (at least 1). |
| Output | An item, a fluid or a chemical, with the amount it gives per run and a chance in percent. |
| Timeout | How long the craft waits while nothing moves on the interface, in ticks (the default is 200, which is 10 seconds). |

The chance of an output is between 1 and 100 percent. An output of 100 always comes out. A smaller chance makes a byproduct that may or may not come out. The network counts a byproduct at its expected amount when it plans the craft, but a byproduct never blocks a craft: the real amount is counted when the machine runs.

Anything else that comes out of the machine is an unexpected output. It goes to the network, and it never fails a craft.

An input can be marked as estimated when its amount came from a meter that measures per tick, and not from the author of the pattern. The Pattern Studio then shows "per run (estimated)" after the amount, and the recipe still runs.

A machine pattern works only from a Crafting Interface that holds it. The interface is described in [](jsc:crafting_interfaces).

## Multi-stage patterns

A multi-stage pattern is a pipeline: an ordered list of stages, and each stage is exactly one bench pattern or one machine pattern. Stage 1 takes its inputs from the network. Each later stage takes the output of the stage before it, and it starts only when that stage has made its result.

> [!DEFINE] pipeline
> A multi-stage pattern: an ordered list of stages that the network runs in turn.

Fluids work only inside machine stages. A pipeline cannot hold another pipeline, and it holds up to 16 stages.

The network works out how much each stage must make, starting from the last stage and going back. For example, a stage that makes one iron block needs nine ingots from the stage before it, and a stage that makes one ingot needs nine nuggets from the stage before it.

## If something goes wrong

> [!PROBLEM] The pipeline has no stages.
> Add a bench or a machine pattern as a stage.

> [!PROBLEM] The screen says "A pipeline cannot hold another pipeline".
> Put the stages of the inner pipeline in directly, one bench or machine pattern each.

> [!PROBLEM] A pipeline with 16 stages refuses another.
> Remove a stage before you add one: a pipeline holds 16 stages at most.

> [!SEE] jsc:pattern_studio, jsc:pattern_encoders, jsc:crafting_interfaces, jsc:fluids, jsc:when_a_craft_goes_wrong
