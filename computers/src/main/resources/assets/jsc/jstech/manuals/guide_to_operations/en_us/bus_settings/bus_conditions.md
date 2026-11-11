---
section: bus_settings
order: 128
---
# Conditions

A condition is a rule that a bus checks before it moves. A Standard or later bus can have up to 4 conditions, and every one of them must hold for the bus to move. A bus whose condition does not hold waits, and its Activity tab says so.

The bus checks its conditions every 20 ticks, which is a second, and keeps the answer until the next check. You can also write conditions in IQL ([](jsc:iql)) with WHEN.

Table: The three kinds of condition

| Kind | What the window says | It holds when |
| --- | --- | --- |
| `STOCK` | only while the network holds under 64 minecraft:iron_ore | The network holds fewer than that number. |
| `HOURS` | only between 18:00 and 06:00 | The game's clock is between the two hours. |
| `AFTER` | after the bus Coal in finishes | The named bus has made no move for 40 ticks. |

## STOCK

STOCK counts what the whole network holds. The condition holds while that total is below the number you set. The number starts at 64 items and changes by 1 item; with Shift held, it changes by 64 items.

> [!DEFINE] Stock
> How many of an item the whole network holds, not the count in one chest.

Write a tag after a # sign, such as #c:ores, and STOCK counts every held item that carries the tag. In the window, click the item field with an item in your hand to write that item's id into it.

## HOURS

HOURS holds between two hours of the day. You set FROM and TO, and when TO comes before FROM, the window runs past midnight. The default is from 18:00 to 06:00, the night.

The hours follow the game's clock, not the real one. 06:00 is the start of each game day. If FROM and TO are the same hour, the condition holds all day.

A bus holds only one hours window. Adding a second one is refused, and the action bar says "A bus holds only one hours window. Remove it first to set another." From IQL, a new hours window replaces the old one.

## AFTER

AFTER waits for another bus on the same network. It holds once the named bus has made no move for 40 ticks, which is 2 seconds. Finished here means no move for that long, not that the job is done.

If no bus has that name, nothing holds this bus back, so the condition holds. Type the name in the window, up to 32 characters. The name is not case-sensitive.

## Adding and removing

1. Open the bus's window and go to CONFIGURE.
2. Click + condition, and choose STOCK, HOURS or AFTER.
3. Fill in the editor, with the - and + buttons for the numbers.
4. To take a condition off, click the right end of its row.

## If something goes wrong

> [!PROBLEM] The bus waits and does not move.
> Open its Activity tab. A line that says "a condition holds it" means a condition is not met, so the bus waits. Read the sentence of each condition in CONFIGURE ([](jsc:bus_activity)).

> [!PROBLEM] The window refuses a STOCK condition.
> A count below 1, an unknown item id or a bad tag is refused. The window shows nothing when it refuses.

> [!PROBLEM] The bus does not take a fifth condition.
> A bus holds up to 4 conditions. Take one off first, then add the new one.

> [!PROBLEM] The HOURS condition holds all day.
> FROM and TO are the same hour. Set them to two different hours.

> [!SEE] jsc:bus_activity
