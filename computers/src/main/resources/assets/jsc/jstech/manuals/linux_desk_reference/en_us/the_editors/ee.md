---
section: the_editors
order: 278
---
# ee

ee is a small editor, and it comes only with FreeBSD. It has no modes. Every key you press types a letter, except the keys held with Control and the Escape key.

While the info window is on, five rows of shortcuts sit on the screen. Each row shows a key and the name of what it does. The ^ in front of a letter means Control: ^k is Control with K.

> [!DEFINE] Info window
> The five rows of shortcuts on the screen, which you can hide in the settings.

Table: The shortcut rows

| Key | Name on the screen | What it does |
| --- | --- | --- |
| `^[` | (escape) menu | Opens the main menu. |
| `^y` | search prompt | Asks for the text to search for. |
| `^k` | delete line | Deletes the line the caret is on, and keeps it for undelete line. |
| `^p` | prev li | Moves up one line. |
| `^g` | prev page | Moves up one screen. |
| `^o` | ascii code | Asks for a number from 0 to 255, and types that character. |
| `^x` | search | Searches again for the last text you searched for. |
| `^l` | undelete line | Puts the last deleted line back under the caret's line. |
| `^n` | next li | Moves down one line. |
| `^v` | next page | Moves down one screen. |
| `^u` | end of file | Moves to the last line of the file. |
| `^a` | begin of line | Moves to the start of the line. |
| `^w` | delete word | Deletes the word after the caret, and keeps it for restore word. |
| `^b` | back 1 char | Moves one character left. |
| `^z` | next word | Moves to the start of the next word. |
| `^t` | top of text | Moves to the start of the text. |
| `^e` | end of line | Moves to the end of the line. |
| `^r` | restore word | Puts back the word that delete word removed last. |
| `^f` | forward char | Moves one character right. |
| `^c` | command | Asks for a command: write, quit or exit. |
| `^d` | delete char | Deletes the character under the caret, and keeps it for undelete char. |
| `^j` | undelete char | Puts back the character that delete char removed last. |

The fifth row ends with the tip ESC-Enter: exit. Names such as prev li and next li are short, because the screen is narrow.

## The main menu

Press [[Esc]] to open the main menu, titled ee main menu. Move with the Up and Down keys, choose with [[Enter]], or close the menu with [[Esc]].

Table: The main menu

| Item | What it does |
| --- | --- |
| leave editor | Asks what to do with the changes: save changes, which is selected at first, or no save. |
| help | Shows the page ee command reference; press any key to close it. |
| file operations | read a file asks for a file and puts its text in at the caret; save file writes the open file. |
| redraw screen | Draws the screen again. |
| settings | info window: on and info window: off show or hide the five rows. |
| search | Asks for the text to search for. |

## Leaving quickly

1. Press [[Esc]] to open the main menu.
2. Press [[Enter]] to choose leave editor.
3. Press [[Enter]] again to choose save changes, which is selected at first.

Choose no save instead to leave without writing the file. ee does not ask again before it leaves.

## Opening, saving and the position row

When you open a file, the bottom row says "hello.txt" [New file] for a new file. For an existing file it says the name, the lines and the characters, as in "hello.txt" 3 lines, 42 characters. Saving says the same thing.

Under the shortcut rows, a row shows where the caret is: its line, its column and how many lines it is from the top of the screen.

## The command line

Press ^c. The bottom row shows the label "Command" and waits for a word. Type the word and press [[Enter]].

Table: Command words

| Word | What it does |
| --- | --- |
| `write` | Saves the file. |
| `quit` | Leaves the editor at once, without saving. |
| `exit` | Saves the file, then leaves the editor. |

## Searching and prompts

Press ^y, or choose search in the main menu, and type the text. Press [[Enter]]. The caret moves to the text. ^x searches again for the last text you typed.

Any prompt on the bottom row waits for you. Press [[Enter]] to answer it, [[Esc]] to cancel it, and Backspace to correct what you typed.

```command
id: jsc:command/freebsd/ee
```

## If something goes wrong

> [!PROBLEM] The bottom row says Unknown command, followed by what you typed.
> The command line knows only write, quit and exit. Type one of those words.

> [!PROBLEM] The bottom row says Not found, followed by your text.
> The text is not in the file, or it is spelled differently. Check it and search again.

> [!PROBLEM] You press ^x, and nothing happens.
> No search text has been typed yet. Search with ^y first, then ^x searches again.

> [!PROBLEM] You choose read a file, and the bottom row says "name": No such file or directory.
> The file does not exist, or its name is wrong. Type the name again, exactly as the file has it.

> [!PROBLEM] You leave with no save or with quit, and your changes are gone.
> ee does not ask first when you leave with no save or with quit. Choose save changes in the main menu, or type write at the command prompt (^c), before you leave.

> [!PROBLEM] The shortcut rows have disappeared.
> The info window is off. Open the main menu, choose settings, then info window: on.

> [!SEE] jsc:vi, jsc:vim, jsc:the_systems
