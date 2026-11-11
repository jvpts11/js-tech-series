---
section: help_at_the_prompt
order: 281
icon: jsc:guide_to_operations
---
# The man command

man shows the manual page of a command. It works on the UNIX systems: Linux, UNIX System V and FreeBSD. On MC-DOS, MC-NET and Frames, use help instead.

When the manuals of the series hold a page for the command, man opens that page in the pager. When they hold none, man prints the command's own page on the terminal, in capitals, such as LS(1).

On UNIX and FreeBSD, man also reads the entries of the manuals, which you can open by name. An entry named by its last part, such as graphics-cards, opens in the pager as a page of section 7. The entry on graphics cards is one of them ([](jsc:graphics_cards)).

## Using man

1. Type man and a command's name, such as man ls on Linux.
2. Read the page, then press q to return to the prompt.
3. On UNIX or FreeBSD, type man graphics-cards to read the entry on graphics cards.

Table: Keys in the pager

| Key | What it does |
| --- | --- |
| `Space` | Moves one page down. |
| `b` | Moves one page up. |
| `j or Down` | Moves one line down. |
| `k or Up` | Moves one line up. |
| `Page Down, Page Up` | Moves one page in either direction. |
| `g or Home` | Goes to the top of the page. |
| `G or End` | Goes to the end of the page. |
| `/` | Starts a search for the text you type. |
| `n` | Finds the next match of the search. |
| `q or Escape` | Gives the terminal back. |

The line at the foot shows the page's name with (7), the line you are on, and "(press q to quit)".

When you type a word after man, it looks for a match in this order, and stops at the first match it finds.

Table: The order man follows

| Step | What man shows |
| --- | --- |
| A command of this system, with a page in the manuals | That page, opened in the pager. |
| An entry of the manuals, on UNIX and FreeBSD | The entry, opened in the pager. |
| A command of this system, with no page in the manuals | The command's own page, printed on the terminal in capitals, such as LS(1). |
| A topic such as intro, on FreeBSD | The topic's page, printed on the terminal as INTRO(1). |
| Nothing of these | "No manual entry for" and the word you typed. |

```command
id: jsc:command/linux/man
```

> [!DEFINE] section
> The number in brackets after a page's name: (1) for a command, and (7) for a manual entry.

> [!DEFINE] pager
> A screen that shows a long text one screenful at a time, with keys to move through it.

## If something goes wrong

> [!PROBLEM] The reply is "No manual entry for graphics-cards" on Linux.
> On Linux, man reads only commands. Read the entry with info ([](jsc:info_keys)) instead.

> [!PROBLEM] The reply is "No manual entry for graphics".
> Type the whole name, graphics-cards. A part of the name finds nothing.

> [!PROBLEM] The reply is "What manual page do you want?".
> You typed man with no name. Type man and a command's name, such as man ls on Linux.

> [!PROBLEM] The hint under it suggests "man intro", and that gives "No manual entry for intro".
> The topic intro exists on FreeBSD only. On Linux and UNIX System V, use help or apropos.

> [!SEE] jsc:whatis_and_apropos, jsc:info_keys, jsc:graphics_cards
