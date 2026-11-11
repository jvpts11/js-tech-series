---
section: iql_start
order: 363
---
# Where you type IQL

You can type IQL in five places. Each one needs the network to be ready first, and each one reads its statements a little differently. The list below names the five places, and the parts after it show how to use each.

> [!DEFINE] script
> A file of IQL statements that the network runs one after another.

Table: The five places

| Place | How you reach it | What it is for |
| --- | --- | --- |
| The iql command | Type iql at the prompt, then a statement | One statement at a time |
| IQL Server Management Studio (ISMS) | Open the program | Writing and running whole scripts |
| `run` | Type run and a file name, on MC-NET | Running a file of statements |
| The Iql class of Sigma | Write a Sigma program that calls it | Programs that ask the network |
| Automation Manager | Open the program and fill in its form | Work the network repeats for you |

## The iql command at the prompt

Type iql, a space, then one statement, and press [[Enter]]. The command also answers to the names operation, op and sql, so op SELECT 64 iron_ingot does the same. It needs a data network under the computer. A read on a computer with no network answers "not on a network".

A read answers with rows, at most 64 of them. A write answers with one line, such as "SELECT queued: 64 Iron Ingot -> local storage". The item is named as the game shows it, such as Iron Ingot, not by its internal name.

> [!DEFINE] local storage
> This computer's own storage, where the items you take out of the network go.

## IQL Server Management Studio (ISMS)

The IQL Server Management Studio (ISMS) is the program for writing scripts. A script is several statements in one file, with room to edit them. ISMS opens and saves files that end in .iql.

In ISMS, each statement ends with a semicolon. A comment starts with two dashes and runs to the end of the line. Press the keys below to run what you wrote.

Table: The keys of ISMS

| Key | The command, as the menu writes it |
| --- | --- |
| `F5` | Execute |
| `Ctrl+E` | Execute Selection |
| `Alt+Break` | Cancel Executing Query |
| `F1` | IQL Reference |

A script can ask for values first. Its parameters are written in angle brackets with a type and a default, and the dialog "Specify Values for Template Parameters" lists each one as name (type).

## run, for a file

The run command runs a file of IQL on a computer that runs MC-NET ([](jsc:mc_net)). Type run, a space and the file's name. The computer needs a disk, and run takes only files that end in .iql.

run is stricter than ISMS. It reads one statement a line, skips every line that starts with two dashes, and takes statements without semicolons. It asks you for no values. It refuses the reads, QUERY and COUNT. It checks the whole file before the first line runs, so a bad line moves nothing.

## The Iql class in Sigma

Sigma, one of the programming languages of the computers, can ask the network too. Its class Iql, in the namespace System.Network, has four members.

Table: The members of the Iql class

| Member | What it gives back |
| --- | --- |
| `Run` | An IqlResult, with Ok, Message and Rows |
| `Query` | The rows of a read |
| `Exec` | An IqlResult. It also takes a list of strings. |
| `RunFile` | An IqlResult, from a file |

The exact way to call it is in [Iql](jsc:iql_library), in the Book of Sigma.

## Automation Manager

The Automation Manager ([](jsc:automation_manager)) does not take typed IQL. Its form makes work that repeats: Restock below, Batch Craft, Move, or IQL. For the IQL kind, you pick a .iql file from the Mainframe's system disk, and you set how often it runs in the field Every (30s).

Typing IQL needs the Midsoft IQL Server running. You start and stop it with the command iqlengine ([](jsc:engine_services)).

## If something goes wrong

> [!PROBLEM] You type iql alone, and the reply shows a usage line with an example.
> Type a statement after iql.

> [!PROBLEM] The reply says "not on a network".
> The computer has no data network under it. Join it with a data cable ([](jsc:data_cables)).

> [!PROBLEM] run says "only .iql files can be run".
> Rename the file so that it ends in .iql, or write the statements in a new .iql file.

> [!PROBLEM] run says "QUERY/COUNT are not supported by 'run'".
> run does not take reads, QUERY or COUNT. Type the read at the prompt, with iql or operation.

> [!PROBLEM] run says "syntax error in", followed by a line.
> The whole file is checked first, so no line ran. Fix the line that the message names.

> [!PROBLEM] ISMS says "No compatible Midsoft IQL Server was found on this network."
> Make sure the Mainframe is on, and that the Midsoft IQL Server is installed and running ([](jsc:engine_services)).

> [!PROBLEM] ISMS says the Midsoft IQL Server is not installed.
> Install it from the Network Manager, in its Network Services, as the message says ([](jsc:network_manager)).

> [!PROBLEM] A statement answers "Network Operations Service unavailable".
> The Midsoft IQL Server is stopped. Start it with iqlengine start.

> [!PROBLEM] The Automation Manager says "No engine - install the Automation Engine on the Mainframe".
> Install the Automation Engine on the Mainframe, then try again.

> [!PROBLEM] The reply says "jobs are kept by the Automation Engine".
> Install the Automation Engine on the Mainframe first.

> [!SEE] jsc:isms_studio, jsc:automation_manager, jsc:iql_engines_overview
