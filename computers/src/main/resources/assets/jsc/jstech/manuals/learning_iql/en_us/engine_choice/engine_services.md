---
section: engine_choice
order: 384
icon: jsc:mainframe
---
# Starting, stopping and replacing an engine

You start, stop and replace engines in the Services tab of the Network Manager. The tab's header card shows the state of the engine in one word: Running, Stopped or Replacing. The buttons under it depend on that word.

## The buttons

Table: The buttons of the Services tab

| State | Buttons | What you can press |
| --- | --- | --- |
| Running | Configure..., Stop, Replace... | Stop and Replace are available; Configure... is always greyed out. |
| Replacing | Configure..., Stop, Replace... | Nothing: all three are greyed out. |
| Stopped, or none chosen | Start... | Start..., when any engine is installed. |

A button does what it says or nothing at all. If the server refuses a press, nothing happens and the tab shows the true state again.

## Stopping an engine

Stop asks nothing. It stops the active engine, which stays installed and chosen. Until you start an engine again, the network has no engine running.

The tab then says "No Network Operations Engine is running". Storage stays, and the Operations in flight carry on. Transfers still work, but crafts and IQL queries are refused until an engine runs.

An engine runs only while its Mainframe is on. When the Mainframe is turned off, the engine stops with it.

## Starting an engine

1. Press Start... in the Services tab.
2. Choose the engine in the list titled "Start a Network Operations Engine".
3. Press Start to confirm.

The list starts with the first stopped engine selected. Its note says "The engine starts at once and plans every new request."

If you choose an installed engine that is not the active one, the network switches to it at once. The timed steps of a replacement do not run then.

## Replacing an engine

A replacement switches the network from the running engine to another installed one. It takes time, and the tab shows Replacing until it ends. The new engine starts at the end.

> [!DEFINE] Replacement
> The timed switch from one installed engine to another, during which no engine plans.

1. Press Replace... in the Services tab.
2. Choose the engine in the list titled "Replace the Network Operations Engine".
3. Read what the dialog says, then press Replace to confirm.

In the dialog, the Replace button is greyed out when the engine you chose is the one already active.

Table: What the Replace dialog says

| Line of the dialog | What it tells you |
| --- | --- |
| New requests go to <new engine>. | Every request from now on is planned by the new engine. |
| The <number> Operations in flight finish on the plans they started with. | Work already running is not planned again. |
| Scripts written in <old engine>'s own statements may stop working; the IQL core works on every engine. | Statements that use the old engine's extras may fail, but core IQL still works. |
| <Subframe> will take no work until it runs <new engine> too. | A Subframe takes work only when it runs the same engine as the Mainframe. |
| No other engine is installed on this Mainframe. Install one from its disc to replace this one. | There is nothing to switch to yet. |

> [!DEFINE] Subframe
> A second computer of the network that takes work, when it runs the same engine as the Mainframe.

## How long a replacement takes

A replacement takes 40 ticks, plus one tick for every four item types the network stores, up to 2,400 ticks. A tick is the game's basic time step: 20 ticks make a second.

> [!DEFINE] Tick
> One twentieth of a second in the game.

> [!DEFINE] Item type
> One kind of item the network stores, such as an iron ingot.

Table: How long a replacement takes

| Item types stored | Time |
| --- | --- |
| 0 | 40 ticks, 2 seconds |
| 400 | 140 ticks, 7 seconds |
| 9,440 or more | 2,400 ticks, 2 minutes |

The progress window moves through the steps of the replacement in turn. N in the Keep step is the number of Operations still running.

Table: The steps of a replacement

| Step on the progress window | Share of the time |
| --- | --- |
| Stop taking new plans | 2% |
| Keep N Operations in flight on their plans | 2% |
| Stop <old engine>, or Nothing to stop when the network had none | 2% |
| Start <new engine> | 2% |
| Discover storage and machines | 22% |
| Build indexes | 60% |
| Publish capabilities | 10% |

Building indexes takes the most time, because the network indexes each item type it stores. The window then says Ready.

While a replacement runs, no engine plans. The Operations already in flight carry on, but a new request that needs planning is refused until the new engine is ready. When the time ends, the new engine becomes the active one and starts.

## Installing an engine

An engine comes from its disc, like every program. You put the disc in a drive linked to the Mainframe, then install the program as you install any other ([](jsc:programs)).

The install puts the engine in the Mainframe's list, at the version for the Mainframe's age. If no engine is chosen yet, the new one becomes the active engine and starts at once.

An install is refused when the engine is already installed, or when it was not made for the Mainframe's age.

> [!WARNING] Formatting the disk of the Mainframe removes every engine installed on it.

Removing the engine that runs leaves the network with no engine, as a stop does.

## The iqlengine command

The Midsoft IQL Server also has a command of its own, typed at a prompt: iqlengine. It takes one of four verbs.

Table: The iqlengine verbs

| Verb | What it does |
| --- | --- |
| `install` | Puts the Midsoft IQL Server on the Mainframe. |
| `start` | Starts the Midsoft IQL Server, and switches the network to it at once if another engine is active. |
| `stop` | Stops the Midsoft IQL Server, but only while it is the active engine and running. |
| `status` | Shows whether the Midsoft IQL Server is not installed, running or stopped. |

On a network that runs NextgreIQL, iqlengine stop answers that the Midsoft IQL Server is already stopped, because the Midsoft is not the active engine.

## If something goes wrong

> [!PROBLEM] You press Replace, and nothing happens.
> Replace is greyed out while a replacement runs, when the chosen engine already runs, or when the Mainframe is off. Read the state word first.

> [!PROBLEM] The Replace dialog says no other engine is installed.
> Install another engine from its disc in a drive linked to the Mainframe.

> [!PROBLEM] The Services tab says no engine is installed.
> Put the engine's disc in a drive linked to the Mainframe, then install it.

> [!PROBLEM] A replacement takes longer than you expected.
> The time grows with the item types the network stores, up to 2 minutes.

> [!SEE] jsc:engine_problems, jsc:midsoft_iql_server
