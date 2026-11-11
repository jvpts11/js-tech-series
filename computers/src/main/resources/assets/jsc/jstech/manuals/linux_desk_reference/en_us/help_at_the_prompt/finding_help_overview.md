---
section: help_at_the_prompt
order: 279
icon: jsc:guide_to_operations
---
# Where to look

Forgot the name of a command, or the option you need? You can ask the computer at the prompt, without leaving the terminal. Five tools answer the question what a command does, and one tool lists every command the computer has.

Each tool works on some systems and not on others. The same pages appear in every book that holds these chapters, so each page says which systems it applies to.

Table: Where each tool works

| Tool | Where it works | What it gives you |
| --- | --- | --- |
| `help` | MC-DOS, MC-NET, Frames, Linux, UNIX System V | A command list, or one command's page; full screen on MC-DOS and MC-NET. |
| `man` | Linux, UNIX System V, FreeBSD | A command's manual page, and the entries of the manuals. |
| `whatis` | Linux, UNIX System V, FreeBSD | One line that says what a command is for. |
| `apropos` | Linux, UNIX System V, FreeBSD | The commands whose name or summary holds a word you give. |
| `info` | Linux | The manuals as linked screens, read with keys. |
| `listcmd` | Every system, when the server allows it | Every command and program this computer has. |

Table: Which tool to start with

| You want | Start with |
| --- | --- |
| How a command is used, when you know its name | help <command> where help exists; man <command> on UNIX systems |
| What a command is for, in one line | `whatis <command>` |
| A command, found by what it does | `apropos <word>` |
| Every manual and command, read as linked screens | `info` |
| Every command and program this computer has | `listcmd` |

Most systems answer an unknown word with "command not found:" and the word, then a hint to type help. UNIX System V answers with the word and "not found" only, and FreeBSD, which has no help command, points you to apropos and man intro.

> [!DEFINE] command
> A word typed at the prompt that makes the system do one job, such as ls, help or man.

> [!DEFINE] manual page
> The short written page that explains one command: its name, its form, what it does and its options.

## If something goes wrong

> [!PROBLEM] The reply is "command not found: info", or on FreeBSD "info: not found. Try: apropos <word>, or man intro".
> info is on Linux only. On UNIX System V and FreeBSD, use man <command>; on MC-DOS, MC-NET and Frames, use help <command>.

> [!PROBLEM] The reply is "command not found: listcmd".
> The server has the switch off. Read the listcmd page ([](jsc:listcmd)) to see how it is turned on.

> [!SEE] jsc:help_on_a_computer, jsc:shell
