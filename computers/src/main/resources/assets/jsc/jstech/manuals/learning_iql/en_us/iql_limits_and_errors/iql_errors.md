---
section: iql_limits_and_errors
order: 382
---
# Error messages and their fixes

A refusal is an error message that stops a statement before it runs. You read it from the start: the first words say what went wrong, and the words in angle brackets show where your own text goes. Find the message below, and read the fix beside it.

## The form of a statement

Table: Refusals of the form

| The message | What it means, and the fix |
| --- | --- |
| `empty statement` | The line holds nothing. Type a statement. |
| `unknown verb: <word>` | The first word is not a verb. Check its spelling against the verbs in this book. |
| `unexpected token: <text>` | A word or sign stands where the statement does not take it. Remove it, or use a verb that takes it. |
| `percentage quantities are not supported yet: <quantity>` | Type a whole number, or ALL. |
| `LIMIT takes a row count, not a percentage: <quantity>` | Type a whole number of rows after LIMIT. |
| `unknown priority level: <word> (expected LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or HIGH)` | Type one of the five levels. |
| `DELETE exports out of the network and needs a TO destination` | Add TO and the name of a bus. |
| `MOVE is internal and needs both FROM and TO` | Add FROM and TO. |
| `<VERB> has no TO destination` | Remove TO. SELECT, INSERT, DROP and UPDATE take none. |
| `<VERB> is not valid on a read` | A QUERY refuses FROM, TO, IF and PRIORITY. Put the server into WHERE instead. |
| `UPDATE needs SET and what the card is to do, after the item` | Add SET, then the action word. |
| `unknown action: <word> (expected SMELT, ENCHANT, REPAIR, COMBINE or NAME)` | Use one of the five action words. |
| `an offer is 1, 2 or 3, not <n>` | Type 1, 2 or 3. |
| `a name is at most 50 characters long` | Shorten the new name of the anvil to 50 characters or fewer. |
| `a condition nests at most 64 NOTs and brackets deep` | Remove some NOT or brackets. |
| `a condition compares at most 256 things` | Shorten the condition. |
| `<ACTION> takes no WITH` | Only REPAIR and COMBINE take WITH, so remove it. |
| `COMBINE needs WITH and the item it adds` | Add WITH and the second item. |
| `unknown operator: <text>` | Use one of the operators of WHERE ([](jsc:query_and_select)). |
| `unknown duration unit '<unit>' in: <text>` | Use t, s, m or h as the unit, or none for seconds. |

Table: Missing parts: expected

| The message | What it means, and the fix |
| --- | --- |
| `expected a verb` | Type a verb at the start, such as QUERY. |
| `expected an item` | Type the item's id after the verb. |
| `expected an object to query` | Type the object after QUERY, such as items. |
| `expected a quantity, got: -5` | A quantity is a whole number of zero or more. |
| `expected a location after FROM` | Type the server's or the bus's name as one word. |
| `expected a location after TO` | Type the bus's name as one word. |
| `expected BY` | Type BY after ORDER. |
| `expected a sort field after ORDER BY` | Type a field name after ORDER BY. |
| `expected a level after PRIORITY (LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH)` | Type one of the five levels after PRIORITY. |
| `expected a row count after LIMIT` | Type a whole number after LIMIT. |
| `expected a field name` | Type a field name, such as qty. |
| `expected a comparison operator` | Type an operator, such as = or >. |
| `expected a value` | Type a value after the operator. |
| `expected an action after SET (SMELT, ENCHANT, REPAIR, COMBINE, NAME)` | Type one action word after SET. |
| `expected an offer after OFFER (1, 2 or 3)` | Type 1, 2 or 3. |
| `expected a name in quotes after NAME` | Put the new name in quotes. |
| `expected the pattern's name` | Type the pattern's name. |
| `expected one of the pattern's inputs` | Type one of the pattern's inputs. |
| `expected the router's name` | Type the router's name. |
| `expected a list of items` | Type at least one item or tag. |
| `expected a count` | Type a whole number. |

## The network's answers

Table: Refusals from the network

| The message | What it means, and the fix |
| --- | --- |
| `Network Operations Service unavailable` | No engine that the statement needs runs on the Mainframe. Start one ([](jsc:iql_engines_overview)). |
| `the network has no running Mainframe` | A Mainframe must run on the network ([](jsc:mainframes)). |
| `syntax: <reason>` | The engine could not read the statement. The reason after it says what is wrong. |
| `unknown item: <name>` | The name is not an item, or the WHERE kept no row for it. Check the spelling and the WHERE. |
| `no server named '<server>'` | The server does not exist. Check its name. |
| `no server or bus named '<name>'` | Neither a server nor a bus has that name. |
| `no bus named '<bus>'` | No bus has that name on the network. |
| `the bus '<bus>' touches no inventory` | The bus has no chest or machine beside it. |
| `the network holds no <item>` | No copy of the item is on the network. |
| `no pattern crafts <item>` | No recipe on the network makes the item. |
| `this computer has no <card name>` | Put that card into this Personal Computer. |
| `only a Personal Computer's own cards change items for the network` | Run the statement on a Personal Computer. |
| `somebody has to ask for it: the card works it for them, as they would at the block` | Run the statement from a player's terminal. |
| `the <item> does not smelt` | Pick an item that smelts. |
| `the <item> takes no enchantment` | Pick an item that takes an enchantment. |
| `the <item> is not worn` | REPAIR needs a damaged copy. |
| `the network holds nothing that mends the <item>` | Put a material on the network, or name one after WITH. |
| `maintenance runs on the Mainframe only` | Type the command at the Mainframe itself. |
| `the network's engine keeps no views, procedures or jobs` | Use the [Midsoft IQL Server](jsc:midsoft_iql_server). |
| `jobs are kept by the Automation Engine; install it on the Mainframe first` | Install the Automation Engine on the Mainframe. |
| `the destination server is unavailable` | The destination cannot take items now. |
| `could not start the MOVE` | The network did not start the move. |
| `could not start the UPDATE` | The network did not start the change. |
| `nothing to lock: the network holds no free <item>` | Wait for free stock, then lock it. |
| `<item> is already locked` | The hold is already in place. |
| `<item> is not locked` | There was no hold to release. |

