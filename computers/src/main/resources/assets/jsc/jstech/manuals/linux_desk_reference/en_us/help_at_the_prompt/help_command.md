---
section: help_at_the_prompt
order: 280
icon: jsc:guide_to_operations
---
# The help command

help is the command that tells you what the other commands do. Type help on its own, and it lists the commands this computer has. Type help and a command's name, and it prints that command's page: its form, what it does and its options.

On MC-DOS and MC-NET, help takes the whole screen, and you move through the page with the keys below. On Frames, Linux and UNIX System V it prints the page as text. The Help windows of the desktop are a different thing ([](jsc:help_on_a_computer)).

## Using help

1. Type help and press [[Enter]] to list the commands this computer has.
2. Type help, a space and a name, such as help ls on Linux, to read that command's page.
3. On MC-DOS or MC-NET, press [[Esc]] when you are done, to return to the prompt.

On MC-DOS and MC-NET, the top line of the full screen reads, for example, "MC-DOS Help: dir". The keys are the ones below.

Table: Keys of the full-screen help

| Key | What it does |
| --- | --- |
| `Tab` | Picks the next button or link. |
| `Shift+Tab` | Picks the button or link before. |
| `Enter` | Follows the picked link, or picks a menu item. |
| `Up, Down` | Scroll one line. |
| `Page Up, Page Down` | Move one page. |
| `Home, End` | Go to the start or the end of the topic. |
| `F10 or Alt+F` | Opens the File menu. |
| `Alt+S` | Opens the Search menu. |
| `F3` | Repeats the last find. |
| `Alt+C, Alt+N, Alt+B, Alt+I` | Show the contents, the next topic, the topic before, and the index. |
| `Escape` | Closes an open menu or search, or leaves the help when none is open. |
| `Alt+F4` | Leaves the help. |

To search, press [[Alt]]+[[S]], then press [[Enter]] on the first item of the Search menu, the one named Find with three dots after it. The screen asks "Find what:". Type the word and press [[Enter]].

The second item, "Repeat Last Find", searches again. When the word is not in the text, the screen says "Match not found."

```command
id: jsc:command/linux/help
```

> [!DEFINE] topic
> A subject the full-screen help opens on, such as a command's name or a title in its contents.

> [!DEFINE] link
> A word in the help text that you pick with Tab and follow with Enter.

## If something goes wrong

> [!PROBLEM] The reply is "no such command:" and the name you typed.
> The name is not a command of this computer. Type help on its own to list the commands it has.

> [!PROBLEM] The reply is "help: not found. Try: apropos <word>, or man intro".
> FreeBSD has no help command. Type apropos and a word, or man intro for the introduction.

> [!PROBLEM] The full screen does not close.
> Press Escape once for each open menu or search, then once more to leave the help.

> [!PROBLEM] The reply is "Match not found."
> Check the spelling of the word, or try a shorter word.

> [!SEE] jsc:man_pages, jsc:help_on_a_computer
