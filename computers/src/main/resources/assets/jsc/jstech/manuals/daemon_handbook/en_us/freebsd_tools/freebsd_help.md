---
section: freebsd_tools
order: 341
---
# Finding your way without help

FreeBSD has no help command. Typing help gets a line that points to two other ways. You use those ways, and the Tab key, to find any command.

> [!DEFINE] Candidate
> A command name that Tab could fill in.

At the prompt, type help and press [[Enter]]. FreeBSD answers with one line:

```session
{"lines": [{"text": "help: not found. Try: apropos <word>, or man intro"}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

## apropos, man and whatis

Table: The helpers of FreeBSD

| Command | What it does |
| --- | --- |
| `apropos <word>` | Searches the name and the summary of every command here, and every topic, for the word. |
| `man <command>` | Opens the manual page of a command. A manual entry such as graphics-cards opens the same way. |
| `man intro` | Opens the one topic FreeBSD has: the introduction to the commands. |
| `whatis <command>` | Shows the one line that a manual page opens with. |

1. Type apropos and a word for what you want to do, such as apropos network.
2. Read the lines it prints. Each line is a command name and its one line.
3. Type man and the command name to read its manual page.

The introduction is short, so read it first. It says that a command is a small program that does one thing, and that a shell runs the one you type at its prompt.

## Tab

Press [[Tab]] once to fill in a command name, when only one name fits. Press it twice in a row, with nothing left to fill, to list every candidate. The candidates show on the line above the prompt, with two spaces between them.

Table: Messages of the helpers

| Message | What it means |
| --- | --- |
| `help: not found. Try: apropos <word>, or man intro` | FreeBSD has no help command. Use one of the two ways it names. |
| `<text>: nothing appropriate.` | No command or topic matches the word you searched. |
| `No manual entry for <word>` | There is no manual page with that name. Check the spelling. |

## If something goes wrong

> [!PROBLEM] The computer says help: not found.
> Type apropos and a word for what you want to do, or type man intro.

> [!PROBLEM] apropos says nothing appropriate.
> Try a shorter word, or a different one.

> [!PROBLEM] man says No manual entry for graphics.
> Type the whole name, such as man graphics-cards.

> [!SEE] jsc:help_on_a_computer, jsc:finding_help_overview, jsc:man_pages, jsc:whatis_and_apropos, jsc:pipes_and_variables
