---
section: shell_basics
order: 250
---
# Pipes, redirection, background jobs, variables, wildcards and Tab

A few small signs change what a command reads and where its answer goes. They work on Linux, FreeBSD and UNIX System V, and this entry says where the three differ.

## Pipes and redirection

A pipe, written with a bar, takes what the command on its left prints and gives it to the command on its right. Two commands joined this way work as one.

The shell reads a bar or an ampersand only as a word of its own. Written as ls|grep x, the whole thing is one word, and no pipe is made.

Table: Operators

| Operator | What it does |
| --- | --- |
| `\|` | Sends the answer of the left command to the right one. |
| `>` | Writes the answer to a file, and replaces what the file held. |
| `>>` | Adds the answer to the end of a file. |
| `<` | Reads a file as the input of the command. |
| `&` | Runs the command in the background, as the last word of the line. |

The file name may stick to the arrow, as in >notes.txt, or follow it as its own word, as in > notes.txt. An arrow with no file name is a syntax error. A quoted operator is plain text, so a quoted bar is a plain character.

A pipe carries at most 4,096 lines from one command to the next.

Some commands take the whole terminal, so they cannot be piped or redirected: less, vim, emacs, vi and ee, info on Linux, and man when it opens a manual entry.

> [!DEFINE] Pipe
> A connection that gives what one command prints to the next command as its input.

> [!DEFINE] Redirection
> Sending a command's answer to a file with the arrows > and >>, or reading its input from a file with <.

## Background jobs

Put an ampersand as the last word of a line, and the command runs in the background. You get the prompt back at once, and you can go on typing while the command works.

> [!DEFINE] Job
> A command you started with the ampersand and left to run in the background. It has a number, which you use to list it or stop it.

Each background job takes 1 MB of the machine's memory. When no room is left, the shell refuses the new job.

## Variables

A variable is a name that stands for a text value. You give it a value with NAME=value on a line of its own, and you use it with a dollar sign: $NAME, or ${NAME} when letters follow the name directly.

> [!DEFINE] Variable
> A name that stands for a text value, such as USER for the login name.

A name holds only letters, digits and underscores, and it does not start with a digit. The set command gives a value on every machine. The export command makes a value visible to the programs the shell runs, and unset forgets a name.

Table: Names the machine answers

| Name | Value |
| --- | --- |
| `HOSTNAME` | The computer's name. |
| `USER` | The login name, which is always player. |
| `OS` | The system: Linux, FreeBSD or UNIX. |
| `PWD` | The folder you are in. |
| `PATH` | Set by default only on UNIX System V; on Linux and FreeBSD it is empty until you set it. |

A variable belongs to the computer, not to one window, so a name you set stays set for the whole computer.

## Wildcards

A star stands for any text in a file name. An unquoted word with a star opens into the names of the matching files in its folder, before the command runs.

> [!DEFINE] Wildcard
> A symbol, such as the star, that stands for other text in names. The shell replaces it with the matching names.

Put the word in quotes, as in "notes\*", and the shell keeps the star as you wrote it. On MC-DOS and the Frames systems the command gets the star itself.

## Tab

Press Tab to complete a word you have started. On the first word, Tab completes a command name. After a command such as mkfs.ext4 or mount, it completes a device name. Tab does not complete file or folder names.

Press Tab twice in a row, and the shell prints every possible completion above the prompt.

## What is not here

Some shell features of other systems are missing from these three. The table says what each one does here.

Table: Missing here

| Feature | What happens |
| --- | --- |
| `;` | The semicolon, && and \|\| are plain words that the command receives as text. |
| `2>` | There is no way to send errors to a file; only the arrows above exist. |
| `$(command)` | A command cannot take the place of its own answer. |
| `alias` | There is no alias command, and no ! history form, on any of these systems. |
| `find` | Not on Unix, where find is a DOS command; grep searches the lines of a file instead. |
| `more` | Not on Unix, where the command for reading by the page is less. |

## If something goes wrong

> [!PROBLEM] The shell says syntax error near unexpected token, followed by a bar or an arrow.
> A pipe needs a command on each side, and an arrow needs a file name. Put a space around the bar.

> [!PROBLEM] The shell says the command takes the whole terminal, so it cannot be piped or redirected.
> Run the command alone. To read a long answer, pipe the command into grep or head to keep only the lines you need, or write the answer to a file and read it with less.

> [!PROBLEM] The shell says the machine has no memory left for another job.
> Stop a finished or unwanted job with kill %<id> ([](jsc:processes_and_jobs)).

> [!PROBLEM] ls | wc -l prints 1.
> The ls command prints every name on one line unless you give -l. Use ls -l, which prints one line for each name.

> [!PROBLEM] The shell says a name is letters, digits and underscores, and does not start with a digit.
> Choose a variable name with only letters, digits and underscores, and do not start it with a digit.

> [!SEE] jsc:shell_prompt, jsc:processes_and_jobs
