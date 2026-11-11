---
section: the_editors
order: 275
---
# Vim

Vim works in modes. A mode decides what a key does: in one mode a letter is typed into the file, and in another the same letter is a command that moves the caret or deletes text.

The Vim here is a modern copy of the editor. It is not all of Vim. It has what you need to write and change a program: moving, typing, deleting, copying a line, undoing and a few colon commands. It has no search and no split windows.

The bottom row shows the name of the file in quotes, with [+] after it when the text has changes that are not saved. In insert mode it also says -- INSERT --.

> [!DEFINE] Mode
> The state that decides what a key does: normal, insert or command.

## The three modes

You start in normal mode. Press [[Esc]] to return to normal mode from insert mode or from command mode.

Table: The modes

| Mode | What the keys do |
| --- | --- |
| Normal | A letter is a command: it moves the caret, deletes, copies or starts insert mode. |
| Insert | Letters, digits and symbols are typed into the file. |
| Command | What you type after the colon runs when you press [[Enter]]. |

## Keys in normal mode

Table: Keys in normal mode

| Key | What it does |
| --- | --- |
| `i` | Starts insert mode before the caret. |
| `a` | Starts insert mode after the caret. |
| `A` | Starts insert mode at the end of the line. |
| `I` | Starts insert mode at the start of the line. |
| `o` | Opens a new line below and starts insert mode there. |
| `O` | Opens a new line above and starts insert mode there. |
| `h j k l` | Moves left, down, up and right. |
| `w` | Moves to the start of the next word, on the next line when this line has no more words. |
| `b` | Moves to the start of the previous word. |
| `e` | Moves to the end of the word the caret is on, or of the next one. |
| `0` | Moves to the start of the line. |
| `^` | Moves to the first character that is not a space. |
| `$` | Moves to the end of the line. |
| `G` | Moves to the last line of the file. |
| `gg` | Moves to the first line of the file. |
| `x` | Deletes the character under the caret, unless the caret is at the end of the line. |
| `D` | Deletes from the caret to the end of the line. |
| `dd` | Deletes the whole line and keeps it in the register. |
| `dw` | Deletes up to the next word. When the next word is on another line, it deletes to the end of this line. |
| `yy` | Copies the whole line into the register. |
| `p` | Puts the register back on the line below. |
| `P` | Puts the register back on the line above. |
| `u` | Undoes the last change. |
| `Ctrl+R` | Redoes the change that undo took back, in normal mode. |
| `:` | Starts a colon command. The bottom row shows the colon and what you type. |
| `Arrows, Home, End` | Move the caret. |
| `PageUp, PageDown` | Move the caret ten lines up or down. |
| `Enter` | Moves down one line. |
| `Backspace` | Moves left one column. |
| `Esc` | Stays in normal mode, and cancels a half-typed dd, dw, yy or gg. |

The keys d, y and g wait for a second key. Only dd, dw, yy and gg do anything. Any other second key is ignored, and the wait ends.

> [!DEFINE] Register
> The place where dd and yy keep the line they copied, so that p and P can put it back.

## Keys in insert mode

Table: Keys in insert mode

| Key | What it does |
| --- | --- |
| Letters, digits and symbols | Are typed into the file. |
| `Enter` | Starts a new line with the same indent as the line above. After a brace {, it indents four more spaces. |
| `Tab` | Types four spaces. |
| `Backspace` | Deletes the character before the caret. |
| `Delete` | Deletes the character under the caret. |
| `Esc` | Returns to normal mode. One undo takes back the whole insert. |

## Colon commands

Type a colon command after the colon, then press [[Enter]]. Nothing happens until you press it.

Table: Colon commands

| Command | What it does |
| --- | --- |
| `:w` | Writes the file to disk. |
| `:q` | Quits. It is refused while the text has changes that are not saved. |
| `:q!` | Quits without saving, even with changes. |
| `:wq` | Writes the file, then quits. |
| `:x` | Writes the file, then quits. |
| `:wq!` | Writes the file, then quits. |
| `:x!` | Writes the file, then quits. |
| `:<number>` | Goes to that line. 0 goes to line 1, and a number past the end goes to the last line. |
| `: Enter` | With nothing after the colon, does nothing. |

## Messages on the bottom row

Table: Messages

| Message | When you see it |
| --- | --- |
| "hello.txt" [New] | You open a file that does not exist yet. |
| "hello.txt" written | You write the file with :w, :wq or :x. |
| 1 line yanked | You copy a line with yy. |
| 1 change; before | You undo a change with u. |
| 1 change; after | You redo a change with Ctrl+R. |
| Already at oldest change | You press u, and nothing is left to undo. |
| Already at newest change | You press Ctrl+R, and nothing is left to redo. |
| E492: not an editor command: followed by what you typed | The colon command is not one of the commands above. |
| E37: no write since last change (add ! to override) | You try to quit with changes that are not saved. |
| Too long to save: followed by two numbers | The text is too long to save, and the file keeps its old content. |

```command
id: jsc:command/linux/vim
```

## If something goes wrong

> [!PROBLEM] The bottom row says E37: no write since last change (add ! to override).
> The text has changes that are not saved. Type :w to save, then :q. Or type :q! to leave without saving.

> [!PROBLEM] The bottom row says E492: not an editor command, followed by what you typed.
> The colon command is not in the table above. Check its spelling.

> [!PROBLEM] The bottom row says Too long to save.
> The text is more than 32,767 characters, so the file keeps its old content. Make the text shorter, or split it into more than one file.

> [!PROBLEM] You type 3 and then dd, and only one line goes.
> Counts are not supported here. Type dd once for each line.

> [!PROBLEM] After d, y or g, nothing happens.
> Only dd, dw, yy and gg are commands. Press [[Esc]] to cancel the wait, then type a command.

> [!PROBLEM] The bottom row says Already at oldest change.
> There is nothing left to undo. Make the change again, or leave the file as it is.

> [!SEE] jsc:emacs, jsc:vi, jsc:ee
