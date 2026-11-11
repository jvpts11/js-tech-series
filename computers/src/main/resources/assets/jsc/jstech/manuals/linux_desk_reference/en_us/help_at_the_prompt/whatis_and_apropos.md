---
section: help_at_the_prompt
order: 282
icon: jsc:guide_to_operations
---
# whatis and apropos

whatis gives the one line that says what a command is for. It is the quickest answer, and it tells you whether the full page is worth reading.

apropos is for when you know the job, not the name. It searches the name and the one-line summary of every command this computer has, and prints the commands that match.

The summaries are in English, whichever language the game shows you. Search with English words, such as network or disk.

## Using them

1. Type whatis and a command's name, such as whatis ls.
2. Type apropos and a word, such as apropos network, to find commands by their job.
3. Type man and the name of a command you want to read in full.

Table: What whatis prints

| Part of the line | What it tells you |
| --- | --- |
| The name, such as ls | The command this line is about. |
| `(1)` | The section. Every command's page is in section 1. |
| A hyphen and the summary | What the command is for, in a few words. |

Everything you type after apropos is the search. apropos network card looks for the whole phrase, network card, not for two words. Only the commands this computer has are searched.

```command
id: jsc:command/linux/whatis
```

```command
id: jsc:command/linux/apropos
```

> [!DEFINE] one-line summary
> The short description a command gives of itself in one line, which whatis and apropos read.

## If something goes wrong

> [!PROBLEM] The reply ends with "nothing appropriate." after the word you typed.
> No name or summary holds that word. Try a shorter word, or an English word from a summary, such as disk.

> [!PROBLEM] The reply is "whatis what?".
> You typed whatis with no name. Type whatis and a command's name, such as whatis ls.

> [!PROBLEM] The reply is "apropos what?".
> You typed apropos with no word. Type apropos and the word to search for, such as apropos network.

> [!PROBLEM] apropos finds nothing for a word in another language.
> The search reads the English summaries only. Type the English word.

> [!SEE] jsc:man_pages, jsc:help_command
