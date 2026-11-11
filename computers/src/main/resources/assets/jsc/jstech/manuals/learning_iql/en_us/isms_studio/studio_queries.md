---
section: isms_studio
order: 394
---
# Writing and running queries

Each query tab holds one script: the statements you write, and the results of the last run. The tabs are named Query 1, Query 2 and so on. [[Ctrl]]+[[Tab]] moves to the next tab.

## Your first run

1. Press [[Ctrl]]+[[N]] to open a new query tab.
2. Type a statement and end it with a semicolon, for example QUERY items LIMIT 10;
3. Press [[F5]] to run the whole script.
4. Read the results under the script. Press [[Ctrl]]+[[D]] to see them as a grid.

Statements run one at a time. The studio sends the next one only when the last one has come back.

Each Operation that a run starts is listed in the Messages tab, as "Operation #<id> started - Cancel Executing Query stops it". Press [[Alt]]+[[Break]], or the toolbar's Cancel, to stop the run. The studio then answers "Query was cancelled by user."

## Statements that cannot be undone

A DROP runs as you type it, with no question, as on a real database: what it drops is gone. A DELETE of ALL sends everything of an item out of the network, and before one runs the studio asks "This statement cannot be undone:", shows the statement and asks "Run it?". Its buttons are Yes, No and a box named "Don't ask again". Ticking that box turns the question off.

## Options

Table: Options of the studio

| Option | Default | What it does |
| --- | --- | --- |
| Ask before a statement that cannot be undone | on | Asks before each DELETE of ALL. Tools > Options. |
| Open a new query when the studio starts | on | Opens an empty query tab at the start. Tools > Options. |
| Show line numbers | on | Shows a number beside each line of a script. Tools > Options. |
| Stop the script at the first error | on | Stops the run at the first failed statement. Query > Query Options. |

## Limits

A statement may be 512 characters long. A longer one is refused, and the studio marks the line where it starts.

## Results and messages

Results appear in a grid by default. [[Ctrl]]+[[T]] shows them as text, and [[Ctrl]]+[[Shift]]+[[F]] saves them to a file as comma-separated values.

The Messages tab, next to Results, lists what the run did. It keeps at most 200 messages for each tab.

## Checking and planning

[[Ctrl]]+[[E]] runs only the selected text. [[Ctrl]]+[[F5]] checks the syntax without running anything. A clean check answers "Commands completed successfully."

[[Ctrl]]+[[L]] shows the estimated plan of a CRAFT statement. For any other statement the studio answers "Display Estimated Plan works on a CRAFT statement." The plan lists the steps chosen to make the craft.

## Saving and opening

Save writes the tab to its file. Save As... writes a new script file. Scripts are saved with the extension .iql, and a file typed as IQL opens in the studio.

## If something goes wrong

> [!PROBLEM] The studio says "There is nothing to run."
> Type a statement in the tab first, or select the text you want to run.

> [!PROBLEM] The studio marks a line: "Line <n>: the statement is <length> characters long, more than the 512 a statement may be."
> Split the statement into shorter statements, each ending with a semicolon.

> [!PROBLEM] The studio asks "<tab> has changes that are not saved. Close it anyway?"
> Press [[Ctrl]]+[[S]] to save the tab first, then close it.

> [!PROBLEM] The run answers "Query completed with errors".
> Read the Messages tab, fix the line it names, and run the script again.

> [!PROBLEM] Parse answers "Parse found an error" beside a line.
> Fix the line that the message names, then parse again with [[Ctrl]]+[[F5]].

> [!PROBLEM] Save answers "There are no results to save."
> Run a query first, then save its results.

> [!PROBLEM] The studio answers "Display Estimated Plan works on a CRAFT statement."
> Put a CRAFT statement in the tab, then press [[Ctrl]]+[[L]].

> [!SEE] jsc:query_and_select, jsc:iql_limits
