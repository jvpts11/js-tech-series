# Autocrafting

How the network makes things for you: you ask for 512 pistons and it works out every step from what it has, runs
the crafting table recipes and the machines, and puts the pistons in storage. This page explains the parts, how a
recipe gets into the network, and what can go wrong.

## The words

- A **pattern** is a recipe the network knows: what goes in and what comes out. There are three kinds:
  - a **crafting table pattern**: a 3x3 grid and its result (a cell can take any item of a tag, "any planks");
  - a **machine pattern**: what to put in a machine and what it gives back, with a time limit and, for an output
    that only sometimes comes, its chance;
  - a **many-step pattern**: steps of both kinds in order, each starting when the one before has made its result.
- A **Crafting Computer** is the computer that crafts. Its **Crafting Card** decides how much it can drive, and
  keeps its crafting table patterns in the card's own memory, so they move with the card.
- A **Crafting Interface** sits on the crafting cable against a machine, holds that machine's patterns and feeds it.

## The parts

*Added 2026-06-05.*

| Part | What it does |
| --- | --- |
| Crafting Computer | Runs the crafts. Needs a Crafting Card, and the Crafting Manager program. |
| Crafting Card | Drives that many Crafting Interfaces and keeps that many crafting table patterns: 2 (Vintage), 4 (Legacy), 5 (Transition), 6 (Standard), 8 (Advanced). |
| Pattern Studio | The program where you write patterns. |
| Pattern Encoder | A device joined to a computer by Peripheral Cable that burns patterns onto media: floppies in the Vintage, CDs in the Legacy, DVDs, CDs or USB sticks from the Standard. |
| Crafting Cable | The cable the crafting parts sit on, one for every era. |
| Crafting Interface | Holds a machine's patterns (3 to 12 by its era) and feeds that machine. |
| Crafting Input Router | Feeds one input face of a machine with several inputs. |
| Crafting Receiving Bus | Takes a machine's output and credits it to the craft that fed it. |

## Getting a recipe into the network

*Added 2026-06-05.*

1. Open the **Pattern Studio** on a computer. With JEI installed, you can carry a recipe straight from JEI into
   it, and drag items from JEI's list into its grid.
2. Write the pattern: the grid for a crafting table pattern, or the inputs, the machine and the outputs for a
   machine pattern.
3. Put a blank medium in a **Pattern Encoder** joined to the computer, and burn the pattern onto it.
4. On the **Crafting Computer**, open the **Crafting Manager** and load the pattern from the medium: a crafting
   table pattern into the card's memory, a machine pattern into the Crafting Interface of its machine.

## Building a machine line

*Added 2026-06-05.*

1. Lay **Crafting Cable** from the Crafting Computer to the machine.
2. Put a **Crafting Interface** on the cable against the machine. It feeds the machine directly when it sits
   against it, or through a crafting cable of its own, dyed apart from the main one.
3. For a machine that takes inputs on more than one face, put a **Crafting Input Router** against each input
   face, on the interface's own cable.
4. Put a **Crafting Receiving Bus** against the machine's output.
5. Give the machine power, the way its own mod says.

Any machine that offers the usual item, fluid, energy and chemical access works, from any mod: J's Industrial's,
Mekanism's, and others.

## Asking for a craft

*Added 2026-06-11.*

Ask for an item the way you ask for anything: in the Network Interactor, or in IQL, `CRAFT 512 piston`. The
network plans it from the bottom up: what it has, what it must make first, in which order, on which machines, and
runs the steps on as many machines as it has, at the same time where they do not depend on each other. The
**Craft Planner** shows the plan before you start it (what it needs, what is missing, what it costs), and the
**Crafting Manager** shows the jobs while they run.

A machine's interface works one recipe at a time or several jobs at once, and can be paused and given a limit.

## What can go wrong

- **"Missing" in the plan.** Some item has no pattern and none in storage. Add its pattern, or the item.
- **A many-step craft fails at one step.** That step ran out of time: its machine has no power, is full, or never
  gave its output to the Receiving Bus. The craft says which step. What the crafting table steps took is given
  back; what a machine step took stays in the machine.
- **A machine is never fed.** Its Crafting Interface is not on a cable a Crafting Computer drives, or the
  computer is off, or its card drives fewer interfaces than the cable reaches (the first ones on the cable get
  driven).
- **The output never comes back.** No Crafting Receiving Bus faces the machine's output.
- **A crafting table pattern disappears with the computer.** It lives on the Crafting Card. Move the card and the
  pattern goes with it.
