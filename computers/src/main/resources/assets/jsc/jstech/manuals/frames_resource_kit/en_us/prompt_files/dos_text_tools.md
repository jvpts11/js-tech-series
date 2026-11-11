---
section: prompt_files
order: 239
---
# Text: find, sort, more and the pipe

The DOS family has three verbs for text. find prints the lines that hold some text, sort puts lines in order, and more shows a file one screen at a time.

find and sort take their text from a file that you name, or from a pipe. more reads only the file that you name.

> [!DEFINE] Pipe
> A line of two or more commands joined by a bar symbol, so that the output of the first command becomes the input of the next.

The pipe sends the output of one command to the next command as its input. Each bar needs a command on each side of it. A stage hands at most 4,096 lines to the next one.

> [!DEFINE] Redirection
> Sending the output of a command to a file, or reading its input from a file, with an arrow symbol.

Table: Symbols that join and redirect

| Symbol | What it does |
| --- | --- |
| `\|` | Sends the output of the left command to the right command. |
| `>` | Writes the output to a file, replacing what the file had. |
| `>>` | Adds the output to the end of a file. |
| `<` | Reads the input from a file. |

The DOS text tools take a file name or a pipe. The Unix systems have tools of the same names that work separately, with their own rules ([](jsc:text_tools)).

## find

find prints the lines that hold the text in double quotes. It is case-sensitive unless you add /I, which ignores capital letters. /V prints the lines that do not hold the text.

```command
id: jsc:command/mc_dos/find
```

## sort

sort puts the lines in order, comparing them as text. /R reverses the order. It reads a pipe or the file that you name.

```command
id: jsc:command/mc_dos/sort
```

## more

more shows a file one screen at a time. It is a pager: a viewer that waits for you between screens. Its keys are these.

> [!DEFINE] Pager
> A viewer that shows one screen of a file at a time and waits for you to go on.

Table: Keys in more

| Key | What it does |
| --- | --- |
| `Space` | Goes on to the next screen. |
| `Page Down` | Goes on to the next screen. |
| `b` | Goes back one screen. |
| `Page Up` | Goes back one screen. |
| Up and Down arrows | Move one line. |
| `/` | Looks for the text you type. |
| `n` | Finds the next match of the search. |
| `q` | Gives the terminal back. |

```command
id: jsc:command/mc_dos/more
```

## If something goes wrong

> [!PROBLEM] find prints nothing.
> find is case-sensitive unless you add /I. Check the capital letters too, and keep the text in double quotes.

> [!PROBLEM] sort does not put the numbers in order as numbers.
> sort compares lines as text, so a line "10" comes before a line "9". The DOS sort has no numeric option.

> [!PROBLEM] The line says syntax error near unexpected token, and names the bar.
> A pipe needs a command on each side of the bar.

> [!PROBLEM] An arrow has no file name after it, and the line stops with a syntax error.
> Type the file name after the arrow, such as > RESULT.TXT.

> [!SEE] jsc:pipes_and_variables, jsc:text_tools
