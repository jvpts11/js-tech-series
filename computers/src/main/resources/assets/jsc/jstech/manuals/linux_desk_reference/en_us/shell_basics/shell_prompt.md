---
section: shell_basics
order: 248
---
# What a shell is, and its prompt

A shell is the program that reads the line you type, runs the command named in that line, and prints what the command answers. You type into the terminal, the text screen of a computer, at the prompt: the short line of text that waits for you.

Each computer has one prompt, and you type into it from the computer's own screen. Linux, FreeBSD and UNIX System V share one set of commands and one way of reading a line. A command that one of these systems lacks is not in its list, so it cannot run there.

You log in as player on all three systems, and the prompt shows that name first. The part after the at sign is the computer's own name, which you change with the config command ([](jsc:system_commands)).

## The prompt on each system

Table: Prompt by system

| System | Shell | Prompt |
| --- | --- | --- |
| Ubuntu, Debian and Fedora | bash | `player@<name>:<folder>$` |
| Gentoo | bash | `player@<name>:<folder>$` |
| Arch Linux | zsh | `player@<name> <folder> %` |
| FreeBSD | sh | `root@<name>:<folder> #` |
| UNIX System V | sh | `player@<name>:<folder> $` |

The folder part shows where you are. The sign ~ stands for your home folder, the folder your files live in: /home/player on Linux and FreeBSD, and /usr/player on UNIX System V.

On FreeBSD the prompt shows root and ends in #, because a fresh FreeBSD machine opens as root. These systems have no sudo and no su command, so you do not change accounts with a command.

> [!DEFINE] Shell
> The program that reads the lines you type and runs the commands in them.

> [!DEFINE] Prompt
> The text before the cursor, which names who you are, on which computer and in which folder.

> [!DEFINE] Root
> The all-powerful account of a system. On FreeBSD the prompt names that account and ends in the sign #.

> [!DEFINE] Terminal
> The text screen of a computer, with a keyboard, where you type commands and read the answers.

> [!DEFINE] Command
> A program you call by its name, with the words after the name that say what it works on.

## If something goes wrong

> [!PROBLEM] I cannot find the command I need.
> Type help on Linux or UNIX System V to list the commands; FreeBSD has no help command. The apropos command with one word finds commands by what they do ([](jsc:whatis_and_apropos)).

> [!PROBLEM] The shell says the name is not found, and suggests apropos or man intro.
> apropos with one word finds the command you want. man intro reads a manual entry on FreeBSD only.

> [!PROBLEM] A command the manual lists says it is not found.
> The command belongs to a package you have not installed. The which command says whether it is here and what put it there ([](jsc:software_commands)).

> [!SEE] jsc:file_tree, jsc:help_command
