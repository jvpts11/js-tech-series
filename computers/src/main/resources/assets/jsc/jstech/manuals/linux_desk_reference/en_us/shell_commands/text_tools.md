---
section: shell_commands
order: 252
---
# Reading and sorting text

Text tools read lines of a file, or the lines that a pipe brings in. They count the lines, pick some out, put them in order, or show them one screen at a time.

A line is the text between two line breaks. Most of these tools count or choose by line.

> [!DEFINE] Pager
> A program that shows one screen of text at a time, so you can read a long file.

```command
id: jsc:command/linux/grep
```

```command
id: jsc:command/linux/wc
```

```command
id: jsc:command/linux/head
```

```command
id: jsc:command/linux/tail
```

```command
id: jsc:command/linux/sort
```

## Keys in less

less shows a file one page at a time. These keys move you through it, and each one works while less holds the whole terminal.

Table: Keys in less

| Key | What it does |
| --- | --- |
| `Space` | Shows the next page. |
| `b` | Shows the page before. |
| `Arrow keys` | Move one line at a time. |
| `/` | Searches the text for what you type next. |
| `n` | Goes to the next match of the search. |
| `q` | Leaves less and gives you back the prompt. |

```command
id: jsc:command/linux/less
```

## If something goes wrong

> [!PROBLEM] grep shows no lines at all.
> grep matches the exact text you type, so check the spelling. Add -i to ignore upper and lower case.

> [!PROBLEM] less says it cannot be piped.
> Type less alone, with a file name. To read only some lines, first pick them with grep, then read the result in a file with less.

> [!SEE] jsc:file_commands, jsc:pipes_and_variables
