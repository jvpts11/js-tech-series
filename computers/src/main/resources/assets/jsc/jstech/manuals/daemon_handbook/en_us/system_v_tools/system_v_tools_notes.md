---
section: system_v_tools
order: 354
---
# vi, lp and man on System V

Three commands of this group are explained in full in other chapters, because the UNIX systems share them. This page says only what UNIX System V does on its own.

## vi

vi comes with the system, so it is always there, and you never install it. It uses the same engine as [vim](jsc:vim), with the messages of System V. The screen has no colour and no ruler in the corner. Its keys and colon commands are in [](jsc:vi).

Table: Messages when a file opens or is written

| Message | What it means |
| --- | --- |
| `"<name>" [New file]` | The file does not exist yet. Writing it makes the file. |
| `"<name>" <lines> lines, <characters> characters` | The file exists. The counts are its size. |

## lp

lp sends a file to a printer linked to the computer ([](jsc:peripheral_cables)). Its options and messages are on the printing page, [](jsc:printing). Unless you name a printer with -d, lp uses the default one.

## man

man shows the manual page of a command. On this system it also reads the wider manuals: man graphics-cards opens the manual's entry on graphics cards, as a page of section 7. The command is explained in [](jsc:man_pages), and whatis and apropos are in [](jsc:whatis_and_apropos).

## If something goes wrong

> [!PROBLEM] vi says "E37: no write since last change (add ! to override)".
> Write the file with :w first, or leave with :q! to throw the changes away.

> [!PROBLEM] lp says "lp: no default destination".
> Link a printer to the computer. The first printer linked becomes the default.

> [!PROBLEM] man says "No manual entry for intro".
> The intro topic belongs to FreeBSD. On UNIX System V, type man and the name of a command.

> [!PROBLEM] man says "What manual page do you want?"
> Give the name of a command after man, for example man graphics-cards.

> [!SEE] jsc:vi, jsc:printing, jsc:man_pages
