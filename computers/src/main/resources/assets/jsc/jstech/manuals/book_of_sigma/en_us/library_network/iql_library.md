---
section: library_network
order: 446
---
# Iql: the network's language from a program

IQL is the network's query language. One IQL statement asks for something, or changes what the network holds and does. The language itself is in [](jsc:iql), and the book Learning IQL teaches it from zero in [](jsc:what_iql_is).

Iql lets a program send those statements itself, instead of you typing them at a prompt. Every Iql call needs a network with a Mainframe. Only Sigma# programs can use it.

Table: Iql members

| Member | What it does |
| --- | --- |
| `Iql.Run(statement)` | Runs one statement and gives an IqlResult. A refused statement does not stop the program. |
| `Iql.Query(statement)` | Runs a statement and gives its rows as a list of maps. A refused statement stops the program. |
| `Iql.Exec(statement)` | Runs a statement and gives an IqlResult. |
| `Iql.Exec(statement, values)` | The same, with the values passed to a stored statement. |
| `Iql.RunFile(path)` | Runs each statement in the file and gives an IqlResult. |

Exec puts EXEC in front of your statement, and adds each value after the statement, one after another. Use it to pass values to a stored statement.

RunFile reads the file first. When the file cannot be read, the program stops with the file's message.

## What the results hold

Table: Records of Iql

| Record | Fields | What each field holds |
| --- | --- | --- |
| `IqlResult` | `IqlResult.Ok, IqlResult.Message, IqlResult.Rows` | Ok: yes when the network took the statement. Message: the network's answer. Rows: the result lines. |

Each row is a map with three keys: name, quantity and detail. The name is the item or thing the row is about, quantity is its count, and detail says more about it.

> [!DEFINE] Row
> One line of a result, given as a map of name, quantity and detail.

## If something goes wrong

> [!PROBLEM] The program stops and says this computer is not on a network with a Mainframe.
> Build a network with a Mainframe, and check Mainframe.Online before you call Iql.

> [!PROBLEM] Query stops the program with the engine's text.
> Use Run or Exec instead. They give Ok false and the message, and the program goes on.

> [!PROBLEM] Ok is false, and the message says the statement was refused.
> Read the message. It is the network's answer, and the verbs are explained in [](jsc:iql).

> [!PROBLEM] RunFile stops the program with the file's message.
> Check the path with File.Exists before you call RunFile.

> [!SEE] jsc:iql, jsc:what_iql_is, jsc:mainframes
