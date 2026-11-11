---
section: dos_programs
order: 208
---
# The help, full screen

Type help at the prompt to open the help of MC-DOS. It takes the whole screen, in place of the prompt, and it shows the topics of the manual. Its topics come from the Frames Resource Kit, the book you are reading.

Type help and a command's name, such as help dir, to open the page of that command. The same page opens when you type the command's name, a space and /?.

The help screen has four parts, from the top down, as the table shows.

Table: The parts of the help screen

| Part | What it shows |
| --- | --- |
| Top line | The menus File and Search. |
| Second line | Four buttons: Next, Back, Contents and Index. |
| Middle | The topic, with its links. |
| Bottom line | The keys, a message or the Find prompt. |

1. Type help at the prompt and press [[Enter]].
2. Press [[Tab]] until a button or a link has the focus, then press [[Enter]] to follow it.
3. Press [[Alt]]+[[N]] for the next topic, [[Alt]]+[[B]] for the one before, [[Alt]]+[[C]] for the Contents and [[Alt]]+[[I]] for the Index.
4. Press [[Alt]]+[[S]] to open the Search menu, pick Find... and press [[Enter]].
5. Type the words to find, then press [[Enter]].
6. Press [[Esc]] to leave the help, or press [[Alt]]+[[F4]].

Table: The keys of the help

| Key | What it does |
| --- | --- |
| `Tab` | Picks the next button or link, and Shift with Tab picks the previous one. |
| `Enter` | Follows the link or button picked. |
| `Up and Down` | Scroll one line. |
| `Page Up and Page Down` | Scroll one page. |
| `Home and End` | Go to the top and the bottom of the topic. |
| `F10 or Alt+F` | Opens the File menu, with Exit. |
| `Alt+S` | Opens the Search menu, with Find... and Repeat Last Find. |
| `Alt+C` | Shows the Contents. |
| `Alt+N` | Shows the next topic. |
| `Alt+B` | Shows the topic before. |
| `Alt+I` | Shows the Index. |
| `F3` | Repeats the last find. |
| `Esc` | Closes an open menu or the Find prompt, or leaves the help when nothing is open. |
| `Alt+F4` | Leaves the help. |

The bottom line shows only Alt+C=Contents, Alt+N=Next and Alt+B=Back. Alt+I and Alt+S work too.

Find first looks for a command with exactly that name. Then it looks for the next entry of the manual you are reading whose title or text has every word you typed. It goes round from the topic on the screen. Repeat Last Find, or [[F3]], looks for the same words again.

> [!DEFINE] Topic
> One page of the help: the page of a command, or an entry of a manual.

> [!DEFINE] Link
> A word in a page that you pick with Tab and follow with Enter.

## If something goes wrong

> [!PROBLEM] The bottom line says Match not found.
> No command and no entry has every word you typed. Try fewer words, or the name of a command.

> [!PROBLEM] Esc does not leave the help.
> Esc first closes an open menu or the Find prompt. Press it again, or press [[Alt]]+[[F4]].

> [!SEE] jsc:help_on_a_computer, jsc:dos_prompt
