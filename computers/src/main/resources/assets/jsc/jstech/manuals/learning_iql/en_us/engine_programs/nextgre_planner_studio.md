---
section: engine_programs
order: 398
---
# Nextgre Planner Studio

The Nextgre Planner Studio shows how NextgreIQL plans a craft, and how long each step of the plan takes. Use it when a craft takes longer than you expect, or when you want to tune the planner.

The studio needs the NextgreIQL engine, version 7.0, running on the Mainframe. The engine is explained in [the NextgreIQL engine](jsc:nextgre_iql).

> [!DEFINE] Planner
> The part of NextgreIQL that picks how a craft is made, by weighing the recipes it knows.

> [!DEFINE] Lane
> One machine that can run at the same time as other machines.

> [!DEFINE] Bench
> A recipe made by hand at a crafting bench, as opposed to one that runs in a machine.

Table: The four tabs

| Tab | What it shows |
| --- | --- |
| Explain | The plan of a statement, box by box. |
| Statistics | What the planner has measured on this network. |
| Rules | The planner's rules, which you can switch on and off. |
| History | Every plan the planner made lately. |

## Explain

Type a CRAFT or a SELECT, then press [[F7]] for Explain, or [[Shift]]+[[F7]] for Explain Analyze. Explain plans the statement without running it. Explain Analyze runs it too, and the boxes fill in as the craft goes.

The plan is a tree of boxes. Each box is one step, with its name and its time. Before the craft runs, a box reads "est <n> s", the estimate in seconds (20 ticks make one second). After Explain Analyze, it reads "est <n> s, actual <n> s".

Table: The text on a box

| Text | What the step does |
| --- | --- |
| `Craft <item> x<quantity>` | Makes the item from a recipe. |
| `Process <item> x<quantity>` | Makes the item in a machine. |
| `Pull <item> x<quantity>` | Takes the raw items from storage. |
| `Missing <item> x<quantity>` | Nothing on the network holds the item. |
| `Index Seek <item> x<quantity>` | Reads the item through the index. |

Under the name, a box says where its step runs: "at a bench, <item> runs" or "on a machine, <item> runs". It also says "from <server>" for the server that gives the items, and "<n> steps, <n> at once" when steps run side by side. The hints of the statement are marked on the boxes they changed.

The header shows "Planning <n> ms" and "Execution <n>". Beside the tree, PLANS WEIGHED lists each plan the planner weighed, with its cost in ticks. The chosen plan reads "chosen". A plan that was set aside reads "set aside by", and the reason.

A partial plan reads "makes <n> of the <n> asked for". A plan that cannot be made reads "cannot be made: <n> <item> missing".

## Statistics

The Statistics tab shows what the planner has measured on this network. Press Gather Statistics to measure again. The reply reads "statistics gathered: <n> item types on <n> servers, <n> recipes timed".

Table: The statistics

| Line | What it counts |
| --- | --- |
| Item types indexed | The item types the index knows. |
| Servers | The storage servers the planner knows. |
| Recipes timed | The recipes that have a measured time. |
| Crafts measured | The crafts the planner has measured. |
| Last ANALYZE | "never", or how many minutes ago ANALYZE last ran. |
| <recipe> at a bench, <recipe> on a machine | The measured time of one recipe. |

## Rules

A rule is a choice the planner makes when it weighs plans. You can switch each rule on or off for this Mainframe. Rules that other mods add are listed under RULES FROM OTHER MODS.

Table: The planner's own rules

| Rule | What it does |
| --- | --- |
| Weigh machines against benches | Also plans the craft with machine recipes before bench recipes, and keeps the cheaper. |
| Weigh every recipe of the result | Plans the craft once with each bench recipe that makes what was asked for, and keeps the cheapest. |
| Use measured times | Reckons each recipe at the time it was measured to take on this network, rather than at a guess. |
| Count steps that run at once | Reckons steps that do not wait on each other as running side by side. |
| Weigh the fastest servers | Also plans taking the raw materials from the servers that hand them over fastest. |

## Hints

A hint goes after a CRAFT and changes its plan. The built-in hints are these. The hints are explained in full in [the NextgreIQL hints](jsc:nextgre_hints).

Table: The built-in hints

| Hint | Takes a value | What it does |
| --- | --- | --- |
| `PREFER SOURCE <server>` | Yes, a server name | Takes the raw materials from the named server first. |
| `AVOID SOURCE <server>` | Yes, a server name | Takes nothing from the named server while another one has it. |
| `MAX PARALLEL <n>` | Yes, a whole number above 0 | Runs at most that many stages of the craft at once. |
| `PREFER MACHINE` | No | Makes on a machine whatever both a machine and a bench make. |
| `PREFER BENCH` | No | Makes at a bench whatever both a machine and a bench make. |

## History

Every plan NextgreIQL made lately is kept on the Mainframe. Click a plan to show it on the Explain tab. The State column reads "planned", "running", "done" or "fell short". Until a craft is planned, the History tab says "No plans yet. Every craft NextgreIQL plans is kept here."

## If something goes wrong

> [!PROBLEM] The program refuses to open and says that no compatible engine was found on this network.
> Run NextgreIQL, version 7.0, on the Mainframe ([](jsc:engine_services)).

> [!PROBLEM] The studio says "EXPLAIN needs a statement to explain".
> Write a CRAFT or a SELECT after EXPLAIN.

> [!PROBLEM] The studio says "EXPLAIN shows the plans of CRAFT and SELECT".
> Explain a CRAFT or a SELECT. Other statements have no plan to show.

> [!PROBLEM] The studio says "hints change the plan of a CRAFT".
> Put the hint after a CRAFT, not after another statement.

> [!PROBLEM] The studio says "MAX PARALLEL takes a whole number above nought".
> Write a whole number above 0 after MAX PARALLEL.

> [!PROBLEM] The studio says "no server on the network is called <name>".
> Check the server's name in the Object Explorer ([](jsc:studio_explorer)).

> [!PROBLEM] The studio says "no item is called <name>".
> Check the spelling of the item's name.

> [!PROBLEM] The studio says "nothing on the network knows how to make <name>".
> Choose an item that a recipe on the network makes.

> [!PROBLEM] The studio says "every plan for <name> was set aside".
> Look at PLANS WEIGHED to see why each plan was set aside, and at the Rules tab.

> [!PROBLEM] The studio says "the network could not start the craft of <name>".
> Look at the Operations in the Activity Monitor ([](jsc:studio_activity)).

> [!PROBLEM] A plan reads "cannot be made: <n> <item> missing".
> The network does not hold enough of that item. Bring the missing items, then explain again.

> [!SEE] jsc:nextgre_iql, jsc:nextgre_hints, jsc:craft_planner