## Settings of buses, interfaces and redstone

Table: Refusals of the settings

| The message | What it means, and the fix |
| --- | --- |
| `SET BUS needs the bus's name, in quotes when it has spaces` | Type the bus's name after BUS. |
| `SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY, WHEN, AFTER BUS or ACCESS` | Add one setting word after the name. |
| `a bus has no setting <word>` | Use a setting from the table of SET BUS. |
| `not an hour of the day: <text>` | Type an hour from 0 to 23. |
| `SET INTERFACE needs the interface's name, in quotes when it has spaces` | Type the interface's name after INTERFACE. |
| `SET ROUTER needs the router's name, in quotes when it has spaces` | Type the router's name after ROUTER. |
| `SET INTERFACE needs a setting: EXCLUSIVE, MAX JOBS or ROUTE` | Add one of those three settings after the name. |
| `an interface has no setting <word>` | Use EXCLUSIVE, MAX JOBS or ROUTE. |
| `RENAME needs TO and the new name` | Write TO and the new name after the old name. |
| `SET REDSTONE needs the interface's name, in quotes when it has spaces` | Type the interface's name after REDSTONE. |
| `SET REDSTONE needs IN, or OUT and a strength from 0 to 15` | Add IN, or OUT followed by a number. |
| `not a redstone strength, which runs from 0 to 15: <text>` | Type a whole number from 0 to 15. |

## Saved statements, durations and files

Table: Messages of saved statements, durations and files

| The message | What it means, and the fix |
| --- | --- |
| `CREATE <TYPE> needs a name and an AS body` | Type a name, then AS and the body, in that order. TYPE is VIEW, PROCEDURE or JOB. |
| `expected AS after the <TYPE> name` | Type AS after the name. |
| `CREATE <TYPE> needs a body after AS` | Type the body after AS. |
| `CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>` | Add EVERY and a duration, or WHEN and a condition. |
| `CREATE JOB needs a body before its trigger` | Put the statement before EVERY or WHEN. |
| `CREATE JOB trigger needs a value` | Type the duration or the condition after the trigger word. |
| `DROP <TYPE> needs a name` | Type the name after the kind. |
| `EXEC needs a procedure name` | Type the name after EXEC. |
| `EXEC takes a single procedure name` | Run one procedure per EXEC. |
| `empty duration` | Type a duration after EVERY, such as 30s. |
| `not a duration: <text>` | Type a whole number, with t, s, m or h after it. A decimal such as 1.5s is refused. |
| `duration must be >= 0: <text>` | Type a duration of 0 or more. |
| `no view named <name>` | No view has that name. |
| `no procedure named <name>` | No procedure has that name. |
| `no job named <name>` | No job has that name. |
| `procedure <name> stopped at statement <n>: <error>` | Fix the statement that failed, then run EXEC again. |
| `IQL recursion too deep (a procedure or view referencing itself?)` | A saved statement runs itself. Remove the loop. |
| `IQL stopped after 4096 statements for one request (procedures running others too many times?)` | Give the procedures fewer statements to run. |
| `the IF did not hold, so the <VERB> was not run` | Not a failure: the IF was false. Check the value of the field it compares. |
| `<file>: syntax error in '<statement>': <reason>` | A line of the file is not a statement. Nothing runs until the line is fixed. |
| `<file>: only .iql files can be run (got .<ext>)` | Only a file that ends in .iql runs. Rename the file. |
| `<file>: nothing to run` | The file holds no statement. |
| `<file>: QUERY/COUNT are not supported by 'run', use 'operation' instead` | A file cannot run a read. Type the read at the prompt. |

A file runs one statement per line. Blank lines and lines that start with -- are left out. A semicolon does not split a line in a file, so put one statement on each line. The file stops at the first statement that fails, and the statements above it have already run. The whole file is read first, so a syntax error on any line means nothing runs.

## If something goes wrong

> [!PROBLEM] a name is at most 50 characters long
> Shorten the name. Only the new name of an anvil has this limit.

> [!PROBLEM] unknown item: <name>
> Check the spelling, then check that the WHERE keeps the row you want.

> [!SEE] jsc:iql_limits, jsc:query_and_select, jsc:iql_engines_overview
